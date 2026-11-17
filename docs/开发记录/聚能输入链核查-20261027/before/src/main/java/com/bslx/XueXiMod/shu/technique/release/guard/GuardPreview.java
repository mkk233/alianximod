package com.bslx.XueXiMod.shu.technique.release.guard;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/** 箭还没打到时先把环抬起来。不改箭的碰撞，也不记水势。 */
final class GuardPreview {
    private static final double REACH = 8.0;
    private static final int CANDIDATE_CAP = 16;
    private static final double LOOKAHEAD = 4.0;

    private GuardPreview() {
    }

    static void look(ServerPlayer owner, GuardStates.BodyGuard guard, int now) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return;
        }
        AABB area = owner.getBoundingBox().inflate(REACH);
        List<Projectile> found = level.getEntitiesOfClass(Projectile.class, area, Projectile::isAlive);
        found.sort(Comparator.comparingDouble(projectile -> projectile.distanceToSqr(owner)));
        int scanned = Math.min(CANDIDATE_CAP, found.size());
        Projectile chosen = null;
        double soonest = LOOKAHEAD + 1.0;
        Vec3 chosenDirection = null;
        for (int candidateIndex = 0; candidateIndex < scanned; candidateIndex++) {
            Projectile projectile = found.get(candidateIndex);
            if (projectile.onGround() || projectile.getDeltaMovement().lengthSqr() < 1.0E-4) {
                continue;
            }
            Vec3 relative = projectile.position().subtract(owner.position());
            Vec3 velocity = projectile.getDeltaMovement();
            if (relative.dot(velocity) > 0.0 && relative.lengthSqr() > 1.0) {
                continue;
            }
            double speedSqr = velocity.lengthSqr();
            double approach = speedSqr < 1.0E-6 ? 0.0 : -relative.dot(velocity) / speedSqr;
            if (approach < 0.0) {
                approach = 0.0;
            }
            if (approach > LOOKAHEAD) {
                continue;
            }
            Vec3 future = projectile.position().add(velocity.scale(approach));
            if (future.distanceToSqr(owner.getBoundingBox().getCenter()) > 2.25) {
                continue;
            }
            if (blocked(level, projectile, owner, future)) {
                continue;
            }
            if (approach < soonest) {
                soonest = approach;
                chosen = projectile;
                chosenDirection = owner.position().subtract(projectile.position());
            }
        }
        if (chosen == null || chosenDirection == null) {
            return;
        }
        GuardStates.preview(owner, guard, chosenDirection, chosen.getId(), now);
    }

    private static boolean blocked(ServerLevel level, Projectile projectile, ServerPlayer owner, Vec3 future) {
        BlockHitResult hit = level.clip(new ClipContext(projectile.position(), future,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, projectile));
        if (hit.getType() == HitResult.Type.BLOCK) {
            return true;
        }
        AABB swept = new AABB(projectile.position(), future).inflate(0.3);
        List<Entity> between = level.getEntities(projectile, swept, entity -> entity != owner && entity.isAlive());
        return !between.isEmpty();
    }
}
