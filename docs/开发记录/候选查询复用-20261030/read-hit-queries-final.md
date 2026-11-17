我先读这份任务说明，确认授权范围后再按它执行。任务是只读定位命中候选查询。我先读目录记录和两份索引，再用源码核对查询入口。索引里的行号可能已经过期。我按当前源码读各查询入口和它们直接调用的判定。只读定位完成。没有改文件，没有后台作业。下面只记当前源码里能对上的调用、顺序和数据关系。`getEntitiesOfClass` 的引擎内部遍历、谓词调用次序、返回列表是否可改、以及 `setPos` / `hurt` / `clip` 是否再触发未打开的原版或 Forge 回调，都记为未知。

## 查询入口

这些调用都是 `getEntitiesOfClass(LivingEntity.class, aabb, predicate)`，返回 `List`。本仓库这些位置都没有把调用者自己的容器传进去。返回列表在这些方法里只被 `for` 遍历或取 `size()` / `isEmpty()`，没有 `sort`、`add`、`remove`、`clear`，也没有存进字段。

| 入口 | 行号 | 包围盒 | 谓词 | 遍历与早停 | 计入 `ENTITY_QUERY_CALLS` |
|---|---|---|---|---|---|
| 陷阱踏板 | `TechniqueHitEntity.java` 331–346 | `getBoundingBox().inflate(0.3)` | `TechniqueHitSettle.canHit` | 不逐个处理；非空才引爆。查询本身不早停 | 是，用 tick 开头的 `recordingNow` |
| 普通命中 | 同文件 348–379 | `hitbox.broadPhase(this)` | 同上 | 返回列表全遍历，无 `break` | 是，同上 |
| 弧带 | 同文件 932–971 | `sliceBroadBounds` 的六个数做成 `AABB` | 同上 | `cursor.swept()` 才查；列表全遍历 | 是，查询之后再读 `isRecording()` |
| 引爆 | `TechniqueHitSettle.java` 216–239 | `getBoundingBox().inflate(1.5)` | 同上 | 列表全遍历 | 是，查询之后再读 |
| 剑扫掠 | `SwordEntityMotion.java` 82–159 | `new AABB(from, targetPoint).inflate(0.4)` | `SwordEntityMotion.canHit` | 列表全遍历。已命中或 `hit.size()>=5` 时 `continue`，不 `break` | 是，方法开头的 `recordingNow` |
| 体积盒 | `HitVolumes.java` 110–130 | 该体积当前 `volume.box` | `isAlive()` 且 `!volume.hit.contains(entity)` | 全遍历 | 是，查询之后再读 |
| 光束 | 同文件 184–218 | `new AABB(start, end).inflate(1.5)` | `isAlive()`、不是主人、非旁观 | 全遍历；窄判定失败或 10 拍内再次接触则 `continue` | 是，查询之后再读 |
| 追踪选敌 | `TrackingSupport.java` 36–42 | `new AABB(position, position).inflate(5.0)` | 见下 | 锁定 UUID 仍合格时走 `level.getEntity`，不发这次空间查询。否则列表经 `stream().min` 取尽 | **否** |

`TrackingSupport.resolveTarget` 的空间谓词是：未移除、`isAlive()`、非旁观、`level()` 相同，再调用传入的 `eligible`。调用点是 `TrackingMotion.java` 57 行（`entity::canTrack`）和 `SwordVortex.steer`（`SwordVortex.java` 135–137 行，`canHit` 且 UUID 不在 `sword.hit`）。`min` 的比较键是中心点 `distanceToSqr`，然后是 `Entity.getId`。它不改原列表。距离平方还要 `<= 25.0`，否则被滤掉。

`canHit`（`TechniqueHitSettle.java` 28–36）只读：`null` 或未存活、旁观者为假；`casterUuid()` 为 `null` 或与目标 UUID 不同为真。不写位置、大小、生死，不发事件。`SwordEntityMotion.canHit`（162–164 行）不判 `null`，要求存活、非旁观、UUID 不等于 `sword.owner`。

