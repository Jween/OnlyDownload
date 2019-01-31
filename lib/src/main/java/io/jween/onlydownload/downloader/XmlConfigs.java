package io.jween.onlydownload.downloader;

import android.content.Context;
import android.content.res.Resources;

import io.jween.onlydownload.R;

/**
 * XmlConfigs 包含所有下载配置
 * 这些配置可以被使用者用 string 资源来重写修改
 */
public class XmlConfigs {

    /**
     * 下载存储的子文件目录, 默认是 AppCenter/Apk, 附文件目录 Download/ 不可更改
     *
     */
    public final String downloadSubFolder;
    /**
     * 每个下载任务最多允许多线程分片断点下载的并发数, 默认是 3 个并发下载一个文件
     */
    public final int maxThreadCount;

    /**
     * 分片下载的时候, 每个分片的大小
     */
    public final long maxBlockSize;

    private XmlConfigs(Context context) {
        Resources resources = context.getResources();
        downloadSubFolder = resources.getString(R.string.download_sub_folder_path);
        maxThreadCount = resources.getInteger(R.integer.download_max_thread_count);
        maxBlockSize = resources.getInteger(R.integer.download_max_block_size);
    }

    private static XmlConfigs CONFIGS_INSTANCE;

    public static XmlConfigs from(Context context) {
        if (CONFIGS_INSTANCE == null) {
            XmlConfigs configs = new XmlConfigs(context);
            if (CONFIGS_INSTANCE == null) {
                CONFIGS_INSTANCE = configs;
            }
        }
        return CONFIGS_INSTANCE;
    }
}
