package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.shu.ModTechniques;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.shu.technique.enchant.TideMarkDamageScope;
import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainDefinition;
import com.bslx.XueXiMod.shu.technique.release.payload.FlowSuccessPayload;
import com.bslx.XueXiMod.shu.technique.release.payload.HitPayloads;
import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierDefinition;
import com.bslx.XueXiMod.shu.technique.release.conditional.WaterPrisonEffect;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * 直接调用垂丝的数值、载荷合并、轮次账本和成功范围。
 * 不构造生物、世界或启动器。实体 hurt、无敌帧和瀑布宿主注册留到运行时。
 */
public final class WeepingSettlementCheck {
    private static int assertionCount;

    private WeepingSettlementCheck() {
    }

    public static void main(String[] arguments) throws Exception {
        ReleaseDefinitions.registerAll();
        settingsAndRegistration();
        otherCostsStayDerived();
        payloadMerge();
        roundLedger();
        successScope();
        wiring();
        System.out.println("未执行 实体 hurt、原版无敌帧、领域并伤、瀑布宿主注册和游戏画面：需要世界或另外两位作者的类型");
        System.out.println("垂丝结算检查通过");
        System.out.println("断言 " + assertionCount);
    }

    private static void settingsAndRegistration() {
        check(WeepingSettings.BASE_DAMAGE == 2.0f, "底伤不是 2");
        check(WeepingSettings.WETNESS == 3, "基础潮湿不是 3");
        check(WeepingSettings.HIT_INTERVAL == 10, "尝试间隔不是 10");
        check(WeepingSettings.LIFETIME == 40, "寿命不是 40");
        check(WeepingSettings.FILL_TICKS == 8, "充水拍数不是 8");
        check(WeepingSettings.DRAIN_START == 32, "排空起点不是 32");
        check(WeepingSettings.FALL_DISTANCE_LIMIT == 6.0, "最大落差不是 6");
        check(WeepingSettings.RETAINED_MANA == 10, "保留魔力不是 10");
        check(FlowRoundLedger.FLUSH_DELAY == 6, "汇总窗口不是 6 拍");

        ReleaseDefinition weeping = ReleaseRegistry.get("water_weeping_arc");
        Technique technique = ModTechniques.WATER_WEEPING_ARC;
        check(weeping != null, "垂丝没有登记");
        check(weeping.persistentFlowSettlement(), "垂丝没有打开持续结算");
        check(weeping.deflectsProjectiles(), "垂丝没有打开承接来袭弹");
        check(weeping.wetnessGrant() == WeepingSettings.WETNESS, "垂丝潮湿不是集中的 3");
        check(weeping.lifetimeTicks() == WeepingSettings.LIFETIME, "垂丝寿命没有读集中常量");
        check(weeping.explicitManaCost() == WeepingSettings.RETAINED_MANA, "垂丝没有显式保留魔力");
        check(technique.getBaseDamage() == WeepingSettings.BASE_DAMAGE, "垂丝底伤不是 2");
        check(technique.getBaseCooldown() == 20, "垂丝冷却不是 20");
        int derived = (int) Math.ceil(technique.getBaseDamage() * 2.0);
        check(derived == 4, "底伤 2 的旧推导应是 4，实际 " + derived);
        check(technique.manaCost() == WeepingSettings.RETAINED_MANA, "展示魔力没有保住 10，实际 " + technique.manaCost());

        ReleaseDefinition heat = ReleaseRegistry.get("heat_beam");
        check(heat != null && !heat.persistentFlowSettlement(), "热光束被持续结算带上了");
        check(!heat.deflectsProjectiles(), "热光束不承接来袭弹");
        check(heat.explicitManaCost() < 0, "热光束不该改成显式魔力");
        Technique heatTechnique = ModTechniques.HEAT_BEAM;
        int heatDerived = (int) Math.ceil(heatTechnique.getBaseDamage() * 2.0);
        check(heatTechnique.manaCost() == heatDerived, "热光束魔力仍按伤害推导");
    }

