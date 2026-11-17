# 火种双路线消费核查

日期：2026-10-29。检查者：本轮唯一的 Cursor CLI Grok 4.7 High Fast。只读生产和现有测试源码，未改它们，未改制作表和旧批次记录。

## 直接结论

持续燃烧会实际扣火种。成功走到挂载时，扣款发生一次，扣的是本次投入，不是每一段再扣一次。

立即引爆会实际扣火种。它和持续燃烧调用同一个 `FireSeedState.consume`。伤害使用的是这次扣除得到的快照；预览和实扣之间账没有被改动时，两份快照的点数、档位、余数和炉心相同。

目标燃烧期间禁止引爆，并暂停所有产种：当前代码没有这套目标级策略。已有燃烧只按效果键和施法者排他。引爆不读燃烧账。新的施法上下文仍可给该目标补火种，再把补上的火种引爆。这是新需求与当前能力的差距。本轮没有实现它。它不能解释成燎原、焚天、燎里已经漏扣，也不能据此说现有术式测试全部失效。

现行三式在纯计划、同线程、计划能挂上的路径上，没有查出漏扣、双扣或燃烧回调把旧余额写回去。扣款后若最终挂载或最终分量被拒绝，源码会留下已扣余额并返回空结果；当前三个计划函数不改余额、不改燃烧账，这条分支进不去。没有实体，这条分支没有被跑成一次真实丢种。

## 真实扣款调用链

两条路线的最后一笔都是：

1. `FireConsumption.schedule` 或 `detonate` 先做宿主、冲突或分量预检。`src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/FireConsumption.java` 23–45 行、74–88 行。
2. 通过后调用 `FireSeedState.consume`。同文件 46 行、89 行。
3. `FireSeedState.consume` 读取属性状态，交给 `FireSeedAccount.consume` 减余额并 `markConsumed`，再 `persist`。`src/main/java/com/bslx/XueXiMod/status/fire/seed/FireSeedState.java` 41–50 行、88–91 行。`FireSeedAccount.java` 41–48 行、51–62 行。
4. `persist` 在原始值和新余额不同时调用 `AttributeStateService.write`。写入落在实体持久数据 `xueximod_attribute_states` 的 `fire_seed`。`AttributeStateService.java` 25–36 行，`AttributeStateData.java` 8–33 行。成功扣除的投入至少 1，0–100 的正常余额一定会变得更小，因此会写。请求被拒绝时余额不变，正常值不会写。
5. 持续燃烧接着用这次快照 `admit` 并 `BurnCarrier.write`。`FireConsumption.java` 50–70 行。之后每段只读已封存的伤害。`BurnDispatch.java` 26–46 行，`BurnCarrier.java` 23–35 行。这里没有 `FireSeedState.consume`、`grant` 或属性写入。
6. 立即引爆接着用这次快照的分量打一次。`FireConsumption.java` 93–104 行。`strikeOnce` 的返回值不参与退款。

预览在 `FireSeedState.preview` 36–38 行，只调用 `FireSeedAccount.preview`，不减余额、不标记、不写存储。`FireSeedAccount.java` 37–38 行。

投入规则在 `FireSeedMath.invested` 74–82 行。`Integer.MAX_VALUE` 表示全部余额。其他非负请求取与余额的较小值。0 和算出来的 0 都不消费、不标记。整档请求是 `wholeTierSpend` 33–35 行，由调用者传入。燎原、焚天、燎里传的是 `FireConsumerPlans.ALL_SEEDS`，也就是全部消费。`FireConsumerPlans.java` 18 行。

## 1. 持续燃烧何时扣、扣在哪里

入口是 `FireConsumption.schedule`。真正改小目标余额的方法是 `FireSeedAccount.consume`，经 `FireSeedState.consume` 写回。

成功挂载的顺序是：目标仍在服务端主线程且存活，燃烧键不冲突，轨道数小于 8，预览能得到正投入，计划能建成并通过 `canSeal`，然后才 `consume`。扣款在挂载之前的同一次调用里完成，不是每段扣。

`BurnLedger.Track` 把投入、档位、余数和炉心抄进轨道。`BurnLedger.java` 185–188 行。`BurnIdentity` 的注释写明燃烧期间不再读目标上的新余额。`BurnIdentity.java` 5 行。

