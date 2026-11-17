我先读取这份任务说明，确认授权范围后再按它执行。任务是只读核验，我先把合同、设计决定和 A/B/C 最终报告及证据清单读齐。报告已对齐。接下来按契约核对 A/B/C 实际源码的接点，而不是只复述报告。这是实现第 1/3 轮的只读事实核验，不是验收结论。未跑编译、命名门、测试和游戏。下面只记源码里能对上的调用链。

## 直接问题

**松散释放把 `effect.execute` 正常返回当成已生成，并保留整份份额。** 只有 `space_cut` 在 `ReleaseRegistry.HOST_GROUP` 里。宿主组要 `published.size() == requiredSpawns` 才 `COMMITTED`（`HostGroupContext.seal`，约 172 行）。其余攻击走 `AutomaticRound.spawnMember` 的循环：`SkillSegmentRunner` 在 `execute` 返回后无条件 `automaticSpawned.set(true)`（约 741–744 行），随后 `consumeAutomaticSpawn` 为真就 `budget.commit`（`AutomaticRound` 约 271–278、175–176 行）。效果提前返回、实体一个都没放进世界，份额仍然留下。

万剑归宗的实际链是：

1. `spawnMember` 约 211–213 行先查令牌；`runSegment` 约 720–726 行在 `execute` 前再查。这里失败会直接返回，不置标记，份额留到 `finish`/`settle` 退未提交部分。`beginVortex` 走不到。
2. 令牌仍有效时进入 `SwordReturnAllEffect.execute`。剑列表为空时约 20–23 行发「没有可操纵的剑」并返回，不改剑。
3. `beginVortex` 约 272–278 行：自动来源且 `LaunchAim.spawnAllowed` 为假时，在改状态之前返回，没有失败回执。
4. 只要 `execute` 正常结束，第 2 步的标记仍会置真，整份份额 `retainShare`。同一拍里第 1 步已通过时，第 3 步的令牌通常仍有效；要在 `execute` 过程中把领域 `active` 打成假，静默返回才会和提交叠在一起。

因此：执行前令牌失效会退、不生成。空剑列表会提交，并且每 20 刻重复那句提示。`beginVortex` 的静默返回没有单独失败回执，调用方仍按成功提交。这和「没生成不提交」是同一处接线，不是万剑归宗自己多扣了一笔。

幻剑是同一机制的延迟例子。`PhantomSwordEffect` 约 30–34 行要求本轮武器副本和当前主手都 `acceptsSwordSkill`，主手还要过 `HeldSwordGate`。成员 0 与快照同一拍，两边一致。双重的第二成员在 5 刻后才生成：当时主手已能过门，但副本仍是开轮时的非剑，效果直接返回，runner 仍保留这一成员的整份份额。

生成过程中抛出，或同线程里 `close` 先 `cancel`，`commit` 都在 `spawnMember` 返回之后。已经 `addFreshEntity` 的那一成员若还没 `retainShare`，`settle` 会把整份未提交余额退掉。宿主组失败不会走这条，因为回执不是 `COMMITTED`，`AutomaticRound` 不会 `commit`。

## 1. 契约对接

`bind` 包 11 个类型已在源码树。`BindingLayout` 不引用 network、`DomainService`、`CastFrame`、`LaunchAim`。`SkillUsePacket.register` 在 `channel.registerMessage` 之前安装 `BindingStructureCompiler` 和 `AutomaticRound`（约 1208–1210 行），由 `ModNetwork.register` 调用。C 不调用 `install`。未安装时 `compile` / `executeAutomatic` 抛 `IllegalStateException`。`automatic` 缺世界、主人、两个 UUID、原点、方向、体积、目标检查、武器或令牌，或方向过短，抛 `IllegalArgumentException`；`ItemStack.EMPTY` 可以过。`weaponCopy()` 每次再 `copy`。

`DomainService.tryWave` / `tryWaveReserved` 的新重载与三处调用一致。`plan != null` 且后缀空则不放置。`LaunchAim` 六参数保存 `TargetCheck`；五参数在 `automaticLifetime` 为真时构造即失败。`CastFrame.execution` / `launchAim`、`TechniqueHitSpec` 的源点、方向、局部目标和 `launchAim` 都有字段。`ChainRole.RULE_DOMAIN_BIND` 加在枚举末尾。看到的角色 `switch` 带 `default`。几何实现只有 `AxisBoxGeometry`。静态阅读没有缺类型或对不上的公开签名。这不是 `compileJava` 的结果。

短 `define` 把显式魔力和显式装配写成 `-1`，`chainEnergyScale` 为 1。`Technique.manaCost()` 对 `ReleaseKind.RULE` 返回 0；`assemblyCost()` 对非附魔、非动作、零伤害返回 0。标识 `rule_domain_bind`，名称「领域绑定」，通用、无属性、零基础伤害，空效果，图标仍是 `other_none`。采样在跨度大于 `2.0E-4` 时用 `[min+1.0E-4, max-1.0E-4)`，上端抽不中；`clampPoint` 含两端。点仍在要求的内缩范围内。

