# 平衡公共 API 合同（A1）

包名 `com.bslx.XueXiMod.shu.balance`。这是已批准架构的实现接口，不新增审查轮次。

A1 提供声明、封存、不可变快照和读取。它不读取 JSON，不写 `defaultconfigs` 或世界目录，不改 `Technique`、网络包、连锁结算和对外事件。那些属于 A2。B/C 只调用下面的声明和读取入口，不再各写配置读取器。

类加载不读取配置，也没有把有效配置放进 `static final`。代码默认快照在封存之后按需生成；世界配置只存在于 `install` 进去的那一份快照。

## 1. 调用顺序

1. 内容登记期间，B/C 调用 `BalanceRegistry.declareSpecial`。此时只保存声明，不求原始默认计算器。
2. 内置和外部术式都登记完之后，A2 对每个正式编号调用 `fillStandard`。
3. A2 调用 `seal`，传入全部已登记术式编号。封存时每个计算器求值一次并冻结。
4. 服务端加载成功后 `BalanceSnapshot.resolve`，再 `install(SERVER)` 并在服务端刻线程 `bind(SERVER)`。文件不可用时改为 `install(SERVER, BalanceSnapshot.defaults())`，不改写原文件。
5. 客户端收到已接受的覆盖值后，在同样的声明上 `resolve`，`install(CLIENT)`，并在客户端刻线程 `bind(CLIENT)`。
6. 玩法读取用 `BalanceService`。未绑定线程在两侧都已安装时必须使用带 `BalanceSide` 的重载。

不要在 `Technique` 或 `ModTechniques` 的静态初始化里调用 `seal`、`resolve`、`install` 或任何读取方法。声明本身可以发生在登记方法里。禁止把 `current()` 或 `view()` 的返回值写进 `static final`。

## 2. 类型与签名

下列都是 `public`。读取重载里，带 `BalanceSide` 的只看该侧；不带的使用当前线程绑定。

### 目录

```text
String BalanceRegistry.canonicalize(String techniqueId)
BalanceRegistry BalanceRegistry.current()
void BalanceRegistry.declareSpecial(String techniqueId, BalanceParameter parameter)
void BalanceRegistry.declareSpecial(TechniqueBalanceSpec spec)
void BalanceRegistry.acceptSpecial(String techniqueId, BalanceParameter parameter)
void BalanceRegistry.acceptSpecial(TechniqueBalanceSpec spec)
void BalanceRegistry.completeStandard(TechniqueBalanceSpec spec)
void BalanceRegistry.fillStandard(String techniqueId, RawDefault cooldownDefault, RawDefault manaDefault, boolean includeDamageMultiplier)
void BalanceRegistry.seal(Collection<String> registeredTechniqueIds)
boolean BalanceRegistry.isSealed()
int BalanceRegistry.generation()
boolean BalanceRegistry.isDeclared(String techniqueId, String parameterKey)
TechniqueBalanceSpec BalanceRegistry.spec(String techniqueId)
List<TechniqueBalanceSpec> BalanceRegistry.specs()
List<String> BalanceRegistry.declaredTechniqueIds()
boolean BalanceRegistry.hasFrozenDefault(String techniqueId, String parameterKey)
double BalanceRegistry.frozenDefault(String techniqueId, String parameterKey)
String BalanceRegistry.contractText()
```

`manaDefault` 允许为空。为空表示不补标准键 `manaCost`，封存时改由其他魔力单位键满足“至少一笔费用”。`generation()` 只在本进程内比较快照，不能写入同步包。`spec` 未声明时返回 `null`。`frozenDefault` 在未封存，或该键是调用方来源且没有冻结数字时抛出。

`contractText()` 在封存后返回声明文本，含编号、键、种类、单位、边界、默认来源和已冻结数字。服务端与客户端不一致时，不要安装对方快照。

### 声明

