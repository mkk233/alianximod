我先读取这份任务说明，确认授权范围后再按它执行。这是 C1 只读采集：先读本目录的架构裁决、实现合同和三份终稿，再只定向读归 C 的源码。不读 `shu/balance` 的中间代码，也不写文件。文档里的火伤和附魔路径已经对上。接下来只读 C 名下的源码，核对原公式收束点和跨界调用签名。C1 只读采集完成。未改文件，未编译，未测试，未派代理，未读 `shu/balance`，未读写制作表。设计仍为 1/2，实现 0/3。下面是后续实现要用的接口和事实。

## 1. 火伤 / 炉心：原公式收束点

统一合同是：每笔贡献先按现有公式得到原始金额，再按来源术式调用一次 `damageContribution(id, rawAmount)`。`FireDamage.strikeOnce` 之后只结算，不再乘配置。总出口不乘：`FireDamage.strikeOnce`、`FireRuntimeEvents.onHurt`、`ElementAdjustment`、`FireHurtBoundary` 都不加配置倍率。

现有结算签名保持不变：

```34:35:src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/FireDamage.java
    public static boolean strikeOnce(LivingEntity target, UUID casterId, double damage, double fireBase,
                                     boolean furnaceSettled) {
```

`damage` 是送进 `hurt` 的金额。`furnaceSettled == false` 时，`LivingHurtEvent` 里才补炉心：`currentAmount + fireBase * 当时炉心`。`furnaceSettled == true` 时原样返回。炉心热量公式本身不开放。

```11:18:src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/ElementAdjustment.java
    public static double fireAmount(double currentAmount, double fireBase, double furnaceBonus, boolean furnaceSettled) {
        if (furnaceSettled || !Double.isFinite(currentAmount)) {
            return currentAmount;
        }
        // ...
        double adjusted = currentAmount + fireBase * furnaceBonus;
```

已烤进公式的炉心用 `FireLayer.combine(base, resourceAboveOne, furnaceBonus)`，即 `B * (1 + A + F)`。在线炉心读数是 `FurnaceService.fireBonus(LivingEntity)`；非法或施法者不在线时，现有调用把它当成 0。

