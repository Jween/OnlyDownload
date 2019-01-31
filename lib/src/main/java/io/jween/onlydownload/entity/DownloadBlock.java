package io.jween.onlydownload.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(
        foreignKeys = @ForeignKey(entity = DownloadFileInfo.class,
                parentColumns = "id",
                childColumns = "fileInfoId",
                onDelete = CASCADE),
        indices = @Index(value="fileInfoId")
)
public class DownloadBlock {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private long freeSpace;
    private long writerPosition;

    @Ignore
    private final Object mutationLock = new Object[0];

    private long fileInfoId;

    @Ignore
    public DownloadBlock(long fileInfoId, long writerPosition, long freeSpace) {
        this(0, fileInfoId, writerPosition, freeSpace);
    }

    public DownloadBlock(long id, long fileInfoId, long writerPosition, long freeSpace) {
        this.id = id;
        this.fileInfoId = fileInfoId;
        this.writerPosition = writerPosition;
        this.freeSpace = freeSpace;
    }

    public long getWriterPosition() {
        synchronized (mutationLock) {
            return writerPosition;
        }
    }

    public long getTerminalPosition() {
        synchronized (mutationLock) {
            return writerPosition + freeSpace;
        }
    }

    public void increaseWriterPosition(long size) {
        synchronized (mutationLock) {
            this.writerPosition += size;
            this.freeSpace -= size;
        }
    }

    public void decreaseWriterPosition(long size) {
        synchronized (mutationLock) {
            this.writerPosition -= size;
            this.freeSpace += size;
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getFileInfoId() {
        return fileInfoId;
    }

    public void setFileInfoId(int fileInfoId) {
        this.fileInfoId = fileInfoId;
    }

    public long getFreeSpace() {
        synchronized (mutationLock) {
            return freeSpace;
        }
    }

    public void markDirty() {
        this.fileInfoId = 0;
    }

    public boolean isDirty() {
        return fileInfoId == 0;
    }

    public DownloadBlock copy() {
        return new DownloadBlock(getId(), getFileInfoId(), getWriterPosition(), getFreeSpace());
    }
}
