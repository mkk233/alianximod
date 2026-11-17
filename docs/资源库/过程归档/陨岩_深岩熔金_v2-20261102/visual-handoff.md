# 视觉接入交接 — 陨岩深岩熔金 v2

Blockbench 适用性：适用。材质铺在游戏内一格立方体六个面上，需要核对立体明暗与 UV。
版本：meteor-dark-gold-v2（2026-11-02）。
确认：用户仅选择美术方向；本实际版本待用户确认。
二维源：meteorite-dark-gold.aseprite；32×32；1 帧静态；5 层（岩壳底色、大块岩面、碎面与凹痕、炭化裂缝、熔金裂隙）；无播放时长或循环要求。
二维导出 / 素材：meteorite-dark-gold.png；32×32 RGBA，全不透明；SHA256 aa9d1513b67608ae2013a540ef744e91e087728d2bab57a44e17bb3b390e165b。
二维预览：texture-preview-16x.png（最近邻放大）。
.bbmodel：meteorite-dark-gold-v2.bbmodel；SHA256 8a79abf276f5453dd4804a17948944113c51358e7d6ed8cef813b0e44d657c2a。
实际预览：bb-hero.png、bb-reverse.png、bb-old-left-new-right.png、blockbench-active-view.png；真实 Blockbench 5.2.2 / MCP 1.10.1 原生视口。模型内新纹理内容与原尺寸 PNG 字节一致。各文件哈希见 manifest.json。

换算：16 个模型单位 = 游戏 1 格；材质对象为 16×16×16。
轴向与前向：+Y 向上，X/Z 为地面平面；north=-Z，south=+Z，east=+X，west=-X。静态方块材质无额外运动前向。
原点 / 锚点 / 旋转中心：新方块从 [0,0,0] 到 [16,16,16]，中心 [8,8,8]；组 origin=[8,8,8]，立方体 origin=[0,0,0]、旋转零。示意中不执行旋转动画。
父子层级：meteorite_v2_material / meteorite_dark_gold_v2。其他组均为示意辅助。
局部坐标与世界坐标：场景坐标仅用于查看一格立方体；运行时继续使用原方块与原弹道位置绑定。
尺寸 / 偏移 / 朝向：完整一格立方体，不增加外壳几何，不改变落地偏移或朝向。
UV / 帧布局：六面均使用同一 32×32 全图；UV=[0,0,32,32]，rotation=0，box_uv=false；不是六面图集，没有动画条或 .mcmeta。
材质 / 透明：default 材质、普通受光、全不透明；少量金色只是画出的热感，不要求 emissive、additive、半透明或 bloom。
时间 / 循环 / 相位：静态单帧，不适用。
跟随对象与运动绑定：现有陨岩方块/飞行载体使用同一个运行材质引用，不改变命中后继续飞行直到触地的已交付行为。
预览速度：静态观察，无演示播放速度，不代表施法时长。

运行资源：确认后将原尺寸 PNG 复制为 src/main/resources/assets/xueximod/textures/block/meteorite_v1.png；现有 models/block/meteorite.json 使用 minecraft:block/cube_all 和 xueximod:block/meteorite_v1。不要把预览放大图作为运行贴图。
示意辅助：示意辅助_旧版陨岩（默认隐藏）、示意辅助_地面、示意辅助_人偶_*（默认隐藏）；aux_* 纹理；旧版块用于比较，灰人偶为两格高比例示意。全部不进入运行资源。
Blockbench 不能代表的运行效果：Minecraft 光照、着色器、飞行时粒子、受击/落地行为及游戏内远近可读性；由用户游戏实测。

工作流状态：待本版本视觉确认；未正式归档采用，未替换运行资源，未改 Java，未编译，未打包。原批次设计/实现复核额度不因本次材质重绘重置。
