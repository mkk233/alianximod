package com.bslx.XueXiMod.event;

import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.skill.SkillType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

public class BoundSkillTooltip {

    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        CompoundTag tag = stack.getTag();

        if (tag == null || !tag.getBoolean("HasBoundSkills")) return;

        // 添加分隔线
        event.getToolTip().add(Component.literal(""));

        // 标题
        event.getToolTip().add(Component.literal("§6§l【已绑定技】"));

        // 遍历4个槽位
        for (int i = 0; i < 4; i++) {
            String key = "BoundSkill_" + i;
            if (tag.contains(key)) {
                Skill skill = Skill.fromNbt(tag.getCompound(key));
                if (skill != null) {
                    // 获取按键名称
                    String keyName = getKeyDisplayName(i);

                    // 技名称和类型
                    String typeIcon = skill.getType() == SkillType.SWORD ? "§c⚔" : "§9✦";
                    event.getToolTip().add(Component.literal(
                        "§7" + keyName + " " + typeIcon + " §f" + skill.getName()
                    ));

                    // 组成术（简化显示）
                    StringBuilder techs = new StringBuilder("§7  术: ");
                    for (int j = 0; j < skill.getTechniques().size(); j++) {
                        if (j > 0) techs.append(" + ");
                        techs.append(skill.getTechniques().get(j).getName());
                    }
                    event.getToolTip().add(Component.literal(techs.toString()));

                    // 属性
                    event.getToolTip().add(Component.literal(
                        "§7  伤害:§c" + skill.getTotalBaseDamage() +
                        " §7冷却:§b" + skill.getTotalBaseCooldown() + "t"
                    ));
                }
            }
        }

        // 使用提示
        event.getToolTip().add(Component.literal(""));
        event.getToolTip().add(Component.literal("§e右键/C/V/R 释放技"));
    }

    /**
     * 获取槽位对应的按键显示名称
     */
    private static String getKeyDisplayName(int slot) {
        return switch (slot) {
            case 0 -> "[右键]";
            case 1 -> "[C]";
            case 2 -> "[V]";
            case 3 -> "[R]";
            default -> "[?]";
        };
    }
}
