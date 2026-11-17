package com.bslx.XueXiMod.shu.technique.sword;

import com.bslx.XueXiMod.debug.performance.PerfCounter;
import com.bslx.XueXiMod.debug.performance.PerfMetric;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.network.ModNetwork;
import com.bslx.XueXiMod.network.ShuSyncPacket;
import com.bslx.XueXiMod.player.PlayerShuData;
import com.bslx.XueXiMod.shu.technique.effects.LockTargetService;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.TechniqueHits;
import com.bslx.XueXiMod.shu.technique.release.aim.LaunchAim;
import com.bslx.XueXiMod.shu.technique.release.bind.PublicationAttempt;
import com.bslx.XueXiMod.shu.technique.release.entity.ReleaseEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 实体剑。幻剑和玩家丢出的真剑共用，用状态区分停滞、飞行和插地。
 */
public class SwordEntity extends Entity {
    public static final int HOVER = 0;
    public static final int FLY = 1;
    public static final int PLANTED = 2;
    public static final int RETURN = 3;
    public static final int VORTEX = 4;

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
    float copiedDamage = 1f;
    int tideMarks;
    private boolean lockAim;
    private LaunchAim launchAim;
    private Vec3 controlAnchor;
    private boolean hoverHeldDirection;
    private final com.bslx.XueXiMod.shu.technique.release.entity.ClientTrack clientTrack =
            new com.bslx.XueXiMod.shu.technique.release.entity.ClientTrack();

    public SwordEntity(EntityType<? extends SwordEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static SwordEntity phantom(ServerLevel level, ServerPlayer player, Vec3 position, ItemStack copy, CastFrame frame, boolean lockAim) {
        SwordEntity sword = new SwordEntity(ReleaseEntities.SWORD.get(), level);
        sword.setPos(position.x, position.y, position.z);
        sword.setOwner(player.getUUID());
        sword.lockAim = lockAim;
        sword.entityData.set(DATA_PHANTOM, true);
        sword.entityData.set(DATA_ITEM, copy.copy());
        sword.entityData.set(DATA_STATE, HOVER);
        sword.copiedDamage = SwordEntityMotion.damageOf(copy);
        sword.burnSeconds = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, copy) * 4;
        if (frame != null) {
            sword.trackingEnabled = frame.tracking;
            sword.burnSeconds = Math.max(sword.burnSeconds, frame.burnSeconds);
            sword.seedPoints = Math.max(0, frame.seedPoints);
            sword.controlTicks = frame.controlTicks;
            sword.extinguish = frame.extinguish;
            sword.knockback = frame.knockback;
            if (frame.fieldDamageScale > 1.001f) {
                sword.copiedDamage *= frame.fieldDamageScale;
            }
            int marks = frame.tideSuccess == null ? 0 : frame.tideSuccess.marks();
            sword.tideMarks = marks;
            sword.copiedDamage += marks * 2f;
            com.bslx.XueXiMod.shu.technique.release.payload.HitPayloads.attach(sword, frame);
        }
        sword.setYRot(player.getYRot());
        sword.setXRot(player.getXRot());
        return sword;
    }

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

    public boolean isManipulable() {
        return !this.occupied && (state() == PLANTED || state() == HOVER);
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
        this.hit.clear();
        this.homeToMark = false;
        this.recallIdle = false;
        setUprightFace(false);
        this.returnSpot = SwordEntityMotion.approachPoint(this, eyeSpot);
        setState(RETURN);
        this.realFalls = false;
        this.setDeltaMovement(Vec3.ZERO);
    }

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
        if (automatic) {
            if (!(this.level() instanceof ServerLevel)) {
                return false;
            }
            if (!LaunchAim.spawnAllowed(frame, (ServerLevel) this.level())) {
                return false;
            }
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

    void setUprightFace(boolean uprightFace) {
        this.entityData.set(DATA_UPRIGHT_FACE, uprightFace);
    }

    void spawnCarried(ItemStack stack) {
        this.spawnAtLocation(stack);
    }

    private int state() {
        return this.entityData.get(DATA_STATE);
    }

    void setState(int state) {
        if (state == RETURN || state == VORTEX || state == PLANTED) {
            this.trackingEnabled = false;
            this.trackingTarget = null;
        }
        this.entityData.set(DATA_STATE, state);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_ITEM, ItemStack.EMPTY);
        this.entityData.define(DATA_STATE, HOVER);
        this.entityData.define(DATA_PHANTOM, true);
        this.entityData.define(DATA_OWNER, Optional.empty());
        this.entityData.define(DATA_UPRIGHT_FACE, false);
    }

    @Override
    public void lerpTo(double coordX, double coordY, double coordZ, float yaw, float pitch, int steps, boolean teleport) {
        if (this.level().isClientSide()) {
            this.clientTrack.onClientMove(this, coordX, coordY, coordZ, yaw, pitch);
            return;
        }
        super.lerpTo(coordX, coordY, coordZ, yaw, pitch, steps, teleport);
    }

    @Override
    public void moveTo(double coordX, double coordY, double coordZ, float yaw, float pitch) {
        if (this.level().isClientSide()) {
            this.clientTrack.onClientMove(this, coordX, coordY, coordZ, yaw, pitch);
            return;
        }
        super.moveTo(coordX, coordY, coordZ, yaw, pitch);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.clientTrack.afterClientTick(this);
            return;
        }
        boolean recordingNow = PerformanceRecorder.isRecording();
        long updateStart = PerformanceRecorder.begin();
        try {
        ServerLevel level = (ServerLevel) this.level();
        if (state() == HOVER) {
            this.hoverTicks++;
            if (this.launchAim != null && this.launchAim.automaticLifetime()) {
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

    private void fly(ServerLevel level) {
        SwordEntityMotion.flyStep(this, level);
    }

    /** 悬停只吃这一次的局部目标。起飞后 flyDirection 不再改，沿用原冻结。 */
    private void aimFromLaunch(ServerLevel level) {
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
        Vec3 step = delta.scale(Math.min(1.5, distance) / distance);
        Vec3 targetPoint = from.add(step);
        SwordEntityMotion.hurtAlong(this, level, from, targetPoint);
        this.setPos(targetPoint.x, targetPoint.y, targetPoint.z);
        SwordEntityMotion.aim(this, step);
        } finally {
            PerformanceRecorder.end(PerfMetric.MOTION, motionStart);
        }
    }

    /** 每把剑沿着自己相对眼睛的水平方向回来，落在眼睛外侧 1 格，避免挤成同一条线。 */

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
        SwordEntityMotion.loadFlight(this, tag);
    }

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
        SwordEntityMotion.saveFlight(this, tag);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        com.bslx.XueXiMod.shu.technique.release.payload.HitPayloads.clear(this);
        super.remove(reason);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return true;
    }
}
