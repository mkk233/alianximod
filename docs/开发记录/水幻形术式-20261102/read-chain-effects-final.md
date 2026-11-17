我先完整读取这份任务说明，确认授权范围后再按其中的步骤执行。任务是只读事实补充，不改代码。我先读本批用户决定和已有事实，再核对三条链路。已有事实里，链捕获、收费时点和客户端短动画都还空着。我按这三块只核对现行入口。水幻形仍无正式术式。用户决定是：从装配链指定，先支持适配过的水系攻击；未适配不能默默执行，也不能收费后空放。普通释放留置、潜行立即、复杂链在收费前拒绝，只作读上下文，这里不拍板。

## 1. 收费前的共同口，以及“捕获一段、主人不再放”的现成跳法

`SkillUsePacket.handle` 只带槽位和 `holdGesture`（0 一次、1 按住、2 松开）。`GESTURE_HOLD` 先 `HoldRepeat.arm`，再 `offer`。`offer` 固定重构成 `GESTURE_ONCE` 后进入 `offerNow`。`HoldRepeat.tick` 以后也走 `offer`，不再看到当初那个手势字节。

`offerNow` 在进入 `executeSkill` 之前只做这些，且都不报价、不扣款、不推进游标：法杖或槽位、`TechniqueAccess.skillAllowed`（185 行）、`HeldSwordGate.refusal`（原始整链，189 行）、法杖拒绝剑术、`CastCooldown.busy`。`executeSkill` 244 行再查一次准入，248–249 行只算武器加成。

三条路在 `executeSkill` 250 行 `BatchReleaseCompiler.compile` 处分叉。这是报价计算、扣款、游标推进之前，Batch、领域绑定、双重都能走到的最后一处。

- 链声明了批量能力时，报价溢出在 `compile` 内部变成 `refusalKey`，252–255 行直接拒绝，还没进 `BatchCastSession.open`。合法批量则在 `open` 里 `GroupBudget.reserve(plan.reservedTotal)` 一次扣清。这条路不调用 `ChainProgress.advance`。
- `compile` 返回空才到 258 行 `BindingLayout.compile`。结构失败在这里拒绝，仍未扣款、未读游标。成功则用投影技能替换原链，被收走的下标不再出现在父链里。
- 游标只在这条非批量路上读取：`ChainProgress.current`（295 或 324 行）。推进更晚，而且分叉：普通成功在 1008 行，条件父结算在 1200 行，双重要等 `CastExecutionSession.tryFinish` 618 行。读取不等于推进。
- 双重在 388 行 `CompiledReleaseStep.compile`。`chargeRefusal` 只是报价失败文字，390–392 行拒绝，尚未扣款。真正扣款在 `CastExecutionSession.startAction` 160 行，先扣 `actionShare`；接触后 `beginGroup` 再按 `groupBill` 另留一轮。动作引导的双重因此是两个收费时点。
- 都不是上述分支时，418–427 行 `SkillChainPlan.pressCommitCost` 合成一个整数，`MagicPool.trySpend` 扣一次。

因此，未适配或复杂链要在所有收费前一致拒绝，检查只能放在 250 行之前，对象是原始 `Skill.getTechniques()`。放在 `BindingLayout` 时，批量链已经返回，合法批量可能已经扣款。放在双重 `chargeRefusal` 时，批量已分叉，游标也已读过。250 行之前现有的准入和持剑门不认识“一个捕获术式加紧邻一条攻击”。

声明式消费合同在领域绑定，不在分身。`SkillUsePacket.register` 安装 `BindingStructureCompiler` 和 `AutomaticRound.executor`。`BindingLayout.compile` 不扣费。`BoundReleasePlan` 冻结术式、胚胎、`frozenOriginalIndexes`、`nextOriginalIndex`（没读到的下一原始格，-1 表示旧工厂没有游标）、`consumedOriginalIndexes`（收进冻结序列的原始格）、`chargedOriginalIndexes`（展开给出且确实进了冻结序列的计费格；空列表表示这次没有展开账单）。`BoundMember` 保存冻结序列里的宿主、原始下标和 `ReleaseInput`。`StructureFault` 是原始下标加一句失败。父链用投影技能继续，捕获段不会再被父循环执行。被捕获的内容以后由 `DomainBindingRuntime` 调 `BoundReleases.executeAutomatic`，仍以在线主人为施术者；每轮 `AutomaticRound.priceQuote` 再 `GroupBudget.reserve` 一次。这要求绑定标记紧挨领域术式，不是任意捕获术式。

