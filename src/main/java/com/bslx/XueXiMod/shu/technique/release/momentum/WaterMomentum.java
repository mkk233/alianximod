package com.bslx.XueXiMod.shu.technique.release.momentum;

import com.bslx.XueXiMod.network.ModNetwork;
import com.bslx.XueXiMod.network.WaterMomentumNotice;
import com.bslx.XueXiMod.player.PlayerShuData;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

/**
 * 水势账。和魔力分开，0 到 100。
 * 余额在 {@link com.bslx.XueXiMod.player.PlayerShuData}；还没提交的预留只留在本类的持有列表。
 * {@link #reserve} 预留，{@link #commit}、{@link #commitReserved} 和 {@link #settle} 扣余额，
 * {@link #release}、{@link #releaseAll} 只取消预留。可用额是余额减去还没提交的预留。
 * 同一笔只能提交一次，取消不退已提交的。本类不读、不写魔力。
 */
public final class WaterMomentum {
    public static final float UPPER_BOUND = 100f;

    private static final ArrayList<Hold> HOLDS = new ArrayList<>();

    private WaterMomentum() {
    }

    /** 余额减去这位玩家仍处于预留状态的持有额。 */
    public static float available(ServerPlayer player) {
        if (player == null) {
            return 0f;
        }
        float reserved = 0f;
        UUID playerId = player.getUUID();
        for (Hold hold : HOLDS) {
            if (hold.state == Hold.State.RESERVED && hold.playerId.equals(playerId)) {
                reserved += hold.amount;
            }
        }
        float remaining = PlayerShuData.get(player).getWaterMomentum() - reserved;
        if (!Float.isFinite(remaining) || remaining <= 0f) {
            return 0f;
        }
        return remaining;
    }

    /** 预留一笔。不是正数时直接记成已提交；超过可用额则拒绝，不进持有列表。 */
    public static Hold reserve(ServerPlayer player, float amount) {
        float requested = amount > 0f && Float.isFinite(amount) ? amount : 0f;
        Hold hold = new Hold(player.getUUID(), requested);
        if (requested <= 0f) {
            hold.state = Hold.State.COMMITTED;
            return hold;
        }
        if (requested > available(player) + 0.001f) {
            hold.state = Hold.State.REJECTED;
            return hold;
        }
        hold.state = Hold.State.RESERVED;
        HOLDS.add(hold);
        return hold;
    }

    /** 把仍属于该玩家的预留提交成扣款。已提交、已释放或持有人不符时不动余额。 */
    public static void commit(ServerPlayer player, Hold hold) {
        if (hold == null || player == null || hold.state != Hold.State.RESERVED) {
            return;
        }
        if (!hold.playerId.equals(player.getUUID())) {
            return;
        }
        hold.state = Hold.State.COMMITTED;
        HOLDS.remove(hold);
        if (hold.amount <= 0f) {
            return;
        }
        store(player, PlayerShuData.get(player).getWaterMomentum() - hold.amount);
    }

    /**
     * 只有这笔仍是该玩家的 RESERVED 并真正扣到余额时返回真。
     * 现有 {@link #commit} 不改返回值，避免旧调用方把“没扣成”当成成功。
     */
    public static boolean commitReserved(ServerPlayer player, Hold hold) {
        if (hold == null || player == null || hold.state != Hold.State.RESERVED) {
            return false;
        }
        if (!hold.playerId.equals(player.getUUID())) {
            return false;
        }
        hold.state = Hold.State.COMMITTED;
        HOLDS.remove(hold);
        if (hold.amount > 0f) {
            store(player, PlayerShuData.get(player).getWaterMomentum() - hold.amount);
        }
        return true;
    }

    /** 只取消仍处于预留的这一笔，不改余额。 */
    public static void release(Hold hold) {
        if (hold == null || hold.state != Hold.State.RESERVED) {
            return;
        }
        hold.state = Hold.State.RELEASED;
        HOLDS.remove(hold);
    }

