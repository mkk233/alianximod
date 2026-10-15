package com.bslx.XueXiMod.item;

import com.bslx.XueXiMod.shu.ShuAttribute;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 统一原胚物品。属性存在 NBT Attribute 上，决定名称、贴图和堆叠分组。
 */
public class ShuEmbryoItem extends Item {
    public static final String ATTR_KEY = "Attribute";

    public ShuEmbryoItem(Properties properties) {
        super(properties);
    }

    public static ShuAttribute getAttribute(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ATTR_KEY)) return ShuAttribute.NONE;
        return ShuAttribute.fromId(tag.getString(ATTR_KEY));
    }

    public static void setAttribute(ItemStack stack, ShuAttribute attribute) {
        if (attribute == null || attribute == ShuAttribute.NONE) {
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                tag.remove(ATTR_KEY);
                if (tag.isEmpty()) stack.setTag(null);
            }
            return;
        }
        stack.getOrCreateTag().putString(ATTR_KEY, attribute.getId());
    }

    @Override
    public void verifyTagAfterLoad(CompoundTag tag) {
        super.verifyTagAfterLoad(tag);
        if (tag.contains(ATTR_KEY) && ShuAttribute.fromId(tag.getString(ATTR_KEY)) == ShuAttribute.NONE) {
            tag.remove(ATTR_KEY);
        }
    }

    public static ItemStack create(ShuAttribute attribute) {
        ItemStack stack = new ItemStack(ModItems.shu_embryo.get());
        setAttribute(stack, attribute);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.xueximod.shu_embryo." + getAttribute(stack).getId());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        ShuAttribute attribute = getAttribute(stack);
        tooltip.add(Component.translatable("tooltip.xueximod.embryo.material"));
        tooltip.add(Component.translatable("tooltip.xueximod.embryo.attribute",
                Component.translatable(attribute.getTranslationKey())));
        tooltip.add(Component.translatable("tooltip.xueximod.embryo.rule"));
        tooltip.add(Component.translatable("tooltip.xueximod.embryo.match"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
