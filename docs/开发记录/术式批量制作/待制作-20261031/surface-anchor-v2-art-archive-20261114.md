# 美术正式归档回执 — 地表定位 v2（棱面像素倒三角 · 用户确认采用版）

## 1. 任务头与边界回执
- **工作区**：`D:\alianximod`
- **batchId**：`待制作-20261031`
- **taskId**：`rule_surface_anchor_art_v2`
- **attemptId**：`20261114-art-surface-anchor-v2-archive-01`
- **前序依赖**：`20261114-art-surface-anchor-v2-01`（已完成回执 `D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/surface-anchor-v2-preview-20261114.md`）
- **stage**：`delivery`（用户同版本确认后的正式资源库归档与索引更新）
- **操作角色**：`art`（兼本次 `D:/alianximod/docs/资源库/README.md` 指定唯一索引更新作者）
- **真实直接父代理**：主会话
- **关联术式 B**：`rule_surface_anchor`（地表定位）
- **授权来源与用户确认依据**：`2026-11-14` 用户在审阅 `地表定位_倒三角_v2_20261114` 二维与真实 Blockbench 三维视口预览后，明确回复原话：**「你先继续，我忘了地面定位之前你给出的版本我还没说采用，之前那个版本的效果可以接入」**
- **复审计数**：设计 `2/2`、实现 `3/3`（归档不计复审轮次、不重置计数）
- **编译 / 制作表写回权限**：无
- **边界与禁区核验**：
  - 全程未搜索、未调用任何 Blockbench MCP 或 Aseprite MCP（零干扰并行美术会话）。
  - 仅写入授权的三条独占路径：`D:/alianximod/docs/资源库/特效/标记/地表定位_倒三角_v2/`、`D:/alianximod/docs/资源库/README.md` 与本回执文件。
  - 只读源目录 `D:/alianximod/docs/资源库/过程归档/地表定位_倒三角_v2_20261114/`（11 个文件）与历史暂用目录 `D:/alianximod/docs/资源库/特效/标记/地表定位_v1/`（6 个文件）原样保留，字节数与 SHA256 零改动。
  - 未修改或覆盖 `src/`、`docs/术式制作表.xlsx`、`记录.md`、`实测清单.md`；未运行 `gradlew`、未打包、未启动游戏。

## 2. 正式归档目录文件清单与 SHA256 校验
正式归档目录：`D:/alianximod/docs/资源库/特效/标记/地表定位_倒三角_v2/`（共 12 个文件；其中 1–9 号二维/三维源与预览均逐字节复制自过程归档且实测 SHA256 `100%` 一致，`manifest.json` 内记录的其余全部 11 个文件字节数与 SHA256 均已实测 `100%` 匹配）：

1. `D:/alianximod/docs/资源库/特效/标记/地表定位_倒三角_v2/surface_anchor_v2.aseprite`
   - 角色：二维分层可编辑像素源（`64×64` × 16 帧，4 层；逐字节复制自过程归档，哈希一致）
   - 大小：`15,885` bytes
   - SHA256：`6EF4C6D35ED70916B1CC1BABEA0B56384EF6019B064C9757DBF9464527FFD335`
2. `D:/alianximod/docs/资源库/特效/标记/地表定位_倒三角_v2/surface_anchor_v2_strip.png`
   - 角色：二维确认版原尺寸竖条导出（`64×1024` RGBA，16 帧 × `64×64`，纯二值透明；逐字节复制自过程归档，哈希一致）
   - 大小：`4,741` bytes
   - SHA256：`4434BDC7B0311E863277E9BD23761139E77489AD6B548FBB48AC202C4FB35A20`
3. `D:/alianximod/docs/资源库/特效/标记/地表定位_倒三角_v2/surface_anchor_v2.gif`
   - 角色：二维 `4×` 最近邻循环动画预览（`256×256`；逐字节复制自过程归档，哈希一致）
   - 大小：`31,730` bytes
   - SHA256：`E71D3E77DBD1AC58D9B86F563A5013F66170467DD465530045314B1DE0695393`
4. `D:/alianximod/docs/资源库/特效/标记/地表定位_倒三角_v2/surface_anchor_v2_sheet.png`
   - 角色：二维全 16 帧序列与草方块/浅灰石砖/夜晚深暗地面可读性评审板（`1160×740`；逐字节复制自过程归档，哈希一致）
   - 大小：`22,178` bytes
   - SHA256：`07C3D55DCEE8CEAD082CC3437FAF6EC48F2A61286E0CA92B7A22FE529B57C020`
