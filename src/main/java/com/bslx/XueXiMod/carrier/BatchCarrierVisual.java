package com.bslx.XueXiMod.carrier;

import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
import com.bslx.XueXiMod.shu.technique.release.batch.HitClock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 载体视觉和留焰跟踪。不造成陨岩中心爆炸，也不自己登记到总线。
 * 平色由 {@link #applyFlatTint(int, int, int)} 写入，默认白色，留给以后的幽冥纯色。
 */
public class BatchCarrierVisual extends Entity {
    private static final EntityDataAccessor<Integer> DATA_SHAPE =
            SynchedEntityData.defineId(BatchCarrierVisual.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TEXTURE =
            SynchedEntityData.defineId(BatchCarrierVisual.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DURATION =
            SynchedEntityData.defineId(BatchCarrierVisual.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_HALF_SPAN =
            SynchedEntityData.defineId(BatchCarrierVisual.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_TINT =
            SynchedEntityData.defineId(BatchCarrierVisual.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HOST =
            SynchedEntityData.defineId(BatchCarrierVisual.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_TANGENT_X =
            SynchedEntityData.defineId(BatchCarrierVisual.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TANGENT_Y =
            SynchedEntityData.defineId(BatchCarrierVisual.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TANGENT_Z =
            SynchedEntityData.defineId(BatchCarrierVisual.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_SAMPLE_ORDER =
            SynchedEntityData.defineId(BatchCarrierVisual.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SPARK_MESH_ANCHOR =
            SynchedEntityData.defineId(BatchCarrierVisual.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_SPARK_LAUNCH_X =
            SynchedEntityData.defineId(BatchCarrierVisual.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SPARK_LAUNCH_Y =
            SynchedEntityData.defineId(BatchCarrierVisual.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SPARK_LAUNCH_Z =
            SynchedEntityData.defineId(BatchCarrierVisual.class, EntityDataSerializers.FLOAT);

    private boolean releaseOnLoad;
    private boolean trackerArmed;
    /** 宿主还在时不因原寿命丢掉外观，宿主消失后马上移除。 */
    private boolean holdForHost;
    private boolean retainedRoot;
    private boolean hasLastPoint;
    private int orphanTicks;
    private int hostMissingTicks;
    private UUID boundHostId;
    private static final ConcurrentMap<UUID, UUID> HOST_VISUALS = new ConcurrentHashMap<>();
    private float trailDamage;
    private double lastPointX;
    private double lastPointY;
    private double lastPointZ;
    private double pendingDistance;
    private final List<Long> sampleBirths = new ArrayList<>();
    private UUID rootCastId;
    private UUID ownerIdentifier;
    private HitClock sharedClock;
    private TideMarkSuccess trailTide = TideMarkSuccess.NONE;
    private int nextTrailSampleOrder;

    public BatchCarrierVisual(EntityType<? extends BatchCarrierVisual> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.setInvulnerable(true);
        this.setNoGravity(true);
    }

    @Override
    protected AABB makeBoundingBox() {
        if (this.entityData != null && this.textureKind() == BatchVisualTexture.SURFACE) {
            return super.makeBoundingBox().expandTowards(0.0D, 0.5D, 0.0D);
        }
        return super.makeBoundingBox();
    }

    public static boolean spawn(ServerLevel level, Vec3 point, BatchVisualShape shape, BatchVisualTexture texture,
                                int durationTicks, float halfSpan, int hostId) {
        return spawn(level, point, shape, texture, durationTicks, halfSpan, hostId, false);
    }

    public static boolean spawn(ServerLevel level, Vec3 point, BatchVisualShape shape, BatchVisualTexture texture,
                                int durationTicks, float halfSpan, int hostId, boolean holdForHost) {
        return spawnBound(level, point, shape, texture, durationTicks, halfSpan, hostId, holdForHost, null);
    }

    public static boolean spawn(ServerLevel level, Vec3 point, BatchVisualShape shape, BatchVisualTexture texture,
                                int durationTicks, float halfSpan, int hostId, boolean holdForHost, UUID hostKey) {
        return spawnBound(level, point, shape, texture, durationTicks, halfSpan, hostId, holdForHost, hostKey);
    }

    private static boolean spawnBound(ServerLevel level, Vec3 point, BatchVisualShape shape, BatchVisualTexture texture,
                                       int durationTicks, float halfSpan, int hostId, boolean holdForHost, UUID hostKey) {
        if (level == null || point == null || shape == null || texture == null || !BatchCarrierTypes.VISUAL.isPresent()) {
            return false;
        }
        BlockPos cell = BlockPos.containing(point);
        if (!level.isLoaded(cell) || !level.isInWorldBounds(cell)) {
            return false;
        }
        BatchCarrierVisual visual = new BatchCarrierVisual(BatchCarrierTypes.VISUAL.get(), level);
        visual.entityData.set(DATA_SHAPE, shape.ordinal());
        visual.entityData.set(DATA_TEXTURE, texture.ordinal());
        visual.entityData.set(DATA_DURATION, Math.max(1, durationTicks));
        visual.entityData.set(DATA_HALF_SPAN, halfSpan > 0.0F ? halfSpan : 0.5F);
        visual.entityData.set(DATA_TINT, 0xFFFFFF);
        visual.entityData.set(DATA_HOST, hostId);
        visual.holdForHost = holdForHost;
        visual.boundHostId = hostKey;
        visual.setPos(point.x, point.y, point.z);
        boolean accepted = level.addFreshEntity(visual);
        if (accepted && hostKey != null && !level.isClientSide()) {
            HOST_VISUALS.put(hostKey, visual.getUUID());
        }
        return accepted;
    }

    /**
     * 留焰采样点。切向按真实弹道段归一化，模长过短时回退为世界 +X。
     */
    public static boolean spawnTrailSample(ServerLevel level, Vec3 point, Vec3 tangent, int sampleOrder,
                                           int durationTicks, float halfSpan, boolean sparkMeshAnchor,
                                           float sparkLaunchX, float sparkLaunchY, float sparkLaunchZ) {
        if (level == null || point == null || !BatchCarrierTypes.VISUAL.isPresent()) {
            return false;
        }
        BlockPos cell = BlockPos.containing(point);
        if (!level.isLoaded(cell) || !level.isInWorldBounds(cell)) {
            return false;
        }
        Vec3 unitTangent = unitTangent(tangent);
        BatchCarrierVisual visual = new BatchCarrierVisual(BatchCarrierTypes.VISUAL.get(), level);
        visual.entityData.set(DATA_SHAPE, BatchVisualShape.BILLBOARD.ordinal());
        visual.entityData.set(DATA_TEXTURE, BatchVisualTexture.TRAIL.ordinal());
        visual.entityData.set(DATA_DURATION, Math.max(1, durationTicks));
        visual.entityData.set(DATA_HALF_SPAN, halfSpan > 0.0F ? halfSpan : 0.5F);
        visual.entityData.set(DATA_TINT, 0xFFFFFF);
        visual.entityData.set(DATA_HOST, -1);
        visual.entityData.set(DATA_TANGENT_X, (float) unitTangent.x);
        visual.entityData.set(DATA_TANGENT_Y, (float) unitTangent.y);
        visual.entityData.set(DATA_TANGENT_Z, (float) unitTangent.z);
        visual.entityData.set(DATA_SAMPLE_ORDER, sampleOrder);
        visual.entityData.set(DATA_SPARK_MESH_ANCHOR, sparkMeshAnchor);
        visual.entityData.set(DATA_SPARK_LAUNCH_X, sparkLaunchX);
        visual.entityData.set(DATA_SPARK_LAUNCH_Y, sparkLaunchY);
        visual.entityData.set(DATA_SPARK_LAUNCH_Z, sparkLaunchZ);
        visual.setPos(point.x, point.y, point.z);
        return level.addFreshEntity(visual);
    }

    /**
     * 火苗只在生成时锁定一次发射方向。之后不跟相机，也不用位移猜滚转。
     */
    public static boolean spawnSpark(ServerLevel level, Vec3 point, Vec3 launchDirection,
                                     int durationTicks, float halfSpan, int hostId, UUID hostKey) {
        if (level == null || point == null || !BatchCarrierTypes.VISUAL.isPresent()) {
            return false;
        }
        BlockPos cell = BlockPos.containing(point);
        if (!level.isLoaded(cell) || !level.isInWorldBounds(cell)) {
            return false;
        }
        Vec3 unitLaunch = unitLaunchDirection(launchDirection);
        BatchCarrierVisual visual = new BatchCarrierVisual(BatchCarrierTypes.VISUAL.get(), level);
        visual.entityData.set(DATA_SHAPE, BatchVisualShape.BILLBOARD.ordinal());
        visual.entityData.set(DATA_TEXTURE, BatchVisualTexture.SPARK.ordinal());
        visual.entityData.set(DATA_DURATION, Math.max(1, durationTicks));
        visual.entityData.set(DATA_HALF_SPAN, halfSpan > 0.0F ? halfSpan : 0.5F);
        visual.entityData.set(DATA_TINT, 0xFFFFFF);
        visual.entityData.set(DATA_HOST, hostId);
        visual.entityData.set(DATA_TANGENT_X, (float) unitLaunch.x);
        visual.entityData.set(DATA_TANGENT_Y, (float) unitLaunch.y);
        visual.entityData.set(DATA_TANGENT_Z, (float) unitLaunch.z);
        visual.holdForHost = true;
        visual.boundHostId = hostKey;
        visual.setPos(point.x, point.y, point.z);
        boolean accepted = level.addFreshEntity(visual);
        if (accepted && hostKey != null && !level.isClientSide()) {
            HOST_VISUALS.put(hostKey, visual.getUUID());
        }
        return accepted;
    }

    /** 宿主移除时只收掉绑定到它的那一份视觉，不按相邻实体编号猜测。 */
    public static void discardForHost(ServerLevel level, UUID hostId) {
        if (level == null || hostId == null) {
            return;
        }
        UUID visualId = HOST_VISUALS.remove(hostId);
        if (visualId == null) {
            return;
        }
        Entity visual = level.getEntity(visualId);
        if (visual instanceof BatchCarrierVisual carrierVisual && !carrierVisual.isRemoved()) {
            carrierVisual.discard();
        }
    }

    public void armTracker(UUID rootIdentifier, UUID ownerId, HitClock hitClock, float damage, TideMarkSuccess tide,
                           boolean retainRoot) {
        this.trackerArmed = true;
        this.rootCastId = rootIdentifier;
        this.ownerIdentifier = ownerId;
        this.sharedClock = hitClock;
        this.trailDamage = damage;
        this.trailTide = tide == null ? TideMarkSuccess.NONE : tide;
        this.retainedRoot = retainRoot;
        this.entityData.set(DATA_SHAPE, BatchVisualShape.HIDDEN.ordinal());
        this.entityData.set(DATA_TEXTURE, BatchVisualTexture.NONE.ordinal());
    }

    public void assignHost(int hostId) {
        this.entityData.set(DATA_HOST, hostId);
    }

    /**
     * 幽冥纯色的写入点。当前没有调用方，默认保持白色。不要在这里引用尚未交付的领域类型。
     */
    public void applyFlatTint(int redChannel, int greenChannel, int blueChannel) {
        int packed = ((redChannel & 255) << 16) | ((greenChannel & 255) << 8) | (blueChannel & 255);
        this.entityData.set(DATA_TINT, packed);
    }

    public BatchVisualShape shape() {
        return BatchVisualShape.fromOrdinal(this.entityData.get(DATA_SHAPE));
    }

    public BatchVisualTexture textureKind() {
        return BatchVisualTexture.fromOrdinal(this.entityData.get(DATA_TEXTURE));
    }

    public int durationTicks() {
        return this.entityData.get(DATA_DURATION);
    }

    public float halfSpan() {
        return this.entityData.get(DATA_HALF_SPAN);
    }

    public int flatTintPacked() {
        return this.entityData.get(DATA_TINT);
    }

    public int hostId() {
        return this.entityData.get(DATA_HOST);
    }

    public float tangentX() {
        return this.entityData.get(DATA_TANGENT_X);
    }

    public float tangentY() {
        return this.entityData.get(DATA_TANGENT_Y);
    }

    public float tangentZ() {
        return this.entityData.get(DATA_TANGENT_Z);
    }

    public int sampleOrder() {
        return this.entityData.get(DATA_SAMPLE_ORDER);
    }

    public void markSparkMeshAnchor() {
        this.entityData.set(DATA_SPARK_MESH_ANCHOR, true);
    }

    public boolean sparkMeshAnchor() {
        return this.entityData.get(DATA_SPARK_MESH_ANCHOR);
    }

    public void assignSparkLaunch(Vec3 launch) {
        Vec3 unitLaunch = unitLaunchDirection(launch);
        this.entityData.set(DATA_SPARK_LAUNCH_X, (float) unitLaunch.x);
        this.entityData.set(DATA_SPARK_LAUNCH_Y, (float) unitLaunch.y);
        this.entityData.set(DATA_SPARK_LAUNCH_Z, (float) unitLaunch.z);
    }

    public float sparkLaunchX() {
        return this.entityData.get(DATA_SPARK_LAUNCH_X);
    }

    public float sparkLaunchY() {
        return this.entityData.get(DATA_SPARK_LAUNCH_Y);
    }

    public float sparkLaunchZ() {
        return this.entityData.get(DATA_SPARK_LAUNCH_Z);
    }

    /** 同一条留焰跟踪器上的采样序号，从 0 递增。 */
    public int nextSampleOrder() {
        int sampleOrder = this.nextTrailSampleOrder;
        if (this.nextTrailSampleOrder < Integer.MAX_VALUE) {
            this.nextTrailSampleOrder++;
        }
        return sampleOrder;
    }

    private static Vec3 unitTangent(Vec3 tangent) {
        if (tangent == null || tangent.lengthSqr() <= 1.0E-8D) {
            return new Vec3(1.0D, 0.0D, 0.0D);
        }
        return tangent.normalize();
    }

    private static Vec3 unitLaunchDirection(Vec3 launchDirection) {
        if (launchDirection == null || !Double.isFinite(launchDirection.x) || !Double.isFinite(launchDirection.y)
                || !Double.isFinite(launchDirection.z) || launchDirection.lengthSqr() <= 1.0E-8D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 unitDirection = launchDirection.normalize();
        if (!Double.isFinite(unitDirection.x) || !Double.isFinite(unitDirection.y) || !Double.isFinite(unitDirection.z)) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return unitDirection;
    }

    boolean trackerArmed() {
        return this.trackerArmed;
    }

    HitClock sharedClock() {
        return this.sharedClock;
    }

    float trailDamage() {
        return this.trailDamage;
    }

    TideMarkSuccess trailTide() {
        return this.trailTide;
    }

    UUID ownerIdentifier() {
        return this.ownerIdentifier;
    }

    int activeSamples(long gameTime) {
        this.sampleBirths.removeIf(birthTick -> gameTime - birthTick >= com.bslx.XueXiMod.shu.technique.enchant.resource.TrailContract.LIFE_TICKS);
        return this.sampleBirths.size();
    }

    void noteSample(long gameTime) {
        this.sampleBirths.add(gameTime);
    }

    boolean hasLastPoint() {
        return this.hasLastPoint;
    }

    Vec3 lastPoint() {
        return new Vec3(this.lastPointX, this.lastPointY, this.lastPointZ);
    }

    void moveLastPoint(Vec3 point) {
        this.lastPointX = point.x;
        this.lastPointY = point.y;
        this.lastPointZ = point.z;
        this.hasLastPoint = true;
    }

    double pendingDistance() {
        return this.pendingDistance;
    }

    void setPendingDistance(double distance) {
        this.pendingDistance = distance;
    }

    void clearLastPoint() {
        this.hasLastPoint = false;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_SHAPE, BatchVisualShape.HIDDEN.ordinal());
        this.entityData.define(DATA_TEXTURE, BatchVisualTexture.NONE.ordinal());
        this.entityData.define(DATA_DURATION, BatchCarrierIds.STRIP_FRAMES);
        this.entityData.define(DATA_HALF_SPAN, 0.5F);
        this.entityData.define(DATA_TINT, 0xFFFFFF);
        this.entityData.define(DATA_HOST, -1);
        this.entityData.define(DATA_TANGENT_X, 1.0F);
        this.entityData.define(DATA_TANGENT_Y, 0.0F);
        this.entityData.define(DATA_TANGENT_Z, 0.0F);
        this.entityData.define(DATA_SAMPLE_ORDER, 0);
        this.entityData.define(DATA_SPARK_MESH_ANCHOR, false);
        this.entityData.define(DATA_SPARK_LAUNCH_X, 0.0F);
        this.entityData.define(DATA_SPARK_LAUNCH_Y, 0.0F);
        this.entityData.define(DATA_SPARK_LAUNCH_Z, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.releaseOnLoad) {
            this.finishTracker();
            return;
        }
        if (this.trackerArmed) {
            BatchFlameTrail.tick(this, serverLevel);
            return;
        }
        int host = this.hostId();
        if (host >= 0) {
            Entity followed = serverLevel.getEntity(host);
            if (followed == null || followed.isRemoved()) {
                if (this.holdForHost) {
                    this.discard();
                    return;
                }
                this.hostMissingTicks++;
                if (this.hostMissingTicks >= 20) {
                    this.discard();
                }
                return;
            }
            this.hostMissingTicks = 0;
            this.setPos(followed.getX(), followed.getY(), followed.getZ());
        }
        if (this.holdForHost) {
            return;
        }
        if (this.tickCount >= this.durationTicks()) {
            this.discard();
        }
    }

    void noteHostMissing() {
        this.orphanTicks++;
    }

    int orphanTicks() {
        return this.orphanTicks;
    }

    void finishTracker() {
        this.trackerArmed = false;
        if (this.retainedRoot && this.rootCastId != null) {
            com.bslx.XueXiMod.shu.technique.release.batch.RootCastBindings.release(this.rootCastId);
            this.retainedRoot = false;
        }
        this.discard();
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
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (this.boundHostId != null) {
            HOST_VISUALS.remove(this.boundHostId, this.getUUID());
            this.boundHostId = null;
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("VisualShape")) {
            this.entityData.set(DATA_SHAPE, tag.getInt("VisualShape"));
        }
        if (tag.contains("VisualTexture")) {
            this.entityData.set(DATA_TEXTURE, tag.getInt("VisualTexture"));
        }
        if (tag.contains("VisualDuration")) {
            this.entityData.set(DATA_DURATION, tag.getInt("VisualDuration"));
        }
        if (tag.contains("VisualHalfSpan")) {
            this.entityData.set(DATA_HALF_SPAN, tag.getFloat("VisualHalfSpan"));
        }
        if (tag.contains("VisualTint")) {
            this.entityData.set(DATA_TINT, tag.getInt("VisualTint"));
        }
        if (tag.contains("VisualHost")) {
            this.entityData.set(DATA_HOST, tag.getInt("VisualHost"));
        }
        if (tag.contains("VisualTangentX")) {
            this.entityData.set(DATA_TANGENT_X, tag.getFloat("VisualTangentX"));
        }
        if (tag.contains("VisualTangentY")) {
            this.entityData.set(DATA_TANGENT_Y, tag.getFloat("VisualTangentY"));
        }
        if (tag.contains("VisualTangentZ")) {
            this.entityData.set(DATA_TANGENT_Z, tag.getFloat("VisualTangentZ"));
        }
        if (tag.contains("VisualSampleOrder")) {
            this.entityData.set(DATA_SAMPLE_ORDER, tag.getInt("VisualSampleOrder"));
        }
        if (tag.contains("SparkMeshAnchor")) {
            this.entityData.set(DATA_SPARK_MESH_ANCHOR, tag.getBoolean("SparkMeshAnchor"));
        }
        if (tag.contains("SparkLaunchX")) {
            this.entityData.set(DATA_SPARK_LAUNCH_X, tag.getFloat("SparkLaunchX"));
        }
        if (tag.contains("SparkLaunchY")) {
            this.entityData.set(DATA_SPARK_LAUNCH_Y, tag.getFloat("SparkLaunchY"));
        }
        if (tag.contains("SparkLaunchZ")) {
            this.entityData.set(DATA_SPARK_LAUNCH_Z, tag.getFloat("SparkLaunchZ"));
        }
        this.holdForHost = tag.getBoolean("HoldForHost");
        this.trackerArmed = tag.getBoolean("TrackerArmed");
        this.retainedRoot = tag.getBoolean("RetainedRoot");
        if (tag.hasUUID("RootCast")) {
            this.rootCastId = tag.getUUID("RootCast");
        }
        if (this.trackerArmed) {
            this.releaseOnLoad = true;
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("VisualShape", this.entityData.get(DATA_SHAPE));
        tag.putInt("VisualTexture", this.entityData.get(DATA_TEXTURE));
        tag.putInt("VisualDuration", this.entityData.get(DATA_DURATION));
        tag.putFloat("VisualHalfSpan", this.entityData.get(DATA_HALF_SPAN));
        tag.putInt("VisualTint", this.entityData.get(DATA_TINT));
        tag.putInt("VisualHost", this.entityData.get(DATA_HOST));
        tag.putFloat("VisualTangentX", this.entityData.get(DATA_TANGENT_X));
        tag.putFloat("VisualTangentY", this.entityData.get(DATA_TANGENT_Y));
        tag.putFloat("VisualTangentZ", this.entityData.get(DATA_TANGENT_Z));
        tag.putInt("VisualSampleOrder", this.entityData.get(DATA_SAMPLE_ORDER));
        tag.putBoolean("SparkMeshAnchor", this.entityData.get(DATA_SPARK_MESH_ANCHOR));
        tag.putFloat("SparkLaunchX", this.entityData.get(DATA_SPARK_LAUNCH_X));
        tag.putFloat("SparkLaunchY", this.entityData.get(DATA_SPARK_LAUNCH_Y));
        tag.putFloat("SparkLaunchZ", this.entityData.get(DATA_SPARK_LAUNCH_Z));
        tag.putBoolean("HoldForHost", this.holdForHost);
        tag.putBoolean("TrackerArmed", this.trackerArmed);
        tag.putBoolean("RetainedRoot", this.retainedRoot);
        if (this.rootCastId != null) {
            tag.putUUID("RootCast", this.rootCastId);
        }
    }
}
