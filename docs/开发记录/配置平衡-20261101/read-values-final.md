我先读取这份任务说明，按它授权的范围执行，并给出完整最终报告。任务是只读事实采集：登记术式的可调平衡字段、真正消费处，以及伤害、魔力、冷却和同步链路。我先定位注册与结算入口。注册表里的伤害、冷却和寿命已经能对上。接下来顺着真正扣血、扣魔力和冷却的调用链往下读。现有数值全是 Java 常量，没有平衡配置接入伤害、魔力或冷却。`Config.java` 和地形破坏开关都不读这些字段。客户端界面读的是本机同一套登记，伤害数字不进实体同步。

## 共用字段与真正消费处

`Technique`（`shu/Technique.java` 20–21、63–66、97–120）上能调的是 `baseDamage`、`baseCooldown`。`projectileSpeed` 只进术石数据（`shu/SkillStoneData.java` 31–40）和缺月构造时从释放定义抄来的速度（`shu/WaterTechniques.java` 23–27），飞行不读它。

`ReleaseDefinition`（`shu/technique/release/ReleaseDefinition.java` 21–25、133–145、143–149、171–251）上能调的是寿命、速度、厚度、显式魔力、显式装配消耗、自身能量、链聚能、场地半径、治疗、牵引、潮湿、击退、上抬。`chainEnergyScale != 1` 的规则会被当成链修饰（`network/SkillSegmentMath.java` 111–116）。聚能登记是链聚能 2（`shu/technique/release/ReleaseDefinitions.java` 31）。

登记入口：

- 绝大多数术式：`shu/ModTechniques.java` 47–277 的伤害和冷却，加上 `shu/technique/release/ReleaseDefinitions.java` 11–58 的寿命、速度、厚度、显式魔力。
- 水系成品：`shu/WaterTechniques.java` 21–39。
- 陨岩、热波、火苗、幽冥领域：`shu/technique/release/batch/ListedRelease.java` 71–111。
- 炉心三式显式魔力 20、20、0：`shu/technique/release/selfstatus/SelfStatusDefinition.java` 23–26、85–86。术式冷却仍是 500、500、0（`ModTechniques.java` 181–191）。
- 垂丝单独常量：`shu/technique/release/WeepingSettings.java` 8–18。底伤 2，显式魔力仍是 10，寿命 40，潮湿 3，命中间隔 10。

魔力报价顺序（`Technique.java` 97–120，再被 `SkillSegmentMath.manaCostOf` 239–252 包一层）：资源附魔显式价，否则释放定义 `explicitManaCost`，否则附魔走 `magicDamage`，否则领域 `initialCost`，规则段为 0，再否则 `ceil(baseDamage * 2)`。水牢另走单价 40（`shu/technique/release/conditional/WaterPrisonEffect.java` 35、42），不走这条底伤公式。

## 伤害调用链

玩家攻击这一路最终都是 `playerAttack`，入口在 `shu/technique/release/TechniqueHits.java` 38–41、47–65、90–103、117–129。

1. 组段伤害在 `network/SkillUsePacket.java` 810–817。没有附魔时走 `SkillSegmentMath.segmentDamage`（131–136）：`baseDamage` 先乘 `(1 + 法杖属性加成)`（仅当底伤大于 0 且加成非空，138–140），再加武器加成和附魔 `magicDamage`。有附魔索引时改走 `EnchantBonus.carriedDamage`（`EnchantBonus.java` 71–73、80–84）：底伤经同属性加成后的 `attackDamage`，加 `addedDamage`，加潮印条数乘 2，加武器。这两支互斥，不会把 `magicDamage` 再加一次。
2. `SkillSegmentRunner` 608 行再乘场地 `damageScale`，写入 `CastFrame.damage`（`CastFrame.java` 103）。场地规则是 0 块或 1 块为 1 倍，两块及以上等于块数（`field/FieldInfluence.java` 33–38）。
3. `TechniqueHitSpec.spawn` 198 行把帧伤害抄进 `settleDamage`。`suppressRootSettle` 为真时不抄，燎里走这条（`effects/LiaoliEffect.java` 65、125–126）。
4. 无账本：`TechniqueHitSettle.java` 175–187。尊重落点用 `classifyRespecting`，不绕过无敌帧，也不加领域。否则 `hurtRoot`，绕过无敌帧，并加一次领域。
5. 有账本：同文件 125–131 只记账。`FlowRoundLedger` 37–38、59–64 留下最后一份伤害，结算时乘份数。潮印按份累加。6 tick 后 `ReleaseBatch.Tally.flush`（`ReleaseBatch.java` 290、400–429）打出一刀。有持续接触载荷时先 `DomainAuraResolver.merge` 再 `hurtConfirmed`。无载荷且尊重落点时不加领域。其余走 `hurtRoot`。一分为三因此是同一份伤害乘 3，领域只加在合计上一次。

