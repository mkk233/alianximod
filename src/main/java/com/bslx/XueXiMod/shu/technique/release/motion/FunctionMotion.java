package com.bslx.XueXiMod.shu.technique.release.motion;

import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.TrajectorySampler;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 函数弹道。先找起点正下方最多 48 格的碰撞面，再交给已登记的函数。
 * 找不到承托面时地面点为空。随后可让函数重配体积，不改无敌帧或账本。
 */
public final class FunctionMotion implements TechniqueMotion {
    @Override
    /** 位置和朝向都来自同一次函数采样。体积适配失败时不回滚位置。 */
    public void tick(TechniqueHitEntity entity, ServerLevel level) {
        Vec3 groundPoint = groundBelow(level, entity, entity.origin());
        TrajectorySampler.Sample sample = TrajectorySampler.sample(
                Trajectory.FUNCTION, entity.origin(), entity.forward(), entity.speed(),
                entity.lifetime(), entity.age(), groundPoint, false, entity.forward(), entity.functionId());
        entity.placeAt(sample.position(), sample.direction());
        TrajectorySampler.fitVolume(entity.functionId(), entity, level, groundPoint);
    }

    private static Vec3 groundBelow(ServerLevel level, TechniqueHitEntity entity, Vec3 origin) {
        Vec3 from = origin == null ? entity.position() : origin;
        Vec3 below = from.add(0.0, -48.0, 0.0);
        BlockHitResult hit = level.clip(new ClipContext(from, below, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        Vec3 location = hit.getLocation();
        // 没打到下方方块时不能把射线起点交回去，否则会把起点当成地面。
        if (location == null || from.y - location.y < 0.05) {
            return null;
        }
        return location;
    }
}
