package io.jween.onlydownload.dao;

import io.jween.onlydownload.entity.DownloadFileInfo;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import io.reactivex.Flowable;
import io.reactivex.Single;

@Dao
public interface DownloadFileInfoDao {
    @Query("SELECT * FROM DownloadFileInfo WHERE id = :fileInfoId LIMIT 1")
    Flowable<DownloadFileInfo> flowDownloadFileInfoById(long fileInfoId);

    @Query("SELECT * FROM DownloadFileInfo WHERE id = :fileInfoId")
    DownloadFileInfo findDownloadFileInfoById(long fileInfoId);

    @Query("SELECT * FROM DownloadFileInfo WHERE url = :url LIMIT 1")
    Flowable<DownloadFileInfo> flowDownloadFileInfoByUrl(String url);

    @Query("SELECT * FROM DownloadFileInfo WHERE url = :url LIMIT 1")
    Single<DownloadFileInfo> singleDownloadFileInfoByUrl(String url);


    @Query("SELECT * FROM DownloadFileInfo WHERE url = :url")
    DownloadFileInfo findDownloadFileInfoByUrl(String url);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertDownloadFileInfo(DownloadFileInfo  downloadFileInfo);

    @Update
    int updateDownloadFileInfo(DownloadFileInfo downloadFileInfo);

    @Delete
    int deleteDownloadFileInfo(DownloadFileInfo downloadFileInfo);
}
