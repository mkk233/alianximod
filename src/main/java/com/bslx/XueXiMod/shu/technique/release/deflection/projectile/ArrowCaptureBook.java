package com.bslx.XueXiMod.shu.technique.release.deflection.projectile;

import com.bslx.XueXiMod.shu.technique.release.barrier.ProjectileBarrierTracker;
import com.bslx.XueXiMod.shu.technique.release.deflection.CaptureRoute;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHost;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionMarks;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionService;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHosts;
import com.bslx.XueXiMod.shu.technique.release.deflection.hosts.RingDeflectionHost;
import com.bslx.XueXiMod.shu.technique.release.deflection.HostContact;
import com.bslx.XueXiMod.shu.technique.release.deflection.HostKey;
import com.bslx.XueXiMod.shu.technique.release.deflection.ReleaseSample;
import com.bslx.XueXiMod.shu.technique.release.deflection.RouteSample;
import com.bslx.XueXiMod.shu.technique.release.relations.Relations;
import com.bslx.XueXiMod.shu.technique.release.relations.CombatOwnership;
import com.bslx.XueXiMod.shu.technique.release.event.ResponseWindowService;
import com.bslx.XueXiMod.shu.technique.effects.SwordGuardEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 同一支箭被路线带着走。速度和重力记在这里，宿主只给位置和放出方向。
 * 卸载、主人离开或路线失效都走放出，不再扣款、不加完整次数。
 */
public final class ArrowCaptureBook {
    private static final ConcurrentHashMap<UUID, Held> HELD = new ConcurrentHashMap<>();

    private ArrowCaptureBook() {
    }

    /** 这支箭是否正在被捕获路线带着走。 */
    public static boolean holding(UUID arrowId) {
        return arrowId != null && HELD.containsKey(arrowId);
    }

    /** 开始捕获。路线建不起来时立即放出，不再扣款。 */
    public static void start(AbstractArrow arrow, DeflectionHost host, HostContact contact, Vec3 incoming, double speed) {
        if (arrow == null || host == null || contact == null) {
            return;
        }
        ServerLevel level = host.level();
        Vec3 heading = unitHeading(incoming);
        CompoundTag tag = arrow.getPersistentData();
        ArrowBook.writeCapture(tag, speed, arrow.isNoGravity(), arrow.isNoPhysics(), heading, host.key());
        claimOwner(arrow, host, level);
        CaptureRoute route = openRoute(host, contact, incoming, level.getGameTime());
        Held held = new Held(arrow.getUUID(), level, route, speed, arrow.isNoGravity(), arrow.isNoPhysics(), heading,
                host.ownerId(), host.key());
        HELD.put(arrow.getUUID(), held);
        ((ArrowCaptureAccess) arrow).xuexiSetCaptured(true);
        if (route == null) {
            releaseAlong(arrow, held, heading, contact.point());
            return;
        }
        advance(arrow);
    }

    /** 推进一步路线。结束、挡路或宿主失效时放出并返回假。 */
    public static boolean advance(AbstractArrow arrow) {
        Held held = arrow == null ? null : HELD.get(arrow.getUUID());
        if (held == null) {
            return false;
        }
        long gameTick = held.level.getGameTime();
        if (held.lastTick == gameTick) {
            return HELD.containsKey(arrow.getUUID());
        }
        held.lastTick = gameTick;
        if (!stillUsable(held)) {
            releaseHeld(arrow, held);
            return false;
        }
        if (ringGuardPaused(held)) {
            return true;
        }
        try {
            RouteSample sample = held.route.sample(gameTick);
            if (sample == null || sample.finished() || !readActive(held.route)) {
                ReleaseSample exit = held.route.emergencyRelease(gameTick);
                place(arrow, held, exit.position(), exit.unitDirection(), gameTick);
                return false;
            }
            Vec3 next = sample.position();
            if (blocked(arrow, arrow.position(), next)) {
                releaseHeld(arrow, held);
                return false;
            }
            Vec3 step = next.subtract(arrow.position());
            Vec3 facing = step.lengthSqr() > 1.0E-8 ? step : held.incoming;
            move(arrow, next, facing);
            arrow.setDeltaMovement(Vec3.ZERO);
            arrow.hasImpulse = true;
            ArrowBook.writeExit(arrow.getPersistentData(), unitHeading(facing));
            return true;
        } catch (RuntimeException failure) {
            releaseHeld(arrow, held);
            return false;
        }
    }

    /** 放出这一支箭。没有捕获记录时什么都不做。 */
    public static void releaseOne(AbstractArrow arrow) {
        if (arrow == null) {
            return;
        }
        Held held = HELD.get(arrow.getUUID());
        if (held != null) {
            releaseHeld(arrow, held);
        }
    }

