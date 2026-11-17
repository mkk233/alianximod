# D 接口：幽冥周期与色调

本单没有改 A、B 的源码，也没有读取 C 正在写的载体渲染类。下面是已经落地的签名。

## 给 C 的色调查询

```java
NetherDomainTint.Sample NetherDomainTint.sample(Level level, double worldX, double worldY, double worldZ);
boolean NetherDomainTint.gloom(Level level, double worldX, double worldY, double worldZ);
```

`Sample` 的字段是 `gloom`、`red`、`green`、`blue`、`tintKey`。

- 不在幽冥里：`gloom=false`，颜色 `255,255,255`，`tintKey=""`。
- 在幽冥里：`gloom=true`，颜色 `90,160,255`，`tintKey` 等于 `NetherContract.VISUAL_TINT`，也就是 `"blue"`。

客户端只认 `NetherDomainSync` 同步过来的领域盒子和维度、视觉结束时刻。服务端只认仍存活租约的 `geometry().bounds()`。两边都不按本地玩家远近猜测。

火苗、热波、留焰、陨岩的新渲染类各自在画顶点前调用 `sample`。本单不知道这些类的名字，也没有去接它们。

已接入的旧火色调：

- `FireVisualClient` 里世界坐标的片和十字，以及身体公告板（公告板用火焰的世界坐标，不用局部顶点）。
- `FireSeedFlameClient` 的余额火苗，按火苗世界坐标着色。

这两处只改顶点颜色，不改贴图、几何和伤害公式。

## 领域挂接

```java
boolean NetherDomainCycle.consider(ContactCarrier carrier, EnchantmentSnapshot snapshot);
boolean NetherDomainCycle.attach(ContactCarrier carrier, ResourceEnchantLoad load, UUID rootCastId);
boolean NetherDomainCycle.bindRoot(UUID domainId, UUID rootCastId);
boolean DomainService.tryWaveLoaded(..., ResourceEnchantLoad load, UUID rootCastId);
boolean DomainService.tryWaveReservedLoaded(..., ResourceEnchantLoad load, UUID rootCastId);
```

`consider` 在 `ContactEnchantState.open` 里、接触伤害判断之前执行。快照的 `basicTypeIds` 含 `fire_nether_gloom` 就登记，接触伤害为 0 也登记，但不会因此进入接触伤害扫描。

`attach` 只在 `load.gloom()` 为真时登记。`rootCastId` 必须已经在 `RootCastBindings` 里，否则只记住、不调用 `open`，也不新建 `FireCastContext`。领域关闭、维度卸载或服务器停止时，持有过的那一次会 `release`。

可挂接的宿主只有两处：`DomainInstance`，以及类名以 `SurfaceFireHosts$Host` 结尾的灼地宿主。屏障、护体和其他接触载体不会被当成幽冥领域。

## 产种

每到主世界游戏时间能被 20 整除的刻，对每个目标调用一次：

```java
NetherWindowResult NetherSeedArbiter.grant(LivingEntity target, long windowId, List<NetherOffer> offers);
```

`windowId` 是主世界 `gameTime / 20`。`offers` 是这一刻覆盖该目标、并且领域年龄已经满 20 服务器刻的全部租约。择高和同值用领域标识字典序，都在 B 的仲裁器里完成。这里不按领域各入一次账，也不每周期新建施法。

敌对过滤沿用 `ContactPolicy.mayContactAffect`。现有 `Relations.ally` 恒为 false，因此排除的是自己和旁观者，没有第二套阵营。

## 同步包

见 `author-d-registration.md`。未登记前 `NetherDomainSync.emit` 直接返回，不发送未注册报文。
