# 水幻形 A 组实现报告

日期：2026-11-02。作者只写 `shu/technique/release/clone/core/` 与本报告。没有改 B/C/D 的文件，没有登记 `water_phantom`，没有读美术中间稿，没有改制作表，没有提交，没有打包，没有启动游戏。

**未编译。未进游戏。** 本任务没有 Gradle 授权。统一编译写成「待统一编译」。本报告写完后停止改源码。

命名门禁已在项目根执行 `powershell -NoProfile -File tools/check-name-gate.ps1`，退出码 0。门禁只查名字。C 的三个性能计数还没进 `PerfCounter`，所以这次通过不等于能编译。同期 B 的 `clone/capture` 已经出现在树上；本作者没有读它的实现来判断对错，只按冻结合同调用。

## 实际改动

新建（都在 `src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/core/`）：

| 文件 | 作用 |
|---|---|
| `CloneSpawnAuthority.java` | `DEVELOPMENT_COMMAND` / `PLAYER_CAPTURED_CAST` |
| `PresentationKey.java` | `LEGACY_HUMANOID` / `WATER_OUTLINE`，未知序号回旧人形 |
| `TerminationPolicy.java` | `BUDGET` / `BREAK_ON_TRIGGER` |
| `NoticePhase.java` | `MAINTAIN` / `SCATTER` / `VOLUME` |
| `VolumeVisualKind.java` | `NONE` / `TIDE_SLASH_FULL` |
| `CaptureEndReason.java` | 合同列出的 11 个结束原因 |
| `CaptureTriggerKind.java` | `CONTACT` / `ACCEPTED_HURT` / `IMMEDIATE` |
| `CapturedAttackSnapshot.java` | 不可变快照，不含技能、帧、回调、活目标 |
| `DetachedActorFrame.java` | 不含实体和运行态 |
| `PreparedCapture.java` | 只接受 `PLAYER_CAPTURED_CAST`；立即模式诱敌半径必须是 0 |
| `CapturedResponseTicket.java` | 票只含 `deploymentId`，不含租约对象 |
| `DeploymentLease.java` | 包内构造。只允许 `BODY → TICKET → VOLUME → RELEASED`。`release()` 包内、幂等 |
| `CapturedResponseRuntime.java` | 票队列、取消、执行、容量脱离计数 |
| `CapturedBoard.java` / `CapturedDeployment.java` | 每台服务器一份表，挂在 `CloneRuntime` |
| `CapturedCasts.java` | `spawnCaptured` / `breakForResponse` / `finishDeployment` 的实现 |
| `CloneContacts.java` | 接触资格。扫描和 `push` 走同一套 |
| `HurtAttempt.java` | 包内受击事务 |
| `CaptureVectors.java` | 有限坐标、单位视线、退化时回退冻结视线 |
| `ClonePresentation.java` | `install` / `publish` / `installed` |
| `ClonePresentationNotice.java` / `ClonePresentationSink.java` | 通知不含贴图路径 |
| `NoopClonePresentation.java` | 未安装时丢弃通知，不把逻辑报成完成 |

改过的既有文件：

- `CloneSpec.java`：`TriggerMode.CONTACT_OR_HURT`（40 行）。记录在原 10 项后追加终止策略、表现键、修订号、可空 `deploymentId`。`deploymentId == null` 只允许旧开发规格（预算、旧人形、`MANUAL_AND_HURT`）。
- `CloneRuntimeState.java`：包内字段 `deploymentLease`、`capturedSnapshot`、`cosmeticOnly`、`frozenLook`、`hurtAttempt`、`leaseTransferred`。没有公开给 B/C 的写入器。
- `CloneService.java`：`spawnCaptured` 155 行，`breakForResponse` 159 行，`finishDeployment` 164 行。旧 `spawn` 仍走 `developmentOnly`。`specFor` 678 行补 `BUDGET`、`LEGACY_HUMANOID`、修订号 0、`deploymentId = null`。`onAcceptedHurt` 对 `BREAK_ON_TRIGGER` 直接返回，水幻形致死不进旧 `request`。
- `CloneRuntime.java`：`drain` 144 行，`endActor` 357 行，预约计入脱离中的租约，旧队列和捕获票共用 32 次提交。
- `CloneEntity.java`：只读 `presentationKey` 109 行、`presentationRevision`、`syncedSessionToken`。同步数据只在变化时写。旧实体默认旧人形、修订号 0。
- `CloneTriggerPolicy.java` 18 行：`CONTACT_OR_HURT` 拒绝旧的 `request` 路径。

`CloneLifecycleEvents.java` 没有改。登出、死亡、克隆、换维、离维、世界卸载、停服仍进原来的 `endOwned` / `endDimension` / `shutdown`。这三处现在会取消票和体积。`OWNER_DEATH` 不延后。玩家克隆只清旧主人的部署，不复制水幻形。

没有改父包的 `CloneTechniqueRegistry`、`CloneReleaseDefinition`、`CloneBalance`、`CloneEntityTypes`。没有加命令。没有把候选贴图写进 `src`。

## 合同行为

