package com.bslx.XueXiMod.shu.technique.release.clone.core;

import com.bslx.XueXiMod.shu.technique.release.clone.CloneBalance;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeStructureLimits;
import com.bslx.XueXiMod.shu.technique.release.firecast.DamagePermission;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

public class CloneEntity extends PathfinderMob {
    static final int SAVE_VERSION = 1;
    private static final float EYE_HEIGHT = 1.74F;
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final EntityDataAccessor<Integer> DATA_KIND =
            SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_LIFE =
            SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_RESPONSE =
            SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_SEQUENCE =
            SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_GENERATION =
            SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_SESSION =
            SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DATA_PRESENTATION =
            SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PRESENTATION_REVISION =
            SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_DEPLOYMENT =
            SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private long actorGeneration;
    private boolean fromSave;
    private boolean admitted;
    private boolean capturedCosmetic;
    private boolean hurtNesting;
    @Nullable
    private SpawnTicket ticket;

    public CloneEntity(EntityType<? extends CloneEntity> entityType, Level level) {
        super(entityType, level);
        this.setCanPickUpLoot(false);
        this.xpReward = 0;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, CloneBalance.BODY_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D);
    }

    static SpawnTicket ticket(long sessionToken, UUID ownerId, long actorGeneration, CloneSpec spec) {
        return new SpawnTicket(sessionToken, ownerId, actorGeneration, spec);
    }

    void arm(SpawnTicket spawnTicket, long sessionToken) {
        this.ticket = spawnTicket;
        this.actorGeneration = spawnTicket.actorGeneration;
        this.syncLong(DATA_GENERATION, spawnTicket.actorGeneration);
        this.syncLong(DATA_SESSION, sessionToken);
        this.syncInt(DATA_KIND, spawnTicket.spec.kind().ordinal());
        this.syncOwner(spawnTicket.ownerId);
        this.syncInt(DATA_LIFE, CloneLifeState.ACTIVE.ordinal());
        this.syncInt(DATA_RESPONSE, CloneResponseState.READY.ordinal());
        this.syncInt(DATA_PRESENTATION, spawnTicket.spec.presentationKey().ordinal());
        this.syncInt(DATA_PRESENTATION_REVISION, spawnTicket.spec.presentationRevision());
        this.syncDeployment(spawnTicket.spec.deploymentId());
        this.applyBodyHealth(spawnTicket.spec.bodyHealth());
    }

    void markCaptured(boolean cosmetic) {
        this.capturedCosmetic = cosmetic;
    }

    boolean capturedCosmetic() {
        return this.capturedCosmetic;
    }

    public PresentationKey presentationKey() {
        return PresentationKey.fromOrdinal(this.entityData.get(DATA_PRESENTATION));
    }

    public int presentationRevision() {
        return this.entityData.get(DATA_PRESENTATION_REVISION);
    }

    public long syncedSessionToken() {
        return this.entityData.get(DATA_SESSION);
    }

    /**
     * 捕获部署的表现标识。旧分身没有单独的部署标识，通知用的就是实体 UUID。
     */
    public UUID presentationDeploymentId() {
        UUID synced = this.entityData.get(DATA_DEPLOYMENT).orElse(null);
        if (synced != null) {
            return synced;
        }
        return this.getUUID();
    }

    public CloneKind kind() {
        return CloneKind.fromOrdinal(this.entityData.get(DATA_KIND));
    }

    public UUID ownerId() {
        return this.entityData.get(DATA_OWNER).orElse(null);
    }

    public CloneLifeState lifeState() {
        return CloneLifeState.fromOrdinal(this.entityData.get(DATA_LIFE));
    }

    public CloneResponseState responseState() {
        return CloneResponseState.fromOrdinal(this.entityData.get(DATA_RESPONSE));
    }

    public long actionSequence() {
        return this.entityData.get(DATA_SEQUENCE);
    }

    public long generation() {
        return this.actorGeneration != 0L ? this.actorGeneration : this.entityData.get(DATA_GENERATION);
    }

    void mirror(CloneRuntimeState state) {
        long shownGeneration = state.life == CloneLifeState.ACTIVE ? state.actorGeneration : 0L;
        this.actorGeneration = shownGeneration;
        this.syncInt(DATA_LIFE, state.life.ordinal());
        this.syncInt(DATA_RESPONSE, state.response.ordinal());
        this.syncLong(DATA_SEQUENCE, state.actionSequence);
        this.syncLong(DATA_GENERATION, shownGeneration);
    }

    void face(net.minecraft.world.phys.Vec3 lookUnit) {
        double horizontal = Math.sqrt(lookUnit.x * lookUnit.x + lookUnit.z * lookUnit.z);
        float bodyYaw = (float) (Math.atan2(-lookUnit.x, lookUnit.z) * (180.0D / Math.PI));
        float bodyPitch = (float) (Math.atan2(-lookUnit.y, horizontal) * (180.0D / Math.PI));
        this.setYRot(bodyYaw);
        this.setXRot(bodyPitch);
        this.yRotO = bodyYaw;
        this.setYHeadRot(bodyYaw);
        this.setYBodyRot(bodyYaw);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_KIND, CloneKind.PHANTOM.ordinal());
        this.entityData.define(DATA_OWNER, Optional.empty());
        this.entityData.define(DATA_LIFE, CloneLifeState.ENDED.ordinal());
        this.entityData.define(DATA_RESPONSE, CloneResponseState.READY.ordinal());
        this.entityData.define(DATA_SEQUENCE, 0L);
        this.entityData.define(DATA_GENERATION, 0L);
        this.entityData.define(DATA_SESSION, 0L);
        this.entityData.define(DATA_PRESENTATION, PresentationKey.LEGACY_HUMANOID.ordinal());
        this.entityData.define(DATA_PRESENTATION_REVISION, 0);
        this.entityData.define(DATA_DEPLOYMENT, Optional.empty());
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public float getEyeHeight(Pose pose) {
        return EYE_HEIGHT;
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return EYE_HEIGHT;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor levelAccessor, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData group,
                                        @Nullable CompoundTag compound) {
        this.setCanPickUpLoot(false);
        this.setPersistenceRequired();
        this.xpReward = 0;
        if (this.ticket != null) {
            this.applyBodyHealth(this.ticket.spec.bodyHealth());
        }
        return group;
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (this.level().isClientSide()) {
            this.admitted = !this.fromSave;
            if (this.fromSave) {
                this.discard();
            }
            return;
        }
        if (this.fromSave || !this.ticketValid()) {
            this.discard();
            return;
        }
        this.admitted = true;
    }

    @Override
    public void tick() {
        if (this.isRemoved()) {
            return;
        }
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (this.fromSave || !this.admitted) {
            this.discard();
        }
    }

    /**
     * 超过短距上限的位置包直接落到目标点。
     * 原版传送包仍会请求若干刻插值，远距归位不能沿这段距离滑过去。
     */
    @Override
    public void lerpTo(double targetX, double targetY, double targetZ, float targetYaw, float targetPitch,
                       int steps, boolean teleport) {
        double snapDistance = CooperativeStructureLimits.REPOSITION_DISTANCE_CAP;
        if (this.distanceToSqr(targetX, targetY, targetZ) > snapDistance * snapDistance) {
            super.lerpTo(targetX, targetY, targetZ, targetYaw, targetPitch, 0, true);
            this.setPos(targetX, targetY, targetZ);
            this.setYRot(targetYaw);
            this.setYHeadRot(targetYaw);
            this.setYBodyRot(targetYaw);
            this.setXRot(targetPitch);
            this.yRotO = targetYaw;
            this.xRotO = targetPitch;
            this.yBodyRotO = targetYaw;
            this.yHeadRotO = targetYaw;
            this.xo = targetX;
            this.yo = targetY;
            this.zo = targetZ;
            this.xOld = targetX;
            this.yOld = targetY;
            this.zOld = targetZ;
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        super.lerpTo(targetX, targetY, targetZ, targetYaw, targetPitch, steps, teleport);
    }

    /** 服务端一次改脚点。不走 travel，因此没有途中碰撞、接触或摔落伤害。 */
    void snapToFeet(Vec3 feet) {
        this.moveTo(feet.x, feet.y, feet.z, this.getYRot(), this.getXRot());
        this.setDeltaMovement(Vec3.ZERO);
        this.fallDistance = 0.0F;
        this.hasImpulse = true;
        this.setNoGravity(false);
        this.xo = feet.x;
        this.yo = feet.y;
        this.zo = feet.z;
        this.xOld = feet.x;
        this.yOld = feet.y;
        this.zOld = feet.z;
    }

    @Override
    public boolean isPushable() {
        return !this.capturedCosmetic && super.isPushable();
    }

    @Override
    public boolean canBeCollidedWith() {
        return !this.capturedCosmetic && super.canBeCollidedWith();
    }

    @Override
    public void push(Entity other) {
        if (this.capturedCosmetic) {
            return;
        }
        super.push(other);
        if (this.level().isClientSide() || this.isRemoved() || !(other instanceof LivingEntity living)) {
            return;
        }
        CloneContacts.offerEntity(this, living);
    }

    @Override
    public void remove(RemovalReason reason) {
        HurtAttempt attempt = this.openHurtAttempt();
        if (attempt != null && attempt.deferralOpen() && reason == RemovalReason.KILLED) {
            attempt.notePendingKilledRemoval();
            return;
        }
        if (attempt != null && attempt.deferralOpen()) {
            attempt.noteHardCancel();
        }
        super.remove(reason);
    }

    @Override
    protected void tickDeath() {
        if (!this.breaksOnTrigger() || this.hurtNesting) {
            if (!this.breaksOnTrigger()) {
                super.tickDeath();
            }
            return;
        }
        if (!this.isRemoved()) {
            this.discard();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float incoming) {
        if (this.level().isClientSide()) {
            return super.hurt(source, incoming);
        }
        if (!(this.level() instanceof ServerLevel serverLevel) || !serverLevel.getServer().isSameThread()) {
            return false;
        }
        if (this.fromSave || !this.admitted || this.lifeState() != CloneLifeState.ACTIVE) {
            return false;
        }
        if (CloneSourceScope.blocksResponse()) {
            return false;
        }
        if (this.capturedCosmetic) {
            return false;
        }
        ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(this.ownerId());
        Entity attacker = source == null ? null : source.getEntity();
        if (rejectsIncomingAttacker(owner, attacker)) {
            return false;
        }
        CloneRuntime runtime = CloneRuntime.get(serverLevel.getServer());
        CloneRuntimeState state = runtime == null ? null : runtime.find(this.getUUID());
        if (!this.armsHurtGate(state)) {
            return this.hurtOrdinary(source, incoming);
        }
        if (this.hurtNesting) {
            return false;
        }
        this.hurtNesting = true;
        HurtAttempt attempt = HurtAttempt.open(state, this, attacker == null ? null : attacker.getUUID());
        state.hurtAttempt = attempt;
        boolean saved = false;
        try {
            saved = super.hurt(source, incoming);
            attempt.closeDeferral();
            this.finishHurt(serverLevel, state, attempt, saved);
            return saved;
        } catch (RuntimeException failure) {
            LOGGER.error("水幻形受击事务失败 actor={}", this.getUUID(), failure);
            if (attempt.deferralOpen()) {
                attempt.closeDeferral();
            }
            this.finishHurt(serverLevel, state, attempt, false);
            return false;
        } finally {
            state.hurtAttempt = null;
            this.hurtNesting = false;
        }
    }

    private boolean hurtOrdinary(DamageSource source, float incoming) {
        boolean accepted = super.hurt(source, incoming);
        if (!accepted) {
            return false;
        }
        if (!this.isAlive()) {
            CloneService.endLoaded(this, CloneEndReason.DEATH);
            return true;
        }
        CloneService.onAcceptedHurt(this, source);
        return true;
    }

    private void finishHurt(ServerLevel serverLevel, CloneRuntimeState state, HurtAttempt attempt, boolean saved) {
        boolean ownerReady = this.ownerReady(serverLevel, state);
        boolean qualified = saved && !attempt.hardCancelled() && ownerReady && this.sourceStillQualified(serverLevel, attempt);
        boolean leaseReady = state.deploymentLease == attempt.lease && attempt.lease.phase() == DeploymentLease.Phase.BODY
                && !attempt.lease.released();
        if (qualified && leaseReady) {
            Vec3 aimed = this.acceptedLook(serverLevel, attempt);
            boolean opened = aimed != null && CloneService.breakForResponse(attempt.handle, CaptureTriggerKind.ACCEPTED_HURT,
                    attempt.sourceId, aimed);
            if (!opened) {
                this.completeHurtCleanup(attempt);
            }
            return;
        }
        if (!saved && attempt.pendingDeath() && this.isAlive() && !attempt.hardCancelled()) {
            return;
        }
        this.completeHurtCleanup(attempt);
    }

    private void completeHurtCleanup(HurtAttempt attempt) {
        if (this.isRemoved()) {
            return;
        }
        if (attempt.pendingDeath() || !this.isAlive() || attempt.pendingKilledRemoval() || attempt.hardCancelled()) {
            CloneService.endLoaded(this, attempt.pendingDeath() || !this.isAlive()
                    ? CloneEndReason.DEATH : CloneEndReason.UNLOAD);
        }
        if (attempt.pendingKilledRemoval() && !this.isRemoved()) {
            this.discard();
        }
    }

    private boolean ownerReady(ServerLevel serverLevel, CloneRuntimeState state) {
        CloneRuntime runtime = CloneRuntime.get(serverLevel.getServer());
        if (runtime == null || runtime.sessionToken() != this.syncedSessionToken()) {
            return false;
        }
        ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(state.ownerId);
        return owner != null && owner.isAlive() && !owner.isRemoved() && !owner.hasDisconnected()
                && owner.level().dimension().equals(state.dimension);
    }

    private boolean sourceStillQualified(ServerLevel serverLevel, HurtAttempt attempt) {
        if (attempt.sourceId == null) {
            return false;
        }
        Entity found = serverLevel.getEntity(attempt.sourceId);
        if (!(found instanceof LivingEntity living)) {
            return false;
        }
        ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(this.ownerId());
        return !rejectsPulseTarget(owner, living);
    }

    @Nullable
    private Vec3 acceptedLook(ServerLevel serverLevel, HurtAttempt attempt) {
        Vec3 fallback = CaptureVectors.unit(attempt.lookUnit) ? attempt.lookUnit : this.getLookAngle();
        if (attempt.sourceId == null) {
            return CaptureVectors.unit(fallback) ? fallback : null;
        }
        Entity found = serverLevel.getEntity(attempt.sourceId);
        if (!(found instanceof LivingEntity living)) {
            return CaptureVectors.unit(fallback) ? fallback : null;
        }
        Vec3 aimed = CaptureVectors.aimOrFrozen(attempt.eyeOrigin, living.getBoundingBox().getCenter(), fallback);
        return CaptureVectors.unit(aimed) ? aimed : null;
    }

    private boolean armsHurtGate(CloneRuntimeState state) {
        if (state == null || state.life != CloneLifeState.ACTIVE || state.cosmeticOnly || state.budgetSpent) {
            return false;
        }
        if (state.spec.terminationPolicy() != TerminationPolicy.BREAK_ON_TRIGGER
                || state.response != CloneResponseState.READY) {
            return false;
        }
        DeploymentLease lease = state.deploymentLease;
        return lease != null && !lease.released() && lease.phase() == DeploymentLease.Phase.BODY;
    }

    @Nullable
    private HurtAttempt openHurtAttempt() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        CloneRuntime runtime = CloneRuntime.get(serverLevel.getServer());
        if (runtime == null) {
            return null;
        }
        CloneRuntimeState state = runtime.find(this.getUUID());
        return state == null ? null : state.hurtAttempt;
    }

    private boolean breaksOnTrigger() {
        return this.ticket != null && this.ticket.spec.terminationPolicy() == TerminationPolicy.BREAK_ON_TRIGGER;
    }

    public static boolean rejectsIncomingAttacker(@Nullable ServerPlayer owner, @Nullable Entity attacker) {
        if (attacker == null) {
            return false;
        }
        if (attacker instanceof CloneEntity) {
            return true;
        }
        if (owner == null) {
            return attacker instanceof Player;
        }
        if (attacker.getUUID().equals(owner.getUUID())) {
            return true;
        }
        if (attacker instanceof ServerPlayer other) {
            return !other.canHarmPlayer(owner);
        }
        if (attacker instanceof Player) {
            return true;
        }
        return ownedBy(owner, attacker) || sameTeam(owner, attacker);
    }

    public static boolean rejectsPulseTarget(@Nullable ServerPlayer owner, @Nullable LivingEntity target) {
        if (owner == null || target == null || !target.isAlive() || target.isSpectator() || target.isRemoved()) {
            return true;
        }
        if (target.getUUID().equals(owner.getUUID()) || target instanceof CloneEntity) {
            return true;
        }
        if (!DamagePermission.allows(owner, target)) {
            return true;
        }
        return ownedBy(owner, target) || sameTeam(owner, target);
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hitByPlayer) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("CloneVersion", SAVE_VERSION);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        this.fromSave = true;
        this.admitted = false;
        super.readAdditionalSaveData(compound);
        this.syncInt(DATA_LIFE, CloneLifeState.ENDED.ordinal());
        this.syncLong(DATA_GENERATION, 0L);
    }

    private boolean ticketValid() {
        if (this.ticket == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        CloneRuntime runtime = CloneRuntime.get(serverLevel.getServer());
        if (runtime == null || !runtime.accepting() || runtime.sessionToken() != this.entityData.get(DATA_SESSION)) {
            return false;
        }
        return this.ticket.sessionToken == runtime.sessionToken()
                && this.ticket.actorGeneration > 0L
                && this.ticket.ownerId != null
                && this.ticket.spec != null;
    }

    private void applyBodyHealth(double bodyHealth) {
        if (!Double.isFinite(bodyHealth)) {
            return;
        }
        AttributeInstance health = this.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(bodyHealth);
        }
        this.setHealth((float) bodyHealth);
    }

    private void syncDeployment(@Nullable UUID deploymentId) {
        Optional<UUID> next = Optional.ofNullable(deploymentId);
        if (!this.entityData.get(DATA_DEPLOYMENT).equals(next)) {
            this.entityData.set(DATA_DEPLOYMENT, next);
        }
    }

    private void syncOwner(@Nullable UUID ownerId) {
        Optional<UUID> next = Optional.ofNullable(ownerId);
        if (!this.entityData.get(DATA_OWNER).equals(next)) {
            this.entityData.set(DATA_OWNER, next);
        }
    }

    private void syncInt(EntityDataAccessor<Integer> accessor, int next) {
        if (this.entityData.get(accessor) != next) {
            this.entityData.set(accessor, next);
        }
    }

    private void syncLong(EntityDataAccessor<Long> accessor, long next) {
        if (this.entityData.get(accessor) != next) {
            this.entityData.set(accessor, next);
        }
    }

    private static boolean ownedBy(ServerPlayer owner, Entity attacker) {
        if (!(attacker instanceof TamableAnimal tame)) {
            return false;
        }
        UUID tameOwner = tame.getOwnerUUID();
        return owner.getUUID().equals(tameOwner);
    }

    private static boolean sameTeam(ServerPlayer owner, Entity attacker) {
        if (attacker instanceof Player) {
            return false;
        }
        Team ownerTeam = owner.getTeam();
        Team attackerTeam = attacker.getTeam();
        return ownerTeam != null && attackerTeam != null && ownerTeam.isAlliedTo(attackerTeam);
    }

    static final class SpawnTicket {
        final long sessionToken;
        final UUID ownerId;
        final long actorGeneration;
        final CloneSpec spec;

        private SpawnTicket(long sessionToken, UUID ownerId, long actorGeneration, CloneSpec spec) {
            this.sessionToken = sessionToken;
            this.ownerId = ownerId;
            this.actorGeneration = actorGeneration;
            this.spec = spec;
        }
    }
}
