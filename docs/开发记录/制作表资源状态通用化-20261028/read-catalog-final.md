我先读取这份任务说明，确认授权范围后再按它执行。任务是只读事实清单。我先定位资源、状态和领域的登记入口，再按符号核对默认值。代码里没有火种和炉心的登记。接下来按符号读潮湿、水势、魔力和领域周期的定义与默认值。只读事实清单。未改文件，未编译，未跑游戏。火种与炉心在 `src/main/java/com/bslx/XueXiMod` 中无登记。下面以现行代码为准；`docs/底座功能点.md` 里「攻击不施湿」「深海半径 5、100 tick、到期爆发 14」与现行领域定义不一致。

## 1. 已登记、可积累或消费的资源

**魔力**是通用施放消耗，不是术式链资源。玩家池在 `PlayerShuData`：初始 `magic = 100`、`magicUpperBound = 100`（约 42–43、157–166 行），NBT 键 `Magic` / `MagicMax`，外层 `xueximod_shu_data`（26、323 行附近）。入口是 `MagicPool.trySpend` / `spendInto` / `canAfford`（`MagicPool.java` 27–47、50 行）。玩家池先加法杖存货再扣；不够则整笔拒绝。回复每 20 tick 5 点（21–22 行）。显示费用：`Technique.manaCost()`（93–111 行）附魔走附魔定义；条件释放且 `explicitManaCost() >= 0` 用该显式值；有领域则用 `DomainDefinition.initialCost()`；规则术式为 0；否则 `explicitManaCost() >= 0` 用显式值，缺省 `ceil(基础伤害 × 2)`。`ReleaseDefinition.explicitManaCost()` 缺省 `-1`（131–132 行）。

**水势**是施术者链资源，与魔力分开。稳定存储名是玩家 NBT 浮点 `WaterMomentum`，没有 `ResourceLocation`，也不在 `ResourceStatuses`。范围是该玩家；单位是有限浮点，`WaterMomentum.UPPER_BOUND = 100f`（17 行），`setWaterMomentum` 非有限或非正写成 0，超过 100 截成 100（`PlayerShuData.java` 45–46、169–178 行）。可用额是余额减未提交预留（`available` 24–39 行）。`reserve` / `commit` / `release` 在 42–97 行；`credit` 按实际增量入账并截顶（114–119 行）。

生产：实际掉血给受害者；非水牢压强时攻击者也入账（`ReleaseBaseEvents.java` 138–146 行）；湿目标额外奖励走 `WetAttackReward`（按档，见下）；雾雨领域周期入账；潮印按条数给施术者。消费：`momentumSpend()` 缺省 0（`ReleaseDefinition.java` 140–142 行），现行注册没有覆盖它。让锋另走 `WaterPartingService.RESERVE_COST = 50f`（33、55–58 行），可用额必须严格大于 50。偏转经 `PlayerMomentumPurse` 再调同一套预留。重水不再扣这份账（见第 4 节）。

**潮湿数值**被查询层叫资源，但是实体属性状态，不是施术者池。`ResourceStatuses` 只内建 `xueximod:wetness`（14、23–29、80–92 行）。`ResourceView` 是当时的数值、档、剩余 tick（`ResourceView.java` 6 行）。全仓没有其它 `ResourceStatuses.register` 调用。

## 2. 已登记状态

只有潮湿。`ModMobEffects` 只注册 `xueximod:wetness`（`ModMobEffects.java` 16–17 行），外壳 `AttributeStateMobEffect`，颜色 `0x3A7CA5`。精确值在实体持久数据 `xueximod_attribute_states` 的整数键 `wetness`（`AttributeStateData.java` 9、18 行；`WetnessState.DEFINITION` 上限 100，10 行）。