领域附加是相加、取最大的一份，不是倍率（`domain/DomainAuraResolver.java` 12–20、39–44）。雾雨和深压用潮印中性光环；幽冥光环伤害是 0（`DomainDefinition.java` 62–68、120–126、142–150）。`upkeepCost` 已废弃，不是扣款依据（79–84）。

固定火伤不走 `playerAttack`，也不加领域。`fire/FireDamage.java` 34–56 用模组火焰伤害类型，不改无敌帧。

- 燎原本体每拍 8，且 `furnaceSettled=false`（`firecast/GroundFireFields.java` 39、202）。这个 8 与 `Technique` 底伤 8 是两份常量。火种段是 `4×(1+0.1n+F)`，间隔 20，基础 3 段（`firecast/FireConsumerPlans.java` 30–38）。
- 焚天是 `2×(1.2^层 + F)`，间隔 4，最多 10 段（同文件 41–55）。半径 8 在效果里写死，只乘体积倍率（`effects/FentianEffect.java` 27、41–43）。
- 灼地首次接触打 1（`firecast/SurfaceFireHosts.java` 299）。
- 引燃段伤 2，每段要 10 火种（`enchant/resource/ResourceEnchantNumbers.java` 8–12；`fire/KindlePlan.java` 39–41）。
- 引爆是 `0.2 × 余额 × (1+炉心)`（`fire/DetonatePlan.java` 24、45–46）。
- 燎里物理是底伤 6 加迎面速度，`hurtRoot`，不读武器；火种另走引爆（`LiaoliEffect.java` 57、116–126）。
- 幽冥产种基数 5，周期 20（`ResourceEnchantNumbers.java` 13–14）。不打直接伤害。

剑实体打的是物品攻击力，不是术式底伤。`SwordEntityMotion.damageOf` 285–293 取主手加法攻击加锋利每级 1.25。主人在线走 `hurtRoot`（112 行），主人不在走 `magic()`（123 行）。幻剑生成时再乘场地倍率，并加上潮印条数乘 2（`sword/SwordEntity.java` 119–133）。因此幻剑底伤 2、万剑归宗底伤 8 只影响魔力和界面，不进入这一刀。

武器附魔记在主手 NBT，下一次普攻消费：`Attack + Added`，`playerAttack`，不加领域、不乘法杖（`enchant/WeaponEnchant.java` 150–157）。打中后没有资源附魔就删掉这笔（174–178）。

## 已经乘过的倍率

