# 作者 A 接口终稿

v2、v3 的可调用签名仍然有效。本文件补上载体注册、陨岩命中缝、喷涌改写、火球留焰、幽冥提交，并修正武器租约。这不是正式复审。

## 仍然有效

```text
FireCastContext RootCastBindings.open(UUID rootCastId, UUID casterId)
FireCastContext RootCastBindings.adopt(FireCastContext cast)
FireCastContext RootCastBindings.lookup(UUID rootCastId)
void RootCastBindings.retain(UUID rootCastId)
void RootCastBindings.release(UUID rootCastId)
ResolvedHit(...)
CarrierHooks.VerdictListener.onResolved(ResolvedHit)
ResourceHitCommit.commit(target, cast, caster, load, bodyScaledBase, legacyFixedPoints, damageAccepted)
void NetherDomainSync.register(SimpleChannel channel, int packetId)
UUID BatchCastSession.currentRootId(ServerPlayer owner)
boolean DomainService.tryWaveLoaded(..., ResourceEnchantLoad load, UUID rootCastId)
boolean DomainService.tryWaveReservedLoaded(..., ResourceEnchantLoad load, UUID rootCastId)
boolean NetherDomainCycle.attach(ContactCarrier carrier, ResourceEnchantLoad load, UUID rootCastId)
```

一次逻辑释放仍只有一个根标识。火苗本体基数 2 不写入 `seedPoints`。普通幽冥加旧领域或灼地、且没有接续、地表、喷涌、抛物线时，批量编译返回 null，领域按原费用开。协议通道和 14 号包不变，不重复登记残骸类型、地形开关或陨岩物品。

## 载体注册

`XueXiMod.registerContent` 在残骸登记之后调用一次 `BatchCarrierTypes.register`。`commonSetup` 在 `ModTechniques.registerAll()` 之后调用一次 `BatchCarrierBootstrap.install()`。客户端 `EntityRenderersEvent` 把 `BatchCarrierTypes.VISUAL` 交给 `BatchCarrierVisualRenderer`。`EntityRendererFix` 按 `batch_carrier_visual` 重绑。语言键 `entity.xueximod.batch_carrier_visual`。该实体不进创造标签。`install()` 登记火苗、热波、陨岩、地表、喷涌、错时、接续、留焰、种火、引燃、引爆、崩地，不登记幽冥。

## 陨岩命中缝

`TechniqueHitSettle.sweepEarliest` 在分类之前调用 `BatchCarrierHooks.intercept`。已被载体接住的接触不再走 A 的 `classifyRespecting`、`ResourceSettlement`、`WetPressure`。实体接触由 C 扩圈并提交；A 只在裁决可接续、且不是取消或未决时调用一次 `batchFollow`。地形接触先拦截，再按 C 的 `continueChildren` 决定是否接续，然后 `onExpire`。寿命结束走 `expireLifetime`，拦截种类是超时，不当成落地。方块截断走 `expireTerrain`。

## 喷涌

`BatchReleaseCompiler` 把喷涌规则写进分支的 `upwellPath`。同分支再出现垂落、追踪、抛物线或涡旋时仍拒绝，不叠路径。`BirthLayout` 和 `TechniqueHitSpec.upwellThisCast` 把这个标记交给最终宿主。宿主弹道可变、且本身不是抛物线或涡旋时，`applySpec` 使用 `UpwellMotion`。单份竖直，多份 25 度等方位锥，初速 1，重力 0.08，运动不超过 100 tick，水平 32 格，仍受宿主寿命限制。

## 火球留焰

`FireOrbEffect` 只增加 `publishedHook`。出生槽和命中钟还在时，用火球键、槽上的根、载荷、本体基数和既有产种点构造飞行记录，并调用 `BatchFlameTrail.attachLoose`。没有槽或命中钟就返回。产种点仍是 3，燃烧仍是 4 秒，不新增爆炸。留焰只认火苗、火球、陨岩。

## 幽冥

`ModTechniques.registerAll` 登记 `GloomDomainEffect`。效果收集幽冥载荷后，把施法者已经打开的 `DomainInstance` 交给 `NetherDomainCycle.attach`。没有活着的领域就不生成攻击体，也不调用 `tryWave`。灼地宿主不在这里挂接。

## 武器

普通攻击的资源提交只用栈上的 `FireCastContext` 交给 `ResourceHitCommit`。不再 `adopt`，成功或失败都不留下静态根。失败仍返回 false 并保留印记。不二次扣魔力。留焰不挂在武器、剑实体或热光束上，武器也不生成陨岩。

## 仍待 D

灼地根接续仍要 `ContactEnchantState.open(ContactCarrier, EnchantmentSnapshot, ResourceEnchantLoad, UUID)`，由 `SurfaceFireHosts.open` 传入同一载荷和根，且不新建 `FireCastContext`。D 的灼地桥结果尚未交回，本终稿不读其在写源码。

## 返修后的命中钟和净空

```text
boolean HitClock.occupied(UUID targetId, long gameTime, int intervalTicks)
double ReleaseDefinition.birthHorizontalRadius()
double ReleaseDefinition.birthVerticalHalf()
BirthClearance BirthClearance.cover(ReleaseDefinition definition)
```

`occupied` 不写钟。本体只在裁决 `continues()` 时 `tryMark`。取消和 `PENDING` 不占钟。已经占用的目标从本轮扫掠候选跳过。没有命中钟的火球仍走原来的 `hitOnce`。

地表计划分开保存 `clearanceHorizontal` 和 `clearanceVertical`。`SurfaceAnchor.resolve` 用竖向半高再加 0.125。热波登记为水平 5、竖向半高 1.5。火苗 0.125 / 0.125。陨岩 0.5 / 0.5。

非法杖释放保存服务端 `ChainSteps.signature`。该槽装配变化时走现有 `cancel`。

## 仍待 C

留焰在 `BatchFlameTrail.sweep` 里先 `tryMark` 再分类。取消的火径因此会占住共享钟。请先分类，只在 `continues()` 时占钟。可以用上面的 `occupied` 做不写状态的查询。A 不改该文件。

原先陨岩取消扩圈的请求已经由 C 收束，这里不再要求改回。
