我先通读任务说明，确认授权范围后再按其中的要求产出完整报告。任务是两路实现的集中只读审查。我先读作者终稿、任务书和改动基线，再按六项核对实际代码。作者终稿已对齐。接下来对照改前快照和现行源码，核对维护停用、接触计费和资金路径。本次两路实现未发现需要返修的 P1 或 P2。结论只覆盖下面核对过的源码和改前快照；没有跑编译、测试或游戏，不能写成运行通过。

## 范围

对照了 `lifecycle-evidence/before`、`contact-evidence/before` 和现行源码。本次生产改动是三份生命周期文件、`DomainInstance`、`ContactEnchantState`、`EnchantmentSnapshot`，以及新增的 `DomainContactCharge`、`DomainContactBilling`。契约只改了 `ReleaseContainerEnergyCheck` 第 81 行。没有把未跟踪的整套领域代码当成这次新增。

## 1. 维护停用

领域存续扣款已经从定义、服务、实例和水域适配器一起停掉。

- `DomainService.tick`（118–135 行）只在主人不在、死亡、维度不一致或寿命到期时 `closeAt`（183–187 行），然后 `DomainPulses.fire`。改前 134–139 行的 `nextUpkeepAt` 扣款和魔力不足关闭已经不在。全文件不再调用 `upkeepCost`、`upkeepPeriod`、`nextUpkeepAt`。
- `closeOwned`（137–147 行）仍在。死亡、下线和跨维仍走 `ReleaseBaseEvents.endCast`（202 行）进 `DomainService.closeOwned`。同一服务器 tick 末尾先领域 tick、再接触 tick（181–182 行），所以主人失效会先关域，接触扫描看到的是已关闭宿主。
- `DomainDefinition.mistRain`（62–69 行）和 `deepPressure`（120–127 行）走新构造（27–31 行），生成费 40、寿命 200，维护参数写成 0、0。盒子和原生周期与改前一致。旧构造（37–60 行）仍保存传入的维护值，但 tick 不再读取。`upkeepCost` / `upkeepPeriod`（83–93 行）标了弃用。
- `DomainInstance` 构造（29–41 行）不再计算下次维护时刻，`nextUpkeepAt` / `setNextUpkeepAt` 已删除。`close`（70–79 行）仍先关接触再标非活跃。寿命、几何、视觉字段还在。
- `WaterReleaseDefinition.domain`（104–112 行）不再把 `domain.upkeepCost()` 写入场地维护，该字段保持默认 0。`zone`（84–91 行）和 `barrier`（94–101 行）仍写各自维护费。领域的 `fieldRadius` 仍是 0，`FieldSpawner`（57–58 行）不会为它建场地。

护体、屏障、场地的维护扣款仍在 `GuardStates`、`BarrierBody`、`FieldZones`。

## 2. 只对领域接触收费

`ContactEnchantState.scan`（83–86 行）只有 `DomainInstance` 进入 `DomainContactBilling.collect`。屏障、场地、护体、监牢仍在 87–104 行走原来的 `TechniqueHits.hurt`。

单价是独立常量 `DomainContactCharge.MANA_PER_BASIC_ENCHANTMENT = 1`（第 14 行），不读 `Config`，也不是旧的 8 点维护。`quote`（16–29 行）等于冻结快照里不同基础附魔种类数乘 1。种类来自 `EnchantmentSnapshot.distinctBasicTypeIds`（93–121 行）：`EnchantmentDefinition.id()`，空标识才回退术式 id，`TreeSet` 每种一次。`damageOf`（62–80 行）仍按原条目累加，重复条目会加伤害，费用不加。

接触状态每 2 tick 采样一次（50–52 行）。`collect` 要求接触伤害大于 0、目标存活、`ContactPolicy.mayContactAffect` 和身体相交（33–41 行）。预留发生在 `hurtConfirmed` 返回真之后（99–102 行）。采样本身不扣费；无敌帧挡住时会预留再退回。

当前五种基础附魔标识都非空，而且 `magicDamage` 为正。能打开接触状态的快照，种类数和接触伤害是一起冻结的。

## 3. 预留、正伤、退款

`apply`（81–112 行）对单价 1 的实际路径是：资格或种类数不够就直接返回；然后 `ManaPaymentReceipt.charge(主人, 价格, false)`。`false` 沿用 `MagicPool.trySpendTracked`（50–86 行）的静默失败，不发「魔力不足」。失败返回 null，不调用伤害，不关领域。

