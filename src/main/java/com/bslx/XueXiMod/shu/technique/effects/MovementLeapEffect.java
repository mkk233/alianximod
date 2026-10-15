package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class MovementLeapEffect implements TechniqueEffect {
    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        // 向上方弹射
        player.setDeltaMovement(player.getDeltaMovement().add(0, 1.5, 0));
        player.hurtMarked = true;
    }
}
