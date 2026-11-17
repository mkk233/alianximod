# 美术正式归档回执 — 火苗 v3.1（十字穿插直射 · 用户确认采用版）

## 1. 任务头与边界回执
- **工作区**：`D:\alianximod`
- **batchId**：`待制作-20261031`
- **taskId**：`fire_spark_crossed_launch_preview`
- **attemptId**：`20261113-art-spark-cross-archive-01`
- **前序依赖**：`20261113-art-spark-cross-01`（已完成真实 Blockbench 三维视口预览交付）
- **stage**：`delivery`（用户同版本确认后的正式资源库归档与索引更新）
- **操作角色**：`art`（`antigravity/gemini-4-carbon`，兼本次 `docs/资源库/README.md` 指定索引更新作者）
- **真实直接父代理**：主会话
- **关联术式 B**：`fire_spark`（火苗）
- **用户确认依据**：`2026-11-13` 用户审阅 `v3.1` 十字穿插直射真实 Blockbench 三维视口预览后明确回复原话：**「采用」**（二维竖条沿用 `2026-11-11` 用户确认「这版可以接入」的 `fire_spark_v3`）。
- **复审计数**：设计 `2/2`、实现 `3/3`（保持既有计数，未重置、未新增任何复审轮次）。
- **边界与隔离执行情况**：
  - 未打开、新建或触碰任何 Blockbench / Aseprite 实例与未保存工程。
  - 只读源目录 `D:/alianximod/docs/资源库/特效/火焰/火苗_飞行火舌_v3/`、`D:/alianximod/docs/资源库/特效/火焰/火苗_v1/` 与 `D:/alianximod/docs/资源库/过程归档/火苗_v3.1_十字穿插直射示意_20261113/` 完整保留，字节与 SHA256 零改动。
  - 未触碰 `src/`、`docs/术式制作表.xlsx`、`记录.md`、`实测清单.md`；未运行 `gradlew`；未打包、未启动游戏。

## 2. 正式归档目录文件清单与 SHA256 校验
正式归档目录：`D:/alianximod/docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/`（共 12 个文件，`manifest.json` 内记录的其余全部 11 个文件字节数与 SHA256 均已实测 100% 匹配）：

1. `D:/alianximod/docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/fire_spark_v3.aseprite`
   - 角色：二维可编辑源（逐字节复制自 `火苗_飞行火舌_v3/`，哈希一致）
   - 大小：`10,644` bytes
   - SHA256：`710957E6C13AB8D8CC0C9EA5BD6CFD424041EE5C8F9BE8231CF75D4EB15C8FCA`
2. `D:/alianximod/docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/fire_spark_v3_strip.png`
   - 角色：二维确认版原尺寸竖条导出（`64×1024`，16 帧 `64×64`，逐字节复制自 `火苗_飞行火舌_v3/`，哈希一致）
   - 大小：`5,005` bytes
   - SHA256：`672DA9132E7E91A83636836A62ADE4778781D51592A541041C511653238DEC5E`
3. `D:/alianximod/docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/fire_spark_v3.gif`
   - 角色：二维循环动画预览（逐字节复制自 `火苗_飞行火舌_v3/`，哈希一致）
   - 大小：`26,426` bytes
   - SHA256：`A3EA8EB30C557859FCE0D04E21EE0B7E1BFD5E6ED6A155DA456E8F8EE12AB0AE`
4. `D:/alianximod/docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/fire-spark-v3-1-crossed-launch-preview.bbmodel`
   - 角色：同版真实 Blockbench `5.2.2` 三维空间示意工程（逐字节复制自过程归档，哈希一致；非运行资源）
   - 大小：`42,052` bytes
   - SHA256：`8E676075011EC74FC596BC0E1F046D7CAC62DACEE87A0CF3B9C79A3567B4C47B`
5. `D:/alianximod/docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/fire-spark-v3-1-3d-01-first-person-launch.png`
   - 角色：真实视口预览 01 — 第一人称发射视角（`1366×973`，逐字节复制自过程归档，哈希一致）
   - 大小：`50,738` bytes
   - SHA256：`A25A668C6ED21C136467E74039802B61F2B8E2FED3AD1C4634F9C86EC3FAF68D`
6. `D:/alianximod/docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/fire-spark-v3-1-3d-02-hero-3quarter.png`
   - 角色：真实视口预览 02 — 3/4 近景透视主图（`1366×973`，逐字节复制自过程归档，哈希一致）
   - 大小：`107,329` bytes
   - SHA256：`B06516E2AB4197529B23851463050E3F5B00525EB4879FEBC47EAEB1190ACA06`
7. `D:/alianximod/docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/fire-spark-v3-1-3d-03-axial-cross-section.png`
   - 角色：真实视口预览 03 — 迎面轴向十字截面透视图（`1366×973`，逐字节复制自过程归档，哈希一致）
   - 大小：`91,082` bytes
   - SHA256：`F4C48F8A7B95B38D1E98BA06F74CB6D930AC686ADB139A2E6F43C6D49CA46F07`
8. `D:/alianximod/docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/fire-spark-v3-1-3d-04-side-view.png`
   - 角色：真实视口预览 04 — 正交正侧视图（`1366×973`，逐字节复制自过程归档，哈希一致）
   - 大小：`31,892` bytes
   - SHA256：`0039AC3FB33439C300909DDBB4A9F09BB47FF67AE72327155784144783EF52A8`
