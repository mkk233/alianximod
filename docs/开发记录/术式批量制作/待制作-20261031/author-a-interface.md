# 作者 A 交接接口

本轮没有登记 13 个新术式，也没有它们的效果实现。下面的接口在定义声明了新能力之后才会进入新报价。法杖 32 槽本身不切换报价。没有监听器、没有 `TechniqueEffectRegistry` 条目时，不会假装火种、地形或攻击已经成功。

## 调用入口

- `BatchReleaseCompiler.compile(Skill skill, double cooldownReduction)`：没有新能力时返回 `null`，旧链继续原编译。有新能力但结构不合法时，`BatchReleasePlan.refusalKey()` 为语言键，不收费、不进冷却。
- `BatchCastSession.open(ServerPlayer, ItemStack, int slot, Skill, BatchReleasePlan, float weaponBonus, Double attributeBonus)`：包内入口。地表失败在 `GroupBudget.reserve` 之前返回拒绝。成功返回 `CastTiming.deferred`。
- `BatchCastSession.cancelOwned(ServerPlayer)` / `cancelAll()`：停未出生的份和未触发的子段，结算退还未提交份额，不写冷却。已出生的攻击和残留不删。松键不走这里。死亡、下线、换维经 `CastExecutionSession.cancelOwned` / `cancelAll` 转进来。换手和装配变化在会话 `stillValid` 里用主手物品类型、法杖付款身份和 `StaffSkills.signature` 判断。
- 法杖释放包仍是槽 0。服务端重读主手。`StaffSkills.castRefusal` 在 `asSkill` 之前拒绝超长、未知、剑术、未学会或槽位数不合法的链，不改 NBT。

## 定义侧开关

`ReleaseDefinition` 新增默认值，全是关闭：

- `boolean declaresBatchQuote()`
- `MotionProfile motionProfile()`，默认 `MotionProfile.NONE`
- `boolean acceptsSurfaceAnchor()`
- `boolean acceptsHitContinue()`
- `boolean acceptsBatchPlan()`
- `boolean prefersEarliestContact()`

`ChainRole` 在原有序号之后追加：`SEEK_SURFACE`、`TRAJECTORY_UPWELL`、`RULE_STAGGER`、`RULE_HIT_CONTINUE`。`languageKey()` 为 `gui.xueximod.role.` 加枚举名小写。`batchLifecycle()` 为真时整组走新报价。

`Trajectory` 追加 `PARABOLA`、`UPWELL`。出现这两种弹道也会走新报价，不会在封闭核里掉回直线。

## 根标识、分支、命中、费用

1. 会话生成一个 `rootCastId`。同一组的父份和子份共用它。`HostHandoff` 带上 `rootCastId`、主人、维度、`branchOrdinal`、`copyOrdinal`、`HitClock`、事件点和方向，以及是否子实例。
2. 父份按分支基准时刻出生。错开是基准后再加 `10 * copyOrdinal`。双重释放的下一段成员间隔仍是 3 刻，两套间隔不互相代替。
3. 父份实际份数和子段自己的份数分开。子实例数是父份数乘子段自己的份数。父段的复制倍数不会再乘到子段上。
4. 费用先整组预留最坏子份。附魔在该分支第一份成功出生时扣一次，不按份数乘。宿主费按实际出生的那一份提交。未出生、未触发的份额在 `GroupBudget.settle` 退回原收据的玩家池和原法杖，不退到当前主手。
5. 取消只停未出生和未触发的接续，不删已经出生的攻击。父段出生时不关账。没有剩余出生或接续机会时才结算。
6. 命中结果只有四种：`INVALID_OR_CANCELLED`、`ESTABLISHED_NO_DAMAGE`、`DAMAGE_ACCEPTED`、`PENDING`。账本未冲刷是 `PENDING`，不是成功。`continues()` 只含后两种已成立的结果。`grantsSeeds()` 只有正伤。
7. 同一扫掠上比较最早的实体和地形。取消不消耗这一次扫掠机会。免疫或最终伤害不是正数可以接续，不产种。正伤才产种。
8. 父段结算后再排子段。直接命中的子段在成立后延迟 6 刻，和 `FlowRoundLedger.FLUSH_DELAY` 相同。遮挡名单在父事件点冻结。子段只打名单内目标，并排除建立那次的中心目标。
9. 同一 `HitClock` 给出身体和火焰拖尾去重。根火种去重是另一份 `RootSeedLedger`，不在账本冲刷或分支记账时清空。
10. 旧 `TechniqueHits.hurtRoot` 仍绕过无敌帧。新路径 `classifyRespecting` 使用尊重无敌帧的 `playerAttack`，不另加一条绕过。`respectLanding` 未打开的旧账本冲刷仍走 `hurtRoot`。

