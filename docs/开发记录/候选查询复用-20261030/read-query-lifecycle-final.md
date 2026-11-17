我先读这份任务说明，按它划定的范围做只读查询，再交完整报告。任务范围是只读定位查询与实体生命周期。我先读本目录记录和缓存说明，再按符号精确检索相关源码。记录和锁定查询已读到。接下来只看段执行里的目标传递、剑实体查询，以及性能计数和实体生命周期相关 mixin。映射源码在本机 Forge 缓存的 sources jar 里。我只从里面抽出查询和实体进出相关的方法，不落盘。同一次释放里，锁定查询没有候选缓存。非宿主的多份复制会各自再查一次，两次查询之间已经跑完上一份的后续写入。距离相等时，锁定查询留下列表里更早的那个。

## 同一次释放会不会重复查

`LockTargetService.resolve` 的调用点只有两处：`SkillSegmentRunner.runSegment`，以及 `UniversalLockTargetEffect.execute`。链上的「锁定实体」角色是 `SEEK_ENTITY`，`SkillSegmentMath.isChainModifier` 为真。`SkillUsePacket` 排段时会跳过这种修饰，不把它自己排成动作。常规链因此走到后面那次攻击的 `runSegment`，不走到 `UniversalLockTargetEffect`。

`runSegment` 在 `entityLock && !useFrozen` 时调用一次 `resolve`。`entityLock` 来自更早术式的角色扫描，或 `FieldZones.influence` 里的 `attachedLock`。`lockThisCast` 只传给 `TechniqueCastContext`，不决定这次查不查。

一次 `runSegment` 里，这次锁定查询之后还可能再做一次同类空间查询：

- `mutable && batch != null && (spaceLock || pathOverride == VORTEX)` 时调用 `ReleaseBatch.crosshairCenter`。
- 否则 `spaceLock && batch != null` 时调用 `resolveAnchor`。锚点字段还是空、并且 `wantAnchor` 为真时，它内部再调用 `getEntitiesOfClass`。

这两次都记在 `TARGET_QUERY` / `TARGET_QUERY_CALLS`。锁定查询和它们之间已经执行过：目标非空时的 `addEffect(GLOWING, 200, 0)`，以及 `fieldRadius > 0` 时的 `FieldSpawner.releaseAttached`（其中调用 `FieldZones.add`）。`FieldZones.influence` 本身只扫已有场地列表，不调用 `getEntitiesOfClass`。

多份复制：

- 非宿主：`SkillChainPlan.firePaidPlan` 对同一波的每一份调用 `buildSegmentRunner`，排在同一 tick。每一份进入 `runSegment` 且仍要锁定时，各自再 `resolve` 一次。
- 宿主组：`commitHostGroup` 在一次回调里按份循环。`copyIndex > 0` 的已排任务在 `commitLooseHost` 直接返回，不再进入 `runSegment`。循环内第一份查询之后调用 `freezeLook`。`frozenLook` 已写入时，后面的份 `useFrozen` 为真，不再 `resolve`。`freezeLook` 只在视线非空且 `lengthSqr > 1.0E-8` 时写入；没写入时，后面的份会再查。
- 波次间隔是 `DUAL_STEP_TICKS = 5`。每一波是另一次调度，锁定条件仍成立时会再查。

`ChainScheduler` 在 `runAtTick <= 当前 tick` 时当场执行；未来 tick 则入 `ConcurrentLinkedQueue`，在 `ServerTickEvent` 的 `END` 按入队顺序执行。同一波的非宿主各份因此按排程顺序依次跑完。上一份在 `resolve` 之后会继续执行 `effect.execute` 以及上面的发光和场地写入，这些都发生在下一份 `resolve` 之前。`effect.execute` 的各个实现没有逐个读完，不能把这段当成没有世界写入。

蓄力且闸门未武装时，`runSegment` 在调用 `resolve` 之前就排后续任务并返回。查询发生在延迟后的那一次进入。

