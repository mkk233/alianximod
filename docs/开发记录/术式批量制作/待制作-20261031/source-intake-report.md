# 新批次源码事实采集

阶段是只读信息采集，给唯一 Astra 的本批第 1 轮设计裁决提供底座事实。不是设计或实现正式复审，不计轮次。没有正确性结论，也不表示任何一项可验收。

源码、资源、制作表和本目录 `记录.md` 都没有改。没有编译、测试、打包或启动游戏。本单只写本目录 `source-intake-report.md` 与 `source-intake.json`。写完即停止。

输入是本目录 `intake.json` 的 13 项待制作行。表哈希 `3905785BD053899D1CA429E07E4942E10F0B205AA2403013762C44B566D8A183`。现行入口对照 `docs/底座功能点.md`（2026-10-30）和 `docs/交接索引.md`。`src/main/java` 下这 13 个编号没有匹配。

## 1. 十三项可复用入口与缺的枚举

普通附魔不登记 `ChainRole`。现有五件是 `magic_ignite`、`vine_mark`、`tide_mark`、`sinking_edge`、`keen_mark`：`ModTechniques` 用最后一个参数 `true` 标成附魔，`EnchantmentDefinitions.registerAll`（约第 8–15 行）只登记元素和效果键 `fire` / `wood` / `water` / `earth` / `metal`。`ReleaseDefinitions.registerAll` 没有它们。`ReleaseRegistry.get` 为空时，`TechniqueChain.waitsForHost`（约第 49–52 行）返回 false；切段时 `isEnchantment()` 为真就进入附魔缓冲（`TechniqueChain.split`，约第 29–40 行），挂到后面第一个不等宿主的段，链尾剩下的各自成段。

`ReleaseDefinition.role()` 的接口默认是 `ATTACK`（`ReleaseDefinition` 约第 100–102 行）。`FieldSpawner.roleOf`（约第 87–92 行）在定义为空时也返回 `ATTACK`，只供场地扫描。这两处都不是附魔的链上角色登记，也不能用来把表上留空的链上角色补成攻击或动作。这 13 个编号目前都还不是 `ReleaseDefinition`。

数据表「链上角色」只有：攻击、动作、落点指定、锁定实体、追踪、垂落改写、三维交错、一分为三、双重释放。没有地表定位、喷涌、错时、命中接续、附魔、留焰。代码 `ChainRole`（`ChainRole.java` 第 6–17 行）是 `ATTACK`、`ACTION`、`SEEK_SPACE`、`SEEK_ENTITY`、`TRAJECTORY_TRACKING`、`TRAJECTORY_DROP`、`TRAJECTORY_SCATTER`、`RULE_TRIPLE`、`RULE_DUAL`、`RULE_DOMAIN_BIND`。表的下拉名单里没有 `RULE_DOMAIN_BIND`；代码里有，对应已装配的领域绑定。

数据表「弹道」只有：直线、垂落、跟随准星、曲线函数、龙卷。没有喷涌。代码 `Trajectory` 是 `STRAIGHT`、`DROP`、`FOLLOW_LOOK`、`FUNCTION`、`VORTEX`。

数据表「触发时机」有命中成功、燃烧结算、领域周期等，没有父攻击首次接触、火径接触或有效撞地。没有点燃时长字段，也没有术式地形破坏开关字段。

魔力、装配消耗、等级上限、复杂度在这 13 行都空。表头批注 L2 写附魔装配消耗为 2、规则魔力为 0、其余为伤害向上取整后乘 2；`Technique.assemblyCost`（约第 86–95 行）和 `Technique.manaCost`（约第 97–116 行）是同一套。各行 `DV` 写明这些格未定，不用 0 代替。0 在代码里有别的意思：复杂度 0 表示默认解锁（`Technique` 类注释），规则种类魔力为 0，显式魔力 0 表示这发不耗这份魔力（表头 `AP2`）。所以这些空格不能用代码默认或表头批注填上。

`PlayerShuData.isUnlocked`（约第 198–207 行）：通用和动作始终解锁；剑术、法术用对应等级对比复杂度。领域许可另开。装配界面走已学且已解锁。这只说明空着的复杂度还不能登记，不表示可以先写成 0。

