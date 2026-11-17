我先读取这份任务说明，按里面授权的范围执行并给出完整报告。任务是只读定位释放链的可插桩点。我按授权范围读 `SkillUsePacket`、链解析和释放相关代码。释放目录很大。我先读链调度、释放入口和延迟结算这几条主路径，不展开实体与碰撞子目录。没有改动任何文件。下面是释放链的只读定位，供插桩使用。授权目录内没有 Minecraft Profiler，也没有 `nanoTime`。现有记录只有 `DebugLog`。

## 线程边界

玩法都在逻辑服务端线程。`SkillUsePacket.handle`（`network/SkillUsePacket.java` 100–118 行）用 `enqueueWork` 把释放放进服务端主线程；`setPacketHandled` 在网络线程，不含释放本体。

`ChainScheduler`（`shu/technique/chain/ChainScheduler.java`）没有工作线程。`schedule`（18–24 行）在 `runAtTick <= getTickCount()` 时于调用方线程当场 `action.run()`，否则只入 `ConcurrentLinkedQueue`。到期任务在 `TickHandler.onServerTick`（29–47 行）的 `TickEvent.Phase.END` 执行。同一条队列任务若再 `schedule` 且 tick 已到，会在当前任务栈内同步重入。

`ReleaseBatch.CURRENT`、`CastFrame.CURRENT`、`SkillUsePacket.ANNOUNCE` 都是 `ThreadLocal`，只在这条服务端线程上有意义。`executeSkill` 的 `finally`（888–896 行）会 `ReleaseBatch.unbind()`。延后任务靠闭包里的 batch 对象，在 `SkillSegmentRunner.runSegment`（429–432、601–603 行）再绑定并恢复，不能把排程时的 `current()` 当成执行时的账本。

## 嵌套：哪些计时含子项

一次按键的根区间是 `handle` 的 `enqueueWork` 体，含 `HoldRepeat` 与 `offer` → `offerNow` → `executeSkill`。它只包住已经当场跑完的工作。延后的链段、蓄力重入、Tally 结算、接触附魔扫描都不在这根区间里。

`ChainScheduler.schedule` 的当场分支含整段 `action.run()`。外层若包住 `schedule`，会再算一遍这段。两处只能留一处，不能相加。

`TechniqueChain.split`（`chain/TechniqueChain.java` 18–38 行）只在按下时同步切段。`runSegment` 里 440–464 行会再向前扫描索敌、弹道、规则。这是执行时的第二次归类，与 `split` 分开记。

`runSegment`（363–605 行）含：角色扫描、锁定、落点查询、`CastFrame.forAttack`、`CastFrame.bind`、`effect.execute`。`effect.execute` 之后的实体生成在未读的 `release/entity`。这一段无法再拆出生成耗时。`forAttack` 只算序号 0 的点（`volumePoint(0)`、`aimFor(0)`、`vortexPhase(0)`）。之后按序号再取样不在这次调用里。接触点在 560–571 行覆盖几何，覆盖前的 `forAttack` 仍算在 `runSegment` 内。

蓄力：`chargeTicks > 0` 且未武装时，398–413 行另排一次 `runSegment` 后返回。第一次调用不含那次重入。`commitNow`（208–210 行）和 `commitHostGroup`（274–277 行）在未武装时直接拒绝，不排程。

`commitHostGroup`（287–295 行）在同一次提交回调里连续调用多份 `runSegment`。宿主组任务的时长已含全部分份。`firePaidPlan` 的非宿主路径是每一份各 `schedule` 一次；tick 已到时，这些调用在 `for` 循环里同步连跑，`firePaidPlan` 的时长已含全部分份。

## 链解析与按下路径

`offerNow`（139–218 行）做手持、冷却、扣费前检查。`executeSkill`（221 行）开头还有装配拒绝、`ChainSteps` 切片，然后 258 行 `split`。

有 `RULE_DUAL` 时，`CompiledReleaseStep.compile`（`network/CompiledReleaseStep.java` 50–97 行）必定返回非空。`executeSkill` 299–307 行进入 `CastExecutionSession.open` 并返回，不执行 353 行的 `ReleaseBatch.open`，也不进入 365 行的段循环，因此不会走到 `SkillChainPlan.fireDualSequence`。没有双重、且动作后面只有一个条件释放时，`actionConditional`（104–156 行）同样走会话。其余链才进段循环。

段循环按角色分叉，都在服务端线程：

