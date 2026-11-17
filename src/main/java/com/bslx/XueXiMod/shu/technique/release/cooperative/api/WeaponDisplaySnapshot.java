package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * 只读武器显示快照。构造时复制物品标识和有界 NBT，不保留主手引用。
 * 超限直接拒绝，不截短后假装已经保存。
 */
public final class WeaponDisplaySnapshot {
    public static final WeaponDisplaySnapshot EMPTY = new WeaponDisplaySnapshot(
            new ResourceLocation("minecraft", "air"), 0, null, true);

    private final ResourceLocation itemId;
    private final int count;
    private final @Nullable CompoundTag frozenTag;

    private WeaponDisplaySnapshot(ResourceLocation itemId, int count, @Nullable CompoundTag frozenTag,
                                  boolean trusted) {
        if (itemId == null) {
            throw new IllegalArgumentException("武器快照缺少物品标识");
        }
        if (count < 0 || count > 64) {
            throw new IllegalArgumentException("武器快照数量超出显示范围");
        }
        if (count == 0 && !"air".equals(itemId.getPath())) {
            throw new IllegalArgumentException("空武器快照只能是空气");
        }
        this.itemId = itemId;
        this.count = count;
        if (frozenTag == null) {
            this.frozenTag = null;
        } else if (trusted) {
            this.frozenTag = frozenTag;
        } else {
            this.frozenTag = copyBounded(frozenTag);
        }
    }

    public WeaponDisplaySnapshot(ResourceLocation itemId, int count, @Nullable CompoundTag frozenTag) {
        this(itemId, count, frozenTag, false);
    }

    public static WeaponDisplaySnapshot capture(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return EMPTY;
        }
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) {
            throw new IllegalArgumentException("武器快照缺少物品标识");
        }
        CompoundTag frozenTag = stack.getTag() == null ? null : stack.getTag().copy();
        return new WeaponDisplaySnapshot(itemId, stack.getCount(), frozenTag, false);
    }

    public ResourceLocation itemId() {
        return itemId;
    }

    public int count() {
        return count;
    }

    public @Nullable CompoundTag frozenTag() {
        return frozenTag == null ? null : frozenTag.copy();
    }

    private static CompoundTag copyBounded(CompoundTag source) {
        if (measure(source, 1) > CooperativeStructureLimits.WEAPON_TAG_ENTRY_CAP) {
            throw new IllegalArgumentException("武器显示数据超出结构上限");
        }
        return source.copy();
    }

    private static int measure(Tag tag, int depth) {
        if (tag == null || depth > CooperativeStructureLimits.WEAPON_TAG_DEPTH_CAP) {
            return Integer.MAX_VALUE;
        }
        if (tag instanceof StringTag text) {
            return text.getAsString().length() > CooperativeStructureLimits.WEAPON_TAG_STRING_CAP
                    ? Integer.MAX_VALUE : 1;
        }
        if (tag instanceof CompoundTag compound) {
            int total = 0;
            for (String key : compound.getAllKeys()) {
                if (key.length() > CooperativeStructureLimits.WEAPON_TAG_STRING_CAP) {
                    return Integer.MAX_VALUE;
                }
                total += 1;
                total += measure(compound.get(key), depth + 1);
                if (total > CooperativeStructureLimits.WEAPON_TAG_ENTRY_CAP) {
                    return Integer.MAX_VALUE;
                }
            }
            return total;
        }
        if (tag instanceof ListTag listed) {
            int total = 0;
            for (Tag element : listed) {
                total += 1;
                total += measure(element, depth + 1);
                if (total > CooperativeStructureLimits.WEAPON_TAG_ENTRY_CAP) {
                    return Integer.MAX_VALUE;
                }
            }
            return total;
        }
        return 1;
    }
}
