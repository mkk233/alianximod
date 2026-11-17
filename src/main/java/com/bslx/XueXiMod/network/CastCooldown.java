package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.item.StaffItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 剑技四个链条各自记冷却。法杖的冷却写在这根法杖上，不占用剑技槽，也不跟另一根法杖共用。
 */
public final class CastCooldown {
    private static final String STAFF_READY = "xueximod_cast_ready";
    private static final String STAFF_SESSION = "xueximod_cast_session";
    private static final ConcurrentHashMap<UUID, int[]> SWORD_READY = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> EXECUTING = new ConcurrentHashMap<>();
    private static long castSession = System.nanoTime();
    /** 临时测试。为真时不看重放时刻，正在结算的那一发仍占着执行锁。 */
    private static boolean cooldownIgnored = false;

    private CastCooldown() {
    }

    /** 新的一局服务器开始。法杖上旧的绝对 tick 不再当成冷却。 */
    public static void beginSession() {
        castSession = System.nanoTime();
        cooldownIgnored = false;
        SWORD_READY.clear();
        EXECUTING.clear();
        HoldRepeat.clear();
    }

    public static void endSession() {
        cooldownIgnored = false;
        SWORD_READY.clear();
        EXECUTING.clear();
        HoldRepeat.clear();
    }

    public static boolean cooldownIgnored() {
        return cooldownIgnored;
    }

    /** 打开时清掉已经记上的重放时刻。正在执行的锁不动。 */
    public static void setCooldownIgnored(boolean ignored) {
        cooldownIgnored = ignored;
        if (!ignored) {
            return;
        }
        castSession = System.nanoTime();
        SWORD_READY.clear();
    }

    public static String carrierKey(ServerPlayer player, ItemStack weapon, int slot) {
        if (StaffItem.holds(weapon)) {
            return "staff:" + StaffItem.payIdentity(weapon);
        }
        return "sword:" + player.getUUID() + ":" + slot;
    }

    public static boolean acquire(String carrierKey) {
        if (carrierKey == null) {
            return false;
        }
        return EXECUTING.putIfAbsent(carrierKey, Boolean.TRUE) == null;
    }

    public static void release(String carrierKey) {
        if (carrierKey != null) {
            EXECUTING.remove(carrierKey);
        }
    }

    public static boolean busy(ServerPlayer player, ItemStack weapon, int slot) {
        if (EXECUTING.containsKey(carrierKey(player, weapon, slot))) {
            return true;
        }
        if (cooldownIgnored) {
            return false;
        }
        int gameTick = player.getServer().getTickCount();
        if (StaffItem.holds(weapon)) {
            return gameTick < staffReady(weapon);
        }
        if (slot < 0 || slot > 3) {
            return true;
        }
        int[] readyTicks = SWORD_READY.get(player.getUUID());
        return readyTicks != null && gameTick < readyTicks[slot];
    }

    public static void mark(ServerPlayer player, ItemStack weapon, int slot, int untilTick) {
        if (cooldownIgnored) {
            return;
        }
        if (StaffItem.holds(weapon)) {
            CompoundTag tag = weapon.getOrCreateTag();
            if (tag.getLong(STAFF_SESSION) != castSession || untilTick > staffReady(weapon)) {
                tag.putLong(STAFF_SESSION, castSession);
                tag.putInt(STAFF_READY, untilTick);
            }
            return;
        }
        if (slot < 0 || slot > 3) {
            return;
        }
        int[] readyTicks = SWORD_READY.computeIfAbsent(player.getUUID(), playerId -> new int[4]);
        if (untilTick > readyTicks[slot]) {
            readyTicks[slot] = untilTick;
        }
    }

    private static int staffReady(ItemStack weapon) {
        CompoundTag tag = weapon.getTag();
        if (tag == null || !tag.contains(STAFF_READY) || tag.getLong(STAFF_SESSION) != castSession) {
            return 0;
        }
        return tag.getInt(STAFF_READY);
    }
}
