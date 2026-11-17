# 视觉接入交接 — 陨岩烧蚀凹坑 v3

Blockbench 适用性：适用，检查完整立方体的六面材质、接缝及凹陷明暗。
版本：meteor-ablation-v3（2026-11-02）。
确认：用户否定 v2 的矿石感；本次 v3 实际成品待确认。
二维源：meteorite-ablation-v3.aseprite；32×32，1 帧静态，5 个可编辑层。
二维导出 / 素材：meteorite-ablation-v3.png，32×32，全不透明；SHA256 06f22f3a56acdb9ae4f8350a4d8b384843d90deb2985850cfa856f9ee68dcc74。
二维预览：texture-preview.png，最近邻放大。
.bbmodel：meteorite-ablation-v3.bbmodel；SHA256 812d35e9e2667842efc3e8f9fbc34ba8ad1ea5c03634041ee12200a1e90137bf。
实际预览：bb-hero.png、bb-reverse.png、bb-v2-left-v3-right.png、blockbench-active-view.png；Blockbench 5.2.2 / MCP 1.10.1 原生视口，项目 UUID 3fce4444-48fd-8252-e799-644881ff4169。嵌入材质与 Aseprite 原尺寸导出字节一致，所有关键文件哈希见 manifest.json。

换算：16 个模型单位 = 1 游戏格。
轴向与前向：+Y 朝上，X/Z 为地面；north=-Z，south=+Z，east=+X，west=-X；材质无运动前向。
原点 / 锚点 / 旋转中心：材质方块从 [0,0,0] 到 [16,16,16]，中心 [8,8,8]；组 origin=[8,8,8]，立方体 origin=[0,0,0]，零旋转。
父子层级：meteorite_v3_material / meteorite_ablation_v3。
局部坐标与世界坐标：示意只有静态一格方块；游戏继续使用现有载体/方块位置。
尺寸 / 偏移 / 朝向：保留一格立方体，凹坑只由材质表现，不改变几何、碰撞、落地偏移或朝向。
UV / 帧布局：六面同一张全图，UV=[0,0,32,32]，rotation=0，box_uv=false；没有帧条或 .mcmeta。
材质 / 透明：default、普通受光、全不透明；烧蚀壳为黑褐色，两处破口暖色只是绘制的余热，没有 emissive、bloom 或半透明要求。
时间 / 循环 / 相位：静态单帧，不适用。
跟随对象与运动绑定：沿用陨岩当前实体命中后续飞直到触地的行为与现有材质引用，不改逻辑。
预览速度：静态视角，无施法或动画时长。

运行资源：经用户确认后，计划将 meteorite-ablation-v3.png 复制为 src/main/resources/assets/xueximod/textures/block/meteorite_v1.png；现有 models/block/meteorite.json 使用 minecraft:block/cube_all 与 xueximod:block/meteorite_v1。仅复制原尺寸纹理，不复制预览放大图。
示意辅助：示意辅助_对照与地面 / 示意辅助_被否定的矿石版_v2（隐藏）、示意辅助_地面，以及 aux_* 纹理；这些不进运行资源。
Blockbench 不能代表的运行效果：游戏光照、着色器、飞行/落地粒子、动态远近可读性以及实际碰撞行为；用户在游戏中实测。

状态：待用户确认；未正式采用、未接入、未修改运行资源、未编译或打包。
