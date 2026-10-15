package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class CastAnchorEffect implements TechniqueEffect {
    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        ReleaseBatch batch = ReleaseBatch.current();
        if (batch != null) {
            batch.resolveAnchor(player);
        }
    }
}
