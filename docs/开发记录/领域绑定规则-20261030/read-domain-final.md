我先读任务说明，确认授权范围后再按它完成只读报告。任务是只读采集，不改文件。我先读本目录记录和领域相关源码，再按五个问题整理事实。只读采集完成。未改文件，未编译，未写制作表，未启动游戏。这不是正式正确性审查。前期接触伤害计费（设计 2/2、实现 3/3）不复审；`DomainContactBilling` 只确认它是另一条已交付路径。

## 1. 领域实例、服务与可放冻结数据的位置

`DomainInstance`（`domain/DomainInstance.java`）没有复制方法，也没有子链、下次触发刻或 `ReleaseInput` 字段。

| 内容 | 位置 |
|---|---|
| 创建 | 构造 29–41 行。`instanceId` 随机，`ownerId`、`ServerLevel`、几何、`DomainDefinition`、`startedAt` 一次写入。`revision` 固定为 1（20 行）。 |
| 寿命冻结 | `FrozenClock.freeze` 43–60 行。正寿命用 `ReleaseInput.expiryTick` / `expiryGameTime`；非正寿命直接相加。结果只进 `expiresAt`、`visualEnd`。 |
| 附魔冻结 | `bindContact` 63–68 行，把 `EnchantmentSnapshot` 交给 `ContactEnchantState`。这是开域时已有的冻结点，冻的是接触附魔，不是后续术式链。 |
| 关闭 | `close` 70–79 行：`active=false`，并关掉接触状态。没有退开域费，没有取消延迟任务。 |
| 几何与归属 | `geometry()` 85–87、`ownerId()` 136–139、`active()` 141–144、`hostLevel()` 151–154、`queryBox()` 156–159。`queryBox` 是包围盒再外扩 1 格，不是域内。 |

`DomainService`（`domain/DomainService.java`）：

- 存活表是 `ACTIVE`，按追加顺序（22、106–109 行）。`active()` 149–151 行返回这张活表，不是副本。
- `tryWave` / `tryWaveReserved` 39–57 行进入 `releaseWave` 59–116 行。`copies` 份各自再 `place(player)`，再经 `placedGeometry` 平移和放大（91–97 行）。同一只脚下，多份盒子重合，但是不同实例。
- 同刻去重：`PLACED_THIS_TICK` 72–84、114 行。键是主人、原生效果键、开域费、寿命，可选 `claimSuffix`。命中则直接返回 true，不再放第二份。
- 未预留时按 `initialCost * copies` 用 `MagicPool.canAfford` 和 `ManaPaymentReceipt.charge`（85–104 行）。收据不保存。预留路径不在这里扣开域费。
- `tick` 118–135 行先倒序：实例已关闭、主人不在、死亡、所在维度不是 `hostLevel`、或 `withinLife` 失败，就 `closeAt`。倒序扫完才 `DomainPulses.fire`（134 行）。本刻被关掉的实例不会再进原生脉冲。
- `closeOwned` 137–147 行按主人清全部。`discard` 189–193 行只关掉还没进表的待发布实例。
- `placedGeometry` / `enlarge` 161–177 行：先按接触点平移成 `AxisBoxGeometry`，再 `scaledFromGround`。`ReleaseInput` 只参与寿命和放大，不留在实例上。

`DomainDefinition.place` 71–73 行只是放置器。雾雨 62–68 行：脚下 7×7×8，费 40，寿命 200，封闭。深海重压 120–127 行：7×7×3，六面开放。`upkeepCost` / `upkeepPeriod` 10–13、79–93 行已废弃，服务不再用来扣费或节拍。

`DomainGeometry` 只有包围盒、完全包含、相交、位移区间、边界裁剪、身体放得下（`DomainGeometry.java` 7–22 行）。实现仅 `AxisBoxGeometry`。盒子在生成时写成世界坐标，不跟随玩家（`AxisBoxGeometry.java` 6–22、40–43 行）。

`DomainBound` 只提供主人、几何、是否存活、边界和放行（`DomainBound.java` 9–22 行）。

`DomainPulses.due` 69–74 行是现成节拍：`period > 0` 且 `now >= startedAt + delay` 时，`(now - startedAt) % period == 0`。它是私有的，只驱动潮湿、减速、水势。延迟为 0、周期 20 时，开域那一拍也会到期。原生脉冲不生成攻击。

