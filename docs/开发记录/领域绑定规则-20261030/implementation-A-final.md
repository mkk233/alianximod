# A 实现终报

released=true

范围：绑定结构、共享自动释放与周期预算。设计 2/2 已用完，本次未做设计审查，也未做实现复审。统一编译未跑。整个功能未验证。

## 源码与新增接口

新增 `shu/technique/release/bind/`：

- `StructureFault`：结构错误下标与原文。
- `ReleaseInputMode`：`MANUAL`、`DOMAIN_AUTOMATIC`。
- `ReleaseVolume.clampPoint`、`TargetCheck.stillValid`、`LifetimeToken.valid`。
- `RoundHandle`：`roundId`、`cancel`、`cancelled`、`done`。
- `BoundMember.create`、`BoundReleasePlan.create`。计划额外保存技能类型和名字，`frozenSkill()` 按此重建冻结技能。
- `BindingLayout`：`Compiler`、`installCompiler`、`compile`、`ready`、`failed`、`projectedSkill`、`originalIndex`、`originalFromStep`、`planAtDomain`、`faults`。
- `ReleaseExecutionContext`：`ArrayDeque` 的 `ThreadLocal`。`bind` 压栈，`unbind` 弹一层，空栈时移除。`automatic` 缺少世界、主人、两个 UUID、原点、方向、体积、目标检查、武器副本或令牌时抛 `IllegalArgumentException`。方向会归一化。`weaponCopy()` 每次返回新副本。不读眼睛或准星。
- `BoundReleases`：`Executor`、`install`、`executeAutomatic`。未安装时抛异常。

新增 `network`：

- `GroupBudget`：从原会话内部类提出。`reserve(owner, cost)` 仍等于 `tellPlayer=true`。`reserve(owner, cost, tellPlayer)` 只保存 `ManaPaymentReceipt.charge` 返回的收据。`settle` 幂等。未改 `MagicPool` 或 `ManaPaymentReceipt`。
- `DomainOpening`：一次按键的布局、投影步下标和释放 UUID。`claimFor` 在有计划时返回 `bind:<原始领域下标>:<释放UUID>`。
- `BindingStructureCompiler`：`BindingLayout.Compiler`。
- `AutomaticRound`：`BoundReleases.Executor`。

`SkillUsePacket.register` 在 `channel.registerMessage` 之前安装 `BindingStructureCompiler.compiler()` 和 `AutomaticRound.executor()`。

改过的既有文件：`SkillUsePacket`、`SkillChainPlan`、`CastExecutionSession`、`SkillSegmentRunner`、`ReleaseBatch`。

未改、由投影间接复用：`ChainSteps`、`TechniqueChain`、`SkillSegmentMath`、`CompiledReleaseStep`、`CompiledCastPlan`、`CastPlanContext`。未改 B 的 `CastFrame`、`LaunchAim`，未改 C 的 `DomainService`。只调用已有的 `CastFrame.execution` 字段和 `DomainService.tryWave` / `tryWaveReserved` 新重载。

## 捕获例子与开域费用隔离

编译只看角色，不按术式 id 特判。标记角色是 `ChainRole.RULE_DOMAIN_BIND`。只有标记存在时才校验它的紧邻前驱。合格领域：`domain()!=null`、`baseDamage<=0`、不是附魔。合格攻击：`baseDamage>0`、不是附魔、角色 `ATTACK`，且不是领域、守护、屏障、条件或 `SELF_STATUS`。任一结构错误拒绝整次释放，`SkillUsePacket` 用 `§c` 加上 `faults().get(0)`，在扣费之前返回。

- 领域、绑定、修饰或附魔、攻击：投影保留领域以及领域之前的修饰和附魔。冻结计划是绑定之后到该攻击，不含标记本身。按键账单只含投影链。该攻击不再成为后续按键步。
- 领域、绑定、双重、A、附魔、B、尾部：成员恰好是 A 然后 B。B 延迟 `CastExecutionSession.STEP_GAP`（5 tick）。B 之后的尾部留在投影里，成为下一次按键步。第三个攻击不捕获。只有第一处双重的 `pullAfterA` 扩大名额，名额上限 2。后一个双重不扩大名额。
- 双重、领域、绑定、攻击：投影里的双重只能把领域当普通成员，拉不到已捕获的攻击。
- 领域、附魔、绑定：原文「领域绑定必须紧接领域术式，中间不能夹附魔或其他术式」。不扣费，同链其他领域也不开。
- 绑定前面没有紧邻领域：原文「领域绑定必须紧接在领域术式之后」。
- 屏障、守护、冲刺、自身、条件或第二个绑定挡在攻击前：原文「领域绑定不能越过「名字」继续寻找攻击」。
- 后面没有合格攻击：原文「领域绑定后面没有可绑定的攻击」。
- 双重只有一个合格攻击，或第二个宿主不合格：原文「双重释放后面缺少第二个攻击」。
- 捕获扫描范围内遇到已移除规则：原文「链里有已移除的术式，请重新装配」。范围外仍走原有警告并跳过。
- 多个互不重叠的「领域、绑定、子链」可以并存。子计划里再出现领域宿主则按「不能越过」拒绝。

