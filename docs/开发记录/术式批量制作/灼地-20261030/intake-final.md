我先读这份任务说明，按它写明的授权范围执行。任务是只读定位「灼地」，不改代码、不写表。我先读流程文档，并提取制作表现行。流程已读过。接下来核对现行制作表，并只在源码里定位灼地会用到的现有接口。灼地在待制作第 4 行。我接着把空列、资源明细和源码里可复用的领域、火种接口对上。灼地仍在待制作，编号 `fire_scorch_field`，没有领取，也没有代码。这次只读定位，不改表、不改源码、不编译，也不计设计或实现校验轮次。

## 表位置与唯一性

现行 `docs/术式制作表.xlsx`：SHA256 `83D13762D627FF953A184F5B7F6247E88A972ED9FA139099D6FFA5A12DFEFB9E`，172191 字节，最后写入 2026-10-30 18:05:12（本地）。

四张状态页：已完成 40、待制作 14、制作中 3、待验收 3。第 3 行起非空 `B` 共 60 个，四页内没有重复，也没有「`B` 空但同行有内容」的行。

灼地只在待制作第 4 行。资源状态明细只有第 7 行、配置序号 2。待制作原始数据里没有这个编号，说明还没有首次领取留档。

同页另有三行在正文里提到灼地，编号都不是它：第 3 行留焰 `fire_flame_trail`、第 7 行地表定位 `rule_surface_anchor`、第 14 行幽冥 `fire_nether_gloom`。

## 输入原文

待制作第 4 行非空列如下。空列不逐格抄；费用、领域、弹道和触发时机的空列写在后面的未定项里。

| 列 | 表头 | 原文 |
|---|---|---|
| A | 名称 | 灼地 |
| B | 编号 | `fire_scorch_field` |
| C | 类型 | 法术 |
| D | 属性 | 火 |
| E | 基础伤害 | 1 |
| F | 基础冷却tick | 30 |
| J | 附魔 | 否 |
| M | 可接受附魔 | 是 |
| P | 释放种类 | 命中体 |
| Q | 释放标签 | 持续 |
| S | 链上角色 | 攻击 |
| AB | 必须手持剑 | 否 |
| AH | 接受弹道改写 | 否 |
| AK | 弹道可被修改 | 否 |
| AS | 场地半径 | 2 |
| BW | 场地寿命tick | 100 |
| DW | 配置启用 | 是 |
| DX | 资源 | 火种 |
| DZ | 操作 | 增加 |
| EA | 作用对象 | 领域内敌对活物 |
| EC | 数值 | 20 |
| EE | 寿命策略 | 不适用 |
| EI | 计量单位 | 每目标每次施法 |
| EJ | 生效条件 | 同一施法者、同一次真实释放、同一合法敌对目标只记首次接触一次。反复进出不再给 20。只有低矮体积内的接触才算，不打整根竖直柱。 |
| EK | 结算约束 | 基础 20，再按现有炉心产种倍率与上限 100 沿用底座取整，直接加入唯一共享余额。同时有 1 点火伤，但产种不依赖伤害命中，不因此触发种火、引燃或引爆。 |
| EL | 迁移来源 | N/DV |
| EM | 关联说明 | 首条：首次接触基础 +20。周期 +15 只在资源状态明细序号 2，不在本行重复。 |

`BW2` 表头批注：「没有单独改场地时，水系释放定义默认 80，其它释放定义默认 200。」本行已经写成 100。该行单元格没有自己的批注。待制作页其余批注都在表头，正文不提灼地。

**N 功能描述**

灼地是地面产种术式。在合法地表形成固定火场，敌人停留在地表火场中积累火种，供玩家之后选择引燃或引爆。原有持续 2 点伤害、每 20 tick 一次、120 tick 寿命不再适用。

基础半径 2 格，持续 100 tick（5 秒）。只有接触火场实际低矮判定体积的合法敌对目标受影响，不自动覆盖整根竖直柱。单独使用时默认脚前合法地表；装配地表定位后可投到远处。火场位置固定，不被弹道改成飞行火场。保留地面定位、合法表面、固定区域，并继续作为领域载体。不放置原版火或岩浆，不蔓延、不破坏地形、不烧掉落物。火场到期清理。留在目标身上的其他独立燃烧按各自生命周期运行。已经挂上的火种遵循底座正常规则，不因离场清除。

