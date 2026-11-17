package com.bslx.XueXiMod.status.wood.vine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/** 野蔷薇花谱。只记在玩家持久数据上，不写武器一次性附魔。 */
public final class VineMarkSpectrum {
    private static final String ROOT = "xueximod_rose_spectrum";
    private static final String EXPIRE = "ExpireGameTime";
    private static final String PENDING = "PendingCommit";
    private static final String PENDING_MOST = "PendingMost";
    private static final String PENDING_LEAST = "PendingLeast";

    private VineMarkSpectrum() {
    }

    public static void armPending(ServerPlayer player, UUID receiptId) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        data.putBoolean(pendingKey(), true);
        if (receiptId == null) {
            data.remove(pendingMostKey());
            data.remove(pendingLeastKey());
            return;
        }
        data.putLong(pendingMostKey(), receiptId.getMostSignificantBits());
        data.putLong(pendingLeastKey(), receiptId.getLeastSignificantBits());
    }

    public static boolean matches(ServerPlayer player, com.bslx.XueXiMod.player.ManaPaymentReceipt receipt) {
        if (player == null || player.level().isClientSide() || receipt == null
                || !player.getPersistentData().getBoolean(pendingKey())) {
            return false;
        }
        CompoundTag data = player.getPersistentData();
        if (!data.contains(pendingMostKey()) || !data.contains(pendingLeastKey())) {
            return false;
        }
        UUID pendingId = new UUID(data.getLong(pendingMostKey()), data.getLong(pendingLeastKey()));
        return pendingId.equals(receipt.receiptId());
    }

    public static void clearPending(ServerPlayer player) {
        if (player == null) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        data.remove(pendingKey());
        data.remove(pendingMostKey());
        data.remove(pendingLeastKey());
    }

    public static void clearPending(ServerPlayer player, UUID receiptId) {
        if (receiptId == null || !matches(player, receiptId)) {
            return;
        }
        clearPending(player);
    }

    private static boolean matches(ServerPlayer player, UUID receiptId) {
        if (player == null || player.level().isClientSide() || receiptId == null
                || !player.getPersistentData().getBoolean(pendingKey())) {
            return false;
        }
        CompoundTag data = player.getPersistentData();
        if (!data.contains(pendingMostKey()) || !data.contains(pendingLeastKey())) {
            return false;
        }
        UUID pendingId = new UUID(data.getLong(pendingMostKey()), data.getLong(pendingLeastKey()));
        return pendingId.equals(receiptId);
    }

    public static void refresh(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        clearPending(player);
        long duration = VineMarkBalance.spectrumDurationTicks();
        long gameTime = player.serverLevel().getGameTime();
        player.getPersistentData().putLong(key(), gameTime + duration);
    }

    public static boolean active(Player player) {
        if (player == null || player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        CompoundTag data = serverPlayer.getPersistentData();
        if (!data.contains(key())) {
            return false;
        }
        return serverPlayer.serverLevel().getGameTime() < data.getLong(key());
    }

    private static String key() {
        return ROOT + "." + EXPIRE;
    }

    private static String pendingKey() {
        return ROOT + "." + PENDING;
    }

    private static String pendingMostKey() {
        return ROOT + "." + PENDING_MOST;
    }

    private static String pendingLeastKey() {
        return ROOT + "." + PENDING_LEAST;
    }
}
