我先完整读取任务文件和提示文件，按它们写明的范围执行。任务是只读收集现有攻击、动作和链结算事实。我先看本批记录，再沿横劈、释放包和动作接口定向查调用链。现有横劈、空间切割和冲刺已经把「独立命中体」「宿主组提交后的视觉动作」「动作窗口再带后续攻击」分成三条链。回旋转身、按姿态出刀，以及不带伤害的协同选分身，代码里都还没有对应术式。

## 独立攻击最小登记

横劈是完整单人攻击，稳定 ID `sword_horizontal_slash`。

- 术式：`D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\ModTechniques.java` 第 55–58 行，`Technique("sword_horizontal_slash", "横劈", TechniqueType.SWORD, 4.0f, 20, 20, ...)`。效果在第 353 行登记 `SwordHorizontalSlashEffect`。
- 释放：`ReleaseDefinitions.java` 第 16 行  
  `cohortSword("sword_horizontal_slash", HIT_VOLUME, TRIGGER, STRAIGHT, false, 4, 0f, 0.5f, 0, false, false, false, false, ATTACK, false, false)`。  
  含义是静止、寿命 4、速度 0、厚度 0.5、不接受目标或路径覆盖、`ChainRole.ATTACK`、`trajectoryMutable=false`、不操纵世界剑。`cohortSword` 把 `separatesCohortVisuals` 设为 true（同文件第 68–77 行）。
- 效果：`SwordHorizontalSlashEffect.execute` 第 37–91 行。`TechniqueHitSpec.path = STILL_AIM`，`speed = 0`，`length/width` 读 `boxDepth`/`boxWidth`（默认 3 和 5），贴图 `horizontal_slash.png`，寿命读定义。`TechniqueHitSpec.spawn(ServerLevel, ServerPlayer, TechniqueHitSpec)`。
- 同方法第 41–45 行把实体触及距离写入局部 `width`，随后的 `spec` 没有使用它。
- 宿主组白名单只有 `space_cut`：`ReleaseRegistry.java` 第 12、30–34 行。横劈不走 `submitsHostGroup()`，因此不会登记身体动作。

空间切割是更新的独立静止斩，ID `space_cut`，仍是攻击而不是动作前缀。

- `ModTechniques.java` 第 109–112、365 行：底伤 4、冷却 20。
- `ReleaseDefinitions.java` 第 31 行：寿命 10、速度 0、厚度 0，其余链标志与横劈相同，`trajectoryMutable=false`。
- `SpaceCutEffect.execute` 第 41–107 行：同样 `STILL_AIM`；非自动且没有 `ReleaseBatch` 时锚点是眼睛沿视线 4 格；水平朝向作刀面法线。仅当 `HostGroupContext.current()` 非空时调用 `registerAction(OriginalHumanoid.SPACE_CUT_ID)`（第 70–73 行）。

构造签名：`Technique(String techniqueId, String name, TechniqueType type, float baseDamage, int baseCooldown, int levelCap, String description)`，`Technique.java` 第 30–32 行。

## 动作如何带出后续攻击

链上动作角色只有冲刺。`ReleaseDefinitions.java` 第 15 行：`movement_dash` 为 `ReleaseKind.ACTION`、`ChainRole.ACTION`、寿命 10、速度 2、`trajectoryMutable=false`。`ModTechniques.java` 第 65–68 行底伤 2。跳跃 `movement_leap` 底伤 0，没有释放定义；`SkillSegmentMath.roleOf` 在定义缺失时返回 `ATTACK`（第 86–91 行），效果只改速度，不打开窗口（`MovementLeapEffect.java` 第 9–16 行）。

按下后的动作分支在 `SkillUsePacket.java` 第 629–677 行：

- `SkillChainPlan.collectModifiers` 从动作下一段找攻击。
- 没有双重规则时：`AttackBudgetWindow.open(...)`，再 `ChainScheduler.schedule` 到 `SkillSegmentRunner.playAction(player, action, techniques, actionIndex, weapon, window, actionInput, planContext)`。
- 后面有双重规则时：不走接触窗口，改用 `buildSegmentRunner` 的 `onFirstTrigger`，触发后调用 `SkillChainPlan.firePlan(...)`。

