package com.bslx.XueXiMod.shu.technique.release.water;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 水刃三拍的内存账。键是玩家和技能槽，载体变了就清这一槽。
 * 不写 NBT，也不碰链游标。准备不推进；成功提交才推进。
 */
public final class WaterBeatLedger {
    /** 距上次成功不超过这么多 tick 才衔接。恰好等于仍衔接，再多一拍回左斜。 */
    public static final long CONTINUE_SPAN = 40L;
    private static final long ABSENT = Long.MIN_VALUE;

    private static final ConcurrentHashMap<SlotKey, BeatTrack> TRACKS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, PreparedBeat> PREPARED = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<SlotKey, HeldTrack> HELD_TRACKS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, HeldPrepared> HELD_PREPARED = new ConcurrentHashMap<>();

    private WaterBeatLedger() {
    }

    public static int prepare(UUID playerId, int skillSlot, String carrierItemKey, UUID groupId, long gameTime) {
        if (playerId == null || groupId == null) {
            return 0;
        }
        String carrier = carrierItemKey == null ? "" : carrierItemKey;
        PreparedBeat existing = PREPARED.get(groupId);
        if (existing != null) {
            return existing.beat;
        }
        BeatTrack track = TRACKS.computeIfAbsent(new SlotKey(playerId, skillSlot), ignored -> new BeatTrack());
        int beat = selectBeat(track, carrier, gameTime);
        PREPARED.put(groupId, new PreparedBeat(playerId, skillSlot, carrier, beat));
        return beat;
    }

    public static boolean commit(UUID groupId, long gameTime) {
        if (groupId == null) {
            return false;
        }
        PreparedBeat prepared = PREPARED.get(groupId);
        if (prepared == null || prepared.committed) {
            return false;
        }
        prepared.committed = true;
        BeatTrack track = TRACKS.computeIfAbsent(new SlotKey(prepared.playerId, prepared.skillSlot), ignored -> new BeatTrack());
        track.carrierItemKey = prepared.carrierItemKey;
        track.nextBeat = Math.floorMod(prepared.beat + 1, 3);
        track.commitGameTime = gameTime;
        return true;
    }

    /**
     * 按已经捕获的父来源准备。提交时用的是这份候选，不再读当时的主手。
     * 同一组重复准备交回同一拍。
     */
    public static int prepareHeld(UUID playerId, CastSource source, UUID groupId, long gameTime) {
        if (playerId == null || source == null || groupId == null) {
            return 0;
        }
        HeldPrepared existing = HELD_PREPARED.get(groupId);
        if (existing != null) {
            return existing.beat;
        }
        HeldTrack track = HELD_TRACKS.computeIfAbsent(new SlotKey(playerId, source.skillSlot()), ignored -> new HeldTrack());
        int beat = selectHeld(track, source.stackRef(), source.generation(), gameTime);
        HELD_PREPARED.put(groupId, new HeldPrepared(playerId, source.skillSlot(), source.stackRef(), source.generation(), beat));
        return beat;
    }

    public static boolean commitHeld(UUID groupId, long gameTime) {
        if (groupId == null) {
            return false;
        }
        HeldPrepared prepared = HELD_PREPARED.get(groupId);
        if (prepared == null || prepared.committed) {
            return false;
        }
        prepared.committed = true;
        HeldTrack track = HELD_TRACKS.computeIfAbsent(new SlotKey(prepared.playerId, prepared.skillSlot), ignored -> new HeldTrack());
        track.stackRef = prepared.stackRef;
        track.generation = prepared.generation;
        track.nextBeat = Math.floorMod(prepared.beat + 1, 3);
        track.commitGameTime = gameTime;
        return true;
    }

    public static void abandonHeld(UUID groupId) {
        if (groupId == null) {
            return;
        }
        HeldPrepared prepared = HELD_PREPARED.get(groupId);
        if (prepared == null || prepared.committed) {
            return;
        }
        HELD_PREPARED.remove(groupId, prepared);
    }