同一次灼地对同一合法敌对目标的首次接触：造成 1 点火伤，并增加基础 20 点火种。这份首次接触奖励在同一次灼地、同一目标只给一次，反复进出不能再刷 20 点。每次真实重新释放，其首次接触奖励仍可正常生效。

后续持续接触每 20 tick 增加基础 15 点火种，不再造成直接伤害。后续周期产种是独立资源效果，不依赖造成伤害，不制造伤害命中，不因此重复触发种火、引燃或引爆。重新进入不重置产种计时。离开火场停止获得火种，离场错过的周期不追补。周期计时不能被进出或重叠火场切换绕过。

目标在火场生成时即接触且一直停留：第 0 tick 首次 20，第 20、40、60、80 tick 各 15，第 100 tick 结束不额外结算。没有炉心增幅、也没有其他生产和消费时，总计 80 点火种、1 点直接伤害；停留到第 40 tick 时已有 50 点火种。80 与 50 是该条件下的资源产量，不是任何环境下的剩余余额保证。

同一施法者的多个灼地重叠时，对同一目标的持续产种只取一份，不按火场数量倍增。

正在燃烧的目标也可以获得火种，直接加入唯一共享余额。现有存活燃烧层逐段消耗，多层加快消耗。不复制私有燃料，不刷新燃烧层已有的启动倍率或时长，不设燃烧时禁止补种或禁止引爆的锁。

产种量接受现有炉心产种倍率和火种上限 100，沿用底座正常取整规则。

与幽冥或其他不同来源的产种如何叠加，此次未定。本次已确定的重叠限制只针对同一施法者的多个灼地。

灵魂火地带组合仍是地表定位、幽冥、灼地，不另建组合行。灼地作为领域载体。幽冥把火场表现为蓝火；蓝火不改变伤害归属或免疫，不给减速，不放置灵魂沙或原版灵魂火方块。基础冷却 30 tick。费用、魔力、复杂度、装配消耗、等级上限和等级成长仍未定。

**O 特效**

贴地火舌。每次产种时，火星从地面向目标身上汇聚，表现积火。不采用持续伤害命中的闪烁或受击反馈来假装周期产种在扣血。装上幽冥后，火场表现为蓝色魂火。

**DV 备注**

魂焰的 40 tick、20% 减速已被用户否决，不写入本行。火场半径、寿命和伤害间隔是来源稿的基础参数。本次录入 2026-10-29。来源 `docs/火属性基础术式与组合设计.md`，并按用户最新决定替换魂焰、采用共享燃料。只进入待制作，尚未领取，不开始实现。基础冷却 30 tick。魔力、复杂度、装配消耗、等级上限和等级成长未定，对应数值格留空，不用 0 代替。

2026-10-30 用户确认：「嗯按照这版写入待制作表」。灼地改为地面产种。基础伤害改为 1，只在同一次灼地、同一合法敌对目标首次接触时结算一次，没有伤害周期。场地半径仍为 2。场地寿命改为 100 tick。旧的持续 2 点伤害、每 20 tick 伤害、120 tick 寿命和场地维持周期 20 不再适用；20 tick 只是后续产种周期，不留在场地维持周期。首条资源是首次接触基础增加 20 点火种，写在本行 `DW:EN`。第二条及以后是火场第 20、40、60、80 tick 各基础增加 15 点火种，写在资源状态明细配置序号 2，第 100 tick 不额外结算。触发时机列表没有能准确表达地表首次接触或停留接触周期的选项，两处触发时机留空；不填命中成功、燃烧结算、首次带资源接触或领域周期。作用对象使用现有「领域内敌对活物」，低矮体积和敌对限制写在功能描述里。费用、魔力、复杂度、装配消耗、等级上限、等级成长，以及与幽冥或其他不同来源产种的叠加细则仍未定。无炉心且无其他生产消费时，一直停留到结束为 80 点火种加 1 点直接伤害，停留到 40 tick 为 50 点火种；这是产量示例，不是剩余余额保证。本条只录入已确认需求，保持待制作，不领取。

