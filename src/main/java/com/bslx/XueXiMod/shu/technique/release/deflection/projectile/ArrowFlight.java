package com.bslx.XueXiMod.shu.technique.release.deflection.projectile;

import com.bslx.XueXiMod.shu.technique.release.barrier.ProjectileBarrierTracker;
import com.bslx.XueXiMod.shu.technique.release.deflection.AttackKey;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionAttack;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionEconomy;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionEligibility;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHost;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHosts;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionService;
import com.bslx.XueXiMod.shu.technique.release.deflection.HostContact;
import com.bslx.XueXiMod.shu.technique.release.deflection.HostKey;
import com.bslx.XueXiMod.shu.technique.release.deflection.hosts.IncomingPassage;
import com.bslx.XueXiMod.shu.technique.release.prison.deflection.PrisonStrike;
import com.bslx.XueXiMod.shu.technique.release.domain.AttackEnchantmentContext;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainAuraResolver;
import com.bslx.XueXiMod.shu.technique.release.momentum.WaterMomentum;
import com.bslx.XueXiMod.status.WetnessTiers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * 在原版穿透循环的每一次查找前决定要不要接这支箭。
 * 水比合法实体和方块更远时不接。完整承接才取消这一拍剩余的 tick。
 */
public final class ArrowFlight {
    private static final int KEEP_LOOKING = 0;
    private static final int STOP_HERE = 1;
    private static final int TAKEN_OVER = 2;

    private ArrowFlight() {
    }

    /** 箭已经离开记下的水体时清掉离体宿主。 */
    public static void refreshExit(AbstractArrow arrow) {
        if (arrow == null || !(arrow.level() instanceof ServerLevel level)) {
            return;
        }
        CompoundTag tag = arrow.getPersistentData();
        HostKey latched = ArrowBook.exitLatch(tag);
        if (latched == null) {
            return;
        }
        double radius = arrow.getBbWidth() * 0.5D;
        if (radius < 0.0D) {
            radius = 0.0D;
        }
        if (!DeflectionHosts.covers(latched, arrow.position(), radius, level.getGameTime())) {
            ArrowBook.clearExit(tag);
            ProjectileBarrierTracker.clearReplacedBody(arrow.getUUID());
        }
    }

    /** 水体更早时尝试收购。完整承接返回真；失败或只部分卸力返回假。 */
    public static boolean consider(AbstractArrow arrow, Vec3 from, Vec3 toward, boolean blockLimited,
                                   EntityHitResult legal, IntUnaryOperator roller) {
        if (!ArrowSupport.supported(arrow) || arrow.isNoPhysics() || arrow.isRemoved() || roller == null) {
            return false;
        }
        if (!(arrow.level() instanceof ServerLevel level) || from == null || toward == null) {
            return false;
        }
        refreshExit(arrow);
        CompoundTag tag = arrow.getPersistentData();
        double radius = arrow.getBbWidth() * 0.5D;
        List<HostContact> ordered = DeflectionHosts.contactsOrdered(level, from, toward, radius, level.getGameTime());
        if (ordered.isEmpty()) {
            ArrowBook.leaveContact(tag, arrow.getUUID());
            return false;
        }
        ArrowBook.stayContact(tag);
        double entityT = 1.0D;
        boolean entityHit = legal != null && legal.getLocation() != null;
        if (entityHit) {
            Vec3 point = legal.getLocation();
            entityT = ArrowSegmentOrder.parameter(from.x, from.y, from.z, toward.x, toward.y, toward.z,
                    point.x, point.y, point.z);
        }
        HostKey latched = ArrowBook.exitLatch(tag);
        boolean frozen = ArrowBook.hasFrozen(tag);
        boolean rolled = false;
        boolean quoted = false;
        float direct = 0f;
        int fullCount = ArrowBook.fullCount(tag);
        if (fullCount < 0 || fullCount >= DeflectionEconomy.FULL_DEFLECTION_LIMIT) {
            return false;
        }
        double handled = ArrowTickState.handled(arrow.getUUID());
        double[] waterTimes = new double[ordered.size()];
        for (int cursor = 0; cursor < ordered.size(); cursor++) {
            waterTimes[cursor] = ordered.get(cursor).segmentT();
        }
        ArrowSegmentOrder.Passage passage = ArrowSegmentOrder.passage(
                waterTimes, blockLimited, entityT, entityHit, handled);
        LivingEntity shooter = arrow.getOwner() instanceof LivingEntity livingShooter ? livingShooter : null;
        Entity struck = entityHit ? legal.getEntity() : null;
        LivingEntity struckLiving = struck instanceof LivingEntity livingTarget ? livingTarget : null;
        DeflectionAttack attack = new DeflectionAttack(shooter, arrow, struckLiving, DeflectionAttack.Kind.PROJECTILE);
        for (int slot : passage.admitted()) {
            HostContact contact = ordered.get(slot);
            DeflectionHost host = contact.host();
            if (host == null || host.key() == null) {
                continue;
            }
            if (latched != null && latched.equals(host.key())) {
                continue;
            }
            if (!DeflectionEligibility.allows(host, attack)) {
                continue;
            }
            if (!PrisonStrike.confinesActor(host, attack) && IncomingPassage.letsPass(host.ownerId(), arrow)) {
                continue;
            }
            WetnessTiers.Profile profile = shooterProfile(arrow);
            if (!afford(level, host, profile)) {
                continue;
            }
            if (!quoted) {
                direct = frozen ? ArrowBook.remaining(tag) : quote(arrow, roller);
                quoted = true;
                rolled = !frozen;
            }
            if (!Float.isFinite(direct) || direct < 0f) {
                return false;
            }
            if (direct == 0f) {
                if (rolled && arrow.isCritArrow() && !ArrowBook.hasFrozen(tag)) {
                    ArrowBook.rememberPending(arrow.getUUID(), 0f);
                }
                return false;
            }
            int outcome = attempt(arrow, tag, host, contact, direct, profile);
            if (outcome == TAKEN_OVER) {
                return true;
            }
            if (outcome == STOP_HERE) {
                if (rolled && !ArrowBook.hasFrozen(tag)) {
                    ArrowBook.rememberPending(arrow.getUUID(), direct);
                }
                return false;
            }
        }
        if (passage.blocked() && entityHit) {
            ArrowTickState.handleThrough(arrow.getUUID(), entityT);
        }
        if (rolled && !ArrowBook.hasFrozen(tag)) {
            ArrowBook.rememberPending(arrow.getUUID(), direct);
        }
        return false;
    }

