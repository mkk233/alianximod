# 公共路由接线

当前只支持元素法术消费聚能。无属性法术不支持。这是按较窄原需求收口，不是用户从选项里点过一项。

## 冻结

`SkillChainPlan.frozenChainInput(Skill, List<Technique>, int)` 在计划建立时调用一次。

- `consumesElementMagic` 全部成立才扫描：技是 `SkillType.MAGIC`，成员是 `TechniqueType.MAGIC`，属性不是空也不是 `NONE`，定义 `ReleaseKind.HIT_VOLUME`，并且不是附魔。空列表、非法索引、空成员都是 `BASE`。不按术式 id，也不用正伤害过滤。
- 否则返回 `ReleaseInput.BASE`。通过后沿用 `chainEnergyBefore` 的连乘。往前扫到释放边界就停：不是附魔，定义存在，`ReleaseKind.HIT_VOLUME` 且 `ChainRole.ATTACK`。`sharesDualRelease` 为真时跳过这个边界继续，否则 `break`。聚能是规则加攻击职责，不是边界。动作宿主不截断。未注册定义仍跳过。无属性法术和剑术不能消费，但只要符合这条边界，聚能就不能跨过它们。非有限或 `<= 0` 回到 `BASE`。倍率恰好为 1 时仍是新对象，不收成单例。
- `geometryEnergy(ownEnergy, input)` 只做自身 `energyScale * sizeMultiplier()`。附魔半径仍由 `CastFrame.volumeScale` 再乘。缺月 1.35 留在定义自身倍率里。

`ModifierPlan.memberInputs` 在 `collectModifiers` 结束时写入。双重每个实际成员一份；普通攻击一份，复制和延迟都读这一份。`CompiledReleaseStep.memberInputs` / `actionInput` 在 `compile` 时写入。执行不再调用 `chainEnergyBefore`。

## 入口

| 入口 | 传入 |
|---|---|
| `SkillUsePacket` 领域 `tryWave` | 该段 `frozenChainInput` |
| `SkillUsePacket` 屏障 `tryPlace` | 同上 |
| `SkillUsePacket` 护体 `schedule` | 同上 |
| `SkillUsePacket` 场地 `releaseFromChain` | 延迟 lambda 捕获该段快照 |
| `SkillUsePacket` 条件 `commitConditional` | 立即和延迟都传该段快照，四参 `attempt` |
| `SkillUsePacket` 操纵剑、冲刺、触发下一发、宿主组、普通 runner | 各成员自己的快照，显式传入 runner / `commitHostGroup` |
| `SkillChainPlan.firePaidPlan` | `plan.attackInput()`，全部齐射和复制共用 |
| `SkillChainPlan.fireDualSequence` | `plan.dualInput(序号)`，领域、屏障、护体和攻击 runner 的立即与延迟都用它 |
| `CastExecutionSession` | `plan.memberInput` / `plan.actionInput`。接触、第二发、预约容器、条件、宿主和 `commitNow` 都读编译时的快照 |
| `SkillSegmentRunner.runSegment` | 使用传入的 `ReleaseInput` 写 `frame.releaseInput`，几何走 `geometryEnergy`。蓄力延迟重入同一份。`releaseAttached` 传同一份 |
| `SkillSegmentRunner.commitConditional` | 四参 `attempt(..., input)`。无参旧重载仍是 `BASE` |

接触窗口 `AttackBudgetWindow` 没有改。它稍后调用的 `firePaidPlan` 读的是建计划时已经放进 `ModifierPlan` 的快照。

费用、单次伤害、冷却、速度、索敌范围、维护周期没有乘。操作号、预扣和退款没改。没有把输入写进 `EnchantmentSnapshot`。`ChainRole.ATTACK` 没改。

## 剑术

聚能注册类型是 `TechniqueType.MAGIC`。现行装配是 `SpellAssemblyMenu`：剑技槽里 `rejectReason` 对法术且 `!SkillCarrier.allowsMagic` 返回「法术不能装入剑技槽」，`GearSlot.mayPlace` 据此拒绝，`set` 提示后不写入。`allowsMagic` 只对法杖为真。没有另加术式 id 判断。

旧的 `SkillAssemblyPacket.put` 在非法杖分支先做同一条法术检查，后面的 `suppliesChainEnergy` 分支新的法术聚能到不了。

已有旧链：剑技整条，以及剑术、无属性、动作、规则、通用和附魔成员，服务端冻结都是基准倍率。

## 资格

只支持元素法术。无属性法术不支持。聚能源自己的类型、属性和原胚不能代替目标成员。

## 遗留入口

`SkillFollowCast` 在 `src/main/java` 没有外部调用，只有类内部一处。它是未使用的旧入口，不是当前运行时漏接。本次不改、不重新接通，也不把它当成真实运行阻塞。真实父窗口是 `CompiledReleaseStep` 到 `CastExecutionSession`。旧的 while 调度风险只记遗留，没有宣称已经验证或修复。

## 测试

`src/test/java/com/bslx/XueXiMod/network/RouteReleaseInputCheck.java` 调用生产的 `collectModifiers`、`CompiledReleaseStep.compile`、`frozenChainInput`、`geometryEnergy`、`suppliesChainEnergy`。元素法术正伤和零伤容器覆盖 2 和 4。无属性、元素剑术、剑技槽里的法术、动作、通用、规则种类和附魔，以及空链、非法索引、空成员、空定义，都按倍率 1 比较，不比较基准单例。第二名成员的倍率是 1，不复用第一名的 2。缺月自身 1.35 仍是 1.35。聚能注册类型断言为法术，且 `canUseForSwordSkill` 为假。资格收口后曾是 46 条。释放边界收口后重跑：68 条，失败 0，退出码 0，日志 `check-route-boundary.log`。菜单的 `rejectReason` / `mayPlace` / `set` 需要玩家，没有启动菜单。

命名门禁里本次文件没有新违规。旧的两处仍在 `OriginalHumanoid.java` 第 165 行 `i`、第 202 行 `to`，没有改。