```text
TechniqueBalanceSpec.Builder TechniqueBalanceSpec.builder(String techniqueId)
Builder TechniqueBalanceSpec.Builder.parameter(BalanceParameter parameter)
Builder TechniqueBalanceSpec.Builder.damageMultiplier()
Builder TechniqueBalanceSpec.Builder.manaCost(RawDefault rawDefault)
Builder TechniqueBalanceSpec.Builder.manaFee(String parameterKey, RawDefault rawDefault, String description)
Builder TechniqueBalanceSpec.Builder.cooldownTicks(RawDefault rawDefault)
Builder TechniqueBalanceSpec.Builder.numberParameter(String parameterKey, RawDefault rawDefault, BalanceBound bound, BalanceUnit unit, String description)
Builder TechniqueBalanceSpec.Builder.integerParameter(String parameterKey, RawDefault rawDefault, BalanceBound bound, BalanceUnit unit, String description)
TechniqueBalanceSpec TechniqueBalanceSpec.Builder.build()
String TechniqueBalanceSpec.techniqueId()
Map<String, BalanceParameter> TechniqueBalanceSpec.parameters()
BalanceParameter TechniqueBalanceSpec.parameter(String parameterKey)
```

`builder` 会把编号收成 `TechniqueIds.canonicalId`。`water_open_ring` 因此并入 `water_blade`。`parameter` 未找到时返回 `null`。

```text
BalanceParameter BalanceParameter.of(BalanceValueKind kind, String parameterKey, RawDefault rawDefault, BalanceBound bound, BalanceUnit unit, String description)
BalanceParameter BalanceParameter.damageMultiplier()
BalanceParameter BalanceParameter.manaCost(RawDefault rawDefault)
BalanceParameter BalanceParameter.manaCost(String parameterKey, RawDefault rawDefault, String description)
BalanceParameter BalanceParameter.cooldownTicks(RawDefault rawDefault)
```

`damageMultiplier()` 的代码默认值固定为字面量 `1`。配置可以把它改成别的非负倍率，代码默认不能改，避免未配置时改变现有伤害。

```text
RawDefault RawDefault.literal(int amount)
RawDefault RawDefault.literal(double amount)
RawDefault RawDefault.calculator(BalanceDefaultCalculator calculator)
RawDefault RawDefault.callSite()
double BalanceDefaultCalculator.rawDefault(String techniqueId)
```

### 快照与服务

```text
void BalanceSchema.requireSupported(int schemaVersion)
int BalanceSchema.CURRENT_VERSION
```

`CURRENT_VERSION` 为 `1`。版本不符抛出 `BalanceDocumentException`。调用方保留原文件，并安装 `BalanceSnapshot.defaults()`。语法损坏同样不要调用 `resolve`，直接安装默认快照。

```text
BalanceSnapshot BalanceSnapshot.defaults()
BalanceSnapshot BalanceSnapshot.resolve(Map<String, Double> globalOverrides, Map<String, Map<String, Double>> techniqueOverrides, List<BalanceWarning> warnings)
```

两个覆盖参数和 `warnings` 都允许为空。`warnings` 非空时，本快照的警告会追加进去。快照自己的 `warnings()` 是不可变副本。

```text
int schemaVersion()
int registryGeneration()
boolean fromSealedCatalog()
GlobalBalance globals()
List<BalanceWarning> warnings()
Map<String, Double> acceptedGlobalOverrides()
Map<String, Map<String, Double>> acceptedTechniqueOverrides()
Map<String, Map<String, Double>> effectiveValues()
BalanceParameter parameter(String techniqueId, String parameterKey)
boolean overrideApplied(String techniqueId, String parameterKey)
```

`effectiveValues()` 是全局倍率之前的技术数值。调用方来源且还没有覆盖值的键不出现。`parameter` 未声明时返回 `null`。

```text
GlobalBalance GlobalBalance.identity()
GlobalBalance GlobalBalance.checked(double damageMultiplier, double manaMultiplier, double cooldownMultiplier)
double damageMultiplier()
double manaMultiplier()
double cooldownMultiplier()
```

三个全局默认值都是 `1`。

```text
void BalanceService.install(BalanceSide side, BalanceSnapshot snapshot)
void BalanceService.clear(BalanceSide side)
BalanceSnapshot BalanceService.installed(BalanceSide side)
BalanceSnapshot BalanceService.view(BalanceSide side)
void BalanceService.bind(BalanceSide side)
void BalanceService.unbind()
BalanceSide BalanceService.boundSide()
BalanceSnapshot BalanceService.current()
boolean BalanceService.isDeclared(String techniqueId, String parameterKey)
```

`installed` 与 `boundSide` 未设置时返回 `null`。`view` 在该侧尚未安装时返回代码默认快照，不会读取另一侧。`current` 在已绑定线程上等于 `view(boundSide)`。两侧都未安装且线程未绑定时，`current` 返回代码默认快照。任一侧已安装但线程未绑定时，抛出 `BalanceSideException`。

