# 左上角生存资源条（青莲散雾法力 + 三叠疾水水势）v5 高精还原候选 — 视觉接入交接（候选稿）

- **Blockbench 适用性**：不适用。左上角生存资源条仅在二维平面 HUD 左上角显示，无空间形状、朝向、厚度、遮挡、环绕、挂载或三维运动姿态关系。
- **关联术式 B / 原 batch**：无 `docs/术式制作表.xlsx` 条目（HUD 界面常驻资源）；沿用批次 `hud-bars-20261110`（`taskId`: `hud_bars_v3_art_preview`，`attemptId`: `20261113-v5-lotus-mist-stream8`；当前正式复审计数维持：设计 `0/2`、实现 `0/3`）。
- **资源状态**：**待确认（过程归档候选版 `v5-candidate`）**。未经用户同版本确认前，不得列为正式采用版本，不得迁入 `docs/资源库/界面/` 正式目录，也不得复制到 `src/main/resources/assets/xueximod/` 替换现行 `resource_bars_v2`。
- **版本**：`resource-bars-lotus-mist-stream8-v5-candidate`
- **确认**：待用户确认（确认图二 D「青莲朝外散出云雾」16 帧动态法力图标、图三 `8` 号「三道顺流疾掠水纹」水势唯一图标、双阶梯中式回字纹桥接槽体，以及保留 `v4.1` 节奏并升采样至 `180×10` 的流云/四档水流填充动态）。

## 1. 二维源、导出与预览核对清单

### 1.1 参考概念与局部特写标准图（只读保留）
- `ref/crop_ref_D_lotus.png`（`426×257`，图二 D 局部放大标准图：盛放冰蓝青莲 + 暖金白/淡紫霞花蕊 + 古镜暗底细环 + 上下灵晶坠珠 + 向左右外侧舒展散出的流云薄雾 + 双阶梯中式回字纹桥接卡口）：`8d558063d688b4d8f7f166127e50c6e51aaad89ea86aa27cd60ca4841a59c907`
- `ref/crop_ref_8_water.png`（`302×373`，图三 `8` 号局部放大标准图：三道 S 形顺流刀锋水纹 + 幽深钴蓝起势→电光亮青斜掠→纯白炽亮腹线与上挑针尖 + 顺流青色飞沫与尾端微水珠）：`78233d54c9e22fb40ff2f120932a2b3ff5556d8a792b88f148a4ad18ea17d38c`
- `ref/ref_sheet2_eastern_BD.png`（`1774×887`，图二原版总表）：`1fd9377bee3dae50bf00c2e8d3acb1c8cf4b6cf073ff7dfc636739c2f4e66b76`
- `ref/ref_sheet3_icons_6_8_11.png`（`1774×887`，图三原版总表）：`4db3d016fb8778ac79bffbb85582195baf901b2fd9f2555a85b7163e0b1b1aa1`

