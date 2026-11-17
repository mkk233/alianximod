package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.debug.DebugLog;
import com.bslx.XueXiMod.shu.technique.effects.LockTargetService;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public final class HitVolumes {
    private static final ConcurrentLinkedQueue<Volume> ACTIVE = new ConcurrentLinkedQueue<>();
    private static final ConcurrentHashMap<UUID, Beam> BEAMS = new ConcurrentHashMap<>();

    private HitVolumes() {}

    public static void staticBox(ServerLevel level, Vec3 origin, Vec3 look,
                                 float width, float length, float thickness,
                                 int lifetimeTicks, Consumer<LivingEntity> onHit,
                                 Consumer<Vec3> onExpire) {
        Vec3 end = origin.add(look.scale(length));
        AABB box = new AABB(origin, end).inflate(width / 2.0, thickness / 2.0, width / 2.0);
        ACTIVE.add(new Volume(level, box, null, null, 0f, 0.0, lifetimeTicks, onHit, onExpire,
                Trajectory.STRAIGHT, null, false, false, false));
    }

    public static void movingBox(ServerLevel level, Vec3 origin, Vec3 direction,
                                 float size, float speed, int lifetimeTicks,
                                 Consumer<LivingEntity> onHit, Consumer<Vec3> onExpire) {
        movingBox(level, origin, direction, size, speed, lifetimeTicks, onHit, onExpire,
                Trajectory.STRAIGHT, null, false);
    }

    public static void movingBox(ServerLevel level, Vec3 origin, Vec3 direction,
                                 float size, float speed, int lifetimeTicks,
                                 Consumer<LivingEntity> onHit, Consumer<Vec3> onExpire,
                                 Trajectory trajectory, UUID launcherUuid, boolean lockThisCast) {
        AABB box = new AABB(origin, origin).inflate(size / 2.0);
        ACTIVE.add(new Volume(level, box, origin, direction, size, speed, lifetimeTicks, onHit, onExpire,
                trajectory, launcherUuid, lockThisCast, true, true));
    }

    public static void beam(ServerLevel level, ServerPlayer owner, int lifetimeTicks, float damage,
                            boolean lockThisCast, boolean dropThisCast,
                            Consumer<LivingEntity> onHit, Consumer<Vec3> onExpire) {
        BEAMS.put(owner.getUUID(), new Beam(level, owner, lifetimeTicks, damage, lockThisCast, dropThisCast, onHit, onExpire));
    }

    @Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
    public static final class TickHandler {
        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Iterator<Volume> iterator = ACTIVE.iterator();
            while (iterator.hasNext()) {
                Volume volume = iterator.next();
                volume.remainingTicks--;
                if (volume.remainingTicks <= 0) {
                    iterator.remove();
                    volume.discardVisual();
                    if (volume.onExpire != null) {
                        try {
                            volume.onExpire.accept(volume.box.getCenter());
                        } catch (Throwable throwable) {
                            DebugLog.log("命中盒过期回调异常：" + throwable);
                        }
                    }
                    continue;
                }
                if (volume.moving) {
                    volume.age++;
                    Vec3 targetPoint = null;
                    if (volume.lockThisCast) {
                        LivingEntity target = LockTargetService.getMarkedTarget(volume.level, volume.launcherUuid);
                        if (target != null) {
                            targetPoint = target.getBoundingBox().getCenter();
                        }
                    }
                    TrajectorySampler.Sample sample = TrajectorySampler.sample(
                            volume.trajectory, volume.origin, volume.forward, volume.speed,
                            volume.lifetimeTicks, volume.age, targetPoint, false, volume.forward, null);
                    volume.box = new AABB(sample.position(), sample.position()).inflate(volume.size / 2.0);
                    volume.direction = sample.direction();
                    volume.syncVisual();
                }
                List<LivingEntity> entities = volume.level.getEntitiesOfClass(
                        LivingEntity.class, volume.box,
                        entity -> entity.isAlive() && !volume.hit.contains(entity));
                for (LivingEntity entity : entities) {
                    volume.hit.add(entity);
                    try {
                        volume.onHit.accept(entity);
                    } catch (Throwable throwable) {
                        DebugLog.log("命中盒回调异常：" + throwable);
                    }
                }
            }

            Iterator<Beam> beams = BEAMS.values().iterator();
            while (beams.hasNext()) {
                Beam beam = beams.next();
                beam.remainingTicks--;
                if (beam.remainingTicks <= 0) {
                    beams.remove();
                    ServerPlayer expiredOwner = beam.level.getServer().getPlayerList().getPlayer(beam.ownerUuid);
                    Vec3 expirePoint = expiredOwner != null
                            ? expiredOwner.getEyePosition().add(expiredOwner.getLookAngle().scale(6.0))
                            : beam.lastPoint;
                    if (beam.onExpire != null) {
                        try {
                            beam.onExpire.accept(expirePoint);
                        } catch (Throwable throwable) {
                            DebugLog.log("光束过期回调异常：" + throwable);
                        }
                    }
                    continue;
                }
                ServerPlayer owner = beam.level.getServer().getPlayerList().getPlayer(beam.ownerUuid);
                if (owner == null || owner.isDeadOrDying() || owner.isRemoved()
                        || owner.hasDisconnected() || owner.serverLevel() != beam.level) {
                    beams.remove();
                    if (beam.onExpire != null) {
                        try {
                            beam.onExpire.accept(owner != null ? owner.getEyePosition() : beam.lastPoint);
                        } catch (Throwable throwable) {
                            DebugLog.log("光束过期回调异常：" + throwable);
                        }
                    }
                    continue;
                }
                Vec3 origin = beam.dropThisCast ? beam.launchOrigin : owner.getEyePosition();
                Vec3 look = beam.dropThisCast ? beam.launchLook : owner.getLookAngle();
                Vec3 targetPoint = null;
                if (beam.lockThisCast) {
                    LivingEntity target = LockTargetService.getMarkedTarget(beam.level, beam.ownerUuid);
                    if (target != null) {
                        targetPoint = target.getBoundingBox().getCenter();
                    }
                }
                Trajectory trajectory = beam.dropThisCast ? Trajectory.DROP : Trajectory.FOLLOW_LOOK;
                beam.age++;
                TrajectorySampler.Sample sample = TrajectorySampler.sample(
                        trajectory, origin, look, 0.5, beam.lifetimeTicks, beam.age, targetPoint,
                        !beam.dropThisCast, look, null);
                Vec3 unit = sample.direction().normalize();
                Vec3 direction = unit.scale(6.0);
                Vec3 start = beam.dropThisCast ? sample.position() : origin;
                Vec3 end = start.add(direction);
                beam.lastPoint = end;
                AABB searchBox = new AABB(start, end).inflate(1.5);
                List<LivingEntity> entities = beam.level.getEntitiesOfClass(LivingEntity.class, searchBox,
                        candidate -> candidate.isAlive() && candidate != owner && !candidate.isSpectator());
                for (LivingEntity entity : entities) {
                    AABB hitBox = entity.getBoundingBox().inflate(0.5);
                    if (!hitBox.contains(start) && hitBox.clip(start, end).isEmpty()) {
                        continue;
                    }
                    long now = beam.level.getGameTime();
                    Long last = beam.lastHitTick.get(entity.getUUID());
                    if (last != null && now - last < 10L) {
                        continue;
                    }
                    boolean hurtApplied = TechniqueHits.hurtRoot(entity, owner, beam.damage);
                    if (hurtApplied) {
                        beam.lastHitTick.put(entity.getUUID(), now);
                        if (beam.onHit != null) {
                            try {
                                beam.onHit.accept(entity);
                            } catch (Throwable throwable) {
                                DebugLog.log("光束命中回调异常：" + throwable);
                            }
                        }
                    }
                }
                for (int segmentIndex = 0; segmentIndex <= 6; segmentIndex++) {
                    Vec3 point = start.add(unit.scale(segmentIndex));
                    beam.level.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }

        @SubscribeEvent
        public static void onProjectileImpact(ProjectileImpactEvent event) {
            if (event.getEntity().getPersistentData().getBoolean("xueximod_visual")) {
                event.setCanceled(true);
            }
        }
    }

    private static final class Volume {
        final ServerLevel level;
        final Vec3 origin;
        final Vec3 forward;
        final double speed;
        final float size;
        final int lifetimeTicks;
        final Trajectory trajectory;
        final UUID launcherUuid;
        final boolean lockThisCast;
        final boolean moving;
        AABB box;
        Vec3 direction;
        int remainingTicks;
        int age;
        final ShulkerBullet visual;
        final Consumer<LivingEntity> onHit;
        final Consumer<Vec3> onExpire;
        final Set<LivingEntity> hit = new HashSet<>();

        Volume(ServerLevel level, AABB box, Vec3 origin, Vec3 forward, float size, double speed,
               int lifetimeTicks, Consumer<LivingEntity> onHit, Consumer<Vec3> onExpire,
               Trajectory trajectory, UUID launcherUuid, boolean lockThisCast, boolean moving,
               boolean withVisual) {
            this.level = level;
            this.box = box;
            this.origin = origin;
            this.forward = forward;
            this.size = size;
            this.speed = speed;
            this.lifetimeTicks = lifetimeTicks;
            this.remainingTicks = lifetimeTicks;
            this.trajectory = trajectory;
            this.launcherUuid = launcherUuid;
            this.lockThisCast = lockThisCast;
            this.moving = moving;
            this.onHit = onHit;
            this.onExpire = onExpire;
            this.visual = withVisual ? EntityType.SHULKER_BULLET.create(level) : null;
            if (this.visual != null) {
                Vec3 center = box.getCenter();
                this.visual.setPos(center.x, center.y, center.z);
                this.visual.setNoGravity(true);
                this.visual.noPhysics = true;
                this.visual.setSilent(true);
                this.visual.getPersistentData().putBoolean("xueximod_visual", true);
                level.addFreshEntity(this.visual);
            }
        }

        void discardVisual() {
            if (this.visual != null && this.visual.isAlive()) {
                this.visual.discard();
            }
        }

        void syncVisual() {
            if (this.visual == null || !this.visual.isAlive() || this.direction == null) {
                return;
            }
            Vec3 center = this.box.getCenter();
            this.visual.setPos(center.x, center.y, center.z);
            this.visual.setDeltaMovement(0.0, 0.0, 0.0);
            Vec3 heading = this.direction;
            double horizontal = Math.sqrt(heading.x * heading.x + heading.z * heading.z);
            float yaw = (float) (Math.atan2(heading.z, heading.x) * (180.0 / Math.PI)) - 90.0F;
            float pitch = (float) (-Math.atan2(heading.y, horizontal) * (180.0 / Math.PI));
            this.visual.setYRot(yaw);
            this.visual.setXRot(pitch);
            this.visual.yRotO = yaw;
            this.visual.xRotO = pitch;
        }
    }

    private static final class Beam {
        final ServerLevel level;
        final UUID ownerUuid;
        final float damage;
        final boolean lockThisCast;
        final boolean dropThisCast;
        final int lifetimeTicks;
        final Consumer<LivingEntity> onHit;
        final Consumer<Vec3> onExpire;
        int remainingTicks;
        int age;
        Vec3 lastPoint;
        final Vec3 launchOrigin;
        final Vec3 launchLook;
        final Map<UUID, Long> lastHitTick = new HashMap<>();

        Beam(ServerLevel level, ServerPlayer owner, int lifetimeTicks, float damage,
             boolean lockThisCast, boolean dropThisCast,
             Consumer<LivingEntity> onHit, Consumer<Vec3> onExpire) {
            this.level = level;
            this.ownerUuid = owner.getUUID();
            this.damage = damage;
            this.lockThisCast = lockThisCast;
            this.dropThisCast = dropThisCast;
            this.lifetimeTicks = lifetimeTicks;
            this.onHit = onHit;
            this.onExpire = onExpire;
            this.remainingTicks = lifetimeTicks;
            this.launchOrigin = owner.getEyePosition();
            this.launchLook = owner.getLookAngle();
            this.lastPoint = this.launchOrigin;
        }
    }
}
