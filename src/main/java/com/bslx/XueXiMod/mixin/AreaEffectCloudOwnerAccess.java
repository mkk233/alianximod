package com.bslx.XueXiMod.mixin;

import net.minecraft.world.entity.AreaEffectCloud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(AreaEffectCloud.class)
public interface AreaEffectCloudOwnerAccess {
    @Accessor("ownerUUID")
    UUID xuexiOwnerUUID();

    @Accessor("ownerUUID")
    void xuexiSetOwnerUUID(UUID ownerId);
}
