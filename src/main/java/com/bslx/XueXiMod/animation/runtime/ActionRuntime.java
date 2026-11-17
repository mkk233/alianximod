package com.bslx.XueXiMod.animation.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.bslx.XueXiMod.animation.core.AnimationClip;
import com.bslx.XueXiMod.animation.core.Pose;
import com.bslx.XueXiMod.network.ActionSyncS2CPacket;
import com.bslx.XueXiMod.network.ModNetwork;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeActionIds;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * 服务端姿态会话。身份是主人、实体、维度、纪元和递增序号。
 * 替换姿态不会取消已经提交的攻击几何。结束和取消沿用开始时的序号。
 * 不负责命中或伤害结算。
 */
public final class ActionRuntime {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, Long> EPOCHS = new HashMap<>();
    private static final Map<UUID, Long> SEQUENCES = new HashMap<>();
    private static final Map<UUID, List<String>> VISUAL_NOTICES = new HashMap<>();

    private ActionRuntime() {
    }

    /** 直接打开一段姿态。这条路径不检查攻击组提交许可。 */
    public static Receipt play(ServerPlayer player, ResourceLocation actionId, float speed, boolean looping, Float frozenYaw, UUID parentGroupId) {
        return open(player, actionId, speed, looping, frozenYaw, parentGroupId, null);
    }

    /** 没有提交许可时一律拒绝，避免把攻击组通知当成可播放。 */
    public static Receipt notifyAttackGroup(ServerPlayer player, UUID groupId, ResourceLocation actionId) {
        return Receipt.reject(actionId, groupId, "攻击组需要提交许可");
    }

    /** 许可和提交一致后才打开姿态。动作和兜底都不可播放时不打开。 */
    public static Receipt notifyAttackGroup(ServerPlayer player, CommitPermit permit, ActionCommit commit) {
        String invalidPlayer = validatePlayer(player);
        if (invalidPlayer != null) {
            return Receipt.reject(commit == null ? null : commit.actionId(), commit == null ? null : commit.groupId(), invalidPlayer);
        }
        long serverTick = player.getServer().getTickCount();
        PermitPreparation preparation = preparePermit(player.getUUID(), permit, commit, serverTick);
        if (preparation.outcome() != CommitAuthorization.Outcome.READY || preparation.actionId() == null) {
            return Receipt.reject(commit == null ? null : commit.actionId(), commit == null ? null : commit.groupId(), preparation.reasonText());
        }
        return open(player, preparation.actionId(), 1.0F, false, null, commit.groupId(), commit.withFallback(preparation.fallbackId()));
    }

    /** 判断许可，并选出这次能播放的动作或兜底。 */
    public static PermitPreparation preparePermit(UUID playerId, CommitPermit permit, ActionCommit commit, long serverTick) {
        CommitAuthorization.Outcome outcome = CommitAuthorization.decide(playerId, permit, commit, serverTick);
        if (outcome != CommitAuthorization.Outcome.READY) {
            return PermitPreparation.blocked(outcome);
        }
        ResourceLocation playId = selectPlayable(commit);
        ResourceLocation fallbackId = commit.fallbackActionId() != null ? commit.fallbackActionId() : ActionVisualFallback.fallbackFor(commit.actionId());
        GroupReplayLedger.remember(playerId, commit.groupId(), serverTick, playId == null);
        if (playId == null) {
            return new PermitPreparation(CommitAuthorization.Outcome.READY, null, fallbackId, "动作和兜底都不可播放");
        }
        return new PermitPreparation(CommitAuthorization.Outcome.READY, playId, fallbackId, "");
    }

    /** 主动作能播就用主动作，否则改用可播放的兜底。 */
    public static ResourceLocation selectPlayable(ActionCommit commit) {
        if (commit == null || commit.actionId() == null) {
            return null;
        }
        if (playable(commit.actionId())) {
            return commit.actionId();
        }
        ResourceLocation fallbackId = commit.fallbackActionId() != null
                ? commit.fallbackActionId()
                : ActionVisualFallback.fallbackFor(commit.actionId());
        if (fallbackId.equals(commit.actionId()) || !playable(fallbackId)) {
            return null;
        }
        return fallbackId;
    }

