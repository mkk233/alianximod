package com.bslx.XueXiMod.shu.technique.sword;

import com.bslx.XueXiMod.shu.technique.effects.LockTargetService;
import com.bslx.XueXiMod.shu.technique.release.aim.LaunchAim;
import com.bslx.XueXiMod.shu.technique.release.motion.TrackingSupport;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class SwordVortex {
    private static final int SPIN_DEGREES = 12;
    private static final int WINDUP_TICKS = 24;
    private static final int LAUNCH_GAP_TICKS = 6;
    private static final double VERTICAL_REACH = 1.8;
    private static final double ORBIT_LIFT = 1.0;
    private static final double GOLDEN_TURN = Math.PI * (3.0 - Math.sqrt(5.0));
    private static int plannedTick = Integer.MIN_VALUE;
    private static final Map<UUID, Set<UUID>> LAUNCHING = new HashMap<>();

    static void assignOrbit(SwordEntity sword) {
        int swordCount = Math.max(1, sword.vortexCount);
        int slot = Mth.clamp(sword.vortexSlot, 0, swordCount - 1);
        double elevation = swordCount == 1 ? 0.0 : 1.0 - (2.0 * slot) / (swordCount - 1);
        double azimuth = slot * GOLDEN_TURN;
        elevation += (sword.nextOrbitRoll() - 0.5) * 0.45;
        azimuth += (sword.nextOrbitRoll() - 0.5) * 1.1;
        sword.orbitElevation = Mth.clamp(elevation, -1.0, 1.0);
        sword.orbitAzimuth = azimuth;
        sword.orbitRadius = 4.0 + sword.nextOrbitRoll() * 1.4;
        sword.orbitBobPhase = sword.nextOrbitRoll() * Math.PI * 2.0;
    }

    static void arm(SwordEntity sword) {
        if (!(sword.level() instanceof ServerLevel serverLevel) || serverLevel.getServer() == null) {
            return;
        }
        sword.vortexLaunchAt = serverLevel.getServer().getTickCount() + WINDUP_TICKS;
    }

    static void tick(SwordEntity sword, ServerLevel level) {
        Player ownerPlayer = level.getPlayerByUUID(sword.owner);
        if (!(ownerPlayer instanceof ServerPlayer owner) || !owner.isAlive() || owner.level() != level) {
            if (sword.isPhantom()) {
                sword.discard();
            } else {
                SwordEntityMotion.dropRealItem(sword, level);
            }
            return;
        }
        sword.vortexAge++;
        place(sword, owner);
        if (owner.getServer() == null) {
            return;
        }
        int nowTick = owner.getServer().getTickCount();
        if (plannedTick != nowTick) {
            plannedTick = nowTick;
            LAUNCHING.clear();
        }
        Set<UUID> due = LAUNCHING.get(sword.owner);
        if (due == null) {
            if (nowTick < sword.vortexLaunchAt) {
                return;
            }
            due = planWave(level, sword.owner, nowTick);
            LAUNCHING.put(sword.owner, due);
        }
        if (due.contains(sword.getUUID())) {
            launch(sword, level, owner);
        }
    }

    /** 这一拍只安排一次。按剩余剑数降档；初始至少 100 把时，最低保留 99 把对应的档位。 */
    private static Set<UUID> planWave(ServerLevel level, UUID ownerId, int nowTick) {
        List<SwordEntity> remaining = new ArrayList<>();
        for (SwordEntity orbiting : SwordEntity.ownedBy(level, ownerId)) {
            if (orbiting.isVortex() && orbiting.level() == level) {
                remaining.add(orbiting);
            }
        }
        remaining.sort(Comparator.comparingInt(orbiting -> orbiting.vortexSlot));
        boolean keepMinimumWave = remaining.stream().anyMatch(orbiting -> orbiting.vortexCount >= 100);
        int waveSize = simultaneousCount(remaining.size());
        if (keepMinimumWave) {
            waveSize = Math.min(remaining.size(), Math.max(waveSize, simultaneousCount(99)));
        }
        Set<UUID> due = new HashSet<>();
        int nextAt = nowTick + LAUNCH_GAP_TICKS;
        int taken = 0;
        for (SwordEntity orbiting : remaining) {
            if (taken < waveSize) {
                due.add(orbiting.getUUID());
                taken++;
            }
            orbiting.vortexLaunchAt = nextAt;
        }
        return due;
    }

    /** 按还在风暴里的剑数决定这一波射几把。100 把以内每满 20 把翻一倍，超过 100 把后每再满 100 把才翻一倍。 */
    private static int simultaneousCount(int swordCount) {
        int groups = swordCount <= 100
                ? swordCount / 20
                : 5 + (swordCount - 100) / 100;
        if (groups <= 0) {
            return 1;
        }
        if (groups > 16) {
            groups = 16;
        }
        int widened = 1 << groups;
        return widened > swordCount ? swordCount : widened;
    }

    /** 本拍只解析一次目标。没咬住时不改方向；咬住时写入有界步长，供飞行段消费。 */
    static GuidedStep steer(SwordEntity sword, ServerLevel level, double baseSpeed) {
        if (!sword.trackingEnabled || sword.realFalls) {
            return GuidedStep.idle();
        }
        if (sword.hit.size() >= 5) {
            sword.trackingTarget = null;
            return GuidedStep.idle();
        }
        LaunchAim aim = sword.launchAim();
        if (aim != null && aim.automaticLifetime()) {
            TrackingSupport.bindAutomaticAim(aim);
        }
        LivingEntity target;
        try {
            target = TrackingSupport.resolveTarget(
                    level, sword.position(), sword.trackingTarget,
                    entity -> SwordEntityMotion.canHit(sword, entity) && !sword.hit.contains(entity.getUUID()));
        } finally {
            TrackingSupport.clearAutomaticAim();
        }
        sword.trackingTarget = target == null ? null : target.getUUID();
        if (target == null) {
            return GuidedStep.idle();
        }
        Vec3 towardTarget = target.getBoundingBox().getCenter().subtract(sword.position());
        SwordTrackingStep planned = SwordTrackingStep.plan(sword.flyDirection, towardTarget, baseSpeed);
        sword.flyDirection = planned.direction;
        return GuidedStep.acquired(planned);
    }

    private static void place(SwordEntity sword, ServerPlayer owner) {
        double spin = Math.toRadians(SPIN_DEGREES) * sword.vortexAge;
        double yaw = sword.orbitAzimuth + spin;
        double elevation = Mth.clamp(sword.orbitElevation, -1.0, 1.0);
        double ringFactor = Math.sqrt(Math.max(0.0, 1.0 - elevation * elevation));
        double horizontalRadius = 4.0 + (sword.orbitRadius - 4.0) * (0.42 + 0.58 * ringFactor);
        double bob = Math.sin(sword.vortexAge * 0.18 + sword.orbitBobPhase) * 0.35;
        Vec3 bodyCenter = sword.controlAnchor();
        if (bodyCenter == null) {
            bodyCenter = owner.position().add(0.0, owner.getBbHeight() * 0.5, 0.0);
        }
        sword.setPos(
                bodyCenter.x + Math.cos(yaw) * horizontalRadius,
                bodyCenter.y + ORBIT_LIFT + elevation * VERTICAL_REACH + bob,
                bodyCenter.z + Math.sin(yaw) * horizontalRadius);
    }

    private static void launch(SwordEntity sword, ServerLevel level, ServerPlayer owner) {
        LaunchAim aim = sword.launchAim();
        if (aim != null && aim.automaticLifetime()) {
            LivingEntity localTarget = aim.findTarget(level);
            boolean localReady = acceptsMarked(sword, level, localTarget);
            Vec3 chosen = automaticLaunchDirection(sword, localTarget, localReady, aim.direction());
            aim.rememberDirection(chosen);
            sword.flyDirection = chosen;
            sword.beginSecondFlight();
            sword.homeToMark = false;
            sword.trackingEnabled = true;
            UUID markedId = localReady ? localTarget.getUUID() : null;
            boolean alreadyStruck = markedId != null && sword.hit.contains(markedId);
            sword.trackingTarget = SwordTrackingStep.retainedMark(markedId, localReady, alreadyStruck);
            SwordEntityMotion.aim(sword, sword.flyDirection);
            return;
        }
        LivingEntity marked = LockTargetService.getMarkedTarget(level, sword.owner);
        boolean markedReady = acceptsMarked(sword, level, marked);
        sword.flyDirection = launchDirection(owner, sword, marked, markedReady);
        sword.beginSecondFlight();
        sword.homeToMark = false;
        sword.trackingEnabled = true;
        UUID markedId = markedReady ? marked.getUUID() : null;
        boolean alreadyStruck = markedId != null && sword.hit.contains(markedId);
        sword.trackingTarget = SwordTrackingStep.retainedMark(markedId, markedReady, alreadyStruck);
        SwordEntityMotion.aim(sword, sword.flyDirection);
    }

    /** 与追踪选敌同一套：活着、同一维度、可命中。未命中在清理后再判。 */
    private static boolean acceptsMarked(SwordEntity sword, ServerLevel level, LivingEntity marked) {
        return marked != null
                && !marked.isRemoved()
                && marked.level() == level
                && SwordEntityMotion.canHit(sword, marked);
    }

    private static Vec3 automaticLaunchDirection(SwordEntity sword, LivingEntity localTarget, boolean localReady, Vec3 backup) {
        if (localReady && localTarget != null) {
            Vec3 towardTarget = localTarget.getBoundingBox().getCenter().subtract(sword.position());
            if (towardTarget.lengthSqr() >= 1.0E-6) {
                return towardTarget.normalize();
            }
        }
        if (backup != null && backup.lengthSqr() >= 1.0E-6) {
            return backup.normalize();
        }
        return new Vec3(0.0, 0.0, 1.0);
    }

    private static Vec3 launchDirection(ServerPlayer owner, SwordEntity sword, LivingEntity marked, boolean markedReady) {
        if (markedReady) {
            Vec3 towardTarget = marked.getBoundingBox().getCenter().subtract(sword.position());
            if (towardTarget.lengthSqr() >= 1.0E-6) {
                return towardTarget.normalize();
            }
        }
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() < 1.0E-6) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return look.normalize();
    }

    static final class GuidedStep {
        final boolean acquired;
        final Vec3 direction;
        final double stepLength;

        private GuidedStep(boolean acquired, Vec3 direction, double stepLength) {
            this.acquired = acquired;
            this.direction = direction;
            this.stepLength = stepLength;
        }

        static GuidedStep idle() {
            return new GuidedStep(false, Vec3.ZERO, 0.0);
        }

        static GuidedStep acquired(SwordTrackingStep planned) {
            return new GuidedStep(true, planned.direction, planned.stepLength);
        }
    }
}
