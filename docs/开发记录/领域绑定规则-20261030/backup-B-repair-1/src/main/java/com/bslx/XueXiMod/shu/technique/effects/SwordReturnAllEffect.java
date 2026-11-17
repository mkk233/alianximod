package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
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
        List<SwordEntity> swords = SwordEntity.ownedBy(level, player.getUUID());
        if (swords.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c没有可操纵的剑"));
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