| 贡献 | 原公式收束 | 现有调用 | 缩放后怎么进 `strikeOnce` |
| --- | --- | --- | --- |
| 燎原本休 | 固定 `GROUND_DAMAGE = 8`。`furnaceSettled=false`，且 `damage` 与 `fireBase` 都是 8，所以最终原始金额是 `8 * (1 + 当时 F)` | `GroundFireFields` 本体拍：`strikeOnce(target, casterId, 8, 8, false)` | 先读与 `FireRuntimeEvents.casterBonus` 相同的当时 F，算出 `8*(1+F)`，再按 `fire_liaoyuan` 缩放。然后 `strikeOnce(..., 已配置金额, 0, true)`。不要留着 `false`，否则会再加一截未缩放的 `8*F` |
| 燎原火种段 | 挂载时冻结 F：`4 * (1 + 0.1*档 + F)`，档 = 余额/10，间隔 20，段数 = 3+档 | `FireConsumerPlans.spreadSpec(int, double)` → `FireConsumption.scheduleOpening(..., "xueximod:fire_liaoyuan", spreadSpec)` | 整段原始金额按 `fire_liaoyuan` 缩放一次，写入 `SharedSpec.fullSegmentDamage`。燃料比例 `full * share / demand` 只分这一份已配置金额，不是第二次配置倍率。脉冲仍走现有 `strikeOnce(entity, casterId, pulse.damage(), 0, true)` |
| 焚天段 | `2 * (1.2^档 + F)`，档上限 10，间隔 4，最多 10 段 | `skySpec(int, double)`；B 调用 `FireConsumption.schedule(target, cast, "xueximod:fire_fentian", skySpec)` | 同燎原段：挂载时按 `fire_fentian` 缩放整段。`spread(int,int,int)` 和 `sky(int,int,int)` 没有调用方，不是运行时收束点 |
| 引燃段 | `2 * (1 + F)`，A=0；期限 `40+10*档`，段数 = 期限/10，间隔 10，每段燃料 10 | `KindlePlan.specify(int, double)`，经 `FireConsumption.scheduleKindle` | 整段按 `fire_kindle` 缩放后锁进 `fullSegmentDamage`。段伤 2、平直 40、每档 10、间隔 10 是公式输入，先替换默认值，再进公式，再做这一次伤害缩放 |
| 引爆 | `0.2 * 余额 * (1 + 当时 F)`，已经 `furnaceSettled=true`。旁边的 `fireBase = 0.2*余额` 因为已结算不会再加 | `DetonatePlan.strike(LivingEntity, FireCastContext)` | `damage(int, double)` 继续返回原始公式。只在 `strike` 里、`strikeOnce` 之前按 `fire_detonate` 缩放。保持 `furnaceSettled=true` |
| 燎里火种 | `FireLayer.combine(花掉的火种, 0, 快照 F)`，已结算。唯一调用方是燎里 | `FireConsumption.detonate(target, cast, requested, StrikePlanner)`；燎里传入 `ALL_SEEDS` 和 `FireConsumerPlans::thrust` | C 在 `detonate` 内部按 `fire_liaoli` 缩放合入后的金额。签名不变。不缩放火种数量 |
| 灼地接触 | 固定 1，且已经 `furnaceSettled=true`，炉心不加 | `SurfaceFireHosts` 首次接触：`strikeOnce(target, ownerId, 1, 1, true)` | 按 `fire_scorch_field` 缩放这 1。保持已结算，不要改成补炉心 |
| 主动冷却蒸汽 | 固定 2，中性帧，不读炉心、火种、元素 | `SteamBurst.release(Player)` → `candidate.hurt(source, 2)`；由 `FurnaceService.cool` 调用 | 按 `fire_cool` 缩放这 2，仍走中性帧。不改成火元素帧 |

燎原本休和燎原火种段是两笔贡献，都用 `fire_liaoyuan`，各缩放一次。焚天、引燃、引爆的延迟金额在挂载/打击当时就带有来源，不能等到 `hurt` 再从伤害类型猜。`BurnPulse.effectKey` 今天在 `BurnCarrier` 的 lambda 里被丢掉；缩放放在写入 `fullSegmentDamage` 之前，脉冲金额已经是配置后的值。

效果键不是术式 ID：`"xueximod:fire_liaoyuan"`、`"xueximod:fire_fentian"` 对应 `fire_liaoyuan`、`fire_fentian`；引燃键本身就是 `fire_kindle`。`damageContribution` 用术式 ID。

燃烧 NBT 现在只接受版本 1 和 2，其它版本整份丢弃。版本 1/2 的 `damages` 和 `full` 已是旧公式的最终金额，读档不得再乘。新挂载若存的是已配置金额，写入须用新版本并让读取接受它；缺版本仍按旧最终金额解释。存的时候乘过，读的时候不能再乘。

不缩放的边界：原版点燃后的燃烧伤害、炉心热量/产种倍率/水冰承伤、`SEED_CAP=100`、每段燃料 10、焚天 10 段上限、档位上限 10。

## 2. 专项登记由谁调用

现有玩法登记是 `ModTechniques.registerAll` 里的 `EnchantmentDefinitions.registerAll()`。那里只建附魔定义和原始默认值，不能读有效快照，也不能把配置写进 `static final`。

建议 A 在内置和外部术式都登记完、封存参数目录时只调用一次：

`EnchantmentDefinitions.declareBalance(BalanceRegistry registry)`

这个方法再按固定顺序调用 C 自己的工厂声明。B 的效果类不要再声明同一批键。A 不自动从 `Technique.baseDamage` 补第二份同名键。

