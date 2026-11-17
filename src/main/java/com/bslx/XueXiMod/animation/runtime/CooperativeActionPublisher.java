package com.bslx.XueXiMod.animation.runtime;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.network.CooperativeActionS2CPacket;
import com.bslx.XueXiMod.network.ModNetwork;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneEntity;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ActionPlayback;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ActorActionPublisher;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeActionIds;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeStructureLimits;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.FamilyKey;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * 服务端动作端口。只保存每个演员最新的一条播放，并把开始、停止和跟踪快照发给附近客户端。
 * 回旋尚未结束时，单独斩击的姿态不能盖掉它。斩光不由这个端口提交。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class CooperativeActionPublisher implements ActorActionPublisher {
    public static final int TRACKING_BLOCKS = 128;
    private static final int CACHE_CAP = CooperativeStructureLimits.ACTIVE_FAMILY_CAP
            * CooperativeStructureLimits.ACTORS_PER_COOPERATION;
    private static final CooperativeActionPublisher SHARED = new CooperativeActionPublisher();
    private static final Map<UUID, ActionPlayback> ACTIVE = new LinkedHashMap<>();

    private CooperativeActionPublisher() {
    }

    public static ActorActionPublisher publisher() {
        return SHARED;
    }

    public static void sendTracking(ServerPlayer watcher) {
        SHARED.deliverTracking(watcher);
    }

    @Override
    public void start(ActionPlayback playback) {
        if (!accepted(playback)) {
            return;
        }
        ServerLevel level = levelOf(playback);
        if (level == null) {
            return;
        }
        long gameTime = level.getGameTime();
        ActionPlayback current = ACTIVE.get(playback.actorId());
        if (current != null
                && !finished(current, gameTime)
                && CooperativeActionIds.SPIN.equals(current.actionId())
                && CooperativeActionIds.OBLIQUE_SLASH.equals(playback.actionId())) {
            return;
        }
        ACTIVE.put(playback.actorId(), playback);
        trim();
        send(level, playback, CooperativeActionS2CPacket.start(playback));
    }

    @Override
    public void stop(FamilyKey family, UUID actorId, long sequence) {
        if (family == null || actorId == null || sequence < 1L) {
            return;
        }
        ActionPlayback current = ACTIVE.get(actorId);
        if (current == null || current.sequence() != sequence || !current.family().equals(family)) {
            return;
        }
        ACTIVE.remove(actorId);
        ServerLevel level = levelOf(current);
        if (level != null) {
            send(level, current, CooperativeActionS2CPacket.stop(family, actorId, sequence));
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendTracking(player);
        }
    }

    @SubscribeEvent
    public static void track(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer watcher) || event.getTarget() == null) {
            return;
        }
        ActionPlayback playback = ACTIVE.get(event.getTarget().getUUID());
        if (playback == null) {
            return;
        }
        ServerLevel level = watcher.serverLevel();
        if (level == null || finished(playback, level.getGameTime())) {
            return;
        }
        if (!playback.dimension().equals(level.dimension())) {
            return;
        }
        ModNetwork.sendToPlayer(watcher, CooperativeActionS2CPacket.tracking(playback));
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearActor(player.getUUID(), player.serverLevel());
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearActor(player.getUUID(), player.serverLevel());
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        ACTIVE.clear();
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            ACTIVE.clear();
            return;
        }
        Iterator<Map.Entry<UUID, ActionPlayback>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            ActionPlayback playback = iterator.next().getValue();
            ServerLevel level = server.getLevel(playback.dimension());
            if (level == null || finished(playback, level.getGameTime()) || retired(level, playback)) {
                iterator.remove();
                if (level != null) {
                    send(level, playback, CooperativeActionS2CPacket.stop(playback.family(), playback.actorId(), playback.sequence()));
                }
            }
        }
    }

    private void deliverTracking(ServerPlayer watcher) {
        if (watcher == null || watcher.serverLevel() == null) {
            return;
        }
        long gameTime = watcher.serverLevel().getGameTime();
        for (ActionPlayback playback : ACTIVE.values()) {
            if (!playback.dimension().equals(watcher.serverLevel().dimension()) || finished(playback, gameTime)) {
                continue;
            }
            Entity actor = watcher.serverLevel().getEntity(playback.entityId());
            if (actor != null && watcher.distanceToSqr(actor) <= (double) TRACKING_BLOCKS * TRACKING_BLOCKS) {
                ModNetwork.sendToPlayer(watcher, CooperativeActionS2CPacket.tracking(playback));
            }
        }
    }

    private static void clearActor(UUID actorId, ServerLevel level) {
        ActionPlayback playback = ACTIVE.remove(actorId);
        if (playback != null && level != null) {
            send(level, playback, CooperativeActionS2CPacket.stop(playback.family(), actorId, playback.sequence()));
        }
    }

    private static boolean retired(ServerLevel level, ActionPlayback playback) {
        Entity actor = level.getEntity(playback.entityId());
        if (actor == null || !playback.actorId().equals(actor.getUUID()) || !actor.isAlive()) {
            return true;
        }
        if (actor instanceof CloneEntity cloneBody && cloneBody.generation() != playback.actorGeneration()) {
            return true;
        }
        if (actor instanceof ServerPlayer && playback.actorGeneration() != CooperativeStructureLimits.OWNER_BODY_GENERATION) {
            return true;
        }
        return false;
    }

    private static boolean finished(ActionPlayback playback, long gameTime) {
        long duration = (long) playback.windupTicks() + playback.recoveryTicks();
        return gameTime - playback.serverStartTick() >= duration;
    }

    private static boolean accepted(ActionPlayback playback) {
        if (playback == null) {
            return false;
        }
        return CooperativeActionIds.OBLIQUE_SLASH.equals(playback.actionId())
                || CooperativeActionIds.SPIN.equals(playback.actionId());
    }

    private static ServerLevel levelOf(ActionPlayback playback) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        return server.getLevel(playback.dimension());
    }

    private static void send(ServerLevel level, ActionPlayback playback, CooperativeActionS2CPacket packet) {
        Entity actor = level.getEntity(playback.entityId());
        double reachSquared = (double) TRACKING_BLOCKS * TRACKING_BLOCKS;
        for (ServerPlayer watcher : level.players()) {
            if (actor == null) {
                if (watcher.getUUID().equals(playback.actorId()) || watcher.getUUID().equals(playback.family().ownerId())) {
                    ModNetwork.sendToPlayer(watcher, packet);
                }
                continue;
            }
            if (watcher == actor || watcher.distanceToSqr(actor) <= reachSquared) {
                ModNetwork.sendToPlayer(watcher, packet);
            }
        }
    }

    private static void trim() {
        while (ACTIVE.size() > CACHE_CAP) {
            UUID eldest = ACTIVE.keySet().iterator().next();
            ACTIVE.remove(eldest);
        }
    }
}