数据表「触发时机」当前确有：成功施放、出手提交、命中成功、潮印命中成功、领域周期、首次带资源接触、范围选取、主动结束、燃烧结算。DV 明确不选用其中的命中成功、燃烧结算、首次带资源接触、领域周期。链上角色「攻击」对应代码 `ATTACK`，释放种类「命中体」对应 `HIT_VOLUME`，释放标签「持续」对应 `SUSTAINED`。

**资源状态明细第 7 行**

| 列 | 表头 | 原文 |
|---|---|---|
| A | 术式编号 | `fire_scorch_field` |
| B | 配置序号 | 2 |
| C | 配置启用 | 是 |
| D | 资源 | 火种 |
| F | 操作 | 增加 |
| G | 作用对象 | 领域内敌对活物 |
| I | 数值 | 15 |
| K | 寿命策略 | 不适用 |
| M | 周期tick | 20 |
| O | 计量单位 | 每次触发 |
| P | 生效条件 | 目标仍接触该施法者灼地的低矮体积。离开即停。重新进入不重置计时，错过的周期不追补。同一施法者多块重叠只取一份。 |
| Q | 结算约束 | 按火场自身第 20、40、60、80 tick 各基础增加 15。第 100 tick 结束不额外结算。不造成直接伤害，不制造伤害命中，不重复触发种火、引燃或引爆。不包含首次接触的 20。新的真实释放另计首次接触。炉心倍率与上限 100 沿用底座取整。 |
| R | 迁移来源 | N/DV |
| S | 关联说明 | 灼地第二条及以后的周期产种。不重复主行首次接触。 |

明细的触发时机、首次延迟、状态、寿命 tick 都空。主行的触发时机、周期 tick、首次延迟也空。

主行另外留空、且 DV 写明未定或不要用 0 代替的有：等级上限、复杂度、装配消耗、登记魔力、显式魔力、弹道、场地生成消耗、场地维持消耗、场地维持周期，以及整组领域列（宽、深、高、消耗、寿命、边界、外观、效果键）。

## 历史与轮次

`src/main` 和语言资源里都没有 `fire_scorch_field` 或「灼地」。交接索引和交接文档也没有这项。

已有记录都是入表，不是领取或实现：

- `docs/开发记录/火系基础术式入待制作-20261029`：2026-10-29 写入待制作，当时写明尚未领取。
- `docs/开发记录/灼地产种设计入表-20261030`：2026-10-30 按已确认设计改待制作行和明细第二条。任务写明保持待制作，这次录入不是设计或实现复审，不追加、不重置额度。随后批注把基础冷却写成 30 tick，仍未领取。热波、编号前缀、火苗等后续写表都声明不改灼地的领取状态。
- 火系六式批次的设计 2/2、实现 3/3 属于燎里等已有项，记录里没有把灼地算进那份额度。

因此灼地没有已记录的设计轮次，也没有已记录的实现轮次。本次只读定位继续不计轮次。不能把入表、批注或这次定位当成 1/2 或 1/3。

## 现有代码事实

注册入口在 `ModTechniques.java` 和 `ReleaseDefinitions.registerAll()`。`Technique` 构造参数是编号、名称、类型、基础伤害、冷却、等级上限、描述、复杂度、属性、弹速、是否附魔。类型「法术」对应 `TechniqueType.MAGIC`。`ReleaseDefinitions` 里没有 `fire_scorch_field`。`ChainRole` 现有 `ATTACK`、`ACTION`、`SEEK_SPACE`、`SEEK_ENTITY`、`TRAJECTORY_TRACKING`、`TRAJECTORY_DROP`、`TRAJECTORY_SCATTER`、`RULE_TRIPLE`、`RULE_DUAL`、`RULE_DOMAIN_BIND`，没有地表或产种角色。

和表内「场地」最接近的现成路径，和需求对不上：

- `SkillUsePacket` 约 757 行：只有 `fieldRadius() > 0` 且基础伤害不大于 0，才调用 `FieldSpawner.releaseFromChain`。灼地基础伤害是 1，这条分支不会接住它。
- `FieldSpawner.anchorAt` 把场地放在身体中点。类注释写明链上索敌和弹道只附加，不改这个点。`FieldZones.coversBody` 是以锚点为心的球体，不是低矮地表层。`FieldZones.pulse` 每 10 tick 处理压强、侵蚀、潮湿、减速和牵引，不产火种。
- `ReleaseDefinition.fieldLifetimeTicks()` 默认 200，`fieldUpkeepPeriod()` 默认 20。表把寿命写成 100，并写明 20 tick 不是场地维持周期。

