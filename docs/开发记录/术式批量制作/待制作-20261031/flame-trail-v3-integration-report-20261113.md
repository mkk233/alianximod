# 留焰 v3 正式归档与运行接入回执 — 2026-11-13

## 身份与输入版本

- 工作区：`D:/alianximod`
- batchId：`待制作-20261031`
- taskId：`flame_trail_v3_integration`
- attemptId：`20261113-integrate-01`
- 关联 B：`fire_flame_trail`
- 操作角色：`implement`；stage：`implementation`
- 请求模型 / 档位：`cursor/grok-4.7@500k` / `high` + Fast。客户端按 `service_tier="priority"` 配置；工具无 tier 参数，不声称后端已独立验证。可观察返回身份未知，后端或请求追踪信息未知。
- 授权来源：用户于当前会话对留焰 v3 二维与真实 Blockbench 三维预览明确回复「接入这版」（2026-11-13）。
- 输入版本：
  - `D:/alianximod/docs/资源库/过程归档/留焰_v3_真实留焰翻腾尾迹_20261113/visual-handoff.md`
  - 同目录 `manifest.json`
  - `D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/flame-trail-v3-preview-20261113.md`
  - Flash 事实包：代理 `01a1d0d9-4057-79e0-b616-68543d76d309`（Kuhn），`flame_trail_v3_integration_facts` / `read-facts-01`
- 依赖：`flame_trail_art_preview`（`attemptId=20261113-v3-gemini-carbon`，已完成并获用户确认）；`flame_trail_v3_integration_facts`（已完成）。
- 当前阶段校验计数：设计 `2/2`、实现 `3/3`，均已耗尽。本单不重置、不追加正式正确性复审、未启动 Astra/Opus。
- 真实直接父代理：当前主会话。终态回传方式：向主会话提交本完整 final。父方接收时间由主会话登记。
- 本单未派 Flash。下属 Flash：不适用；读取范围不适用；未决项无。

## 完成与未完成

完成：确认版素材逐字节归档、资源库索引更新、运行贴图与 mcmeta 接入、`BatchVisualTexture.TRAIL` 路径切换、只读契约核对、命名门禁、本回执。

未完成：统一 `compileJava`、游戏实测、制作表迁移。这些都不属于本单授权。

## 四项独立状态

- 功能：已实现。本单未改玩法、伤害、寿命、尺寸、采样间距或配置底座。
- 美术：v3 已正式归档。过程稿保留，像素、GIF、视口截图和 `.bbmodel` 均未重画或改字节。
- 用户确认：已确认。原话为「接入这版」，确认对象是本版二维与真实 Blockbench 三维预览。
- 正式接入：运行资源已接入，待统一编译与游戏实测。没有剩余占位待替换项；旧 `flame_trail_v1` 保留为历史，不再作为 `TRAIL` 运行路径。

不能迁待验收：最终编译与第 8 节其余交付证据尚未由主会话收齐，本单也没有写表权限。

## N / O 映射

- 游戏内一句话 N：`沿载体留下不产种的火焰轨迹`。
- 游戏内名称：`留焰`。
- 真实运行资源 O：`assets/xueximod/textures/effect/flame_trail_v3/flame_trail.png`。
- 代码引用：`BatchVisualTexture.TRAIL("textures/effect/flame_trail_v3/flame_trail.png")`。
- 来源核对：`D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/ModTechniques.java:300-302`。本单未改该文件，也未改制作表。

## 与视觉交接逐项核对

以下均只读核对，未改对应源码：

