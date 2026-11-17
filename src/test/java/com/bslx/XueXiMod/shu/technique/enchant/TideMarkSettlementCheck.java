package com.bslx.XueXiMod.shu.technique.enchant;

import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.shu.technique.EnchantmentDefinitions;
import com.bslx.XueXiMod.shu.technique.EnchantmentRegistry;
import com.bslx.XueXiMod.shu.technique.release.TideMarkCopies;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainAuraSpec;
import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.skill.SkillType;
import net.minecraft.nbt.CompoundTag;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * 不启动游戏。核对潮印解析、条数、成功范围和账本聚合。
 * 实体 hurt 与物品栈环境不在这里伪造。
 */
public final class TideMarkSettlementCheck {
    public static void main(String[] arguments) throws Exception {
        EnchantmentDefinitions.registerAll();
        parseSingleAndSameWater();
        parseTwoMarks();
        contactReadKeepsSnapshotPayload();
        weaponTagBoundary();
        scopeSuccessAndFailure();
        copyLedger();
        domainDamageOnly();
        publicMomentumCreditUnchanged();
        contactDoesNotDropSnapshot();
        System.out.println("潮印结算检查通过");
        System.out.println("断言 " + assertionCount);
    }

    private static void parseSingleAndSameWater() {
        Technique blade = new Technique("tide_sever", "断潮", TechniqueType.SWORD,
                6f, 20, 20, "切", 0, ShuAttribute.WATER, 1f);
        Technique tide = tideMark();
        Skill plain = new Skill("plain", SkillType.SWORD, List.of(blade, tide),
                List.of(ShuAttribute.NONE, ShuAttribute.NONE));
        Skill sameWater = new Skill("same", SkillType.SWORD, List.of(blade, tide),
                List.of(ShuAttribute.NONE, ShuAttribute.WATER));
        EnchantBonus ordinary = EnchantBonus.resolve(plain, List.of(1), 6f, 1);
        EnchantBonus matched = EnchantBonus.resolve(sameWater, List.of(1), 6f, 1);
        check(ordinary.tideMarks == 1, "单潮印计 1 条");
        check(matched.tideMarks == 1, "同水仍计 1 条");
        check(ordinary.tideFlat() == 2f, "普通潮印固定 2");
        check(matched.tideFlat() == 2f, "同水潮印仍是 2");
        check(ordinary.addedDamage == 0f && matched.addedDamage == 0f, "潮印不进 added");
        check(!ordinary.extinguish && !matched.extinguish, "潮印不再灭火");
        check(matched.attackDamage > ordinary.attackDamage, "同水仍抬宿主底伤");
        Technique metalHost = new Technique("keen_cut", "金切", TechniqueType.SWORD,
                6f, 20, 20, "切", 0, ShuAttribute.METAL, 1f);
        Skill generated = new Skill("metal", SkillType.SWORD, List.of(metalHost, tide),
                List.of(ShuAttribute.NONE, ShuAttribute.METAL));
        EnchantBonus metalWater = EnchantBonus.resolve(generated, List.of(1), 6f, 1);
        check(Math.abs(metalWater.speedMultiplier - 1.10f) < 0.001f, "金生水速度保留");
        check(metalWater.tideFlat() == 2f, "相生不放大潮印");
        var tideDefinition = EnchantmentRegistry.get("tide_mark");
        var fireDefinition = EnchantmentRegistry.get("magic_ignite");
        check(tideDefinition.magicDamage(ElementReaction.of(ShuAttribute.WATER, ShuAttribute.WATER)) == 2, "潮印魔法伤固定 2");
        check(fireDefinition.magicDamage(ElementReaction.of(ShuAttribute.FIRE, ShuAttribute.FIRE)) == 3, "火系公式不动");
    }

    private static void parseTwoMarks() {
        Technique tide = tideMark();
        Technique other = tideMark();
        Skill skill = new Skill("dual", SkillType.SWORD, List.of(tide, other),
                List.of(ShuAttribute.WATER, ShuAttribute.WATER));
        EnchantBonus bonus = EnchantBonus.resolve(skill, List.of(0, 1), 4f, 1);
        check(bonus.tideMarks == 2, "两条潮印都计数");
        check(bonus.tideFlat() == 4f, "两条潮印伤害 4");
        TideMarkSuccess success = bonus.tideSuccess(UUID.randomUUID());
        check(success.wetness() == 10 && success.momentum() == 4, "双潮印 10 湿 4 势");
        EnchantBonus layered = EnchantBonus.resolve(skill, List.of(0), 4f, 2);
        check(layered.tideMarks == 2 && layered.tideFlat() == 4f, "层数重复计入");
        check(!bonus.extinguish, "多条潮印也不灭火");
    }

