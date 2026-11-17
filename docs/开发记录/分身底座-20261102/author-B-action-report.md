# 作者 B 响应与配置报告

批次：分身底座-20261102。角色：响应与配置作者 B。模型：Cursor CLI grok-4.7-high-fast。阶段：实现。设计 1/2 已定稿，实现正式复核仍为 0/3。本单是作者实现和必要定向自检，不是独立测试，也不是正式复审。

候选源码使用正式生产包名，还没有接入 `src/main`。三方停笔前不能把它当成可编译闭环，也不能当成已经在跑的游戏代码。

待统一编译。游戏未测。

## 改动路径

只写了下面这些文件。没有改真实 `src/main`、A 的 `core`、C 的注册和命令、制作表、语言文件或平衡目录。

- `D:/alianximod/docs/开发记录/分身底座-20261102/候选源码/src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/action/CloneAction.java`
- `D:/alianximod/docs/开发记录/分身底座-20261102/候选源码/src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/action/ClonePreparedAction.java`
- `D:/alianximod/docs/开发记录/分身底座-20261102/候选源码/src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/action/CloneActionResult.java`
- `D:/alianximod/docs/开发记录/分身底座-20261102/候选源码/src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/action/CloneActionCapabilities.java`
- `D:/alianximod/docs/开发记录/分身底座-20261102/候选源码/src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/action/CloneActionRegistry.java`
- `D:/alianximod/docs/开发记录/分身底座-20261102/候选源码/src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/action/ClonePulseAction.java`
- `D:/alianximod/docs/开发记录/分身底座-20261102/候选源码/src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/CloneBalance.java`
- `D:/alianximod/docs/开发记录/分身底座-20261102/author-B-action-report.md`
- `D:/alianximod/docs/开发记录/分身底座-20261102/author-B-action-evidence.json`

## 真实完成的能力

`CloneActionRegistry` 按动作键登记。同一键第二次抛 `IllegalStateException`。`admit` 对未知键返回 `UNSUPPORTED_ACTION`；已登记但盖不住要求的能力时返回 `UNSUPPORTED_CHAIN`。不按三个术式编号写分支。`ClonePulseAction.register()` 登记的脉冲七项能力全真，`covers(CloneActionCapabilities.full())` 通过。其他动作若只走 `register(CloneAction)`，能力全为假，对全真要求得到 `UNSUPPORTED_CHAIN`。没有静态初始化自登记。

`ClonePulseAction.prepare` 在服务器线程上做完一次报价和选目标，不造成伤害、不生成实体、不扣魔力、不写冷却、不改次数、不改响应状态。射线原点只用帧上的 `eyeOrigin`。主动只用帧上的单位 `lookUnit`。反击只用帧上的攻击者 UUID。不读主人眼睛、主手、视线或锁定缓存。

同一次 `readPulse(BalanceSide.SERVER)` 的 `rangeBlocks` 同时用于候选盒、方块射线和距离。候选来自 `LivingCandidateQueries.targetQuery`。谓词只去掉空、死亡、旁观和已移除。权限、友方、主人驯养物和任意 `CloneEntity` 在查询之后对实时实体调用 `CloneEntity.rejectsPulseTarget`，不把允许结论放进缓存。

主动：沿冻结方向取包围盒射线的最近交点。距离相同用 UUID 字符串字典序。分身自己从起点里跳过。方块比交点更近的目标不算命中；射程内只有被挡住的交点时原因是 `BLOCKED`；射线上没有目标时是 `NO_TARGET`。最近交点被拒绝时是 `FORBIDDEN_TARGET`，不穿透到后面的目标。

反击：只在这次候选结果里核对冻结 UUID。仍要在同一射程内，并且眼睛到包围盒最近点的视线没有被碰撞箱挡住。找不到、超出射程是 `NO_TARGET`。不改选另一个敌人。

已接受的准备冻结 `ConfiguredDamage`、魔力、冷却和目标 UUID。`commit` 最多一次。目标还在时先 `DamagePermission.allows`，再 `CloneEntity.rejectsPulseTarget`，通过后才 `TechniqueHits.classifyRespecting(target, owner, configuredDamage.amount(), TideMarkSuccess.NONE)`。伤害为 0 仍然可以提交；通过权限时会进入这条裁决，`damageCalled` 为真。目标已经消失、主人已不可用，或实时权限不再允许时，不调用裁决，`damageCalled` 为假，方法正常返回。未接受或第二次 `commit` 抛 `IllegalStateException`，B 不扣费。`cancelBeforeCommit` 只清本对象冻结的目标和报价，可重复调用；已经 `commit` 之后直接返回，不撤销伤害，也不碰钱。

没有调用 `classifyRoot`、`ReleaseBatch`、`Tally`、`ChainScheduler`、火种、潮印发放、武器附魔、`ManaPaymentReceipt` 或性能计数。原版伤害源仍由现有 `classifyRespecting` 使用主人的 `playerAttack`，直接实体和致伤实体都仍是主人。B 不改 `TechniqueHits`，不转动主人，不写主人主手，不移动主人。

## 接口依赖

B 调用、但不编写的 A 类型：`CloneKind`、`CloneActionContext`、`CloneActorFrame`、`CloneEntity`、`CloneLifeState`、`CloneResults.RejectReason`。用到的实体方法是冻结签名里的 `lifeState`、`ownerId`、`rejectsPulseTarget`。`ServerLevel.getEntity(UUID)` 用来按 UUID 取实时实体。

