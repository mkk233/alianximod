我先读这份任务说明，按它写明的授权范围执行，再给出完整终稿。任务是只读事实采集。我先定位配置、术式注册和文档入口，再按路径核对字段与数据流。只读事实清单。未改文件，未编译，未跑游戏，未写制作表。下面按现行源码记录配置生命周期、术式注册和可分文件的依赖，不裁决接入方案。

## 1. Forge 配置

只有 `COMMON`。构造函数里先挂主规格，再挂地形规格。没有 `CLIENT`、`SERVER`，也没有配置网络包。仓库里没有已生成的 toml，本会话没有观察到磁盘文件或真实加载时刻。

`XueXiMod` 构造顺序：`registerContent`（物品、方块、方块实体、菜单、创造栏、释放实体、陨石残骸、载体、药水）→ 登记 `commonSetup` → `MinecraftForge.EVENT_BUS.register` → 创造栏监听 → 两次 `registerConfig`。

```75:78:src/main/java/com/bslx/XueXiMod/XueXiMod.java
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        context.registerConfig(ModConfig.Type.COMMON,
                com.bslx.XueXiMod.terrain.BatchTerrainBreakSwitch.SPEC,
                com.bslx.XueXiMod.terrain.BatchTerrainBreakSwitch.CONFIG_FILE);
```

`mod_id` 是 `xueximod`（`gradle.properties` 45 行，`XueXiMod.MOD_ID` 59 行）。第一份没写文件名，Forge 两参数重载的默认名是 `xueximod-common.toml`，这个字符串不在本仓库里。第二份文件名是常量 `xueximod-batch-terrain.toml`（`BatchTerrainBreakSwitch` 16 行）。

`Config` 在类初始化时 `BUILDER.build()` 得到 `SPEC`（53 行），并调用 `WetnessFlightExempt.ensureDefault()`（55–57 行）。`@Mod.EventBusSubscriber(bus = MOD)`。`onLoad(ModConfigEvent)`（79–94 行）只处理 `event.getConfig().getSpec() == SPEC`。参数是父类型，因此 Forge 的 Loading 与 Reloading 都会进同一方法；代码不区分二者，也不在重载时重登记术式。刷新的是静态缓存：

| 键 | 类型与默认 | 缓存 |
| --- | --- | --- |
| `logDirtBlock` | boolean，`true` | `Config.logDirtBlock` |
| `magicNumber` | int，`42`，范围 `0..Integer.MAX_VALUE` | `Config.magicNumber` |
| `magicNumberIntroduction` | String，`"The magic number is... "` | `Config.magicNumberIntroduction` |
| `items` | 可空列表，默认 `minecraft:iron_ingot` | `Config.items`，经 `ForgeRegistries.ITEMS` 转成 `Set<Item>` |
| `wetness.flightExemptEntityIds` | 可空列表，默认 `minecraft:ender_dragon` | `WetnessFlightExempt.replace` |

`items` 的校验是 `ForgeRegistries.ITEMS.containsKey`（64–66 行）。飞行名单的校验是 `WetnessFlightExempt.validSyntax`：必须是恰好一个冒号、合法且与 `ResourceLocation.toString()` 全等的注册名；加载时不查注册表（`WetnessFlightExempt` 25–27、50–65 行）。`replace` 丢掉语法非法项；空列表写成空集合，不退回末影龙（29–40 行）。类字段初始值已是 `minecraft:ender_dragon`（13–14 行）。`ensureDefault` 只在字段为 null 时补这个默认，当前初始化路径下不会改已有集合（19–22 行）。

玩法读取：`WetnessFlight` 67 行读豁免。`logDirtBlock`、`magicNumber`、`magicNumberIntroduction`、`items` 只在 `Config.onLoad` 写入，其它 Java 不读。类注释 25 行写的伤害倍率、冷却倍率键不存在。`docs/底座功能点.md` 275–277 行与此一致。

`BatchTerrainBreakSwitch`：键 `breakExistingBlocks`，boolean，默认 `true`（18–20 行）。静态缓存初始也是 `true`（22 行）。`onLoad` 同样按 spec 身份过滤后写入（36–40 行）。`allowsBreakingExisting` 只被 `TerrainMutationService` 85 行读取；注释写明只给崩地，陨石安放、生成水、分海不读。规格没挂上时保持 `true`（12–13 行）。`applyConfigured`（31–33 行）没有调用方。