主人循环里另有两处“跳过下一段、但仍由主人执行”：

- `ChainRole.ACTION` 时，`SkillChainPlan.scanModifiers` 从下一段起，把第一段正伤害、非附魔记为 `attackSegment`，`resume` 停在它后面。540–588 行把 `segmentIndex` 设成 `plan.resume`，父循环不再单独排这段。攻击仍由主人经 `AttackBudgetWindow` 和 `SkillSegmentRunner.playAction` 打出。`pressCommitCost` 76–97 行把动作费和这段攻击费加进同一次扣款。双重规则或批量、双重编译命中的链进不了这里。
- 854–893 行的命中触发：循环 `segmentIndex += 2`，`pressCommitCost` 128–134 行也不加下一段的费用。命中回调里再 `MagicPool.trySpend` 一次，仍是主人执行。这是第二笔收费。

`CloneService.spawn(ServerPlayer, CloneKind)` 不接收 `Skill`。生成费和脉冲费是两次 `ManaPaymentReceipt.charge`。没有任何现成入口把紧邻攻击冻成载荷交给分身，并让主人循环跳过它。

潜行没有被写入释放包，`src` 里也没有读取 `isShiftKeyDown` 或蹲姿。服务端玩家实体在 `handle` 当刻带有原版潜行标志，但 `CastTiming`、`ReleaseInput`、收据和游标都不保存它。延后的 `ChainScheduler` 若再读，读到的是开火时的姿态。`HoldRepeat.held` 只表示释放键仍按着，从 `arm` 起可跨 tick 读到，直到 `drop`。它和潜行不是同一个位。普通留置或潜行立即若要冻结，只能在 250 行之前的同一次服务端处理里读取并自行保存；现成字段里没有这一份。

## 2. 客户端临时效果：现成同步和摘掉实体后还能播的例子

`ModNetwork.register` 的 0–15 号包没有分身专用包。分身位置走原版实体追踪。`CloneEntity` 同步的是 `DATA_KIND`、`DATA_OWNER`、`DATA_LIFE`、`DATA_RESPONSE`、`DATA_SEQUENCE`（动作序号）、`DATA_GENERATION`、`DATA_SESSION`。`CloneRuntime.allocateToken` 的服务端动作令牌不在同步字段里。序号在提交后增加，经 `CloneEntity.mirror` 写出。

`CloneRenderer.seenActionSequence` 是渲染器上的 `HashMap`。渲染时实体已移除就删掉该键；序号变化只挥主手。没有撒水、散架，也没有摘掉实体后继续播的画面。实体离开渲染列表后，这个表项不会因此单独形成动画。

追踪加主人广播的真实例子是玩家姿态，不是分身。`ActionSyncS2CPacket` 在通道 9，字段含 `ownerId`、实体网络号、`dimensionId`、`sessionEpoch`、`sequence` 和斩击方向，没有脱离实体的世界坐标。`ActionRuntime.publish` 发给主人，以及同一维度里距离主人不超过 128 的玩家。`ActionServerEvents.onStartTracking` 在观察者开始追踪这名 `ServerPlayer` 时补发 `ActionRuntime.snapshot`。客户端 `ActionClientPlayback` 按 `ownerId` 放在 `VIEWS`：登出清空；卸载的世界与当前绑定世界相同则清空；包的维度与当前客户端维度不一致则丢弃。这是连接和世界范围内的视图，不是跨登出常驻的全局表。

实体移除后仍独立播完的例子只有静止斩面。`StillSheetPlayback` 在命中实体加入时记候选；`offerEntity` 把位置、转角、尺寸、贴图、帧数、每帧刻数和出生时刻抄进 `StillSheetSnapshot`，交给 `StillSheetCache`。`onHitEntityLeave` 只删候选，不删已保留的快照。绘制读的是这份快照。到期由 `dropExpired` 去掉。客户端世界实例变化时 `alignWorld` 清缓存，`dropOtherWorlds` 按维度标识丢掉其他世界；登出 `forgetWorld`；对应世界卸载且正是绑定世界时清缓存。它只认 `TechniqueHitEntity.stillSheet()`，没有分身或水线条人形的调用者。

