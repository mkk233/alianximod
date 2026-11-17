我先通读这份任务说明，按它划定的范围读取并整理最终报告。任务是只读定位，不改代码。我按授权范围去找实体更新、碰撞、伤害、同步和服务器 tick 的接入点。关键方法已经定位。接下来读 tick、生成、碰撞结算和服务器生命周期的具体实现。没有改动任何文件，也没有编译、启动游戏、打包或提交。下面是服务端实体与命令的只读定位，不是正确性复审。

## 线程和客户端分支

命中体和剑的 `tick` 都先 `super.tick()`，再用 `level().isClientSide()` 分开。服务端逻辑跑在 Minecraft 的 **Server thread**：单机是集成服务端那条线程，专用服务器只有这条。客户端副本跑在客户端线程。`isClientSide()` 看的是世界逻辑侧，不是线程名。

`TechniqueHitEntity.tick`（`TechniqueHitEntity.java` 279–287）在客户端只做 `ClientTrack.afterClientTick`；没有弧带时再 `clientDecor`（362–368，粒子），然后返回。`lerpTo` / `moveTo`（261–276）在客户端交给 `ClientTrack.onClientMove`，服务端才走原版。`SwordEntity.tick`（318–322）客户端同样只做 `afterClientTick` 后返回。`ClientTrack`（`ClientTrack.java` 17–41）只在客户端记下移动包到达前的位置，并在 tick 后写回插值起点。

`onSyncedDataUpdated`（`TechniqueHitEntity.java` 206–220，`SwordEntity.java` 154–158）两侧都会跑：弧带字符串在收到同步数据时解开并改包围盒，施法者 UUID 写回字段。这是数据包应用到实体之后，不是发送。

`XueXiMod.ClientModEvents`（`XueXiMod.java` 116）和 `DebugLogCommands`（`DebugLogCommands.java` 13）标了 `Dist.CLIENT`。`ModEvents.ClientEvents.onClientTick`（111–113）也只在客户端。服务端命令和 `ServerTickEvent` 没有 `Dist`，跑在 Server thread。

## 插桩边界

实体自己的 `tick` 发生在本拍世界更新里，早于下面所有 `TickEvent.Phase.END`。END 里只能看到这一拍实体循环已经做完的结果。

**更新。** `TechniqueHitEntity.tick` 279–358。服务端在 `pendingActivation`（289–290）时直接返回，宿主组未放行前不计运动和命中。弧带走 `tickArcBand`（293–295，实现 858–888）后返回。普通路径先记 `previousPosition`，再 `age++`。`SwordEntity.tick` 318–375 按状态分到悬停、`fly`、`returnTick`、`SwordVortex.tick`、插地。悬停满 10 拍才 `setState(FLY)`（341–343）。

**运动。** 命中体的运动点是 `motion.tick(this, level)`（303–305）。接口在 `TechniqueMotion.tick`。`applySpec` 对 `STILL_AIM`、`VORTEX`、`FUNCTION` 会在 `addFreshEntity` 之前再调一次 `motion.tick`（497–503），所以只包 `tick()` 会漏掉生成当次的第一步。`placeAt`（512–519）写位置和朝向。弧带不走 `motion`。剑的飞行是 `SwordEntity.fly` → `SwordEntityMotion.flyStep`（157–211）：步进、方块 `clip`、`setPos`。召回位移在 `returnTick`（381–412）。风暴公转是 `SwordVortex.place` 的 `setPos`（148–159），`launch`（162–172）只改状态和方向，伤害留到之后的 `flyStep`。`HitVolumes.syncVisual`（267–281）也只是 `setPos` 和转角，方法名不代表有自定义包。

**候选查询。** 普通命中：`hitbox.broadPhase` 之后 `ServerLevel.getEntitiesOfClass(LivingEntity, broad, canHit)`（327–330）。陷阱踏板：包围盒 `inflate(0.3)` 再查询（318–325）。弧带：`ProgressArcBand.sliceBroadBounds` 后的 `searchBox` 查询（868–873）。引爆：`detonate` 里再查一次 `inflate(1.5)`（`TechniqueHitSettle.java` 187–190）。剑：`hurtAlong` 用两端点盒 `inflate(0.4)` 查询（78–80）。`SwordEntity.anyOwnedBy` / `ownedBy`（191–216）扫描 `level.getAllEntities()`。风暴每拍每主人只做一次：`SwordVortex.tick` 里 `plannedTick != nowTick` 才清空，`planWave` 再 `ownedBy`（67–77、85–91）。另一条不挂在这两个实体类上的查询是 `HitVolumes.TickHandler.onServerTick`（71–117）：END 阶段对活动体积 `getEntitiesOfClass`。

