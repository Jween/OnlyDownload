package io.jween.onlydownload.entity;

import java.io.File;
import java.io.IOException;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(indices = @Index(value="url", unique = true))
public class DownloadFileInfo {
    private static final String DOWNLOAD_EXTENSION = ".dat";

    @PrimaryKey
    @ColumnInfo(name = "id")
    public long id;

    public String url;
    public String path;
    public String name;
    public String extension;
    public long length;

    public String getDownloadFileFullPath() {
        return fullPathWithExtension(DOWNLOAD_EXTENSION);
    }

    public String getDestinationFileFullPath() {
        return fullPathWithExtension(extension);
    }

    private String fullPathWithExtension(String extension) {
        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }
        return path + File.separator + name + extension;
    }

    public long getLength() {
        return length;
    }

    public File toFile() throws IOException {
        File file = new File(getDownloadFileFullPath());
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            file.createNewFile();
        }
        return file;
    }
}
