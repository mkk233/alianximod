package com.bslx.XueXiMod.shu.technique.release.clone.action;

import com.bslx.XueXiMod.shu.balance.BalanceSide;
import com.bslx.XueXiMod.shu.balance.ConfiguredDamage;
import com.bslx.XueXiMod.shu.technique.query.LivingCandidateQueries;
import com.bslx.XueXiMod.shu.technique.release.TechniqueHits;
import com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict;
import com.bslx.XueXiMod.shu.technique.release.clone.CloneBalance;
import com.bslx.XueXiMod.shu.technique.release.clone.capture.ActorPayloadRegistry;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneActionContext;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneActorFrame;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneEntity;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneLifeState;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneResults;
import com.bslx.XueXiMod.shu.technique.release.firecast.DamagePermission;
import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 从分身眼睛发出的瞬时单目标脉冲。
 * 主动沿冻结方向取最近射线交点；反击只核对冻结的攻击者。
 * 准备不扣费、不改次数。伤害金额只在准备时形成一次。
 */
public final class ClonePulseAction implements CloneAction {
    private static final ClonePulseAction HOLDER = new ClonePulseAction();
    private static final double SURFACE_SLOP = 1.0E-4d;
    private static final double TIE_SLOP = 1.0E-6d;

    private ClonePulseAction() {
    }

    public static void register() {
        CloneActionRegistry.register(HOLDER, CloneActionCapabilities.full());
        ActorPayloadRegistry.registerBuiltins();
    }

    @Override
    public String actionKey() {
        return CloneBalance.PULSE_ID;
    }

    @Override
    public ClonePreparedAction prepare(CloneActionContext actionContext) {
        VerifiedActor verified = verifyActor(actionContext);
        if (verified.rejection != null) {
            return verified.rejection;
        }
        CloneBalance.ClonePulseQuote quote = CloneBalance.readPulse(BalanceSide.SERVER);
        if (!Double.isFinite(quote.rangeBlocks()) || quote.rangeBlocks() <= 0.0d) {
            return reject(CloneResults.RejectReason.INVALID_QUOTE, "脉冲射程不是有限正数");
        }
        CloneActorFrame actorFrame = actionContext.actorFrame();
        if (!finiteVector(actorFrame.eyeOrigin())) {
            return reject(CloneResults.RejectReason.INVALID_GEOMETRY, "分身的瞄准原点无效");
        }
        if (actorFrame.aimSource() == CloneActorFrame.AimSource.MANUAL_DIRECTION) {
            if (!unitVector(actorFrame.lookUnit())) {
                return reject(CloneResults.RejectReason.INVALID_GEOMETRY, "分身的瞄准方向无效");
            }
            return prepareManual(verified, actorFrame, quote, actionContext.cancelToken());
        }
        if (actorFrame.aimSource() == CloneActorFrame.AimSource.HURT_TARGET) {
            return prepareHurt(verified, actorFrame, quote, actionContext.cancelToken());
        }
        return reject(CloneResults.RejectReason.INVALID_GEOMETRY, "分身的瞄准来源无效");
    }

