我先读这份任务说明，确认授权范围后再按它执行。任务是只读采集，范围限定在状态、注册、玩家生命周期和测试接点。我按这些路径定位，不改文件、不编译。只读采集完成。未改文件，未编译，未启动游戏，不计正式复审。`src` 内没有 `fire_seed` / `furnace_core` 实现。

## 1. 已有接点

**实体持久整数。** `AttributeStateData`（`status/AttributeStateData.java`）用 `LivingEntity.getPersistentData()` 的复合标签 `xueximod_attribute_states`：`read` 13–18 行，`write` 21–34 行，数值不大于 0 时删键。`AttributeStateDefinition` 8–18 行保存标识和上限。`AttributeStateService.read/write` 18–37 行把值夹到 `0..upperBound`。唯一实例是 `WetnessState.DEFINITION = ("wetness", 100)`，`WetnessState.java` 第 10 行。

**资源查询。** `ResourceStatuses`（`statusquery/ResourceStatuses.java`）内建只登记 `xueximod:wetness`，23–31 行；`register` 33–46 行拒绝重复；`find` 48–56 行。`WetnessStatus.read` 87–92 行只读数值、档位、剩余时间。`ResourceView` 是 `(identifier, value, tier, durationTicks)`。`StatusQuery.resourceAtLeast` 28–36 行，`resourceMatches` 76–88 行；未知标识不命中。药水侧：`effectActive` 38–43 行，`effectCounts` 100–117 行（无限时长算存在），`activeEffects` 119–138 行产出 `EffectView(identifier, present, amplifier, durationTicks)`。

**MobEffect 注册。** `registry/ModMobEffects.java`：`DeferredRegister` 13–14 行，只注册 `wetness` → `AttributeStateMobEffect`，16–17 行。`XueXiMod.registerContent` 第 90 行调用 `ModMobEffects.register`。语言键只有 `effect.xueximod.wetness`（`zh_cn.json` 第 59 行）。

**效果壳与修饰。** `AttributeStateMobEffect` 是 `HARMFUL`。`addAttributeModifiers` 29–48 行只在定义等于潮湿时，给 `Attributes.MOVEMENT_SPEED` 加固定 UUID 的 `MULTIPLY_TOTAL` 永久修饰。`removeAttributeModifiers` 51–61 行：效果已不在身上时，把该定义的持久整数写成 0。`PrisonLivingMixin` 60–68 行在 `tickEffects` 前后调用 `enterEffectPass` / `leaveEffectPass`；`AttributeStateService.replace` 89–104 行在药水迭代中推迟 `addEffect`。`replace` 里 `durationTicks <= 0` 会清掉状态，现有写入路径没有无限时长。

**登录、登出、死亡、跨维度。** 状态包只订 `EntityJoinLevelEvent`：`AttributeStateEvents.onEntityJoin` 14–19 行，服务端生物进世界时只 `WetnessState.reconcile`。玩家生命周期在 `ModEvents.ServerEvents`：登录 53–55、Clone 58–60、重生 63–65、登出 84–89，都进 `PlayerConnectionHandler`。Clone 35–41 行只复制 `xueximod_bound_skills`、`xueximod_shu_data`、`xueximod_spell_storage`，不含 `xueximod_attribute_states`。同文件的登出 56–61 行只保存术数据。`ReleaseBaseEvents` 的死亡 151–156、登出 158–163、换维 165–170 只结束施法。`HostCastEvents`、`PrisonLifecycle` 同样不读写属性状态。

**牛奶与治疗。** 没有牛奶物品或 `removeAllEffects` 的专用监听。牛奶走原版清药水后，会进上面的 `removeAttributeModifiers`，从而把效果对应的持久整数清零。`PrisonLivingMixin` 70–73 行的 `onEffectRemoved` 只调 `PrisonRuntime.onWetRemoved`。治疗没有 `LivingHealEvent`。直接调用是 `HitPayloads.apply` 61 行、`applyCaster` 84 行，以及 `FlowSuccessPayload` 105 行的 `LivingEntity.heal`。`FoodItem` 第 17 行附加原版 `MobEffects.HEAL`。这些路径都不改属性状态。

## 2. fire_seed / furnace_core

