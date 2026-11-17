Blockbench 适用性：适用
版本：水幻形 Steve 水线 v2.1。用户 2026-11-02 原话「这个版本好接入吧」，确认采用这一版散水并授权接入。造型和两张材质沿用已认可的 v2，散水是本版。
确认：用户，2026-11-02。绑定 `docs/开发记录/水幻形术式-20261102/visual-candidate-v21-manifest.json`。
二维源：不适用。用户直接在 Blockbench 画材质，没有新的 Aseprite。
二维导出：不适用
二维预览：不适用
素材：
- `steve_water_lines.png`，128×128，逻辑 UV 64×64，2687 字节，SHA256 `9E7E5B01A7E09C393D3FFA1800468BD61AE919F68CF628152B00D9C347793DA5`
- `water_flakes.png`，32×32，逻辑 UV 32×32，608 字节，SHA256 `CEFDEE9E3E3FB7F701E19FC9CB3438C586C4841D3A50E3F8C9399A635BD4D70C`
.bbmodel：`water_phantom_steve_v2_1.bbmodel`，281867 字节，SHA256 `4ADFCC75ED1E06E62ECE5FD05E6B109787FB1AC46B29CBFC5A8AF8C5425695F7`。项目 UUID `14aa37f5-ace2-b389-1849-312f5fea6a29`。格式 free。
实际预览：同一目录的真实视口。`bb_hold_three_quarter.png` SHA256 `0C214615F19ADBAF7F2E9CF85DAE3125DE6D9002EF59585CBD6A395E036A3D8B`；`bb_break_mid.png` SHA256 `057F1898C14BE758AB01845ADEC135D7C4B06EBCFC7762564DCD8282F0CE852D`；`bb_ground_drops.png` SHA256 `D0116B3D97ED65EB4524FB88B138A7D6E5BE72B4E0958E0555D3E7DA0A5B0AF6`；`bb_preview_50fps.gif` 1921027 字节，SHA256 `AB9EE8399F3C321ADECFBB3CCAE179653AF5242283FCC1DD8AF96F29A5414901`。视口 `steve_v21`，960×720。99 张逐帧留在过程目录。

换算：几何高 32 模型单位，脚底 y=0，头顶 y=32。运行时 1 模型单位 = 原版 ModelPart 的 1 像素 = 1/16 格。先套 Forge 1.20.1 `LivingEntityRenderer` 的变换：绕 Y 转 `180 - 身体朝向`，再 `scale(-1, -1, 1)`，再下移 1.501 格。脚底因此约在实体位置上方 0.001 格，可视高度 2 格。没有把 32 单位硬拉成 1.8 格。碰撞箱没改。
轴向与前向：确认模型 +Y 向上、面向 -Z。写入运行网格前把 Y 翻进原版模型空间（原版像素 y = 24 - 模型 y）。+X 左臂枢轴 [5, 22, 0]，-X 右臂枢轴 [-5, 22, 0]。生成朝向用实体已冻结的身体朝向，散水快照用通知里的视线。
原点 / 锚点 / 旋转中心：`steve_root` [0, 0, 0]。头和躯干 [0, 24, 0]，右臂 [-5, 22, 0]，左臂 [5, 22, 0]，右腿 [-2, 12, 0]，左腿 [2, 12, 0]。世界锚点是脚底。
父子层级：六个方体、48 个短片 `flake_00` 到 `flake_47`、20 个水滴 `drop_00` 到 `drop_19`。`drop_00` 到 `drop_07` 是驻留高光，`drop_08` 到 `drop_19` 是落地水滴。
局部坐标与世界坐标：根不转时，方体 from/to 是模型坐标。短片和水滴的位置通道是相对各自组原点的位移。运行网格把顶点收成相对该原点的 1/16 格局部坐标。
尺寸 / 偏移 / 朝向：头 [-4, 24, -4] 到 [4, 32, 4]，宽臂，方头，方角。短片仍是约 1.15×0.72×0.24 的扁片。位移曲线原样保留，先慢后快。
UV / 帧布局：皮肤 UV 按逻辑 64 去除，贴图是 128。水片 UV 按逻辑 32 去除，贴图是 32。单帧。像素没有改。
材质 / 透明：双面、不写深度、自发光半透明，顶点亮度拉满。方体外缘仍是贴图里的水色，内部透明。淡出仍是缩放。
时间 / 循环 / 相位：动画 `water_phantom_steve_scatter_v21`，长 1.96 秒，只含 step 和 linear。驻留循环用 0 到 1.12 秒。逻辑触发后的散水从 1.12 秒起，连续时间加 partialTick，0.84 秒后到达 1.96 秒终点。六个方体在 1.12 秒阶跃隐藏。
跟随对象与运动绑定：不跟随玩家骨骼。驻留画在活实体上，散水画在脱离实体的快照上。两触发形式共用这一段散水。
预览速度：1.96 秒和 GIF 每帧 20 毫秒只是演示。运行时不按 99 帧跳，也不把 GIF 送上网。

运行资源：
- `assets/xueximod/textures/entity/xueximod/steve_water_lines.png`，字节与本目录皮肤相同
- `assets/xueximod/textures/entity/xueximod/water_flakes.png`，字节与本目录水片相同
- `assets/xueximod/models/effect/water_phantom_steve_v21/lines.bin`，60666 字节，SHA256 `26EF5093F3FC9CAD03272F7BDFAD06DF771953E161B69310B2FF7E37B89F9025`。74 个部件的局部方体和关键帧，不含假人、地面和整个 bbmodel
示意辅助：`auxiliary_scale` 下的头、躯干、双臂、双腿，以及 `auxiliary_ground`。不进 lines.bin，也不进运行贴图。
Blockbench 不能代表的运行效果：命中、伤害、接触、装配链、网络同步、碰撞箱，以及游戏里还没看过的实际画面。
