我先读取这份提交任务说明，确认授权范围后再做。上次留下的缺口是 `setHealth` 返回后、`ENTITY_DAMAGE` 发出前仍可能抛出。我先核对本地 Forge 的注入点，再只加最小确认钩子。两个新 Mixin 还不存在。我对照本地 Forge 字节码，确认 `setHealth` 在两个 `actuallyHurt` 里的位置和签名。字节码已对上：两边的 `actuallyHurt` 都只有一次 `setHealth`，而且都在它返回之后才发 `ENTITY_DAMAGE`。玩家覆盖了自己的方法，不会走进生物那一版。接下来用返回后的钩子替换较晚的事件监听。确认点改到 `setHealth` 返回之后。`ENTITY_DAMAGE` 仍在其后，不能再当扣血证据。Mixin 里的原方法必须走 `this` 上的 shadow，这样才会替换成原来的 `actuallyHurt`，不会再套一层。上一份报告里的窗口已经关上。`setHealth` 返回之后、`ENTITY_DAMAGE` 之前的语句再抛出，也会按扣血完成记账。

## 费用与异常

付费路径仍是先 `ManaPaymentReceipt.charge` 预留，收据来源不变。`DomainContactBilling` 的判定没有改：正常返回只看 `hurtConfirmed` 的布尔值；异常只看 `healthApplied`。

- **setHealth 尚未返回就抛出**（含 `LivingDamageEvent`、护甲、`onLivingHurt`，以及 `setHealth` 自身在返回前抛出）：`healthApplied` 为假，`refund` 一次，异常原样抛出，不发成功载荷。`setHealth` 本体是 `SynchedEntityData.set` 之后立刻 `return`；这次调用没有返回，就不算扣血完成。
- **setHealth 已经返回后抛出**（生物随后的 `setAbsorptionAmount`，玩家随后的 `awardStat(DAMAGE_TAKEN)`，以及 `gameEvent`、击退、死亡、音效、进度）：`healthApplied` 为真，`retainShare` 一次，`finally` 不再退款，异常原样抛出，不发成功载荷，也不补血。
- **正常返回且确认为正伤**：`retainShare`，然后才发载荷。无敌、取消或零伤害仍退款，不发载荷。魔力不足时 `charge` 返回空，不进入伤害。

`Error` 继续按 `Error` 抛出，其它异常不吞掉。Mixin 的 `finally` 只出栈，不接住异常。

## 确认点与来源

确认点是对应 `actuallyHurt` 里那一次 `setHealth` 返回，不是生命值差额，也不是 `ENTITY_DAMAGE`。上一版 `VanillaGameEvent` 监听已删除，避免较晚事件误判。

- 生物：`LivingEntity.actuallyHurt` 偏移 147，owner 是 `LivingEntity.setHealth(F)V`。返回后才是吸收量与 `ENTITY_DAMAGE`。
- 玩家：`Player.actuallyHurt` 不调用生物版。偏移 133，owner 是 `Player.setHealth(F)V`。返回后才是 `DAMAGE_TAKEN` 与 `ENTITY_DAMAGE`。
- `Player.hurt` 转入 `LivingEntity.hurt`。后者两处 `actuallyHurt` 互斥，都包在 `try/finally` 里，原方法只调用一次。`ServerPlayer` 没有自己的 `actuallyHurt`。

只有 `confirmTracked` 压入监视。监视记下本次目标与 `playerAttack` 的攻击者。`noteHealthCommitted` 要求当前帧就是这层监视绑定的帧，并且目标与 `DamageSource.getEntity()` 都一致。内层同目标同来源不会抢走外层仍在栈上的绑定；内层先结束则外层可以重新绑定。没有监视时不压栈，普通 `hurt` / `hurtConfirmed` 的返回值和无敌帧不变。

## 改动与注册

- `TechniqueHits.java`：98–183、241–243、252–272 行。跟踪、入栈、出栈和 `setHealth` 返回记录。
- `LivingDomainDamageCommitMixin.java`：23–37 行包装两处 `actuallyHurt`；40–51 行在 `LivingEntity.setHealth` 返回后记录。
- `PlayerDomainDamageCommitMixin.java`：18–29 行在 `Player.setHealth` 返回后记录。
- `xueximod.mixins.json`：33–34 行追加这两个类名。`plugin`、`client` 和其余条目未动。
- `DomainContactBilling.java` 未改。异常时仍按 `outcome.healthApplied()` 决定 `retainShare` 或 `refund`。

未改其它 Mixin、`TideMarkDamageScope`、`MagicPool`、`ManaPaymentReceipt` 和伤害公式。原始 `before` 与 `repair-1/before` 未覆盖。

## 静态证据与未运行项

依据写在 `docs/开发记录/领域附魔底座实施-20261029/contact-evidence/repair-commit/bytecode-actuallyHurt.txt`。来源是本地 Forge `1.20.1-47.3.33` parchment recomp 的 `javap`。清单在同目录 `file-list.txt`。

`name-gate` 退出码 1，只有未改动的 `OriginalHumanoid.java:165 i` 与 `:202 to`。本次文件没有新的命名失败。

没有运行 Gradle、`compileJava`、测试或游戏。以上是静态核对，不是新的复审，也不是游戏内验证。

## 最后写入时间

| 文件 | 时间 |
| --- | --- |
| `TechniqueHits.java` | 2026-10-29 22:49:54 |
| `PlayerDomainDamageCommitMixin.java` | 2026-10-29 22:49:58 |
| `xueximod.mixins.json` | 2026-10-29 22:49:59 |
| `LivingDomainDamageCommitMixin.java` | 2026-10-29 22:50:34 |
| `bytecode-actuallyHurt.txt` | 2026-10-29 22:50:42 |
| `file-list.txt` | 2026-10-29 22:50:45 |
| `DomainContactBilling.java`（未改） | 2026-10-29 22:36:15 |
| `repair-commit/before` 的 `TechniqueHits.java` | 2026-10-29 22:36:13 |
| `repair-commit/before` 的 `xueximod.mixins.json` | 2026-10-29 00:33:27 |

写入已收束，待统一编译。