**建议（不是定案）：** 冻结子链和下一次触发刻放在每份 `DomainInstance` 上，或由 `DomainService` 按 `instanceId` 侧表持有。定义、几何和废弃维护周期都不适合。触发应排在倒序关闭之后，并对 `ACTIVE` 做快照再遍历；在 `DomainPulses.fire` 的正向 for-each 里往活表加实例会并发修改这张 `ArrayList`。同一波重合的多份盒子若各存一份子链，会各触发一次。

## 2. 域内随机点，以及方向、落点、锁定怎么进攻击

没有域内随机点接口。可用的真实范围是 `geometry().bounds()`，即平移和放大之后的轴对齐盒。不要用 `queryBox()`。`fullyContains` 有 1e-4 的外缘余量（`AxisBoxGeometry.java` 8、55–64 行），边界上的点会被当成在内，但没有 `Vec3` 包含判断，也没有内缩采样。

`CastFrame` 的采样不能直接当领域采样。`jitter` / `volumePoint` 是中心周围半边 1.0 或 1.5 的立方（`CastFrame.java` 88、125、177–184、203–216、273–278 行）。垂落再把点抬高 8 格，并可能钳到 `groundLevel`。这既不等于 7×7×8 的领域，抬高后也会出盒。

正常攻击的中心和方向在 `SkillSegmentRunner.runSegment` 460–646 行：

- 有接触点时，位置和方向改成接触点（460–465 行）；生成前再次强制 `overrideGeometry`、`origin`、`anchor`、`look`（620–631 行）。
- 实体锁定调用 `LockTargetService.resolve(player)`，读的是玩家标记（542–546 行）。
- 可改弹道且落点指定，或龙卷风：中心用 `ReleaseBatch.crosshairCenter`，准星射线最多 32 格（572–573、104–132 行）。
- 可改弹道且交错、又没有实体锁定：中心在玩家身体附近（574–576 行）。
- 不可改弹道的落点指定：`resolveAnchor`，准星约 8 格（577–578、135–168 行）。
- 其余中心是眼睛（580 行）。方向默认是传入的 `lookVec`，没有接触方向时就是玩家朝向。

`CastFrame.forAttack` 107–116 行：没有交错、没有落点指定、也没有可改路径覆盖时，`overrideGeometry=false`，不写 `origin`。不可改弹道走 `slashGeometry`：只抖动生成点，`look` 仍是 `baseLook`（122–136 行）。可改弹道走 `projectileGeometry`：落点或无锁定的交错才设 `volumeHalf=1.5`（163–190 行）。`aimFor` 219–228 行：垂落方向固定向下；交错且不是垂落才随机方向；否则用 `look`。

`TechniqueHitSpec.spawnReported` 213–248、284–298 行只在 `overrideGeometry` 时采用帧上的点。可改弹道的垂落、直线会写 `start` 和 `aim`；带体积采样时，生成循环再用 `volumePoint` / `aimFor` 覆盖。`TechniqueHitEntity.applySpec` 518–521 行：`start` 为空就用眼睛，`aim` 为空就用当前准星。

垂丝 `water_weeping_arc` 在 `ReleaseDefinitions.java` 24 行：`Trajectory.FUNCTION`，`trajectoryMutable=false`。无覆盖时，`WeepingCurtainEffect` 42–55 行用眼睛、水平准星前 2.2 格和复制横排，不读帧原点。有覆盖时，效果自己不写起点，由生成器的 `FUNCTION` 分支搬 `frame.origin` 和 `frame.look`（`TechniqueHitSpec.java` 227–233 行）。水帘唇口再抬 4 格，沿世界竖直轴下落；`forward` 只决定帘面朝向（`WeepingCurtain.java` 21–22、57–67、95–99 行）。所以只改一个域内点时，必须同时让 `overrideGeometry` 为真并写入 `origin`；否则垂丝仍从玩家眼睛放。方向即使写入，也不改变下落轴。

飞弹 `magic_arcane_missile` 在 `ReleaseDefinitions.java` 13 行：直线、`trajectoryMutable=true`。`MagicArcaneMissileEffect` 34–69 行不写 `start` 和 `aim`。无覆盖时生成器不搬帧，实体就从眼睛沿准星飞。有覆盖且带体积采样时，点在 1.5 立方内，不是领域盒。`lockThisCast` 为真时，`ProjectileMotion` 15–19 行每拍读主人的锁定标记；没有标记就沿 `forward` 飞（`TrajectorySampler.java` 53–61 行）。垂落采样忽略目标点，水平位置留在起点（63–66 行）。