    private static void otherCostsStayDerived() {
        ReleaseDefinition thrust = ReleaseRegistry.get("sword_thrust");
        Technique thrustTechnique = ModTechniques.SWORD_THRUST;
        int thrustDerived = (int) Math.ceil(thrustTechnique.getBaseDamage() * 2.0);
        check(thrust != null && !thrust.persistentFlowSettlement(), "斩击没有持续结算");
        check(thrust.explicitManaCost() < 0, "斩击保持旧推导");
        check(thrustTechnique.manaCost() == thrustDerived, "斩击魔力被显式费用改掉");

        WaterReleaseDefinition blade = WaterReleaseDefinition.shot("settlement_probe_blade", Trajectory.STRAIGHT, 10, 1.0f, 0.2f);
        ReleaseRegistry.register(blade);
        Technique bladeTechnique = new Technique("settlement_probe_blade", "探针刃", TechniqueType.SWORD,
                7f, 14, 20, "探针", 0, ShuAttribute.WATER, 1f);
        check(blade.explicitManaCost() < 0, "水系射击没有显式费用");
        check(bladeTechnique.manaCost() == 14, "水系射击魔力仍是 14，实际 " + bladeTechnique.manaCost());

        WaterReleaseDefinition conditional = WaterReleaseDefinition.conditional(
                "settlement_probe_prison", 40, WaterPrisonEffect.INSTANCE);
        ReleaseRegistry.register(conditional);
        Technique prisonTechnique = new Technique("settlement_probe_prison", "探针牢", TechniqueType.MAGIC,
                0f, 400, 20, "探针", 0, ShuAttribute.WATER, 0f);
        check(prisonTechnique.manaCost() == 40, "条件释放展示仍是显式 40");

        WaterReleaseDefinition domain = WaterReleaseDefinition.domain(
                "settlement_probe_domain", DomainDefinition.mistRain());
        ReleaseRegistry.register(domain);
        Technique domainTechnique = new Technique("settlement_probe_domain", "探针域", TechniqueType.MAGIC,
                0f, 80, 20, "探针", 0, ShuAttribute.WATER, 0f);
        check(domain.explicitManaCost() == 0, "领域工厂仍写下 0");
        check(domainTechnique.manaCost() == 40, "领域展示仍是初始费用 40，实际 " + domainTechnique.manaCost());

        WaterReleaseDefinition barrier = WaterReleaseDefinition.barrier(
                "settlement_probe_barrier", BarrierDefinition.curtain());
        ReleaseRegistry.register(barrier);
        Technique barrierTechnique = new Technique("settlement_probe_barrier", "探针幕", TechniqueType.MAGIC,
                0f, 40, 20, "探针", 0, ShuAttribute.WATER, 0f);
        check(barrierTechnique.manaCost() == 0, "零伤屏障展示仍是 0");
    }

    private static void payloadMerge() {
        FlowSuccessPayload empty = HitPayloads.capture(null);
        check(empty.wetness() == 0 && empty.knockStrength() == 0f, "没有挂载时不应带着潮湿或击退");

        FlowSuccessPayload first = empty.withStrike(2, 10, 0.2f, 1.0, 0.0, false);
        FlowSuccessPayload second = FlowSuccessPayload.attached(3, 0f, 0f, 0f, false, null)
                .withStrike(5, 40, 0.35f, 0.0, 4.0, false);
        FlowSuccessPayload third = FlowSuccessPayload.attached(3, 1.5f, 0.4f, 0.2f, true, null)
                .withStrike(4, 20, 0.1f, 3.0, 3.0, true);
        FlowSuccessPayload merged = first.mergedWith(second).mergedWith(third);
        check(merged.wetness() == 3, "三份基础潮湿合成了 " + merged.wetness() + "，应是 3");
        check(merged.burnSeconds() == 5, "燃烧应取较长的 5 秒");
        check(merged.slowTicks() == 40, "缓慢应取较长的 40 拍");
        check(merged.knockStrength() == 0.35f, "击退应取较强的 0.35");
        check(merged.pushAcross() == 1.0 && merged.pushAlong() == 0.0, "击退方向应留第一份");
        check(merged.purify(), "任一灭火就保留灭火");
        check(merged.healAmount() == 1.5f, "治疗取较大的一份");
        check(merged.pullStrength() == 0.4f && merged.liftStrength() == 0.2f, "牵引和上抛取较大强度");
        check(merged.pullAnchor() == null, "牵引方向留第一份");
    }

