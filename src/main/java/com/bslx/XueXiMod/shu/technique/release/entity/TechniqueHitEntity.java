package com.bslx.XueXiMod.shu.technique.release.entity;

import com.bslx.XueXiMod.carrier.BatchFlightBook;
import com.bslx.XueXiMod.debug.performance.PerfCounter;
import com.bslx.XueXiMod.debug.performance.PerfMetric;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
import com.bslx.XueXiMod.network.HeldSwordGate;
import com.bslx.XueXiMod.network.HoldRepeat;
import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad;
import com.bslx.XueXiMod.shu.technique.effects.LockTargetService;
import com.bslx.XueXiMod.shu.technique.release.aim.LaunchAim;
import com.bslx.XueXiMod.shu.technique.release.DropTilt;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.batch.BatchFollow;
import com.bslx.XueXiMod.shu.technique.release.batch.HitClock;
import com.bslx.XueXiMod.shu.technique.release.batch.ImpactPartition;
import com.bslx.XueXiMod.shu.technique.release.batch.MotionProfile;
import com.bslx.XueXiMod.shu.technique.release.batch.OcclusionSnapshot;
import com.bslx.XueXiMod.shu.technique.release.batch.RootCastBindings;
import com.bslx.XueXiMod.shu.technique.release.hitbox.TechniqueHitbox;
import com.bslx.XueXiMod.shu.technique.release.hitbox.SizedBoxHitbox;
import com.bslx.XueXiMod.shu.technique.release.motion.DropMotion;
import com.bslx.XueXiMod.shu.technique.release.motion.ParabolaMotion;
import com.bslx.XueXiMod.shu.technique.release.motion.ProjectileMotion;
import com.bslx.XueXiMod.shu.technique.release.motion.TechniqueMotion;
import com.bslx.XueXiMod.shu.technique.release.motion.TrackingMotion;
import com.bslx.XueXiMod.shu.technique.release.motion.TrackingSupport;
import com.bslx.XueXiMod.shu.technique.release.motion.TrapFallMotion;
import com.bslx.XueXiMod.shu.technique.release.motion.UpwellMotion;
import com.bslx.XueXiMod.shu.technique.release.motion.VortexMotion;
import com.bslx.XueXiMod.shu.technique.release.motion.WeepingLipMotion;
import com.bslx.XueXiMod.shu.technique.release.parting.WaterPartingService;
import com.bslx.XueXiMod.shu.technique.release.payload.ObstacleBodies;
import com.bslx.XueXiMod.shu.technique.query.LivingCandidateQueries;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.TrajectorySampler;
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
import java.util.function.Consumer;

