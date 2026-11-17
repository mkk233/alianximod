我先读取这份任务说明，按其中授权范围执行。设计范围已经明确。我先按文件名定位这八个源文件，备份后再按冻结接口插桩。八个文件都是唯一路径。我先把它们原样备份并记下哈希，再按冻结接口插桩。备份已完成。接下来只在这八个文件里按冻结接口加计时和计数，不改原来的查询、随机和结算顺序。定向核对已经对上：查询次数和原来的调用点一致。我现在把最终报告和证据写进本目录。B 的释放链插桩已经写完，八个源文件的所有权释放。设计裁决仍是 1/2，实现正式复审 0/3。没有追加复审，没有编译，没有启动游戏。

公共接口只按冻结契约引用，没有改接口，也没有补建 A 的类：`com.bslx.XueXiMod.debug.performance.PerformanceRecorder` 的 `begin()`、`end(PerfMetric, long)`、`add(PerfCounter, long)`、`isRecording()`。每处都是先声明 `long`，原逻辑放进 `try`，`finally` 里 `end`。早退和原异常仍走原来的路径。`isRecording()` 只包住计数；关闭时不调用 `size()`，也不再执行一次查询、随机或扣费。候选数用原 `getEntitiesOfClass` 返回列表的 `size()`，谓词未改。这些耗时都是 inclusive，嵌套可以大于墙钟，不能相加。

## 覆盖位置

`RELEASE_REQUEST`：`SkillUsePacket.handle` 的 `enqueueWork` 业务体，103–124 行（`begin` 104，`end` 122）。空玩家、松手和按住的早退都在内。`setPacketHandled` 在 125 行，留在网络线程。

`CHAIN_SPLIT`：`TechniqueChain.split` 20–46 行（21 / 44）。

`SEGMENT_EXECUTE` 与 `CHAIN_ROLE_SCAN` 嵌套。`SkillSegmentRunner.runSegment` 365–617 行是段执行体（373 / 615），含早退、蓄力排程、锁定、落点、几何和 `effect.execute`。角色扫描是其中 444–473 行的原向前循环（444 / 472）。原 `finally` 顺序仍是先解除 `CastFrame` 与 `PartingOffers`，再恢复 `ReleaseBatch`、重水和宿主组，最后才结束段计时。蓄力 lambda 里的 `ChargeGate.disarm` 未改。

`SCHEDULED_ACTION` 只包两处真实 `action.run`。当场执行在 `ChainScheduler.schedule` 21–28 行；到期执行在 `onServerTick` 48–57 行，`end` 在内层，原 `catch` 仍在外层并照旧记 `DebugLog`。`schedule` 的入队不在计时里。同步重入时每一层真实 `action.run` 各记一次。

`TARGET_QUERY` 以及 `TARGET_QUERY_CALLS`、`TARGET_CANDIDATES`：

- `LockTargetService.findTarget` 58–89 行，整段搜索含距离循环。`resolve` 的标记写入和调用方发光在此之外。
- `ReleaseBatch.crosshairCenter` 108–131 行，从 `level.clip` 到选最近生物。每次调用都查，射线时间留在 `TARGET_QUERY`。
- `ReleaseBatch.resolveAnchor` 145–167 行，只包 `wantAnchor` 时的首次实体搜索。136–138 行缓存命中直接返回。141–144 行不需要锚点时仍只写眼前 4 格，没有世界查询。

这三处 `getEntitiesOfClass` 各加一次调用计数，并累加该次返回列表的 `size`。

`GEOMETRY` 三处同枚举，重叠照实存在：`CastFrame.forAttack` 90–120 行，`volumePoint` 203–217 行，`aimFor` 219–231 行。`projectileGeometry` 在 184–185 行调用的 `volumePoint(0)` 和 `aimFor(0)` 嵌在 `forAttack` 内。斩击不进入这两个方法。龙卷的 `vortexPhase(0)` 含在 `forAttack` 内，本身没有单独指标。随机仍各求值一次。

`CONTACT_ENCHANT_TICK`：`ContactEnchantState.tick` 54–77 行，含空服务器和奇数 tick 早退。奇数 tick 不扫描，也不调用 `settlePending`。偶数 tick 仍先 `settlePending`，再 `end`。

`ENTITY_QUERY` 以及 `ENTITY_QUERY_CALLS`、`ENTITY_CANDIDATES`：只包 `scan` 里 98–102 行的原查询。消费循环从 108 行开始。谓词仍是 `LivingEntity::isAlive`。领域分支在查询前返回，本文件不加实体查询计数。`livingTargets` 先置 `null` 只为通过赋值分析；成功路径在读取前写入查询结果，查询抛错时异常照常向外传播。

