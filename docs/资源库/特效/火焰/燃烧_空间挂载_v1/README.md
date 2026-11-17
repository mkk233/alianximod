# 燃烧 · 空间挂载 v1

2026-11-01 用户确认：「可以就这版接入」。手臂方向为「整团火焰随手臂一起转动」。当前状态：用户已确认，已接入源码，`compileJava` 已通过，制作表已迁待验收，待用户游戏实测。

## 这份目录里有什么

- `burn-v3-spatial-parts.aseprite`：15 个可见分件的可编辑源。隐藏的原始合并火星层仍在文件里，不进运行网格。
- 15 张原尺寸分件条，外加隐藏合并层 `burn-v3-embers-strip.png`。可见分件是披风、左右背后火舌、左右肩、左右腹、左髋、左右小腿、左右手焰、左右手火星、身体火星。
- `燃烧v3_空间挂载示意_v1.bbmodel`：已确认的 Blockbench 模型。灰人偶、持物杆不进游戏。
- `native-mount-parameters.json`、正背 GIF、侧面、持物近景、Blockbench 应用证明。
- `视觉交接_待确认.md` 是过程稿原文，哈希与确认前一致。现行状态见 `交接状态.md`。

二维五层原件和入场仍在 [燃烧_双手附焰与火焰披风_v3](../燃烧_双手附焰与火焰披风_v3/README.md)：`overdrive-v3.aseprite` 与五张分层，以及 `overdrive-v3-entry.aseprite`。本目录不改那些原件。

## 运行资源

网格：`src/main/resources/assets/xueximod/models/effect/burn_spatial_v1/mount.json`。只含特效面，顶点已换到玩家骨骼局部坐标。

循环贴图在 `textures/effect/burn_spatial_v1/loop/`，入场在同级 `entry/`。模型实际采样的是：

- `cape.png`、`rear.png`、`hand.png`：对应披风、背后火舌、手焰整层。循环图与已确认分件条、以及原先 `furnace_v2` 的 `burn_cape.png`、`burn_rear_fire.png`、`burn_hand_fire.png` 逐像素相同。
- 肩、腹、左髋、小腿、双手火星、身体火星：使用对应分件条。循环图与过程目录里的 `burn-v3-*-strip.png` 逐像素相同。

入场仍是 8 帧 × 60ms。披风、背后火舌、手焰整层与 `furnace_v2/burn_entry_*.png` 逐像素相同。身体七簇和三类火星按循环分件的并集遮罩，从入场整层逐像素拷贝原色；没有重画。身体入场没有落在遮罩外的像素。火星有 95 个原像素落在循环并集外，按原稿火舌根部就近归入已有火星分件，颜色不改。遮罩重叠处每个像素只归一个分件。

维持 20 帧 × 80ms。挂点与入场共用这一套骨骼。演示用的抬手动画不进入游戏。

旧的 `textures/effect/furnace_v2/burn_*.png` 还在磁盘上，燃烧运行路径已不再引用。升温和冷却贴图没有改。
