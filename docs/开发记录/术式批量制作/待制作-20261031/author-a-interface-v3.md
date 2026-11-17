# 作者 A 接口 v3（幽冥领域桥）

v2 的签名仍然可用，没有改参数。本文件只增加领域桥。

## v2 仍有效

```text
FireCastContext RootCastBindings.open(UUID rootCastId, UUID casterId)
FireCastContext RootCastBindings.adopt(FireCastContext cast)
FireCastContext RootCastBindings.lookup(UUID rootCastId)
void RootCastBindings.retain(UUID rootCastId)
void RootCastBindings.release(UUID rootCastId)
ResolvedHit(...)
CarrierHooks.VerdictListener.onResolved(ResolvedHit)
ResourceHitCommit.commit(target, cast, caster, load, bodyScaledBase, legacyFixedPoints, damageAccepted)
```

一次逻辑释放仍只有一个根标识。命中体和延迟子分支的持有、释放规则不变。火苗本体 2 不要再写进 `seedPoints`。

## 本段新增

```text
void NetherDomainSync.register(SimpleChannel channel, int packetId)
UUID BatchCastSession.currentRootId(ServerPlayer owner)
boolean DomainService.tryWaveLoaded(..., ResourceEnchantLoad load, UUID rootCastId)
boolean DomainService.tryWaveReservedLoaded(..., ResourceEnchantLoad load, UUID rootCastId)
```

`ModNetwork.register` 在 13 号燎里环之后用 14 号登记 `NetherDomainSync`。0 到 13 都已占用，14 空闲。仍是原来的 `CHANNEL`，协议字符串没改。

`currentRootId` 只返回这名玩家唯一未结束的批量释放根。没有会话，或同时有多本不同的根，返回 null。不会调用 `ReleaseBatch.fireContext()`。

`SkillUsePacket`、`CastExecutionSession`、`SkillChainPlan` 在载荷 `gloom()` 时走 `tryWaveLoaded` / `tryWaveReservedLoaded`，传入 `EnchantBonus.resourceLoad` 和上面的根。没有幽冥时仍走原来的 `tryWave` / `tryWaveReserved`。延迟开域会先 `retain` 这次根，等 D 的 `attach` 返回后再 `release` 这一次临时持有。领域自己的那一次由 D 在结束时释放。

普通幽冥加领域或灼地、且没有接续、地表、喷涌、抛物线或其他宿主时，批量编译返回 null，领域仍按原费用开。出现不支持的接续时，编译期拒绝，不免费执行。

## 武器

纯幽冥不再 `adopt`。留焰或崩地仍 `adopt`，成功后不释放，避免拆掉它们的租约。提交不是 `APPLIED` 时，只释放这次新 `adopt` 的账。

## 请 D 补的签名

灼地不经过 `DomainService`。现有路径是 `ScorchEffect` 到 `SurfaceFireHosts.open`，再进 `ContactEnchantState.open`，最后只调用 `NetherDomainCycle.consider`。这三处不在本单写权限里，A 拿不到宿主对象，不能代调 `attach`。

请在 D 的 `ContactEnchantState` 增加：

```java
void open(ContactCarrier carrier, EnchantmentSnapshot snapshot, ResourceEnchantLoad load, UUID rootCastId)
```

先按现在的方式 `consider`，再在 `load.gloom()` 时调用已有的 `attach(carrier, load, rootCastId)`。`rootCastId` 为空或绑定表里没有时，不要 `open` 新的 `FireCastContext`。`SurfaceFireHosts.open` 需要把同一次载荷和根传进去。A 接到这个签名后才能接灼地的根，本段没有越界改这些文件。