没有配置键别名、版本迁移或旧键改名。已有文件里的键一旦被用户改过，代码不会在加载时把飞行空名单填回默认。

## 2. 术式与释放定义如何登记

`commonSetup`（`XueXiMod` 99–106 行）顺序：`ModNetwork.register` → `ActionBootstrap.load` → `ModTechniques.registerAll` → `BatchCarrierBootstrap.install` → 在模组总线上 `post(RegisterXueXiContentEvent)`。日志用 `TechniqueManager.getAll().size()`，源码里没有写死这个数。

`ModTechniques.registerAll`（283–384 行）顺序：

1. 类初始化已经把全部 `public static final Technique` 构造完（47–277 行），发生在方法体之前。
2. `WaterCurveFunctions.register()`。
3. `TechniqueManager.register`，56 次。幻剑、剑来、万剑归宗在字段区靠前，登记调用放在最后（337–339 行）。
4. `TechniqueEffectRegistry.register`。部分规则是空 lambda。缠枝、潮印、沉锋、锐锋印以及本批六个附魔不在这份效果表里。
5. `EnchantmentDefinitions.registerAll()`。
6. `ReleaseDefinitions.registerAll()`。中间插入 `SelfStatusDefinition.registerAll()`（40 行），其后是燎原到灼地，再是 `ListedRelease` 的 13 个工厂（45–58 行）。
7. `WaterTechniques.register()`，另登记 6 个术式。

四个表都是 `LinkedHashMap`，内置 `put` 同 id 后写覆盖：`TechniqueManager` 13–15 行、`ReleaseRegistry` 16–18 行、`TechniqueEffectRegistry` 12–14 行、`EnchantmentRegistry` 13–15 行。前三个查询走 `TechniqueIds.canonicalId`；附魔表不走。唯一别名是 `water_open_ring` → `water_blade`（`TechniqueIds` 10–14 行）。每次 register 都 `ChainStructureEpoch.changed()`。`ReleaseRegistry.HOST_GROUP` 只有 `"space_cut"`（12、30–34 行）。缺月不走这个集合，它在 `WaterReleaseDefinition.submitsHostGroup` 返回自己的字段（313–314 行），`swordForm()` 把它设为 true（68–78 行）。

`Technique` 字段全是 `final`，只在构造器赋值（17–60 行）。签名是 `(id, name, type, baseDamage, baseCooldown, levelCap, description[, complexity, attribute, projectileSpeed, enchantment])`。缺省：复杂度 `0`，属性 `NONE`，弹速 `1.0f`，附魔 `false`。本文件里看到的 `levelCap` 都是 `20`。这些字面量在 `ModTechniques` 类初始化时写入，不读 `Config`。配置重载不会重建这些对象。

魔力在调用时现算（97–120 行），不在构造时封进字段：

- 附魔先看 `ResourceEnchantCatalog.explicitManaPrice`；负数则 `max(1, magicDamage(无属性对无属性))`。
- 否则释放定义上条件效果且 `explicitManaCost >= 0` 用该值。
- 否则有领域则 `max(0, domain.initialCost())`。
- 否则 `ReleaseKind.RULE` 为 0。
- 否则 `explicitManaCost >= 0` 用该值。
- 否则 `max(0, ceil(baseDamage * 2))`。

装配消耗（86–94 行）：`explicitAssemblyCost >= 0` 用该值；否则附魔 2、动作 0、`baseDamage > 0` 为 1、其余 0。

`ReleaseDefinitions.define` 返回匿名 `ReleaseDefinition`，getter 返回构造参数（148–340 行）。参数是基本类型或当时传入的引用，登记之后这条匿名类没有 setter。`supportsStructuralPlanCache` 固定 true。短重载把能量倍率填成 `1f`，四向缩放填成 true，显式魔力和装配填成 `-1`。

同一 id 的伤害/冷却在 `Technique`，寿命/速度/厚度/显式费用在释放定义。`Technique.projectileSpeed` 的构造默认 `1.0f` 会真实存进去。已核对的分叉：奥术飞弹术式弹速 `1.0f`、释放 `speed 1.5f`；幻剑术式 `1.0f`、释放 `2.5f`；热光束术式 `1.0f`、释放 `0f`。火球两边都是 `1.2f`，爆燃 `0.6f`，金针 `1.5f`，陨岩 `0.8f`。

