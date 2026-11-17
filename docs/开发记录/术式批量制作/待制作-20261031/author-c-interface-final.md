# C 接口终稿

本文件收束载体接口。设计 2/2，实现正式复审 0/3。签名相对上一份没有改。取消接触这一次不是新的正式复审。

## 色调

绘制时调用：

```java
NetherDomainTint.Sample NetherDomainTint.sample(Level level, double worldX, double worldY, double worldZ);
```

入口在 `BatchCarrierVisualRenderer.flameChannels`。`level` 用视觉实体自己的关卡。`worldX/Y/Z` 是该帧插值后的实体位置；热波环的 Y 再加 0.05。返回的红绿蓝直接乘进顶点，透明度仍是 255，帧的 V 范围不变。领域内是 `90,160,255`，外面是 `255,255,255`。土尘、地表锚点、陨岩方块模型和无伤残骸表面不取这层火色。

## 陨岩接触

签名不变：

```java
MeteorContactResult BatchCarrierHooks.intercept(
        TechniqueHitEntity body, ServerLevel level, Vec3 point,
        LivingEntity struck, MeteorContactKind kind);
```

`handled()`、`continueChildren()`、`verdict()` 也不变。没有新增返回状态。

实体候选一律先返回 `notHandled()`。A 对这一下做唯一一次 `classifyRespecting`。取消或无效时，C 不扩中心圈、不提交资源、不生成残骸、不写 `expanded`、`followSent`、`contacted`、`landed`，也不占用命中钟。A 把该目标放进 `rejectedTargets` 后继续扫下一个。同一个取消目标不会再次进入中心圈。

`PENDING` 在 `onResolved` 里直接返回，不当成接受，也不扩圈。合法免疫和最终伤害不是正数走 `ESTABLISHED_NO_DAMAGE`：A 先完成这一下的结算，然后才接续。正伤走 `DAMAGE_ACCEPTED`。这两种才会扩圈。圈里跳过已经结算的目标和已经取消的目标，避免和 A 的直接结算打成两次。父结算回调回来之后，A 再调用一次 `batchFollow`。

地形和超时仍返回 `handled`。超时只留无伤残骸。地形先崩再放，并按 `continueChildren` 接续。

A 已经接上的登记、渲染、语言、火球留焰和这条扫掠缝保持原样。本接口不要求再改 A 的命中类。普通武器、剑实体和光束仍然不是留焰的移动宿主。

## 留焰命中钟

`BatchFlameTrail.sweep` 仍用宿主出生槽上的那一只 `HitClock`，不另造钟。先 `occupied(目标, 当前刻, 0)`，已占用就跳过，不分类。未占用才 `classifyRespecting` 一次。裁决 `continues()` 之后才 `tryMark` 一次。取消、无效和 `PENDING` 不写钟。没有新的公共方法。
