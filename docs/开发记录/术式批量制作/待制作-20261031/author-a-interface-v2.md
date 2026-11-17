# 作者 A 接口 v2（资源接线）

本文件只给 C 和主会话调用。没有改 B 的源码。B 已公开的方法够用，本轮不另提构造请求。

## 13 条登记

`ModTechniques` 与 `ReleaseDefinitions` 已登记下面 13 个 id。没有向 `TechniqueEffectRegistry` 注册效果。攻击宿主在效果登记之前会被现有提交拒绝，份额留在预算里，不能当成已经释放成功。

| id | 名称 | 魔力 | 冷却 | 复杂 | 装配 | 角色 |
| --- | --- | --- | --- | --- | --- | --- |
| fire_flame_trail | 留焰 | 2 | 5 | 2 | 2 | 无 |
| fire_meteorite | 陨岩 | 48 | 40 | 6 | 1 | ATTACK |
| fire_shock_ring | 热波 | 8 | 16 | 2 | 1 | ATTACK |
| rule_surface_anchor | 地表定位 | 0 | 0 | 1 | 1 | SEEK_SURFACE |
| fire_upwell | 喷涌 | 0 | 0 | 2 | 1 | TRAJECTORY_UPWELL |
| rule_stagger | 错时 | 0 | 0 | 2 | 1 | RULE_STAGGER |
| rule_hit_continue | 命中接续 | 0 | 0 | 3 | 1 | RULE_HIT_CONTINUE |
| fire_seed_sow | 种火 | 2 | 5 | 1 | 2 | 无 |
| fire_kindle | 引燃 | 2 | 5 | 2 | 2 | 无 |
| fire_detonate | 引爆 | 2 | 5 | 2 | 2 | 无 |
| fire_nether_gloom | 幽冥 | 2 | 5 | 3 | 2 | 无 |
| earth_break_ground | 崩地 | 2 | 5 | 3 | 2 | 无 |
| fire_spark | 火苗 | 2 | 5 | 0 | 1 | ATTACK |

六条附魔的链角色是 `ChainRole.NOT_APPLICABLE`，不是 `ATTACK`。印记树里四条新规则分别进索敌、弹道、规则；附魔仍按元素分组。等级上限 20。没有单独的战斗成长字段，成长保持 0。

陨岩：物理 24，速度 0.8，俯角 +15°，重力 0.08，100 tick，碰撞 1，中心半径 1.5，总高 3。热波：物理 4，外径 5，高 3，视觉 12 tick，不移动。火苗：火焰伤害 1，速度 0.8，30 tick，碰撞 0.25，本体种火基数 2。种火基数 10 在附魔载荷里，不写进火苗的固定点数。

## C 要接的最小签名

```java
FireCastContext RootCastBindings.open(UUID rootCastId, UUID casterId);
FireCastContext RootCastBindings.adopt(FireCastContext cast);
FireCastContext RootCastBindings.lookup(UUID rootCastId);
void RootCastBindings.retain(UUID rootCastId);
void RootCastBindings.release(UUID rootCastId);

ResolvedHit(HostHandoff handoff, LivingEntity target, LivingEntity owner, HitVerdict verdict,
        ResourceEnchantLoad resourceLoad, FireCastContext cast, int bodyScaledBase,
        int legacyFixedPoints, float hostBase);
```

`CarrierHooks.VerdictListener` 新增默认方法 `onResolved(ResolvedHit)`。`notifyResolved` 先走原来的 `onVerdict`，再走 `onResolved`。旧监听不用改。

`ResolvedHit` 给出真实目标、主人、裁决、B 载荷、根上下文、本体基数、旧固定点数和宿主基数。不要按距离猜目标。

## 根上下文

一次逻辑释放只有一个 `FireCastContext`，id 就是 `BatchCastSession.rootCastId()`。延迟、子分支、拖尾和未完账本都查这一个。会话打开时 `open`，会话结束时 `release` 一次。命中体生成时再 `retain`，移除时 `release`。错时子分支在等待期间额外持有一次。费用结算不提前清去重。持有数到 0 才移出表。取消且没有未完命中时，会话释放自己的那一次。

拖尾、幽冥、崩地若比命中体活得更久，C 必须在命中体释放前 `retain`，并在自己的最后一段结束时 `release`。普通武器若带这三项，`WeaponEnchant` 会 `adopt` 一个上下文；C 结束时同样 `release`。没有这三项的武器命中不进这张表。

## 基数

新链命中只调用一次：

```java
ResourceHitCommit.commit(target, cast, caster, load, bodyScaledBase, legacyFixedPoints, damageAccepted)
```

本体基数与附魔缩放基数先收齐。同一份 2 或 10 不再调用 `FireSeedGrant.deposit` 或 `FireSeedProduction.produce`。种火 10 在 `ResourceEnchantLoad.scaledSeedBase()`，由 B 的 `SeedMerge` 并进这次提交。旧点火 2、热光束 1、火球 3 留在 `legacyFixedPoints`，不乘种火倍率。火苗本体 2 来自 `ReleaseDefinition.bodyScaledSeedBase()`。C 写火苗效果时不要再把 2 写进 `seedPoints`，否则会再进旧固定点数。

没有新能力的旧点火、热光束、火球仍只走原来的 `deposit`，基数不改成种火 10。

`DAMAGE_ACCEPTED` 才把 `damageAccepted` 传 true。`INVALID_OR_CANCELLED`、`ESTABLISHED_NO_DAMAGE`、`PENDING` 都不是资源成功。`PENDING` 不接子分支。子分支等父分支的真实完成回调，不再加固定 6 tick。错时仍是每份 +10 tick。

## 普通武器

纯新载荷不再因为旧伤害、旧种火点数为 0 被丢掉。`ResourceIds` 只存术式 id，不存熔炉热量。下一次合法普通攻击被接受后提交一次。提交不是 `APPLIED` 时印记保留，不能当成成功。不第二次收魔力。引燃热量由 B 在提交当时读取。

## 仍由 C 实现

陨岩、热波、火苗的命中效果，留焰几何，幽冥周期，崩地方块，以及对应实体或方块注册。A 不猜类名，不改 `ModEntities`、`ModBlocks`。

## 给 B 的请求

无。使用的是 `FireCastContext(UUID, UUID)`、`ResourceEnchantCatalog.explicitManaPrice`、`ResourceEnchantCatalog.collect`、`ResourceHitCommit.commit`、`FireCastContext.claimEffect`。
