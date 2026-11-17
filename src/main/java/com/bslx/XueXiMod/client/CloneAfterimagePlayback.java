package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.network.ClonePresentationS2CPacket;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneEntity;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneLifeState;
import com.bslx.XueXiMod.shu.technique.release.clone.core.ClonePresentationNotice;
import com.bslx.XueXiMod.shu.technique.release.clone.core.NoticePhase;
import com.bslx.XueXiMod.shu.technique.release.clone.core.PresentationKey;
import com.bslx.XueXiMod.shu.technique.release.clone.core.VolumeVisualKind;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 客户端世界上的维持、散水和断潮快照。上限 256。
 * 水线驻留优先画在活实体上；同部署标识的维持快照不再叠一层。
 * 实体离开只结束维持；已经收下的散水和斩击继续播到时长结束。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, value = Dist.CLIENT)
public final class CloneAfterimagePlayback {
    private static final int SNAPSHOT_LIMIT = 256;
    private static final int TRACE_LIMIT = 256;
    private static final Map<SnapshotKey, CloneAfterimageSnapshot> snapshots = new LinkedHashMap<>();
    private static final Map<UUID, DeploymentTrace> traces = new LinkedHashMap<>();
    private static final Map<UUID, UUID> actorLinks = new LinkedHashMap<>();
    private static ClientLevel boundLevel;
    private static boolean sessionBound;
    private static long boundSession;
    private static boolean clientBound;

    private CloneAfterimagePlayback() {
    }

    public static void bind() {
        clientBound = true;
    }