`getEntitiesOfClass` 会给分析器计数 `getEntities`。谓词是调用方的 `entity != player && entity.isAlive()`。`LivingEntity.isAlive` 不是 `final`，基类体只读移除标记和生命值；子类覆盖没有逐个证实。加入世界时的 `EntityJoinLevelEvent` 不在这条查询路径上。

## 距离相等时留下谁

`LockTargetService.findTarget` 用 `distSqr < bestDistSqr`。相等时保留列表中更早的候选，没有实体 id 二级比较。超过 `8²` 的候选在比较前被跳过。视线两端重合时 `abSqr == 0`，代码仍做除法；比较失败时 `best` 维持空。

`getEntitiesOfClass(Class, AABB, Predicate)` 在 `EntityGetter` 第 28–30 行，转到 `Level.getEntities`（第 732–766 行）。列表顺序是下面的遍历顺序，不是距离排序：

1. `EntitySectionStorage.forEachAccessibleNonEmptySection`：section X 从查询盒 `minX - 2` 到 `maxX + 2` 的 section 坐标，按 int 从小到大。每个 X 取 `LongAVLTreeSet` 里该 X 的键。键由 `SectionPos.asLong` 拼成：X 在第 42 位起，Z 在第 20–41 位，Y 在第 0–19 位。同一 X 内按这个 long 升序，也就是 Z 字段的位模式在前、Y 字段在后。负坐标经过掩码后高位为 1，排在同一字段的非负坐标之后。Y 还要落在 `minY - 4` 到 `maxY` 的 section 里，Z 落在 `minZ - 2` 到 `maxZ + 2`；空 section 或 `!isAccessible()` 跳过。
2. section 内 `ClassInstanceMultiMap.find(LivingEntity.class)`。该类列表第一次由 `allInstances` 过滤生成，之后的 `add` 追加，`remove` 保持其余相对顺序。
3. 先 `getBoundingBox().intersects(查询盒)`，相交才执行谓词。谓词通过才追加进 `ArrayList`。
4. section 扫完后，`getPartEntities()` 里能转成目标类、相交且谓词通过的再追加。末影龙部件展开被 `if (false)` 关掉。

同一次 `runSegment` 里的另外两次查询规则不同：`crosshairCenter` 用 `distance <= bestDistance`，相等时换成后面的实体；`resolveAnchor` 用 `distance < best`，相等时留前面的。`resolveAnchor` 把结果写进该 `ReleaseBatch` 的 `anchor`，之后直接返回这个向量，不再查询。这段代码里没有看到移动、扩缩或进出世界时清掉 `anchor` 的回调。

## 看向和位置会不会随份、随段变

`buildSegmentRunner` 在任务真正执行时读 `player.getLookAngle()` 和 `player.position()`，再传给 `runSegment`。`buildSegmentRunnerAt` 使用构造时传入的向量，执行时不再读玩家。`commitHostGroup` 在循环前取一份 `sharedLook` / `sharedPlace`；参数为空时才当场读一次，然后各份共用。

`findTarget` 不用这些参数。它在查询当时另读 `player.getEyePosition()` 和 `player.getLookAngle()`。`runSegment` 开头还可以用 `contactOrigin` 换位置、用足够长的 `contactLook` 换视线。`dedicatedAim` 且锁定到目标时，再用眼睛（或接触点）到目标包围盒中心的方向覆盖视线。宿主组随后 `freezeLook` 的是这份已经覆盖过的视线。后面的份若 `useFrozen`，`CastFrame` 用冻结视线，并且不再做锁定查询。

`CastFrame` 的中心经常另读 `player.getEyePosition()`，交错时另读 `player.position()` 和 `getBbHeight()`。接触覆盖会把 `frame.origin` 设成已经替换过的 `position`。

## 现有缓存和失效

锁定候选没有缓存。每次 `findTarget` 都调用 `getEntitiesOfClass`。`MARKS` 是按玩家 `UUID` 存实体 int id 的静态 `ConcurrentHashMap`，不是空间索引。`getMarkedTarget` 用 `level.getEntity(id)`，不是范围查询。查到空、不是 `LivingEntity` 或 `!isAlive()` 时删掉这条标记。移动、扩缩、跨 section、加入和离开都没有订阅者去删它。读过的代码里也没有停服清理。

