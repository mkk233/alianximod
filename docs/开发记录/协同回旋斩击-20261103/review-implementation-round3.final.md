这是实现第 3 轮只读复核。我先读任务文件和新的提示，再只核对上一轮缺陷是否已经落到代码里。上轮七项是这轮的核对范围。我先读返修说明和记录，再按当前调用链逐项对代码。本轮结论是必须返修。七项里站位复验、领域协同费、结算权限、换手、双人失败回退和共同回旋已经落到当前代码；冲刺的后续接触仍会被第一次发布槽吃掉，接触目标也没有进入命中。

## 必须返修

### 冲刺的后续接触不会再生成斩击（I）

冲刺窗口对每个新的敌对实体各调用一次 `onContact`，并带上该次的接触点、方向和目标（`D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/effects/MovementDashEffect.java` 128–131）。桥接会为每次接触新开一把只触发一次的门，再把这三项交给发布（`CooperativeContactBridge.java` 18–22，`FamilyExecutionService.java` 1006–1011、1028–1029、1070–1077）。

第一次接触会把点用作锚点、把方向用作刀向（`FamilyExecutionService.java` 733–750）。发布槽的键只有成员、副本、生成序号和演员序号（633–636），没有这次接触的目标或点。同一次冲刺碰到第二名敌人时，`publishedSlots.add` 失败，该次的点和方向不再进入几何。整段攻击份额也在第一次成功发布时提交（686–690），后面的接触没有自己的提交线。

接触目标被写进 `TechniqueHitSpec.localTargetId`（`CooperativeVolumePublisher.java` 46）。命中实体没有这个字段，生成结束后规格上的值被清空（`TechniqueHitSpec.java` 481）。目标标识没有参与摆放或结算。

复现：冲刺加斜斩，一次扫过两名敌对生物。只有第一名接触点上出现斩击，第二名只有冲刺原有碰撞。

## 已核对通过

1. 执行族不再二次归位。出刀前用当前主人帧调用 `validate`（`FamilyExecutionService.java` 584–586）。失败会取消预约并在同一次调用里改发单人。站位仍由 `CloneServerPorts.tryReserve` 和 `advance` 完成。
2. 领域报价会计入协同。`RULE_COOPERATIVE` 单独成段，不会并进前一个宿主，扫描停在后面的第一段正伤攻击，双重只把恢复点延伸到同一组（`TechniqueChain.java` 56–59，`SkillChainPlan.java` 72–75、215–250）。预付份额使用报价金额，`detach` 移出轮末待退余额（`CooperativeCastRouter.java` 70–76，`GroupBudget.java` 31–37）。没有双人发布时，协同份额经 `refundShare` 退回同一张收据。冷却额在预付时为 0。
3. 记录和每个目标的刷新都调用 `allows`（`ReleaseBatch.java` 272–275、439–443）。守卫重查会话、维度、主人，并用 `CloneEntity.rejectsPulseTarget` 覆盖权限、分身、驯服动物和同队（`CooperativeSettlementGuard.java` 30–52）。没有守卫的旧账本不走这套判断。来源作用域在同一次刷新里以深度 1 打开并关闭。
4. 换手只退未提交份额，不关守卫、不清已记账的刷新（`FamilyExecutionService.java` 929–936、972–983）。死亡、父取消等硬原因仍丢待结算并停止动作。
5. 双人进世界失败时先丢弃本次暂存并撤回槽位，再在同一次调用里发一次单人（672–684）。`soloFallback` 之后下一刻不会再发双人。费用只在成功发布时提交。
6. 有预约且尚未 `READY` 时，定时门不进入 `RUNNING`，也不提交回旋费（448–455、529–530）。到位或退回单人后，玩家和分身使用同一个 `runningTick`、前摇和收势；分身 `mirrored` 为真，世代来自预约句柄（767–789）。没有分身的回旋立即开始。回旋没收势之前不播单独斩击。硬取消会按原序号停止各自动作。

## 无法静态确认

没有编译，也没有进游戏。楼梯承托、邻块加载和专服类加载仍不能从源码定成缺陷。

## 最小修复

交回 I：每次合法接触使用独立的发布身份，把该次的点、方向和目标送进这一次体积；不能复用第一次的发布槽。同一接触只提交一次，不能把第一下的玩家体积再发一份。

<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"必须返修。冲刺对第二名及以后的敌人不再生成斩击，发布槽不含接触身份，目标标识写入后未被命中逻辑读取。站位复验、领域协同费与detach退款、结算权限、换手保留已付账本、双人失败同栈改单人、双方回旋时间线已在代码中成立。未编译，未进游戏。","changedFiles":[],"checks":["站位一次归位与validate","接触点方向目标与后续接触","领域报价detach与协同退款","record/flush权限与来源作用域","载体失效与硬取消","双人失败同栈单人","回旋READY与双方时间线","TechniqueChain宿主归属"],"notRun":["未编译","未游戏验证"],"writesSettled":true,"blocker":null,"nextAction":"主会话仅按最终问题交原作者返修并转达结果；本批实现3/3已用完，不自动追加复审；写入收束满足条件后指定Cursor统一编译"}
<<<END_CURSOR_TASK_REPORT>>>
