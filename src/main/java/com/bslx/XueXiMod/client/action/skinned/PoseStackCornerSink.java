package com.bslx.XueXiMod.client.action.skinned;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.bslx.XueXiMod.client.action.mesh.CornerSink;
import com.bslx.XueXiMod.client.action.mesh.SkinDiagnostic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** 角点已经在 A*sourceToView 空间里。这里只乘当前 PoseStack 一次。 */
public final class PoseStackCornerSink implements CornerSink {
    private final PoseStack poseStack;
    private final VertexConsumer consumer;
    private final int red;
    private final int green;
    private final int blue;
    private final int alpha;

    public PoseStackCornerSink(PoseStack poseStack, VertexConsumer consumer, float red, float green, float blue, float alpha) {
        if (poseStack == null || consumer == null) {
            throw new SkinDiagnostic("sink", "绘制矩阵或顶点消费者缺失");
        }
        this.poseStack = poseStack;
        this.consumer = consumer;
        this.red = colorByte(red);
        this.green = colorByte(green);
        this.blue = colorByte(blue);
        this.alpha = colorByte(alpha);
    }

    @Override
    public void corner(
            float positionX,
            float positionY,
            float positionZ,
            float normalX,
            float normalY,
            float normalZ,
            float textureU,
            float textureV,
            int packedLight,
            int overlay) {
        Matrix4f poseMatrix = this.poseStack.last().pose();
        Matrix3f normalMatrix = this.poseStack.last().normal();
        this.consumer.vertex(poseMatrix, positionX, positionY, positionZ)
                .color(this.red, this.green, this.blue, this.alpha)
                .uv(textureU, textureV)
                .overlayCoords(overlay)
                .uv2(packedLight)
                .normal(normalMatrix, normalX, normalY, normalZ)
                .endVertex();
    }

    private static int colorByte(float channel) {
        if (!Float.isFinite(channel) || channel <= 0.0F) {
            return 0;
        }
        if (channel >= 1.0F) {
            return 255;
        }
        return (int) (channel * 255.0F);
    }
}
