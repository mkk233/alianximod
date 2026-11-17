package com.bslx.XueXiMod.shu.technique.release.statusquery;

import net.minecraft.resources.ResourceLocation;

/** 一次查询读到的药水快照。存在、等级和剩余时间都来自当时的实例。 */
public record EffectView(ResourceLocation identifier, boolean present, int amplifier, int durationTicks) {
    public EffectView {
        if (identifier == null) {
            throw new IllegalArgumentException("identifier");
        }
    }
}