### 留焰 `fire_flame_trail`（待制作第 3 行）

已填：通用、火、附魔是、可接受附魔否、释放种类规则、释放标签持续、必须手持剑否、资源与状态否。链上角色和弹道空。`N` 要求附在会移动的载体上，沿真实路径留短暂火径，和载体共用同一次施法上下文与同一目标命中计时，不产种不耗种，不放原版火，不破坏地形。

可对照的现有行为：附魔不单独生成命中体，而是挂到宿主段；`TrajectorySampler.sample` 每 tick 给载体取样，不留下一段段独立判定。没有火径寿命、宽度、采样间距、片段上限的字段或默认。费用、冷却、伤害映射未定，不能用点火附魔的 2 点或火球的数值代替。

### 陨岩 `fire_meteorite`（第 4 行）

已填：法术、火、基础伤害 24、基础冷却 40 tick、附魔否、可接受附魔是、命中体、触发、链上角色攻击、必须手持剑否、接受弹道改写是、弹道可被修改是。弹道空。`N` 另写中心水平半径 1.5、与热波组合时垂直总高 3、本体约一个方块、受重力、第一次合法实体或地形碰撞后中心一次物理冲击、最后留下可存档可采掘方块。

可复用的类别开关与火球同层：`ReleaseDefinitions` 第 23 行 `fire_orb` 是 `HIT_VOLUME`、`TRIGGER`、`STRAIGHT`、`moving=true`、`acceptsTargetOverride=true`、`acceptsPathOverride=true`、`ChainRole.ATTACK`、`trajectoryMutable=true`。`ReleaseDefinition.acceptsInitialAim` 默认跟着 `acceptsTargetOverride`（约第 39–40 行）。这些开关能对上「允许垂落、喷涌等弹道修改」里已经存在的垂落；喷涌本身还没有弹道枚举。

不能沿用的火球数值：`ModTechniques` 第 172–175 行伤害 5、冷却 20、等级上限 20、复杂度 0、弹速 1.2；`ReleaseDefinitions` 第 23 行寿命 40、速度 1.2、厚度 0.4；`FireOrbEffect` 第 19 行原版燃烧 4 秒，第 34–43 行盒子 0.45、视觉 4、模型缩放 1.2、产种 3。陨岩 `DV` 写明抛射速度和弹道未定，不填成直线或垂落。

命中体默认 `noPhysics=true`、`setNoGravity(true)`（`TechniqueHitEntity` 约第 180–181 行）。方块裁剪只在 `burstOnImpact` 为真时做，碰到后调用 `TechniqueHitSettle.onExpire`（约第 370–384 行），不是带重力的落体，也不是可存档方块。`ModBlocks` 现有术式块、印台和源质矿，没有陨石方块。

### 热波 `fire_shock_ring`（第 5 行）

已填：法术、火、基础伤害 4、基础冷却 16 tick、附魔否、可接受附魔是、命中体、触发、链上角色攻击、必须手持剑否。弹道、接受弹道改写、弹道可被修改都空。`N` 写内半径 1.5、外半径 5、中间为空、只命中一次；单独使用以施法位置为中心，接续时用父攻击的事件点；与陨岩配套时排除中心目标名单。击退未定。

现有形状在 `TechniqueHitSpec` 和命中结算里是弧扇、有向盒、轴对齐盒、球、尺寸盒。没有内半径留空的环。没有把一次攻击的命中名单交给下一次攻击做排除的接口。`ReleaseBatch.Tally` 的去重是同一账本内同一目标、同一轮、同一复制序号，不是父子两段之间的排除名单。

### 地表定位 `rule_surface_anchor`（第 6 行）

已填：通用、无、附魔否、命中体、触发、必须手持剑否。链上角色和弹道空。`DV` 写明不能填成落点指定或垂落。

现有落点是 `rule_cast_anchor`，`ChainRole.SEEK_SPACE`（`ReleaseDefinitions` 第 28 行）。`ReleaseBatch.crosshairCenter`（约第 154–178 行）从眼睛沿视线最多 32 格裁到碰撞体，再在线段 1.5 格内改选生物中心。`CastFrame` 的落点立方半宽是 1.5（约第 94、185–190 行）。这是空间取点，不是「可见地表上方、能放下整颗本体、不合法就拒绝」的锚。没有地表占用检测，也没有失败即取消整次释放的地表角色。