    /** 取消当前未结束的姿态，并沿用原序号通知。 */
    public static Receipt stop(ServerPlayer player) {
        String invalidPlayer = validatePlayer(player);
        if (invalidPlayer != null) {
            return Receipt.reject(null, null, invalidPlayer);
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.terminal) {
            return Receipt.reject(session == null ? null : session.actionId, session == null ? null : session.parentGroupId, "没有可取消的姿态");
        }
        ActionPlayer.Decision decision = session.player.stop();
        if (!decision.accepted()) {
            return Receipt.reject(session.actionId, session.parentGroupId, decision.reasonText());
        }
        session.terminal = true;
        session.phase = ActionPlayer.Phase.CANCELLED;
        session.paused = false;
        session.revision++;
        GroupReplayLedger.markTerminal(session.ownerId, session.parentGroupId);
        rememberVisual(player.getUUID(), session.player.drainNotices());
        publish(player, packetFor(session, ActionSyncS2CPacket.CANCEL));
        return accepted(session, false);
    }

    /** 先推进已到时刻的事件。姿态已经自然结束时不再暂停。 */
    public static Receipt pause(ServerPlayer player) {
        String invalidPlayer = validatePlayer(player);
        if (invalidPlayer != null) {
            return Receipt.reject(null, null, invalidPlayer);
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.terminal) {
            return Receipt.reject(session == null ? null : session.actionId, session == null ? null : session.parentGroupId, "没有可暂停的姿态");
        }
        long nowTick = player.serverLevel().getGameTime();
        session.player.advanceEvents(nowTick, 0.0F);
        rememberVisual(player.getUUID(), session.player.drainNotices());
        if (session.player.phase() == ActionPlayer.Phase.FINISHED) {
            session.terminal = true;
            session.paused = false;
            session.phase = ActionPlayer.Phase.FINISHED;
            session.revision++;
            publish(player, packetFor(session, ActionSyncS2CPacket.FINISH));
            return Receipt.reject(session.actionId, session.parentGroupId, "姿态已经结束");
        }
        ActionPlayer.Decision decision = session.player.pause(nowTick);
        if (!decision.accepted()) {
            return Receipt.reject(session.actionId, session.parentGroupId, decision.reasonText());
        }
        session.paused = true;
        session.phase = ActionPlayer.Phase.PAUSED;
        session.frozenSampleSeconds = session.player.sampleSeconds(nowTick, 0.0F);
        session.revision++;
        publish(player, packetFor(session, ActionSyncS2CPacket.PAUSE));
        return accepted(session, false);
    }

    /** 把暂停中的姿态恢复播放。 */
    public static Receipt resume(ServerPlayer player) {
        String invalidPlayer = validatePlayer(player);
        if (invalidPlayer != null) {
            return Receipt.reject(null, null, invalidPlayer);
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.terminal) {
            return Receipt.reject(session == null ? null : session.actionId, session == null ? null : session.parentGroupId, "没有可恢复的姿态");
        }
        long nowTick = player.serverLevel().getGameTime();
        ActionPlayer.Decision decision = session.player.resume(nowTick);
        if (!decision.accepted()) {
            return Receipt.reject(session.actionId, session.parentGroupId, decision.reasonText());
        }
        session.paused = false;
        session.phase = ActionPlayer.Phase.PLAYING;
        session.revision++;
        publish(player, packetFor(session, ActionSyncS2CPacket.RESUME));
        return accepted(session, false);
    }

    /** 收束自然播完的姿态。玩家离开原维度或不再存活时退役会话。 */
    public static void finishExpired(MinecraftServer server) {
        if (server == null) {
            return;
        }
        GroupReplayLedger.expireAll(server.getTickCount());
        List<UUID> owners = new ArrayList<>(SESSIONS.keySet());
        for (UUID ownerId : owners) {
            Session session = SESSIONS.get(ownerId);
            if (session == null || session.terminal || session.paused) {
                continue;
            }
            ServerLevel level = server.getLevel(session.dimensionKey);
            ServerPlayer player = server.getPlayerList().getPlayer(ownerId);
            if (level == null || player == null || !player.isAlive() || player.serverLevel() != level) {
                if (player != null && level != null) {
                    retire(player, level);
                } else {
                    SESSIONS.remove(ownerId);
                    GroupReplayLedger.clearOwner(ownerId);
                    bumpEpoch(ownerId);
                }
                continue;
            }
            long nowTick = level.getGameTime();
            ActionPlayer.Phase before = session.player.phase();
            session.player.advanceEvents(nowTick, 0.0F);
            rememberVisual(ownerId, session.player.drainNotices());
            if (before == ActionPlayer.Phase.PLAYING && session.player.phase() == ActionPlayer.Phase.FINISHED) {
                session.terminal = true;
                session.paused = false;
                session.phase = ActionPlayer.Phase.FINISHED;
                session.revision++;
                GroupReplayLedger.markTerminal(session.ownerId, session.parentGroupId);
                publish(player, packetFor(session, ActionSyncS2CPacket.FINISH));
            }
        }
    }

