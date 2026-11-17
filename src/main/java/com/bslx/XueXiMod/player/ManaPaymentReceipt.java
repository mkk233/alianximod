package com.bslx.XueXiMod.player;

import com.bslx.XueXiMod.item.StaffItem;
import com.bslx.XueXiMod.network.ModNetwork;
import com.bslx.XueXiMod.network.ShuSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * 一次扣款的真实来源。退款回到原来的法杖和玩家池，不送给后来手上的另一根法杖。
 * 预扣留在原余额里。按目标追加的费用进单独口袋，保留或退回时先动这只口袋。
 * 玩家池和法杖存量都由本收据按记录退回；不读写水势。
 */
public final class ManaPaymentReceipt {
    public enum State {
        COMMITTED,
        ROLLED_BACK
    }

    private final UUID playerId;
    /** 预扣已带来源时在构造时写下。零预扣收据等到第一笔法杖扣款再绑定，之后不再改。 */
    private UUID staffIdentity;
    private int leftStaff;
    private int leftPlayer;
    private int dynamicStaff;
    private int dynamicPlayer;
    private State state;
    private boolean roseCommitted;
    private final UUID receiptId;

    private ManaPaymentReceipt(UUID playerId, UUID staffIdentity, int fromStaff, int fromPlayer) {
        this.playerId = playerId;
        this.staffIdentity = staffIdentity;
        this.leftStaff = Math.max(0, fromStaff);
        this.leftPlayer = Math.max(0, fromPlayer);
        this.state = State.COMMITTED;
        this.receiptId = UUID.randomUUID();
    }

    /** 当场扣款并取回刚记下的收据。费用不是正数时返回零额收据，扣不成时返回空。 */
    public static ManaPaymentReceipt charge(ServerPlayer player, int cost, boolean tellPlayer) {
        if (player == null || cost <= 0) {
            return new ManaPaymentReceipt(player == null ? null : player.getUUID(), null, 0, 0);
        }
        if (!MagicPool.trySpendTracked(player, cost, tellPlayer)) {
            return null;
        }
        return MagicPool.lastReceipt(player.getUUID());
    }

    /** 把这张收据上还没划走的全部份额退回。 */
    public void refund(ServerPlayer player) {
        refundShare(player, leftStaff + leftPlayer + dynamicStaff + dynamicPlayer);
    }

    /** 只退还没提交的那一截。目标口袋先退，剩下的才按预扣余额的比例回去。 */
    public void refundShare(ServerPlayer player, int share) {
        if (player == null || state != State.COMMITTED || share <= 0 || !player.getUUID().equals(playerId)) {
            return;
        }
        int available = leftStaff + leftPlayer + dynamicStaff + dynamicPlayer;
        if (available <= 0) {
            state = State.ROLLED_BACK;
            return;
        }
        ShareSplit returned = drainShare(Math.min(share, available));
        if (returned.playerPart > 0) {
            PlayerShuData data = PlayerShuData.get(player);
            int restored = Math.min(data.getMagicMax(), data.getMagic() + returned.playerPart);
            if (restored != data.getMagic()) {
                data.setMagic(restored);
                PlayerShuData.save(player, data);
                ModNetwork.sendToPlayer(player, new ShuSyncPacket(data));
            }
        }
        if (returned.staffPart > 0 && staffIdentity != null) {
            ItemStack staff = findStaff(player, staffIdentity);
            if (staff != null) {
                int cap = ((StaffItem) staff.getItem()).data().maxManaBonus();
                int stored = StaffItem.storedMana(staff);
                StaffItem.setStoredMana(staff, Math.min(cap, stored + returned.staffPart));
            }
        }
        if (leftStaff + leftPlayer + dynamicStaff + dynamicPlayer <= 0) {
            state = State.ROLLED_BACK;
        }
        if (returned.total() > 0) {
            MagicPool.showBalance(player);
        }
        com.bslx.XueXiMod.status.wood.vine.VineMarkSpectrum.clearPending(player, receiptId);
    }

    /**
     * 把已经生效的份额从可退余额划走。
     * 先划目标口袋，再划预扣余额。不退回玩家或法杖，也不把收据标成已退回。
     */
    public void retainShare(int share) {
        if (state != State.COMMITTED || share <= 0) {
            return;
        }
        int available = leftStaff + leftPlayer + dynamicStaff + dynamicPlayer;
        if (available <= 0) {
            return;
        }
        drainShare(Math.min(share, available));
    }