### 喷涌 `fire_upwell`（第 7 行）

已填：通用、火、附魔否、命中体、触发、必须手持剑否。链上角色和弹道空。`DV` 写明上抛速度、锥角、飞行距离未定，不能记成垂落。

现有垂落：`trajectory_drop` 的角色是 `TRAJECTORY_DROP`，弹道 `DROP`（`ReleaseDefinitions` 第 20 行）。`CastFrame` 在路径覆盖为 `DROP` 时把原点抬高 8（`DROP_LIFT`，约第 92、190 行）。之后 `JavaReference` 约第 64–68 行按 `0.5 * 0.08 * age²` 从该原点向下掉，水平速度不保留，方向固定向下。风暴轨迹 `trajectory_storm` 用的是 `Trajectory.VORTEX`，角色仍是 `TRAJECTORY_DROP`（`ReleaseDefinitions` 第 22 行）。没有向上抛出再落回、也没有多份锥角分配的弹道。一条路径声明会盖住另一条，这是现有「靠近宿主的最后一条路径留下」的行为，不是喷涌、垂落、风暴可以叠成一条复合运动的接口。

### 错时 `rule_stagger`（第 8 行）

已填：通用、无、附魔否、规则、触发、必须手持剑否。链上角色空。`DV` 写明不能填成一分为三。`N` 里的 0、10、20 tick 只是三份示例。

现有延迟只挂在多重成员序号上。`MulticastExpansion.MEMBER_GAP_TICKS = 3`（约第 12 行），`gapTicks`（约第 93–101 行）是序号乘 3，序号 0 为 0。`CastExecutionSession.beginGroup`（约第 255、290–298 行）让同一组后续成员走 `ChainScheduler`。同一成员上的复制份共用这个序号，不会彼此错开。`RULE_TRIPLE` 只乘份数（`ReleaseDefinitions` 第 29 行，`ReplicationSpec.of(3)`），不另给时间。没有「不增加份数、只把已有副本错开、并冻结当时目标与锚点」的角色。后续波次费用未定，不能把成员间隔 3 tick 或账本冲刷 6 tick 当成这行的间隔。

### 命中接续 `rule_hit_continue`（第 9 行）

已填：通用、无、附魔否、规则、触发、必须手持剑否。链上角色空。`DV` 写明不能填成落点指定或一分为三。

现有接触回调是 `TechniqueHitSpec.HitListener`：`onHit(LivingEntity)` 和 `onExpire(Vec3)`（约第 55–58 行）。`burstOnImpact` 的方块裁剪走到 `onExpire`，不走普通伤害结算。`TechniqueCastContext.onFirstTrigger` 由火球、爆燃这类效果在第一次命中或到期时调用一次。领域绑定 `RULE_DOMAIN_BIND` 把后面的攻击冻成 `BoundReleasePlan`，按领域轮次发射，不是父攻击接触点上的子分支。没有父子段、没有「实体命中和撞地共享一次机会」、没有接触点坐标交给另一条分支、也没有中心名单排除。附魔现在挂在所属段的下标上，没有父段附魔与子段附魔隔离的单独接口。

### 种火 `fire_seed_sow`（第 10 行）

已填：通用、火、基础冷却 5 tick、附魔是、规则、触发、必须手持剑否。资源行：火种、增加、命中目标、命中成功、数值 10、每目标每次施法。`N` 写基础 10，乘炉心产火种倍率后向下取整，上限 100，无炉心为 10；同一次真实释放对同一目标只生产一次。`DV` 里普通炉心约 11–16 点、过载约 22 点是草稿，没有写入数值格。