    private static ClonePreparedAction prepareManual(VerifiedActor verified, CloneActorFrame actorFrame,
                                                     CloneBalance.ClonePulseQuote quote,
                                                     CloneActionContext.CancelToken cancelToken) {
        Vec3 eyeOrigin = actorFrame.eyeOrigin();
        Vec3 lookUnit = actorFrame.lookUnit();
        double rangeBlocks = quote.rangeBlocks();
        Vec3 rayEnd = eyeOrigin.add(lookUnit.scale(rangeBlocks));
        double blockLimit = terrainLimit(verified.level, eyeOrigin, rayEnd, rangeBlocks, verified.actor);
        AABB searchBox = new AABB(eyeOrigin, eyeOrigin).inflate(rangeBlocks);
        List<LivingEntity> candidates = LivingCandidateQueries.targetQuery(
                verified.level, searchBox, ClonePulseAction::coarseLiving);
        if (cancelToken.cancelled()) {
            return reject(CloneResults.RejectReason.BUSY, "这次响应已经取消");
        }
        RayPick picked = nearestAlongRay(candidates, verified.actor, eyeOrigin, rayEnd, rangeBlocks, blockLimit);
        if (picked.target == null) {
            if (picked.occluded) {
                return reject(CloneResults.RejectReason.BLOCKED, "目标被方块挡住");
            }
            return reject(CloneResults.RejectReason.NO_TARGET, "射程内没有可攻击的目标");
        }
        LivingEntity liveTarget = liveCopy(verified.level, picked.target.getUUID());
        if (liveTarget == null || liveTarget.level() != verified.level) {
            return reject(picked.occluded
                    ? CloneResults.RejectReason.BLOCKED
                    : CloneResults.RejectReason.NO_TARGET,
                    picked.occluded ? "目标被方块挡住" : "射程内没有可攻击的目标");
        }
        if (CloneEntity.rejectsPulseTarget(verified.owner, liveTarget)) {
            return reject(CloneResults.RejectReason.FORBIDDEN_TARGET, "这个目标不能由分身攻击");
        }
        return PulsePrepared.ready(quote, liveTarget.getUUID(), verified.owner, verified.level);
    }

    private static ClonePreparedAction prepareHurt(VerifiedActor verified, CloneActorFrame actorFrame,
                                                   CloneBalance.ClonePulseQuote quote,
                                                   CloneActionContext.CancelToken cancelToken) {
        UUID attackerId = actorFrame.targetId();
        if (attackerId == null) {
            return reject(CloneResults.RejectReason.NO_TARGET, "没有可反击的攻击者");
        }
        Vec3 eyeOrigin = actorFrame.eyeOrigin();
        double rangeBlocks = quote.rangeBlocks();
        AABB searchBox = new AABB(eyeOrigin, eyeOrigin).inflate(rangeBlocks);
        List<LivingEntity> candidates = LivingCandidateQueries.targetQuery(
                verified.level, searchBox, ClonePulseAction::coarseLiving);
        if (cancelToken.cancelled()) {
            return reject(CloneResults.RejectReason.BUSY, "这次响应已经取消");
        }
        boolean listed = false;
        for (LivingEntity candidate : candidates) {
            if (candidate != null && attackerId.equals(candidate.getUUID())) {
                listed = true;
                break;
            }
        }
        if (!listed) {
            return reject(CloneResults.RejectReason.NO_TARGET, "攻击者已经不在射程内");
        }
        LivingEntity liveTarget = liveCopy(verified.level, attackerId);
        if (liveTarget == null || liveTarget.level() != verified.level
                || liveTarget == verified.actor || attackerId.equals(verified.actor.getUUID())) {
            return reject(CloneResults.RejectReason.NO_TARGET, "攻击者已经不在射程内");
        }
        double reach = closestDistance(eyeOrigin, liveTarget.getBoundingBox());
        if (!Double.isFinite(reach) || reach > rangeBlocks + SURFACE_SLOP) {
            return reject(CloneResults.RejectReason.NO_TARGET, "攻击者已经不在射程内");
        }
        if (sightBlocked(verified.level, eyeOrigin, liveTarget, verified.actor)) {
            return reject(CloneResults.RejectReason.BLOCKED, "攻击者被方块挡住");
        }
        if (CloneEntity.rejectsPulseTarget(verified.owner, liveTarget)) {
            return reject(CloneResults.RejectReason.FORBIDDEN_TARGET, "这个目标不能由分身攻击");
        }
        return PulsePrepared.ready(quote, liveTarget.getUUID(), verified.owner, verified.level);
    }

