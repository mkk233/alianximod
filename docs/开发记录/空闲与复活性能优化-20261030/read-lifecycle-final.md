我先读取这份任务说明，确认授权范围后再按它执行。任务是只读采集复活与常驻服务的开销证据。我先定位登录、死亡、克隆和持续 tick 的入口，再核对配置与日志。空闲进世界和死亡复活这条链上，允许范围内没有测到能单独解释 40–50 FPS 的常驻大循环。`run/logs/latest.log` 里这次进世界把视距改成了 24，但没有死亡、复活或掉 tick 记录。下面是源码和这次日志能对上的事实，不是根因，也没有帧数收益判断。

## 运行参数和这次日志

开发启动和游戏堆是两套配置。`gradle.properties` 第 3 行 `org.gradle.jvmargs=-Xmx3G` 只给 Gradle。`build.gradle` 的 `runs` 没有给客户端加 `-Xmx`。`latest.log` 第 1 行的 `forgeclientuserdev` 启动参数里也没有堆大小，所以这次游戏堆没有直接证据。

`build.gradle` 第 96 行把开发控制台设成 `debug`。这次 `latest.log` 没有出现按 tick 刷屏。`[调试]` 共 59 条，从第 80 行起，都是施法或中断，空闲段没有。

视距有直接记录：

- `run/options.txt` 第 34–35 行：`renderDistance:24`，`simulationDistance:16`
- `latest.log` 第 68–69 行：集成服务器 `Changing view distance to 24, from 10`，随后 `Changing simulation distance to 16, from 0`
- 第 58–67 行：主世界出生区准备用了 3401 ms，发生在视距改成 24 之前
- 第 30 行：注册了 47 个术
- 第 71–74 行：01:11:10 玩家进入 `(147.27, 75.0, -558.87)`，约 0.6 秒后渲染器重绑一次
- 第 76–79 行：进入后约 2 秒就暂停存档，三个维度都写了
- 全文没有死亡、复活、`Can't keep up`，也没有 `[furnace]`

允许范围内的模组代码不读取 24 或 16。24 会扩大原版区块发送和实体开始追踪的范围；16 是模拟距离。这次日志没有给出加载了多少区块或实体。

`mcp` 包不存在。`block` 包没有 tick。监听器没有发现重复注册：各类靠 `@Mod.EventBusSubscriber` 各注册一次，`XueXiMod.java` 第 70 行只把主类挂上总线，第 111–113 行的开服回调是空的。

## 八个有证据的热点

**1. 视距 24 会放大「区块加载」和「开始追踪」，模组钩子本身不扫 24 圈**

`ReleaseBaseEvents.java` 第 187–191 行，每个服务端 `ChunkEvent.Load` 都进入 `WaterPartingService.onChunkLoad`。第 107–114 行先取维度存档，再 `claimPending`；待处理列为空就返回，不扫方块。

`FireVisualRelay.java` 第 132–140 行，玩家开始追踪每个生物时调用 `sendCurrent`。`BurnCarrier.java` 第 40–46 行在没有燃烧标签时仍 `new BurnLedger()`。第 152–154 行状态为空则不发包。`WATCH_REACH` 在第 29 行是 96 格，不是 24 区块。

空闲、没施法也会走这两条，因为它们跟加载和追踪绑定，不跟施法绑定。数据规模取决于这次视距下新加载的区块和新追踪的生物；日志没给数量。死亡复活会让玩家实体重新追踪周围生物，所以同一钩子会再来一轮。这是源码路径，这次日志没采到复活。

现有短路：分水待处理列为空、燃烧状态为空都不继续写世界或发包。生命周期：分水租约在存档里；燃烧显示表见热点 7。

最小建议：待处理列和燃烧标签都不存在时，不要为每个区块或每个生物分配临时账本。涉及 `ReleaseBaseEvents.java`、`WaterPartingService.java`、`FireVisualRelay.java`、`BurnCarrier.java`。

**2. 没施法时，每个服务端玩家 tick 仍做炉心、魔力和主手观察**

