我先把任务说明全文读完，再按里面的范围执行。任务是只读事实定位：优化、配置和生命周期。我先读指定文档和技能说明，再核对相关 Java 入口。调查 B 是只读事实，不是架构方案，也不是正式复审。没有改文件、没有编译、没有测试、没有启动游戏。下面区分当前代码事实和建议。

## 可复用入口和不能拆的边界

仓库里没有名为「空闲休眠」的现行说明。`docs/开发记录/空闲与复活性能优化-20261030/记录.md` 记的是客户端静止斩面、无动作时绕过快照、领域移动空列表；该记录写明设计 1/2、实现 1/3，编译通过，游戏未测。服务端「休眠」只出现在性能口径里：`HIT_ENTITY_UPDATES` 与 `SWORD_UPDATES` 含等待激活和插地召回空转，不是活跃实体数。

**候选查询。** 入口是 `LivingCandidateQueries.entityQuery` / `targetQuery`（`LivingCandidateQueries.java` 178–183 行）。只缓存同一服务端 tick、同一 `ServerLevel` 身份下的 `LivingEntity` 引用。键是世界对象再加覆盖 AABB。默认外扩 8 格；任一轴超过 48 格则覆盖盒退回查询盒。每世界最多 4 块覆盖，每块最多 256 个候选。超限不入库，并在本 tick 把该区域记为拒绝区。钩子未齐、未绑定、线程不是绑定的服务端线程、坐标非有限时，直接走 `getEntitiesOfClass`。

失效来自 `LivingCandidateEntityMixin`（Living 的 `setPosRaw` 真位移、包围盒真变化）和 `LivingCandidateSectionMixin`（section 加入/离开、可访问性变化）。攻击体和飞剑不是 Living，普通移动不冲掉缓存。`LivingCandidateEvents` 在 `ServerStarted` 绑定、`ServerStopping` 释放、tick START 最高优先级和 END 最低优先级清候选、世界卸载丢该世界。代际变化时正在构建的覆盖不入库。谓词抛出不吞掉。`CANDIDATE_QUERY_LOOKUP` 包含内部真实查询，不能与 `TARGET_QUERY`、`ENTITY_QUERY` 相加。调用方不要再包一层查询计时。

**术式链计划缓存。** `CastPlanCache` 是包内类。`lookup(MinecraftServer, Skill)` 在 `SkillUsePacket` 约 275 行调用。未绑定、服务器对象或线程不符、链长超过 64、任一已登记定义的 `supportsStructuralPlanCache()` 为 false、或编译超出 16384 单元时返回 null，释放走旧路径。条目上限 64，超出淘汰最旧。键在 `CastPlanKey`（131–180 行）：技能类型、术式 id 原顺序、各位置原胚、`ChainStructureEpoch`、以及 `capabilityStamp`（角色序数、弹道序数、`declaresBatchQuote`）。现行说明只写了前四项；代码多了这个戳。戳变了是另一条键，不会因此清空整表。代际变化会先 `plans.clear()`。代际是进程内 `AtomicLong`，重进服不归零（`ChainStructureEpoch.java` 6–20 行）。`ReleaseRegistry.register`、`TechniqueManager.register`、`EnchantmentRegistry`、`TechniqueEffectRegistry` 以及 `WaterReleaseDefinition` 的公开写方法会 `changed()`。停服 `releaseServer` 清空并丢掉 map。正在进行的释放继续拿手里那份分段图；说明写明代际中途变化后，原先要重读的角色、份数、锁定和垂落改回动态读取。默认 `ReleaseDefinition.supportsStructuralPlanCache()` 返回 false（429–430 行）。当前显式返回 true 的是 `ReleaseDefinitions` 的内置匿名实现、`WaterReleaseDefinition`、`SelfStatusDefinition`。未知定义整链不进缓存；未登记 id 可以记住「没有定义」。费用、伤害、冷却、武器、视线、随机数不进缓存。

**纯计算。** 只认 JVM 参数 `-Dxueximod.compute.backend` 与 `-Dxueximod.compute.library`，不进模组配置（`ComputeCodes` 59–65 行）。默认 Java。`backend=native` 且握手版本为 1、能力位打开时，现有单次计算以 `count=1` 进 JNI（`ComputeSelection.liveRoute`）。公开批量入口仍要求至少 8 条，少了继续 Java。同一线程 native 缓冲最多嵌套 4 层（`ComputeLiveDispatch.FRAME_LIMIT`），再往里走 Java。轨迹函数回调、体积贴合、方块射线、实体查询和随机数留在 Java。三维交错和一分为三没有接入。采样关闭时不读 `nanoTime`。`COMPUTE_BACKEND` 不能与 `MOTION`、`GEOMETRY`、`NARROW_PHASE` 相加。说明写明 DLL 未装进游戏 JVM，没有数值对照。