    /** 玩家离开时取消姿态并提升纪元，旧同步不再匹配。 */
    public static void retire(ServerPlayer player, ServerLevel level) {
        if (player == null) {
            return;
        }
        Session session = SESSIONS.remove(player.getUUID());
        GroupReplayLedger.clearOwner(player.getUUID());
        if (session != null && level != null) {
            session.terminal = true;
            session.paused = false;
            session.phase = ActionPlayer.Phase.CANCELLED;
            session.revision++;
            sendLevel(level, packetFor(session, ActionSyncS2CPacket.CANCEL));
        }
        bumpEpoch(player.getUUID());
        VISUAL_NOTICES.remove(player.getUUID());
    }

    /** 给后来的观察者一份当前姿态快照。 */
    public static ActionSyncS2CPacket snapshot(ServerPlayer owner) {
        if (owner == null) {
            return null;
        }
        Session session = SESSIONS.get(owner.getUUID());
        if (session == null) {
            return null;
        }
        return packetFor(session, ActionSyncS2CPacket.SNAPSHOT);
    }

    /** 按当前游戏时间取样。没有会话时给空姿态。 */
    public static Pose sample(ServerPlayer player) {
        if (player == null) {
            return new Pose();
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return new Pose();
        }
        return session.player.sample(player.serverLevel().getGameTime(), 0.0F);
    }

    /** 返回只读的取样上下文，不推进姿态。 */
    public static ActionSampleContext inspect(ServerPlayer player) {
        if (player == null) {
            return ActionSampleContext.inactive(null);
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.player == null) {
            return ActionSampleContext.inactive(player.getUUID());
        }
        long gameTime = player.serverLevel().getGameTime();
        return contextFor(session, gameTime, 0.0F);
    }

    /** 停服时清掉全部姿态会话和视觉提示。 */
    public static void releaseServer() {
        SESSIONS.clear();
        EPOCHS.clear();
        SEQUENCES.clear();
        VISUAL_NOTICES.clear();
        GroupReplayLedger.clearAll();
    }

    /** 返回这个主人最近的视觉提示副本。 */
    public static List<String> visualNotices(UUID ownerId) {
        List<String> notices = VISUAL_NOTICES.get(ownerId);
        if (notices == null) {
            return List.of();
        }
        return List.copyOf(notices);
    }

    private static void publish(ServerPlayer owner, ActionSyncS2CPacket packet) {
        ServerLevel level = owner.serverLevel();
        for (ServerPlayer watcher : level.players()) {
            if (watcher == owner || watcher.distanceTo(owner) <= 128.0F) {
                ModNetwork.sendToPlayer(watcher, packet);
            }
        }
    }

    private static void sendLevel(ServerLevel level, ActionSyncS2CPacket packet) {
        for (ServerPlayer watcher : level.players()) {
            ModNetwork.sendToPlayer(watcher, packet);
        }
    }

