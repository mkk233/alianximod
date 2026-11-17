我先读完整份任务说明，再按其中授权范围执行。任务是只读采集领域现状。我按说明定向查注册、生命周期和相关文档。当前只注册了两个领域，都走 Domain 底座。雾雨是 `water_mist`，深海重压是 `water_deep_pressure`。二者都已限时，并且现在仍按周期收维护魔力。领域包里没有绑定术式、触发条件或按次放垂丝的实现。同名替换、单目标、缺魔跳过但不消域，代码里都没有。

## 1. 注册、费用、寿命、复制

`WaterTechniques.register`（`WaterTechniques.java` 21–38 行）登记这两个领域。雾雨冷却 80、复杂度 10、基础伤害 0，说明是「固定的雨雾领域。完整进入的敌人出不去，并会变潮湿」。深海重压同样是冷却 80、复杂度 10、伤害 0。工厂在 77–83 行写成 `TechniqueType.MAGIC`，并注册 `WaterReleaseDefinition.domain`。`WaterReleaseDefinition.role`（321 行）固定返回 `ChainRole.ATTACK`。`ReleaseDefinitions.registerAll` 不登记领域；`ReleaseDefinition.domain`（184–186 行）默认是空。全仓只有这两处 `DomainDefinition` 工厂。

`WaterReleaseDefinition.domain`（104–110 行）把显式魔力写成 0，寿命和 `fieldLife` 抄成领域寿命，并把维护费抄进场地的 `upkeepCost`。领域的 `fieldRadius` 保持默认 0，所以不会进 `FieldSpawner`。`Technique.manaCost`（108–109 行）对外仍显示 `initialCost`。真正扣生成费的是 `DomainService`，或双重释放那条已经预扣的账单。

`DomainDefinition.mistRain`（40–46 行）：脚底轴对齐盒，宽 7、深 7、高 8（`AxisBoxGeometry.atFeet` 16–21 行，水平各半宽）；生成费 40，维护费 8，周期 20 tick，寿命 200 tick；`learnsWhenReady` 为真；边界和视觉用构造默认值 `DomainBoundary.SEAL`、`DomainVisual.VOLUME`（19–22 行）。原生策略 `DomainNativePolicy`（18–21 行）的效果键是 `mist_rain`：潮湿周期 10、量 2、延迟 10；水势周期 20、量 2、延迟 20。没有减速，呼吸倍率是构造默认 0。

`DomainDefinition.deepPressure`（89–96 行）：同样脚底锚点，宽 7、深 7、高 3；费用和寿命与雾雨相同，40 / 8 / 20 / 200。原生键 `deep_pressure`：潮湿 10 tick、量 4、延迟 10；水势周期 20 但量为 0；减速放大器 0、持续 20、周期 10、延迟 10；呼吸倍率 2。边界 `OPEN`，视觉 `PRESSURE`。

几何在生成时写入世界坐标，之后不跟施法者（`AxisBoxGeometry` 类注释）。链聚能会同时放大盒子和寿命：`DomainService.frozenLife`（161–166 行）走 `ReleaseInput.scaleDuration`（`ReleaseInput.java` 22–36 行，正寿命向上取整）；`enlarge`（179–184 行）按同一倍率绕地面中心放大。接触点释放时，`placedGeometry`（170–176 行）先把整盒平移到接触点。

生成费按真实份数乘一次。`releaseWave`（71、85、91–97 行）里 `count = max(1, copies)`，账单是 `initialCost * count`，每一份各建一个 `DomainInstance`。一分为三的份数来自 `SkillChainPlan.copiesBefore`（495–506 行，遇 `RULE_TRIPLE` 乘 3）。同一波循环里多次 `place(player)`，玩家未移动时盒子重合。这些副本不会收成一个域，也没有水牢那种 `copiesCoalesce`。

同一次 tick、同一声明串第二次调用直接返回成功，不再放第二波。声明串是 `ownerUUID:效果键:initialCost:基础寿命`，可选再加后缀（78–83、114 行）。用的是未缩放的 `lifetimeTicks()`。这只防同一 tick 重复调用，不会关掉已经存在的同名域。再次释放会新建实例，旧域继续走到自己的结束条件。没有按术式 id、效果键或释放组替换旧域的代码。

