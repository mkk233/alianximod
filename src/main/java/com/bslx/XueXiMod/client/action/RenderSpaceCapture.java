package com.bslx.XueXiMod.client.action;

import java.util.ArrayDeque;

import org.joml.Matrix4f;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * 在现有玩家渲染入口记下世界到模型之前的矩阵。
 * setupAnim 时再读同一条 PoseStack，因此游泳、鞘翅和其它原版旋转都在里面。
 */
public final class RenderSpaceCapture {
    private static final ArrayDeque<PoseStack> STACKS = new ArrayDeque<>();
    private static final ArrayDeque<OpenMatrix4f> ENTRIES = new ArrayDeque<>();

    private RenderSpaceCapture() {
    }

    public static void push(PoseStack poseStack) {
        STACKS.push(poseStack);
        ENTRIES.push(copy(poseStack));
    }

    public static void pop() {
        if (!STACKS.isEmpty()) {
            STACKS.pop();
            ENTRIES.pop();
        }
    }

    public static OpenMatrix4f worldToModel() {
        if (STACKS.isEmpty() || ENTRIES.isEmpty()) {
            return null;
        }
        OpenMatrix4f current = copy(STACKS.peek());
        OpenMatrix4f entry = ENTRIES.peek();
        if (current == null || entry == null) {
            return null;
        }
        try {
            OpenMatrix4f inverseCurrent = new OpenMatrix4f(current);
            inverseCurrent.invert();
            OpenMatrix4f combined = OpenMatrix4f.mul(inverseCurrent, entry, null);
            combined.m30 = 0.0F;
            combined.m31 = 0.0F;
            combined.m32 = 0.0F;
            combined.m33 = 1.0F;
            if (!combined.hasFiniteElements()) {
                return null;
            }
            return combined;
        } catch (IllegalArgumentException failure) {
            return null;
        }
    }

    private static OpenMatrix4f copy(PoseStack poseStack) {
        if (poseStack == null) {
            return null;
        }
        Matrix4f live = poseStack.last().pose();
        OpenMatrix4f copied = new OpenMatrix4f();
        copied.m00 = live.m00();
        copied.m01 = live.m01();
        copied.m02 = live.m02();
        copied.m03 = live.m03();
        copied.m10 = live.m10();
        copied.m11 = live.m11();
        copied.m12 = live.m12();
        copied.m13 = live.m13();
        copied.m20 = live.m20();
        copied.m21 = live.m21();
        copied.m22 = live.m22();
        copied.m23 = live.m23();
        copied.m30 = live.m30();
        copied.m31 = live.m31();
        copied.m32 = live.m32();
        copied.m33 = live.m33();
        return copied;
    }
}
