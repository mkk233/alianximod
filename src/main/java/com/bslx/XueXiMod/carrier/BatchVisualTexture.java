package com.bslx.XueXiMod.carrier;

/**
 * 运行时竖条。每张都是 64×1024 的 16 帧，渲染按帧切 V，不把整张拉成一个面。
 * 幽冥条带不在这里。陨岩六面贴图另走方块模型。
 */
public enum BatchVisualTexture {
    SPARK("textures/effect/fire_spark_v3/fire_spark.png"),
    TRAIL("textures/effect/flame_trail_v3/flame_trail.png"),
    SHOCK("textures/effect/shock_ring_v1/shock_ring.png"),
    SURFACE("textures/effect/surface_anchor_v2/surface_anchor.png"),
    SEED("textures/effect/seed_sow_v3/seed_sow.png"),
    KINDLE("textures/effect/kindle_v1/kindle.png"),
    DETONATE("textures/effect/detonate_v2_3d/explosion_puff_orange.png"),
    DUST("textures/effect/ground_dust_v2r2/ground_airborne.png"),
    NONE("");

    private final String texturePath;

    BatchVisualTexture(String texturePath) {
        this.texturePath = texturePath;
    }

    public String texturePath() {
        return this.texturePath;
    }

    public static BatchVisualTexture fromOrdinal(int ordinal) {
        BatchVisualTexture[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return NONE;
        }
        return values[ordinal];
    }
}
