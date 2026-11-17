package com.bslx.XueXiMod.shu.technique.release.clone.capture;

import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * 部署当时已经算好的载荷输入。没有玩家，也没有活主手。
 * 武器在构造时再复制一次，断潮报价不要求它是剑。
 */
public record CaptureQuoteContext(
        Technique payload,
        ItemStack weaponCopy,
        float weaponBonus,
        @Nullable Double attributeDamageBonus,
        ShuAttribute embryo,
        long structureEpoch) {

    public CaptureQuoteContext {
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(weaponCopy, "weaponCopy");
        Objects.requireNonNull(embryo, "embryo");
        weaponCopy = weaponCopy.copy();
    }
}