### 1.2 二维可编辑源（Aseprite v1.3.17，RGB）
- `src/mana_lotus_icon_v5.aseprite`：`64×56`，纯透明底 4 图层（`mirror_ring_and_pendants`、`rear_mist_loops`、`azure_lotus_petals_and_core`、`front_outward_cloud_mist`），16 帧 @ `90ms`（周期 `1.44s` 正向无缝循环），SHA-256 `21a07b10c2cd15f50a49fb98ec436280a2be585e4c08614b0afab93e4a04fcef`
- `src/water_stream8_icon_v5.aseprite`：`48×40`，纯透明底 3 图层（`soft_deep_sea_cushion`、`three_s_stream_bodies`、`white_katana_bellies_and_spray`），16 帧 @ `90ms`，SHA-256 `7e418bff81dd80abd7df68957f8bb043437a7e6fc5ac063ddc30752eb4cab365`
- `src/hud_bars_v5_components.aseprite`：`560×260`，4 图层（`preview_dark_backing`、`fretwork_tracks_and_clasps`、`cloud_and_water_fills_16f`、`lotus_and_stream8_icons_16f`），16 帧 @ `90ms`，SHA-256 `c4e15754ccb9eaf00c3e14889719ade6a1e985b5f15d2c055d15d20c00ccc545`
- `src/hud_context_day_night.aseprite`：`1044×570`，1 帧（`2x` 原生精度昼夜实装视口），SHA-256 `0738ea6a45b40498cabf95a8013f25ecec63f7ca3a913f556c816f07dac4dc87`
- `src/states_progression.aseprite`：`872×612`，1 帧（`2x` 原生精度五态阶梯全览），SHA-256 `cf055fd9aa7d2bc0847af12cc3d77a4d5ad6ae03d8b2b5a4d6de8c16aba6e935`
- `src/lowkey_flow_preview.aseprite`：`476×336`，16 帧 @ `90ms`（`2x` 原生精度动态源），SHA-256 `35135827569cf35d473b0ece32cfd1c03538971cd236e03a671e609d24ddaaf3`
- 生成脚本：`src/build_hud_bars_v5.lua`（`98057 B`），SHA-256 `034d5281a89f1237985c0a345c409ed4c0efc4f16148639cb9e8a0979ff0286a`

### 1.3 原尺寸（`2x` 高精像素，HUD `0.5F` 半像素绘制）候选部件导出（干净透明底）
- **法力部件（还原 `crop_ref_D_lotus.png`：多层雪白亮边冰蓝青莲 + 暖金白/紫霞灵蕊 + 古镜细环与双露珠 + 16 帧朝外散出流云薄雾 + 大跨度月华流云充盈体）**：
  - `export/mana_icon_empty.png`（`64×56`，`0/max` 灵蕊熄暗无散雾静默态）：`6bec168910e21bc512967005e09406f47c3466a12a238a3afae764346ec6ba83`
  - `export/mana_icon.png`（`64×56`，`1..max-1` 常驻散雾首帧代表单图）：`e507e42d989622b596775e206c3612cb0fd7719e78b7c9163d3c561864739390`
  - `export/mana_icon_strip.png`（`64×896`，16 帧 × `56px` 纵向帧条：莲体沉稳、金蕊轻柔呼吸，左右两侧流云薄雾由莲座源源不断向外侧舒展飘散消融）：`b0138d1dbe460a1b2c9cf33c925da31f53307f6b84606b71de4cf5d3540b4219`
  - `export/mana_icon_full.png`（`64×56`，`max/max` 满盈盛放散雾首帧单图）：`c0bb8bc1c4b39ce77bc812b1128d3dfbf46cc5075dbd35a1c52ee40ccaa0e579`
  - `export/mana_icon_full_strip.png`（`64×896`，16 帧 × `56px` 满法力纵向帧条：金白辉光更盛、外散云雾更丰盈明亮）：`ee7cb5852601dd454eaa6384b2108abb90bceb0971a3802ef3a9d9c5349871c5`
  - `export/mana_track.png`（`204×20`，左端 `tx=0..17` 双阶梯中式回字纹桥接卡口 + `6px` 外露横舌，中段 `180×10` 暗底内腔，右端 `tx=198..203` 回纹收尾）：`d794835224c9a31711f9ee6c1ce1f74c19e13de526ec7f4d573c0d8543fa73ba`
  - `export/mana_fill_low.png`（`180×10`，`<=22%` 低竭收敛月影云丝）：`d2740621236f816b633d8417455f10cc39bda72c6ba819ec9ae5c2b83209a3e4`
  - `export/mana_fill_strip.png`（`180×160`，16 帧 × `10px` 常驻态大跨度月华流云充盈动画，保留 `v4.1` `76px/106px` 波长柔和节奏）：`e970363797aff324c054de7e1d3edc9e353c03bfd963687e4ff59dc6c17ddf06`
  - `export/mana_fill_full_strip.png`（`180×160`，16 帧 × `10px` 满法力凝实流云辉光帧条）：`95c3b5f5fb918e4b6332eefb7a5f4a1db65af5f329595bf8ef95f76720ea652d`