`canTrack`（`TechniqueHitEntity.java` 773–776）是 `canHit` 再加：`hitInterval > 0`，或者 UUID 不在 `hitOnce`。它会在追踪查询的谓词里被调用。

## 普通命中的宽盒与窄判定

普通路径的前提是 `hitbox != null` 且 `age >= hitReadyAge`（348 行）。`TechniqueHitbox` 只有 `broadPhase` 和 `contains`，不能填容器。实现类的宽盒：

- `SizedBoxHitbox` 10–12 行：上一位置到当前位置，水平膨胀 `max(length, width)+1`，竖直膨胀 `max(hitHeight(), 1)`。
- `BoxHitbox` 10–12 行：只包当前位置，均匀膨胀 `boxSize().length()*0.5+1`。
- `SphereHitbox` 10–12 行：上一位置到当前位置，均匀膨胀 `max(boxSize().x, 0.3)+2`。
- `ObbHitbox` 10–12 行：与 `BoxHitbox` 的宽盒相同。
- `ArcHitbox` 10–13 行：锚点，否则施法者位置，否则实体位置；水平膨胀 `arcRadius()+1`，竖直膨胀 `arcHeight()+2`。

窄判定在查询之后，每个候选一次，无列表级早停：

- `SizedBoxHitbox.contains`：光束且 `clipsSolids()` 走 `MeleeCorridor.hits`（其中有一次 `level.clip`，17–47 行）；否则光束用目标盒再膨胀半宽半高做线段测试。三维姿态用中心点投影。精确体积只认当前盒子且 `hitHeight()>0.05`；非精确还接受上一中心到当前中心、目标盒膨胀 0.2 的扫掠。
- `BoxHitbox`：轴对齐盒与目标盒 `intersects`。
- `SphereHitbox`：目标盒膨胀半径后看两端点或线段。
- `ObbHitbox`：按实体 `getYRot()` 转到局部，再比半尺寸。中心点，不看目标盒边界。
- `ArcHitbox`：水平距离、竖直范围、与视线水平夹角不超过 `arcDegrees()*0.5`。`getCaster()` 会按 UUID 取玩家。

弧带窄判定是 `ProgressArcBand.intersectsSlice`（77–97、198–213 行）。它会再算一次 `sliceBroadBounds`。弦段最多 256。某一弦已经碰到就从弦循环返回，实体列表仍继续。`walkedChords` 是静态计数，命中路径上没有看到清零。

光束窄判定（`HitVolumes.java` 198–206）不走 `NARROW_PHASE` 计数：目标盒膨胀 0.5，须含起点或与线段相交；再要求 `now - last >= 10`。体积盒没有第二层几何测试。陷阱踏板也没有；非空就引爆，引爆范围不是踏板那份列表。

## 每次查询前后，源码里可见的变化

`TechniqueHitEntity.tick` 先 `super.tick()`，客户端直接返回。服务端 `pendingActivation` 直接返回。弧带走 `tickArcBand` 后返回，不会再进陷阱或普通查询。

非弧带、查询之前：