`FurnaceEvents.java` 第 25–37 行：服务端玩家 tick 的 START 调 `FurnaceRuntime.reconcile`，END 调 `advance`。没炉心时 `FurnaceRuntime.java` 第 15–24 行仍查持久标签，并经 `FurnaceAttributes.java` 第 44–47、85–90 行查生命、护甲、韧性三个修饰符。`advance` 在第 43–45 行因没有运行态返回。

`ModEvents.java` 第 91–99 行：每个服务端玩家 END tick 调 `MagicPool.tickRegen`。`MagicPool.java` 第 181–188 行每 tick 给该 UUID 加一；满 20 tick 才继续。第 188–209 行接着 `PlayerShuData.get`。`PlayerShuData.java` 第 57–62 行每次都新建对象并解析整份 `xueximod_shu_data`。魔力已满则不 `save`、不发 `ShuSyncPacket`。默认池是 100/100，所以空闲稳定后应只解析、不发包。这次日志不能证明池是否已满。

`HostCastEvents.java` 第 39–47 行：同一 END tick 还调用 `CarrierWatch.observe`。`CarrierWatch.java` 第 16–20 行每 tick `computeIfAbsent`。死亡、登出、换维会清这张表（`HostCastEvents.java` 第 50–79 行）。

这些都在服务端线程。`latest.log` 把服务端线程和渲染线程分开，且没有掉 tick。因此它们不能从这次日志说成渲染线程的 40–50 FPS。

最小建议：没有炉心效果且没有运行态时，不要每 tick 查三个属性；魔力计数未到 20 时不要解析整份术数据。涉及 `FurnaceEvents.java`、`FurnaceRuntime.java`、`FurnaceAttributes.java`、`ModEvents.java`、`MagicPool.java`、`PlayerShuData.java`、`HostCastEvents.java`、`CarrierWatch.java`。

**3. 分水服务每服务端 tick 走一遍全部已加载维度**

`ReleaseBaseEvents.java` 第 183 行无条件调用 `WaterPartingService.tick`。`WaterPartingService.java` 第 97–104 行对 `server.getAllLevels()` 每个维度做 `finishWaiting` 和 `observeAndExpire`。第 161–166 行会扫全部未完成分水操作；没有操作时就是空表。`WaterPartingSavedData.java` 第 39–43 行每次 `computeIfAbsent` 取出 `xueximod_water_parting`，第 152–157 行再扫已租格子。空表不会 `getBlockState`。

这次日志第 77–79 行写出了主世界、末地、下界三个维度，所以空闲时每 tick 至少进三个维度的存档对象。这不按 24 区块放大，除非租约表里真有格子。没施法且没有未完成操作时，工作就是空表遍历。

最小建议：操作表、租约表、待处理列都空时，不要每 tick 取全部维度。涉及 `ReleaseBaseEvents.java`、`WaterPartingService.java`、`WaterPartingSavedData.java`。

**4. 一条服务端 END tick 固定扇出八处，空集合也进入**

`ReleaseBaseEvents.java` 第 173–184 行每 tick 依次调用：`FieldZones.tick`、`GuardMitigation.flush`、`GuardStates.tick`、`ObstacleBodies.tick`、`DomainService.tick`、`ContactEnchantState.tick`、`WaterPartingService.tick`、`HoldRepeat.tick`。

空闲短路是各表自己的：`HoldRepeat.java` 第 51–52 行表空即返回；`GuardMitigation.java` 第 56–57 行同样；`PrisonRuntime.java` 第 297–299 行牢表空即返回，但它在另一条 `PrisonLifecycle.java` 第 21–25 行。`DomainService.java` 第 118–135 行即使没有领域，仍调用 `DomainPulses.fire`。`DomainPulses.java` 第 28–30 行每次新建两张 `HashMap`。`ContactEnchantState.java` 第 50–66 行偶数 tick 仍扫空表，并在 `finally` 里进入结算；`DomainContactBilling.java` 第 61–62 行队列空才返回。`ActionServerEvents.java` 第 25–29 行每 tick 调 `ActionRuntime.finishExpired`，第 165–166 行复制会话键集。`ChainScheduler.java` 第 29–36 行和 `HitVolumes.java` 第 71–76 行每 tick 扫自己的队列，空则马上结束。