`ownedBy` / `anyOwnedBy` 每次都扫 `level.getAllEntities()`。`getAll()` 来自 `EntityLookup.byId`，类型是 `Int2ObjectLinkedOpenHashMap`。这个 fastutil 类的遍历契约不在已读的 Minecraft 源码里。`anyOwnedBy` 遇到第一把匹配剑就返回。`ownedBy` 收齐后按 `Entity.getId()` 升序排序。没有索引，也没有失效回调。

`SwordVortex.planWave` 在同一服务器 tick、同一主人下，第一把已经到发射时刻的剑调用一次 `ownedBy`，结果放进静态 `LAUNCHING`。同一 tick 后续剑复用这组 id。`plannedTick` 变化时清空整个表。没有读到停服清理。

`getAllEntities` 和范围查询不是同一个集合。范围查询走 section 存储；`getAllEntities` 走 `startTracking` 之后的可见查找表。section 尚不可访问时，实体可以已经在 section 里，但还不在 `getAllEntities` 里。

术式链计划缓存按 `docs/术式链计划缓存说明.md` 只存链结构。该说明写明锁定、落点和场地查询仍在原来的时刻读取。`CastPlanCache` 的代际清空不覆盖 `MARKS` 或剑扫描。

性能计数是采样计数，不是候选缓存。采样关闭或不在绑定服务器线程上时，`begin` / `end` / `add` 不累计。`TARGET_QUERY_CALLS` 与 `ENTITY_QUERY_CALLS` 分开；剑扫描用 `SWORD_OWNED_SCAN_CALLS` 和 `SWORD_OWNED_SCANNED`，导出说明写明不计入 `query_calls_total`。枚举注释写「缓存命中不加」，当前 `findTarget` 没有命中分支，查询始终执行，计数只在 `isRecording()` 时追加。

## 查询 API 与直接证据

| 符号 | 签名与位置 |
| --- | --- |
| 锁定查询 | `LockTargetService.findTarget`，`LockTargetService.java` 58–88 行。盒是眼睛到视线 20 格再 `inflate(8)`。谓词 `entity != player && entity.isAlive()`。 |
| 标记 | `resolve` 27–34 行写入或删除；`getMarkedTarget` 45–55 行。 |
| 段内调用 | `SkillSegmentRunner.java` 537–546 行查询并发光；570–581 行选择中心并可能再查；644–646 行 `effect.execute`。 |
| 份与视线 | 119–120 行执行时读玩家；182–199 行沿用传入向量；343–357 行宿主组共用一份；123–136 行 `HostGroupContext.freezeLook`。 |
| 另两次目标查询 | `ReleaseBatch.java` 104–132 行、135–168 行。 |
| 第二处 resolve | `UniversalLockTargetEffect.java` 21–23 行。 |
| 剑扫描 | `SwordEntity.java` 194–251 行。风暴在 `SwordVortex.java` 67–77、85–90 行。 |
| 调度 | `ChainScheduler.java` 20–30、35–58 行。波次间隔 `SkillChainPlan.java` 1000 行。 |
| 计数 | `PerfCounter.java` 8–19 行；`PerfMetric.java` 21–36 行；`PerformanceRecorder.java` 72–129、135–153 行。 |

`ServerLevel.getEntity(int)` 在映射源码 1139–1140 行，走本维度 `EntityLookup`。`getEntitiesOfClass` 不按 id 查找。

## 移动、扩缩、跨 section、加入、移除

映射来源是本机 `forge-1.20.1-47.3.33_mapped_parchment_2023.06.26-1.20.1-sources.jar`。

section 归属按方块坐标的 `SectionPos.asLong(blockPosition())`，不按包围盒。`Entity.setBoundingBox`（3076–3078 行）只赋值，不调用 `levelCallback`。

`Entity.setPos`（405–407 行）先 `setPosRaw`，再 `setBoundingBox(makeBoundingBox())`。`setPosRaw`（3593–3608 行）仅在坐标变化时更新位置并调用 `onMove()`。坐标相同则不调用。

