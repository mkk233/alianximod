package com.bslx.XueXiMod.shu.technique.release.clone.core;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ClonePresentation {
    static final int NOTICE_WINDOW_TICKS = 20;
    private static final double NOTICE_RANGE_BLOCKS = 160.0D;
    private static final double NOTICE_RANGE_SQUARED = NOTICE_RANGE_BLOCKS * NOTICE_RANGE_BLOCKS;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<PublishedNotice> PUBLISHED = new HashSet<>();

    private static ClonePresentationSink sink = NoopClonePresentation.INSTANCE;
    private static boolean installed;
    private static long nextSerial = 1L;

    private ClonePresentation() {
    }

    public static void install(ClonePresentationSink nextSink) {
        if (nextSink == null) {
            throw new IllegalArgumentException("表现接收器不能为空");
        }
        if (installed) {
            throw new IllegalStateException("表现接收器已经安装");
        }
        sink = nextSink;
        installed = true;
    }

    public static void publish(ClonePresentationNotice notice) {
        if (notice == null) {
            return;
        }
        ClonePresentationSink target = sink == null ? NoopClonePresentation.INSTANCE : sink;
        try {
            target.accept(notice);
        } catch (RuntimeException exception) {
            LOGGER.error("水幻形表现通知没有送出 deployment={}", notice.deploymentId(), exception);
        }
    }

    public static boolean installed() {
        return installed;
    }

    static void emit(MinecraftServer server, CloneRuntimeState state, Vec3 worldPosition, Vec3 lookUnit,
                     NoticePhase phase, VolumeVisualKind visual) {
        if (server == null || state == null || state.spec == null || phase == null || !server.isSameThread()) {
            return;
        }
        if (!CaptureVectors.finite(worldPosition) || !CaptureVectors.unit(lookUnit)) {
            return;
        }
        long actionToken = state.occupiedToken;
        PublishedNotice key = new PublishedNotice(state.deploymentLease == null
                ? state.actorId : state.deploymentLease.deploymentId(), actionToken, phase);
        if (!PUBLISHED.add(key)) {
            return;
        }
        CloneRuntime runtime = CloneRuntime.get(server);
        if (runtime == null) {
            PUBLISHED.remove(key);
            return;
        }
        ServerLevel level = server.getLevel(state.dimension);
        if (level == null) {
            PUBLISHED.remove(key);
            return;
        }
        if (!audiencePresent(level, state.ownerId, worldPosition)) {
            PUBLISHED.remove(key);
            return;
        }
        if (nextSerial == Long.MAX_VALUE) {
            PUBLISHED.remove(key);
            return;
        }
        long serial = nextSerial++;
        VolumeVisualKind shown = visual == null ? VolumeVisualKind.NONE : visual;
        try {
            publish(new ClonePresentationNotice(state.dimension, runtime.sessionToken(), key.deploymentId,
                    actionToken, serial, state.spec.presentationKey(), state.spec.presentationRevision(),
                    worldPosition, lookUnit, phase, NOTICE_WINDOW_TICKS, shown));
        } catch (RuntimeException exception) {
            PUBLISHED.remove(key);
            LOGGER.error("水幻形表现通知没有形成 deployment={}", key.deploymentId, exception);
        }
    }

    static void emitVolume(MinecraftServer server, CapturedResponseTicket ticket, CloneSpec spec,
                           Vec3 worldPosition, VolumeVisualKind visual) {
        if (server == null || ticket == null || spec == null || !server.isSameThread()) {
            return;
        }
        if (!CaptureVectors.finite(worldPosition) || !CaptureVectors.unit(ticket.actorFrame().lookUnit())) {
            return;
        }
        PublishedNotice key = new PublishedNotice(ticket.deploymentId(), ticket.actionToken(), NoticePhase.VOLUME);
        if (!PUBLISHED.add(key)) {
            return;
        }
        ResourceKey<Level> dimension = ticket.actorFrame().dimension();
        ServerLevel level = server.getLevel(dimension);
        if (level == null) {
            PUBLISHED.remove(key);
            return;
        }
        if (!audiencePresent(level, ticket.ownerId(), worldPosition)) {
            PUBLISHED.remove(key);
            return;
        }
        if (nextSerial == Long.MAX_VALUE) {
            PUBLISHED.remove(key);
            return;
        }
        long serial = nextSerial++;
        VolumeVisualKind shown = visual == null ? VolumeVisualKind.NONE : visual;
        try {
            publish(new ClonePresentationNotice(dimension, ticket.sessionToken(), ticket.deploymentId(),
                    ticket.actionToken(), serial, spec.presentationKey(), spec.presentationRevision(),
                    worldPosition, ticket.actorFrame().lookUnit(), NoticePhase.VOLUME, NOTICE_WINDOW_TICKS, shown));
        } catch (RuntimeException exception) {
            PUBLISHED.remove(key);
            LOGGER.error("水幻形体积通知没有形成 deployment={}", ticket.deploymentId(), exception);
        }
    }

    static void forget(UUID deploymentId) {
        if (deploymentId == null || PUBLISHED.isEmpty()) {
            return;
        }
        PUBLISHED.removeIf(notice -> deploymentId.equals(notice.deploymentId));
    }

    static void clearHistory() {
        PUBLISHED.clear();
        nextSerial = 1L;
    }

    private static boolean audiencePresent(ServerLevel level, UUID ownerId, Vec3 worldPosition) {
        List<ServerPlayer> players = new ArrayList<>(level.players());
        for (ServerPlayer player : players) {
            if (player == null || player.hasDisconnected()) {
                continue;
            }
            if (ownerId != null && ownerId.equals(player.getUUID())) {
                return true;
            }
            if (player.distanceToSqr(worldPosition) <= NOTICE_RANGE_SQUARED) {
                return true;
            }
        }
        return false;
    }

    private record PublishedNotice(UUID deploymentId, long actionToken, NoticePhase phase) {
    }
}