本轮夹具对稳定账本的实测：100 先预览仍是 100，随后全部消费变成 0，快照投入 100、10 档、余数 0。10 点燎原在消费后余额已是 0，再跑完 4 段、80 步，余额仍是 0，四段伤害都是挂载时封存的 2.2。100 点焚天 10 段、40 步打完，余额仍是 0，轨道结束，没有余额可写回。

## 2. 立即引爆是否同一笔消费

是。`detonate` 74–104 行调用同一个 `FireSeedState.consume`。分量先用预览快照检查；消费成功后若点数、档位、余数或炉心变了，才按实扣快照重算。伤害是 `FireLayer.combine(实扣后的分量, 实扣快照的炉心)`，然后 `strikeOnce`。

无扣款仍打资源附伤的分支，在 `detonate` 里不存在。`strikeOnce` 在 `consume` 返回可用快照之后。消费失败会在 89–91 行或更早返回，到不了 103 行。

夹具按这个顺序调用真实账本和 `FireConsumerPlans.thrust`：80 全部消费后余额 0，火伤 28.8，等于 `0.36 × 80`。100 全部消费后火伤 36.0。预览快照和实扣快照的点数一致。

燎里命中后的物理刺击是另一条伤害。`LiaoliEffect.java` 57–62 行先尝试引爆，再按术式基础伤害 `hurtRoot`。引爆失败时物理刀仍会打，那不是火种附伤。

燎原火场在附着失败时会打固定 2 点。`GroundFireFields.java` 35 行、122–129 行。这 2 点不随火种点数变化。附着成功则跳过这 2 点。它不是“没扣火种却按火种加伤”。

## 3. 0、9、10、19、99、100 以及整档和全部消费

夹具对每个起点各做一次预览和一次真实 `consume`。预览不改余额。下表是实扣后的结果。

| 起点 | 整档请求 | 整档扣掉 | 整档剩余 | 全部消费扣掉 | 全部消费剩余 | 全部消费档 / 余 |
| --- | --- | --- | --- | --- | --- | --- |
| 0 | 0 | 不扣、不标记 | 0 | 不扣、不标记 | 0 | — |
| 9 | 0 | 不扣、不标记 | 9 | 9 | 0 | 0 / 9 |
| 10 | 10 | 10 | 0 | 10 | 0 | 1 / 0 |
| 19 | 10 | 10 | 9 | 19 | 0 | 1 / 9 |
| 99 | 90 | 90 | 9 | 99 | 0 | 9 / 9 |
| 100 | 100 | 100 | 0 | 100 | 0 | 10 / 0 |

燎原、焚天、燎里都传全部消费。按当前三式，9、19、99、100 在成功提交时余额都是 0，余数不会留下。整档路径仍在账本里，三式没有调用它。

烧满 100 或引爆满 100，余额在 `consume` 返回时已经是 0，不是等燃烧段打完才变成 0。随后的脉冲没有把余额改回 100，也没有再次扣除。夹具里轨道导出的投入一直是挂载时的 100，直到轨道结束。

## 4. 扣款前拒绝，以及提交后不退款

已在真实账本上执行、并且余额保持不变的拒绝：

- 余额 0、请求 0、负数请求、非数炉心、负炉心：不扣、不标记。`FireSeedAccount.java` 51–60 行。
- 9 点整档请求为 0：不消费。
- 0 点时燎原、焚天、燎里计划都是空。`FireConsumerPlans.java` 27–31 行、49–53 行、60–63 行。
- 非法效果键建不出 `BurnSchedule`。
- 同施法者、同一燎原键冲突时，冲突判断本身不改另一份 50 点余额。第 9 条轨道 `admit` 失败，那份 50 点仍在。容量上限是 `FireLimits.PLAN_CAP = 8`。`FireLimits.java` 3 行，`BurnLedger.java` 40–45 行。
- 同一 `FireCastContext` 对同一目标第二次 `consume`：空结果，余额不变。19 点先扣 10 后剩 9，同一对象再请求全部，9 仍在。

`FireConsumption` 在调用 `consume` 之前还会拒绝：空目标、空上下文、负请求、死亡、旁观、已移除、客户端、非服务端主线程。`FireConsumption.java` 107–117 行。`FireSeedState.actionable` 94–104 行再挡一层。燃烧冲突和轨道已满在 28–31 行返回。计划为空或 `canSeal` 失败在 40–44 行返回。这些分支没有实体，本轮没有跑进 `FireConsumption` 方法本身。

