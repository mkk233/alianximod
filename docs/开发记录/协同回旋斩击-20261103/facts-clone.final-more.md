我先读取原任务和这次的补充提示，只按它们写明的范围补事实。补查只看术式类型、`clone_body` 的登记与付费入口，以及普通玩家装配门和 `CloneService` 上现成的预算签名。`TechniqueType` 只有四项。`clone_body` 是开发用法术，普通玩家没有一条能把它自己部署出去的载体路径。分身状态里能认出普通 `REPEAT`，没有“可协作”字段。

## TechniqueType

`D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\TechniqueType.java` 第 10–14 行，完整枚举是 `SWORD`、`MOVEMENT`、`MAGIC`、`UNIVERSAL`。

`canUseForSwordSkill()` 只接受 `SWORD` 或 `UNIVERSAL`。`canUseForMagicSkill()` 只接受 `MAGIC` 或 `UNIVERSAL`。

## clone_body 登记、归类、报价

登记在 `CloneTechniqueRegistry.bodyTechnique()`，第 74–87 行，走 `Technique` 的十一参数构造：

```54:57:src/main/java/com/bslx/XueXiMod/shu/Technique.java
public Technique(String techniqueId, String name, TechniqueType type,
                 float baseDamage, int baseCooldown, int levelCap, String description,
                 int complexity, ShuAttribute attribute, float projectileSpeed, boolean enchantment,
                 TechniqueAvailability availability)
```

实际参数是 `clone_body`、显示名「普通分身·基础」、`TechniqueType.MAGIC`、`RAW_SPAWN_DAMAGE` 0、冷却 20、等级上限 1、`spawnDescription()`、复杂度 0、`ShuAttribute.NONE`、弹速 0、附魔 `false`、`TechniqueAvailability.DEVELOPMENT_ONLY`。

释放定义在 `CloneReleaseDefinition.registerAll()` 第 27–28 行：`ChainRole.ACTION`，`ReleaseKind.ACTION`，`ReleaseTag.TRIGGER`，显式魔力 `SPAWN_MANA_COST`（10）。`Technique.manaCost()` 在释放定义给出非负显式魔力时返回该值。

生效生成报价是 `CloneBalance.readSpawn(BalanceSide side, CloneKind kind)`。`NORMAL` 读 `clone_body`。代码默认是魔力 10、冷却 20 刻、寿命 0、生命 20、诱敌半径 8。专项键只有 `lifetimeTicks`、`maxHealth`、`decoyRadius`；标准魔力与冷却由封存读取，这里不重复声明。返回 `CloneSpawnQuote(techniqueId, kind, manaCost, cooldownTicks, lifetimeTicks, bodyHealth, decoyRadiusBlocks)`。

`CloneService.specFor` 对 `CloneKind.NORMAL` 写成 `ResponseBudget.REPEAT`、`endAfterBudgetSpent=false`、动作键 `clone_response_pulse`、`MANUAL_AND_HURT`、`STATIONARY`、`TerminationPolicy.BUDGET`、`LEGACY_HUMANOID`、部署标识 `null`。

## 开发部署与付费入口

全仓只有 `CloneCommands.spawn(CommandContext<CommandSourceStack>, CloneKind)` 调用 `CloneService.spawn(ServerPlayer owner, CloneKind kind)`。命令要求 `CommandSourceStack.hasPermission(2)`，字面量是 `clone spawn normal`。

`spawn` 先调用包内 `CloneRuntime.reserve(UUID ownerId)`，再 `ManaPaymentReceipt.charge(ServerPlayer player, int cost, boolean tellPlayer)`，金额是报价魔力，`tellPlayer=true`。未发布则 `receipt.refund(ServerPlayer)` 并 `releaseReserve(UUID)`。

`developmentOnly(CloneKind)` 要求身体术式和脉冲都仍是 `DEVELOPMENT_ONLY`，否则 `spawn` 拒绝并给出 `DEVELOPMENT_ENTRY_ONLY`。玩家技能链不会调用 `spawn`。若链执行到效果，`CloneTechniqueRegistry.registerDefense` 登记的是 `TechniqueEffect.execute(ServerPlayer, Technique, ItemStack, TechniqueCastContext)`，实现直接抛 `IllegalStateException(DEVELOPMENT_ENTRY_ONLY)`。`SkillSegmentRunner.runSegment` 在第 765 行调用这个 `execute`。

## 普通玩家装配门

