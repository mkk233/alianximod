# 美术制作与二维/三维空间示意回执：`rule_surface_anchor`（地表定位 v2 · 倒三角）

## 1. 任务头与边界核验

- **工作区**：`D:\alianximod`
- **batchId**：`待制作-20261031`
- **taskId**：`rule_surface_anchor_art_v2`
- **attemptId**：`20261114-art-surface-anchor-v2-01`
- **stage**：`implementation`（正式美术候选稿制作 | Aseprite 二维像素 + 真实 Blockbench 三维空间示意预览 | **待用户确认**）
- **操作角色**：`art`（遵循 `C:/Users/baishang/.codex/skills/xueximod-art-preview/SKILL.md` 与 `references/visual-handoff.md`）
- **关联术式 B**：`rule_surface_anchor`（`地表定位` / `通用` / `无` / `规则 · 触发 · 地表定位`）
- **输入表校验**：`D:/alianximod/docs/术式制作表.xlsx`（SHA256 `AB344CCAAFBE723AD0ED6C6DD0834EB3B264EC5184227F655FAE9FC1B0F12C26`，`制作中` 第 3 行）
- **正式复审计数**：设计 `2/2`、实现 `3/3`（本次美术制作与预览不计正式复审轮次、不重置计数）
- **写入范围与禁区核验**：
  - 仅写入 `D:/alianximod/docs/资源库/过程归档/地表定位_倒三角_v2_20261114/` 与本回执文件；
  - **未改动** `src/`、Java 源码、`docs/术式制作表.xlsx`、`记录.md`、`实测清单.md`、`docs/资源库/README.md`、`docs/资源库/特效/标记/地表定位_v1/`；
  - **未执行** `gradlew` 编译、打包或启动游戏。

## 2. 美术 MCP 连接与既有工程保护审计

- **Aseprite MCP（`mcp__magpie__aseprite`）**：通过 `status` 查询可达，调用 `D:/Aseprite/Aseprite.exe`（`v1.3.17`，API `39`，`batch` 模式）完成 `64×64×16f` 分层绘制、竖条导出、动态 GIF 与多底色评审板导出，无第三方生图或拼贴冒充。
- **Blockbench MCP（`mcp__magpie__blockbench`）**：连接真实 Blockbench Desktop `5.2.2`，新建独立 `free` 通用模型工程 `surface-anchor-v2-inverted-triangle-preview`（UUID `19a21c6d-556d-a2cf-167b-83abca62ed0a`）。
- **既有打开工程零触碰核验**（前后只读审计 `ModelProject.all` 完全一致，未关闭、未修改、未覆盖）：
  1. `cool-v4.3-equal-width-steam-ground-wave-preview`（`fe1b1c81-3962-2fbb-bc94-6a37e1b6e5ef`，`saved: true`，`29` elements）
  2. `flame-trail-v1-spatial-draft-20261112`（`e380502f-05e4-5cab-c7a2-4ff5f7e3c7c5`，`saved: true`，`514` elements）
  3. `flame-trail-v2-projectile-composite-draft-20261112`（`8a58e270-0fce-3a38-f8c5-528d850c4a39`，`saved: true`，`109` elements）
  4. `flame-trail-v3-projectile-composite`（`8cdb8be1-7355-a034-3a20-7ae41a0e5af1`，`saved: true`，`80` elements）
  5. `fire-spark-v3-1-crossed-launch-preview`（`3cc70eec-3c0d-db2e-731f-2581790ce09d`，`saved: false`，`51` elements）
  6. `fire-upwell-parabolic-motion-preview`（`6cfd19e7-972f-1e54-9c0f-bd74cd037ea9`，`saved: false`，`153` elements）
- **视口清理**：4 张多角度 PNG 从真实 Blockbench 视口无损导出并经 `view_image` 逐张自检后，已调用 `delete_offscreen_view` 销毁临时离屏视口 `v2_cam`（当前仅剩默认 `["media", "no_aa_media", "main"]`），主视口 `main` 停靠于 3/4 近景透视主图机位。

## 3. v1 问题根因与 v2 视觉/空间方案总结

1. **彻底移除伪透视地面圆环，改为纯正视棱面像素倒三角（`▼`）**：
   - 采用通用无属性高对比定位配色：`2 px` 深曜石黑青描边（`#141822` / `#262D3E`）+ 连续暖白香槟内高光框（`#FFF4CA`）+ 琥珀金切面外框（`#F6C44A` / `#E09C2A` / `#BC7418`）+ 深灰蓝凹槽（`#242B3C`）+ 中心 `2 px` 暖白垂向中脊与指向尖端（`#FFFDF0`）。
   - 白天翠绿草方块、白天浅灰石砖、夜晚深暗地面三种底色下轮廓与指向尖端均清晰醒目；不受火焰通道 `NetherDomainTint` 染色。
2. **严格二值透明与 `16` 帧节奏（`64×1024` 竖条，`50 ms/帧`）**：
   - 实测 `alpha ∈ {0, 255}`（`nonBinaryCount = 0`，不透明像素 `16,086`），全帧关于 `X = 31.5`（`U = 0.5`）严格左右对称。
   - `Frame 0..2`：自上方短促落位定锚；`Frame 3..11`：在方块顶面上方稳定悬停直指锚点，伴随 `1 px` 轻脉动与切面流光；`Frame 12..15`：底尖持续锁住锚点的同时做干净像素阶梯收束消隐。
