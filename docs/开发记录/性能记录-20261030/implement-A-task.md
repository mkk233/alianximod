# A：性能记录公共设施

你是唯一 A 作者，Cursor CLI --model grok-4.7-high-fast；不委托子代理。用户已授权开始性能记录。本阶段为实现，不是新设计复审。完整读取本目录 design-decision.md 并严格按其已经冻结的接口、枚举、会话、命令和统计边界实施；设计裁决已用 1/2，实现复审尚未启动 0/3。不再自行另开正式复审或测试。

唯一可写源码（根 D:/alianximod/src/main/java/com/bslx/XueXiMod/）：新增 debug/performance/PerformanceRecorder.java、PerfMetric.java、PerfCounter.java、PerformanceCommands.java、PerformanceEvents.java；已有 command/ModCommands.java 只追加命令注册。另可新增 D:/alianximod/docs/性能记录使用说明.md，及本目录 implementation-A-final.md、implementation-A-evidence.json、before-A/ 备份。其他一律只读。B/C 正在互斥文件并行插桩，不读他们中间改动，不改他们文件。公共接口与枚举必须完全照 design-decision.md，B/C 直接依赖。

任务：实现默认关闭的、服务端线程/服务器identity门控、每完整 tick 聚合、有界6000 tick、start默认1200、stop/status/export权限2命令；本地summary.txt+ticks.csv，精确P95算法与来源边界；新的 Forge 事件订阅器处理start/stop/tick，事件优先级与生命周期遵照裁决。导出只在STOPPED，唯一目录不覆盖，失败保留内存。关闭状态不读nanoTime/不分配，热路径不创建Scope/字符串/lambda/日志。不要把inclusive项加成总耗时，不用客户端DebugLog状态冒充服务端状态。准确处理 stop pending、armed取消、残缺tick、同JVM多次服务器。

原有大量未提交改动必须完整保留。先备份将写的现有文件到 before-A/，记录其原hash；禁止reset/clean/checkout覆盖、提交、制作表修改、打包、运行游戏。只读依赖定位可以使用 rg 精确符号和本机 Forge/Java API，不整仓通读。git 若需要用单次 -c safe.directory=D:/alianximod。不改Gradle或配置来绕过编译。

无编译授权，不跑gradlew、build、jar、reobf、runClient，不建立独立测试。实现结束做必要的本任务定向完整性检查，不能从设计外自增功能。若固定模型不可用或涉及架构阻塞，保留现场如实报告，不能降档。

最终必须写 implementation-A-final.md 与 evidence.json：完整改动路径、关键行号、实现行为、未覆盖项、待统一编译、没有游戏实测；文件最后写入UTC时间/hash；自己对列明文件已结束写入并释放所有权。最终反馈应一次完整返回，不要求主会话持续跟进过程。不得在报告中声称性能提升或用户已经实测。