    /** 重新加载后恢复自由飞行。不重新扣水势，也不再加完整次数。 */
    public static void resumeLoaded(AbstractArrow arrow) {
        if (arrow == null) {
            return;
        }
        CompoundTag tag = arrow.getPersistentData();
        if (!ArrowBook.captured(tag) || HELD.containsKey(arrow.getUUID())) {
            return;
        }
        Vec3 heading = unitHeading(ArrowBook.exitOrIncoming(tag));
        arrow.setDeltaMovement(heading.scale(ArrowBook.savedSpeed(tag)));
        move(arrow, arrow.position(), heading);
        arrow.hasImpulse = true;
        HostKey savedHost = ArrowBook.captureHost(tag);
        ArrowBook.markSafeRelease(tag);
        if (savedHost != null) {
            ArrowBook.latchExit(tag, savedHost);
        }
        ArrowBook.leaveContact(tag, arrow.getUUID());
        ProjectileBarrierTracker.releaseSampling(arrow);
        ((ArrowCaptureAccess) arrow).xuexiSetCaptured(false);
    }

    /** 放出这个主人名下的捕获箭。 */
    public static void releaseOwned(UUID playerId) {
        if (playerId == null) {
            return;
        }
        for (Held held : List.copyOf(HELD.values())) {
            if (!playerId.equals(held.hostOwner)) {
                continue;
            }
            AbstractArrow arrow = resolve(held);
            if (arrow == null) {
                HELD.remove(held.arrowId);
                DeflectionService.retireProjectile(held.arrowId);
                continue;
            }
            releaseHeld(arrow, held);
        }
    }

    /** 放出这个世界里的捕获箭。 */
    public static void releaseLevel(ServerLevel level) {
        if (level == null) {
            return;
        }
        for (Held held : List.copyOf(HELD.values())) {
            if (held.level != level) {
                continue;
            }
            AbstractArrow arrow = resolve(held);
            if (arrow == null) {
                HELD.remove(held.arrowId);
                DeflectionService.retireProjectile(held.arrowId);
                continue;
            }
            releaseHeld(arrow, held);
        }
    }

    /** 放出全部捕获箭。 */
    public static void releaseAll() {
        for (Held held : List.copyOf(HELD.values())) {
            AbstractArrow arrow = resolve(held);
            if (arrow == null) {
                HELD.remove(held.arrowId);
                DeflectionService.retireProjectile(held.arrowId);
                continue;
            }
            releaseHeld(arrow, held);
        }
    }

    /** 箭离开世界：先结束捕获，再清这支箭的防重和离体闸门。 */
    public static void endProjectile(AbstractArrow arrow) {
        if (arrow == null) {
            return;
        }
        releaseOne(arrow);
        if (DeflectionService.retireProjectile(arrow.getUUID())) {
            ArrowBook.sealEnded(arrow.getPersistentData());
        }
    }

    private static void releaseHeld(AbstractArrow arrow, Held held) {
        long gameTick = held.level.getGameTime();
        try {
            if (readActive(held.route)) {
                ReleaseSample exit = held.route.emergencyRelease(gameTick);
                place(arrow, held, exit.position(), exit.unitDirection(), gameTick);
                return;
            }
        } catch (RuntimeException failure) {
            // 宿主给不出出口时，下面改走保存的入射方向。
        }
        releaseAlong(arrow, held, held.incoming, arrow.position());
    }

    private static void releaseAlong(AbstractArrow arrow, Held held, Vec3 heading, Vec3 desired) {
        place(arrow, held, desired, heading, held.level.getGameTime());
    }

    private static void place(AbstractArrow arrow, Held held, Vec3 desired, Vec3 heading, long gameTick) {
        Vec3 unit = unitHeading(heading == null ? held.incoming : heading);
        Vec3 place = finite(desired) ? desired : arrow.position();
        if (blocked(arrow, arrow.position(), place)) {
            place = arrow.position();
        }
        DeflectionService.armSameTickGate(arrow.getUUID(), place, gameTick);
        move(arrow, place, unit);
        arrow.setDeltaMovement(unit.scale(held.speed));
        arrow.setNoGravity(held.noGravity);
        arrow.setNoPhysics(held.noPhysics);
        arrow.hasImpulse = true;
        ArrowBook.writeExit(arrow.getPersistentData(), unit);
        CompoundTag tag = arrow.getPersistentData();
        ArrowBook.markSafeRelease(tag);
        if (held.hostKey != null) {
            ArrowBook.latchExit(tag, held.hostKey);
        }
        ArrowBook.leaveContact(tag, arrow.getUUID());
        ProjectileBarrierTracker.releaseSampling(arrow);
        ((ArrowCaptureAccess) arrow).xuexiSetCaptured(false);
        HELD.remove(arrow.getUUID());
    }