5. `D:/alianximod/docs/资源库/特效/标记/地表定位_倒三角_v2/surface-anchor-v2-inverted-triangle-preview.bbmodel`
   - 角色：同版真实 Blockbench `5.2.2` 三维空间示意工程（逐字节复制自过程归档，内嵌纹理槽 `0` 与竖条 SHA256 一致；非运行资源）
   - 大小：`40,507` bytes
   - SHA256：`6425325F4C775CCA61BF5460500063A7DED30EF5DCB8F5C0C2010BA48E2AFF56`
6. `D:/alianximod/docs/资源库/特效/标记/地表定位_倒三角_v2/surface-anchor-v2-3d-01-player-aim-view.png`
   - 角色：真实视口预览 01 — 第一人称施法瞄准视角（`1152×816`；逐字节复制自过程归档，哈希一致）
   - 大小：`83,690` bytes
   - SHA256：`9C28454F62A2FAAA85594465C2610DE85A21829B642211A840C045DF73F22DF4`
7. `D:/alianximod/docs/资源库/特效/标记/地表定位_倒三角_v2/surface-anchor-v2-3d-02-hero-3quarter.png`
   - 角色：真实视口预览 02 — 3/4 近景透视主图（`1152×816`；逐字节复制自过程归档，哈希一致）
   - 大小：`150,873` bytes
   - SHA256：`CB1A19A7477BB6C863750C96D41A429BE2D4FA11FD8E54EAC177BD37E219080C`
8. `D:/alianximod/docs/资源库/特效/标记/地表定位_倒三角_v2/surface-anchor-v2-3d-03-side-elevation.png`
   - 角色：真实视口预览 03 — 正交正侧视图（`1152×816`；逐字节复制自过程归档，哈希一致）
   - 大小：`26,324` bytes
   - SHA256：`609D4D59ADED6B377112B65A13A698B312181BE993C57B560ED206BC19564C7A`
9. `D:/alianximod/docs/资源库/特效/标记/地表定位_倒三角_v2/surface-anchor-v2-3d-04-azimuth-angles.png`
   - 角色：真实视口预览 04 — 东北高俯角多方位对照图（`1152×816`；逐字节复制自过程归档，哈希一致）
   - 大小：`207,534` bytes
   - SHA256：`FD272E7CC0B050D169406D888B5C5E31A541FAA0E67C44E67D5D8DC0D063607A`
10. `D:/alianximod/docs/资源库/特效/标记/地表定位_倒三角_v2/visual-handoff.md`
    - 角色：正式确认版视觉接入交接参数说明
    - 大小：`12,192` bytes
    - SHA256：`1BA3B21214D2CA2AE7DCAF9E28BAA31A778C1DBFEAE66C6F127DFBEDCCB2C749`
11. `D:/alianximod/docs/资源库/特效/标记/地表定位_倒三角_v2/README.md`
    - 角色：正式归档目录说明文档
    - 大小：`5,142` bytes
    - SHA256：`7FBA4F2AAB7413BB414E8FCE2B0E4BEA7212DB64C573041CC6FEF234E74FA8AC`
12. `D:/alianximod/docs/资源库/特效/标记/地表定位_倒三角_v2/manifest.json`
    - 角色：正式归档确认元数据与其余 11 项文件精确字节数/SHA256 清单
    - 大小：`7,445` bytes
    - SHA256：`85603BFBDD5898DD15E518CA7B69BED29857801AE019A987377E0E74F3A89B62`

## 3. `docs/资源库/README.md` 索引更新前后哈希对照
- **文件路径**：`D:/alianximod/docs/资源库/README.md`
- **更新前（开工前实时校验）**：`31,505` bytes，SHA256 `02DC69DC7505CC920D1D21B617D689246ED17A20DBF605FBBEA65BAB0BA27A04`（UTF-8 无 BOM，`161` 处 CRLF / `162` 拆分行）
- **更新后（写入后实时校验）**：`32,723` bytes，SHA256 `7F2C13692D20486BA480A48EE47E4672A7DA9690F2BEB4A0BF7CF4066EA747BE`（UTF-8 无 BOM，`163` 处 CRLF / `164` 拆分行）
- **变更摘要**：
  1. 在 `## 当前资源` 顶部新增 `[地表定位 · 棱面像素倒三角 v2（用户已确认采用，运行接入中，待游戏实测）](特效/标记/地表定位_倒三角_v2/README.md)` 正式条目。
  2. 在原 `[新十三式二维 v1（暂用）]` 条目末尾（紧接留焰 v3 替换说明之后）补一句说明：`地表定位运行贴图现已确认升级为 `地表定位_倒三角_v2`（`textures/effect/surface_anchor_v2/surface_anchor.png`），`地表定位_v1` 目录保留为历史。`
  3. 其余既有条目一字未改、完整保留。

