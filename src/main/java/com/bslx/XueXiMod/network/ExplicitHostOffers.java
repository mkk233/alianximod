package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.skill.Skill;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** 按键当时把显式宿主的绑定计划和附魔快照记到这本释放账上。后续复制和跟随读的是同一本账。 */
final class ExplicitHostOffers {
    private ExplicitHostOffers() {
    }

    static void capture(ReleaseBatch batch, Skill skill, List<Technique> techniques, List<ChainSegment> segments,
                        DomainOpening opening, ServerPlayer player, String lane) {
        if (batch == null || skill == null || techniques == null || segments == null || opening == null || player == null) {
            return;
        }
        if (lane == null || lane.isEmpty()) {
            return;
        }
        for (ChainSegment segment : segments) {
            if (segment == null) {
                continue;
            }
            int actionIndex = segment.actionIndex();
            if (actionIndex < 0 || actionIndex >= techniques.size()) {
                continue;
            }
            Technique technique = techniques.get(actionIndex);
            if (technique == null) {
                continue;
            }
            ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
            if (definition == null || !definition.explicitDomainHost()) {
                continue;
            }
            batch.rememberExplicitHost(lane, actionIndex, opening.planFor(actionIndex),
                    EnchantmentSnapshot.from(skill, segment, player));
        }
    }
}
