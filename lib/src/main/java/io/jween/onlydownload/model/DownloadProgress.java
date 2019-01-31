package io.jween.onlydownload.model;

import io.jween.onlydownload.downloader.DownloadState;

public class DownloadProgress {

    public long downloadedSize;
    public long totalSize;
    public long downloadSpeed;
    public DownloadState downloadState;

    public String speedInString() {

        return "";
    }

    public String progressInString() {
        return "";
    }

    public String downloadedSizeInString() {
        return "";
    }

    /**
     *
     * @return 0 ~ 100 if downloading,
     * -1 if not started
     */
    public int getProgress() {
        return (int) (100.f * downloadedSize / totalSize) ;
    }
}