燎原的地表火在另一套类里：

- `LiaoyuanEffect.execute` 用 `ReleaseBatch.claimSurfaceFire()`。该方法注释是「同一本账只铺一片地表火场」，第二次返回 false。
- `GroundFireFields.open` 的脉冲走燎原：每 20 tick，有火种就挂燃烧消耗，否则打固定 2 点伤害。常量半径 6、层高 1、垂直余量 3、寿命默认 200。它不放置原版火，也不产种。
- 可分开看的几何是 `GroundFireMath.insideDisc`、`GroundFireMath.inLayer` 和 `StandSurface.columnTop`。它们判断脚是否落在地表圆盘的一层里，不包含灼地的产种规则。

领域也不是现成宿主：

- `ReleaseDefinition.domain()` 默认 null。灼地的领域宽、深、高、寿命都空。
- `DomainContactBilling` 的类注释写明只给领域实例做接触预留，屏障、场地、护体和普通攻击不进这里。接触伤害来自附魔快照的 `contactDamage`。
- `DomainPulses.fire` 按领域原生策略发潮湿、减速和水势，不产火种。
- `DomainNative.hostileInside` 要求整段身体都在领域盒内，并排除主人和盟友。

接触附魔也不是停留产种：

- `ContactEnchantState.open` 在 `contactDamage <= 0` 时直接返回 null。采样是每 2 tick。打中之后才 `FireSeedGrant.deposit`。
- `ContactPolicy.mayContactAffect` 排除 `Relations.permits` 和 `Relations.ally`。

火种账可以复用余额，不能直接复用「同一施法只生产一次」：

- 资源是 `xueximod:fire_seed`，上限 `FireSeedMath.UPPER_BOUND = 100`。`FireSeedMath.floorProduction` 是 `floor(基础值 × 倍率)`，再由余额钳到 100。
- `FireSeedState.grant(target, cast, baseAmount, productionMultiplier)` 进入 `FireSeedAccount.grant`。同一 `FireCastContext` 对同一目标只 `markProduced` 一次，之后再产返回 0。
- `ReleaseBatch.fireContext()` 一次释放共用一个上下文。
- `FireSeedGrant.deposit` 每次新建上下文，倍率写死 `1.0`，并且先过 `DamagePermission.allows`。全库没有调用 `FurnaceService.seedMultiplier`。炉心公式在 `FurnaceMath.seedMultiplier`：无炉心 1.0，普通炉心约 1.12 到 1.60，超频 2.2。它现在只是查询，没有接到生产入账。

敌对判断现有三套，口径不同：`ContactPolicy.mayContactAffect` 和 `DomainNative.hostileInside` 排除盟友；`DamagePermission.allows` 不看 `Relations`，只排除自己、旁观、无敌，以及原版不允许伤害的玩家；`ActionWindow.hostileTo` 同样排除盟友。`FieldZones.pulse` 只跳过主人和旁观。

命中结算在 `TechniqueHitSettle.tryHit` / `settle`。没有周期时同一目标只打一次；有伤害落地才 `FireSeedGrant.deposit`。这和「周期产种不制造伤害命中」不是同一条结算。

视觉现成的是燎原地面片 `FireVisualClient`（贴图来自 `FireVisualLayout`，按同步的原点、半径和寿命铺在地表），以及目标身上的 `FireSeedFlameClient`（按余额档位数显示火苗）。命中体上的 `ParticleTypes.FLAME` 在 `TechniqueHitEntity` 和 `HitVolumes`。没有灼地专有贴图，也没有已确认的蓝火资源。

地表定位 `rule_surface_anchor` 仍在待制作第 7 行，链上角色和弹道都空，源码里没有这个编号。现有落点指定是 `rule_cast_anchor` / `SEEK_SPACE`。`ReleaseBatch.resolveAnchor` 取视线附近实体中心，否则退回视线前方，不是「表面上方能放下完整本体」的地表锚点。

## 最小相关文件

这些是现状定位，不是改动清单。

