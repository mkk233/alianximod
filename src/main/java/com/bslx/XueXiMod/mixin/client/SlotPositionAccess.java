package com.bslx.XueXiMod.mixin.client;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 只改客户端已有槽位的坐标，不替换槽位对象。 */
@Mixin(Slot.class)
public interface SlotPositionAccess {
    @Mutable
    @Accessor("x")
    void xuexi$setX(int x);

    @Mutable
    @Accessor("y")
    void xuexi$setY(int y);
}
