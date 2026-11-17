# 作者 A 核心候选源码报告

状态：**待统一编译**。**游戏未测**。本目录是候选源码，包名是正式生产包 `com.bslx.XueXiMod.shu.technique.release.clone.core`，尚未按原字节接入 `src/main`，不能当成已经在跑的运行源码。

本单没有跑 `gradlew`，没有打包，没有启动游戏，没有改 `implementation-contract.md`，没有改 `src/main`，也没有改其他作者的文件。官方命名门禁没有跑：`tools/check-name-gate.ps1` 只扫描真实 `src`。候选源码的统一门禁留到接入之后。

## 写入路径

只写了下面 20 个 Java 文件，根目录是：

`docs/开发记录/分身底座-20261102/候选源码/src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/core/`

- `CloneKind.java`
- `CloneLifeState.java`
- `CloneResponseState.java`
- `CloneEndReason.java`
- `CloneSpec.java`
- `CloneHandle.java`
- `CloneActorFrame.java`
- `CloneTriggerInput.java`
- `CloneActionRequest.java`
- `CloneActionContext.java`
- `CloneResults.java`
- `CloneSourceScope.java`
- `CloneRuntimeState.java`
- `ClonePositionPolicy.java`
- `CloneTriggerPolicy.java`
- `CloneEntity.java`
- `CloneDecoyPolicy.java`
- `CloneRuntime.java`
- `CloneService.java`
- `CloneLifecycleEvents.java`

另外两份交付是本批的 `author-A-core-report.md` 和 `author-A-core-evidence.json`。Java 的 SHA-256 与最后写入时间列在文末；报告自身的哈希只放在证据文件里。

## 已实现的能力

两类驻留分身都走同一条生成事务。落点是主人脚下，朝向用服务器当时看到的主人视线。先查开发入口、已加载、世界边界和实心方块，再占槽，然后 `CloneBalance.readSpawn(BalanceSide.SERVER, kind)`。报价通过后才 `ManaPaymentReceipt.charge`。`addFreshEntity` 之前绑定不可伪造的出生票。索引发布成功才写主人共用的生成冷却，并给 `CLONE_SPAWNS` 加 1。发布前失败会放回槽位，并对这张收据退款一次，不写冷却。

幻影是 `ONE` 且提交后结束。普通是 `REPEAT`。生命状态和响应状态分开。请求先变成 `RESERVED` 且不扣钱，`executeTick = requestedTick + 1`，过 `expireTick` 则取消并计一次拒绝。执行时先准入、再取帧、再 `prepare`。A 校验魔力、冷却和伤害之后才扣款。收据只留在 A。跨过提交线时才写 `COMMITTING`、动作令牌、幻影额度、至少 1 tick 的冷却和动作序号，然后打开 `CLONE_RESPONSE` 深度 1，再 `commit()`。提交线之后不退款。`finally` 先关作用域；实体已进入 `ENDING` 或 `ENDED` 时不恢复 `READY`。

受击只在 `super.hurt` 返回 true 且分身仍存活之后排队。来源作用域、主人、其他分身、不能伤害主人的玩家、主人的驯养物、与主人同队的非玩家，在伤害之前拒绝。环境伤害不在这里拒绝，也没有反击请求。

诱敌由运行态按 20 tick 错开调用。半径用生成时冻结值，0 则不查询、不计 `CLONE_DECOY_PASSES`。查询用 `LivingCandidateQueries.entityQuery`，粗筛是 `Enemy` 并且是 `Zombie` 或 `AbstractSkeleton`。之后再看距离、`canAttack`、视线和当前目标。只对无目标或正在追主人的个体 `setTarget`。每 tick 最多 4 次查询，每次最多看 32 个、新吸引 8 个，每个分身最多记 8 个 UUID。结束时只清仍把目标指回该分身的记录。

停服、登出、死亡、换维、离开世界和世界卸载都会结束对应分身，重复结束只计一次 `CLONE_ENDS`。存档读入把生命标成 `ENDED` 并丢弃，不能变成 `ACTIVE`。会话按 `MinecraftServer` 对象身份隔离。单调 tick 在 `START` 加一，不用世界时间。队列、分身和主人记录都空时，`END` 第一行返回，不 `begin` `CLONE_TICK`。

显示名由 A 写成「幻影分身·基础」和「普通分身·基础」。成功请求的说明固定是「请求已接受」。主动执行失败才由 A 发一条系统消息；当场拒绝把说明交回调用方；受击失败不发聊天。

