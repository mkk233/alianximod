package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.shu.technique.query.LivingCandidateQueries;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.bind.BoundReleases;
import com.bslx.XueXiMod.shu.technique.release.bind.LifetimeToken;
import com.bslx.XueXiMod.shu.technique.release.bind.ReleaseExecutionContext;
import com.bslx.XueXiMod.shu.technique.release.bind.ReleaseVolume;
import com.bslx.XueXiMod.shu.technique.release.bind.RoundHandle;
import com.bslx.XueXiMod.shu.technique.release.bind.TargetCheck;
import com.bslx.XueXiMod.shu.technique.release.relations.Relations;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 绑定领域的周期调度。首发在开域后 20 刻，之后每 20 刻一次。
 * 开域费仍由领域服务收取。每一轮的子计划费用由已安装的自动执行器收取。
 */
public final class DomainBindingRuntime {
    public static final int PERIOD_TICKS = 20;

    private DomainBindingRuntime() {
    }

    public static void tick(MinecraftServer server, int now, List<DomainInstance> activeSnapshot) {
        if (server == null || activeSnapshot == null || activeSnapshot.isEmpty()) {
            return;
        }
        ArrayList<DomainInstance> ordered = new ArrayList<>();
        for (DomainInstance domain : activeSnapshot) {
            if (domain == null || domain.boundPlan() == null || domain.ownerId() == null || domain.instanceId() == null) {
                continue;
            }
            ordered.add(domain);
        }
        if (ordered.isEmpty()) {
            return;
        }
        ordered.sort(DomainBindingRuntime::byOwnerThenInstance);
        for (DomainInstance domain : ordered) {
            domain.sweepFinishedRounds();
            if (!domain.active()) {
                domain.cancelOpenRounds();
            }
        }
        for (DomainInstance domain : ordered) {
            fireDue(server, now, domain);
        }
    }

    private static int byOwnerThenInstance(DomainInstance left, DomainInstance right) {
        int ownerOrder = left.ownerId().compareTo(right.ownerId());
        if (ownerOrder != 0) {
            return ownerOrder;
        }
        return left.instanceId().compareTo(right.instanceId());
    }

    private static void fireDue(MinecraftServer server, int now, DomainInstance domain) {
        if (domain == null || domain.boundPlan() == null || !domain.active()) {
            return;
        }
        if (ReleaseInput.hasExpired(now, domain.expiresAt())) {
            return;
        }
        int firstBeat = domain.startedAt() + PERIOD_TICKS;
        int dueBeat = domain.nextBindingBeat();
        if (!reached(now, firstBeat) || !reached(now, dueBeat)) {
            return;
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(domain.ownerId());
        if (owner == null || !owner.isAlive() || owner.level() != domain.hostLevel()) {
            return;
        }
        int elapsed = now - firstBeat;
        int steps = Math.floorDiv(elapsed, PERIOD_TICKS);
        long following = (long) firstBeat + ((long) steps + 1L) * (long) PERIOD_TICKS;
        int followingBeat = following > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) following;
        domain.advanceBindingBeat(followingBeat);
        RoundHandle handle = releaseOne(domain, owner, dueBeat);
        domain.trackRound(handle);
    }

    /** 当前刻已经到达节拍。用 int 差值，和领域寿命的到期判断同一套环绕比较。 */
    private static boolean reached(int now, int beat) {
        return now - beat >= 0;
    }