## 可调用签名

```text
HitVerdict.continues()
HitVerdict.grantsSeeds()
TechniqueHits.classifyRespecting(LivingEntity target, ServerPlayer player, float amount, TideMarkSuccess tide)
TideMarkDamageScope.finishReading(Token token, boolean hurtSucceeded)
TideMarkDamageScope.Reading.canceled() / eventSeen() / accepted() / amount()
ReleaseBatch.Tally.respectLanding()
ReleaseBatch.Tally.whenComplete(Completion completion)
ReleaseBatch.Tally.settledVerdict(UUID targetId)
ReleaseBatch.Tally.Completion.onSettled(UUID targetId, LivingEntity target, HitVerdict verdict, int copyIndex)
HitClock.tryMark(UUID targetId, long gameTime, int intervalTicks)
HitClock.alreadySeen(UUID targetId)
RootSeedLedger.claim(UUID rootCastId, UUID targetId)
RootSeedLedger.alreadyClaimed(UUID rootCastId, UUID targetId)
RootSeedLedger.release(UUID rootCastId)
CarrierHooks.addBirthListener(BirthListener listener)
CarrierHooks.addVerdictListener(VerdictListener listener)
BirthListener.onBorn(HostHandoff handoff)
VerdictListener.onVerdict(HostHandoff handoff, HitVerdict verdict)
HostHandoff.rootCastId() / ownerId() / dimensionId() / branchOrdinal() / copyOrdinal()
HostHandoff.hitClock() / eventPoint() / direction() / childInstance()
SurfaceAnchor.resolve(ServerPlayer player, double halfWidth, double halfHeight)
OcclusionSnapshot.capture(ServerLevel, Vec3 eventPoint, Vec3 direction, double radius, ...)
OcclusionSnapshot.allows(UUID targetId)
ImpactPartition.none() / excluding(UUID) / excludingAll(Set<UUID>)
MotionProfile.approvedParabola() / approvedUpwell() / resolve(Trajectory)
StaffSkills.overflowHeld(ItemStack)
StaffSkills.signature(ItemStack)
StaffSkills.castRefusal(ServerPlayer, ItemStack)
StaffData.SLOT_CAP == 32
```

`BirthLayout` 只在生成命中体前由会话安装，后续作者不要自己开第二套生成。

## 给 B

- 新附魔或新规则要整组改报价时，在定义上返回 `declaresBatchQuote()`，不要让 A 按术式编号分支。
- 火种去重调用 `RootSeedLedger.claim(rootCastId, targetId)`。A 不产种，也不在费用结算时 `release`。最后一份效果结束时由 B 调用 `release(rootCastId)`。
- 出生和命中结果用 `CarrierHooks` 的两个监听器。监听器列表为空时，现有宿主提交照常进行，不会假造成功效果。
- 只有 `DAMAGE_ACCEPTED` 可以挂种。`PENDING` 和取消、免疫都不是正伤。
- 身体与火焰拖尾共用 `HostHandoff.hitClock()`。

## 给 C

- 术式登记时设置 `ChainRole`、`Trajectory`。建议代号就是已追加的 `SEEK_SURFACE`、`TRAJECTORY_UPWELL`、`RULE_STAGGER`、`RULE_HIT_CONTINUE`，弹道 `PARABOLA`、`UPWELL`。
- `MotionProfile.NONE` 遇上这两种弹道时使用批准常数：抛物线速度 0.8、俯仰 +15 度、重力 0.08、100 刻；喷涌速度 1、重力 0.08、锥半角 25 度、水平限 32、100 刻。单份喷涌朝世界上方，多份绕冻结前向等分锥角。
- 领域、屏障、护体、场地、条件和自身状态必须显式 `acceptsBatchPlan()`。要接续再开 `acceptsHitContinue()`。要落承托面再开 `acceptsSurfaceAnchor()`。要最早接触再开 `prefersEarliestContact()`。否则编译期拒绝。
- `TechniqueEffectRegistry` 没有该编号时，宿主提交会拒绝这一份，份额留在可退回的账上。A 不写空的成功实现。
- 水的放置和分开不在本轮。没有新的地形破坏开关。
- `ImprintTreeView` 对新增角色走原有 default，显示值是 -1。A 没有改那个文件。语言键 `gui.xueximod.role.*` 已写入中英语言文件，制作表上的中文由填表人写。

## 仍待后续登记

13 个术式的 `ReleaseDefinitions` 登记、效果注册、资源、领域或地形行为都不在本轮。接口留好之后，B 和 C 按上面的开关接入。在那之前这些术式不能释放。