`install` 要求目录已封存，且快照是封存后的同一代声明构建的。封存前构建的快照不能安装。

```text
ConfiguredDamage BalanceService.damageContribution(String techniqueId, double rawAmount)
ConfiguredDamage BalanceService.damageContribution(BalanceSide side, String techniqueId, double rawAmount)
ConfiguredDamage BalanceService.damageContribution(String techniqueId, RawDamage rawDamage)
ConfiguredDamage BalanceService.damageContribution(BalanceSide side, String techniqueId, RawDamage rawDamage)
ConfiguredDamage BalanceService.globalDamageContribution(double rawAmount)
ConfiguredDamage BalanceService.globalDamageContribution(BalanceSide side, double rawAmount)
int BalanceService.manaCost(String techniqueId, String parameterKey, int rawDefault)
int BalanceService.manaCost(BalanceSide side, String techniqueId, String parameterKey, int rawDefault)
int BalanceService.cooldownTicks(String techniqueId, int rawDefault)
int BalanceService.cooldownTicks(BalanceSide side, String techniqueId, int rawDefault)
double BalanceService.number(String techniqueId, String parameterKey)
double BalanceService.number(BalanceSide side, String techniqueId, String parameterKey)
int BalanceService.intValue(String techniqueId, String parameterKey)
int BalanceService.intValue(BalanceSide side, String techniqueId, String parameterKey)
```

```text
RawDamage RawDamage.of(double amount)
double RawDamage.amount()
ConfiguredDamage ConfiguredDamage.retained(float storedAmount)
float ConfiguredDamage.amount()
ConfiguredDamage ConfiguredDamage.plus(ConfiguredDamage other)
ConfiguredDamage ConfiguredDamage.timesHitCount(int hitCount)
```

### 失败类型

均继承 `BalanceException`。

| 类型 | 何时 |
| --- | --- |
| `BalanceDeclarationException` | 声明、封存或原始默认计算器不合法。这是代码错误。 |
| `BalanceConflictException` | 同一术式同一键的两份合同不能合并。含 `techniqueId()` 与 `parameterKey()`。 |
| `BalanceValueException` | 未声明键、原始金额不合法，或缩放结果无法表示。 |
| `BalanceSideException` | 已安装分侧快照，但当前线程没有绑定。 |
| `BalanceDocumentException` | `schemaVersion` 不是 `1`。保留原文件，改用代码默认快照。 |

玩家配置里的单个坏字段不抛这些异常，而是写入 `BalanceWarning` 并回退该字段。

`BalanceWarning.Kind`：`UNKNOWN_TECHNIQUE`、`UNKNOWN_PARAMETER`、`INVALID_VALUE`、`ALIAS_IGNORED`、`ALIAS_CANONICALIZED`。全局警告的术式编号为空字符串。

## 3. 标准键

术式对象里的标准键：

| 键 | 种类 | 单位 | 代码默认 | 读取 |
| --- | --- | --- | --- | --- |
| `damageMultiplier` | `NUMBER` | `MULTIPLIER` | 字面量 `1` | 只由 `damageContribution` 与全局伤害倍率各乘一次 |
| `manaCost` | `INTEGER` | `MANA` | 调用方传入的原始费用，或计算器 | `manaCost(id, "manaCost", rawDefault)` |
| `cooldownTicks` | `INTEGER` | `TICKS` | 调用方传入的原始冷却，或计算器 | `cooldownTicks(id, rawDefault)` |

全局对象里的键，默认都是 `1`，范围见倍率边界：

| 键 | 使用位置 |
| --- | --- |
| `damageMultiplier` | 只在 `damageContribution` 内乘一次 |
| `manaMultiplier` | 只在 `manaCost` 内乘一次 |
| `cooldownMultiplier` | 只在 `cooldownTicks` 内乘一次 |

术式可以声明 `damageMultiplier`。术式不能声明 `manaMultiplier` 或 `cooldownMultiplier`，这两项只属于全局。也不能声明 `baseDamage`、`projectileSpeed`、`chainRole`、`replicationCount`、`multicastCount`、`copyCount`、`entityCount`、`settlementOrder`、`dedupe`、`hitDedupe`、`assemblyCost`、`unlockLevel`、`complexity`、`levelCap`、`geometryEnergy`、`chainEnergyScale`。