    public UUID ownerId() {
        return playerId;
    }

    public UUID receiptId() {
        return receiptId;
    }

    /** 退款会找的那根法杖。还没从法杖扣过时为空。 */
    public UUID paymentStaff() {
        return staffIdentity;
    }

    public int prepaidBalance() {
        return leftStaff + leftPlayer;
    }

    /** 追加口袋里还没划走的法杖份额和玩家池份额。 */
    public int dynamicBalance() {
        return dynamicStaff + dynamicPlayer;
    }

    /** 追加前先问。法杖身份和已有收据冲突时，收据与魔力池都不应改。 */
    public boolean canAcceptSpend(UUID incomingStaff, int staffTaken, int playerTaken) {
        if (state != State.COMMITTED || staffTaken < 0 || playerTaken < 0) {
            return false;
        }
        if (staffTaken == 0 && playerTaken == 0) {
            return true;
        }
        return staffTaken <= 0 || (incomingStaff != null && (staffIdentity == null || staffIdentity.equals(incomingStaff)));
    }

    /** 把新扣到的法杖和玩家池份额记进目标口袋。不替换上次收据。 */
    public boolean appendSpend(UUID incomingStaff, int staffTaken, int playerTaken) {
        if (!canAcceptSpend(incomingStaff, staffTaken, playerTaken)) {
            return false;
        }
        if (staffTaken > 0 && staffIdentity == null) {
            staffIdentity = incomingStaff;
        }
        dynamicStaff += staffTaken;
        dynamicPlayer += playerTaken;
        return true;
    }

    public State state() {
        return state;
    }

    public boolean roseCommitOnce() {
        if (this.roseCommitted) {
            return false;
        }
        this.roseCommitted = true;
        return true;
    }

    /** 用已经扣下的玩家池和法杖份额做一张已提交收据。 */
    public static ManaPaymentReceipt committed(UUID playerId, UUID staffIdentity, int fromStaff, int fromPlayer) {
        return new ManaPaymentReceipt(playerId, staffIdentity, fromStaff, fromPlayer);
    }

    /** 先从追加口袋划，再从预扣余额划。两段各自按法杖和玩家池的现有比例拆。 */
    private ShareSplit drainShare(int share) {
        int remaining = share;
        ShareSplit dynamicSplit = splitShare(dynamicStaff, dynamicPlayer, remaining);
        dynamicStaff -= dynamicSplit.staffPart;
        dynamicPlayer -= dynamicSplit.playerPart;
        remaining -= dynamicSplit.total();
        ShareSplit prepaidSplit = splitShare(leftStaff, leftPlayer, remaining);
        leftStaff -= prepaidSplit.staffPart;
        leftPlayer -= prepaidSplit.playerPart;
        return new ShareSplit(dynamicSplit.staffPart + prepaidSplit.staffPart,
                dynamicSplit.playerPart + prepaidSplit.playerPart);
    }

    /** 按法杖份额占这一段的比例拆。玩家池不够时把差额改记到法杖，反过来也一样。 */
    private static ShareSplit splitShare(int staffPool, int playerPool, int share) {
        int total = staffPool + playerPool;
        if (share <= 0 || total <= 0) {
            return new ShareSplit(0, 0);
        }
        int giving = Math.min(share, total);
        int staffPart = (int) (((long) giving * staffPool) / total);
        int playerPart = giving - staffPart;
        if (playerPart > playerPool) {
            staffPart += playerPart - playerPool;
            playerPart = playerPool;
        }
        if (staffPart > staffPool) {
            staffPart = staffPool;
            playerPart = giving - staffPart;
        }
        return new ShareSplit(staffPart, playerPart);
    }

    private static final class ShareSplit {
        final int staffPart;
        final int playerPart;

        ShareSplit(int staffPart, int playerPart) {
            this.staffPart = staffPart;
            this.playerPart = playerPart;
        }

        int total() {
            return staffPart + playerPart;
        }
    }

    /** 按扣款时记下的身份找法杖。找不到就退不回法杖份额。 */
    private static ItemStack findStaff(ServerPlayer player, UUID staffIdentity) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (StaffItem.holds(stack) && staffIdentity.equals(StaffItem.payIdentity(stack))) {
                return stack;
            }
        }
        return null;
    }
}
