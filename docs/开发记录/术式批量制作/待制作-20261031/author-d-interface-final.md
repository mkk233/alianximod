# D 最终接口：灼地幽冥载荷与真实范围

`NetherDomainTint.sample` 的参数没有变。C 已接的调用不用改签名。

## 色调

```java
NetherDomainTint.Sample NetherDomainTint.sample(Level level, double worldX, double worldY, double worldZ);
boolean NetherDomainTint.gloom(Level level, double worldX, double worldY, double worldZ);
```

`Sample` 仍是 `gloom`、`red`、`green`、`blue`、`tintKey`。

- 点不在任何真实切片里：`255,255,255`，`tintKey` 为空。
- 点在切片里：`90,160,255`，`tintKey` 为 `"blue"`。

雾雨、重压仍是一个轴对齐盒子，这个盒子就是领域本身。灼地改按当前贴地切片判断，切片之间的空隙不染蓝。外包盒还在同步前缀里，只作摆放参考，不参与 `sample`。

判断用低边界包含、高边界排除：`>= low` 且 `< high`。服务端读存活租约的切片，客户端读同一份已收齐的分片。不按本地玩家距离猜测。

## 灼地载荷

```java
void ContactEnchantState.open(ContactCarrier carrier, EnchantmentSnapshot snapshot, ResourceEnchantLoad load, UUID rootCastId);
boolean SurfaceFireHosts.open(ServerLevel level, ServerPlayer player, ReleaseBatch batch, BoundReleasePlan plan, EnchantmentSnapshot snapshot, ResourceEnchantLoad load, UUID rootCastId);
```

旧的两参 `ContactEnchantState.open` 和五参 `SurfaceFireHosts.open` 还在。四参版本先走旧两参，因此 `consider` 只发生一次；`load.gloom()` 为真时再 `attach`。根标识为空，或 `RootCastBindings.lookup` 找不到时，不 `open` 新的 `FireCastContext`。

接触伤害大于 0 的扫描、灼地伤害、几何和 `touches` 没有改。

## 范围出口

```java
int GloomVolumes.exportVolumes(double[] destination);
void NetherDomainCycle.sync(ContactCarrier carrier);
void NetherDomainCycle.drop(UUID domainId);
```

`exportVolumes` 只拷贝宿主已经算好的切片。目标数组为空或放不下时不写，返回片数。