`number` / `intValue` 遇到魔力单位或 `cooldownTicks` 会抛出，避免漏乘全局倍率。`number(id, "damageMultiplier")` 只返回术式自己的倍率，不含全局倍率，不能再乘到 `damageContribution` 的结果上。

常量都在 `BalanceKeys`。标准三项同时由 `BalanceKeys.isStandardTechniqueKey` 识别。

## 4. 专项键命名

专项键是小驼峰，首字母小写，只含字母和数字，长度 3 到 48。不要用下划线、中文或制作表编号。常量只规定拼写；没有 `declareSpecial` 的键不会进入目录，读取会抛出。

| 常量 | 字符串 | 建议单位与边界 |
| --- | --- | --- |
| `LIFETIME_TICKS` | `lifetimeTicks` | `TICKS`，`positiveTicks()` |
| `MOTION_SPEED` | `motionSpeed` | `BLOCKS_PER_TICK`，速度允许 0 时用 `nonNegativeSpeed()` |
| `HIT_SIZE` | `hitSize` | `BLOCKS`，`positiveSize()` 或允许 0 的 `nonNegativeSize()` |
| `THICKNESS` | `thickness` | 同上 |
| `BEAM_LENGTH` | `beamLength` | `BLOCKS`，`positiveSize()` |
| `HIT_INTERVAL_TICKS` | `hitIntervalTicks` | `TICKS`，`positiveTicks()` |
| `SPAWN_MANA_COST` | `spawnManaCost` | `MANA`，用 `manaFee` |
| `UPKEEP_MANA_COST` | `upkeepManaCost` | `MANA`，用 `manaFee` |
| `UPKEEP_INTERVAL_TICKS` | `upkeepIntervalTicks` | `TICKS`，`positiveTicks()` |
| `RADIUS` | `radius` | `BLOCKS`，`positiveSize()` |
| `EFFECT_RADIUS` | `effectRadius` | `BLOCKS`，`positiveSize()` |
| `DAMAGE_REDUCTION` | `damageReduction` | `RATIO`，`damageReduction()`，上端与现有 `0.6f` 封顶一致 |
| `SPEED_DECAY` | `speedDecay` | `RATIO`，`unitRatio()` |
| `SEARCH_RANGE` | `searchRange` | `BLOCKS`，`positiveSize()` |
| `SCAN_RANGE` | `scanRange` | `BLOCKS`，`positiveSize()` |
| `SPREAD_RANGE` | `spreadRange` | `BLOCKS`，`nonNegativeSize()` |
| `PRISON_DURATION_TICKS` | `prisonDurationTicks` | `TICKS`，`positiveTicks()` |
| `PRESSURE_INTERVAL_TICKS` | `pressureIntervalTicks` | `TICKS`，`positiveTicks()` |
| `SEED_COUNT` | `seedCount` | `COUNT`，上端用现有资源上限 |
| `SEED_INTERVAL_TICKS` | `seedIntervalTicks` | `TICKS`，`positiveTicks()` |
| `SEGMENT_DAMAGE` | `segmentDamage` | `DAMAGE`，`flatDamage()`，这是公式系数 |
| `SEGMENT_INTERVAL_TICKS` | `segmentIntervalTicks` | `TICKS`，`positiveTicks()` |
| `SEGMENT_COUNT` | `segmentCount` | `COUNT` |
| `DURATION_TICKS` | `durationTicks` | `TICKS`，`positiveTicks()` |
| `CONTROL_DURATION_TICKS` | `controlDurationTicks` | `TICKS`，`positiveTicks()` |
| `TIDE_COUNT` | `tideCount` | `COUNT`。条数不随伤害倍率改变 |
| `DETONATE_RATIO` | `detonateRatio` | `RATIO`，`unitRatio()` |
| `BOX_WIDTH` / `BOX_HEIGHT` / `BOX_DEPTH` | `boxWidth` / `boxHeight` / `boxDepth` | `BLOCKS`，`positiveSize()` |

只声明已经有实际读取点的键。不要为了“所有数值可配置”声明结构份数、去重、结算顺序、装配消耗、解锁、炉心内部公式或视觉常量。

压强伤害、剑物品伤害、火伤公式输出都不另设一个“最终伤害”键。它们先按原公式得到原始贡献，再调用一次 `damageContribution`。潮印条数用 `tideCount`；伤害用 `damageContribution("tide_mark", 原始条数乘 2)` 后保存，不再用 `Tide * 2` 回推。

## 5. B/C 怎样登记，A2 怎样封存