`playAction` 签名在 `SkillSegmentRunner.java` 第 435–443 行。它要求效果已登记，然后以伤害 0 调用 `runSegment`，把 `ActionWindow.Sink` 放进施放上下文。

`ActionWindow.Sink`（`ActionWindow.java` 第 15–25 行）：

- `void onContact(UUID targetId, Vec3 point, Vec3 direction)`
- `void onEnded()`
- `void onFinished()`，默认空
- `boolean closed()`

冲刺在 `MovementDashEffect.java` 第 83–147 行每 tick 做身体扫掠，命中调用 `sink.onContact`，寿命结束调用 `onEnded` 和 `onFinished`。`AttackBudgetWindow.onContact`（第 77–82 行）每次接触都 `SkillChainPlan.firePaidPlan(...)`；`onEnded`/`onFinished` 只退还未取走的魔力份额，没有接触就不会放出后续攻击。

双重成员间隔是 `MulticastExpansion.MEMBER_GAP_TICKS = 3`。旧双重步进常量 `SkillChainPlan.DUAL_STEP_TICKS = 5`（第 1141 行）。

## 姿态数据与空间切割拔刀

服务端播放入口：

- `ActionRuntime.play(ServerPlayer, ResourceLocation, float speed, boolean looping, Float frozenYaw, UUID parentGroupId)`，第 34–36 行。注释写明替换姿态不会取消已经提交的攻击几何。
- `ActionRuntime.notifyAttackGroup(ServerPlayer, CommitPermit, ActionCommit)`，第 42–52 行。无许可的重载直接拒绝（第 38–40 行）。
- `ActionCommit.group(UUID groupId, ResourceLocation actionId)`，`ActionCommit.java` 第 52–56 行。

空间切割的实际衔接是宿主组提交成功之后：`HostGroupCommit.commitOnce` 第 77–88 行读 `context.actionCandidate()`，调用 `notifyAttackGroup`。视觉异常不回滚攻击组。`HostGroupContext.registerAction` 第 98–102 行同组只保留第一个 ID。

`OriginalHumanoid.registerSpaceCut` 第 70–101 行：

- ID `xueximod:space_cut`，时长 0.5 秒，混合 0.06 秒，只旋转 `Body/Head/ArmRight/ArmLeft`。
- 服务端片段是这些骨的保持帧，不包含拔刀关键帧。
- `TimeNotice(0.15F, "draw-release")`。状态说明写「不推迟伤害」。
- `draw-release` 和水平兜底的 `slash-mid` 只被放进通知列表再 `drainNotices()`，没有玩法消费者按这两个 ID 延迟命中。

客户端姿态只认这个 ID：`SpaceCutSampler.sample(Player, float)` 第 45–64 行，资源 `actions/space_cut_grip_fast_v6.json`，9 骨在 Blockbench 空间合成。`SpaceCutScope.push` 第 14–20 行只在本次绘制期间持有帧。`ActionVisualCatalog` 第 176 行把该 ID 标成上身斩击视觉。

水刃兜底 `fallback/water_blade_horizontal` 有身体和右臂旋转关键帧（`OriginalHumanoid.java` 第 33–55、122–131 行），目录在 `ActionVisualCatalog.java` 第 173–175 行。它没有接到 `sword_horizontal_slash`。

代码里没有可复用的转身、回旋或按出刀帧释放任意后续攻击的术式。

## 链结算

`ChainRole` 在 `ChainRole.java` 第 6–26 行。`SkillSegmentMath.isChainModifier` 第 94–105 行包含索敌、弹道、一分为三、双重、错开和命中接续。`ACTION` 不是修饰角色，单独走上面的窗口。

`collectModifiers`（`SkillChainPlan.java` 第 233–247 行）在遇到正底伤、非附魔术后停止，并累计：`SEEK_SPACE`、`SEEK_ENTITY`、`TRAJECTORY_DROP`、`TRAJECTORY_TRACKING`、`TRAJECTORY_SCATTER`、`RULE_TRIPLE`（`copies *= 3`）、`RULE_DUAL`。执行期 `scanRoles` 从攻击往前扫到上一个正底伤（`SkillSegmentRunner.java` 第 825–858 行），不把 `ACTION` 扫成几何开关。

