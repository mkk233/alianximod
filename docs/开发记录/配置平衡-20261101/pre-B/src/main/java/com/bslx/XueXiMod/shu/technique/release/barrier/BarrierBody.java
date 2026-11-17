package com.bslx.XueXiMod.shu.technique.release.barrier;

import com.bslx.XueXiMod.player.MagicPool;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactCarrier;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactEnchantState;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHosts;
import com.bslx.XueXiMod.shu.technique.release.deflection.hosts.WallDeflectionHost;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.Optional;
import java.util.UUID;

/** 固定在施法位置的薄墙实体。主人走动不会带走它。 */
public class BarrierBody extends Entity implements ContactCarrier {
    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(BarrierBody.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> THIN_AXIS =
            SynchedEntityData.defineId(BarrierBody.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> WALL_WIDTH =
            SynchedEntityData.defineId(BarrierBody.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> WALL_HEIGHT =
            SynchedEntityData.defineId(BarrierBody.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> WALL_THICK =
            SynchedEntityData.defineId(BarrierBody.class, EntityDataSerializers.FLOAT);

    private AxisAlignedWall wall = new AxisAlignedWall(new AABB(0, 0, 0, 1, 1, 1));
    private int expireTick;
    private int nextUpkeepTick;
    private int upkeepCost;
    private ContactEnchantState contact;
    private int contactRevision = 1;
    private boolean deflectIncoming;
    private long deflectionGeneration = 1L;
    private WallDeflectionHost deflectionHost;

    public BarrierBody(EntityType<? extends BarrierBody> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.setInvulnerable(true);
        this.setNoGravity(true);
    }

    public void configure(UUID ownerId, AxisAlignedWall placed, int expireTick, int nextUpkeepTick, int upkeepCost,
                          EnchantmentSnapshot snapshot) {
        configure(ownerId, placed, expireTick, nextUpkeepTick, upkeepCost, snapshot, false);
    }

    public void configure(UUID ownerId, AxisAlignedWall placed, int expireTick, int nextUpkeepTick, int upkeepCost,
                          EnchantmentSnapshot snapshot, boolean deflectIncoming) {
        this.wall = placed;
        this.expireTick = expireTick;
        this.nextUpkeepTick = nextUpkeepTick;
        this.upkeepCost = upkeepCost;
        this.deflectIncoming = deflectIncoming;
        this.entityData.set(OWNER, Optional.ofNullable(ownerId));
        Vec3 center = placed.center();
        this.setPos(center.x, center.y, center.z);
        this.setBoundingBox(placed.box());
        writeExtents(placed.box());
        if (snapshot != null && this.level() instanceof ServerLevel) {
            this.contact = ContactEnchantState.open(this, snapshot);
        }
    }

    public UUID ownerId() {
        return this.entityData.get(OWNER).orElse(null);
    }

    public AxisAlignedWall wall() {
        return wall;
    }

    public float attenuation() {
        return BarrierDefinition.curtain().attenuation();
    }

    public int expireTick() {
        return expireTick;
    }

    public static boolean withinLife(int now, int expireTick) {
        return !ReleaseInput.hasExpired(now, expireTick);
    }

    public boolean deflectsProjectiles() {
        return deflectIncoming;
    }

    /** 墙已经进世界之后再登记。放不下的那面不要进注册表。 */
    public void openDeflection() {
        if (!this.deflectIncoming || this.deflectionHost != null || this.level().isClientSide || ownerId() == null) {
            return;
        }
        if (!(this.level() instanceof ServerLevel)) {
            return;
        }
        this.deflectionHost = new WallDeflectionHost(this, this.deflectionGeneration);
        DeflectionHosts.register(this.deflectionHost);
    }

    public void closeDeflection() {
        if (this.deflectionHost == null) {
            return;
        }
        DeflectionHosts.unregister(this.deflectionHost.key());
        this.deflectionHost = null;
    }

    @Override
    public UUID hostId() {
        return getUUID();
    }

    @Override
    public int revision() {
        return contactRevision;
    }

    @Override
    public boolean active() {
        return isAlive() && !isRemoved();
    }

    @Override
    public boolean touches(LivingEntity target) {
        return wall.touches(target, ContactEnchantState.SURFACE_SLOP);
    }

    @Override
    public ServerLevel hostLevel() {
        return this.level() instanceof ServerLevel serverLevel ? serverLevel : null;
    }

    @Override
    public AABB queryBox() {
        return wall.box().inflate(2.0);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER, Optional.empty());
        this.entityData.define(THIN_AXIS, 0);
        this.entityData.define(WALL_WIDTH, 3.2f);
        this.entityData.define(WALL_HEIGHT, 2.8f);
        this.entityData.define(WALL_THICK, 0.25f);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        refreshWall();
    }

    @Override
    public void tick() {
        super.tick();
        refreshWall();
        if (this.level().isClientSide) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) this.level();
        int now = serverLevel.getServer().getTickCount();
        UUID owner = ownerId();
        ServerPlayer player = owner == null ? null : serverLevel.getServer().getPlayerList().getPlayer(owner);
        if (player == null || !player.isAlive() || player.level() != serverLevel || !withinLife(now, expireTick)) {
            discard();
            return;
        }
        if (now >= nextUpkeepTick) {
            if (!MagicPool.trySpend(player, upkeepCost, false)) {
                discard();
                return;
            }
            nextUpkeepTick = now + BarrierDefinition.curtain().upkeepPeriod();
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        closeDeflection();
        if (contact != null) {
            contact.close();
            contact = null;
        }
        BarrierService.forget(this);
        super.remove(reason);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
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
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void writeExtents(AABB box) {
        boolean thinX = (box.maxX - box.minX) < (box.maxZ - box.minZ);
        this.entityData.set(THIN_AXIS, thinX ? 0 : 1);
        this.entityData.set(WALL_WIDTH, (float) (thinX ? box.maxZ - box.minZ : box.maxX - box.minX));
        this.entityData.set(WALL_HEIGHT, (float) (box.maxY - box.minY));
        this.entityData.set(WALL_THICK, (float) (thinX ? box.maxX - box.minX : box.maxZ - box.minZ));
    }

    private void refreshWall() {
        if (!this.level().isClientSide && wall != null) {
            this.setBoundingBox(wall.box());
            return;
        }
        float width = this.entityData.get(WALL_WIDTH);
        float height = this.entityData.get(WALL_HEIGHT);
        float thick = this.entityData.get(WALL_THICK);
        boolean thinX = this.entityData.get(THIN_AXIS) == 0;
        double halfWidth = width * 0.5;
        double halfHeight = height * 0.5;
        double halfThick = thick * 0.5;
        AABB rebuilt = thinX
                ? new AABB(getX() - halfThick, getY() - halfHeight, getZ() - halfWidth,
                getX() + halfThick, getY() + halfHeight, getZ() + halfWidth)
                : new AABB(getX() - halfWidth, getY() - halfHeight, getZ() - halfThick,
                getX() + halfWidth, getY() + halfHeight, getZ() + halfThick);
        this.wall = new AxisAlignedWall(rebuilt);
        this.setBoundingBox(rebuilt);
    }
}
