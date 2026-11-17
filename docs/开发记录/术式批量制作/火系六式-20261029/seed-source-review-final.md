我先读这份任务说明，确认授权范围后再按它完成审查并交完整报告。任务是实现累计第 3 轮的只读复核。我先读流程和作者交回的材料，再沿实际调用链核对火种来源。作者报告和需求快照已对齐。接下来核对源码哈希，并沿伤害成功路径检查点火、热光束和火球的挂种。点火会自己写入 `added = 2`。我接着核对普通武器是否只在这份额外伤害成功时才挂种，以及光束和火球是否真的走进结算。**通过。** 实现累计第 3 轮只读核对没有发现可定位的生产缺陷。18 个源码文件的 SHA256 与修改时间与作者最终证据一致。本轮不追加复审，也不改代码。

点火额外 +2、热光束每次接受的命中 +1、火球每个实际目标 +3，可相加为 3 和 5。点数固定，生产倍率 1，不随伤害、炉心或聚能变化。100 上限、死亡清零和消费者的一次生产/消费契约没有改。`MagicIgniteEffect` 的视线盒子不产种。`SkillUsePacket` 和 `release/fire` 没有按这三项 id 分支。

## 核对结论

1. **数值进了真实受伤路径。** `HeatBeamEffect` 第 36 行写 1，`FireOrbEffect` 第 43 行写 3。`EnchantBonus.resolve` 第 193 行有火效果就写 2，`EnchantmentSnapshot.read` 第 112 行同样写 2。`SkillSegmentRunner` 第 534 行放进 `CastFrame.seedPoints`。`TechniqueHitSpec` 第 162 与 192 行把术式点数和附魔点数加进 `settleSeedPoints`。`TechniqueHitEntity` 第 424 行抄到实体上。无账本时，`TechniqueHitSettle` 在 `hurtRoot` 或持续流 `hurtConfirmed` 为真之后才 `deposit`。有账本时，`ReleaseBatch.Tally` 先按目标加轮次记下第一份正点数，伤害被接受后第 291 行只挂一次。

2. **仅点火的普通武器可以 +2。** 点火会把自己的附加伤害写成至少 2，但挂种不看这项。印记门槛包含 `seedPoints`。无潮印时，`WeaponEnchant.onAttack` 用印记里的攻击力加附加伤害去 `hurt`，只有这一下返回真才按 `SeedPoints` 挂 2，不要求另外再有木、土、金附魔。有潮印时 `onAttack` 直接返回，`SwingTide.finish` 在原版那一刀被 `TideMarkDamageScope` 接受后挂一次。接触宿主在 `ContactEnchantState` 原有 `hurt` 已成功且目标仍存活时挂一次。点火的接触伤害本身至少为 2，所以仅点火也能打开接触。

3. **热光束后续命中不会被同一本账吞掉。** 间隔仍是 10 tick，`persistentFlowSettlement` 仍是关。单独释放没有 `Tally`，每次 `hurtRoot` 成功后新建一本 `FireCastContext`。一分为三才共用 `Tally`：`FlowRoundLedger.FLUSH_DELAY` 是 6，小于 10，下一轮是新的 `uuid#轮次`。`putIfAbsent` 不按复制份数相加。`grantedSeeds` 只挡住同一轮。消费者仍用 `ReleaseBatch.fireContext()`，燎原、燎里、焚天没有改到这本账。

4. **火球仍是直线、穿透、每目标 `hitOnce`。** 默认间隔 0，所以走 `hitOnce` 而不是 10 tick 门。命中不销毁、不改成爆炸。每个新目标在伤害被接受后 +3；带点火则 3+2 一次挂 5。

5. **空挥、拒绝、自己、旁观、无敌、客户端、已死不挂。** `FireSeedGrant.deposit` 先过 `DamagePermission`，再走 `FireSeedState.grant`；后者要求服务端主线程且目标存活。客户端 `tick` 在命中前返回。没有 `LivingHurtEvent` 产种。`setSecondsOnFire`、火球监听和载荷 `apply` 都不产种。伤害调用没发生或返回假时不挂。杀死目标后实体已死，这一下不加火种，与「死亡不产种」一致。

6. **同一次结算只加一次。** 无账本与有账本互斥。三份复制在同一轮只留第一份正点数。武器无潮印和有潮印不会两边都挂。剑在自己的 `hurtRoot` 成功后挂帧上的点火点数，不进命中账本。旧档没有 `SeedPoints` 时 `getInt` 为 0。

语言键 `technique.xueximod.heat_beam`、`fire_orb`、`magic_ignite` 及各自 `.desc` 都保留了原有行为，并写上 1、3、2。热光束视觉号仍是 2，火球仍是 4。已完成页缺历史详细快照只约束表迁移，不是这次源码缺陷。

## 源码哈希

与 `seed-source-implementation-evidence.json` 的 18 条 SHA256 和 mtime 全部一致，包括 `FireSeedGrant.java` `6B2BC42A…`、`WeaponEnchant.java` `6251AE5D…`、`ReleaseBatch.java` `382015A6…`、`HeatBeamEffect.java` `7F340381…`、`FireOrbEffect.java` `197E580E…`。

## 未验证

未跑 Gradle，未进游戏，未重跑名称门禁。没有语言文件基线，不能证明其它键未被删改；已核对的三条描述是完整的。命中体的 `settleSeedPoints` 不进自己的存档，和原本就不保存的 `settleDamage` 一样，只活在这一次生成里。`hurt()` 返回真之后，护甲或事件把最终伤害收成 0 时会不会仍被当成接受，没有打开 Forge 的 `LivingEntity.hurt`，不能判定；现有非持续流结算用的就是这个返回值。

本轮只读，没有写源码，到此结束。
