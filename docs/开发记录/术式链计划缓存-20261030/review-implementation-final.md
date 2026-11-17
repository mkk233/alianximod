我先读任务说明和相关流程，确认这次授权范围后再按它执行。这是实现阶段第 2/3 轮只读复核。我先读本目录的设计、两作者终稿和证据，再只对照他们实际改过的源码。设计契约和两份终稿已经对齐。接下来只核对作者 A、B 实际改过的源码，以及验证这些问题必须看的直接依赖。未发现阻塞实现缺陷。按已批准契约，作者 A、B 的结构缓存会被列出的释放入口实际消费；静态阅读不能代替编译和游戏实测。

本轮是实现阶段集中只读复核，累计 2/3。设计 2/2 已用完，这里不再做设计判断，也不再派代理。

## 结论

当前工作区里的两作者源码与 `design-decision.md` 一致。命中、未命中和回退都还在原时点算费用、冷却和舍入；`ModifierPlan` 按次新建；缓存图不保存技能、定义、玩家、世界或闭包；未知定义在编译前整链 bypass；代际失配保持原切片，只把动态读取退回原函数。

## 覆盖范围

对照 `backup-A/`、`backup-B/` 读了当前源码，没有把仓库里其他未提交改动当成这次新增。

作者 A：`CastPlanCache.java`、`CastPlanContext.java`、`CastPlanCacheEvents.java`、`CompiledCastPlan.java`，以及 `ChainSteps.java`、`CompiledReleaseStep.java`、`SkillChainPlan.java`、`SkillUsePacket.java`、`SkillSegmentRunner.java`、`CastExecutionSession.java`、`AttackBudgetWindow.java`。`TechniqueChain.java` 未改。

作者 B：`ChainStructureEpoch.java`，以及 `ReleaseDefinition.java`、`ReleaseDefinitions.java`、`WaterReleaseDefinition.java`、`SelfStatusDefinition.java`、四个注册器和 `PerfCounter.java`、`PerfMetric.java`。

直接依赖只核对了这些：`TechniqueChain.split` 的计时、`ChainSegment` 的身份相等、`ChainProgress` 的签名、`Skill.getEmbryo` 与 `SkillType`、`ReleaseInput`、`FlowRoundLedger.FLUSH_DELAY`、`HoldRepeat` 到 `offer`、`SkillFollowCast` 的空上下文重载、`PerformanceRecorder` 按枚举长度分槽、`SkillSegmentMath.scaleCooldown`。`SkillUsePacket` 后半段没有逐行重读与缓存无关的原结算，结构调用点已用搜索核对。

## 契约核对

入口在消费缓存。`offerNow` 仍先做手持、法杖冷却缩减和属性加成，再从本次物品读出新的 `Skill`（`SkillUsePacket.java` 146–199 行）。`executeSkill` 在原结构区调用 `CastPlanCache.lookup`（250 行）。命中后步进下标、`progressSignature` 和切片段来自计划；冷却仍是 `ChainSteps.cooldown(offeredSkill, selectedIndexes, cooldownReduction)`（268 行）。未命中、无资格、超长、超预算、未绑定或线程不符时 `lookup` 返回 null，随后走原来的 `list`、`signature`、`slice` 和 `split`（279–311 行）。`HoldRepeat` 仍调用 `SkillUsePacket.offer`（`HoldRepeat.java` 73 行），同内容会再次查找。`SkillFollowCast` 仍走传入 null 的旧重载，没有 `ThreadLocal`。

费用和冷却的时点还在。`CompiledReleaseStep.applyChoice` 在按下时重算份额、整链冷却、动作冷却和蓄力拒绝（`CompiledReleaseStep.java` 144–168 行），不把 `cooldownReduction = 0` 的结果再缩放。附魔冷却仍是先 `Math.round` 再 `Math.max(1, …)`，然后整段一次 `scaleCooldown`（`ChainSteps.java` 96–116 行；`SkillSegmentMath.java` 140–142 行）。`skill == null` 的预扣走 `ReleaseInput.BASE`（`SkillChainPlan.java` 612–614、946–951 行）。法杖属性加成仍传到原 `segmentDamage`（`SkillUsePacket.java` 765、791 行）。普通段间隔、尾段冷却和条件重放仍在原分支调用 `scaleCooldown` 或 `ChainSteps.cooldown`。

