package com.bslx.XueXiMod.debug;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.gui.DebugLogScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, value = Dist.CLIENT)
public final class DebugLogCommands {
    private DebugLogCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("shulog")
                .executes(ctx -> open())
                .then(Commands.literal("on").executes(ctx -> {
                    DebugLog.setEnabled(true);
                    DebugLog.log("调试日志已打开");
                    return open();
                }))
                .then(Commands.literal("off").executes(ctx -> {
                    DebugLog.setEnabled(false);
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.displayClientMessage(Component.literal("调试日志已关闭"), false);
                    }
                    return 1;
                }))
                .then(Commands.literal("clear").executes(ctx -> {
                    DebugLog.clear();
                    return open();
                })));
    }

    private static int open() {
        DebugLog.setEnabled(true);
        Minecraft.getInstance().setScreen(new DebugLogScreen());
        return 1;
    }
}