伤害倍率、费用、冷却仍是 A 的标准键，C 不重复 `put`。下列术式的实伤不是 `baseDamage`，或者 `baseDamage` 为 0；A 的标准伤害倍率仍要有，否则 C 无法调用 `damageContribution`：`fire_liaoyuan`、`fire_fentian`、`fire_liaoli`、`fire_scorch_field`、`fire_kindle`、`fire_detonate`、`fire_cool`。

C 只声明下面这些专项键。默认值等于现在的字面量。消费者在施放或挂载时读快照，不在类初始化时读。

| 建议键 | 默认 | 唯一消费者 |
| --- | --- | --- |
| `fire_seed_sow` `seedCount` | 10 | `ResourceEnchantCatalog.collect` 写入载荷的种火基数。只替换附魔这截，不改调用方传入的本体基数 |
| `fire_kindle` `segmentDamage` / `intervalTicks` / `flatTicks` / `tierTicks` | 2 / 10 / 40 / 10 | `KindlePlan.specify`、`durationTicks`。每段燃料需求 10 不开放 |
| `fire_nether_gloom` `seedCount` | 5 | `NetherSeedArbiter.grant`，在 `FireSeedMath.floorProduction` 之前替换基数。炉心产种倍率仍在其后乘一次 |
| `fire_nether_gloom` `periodTicks` | 20 | 见第 4 节，B 改读方法 |
| `fire_scorch_field` `lifetimeTicks` | 100 | `SurfaceFireHosts` 的 `FIELD_LIFE`。灼地效果不读释放定义寿命，B 不要再声明一条 |
| `fire_scorch_field` `halfExtent` | 2 | `SurfaceContactField.HALF_EXTENT`。前进步长 1、竖直松弛 3 不开放 |
| `fire_cool` `radius` | 3 | `SteamTuning.RADIUS`。击退强度不开放 |
| `fire_liaoyuan` `radius` | 10 | 只声明这一次。B 在乘 `frame.volumeScale()` 之前读取，再传给现有 `GroundFireFields.open`。C 不在 `open` 里重读。寿命用释放定义那一条，C 的 `FIELD_LIFE=200` 只留作定义为空时的同一默认 |
| `vine_mark` `controlTicks` | 40 | `EnchantBonus.resolve` 与 `EnchantmentSnapshot.read` 里替换字面量 40，其后仍只乘一次反应持续时间 |
| `magic_ignite` `burnSeconds` | 5 | 同上两处替换着火秒数基数。这只改着火时长，不宣称控制原版燃烧伤害 |

焚天半径 8 在 B 的 `FentianEffect`，由 B 声明和消费。C 不声明 `fire_fentian` 的半径。焚天段数上限保持 10。

魔力仍由 A 在单价形成时包一层。`ResourceEnchantCatalog.explicitManaPrice` 继续返回原始单价 2，C 不在这里乘全局魔力。旧五附魔的 `magicDamage(ElementReaction)` 继续做原始费用计算器：潮印固定 2，其余 `ceil(2 * 反应伤害倍率)`。配置潮印伤害不得改这个返回值。

## 3. 给 A 的附魔金额，以及潮印 NBT

链段现在把四样加在一起。A 的 `SkillUsePacket`、`BatchCastSession`、`CastExecutionSession`、`SkillChainPlan`、`AutomaticRound`、`SkillFollowCast` 都调用：

`EnchantBonus.resolve(Skill, List<Integer>, float attackBase, int layers)`

`float carriedDamage(float weaponBonus)` = 同属性加成后的 `attackDamage` + `addedDamage` + `tideMarks * 2` + 武器

这条在 C2 里保持为原始合计，供尚未切换的调用方行为不变。C 不在 `carriedDamage` 里缩放，避免 A 再乘一次。

建议新增只读拆分，A 用它们组已配置段伤：