## 接口依赖

A 调用 B，不读 B 的进行中源码：

- `CloneBalance.readSpawn`、`CloneBalance.BODY_HEALTH`、`CloneBalance.PULSE_ID`
- `CloneActionRegistry.admit` / `get`
- `CloneActionCapabilities.full()`
- `CloneAction.prepare`、`ClonePreparedAction` 的接受、报价、`commit`、`cancelBeforeCommit`

A 调用 C，不读 C 的进行中源码：

- `CloneEntityTypes.cloneType()`
- `CloneTechniqueRegistry.profile` / `technique` / `pulse`
- `Technique.availability()` 与 `TechniqueAvailability.DEVELOPMENT_ONLY`
- `PerfMetric.CLONE_TICK`、`CLONE_RESPONSE`
- `PerfCounter.CLONE_SPAWNS`、`CLONE_ENDS`、`CLONE_RESPONSE_REQUESTS`、`CLONE_RESPONSE_COMMITS`、`CLONE_RESPONSE_REJECTIONS`、`CLONE_DECOY_PASSES`

既有代码只使用 `ManaPaymentReceipt.charge` / `refund`、`DamagePermission.allows`、`LivingCandidateQueries.entityQuery`、`PerformanceRecorder.begin` / `end` / `add`，以及 `XueXiMod.MOD_ID`。没有调用 `FakePlayer`、`ChainScheduler`、`ReleaseBatch.Tally` 或 `classifyRoot`。

B 可以读 `CloneActionContext`、`CloneActorFrame`、`CloneEntity.rejectsPulseTarget` 和 `CloneResults.RejectReason`。C 可以读 `CloneService` 的生成、请求、撤回、结束、状态、列表，以及 `CloneEntity.createAttributes` 和同步状态。

## 配置消费者

A 不声明平衡键，也不再抄一套魔力、冷却或伤害默认值。生成只在服务器侧读 `readSpawn`。读到的魔力用于这次 `charge`，冷却在索引发布成功后写入主人生成冷却，寿命、`bodyHealth` 和诱敌半径冻进 `CloneSpec`。`bodyHealth` 只在出生时写入最大生命和当前生命，之后不每 tick 回写。属性模板里的 `MAX_HEALTH` 用 `CloneBalance.BODY_HEALTH`，这是合同要求的初始属性，不是另一套默认值。

脉冲不由 A 调用 `readPulse`。A 消费的是 B 冻进 `ClonePreparedAction` 的 `quotedMana`、`quotedCooldownTicks` 和 `configuredDamage`。扣款和冷却长度用这张报价，不另乘伤害倍率。

## 合同里的机械补充

这些补充不改设计语义，也没有写回共享合同。

1. 增加 `CloneService.handleFor(ServerPlayer, UUID)`。命令侧不能自己编会话或代次。代次从 1 起，0 对不上任何存活分身。
2. `ClonePreparedAction` 没有射程访问器。A 只核对魔力 0..1000000、冷却非负、伤害有限且非负。射程仍由 B 在 `prepare` 里用同一次 `readPulse` 的 `rangeBlocks` 判断。合同第 5 节「射程有限」因此没有落在 A。
3. 第 5 节散文写 `CloneActionCapabilities.FULL`，第 6 节签名是 `full()`。实现调用 `full()`。
4. `PathfinderMob.createMobAttributes()` 在 1.20.1 上是 `Mob` 的静态方法。实现按合同通过子类名调用。
5. 主动方向不合法是 `INVALID_GEOMETRY`。反击找不到攻击者是 `NO_TARGET`。来源仍是 `CLONE_RESPONSE` 或深度大于 0 时用 `BUSY`，说明是「这次响应还在提交，不能再触发」。
6. `CloneSourceScope.blocksResponse()` 是同一条件的读取口，供受伤和触发判断使用。
7. `CancelToken` 仍只有 `cancelled()`。分身进入结束时由 A 把内部标记设上，B 只能读。

## 自检与未做

对照过这些边界：生成槽位可撤销且不留下空主人记录；发布前退款只发生一次；提交线前失败会 `cancelBeforeCommit` 并退款；提交线后异常不退款；幻影提交后 `SPENT` 再 `BUDGET_SPENT`；普通冷却即使报价为 0 也至少留到下一个单调 tick；排队取消和执行失败对同一序号只计一次拒绝；生成失败不加响应拒绝计数；诱敌有实际查询、错开和预算。局部变量、参数和字段在这次阅读里没有短于 3 个字符的名字，也没有命中门禁缩写表。这不是官方门禁结果。

