package com.bslx.XueXiMod.client.action;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.animation.core.Pose;
import com.bslx.XueXiMod.animation.core.model.Armature;
import com.bslx.XueXiMod.animation.runtime.ActionClientIngress;
import com.bslx.XueXiMod.animation.runtime.ActionDefinition;
import com.bslx.XueXiMod.animation.runtime.ActionPlaybackGate;
import com.bslx.XueXiMod.animation.runtime.ActionPlayer;
import com.bslx.XueXiMod.animation.runtime.ActionRegistry;
import com.bslx.XueXiMod.animation.runtime.ActionSampleContext;
import com.bslx.XueXiMod.animation.runtime.ActionVisualFallback;
import com.bslx.XueXiMod.animation.runtime.loader.DevActionLoader;
import com.bslx.XueXiMod.network.ActionSyncS2CPacket;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 客户端按服务端关卡 gameTime 取样。采样和只读上下文都不推进事件。 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ActionClientPlayback {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, View> VIEWS = new HashMap<>();
    private static ClientLevel boundLevel;
    private static Object boundLink;
    private static Object retiredLink;

    private ActionClientPlayback() {
    }

    public static void bindIngress() {
        ActionClientIngress.bind(ActionClientPlayback::accept);
    }

    /**
     * 该 UUID 在客户端地图里是否还有播放记录。
     * 不采样、不推进、不清理。淡出、结束、取消，以及过期后还留在地图里的记录都算有。
     */
    public static boolean hasPlaybackRecord(UUID ownerId) {
        return ownerId != null && VIEWS.containsKey(ownerId);
    }

    public static Pose sample(UUID ownerId, long gameTime, float partialTick) {
        View view = VIEWS.get(ownerId);
        if (view == null || view.player == null || view.cancelled || !entityApplies(view)) {
            return new Pose();
        }
        return view.player.sample(gameTime, partialTick);
    }

    public static ActionSampleContext inspect(UUID ownerId, long gameTime, float partialTick) {
        View view = VIEWS.get(ownerId);
        if (view == null || view.player == null) {
            return ActionSampleContext.inactive(ownerId);
        }
        ActionPlayer.Phase phase = view.player.phase();
        boolean active = entityApplies(view) && (phase == ActionPlayer.Phase.PLAYING || phase == ActionPlayer.Phase.PAUSED);
        Pose pose = phase == ActionPlayer.Phase.CANCELLED ? new Pose() : view.player.sample(gameTime, partialTick);
        float elapsed = view.player.unwrappedElapsed(gameTime, partialTick);
        return ActionSampleContext.capture(
                active,
                view.ownerId,
                view.entityNetworkId,
                view.sessionEpoch,
                view.sequence,
                view.groupId,
                parseId(view.actionIdText),
                view.visualActionId,
                view.armatureName,
                view.hasSlashFrame,
                view.directionX,
                view.directionY,
                view.directionZ,
                view.planeX,
                view.planeY,
                view.planeZ,
                ActionSampleContext.normalized(view.player.looping(), elapsed, view.authoritativeDurationSeconds),
                pose,
                view.startGameTick,
                view.authoritativeDurationSeconds,
                elapsed,
                view.beatIndex,
                view.revision,
                view.hasFrozenYaw,
                view.frozenYaw);
    }

    public static Armature armature(UUID ownerId) {
        View view = VIEWS.get(ownerId);
        if (view == null || view.definition == null) {
            return null;
        }
        return ActionRegistry.armature(view.definition.armatureName());
    }

    public static String actionIdText(UUID ownerId) {
        View view = VIEWS.get(ownerId);
        return view == null ? "" : view.actionIdText;
    }

    public static boolean hasFrozenYaw(UUID ownerId) {
        View view = VIEWS.get(ownerId);
        return view != null && view.hasFrozenYaw;
    }

    public static float frozenYaw(UUID ownerId) {
        View view = VIEWS.get(ownerId);
        return view == null ? 0.0F : view.frozenYaw;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Object liveLink = liveLink(minecraft);
        if (ActionPlaybackGate.replaceWorld(boundLevel, level, boundLink, liveLink)) {
            bindWorld(level, liveLink);
        }
        if (level == null) {
            return;
        }
        long gameTime = level.getGameTime();
        for (View view : VIEWS.values()) {
            if (view.player != null && view.player.phase() == ActionPlayer.Phase.PLAYING) {
                view.player.advanceEvents(gameTime, 0.0F);
                view.player.drainNotices();
                if (view.player.terminal()) {
                    view.terminal = true;
                    view.cancelled = view.player.phase() == ActionPlayer.Phase.CANCELLED;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        retiredLink = liveLink(Minecraft.getInstance());
        boundLevel = null;
        boundLink = null;
        VIEWS.clear();
    }

    @SubscribeEvent
    public static void onClientLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ClientLevel level)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        bindWorld(level, liveLink(minecraft));
    }

    @SubscribeEvent
    public static void onClientLevelUnload(LevelEvent.Unload event) {
        if (ActionPlaybackGate.sameLevel(boundLevel, event.getLevel())) {
            boundLevel = null;
            VIEWS.clear();
        }
    }

    private static void accept(ActionSyncS2CPacket packet, Object packetLink) {
        Minecraft minecraft = Minecraft.getInstance();
        Object liveLink = liveLink(minecraft);
        if (packetLink == null || packetLink == retiredLink || !ActionPlaybackGate.sameLink(liveLink, packetLink)) {
            return;
        }
        ClientLevel level = minecraft.level;
        if (ActionPlaybackGate.replaceWorld(boundLevel, level, boundLink, liveLink)) {
            bindWorld(level, liveLink);
        }
        if (level == null || packet.ownerId() == null) {
            return;
        }
        if (!level.dimension().location().toString().equals(packet.dimensionId())) {
            return;
        }
        View view = VIEWS.get(packet.ownerId());
        if (view != null && view.connectionLink != packetLink) {
            VIEWS.remove(packet.ownerId());
            view = null;
        }
        boolean hasView = view != null;
        if (!ActionPlaybackGate.admit(
                hasView,
                hasView ? view.sessionEpoch : 0L,
                hasView ? view.sequence : 0L,
                hasView ? view.revision : 0,
                hasView && view.terminal,
                hasView && view.cancelled,
                packet.sessionEpoch(),
                packet.sequence(),
                packet.revision(),
                packet.kind())) {
            return;
        }
        boolean sameLife = hasView && packet.sessionEpoch() == view.sessionEpoch && packet.sequence() == view.sequence;
        long gameTime = level.getGameTime();
        if (packet.kind() == ActionSyncS2CPacket.CANCEL
                || (packet.kind() == ActionSyncS2CPacket.SNAPSHOT && packet.stateCode() == ActionSyncS2CPacket.STATE_CANCELLED)) {
            clear(packet, view, packetLink);
            return;
        }
        if (sameLife && view.player != null && (packet.kind() == ActionSyncS2CPacket.PAUSE
                || packet.kind() == ActionSyncS2CPacket.RESUME
                || packet.kind() == ActionSyncS2CPacket.SNAPSHOT
                || packet.kind() == ActionSyncS2CPacket.FINISH)) {
            applyClock(packet, view, gameTime, packetLink);
            return;
        }
        View adopted = adopt(packet, view, gameTime, packetLink);
        if (adopted != null) {
            VIEWS.put(packet.ownerId(), adopted);
        }
    }

    private static void bindWorld(ClientLevel level, Object liveLink) {
        if (!ActionPlaybackGate.replaceWorld(boundLevel, level, boundLink, liveLink)) {
            return;
        }
        boundLevel = level;
        boundLink = liveLink;
        VIEWS.clear();
    }

    private static Object liveLink(Minecraft minecraft) {
        if (minecraft.getConnection() == null) {
            return null;
        }
        return minecraft.getConnection().getConnection();
    }

    private static boolean entityApplies(View view) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || view == null) {
            return false;
        }
        Entity entity = minecraft.level.getEntity(view.entityNetworkId);
        return ActionPlaybackGate.entityApplies(
                view.ownerId,
                view.entityNetworkId,
                entity == null ? null : entity.getUUID(),
                entity == null ? -1 : entity.getId(),
                entity != null);
    }

    private static void applyClock(ActionSyncS2CPacket packet, View view, long gameTime, Object connectionLink) {
        if (packet.kind() == ActionSyncS2CPacket.FINISH || packet.stateCode() == ActionSyncS2CPacket.STATE_FINISHED) {
            if (!view.player.terminal()) {
                view.player.finish(gameTime);
            }
            view.terminal = true;
            view.cancelled = false;
            rememberFrame(view, packet, connectionLink);
            view.player.drainNotices();
            return;
        }
        boolean paused = packet.kind() == ActionSyncS2CPacket.PAUSE || packet.stateCode() == ActionSyncS2CPacket.STATE_PAUSED;
        if (packet.kind() == ActionSyncS2CPacket.RESUME) {
            paused = false;
        }
        ActionPlayer.Decision decision = view.player.alignClock(
                packet.clockOffsetTicks(),
                paused,
                packet.pausedElapsedSeconds(),
                packet.pausedBlendElapsedSeconds(),
                packet.pauseBeganTick());
        if (!decision.accepted()) {
            LOGGER.info("动作时钟没有对齐：{}", decision.reasonText());
            return;
        }
        rememberFrame(view, packet, connectionLink);
        view.terminal = view.player.terminal();
        view.cancelled = view.player.phase() == ActionPlayer.Phase.CANCELLED;
        view.player.drainNotices();
    }

    private static void clear(ActionSyncS2CPacket packet, View view, Object connectionLink) {
        View target = view == null ? new View() : view;
        if (target.player != null && !target.player.terminal()) {
            target.player.stop();
        }
        target.ownerId = packet.ownerId();
        target.sessionEpoch = packet.sessionEpoch();
        target.sequence = packet.sequence();
        target.revision = packet.revision();
        target.terminal = true;
        target.cancelled = true;
        target.actionIdText = packet.actionIdText();
        target.dimensionId = packet.dimensionId();
        target.connectionLink = connectionLink;
        VIEWS.put(packet.ownerId(), target);
    }

    private static View adopt(ActionSyncS2CPacket packet, View previous, long gameTime, Object connectionLink) {
        ResourceLocation requestedId = parseId(packet.actionIdText());
        if (requestedId == null) {
            LOGGER.info("动作包 ID 无法解析：{}", packet.actionIdText());
            return null;
        }
        ResourceLocation fallbackId = parseId(packet.fallbackActionIdText());
        if (fallbackId == null) {
            fallbackId = ActionVisualFallback.fallbackFor(requestedId);
        }
        ActionVisualFallback.Prepared prepared = ActionVisualFallback.prepare(
                requestedId, fallbackId, packet.authoritativeDurationSeconds(), packet.blendSeconds());
        if (prepared == null) {
            LOGGER.info("动作 {} 没有可显示的兜底：{}", requestedId, ActionRegistry.gateText());
            return null;
        }
        boolean replacing = previous != null && previous.player != null
                && (previous.player.phase() == ActionPlayer.Phase.PLAYING || previous.player.phase() == ActionPlayer.Phase.PAUSED)
                && packet.sequence() > previous.sequence;
        ActionPlayer player = replacing ? previous.player : new ActionPlayer();
        boolean finishedSnapshot = packet.kind() == ActionSyncS2CPacket.FINISH
                || packet.stateCode() == ActionSyncS2CPacket.STATE_FINISHED;
        boolean pausedSnapshot = !finishedSnapshot && (packet.kind() == ActionSyncS2CPacket.PAUSE
                || packet.stateCode() == ActionSyncS2CPacket.STATE_PAUSED);
        ActionPlayer.Decision decision;
        if (replacing) {
            decision = player.replace(prepared.definition(), prepared.clip(), gameTime, packet.startGameTick(),
                    packet.speed(), packet.looping(), prepared.definition().blendSeconds());
        } else {
            decision = player.adoptClock(prepared.definition(), prepared.clip(), packet.startGameTick(), gameTime,
                    packet.speed(), finishedSnapshot ? false : packet.looping(), prepared.definition().blendSeconds(),
                    packet.clockOffsetTicks(), pausedSnapshot, packet.pausedElapsedSeconds(), packet.pausedBlendElapsedSeconds(),
                    packet.pauseBeganTick());
        }
        if (!decision.accepted()) {
            LOGGER.info("动作 {} 没有被客户端接受：{}", requestedId, decision.reasonText());
            return null;
        }
        if (finishedSnapshot && !player.terminal()) {
            player.finish(gameTime);
        }
        View view = previous == null ? new View() : previous;
        view.player = player;
        view.definition = prepared.definition();
        view.visualActionId = prepared.visualActionId();
        view.armatureName = prepared.armatureName();
        rememberFrame(view, packet, connectionLink);
        view.terminal = player.terminal();
        view.cancelled = player.phase() == ActionPlayer.Phase.CANCELLED;
        player.drainNotices();
        return view;
    }

    private static void rememberFrame(View view, ActionSyncS2CPacket packet, Object connectionLink) {
        view.ownerId = packet.ownerId();
        view.entityNetworkId = packet.entityNetworkId();
        view.dimensionId = packet.dimensionId();
        view.connectionLink = connectionLink;
        view.sessionEpoch = packet.sessionEpoch();
        view.sequence = packet.sequence();
        view.revision = packet.revision();
        view.actionIdText = packet.actionIdText();
        view.groupId = packet.parentGroupId();
        view.startGameTick = packet.startGameTick();
        view.authoritativeDurationSeconds = packet.authoritativeDurationSeconds();
        view.beatIndex = packet.beatIndex();
        view.hasSlashFrame = packet.hasSlashFrame();
        view.directionX = packet.directionX();
        view.directionY = packet.directionY();
        view.directionZ = packet.directionZ();
        view.planeX = packet.planeX();
        view.planeY = packet.planeY();
        view.planeZ = packet.planeZ();
        view.hasFrozenYaw = packet.hasFrozenYaw();
        view.frozenYaw = packet.frozenYaw();
    }

    private static ResourceLocation parseId(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return DevActionLoader.parseActionId(text);
        } catch (RuntimeException failure) {
            return null;
        }
    }

    private static final class View {
        private ActionPlayer player;
        private ActionDefinition definition;
        private UUID ownerId;
        private int entityNetworkId;
        private String dimensionId = "";
        private Object connectionLink;
        private long sessionEpoch;
        private long sequence;
        private int revision;
        private String actionIdText = "";
        private ResourceLocation visualActionId;
        private String armatureName = "";
        private UUID groupId;
        private long startGameTick;
        private float authoritativeDurationSeconds;
        private int beatIndex = -1;
        private boolean hasSlashFrame;
        private float directionX;
        private float directionY;
        private float directionZ;
        private float planeX;
        private float planeY;
        private float planeZ;
        private boolean terminal;
        private boolean cancelled;
        private boolean hasFrozenYaw;
        private float frozenYaw;
    }
}
