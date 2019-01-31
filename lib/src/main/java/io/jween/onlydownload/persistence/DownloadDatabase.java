package io.jween.onlydownload.persistence;

import android.content.Context;

import io.jween.onlydownload.dao.DownloadBlockDao;
import io.jween.onlydownload.dao.DownloadFileInfoDao;
import io.jween.onlydownload.entity.DownloadBlock;
import io.jween.onlydownload.entity.DownloadFileInfo;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {DownloadBlock.class, DownloadFileInfo.class}, version = 1)
public abstract class DownloadDatabase extends RoomDatabase {
    private static volatile DownloadDatabase INSTANCE;

    public abstract DownloadBlockDao downloadBlockDao();
    public abstract DownloadFileInfoDao downloadFileInfoDao();

    public static DownloadDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (DownloadDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            DownloadDatabase.class, "download.db")
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public static void releaseInstance() {
        if (INSTANCE != null) {
            synchronized (DownloadDatabase.class) {
                INSTANCE.close();
                INSTANCE = null;
            }
        }
    }
}
