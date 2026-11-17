# 引爆三维扩散 v2 统一编译回执

- batchId：`待制作-20261031`
- taskId：`fire_detonate_v2_compile`
- attemptId：`20261116-compile-detonate-3d-01`
- 关联 B：`fire_detonate`
- 输入版本：`fire_detonate_v2_3d_candidate_20261116`（接入 01 + 挂点 02 已收束）
- 直接父会话：`01a1ce06-b681-7c90-b11d-260d3c169911`
- 角色：本轮唯一编译者。无下属，未派 Flash，未新增正式复审。
- 额度：设计 2/2、实现 3/3，本阶段额度已用完，返修结果未追加复审。本回执只记录编译事实，不作正确性裁决。

## 命令与结果

- 工作目录：`D:\alianximod`
- 命令：`.\gradlew.bat compileJava --offline --no-daemon`
- 开始：`2026-11-16 16:48:39.021 +08:00`
- 结束：`2026-11-16 16:49:00.628 +08:00`
- 壁钟：`21607 ms`
- 进程退出码：`0`
- Gradle 自计：`BUILD SUCCESSFUL in 20s`；`4 actionable tasks: 2 executed, 2 up-to-date`
- 环境：Java `17.0.6`，JVM `17.0.6+9-LTS-190`（Oracle），amd64；Gradle `8.8`。`--no-daemon` 仍按现有 JVM 参数派生单次 Daemon，日志写明结束时停止。编译后无 `java` / `javaw` / `gradle` 进程。
- 任务：`:downloadMcpConfig` 执行，`:extractSrg` 与 `:createMcpToSrg` 为 UP-TO-DATE，`:compileJava` 执行。
- 警告：57 个既有过时 API 警告，无编译错误。未改源码，未顺手修复。
- 原始输出：`D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031\detonate-v2-3d-compile-20261116.log`，19825 字节，cmd 原样重定向，编码为 CP936。SHA256 `6B0E4E8C4C7AB841BFFE8307E219265305C01A93C7608A4C7569E2932E325681`。
- 时间侧记：`D:\alianximod\docs\开发记录\术式批量制作\待制作-20261031\detonate-v2-3d-compile-20261116-timing.txt`，180 字节，SHA256 `28D85D8592D890ECA6B01643BAAAE6D2F328A5DAF659633E56DCB725043B0C0F`。

## 占用与覆盖关系

- 启动前磁盘上有 `.gradle` 元数据锁文件，但没有 `java` / `javaw` / `gradle` 进程。这不算同工作区正在编译，核对后才启动本次编译。
- 相关源码最晚写入：`TechniqueHitSettle.java` 为 `2026-11-16 16:44:47.251`。编译开始于 `16:48:39.021`，晚于该写入。
- 编译前后 `src/main` 均为 1615 个文件，清单逐字节相同：新增 0、删除 0、内容或时间变化 0。
- 清单树哈希（清单 UTF-8 文本的 SHA256）前后都是 `612909CA7ACFA5C14EEC8B934D05F6703764A7AB2D8F3E35456FA28B0AF31CE2`。两份清单文件哈希相同是因为内容相同，不是复制漏记。
- 前：`detonate-v2-3d-compile-20261116-src-before.txt`，268459 字节。
- 后：`detonate-v2-3d-compile-20261116-src-after.txt`，268459 字节。
- 产物类文件写于 `16:48:55`–`16:48:59`，晚于上述源码，且编译期间源码未再变。因此本次 `compileJava` 输出覆盖的是这份最终源码。

## 纳入编译的源码指纹

以下为编译前清单中的现行字节，编译后未变。

