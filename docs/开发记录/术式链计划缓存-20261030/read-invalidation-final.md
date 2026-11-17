我先读这份任务说明，按它写明的授权范围执行，再交完整终稿。任务是只读定位装配、定义和缓存失效来源。我按指定文件精确查找读写入口，不展开整仓。装配链的结构来自每次读取时重新解析的术式序列。当前没有按内容保存的计划缓存；`CompiledReleaseStep` 只在一次按键里现编译。

## 读取口

剑技槽 0–3 在玩家持久数据里。`PlayerBoundSkills.get`（`D:\alianximod\src\main\java\com\bslx\XueXiMod\player\PlayerBoundSkills.java` 第 10–19 行）每次 `Skill.fromNbt`，得到新的 `Skill`。`getTechniques()` / `getEmbryos()` 返回内部 `ArrayList`，可被装配代码直接改（`D:\alianximod\src\main\java\com\bslx\XueXiMod\skill\Skill.java` 第 33–37、44、51 行）。未知 `Id` 会被丢掉（第 125–139 行）。

法杖链在主手物品上。`StaffSkills.asSkill`（`D:\alianximod\src\main\java\com\bslx\XueXiMod\item\StaffSkills.java` 第 106–121 行）每次新建 `Skill("法杖", SkillType.MAGIC, …)`，同样跳过解析失败的 `Id`。`entries` 是新的 `ArrayList<Entry>`（第 41–53 行）。`readTechniques` 在已有 `Techniques` 时返回物品标签里的那份 `ListTag`（第 124–132 行）；没有该键时走 `migrate` 并写回（第 135–140 行）。

释放入口是 `SkillUsePacket.offerNow`（`D:\alianximod\src\main\java\com\bslx\XueXiMod\network\SkillUsePacket.java` 第 146–170 行）：法杖读主手 `asSkill`，且 `slotIndex` 必须为 0；剑技读 `PlayerBoundSkills.get`。`SkillCarrier` 只判断手持物能不能放，不存链。`StaffData` 全是 `final` 字段，挂在法杖物品类上（槽位数、冷却缩减、属性伤害加成），不在链 NBT 里（`D:\alianximod\src\main\java\com\bslx\XueXiMod\item\StaffData.java` 第 4–25 行）。

结构解析发生在切片之后：`ChainSteps.list` → 按签名取游标 → `slice` → `TechniqueChain.split` → `CompiledReleaseStep.compile`（`SkillUsePacket.java` 第 249–265、306 行）。`split` 返回新的 `ArrayList`；`ChainSegment` 的附魔下标是 `List.copyOf`（`D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\chain\ChainSegment.java` 第 9–11 行）。

## 什么会改变结构

`ChainSteps.list`（`D:\alianximod\src\main\java\com\bslx\XueXiMod\network\ChainSteps.java` 第 21–62、154–169 行）和 `TechniqueChain.split`（`D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\chain\TechniqueChain.java` 第 20–61 行）看的是顺序、`isEnchantment()`、`ReleaseDefinition.role()`、`chainEnergyScale()`，以及双重的 `pullAfterA()`。没有单独的步进模式字段；步进表是由这些定义推出来的。

`SkillType` 不进入 `list` / `split`。它进入 `frozenChainInput`：只有 `SkillType.MAGIC`、成员为魔法且非附魔、术式属性不是 `NONE`、种类为命中体积时，才把前面的链能量收成 `ReleaseInput`（`D:\alianximod\src\main\java\com\bslx\XueXiMod\network\SkillChainPlan.java` 第 545–573 行）。法杖路径固定写成 `MAGIC`，剑技装配写成 `SWORD`。

原胚 `Embryo` 不改变分段成员，也不进入 `ChainSteps.signature`（第 65–73 行，只拼接术式 id）。它会进入 `slice`，并改变附魔冷却：`ChainSteps.enchantCooldown`（第 100–107 行）和 `TechniqueChain.gapBefore`（`TechniqueChain.java` 第 64–76 行）。无法识别的原胚变成 `NONE`（`ShuAttribute.java` 第 33–38 行）。技名不参与上述解析。

`CompiledReleaseStep.compile` 还会把 `cooldownReduction`、领域/屏障/护体费用、蓄力拒绝写进这一次的对象（`D:\alianximod\src\main\java\com\bslx\XueXiMod\network\CompiledReleaseStep.java` 第 50–97 行）。冷却缩减来自法杖的 `StaffData`，剑技传入 0。属性伤害加成和主手攻击力在 `executeSkill` 里另算（`SkillUsePacket.java` 第 228–231 行），没有写进分段表。

