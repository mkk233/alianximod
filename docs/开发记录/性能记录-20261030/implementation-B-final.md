# B 实现最终报告：释放链性能插桩

作者：B，唯一作者。模型 Cursor CLI grok-4.7-high-fast。未委托子代理。阶段：实现。设计裁决 1/2 已完成，实现正式复审 0/3。本作者不自行追加复审、不跑测试、不编译、不启动游戏、不改制作表、不提交。

公共接口按冻结契约引用，未改接口，也未补建 A 的文件：`com.bslx.XueXiMod.debug.performance.PerformanceRecorder` 的 `begin()`、`end(PerfMetric, long)`、`add(PerfCounter, long)`、`isRecording()`。A 的类当前可以不存在。

八个源文件已全部写完。本作者对此八个文件的写入结束，并释放所有权：

- `src/main/java/com/bslx/XueXiMod/network/SkillUsePacket.java`
- `src/main/java/com/bslx/XueXiMod/network/SkillSegmentRunner.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/chain/TechniqueChain.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/chain/ChainScheduler.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/effects/LockTargetService.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseBatch.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/CastFrame.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/contact/ContactEnchantState.java`

同名文件各只有一处。备份在 `docs/开发记录/性能记录-20261030/before-B/`，原哈希见文末。既有未提交改动保留。没有 reset、clean、checkout。

## 插桩形状

每处都是先声明 `long`，再把原逻辑放进 `try`，`finally` 里 `end`。早退和原异常仍走原来的路径。没有用开关包住玩法。`isRecording()` 只包住计数：关闭时不调用 `size()`，也不再执行一次查询、随机或扣费。`add` 使用原 `getEntitiesOfClass` 返回列表的 `size()`，谓词原文未改，没有在谓词里计数，没有重查。

以下耗时都是 inclusive。嵌套或同枚举重入可以让累计值大于墙钟，禁止相加当成总 CPU。

## 覆盖的指标与计数

### RELEASE_REQUEST

`SkillUsePacket.handle` 的 `enqueueWork` 业务体，103–124 行；`begin` 104 行，`end` 122 行。含空玩家、松手、按住和 `offer` 的早退。`setPacketHandled` 在 125 行，留在网络线程，不在此区间。

### CHAIN_SPLIT

`TechniqueChain.split` 20–46 行；`begin` 21 行，`end` 44 行。返回仍在 `try` 内。`gapBefore` 不在此指标内。

### CHAIN_ROLE_SCAN 与 SEGMENT_EXECUTE

二者嵌套。`SkillSegmentRunner.runSegment` 365–617 行是段执行体，`begin` 373 行，`end` 615 行，包住该方法全部早退、蓄力排程、角色扫描、锁定、落点、几何、`effect.execute`，以及原有的两层 `finally`。

角色扫描是其中 444–473 行的原向前扫描循环，`begin` 444 行，`end` 472 行。循环之后的场地影响、冻结朝向和落点选择不在扫描区间内。

原 `finally` 顺序仍是：先 `PartingOffers.clear` 与 `CastFrame.unbind`（596–598 行），再恢复 `ReleaseBatch`、重水和宿主组（600–613 行），最后才结束 `SEGMENT_EXECUTE`。蓄力 lambda 里的 `ChargeGate.disarm`（409–414 行）未改；那次重入是之后的另一次 `runSegment`。

### SCHEDULED_ACTION

只包两处真实 `action.run`，不包 `schedule` 整体。

- 当场执行：`ChainScheduler.schedule` 21–28 行。`runAtTick <= getTickCount()` 时只包 `action.run()`。入队仍在计时之外。
- 到期执行：`TickHandler.onServerTick` 48–57 行。`end` 在内层 `finally`，原 `catch` 仍在外层，异常仍记 `DebugLog` 后吞掉这一处任务异常。当场分支没有 `catch`，异常在 `end` 之后照常抛出。

同步重入时，每一层真实 `action.run` 各记一次。

### TARGET_QUERY、TARGET_QUERY_CALLS、TARGET_CANDIDATES

三次实际 `getEntitiesOfClass` 各记一次调用，并把该次返回列表的 `size` 累加到候选数。射线 `clip` 留在 `TARGET_QUERY` 内，没有记入 `BLOCK_CLIP`。

- `LockTargetService.findTarget` 58–89 行，整段搜索，含距离循环。`resolve` 的标记写入和调用方发光不在此区间。
- `ReleaseBatch.crosshairCenter` 108–131 行：从 `level.clip` 到选最近生物。眼睛和朝向的准备在计时之前。没有缓存，每次调用都查。
- `ReleaseBatch.resolveAnchor` 145–167 行：仅 `wantAnchor` 时的首次实体搜索。136–138 行锚点已有则直接返回，不计时、不计数。141–144 行不需要锚点时仍只写眼前 4 格，没有世界查询，不计时、不计数；写入后的后续调用走缓存返回。

