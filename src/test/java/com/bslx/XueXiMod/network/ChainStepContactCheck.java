package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.ModTechniques;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.TechniqueManager;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.skill.SkillType;

import java.util.List;

/**
 * 直接调用生产的 ChainSteps 和 ContactCast。
 * 不复制分步算法，也不构造玩家或世界。
 */
public final class ChainStepContactCheck {
    private static int assertionCount;

    private ChainStepContactCheck() {
    }

    public static void main(String[] arguments) {
        ModTechniques.registerAll();
        ordinaryAttacksSplit();
        tideOnCurtainStaysTogether();
        swordComeRejectsContact();
        System.out.println("断言 " + assertionCount);
        System.out.println("链步和接触承载检查通过");
    }

    private static void ordinaryAttacksSplit() {
        Skill skill = new Skill("两段攻击", SkillType.SWORD,
                List.of(ModTechniques.SWORD_THRUST, ModTechniques.TIDE_SEVER),
                List.of(ShuAttribute.NONE, ShuAttribute.WATER));
        List<List<Integer>> steps = ChainSteps.list(skill);
        check(steps.size() == 2, "普通攻击 A+B 分成两步");
        check(steps.get(0).size() == 1 && steps.get(0).get(0) == 0, "第一步只有突刺");
        check(steps.get(1).size() == 1 && steps.get(1).get(0) == 1, "第二步只有断潮");
    }

    private static void tideOnCurtainStaysTogether() {
        var curtain = TechniqueManager.getById("water_curtain");
        check(curtain != null && curtain.getBaseDamage() == 0f, "水幕是零伤害宿主");
        Skill skill = new Skill("潮印水幕", SkillType.SWORD,
                List.of(ModTechniques.TIDE_MARK, curtain),
                List.of(ShuAttribute.WATER, ShuAttribute.WATER));
        List<List<Integer>> steps = ChainSteps.list(skill);
        check(steps.size() == 1, "潮印和零伤害水幕在同一步");
        check(steps.get(0).size() == 2 && steps.get(0).get(0) == 0 && steps.get(0).get(1) == 1,
                "这一步先附魔再宿主");
        float contactDamage = EnchantmentSnapshot.damageOf(skill, List.of(0));
        check(contactDamage == 2f, "单条潮印接触伤害是 2");
        String refusal = ContactCast.refusal(skill, List.of(new ChainSegment(1, List.of(0))));
        check(refusal == null, "水幕能承载这条潮印");
    }

    private static void swordComeRejectsContact() {
        Skill skill = new Skill("潮印剑来", SkillType.SWORD,
                List.of(ModTechniques.TIDE_MARK, ModTechniques.SWORD_COME),
                List.of(ShuAttribute.WATER, ShuAttribute.NONE));
        List<List<Integer>> steps = ChainSteps.list(skill);
        check(steps.size() == 1 && steps.get(0).size() == 2, "不能承载的零伤害宿主仍和附魔同一步");
        String refusal = ContactCast.refusal(skill, List.of(new ChainSegment(1, List.of(0))));
        check(refusal != null && refusal.contains("还不能承载"), "剑来在扣费前被接触检查拒绝");
    }

    private static void check(boolean passed, String label) {
        assertionCount++;
        if (!passed) {
            throw new IllegalStateException(label);
        }
    }
}