领域本身不走命中体。`WaterFormEffect` 31–32 行见到 `domain()` 就返回。开域入口是 `SkillUsePacket` 572–588 行、`SkillChainPlan` 369–389 行、`CastExecutionSession` 305–307 行，都调用 `tryWave` 或 `tryWaveReserved`。前两处用玩家脚下；接触释放才把盒子平移到接触点。开域之后链照样继续，攻击段仍会当次按键执行。

## 3. 扣款、退款、延迟取消，以及主人状态

可用扣款是 `ManaPaymentReceipt.charge`（`ManaPaymentReceipt.java` 39–47 行）和 `MagicPool.trySpendTracked` / `canAfford`（`MagicPool.java` 35–47、50–86 行）。付得起才扣，先主手法杖存量，再玩家池。`charge` 在费用小于等于 0 时给零额收据；不够返回 null。`canAfford` 只看当前主手和玩家池。

退款是 `refund` / `refundShare` 49–87 行，提交是 `retainShare` 93–102 行。收据记下开扣时的法杖身份；退款按身份搜背包，不退给后来拿在手上的另一根法杖。法杖已经不在背包时，法杖那一份不会回到任何容器。玩家必须仍是原 UUID。`MagicPool.spendInto` 93–110 行可往同一张收据追加，但法杖身份冲突则整笔不动。`LAST_RECEIPT` 每个玩家只有一张，后一次 `charge` 会换掉映射里的引用；已经拿在手里的收据对象还在。

一次按键的正常费用不在领域服务里。按键收据由链上 `UncommittedShare` 分份额，提交或 `cancel` 退款（`UncommittedShare.java` 37–68 行）。双重释放用 `GroupBudget.reserve` 预扣，`commit` 留下份额，`settle` 退未提交部分（`CastExecutionSession.java` 492–528 行）。领域开域费是另一笔 `initialCost * copies`，收据随即丢掉，关闭不退。

延迟任务在 `ChainScheduler`（`chain/ChainScheduler.java` 20–31、34–58 行）。到时就跑，没有按主人、领域或任务号取消的接口。已有的提前返回是调用方自己写的：

- 蓄力段：`ChargeGate.same` 且玩家存活才继续（`SkillSegmentRunner.java` 472–496 行）。
- 双重释放第二段：会话已关、或 `continues` 失败就结束。`continues` 要求存活、仍在施法维度、蓄力代次未变（`CastExecutionSession.java` 262–268、444–446 行）。
- 延后开域：`castContinues` 同样三项（`SkillChainPlan.java` 383–389、730–731 行）。失败只是那个任务返回，任务仍会到点被调用。

主人结束集中在 `ReleaseBaseEvents.endCast` 194–203 行，由死亡、下线、跨维触发（151–169 行）。它会打断蓄力、取消该玩家的 `CastExecutionSession`、再 `DomainService.closeOwned`。`tick` 129–131 行再用主人缺失、死亡、维度不同、到期收一遍。换武器没有关闭入口。领域不跨维迁移，几何留在创建时的 `ServerLevel`。

尚不存在的能力：按领域取消已排任务、把开域费或周期费记在实例上以便关闭时退、换武器后仍扣开域那根法杖、主人离线时退款、周期触发的重入保护。`ChargeGate.interrupt` 是玩家级的，死亡、下线、跨维会打断该玩家全部蓄力，不是某一份领域。

接触计费的缺魔跳过并保留领域，是已交付边界，这里不复审其实现。周期绑定若要同样“缺魔只跳过这一发”，要走新的一次 `charge`，不能复用接触单价。

## 4. 多份领域同时触发，以及无人也要生成

同时存在的设施只有 `ACTIVE` 追加顺序。关闭扫描是倒序；原生脉冲是关闭后的正向遍历。没有按实例排序的第二队列，也没有每份独立的账单。同刻相同去重键会并成一次成功。不同效果键、或带不同 `claimSuffix` 的接触释放，可以各留一份。

若周期触发跟着脉冲之后、按快照的追加顺序对每份实例各做一次 `charge`，顺序就是入表顺序，费用也按次独立。倒序只表示同一拍里谁先被关掉。同一主人的两次 `charge` 会串行改同一魔力池，并换掉 `LAST_RECEIPT`。

