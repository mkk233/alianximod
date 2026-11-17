我先读取这份任务说明，确认授权范围后只采集它点名的入口事实。另一份报告和上次结论冲突，我按三条入口的实际调用核对切步，并核对调度器与领域 tick 的先后。切步发生在按键循环之前。雾雨后的垂丝不会在同一次普通按键里放出；双重会把雾雨和垂丝收进同一次会话，第二段再晚 5 tick。调度器与领域 tick 没有源码上的先后。

## 1. 三个入口怎样消费下标

`ChainSteps.list` 按原始下标分组。不等宿主的段会结束当前步（`ChainSteps` 46–56）。等宿主的是索敌、弹道、一分为三、双重，或链能量倍率不是 1（163–178）。`slice` 按这些下标抽出新技能（85–92），之后段里的 `actionIndex` 是新列表下标。`executeSkill` 先取当前步（`SkillUsePacket` 257–305），再编译、再进循环。

雾雨是 `ATTACK`、伤害 0、倍率 1，自己结束一步。垂丝伤害是 2（`WeepingSettings` 8），也是宿主。一分为三等宿主。

雾雨 → 一分为三 → 垂丝因此是两步：原始下标 `[0]`，以及 `[1, 2]`。第一次按键的列表里只有雾雨。循环走到 `tryWave`（`SkillUsePacket` 572–588，脚点参数为空，盒子留在放置器给出的位置），`segmentIndex += 1` 之后没有后段。垂丝要等 `ChainProgress` 推进后的下一次按键；那次列表是切出来的一分为三和垂丝，没有双重，编译结果为空，才走普通 `firePlan`。

双重的 `pullAfterA` 是 1，`extraHostsAfter` 再留一个宿主（`ChainSteps` 130–138，49–51）。双重 → 雾雨 → 垂丝因此是一步，原始下标 `[0, 1, 2]`。`CompiledReleaseStep.chooseStructure` 见到 `RULE_DUAL` 就取后面向前两个非修饰段（68–89）。`releasedSegments` 跳过修饰，房间是 2，顺序是雾雨、垂丝。`executeSkill` 352–359 直接 `CastExecutionSession.open`，不再进入 572 行那段循环。

会话在 `startOrdinary` 用按下时的脚点和准星做一份 `ActionContact`（160–163）。`beginGroup` 立刻提交成员 0：雾雨走 `tryWaveReserved`，盒子平移到这份接触点（`CastExecutionSession` 305–307；`DomainService` 47–48）。成员 1 垂丝排在 `first.tick() + STEP_GAP`，`STEP_GAP` 是 5（45，257–258），`commitNow` 把同一接触点的位置和朝向传进去（357–361）。同一次按键受理两段，垂丝不在同一 tick。动作领先的接触才会改用命中点（174–188）；这条链没有动作，`actionLed` 为假。

所以「开域后循环继续放攻击」只在后段还留在这次 `segments` 里时成立。普通切步已经把雾雨后面的段拿掉了。双重不是靠 `segmentIndex += 1` 带上垂丝，而是会话的成员表。

`DomainService.tick` 只关闭过期领域并做潮湿、减速、水势脉冲（118–134；`DomainPulses.fire` 28–44）。它不读取技能链，也不会放出垂丝。

## 2. 会话预算不能直接当作自动子释放

`CompiledReleaseStep` 只有段下标、份额、冷却和链能量。没有取消条件、槽位、武器或几何。

`CastExecutionSession.open` 把载体、槽位和按下时的 `weaponBonus` 浮点存进会话（78–95）。载体是传入的 `ItemStack` 引用，没有复制。提交时持剑门读的是当时主手（`SkillSegmentRunner.commitNow` 233），不是那份引用。

槽位写进锁键 `sword:uuid:slot`（`CastCooldown.carrierKey` 56–61）。会话本身不拒绝 `-1`，`acquire` 也能占用这把锁。但 `busy` 对剑槽小于 0 或大于 3 直接算忙（87–88），`mark` 对这种槽直接返回（106–107），冷却记不上。法杖键不包含槽位（57–59）。

取消只有 `continues`：活着、同一维度、`ChargeGate` 代际不变（444–445）。没有额外谓词，也不读领域寿命。`runSecond` 失败或中断只结束这一组（262–274）。

几何是组里那一份接触点。普通双重在开始时取一次脚点和准星，两个成员共用。不能按波传入域内随机点。实体锁仍会按玩家当时准星再解析。

费用是 `groupBill()` 一次 `charge`（230–235，502–507）。领域份额已含 `initialCost * copies`（`CompiledReleaseStep` 244–246），第一段提交时连规则份额一起 `retain`（247）。份数被乘进一个整数，不是每份到点再收。间隔固定 5 tick，不是 20。

## 3. 同 tick 到期没有固定先后

`ChainScheduler.TickHandler.onServerTick` 与 `ReleaseBaseEvents.onServerTick` 都是 `Phase.END`、默认优先级，谁也不调用谁（`ChainScheduler` 33–40；`ReleaseBaseEvents` 173–181）。源码不能认定调度器先于 `DomainService.tick`。

`schedule` 在目标 tick 已到时当场执行（20–28）。排队任务要等自己的监听器，条件是 `runAtTick <= now`。领域在 `now - expiresAt >= 0` 的那一 tick 被关掉，然后才发脉冲（`DomainService` 130–134；`withinLife` 179–180；`ReleaseInput.hasExpired` 45–46）。排到同一 tick 的子动作可能赶在关闭前，也可能赶在关闭后。`continues` 不看这份寿命，所以子动作必须自己检查领域是否还活着。