- `BatchCarrierIds.STRIP_FRAMES = 16`。
- `TrailContract.DIAMETER = 0.5`；`BatchFlameTrail` 以 `DIAMETER * 0.5` 传入 `halfSpan`，即 `0.25`。
- `TrailContract.SAMPLE_SPACING = 0.25`。
- `TrailContract.LIFE_TICKS = 40`。
- `TrailContract.ACTIVE_POINT_CAP = 256`。
- `BatchCarrierVisualRenderer.frameIndex` 为 `floor(elapsed * 16 / duration)`；duration 为 40 tick 时即 `floor(age * 16 / 40)`。
- `topV = frame / 16`，`bottomV = (frame + 1) / 16`。
- 非尘土、非地表贴图使用 `LightTexture.FULL_BRIGHT`，`TRAIL` 包含在内。
- `drawBillboard` 使用 `RenderType.entityTranslucent`，四个顶点 alpha 均为 `255`。
- 告示板先套相机朝向；飞行滚转只在 `BatchVisualTexture.SPARK` 分支，`TRAIL` 无滚转。
- 形状为 `BatchVisualShape.BILLBOARD`，采样点留在生成时的世界坐标。
- 运行 PNG：`64×1024`、8-bit、color type 6（RGBA）。SHA256 严格等于 `50946B75FBBEFDA2F4A34FC337FD8A665A18F0428638F574CB2880CA88B06D1D`。
- 预览弹速 `0.625` 格/tick 未写入配置。地面、刻度、假人、helper 和载体示意没有进入 `src/main/resources/`。

## 配置兼容字段

- `cooldownTicks`：默认 `5` tick，范围 `0..1200000`。原始默认不为 0，因此配置 0 不合法。由 `BalanceCatalog.sealAfterContent()` 通过 `BalanceCatalog.addCooldown()` 声明。
- `manaCost`：默认 `2` 法力，范围 `0..1000000`。显式价来自 `ResourceEnchantNumbers.MANA_PRICE`，经 `ResourceEnchantSlice.trail()`、`Technique.manaCost()` 与 `SkillSegmentMath.manaCostOf()` 进入报价。
- 消费路径：`SkillSegmentMath.configuredBaseCooldown()` 调 `BalanceDisplay.cooldown()`；`SkillSegmentMath.quotedMana()` 调 `BalanceDisplay.mana()`。服务端 `CooperativeChainCompiler` 分别调 `BalanceService.cooldownTicks(BalanceSide.SERVER, ...)` 与 `BalanceService.manaCost(BalanceSide.SERVER, ...)`。`BalanceDisplay` 与 `BalanceService.current()` 按已绑定侧读取，客户端显示与服务端结算使用同一套已声明键。
- `baseDamage = 0`。`fire_flame_trail` 不在 `BalanceCatalog.DAMAGE_HOSTS`，也不在 `DAMAGE_EXCLUDED`；标准声明只在基础伤害大于 0 或列入伤害宿主时补 `damageMultiplier`。因此本术式不声明 `damageMultiplier`。
- 本单没有新增或重命名配置键，没有改默认值、范围或覆盖值。

## 正式归档文件

目录：`D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/`。前 20 项从过程目录逐字节复制，复制前后 SHA256 均与确认版一致。时间为本地 `Asia/Shanghai`。

