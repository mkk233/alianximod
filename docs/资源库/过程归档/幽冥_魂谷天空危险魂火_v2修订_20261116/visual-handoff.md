# 幽冥 v2 修订：魂谷天空与危险低焰

**candidate，待用户同版确认；未接入。** Blockbench 适用。版本 `gloom-v2-soulvalley-danger-rev01-20261116`；batch `待制作-20261031`；task `fire_nether_gloom_v2_domain_preview`；attempt `20261116-art-gloom-rev-01`。本阶段额度已用完，返修结果未追加复审。

## 这版看什么

- `01-inside-eye-sky-and-fire.png`：真实领域内第一人称；眼高 1.62 格，同时看天空、远景和地火。这也是新版同尺度比较图。
- `compare-old-same-camera.png`：修改前独立实拍。与新版相同镜头 `[0,25.92,42]`、目标 `[0,22,-50]`、FOV 68°、时刻 0.7 秒、1280×912；不是拼图。
- `02-overall-three-quarter.png`、`03-side-person-height.png`：整体及侧面身高参照。外部检查镜头隐藏天空/远景代理，不表示领域外也染天。
- `inside-soulvalley-4s.gif`：80 张真实主视口帧，960×684、50ms/帧、4 秒。只演示领域内持续氛围、错相起伏和少量余烬；**没有演示渐入/退出**。GIF 循环重启片段，不是无缝大周期，更不是施法时长。
- `youming-v2-soulvalley-danger-candidate.bbmodel`：真实 Blockbench 原生保存、复开；273 方体、65 组、19 纹理、1 动画。49 地火簇、8 个视觉余烬；其余含明确命名的辅助几何。

## 保持与变化

保持前版固定 UUID `20261116-0000-4000-8000-000000000049` 的 49 簇布局、随机 yaw 和相位。16 模型单位 = 1 格；X/Z 水平、Y 向上。完整领域 X/Z ±3.5、Y 0…8，即 7×7×8 格；默认 200tick=10 秒不变。原点为施法脚底，固定世界位置，不跟随玩家或镜头；每簇仍是竖直双面交叉面，挂点 minY+0.15，不铺满 8 格高。地砖 1 格，人物 1.8 格。第一人称位置折算为 `[0,1.62,2.625]`，确在样例盒内。

新版视觉候选将原簇基础宽乘 0.94、高乘 1.16：理论宽 0.6486–0.9964 格，高 0.58–1.2296 格。脉动宽缩放改为 0.82–1，高缩放 0.52–1；仍为 3.84 秒周期、逐簇错相。最高理论火面顶不超过脚底约 1.38 格；主要火势仍低于人物，不建高墙。这些是**待确认的客户端外观参数**，不是已修改运行画法或任何玩法范围。

余烬仅 8 个小方像素，宽约 0.028–0.048 格、高约 0.042–0.072 格；从地面约 0.71 格升至 1.65 格附近，1.6 秒错相循环，以正弦缩放从零长出再缩至零；横向摆幅不超过约 0.041 格。无新资源、命中、产种或伤害。余烬色取本次 v2 热核色。

## 二维与材质

沿用前版 `youming_cyan_soul_v2_100ms.aseprite`，未采用 v3。为加强危险感，在**真实 Aseprite GUI 的新源** `youming_v2_soulvalley_palette.aseprite` 只做色阶重映射，不重画火形、不加新火舌像素、不变透明遮罩和帧顺序。青白热核 `#CCFAF5`、亮青 `#50DBDE`、中青 `#0E8B9E`、深根 `#074253`；映射规则见 `evidence/tool-evidence.json`。所有可见像素的 RGB 有变化，因此不是声称与运行 v2 RGB 完全相同的版本。

原生导出 `textures/youming_v2_soulvalley_strip.png` 为 64×1024、16 个 64×64 帧，100ms/帧、1.6 秒循环；`v2-palette-preview.gif` 为原生二维预览。与运行 v2 比较，整张 alpha 通道逐像素完全一致，21492 个非透明位置保持；详见 `evidence/encoding-and-pixels.json`。源已原生复开。

Blockbench 四组纹理沿用总相位 15 与 0/4/8/12 偏移，最近邻、双面、无插值。`textures/preview_alpha210.png` 只在原生场景中将新版 alpha 乘 210/255，模拟运行顶点 alpha210；此文件**不是运行贴图替换源**。全亮用模型材质白色无方向光示意；复开后截图重新施加局部材质 uniform，未改应用全局配置。工程时间线与纹理播放是独立控件，交付 GIF 已按同一时间同步。

## 天空、远景与待接入边界

参考用户指定的 Minecraft 官方 Soul Sand Valley 文章，仅取暗青蓝天空、远处青雾的视觉方向：
https://www.minecraft.net/en-us/article/around-block--soul-sand-valley

实际预览用原生工程中的 `AUX_SKY_PREVIEW_ONLY_not_domain_boundary` 远处天空包围材质，顶部 `#081923`、高空 `#102C3B`、近地平线 `#416E77`，以及 `AUX_DISTANT_SCENERY_PREVIEW_ONLY_not_runtime` 的三级雾色远景块体。它们只是**可编辑场景背景/远雾代理**：天空包围体远在 ±128 格，不是领域边缘蓝墙；远景块体不代表要生成方块、灵魂沙、原版魂火或新地形。无 Forge 雾密度实测，不能把代理颜色/距离直接当游戏雾算法。AUX 地面变暗也只是氛围对照，不要求替换世界方块贴图。

