package com.bslx.XueXiMod.shu.technique.release.phantom;

import net.minecraft.world.phys.Vec3;

/** 幻影契约。这轮只有数据，没有分身智能。 */
public interface PhantomSpec {
    Vec3 origin();

    Vec3 aim();

    int lifetimeTicks();
}
