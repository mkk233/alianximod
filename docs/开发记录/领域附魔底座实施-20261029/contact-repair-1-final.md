我先读取这份返修任务说明，确认授权范围后再改。裁决只要求修异常结算：扣血之后抛出不能退款，伤害还没生效也不能直接收费。我先核对现有伤害确认是怎么判定“已经生效”的。本地 1.20.1 里，`setHealth` 完成之后才会发 `ENTITY_DAMAGE`。正值伤害事件发生在扣血之前，所以异常路径要用扣血之后的这个信号来决定收不收费。先备份，再只改结算和确认接口。领域接触在异常退出时，已经按扣血之后的信号决定收费。正常命中路径没有改。写入已收束，待统一编译。本轮没有新增复审。

## 修复范围

授权文件内的主路径已经接上。`LivingDamageEvent` 发生在 `setHealth` 之前，所以没有把它当成已扣血。比较前后生命值会漏掉吸收和同步回调，也没有用。

依据是 Forge 1.20.1-47.3.33 里玩家和其它生物的 `actuallyHurt`：非零伤害都是 `setHealth` 返回之后，才 `gameEvent(ENTITY_DAMAGE)`。服务端把它发成 `VanillaGameEvent`。领域确认在这次调用上监听该事件，来源实体必须是本次目标。

还没有覆盖的一段：`setHealth` 已经返回，但 `ENTITY_DAMAGE` 还没发出。玩家在这段里是伤害统计，其它生物是第二次吸收写入。这两步自己抛出时，公开事件还看不到，费用仍会退回。要盖住它，需要在下面两处 `setHealth` 调用返回后加 Mixin：

- `net.minecraft.world.entity.LivingEntity.actuallyHurt`
- `net.minecraft.world.entity.player.Player.actuallyHurt`

这次没有这些文件的所有权，所以没有改，这一小段也不算已经解决。

## 资金和异常

`DomainContactBilling.apply`（第 92–120 行）仍先 `ManaPaymentReceipt.charge`。收据和原来源没变。

- 正常确认正伤，包括致死：`confirmTracked` 返回真，`retainShare` 一次。目标还活着才发火种和附魔载荷。
- 取消、无敌帧挡住、最终零伤，或确认返回假：退款一次，不发载荷。
- 扣血前抛出，包括伤害事件监听里抛出：`healthApplied` 为假，退款一次，然后把原异常抛出去，不发载荷，也不补血。
- `ENTITY_DAMAGE` 已经发出之后抛出：`retainShare` 一次，然后把原异常抛出去。不发成功载荷。`hurtConfirmed` 这时仍按未返回处理，潮印奖励也不会发。

`RuntimeException` 和 `Error` 都原样继续抛。不吞异常。

`hurt`、`hurtRoot`、`hurtConfirmed` 的返回语义没改。屏障、场地、护体仍走原来的 `hurt`。排序、领域寿命和关闭也没改。

## 改动

返修前快照在 `docs/开发记录/领域附魔底座实施-20261029/contact-evidence/repair-1/before/`，没有覆盖最初的 `contact-evidence/before`。

- `TechniqueHits.java`：新增 `confirmTracked`（第 101–127 行）和 `ConfirmedDamage`。`noteHealthApplied` 在第 147–160 行。只有推入监视的确认才会记账。
- `DomainContactBilling.java`：正价路径改用 `confirmTracked`。单价仍是每种基础附魔 1 点，多种相加。

清单：`docs/开发记录/领域附魔底座实施-20261029/contact-evidence/repair-1/file-list.txt`。

| 文件 | 末次写入 |
|---|---|
| `TechniqueHits.java` | 2026-10-29 22:36:13 |
| `DomainContactBilling.java` | 2026-10-29 22:36:15 |
| `repair-1/file-list.txt` | 2026-10-29 22:37:22 |

已跑 `powershell -NoProfile -File tools/check-name-gate.ps1`。这次改过的文件没有新的命名违规。退出码 1，仍是未改动的 `OriginalHumanoid.java` 第 165 行 `i`、第 202 行 `to`。

未跑编译、测试和游戏，也没有实际跑“扣血前抛出”和“扣血后抛出”两个分支。

写入已收束，待统一编译。
