package io.jween.onlydownload.downloader;

import android.os.OperationCanceledException;

import io.jween.onlydownload.dao.DownloadBlockDao;
import io.jween.onlydownload.entity.DownloadBlock;
import io.jween.onlydownload.file.FileWriter;
import io.jween.onlydownload.http.RxStreaming;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

public class BlockDownloader {
    private String url; // key
    private volatile DownloadBlock block;
    private DownloadBlockDao blockDao;
//    private MappedByteBuffer blockWriter;
    private FileWriter fileWriter;

    private Disposable dbSyncDisposer;

    private BehaviorSubject<DownloadBlock> blockPublisher;

    private boolean dirty = false;

    public BlockDownloader(String url, DownloadBlock block, DownloadBlockDao dao, FileWriter fileWriter) throws IOException {
        this.block = block;
        this.url = url;
        this.blockDao = dao;
        this.fileWriter = fileWriter;
        refresh();
    }

    void refresh() {
        if (blockPublisher == null
                || blockPublisher.hasThrowable()
                || blockPublisher.hasComplete()) {
            blockPublisher = BehaviorSubject.createDefault(block);
        }

        this.dirty = false;
    }

    public Flowable<DownloadBlock> observeDownloadBlock() {
        return blockPublisher
                .toFlowable(BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io());
    }

    public DownloadBlock copyDownloadBlock() {
        return block.copy();
    }

    /**
     * 已经废弃, FileDownloader 计算进度的时候, 直接通过文件增长计算总下载速度
     * 区块的下载速度, 没有作用
     * 计算该区块的下载速度
     * @return
     */
    @Deprecated
    public Flowable<Long> observeSpeed() {
        return observeDownloadBlock()
                .throttleLast(1, TimeUnit.SECONDS)
                .map(new Function<DownloadBlock, Long>() {
                    @Override
                    public Long apply(DownloadBlock downloadBlock) throws Exception {
                        return downloadBlock.getWriterPosition();
                    }
                })
                .buffer(2, 1)
                .map(new Function<List<Long>, Long>() {
                    @Override
                    public Long apply(List<Long> longs) {
                        long result = 0;
                        if (longs.size() == 2) {
                            result = longs.get(1) - longs.get(0);
                        }
                        return result;
                    }
                })
                .startWith(0L);
    }

    public Flowable<DownloadBlock> start() {
        if (isDirty() ) {
            refresh();
        }
        syncDB();

        return RxStreaming.download(url, block.getWriterPosition(), block.getFreeSpace())
                .map(new Function<byte[], DownloadBlock>() {
                    @Override
                    public DownloadBlock apply(byte[] bytes) throws Exception {
                        // 写入文件流
                        try {
                            fileWriter.write(bytes, block.getWriterPosition());
                            block.increaseWriterPosition(bytes.length);

                            // 通知变更, 用于 UI 展示, DB 更新等
                            if (!blockPublisher.hasThrowable()) {
                                blockPublisher.onNext(copyDownloadBlock());
                            }
                        } catch (Exception e) {
                            // 忽略掉写入失败
                        }
                        return block;
                    }
                })
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        markDirty();
                        blockDao.updateDownloadBlock(block);
                        blockPublisher.onError(throwable);
                    }
                })
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        markDirty();
                        blockDao.deleteDownloadBlock(block);
                        blockPublisher.onComplete();
                    }
                })
                .doOnCancel(new Action() {
                    @Override
                    public void run() throws Exception {
                        markDirty();
                        blockDao.updateDownloadBlock(block);
                        if (!blockPublisher.hasThrowable()
                                && !blockPublisher.hasComplete()) {
                            blockPublisher.onError(new OperationCanceledException());
                        }
                    }
                });
    }

    private void syncDB() {
        dbSyncDisposer = observeDownloadBlock()
                .throttleLast(1, TimeUnit.SECONDS)
                .observeOn(Schedulers.io())
                .subscribe(new Consumer<DownloadBlock>() {
                    @Override
                    public void accept(DownloadBlock downloadBlock) throws Exception {
                        blockDao.updateDownloadBlock(downloadBlock);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        // ignore
                    }
                });
    }

    public boolean isDirty() {
        return dirty;
    }

    private void markDirty() {
        this.dirty = true;
        if (dbSyncDisposer != null && !dbSyncDisposer.isDisposed()) {
            dbSyncDisposer.dispose();
        }
    }
}