`ReleaseDefinitions.registerAll` 里非默认数字（寿命、速度、厚度；未写的显式费用是 `-1`）：

| id | 寿命 | 速度 | 厚度 | 其它 |
| --- | --- | --- | --- | --- |
| `movement_dash` | 10 | 2 | 0 | `ChainRole.ACTION` |
| 三道剑斩 | 4 | 0 | 0.5 | 队列分道视觉 |
| `magic_arcane_missile` | 40 | 1.5 | 0.1 | 弹道可变 |
| `test_sustained` | 20 | 0.4 | 0.1 | |
| `heat_beam` | 40 | 0 | 1 | 弹道可变 |
| `fire_orb` | 40 | 1.2 | 0.4 | |
| `fire_burst` | 40 | 0.6 | 0.8 | |
| `water_yielding_ribbon` | 32 | 0 | 0.55 | 队列分道 |
| `water_weeping_arc` | `WeepingSettings.LIFETIME` 40 | 0 | 0.35 | 潮湿 3，显式魔力 10 |
| `space_cut` | 10 | 0 | 0 | |
| `parasite_seed` | 160 | 0 | 0.5 | |
| `tide_sever` | 6 | 0 | 0.4 | |
| `stone_hammer` | 10 | 0 | 1 | |
| `metal_needle` | 15 | 1.5 | 0.2 | |
| `phantom_sword` | 80 | 2.5 | 0.5 | |
| `sword_come` | 20 | 0 | 0.5 | 操纵剑 |
| `sword_return_all` | 40 | 0 | 0.5 | 操纵剑 |
| `fire_liaoyuan` | 200 | 0 | 0 | 魔力 60，装配 3 |
| `fire_liaoli` | 40 | 0 | 0.6 | 魔力 40，装配 3，要手持剑 |
| `fire_fentian` | 40 | 0 | 0 | 魔力 100，装配 3 |
| `fire_scorch_field` | 100 | 0 | 0 | 费用 -1，`explicitDomainHost` |
| `rule_dual_release` | 1 | 0 | 0 | `MulticastSpec.fixed(2)`，`pullAfterA` 1 |
| `rule_triple_cast` | 1 | 0 | 0 | `ReplicationSpec.of(3)` |
| `rule_gather_energy` | 1 | 0 | 0 | `chainEnergyScale` 2 |

索敌、落点、垂落、追踪、风暴、交错的释放寿命是 1，速度和厚度是 0。风暴的弹道是 `Trajectory.VORTEX`，角色仍是 `TRAJECTORY_DROP`（22 行）。

`WeepingSettings`（8–18 行）是 `public static final` 基本类型：底伤 `2.0f`，潮湿 `3`，命中间隔 `10`，寿命 `40`，填充 `8`，排空起点 `32`，下落上限 `6.0`，帘宽 `0.5`，保留魔力 `10`。这些常量会被编译进 `ModTechniques` 和 `ReleaseDefinitions` 的常量池。`drainSpan()` 仍是方法。

`ListedRelease` 字段在工厂时写成 `final`（71–127 行）。陨岩：寿命 100，速度 0.8，厚度 1，出生半径 0.5/0.5，装配 `-1`，最早接触。热波：寿命 12，速度 0，厚度 5，半径 5/1.5。火苗：寿命 30，速度 0.8，厚度 0.25，半径 0.125，产种基数 2。四条规则：寿命 1，速度 0，装配 1，`declaresBatchQuote`。六个附魔：`ReleaseKind.RULE`，`ChainRole.NOT_APPLICABLE`，寿命 1，装配 `-1`，`batchQuote`。幽冥另持 `DomainDefinition.gloom()`。`ListedRelease` 不覆盖 `motionProfile()`，接口默认是 `MotionProfile.NONE`（`ReleaseDefinition` 442–444 行）。`MotionProfile.resolve`（82–92 行）在 `NONE` 且弹道为抛物线或喷涌时改用首版：抛物线初速 0.8、仰角 15、重力 0.08、最长 100 刻；喷涌初速 1、半角 25、重力 0.08、水平 32、最长 100 刻。`ParabolaMotion` 在种类不是抛物线时同样落到 `approvedParabola()`（13–17 行）。