可沿用的计算：`FireSeedMath.UPPER_BOUND = 100`（约第 8 行），`floorProduction`（约第 56–67 行）是 `floor(基数 × 倍率)`，结果再被余额钳到 100。`FurnaceMath.seedMultiplier`（约第 211–225 行）：没有炉心档位时返回 1.0；普通档随热量从 1.12 到 1.60；过载返回 2.2。`FireSeedProduction.produce`（约第 18–26 行）会读这个倍率。无炉心得到 10，和 `N` 一致。草稿里的 11–16 和 22 不能当成已定点数。

现有火附魔不会按 10 点登记。`EnchantBonus` 约第 175–194 行：任一效果键为 `fire` 的附魔就把快照火种写成 2、燃烧秒数按持续时间缩放，不是按该附魔自己的数值格。点火仍是这套火附魔，`N` 要求不改点火的额外 2、热光束的 1、火球的 3。

### 引燃 `fire_kindle`（第 11 行）

已填：通用、火、附魔是、规则、触发、必须手持剑否。资源行：火种、消费、命中目标、燃烧结算、数值 10、周期 tick 10、每次触发。`EJ` 写满 8 条计划则在扣火种前拒绝。`N` 和 `DV` 都写单段 `2×(1+F)`、间隔 10 tick、最长 `40+10×n` 是录入草案，待定稿。冷却、魔力、复杂度、装配消耗、等级未定。

名字已经存在、且和草案里若干约束同名的类型：`FireLimits.PLAN_CAP = 8`，`STANDARD_SEGMENT_DEMAND = 10`，间隔下限 1、上限 12000（`FireLimits.java`）。`BurnLedger.duplicate` 按同一次 `castId`、目标、效果键拒绝重复。`BurnLedger.admitShared` 在计划数达到上限或重复时拒绝。`FireSeedAccount.draw` 按段从共享余额扣点，不占用该施法上下文的一次消费标记。这些是现有共享账的名字和常量，不是本行草案已经接入，也不表示应把草案公式写进旧火系效果。旧火系六式本单不读、不改。

### 引爆 `fire_detonate`（第 12 行）

已填：通用、火、附魔是、规则、触发、必须手持剑否。资源行：火种、消费、命中目标、命中成功、数值「全部」、每目标每次施法。`N` 写 `0.2×S×(1+F)`，`DV` 标明这是草稿，不是燎里。费用和魔力等未定。

`FireSeedMath.invested`：请求为 `Integer.MAX_VALUE` 时表示当时全部余额。`FireSeedAccount.consume` 在同一 `FireCastContext`、同一目标上只消费一次，并 `markConsumed`。之后同一上下文的 `grant` 看到 `hasConsumed` 就返回 0。没有 `0.2×S×(1+F)` 这个伤害公式。`FireSeedGrant.deposit` 和 `FireSeedProduction.produce` 每次新建上下文，不会自动进入调用方那本消费账。

### 幽冥 `fire_nether_gloom`（第 13 行）

已填：通用、火、附魔是、规则、持续、必须手持剑否。资源行：火种、增加、领域内敌对活物、领域周期、每次触发。数值和周期空。`DV` 写曾建议每 20 tick 基础加 5、重叠取强，尚未定稿。不减速，不自建领域。

领域本体由载体的 `DomainDefinition` 打开。`DomainService` 放置时 `domain.bindContact(snapshot)`（约第 141 行）。`ContactEnchantState` 约第 42 行要求 `snapshot.contactDamage > 0` 才采样，周期是每 2 tick，不是表上的「领域周期」。接触采样打出的火种仍走 `FireSeedGrant.deposit`，点数来自快照；火附魔快照固定 2。原生领域脉冲不执行外接附魔。没有「领域内火系表现改成蓝火」的开关；源码里没有灵魂火或蓝火重着色入口。`DI2` 批注的领域减速 -1 是领域列自己的减速等级，幽冥没有填领域列，不能拿来当已关闭减速的登记。

### 崩地 `earth_break_ground`（第 14 行）

已填：通用、土、附魔是、规则、触发、必须手持剑否。链上角色空。坑形、半径、深度、和体积增幅的关系未定。`N` 写统一地形破坏开关首版默认开启，关闭后停止破坏已有方块，但不关闭陨岩残留。