扣费分两条。普通释放在 `SkillUsePacket` 519–524 行调用 `tryWave`，由 `DomainService` 99–104 行用 `ManaPaymentReceipt.charge` 扣 `40×份数`。因为 `explicitCost` 是 0，`SkillSegmentMath.manaCostOf`（236–241 行）不会把这 40 再算进按键预扣；预扣里仍有附魔段的 `enchantOnlyCost`。双重释放若走 `CompiledReleaseStep.memberShare`（209–210 行），账单已含 `initialCost×份数` 再加附魔，`CastExecutionSession` 298 行改走 `tryWaveReserved`，领域服务不再扣第二次。`SkillChainPlan.fireDualSequence`（320–341 行）仍调用会自己扣费的 `tryWave`，双重步骤间隔是 5 tick（815 行）。

服务端启动时 `DomainService.onServerStarted`（30–36 行）只清一次旧场地：半径约 4.5、湿 2、维护 1 的雾，以及半径约 5、湿 4、维护 4、带压强和到期爆发的重压（`FieldZones.retireLegacyMist` / `retireLegacyPressure`，103–116 行）。现行领域不进 `FieldZones`。

## 2. 生命周期与三类效果

服务器 tick 末尾顺序在 `ReleaseBaseEvents.onServerTick`（173–182 行）：`FieldZones.tick`、护体结算、`DomainService.tick`、然后 `ContactEnchantState.tick`。领域 tick（118–142 行）从后往前：已不活跃就摘掉；主人不在线、已死、所在 `ServerLevel` 与领域宿主层不是同一个，或 `withinLife` 为假，就关闭。寿命用 `ReleaseInput.hasExpired`（45–46 行），到期那一 tick 先关闭，不再扣当拍维护，也不再进后面的脉冲。维护时刻到了则 `MagicPool.trySpend(owner, upkeepCost, false)`，失败即关；成功则把下次维护推后 `upkeepPeriod`。首扣在 `startedAt + upkeepPeriod`（`DomainInstance` 42 行），雾雨和重压都是生成后 20 tick。维护循环结束后才 `DomainPulses.fire`。

`DomainInstance`（15–28、30–42 行）持有实例 id、主人、宿主层、固定几何、定义、修订号恒为 1、开始 tick、到期 tick、视觉起止、动画种子、下次维护 tick、活跃标记和接触状态。不写入玩家永久状态，也不存档。进程内静态列表 `ACTIVE`，重启即失。没有区块卸载回调；主人仍在同一层时，领域盒所在区块卸了，实例还在，卸载区内的实体扫描会空。

主人死亡、下线、跨维时，`ReleaseBaseEvents.endCast`（194–202 行）调用 `DomainService.closeOwned`（145–154 行）。关闭会 `contact.close()`、移出列表，并 `DomainSyncPacket.broadcast(..., true)`（191–194 行）。传送只 `DomainMotion.forget`（215–217 行），不关域，也不把人拉回。

原生脉冲在 `DomainPulses.fire`（28–51 行）。资格是 `DomainNative.hostileInside`（11–22 行）：活着、非旁观、同一层、身体放得进且完整包含，并且不是主人。`Relations.ally`（16–18 行）恒为假，所以盟友分支目前不会放行。潮湿和减速先按目标收成一份再给：潮湿同窗口只补差额到更大的量（54–66 行）；减速不盖过更强或更长的缓慢（107–115 行）。到期条件是 `now >= startedAt + delay` 且 `(now - startedAt) % period == 0`（69–73 行）。雾雨因此约在生成后 10、20、30… tick 给敌方湿 +2；重压同时刻给湿 +4，同轮取大量。水势只给完整位于域内、主人关系通过的存活非旁观玩家（118–139 行），雾雨每 20 tick +2，经 `WaterMomentum.credit` 写入，`PlayerShuData.setWaterMomentum`（173–178 行）封顶 100。重压量为 0，不供势。离域不清湿、不清缓慢、不扣回已给的水势。

呼吸不在脉冲里。`PressureBreath.onBreathe`（79–110 行）对敌向完整在域内的人，把领域倍率与潮湿倍率取较大值，再封顶 2，然后禁止换气并放大原版耗氧。倍率不大于 1，或原版消耗已是 0，则不放大。溺水范围由 `PressureDrownMixin` 进出 `openDrownScope` / `closeDrownScope`。这与维护扣款无关，域还活着就生效。

