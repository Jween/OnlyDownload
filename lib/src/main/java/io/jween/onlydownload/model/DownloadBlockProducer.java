package io.jween.onlydownload.model;

import io.jween.onlydownload.dao.DownloadBlockDao;
import io.jween.onlydownload.entity.DownloadBlock;
import io.jween.onlydownload.entity.DownloadFileInfo;

import java.util.LinkedList;
import java.util.List;

/**
 * 提供 区块 信息的类
 *
 * 线程安全的类
 */
public class DownloadBlockProducer {
    /**
     * 区块依赖文件信息
     */
    private DownloadFileInfo downloadFileInfo;

    private DownloadBlockDao dao;
    private List<DownloadBlock> pendingBlocks;  // 从 DB 读取的 DownloadBlock(断点续传什么的)


    /**
     * 每一个区块的大小
     */
//    private static final long MAX_BLOCK_SIZE = 2 * 1024 * 1024; // 2MB

    /**
     * 已经分配的所有区块总长度, 用于分配新区块
     */
    private long allocatedLength = 0;
    private final long maxBlockSize;

    public DownloadBlockProducer(DownloadFileInfo fileInfo, long maxBlockSize, DownloadBlockDao dao) {
        this.downloadFileInfo = fileInfo;
        this.dao = dao;
        this.maxBlockSize = maxBlockSize;
        initPendingBlocksFromDB();
    }

    /**
     * 读取 DB 记录的上次未下载完的区块信息
     */
    private synchronized void initPendingBlocksFromDB() {
        List<DownloadBlock> blocks = dao.findDownloadBlocksForDownloadFileInfo(downloadFileInfo.id);
        if (blocks != null) {
            int size = blocks.size();
            if (size > 0) {
                DownloadBlock latestBlock = blocks.get(size - 1);
                allocatedLength = latestBlock.getTerminalPosition();
            }
        } else {
            blocks = new LinkedList<>();
        }
        pendingBlocks = blocks;
    }

    public synchronized boolean isFullAllocated() {
        return allocatedLength >= downloadFileInfo.length;
    }

    /**
     * 创建新的区块
     * @return 返回创建的区块
     */
    private synchronized DownloadBlock createDownloadBlock() {
        long fileSize = downloadFileInfo.length;
        if (isFullAllocated()) {
            throw new RuntimeException("You can't create a DownloadBlock, when file is full allocated");
        }
        long blockSize = 0;
        if (fileSize >= allocatedLength + maxBlockSize) {
            blockSize = maxBlockSize;
        } else {
            blockSize = fileSize - allocatedLength;
        }

        DownloadBlock block = new DownloadBlock(downloadFileInfo.id,
                allocatedLength, blockSize);
        allocatedLength += blockSize;
        long blockId = dao.insertDownloadBlock(block);
        block.setId(blockId);
        return block;
    }

    public synchronized long getAllocatedLength() {
        return allocatedLength;
    }

    public synchronized DownloadBlock offer() {
        if (isFullAllocated()) {
            return null;
        }

        if (pendingBlocks.size() > 0) {
            return pendingBlocks.remove(0);
        } else {
            return createDownloadBlock();
        }
    }
}
