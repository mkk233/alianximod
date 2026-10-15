package com.bslx.XueXiMod.shu.technique;

import com.bslx.XueXiMod.shu.Technique;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public interface TechniqueEffect {
    void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast);
}
