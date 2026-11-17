package com.bslx.XueXiMod.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * 可保存、可碰撞的陨石方块。硬度 3、抗爆 6，石镐及以上才掉落自身。
 * 没有随机刻、没有原版火，也不登记燃料或矿物用途。
 */
public class MeteoriteBlock extends Block {
    public MeteoriteBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .strength(3.0F, 6.0F)
                .sound(SoundType.BASALT)
                .requiresCorrectToolForDrops());
    }
}
