package com.bslx.XueXiMod.status.wood.vine;

import com.bslx.XueXiMod.shu.technique.release.entity.ReleaseEntities;
import com.bslx.XueXiMod.shu.technique.release.firecast.DamagePermission;
import com.bslx.XueXiMod.shu.technique.release.relations.HarmPolicy;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** 调动后的直线枝条。只负责升级已有一级流血，不另加伤害。 */
public final class RoseVineProjectile extends ThrowableItemProjectile {
    private UUID ownerId = new UUID(0L, 0L);
    private int lifeTicks;
    private boolean spent;

    public RoseVineProjectile(EntityType<? extends RoseVineProjectile> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static RoseVineProjectile launch(ServerPlayer owner, Vec3 from, Vec3 direction) {
        RoseVineProjectile projectile = new RoseVineProjectile(ReleaseEntities.ROSE_VINE.get(), owner.serverLevel());
        projectile.ownerId = owner.getUUID();
        projectile.lifeTicks = VineMarkBalance.fallenFlightTicks();
        projectile.setOwner(owner);
        projectile.setPos(from.x, from.y, from.z);
        Vec3 unit = direction.lengthSqr() < 1.0E-8d ? owner.getLookAngle() : direction.normalize();
        double speed = VineMarkBalance.fallenSpeed();
        projectile.setDeltaMovement(unit.scale(speed));
        projectile.setNoGravity(true);
        owner.serverLevel().addFreshEntity(projectile);
        return projectile;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.level().addParticle(ParticleTypes.FALLING_SPORE_BLOSSOM,
                    this.getX(), this.getY(), this.getZ(), 0.0d, 0.0d, 0.0d);
            return;
        }
        this.lifeTicks--;
        if (this.lifeTicks <= 0) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (this.level().isClientSide() || this.spent || !(hit.getEntity() instanceof LivingEntity target)) {
            return;
        }
        ServerPlayer owner = ownerPlayer();
        if (owner == null || !DamagePermission.allows(owner, target) || HarmPolicy.denies(owner, target)) {
            this.spent = true;
            this.discard();
            return;
        }
        this.spent = true;
        VineMarkRuntime.upgradeFromProjectile(owner, target);
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        this.spent = true;
        this.discard();
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SPORE_BLOSSOM;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putUUID("RoseOwner", this.ownerId);
        tag.putInt("RoseLife", this.lifeTicks);
        tag.putBoolean("RoseSpent", this.spent);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.ownerId = tag.hasUUID("RoseOwner") ? tag.getUUID("RoseOwner") : new UUID(0L, 0L);
        this.lifeTicks = tag.getInt("RoseLife");
        this.spent = tag.getBoolean("RoseSpent");
        this.setNoGravity(true);
    }

    private ServerPlayer ownerPlayer() {
        if (!(this.level() instanceof ServerLevel level) || level.getServer() == null) {
            return null;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(this.ownerId);
        if (player == null || !player.isAlive()) {
            return null;
        }
        return player;
    }
}
