# 陨岩 · 熔岩岩壳 v5

采用状态：用户 2026-11-02 看过这一版 Aseprite 和真实 Blockbench 预览后回复“采用这个版本”。此前要求“火焰纹理太少了要更像岩浆块”。版本 `meteor-magma-crust-v5`。正式归档和运行贴图已替换。2026-11-02 02:22:26.8186215+08:00 至 02:22:41.5564331+08:00 在项目根执行 `.\gradlew.bat compileJava --offline --no-daemon`，exit 0。`:compileJava` 为 UP-TO-DATE，没有重新运行 javac。已接入，这次编译成功，游戏待用户实测。本阶段额度已用完，返修结果未追加复审。

外观是 32×32、静态、1 帧、全不透明。六面用同一张图，体积仍是一格立方体，材质是普通受光。平整暗色岩壳之间是成片橙红熔岩，局部亮黄，没有孔洞。图上的亮黄只是颜色，不是新加的真实发光、动画或方块照明。预览里的地面不进游戏。

- 可编辑源：`meteorite-magma-v5.aseprite`，5 层。
- 原尺寸导出：`meteorite-magma-v5.png`。SHA256 `a0f7027b8b39c4f9f3aaabb9e05713c42e5b0e86697c89d9294848a9b228e1d8`。
- 二维预览：`texture-preview.png`，最近邻放大，不进游戏。
- 示意模型：`meteorite-magma-v5.bbmodel`。SHA256 `fbd11fd97791f43d95d0a36b37bc95511c34ae26108f99e0de054ed86843fd33`。16 模型单位等于游戏 1 格。
- 原生视口：`bb-hero.png`、`bb-reverse.png`、`blockbench-active-view.png`。
- 运行贴图：`src/main/resources/assets/xueximod/textures/block/meteorite_v1.png`，与原尺寸 PNG 字节相同。方块模型仍是 `minecraft:block/cube_all`，贴图 `xueximod:block/meteorite_v1`。没有动画 `.mcmeta`。
- 过程稿：`docs/资源库/过程归档/陨岩_熔岩岩壳_v5-20261102/`。制作脚本留在那里。
- 旧稿：`docs/资源库/材质/方块/陨岩_v1/` 保留。

换图不改变玩法。命中实体后仍沿当前弹道续飞，触地才落块。伤害、碰撞、体积和照明逻辑未改。没有改 Java。这次只做上述编译，`:compileJava` 为 UP-TO-DATE。