法杖优先、玩家池补足发生在 `trySpendTracked`。退款走 `receipt.refund`（49–86 行），按原法杖身份和玩家池拆回。`DomainContactBilling` 没有调用 `MagicPool.restore`。

`confirmDamage`（127–130 行）调用 `TechniqueHits.hurtConfirmed`，着陆为 `RESPECT_INVULNERABILITY`，不走 `hurtRoot`，接触伤害不并领域光环。成功与否由 `TideMarkDamageScope.finish`（54–84 行）决定：`hurt` 为真、伤害事件已配对、未取消、最终数量大于 0。监听在 `ReleaseBaseEvents.onDamage`（116–120 行）。

- 无敌帧挡住：`hurt` 返回假，退款，不挂载荷。
- 事件取消或最终数量为 0：`finish` 返回假，退款，不挂载荷。本仓取消点在攻击事件、丢物品和弹射物撞击，不在 `LivingDamageEvent`。
- 正伤：`retainShare(price)` 后把本地 `committed` 设为真，`finally` 不再退款。
- 致死：`finish` 不要求目标还活着，费用留下。`deliverPayload`（132–137 行）在领域仍活跃且目标还活着时才给火种和附魔载荷。
- 同一收据要么 `retainShare` 要么 `refund` 一次。`refundShare` 在已回滚状态会直接返回。

预留和 `try` 之间没有空隙。确认前再次 `eligible`（114–125 行）；此时领域已关则退款且不伤害。

`hurt` 若在已经扣血之后抛出，赋值完不成，`committed` 仍为假，收据会退回。这条路径本仓的接触监听没有必然抛出的证据，不单列要修。

## 4. 顺序、副本、生成费

领域事件在该偶数拍的扫描结束后一次性结算（65–67 行，55–79 行）。排序键是主人 UUID、领域实例 UUID、目标 UUID（144–154 行）。每一笔 `charge` 都读当时余额，不共用扣前余额，也不按 `HashMap` 迭代。

同一实例、同一目标在这一拍只结算一次（156–159 行）。不同实例各自保留，包括同一次生成的多份副本。

非领域宿主仍在扫描循环里按 `ACTIVE` 逆序立刻结算，彼此相对顺序还在。领域整批移到循环之后，所以同一拍里若非领域宿主先打中同一目标，领域这次确认可能被无敌帧挡住并退款。这是先收齐再按稳定键扣费的直接结果。

生成费仍是 `DomainService.releaseWave` 的 `initialCost * count` 一次（85–105 行），与改前相同。接触计费不进入这条账单。同一 tick 的声明串去重也还在（82–84 行）。

## 5. 授权外行为

`damageOf`、`read`、`applyEffects` 与改前一致。普通攻击、箭和根术式仍由 `DomainAuraResolver.merge` 加光环，光环不进接触收据。`tideNeutral()` 仍是潮印伤害，只加在这些攻击上。原生潮湿、减速、水势、耗氧、封界仍由未改的 `DomainPulses.fire` 处理，不收费。

没有新的 NBT、开关、垂丝绑定或同名替换。`TechniqueHits`、`MagicPool`、`ManaPaymentReceipt`、`Config` 不在本次写入里。

## 6. 接口与 name-gate

两路能对上。`DomainService` 第 97 行仍用原来的 `DomainInstance` 构造。服务端不再调用已删除的维护时钟。计费类不要求 `DomainDefinition` 增加字段。`EnchantmentSnapshot.from` 的对外签名未变，种类名单只在私有构造里增加。

静态引用没有断点。`instanceof DomainInstance` 与仓库里已有的模式匹配相同。`ReleaseContainerEnergyCheck` 第 81 行现为生成费 40、维护费 0、周期 0，与 `mistRain()` 一致。水环（132、143 行）、水幕（215 行）和深海重压的寿命 200、生成费 40（113–114 行）仍是原检查。深海重压工厂同样是维护 0、周期 0，这条测试没有再锁这两个读数；契约任务只同步了第 81 行。

`WaterBladeMomentumCheck` 第 127 行仍调用旧构造，参数是维护费 0、周期 20、寿命 80。它能编过，而且 tick 不会按这个周期扣费。

name-gate 这次没有重跑。脚本会在需要时编译到 `build/namegate`。对照规则看过本次新标识，没有短于 3 字符的名字，也没有禁用缩写。作者两次报告的退出码 1 只指向未改的 `OriginalHumanoid.java` 第 165 行 `i`、第 202 行 `to`。

## 要修项

没有。

## 未知

未执行 `compileJava`、测试和游戏。正伤、无敌帧、缺魔跳过和光环不加价的判断来自源码路径，不是实测结果。
