package io.jween.onlydownload.model;

import io.jween.onlydownload.dao.DownloadFileInfoDao;
import io.jween.onlydownload.entity.DownloadFileInfo;
import io.jween.onlydownload.http.RxStreaming;
import io.jween.onlydownload.file.DownloadFiles;

import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Response;

/**
 * 提供 文件信息 的类
 */
public class DownloadFileInfoProvider extends RxInMemHolder<DownloadFileInfo> {

    private String url;
    private String fileName;
    private String downloadFolderPath;
    private DownloadFileInfoDao dao;

    public DownloadFileInfoProvider(String url, String fileName, String downloadFolderPath, DownloadFileInfoDao dao) {
        this.url = url;
        this.dao = dao;
        this.fileName = fileName;
        this.downloadFolderPath = downloadFolderPath;
    }

    @Override
    protected Single<DownloadFileInfo> provide() {
        return dao.singleDownloadFileInfoByUrl(url)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .onErrorResumeNext(createDownloadFileInfo(url));
    }

    private Single<Long> getContentLength(String url) {
        return RxStreaming.request(url)
                .map(new Function<Response, Long>() {
                    @Override
                    public Long apply(Response response) throws Exception {
                        long contentLength = response.body().contentLength();
                        response.close();
                        return contentLength;
                    }
                });
    }

    /**
     * 根据文件 url 创建文件信息
     * @param url 文件的 url
     * @return 返回创建的文件信息
     */
    private Single<DownloadFileInfo> createDownloadFileInfo(final String url) {
        return getContentLength(url)
                .map(new Function<Long, DownloadFileInfo>() {
                    @Override
                    public DownloadFileInfo apply(Long length) throws Exception {
                        DownloadFileInfo fileInfo;
                        // 不存在任务, 创建任务, 开始下载
                        fileInfo = new DownloadFileInfo();
                        fileInfo.url = url;
                        fileInfo.name = fileName;
                        fileInfo.path = downloadFolderPath;
                        fileInfo.extension = DownloadFiles.getFileExtension(url);
                        fileInfo.length = length;
                        fileInfo.id = dao.insertDownloadFileInfo(fileInfo);
                        return fileInfo;
                    }
                });
    }

}