源码里没有这个开关，没有术式破坏方块的公共入口，也没有「有效撞地后挖一次坑」的附魔效果。土附魔 `sinking_edge` 走通用附魔快照，效果键 `earth`，当前折叠结果是击退一类的固定附魔，不是挖坑。`MagicCreateWaterEffect` 会 `setBlock` 放水，那是生成水自己的效果，不是地形开关。

### 火苗 `fire_spark`（第 15 行）

已填：法术、火、基础伤害 1、基础冷却 5 tick、附魔否、可接受附魔是、命中体、触发、弹道直线、链上角色攻击、移动中的命中体是、接受持续索敌是、接受出手初瞄是、出手后不再跟随否、必须手持剑否、接受弹道改写是、弹道可被修改是。资源行：火种增加、命中目标、命中成功、数值 2、每目标每次施法。

类别开关与火球一致，见上面陨岩一节的 `fire_orb` 登记。`dedicatedInitialAim`（`ReleaseDefinition` 约第 47–48 行）要同时满足接受初瞄、不接受持续索敌、弹道不可改；火球和本行这三列的填法都使它为否，与「出手后不再跟随 = 否」一致。

`DV` 写明飞行速度、存活 tick、碰撞尺寸没有照搬火球。因此火球的寿命 40、速度 1.2、厚度 0.4、盒子 0.45、燃烧 4 秒、产种 3 都不能填进火苗。原版燃烧时长未定；建议 20 tick 没有写入数值格。

本体产种 2 与种火基础 10 的合并：`TechniqueHitSpec.spawnReported` 约第 176 和 205–206 行把 `spec.seedPoints` 与 `frame.seedPoints` 相加，但前提是没有 `suppressRootSettle`。`frame.seedPoints` 来自 `EnchantBonus`，火附魔固定写成 2，不是种火数值格里的 10。`ReleaseBatch.Tally.rememberSeed`（约第 338–347 行）对同一目标、同一轮 `putIfAbsent`，`retireSeeds`（约第 349–373 行）再取较大值，然后 `FireSeedGrant.deposit` 一次。没有「基数 2 与基数 10 各自乘炉心倍率再合成一次入账」的入口。两次分开调用 `produce` 会各建一本上下文，去重互不相见，变成两次入账；共用同一本 `FireCastContext` 时，第二次 `grant` 因 `hasProduced` 返回 0。`N` 写的是设计预期，现有入口对不上这个预期。

## 2. 点名接口：有的写出名字，没有的只记没有

| 需求 | 现状 |
| --- | --- |
| 同一次释放标识 | 有。`ReleaseBatch.fireContext()`（约第 55–60 行）懒创建一本 `FireCastContext`，`castId` 为随机 UUID。注释写明复制份共用这个对象。`CastExecutionSession.beginGroup` 每组 `ReleaseBatch.open` 一次，延迟成员重新 `bind` 同一本账。 |
| 副本组 | 有多重成员组：`CastExecutionSession.Group` 持有这本 `ReleaseBatch`，成员序号和 `plan.gapTicks`。复制份数在同一成员序号上，不是独立的错时副本组。 |
| 延迟副本 | 只有多重成员延迟：`MulticastExpansion.gapTicks` = 序号 × 3 tick，由 `ChainScheduler.schedule` 执行。没有按 0/10/20 错开同一成员复制份的接口。`ChainScheduler` 本身能按 tick 排队，但没有错时角色去调用它。 |
| 父子段 | 没有命中接续那种父子段。领域子计划是 `BoundReleasePlan`，由 `RULE_DOMAIN_BIND` 冻住后续攻击。`ReleaseBatch.rememberExplicitHost` 按通道和槽位记住宿主配置，供同一槽的复制份读取。 |
| 事件接触点 | 只有 `HitListener.onHit` / `onExpire`，以及 `burstOnImpact` 的方块裁剪点。没有「首次合法实体或首次地形、二者共享一次、再从该点放子攻击」。 |
| 命中名单共享 | 没有跨两次攻击传递排除名单。`ReleaseBatch.Tally` 只在自己的账里按目标、轮次、复制序号去重。`TechniqueHitEntity.hitOnce`（约第 167 行）是这一颗命中体自己的目标集合；`hitInterval <= 0` 时每个目标只进一次（`TechniqueHitSettle` 约第 42–43 行）。 |
| 附魔产种合并 | 没有按附魔数值格合并。火附魔快照固定 2 点（`EnchantBonus` 约第 193 行）。规格点数与帧点数相加后，账本同一目标同一轮保留先写入的较大值，再 `FireSeedGrant.deposit` 一次。`deposit` 和 `produce` 都新建上下文。 |
| 每目标去重 | 有，但是分账。`FireCastContext.producedTargets` / `consumedTargets` 只约束拿到同一本上下文的 `FireSeedAccount.grant` / `consume`。`BurnLedger.duplicate(castId, targetId, effectKey)` 约束燃烧挂层。命中体另有 `hitOnce` 和账本轮次。火径片段、父子段、本体产种与附魔产种之间没有统一去重。 |
| 领域附魔 | 接触附魔有：`DomainInstance.bindContact` → `ContactEnchantState`，要求接触伤害大于 0，每 2 tick 采样。领域周期产种、不带接触伤害、不减速、只改蓝火表现：没有。 |
| 轨迹采样 | 有。`TrajectorySampler.sample`，直线、垂落、螺旋、跟随、函数都走它。它返回当前位置和方向，不生成沿途残留判定。 |

