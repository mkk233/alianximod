package com.bslx.XueXiMod.shu.technique.chain;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.debug.DebugLog;
import com.bslx.XueXiMod.debug.performance.PerfMetric;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/** 服务器刻队列。到期任务在服务器 tick 结束执行。不解释术式，也不保证动作本身成功。 */
public final class ChainScheduler {
    private static final ConcurrentLinkedQueue<Task> TASKS = new ConcurrentLinkedQueue<>();

    private ChainScheduler() {}

    /** 目标刻已到就立即执行；否则入队。此处只根据本文件可见调用。 */
    public static void schedule(MinecraftServer server, int runAtTick, Runnable action) {
        if (runAtTick <= server.getTickCount()) {
            long actionStart = PerformanceRecorder.begin();
            try {
                action.run();
            } finally {
                PerformanceRecorder.end(PerfMetric.SCHEDULED_ACTION, actionStart);
            }
            return;
        }
        TASKS.add(new Task(runAtTick, action));
    }

    @Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
    public static final class TickHandler {
        /** 只在 tick 结束取出到期任务。单个动作抛出的异常记日志后继续后续任务。 */
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
                    long actionStart = PerformanceRecorder.begin();
                    try {
                        task.action.run();
                    } finally {
                        PerformanceRecorder.end(PerfMetric.SCHEDULED_ACTION, actionStart);
                    }
                } catch (Throwable throwable) {
                    DebugLog.log("链段执行异常：" + throwable);
                }
            }
        }
    }

    private record Task(int runAtTick, Runnable action) {}
}