- `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/flame_trail_v3.aseprite`：5259 bytes；mtime `2026-11-13 00:49:43.380`；SHA256 `63E13A9356846D981AB111DFE2A774ED50827E741EB8654E9385293E77DDFD9E`
- `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/flame-trail-v3-scene.aseprite`：104367 bytes；mtime `2026-11-13 00:50:22.609`；SHA256 `21EFDFE97C317CF36DB57164A8BE0874434FBE64426FABB96FB40A591B04EC49`
- `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/flame_trail_v3_strip.png`：3718 bytes；mtime `2026-11-13 00:50:02.075`；SHA256 `50946B75FBBEFDA2F4A34FC337FD8A665A18F0428638F574CB2880CA88B06D1D`
- `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/flame-trail-v3-complete-effect.gif`：111390 bytes；mtime `2026-11-13 00:52:35.480`；SHA256 `C1ACF6A69E6DC464C158F1A7EFD96942A3C54A5C9E51F9AE9F4CDC773110BF44`
- `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/flame_trail_v3_single.gif`：10375 bytes；mtime `2026-11-13 00:52:53.270`；SHA256 `8F8EFCEB9094E551619AC4CA2ED023035ABE94ECA3C9FF8A1C66C2CABD1C8217`
- `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/flame_trail_v3_grid_4x4.png`：4893 bytes；mtime `2026-11-13 00:54:08.216`；SHA256 `5B02474D568E799CB3D42721BBFA8F56787C45478155657F0172817D71A3D6C4`
- `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/flame-trail-v3-2d-stage0-launch-t03.png`：3378 bytes；mtime `2026-11-13 00:53:50.224`；SHA256 `E9108FC52EE2E5584F6A45E6F5C198ECED4538413451242E9CDEA2BAE1DA0098`
- `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/flame-trail-v3-2d-stage1-dash-t08.png`：4073 bytes；mtime `2026-11-13 00:50:22.692`；SHA256 `1666042D4892ED4290634E25C394C223F20F687130661F6122D67650DEBCEB7B`
- `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/flame-trail-v3-2d-stage2-full-wake-t14.png`：4725 bytes；mtime `2026-11-13 00:50:22.773`；SHA256 `21A3B7E311F154D5B1FC535C3E6CBEE0732EFA02418E4D57D10FDD659CC59106`
- `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/flame-trail-v3-2d-stage3-linger-rise-t24.png`：4503 bytes；mtime `2026-11-13 00:50:22.857`；SHA256 `CA15F0214139F336AC56A3496DEF4C65AA97AAD51B1F5C10C7621FC720F88D72`
- `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/flame-trail-v3-2d-stage4-breakup-embers-t36.png`：3552 bytes；mtime `2026-11-13 00:50:22.942`；SHA256 `2C86E2281378D57D22C8049701933E47F54C5768A10885B05A3EA97EF49ACC5E`
- `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/flame-trail-v3-2d-stage5-fadeout-t48.png`：2651 bytes；mtime `2026-11-13 00:53:50.302`；SHA256 `5889648709ADF863A47A90337563E2C895F58368469BBD210C29180F8F18231E`
- `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/flame-trail-v3-projectile-composite.bbmodel`：70103 bytes；mtime `2026-11-13 01:01:00.227`；SHA256 `365064F04B05A9816CDCE2E37B77B09F2944A34A4EF31D82B968C9F2701CD85F`
- `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/flame-trail-v3-3d-hero-closeup.png`：102042 bytes；mtime `2026-11-13 01:02:09.746`；SHA256 `971A4D712280FE532EA627527FEA64C057D8382DB6141E895A4C9E0361ACB377`
- `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/flame-trail-v3-3d-side-view.png`：63633 bytes；mtime `2026-11-13 01:01:00.195`；SHA256 `6578840919FBE79E9415C81F7C5186D6C69BA1403441127A77C44A716024DC1B`
- `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/flame-trail-v3-3d-stage1-launch-t03.png`：62143 bytes；mtime `2026-11-13 01:01:00.044`；SHA256 `9EFF891C5CEB834BA1850E128B51B3DEE6EBE8ABBB3439FC5D1EAD4B7EA6B8A7`
- `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/flame-trail-v3-3d-stage2-dash-t08.png`：66244 bytes；mtime `2026-11-13 01:01:00.073`；SHA256 `EEE351B9D1BF4DDB7020E5FCB02964383B4D35C731C531A9CB146770117933F9`
- `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/flame-trail-v3-3d-stage3-full-wake-t14.png`：70414 bytes；mtime `2026-11-13 01:01:00.102`；SHA256 `1F876758949F26CDD57651197BB5DEFC2C75B6FFCD8B7B513BA7938F105D7A74`
- `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/flame-trail-v3-3d-stage4-linger-rise-t24.png`：68013 bytes；mtime `2026-11-13 01:01:00.134`；SHA256 `3C0DDE4900A6FF661F4488FE22F29A5D917762D6277FC13BCC7626A7687549C4`
- `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/flame-trail-v3-3d-stage5-breakup-embers-t38.png`：63498 bytes；mtime `2026-11-13 01:01:00.167`；SHA256 `BA1BBE06A522A7526C6DEFE9A45193014F12909AC0F02CB52410BF70D3B1FB28`
- `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/README.md`：3321 bytes；mtime `2026-11-13 01:53:17.978`；SHA256 `7E1F72F46450DD3FF8D16950661D97A4EA591D9A0A102DEEA0E11EF1D53827CC`
- `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/manifest.json`：7055 bytes；mtime `2026-11-13 01:53:28.506`；SHA256 `5C2D430D97F08BF7CF91C35A4DE55FDB7F223D8D72B64577AD49976804351FFC`。清单记录除自身外的 21 个同目录文件；自身哈希只记录在本回执，避免写入正文后改变哈希。

