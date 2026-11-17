package com.bslx.XueXiMod.shu.technique.release.statusquery;

import net.minecraft.resources.ResourceLocation;

/** 一次查询读到的资源快照。数值、档位和剩余时间都只描述当时，不写回实体。 */
public record ResourceView(ResourceLocation identifier, int value, int tier, int durationTicks) {
    public ResourceView {
        if (identifier == null) {
            throw new IllegalArgumentException("identifier");
        }
    }
}
