package com.bslx.XueXiMod.status.wood.resource;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 一次发布里的一根地面资源。寿命是该维度游戏时间的正 tick 数，没有 6 秒默认。
 * 坐标是世界坐标。调用时目标区块必须已经加载。
 */
public record GroundLotSpec(
        FlowerKind flower,
        int effectiveUnits,
        int potentialUnits,
        long lifeTicks,
        ResourceKey<Level> dimension,
        Vec3 position) {
}
