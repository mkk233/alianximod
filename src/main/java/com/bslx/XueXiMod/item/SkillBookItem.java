package com.bslx.XueXiMod.item;

import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.skill.SkillType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 技能书物品。剑术书记载剑技，法术书记载法技。
 * 玩家右键只阅读书上的技；当前使用路径不再绑定武器。
 * 不负责装配、释放或命中结算。
 */
public class SkillBookItem extends Item {
    private final SkillType skillType;

    public SkillBookItem(Properties properties, SkillType skillType) {
        super(properties);
        this.skillType = skillType;
    }

    public SkillType getSkillType() {
        return skillType;
    }

    /**
     * 将技数据存储到物品NBT
     */
    public static void setSkill(ItemStack stack, Skill skill) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.put("SkillData", skill.toNbt());
    }

    /**
     * 从物品NBT读取技数据
     */
    @Nullable
    public static Skill getSkill(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("SkillData")) {
            return Skill.fromNbt(tag.getCompound("SkillData"));
        }
        return null;
    }

    /**
     * 把书上的技写进武器指定槽。当前右键阅读不会走到这里。
     * @param book 技能书
     * @param weapon 武器
     * @param slot 槽位 0-3
     * @return 是否成功
     */
    public static boolean bindBookToWeapon(ItemStack book, ItemStack weapon, int slot) {
        if (weapon.isEmpty() || book.isEmpty()) return false;
        if (!(book.getItem() instanceof SkillBookItem bookItem)) return false;

        Skill skill = getSkill(book);
        if (skill == null) return false;

        // 检查类型兼容性
        if (bookItem.getSkillType() == SkillType.SWORD) {
            // 剑术书只能绑定到近战武器
            if (weapon.getItem() instanceof BowItem || weapon.getItem() instanceof CrossbowItem) {
                return false;
            }
        }

        // 写入武器NBT
        CompoundTag tag = weapon.getOrCreateTag();
        tag.put("BoundSkill_" + slot, skill.toNbt());
        tag.putBoolean("HasBoundSkills", true);

        return true;
    }

    /** 有技时在服务端列出名称、类型、组成和展示数值，并说明装配与释放尚未开放。 */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Skill skill = getSkill(stack);

        if (skill != null) {
            // 普通右键：显示详情（绑定武器已停用，装配阶段再做）
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("§6=== " + skill.getName() + " ==="), false);
                player.displayClientMessage(Component.literal("§7类型: " + (skill.getType() == SkillType.SWORD ? "§c剑术" : "§9术法")), false);
                player.displayClientMessage(Component.literal("§7组成术:"), false);
                skill.getTechniques().forEach(technique ->
                    player.displayClientMessage(Component.literal("  §f- " + technique.getName() + " §7(" + technique.getType() + ")"), false)
                );
                player.displayClientMessage(Component.literal("§7总伤害: §c" + skill.displayedTotalDamage()), false);
                player.displayClientMessage(Component.literal("§7总冷却: §b" + skill.displayedTotalCooldown() + "t"), false);
                player.displayClientMessage(Component.literal("§7装配与释放尚未开放"), false);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        return InteractionResultHolder.pass(stack);
    }

    /** 有技时列出组成和阅读提示；空白书只说明可用命令写入。 */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        Skill skill = getSkill(stack);
        if (skill != null) {
            tooltip.add(Component.literal("§6" + skill.getName()));
            tooltip.add(Component.literal("§7类型: " + (skill.getType() == SkillType.SWORD ? "§c剑术" : "§9术法")));
            tooltip.add(Component.literal("§7组成术:"));
            skill.getTechniques().forEach(technique ->
                tooltip.add(Component.literal("  §f- " + technique.getName()))
            );
            tooltip.add(Component.literal("§7总伤害: §c" + skill.displayedTotalDamage()));
            tooltip.add(Component.literal("§7总冷却: §b" + skill.displayedTotalCooldown() + "t"));
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("§e右键阅读"));
        } else {
            tooltip.add(Component.literal("§7空白的" + (skillType == SkillType.SWORD ? "剑术书" : "术法集")));
            tooltip.add(Component.literal("§7可通过命令写入技数据"));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