    private static ActionSyncS2CPacket packetFor(Session session, int kind) {
        int stateCode = switch (session.phase) {
            case PAUSED -> ActionSyncS2CPacket.STATE_PAUSED;
            case FINISHED -> ActionSyncS2CPacket.STATE_FINISHED;
            case CANCELLED -> ActionSyncS2CPacket.STATE_CANCELLED;
            default -> ActionSyncS2CPacket.STATE_PLAYING;
        };
        return new ActionSyncS2CPacket(
                kind,
                stateCode,
                session.ownerId,
                session.entityNetworkId,
                session.dimensionId,
                session.sessionEpoch,
                session.sequence,
                session.startGameTick,
                session.actionId.toString(),
                session.speed,
                session.looping,
                session.blendSeconds,
                session.frozenSampleSeconds,
                session.hasFrozenYaw,
                session.frozenYaw,
                session.parentGroupId != null,
                session.parentGroupId,
                session.revision,
                session.player.clockOffsetTicks(),
                session.authoritativeDurationSeconds,
                session.player.pausedElapsedSeconds(),
                session.player.pausedBlendElapsedSeconds(),
                session.player.pauseBeganTick(),
                session.fallbackActionId == null ? "" : session.fallbackActionId.toString(),
                session.beatIndex,
                session.hasSlashFrame,
                session.directionX,
                session.directionY,
                session.directionZ,
                session.planeX,
                session.planeY,
                session.planeZ);
    }

    private static Receipt open(ServerPlayer player, ResourceLocation actionId, float speed, boolean looping, Float frozenYaw, UUID parentGroupId, ActionCommit commit) {
        if (CooperativeActionIds.OBLIQUE_SLASH.equals(actionId) || CooperativeActionIds.SPIN.equals(actionId)) {
            return Receipt.reject(actionId, parentGroupId, "协同动作由 ActorActionPublisher 播放");
        }
        String invalidPlayer = validatePlayer(player);
        if (invalidPlayer != null) {
            return Receipt.reject(actionId, parentGroupId, invalidPlayer);
        }
        ActionStatus status = ActionRegistry.status(actionId);
        ActionDefinition definition = ActionRegistry.definition(actionId);
        AnimationClip clip = ActionRegistry.clip(actionId);
        if (status == null || definition == null || clip == null || !status.posePlayable()) {
            String reason = status == null ? "没有这个动作" : status.reasonText();
            return Receipt.reject(actionId, parentGroupId, reason);
        }
        ResourceLocation fallbackId = commit != null && commit.fallbackActionId() != null
                ? commit.fallbackActionId()
                : ActionVisualFallback.fallbackFor(actionId);
        if (ActionRegistry.definition(fallbackId) == null) {
            fallbackId = ActionVisualFallback.fallbackFor(actionId);
        }
        Session current = SESSIONS.get(player.getUUID());
        boolean replacing = current != null && !current.terminal;
        long epoch = epochOf(player.getUUID());
        long sequence = nextSequence(player.getUUID());
        long startTick = player.serverLevel().getGameTime();
        ActionPlayer playerState = replacing ? current.player : new ActionPlayer();
        ActionPlayer.Decision decision = replacing
                ? playerState.replace(definition, clip, startTick, startTick, speed, looping, definition.blendSeconds())
                : playerState.play(definition, clip, startTick, speed, looping, definition.blendSeconds());
        if (!decision.accepted()) {
            return Receipt.reject(actionId, parentGroupId, decision.reasonText());
        }
        Session session = new Session();
        session.ownerId = player.getUUID();
        session.entityNetworkId = player.getId();
        session.dimensionId = player.serverLevel().dimension().location().toString();
        session.dimensionKey = player.serverLevel().dimension();
        session.sessionEpoch = epoch;
        session.sequence = sequence;
        session.startGameTick = startTick;
        session.parentGroupId = parentGroupId;
        session.actionId = actionId;
        session.speed = speed;
        session.looping = looping;
        session.blendSeconds = definition.blendSeconds();
        session.paused = false;
        session.frozenSampleSeconds = 0.0F;
        session.hasFrozenYaw = frozenYaw != null && Float.isFinite(frozenYaw);
        session.frozenYaw = session.hasFrozenYaw ? frozenYaw : 0.0F;
        session.terminal = playerState.terminal();
        session.phase = playerState.phase();
        session.player = playerState;
        session.revision = 1;
        session.fallbackActionId = fallbackId;
        session.authoritativeDurationSeconds = definition.durationSeconds();
        session.beatIndex = commit == null ? -1 : commit.beatIndex();
        session.hasSlashFrame = commit != null && commit.hasSlashFrame();
        session.directionX = session.hasSlashFrame ? commit.directionX() : 0.0F;
        session.directionY = session.hasSlashFrame ? commit.directionY() : 0.0F;
        session.directionZ = session.hasSlashFrame ? commit.directionZ() : 0.0F;
        session.planeX = session.hasSlashFrame ? commit.planeX() : 0.0F;
        session.planeY = session.hasSlashFrame ? commit.planeY() : 0.0F;
        session.planeZ = session.hasSlashFrame ? commit.planeZ() : 0.0F;
        session.commit = commit == null ? null : commit.withFallback(fallbackId).withAuthoritativeDuration(definition.durationSeconds());
        SESSIONS.put(player.getUUID(), session);
        rememberVisual(player.getUUID(), playerState.drainNotices());
        publish(player, packetFor(session, session.terminal ? ActionSyncS2CPacket.FINISH : ActionSyncS2CPacket.START));
        return accepted(session, replacing);
    }

