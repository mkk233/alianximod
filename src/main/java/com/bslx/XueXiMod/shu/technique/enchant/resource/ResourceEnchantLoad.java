package com.bslx.XueXiMod.shu.technique.enchant.resource;

import java.util.List;

/** 一次结算里去重后的新附魔载荷。相同效果只留一份，魔力价格在收集时按格子另计。 */
public final class ResourceEnchantLoad {
    public static final ResourceEnchantLoad EMPTY = new ResourceEnchantLoad(0, 0, 0, false, false, false, false, false,
            List.of());

    private final int manaPrice;
    private final int directDamage;
    private final int scaledSeedBase;
    private final boolean kindle;
    private final boolean detonate;
    private final boolean trail;
    private final boolean gloom;
    private final boolean breakGround;
    private final List<ResourceEnchantSlice> slices;

    ResourceEnchantLoad(int manaPrice, int directDamage, int scaledSeedBase, boolean kindle, boolean detonate,
                        boolean trail, boolean gloom, boolean breakGround, List<ResourceEnchantSlice> slices) {
        this.manaPrice = manaPrice;
        this.directDamage = directDamage;
        this.scaledSeedBase = scaledSeedBase;
        this.kindle = kindle;
        this.detonate = detonate;
        this.trail = trail;
        this.gloom = gloom;
        this.breakGround = breakGround;
        this.slices = slices;
    }

    /** 领域周期只带能对实体重复施加的切片。名单为空时就是空载荷。 */
    public static ResourceEnchantLoad of(int manaPrice, int directDamage, int scaledSeedBase, boolean kindle,
                                          boolean detonate, List<ResourceEnchantSlice> slices) {
        return of(manaPrice, directDamage, scaledSeedBase, kindle, detonate, false, false, false, slices);
    }

    /** 保留轨迹、幽冥和崩地标记。名单为空时仍是空载荷，不把这些标记单独留下来。 */
    public static ResourceEnchantLoad of(int manaPrice, int directDamage, int scaledSeedBase, boolean kindle,
                                          boolean detonate, boolean trail, boolean gloom, boolean breakGround,
                                          List<ResourceEnchantSlice> slices) {
        if (slices == null || slices.isEmpty()) {
            return EMPTY;
        }
        return new ResourceEnchantLoad(manaPrice, directDamage, scaledSeedBase, kindle, detonate, trail, gloom,
                breakGround, List.copyOf(slices));
    }

    public boolean empty() {
        return this.slices.isEmpty();
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

    public boolean kindle() {
        return this.kindle;
    }

    public boolean detonate() {
        return this.detonate;
    }

    public boolean trail() {
        return this.trail;
    }

    public boolean gloom() {
        return this.gloom;
    }

    public boolean breakGround() {
        return this.breakGround;
    }

    public List<ResourceEnchantSlice> slices() {
        return this.slices;
    }
}
