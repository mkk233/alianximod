# 视觉接入交接 — 熔岩岩壳 v5

Blockbench 适用性：适用，六面材质和一格体积关系。
版本：meteor-magma-crust-v5（2026-11-02）。
确认：用户 2026-11-02 回复“采用这个版本”。此前“火焰纹理太少了要更像岩浆块”。已归档并替换运行贴图。2026-11-02 02:22:26.8186215+08:00 至 02:22:41.5564331+08:00 的 `compileJava --offline --no-daemon` exit 0，`:compileJava` 为 UP-TO-DATE。游戏待用户实测。
二维源：meteorite-magma-v5.aseprite；32×32，1 帧，5 层；静态，无播放时长或循环。
二维导出 / 素材：meteorite-magma-v5.png，全不透明；SHA256 a0f7027b8b39c4f9f3aaabb9e05713c42e5b0e86697c89d9294848a9b228e1d8。
二维预览：texture-preview.png，最近邻放大，不进游戏。
.bbmodel：meteorite-magma-v5.bbmodel；SHA256 fbd11fd97791f43d95d0a36b37bc95511c34ae26108f99e0de054ed86843fd33。
实际预览：bb-hero.png、bb-reverse.png、blockbench-active-view.png；来自真实 Blockbench 5.2.2 / MCP 1.10.1 原生视口，项目 78785cc2-7705-352b-40b4-690c2d3984e1。最终 PNG 与模型内嵌纹理字节一致，哈希见 manifest.json。

换算：16 模型单位 = 游戏 1 格。
轴向与前向：Y 向上；north=-Z，south=+Z，east=+X，west=-X；纹理本身没有运动前向。
原点 / 锚点 / 旋转中心：方块从 [0,0,0] 到 [16,16,16]，中心 [8,8,8]；组 origin=[8,8,8]，立方体 origin=[0,0,0]，零旋转。
父子层级：meteorite_v5_material / meteorite_magma_v5。
局部坐标与世界坐标：示意是静态一格方块；游戏沿用现有载体/方块的位置绑定。
尺寸 / 偏移 / 朝向：保留现有完整一格立方体，不改变几何、碰撞、偏移或落地朝向；表面没有建模孔洞。
UV / 帧布局：六面共用 32×32 全图；UV=[0,0,32,32]，rotation=0，box_uv=false；不使用动画条或 .mcmeta。
材质 / 透明：default，普通受光，全不透明。暗色岩壳和橙红熔岩，局部亮黄；亮色不是 shader 发光，也不改变方块照明。
时间 / 循环 / 相位：静态单帧，不适用。
跟随对象与运动绑定：沿用陨岩实体命中后沿当前弹道续飞直到触地的现有行为，仅替换纹理。
预览速度：静态，不代表施法或飞行时长。

正式目录：docs/资源库/材质/方块/陨岩_熔岩岩壳_v5/。
运行资源：src/main/resources/assets/xueximod/textures/block/meteorite_v1.png，已按原尺寸 PNG 原样复制。模型仍是 minecraft:block/cube_all，贴图 xueximod:block/meteorite_v1。不要复制放大预览或 .bbmodel。
示意辅助：示意辅助_地面与 aux_floor_v5 纹理，均不进入运行资源。
Blockbench 不能代表的运行效果：Minecraft 光照、着色器、飞行/落地动态、粒子及实际游戏可读性，用户实测。

状态：用户已确认，已接入。这次编译成功，`:compileJava` 为 UP-TO-DATE，没有重新 javac。游戏待用户实测。未改 Java，未打包，未启动游戏。本阶段额度已用完，返修结果未追加复审。