封界只作用于 `SEAL`。`EntityMoveMixin` 在 `Entity.move` 前调用 `DomainMotion.clip`，返回时 `finish`。`clip`（34–42 行）要求域仍活跃、边界不是 `OPEN`、整组未获通行、身体放得进，并且当前已经完整在内，然后按轴裁掉穿出的位移。`AxisBoxGeometry.fullInterval`（72–85 行）没有任何调用方。从外一 tick 穿过整盒时，开始时并未完整在内，这段不会被裁。`finish`（60–72 行）若裁切后仍算出逃出，会 `setPos` 回到允许点。深海重压是 `OPEN`，六面不裁。客户端候选来自 `DomainClientBridge.targets`（105–109 行）。通行豁免是主人，或 `Relations` 的许可/盟友；盟友目前恒假。

友方光环在命中时现查，不在领域 tick 里结算。`DomainAuraResolver.choose`（23–46 行）要求攻击者是生物、与域同层、主人或盟友、身体完整包含，然后在所有域里只取一份最大 `auraDamage`，相同则比实例 id。`DomainAuraSpec.tideNeutral`（18–24 行）读 `tide_mark` 对无属性到水的 `magicDamage`；`EnchantmentDefinitions` 49–51 行对该 id 固定返回 2。`AttackEnchantmentContext.grantsAura`（17–18 行）只放行 `ROOT_MELEE`、`ROOT_ARROW`、`ROOT_TECHNIQUE`。接入点是 `PlayerAttackMixin` 38 与 70 行、`ArrowAuraMixin` 24 行、`ArrowFlight.quote` 176 行、`TechniqueHits.hurtRoot` 30 行、`TechniqueHitSettle.settleFlow` 157 行、`ReleaseBatch.flush` 238 行。接触伤害走 `TechniqueHits.hurt`，不走 `hurtRoot`，因此不再加这份光环。

外接基础附魔接触：`ContactCast.snapshot`（55–70 行）只在宿主零伤、领域能承载、接触伤害大于 0 时做快照。`EnchantmentSnapshot.damageOf`（47–64 行）把各附魔 `magicDamage` 相加。`DomainInstance.bindContact`（65–69 行）交给 `ContactEnchantState.open`；伤害不大于 0 则不登记（35–37 行）。扫描在偶数服务器 tick（48–50 行），查询盒是领域盒外扩 1 格（`DomainInstance.queryBox` 166–168 行），真正命中用体积相交（157–158 行），不是完整包含。`ContactPolicy.mayContactAffect`（45–52 行）排除主人、盟友、旁观和死者。`TechniqueHits.hurt` 使用 `RESPECT_INVULNERABILITY`（`ContactEnchantState` 85–86 行）。hurt 被接受且目标仍活着，才 `FireSeedGrant.deposit` 并 `applyEffects`（89–93 行）。不因此续命，也不在这里扣魔力。

## 3. 伤害从哪来，取消维护后还剩什么

领域本身的生成费只在放出时收。存在期间的唯一持续扣款是每 20 tick 的 8 点维护。光环加伤、接触伤害、潮湿、减速、水势、呼吸、封界都不再另收每命中或每周期的魔力。附魔魔力在释放当次由 `enchantOnlyCost` 收一次，和后来每 2 tick 的接触无关。

接触伤害等于绑上去的基础附魔 `magicDamage` 之和。友方附加伤害等于潮印中性 `magicDamage`，当前代码是 2，加在根近战、箭或攻击术式上。原生脉冲不调用 `hurt`，也不产「领域 DOT 伤害」。重压的窒息来自原版空气消耗，不是 `TechniqueHits`。

维护失败才会把整份域关掉，从而连带停掉上述效果。扣款本身不创造这些效果。若不再因维护失败关闭，只要实例仍因寿命、主人死亡、下线或跨维而活着，这些就会继续：敌向潮湿与减速、雾雨友方水势、重压加倍耗氧、雾雨封界裁切、友方最多一份加伤、已绑定且接触伤害大于 0 的基础附魔接触。离域后的潮湿、缓慢和水势池本来就不会随关域立刻清掉。视觉随关闭包停播。`MagicPool.trySpend` 在费用小于等于 0 时直接成功（50–56 行）；现在费用是 8，失败仍会关域。

## 4. 触发绑定：现状与建议改动面