Java、资源和语言里都没有这两项。进行中记录把它们标成仅设计、未注册：`docs/开发记录/制作表资源状态通用化-20261028/migration-spec.json` 中字典键 `fire_seed`、`furnace_core`，说明为设计标识且真实存储为空；同目录 `migration-author-final.md`、`docs/术式制作表-资源状态字段说明.md` 相同。本批 `docs/开发记录/火属性二级底座-20261028/记录.md` 写明只做底层、不做术式。没有对应测试。

## 3. 可复用 API 与抗性钩子

火种（实体整数 0–100）可直接调用：`AttributeStateDefinition(id, 100)`、`AttributeStateData.read/write`、`AttributeStateService.read/write`，以及 `ResourceStatus` + `ResourceStatuses.register` + `ResourceView` + `StatusQuery.resourceAtLeast`。`WetnessState.grant` 会刷新 1000 tick 并按四档换 amplifier，不是通用整数写入。

炉心（玩家状态，amplifier 0/1）的等级口是 `MobEffectInstance` 的 amplifier，读取口是 `StatusQuery.effectActive` 和 `EffectView.amplifier`。玩家侧已能跨 Clone 保留的自定义数据，只有 `PlayerConnectionHandler` 复制的三把键；`PlayerShuData` 的水势是 `xueximod_shu_data` 里的 float，上限 100（`setWaterMomentum` 173–178 行）。状态包里没有爬坡字段，也没有无限时长的写入函数。

属性修饰只有潮湿移速。剑实体另读主手 `Attributes.ATTACK_DAMAGE` 的物品修饰，与状态无关。没有 `ARMOR` / `DAMAGE_RESISTANCE` 修饰，也没有 `getDamageAfterMagicAbsorb` 的 mixin。`LivingHurtEvent` 接点是 `ReleaseBaseEvents.onHurt` 44–93 行：箭矢先乘屏障保留比例，玩家再按场地或护体卸力减 `event.setAmount`。这改的是事件里已经过原版护甲和抗性之后的数额，不单拆抗性减伤。`LivingDamageEvent` 116–149 行只给水势记账。`data/minecraft/tags/damage_type/bypasses_resistance.json` 只追加 `xueximod:water_prison_pressure`。`TechniqueHits.hurt` 47、53 行使用 `damageSources().playerAttack`，不走该标签。

## 4. 测试

`src/test/java` 共 52 个类，没有 JUnit。`build.gradle` 没有 `test {}`、没有 JUnit 依赖；测试源集靠 Java 插件的 `compileTestJava`。`gameTestServer` 只是 Forge GameTest 运行配置。现有检查是 `public static void main`，类名用 `Check`、`Checks`、`Regression`，包路径镜像主代码，自行计断言，失败 `System.exit(1)`，并打印未运行项。状态相关是 `status/WetnessTierCheck`、`status/WetnessFlightCheck`、`FlightExemptCheck`、`statusquery/StatusQueryCheck`、`statusquery/RelationDecisionCheck`。离线编译测试源集用 `.\gradlew.bat compileTestJava --offline --no-daemon`；单个 `Check` 要在编译后的 classpath 上跑它的 `main`。`gradlew test` 不会执行这些 `main`。本次没有跑编译或测试。

## 5. 归属（只列依赖，不审架构）

可独占的新目录目前不存在：`src/main/java/com/bslx/XueXiMod/status/fire/`，以及对应的 `src/test/java/com/bslx/XueXiMod/status/fire/`。现有 `Wetness*` 不必为了火种改写。

必须由整合者单写的已有文件：`registry/ModMobEffects.java`；`statusquery/ResourceStatuses.java`；若进世界时要对齐新状态，则 `status/AttributeStateEvents.java`；若玩家炉心要跨 Clone 保留，则 `event/PlayerConnectionHandler.java`；若新增 mixin，则 `src/main/resources/xueximod.mixins.json`；效果文案则 `assets/xueximod/lang/zh_cn.json` 与 `en_us.json`。`XueXiMod.registerContent` 已接上现有效果注册表；只有再增加第二个 `DeferredRegister` 才要改 `XueXiMod.java`。若要改夹值、清零或修饰语义，`AttributeStateData.java`、`AttributeStateService.java`、`AttributeStateMobEffect.java` 也各只能一个作者写。
