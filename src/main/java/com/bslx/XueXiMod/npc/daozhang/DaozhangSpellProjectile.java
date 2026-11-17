package com.bslx.XueXiMod.npc.daozhang;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** 道长自己的弹体。不走玩家施法，不点燃地面。 */
public class DaozhangSpellProjectile extends ThrowableItemProjectile {
    public enum Kind {
        TALISMAN,
        FIRE
    }

    private Vec3 origin = Vec3.ZERO;

    public DaozhangSpellProjectile(EntityType<? extends DaozhangSpellProjectile> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public DaozhangSpellProjectile(ServerLevel level, DaozhangEntity shooter) {
        super(DaozhangTypes.SPELL.get(), shooter, level);
        this.setNoGravity(true);
        this.origin = shooter.getEyePosition();
    }

    public static void shoot(DaozhangEntity daozhang, LivingEntity target, Kind kind) {
        if (!(daozhang.level() instanceof ServerLevel level) || target == null || kind == null) {
            return;
        }
        Vec3 from = daozhang.getEyePosition();
        Vec3 aim = target.getEyePosition().subtract(from);
        if (aim.lengthSqr() < 1.0E-6) {
            return;
        }
        DaozhangSpellProjectile projectile = new DaozhangSpellProjectile(level, daozhang);
        projectile.setPos(from.x, from.y, from.z);
        projectile.setItem(kind == Kind.FIRE ? new net.minecraft.world.item.ItemStack(Items.FIRE_CHARGE)
                : new net.minecraft.world.item.ItemStack(Items.PAPER));
        projectile.setDeltaMovement(aim.normalize().scale(DaozhangConfig.projectileSpeed()));
        level.addFreshEntity(projectile);
        level.playSound(null, daozhang.getX(), daozhang.getEyeY(), daozhang.getZ(),
                kind == Kind.FIRE ? SoundEvents.BLAZE_SHOOT : SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.HOSTILE, 0.8F, kind == Kind.FIRE ? 0.9F : 1.3F);
    }

    public Kind kind() {
        return this.getItem().is(Items.FIRE_CHARGE) ? Kind.FIRE : Kind.TALISMAN;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.PAPER;
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide && !this.ownerValid()) {
            this.discard();
            return;
        }
        if (!this.level().isClientSide && (this.tickCount > DaozhangConfig.projectileLifeTicks()
                || this.position().distanceTo(this.origin) > DaozhangConfig.castRange())) {
            this.discard();
            return;
        }
        super.tick();
        if (this.level().isClientSide) {
            this.level().addParticle(this.kind() == Kind.FIRE ? ParticleTypes.FLAME : ParticleTypes.ENCHANT,
                    this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        if (this.level().isClientSide || result == null) {
            return;
        }
        if (result.getType() == HitResult.Type.ENTITY && result instanceof EntityHitResult entityHit) {
            this.hitLiving(entityHit);
            return;
        }
        if (result.getType() == HitResult.Type.BLOCK) {
            this.discard();
        }
    }

    private void hitLiving(EntityHitResult result) {
        if (!(this.getOwner() instanceof DaozhangEntity daozhang) || !daozhang.isAlive()
                || !(result.getEntity() instanceof LivingEntity target) || !target.isAlive()) {
            this.discard();
            return;
        }
        if (target instanceof Player player && daozhang.relation(player.getUUID()) != DaozhangRelation.HOSTILE) {
            this.discard();
            return;
        }
        float amount = this.kind() == Kind.FIRE ? DaozhangConfig.fireDamage() : DaozhangConfig.talismanDamage();
        boolean accepted = target.hurt(DaozhangDamage.source((ServerLevel) this.level(), this, daozhang), amount);
        if (accepted && this.kind() == Kind.FIRE && DaozhangConfig.fireSeconds() > 0) {
            target.setSecondsOnFire(DaozhangConfig.fireSeconds());
        }
        this.discard();
    }

    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity entity) {
        return super.canHitEntity(entity) && entity instanceof LivingEntity && entity != this.getOwner();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("OriginX", this.origin.x);
        tag.putDouble("OriginY", this.origin.y);
        tag.putDouble("OriginZ", this.origin.z);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.origin = new Vec3(tag.getDouble("OriginX"), tag.getDouble("OriginY"), tag.getDouble("OriginZ"));
    }

    private boolean ownerValid() {
        return this.getOwner() instanceof DaozhangEntity daozhang && daozhang.isAlive() && !daozhang.isRemoved()
                && daozhang.level() == this.level();
    }
}