- `rawHostAttack()`：字段 `attackDamage`。这是宿主底伤经过同属性 bump 之后的数，不含附加、潮印和武器。A 把 `rawHostAttack + weaponBonus` 作为宿主一笔，用宿主术式 ID 缩放一次。武器不再单独缩放。
- `rawAddedDamage()`：火/木/土/金那一笔附加，已经包含同属性 bump。
- `addedSourceId()`：这一笔的术式 ID。
- `tideMarks()`：条数。
- `rawTideDamage()`：现有 `tideFlat()`，即 `tideMarks * 2`。A 只用 `tide_mark` 缩放这一笔。

接触伤是另一条公式。`EnchantmentSnapshot.damageOf` 按每个附魔下标累加 `magicDamage`，潮印每个下标加 2，资源附魔加 0，不加武器、不加 `addedDamage`。建议在 `damageOf` 的循环里按该下标的术式 ID 各缩放一次，再写入 `contactDamage`。B 的 `ContactEnchantState` 和 `DomainContactBilling` 继续把这个 float 原样送进现有 `hurt`，不要再乘。条数、类型名单和幽冥标记保持独立，伤害倍率变成 0 时不得把它们清掉。

潮印条数和有效伤害必须分开：

- 条数继续驱动潮湿 `marks * 5`、水势 `marks * 2` 和 `TideMarkSuccess`。不因伤害配置改变条数。
- 有效伤害是单独金额。链段用上面的 `rawTideDamage`。武器印记不能再靠 `Tide * 2` 还原配置后的伤害。

武器 NBT 根键是 `xueximod_weapon_enchant`。今天有 `Attack`、`Added`、`Burn`、`SeedPoints`、`Radius`、`Control`、`Extinguish`、`Knockback`、`Tide`，印记时另有 `ResourceIds`。没有版本，也没有独立伤害字段。

`includeTideFlat` 只加在 `PlayerAttackMixin` 的主目标那一刀上：`原版金额 + Tide*2 + Added`。横扫那一刀不加。`Tide > 0` 时 `onAttack` 直接返回，不会再打第二刀。无潮印时，第二刀是 `playerAttack(Attack + Added)`；这里的 `Attack` 是印记当时的武器攻击力，可能已被同属性 bump，没有宿主术式 ID。

建议：

- 新增版本字段。缺版本视为旧印记：`Tide * 2` 与 `Added` 都是旧的最终附加额，读出即用，不再缩放。
- 新版本另存 `TideDamage`（已按 `tide_mark` 缩放）和已按来源缩放的 `Added`。读出后只相加。`Tide` 仍只是条数。
- 并进原版主刀时只加这些已配置附加额，不乘原版那一刀，也不给横扫加这份金额。
- 无潮印第二刀的 `Attack` 保持印记时的武器数，不送进 `damageContribution`。只把已配置的 `Added` 加进去。

有一个已批准接口盖不住的点，不改公式：`EnchantBonus.resolve` 里火、木、土、金共用一笔附加。第一笔把它设为 2，后面的同类不再各加 2，同属性 bump 打在这一笔上。多个来源同时改写这一笔时，不存在「按来源各缩放再相加、且默认倍率下仍等于现在这一笔」的拆分。单一伤害来源时，`addedSourceId()` 就是那个术式。接触伤路径本身就是按下标累加，可以各缩放一次。这两条路径不要合并成同一公式。

## 4. B/C 跨界调用

C 内部能完成、B 不必改签名的：

- `FireConsumption.schedule(LivingEntity, FireCastContext, String, SpecSource)`  
  焚天：`schedule(target, fireCast, "xueximod:fire_fentian", FireConsumerPlans::skySpec)`
- `FireConsumption.scheduleOpening(...)`  
  燎原火种段由 `GroundFireFields` 自己挂上。
