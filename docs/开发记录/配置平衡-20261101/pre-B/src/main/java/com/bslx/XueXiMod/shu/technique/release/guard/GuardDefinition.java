package com.bslx.XueXiMod.shu.technique.release.guard;

import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;

/**
 * 随身护体的参数。没有声明护体的术式保持原样。
 * 类型键用来认同一类护体，公共入口不按术式编号分支。
 */
public final class GuardDefinition {
    private final String typeKey;
    private final int lifetimeTicks;
    private final int spawnCost;
    private final int upkeepCost;
    private final int upkeepPeriod;
    private final float unloadRatio;
    private final double radius;
    private final double band;
    private final double bodyHeight;
    private final float baseTilt;
    private final boolean deflectsProjectiles;

    public GuardDefinition(String typeKey, int lifetimeTicks, int spawnCost, int upkeepCost, int upkeepPeriod,
                           float unloadRatio, double radius, double band, double bodyHeight, float baseTilt) {
        this(typeKey, lifetimeTicks, spawnCost, upkeepCost, upkeepPeriod, unloadRatio, radius, band, bodyHeight, baseTilt, false);
    }

    public GuardDefinition(String typeKey, int lifetimeTicks, int spawnCost, int upkeepCost, int upkeepPeriod,
                           float unloadRatio, double radius, double band, double bodyHeight, float baseTilt,
                           boolean deflectsProjectiles) {
        this.typeKey = typeKey;
        this.lifetimeTicks = lifetimeTicks;
        this.spawnCost = spawnCost;
        this.upkeepCost = upkeepCost;
        this.upkeepPeriod = upkeepPeriod;
        this.unloadRatio = unloadRatio;
        this.radius = radius;
        this.band = band;
        this.bodyHeight = bodyHeight;
        this.baseTilt = baseTilt;
        this.deflectsProjectiles = deflectsProjectiles;
    }

    /** 水环显式承接来袭弹。其它护体构造保持关闭。 */
    public static GuardDefinition personalRing() {
        return new GuardDefinition("personal_ring", 200, 8, 2, 20, 0.20f, 0.9, 0.12, 0.55, 0f, true);
    }

    public String typeKey() {
        return typeKey;
    }

    public int lifetimeTicks() {
        return lifetimeTicks;
    }

    public int spawnCost() {
        return spawnCost;
    }

    public int upkeepCost() {
        return upkeepCost;
    }

    public int upkeepPeriod() {
        return upkeepPeriod;
    }

    public float unloadRatio() {
        return unloadRatio;
    }

    public double radius() {
        return radius;
    }

    public double band() {
        return band;
    }

    public double bodyHeight() {
        return bodyHeight;
    }

    public float baseTilt() {
        return baseTilt;
    }

    public boolean deflectsProjectiles() {
        return deflectsProjectiles;
    }

    /** 新的一份参数。半径和带宽随链聚能放大，寿命只延长有限正值。原对象不改。 */
    public GuardDefinition withInput(ReleaseInput input) {
        ReleaseInput frozen = input == null ? ReleaseInput.BASE : input;
        double factor = frozen.sizeMultiplier();
        return new GuardDefinition(typeKey, frozen.scaleDuration(lifetimeTicks), spawnCost, upkeepCost, upkeepPeriod,
                unloadRatio, radius * factor, band * factor, bodyHeight, baseTilt, deflectsProjectiles);
    }
}
