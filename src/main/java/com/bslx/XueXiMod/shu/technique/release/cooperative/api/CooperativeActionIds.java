package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.resources.ResourceLocation;

/** 已冻结的动作资源标识。协同复用这些标识，并靠镜像和演员序号区分，不另建伤害动作。 */
public final class CooperativeActionIds {
    public static final ResourceLocation OBLIQUE_SLASH = new ResourceLocation(XueXiMod.MOD_ID, "oblique_slash");
    public static final ResourceLocation SPIN = new ResourceLocation(XueXiMod.MOD_ID, "spin");

    private CooperativeActionIds() {
    }
}
