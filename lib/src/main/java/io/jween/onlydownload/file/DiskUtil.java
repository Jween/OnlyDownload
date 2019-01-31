package io.jween.onlydownload.file;

import android.os.Environment;
import android.os.StatFs;

import java.io.File;

public class DiskUtil {

    private static final long MEGA_BYTE = 1048576;

    /**
     * Calculates total space on disk
     * @param external  If true will query external disk, otherwise will query internal disk.
     * @return Number of mega bytes on disk.
     */
    public static int totalSpace(boolean external) {
        StatFs statFs = getStats(external);
        long total = (statFs.getBlockCountLong() * statFs.getBlockSizeLong()) / MEGA_BYTE;
        return (int) total;
    }

    /**
     * Calculates free space on disk
     * @param external  If true will query external disk, otherwise will query internal disk.
     * @return Number of free mega bytes on disk.
     */
    public static int freeSpace(boolean external) {
        StatFs statFs = getStats(external);
        long availableBlocks = statFs.getAvailableBlocksLong();
        long blockSize = statFs.getBlockSizeLong();
        long freeBytes = availableBlocks * blockSize;

        return (int) (freeBytes / MEGA_BYTE);
    }

    /**
     * Calculates occupied space on disk
     * @param external  If true will query external disk, otherwise will query internal disk.
     * @return Number of occupied mega bytes on disk.
     */
    public static int busySpace(boolean external) {
        StatFs statFs = getStats(external);
        long total = (statFs.getBlockCountLong() * statFs.getBlockSizeLong());
        long free  = (statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong());

        return (int) ((total - free) / MEGA_BYTE);
    }

    private static StatFs getStats(boolean external) {
        String path;

        if (external){
            path = Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        else{
            path = Environment.getRootDirectory().getAbsolutePath();
        }

        return new StatFs(path);
    }

    public static String getExternalStorageDirectory() {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ) {
            return "";
        }
        return Environment.getExternalStorageDirectory().getPath();
    }

    /**
     *
     * @return /storage/emulated/0/Download folder file
     */
    public static File getDownloadDir() {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ) {
            return null;
        }

        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }
}
