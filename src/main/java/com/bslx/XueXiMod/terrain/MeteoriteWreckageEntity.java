package com.bslx.XueXiMod.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

/**
 * 无伤待安放残骸。只保存身份和一次崩地/安放状态，落地后才处理地形。
 * 不造成伤害，不产种，不接续，不超时删除，也不强载区块。
 * 调用方必须已经完成父子结算。本类不读取施法帧。
 */
public class MeteoriteWreckageEntity extends Entity {
    public static final int RETRY_TICKS = 20;
    private static final double FALL_PER_TICK = 0.04D;

    private UUID ownerIdentifier;
    private UUID wreckageIdentifier;
    private boolean breakRequested;
    private boolean breakAlreadyDone;
    private boolean awaitingPlacement = true;
    private boolean placementCommitted;
    private boolean impactReady;
    private int impactX;
    private int impactY;
    private int impactZ;
    private int committedX;
    private int committedY;
    private int committedZ;
    private int settleCooldown;

    public MeteoriteWreckageEntity(EntityType<? extends MeteoriteWreckageEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = false;
        this.setInvulnerable(true);
        this.setNoGravity(false);
    }

    /**
     * 结算完成之后生成一颗残骸。类型还没挂到总线上、或出生点区块没加载时返回 false，不强载。
     */
    public static boolean spawnAfterSettlement(ServerLevel level, UUID ownerIdentifier, Vec3 spawnPosition,
                                               Vec3 initialMotion, boolean allowOnce) {
        if (level == null || ownerIdentifier == null || spawnPosition == null) {
            return false;
        }
        if (!MeteoriteWreckageTypes.WRECKAGE.isPresent()) {
            return false;
        }
        BlockPos spawnCell = BlockPos.containing(spawnPosition);
        if (!level.isLoaded(spawnCell) || !level.isInWorldBounds(spawnCell)) {
            return false;
        }
        MeteoriteWreckageEntity wreckage = new MeteoriteWreckageEntity(MeteoriteWreckageTypes.WRECKAGE.get(), level);
        wreckage.ownerIdentifier = ownerIdentifier;
        wreckage.wreckageIdentifier = UUID.randomUUID();
        wreckage.breakRequested = allowOnce;
        wreckage.breakAlreadyDone = false;
        wreckage.awaitingPlacement = true;
        wreckage.placementCommitted = false;
        wreckage.impactReady = false;
        wreckage.setPos(spawnPosition.x, spawnPosition.y, spawnPosition.z);
        Vec3 motion = initialMotion == null ? Vec3.ZERO : initialMotion;
        wreckage.setDeltaMovement(motion);
        return level.addFreshEntity(wreckage);
    }

    public UUID ownerId() {
        return this.ownerIdentifier;
    }

    public UUID wreckageId() {
        return this.wreckageIdentifier;
    }

    public boolean breakDone() {
        return this.breakAlreadyDone;
    }