“仅摄像机在领域内时渐入，离开或结束后平滑恢复”来自**主会话视觉接入建议**，不是用户逐字参数。可交接建议为进入 0.6 秒、退出 0.8 秒，使用 `smoothstep(u)=u²(3−2u)` 混合当前正常天空与目标青蓝；本次动画未验证该曲线，数值未获用户确认。当前预览没有自动体积检测，不宣称外部全世界天空已受正确隔离。

父方收束的 Flash Carver 事实：当前没有 ViewportEvent/FogRenderer/skyColor 处理。`NetherDomainSight.probe` / `Sight.covers` 才有逐片真实体积 `[min,max)` 点包含；`NetherDomainTint.gloom(...)` 有布尔入口。不能以 bounds 外包盒判洞/切片。live 要求当前维度相同且 `gameTime<visualEnd`；removal 直接删除，无客户端 owner 存活字段。当前地火到期停画、无淡出。因此天空、雾、退出余辉/平滑恢复均是**待接入的新视觉**，不以模型预览冒充已经实现。玩法、范围、生成及产种保持不动。

## 复开、采用与限制

源、PNG、工程、四张核心实拍及动画属于本目录同版候选，完整哈希见 `manifest.json`。真实复开图为 `evidence/reopened-actual-viewport.png`；局部预览恢复脚本为 `evidence/restore-preview.native.js`，只能在本候选工程中执行。所有 AUX 和整个 bbmodel 都不直接复制到运行目录。

当前运行资源仍是 `src/main/resources/assets/xueximod/textures/effect/nether_gloom_v2/nether_gloom.png`，未改。若用户确认，Grok 再决定确认版客户端渲染/余烬/天空雾接入及正式资源映射，不由美术作者写 Java。原表 N/O/DV 与 approved-v1 来源仍指向前版需求快照；周期 5 与批准 20 的事项只交回功能作者。

收束哈希对账以**前次回执**为基线：前版 130 个产物、运行 PNG、所核对 Java 及历史源一致；制作表与共享资源 README 已不同于前次哈希。本作者本轮没有写这两项，未追查外部修改作者/时间；不能把前次表哈希当成本轮当前表哈希。详情见 `evidence/input-preservation.json`，本候选需求仍取本次明确授权及指定前版快照。

本次不编译、不打包、不启动游戏。Forge 透明排序、光照/雾混合、真实地形遮挡、维度切换、切片洞、结束时平滑退出与性能均未验证。旧版及其他用户项目保留，未关闭未保存项目。候选未确认、未正式采用、未接入；不能迁待验收。父方展示同版预览后，由唯一 Grok 登记进度。

---

## 同版补充：transition-02，2026-11-16

attempt `20261116-art-gloom-transition-02`。本段为同版交付补齐，不是新设计或正式复审。上文 rev01 的“持续 GIF 没有演示渐入/退出”仍是该次交付的真实历史；原文与 manifest 的补充前副本存于 `evidence/transition02/visual-handoff-rev01.md`、`manifest-rev01.json`。原 4 秒 GIF、所有旧截图、Aseprite/PNG、bbmodel 均保留，未改火形、配色、布局、尺寸或动态曲线。

新增 `inside-tone-transition-3p8s.gif`：76 帧、20fps、960×684、3.8 秒，全部从真实 Blockbench 当前 GUI 主视口同步取得。固定原第一人称镜头 `[0,25.92,42]`、FOV68°、眼高1.62格。时间安排：

- 0–0.4 秒：正常色调参照；
- 0.4–1.0 秒：0.6 秒 smoothstep 混合至既有暗青候选；
- 1.0–2.2 秒：候选色调保持；
- 2.2–3.0 秒：0.8 秒 smoothstep 混合回正常参照；
- 3.0–3.8 秒：恢复后保持。

曲线 `s(u)=u²(3−2u)`，进入权重 s，退出权重 1−s。**本次实际示意了这两个过渡时长及曲线**，并非只写建议。三个关键实拍为 `transition-normal.png`（t=0）、`transition-domain.png`（t=1.4）、`transition-restored.png`（t=3.4）；均逐张 view_image 查看，GIF 亦已查看。

混合只发生于原场景的天空、远处地面和三级远景代理材质；正常端是本次中性日间参照色，不是 Minecraft 实测正常天空，也不表示任何维度均应恢复到固定蓝天。既有地砖、人物、魂火和余烬材质不参与环境混合；火焰使用原动画从0.7秒连续推进，**退出时仍继续燃烧**。

可复现脚本 `evidence/transition02/capture-transition.native.js`：在真实 GUI 复开既有 `youming-v2-soulvalley-danger-candidate.bbmodel` 后，通过当前喜鹊 Blockbench 的原生 JavaScript 执行工具运行。脚本只临时向5个环境代理材质加入 normal-map 与混合权重 uniform，先由真实主视口渲染再 Screencam 取帧；结束恢复原 shader、材质和预览时刻。不另存或覆盖 bbmodel，不修改纹理源；全部时间/正常端参数及逐帧权重在 `evidence/transition02/native-capture.json`。GIF 编码只封装已有真实帧，不对截图离线加滤镜或拼贴。

该段名称应是**“视点进入/离开时的环境色调混合示意”**：镜头位置不移动，体积包含判定没有运行，不是实际跨边界测试，也不是领域到期熄灭。正常/领域/恢复是演示权重，不代表当前摄像机真的离开盒体。Forge天空/雾、切片判定、回正常场景的正确来源、运行退出状态保持及到期余辉仍未接入/未验证；不新增或改动范围、生成、产种。

本段仍为 candidate，待用户同版确认。未写 src、表、共享索引；未编译、未运行游戏。**本阶段额度已用完，返修结果未追加复审。**