    /** 用冻结或待定伤害替换原版伤害。都没有时保持原值。 */
    public static float hurtAmount(AbstractArrow arrow, float vanillaAmount) {
        if (arrow == null) {
            return vanillaAmount;
        }
        CompoundTag tag = arrow.getPersistentData();
        if (ArrowBook.hasFrozen(tag)) {
            return ArrowBook.remaining(tag);
        }
        Float pending = ArrowBook.peekPending(arrow.getUUID());
        if (pending != null) {
            return pending;
        }
        return vanillaAmount;
    }

    private static float quote(AbstractArrow arrow, IntUnaryOperator roller) {
        double speed = arrow.getDeltaMovement().length();
        float bonus = DomainAuraResolver.merge(0f, arrow.getOwner(), AttackEnchantmentContext.Kind.ROOT_ARROW);
        int base = ArrowDamageFreeze.ceilBase(speed, arrow.getBaseDamage());
        if (!arrow.isCritArrow() && base <= 0 && bonus <= 0f) {
            return 0f;
        }
        return ArrowDamageFreeze.freeze(speed, arrow.getBaseDamage(), arrow.isCritArrow(), roller, bonus);
    }

    /** 每次接触读当前主人。主人不是生物时按 0 档。死亡生物仍读取。快照只交给这一次事务，不写进箭。 */
    private static WetnessTiers.Profile shooterProfile(AbstractArrow arrow) {
        Entity owner = arrow.getOwner();
        if (owner instanceof LivingEntity shooter) {
            return WetnessTiers.of(shooter);
        }
        return WetnessTiers.of(null);
    }

    private static boolean afford(ServerLevel level, DeflectionHost host, WetnessTiers.Profile profile) {
        if (level.getServer() == null || host.ownerId() == null || profile == null) {
            return false;
        }
        ServerPlayer caster = level.getServer().getPlayerList().getPlayer(host.ownerId());
        if (caster == null) {
            return false;
        }
        float gate = profile.qualifyingFunds();
        if (!Float.isFinite(gate) || gate < 0f) {
            return false;
        }
        return WaterMomentum.available(caster) >= gate;
    }

    private static int attempt(AbstractArrow arrow, CompoundTag tag, DeflectionHost host, HostContact contact,
                               float direct, WetnessTiers.Profile profile) {
        DeflectionService.Transaction booking = null;
        try {
            int serial = ArrowBook.contactSerial(tag);
            booking = DeflectionService.tryAcquire(host, contact,
                    AttackKey.projectile(arrow.getUUID(), serial), direct, ArrowBook.fullCount(tag), false, profile);
            if (!booking.acquired()) {
                return booking.passedOver() ? STOP_HERE : KEEP_LOOKING;
            }
            booking.commit();
            if (!booking.committed() || !ArrowBook.applyCommit(tag, booking.quote(), direct, serial, true)) {
                return STOP_HERE;
            }
            if (!booking.quote().spendsFullAttempt()) {
                ArrowTickState.markPartial(arrow.getUUID());
                ArrowTickState.armSuppress(arrow.getUUID());
                ArrowTickState.handleThrough(arrow.getUUID(), contact.segmentT());
                return STOP_HERE;
            }
            booking.close();
            ProjectileBarrierTracker.takeOver(arrow, host.key().hostId());
            ArrowCaptureBook.start(arrow, host, contact, arrow.getDeltaMovement(), arrow.getDeltaMovement().length());
            return TAKEN_OVER;
        } catch (RuntimeException failure) {
            return STOP_HERE;
        } finally {
            if (booking != null) {
                booking.close();
            }
        }
    }
}
