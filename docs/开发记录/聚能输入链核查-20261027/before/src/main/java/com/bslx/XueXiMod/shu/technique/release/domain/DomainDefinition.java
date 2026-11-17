package com.bslx.XueXiMod.shu.technique.release.domain;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** 一份领域声明。盒子尺寸由各术式传入，公共代码不写死雾雨。 */
public final class DomainDefinition {
    private final Placer placer;
    private final int initialCost;
    private final int upkeepCost;
    private final int upkeepPeriod;
    private final int lifetimeTicks;
    private final DomainNativePolicy nativePolicy;
    private final DomainAuraSpec auraSpec;
    private final boolean learnsWhenReady;
    private final DomainBoundary boundary;
    private final DomainVisual visual;

    public DomainDefinition(Placer placer, int initialCost, int upkeepCost, int upkeepPeriod, int lifetimeTicks,
                            DomainNativePolicy nativePolicy, DomainAuraSpec auraSpec, boolean learnsWhenReady) {
        this(placer, initialCost, upkeepCost, upkeepPeriod, lifetimeTicks, nativePolicy, auraSpec, learnsWhenReady,
                DomainBoundary.SEAL, DomainVisual.VOLUME);
    }

    public DomainDefinition(Placer placer, int initialCost, int upkeepCost, int upkeepPeriod, int lifetimeTicks,
                            DomainNativePolicy nativePolicy, DomainAuraSpec auraSpec, boolean learnsWhenReady,
                            DomainBoundary boundary, DomainVisual visual) {
        this.placer = placer;
        this.initialCost = initialCost;
        this.upkeepCost = upkeepCost;
        this.upkeepPeriod = upkeepPeriod;
        this.lifetimeTicks = lifetimeTicks;
        this.nativePolicy = nativePolicy;
        this.auraSpec = auraSpec;
        this.learnsWhenReady = learnsWhenReady;
        this.boundary = boundary == null ? DomainBoundary.SEAL : boundary;
        this.visual = visual == null ? DomainVisual.VOLUME : visual;
    }

    public static DomainDefinition mistRain() {
        return new DomainDefinition(
                player -> AxisBoxGeometry.atFeet(player.position(), 7.0, 7.0, 8.0),
                40, 8, 20, 200,
                new DomainNativePolicy("mist_rain", 10, 2, 10, 20, 2, 20),
                DomainAuraSpec.tideNeutral(),
                true);
    }

    public DomainGeometry place(ServerPlayer player) {
        return placer.place(player);
    }

    public int initialCost() {
        return initialCost;
    }

    public int upkeepCost() {
        return upkeepCost;
    }

    public int upkeepPeriod() {
        return upkeepPeriod;
    }

    public int lifetimeTicks() {
        return lifetimeTicks;
    }

    public DomainNativePolicy nativePolicy() {
        return nativePolicy;
    }

    public DomainAuraSpec auraSpec() {
        return auraSpec;
    }

    public boolean learnsWhenReady() {
        return learnsWhenReady;
    }

    public DomainBoundary boundary() {
        return boundary;
    }

    public DomainVisual visual() {
        return visual;
    }

    /** 7×7×3，六面开放。减速、潮湿和加倍耗氧，不产原生水势，也不封人。 */
    public static DomainDefinition deepPressure() {
        return new DomainDefinition(
                player -> AxisBoxGeometry.atFeet(player.position(), 7.0, 7.0, 3.0),
                40, 8, 20, 200,
                new DomainNativePolicy("deep_pressure", 10, 4, 10, 20, 0, 20, 0, 20, 10, 10, 2f),
                DomainAuraSpec.tideNeutral(), true,
                DomainBoundary.OPEN, DomainVisual.PRESSURE);
    }

    @FunctionalInterface
    public interface Placer {
        DomainGeometry place(ServerPlayer player);
    }

    public static DomainGeometry feetBox(Vec3 feet, double width, double depth, double height) {
        return AxisBoxGeometry.atFeet(feet, width, depth, height);
    }
}
