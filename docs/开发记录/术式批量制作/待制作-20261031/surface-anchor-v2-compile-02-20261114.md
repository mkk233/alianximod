# 地表定位 v2 — 返修后统一编译回执

- batchId：`待制作-20261031`
- taskId：`rule_surface_anchor_v2_compile`
- attemptId：`20261114-compile-surface-anchor-v2-02`
- 工作区：`D:\alianximod`
- stage：`delivery`
- 操作角色：compile（本批本次唯一指定统一编译执行者）
- 请求模型：任务单指定本执行者直接编译。宿主未返回可核验的后端模型名，记未知。
- 可观察返回身份 / 后端追踪：未暴露，记未知。
- 原生工具：本执行者直接完成，未再派生子代理。子代理标识不适用。
- 授权来源：用户 2026-11-14 原话「你先继续，我忘了地面定位之前你给出的版本我还没说采用，之前那个版本的效果可以接入」及流程第 7 节统一编译规范。前序返修 `20261114-integrate-surface-anchor-v2-02` 已修复 `BatchCarrierVisual.java:86` 的私有访问报错。本单只授予唯一编译：`.\gradlew.bat compileJava --offline --no-daemon`。源码修改与制作表写回权限为无。
- 输入版本：
  - `D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/surface-anchor-v2-integration-report-20261114.md`
  - `D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/surface-anchor-v2-integration-report-02-20261114.md`
  - `D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/surface-anchor-v2-art-archive-20261114.md`
- 依赖：`20261114-integrate-surface-anchor-v2-01`、`20261114-integrate-surface-anchor-v2-02`、`20261114-art-surface-anchor-v2-archive-01`（均已收束关闭）
- 真实直接父代理：主会话。父方接收时间由主会话收到本 final 后填写，本回执不代填。
- 关联 B：`rule_surface_anchor`（地表定位）
- 嵌套读取：无。未派 Flash，未派任何子代理。
- 额度：设计 `2/2`、实现 `3/3`，均已用完。统一编译不计复审轮次，未追加正式正确性复审，未重置计数。

## 结果

`compileJava` 已实际执行授权命令，退出码 0。Gradle 记本次 `:compileJava` 为 `UP-TO-DATE`，没有在本次进程里重新运行 javac，也没有改写 4 个目标 class。本单没有修改任何源码或资源，没有边编边改，没有加 `--rerun-tasks` 或其他 Gradle 任务。

## 六个目标文件

时间为东八区本地时间，毫秒精度。下表先于 2026-11-14 04:53:14.227 核对，编译启动前再核对一次，编译后再核对一次。三次字节、mtime、SHA256 都相同。期望值：`BatchCarrierVisual.java` 以本单为准，其余 5 项与 `surface-anchor-v2-compile-20261114.md` 一致。比较忽略大小写。

| 路径 | 字节 | mtime | SHA256 | 对期望 | 编译后 |
| --- | ---: | --- | --- | --- | --- |
| `src/main/java/com/bslx/XueXiMod/carrier/BatchVisualTexture.java` | 1188 | 2026-11-14 04:33:09.532 +08:00 | `2565e67152aa4b5743ddf50734e28886538756d038551e8296786858d8ff64e4` | 一致 | 零差异 |
| `src/main/java/com/bslx/XueXiMod/carrier/BatchTriggerVisuals.java` | 4306 | 2026-11-14 04:33:09.540 +08:00 | `049f7bc0eef4f291143b492e977d2d3bb15978bccc34bfb48b8759c4ad6e4226` | 一致 | 零差异 |
| `src/main/java/com/bslx/XueXiMod/carrier/BatchCarrierVisual.java` | 21477 | 2026-11-14 04:48:11.855 +08:00 | `f60325e434a1a267798a6be501d20f63348f917ea5264adef9031e7528c4bc48` | 一致 | 零差异 |
| `src/main/java/com/bslx/XueXiMod/client/BatchCarrierVisualRenderer.java` | 32520 | 2026-11-14 04:33:45.300 +08:00 | `b848c0a67c26fc06ea09db398eb8c7b620b556a80718f1f04af7e608672696be` | 一致 | 零差异 |
| `src/main/resources/assets/xueximod/textures/effect/surface_anchor_v2/surface_anchor.png` | 4741 | 2026-11-14 00:48:01.980 +08:00 | `4434bdc7b0311e863277e9bd23761139e77489ad6b548fbb48ac202c4fb35a20` | 一致 | 零差异 |
| `src/main/resources/assets/xueximod/textures/effect/surface_anchor_v2/surface_anchor.png.mcmeta` | 64 | 2026-11-12 13:07:16.892 +08:00 | `b2caa414505e1c556cf68460294471d97198a50f3d69ed1fd9b717510c0d6f4a` | 一致 | 零差异 |

