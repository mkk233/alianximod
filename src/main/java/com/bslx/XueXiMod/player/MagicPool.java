package com.bslx.XueXiMod.player;

import com.bslx.XueXiMod.combat.counter.ManaRestoreMath;
import com.bslx.XueXiMod.item.StaffItem;
import com.bslx.XueXiMod.network.ModNetwork;
import com.bslx.XueXiMod.network.ShuSyncPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 魔力池：负责魔力的消耗、回复与查询。
 * 玩家池在 {@link PlayerShuData}，法杖存量在主手物品；本类不读写水势。
 * 扣款由 {@link #trySpendTracked} 和 {@link #spendInto} 当场完成。
 * 最近一笔扣款交给 {@link ManaPaymentReceipt} 退回；往已有收据追加的份额不进那份最近收据。
 * 不负责释放效果。
 */
public final class MagicPool {

    private static final ConcurrentHashMap<UUID, Integer> REGEN_TICKS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, ManaPaymentReceipt> LAST_RECEIPT = new ConcurrentHashMap<>();
    private static final int REGEN_INTERVAL = 20;
    private static final int REGEN_AMOUNT = 5;

    private MagicPool() {
    }

    /** 扣款并在不足时提示。 */
    public static boolean trySpend(ServerPlayer player, int cost) {
        return trySpend(player, cost, true);
    }

    /** 扣款。{@code tellPlayer} 只决定不足时是否提示。 */
    public static boolean trySpend(ServerPlayer player, int cost, boolean tellPlayer) {
        return trySpendTracked(player, cost, tellPlayer);
    }

    /** 玩家池加上主手法杖存量够不够。费用不是正数时视为够。 */
    public static boolean canAfford(ServerPlayer player, int cost) {
        if (cost <= 0) {
            return true;
        }
        if (player == null) {
            return false;
        }
        PlayerShuData data = PlayerShuData.get(player);
        ItemStack main = player.getMainHandItem();
        int bonus = staffBonus(main);
        int staffStored = bonus > 0 ? StaffItem.storedMana(main) : 0;
        return data.getMagic() + staffStored >= cost;
    }

    /** 扣款并记下法杖与玩家池各自出了多少，供同一笔事务退回。 */
    public static boolean trySpendTracked(ServerPlayer player, int cost, boolean tellPlayer) {
        if (player == null) {
            return false;
        }
        if (cost <= 0) {
            LAST_RECEIPT.put(player.getUUID(), ManaPaymentReceipt.committed(player.getUUID(), null, 0, 0));
            return true;
        }
        PlayerShuData data = PlayerShuData.get(player);
        ItemStack main = player.getMainHandItem();
        int bonus = staffBonus(main);
        int staffStored = bonus > 0 ? StaffItem.storedMana(main) : 0;
        int playerMagic = data.getMagic();
        if (playerMagic + staffStored < cost) {
            if (tellPlayer) {
                player.sendSystemMessage(Component.literal("§c法力不足"));
            }
            return false;
        }
        int remaining = cost;
        int takenStaff = 0;
        UUID staffIdentity = null;
        if (staffStored > 0) {
            takenStaff = Math.min(staffStored, remaining);
            staffIdentity = StaffItem.payIdentity(main);
            StaffItem.setStoredMana(main, staffStored - takenStaff);
            remaining -= takenStaff;
        }
        int takenPlayer = 0;
        if (remaining > 0) {
            takenPlayer = remaining;
            data.setMagic(playerMagic - takenPlayer);
            PlayerShuData.save(player, data);
            ModNetwork.sendToPlayer(player, new ShuSyncPacket(data));
        }
        LAST_RECEIPT.put(player.getUUID(), ManaPaymentReceipt.committed(player.getUUID(), staffIdentity, takenStaff, takenPlayer));
        return true;
    }

    /**
     * 往已经存在的父收据上追加一笔。付不起或法杖身份冲突时，池和收据都不变。
     * 不写入 {@link #LAST_RECEIPT}。
     */
    public static boolean spendInto(ManaPaymentReceipt receipt, ServerPlayer player, int cost) {
        if (player == null || receipt == null) {
            return false;
        }
        if (!player.getUUID().equals(receipt.ownerId())) {
            return false;
        }
        if (cost <= 0) {
            return true;
        }
        PlayerShuData data = PlayerShuData.get(player);
        ItemStack main = player.getMainHandItem();
        int bonus = staffBonus(main);
        int staffStored = bonus > 0 ? StaffItem.storedMana(main) : 0;
        int playerMagic = data.getMagic();
        if (playerMagic + staffStored < cost) {
            return false;
        }
        int takenStaff = 0;
        UUID staffIdentity = null;
        int takenPlayer = cost;
        if (staffStored > 0) {
            int proposedStaff = Math.min(staffStored, cost);
            UUID proposedIdentity = StaffItem.payIdentity(main);
            int proposedPlayer = cost - proposedStaff;
            if (receipt.canAcceptSpend(proposedIdentity, proposedStaff, proposedPlayer)) {
                takenStaff = proposedStaff;
                staffIdentity = proposedIdentity;
                takenPlayer = proposedPlayer;
            } else if (playerMagic < cost || !receipt.canAcceptSpend(null, 0, cost)) {
                return false;
            }
        }
        if (takenStaff == 0 && playerMagic < cost) {
            return false;
        }
        if (!receipt.canAcceptSpend(staffIdentity, takenStaff, takenPlayer)) {
            return false;
        }
        if (takenStaff > 0) {
            StaffItem.setStoredMana(main, staffStored - takenStaff);
        }
        if (takenPlayer > 0) {
            data.setMagic(playerMagic - takenPlayer);
            PlayerShuData.save(player, data);
            ModNetwork.sendToPlayer(player, new ShuSyncPacket(data));
        }
        if (!receipt.appendSpend(staffIdentity, takenStaff, takenPlayer)) {
            if (takenStaff > 0) {
                StaffItem.setStoredMana(main, staffStored);
            }
            if (takenPlayer > 0) {
                data.setMagic(playerMagic);
                PlayerShuData.save(player, data);
                ModNetwork.sendToPlayer(player, new ShuSyncPacket(data));
            }
            return false;
        }
        return true;
    }

    public static ManaPaymentReceipt lastReceipt(UUID playerId) {
        return LAST_RECEIPT.get(playerId);
    }

    /** 把已经扣掉的魔力还回玩家池。实际加了多少由 {@link #restorePool} 返回。 */
    public static void restore(ServerPlayer player, int amount) {
        restorePool(player, amount);
    }

    /**
     * 只补玩家池，不填法杖。返回实际增加量。
     * 非正请求、非服务端线程或无效玩家返回 0；池没有变化时不存盘、不同步。
     */
    public static int restorePool(ServerPlayer player, int requested) {
        if (player == null || requested <= 0 || player.isRemoved() || player.level().isClientSide()) {
            return 0;
        }
        MinecraftServer server = player.getServer();
        if (server == null || !server.isSameThread()) {
            return 0;
        }
        PlayerShuData data = PlayerShuData.get(player);
        int before = data.getMagic();
        int granted = ManaRestoreMath.accepted(before, data.getMagicMax(), requested);
        if (granted <= 0) {
            return 0;
        }
        long summed = (long) before + (long) granted;
        int updated = summed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) summed;
        if (updated == before) {
            return 0;
        }
        data.setMagic(updated);
        int after = data.getMagic();
        if (after == before) {
            return 0;
        }
        PlayerShuData.save(player, data);
        ModNetwork.sendToPlayer(player, new ShuSyncPacket(data));
        long actual = (long) after - (long) before;
        if (actual <= 0L) {
            return 0;
        }
        if (actual > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) actual;
    }

    /** 把玩家池和主手法杖存量合成一条客户端提示。上限是池上限加上法杖加成。 */
    public static void showBalance(ServerPlayer player) {
        PlayerShuData data = PlayerShuData.get(player);
        ItemStack main = player.getMainHandItem();
        int current = data.getMagic() + StaffItem.storedMana(main);
        int upperBound = Math.max(1, data.getMagicMax() + staffBonus(main));
        player.displayClientMessage(Component.literal("法力：" + current + "/" + upperBound), true);
    }

    /** 每 {@link #REGEN_INTERVAL} 刻回复一次。先填玩家池，剩余再填主手法杖；法杖没有加成则不填。 */
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

    /** 没拿法杖时用固定回复；拿着时再乘这根法杖的回复效率。 */
    private static int regenAmount(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (!StaffItem.holds(main)) {
            return REGEN_AMOUNT;
        }
        double efficiency = ((StaffItem) main.getItem()).data().magicRecoveryEfficiency();
        return Math.max(0, (int) Math.round(REGEN_AMOUNT * (1.0 + efficiency)));
    }
}