旧 `CloneService.spawn(owner, kind)` 仍收开发术式，仍读 `CloneBalance.readSpawn`，仍上开发生成冷却。水幻形走 `spawnCaptured`：不读平衡、不上那条冷却、不调用 `developmentOnly`。

成功时 `accepted == true`，`chargedMana == prepared.totalMana()`。付款只有 `ManaPaymentReceipt.charge(owner, totalMana, true)` 一次。`null` 是魔力不足。发布前失败退一次并放开预约。发布成功不退，之后撤回、到期、离线、换维、停服都不退。立即模式若票没有登记成功，视为还没发布：退一次并拆掉身体。

身体用现有 `clone_actor` 尺寸。规格是幻影、一次预算、`CONTACT_OR_HURT`、`BREAK_ON_TRIGGER`、`WATER_OUTLINE`、表现修订号 0、术式标识字符串 `water_phantom`。这只是规格标签，没有注册术式。`residenceTicks == 0` 这一加载会话内不到期。正数从生成时的分身单调 tick 起算。立即模式不靠寿命推迟出招。诱敌半径写入规格；0 沿用原策略直接跳过。立即模式半径必须是 0，外观不诱敌、不碰撞、不受击、不参加接触。

`breakForResponse` 不收费。成功才把租约从 `BODY` 转到 `TICKET` 并登记唯一票。`executeTick = 当前分身 tick + 1`，`expireTick = executeTick + 1`。失败不改身体。驻留的接触或受击在转票后发散水并摘掉身体。立即模式先留仅外观的身体，下一 tick 出体积再散。创建时站在敌人身上不当成接触。

容量从 `reserve` 起占本人 4 和全服 128。身体在 `actors` 里时算这一份。转出并移出 `actors` 之后，同一份改记在脱离计数里，直到 `release()`。不提前还，不双计。旧开发分身仍在 `endActor` 时按原索引释放。

`drain` 空转条件是没有身体、没有旧队列、没有主人记录，并且捕获表和 `ActorVolumeRuntime.idle()` 都空。非空时先到期和主人检查，再接触，再旧 `pumpQueue`，剩下的提交额度给 `CapturedResponseRuntime.tick`，然后 `ActorVolumeRuntime.tick`，最后才诱敌。旧队列不能先用满 32 再让新票另用 32。票执行成功并 `submit == true` 时，`CLONE_RESPONSE_COMMITS` 加一。破散当 tick 不加。`submit == false` 走 `finishDeployment(..., FAILED_SUBMIT)`，不加提交计数。

票执行检查会话、主人存活且同维、眼位区块已加载、适配器 `find(id).version()`、几何有限。失败则结束部署。立即模式给主人一条「水幻形这一下没有打出去」。驻留反击不发聊天。通过后 `instantiate`，租约转入 `VOLUME`，再 `submit`。不调用旧效果、`TechniqueHitSpec`，不移动主人，不用假人。来源作用域由体积侧打开。

`finishDeployment` 核对服务器线程、会话、表里的同一租约对象。不符或已经释放则返回，不退款，不重新生成身体。相符则 `release()` 一次。体积侧看到 `released()` 后停；停服另调用 `ActorVolumeRuntime.shutdown(SERVER_STOP)`。

表现：`install` 只能成功一次，第二次抛 `IllegalStateException`。未安装时 `publish` 交给无操作接收器。无操作不注册术式、不选贴图。每个 `deploymentId + actionToken + phase` 只发一次。散水和维持尽量在 `discard` 前发出。同维主人，以及同维、距通知坐标不超过 160 格的玩家，有人在才发。通知记录里没有玩家列表，接收者由 D 的 sink 按维度和坐标再取。`VOLUME` 的 `worldPosition` 是眼原点沿冻结视线前移 `depth/2` 的体积中心。维持和散水用脚底。时长固定 20 tick，只是包内 0..200 的协议窗口，不是美术帧率。

## 致死六步

门只对 `TerminationPolicy.BREAK_ON_TRIGGER`、租约仍是 `BODY`、生命为活动、响应仍是就绪、不是立即外观的身体打开。普通分身和开发幻影走 `hurtOrdinary`（`CloneEntity` 337 行），和原来一样：接受且还活着才 `onAcceptedHurt`，死了就 `endLoaded(DEATH)`。

