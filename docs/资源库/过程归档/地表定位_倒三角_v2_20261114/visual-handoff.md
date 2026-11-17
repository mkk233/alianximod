# 视觉接入交接：`rule_surface_anchor`（地表定位 v2 · 倒三角）

```text
Blockbench 适用性：适用（地表定位标记悬停于选中方块顶面上方，涉及 Y=0 顶面间隙、1×1×1 方块比例、CameraFacingTag.NONE 多方位可见性及正交十字穿插关系；已在真实 Blockbench Desktop 5.2.2 搭建空间示意并从实际视口导出 4 张多角度 PNG）
关联术式 B / 原 batch：rule_surface_anchor（地表定位） / 待制作-20261031（docs/术式制作表.xlsx SHA256 AB344CCAAFBE723AD0ED6C6DD0834EB3B264EC5184227F655FAE9FC1B0F12C26，制作中第 3 行）
资源状态：待确认（过程归档候选稿；未经用户同版本确认前不得列为正式采用版、不得迁入正式资源目录或替换 src/ 运行资源，也不能作为迁待验收交付）
版本：v2（地表定位_倒三角_v2_20261114，attemptId 20261114-art-surface-anchor-v2-01）
确认：待用户确认本版同版本二维预览（surface_anchor_v2_sheet.png、surface_anchor_v2.gif）与真实 Blockbench 三维视口预览（surface-anchor-v2-3d-01..04.png）；确认后补充时间与回复依据
二维源：D:/alianximod/docs/资源库/过程归档/地表定位_倒三角_v2_20261114/surface_anchor_v2.aseprite（15,885 bytes，SHA256 6EF4C6D35ED70916B1CC1BABEA0B56384EF6019B064C9757DBF9464527FFD335）；64×64 RGBA；4 个可编辑命名图层（1_Outer_Border、2_Prism_Body、3_Inner_Core_Spine、4_Specular_And_Tip）；16 帧（Frame 0..15 自上而下对应动画序列）；每帧 50 ms（总长 800 ms）；运行时单次触发播放 1 轮后销毁，预览 GIF 循环仅用于审阅
二维导出：D:/alianximod/docs/资源库/过程归档/地表定位_倒三角_v2_20261114/surface_anchor_v2_strip.png（4,741 bytes，SHA256 4434BDC7B0311E863277E9BD23761139E77489AD6B548FBB48AC202C4FB35A20）；64×1024 RGBA 竖条 PNG（16 帧 × 64×64），纯二值透明 alpha ∈ {0, 255}（nonBinaryCount = 0，不透明像素总数 16,086），严格关于 X = 31.5（U = 0.5）左右对称
二维预览：
  - D:/alianximod/docs/资源库/过程归档/地表定位_倒三角_v2_20261114/surface_anchor_v2_sheet.png（22,178 bytes，SHA256 07C3D55DCEE8CEAD082CC3437FAF6EC48F2A61286E0CA92B7A22FE529B57C020；1160×740 最近邻评审板，含 F00..F15 全帧、F06 4× 主帧、色板及白天草方块/白天浅色石砖/夜晚深色地面可读性对比）
  - D:/alianximod/docs/资源库/过程归档/地表定位_倒三角_v2_20261114/surface_anchor_v2.gif（31,730 bytes，SHA256 E71D3E77DBD1AC58D9B86F563A5013F66170467DD465530045314B1DE0695393；256×256 即 4× 最近邻循环预览）
素材：D:/alianximod/docs/资源库/过程归档/地表定位_倒三角_v2_20261114/surface_anchor_v2_strip.png（SHA256 4434BDC7B0311E863277E9BD23761139E77489AD6B548FBB48AC202C4FB35A20；与 .bbmodel 纹理槽 0 内嵌 PNG 解码字节及 SHA256 完全一致）
.bbmodel：D:/alianximod/docs/资源库/过程归档/地表定位_倒三角_v2_20261114/surface-anchor-v2-inverted-triangle-preview.bbmodel（40,507 bytes，SHA256 6425325F4C775CCA61BF5460500063A7DED30EF5DCB8F5C0C2010BA48E2AFF56；Blockbench 5.2.2 free 通用模型，工程 UUID 19a21c6d-556d-a2cf-167b-83abca62ed0a）
实际预览（均导出自真实 Blockbench Desktop 5.2.2 视口，1152×816，antialias=false，展示 F06 稳定悬停帧）：
  - surface-anchor-v2-3d-01-player-aim-view.png（83,690 bytes，SHA256 9C28454F62A2FAAA85594465C2610DE85A21829B642211A840C045DF73F22DF4；第一人称施法眼高瞄准视角：position [-59.6, 25.92, 0]，target [0, 3.5, 0]，perspective，fov 56）
  - surface-anchor-v2-3d-02-hero-3quarter.png（150,873 bytes，SHA256 CB1A19A7477BB6C863750C96D41A429BE2D4FA11FD8E54EAC177BD37E219080C；3/4 近景透视主图：position [20, 24, 38]，target [-13, 8.5, -2]，perspective，fov 46）
  - surface-anchor-v2-3d-03-side-elevation.png（26,324 bytes，SHA256 609D4D59ADED6B377112B65A13A698B312181BE993C57B560ED206BC19564C7A；正交正侧视图：position [-24, 6.5, 180]，target [-24, 6.5, 0]，orthographic，zoom 0.27，near=171.95 沿 Z=+8.05 正侧基准切面）
  - surface-anchor-v2-3d-04-azimuth-angles.png（207,534 bytes，SHA256 FD272E7CC0B050D169406D888B5C5E31A541FAA0E67C44E67D5D8DC0D063607A；东北高俯角多方位对照图：position [18, 42, -32]，target [-6, 2.5, 0]，perspective，fov 44）

换算：16.0 模型单位 = 1.0 Minecraft 世界格（1.0 模型单位 = 0.0625 世界格 = 1/16 方块像素）
轴向与前向：+Y 为世界上方（Direction.UP）；维持 CameraFacingTag.NONE（世界锁定，不随镜头偏航或俯仰旋转，无单一水平前向；采用绕竖直中轴 ±45° 对角正交穿插的两张双面竖直面片）
原点 / 锚点 / 旋转中心：模型原点 [0, 0, 0] 对应 SurfaceAnchor.resolve 返回的方块顶面命中点 sample.point() = (x, y, z)，其中 Y = 0 恰为被选中方块的碰撞顶面（无预抬高）；两张面片绕 [0, 0.64, 0]（世界 (point.x, point.y + 0.04, point.z)）竖直中轴分别旋转 +45° 与 -45°
父子层级：无骨骼父子动画；运行时由 BatchCarrierVisualRenderer 在实体原点 (point.x, point.y, point.z) 直接构建两张正交对角竖直双面面片（anchor_blade_diag_plus45 与 anchor_blade_diag_minus45）
局部坐标与世界坐标：
  - 选中目标方块参照：模型 [-8, -16, -8] ~ [8, 0, 8] 对应世界 [-0.5, -1.0, -0.5] ~ [+0.5, 0.0, +0.5] 格（顶面位于局部 Y = 0.0）
  - 倒三角标记主体：局部竖直范围 Y ∈ [+0.04, +0.84] 世界格（模型 Y ∈ [+0.64, +13.44]），完全位于 Y = 0 顶面上方，留有 +0.04 格（0.64 模型单位）防穿模与防 Z-fighting 间隙，绝不伸入 Y < 0 实心方块
尺寸 / 偏移 / 朝向：
  - 单张面片宽 0.80 世界格（halfSpan = 0.40F，模型 [-6.4, +6.4] = 12.8 单位）× 高 0.80 世界格（模型 [0.64, 13.44] = 12.8 单位），保持 1:1 等比映射 64×64 帧，收束在 1×1 方块顶面之内
  - 对角半偏移 d = halfSpan * INV_SQRT_2 = 0.40F * 0.70710677F = 0.2828427F 世界格（模型 ±4.5255）：
    * 平面 A（+45°）：(-d, yBottom..yTop, -d) <-> (+d, yBottom..yTop, +d)
    * 平面 B（-45°）：(-d, yBottom..yTop, +d) <-> (+d, yBottom..yTop, -d)
UV / 帧布局：
  - 贴图总尺寸 64×1024，共 16 帧（frame ∈ 0..15），每帧 64×64
  - 竖直 V：topV = frame / 16.0F 必须绑定在 yTop（+0.84F 上沿），bottomV = (frame + 1) / 16.0F 必须绑定在 yBottom（+0.04F 下沿），确保倒三角宽顶在上、尖端朝 -Y 直指方块顶面锚点 (0, 0, 0)（Blockbench 预览帧 F06 对应像素 UV [0, 384, 64, 448]，即 V ∈ [6/16, 7/16]）
  - 水平 U：u0 = 0.0F 至 u1 = 1.0F；因每帧严格关于 U = 0.5 左右对称，两张面片在中轴 (0, y, 0) 处脊线与底尖完全重合无错位
材质 / 透明：
  - 贴图采用严格二值透明 alpha ∈ {0, 255}（无半透明边缘），运行时建议使用 RenderType.entityCutoutNoCull(sheet)（或在 RenderType.entityCutout(sheet) 下对每张对角面显式提交正反两面共 4 个单向 quad = 16 顶点，防止默认背面剔除导致背向缺片），消除半透明深度排序穿插伪影
  - 颜色与光照：通用无属性标记，acceptsFlameTint(SURFACE) 维持 false（纯白顶点色 255,255,255,255，不走 NetherDomainTint 火焰染色）；光照建议对 SURFACE 采用 LightTexture.FULL_BRIGHT，保证夜晚无火把地面选中方块同样清晰醒目
时间 / 循环 / 相位：
  - 运行时长固定 16 ticks（0.80 s），每 tick 推进 1 帧（age 0..15 -> Frame 0..15），单次播放完毕即 discard 销毁，不循环
  - 帧阶段：Frame 0..2 自上方短促下压落位；Frame 3..11 在顶面上方稳定悬停（含 1 px 轻脉动与棱面高光流转）；Frame 12..15 保持尖端锁定锚点的同时阶梯收窄消隐
跟随对象与运动绑定：出生后锁定在 BatchTriggerVisuals.surface(level, sample.point()) 传入的世界坐标 sample.point()，不跟随施法者或目标实体移动，不随镜头旋转（CameraFacingTag.NONE）
预览速度：surface_anchor_v2.aseprite 与 surface_anchor_v2.gif 设为 50 ms/帧（16 帧 = 0.8 s）循环播放仅用于审阅动态节奏；运行时不循环，亦不代表前摇或施法读条时长

待替换占位：
  - 运行时旧占位贴图：src/main/resources/assets/xueximod/textures/effect/surface_anchor_v1/surface_anchor.png（4,058 bytes，SHA256 7801A4513A63D26DAA8567F06A33834E46FD9F0AD71F5CDA3CE7F471AC39D018）及 BatchVisualTexture.SURFACE 枚举路径与旧单平面居中 billboard 几何（halfSpan = 0.75F，y ∈ [-0.75, +0.75] 埋入方块且 ±X 侧视厚度为 0）
  - 资源库旧 v1 目录：docs/资源库/特效/标记/地表定位_v1/（本单严禁覆盖或改动，待用户确认 v2 后按归档规范新增 v2 正式目录并更新索引）
运行资源：待用户确认后，计划归档至 docs/资源库/特效/标记/地表定位_v2/ 并复制 surface_anchor_v2_strip.png 至 src/main/resources/assets/xueximod/textures/effect/surface_anchor_v2/surface_anchor.png（对应 BatchVisualTexture.SURFACE），同时在 BatchTriggerVisuals.surface（halfSpan = 0.40F）与 BatchCarrierVisualRenderer（SURFACE + CameraFacingTag.NONE 正交对角竖直十字双面片，y ∈ [+0.04, +0.84]）接入
接入回执（由 Grok 接入者补充）：未接入（当前处于待用户确认阶段，未改动 src/）
迁待验收：当前不可迁待验收；须待用户明确确认本版同版本二维/三维预览、完成正式资源库归档、并由 Grok 完成确认版运行接入与 compileJava 后，按项目流程第 8 节核对全部条件
示意辅助：分组 `示意辅助`（UUID 85f94794-30d7-ffa0-95f1-490feb4ff0ec）及其子组 `示意辅助_选中目标方块_顶面Y0`（含 `示意辅助_target_grass_block`）、`示意辅助_地面参照网格_顶面Y0`（含 27 块 `示意辅助_grid_*` 石砖地面与步道）、`示意辅助_施法玩家假人_28_8高`（含 `示意辅助_dummy_*` 共 7 个部件，高 28.8 单位 = 1.80 格，眼高 25.92 单位 = 1.62 格）以及 `helper_*` 辅助贴图全部仅用于空间比例与瞄准视线参照，不进运行资源
Blockbench 不能代表的运行效果：
  1. 真实游戏内 `SurfaceAnchor.resolve` 的 32 格视线遮挡判定、非隔墙选点拒绝释放逻辑与不规则碰撞箱（台阶/半砖/楼梯）顶面采样高度；
  2. Forge 客户端 `BatchCarrierVisual` 实体每 tick 帧推进生命周期（16 ticks 自动销毁）及 `RenderType` 与游戏内光影/雾效交互；
  3. 与后续攻击术式（如灼地、火山喷发）同帧出生时的粒子叠加层次，须以接入后用户实机测试为准。
```