    private static void contactReadKeepsSnapshotPayload() {
        Technique tide = tideMark();
        Skill skill = new Skill("contact", SkillType.MAGIC, List.of(tide, tide),
                List.of(ShuAttribute.WATER, ShuAttribute.NONE));
        EnchantmentSnapshot.Read loaded = EnchantmentSnapshot.read(skill, List.of(0, 1));
        check(loaded.damage == 4f, "接触两条潮印伤害 4");
        check(loaded.tideMarks == 2, "接触按条计数");
        check(!loaded.extinguish, "接触潮印不灭火");
        TideMarkSuccess success = TideMarkSuccess.of(loaded.tideMarks, UUID.randomUUID());
        check(success.wetness() == 10 && success.momentum() == 4, "接触成功载荷 10/4");
    }

    private static void weaponTagBoundary() {
        Technique tide = tideMark();
        Technique fire = new Technique("magic_ignite", "灼印", TechniqueType.UNIVERSAL,
                0f, 40, 20, "火", 0, ShuAttribute.FIRE, 1f, true);
        Skill mixed = new Skill("mixed", SkillType.SWORD, List.of(fire, tide),
                List.of(ShuAttribute.NONE, ShuAttribute.NONE));
        EnchantBonus bonus = EnchantBonus.resolve(mixed, List.of(0, 1), 12f, 1);
        CompoundTag tag = WeaponEnchant.toTag(bonus);
        float flatDamage = WeaponEnchant.meleeFlat(tag);
        check(tag.getFloat("Attack") == 12f, "印记仍记下武器攻击");
        check(flatDamage == bonus.tideFlat() + bonus.addedDamage, "主攻击只加附加值");
        check(flatDamage != tag.getFloat("Attack") + bonus.addedDamage, "附加值不含保存的武器攻击");
        check(tag.getInt("Tide") == 1, "印记记下一条潮印");
        check(!tag.getBoolean("Extinguish"), "潮印印记不灭火");
        CompoundTag pure = WeaponEnchant.toTag(EnchantBonus.resolve(
                new Skill("pure", SkillType.SWORD, List.of(tide), List.of(ShuAttribute.NONE)),
                List.of(0), 9f, 1));
        check(WeaponEnchant.meleeFlat(pure) == 2f, "纯潮印附加 2");
        check(!WeaponEnchant.consumeImprint(false, pure, pure.copy()), "失败保留印记");
        CompoundTag changed = pure.copy();
        changed.putInt("Tide", 3);
        check(!WeaponEnchant.consumeImprint(true, pure, changed), "新印记不被旧捕获消费");
        CompoundTag spent = pure.copy();
        check(WeaponEnchant.consumeImprint(true, pure, spent), "成功且身份一致才消费");
        spent.remove("Tide");
        check(!WeaponEnchant.consumeImprint(true, pure, spent), "消费后不能再消费");
    }

    private static void scopeSuccessAndFailure() {
        Object target = new Object();
        Object source = new Object();
        TideMarkSuccess one = TideMarkSuccess.of(1, UUID.randomUUID());
        TideMarkDamageScope.Token success = TideMarkDamageScope.open(target, source, one);
        TideMarkDamageScope.note(target, source, false, 4f);
        check(TideMarkDamageScope.finish(success, true), "单份成功");
        check(one.wetness() == 5 && one.momentum() == 2, "单份 5/2");
        check(one.flatDamage() == 2f && one.wetness() == 5 && one.momentum() == 2, "单条潮印 2 伤 5 湿 2 势");
        check(!TideMarkDamageScope.finish(success, true), "成功只提交一次");

        TideMarkDamageScope.Token canceled = TideMarkDamageScope.open(target, source, one);
        TideMarkDamageScope.note(target, source, true, 4f);
        check(!TideMarkDamageScope.finish(canceled, true), "取消不奖");

        TideMarkDamageScope.Token absorbed = TideMarkDamageScope.open(target, source, one);
        TideMarkDamageScope.note(target, source, false, 0f);
        check(!TideMarkDamageScope.finish(absorbed, true), "零伤不奖");

        TideMarkDamageScope.Token missed = TideMarkDamageScope.open(target, source, one);
        TideMarkDamageScope.note(target, source, false, 4f);
        check(!TideMarkDamageScope.finish(missed, false), "hurt 失败不奖");

        Object otherTarget = new Object();
        Object otherSource = new Object();
        TideMarkDamageScope.Token outer = TideMarkDamageScope.open(target, source, one);
        TideMarkDamageScope.Token inner = TideMarkDamageScope.open(otherTarget, otherSource, one);
        TideMarkDamageScope.note(otherTarget, otherSource, false, 3f);
        check(TideMarkDamageScope.finish(inner, true), "内层自己的伤害可成功");
        TideMarkDamageScope.note(otherTarget, otherSource, false, 9f);
        check(!TideMarkDamageScope.finish(outer, true), "嵌套无关伤害不给外层");

        TideMarkDamageScope.Token paired = TideMarkDamageScope.open(target, source, TideMarkSuccess.of(2, UUID.randomUUID()));
        TideMarkDamageScope.note(otherTarget, otherSource, false, 8f);
        TideMarkDamageScope.note(target, source, false, 2f);
        check(TideMarkDamageScope.finish(paired, true), "无关事件之后仍认自己的正伤");
    }

