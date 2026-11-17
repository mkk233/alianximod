package com.bslx.XueXiMod.shu.technique.sword;

import com.bslx.XueXiMod.debug.performance.PerfCounter;
import com.bslx.XueXiMod.debug.performance.PerfMetric;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.technique.effects.LockTargetService;
import com.bslx.XueXiMod.shu.technique.effects.PhantomSwordSurgeEffect;
import com.bslx.XueXiMod.shu.technique.effects.SwordArrayEffect;
import com.bslx.XueXiMod.shu.technique.effects.SwordFlightEffect;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.aim.LaunchAim;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.release.bind.PublicationAttempt;
import com.bslx.XueXiMod.shu.technique.release.entity.ClientTrack;
import com.bslx.XueXiMod.shu.technique.release.entity.ReleaseEntities;
import com.bslx.XueXiMod.shu.technique.release.payload.HitPayloads;
import com.bslx.XueXiMod.visual.facing.CameraFacingEffectId;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 实体剑。幻剑和玩家丢出的真剑共用，用状态区分停滞、飞行和插地。
 * 飞行、沿途命中和回收交给运动部分。不负责术式链结算。
 */
public class SwordEntity extends Entity {
    public static final double RETURN_STEP = 1.5D;
    public static final double RECOVER_REACH = 1.2D;
    public static final double REAL_FLIGHT = 1.2D;
    public static final double ORBIT_BASE = 4.0D;
    public static final int HOVER = 0;
    public static final int FLY = 1;
    public static final int PLANTED = 2;
    public static final int RETURN = 3;
    public static final int VORTEX = 4;
    public static final int ARRAY_HOLD = 5;

    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
            SynchedEntityData.defineId(SwordEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DATA_STATE =
            SynchedEntityData.defineId(SwordEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PHANTOM =
            SynchedEntityData.defineId(SwordEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(SwordEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_UPRIGHT_FACE =
            SynchedEntityData.defineId(SwordEntity.class, EntityDataSerializers.BOOLEAN);

    UUID owner = UUID.randomUUID();
    private int hoverTicks;
    int flyTicks;
    private int plantedTicks;
    private int arrayHoldTicks;
    private int arrayResumeState = -1;
    private int arrayResumeTicks;
    private int arrayResumeGeneration;
    private boolean arrayResumeSafeFall;
    Vec3 flyDirection = new Vec3(0, 0, 1);
    double flown;
    boolean realFalls;
    BlockPos plantedBlock = BlockPos.ZERO;
    private Vec3 returnSpot;
    private boolean recallIdle;
    private boolean occupied;
    int vortexSlot;
    int vortexCount = 1;
    int vortexAge;
    int vortexLaunchAt;
    double orbitAzimuth;
    double orbitElevation;
    double orbitRadius = 3.0;
    double orbitBobPhase;
    boolean homeToMark;
    boolean trackingEnabled;
    UUID trackingTarget;
    final Set<UUID> hit = new HashSet<>();
    int burnSeconds;
    int seedPoints;
    int controlTicks;
    boolean extinguish;
    float knockback;
    public java.util.UUID vineActionId;
    public boolean vineSpectrumEligible;
    float copiedDamage = 1f;
    int tideMarks;
    boolean surgeFlight;
    double surgeSpeed;
    float surgeDamage;
    private boolean lockAim;
    private LaunchAim launchAim;
    private Vec3 controlAnchor;
    private boolean hoverHeldDirection;
    /** 显式垂落已经写好飞向原落点的方向，悬停结束不得改回视线。 */
    private boolean keepDropHeading;
    private SwordFlightMount flightMount;
    private final ClientTrack clientTrack = new ClientTrack();

    public SwordEntity(EntityType<? extends SwordEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** 登记剑来和万剑归宗的回收步长、距离、飞行速度和环绕半径。 */
    public static void declareBalance() {
        SpecialBalance.publish(SpecialBalance.blocks(
                SpecialBalance.speed(SpecialBalance.begin("sword_come"), (float) RETURN_STEP, "剑来回收步长"),
                BalanceKeys.HIT_SIZE, RECOVER_REACH, "剑来回收距离"));
        SpecialBalance.publish(SpecialBalance.blocks(
                SpecialBalance.speed(SpecialBalance.begin("sword_return_all"), (float) REAL_FLIGHT, "万剑归宗飞行速度"),
                BalanceKeys.RADIUS, ORBIT_BASE, "万剑归宗环绕半径"));
    }

    /** 在给定位置生成悬停幻剑，并抄下这一帧的伤害、附魔和载荷。 */
    public static SwordEntity phantom(ServerLevel level, ServerPlayer player, Vec3 position, ItemStack copy, CastFrame frame, boolean lockAim) {
        SwordEntity sword = new SwordEntity(ReleaseEntities.SWORD.get(), level);
        sword.setPos(position.x, position.y, position.z);
        sword.setOwner(player.getUUID());
        sword.lockAim = lockAim;
        sword.entityData.set(DATA_PHANTOM, true);
        sword.entityData.set(DATA_ITEM, copy.copy());
        sword.entityData.set(DATA_STATE, HOVER);
        float weaponDamage = SwordEntityMotion.damageOf(copy);
        if (frame != null && frame.fieldDamageScale > 1.001f) {
            weaponDamage *= frame.fieldDamageScale;
        }
        sword.copiedDamage = SpecialBalance.contributed("phantom_sword", weaponDamage);
        sword.burnSeconds = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, copy) * 4;
        if (frame != null) {
            sword.trackingEnabled = frame.tracking;
            sword.burnSeconds = Math.max(sword.burnSeconds, frame.burnSeconds);
            sword.seedPoints = Math.max(0, frame.seedPoints);
            sword.controlTicks = frame.controlTicks;
            sword.extinguish = frame.extinguish;
            sword.knockback = frame.knockback;
            com.bslx.XueXiMod.status.wood.vine.VineMarkActions.Action vineAction =
                    com.bslx.XueXiMod.status.wood.vine.VineMarkActions.current();
            if (vineAction != null && vineAction.source()
                    == com.bslx.XueXiMod.status.wood.vine.VineMarkActions.Source.SWORD) {
                sword.vineActionId = vineAction.actionId();
                sword.vineSpectrumEligible = vineAction.spectrumEligible();
            }
            int marks = frame.tideSuccess == null ? 0 : frame.tideSuccess.marks();
            sword.tideMarks = marks;
            if (marks > 0) {
                sword.copiedDamage += SpecialBalance.contributed("tide_mark", marks * 2.0d);
            }
            HitPayloads.attach(sword, frame);
        }
        sword.setYRot(player.getYRot());
        sword.setXRot(player.getXRot());
        return sword;
    }

    /** 在玩家脚下准备承载用真剑。加入世界和乘客关系由承载会话同一线程完成。 */
    public static SwordEntity flightMount(ServerLevel level, ServerPlayer player, ItemStack stack) {
        SwordEntity sword = new SwordEntity(ReleaseEntities.SWORD.get(), level);
        sword.setPos(player.getX(), player.getY(), player.getZ());
        sword.setOwner(player.getUUID());
        sword.entityData.set(DATA_PHANTOM, false);
        sword.entityData.set(DATA_ITEM, stack.copy());
        sword.entityData.set(DATA_STATE, HOVER);
        sword.copiedDamage = 0.0F;
        sword.flyDirection = new Vec3(0.0D, 1.0D, 0.0D);
        sword.noPhysics = false;
        sword.setNoGravity(true);
        sword.setYRot(player.getYRot());
        sword.setXRot(0.0F);
        return sword;
    }

    public void attachFlightMount(SwordFlightMount mount) {
        this.flightMount = mount;
        if (mount != null) {
            this.noPhysics = false;
            this.setNoGravity(true);
            this.copiedDamage = 0.0F;
        }
    }

    public void detachFlightMount() {
        this.flightMount = null;
    }

    public SwordFlightMount flightMount() {
        return this.flightMount;
    }

    public boolean carriesFlight() {
        return this.flightMount != null;
    }

    /** 承载朝向只取玩家水平视线，玩家模型继续直立。 */
    public Vec3 flightLook(ServerPlayer rider) {
        Vec3 look = rider == null ? this.flyDirection : rider.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        if (flat.lengthSqr() < 1.0E-8D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return flat.normalize();
    }

    public void markRealFalling() {
        this.realFalls = true;
        this.setNoGravity(false);
    }

    /** 从眼睛生成正在飞行的真剑，方向跟当前视线。 */
    public static SwordEntity real(ServerLevel level, ServerPlayer player, ItemStack stack) {
        SwordEntity sword = new SwordEntity(ReleaseEntities.SWORD.get(), level);
        Vec3 eye = player.getEyePosition();
        sword.setPos(eye.x, eye.y, eye.z);
        sword.setOwner(player.getUUID());
        sword.entityData.set(DATA_PHANTOM, false);
        sword.entityData.set(DATA_ITEM, stack.copy());
        sword.entityData.set(DATA_STATE, FLY);
        sword.flyDirection = player.getLookAngle().normalize();
        sword.copiedDamage = SwordEntityMotion.damageOf(stack);
        sword.burnSeconds = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, stack) * 4;
        sword.setYRot(player.getYRot());
        sword.setXRot(player.getXRot());
        return sword;
    }

    public UUID ownerUuid() {
        return this.entityData.get(DATA_OWNER).orElse(this.owner);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_OWNER.equals(key)) {
            this.entityData.get(DATA_OWNER).ifPresent(ownerId -> this.owner = ownerId);
        }
    }

    private void setOwner(UUID ownerId) {
        this.owner = ownerId;
        this.entityData.set(DATA_OWNER, Optional.ofNullable(ownerId));
    }

    public boolean isPhantom() {
        return this.entityData.get(DATA_PHANTOM);
    }

    public boolean isReal() {
        return !isPhantom();
    }

    public boolean isPlanted() {
        return state() == PLANTED;
    }

    public boolean isReturning() {
        return state() == RETURN;
    }

    public boolean isVortex() {
        return state() == VORTEX;
    }

    /** 只有还没被占用的插地或悬停剑可以操纵。 */
    public boolean isManipulable() {
        return !this.occupied && (state() == PLANTED || state() == HOVER);
    }

    /** 幻剑纵横可操纵插地、悬停和列阵中的自有剑。 */
    public boolean surgeSelectable(Player player) {
        return player != null
                && player.getUUID().equals(ownerUuid())
                && isAlive()
                && !this.occupied
                && (state() == PLANTED || state() == HOVER || state() == VORTEX || state() == ARRAY_HOLD);
    }

    /** 插地剑先升高；已由列剑抬起的剑不再二次升高。随后进入只对本次有效的喷涌飞行。 */
    public void beginSurge(Vec3 heading) {
        boolean alreadyLifted = state() == ARRAY_HOLD;
        clearArrayHold();
        if (!alreadyLifted && state() == PLANTED) {
            this.setPos(this.getX(), this.getY() + PhantomSwordSurgeEffect.LIFT_BLOCKS, this.getZ());
        }
        Vec3 frozen = heading == null || heading.lengthSqr() < 1.0E-8D ? this.flyDirection : heading;
        if (frozen.lengthSqr() < 1.0E-8D) {
            frozen = new Vec3(0.0D, 0.0D, 1.0D);
        }
        this.flyDirection = frozen.normalize();
        this.hit.clear();
        this.flyTicks = 0;
        this.flown = 0.0D;
        this.hoverTicks = 0;
        this.plantedTicks = 0;
        this.plantedBlock = null;
        this.recallIdle = false;
        this.homeToMark = false;
        this.trackingEnabled = false;
        this.trackingTarget = null;
        this.returnSpot = null;
        this.realFalls = false;
        this.lockAim = false;
        this.launchAim = null;
        this.hoverHeldDirection = true;
        this.keepDropHeading = false;
        this.surgeFlight = true;
        this.surgeSpeed = PhantomSwordSurgeEffect.FLIGHT_SPEED;
        this.surgeDamage = PhantomSwordSurgeEffect.FLIGHT_DAMAGE;
        setUprightFace(false);
        setState(FLY);
        this.setDeltaMovement(Vec3.ZERO);
        SwordEntityMotion.aim(this, this.flyDirection);
    }

    /** 列剑只选择玩家已有的插地、悬停或飞行剑。召回、承载和被占用的剑不选。 */
    public boolean arraySelectable(Player player) {
        return player != null
                && player.getUUID().equals(ownerUuid())
                && isAlive()
                && !isVehicle()
                && !this.occupied
                && (state() == PLANTED || state() == HOVER || state() == FLY);
    }

    /** 非悬停剑原地上移后进入独立停驻。悬停剑不重复升高。 */
    public void beginArrayHold() {
        if (!arraySelectable(ownerPlayer())) {
            return;
        }
        int sourceState = state();
        int sourceTicks = sourceState == HOVER ? this.hoverTicks : this.flyTicks;
        boolean safeFall = sourceState == PLANTED;
        if (sourceState != HOVER) {
            this.setPos(this.getX(), this.getY() + SwordArrayEffect.LIFT_BLOCKS, this.getZ());
        }
        this.arrayHoldTicks = 0;
        this.arrayResumeState = sourceState;
        this.arrayResumeTicks = Math.max(0, sourceTicks);
        this.arrayResumeGeneration++;
        this.arrayResumeSafeFall = safeFall;
        boolean heldSurge = this.surgeFlight;
        double heldSurgeSpeed = this.surgeSpeed;
        float heldSurgeDamage = this.surgeDamage;
        setState(ARRAY_HOLD);
        this.surgeFlight = heldSurge;
        this.surgeSpeed = heldSurgeSpeed;
        this.surgeDamage = heldSurgeDamage;
        this.setDeltaMovement(Vec3.ZERO);
    }

    /** 当前维度里是否存在该玩家拥有的真剑或幻剑实体（任意状态）。 */
    public static boolean anyOwnedBy(ServerLevel level, UUID owner) {
        if (level == null || owner == null) {
            return false;
        }
        boolean recordingScan = PerformanceRecorder.isRecording();
        int scanned = 0;
        long scanStart = PerformanceRecorder.begin();
        try {
            for (Entity entity : level.getAllEntities()) {
                if (recordingScan) {
                    scanned++;
                }
                if (entity instanceof SwordEntity sword
                        && sword.isAlive()
                        && owner.equals(sword.ownerUuid())) {
                    return true;
                }
            }
            return false;
        } finally {
            PerformanceRecorder.end(PerfMetric.SWORD_OWNED_SCAN, scanStart);
            if (recordingScan) {
                PerformanceRecorder.add(PerfCounter.SWORD_OWNED_SCAN_CALLS, 1L);
                if (scanned > 0) {
                    PerformanceRecorder.add(PerfCounter.SWORD_OWNED_SCANNED, scanned);
                }
            }
        }
    }

    /** 收集这个主人还活着的全部剑，按实体编号排序。 */
    public static List<SwordEntity> ownedBy(ServerLevel level, UUID owner) {
        List<SwordEntity> swords = new ArrayList<>();
        if (level == null || owner == null) {
            return swords;
        }
        boolean recordingScan = PerformanceRecorder.isRecording();
        int scanned = 0;
        long scanStart = PerformanceRecorder.begin();
        try {
            for (Entity entity : level.getAllEntities()) {
                if (recordingScan) {
                    scanned++;
                }
                if (entity instanceof SwordEntity sword && sword.isAlive() && owner.equals(sword.ownerUuid())) {
                    swords.add(sword);
                }
            }
        } finally {
            PerformanceRecorder.end(PerfMetric.SWORD_OWNED_SCAN, scanStart);
            if (recordingScan) {
                PerformanceRecorder.add(PerfCounter.SWORD_OWNED_SCAN_CALLS, 1L);
                if (scanned > 0) {
                    PerformanceRecorder.add(PerfCounter.SWORD_OWNED_SCANNED, scanned);
                }
            }
        }
        swords.sort(Comparator.comparingInt(Entity::getId));
        return swords;
    }

    /** 召回：飞行中、停滞中、插地中都可以被召回。落点在飞行中按主人当前眼睛重算。 */
    public void startReturning(Vec3 eyeSpot) {
        clearArrayHold();
        this.hit.clear();
        this.homeToMark = false;
        this.recallIdle = false;
        setUprightFace(false);
        this.returnSpot = SwordEntityMotion.approachPoint(this, eyeSpot);
        setState(RETURN);
        this.realFalls = false;
        this.setDeltaMovement(Vec3.ZERO);
    }

    /** 发布许可通过后进入环绕。自动来源还要本轮令牌仍有效。 */
    public boolean beginVortex(int slot, int swordCount) {
        CastFrame frame = CastFrame.current();
        boolean automatic = LaunchAim.automatic(frame);
        PublicationAttempt attempt = PublicationAttempt.current();
        if (attempt != null && !attempt.allowsPublication()) {
            return false;
        }
        if (automatic && attempt == null) {
            return false;
        }
        if (automatic && (!(this.level() instanceof ServerLevel serverLevel)
                || !LaunchAim.spawnAllowed(frame, serverLevel))) {
            return false;
        }
        this.hit.clear();
        this.vortexSlot = slot;
        this.vortexCount = Math.max(1, swordCount);
        this.vortexAge = 0;
        this.realFalls = false;
        this.recallIdle = false;
        this.homeToMark = false;
        this.returnSpot = null;
        setUprightFace(true);
        SwordVortex.assignOrbit(this);
        SwordVortex.arm(this);
        setState(VORTEX);
        this.setDeltaMovement(Vec3.ZERO);
        if (attempt != null) {
            attempt.reportAccepted();
        }
        if (automatic) {
            Vec3 rawAnchor = frame.anchor != null ? frame.anchor : frame.execution.origin();
            Vec3 clampedAnchor = frame.clampAutomaticSource(rawAnchor);
            Vec3 facing = frame.look != null ? frame.look : frame.execution.baseDirection();
            this.adoptAutomaticAim(LaunchAim.capture(frame, clampedAnchor, facing), clampedAnchor);
        }
        return true;
    }

    /** 换上这轮冻结的瞄准和锚点。自动寿命时方向跟快照，不再锁玩家标记。 */
    public void adoptAutomaticAim(LaunchAim aim, Vec3 anchor) {
        this.launchAim = aim;
        this.controlAnchor = anchor;
        if (aim != null && aim.automaticLifetime()) {
            this.flyDirection = aim.direction();
            this.lockAim = false;
        }
    }

    LaunchAim launchAim() {
        return this.launchAim;
    }

    Vec3 controlAnchor() {
        return this.controlAnchor;
    }

    double nextOrbitRoll() {
        return this.random.nextDouble();
    }

    /** 环绕结束后再飞一次，并清掉这把剑已经打过的目标。 */
    void beginSecondFlight() {
        this.hit.clear();
        this.flyTicks = 0;
        this.flown = 0.0;
        this.plantedTicks = 0;
        this.realFalls = false;
        setUprightFace(false);
        setState(FLY);
        this.setDeltaMovement(Vec3.ZERO);
    }

    public ItemStack swordItem() {
        return this.entityData.get(DATA_ITEM);
    }

    public boolean uprightFace() {
        return this.entityData.get(DATA_UPRIGHT_FACE);
    }

    /** 万剑归宗漩涡的特殊朝向标识。飞行姿态不使用它。 */
    public CameraFacingEffectId uprightFacingEffect() {
        return CameraFacingEffectId.SWORD_RETURN_ALL_VORTEX;
    }

    void setUprightFace(boolean uprightFace) {
        this.entityData.set(DATA_UPRIGHT_FACE, uprightFace);
    }

    void spawnCarried(ItemStack stack) {
        this.spawnAtLocation(stack);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level().isClientSide() || hand != InteractionHand.MAIN_HAND
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (this.flightMount != null) {
            return this.flightMount.returnByUse(this, serverPlayer) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        if (!isReal() || !serverPlayer.getUUID().equals(ownerUuid()) || !isAlive()) {
            return InteractionResult.PASS;
        }
        double reach = SwordFlightEffect.RETURN_REACH;
        if (serverPlayer.distanceToSqr(this) > reach * reach) {
            return InteractionResult.PASS;
        }
        if (!SwordFlightReturn.tryReturn(serverPlayer, swordItem())) {
            return InteractionResult.PASS;
        }
        if (ItemStack.matches(serverPlayer.getMainHandItem(), swordItem())) {
            discard();
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isPickable() {
        return isReal();
    }

    @Override
    public boolean canBeCollidedWith() {
        return this.flightMount != null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.flightMount != null && this.getPassengers().isEmpty();
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.35D;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!this.hasPassenger(passenger)) {
            return;
        }
        double ridingY = this.getY() + this.getPassengersRidingOffset() + passenger.getMyRidingOffset();
        moveFunction.accept(passenger, this.getX(), ridingY, this.getZ());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        if (passenger == null || this.flightMount == null) {
            return super.getDismountLocationForPassenger(passenger);
        }
        return passenger.position();
    }

    int state() {
        return this.entityData.get(DATA_STATE);
    }

    void setState(int state) {
        if (state != FLY) {
            this.surgeFlight = false;
            this.surgeSpeed = 0.0D;
            this.surgeDamage = 0.0F;
        }
        if (state == RETURN || state == VORTEX || state == PLANTED) {
            this.trackingEnabled = false;
            this.trackingTarget = null;
        }
        this.entityData.set(DATA_STATE, state);
    }

    /** 撤销列剑停驻并作废旧恢复回调。不改剑的主人、物品和命中记录。 */
    private void clearArrayHold() {
        this.arrayHoldTicks = 0;
        this.arrayResumeState = -1;
        this.arrayResumeTicks = 0;
        this.arrayResumeGeneration++;
        this.arrayResumeSafeFall = false;
    }

    private Player ownerPlayer() {
        return this.level() instanceof ServerLevel serverLevel ? serverLevel.getPlayerByUUID(ownerUuid()) : null;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_ITEM, ItemStack.EMPTY);
        this.entityData.define(DATA_STATE, HOVER);
        this.entityData.define(DATA_PHANTOM, true);
        this.entityData.define(DATA_OWNER, Optional.empty());
        this.entityData.define(DATA_UPRIGHT_FACE, false);
    }

    /** 客户端先记下插值起点，避免非生物实体瞬移。 */
    @Override
    public void lerpTo(double coordX, double coordY, double coordZ, float yaw, float pitch, int steps, boolean teleport) {
        if (this.level().isClientSide()) {
            this.clientTrack.onClientMove(this, coordX, coordY, coordZ, yaw, pitch);
            return;
        }
        super.lerpTo(coordX, coordY, coordZ, yaw, pitch, steps, teleport);
    }

    /** 与移动插值同一条客户端路径：先记起点，再写目标位置。 */
    @Override
    public void moveTo(double coordX, double coordY, double coordZ, float yaw, float pitch) {
        if (this.level().isClientSide()) {
            this.clientTrack.onClientMove(this, coordX, coordY, coordZ, yaw, pitch);
            return;
        }
        super.moveTo(coordX, coordY, coordZ, yaw, pitch);
    }

    /** 客户端只做插值。服务端按停滞、飞行、召回、环绕和插地分派。 */
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.clientTrack.afterClientTick(this);
            return;
        }
        if (this.flightMount != null) {
            this.flightMount.tick(this, (ServerLevel) this.level());
            return;
        }
        boolean recordingNow = PerformanceRecorder.isRecording();
        long updateStart = PerformanceRecorder.begin();
        try {
        ServerLevel level = (ServerLevel) this.level();
        if (state() == HOVER) {
            this.hoverTicks++;
            if (this.keepDropHeading) {
                SwordEntityMotion.aim(this, this.flyDirection);
            } else if (this.launchAim != null && this.launchAim.automaticLifetime()) {
                this.aimFromLaunch(level);
            } else {
                Player ownerPlayer = level.getPlayerByUUID(this.owner);
                if (ownerPlayer != null) {
                    this.flyDirection = ownerPlayer.getLookAngle().normalize();
                    if (this.lockAim) {
                        LivingEntity target = LockTargetService.getMarkedTarget(level, this.owner);
                        if (target != null) {
                            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(this.position());
                            if (toTarget.lengthSqr() >= 1.0E-6) {
                                this.flyDirection = toTarget.normalize();
                            }
                        }
                    }
                    SwordEntityMotion.aim(this, this.flyDirection);
                }
            }
            if (this.hoverTicks >= 10) {
                setState(FLY);
            }
            return;
        }
        if (state() == ARRAY_HOLD) {
            holdArray();
            return;
        }
        if (state() == FLY) {
            fly(level);
            return;
        }
        if (state() == RETURN) {
            returnTick(level);
            return;
        }
        if (state() == VORTEX) {
            SwordVortex.tick(this, level);
            return;
        }
        this.plantedTicks++;
        if (this.recallIdle) {
            SwordEntityMotion.tryRecoverOwner(this, level);
            return;
        }
        if (isPhantom() && this.plantedTicks >= 400) {
            discard();
            return;
        }
        if (this.plantedBlock != null && (this.plantedTicks % 10) == 0
                && level.getBlockState(this.plantedBlock).isAir()) {
            if (isPhantom()) {
                discard();
            } else {
                SwordEntityMotion.dropRealItem(this, level);
            }
        }
        } finally {
            PerformanceRecorder.end(PerfMetric.SWORD_UPDATE, updateStart);
            if (recordingNow) {
                PerformanceRecorder.add(PerfCounter.SWORD_UPDATES, 1L);
            }
        }
    }

    /** 停驻期间暂停飞行命中、自动回收和悬停发射。到期后恢复原过程或安全下落。 */
    private void holdArray() {
        int generation = this.arrayResumeGeneration;
        this.arrayHoldTicks++;
        this.setDeltaMovement(Vec3.ZERO);
        if (this.arrayHoldTicks < SwordArrayEffect.HOLD_TICKS) {
            return;
        }
        resumeArrayHold(generation);
    }

    /** 只接受当前代次。剑来、纵横或其他状态切换留下的旧回调直接失效。 */
    private void resumeArrayHold(int generation) {
        if (generation != this.arrayResumeGeneration || state() != ARRAY_HOLD) {
            return;
        }
        int resumeState = this.arrayResumeState;
        int resumeTicks = this.arrayResumeTicks;
        boolean safeFall = this.arrayResumeSafeFall;
        clearArrayHold();
        if (safeFall || resumeState == PLANTED) {
            this.plantedBlock = null;
            this.plantedTicks = 0;
            this.flyTicks = 0;
            this.flown = 0.0D;
            this.realFalls = true;
            this.flyDirection = new Vec3(0.0D, -1.0D, 0.0D);
            this.setDeltaMovement(Vec3.ZERO);
            this.surgeFlight = false;
            this.surgeSpeed = 0.0D;
            this.surgeDamage = 0.0F;
            setState(FLY);
            SwordEntityMotion.aim(this, this.flyDirection);
            return;
        }
        if (resumeState == HOVER) {
            this.hoverTicks = Math.max(0, resumeTicks);
            setState(HOVER);
            return;
        }
        this.flyTicks = Math.max(0, resumeTicks);
        setState(FLY);
    }

    private void fly(ServerLevel level) {
        SwordEntityMotion.flyStep(this, level);
    }

    /** 旧垂落入口。方向在生成时写死，悬停和起飞都不再改成视线或锁定目标。 */
    public void keepDropHeading(Vec3 heading) {
        holdLaunchHeading(heading);
    }

    /** 生成时写死飞行方向。悬停期间保持该朝向，不再跟随视线或锁定目标。 */
    public void holdLaunchHeading(Vec3 heading) {
        if (heading == null || heading.lengthSqr() < 1.0E-8) {
            return;
        }
        this.flyDirection = heading.normalize();
        this.keepDropHeading = true;
        this.hoverHeldDirection = true;
        SwordEntityMotion.aim(this, this.flyDirection);
    }

    /** 悬停只吃这一次的局部目标。起飞后 flyDirection 不再改，沿用原冻结。 */
    private void aimFromLaunch(ServerLevel level) {
        if (this.keepDropHeading) {
            SwordEntityMotion.aim(this, this.flyDirection);
            return;
        }
        LivingEntity target = this.launchAim.findTarget(level);
        if (target != null) {
            Vec3 towardTarget = target.getBoundingBox().getCenter().subtract(this.position());
            if (towardTarget.lengthSqr() >= 1.0E-6) {
                this.flyDirection = towardTarget.normalize();
                this.launchAim.rememberDirection(this.flyDirection);
                this.hoverHeldDirection = true;
            }
        } else if (!this.hoverHeldDirection) {
            this.flyDirection = this.launchAim.direction();
        }
        SwordEntityMotion.aim(this, this.flyDirection);
    }

    private void returnTick(ServerLevel level) {
        long motionStart = PerformanceRecorder.begin();
        try {
        Player ownerPlayer = level.getPlayerByUUID(this.owner);
        boolean ownerHere = ownerPlayer instanceof ServerPlayer serverPlayer && serverPlayer.isAlive();
        if (ownerHere) {
            this.returnSpot = SwordEntityMotion.approachPoint(this, ownerPlayer.getEyePosition());
        }
        if (this.returnSpot == null) {
            setState(PLANTED);
            this.recallIdle = true;
            return;
        }
        if (SwordEntityMotion.tryRecoverOwner(this, level)) {
            return;
        }
        Vec3 from = this.position();
        Vec3 delta = this.returnSpot.subtract(from);
        double distance = delta.length();
        if (distance <= 0.4) {
            this.setPos(this.returnSpot.x, this.returnSpot.y, this.returnSpot.z);
            if (!ownerHere) {
                setState(PLANTED);
                this.plantedBlock = null;
                this.recallIdle = true;
                this.setDeltaMovement(Vec3.ZERO);
            }
            return;
        }
        double returnStep = SpecialBalance.number("sword_come", BalanceKeys.MOTION_SPEED, RETURN_STEP);
        Vec3 step = delta.scale(Math.min(returnStep, distance) / distance);
        Vec3 targetPoint = from.add(step);
        SwordEntityMotion.hurtAlong(this, level, from, targetPoint);
        this.setPos(targetPoint.x, targetPoint.y, targetPoint.z);
        SwordEntityMotion.aim(this, step);
        } finally {
            PerformanceRecorder.end(PerfMetric.MOTION, motionStart);
        }
    }

    /** 读回主人、状态、环绕、冻结瞄准和飞行数据。读档时不重算命中。 */
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.setOwner(tag.getUUID("Owner"));
        }
        this.entityData.set(DATA_PHANTOM, tag.getBoolean("Phantom"));
        this.entityData.set(DATA_STATE, tag.getInt("State"));
        this.hoverTicks = tag.getInt("Hover");
        this.flyTicks = tag.getInt("Fly");
        this.plantedTicks = tag.getInt("Planted");
        this.copiedDamage = tag.getFloat("Damage");
        this.burnSeconds = tag.getInt("Burn");
        this.seedPoints = tag.getInt("SeedPoints");
        this.controlTicks = tag.getInt("Control");
        this.extinguish = tag.getBoolean("Extinguish");
        this.knockback = tag.getFloat("Knock");
        this.vineSpectrumEligible = tag.getBoolean("VineSpectrum");
        this.vineActionId = tag.hasUUID("VineAction") ? tag.getUUID("VineAction") : null;
        this.tideMarks = tag.getInt("TideMarks");
        this.lockAim = tag.getBoolean("LockAim");
        this.vortexSlot = tag.getInt("VortexSlot");
        this.vortexCount = Math.max(1, tag.getInt("VortexCount"));
        this.vortexAge = tag.getInt("VortexAge");
        this.vortexLaunchAt = tag.getInt("VortexLaunch");
        this.orbitAzimuth = tag.getDouble("OrbitAzimuth");
        this.orbitElevation = tag.getDouble("OrbitElevation");
        this.orbitRadius = tag.contains("OrbitRadius") ? tag.getDouble("OrbitRadius") : 3.0;
        this.orbitBobPhase = tag.getDouble("OrbitBob");
        this.homeToMark = tag.getBoolean("HomeToMark");
        this.trackingEnabled = tag.getBoolean("TrackingEnabled")
                && (state() == HOVER || state() == FLY);
        this.trackingTarget = this.trackingEnabled && tag.hasUUID("TrackingTarget")
                ? tag.getUUID("TrackingTarget") : null;
        this.entityData.set(DATA_UPRIGHT_FACE, tag.getBoolean("UprightFace"));
        if (tag.contains("Item")) {
            this.entityData.set(DATA_ITEM, ItemStack.of(tag.getCompound("Item")));
        }
        if (tag.contains("Block")) {
            this.plantedBlock = BlockPos.of(tag.getLong("Block"));
        }
        if (tag.contains("ReturnX")) {
            this.returnSpot = new Vec3(tag.getDouble("ReturnX"), tag.getDouble("ReturnY"), tag.getDouble("ReturnZ"));
        }
        this.recallIdle = tag.getBoolean("RecallIdle");
        if (tag.contains("LaunchOriginX")) {
            Vec3 source = new Vec3(tag.getDouble("LaunchOriginX"), tag.getDouble("LaunchOriginY"), tag.getDouble("LaunchOriginZ"));
            Vec3 facing = new Vec3(tag.getDouble("LaunchFacingX"), tag.getDouble("LaunchFacingY"), tag.getDouble("LaunchFacingZ"));
            UUID localTarget = tag.hasUUID("LaunchTarget") ? tag.getUUID("LaunchTarget") : null;
            this.launchAim = LaunchAim.remembered(source, facing, localTarget,
                    tag.getBoolean("LaunchAutomatic"), tag.getLong("LaunchSeed"));
        }
        if (tag.contains("ControlAnchorX")) {
            this.controlAnchor = new Vec3(tag.getDouble("ControlAnchorX"), tag.getDouble("ControlAnchorY"), tag.getDouble("ControlAnchorZ"));
        }
        this.hoverHeldDirection = tag.getBoolean("HoverHeldDirection");
        this.keepDropHeading = tag.getBoolean("KeepDropHeading");
        this.surgeFlight = tag.getBoolean("SurgeFlight");
        this.arrayHoldTicks = tag.getInt("ArrayHold");
        this.arrayResumeState = tag.contains("ArrayResumeState") ? tag.getInt("ArrayResumeState") : -1;
        this.arrayResumeTicks = Math.max(0, tag.getInt("ArrayResumeTicks"));
        this.arrayResumeGeneration = Math.max(0, tag.getInt("ArrayResumeGeneration"));
        this.arrayResumeSafeFall = tag.getBoolean("ArrayResumeSafeFall");
        if (state() != ARRAY_HOLD) {
            clearArrayHold();
        } else if (this.arrayResumeState == HOVER || this.arrayResumeState == FLY) {
            this.trackingEnabled = tag.getBoolean("TrackingEnabled");
            this.trackingTarget = this.trackingEnabled && tag.hasUUID("TrackingTarget")
                    ? tag.getUUID("TrackingTarget") : null;
        }
        this.surgeSpeed = tag.contains("SurgeSpeed")
                ? tag.getDouble("SurgeSpeed")
                : PhantomSwordSurgeEffect.FLIGHT_SPEED;
        this.surgeDamage = tag.contains("SurgeDamage")
                ? tag.getFloat("SurgeDamage")
                : PhantomSwordSurgeEffect.FLIGHT_DAMAGE;
        boolean arrayKeepsSurge = state() == ARRAY_HOLD && this.arrayResumeState == FLY;
        if (!this.surgeFlight || (state() != FLY && !arrayKeepsSurge)) {
            this.surgeFlight = false;
            this.surgeSpeed = 0.0D;
            this.surgeDamage = 0.0F;
        }
        SwordEntityMotion.loadFlight(this, tag);
    }