## 2. 切步前捕获

`SkillUsePacket.executeSkill` 约 236–243 行先 `BindingLayout.compile`，失败则 `tell` 第一条并在扣费前返回。通过后才把技能换成 `projectedSkill()`，再走 `CastPlanCache` / `ChainSteps`。标记按 `ChainRole.RULE_DOMAIN_BIND`，不按 id。领域要 `domain()!=null`、基础伤害不大于 0、不是附魔。攻击要基础伤害大于 0、角色 `ATTACK`，且不是领域、护体、屏障、条件或 `SELF_STATUS`。

投影去掉从标记到被捕获攻击（双重则到 B）的闭区间，领域本身留在投影里。`originalIndexes` 与投影顺序对齐。`DomainOpening.planFor` 用当前步下标进 `chosenStepIndexes`，再映射回原始下标。切片技能的 `actionIndex` 与这一步列表同序，三处开域因此能查到同一份计划：`SkillUsePacket` 约 591–598 行、`SkillChainPlan.fireDualSequence` 约 375–397 行、`CastExecutionSession.commitMember` 约 305–324 行。有计划时后缀是 `bind:<原始领域下标>:<本次释放UUID>`。

附魔按 `TechniqueChain.split`：等宿主的规则不吞附魔，标记后到 A 之前归 A，A 与 B 之间归 B。领域前的附魔不进冻结序列。聚能只对冻结技能调用 `frozenChainInput`。领域几何不进这条乘法。现有 `sharesDualRelease` 为真时，`chainEnergyBefore` 会越过 A，所以冻结序列里 A 前面的聚能仍会乘进 B。这是旧双重扫描。

双重只认标记后、第一个非修饰宿主之前的第一处 `RULE_DUAL`，房间是 `pullAfterA+1` 且上限 2。第二个双重不扩房间，现有编译器也只取第一处并跳过后面的修饰，没有另写一句拒绝。缺第二个合格攻击时整次释放失败，原文「双重释放后面缺少第二个攻击」。夹在领域和标记之间、标记不在领域后、越过屏障等非攻击、后面没有攻击、扫描范围内的已移除规则，都在开域前拒绝整条链。多个互不重叠的「领域、绑定、子链」按领域原始下标各存一份。子段里再出现领域宿主，会因不合格攻击失败。领域前面的双重只能看到投影里剩下的领域，拉不到已捕获攻击。尾部留在投影里，成为后续按键步。

## 3. 费用

开域账单在投影之后，约 388 行 `pressCommitCost`，或手动会话的 `groupBill`。领域服务另收 `initialCost * copies`，不收子链。同一次 `tryWave` 的多份实例保存同一个 `BoundReleasePlan`；同一声明后缀在同一刻再入会直接返回真。自动轮对冻结技能另做 `GroupBudget.reserve(owner, cost, false)`。非双重用 `pressCommitCost`；双重用 `memberShares`，规则份额加在成员 0。一分为三的倍数在 `copiesBefore` / `memberShare` 里乘进这一份，循环生成不再 `charge`。余额不足返回已 `done` 的 `ClosedRound`，不生成，不关领域，不提示缺魔。费用为 0 时 `charge` 仍返回一张零收据。`reserve` 只保存这次返回值。

持剑看生成当时的主手 `HeldSwordGate`。伤害和效果用本轮 `weaponBonus` 与 `weaponCopy()`。任一份生成成功就保留该成员整份；同成员里稍后因令牌或持剑失败的份数不单独退。第二成员在第一成员失败后仍会按 5 刻调度，只结清它自己没提交的份额。手动双重仍会在第一成员失败时停整组。`cancel` 与 `finish` 用同一个 `AtomicBoolean`，`settle` 再挡一次。已 `retain` 的不退。取消使用开轮时保存的主人和那张收据。法杖已不在可找到的背包里时，旧的 `findStaff` 失败仍然不退法杖那一部分。

## 4. 周期、令牌、关闭

`PERIOD_TICKS` 为 20。下一拍初始是 `startedAt + 20`。开域那一拍不发射。无目标仍取球面单位向量并调用 `executeAutomatic`。跳过多个节拍只发当前一次，下一拍写成严格晚于现在的未来节拍。同刻先 `List.copyOf(ACTIVE)`，再按主人 UUID、实例 UUID 排序。`DomainService.tick` 先按原条件关闭死亡、下线、跨维和到期，再脉冲，再跑绑定。到期用 `ReleaseInput.hasExpired`，与 `now >= expiresAt` 同一套差值。

`LifetimeToken.valid` 同时要求 `active`、未到期、主人在线且活着、维度等于宿主世界。`TechniqueHitSpec.spawn` 在每个实体 `addFreshEntity` 前调用 `LaunchAim.spawnAllowed`。幻剑每把剑生成前也查。`close` 先把 `active` 写成假，再取消未完成句柄并清空集合。`trackRound` 只收未 `done` 的句柄；领域 tick 对快照调用 `sweepFinishedRounds`。`close` 不删除已经生成的实体。`TargetCheck` 不读 `active`，也不持有令牌。

