package com.bslx.XueXiMod.event;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 物品Tooltip事件处理
 * 为绑定技的武器显示已绑定的技信息
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public class ItemTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        BoundSkillTooltip.onItemTooltip(event);
    }
}
