# 冷却 · 贴地蒸汽冲击波 v4.1

2026-11-12 用户确认：“还是采用v4.1接入吧”。状态：**v4.1 已接入，最终编译通过，待用户游戏实测**。过程稿和更晚的 v4.2、v4.3 对照版均保留；本目录为本次选定的 v4.1。已确认的视觉源、PNG、bbmodel 和 GIF 未改。

蒸汽先在肩部冒出，依次从上身、腰部到腿部向外、略向下散开；随后淡蓝白色冲击波从脚边贴近地面向外扩散，边缘逐渐碎开消散。设计目的是强化推开周围目标的视觉感受；实际玩法与击退按现有术式需求和代码实现，不凭预览变更数值。

## 版本与文件

- `cool_v4_1_vapour.aseprite`：64×64、16 帧、3 个图层，源文件。
- `cool_v4_1_shockwave.aseprite`：96×96、16 帧、3 个图层，源文件。
- `cool_v4_1_vapour_strip.png` 及 `rear`、`front`、`fragments` 分层纵条：64×1024，帧自上而下排列。
- `cool_v4_1_shockwave_strip.png`：96×1536，帧自上而下排列。
- `cool_v4_1_preview.aseprite`、`cool_v4_1_preview.gif`：深浅背景二维预览。
- `cool_v4_1_spatial.bbmodel`、`cool_v4_1_spatial_preview.aseprite`、`cool_v4_1_spatial_preview.gif`：同版本真实 Blockbench 场景及实际视口动画。
- `cool_v4_1_spatial.png`、`cool_v4_1_spatial_side.png`、`cool_v4_1_spatial_high_angle.png`、`cool_v4_1_spatial_keyframes.png`：同版本实际视口截图与关键帧排版。
- `manifest.json`：本目录文件 SHA-256。原尺寸素材从过程稿复制后逐文件核对哈希一致。
- 过程源及绘制、场景和截图脚本：[过程稿](../../../过程归档/冷却_贴地冲击波_v4.1-20261112/README.md)。

## 视觉交接

Blockbench 适用：适用；用户已看到与二维素材同版的真实 Blockbench 三维动画，并明确选择本版接入。

时间：源动画 16 帧，每帧 75ms，单次播放 1.2 秒。蒸汽第 2 帧（75ms）开始出现；冲击波第 7 帧（450ms）开始出现，第 16 帧透明。两者使用同一帧序列。GIF 因格式精度使用交替 70/80ms，尾部透明姿态额外停留仅用于观察，不作为运行时长。

空间：示意人物高 32 模型单位，每 16 单位为一个参考方格；冲击波为世界水平面，位于参考地面上方 1.2 模型单位，即 0.075 个参考方格。冲击波贴片边长 64 模型单位，素材实际最外亮边最大半径约 1.8 个参考方格。位置以施法者脚底或对应释放位置为锚点；地形、跟随及实际放置方式需由接入者按当前术式上下文落实。贴片自身为无厚度的水平圆环，不应作为垂直面随镜头倾斜。

蒸汽：示意使用前后分层贴片，像素映射为每像素 2/3 模型单位，对照 `cool_v4_1_spatial.bbmodel`。按人物对应位置和遮挡关系呈现，保持先上后下的时序。假人、地面、网格仅作参考，不属于运行资源。

材质：透明 RGBA 像素贴图、最近邻；保留半透明边缘。前后层和碎点与冲击波同步取帧，不把图集当单张整图显示。Blockbench 预览只验证外观和空间关系，不证明游戏内挂载、透视、遮挡或地形适配已实现。

## 运行接入（2026-11-12）

状态：v4.1 已接入，最终编译通过，待用户游戏实测。v4.2、v4.3 未采用。归档动画不是游戏内已验证的证据。

实现和编译回执来自 `01a1ce12-fe43-76f0-8c8e-5d1b41a102eb`，见 `docs/开发记录/术式批量制作/火系六式-20261029/cool-v41-integration-report-20261112.md` 与 `cool-v41-compile-20261112.log`。

四张运行贴图位于 `src/main/resources/assets/xueximod/textures/effect/cool_v41/`，并与本目录原确认 PNG 的 SHA-256 一致：

- `cool_v4_1_rear_strip.png`
- `cool_v4_1_front_strip.png`
- `cool_v4_1_fragments_strip.png`
- `cool_v4_1_shockwave_strip.png`

每张旁有同名 `.png.mcmeta`，均为 `blur=false`、`clamp=true`。16 帧、每帧 75ms，分层同步；蒸汽 75ms 开始可见，冲击波 450ms 开始可见，单次约 1.2 秒。

实现回执中的放置：蒸汽贴片宽高 42.66666 模型单位，每像素 2/3 单位；左右 ±21.33333，底 -5.33333，顶 37.33333。层深为后 -2.5、前 2.7、碎点 2.9 模型单位。16 模型单位为 1 格。本地 +Z 对玩家前向，跟随脚点和身体 Y 朝向，不随镜头俯仰。冲击波边长 64 模型单位，位于地面上方 1.2 模型单位（0.075 格），世界水平双面。

最终源码是 `FurnaceBodyVisual.java`，SHA-256 `689062B8CC9282655F1039B35D520C3277F5ED564DAFDDCFA526928142D3797C`；以及 `FireVisualLayout.java`，SHA-256 `3845368C24FC0571AD507F17F0820DB4A587BF77FF12118EF91E29C082796ADB`。`FireVisualClient.java` 曾暂改后还原，最终无差异。

`compileJava --offline --no-daemon` 于 2026-11-12 13:21:15 至 13:21:41 退出 0，BUILD SUCCESSFUL，compileJava 实际执行。游戏尚未验证遮挡、贴地和地形、透明排序、第一人称、其他玩家插值，以及原有伤害和敌我筛选。