1. `previousPosition = position()`，然后 `age++`。`age == Integer.MAX_VALUE` 时 `onExpire` 后返回，不查询。
2. `motion.tick`。已读实现都可能 `placeAt` → `setPos`，或 `AttachedSwingMotion` 直接 `setPos`。`placeAt` 还可能改 yaw/pitch。`setPos` 内部是否改包围盒或触发区块回调，未打开。
3. 追踪且 `age != 0`：可能先做上面的未计数空间查询，再 `placeAt`。锁定目标仍有效时这次空间查询不发生。`LockTargetService.getMarkedTarget` 只是 `getEntity(id)`，不是空间查询；目标已死会从 `MARKS` 删掉。
4. `FunctionMotion` 在 `placeAt` 之后调用 `TrajectorySampler.fitVolume`。垂丝注册的 `WeepingCurtain.fit`（57–72 行）会探地、写入 `CACHED_SEGMENTS`、再次 `placeAt`、`resizeVolume`（`exactVolume=true`，同步长宽高，并把包围盒改成以当前位置为中心的立方范围），满足条件时 `sendParticles`。`probeTerrain` 本体未打开。
5. `refreshWeepingDeflection`：非持续接触立即返回。否则 `WeepingCurtain.active` 只读缓存；`WeepingDeflectionBinding.update` 改 `ConcurrentHashMap` 并 `DeflectionHosts.register`。注销时 `forgetHosts` 只从两张打开表里删键（`DeflectionService.java` 174–180）。
6. `age` 第一次达到 `hitReadyAge` 时 `WaterPartingService.activated`。已扫描、已取消或没有登记则返回。否则 `tryCommit` 可能 `setBlock`、回滚、改存档。方块写入发生在本次生物查询之前。
7. `endsWhenReleased` 且 `releaseStopped()` 时 `onExpire` 返回，不查询。`centered` 时把 `previousPosition` 改成运动之后的位置，宽盒的扫掠段变成点。

查询之后，普通和弧带：窄判定通过才 `tryHit`。`tryHit` 先写 `hitOnce` 或 `lastHitTick`；间隔不够就返回。随后 `WetPressure.note`、非持续接触的 `HitPayloads.apply`。`burstOnImpact` 置 `detonated`、调用 `listener.onHit`、`discard`，不再 `settle`。否则 `settle` 可能点火、灭火、缓慢、击退、记账，或 `TechniqueHits.hurtRoot`。`hurtRoot` 先做领域合并，再把 `invulnerableTime` 置 0 后 `target.hurt(playerAttack)`，然后恢复。`listener.onHit` 的各实现未打开。有账本时反馈用 `ChainScheduler.schedule` 推迟，不在这次查询返回前执行 `broadcastEntityEvent` 和 `playSound`。持续接触走 `settleFlow`：有账本只记账；否则 `hurtConfirmed`，不把无敌时间置 0。

同一条返回列表里，前一个目标的 `tryHit` 跑完才看下一个。这是同一次查询的消费，不是另一次查询。

弧带列表处理完才写 `lastProcessedProgress`、`arcDamageClosed`，再 `refreshArcDeflection`。打开条件满足时 `ArcDeflectionHost.bind` 并登记。然后寿命到了才 `onExpire`。

陷阱查询与引爆查询之间，源码只有 `detonated = true` 和 `inflate(1.5)`。没有 `listener`、`hurt`、粒子或 `discard`。第二份列表用的仍是 `canHit`，不要求目标来自第一份列表。第二份列表开始 `settle` 之后才有点火、击退、`hurtRoot`、`LifeDrain.start`（只向 `ChainScheduler` 登记，第一次在 `tickCount + 5`）、`listener.onHit`。循环结束后 `sendParticles`、`discard`、清载荷。

普通查询之后还有方块 `level.clip`。打到方块会 `setPos` 到击中点并 `onExpire`。寿命到期同样 `onExpire`。`onExpire` 调 `listener.onExpire` 然后 `discard`。`discard` 经 `remove` 卸下垂丝和弧带偏转宿主。

剑的 `hurtAlong` 在窄判定通过后才 `sword.hit.add`。主人在线走 `hurtRoot` 和 `HitPayloads.apply`；主人不在则 `target.hurt(magic)`。然后可能点火、灭火、缓慢、击退。`flyStep` 里零步长分支只查这一次，之后可能 `discard`。普通飞行先 `level.clip`，再 `hurtAlong`，再可能 `plant`（`setPos`）或 `setPos` 到停点。`returnTick` 可能先 `tryRecoverOwner`（幻剑改魔力并 `discard`，真剑进背包或掉落）；没回收才 `hurtAlong`，然后 `setPos`。悬停、插地不调用 `hurtAlong`。风暴 `SwordVortex.tick` 只 `setPos` 公转；到期那一拍 `ownedBy` 后改状态，不在这一拍 `hurtAlong`。

