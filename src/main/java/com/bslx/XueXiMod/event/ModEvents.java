package com.bslx.XueXiMod.event;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.client.EntityRendererFix;
import com.bslx.XueXiMod.network.BoundSkillSyncPacket;
import com.bslx.XueXiMod.network.CastCooldown;
import com.bslx.XueXiMod.network.CastExecutionSession;
import com.bslx.XueXiMod.network.GuardSyncPacket;
import com.bslx.XueXiMod.network.WaterMomentumNotice;
import com.bslx.XueXiMod.player.MagicPool;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHosts;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainPulses;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainService;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainSightStore;
import com.bslx.XueXiMod.shu.technique.sword.SwordEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 事件处理
 * - 玩家登录/登出：保存/加载术数据
 * - 客户端Tick：检测所有按键（右键/C/V/R）发送技使用包
 */
public class ModEvents {

    /**
     * 服务端：玩家登录时加载数据，给新玩家初始术
     */
    @Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
    public static class ServerEvents {
        @SubscribeEvent
        public static void onServerStarted(ServerStartedEvent event) {
            CastExecutionSession.cancelAll();
            CastCooldown.beginSession();
            DomainPulses.clear();
            DomainService.onServerStarted();
        }

        @SubscribeEvent
        public static void onServerStopping(ServerStoppingEvent event) {
            CastExecutionSession.cancelAll();
            DomainPulses.clear();
            CastCooldown.endSession();
            DeflectionHosts.onServerStopping();
        }

        /** 只丢掉这个维度里登记过的偏转宿主，不清领域、护体或水势账。 */
        @SubscribeEvent
        public static void onLevelUnload(LevelEvent.Unload event) {
            DeflectionHosts.onLevelUnload(event.getLevel());
        }

        @SubscribeEvent
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            PlayerConnectionHandler.onPlayerLogin(event);
        }

        @SubscribeEvent
        public static void onPlayerClone(PlayerEvent.Clone event) {
            PlayerConnectionHandler.onPlayerClone(event);
        }

        @SubscribeEvent
        public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
            PlayerConnectionHandler.onPlayerRespawn(event);
        }

        @SubscribeEvent
        public static void onTossSword(ItemTossEvent event) {
            if (!(event.getPlayer() instanceof ServerPlayer player)) {
                return;
            }
            if (!(event.getEntity().getItem().getItem() instanceof SwordItem)) {
                return;
            }
            event.setCanceled(true);
            ServerLevel level = player.serverLevel();
            level.addFreshEntity(SwordEntity.real(level, player, event.getEntity().getItem()));
            event.getEntity().discard();
        }

        @SubscribeEvent
        public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            PlayerConnectionHandler.onPlayerLogout(event);
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                MagicPool.forget(serverPlayer);
            }
        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            if (!(event.player instanceof ServerPlayer serverPlayer)) {
                return;
            }
            MagicPool.tickRegen(serverPlayer);
        }
    }

    /**
     * 客户端：按键检测
     * 右键(Use) / C / V / R 统一在tick中检测，边沿触发（按下瞬间）
     * M 键打开术管理GUI（无论是否持有武器）
     */
    @Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            EntityRendererFix.onClientTick(event);
            SkillHotkeyHandler.onClientTick(event);
        }

        @SubscribeEvent
        public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            BoundSkillSyncPacket.clearClient();
            WaterMomentumNotice.ClientNotice.clear();
            GuardSyncPacket.ClientRings.clear();
            DomainSightStore.clear();
        }

        @SubscribeEvent
        public static void onClientClone(ClientPlayerNetworkEvent.Clone event) {
            if (event.getOldPlayer() == null || event.getPlayer() == null
                    || event.getOldPlayer().level().dimension() != event.getPlayer().level().dimension()) {
                DomainSightStore.clear();
            }
        }
    }
}
