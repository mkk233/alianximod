package com.bslx.XueXiMod.shu.technique.release.clone.core;

import com.bslx.XueXiMod.shu.technique.release.clone.CloneBalance;
import com.bslx.XueXiMod.shu.technique.release.firecast.DamagePermission;
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
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class CloneEntity extends PathfinderMob {
    static final int SAVE_VERSION = 1;
    private static final float EYE_HEIGHT = 1.74F;

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

    private long actorGeneration;
    private boolean fromSave;
    private boolean admitted;
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
        this.applyBodyHealth(spawnTicket.spec.bodyHealth());
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
        ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(this.ownerId());
        if (rejectsIncomingAttacker(owner, source == null ? null : source.getEntity())) {
            return false;
        }
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