档位 `WetnessState.tierOf`（15–28 行）：0 及以下为 0 档；1–25 为一档；26–50 为二档；51–75 为三档；76–100 为四档。药水 `amplifier = 档位 − 1`，所以一至四档是 0、1、2、3（`grant` 45–47 行；`AttributeStateMobEffect.wetnessForAmplifier` 64–75 行）。`grant` 只在服务端、目标存活且加点为正时累加并封顶，然后把持续时间刷成 `DURATION_TICKS = 1000`（50 秒，11、35–47 行）。`replaceExact` / `replacePreservingDuration` 改精确值但不刷回 1000（66–98 行）。效果移除时精确值归零（`AttributeStateMobEffect.java` 59–61 行）。

移速只挂在潮湿外壳上，按档固定比例，不按原版 amplifier 连乘：一、二档 −15%，三、四档 −30%（`WetnessTiers.java` 24–33 行）。同表还有攻击奖励水势 0 / 0 / 1 / 2 / 2，以及偏转相关资金与近战费用 20 / 20 / 15 / 15 / 10、箭费比 1 / 1 / 0.75 / 0.75 / 0.5、呼吸倍率 1 / 1 / 1 / 1 / 2。四档呼吸与深海领域耗氧取较大值后再封顶 2（`PressureBreath.java` 15–18、55 行注释）。

水牢结束时按成牢锁定档处理湿度，不重新 `grant`：二档把当前值整除 2 并保留剩余时间；其它档或湿度已空则 `clear`（`PrisonRuntime.java` 436–455 行）。牢寿命：一档 20 tick，二档 100 tick，三档为剩余潮湿时间的一半，四档等于剩余时间（30–31、417–432 行）。

领域里的缓慢是原版 `MobEffects.MOVEMENT_SLOWDOWN`，不是模组状态。持续接触载荷里的燃烧是 `setSecondsOnFire`，缓慢 amplifier 固定 4（`FlowSuccessPayload.java` 92–99 行）。锁定发光是原版发光，不是本模组状态。

**仅设计、代码未实现：** `docs/火属性二级玩法设计.md` 物理第 1 行。炉心 `mod:furnace_core`，自定义 MobEffect；amplifier 0 显示「炉心」，1 显示「炉心·过载」；持续时间无限。运行态 `FurnaceRuntime` 另存 `accumulatedFireBonus`（普通每秒 +4% 封顶 +30%，过载每秒 +6% 封顶 +60%），与药水 amplifier 不是同一个数。牛奶不能移除；死亡清除且不放蒸汽；主动冷却才放固定蒸汽。三式 `fire_heat_up` / `fire_burn` / `fire_cool` 也未进 Java。火种 `mod:fire_seed` 设计为挂在目标实体上的 0–100 整数，不挂施法者，不注册 CASTER 或 WORLD。档案 `BURST` / `SUSTAIN` / `SPREAD` 未实现。待制作里的燎原 `fire_liaoyuan`、燎里 `fire_liaoli`、焚天 `fire_fentian` 只在表上，Java 无这些标识；评审记录写明它们只消费火种，且公共消费能力尚未进代码。

## 3. 触发入口与「多个」如何表达

没有「一条术式挂一组状态」或「同时操作一组资源」的列表接口。现有表达是并列标量，或查询侧的与条件。

领域：`DomainNativePolicy` 固定潮湿周期、加点、延迟，以及水势周期、加点、延迟，另加缓慢四元组与耗氧倍率（4–16、18–39 行）。`DomainPulses.fire` 同一轮先收齐再给一次（28–51 行）。潮湿只给领域内敌对活物，多领域取更大加点，窗口内不重复给满（76–87、54–66 行）。水势只给完全处于领域内的主人或盟友玩家，`momentumAmount() > 0` 才入账（42–44、118–139 行）。领域维护扣的是魔力：开场 `initialCost × 份数`，之后每 `upkeepPeriod` 扣 `upkeepCost`，扣失败就关（`DomainService.java` 85–104、134–139 行）。`DomainAuraSpec.tideNeutral()` 只给友方那份潮印伤害，注释写明不因此施湿、不给固定水势、不灭火（7–24 行）。