3. **解决 `CameraFacingTag.NONE` 下埋地与侧视消失的三维空间结构**：
   - 维持用户要求的 `CameraFacingTag.NONE`（不随玩家镜头旋转抖动）。
   - 以选中方块顶面命中点 `sample.point()` 为 `Y = 0` 基准，面片竖直区间置于 `Y ∈ [+0.04, +0.84]` 世界格（模型 `Y ∈ [+0.64, +13.44]`，高 `0.80` 格），彻底消除旧版 `[-0.75, 0]` 埋入实心方块与顶面 Z-fighting。
   - 水平宽度 `0.80` 世界格（`halfSpan = 0.40F`，模型 `12.8` 单位），采用绕竖直中轴 `±45°` 正交穿插的两张双面竖直面片：两片中轴脊线与底部尖端在 `(0, y, 0)` 严丝合缝交汇，从正东西、正南北、3/4 斜向与高俯角观察均呈饱满立体四翼倒三角，彻底消除单平面侧视厚度为 0 的问题。

## 4. 产出清单与 SHA256 校验（全部位于过程归档目录）

目录：`D:/alianximod/docs/资源库/过程归档/地表定位_倒三角_v2_20261114/`

| 文件名 | 用途 | 规格 | 字节数 | SHA256 |
| --- | --- | --- | ---: | --- |
| `surface_anchor_v2.aseprite` | Aseprite 分层可编辑源 | `64×64` × 16 帧（4 层） | `15,885` | `6EF4C6D35ED70916B1CC1BABEA0B56384EF6019B064C9757DBF9464527FFD335` |
| `surface_anchor_v2_strip.png` | 原尺寸运行时候选竖条贴图 | `64×1024` RGBA（二值透明） | `4,741` | `4434BDC7B0311E863277E9BD23761139E77489AD6B548FBB48AC202C4FB35A20` |
| `surface_anchor_v2.gif` | 最近邻动态循环预览 | `256×256`（`4×`） | `31,730` | `E71D3E77DBD1AC58D9B86F563A5013F66170467DD465530045314B1DE0695393` |
| `surface_anchor_v2_sheet.png` | 16 帧序列 + 三底色可读性评审板 | `1160×740` | `22,178` | `07C3D55DCEE8CEAD082CC3437FAF6EC48F2A61286E0CA92B7A22FE529B57C020` |
| `surface-anchor-v2-inverted-triangle-preview.bbmodel` | Blockbench 真实三维空间示意工程（内嵌同版贴图 SHA256 一致） | `free`（40 cubes / 5 groups） | `40,507` | `6425325F4C775CCA61BF5460500063A7DED30EF5DCB8F5C0C2010BA48E2AFF56` |
| `surface-anchor-v2-3d-01-player-aim-view.png` | 真实视口 1：第一人称施法瞄准视角 | `1152×816` PNG | `83,690` | `9C28454F62A2FAAA85594465C2610DE85A21829B642211A840C045DF73F22DF4` |
| `surface-anchor-v2-3d-02-hero-3quarter.png` | 真实视口 2：3/4 近景透视主图（含施法假人与目标草方块） | `1152×816` PNG | `150,873` | `CB1A19A7477BB6C863750C96D41A429BE2D4FA11FD8E54EAC177BD37E219080C` |
| `surface-anchor-v2-3d-03-side-elevation.png` | 真实视口 3：正交正侧视图（核对 `Y=0` 离地间隙与 `1.8` 格假人比例） | `1152×816` PNG | `26,324` | `609D4D59ADED6B377112B65A13A698B312181BE993C57B560ED206BC19564C7A` |
| `surface-anchor-v2-3d-04-azimuth-angles.png` | 真实视口 4：东北高俯角多方位对照图（展示 `×` 正交四翼不退化） | `1152×816` PNG | `207,534` | `FD272E7CC0B050D169406D888B5C5E31A541FAA0E67C44E67D5D8DC0D063607A` |
| `visual-handoff.md` | 完整视觉接入交接参数说明 | UTF-8 Markdown | `10,740` | `92FE82B4A2B32EDD0386C581E24DDBF608C8B1AE44509623FE2D5990C17C4872` |
| `manifest.json` | 产物元数据与既有工程保护审计清单 | UTF-8 JSON | `5,686` | `443A105C9285E5B80168E41D56B1C4F48CF0A0A25AE27A362C95EC03A1F83C49` |

## 5. 当前状态与后续衔接说明

- **当前状态**：**美术候选稿待用户确认**（`rule_surface_anchor` 仍留 `制作中`；未进正式资源目录、未替换 `src/main/resources/assets/xueximod/textures/effect/surface_anchor_v1/surface_anchor.png`、未运行 `compileJava`、未做游戏内验证）。
- **下一步**：请主会话向用户展示上述二维评审板/动图与 4 张真实 Blockbench 视口截图；待用户明确确认该同版本效果后，再执行正式资源库归档并派发 Grok 按 `visual-handoff.md` 参数完成运行时正交十字双面片与贴图替换及 `compileJava`。
