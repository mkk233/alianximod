我先读取第 2 轮核验任务说明，确认授权范围后再按它核对源码。任务是只核对本轮 R1–R5 返修。我先读裁决、返修契约和 A/B/C 最终报告。报告已对齐。接下来按 R1–R5 核对发布回执、拆价、在途取消和句柄交接的实际行号。返修后仍有一处真实控制不记账。`SwordComeEffect.execute` 在 `SwordComeEffect.java` 第 19–23 行对 20 格内存活剑调用 `startReturning`。`SwordEntity.startReturning` 在 `SwordEntity.java` 第 260–266 行直接 `setState(RETURN)`，不读 `PublicationAttempt`，也不调用 `reportAccepted`。自动松散副本的 `accepted` 保持 false，`PublicationAttempt.close` 不走 `retainLoose`，这一份在 `settle` 时退回，剑已经进入 `RETURN`。`beginVortex` 已接上通知，`startReturning` 没有。

除此以外，按当前源码对照 R1–R5，没有看到新的反例。本报告只记录这五项的静态证据。`compileJava`、测试和游戏都没有跑。

**R1 回执与退款**

空 `execute`、幻剑资格早退、没有可操纵的剑、`addFreshEntity` 返回 false，都不会把 `accepted` 设为 true。`close`（`PublicationAttempt.java` 第 141–149 行）只在松散且已接受时 `retainLoose`，其余份额留到 `GroupBudget.settle`（`GroupBudget.java` 第 41–48 行）按仍开放的份额退一次。

- 幻剑两道持剑资格在 `PhantomSwordEffect.java` 第 31–35 行返回，不通知。
- 万剑归宗空列表在 `SwordReturnAllEffect.java` 第 26–30 行返回。自动不发「没有可操纵的剑」；手动仍发。
- `TechniqueHitSpec.java` 第 405–411 行、`PhantomSwordEffect.java` 第 82–88 行：`addFreshEntity` 为 false 时不通知。
- 为 true 时，命中体在第 404 行加入后，第 413–415 行立刻 `reportAccepted`。幻剑在第 82 行加入后，第 84–86 行立刻通知。
- 万剑归宗：`beginVortex` 先在第 275–287 行拒绝（不允许发布、自动却没有尝试、令牌或生成不允许），此时不改状态。通过后第 300 行 `setState(VORTEX)`，第 301 行速度清零，第 302–304 行通知，然后才 `adoptAutomaticAim`。同一计划副本里多把剑重复通知时，第 119–120 行第二次直接返回 true，只记这一份的价。

宿主组与松散收据分开。`reportAccepted` 第 116–117 行对 `HOST_GROUP` 恒为 false。费用只在 `AutomaticRound.java` 第 235–241 行：`ReleaseOutcome.committed()` 才 `noteHostCommitted(true)`，`retainHost`（第 447–461 行）提交该成员整池。注册里 `submitsHostGroup` 只有 `space_cut`（`ReleaseRegistry.java` 第 12 行）。失败走原有 `hostGroup.fail()`，不会因为回执返回 false 删掉已经加入的实体。

**R2 三份里只成一份**

报价在 `priceQuote`（第 95–116 行）一次 `reserve`。非双重：成员 0 的池是 `pressCommitCost`，规则份额是 0。双重：池只取 `memberShares`，`ruleShare` 另计。`splitPool`（第 297–306 行）向零取整，余数给较小下标。池 10、三份：价格 4、3、3，和为 10。只成功第 0 份时，`retainLoose`（第 438–442 行）只 `commit(4)`；成员 0 再 `commit` 一次规则份额。其余两份不提交，结算退回。同一副本里多实体多次 `reportAccepted`，`retained[copy]` 挡住第二次 `commit`。成员 0 正常失败不 `halt`，成员 1 仍 `launchMember`。某一份在已通知之后抛异常：该份 `finally` 里先 `close` 提交，再由 `finishMember`（第 168–170 行）`haltRemainder`，还没开的后续成员被 `blocksLaunch` 挡住。全部失败则没有任何 `retain`，整笔预留含规则份额退回。宿主 `COMMITTED` 是整池一次，不按实体拆；未 `COMMITTED` 不 `retainHost`。

**R3 异常、重入取消、done**

`close` 先提交，再弹出，再 `attemptFinished`。`settleNow`（第 476–484 行）只在 `inFlight` 回到 0 且已请求取消、已 halt 或成员都结束时发生。`done()` 就是 `settled`（第 376–377 行）。`reportAccepted` 不复查取消和令牌。重入取消时若 `inFlight > 0`，`cancel`（第 360–367 行）只置 `cancelRequested`，`done` 仍为 false；已经 `addFreshEntity` 为 true 的那次仍可通知并在 `close` 保留；尚未发布的 `allowsPublication` 变 false。这次 `close` 之后才结算，`done` 才变 true。没有在途尝试时，`cancel` 立刻 `settleNow`。不把引擎在 `addFreshEntity` 返回 true 之前的半写入算成额外反例；返回 true 之后，通知是下一条调用。

**R4** 见上面万剑归宗第 26–30 行。

**R5 垂丝**

自动且规则尚未摆位时，`WeepingCurtainEffect.java` 第 43–52 行用 `copyIndex`、`copyCount` 和 `STRIP_STEP` 0.72，源点取帧原点，再 `clampAutomaticSource`。`CastFrame.forAttack` 第 106–107 行写入这两个下标。三份侧移是 -0.72、0、+0.72。单份侧移是 0，留在本轮原点再夹紧，不改到玩家眼睛，也不加手动的前向 2.2。规则已摆位（第 72–74 行：`cohortDispersed`、`pose3d`、`scatterAim`、`volumeHalf > 0`）不再加第二层横排。`water_weeping_arc` 的 `trajectoryMutable` 为 false（`ReleaseDefinitions.java` 第 24 行）。`TechniqueHitSpec.java` 第 235–244 行对 `FUNCTION` 保留效果已经写上的 `start`，不把路径改成斩击锚点。手动分支仍是第 53–66 行：眼睛、朝向、前向 2.2、侧移 0.72。唇口仍是 `WeepingCurtain.lipOf` 第 95–99 行，在源点高度上加 `LIP_LIFT`。函数 id 和寿命沿用定义，这次垂丝改动没有改它们。

**C 的交接**

`DomainBindingRuntime.releaseOne` 第 150 行返回后，第 151–153 行用同一个 `now`：句柄未 `done` 且 `roundStillHosted` 为假就 `cancel`，然后仍返回该句柄。`DomainInstance.trackRound` 第 135–148 行：未托管且未 `done` 再 `cancel` 并且不加入；已 `done` 直接返回。`roundStillHosted` 第 120–132 行检查仍开启、未过期、主人在线且存活、仍在宿主维度。取消后的句柄不会被加回 `openRounds`。收据栈与 `ReleaseExecutionContext` 分开；`spawnMember` 第 279–281 行在 `finally` 解绑。延迟的第二成员和蓄力回调仍查这一轮的 `blocksLaunch` 与令牌。

`PublicationAttempt` 的 `openLoose`、`openHostGroup`、`close`、`noteHostCommitted`、`current`、`allowsPublication`、`reportAccepted` 与 A、B 的调用一致。`beginVortex` 改为返回 boolean 后，唯一调用点 `SwordReturnAllEffect.java` 第 39 行忽略返回值。`trackRound(RoundHandle, int)` 的调用在 `DomainBindingRuntime.java` 第 95 行。`GroupBudget`、`ReleaseExecutionContext`、`ReleaseBatch`、`SwordVortex` 这次返修没有改。这些是调用关系对齐，不是编译结果。
