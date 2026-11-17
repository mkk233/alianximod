package com.bslx.XueXiMod.shu.technique.release.deflection;

import com.bslx.XueXiMod.status.WetnessTiers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

/**
 * 一次接触的报价、占用和预留。
 * 生产入口只在主服务器线程工作。没有注册宿主时收购直接失败，不扣水势。
 */
public final class DeflectionService {
    /** 同一 tick 内，离完整放出点不超过这个距离就不再接回。大于水幕厚度 0.25，尚未进游戏调过。 */
    public static final double SAME_TICK_RELEASE_CLEARANCE = 0.75;

    private static final HashMap<AttackKey, HostKey> OPEN_BY_KEY = new HashMap<>();
    private static final HashMap<UUID, HostKey> OPEN_BY_PROJECTILE = new HashMap<>();
    private static final HashSet<AttackKey> SETTLED = new HashSet<>();
    private static final ArrayList<ReleaseGate> GATES = new ArrayList<>();

    private DeflectionService() {
    }

    /** 按 0 档尝试占用。失败不扣水势。 */
    public static Transaction tryAcquire(DeflectionHost host, HostContact contact, AttackKey attackKey,
                                         float direct, int fullCount, boolean isMelee) {
        return tryAcquire(host, contact, attackKey, direct, fullCount, isMelee, null);
    }

