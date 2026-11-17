我先读阶段 B 的任务说明，确认授权范围后再接上次的产种账。阶段 B 要完成灼地注册和通用接入。我先读本批已裁决事实和现有释放、领域、视觉接口，再按那个范围落代码。阶段 A 的账本会原样接上。接下来补灼地的宿主、方形贴地检测、绑定和画面。包和登记还差画面包、客户端绘制，以及几处调用点。灼地阶段 B 已按已定方案落地。术式编号 `fire_scorch_field`，显示名「灼地」。原生接触是 1 点伤害加首次 20、周期 15 火种；绑定和附魔走现有通用入口。制作表和 `记录.md` 没有改。游戏内尚未实测，实现正式复审仍是 0/3。

## 登记与费用

`ModTechniques` 196–199 行、264 行、304 行，`ReleaseDefinitions` 44 行。

- 类型 MAGIC，属性 FIRE，基础伤害 1，冷却 30，等级上限 20，复杂度 0，弹速 0
- `HIT_VOLUME` / `SUSTAINED` / `ATTACK`，寿命 100，不移动，弹道不可改，不要求持剑
- `fieldRadius()` 仍是 0，`domain()` 仍是 null，所以不会进燎原场地生成，也不会走 `domain.initialCost`
- 显式魔力 `-1`，显式装配 `-1`。按底座公式，魔力是 `ceil(1×2)=2`，装配是 1。这是公式结果，不是已批准的平衡数，也没有场地维持费
- `explicitDomainHost()` 只有灼地为 true

给制表的一句说明：在脚前合法地表铺开边长4格的贴地火场，持续100刻，接触的敌人按首次和停留周期获得火种。

## 运行路径

一次按键在 `SkillUsePacket` 436 行把显式宿主的绑定计划和附魔快照记进 `ReleaseBatch`（79–86 行）。条件组在 `CastExecutionSession` 256 行同样记到那本账。同一术式编号写两次时，后写覆盖前写。`ScorchEffect` 只打开宿主。

落点：默认在脚前方水平视线 1 格，用已有 `StandSurface.columnTop`，上下容差 3。这两个数是实现默认值。已有 `CastFrame.overrideGeometry` 且 `origin` 存在时，改在该 XZ 上、以 origin 的 Y 为参考做同一次合法顶面搜索。找不到顶面就不生成，也不占释放名额。中心和参考高度在生成后固定。

检测在 `SurfaceContactField.collect`（30 行起）。世界坐标轴对齐正方形是 `[中心X-2, 中心X+2] × [中心Z-2, 中心Z+2]`，边长 4 格。每格读真实碰撞，取朝上暴露的顶面，顶上留满 1 格净空才生成检测盒；被挡住的部分切掉。半砖、楼梯按各自顶面拆开。同一水平位置在参考高度上下 3 格内的多片顶面都保留。无支撑、流体、虚空没有盒子。墙的侧面没有盒子，范围内且有净空的墙顶可以有。不相连的碎片不填洞。未加载区块不读。同步切片超过 128 片时只保留面积较大的 128 片。

`SurfaceFireHosts.settle`（201 行）在服务器刻末、优先级 HIGH 上跑。同一刻的几何可复用。出生刻在 `open`（56 行）里就检测。年龄用 `birth+100`：当前刻到达到期刻时先 `close`（368 行），不再检测、产种或结算绑定。所以实际生效是出生刻起的 100 刻，周期点仍是 20、40、60、80。地形变化下一刻就进几何。支撑全部消失时宿主继续走到原寿命，没有盒子就没有效果，恢复后不重置寿命和奖励。施法者离线或死亡时，这一刻跳过接触，宿主不结束。

接触必须是活着、同维度、过 `ContactPolicy` 和 `DamagePermission`，并且身体盒与至少一片真实检测盒有正体积相交。并集盒只用来粗查。脚点、圆盘和并集盒都不能代替。边界贴住不算。

火种仍用阶段 A 的 `ScorchSeedLedger` 和 `FireSeedProduction`，本阶段没有改这两个文件。释放令牌是 `ReleaseBatch.fireContext().castId()`。`claimFirst` 成功后另打 `FireDamage.strikeOnce` 1 点（火基 1，炉心按已结算），然后 `produce` 20。伤害失败或实际增加 0 都不撤回首次资格。这个宿主在这一刻不再 `admitPeriod`。其它正在接触的宿主仍走共享 20 刻门。周期只在 20/40/60/80 `produce` 15，不造成伤害、不发命中、不引燃。不补跑。首次不吞掉周期门。倍率读施法者当前炉心，向下取整，上限 100。结束时 `releaseHost`，持有数到 0 才 `discardRelease`。离开、换人、单场结束不清除周期门。服务器停止才 `clearTransient`。登出只关该玩家的宿主，维度卸载只关该维度的宿主。这些清理都不删除已有燃烧和火种余额。

## 附魔实际接法

有附魔时，`EnchantmentSnapshot.from` 存在快照，宿主把自己交给现有 `ContactEnchantState.open`。没有附魔时 `from` 返回 null，不打开接触状态。`ContactCast.snapshot` 对正伤害仍返回 null，灼地不走那条。

这条现有接触链的实际顺序是：每 2 刻采样，`TechniqueHits.hurt` 成功之后才 `FireSeedGrant.deposit`（倍率固定 1），然后 `applyEffects`。燃烧秒数大于 0 时会 `setSecondsOnFire`。所以附魔接触依赖一次成功的伤害，并且会按附魔自己的火种和点燃走。它与原生的 1 点伤害、首次 20、周期 15 是两条账。原生产种不经过 `TechniqueHits`、`FireSeedGrant.deposit` 或点燃。接触伤害不大于 0 的附魔不会进入这条链。原生结算在刻末 HIGH，接触状态在其后的普通优先级，同一刻的无敌帧可能让附魔那一下延后。

