package com.bslx.XueXiMod.shu.technique.release.entity;

import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
import com.bslx.XueXiMod.shu.technique.release.hitbox.TechniqueHitbox;
import com.bslx.XueXiMod.shu.technique.release.hitbox.SizedBoxHitbox;
import com.bslx.XueXiMod.shu.technique.release.motion.TechniqueMotion;
import com.bslx.XueXiMod.shu.technique.release.payload.ObstacleBodies;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBandSnapshot;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBounds;
import com.bslx.XueXiMod.shu.technique.release.water.ProgressArcBand;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TechniqueHitEntity extends Entity {
    private static final EntityDataAccessor<String> DATA_TECHNIQUE_ID =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_VISUAL =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_MODEL_SCALE =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_TEXTURE =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_LENGTH =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_WIDTH =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ROLL =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_ARC_SNAPSHOT =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_DISPLAY_INDEX =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DISPLAY_COUNT =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_DISPLAY_LANE =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_LANE_INDEX =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LANE_COUNT =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID casterUuid;
    private TechniqueMotion motion;
    private TechniqueHitbox hitbox;
    private int lifetime;
    private double speed;
    private boolean lockThisCast;
    private boolean dropThisCast;
    private boolean centered;
    private boolean pose3d;
    private boolean beam;
    private boolean pinned;
    private boolean trap;
    boolean burstOnImpact;
    private boolean planted;
    /** 盒子高度就是当前水体，不再保底成一格。 */
    private boolean exactVolume;
    /** 宿主组尚未整组成功时不结算伤害。旧路径保持关闭。 */
    private boolean pendingActivation;
    @Nullable
    private ArcBandSnapshot arcSnapshot;
    private double lastProcessedProgress;
    private boolean arcDamageClosed;
    boolean detonated;
    private final ClientTrack clientTrack = new ClientTrack();
    int settleControlTicks;
    boolean settleExtinguish;
    float settleKnockback;
    private double arcRadius;
    private double arcDegrees;
    private double arcHeight;
    private Vec3 boxSize = Vec3.ZERO;
    private double length = 1.0;
    private double width = 1.0;
    private double height = 1.0;
    int hitInterval;
    int hitReadyAge;
    @Nullable
    TechniqueHitSpec.HitListener listener;
    private int age;
    private Vec3 origin = Vec3.ZERO;
    Vec3 forward = new Vec3(0.0, 0.0, 1.0);
    private String functionId = "";
    private Vec3 previousPosition = Vec3.ZERO;
    @Nullable
    private Vec3 anchor;
    @Nullable
    private Vec3 aim;
    final Set<UUID> hitOnce = new HashSet<>();
    final Map<UUID, Long> lastHitTick = new HashMap<>();
    float settleDamage = Float.NaN;
    EnchantmentResolution settleEnchantments;
    ReleaseBatch.Tally settleTally;
    int settleCopyIndex;
    int settleBurnSeconds;

    public TechniqueHitEntity(EntityType<? extends TechniqueHitEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_TECHNIQUE_ID, "");
        this.entityData.define(DATA_VISUAL, 0);
        this.entityData.define(DATA_MODEL_SCALE, 1.0F);
        this.entityData.define(DATA_TEXTURE, "");
        this.entityData.define(DATA_LENGTH, 1.0F);
        this.entityData.define(DATA_WIDTH, 1.0F);
        this.entityData.define(DATA_HEIGHT, 1.0F);
        this.entityData.define(DATA_ROLL, 0.0F);
        this.entityData.define(DATA_ARC_SNAPSHOT, "");
        this.entityData.define(DATA_DISPLAY_INDEX, 0);
        this.entityData.define(DATA_DISPLAY_COUNT, 1);
        this.entityData.define(DATA_DISPLAY_LANE, false);
        this.entityData.define(DATA_LANE_INDEX, 0);
        this.entityData.define(DATA_LANE_COUNT, 1);
    }

    @Override
    protected AABB makeBoundingBox() {
        AABB arcBox = this.arcWorldBox();
        return arcBox != null ? arcBox : super.makeBoundingBox();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_ARC_SNAPSHOT.equals(key)) {
            ArcBandSnapshot unpacked = ArcBandSnapshot.unpack(this.entityData.get(DATA_ARC_SNAPSHOT));
            if (unpacked != null) {
                this.arcSnapshot = unpacked;
                AABB arcBox = this.arcWorldBox();
                if (arcBox != null) {
                    this.setBoundingBox(arcBox);
                }
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return ObstacleBodies.blocks(this);
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void lerpTo(double targetX, double targetY, double targetZ, float yaw, float pitch, int steps, boolean teleport) {
        if (this.level().isClientSide()) {
            this.clientTrack.onClientMove(this, targetX, targetY, targetZ, yaw, pitch);
            return;
        }
        super.lerpTo(targetX, targetY, targetZ, yaw, pitch, steps, teleport);
    }

    @Override
    public void moveTo(double targetX, double targetY, double targetZ, float yaw, float pitch) {
        if (this.level().isClientSide()) {
            this.clientTrack.onClientMove(this, targetX, targetY, targetZ, yaw, pitch);
            return;
        }
        super.moveTo(targetX, targetY, targetZ, yaw, pitch);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.clientTrack.afterClientTick(this);
            if (this.hasArcBand()) {
                return;
            }
            this.clientDecor();
            return;
        }
        if (this.pendingActivation) {
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        if (this.arcSnapshot != null) {
            this.tickArcBand(level);
            return;
        }
        this.previousPosition = this.position();
        this.age++;
        if (this.motion != null) {
            this.motion.tick(this, level);
        }
        if (this.centered) {
            this.previousPosition = this.position();
        }
        if (this.trap) {
            if (this.planted && !this.detonated) {
                AABB box = this.getBoundingBox().inflate(0.3);
                List<LivingEntity> stepped = level.getEntitiesOfClass(LivingEntity.class, box,
                        entity -> TechniqueHitSettle.canHit(this, entity));
                if (!stepped.isEmpty()) {
                    TechniqueHitSettle.detonate(this, level);
                }
            }
        } else if (this.hitbox != null && this.age >= this.hitReadyAge) {
            AABB broad = this.hitbox.broadPhase(this);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, broad,
                    entity -> TechniqueHitSettle.canHit(this, entity));
            for (LivingEntity target : targets) {
                if (this.hitbox.contains(this, target)) {
                    TechniqueHitSettle.tryHit(this, level, target);
                }
            }
        }
        if (this.burstOnImpact && !this.isRemoved() && this.previousPosition != null) {
            BlockHitResult blockHit = level.clip(new ClipContext(
                    this.previousPosition, this.position(),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (blockHit.getType() == HitResult.Type.BLOCK) {
                Vec3 hitPoint = blockHit.getLocation();
                this.setPos(hitPoint.x, hitPoint.y, hitPoint.z);
                TechniqueHitSettle.onExpire(this, level);
                return;
            }
        }
        if (this.age >= this.lifetime) {
            TechniqueHitSettle.onExpire(this, level);
        }
    }

    /** 粒子只在客户端逻辑拍生成，避免渲染帧里按光束长度狂撒。 */
    private void clientDecor() {
        int visual = this.getVisual();
        if (visual == 4) {
            this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY() + 0.2, this.getZ(), 0.0, 0.01, 0.0);
        } else if (visual == 3 && (this.tickCount & 7) == 0) {
            this.level().addParticle(ParticleTypes.CRIMSON_SPORE, this.getX(), this.getY() + 0.2, this.getZ(), 0.0, 0.02, 0.0);
        }
    }

    public void holdActivation() {
        this.pendingActivation = true;
    }

    public void releaseActivation() {
        this.pendingActivation = false;
        if (this.arcSnapshot != null && this.arcSnapshot.birthTick() < 0L && !this.level().isClientSide()) {
            this.adoptArc(this.arcSnapshot.withBirth(this.level().getGameTime()));
        }
    }

    public boolean hasArcBand() {
        if (this.arcSnapshot != null) {
            return true;
        }
        String packed = this.entityData.get(DATA_ARC_SNAPSHOT);
        return packed != null && !packed.isEmpty();
    }

    @Nullable
    public ArcBandSnapshot arcBand() {
        if (this.arcSnapshot == null) {
            this.arcSnapshot = ArcBandSnapshot.unpack(this.entityData.get(DATA_ARC_SNAPSHOT));
        }
        return this.arcSnapshot;
    }

    public void applySpec(ServerPlayer caster, TechniqueHitSpec spec) {
        CohortVisual shown = spec.cohortVisual == null ? CohortVisual.SINGLE : spec.cohortVisual.stored();
        this.entityData.set(DATA_DISPLAY_INDEX, shown.displayIndex());
        this.entityData.set(DATA_DISPLAY_COUNT, shown.displayCount());
        this.entityData.set(DATA_DISPLAY_LANE, shown.laneSplit());
        this.entityData.set(DATA_LANE_INDEX, shown.laneIndex());
        this.entityData.set(DATA_LANE_COUNT, shown.laneCount());
        this.casterUuid = caster.getUUID();
        this.lifetime = spec.lifetime;
        this.speed = spec.speed;
        this.lockThisCast = spec.lockThisCast;
        this.dropThisCast = spec.dropThisCast;
        this.centered = spec.centered;
        this.pose3d = spec.pose3d;
        this.arcRadius = spec.arcRadius;
        this.arcDegrees = spec.arcDegrees;
        this.arcHeight = spec.arcHeight;
        this.boxSize = spec.boxSize == null ? Vec3.ZERO : spec.boxSize;
        this.hitInterval = spec.hitInterval;
        this.hitReadyAge = Math.max(0, spec.hitReadyAge);
        this.listener = spec.listener;
        this.settleDamage = spec.settleDamage;
        this.settleEnchantments = spec.settleEnchantments;
        this.settleTally = spec.settleTally;
        this.settleCopyIndex = spec.settleCopyIndex;
        this.settleBurnSeconds = spec.settleBurnSeconds;
        this.settleControlTicks = spec.settleControlTicks;
        this.settleExtinguish = spec.settleExtinguish;
        this.settleKnockback = spec.settleKnockback;
        this.trap = spec.trap;
        this.burstOnImpact = spec.burstOnImpact;
        if (spec.blocksMovement) ObstacleBodies.track(this, spec.length, spec.width, spec.height);
        this.anchor = spec.anchor;
        this.aim = spec.aim;
        this.length = spec.length;
        this.width = spec.width;
        this.height = spec.height;
        this.functionId = spec.functionId == null ? "" : spec.functionId;
        if (spec.frozenArc != null) {
            this.installArc(spec.frozenArc);
            return;
        }
        this.origin = spec.start != null ? spec.start : caster.getEyePosition();
        this.forward = spec.aim != null && spec.aim.lengthSqr() > 1.0E-8
                ? spec.aim.normalize()
                : caster.getLookAngle();
        TechniqueHitSpec.Path path = spec.path;
        if (path == null) {
            path = switch (spec.motion) {
                case DROP -> TechniqueHitSpec.Path.DROP;
                case FOLLOW -> TechniqueHitSpec.Path.FOLLOW;
                case SWING -> TechniqueHitSpec.Path.STILL_AIM;
                case PROJECTILE -> TechniqueHitSpec.Path.STRAIGHT;
            };
        }
        if (spec.dropThisCast) {
            path = TechniqueHitSpec.Path.DROP;
        }
        this.beam = path == TechniqueHitSpec.Path.FOLLOW;
        this.pinned = this.beam && spec.start != null;
        this.entityData.set(DATA_TECHNIQUE_ID, spec.techniqueId == null ? "" : spec.techniqueId);
        this.entityData.set(DATA_VISUAL, spec.visual);
        this.entityData.set(DATA_MODEL_SCALE, spec.modelScale);
        this.entityData.set(DATA_TEXTURE, spec.texture == null ? "" : spec.texture);
        this.entityData.set(DATA_LENGTH, (float) spec.length);
        this.entityData.set(DATA_WIDTH, (float) spec.width);
        this.entityData.set(DATA_HEIGHT, (float) spec.height);
        this.entityData.set(DATA_ROLL, spec.roll);
        this.motion = spec.trap ? new com.bslx.XueXiMod.shu.technique.release.motion.TrapFallMotion() : TechniqueHitKinds.motion(path);
        if (spec.tracking && !spec.trap && (path == TechniqueHitSpec.Path.STRAIGHT
                || path == TechniqueHitSpec.Path.DROP || path == TechniqueHitSpec.Path.VORTEX)) {
            this.motion = new com.bslx.XueXiMod.shu.technique.release.motion.TrackingMotion(path);
        }
        this.hitbox = spec.path == null && spec.texture == null ? TechniqueHitKinds.hitbox(spec.hitbox) : new SizedBoxHitbox();
        if (path == TechniqueHitSpec.Path.STILL_AIM && this.motion != null && caster.level() instanceof ServerLevel serverLevel) {
            this.motion.tick(this, serverLevel);
            this.previousPosition = this.position();
        } else if ((path == TechniqueHitSpec.Path.VORTEX || path == TechniqueHitSpec.Path.FUNCTION)
                && this.motion != null && caster.level() instanceof ServerLevel vortexLevel) {
            this.motion.tick(this, vortexLevel);
            this.previousPosition = this.position();
        } else if (this.centered && this.anchor != null) {
            this.placeAt(this.anchor, this.aim != null ? this.aim : this.forward);
            this.previousPosition = this.anchor;
        } else {
            this.placeAt(this.origin, this.forward);
        }
    }

    public void placeAt(Vec3 position, Vec3 direction) {
        this.setPos(position.x, position.y, position.z);
        if (direction != null && direction.lengthSqr() > 1.0E-8) {
            Vec3 unitDirection = direction.normalize();
            double horizontal = Math.sqrt(unitDirection.x * unitDirection.x + unitDirection.z * unitDirection.z);
            this.setYRot((float) (Math.atan2(unitDirection.z, unitDirection.x) * (180.0 / Math.PI)) - 90.0F);
            this.setXRot((float) (-Math.atan2(unitDirection.y, horizontal) * (180.0 / Math.PI)));
        }
    }

    @Nullable
    public ServerPlayer getCaster() {
        if (this.casterUuid == null || this.level().isClientSide()) {
            return null;
        }
        MinecraftServer server = this.level().getServer();
        if (server == null) {
            return null;
        }
        return server.getPlayerList().getPlayer(this.casterUuid);
    }

    public int age() {
        return this.age;
    }

    public int lifetime() {
        return this.lifetime;
    }

    public double speed() {
        return this.speed;
    }

    public boolean lockThisCast() {
        return this.lockThisCast;
    }

    public boolean dropThisCast() {
        return this.dropThisCast;
    }

    public boolean centered() {
        return this.centered;
    }

    public boolean pose3d() {
        return this.pose3d;
    }

    public boolean beam() {
        return this.beam;
    }

    public boolean pinned() {
        return this.pinned;
    }

    public boolean planted() {
        return this.planted;
    }

    public void setPlanted(boolean planted) {
        this.planted = planted;
    }

    public float roll() {
        return this.entityData.get(DATA_ROLL);
    }

    public double arcRadius() {
        return this.arcRadius;
    }

    public double arcDegrees() {
        return this.arcDegrees;
    }

    public double arcHeight() {
        return this.arcHeight;
    }

    public Vec3 boxSize() {
        return this.boxSize;
    }

    public Vec3 origin() {
        return this.origin;
    }

    public Vec3 forward() {
        return this.forward;
    }

    public String functionId() {
        return this.functionId;
    }

    public Vec3 previousPosition() {
        return this.previousPosition;
    }

    @Nullable
    public Vec3 anchor() {
        return this.anchor;
    }

    @Nullable
    public Vec3 aim() {
        return this.aim;
    }

    public UUID casterUuid() {
        return this.casterUuid;
    }

    public void setOrigin(Vec3 origin) {
        this.origin = origin;
    }

    public void setForward(Vec3 forward) {
        this.forward = forward;
    }

    public boolean canTrack(LivingEntity target) {
        return TechniqueHitSettle.canHit(this, target)
                && (this.hitInterval > 0 || !this.hitOnce.contains(target.getUUID()));
    }

    public String getTechniqueId() {
        return this.entityData.get(DATA_TECHNIQUE_ID);
    }

    public int getVisual() {
        return this.entityData.get(DATA_VISUAL);
    }

    public int displayCopyIndex() {
        return this.entityData.get(DATA_DISPLAY_INDEX);
    }

    public int displayCopyCount() {
        return this.entityData.get(DATA_DISPLAY_COUNT);
    }

    public boolean displayLaneSplit() {
        return this.entityData.get(DATA_DISPLAY_LANE);
    }

    public int displayLaneIndex() {
        return this.entityData.get(DATA_LANE_INDEX);
    }

    public int displayLaneCount() {
        return this.entityData.get(DATA_LANE_COUNT);
    }

    public CohortVisual cohortVisual() {
        return CohortVisual.read(this.displayCopyIndex(), this.displayCopyCount(),
                this.displayLaneIndex(), this.displayLaneCount(), this.displayLaneSplit());
    }

    public float getModelScale() {
        return this.entityData.get(DATA_MODEL_SCALE);
    }

    public String texture() {
        return this.entityData.get(DATA_TEXTURE);
    }

    public double length() {
        return this.level().isClientSide() ? this.entityData.get(DATA_LENGTH) : this.length;
    }

    public double width() {
        return this.level().isClientSide() ? this.entityData.get(DATA_WIDTH) : this.width;
    }

    public double height() {
        return this.level().isClientSide() ? this.entityData.get(DATA_HEIGHT) : this.height;
    }

    public boolean exactVolume() {
        return this.exactVolume;
    }

    public double hitHeight() {
        if (this.exactVolume) {
            return this.height() > 0.0 ? this.height() : 0.0;
        }
        if (this.height() <= 0.05) {
            return Math.max(this.length(), 1.0);
        }
        return Math.max(this.height(), 1.0);
    }

    public void resizeVolume(double along, double across, double vertical) {
        this.exactVolume = true;
        this.length = along;
        this.width = across;
        this.height = vertical;
        this.entityData.set(DATA_LENGTH, (float) along);
        this.entityData.set(DATA_WIDTH, (float) across);
        this.entityData.set(DATA_HEIGHT, (float) vertical);
        double reach = Math.max(along, Math.max(across, vertical)) * 0.5;
        this.setBoundingBox(new AABB(
                this.getX() - reach, this.getY() - reach, this.getZ() - reach,
                this.getX() + reach, this.getY() + reach, this.getZ() + reach));
    }

    private void installArc(ArcBandSnapshot snapshot) {
        this.motion = null;
        this.hitbox = null;
        this.centered = false;
        this.adoptArc(snapshot);
        this.forward = new Vec3(snapshot.forwardAxis().east(), snapshot.forwardAxis().above(), snapshot.forwardAxis().south());
        this.origin = new Vec3(snapshot.origin().east(), snapshot.origin().above(), snapshot.origin().south());
        this.placeAt(this.origin, this.forward);
        this.lifetime = snapshot.lifeTicks();
    }

    private void adoptArc(ArcBandSnapshot snapshot) {
        this.arcSnapshot = snapshot;
        this.entityData.set(DATA_ARC_SNAPSHOT, snapshot.pack());
    }

    private void tickArcBand(ServerLevel level) {
        if (this.arcSnapshot.birthTick() < 0L) {
            return;
        }
        long ageTicks = level.getGameTime() - this.arcSnapshot.birthTick();
        if (ageTicks < 0L) {
            ageTicks = 0L;
        }
        ProgressArcBand.SweepCursor cursor = ProgressArcBand.advance(
                this.lastProcessedProgress, this.arcDamageClosed, ageTicks, this.arcSnapshot.activeTicks());
        if (cursor.swept()) {
            ArcBounds broad = ProgressArcBand.sliceBroadBounds(this.arcSnapshot, cursor.fromProgress(), cursor.toProgress());
            AABB searchBox = new AABB(broad.lowEast(), broad.lowAbove(), broad.lowSouth(),
                    broad.highEast(), broad.highAbove(), broad.highSouth());
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                    entity -> TechniqueHitSettle.canHit(this, entity));
            for (LivingEntity target : targets) {
                AABB targetBox = target.getBoundingBox();
                ArcBounds targetBounds = new ArcBounds(targetBox.minX, targetBox.minY, targetBox.minZ,
                        targetBox.maxX, targetBox.maxY, targetBox.maxZ);
                if (ProgressArcBand.intersectsSlice(this.arcSnapshot, targetBounds, cursor.fromProgress(), cursor.toProgress())) {
                    TechniqueHitSettle.tryHit(this, level, target);
                }
            }
        }
        this.lastProcessedProgress = cursor.lastProgress();
        this.arcDamageClosed = cursor.damageClosed();
        if (ageTicks >= this.arcSnapshot.lifeTicks()) {
            TechniqueHitSettle.onExpire(this, level);
        }
    }

    @Nullable
    private AABB arcWorldBox() {
        ArcBandSnapshot snapshot = this.arcBand();
        if (snapshot == null) {
            return null;
        }
        ArcBounds broad = ProgressArcBand.sliceBroadBounds(snapshot, 0.0, 1.0);
        return new AABB(broad.lowEast(), broad.lowAbove(), broad.lowSouth(),
                broad.highEast(), broad.highAbove(), broad.highSouth());
    }
}