    private static VerifiedActor verifyActor(CloneActionContext actionContext) {
        if (actionContext == null || actionContext.owner() == null || actionContext.actorFrame() == null
                || actionContext.cancelToken() == null) {
            return VerifiedActor.rejected(CloneResults.RejectReason.OWNER_UNAVAILABLE, "主人或分身帧不可用");
        }
        if (actionContext.cancelToken().cancelled()) {
            return VerifiedActor.rejected(CloneResults.RejectReason.BUSY, "这次响应已经取消");
        }
        ServerPlayer owner = actionContext.owner();
        MinecraftServer server = owner.getServer();
        if (server == null || !server.isSameThread()) {
            return VerifiedActor.rejected(CloneResults.RejectReason.WRONG_SERVER, "响应只能在服务器线程执行");
        }
        CloneActorFrame actorFrame = actionContext.actorFrame();
        ServerLevel level = actorFrame.level();
        if (!owner.isAlive() || owner.isRemoved() || owner.isSpectator()) {
            return VerifiedActor.rejected(CloneResults.RejectReason.OWNER_UNAVAILABLE, "主人现在不能承担这次响应");
        }
        if (level == null || owner.level() != level) {
            return VerifiedActor.rejected(CloneResults.RejectReason.WRONG_DIMENSION, "分身和主人不在同一个世界");
        }
        if (!owner.getUUID().equals(actorFrame.ownerId())) {
            return VerifiedActor.rejected(CloneResults.RejectReason.WRONG_OWNER, "这具分身不属于你");
        }
        // 帧上 0 表示未生成或已作废。冻结的实体签名没有代次读取方法，这里不能再和实体比对。
        if (actorFrame.actorGeneration() < 1L || actorFrame.actorId() == null) {
            return VerifiedActor.rejected(CloneResults.RejectReason.ACTOR_GONE, "分身代次已经作废");
        }
        Entity found = level.getEntity(actorFrame.actorId());
        if (!(found instanceof CloneEntity actor) || !actor.isAlive() || actor.isRemoved()
                || actor.level() != level || !actorFrame.actorId().equals(actor.getUUID())) {
            return VerifiedActor.rejected(CloneResults.RejectReason.ACTOR_GONE, "分身已经不在");
        }
        if (actor.lifeState() != CloneLifeState.ACTIVE) {
            return VerifiedActor.rejected(CloneResults.RejectReason.ACTOR_GONE, "分身已经结束");
        }
        if (!owner.getUUID().equals(actor.ownerId())) {
            return VerifiedActor.rejected(CloneResults.RejectReason.WRONG_OWNER, "这具分身不属于你");
        }
        return VerifiedActor.ready(owner, actor, level);
    }

    private static boolean coarseLiving(LivingEntity candidate) {
        return candidate != null && candidate.isAlive() && !candidate.isRemoved() && !candidate.isSpectator();
    }

    private static RayPick nearestAlongRay(List<LivingEntity> candidates, CloneEntity actor, Vec3 eyeOrigin,
                                           Vec3 rayEnd, double rangeBlocks, double blockLimit) {
        LivingEntity chosenTarget = null;
        UUID chosenId = null;
        double chosenDistance = Double.POSITIVE_INFINITY;
        boolean occluded = false;
        if (candidates == null) {
            return new RayPick(null, false);
        }
        for (LivingEntity candidate : candidates) {
            if (!coarseLiving(candidate) || candidate == actor || actor.getUUID().equals(candidate.getUUID())) {
                continue;
            }
            Optional<Vec3> intersection = candidate.getBoundingBox().clip(eyeOrigin, rayEnd);
            Vec3 hitPoint;
            if (intersection.isPresent()) {
                hitPoint = intersection.get();
            } else if (candidate.getBoundingBox().contains(eyeOrigin)) {
                hitPoint = eyeOrigin;
            } else {
                continue;
            }
            double hitDistance = eyeOrigin.distanceTo(hitPoint);
            if (!Double.isFinite(hitDistance) || hitDistance > rangeBlocks + SURFACE_SLOP) {
                continue;
            }
            if (hitDistance > blockLimit + SURFACE_SLOP) {
                occluded = true;
                continue;
            }
            UUID candidateId = candidate.getUUID();
            boolean nearer = chosenTarget == null || hitDistance < chosenDistance - TIE_SLOP;
            boolean sameDistance = chosenTarget != null && Math.abs(hitDistance - chosenDistance) <= TIE_SLOP;
            if (nearer || (sameDistance && candidateId.toString().compareTo(chosenId.toString()) < 0)) {
                chosenTarget = candidate;
                chosenId = candidateId;
                chosenDistance = hitDistance;
            }
        }
        return new RayPick(chosenTarget, occluded);
    }

