package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.debug.DebugLog;
import com.bslx.XueXiMod.shu.technique.effects.LockTargetService;
import com.bslx.XueXiMod.shu.technique.query.LivingCandidateQueries;
import com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict;
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

/**
 * 旧的临时命中体积。静止盒、移动盒和持续光束都挂在服务器 tick 上。
 * 不是 {@code TechniqueHitEntity} 的规格或运动。盒子只去重一次，光束另有 10 tick 间隔；
 * 光束伤害走根结算并默认绕过无敌帧，回调不改账本。
 */
public final class HitVolumes {
    private static final ConcurrentLinkedQueue<Volume> ACTIVE = new ConcurrentLinkedQueue<>();
    private static final ConcurrentHashMap<UUID, Beam> BEAMS = new ConcurrentHashMap<>();

    private HitVolumes() {}

    /** 沿视线放一个不移动的盒。同一活体只交给回调一次。 */
    public static void staticBox(ServerLevel level, Vec3 origin, Vec3 look,
                                 float width, float length, float thickness,
                                 int lifetimeTicks, Consumer<LivingEntity> onHit,
                                 Consumer<Vec3> onExpire) {
        Vec3 end = origin.add(look.scale(length));
        AABB box = new AABB(origin, end).inflate(width / 2.0, thickness / 2.0, width / 2.0);
        ACTIVE.add(new Volume(level, box, null, null, 0f, 0.0, lifetimeTicks, onHit, onExpire,
                Trajectory.STRAIGHT, null, false, false, false));
    }

    /** 直线移动盒。不读取锁定目标。 */
    public static void movingBox(ServerLevel level, Vec3 origin, Vec3 direction,
                                 float size, float speed, int lifetimeTicks,
                                 Consumer<LivingEntity> onHit, Consumer<Vec3> onExpire) {
        movingBox(level, origin, direction, size, speed, lifetimeTicks, onHit, onExpire,
                Trajectory.STRAIGHT, null, false);
    }

    /** 按指定弹道移动。锁定时采样点改成标记目标中心，并带一个只用于显示的弹体。 */
    public static void movingBox(ServerLevel level, Vec3 origin, Vec3 direction,
                                 float size, float speed, int lifetimeTicks,
                                 Consumer<LivingEntity> onHit, Consumer<Vec3> onExpire,
                                 Trajectory trajectory, UUID launcherUuid, boolean lockThisCast) {
        AABB box = new AABB(origin, origin).inflate(size / 2.0);
        ACTIVE.add(new Volume(level, box, origin, direction, size, speed, lifetimeTicks, onHit, onExpire,
                trajectory, launcherUuid, lockThisCast, true, true));
    }

    /** 同一施法者的新光束替换旧光束。垂落钉住发射时的眼睛和视线。 */
    public static void beam(ServerLevel level, ServerPlayer owner, int lifetimeTicks, float damage,
                            boolean lockThisCast, boolean dropThisCast,
                            Consumer<LivingEntity> onHit, Consumer<Vec3> onExpire) {
        BEAMS.put(owner.getUUID(), new Beam(level, owner, lifetimeTicks, damage, lockThisCast, dropThisCast, onHit, onExpire));
    }

    @Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
    public static final class TickHandler {
        @SubscribeEvent
        /** 只在 tick 末尾推进。过期、施法者失效和回调异常都不向外抛。 */
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
                    invokeCallback(volume.onExpire, volume.box.getCenter(), "命中盒过期回调异常：");
                    continue;
                }
                if (volume.moving) {
                    volume.age++;
                    Vec3 targetPoint = resolveTargetPoint(volume.level, volume.launcherUuid, volume.lockThisCast);
                    TrajectorySampler.Sample sample = TrajectorySampler.sample(
                            volume.trajectory, volume.origin, volume.forward, volume.speed,
                            volume.lifetimeTicks, volume.age, targetPoint, false, volume.forward, null);
                    volume.box = new AABB(sample.position(), sample.position()).inflate(volume.size / 2.0);
                    volume.direction = sample.direction();
                    volume.syncVisual();
                }
                List<LivingEntity> entities = LivingCandidateQueries.entityQuery(
                        volume.level, volume.box,
                        entity -> entity.isAlive() && !volume.hit.contains(entity));
                for (LivingEntity entity : entities) {
                    volume.hit.add(entity);
                    invokeCallback(volume.onHit, entity, "命中盒回调异常：");
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
                    invokeCallback(beam.onExpire, expirePoint, "光束过期回调异常：");
                    continue;
                }
                ServerPlayer owner = beam.level.getServer().getPlayerList().getPlayer(beam.ownerUuid);
                if (owner == null || owner.isDeadOrDying() || owner.isRemoved()
                        || owner.hasDisconnected() || owner.serverLevel() != beam.level) {
                    beams.remove();
                    invokeCallback(beam.onExpire, owner != null ? owner.getEyePosition() : beam.lastPoint, "光束过期回调异常：");
                    continue;
                }
                Vec3 origin = beam.dropThisCast ? beam.launchOrigin : owner.getEyePosition();
                Vec3 look = beam.dropThisCast ? beam.launchLook : owner.getLookAngle();
                Vec3 targetPoint = resolveTargetPoint(beam.level, beam.ownerUuid, beam.lockThisCast);
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
                List<LivingEntity> entities = LivingCandidateQueries.entityQuery(beam.level, searchBox,
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
                    HitVerdict verdict = TechniqueHits.classifyRoot(entity, owner, beam.damage);
                    if (verdict.continues()) {
                        beam.lastHitTick.put(entity.getUUID(), now);
                        invokeCallback(beam.onHit, entity, "光束命中回调异常：");
                    }
                }
                for (int segmentIndex = 0; segmentIndex <= 6; segmentIndex++) {
                    Vec3 point = start.add(unit.scale(segmentIndex));
                    beam.level.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }

        @SubscribeEvent
        /** 只取消带显示标记的弹体撞击，避免旧体积的视觉弹体打出原版效果。 */
        public static void onProjectileImpact(ProjectileImpactEvent event) {
            if (event.getEntity().getPersistentData().getBoolean("xueximod_visual")) {
                event.setCanceled(true);
            }
        }

        private static Vec3 resolveTargetPoint(ServerLevel level, UUID ownerUuid, boolean lockThisCast) {
            if (!lockThisCast) {
                return null;
            }
            LivingEntity target = LockTargetService.getMarkedTarget(level, ownerUuid);
            return target != null ? target.getBoundingBox().getCenter() : null;
        }

        private static <T> void invokeCallback(Consumer<T> callback, T argument, String errorPrefix) {
            if (callback == null) {
                return;
            }
            try {
                callback.accept(argument);
            } catch (Throwable throwable) {
                DebugLog.log(errorPrefix + throwable);
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