    private static void roundLedger() {
        UUID targetId = UUID.randomUUID();
        FlowRoundLedger ledger = new FlowRoundLedger();
        FlowSuccessPayload first = FlowSuccessPayload.attached(3, 0f, 0f, 0f, false, null)
                .withStrike(2, 10, 0.2f, 1.0, 0.0, false);
        FlowSuccessPayload second = FlowSuccessPayload.attached(3, 0f, 0f, 0f, false, null)
                .withStrike(5, 40, 0.35f, 0.0, 4.0, false);
        FlowSuccessPayload third = FlowSuccessPayload.attached(3, 0f, 0f, 0f, false, null)
                .withStrike(4, 20, 0.1f, 3.0, 3.0, false);
        check(ledger.accept(targetId, 0, 0, 2f, 1, first), "第一份应收下");
        check(ledger.accept(targetId, 0, 1, 2f, 1, second), "第二份应收下");
        check(ledger.accept(targetId, 0, 2, 5f, 1, third), "第三份应收下");
        check(!ledger.accept(targetId, 0, 1, 9f, 1, third), "同一轮同一份不重复收");
        check(ledger.count(targetId) == 3, "三份都计数");
        check(ledger.aggregatedDamage(targetId) == 15f, "伤害按最后一份乘份数，应是 15");
        check(ledger.aggregatedDamage(targetId) != 9f, "三份伤害不是把 2、2、5 加起来");
        FlowSuccessPayload merged = ledger.payload(targetId);
        check(merged != null && merged.wetness() == 3, "账本里的基础潮湿只留 3");
        check(merged.knockStrength() == 0.35f && merged.pushAcross() == 1.0, "账本击退取强度、留第一方向");
        check(ledger.takeMarks(targetId) == 3, "三份潮印条数都累加");
        check(TideMarkSuccess.of(3, null).wetness() == 15, "三份潮印潮湿仍是 15，和基础潮湿分开");
        ledger.clearSettled();
        check(ledger.payload(targetId) == null && ledger.count(targetId) == 0, "结算后清掉本轮载荷");
        check(!ledger.accept(targetId, 0, 0, 2f, 1, first), "旧轮次不能再收");
        check(ledger.accept(targetId, 1, 0, 2f, 1, first), "下一轮可以再收");
        check(ledger.takeMarks(targetId) == 1, "下一轮只算新的一条");

        FlowRoundLedger partial = new FlowRoundLedger();
        partial.accept(targetId, 0, 0, 4f, 1, first);
        partial.accept(targetId, 0, 2, 4f, 1, second);
        check(partial.count(targetId) == 2, "缺一份时按实收份数");
        check(partial.aggregatedDamage(targetId) == 8f, "两份按最后伤害乘 2");
        check(partial.payload(targetId).wetness() == 3, "两份基础潮湿仍是 3");
        check(partial.takeMarks(targetId) == 2, "两份潮印是 2 条");

        FlowRoundLedger plain = new FlowRoundLedger();
        UUID otherTarget = UUID.randomUUID();
        check(plain.accept(otherTarget, 0, 0, 6f, 0, null), "旧模式没有持续载荷也能入账");
        check(plain.payload(otherTarget) == null, "旧模式不留下持续载荷");
        check(plain.aggregatedDamage(otherTarget) == 6f, "单份旧伤害保持原数");

        check(FlowRoundLedger.roundIndex(10, 100, 109) == 0, "间隔未满仍是本轮");
        check(FlowRoundLedger.roundIndex(10, 100, 110) == 1, "满 10 拍进入下一轮");
        check(FlowRoundLedger.roundIndex(0, 100, 200) == 0, "没有间隔时轮次保持 0");
        check(FlowRoundLedger.FLUSH_DELAY == 6, "窗口仍是 6 拍");
    }