游标另存。签名只含 id，所以只改原胚不会把游标打回 0。剑技游标在 `ChainProgress` 的 `ConcurrentHashMap<UUID, …>`（`D:\alianximod\src\main\java\com\bslx\XueXiMod\network\ChainProgress.java` 第 13–16、32–41 行）。法杖游标在物品标签 `xueximod_chain_sign`、`xueximod_chain_step`（第 25–30、46–49 行）。全库没有第二处引用，登出和停服都不清这张表。

## 和链有关的 NBT 与写入口

会改变下一次释放所读序列的写入：

| 位置 | 键 | 写入 |
| --- | --- | --- |
| 玩家 `getPersistentData()` | `xueximod_bound_skills` / `BoundSkill_0`…`3` | `PlayerBoundSkills.set` 第 22–32 行。内层是 `Skill.toNbt`：`Name`、`Type`、`Techniques[{Id, Embryo}]`（`Skill.java` 第 81–97 行） |
| 法杖物品标签 | `xueximod_staff_skills` / `Techniques[{Id, Embryo}]` | `StaffSkills.add` / `remove` / `move`（第 55–104 行）。旧的 `SlotN` 只在读取迁移时被展平 |
| 同上 | `xueximod_chain_sign`、`xueximod_chain_step` | 只有释放推进时写，装配写入不改它们 |

直接写入口有两处，都在服务端：

- `SkillAssemblyPacket` 的放入、移除、左右移（`D:\alianximod\src\main\java\com\bslx\XueXiMod\network\SkillAssemblyPacket.java` 第 101–173、176–252、255–262 行）。剑技随后 `BoundSkillSyncPacket.send`。
- `SpellAssemblyMenu` 格子 `set` / `onTake` 调用 `writeGear`（`D:\alianximod\src\main\java\com\bslx\XueXiMod\gui\SpellAssemblyMenu.java` 第 241–289、560–575 行）。

换武器：剑技不在武器上，换剑不换这四条链。换法杖会换 `asSkill(主手)` 读到的物品标签。复制、分堆、存档带走的是物品标签本身，没有单独的复制钩子。玩家存档带走 `xueximod_bound_skills`。死亡克隆在 `PlayerConnectionHandler.onPlayerClone` 复制该键，以及 `xueximod_shu_data`、`xueximod_spell_storage`（`D:\alianximod\src\main\java\com\bslx\XueXiMod\event\PlayerConnectionHandler.java` 第 35–40 行）。

不参与当前释放解析的标签：术式匣 `Items[{spell_id, embryo, count}]`（`SkillStorageData.java` 第 12–15、33–42 行），只在装配时被消耗或退回；技能书 `SkillData`；命令 `bindBookToWeapon` 写到武器上的 `BoundSkill_N` 和 `HasBoundSkills`（`SkillBookItem.java` 第 38–83 行，`ShuCommands.java` 第 425 行起）。释放不读武器上的 `BoundSkill_`。`BoundSkillTooltip` 会读。命令路径不调用 `PlayerBoundSkills.set`。

## 定义对象与代际

`Technique` 字段全是 `final`，没有 setter（`Technique.java` 第 17–27 行）。四张表都是 `LinkedHashMap.put`，没有 generation / revision，也没有卸下或数据包重载：

- `TechniqueManager.register` 按 id 覆盖（第 12–14 行）。`getAll()` 是不可变视图，元素仍是表中的同一对象。
- `ReleaseRegistry.register` 同样覆盖（第 15–16 行）。`ReleaseDefinitions.define` 的匿名实现只闭合构造参数（`ReleaseDefinitions.java` 第 112 行起）。`WaterReleaseDefinition` 有非 `final` 字段，登记时用 `swordForm()` 等改完再放入表（`WaterTechniques.java` 第 21–29 行，由 `ModTechniques.registerAll` 第 303 行调用）。登记后没有只读包装。
- `TechniqueEffectRegistry`、`EnchantmentRegistry` 也是 put 一张实例（两文件第 12–18 行附近）。效果接口的 `execute` 接收当次的玩家、武器和施放上下文。附魔定义另建累加器，累加器不属于定义本身。