    public boolean awaitingPlacement() {
        return this.awaitingPlacement;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.wreckageIdentifier == null) {
            this.wreckageIdentifier = UUID.randomUUID();
        }
        if (this.placementCommitted) {
            this.discard();
            return;
        }
        this.fallOnce();
        if (this.settleCooldown > 0) {
            this.settleCooldown--;
            return;
        }
        if (!this.onGround() || !this.verticalCollisionBelow) {
            return;
        }
        this.settle(serverLevel);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        return true;
    }

    @Override
    public void thunderHit(ServerLevel level, net.minecraft.world.entity.LightningBolt bolt) {
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    protected void onBelowWorld() {
        if (this.level().isClientSide()) {
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.setDeltaMovement(Vec3.ZERO);
        this.setPos(this.getX(), serverLevel.getMinBuildHeight() + 1.0D, this.getZ());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.ownerIdentifier = tag.hasUUID("OwnerUuid") ? tag.getUUID("OwnerUuid") : null;
        this.wreckageIdentifier = tag.hasUUID("WreckageId") ? tag.getUUID("WreckageId") : UUID.randomUUID();
        this.breakRequested = tag.getBoolean("BreakRequested");
        this.breakAlreadyDone = tag.getBoolean("BreakDone");
        this.placementCommitted = tag.getBoolean("PlacementCommitted");
        this.awaitingPlacement = tag.contains("AwaitingPlacement")
                ? tag.getBoolean("AwaitingPlacement")
                : !this.placementCommitted;
        this.impactReady = tag.getBoolean("ImpactReady");
        this.impactX = tag.getInt("ImpactX");
        this.impactY = tag.getInt("ImpactY");
        this.impactZ = tag.getInt("ImpactZ");
        this.committedX = tag.getInt("CommittedX");
        this.committedY = tag.getInt("CommittedY");
        this.committedZ = tag.getInt("CommittedZ");
        this.settleCooldown = Math.max(0, tag.getInt("SettleCooldown"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerIdentifier != null) {
            tag.putUUID("OwnerUuid", this.ownerIdentifier);
        }
        if (this.wreckageIdentifier != null) {
            tag.putUUID("WreckageId", this.wreckageIdentifier);
        }
        tag.putBoolean("BreakRequested", this.breakRequested);
        tag.putBoolean("BreakDone", this.breakAlreadyDone);
        tag.putBoolean("AwaitingPlacement", this.awaitingPlacement);
        tag.putBoolean("PlacementCommitted", this.placementCommitted);
        tag.putBoolean("ImpactReady", this.impactReady);
        tag.putInt("ImpactX", this.impactX);
        tag.putInt("ImpactY", this.impactY);
        tag.putInt("ImpactZ", this.impactZ);
        tag.putInt("CommittedX", this.committedX);
        tag.putInt("CommittedY", this.committedY);
        tag.putInt("CommittedZ", this.committedZ);
        tag.putInt("SettleCooldown", this.settleCooldown);
    }

    private void fallOnce() {
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -FALL_PER_TICK, 0.0D));
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        Vec3 motion = this.getDeltaMovement();
        if (this.onGround()) {
            this.setDeltaMovement(motion.x * 0.7D, motion.y * -0.5D, motion.z * 0.7D);
        } else {
            this.setDeltaMovement(motion.scale(0.98D));
        }
    }

    private void settle(ServerLevel serverLevel) {
        if (!this.impactReady) {
            this.captureImpact(serverLevel);
            if (!this.impactReady) {
                this.settleCooldown = RETRY_TICKS;
                return;
            }
        }
        BlockPos impact = new BlockPos(this.impactX, this.impactY, this.impactZ);
        if (this.breakRequested && !this.breakAlreadyDone) {
            TerrainMutationService.BreakGroundResult breakResult = TerrainMutationService.breakExistingOnce(
                    serverLevel,
                    this.ownerIdentifier,
                    impact,
                    true,
                    false,
                    this::markBreakExecuted);
            if (breakResult == TerrainMutationService.BreakGroundResult.EXECUTED) {
                com.bslx.XueXiMod.carrier.BatchTriggerVisuals.dust(serverLevel, Vec3.atCenterOf(impact));
            }
        }
        if (!this.awaitingPlacement || this.placementCommitted) {
            return;
        }
        TerrainMutationService.PlaceOutcome outcome = TerrainMutationService.placeMeteorite(
                serverLevel, this.ownerIdentifier, impact);
        if (outcome.placeResult() == TerrainMutationService.PlaceResult.PLACED && outcome.placedCell() != null) {
            this.placementCommitted = true;
            this.awaitingPlacement = false;
            this.committedX = outcome.placedCell().getX();
            this.committedY = outcome.placedCell().getY();
            this.committedZ = outcome.placedCell().getZ();
            this.discard();
            return;
        }
        this.settleCooldown = RETRY_TICKS;
    }

    private void captureImpact(ServerLevel serverLevel) {
        BlockPos feet = this.blockPosition();
        if (!serverLevel.isLoaded(feet) || !serverLevel.isInWorldBounds(feet)) {
            return;
        }
        BlockPos below = feet.below();
        if (!serverLevel.isInWorldBounds(below)) {
            this.impactX = feet.getX();
            this.impactY = feet.getY();
            this.impactZ = feet.getZ();
            this.impactReady = true;
            return;
        }
        if (!serverLevel.isLoaded(below)) {
            return;
        }
        this.impactX = below.getX();
        this.impactY = below.getY();
        this.impactZ = below.getZ();
        this.impactReady = true;
    }

    private void markBreakExecuted() {
        this.breakAlreadyDone = true;
    }
}