**性能记录。** 热路径只有 `PerformanceRecorder.isRecording`、`begin`、`end(PerfMetric, long)`、`add(PerfCounter, long)`。关闭或当前线程不是绑定的服务端线程时，不读 `nanoTime`、不计数。`begin` 包住原逻辑，异常继续抛。命令 `/shuperf` 权限等级 2，只在服务端（`PerformanceCommands.java` 16 行起）。状态机是 IDLE、ARMED、RECORDING、STOP_PENDING、STOPPED。`ServerStarted` 强制回到 IDLE 并丢掉上一局内存结果。观测 tick 是 Forge `ServerTickEvent` START 最高优先级到 END 最低优先级的墙钟，不含 tick 之间的等待，也不是原版完整 MSPT。业务耗时都是 inclusive。`query_calls_total` 只等于两次真实 `getEntitiesOfClass`。剑的 `getAllEntities` 用 `SWORD_OWNED_SCAN`，不进查询次数。首版没有活跃实体总量，也没有休眠与活跃分开的计数。新分身若需要计数，要新增枚举项；不能把现有更新次数当成实例数。

**服务端等待激活，不是跳过 tick。** `TechniqueHitEntity.holdActivation` 把 `pendingActivation` 设为 true（476–478 行）。服务端 `tick` 在 341–342 行直接返回，但 `finally` 仍记 `HIT_ENTITY_UPDATE` 和 `HIT_ENTITY_UPDATES`。`SwordEntity` 插地且 `recallIdle` 时只调用 `tryRecoverOwner` 后返回（473–476 行），同样计入 `SWORD_UPDATES`。`PhantomFactory.spawn` 生成现有命中体后总是返回 `Dormant.INSTANCE`，`active()` 为 false，类注释写明不做分身（`PhantomFactory.java` 4–31 行）。

## 平衡配置

目录按正式术式 id。`BalanceRegistry.canonicalize` 要求 `[a-z0-9_]{3,128}`，再经 `TechniqueIds.canonicalId`。目前唯一别名是 `water_open_ring` 收成 `water_blade`。

封存顺序在 `XueXiMod.commonSetup`：`ModTechniques.registerAll()`，`BalanceCatalog.noteBuiltins`，投递 `RegisterXueXiContentEvent`，然后 `BalanceCatalog.sealAfterContent()`。后者对 `TechniqueManager.getAll()` 补标准键，再 `ModTechniques.declareSpecialBalances()`、`EnchantmentDefinitions.declareBalance`，最后 `BalanceRegistry.seal(registered)`。`seal` 要求每个已登记术式都有 `cooldownTicks` 和至少一个魔力单位键；声明了未登记 id 会抛「声明了未登记术式」（`BalanceRegistry.java` 89–131 行）。同一键再声明直接拒绝。

因此底座参数若要进 `techniques`，必须先有正式 `Technique`，不能造一个只存在于 JSON 的 id。外部入口是 `RegisterXueXiContentEvent.registerBalance(TechniqueBalanceSpec)`：失败返回 false 并记日志，不另建目录；标准键仍由封存补齐。没有非术式的 service 配置表。全局只有 `GlobalBalance` 的 `damageMultiplier`、`manaMultiplier`、`cooldownMultiplier`，范围 0 到 1000，默认 1。术式对象里声明 `manaMultiplier` 或 `cooldownMultiplier` 会被 `BalanceKeys.checkDeclarable` 拒绝。同表还拒绝 `entityCount`、`copyCount`、`replicationCount`、`chainRole`、`settlementOrder`、`hitDedupe`、`geometryEnergy` 等结构键。

读取签名：

- `BalanceService.damageContribution(String techniqueId, RawDamage rawDamage)`，以及带 `BalanceSide` 的重载
- `BalanceService.globalDamageContribution(double rawAmount)`，只乘全局伤害一次
- `BalanceService.manaCost(techniqueId, parameterKey, rawDefault)`
- `BalanceService.cooldownTicks(techniqueId, rawDefault)`
- `BalanceService.number` / `intValue` 不乘全局倍率；对魔力单位或 `cooldownTicks` 抛出，避免漏乘

`RawDamage.of` 只接受有限非负数。`ConfiguredDamage.plus` 与 `timesHitCount` 不再读倍率。`retained(float)` 给已保存金额。`number(id, "damageMultiplier")` 只返回该术式自己的倍率。