某一轮在节拍已经推进之后抛出，同一次排序里还没执行的实例留到下一 tick。采样点不是有限数时这一拍被消耗，且没有句柄入集。

## 5. 自动帧传到生成和运动

`runSegment` 在自动上下文里不调用 `LockTargetService.resolve`，不给目标发光。失效则用 `baseDirection()`，不取消整轮。种子是 `roundSeed ^ (memberIndex << 16)`。`forAttack` 之后 `applyDomainFrame` 写入 `frame.execution`，钳原点和锚点，并置 `overrideGeometry`。垂落视线是 `(0,-1,0)`。地面高度保持 `NEGATIVE_INFINITY`，避免二次采样贴到玩家脚底。`volumePoint` 在抖动和垂落抬高 8 格之后 `clampAutomaticSource`。`ReleaseBatch.crosshairCenter` / `resolveAnchor` 在自动上下文直接返回本轮原点。

跟随：`FollowCasterMotion` 有自动 `LaunchAim` 时钉在 `aim.origin()`，目标用 `findTarget`，失效留最后合法方向，垂落仍向下。追踪：实体 tick 把 `launchAim` 绑进 `TrackingSupport`，拍末清除。自动且带追踪或垂落时清掉 `lockThisCast`。`TrackingMotion` 因此不读玩家标记；`resolveTarget` 只重验局部目标，失效不再做 5 格查询。直线锁定走 `ProjectileMotion` 的 `findTarget`。`DropMotion` 只在 `lockThisCast` 时读标记，自动垂落已关掉这个标志。`StillAimMotion` 未改；静止斩在生成前写入非空锚点和瞄准。

空间切割自动分支不调用 `resolveAnchor`，锚点再钳一次。幻剑悬停走 `aimFromLaunch`，`behind()` 只在非自动路径。万剑归宗不新造剑；轨道中心是钳过的控制锚点，每拍按原轨道半径绕它摆放，不把剑坐标写成采样点本身。飞出后的追踪绑局部 `LaunchAim`。燎里 `automaticLifetime` 为真时 `releaseStopped` 直接返回假，`releaseSlot < 0`、松键、换手和施法者倒下都不提前结束，寿命仍按年龄。跟随原点是该次 `LaunchAim.origin`。关域后实体仍留着自己的 `LaunchAim`，运动不再查令牌。

垂丝：`overrideGeometry` 为真时，`WeepingCurtainEffect` 约 42–56 行的准星和横向 0.72 排开不执行，源点用已经钳过的 `frame.origin`。一分为三的各份因此叠在同一个源点。唇口是 `WeepingCurtain.lipOf`，在源点上方 `LIP_LIFT`，不经过 `clampAutomaticSource`。函数轨迹没有改成飞弹或垂落。

候选查询的 `targetQuery` / `entityQuery` 仍在。手动射线和手动 5 格追踪还调用它们。本次没有改 `query/` 的实现体。

## 6. 无绑定手动路径

`ReleaseExecutionContext.current()` 为空，或模式不是 `DOMAIN_AUTOMATIC` 时，锁定、准星、眼位和松手逻辑仍走原分支。`GroupBudget.reserve(owner, cost)` 仍是 `tellPlayer == true`，手动会话约 241 行用的就是这个两参数方法。自动轮不调用 `CastExecutionSession.open`、`CastCooldown.acquire`、`ChainProgress.advance`。无绑定的去重键不加后缀。

上下文用 `ThreadLocal<ArrayDeque>`：`bind` 压栈，`unbind` 只弹一层，空栈才移除。`spawnMember` 在 `try/finally` 里配对。第二成员和蓄力回调再次进入 `spawnMember` 时重新 `bind`，不依赖上一拍残留。自动蓄力由 `AutomaticRound` 先 `openChargeBypass`，避免 `runSegment` 再排一次；手动 `ChargeGate` 分支还在。`chargeReady` 这个布尔不是栈，内层 `closeChargeBypass` 会把它写成假。

## 已接到的入口，以及还没验证的边界

已从源码对上的入口：普通按键开域、双重立即和延迟开域、接触 `tryWaveReserved`、投影切步、自动轮报价和 5 刻第二成员、20 刻调度、关域取消、跟随、追踪、直线锁定、垂落、静止斩、空间切割、幻剑、万剑归宗、燎里、垂丝源点和唇口。

尚未用运行确认的边界：真实编译；跳 tick 与领域 tick、`ChainScheduler` 的先后；关域退款和法杖找不到；读档后的 `LaunchAim.remembered` 没有活的 `TargetCheck`，只留方向；主人死亡时风暴剑仍走原有主人检查；一分为三垂丝的横向排开；生成中途异常或重入 `close` 时，已出现的实体和退款是否同时发生。未游戏验证本身不是上述代码问题。
