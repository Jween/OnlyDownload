package io.jween.onlydownload.dao;

import io.jween.onlydownload.entity.DownloadBlock;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import io.reactivex.Flowable;
import io.reactivex.Single;

@Dao
public interface DownloadBlockDao {
    @Query("SELECT * FROM DownloadBlock WHERE fileInfoId = :fileInfoId")
    Single<List<DownloadBlock>> singleDownloadBlocksForDownloadFileInfo(long fileInfoId);

    @Query("SELECT * FROM DownloadBlock WHERE fileInfoId = :fileInfoId")
    Flowable<List<DownloadBlock>> flowDownloadBlocksForDownloadFileInfo(long fileInfoId);

    @Query("SELECT * FROM DownloadBlock WHERE fileInfoId = :fileInfoId")
    List<DownloadBlock> findDownloadBlocksForDownloadFileInfo(long fileInfoId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertDownloadBlock(DownloadBlock block);

    @Update
    int updateDownloadBlock(DownloadBlock block);

    @Delete
    int deleteDownloadBlock(DownloadBlock block);
}
