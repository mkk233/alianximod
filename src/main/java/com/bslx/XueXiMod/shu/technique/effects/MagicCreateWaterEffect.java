package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.release.bind.PublicationAttempt;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class MagicCreateWaterEffect implements TechniqueEffect {
    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        PublicationAttempt attempt = PublicationAttempt.current();
        if (attempt != null && !attempt.allowsPublication()) {
            return;
        }
        Vec3 look = cast.getLook();
        Vec3 castPosition = cast.getPos();
        Vec3 target = castPosition.add(look.scale(5));
        net.minecraft.core.BlockPos targetBlock = new net.minecraft.core.BlockPos((int) target.x, (int) target.y, (int) target.z);
        if (player.level().isEmptyBlock(targetBlock)) {
            boolean placedWater = player.level().setBlock(targetBlock,
                    net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
            if (placedWater && attempt != null) {
                attempt.reportAccepted();
            }
        }
    }
}