火种余额是实体上 0–100 的整数，`FireSeedState.grant` 入账。燃烧按段扣余额用 `FireSeedAccount.draw`，不占引爆那一次 `consume`。同一次上下文里既生产又消费时，先消费会使后续生产直接返回 0。这是现有账本规则，不是引燃或引爆的已定稿公式。

## 3. 共享文件与可拆开的作者边界

下面这些文件是现有接缝。后续真要改时，每个文件只能一个作者。这里只列边界，不指定谁先改、也不选方案。

注册与角色，必须共享：

- `src/main/java/com/bslx/XueXiMod/shu/technique/release/ChainRole.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/Trajectory.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseDefinitions.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseDefinition.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseRegistry.java`
- `src/main/java/com/bslx/XueXiMod/shu/ModTechniques.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/EnchantmentDefinitions.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/enchant/EnchantBonus.java`
- `src/main/java/com/bslx/XueXiMod/shu/Technique.java`

释放、延迟、命中，必须共享：

- `src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseBatch.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/CastFrame.java`
- `src/main/java/com/bslx/XueXiMod/network/CastExecutionSession.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/multicast/MulticastExpansion.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/chain/TechniqueChain.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitSpec.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitEntity.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitSettle.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/TrajectorySampler.java`
- `src/main/java/com/bslx/XueXiMod/shu/compute/JavaReference.java`

火种、燃烧账、领域、方块，必须共享：

- `src/main/java/com/bslx/XueXiMod/status/fire/seed/FireCastContext.java`
- `src/main/java/com/bslx/XueXiMod/status/fire/seed/FireSeedAccount.java`
- `src/main/java/com/bslx/XueXiMod/status/fire/seed/FireSeedState.java`
- `src/main/java/com/bslx/XueXiMod/status/fire/seed/FireSeedGrant.java`
- `src/main/java/com/bslx/XueXiMod/status/fire/seed/FireSeedProduction.java`
- `src/main/java/com/bslx/XueXiMod/status/fire/seed/FireSeedMath.java`
- `src/main/java/com/bslx/XueXiMod/status/fire/furnace/FurnaceMath.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/FireLimits.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/BurnLedger.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/FireConsumption.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainService.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainInstance.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/contact/ContactEnchantState.java`
- `src/main/java/com/bslx/XueXiMod/block/ModBlocks.java`

这 13 个编号都还没有自己的效果类。若以后新增类，可以按编号拆成互不重叠的文件，例如每个攻击或规则一个效果类。它们都依赖上面还没有的公共能力，不能靠各自新文件绕开共享注册器。

预计要有、但本单不选定形态的公共能力：