    private static void successScope() {
        Object target = new Object();
        Object source = new Object();
        TideMarkDamageScope.Token closed = TideMarkDamageScope.open(target, source, TideMarkSuccess.NONE);
        TideMarkDamageScope.note(target, source, false, 4f);
        check(!TideMarkDamageScope.finish(closed, true), "旧的零条入口仍不建帧");

        TideMarkDamageScope.Token watched = TideMarkDamageScope.watch(target, source, TideMarkSuccess.NONE);
        TideMarkDamageScope.note(target, source, false, 4f);
        check(TideMarkDamageScope.finish(watched, true), "观察帧承认最终正伤");
        check(TideMarkSuccess.NONE.wetness() == 0, "零条观察不产生潮印潮湿");

        TideMarkDamageScope.Token canceled = TideMarkDamageScope.watch(target, source, TideMarkSuccess.NONE);
        TideMarkDamageScope.note(target, source, true, 4f);
        check(!TideMarkDamageScope.finish(canceled, true), "取消不算成功");

        TideMarkDamageScope.Token absorbed = TideMarkDamageScope.watch(target, source, TideMarkSuccess.NONE);
        TideMarkDamageScope.note(target, source, false, 0f);
        check(!TideMarkDamageScope.finish(absorbed, true), "零伤不算成功");

        TideMarkDamageScope.Token missed = TideMarkDamageScope.watch(target, source, TideMarkSuccess.NONE);
        TideMarkDamageScope.note(target, source, false, 4f);
        check(!TideMarkDamageScope.finish(missed, false), "hurt 失败不算成功");

        TideMarkSuccess one = TideMarkSuccess.of(1, UUID.randomUUID());
        TideMarkDamageScope.Token marked = TideMarkDamageScope.watch(target, source, one);
        TideMarkDamageScope.note(target, source, false, 2f);
        check(TideMarkDamageScope.finish(marked, true), "有条数的观察帧仍按原规则提交");
        check(one.wetness() == 5 && one.momentum() == 2, "观察帧不改潮印奖励");
    }

