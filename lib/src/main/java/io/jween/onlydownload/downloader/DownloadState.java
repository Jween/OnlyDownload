package io.jween.onlydownload.downloader;

public enum DownloadState {
    PENDING,
    STARTED,
    DOWNLOADING,
    ERROR,
    PAUSED,
    DOWNLOADED,
    CANCELED
}