B 或 C 在自己的登记方法里写入专项参数，不要新建读取器，也不要调用 `seal`：

```java
BalanceRegistry.declareSpecial(TechniqueBalanceSpec.builder("water_curtain")
        .manaFee(BalanceKeys.SPAWN_MANA_COST, RawDefault.literal(8), "水幕生成费")
        .manaFee(BalanceKeys.UPKEEP_MANA_COST, RawDefault.literal(2), "水幕维护费")
        .integerParameter(BalanceKeys.UPKEEP_INTERVAL_TICKS, RawDefault.literal(20),
                BalanceBound.positiveTicks(), BalanceUnit.TICKS, "水幕维护周期")
        .integerParameter(BalanceKeys.LIFETIME_TICKS, RawDefault.literal(80),
                BalanceBound.positiveTicks(), BalanceUnit.TICKS, "水幕寿命")
        .build());
```

数字来自现有常量时，把那个常量传进 `RawDefault.literal`，不要在声明旁再抄一份。登记方法结束前不要读取 `BalanceService`。

全部 `register` 和外部内容事件都返回后，A2 再补标准键：

```java
BalanceRegistry.fillStandard(technique.getId(),
        RawDefault.calculator(techniqueId -> TechniqueManager.getById(techniqueId).getBaseCooldown()),
        RawDefault.calculator(techniqueId -> TechniqueManager.getById(techniqueId).manaCost()),
        hasRealDamageContribution);
BalanceRegistry.seal(registeredIds);
```

`hasRealDamageContribution` 只在这条术式真的会形成一笔可缩放伤害时为真。零伤规则、剑来、万剑归宗不要打开它。只存在生成费或维护费、而 `Technique.manaCost()` 不是收费依据时，`manaDefault` 传 `null`，保留 B/C 已经声明的魔力键。

封存检查：

- 每个已登记编号都有声明，且声明里的编号也都已登记。
- 每个术式都有 `cooldownTicks`。
- 每个术式至少有一个单位为 `MANA` 的整数键。费用原来是 0 也要显式声明，收费次数仍由原账单决定。
- 不要求 `damageMultiplier`。没有这个键时，直接调用 `damageContribution` 会抛出；生产结算保持原金额，不另造一条伤害公式。

外部内容事件增加 `registerBalance(TechniqueBalanceSpec)`，内部委托 `BalanceRegistry.declareSpecial`。旧的 `registerCast` / `registerEnchantment` 仍可单独使用。封存前对尚未声明的标准键补冷却、魔力和确实存在的伤害倍率，并警告外部术式的专项参数还没有声明。不要为了旧扩展发明范围或速度键。

`EnchantmentDefinitions.declareBalance(BalanceRegistry)` 收到的是 `BalanceRegistry.current()`。实例方法 `acceptSpecial` 和静态 `declareSpecial` 写同一份目录，不能另建注册表。

`globalDamageContribution` 只乘全局伤害一次，四舍五入、1e9 上限和倍率恰好为 1 时保留原 `float` 的规则与 `damageContribution` 相同。它不读取术式自己的 `damageMultiplier`。

## 6. 合并规则

同一术式的不同键按并集保存。同一键再次声明直接拒绝，即使默认值、种类、单位和边界都相同。

- 不同键：直接合并。这是 B/C 专项声明和 A2 标准补齐的正常情况，不报重复。
- 同键：抛出 `BalanceConflictException`。相同字面量、相同计算器来源、说明文字不同，都算重复声明。

因此标准键只由 A2 声明一次。B/C 不要再声明 `cooldownTicks`、`manaCost` 或 `damageMultiplier`。专项费用用 `spawnManaCost` 这类单独的键。A2 补标准键时若发现该键已存在，跳过这一键，不覆盖已有专项声明。

同一份 `TechniqueBalanceSpec` 里同一个键出现两次，构建时就拒绝。

## 7. 原始默认值的时机

三种来源：

| 模式 | 何时变成数字 | 适合 |
| --- | --- | --- |
| `literal` | 声明时检查边界，封存时冻结 | 效果旁边的现有常量 |
| `calculator` | 只在 `seal` 时对每个键求一次，之后快照不再调用 | `getBaseCooldown()`、`manaCost()` 这类要等登记完成才能读的原始值 |
| `callSite` | 不冻结。只有 `manaCost` 和 `cooldownTicks` 在没有覆盖值时使用方法参数 `rawDefault` | 数字只存在于唯一调用点、而且默认文件可以先省略该键 |