`TALLY_RECORD`：真正入账的 `ReleaseBatch.Tally.record` 201–233 行。188–199 行三个转发重载不再各包一层，一次玩法入账对应一次样本。`TALLY_FLUSH`：`flush` 243–286 行。当前 `FLUSH_DELAY` 为 6，结算排在之后的 tick，经 `SCHEDULED_ACTION` 执行，不在这次 `record` 里。

## 保留的语义

查询、`clip`、随机、扣费、`schedule` 的当场或延后、Tally 改期、接触附魔的偶数 tick 和 `settlePending` 条件都保持原样。开关不控制业务本体。双重释放间隔等与旧文档不一致的常量没有改。`CastExecutionSession` 和 `SkillChainPlan` 没有加计时。

## 未覆盖

本组没有插入 C 负责的命中体、剑、窄判定、方块裁剪、生成、结算、伤害和显式发送。按住重复在后续 tick 直接调用的 `offer`、`gapBefore`、`getMarkedTarget`、`resolve` 的标记与发光、锚点缓存命中、不需要锚点时的 4 格回退、`DomainContactBilling.collect` 在其他类中的查询、三个 `record` 转发重载，都留在对应区间外。`DebugLog` 原调用点仍在若干区间内。没有超出冻结设计的阻塞。

## 检查与哈希

命名门禁退出码 1。这八个文件没有新违规。未改文件 `OriginalHumanoid.java` 165 行 `i`、202 行 `to` 仍是旧违规，没有顺手改名。未运行 `gradlew`，未做游戏实测。统一编译和游戏实测都还没做。

备份在 `docs/开发记录/性能记录-20261030/before-B/`。SHA-256 如下。

| 文件 | 原 SHA-256 | 现最后写入 UTC | 现 SHA-256 |
| --- | --- | --- | --- |
| SkillUsePacket.java | 3BF4BC544CA3F3F5D27F32118DBFA34CBE7F3095798D995BF953F645137C07D3 | 2026-10-29T20:03:53.8140816Z | CD687E039956C02BE1142949844E5D6B8B0C34F33BA6EF9BC7149DFB47317EB2 |
| SkillSegmentRunner.java | C555BF9C6DD5082F51CA6C8BC56B95C453E4DEEB273598F9A5052BD75F211EB6 | 2026-10-29T20:04:33.7144074Z | 334F936B45C2D44DC628C574CFEB5A5B0D8E1C000B7C89B0AB096E447DE8AA27 |
| TechniqueChain.java | BA04C8FE248B8F07F661B5273D310DB7BE94E2A582EE4D9C7D9C86D287FEF0C5 | 2026-10-29T20:03:55.0664489Z | F152C5089B4B197E1434C665A60A5EAF01D0B5BFAD06CC73B304CEDF580D962F |
| ChainScheduler.java | A01AD8E30A671D203B47767A4290675C8DEDCA57E2C83B8790852670023E8578 | 2026-10-29T20:03:57.5618710Z | 2CF06EDEEF34A8B486AA019F71895FEFF9F763BC5AB366EE649686A0488AD309 |
| LockTargetService.java | 9DBE37B9C4E026B6C17FC3FB6107D0C544365A49F613F30B3513A55874AC780C | 2026-10-29T20:04:00.8239153Z | 7517E05245488AB1271B6F263DDBA88BCAB3DBAEFA4C1EA31FFBFC7EA49FB027 |
| ReleaseBatch.java | 382015A6071CA0204BF3CF343D230E7AD374A5C150FE371D9472203869C316A7 | 2026-10-29T20:04:20.4469837Z | 6DAA8141C265012DB58F7AB76ED7CB40B3BA4869020862E7F2DD03E02881F1FD |
| CastFrame.java | F7C60E01EC9295F3C0EE24B31029A614AB5F80ECD3468A81366B81E2B6FC0C53 | 2026-10-29T20:04:23.8162227Z | 58C587F4E7530B3510CB66028BB72000E89353E2498D327F22BCCE5E17CBF8FB |
| ContactEnchantState.java | A0771E42AFCACA617A7B6F9699306E0B1C2AF35546A2C0F9313D53A1174BB152 | 2026-10-29T20:05:43.0251843Z | A3DDC72B68EAB6A711D19C198E34C4789F449F22BB991A193C70EAC0975E6309 |

报告文件：`implementation-B-final.md`，2026-10-29T20:09:00.0241261Z，`6FB253E96E0AE19781E3EFF4B2FE8ADD2590C1DA47C88125CD4AD459FC75972F`。`implementation-B-evidence.json`，2026-10-29T20:09:16.1889644Z，`60DAA77404B29510B8776B47B0E2454DAB24DD9BD3307A3C443F3853AD0D7100`。

本作者全部写入到此结束，上述八个 Java 文件的所有权释放。
