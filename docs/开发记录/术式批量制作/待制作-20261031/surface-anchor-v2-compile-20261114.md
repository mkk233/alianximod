# 地表定位 v2 — 统一编译回执

- batchId：`待制作-20261031`
- taskId：`rule_surface_anchor_v2_compile`
- attemptId：`20261114-compile-surface-anchor-v2-01`
- 工作区：`D:\alianximod`
- stage：`delivery`
- 操作角色：compile（本批本次指定统一编译执行者）
- 请求模型：任务单指定本执行者直接编译。宿主未返回可核验的后端模型名，记未知。
- 可观察返回身份 / 后端追踪：未暴露，记未知。
- 原生工具：本执行者直接完成，未再派生子代理。子代理标识不适用。
- 授权来源：用户确认原话「你先继续，我忘了地面定位之前你给出的版本我还没说采用，之前那个版本的效果可以接入」及流程第 7 节统一编译规范。本单只授予唯一编译：`.\gradlew.bat compileJava --offline --no-daemon`。制作表写回与源码修改权限为无。
- 输入版本：
  - `D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/surface-anchor-v2-integration-report-20261114.md`
  - `D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/surface-anchor-v2-art-archive-20261114.md`
- 依赖：`20261114-integrate-surface-anchor-v2-01`（已完成）、`20261114-art-surface-anchor-v2-archive-01`（已完成）
- 真实直接父代理：主会话。父方接收时间由主会话收到本 final 后填写，本回执不代填。
- 关联 B：`rule_surface_anchor`（地表定位）
- 嵌套读取：无。未派 Flash，未派任何子代理。
- 额度：设计 2/2、实现 3/3，均已用完。统一编译不计复审轮次，未追加正式正确性复审，未重置计数。

## 结果

04:42 的预检在启动 Gradle 前因 `docs/术式制作表.xlsx` 被其他进程独占而中断，没有执行 `gradlew`。当时只读到 `src/main` 为 1454 个文件、13760478 字节、树哈希 `f1430750511f67006e5f876982d14f3bbcff3d2038cd17d407ddbacaf97f83aa`。下面的时间、退出码和树哈希都来自随后这一次实际编译。

`compileJava` 已实际执行授权命令，退出码 1。本单没有修改任何源码或资源。失败只记录，不边编边改。

## 六个目标文件

时间为东八区本地时间，毫秒精度。下表在编译前核对，编译后再核对一次。期望值来自接入回执 `surface-anchor-v2-integration-report-20261114.md`，比较时忽略大小写。

| 路径 | 字节 | mtime | SHA256 | 对接入回执 | 编译后 |
| --- | ---: | --- | --- | --- | --- |
| `src/main/java/com/bslx/XueXiMod/carrier/BatchVisualTexture.java` | 1188 | 2026-11-14 04:33:09.532 +08:00 | `2565e67152aa4b5743ddf50734e28886538756d038551e8296786858d8ff64e4` | 一致 | 零差异 |
| `src/main/java/com/bslx/XueXiMod/carrier/BatchTriggerVisuals.java` | 4306 | 2026-11-14 04:33:09.540 +08:00 | `049f7bc0eef4f291143b492e977d2d3bb15978bccc34bfb48b8759c4ad6e4226` | 一致 | 零差异 |
| `src/main/java/com/bslx/XueXiMod/carrier/BatchCarrierVisual.java` | 21502 | 2026-11-14 04:35:32.426 +08:00 | `2bf108f5770e7509880ce9cecb847a3b890d7037fcbd4240758012a615f45efd` | 一致 | 零差异 |
| `src/main/java/com/bslx/XueXiMod/client/BatchCarrierVisualRenderer.java` | 32520 | 2026-11-14 04:33:45.300 +08:00 | `b848c0a67c26fc06ea09db398eb8c7b620b556a80718f1f04af7e608672696be` | 一致 | 零差异 |
| `src/main/resources/assets/xueximod/textures/effect/surface_anchor_v2/surface_anchor.png` | 4741 | 2026-11-14 00:48:01.980 +08:00 | `4434bdc7b0311e863277e9bd23761139e77489ad6b548fbb48ac202c4fb35a20` | 一致 | 零差异 |
| `src/main/resources/assets/xueximod/textures/effect/surface_anchor_v2/surface_anchor.png.mcmeta` | 64 | 2026-11-12 13:07:16.892 +08:00 | `b2caa414505e1c556cf68460294471d97198a50f3d69ed1fd9b717510c0d6f4a` | 一致 | 零差异 |

六个目标里最后一次写入是 `src/main/java/com/bslx/XueXiMod/carrier/BatchCarrierVisual.java` 的 2026-11-14 04:35:32.426 +08:00。编译开始于 2026-11-14 04:44:51.256 +08:00，晚于该写入。

## src/main 树

算法与 `spark-crossed-compile-03-20261113.md` 一致：取 `src/main` 下全部文件（含隐藏文件），按完整路径做 PowerShell `Sort-Object FullName`，相对路径从项目根算并改为正斜杠。每行是相对路径、字节数、小写 SHA256，以制表符分隔并以 LF 结尾，拼成 UTF-8 无 BOM 后再做 SHA256，结果取小写。

