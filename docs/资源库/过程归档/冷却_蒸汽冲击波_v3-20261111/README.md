# 冷却 v3：贴身蒸汽＋向外扩散冲击波

状态：视觉预览稿，等待用户确认。本目录不代表正式采用或已接入游戏。
本稿在 2026-11-11 绘制并完成真实 Blockbench 视口采集，2026-11-12 合成预览。

## 本轮变化

沿用用户认可的 v2 贴身蒸汽，新增一圈从身体附近水平向外推出的淡蓝白汽浪。
主圈前缘较亮，内侧有较淡的拖散汽纹；扩散后出现缺口与零碎余汽，先于贴身蒸汽消散。
原蒸汽仍从手臂、腰腹、腿侧附近升起，后段自然散去。

## Aseprite 文件与时序

- `cool_v3_vapour.aseprite`：64×64，16 帧，每帧 75ms，源动画 1.2 秒单次播放。沿用 v2 的三个图层。
- `cool_v3_shockwave.aseprite`：96×96，16 帧，每帧 75ms。分为拖散汽纹、破碎亮边、外散碎点三个图层。
- 冲击波第 1 帧透明，第 2～10 帧可见，第 11～16 帧透明；可见时段 0.075～0.750 秒，共 675ms。
- `cool_v3_*_strip.png`：按时间自上至下排列的原尺寸 RGBA 纵条。蒸汽及分层图集为 64×1024，冲击波为 96×1536。
- `frames/`：同版本逐帧透明 PNG。
- `cool_v3_preview.aseprite`、`cool_v3_preview.gif`：二维深浅背景对照，人物为示意辅助。二维图中的椭圆是水平圆环的投影。
- GIF 使用交替 70/80ms 帧时长匹配 GIF 的 10ms 精度，最后透明姿态停留 450ms 便于循环观看；预览循环总长 1570ms，不作为运行时施法时长。

## 真实 Blockbench 预览

- `cool_v3_spatial.bbmodel`：真实桌面 Blockbench 5.2.2 / MCP 插件 1.10.1 中创建并保存的可编辑场景，直接使用本目录的 Aseprite 图集。
- `blockbench_frames/frame_01.png` 至 `frame_16.png`：实际 `Preview.screenshot` 输出，未在离线绘图中重新合成三维内容。
- `cool_v3_spatial_preview.gif`：上述 16 帧经 Aseprite 编码的动画。
- `cool_v3_spatial_preview.aseprite`：16 帧实际视口截图的可编辑容器，每帧 75ms。
- `cool_v3_spatial.png`、`cool_v3_spatial_high_angle.png`、`cool_v3_spatial_side.png`：透视、较高角度及侧面实际视口。
- `cool_v3_spatial_keyframes.png`：实际视口第 2、4、6、8、10、13 帧排版，按从左至右、从上至下阅读。

场景中 16 模型单位对应一个参考方格。示意人物高 32 单位，作为 Minecraft 人物比例参照，不是实体碰撞箱声明。
冲击波贴片为 64×64 模型单位，位于 y=10（参考地面上方 0.625 格），贴图顶视为圆形。可见亮边最大半径约 1.8 格，整个贴片半径为 2 格。
蒸汽贴片按每像素 2/3 模型单位映射，分置人物前后，供本稿的层次示意。侧视保留了贴片构造；实际运行时的粒子朝向与挂载尚未交付实现。
假人、地面、网格统一在 `REFERENCE_ONLY_player_and_ground`，只用于预览，不作为运行资源。

本目录中的范围、高度及节奏属于美术示意，不构成冷却术式数值或机制调整。游戏实际表现仍由用户实测。

## 制作记录

- `draw_cool_v3.lua`：Aseprite 绘制与二维导出脚本。
- `build_blockbench_v3.js`：通过 Blockbench MCP 执行的真实场景创建脚本。
- 实际视口捕捉调用在导出前为特效贴图启用了 `force_translucent`，按帧设置 `currentFrame` 并更新 UV；保存的 `.bbmodel` 包含该设置。
- `encode_blockbench_preview.lua`：Aseprite 对实际截图编码、关键帧排版和 GIF 时长处理。
- `manifest.json`：源文件、导出、模型及预览的 SHA-256，以及蒸汽图集与 v2 一致性的记录。
