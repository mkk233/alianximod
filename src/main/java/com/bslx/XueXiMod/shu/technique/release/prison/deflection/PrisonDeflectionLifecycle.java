package com.bslx.XueXiMod.shu.technique.release.prison.deflection;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.network.prison.PrisonSyncPacket;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHosts;
import com.bslx.XueXiMod.shu.technique.release.deflection.HostKey;
import com.bslx.XueXiMod.shu.technique.release.prison.EndReason;
import com.bslx.XueXiMod.shu.technique.release.prison.PrisonEvents;
import com.bslx.XueXiMod.shu.technique.release.prison.PrisonService;
import com.bslx.XueXiMod.shu.technique.release.prison.PrisonView;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 服务端启动时登记一次水牢监听，并把开始追踪的快照补给观察者。
 * 不引用客户端类型。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class PrisonDeflectionLifecycle {
    private static final AtomicBoolean LISTENING = new AtomicBoolean();
    private static final PrisonEvents.Listener LISTENER = new Bridge();

    private PrisonDeflectionLifecycle() {
    }

    /** 每个服务端只登记一次水牢创建和结束桥。 */
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        if (!LISTENING.compareAndSet(false, true)) {
            return;
        }
        PrisonEvents.register(LISTENER);
    }

    /** 观察者开始追踪被困者时补一次牢快照。查询失败不影响牢。 */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer watcher) || event.getTarget() == null) {
            return;
        }
        Optional<PrisonView> found;
        try {
            found = PrisonService.view(event.getTarget().getUUID());
        } catch (RuntimeException failure) {
            return;
        }
        if (found == null || found.isEmpty()) {
            return;
        }
        PrisonSyncPacket.sendSnapshot(watcher, found.get(), event.getTarget().getId());
    }

    private static final class Bridge implements PrisonEvents.Listener {
        @Override
        public void created(PrisonView view) {
            if (view == null || view.prisonId() == null) {
                return;
            }
            try {
                DeflectionHosts.register(new PrisonDeflectionHost(view));
            } catch (RuntimeException failure) {
                // 宿主登记失败不能把已经建好的牢回滚。
            }
            try {
                PrisonSyncPacket.sendCreated(view);
            } catch (RuntimeException failure) {
                // 同步失败留给追踪开始时补快照。
            }
        }

        @Override
        public void ended(PrisonView view, EndReason reason) {
            if (view == null || view.prisonId() == null) {
                return;
            }
            try {
                DeflectionHosts.unregister(new HostKey(view.prisonId(), view.revision()));
            } catch (RuntimeException failure) {
                // 注销失败时宿主自己的 active 也会在到期后拒绝接触。
            }
            try {
                PrisonSyncPacket.sendRemove(view);
            } catch (RuntimeException failure) {
                // 客户端仍按 endTick 和换维清掉残影。
            }
        }
    }
}
