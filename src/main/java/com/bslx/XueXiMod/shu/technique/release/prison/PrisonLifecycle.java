package com.bslx.XueXiMod.shu.technique.release.prison;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 水牢自己的生命周期。登出在存档事件里同步结算，不写进别的订阅者。 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class PrisonLifecycle {
    private PrisonLifecycle() {
    }

    /** 服务器拍末推进活动牢。 */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }
        PrisonRuntime.tick(event.getServer());
    }

    /** 玩家离线时结束其相关牢，并丢掉末地返回湿度快照。 */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PrisonRuntime.onLogout(player);
            PrisonWetTransfer.discard(player);
        }
    }

    /** 登入后套用尚未消费的末地返回湿度快照。 */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PrisonWetTransfer.apply(player);
        }
    }

    /** 非死亡复制玩家时套用末地返回湿度。普通死亡不走这里。 */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath() || !(event.getEntity() instanceof ServerPlayer fresh)) {
            return;
        }
        PrisonWetTransfer.apply(fresh);
    }

    /** 停服前结束仍在活动的牢并尝试结算湿度。 */
    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        PrisonRuntime.shutdown(event.getServer());
    }

    /** 玩家换维时结束其被困的牢和他施放的牢。 */
    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PrisonRuntime.onDimension(player);
        }
    }

    /** 生物重新进入服务端世界时，按编号重新绑定尚未到期的牢。 */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        PrisonRuntime.rebind(living);
    }
}
