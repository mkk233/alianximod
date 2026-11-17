# 燎原灰烬二维素材草稿

## 当前候选 v5：草方块尺度的16×16离散杂色

用户反馈v4太像大理石，要求参考草方块杂色。v5改为原生16×16像素，逐像素离散分配中灰、深灰、少量灰褐与浅灰，不再使用连续云纹、灰尘场、长裂缝、连片浅灰多边形或大尺度纹路。暖橘火星采用原生像素粒度，每块3个稀疏热点错相明灭，少量峰值带暗红邻像素；保留此前增强可读性的方向。

本地查看参考来自 `C:/Users/baishang/.gradle/caches/forge_gradle/bundeled_repo/net/minecraft/client/1.20.1/client-1.20.1-extra.jar` 内 `assets/minecraft/textures/block/grass_block_top.png`；提取副本 `reference-mc-grass-top.png` 仅供颗粒尺度参考，未复制入原创底纹或游戏资源。

- 主预览：`mixed-surface-preview-v5.gif` / `.png` / `.aseprite`。4×4块、原生64×64最近邻放大512×512，24帧、50ms。二维材质示意，非游戏截图。
- 四种底纹：`ground_ash_base-v5.png`、`ground_ash_base-v5b.png`、`ground_ash_base-v5c.png`、`ground_ash_base-v5d.png`，全部原生16×16。
- 四份分层源：`liaoyuan-ground-ash-v5.aseprite` 及v5b/v5c/v5d同名系列，16×16、24帧、50ms。
- 四份合成纵条：`ground_ash_strip-v5.png` 及v5b/v5c/v5d系列，16×384。
- 共享透明余烬：`liaoyuan-ground-embers-v5.aseprite`、`ground_embers_strip-v5.png`，16×16帧，24帧、50ms，纵条16×384。
- 单块放大：`single-tile-preview-v5.png` 及v5b/v5c/v5d系列；脚本 `draw-ash-v5.lua`。

接入适配：推荐四种16×16静态base稳定坐标选型，加共用16×384透明embers，最近邻采样；不混用旧64px云纹base。动画帧数仍24，纵向UV仍frame/24，但实际贴图尺寸已改为16×384，不能硬编码64×1536。若运行必须64px，可由Aseprite将原生16px整数最近邻放大4倍，每个原生像素对应4×4；不可插值平滑。每块底纹生命周期内固定，仅火星按相位变化。也可用一份base实现单张契约，但四变体更少重复。

旧版本保留。v5为当前待确认过程草稿，未正式归档、未接入、未改表/代码或编译，未追加复审。

## 历史候选 v4：多层灰烬杂色与四种底纹

用户反馈v3“灰烬杂色过少有种砖块的感觉，再调整一下”。v4完全重构底纹分布：移除旧固定步长/规则斜线颗粒，改为随机不规则重叠粉尘、1–6像素不同形状灰片、短炭屑与细粉。浅灰、冷灰、灰褐、深炭分层；仍无长裂缝。增强火星沿用v2/v3。

当前主预览为 `mixed-surface-preview-v4.gif` / `.png` / `.aseprite`：四种底纹错开混铺，火星各块错相，512×512。二维材质示意，非游戏截图。各 `tiled-surface-preview-v4*.gif` 仅展示对应单变体重复铺贴，不是推荐的大范围铺设方式。

四变体后缀为 `v4`（A）、`v4b`（B）、`v4c`（C）、`v4d`（D）。每种均有以下文件，星号替换为上述后缀：

- `liaoyuan-ground-ash-*.aseprite`：64×64，24帧，50ms，灰烬与火星分层源。
- `ground_ash_base-*.png`：64×64静态底纹。
- `ground_ash_strip-*.png`：64×1536合成动画纵条。
- `liaoyuan-ground-embers-*.aseprite` / `ground_embers_strip-*.png`：透明余烬源与64×1536纵条，四份余烬相同，可以只复用 `ground_embers_strip-v4.png`。
- `single-tile-preview-*.png`、`tiled-surface-preview-*` PNG/GIF/Aseprite、`draw-ash-*.lua`。混合预览脚本 `draw-mixed-preview-v4.lua`。

给Cursor：四种底纹外沿采用同一A边缘并向内3像素过渡，使变体互相兼容。推荐四张静态base按地表坐标稳定选型，叠一张共用透明余烬动画；不需要四份余烬同时加载。也可选四张合成动画之一，但不要再叠透明余烬。选型应使用坐标哈希打散，而非固定ABCD棋盘/斜线循环；火星相位另按坐标错开。单个表面在寿命内不要随机换底纹，避免灰烬闪动。帧数、帧长、纵向UV规则不变。

旧版本保留。v4为当前待确认过程草稿，未正式归档、未接入、未改表/代码或编译，未追加复审。

## 历史候选 v3：去掉长裂缝

用户看过v2后要求“去掉底色中的裂缝或者大幅减少”。v3移除全部手绘长条与分叉裂缝路径及其描边，保留细碎颗粒、短炭屑和灰烬明暗起伏；沿用v2火星的数量、位置、亮度与错相动画。旧版本完整保留。v3取代v2成为当前待确认候选。

