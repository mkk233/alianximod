package com.bslx.XueXiMod.shu.technique.release.barrier;

/** 固定薄墙的声明。水幕用它，不再走球形场地。 */
public final class BarrierDefinition {
    private final double width;
    private final double height;
    private final double thickness;
    private final double forward;
    private final int lifetimeTicks;
    private final int spawnCost;
    private final int upkeepCost;
    private final int upkeepPeriod;
    private final float attenuation;
    private final boolean deflectsProjectiles;

    public BarrierDefinition(double width, double height, double thickness, double forward,
                             int lifetimeTicks, int spawnCost, int upkeepCost, int upkeepPeriod, float attenuation) {
        this(width, height, thickness, forward, lifetimeTicks, spawnCost, upkeepCost, upkeepPeriod, attenuation, false);
    }

    public BarrierDefinition(double width, double height, double thickness, double forward,
                             int lifetimeTicks, int spawnCost, int upkeepCost, int upkeepPeriod, float attenuation,
                             boolean deflectsProjectiles) {
        this.width = width;
        this.height = height;
        this.thickness = thickness;
        this.forward = forward;
        this.lifetimeTicks = lifetimeTicks;
        this.spawnCost = spawnCost;
        this.upkeepCost = upkeepCost;
        this.upkeepPeriod = upkeepPeriod;
        this.attenuation = attenuation;
        this.deflectsProjectiles = deflectsProjectiles;
    }

    /** 水幕显式承接来袭弹。其它薄墙构造保持关闭。 */
    public static BarrierDefinition curtain() {
        return new BarrierDefinition(3.2, 2.8, 0.25, 2.0, 80, 8, 2, 20, 0.5f, true);
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    public double thickness() {
        return thickness;
    }

    public double forward() {
        return forward;
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

    public float attenuation() {
        return attenuation;
    }

    public boolean deflectsProjectiles() {
        return deflectsProjectiles;
    }
}