**精确碰撞。** 候选之后才窄判定。默认实现是 `SizedBoxHitbox.contains`（16–32）：光束走 `intersectsBeam` 或 `MeleeCorridor.hits`；三维姿态走 `intersectsTiltedPlane`；精确体积只认当前盒子 `holdsPoint`（35–47、53–57）；非精确体积再接受上一中心到当前中心的扫掠 `sweptSegmentHits`（60–66，目标盒膨胀 0.2）。弧带窄判定是 `ProgressArcBand.intersectsSlice`（874–879）。方块截断是 `level.clip`（命中体 337–346，剑 196–201），和生物查询分开。剑的窄判定在 `hurtAlong`（84–86）：线段 `clip` 为空且膨胀 0.2 的盒子不含终点则跳过。

**实际伤害。** 窄判定通过后进 `TechniqueHitSettle.tryHit`（35–77）。`hitInterval <= 0` 用该实体的 `hitOnce`；大于 0 用 `lastHitTick` 和 `level.getGameTime()`。然后才 `settle`（80–144）。有 `settleTally` 时这里只 `record`，不调用 `hurt`。没有账本时才 `TechniqueHits.hurtRoot`（137）。持续接触 `settleFlow`（146–165）同样：有账本只记账；没有账本才 `hurtConfirmed`。`hurtRoot`（`TechniqueHits.java` 34–37）先做领域合并，再以 `BYPASS_INVULNERABILITY` 进入 `hurt`（44–68）：短暂把 `invulnerableTime` 置 0 后 `target.hurt(playerAttack)`。`hurtConfirmed`（76–90）保持原版无敌帧。剑在 `hurtAlong`（87–104）当场 `hurtRoot`，主人不在时直接 `target.hurt(magic)`，不进 `Tally`。`detonate`（191–193）对范围内目标再走 `settle`。

**生成。** `TechniqueHitSpec.spawn`（154–156）转到 `spawnReported`（159–354）。数量：无宿主组用 `SkillEntityCount.entityCount()`，有宿主组用 `hostGroup.spawnCount()`（256）。循环里 `new TechniqueHitEntity`、可选 `holdActivation`、`applySpec`、`level.addFreshEntity`（308–328）。`accepted` 只在 `addFreshEntity` 返回 true 时加一（328–337）。`SpawnReport.requested` 是循环次数，失败的弧带或异常不计入 `accepted`（293–320）。幻剑生成在 `PhantomSwordEffect`：按 `SkillEntityCount` 循环 `SwordEntity.phantom` 再 `addFreshEntity`（27–35）。真剑生成在 `ModEvents.onTossSword`（68–79）：`SwordEntity.real` 后 `addFreshEntity`。`HitVolumes` 的视觉体是另一路 `addFreshEntity`（250–257），查询仍在 END 的体积循环里。

**显式同步。** 这两类实体没有自己的 `SimpleChannel` 包。可见状态走 `SynchedEntityData`：`defineSynchedData`（命中体 175–197，剑 291–297），写入在 `applySpec` 的 `entityData.set`（406–490）、`adoptArc`（855）、`pulseRearRing`（646–650）、`stampStillBirth`（767–774）、`resizeVolume`（833–835）、剑的 `setOwner` / `setState` / `setUprightFace`（161–164、270–287）。`entityData.set` 和 `setPos` 只把实体标脏。生成包、位置包、实体数据包由原版追踪在这些方法返回之后发送。客户端吃到移动包的入口是上面的 `lerpTo` / `moveTo`。