    private static ActionSampleContext contextFor(Session session, long gameTime, float partialTick) {
        ActionDefinition definition = session.player.definition();
        boolean active = !session.terminal
                && (session.phase == ActionPlayer.Phase.PLAYING || session.phase == ActionPlayer.Phase.PAUSED);
        Pose pose = session.phase == ActionPlayer.Phase.CANCELLED ? new Pose() : session.player.sample(gameTime, partialTick);
        float elapsed = session.player.unwrappedElapsed(gameTime, partialTick);
        float duration = session.authoritativeDurationSeconds;
        return ActionSampleContext.capture(
                active,
                session.ownerId,
                session.entityNetworkId,
                session.sessionEpoch,
                session.sequence,
                session.parentGroupId,
                session.actionId,
                session.actionId,
                definition == null ? "" : definition.armatureName(),
                session.hasSlashFrame,
                session.directionX,
                session.directionY,
                session.directionZ,
                session.planeX,
                session.planeY,
                session.planeZ,
                ActionSampleContext.normalized(session.looping, elapsed, duration),
                pose,
                session.startGameTick,
                duration,
                elapsed,
                session.beatIndex,
                session.revision,
                session.hasFrozenYaw,
                session.frozenYaw);
    }

    private static Receipt accepted(Session session, boolean replacedPose) {
        return new Receipt(true, "", session.actionId, session.sequence, session.sessionEpoch, session.parentGroupId, session.ownerId,
                replacedPose, true, session.startGameTick, session.authoritativeDurationSeconds, session.fallbackActionId,
                session.beatIndex, session.commit);
    }

    private static void rememberVisual(UUID ownerId, List<String> notices) {
        if (notices == null || notices.isEmpty()) {
            return;
        }
        List<String> stored = VISUAL_NOTICES.computeIfAbsent(ownerId, ignored -> new ArrayList<>());
        stored.addAll(notices);
        if (stored.size() > 32) {
            stored.subList(0, stored.size() - 32).clear();
        }
    }

    private static boolean playable(ResourceLocation actionId) {
        ActionStatus status = ActionRegistry.status(actionId);
        return status != null && status.posePlayable()
                && ActionRegistry.definition(actionId) != null
                && ActionRegistry.clip(actionId) != null;
    }

    public static final class PermitPreparation {
        private final CommitAuthorization.Outcome outcome;
        private final ResourceLocation actionId;
        private final ResourceLocation fallbackId;
        private final String reasonText;

        private PermitPreparation(CommitAuthorization.Outcome outcome, ResourceLocation actionId, ResourceLocation fallbackId, String reasonText) {
            this.outcome = outcome;
            this.actionId = actionId;
            this.fallbackId = fallbackId;
            this.reasonText = reasonText == null ? "" : reasonText;
        }

        private static PermitPreparation blocked(CommitAuthorization.Outcome outcome) {
            String reasonText = switch (outcome) {
                case MISMATCH -> "提交许可和攻击组不一致";
                case DUPLICATE -> "同一攻击组已经播放过";
                case SPENT -> "提交许可已经用过";
                case READY -> "";
            };
            return new PermitPreparation(outcome, null, null, reasonText);
        }

        public CommitAuthorization.Outcome outcome() {
            return this.outcome;
        }

        public ResourceLocation actionId() {
            return this.actionId;
        }

        public ResourceLocation fallbackId() {
            return this.fallbackId;
        }

        public String reasonText() {
            return this.reasonText;
        }
    }

    private static long epochOf(UUID ownerId) {
        return EPOCHS.computeIfAbsent(ownerId, ignored -> 1L);
    }

