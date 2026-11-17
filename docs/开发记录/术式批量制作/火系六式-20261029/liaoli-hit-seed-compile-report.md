# 燎里命中火环与火种常驻显示：最终编译

本单只编译，同时覆盖性能优化批已经冻结的 11 个文件。不是正确性复审。设计 2/2、实现 3/3 不重置。本阶段额度已用完，返修结果未追加复审。没有独立测试。编译通过不是游戏实测，也不能据此迁验收。

## 所有权与停写

本批源码作者是 `23ef4660-da4b-47a8-9341-8ec236ec5cc3`。实现报告写明证据写完后停止写入。原作者 `1f4155a4-e537-4518-a3f7-893f09c5daff` 只读返回，没有写这次源码。表作者 `d6f84d0b-4ea1-4368-a576-8acc20cc2afd` 的记录任务已停止，不写 `src`。主会话的火苗图已停写。

性能批 A/B/C 三名作者和只读复核都已停止。`记录.md` 写明源码停写、检查收束，并由本编译者执行 `compileJava`，该批自己不另开 Gradle。复核是 `review-1-final.md`，会话 `fb3388f6-e1a6-4eca-a6f1-09f97bd36788`。11 个文件的冻结清单是 `frozen-source-metadata.json`。该批此前没有成功的统一编译可引用，所以这次实际编译。

领域附魔底座和火种共享燃料的源码作者记录都已收束，早于这次写入。线程摘要里仍标为进行中的是两个调度会话，不是源码占用。空闲 daemon 73124 没有被结束，当时也没有正在进行的构建。

最后一次 `src/main` 写入是 `StillSheetPlayback.java`，2026-10-30 01:57:10.7875292。编译开始于 02:15:29，晚于这次写入。

## 编译前哈希

采样 2026-10-30T02:15:17.7514505+08:00。本批 10 个 Java、1 张火苗 PNG，以及证据里列出的 4 个未改文件，SHA256 和最后写入时间与 `liaoli-hit-seed-implement-evidence.json` 一致。性能批 11 个文件的 SHA256 与冻结清单一致，最后写入时间也与清单里的本地时间一致。

运行时 `textures/effect/fire_seed/fire_seed_flame.png` 与正式归档 `docs/资源库/特效/火焰/火种常驻火苗_v1/fire_seed_flame.png` 都是 `106DD7C62A1E360C859FB598B320EBCF10A40568555C763465023380E5DE800B`。

## 编译

命令：`.\gradlew.bat compileJava --offline --no-daemon`

目录：`D:\alianximod`。`JAVA_HOME=C:\Program Files\Java\jdk-17`，`GRADLE_USER_HOME=C:\Users\baishang\.gradle`，沿用 `-Xmx3G`。日志：`docs/开发记录/术式批量制作/火系六式-20261029/liaoli-hit-seed-gradle.log`。

开始 2026-10-30T02:15:29.2151964+08:00，结束 2026-10-30T02:15:55.4174849+08:00，退出码 0。`compileJava` 实际执行，不是 UP-TO-DATE。BUILD SUCCESSFUL in 25s。4 个任务里 2 个执行、2 个 UP-TO-DATE。33 条待删除 API 警告，没有错误。警告没有让构建失败，本单不改这些调用。

这次单次进程已经结束。空闲 daemon 73124 仍在。

## 编译后

`src/main` 共 774 个文件。编译前后逐行清单相同，内容差异 0。清单是 `liaoli-hit-seed-compile-src-main-before.txt` 和 `liaoli-hit-seed-compile-src-main-after.txt`。这轮覆盖双方最终字节。

## 未跑

没有运行测试，没有 `build`、`jar`、`reobf`、`runClient`，没有启动游戏，没有重跑名称门禁，没有做第二次代码审查。

本单写入到此停止，并释放这次编译占用。不自行写表。