## 索引、代码与运行资源

- `D:/alianximod/docs/资源库/README.md`：25897 bytes；mtime `2026-11-13 01:53:17.928`；SHA256 `20AFEC2CDAE3935A82CC67E8BB3D07B48B838DAFF7A3502C210F87C1C64A46C1`。当前资源顶部已新增 v3 条目；新十三式段落末尾已说明留焰运行贴图替换，并保留 `留焰_v1` 为历史。
- `D:/alianximod/src/main/java/com/bslx/XueXiMod/carrier/BatchVisualTexture.java`：1188 bytes；mtime `2026-11-13 01:52:27.987`；SHA256 `DD8D646312CB66C64F1BEA53A99D7E54198DBFD50E749241BFAC77458D742B56`。唯一代码改动是 `TRAIL` 路径由 v1 改为 v3。
- `D:/alianximod/src/main/resources/assets/xueximod/textures/effect/flame_trail_v3/flame_trail.png`：3718 bytes；mtime `2026-11-13 00:50:02.075`；SHA256 `50946B75FBBEFDA2F4A34FC337FD8A665A18F0428638F574CB2880CA88B06D1D`。
- `D:/alianximod/src/main/resources/assets/xueximod/textures/effect/flame_trail_v3/flame_trail.png.mcmeta`：64 bytes；mtime `2026-11-12 13:07:16.892`；SHA256 `B2CAA414505E1C556CF68460294471D97198A50F3D69ED1FD9B717510C0D6F4A`。内容为 `texture.blur=false`、`texture.clamp=true`。

## 保留项核对

- `D:/alianximod/docs/资源库/特效/火焰/留焰_v1/` 仍存在。
- `D:/alianximod/docs/资源库/过程归档/留焰_v3_真实留焰翻腾尾迹_20261113/visual-handoff.md` 仍存在。
- 过程竖条复制后 SHA256 仍是 `50946B75FBBEFDA2F4A34FC337FD8A665A18F0428638F574CB2880CA88B06D1D`。
- 旧运行贴图仍存在：`D:/alianximod/src/main/resources/assets/xueximod/textures/effect/flame_trail_v1/flame_trail.png`，12057 bytes，SHA256 `F1B0D723B4AEA57B37FCA4928AFAF492E8F1020D864183F36EDABF379BE95284`。复制前后哈希一致。
- 运行目录只有 `flame_trail.png` 与 `flame_trail.png.mcmeta`，没有 `.aseprite`、`.bbmodel`、GIF、视口截图或示意假人。

## 命令、编译与验证