A 需要调用：`CloneActionRegistry.admit(actionKey, CloneActionCapabilities.full())`、`get`、`prepare`、`commit`、`cancelBeforeCommit`，以及生成事务里的 `CloneBalance.readSpawn(BalanceSide.SERVER, kind)`。`prepare` 或 `readSpawn` / `readPulse` 抛出的 `BalanceValueException`、`BalanceSideException`、`BalanceDeclarationException` 应由 A 在扣款前收成 `INVALID_QUOTE`。B 不捕获它们。

C 需要调用一次 `CloneBalance.declareSpecials()`，一次 `ClonePulseAction.register()`，并把 `PULSE_ID` 放进 `BalanceCatalog` 的内置伤害名单。C 不改 `CloneBalance`。两个生成 ID 不进伤害名单。

非服务端的 `readSpawn` / `readPulse` 立刻抛 `IllegalArgumentException`，不读客户端快照，也不走没有 `BalanceSide` 的重载。

交回的缺口：冻结的 `CloneEntity` 没有代次读取方法。准备只拒绝帧上 `actorGeneration < 1`。活体代次是否与帧一致，要等 A 提供只读方法后才能在 B 里比对。合同没有单独的“已取消”原因码；取消令牌为真时准备返回 `BUSY`，说明文字是「这次响应已经取消」。合同把 `commit` 限定为权限判断和 `classifyRespecting`，所以 B 不转分身朝向；挥手仍应由 A 递增 `actionSequence` 后交给客户端。

## 配置的实际消费者

数值常量只在 `CloneBalance`。专项由 `declareSpecials` 声明，读取由 `readSpawn` / `readPulse` 消费。标准 `manaCost`、`cooldownTicks`、`damageMultiplier` 不重复声明。`PULSE_DAMAGE_MULTIPLIER` 只保留代码默认 1，不再乘到已配置伤害上。

| 稳定 ID | 键 | 代码默认 | 单位 | 范围 | 生效时机 |
|---|---|---|---|---|---|
| `clone_phantom`、`clone_body` | `lifetimeTicks` | 0 | 刻 | 0..1200000 | `readSpawn` 读出后由 A 冻进该次 `CloneSpec`。0 表示没有自然到期 |
| 同上 | `maxHealth` | 20 | 个数，语义是生命值 | 1..1024 | 同一次生成报价。Java 字段名是 `bodyHealth` |
| 同上 | `decoyRadius` | 8 | 格 | 0..16 | 同一次生成报价。0 关闭诱敌，查询本身由 A 消费 |
| 同上 | `manaCost` | 10 | 魔力 | 标准魔力边界 | `BalanceService.manaCost`，只乘一次全局魔力 |
| 同上 | `cooldownTicks` | 20 | 刻 | 标准冷却边界 | `BalanceService.cooldownTicks`，只乘一次全局冷却 |
| `clone_response_pulse` | `fixedDamage` | 2 | 个数，语义是生命伤害 | 0..1000000 | 每次 `prepare` 读出原始值，`RawDamage.of` 后只做一次 `damageContribution` |
| 同上 | `range` | 8 | 格 | 0.1..32 | 同一次准备冻结，候选盒、阻挡和距离共用 |
| 同上 | `manaCost` | 2 | 魔力 | 标准魔力边界 | 准备时报价，由 A 在提交前扣。改伤害不改这项默认 |
| 同上 | `cooldownTicks` | 20 | 刻 | 标准冷却边界 | 准备时报价，由 A 在提交线上写入该分身冷却 |
| 同上 | `damageMultiplier` | 1 | 倍率 | 标准倍率边界 | 不在专项声明。C 把该 ID 纳入内置伤害名单后，只在 `damageContribution` 内乘一次 |

世界配置下次开世界生效。缺项回代码默认。已配置伤害为 0 仍是可提交响应。

## 自检与未做

候选七个 Java 文件用与 `tools/check-name-gate.ps1` 相同的局部变量、参数和字段规则做过解析检查，7 个文件均通过。正式脚本只扫描真实 `src`，本单没有改 `src`，留到整合后统一执行。

没有跑 `gradlew`，没有 `compileJava`，没有打包，没有 `runClient`，没有启动游戏，没有改表，没有提交，没有派其他代理。

待统一编译。游戏未测。

## 哈希

SHA256 与本地最后写入时间：

| SHA256 | 本地时间 | 文件 |
|---|---|---|
| `F573098457B7AAECFD1D21F1E516F5B28182C1A85454799B6ED031F94C010450` | 2026-11-02 04:09:45.413 | `CloneAction.java` |
| `B4918BDB5905732F990FF4000C8E12408269F2FF8F78E290CDAEAF3353D18EF1` | 2026-11-02 04:09:46.132 | `CloneActionResult.java` |
| `5B28EBDE0FEA37FFE2D1CC112B0D79BAB3EDA41701B5B57B1564CBB621FF34BF` | 2026-11-02 04:09:47.679 | `ClonePreparedAction.java` |
| `7013C9099C54320F6AC0201AD153195AAC408EA9507C404FCAA2A36ED8EC6152` | 2026-11-02 04:09:49.887 | `CloneActionCapabilities.java` |
| `1759FEBD1CAE45516DBD72845EB2EE634F744A7602989AC257AE9599FC4C9084` | 2026-11-02 04:09:53.092 | `CloneActionRegistry.java` |
| `C1033004DED082C9ECCB64D14D3C10D7223D63EE6B3ED74C1747E994AF11E62C` | 2026-11-02 04:10:01.607 | `CloneBalance.java` |
| `2319CABFE499D927E7D734DA5C055912B75E867CB04F24F111F4D881EB7A38C4` | 2026-11-02 04:12:51.771 | `ClonePulseAction.java` |

## 停止写入

作者 B 停止写入，并释放本单列出的候选源码、本报告和证据文件。后续再改这些路径需要重新指定作者。
