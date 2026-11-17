package com.bslx.XueXiMod.shu.technique.chain;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.debug.DebugLog;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ChainScheduler {
    private static final ConcurrentLinkedQueue<Task> TASKS = new ConcurrentLinkedQueue<>();

    private ChainScheduler() {}

    public static void schedule(MinecraftServer server, int runAtTick, Runnable action) {
        if (runAtTick <= server.getTickCount()) {
            action.run();
            return;
        }
        TASKS.add(new Task(runAtTick, action));
    }

    @Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
    public static final class TickHandler {
        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            int now = event.getServer().getTickCount();
            Iterator<Task> iterator = TASKS.iterator();
            while (iterator.hasNext()) {
                Task task = iterator.next();
                if (task.runAtTick > now) {
                    continue;
                }
                iterator.remove();
                try {
                    task.action.run();
                } catch (Throwable throwable) {
                    DebugLog.log("链段执行异常：" + throwable);
                }
            }
        }
    }

    private record Task(int runAtTick, Runnable action) {}
}