计算器禁止调用 `BalanceService`、`BalanceSnapshot` 和嵌套求值，也禁止在计算器里再声明或封存。它只能读已经登记好的代码原始值。违反时抛出 `BalanceDeclarationException`，目录保持未封存。

已经有冻结值或有效覆盖时，读取方法忽略参数里的 `rawDefault`。`rawDefault` 只填调用方来源、且该键没有有效覆盖的情况。这样调用点不必把同一个数字再写进快照。标准冷却和标准费用优先用计算器，默认文件才能写出数字，服务端和客户端的 `contractText()` 也能比对该数字。

调用方来源不能用于 `number` / `intValue` 能读到的专项键，那些键必须是字面量或计算器。

## 8. 快照里的值

术式字段是全局倍率之前的绝对技术值。三个全局倍率单独存放，只在三个读取入口各用一次。

覆盖值的处理：

- 未知术式、未知键：警告并忽略，不写回文件。
- 非有限数、越界、整数键带小数：警告，该字段回退冻结默认或调用方原始值。
- 正式编号与 `water_open_ring` 这类别名同时存在：正式编号的整份对象优先，别名对象忽略，警告 `ALIAS_IGNORED`。只有别名时，按正式编号收录并警告 `ALIAS_CANONICALIZED`。
- 冷却的代码默认值不是 0 时，配置里的 0 非法并回退。代码默认值本身是 0 时，0 可以留下。正冷却经过非 1 的全局倍率后，若舍入结果小于 1，则提到 1。全局冷却倍率恰好为 0 时，正冷却的结果是 0。
- 调用方来源的冷却配置成 0 时，快照会警告：读取时若 `rawDefault` 不是 0 就回退。标准冷却请用计算器，避免把这项留到读取时。

`resolve` 不判断 JSON 语法和 `schemaVersion`。A2 在调用前用 `requireSupported`。失败则不要用残缺映射冒充覆盖值。

同步包只带 `schemaVersion`、`acceptedGlobalOverrides()` 和 `acceptedTechniqueOverrides()`。客户端用同一套已封存声明再 `resolve`。不要同步 `generation()`，也不要把 `effectiveValues()` 当成第二份配置再乘一次。非法值已经在服务端丢掉，所以客户端不会重新看到它们；警告以服务端日志为准。

## 9. 原始伤害与已配置伤害

`RawDamage` 是尚未做平衡缩放的一笔贡献。原公式、武器和法杖加成先算进这个数。`Technique.baseDamage` 仍是结构字段，宿主、附魔边界、默认耗魔和装配继续读它。调整伤害倍率不改变默认耗魔。

`damageContribution` 做且只做这一次：

```text
已配置 = 原始贡献 × 术式 damageMultiplier × 全局 damageMultiplier
```

两个倍率都恰好是 `1` 时，结果是 `(float) rawAmount`，不再做小数舍入，也不套用十亿上限。任一倍率不是 `1` 时，乘积按半远离零舍入到小数点后 4 位，再转成 `float`；结果非有限或大于 `1000000000` 时抛出 `BalanceValueException`。

返回值是 `ConfiguredDamage`。之后：

- 领域光环和其他已配置贡献用 `plus` 相加。
- 账本最后一份伤害用 `timesHitCount` 乘命中份数。
- 旧存档、已生成的剑伤、已经印在武器上的金额用 `retained`。
- 这三种运算都不再读取平衡倍率。
- 不要把 `ConfiguredDamage.amount()` 再送进 `damageContribution`。`double` 重载无法从类型上拒绝这种误用，调用点要传入原始公式结果。优先使用 `RawDamage` 重载。
- 没有 `damageMultiplier` 的术式，生产结算不调用 `damageContribution`，原金额保持不变。直接调用 `damageContribution` 仍然抛出。

伤害倍率配置成 `0` 时，贡献金额可以是 `0`。它仍是一笔攻击贡献。调用方不得据此改 `ChainRole`、取消命中或拆掉领域绑定。

公式系数（例如段伤基数、引爆系数）用 `number` / `intValue` 读出后代入原公式。公式输出才是 `rawAmount`。不要把系数和 `damageContribution` 理解成两次平衡缩放；默认倍率是 `1`，系数默认等于旧常量，未改配置时输出不变。

