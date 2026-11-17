# 冷却 v4.1：贴近地面的冲击波

状态：二维及真实 Blockbench 预览稿，等待用户确认。日期：2026-11-12。

本轮按用户反馈，将冲击波贴片从地面上方 10 模型单位降至 1.2 单位。示意场景以 16 模型单位对应一个参考方格，即新高度为 0.075 格，位于脚底附近。冲击波沿世界水平面扩散。

蒸汽仍从肩部开始，依次沿腰部、腿部向外并略向下散开。蒸汽首次可见于 75ms，冲击波首次可见于 450ms；16 帧，每帧 75ms，单次源动画总长 1.2 秒。冲击波及蒸汽的原尺寸像素图集沿用 v4 内容，本轮只调整空间高度和对应的二维投影位置。

## 文件

- `cool_v4_1_vapour.aseprite`、`cool_v4_1_shockwave.aseprite`：可编辑二维素材，分别为 64×64 和 96×96，每项 16 帧。
- `cool_v4_1_*_strip.png`：按时间从上到下的透明纵条图集。蒸汽为 64×1024，冲击波为 96×1536。
- `frames/`：分层及冲击波的逐帧透明 PNG。
- `cool_v4_1_preview.aseprite`、`cool_v4_1_preview.gif`：更新了冲击波高度的深浅背景二维预览。
- `cool_v4_1_spatial.bbmodel`：在真实桌面 Blockbench 5.2.2 / MCP 1.10.1 中修改、另存的可编辑模型。
- `blockbench_frames/`：16 张真实 Blockbench 视口截图。
- `cool_v4_1_spatial_preview.aseprite`、`cool_v4_1_spatial_preview.gif`：同版本实际视口动画。
- `cool_v4_1_spatial.png`、`cool_v4_1_spatial_side.png`、`cool_v4_1_spatial_high_angle.png`：透视、侧视和较高角度的实际视口图。
- `cool_v4_1_spatial_keyframes.png`：实际视口第 3、5、7、9、12、15 帧的排版。
- `draw_cool_v4_1.lua`、`lower_wave_and_capture_v4_1.js`、`encode_blockbench_preview.lua`：Aseprite 绘制、真实场景位置修改及采集、实际截图编码脚本。
- `manifest.json`：本稿文件 SHA-256，以及原尺寸图集与 v4 一致性的记录。

GIF 使用交替 70/80ms 匹配格式精度，末尾透明姿态停留 450ms，预览循环总长 1570ms；该停留仅为方便查看，不计入运行时效果时长。

示意人物、地面和网格在 `REFERENCE_ONLY_player_and_ground` 分组，属于辅助参照。场景范围和高度是本次美术预览参数，不声明实际技能范围、击退力度或地形适配已经实现。本稿尚未接入游戏。