    /** 取消这位玩家全部未提交预留，不改余额。 */
    public static void releaseAll(UUID playerId) {
        if (playerId == null) {
            return;
        }
        Iterator<Hold> iterator = HOLDS.iterator();
        while (iterator.hasNext()) {
            Hold hold = iterator.next();
            if (hold.playerId.equals(playerId) && hold.state == Hold.State.RESERVED) {
                hold.state = Hold.State.RELEASED;
                iterator.remove();
            }
        }
    }

    /**
     * 同一笔里先扣再返，只写一次余额，只通知一次。
     * 正费用必须严格不大于可用额，不用 0.001 容差把零余额收成免费。
     * 中间不会先写成 0。上限仍由玩家数据夹到 100。
     */
    public static Settlement settle(ServerPlayer player, float charge, float refund) {
        if (player == null) {
            return new Settlement(false, 0F);
        }
        float safeCharge = Float.isFinite(charge) && charge > 0F ? charge : 0F;
        float safeRefund = Float.isFinite(refund) && refund > 0F ? refund : 0F;
        float stored = PlayerShuData.get(player).getWaterMomentum();
        if (safeCharge > 0F && !(available(player) >= safeCharge)) {
            return new Settlement(false, stored);
        }
        storeSettled(player, stored - safeCharge + safeRefund);
        return new Settlement(true, PlayerShuData.get(player).getWaterMomentum());
    }

    /** 实际打出或实际受到的伤害。超出 100 的部分丢掉。 */
    public static void credit(ServerPlayer player, float amount) {
        if (player == null || amount <= 0f || !Float.isFinite(amount)) {
            return;
        }
        store(player, PlayerShuData.get(player).getWaterMomentum() + amount);
    }

    /** 结余有变化就通知客户端，含不大于 0.05 的差额。浮动行仍由客户端按 0.05 过滤。 */
    private static void store(ServerPlayer player, float next) {
        PlayerShuData data = PlayerShuData.get(player);
        float before = data.getWaterMomentum();
        data.setWaterMomentum(next);
        float after = data.getWaterMomentum();
        PlayerShuData.save(player, data);
        float change = after - before;
        if (Float.compare(before, after) != 0) {
            ModNetwork.sendToPlayer(player, new WaterMomentumNotice(change, after));
        }
    }

    /** 入卷和偏转的最终差额即使小于 0.05 也通知一次。 */
    private static void storeSettled(ServerPlayer player, float next) {
        PlayerShuData data = PlayerShuData.get(player);
        float before = data.getWaterMomentum();
        data.setWaterMomentum(next);
        float after = data.getWaterMomentum();
        PlayerShuData.save(player, data);
        if (Float.compare(before, after) != 0) {
            ModNetwork.sendToPlayer(player, new WaterMomentumNotice(after - before, after));
        }
    }

    /** 只读当前结余并发给客户端，不改余额。变化为 0，客户端只更新条、不加浮动行。 */
    public static void syncBalance(ServerPlayer player) {
        if (player == null) {
            return;
        }
        float balance = PlayerShuData.get(player).getWaterMomentum();
        ModNetwork.sendToPlayer(player, new WaterMomentumNotice(0f, balance));
    }

    /** 只记本模组这次拦截真正减掉的伤害，不把别的模组的减伤再记一遍。 */
    public static void creditMitigated(ServerPlayer player, float reduced) {
        credit(player, reduced);
    }

    public static final class Settlement {
        private final boolean paid;
        private final float after;

        Settlement(boolean paid, float after) {
            this.paid = paid;
            this.after = after;
        }

        public boolean paid() {
            return paid;
        }

        public float after() {
            return after;
        }
    }

    public static final class Hold {
        public enum State {
            RESERVED,
            COMMITTED,
            RELEASED,
            REJECTED
        }

        final UUID playerId;
        final float amount;
        State state = State.REJECTED;

        Hold(UUID playerId, float amount) {
            this.playerId = playerId;
            this.amount = amount;
        }

        public boolean accepted() {
            return this.state == State.RESERVED || this.state == State.COMMITTED;
        }

        public boolean reserved() {
            return this.state == State.RESERVED;
        }
    }
}