- 主预览：`tiled-surface-preview-v3.gif` / `.png` / `.aseprite`，512×512的二维4×4铺贴示意，非游戏截图。
- 单块最近邻预览：`single-tile-preview-v3.png`。
- 源：`liaoyuan-ground-ash-v3.aseprite`、`liaoyuan-ground-embers-v3.aseprite`，64×64、24帧、50ms/帧。
- 静态底层：`ground_ash_base-v3.png`，64×64。必须使用此v3底层，旧无后缀base包含裂缝。
- 动画导出：`ground_ash_strip-v3.png`、`ground_embers_strip-v3.png`，64×1536纵条。透明embers像素沿用v2，另存v3以保证当前资源名一致。
- 脚本：`draw-ash-v3.lua`。推荐v3 base + v3透明embers两层，或仅v3合成ash条；不要重复叠余烬。
- 仅过程草稿，尚未用户视觉确认、未接入游戏、未正式归档，未改表/代码或编译，未追加复审。

## 历史候选 v2：火星更明显

用户看过初版动态预览后要求“火星再明显一些”。当次保留初版文件，新增带 `-v2` 后缀的源文件、导出和预览；后续已由上述v3取代。灰烬底色与纹理完全沿用，热点数量、位置和错相周期保持不变；提高暖橘亮度，加入暗红边缘，部分热点峰值具有2–3像素亮芯，极少峰值补一个浅金像素，不增加大火柱。

- 主动态预览：`tiled-surface-preview-v2.gif`；静态同名 `.png`；可编辑预览同名 `.aseprite`。仍是二维4×4铺贴示意，非游戏截图。
- 源文件：`liaoyuan-ground-ash-v2.aseprite`、`liaoyuan-ground-embers-v2.aseprite`，64×64、24帧、50ms/帧。
- 原尺寸导出：`ground_ash_strip-v2.png`、`ground_embers_strip-v2.png`，64×1536纵条；静态底层继续用 `ground_ash_base.png`。
- 单块最近邻预览：`single-tile-preview-v2.png`；绘制脚本：`draw-ash-v2.lua`。
- 适配方式与下文相同，使用v2对应导出；尚未用户视觉确认、未正式归档、未接入游戏，未追加复审。

2026-10-31，Aseprite MCP / Aseprite 1.3.17 batch 实际绘制。仅过程归档，待用户视觉确认；未接入游戏，未改制作表或正式资源索引。没有追加正式设计/代码复审。

连续灰黑地表、细碎炭屑与短裂纹，稀疏暗红至橙黄余烬错相明灭。风格参考 burnt 的炭化草地描述，全部像素原创；未复制参考 JAR 贴图。spread_wave 旧 12 帧动画未修改。伤害翻倍与范围逻辑由代码执行者负责，本素材不含数值逻辑。

## 文件

- `liaoyuan-ground-ash.aseprite`：64×64，24 帧，50ms/帧，连续炭灰与余烬分层，可编辑。
- `ground_ash_strip.png`：64×1536，24 帧自上向下，完整不透明灰烬地表加余烬合成。
- `ground_ash_base.png`：64×64，静态不透明灰烬底层。
- `liaoyuan-ground-embers.aseprite` / `ground_embers_strip.png`：64×64 / 64×1536，24 帧，50ms/帧，透明背景的独立余烬层。
- `single-tile-preview.png`：单块放大至512×512，最近邻。
- `tiled-surface-preview.png` / `.gif` / `.aseprite`：4×4铺贴示意、512×512，24帧/50ms，每块有时间相位偏移。仅二维材质预览，不是游戏截图，不代表实际20×20格范围或火场寿命。
- `draw-ash.lua`：本次通过Aseprite MCP run_lua执行的绘制脚本。

## 给 Cursor 的资源适配说明

1. 用户确认后再归档、接入。目前全部为过程草稿。推荐使用 ground_ash_base + ground_embers_strip 两层；也可单层使用 ground_ash_strip。不要合成贴图再叠同一余烬层，避免重复发亮。
2. 动画与旧24帧/50ms兼容：帧索引0–23，纵向UV为 frame/24 至 (frame+1)/24。循环1.2秒，仅为素材循环，不控制术式200tick总寿命。各地表块可按坐标稳定偏移帧相位，避免同步闪烁；底层不漂移。
3. 连续面应按真实地表贴合并覆盖，不能沿旧稀疏火焰斑点密度抽样；余烬可独立调低密度。贴图使用最近邻、正常透明混合，底层无需自发光。不要将整层灰烬用加色混合，否则无法形成炭灰覆盖。
4. 本素材是整块可平铺纹理，没有将全场圆形蒙版烘焙进去。外缘破碎、方形有效接触区角部提示、中心向外12tick展开、少量上升火星和结束淡出由原渲染/本次实现处理；不把预览整图加载成游戏贴图。
5. 半径10格与约20×20格接触区由代码决定。术式结束解除临时覆盖，恢复原方块外观；本素材不授权替换真实方块、放置原版火或永久烧焦。

本美术任务代码、编译、游戏验证不适用。二维导出已打开观察；尚未用户确认，尚未游戏实测。
