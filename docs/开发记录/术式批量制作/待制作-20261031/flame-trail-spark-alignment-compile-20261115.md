# 火苗留焰锁定发射方向最终编译回执

- batchId：`待制作-20261031`
- taskId：`flame_trail_spark_alignment`
- attemptId：`20261115-compile-alignment-01`
- 工作区：`D:/alianximod`
- stage：`delivery`
- 操作角色：本批本次唯一指定最终编译者
- 请求模型 / 后端模型：宿主未返回，记未知
- 可观察身份：`CODEX_SESSION_ID=01a1ce06-b681-7c90-b11d-260d3c169911`，`CODEX_THREAD_ID=01a1db83-1b2f-7182-b018-f091a869960f`
- 真实直接父代理：当前主会话，UUID 未知，不编造。父方接收时间由主会话收到本 final 后填写，本回执不代填。
- 原生工具：本执行者直接编译，未派子代理。下属 Flash 不适用。
- 关联实现：`01a1db6e-43ac-7c22-8a84-56c2a5d6c4ac`（`20261115-impl-alignment-01`）与 `01a1db78-ce2c-7fd2-ba27-45a6ebda721a`（`20261115-impl-alignment-02`）均已 final 并 close。
- 输入回执：`docs/开发记录/术式批量制作/待制作-20261031/flame-trail-spark-alignment-20261115.md`、`flame-trail-spark-alignment-02-20261115.md`
- 额度：设计 `2/2`、实现 `3/3` 已用完。本次编译不计复审，未追加正式复审，未重置计数。

## 根因修正

本单没有改代码，以下只记录已经收束的修正。第一轮给火苗留焰加了 `SparkMeshAnchor`，偏移仍是 `0.245`，但火苗取生成时锁定的发射方向，留焰取每段当前运动切线。喷涌、追踪等改变方向后，留焰会跟着当前切线偏走。

第二轮让火苗跟踪器在挂接时读取 `TechniqueHitEntity.launchDirection()`，把生成当刻的单位发射方向写入 `SparkLaunchX/Y/Z`，每个留焰采样复制同一组值。采样位置和 `VisualTangentX/Y/Z` 仍由当前段决定。渲染时当前切线只用于面片朝向和沿火径展开；`SparkMeshAnchor` 为真时，才用锁定发射方向重建与火苗相同的上轴，并沿该上轴反向平移 `0.245`。火球、陨岩等其它宿主不写标记，也不写这组方向。

## 占用

编译前没有本工作区的 `GradleWrapperMain`，也没有游戏客户端。唯一 Java 进程是 PID `23744`，`org.gradle.launcher.daemon.bootstrap.GradleDaemon 8.8`，父进程 `idea64.exe`，创建于 `2026-11-15 02:38:07.419`，无子进程；日志只在刷新 daemon 注册表，属于 IDEA 空闲 daemon。`D:/alianximod/.gradle/8.8/executionHistory/executionHistory.lock`、`fileHashes/fileHashes.lock`、`buildOutputCleanup/buildOutputCleanup.lock` 和用户目录 `daemon/8.8/registry.bin.lock` 当时都可独占打开。因此没有并行构建。未终止任何用户进程，也未停止这个空闲 daemon。编译后仍只有 PID `23744`。

追加前批次记录没有这两次实现的未收束占用条目。两位作者已由任务和其回执标为 final、close、写入收束。三个目标文件的最后写入是 `2026-11-15 03:20:11.222`，编译开始晚于该时间，编译窗口内 `src/main` 树哈希未变。

## 源码

比较忽略大小写。编译前、命令启动前的即时复核、编译后三次一致。

| 路径 | 字节 | mtime | SHA256 |
| --- | ---: | --- | --- |
| `src/main/java/com/bslx/XueXiMod/carrier/BatchCarrierVisual.java` | 24518 | 2026-11-15 03:20:00.158 +08:00 | `8330A16E9A6796FE074584B61EB4F7F1E4A12CEB5DDCD80336597ACD6FCA8220` |
| `src/main/java/com/bslx/XueXiMod/carrier/BatchFlameTrail.java` | 10072 | 2026-11-15 03:20:05.346 +08:00 | `6ADC888B59782CF03DDDD8040FBB4A26FD7ACD33B3E5C3379B3409A634DBAA52` |
| `src/main/java/com/bslx/XueXiMod/client/BatchCarrierVisualRenderer.java` | 33055 | 2026-11-15 03:20:11.222 +08:00 | `692911AE7727A2A4BED3001DB30F03400F7344B532B7997A81A173A1C8A5E798` |

