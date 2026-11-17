package com.bslx.XueXiMod.client.action.mesh;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** 下一批可直接把退化四边形角点交给原版 VertexConsumer。本批不绑定皮肤纹理。 */
public final class VertexConsumerCornerSink implements CornerSink {
    private final VertexConsumer consumer;
    private final int red;
    private final int green;
    private final int blue;
    private final int alpha;

    public VertexConsumerCornerSink(VertexConsumer consumer, int red, int green, int blue, int alpha) {
        if (consumer == null) {
            throw new SkinDiagnostic("sink", "顶点消费者缺失");
        }
        this.consumer = consumer;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
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
        this.consumer.vertex(positionX, positionY, positionZ)
                .color(this.red, this.green, this.blue, this.alpha)
                .uv(textureU, textureV)
                .overlayCoords(overlay)
                .uv2(packedLight)
                .normal(normalX, normalY, normalZ)
                .endVertex();
    }
}
