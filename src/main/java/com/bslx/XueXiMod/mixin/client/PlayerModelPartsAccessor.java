package com.bslx.XueXiMod.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

@Mixin(PlayerModel.class)
public interface PlayerModelPartsAccessor {
    @Accessor("cloak")
    ModelPart xuexiCloak();

    @Accessor("ear")
    ModelPart xuexiEar();
}