- **水势部件（唯一标准还原 `crop_ref_8_water.png` 三道顺流疾掠水纹，零黑描边 + 柔化深海气垫 + 20/50 嵌入式回纹扣 + 四档流速水脉）**：
  - `export/water_icon_idle.png`（`48×40`，`0/100` 沉静深海蓝静默态，无白炽刃光与飞沫）：`8c5dcfe06b8228ca99052d814a60935b19e8160531e8d19c72abd0efbb35419b`
  - `export/water_icon.png`（`48×40`，`1..49` 顺流态首帧单图）：`57436522fb2b02bc76bfcaeb87b48949340ced9139e5a312e9bff5a74b19be67`
  - `export/water_icon_stream_strip.png`（`48×640`，16 帧 × `40px` `1..49` 纵向帧条：三道 S 形水脉 `1x` 流速滑掠亮白腹刃与飞沫）：`57a75976b216f1bafd273069e4d8596dec5cb8efde5b879685a9dc0a844ff544`
  - `export/water_icon_surge.png`（`48×40`，`>=50` 急湍态首帧单图）：`441f624befb0f55c0df819eee01eb3d0f3d6b81fda429425bf6a6bec40c1f94a`
  - `export/water_icon_surge_strip.png`（`48×640`，16 帧 × `40px` `>=50` 纵向帧条：白炽水锋更饱满锐利，流光与飞沫按 `2x` 双倍速疾掠）：`50b89717176cdb425feab1ebe3673e90d322d531a33de5a133797f080f2e136e`
  - `export/water_track.png`（`204×20`，`<20` 基础嵌入式回纹暗扣槽）：`3961ab13336acbbbbbe9f92981618085ba9b47edbe56ea2a59cd34925f0cbc11`
  - `export/water_track_states.png`（`204×60`，3 态纵向合图：`v=0` 为 `<20` 未亮，`v=20` 为 `20..49.99` 点亮 `tx=53` 即 `36px` 处 `20` 门槛云扣，`v=40` 为 `>=50` 同时点亮 `20` 云扣与 `tx=107` 即 `90px` 处 `50` 双咬合主回纹扣）：`5e52a96274dd4a636643d72251c664d35a8fb324ec59ea3a78f8df9939a06878`
  - `export/water_fill_tier1_trickle_strip.png`（`180×160`，16 帧 × `10px` `1..19` 浅层底部慢丝，`1x` 速）：`3974afbac4fb85b7bb56602fe651a1de5d37dd31a52981a5dfae7cb0ccc949ed`
  - `export/water_fill_tier2_stream_strip.png`（`180×160`，16 帧 × `10px` `20..49` 满深双叠顺流青碧长带，`1x` 速）：`ecd0d6d35bdc5905b112cb8a26f3fe705856bef6b5c4fd1e5d70feeb1039f0c1`
  - `export/water_fill_tier3_torrent_strip.png`（`180×160`，16 帧 × `10px` `50..100` `2x` 双倍频激流白沫长水锋）：`d3272643309f9f44b5c5e13a717883931d71eb36906f507445276e76b23d80c4`
  - `export/components_overview_sheet.png`（`560×260`，仅部件总览核对，不进运行资源）：`ac63f3ed5433948c899423461c42e2a3c012751b5f1d0138dddd332653d57374`

