package com.bslx.XueXiMod.shu.technique.release.motion;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class TrapFallMotion implements TechniqueMotion {
    @Override
    public void tick(TechniqueHitEntity entity, ServerLevel level) {
        if (entity.planted()) {
            return;
        }
        Vec3 pos = entity.position();
        Vec3 next = pos.add(0.0, -0.35, 0.0);
        BlockPos below = BlockPos.containing(next.x, next.y - 0.05, next.z);
        if (next.y <= level.getMinBuildHeight() || !level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
            double top = below.getY() + 1.0;
            entity.placeAt(new Vec3(pos.x, Math.max(top, next.y), pos.z), new Vec3(0.0, 1.0, 0.0));
            entity.setPlanted(true);
            return;
        }
        entity.placeAt(next, new Vec3(0.0, -1.0, 0.0));
    }
}
