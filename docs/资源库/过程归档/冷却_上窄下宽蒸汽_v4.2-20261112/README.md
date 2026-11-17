# 冷却 v4.2：上窄下宽的蒸汽轮廓

状态：二维及真实 Blockbench 预览稿，等待用户确认。日期：2026-11-12。

## 本轮要求与画面

用户要求“蒸汽整体应该是正三角上窄下宽”。

本稿将上胸部的汽流集中为较窄的顶部，向腰侧、腿侧逐层扩大横向距离，脚边汽团最大、最宽。各层按上至下的顺序出现，并短暂重叠，使展开阶段可看清上窄下宽的三角形外轮廓。轮廓由离散汽团和汽流构成，没有填成不透明三角形。

颜色沿用淡蓝灰边缘、浅白汽芯。源动画为 16 帧 × 75ms，总长 1.2 秒，单次播放。蒸汽首次可见于 75ms；贴地冲击波从 450ms 出现。冲击波贴片仍位于参考地面上方 1.2 模型单位，即 0.075 个参考方格；其图集与 v4.1 相同。

## 资源与预览

- `cool_v4_2_vapour.aseprite`：64×64、16 帧，可编辑后层汽流、前层汽流、碎点三个图层。
- `cool_v4_2_shockwave.aseprite`：96×96、16 帧，可编辑冲击波源文件。
- `cool_v4_2_*_strip.png`：原尺寸透明纵条；蒸汽及分层为 64×1024，冲击波为 96×1536。
- `frames/`：同版本原尺寸逐帧透明图。
- `cool_v4_2_preview.aseprite`、`cool_v4_2_preview.gif`：深浅背景二维动画。
- `cool_v4_2_preview.png`、`cool_v4_2_keyframes.png`：二维关键帧。
- `cool_v4_2_spatial.bbmodel`：真实桌面 Blockbench 5.2.2 / MCP 1.10.1 中载入本稿素材后另存的可编辑场景。
- `blockbench_frames/`：16 张实际 `Preview.screenshot` 视口图，尺寸 720×560。
- `cool_v4_2_spatial_preview.aseprite`、`cool_v4_2_spatial_preview.gif`：实际视口帧容器与动画。
- `cool_v4_2_spatial.png`：透视实际截图；`cool_v4_2_spatial_front.png`：便于检查三角轮廓的正面实际截图。
- `cool_v4_2_spatial_high_angle.png`、`cool_v4_2_spatial_side.png`：较高角度与侧面实际截图。
- `cool_v4_2_spatial_keyframes.png`：实际视口第 3、5、7、9、12、15 帧，按从左至右、从上至下排列。
- `draw_cool_v4_2.lua`：Aseprite 绘制与二维导出脚本。
- `preview_triangular_steam_v4_2.js`：真实 Blockbench 素材更新、模型另存及视口采集脚本。
- `encode_blockbench_preview.lua`：通过 Aseprite 编码实际视口动画。
- `manifest.json`：文件 SHA-256 和冲击波图集一致性记录。

GIF 用交替 70/80ms 帧时长匹配格式精度，末尾透明姿态停留 450ms，预览循环总长 1570ms；该停留仅方便观看，不作为运行时施法时长。

场景沿用每 16 模型单位一个参考方格、32 单位高假人的比例。`REFERENCE_ONLY_player_and_ground` 为示意辅助，蒸汽前后贴片供检查轮廓、时序和遮挡；实际运行时粒子朝向与挂载仍需另行接入。本稿尚未列为正式采用素材或接入游戏。
