package com.bslx.XueXiMod.mixin.client;

import java.util.Deque;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.vertex.PoseStack;

@Mixin(PoseStack.class)
public interface PoseStackFramesAccessor {
    @Accessor("poseStack")
    Deque<PoseStack.Pose> xuexiFrames();
}
