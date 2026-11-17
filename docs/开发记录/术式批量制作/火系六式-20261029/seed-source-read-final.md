我先读这份火种来源任务说明，再只按它写明的范围做。这次只读现有接线：点火、热光束、火球怎么命中，以及火种挂载接口现在接到哪里。现有接线已经核对完。正在把只读结论写入本批允许的两份记录。本单只采集事实。未改源码，未做方案裁决，未做实现复审。未跑 Gradle，未跑名称门禁，未启动游戏。设计 2 次、实现 2 次保持原计数；历史测试 1 次不删。本次不追加复审。

用户要的点数记在这里，供以后落地时对照：点火附在术式上，或记到武器上下一次普通攻击，额外挂 2 点；热光束每次命中叠加 1 点；火球命中提供 3 点。本单不实现。

## 三条术式现在怎么进世界

| 术式 | id | 登记 | 释放定义 | 命中形态 |
| --- | --- | --- | --- | --- |
| 点火 | `magic_ignite` | `ModTechniques.java` 87–90、227、269 | 不在 `ReleaseDefinitions` | 附魔。链上不生成命中体积 |
| 热光束 | `heat_beam` | 81–84、230、272；`ReleaseDefinitions.java` 15 | `HIT_VOLUME` / `SUSTAINED` / `FOLLOW_LOOK`，寿命 40，速度 0，可改弹道 | 持续体积，间隔 10 tick |
| 火球 | `fire_orb` | 167–170、247、285；`ReleaseDefinitions.java` 21 | `HIT_VOLUME` / `TRIGGER` / `STRAIGHT`，寿命 40，速度 1.2，可改弹道 | 一颗直线弹，每个目标只收一次 |

三者 `fieldRadius` 走 `ReleaseDefinition.java` 169–170 的默认 0，不是场地。`ReleaseDefinitions` 的短 `define` 把 `persistentFlow` 传成 false，所以热光束和火球不走持续流载荷。

`SkillSegmentRunner.runSegment`（363–585 行）对已登记效果调用 `effect.execute`。热光束和火球各自在效果里 `TechniqueHitSpec.spawn`。生成时若当前有 `CastFrame` 且没有 `suppressRootSettle`，`TechniqueHitSpec.spawnReported`（155–165 行）把本帧伤害、附魔、账本和燃烧秒数抄进实体。公共链按 `ChainRole` 结算，`SkillUsePacket` 里没有这三条 id 的分支。

## 点火的两条活路径

`MagicIgniteEffect.execute`（16–27 行）在视线方向 5 格、膨胀 0.5 的盒子里取第一个存活且不是施法者的生物，`setSecondsOnFire(5)`。它不造成伤害，不调用火种。当前施法循环见到 `action.isEnchantment()` 就 `SkillChainPlan.imprintEnchant` 然后跳过该段（`SkillUsePacket.java` 387–396 行）。纯附魔链走 `imprintEnchantChain`（1100–1115 行）。因此这条视线盒子现在不被释放链调用。效果类仍在 `ModTechniques.java` 269 行登记。

活路径有两条：

1. 点火排在攻击术式前面时，`TechniqueChain.split` 把它收进下一段的附魔序号。`SkillChainPlan.firePaidPlan`（181–186 行）用这些序号做 `EnchantBonus.resolve`。`EnchantBonus.java` 92–104、166–179 行对效果 id `fire` 给出燃烧秒数 `max(1, round(5 × 持续时间倍率))`。`EnchantmentDefinitions.java` 10、31 行把 `magic_ignite` 登记成效果 `fire`，持续时间基数 5。秒数经 `CastFrame.burnSeconds` 抄到命中实体，在 `TechniqueHitSettle.settle`（84–90 行）里、伤害结算之前 `setSecondsOnFire`。
2. 链上只剩附魔、没有攻击宿主时，`WeaponEnchant.imprint`（31–48 行）把同一份 `burnSeconds` 写进主手 `xueximod_weapon_enchant`。下一次普通攻击：无潮印时 `onAttack`（105–142 行）立刻额外 `hurt`、`setSecondsOnFire`，并马上删掉印记，不看这次挥击是否打中。有潮印时 `SwingTide.finish`（183–208 行）等潮印范围收下后才 `setSecondsOnFire` 并删印记。

两条路径都只点原版火，不生产火种。

## 火种挂载 API

`FireSeedState.grant`（53–61 行）是服务端生产入口。`src/main/java` 里没有 `FireSeedState.grant(` 调用。余额是目标实体持久数据 `xueximod_attribute_states` 下的整数键 `fire_seed`（`AttributeStateData.java` 9–27 行，标识 `xueximod:fire_seed`）。上限 `FireSeedMath.UPPER_BOUND` 为 100。没有衰减方法；时长哨兵 `UNLIMITED_DURATION = -1`，不注册药水。死亡和死亡克隆由 `FireSeedLifecycle` 清零；存活克隆只抄这个整数。

