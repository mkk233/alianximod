package com.bslx.XueXiMod.shu.technique.release.entity;

import com.bslx.XueXiMod.debug.performance.PerfCounter;
import com.bslx.XueXiMod.debug.performance.PerfMetric;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
import com.bslx.XueXiMod.network.HeldSwordGate;
import com.bslx.XueXiMod.network.HoldRepeat;
import com.bslx.XueXiMod.shu.technique.release.hitbox.TechniqueHitbox;
import com.bslx.XueXiMod.shu.technique.release.hitbox.SizedBoxHitbox;
import com.bslx.XueXiMod.shu.technique.release.motion.TechniqueMotion;
import com.bslx.XueXiMod.shu.technique.release.parting.WaterPartingService;
import com.bslx.XueXiMod.shu.technique.release.payload.ObstacleBodies;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.WeepingCurtain;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHosts;
import com.bslx.XueXiMod.shu.technique.release.deflection.hosts.ArcDeflectionHost;
import com.bslx.XueXiMod.shu.technique.release.deflection.hosts.WeepingDeflectionBinding;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBandMode;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBandSnapshot;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBounds;
import com.bslx.XueXiMod.shu.technique.release.water.ProgressArcBand;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
import java.util.Optional;
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
    private static final EntityDataAccessor<Integer> DATA_STILL_FRAMES =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STILL_FRAME_TICKS =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_STILL_BIRTH =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> DATA_STILL_CANCELLED =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_CASTER =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    /** 燎里后环开始时的世界时刻。-1 表示这次还没有合格命中。 */
    private static final EntityDataAccessor<Long> DATA_RING_BIRTH =
            SynchedEntityData.defineId(TechniqueHitEntity.class, EntityDataSerializers.LONG);
    private static final String LIFETIME_KEY = "XuexiHitLifetime";

    @Nullable
    private UUID casterUuid;
    private boolean deflectsIncoming;
    /** 持续接触结算。只从释放定义抄来，客户端不登记瀑布宿主。 */
    boolean persistentFlow;
    private ArcDeflectionHost arcDeflection;
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
    /** 跟随走廊会被实心方块截断。 */
    private boolean clipsSolids;
    /** 松开、换掉要求的剑或施法者倒下时结束。 */
    private boolean endsWhenReleased;
    /** 持续按键所在的技槽。 */
    private int releaseSlot = -1;
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
    /** 就绪年龄只通知一次。不在这里识别术式名字。 */
    private boolean partingNotified;
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
    int settleSeedPoints;
    com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess settleTide =
            com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess.NONE;

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
        this.entityData.define(DATA_STILL_FRAMES, 0);
        this.entityData.define(DATA_STILL_FRAME_TICKS, 1);
        this.entityData.define(DATA_STILL_BIRTH, StillSheetVisual.UNBORN);
        this.entityData.define(DATA_STILL_CANCELLED, false);
        this.entityData.define(DATA_LIFETIME, 0);
        this.entityData.define(DATA_CASTER, Optional.empty());
        this.entityData.define(DATA_RING_BIRTH, -1L);
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
        if (DATA_CASTER.equals(key)) {
            this.casterUuid = this.entityData.get(DATA_CASTER).orElse(null);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains(LIFETIME_KEY, Tag.TAG_INT)) {
            this.storeLifetime(tag.getInt(LIFETIME_KEY));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt(LIFETIME_KEY, this.lifetime());
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
        boolean recordingNow = PerformanceRecorder.isRecording();
        long updateStart = PerformanceRecorder.begin();
        try {
        if (this.pendingActivation) {
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        if (this.arcSnapshot != null) {
            this.tickArcBand(level);
            return;
        }
        this.previousPosition = this.position();
        if (this.age == Integer.MAX_VALUE) {
            TechniqueHitSettle.onExpire(this, level);
            return;
        }
        this.age++;
        if (this.motion != null) {
            long motionStart = PerformanceRecorder.begin();
            try {
                this.motion.tick(this, level);
            } finally {
                PerformanceRecorder.end(PerfMetric.MOTION, motionStart);
            }
        }
        this.refreshWeepingDeflection();
        if (!this.partingNotified && this.age >= this.hitReadyAge) {
            this.partingNotified = true;
            WaterPartingService.activated(this);
        }
        if (this.endsWhenReleased && this.releaseStopped()) {
            TechniqueHitSettle.onExpire(this, level);
            return;
        }
        if (this.centered) {
            this.previousPosition = this.position();
        }
        if (this.trap) {
            if (this.planted && !this.detonated) {
                AABB box = this.getBoundingBox().inflate(0.3);
                long queryStart = PerformanceRecorder.begin();
                List<LivingEntity> stepped;
                try {
                    stepped = level.getEntitiesOfClass(LivingEntity.class, box,
                            entity -> TechniqueHitSettle.canHit(this, entity));
                } finally {
                    PerformanceRecorder.end(PerfMetric.ENTITY_QUERY, queryStart);
                }
                if (recordingNow) {
                    PerformanceRecorder.add(PerfCounter.ENTITY_QUERY_CALLS, 1L);
                    PerformanceRecorder.add(PerfCounter.ENTITY_CANDIDATES, stepped.size());
                }
                if (!stepped.isEmpty()) {
                    TechniqueHitSettle.detonate(this, level);
                }
            }
        } else if (this.hitbox != null && this.age >= this.hitReadyAge) {
            AABB broad = this.hitbox.broadPhase(this);
            long queryStart = PerformanceRecorder.begin();
            List<LivingEntity> targets;
            try {
                targets = level.getEntitiesOfClass(LivingEntity.class, broad,
                        entity -> TechniqueHitSettle.canHit(this, entity));
            } finally {
                PerformanceRecorder.end(PerfMetric.ENTITY_QUERY, queryStart);
            }
            if (recordingNow) {
                PerformanceRecorder.add(PerfCounter.ENTITY_QUERY_CALLS, 1L);
                PerformanceRecorder.add(PerfCounter.ENTITY_CANDIDATES, targets.size());
            }
            for (LivingEntity target : targets) {
                long narrowStart = PerformanceRecorder.begin();
                boolean narrowPassed;
                try {
                    narrowPassed = this.hitbox.contains(this, target);
                } finally {
                    PerformanceRecorder.end(PerfMetric.NARROW_PHASE, narrowStart);
                }
                if (recordingNow) {
                    PerformanceRecorder.add(PerfCounter.NARROW_PHASE_TESTS, 1L);
                    if (narrowPassed) {
                        PerformanceRecorder.add(PerfCounter.NARROW_PHASE_PASSED, 1L);
                    }
                }
                if (narrowPassed) {
                    TechniqueHitSettle.tryHit(this, level, target);
                }
            }
        }
        if (this.burstOnImpact && !this.isRemoved() && this.previousPosition != null) {
            long clipStart = PerformanceRecorder.begin();
            BlockHitResult blockHit;
            try {
                blockHit = level.clip(new ClipContext(
                        this.previousPosition, this.position(),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            } finally {
                PerformanceRecorder.end(PerfMetric.BLOCK_CLIP, clipStart);
            }
            if (blockHit.getType() == HitResult.Type.BLOCK) {
                Vec3 hitPoint = blockHit.getLocation();
                this.setPos(hitPoint.x, hitPoint.y, hitPoint.z);
                TechniqueHitSettle.onExpire(this, level);
                return;
            }
        }
        int lifeTicks = this.lifetime();
        boolean lifeEnded;
        if (lifeTicks <= 0) {
            lifeEnded = this.age >= lifeTicks;
        } else {
            int expiryTick = ReleaseInput.expiryTick(0, lifeTicks);
            lifeEnded = ReleaseInput.hasExpired(this.age, expiryTick);
        }
        if (lifeEnded) {
            TechniqueHitSettle.onExpire(this, level);
        }
        } finally {
            PerformanceRecorder.end(PerfMetric.HIT_ENTITY_UPDATE, updateStart);
            if (recordingNow) {
                PerformanceRecorder.add(PerfCounter.HIT_ENTITY_UPDATES, 1L);
            }
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
            if (this.level() instanceof ServerLevel serverLevel) {
                this.refreshArcDeflection(serverLevel, 0L);
            }
        }
        this.stampStillBirth();
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
        this.applySpec(caster, spec, spec.lifetime);
    }

    public void applySpec(ServerPlayer caster, TechniqueHitSpec spec, int instanceLifetime) {
        CohortVisual shown = spec.cohortVisual == null ? CohortVisual.SINGLE : spec.cohortVisual.stored();
        this.entityData.set(DATA_DISPLAY_INDEX, shown.displayIndex());
        this.entityData.set(DATA_DISPLAY_COUNT, shown.displayCount());
        this.entityData.set(DATA_DISPLAY_LANE, shown.laneSplit());
        this.entityData.set(DATA_LANE_INDEX, shown.laneIndex());
        this.entityData.set(DATA_LANE_COUNT, shown.laneCount());
        this.casterUuid = caster.getUUID();
        this.entityData.set(DATA_CASTER, Optional.of(caster.getUUID()));
        ReleaseDefinition boundDefinition = ReleaseRegistry.get(spec.techniqueId);
        this.deflectsIncoming = boundDefinition != null && boundDefinition.deflectsProjectiles();
        this.persistentFlow = boundDefinition != null && boundDefinition.persistentFlowSettlement();
        this.storeLifetime(instanceLifetime);
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
        this.settleSeedPoints = spec.settleSeedPoints;
        this.settleControlTicks = spec.settleControlTicks;
        this.settleExtinguish = spec.settleExtinguish;
        this.settleKnockback = spec.settleKnockback;
        this.settleTide = spec.settleTide == null
                ? com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess.NONE
                : spec.settleTide;
        this.trap = spec.trap;
        this.burstOnImpact = spec.burstOnImpact;
        this.clipsSolids = spec.clipsSolids;
        this.endsWhenReleased = spec.endsWhenReleased;
        this.releaseSlot = spec.releaseSlot;
        if (spec.clipsSolids) {
            this.exactVolume = true;
        }
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
        StillSheetVisual stillSheet = spec.stillSheet == null ? StillSheetVisual.NONE : spec.stillSheet;
        this.entityData.set(DATA_STILL_FRAMES, stillSheet.enabled() ? stillSheet.frameCount() : 0);
        this.entityData.set(DATA_STILL_FRAME_TICKS, stillSheet.enabled() ? stillSheet.ticksPerFrame() : 1);
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
            long motionStart = PerformanceRecorder.begin();
            try {
                this.motion.tick(this, serverLevel);
            } finally {
                PerformanceRecorder.end(PerfMetric.MOTION, motionStart);
            }
            this.previousPosition = this.position();
        } else if ((path == TechniqueHitSpec.Path.VORTEX || path == TechniqueHitSpec.Path.FUNCTION)
                && this.motion != null && caster.level() instanceof ServerLevel vortexLevel) {
            long motionStart = PerformanceRecorder.begin();
            try {
                this.motion.tick(this, vortexLevel);
            } finally {
                PerformanceRecorder.end(PerfMetric.MOTION, motionStart);
            }
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
        return this.entityData.get(DATA_LIFETIME);
    }

    private void storeLifetime(int instanceLifetime) {
        this.lifetime = instanceLifetime;
        this.entityData.set(DATA_LIFETIME, instanceLifetime);
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
        Optional<UUID> synced = this.entityData.get(DATA_CASTER);
        if (synced != null && synced.isPresent()) {
            return synced.get();
        }
        return this.casterUuid;
    }

    /** 这次后环的世界时刻。没有触发时为 -1。不改变命中体寿命。 */
    public long rearRingBirth() {
        return this.entityData.get(DATA_RING_BIRTH);
    }

    /**
     * 记一次燎里后环。只改客户端能看见的开始时刻，不缩短命中体寿命。
     * 同一世界时刻重复调用仍是这一轮。
     */
    public void pulseRearRing() {
        if (this.level() == null || this.level().isClientSide() || this.isRemoved()) {
            return;
        }
        this.entityData.set(DATA_RING_BIRTH, this.level().getGameTime());
    }

    /**
     * 当前这轮后环已经播了多久。没有触发、或已经走完一个周期时返回 -1。
     * 用同步来的世界时刻计算，逐帧读取不会把同一轮重新从头播放。
     */
    public double rearRingVisualAge(float partialTicks, double cycleTicks) {
        long started = this.entityData.get(DATA_RING_BIRTH);
        if (started < 0L || cycleTicks <= 0.0d || this.level() == null) {
            return -1.0d;
        }
        double age = this.level().getGameTime() + partialTicks - started;
        if (age < -2.0d || age >= cycleTicks) {
            return -1.0d;
        }
        if (age < 0.0d) {
            return 0.0d;
        }
        return age;
    }

    public boolean deflectsProjectiles() {
        return this.deflectsIncoming;
    }

    @Override
    public void remove(RemovalReason reason) {
        this.releaseWeepingDeflection();
        this.closeArcDeflection();
        super.remove(reason);
    }

    /** 运动之后、命中之前。空水段也要更新，旧宿主才能马上停。 */
    private void refreshWeepingDeflection() {
        if (this.level().isClientSide() || !this.persistentFlow) {
            return;
        }
        WeepingDeflectionBinding.update(this, WeepingCurtain.active(this));
    }

    private void releaseWeepingDeflection() {
        if (this.level().isClientSide() || !this.persistentFlow) {
            return;
        }
        WeepingDeflectionBinding.remove(this);
        WeepingCurtain.forget(this);
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

    public StillSheetVisual stillSheet() {
        return StillSheetVisual.once(this.texture(), this.entityData.get(DATA_STILL_FRAMES),
                this.entityData.get(DATA_STILL_FRAME_TICKS));
    }

    public long visualBirth() {
        return this.entityData.get(DATA_STILL_BIRTH);
    }

    public boolean stillSheetCancelled() {
        return this.entityData.get(DATA_STILL_CANCELLED);
    }

    /** 正式激活时写一次出生时间。暂挂和 applySpec 都不调用。 */
    public void stampStillBirth() {
        if (this.level().isClientSide() || !this.stillSheet().enabled()) {
            return;
        }
        if (this.entityData.get(DATA_STILL_BIRTH) >= 0L) {
            return;
        }
        this.entityData.set(DATA_STILL_BIRTH, this.level().getGameTime());
    }

    /** 显式取消视觉。命中到期移除不走这里。 */
    public void cancelStillSheet() {
        if (this.level().isClientSide()) {
            return;
        }
        this.entityData.set(DATA_STILL_CANCELLED, true);
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

    public boolean clipsSolids() {
        return this.clipsSolids;
    }

    private boolean releaseStopped() {
        ServerPlayer caster = this.getCaster();
        if (caster == null || !caster.isAlive()) {
            return true;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(this.entityData.get(DATA_TECHNIQUE_ID));
        if (definition != null && definition.requiresHeldSword()
                && !HeldSwordGate.allows(definition, caster.getMainHandItem())) {
            return true;
        }
        return this.releaseSlot < 0 || !HoldRepeat.held(caster.getUUID(), this.releaseSlot);
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
        this.storeLifetime(snapshot.lifeTicks());
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
            long queryStart = PerformanceRecorder.begin();
            List<LivingEntity> targets;
            try {
                targets = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                        entity -> TechniqueHitSettle.canHit(this, entity));
            } finally {
                PerformanceRecorder.end(PerfMetric.ENTITY_QUERY, queryStart);
            }
            boolean recordingNow = PerformanceRecorder.isRecording();
            if (recordingNow) {
                PerformanceRecorder.add(PerfCounter.ENTITY_QUERY_CALLS, 1L);
                PerformanceRecorder.add(PerfCounter.ENTITY_CANDIDATES, targets.size());
            }
            for (LivingEntity target : targets) {
                AABB targetBox = target.getBoundingBox();
                ArcBounds targetBounds = new ArcBounds(targetBox.minX, targetBox.minY, targetBox.minZ,
                        targetBox.maxX, targetBox.maxY, targetBox.maxZ);
                long narrowStart = PerformanceRecorder.begin();
                boolean narrowPassed;
                try {
                    narrowPassed = ProgressArcBand.intersectsSlice(this.arcSnapshot, targetBounds, cursor.fromProgress(), cursor.toProgress());
                } finally {
                    PerformanceRecorder.end(PerfMetric.NARROW_PHASE, narrowStart);
                }
                if (recordingNow) {
                    PerformanceRecorder.add(PerfCounter.NARROW_PHASE_TESTS, 1L);
                    if (narrowPassed) {
                        PerformanceRecorder.add(PerfCounter.NARROW_PHASE_PASSED, 1L);
                    }
                }
                if (narrowPassed) {
                    TechniqueHitSettle.tryHit(this, level, target);
                }
            }
        }
        this.lastProcessedProgress = cursor.lastProgress();
        this.arcDamageClosed = cursor.damageClosed();
        this.refreshArcDeflection(level, ageTicks);
        if (ageTicks >= this.arcSnapshot.lifeTicks()) {
            TechniqueHitSettle.onExpire(this, level);
        }
    }

    private void refreshArcDeflection(ServerLevel level, long ageTicks) {
        if (!this.deflectsIncoming || this.level().isClientSide()) {
            this.closeArcDeflection();
            return;
        }
        ArcBandSnapshot snapshot = this.arcSnapshot;
        boolean open = snapshot != null
                && snapshot.mode() == ArcBandMode.RADIAL_BLADE
                && snapshot.birthTick() >= 0L
                && ageTicks >= 0L
                && ageTicks <= snapshot.activeTicks()
                && this.isAlive()
                && !this.isRemoved()
                && this.casterUuid != null;
        if (!open) {
            this.closeArcDeflection();
            return;
        }
        if (this.arcDeflection == null) {
            this.arcDeflection = ArcDeflectionHost.bind(this);
            if (this.arcDeflection != null) {
                DeflectionHosts.register(this.arcDeflection);
            }
        }
    }

    private void closeArcDeflection() {
        if (this.arcDeflection == null) {
            return;
        }
        DeflectionHosts.unregister(this.arcDeflection.key());
        this.arcDeflection = null;
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