| 倍率 | 应用点 | 次数 |
| --- | --- | --- |
| 法杖属性加成 | 仅无附魔的 `segmentDamage` | 一次。两根法杖传入的都是 null（`item/ModItems.java` 25–28） |
| 武器 `max(攻击-1, 0)` | `SkillUsePacket.java` 240，再进段伤害或 `carriedDamage` | 一次。剑实体另算物品面板，不走这里 |
| 同属性 +30% | `ElementReaction.java` 43–45；`EnchantBonus.bump` 211–219 | 附魔路径一次。无附魔路径不加 |
| 附魔冷却 | 同属性 0.7，层数大于 1 时 0.4，金属再乘 0.9（`EnchantBonus.java` 181–187） | 只改段间隔（`SkillUsePacket.java` 818–820），不改收尾冷却 |
| 法杖冷却减免 | `scaleCooldown`：`round(ticks × (1-减免))`，至少 1（`SkillSegmentMath.java` 143–145） | 基础法杖 0.10，高阶 0.50 |
| 链聚能、自身能量、附魔半径 | 尺寸：`geometryEnergy` 再乘半径（`SkillChainPlan.java` 698–705；`CastFrame.volumeScale` 70–76；`TechniqueHitSpec.java` 203–221）。寿命：`ReleaseInput.scaleDuration` 向上取整（`ReleaseInput.java` 22–36；`TechniqueHitSpec.instanceLifetime` 477–482） | 各一次。不改伤害。`fixedLifetime` 或弧带不再缩寿命 |
| 速度附魔 | 仅当 `speedMultiplier > 1.001` 才乘 `spec.speed`（`TechniqueHitSpec.java` 236–237） | 小于 1 的减速不会生效。金生水是 1.10（`ElementReaction.java` 69–71） |
| 领域光环 | `hurtRoot`，或持续接触结算前的 `merge` | 见上，不是每条命中路径都有 |
| 三重份数 | 账本最后一份伤害乘份数 | 一次，不是每份各打一刀 |
| 魔力回复 | 每 20 tick 回复 5，再乘 `(1+法杖回复效率)`（`player/MagicPool.java` 21–22、221–227） | 与术式消耗无关 |

旧五附魔的 `magicDamage` 是 `ceil(2 × 伤害倍率)`，潮印固定返回 2（`EnchantmentDefinitions.java` 62–66）。资源附魔魔力每格 2，直接伤害 0，种火基数 10（`ResourceEnchantNumbers.java` 5–7）。同一种附魔效果只留一份，魔力仍按装配格相加（`ResourceEnchantCatalog.java` 62–68）。

## 冷却、射程、速度、持续

冷却只来自 `Technique.baseCooldown`。段间隔、收尾、批量计划和印记链都走 `scaleCooldown`（`SkillUsePacket.java` 466、507、1034、1208–1209；`BatchReleaseCompiler.java` 407–410、466）。释放定义没有冷却字段。

速度和寿命分成两类。

读释放定义的：奥术飞弹、热光束寿命、持续试射、火球、爆燃、金针、冲刺、垂丝寿命、燎原寿命、燎里寿命、水系形态（`WaterFormEffect.java` 45–46）。

效果里另写死、与登记重复的：三斩寿命 4 和速度 0；断潮 6；岩槌 10；寄生种子 160、三颗、半径 1.2；热光束长度 6、间隔 10、火种 1、速度 0；空间切割和让锋用效果内常量。改登记不会带动这些效果。

射程和体积多数在效果或领域工厂里：燎原半径 10（`GroundFireFields.BASE_RADIUS`），焚天 8，雾雨和幽冥 7×7×8，深压 7×7×3，水幕 3.2×2.8×0.25、衰减 0.5、生成魔力 8、维持 2/20 tick、寿命 80（`BarrierDefinition.java` 39–40），水环寿命 200、卸伤 0.20、半径 0.9、生成 8、维持 2/20（`GuardDefinition.java` 44–45）。落点射线距离在 `ReleaseBatch` 的锚点常量，不是术式字段。

## 客户端与保存

`TechniqueHitEntity` 同步的是外观：尺寸、模型倍率、寿命、施法者、弧快照（`TechniqueHitEntity.java` 58–100、209–229）。不同步伤害、速度、魔力、冷却。存档只写 `XuexiHitLifetime`（258–266）。客户端渲染只用同步来的模型倍率（`client/TechniqueHitRenderer.java` 131–136）。

会保存的是：剑的 `Damage`（`SwordEntity.java` 545、599）；武器附魔的 `Attack`、`Added` 等（`WeaponEnchant.java` 58、85、150–152）；玩家 `Magic`、`MagicMax`（`player/PlayerShuData.java` 319–323），扣魔力后用 `ShuSyncPacket` 同步当前魔力（`MagicPool.java` 81–83）。没有术式平衡表的网络包。装配界面伤害、冷却、魔力来自本地登记（`gui/SpellAssemblyScreen.java` 329–331）。

## 按术式：关键数和实际打出去的数

底伤会进入玩家攻击的：刺击 6、横劈 4、竖劈 8、冲刺 2、飞弹 5、试射 1、热光束 3、空间切割 4、种子 4、断潮 6、岩槌 8、金针 5、火球 5、爆燃 8、让锋 40、垂丝 2、缺月 7、陨岩 24、热波 4、火苗 1。魔力默认是底伤乘 2 向上取整，除非上面写了显式价。