`src/main` 前后都是 1456 个文件、13766184 字节。清单按完整路径排序，相对路径用正斜杠，每行是路径、字节、小写 SHA256，制表符分隔并以 LF 结尾，再对 UTF-8 无 BOM 字节做 SHA256。树哈希前后都是 `351eb181c4eb75b138b9370a8b2fce8caeaba71026d408a240c627f62bf408d4`。

## 编译

- 命令全文：`./gradlew.bat compileJava --offline --no-daemon`
- 工作目录：`D:/alianximod`
- 次数：1
- 开始：2026-11-15 03:25:54.673 +08:00
- 结束：2026-11-15 03:26:22.708 +08:00
- 壁钟：28035 毫秒
- 退出码：0
- 是否晚于最后一次相关写入：是。最后写入是 `BatchCarrierVisualRenderer.java` 的 2026-11-15 03:20:11.222 +08:00。
- 关键行：`Java: 17.0.6`；`:downloadMcpConfig`；`:extractSrg UP-TO-DATE`；`:createMcpToSrg UP-TO-DATE`；`:compileJava`；`BUILD SUCCESSFUL in 26s`；`4 actionable tasks: 2 executed, 2 up-to-date`。
- `:compileJava` 本次实际执行，不是 `UP-TO-DATE`。`26s` 是 Gradle 取整文本，起止以上面的壁钟为准。
- 日志按 CP936 可解码。内容是 54 个既有 `[removal]` 过时警告，没有 `错误` 行，三个目标文件没有出现在警告位置。本单未改这些旧警告。

编译前三个 class 仍是 2026-11-15 01:45:58 的旧产物，早于 03:20 的最终源码。编译后：

| class | 字节 | mtime | SHA256 |
| --- | ---: | --- | --- |
| `build/classes/java/main/com/bslx/XueXiMod/carrier/BatchCarrierVisual.class` | 21222 | 2026-11-15 03:26:16.088 +08:00 | `92FA831C46847D7B185B479366B3676BFB66C34CDDAB5A1FD4304C13C8381D74` |
| `build/classes/java/main/com/bslx/XueXiMod/carrier/BatchFlameTrail.class` | 11238 | 2026-11-15 03:26:16.110 +08:00 | `82BD178A1D2D357A23C5F886A88CFB70E6C6E880B9B9126ADB6DBB7B0FA2681E` |
| `build/classes/java/main/com/bslx/XueXiMod/client/BatchCarrierVisualRenderer.class` | 22055 | 2026-11-15 03:26:16.949 +08:00 | `E8CB9135720EF557BD73417284FC48AB18CB686756DFB671F53A9C2FDEE4DF9D` |

这三个 class 的 mtime 晚于最终源码，并落在 03:25:54.673 至 03:26:22.708 之间，所以本次成功编译覆盖的是上面三个最终源码哈希。本单没有手工改 `build`，没有跑 `build`、`jar`、`reobf` 或 `runClient`。

## 日志

- 路径：`docs/开发记录/术式批量制作/待制作-20261031/flame-trail-spark-alignment-compile-20261115.log`
- 字节：18959
- mtime：2026-11-15 03:26:22.141 +08:00
- SHA256：`3935A9D80855060E53E52CF0CD2E9638C66628B9E6A80DC3969E9D76B908E009`
- 编码：cmd 捕获的原始字节。英文行是 ASCII，javac 中文行是 CP936。本单没有回写或转码。

## 状态

- 功能：两轮实现已收束。本单只编译。
- 美术、用户确认、正式接入：本单未重做，也未因此迁表。
- 游戏验证：未做。
- 制作表：未读未写，未迁移。
- 独立测试阶段：没有。
- 记录基线：追加前 `记录.md` 为 77457 字节、376 行、SHA256 `076E0378B1DC7F692B9509E5F616971CCBD28E60073A90E62720D280D0C603B3`、LastWriteTime `2026-11-15 01:51:26.238 +08:00`。只在独占句柄内确认该哈希后追加。

## 实测建议

- 编号 / 名称：火苗留焰。直射、斜射、喷涌。
- 前置：创造栏或给予测试法杖，装配火苗；喷涌测试时把喷涌放在火苗前面。
- 步骤：分别做直射、斜射、喷涌，并在飞行中转动镜头。
- 预期：留焰贴合火苗，不随相机转。用户反馈：（留空待填）

## 收束

本单写入已收束。只写本回执、编译日志，并追加批次记录末尾。日志在编译时已关闭。没有其他作者持有这三份路径。未派子代理，未做 git 操作。

设计 `2/2`、实现 `3/3` 在开工前已用完。未借编译追加复审。审批拒绝：无。编译失败：无。阻塞：无。

下一步：交回主会话。不迁表，不进入独立测试阶段。