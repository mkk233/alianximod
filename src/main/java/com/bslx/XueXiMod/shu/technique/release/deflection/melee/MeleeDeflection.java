package com.bslx.XueXiMod.shu.technique.release.deflection.melee;

import com.bslx.XueXiMod.shu.technique.release.deflection.AttackKey;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionAttack;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionEligibility;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHost;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHosts;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionService;
import com.bslx.XueXiMod.shu.technique.release.deflection.HostContact;
import com.bslx.XueXiMod.shu.technique.release.deflection.hosts.IncomingPassage;
import com.bslx.XueXiMod.shu.technique.release.deflection.hosts.RingDeflectionHost;
import com.bslx.XueXiMod.shu.technique.release.guard.GuardPose;
import com.bslx.XueXiMod.shu.technique.release.guard.GuardStates;
import com.bslx.XueXiMod.shu.technique.release.prison.deflection.PrisonStrike;
import com.bslx.XueXiMod.status.WetnessTiers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 同一次 Player.attack / Mob.doHurtTarget，以及绕开这两处的直接怪物近战。
 * 合法的服务端双方先留下本次尝试，再做几何和报价。没有账的尝试不能提交，也不能扣水。
 * 水环只在这一刀提交成功后把护体摆成水平近战姿态，一次。预留或提交失败不摆。
 */
public final class MeleeDeflection {
    private static final ThreadLocal<ArrayDeque<Pending>> STACK = ThreadLocal.withInitial(ArrayDeque::new);
    private static final AtomicLong CONTACT_IDENTITY = new AtomicLong();

    private MeleeDeflection() {
    }

    /** 旧攻击入口先留下这次近战尝试，再做几何和报价。 */
    public static void arm(LivingEntity attacker, Entity target, boolean restoreFire) {
        if (!legalServerPair(attacker, target)) {
            return;
        }
        LivingEntity victim = (LivingEntity) target;
        AttemptToken token = pushAttempt(attacker.getUUID(), victim.getUUID(), target,
                target.getRemainingFireTicks(), restoreFire);
        if (token == null) {
            return;
        }
        attachBooking(token, attacker, victim);
    }

    /**
     * 补充入口自己的一次尝试。已有同对尝试则什么都不压、也不关外层。
     * 返回真只表示这一帧的账已经提交。调用方负责取消事件。
     */
    public static boolean settleOwnedDirect(LivingEntity attacker, LivingEntity victim) {
        if (!legalServerPair(attacker, victim)) {
            return false;
        }
        return ownAttempt(attacker.getUUID(), victim.getUUID(), victim, victim.getRemainingFireTicks(), false,
                token -> attachBooking(token, attacker, victim));
    }

    /** 已提交的顶层近战才取消原伤害。没有账时为假。 */
    public static boolean cancelCommitted(Entity attacker, Entity target) {
        return commitPending(matching(attacker, target), target);
    }

    /**
     * 只有水环、并且这一刀已经提交成功，才摆水平近战姿态。其他宿主和未提交都不摆。
     */
    public static boolean horizontalAfterCommit(DeflectionHost host, boolean hurtCommitted) {
        return hurtCommitted && host instanceof RingDeflectionHost;
    }

    /** 关闭这对攻击者的顶层尝试。未提交的预留由事务退回。 */
    public static void close(Entity attacker, Entity target) {
        Pending pending = matching(attacker, target);
        if (pending == null) {
            return;
        }
        finish(pending);
    }

    /** 清掉这一拍没收束的近战尝试和事件重入账。 */
    public static void closeStale() {
        DirectMeleeAttempts.discard();
        ArrayDeque<Pending> pending = STACK.get();
        int guard = pending.size();
        while (!pending.isEmpty() && guard >= 0) {
            Pending last = pending.peekLast();
            finish(last);
            if (last != null && !last.retired) {
                break;
            }
            guard--;
        }
        if (pending.isEmpty()) {
            STACK.remove();
        }
    }

    static boolean attemptLive(UUID attackerId, UUID victimId) {
        if (attackerId == null || victimId == null) {
            return false;
        }
        for (Pending pending : STACK.get()) {
            if (attackerId.equals(pending.attackerId) && victimId.equals(pending.victimId)) {
                return true;
            }
        }
        return false;
    }

    static int attemptCount() {
        return STACK.get().size();
    }

    static AttemptToken pushAttempt(UUID attackerId, UUID victimId, Entity struck, int fireTicks, boolean restoreFire) {
        if (attackerId == null || victimId == null || attackerId.equals(victimId)) {
            return null;
        }
        Pending pending = new Pending(attackerId, victimId, struck, fireTicks, restoreFire);
        STACK.get().addLast(pending);
        return new AttemptToken(pending);
    }

    static boolean commitToken(AttemptToken token) {
        if (token == null) {
            return false;
        }
        return commitPending(token.pending, token.pending.struck);
    }

    static void finishToken(AttemptToken token) {
        if (token == null) {
            return;
        }
        finish(token.pending);
    }

    static boolean ownAttempt(UUID attackerId, UUID victimId, Entity struck, int fireTicks, boolean restoreFire,
                              AttemptFill fill) {
        if (attackerId == null || victimId == null || attackerId.equals(victimId)) {
            return false;
        }
        if (attemptLive(attackerId, victimId)) {
            return false;
        }
        AttemptToken token = pushAttempt(attackerId, victimId, struck, fireTicks, restoreFire);
        if (token == null) {
            return false;
        }
        try {
            if (fill != null) {
                fill.accept(token);
            }
            return commitToken(token);
        } finally {
            finishToken(token);
        }
    }