    private static void wiring() throws Exception {
        String effect = Files.readString(Path.of(
                "src/main/java/com/bslx/XueXiMod/shu/technique/effects/WeepingCurtainEffect.java"));
        String heat = Files.readString(Path.of(
                "src/main/java/com/bslx/XueXiMod/shu/technique/effects/HeatBeamEffect.java"));
        check(effect.contains("WeepingSettings.HIT_INTERVAL"), "垂丝间隔读集中常量");
        check(heat.contains("spec.hitInterval = 10"), "热光束仍是自己的间隔");
        check(!heat.contains("persistentFlowSettlement"), "热光束效果不打开新模式");

        String settle = Files.readString(Path.of(
                "src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitSettle.java"));
        int tryAt = settle.indexOf("static void tryHit");
        int settleAt = settle.indexOf("static void settle(");
        int flowAt = settle.indexOf("private static void settleFlow");
        int detonateAt = settle.indexOf("static void detonate");
        check(tryAt >= 0 && settleAt > tryAt && flowAt > settleAt && detonateAt > flowAt, "结算方法还在");
        String tryBody = settle.substring(tryAt, settleAt);
        String oldBody = settle.substring(settleAt, flowAt);
        String flowBody = settle.substring(flowAt, detonateAt);
        check(tryBody.contains("if (!hitEntity.persistentFlow)"), "新模式不走旧的提前施加");
        check(tryBody.contains("HitPayloads.apply"), "旧模式仍提前施加挂载");
        check(!tryBody.contains("lastHitTick.remove") && !tryBody.contains("hitOnce.clear"), "失败不清闸门");
        check(oldBody.contains("hurtRoot") && oldBody.contains("setSecondsOnFire"), "旧结算仍清无敌帧路径并当场着火");
        check(flowBody.contains("hurtConfirmed") && flowBody.contains("DomainAuraResolver.merge"), "新结算确认一次正伤");
        check(!flowBody.contains("hurtRoot") && !flowBody.contains("HitPayloads.apply"), "新结算不走旧根伤和提前挂载");

        String hits = Files.readString(Path.of(
                "src/main/java/com/bslx/XueXiMod/shu/technique/release/TechniqueHits.java"));
        check(hits.contains("BYPASS_INVULNERABILITY"), "旧根伤仍绕过无敌帧");
        int confirmedAt = hits.indexOf("boolean hurtConfirmed");
        int readAt = hits.indexOf("private static float readLastHurt");
        check(confirmedAt >= 0 && readAt > confirmedAt, "确认伤害方法还在");
        String confirmedBody = hits.substring(confirmedAt, readAt);
        check(confirmedBody.contains("RESPECT_INVULNERABILITY"), "确认伤害尊重无敌帧");
        check(confirmedBody.contains("TideMarkDamageScope.watch"), "确认伤害看最终正伤");
        check(!confirmedBody.contains("DomainAuraResolver") && !confirmedBody.contains("invulnerableTime = 0"),
                "确认伤害不再并领域，也不清无敌时间");

        String batch = Files.readString(Path.of(
                "src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseBatch.java"));
        check(batch.contains("FlowRoundLedger.FLUSH_DELAY"), "汇总仍用 6 拍窗口");
        check(batch.contains("hurtRoot") && batch.contains("hurtConfirmed"), "新旧汇总都还在");
        check(batch.contains("originTick") && !batch.contains("hitOnce"), "账本不清理旧的一生一次");

        String entity = Files.readString(Path.of(
                "src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitEntity.java"));
        int tickAt = entity.indexOf("public void tick()");
        int decorAt = entity.indexOf("private void clientDecor()");
        check(tickAt >= 0 && decorAt > tickAt, "找到实体节拍");
        String tickBody = entity.substring(tickAt, decorAt);
        int motionAt = tickBody.indexOf("this.motion.tick(this, level)");
        int refreshAt = tickBody.indexOf("this.refreshWeepingDeflection()");
        int hitAt = tickBody.indexOf("TechniqueHitSettle.tryHit");
        check(motionAt >= 0 && refreshAt > motionAt && hitAt > refreshAt, "瀑布宿主在运动之后、命中之前更新");
        int releaseAt = entity.indexOf("private void releaseWeepingDeflection()");
        int originAt = entity.indexOf("public void setOrigin");
        check(releaseAt > 0 && originAt > releaseAt, "找到移除清理");
        String releaseBody = entity.substring(releaseAt, originAt);
        int bindingAt = releaseBody.indexOf("WeepingDeflectionBinding.remove");
        int forgetAt = releaseBody.indexOf("WeepingCurtain.forget");
        check(releaseBody.contains("isClientSide()") && bindingAt >= 0 && forgetAt > bindingAt, "服务端先卸宿主再忘掉水段");
        check(!releaseBody.contains("settleTally"), "水段到期不取消已经记下的汇总");
        String refreshBody = entity.substring(entity.indexOf("private void refreshWeepingDeflection()"), releaseAt);
        check(refreshBody.contains("isClientSide()") && refreshBody.contains("WeepingDeflectionBinding.update"),
                "服务端用当前水段更新宿主");
        check(!entity.contains("water_weeping_arc"), "实体接线不按术式编号分支");

        String pricing = Files.readString(Path.of(
                "src/main/java/com/bslx/XueXiMod/network/SkillSegmentMath.java"));
        int costAt = pricing.indexOf("private static int manaCostOf");
        int nextAt = pricing.indexOf("static boolean sharesDualRelease");
        check(costAt >= 0 && nextAt > costAt, "找到实际扣费");
        String costBody = pricing.substring(costAt, nextAt);
        int explicitAt = costBody.indexOf("explicitManaCost()");
        int fallbackAt = costBody.indexOf("technique.manaCost()");
        check(explicitAt >= 0 && fallbackAt > explicitAt, "扣费先看显式魔力，再回落到旧推导");
    }

    private static void check(boolean condition, String label) {
        assertionCount++;
        if (!condition) {
            throw new IllegalStateException(label);
        }
    }
}
