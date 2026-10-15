package com.bslx.XueXiMod.shu.technique.sword;

import com.bslx.XueXiMod.network.ModNetwork;
import com.bslx.XueXiMod.network.ShuSyncPacket;
import com.bslx.XueXiMod.player.PlayerShuData;
import com.bslx.XueXiMod.shu.technique.effects.LockTargetService;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.entity.ReleaseEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashSet;
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

    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
            SynchedEntityData.defineId(SwordEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DATA_STATE =
            SynchedEntityData.defineId(SwordEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PHANTOM =
            SynchedEntityData.defineId(SwordEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(SwordEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private UUID owner = UUID.randomUUID();
    private int hoverTicks;
    private int flyTicks;
    private int plantedTicks;
    private Vec3 flyDirection = new Vec3(0, 0, 1);
    private double flown;
    private boolean realFalls;
    private BlockPos plantedBlock = BlockPos.ZERO;
    private Vec3 returnSpot;
    private boolean recallIdle;
    private boolean rising;
    private double riseTargetY;
    private boolean occupied;
    private final Set<UUID> hit = new HashSet<>();
    private int burnSeconds;
    private int controlTicks;
    private boolean extinguish;
    private float knockback;
    private float copiedDamage = 1f;
    private boolean lockAim;
    private final com.bslx.XueXiMod.shu.technique.release.entity.ClientTrack clientTrack =
            new com.bslx.XueXiMod.shu.technique.release.entity.ClientTrack();

    public SwordEntity(EntityType<? extends SwordEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static SwordEntity phantom(ServerLevel level, ServerPlayer player, Vec3 pos, ItemStack copy, CastFrame frame, boolean lockAim) {
        SwordEntity sword = new SwordEntity(ReleaseEntities.SWORD.get(), level);
        sword.setPos(pos.x, pos.y, pos.z);
        sword.setOwner(player.getUUID());
        sword.lockAim = lockAim;
        sword.entityData.set(DATA_PHANTOM, true);
        sword.entityData.set(DATA_ITEM, copy.copy());
        sword.entityData.set(DATA_STATE, HOVER);
        sword.copiedDamage = damageOf(copy);
        sword.burnSeconds = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, copy) * 4;
        if (frame != null) {
            sword.burnSeconds = Math.max(sword.burnSeconds, frame.burnSeconds);
            sword.controlTicks = frame.controlTicks;
            sword.extinguish = frame.extinguish;
            sword.knockback = frame.knockback;
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
        sword.copiedDamage = damageOf(stack);
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
            this.entityData.get(DATA_OWNER).ifPresent(id -> this.owner = id);
        }
    }

    private void setOwner(UUID id) {
        this.owner = id;
        this.entityData.set(DATA_OWNER, Optional.ofNullable(id));
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

    public boolean isManipulable() {
        return !this.occupied && (state() == PLANTED || state() == HOVER);
    }

    /** 当前维度里是否存在该玩家拥有的真剑或幻剑实体（任意状态）。 */
    public static boolean anyOwnedBy(ServerLevel level, UUID owner) {
        if (level == null || owner == null) {
            return false;
        }
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof SwordEntity sword
                    && sword.isAlive()
                    && owner.equals(sword.ownerUuid())) {
                return true;
            }
        }
        return false;
    }

    /** 召回：飞行中、停滞中、插地中都可以被召回。eyeSpot 固定为召回瞬间的眼睛位置。 */
    public void startReturning(Vec3 eyeSpot) {
        this.hit.clear();
        this.returnSpot = eyeSpot;
        this.recallIdle = false;
        this.rising = this.getY() < eyeSpot.y;
        if (this.rising) {
            this.riseTargetY = this.getY() + 2.0 + this.random.nextDouble() * 2.0;
        }
        this.returnSpot = this.approachPoint(eyeSpot);
        setState(RETURN);
        this.realFalls = false;
        this.setDeltaMovement(Vec3.ZERO);
    }

    public ItemStack swordItem() {
        return this.entityData.get(DATA_ITEM);
    }

    private int state() {
        return this.entityData.get(DATA_STATE);
    }

    private void setState(int state) {
        this.entityData.set(DATA_STATE, state);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_ITEM, ItemStack.EMPTY);
        this.entityData.define(DATA_STATE, HOVER);
        this.entityData.define(DATA_PHANTOM, true);
        this.entityData.define(DATA_OWNER, Optional.empty());
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        if (this.level().isClientSide()) {
            this.clientTrack.onClientMove(this, x, y, z, yRot, xRot);
            return;
        }
        super.lerpTo(x, y, z, yRot, xRot, steps, teleport);
    }

    @Override
    public void moveTo(double x, double y, double z, float yRot, float xRot) {
        if (this.level().isClientSide()) {
            this.clientTrack.onClientMove(this, x, y, z, yRot, xRot);
            return;
        }
        super.moveTo(x, y, z, yRot, xRot);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.clientTrack.afterClientTick(this);
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        if (state() == HOVER) {
            this.hoverTicks++;
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
                aim(this.flyDirection);
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
        this.plantedTicks++;
        if (this.recallIdle) {
            tryRecoverOwner(level);
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
                dropRealItem(level);
            }
        }
    }

    private void fly(ServerLevel level) {
        this.flyTicks++;
        double speed = isPhantom() ? 2.5 : 1.2;
        if (!isPhantom() && this.flyTicks >= 40) {
            this.realFalls = true;
        }
        Vec3 from = this.position();
        Vec3 step = this.realFalls
                ? this.getDeltaMovement().add(0, -0.08, 0)
                : this.flyDirection.scale(speed);
        if (this.realFalls) {
            this.setDeltaMovement(step);
        }
        Vec3 to = from.add(step);
        this.flown += from.distanceTo(to);
        BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 stop = block.getType() == HitResult.Type.BLOCK ? block.getLocation() : to;
        hurtAlong(level, from, stop);
        if (block.getType() == HitResult.Type.BLOCK && level.getBlockState(block.getBlockPos()).getFluidState().isEmpty()) {
            plant(block);
            return;
        }
        this.setPos(stop.x, stop.y, stop.z);
        aim(this.realFalls ? step : this.flyDirection);
        if (isPhantom() && (this.flyTicks >= 80 || this.flown >= 64)) {
            discard();
            return;
        }
        if (this.getY() < level.getMinBuildHeight() - 8 && isReal()) {
            returnReal(level);
        }
    }

    private void returnTick(ServerLevel level) {
        if (this.returnSpot == null) {
            setState(PLANTED);
            this.recallIdle = true;
            return;
        }
        if (this.rising) {
            if (this.getY() >= this.riseTargetY) {
                this.rising = false;
                return;
            }
            Vec3 from = this.position();
            double dy = Math.min(1.5, this.riseTargetY - this.getY());
            Vec3 to = from.add(0, dy, 0);
            hurtAlong(level, from, to);
            this.setPos(to.x, to.y, to.z);
            aim(this.returnSpot.subtract(this.position()));
            if (this.getY() >= this.riseTargetY) {
                this.rising = false;
            }
            return;
        }
        if (tryRecoverOwner(level)) {
            return;
        }
        Vec3 delta = this.returnSpot.subtract(this.position());
        double dist = delta.length();
        if (dist <= 0.4) {
            this.setPos(this.returnSpot.x, this.returnSpot.y, this.returnSpot.z);
            setState(PLANTED);
            this.plantedBlock = null;
            this.recallIdle = true;
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        Vec3 step = delta.scale(Math.min(1.5, dist) / dist);
        Vec3 from = this.position();
        Vec3 to = from.add(step);
        hurtAlong(level, from, to);
        this.setPos(to.x, to.y, to.z);
        aim(step);
    }

    /** 每把剑沿着自己相对眼睛的水平方向回来，落在眼睛外侧 1 格，避免挤成同一条线。 */
    private Vec3 approachPoint(Vec3 eye) {
        double dx = this.getX() - eye.x;
        double dz = this.getZ() - eye.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-4) {
            double ang = (this.getId() & 7) * (Math.PI / 4.0);
            dx = Math.cos(ang);
            dz = Math.sin(ang);
            len = 1.0;
        }
        return new Vec3(eye.x + dx / len, eye.y, eye.z + dz / len);
    }

    /** 主人包围盒与剑的包围盒距离小于 1.2 格时回收，只认主人。 */
    private boolean tryRecoverOwner(ServerLevel level) {
        if (!(level.getPlayerByUUID(this.owner) instanceof ServerPlayer ownerPlayer) || !ownerPlayer.isAlive()) {
            return false;
        }
        if (this.getBoundingBox().inflate(1.2).intersects(ownerPlayer.getBoundingBox())) {
            recover(ownerPlayer);
            return true;
        }
        return false;
    }

    private void recover(ServerPlayer player) {
        if (isPhantom()) {
            PlayerShuData data = PlayerShuData.get(player);
            data.setMagic(Math.min(data.getMagicMax(), data.getMagic() + 8));
            PlayerShuData.save(player, data);
            ModNetwork.sendToPlayer(player, new ShuSyncPacket(data));
            discard();
            return;
        }
        ItemStack stack = swordItem().copy();
        if (player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        } else if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        discard();
    }

    private void hurtAlong(ServerLevel level, Vec3 from, Vec3 to) {
        AABB box = new AABB(from, to).inflate(0.4);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, this::canHit)) {
            if (this.hit.contains(target.getUUID()) || this.hit.size() >= 5) {
                continue;
            }
            if (target.getBoundingBox().clip(from, to).isEmpty() && !target.getBoundingBox().inflate(0.2).contains(to)) {
                continue;
            }
            this.hit.add(target.getUUID());
            Player ownerPlayer = level.getPlayerByUUID(this.owner);
            DamageSource source = ownerPlayer instanceof ServerPlayer server
                    ? level.damageSources().playerAttack(server)
                    : level.damageSources().magic();
            target.hurt(source, this.copiedDamage);
            if (this.burnSeconds > 0) {
                target.setSecondsOnFire(this.burnSeconds);
            }
            if (this.extinguish) {
                target.clearFire();
            }
            if (this.controlTicks > 0) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, this.controlTicks, 4));
            }
            if (this.knockback > 0f) {
                Vec3 delta = target.position().subtract(from);
                target.knockback(this.knockback, delta.x, delta.z);
            }
        }
    }

    private boolean canHit(LivingEntity entity) {
        return entity.isAlive() && !entity.isSpectator() && !entity.getUUID().equals(this.owner);
    }

    /**
     * Tip follows the release direction. Center stays on the surface so the tip
     * sits about half a block inside. Shared by real and phantom swords.
     */
    private void plant(BlockHitResult hit) {
        Vec3 into = Vec3.atLowerCornerOf(hit.getDirection().getOpposite().getNormal());
        Vec3 tipDir = plantTipDirection(into, this.flyDirection);
        // FIXED model center ~= mid-blade; center-to-tip ~0.5; tip embed ~0.5 => center on surface
        double tipFromCenter = 0.5;
        double tipEmbed = 0.5;
        Vec3 loc = hit.getLocation().add(tipDir.scale(tipEmbed - tipFromCenter));
        this.setPos(loc.x, loc.y, loc.z);
        aim(tipDir);
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        setState(PLANTED);
        this.setDeltaMovement(Vec3.ZERO);
        this.plantedBlock = hit.getBlockPos();
        this.realFalls = false;
    }

    /** Sword axis stays parallel to the release direction. Flip only so the tip points into the block. */
    private static Vec3 plantTipDirection(Vec3 into, Vec3 incoming) {
        Vec3 in = into.normalize();
        Vec3 tip = incoming.lengthSqr() < 1.0E-6 ? in : incoming.normalize();
        if (tip.dot(in) < 0.0) {
            tip = tip.scale(-1.0);
        }
        return tip;
    }

    private void aim(Vec3 dir) {
        if (dir.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3 d = dir.normalize();
        double horizontal = Math.sqrt(d.x * d.x + d.z * d.z);
        this.setYRot((float) (Math.atan2(d.z, d.x) * (180.0 / Math.PI)) - 90.0F);
        this.setXRot((float) (-Math.atan2(d.y, horizontal) * (180.0 / Math.PI)));
    }

    private void returnReal(ServerLevel level) {
        ItemStack stack = swordItem().copy();
        Player ownerPlayer = level.getPlayerByUUID(this.owner);
        if (ownerPlayer instanceof ServerPlayer server) {
            if (!server.getInventory().add(stack)) {
                server.drop(stack, false);
            }
        } else {
            spawnAtLocation(stack);
        }
        discard();
    }

    private void dropRealItem(ServerLevel level) {
        spawnAtLocation(swordItem().copy());
        discard();
    }

    private static float damageOf(ItemStack stack) {
        float damage = 1f;
        for (AttributeModifier modifier : stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                damage += (float) modifier.getAmount();
            }
        }
        damage += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, stack) * 1.25f;
        return damage;
    }

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
        this.controlTicks = tag.getInt("Control");
        this.extinguish = tag.getBoolean("Extinguish");
        this.knockback = tag.getFloat("Knock");
        this.lockAim = tag.getBoolean("LockAim");
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
        tag.putInt("Control", this.controlTicks);
        tag.putBoolean("Extinguish", this.extinguish);
        tag.putFloat("Knock", this.knockback);
        tag.putBoolean("LockAim", this.lockAim);
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
