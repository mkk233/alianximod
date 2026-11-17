package com.bslx.XueXiMod.status.fire.seed;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.network.FireSeedVisualPacket;
import com.bslx.XueXiMod.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * 把已经入账的火种余额告诉正在看这个实体的客户端。
 * 不扫描世界，也不在每拍重发全部实体。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class FireSeedVisualRelay {
    private FireSeedVisualRelay() {
    }

    /** 余额已经写入后，发给正在追踪这个实体的客户端和实体自己。 */
    public static void publish(LivingEntity living, int balance) {
        if (living == null || living.getUUID() == null || living.level() == null || living.level().isClientSide()) {
            return;
        }
        if (living.getId() <= 0 || !living.isAddedToWorld()) {
            return;
        }
        int shown = FireSeedMath.clamp(balance);
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> living),
                new FireSeedVisualPacket(living.getId(), living.getUUID(), shown));
    }

    /** 玩家登录时补发自己的火种显示。 */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            offer(player, player);
        }
    }

    /** 重生后补发自己的火种显示。 */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            offer(player, player);
        }
    }

    /** 换维度后补发自己的火种显示。 */
    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            offer(player, player);
        }
    }

    /** 开始看见一个生物时，把该生物当前火种发给观察者。 */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer watcher)) {
            return;
        }
        if (!(event.getTarget() instanceof LivingEntity living) || living.level().isClientSide()) {
            return;
        }
        offer(watcher, living);
    }

    /** 开始看这个实体时发当前余额。0 也发，离开视野期间错过的扣除才能清掉。 */
    private static void offer(ServerPlayer watcher, LivingEntity living) {
        if (watcher == null || living == null || living.getUUID() == null || living.getId() <= 0) {
            return;
        }
        int balance = FireSeedState.value(living);
        ModNetwork.sendToPlayer(watcher, new FireSeedVisualPacket(living.getId(), living.getUUID(), balance));
    }
}
