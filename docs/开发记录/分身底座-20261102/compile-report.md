# 分身底座当前编译状态（调度汇总）

当前结果：**compileJava 通过，最终命名门禁通过，待用户游戏实测。**

复用共享工作区由 Cursor CLI `--model grok-4.7-high-fast` 完成的最终构建。在 `D:/alianximod` 执行 `.\gradlew.bat compileJava --offline --no-daemon`，时间为 2026-11-02 05:55:18–05:55:43，退出码 0，`BUILD SUCCESSFUL in 24s`。`:compileJava` 实际执行，41 条弃用警告、没有错误。使用既有 JDK 17 和 `C:/Users/baishang/.gradle` 缓存。

本批固定模型编译者随后核对了完整最终日志及快照：整个 `src` 1200 个文件编译前后哈希差异 0；当前 `src/main` 1061 个文件仍与成功编译后快照一致，差异 0。本批清单里的 54 个正式文件全部包含在覆盖范围。没有重复运行 Gradle，没有改源码。

详细依据：

- [共享编译承接报告](D:/alianximod/docs/开发记录/分身底座-20261102/compile-shared-adoption-report.md)
- [共享编译承接证据](D:/alianximod/docs/开发记录/分身底座-20261102/compile-shared-adoption-evidence.json)
- [最终命名门禁日志](D:/alianximod/docs/开发记录/分身底座-20261102/naming-gate-final-current.log)：退出码 0，发现 0 条。

历史尝试保留为 `compile-attempt-1-*`、`compile-attempt-2-*`、`compile-attempt-3-*`：第一次因活动源码作者未执行；第二次临时缓存缺 foojay，未进入 Java 编译；第三次使用既有缓存实际编译，报空间切割模块 5 个 API 错误。该模块原作者随后修复，最终共享构建已通过。历史失败日志不删除，也不当作当前构建结果。

没有游戏实测、独立测试、打包或发布。编译与哈希核对不是额外代码正确性复审；本批设计 1/2、实现 2/3。完整交付边界见[首版交付与实测](D:/alianximod/docs/分身底座首版交付与实测.md)。
