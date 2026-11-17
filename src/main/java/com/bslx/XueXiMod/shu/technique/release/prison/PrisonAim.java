package com.bslx.XueXiMod.shu.technique.release.prison;

import com.bslx.XueXiMod.registry.ModMobEffects;
import com.bslx.XueXiMod.status.WetnessState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** 眼射线先截方块，再取最近相交的生物或龙部位。不湿的前景目标直接失败，不改挑后面的湿目标。 */
public final class PrisonAim {
    private PrisonAim() {
    }

    /** 在方块截断前选最近的湿生物。前景不湿就失败，不占用目标。 */
    public static LivingEntity select(ServerPlayer caster, double range) {
        if (caster == null || !(caster.level() instanceof ServerLevel level)) {
            return null;
        }
        if (!(range > 0.0D) || Double.isNaN(range) || Double.isInfinite(range)) {
            return null;
        }
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 far = eye.add(look.scale(range));
        BlockHitResult blockHit = level.clip(new ClipContext(
                eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 limit = blockHit.getType() == HitResult.Type.MISS ? far : blockHit.getLocation();
        double reach = eye.distanceTo(limit);
        AABB searchBox = caster.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        Entity nearest = null;
        double nearestDistance = reach + 1.0E-4D;
        for (Entity candidate : level.getEntities(caster, searchBox, entity -> entity != caster && canConsider(entity))) {
            AABB box = candidate.getBoundingBox().inflate(candidate.getPickRadius());
            double distance;
            if (box.contains(eye)) {
                distance = 0.0D;
            } else {
                Optional<Vec3> hit = box.clip(eye, limit);
                if (hit.isEmpty()) {
                    continue;
                }
                distance = eye.distanceTo(hit.get());
            }
            if (distance > reach || distance >= nearestDistance) {
                continue;
            }
            nearestDistance = distance;
            nearest = candidate;
        }
        LivingEntity chosen = resolve(nearest);
        if (chosen == null || chosen == caster || !chosen.isAlive() || chosen.isSpectator()) {
            return null;
        }
        if (WetnessState.read(chosen) <= 0 || chosen.getEffect(ModMobEffects.WETNESS.get()) == null) {
            return null;
        }
        return chosen;
    }

    private static boolean canConsider(Entity candidate) {
        if (candidate == null || !candidate.isAlive() || candidate.isSpectator()) {
            return false;
        }
        return candidate instanceof LivingEntity || candidate instanceof EnderDragonPart;
    }

    private static LivingEntity resolve(Entity nearest) {
        if (nearest instanceof EnderDragonPart part) {
            return part.parentMob;
        }
        if (nearest instanceof LivingEntity living) {
            return living;
        }
        return null;
    }
}
