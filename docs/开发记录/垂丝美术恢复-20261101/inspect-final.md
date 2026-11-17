我先读这份任务说明，确认授权范围后再按它执行。任务是只读定位：核对垂丝运行素材和归档原版，并查清实际生效的美术改动。我先对相关路径做哈希、尺寸和 Git 记录比对。四张运行贴图和归档原版哈希一致，且 Git 里都是未跟踪。接下来对照渲染代码和接入时的基线，看外观变化是不是出在引用或 UV。四张瀑布运行贴图没有被换掉，渲染器也没有改引用或 UV。现在能看见的差别，是今天把水帘出生宽度从 0.5 格改成 1.2 格，同一套 4px 分件被多铺了几遍。

## 事实

游戏目录里的四张 PNG 与 `docs/资源库/特效/水流/垂丝聚能瀑布_v1/` 逐字节相同，修改时间仍是 2026-10-26。Git 不跟踪它们，`git log --all` 也没有 waterfall 记录，没有可检出的另一版。

| 文件 | 尺寸 | SHA256 | 与归档 |
| --- | --- | --- | --- |
| `waterfall_runtime_left.png` | 4×2048，8 位 RGBA | `DEC6990BCF962707FBC25E5218837DF7ADD4A34227C95F58C65F0E27428443E8` | 一致，也与 `asset-hashes.json` 一致 |
| `waterfall_runtime_middle.png` | 4×2048，8 位 RGB，无 alpha | `6BB9C161784BB89F9F5B95580EF56AC92E53ECDD5ED694EDAE923F29FA73AAE7` | 一致 |
| `waterfall_runtime_right.png` | 4×2048，8 位 RGBA | `CF6D6241880EF0833ABB53EB46EA847581BD52F15B3AF4A16EE4CD12D70A368F` | 一致 |
| `waterfall_thin.png` | 64×2048，8 位 RGBA | `E1DA1C1DC49776AD1BFBDB09B8EC01E919D6354F9CB09BFB1E8236759BE9A76D` | 一致，与 2026-10-26 接入记录一致 |

像素也还是当时那套：左右每帧相对上一帧下移 8 像素，第 15 帧卷回第 0 帧，错位为 0。水色是 `#143F7A`、`#126AAF`、`#199ED2`、`#66D8EA`、`#C6F8FA`、`#F5FFFF`。左右有透明边，中段四列全不透明，这在接入记录里已经写明，不是后来改坏的。没有 `.mcmeta`。旧的 8px `waterfall_left/middle/right.png`（8×2048、16×2048、8×2048）和 `waterfall_flow.png`、`waterfall_charge.png` 仍只在资源库，游戏代码不引用。

`WaterCurtainRenderer.java`、`WaterCurtainLayout.java` 的修改时间是 2026-10-27 04:37，与当天审查快照全文一致，今天没动。上一轮说没改渲染器，对当前文件成立。

实际绑定仍是这四张，见 `WaterCurtainRenderer.java` 第 27–38 行。`TechniqueHitRenderer.java` 第 84–86 行仍把 visual 7/8 交给它；同文件第 30 行的 `shulker/spark.png` 不走这条。材质仍是实体半透明、最近邻、无 mipmap、`NO_CULL`、全亮，顶点 alpha 255；普通白，重水 `(210,232,255)`，第 39–41、87–104、132–134 行。横向仍是 24 像素/格，左右边缘各 4px（1/6 格），中段按 4px 重复，末段连 U 一起裁；纵向按当前帧铺，不把 128px 拉满整条帘。窄于两条边缘才用 thin，U 固定 25/64–39/64。`WaterCurtainLayout.java` 第 14–25、118–165、167–205 行。

## 生效的外观变化

今天改过的是宽度，不是贴图。

- 2026-10-27 快照和 2026-10-30 备份 `WeepingCurtainEffect.java` 第 34 行都是 `spec.width = 0.5`。
- 现在 `WeepingSettings.java` 第 17 行是 `SHEET_ACROSS = 1.2`，`WeepingCurtainEffect.java` 第 35 行把出生宽度设成它。`WeepingCurtain.sheetAcross` 原样把实体宽度交给盒子，渲染器第 50 行读 `entity.width()`。

0.5 格正好是左 4px + 中 4px + 右 4px，也就是接入时的最细铺法。1.2 格仍是左右各一条固定边，中段变成 5 整段再加 U=0.2 的裁切（约 0.8px）。密度还是 24 像素/格，没有把纹素拉宽。中段贴图没有透明，多铺几次后，不透明带从 4px 变成大约 20.8px，帘看起来更宽、更实。无聚能时就是这个宽度；聚能仍会再乘 `volumeScale`，底数已经从 0.5 换成 1.2。0.5 和 1.2 都不会走到 thin。

上沿移动不改这套铺法。`WeepingLipMotion` 只挪上沿，`WeepingCurtain.fit` 仍从当前上沿沿世界竖直轴挂下，再用 `resizeVolume(厚, 宽, 活动高)` 摆盒子。渲染器的 Y 是竖直偏移，水纹不沿飞行方向斜铺。

## 最小恢复

- 素材：不用拷。游戏四张已经等于归档。不要改去贴 8px 分件、`waterfall_flow` 或 `waterfall_charge`。
- 渲染：`WaterCurtainRenderer`、`WaterCurtainLayout` 不用改。`docs/资源库/过程归档/垂丝瀑布接入-20261026/baseline/` 里的渲染器画的是 `shulker/spark.png`，那是接瀑布之前的版本，退回去会丢掉瀑布。
- 若要回到接入时的铺法：只把出生宽度从 1.2 改回 0.5。这是尺度，不是新美术。
- 不回滚：上沿走弹道、帘身垂直下泻，以及锁定、追踪、领域已经摆位后不再横移。这些不换贴图。

需要改 Java 的只有「恢复 0.5 格」这一项，落在 `WeepingSettings` 和 `WeepingCurtainEffect` 的宽度赋值。渲染器和贴图都不用动。

## 阻塞

Git 里没有这些 PNG，也没有接入后的渲染器，不能从提交恢复。归档原版在，而且已经和游戏文件相同。更早的 spark 渲染器不能当作瀑布原版。这次没有进游戏，1.2 格的观感是按现有分件公式推出来的。