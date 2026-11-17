package com.bslx.XueXiMod.shu.technique.release.capture;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 服务端这一次释放已经冻结的输入。视线和潜行不读客户端包。
 */
public record CapturedInput(
        boolean ownerShift,
        Vec3 feetPosition,
        Vec3 eyePosition,
        Vec3 lookUnit,
        float weaponBonus,
        @Nullable Double attributeDamageBonus,
        double cooldownReduction,
        ItemStack weaponCopy,
        int slotIndex,
        boolean staffCarrier,
        int serverGameTick) {

    public CapturedInput {
        if (feetPosition == null || eyePosition == null || lookUnit == null || weaponCopy == null) {
            throw new IllegalArgumentException("捕获输入不完整");
        }
        weaponCopy = weaponCopy.copy();
    }
}
