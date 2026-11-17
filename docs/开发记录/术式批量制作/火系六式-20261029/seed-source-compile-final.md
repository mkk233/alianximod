先读火种来源编译任务的完整说明，再按其中授权范围执行。本单只做最终编译。先核对实现、复核和停写证据，确认源码已经收束再编译。`compileJava` 退出码 0。编译晚于最后一次源码写入，前后 18 个文件的哈希与作者证据一致。本单不是复核。

## 停写

源码作者 `1f4155a4-e537-4518-a3f7-893f09c5daff` 已在实现报告里停写。复核只读，没有改源码。表和批次记录不写 `src`。第 3 轮复核已经交回并通过，其后没有返修产物。本单不追加复审，不重置设计 2/2、实现 3/3。

最后写入是 `en_us.json`，2026-10-29 12:46:40.4509151。编译前采样 13:08:05，18 条 SHA256 与 mtime 差异为 0。

## 编译

`D:\alianximod` 下执行 `.\gradlew.bat compileJava --offline --no-daemon`。JDK 17.0.6，沿用 `-Xmx3G`，daemon 关闭。

开始 2026-10-29T13:08:23.9470594+08:00，结束 13:08:48.9103912+08:00。`compileJava` 实际执行。BUILD SUCCESSFUL in 24s。32 条待删除 API 警告，没有错误。日志：`docs/开发记录/术式批量制作/火系六式-20261029/seed-source-gradle.log`。

编译后再算，18 个文件仍与证据一致。清单在 `seed-source-compile-hashes.txt`。结果在 `seed-source-compile-results.json`，说明在 `seed-source-compile-report.md`。

`zh_cn.json` 和 `en_us.json` 编译前都能解析成对象。这只是语法检查。

没有打包，没有启动游戏，没有编译或运行测试，没有重跑名称门禁。这次单次编译进程已结束。编译前已有的空闲 Gradle 进程 74860 不是这次启动的，仍在。
