package com.bslx.XueXiMod.gui;

import com.bslx.XueXiMod.item.ShuEmbryoItem;
import com.bslx.XueXiMod.item.SkillStoneItem;
import com.bslx.XueXiMod.player.PlayerShuData;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.SkillStoneData;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ImprintActions {
    public static boolean imprint(Player imprintPlayer, String techniqueId, SimpleContainer crafts) {
        if (imprintPlayer.level().isClientSide) return false;
        ItemStack embryo = crafts.getItem(0);
        if (embryo.isEmpty() || !(embryo.getItem() instanceof ShuEmbryoItem)) {
            imprintPlayer.displayClientMessage(Component.literal("§c请先放入原胚"), true);
            return false;
        }
        if (!crafts.getItem(1).isEmpty()) {
            imprintPlayer.displayClientMessage(Component.literal("§c请先取走成品术式"), true);
            return false;
        }
        Technique technique = TechniqueManager.getById(techniqueId);
        if (technique == null) {
            imprintPlayer.displayClientMessage(Component.literal("§c未知的术"), true);
            return false;
        }
        PlayerShuData data = PlayerShuData.get(imprintPlayer);
        if (!data.hasLearned(techniqueId) || !data.isUnlocked(technique)) {
            imprintPlayer.displayClientMessage(Component.literal("§c尚未学会该术"), true);
            return false;
        }

        ShuAttribute embryoAttribute = ShuEmbryoItem.getAttribute(embryo);
        SkillStoneData stoneData = SkillStoneData.imprint(technique, embryoAttribute);
        ItemStack stone = SkillStoneItem.create(stoneData);
        crafts.setItem(1, stone);
        embryo.shrink(1);
        crafts.setChanged();
        imprintPlayer.displayClientMessage(
                Component.literal("§a已刻印「" + technique.getName() + "」"), true);
        return true;
    }
}