未做：统一编译、游戏实测、接入 `src/main`、官方命名门禁、正式实现复核。B/C 的类型还没有进入同一编译源时，这份候选不能单独编译。没有空的核心方法，也没有用 TODO 代替生成、请求、诱敌或生命周期。

## Java 哈希

算法 SHA-256，时间为本机最后写入时间。

| 文件 | 最后写入 | SHA-256 |
|---|---|---|
| CloneActionContext.java | 2026-11-02 04:10:15 | 62a5a62a271671e966e62fa99b9ad1861d2724ad2639eb894621200939695d7f |
| CloneActionRequest.java | 2026-11-02 04:10:14 | 7433764f3b1c3a2eb698520168f6d727719d7e04f173b653f67d6db8dbf98f21 |
| CloneActorFrame.java | 2026-11-02 04:10:12 | 148bd3835188bf3e4dd0ad362ecf2d06e59520fb8d7f887af4bb012ffb4be6a3 |
| CloneDecoyPolicy.java | 2026-11-02 04:13:15 | 3e41f2e4e6de0ca0cad36f810b4672e3fc2d5c3ab5ec467cf250dafd6b426f61 |
| CloneEndReason.java | 2026-11-02 04:10:07 | 8b68e1a7131c0e339fcb8edcadeb957501e1e6d2e48f2891ee4661bdd032624b |
| CloneEntity.java | 2026-11-02 04:21:48 | 1fd9cf0e75c915a99f5cb4c88e70206c8abb4128671ca716ebbc87ceafe8285f |
| CloneHandle.java | 2026-11-02 04:10:10 | ecf3e3da1989c17395ae1729fc4619e053090b07050d8d02d8cf01be1fe5a830 |
| CloneKind.java | 2026-11-02 04:10:05 | abc236aae6511c2e56fab0ebd034970f63585a7c35c1b480102be2e397b875ef |
| CloneLifecycleEvents.java | 2026-11-02 04:15:15 | 0d1eb936e2e707c1cb1fe9c04cc2ee0fe28161db12bf872773d4bb2b235d4ec2 |
| CloneLifeState.java | 2026-11-02 04:21:48 | 1477f755b85ad943df5c76fbf8fe828a57eabd2c8c3f1fe3bf1df5e2bd6d21d1 |
| ClonePositionPolicy.java | 2026-11-02 04:10:37 | 646e5b84a05928019eb4eb4911e424b684c24387b57e583975211e672baabad5 |
| CloneResponseState.java | 2026-11-02 04:10:07 | ca4254b92a901db7397b9873abba8adb6a1403a7b165acbcbf42182c509b30d8 |
| CloneResults.java | 2026-11-02 04:10:25 | d29a8de9536e171c50457854fb1c3b5dc19aaf1e2df57907490a7a7070d96558 |
| CloneRuntime.java | 2026-11-02 04:21:49 | 97868be321f3ab824e8798e86df46f20cfbb772b54014c8f917a92960d278a5e |
| CloneRuntimeState.java | 2026-11-02 04:21:48 | 495e85f06664afa21bc2c81115fbf32f781e5c11f731e1a160dba62b254abc72 |
| CloneService.java | 2026-11-02 04:23:22 | 0e39fbfc8c0f1cbf388098f214df948e342e4fa5d6b38e8d9c8d91af690d4e34 |
| CloneSourceScope.java | 2026-11-02 04:10:30 | 98ba28afd837c3c2fcb87c1a5e2793399c4ef729057db06004895c96d0136245 |
| CloneSpec.java | 2026-11-02 04:10:09 | 2d2c278dca12c4030c3dd2756f5c79ac475bc3588af37d9ac9790e6fb121aae8 |
| CloneTriggerInput.java | 2026-11-02 04:10:12 | fd5e5e7d2ec81d740488ef12fb3fa83a84d614105c55ddf3906847bd955269c0 |
| CloneTriggerPolicy.java | 2026-11-02 04:13:16 | ede8a3c1b984e9772c40dfc4b39e8a35f7180994795f821344166422c252abe6 |

## 停笔

作者 A 的核心实现单到此停止写入并释放。候选源码等待整合者按原字节接入；接入前不得把它当成运行源码。待统一编译。游戏未测。