`FireSeedAccount.grant`（21–34 行）：同一 `FireCastContext` 对象、同一目标 UUID，已经生产过或已经消费过就返回 0。`floorProduction` 向下取整，倍率 1 时 1、2、3 原样通过。取整结果大于 0 才占生产机会；余额已经是 100 时增加量可以是 0，机会仍然占上。客户端、非主线程、已死亡的 `grant` 返回 0（`actionable`，94–103 行）。

归属：余额在目标身上，不按施法者拆账。上下文只保存施法者 UUID，消费快照会带上它。`ReleaseBatch.fireContext()`（40–46 行）为同一次真实释放懒创建一个上下文，注释写明复制份共用这个对象。同标识的另一个新对象不去重（`FireCastContext.java` 7–11 行；`FireSeedCheck.java` 142–151 行）。

## 一次有效命中现在判在哪

服务器 `TechniqueHitEntity.tick`（267–323 行）在客户端直接返回。体积命中顺序是：

1. `TechniqueHitSettle.canHit`（24–31 行）：必须存活、非旁观、不是施法者 UUID。不看队伍，不调用 `DamagePermission`。
2. `tryHit`（34–46 行）：`hitInterval <= 0` 时每个目标 UUID 只进一次 `hitOnce`；大于 0 时距上次不足间隔就返回。热光束效果把间隔写成 10。火球不写间隔，默认 0。
3. 非持续流先 `HitPayloads.apply`。`burstOnImpact` 为真则回调后丢弃实体并返回。两条效果都没打开这个开关，所以命中生物不销毁弹体，tick 里的方块裁剪也不走（325–334 行）。火球可以穿过多个生物，每个生物每颗弹一次；它不是爆炸范围。
4. `settle`（79–139 行）先 `setSecondsOnFire`，再在有账本时 `Tally.record`，没有账本时立刻 `TechniqueHits.hurtRoot`。
5. 非爆炸路径在 `settle` 之后调用 `listener.onHit`。热光束的监听只负责第一次触发点。火球的监听再 `setSecondsOnFire(4)`。这一步不看稍后的 `hurt` 结果。

`ReleaseBatch.Tally` 在份数大于 1 时打开（`SkillChainPlan.java` 141 行把一分为三乘进 `copies`，221 与 275 行共享同一本账）。`FlowRoundLedger.accept`（33–43 行）对同一目标、同一轮、同一复制序号只收一次。`aggregatedDamage` 用最后一份伤害乘上收下的份数。`flush` 延迟 `FLUSH_DELAY = 6`（`FlowRoundLedger.java` 17 行）。flush 时目标已死就跳过。持续流要等 `hurtConfirmed` 为真才 `apply`；热光束和火球不是持续流，所以走 `hurtRoot`（232 行）。`hurtRoot` 清掉无敌时间再 `playerAttack`，返回值是这次 `hurt` 是否落下。`hurt` 在伤害不大于 0 或目标、施法者为空时直接 false（`TechniqueHits.java` 39–42 行）。

因此现在有三个不同的“打中了”：

- 间隔或一次门收下：几何接触，点燃和 `onHit` 发生在这里，伤害可能还没打。
- `hurtRoot` / `hurtConfirmed` 返回真：这一下玩家伤害被接受。
- 账本 flush：同一轮多份复制合成一次伤害。热光束间隔 10 大于 flush 延迟 6，相邻两轮不会并进同一次 flush；同一 tick 的复制份会并成一次。

`DamagePermission.allows`（`firecast/DamagePermission.java` 12–26 行）另有一套：存活、非旁观、未移除、非自己、非无敌，玩家还要 `canHarmPlayer`。命中体积不用它。六式地火和蒸汽用各自的过滤，本单不重审那些逻辑。

友方：`canHit` 不排除。点燃和 `onHit` 仍会发生；原版 `hurt` 可能拒绝玩家对玩家。自己：`canHit` 用施法者 UUID 排除。尸体：`canHit` 和 `grant` 都要求 `isAlive`；死亡清火种。客户端：命中 tick 和 `grant` 都不在客户端改账。

## 2 点与 1、3 点怎样才不会被加第二次

现有事件里没有火种生产。下面是接线事实，不是落点裁决。

