package com.bslx.XueXiMod.shu.technique.enchant.resource;

import com.bslx.XueXiMod.shu.technique.release.fire.ResourceEffectKeys;

/**
 * 一个新附魔的类型载荷。魔力价格、直接伤害、产种、燃烧、即时消费、轨迹、领域和地形彼此分开。
 */
public final class ResourceEnchantSlice {
    private final String enchantmentId;
    private final int manaPrice;
    private final int directDamage;
    private final int scaledSeedBase;
    private final boolean kindle;
    private final boolean detonate;
    private final boolean trail;
    private final boolean gloom;
    private final boolean breakGround;

    private ResourceEnchantSlice(String enchantmentId, int manaPrice, int directDamage, int scaledSeedBase,
                                 boolean kindle, boolean detonate, boolean trail, boolean gloom,
                                 boolean breakGround) {
        this.enchantmentId = enchantmentId;
        this.manaPrice = manaPrice;
        this.directDamage = directDamage;
        this.scaledSeedBase = scaledSeedBase;
        this.kindle = kindle;
        this.detonate = detonate;
        this.trail = trail;
        this.gloom = gloom;
        this.breakGround = breakGround;
    }

    public static ResourceEnchantSlice trail() {
        return mark(ResourceEffectKeys.TRAIL, 0, false, false, true, false, false);
    }

    public static ResourceEnchantSlice sow() {
        return mark(ResourceEffectKeys.SOW, ResourceEnchantNumbers.SOW_BASE, false, false, false, false, false);
    }

    public static ResourceEnchantSlice kindle() {
        return mark(ResourceEffectKeys.KINDLE, 0, true, false, false, false, false);
    }

    public static ResourceEnchantSlice detonate() {
        return mark(ResourceEffectKeys.DETONATE, 0, false, true, false, false, false);
    }

    public static ResourceEnchantSlice gloom() {
        return mark(ResourceEffectKeys.GLOOM, ResourceEnchantNumbers.GLOOM_BASE, false, false, false, true, false);
    }

    public static ResourceEnchantSlice breakGround() {
        return mark(ResourceEffectKeys.BREAK_GROUND, 0, false, false, false, false, true);
    }

    private static ResourceEnchantSlice mark(String enchantmentId, int scaledSeedBase, boolean kindle,
                                             boolean detonate, boolean trail, boolean gloom,
                                             boolean breakGround) {
        return new ResourceEnchantSlice(enchantmentId, ResourceEnchantNumbers.MANA_PRICE,
                ResourceEnchantNumbers.DIRECT_DAMAGE, scaledSeedBase, kindle, detonate, trail, gloom, breakGround);
    }

    public String enchantmentId() {
        return this.enchantmentId;
    }

    public int manaPrice() {
        return this.manaPrice;
    }

    public int directDamage() {
        return this.directDamage;
    }

    public int scaledSeedBase() {
        return this.scaledSeedBase;
    }

    public boolean hasKindle() {
        return this.kindle;
    }

    public boolean hasDetonate() {
        return this.detonate;
    }

    public boolean hasTrail() {
        return this.trail;
    }

    public boolean hasGloom() {
        return this.gloom;
    }

    public boolean hasBreakGround() {
        return this.breakGround;
    }
}
