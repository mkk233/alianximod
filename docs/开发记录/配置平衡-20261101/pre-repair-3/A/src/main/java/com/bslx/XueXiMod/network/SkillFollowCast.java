package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.player.MagicPool;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantBonus;
import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.ReleaseTag;
import com.bslx.XueXiMod.skill.Skill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class SkillFollowCast {
    static Consumer<Vec3> buildRuleTrigger(ServerPlayer player, Skill skill,
                                            List<Technique> techniques, ItemStack weapon,
                                            List<ChainSegment> segments, int ruleIndex, float weaponBonus, int slot,
                                            double cooldownReduction, Double attributeDamageBonus) {
        if (SkillChainPlan.opensMulticast(segments, ruleIndex, techniques)) {
            return position -> {};
        }
        ReleaseBatch pressed = ReleaseBatch.current();
        List<ChainSegment> released = SkillChainPlan.releasedSegments(segments, ruleIndex, techniques);
        if (released.isEmpty()) {
            return position -> {};
        }
        ChainSegment aSegment = released.get(0);
        List<ChainSegment> follow = released.subList(1, released.size());

        Technique aAction = techniques.get(aSegment.actionIndex());
        EnchantmentResolution aEnchantments = SkillSegmentMath.resolveSegmentEnchantments(skill, aSegment);
        EnchantBonus aBonus = EnchantBonus.resolve(skill, aSegment.enchantIndexes(), aAction.getBaseDamage(), 1);
        boolean aCarries = aAction.getBaseDamage() > 0 && !aSegment.enchantIndexes().isEmpty();
        float aDamage = hostedDamage(skill, aSegment, aAction, weaponBonus, attributeDamageBonus, aBonus);
        int totalCooldown = SkillSegmentMath.scaleCooldown(SkillSegmentMath.baseCooldownSum(released, techniques), cooldownReduction);

        if (SkillSegmentMath.releaseKind(aAction.getId()) == ReleaseKind.ACTION) {
            Consumer<Vec3> followTrigger = buildFollowTrigger(player, skill, techniques, weapon, segments, follow, weaponBonus, slot, aAction, cooldownReduction, attributeDamageBonus);
            Runnable aRunner = SkillSegmentRunner.buildSegmentRunner(player, aAction, techniques, aSegment.actionIndex(), weapon, aDamage, aCarries, aEnchantments, followTrigger,
                    SkillChainPlan.lockThisCast(techniques, aSegment.actionIndex()), SkillChainPlan.dropThisCast(techniques, segments, aSegment.actionIndex()),
                    null, 0, 1, ReleaseBatch.current(), aBonus, 0, null);
            return parentPosition -> ChainScheduler.schedule(player.getServer(), player.getServer().getTickCount(), aRunner);
        }

        return parentPosition -> {
            Vec3 parent = parentPosition != null ? parentPosition : player.position();
            SkillSegmentRunner.buildSegmentRunnerAt(player, aAction, techniques, aSegment.actionIndex(), weapon, aDamage, aCarries, aEnchantments, null,
                    player.getLookAngle(), parent, SkillChainPlan.lockThisCast(techniques, aSegment.actionIndex()), SkillChainPlan.dropThisCast(techniques, segments, aSegment.actionIndex()),
                    null, 0, 1, pressed, aBonus, 0, null).run();

            int lastTriggerIndex = -1;
            for (int followIndex = 0; followIndex < follow.size(); followIndex++) {
                if (SkillSegmentMath.isTriggerRelease(techniques.get(follow.get(followIndex).actionIndex()).getId())) {
                    lastTriggerIndex = followIndex;
                }
            }

            int tick = player.getServer().getTickCount();
            for (int followIndex = 0; followIndex < follow.size(); followIndex++) {
                ChainSegment seg = follow.get(followIndex);
                Technique tech = techniques.get(seg.actionIndex());
                EnchantmentResolution enchantments = SkillSegmentMath.resolveSegmentEnchantments(skill, seg);
                EnchantBonus bonus = EnchantBonus.resolve(skill, seg.enchantIndexes(), tech.getBaseDamage(), 1);
                boolean carries = tech.getBaseDamage() > 0 && !seg.enchantIndexes().isEmpty();
                float damage = hostedDamage(skill, seg, tech, weaponBonus, attributeDamageBonus, bonus);

                Runnable runner;
                if (followIndex == lastTriggerIndex) {
                    runner = () -> {
                        Vec3 look = player.getLookAngle();
                        Vec3 position = player.position();
                        SkillSegmentRunner.buildSegmentRunnerAt(player, tech, techniques, seg.actionIndex(), weapon, damage, carries, enchantments, null, look, position,
                                SkillChainPlan.lockThisCast(techniques, seg.actionIndex()), SkillChainPlan.dropThisCast(techniques, segments, seg.actionIndex()),
                                null, 0, 1, pressed, bonus, 0, null).run();
                    };
                } else {
                    runner = () -> {
                        Vec3 look = player.getLookAngle();
                        SkillSegmentRunner.buildSegmentRunnerAt(player, tech, techniques, seg.actionIndex(), weapon, damage, carries, enchantments, null, look, parent,
                                SkillChainPlan.lockThisCast(techniques, seg.actionIndex()), SkillChainPlan.dropThisCast(techniques, segments, seg.actionIndex()),
                                null, 0, 1, pressed, bonus, 0, null).run();
                    };
                }
                ChainScheduler.schedule(player.getServer(), tick, runner);
            }

            CastCooldown.mark(player, weapon, slot, tick + totalCooldown);
        };
    }

    static Consumer<Vec3> buildFollowTrigger(ServerPlayer player, Skill skill,
                                              List<Technique> techniques, ItemStack weapon,
                                              List<ChainSegment> segments, List<ChainSegment> follow,
                                              float weaponBonus, int slot, Technique action,
                                              double cooldownReduction, Double attributeDamageBonus) {
        ReleaseBatch pressed = ReleaseBatch.current();
        List<ChainSegment> triggerSegments = new ArrayList<>();
        List<Technique> triggerActions = new ArrayList<>();
        List<Float> triggerDamages = new ArrayList<>();
        List<Boolean> triggerCarries = new ArrayList<>();
        List<EnchantmentResolution> triggerEnchantments = new ArrayList<>();
        List<EnchantBonus> triggerBonuses = new ArrayList<>();

        List<ChainSegment> sustainedSegments = new ArrayList<>();
        List<Technique> sustainedActions = new ArrayList<>();
        List<Float> sustainedDamages = new ArrayList<>();
        List<Boolean> sustainedCarries = new ArrayList<>();
        List<EnchantmentResolution> sustainedEnchantments = new ArrayList<>();
        List<EnchantBonus> sustainedBonuses = new ArrayList<>();

        for (ChainSegment seg : follow) {
            Technique technique = techniques.get(seg.actionIndex());
            ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
            if (definition == null) {
                continue;
            }
            EnchantmentResolution enchantments = SkillSegmentMath.resolveSegmentEnchantments(skill, seg);
            EnchantBonus bonus = EnchantBonus.resolve(skill, seg.enchantIndexes(), technique.getBaseDamage(), 1);
            boolean carries = technique.getBaseDamage() > 0 && !seg.enchantIndexes().isEmpty();
            float damage = hostedDamage(skill, seg, technique, weaponBonus, attributeDamageBonus, bonus);
            if (definition.tag() == ReleaseTag.TRIGGER) {
                triggerSegments.add(seg);
                triggerActions.add(technique);
                triggerDamages.add(damage);
                triggerCarries.add(carries);
                triggerEnchantments.add(enchantments);
                triggerBonuses.add(bonus);
            } else if (definition.tag() == ReleaseTag.SUSTAINED) {
                sustainedSegments.add(seg);
                sustainedActions.add(technique);
                sustainedDamages.add(damage);
                sustainedCarries.add(carries);
                sustainedEnchantments.add(enchantments);
                sustainedBonuses.add(bonus);
            }
        }

        int triggerManaCostSum = 0;
        for (ChainSegment seg : triggerSegments) {
            triggerManaCostSum += SkillSegmentMath.segmentManaCost(techniques, seg);
        }
        int sustainedManaCostSum = 0;
        for (ChainSegment seg : sustainedSegments) {
            sustainedManaCostSum += SkillSegmentMath.segmentManaCost(techniques, seg);
        }
        final int triggerMana = triggerManaCostSum;
        final int sustainedMana = sustainedManaCostSum;

        int triggerCooldown = SkillSegmentMath.scaleCooldown(SkillSegmentMath.baseCooldownSum(triggerSegments, techniques), cooldownReduction);
        int sustainedCooldown = SkillSegmentMath.baseCooldownSum(sustainedSegments, techniques);
        int interval = SkillSegmentMath.scaleCooldown(Math.max(1,
                SkillSegmentMath.configuredBaseCooldown(action) + sustainedCooldown), cooldownReduction);
        ReleaseDefinition actionDefinition = ReleaseRegistry.get(action.getId());
        int lifetime = actionDefinition != null ? actionDefinition.lifetimeTicks() : 10;

        return position -> {
            int tick = player.getServer().getTickCount();

            int firstCost = triggerMana + sustainedMana;
            boolean firstPaid = MagicPool.trySpend(player, firstCost);
            if (firstPaid && firstCost > 0) {
                MagicPool.showBalance(player);
            }

            if (firstPaid && !triggerSegments.isEmpty()) {
                Vec3 hitPosition = position != null ? position : player.position();
                Vec3 look = player.getLookAngle();
                for (int triggerIndex = 0; triggerIndex < triggerSegments.size(); triggerIndex++) {
                    Technique technique = triggerActions.get(triggerIndex);
                    float damage = triggerDamages.get(triggerIndex);
                    boolean carries = triggerCarries.get(triggerIndex);
                    EnchantmentResolution enchantments = triggerEnchantments.get(triggerIndex);
                    EnchantBonus bonus = triggerBonuses.get(triggerIndex);
                    final int index = triggerIndex;
                    Runnable runner = () -> SkillSegmentRunner.buildSegmentRunnerAt(player, technique, techniques, triggerSegments.get(index).actionIndex(), weapon, damage, carries, enchantments, null,
                            look, hitPosition,
                            SkillChainPlan.lockThisCast(techniques, triggerSegments.get(index).actionIndex()),
                            SkillChainPlan.dropThisCast(techniques, segments, triggerSegments.get(index).actionIndex()),
                            null, 0, 1, pressed, bonus, 0, null).run();
                    if (triggerIndex == 0) {
                        runner.run();
                    } else {
                        ChainScheduler.schedule(player.getServer(), tick, runner);
                    }
                }
                CastCooldown.mark(player, weapon, slot, tick + triggerCooldown);
            }

            if (!sustainedActions.isEmpty()) {
                Runnable round = () -> {
                    for (int sustainedIndex = 0; sustainedIndex < sustainedActions.size(); sustainedIndex++) {
                        SkillSegmentRunner.buildSegmentRunner(player, sustainedActions.get(sustainedIndex), techniques, sustainedSegments.get(sustainedIndex).actionIndex(), weapon,
                                sustainedDamages.get(sustainedIndex), sustainedCarries.get(sustainedIndex),
                                sustainedEnchantments.get(sustainedIndex), null,
                                SkillChainPlan.lockThisCast(techniques, sustainedSegments.get(sustainedIndex).actionIndex()), SkillChainPlan.dropThisCast(techniques, segments, sustainedSegments.get(sustainedIndex).actionIndex()),
                                null, 0, 1, pressed, sustainedBonuses.get(sustainedIndex), 0, null).run();
                    }
                };
                if (firstPaid) {
                    round.run();
                }
                int elapsed = interval;
                while (elapsed < lifetime) {
                    int delay = elapsed;
                    ChainScheduler.schedule(player.getServer(), tick + delay, () -> {
                        int pulseCost = sustainedMana;
                        if (!MagicPool.trySpend(player, pulseCost)) {
                            return;
                        }
                        if (pulseCost > 0) {
                            MagicPool.showBalance(player);
                        }
                        round.run();
                    });
                    elapsed += interval;
                }
            }
        };
    }

    private static float hostedDamage(Skill skill, ChainSegment segment, Technique technique,
                                       float weaponBonus, Double attributeScale, EnchantBonus bonus) {
        EnchantBonus resolved = bonus == null ? EnchantBonus.NONE : bonus;
        int magicDamage = SkillSegmentMath.segmentMagicDamage(skill, segment);
        return SkillSegmentMath.configuredSegmentDamage(technique, segment, weaponBonus, magicDamage,
                attributeScale, resolved);
    }
}