### GEOMETRY

三处同枚举，重叠照实存在，不能相加。

- `CastFrame.forAttack` 90–120 行，含不改生成点的早退，以及 `slashGeometry` / `projectileGeometry`。
- `volumePoint` 203–217 行。
- `aimFor` 219–231 行，含垂落、散射随机方向和原朝向三条返回。随机仍各求值一次。

`projectileGeometry` 在 184–185 行调用 `volumePoint(0)` 和 `aimFor(0)`，这两次嵌在 `forAttack` 的 `GEOMETRY` 里面。斩击走 `slashGeometry`，不再进入这两个方法，只有外层一次 `GEOMETRY`。龙卷在 170 行调用 `vortexPhase(0)`，时间含在 `forAttack` 内，`vortexPhase` 本身不是单独的 `GEOMETRY`。此后实体代码再调用 `volumePoint` 或 `aimFor` 时，会各自再记一次，因为计时在方法上。

### CONTACT_ENCHANT_TICK 与 ENTITY_QUERY

`ContactEnchantState.tick` 54–77 行是服务端处理体，含空服务器和奇数 tick 的早退。奇数 tick 只返回，不进入原扫描，也不调用 `DomainContactBilling.settlePending`。偶数 tick 的原 `finally` 仍先 `settlePending`（71–73 行），然后才 `end`（74–76 行）。

`ENTITY_QUERY` 只包 `scan` 里原查询表达式，98–102 行。消费循环从 108 行开始，在计时之外。查询成功后，仅当 `isRecording()` 时 `ENTITY_QUERY_CALLS` 加 1、`ENTITY_CANDIDATES` 加 `size`（104–106 行）。谓词仍是 `LivingEntity::isAlive`。领域分支在 92–94 行调用 `DomainContactBilling.collect` 后返回，本文件没有 `getEntitiesOfClass`，因此不加实体查询计数。该查询嵌套在偶数 tick 的 `CONTACT_ENCHANT_TICK` 内。

`livingTargets` 先置 `null`，只为通过编译器的赋值分析。成功路径在读列表之前写入查询结果。查询抛错时异常仍从 `finally` 之后向外传播，循环不会执行。

### TALLY_RECORD 与 TALLY_FLUSH

分开计时。

- 真正入账的 `Tally.record` 在 201–233 行，含拒绝入账、服务器为空、窗口内不再改期这三处早退。`ChainScheduler.schedule` 仍在入账末尾，当前 `FlowRoundLedger.FLUSH_DELAY` 为 6，结算排在之后的 tick，不在这次 `record` 里执行。
- 188–199 行的三个重载只转发到这个实现，不再各包一层。一次玩法入账对应一次 `TALLY_RECORD`。
- `Tally.flush` 243–286 行，含 `flushPending` 为假的早退。伤害、领域合并和原结算顺序未改。若结算回调里再次 `record`，那是嵌套的另一次入账，会另排 6 tick，两段不能加成同一次伤害。

## 保留的语义

- 每次 `getEntitiesOfClass`、`clip`、随机取样、魔力与动量扣除、`schedule` 入队或当场执行、Tally 改期、接触附魔偶数 tick 和 `settlePending` 的条件都保持原样。
- 开关只决定是否记数。关闭时业务体仍完整执行。
- 谓词、候选循环的短路、`action.run` 的次数和返回路径未改。
- 双重释放间隔等与旧文档不一致的常量没有改。
- 没有改 `CastExecutionSession`、`SkillChainPlan`，也没有给会话层另加计时。

## 未覆盖与边界

本组没有插入、也不属于这八个文件的指标：`HIT_ENTITY_UPDATE`、`SWORD_UPDATE`、`MOTION`、`SWORD_OWNED_SCAN`、`NARROW_PHASE`、`BLOCK_CLIP`、`HIT_SPAWN`、`PHANTOM_SWORD_SPAWN`、`HIT_SETTLE`、`DAMAGE_APPLY`、`EXPLICIT_SEND`，以及对应的生成、窄判定、伤害和显式发送计数。C 的实体文件未读、未改。

本组范围内明确不记的点：