## 4. 待 Grok 执行的确认版运行资源与渲染参数映射清单
1. **运行贴图文件与元数据接入**：
   - 确认版源导出：`D:/alianximod/docs/资源库/特效/标记/地表定位_倒三角_v2/surface_anchor_v2_strip.png`（`4,741` bytes，SHA256 `4434BDC7B0311E863277E9BD23761139E77489AD6B548FBB48AC202C4FB35A20`）。
   - 目标运行贴图：`src/main/resources/assets/xueximod/textures/effect/surface_anchor_v2/surface_anchor.png`（资源路径 `assets/xueximod/textures/effect/surface_anchor_v2/surface_anchor.png`），须与 `surface_anchor_v2_strip.png` 逐字节一致。
   - 目标运行贴图元数据：`src/main/resources/assets/xueximod/textures/effect/surface_anchor_v2/surface_anchor.png.mcmeta`（UTF-8 无 BOM，配置 `blur=false, clamp=true`）。
   - 旧运行文件 `src/main/resources/assets/xueximod/textures/effect/surface_anchor_v1/surface_anchor.png` 保留不删，仅将 `BatchVisualTexture.SURFACE` 切换至 `textures/effect/surface_anchor_v2/surface_anchor.png`。
2. **空间朝向与正交十字双面片几何接入**：
   - `CameraFacingRegistry`：`SURFACE -> CameraFacingTag.NONE`（锁定世界坐标，不随镜头偏航或俯仰旋转）。
   - `BatchTriggerVisuals.surface`：以 `sample.point()` 为 `(0, 0, 0)` 顶面锚点，`halfSpan = 0.40F`（单翼宽高 `0.80 × 0.80` 世界格），生命周期 `16 ticks` 单轮播完销毁。
   - `BatchCarrierVisualRenderer`（针对 `SURFACE`）：
     - 竖直区间 `yBottom = +0.04F`、`yTop = +0.84F` 世界格（完全悬于 `Y = 0` 方块顶面上方，留 `+0.04` 格防穿模/防 Z-fighting 间隙）。
     - 绕竖直中轴 `±45°` 正交对角穿插两张双面竖直面片（对角半偏移 `d = 0.40F * INV_SQRT_2 = 0.2828427F`）：平面 A `(-d, yBottom..yTop, -d) <-> (+d, yBottom..yTop, +d)`，平面 B `(-d, yBottom..yTop, +d) <-> (+d, yBottom..yTop, -d)`。
     - UV 映射：`topV = frame / 16.0F` 绑定 `yTop (+0.84F)`，`bottomV = (frame + 1) / 16.0F` 绑定 `yBottom (+0.04F)`，`u ∈ [0.0F, 1.0F]`。
     - 材质与光照：二值透明裁切（`RenderType.entityCutoutNoCull` 或 `entityCutout` 下双面显式 4 quad = 16 顶点）、`acceptsFlameTint(SURFACE) = false`（纯白顶点色 `255,255,255,255`）、`LightTexture.FULL_BRIGHT`。
3. **非运行资源隔离要求**：
   - `surface-anchor-v2-inverted-triangle-preview.bbmodel` 及场景内全部 `示意辅助*`（目标草方块、石砖地面参照网格、`1.80` 格施法玩家假人）与 `helper_*` 辅助贴图仅保存在资源库，严禁进入 `src/main/resources/assets/xueximod/`。
4. **下一步交接**：
   - 交回主会话派发 Grok 执行上述确认版运行贴图复制、渲染几何接入与统一 `.\gradlew.bat compileJava --offline --no-daemon`，并在满足《统一开发与术式批量制作流程》第 8 节全部条件后迁回 `待验收`。