命中：`ReleaseDefinition.wetnessGrant()` 缺省 0（218–219 行）。`HitPayloads` 把治疗、牵引、潮湿、灭火、上抛合成这一发的一份载荷（13–31、46–58 行）。持续接触用 `FlowSuccessPayload`：潮湿、燃烧秒、缓慢 tick、击退、治疗、牵引各自取较大值，方向留第一份；潮湿不按复制份数相乘；潮印不在这份载荷里（9–14、66–90 行）。潮印成功另算：每条目标 `WetnessState.grant(条数×5)`，施术者 `WaterMomentum.credit(条数×2)`（`TideMarkSuccess.java` 41–72 行）。它和该发自己的 `wetnessGrant` 是两次调用，不是一个合并字段。

自身/条件：水牢 `StatusQuery.and(resourceAtLeast(xueximod:wetness, 1), effectActive(同名))`（`WaterPrisonEffect.java` 71–76 行）。数值和药水必须同时在。单价 `SUCCESS_MANA = 40`（35、42–43 行）。`SkillSegmentMath.settledSegmentCost` 在单价大于 0 时预扣不含这段显式魔力，成功目标再记到收据（178–184 行）。关不进去不扣这 40。

出手水势：`SkillSegmentRunner` 在蓄力前 `reserve(momentumSpend())`，打断 `release`，放出 `commit`；无蓄力则立刻预留并提交（390–416 行）。现行注册全是 0，这条路径不改余额。`heavyMomentumPerCopy() > 0` 才会进 `HeavyWater.enter`（420–427 行），但进入实现已不再预留（见下）。

## 4. 旧表字段与已注册术式的现行值

字段对应代码，缺省即「不启用」：

| 旧列 | 代码 | 缺省 | 含义 |
| --- | --- | --- | --- |
| 潮湿周期 tick / 每次加点 / 延迟 | `wetPeriod` / `wetAmount` / `wetDelay` | 无领域则为无 | `period <= 0` 或未到 `startedAt + delay` 不触发；到期后 `(now - startedAt) % period == 0`（`DomainPulses.due` 69–73 行）。加点进入 `WetnessState.grant`，刷新 1000 tick |
| 水势周期 tick / 每次加点 / 延迟 | `momentumPeriod` / `momentumAmount` / `momentumDelay` | 同上 | 加点必须大于 0 才入账；单位是浮点水势，对象是领域内主人或盟友 |
| 命中潮湿 | `wetnessGrant()` | 0 | 这一发命中体上的一份加点，不按复制份数乘 |
| 重水预留 | `heavyMomentumPerCopy()` | 0 | 注释仍写「每份有效攻击预留的水势」；运行时已停 |
| 出手扣水势 | `momentumSpend()` | 0 | 放出前预留并提交，没命中也提交；蓄力打断退预留 |

`HeavyWater.enter` 忽略单价和份数，新会话 `heavy = false`、预留额 0（8–32 行）。`WetPressure.attach` 空方法，不再因潮湿门槛给缓慢或下压（9–18 行）。全仓没有 `.heavyMomentum(` 或 `.pressureWetness(` 调用。因此表上的重水预留和重压门槛即使留着数字，现行代码也不扣水势、不挂重压。门槛注释仍写 26 起为二档（`ReleaseDefinition.java` 321–326 行），只是字段语义，没有术式把它设成非 0。

已注册且真正写入这些字段的只有：

