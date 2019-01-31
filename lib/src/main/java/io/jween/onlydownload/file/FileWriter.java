package io.jween.onlydownload.file;

import io.jween.onlydownload.entity.DownloadFileInfo;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * 处理 下载文件 相关的类
 */
public class FileWriter implements Closeable {
    private DownloadFileInfo fileInfo;
    private File file;
    private RandomAccessFile raf;
    private final Object WRITER_LOCK = new Object[0];
    volatile boolean open = true;

    private FileWriter() {}

    public static FileWriter from(DownloadFileInfo fileInfo) throws IOException {
        FileWriter fileWriter = new FileWriter();
        fileWriter.fileInfo = fileInfo;
        fileWriter.file = fileInfo.toFile();
        fileWriter.raf = new RandomAccessFile(fileWriter.file, "rwd");

        return fileWriter;
    }

//    // TODO MappedByteBuffer 在 Android 系统上无法手动回收, 需要修改成其它方式
//    public MappedByteBuffer getMappedByteBuffer(long offset, long length) throws IOException {
//        synchronized (WRITER_LOCK) {
//            return channel.map(FileChannel.MapMode.READ_WRITE, offset, length);
//        }
//    }

    public void write(byte[] bytes, long position) throws IOException {
        synchronized (WRITER_LOCK) {
            raf.seek(position);
            raf.write(bytes);
        }
    }

    public DownloadFileInfo getFileInfo() {
        return fileInfo;
    }

    @Override
    public void close() throws IOException {
        synchronized (WRITER_LOCK) {
            open = false;
            raf.close();
        }
    }

    public boolean isOpen() {
        return open;
    }

    public void delete() {
        file.delete();
    }

    public void rename() {
        moveTo(fileInfo.getDestinationFileFullPath());
    }

    public void renameTo(String name) {
        file.renameTo(new File(file.getParentFile(), name));
    }

    public void moveTo(String fullPath) {
        DownloadFiles.move(this.file, fullPath);
    }
}