提交之后：

- 设计写明扣款成功后的死亡和后续免疫不退款。`docs/火属性二级玩法设计.md` 48 行。
- `detonate` 在写入已经发生后调用 `strikeOnce`，不看它是否返回 false，也不把余额加回。`FireConsumption.java` 89–104 行。
- 死亡清火种是把剩余值写成 0。`FireSeedLifecycle.java` 21–27 行，`FireSeedState.discard` 65–69 行。它不会把已花掉的点数补回。
- 燃烧段打出时免疫，段已经从账上拿走，不补打。`BurnLedger.advance` 55–76 行。这段不读火种余额。

因此，免疫或死亡导致后面的伤害没打出来，应算已经消费。不能把 `detonate` 返回了快照理解成没扣款。

有一个条件性缺口：`schedule` 在 46 行已经 `consume` 并因此写存储，若随后快照变化导致计划重封失败，或第二次读账后 `admit` 失败，58–59 行和 64–67 行只记错误并返回空，没有退款，也没有撤销 `markConsumed`。`detonate` 在 89 行消费后，若重算分量不合格，98–100 行同样返回空且不退款。设计 44 行和 48 行要求不能挂载的计划必须不扣款。当前 `FireConsumerPlans.spread`、`sky`、`thrust` 只做纯计算。同线程上预览和实扣之间没有第三笔写入时，快照不会变，`canSeal` 通过后的 `admit` 也不会因为账本自己变了而失败。本轮没有构造会改账的计划器，也没有 `LivingEntity`，所以没有观察到一次真实的“扣了却没挂上”。

## 5. 同一上下文、复制体、迟到回调和两本新账

同一个 `FireCastContext` 对象对同一目标只消费一次。先 `schedule` 所用的全部消费，再 `detonate` 所用的全部消费：第二次为空，余额保持 0。先引爆 80，再燃烧：第二次为空，余额保持 0。标记在 `FireCastContext.consumedTargets`。`FireCastContext.java` 16 行、39–40 行、49–53 行。复制体和迟到回调必须复用这个对象，去重才成立。

只复制标识不够。两本 `new FireCastContext(同一个 castId)` 是两套集合。夹具里第一本把 19 点扣成 9，第二本把剩下的 9 扣成 0。`FireCastContext.java` 7–11 行写明了这个契约。设计也要求同一次逻辑施法共用同一个对象。

现行三式拿的是 `ReleaseBatch.fireContext()`。`ReleaseBatch.java` 45–50 行懒创建一次，70–80 行一次真实释放打开这一本。燎原把这个对象交给火场。`LiaoyuanEffect.java` 20–38 行，`GroundFireFields.java` 43–53 行、86 行。火场以后的脉冲仍用这一个对象。`GroundFireFields.java` 123–124 行。焚天和燎里在同一次 `execute` 里取出同一个对象；燎里的命中监听器捕获的是这个局部变量。`FentianEffect.java` 45–59 行，`LiaoliEffect.java` 33 行、51–58 行。因此同一次释放里的复制份和燎里的迟到命中，只要还在这本 `ReleaseBatch` 上，不能第二次消费同一目标。本轮没有启动一次一分为三的真实释放。

两个新的施法上下文读的是同一份目标余额，各自没有私有余额。`FireSeedState.consume` 每次从属性状态新建 `FireSeedAccount`。夹具按这个边界模拟存储整数：第一本账把 40 扣成 0 并写回；第二本账从 0 读起，扣不到；它再记入 12 后，第三本账读到 12 并扣成 0。第一本账不能再扣这 12。炉心只记在快照上，不改扣点。`FireSeedAccount.java` 46 行只减 `snapshot.amount()`。

## 6. 燎原、焚天、燎里是否接到存储

三式都进入上述消费入口，消费不是只算不写。

- 燎原：`LiaoyuanEffect.execute` 铺火场，不直接扣点。火场脉冲在目标站进火、且该施法者的燎原键不冲突、余额大于 0 时调用 `FireConsumption.schedule(target, 这一本 cast, ALL_SEEDS, "xueximod:fire_liaoyuan", false, spread)`。`GroundFireFields.java` 119–124 行。
- 焚天：对球内每个有火种的目标调用 `FireConsumption.schedule(..., ALL_SEEDS, "xueximod:fire_fentian", true, sky)`。`FentianEffect.java` 55–59 行。排他是目标级的焚天键。
- 燎里：被接受的命中体回调里，余额大于 0 时调用 `FireConsumption.detonate(..., ALL_SEEDS, thrust)`。`LiaoliEffect.java` 51–58 行。没有效果键，也不读 `BurnLedger`。