`Callback.onMove`（375–392 行）只在 section 键变化时：从旧 section 移除、空则删 section、新 section 加入、按可见性切换追踪和 tick、可访问时 `onSectionChange`，然后 `ForgeHooks.onEntityEnterSection`。同一 section 内的移动仍然会进入 `onMove`，但键相同就返回。跨 section 发生在新包围盒写入之前，转移当时盒子仍是旧的。

`LivingEntity` 不覆盖 `setPos` 或 `setBoundingBox`。`getDimensions`（3362–3363 行）在非睡眠姿态用基类尺寸乘 `getScale()`。姿态同步走 `Entity.onSyncedDataUpdated`：`DATA_POSE` 调用 `refreshDimensions`（3011–3030 行）。该方法读取尺寸事件、写 `dimensions`，再 `reapplyPosition` → `setPos`。位置不变时 `setPosRaw` 不调用 `onMove`，随后只换包围盒，section 不变。潜影贝是例外：附着面同步时只调用 `setBoundingBox(makeBoundingBox())`（`Shulker.java` 475–477 行），没有 `onMove`。

非 Living 的命中体移动不是同一条路径。`AbstractArrow` 飞行用 `setPos`（280 行），位置回调和包围盒一起更新；打中方块时只 `setPosRaw`（439 行），该方法不重写包围盒。挂画类 `setPos` 不调用 `Entity.setPos`，`recalculateBoundingBox` 先 `setPosRaw` 再写自定义盒子。本仓 `TechniqueHitEntity` 在弧快照同步（209–218 行）和 `resizeVolume`（894–906 行）直接 `setBoundingBox`；按上面的 `Entity.setBoundingBox`，这两处不调用 `onMove`。同文件另有 `setPos`，那条会走 `setPosRaw`。

加入：`ServerLevel.addFreshEntity` → `addEntity`（826–884 行）。未移除时 `addNewEntity`。`EntityJoinLevelEvent` 若被取消，函数返回 false，实体不进 section。未取消时：登记 UUID、按当前位置放入 section、安装 `levelCallback`、`onCreated`（服务端实现为空）、section 可访问则 `startTracking`（先进入可见查找表，再 `onTrackingStart`）、可 tick 则加入 `entityTickList`。玩家加入用 `addNewEntityWithoutEvent`，事件在那之前单独发送。

移除：`Entity.remove` → `setRemoved` → `onRemove`（428–448 行）。顺序是 section 移除、停止 tick、可访问时 `onTrackingEnd`（此处发送 `EntityLeaveLevelEvent`，然后才从可见查找表删除）、需要销毁时 `onDestroyed`、去掉 UUID、回调换成 `NULL`、空 section 删除。

因此，新进入区域的实体要等加入路径把它放进可访问 section 之后，下一次范围查询才会扫到。只保留上一次候选列表、不再走这次 section 遍历，不会经过这条加入路径。移动后的可见性取决于实体所在 section 是否落在查询窗口里，以及查询当时的包围盒是否相交；只 `setBoundingBox` 不会改 section。

## 服务端绑定和停服清理的现有写法

两套都在 Forge 总线，没有 `Dist` 限制：

- `PerformanceEvents.onServerStarted` 取 `getRunningThread()`，空则用当前线程，调用 `bindFreshServer`：先关采样并 `releaseMemory`，再保存服务器和线程。`onServerStopping` 只在事件服务器与绑定实例相同时清空并丢掉引用。`isRecording` 还要求当前线程就是绑定线程。tick 开始优先级 `HIGHEST`，结束 `LOWEST`。类注释写明同优先级先后不保证。
- `CastPlanCacheEvents` 在 `ServerStartedEvent` 调用 `bindServer`：保存事件服务器、`Thread.currentThread()`、当前代际，并新建计划表。`ServerStoppingEvent` 在绑定服务器为空或就是该服务器时清空并置空。

`LockTargetService.MARKS` 和 `SwordVortex.LAUNCHING` 没有对应的启动绑定或停服清理。

