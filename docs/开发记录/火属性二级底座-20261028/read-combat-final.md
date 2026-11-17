我先读这份任务说明，按它写明的授权范围执行。任务是只读采集：施法 token、命中结算、伤害类型和友敌关系。我按这五条去定位相关类，不改文件。本地 Forge 映射包里有 `LivingEntity.hurt` 的护甲和抗性分支，我对照字节码确认无敌帧和标签，不改任何文件。同一次施法里，三份复制共用一枚父令牌和一个结算账本；真正打出血的接点只有 `TechniqueHits` 的两条 `hurt`。源码里没有蒸汽、没有冰，也没有火属性伤害类型。

## 1. 跨三份的令牌、命中成功、载荷

一次按键在 `SkillUsePacket.executeSkill` 里 `ReleaseBatch.open`（约 351 行），整段链共用这个 batch，`finally` 里 `unbind`。一分为三在 `SkillChainPlan` 里 `copies *= 3`（约 137 行）；`copies > 1` 时只 `openGroupTally()` 一次（约 217、271、427 行），三份写进同一个 `ReleaseBatch.Tally`。

稳定编号是 `CastToken.open()` 的随机 UUID（`CastToken` 13–18 行），由 `HostCast.open` 持有（`HostCast` 36–39、65–70 行）。`HostGroupIds.castHost(castToken, memberIndex, volley)`（18–24 行）只混成员和波次，注释写明复制份不进编号，所以三份共用一个宿主组号。`sessionHost` 用接触组 UUID 加成员盐。每个会提交宿主组的链段会自己 `HostCast.open`（`SkillUsePacket` 约 751 行，`SkillSegmentRunner.commitLooseHost` 约 628 行），不是整条链一枚令牌。`CastFrame.releaseGroup`（54 行）是重水会话组，不是这枚父令牌。

命中入口是 `TechniqueHitEntity.tick` → `TechniqueHitSettle.tryHit`（约 303–309、34 行）。`canHit`（24–31 行）只要求活着、非旁观、不是施法者。`hitInterval <= 0` 时每个目标只进一次；大于 0 时按该命中体、该目标的 `lastHitTick` 间隔放行。

真正成功：

- 非持续接触、有 tally：先 `record`，约 6 tick 后 `Tally.flush`（`FlowRoundLedger.FLUSH_DELAY = 6`）里 `TechniqueHits.hurtRoot`。
- 没有 tally：当场 `hurtRoot`（`TechniqueHitSettle` 136 行）。
- `persistentFlow`（目前只有 `water_weeping_arc`）：`hurtConfirmed`，且只有返回真才 `FlowSuccessPayload.apply`（142–160 行）。
- `TideMarkDamageScope.finish`（54–84 行）才算确认：`hurt` 为真，且 `LivingDamageEvent` 未取消、最终伤害大于 0。水牢来源不进这个观察（`ReleaseBaseEvents.onDamage` 118–120 行）。

载荷没有插件表。`TechniqueHitSpec.spawnReported` 在实体进世界前 `HitPayloads.attach(entity, frame)`（约 294 行），按实体 UUID 记下治疗、牵引、潮湿、灭火、上抛。持续接触不在 `tryHit` 里立刻 `apply`，而是 `HitPayloads.capture` 再 `withStrike` 收成不可变的 `FlowSuccessPayload`；同目标同轮合并时强度取较大值，击退方向留第一次。`WetPressure.attach` 仍被调用，方法体是空的。

## 2. 属性、伤害类型、hurt、归属

元素只有 `ShuAttribute`：`none/wood/fire/water/earth/metal`，没有冰。同属性是 `ElementReaction.Kind.SAME`，伤害倍率 1.30。这不是 `DamageType`。

自定义伤害类型只有 `xueximod:water_prison_pressure`（`PrisonPressure.TYPE`，18–20 行；json 的 `scaling` 为 `never`，`exhaustion` 0）。它被放进 `bypasses_armor`、`bypasses_resistance`、`bypasses_effects`、`bypasses_enchantments`、`always_hurts_ender_dragons`。没有 `bypasses_invulnerability` 或冷却类 tag 文件。

普通术式伤害全是 `damageSources().playerAttack(player)`（`TechniqueHits` 47、53、81 行），来源实体和直接实体都是玩家。剑的主人不在线时改走 `damageSources().magic()`，没有玩家（`SwordEntityMotion` 97 行）。水牢 `new DamageSource(holder)`，没有攻击者（`PrisonPressure` 49–51 行）。潮湿四级另用 `damageSources().drown()` 打 1 点（`WetnessSensitivity` 19 行）。

事件：`LivingHurtEvent` 高优先级在 `ReleaseBaseEvents.onHurt`（44 行），水牢直接返回；箭可按屏障比例减；玩家再吃场地或格挡卸伤。`LivingDamageEvent` 最低优先级记实际掉血和水势；水牢不记潮印。`WetAttackReward` 只认玩家近战，或有支撑的箭；溺水不加。