### 1.4 二维最近邻预览（严禁作为游戏贴图加载）
- `preview/ref_vs_v5_gate.png`（`1660×1060`，原参考裁切图 `crop_ref_D_lotus.png` / `crop_ref_8_water.png` vs 本版 `v5` 暗底 vs 白昼浅蓝天空底 + 真实 HUD `0.5x` 比例 1:1 严苛门禁对照图）：`6d15e8b24fbedf403944ecd3336b97e31c66ab9a5a4e07e344cf6e57b58e5f03`
- `preview/hud_context_day_night_4x.png`（`2088×1396`，白昼天空草地底 + 洞穴深暗底双场景实装模拟大图，底部附 GUI Scale 2 `1:1` 原生像素与 GUI Scale 3 `1.5x` 真实 `0.5F` 渲染比例镶嵌窗）：`7c6f17c22440d9a118f7f414e01930f7f7245ba4e8463cbf9e8d068ed94c38a1`
- `preview/states_progression_4x.png`（`1744×1224`，法力五态 `0 / 14 / 46 / 48<-68 施法残影 / 80 满盈` 与水势五态 `0 / 12 / 35 / 68 / 100` 昼夜双底阶梯全览）：`a492b339cbdc8f92574898ccce92e48ae390a4208e31e3c495f75919fc2a7354`
- `preview/lowkey_flow_preview_4x.gif`（`952×672`，16 帧 × `90ms` 无缝循环动态预览：同步呈现法力青莲向外散出云雾 + 槽内流云充盈，以及水势 `8` 号三叠水纹 + 槽内四档流速强弱）：`da7cc2ce7d4b558c9d2529862aa95f73720d01e11785fb7576393441dad20a3b`
- `.bbmodel`：不适用（纯二维平面 HUD 界面资源，无三维空间形态、厚度或挂载姿态）。
- 实际三维视口预览：不适用（原因同上）。

## 2. 左上角 `0.5F` 半像素高清绘制坐标、UV 帧布局与分档渲染规则（供用户确认后 Grok 接入参考）

- **渲染矩阵与最近邻原则**：
  - 客户端 `GuiGraphics` 在绘制条部件与图标前执行 `pose.pushPose(); pose.scale(0.5F, 0.5F, 1.0F);`，在 `2x` 高精像素坐标系下完成 `blit` 后 `pose.popPose()`，数值文本仍按常规 `1.0F` 逻辑字体绘制。在玩家常用的 GUI Scale 2 / 3 / 4 下 `100%` 锐利对齐物理像素，逻辑占地小巧低调（法力图标逻辑尺寸仅 `32×28`、水势图标 `24×20`、回纹槽 `102×10`、内腔 `90×5`）。
  - 绘制顺序：先画 `track`（`204×20`），再在槽内填充腔 `(trackX + 18, trackY + 5)` 绘制裁剪后的 `fill`（`fillPx × 10`，`fillPx = 0..180`），最后在左侧叠加绘制 `icon`（使青莲翼瓣与外散薄雾自然跨接在回纹左舌之上，无缝咬合）。
- **法力条默认坐标（括号内为 `0.5F` 等效逻辑 GUI 坐标）**：
  - 图标：`2x` 坐标 `(10, 4)`（逻辑 `(5, 2)`），尺寸 `64×56`（逻辑 `32×28`）。`current <= 0` 取 `mana_icon_empty.png`；`0 < current < max` 从 `mana_icon_strip.png` 按 `v = frameIndex * 56` 采样 `64×56`；`current >= max` 从 `mana_icon_full_strip.png` 按 `v = frameIndex * 56` 采样 `64×56`。
  - 回纹槽：`2x` 坐标 `(62, 18)`（逻辑 `(31, 9)`），尺寸 `204×20`（逻辑 `102×10`）。
  - 填充腔：`2x` 坐标 `(80, 23)`（逻辑 `(40, 11.5)`，槽内偏移 `dx=18, dy=5`），最大尺寸 `180×10`（逻辑 `90×5`）：
    - `current <= 0`：不绘制填充。
    - `0 < current < max`：`fillPx = max(1, min(180, floor(current / max * 180 + 0.5)))`；`current / max <= 0.22` 取 `mana_fill_low.png`，否则从 `mana_fill_strip.png` 按 `v = frameIndex * 10` 采样 `fillPx × 10`。
    - `current >= max`：从 `mana_fill_full_strip.png` 按 `v = frameIndex * 10` 采样满宽 `180×10`。
  - 数值文本：逻辑坐标 `(137, 10)`，显示 `当前/上限`（建议颜色 `0xE2EEFF` 带阴影）。
