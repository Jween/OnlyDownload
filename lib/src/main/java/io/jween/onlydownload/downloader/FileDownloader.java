package io.jween.onlydownload.downloader;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import io.jween.onlydownload.dao.DownloadBlockDao;
import io.jween.onlydownload.dao.DownloadFileInfoDao;
import io.jween.onlydownload.entity.DownloadBlock;
import io.jween.onlydownload.file.DiskUtil;
import io.jween.onlydownload.file.FileWriter;
import io.jween.onlydownload.model.DownloadBlockProducer;
import io.jween.onlydownload.model.DownloadFileInfoProvider;
import io.jween.onlydownload.model.DownloadProgress;
import io.jween.onlydownload.namer.FileNamer;
import io.jween.onlydownload.persistence.DownloadDatabase;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

public class FileDownloader {
    private final String url;
    private DownloadFileInfoDao fileInfoDao;
    private DownloadFileInfoProvider downloadFileInfoProvider;
    private DownloadBlockDao blockDao;

    private FileWriter fileWriter;
    private DownloadBlockProducer blockProducer;
    private ConcurrentLinkedQueue<BlockDownloader>
            blockDownloaders = new ConcurrentLinkedQueue<>();
    private XmlConfigs configs;
    private CompositeDisposable blockDisposers = new CompositeDisposable();

    private StateProvider stateProvider = new StateProvider();

    /**
     * 下载状态管理内部类
     */
    private class StateProvider {
        /**
         * 下载状态, 其中 DOWNLOADING 状态会在下载时一直 emit, 方便计算速度
         * 只需要一个 DOWNLOADING 进行逻辑处理的地方, 调用 distinctUtilChanged 过滤
         */
        BehaviorSubject<DownloadState> stateBehaviorSubject =
                BehaviorSubject.createDefault(DownloadState.PENDING);

        synchronized void changeState(DownloadState newState) {
            stateBehaviorSubject.onNext(newState);
        }

        synchronized void changeToComplete() {
            stateBehaviorSubject.onComplete();
        }

        synchronized void changeToError(Throwable throwable) {
            stateBehaviorSubject.onError(throwable);
        }

        synchronized DownloadState getLatestDownloadState() {
            return stateBehaviorSubject.getValue();
        }

        synchronized boolean isDownloading() {
            DownloadState downloadState = getLatestDownloadState();
            return downloadState == DownloadState.STARTED
                    || downloadState == DownloadState.DOWNLOADING;
        }

        synchronized Flowable<DownloadState> toFlowable() {
            return stateBehaviorSubject.toFlowable(BackpressureStrategy.LATEST);
        }

    }
    public FileDownloader(Context context, String url, FileNamer fileNamer) {
        this.url = url;
        this.fileInfoDao =  DownloadDatabase.getInstance(context).downloadFileInfoDao();
        this.blockDao = DownloadDatabase.getInstance(context).downloadBlockDao();
        this.configs = XmlConfigs.from(context);
        String downloadFolderPath = new File(DiskUtil.getDownloadDir(),
                configs.downloadSubFolder).getAbsolutePath();
        this.
        downloadFileInfoProvider = new DownloadFileInfoProvider(url,
                fileNamer.from(url), downloadFolderPath, fileInfoDao);

        dispatchDownloadState();
    }

    public String getUrl() {
        return url;
    }

    /**
     * long time running task , should run in background task
     */
    private void prepareFileSystem() throws IOException {
        if (fileWriter == null || !fileWriter.isOpen()) {
            fileWriter = FileWriter.from(downloadFileInfoProvider.get().blockingGet());
        }
    }

    private void prepareBlockProducer() {
        if (blockProducer == null) {
            blockProducer = new DownloadBlockProducer(
                    downloadFileInfoProvider.get().blockingGet(), configs.maxBlockSize, blockDao);
        }
    }

    public DownloadState getLatestDownloadState() {
        return stateProvider.getLatestDownloadState();
    }

    public void start() {
        stateProvider.changeState(DownloadState.STARTED);
    }

    public void pause() {
        stateProvider.changeState(DownloadState.PAUSED);
    }

    public void cancel() {
        stateProvider.changeState(DownloadState.CANCELED);
    }

    public void clear() {
        pause();
        stateProvider.changeToError(new CancellationException());
        try {
            fileWriter.close();
        } catch (IOException e) {
            // do nothing
        }
    }

    private BlockDownloader createBlockDownloader() throws IOException {
        DownloadBlock downloadBlock = blockProducer.offer();
        if (downloadBlock == null) {
            return null;
        }

        return new BlockDownloader(url, downloadBlock, blockDao, fileWriter);
    }

