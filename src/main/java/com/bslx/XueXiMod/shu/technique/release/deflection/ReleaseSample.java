package com.bslx.XueXiMod.shu.technique.release.deflection;

import net.minecraft.world.phys.Vec3;

/** 紧急或正常放出时的世界位置和单位方向。速度幅值仍由弹体自己持有。 */
public record ReleaseSample(Vec3 position, Vec3 unitDirection) {
    public ReleaseSample {
        if (position == null || unitDirection == null) {
            throw new IllegalArgumentException("放出样本不完整");
        }
    }
}