    /** 写下当前状态和冻结瞄准。目标检查本身不进存档。 */
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putUUID("Owner", this.owner);
        tag.putBoolean("Phantom", isPhantom());
        tag.putInt("State", state());
        tag.putInt("Hover", this.hoverTicks);
        tag.putInt("Fly", this.flyTicks);
        tag.putInt("Planted", this.plantedTicks);
        tag.putFloat("Damage", this.copiedDamage);
        tag.putInt("Burn", this.burnSeconds);
        tag.putInt("SeedPoints", this.seedPoints);
        tag.putInt("Control", this.controlTicks);
        tag.putBoolean("Extinguish", this.extinguish);
        tag.putFloat("Knock", this.knockback);
        tag.putBoolean("VineSpectrum", this.vineSpectrumEligible);
        if (this.vineActionId != null) {
            tag.putUUID("VineAction", this.vineActionId);
        }
        tag.putInt("TideMarks", this.tideMarks);
        tag.putBoolean("LockAim", this.lockAim);
        tag.putInt("VortexSlot", this.vortexSlot);
        tag.putInt("VortexCount", this.vortexCount);
        tag.putInt("VortexAge", this.vortexAge);
        tag.putInt("VortexLaunch", this.vortexLaunchAt);
        tag.putDouble("OrbitAzimuth", this.orbitAzimuth);
        tag.putDouble("OrbitElevation", this.orbitElevation);
        tag.putDouble("OrbitRadius", this.orbitRadius);
        tag.putDouble("OrbitBob", this.orbitBobPhase);
        tag.putBoolean("HomeToMark", this.homeToMark);
        tag.putBoolean("TrackingEnabled", this.trackingEnabled);
        if (this.trackingTarget != null) {
            tag.putUUID("TrackingTarget", this.trackingTarget);
        }
        tag.putBoolean("UprightFace", uprightFace());
        tag.put("Item", swordItem().save(new CompoundTag()));
        if (this.plantedBlock != null) {
            tag.putLong("Block", this.plantedBlock.asLong());
        }
        if (this.returnSpot != null) {
            tag.putDouble("ReturnX", this.returnSpot.x);
            tag.putDouble("ReturnY", this.returnSpot.y);
            tag.putDouble("ReturnZ", this.returnSpot.z);
        }
        tag.putBoolean("RecallIdle", this.recallIdle);
        if (this.launchAim != null) {
            tag.putBoolean("LaunchAutomatic", this.launchAim.automaticLifetime());
            Vec3 source = this.launchAim.origin();
            tag.putDouble("LaunchOriginX", source.x);
            tag.putDouble("LaunchOriginY", source.y);
            tag.putDouble("LaunchOriginZ", source.z);
            Vec3 facing = this.launchAim.direction();
            tag.putDouble("LaunchFacingX", facing.x);
            tag.putDouble("LaunchFacingY", facing.y);
            tag.putDouble("LaunchFacingZ", facing.z);
            tag.putLong("LaunchSeed", this.launchAim.seed());
            if (this.launchAim.localTargetId() != null) {
                tag.putUUID("LaunchTarget", this.launchAim.localTargetId());
            }
        }
        if (this.controlAnchor != null) {
            tag.putDouble("ControlAnchorX", this.controlAnchor.x);
            tag.putDouble("ControlAnchorY", this.controlAnchor.y);
            tag.putDouble("ControlAnchorZ", this.controlAnchor.z);
        }
        tag.putBoolean("HoverHeldDirection", this.hoverHeldDirection);
        tag.putBoolean("KeepDropHeading", this.keepDropHeading);
        tag.putBoolean("SurgeFlight", this.surgeFlight);
        if (state() == ARRAY_HOLD) {
            tag.putInt("ArrayHold", this.arrayHoldTicks);
            tag.putInt("ArrayResumeState", this.arrayResumeState);
            tag.putInt("ArrayResumeTicks", this.arrayResumeTicks);
            tag.putInt("ArrayResumeGeneration", this.arrayResumeGeneration);
            tag.putBoolean("ArrayResumeSafeFall", this.arrayResumeSafeFall);
        }
        tag.putDouble("SurgeSpeed", this.surgeSpeed);
        tag.putFloat("SurgeDamage", this.surgeDamage);
        SwordEntityMotion.saveFlight(this, tag);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        HitPayloads.clear(this);
        super.remove(reason);
    }

    @Override
    public boolean shouldBeSaved() {
        return true;
    }
}