炉心三式（`SelfStatusDefinition` 23–26、85–91 行）：`fire_heat_up` 与 `fire_burn` 显式魔力 20，`fire_cool` 为 0；装配都是 1；寿命 1；角色 `ACTION`；`refusesMixedChain`。字段 `final`。术式侧冷却分别是 500、500、0。

水系在 `WaterTechniques.register` 现场 `new`，不是 `ModTechniques` 静态字段。缺月：伤害 7，冷却 14，寿命 10，速度 1.3，厚度 0.35，`lengthScale` 3.2，`spanScale` 0.7，能量 1.35，潮湿 6。水幕冷却 40，屏障 `curtain()`：3.2×2.8×0.25，前向 2，寿命 80，生成费 8，维护 2/20 刻，衰减 0.5。雾雨、深海重压冷却 80、复杂度 10；领域初始费 40、寿命 200。雾雨盒子 7×7×8，原生周期 `("mist_rain", 10, 2, 10, 20, 2, 20)`。深海 7×7×3，开放边界，`("deep_pressure", 10, 4, 10, 20, 0, 20, 0, 20, 10, 10, 2f)`。水环冷却 40，`personalRing()`：寿命 200，生成 8，维护 2/20，减伤 0.20，半径 0.9，带宽 0.12，身高 0.55。水牢：术式伤害 0、冷却 400；定义寿命 1、显式魔力 40；`WaterPrisonEffect.SUCCESS_MANA` 也是 40。领域和水环的 `WaterReleaseDefinition.explicitCost` 被工厂写成 0（99、109、119 行），魔力改走 `domain.initialCost()` 或护体/屏障自己的字段。`WaterReleaseDefinition` 的 id、弹道、寿命、速度、厚度是 `final`；能量、费用、潮湿、场地等是可变字段，fluent 写完会 `ChainStructureEpoch.changed()`（135–138、250–254 行）。登记后仍是同一个对象。

旧五附魔（`EnchantmentDefinitions` 13–17、30–67 行）每次 `createAccumulator` 用字面量 `(effectId, 0, 0.5f, 5f, 1f, 1f, 1f, 1f)`。`tide_mark` 的 `magicDamage` 固定 2；其余是 `ceil(2 * damageMultiplier)`。六个资源附魔的切片在 `registerAll` 时构造，字段 `final`。`ResourceEnchantNumbers`：魔力 2，直接伤害 0，种火基数 10，引燃段伤 2、间隔 10、平直 40、档位 10、段需求 10，幽冥基数 5、周期 20，引爆系数 0.2。`ResourceEnchantDefinition.magicDamage` 返回 `DIRECT_DAMAGE`（0）。id 与 `ResourceEffectKeys` 相同：`fire_flame_trail`、`fire_seed_sow`、`fire_kindle`、`fire_detonate`、`fire_nether_gloom`、`earth_break_ground`。

`ChainRole` 现有值：`ATTACK`、`ACTION`、`SEEK_SPACE`、`SEEK_ENTITY`、`TRAJECTORY_TRACKING`、`TRAJECTORY_DROP`、`TRAJECTORY_SCATTER`、`RULE_TRIPLE`、`RULE_DUAL`、`RULE_DOMAIN_BIND`、`SEEK_SURFACE`、`TRAJECTORY_UPWELL`、`RULE_STAGGER`、`RULE_HIT_CONTINUE`、`NOT_APPLICABLE`。后四个 `batchLifecycle()` 为真（33–38 行）。枚举声明顺序不是结算顺序。

## 3. 扩展入口与键约束

对外入口是 `RegisterXueXiContentEvent`（`api/RegisterXueXiContentEvent.java`）。它在内置 `registerAll` 之后投递。`registerCast` 要求术式、效果、释放定义三者非空且 id 一致，任一表已有该 id 就拒绝。`registerEnchantment` 还要求累加器 `effectId` 属于 `fire`、`wood`、`water`、`earth`、`metal`（24 行）。没有卸载。客户端和专用服务器各自登记；事件类注释要求两边同一批 id（19–20 行）。

内置扩展面是：`Technique` 构造器、`ReleaseDefinition` 的 default、`ReleaseDefinitions.define`、`ListedRelease` 工厂、`SelfStatusDefinition`、`WaterReleaseDefinition` fluent、`EnchantmentDefinition`、`ResourceEnchantSlice`、`MotionProfile.parabola` / `upwell`、`MulticastSpec`、`ReplicationSpec`。未知实现的 `supportsStructuralPlanCache` 默认 false（`ReleaseDefinition` 429–430 行）。`docs/底座功能点.md` 451 行写明：缓存不保存费用、伤害、冷却；这些数仍在原来的时刻读。