    @Nullable
    private static RoundHandle releaseOne(DomainInstance domain, ServerPlayer owner, int dueBeat) {
        DomainGeometry geometry = domain.geometry();
        ServerLevel hostLevel = domain.hostLevel();
        UUID ownerId = domain.ownerId();
        UUID instanceId = domain.instanceId();
        if (geometry == null || hostLevel == null || ownerId == null || instanceId == null || owner == null) {
            return null;
        }
        long roundSeed = mixSeed(instanceId, dueBeat);
        RandomSource random = RandomSource.create(roundSeed);
        Vec3 sampleOrigin = geometry.samplePoint(random);
        if (!finitePoint(sampleOrigin)) {
            return null;
        }
        LivingEntity nearest = nearestTarget(hostLevel, geometry, ownerId, sampleOrigin);
        UUID localTargetId = null;
        Vec3 baseDirection = null;
        if (nearest != null) {
            localTargetId = nearest.getUUID();
            baseDirection = directionToward(sampleOrigin, nearest.getBoundingBox().getCenter());
        }
        if (!finitePoint(baseDirection)) {
            baseDirection = uniformSphere(random);
        }
        if (!finitePoint(baseDirection) || baseDirection.lengthSqr() < 1.0E-8D) {
            return null;
        }
        ItemStack heldStack = owner.getMainHandItem();
        ItemStack weaponCopy = heldStack == null ? ItemStack.EMPTY.copy() : heldStack.copy();
        float weaponBonus = 0.0f;
        if (heldStack != null && owner.getMainHandItem() == heldStack) {
            AttributeInstance attackDamage = owner.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackDamage != null) {
                double damageReading = attackDamage.getValue();
                if (Double.isFinite(damageReading)) {
                    weaponBonus = (float) Math.max(0.0D, damageReading - 1.0D);
                }
            }
        }
        UUID roundId = UUID.randomUUID();
        ReleaseVolume volume = new GeometryVolume(geometry);
        TargetCheck targetCheck = new FrozenTargetCheck(hostLevel, geometry, ownerId);
        LifetimeToken token = new DomainLifetimeToken(domain, hostLevel, ownerId);
        ReleaseExecutionContext context = ReleaseExecutionContext.automatic(
                hostLevel, owner, instanceId, roundId, sampleOrigin, baseDirection, localTargetId,
                volume, targetCheck, weaponCopy, weaponBonus, roundSeed, token);
        return BoundReleases.executeAutomatic(domain.boundPlan(), context, token);
    }

    private static long mixSeed(UUID instanceId, int dueBeat) {
        long highBits = instanceId.getMostSignificantBits();
        long lowBits = instanceId.getLeastSignificantBits();
        long mixed = highBits ^ Long.rotateLeft(lowBits, 17);
        mixed ^= (long) dueBeat * 0x9E3779B97F4A7C15L;
        return mixed;
    }

    @Nullable
    private static LivingEntity nearestTarget(ServerLevel hostLevel, DomainGeometry geometry, UUID ownerId, Vec3 sampleOrigin) {
        AABB queryBounds = geometry.bounds();
        if (queryBounds == null) {
            return null;
        }
        List<LivingEntity> candidates = LivingCandidateQueries.targetQuery(hostLevel, queryBounds,
                candidate -> acceptsTarget(hostLevel, geometry, ownerId, candidate));
        LivingEntity chosen = null;
        UUID chosenId = null;
        double chosenDistance = Double.POSITIVE_INFINITY;
        for (LivingEntity candidate : candidates) {
            if (!acceptsTarget(hostLevel, geometry, ownerId, candidate)) {
                continue;
            }
            Vec3 center = candidate.getBoundingBox().getCenter();
            double distance = center.distanceToSqr(sampleOrigin);
            if (!Double.isFinite(distance)) {
                continue;
            }
            UUID candidateId = candidate.getUUID();
            if (candidateId == null) {
                continue;
            }
            boolean closer = chosen == null || distance < chosenDistance;
            boolean sameDistance = chosen != null && distance == chosenDistance && candidateId.compareTo(chosenId) < 0;
            if (closer || sameDistance) {
                chosen = candidate;
                chosenId = candidateId;
                chosenDistance = distance;
            }
        }
        return chosen;
    }

    private static boolean acceptsTarget(ServerLevel hostLevel, DomainGeometry geometry, UUID ownerId, LivingEntity candidate) {
        if (hostLevel == null || geometry == null || ownerId == null || candidate == null) {
            return false;
        }
        if (!candidate.isAlive() || candidate.isSpectator() || candidate.level() != hostLevel) {
            return false;
        }
        if (ownerId.equals(candidate.getUUID())) {
            return false;
        }
        if (!geometry.fullyContains(candidate.getBoundingBox())) {
            return false;
        }
        return !Relations.permits(ownerId, candidate) && !Relations.ally(ownerId, candidate);
    }

    @Nullable
    private static Vec3 directionToward(Vec3 origin, Vec3 center) {
        if (!finitePoint(origin) || !finitePoint(center)) {
            return null;
        }
        double across = center.x - origin.x;
        double upward = center.y - origin.y;
        double depth = center.z - origin.z;
        double lengthSquared = across * across + upward * upward + depth * depth;
        if (!Double.isFinite(lengthSquared) || lengthSquared < 1.0E-8D) {
            return null;
        }
        double length = Math.sqrt(lengthSquared);
        return new Vec3(across / length, upward / length, depth / length);
    }

    private static Vec3 uniformSphere(RandomSource random) {
        double vertical = random.nextDouble() * 2.0D - 1.0D;
        double azimuth = random.nextDouble() * (Math.PI * 2.0D);
        double ring = Math.sqrt(Math.max(0.0D, 1.0D - vertical * vertical));
        double across = ring * Math.cos(azimuth);
        double depth = ring * Math.sin(azimuth);
        if (!Double.isFinite(across) || !Double.isFinite(vertical) || !Double.isFinite(depth)) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        double lengthSquared = across * across + vertical * vertical + depth * depth;
        if (!Double.isFinite(lengthSquared) || lengthSquared < 1.0E-8D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        double length = Math.sqrt(lengthSquared);
        return new Vec3(across / length, vertical / length, depth / length);
    }

    private static boolean finitePoint(Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }

    /** 把领域几何的内缩钳制交给绑定体积，不把几何类型放进绑定包。 */
    private static final class GeometryVolume implements ReleaseVolume {
        private final DomainGeometry geometry;

        private GeometryVolume(DomainGeometry geometry) {
            this.geometry = geometry;
        }

        @Override
        public Vec3 clampPoint(Vec3 point) {
            if (geometry == null) {
                return point;
            }
            return geometry.clampPoint(point);
        }
    }

    /**
     * 冻结这一轮的几何、宿主世界和主人关系。
     * 不读取领域是否仍开启，关域后已生成攻击仍可按当时的局部目标复核。
     */
    private static final class FrozenTargetCheck implements TargetCheck {
        private final ServerLevel hostLevel;
        private final DomainGeometry geometry;
        private final UUID ownerId;

        private FrozenTargetCheck(ServerLevel hostLevel, DomainGeometry geometry, UUID ownerId) {
            this.hostLevel = hostLevel;
            this.geometry = geometry;
            this.ownerId = ownerId;
        }

        @Override
        @Nullable
        public LivingEntity stillValid(@Nullable UUID targetId) {
            if (targetId == null || hostLevel == null) {
                return null;
            }
            Entity resolved = hostLevel.getEntity(targetId);
            if (!(resolved instanceof LivingEntity candidate)) {
                return null;
            }
            if (!acceptsTarget(hostLevel, geometry, ownerId, candidate)) {
                return null;
            }
            return candidate;
        }
    }

    /** 领域仍开启、未到到期刻，且主人存活、在线、留在宿主维度时，这一轮才有效。 */
    private static final class DomainLifetimeToken implements LifetimeToken {
        private final DomainInstance domain;
        private final ServerLevel hostLevel;
        private final UUID ownerId;

        private DomainLifetimeToken(DomainInstance domain, ServerLevel hostLevel, UUID ownerId) {
            this.domain = domain;
            this.hostLevel = hostLevel;
            this.ownerId = ownerId;
        }

        @Override
        public boolean valid(int now) {
            if (domain == null || hostLevel == null || ownerId == null || !domain.active()) {
                return false;
            }
            if (ReleaseInput.hasExpired(now, domain.expiresAt())) {
                return false;
            }
            MinecraftServer server = hostLevel.getServer();
            if (server == null) {
                return false;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
            return owner != null && owner.isAlive() && owner.level() == hostLevel;
        }
    }
}