无人也生成，现有原生态只是“到点仍计算，没有合格生物就不给效果”。攻击生成没有“域内有目标才放”的开关；`TechniqueHitSpec.spawn` 不查领域内实体。会把“域内随机、不读准星、没目标也放”挡回去或拉回玩家的，是这些能力：

- `overrideGeometry` 为假：垂丝和飞弹都回眼睛和准星。
- 子链里的落点指定、交错、实体锁定仍走 `crosshairCenter`、`resolveAnchor`、`LockTargetService.resolve`。
- `lockThisCast`：直线每拍转向主人标记；没有标记才沿已写方向飞。垂落不转向。
- 跟随准星：`FollowCasterMotion` 每拍把原点改回眼睛、方向改回准星（`FollowCasterMotion.java` 19–46 行）。`LiaoliEffect` 把 `keepFollowing` 设为真后，生成器会清掉 `start` 和 `anchor`（`TechniqueHitSpec.java` 309–313 行）。
- `requiresHeldSword`：主手不是剑时，链前有 `HeldSwordGate`；存续中 `TechniqueHitEntity.releaseStopped` 877–879 行也会停。幻剑另要武器接受剑技，否则效果直接返回（`PhantomSwordEffect.java` 21–23 行）。默认原点在眼后，只有 `overrideGeometry` 且已有 `origin` 才用帧（30–37 行）。
- 剑来按眼睛周围 20 格找剑，不读领域点（`SwordComeEffect.java` 19–23 行）；没有剑就是空放。
- `releaseSlot >= 0` 且没有按住对应键，会在存续中停止（`TechniqueHitEntity.java` 881 行）。周期自动放没有按住状态。
- 蓄力大于 0 的攻击会再排 `ChainScheduler`，只检查玩家存活和蓄力代次，不检查领域还在不在。
- 领域、屏障、护体、零伤条件术式不会变成这次攻击的命中体。

因此周期触发若直接复用 `runSegment`，仍会读准星，并继承上述限制。要满足“没目标也放”，调用前得写成覆盖几何，并按术式开关判断持剑、跟随、操纵剑和蓄力。

## 5. 可能改动的文件、共享点、待确认

**代码事实：** 领域侧今天能挂住的只有几何、寿命、主人和接触附魔快照。20 拍攻击、域内随机点和冻结子链都没有落点。

**建议中的领域侧文件：** `DomainInstance.java`、`DomainService.java`。节拍不要写进 `DomainPulses` 的潮湿逻辑，也不要恢复 `upkeepPeriod`。若随机点要跟着形状走，再给 `DomainGeometry` / `AxisBoxGeometry` 加采样；只读 `bounds()` 也可以先不做接口。`ManaPaymentReceipt` 和 `MagicPool` 现成够用，除非必须冻开头的那根法杖。不要改 `DomainContactBilling`、`DomainContactCharge`。

**和链作者共享、只能一人写：**

- `SkillUsePacket.java`、`SkillChainPlan.java`：开域后链仍继续；捕获“领域后的规则直到第一个攻击”，以及开域当次不再打出这段，都在这里。
- `SkillSegmentRunner.java`：准星中心、锁定和 `effect.execute` 都在这里。周期触发要避开准星，就要新入口或新参数。
- `CastFrame.java`、`TechniqueHitSpec.java`：覆盖几何和垂落抬高 8 格。垂丝、飞弹可以不改，前提是调用前已经写好 `overrideGeometry`、`origin`、`look`。
- `CastExecutionSession.java`：接触开域和延迟第二段。
- `ReleaseDefinitions.java`：新规则的登记。`WaterReleaseDefinition.domain` 106–113 行只包现有领域定义。
- `ReleaseBaseEvents.java`：只有死亡、下线、跨维关领域。换武器是否关闭，改这里就会和生命周期作者重叠。

**待确认：**

- 随机点是整盒均匀，还是要内缩；方向是随机单位向量、指向第二个随机点，还是垂落固定向下。垂丝下落轴仍是竖直的。
- 第一发在开域当拍，还是满 20 拍之后。现成 `due` 在延迟为 0 时会打在开域拍。
- 冻结子链里若仍带落点指定、交错或锁定，现有执行会读准星。
- 同一波多份重合领域是否各扣一次、各放一次。
- 缺魔是跳过这一发并留领域，还是停掉后续触发。
- 换武器后扣当前主手，还是开域那根法杖。换武器现在不关领域。
- 开域当次是否仍执行被绑定的攻击。记录里此项尚未定案；当前代码会执行。
