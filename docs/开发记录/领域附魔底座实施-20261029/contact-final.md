我先读取这份任务说明，确认授权范围后再按其中的要求执行。任务已授权实现领域接触成功伤害计费。我先读方案、设计裁决和相关事实，再只改允许的文件。方案和裁决已对齐。接下来只读领域实例、接触结算和收据接口，确认计费该接在哪。领域服务里的维护扣款已经不在了。我再确认实例上的维护接口是否还被调用，然后备份并改接触计费。维护时钟的调用已经从领域服务拿掉。我开始改领域实例和接触计费，并保持非领域宿主的原结算。领域接触的成功伤害已经改成按冻结快照里的基础附魔种类收费。维护时钟已从领域实例去掉。写入已收束，待统一编译。

## 范围

只改了领域实例、接触扫描和附魔快照，并新增两份领域计费类。`DomainDefinition.java`、`DomainService.java`、`WaterReleaseDefinition.java`、`Config.java`、`MagicPool.java`、`ManaPaymentReceipt.java`、`TechniqueHits.java` 都没有改。读到的 `DomainService.tick` 已经不再调用维护接口。

## 基线与新增

基线原文在 `docs/开发记录/领域附魔底座实施-20261029/contact-evidence/before/`，相对路径与源码一致，复制时核对过字节数：

- `src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainInstance.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/contact/ContactEnchantState.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/enchant/EnchantmentSnapshot.java`

新增：

- `src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainContactCharge.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainContactBilling.java`

清单：`docs/开发记录/领域附魔底座实施-20261029/contact-evidence/file-list.txt`

## 费用与成败

单价是 `DomainContactCharge.MANA_PER_BASIC_ENCHANTMENT`，当前为 **1**（第 14 行）。`quote` 在第 16–29 行：

**费用 = 冻结快照中不同基础附魔种类数 × 1**

种类取 `EnchantmentDefinition.id()`，空标识才回退到术式 id。同一标识只计一次，用 `TreeSet` 收集（`EnchantmentSnapshot.distinctBasicTypeIds`，第 93–121 行；只读入口 `distinctBasicTypeCount` 第 49–51 行，`basicTypeIds` 第 54–56 行）。`damageOf`（第 62 行起）和效果读取仍按原条目累加，重复潮印仍会加伤害和条数，只是费用不因重复条目变多。

一次领域接触按这个顺序处理（`DomainContactBilling.apply`，第 81–112 行）：

1. 主人在线、领域仍活跃、修订号一致、接触伤害大于 0、目标通过 `ContactPolicy.mayContactAffect`、身体与领域相交、种类数大于 0。
2. 按上面的公式报价。
3. `ManaPaymentReceipt.charge(主人, 费用, false)` 预留。法杖优先、玩家池补足，沿用现有 `trySpendTracked`。失败返回 null，本次不调用伤害，领域保持开启，也不发「魔力不足」。
4. 预留成功后调用已有 `TechniqueHits.hurtConfirmed`，着陆为 `RESPECT_INVULNERABILITY`（第 127–130 行）。这里不走 `hurtRoot`，接触伤害不并领域光环。
5. 返回真才 `retainShare` 提交。取消、无敌帧挡住或最终零伤则 `receipt.refund`，退回原法杖和玩家池。每个事件只提交或退款一次。
6. 提交之后，领域仍活跃且目标还活着，才 `FireSeedGrant.deposit` 和 `applyEffects`（第 132–137 行）。致死仍算成功并扣费；目标已死则不再挂火种和附魔载荷。

单价以后若调成 0 或负数，`quote` 得到 0，这种接触仍做正伤确认，但不再预留。当前常量是 1，带种类的接触会走到预留。

`hurtConfirmed` 若在原版已经扣血之后抛出，收据会退回，这次改动不能把已经扣掉的血量自动加回去。

## 非领域

`ContactEnchantState.scan` 只有 `DomainInstance` 进入计费队列（第 83–86 行）。屏障、场地、护体和其他接触宿主仍走原来的 `TechniqueHits.hurt`（第 95–96 行），不去重附魔种类，也不收费。普通攻击、固定潮印光环、潮湿、水势、耗氧和封界没有改。生成费和开域附魔预扣没有动。没有新的 NBT，也没有绑定术式调度。

同一偶数拍里，非领域宿主仍在扫描循环中立刻结算；领域事件在该拍循环结束后再逐笔预留（`tick` 第 50–68 行，`settlePending` 第 55–79 行）。

## 排序

排序键在 `compareEvents`（第 144–154 行），全部用 `UUID.compareTo`：

1. 领域主人标识
2. 领域实例标识
3. 目标标识

同一实例、同一目标只结算一次（第 156–159 行）。不同领域实例、包括合法复制出来的多份，各自保留。收集和结算都走列表排序，不按 `HashMap` 的遍历顺序扣费。每一笔预留都读当时余额。

`DomainInstance` 已删除 `nextUpkeepAt`、`setNextUpkeepAt` 和构造时的下次维护时刻。寿命、`close`（第 70–79 行）、接触登记和视觉字段仍在。

## 检查与时间

已跑 `powershell -NoProfile -File tools/check-name-gate.ps1`。本次改过的文件没有命名违规。脚本退出码 1，因为未改动的 `OriginalHumanoid.java` 第 165 行 `i`、第 202 行 `to` 仍是旧违规，这次没有改它们。

未跑 `compileJava`、测试和游戏。

| 文件 | 末次写入 |
|---|---|
| `DomainInstance.java` | 2026-10-29 22:05:26 |
| `EnchantmentSnapshot.java` | 2026-10-29 22:05:32 |
| `ContactEnchantState.java` | 2026-10-29 22:05:35 |
| `DomainContactCharge.java` | 2026-10-29 22:06:26 |
| `DomainContactBilling.java` | 2026-10-29 22:06:34 |
| `contact-evidence/file-list.txt` | 2026-10-29 22:09:25 |

写入已收束，待统一编译。