1. `CloneEntity.hurt` 286 行。来源先过 `rejectsIncomingAttacker`。水幻形在 `super.hurt` 前写下包内 `HurtAttempt`：句柄、租约、眼睛、朝向、来源 UUID、事务序号。不消费额度，不发包，不排队。同一段同步 `hurt` 重入返回 `false`。环境伤害的来源实体为空，`rejectsIncomingAttacker` 不拒绝，所以门会打开；第 4 步没有合格生物来源时不建票。
2. `super.hurt` 照常。`CloneRuntime.endActor` 357 行在 `reason == DEATH` 且延后门开着时只记 `pendingDeath` 并返回：不设 `prepareCancelled`，不取消旧请求，不删 `actors`，不删主人索引。`hurt` 里的死亡和 `CloneLifecycleEvents.onDeath` 都进这一个 `endActor`。`OWNER_DEATH` 不进这道门。
3. `remove` 260 行。`KILLED` 且门开着只记 `pendingKilledRemoval`，不调用 `super.remove`。`die` 仍走父类。`UNLOADED_TO_CHUNK`、`DISCARDED` 和其他原因不延期，标记硬取消并立刻 `super.remove`。`tickDeath` 273 行：水幻形不留 20 tick 尸体；门还开着时先返回，门关了就 `discard`。普通分身仍走原版等待。
4. 保存 `super.hurt` 的布尔值，再关延后门。`finishHurt` 350 行核对租约、会话、主人在线且同维。只有返回 `true`、来源仍过 `rejectsPulseTarget`、没有硬取消，才 `breakForResponse(..., ACCEPTED_HURT, ...)`。允许此时生命不大于 0。不调用 `onAcceptedHurt` 或 `request`。方向用事务里保存的眼睛；攻击者中心重合时用冻结视线。
5. 合格触发把租约转到 `TICKET`，标记预算已用，驻留路径再按身体结束摘掉实体。此后 `endActor(DEATH)` 看到租约已不是 `BODY`，只清身体，不取消票。不合格、返回 `false`、硬取消：没有票，按记下的死亡或移除把清理做完。死亡被取消且这次 `hurt` 返回 `false`、人还活着：留在就绪，不凭 `pendingDeath` 出手。`hurt` 返回 `true` 即使死亡被取消，仍是一次有效受击。
6. `finally` 331 行清掉 `HurtAttempt` 和重入标记。异常不重试 `hurt`，返回 `false`，不宣称这次事务已接受。重复的结束或移除在已转出时只清身体。`discard` 引起的 `onLeave` → `UNLOAD` 在转出后不取消票。主人、维度、世界和停服仍通过部署表取消票和体积。

接触不进 `hurt`。`drain` 里、诱敌之前，对 `CONTACT_OR_HURT` 且不是立即外观的身体做 `LivingCandidateQueries.entityQuery`，再精确相交。最多看 32 个候选，第一个通过 `rejectsPulseTarget`、且不是无敌或无敌帧中的目标，调用 `breakForResponse(CONTACT)`。主人、分身、宠物、同队、旁观、死亡、移除、物品和地面不触发。`CloneEntity.push` 248 行走同一资格。

## 配置消费

A 不声明平衡键，不调用 `BalanceService`，不给水幻形加伤害倍率，不改默认耗魔。

C 已经算进 `PreparedCapture` 的值，A 这样用：

| 字段 | 生效时机 |
|---|---|
| `totalMana` | 生成时一次 `ManaPaymentReceipt.charge`。不再乘全局魔力。失败且未发布则原收据退一次 |
| `residenceTicks` | 生成时冻进 `CloneSpec.LifetimePolicy`。0 不到期。立即模式不靠它延迟出招 |
| `decoyRadiusBlocks` | 生成时冻进规格。0 关闭诱敌。立即模式必须是 0 |
| `bodyHealth` | 必须是 `1.0`，否则 `INVALID_QUOTE`。A 不读生命配置 |
| 快照里的伤害、几何、载荷魔力、载荷冷却、适配器版本 | 原样放进票，执行时交给 B。A 不二次缩放 |

## 依赖和阻塞

- B 的 `clone/capture` 已在树上。A 调用的 `find`、`instantiate`、`submit(spec, lease)`、`tick`、`shutdown`、`idle` 与合同签名一致。本作者没有复核 B 的体积逻辑。停服时 A 调用 `ActorVolumeRuntime.shutdown`；单份体积的停止信号是租约 `release()`。合同没有按租约停止的第二个方法。
- C 还没把 `PerfCounter.CAPTURE_DEPLOYS`、`CAPTURE_BREAKS`、`CAPTURE_RESPONSE_CANCELS` 加进枚举。A 已经引用这三项。统一编译会因此失败，直到 C 按合同加在枚举末尾。`CLONE_RESPONSE_COMMITS` 是原有常量。
- D 尚未 `ClonePresentation.install`。通知被无操作接收器丢掉，身体和票仍会结束。`WATER_OUTLINE` 的渲染早退不在本任务。现有渲染器仍会把一切 `CloneEntity` 画成僵尸，所以不能把正式术式放进世界。
- 第 9 节的装配门没改。玩家仍然装不上「水幻形 → 断潮」。这是合同里的真实阻塞，留给主会话指定走哪一扇门。
- 主动解散没有单独的 `CaptureEndReason`。`dismiss` / `dismissAll` 直接释放租约。已经形成票或体积时计 `CAPTURE_RESPONSE_CANCELS`。仍停在 `BODY` 的自然到期或环境死亡只释放身体和名额，不加这条取消计数。
- 表现通知没有接收者字段。A 只保证同一次部署、动作记号和阶段发一次，并且附近有接收者才发。

## 未跑

- 未执行 `gradlew compileJava`。待统一编译。
- 未打包，未 `runClient`，未进游戏。
- 没有新增测试。
- 没有做正式实现复核。本阶段实现复核仍是 0/3，作者本次不自行开审查。

本作者停止写入。