    public static void accept(ClonePresentationS2CPacket packet) {
        if (!clientBound || packet == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            return;
        }
        ClientLevel clientLevel = minecraft.level;
        if (clientLevel == null) {
            forgetWorld();
            return;
        }
        ClonePresentationNotice notice = packet.noticeOrNull();
        if (notice == null || !notice.dimension().equals(clientLevel.dimension())) {
            return;
        }
        alignWorld(clientLevel);
        if (!sessionAllowed(clientLevel, notice.sessionToken())) {
            return;
        }
        long gameTime = clientLevel.getGameTime();
        dropExpired(gameTime);
        DeploymentTrace trace = prepareTrace(notice.deploymentId(), gameTime);
        if (trace == null) {
            return;
        }
        if (trace.serialKnown && notice.eventSerial() < trace.newestSerial) {
            return;
        }
        if (trace.seenPhases.contains(notice.phase())) {
            return;
        }
        if (notice.phase() == NoticePhase.MAINTAIN && trace.terminated) {
            return;
        }
        trace.seenPhases.add(notice.phase());
        trace.serialKnown = true;
        if (notice.eventSerial() > trace.newestSerial) {
            trace.newestSerial = notice.eventSerial();
        }
        long retainTicks = Math.max(notice.durationTicks(), ClonePresentationS2CPacket.TRACKING_WINDOW_TICKS);
        long until = gameTime + retainTicks;
        if (until > trace.expireGameTime) {
            trace.expireGameTime = until;
        }
        if (notice.phase() == NoticePhase.SCATTER) {
            trace.terminated = true;
            trace.scatterSeen = true;
            snapshots.remove(new SnapshotKey(notice.deploymentId(), NoticePhase.MAINTAIN));
            forgetActorLinks(notice.deploymentId());
        }
        offerSnapshot(notice, gameTime);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ClientLevel clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) {
            forgetWorld();
            return;
        }
        service(clientLevel);
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        ClientLevel clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) {
            return;
        }
        service(clientLevel);
        if (snapshots.isEmpty()) {
            return;
        }
        draw(event, clientLevel);
    }

    @SubscribeEvent
    public static void onActorLeave(EntityLeaveLevelEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ClientLevel clientLevel)) {
            return;
        }
        if (!(event.getEntity() instanceof CloneEntity actor) || boundLevel != clientLevel) {
            return;
        }
        if (actor.presentationKey() != PresentationKey.WATER_OUTLINE) {
            return;
        }
        long gameTime = clientLevel.getGameTime();
        actorLinks.remove(actor.getUUID());
        endMaintain(actor.presentationDeploymentId(), gameTime);
    }

    public static boolean scatterHidesBody(UUID deploymentId) {
        if (deploymentId == null) {
            return false;
        }
        DeploymentTrace trace = traces.get(deploymentId);
        return trace != null && trace.scatterSeen;
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        forgetWorld();
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel unloaded && boundLevel == unloaded) {
            forgetWorld();
        }
    }

    private static void service(ClientLevel clientLevel) {
        alignWorld(clientLevel);
        dropExpired(clientLevel.getGameTime());
        linkActors(clientLevel);
    }

    private static void alignWorld(ClientLevel clientLevel) {
        if (boundLevel == clientLevel) {
            return;
        }
        forgetWorld();
        boundLevel = clientLevel;
    }

    private static void forgetWorld() {
        snapshots.clear();
        traces.clear();
        actorLinks.clear();
        boundLevel = null;
        sessionBound = false;
        boundSession = 0L;
    }

    private static boolean sessionAllowed(ClientLevel clientLevel, long sessionToken) {
        if (sessionBound && sessionToken != boundSession) {
            return false;
        }
        if (!matchesLivingActors(clientLevel, sessionToken)) {
            return false;
        }
        if (!sessionBound) {
            sessionBound = true;
            boundSession = sessionToken;
        }
        return true;
    }

    private static boolean matchesLivingActors(ClientLevel clientLevel, long sessionToken) {
        boolean sawSynced = false;
        for (Entity entity : clientLevel.entitiesForRendering()) {
            if (!(entity instanceof CloneEntity actor)) {
                continue;
            }
            long synced = actor.syncedSessionToken();
            if (synced <= 0L) {
                continue;
            }
            sawSynced = true;
            if (synced == sessionToken) {
                return true;
            }
        }
        return !sawSynced;
    }

    private static DeploymentTrace prepareTrace(UUID deploymentId, long gameTime) {
        DeploymentTrace existing = traces.get(deploymentId);
        if (existing != null && gameTime < existing.expireGameTime) {
            return existing;
        }
        if (existing != null) {
            traces.remove(deploymentId);
            forgetActorLinks(deploymentId);
        }
        dropExpiredTraces(gameTime);
        while (traces.size() >= TRACE_LIMIT) {
            Iterator<Map.Entry<UUID, DeploymentTrace>> cursor = traces.entrySet().iterator();
            if (!cursor.hasNext()) {
                break;
            }
            cursor.next();
            cursor.remove();
        }
        if (traces.size() >= TRACE_LIMIT) {
            return null;
        }
        DeploymentTrace created = new DeploymentTrace();
        created.expireGameTime = gameTime;
        traces.put(deploymentId, created);
        return created;
    }

    private static void offerSnapshot(ClonePresentationNotice notice, long gameTime) {
        double depthBlocks = 0.0d;
        double heightBlocks = 0.0d;
        double widthBlocks = 0.0d;
        if (notice.phase() == NoticePhase.VOLUME) {
            if (notice.volumeVisual() != VolumeVisualKind.TIDE_SLASH_FULL) {
                return;
            }
            double[] extents = ActorVolumeVisual.sampleTideSeverExtents();
            if (extents == null) {
                return;
            }
            depthBlocks = extents[0];
            heightBlocks = extents[1];
            widthBlocks = extents[2];
        } else if (notice.presentationKey() != PresentationKey.WATER_OUTLINE) {
            return;
        }
        SnapshotKey key = new SnapshotKey(notice.deploymentId(), notice.phase());
        if (snapshots.containsKey(key)) {
            return;
        }
        while (snapshots.size() >= SNAPSHOT_LIMIT) {
            Iterator<Map.Entry<SnapshotKey, CloneAfterimageSnapshot>> cursor = snapshots.entrySet().iterator();
            if (!cursor.hasNext()) {
                break;
            }
            cursor.next();
            cursor.remove();
        }
        if (snapshots.size() >= SNAPSHOT_LIMIT) {
            return;
        }
        snapshots.put(key, new CloneAfterimageSnapshot(notice.deploymentId(), notice.actionToken(),
                notice.eventSerial(), notice.sessionToken(), notice.phase(), notice.presentationKey(),
                notice.presentationRevision(), notice.worldPosition().x, notice.worldPosition().y,
                notice.worldPosition().z, notice.lookUnit().x, notice.lookUnit().y, notice.lookUnit().z,
                notice.durationTicks(), notice.volumeVisual(), depthBlocks, heightBlocks, widthBlocks, gameTime,
                notice.dimension()));
    }

    private static void endMaintain(UUID deploymentId, long gameTime) {
        snapshots.remove(new SnapshotKey(deploymentId, NoticePhase.MAINTAIN));
        forgetActorLinks(deploymentId);
        DeploymentTrace trace = traces.get(deploymentId);
        if (trace == null || gameTime >= trace.expireGameTime) {
            traces.remove(deploymentId);
            dropExpiredTraces(gameTime);
            while (traces.size() >= TRACE_LIMIT) {
                Iterator<Map.Entry<UUID, DeploymentTrace>> cursor = traces.entrySet().iterator();
                if (!cursor.hasNext()) {
                    break;
                }
                cursor.next();
                cursor.remove();
            }
            if (traces.size() >= TRACE_LIMIT) {
                return;
            }
            trace = new DeploymentTrace();
            trace.expireGameTime = gameTime;
            traces.put(deploymentId, trace);
        }
        trace.terminated = true;
        long until = gameTime + (long) ClonePresentationS2CPacket.TRACKING_WINDOW_TICKS;
        if (until > trace.expireGameTime) {
            trace.expireGameTime = until;
        }
    }

    private static void linkActors(ClientLevel clientLevel) {
        boolean pending = false;
        for (CloneAfterimageSnapshot snapshot : snapshots.values()) {
            if (snapshot.phase() == NoticePhase.MAINTAIN) {
                pending = true;
                break;
            }
        }
        if (!pending) {
            return;
        }
        for (Entity entity : clientLevel.entitiesForRendering()) {
            if (!(entity instanceof CloneEntity actor) || actor.isRemoved()) {
                continue;
            }
            if (actor.presentationKey() != PresentationKey.WATER_OUTLINE) {
                continue;
            }
            if (actorLinks.containsKey(actor.getUUID())) {
                continue;
            }
            UUID deploymentId = actor.presentationDeploymentId();
            CloneAfterimageSnapshot maintain = snapshots.get(new SnapshotKey(deploymentId, NoticePhase.MAINTAIN));
            if (maintain == null) {
                continue;
            }
            long synced = actor.syncedSessionToken();
            if (synced > 0L && synced != maintain.sessionToken()) {
                continue;
            }
            while (actorLinks.size() >= SNAPSHOT_LIMIT) {
                Iterator<Map.Entry<UUID, UUID>> cursor = actorLinks.entrySet().iterator();
                if (!cursor.hasNext()) {
                    break;
                }
                cursor.next();
                cursor.remove();
            }
            if (actorLinks.size() < SNAPSHOT_LIMIT) {
                actorLinks.put(actor.getUUID(), deploymentId);
            }
        }
    }

    private static boolean actorCoversMaintain(ClientLevel clientLevel, CloneAfterimageSnapshot snapshot) {
        for (Entity entity : clientLevel.entitiesForRendering()) {
            if (!(entity instanceof CloneEntity actor) || actor.isRemoved()) {
                continue;
            }
            if (actor.presentationKey() != PresentationKey.WATER_OUTLINE
                    || actor.lifeState() != CloneLifeState.ACTIVE) {
                continue;
            }
            if (!snapshot.deploymentId().equals(actor.presentationDeploymentId())) {
                continue;
            }
            long synced = actor.syncedSessionToken();
            if (synced > 0L && synced != snapshot.sessionToken()) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static void dropExpired(long gameTime) {
        Iterator<Map.Entry<SnapshotKey, CloneAfterimageSnapshot>> snapshotCursor = snapshots.entrySet().iterator();
        while (snapshotCursor.hasNext()) {
            Map.Entry<SnapshotKey, CloneAfterimageSnapshot> entry = snapshotCursor.next();
            if (entry.getValue().expired(gameTime)) {
                snapshotCursor.remove();
                if (entry.getKey().phase() == NoticePhase.MAINTAIN) {
                    forgetActorLinks(entry.getKey().deploymentId());
                }
            }
        }
        dropExpiredTraces(gameTime);
    }

    private static void dropExpiredTraces(long gameTime) {
        Iterator<Map.Entry<UUID, DeploymentTrace>> cursor = traces.entrySet().iterator();
        while (cursor.hasNext()) {
            Map.Entry<UUID, DeploymentTrace> entry = cursor.next();
            if (gameTime >= entry.getValue().expireGameTime) {
                cursor.remove();
            }
        }
    }

    private static void forgetActorLinks(UUID deploymentId) {
        Iterator<Map.Entry<UUID, UUID>> cursor = actorLinks.entrySet().iterator();
        while (cursor.hasNext()) {
            if (deploymentId.equals(cursor.next().getValue())) {
                cursor.remove();
            }
        }
    }

    private static void draw(RenderLevelStageEvent event, ClientLevel clientLevel) {
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();
        boolean drewSlash = false;
        boolean drewWater = false;
        float partialTick = event.getPartialTick();
        long gameTime = clientLevel.getGameTime();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        for (CloneAfterimageSnapshot snapshot : snapshots.values()) {
            if (snapshot.dimension() == null || !snapshot.dimension().equals(clientLevel.dimension())
                    || snapshot.expired(clientLevel.getGameTime())) {
                continue;
            }
            if (snapshot.phase() == NoticePhase.VOLUME) {
                if (snapshot.volumeVisual() == VolumeVisualKind.TIDE_SLASH_FULL) {
                    ActorVolumeVisual.draw(pose, buffers, clientLevel, snapshot);
                    drewSlash = true;
                }
                continue;
            }
            if (snapshot.presentationKey() != PresentationKey.WATER_OUTLINE) {
                continue;
            }
            if (snapshot.phase() == NoticePhase.MAINTAIN) {
                if (actorCoversMaintain(clientLevel, snapshot)) {
                    continue;
                }
                if (CloneOutlineVisual.drawMaintained(snapshot, pose, buffers, lightAt(clientLevel, snapshot),
                        partialTick, gameTime)) {
                    drewWater = true;
                }
            } else if (snapshot.phase() == NoticePhase.SCATTER) {
                if (CloneOutlineVisual.drawScattered(snapshot, pose, buffers, lightAt(clientLevel, snapshot),
                        partialTick, gameTime)) {
                    drewWater = true;
                }
            }
        }
        pose.popPose();
        if (drewSlash) {
            buffers.endBatch(ActorVolumeVisual.slashType());
        }
        if (drewWater) {
            buffers.endBatch(SteveWaterLineVisual.skinType());
            buffers.endBatch(SteveWaterLineVisual.flakeType());
        }
    }

    private static int lightAt(ClientLevel clientLevel, CloneAfterimageSnapshot snapshot) {
        return LevelRenderer.getLightColor(clientLevel,
                BlockPos.containing(snapshot.worldX(), snapshot.worldY(), snapshot.worldZ()));
    }

    private record SnapshotKey(UUID deploymentId, NoticePhase phase) {
    }

    private static final class DeploymentTrace {
        private final EnumSet<NoticePhase> seenPhases = EnumSet.noneOf(NoticePhase.class);
        private long newestSerial;
        private long expireGameTime;
        private boolean serialKnown;
        private boolean terminated;
        private boolean scatterSeen;
    }
}
