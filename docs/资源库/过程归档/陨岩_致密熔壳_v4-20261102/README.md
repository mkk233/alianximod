# 陨岩致密熔壳 v4 — 待确认

用户已否定 v2 的矿石感，以及 v3 的坑洼外观。本稿移除孔洞与分叉金线，改成连续致密的黑色熔壳、大块斜切岩面、低对比浅擦痕和一处边缘余热。

- 源：meteorite-solid-crust-v4.aseprite，32×32，1 帧，4 个可编辑图层。
- 运行候选：meteorite-solid-crust-v4.png，32×32，全不透明静态纹理。
- 二维预览：texture-preview.png，512×512 最近邻放大。
- 真实 Blockbench 模型：meteorite-solid-crust-v4.bbmodel。
- 真实原生视口：bb-hero.png、bb-reverse.png、blockbench-active-view.png。
- 绑定参数：visual-handoff.md；内容哈希：manifest.json。
- 绘制脚本：draw-meteor-v4.lua；原生场景操作脚本：build-blockbench.js。

仍是六面共享一张材质的一格立方体。本轮承接已读取的制作表“待验收”第 9 行 fire_meteorite N/O/DV 与实体命中续飞修订需求；不改行为。用户尚未确认此实际版本，仅保留过程草稿，未改运行素材、Java、正式资源库索引或制作表，未编译、打包或启动游戏。

确认后由固定 Cursor CLI --model grok-4.7-high-fast 接入；正式代码复核额度不因美术重画重置。旧稿保留，不采用。

## 后续反馈（2026-11-02）

用户要求“火焰纹理太少了要更像岩浆块”。本版未采用，后续草稿：../陨岩_熔岩岩壳_v5-20261102/。
