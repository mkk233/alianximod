package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class MagicCreateWaterEffect implements TechniqueEffect {
    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        Vec3 look = cast.getLook();
        Vec3 pos = cast.getPos();
        Vec3 target = pos.add(look.scale(5));
        net.minecraft.core.BlockPos blockPos = new net.minecraft.core.BlockPos((int) target.x, (int) target.y, (int) target.z);
        if (player.level().isEmptyBlock(blockPos)) {
            player.level().setBlock(blockPos,
                    net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
        }
    }
}