- 载体：`CastExecutionSession` 持有 `ItemStack carrier`；攻击结算用主手和 `weaponBonus`。`firePaidPlan` 第 296–300 行用 `configuredSegmentDamage(...)`。
- 附魔：无领域的附魔段在 `SkillUsePacket.java` 第 586–596 行直接 `imprintEnchant` 后跳过。有攻击时，`firePaidPlan` 第 297–301 行 `resolveSegmentEnchantments` 后 `carries` 为真才把附魔放进该段。没有攻击可挂时，`imprintEnchant` 第 816–844 行记到主手。
- 一分为三：`rule_triple_cast` 底伤 0，`ReplicationSpec.of(3)`，效果为空。`copies > 1` 时 `openGroupTally()`（第 390 行）。`FlowRoundLedger.aggregatedDamage` 第 59–64 行是单份伤害乘命中份数；`FLUSH_DELAY = 6`（`FlowRoundLedger.java` 第 17 行），一次 `playerAttack`。
- 双重：`rule_dual_release` 底伤 0，`MulticastSpec.fixed(2)`，效果为空。它按后续格顺序释放，不是分身站位。
- 零伤规则本身不生成斩击。聚能 `rule_gather_energy` 虽是 `ChainRole.ATTACK`，但 `chainEnergyScale` 为 2，因此仍算链修饰。

`trajectoryMutable=false` 时，`CastFrame.forAttack` 第 114–123 行：没有落点指定、没有交错、也没有可变路径替换时，`overrideGeometry=false`，效果自己的起点和朝向保留。有落点或交错时走 `slashGeometry`：改生成点，视线方向保留，无法线时刀面取水平法线，非交错时 `roll=0`、`pose3d=false`。垂落、涡旋等路径替换要求 `trajectoryMutable`（第 112 行）。`TechniqueHitSpec` 第 247–280 行在 `overrideGeometry` 且静止时，把锚点和朝向改成帧的原点和刀面法线。接触点会在 `runSegment` 第 730–741 行强制覆盖几何。横劈和空间切割的 `acceptsPathOverride` 都是 false，固定斩击不会被改成弹丸路径。

## 配置与性能预算

标准键由 `BalanceCatalog.sealAfterContent` 补齐，签名见 `docs\配置平衡指南.md` 第 253–273 行。横劈和空间切割已在伤害倍率名单（指南第 119 行）。专项只消费真实读到的键：

- 寿命：`motion(..., includeLife=true, includeSpeed=false, includeThickness=false)`，`ReleaseDefinitions.java` 第 352、362、395–416 行。速度为 0 不登记 `motionSpeed`。
- 斩击盒：`SwordHorizontalSlashEffect.declareBalance` 第 29–34 行，`boxDepth` 默认 3、`boxWidth` 默认 5。空间切割同形，第 32–37 行。读取用 `SpecialBalance.number(id, key, default)`。
- 指南第 107 行：改 `damageMultiplier` 不改默认 `manaCost`。

已有预算常数：

- `CastPlanCache`：链长 64、缓存项 64、单元 16384（第 25–27 行）。超限走旧路径。
- `LivingCandidateQueries`：每 tick 每世界 4 条候选、256 个生物、边长 48、外扩 8（第 32–35 行）。超限走原版查询。
- `SkillEntityCount` 默认 1，测试上限 4096。这是全局生成倍率，不是协同人数。
- `PerfMetric` 只记 inclusive 耗时，没有单独的新术式 CPU 配额。

## 三项各自能复用什么

回旋、可复用动作：已有 `ChainRole.ACTION`、`ActionWindow.Sink`、冲刺的接触或结束时序，以及与术式 ID 分开的 `ActionRuntime`。没有转身或回旋术式，没有按姿态时刻放出后续任意攻击的实现。冲刺自己有底伤 2。`draw-release` 不参与命中时序。

