package com.bslx.XueXiMod.shu.technique.release.prison;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 只记住末地通关这次非死亡结算写出来的湿度。普通死亡不记、不在复活时抄回去。
 * 同一玩家只留一条，Clone 用掉后删除。断线时丢掉，因为即将存档的玩家自己带着结算结果。
 */
public final class PrisonWetTransfer extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String STORAGE_ID = "xueximod_prison_wet_transfer";
    private static final Set<UUID> APPLYING = new HashSet<>();

    private final Map<UUID, Snapshot> snapshots = new HashMap<>();

    private PrisonWetTransfer() {
    }

    /** 记下末地通关这次非死亡结算写出的湿度。同一玩家只留最新一条。 */
    public static void capture(ServerPlayer player) {
        if (player == null || player.getServer() == null) {
            return;
        }
        int amount = com.bslx.XueXiMod.status.WetnessState.read(player);
        int durationTicks = amount <= 0 ? 0 : com.bslx.XueXiMod.status.WetnessState.remainingDuration(player);
        long settledTick = player.getServer().getTickCount();
        PrisonWetTransfer data = storage(player.getServer().overworld());
        data.snapshots.put(player.getUUID(), new Snapshot(player.getUUID(), amount, durationTicks, settledTick));
        data.setDirty();
    }

    /** 把快照按流逝时间写到新玩家并删掉。失败时快照仍留着。 */
    public static void apply(ServerPlayer fresh) {
        if (fresh == null || fresh.getServer() == null) {
            return;
        }
        UUID playerId = fresh.getUUID();
        if (!APPLYING.add(playerId)) {
            return;
        }
        try {
            PrisonWetTransfer data = storage(fresh.getServer().overworld());
            Snapshot snapshot = data.snapshots.get(playerId);
            if (snapshot == null) {
                return;
            }
            long elapsed = fresh.getServer().getTickCount() - snapshot.settledTick();
            if (elapsed < 0L) {
                elapsed = 0L;
            }
            int durationTicks = snapshot.durationTicks();
            if (elapsed > 0L) {
                if (elapsed >= durationTicks) {
                    durationTicks = 0;
                } else {
                    durationTicks -= (int) elapsed;
                }
            }
            if (snapshot.amount() <= 0 || durationTicks <= 0) {
                com.bslx.XueXiMod.status.WetnessState.clear(fresh);
            } else {
                com.bslx.XueXiMod.status.WetnessState.replaceExact(fresh, snapshot.amount(), durationTicks);
            }
            data.snapshots.remove(playerId);
            data.setDirty();
        } catch (RuntimeException failure) {
            LOGGER.error("water prison end-return wet transfer failed for {}", playerId, failure);
        } finally {
            APPLYING.remove(playerId);
        }
    }

    /** 断线时丢掉还没用掉的快照。玩家自己的存档仍保留结算结果。 */
    public static void discard(ServerPlayer player) {
        if (player == null) {
            return;
        }
        net.minecraft.server.MinecraftServer server = player.getServer();
        if (server == null) {
            server = ServerLifecycleHooks.getCurrentServer();
        }
        if (server == null) {
            return;
        }
        PrisonWetTransfer data = storage(server.overworld());
        if (data.snapshots.remove(player.getUUID()) != null) {
            data.setDirty();
        }
    }

    private static PrisonWetTransfer storage(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PrisonWetTransfer::load, PrisonWetTransfer::fresh, STORAGE_ID);
    }

    public static PrisonWetTransfer fresh() {
        return new PrisonWetTransfer();
    }

    public static PrisonWetTransfer load(CompoundTag tag) {
        PrisonWetTransfer data = new PrisonWetTransfer();
        ListTag list = tag.getList("snapshots", Tag.TAG_COMPOUND);
        for (int cursor = 0; cursor < list.size(); cursor++) {
            CompoundTag snapshotTag = list.getCompound(cursor);
            if (!snapshotTag.hasUUID("playerId")) {
                continue;
            }
            UUID playerId = snapshotTag.getUUID("playerId");
            data.snapshots.put(playerId, new Snapshot(
                    playerId,
                    snapshotTag.getInt("amount"),
                    snapshotTag.getInt("durationTicks"),
                    snapshotTag.getLong("settledTick")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Snapshot snapshot : this.snapshots.values()) {
            CompoundTag snapshotTag = new CompoundTag();
            snapshotTag.putUUID("playerId", snapshot.playerId());
            snapshotTag.putInt("amount", snapshot.amount());
            snapshotTag.putInt("durationTicks", snapshot.durationTicks());
            snapshotTag.putLong("settledTick", snapshot.settledTick());
            list.add(snapshotTag);
        }
        tag.put("snapshots", list);
        return tag;
    }

    public record Snapshot(UUID playerId, int amount, int durationTicks, long settledTick) {
    }
}