聚能只对冻结子技能调用 `SkillChainPlan.frozenChainInput`，领域几何和领域链能量不乘进子链。`BoundMember.chainInput` 在编译时写入。

按键账单是投影链的 `pressCommitCost` 或手动会话的 `groupBill`。自动账单是冻结计划另一次 `GroupBudget.reserve(..., false)`。绑定规则本身不在冻结可执行列表里，费用为 0。双重把 `CompiledReleaseStep.groupBill` 的规则份额加到成员 0，与手动会话把 `ruleShare` 加进第一份相同。一分为三把报价乘进这一份，不为每个复制体再扣。自动预留不足返回已完成、未取消、未调度的 `ClosedRound`，不生成，不发魔力不足，领域保留。费用为 0 时仍拿 `charge` 返回的零收据。双重编译失败时不预留，直接 `ClosedRound`。

三处开域都传同一份 `BoundReleasePlan`：`SkillUsePacket` 的领域步、`SkillChainPlan.fireDualSequence` 的立即与延迟 `tryWave`、`CastExecutionSession.commitMember` 的 `tryWaveReserved`。有计划时后缀换成 `bind:<原始领域下标>:<本次释放UUID>`。无计划时后缀保持原组 id 或原空后缀。同一释放、同一原始领域下标共用一个声明；不同领域下标不合并。`plan != null` 且后缀空时由 C 的 `missingBindingSuffix` 拒绝放置，A 不另造放置。

## 实际复用路径

自动轮报价复用 `CompiledReleaseStep.compile(...).groupBill()` 的份额字段，或非双重的 `pressCommitCost`。生成复用 `HeldSwordGate.allows`（当时主手，不是冻结武器）、`copiesBefore`、附魔解析、伤害公式、`ReleaseBatch.openGroupTally`、`SkillSegmentRunner.commitHostGroup` / `runSegment`、`FieldZones.bindBorn`、`HostCast`、`ChainScheduler`。接触原点与接触朝向传 `null`，避免 `commitNow` 把散射和垂落压成接触点。不调用 `CastExecutionSession.open`、`CastCooldown.acquire`、`ChainProgress.advance`，不把持续槽绑成手持，不发按键包，不裸 `spawn`。

装备资格在每次真实生成前用当前主手检查。效果收到的是 `weaponCopy()` 的副本。宿主组在进入前提前检查令牌；松散复制在每一份前提前检查令牌和主手。

## 延迟、预算与准星替代

成员间隔是 5 tick，不是 20。第二成员在第一成员失败后仍会调度；只退该成员尚未提交的份额。手动双重会话仍会在第一成员失败时中止整组，这条没改。

`OpenRound.cancel` 与 `finish` 都用 `AtomicBoolean` 只结清一次。`cancel` 置取消标记。`GroupBudget.settle` 再幂等一次。异常先 `finish` 再抛出，不吞成成功。`ClosedRound` 一开始就是 `done`，`cancel` 为空操作。缺魔、空成员、令牌已失效、没有服务器都走它，且发生在预留之前或预留失败之后。令牌在调度回调和每一份生成前再查。调度器与领域 tick 同为 `Phase.END`，先后未假设。已生成的一份不退；任一份成功就保留该成员整份份额。

`ReleaseExecutionContext` 为 `DOMAIN_AUTOMATIC` 时：不调用 `LockTargetService`，不给目标发光。`stillValid(localTargetId)` 失效则用 `baseDirection`，不取消整轮。落点中心是 `origin()`，地面高度保持 `NEGATIVE_INFINITY`。种子是 `roundSeed ^ (memberIndex << 16)`。专用瞄准在链本身锁定且目标仍有效时，从 `origin()` 朝目标中心，不从眼睛。规则几何先走 `CastFrame.forAttack`，再把 `frame.execution` 设上，并用体积 `clampPoint` 收回原点和锚点，置 `overrideGeometry`。垂落视线强制 `(0,-1,0)`。原点为空且垂落时，先抬 8 再夹紧。没有场地锚点时牵引锚点用 `origin()`。

`ReleaseBatch.crosshairCenter` 与 `resolveAnchor` 在自动上下文下直接返回 `origin()`，不射线、不写锚点缓存。手动的 `LivingCandidateQueries.targetQuery` 路径保留。

当前攻击 `chargeTicks` 默认 0，没有新蓄力玩法。将来大于 0 时，自动轮用同一令牌延迟，再打开 `chargeReady`，避免内层再排一次或被 `ChargeGate` 代际取消；该延迟路径会按原 `WaterMomentum` 预留并提交。手动 `ChargeGate` 路径未改。

## 未完成与阻塞

`backup-A/` 没有写入。实施前的 `Copy-Item` 被 CLI 允许列表拒绝，按任务未换壳重试。续写时已无法保证五个改动文件的逐字节原内容，因此没有伪造备份。

命名门命令 `powershell -NoProfile -File tools/check-name-gate.ps1` 同样被拒绝，记为未跑。没有改名。

未改 B、C 的文件。若统一编译暴露他们的接点缺口，需要他们改自己的文件。

## 未验证

未跑 Gradle、`compileJava`、测试、打包、游戏。待统一编译。不宣称绑定、自动轮、退款或准星替代已经验证。