    private static void copyLedger() {
        UUID firstTarget = UUID.randomUUID();
        UUID secondTarget = UUID.randomUUID();
        TideMarkCopies copies = new TideMarkCopies();
        check(copies.accept(firstTarget, 0, 0, 1), "第一份收下");
        check(copies.accept(firstTarget, 0, 1, 1), "第二份收下");
        check(copies.accept(firstTarget, 0, 2, 1), "第三份收下");
        check(!copies.accept(firstTarget, 0, 1, 1), "重复份不增");
        check(copies.take(firstTarget) == 3, "三份全中 3 条");
        check(TideMarkSuccess.of(3, null).wetness() == 15, "三份 15 湿");
        check(TideMarkSuccess.of(3, null).momentum() == 6, "三份 6 势");

        TideMarkCopies partial = new TideMarkCopies();
        partial.accept(firstTarget, 0, 0, 1);
        partial.accept(firstTarget, 0, 2, 1);
        check(partial.take(firstTarget) == 2, "部分命中 2 条");
        check(TideMarkSuccess.of(2, null).wetness() == 10 && TideMarkSuccess.of(2, null).momentum() == 4, "部分 10/4");

        TideMarkCopies late = new TideMarkCopies();
        late.accept(firstTarget, 0, 0, 1);
        late.accept(firstTarget, 0, 1, 1);
        check(late.take(firstTarget) == 2, "先结算两份");
        check(late.accept(firstTarget, 0, 2, 1), "晚到第三份");
        check(!late.accept(firstTarget, 0, 0, 1), "旧份不重算");
        check(late.take(firstTarget) == 1, "晚到只算增量");

        TideMarkCopies rounds = new TideMarkCopies();
        rounds.accept(firstTarget, 0, 0, 1);
        rounds.accept(firstTarget, 1, 0, 1);
        rounds.accept(secondTarget, 0, 0, 2);
        check(rounds.take(firstTarget) == 2, "不同轮都算");
        check(rounds.take(secondTarget) == 2, "不同目标单独算");

        TideMarkCopies groupLeft = new TideMarkCopies();
        TideMarkCopies groupRight = new TideMarkCopies();
        check(groupLeft.accept(firstTarget, 0, 0, 1), "甲组收下");
        check(groupRight.accept(firstTarget, 0, 0, 1), "乙组不跟甲组去重");
        check(groupLeft.take(firstTarget) == 1 && groupRight.take(firstTarget) == 1, "两组各算各的");
    }

    private static void domainDamageOnly() {
        DomainAuraSpec aura = DomainAuraSpec.tideNeutral();
        check(aura.bonusDamage() == 2f, "领域附伤 2");
        check("tide_mark".equals(aura.enchantmentId()), "领域仍指向潮印");
    }

    private static void publicMomentumCreditUnchanged() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bslx/XueXiMod/shu/technique/release/momentum/ReleaseBaseEvents.java"));
        int start = source.indexOf("public static void onDamage");
        int end = source.indexOf("public static void onDeath");
        check(start >= 0 && end > start, "找到公共伤害回势");
        String method = source.substring(start, end);
        check(method.contains("WaterMomentum.credit(player, dealt)"), "受害玩家仍按实际掉血");
        check(method.contains("WaterMomentum.credit(attacker, dealt)"), "攻击者仍按实际掉血");
        check(!method.contains("dealt - 2") && !method.contains("dealt-2"), "不从实际掉血里扣潮印");
    }

    private static void contactDoesNotDropSnapshot() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bslx/XueXiMod/shu/technique/release/contact/ContactEnchantState.java"));
        int start = source.indexOf("private void scan");
        int end = source.indexOf("private AABB queryBox");
        check(start >= 0 && end > start, "找到接触扫描");
        String method = source.substring(start, end);
        check(method.contains("tideSuccess()"), "接触成功载荷进同一 hurt");
        check(method.contains("RESPECT_INVULNERABILITY"), "接触仍尊重无敌帧");
        check(method.contains("applyEffects"), "原有接触效果还在");
        check(!method.contains(".close()") && !method.contains("snapshot = null"), "命中不摘快照");
    }

    private static Technique tideMark() {
        return new Technique("tide_mark", "潮印", TechniqueType.UNIVERSAL,
                0f, 40, 20, "潮", 0, ShuAttribute.WATER, 1f, true);
    }

    private static int assertionCount;

    private static void check(boolean condition, String label) {
        assertionCount++;
        if (!condition) {
            throw new IllegalStateException(label);
        }
    }
}
