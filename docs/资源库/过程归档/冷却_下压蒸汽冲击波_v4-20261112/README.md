# 冷却 v4：先冒汽、从上到下散开、随后推出冲击波

状态：二维及同版本真实 Blockbench 预览已完成，等待用户查看确认。本稿没有进入正式采用目录或游戏资源。
制作日期：2026-11-12。

## 用户本轮要求

“蒸汽应该从上到下散开这样才有产生推力推开怪物的感觉，而且应该先冒出蒸汽再出现冲击波。”

## 视觉变化

蒸汽按肩部、上身、腰部、腿部、脚踝的顺序出现。每团汽流从身体附近向外并略向下散去，上层先散，下层接续，形成下压和向外释压的连续动作。沿用此前淡蓝灰边、浅白汽芯的像素色彩。

冲击波保留 v3 的破碎环形素材，延后 5 帧（375ms）。蒸汽在第 2 帧首次可见，冲击波从第 7 帧才开始可见。两者首次出现相隔 375ms。
整段源动画仍为 16 帧 × 75ms = 1.2 秒，单次播放。冲击波可见区间为 450～1125ms，第 16 帧回到透明。

此处的推力感是视觉表现，不构成击退逻辑、力度、范围或其他游戏机制的变更。

## 文件

- `cool_v4_vapour.aseprite`：64×64，16 帧，三个可编辑图层，分别为后层汽流、前层汽流、外散碎点。
- `cool_v4_shockwave.aseprite`：96×96，16 帧，保留 v3 的三个图层并延后时序。
- `cool_v4_vapour_strip.png`、`cool_v4_rear_strip.png`、`cool_v4_front_strip.png`、`cool_v4_fragments_strip.png`：64×1024，原尺寸透明纵条。
- `cool_v4_shockwave_strip.png`：96×1536，原尺寸透明纵条。
- `frames/`：同版本分层与冲击波的逐帧透明图。
- `cool_v4_preview.aseprite`：252×116，深浅背景二维审稿板，16 帧 × 75ms。
- `cool_v4_preview.gif`：最近邻放大 4 倍。GIF 帧时长交替 70/80ms，末尾透明姿态停留 450ms，循环总长 1570ms；该额外停留只方便观看。
- `cool_v4_preview.png`：第 9 帧。
- `cool_v4_keyframes.png`：第 3、5、7、9、12、15 帧，依从左至右、从上至下排列。
- `draw_cool_v4.lua`：由 Aseprite MCP 执行的绘制与导出脚本。
- `manifest.json`：本稿文件 SHA-256。

## 三维阶段状态

2026-11-12 用户要求制作三维预览后，重新发现并成功调用 Blockbench MCP 能力工具。实际连接为桌面 Blockbench 5.2.2 / 插件 1.10.1，连接时没有活动项目。新建独立 v4 场景，使用本目录原尺寸 Aseprite 图集；全程经 MCP 操作。

- `cool_v4_spatial.bbmodel`：真实应用创建和保存的可编辑场景，项目名 `cool-v4-downward-vapour-shockwave-preview`，UUID `fe1b1c81-3962-2fbb-bc94-6a37e1b6e5ef`。
- `blockbench_frames/frame_01.png` 至 `frame_16.png`：通过实际 `Preview.screenshot` 逐帧采集的 720×560 视口图。
- `cool_v4_spatial_preview.aseprite`：真实视口图的 16 帧容器，每帧 75ms。
- `cool_v4_spatial_preview.gif`：经 Aseprite 编码的实际视口动画，帧时长及循环尾部停留规则与二维预览相同。
- `cool_v4_spatial_keyframes.png`：实际视口第 3、5、7、9、12、15 帧的排版，按从左至右、从上至下阅读。
- `cool_v4_spatial.png`、`cool_v4_spatial_high_angle.png`、`cool_v4_spatial_side.png`：透视、较高角度和侧面的实际截图。
- `build_blockbench_v4.js`：经 Blockbench MCP 执行的实际场景创建脚本。
- `encode_blockbench_preview.lua`：实际视口截图的 Aseprite 编码和排版脚本。

场景中 16 模型单位对应一个参考方格，假人高 32 单位。该尺寸用于 Minecraft 人物比例参照，不声明实体碰撞箱大小。
冲击波是位于 y=10 的世界水平贴片，尺寸 64×64 模型单位，可见亮边最大半径约 1.8 个参考方格，源图顶视为圆。蒸汽沿用二维稿的比例，每像素 2/3 模型单位，三个图层置于人物前后用于检查时序及遮挡。侧视保留该贴片构造，运行时粒子挂载与朝向仍需另行交付实现。
假人、地面、参考网格位于 `REFERENCE_ONLY_player_and_ground`，只作示意，不进入运行资源。
Blockbench 的正视、俯视及侧视截图均来自 v4；没有复用旧版视口冒充本版。主视口留在本稿场景以便用户继续查看。
