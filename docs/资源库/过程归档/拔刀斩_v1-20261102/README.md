# 拔刀斩单次动画 v1

状态：预览版，等待用户评价，未正式采用、未接入游戏。

用户本次要求用 Blockbench 制作拔刀斩动画。此目录保留真实 Blockbench 模型、Aseprite 材质源及真实视口动画预览；旧稿没有改动。

## 查看

- 可编辑模型：[iaido-single-v1.bbmodel](iaido-single-v1.bbmodel)，在 Blockbench 动画模式选择 animation.iaido_single_v1。
- [正面动画预览](iaido-front-preview.gif) / [侧面动画预览](iaido-side-preview.gif)。
- [Aseprite 材质源](iaido-atlas.aseprite) / [64×64 导出](iaido-atlas.png) / [放大预览](atlas-preview.png)。
- [空间与动作交接](visual-handoff.md) / [文件哈希](manifest.json)。

## 动作

单次 1.6 秒：0–0.20 秒握柄蓄势；0.20–0.48 秒右手抽刀、左手退鞘；0.48–0.60 秒快速横斩；0.60–0.85 秒停势；0.85–1.60 秒落刀至右前低位。结束保持持刀，不自动入鞘。

一把刀始终跟随右手，刀尖退出鞘口后才横斩。上臂与前臂各 6 模型单位，按手部轨迹反求关节并烘焙。

GIF 来自真实 Blockbench 视口，41 帧、每 0.04 秒采样，首尾增加观看停顿，总 2.36 秒并循环显示；模型动画本身为 once。

## 校对与限制

已检查正面/侧面关键姿态及导出纹理一致性。native-pose-check.json 记录实际骨骼世界位置/方向检查；单刀绑定、抽出后挥斩和终态落刀都已在视口预览。

假人、地面是示意。模型格式为 free，双节手臂供动作设计使用，尚未映射游戏玩家骨架、第一人称或运行时动画系统。未改 Java、未编译、未打包、未游戏内测试。

源和预览对应同一 v1，哈希见 manifest.json。frames-front/ 和 frames-side/ 是原始视口帧；脚本与关键帧数据保留用于重制。