没有一条玩家载体路径会部署独立的 `clone_body`。现有门按拒绝顺序是：

- `TechniqueAccess.allowsPlayerUse(Technique)`：可用性不是 `DEVELOPMENT_ONLY` 才为真。`playerCatalog()` 和 `TechniqueManager.playerCatalog()` 都走它。`skillAllowed(Skill)` 遇到已登记的 `DEVELOPMENT_ONLY` 返回假。
- `PlayerShuData.learnTechnique(String)` 对 `DEVELOPMENT_ONLY` 直接返回，不写入已学。`isUnlocked(Technique)` 对法术是 `magicLevel >= complexity`；`clone_body` 复杂度是 0，但学不会就不会进装配。
- 装配第一检也是 `allowsPlayerUse`。`SpellAssemblyMenu.rejectReason` 第 355 行、`SkillAssemblyPacket.put` 第 127 行失败时返回「该术式只属于开发入口」。
- 类型门在可用性之后。法术不能进剑技槽，`rejectReason` 第 386–387 行返回「无法装配：法术不能装入剑技槽」。法术的现有载体是法杖：`StaffSkills.asSkill(ItemStack)` 编成 `new Skill("法杖", SkillType.MAGIC, techniques, embryos)`。法杖还要求 `hasLearned` 且 `isUnlocked`。
- 释放前 `StaffSkills.castRefusal(ServerPlayer, ItemStack)` 第 137 行再次 `allowsPlayerUse`；`SkillUsePacket` 第 195 行再查 `skillAllowed`。

这些门都不调用 `CloneService.spawn`，也不走水幻形捕获。

## 区分普通 REPEAT 与预算签名

没有名为协作、可协作或 cooperative 的字段、枚举或能力。能认出开发普通分身的是现有规格和状态：

- `CloneSpec.budget()` 为 `ResponseBudget.REPEAT`，`endAfterBudgetSpent()` 为假，`kind()` 为 `NORMAL`，`techniqueId()` 为 `clone_body`。
- `CloneRuntimeState.budgetSpent()` 在 `REPEAT` 提交后仍为假。响应状态是 `READY`、`RESERVED`、`COMMITTING`、`COOLDOWN`、`SPENT`。
- `CloneService.status` / `list` 给出的 `CloneResults.Status.responsesRemaining`，`REPEAT` 时是 `REPEAT_CHARGES`（-1）。`ONE` 未花是 1，已花是 0。

可复用的现有签名只有这些，都不是公开的“协作预算”接口：

- 生成名额：包内 `CloneRuntime.reserve(UUID ownerId)`、`reserve(UUID ownerId, boolean respectSpawnCooldown)`，成功返回 `null`，否则 `RejectReason`。`releaseReserve(UUID ownerId)` 退名额。这是主人 4、服务器 128 的身体名额，不是响应次数。
- 响应预留：公开 `CloneService.request(ServerPlayer owner, CloneHandle handle, CloneTriggerInput input)`。接受后把 `response` 设为 `RESERVED` 并写入 `pending`。
- 提交：包内 `CloneService.perform(CloneRuntime runtime, CloneActionRequest request)`。过提交线时 `ONE` 才把 `budgetSpent` 设为真；`REPEAT` 进入 `COOLDOWN`，不结束身体。`crossCommitLine`、`settleCommitted`、`restoreReady` 是私有的。
- 取消：包内 `CloneService.cancelQueued(CloneRuntime runtime, CloneActionRequest request)`。未过提交线时清 `pending`，`RESERVED` 回到 `READY`。`dismiss`、`dismissAll`、`end` 结束身体并取消未执行请求。

`CloneTriggerPolicy.rejection(CloneTriggerInput, CloneRuntimeState)` 在 `budgetSpent` 或 `SPENT` 时返回 `BUDGET_SPENT`。水幻形不走这套 `request`。

<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"补齐 TechniqueType、clone_body 登记报价与开发付费入口、玩家装配门和 REPEAT 预算签名；无协作字段，无源码写入","changedFiles":[],"checks":["TechniqueType","CloneTechniqueRegistry.bodyTechnique","CloneBalance.readSpawn","CloneService.spawn/request","TechniqueAccess 与法杖装配门"],"notRun":["未实现","未编译","未游戏验证","未改表","未正式复审"],"writesSettled":true,"blocker":null,"nextAction":"将本补查事实交给唯一Astra做本批设计第1轮，不实现、不把开发术式改成玩家可用"}
<<<END_CURSOR_TASK_REPORT>>>