    private static void claimOwner(AbstractArrow arrow, DeflectionHost host, ServerLevel level) {
        UUID ownerId = host.ownerId();
        if (ownerId == null || level.getServer() == null) {
            return;
        }
        CompoundTag tag = arrow.getPersistentData();
        ArrowBook.appendExcluded(tag, ownerId);
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (Relations.permits(ownerId, player) || Relations.ally(ownerId, player)) {
                ArrowBook.appendExcluded(tag, player.getUUID());
            }
        }
        ServerPlayer caster = level.getServer().getPlayerList().getPlayer(ownerId);
        if (caster == null) {
            return;
        }
        int ordinal = arrow.pickup.ordinal();
        tag.putInt(DeflectionMarks.SAVED_PICKUP, ordinal);
        arrow.setOwner(caster);
        CombatOwnership.transfer(arrow, caster);
        arrow.pickup = AbstractArrow.Pickup.byOrdinal(tag.getInt(DeflectionMarks.SAVED_PICKUP));
    }

    private static CaptureRoute openRoute(DeflectionHost host, HostContact contact, Vec3 incoming, long gameTick) {
        try {
            return host.route(contact, incoming, gameTick);
        } catch (RuntimeException failure) {
            return null;
        }
    }

    private static boolean stillUsable(Held held) {
        if (!readActive(held.route) || held.level.getServer() == null || held.hostOwner == null) {
            return false;
        }
        ServerPlayer caster = held.level.getServer().getPlayerList().getPlayer(held.hostOwner);
        return caster != null && caster.level() == held.level;
    }

    /** 只暂停这支箭所属水环主人的架剑窗口。其他宿主和其他玩家的箭照常走。 */
    private static boolean ringGuardPaused(Held held) {
        if (held.hostKey == null) {
            return false;
        }
        DeflectionHost host = DeflectionHosts.findRegistered(held.hostKey);
        if (!(host instanceof RingDeflectionHost) || held.hostOwner == null || held.level == null) {
            return false;
        }
        return ResponseWindowService.open(held.hostOwner, SwordGuardEffect.TECHNIQUE_ID, held.level.getGameTime());
    }

    private static boolean readActive(CaptureRoute route) {
        try {
            return route != null && route.active();
        } catch (RuntimeException failure) {
            return false;
        }
    }

    private static AbstractArrow resolve(Held held) {
        if (held.level.getEntity(held.arrowId) instanceof AbstractArrow arrow) {
            return arrow;
        }
        return null;
    }

    private static boolean blocked(AbstractArrow arrow, Vec3 from, Vec3 toward) {
        if (!finite(from) || !finite(toward) || from.distanceToSqr(toward) < 1.0E-8) {
            return false;
        }
        BlockHitResult hit = arrow.level().clip(new ClipContext(from, toward, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, arrow));
        return hit.getType() != HitResult.Type.MISS;
    }

    private static void move(AbstractArrow arrow, Vec3 place, Vec3 heading) {
        Vec3 unit = unitHeading(heading);
        double flat = Math.sqrt(unit.x * unit.x + unit.z * unit.z);
        float yaw = (float) (Math.atan2(unit.x, unit.z) * (180.0D / Math.PI));
        float pitch = (float) (Math.atan2(unit.y, flat) * (180.0D / Math.PI));
        arrow.moveTo(place.x, place.y, place.z, yaw, pitch);
        arrow.yRotO = yaw;
        arrow.xRotO = pitch;
    }

    public static Vec3 unitHeading(Vec3 motion) {
        if (!finite(motion)) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        double span = motion.length();
        if (span < 1.0E-8) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return motion.scale(1.0D / span);
    }

    private static boolean finite(Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }

    private static final class Held {
        private final UUID arrowId;
        private final ServerLevel level;
        private final CaptureRoute route;
        private final double speed;
        private final boolean noGravity;
        private final boolean noPhysics;
        private final Vec3 incoming;
        private final UUID hostOwner;
        private final HostKey hostKey;
        private long lastTick = Long.MIN_VALUE;

        private Held(UUID arrowId, ServerLevel level, CaptureRoute route, double speed, boolean noGravity,
                     boolean noPhysics, Vec3 incoming, UUID hostOwner, HostKey hostKey) {
            this.arrowId = arrowId;
            this.level = level;
            this.route = route;
            this.speed = speed;
            this.noGravity = noGravity;
            this.noPhysics = noPhysics;
            this.incoming = incoming;
            this.hostOwner = hostOwner;
            this.hostKey = hostKey;
        }
    }
}