- **水势条默认坐标（括号内为 `0.5F` 等效逻辑 GUI 坐标）**：
  - 图标：`2x` 坐标 `(16, 62)`（逻辑 `(8, 31)`），尺寸 `48×40`（逻辑 `24×20`）。`balance <= 0` 取 `water_icon_idle.png`；`0 < balance < 50` 从 `water_icon_stream_strip.png` 按 `v = frameIndex * 40` 采样 `48×40`；`balance >= 50` 从 `water_icon_surge_strip.png` 按 `v = frameIndex * 40` 采样 `48×40`。
  - 回纹槽：`2x` 坐标 `(62, 72)`（逻辑 `(31, 36)`），从 `water_track_states.png` 按门槛状态裁剪 `204×20`：`<20` 取 `v=0`，`20 <= balance < 50` 取 `v=20`，`balance >= 50` 取 `v=40`。
  - `20` 与 `50` 回纹云扣整除对位：内腔宽 `180px` 时，`20/100` 精确对应 `36px`（槽内 `tx = 18 + 36 - 1 = 53`），`50/100` 精确对应 `90px`（槽内 `tx = 18 + 90 - 1 = 107`），零舍入误差！
  - 填充腔：`2x` 坐标 `(80, 77)`（槽内偏移 `dx=18, dy=5`），最大尺寸 `180×10`：
    - `balance <= 0`：不绘制填充。
    - `0 < balance < 20`（涓流）：从 `water_fill_tier1_trickle_strip.png` 按 `v = frameIndex * 10` 采样 `fillPx × 10`。
    - `20 <= balance < 50`（涌流）：从 `water_fill_tier2_stream_strip.png` 按 `v = frameIndex * 10` 采样 `fillPx × 10`。
    - `balance >= 50`（激流 / 满势）：从 `water_fill_tier3_torrent_strip.png` 按 `v = frameIndex * 10` 采样 `fillPx × 10`。
  - 数值文本：逻辑坐标 `(137, 37)`；水势瞬时增减提示从逻辑 `y = 49` 起排布。
- **时间 / 循环 / 相位**：
  - 全套动态条与散雾/水流图标统一为 `16` 帧，客户端按 `(System.currentTimeMillis() / 90L) % 16`（`90ms/帧`，总周期 `1.44s`）同步采样纵向帧条。
- **非静态预览可替代的运行效果**：不同窗口分辨率与 GUI Scale（`1x`–`4x`）下的实机像素对齐、动态长数字宽度与帧率表现，须在用户确认并由 Grok 正式接入后于游戏内实测。

## 3. 待用户确认项与后续边界
1. **法力青莲散雾确认**：`crop_ref_D_lotus.png` 还原的「盛放冰蓝青莲 + 暖金紫霞莲蕊 + 16 帧朝外散出流云薄雾」+ 双阶梯中式回纹槽 + 流云充盈动画是否满意？
2. **水势 `8` 号三叠水纹确认**：`crop_ref_8_water.png` 还原的「三道顺流疾掠水纹（无黑描边 + 柔化深海暗影气垫）」+ 四档流速水脉是否满意？
3. **后续流程**：待用户明确确认后，先归档至 `docs/资源库/界面/` 正式目录并更新 `docs/资源库/README.md`，再交由 Grok 子代理按 `0.5F` 半像素方案接入 `ResourceBarLayout.java`、`MagicHudOverlay.java`、`WaterMomentumOverlay.java` 与 `assets/xueximod/textures/gui/` 并执行 `compileJava`。
