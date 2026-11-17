# 地表定位 · 棱面像素倒三角 v2

- **采用状态**：**用户已确认采用（正式归档，待 Grok 运行接入、统一编译与游戏实测）**
- **用户确认日期与原话**：`2026-11-14` 用户审阅同版本二维预览与真实 Blockbench 三维视口预览后明确回复原话：**「你先继续，我忘了地面定位之前你给出的版本我还没说采用，之前那个版本的效果可以接入」**
- **关联术式**：`rule_surface_anchor`（地表定位 / 通用 / 无属性 / 规则·触发·地表定位）
- **与旧版替换关系**：
  - 本版 `地表定位_倒三角_v2` 正式替换旧暂用版 [地表定位_v1](../地表定位_v1/README.md) 的运行时表现（移除旧版伪透视地面圆环、埋入 `Y < 0` 实心方块与单平面侧视厚度为 `0` 的问题）。
  - 旧目录 [地表定位_v1](../地表定位_v1/README.md)（2026-10-31 暂用二维归档）与过程归档 [地表定位_倒三角_v2_20261114](../../../过程归档/地表定位_倒三角_v2_20261114/visual-handoff.md) 完整保留为历史记录，不覆盖、不删除；运行端旧文件 `textures/effect/surface_anchor_v1/surface_anchor.png` 亦保留不删，运行枚举切换至 `textures/effect/surface_anchor_v2/surface_anchor.png`。

在被选中的方块碰撞顶面（`Y = 0`）正上方 `Y ∈ [+0.04, +0.84]` 世界格处，生成通用无属性高对比棱面像素倒三角定位标记（深曜石黑青硬边描边 + 暖白香槟内框 + 琥珀金切面 + 中心垂向亮白脊线与底尖直指锚点），采用 `CameraFacingTag.NONE` 世界锁定下绕竖直中轴 `±45°` 正交穿插的两张双面竖直面片（`0.80 × 0.80` 世界格，`halfSpan = 0.40F`），保证不随镜头偏转抖动且在全水平方位与俯仰视角下均清晰立体。

## 文件清单

- `surface_anchor_v2.aseprite`：`64×64` RGBA、4 图层、16 帧（`50 ms/帧`）可编辑二维像素源。`15,885` bytes，SHA256 `6EF4C6D35ED70916B1CC1BABEA0B56384EF6019B064C9757DBF9464527FFD335`。
- `surface_anchor_v2_strip.png`：`64×1024` RGBA 确认版原尺寸竖条导出（16 帧 × `64×64`，纯二值透明 `alpha ∈ {0, 255}`，关于 `U = 0.5` 严格左右对称）。`4,741` bytes，SHA256 `4434BDC7B0311E863277E9BD23761139E77489AD6B548FBB48AC202C4FB35A20`。
- `surface_anchor_v2.gif`：`256×256`（`4×`）最近邻动态循环预览（运行时单轮 `16 ticks` 播完即销毁，不循环）。`31,730` bytes，SHA256 `E71D3E77DBD1AC58D9B86F563A5013F66170467DD465530045314B1DE0695393`。
- `surface_anchor_v2_sheet.png`：`1160×740` 最近邻全 16 帧序列与草方块/浅灰石砖/夜晚深暗地面三底色可读性评审板。`22,178` bytes，SHA256 `07C3D55DCEE8CEAD082CC3437FAF6EC48F2A61286E0CA92B7A22FE529B57C020`。
- `surface-anchor-v2-inverted-triangle-preview.bbmodel`：同版真实 Blockbench `5.2.2` `free` 三维空间示意工程（内嵌纹理槽 `0` 字节与 `surface_anchor_v2_strip.png` SHA256 完全一致；非运行资源）。`40,507` bytes，SHA256 `6425325F4C775CCA61BF5460500063A7DED30EF5DCB8F5C0C2010BA48E2AFF56`。
- `surface-anchor-v2-3d-01-player-aim-view.png`：真实视口预览 01 — 第一人称施法瞄准视角（`1152×816`）。`83,690` bytes，SHA256 `9C28454F62A2FAAA85594465C2610DE85A21829B642211A840C045DF73F22DF4`。
- `surface-anchor-v2-3d-02-hero-3quarter.png`：真实视口预览 02 — 3/4 近景透视主图（`1152×816`）。`150,873` bytes，SHA256 `CB1A19A7477BB6C863750C96D41A429BE2D4FA11FD8E54EAC177BD37E219080C`。
- `surface-anchor-v2-3d-03-side-elevation.png`：真实视口预览 03 — 正交正侧视图（核对 `Y = +0.04` 离地间隙与 `1.80` 格施法假人比例，`1152×816`）。`26,324` bytes，SHA256 `609D4D59ADED6B377112B65A13A698B312181BE993C57B560ED206BC19564C7A`。
- `surface-anchor-v2-3d-04-azimuth-angles.png`：真实视口预览 04 — 东北高俯角多方位对照图（展示 `×` 正交十字四翼，`1152×816`）。`207,534` bytes，SHA256 `FD272E7CC0B050D169406D888B5C5E31A541FAA0E67C44E67D5D8DC0D063607A`。
- `visual-handoff.md`：正式确认版视觉接入交接参数规范。
- `manifest.json`：本正式目录内各文件的字节数、SHA256 与用户确认元数据清单。

## 运行贴图映射与非运行辅助隔离

- **目标运行贴图**：`src/main/resources/assets/xueximod/textures/effect/surface_anchor_v2/surface_anchor.png`（资源路径 `assets/xueximod/textures/effect/surface_anchor_v2/surface_anchor.png`），须与本目录 `surface_anchor_v2_strip.png` 逐字节一致。
- **目标运行贴图元数据**：`src/main/resources/assets/xueximod/textures/effect/surface_anchor_v2/surface_anchor.png.mcmeta`（配置 `blur=false, clamp=true`）。
- **非运行资源隔离**：`surface-anchor-v2-inverted-triangle-preview.bbmodel` 及场景内全部 `示意辅助*`（目标草方块、石砖地面参照网格、`1.80` 格施法玩家假人）与 `helper_*` 辅助贴图仅保存在资源库供空间核查，严禁进入 `src/main/resources/assets/xueximod/`。完整空间坐标、UV 与双面片顶点规范见 [visual-handoff.md](visual-handoff.md)。