方法体内真正把包交出去的是：`TechniqueHitSettle.settle` 里 `level.broadcastEntityEvent(target, (byte) 2)`（117），以及 `playSound`（118–120）；`detonate` 的 `level.sendParticles`（202）。账本反馈被 `ChainScheduler.schedule` 推迟（113）。命令侧是 `ShuCommands.setManaUpperBound` 的 `ModNetwork.sendToPlayer`（522）。`ModNetwork.sendToPlayer`（30–31）是 `CHANNEL.send(PacketDistributor.PLAYER.with(...))`。`FireVisualRelay` 用 `TRACKING_ENTITY_AND_SELF` 发 `FireVisualPacket`（97–98），同样停在这条通道的发送分发。

## 同 tick、多实体、延迟结算怎么计数

一次 `spawnReported` 在同一次调用里连续 `addFreshEntity`。这些实体要到之后的世界实体循环才各自 `tick`，同一拍里按世界里的实体顺序逐个执行，共用一个 Server thread。

去重是按实体实例的。`hitOnce` 和 `lastHitTick` 互不共享。`hitInterval <= 0` 时同一实体对同一 UUID 只进入一次 `settle`。`hitInterval > 0` 时间隔看 `getGameTime()`，不是 `getTickCount()`。剑的 `hit` 也是每把剑一份，并且 `hit.size() >= 5` 后不再打（`hurtAlong` 81–82）。

共享的是 `ReleaseBatch.Tally`。`spawnReported` 把 `frame.tally` 抄进每个 spec（168），多实体可以记进同一本账。`Tally.record`（180–206）用 `getTickCount()`。`FlowRoundLedger.roundIndex`（24–29）在 `hitInterval > 0` 时是 `(nowTick - originTick) / hitInterval`，否则轮次为 0。`TideMarkCopies.accept`（18–30）对同一目标、同一轮、同一 `copyIndex` 只收一次。`copyIndex` 在生成时被写成 `CohortVisual.settleEncoding(baseCopy, spawnIndex)`（275）。收下之后 `counts` 加一，`damages.put` 留下最后一次伤害（`FlowRoundLedger.java` 37–38）。`aggregatedDamage`（59–64）是这份最后伤害乘份数，不是各次伤害相加。

`FlowRoundLedger.FLUSH_DELAY` 是 6（17）。已有未到期的 `flushPending` 时，后续 `record` 只入账，不改 `flushTick`（201–203）。第一次才 `schedule(now + 6)`（204–206）。`ChainScheduler.schedule`（18–23）若 `runAtTick <= getTickCount()` 会在调用栈上立刻 `run`；否则进队列，由 `TickHandler.onServerTick` 的 END（29–46）执行。实体 `tick` 早于 END，所以本拍记入、到期日为今天的账，会在本拍实体循环之后的 END 里 `flush`。`flush`（217–251）每个目标打一次：有持续接触载荷走 `hurtConfirmed`，否则 `hurtRoot`，然后 `clearSettled` 清份数和伤害。已见过的轮次和复制序号留在 `TideMarkCopies`，同一份不会再进账。

反馈音效和击退是另一条延迟：`feedbackTick = getTickCount() + settleCopyIndex * 2`，只有它小于 `flushTick()` 才排队（111–113）。复制序号较大时这次反馈不会排队。`ChainScheduler` 的队列是进程级 `ConcurrentLinkedQueue`，END 时 `runAtTick <= now` 就执行。

`pendingActivation` 为真时实体已在世界里，但 `tick` 不查询、不命中。`releaseActivation`（375–383）才开始；出生时间在宿主组路径上也不在 `applySpec` 里写，而在放行或非宿主组的 `stampStillBirth`（343–345、767–774）。

风暴的“同一拍多把剑”：`planWave` 按剩余剑数决定本波数量（111–123），本拍选中的才 `launch`。公转本身不调用 `hurtAlong`。

## 网络耗时测到哪

`CHANNEL.send`、`broadcastEntityEvent`、`sendParticles`、`playSound` 返回时，包已经交给连接的发送队列。这段耗时是入队，不包含线上字节、对端到达、客户端解码或插值。`entityData.set`、`setPos`、`addFreshEntity` 连入队都还没做，只改服务端实体或把它放进世界；追踪包在原版实体追踪里另发。`SpawnReport.accepted` 是成功进世界的个数。`ClientTrack.onClientMove` 是客户端应用移动包之后的插值，和发送耗时无关。`HitVolumes.syncVisual` 没有发送。`DebugLog` 只写本地队列和 slf4j。

