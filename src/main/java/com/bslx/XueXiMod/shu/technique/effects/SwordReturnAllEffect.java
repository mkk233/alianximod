package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.aim.LaunchAim;
import com.bslx.XueXiMod.shu.technique.release.bind.PublicationAttempt;
import com.bslx.XueXiMod.shu.technique.sword.SwordEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class SwordReturnAllEffect implements TechniqueEffect {
    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        CastFrame frame = CastFrame.current();
        boolean automatic = LaunchAim.automatic(frame);
        List<SwordEntity> swords = SwordEntity.ownedBy(level, player.getUUID());
        if (swords.isEmpty()) {
            if (!automatic) {
                player.sendSystemMessage(Component.literal("§c没有可操纵的剑"));
            }
            return;
        }
        PublicationAttempt attempt = PublicationAttempt.current();
        if (automatic && (attempt == null || !attempt.allowsPublication())) {
            return;
        }
        int swordCount = swords.size();
        int slot = 0;
        for (SwordEntity sword : swords) {
            sword.beginVortex(slot, swordCount);
            slot++;
        }
    }
}