9. `D:/alianximod/docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/fire-spark-v3-1-3d-05-ray-two-stages.png`
   - 角色：真实视口预览 05 — 同射线双阶段直射姿态对照图（`1366×973`，逐字节复制自过程归档，哈希一致）
   - 大小：`71,835` bytes
   - SHA256：`7280F8D7ACFFB13D50881AC04340E8F119DA275D6F006218FA3CC4312844C689`
10. `D:/alianximod/docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/visual-handoff.md`
    - 角色：正式确认版视觉接入交接说明
    - 大小：`12,680` bytes
    - SHA256：`BA8E5DA35BB037D07BD79A7B63590C39B8F47D7893E3773AA94B9175A345842C`
11. `D:/alianximod/docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/README.md`
    - 角色：正式归档目录说明文档
    - 大小：`5,517` bytes
    - SHA256：`00662EEBE9C167F4BE605D59B821F2F3DC3EB6E1019F8347738C57D3D5B74B7E`
12. `D:/alianximod/docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/manifest.json`
    - 角色：正式归档元数据与 11 项文件哈希清单
    - 大小：`5,955` bytes
    - SHA256：`597248395F717B1AE5D91E77CD020A0B48AAB813070789F582F41076BBD54C65`

## 3. `docs/资源库/README.md` 索引更新前后哈希对照
- **文件路径**：`D:/alianximod/docs/资源库/README.md`
- **更新前（开工前实时校验）**：`26,028` bytes，SHA256 `DED0CF48AA48EA775E8846E53F6FEFAC9207468F752152EA968D9767ADE09C99`（共 154 行）
- **更新后（写入后实时校验）**：`27,074` bytes，SHA256 `B59EC38C4877CAD0B48FE4AF1285369CF580766A63813193B2962E0FB42B239B`（共 157 行）
- **变更摘要**：
  1. 在 `## 当前资源` 顶部新增 `[火苗 · 十字穿插直射 v3.1（用户已确认采用，待运行核对与游戏实测）](特效/火焰/火苗_十字穿插直射_v3.1/README.md)` 正式条目。
  2. 在底部 `### 2026-11-11 火苗飞行火舌 v3` 下方补充说明二维竖条继续复用、空间朝向升级至 `火苗_十字穿插直射_v3.1`（`CameraFacingTag.NONE` 箭矢式 `±45°` 十字穿插直射）且 `v3` 目录完整保留。
  3. 其余全部既有条目一字未改、完整保留。

## 4. 待 Grok 核对的运行资源与渲染参数映射清单
1. **运行贴图文件核对**：
   - 确认版源导出：`D:/alianximod/docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/fire_spark_v3_strip.png`（`5,005` bytes，SHA256 `672DA9132E7E91A83636836A62ADE4778781D51592A541041C511653238DEC5E`）。
   - 目标运行贴图：`src/main/resources/assets/xueximod/textures/effect/fire_spark_v3/fire_spark.png`（资源路径 `assets/xueximod/textures/effect/fire_spark_v3/fire_spark.png`），须与 `fire_spark_v3_strip.png` 逐字节一致。
   - 目标运行贴图元数据：`src/main/resources/assets/xueximod/textures/effect/fire_spark_v3/fire_spark.png.mcmeta`，配置 `blur=false`、`clamp=true`。
2. **空间朝向与渲染常量核对**：
   - `CameraFacingRegistry`：`SPARK -> CameraFacingTag.NONE`（飞行全程不面向相机旋转、不按每刻微位移重算滚转）。
   - `BatchCarrierVisual.spawnSpark`：仅在出生瞬间按归一化发射方向 `unitLaunchDirection` 锁定前向切线。
   - `BatchCarrierVisualRenderer.SPARK_FIRST_PERSON_DROP = 0.245D`：客户端渲染锚点沿 `-upAxis` 下移 `0.245` 世界格纯视觉偏置（不改服务端碰撞体起点）。
   - `BatchCarrierVisualRenderer.drawSparkCross`：单翼 `0.80×0.80` 世界格（`halfSpan = 0.40`），绕发射前向轴 `±45°` 正交穿插双面片（`firstFin = (upAxis + sideAxis) / √2`、`secondFin = (upAxis - sideAxis) / √2`）；`+half * tangent` 对应 `U = 1.0` 火头朝前，`-half * tangent` 对应 `U = 0.0` 火尾朝后；`16` 帧按存活进度单轮线性推进；`RenderType.entityCutout` + `LightTexture.FULL_BRIGHT` + `NetherDomainTint`。
3. **非运行资源隔离核对**：
   - `fire-spark-v3-1-crossed-launch-preview.bbmodel` 及场景内全部 `示意辅助*`（假人、1 格地面刻度网格、中段直射对照）与 `helper_*` 辅助贴图仅保存在资源库，严禁复制入 `src/main/resources/assets/xueximod/`。
4. **下一步交接**：
   - 交回主会话派发 Grok 执行上述确认版运行接入核对、执行最终统一 `.\gradlew.bat compileJava --offline --no-daemon`，并在满足《统一开发与术式批量制作流程》第 8 节全部条件后迁回 `待验收`。
