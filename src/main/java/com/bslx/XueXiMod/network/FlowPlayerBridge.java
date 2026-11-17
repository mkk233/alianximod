package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.technique.release.domain.DomainInstance;
import com.bslx.XueXiMod.shu.technique.release.domain.motion.FlowMotionIntents;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** 只向受影响的玩家或驾驶者发送临时流场指令，并校验回执。 */
public final class FlowPlayerBridge {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, Engagement> ENGAGED = new HashMap<>();
    private static final ThreadLocal<Boolean> INSIDE = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean> SUPPRESS_JUMP = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private FlowPlayerBridge() {
    }

    public static void notice(DomainInstance domain, Entity root, FlowMotionIntents.HeldShift shift) {
        ServerPlayer driver = driverOf(root);
        if (driver == null || shift == null) {
            return;
        }
        Engagement engagement = ENGAGED.computeIfAbsent(driver.getUUID(), ignored -> new Engagement());
        if (engagement.engagedTick < 0) {
            engagement.engagedTick = driver.server.getTickCount();
        }
        engagement.commands.put(root.getUUID(), shift);
        FlowIntentS2CPacket.send(driver, shift, false);
    }

    public static void recall(UUID rootId) {
        if (rootId == null) {
            return;
        }
        Iterator<Map.Entry<UUID, Engagement>> entries = ENGAGED.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<UUID, Engagement> entry = entries.next();
            Engagement engagement = entry.getValue();
            FlowMotionIntents.HeldShift removed = engagement.commands.remove(rootId);
            if (removed == null) {
                continue;
            }
            ServerPlayer driver = player(entry.getKey());
            if (driver != null) {
                FlowIntentS2CPacket.send(driver, removed, true);
            }
            if (engagement.commands.isEmpty() && engagement.awaitingSequence == 0) {
                entries.remove();
            }
        }
    }

    public static void forgetInstance(UUID instanceId) {
        Iterator<Map.Entry<UUID, Engagement>> entries = ENGAGED.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<UUID, Engagement> entry = entries.next();
            Engagement engagement = entry.getValue();
            boolean changed = engagement.commands.entrySet().removeIf(command -> instanceId.equals(command.getValue().instanceId));
            if (!changed) {
                continue;
            }
            ServerPlayer driver = player(entry.getKey());
            if (driver != null) {
                FlowIntentS2CPacket.sendCleared(driver, instanceId);
            }
            if (engagement.commands.isEmpty() && engagement.awaitingSequence == 0) {
                entries.remove();
            }
        }
    }

    public static void clear(ServerPlayer player) {
        if (player != null) {
            ENGAGED.remove(player.getUUID());
        }
    }

    public static boolean insideWrapped() {
        return INSIDE.get();
    }

    public static boolean suppressJump() {
        return SUPPRESS_JUMP.get();
    }

    public static boolean blockVanilla(ServerPlayer player) {
        Engagement engagement = engagementOf(player);
        if (engagement == null) {
            return false;
        }
        if (engagement.awaitingSequence != 0) {
            return true;
        }
        if (engagement.wrappedSeen) {
            return true;
        }
        return player.server.getTickCount() - engagement.engagedTick > 10;
    }

    public static void rejectVanilla(ServerPlayer player, String reason) {
        if (player == null) {
            return;
        }
        Engagement engagement = ENGAGED.computeIfAbsent(player.getUUID(), ignored -> new Engagement());
        if (engagement.engagedTick < 0) {
            engagement.engagedTick = player.server.getTickCount();
        }
        correct(player, engagement, reason);
    }

    public static void acceptPlayer(ServerPlayer player, FlowMoveC2SPacket packet) {
        if (player == null || packet == null || player.connection == null) {
            return;
        }
        Engagement engagement = engagementOf(player);
        if (engagement != null && engagement.awaitingSequence != 0) {
            correct(player, engagement, "awaiting-correction");
            return;
        }
        if (!packet.matchesPlayer(player)) {
            correct(player, engagement == null ? ENGAGED.computeIfAbsent(player.getUUID(), ignored -> new Engagement()) : engagement,
                    "player-mismatch");
            return;
        }
        if (!receiptHolds(player, packet)) {
            Engagement held = ENGAGED.computeIfAbsent(player.getUUID(), ignored -> new Engagement());
            correct(player, held, "stale-flow");
            return;
        }
        if (engagement != null) {
            engagement.wrappedSeen = true;
        }
        boolean suppress = packet.hasRise() && !packet.playerJumped();
        INSIDE.set(true);
        SUPPRESS_JUMP.set(suppress);
        FlowMotionIntents.alreadyIncluded(true);
        try {
            player.connection.handleMovePlayer(packet.toPlayerPacket(player));
        } finally {
            INSIDE.set(false);
            SUPPRESS_JUMP.set(false);
            FlowMotionIntents.alreadyIncluded(false);
        }
    }

    public static void acceptVehicle(ServerPlayer player, FlowMoveC2SPacket packet) {
        if (player == null || packet == null || player.connection == null) {
            return;
        }
        Engagement engagement = engagementOf(player);
        if (engagement != null && engagement.awaitingSequence != 0) {
            correct(player, engagement, "awaiting-correction");
            return;
        }
        Entity root = player.getRootVehicle();
        if (root == player || root instanceof AbstractMinecart || !packet.matchesRoot(root)) {
            Engagement held = engagement == null ? ENGAGED.computeIfAbsent(player.getUUID(), ignored -> new Engagement()) : engagement;
            correct(player, held, "vehicle-mismatch");
            return;
        }
        if (!receiptHolds(player, packet)) {
            Engagement held = ENGAGED.computeIfAbsent(player.getUUID(), ignored -> new Engagement());
            correct(player, held, "stale-flow");
            return;
        }
        if (engagement != null) {
            engagement.wrappedSeen = true;
        }
        INSIDE.set(true);
        FlowMotionIntents.alreadyIncluded(true);
        try {
            player.connection.handleMoveVehicle(packet.toVehiclePacket());
        } finally {
            INSIDE.set(false);
            FlowMotionIntents.alreadyIncluded(false);
        }
    }

    public static void acceptAck(ServerPlayer player, int sequence) {
        Engagement engagement = engagementOf(player);
        if (engagement == null || sequence != engagement.awaitingSequence) {
            LOGGER.warn("水流流场纠正确认不匹配：{} 序号 {}", player == null ? "unknown" : player.getScoreboardName(), sequence);
            return;
        }
        engagement.awaitingSequence = 0;
        if (engagement.commands.isEmpty()) {
            ENGAGED.remove(player.getUUID());
        }
    }

    private static boolean receiptHolds(ServerPlayer player, FlowMoveC2SPacket packet) {
        long epoch = FlowMotionIntents.epochOf(packet.instanceId());
        if (epoch <= 0L || epoch != packet.epoch()) {
            return false;
        }
        Engagement engagement = engagementOf(player);
        if (engagement == null) {
            return false;
        }
        FlowMotionIntents.HeldShift issued = engagement.commands.get(packet.rootId());
        if (issued == null || !issued.instanceId.equals(packet.instanceId()) || issued.epoch != packet.epoch()) {
            return false;
        }
        if (packet.step() <= 0L || packet.step() > issued.step) {
            return false;
        }
        return Double.isFinite(packet.nativeVertical()) && Math.abs(packet.nativeVertical()) < 10.0D;
    }

    private static void correct(ServerPlayer player, Engagement engagement, String reason) {
        int now = player.server.getTickCount();
        if (engagement.lastLoggedTick != now) {
            engagement.lastLoggedTick = now;
            LOGGER.warn("水流流场拒绝移动提案：{} 原因 {}", player.getScoreboardName(), reason);
        }
        if (engagement.awaitingSequence != 0) {
            return;
        }
        engagement.awaitingSequence = engagement.nextSequence++;
        Entity root = player.getRootVehicle();
        FlowCorrectionS2CPacket.send(player, root, engagement.awaitingSequence);
    }

    private static Engagement engagementOf(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        return ENGAGED.get(player.getUUID());
    }

    private static ServerPlayer driverOf(Entity root) {
        if (root instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }
        if (root instanceof AbstractMinecart) {
            return null;
        }
        if (root.getControllingPassenger() instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }
        return null;
    }

    private static ServerPlayer player(UUID playerId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || playerId == null) {
            return null;
        }
        return server.getPlayerList().getPlayer(playerId);
    }

    private static final class Engagement {
        private final Map<UUID, FlowMotionIntents.HeldShift> commands = new HashMap<>();
        private int engagedTick = -1;
        private boolean wrappedSeen;
        private int awaitingSequence;
        private int nextSequence = 1;
        private int lastLoggedTick = -1;
    }
}