## 3. 每 4 tick 独立结算，且不改无敌帧

现在没有这种结算。

`TechniqueHits.hurt` / `hurtRoot` 会把该目标的 `invulnerableTime` 临时写成 0，打完再写成 `max(原值, 打完后的值)`；原值大于 10 时还抬高 `lastHurt`（46–58 行）。场地脉冲（`FieldZones` 240、308 行，每 10 tick）、吸取（`LifeDrain` 用 `ChainScheduler`，间隔由调用方传入）都走这条。

`hurtConfirmed` 只接受 `RESPECT_INVULNERABILITY`，不写这两个字段（71–85 行）。它仍是 `playerAttack`。项目测试要求这条路径里不能出现 `invulnerableTime = 0`。原版在无敌帧后半段会拒绝不高于上次的伤害，更高伤害只结算差额。所以每 4 tick 用它打同额，不能每次都独立全额落下。

命中体的 `hitInterval` 只决定何时允许 `record`。热光束和垂丝都是 10，光束体积也写死 10（`HitVolumes` 181 行）。tally 还要把一轮收成一次，再延迟 6 tick 打出，不是每 4 tick 一次独立 `hurt`。水牢每 20 tick 调用 `target.hurt`，不改无敌帧，来源也没有攻击者（`PrisonPressure` 16–22、33–43 行）。

`ChainScheduler.schedule(server, runAtTick, runnable)` 可以按绝对 tick 排程，但它不提供新的伤害类型，也不绕过上面的无敌帧政策。

不要把火伤或新类型放进水牢那四个绕过 tag。那会让护甲、抗性、效果和保护附魔一起失效。模组没有 mixin 改护甲吸收或魔法吸收。

## 4. 同层加算、护甲抗性、固定伤、击退、友敌

同层加算在 `EnchantBonus.resolve`：火、木、土、金同层时 `bump`，加 `max(原值×0.3, 1)` 到附加伤和攻击伤（87–99、186–194 行）。水同层只抬攻击伤；潮印每条固定 +2，不进附加伤，也不吃这 30%（27、45–51 行）。无附魔时伤害是 `scaledBase + 武器 + magicDamage`（`SkillSegmentMath` 107–116、158–169 行）；有附魔时改用 `carriedDamage`，不再加 `magicDamage`。`magicDamage` 除潮印恒为 2 外，是 `ceil(2 × 同属性 1.30)`。领域只加一份最大光环（`DomainAuraResolver.merge` 12–20 行）。场地份数在施放时乘到伤害上（`FieldInfluence.damageScale` 33–38 行；两块才是 2 倍）。账本里同目标同轮同复制序号只收一次，伤害留最后一份再乘份数（`FlowRoundLedger` 32–64 行）。

护甲看 `BYPASSES_ARMOR`，抗性效果看 `BYPASSES_RESISTANCE`，保护附魔看 `BYPASSES_ENCHANTMENTS`；`BYPASSES_EFFECTS` 会整段跳过魔法吸收。这些都是原版 `hurt` 之后的吸收，模组只通过 tag 影响水牢。火抗性只吃带火标签的来源；`playerAttack` 不带这个标签。点燃是 `setSecondsOnFire`，和伤害类型分开。

没有蒸汽路径，也没有蒸汽用的固定基础伤害。现成固定数额是水牢 4、潮印每条 2、垂丝底伤 2（`WeepingSettings.BASE_DAMAGE`）、溺水 1。

击退是 `LivingEntity.knockback(强度, x, z)`。定义值在 `ReleaseDefinition.knockbackStrength`（默认 0），施放时和附魔击退取较大值（`SkillSegmentRunner` 536、549–550 行）。持续接触记在 `FlowSuccessPayload`，确认正伤后才施加。水牢里的生物由 `PrisonLivingMixin` 在 `knockback` 开头取消。

友敌：`Relations.ally` 恒为 false，`permits` 只认主人自己（12–18 行）。`RelationDecision` / `RelationPolicy.hostileCapture` 只给水牢捕获用，命中体不过这套过滤。

## 5. 火包和现有登记

建议新火属性底层只放在 `com.bslx.XueXiMod.shu.technique.release.fire`。登记方式对齐 `WaterTechniques.register`，由 `ModTechniques.registerAll` 末尾调用（约 264 行）。整合者要改的既有文件主要是 `ModTechniques.java`。若新增伤害类型，再加 `data/xueximod/damage_type` 和对应 tag，不要照抄水牢的四个绕过 tag；若新来源不能进水势记账，再改 `ReleaseBaseEvents`。单术式不必改 `SkillUsePacket`。

当前没有 `release.fire` 包。已注册且带 `ShuAttribute.FIRE` 的只有旧的 `heat_beam`、`magic_ignite`、`fire_orb`、`fire_burst`（`ModTechniques` 78–86、164–170 行；`ReleaseDefinitions` 13、19–20 行）。没有另一条新的火术式。