- 注册：`src/main/java/com/bslx/XueXiMod/shu/ModTechniques.java`，`shu/technique/release/ReleaseDefinitions.java`，`shu/technique/release/ReleaseDefinition.java`，`shu/technique/release/ChainRole.java`
- 场地：`shu/technique/release/field/FieldSpawner.java`，`FieldZones.java`；调用门在 `network/SkillUsePacket.java`
- 燎原地火：`shu/technique/effects/LiaoyuanEffect.java`，`shu/technique/release/firecast/GroundFireFields.java`，`GroundFireMath.java`，`StandSurface.java`，`ReleaseBatch.claimSurfaceFire`
- 领域与接触：`shu/technique/release/domain/DomainService.java`，`DomainContactBilling.java`，`DomainNative.java`，`DomainPulses.java`，`shu/technique/release/contact/ContactEnchantState.java`，`ContactPolicy.java`
- 火种与炉心：`status/fire/seed/FireSeedState.java`，`FireSeedAccount.java`，`FireSeedGrant.java`，`FireSeedMath.java`，`FireCastContext.java`，`status/fire/furnace/FurnaceService.java`，`FurnaceMath.java`
- 命中与敌对：`shu/technique/release/entity/TechniqueHitSettle.java`，`shu/technique/release/firecast/DamagePermission.java`，`shu/technique/ActionWindow.java`
- 画面：`client/FireVisualClient.java`，`client/FireSeedFlameClient.java`，`visual/fire/FireVisualLayout.java`
- 锚点：`shu/technique/effects/CastAnchorEffect.java`，`shu/technique/release/ReleaseBatch.java` 的 `resolveAnchor`

## 工作区占用

`git -c safe.directory=D:/alianximod` 能读到仓库。相对 HEAD，`ReleaseDefinitions.java`、`ModTechniques.java`、`SkillUsePacket.java` 已是修改；场地、接触、领域、`firecast`、`status/fire`、`FireVisualClient.java`、`FireSeedFlameClient.java` 和制作表本身是未跟踪。这些都是已有未提交内容，必须保留。

没有交接或批次把这些文件标成「正在为灼地写入」。火系六式记录里，燎里源码作者和表作者的最后状态都是停写。制作中仍有升温、燃烧、冷却，待验收仍有焚天、燎原、燎里；它们和灼地共用炉心、地表火、火种和地面画面。本次会话没有改这些文件。

## 需要 Astra 裁决的问题

以下都还没有拍板。未拍板前不能选一条路径开工。它们属于开工前的方案裁决，会计入设计额度；这次报告只列出问题，没有做那一轮裁决。

1. 宿主选哪条。表是命中体、攻击、半径 2、寿命 100，领域列全空；正文又写固定地表火场和领域载体。现有场地分支不接收正伤害，燎原地火是消费或 2 点伤害且一次释放只认一片，领域接触不接收场地。低矮停留接触和周期产种是否必须新做宿主。
2. 产种怎么入账。同一施法上下文对同一目标只能生产一次，而灼地要首次 20 再加四次 15。`deposit` 的倍率是 1.0，炉心倍率函数没有被任何生产调用。倍率读施法者还是目标，以及周期入账能否绕过「一次生产」标记。
3. 敌对用哪一套。正文写合法敌对，三套现成判断并不相同。
4. 空着的费用、魔力、复杂度、装配消耗、等级、场地生成消耗和场地维持周期，是保持不做，还是必须等用户补数。文字已禁止用 0 填这些空格。
5. 与幽冥或其他来源的产种叠加仍未定。已定的只有同一施法者多块灼地的持续产种取一份。实现是否必须把未定叠加留成未接入。
6. 单独释放的「脚前合法地表」没有距离数字。地表定位还在待制作，现有落点指定也不是地表锚点。这一项是否只做脚前，远处投掷等地表定位。
7. 低矮体积没有高度列。燎原的 1 格层高是它自己的常量。判定形状能否沿用 `GroundFireMath.inLayer`，高度是否仍未定。
8. 特效还是待制作文字，没有确认版资源名。能否只复用现有地面火焰片和火苗；蓝火是否等幽冥，而不是这次新做素材。
9. 触发时机是有意留空，并点名不使用现有的「首次带资源接触」和「领域周期」。行为按正文做，还是先改数据表枚举。本会话不能改表。
