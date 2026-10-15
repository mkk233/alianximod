package com.bslx.XueXiMod.player;

import com.bslx.XueXiMod.item.StaffItem;
import com.bslx.XueXiMod.network.ModNetwork;
import com.bslx.XueXiMod.network.ShuSyncPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 魔力池：负责魔力的消耗、回复与查询
 * 不负责释放效果
 */
public final class MagicPool {

    private static final ConcurrentHashMap<UUID, Integer> REGEN_TICKS = new ConcurrentHashMap<>();
    private static final int REGEN_INTERVAL = 20;
    private static final int REGEN_AMOUNT = 5;

    private MagicPool() {
    }

    public static boolean trySpend(ServerPlayer player, int cost) {
        if (cost <= 0) {
            return true;
        }
        PlayerShuData data = PlayerShuData.get(player);
        ItemStack main = player.getMainHandItem();
        int bonus = staffBonus(main);
        int staffStored = bonus > 0 ? StaffItem.storedMana(main) : 0;
        int playerMagic = data.getMagic();
        if (playerMagic + staffStored < cost) {
            player.sendSystemMessage(Component.literal("§c魔力不足"));
            return false;
        }
        int remaining = cost;
        if (staffStored > 0) {
            int take = Math.min(staffStored, remaining);
            StaffItem.setStoredMana(main, staffStored - take);
            remaining -= take;
        }
        if (remaining > 0) {
            data.setMagic(playerMagic - remaining);
            PlayerShuData.save(player, data);
            ModNetwork.sendToPlayer(player, new ShuSyncPacket(data));
        }
        return true;
    }

    public static void showBalance(ServerPlayer player) {
        PlayerShuData data = PlayerShuData.get(player);
        ItemStack main = player.getMainHandItem();
        int current = data.getMagic() + StaffItem.storedMana(main);
        int max = Math.max(1, data.getMagicMax() + staffBonus(main));
        player.displayClientMessage(Component.literal("魔力：" + current + "/" + max), true);
    }

    public static void tickRegen(ServerPlayer player) {
        int ticks = REGEN_TICKS.merge(player.getUUID(), 1, Integer::sum);
        if (ticks < REGEN_INTERVAL) {
            return;
        }
        REGEN_TICKS.put(player.getUUID(), 0);

        PlayerShuData data = PlayerShuData.get(player);
        ItemStack main = player.getMainHandItem();
        int before = data.getMagic();
        int amount = regenAmount(player);

        int playerMagic = Math.min(before, data.getMagicMax());
        int playerFill = Math.min(amount, Math.max(0, data.getMagicMax() - playerMagic));
        playerMagic += playerFill;
        amount -= playerFill;

        int bonus = staffBonus(main);
        int staffBefore = StaffItem.storedMana(main);
        if (amount > 0 && bonus > 0 && staffBefore < bonus) {
            StaffItem.setStoredMana(main, staffBefore + Math.min(amount, bonus - staffBefore));
        }

        boolean playerChanged = playerMagic != before;

        if (playerChanged) {
            data.setMagic(playerMagic);
            PlayerShuData.save(player, data);
            ModNetwork.sendToPlayer(player, new ShuSyncPacket(data));
        }
    }

    public static void forget(ServerPlayer player) {
        REGEN_TICKS.remove(player.getUUID());
    }

    private static int staffBonus(ItemStack main) {
        return StaffItem.holds(main) ? ((StaffItem) main.getItem()).data().maxManaBonus() : 0;
    }

    private static int regenAmount(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (!StaffItem.holds(main)) {
            return REGEN_AMOUNT;
        }
        double efficiency = ((StaffItem) main.getItem()).data().magicRecoveryEfficiency();
        return Math.max(0, (int) Math.round(REGEN_AMOUNT * (1.0 + efficiency)));
    }
}
