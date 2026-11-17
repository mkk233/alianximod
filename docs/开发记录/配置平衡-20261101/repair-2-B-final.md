我先读这份返修任务的完整范围，再只改它授权的部分。幻剑产种和接触是否成立已经改成看类型化裁决。有限的配置 0 可以留下命中和接触效果，不会再被当成正伤产种，也不会先扣款再因为没有正伤退回。最初的 `pre-B` 和上次 `pre-repair-1` 都还在。本次写入已经释放。

## 类型化裁决

幻剑主人命中调用 `TechniqueHits.classifyRoot`，参数与原来的 `hurtRoot(目标, 主人, copiedDamage, 潮印)` 相同。命中占位仍在裁决前写入。`HitPayloads.apply` 仍在产种判断之前。只有 `grantsSeeds()` 时才 `FireSeedGrant.deposit`。有限 0 会留下占位和载荷，不产种。读档仍直接使用已保存的 `Damage`，没有再乘。非主人的原版魔法伤害分支没有改。

水幕、水环、水牢、场地和灼地的接触扫描调用 `classifyHit`，参数与原来的 `hurt(..., RESPECT_INVULNERABILITY, 潮印)` 相同。`continues()` 才下发附魔状态。`grantsSeeds()` 才产种。权限不成立时两条都不做。

领域结算调用 `classifyConfirmed`，参数与原来的 `hurtConfirmed` 相同。

## 接触 0 的收费、状态和产种

安装、收集和资格不再把有限的 0 当成没有接触。负数、非有限值，以及没有任何基础附魔的空快照，仍然不安装。快照上没有保存 `rawDamageOf` 的结果，所以有限 0 还要求冻结快照里已经有基础附魔标识。上游 `ContactCast` 已用原始加数挡过非幽冥的空接触，这次没有改那份快照类。

领域有限 0：`continues()` 成立后才扣接触费并保留收据，同时下发状态，不产种。同一次停留里后面的采样只补状态，不再扣。目标离开这次收集后，停留记录去掉，再次进入可以再扣。权限或裁决不成立时不扣、不退、不下发、不产种。

正伤仍先扣款。只有 `grantsSeeds()` 才留下收据、下发状态并产种。没有打出正伤时退回这一笔，和原来的确认失败相同。

## 改动

- `shu/technique/sword/SwordEntityMotion.java`
- `shu/technique/release/contact/ContactEnchantState.java`
- `shu/technique/release/domain/DomainContactBilling.java`

修前字节在 `docs/开发记录/配置平衡-20261101/pre-repair-2/B/`。`author-B-manifest.json` 已记下这三项。`DomainAuraResolver`、`TechniqueHits`、网络、批次和命中实体没有改。

## 未完成

`classifyHit`、`classifyRoot`、`classifyConfirmed` 按约定的重载调用，当前树里的 `TechniqueHits` 还没有这三个方法，要等 A 的接口落地后一起编译。正伤抛出时，原来 `confirmTracked` 会凭已扣血标记留下收据；这个跟踪在 `TechniqueHits` 上，本单不能改，所以异常仍退回收据。正常返回不受影响。

变量名检查仍只报出未改的 `OriginalHumanoid.java` 第 165 行和第 202 行。统一编译没有跑，游戏也没有实测。
