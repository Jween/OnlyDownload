### download 模块

+ `service`: 暴露 Schizo 接口, 提供跨进程访问服务
    + `DownloadService`: 下载服务的对外跨进程访问接口
    + `DownloadQueue`: 额外(举例)实现的排队下载管理


+ `downloader/`: 下载器包
    + `FileDownloader`: 文件下载器, 一个文件的下载会重复用到很多个区块下载
    + `BlockDownloader`: 区块下载器
    + `DownloadState`: 下载状态枚举
    + `XmlConfigs`: 自定义配置, 使用者通过 xml 资源来自定义下载配置
+ `dao/`: dao 包, 记录下载文件与断点信息
    + `DownloadFileInfoDao`: DB 对 文件信息的操作
    + `DownloadBlockDao`: DB 对区块信息的操作
+ `entity/`: DB 实体包
    + `DownloadFileInfo`: 文件信息, 不可变
    + `DownloadBlock`: 区块信息, 随下载变化
+ `persistence/`: 持久化
    + DownloadDatabase: 下载数据库
+ `file/`: 文件处理包
    + `DownloadFiles`: 处理下载文件的工具类
    + `FileWriter`: 下载流文件写入相关功能
+ `http/`: http 包
    + `RxStreaming`: 将 http 请求 Rx 数据流化, 并处理分段下载逻辑
+ `model/`: model 包, producer, provider, 都是异步并提供 InMem 缓存的
    + `DownloadProgress`: 下载进度 model, data 类, 不提供任何额外功能
    + `DownloadFileInfoProvider`: 创建文件信息的类(根据 url 创建下载文件信息)
    + `DownloadBlockProducer`: 提供和创建区块信息的类, 线程安全
    + `RxInMemHolder`: InMem 数据的 Rx 简单实现
+ `namer/` 文件改名包
    + `FileNamer`: 文件改名接口
    + `HashFileNamer`: 根据 url, 以 hash 值为文件命名