- 命令：`powershell -NoProfile -File tools/check-name-gate.ps1`
- 工作目录：`D:/alianximod`
- 退出码：`1`
- 本次修改的 `BatchVisualTexture.java`：无违规报告。
- 既有历史报告 14 处，本单未改、未处理：
  - `client/BatchCarrierVisualRenderer.java:111:20 dx`
  - `client/BatchCarrierVisualRenderer.java:112:20 dy`
  - `client/BatchCarrierVisualRenderer.java:113:20 dz`
  - `client/BatchCarrierVisualRenderer.java:130:15 up`
  - `client/BatchCarrierVisualRenderer.java:40:24 SPARK_ROLL_MIN_PROJECTION`
  - `client/NetherDomainWisps.java:38:33 SPACING_MIN`
  - `client/NetherDomainWisps.java:41:33 WIDTH_MIN`
  - `client/NetherDomainWisps.java:43:33 HEIGHT_MIN`
  - `client/furnace/HeatUpSpatialMount.java:138:15 minX`
  - `client/furnace/HeatUpSpatialMount.java:139:15 maxX`
  - `client/furnace/HeatUpSpatialMount.java:140:15 minY`
  - `client/furnace/HeatUpSpatialMount.java:141:15 maxY`
  - `client/furnace/HeatUpSpatialMount.java:142:15 minZ`
  - `client/furnace/HeatUpSpatialMount.java:143:15 maxZ`
- 编译：待统一编译。未运行 `gradlew`，未打包，未 `runClient`，未启动游戏。
- 未跑验证：资源加载、透明排序、面向相机、曲线轨迹逐 tick 生成、第一人称与其他玩家观察、光照对比、命中与敌我筛选。游戏实测由用户进行。

## 写入收束与下一步

- 本单写入已全部收束。无在跑写入，无在跑下属。
- 未修改 `docs/术式制作表.xlsx`、`记录.md`、`实测清单.md`、语言文件、玩法逻辑或配置底座。
- 审批、失败与阻塞：无。命名门禁的退出码 1 是既有历史报告，不是本单阻塞。
- 下一步：主会话接收本回执并确认写入收束后，派发统一 `compileJava` 任务。

## 统一编译（2026-11-13）

- attemptId：`20261113-final-compile-01`。角色只做统一编译与状态同步。未改 `src/`、贴图、`.mcmeta`、`gradle.properties`、`docs/术式制作表.xlsx`、`记录.md`、`实测清单.md`。未运行 `build`、`jar`、`reobf`、`runClient`，未打包，未启动游戏，未结束 IDE、Excel 或 Gradle Daemon。
- 所有权：`BatchVisualTexture.java` 归属 `flame_trail_v3_integration`。本回执在追加本节前的 mtime 为 `2026-11-13 01:54:41.704 +08:00`，与指定收束时刻 `01:54:41` 一致。记录准备回执 mtime 为 `2026-11-13 01:56:38.423 +08:00`，只写文档。编译前 `src/main` 没有更晚写入。

### 编译前冻结

- `src/main` 文件数 `1375`。最新修改是 `D:/alianximod/src/main/java/com/bslx/XueXiMod/carrier/BatchVisualTexture.java`，mtime `2026-11-13 01:52:27.987 +08:00`。
- `BatchVisualTexture.java`：1188 bytes；mtime `2026-11-13 01:52:27.987 +08:00`；SHA256 `DD8D646312CB66C64F1BEA53A99D7E54198DBFD50E749241BFAC77458D742B56`。与接入回执一致。
- `D:/alianximod/src/main/resources/assets/xueximod/textures/effect/flame_trail_v3/flame_trail.png`：3718 bytes；mtime `2026-11-13 00:50:02.075 +08:00`；SHA256 `50946B75FBBEFDA2F4A34FC337FD8A665A18F0428638F574CB2880CA88B06D1D`。与接入回执一致。
- `D:/alianximod/src/main/resources/assets/xueximod/textures/effect/flame_trail_v3/flame_trail.png.mcmeta`：64 bytes；mtime `2026-11-12 13:07:16.892 +08:00`；SHA256 `B2CAA414505E1C556CF68460294471D97198A50F3D69ED1FD9B717510C0D6F4A`。与接入回执一致。
- 进程：`java.exe` PID `52656` 是 IntelliJ `idea64.exe` PID `33864` 于 `2026-11-13 01:36:49` 启动的空闲 Gradle 8.8 Daemon，没有子编译进程。`.gradle` 锁最后写入 `2026-11-13 01:40:01`，`build` 当时最新文件为 `2026-11-13 01:37:53.045`。没有 `javaw` Forge 客户端，也没有另一路 Gradle 正在写 `D:/alianximod/build`。本次使用 `--no-daemon`，未触碰该空闲 Daemon。

