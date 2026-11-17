# 视觉接入交接 — 致密熔壳 v4

Blockbench 适用性：适用，六面共享贴图的一格立方体。
版本：meteor-solid-crust-v4，2026-11-02。
确认：待用户确认本实际版本；v2、v3 均被用户否定。
二维源：meteorite-solid-crust-v4.aseprite；32×32，1 帧，4 图层，无动画循环要求。
二维导出 / 素材：meteorite-solid-crust-v4.png；全不透明；SHA256 ecd7526410007f4751891089721130df2d913a8da862470594ffa48d02125858。
二维预览：texture-preview.png，最近邻放大。
.bbmodel：meteorite-solid-crust-v4.bbmodel；SHA256 67b6086e8f54dbc8c29fa6a58b7b8006d239c13b34108c36f22b83aefd372857。
实际预览：bb-hero.png、bb-reverse.png、blockbench-active-view.png；真实 Blockbench 5.2.2 / MCP 1.10.1 原生视口。项目 f35ba9a9-d45b-867b-865d-a58524104fa4。模型嵌入 PNG 与原尺寸导出字节一致；文件哈希见 manifest.json。

换算：16 模型单位 = 游戏 1 格。
轴向与前向：Y 朝上；north=-Z，south=+Z，east=+X，west=-X；静态材质无运动前向。
原点 / 锚点 / 旋转中心：立方体 [0,0,0] 至 [16,16,16]，中心 [8,8,8]；组 origin=[8,8,8]，立方体 origin=[0,0,0]，零旋转。
父子层级：meteorite_v4_material / meteorite_solid_crust_v4。
局部坐标与世界坐标：示意只有静态一格方块；沿用运行载体/方块位置绑定。
尺寸 / 偏移 / 朝向：一格立方体；不改形状、碰撞、落地位置或朝向。全部岩面表现来自像素明暗。
UV / 帧布局：六面 UV=[0,0,32,32]，rotation=0，box_uv=false；同一张 32×32 纹理，无动画条和 .mcmeta。
材质 / 透明：default，普通受光，全不透明；冷黑岩壳、浅灰斜面、边缘暗红橙余热，不要求 emissive/bloom。
时间 / 循环 / 相位：静态单帧，无时间参数。
跟随对象与运动绑定：既有陨岩行为不变，实体命中后续飞直到触地；本稿仅更换视觉纹理。
预览速度：静态观察，不代表施法时长。

运行资源：确认后将原尺寸 PNG 复制至 src/main/resources/assets/xueximod/textures/block/meteorite_v1.png，沿用现有 minecraft:block/cube_all 模型及引用。
示意辅助：示意辅助_地面与 aux_floor_v4，不进入运行资源。
Blockbench 不能代表的运行效果：Minecraft 光照、着色器、动态粒子与运动；游戏实测由用户进行。

状态：仅待确认过程稿；未正式采用、未接入、未编译、未打包。
