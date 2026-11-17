package com.bslx.XueXiMod.shu.technique.enchant.resource;

import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.technique.EnchantmentAccumulator;
import com.bslx.XueXiMod.shu.technique.EnchantmentDefinition;

/**
 * 新资源附魔定义。magicDamage 固定为 0，避免沿用旧收费入口时附带 2 点直接伤害。
 * 折叠进共享结算时伤害和速度倍率保持 1，不改旧五元素的乘区。
 */
public final class ResourceEnchantDefinition implements EnchantmentDefinition {
    private final String enchantmentId;
    private final ShuAttribute element;
    private final ResourceEnchantSlice slice;

    public ResourceEnchantDefinition(String enchantmentId, ShuAttribute element, ResourceEnchantSlice slice) {
        this.enchantmentId = enchantmentId;
        this.element = element;
        this.slice = slice;
    }

    @Override
    public String id() {
        return this.enchantmentId;
    }

    @Override
    public ShuAttribute element() {
        return this.element;
    }

    public ResourceEnchantSlice slice() {
        return this.slice;
    }

    public int manaPrice() {
        return this.slice.manaPrice();
    }

    @Override
    public int magicDamage(ElementReaction reaction) {
        return ResourceEnchantNumbers.DIRECT_DAMAGE;
    }

    @Override
    public String attackPayloadId() {
        return this.slice.hasGloom() ? this.enchantmentId : "";
    }

    @Override
    public EnchantmentAccumulator createAccumulator() {
        return new EnchantmentAccumulator(this.enchantmentId, 0, 0f, 0f, 1f, 1f, 1f, 1f);
    }

    @Override
    public EnchantmentAccumulator fold(ElementReaction reaction, EnchantmentAccumulator current) {
        EnchantmentAccumulator base = current != null ? current : createAccumulator();
        return new EnchantmentAccumulator(base.effectId(), base.layers() + 1, base.radiusBase(), base.durationBase(),
                1f, 1f, 1f, 1f);
    }
}