## 本仓 mixin 名称与放置

`src/main/resources/xueximod.mixins.json` 没有 `server` 数组。包名 `com.bslx.XueXiMod.mixin`。公共 `mixins` 在专用服和客户端都会应用；`client` 数组是客户端项。插件 `FireHurtMixinPlugin.shouldApplyMixin` 恒为真，只给 `FireResourceHurtMixin` 做额外 `preApply`。

和实体移动有关的公共 mixin：

- `EntityMoveMixin`：`Entity.move` 头部改位移参数，返回时再调一次。
- `PrisonEntityMixin`：可取消 `move`；把 `setPos` 里的 `setPosRaw` 重定向到锚点；另有传送、骑乘和跨维注入。
- `EntityBarrierMixin`：注入 `canCollideWith`。

没有 mixin 挂在 `setBoundingBox`、`levelCallback`、section 存储或实体加入/移除上。

Forge 事件订阅者里，加入/离开包括 `AttributeStateEvents`（服务端 Living 的加入）、`PrisonLifecycle.onJoin`、偏转与屏障追踪，以及若干客户端离开处理。它们不是空间索引的失效回调。现有服务端放置方式是：生命周期放在不引用客户端类、不限制 `Dist` 的 Forge 订阅者上；需要两端都进的注入放在公共 `mixins` 列表。

## 实际读过的文件

本仓：`记录.md`，`docs/术式链计划缓存说明.md`，`LockTargetService.java`，`SkillSegmentRunner.java` 中段执行与角色扫描，`SwordEntity.java` 的拥有扫描与悬停取标记，`SwordVortex.java`，`PerfCounter.java`，`PerfMetric.java`，`PerformanceRecorder.java` 的计数、绑定和释放，`PerformanceEvents.java`，`CastPlanCache.java` 的绑定与查找，`CastPlanCacheEvents.java`，`xueximod.mixins.json`，`FireHurtMixinPlugin.java`，`EntityMoveMixin.java`，`PrisonEntityMixin.java`，`ChainScheduler.java`，`HostGroupContext` 的冻结视线，`UniversalLockTargetEffect.java`，`FieldZones.influence`，`FieldSpawner.releaseAttached`，`ReleaseBatch` 的两处目标查询，`SkillChainPlan` 的发射循环与 `DUAL_STEP_TICKS`，`SkillUsePacket` 的修饰跳过和排程调用点，`SkillSegmentMath` 的修饰判断与 `anyOwnedBy` 调用，`ReleaseDefinitions` 的锁定注册，`TechniqueHitEntity` 的 `setBoundingBox` 两处，`PrisonLifecycle` 与 `AttributeStateEvents` 的事件签名。

映射源码：`EntityGetter`、`Level` 的 `getEntities`、`LevelEntityGetter`、`LevelEntityGetterAdapter`、`EntitySection`、`EntitySectionStorage`、`EntityInLevelCallback`、`PersistentEntitySectionManager`、`EntityLookup`、`LevelCallback`、`SectionPos` 打包、`ClassInstanceMultiMap`、`Entity` 的位置/包围盒/移除/`refreshDimensions`、`LivingEntity` 的 `isAlive`/`getDimensions`/同步数据、`ServerLevel` 的加入与 `EntityCallbacks`、`AbstractArrow` 的 `setPos`/`setPosRaw`、`HangingEntity` 的盒子重算、`Shulker` 的附着面同步。

## 缺口

- 没有读 fastutil `Int2ObjectLinkedOpenHashMap` 的源码，`getAllEntities` 的遍历契约未用 Minecraft 源码证实。
- 没有逐个读 `effect.execute`、`FieldZones.add`、`ForgeHooks.onEntityEnterSection` 和尺寸事件类的方法体。
- 没有证实实体 int id 是否会复用。
- `LivingEntity.isAlive` 的子类覆盖没有逐个读。
- 本任务禁止编译、测试和启动游戏，这些都没有做。

本次只读了源码和 sources jar，没有写文件，也没有留下后台任务。