`HitVolumes` 在 `TickEvent.ServerTickEvent` 且 `phase == END`。每个还活着的体积先减寿命；到期则移出队列、丢掉视觉体、调用 `onExpire`，本拍不再查。移动体积会换 `box` 并对视觉潜影贝 `setPos`。然后查询。命中后把实体对象放进 `volume.hit`，再调用 `onHit`。光束在全部体积之后：重算线段、查询、逐个 `hurtRoot`，成功才写入 `lastHitTick` 并调用 `onHit`，然后 7 次 `sendParticles`。`onHit` / `onExpire` 的具体消费者未打开。

`super.tick()`、`Entity.setPos`、`LivingEntity.hurt`、`Level.clip`、`getEntitiesOfClass` 本体都未打开，不能把它们写成无副作用。

## 同一次 tick 里，两次查询之间有什么

同一 `TechniqueHitEntity.tick` 不会同时走弧带、陷阱和普通查询。弧带在前面就返回。陷阱和普通是 `if / else if`。

已能对上的连续查询：

- 踏板列表非空时，紧接着就是引爆查询。中间没有命中回调和 `hurt`。有 `setPos` 等原版回调的可能只存在于未打开的 `getBoundingBox()`。
- `TrackingMotion` 在锁定失效时，空间选敌查询先发生，稍后同一次 `tick` 才是陷阱或普通查询。中间有 `setForward` / `placeAt`，垂丝可能改偏转表，第一次就绪的分水可能 `setBlock`。这些都在第二次查询之前。
- 飞行剑在追踪仍开启、`hit.size()<5` 且未坠落时，`steer` 里的选敌查询先发生，随后同一次 `flyStep` 只有一次 `hurtAlong`。移动分支中间有 `level.clip`。零步长分支中间是 `aim`。`hit.size()>=5` 时 `steer` 不再查询。
- `HitVolumes` 按队列插入顺序逐个体积查询。上一份结果为空时不会调用 `onHit`。静态体积之间源码只看到 `remainingTicks--`。移动体积会在下一次查询前改盒子和视觉体位置。某一份非空就会在下一次查询前调用 `onHit`。体积全部处理完才查光束。
- 同一次释放的多个命中体没有共用一份候选列表。`TechniqueHitSpec` 里没有 `getEntitiesOfClass`。它们是否在生成当拍进入实体遍历，以及谁先 tick，未打开。

`SwordEntity.anyOwnedBy` / `ownedBy`（202–251 行）扫描的是 `level.getAllEntities()`，不是 `LivingEntity` 空间查询。`anyOwnedBy` 找到第一把存活且主人相符的剑就返回。`ownedBy` 收齐后按 `Entity.getId` 排序。风暴里 `plannedTick != nowTick` 才清空 `LAUNCHING`；同一 `getTickCount()`、同一主人的后续剑复用这组 UUID，不再扫描。这组数据只活到下一个不同的服务器 tick 计数。`LAUNCH_GAP_TICKS` 是 6。

## 列表、跨 tick 状态、接口

查询返回的 `List` 不跨 tick 保存。跨 tick 留下的是别的结构：

- `hitOnce`、`lastHitTick` 在命中体实例上。`addAdditionalSaveData` 只写寿命，不写这两份。
- `sword.hit` 跨 tick，并经 `Hits` 写入 NBT。召回、进风暴、二次飞行会 `clear`。
- `Volume.hit` 保存的是 `LivingEntity` 对象引用，体积还在 `ACTIVE` 里就一直用于谓词。`ACTIVE` 是 `ConcurrentLinkedQueue`，`BEAMS` 是 `ConcurrentHashMap`。
- `Beam.lastHitTick` 是 UUID 到 `getGameTime()`。
- `TrackingMotion.lockedTarget` 和剑的 `trackingTarget` 跨 tick。下一拍先按 id 取实体。
- 垂丝水段在 `WeakHashMap` 里，给下一拍偏转读取。它不是生物候选列表。

