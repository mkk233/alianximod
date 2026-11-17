# Steve v2 水感修整

候选，还没正式采用，也没有接入游戏。六个 Steve 方体的尺寸和枢轴没改。32 模型单位仍是几何高度，不是已经核实的 1.8 格。

这一轮去掉了 128 根空心长杆。散开后是 48 片实心短水片，加 20 个水滴。其中 8 个水滴在驻留时沿方边来回移动，那是关键帧，不是把皮肤上的白点说成在流。皮肤贴图仍是单帧。方体外缘深蓝从 [8, 40, 72] 调到 [22, 81, 115]。1.20 秒方体退下，短片往下掉，侧向不多。2.24 秒地面上剩下比较清楚的水滴，随后缩掉。淡出是缩放。没有出手弧线，也没有整个人一起缩小。

未改版在 `docs/资源库/过程归档/水幻形_Steve水线_v2-20261102/initial/`。修整后的模型和预览在同目录上一级：`water_phantom_steve_v2.bbmodel`、`steve_water_lines.png`、`water_flakes.png`、六张视口图、`bb_frame_00.png` 到 `bb_frame_16.png`、`bb_preview_replay.gif`，以及 `visual-handoff-candidate.md`、`bb-preview-evidence.json`。

预览时间：驻留 0.40 秒，散水中段 1.62 秒，落地 2.24 秒。GIF 每帧 160 毫秒，只用于演示。关闭离屏视口时工具返回 `onContextMenu is not defined`，没有改 MCP 配置。Blockbench 不能证明伤害、接触或游戏里的缩放。

已停止写入。