时序未改。双重间隔仍是 5 tick：`SkillChainPlan.DUAL_STEP_TICKS`（1000 行）和 `CastExecutionSession.STEP_GAP`（45、257 行）。一分为三的汇总延迟仍是未改文件 `FlowRoundLedger.FLUSH_DELAY = 6`。蓄力仍在 `runSegment` 里用 `tickCount + chargeTicks` 排程，重入带同一个 `CastPlanContext`，视线和位置仍在进入 `runSegment` 时取得并传给续跑（`SkillSegmentRunner.java` 460–479 行），与备份同一位置。网络包内没有 `Random`。种子仍在真正执行段时用游戏时间计算（569–572 行）。

成员身份和计划对象符合约定。切片段是 `List.copyOf(TechniqueChain.split(...))`（`CompiledCastPlan.java` 98 行）。`ChainSegment` 没有自定义 `equals`，`indexOf` 按身份查找。`CompiledReleaseStep` 的 `members` 再从这一份列表取出并 `List.copyOf`（140–143、38 行）。`ModifierTemplate.instantiate` 每次 `new ModifierPlan` 并复制字段（933–955 行）。缓存只保存代际、字符串、下标、标志、份数、枚举、`ChainSegment` 和 `ReleaseInput` 记录；`CastPlanContext` 里的本次 `Skill` 不写回缓存。

资格、失效和延迟回退符合约定。`supportsStructuralPlanCache` 默认 false（`ReleaseDefinition.java` 360–362 行）。无资格在 `CompiledCastPlan.compile` 之前就 bypass（`CastPlanCache.java` 72–74 行）。缺失定义可以缓存；四个 `register` 都在 `put` 返回后调用 `changed()`。`ChainStructureEpoch` 使用 `AtomicLong`，开服不清零。水系公开 fluent 都在原赋值成功后 `changed()`；非法重水和重压门槛不写字段也不递增。`ReleaseDefinitions` 的构造闭包、`final` 的 `SelfStatusDefinition` 和 `WaterReleaseDefinition` 返回 true。代际失配时上下文仍持有构造时的切片；`canReuseStructure` 为假才调用原来的 `copiesBefore`、`releasedSegments`、`lockThisCast`、`dropThisCast`、`frozenChainInput` 和 `scanRoles`。

键、容量和计数符合约定。键是 `SkillType`、有序术式 id、原胚和代际，`equals` 做全字段比较（`CastPlanCache.java` 164–175 行）。`progressSignature` 只来自 `ChainSteps.signature` 的 id 串（`ChainSteps.java` 65–73 行），交给 `ChainProgress`，不把内容键写进去。原胚变化会换键，签名不变，游标不因此归零。链长、条目和单元上限是 64、64、16384。超长或超预算不拒绝释放。`ServerStarted` 绑定服务器与当前线程并换空表，`ServerStopping` 清空并丢掉引用。一次查找只记 hit、miss、bypass 之一；成功放入后再记 compiles（78–95 行）。预算失败只记 bypass。编译异常不捕获。`CAST_PLAN_COMPILE` 只包 `compile`。`CHAIN_SPLIT` 仍只在 `TechniqueChain.split`。`CHAIN_ROLE_SCAN` 只在 `scanRoles`，命中读标志不计时。`PerfMetric.CHAIN_ROLE_SCAN` 仍是第 3 个常量；四个计数和 `CAST_PLAN_COMPILE` 追加在枚举末尾。记录器按 `values().length` 分槽，报表按该数组输出名称。

## 不要求返修的观察

超预算时，编译可能已经调用过 `TechniqueChain.split`，回退后旧路径会再切一次。两次都是真实切分，释放仍走旧路径。份数以整数保存，没有先展开指数复制体。这不改变玩法能否释放。

## 未验证

没有运行 `gradlew` 或 `compileJava`，没有打包，没有启动游戏，没有做独立测试，也没有重跑名称门禁，没有重算证据哈希。名称门禁的既有 `OriginalHumanoid.java` 短名只转述作者报告，本次未改那个文件。

## 交付状态

两作者授权源码已在当前工作区落地，证据 `released` 均为 true。统一编译和游戏实测都还没有做。本结论只表示静态复核未发现阻塞缺陷，不是编译通过，也不是玩法验收。

本次只读。没有后台作业，没有持有源码写入权。
