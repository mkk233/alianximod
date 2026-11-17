我先读这份任务说明，按它写明的只读范围核对可变定义，再交完整报告。未知点只剩三处：水系定义会不会登记后被改、`ChainSegment` 与 `members` 的身份，以及定义 getter 是否读世界。我按调用点核对。本仓里，`WaterReleaseDefinition` 的字段只在放入 `ReleaseRegistry` 之前被改写。`ChainSegment` 的字段不可变，但相等性按对象身份；`CompiledReleaseStep.members` 保存的是原列表里的同一批对象，且列表本身没有做成不可变。

## 1. WaterReleaseDefinition

文件是 `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\WaterReleaseDefinition.java`。`final` 的只有 `releaseId`、`trajectory`、`moving`、`lifetimeTicks`、`speed`、`thickness`（第 12–17 行）。其余第 18–51 行都可写：弹道可变、初瞄、持剑、宿主组、弧带、拍号、同组视觉、`energyScale`、`explicitCost`、场地半径、维护费、场地寿命、拉拽、潮湿、重水、重压门槛、净化、挡路、压力、击退、抬升、卸力、减速、收招伤害、外观、曲线、形状、跨度、长度、护体、屏障、领域、条件效果、来袭偏转。

和链结构、费用、冷却、输入倍率有关的写入：

- `energy`（第 133–135 行）写 `energyScale`。`energyScale()`（第 331–332 行）读它。类没有覆写 `chainEnergyScale()`。
- `swordForm`（第 67–76 行）把 `trajectoryMutable` 写成 `false`，并打开初瞄、持剑、宿主组、弧带、拍号、同组视觉、偏转。`role()` 仍恒为 `ChainRole.ATTACK`（第 321–322 行）。它不写费用，也不写冷却；冷却在 `Technique` 上。
- 费用和编译分支用的 `explicitCost`、`upkeepCost`、`guard`、`barrier`、`domain`、`conditionalEffect` 只在工厂里赋值：`zone` 第 84–90 行，`barrier` 第 94–100 行，`domain` 第 104–111 行，`bodyGuard` 第 114–118 行，`conditional` 第 123–129 行。没有对应的公开后续 setter。
- `wetness`、`pull`、`span`、`along` 等其余 fluent 改的是命中或几何字段，不改 `role`、链能量、显式魔力或冷却。

依赖非 `final` 字段的 getter 从第 270 行起：`acceptsTargetOverride`、`acceptsInitialAim`、`requiresHeldSword`、`submitsHostGroup`、`usesArcBand`、`tracksBeatCycle`、`deflectsProjectiles`、`separatesCohortVisuals`、`acceptsPathOverride`、`trajectoryMutable`、`energyScale`、`explicitManaCost`、`fieldRadius`、`guard`、`barrier`、`domain`、`conditionalEffect`、`fieldUpkeepCost`、`fieldLifetimeTicks`，以及第 376–456 行的力度、形状和缩放。`id`、`trajectory`、`moving`、`lifetimeTicks`、`speed`、`thickness` 读 `final` 字段。`kind`、`tag`、`role` 返回常量。

调用只出现在 `D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\WaterTechniques.java`：`shot(...).along.span.energy.wetness.swordForm` 在第 23–25 行，`ReleaseRegistry.register(blade)` 在第 29 行；水牢在第 46 行建好、第 51 行登记；领域、屏障、护体在第 82、91、100 行把工厂返回值直接登记。本仓没有登记之后再调用这些 fluent 的路径。方法仍是 public，本仓之外是否再改，检索范围不包括。

## 2. ChainSegment 与 members

`D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\chain\ChainSegment.java` 是 `final` 类。`actionIndex` 与 `enchantIndexes` 都是 `final`（第 5–6 行）。构造时 `List.copyOf`（第 11 行），访问器原样返回（第 14–19 行）。没有 setter，也没有 `equals` / `hashCode`，所以相等是对象身份。下标列表不可改，元素是 `Integer`。

`CompiledReleaseStep.members` 是 `final` 字段，但构造时直接保存传入的 `ArrayList`，没有 `List.copyOf`（`D:\alianximod\src\main\java\com\bslx\XueXiMod\network\CompiledReleaseStep.java` 第 21、38 行）。`memberInputs` 才复制（第 46 行）。`compile` 第 68–70 行和 `actionConditional` 第 136–137 行放入的，是 `segments.get` 得到的同一 `ChainSegment`。`releasedSegments` 也是 `released.add(candidate)`，`candidate` 来自原列表（`SkillChainPlan.java` 第 764–769 行）。

登记之后的引用点只有读取：`CastExecutionSession` 第 99、245、274 行。没有找到对 `plan.members` 的 `add`、`remove` 或 `clear`。`memberShare` 用 `segments.indexOf(member)`（`CompiledReleaseStep.java` 第 201 行）；`SkillChainPlan` 第 153、322、349、400、414、470 行和 `CastExecutionSession` 第 284 行同样。身份对不上时，`indexOf` 得到 -1。

## 3. 接口读取与回调

`ReleaseDefinition` 的数值、枚举、布尔和 `String` 方法返回配置。`guard()`、`barrier()`、`domain()` 返回存放的定义引用。`conditionalEffect()` 返回 `ConditionalEffect`，其 `attempt` 接收 `ServerPlayer`（`ConditionalEffect.java` 第 21 行）。`selfStatus()` 返回 `SelfStatusCast`；本仓的 `SelfStatusDefinition.selfStatus` 返回 `this`，`action()` 返回构造时的枚举（`SelfStatusDefinition.java` 第 100–106 行）。

本仓三处登记实现的 getter 都不读世界、玩家或时钟：`WaterReleaseDefinition` 只读字段；`ReleaseDefinitions` 的匿名类只返回构造参数；`SelfStatusDefinition` 只返回 `final` 字段或常量。`EnchantmentDefinitions` 的匿名类里，`id` 与 `element` 返回闭包参数；`createAccumulator` 和 `fold` 每次新建累加器；`magicDamage` 只用闭包 id 和 `ElementReaction`（第 18–54 行）。没有读世界、玩家或时钟。

未打开护体、屏障、领域定义对象内部，也未打开 `ConditionalEffect.attempt` 的实现。第三方通过 `RegisterXueXiContentEvent` 登记的实现不在本次范围内。

本次只读，工作区无改动。