没有现成的“把本次 `LivingEntity` 候选填进调用者容器”的命中接口。`ownedBy` 自己 `new ArrayList`，不是调用者传入的容器。`resolveTarget` 只返回一个实体。

局部可以再次使用、而且源码里已经在用的，是上面的 UUID 集合、锁定 id、风暴当拍的 `LAUNCHING`，以及命中体上的 `previousPosition`。它们都不是一份可交给另一次 `getEntitiesOfClass` 的候选列表。

## 计数和计时的边界

`PerformanceRecorder`：`DEFAULT_TARGET_TICKS = 1200`，`CAPACITY_TICKS = 6000`。命令 tick 数是 1 到 6000。`tickGate` 关闭或不是绑定线程时，`begin` 返回 0，`end` 忽略 0，`add` 直接返回。`add` 在 `delta <= 0` 时也直接返回。`isRecording()` 等于门开着且当前线程是绑定线程。

`PerfMetric.ENTITY_QUERY` 只包 `getEntitiesOfClass` 那个表达式，不包后面的消费循环。它嵌在 `HIT_ENTITY_UPDATE` 里；剑的那次还嵌在 `MOTION` 和 `SWORD_UPDATE` 里。导出说明写明这些耗时不能相加。`note_query_total` 写的是每 tick 查询次数等于 `TARGET_QUERY_CALLS + ENTITY_QUERY_CALLS`，两类不能把同一调用算进两边。`note_candidates` 写明候选数是重复访问照实累加，不是唯一目标，也不是索引内部扫描量。`note_owned_scan` 写明全实体扫描不进 `query_calls_total`。`note_updates` 写明两种 update 含休眠和等待激活。

和真实查询的对应关系：

- 上面七处计数点，每次实际调用且当时允许记账时 `ENTITY_QUERY_CALLS + 1`，`ENTITY_CANDIDATES` 加的是返回列表的 `size()`。`size()==0` 时后者被 `delta<=0` 丢掉，调用次数仍然加一。
- 陷阱、普通命中、`hurtAlong`、两种 update 用的是入口处先读的 `recordingNow`。查询过程中门若关掉，`add` 仍会再检查。弧带、引爆、体积和光束是查询之后才读 `isRecording()`。
- `HIT_ENTITY_UPDATES` / `SWORD_UPDATES` 在服务端 `tick` 的 `finally` 里加一，包括没有查询的等待激活、悬停、插地和未就绪。它们不是查询次数。
- `NARROW_PHASE_TESTS` 只在普通命中、弧带、`hurtAlong` 真正进入窄判定时加。踏板、引爆、体积、光束、追踪选敌不加。`hurtAlong` 因已命中或已满 5 个而 `continue` 的候选不加。
- `DAMAGE_ATTEMPTS` 在 `noteDamageOutcome`，也就是 `hurt` 真正调用之后。记账、间隔拒绝、`amount<=0`、空目标都不加。它不是查询次数，也不是 `tryHit` 次数。
- `TrackingSupport` 的 `getEntitiesOfClass` 不进 `ENTITY_QUERY_CALLS`，也不包 `ENTITY_QUERY` 耗时。
- `TARGET_QUERY_CALLS` 的已看到增量在 `ReleaseBatch.java` 117、152 行和 `LockTargetService.java` 68 行。那两次释放批查询和锁定查找不是这七个命中入口。
- `rg` 还看到其它 `getEntitiesOfClass(LivingEntity.class, …)`，而且不在 `ENTITY_QUERY_CALLS` 的增量点上：`FieldZones.java` 268、354；`DomainContactBilling.java` 37；`GroundFireFields.java` 114；`FentianEffect.java` 46；`SteamBurst.java` 43；`BarrierService.java` 191；`DomainPulses.java` 78、92；`FireBurstEffect.java` 78；`ObstacleBodies.java` 81；`MagicIgniteEffect.java` 23；`EnchantedProjectileHandler.java` 34；`MagicArcaneMissileEffect.java` 51；`MovementDashEffect.java` 46、90、140。这些方法体没有按本次入口打开。`ContactEnchantState.java` 100 行有查询，并且 105 行计入 `ENTITY_QUERY_CALLS`；它不在指定的命中类里。

