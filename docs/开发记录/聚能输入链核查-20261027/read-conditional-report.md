## 结论

条件释放的完整链路为 `ConditionalEffect.attempt` → `PrisonService.prepare` → `PrisonReservation` → `PrisonRuntime.commit` → `PrisonView/PrisonRecord`，这条链只在 `CastExecutionSession.commitMember` 中触发。几何与寿命在 **commit 那一刻**一次性冻结进 `PrisonView`，准备阶段不写任何持久状态。魔力留在 `CastExecutionSession` 的 `GroupBudget`，牢侧全程不扣费。`MagicCreateWaterEffect` 落下的水是一格永久世界方块，没有寿命上限可缩放，属于真实长期世界改变。

## 条件释放链与准确接口

`ConditionalEffect`（`ConditionalEffect.java:12-24`）只有 `attempt(ServerPlayer, UUID operationId, EnchantmentSnapshot contact)`，外加两个默认关闭的 `copiesCoalesce()`、`supportsContact()`。`ConditionalEffects.java:10-18` 是这两个开关的读取口，`ContactCast.java:73-77` 用它判断零伤宿主能否承载接触附魔。`WaterPrisonEffect` 两个开关均为 true（`WaterPrisonEffect.java:24-31`），所以它能接附魔，且复制份只尝试一次。

`CompiledReleaseStep.actionConditional`（`CompiledReleaseStep.java:96-146`）只接受“动作段 + 后面恰好一个零伤条件段”的组合，要求 `definition.conditionalEffect() != null`（117 行），并在 148-157 行以 `chargeTicks() > 0` 拒绝蓄力。`memberShare`（167-190 行）在 `ConditionalEffects.copiesCoalesce(definition)` 为真时返回 `enchantAndHost`，基费不乘份数（182-184 行）。

会话侧在 `CastExecutionSession.commitMember`（268-365 行）。条件分支是 285-293 行：`operationId = ConditionalIntent.operationId(parentCast.tokenId(), memberIndex, 0, group.contact.groupId())`，随后 `SkillSegmentRunner.commitConditional(owner, definition, operationId, snapshot)`。`ConditionalIntent.java:15-26` 的编号只由父令牌、成员、波次、接触组成，复制序号不进入编号，因此同一意图的复制份落到同一预约。提交成功后 `recordConditionalUse`（290 行）。

## 给定调用点

- `WaterPrisonEffect.java:38` 调 `PrisonService.selectTarget(caster, 20.0D)`，范围常量 `TARGET_RANGE = 20.0D`（18 行）。
- `WaterPrisonEffect.java:42` 调 `PrisonService.prepare(caster, target, operationId)`。
- `WaterPrisonEffect.java:53` 直接调 `reservation.commit(contact)`；`59` 行 `finally` 里 `reservation.close()`。
- `PrisonService.java:15-17` 转 `PrisonRuntime.prepare`；`35-40` 另有一个 `commit(reservation, contact)` 包装口，水牢自身走的是上面的直接调用。
- `PrisonRuntime.prepare` 仅此一处被 `PrisonService` 调用。

## 几何与寿命如何冻结

几何：`PrisonRuntime.commit` 84-85 行取 `anchor = target.position()`、`bounds = PrisonShell.expand(target.getBoundingBox())`，外扩 `PADDING = 0.20`（`PrisonShell.java:10,15-20`）；`innerOf` 反向收缩 0.20（22-31 行），得到 `PrisonRecord.inner`（101 行）。`touches`（33-47 行）要求候选包围盒与外壳相交，并且伸出内盒，同时排除囚犯自身（34 行）。这些值全部进入不可变 `PrisonView`（`PrisonView.java:14-25`），创建后没有改写入口。

寿命：79 行 `durationTicks = prisonTicks(tier, wet.getDuration())`，341-358 行的映射是 tier1 固定 20、tier2 固定 100、tier3 取剩余的一半、tier4 取全部剩余。89 行 `birthTick = level.getGameTime()`，`endTick = birthTick + durationTicks`（100 行）。到期在 `PrisonRecord.tick` 455-458 行按维度 gameTime 判定并 `finish(EndReason.DURATION)`；离开加载时 447-449 行只置 `expiredWhileAway`，回来在 `rebind`（`PrisonRuntime.java:263-278`）或 tick 里补结算。可缩放的量只有潮湿剩余时长与 tier 这两项输入。

## 准备与提交/退款语义

`prepare`（42-63 行）：参数缺失返回 `rejected`；同一 `operationId` 重复准备直接返回已存在预约（49-52 行，幂等）；目标已有牢或有他人预约时返回 `"occupied"`（56-58 行）；成功则写 `PREPARED_TARGETS` 与 `SESSIONS`，预约 `acquired = true`。`close`（121-132 行）先移除会话；未提交时只释放自己持有的目标占位，已提交则牢体保留。

`commit`（65-119 行）的失败原因依次为 `dead`、`dimension`、`not_wet`、`duration`、`rejected`。成功时顺序为：卸乘（83 行）、冻结视图、`markCommitted`、清占位、登记三张表、可选 `ContactEnchantState.open`、`PrisonEvents.created`、tier≥4 触发一次 `PrisonPressure.pulse`。后续异常只记日志，仍返回 success（115-118 行），因此服务端不会回滚已经建立的牢。

退款在会话层：`GroupBudget.reserve` 预扣整组 `groupBill()`（`CastExecutionSession.java:221,490-496`），`budget.commit(plan.ruleShare + memberShares.get(0))` 仅在首成员 committed 后执行（238 行），`finishGroup → budget.settle` 把 `openShare` 退回（367-377、509-516 行）。条件被拒时 `first.committed()` 为假，整组预扣原样退还；`actionReceipt` 在 `dropUnaccepted` 里 `refund`（410-412 行）。牢侧没有独立的费用或退款接口。

## 输入量可挂载处

现成贯通到 commit 的载体是 `EnchantmentSnapshot contact`：`attempt` 收它，`PrisonReservation.commit(contact)` 透传，`PrisonRuntime.commit` 108-110 行交给 `ContactEnchantState.open`。它随释放包生成，不进入预约身份，因此附加数值不会改变 `operationId`，也不会触发 `"occupied"`。次选是 `ConditionalIntent.operationId` 的 `volley` 形参（当前恒传 0），但改动它会改变操作号，同一批复制份可能落到不同编号上。`PrisonView` 的 `lockedTier` 与 `endTick` 是冻结后的输出，适合承载“已结算的时长/强度”，不适合承载原始输入。以上仅列事实性挂点，供裁决。

## 生成水

`MagicCreateWaterEffect`（`MagicCreateWaterEffect.java:12-21`）取 `pos + look * 5` 的整数方块，若为空则 `setBlock(Blocks.WATER.defaultBlockState(), 3)`。它写入的是整格水源，只改一格，没有半径、没有计时器、没有回收或到期路径，也不进入任何记录表。因此它是长期世界改变；寿命维度上只有“永不消失”一个取值，没有可缩放的有限范围。只从这份文件看不到它与 `ConditionalEffect` 的注册关系，宿主伤害值也不在给定源码内。