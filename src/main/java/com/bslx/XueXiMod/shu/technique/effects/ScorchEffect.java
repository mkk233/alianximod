package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.item.StaffSkills;
import com.bslx.XueXiMod.network.BatchCastSession;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.chain.TechniqueChain;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantBonus;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.batch.BirthLayout;
import com.bslx.XueXiMod.shu.technique.release.bind.BoundReleasePlan;
import com.bslx.XueXiMod.shu.technique.release.bind.PublicationAttempt;
import com.bslx.XueXiMod.shu.technique.release.firecast.SurfaceFireHosts;
import com.bslx.XueXiMod.skill.Skill;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

/** 灼地。只打开贴地宿主，不生成命中体，也不走燎原的圆盘和消耗。 */
public final class ScorchEffect implements TechniqueEffect {
    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        PublicationAttempt attempt = PublicationAttempt.current();
        if (attempt != null && !attempt.allowsPublication()) {
            return;
        }
        if (player == null || technique == null || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ReleaseBatch batch = ReleaseBatch.current();
        if (batch == null) {
            return;
        }
        BoundReleasePlan plan = ReleaseBatch.activePlan();
        EnchantmentSnapshot snapshot = ReleaseBatch.activeSnapshot();
        ResourceEnchantLoad load = sameCastLoad(player, technique, weapon, snapshot);
        UUID rootCastId = BatchCastSession.currentRootId(player);
        boolean opened = SurfaceFireHosts.open(serverLevel, player, batch, plan, snapshot, load, rootCastId);
        if (opened && attempt != null) {
            attempt.reportAccepted();
        }
    }

    /**
     * 批量出生槽上的载荷就是这次 {@code ResourceEnchantCatalog.collect}，和 {@link EnchantBonus#resolve} 用的是同一组下标。
     * 普通链没有出生槽时，用这次宿主快照的附魔标识对上法杖链，再解析同一次附魔奖励。对不上就不编一份假载荷。
     */
    private static ResourceEnchantLoad sameCastLoad(ServerPlayer player, Technique technique, ItemStack weapon,
                                                    EnchantmentSnapshot snapshot) {
        BirthLayout.Slot laid = BirthLayout.current();
        if (laid != null) {
            return laid.resourceLoad == null ? ResourceEnchantLoad.EMPTY : laid.resourceLoad;
        }
        if (snapshot == null || technique == null || technique.getId() == null) {
            return ResourceEnchantLoad.EMPTY;
        }
        Skill skill = StaffSkills.asSkill(weapon);
        if (skill == null) {
            return ResourceEnchantLoad.EMPTY;
        }
        List<Technique> techniques = skill.getTechniques();
        List<ChainSegment> segments = TechniqueChain.split(skill);
        int cursor = 0;
        while (cursor < segments.size()) {
            ChainSegment segment = segments.get(cursor);
            cursor++;
            int actionIndex = segment.actionIndex();
            if (actionIndex < 0 || actionIndex >= techniques.size()) {
                continue;
            }
            Technique action = techniques.get(actionIndex);
            if (action == null || !technique.getId().equals(action.getId())) {
                continue;
            }
            EnchantmentSnapshot rebuilt = EnchantmentSnapshot.from(skill, segment, player);
            if (!sameTypes(snapshot, rebuilt)) {
                continue;
            }
            EnchantBonus bonus = EnchantBonus.resolve(skill, segment.enchantIndexes(), technique.getBaseDamage(), 1);
            return bonus.resourceLoad == null ? ResourceEnchantLoad.EMPTY : bonus.resourceLoad;
        }
        return ResourceEnchantLoad.EMPTY;
    }

    private static boolean sameTypes(EnchantmentSnapshot active, EnchantmentSnapshot rebuilt) {
        if (active == null) {
            return false;
        }
        List<String> left = active.basicTypeIds();
        List<String> right = rebuilt == null ? List.of() : rebuilt.basicTypeIds();
        if (left.size() != right.size()) {
            return false;
        }
        int cursor = 0;
        while (cursor < left.size()) {
            if (!left.get(cursor).equals(right.get(cursor))) {
                return false;
            }
            cursor++;
        }
        return true;
    }
}