- `src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/DetonatePlan.java`：`642E74317C4434D8097A88EC47EC79DF63114BE5C92F01B85E19A685FD4817A1`，6004 字节，`16:44:35.520`
- `src/main/java/com/bslx/XueXiMod/shu/technique/enchant/resource/ResourceHitCommit.java`：`8832A1F2A689DE0F6EC10A686C1545B6EB7265C66C94B480FB47FD0F07D7633E`，3942 字节，`16:44:35.526`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/ResourceSettlement.java`：`320AFC05E55BD862E8A38A0895DEF8925CBE83B98F2E09AE2E0047E7B2E3399F`，2709 字节，`16:44:35.531`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitSettle.java`：`A70AD86E80CB9EB76C30E95B53D9B99ABAFA69A4EFD802E3054A0977FA0F8C40`，21059 字节，`16:44:47.251`
- `src/main/java/com/bslx/XueXiMod/carrier/DetonateCloudLayout.java`：`8C4BAFEBD291B9B8A57DE8888C43A14E66F938B6BD3D403F54A646DCBA742D68`，6015 字节，`16:37:43.450`
- `src/main/java/com/bslx/XueXiMod/carrier/BatchTriggerVisuals.java`：`97A6B5B7D43965A4C3CB2E2CB3397EA1BFA2CFA3921B9DC4A25B3BAE4B5E1F81`，4984 字节，`16:37:57.713`
- `src/main/java/com/bslx/XueXiMod/carrier/BatchVisualTexture.java`：`83CA33CDAFA36805955D60124B39FACA31AD2203D43E88CD36B9F3275C792AA8`，1204 字节，`16:37:56.995`
- `src/main/java/com/bslx/XueXiMod/client/DetonateCloudMesh.java`：`3BA9C02A9B7A51DC278086BBA7DD4C30902BA5DC37B43755B976B7F8DC9543E1`，1643 字节，`16:37:54.420`
- `src/main/java/com/bslx/XueXiMod/client/BatchCarrierVisualRenderer.java`：`A587B2F9F255C16A0F3705EFB1ACD9F53D419625A51FF01D767674C7E3AA3FC9`，42512 字节，`16:39:26.444`
- `src/main/java/com/bslx/XueXiMod/client/CameraFacingClient.java`：`947DD3C45D02EAFE399335FBE2821434E24D4CE36E9575474ABAE47E1589E84B`，3250 字节，`16:38:04.998`
- `src/main/java/com/bslx/XueXiMod/visual/facing/CameraFacingRegistry.java`：`2325BDB82679CA7783CEBC184FC90F4C7291E30BCFA4D67BC267D8D3FA3E883D`，1068 字节，`16:37:58.561`

对应产物：

- `DetonateCloudLayout.class` `16:48:55.887`，3711 字节
- `BatchTriggerVisuals.class` `16:48:55.879`，5581 字节
- `BatchVisualTexture.class` `16:48:55.806`，2391 字节
- `DetonateCloudMesh.class` `16:48:56.492`，5828 字节
- `BatchCarrierVisualRenderer.class` `16:48:56.341`，28166 字节
- `CameraFacingClient.class` `16:48:56.349`，3296 字节
- `DetonatePlan.class` `16:48:58.714`，5285 字节
- `ResourceHitCommit.class` `16:48:58.722`，5276 字节
- `ResourceSettlement.class` `16:48:59.359`，3953 字节
- `TechniqueHitSettle.class` `16:48:59.367`，21922 字节
- `CameraFacingRegistry.class` `16:48:59.909`，1678 字节

产物目录：`D:\alianximod\build\classes\java\main\com\bslx\XueXiMod\`。

## 运行资源与归档

橙色运行贴图：`src/main/resources/assets/xueximod/textures/effect/detonate_v2_3d/explosion_puff_orange.png`。SHA256 `28666B39C8A80E0A18EB04DDAE7F7422A546B179307DB0990F7F8109ADF8D0F7`，2179 字节，32×512。与正式归档 `D:\alianximod\docs\资源库\特效\火焰\引爆_三维扩散_v2\textures\explosion_puff_orange-strip.png` 哈希和字节数相同。

幽冥蓝运行贴图：`src/main/resources/assets/xueximod/textures/effect/detonate_v2_3d/explosion_puff_soul.png`。SHA256 `C4B656B506B6CB721BCEF39CFED9BA99AA0AE21F2EDED7839057FC3573835856`，2193 字节，32×512。与归档 `explosion_puff_soul-strip.png` 哈希和字节数相同。

两份 `.mcmeta` 均为 `{ "texture": { "blur": false, "clamp": true } }`，SHA256 `6C5F3AB6BDD0921FB58CC5837E09DADE2660606111271B736860C2FEA01E1651`，47 字节。

引用：`BatchVisualTexture.DETONATE` 指向橙色运行贴图；幽冥蓝分支由 `BatchCarrierVisualRenderer` 切到同目录 `explosion_puff_soul.png`。旧文件 `textures/effect/detonate_v1/detonate.png` 仍在磁盘，SHA256 `F3F0FCEB4A8AAECBA5EAC2B300C1FD5036B0E9EEAF1CD04B1A4F38FC1C79D858`，本轮 Java 引用检索没有再指向 `detonate_v1`。

## 写入与未跑项

- 本轮只新增本回执、日志、时间侧记和两份 `src/main` 指纹。未改 `src`、美术源、正式归档、资源库 README、共享记录或制作表。
- 未做参数复审，未改伤害、消费、去重或视觉参数。
- 无下属代理；没有待关闭的嵌套会话。
- 未跑：`runClient`、`build`、`jar`、`reobf`、游戏启动、本轮命名门禁、制作表迁页。编译通过不等于游戏内验证通过，也不单独满足迁待验收。
- 下一步：交主会话按流程第 8 节由 Rawls 核对已有交付后写表。表格同步本轮未做。
