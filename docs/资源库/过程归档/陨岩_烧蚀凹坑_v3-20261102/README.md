# 陨岩烧蚀凹坑 v3 — 待确认美术稿

2026-11-02 用户反馈 v2“太像矿石，不像陨石”。本版取消分叉金色矿脉，改为黑褐烧蚀壳、大小不一的凹坑、剥落流痕，以及两处破口内的暗红余热。凹陷由像素明暗表现，立方体几何不变。

本实际版本尚未获得用户确认，仅保留过程稿；没有修改运行素材、制作表、正式资源库索引或 Java，没有编译、打包或启动游戏。

- 源文件：meteorite-ablation-v3.aseprite，32×32，1 帧，5 层。
- 原尺寸导出：meteorite-ablation-v3.png，全不透明，静态。
- 二维预览：texture-preview.png，512×512 最近邻放大。
- 原生 Blockbench：meteorite-ablation-v3.bbmodel。
- 原生视口：bb-hero.png、bb-reverse.png、blockbench-active-view.png。
- 对比：bb-v2-left-v3-right.png（左为用户否定的 v2，右为本版）。
- 制作脚本：draw-meteor-v3.lua、build-blockbench.js。
- 参数交接：visual-handoff.md；文件哈希：manifest.json。

制作前读取了制作表“待验收”第 9 行 fire_meteorite 的 N/O/DV，并沿用 docs/开发记录/陨岩实体命中续飞-20261101/快照/修订需求-20261102-002838-1.md 的既有行为。仅修改美术稿；当前仍是六面共用 32×32 贴图和一格立方体。用户反馈改变的是视觉方向，不重置设计 2/2、实现 3/3 的正式复核额度。

待用户确认本实际版本后，再归档到正式资源库；如需游戏接入，交固定 Cursor CLI --model grok-4.7-high-fast 复制原尺寸 PNG 至现有 meteorite_v1.png 引用并处理编译。不要导入整个示意场景作为游戏模型。

## 后续反馈（2026-11-02）

用户否定本稿：“这个太丑了坑坑洼洼的”。本版不采用；新稿 ../陨岩_致密熔壳_v4-20261102/。
