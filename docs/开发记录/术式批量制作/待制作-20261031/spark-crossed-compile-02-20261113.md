# 火苗十字穿插 — 统一编译回执 02

- batchId：`待制作-20261031`
- taskId：`fire_spark_crossed_launch`
- attemptId：`20261113-compile-spark-cross-02`
- 工作区：`D:\alianximod`
- stage：`implementation`
- 操作角色：compile（指定统一编译者）
- 请求模型：`cursor/grok-4.7@500k` · high + Fast。沿用客户端 `service_tier="priority"` 配置；宿主未单独返回后端已独立验证 Fast，本回执不把该配置写成已验证事实。
- 可观察返回身份 / 后端追踪：未暴露，记未知。
- 原生工具：本执行者直接完成，未再派生子代理。子代理标识不适用。
- 授权来源：用户已授权修复火苗（`fire_spark`）转圈问题，改为按发射方向生成箭矢式 `±45°` 十字穿插双面片（`CameraFacingTag.NONE`）并在第一人称清晰可见。前序 `20261113-compile-spark-cross-01` 因并发任务文件编译失败；该文件已由并发任务修复，本项 `20261113-impl-spark-cross-02` 已收束。本单只授予唯一编译：`.\gradlew.bat compileJava --offline --no-daemon`。制作表写回权限为无。
- 输入版本：
  - `D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/spark-crossed-launch-impl-20261113.md`
  - `D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/spark-crossed-launch-impl-02-20261113.md`
  - `D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/spark-crossed-compile-20261113.md`
- 依赖：`20261113-impl-spark-cross-01`（已完成并关闭）、`20261113-impl-spark-cross-02`（已完成并关闭，代理 `01a1d30f-c537-7dd0-8b4f-a5dc7b7110e4`）
- 真实直接父代理：主会话。父方接收时间由主会话收到本 final 后填写，本回执不代填。
- 关联 B：`fire_spark`
- 嵌套读取：无。未派 Flash，未派任何子代理。
- 额度：设计 2/2、实现 3/3，均已用完。本单未追加正式正确性复审，未重置计数。

## 结果

`compileJava` 已实际执行，退出码 0，`BUILD SUCCESSFUL`。编译器报 0 个错误、54 个警告。警告都是已过时或待删除 API，不在本项 6 个目标 Java 文件新增的改动上，本单没有为消警告改源码。

`:compileJava` 本次是执行，不是 `UP-TO-DATE`。另一个执行任务是 `:downloadMcpConfig`；`:extractSrg` 与 `:createMcpToSrg` 为 `UP-TO-DATE`。合计 4 个可执行任务：2 个执行，2 个已是最新。

本单没有修改任何 `src/`、资源、`docs/术式制作表.xlsx`、`记录.md` 或 `实测清单.md`。未执行 `build`、`jar`、`reobf`、`runClient`，未启动游戏。编译通过不代表正式复审通过，也不构成迁待验收条件。

## 六个 Java 文件

时间为东八区本地时间，毫秒精度。下表在编译前核对，编译后再核对一次：字节数、mtime、SHA256 都与 `spark-crossed-launch-impl-02-20261113.md` 一致，且编译前后不变。