登记顺序：`XueXiMod.commonSetup` 调用 `ModTechniques.registerAll()`，然后投递 `RegisterXueXiContentEvent`（`XueXiMod.java` 第 93–98 行）。事件拒绝已占用 id（`RegisterXueXiContentEvent.java` 第 36–42、70–74 行）。直接再调 `TechniqueManager.register` 仍会覆盖。`XueXiMod.onServerStarting` 是空的（第 112–114 行）。领域、护体、接触上的 `revision` 是运行实例代际，不是这四张登记表的代际。

## 共享内容键的事实约束

分段、步进表和 `frozenChainInput` 的输入是有序术式 id、每个下标的原胚、`SkillType`，以及进程内不变的登记对象。玩家 id 和槽号不进入这三个函数。槽号进入游标和冷却载体键。

若缓存对象里含已经乘过冷却缩减的整数，键还要带上法杖的 `cooldownReduction`；剑技这条输入恒为 0。游标、本次魔力回执、`ReleaseBatch`、宿主提交都不能放进可共享对象。共享对象也不能持有玩家、`Level`、实体、可变 `ItemStack`，或活的 `CompoundTag` / `ListTag`。`CompiledReleaseStep.members` 是可变 `ArrayList`，并已烤进冷却缩减（构造函数第 32–47 行），它现在只挂在当次 `CastExecutionSession` 上。

装配写入没有失效回调。下一次 `get` / `asSkill` 会从标签重新解析。

## 已有事件与计划设施

Forge 总线、`@Mod.EventBusSubscriber(modid = xueximod)`：

- `ModEvents.ServerEvents`：`ServerStartedEvent` 取消施放会话、冷却换代、清领域脉冲（第 31–36 行）；`ServerStoppingEvent` 再取消并结束冷却（第 39–44 行）；`LevelEvent.Unload` 只清偏转宿主（第 48–49 行）；登录、克隆、重生、登出转给 `PlayerConnectionHandler`（第 53–88 行）。登出只 `PlayerShuData.save` 并 `MagicPool.forget`，不清剑技游标，也不清绑定链。
- `PerformanceEvents` 在 `ServerStartedEvent` 里 `bindFreshServer`，停服时 `releaseServer`（`PerformanceEvents.java` 第 21–36 行）。
- 客户端 `ClientPlayerNetworkEvent.LoggingOut` 调用 `BoundSkillSyncPacket.clearClient()`（`ModEvents.java` 第 117–118 行）。

现成设施是按次编译，不是缓存图：`CompiledReleaseStep.compile`、`SkillChainPlan.ModifierPlan`（字段可变，第 774–797 行）、`ChainProgress` 游标。没有计划 `Map`，登记表也没有代际可作键。

## 命中计数的最低接入

`PerfMetric`、`PerfCounter` 是封闭枚举。`PerformanceRecorder.add` 只在采样 tick 门打开、且调用发生在绑定的服务器线程上时，把正增量加进该枚举序号（`PerformanceRecorder.java` 第 117–128 行）。`/shuperf export` 按 `PerfCounter.values()` 把 `counter <NAME> total=` 写入摘要（第 575–580 行；命令在 `PerformanceCommands.java` 第 27–28 行）。新增常量并在命中/未命中处 `add(..., 1)` 后，导出会自动带上该行。零和负数会被丢掉。

`CHAIN_SPLIT` 已经包住 `TechniqueChain.split`（`PerfMetric.java` 第 10–11 行，`TechniqueChain.java` 第 21–44 行）。跳过 `split` 的命中会减少这项的调用次数。现有计数里没有术式链计划命中项。`TARGET_QUERY_CALLS` 的注释只约定实体查询缓存命中不加一，和链计划无关。

## 未知与候选文件

未知：登记完成后是否还有代码再调用 `WaterReleaseDefinition` 的链式改字段方法。本次只看到 `WaterTechniques.register`。外部模组若绕过事件、直接 `TechniqueManager.register`，可以覆盖同一 id，且没有代际可观察。

下一步若做缓存，相关文件是：`SkillUsePacket.java`、`ChainSteps.java`、`TechniqueChain.java`、`ChainSegment.java`、`CompiledReleaseStep.java`、`SkillChainPlan.java`、`ChainProgress.java`、`PlayerBoundSkills.java`、`StaffSkills.java`、`Skill.java`、`SkillAssemblyPacket.java`、`SpellAssemblyMenu.java`、`ModEvents.java`、`PlayerConnectionHandler.java`。观察命中再动 `PerfCounter.java` 与调用处的 `PerformanceRecorder.add`。`SkillStorageData.java`、`SkillBookItem.java`、`ShuCommands.java` 的武器绑定键不是当前释放链。

本次只读，工作区无改动。
