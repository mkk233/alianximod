# C 第二段接口（v2）

本文件只描述 C 已经落地的接缝和仍要 A、D 接线的位置。设计 2/2，实现正式复审 0/3。这里的自查不是新一轮正式复审。

## 已可调用

`BatchCarrierBootstrap.install()`  
在 `ModTechniques.registerAll()` 之后调用一次。它向 `TechniqueEffectRegistry` 登记火苗、热波、陨岩、地表定位，以及喷涌、错时、命中接续、留焰、种火、引燃、引爆、崩地的通过效果。不登记幽冥。重复调用不会注册第二份。同时挂上 `CarrierHooks` 的 `onResolved`。

`BatchCarrierTypes.register(IEventBus)`  
登记 `batch_carrier_visual`。不在 `XueXiMod` 里自挂。

`MeteoriteWreckageTypes.register(IEventBus)`  
第一段的无伤残骸，仍然要挂。

`BatchCarrierHooks.intercept(TechniqueHitEntity body, ServerLevel level, Vec3 point, LivingEntity struck, MeteorContactKind kind)`  
返回 `MeteorContactResult`。`handled()` 为真时，这一次陨岩接触已经由载体打过中心。`continueChildren()` 只在 `TERRAIN` 为真。`TIMEOUT` 和 `ENTITY` 都是假。`verdict()` 供父结算之后的子段使用。

火苗 `seedPoints` 固定写 0。本体产种基数 2 只来自释放登记的 `bodyScaledBase`，再由 A 的 `ResourceSettlement` 提交一次。C 的 `onResolved` 不再调用 `ResourceHitCommit`。

## A 需要改的唯一命中点

文件：`TechniqueHitSettle.sweepEarliest`。C 不改这个文件。

实体分支里，局部变量已经叫 `contact`。载体结果用另一个名字。插在 `TechniqueHits.classifyRespecting` 之前：

```java
Vec3 sweepPoint = contact.point() == null ? hitEntity.position() : contact.point();
MeteorContactResult carrierContact = BatchCarrierHooks.intercept(
        hitEntity, level, sweepPoint, target, MeteorContactKind.ENTITY);
if (carrierContact.handled()) {
    if (hitEntity.burstOnImpact) {
        hitEntity.setPos(sweepPoint.x, sweepPoint.y, sweepPoint.z);
        onExpire(hitEntity, level);
    }
    return true;
}
```

`handled()` 为真时不要再 `classifyRespecting`，不要 `ResourceSettlement.afterVerdict`，不要 `batchFollow.onVerdict`。空中命中不接续子段。现在这条还没接上，所以空中命中仍会由现有扫掠接续子段。这不是完成状态。

地形分支，在 `onExpire` 之前：

```java
Vec3 sweepPoint = hitPoint == null ? hitEntity.position() : hitPoint;
MeteorContactResult carrierContact = BatchCarrierHooks.intercept(
        hitEntity, level, sweepPoint, null, MeteorContactKind.TERRAIN);
if (carrierContact.handled() && carrierContact.continueChildren() && hitEntity.batchFollow != null
        && carrierContact.verdict() != null && carrierContact.verdict().continues()) {
    hitEntity.batchFollow.onVerdict(carrierContact.verdict(), sweepPoint, hitEntity.forward, null);
}
```

然后仍调用原来的 `onExpire`。载体用落地标记避免第二次安放。

不要把 `intercept` 和原来的扫掠叠在同一次接触上。叠用时中心标记能挡住第二次扩圈，但挡不住扫掠自己对第一个目标的那一下。

寿命结束可以不改。载体把“没有命中回调、并且前后位置夹不到方块”当成超时：只生成无伤残骸，不打中心，不接续，不叫做撞地。

## 火球留焰

`fire_orb` 在 `TrailContract.MOVING_HOSTS` 里，但 `FireOrbEffect` 不是 C 的文件。要接留焰时，在它生成命中体的 `publishedHook` 里、`BirthLayout` 还没清掉之前，自己组一个 `BatchFlightRecord`（术式标识 `fire_orb`、本体、命中钟、载荷、主人、伤害）再调用 `BatchFlameTrail.attach(record)`。C 没有改火球效果，所以火球留焰还没接上。

## 普通武器

`WeaponEnchant` 会收养根上下文，但不会调用 `BatchFlightBook.publish`，也不会生成留焰跟踪或无伤残骸。普通武器上的留焰、崩地和陨岩落地不在本段完成范围内，不能写成已经完成。

## 着色

条带顶点颜色只读 `BatchCarrierVisualRenderer.flatTintArgb`。写入用 `BatchCarrierVisual.applyFlatTint(红, 绿, 蓝)`，默认白色。陨岩飞行体用方块模型 `meteorite_v1`，`renderSingleBlock` 没有顶点色，不走这个方法。D 的类型确定之后由 A 统一接线，或先调用 `applyFlatTint`。C 不猜测 D 的类。

## 喷涌

`TechniqueHitEntity.applySpec` 只在释放定义自己的轨迹是 `UPWELL` 时安装喷涌运动。规则把后面的可变弹道改成喷涌，仍是 A 的帧和运动。C 的通过效果不改弹道。
