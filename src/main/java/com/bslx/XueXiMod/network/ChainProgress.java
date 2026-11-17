package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.item.StaffItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 每条剑技、每根法杖各自记住下一次从哪一发开始。装配变了就回到第一发。 */
final class ChainProgress {
    private static final String STEP = "xueximod_chain_step";
    private static final String SIGN = "xueximod_chain_sign";
    private static final ConcurrentHashMap<UUID, int[]> SWORD_STEP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, String[]> SWORD_SIGN = new ConcurrentHashMap<>();

    private ChainProgress() {
    }

    static int current(ServerPlayer player, ItemStack weapon, int slot, String signature, int stepCount) {
        if (stepCount <= 0) {
            return 0;
        }
        if (StaffItem.holds(weapon)) {
            CompoundTag tag = weapon.getTag();
            if (tag == null || !signature.equals(tag.getString(SIGN))) {
                return 0;
            }
            int cursor = tag.getInt(STEP);
            return cursor >= 0 && cursor < stepCount ? cursor : 0;
        }
        if (slot < 0 || slot > 3) {
            return 0;
        }
        String[] signs = SWORD_SIGN.get(player.getUUID());
        int[] cursors = SWORD_STEP.get(player.getUUID());
        if (signs == null || cursors == null || !signature.equals(signs[slot])) {
            return 0;
        }
        return cursors[slot] >= 0 && cursors[slot] < stepCount ? cursors[slot] : 0;
    }

    static void advance(ServerPlayer player, ItemStack weapon, int slot, String signature, int stepCount) {
        int next = stepCount <= 1 ? 0 : (current(player, weapon, slot, signature, stepCount) + 1) % stepCount;
        if (StaffItem.holds(weapon)) {
            CompoundTag tag = weapon.getOrCreateTag();
            tag.putString(SIGN, signature);
            tag.putInt(STEP, next);
            return;
        }
        if (slot < 0 || slot > 3) {
            return;
        }
        String[] signs = SWORD_SIGN.computeIfAbsent(player.getUUID(), playerId -> new String[4]);
        int[] cursors = SWORD_STEP.computeIfAbsent(player.getUUID(), playerId -> new int[4]);
        signs[slot] = signature;
        cursors[slot] = next;
    }
}
