# 陨岩深岩熔金 v2 — 待确认美术稿

用户已选择“深色岩壳＋少量熔金裂隙”；尚未确认本次实际成品。仅存过程归档，未替换游戏材质、未改代码、未编译、未打包，也未修改制作表或正式资源库索引。

新版使用大块炭灰岩面、断裂凹痕和少量断续熔金。亮色裂隙像素 35 / 1024（约 3.4%）。当前是静态颜色纹理，金色不代表真实发光或着色器泛光。

- Aseprite 源：meteorite-dark-gold.aseprite，32×32，1 帧，5 个可编辑图层。
- 原尺寸导出：meteorite-dark-gold.png，32×32，全不透明。
- 二维预览：texture-preview-16x.png，最近邻放大。
- 原生 Blockbench 文件：meteorite-dark-gold-v2.bbmodel。
- 新版主视角：bb-hero.png；反向视角：bb-reverse.png。
- 新旧对照：bb-old-left-new-right.png，左旧右新。
- 当前原生视口：blockbench-active-view.png；项目记录：blockbench-project-info.json。
- 文件哈希与绑定信息：manifest.json；绘制过程：draw-meteor.lua / build-blockbench.js。

预览由运行中的 Blockbench MCP 原生视口生成。应用级截图接口曾返回另一个已有 Blockbench 窗口，错误截图已丢弃，未作本版本证据。此目录所留模型及原生视口绑定项目 a4aeb36c-dd37-cb64-4add-022dcda3aea3。

制作前已读制作表“待验收”第 9 行 fire_meteorite 的 N/O/DV，及 docs/开发记录/陨岩实体命中续飞-20261101/快照/修订需求-20261102-002838-1.md。本次只重画材质，既有一格方块体积、实体命中续飞和触地放置行为均不在本次修改范围。

本实际版本经用户确认后再归入正式资源库；后续由 Cursor CLI --model grok-4.7-high-fast 接入现有 meteorite_v1.png 引用。不要直接把示意场景导出成游戏方块模型。

## 后续反馈（2026-11-02）

用户否定本实际版本：“这个表现太像矿石了不像是陨石”。本版不采用，保留过程稿；新稿位于 ../陨岩_烧蚀凹坑_v3-20261102/。