/**
 * 命中实体。生成规格在 {@link #applySpec} 装配；每 tick 先运动，再按命中盒或最早扫掠交给结算。
 * 去重、共享账本和无敌帧选择都在 {@link TechniqueHitSettle}。本类不保存世界，客户端移动只交给显示跟踪。
 */
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
    /** 显式垂落的固定锚点。原生竖落保持空。 */
    @Nullable
    private Vec3 dropLanding;
    private boolean centered;
    private boolean pose3d;
    private boolean explicitObb;
    private boolean beam;
    private boolean pinned;
    private boolean trap;
    boolean burstOnImpact;
    /** 实体结算后还要沿当前运动飞到地形。默认关闭。 */
    private boolean retainFlightAfterEntity;
    /** 命中实体后仍保留本体。默认消耗，只接受生成规格的显式例外。 */
    private boolean retainAfterEntityHit;
    /** 已经完成一次实体结算，旧攻击寿命不再清掉飞行体。 */
    private boolean flightHeld;
    /** 原运动已经停在寿命末端，改由命中当时的步长和已观察到的下落继续。 */
    private boolean carriedFlight;
    @Nullable
    private Vec3 carriedStep;
    private double carriedDrop;
    @Nullable
    /** 移除时通知一次。调用后立即清空，避免重复移除再次触发。 */
    private Consumer<TechniqueHitEntity> removalHook;
    private boolean planted;
    /** 盒子高度就是当前水体，不再保底成一格。 */
    private boolean exactVolume;
    /** 跟随走廊会被实心方块截断。 */
    private boolean clipsSolids;
    /** 新链才打开。旧命中仍绕过无敌帧。 */
    boolean respectLanding;
    boolean earliestSweep;
    int spreadCount = 1;
    double childRadius;
    @Nullable
    HitClock hitClock;
    @Nullable
    BatchFollow batchFollow;
    @Nullable
    OcclusionSnapshot occlusion;
    @Nullable
    ImpactPartition partition;
    boolean resourceCommit;
    boolean childInstance;
    boolean rootHeld;
    int branchOrdinal;
    int bodyScaledBase;
    int legacyFixedPoints;
    @Nullable
    UUID rootCastId;
    /** 这一次接触指定的目标。空表示仍只按体积扫描。 */
    @Nullable
    private UUID contactTargetId;
    ResourceEnchantLoad resourceLoad = ResourceEnchantLoad.EMPTY;
    final Set<UUID> rejectedTargets = new HashSet<>();
    /** 松开、换掉要求的剑或施法者倒下时结束。 */
    private boolean endsWhenReleased;
    /** 持续按键所在的技槽。 */
    private int releaseSlot = -1;
    /** 自动发射快照。关域不清除，也不当成松手。 */
    @Nullable
    private LaunchAim launchAim;
    private boolean automaticLifetime;
    /** 宿主组尚未整组成功时不结算伤害。旧路径保持关闭。 */
    private boolean pendingActivation;
    @Nullable
    /** 弧带冻结后的几何。有它时不再走普通运动和命中盒。 */
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
    /** 上一拍运动前的位置，供扫掠和体积判定使用。 */
    private Vec3 previousPosition = Vec3.ZERO;
    @Nullable
    private Vec3 anchor;
    @Nullable
    private Vec3 aim;
    final Set<UUID> hitOnce = new HashSet<>();
    final Map<UUID, Long> lastHitTick = new HashMap<>();
    /** 非 NaN 才进入通用伤害结算。 */
    float settleDamage = Float.NaN;
    EnchantmentResolution settleEnchantments;
    ReleaseBatch.Tally settleTally;
    int settleCopyIndex;
    int settleBurnSeconds;
    int settleSeedPoints;
    TideMarkSuccess settleTide = TideMarkSuccess.NONE;
    UUID vineActionId;
    boolean vineSpectrumEligible;
    boolean vineArmed;

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
    /** 有弧带时实体盒改用整段弧的世界范围。 */
    protected AABB makeBoundingBox() {
        AABB arcBox = this.arcWorldBox();
        return arcBox != null ? arcBox : super.makeBoundingBox();
    }

    @Override
    /** 弧快照和施法者标识从同步数据回填。不在这里重跑运动。 */
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
    /** 只恢复寿命。位置、运动和账本都不从存档恢复。 */
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains(LIFETIME_KEY, Tag.TAG_INT)) {
            this.storeLifetime(tag.getInt(LIFETIME_KEY));
        }
    }

    @Override
    /** 存档只留寿命。实体注册本身仍不保存。 */
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt(LIFETIME_KEY, this.lifetime());
    }

    @Override
    /** 玩家不能点选命中体。 */
    public boolean isPickable() {
        return false;
    }

    @Override
    /** 命中体不接受推动。 */
    public boolean isPushable() {
        return false;
    }

    @Override
    /** 只有登记成障碍体的命中体能挡住其他实体。 */
    public boolean canBeCollidedWith() {
        return ObstacleBodies.blocks(this);
    }

    @Override
    /** 命中体不主动与其他实体碰撞。 */
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
    /** 服务端每拍的总入口。暂挂时不动；弧带走自己的扫掠，其余先运动再结算。 */
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
            TechniqueHitSettle.expireLifetime(this, level);
            return;
        }
        this.age++;
        if (this.motion != null || this.carriedFlight) {
            long motionStart = PerformanceRecorder.begin();
            TrackingSupport.bindAutomaticAim(this.launchAim);
            try {
                Vec3 moveStart = this.previousPosition;
                if (this.carriedFlight) {
                    this.stepCarriedFlight(moveStart);
                } else {
                    this.motion.tick(this, level);
                    if (this.retainFlightAfterEntity) {
                        this.noteFlightStep(moveStart);
                        if (this.flightHeld) {
                            this.takeOverFrozenFlight(moveStart);
                        }
                    }
                }
            } finally {
                TrackingSupport.clearAutomaticAim();
                PerformanceRecorder.end(PerfMetric.MOTION, motionStart);
            }
        }
        this.refreshWeepingDeflection();
        if (!this.partingNotified && this.age >= this.hitReadyAge) {
            this.partingNotified = true;
            WaterPartingService.activated(this);
        }
        if (this.endsWhenReleased && this.releaseStopped()) {
            TechniqueHitSettle.expireLifetime(this, level);
            return;
        }
        if (this.centered) {
            this.previousPosition = this.position();
        }
        if (this.trap) {
            if (this.planted && !this.detonated) {
                AABB box = this.getBoundingBox().inflate(0.3);
                List<LivingEntity> stepped = LivingCandidateQueries.entityQuery(level, box,
                        entity -> TechniqueHitSettle.canHit(this, entity));
                if (!stepped.isEmpty()) {
                    TechniqueHitSettle.detonate(this, level);
                }
            }
        } else if (this.earliestSweep && this.age >= this.hitReadyAge) {
            if (TechniqueHitSettle.sweepEarliest(this, level)) {
                return;
            }
            if (this.retainFlightAfterEntity) {
                BatchFlightBook.holdAfterEntitySweep(this);
            }
        } else if (this.hitbox != null && this.age >= this.hitReadyAge) {
            AABB broad = this.hitbox.broadPhase(this);
            AABB curtainBroad = WeepingCurtain.queryBounds(this);
            if (curtainBroad != null) {
                broad = broad.minmax(curtainBroad);
            }
            List<LivingEntity> targets = LivingCandidateQueries.entityQuery(level, broad,
                    entity -> TechniqueHitSettle.canHit(this, entity));
            for (LivingEntity target : targets) {
                long narrowStart = PerformanceRecorder.begin();
                boolean narrowPassed;
                try {
                    narrowPassed = this.hitbox.contains(this, target) || WeepingCurtain.covers(this, target);
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
                    if (this.isRemoved()) {
                        return;
                    }
                }
            }
            this.strikeNamedContact(level);
            if (this.isRemoved()) {
                return;
            }
        }
        boolean clipTerrain = this.burstOnImpact || (this.retainFlightAfterEntity && this.flightHeld);
        if (clipTerrain && !this.isRemoved() && this.previousPosition != null) {
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
                TechniqueHitSettle.expireTerrain(this, level, blockHit.getLocation());
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
        if (lifeEnded && !(this.retainFlightAfterEntity && this.flightHeld)) {
            TechniqueHitSettle.expireLifetime(this, level);
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

    /** 宿主成组生成时先暂停，避免未整组提交就运动或命中。 */
    public void holdActivation() {
        this.pendingActivation = true;
    }

    /** 解除暂挂。弧带补上出生时刻，静止贴图也在这里记出生。 */
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

    /** 本地快照或同步串任一存在都算弧带。 */
    public boolean hasArcBand() {
        if (this.arcSnapshot != null) {
            return true;
        }
        String packed = this.entityData.get(DATA_ARC_SNAPSHOT);
        return packed != null && !packed.isEmpty();
    }

    @Nullable
    /** 本地没有快照时从同步串拆一次并留下。 */
    public ArcBandSnapshot arcBand() {
        if (this.arcSnapshot == null) {
            this.arcSnapshot = ArcBandSnapshot.unpack(this.entityData.get(DATA_ARC_SNAPSHOT));
        }
        return this.arcSnapshot;
    }

    /** 旧入口。寿命直接用规格上的模板寿命。 */
    public void applySpec(ServerPlayer caster, TechniqueHitSpec spec) {
        this.applySpec(caster, spec, spec.lifetime);
    }

    /**
     * 把这一份规格装到实体上，并按路径、陷阱、链覆盖和追踪选择运动。
     * 静止瞄准、涡旋和函数会先走一拍运动；这里不结算，也不改无敌帧或账本。
     */
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
        this.launchAim = spec.launchAim;
        this.automaticLifetime = spec.automaticLifetime;
        if (this.launchAim != null && this.launchAim.automaticLifetime()
                && (spec.tracking || spec.dropThisCast)) {
            this.lockThisCast = false;
        }
        this.dropThisCast = spec.dropThisCast;
        this.dropLanding = spec.dropLanding;
        this.centered = spec.centered;
        this.pose3d = spec.pose3d;
        this.explicitObb = spec.explicitObb;
        if (this.explicitObb) {
            this.exactVolume = true;
        }
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
        this.contactTargetId = spec.localTargetId;
        this.settleBurnSeconds = spec.settleBurnSeconds;
        this.settleSeedPoints = spec.settleSeedPoints;
        this.settleControlTicks = spec.settleControlTicks;
        this.settleExtinguish = spec.settleExtinguish;
        this.settleKnockback = spec.settleKnockback;
        this.settleTide = spec.settleTide == null ? TideMarkSuccess.NONE : spec.settleTide;
        this.vineArmed = spec.vineArmed;
        this.vineActionId = spec.vineActionId;
        this.vineSpectrumEligible = spec.vineSpectrumEligible;
        this.trap = spec.trap;
        this.burstOnImpact = spec.burstOnImpact;
        this.retainFlightAfterEntity = spec.retainFlightAfterEntity;
        this.retainAfterEntityHit = spec.retainAfterEntityHit;
        this.clipsSolids = spec.clipsSolids;
        this.endsWhenReleased = spec.endsWhenReleased;
        this.releaseSlot = spec.releaseSlot;
        if (spec.clipsSolids) {
            this.exactVolume = true;
        }
        if (spec.blocksMovement) ObstacleBodies.track(this, spec.length, spec.width, spec.height);
        this.anchor = spec.anchor;
        this.aim = spec.aim;
        if (this.automaticLifetime && this.launchAim != null) {
            if (this.anchor == null) {
                this.anchor = this.launchAim.origin();
            }
            if (this.aim == null || this.aim.lengthSqr() <= 1.0E-8) {
                this.aim = this.launchAim.direction();
            }
        }
        this.length = spec.length;
        this.width = spec.width;
        this.height = spec.height;
        this.functionId = spec.functionId == null ? "" : spec.functionId;
        if (spec.frozenArc != null) {
            this.installArc(spec.frozenArc);
            return;
        }
        if (spec.start != null) {
            this.origin = spec.start;
        } else if (this.launchAim != null) {
            this.origin = this.launchAim.origin();
        } else {
            this.origin = caster.getEyePosition();
        }
        Vec3 facing = spec.aim;
        if (facing == null || facing.lengthSqr() <= 1.0E-8) {
            if (this.launchAim != null) {
                facing = this.launchAim.direction();
            } else if (this.automaticLifetime) {
                facing = new Vec3(0.0, 0.0, 1.0);
            } else {
                facing = caster.getLookAngle();
            }
        }
        this.forward = facing.normalize();
        TechniqueHitSpec.Path path = spec.path;
        if (path == null) {
            path = switch (spec.motion) {
                case DROP -> TechniqueHitSpec.Path.DROP;
                case FOLLOW -> TechniqueHitSpec.Path.FOLLOW;
                case SWING -> TechniqueHitSpec.Path.STILL_AIM;
                case PROJECTILE -> TechniqueHitSpec.Path.STRAIGHT;
            };
        }
        if (spec.parallelThisCast) {
            path = TechniqueHitSpec.Path.STRAIGHT;
            this.dropThisCast = false;
            this.dropLanding = null;
            Vec3 parallelHeading = spec.parallelHeading != null && spec.parallelHeading.lengthSqr() > 1.0E-8
                    ? spec.parallelHeading : this.forward;
            double horizontalLength = Math.sqrt(parallelHeading.x * parallelHeading.x + parallelHeading.z * parallelHeading.z);
            this.forward = horizontalLength < 1.0E-4
                    ? new Vec3(0.0, 0.0, 1.0)
                    : new Vec3(parallelHeading.x / horizontalLength, 0.0, parallelHeading.z / horizontalLength);
            this.aim = this.forward;
        } else if (spec.dropThisCast && path != TechniqueHitSpec.Path.VORTEX) {
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
        this.respectLanding = spec.respectLanding;
        this.earliestSweep = spec.earliestSweep;
        this.spreadCount = Math.max(1, spec.spreadCount);
        this.childRadius = spec.childRadius;
        this.hitClock = spec.hitClock;
        this.batchFollow = spec.batchFollow;
        this.occlusion = spec.occlusion;
        this.partition = spec.partition;
        this.resourceCommit = spec.resourceCommit;
        this.childInstance = spec.childInstance;
        this.branchOrdinal = spec.branchOrdinal;
        this.bodyScaledBase = spec.bodyScaledBase;
        this.legacyFixedPoints = spec.legacyFixedPoints;
        this.resourceLoad = spec.resourceLoad == null ? ResourceEnchantLoad.EMPTY : spec.resourceLoad;
        this.rootCastId = spec.rootCastId;
        if (spec.resourceCommit && spec.rootCastId != null) {
            RootCastBindings.retain(spec.rootCastId);
            this.rootHeld = true;
        }
        this.motion = spec.trap ? new TrapFallMotion() : TechniqueHitKinds.motion(path);
        if (boundDefinition != null) {
            MotionProfile profile = boundDefinition.motionProfile().resolve(boundDefinition.trajectory());
            boolean pathMutable = boundDefinition.trajectoryMutable();
            Trajectory registeredTrajectory = boundDefinition.trajectory();
            boolean chainDrop = pathMutable && path == TechniqueHitSpec.Path.DROP && registeredTrajectory != Trajectory.DROP;
            boolean chainVortex = pathMutable && path == TechniqueHitSpec.Path.VORTEX && registeredTrajectory != Trajectory.VORTEX;
            boolean chainUpwell = spec.upwellThisCast && pathMutable;
            boolean chainParallel = spec.parallelThisCast && pathMutable;
            if (chainParallel) {
                this.motion = new ProjectileMotion();
            } else if (chainDrop) {
                this.motion = new DropMotion();
                this.earliestSweep = overrideKeepsSweep(boundDefinition, this.earliestSweep);
            } else if (chainVortex) {
                this.motion = new VortexMotion();
                this.earliestSweep = overrideKeepsSweep(boundDefinition, this.earliestSweep);
            } else if (chainUpwell || registeredTrajectory == Trajectory.UPWELL) {
                this.motion = new UpwellMotion(profile, this.spreadCount, spec.settleCopyIndex);
                this.earliestSweep = true;
            } else if (registeredTrajectory == Trajectory.PARABOLA) {
                this.motion = new ParabolaMotion(profile);
                this.earliestSweep = true;
            } else if (boundDefinition.prefersEarliestContact()) {
                this.earliestSweep = true;
            }
        }
        if (!spec.parallelThisCast && spec.tracking && !spec.trap && (path == TechniqueHitSpec.Path.STRAIGHT
                || path == TechniqueHitSpec.Path.DROP || path == TechniqueHitSpec.Path.VORTEX)) {
            this.motion = new TrackingMotion(path);
        }
        if (path == TechniqueHitSpec.Path.FUNCTION && WeepingCurtain.FUNCTION_ID.equals(this.functionId)) {
            this.motion = new WeepingLipMotion(spec.tracking);
        }
        this.hitbox = spec.path == null && spec.texture == null ? TechniqueHitKinds.hitbox(spec.hitbox) : new SizedBoxHitbox();
        if ((path == TechniqueHitSpec.Path.STILL_AIM
                || path == TechniqueHitSpec.Path.VORTEX
                || path == TechniqueHitSpec.Path.FUNCTION)
                && this.motion != null && caster.level() instanceof ServerLevel serverLevel) {
            long motionStart = PerformanceRecorder.begin();
            TrackingSupport.bindAutomaticAim(this.launchAim);
            try {
                this.motion.tick(this, serverLevel);
            } finally {
                TrackingSupport.clearAutomaticAim();
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

    /**
     * 宿主自己要求最早接触时，覆盖后的扫掠保留。
     * 扫掠只因默认抛物线或喷涌打开时，改随最终运动关掉。
     */
    private static boolean overrideKeepsSweep(ReleaseDefinition definition, boolean requestedSweep) {
        if (definition.prefersEarliestContact()) {
            return true;
        }
        Trajectory registeredTrajectory = definition.trajectory();
        if (registeredTrajectory == Trajectory.PARABOLA || registeredTrajectory == Trajectory.UPWELL) {
            return false;
        }
        return requestedSweep;
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
    /** 客户端和施法者离线时为空。结算遇到空施法者不会打出伤害。 */
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

    @Nullable
    public Vec3 dropLanding() {
        return this.dropLanding;
    }

    public boolean centered() {
        return this.centered;
    }

    public boolean pose3d() {
        return this.pose3d;
    }

    public boolean explicitObb() {
        return this.explicitObb;
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

    /**
     * 生成当刻的单位发射方向。只读当前已决定的出生方向，不推进运动、碰撞或伤害。
     */
    public Vec3 launchDirection() {
        Vec3 chosen = this.forward;
        if (this.dropLanding != null && this.origin != null) {
            chosen = DropTilt.unitToward(this.origin, this.dropLanding);
        } else if (this.lockThisCast && this.origin != null && this.level() instanceof ServerLevel serverLevel) {
            LivingEntity markedTarget = LockTargetService.getMarkedTarget(serverLevel, this.casterUuid);
            if (markedTarget != null) {
                Vec3 towardTarget = markedTarget.getBoundingBox().getCenter().subtract(this.origin);
                if (towardTarget.lengthSqr() > 1.0E-8D) {
                    chosen = towardTarget;
                }
            }
        } else if (this.motion instanceof UpwellMotion) {
            ReleaseDefinition definition = ReleaseRegistry.get(this.entityData.get(DATA_TECHNIQUE_ID));
            MotionProfile profile = definition == null
                    ? MotionProfile.approvedUpwell()
                    : definition.motionProfile().resolve(Trajectory.UPWELL);
            if (profile.kind() != MotionProfile.Kind.UPWELL) {
                profile = MotionProfile.approvedUpwell();
            }
            chosen = TrajectorySampler.upwellVelocity(
                    this.forward, 1.0D, profile.coneDegrees(), this.settleCopyIndex, this.spreadCount);
        }
        return finiteUnit(chosen);
    }

    private static Vec3 finiteUnit(Vec3 direction) {
        if (direction == null || !Double.isFinite(direction.x) || !Double.isFinite(direction.y)
                || !Double.isFinite(direction.z) || direction.lengthSqr() <= 1.0E-8D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 unitDirection = direction.normalize();
        if (!Double.isFinite(unitDirection.x) || !Double.isFinite(unitDirection.y) || !Double.isFinite(unitDirection.z)) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return unitDirection;
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
        return this.entityData.get(DATA_CASTER).orElse(this.casterUuid);
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
        return Math.max(0.0d, age);
    }

    public boolean deflectsProjectiles() {
        return this.deflectsIncoming;
    }

    @Override
    public void remove(RemovalReason reason) {
        this.releaseWeepingDeflection();
        this.closeArcDeflection();
        if (this.rootHeld) {
            this.rootHeld = false;
            RootCastBindings.release(this.rootCastId);
        }
        Consumer<TechniqueHitEntity> hook = this.removalHook;
        this.removalHook = null;
        if (hook != null) {
            hook.accept(this);
        }
        super.remove(reason);
    }

    public boolean remembersStrike(UUID targetId) {
        return targetId != null && this.hitOnce.contains(targetId);
    }

    /** 只标记飞行已保留。真正改由最后一步接管发生在后续运动里。 */
    public void holdFlight() {
        this.flightHeld = true;
    }

    /**
     * 持续、陷阱、静止斩击、明确穿透和明确爆裂都不在这里消耗。
     * 一次性飞行弹丸没有这些声明时，命中后消失。
     */
    boolean consumesOnEntityHit() {
        if (this.retainAfterEntityHit || this.retainFlightAfterEntity || this.burstOnImpact || this.trap) {
            return false;
        }
        if (this.persistentFlow || this.hitInterval > 0 || this.endsWhenReleased || this.automaticLifetime) {
            return false;
        }
        if (this.arcSnapshot != null || this.beam || this.pinned || this.centered) {
            return false;
        }
        return this.motion != null;
    }

    /** 替换移除通知。不追加多个监听。 */
    public void bindRemoval(Consumer<TechniqueHitEntity> hook) {
        this.removalHook = hook;
    }

    /**
     * 记下原运动仍在前进时的实际步长。连续两步的竖直差就是当前重力，不重新读准星。
     */
    private void noteFlightStep(Vec3 moveStart) {
        if (moveStart == null) {
            return;
        }
        Vec3 step = this.position().subtract(moveStart);
        if (step.lengthSqr() < 1.0E-8D) {
            return;
        }
        if (this.carriedStep != null) {
            double observed = this.carriedStep.y - step.y;
            if (observed > 1.0E-6D) {
                this.carriedDrop = observed;
            }
        }
        this.carriedStep = step;
    }

    /** 原运动停住后，用最后一步和已观察到的下落接着走，避免拉回弧线末端。 */
    private void takeOverFrozenFlight(Vec3 moveStart) {
        if (moveStart == null || this.carriedStep == null) {
            return;
        }
        Vec3 step = this.position().subtract(moveStart);
        if (step.lengthSqr() >= 1.0E-8D) {
            return;
        }
        this.carriedFlight = true;
        this.motion = null;
        this.stepCarriedFlight(moveStart);
    }

    private void stepCarriedFlight(Vec3 moveStart) {
        if (moveStart == null || this.carriedStep == null) {
            return;
        }
        Vec3 nextStep = new Vec3(this.carriedStep.x, this.carriedStep.y - this.carriedDrop, this.carriedStep.z);
        Vec3 nextPoint = moveStart.add(nextStep);
        Vec3 heading = nextStep.lengthSqr() < 1.0E-8D ? new Vec3(0.0D, -1.0D, 0.0D) : nextStep.normalize();
        this.placeAt(nextPoint, heading);
        this.carriedStep = nextStep;
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

    @Nullable
    public LaunchAim launchAim() {
        return this.launchAim;
    }

    public boolean automaticLifetime() {
        return this.automaticLifetime;
    }

    /** 接触目标走和范围扫描相同的窄判定与结算，不另造一次伤害。 */
    private void strikeNamedContact(ServerLevel level) {
        if (this.contactTargetId == null || this.hitbox == null || level == null) {
            return;
        }
        Entity found = level.getEntity(this.contactTargetId);
        if (!(found instanceof LivingEntity namedTarget) || !TechniqueHitSettle.canHit(this, namedTarget)) {
            return;
        }
        boolean namedInside = this.hitbox.contains(this, namedTarget) || WeepingCurtain.covers(this, namedTarget);
        if (namedInside) {
            TechniqueHitSettle.tryHit(this, level, namedTarget);
        }
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
        if (this.automaticLifetime) {
            return false;
        }
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

    /** 精确体积用实体高度。旧体积过薄时改用长度，否则至少 1 格。 */
    public double hitHeight() {
        if (this.exactVolume) {
            return Math.max(0.0, this.height());
        }
        if (this.height() <= 0.05) {
            return Math.max(this.length(), 1.0);
        }
        return Math.max(this.height(), 1.0);
    }

    /** 打开精确体积并同步三边。实体盒改成三边最大值的一半。 */
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

    /** 弧带停用普通运动和命中盒，寿命改用快照窗口。 */
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

    /**
     * 按弧的已过时间扫新增区段。区段里命中的目标交给结算。
     * 到寿命后移除；这里不直接改无敌帧或账本。
     */
    private void tickArcBand(ServerLevel level) {
        if (this.arcSnapshot.birthTick() < 0L) {
            return;
        }
        long ageTicks = Math.max(0L, level.getGameTime() - this.arcSnapshot.birthTick());
        ProgressArcBand.SweepCursor cursor = ProgressArcBand.advance(
                this.lastProcessedProgress, this.arcDamageClosed, ageTicks, this.arcSnapshot.activeTicks());
        if (cursor.swept()) {
            ArcBounds broad = ProgressArcBand.sliceBroadBounds(this.arcSnapshot, cursor.fromProgress(), cursor.toProgress());
            AABB searchBox = new AABB(broad.lowEast(), broad.lowAbove(), broad.lowSouth(),
                    broad.highEast(), broad.highAbove(), broad.highSouth());
            List<LivingEntity> targets = LivingCandidateQueries.entityQuery(level, searchBox,
                    entity -> TechniqueHitSettle.canHit(this, entity));
            boolean recordingNow = PerformanceRecorder.isRecording();
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
            TechniqueHitSettle.expireLifetime(this, level);
        }
    }

    /** 只在径向刀弧的有效时间内登记弹道偏转。其他状态立即撤下。 */
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

    /** 弧带关闭时撤下已登记的偏转宿主。 */
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
