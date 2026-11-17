package com.bslx.XueXiMod.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 纠正后重设本地预测基准，不改当前速度。 */
@Mixin(LocalPlayer.class)
public interface LocalPlayerAnchorAccess {
    @Accessor("xLast")
    void xuexiAnchorX(double anchorX);

    @Accessor("yLast1")
    void xuexiAnchorY(double anchorY);

    @Accessor("zLast")
    void xuexiAnchorZ(double anchorZ);

    @Accessor("yRotLast")
    void xuexiAnchorYaw(float anchorYaw);

    @Accessor("xRotLast")
    void xuexiAnchorPitch(float anchorPitch);

    @Accessor("lastOnGround")
    void xuexiAnchorGround(boolean onGround);
}
