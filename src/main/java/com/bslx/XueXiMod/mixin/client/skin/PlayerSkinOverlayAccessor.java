package com.bslx.XueXiMod.mixin.client.skin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

@Mixin(PlayerModel.class)
public interface PlayerSkinOverlayAccessor {
    @Accessor("jacket")
    ModelPart xuexiJacket();

    @Accessor("leftSleeve")
    ModelPart xuexiLeftSleeve();

    @Accessor("rightSleeve")
    ModelPart xuexiRightSleeve();

    @Accessor("leftPants")
    ModelPart xuexiLeftPants();

    @Accessor("rightPants")
    ModelPart xuexiRightPants();

    @Accessor("cloak")
    ModelPart xuexiCloak();
}