没施法时这些表应为空，但扇出和每 tick 两次小分配仍然发生。日志没有这些路径的计数。

最小建议：八个调用前加一层「对应表都空则整段返回」，并让 `DomainPulses.fire` 在没有领域时不要分配。涉及 `ReleaseBaseEvents.java`、`DomainService.java`、`DomainPulses.java`、`ContactEnchantState.java`、`ActionServerEvents.java`、`ActionRuntime.java`、`ChainScheduler.java`、`HitVolumes.java`。

**5. 登录、死亡克隆、复活会整份复制术数据并再发两包**

登录：`PlayerConnectionHandler.java` 第 16–31 行加载术数据，扫全部术（这次日志是 47 个），解锁等级为 0 的补学，然后无条件 `save`。`DomainLearning.java` 第 16–27 行登录时再解析一次并再扫 47 个。随后各发一包 `BoundSkillSyncPacket`（4 格，有技能才写 NBT）和 `ShuSyncPacket`。`ShuSyncPacket.java` 第 56–75 行写出等级、魔力、全部已学术 id 和使用次数；解码上限 256（第 87 行）。

死亡克隆：第 35–40 行深拷贝三把键：`xueximod_bound_skills`、`xueximod_shu_data`、`xueximod_spell_storage`。收纳箱定义是 27 格（`PlayerSpellStorage.java` 第 18–19 行），克隆不逐格读，整份标签 `copy()`。死亡克隆不抄炉心和火种：`FurnaceEvents.java` 第 57–65 行清效果、运行态和属性；`FireSeedLifecycle.java` 第 34–36 行丢掉火种。`ReleaseBaseEvents.java` 第 152–155、194–202 行死亡时停连按、施法会话、场地、护体、屏障和领域。

复活：`PlayerConnectionHandler.java` 第 43–47 行再发上述两包，不再扫 47 个术。`FireVisualRelay.java` 第 118–122 行登录会重发地面火场和自己的燃烧；复活事件本身不在这里。

这是一次性的，不是每帧。包体规模约等于已学术术式数加使用次数条目，再加上最多 27 格物品标签。这次 `latest.log` 没有死亡或复活，所以「复活更明显」还没有测量。

最小建议：登录时术集合没变就不要 `save`；复活只同步客户端还没有的那份。涉及 `PlayerConnectionHandler.java`、`PlayerShuData.java`、`DomainLearning.java`、`ShuSyncPacket.java`、`BoundSkillSyncPacket.java`、`PlayerSpellStorage.java`、`FurnaceEvents.java`、`FireSeedLifecycle.java`、`ReleaseBaseEvents.java`。

**6. 每个生物加入关卡时都对潮湿和水牢票据做一次**

`AttributeStateEvents.java` 第 13–18 行：服务端每个生物 `EntityJoinLevelEvent` 都 `WetnessState.reconcile`。`AttributeStateData.java` 第 13–16 行没有 `xueximod_attribute_states` 就返回 0。有数值但没药水才会改效果（`AttributeStateService.java` 第 44–50 行）。

`PrisonLifecycle.java` 第 64–68 行对同一事件调用 `PrisonRuntime.rebind`。第 280–287 行没有对应牢就返回。在此之前 `PrisonWetDue.settleLoaded`（第 79–97 行）会取出该维度票据表；玩家不在主世界时再取主世界表，并扫全部票据找这个生物。没有票据时循环是空的。

进世界和复活会让范围内生物加入或重新追踪，次数随视距 24 增大。空闲、没施法也会发生。单次成本是一次标签查询或一次空表扫描，不是全图方块循环。日志没有实体数。

最小建议：票据表为空时，加入事件不要按生物扫表。涉及 `AttributeStateEvents.java`、`WetnessState.java`、`AttributeStateData.java`、`PrisonLifecycle.java`、`PrisonRuntime.java`、`PrisonWetDue.java`。

**7. 两张玩家或显示缓存登出时不释放**

`MagicPool.java` 第 19–20 行有两张按 UUID 的表。`forget`（第 213–215 行）只删回复计数，不删 `LAST_RECEIPT`。`ModEvents.java` 第 84–88 行只在登出调用 `forget`，死亡不调用。收据只在扣过魔力后留下一条，空闲不增长。