底伤不进入那一刀的：幻剑 2、剑来 0、万剑归宗 8，命中用剑物品伤害。燎原 8 与火场常量 8 各写一处，火种段另算。灼地底伤 1，接触打的是另一处常量 1。焚天底伤 2 只作为公式起点，实际段伤在消费计划里重算。燎里 6 再加迎面速度。

零伤且不打直接伤害的：跳跃、点火、锁定、双重、垂落、落点、一分为三、领域绑定、聚能、三维交错、缠枝、潮印、沉锋、锐锋印、炉心三式、追踪、风暴、地表定位、喷涌、错时、命中接续、留焰、种火、引燃、引爆、幽冥、崩地、水幕、雾雨、水环、深压、水牢。它们的可调数是冷却、魔力、尺寸、寿命、卸伤或产种，不是底伤。

## 事实风险

- 同一含义常有两份常量。寿命、燎原 8、灼地 1、焚天半径都是登记一份、效果一份。
- 附魔路径不加领域的命中还有：尊重落点且无持续载荷、最早接触扫描（`TechniqueHitSettle.java` 246–247）。火伤路径从不加领域。
- 速度倍率只放大、不缩小。冷却附魔只改间隔，不改收尾冷却。
- 聚能只改尺寸和有限寿命，不改伤害和魔力。弧带寿命不吃聚能。
- 幻剑和万剑归宗的底伤、魔力、界面与真实伤害脱节。
- 法杖属性加成字段存在，当前两根法杖都是 null，等于没乘。
- 没有全局伤害倍率。实体数量倍率是调试用的生成份数，不是伤害倍率。

## 可按文件拆开的作者边界

这些文件今天各自拥有数字或消费点，后续改一处不会自动改到另一处。

1. 术式底伤和冷却：`shu/ModTechniques.java`、`shu/WaterTechniques.java`、`shu/Technique.java`。
2. 寿命、速度、厚度、显式魔力、聚能：`shu/technique/release/ReleaseDefinitions.java`、`batch/ListedRelease.java`、`WeepingSettings.java`、`selfstatus/SelfStatusDefinition.java`、`WaterReleaseDefinition.java`。
3. 领域、水幕、水环几何和费用：`domain/DomainDefinition.java`、`barrier/BarrierDefinition.java`、`guard/GuardDefinition.java`。
4. 报价、冷却合成、段伤害分支：`network/SkillSegmentMath.java`、`SkillUsePacket.java`、`BatchReleaseCompiler.java`、`SkillChainPlan.java`、`SkillSegmentRunner.java`。
5. 附魔数值：`shu/technique/enchant/EnchantBonus.java`、`EnchantmentDefinitions.java`、`shu/ElementReaction.java`、`enchant/resource/ResourceEnchantNumbers.java`。
6. 命中抄表和账本：`release/entity/TechniqueHitSpec.java`、`TechniqueHitSettle.java`、`release/TechniqueHits.java`、`ReleaseBatch.java`、`FlowRoundLedger.java`、`domain/DomainAuraResolver.java`。
7. 效果内写死的寿命、长度、间隔：三斩、断潮、岩槌、寄生种子、空间切割、让锋、热光束、爆燃效果。
8. 火伤和持续段：`fire/FireDamage.java`、`firecast/FireConsumerPlans.java`、`GroundFireFields.java`、`SurfaceFireHosts.java`、`fire/KindlePlan.java`、`DetonatePlan.java`、`effects/FentianEffect.java`、`LiaoyuanEffect.java`、`LiaoliEffect.java`。
9. 剑和普攻附魔：`sword/SwordEntity.java`、`SwordEntityMotion.java`、`enchant/WeaponEnchant.java`。
10. 魔力池和法杖全局：`player/MagicPool.java`、`PlayerShuData.java`、`item/StaffData.java`、`item/ModItems.java`。
11. 只读本地登记的界面：`gui/SpellAssemblyScreen.java`、`gui/ImprintTreeView.java`。客户端不需要为伤害再读包，除非以后平衡表要跟服务器走。
