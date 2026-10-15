package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.item.StaffItem;
import com.bslx.XueXiMod.network.ShuSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public class MagicHudOverlay {
    public static void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        ItemStack main = mc.player.getMainHandItem();
        int current = ShuSyncPacket.ClientShuCache.getMagic() + StaffItem.storedMana(main);
        int max = ShuSyncPacket.ClientShuCache.getMagicMax();
        if (StaffItem.holds(main)) {
            max += ((StaffItem) main.getItem()).data().maxManaBonus();
        }
        max = Math.max(1, max);
        guiGraphics.drawString(gui.getFont(), "魔力 " + current + "/" + max, screenWidth / 2 - 91, screenHeight - 48, 0xFFFFFF);
    }
}
