package com.bslx.XueXiMod.shu.technique;

public final class EnchantmentAccumulator {
    private final String effectId;
    private final int layers;
    private final float radiusBase;
    private final float durationBase;
    private final float radiusMultiplier;
    private final float durationMultiplier;
    private final float damageMultiplier;
    private final float speedMultiplier;

    public EnchantmentAccumulator(String effectId, int layers, float radiusBase, float durationBase,
                                  float radiusMultiplier, float durationMultiplier,
                                  float damageMultiplier, float speedMultiplier) {
        this.effectId = effectId;
        this.layers = layers;
        this.radiusBase = radiusBase;
        this.durationBase = durationBase;
        this.radiusMultiplier = radiusMultiplier;
        this.durationMultiplier = durationMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.speedMultiplier = speedMultiplier;
    }

    public String effectId() {
        return effectId;
    }

    public int layers() {
        return layers;
    }

    public float radiusBase() {
        return radiusBase;
    }

    public float durationBase() {
        return durationBase;
    }

    public float radiusMultiplier() {
        return radiusMultiplier;
    }

    public float durationMultiplier() {
        return durationMultiplier;
    }

    public float damageMultiplier() {
        return damageMultiplier;
    }

    public float speedMultiplier() {
        return speedMultiplier;
    }

    public float radius() {
        return radiusBase * radiusMultiplier;
    }

    public int durationSeconds() {
        return Math.max(1, Math.round(durationBase * durationMultiplier));
    }
}
