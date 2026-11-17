package com.bslx.XueXiMod.item;

import com.bslx.XueXiMod.player.PlayerSpellStorage;
import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.SkillStoneData;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 统一术式物品（代码 ID 仍为 skill_stone）。NBT 为 spell_id + embryo_attribute + snapshot。
 * 这一期不能释放。
 */
public class SkillStoneItem extends Item {
    public static final String DATA_KEY = "SkillStone";

    public SkillStoneItem(Properties properties) {
        super(properties);
    }

    public static void setData(ItemStack stack, SkillStoneData data) {
        stack.getOrCreateTag().put(DATA_KEY, data.toNbt());
    }

    @Nullable
    public static SkillStoneData getData(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(DATA_KEY)) return null;
        return SkillStoneData.fromNbt(tag.getCompound(DATA_KEY));
    }

    public static ItemStack create(SkillStoneData data) {
        ItemStack stack = new ItemStack(ModItems.skill_stone.get());
        setData(stack, data);
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        SkillStoneData data = getData(stack);
        if (data == null) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            if (!PlayerSpellStorage.insertOne(player, stack)) {
                player.displayClientMessage(Component.literal("§c术式收纳已满"), true);
                return InteractionResultHolder.fail(stack);
            }
            stack.shrink(1);
            player.displayClientMessage(Component.literal("§a已收入术式匣"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        SkillStoneData data = getData(stack);
        if (data != null) {
            Technique technique = data.getTechnique();
            String name = technique != null ? technique.getName() : data.getSpellId();
            return Component.translatable("item.xueximod.skill_stone.named", name);
        }
        return Component.translatable("item.xueximod.skill_stone");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        SkillStoneData data = getData(stack);
        if (data == null) {
            tooltip.add(Component.translatable("tooltip.xueximod.skill_stone.empty"));
            super.appendHoverText(stack, level, tooltip, flag);
            return;
        }

        Technique technique = data.getTechnique();
        if (technique != null) {
            tooltip.add(Component.literal("§7类型: " + typeName(technique.getType())));
            tooltip.add(Component.translatable("tooltip.xueximod.skill_stone.spell_attribute",
                    Component.translatable(technique.getAttribute().getTranslationKey())));
            tooltip.add(Component.literal("§7复杂度: " + technique.getComplexity()));
        }
        ShuAttribute embryo = data.getEmbryoAttribute();
        tooltip.add(Component.translatable("tooltip.xueximod.skill_stone.material",
                Component.translatable(embryo != null
                        ? embryo.getTranslationKey()
                        : ShuAttribute.NONE.getTranslationKey())));
        ShuAttribute spell = technique != null ? technique.getAttribute() : ShuAttribute.NONE;
        tooltip.add(Component.literal(ElementReaction.of(
                embryo != null ? embryo : ShuAttribute.NONE,
                spell != null ? spell : ShuAttribute.NONE).describe()));
        tooltip.add(Component.literal("§7伤害: §c" + format(data.displayedDamage())
                + " §7冷却: §b" + data.displayedCooldown() + "t"));
        tooltip.add(Component.translatable("tooltip.xueximod.skill_stone.unused"));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    private static String typeName(TechniqueType type) {
        return switch (type) {
            case SWORD -> "§c剑术";
            case MAGIC -> "§9术法";
            case MOVEMENT -> "§a动作";
            case UNIVERSAL -> "§e通用";
        };
    }

    private static String format(float value) {
        if (value == (int) value) return String.valueOf((int) value);
        return String.format("%.1f", value);
    }
}