### 执行

- 工作目录：`D:/alianximod`。
- 命令全文：`.\gradlew.bat compileJava --offline --no-daemon`。
- 捕获：`cmd.exe /d /c` 只把上述命令的标准输出和标准错误合并写入日志，没有改 Gradle 参数。
- 开始：`2026-11-13 01:58:53.725 +08:00`。
- 结束：`2026-11-13 01:59:19.149 +08:00`。
- 进程退出码：`0`。
- 日志：`D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/flame-trail-v3-compile-20261113.log`；18959 bytes；mtime `2026-11-13 01:59:18.424 +08:00`；SHA256 `79497212A2A21C21E22AAB339615EE11D03A5DF5589030A8D3F6BD3C036B7D07`。文件字节是控制台代码页 CP936 原样输出，未转码。
- 日志结论：`> Task :compileJava`；`54 个警告`，均为既有过时 API 警告，不是本单错误；`BUILD SUCCESSFUL in 23s`；`4 actionable tasks: 2 executed, 2 up-to-date`。

### 编译后核验

- `src/main` 仍是 `1375` 个文件。上面三个文件的字节数、SHA256 和 mtime 与编译前完全相同，最新修改时间仍是 `2026-11-13 01:52:27.987 +08:00`。
- `D:/alianximod/build/classes/java/main/com/bslx/XueXiMod/carrier/BatchVisualTexture.class`：2375 bytes；mtime `2026-11-13 01:59:12.950 +08:00`；SHA256 `10A01E368BEB0B877E796FCDE42FB40FD025D4E18CCB85FD4FB45745BB4FCAB4`。
- class mtime 严格晚于 Java 源码 mtime `2026-11-13 01:52:27.987 +08:00`。编译前该 class 为 `2026-11-12 20:53:01.488 +08:00`、SHA256 `73853B59009E9EDD210B135EC11DB47721902ACFB9FB36D9FF525C4AEDAE2F79`。本次哈希已变，说明这次 `compileJava` 覆盖了收束后的最终源码。

### 文档同步

- `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/README.md`：3539 bytes；mtime `2026-11-13 02:00:12.799 +08:00`；SHA256 `CCD4CAF6F8BB87520991268502B754E19CC2107A74FC16CF65ED35D82666B5AE`。状态改为最终编译通过、待游戏实测，并写明上述时间窗口与退出码 `0`。
- `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/manifest.json` 的 `README.md` 条目已同步为 3539 bytes、SHA256 `CCD4CAF6F8BB87520991268502B754E19CC2107A74FC16CF65ED35D82666B5AE`。清单仍记录除自身外 21 个文件；清单自身 7055 bytes，mtime `2026-11-13 02:00:25.137 +08:00`，SHA256 `8EFEC43A3D993CE50D500C17357BB21778FB99603B058769B3EC3A7A23FA2BF5`。自身哈希只记在本节，不写进清单正文。素材文件未改。
- `D:/alianximod/docs/资源库/README.md`：26028 bytes；mtime `2026-11-13 02:00:16.618 +08:00`；SHA256 `DED0CF48AA48EA775E8846E53F6FEFAC9207468F752152EA968D9767ADE09C99`。顶部留焰 v3 条目改为「已接入，最终编译通过，待游戏实测」，并注明同一时间窗口与退出码 `0`。

### 本次写入收束

- 已收束：编译日志、本节追加、两份 README，以及 v3 `manifest.json` 里的 README 条目。无在跑写入，无在跑下属。
- 审批、失败与阻塞：无。54 个警告不改变退出码 `0`，也未因此改源码。
- 下一步：主会话接收本编译回执后，派发唯一制作表写回代理。
