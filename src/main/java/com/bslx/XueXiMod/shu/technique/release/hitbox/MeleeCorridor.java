package com.bslx.XueXiMod.shu.technique.release.hitbox;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import com.bslx.XueXiMod.shu.technique.release.firecast.MeleeCorridorMath;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** 跟随准星的剑前走廊。实心方块挡住后面的目标。 */
public final class MeleeCorridor {
    private MeleeCorridor() {
    }

    /**
     * 先看走廊是否盖住目标盒，再比较接触距离和实心方块距离。
     * 只返回是否挡住，不移动实体，也不改无敌帧或账本。
     */
    public static boolean hits(TechniqueHitEntity entity, LivingEntity target) {
        if (entity == null || target == null) {
            return false;
        }
        Vec3 start = entity.position();
        Vec3 forward = entity.forward();
        if (start == null || forward == null || forward.lengthSqr() < 1.0e-8d) {
            return false;
        }
        Vec3 unit = forward.normalize();
        AABB box = target.getBoundingBox();
        boolean overlap = MeleeCorridorMath.overlaps(start.x, start.y, start.z, unit.x, unit.y, unit.z,
                entity.length(), entity.width(), entity.height(),
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        if (!overlap) {
            return false;
        }
        double halfX = Math.abs(box.maxX - box.minX) * 0.5d;
        double halfY = Math.abs(box.maxY - box.minY) * 0.5d;
        double halfZ = Math.abs(box.maxZ - box.minZ) * 0.5d;
        double alongPad = halfX * Math.abs(unit.x) + halfY * Math.abs(unit.y) + halfZ * Math.abs(unit.z);
        Vec3 center = box.getCenter();
        double contact = MeleeCorridorMath.contactDistance(start.x, start.y, start.z, unit.x, unit.y, unit.z,
                center.x, center.y, center.z, alongPad);
        Vec3 end = start.add(unit.scale(Math.max(entity.length(), 0.0d)));
        BlockHitResult blockHit = entity.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, entity));
        double obstacle = blockHit.getType() == HitResult.Type.BLOCK
                ? start.distanceTo(blockHit.getLocation())
                : Double.POSITIVE_INFINITY;
        return !MeleeCorridorMath.blocked(contact, obstacle);
    }
}