    private void activateBlockDownloader(final BlockDownloader blockDownloader) {
        final Disposable dis = blockDownloader.start()
                .subscribe(new Consumer<DownloadBlock>() {
                    @Override
                    public void accept(DownloadBlock downloadBlock) throws Exception {
                        // 下载中的 block 信息, 用于下载进度计算
                        if (stateProvider.isDownloading() ) {
                            stateProvider.changeState(DownloadState.DOWNLOADING);
                        }
                    }
                }, new Consumer<Throwable>() { // 区块下载异常
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        // 下载异常需要重新下载
                        if (stateProvider.isDownloading()) {
                            activateBlockDownloader(blockDownloader);
                        }
                    }
                }, new Action() { // 区块下载成功
                    @Override
                    public void run() throws Exception {
                        blockDownloaders.remove(blockDownloader);
                        if (stateProvider.isDownloading() ) {
                            activateNewBlockDownloader();
                        }
                    }
                });
        blockDisposers.add(dis);
    }

    private void activateNewBlockDownloader() throws IOException {
        if (blockProducer.isFullAllocated()) {
            if (blockDownloaders.size() <  1) {
                // 下载完毕
                stateProvider.changeState(DownloadState.DOWNLOADED);
            }
            // 下载快要完毕, 只剩最后几个区块
            return;
        }

        while (blockDownloaders.size() < configs.maxThreadCount) {
            // 多线程资源竞争可能导致生产者返回 null 值(意味着实际上已经下载结束了)
            final BlockDownloader newBD = createBlockDownloader();
            if (newBD != null) {
                activateBlockDownloader(newBD);
                blockDownloaders.add(newBD);
            }
        }
    }

    private void resumeDownload() throws IOException {
        int reviewSize = blockDownloaders.size();

        if (reviewSize > 0) {
            for (final BlockDownloader oldDownloader : blockDownloaders) {
                activateBlockDownloader(oldDownloader);
            }
        }

        if (reviewSize < configs.maxThreadCount) {
            activateNewBlockDownloader();
        }
    }

    private void pauseDownload() {
        blockDisposers.clear();
    }


    private Flowable<DownloadState> listenToDownloadState() {
        return stateProvider.toFlowable();
    }



    private void onStart() throws IOException {
        prepareFileSystem();
        prepareBlockProducer();
        stateProvider.changeState(DownloadState.DOWNLOADING);
    }

    private void onDownload() throws IOException {
        Log.d("STORE", "onDownload.");
        resumeDownload();
    }

    private void onPause() {
        Log.d("STORE", "onPause.");
        pauseDownload();
    }

    private void onCancel() throws IOException {
        Log.d("STORE", "onCancel.");
        pauseDownload();
        stateProvider.changeToComplete();
        removeFileInfoFromDB();
        fileWriter.close();
        fileWriter.delete();
    }

    private void onComplete() throws IOException {
        Log.d("STORE", "onComplete.");
        removeFileInfoFromDB();
        stateProvider.changeToComplete();
        fileWriter.close();
        fileWriter.rename();
    }

    private void onError(Throwable throwable) {
        Log.d("STORE", "onError.", throwable);
        stateProvider.changeToError(throwable);
        try {
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileWriter = null;
    }

    private Disposable dispatchDownloadState() {
        return listenToDownloadState()
                .distinctUntilChanged()
                .observeOn(Schedulers.io())
                .subscribe(new Consumer<DownloadState>() {
                    @Override
                    public void accept(DownloadState state) throws Exception {
                        switch (state) {
                            case PENDING:
                                // do nothing
                                break;
                            case STARTED:
                                onStart();
                                break;
                            case DOWNLOADING:
                                onDownload();
                                break;
                            case PAUSED:
                                onPause();
                                break;
                            case CANCELED:
                               onCancel();
                                break;
                            case DOWNLOADED:
                                onComplete();
                                break;
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        onError(throwable);
                    }
                });
    }

    private void removeFileInfoFromDB() {
        fileInfoDao.deleteDownloadFileInfo(fileWriter.getFileInfo());
    }

    public Flowable<DownloadProgress> listenToProgress() {
        return listenToDownloadState()
                .sample(1, TimeUnit.SECONDS, true)
                .map(new Function<DownloadState, DownloadProgress>() {
                    @Override
                    public DownloadProgress apply(DownloadState state) throws Exception {
                        DownloadProgress progress = new DownloadProgress();
                        progress.downloadState = state;
                        switch (state) {
                            case PENDING:
                            case STARTED:
                                progress.totalSize = 0;
                                progress.downloadedSize = 0;
                                break;
                            case DOWNLOADING:
                            case PAUSED:
                            case CANCELED:
                            case ERROR:
                                // 先减去未写入的空间
                                long totalFreeSpace = 0;
                                for (BlockDownloader bd : blockDownloaders) {
                                    DownloadBlock block = bd.copyDownloadBlock();
                                    totalFreeSpace += block.getFreeSpace();
                                }
                                // 后计算总长度(解决并发引起的速度计算成负数问题)
                                long allocatedLength = blockProducer.getAllocatedLength();
                                progress.downloadedSize = allocatedLength - totalFreeSpace;
                                progress.totalSize = downloadFileInfoProvider.get().blockingGet().length;
                                break;
                            case DOWNLOADED:
                                progress.totalSize = downloadFileInfoProvider.get().blockingGet().length;
                                progress.downloadedSize =  progress.totalSize;
                                break;
                        }
                        return progress;
                    }
                })
                // 计算下载速度
                .scan(new BiFunction<DownloadProgress, DownloadProgress, DownloadProgress>() {
                    @Override
                    public DownloadProgress apply(DownloadProgress downloadProgress, DownloadProgress downloadProgress2) throws Exception {
                        downloadProgress2.downloadSpeed =
                                downloadProgress2.downloadedSize - downloadProgress.downloadedSize;
                        return downloadProgress2;
                    }
                });
    }
}
