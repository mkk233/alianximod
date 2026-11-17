package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.item.StaffItem;
import com.bslx.XueXiMod.network.ShuSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public class MagicHudOverlay {
    public static void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        ItemStack main = minecraft.player.getMainHandItem();
        int current = ShuSyncPacket.ClientShuCache.getMagic() + StaffItem.storedMana(main);
        int upperBound = ShuSyncPacket.ClientShuCache.getMagicMax();
        if (StaffItem.holds(main)) {
            upperBound += ((StaffItem) main.getItem()).data().maxManaBonus();
        }
        upperBound = Math.max(1, upperBound);
        ResourceBarLayout.drawMana(guiGraphics, gui.getFont(), current, upperBound, current + "/" + upperBound, 0xFFFFFF);
    }
}
