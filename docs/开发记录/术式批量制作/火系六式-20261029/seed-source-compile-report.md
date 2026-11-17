# 火种来源：最终编译

本单只编译。不是实现复审，不重置设计 2/2 或实现 3/3。第 3 轮只读复核已经交回并通过，其后没有作者返修产物。本单不再审那一轮，也不把编译写成复核通过。没有独立测试阶段，没有跑测试，没有改源码、语言、制作表、`记录.md`、既有报告或资源库索引。没有打包，没有启动游戏。

## 停写与所有权

源码作者是 `1f4155a4-e537-4518-a3f7-893f09c5daff`。`seed-source-implementation-report.md` 声明源码停写，待统一编译。复核只读，`seed-source-review-final.md` 写明没有改源码。表和 `记录.md` 仍由 `d6f84d0b-4ea1-4368-a576-8acc20cc2afd` 写，不写 `src`。本会话只写编译证据。

最后一次源码写入是 `en_us.json`，mtime 2026-10-29 12:46:40.4509151。编译开始于 13:08:23，晚于这次写入。

编译前 18 个证据文件的 SHA256 和 mtime 与 `seed-source-implementation-evidence.json` 全部一致，差异 0。采样 2026-10-29T13:08:05.7717402+08:00。没有发现第 3 轮之后的返修文件。

## 编译

命令：`.\gradlew.bat compileJava --offline --no-daemon`

目录：`D:\alianximod`。`JAVA_HOME=C:\Program Files\Java\jdk-17`，`GRADLE_USER_HOME=C:\Users\baishang\.gradle`，沿用 `-Xmx3G`。日志：`docs/开发记录/术式批量制作/火系六式-20261029/seed-source-gradle.log`。

开始 2026-10-29T13:08:23.9470594+08:00，结束 2026-10-29T13:08:48.9103912+08:00，退出码 0。Java 17.0.6。`compileJava` 实际执行，不是 UP-TO-DATE。BUILD SUCCESSFUL in 24s。4 个任务里 2 个执行、2 个 UP-TO-DATE。32 条待删除 API 警告，没有错误。警告没有让构建失败，本单不改这些调用。

这次命令使用单次进程，结束时自行停止。编译前已经有一个空闲的 Gradle 8.8 daemon，进程 74860，不是这次启动的，这次没有关掉它。

编译后再算 18 个文件，SHA256 仍与证据一致，差异 0。清单在 `seed-source-compile-hashes.txt`。

## 语言 JSON

编译前用 `ConvertFrom-Json` 解析了 `zh_cn.json` 和 `en_us.json`，两份都能读成对象。这只是语法检查，不是功能测试，也不是游戏验证。

## 未跑

没有 `build`、`jar`、`reobf`、`runClient`，没有启动游戏，没有编译或运行测试，没有重跑名称门禁。作者此前的名称门禁结果不在本单重做。

本单写入到此停止。