火伤、水牢压强、潮印和剑伤都在各自贡献产生的那一层转换。`FireDamage`、账本结算和原版受击出口不再统一乘倍率。普通点燃后的原版燃烧伤害不进这个入口。

## 10. 费用、冷却、尺寸与舍入

`manaCost` 先取该键的技术值（覆盖值，否则冻结默认，否则 `rawDefault`），再乘一次全局魔力倍率。全局倍率恰好为 `1` 时返回原整数。否则半远离零舍入成整数。`0` 仍是 `0`。溢出抛出，不回绕。

返回值是单价。复制份数、成功目标数和维护次数仍按原账单去乘，这里不再乘。退款退收据上的原金额，不要再次调用 `manaCost`。

`cooldownTicks` 同样先取技术值，再乘一次全局冷却倍率。代码默认或有效配置为 `0` 时保持 `0`，不因倍率变成别的数。全局倍率为 `1` 时返回原整数。法杖 `scaleCooldown`、附魔段间隔和收尾冷却合成都在这个结果之后，按原顺序进行，不得再乘全局冷却倍率。

尺寸、速度和寿命用 `number` / `intValue` 替换原默认值，发生在现有能量和附魔倍率之前。它们不是第二套叠乘，也没有全局尺寸倍率。

`intValue` 拒绝带小数的已接受值。构建快照时，整数键上的小数覆盖值已经被丢弃并回退默认。

## 11. 边界

| 工厂 | 区间 |
| --- | --- |
| `multiplier()` | `[0, 1000]` |
| `mana()` | `[0, 1000000]` |
| `cooldownTicks()` | `[0, 1200000]`，并且只有原始默认值为 0 时才接受配置 0 |
| `positiveTicks()` | `[1, 1200000]` |
| `nonNegativeSize()` / `positiveSize()` | `[0, 512]` / `(0, 512]` |
| `nonNegativeSpeed()` / `positiveSpeed()` | `[0, 64]` / `(0, 64]` |
| `damageReduction()` | `[0, (double) 0.6f]` |
| `unitRatio()` | `[0, 1]` |
| `flatDamage()` | `[0, 1000000]` |
| `count(lower, upper)` | 调用方给出的闭区间，数量上端沿用现有资源上限 |
| `inclusive(lower, upper)` | 调用方给出的有限闭区间 |

非有限数一律拒绝。

## 12. A2 安装与清理

```text
BalanceSchema.requireSupported(schemaVersion);
List<BalanceWarning> warnings = new ArrayList<>();
BalanceSnapshot snapshot = BalanceSnapshot.resolve(globalOverrides, techniqueOverrides, warnings);
BalanceService.install(BalanceSide.SERVER, snapshot);
BalanceService.bind(BalanceSide.SERVER);
```

默认文件或世界文件不能用时：

```text
BalanceService.install(BalanceSide.SERVER, BalanceSnapshot.defaults());
BalanceService.bind(BalanceSide.SERVER);
```

不要在这种情况下重写损坏文件，也不要清掉未知键。

客户端登录后，用同步来的已接受覆盖值执行同样的 `resolve`，然后 `install(CLIENT)` 与 `bind(CLIENT)`。断线：

```text
BalanceService.clear(BalanceSide.CLIENT);
```

客户端刻线程保持绑定 `CLIENT`。这样后续读取落到代码默认快照，不会误用服务端快照，也不会因为未绑定而抛出。不要 `clear(SERVER)`。

服务器停止时 `clear(SERVER)`，并 `unbind` 服务端线程。下一次世界仍使用同一次封存的声明，重新 `resolve` 再 `install(SERVER)`。本版不在世界进行中热替换快照。

工作线程如果不能保证绑定，显式传入 `BalanceSide`。集成服务器的服务端刻线程和客户端刻线程要分别绑定，不能共用一个静态“当前侧”。

## 13. 本阶段没有做的事

- 不生成 62 个术式的默认目录，也不读取现有 `Technique`。
- 不加载 `defaultconfigs/xueximod-balance.json` 或世界 `serverconfig`。
- 不修改释放、命中、火伤、附魔、界面或 `RegisterXueXiContentEvent`。
- 不改 `xueximod-common.toml` 与地形配置。

玩法要等 A2 把读取接进真实贡献点，B/C 把专项声明接进真实消费者之后才会变化。默认快照下，倍率按 `1` 处理；伤害在双倍率为 `1` 时只做与现有 `float` 转换相同的收窄，费用和冷却在全局倍率为 `1` 时保持原整数。
