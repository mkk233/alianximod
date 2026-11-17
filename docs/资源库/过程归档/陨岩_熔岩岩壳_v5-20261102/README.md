# 陨岩熔岩岩壳 v5 — 用户已确认

最新用户要求：“火焰纹理太少了要更像岩浆块”。本版沿用平整、没有孔洞的方向，把零星余热改为岩壳之间成片橙红熔岩，局部亮黄；岩壳采用不规则分布。明亮熔岩像素 350/1024，约 34.2%。这是静态颜色纹理，不承诺游戏里自动发光。

- 源：meteorite-magma-v5.aseprite，32×32，单帧、5 层。
- 原尺寸导出：meteorite-magma-v5.png，全不透明。
- 二维预览：texture-preview.png，512×512 最近邻放大。
- 原生模型：meteorite-magma-v5.bbmodel。
- 原生视口：bb-hero.png、bb-reverse.png、blockbench-active-view.png。
- 制作记录：draw-meteor-v5.lua、build-blockbench.js、aseprite-report.json。
- 交接参数：visual-handoff.md；同版绑定哈希：manifest.json。

本版已在真实 Blockbench 5.2.2 / MCP 1.10.1 中贴到一格立方体六面，并检查正反视角。模型内嵌 PNG 与最终原尺寸导出字节一致。

用户 2026-11-02 回复“采用这个版本”。正式目录是 `docs/资源库/材质/方块/陨岩_熔岩岩壳_v5/`。运行贴图已原样替换，待编译和游戏实测。制作脚本仍留在本过程目录。未改 Java，未编译。本阶段额度已用完，返修结果未追加复审。