共享 token 在这一次释放的消费者之间是通的：三处都拿 `batch.fireContext()` 这一个对象。生产火种的入口不拿这个对象。`FireSeedGrant.deposit` 每次 `new FireCastContext(UUID.randomUUID(), 施法者)`，再 `FireSeedState.grant`。`FireSeedGrant.java` 9–22 行。所以点火、热光束、火球的入账不会占用、也不会被占用燎原那本消费者账。这是 `FireSeedGrant` 自己的注释，不是本轮猜的。

当前产种接线会写存储，调用的是 `grant`，不是消费入口：

- 带火附魔的施法，`EnchantBonus` 把 `seedPoints` 设为 2，否则 0。`EnchantBonus.java` 193 行。`SkillSegmentRunner.java` 534 行放进帧。
- 热光束把命中体 `seedPoints` 设为 1。`HeatBeamEffect.java` 36 行。
- 火球把命中体 `seedPoints` 设为 3。`FireOrbEffect.java` 43 行。
- 未抑制根结算时，实际入账点数是命中体点数再加帧上的附魔点数。`TechniqueHitSpec.java` 162 行、191–193 行。因此同一次带火附魔的火球，被接受的目标是 3+2；热光束是 1+2。没有火附魔时帧上是 0，火球就是 3，热光束就是 1。燎里把 `suppressRootSettle` 设为 true，且自己的 `seedPoints` 保持 0，不走这条加种。`LiaoliEffect.java` 41 行。
- 伤害被接受才入账。普通命中在 `TechniqueHitSettle.java` 137–139 行和 161–163 行。一分为三的汇总在 `ReleaseBatch.java` 257–265 行、290–291 行，按目标和回合只保留一份点数，接受后才 `deposit`。近战印记在 `WeaponEnchant.java` 146–147 行和 205–206 行。接触伤害在 `ContactEnchantState.java` 92 行。剑实体在 `SwordEntityMotion.java` 93–94 行。

这些 `deposit` 都受 `FireSeedState.grant` 的主线程和存活检查。燃烧轨道不在 `grant` 的条件里。

## 7. 已经燃烧时补种再引爆

当前允许。

夹具顺序是真实账本和真实燃烧账：100 全部消费并挂上燎原，余额 0，轨道投入仍是 100。推进 5 拍还没有脉冲，余额仍是 0。另一个新上下文 `grant` 30，余额变成 30。原来那本已消费的账不能再产，也不能把这 30 引爆。第三个新上下文可以全部消费这 30，余额回到 0。原轨道还在，投入仍是 100。

排斥范围要分开：

- 同一本账、同一目标：第二次消费被拒绝。这是 token 去重。
- 燎原键 `xueximod:fire_liaoyuan`，`targetExclusive` 为 false：同一施法者冲突，另一施法者不冲突。`BurnLedger.conflicts` 21–34 行。`GroundFireFields.java` 119 行用的就是这个判断。
- 焚天键 `xueximod:fire_fentian`，`targetExclusive` 为 true：目标上已有焚天时，换施法者也挂不上。不同效果键互不冲突，燎原和焚天可以同时在一个目标上。
- 燎里引爆没有效果键，`detonate` 不调用 `conflicts`。夹具里 `xueximod:fire_liaoli` 对已有燎原的账也不冲突。
- 没有“目标上只要有任意燃烧，就拒绝引爆”的判断。
- 没有“燃烧期间暂停一切产种”的判断。暂停只发生在同一本账已经消费或已经生产过该目标时。`FireSeedAccount.grant` 25–26 行。`FireSeedGrant` 每击都新建账，所以点火、热光束和火球在燃烧期间仍可入账。

## 8. 现有检查覆盖了什么

现有检查覆盖了账本整数的“原余额、消费、新余额”和失败不扣，也覆盖了效果键冲突。它们没有覆盖实体属性存储，也没有把“燃烧中的目标再引爆”当成失败。

