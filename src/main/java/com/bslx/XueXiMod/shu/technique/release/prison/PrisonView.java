package com.bslx.XueXiMod.shu.technique.release.prison;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 服务端水牢快照。bounds 是创建时目标真实包围盒外扩后的壳，身份只用 UUID。
 * 同步用的网络编号不在这里。
 */
public record PrisonView(
        UUID prisonId,
        long revision,
        UUID casterId,
        UUID targetId,
        ResourceKey<Level> dimension,
        Vec3 anchor,
        AABB bounds,
        int lockedTier,
        long birthTick,
        long endTick) {
}
