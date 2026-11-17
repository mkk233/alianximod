# 冷却 v4.3：上下等宽蒸汽

状态：二维及真实 Blockbench 预览稿，等待用户查看确认。日期：2026-11-12。

本轮按“改成等宽试试”调整：肩部、腰部、腿部和脚边各层使用相同的横向中心、外散距离和汽团尺寸。汽流由上至下依次出现，展开后构成上下宽度相近的分布，边缘保留像素汽团的自然起伏。

源动画 16 帧，每帧 75ms，总长 1.2 秒，单次播放。蒸汽首次可见于 75ms，贴地冲击波从 450ms 出现。冲击波位于参考地面上方 1.2 模型单位（0.075 格），图集与 v4.2 相同。

## 文件

- `cool_v4_3_vapour.aseprite`、`cool_v4_3_shockwave.aseprite`：64×64 蒸汽及 96×96 冲击波的可编辑源文件，各 16 帧。
- `cool_v4_3_*_strip.png`：原尺寸透明纵条；蒸汽及分层为 64×1024，冲击波为 96×1536。
- `frames/`：逐帧透明导出。
- `cool_v4_3_preview.aseprite`、`cool_v4_3_preview.gif`、`cool_v4_3_preview.png`、`cool_v4_3_keyframes.png`：二维深浅背景预览。
- `cool_v4_3_spatial.bbmodel`：真实桌面 Blockbench 5.2.2 / MCP 1.10.1 中载入本稿素材后另存的可编辑场景。
- `blockbench_frames/`：16 张实际 `Preview.screenshot` 视口图，720×560。
- `cool_v4_3_spatial_preview.aseprite`、`cool_v4_3_spatial_preview.gif`：实际视口帧容器及动画。
- `cool_v4_3_spatial.png`、`cool_v4_3_spatial_front.png`、`cool_v4_3_spatial_side.png`、`cool_v4_3_spatial_high_angle.png`：实际透视、正面、侧面和较高角度截图。
- `cool_v4_3_spatial_keyframes.png`：实际视口第 3、5、7、9、12、15 帧排版。
- `draw_cool_v4_3.lua`、`preview_equal_width_steam_v4_3.js`、`encode_blockbench_preview.lua`：二维绘制、真实场景更新及截图、动画编码脚本。
- `manifest.json`：文件 SHA-256 与冲击波图集一致性记录。

GIF 采用交替 70/80ms 时长匹配格式精度，末尾透明姿态停留 450ms，预览循环总长 1570ms。额外停留只为方便观看。

本轮发现上一个 MCP 会话的离屏视口已销毁，重新通过 MCP 创建了 `cool_v43_review` 视口，再采集全部图片。最终文件均来自新视口，已查看正面和侧面确认画面有效。后续复用预览对象前应核对其仍在 `Preview.all` 中且渲染上下文有效。

场景比例为每 16 模型单位一个参考方格，示意人物高 32 单位。辅助人物、地面和网格位于 `REFERENCE_ONLY_player_and_ground`。前后蒸汽贴片用于查看轮廓、时序和遮挡，运行时朝向与挂载待接入阶段实现。本稿尚未正式采用或接入游戏。