    private static double terrainLimit(ServerLevel level, Vec3 eyeOrigin, Vec3 rayEnd, double rangeBlocks,
                                     Entity actor) {
        BlockHitResult blockHit = level.clip(new ClipContext(
                eyeOrigin, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, actor));
        if (blockHit.getType() != HitResult.Type.BLOCK) {
            return rangeBlocks;
        }
        double blockDistance = eyeOrigin.distanceTo(blockHit.getLocation());
        if (!Double.isFinite(blockDistance) || blockDistance >= rangeBlocks) {
            return rangeBlocks;
        }
        return blockDistance;
    }

    private static boolean sightBlocked(ServerLevel level, Vec3 eyeOrigin, LivingEntity target, Entity actor) {
        Vec3 aimPoint = closestPoint(eyeOrigin, target.getBoundingBox());
        double aimDistance = eyeOrigin.distanceTo(aimPoint);
        if (!Double.isFinite(aimDistance) || aimDistance <= SURFACE_SLOP) {
            return false;
        }
        BlockHitResult blockHit = level.clip(new ClipContext(
                eyeOrigin, aimPoint, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, actor));
        if (blockHit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        double blockDistance = eyeOrigin.distanceTo(blockHit.getLocation());
        return Double.isFinite(blockDistance) && blockDistance + SURFACE_SLOP < aimDistance;
    }

    @Nullable
    private static LivingEntity liveCopy(ServerLevel level, UUID targetId) {
        Entity found = targetId == null ? null : level.getEntity(targetId);
        if (!(found instanceof LivingEntity liveTarget) || !coarseLiving(liveTarget)) {
            return null;
        }
        return liveTarget;
    }

    private static double closestDistance(Vec3 eyeOrigin, AABB bounds) {
        Vec3 aimPoint = closestPoint(eyeOrigin, bounds);
        return eyeOrigin.distanceTo(aimPoint);
    }

    private static Vec3 closestPoint(Vec3 eyeOrigin, AABB bounds) {
        double clampedX = Math.min(Math.max(eyeOrigin.x, bounds.minX), bounds.maxX);
        double clampedY = Math.min(Math.max(eyeOrigin.y, bounds.minY), bounds.maxY);
        double clampedZ = Math.min(Math.max(eyeOrigin.z, bounds.minZ), bounds.maxZ);
        return new Vec3(clampedX, clampedY, clampedZ);
    }

    private static boolean finiteVector(Vec3 vector) {
        return vector != null && Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    private static boolean unitVector(Vec3 vector) {
        if (!finiteVector(vector)) {
            return false;
        }
        double lengthSquared = vector.lengthSqr();
        return Math.abs(lengthSquared - 1.0d) <= 1.0E-4d;
    }

    private static ClonePreparedAction reject(CloneResults.RejectReason reason, String detail) {
        return PulsePrepared.rejected(reason, detail);
    }

    private static final class VerifiedActor {
        private final ServerPlayer owner;
        private final CloneEntity actor;
        private final ServerLevel level;
        private final ClonePreparedAction rejection;

        private VerifiedActor(ServerPlayer owner, CloneEntity actor, ServerLevel level,
                              ClonePreparedAction rejection) {
            this.owner = owner;
            this.actor = actor;
            this.level = level;
            this.rejection = rejection;
        }

        private static VerifiedActor ready(ServerPlayer owner, CloneEntity actor, ServerLevel level) {
            return new VerifiedActor(owner, actor, level, null);
        }

        private static VerifiedActor rejected(CloneResults.RejectReason reason, String detail) {
            return new VerifiedActor(null, null, null, PulsePrepared.rejected(reason, detail));
        }
    }

    private static final class RayPick {
        private final LivingEntity target;
        private final boolean occluded;

        private RayPick(LivingEntity target, boolean occluded) {
            this.target = target;
            this.occluded = occluded;
        }
    }

    private static final class PulsePrepared implements ClonePreparedAction {
        private boolean accepted;
        private CloneResults.RejectReason reason;
        private String detail;
        private ConfiguredDamage configuredDamage;
        private int quotedMana;
        private int quotedCooldownTicks;
        private UUID targetId;
        private ServerPlayer owner;
        private ServerLevel level;
        private boolean committed;
        private boolean withdrawn;

        private PulsePrepared(boolean accepted, CloneResults.RejectReason reason, String detail,
                              ConfiguredDamage configuredDamage, int quotedMana, int quotedCooldownTicks,
                              UUID targetId, ServerPlayer owner, ServerLevel level) {
            this.accepted = accepted;
            this.reason = reason;
            this.detail = detail;
            this.configuredDamage = configuredDamage;
            this.quotedMana = quotedMana;
            this.quotedCooldownTicks = quotedCooldownTicks;
            this.targetId = targetId;
            this.owner = owner;
            this.level = level;
        }

        private static PulsePrepared rejected(CloneResults.RejectReason reason, String detail) {
            return new PulsePrepared(false, reason, detail == null ? "" : detail,
                    null, 0, 0, null, null, null);
        }

        private static PulsePrepared ready(CloneBalance.ClonePulseQuote quote, UUID targetId, ServerPlayer owner,
                                           ServerLevel level) {
            return new PulsePrepared(true, null, "已选定脉冲目标",
                    quote.configuredDamage(), quote.manaCost(), quote.cooldownTicks(),
                    targetId, owner, level);
        }

        @Override
        public boolean accepted() {
            return this.accepted && !this.withdrawn;
        }

        @Override
        public CloneResults.RejectReason reason() {
            return this.reason;
        }

        @Override
        public String detail() {
            return this.detail == null ? "" : this.detail;
        }

        @Override
        public ConfiguredDamage configuredDamage() {
            return this.configuredDamage;
        }

        @Override
        public int quotedMana() {
            return this.quotedMana;
        }

        @Override
        public int quotedCooldownTicks() {
            return this.quotedCooldownTicks;
        }

        @Override
        public UUID targetId() {
            return this.targetId;
        }

        @Override
        public CloneActionResult commit() {
            if (!this.accepted || this.withdrawn) {
                throw new IllegalStateException("分身脉冲尚未接受，不能提交");
            }
            if (this.committed) {
                throw new IllegalStateException("分身脉冲不能重复提交");
            }
            this.committed = true;
            MinecraftServer server = this.owner == null ? null : this.owner.getServer();
            if (server == null || !server.isSameThread()) {
                throw new IllegalStateException("分身脉冲提交离开了服务器线程");
            }
            if (this.configuredDamage == null || this.level == null || this.owner == null) {
                throw new IllegalStateException("分身脉冲报价已经丢失");
            }
            if (!this.owner.isAlive() || this.owner.isRemoved() || this.owner.level() != this.level) {
                return new CloneActionResult(null, false);
            }
            Entity found = this.targetId == null ? null : this.level.getEntity(this.targetId);
            if (!(found instanceof LivingEntity target) || !target.isAlive() || target.isRemoved()
                    || target.level() != this.level) {
                return new CloneActionResult(null, false);
            }
            boolean permitted = DamagePermission.allows(this.owner, target);
            boolean rejected = CloneEntity.rejectsPulseTarget(this.owner, target);
            if (!permitted || rejected) {
                return new CloneActionResult(null, false);
            }
            HitVerdict verdict = TechniqueHits.classifyRespecting(
                    target, this.owner, this.configuredDamage.amount(), TideMarkSuccess.NONE);
            return new CloneActionResult(verdict, true);
        }

        @Override
        public void cancelBeforeCommit() {
            if (this.committed) {
                return;
            }
            this.withdrawn = true;
            this.accepted = false;
            this.configuredDamage = null;
            this.targetId = null;
            this.quotedMana = 0;
            this.quotedCooldownTicks = 0;
            this.owner = null;
            this.level = null;
        }
    }
}