已有配置键若要保持旧文件可载入，路径就是上一节那 6 个。代码里没有把它们映射到术式 id，也没有全局伤害或魔力倍率键。飞行空名单和「示例键不被玩法读取」是当前兼容行为。术式 id 侧只有 `water_open_ring` 这一个别名，而且附魔查询不认它。

## 4. 新术式文档入口

现行入口：

- `docs/统一开发与术式批量制作流程.md` 79 行、185 行：在 `ReleaseDefinitions` 登记 `ChainRole`，走同一套收集和结算；共享注册表由该文件的唯一作者改。
- `docs/底座功能点.md` 559–614 行源码入口；423–447 行对外登记；275–277 行现有配置不是伤害/冷却倍率；237 行起魔力、法杖、水势。
- `docs/交接索引.md` 9 行：和更早交接冲突时以 `底座功能点.md` 为准。
- `AGENTS.md` 第 5 节同一登记口径；第 7 节指向上述流程和 `docs/术式制作表.xlsx` 的「待制作」页。
- `docs/多重释放底座与扩展指南.md`：多重只加 `multicastSpec()`，复制加 `replication()`，不新增 `ChainRole`。
- 缓存契约在 `docs/术式链计划缓存说明.md`，由底座功能点 451 行链出。
- 改 `src` 下 Java 后的局部命名门禁是 `.cursor/rules/name-gate.mdc`，由 `tools/check-name-gate.ps1` 执行。

`docs/交接-2026-10-16-对外注册口.md` 写「代码还没改」。现行代码已经有 `RegisterXueXiContentEvent` 并在 `commonSetup` 投递。这份交接不能当现状。

## 5. 分文件依赖（事实，不是方案）

今天配置和术式数字不相连。`Config.java` 与 `WetnessFlightExempt.java`、`WetnessFlight.java` 是一条线。`BatchTerrainBreakSwitch.java` 与 `TerrainMutationService.java` 是另一条线。两条都只被 `XueXiMod.java` 75–78 行挂上。

术式数字按 id 拆在多份文件里，登记调用却收在少数顺序文件里：

- 公共顺序文件：`XueXiMod.java` 99–106、`ModTechniques.java` 的静态表和 `registerAll`、`ReleaseDefinitions.java` 的 `registerAll` 与 `define`、`Technique.java` 的费用公式、四个 Registry。
- 同一攻击 id 通常同时有 `ModTechniques` 的伤害、冷却、复杂度、弹速，以及 `ReleaseDefinitions` 匿名类的寿命、速度、厚度、显式费用。只改一边，另一边保持原字面量。
- 垂丝再加 `WeepingSettings.java`。常量会被编译进前两个类。
- 本批 13 个 id 同时在 `ModTechniques` 和 `ListedRelease.java`。幽冥再加 `DomainDefinition.gloom`。六个附魔再加 `EnchantmentDefinitions.java`、`ResourceEnchantSlice.java`、`ResourceEnchantNumbers.java`、`ResourceEffectKeys.java`。
- 炉心三式同时在 `ModTechniques` 和 `SelfStatusDefinition.java`。
- 缺月、水幕、雾雨、水环、深海、水牢集中在 `WaterTechniques.java`，数值再进 `WaterReleaseDefinition.java`、`DomainDefinition.java`、`BarrierDefinition.java`、`GuardDefinition.java`、`WaterPrisonEffect.java`。
- 旧五附魔在 `ModTechniques` 与 `EnchantmentDefinitions`，没有对应 `ReleaseDefinition`。
- 抛物线/喷涌首版在 `MotionProfile.java`，由 `resolve` 和 `ParabolaMotion` / `UpwellMotion` 在使用时读取，不写进 `ListedRelease` 字段。

效果类、`SkillUsePacket`、`SkillSegmentMath` 里还有另一层结算数字。本单没有把那些调用点逐个数完。

证据缺口：未看到生成后的 toml；未观察 Forge 对非法 `items` 条目是拒载还是整表回默认；未反汇编确认垂丝常量内联；未统计运行时 `getAll().size()`。按源码登记调用是 56 加 6。