- 链上角色或等价登记：地表定位、喷涌、错时、命中接续。附魔行允许继续没有 `ChainRole`。
- 喷涌这条完整运动，且和垂落、风暴互斥，不叠成多套运动。
- 对已有副本施加延迟，不增加份数，并冻结出手时的目标与锚点。
- 父攻击一次接触机会、接触点、子分支、父子附魔各自留在自己的分支、中心命中名单可交给子段排除、父子仍共用同一次施法上下文。
- 沿移动载体留下短时火径，和载体共享命中计时，重叠片段不按片段数乘伤。
- 内空外实的一次环形命中。
- 受重力、能撞实体和地形的岩体，以及可存档可采掘的陨石方块；方块残留不跟着地形破坏开关一起关。
- 术式地形破坏开关，以及撞地后挖一次坑；坑的形状和半径仍未定，所以接口只能先留参数位置。
- 同一次上下文里把本体产种基数和附魔产种基数合成一次入账，并各自乘炉心倍率后取整、再受 100 上限约束。
- 领域周期内给敌对目标加火种，不要求接触伤害，不自建领域，不减速；蓝火只作为表现，不改伤害归属。
- 引燃的分段消费和引爆的一次消费若要进现有火种账，需要能表达「同一次释放、同一目标、同一效果不重复」以及「消费与生产如何共享或隔离上下文」。是否复用 `BurnLedger` 由设计裁决，本采集不选。

## 4. 阻塞参数、可沿用的明确默认、要用户确认的清单

可沿用、且表里已经写明或代码默认与表内句子一致的：

- 火苗、陨岩、热波已经填上的伤害、冷却、释放种类、标签、链上角色。火苗和陨岩已经填上的索敌与弹道改写开关。火苗的弹道「直线」。
- 火种上限 100，生产取 `floor(基数 × 炉心倍率)`。无炉心倍率 1.0。普通档 1.12 到 1.60，过载 2.2。这是倍率函数，不是种火 `DV` 里的 11–16 / 22 点草稿。
- 一分为三的份数是 3。双重释放向后读 2 格，实际成员间隔 3 tick。这只服务「数量」和现有多重，不是错时的 0/10/20。
- 垂落把原点抬高 8 再按 `0.5 * 0.08 * age²` 下落。只适用于已经登记的垂落，不适用于喷涌。
- 命中体默认无物理、无重力。贯穿且间隔不大于 0 时，每个目标标识只进一次。
- 通用、动作在解锁检查里始终可用。这不填补空着的复杂度。

表里明确待定、不能直接复用的：

- 十三行的魔力、复杂度、装配消耗、等级上限、等级成长都空。不能写成 0，也不能写成表头批注或 `Technique` 构造里常见的 20。
- 留焰：火径寿命、宽度、采样间距、片段上限、费用、冷却、伤害映射、命中策略。
- 陨岩：抛射速度、弹道、方块硬度、采掘工具、掉落、用途、空位搜索边界、连续多组的费用。
- 热波：击退。
- 喷涌：上抛速度、锥角、飞行距离。
- 错时：0/10/20 只是三份示例；后续波次费用。
- 种火：炉心换算成 11–16 / 22 的草稿点数。
- 引燃：单段 `2×(1+F)`、间隔、最长时长。周期格虽然写了 10，`DV` 仍称草案。
- 引爆：`0.2×S×(1+F)` 与费用。`DV` 称草稿。
- 幽冥：产种量、周期、重叠策略。每 20 tick 加 5 只是曾建议。
- 崩地：坑形、半径、深度、与体积增幅的关系。
- 火苗：原版燃烧时长（建议 20 tick 未确认）、飞行速度、射程、寿命、碰撞尺寸。

请主会话向用户集中确认的只有这些未定参数。链上角色和弹道的空格是枚举缺失，不是请用户从现有名单里挑一个替代。本体 2 加种火 10 已写在火苗 `N` 里，缺的是公共入账入口，不用再问用户要不要加。

## 5. 本单没有碰的范围

不读、不改旧火系六式的效果实现。`fire_liaoyuan` 在制作中，另一聊天刚收束编译。`fire_heat_up`、`fire_burn` 在制作中，另一聊天在重画素材。`fire_scorch_field` 在待验收。本单不领取、不改表、不改这些编号的源码。

没有本轮正式校验结论。没有一项被标成可验收或已实现。

源码未改。本目录以外没有写入。采集到此停止。