本轮重跑，工作目录 `D:\alianximod`，`JAVA_HOME=C:\Program Files\Java\jdk-17`。定向 `javac --release 17 -encoding UTF-8` 退出 0，20:21:56–20:21:57。classpath 是当前 `build/classes/java/main`、annotations 24.0.1 和 asm 9.7.1 五件套。class 输出在本任务目录 `check-classes`，没有改测试源码。

| 入口 | 退出码 | 时间 | 实际覆盖 |
| --- | --- | --- | --- |
| `DualRouteConsumeProbe` | 0 | 20:21:57.833–20:21:58.196 | 真实 `FireSeedAccount`、`FireConsumerPlans`、`BurnLedger`。含上表、两种顺序、同标识两本账、共享整数、燃烧中补 30 再引爆、脉冲不改余额 |
| `FireSeedCheck` | 0 | 20:21:58.197–20:21:58.533 | 断言 251，失败 0。整档、全部、失败不扣、共享余额、同标识两本账 |
| `SixFireFunctionalCheck` | 0 | 20:21:58.534–20:21:58.872 | 通过。三式计划、先到先扣、燎原同施法者排他、焚天目标排他。未跑玩家、实体命中、地火 tick、一分为三 |
| `BurnLedgerCheck` | 0 | 20:21:58.874–20:21:59.211 | 断言 121，失败 0 |
| `BurnDispatchCheck` | 0 | 20:21:59.213–20:21:59.505 | 断言 16，失败 0。回调挂新轨道不会被旧账盖回。未跑实体 NBT 和真实 hurt |
| `FireReleaseStructureCheck` | 0 | 20:21:59.506–20:21:59.706 | 断言 41，失败 0。只核对源码顺序：燃烧是冲突、预览、建计划、消费、挂载；引爆是预览、消费、再打击 |

`FireReleaseStructureCheck.java` 64–71 行把“消费发生在最终 `admit` 之前”写成了应保持的源码顺序。这和设计里“不能挂载则必须不扣款”之间的条件性缺口，静态检查不会报。

没有执行的游戏集成：没有 `LivingEntity`，没有服务端主线程，没有 `FireConsumption.schedule` / `detonate` 的真实调用，没有 `AttributeStateData.write`，没有死亡、旁观、错误线程、无敌帧免疫，没有地火 tick，没有一分为三的运行，没有 `compileJava`，没有 `build`、`jar`、`runClient` 或启动游戏。

范围内 57 个直接相关的生产和测试源码，检查前后 SHA-256 一致，`source-hash-pre.txt` 与 `source-hash-post.txt` 的比较是 0 条变化。其中 main 源码对应的 class 修改时间都不早于源文件，所以没有再跑 `.\gradlew.bat compileJava`。夹具调用的就是这些 class 里的账本方法，方法签名和这次读到的源码一致。

## 缺陷和差距

没有把下面第一项写成扣款故障。

1. 新需求差距，当前不实现。目标级“燃烧期间不能引爆、也不能再产种”不存在。最小的未来落点是 `FireConsumption.detonate` 增加对目标燃烧账的拒绝，以及 `FireSeedState.grant` 在目标仍有燃烧轨道时拒绝入账。效果键排他和同一本账的一次消费都已经存在，不能代替这个目标级开关。现有 `FireSeedCheck` 和 `SixFireFunctionalCheck` 没有把“燃烧中补种再引爆”断言为失败；本轮夹具证明它现在会成功。缺这套策略不会让已经按“全部消费一次”写成的账本断言失效。

2. 条件性顺序缺口，当前三式进不去，本轮没有复现成丢种。`FireConsumption.schedule` 52–67 行和 `detonate` 95–100 行在 `FireSeedState.consume` 已经写存储后仍可能返回空。若要让设计 48 行在计划器中途改账时也成立，最小修复是这两处失败分支把本次投入加回，并撤销该目标的消费标记。撤销标记现在没有公开入口，会碰到 `FireCastContext` 和 `FireSeedAccount`。不要用复制一套扣点公式来补。当前 `spread`、`sky`、`thrust` 不需要为了这条做行为变更。

没有发现现行成功路径上的漏扣、双扣或旧余额回写。同一对象的第二次消费会被拒绝。脉冲使用封存伤害，不写火种。

## 源码、占用和进程

生产源码和现有测试源码未改。57 条哈希稳定。本任务目录新增报告、哈希、夹具、`check-classes` 和运行日志。

检查结束时没有 `java`、`javaw` 或 `gradle` 进程。没有仍由本轮持有的源码写入。没有残留子进程。