## 领域绑定实际接法

`ReleaseDefinition.explicitDomainHost`（195 行）默认 false。`BindingStructureCompiler.domainHost`（429 行）对这个标记直接当作宿主，正伤害也可以；其余仍是旧规则：非附魔、伤害不大于 0、且 `domain()!=null`。`qualifiedAttack`（451 行）排除显式宿主，避免把宿主收成被绑定的攻击。灼地不进入 `DomainService.tryWave`。

宿主在出生时，只有绑定计划里真有成员才登记到 `BoundHostBridge`。`DomainBindingRuntime.tick`（39 行，52 行并入）在没有旧领域时也会调度这些宿主。节奏仍是出生后 20 刻第一拍，之后每 20 刻，费用仍走 `BoundReleases.executeAutomatic`。

`SurfacePatchGeometry.selectsByIntersection` 为 true。新宿主选目标用正体积相交；旧领域几何这个方法默认 false，仍是完整包含。采样按顶面面积加权，点在顶面上方约 0.05 格、并留在 1 格净空盒内。没有合法切片时 `samplePoint` 为空，节拍已经推进，`releaseOne`（107 行）在收费前返回，不补跑。到期或提前结束会取消未完成轮次并取消登记。绑定攻击和灼地产种互不占用。年龄到达 100 时，宿主在绑定结算之前关闭。

## 资源

没有新图，也没有把素材复制进资源库。客户端用同步下来的切片铺火，复用：

- `assets/xueximod/textures/effect/liaoyuan_v2/ground_flame.png`
- `assets/xueximod/textures/effect/liaoyuan_v2/ground_embers.png`

旧的 `GROUND` 包和圆盘绘制保持原样。新种类 `SURFACE`（6）、`SURFACE_END`（7）的切片字节只加在原有固定字段之后；`SPARK`（8）没有附加字节。几何签名变了才立刻重发，另外每 40 刻补一次给后进入的客户端。客户端已经有这场火时，重发只换切片，不改出生和结束时刻。实际增加火种大于 0 时，从相交的那片合法顶面朝身体放橙色火焰粒子。样式值留着，当前绘制和粒子都用上述橙色贴图。

待验收一句：灼地沿用燎原地表火焰与余烬两张贴图，没有新图，待验收。

## 用户实测

- 编号名称：`fire_scorch_field` / 灼地
- 前置：创造或有魔力的生存，装配灼地；面前 1 格内有可站立的合法地表；旁边有可伤害的非盟友生物
- 步骤：朝地表释放一次，让生物踏进火场并停留到约 2 秒、4 秒；再在半砖或台阶上放一次
- 预期：脚前出现贴着各方块顶面的 4×4 火，约 5 秒后消失；第一次接触 1 点伤害且火种增加，之后在第 20、40、60、80 刻再增加，不点燃、不每刻跳伤害数字；半砖和台阶上的火跟着各自顶面，而不是只铺最高的一面

## 未完成

地表定位和幽冥都没有做，也没有蓝火贴图。表里的触发时机仍空着，数据表枚举没有改。切片上限 128。同一次按键里同一术式编号的后写计划和快照会覆盖前写。附魔接触仍要先打中，并可能按附魔点燃、以倍率 1 存火种。本会话没有跑 `gradlew`，没有打包，没有进游戏。实现正式复审是 0/3，代码写完不等于游戏内已经验证。

## 命名门禁

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 1。只有两处，都在本次没改的 `OriginalHumanoid.java`：165 行 `i`，202 行 `to`。本次改过的文件没有命中。

## 文件与最后写入时间

本阶段这些文件由本会话写入。时间是本机本地时间。

| 文件 | LastWriteTime |
| --- | --- |
| `HostView.java` | 2026-10-30 20:44:55.180 |
| `BoundHostBridge.java` | 2026-10-30 20:44:56.994 |
| `SurfaceContactField.java` | 2026-10-30 20:45:14.936 |
| `SurfacePatchGeometry.java` | 2026-10-30 20:45:20.459 |
| `ScorchEffect.java` | 2026-10-30 20:45:22.566 |
| `ExplicitHostOffers.java` | 2026-10-30 20:45:24.988 |
| `DomainGeometry.java` | 2026-10-30 20:46:12.486 |
| `DomainInstance.java` | 2026-10-30 20:46:17.160 |
| `DomainBindingRuntime.java` | 2026-10-30 20:46:31.201 |
| `ReleaseDefinition.java` | 2026-10-30 20:46:36.247 |
| `BindingStructureCompiler.java` | 2026-10-30 20:46:38.547 |
| `ReleaseBatch.java` | 2026-10-30 20:46:52.788 |
| `SkillUsePacket.java` | 2026-10-30 20:46:53.035 |
| `CastExecutionSession.java` | 2026-10-30 20:46:53.549 |
| `ModTechniques.java` | 2026-10-30 20:46:58.578 |
| `ReleaseDefinitions.java` | 2026-10-30 20:47:15.851 |
| `FireVisualPacket.java` | 2026-10-30 20:47:42.174 |
| `FireVisualClient.java` | 2026-10-30 20:47:59.374 |
| `zh_cn.json` | 2026-10-30 20:48:00.109 |
| `en_us.json` | 2026-10-30 20:48:01.022 |
| `SurfaceFireHosts.java` | 2026-10-30 20:51:16.673 |

阶段 A 的 `FireSeedProduction.java` 与 `ScorchSeedLedger.java` 本阶段没有再写。

阶段 B 到此停写。待统一编译：`.\gradlew.bat compileJava --offline --no-daemon`。本会话没有跑这条命令。