- 纯附魔链：270 行 `imprintEnchantChain`（1100–1115 行），同步，无 batch。
- 自身状态：401–414 行，当场返回。
- 操纵剑：418–421 行只排 `buildSegmentRunner`。
- 动作带攻击：430–465 行排动作；双重规则在此被上面的早退挡住。
- 链修饰：476–516 行，`firePlan` / `imprintEnchant`。
- 领域、屏障、护体、条件释放、场地：519–703 行。条件释放在 `startTick > openedTick` 时 616 行另排，完成后 `settleConditionalParent`（1023 行）。场地在 `startTick + chargeTicks`。
- 普通攻击：706–826 行先算附魔与伤害，再 `schedule`。冲刺触发下一段时，下一段的 `run()` 嵌在命中回调里（743–756 行），不在按下区间。

`gapBefore`（57–80 行）只在非步进链、且已有上一段时，于循环内同步累加 `startTick`。

`buildSegmentRunner` 的 lambda（98–118 行）含存活检查、松散宿主提交或 `runSegment`。`commitNow`（211–214 行）同步进入 `runSegment`，并把接触点写进几何。`playAction`（322–324 行）同样同步进入，伤害为 0，无 Tally。

## 目标查询

锁定只在 `runSegment` 482–486 行：未冻结朝向且 `entityLock` 时调用 `LockTargetService.resolve`（`effects/LockTargetService.java` 24–32 行）。`resolve` 含 `findTarget`（55–77 行）的 `getEntitiesOfClass`（视线 20 格、盒膨胀 8 格、线段距离）以及标记写入。发光 `addEffect` 在调用方 485 行，不含在 `findTarget` 内。冻结朝向（477–481 行）不再查询。

`getMarkedTarget`（42–53 行）是之后的标记读取。`HitVolumes` 的服务端 END（`release/HitVolumes.java` 71–98 行）会读它。那是命中盒存续期间的另一次查询，与按下时的 `resolve` 分开。`motion/` 未读。

落点在 510–521 行三选一：

- 可变弹道且（落点指定或龙卷）→ `ReleaseBatch.crosshairCenter`（101–121 行）：32 格 `clip`，再沿射线搜生物。没有缓存，每一份各查一次。
- 可变且交错且未锁实体 → 身体中心，无实体查询。
- 斩击落点指定 → `resolveAnchor`（123–148 行）：8 格线段最近生物，否则眼前 4 格。结果写在 batch 的 `anchor`。同一 batch 的后续份只走 124–126 行。
- 其余 → 眼睛位置。

`ReleaseBatch.open`（70–81 行）只扫描是否需要锚点，不查世界。

## 几何

`CastFrame.forAttack`（`release/CastFrame.java` 88–113 行）在不可变且非散布、非落点指定时直接返回，不改生成点。否则 `slashGeometry`（116–153 行，斩击）或 `projectileGeometry`（156–184 行，弹丸、垂落抬 8 格、龙卷只设相位）。立方取样在 `jitter` / `volumePoint`（196–205、256–262 行）。随机方向在 `aimFor` / `randomDirection`（207–223 行）。这些是生成几何的可分离点。`effect.execute`（586 行）及其后的实体轨迹不在本次可读范围内，不能从 `forAttack` 里减出去，因为它不在 `forAttack` 内，而在其后。

## 延迟汇总伤害

一分为三在份数大于 1 且 batch 非空时共用一本 `Tally`：`openGroupTally`（150–152 行）。创建于排程前，发生在 `firePaidPlan` 221、275 行，`fireDualSequence` 431 行，以及会话 `commitMember` 317、338 行。份数为 1 时 Tally 为 null，伤害不走这本账。

`Tally.record`（180–207 行）只入账。`FlowRoundLedger.FLUSH_DELAY` 为 6（`FlowRoundLedger.java` 17 行）。第一次入账把结算订在当前 tick+6；窗口内再次入账不再改期（201–202 行）。`record` 不含结算。

`Tally.flush`（217–255 行）在延后 tick 执行。它先清 `flushPending`，再对每个目标调用 `TechniqueHits.hurtConfirmed` 或 `hurtRoot`。`hurtRoot`（`TechniqueHits.java` 34–37 行）先做领域合并，再进 `hurt`（44–69 行）的 `playerAttack`。`hurt` 含原版受伤、无敌帧处理，以及 `TideMarkDamageScope`。原版受伤会同步触发 `ReleaseBaseEvents.onHurt` / `onDamage`（`momentum/ReleaseBaseEvents.java` 45、117 行）。因此 `flush` 含伤害回调；`record` 不含。结算过程中若再次 `record`，会另排一本新的 6 tick 结算，两段不能加成一次伤害。