`FireVisualRelay.java` 第 31–33 行的 `SHOWN`、`MEMORY`、`GROUNDS` 没有登出或死亡时的整体清空。单条燃烧在结束时移除（第 176–181 行），地面火场在关闭时移除（第 83 行）。没施法且没有残留火场时，这三张表保持空；第 101–114 行仍每 20 服务端 tick 扫一次空的地面表。实体 id 若在残留记录之后复用，旧记录会留到进程结束。这次日志看不出有残留。

`CarrierWatch` 在死亡和登出会清。`HoldRepeat` 在死亡路径的 `endCast` 里清。

最小建议：登出时删掉该玩家的最后收据；没有燃烧和地面火场时跳过 20 tick 扫描，并在登出清显示表。涉及 `MagicPool.java`、`ModEvents.java`、`FireVisualRelay.java`。

**8. 客户端每 tick 仍进入热键和渲染器复查；空手全实体扫描只在按住键时**

`ModEvents.java` 第 111–113 行，每个客户端 END tick 都调用 `EntityRendererFix.onClientTick` 和 `SkillHotkeyHandler.onClientTick`。这在渲染线程。

`EntityRendererFix.java` 第 34–56 行：进世界后若三份术式渲染器都还在，每 20 tick 做三次注册表查找和三次 `containsKey`，然后返回。缺表才会复制渲染表。`latest.log` 第 74 行只有一次「已把术式实体渲染器重新绑到当前注册表」，不是每 tick 一条。渲染表怎么绑，留给负责客户端的会话。

`SkillHotkeyHandler.java` 第 32–68 行：没开界面、没按住使用键或技能键时，只读按键状态。`entitiesForRendering()` 全表扫描在第 137–148 行，只有空手、该槽有操纵剑的术式、并且对应键正按住时才走到。没施法、没按住键时不扫。

最小建议：三份渲染器确认还在后，不要继续每秒复查。涉及 `ModEvents.java`、`EntityRendererFix.java`、`SkillHotkeyHandler.java`。后两个更可能由客户端会话拥有。

## 这次没看成异常的部分

调试日志在开发环境默认开启（`DebugLog.java` 第 18、34 行），但这次只跟施法走，空闲没有洪泛。`ChainScheduler` 和命中盒只在异常时写调试日志。

炉心修复日志只在状态不一致时出现，这次日志里没有。

没有发现同一监听器注册两次。死亡上有多条不同处理：炉心、火种、施法会话、姿态，各自做各自的清理。

`block` 包没有常驻 tick。网络里的连按只在按住表非空时跑。

## 尚未采样、不能下的结论

服务端这些空表遍历跑在服务端线程。这次日志没有掉 tick，也没有帧时间采样，所以不能把它们说成 40–50 FPS 的原因，也不能估计改完能回多少帧。

视距从 10 改到 24 是日志事实。它会让原版多加载、多追踪，并让热点 1 和 6 的次数变多。日志没有区块数、实体数、追踪包数量，也没有死亡复活时刻。复活更卡，目前只是「克隆拷贝加两包同步，并且开始追踪会再来一轮」这条未测量路径。

游戏堆大小不在允许读到的启动参数里。没有采 JFR，也没有改配置。

这不是本批的正确性结论。上面八处改动会碰到的共享文件是：`ModEvents.java`、`PlayerConnectionHandler.java`、`MagicPool.java`、`PlayerShuData.java`、`FurnaceEvents.java`、`FurnaceRuntime.java`、`FurnaceAttributes.java`、`ReleaseBaseEvents.java`、`WaterPartingService.java`、`WaterPartingSavedData.java`、`FireVisualRelay.java`、`BurnCarrier.java`、`DomainService.java`、`DomainPulses.java`、`PrisonLifecycle.java`、`PrisonWetDue.java`、`AttributeStateEvents.java`、`EntityRendererFix.java`、`SkillHotkeyHandler.java`。客户端两个文件建议交给负责渲染的会话，避免两处同时改。
