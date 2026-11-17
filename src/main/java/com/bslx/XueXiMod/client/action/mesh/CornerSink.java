package com.bslx.XueXiMod.client.action.mesh;

/** 无 GPU 的角点出口。下一批把它接到原版 VertexConsumer。 */
public interface CornerSink {
    void corner(
            float positionX,
            float positionY,
            float positionZ,
            float normalX,
            float normalY,
            float normalZ,
            float textureU,
            float textureV,
            int packedLight,
            int overlay);
}
