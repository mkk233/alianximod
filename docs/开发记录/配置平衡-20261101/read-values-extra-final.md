我先读取这份补充任务说明，按它授权的范围补齐事实并给出完整报告。补充两点：各条最终 `hurt` 是否带着术式 ID，以及全部已注册术式的稳定 ID 和效果文件。我按调用链把 ID 在哪一步还在、哪一步丢掉核对清楚。这两条最终 `hurt` 都不带术式 ID。ID 在进入 `DamageSource` 之前就丢掉了。下面按调用顺序写明还在哪、在哪一步没了。已注册术式共 62 个，56 个在 `ModTechniques.registerAll`，6 个在 `WaterTechniques.register`。

## 1. 最终 hurt 与术式 ID

### 水牢压强

`water_prison` 只存在于术式登记和条件效果 `WaterPrisonEffect`。提交监牢时，`PrisonRuntime` 106–116 行构造的 `PrisonView` 只有监牢 UUID、施法者 UUID、目标 UUID、维度、盒子、层数和时刻。`hostId()`（640–641 行）返回的是监牢 UUID，不是 `water_prison`。

压强有两处调用，都只把生物传进去：

- 四层及以上，提交当刻：`PrisonRuntime.java` 129–130
- 之后每 20 tick：同文件 535–537

`PrisonPressure.pulse`（33–43 行）用伤害类型 `xueximod:water_prison_pressure` 造出一个没有攻击者的 `DamageSource`（49–51 行），再 `target.hurt(source, 4)`。末影龙打在头上。不绕过无敌帧。元素帧、施法者、术式 ID 都没有。ID 在构造 `PrisonView` 时就没抄进去，`pulse` 更收不到。

### 潮印并进原版普攻

`tide_mark` 在 `EnchantBonus.resolve`（141–147 行）里只把效果键 `water` 收成条数。术式 ID 没有写入 `EnchantBonus`。

记到武器时，`WeaponEnchant.imprint`（57–74 行）写入 `Tide` 整数、`Added` 等。`ResourceIds` 只存资源附魔的 ID。潮印没有字符串 ID。

原版主目标那一刀在 `PlayerAttackMixin` 30–38 行被改金额：`includeTideFlat`（`WeaponEnchant.java` 110–115、98–102）加上 `Tide * 2 + Added`，不含 NBT 里的 `Attack`。随后 43–51 行仍是原版 `playerAttack` 的 `DamageSource`。`captureSwing`（118–131 行）只把条数和施法者 UUID 放进 `TideMarkDamageScope`，用来在伤害成立后给潮湿和水势，不改伤害来源。

`WeaponEnchant.onAttack`（147–148 行）见到 `Tide > 0` 就返回，所以不会再打第二刀。

ID 丢失点是 `EnchantBonus.resolve`：之后 NBT 和 `hurt` 都只有条数。命中体上的潮印同样只有 `TideMarkSuccess(条数, 施法者)`，`hurtRoot` 也不接收术式 ID。

### 燎里

`LiaoliEffect` 59 行把 `fire_liaoli` 写进命中实体的 `techniqueId`。这只供实体自己识别，不进入伤害。

一次命中两下 `hurt`：

1. 火种引爆：`LiaoliEffect.java` 123 行调用 `FireConsumption.detonate`，计划是 `FireConsumerPlans.thrust`（81–86 行，基数等于花掉的火种）。`FireConsumption.java` 124 行调用 `FireDamage.strikeOnce(目标, 施法者UUID, 伤害, 火基数, 炉心已结算)`。`FireDamage` 66–73 行的来源是火焰伤害类型加施法者。`ElementDamageScope.Frame`（173–184 行）只有火、水、冰、施法者、火基数、炉心标记。没有术式 ID，也没有效果键。
2. 物理刺击：`LiaoliEffect.java` 124–126 行用底伤加迎面速度调用 `TechniqueHits.hurtRoot`，来源是 `playerAttack`。没有把实体上的 `fire_liaoli` 传进去。

两步都在调用 `hurt` 时丢掉 ID。实体上的字符串还在，但伤害载荷没有。

### 资源附魔