- `FireConsumption.scheduleKindle(LivingEntity, FireCastContext)`
- `FireConsumption.detonate(LivingEntity, FireCastContext, int, StrikePlanner)`  
  燎里：`detonate(target, fireCast, Integer.MAX_VALUE, FireConsumerPlans::thrust)`
- `DetonatePlan.strike(LivingEntity, FireCastContext)`
- `GroundFireFields.open(ServerLevel, FireCastContext, Vec3, double radius, int lifeTicks)`  
  半径和寿命仍由调用方传入。
- `SurfaceFireHosts.open(ServerLevel, ServerPlayer, ReleaseBatch, BoundReleasePlan, EnchantmentSnapshot, ResourceEnchantLoad, UUID)`
- `FireSeedProduction.produce(LivingEntity, LivingEntity, int)` 只被灼地调用。炉心产种倍率在内部乘一次；基数是否可配由 C 在调用前替换，不在 `produce` 里猜术式。
- `ResourceHitCommit.commit(LivingEntity, FireCastContext, LivingEntity, ResourceEnchantLoad, int bodyScaledBase, int legacyFixedPoints, boolean)`  
  调用点在 `ReleaseBatch`、`ResourceSettlement`、`BatchFlightBook`、`BatchShockRingEffect`、`WeaponEnchant`。种火配置只替换载荷里的附魔基数。`bodyScaledBase` 和旧固定点数保持调用方原值；旧的 2 点火种在取整之后相加，不参与炉心产种倍率。
- `ResourceEnchantCatalog.explicitManaPrice(String)` 被 `Technique.manaCost` 和 `SkillSegmentMath.manaCostOf` 当作原始单价。A 在这两处包 `manaCost`，每个装配格仍各计一次。
- `SteamBurst.release(Player)` 仍由 `FurnaceService.cool` 调用。蒸汽伤害在 `release` 内缩放。炉心三式的扣费在 B 的 `SelfStatusCommit.attempt`：`definition.explicitManaCost()` 后走 `MagicPool`。C 不在冷却或蒸汽里再乘魔力。

需要 B 改调用、C 不改 B 的文件：

- 燎原半径：`LiaoyuanEffect` 今天读 `GroundFireFields.BASE_RADIUS`，再乘一次 `volumeScale`，寿命读释放定义。B 改为读第 2 节那一个半径键，能量缩放仍只乘一次，然后照旧调用 `open`。
- 燎里物理：原始金额在 `LiaoliEffect`，`technique.getBaseDamage() + 1.5 * 迎面速度`，迎面速度上限 40，再 `TechniqueHits.hurtRoot`。B 对这一笔按 `fire_liaoli` 缩放后再送进 `hurtRoot`。火种那一笔由 C 在 `detonate` 里缩放。两笔都归 `fire_liaoli`，各一次。
- 幽冥周期：B 的 `NetherDomainCycle` 读 `NetherContract.PERIOD_TICKS`（静态 20）三处：开窗取模、窗口号、开域后的等待。C 提供读取已配置周期的方法，默认仍是 20。B 改读该方法。产种基数 5 只在 `NetherSeedArbiter.grant` 消费，B 不传这个数。`NetherContract.BASE_POINTS` 没有其它调用方。
- 焚天半径 8 由 B 在自己的效果里声明和消费。C 不新增同键。
- 留焰的寿命 40 和直径 0.5 由 B 的载体读取 `TrailContract`。批准范围没有给留焰单独的范围/寿命键，C 不另开。

附魔控制时长的实际应用点还有 B/A 侧的帧和剑：`SkillSegmentRunner` 把 `burnSeconds`、`controlTicks` 抄进 `CastFrame`，剑和接触结算再执行。C 在 `EnchantBonus` 和 `EnchantmentSnapshot` 两处用同一默认值生成这些数，生成前替换配置基数。不要在剑或 `hurt` 上再缩放时长。

本次没有发现还要改归属的文件。除上面那一笔无法拆开的附加伤以外，已批准方案能覆盖这些调用。
