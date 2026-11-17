我先把 v2 任务说明整份读完，再按里面的授权范围做。v1 的圆润水线不再续做。我先核对 Blockbench 里现有项目，再单独建一版和史蒂夫方体对齐的水线。皮肤格式没有动画，所以这版用 free 项目，按原版六方体的尺寸和 UV 来画。接下来直接在 Blockbench 里画透明水线。水幻形 v2 已按原版 Steve 方体做完，是过程候选，还没正式采用，也没有接入游戏。

静止时是六个方角方体，不是圆头圆肩。头 8×8×8，躯干 8×12×4，双臂和双腿各 4×12×4，脚底在 y=0，头顶在 y=32。手臂自然下垂，枢轴用原版：右臂 [-5, 22, 0]，左臂 [5, 22, 0]，双腿在 y=12。这 32 单位是原版模型高度，没有把它说成 1.8 格。

水线是在 Blockbench 里直接画的透明皮肤：方形边缘是深蓝加青蓝，带短的近白高光，面内部是空的。正、侧、背都有。1.20 秒时外壳换成贴在边上的直杆，再裂开、下坠，变成小水片和水滴后缩掉。没有整个人缩成一块蓝板，也没有画出手弧线。两触发形式共用这段散水。

灰色 Steve 是同一套方体，放在旁边。叠合那张是拍摄时临时挪到同一位置，水线正好框住灰色方体，拍完已经移回去。

项目 UUID 是 `20e6cc04-1d69-c38a-3030-5a2bacdaf4f4`。v1 和原来的 iaido 项目都还在。

文件在 `docs/资源库/过程归档/水幻形_Steve水线_v2-20261102/`：

- `water_phantom_steve_v2.bbmodel`
- `steve_water_lines.png`
- `bb_hold_three_quarter.png`、`bb_hold_front.png`、`bb_hold_side.png`、`bb_hold_back.png`
- `bb_overlap_front.png`、`bb_break_mid.png`、`bb_ground_drops.png`
- `bb_preview_replay.gif`（17 张真实视口帧编的）
- `visual-handoff-candidate.md`

皮肤格式没有动画，所以用的是 free 项目，尺寸和 UV 按 Steve 写入。Blockbench 不能证明伤害、接触或游戏里的缩放。关闭离屏视口时工具返回 `onContextMenu is not defined`，那是私有视口，没有动你的相机。