相反例子：`FireSeedFlameClient.onEntityLeave` 立刻删掉该实体的火焰；`LiaoliRingTrail.onPlayerLeave` 立刻删掉该施术者的环。两者都不在实体离开后继续播。分身破散没有对应的客户端短动画。`CloneLifecycleEvents.onLevelUnload` 是服务端结束分身，不是画面。

## 3. 生成费加载荷费：一次报价、一次支付，以及要裁决的冲突

全局倍率在 `GlobalBalance`：伤害、魔力、冷却各一份。`BalanceService.manaCost` 对一个术式的一个魔力键乘一次全局魔力。`damageContribution` 对术式伤害倍率和全局伤害倍率乘一次。`BalanceDisplay.mana` 和 `damage` 是链上报价的读口；`SkillSegmentMath.quotedMana` 对每个术式调用一次。两笔 `quotedMana` 相加，不会再把合计乘一次全局魔力。`CloneBalance.readSpawn` 和 `readPulse` 各自读自己的 `manaCost`。脉冲伤害只走一次 `damageContribution`，不把 `PULSE_DAMAGE_MULTIPLIER` 再乘一遍。领域、屏障、护环另有本体单价：`configuredBodyUnit` 里领域读 `manaCost`，水幕和水环读 `spawnManaCost`；宿主占位 0 且基础伤害不为正时，`quotedMana` 不再读一笔，避免叠成两笔。

现成的“一段动作加紧随攻击，一次整数、一次扣款”只在普通链：`pressCommitCost` 把动作段费用、修饰报价和攻击段缩放费用加成一个整数，427 行附近扣一次。随后 `UncommittedShare` 从这张 `ManaPaymentReceipt` 上切份额。这不是配置里的“生成费 + 被捕获 payload 费”组合键，也没有把分身生成和某条水系攻击合成一次报价的方法。

对不上这一次支付的现成路径：

- 命中触发的下一段不进 `pressCommitCost`，命中时再扣一次。
- 领域绑定收走的攻击不进父链报价；自动轮每轮重新报价并 `reserve`，领域运行时约每 20 刻一轮，可能重复扣。
- 双重先扣 `actionShare`，接触后再扣 `groupBill`。
- 批量只扣编译器自己的 `reservedTotal`，与分身生成费、脉冲费无关。
- 分身生成和脉冲本就是两次 `charge`。生成失败或未发布时 `receipt.refund`；脉冲跨过提交线之后异常不退，提交前失败才 `refundOnce`。

冷却也是分开的。普通链重放看末段 `configuredBaseCooldown` 再乘法杖减冷却；分步链用 `ChainSteps.cooldown`。双重在 `tryFinish` 里按是否合法碰撞取 `fullCooldown` 或 `actionCooldown`。分身生成冷却和脉冲冷却分别来自两份 quote。一次扣款不会自动合成一个冷却。

异常退款的位置：普通链 `stepFailed` 且非条件链时，整张 `pressReceipt.refund`，返回值仍是魔力不足。条件链按失败下标退尾段，报价溢出则整张退。条件段提交失败时 `UncommittedShare.cancel`，再退后续费用。`refundIfBlocked` 在施术者死亡、换维、纪元变化或持剑门失败时退这一份额。宿主组中断同样 `claim.cancel`。`GroupBudget.settle` 退还没提交的余额。双重未接受时退 `actionReceipt`。批量抢不到冷却锁时 `settle`。

要设计者定的冲突是：250 行前拒绝才能盖住批量扣款，但那里还没有投影、游标和双重结构；领域绑定的消费索引要求领域标记，不能直接当成捕获术式；普通链的一次扣款会把紧随攻击交给主人执行；若改由分身以后再放，现有脉冲是第二次扣款，和“生成费加载荷费只付一次”冲突。潜行只有处理当刻的原版标志可读，包和收据都不冻结它。客户端没有分身摘除后的短动画，静止斩面快照不能直接套到分身实体上。