- `water_mist` 雾雨，`DomainDefinition.mistRain`（40–46 行）：盒 7×7×8；魔力开场 40、维持 8 / 20 tick、寿命 200。潮湿 10 / +2 / 延迟 10。水势 20 / +2 / 延迟 20。效果键 `mist_rain`。短构造把缓慢 amplifier 设为 −1（不施加），耗氧 0。
- `water_deep_pressure` 深海重压，`deepPressure`（89–96 行）：盒 7×7×3，六面开放；魔力同样 40、8 / 20、寿命 200。潮湿 10 / +4 / 延迟 10。水势周期 20、延迟 20，但加点 0，所以不产水势。缓慢 amplifier 0、持续 20、周期 10、延迟 10。耗氧倍率 2。效果键 `deep_pressure`。
- `water_blade` 缺月：`wetness(6)`（`WaterTechniques.java` 23–24 行）。无领域列。魔力按伤害 7 走 `ceil(7×2)=14`。出手水势 0，重水 0。
- `water_weeping_arc` 垂丝：`WeepingSettings.WETNESS = 3`，显式魔力 10（8–9、16 行；`ReleaseDefinitions.java` 22 行，参数序在 102–104 行：重水 0、重压门槛 0、命中潮湿 3）。无领域周期。
- `tide_mark` 潮印：不是领域列，也不是 `wetnessGrant`。命中成功后每条潮湿 +5、施术者水势 +2、另加 2 点伤害（`ModTechniques.java` 128–130 行；`TideMarkSuccess` 41–50 行）。
- `water_yielding_ribbon` 让锋：释放定义命中潮湿 0、出手扣水势 0。开干通道时固定预留 50 水势（`WaterPartingService.java` 33–58 行），这不是旧「出手扣水势」列。
- `water_prison` 水牢：不写领域周期，不施湿。读取已有潮湿；每个成功目标 40 魔力。结束时按上面的档位减湿或清空。
- `water_curtain` 水幕、`water_ring` 水环：领域潮湿/水势列都是缺省。水幕屏障生成魔力 8、维持 2 / 20、寿命 80、箭伤保留 0.5（`BarrierDefinition.curtain` 39–40 行）。水环护体生成 8、维持 2 / 20、寿命 200、卸伤 0.20（`GuardDefinition.personalRing` 44–45 行）。卸掉的伤害记水势，但不是周期加点。

`FieldZones` 仍能按半径 4.5、潮湿 2、维持 1，以及半径 5、潮湿 4 认出旧场地并在服务器启动时拆掉（103–116 行）。现行 `WaterTechniques` 不再注册这种场地。

## 5. 下拉候选与不能压平的项

**代码已有，可作选项：**

- 资源：魔力（玩家整数池，通用消耗）；水势（玩家浮点 0–100，链资源，带预留）。查询层另有 `xueximod:wetness`，但是状态的读口，不是第三种施术者资源。
- 状态：`xueximod:wetness` 潮湿。精确值 0–100，四档，amplifier 0–3，成功施加刷新 1000 tick。
- 领域上已并列、但不是「潮湿/水势」两列能覆盖的：原版缓慢，深海耗氧。命中载荷上已并列的还有治疗、牵引、灭火、上抛、燃烧秒、击退。它们今天不是数据表下拉项。

**仅设计：** 火种 `mod:fire_seed`（目标实体 0–100）；炉心 `mod:furnace_core`（amplifier 0 普通、1 过载，无限时长，另有火伤爬坡运行态）；三式升温/燃烧/冷却；档案 BURST、SUSTAIN、SPREAD；燎原、燎里、焚天。下拉可以预留名字，不能写成已实现。

**不能简单压平：**

- 潮湿不是普通 MobEffect。档位在 amplifier，点数在持久数据。只同步药水会丢掉 1–100 的精确值。水牢要求「数值 ≥ 1」并且「效果仍在」。`grant` 会刷新 50 秒，减湿走 `replace*`，不会刷新。移速、呼吸、水牢寿命、击湿奖励都读档位表，不读药水等级本身。
- 炉心不是 0–100 资源。它是施法者身上两档无限 MobEffect，加上单独保存的火伤累计。过载不是把普通炉心的点数加满，设计里也没有从过载降回普通档的路径。
- 火种不是第二份水势。设计把它挂在目标上，一次施法对同一目标最多产一次、耗一次，且规则术式不放大产量。水势在施术者身上，可预留、可被实际掉血普遍入账。
- 魔力不要并进链资源下拉。领域开场费和维持费已经是魔力；水牢单价也是魔力；和周期水势、命中潮湿不是同一列。
- 旧领域列是两套固定标量，不是「状态 ID + 资源 ID」列表。一次命中的多种效果靠载荷上的并列字段，潮印再额外各写一次。迁移时不要把潮印的 +5/+2、让锋的 50、或已停用的重水预留，写成新的通用周期行。
