# 幽冥青蓝增量映射交接

task=`nether-blue-visual`，attempt=`art-coverage-02`。所有新增产物均为未确认候选。以下路径未写完整前缀时，相对于本文件所在的 `coverage/`；运行输入相对于 `D:/alianximod/src/main/resources/assets/xueximod/textures/effect/`。

## 覆盖与保留

- **炉身火层 14 图**：`furnace_v3/{entry|loop}_{warmth,hand_rear,leg_rear,torso,hand_front,leg_front,embers}.png` → `textures/furnace_{entry|loop}_{key}_blue.png`。Flash 确认实际消费者是 HeatUpSpatialMount；每帧 96×112，entry 8 帧/896 高，loop 20 帧/2240 高。实际图层是火焰/余烬，不含金属炉壳，非火焰物件与实体参照保持原色。
- **燃烧挂载 26 图**：`burn_spatial_v1/{entry|loop}/{key}.png` → `textures/burn_{entry|loop}_{key}_blue.png`。13 key 为 cape、rear、shoulder_right、shoulder_left、abdomen_right、abdomen_left、hip_left、shin_right、shin_left、hand、hand_embers_right、hand_embers_left、body_embers。尺寸帧数同上；cape 与 hand 是火焰挂载网格，不当衣物或手部皮肤整件染色。
- **燎里火层 4 图**：`liaoli_v6/back_ignite.png` 128×48×12、`back_flow.png` 128×48×24、`thrust_backflow.png` 128×64×24、`rear_ring.png` 96×96×24 → `textures/liaoli_{名称}_blue.png`。rear_ring 虽在代码称“后环”，实际像素是火焰边缘光环，因此只改此独立光层。
- **燎原余烬 1 图**：`liaoyuan_v3/ground_embers.png` 16×16×24 → `textures/liaoyuan_v3_ground_embers_blue.png`。按火光映射，保留中性/煤灰保护条件；本源 65 个可见像素均属需改色火光，未凭空添加灰色。
- **排除 8 图，逐像素保留**：`liaoli_v6/sword_unlit.png`、`sword_hot.png`；`liaoyuan_v3/ground_ash{,_b,_c,_d}.png`；`surface_anchor_v2/surface_anchor.png`；`ground_dust_v1/ground_dust.png`。参考在 `references/preserve_*.png`，未输出伪蓝稿。热剑 338 个可见像素全部属于实体剑身、与未点燃剑轮廓重叠，外部光晕像素 0，连受热金色刃和红棕握柄一并保持。surface_anchor 是金色三角标识而非火，ground_dust 是土尘；四种灰烬中性灰保持，实施时应绕过旧 lit 乘色，不用蓝图“抵消”乘色。
- **原版复用 3 图**：`references/particle_soul_fire_flame.png` 8×8、`block_soul_fire_0.png` 与 `block_soul_fire_1.png` 各 16×512。从父会话指定的本地 1.20.1 client-extra.jar 只读抽取；对应 `minecraft:particle/soul_fire_flame`、`minecraft:block/soul_fire_0`、`minecraft:block/soul_fire_1`。保留各自 `.mcmeta`：fire_0 顺序 16..31 后 0..15；fire_1 默认顺序。粒子入口与实体着火层目前无相应切换/hook 的事实由 Flash 提供，接入由 Grok 后续完成，不能仅换本目录文件便声称覆盖。

## 同版配色与参数

- 运行幽冥基准沿用 v2，SHA256 `C4FA3B1B82BB29E8BD5040FDE4527BC9DBD8451083608DBB619C2AE02C9CB66F`；v3 草稿不是基准。沿用上一版 palette，非逐通道滤镜，不修改形状、尺寸、atlas 布局、帧序、透明度或 UV。
- 对编码 RGB8 求 `Y=0.2126R+0.7152G+0.0722B`，按以下输入亮度锚点分段线性插值并四舍五入输出 RGB：`0→#000000`、`48→#0B3843`、`Y(210,49,13)→#085C66`、`Y(245,84,13)→#0795A2`、`Y(255,139,20)→#0EC4CC`、`Y(255,193,41)→#44EDE7`、`Y(255,227,103)→#A7FFF0`、`Y(255,248,183)→#E2FFF4`、`255→#F0FFFA`。完全透明像素保持原值，其余 alpha 原样保留。
- ground_embers 保护条件为 `Y<40 || (R-G<10 && R-B<20)`。不要把该条件应用到整把剑、标识或土尘来替代语义排除。
- 旧乘色对照为 `round(R*90/255), round(G*160/255), B`，同 alpha。原色/旧色/新色三列均用同帧；原版魂火复用是独立外观选择例外，不承诺与普通火同轮廓。
- 建议接入模式名 `SOUL_PALETTE_TEXTURE`：领域有效时选择蓝像素图，顶点 RGB 白色 `(255,255,255)`，不得再次叠加 NetherDomainTint；离开领域恢复原图。非火焰物件、烟灰/尘土不走蓝火模式。模式名为交接建议，未写入运行代码。
- Aseprite 预览 entry=60ms、loop=80ms；燎里与余烬预览=50ms。这些仅用于候选动画观看，不修改游戏时序、生命周期、帧采样、速度或尺寸。

## 空间证据与复开

- `blockbench/nether_blue_coverage_02.bbmodel` 为真实 BB 原生可编辑工程；整体/侧面共 8 张截图在 `previews/bb_*.png`，具体相机、阶段与帧见 `evidence/blockbench-captures.json`。总览 `previews/coverage_overview.png`，原/旧/新从左到右。
- 升温、燃烧的既有批准空间模型通过 BB 原生 merge 作为几何依据，保留挂载面与局部 UV；三列仅平移，炉身贴身火、披焰/拳火分别可比。燎里借用既有站立剑阵参照，实体剑不改色，独立火层沿剑轴排布；F10 刺击贴图侧面不等于俯冲姿态复现。原版着火为实体参照上的叠层/粒子代表示意，不是已实现的 runtime hook。
- 45 图完整尺寸、每帧高度、帧数、源文件与导出哈希在 `manifest.json`。`evidence/aseprite-verification.json` 记录 alpha/轮廓/源导出差异全 0；`evidence/blockbench-reopen-verification.json` 记录原生保存复开与其他 8 个页面保护结果。

## 既有 v1 用途不变更文件

- 旧候选 `fentian_conflagration` 当前用途是 **引燃_承接焚天_v4**，正式原图 `D:/alianximod/docs/资源库/特效/火焰/引燃_承接焚天_v4/kindle_v4-strip.png`，128×7488，SHA256 `3AE6A95E41D92991B828EDC7491CBB40A18D503CDCF700281A50D901D6AA6A76`。
- 旧候选 `kindle_v3_screenshot` 当前用途是 **焚天_全身烈焰_v3**，正式原图 `D:/alianximod/docs/资源库/特效/火焰/焚天_全身烈焰_v3/fentian_v3-strip.png`，48×1216，SHA256 `EF3476EB01225D2E7478F9BBA5430F28BCF2E1D12198F3F482A634C155A04070`。Sartre 互换完成依据父会话最终事实，本轮不改其文件。
- 原 12 套保持不动；既有资源索引与幽冥实际运行基准矛盾记录沿用，不覆盖别人的索引。新增 coverage 须经展示确认后再接入，全部写入已停止，无下属。