领域定义、实例和服务都没有绑定术式 id、触发周期、索敌或「对域内非释放者放一次」。垂丝是独立命中体 `water_weeping_arc`（`ReleaseDefinitions.java` 24 行），弹道 `FUNCTION`，不读领域。`FieldSpawner` 的附着弹道、锁定和追踪只服务 `fieldRadius > 0` 的旧场地。20 tick 只是雾雨维护周期和友方水势周期，不是通用的「检测并释放」参数。

以下为建议，不是已确认方案，也不是现有行为。

取消存在维护，适合只动扣款点，而不是拆掉寿命。`DomainService.tick` 134–140 行是唯一会因维护失败关域的地方；`DomainDefinition` 两个工厂的 `upkeepCost` 现为 8。寿命、主人结束和 `closeOwned` 可以留下。`WaterReleaseDefinition.domain` 110 行抄出的场地维护费，在 `fieldRadius` 为 0 时不会被 `FieldSpawner` 使用。

「基础附魔附加伤害再耗魔」现在没有落点。接触成功在 `ContactEnchantState.scan` 85–93 行，光环成功在上面列出的 `merge` 调用处。两处都只改伤害，不调用 `MagicPool`。用户说的是触发基础附魔的附加伤害，以及释放绑定术式；友方潮印光环在代码里是另一路固定加伤。缺魔时跳过这一次但保留领域，代码里没有，也不能当成已确认。

绑定术式不宜写进所有领域的默认脉冲。更小的接法是：某份 `DomainDefinition` 另带可选绑定，未声明则没有；雾雨若要示例，只配在 `mistRain` 上，周期由该绑定自带，不复用维护周期 20。触发后的那一次释放应走现有链：`SkillUsePacket`、`SkillChainPlan`、`CastExecutionSession` 已能带规则、弹道和份数，并已能 `tryWave` / `tryWaveReserved`。不要在 `DomainPulses` 里按术式 id 直接生成垂丝实体，否则规则和弹道组合进不去。索敌位置、单目标还是多目标、同一目标是否叠帘、缺魔是跳过这次还是关域，代码都没有对应字段。`FieldZones` 不应再承担这件事。

## 5. 旧文档与额度记录

`docs/雾雨重设计与实现指导-2026-10-23.md`、`docs/高阶附魔底座设计与实现指导-2026-10-23.md`、`docs/深海重压重设计与实现指导-2026-10-23.md` 文首仍是「待实施」。领域包、两条注册、接触、光环、封界和旧场地清退都已在源码里。`docs/交接索引.md` 与 `docs/交接-2026-10-16-当前进度.md` 没有这套领域的实现进度。

雾雨文档写用户确认的完整尺寸是 5×5×8。代码是宽 7、深 7、高 8。深海文档的 7×7×3 与代码一致。文档里的 `DomainBoundaryPolicy`、运动参数区间 `[tEnter,tExit]`、踏步后再校验，对应到现在的 `DomainBoundary` 枚举和 `DomainMotion`；`fullInterval` 无人调用，裁切只处理已经完整在内的移动。文档里的 `AttackEnchantmentContext` 含根攻击 id、修订和「已解析」标记；代码只有种类枚举，光环靠各种类的 `merge` 调用点分开。文档把每 20 tick 8 点写成持续维护，并写明接触不另收费。当前代码仍收这 8 点。2026-10-29 的方向是取消存在维护，只在基础附魔附加伤害和绑定术式释放时另收费；三份旧文没有这条。文档写的历史许可和达级补学已经有实现：`DomainLearning` 迁移版本 2（46–58 行），已学 `water_mist` 或 `water_deep_pressure` 会发领域许可；`PlayerShuData.isUnlocked`（198–200 行）有许可即通过；`learnsWhenReady` 为真时，法术等级达到复杂度 10 会补学（21–39 行）。`Relations.ally` 仍恒为假，与旧文一致。

额度只转述，本次不作评审。`docs/开发记录/领域附魔方案-20261029/记录.md` 第 10 条原文是：「本次并非已有批次的正确性/验收复审，不重置任何已有批次额度。方案正式架构裁决使用唯一 gpt-6-astra，本次设计第1/2轮将在两路最终事实齐备后进行；当前尚未裁决。实现阶段未启动。」同文写明实现阶段未启动，编译、打包与游戏实测均未执行。上述三份 2026-10-23 指导没有设计 n/2 或实现 n/3 的轮次记录。