`FieldZones` 的两处查询不在 `TechniqueHitEntity`、`TechniqueHitSettle`、剑或 `HitVolumes` 的调用链上。本次没有追它的调度者。

## 实际读过的文件

任务与索引：`read-hit-queries-task.md`、`记录.md`、`docs/术式链计划缓存说明.md` 开头与计数节、`read-entities-final.md` 的查询相关行、并行任务 `read-query-lifecycle-task.md`。

入口与直接依赖：`TechniqueHitEntity` 的 tick、弧带、`placeAt`、`resizeVolume`、存档、偏转和 `canTrack`；`TechniqueHitSettle` 全文；`SwordEntityMotion` 的命中、飞行、回收；`SwordEntity` 的 tick 与两处全实体扫描；`SwordVortex` 的 tick、排波、转向、公转和发射；`HitVolumes` 的 END 处理及 `Volume`/`Beam`；`ProgressArcBand` 到径向求交和分段循环；`SizedBoxHitbox` 全文；`BoxHitbox`、`SphereHitbox`、`ObbHitbox`、`ArcHitbox` 的宽窄判定；`MeleeCorridor.hits`；九个 `TechniqueMotion.tick`；`TrackingSupport` 全文；`WeepingDeflectionBinding.update`、`DeflectionHosts` 登记与注销、`DeflectionService.forgetHosts`；`WeepingCurtain.fit/active`；`WaterPartingService.activated/tryCommit` 的提交段；`LifeDrain.start`；`TechniqueHits` 的 `hurtRoot`、`hurt`、`hurtConfirmed`、`noteDamageOutcome`；`TechniqueHitbox` 与 `HitListener`；`LockTargetService.getMarkedTarget`；`PerfCounter`、`PerfMetric`、`PerformanceRecorder` 的门控、`begin/end/add` 和导出说明。

## 与另一只读任务可能重叠的位置

`read-query-lifecycle-task.md` 同时覆盖：`LockTargetService`、`SwordEntity.anyOwnedBy/ownedBy`、`SwordVortex`、`PerfCounter`、`PerfMetric`、`PerformanceRecorder`，以及映射源码里的 `getEntitiesOfClass`、区块和 `setPos`。本次为了命中路径也读了 `SwordVortex.steer`、`TrackingSupport`、`getMarkedTarget` 和计数器门控。实体加入移除、section 回调和查询引擎顺序留给那一项；本次没有打开 Minecraft 映射缓存。

## 未核对

- `ServerLevel.getEntitiesOfClass` 的遍历顺序、AABB 过滤时机、谓词何时执行、返回列表是否可变或是否为活视图。
- `Entity.setPos`、`setBoundingBox`、`Entity.tick`、`LivingEntity.hurt`、`Level.clip` 是否发 Forge 事件或移动回调。
- 各 `HitListener`、体积 `onHit/onExpire`、除垂丝以外的 `VolumeFit`。
- `HitPayloads.apply`、`WetPressure`、`DomainAuraResolver.merge` 的写入。
- `WeepingCurtain.probeTerrain`。
- 生成当拍新实体能否被同拍稍后的查询看见。
- 上表范围外那些 `getEntitiesOfClass` 的谓词、顺序和副作用。
- `FieldZones.pulse` 的调用者，以及场地列表是否跨 tick。