效果键等于术式 ID，写在 `ResourceEffectKeys.java` 5–10 行：`fire_flame_trail`、`fire_seed_sow`、`fire_kindle`、`fire_detonate`、`fire_nether_gloom`、`earth_break_ground`。它们留在载荷、去重和燃烧账上，不留在 `hurt` 上。

引燃和燎原、焚天的延迟段：键存在 `BurnTrackState.effectKey`，并写入目标 NBT 的 `effect`（`BurnCarrier.java` 129 行）。真正打出时，`BurnCarrier.tick` 35–36 行和 `settleOpening` 46–47 行只把 `pulse.damage()` 和 `pulse.casterId()` 交给 `FireDamage.strikeOnce`，火基数传 0。`BurnPulse.effectKey` 在这个 lambda 丢掉。

引爆：`ResourceHitCommit.commit`（46–47 行）调用 `DetonatePlan.strike`。38 行用键 `fire_detonate` 做去重。46 行的 `strikeOnce` 同样只剩施法者、伤害和火基数。

武器印记：`ResourceIds` 一直保存到普攻结算（`WeaponEnchant.java` 67–74、193–211、277–288 行）。`commitResource` 用这些 ID 重建载荷后再走上面的 `strikeOnce`，ID 在这一步离开伤害来源。原版那一刀本身仍是 `playerAttack`，不含这些 ID。

陨岩、火苗、热波的直接命中是 `TechniqueHits.classifyRespecting(目标, 主人, 伤害, 潮印)`（`BatchFlightBook.java` 273 行，`BatchShockRingEffect.java` 94 行）。`techniqueKey` 只留在飞行记录里做分支，不进这次 `hurt`。

## 2. 62 个术式

一行一个稳定 ID。效果路径相对 `src/main/java/com/bslx/XueXiMod/`。数值只写真正被消费的类型。