命中调用 `record` 的位置在未读的 `release/entity/TechniqueHitSettle`。插桩点放在 `record` 与 `flush` 入口即可盖住，不必先进入实体目录。

## 三条不能加在一起的生命周期

附魔有三条时间线。没有攻击时，`imprintSelection`（`SkillChainPlan.java` 625–644 行）在按下时同步写入主手，真正生效在之后的普通攻击，不在这次释放里。攻击带着附魔时，`EnchantBonus.resolve` 在排程前同步完成，对象放进 `CastFrame`；命中时的附魔回调在 `hurt` 栈内，与按下、与 `record` 都不是同一段。接触附魔由 `ReleaseBaseEvents.onServerTick`（173–185 行，同样是 END）调用 `ContactEnchantState.tick`（50–68 行，只在偶数 tick 扫描并 `getEntitiesOfClass`），最后 `DomainContactBilling.settlePending`（55–79 行）。`settling` 为真时 `collect` 直接返回。它与链调度队列无关，不计入某次释放。

Tally 的生命是：按下时创建，命中 tick 入账，约 6 tick 后 `flush` 一次。持续命中可以在清账后再次入账，从而有下一次 6 tick。它不是按下时长的尾部。

多重释放有两条代码，同一次按键只走一条。链上有 `RULE_DUAL` 时只走 `CastExecutionSession`：`open`（96–114 行）后，普通双重在 `startOrdinary` → `beginGroup`（215–252 行）同步提交第 0 个成员，再用 `STEP_GAP = 5`（45 行）排 `runSecond`（254–267 行）。动作带头时 `startAction` 只放出动作并返回 `CastTiming.deferred`；成员要等 `onContact`（166–181 行），间隔从接触 tick 再加 5。`offerNow` 196–200 行因此不在按下时写冷却。每个接触组自己 `ReleaseBatch.open` 后立刻 `unbind`（229–230 行）。`fireDualSequence`（306–455 行）使用 `DUAL_STEP_TICKS = 5`（815 行），按攻击序号错开排程，附魔在循环内同步烙印（398–411 行）。它挂在段循环的 `firePlan` 上；有 `RULE_DUAL` 的按键在 299 行已经返回，不会进入它。`docs/底座功能点.md` 写双重间隔 10 tick；当前这两个常量都是 5。插桩以常量为准。

## 现有设施

`debug/DebugLog.java` 的 `log`（34–45 行）在非生产环境默认开启，写 slf4j，并保留最多 200 行。关闭时直接返回。它不是分层计时。释放路径上的调用点：`SkillUsePacket` 186、190、198、200、649、719、740 行；`ChainScheduler` 44 行只在任务抛错时。`TechniqueHits` 没有日志。开启调试日志本身会进入上述区间，采样时要单独看。

## 最小插桩候选

服务端主线程、互斥区间，子区间不要加进父区间：

1. `handle` 的 `enqueueWork`：按下根，只代表同步部分。
2. `TechniqueChain.split`：切段。`gapBefore` 可并进段循环的排程准备，不要并进 `split`。
3. `ChainScheduler.schedule` 的当场 `action.run()`，以及 `onServerTick` 里每个 `task.action.run()`：延后根。不要再包一层 `schedule` 把当场执行算第二次。
4. `runSegment` 内部分开：440–464 行扫描、`LockTargetService.findTarget`、485 行发光、`crosshairCenter`、`resolveAnchor` 的首次查询与缓存返回、`forAttack`、`effect.execute`。`effect.execute` 含未拆的生成。
5. `Tally.record` 与 `Tally.flush` 分开。`flush` 之下再标 `TechniqueHits.hurt` / `hurtConfirmed`。不要把 `record` 加进 `flush`。
6. `ContactEnchantState.tick` 与 `DomainContactBilling.settlePending` 单独成条。
7. `WeaponEnchant.imprint` 的调用只代表烙印。随后的普通攻击另算。
8. 会话路径标 `beginGroup`、`commitMember`、`runSecond`。第 0 成员在按下或接触回调里，第 1 成员在 5 tick 后。
9. 宿主组用 `commitHostGroup` 的一次回调当父区间；非宿主的同 tick 多份用各次 `runSegment`，父区间已经含它们时不要再加总。
10. `resolveAnchor` 按 batch 只把第一次查询算搜索；`crosshairCenter` 按调用次数算。
