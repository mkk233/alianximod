package com.bslx.XueXiMod.shu.technique.release.deflection;

import net.minecraft.world.phys.Vec3;

/** 捕获路线上的一个世界位置。速度幅值留在弹体，不写进宿主。 */
public record RouteSample(Vec3 position, boolean finished) {
    public RouteSample {
        if (position == null) {
            throw new IllegalArgumentException("路线位置不能空");
        }
    }
}