    /**
     * profile 是这次接触已经读好的攻击者档位。null 按 0 档。
     * 同一次收购里的两次报价、预留和提交都用这一份，不再回读潮湿。
     */
    public static Transaction tryAcquire(DeflectionHost host, HostContact contact, AttackKey attackKey,
                                         float direct, int fullCount, boolean isMelee,
                                         WetnessTiers.Profile profile) {
        WetnessTiers.Profile locked = lockProfile(profile);
        DeflectionEconomy.Quote refused = DeflectionEconomy.quote(0f, direct, fullCount, isMelee, locked);
        if (host == null || contact == null || attackKey == null) {
            return Transaction.rejected(refused);
        }
        ServerLevel level = readLevel(host);
        if (level == null || level.getServer() == null || !level.getServer().isSameThread()) {
            return Transaction.rejected(refused);
        }
        if (DeflectionHosts.find(host.key()) != host || host.ownerId() == null) {
            return Transaction.rejected(refused);
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(host.ownerId());
        if (player == null) {
            return Transaction.rejected(refused);
        }
        return open(host, contact, attackKey, direct, fullCount, isMelee, locked,
                new PlayerMomentumPurse(player), true, level.getGameTime());
    }

    /**
     * 这次接触已经不可能再投递，并且事务已经 close 之后，忘掉它的防重。
     * 键还在占用表里就什么都不改。不退预留，也不拆掉同一支箭的占用或离体闸门。
     * 只在主服务器线程调用。
     */
    public static boolean retireAttack(AttackKey key) {
        if (key == null) {
            return true;
        }
        if (OPEN_BY_KEY.containsKey(key)) {
            return false;
        }
        SETTLED.remove(key);
        return true;
    }

    /**
     * 箭已经移除、卸载或换维，并且没有任何未关闭的接触时，忘掉这支箭的防重和离体闸门。
     * 还有占用就什么都不改。其它箭和近战不动。只在主服务器线程调用。
     */
    public static boolean retireProjectile(UUID projectileId) {
        if (projectileId == null) {
            return true;
        }
        if (projectileOpen(projectileId)) {
            return false;
        }
        SETTLED.removeIf(settledKey -> settledKey.projectile() && projectileId.equals(settledKey.projectileId()));
        GATES.removeIf(gate -> projectileId.equals(gate.projectileId));
        return true;
    }

    /** 同一 tick 的完整放出点。弹体若有更准确的出口，可以在提交后再覆盖一次。 */
    public static void armSameTickGate(UUID projectileId, Vec3 releasePoint, long gameTick) {
        if (projectileId == null || !finitePoint(releasePoint)) {
            return;
        }
        expireGates(gameTick);
        GATES.add(new ReleaseGate(projectileId, releasePoint, gameTick));
    }

    static Transaction open(DeflectionHost host, HostContact contact, AttackKey attackKey,
                            float direct, int fullCount, boolean isMelee, MomentumPurse purse,
                            boolean registryRequired, long gameTick) {
        return open(host, contact, attackKey, direct, fullCount, isMelee, null, purse, registryRequired, gameTick);
    }

    static Transaction open(DeflectionHost host, HostContact contact, AttackKey attackKey,
                            float direct, int fullCount, boolean isMelee, WetnessTiers.Profile profile,
                            MomentumPurse purse, boolean registryRequired, long gameTick) {
        WetnessTiers.Profile locked = lockProfile(profile);
        DeflectionEconomy.Quote refused = DeflectionEconomy.quote(0f, direct, fullCount, isMelee, locked);
        if (host == null || contact == null || attackKey == null || purse == null || contact.host() != host) {
            return Transaction.rejected(refused);
        }
        if (!live(host)) {
            return Transaction.rejected(refused);
        }
        if (registryRequired && DeflectionHosts.find(host.key()) != host) {
            return Transaction.rejected(refused);
        }
        if (SETTLED.contains(attackKey) || alreadyClaimed(attackKey) || blockedByGate(attackKey, contact.point(), gameTick)) {
            return Transaction.rejected(DeflectionEconomy.quote(purse.available(), 0f, 0, false, locked), true);
        }
        float fundsNow = purse.available();
        DeflectionEconomy.Quote priced = DeflectionEconomy.quote(fundsNow, direct, fullCount, isMelee, locked);
        float fundsFinal = purse.available();
        if (Math.abs(fundsFinal - fundsNow) > 0.001f) {
            priced = DeflectionEconomy.quote(fundsFinal, direct, fullCount, isMelee, locked);
        }
        if (!live(host) || !priced.engages()) {
            return Transaction.rejected(priced);
        }
        if (registryRequired && DeflectionHosts.find(host.key()) != host) {
            return Transaction.rejected(priced);
        }
        Object reservation = purse.reserve(priced.cost());
        if (reservation == null) {
            return Transaction.rejected(priced);
        }
        if (!live(host) || (registryRequired && DeflectionHosts.find(host.key()) != host)) {
            purse.release(reservation);
            return Transaction.rejected(priced);
        }
        bindClaim(attackKey, host.key());
        return new Transaction(host, contact, attackKey, priced, purse, reservation, registryRequired, gameTick, false);
    }

    static boolean remembers(AttackKey key) {
        return key != null && SETTLED.contains(key);
    }

    static int departureGateCount(UUID projectileId) {
        if (projectileId == null) {
            return 0;
        }
        int gateCount = 0;
        for (ReleaseGate gate : GATES) {
            if (projectileId.equals(gate.projectileId)) {
                gateCount++;
            }
        }
        return gateCount;
    }

    static void clearBook() {
        OPEN_BY_KEY.clear();
        OPEN_BY_PROJECTILE.clear();
        SETTLED.clear();
        GATES.clear();
    }

    static void forgetHosts(Collection<HostKey> removed) {
        if (removed == null || removed.isEmpty()) {
            return;
        }
        HashSet<HostKey> removedKeys = new HashSet<>(removed);
        OPEN_BY_KEY.entrySet().removeIf(entry -> removedKeys.contains(entry.getValue()));
        OPEN_BY_PROJECTILE.entrySet().removeIf(entry -> removedKeys.contains(entry.getValue()));
    }

    private static boolean projectileOpen(UUID projectileId) {
        if (OPEN_BY_PROJECTILE.containsKey(projectileId)) {
            return true;
        }
        for (AttackKey openKey : OPEN_BY_KEY.keySet()) {
            if (openKey.projectile() && projectileId.equals(openKey.projectileId())) {
                return true;
            }
        }
        return false;
    }

    private static boolean alreadyClaimed(AttackKey attackKey) {
        if (OPEN_BY_KEY.containsKey(attackKey)) {
            return true;
        }
        return attackKey.projectile() && OPEN_BY_PROJECTILE.containsKey(attackKey.projectileId());
    }

    private static boolean live(DeflectionHost host) {
        try {
            return host != null && host.active();
        } catch (RuntimeException failure) {
            return false;
        }
    }

    private static void bindClaim(AttackKey attackKey, HostKey hostKey) {
        OPEN_BY_KEY.put(attackKey, hostKey);
        if (attackKey.projectile()) {
            OPEN_BY_PROJECTILE.put(attackKey.projectileId(), hostKey);
        }
    }

    private static void releaseClaim(AttackKey attackKey, HostKey hostKey) {
        if (attackKey == null || hostKey == null) {
            return;
        }
        HostKey held = OPEN_BY_KEY.get(attackKey);
        if (hostKey.equals(held)) {
            OPEN_BY_KEY.remove(attackKey);
        }
        if (!attackKey.projectile()) {
            return;
        }
        HostKey projectileHeld = OPEN_BY_PROJECTILE.get(attackKey.projectileId());
        if (hostKey.equals(projectileHeld)) {
            OPEN_BY_PROJECTILE.remove(attackKey.projectileId());
        }
    }

    private static boolean blockedByGate(AttackKey attackKey, Vec3 point, long gameTick) {
        if (!attackKey.projectile() || !finitePoint(point)) {
            return false;
        }
        expireGates(gameTick);
        double limit = SAME_TICK_RELEASE_CLEARANCE * SAME_TICK_RELEASE_CLEARANCE;
        for (ReleaseGate gate : GATES) {
            if (gate.gameTick != gameTick || !attackKey.projectileId().equals(gate.projectileId)) {
                continue;
            }
            if (gate.point.distanceToSqr(point) <= limit + 1.0E-8) {
                return true;
            }
        }
        return false;
    }

    private static void expireGates(long gameTick) {
        GATES.removeIf(gate -> gate.gameTick != gameTick);
    }

    private static WetnessTiers.Profile lockProfile(WetnessTiers.Profile profile) {
        if (profile == null) {
            return WetnessTiers.ofValue(0);
        }
        return profile;
    }

    private static ServerLevel readLevel(DeflectionHost host) {
        try {
            return host.level();
        } catch (RuntimeException failure) {
            return null;
        }
    }

    private static boolean finitePoint(Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }

    public static final class Transaction implements AutoCloseable {
        private final DeflectionHost host;
        private final HostContact contact;
        private final AttackKey attackKey;
        private final DeflectionEconomy.Quote quote;
        private final MomentumPurse purse;
        private final Object reservation;
        private final boolean registryRequired;
        private final long gameTick;
        private final boolean passedOver;
        private boolean submitted;
        private boolean closed;

        private Transaction(DeflectionHost host, HostContact contact, AttackKey attackKey, DeflectionEconomy.Quote quote,
                            MomentumPurse purse, Object reservation, boolean registryRequired, long gameTick,
                            boolean passedOver) {
            this.host = host;
            this.contact = contact;
            this.attackKey = attackKey;
            this.quote = quote;
            this.purse = purse;
            this.reservation = reservation;
            this.registryRequired = registryRequired;
            this.gameTick = gameTick;
            this.passedOver = passedOver;
        }

        private static Transaction rejected(DeflectionEconomy.Quote quote) {
            return rejected(quote, false);
        }

        private static Transaction rejected(DeflectionEconomy.Quote quote, boolean passedOver) {
            DeflectionEconomy.Quote settled = quote == null
                    ? DeflectionEconomy.quote(0f, 0f, 0, false)
                    : quote;
            return new Transaction(null, null, null, settled, null, null, false, 0L, passedOver);
        }

        public boolean acquired() {
            return this.reservation != null && this.purse != null;
        }

        /** 占用、已结算序号或同 tick 离体闸门。不要偏转，也不要再套这个宿主的旧比例。 */
        public boolean passedOver() {
            return this.passedOver;
        }

        /** 水势预留是否已经提交。 */
        public boolean committed() {
            return this.submitted;
        }

        /** 这次报价。未成交时仍可读取拒绝结果。 */
        public DeflectionEconomy.Quote quote() {
            return this.quote;
        }

        /** 宿主仍有效时提交预留。没提交成功时留给 close 退回。 */
        public void commit() {
            if (this.closed || this.submitted || this.reservation == null || this.purse == null || this.host == null) {
                return;
            }
            if (!live(this.host)) {
                return;
            }
            if (this.registryRequired && DeflectionHosts.find(this.host.key()) != this.host) {
                return;
            }
            if (!this.purse.commit(this.reservation)) {
                return;
            }
            this.submitted = true;
            if (this.attackKey != null) {
                SETTLED.add(this.attackKey);
            }
            if (this.quote != null && this.quote.spendsFullAttempt() && this.attackKey != null && this.attackKey.projectile()) {
                Vec3 point = this.contact == null ? null : this.contact.point();
                armSameTickGate(this.attackKey.projectileId(), point, this.gameTick);
            }
        }

        @Override
        /** 未提交的预留在这里退回，并放掉这次占用。 */
        public void close() {
            if (this.closed) {
                return;
            }
            this.closed = true;
            if (this.reservation != null && this.purse != null && !this.submitted) {
                this.purse.release(this.reservation);
            }
            if (this.host != null) {
                releaseClaim(this.attackKey, this.host.key());
            }
        }
    }

    private static final class ReleaseGate {
        private final UUID projectileId;
        private final Vec3 point;
        private final long gameTick;

        private ReleaseGate(UUID projectileId, Vec3 point, long gameTick) {
            this.projectileId = projectileId;
            this.point = point;
            this.gameTick = gameTick;
        }
    }
}