- `offer` 若由按住重复在后续 tick 直接调用，不在 `enqueueWork` 内，不计 `RELEASE_REQUEST`。按下包只覆盖当场跑完的同步部分。延后链段、Tally 结算和接触附魔在各自执行 tick 另计。
- `gapBefore`、链段循环里的排程准备、宿主组一次回调里的多份 `runSegment`，都没有再套父区间。父区间已经含当场子调用时，报告里不能把它们加总。
- `LockTargetService.getMarkedTarget` 是标记读取，不是这次的 `getEntitiesOfClass`。
- `resolve` 的 `mark` / `clear`，以及 `runSegment` 里命中后的发光，不在 `TARGET_QUERY`。
- `resolveAnchor` 的缓存命中，以及不需要锚点时的眼前 4 格回退，不是世界查询。
- 领域接触的 `collect` 若在其他类里还有查询，不在本文件，本作者未跨文件补桩。
- `vortexPhase`、`slashGeometry`、`projectileGeometry` 没有单独枚举；后两者的工作包含在 `forAttack` 中。
- 原版追踪、真实网络完成、客户端渲染不在本组。`DebugLog` 原调用点仍在若干计时区间内，采样时日志开销会进 inclusive 时间。没有改动日志开关。

没有超出冻结设计的阻塞，因此没有改公共接口或调度结构。

## 编译、实测与命名检查

无编译授权。未运行 `gradlew`，未做独立测试，未启动游戏。统一编译和游戏实测都还没做。

命名门禁已跑：`powershell -NoProfile -File tools/check-name-gate.ps1`，退出码 1。本作者改过的八个文件没有报出新违规。失败的两处在未改文件 `animation/runtime/OriginalHumanoid.java` 165 行 `i`、202 行 `to`，按规则只列出、没有顺手改名。

## 哈希

算法 SHA-256。时间为文件 `LastWriteTimeUtc`。

| 文件 | 原最后写入 UTC | 原 SHA-256 | 现最后写入 UTC | 现 SHA-256 |
| --- | --- | --- | --- | --- |
| SkillUsePacket.java | 2026-10-28T18:59:04.3123427Z | 3BF4BC544CA3F3F5D27F32118DBFA34CBE7F3095798D995BF953F645137C07D3 | 2026-10-29T20:03:53.8140816Z | CD687E039956C02BE1142949844E5D6B8B0C34F33BA6EF9BC7149DFB47317EB2 |
| SkillSegmentRunner.java | 2026-10-29T04:45:16.5475473Z | C555BF9C6DD5082F51CA6C8BC56B95C453E4DEEB273598F9A5052BD75F211EB6 | 2026-10-29T20:04:33.7144074Z | 334F936B45C2D44DC628C574CFEB5A5B0D8E1C000B7C89B0AB096E447DE8AA27 |
| TechniqueChain.java | 2026-10-23T16:11:15.3382922Z | BA04C8FE248B8F07F661B5273D310DB7BE94E2A582EE4D9C7D9C86D287FEF0C5 | 2026-10-29T20:03:55.0664489Z | F152C5089B4B197E1434C665A60A5EAF01D0B5BFAD06CC73B304CEDF580D962F |
| ChainScheduler.java | 2026-09-27T08:04:09.8529151Z | A01AD8E30A671D203B47767A4290675C8DEDCA57E2C83B8790852670023E8578 | 2026-10-29T20:03:57.5618710Z | 2CF06EDEEF34A8B486AA019F71895FEFF9F763BC5AB366EE649686A0488AD309 |
| LockTargetService.java | 2026-10-19T17:49:55.0421652Z | 9DBE37B9C4E026B6C17FC3FB6107D0C544365A49F613F30B3513A55874AC780C | 2026-10-29T20:04:00.8239153Z | 7517E05245488AB1271B6F263DDBA88BCAB3DBAEFA4C1EA31FFBFC7EA49FB027 |
| ReleaseBatch.java | 2026-10-29T04:46:13.5316186Z | 382015A6071CA0204BF3CF343D230E7AD374A5C150FE371D9472203869C316A7 | 2026-10-29T20:04:20.4469837Z | 6DAA8141C265012DB58F7AB76ED7CB40B3BA4869020862E7F2DD03E02881F1FD |
| CastFrame.java | 2026-10-29T04:45:16.0972745Z | F7C60E01EC9295F3C0EE24B31029A614AB5F80ECD3468A81366B81E2B6FC0C53 | 2026-10-29T20:04:23.8162227Z | 58C587F4E7530B3510CB66028BB72000E89353E2498D327F22BCCE5E17CBF8FB |
| ContactEnchantState.java | 2026-10-29T14:05:35.0242919Z | A0771E42AFCACA617A7B6F9699306E0B1C2AF35546A2C0F9313D53A1174BB152 | 2026-10-29T20:05:43.0251843Z | A3DDC72B68EAB6A711D19C198E34C4789F449F22BB991A193C70EAC0975E6309 |

备份副本与上表原哈希一致，路径镜像在 `before-B/src/main/java/...`。机器可读副本见同目录 `implementation-B-evidence.json` 与 `before-B/original-hashes.json`。

本作者全部写入到此结束。上述八个 Java 文件的所有权释放，可供统一编译前收束。