    private static void bumpEpoch(UUID ownerId) {
        EPOCHS.put(ownerId, epochOf(ownerId) + 1L);
    }

    private static long nextSequence(UUID ownerId) {
        long next = SEQUENCES.getOrDefault(ownerId, 0L) + 1L;
        SEQUENCES.put(ownerId, next);
        return next;
    }

    private static String validatePlayer(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.serverLevel() == null) {
            return "需要存活的服务端玩家";
        }
        return null;
    }

    public static final class Receipt {
        private final boolean accepted;
        private final String reasonText;
        private final ResourceLocation actionId;
        private final long sequence;
        private final long sessionEpoch;
        private final UUID parentGroupId;
        private final UUID ownerId;
        private final boolean replacedPose;
        private final boolean geometryLeftUntouched;
        private final long startGameTick;
        private final float authoritativeDurationSeconds;
        private final ResourceLocation fallbackActionId;
        private final int beatIndex;
        private final ActionCommit commit;

        private Receipt(
                boolean accepted,
                String reasonText,
                ResourceLocation actionId,
                long sequence,
                long sessionEpoch,
                UUID parentGroupId,
                UUID ownerId,
                boolean replacedPose,
                boolean geometryLeftUntouched,
                long startGameTick,
                float authoritativeDurationSeconds,
                ResourceLocation fallbackActionId,
                int beatIndex,
                ActionCommit commit) {
            this.accepted = accepted;
            this.reasonText = reasonText == null ? "" : reasonText;
            this.actionId = actionId;
            this.sequence = sequence;
            this.sessionEpoch = sessionEpoch;
            this.parentGroupId = parentGroupId;
            this.ownerId = ownerId;
            this.replacedPose = replacedPose;
            this.geometryLeftUntouched = geometryLeftUntouched;
            this.startGameTick = startGameTick;
            this.authoritativeDurationSeconds = authoritativeDurationSeconds;
            this.fallbackActionId = fallbackActionId;
            this.beatIndex = beatIndex;
            this.commit = commit;
        }

        private static Receipt reject(ResourceLocation actionId, UUID parentGroupId, String reasonText) {
            return new Receipt(false, reasonText, actionId, 0L, 0L, parentGroupId, null, false, true, 0L, 0.0F, null, -1, null);
        }

        public boolean accepted() {
            return this.accepted;
        }

        public String reasonText() {
            return this.reasonText;
        }

        public ResourceLocation actionId() {
            return this.actionId;
        }

        public long sequence() {
            return this.sequence;
        }

        public long sessionEpoch() {
            return this.sessionEpoch;
        }

        public UUID parentGroupId() {
            return this.parentGroupId;
        }

        /** 与 parentGroupId 是同一个攻击组。 */
        public UUID groupId() {
            return this.parentGroupId;
        }

        public boolean replacedPose() {
            return this.replacedPose;
        }

        public boolean geometryLeftUntouched() {
            return this.geometryLeftUntouched;
        }

        public long startGameTick() {
            return this.startGameTick;
        }

        public float authoritativeDurationSeconds() {
            return this.authoritativeDurationSeconds;
        }

        public ResourceLocation fallbackActionId() {
            return this.fallbackActionId;
        }

        public int beatIndex() {
            return this.beatIndex;
        }

        public ActionCommit commit() {
            return this.commit;
        }
    }

    private static final class Session {
        private UUID ownerId;
        private int entityNetworkId;
        private String dimensionId;
        private ResourceKey<Level> dimensionKey;
        private long sessionEpoch;
        private long sequence;
        private long startGameTick;
        private UUID parentGroupId;
        private ResourceLocation actionId;
        private ResourceLocation fallbackActionId;
        private float authoritativeDurationSeconds;
        private float speed;
        private boolean looping;
        private float blendSeconds;
        private boolean paused;
        private float frozenSampleSeconds;
        private boolean hasFrozenYaw;
        private float frozenYaw;
        private boolean terminal;
        private ActionPlayer.Phase phase;
        private ActionPlayer player;
        private int revision;
        private int beatIndex = -1;
        private boolean hasSlashFrame;
        private float directionX;
        private float directionY;
        private float directionZ;
        private float planeX;
        private float planeY;
        private float planeZ;
        private ActionCommit commit;
    }
}