| 时点 | 核对时间 | 文件数 | 总字节 | 树哈希 |
| --- | --- | ---: | ---: | --- |
| 编译前 | 2026-11-14 04:44:50.569 +08:00（完成于 2026-11-14 04:44:51.115 +08:00） | 1454 | 13761488 | `13ab62652635e0a62de59e98a4bb29ce48020e513c810aa7cbde2c41bf2c09e0` |
| 编译后 | 2026-11-14 04:45:16.645 +08:00（树哈希完成于 2026-11-14 04:45:16.987 +08:00） | 1454 | 13761846 | `e567be39aec8f66083ba9d71cc567b80a958b5771e5be11288630cc21ad08f86` |

两次不相同，或逐项比较有 1 处内容差异。本单没有为此修改源码。
- `CHANGED src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseDefinitions.java`

整树最晚修改文件是 `src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseDefinitions.java`，32822 字节，mtime 2026-11-14 04:44:36.018 +08:00。编译开始晚于该时间。

## 占用检查

编译启动前，没有正在写入本项目的 `GradleWrapperMain`。观察到的 Gradle 进程：67096|DAEMON。空闲 daemon 未被结束。
编译结束后观察到的 Gradle 进程：67096|DAEMON。

## 编译

- 命令全文：`.\gradlew.bat compileJava --offline --no-daemon`
- 工作目录：`D:\alianximod`
- 捕获方式：`cmd.exe /d /c` 运行上述命令，把标准输出和标准错误合并写入日志。没有改命令，没有加其他 Gradle 任务。
- 开始：2026-11-14 04:44:51.256 +08:00
- 结束：2026-11-14 04:45:16.638 +08:00
- 壁钟耗时：25382 ms。
- 退出码：1
- 是否晚于最后一次相关写入：是。开始时间对照六个目标文件最晚写入 2026-11-14 04:35:32.426 +08:00。
- PowerShell：7.6.9
- 日志关键行：
  - `To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.8/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.`
  - `Daemon will be stopped at the end of the build`
  - `> Task :downloadMcpConfig`
  - `> Task :extractSrg UP-TO-DATE`
  - `> Task :createMcpToSrg UP-TO-DATE`
  - `> Task :compileJava`
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\carrier\BatchCarrierVisual.java:86: 错误: dimensions 在 Entity 中是 private 访问控制`
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:38: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:46: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:54: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:55: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:56: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:57: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:58: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:59: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:60: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:61: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:62: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:64: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:65: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:66: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:67: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:254: 错误: 方法不会覆盖或实现超类型的方法`
  - 编译器统计：`17 个错误`、`54 个警告`。这两行不是源码位置。
  - `> Task :compileJava FAILED`
  - `FAILURE: Build failed with an exception.`
  - `* What went wrong:`
  - `BUILD FAILED in 24s`
  - `4 actionable tasks: 2 executed, 2 up-to-date`