六个目标里最后一次写入是 `src/main/java/com/bslx/XueXiMod/carrier/BatchCarrierVisual.java` 的 2026-11-14 04:48:11.855 +08:00。编译开始于 2026-11-14 04:54:01.291 +08:00，晚于该写入。

## src/main 树

算法与 `surface-anchor-v2-compile-20261114.md` 一致：取 `src/main` 下全部文件（含隐藏文件），按完整路径做 PowerShell `Sort-Object FullName`，相对路径从项目根算并改为正斜杠。每行是相对路径、字节数、小写 SHA256，以制表符分隔并以 LF 结尾，拼成 UTF-8 无 BOM 后再做 SHA256，结果取小写。

| 时点 | 核对时间 | 文件数 | 总字节 | 树哈希 |
| --- | --- | ---: | ---: | --- |
| 编译前 | 2026-11-14 04:53:14.763 +08:00（完成于 2026-11-14 04:53:15.308 +08:00） | 1454 | 13762505 | `50dc548bfa38d8a3771d07b9b102bfb9c491e7528477839758af64ae3bb41ca6` |
| 编译后 | 2026-11-14 04:54:44.029 +08:00（树哈希完成于 2026-11-14 04:54:44.529 +08:00） | 1454 | 13762505 | `50dc548bfa38d8a3771d07b9b102bfb9c491e7528477839758af64ae3bb41ca6` |

两次相同，逐项比较没有差异。编译启动前对 6 个目标文件做了立即复核，与上表一致。编译后整树与 04:53:15.308 的清单逐项相同，因此 04:53:15.308 至 04:54:44.529 之间 `src/main` 没有变化。本单没有为此修改源码。

整树最晚修改文件仍是 `src/main/java/com/bslx/XueXiMod/carrier/BatchCarrierVisual.java`，21477 字节，mtime 2026-11-14 04:48:11.855 +08:00。编译开始晚于该时间。

## 占用检查

编译启动前，没有正在运行本工作区的 `GradleWrapperMain`。观察到的 Gradle 进程：67096 DAEMON。命令行不含 `GradleWrapperMain`，也不含本工作区路径。空闲 daemon 未被结束。`docs/术式制作表.xlsx` 被其他进程占用，未因此中断预检或编译；编译不读该表。
编译结束后观察到的 Gradle 进程：67096 DAEMON。仍不是本工作区的 `GradleWrapperMain`，未结束它。

## 编译

- 命令全文：`.\gradlew.bat compileJava --offline --no-daemon`
- 工作目录：`D:\alianximod`
- 捕获方式：`cmd.exe /d /c` 运行上述命令，把标准输出和标准错误合并写入日志。没有改命令，没有加其他 Gradle 任务。
- 开始：2026-11-14 04:54:01.291 +08:00
- 结束：2026-11-14 04:54:14.077 +08:00
- 壁钟耗时：12785 ms。
- 退出码：0
- 是否晚于最后一次相关写入：是。开始时间对照 `BatchCarrierVisual.java` 的 2026-11-14 04:48:11.855 +08:00，也晚于当时整棵 `src/main` 树的最晚修改。
- PowerShell：7.6.9
- 日志关键行：
  - `To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.8/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.`
  - `Daemon will be stopped at the end of the build`
  - `Java: 17.0.6, JVM: 17.0.6+9-LTS-190 (Oracle Corporation), Arch: amd64`
  - `> Task :downloadMcpConfig`
  - `> Task :extractSrg UP-TO-DATE`
  - `> Task :createMcpToSrg UP-TO-DATE`
  - `> Task :compileJava UP-TO-DATE`
  - `BUILD SUCCESSFUL in 12s`
  - `4 actionable tasks: 1 executed, 3 up-to-date`
- javac 报错行：无。本次日志没有错误，也没有警告。

`BUILD SUCCESSFUL in 12s` 是 Gradle 自己的取整文本。本回执的起止时间以上面的壁钟为准。本次实际执行的任务是 `:downloadMcpConfig`；`:compileJava` 未重跑。

## 四个 class

