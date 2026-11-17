package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.network.GuardSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 客户端世界卸载时清掉上一份世界的护体环。登出清理仍走原来的客户端登出事件。 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class GuardClientLifecycle {
    private GuardClientLifecycle() {
    }

    public static long clientGameTick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null) {
            return 0L;
        }
        return minecraft.level.getGameTime();
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        boolean clientSide = event.getLevel() != null && event.getLevel().isClientSide();
        clearIfClientSide(clientSide);
    }

    public static void clearIfClientSide(boolean clientSide) {
        if (!clientSide) {
            return;
        }
        GuardSyncPacket.ClientRings.clear();
    }
}