1. `sword_thrust` — `shu/technique/effects/SwordThrustEffect.java` — 底伤、冷却、默认魔力、效果内寿命与斩击尺寸；伤害经帧进 `playerAttack`。
2. `sword_horizontal_slash` — `shu/technique/effects/SwordHorizontalSlashEffect.java` — 同上。
3. `sword_vertical_slash` — `shu/technique/effects/SwordVerticalSlashEffect.java` — 同上。
4. `movement_dash` — `shu/technique/effects/MovementDashEffect.java` — 底伤、冷却、定义速度与寿命；碰撞走 `hurtRoot`。
5. `movement_leap` — `shu/technique/effects/MovementLeapEffect.java` — 冷却。效果只加固定向上速度，不读底伤，无 `hurt`。
6. `magic_arcane_missile` — `shu/technique/effects/MagicArcaneMissileEffect.java` — 底伤、冷却、定义速度与寿命。
7. `magic_ignite` — `shu/technique/effects/MagicIgniteEffect.java` — 附魔魔力、冷却、火种点数。单独释放只点燃固定秒数，无 `hurt`。挂在攻击上时火种进帧。
8. `magic_create_water` — `shu/technique/effects/MagicCreateWaterEffect.java` — 冷却。底伤 0，无伤害。
9. `test_sustained` — `shu/technique/effects/TestSustainedEffect.java` — 底伤、冷却、定义速度与寿命。
10. `heat_beam` — `shu/technique/effects/HeatBeamEffect.java` — 底伤、冷却、定义寿命；效果内长度、命中间隔、火种。
11. `universal_lock_target` — `shu/technique/effects/UniversalLockTargetEffect.java` — 冷却。索敌距离在锁定服务，无伤害。
12. `rule_dual_release` — 登记处空效果（`shu/ModTechniques.java` 352）。消费在 `BatchReleaseCompiler` / `CompiledReleaseStep`：固定两份、冷却、段间隔。无伤害。
13. `trajectory_drop` — 空效果（`ModTechniques.java` 353）。只改后续弹道为垂落，加冷却。
14. `space_cut` — `shu/technique/effects/SpaceCutEffect.java` — 底伤、冷却；寿命和静止贴图在效果内。伤害经帧。
15. `rule_cast_anchor` — `shu/technique/effects/CastAnchorEffect.java` — 无冷却魔力。落点射线在 `ReleaseBatch.resolveAnchor`。
16. `rule_triple_cast` — `shu/technique/effects/TripleCastEffect.java` — 复制份数 3。伤害在账本按份相乘，本身无底伤。
17. `rule_domain_bind` — 空效果（`ModTechniques.java` 357）。绑定周期在链计划，无伤害。
18. `rule_gather_energy` — 空效果（`ModTechniques.java` 358）。链聚能只放大尺寸和有限寿命。
19. `trajectory_3d_interleave` — `shu/technique/effects/Scatter3dEffect.java` — 散布半径在 `CastFrame` 斩击几何，无伤害。
20. `vine_mark` — 无效果。`EnchantmentDefinitions` 与 `EnchantBonus` 的 `wood`：附魔魔力、冷却、减速时长、附加伤。
21. `parasite_seed` — `shu/technique/effects/ParasiteSeedEffect.java` — 底伤、冷却；颗数、散布和寿命在效果内。
22. `tide_mark` — 无效果。`EnchantBonus` 的 `water` 加 `WeaponEnchant`：魔力、冷却、条数。并进普攻的是条数乘 2。
23. `tide_sever` — `shu/technique/effects/TideSeverEffect.java` — 底伤、冷却、效果内寿命。
24. `sinking_edge` — 无效果。`EnchantBonus` 的 `earth`：魔力、冷却、半径、击退、附加伤。
25. `stone_hammer` — `shu/technique/effects/StoneHammerEffect.java` — 底伤、冷却、效果内寿命。
26. `keen_mark` — 无效果。`EnchantBonus` 的 `metal`：魔力、冷却、冷却倍率、半径、附加伤。
27. `metal_needle` — `shu/technique/effects/MetalNeedleEffect.java` — 底伤、冷却、定义速度与寿命。
28. `fire_orb` — `shu/technique/effects/FireOrbEffect.java` — 底伤、冷却、定义速度与寿命、火种。
29. `fire_burst` — `shu/technique/effects/FireBurstEffect.java` — 底伤、冷却、定义速度与寿命；爆炸半径在效果内。
30. `fire_heat_up` — 空效果（`ModTechniques.java` 367）。显式魔力在 `SelfStatusDefinition`，冷却在术式，动作是进入普通炉心。无 `hurt`。
31. `fire_burn` — 空效果（368 行）。显式魔力、冷却、进入超频炉心。无 `hurt`。
32. `fire_cool` — 空效果（369 行）。魔力 0、冷却 0、主动冷却。蒸汽伤害在 `SteamBurst`，不读这个术式 ID。
33. `fire_liaoyuan` — `shu/technique/effects/LiaoyuanEffect.java` — 显式魔力、装配消耗、定义寿命、火场半径。本体伤在 `GroundFireFields`，火种段在 `FireConsumerPlans.spreadSpec`，都走 `FireDamage`。
34. `fire_scorch_field` — `shu/technique/effects/ScorchEffect.java` — 冷却、定义寿命。接触伤在 `SurfaceFireHosts`，走 `FireDamage`。底伤只进附魔解析。
35. `fire_liaoli` — `shu/technique/effects/LiaoliEffect.java` — 显式魔力、定义寿命、底伤加迎面速度走 `hurtRoot`；火种走 `FireDamage`。
36. `fire_fentian` — `shu/technique/effects/FentianEffect.java` — 显式魔力、效果内半径。段伤在 `FireConsumerPlans.skySpec`，经燃烧账进 `FireDamage`。
37. `trajectory_tracking` — 空效果（374 行）。只开追踪标记，无伤害。
38. `trajectory_storm` — 空效果（375 行）。把可变弹道改成旋涡，无伤害。
39. `water_yielding_ribbon` — `shu/technique/effects/WaterYieldingEffect.java` — 底伤经帧、冷却；寿命、延迟和斩击尺寸在效果内。水面本身不命中。
40. `water_weeping_arc` — `shu/technique/effects/WeepingCurtainEffect.java` — `WeepingSettings` 的底伤、潮湿、寿命、间隔、显式魔力，加定义寿命。
41. `fire_meteorite` — `carrier/BatchMeteoriteEffect.java`，结算在 `carrier/BatchFlightBook.java` — 底伤或载体回退伤、定义寿命与速度、中心半径和高度。`classifyRespecting`。
42. `fire_shock_ring` — `carrier/BatchShockRingEffect.java` — 底伤或环伤害、寿命、内外半径和高度。`classifyRespecting`。
43. `fire_spark` — `carrier/BatchSparkEffect.java`，结算在 `BatchFlightBook.java` — 底伤或火苗伤害、速度、尺寸、寿命、着火时长；产种基数在释放登记。
44. `rule_surface_anchor` — `carrier/BatchSurfaceAnchorEffect.java`。标志在 `BatchReleaseCompiler` 111–112 行。无伤害，冷却 0。失败则整组不放。
45. `fire_upwell` — `carrier/BatchRulePassEffect.java`。路径标志在 `BatchReleaseCompiler` 120–122 行。无伤害，冷却 0。效果只把下一段交回去。
46. `rule_stagger` — 同一个 `BatchRulePassEffect.java`。延迟是 `BatchReleasePlan.STAGGER_GAP` 乘份序号（`BatchReleasePlan.java` 11，`BatchReleaseCompiler.java` 421）。无伤害。
47. `rule_hit_continue` — 同一个 `BatchRulePassEffect.java`。接续结构在 `BatchReleaseCompiler` 48 行。无伤害，冷却 0。
48. `fire_flame_trail` — 效果是空转的 `BatchRulePassEffect.java`。实际消费是附魔魔力、冷却，以及载荷上的轨迹标记。无直接 `hurt`。
49. `fire_seed_sow` — 同上空转效果。消费魔力、冷却、产种基数。无直接 `hurt`。
50. `fire_kindle` — 同上空转效果。`hurt` 在 `BurnCarrier` 到 `FireDamage`。消费魔力与引燃段参数。
51. `fire_detonate` — 同上空转效果。`hurt` 在 `DetonatePlan` 到 `FireDamage`。消费魔力与引爆系数。
52. `fire_nether_gloom` — `shu/technique/release/batch/GloomDomainEffect.java` — 附魔魔力、领域寿命与盒子、产种基数和周期。光环伤害为 0。
53. `earth_break_ground` — `BatchRulePassEffect.java` 空转。消费魔力、冷却；挖地在载体结算，无直接 `hurt`。
54. `phantom_sword` — `shu/technique/effects/PhantomSwordEffect.java` — 冷却、定义寿命与速度。命中用剑物品伤害，经 `SwordEntityMotion.hurtAlong`，不用底伤。
55. `sword_come` — `shu/technique/effects/SwordComeEffect.java` — 冷却、效果内搜索盒。底伤 0。召回途中的伤害仍是剑上已有的物品伤害。
56. `sword_return_all` — `shu/technique/effects/SwordReturnAllEffect.java` — 冷却。底伤只进魔力。飞出后仍用剑物品伤害。
57. `water_blade` — `shu/technique/effects/WaterFormEffect.java` — 底伤、冷却、定义速度、寿命、厚度、能量和潮湿。
58. `water_curtain` — 同一个 `WaterFormEffect.java`，参数在 `BarrierDefinition.curtain` — 冷却、生成魔力、维持魔力、寿命、尺寸、弹速衰减。
59. `water_mist` — 同一个 `WaterFormEffect.java`，参数在 `DomainDefinition.mistRain` — 冷却、领域初始魔力、寿命、盒子、原生水势策略。
60. `water_ring` — 同一个 `WaterFormEffect.java`，参数在 `GuardDefinition.personalRing` — 冷却、生成与维持魔力、寿命、卸伤比例、半径。
61. `water_deep_pressure` — 同一个 `WaterFormEffect.java`，参数在 `DomainDefinition.deepPressure` — 同雾雨，另加减速和耗氧。光环不是直接 `hurt`。
62. `water_prison` — 无 `TechniqueEffect`。条件效果 `shu/technique/release/conditional/WaterPrisonEffect.java`，运行在 `shu/technique/release/prison/PrisonRuntime.java` — 冷却、每座成功的单价、监牢时长、四层及以上的压强数额和间隔。