- 原版燃烧 tick 是点燃之后另一条伤害。`setSecondsOnFire` 本身不加火种。若以后把生产挂在任意 `LivingHurtEvent` 上，这些 tick 和附魔那一刀额外 `hurt` 都会再进一次。
- `Tally.flush` 把多份伤害乘进一次 `hurtRoot`。生产若写在这次 flush 的每个目标上，份数不会再乘火种。生产若写在每份实体的 `tryHit` 或 `onHit` 里，一分为三、双重释放的每一份都会各走一次。
- 火球的 `onHit` 与 `settle` 的附魔点燃是前后两步。3 点若写在监听里，附魔额外 2 点若写在 `settle` 里，同一次接触可以分成两笔；两处都写 3，或两处都写 2，就会双计。
- 热光束每次过 10 tick 间隔都会再进 `tryHit`，并通常再 flush 一次伤害。这就是现成的“每次命中”。同一 `ReleaseBatch.fireContext()` 对同一目标只允许生产一次，第二次 `grant` 返回 0。复用这本账，热光束后续命中加不上；每次命中新建上下文，则同一次释放里的去重和“已消费后不再生产”不再跨命中。
- 点火的额外 2 点对应附魔燃烧秒数和武器印记，不对应未被调用的 `MagicIgniteEffect`。宿主自己的 1 点或 3 点若与这 2 点写在同一步且不加区分，会合成一笔或重复一笔。武器无潮印路径在挥击事件里就消耗印记，和命中体积不是同一个事件。

## 还没选的设计点

本单不选。

- 额外 2 点是加在宿主当次的 1 或 3 之上，还是宿主已经会挂火种时不再另加。
- 未被链调用的视线盒子要不要也挂 2 点。
- 热光束的“每次”算间隔门，还是算 flush 后 `hurt` 为真。失败伤害、友方、无敌是否给火种，跟着这个选择走。
- 火球的 3 点是每个被 `hitOnce` 收下的生物一份，还是整颗弹只给第一个。当前弹体穿过生物，也不因方块停下。
- 生产用哪一本 `FireCastContext`。同批复制和热光束后续命中共用 `ReleaseBatch.fireContext()` 时，第二次会被拒绝。
- `productionMultiplier` 是否保持 1。非 1 会经 `floorProduction` 改变点数。上限、冷却、衰减、伤害、附魔次数和燃烧秒数、按目标记账的归属，这次都不改。

## 可复用的公共边界与建议归属

可复用的是：`FireSeedState.grant`、`ReleaseBatch.fireContext()`、`TechniqueHitSettle.tryHit` 的间隔门、`ReleaseBatch.Tally.flush` 的一次伤害、`EnchantBonus.burnSeconds`、`WeaponEnchant` 的印记消耗。不要新做一套伤害，也不要在 `SkillUsePacket` 按这三条 id 分支。

以后落地时的建议归属，本次不写代码：

- 公共接触与账本：`TechniqueHitSettle`、`ReleaseBatch.Tally`、`FireSeedState` 由同一作者收口。挂载量从 `CastFrame`、附魔结算或规格字段带进去。
- 武器上的额外 2 点：`WeaponEnchant.onAttack` 与 `SwingTide.applyStored`。与上面同一作者，或等那一组写完再交接。两处同时写 2 点会双计。
- `HeatBeamEffect`、`FireOrbEffect`、`MagicIgniteEffect` 继续只负责生成或现有点燃。不要在监听和 `settle` 各写一遍点数。
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/fire` 保持不出现这三条 id。

## 旧检查与验证入口

这些检查仍对着未改的源码。下面的断言会在对应改动落地后不再描述新行为：

- `FireSeedCheck.production`：同一上下文、同一目标，第二次正生产必须是 0（约 109–110 行）；已消费后 `grant` 必须是 0（约 178–179 行）。为了让热光束连击而放宽去重，这两条就不再成立。
- `FireReleaseStructureCheck.runtime`（32–48 行）禁止 `release/fire` 包出现 `heat_beam`、`magic_ignite`、`fire_orb`、`setSecondsOnFire`。挂载写进那个包并带上这些记号时，这条失败。写在效果类或 `TechniqueHitSettle` 不碰到它。

可复用入口，本次不执行：

- `src/test/java/com/bslx/XueXiMod/status/fire/seed/FireSeedCheck.java` 的 `main`
- `src/test/java/com/bslx/XueXiMod/shu/technique/release/fire/FireReleaseStructureCheck.java` 的 `main`

## 写出的记录

- `docs/开发记录/术式批量制作/火系六式-20261029/seed-source-read-report.md`
  SHA256 `8872FE33EA35B2AB05422DF56FC33D57E142D4326AC137966E16DFB763D2DA98`
  2026-10-29 12:36:05.6550458
- `docs/开发记录/术式批量制作/火系六式-20261029/seed-source-read-evidence.json`
  SHA256 `64B3B8B93C25CF0C83B9C9B3545ED2AEB0D39A78E0FBBFB20EC3D03CF83C50DD`
  2026-10-29 12:36:23.8915691

所读源码的 SHA256 与 mtime 在报告的「源码哈希」表和 evidence 的 `sources` 里。这些文件本次没有改。读期间这些路径没有被本会话改写。未列入的其他文件是否仍有人在写，不猜测。

未追加正式复审。本报告写完后停止写入。
