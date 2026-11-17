package com.bslx.XueXiMod.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Projectile.class)
public interface ProjectileOwnerAccess {
    @Invoker("addAdditionalSaveData")
    void xuexiWriteProjectileData(CompoundTag tag);

    @Invoker("readAdditionalSaveData")
    void xuexiReadProjectileData(CompoundTag tag);
}