    private static boolean legalServerPair(LivingEntity attacker, Entity target) {
        return attacker != null
                && target instanceof LivingEntity
                && attacker != target
                && attacker.level() instanceof ServerLevel serverLevel
                && !serverLevel.isClientSide();
    }

    private static void attachBooking(AttemptToken token, LivingEntity attacker, LivingEntity victim) {
        if (token == null || token.pending == null || token.pending.booking != null) {
            return;
        }
        if (!(attacker.level() instanceof ServerLevel serverLevel) || serverLevel.isClientSide()) {
            return;
        }
        Vec3 eyeFrom = attacker.getEyePosition();
        AABB bodyBox = victim.getBoundingBox();
        double[] nearestPoint = MeleeLine.closest(eyeFrom.x, eyeFrom.y, eyeFrom.z, bodyBox.minX, bodyBox.minY,
                bodyBox.minZ, bodyBox.maxX, bodyBox.maxY, bodyBox.maxZ);
        Vec3 toward = new Vec3(nearestPoint[0], nearestPoint[1], nearestPoint[2]);
        Entity struck = token.pending.struck == null ? victim : token.pending.struck;
        DeflectionAttack attack = new DeflectionAttack(attacker, struck, victim, DeflectionAttack.Kind.MELEE);
        List<HostContact> orderedContacts = DeflectionHosts.meleeOrdered(serverLevel, attack, eyeFrom, toward,
                serverLevel.getGameTime());
        if (orderedContacts.isEmpty()) {
            return;
        }
        WetnessTiers.Profile profile = WetnessTiers.of(attacker);
        AttackKey attackKey = AttackKey.melee(attacker.getUUID(), victim.getUUID(), CONTACT_IDENTITY.incrementAndGet());
        for (HostContact contact : orderedContacts) {
            DeflectionHost host = contact.host();
            if (host == null || !DeflectionEligibility.allows(host, attack)) {
                continue;
            }
            if (!PrisonStrike.confinesActor(host, attack) && IncomingPassage.letsPass(host.ownerId(), attacker)) {
                continue;
            }
            DeflectionService.Transaction booking = DeflectionService.tryAcquire(
                    host, contact, attackKey, 0f, 0, true, profile);
            if (!booking.acquired()) {
                boolean settled = booking.passedOver();
                booking.close();
                if (settled) {
                    return;
                }
                continue;
            }
            token.pending.attach(attackKey, booking, host, toward.subtract(eyeFrom));
            return;
        }
    }

    private static boolean commitPending(Pending pending, Entity fireTarget) {
        if (pending == null || pending.booking == null) {
            return false;
        }
        pending.booking.commit();
        if (!pending.booking.committed()) {
            return false;
        }
        if (pending.restoreFire && fireTarget != null) {
            fireTarget.setRemainingFireTicks(pending.fireTicks);
        }
        if (!pending.poseChecked) {
            pending.poseChecked = true;
            if (horizontalAfterCommit(pending.host, true)) {
                poseOwner(pending);
            }
        }
        return true;
    }

    private static Pending matching(Entity attacker, Entity target) {
        if (attacker == null || target == null) {
            return null;
        }
        ArrayDeque<Pending> pending = STACK.get();
        if (pending.isEmpty()) {
            return null;
        }
        Pending top = pending.peekLast();
        if (top.closed || !top.attackerId.equals(attacker.getUUID()) || !top.victimId.equals(target.getUUID())) {
            return null;
        }
        return top;
    }

    private static void finish(Pending pending) {
        if (pending == null) {
            return;
        }
        if (pending.booking == null) {
            pending.closed = true;
            pending.retired = true;
            removeFrame(pending);
            return;
        }
        if (!pending.closed) {
            pending.closed = true;
            pending.booking.close();
        }
        if (!pending.retired) {
            pending.retired = DeflectionService.retireAttack(pending.attackKey);
        }
        if (!pending.retired) {
            return;
        }
        removeFrame(pending);
    }

    private static void removeFrame(Pending pending) {
        ArrayDeque<Pending> pendingStack = STACK.get();
        pendingStack.remove(pending);
        if (pendingStack.isEmpty()) {
            STACK.remove();
        }
    }

    interface AttemptFill {
        void accept(AttemptToken token);
    }

    static final class AttemptToken {
        private final Pending pending;

        private AttemptToken(Pending pending) {
            this.pending = pending;
        }
    }

    private static final class Pending {
        private final UUID attackerId;
        private final UUID victimId;
        private final Entity struck;
        private final int fireTicks;
        private final boolean restoreFire;
        private AttackKey attackKey;
        private DeflectionService.Transaction booking;
        private DeflectionHost host;
        private Vec3 incoming;
        private boolean closed;
        private boolean retired;
        private boolean poseChecked;

        private Pending(UUID attackerId, UUID victimId, Entity struck, int fireTicks, boolean restoreFire) {
            this.attackerId = attackerId;
            this.victimId = victimId;
            this.struck = struck;
            this.fireTicks = fireTicks;
            this.restoreFire = restoreFire;
        }

        private void attach(AttackKey attackKey, DeflectionService.Transaction booking, DeflectionHost host,
                            Vec3 incoming) {
            this.attackKey = attackKey;
            this.booking = booking;
            this.host = host;
            this.incoming = incoming;
        }
    }

    private static void poseOwner(Pending pending) {
        try {
            ServerLevel level = pending.host.level();
            if (level == null || level.getServer() == null) {
                return;
            }
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(pending.host.ownerId());
            if (owner == null || owner.getServer() == null) {
                return;
            }
            Vec3 incoming = pending.incoming == null ? GuardPose.worldLevel() : pending.incoming;
            GuardStates.strike(owner, GuardPose.MELEE, incoming);
        } catch (RuntimeException ignored) {
        }
    }
}