独立斩击：已有横劈和空间切割两套完整单人 `HIT_VOLUME`/`ATTACK` 登记、静止斩盒、统一费用冷却和 `trajectoryMutable=false` 几何。记录要求不要把现有横劈当成这次的新斩击。新 ID 仍要自己的 `Technique`、释放定义、效果和专项声明。空间切割的拔刀姿态绑死 `xueximod:space_cut`，而且只在宿主组提交之后播放。

协同、不自带伤害：已有底伤 0 的规则，双重只重放后续格，一分为三只增加后续攻击份数并共用 6 tick 账本。两者都不选分身、不分配站位或朝向、不调度短距归位。新规则角色要进入 `ChainRole` 和 `isChainModifier` 才会被现有收集器看见；这一步还没有现成角色。

## 最小扩展点与同文件所有权

新攻击效果、新动作效果可以落在新类里。下面这些文件已经是多术式共用入口，三项若同时改会冲突，每份文件只宜一个作者：

- `ModTechniques.java`、`ReleaseDefinitions.java`：登记和专项声明。
- `ReleaseRegistry.java`：宿主组白名单目前只含 `space_cut`。
- `ChainRole.java`、`SkillSegmentMath.java`、`SkillChainPlan.java`、`SkillUsePacket.java`、`SkillSegmentRunner.java`：角色收集、动作窗口和结算。
- `CastFrame.java`、`TechniqueHitSpec.java`：`trajectoryMutable=false` 的生成点与刀面。
- `OriginalHumanoid.java`、`ActionVisualCatalog.java`，以及 `client\action\spacecut\`：姿态只服务空间切割。

## 交给 Astra 的未决点

1. 新斩击使用新的稳定 ID，还是改现有 `sword_horizontal_slash`。代码里的横劈已经能单独释放。
2. 回旋沿用 `ChainRole.ACTION` 加接触窗口，还是要让 `TimeNotice` 真正决定出刀。现在通知不推迟伤害，冲刺也带自己的伤害。
3. 回旋底伤是否为 0。现有动作范例冲刺底伤是 2，跳跃底伤是 0 但不是 `ACTION`。
4. 协同是新 `ChainRole`，还是复用双重或一分为三。后两者会改变后续攻击的次数或账本，并且不会摆分身。
5. 短距归位和朝向用玩家 `ActionRuntime`，还是用分身动作接口。分身预算在另一份事实任务里。
6. 新斩击是否进入 `submitsHostGroup`。只有进入后，提交成功才会通知身体动作，而且现在的采样器只接受空间切割 ID。
7. 合击是否维持「每组一份 `Tally`、6 tick 后按命中份数打一次」。两人同时出现不会自动再乘一次伤害。

未编译，未做游戏验证，未改表，未写源码。

<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"只读事实：横劈与空间切割已是 trajectoryMutable=false 的独立 HIT_VOLUME/ATTACK；冲刺是唯一 ChainRole.ACTION，靠 ActionWindow 接触或双重 onFirstTrigger 带出后续攻击，姿态通知不推迟伤害。没有回旋术式，也没有不带伤害、会选分身和摆位的规则。双重与一分为三只改变后续攻击次数和 6 tick 共享账本。未编译、未游戏验证、未写文件。","changedFiles":[],"checks":["读取本批记录与 facts-chain-actions 任务范围","核对 ReleaseDefinitions/ModTechniques 中横劈、空间切割、冲刺、双重、一分为三登记","核对 SkillUsePacket 动作分支、SkillSegmentRunner.playAction/runSegment/scanRoles、AttackBudgetWindow、MovementDashEffect","核对 ActionRuntime.notifyAttackGroup、HostGroupCommit、OriginalHumanoid.registerSpaceCut、SpaceCutSampler","核对 CastFrame.forAttack 与 TechniqueHitSpec 在 trajectoryMutable=false 时的生成点和刀面","核对 BalanceCatalog/配置平衡指南专项消费，以及 CastPlanCache、LivingCandidateQueries、SkillEntityCount、FLUSH_DELAY"],"notRun":["compileJava","游戏验证","制作表迁移","启动器与批次额度"],"writesSettled":false,"blocker":null,"nextAction":"把本份接口事实交给唯一 Astra 做本批设计第 1 轮；未冻结前不要并行改上述共用文件。"}
<<<END_CURSOR_TASK_REPORT>>>