    public static void abandon(UUID groupId) {
        if (groupId == null) {
            return;
        }
        PreparedBeat prepared = PREPARED.get(groupId);
        if (prepared == null || prepared.committed) {
            return;
        }
        PREPARED.remove(groupId, prepared);
    }

    public static void clearPlayer(UUID playerId) {
        if (playerId == null) {
            return;
        }
        TRACKS.keySet().removeIf(slotKey -> playerId.equals(slotKey.playerId));
        PREPARED.entrySet().removeIf(entry -> playerId.equals(entry.getValue().playerId));
        HELD_TRACKS.keySet().removeIf(slotKey -> playerId.equals(slotKey.playerId));
        HELD_PREPARED.entrySet().removeIf(entry -> playerId.equals(entry.getValue().playerId));
    }

    public static void clearAll() {
        TRACKS.clear();
        PREPARED.clear();
        HELD_TRACKS.clear();
        HELD_PREPARED.clear();
    }

    private static int selectBeat(BeatTrack track, String carrier, long gameTime) {
        if (track.carrierItemKey != null && !track.carrierItemKey.equals(carrier)) {
            track.nextBeat = 0;
            track.commitGameTime = ABSENT;
            track.carrierItemKey = carrier;
            return 0;
        }
        track.carrierItemKey = carrier;
        if (track.commitGameTime == ABSENT || gameTime - track.commitGameTime > CONTINUE_SPAN) {
            return 0;
        }
        return Math.floorMod(track.nextBeat, 3);
    }

    private static int selectHeld(HeldTrack track, Object stackRef, int generation, long gameTime) {
        if (track.stackRef != null && (track.stackRef != stackRef || track.generation != generation)) {
            track.nextBeat = 0;
            track.commitGameTime = ABSENT;
            track.stackRef = stackRef;
            track.generation = generation;
            return 0;
        }
        track.stackRef = stackRef;
        track.generation = generation;
        if (track.commitGameTime == ABSENT || gameTime - track.commitGameTime > CONTINUE_SPAN) {
            return 0;
        }
        return Math.floorMod(track.nextBeat, 3);
    }

    private static final class SlotKey {
        private final UUID playerId;
        private final int skillSlot;

        private SlotKey(UUID playerId, int skillSlot) {
            this.playerId = playerId;
            this.skillSlot = skillSlot;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof SlotKey slotKey)) {
                return false;
            }
            return skillSlot == slotKey.skillSlot && playerId.equals(slotKey.playerId);
        }

        @Override
        public int hashCode() {
            return playerId.hashCode() * 31 + skillSlot;
        }
    }

    private static final class BeatTrack {
        private int nextBeat;
        private long commitGameTime = ABSENT;
        private String carrierItemKey;
    }

    private static final class PreparedBeat {
        private final UUID playerId;
        private final int skillSlot;
        private final String carrierItemKey;
        private final int beat;
        private boolean committed;

        private PreparedBeat(UUID playerId, int skillSlot, String carrierItemKey, int beat) {
            this.playerId = playerId;
            this.skillSlot = skillSlot;
            this.carrierItemKey = carrierItemKey;
            this.beat = beat;
        }
    }

    private static final class HeldTrack {
        private int nextBeat;
        private long commitGameTime = ABSENT;
        private Object stackRef;
        private int generation;
    }

    private static final class HeldPrepared {
        private final UUID playerId;
        private final int skillSlot;
        private final Object stackRef;
        private final int generation;
        private final int beat;
        private boolean committed;

        private HeldPrepared(UUID playerId, int skillSlot, Object stackRef, int generation, int beat) {
            this.playerId = playerId;
            this.skillSlot = skillSlot;
            this.stackRef = stackRef;
            this.generation = generation;
            this.beat = beat;
        }
    }
}
