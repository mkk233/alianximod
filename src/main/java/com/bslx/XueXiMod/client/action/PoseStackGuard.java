package com.bslx.XueXiMod.client.action;

import java.util.Deque;

import com.bslx.XueXiMod.mixin.client.PoseStackFramesAccessor;

import com.mojang.blaze3d.vertex.PoseStack;

/** 记录入口深度，只退回到这一层多出来的矩阵。深度不足时不补推。 */
public final class PoseStackGuard {
    private PoseStackGuard() {
    }

    public static int depth(PoseStack poseStack) {
        return frames(poseStack).size();
    }

    public static void restore(PoseStack poseStack, int entryDepth) {
        if (poseStack == null || entryDepth < 0) {
            return;
        }
        Deque<?> frames = frames(poseStack);
        while (frames.size() > entryDepth) {
            poseStack.popPose();
        }
    }

    private static Deque<?> frames(PoseStack poseStack) {
        return ((PoseStackFramesAccessor) poseStack).xuexiFrames();
    }
}