两侧快照分开。`install` 要求目录已封存且快照代数相同。未绑定线程在两侧都已安装时，`current()` 抛 `BalanceSideException`。集成服两侧各自 `bind`。加载在 `ServerStarting` 的 `BalanceLifecycle.loadWorld`：缺模板则按封存声明写出 `defaultconfigs/xueximod-balance.json`，缺世界文件则复制到 `serverconfig/xueximod-balance.json`，已有世界文件只读。整份损坏则装代码默认，不改原文件。单字段非法只回该字段默认。登录时 `BalanceLifecycle.sendTo`，包号 15，通道协议 `"2"`，载荷是已接受的覆盖值。客户端 `applyClientPayload` 用同一封存目录再解析。`BalanceClientLogout` 断线只 `clear(CLIENT)` 并继续 `bind(CLIENT)`，不清服务端。服务端在 `ServerStopped` 的 `unloadWorld` 清掉。世界开着时改磁盘，这一局不变。指南写明游戏未测，本版不热重载。

`Technique.baseDamage()`、`manaCost()`、`getBaseCooldown()` 仍是原始结构值。调整伤害倍率不改默认耗魔。

## 主人索引、生命周期、队列

没有主人到实体的索引。`SwordEntity.anyOwnedBy` 与 `ownedBy`（221–278 行）每次都 `level.getAllEntities()`。参数为 null 时直接返回，不扫描。扫描计入 `SWORD_OWNED_SCAN`。`anyOwnedBy` 找到即停，计数只含已访问实体。`BatchFlightBook.releaseArmedTrail`（431–440 行）在卸下载体时同样全表找 `BatchCarrierVisual`。

`HostCast.OPEN` 的键是释放 token，不是主人。`closeOwned` 复制 values 再按主人关闭；表空时不扫世界。`HostCastEvents` 在服务端启动和停止时 `closeAll`，登出时 `closeOwned` 并清 `CarrierWatch`、`WaterBeatLedger`。换维、玩家死亡、死亡克隆只清节拍，不关父释放。

`ChainScheduler` 是无上限的静态 `ConcurrentLinkedQueue`。到期或已过点就跑 `Runnable`。每个服务端 tick END 都走队列；空队列不扫实体，但监听器仍在。类内没有停服清空，也不核对任务属于哪一台 `MinecraftServer`。同一 JVM 再开服时，旧任务仍可能按新服务器的 `getTickCount()` 到期。这是跨服残留的现成事实。

有上限、且按主人清理的是动画账，不是实体索引：`GroupReplayLedger` 每个主人最多 256 条、默认保留 1200 tick。`ActionRuntime.finishExpired` 只遍历已有会话；没有会话就不扫玩家。死亡、离线、换维、离开关卡走 `retire`，停服 `releaseServer` 清会话和账。`ActionRuntime` 的静态表在停服前仍是进程级的。

`DeflectionHosts.onServerStopping` 清宿主表；`onLevelUnload` 只摘该 `ServerLevel`。`PrisonLifecycle` 处理登出、换维、停服，死亡克隆只在非死亡克隆时搬潮湿。`ModEvents` 停服取消 `CastExecutionSession`、清领域脉冲、结束冷却会话，并调用偏转宿主停服。玩家登出另 `MagicPool.forget`。这些都是各系统自己的静态表，没有统一的「主人实体登记处」。

新分身零实例时：以空 map 做 `get` 不会扫世界；若照 `SwordEntity.ownedBy` 写，即使没有分身也会扫该维度全部实体。候选查询和计划缓存都是调用才工作，没有实例、没有查询就不会扫。实体引用放进静态表时，现有可跟随的边界是：表按 `MinecraftServer` 或 `ServerLevel` 对象身份绑定，启动清空、停止丢引用，换服不沿用上一局。`ChainScheduler` 目前没有这条边界。

## 工作区进程和编译前收束

本次只看进程和源码时间，没有读其他会话，也没有结束任何进程。快照时全机匹配到的 Java 只有 pid 66116：`java.exe`，JDK 17，2026-11-02 02:06:34 启动，命令行含 `GradleDaemon`，不含 `alianximod`，也没有 `compileJava` 或 `gradlew` 客户端。`D:\alianximod\.gradle\8.8` 的 checksum、executionHistory、fileHashes、buildOutputCleanup 锁的时间是 02:57:01。`src` 最新文件是 `TechniqueHitSpec.java`，02:52:38.160。不能据此断定其他 Cursor 会话已经停写，只能说这一刻没有对着本仓库路径的编译进程。

建议，不是已执行的步骤：等相关作者声明停写后，再对 `src/main` 做一次哈希；编译开始前最后一次源码写入必须早于编译，编译后再比，差异应为 0。同一工作区不要并行开第二份 Gradle。项目约定是 `.\gradlew.bat compileJava --offline --no-daemon`。遗留 daemon 66116 是否停掉要另得授权。这次调查没有编译。

## 已知未决

指南、候选查询、纯计算、计划缓存的现行说明都写明游戏或性能收益未测。计划缓存说明的键比代码少 `capabilityStamp`。`ChainScheduler` 无容量、无停服清空。性能记录没有分身数量或休眠/活跃拆分。`PhantomFactory` 不是分身底座。平衡没有「非术式 service」槽；未登记 id 在封存时会被拒绝。