- javac 报错行：
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\cooperative\api\WeaponDisplaySnapshot.java:18: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\carrier\BatchCarrierVisual.java:86: 错误: dimensions 在 Entity 中是 private 访问控制`。本单 4 个 Java 文件之内（BatchCarrierVisual.java）。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\action\spacecut\SpaceCutSampler.java:34: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\DomainFlowRenderer.java:27: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\DomainFlowRenderer.java:28: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\DomainMistRainRenderer.java:17: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\DomainMistRingRenderer.java:13: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\DomainMistShaders.java:18: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\DomainPressureRenderer.java:17: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\DomainPressureRenderer.java:18: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\DomainWorldRenderer.java:23: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\EntityRendererFix.java:27: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\EntityRendererFix.java:28: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\EntityRendererFix.java:29: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\EntityRendererFix.java:31: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\EntityRendererFix.java:33: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\FireSeedFlameClient.java:49: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\FireVisualClient.java:705: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\furnace\FurnaceBodyVisual.java:260: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\furnace\BurnSpatialMount.java:39: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\furnace\BurnSpatialMount.java:208: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\furnace\HeatUpSpatialMount.java:241: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\LiaoliRingTrail.java:88: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\LiaoliV6Renderer.java:25: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\LiaoliV6Renderer.java:26: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\LiaoliV6Renderer.java:27: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\LiaoliV6Renderer.java:28: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\LiaoliV6Renderer.java:29: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\LiaoliV6Renderer.java:30: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\NetherDomainWisps.java:33: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\network\prison\PrisonSyncPacket.java:243: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\PrisonShellRenderer.java:35: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\StillDomainWaterClient.java:14: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\StillDomainWaterClient.java:15: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\StillDomainWaterClient.java:16: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\StillDomainWaterClient.java:50: 警告: [removal] ItemBlockRenderTypes 中的 setRenderLayer(Block,RenderType) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\SwordEntityRenderer.java:82: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\TechniqueHitRenderer.java:31: 警告: [removal] ResourceLocation 中的 ResourceLocation(String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\TechniqueHitRenderer.java:32: 警告: [removal] ResourceLocation 中的 ResourceLocation(String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\WaterBarrierRenderer.java:22: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\WaterPartingRenderer.java:19: 警告: [removal] ResourceLocation 中的 ResourceLocation(String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\client\WaterRibbonRenderer.java:24: 警告: [removal] ResourceLocation 中的 ResourceLocation(String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\combat\counter\CounterDamage.java:17: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\conditional\WaterPrisonEffect.java:40: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\cooperative\api\CooperativeActionIds.java:8: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\cooperative\api\CooperativeActionIds.java:9: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\cooperative\attack\HorizontalSlashActorAdapter.java:34: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\fire\FireDamage.java:26: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\fire\SteamBurst.java:31: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\HitVolumes.java:219: 警告: [removal] ProjectileImpactEvent 中的 setCanceled(boolean) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\prison\PrisonPressure.java:23: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:38: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:46: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:54: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:55: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:56: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:57: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:58: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:59: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:60: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:61: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:62: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:64: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:65: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:66: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:67: 错误: 不兼容的类型: ChainCompatibility无法转换为boolean`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java:254: 错误: 方法不会覆盖或实现超类型的方法`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\statusquery\ResourceStatuses.java:16: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\status\fire\seed\FireSeedState.java:19: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\status\WetnessFlightExempt.java:13: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。
  - `D:\alianximod\src\main\java\com\bslx\XueXiMod\status\wood\resource\SuihuaStatus.java:15: 警告: [removal] ResourceLocation 中的 ResourceLocation(String,String) 已过时, 且标记为待删除`。不在本单 4 个 Java 文件之内。

## 既有 class

| 路径 | 编译前 mtime | 编译前字节 | 编译后 mtime | 编译后字节 | 本次是否改写 |
| --- | --- | ---: | --- | ---: | --- |
| `build/classes/java/main/com/bslx/XueXiMod/carrier/BatchVisualTexture.class` | 2026-11-14 04:30:53.069 +08:00 | 2375 | 不存在 | - | 否 |
| `build/classes/java/main/com/bslx/XueXiMod/carrier/BatchTriggerVisuals.class` | 2026-11-14 04:30:53.196 +08:00 | 5203 | 不存在 | - | 否 |
| `build/classes/java/main/com/bslx/XueXiMod/carrier/BatchCarrierVisual.class` | 2026-11-14 04:30:53.063 +08:00 | 19287 | 不存在 | - | 否 |
| `build/classes/java/main/com/bslx/XueXiMod/client/BatchCarrierVisualRenderer.class` | 2026-11-14 04:30:53.969 +08:00 | 19690 | 不存在 | - | 否 |

class 文件不在 `src/main` 树哈希内。上表只记录这条已授权编译命令是否留下更新；mtime 未变且哈希未变的 class 不记成本次产物。

## 日志

- 路径：`D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/surface-anchor-v2-compile-20261114.log`
- 字节：30539
- mtime：2026-11-14 04:45:16.501 +08:00
- SHA256：`dab955ee8fce001f669b24b125bc65b713de4fd7a4f286779f482e9a5724b224`
- 编码：日志是 cmd 捕获的原始字节。含非 ASCII 字节，摘录时按系统代码页 936 解读。本单没有回写或改编码这份日志。

## 状态分界

- 功能：前序接入已收束。本单只编译，不重新实现。
- 美术、用户确认、正式接入：本单只核对已接入的 `surface_anchor.png` 与 `surface_anchor.png.mcmeta` 字节和哈希，没有重做美术或确认。
- 游戏实测：未做。
- 制作表：未写。
- `docs/术式制作表.xlsx`：字节、mtime、SHA256 相同。字节 179499，mtime 2026-11-13 13:07:13.390 +08:00，SHA256 `ab344ccaafbe723ad0ed6c6dd0834eb3b264ec5184227f655fae9fc1b0f12c26`。
- `docs/开发记录/术式批量制作/待制作-20261031/记录.md`：字节、mtime、SHA256 相同。字节 42534，mtime 2026-11-13 13:08:31.995 +08:00，SHA256 `6a9cd933136661640a92d793ed4546db49605e34cfd61d41748b3c5f8c010bb5`。
- `docs/开发记录/术式批量制作/待制作-20261031/实测清单.md`：字节、mtime、SHA256 相同。字节 33137，mtime 2026-11-13 13:08:10.663 +08:00，SHA256 `c17e113f2120b24290472d87c3e7610fe36d608de742210243b7f6b3936802d2`。

## 收束与额度

本单写入已收束。只写了本回执和 `surface-anchor-v2-compile-20261114.log`。Gradle 自己的构建输出若有更新，只来自这条已授权命令，本回执不把它记成源码写入。没有其他作者持有这两份记录路径。文件句柄在本 final 返回前关闭。

设计额度 2/2、实现额度 3/3 在开工前已用完。本编译没有借编译名义新增正式正确性复审，没有重置计数。

审批拒绝：无。编译失败：有，见日志。阻塞：无。统一编译命令退出码为 1。本执行者不写表、不改源码。

下一步：交回主会话。