## 关闭时的快路径

现成的关闭路径是 `DebugLog.log`（34–36）：`enabled == false` 时立刻返回，不进队列、不打日志。`setEnabled(false)` 会 `clear`（27–31）。默认 `enabled = !FMLLoader.isProduction()`（18），正式包启动即为关。`/shulog off` 走这条。

以后若加计时，安全的关闭检查是服务端分支里、任何分配之前的一次 `volatile` 读，然后落回现有代码。位置：`TechniqueHitEntity.tick` 在 `isClientSide` 返回之后（288 行附近）、`spawnReported` 循环体开头、`tryHit` 入口、`SwordEntity.tick` 在客户端返回之后（323 行附近）、`flyStep` / `hurtAlong` 入口。不要把检查放进 `getEntitiesOfClass` 的谓词。不要在关闭路径里 `entityData.set`、拼字符串或记日志。`pendingActivation` 的返回是玩法门闩，不能当成性能开关。`ChainScheduler.onServerTick` 不能因为计时关闭而跳过队列，否则延迟结算不会执行；只能让计时包装自己空转。

## 已有调试

没有围绕实体 tick、查询、碰撞、伤害、生成的 `nanoTime` 或采样器。`CastCooldown` 的 `nanoTime` 是会话编号。调试记录只有 `DebugLog`，容量 200。`/shulog`、`on`、`off`、`clear` 在 `DebugLogCommands.register`（19–37），仅客户端。`ShuCommands.writeBook` 在拒绝和成功时打 `DebugLog`。`TechniqueHitSettle`、`ChainScheduler`、`HitVolumes` 只在回调抛错时打日志。`/shu cd off|on` 和 `/shu entities <数量>`（272–278、175–177、541–546）改冷却和生成数量，不是性能开关。

## 命令注册与服务器生命周期

命令注册：`ModCommands.onRegisterCommands`（`ModCommands.java` 22–24）订阅 `RegisterCommandsEvent`，再调 `ShuCommands.register`（50–279，权限等级 2）和 `ActionCommands.register`。客户端调试命令不在这里，而在 `RegisterClientCommandsEvent`。

`XueXiMod` 构造里 `MinecraftForge.EVENT_BUS.register(this)`（70）。`onServerStarting`（112–113）是空的。`commonSetup`（93–99）注册 `ModNetwork` 并装载术式，没有 tick。

`ServerStartedEvent`：`ModEvents.ServerEvents.onServerStarted`（31–36）清释放会话、开始冷却会话、清领域脉冲并 `DomainService.onServerStarted`；`HostCastEvents.onServerStarted`（23–27）`HostCast.closeAll`。

`ServerStoppingEvent`：`ModEvents.onServerStopping`（39–44）取消释放、清领域脉冲、结束冷却、`DeflectionHosts.onServerStopping`（151–154）；`HostCastEvents.onServerStopping`（31–35）；`PrisonLifecycle.onStopping`（52）；`DeflectionFlightEvents.onStopping`（73–77）；`ActionServerEvents.onServerStopping`（33–35）。

`ServerStartingEvent` 的另一处是 `PrisonDeflectionLifecycle.onServerStarting`（33–37），只登记一次水牢监听。

`TickEvent.ServerTickEvent` 且阶段为 END 的入口：`ChainScheduler.TickHandler.onServerTick`（29–46，延迟结算）；`ReleaseBaseEvents.onServerTick`（173–185）；`HitVolumes.TickHandler.onServerTick`（71）；`ActionServerEvents.onServerTick`（25–29）；`FireVisualRelay.onServerTick`（102–114，按间隔发视觉包）；`GroundFireFields.onServerTick`（59）；`PrisonLifecycle.onServerTick`（21–25）；`DeflectionFlightEvents.onServerTick`（81–87）。玩家 END 拍、且已收成 `ServerPlayer` 的是 `ModEvents.onPlayerTick`（92–99）和 `HostCastEvents.onPlayerTick`（39–46）。这些都不是命中体或剑的 `tick`。