这 4 个文件在本次命令启动前已经存在。编译后字节、mtime、SHA256 都没有变化，不记成本次命令新生成的产物。它们的 mtime 晚于已核对的 `BatchCarrierVisual.java` 写入 04:48:11.855，早于本次编译开始 04:54:01.291。本单没有追查是谁在 04:48 写出这些 class。

| 路径 | 编译前 mtime | 编译前字节 | SHA256 | 编译后 mtime | 编译后字节 | 本次是否改写 |
| --- | --- | ---: | --- | --- | ---: | --- |
| `build/classes/java/main/com/bslx/XueXiMod/carrier/BatchVisualTexture.class` | 2026-11-14 04:48:47.325 +08:00 | 2375 | `7d7f1930ceff4d40d10d74d5ce87508b7e7688733af7d9afaa443009fb085502` | 2026-11-14 04:48:47.325 +08:00 | 2375 | 否 |
| `build/classes/java/main/com/bslx/XueXiMod/carrier/BatchTriggerVisuals.class` | 2026-11-14 04:48:47.444 +08:00 | 5203 | `41c95840bc198c3a841357552c1cd3a1aa2c7365a557e2675a2905bd12f0b944` | 2026-11-14 04:48:47.444 +08:00 | 5203 | 否 |
| `build/classes/java/main/com/bslx/XueXiMod/carrier/BatchCarrierVisual.class` | 2026-11-14 04:48:47.319 +08:00 | 19598 | `ce208448fcc0d2a1f0e391ac0bafd618bda11ba3fdf8a40d034e826a9e482121` | 2026-11-14 04:48:47.319 +08:00 | 19598 | 否 |
| `build/classes/java/main/com/bslx/XueXiMod/client/BatchCarrierVisualRenderer.class` | 2026-11-14 04:48:48.192 +08:00 | 21559 | `8d9a607591204f0fea5260f7793acb5d1dd86c5e55d192fbaab81a2980e9ea1b` | 2026-11-14 04:48:48.192 +08:00 | 21559 | 否 |

class 文件不在 `src/main` 树哈希内。

## 编译窗口内的其他文件

2026-11-14 04:54:01.291 至 04:54:20.000 之间，项目根下只有本日志和 `.gradle` 的构建状态文件改变 mtime。没有源码、资源或上述 4 个 class 被改写。这些 `.gradle` 文件只来自已授权命令，不记成源码写入。

## 日志

- 路径：`D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/surface-anchor-v2-compile-02-20261114.log`
- 字节：577
- mtime：2026-11-14 04:54:14.017 +08:00
- SHA256：`e8eaa8fd4d35af51c23c44fad657df6a2a368562a7b5645b77290fad13b5e68f`
- 编码：日志是 cmd 捕获的原始字节，可以作为无 BOM 的 UTF-8 严格解码。本单没有回写或改编码这份日志。

## 状态分界

- 功能：前序接入与返修已收束。本单只编译，不重新实现。
- 美术、用户确认、正式接入：本单只核对已接入的 `surface_anchor.png` 与 `surface_anchor.png.mcmeta` 字节和哈希，没有重做美术或确认。
- 游戏实测：未做。
- 制作表：未写。
- `docs/术式制作表.xlsx`：未写。编译后只读哈希失败，原因是其他进程正在使用该文件。因此不把它记成本次编译失败，也没有当前字节或哈希可报。
- `docs/开发记录/术式批量制作/待制作-20261031/记录.md`：未写。只读结果与上一份编译回执相同。字节 42534，mtime 2026-11-13 13:08:31.995 +08:00，SHA256 `6a9cd933136661640a92d793ed4546db49605e34cfd61d41748b3c5f8c010bb5`。
- `docs/开发记录/术式批量制作/待制作-20261031/实测清单.md`：未写。只读结果与上一份编译回执相同。字节 33137，mtime 2026-11-13 13:08:10.663 +08:00，SHA256 `c17e113f2120b24290472d87c3e7610fe36d608de742210243b7f6b3936802d2`。

## 收束与额度

本单写入已收束。只写了本回执和 `surface-anchor-v2-compile-02-20261114.log`。Gradle 自己的 `.gradle` 状态更新只来自这条已授权命令，不记成源码写入。没有其他作者持有这两份记录路径。文件句柄在本 final 返回前关闭。

设计额度 2/2、实现额度 3/3 在开工前已用完。本编译没有借编译名义新增正式正确性复审，没有重置计数。

审批拒绝：无。编译失败：无。阻塞：无。统一编译命令退出码为 0。本执行者不写表、不改源码。

下一步：交回主会话。