| 路径 | 字节 | mtime | SHA256 | 对输入记录 |
| --- | ---: | --- | --- | --- |
| `src/main/java/com/bslx/XueXiMod/visual/facing/CameraFacingRegistry.java` | 1068 | 2026-11-13 11:00:43.585 +08:00 | `0096b8cef6e46244d2ffc6fe66a9b5ba00fc037cab16ee56021fcb31fe40017b` | 一致 |
| `src/main/java/com/bslx/XueXiMod/client/CameraFacingClient.java` | 3187 | 2026-11-13 11:01:36.894 +08:00 | `4f2af5a4b94f569898ec8f9be8c79ccae218ef0cca8ef63fbfb655f867674219` | 一致 |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitEntity.java` | 59101 | 2026-11-13 11:05:21.368 +08:00 | `b2474242c425e955532b4ec4d2de4121d7760d2088cb1bc61d6787d57e7f029b` | 一致 |
| `src/main/java/com/bslx/XueXiMod/carrier/BatchFlightBook.java` | 21788 | 2026-11-13 11:00:53.124 +08:00 | `dea2ab2bf22452133434ff9385446804e3be2fc07647bb9546e8ef53b0c1b23e` | 一致 |
| `src/main/java/com/bslx/XueXiMod/carrier/BatchCarrierVisual.java` | 21162 | 2026-11-13 11:01:03.531 +08:00 | `2af18907d35327adf45f422d8ab5f81b2a99f73e3a91d7a97f1429bfa03b024b` | 一致 |
| `src/main/java/com/bslx/XueXiMod/client/BatchCarrierVisualRenderer.java` | 27963 | 2026-11-13 12:00:28.504 +08:00 | `e9c4e19fbbd9af111b5091e06136fcca7c92767d00575ce1a6f2a5d8a6fb7387` | 一致 |

本项最后一次相关 Java 写入是 `BatchCarrierVisualRenderer.java` 的 2026-11-13 12:00:28.504 +08:00。编译开始于 2026-11-13 12:04:01.801 +08:00，晚于该写入。

## src/main 树

算法与 `spark-crossed-compile-20261113.md` 一致：取 `src/main` 下全部文件（含隐藏文件），按完整路径做 PowerShell `Sort-Object FullName`，相对路径从项目根算并改为正斜杠。每行是 `相对路径\t字节数\t小写SHA256\n`，拼成 UTF-8 无 BOM 后再做 SHA256，结果取小写。

| 时点 | 核对时间 | 文件数 | 总字节 | 树哈希 |
| --- | --- | ---: | ---: | --- |
| 编译前 | 2026-11-13 12:03:40.760 +08:00（完成于 12:03:41.109） | 1391 | 13079384 | `44acfa4454019ebf26b9b7e97d1bc9a741b7fd3728fb1e8eea378c5daa525be6` |
| 编译后 | 2026-11-13 12:04:45.500 +08:00（树哈希完成于 12:04:45.854） | 1391 | 13079384 | `44acfa4454019ebf26b9b7e97d1bc9a741b7fd3728fb1e8eea378c5daa525be6` |

两次相同。本次编译期间 `src/main` 源码零差异。

这与 `20261113-compile-spark-cross-01` 记下的 1384 个文件、13047969 字节、树哈希 `999a38f85e10b096322c3e59204b0fbbbffe121565172a5f5c9d6b49fb893747` 不同。那次快照在 11:12，之后已有 impl-02 对 `BatchCarrierVisualRenderer.java` 的写入，以及本单开工前就已存在的其他 `src/main` 变化。本单没有把那些差异归因成本次编译改动，也没有改它们。

## 占用检查

编译启动前，没有正在写入本项目的 `GradleWrapperMain` 或 `compileJava`。只有 IDEA（PID 33864，`idea64.exe`）拉起的两个空闲 Gradle 8.8 daemon：PID 64032 与 PID 57824，子进程均为 0。命令行是 `org.gradle.launcher.daemon.bootstrap.GradleDaemon 8.8`，不含 `GradleWrapperMain` 或 `compileJava`。它们不是本次编译的写入进程，没有被结束；编译后这两个 PID 仍在。

本次构建按日志另起一个 single-use daemon，并写明构建结束后停止。编译后没有残留的 `GradleWrapperMain` 或 `compileJava` 进程。

## 编译

- 命令全文：`.\gradlew.bat compileJava --offline --no-daemon`
- 工作目录：`D:\alianximod`
- 捕获方式：`cmd.exe /d /c` 运行上述命令，把标准输出和标准错误合并写入日志。没有改命令，没有加其他 Gradle 任务。
- 开始：2026-11-13 12:04:01.801 +08:00
- 结束：2026-11-13 12:04:26.039 +08:00
- 壁钟耗时：24238 ms。日志自身写 `BUILD SUCCESSFUL in 23s`。
- 退出码：0
- 是否晚于最后一次相关写入：是。开始时间晚于 `BatchCarrierVisualRenderer.java` 的 2026-11-13 12:00:28.504 +08:00。
- Java：日志写明 Java 17.0.6，JVM 17.0.6+9-LTS-190（Oracle Corporation），Arch amd64。
- 任务：`:downloadMcpConfig` 执行，`:extractSrg` UP-TO-DATE，`:createMcpToSrg` UP-TO-DATE，`:compileJava` 执行后成功。`compileJava` 不是 UP-TO-DATE。
- 命令含 `--offline`。日志显示 `:downloadMcpConfig` 被执行且随后编译成功；本回执不把这一行改写成联网下载或已跳过。
- 错误：0 个。警告：按 CP936 解码后，结尾为「54 个警告」。没有「个错误」汇总行。

## 日志

- 路径：`D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/spark-crossed-compile-02-20261113.log`
- 字节：18959
- mtime：2026-11-13 12:04:25.929 +08:00
- SHA256：`336ca17eb625aaf83263190ca6545b17e892d820dbd4ae72a24ff85fc676aa7f`
- 编码：日志是 cmd 捕获的原始字节，中文诊断为控制台代码页 CP936/GBK。上述 SHA256 对的是这些原始字节，不是转码后的文本。本单没有回写或改编码这份日志。

## 状态分界

- 功能：前序实现 `20261113-impl-spark-cross-01` 与 `20261113-impl-spark-cross-02` 已收束。本单只编译，不重新实现。
- 美术、用户确认、正式接入：本单未核对，不改变其状态。
- 游戏实测：未做。
- 制作表：未写。`docs/术式制作表.xlsx` 仍为 177595 字节，mtime 2026-11-13 10:45:04.840 +08:00，SHA256 `b0fe9b4546d0ed446e71c2007c24243f35eeadad72553ccd51b16a55635b934c`。编译前后相同。
- `记录.md`：未写。`docs/开发记录/术式批量制作/待制作-20261031/记录.md` 仍为 29524 字节，mtime 2026-11-13 03:48:13.623 +08:00，SHA256 `ef51e0c7b0822bc9032be724508658f0ce346612ba78329b74a20c1a5fc26f8d`。编译前后相同。
- `实测清单.md`：未写。`docs/开发记录/术式批量制作/待制作-20261031/实测清单.md` 仍为 32561 字节，mtime 2026-11-13 03:47:16.305 +08:00，SHA256 `6652a85c9c3387d4f03714011908ead866b75c96cafc598c03923e10ce119a6a`。编译前后相同。

## 收束与额度

本单写入已收束。只写了本回执和 `spark-crossed-compile-02-20261113.log`，加上 `compileJava` 正常产生的 `build/` 产物。没有其他作者持有这两份记录路径。文件句柄在本 final 返回前关闭。

设计额度 2/2、实现额度 3/3 在开工前已用完。本编译没有借编译名义新增正式正确性复审，没有重置计数。

审批拒绝：无。编译失败：无。阻塞：无。统一编译已通过。本执行者不写表、不改源码。

下一步：交回主会话后，由主会话派唯一制作表写回者执行写表与批次记录同步。
