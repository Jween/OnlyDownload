package io.jween.onlydownload.file;

import io.jween.onlydownload.entity.DownloadFileInfo;

import java.io.File;
import java.io.IOException;

public class DownloadFiles {

    public static String getFileExtension(String fullPathOrUrl) {
        int lastIndexOf = fullPathOrUrl.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // empty extension
        }
        return fullPathOrUrl.substring(lastIndexOf);
    }

    public static void createDownloadFile(DownloadFileInfo fileInfo) throws IOException {
        File file = new File(fileInfo.getDownloadFileFullPath());
        file.createNewFile();
    }

    public static void removeDownloadFile(DownloadFileInfo fileInfo) {
        File file = new File(fileInfo.getDownloadFileFullPath());
        file.delete();
    }


    public static void move(File file, String newPath) {
        File newFile = new File(newPath);
        if (newFile.exists()) {
            move(newFile, newPath + ".old");
        }
        file.renameTo(newFile);
    }
}
