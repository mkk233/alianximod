package com.bslx.XueXiMod.shu.technique.release.deflection.projectile;

import com.bslx.XueXiMod.mixin.EntityRandomAccessor;
import com.bslx.XueXiMod.shu.technique.release.domain.AttackEnchantmentContext;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainAuraResolver;
import com.bslx.XueXiMod.status.WetnessTiers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.function.IntUnaryOperator;

/**
 * 旋流偏转用的标准成本。不走会扣光余额、也不走 20/15/10 资格门的完整拦截报价。
 * 乘 0.2 是固定规则，不是配置开关。
 */
public final class ProjectileStandardCost {
    public static final float DEFLECT_SHARE = 0.2F;
    private static final double TURN = 0.55D;

    private ProjectileStandardCost() {
    }

    /** 按标准直接成本的固定份额给出旋流价格。 */
    public static float deflectPrice(Projectile projectile, float fallbackStandard) {
        float standard = fallbackStandard;
        if (projectile instanceof AbstractArrow arrow && ArrowSupport.supported(arrow)) {
            float direct = frozenDirect(arrow);
            if (direct > 0F) {
                standard = direct * shooterRatio(arrow);
            }
        }
        if (!Float.isFinite(standard) || standard <= 0F) {
            standard = fallbackStandard;
        }
        if (!Float.isFinite(standard) || standard <= 0F) {
            return 0F;
        }
        return standard * DEFLECT_SHARE;
    }

    /** 当前速度是否还能被水平转向改变。 */
    public static boolean canTurn(Projectile projectile) {
        if (projectile == null) {
            return false;
        }
        Vec3 velocity = projectile.getDeltaMovement();
        if (velocity.length() < 1.0E-4D) {
            return false;
        }
        return yaw(velocity).distanceToSqr(velocity) >= 1.0E-8D;
    }

    /** 能改变非零飞行方向时返回真。调用方必须先付款再调用。 */
    public static boolean deflect(Projectile projectile) {
        if (!canTurn(projectile)) {
            return false;
        }
        Vec3 velocity = projectile.getDeltaMovement();
        Vec3 turned = yaw(velocity);
        projectile.setDeltaMovement(turned);
        projectile.hasImpulse = true;
        if (projectile instanceof AbstractHurtingProjectile hurting) {
            double power = Math.sqrt(hurting.xPower * hurting.xPower
                    + hurting.yPower * hurting.yPower
                    + hurting.zPower * hurting.zPower);
            if (power > 1.0E-6D) {
                Vec3 unit = turned.normalize();
                hurting.xPower = unit.x * power;
                hurting.yPower = unit.y * power;
                hurting.zPower = unit.z * power;
            }
        }
        return true;
    }

    private static Vec3 yaw(Vec3 velocity) {
        double cosine = Math.cos(TURN);
        double sine = Math.sin(TURN);
        return new Vec3(
                velocity.x * cosine - velocity.z * sine,
                velocity.y,
                velocity.x * sine + velocity.z * cosine);
    }

    private static float frozenDirect(AbstractArrow arrow) {
        CompoundTag tag = arrow.getPersistentData();
        if (ArrowBook.hasFrozen(tag)) {
            return ArrowBook.remaining(tag);
        }
        Float pending = ArrowBook.peekPending(arrow.getUUID());
        if (pending != null) {
            ArrowBook.freezeDirect(tag, pending);
            return pending;
        }
        float quoted = quoteOnce(arrow);
        ArrowBook.freezeDirect(tag, quoted);
        return quoted;
    }

    private static float quoteOnce(AbstractArrow arrow) {
        double speed = arrow.getDeltaMovement().length();
        float bonus = DomainAuraResolver.merge(0F, arrow.getOwner(), AttackEnchantmentContext.Kind.ROOT_ARROW);
        int base = ArrowDamageFreeze.ceilBase(speed, arrow.getBaseDamage());
        if (!arrow.isCritArrow() && base <= 0 && bonus <= 0F) {
            return 0F;
        }
        IntUnaryOperator roller = bound -> bound <= 0 ? 0
                : ((EntityRandomAccessor) (Object) arrow).xuexiRandom().nextInt(bound);
        return ArrowDamageFreeze.freeze(speed, arrow.getBaseDamage(), arrow.isCritArrow(), roller, bonus);
    }

    private static float shooterRatio(AbstractArrow arrow) {
        Entity owner = arrow.getOwner();
        if (owner instanceof LivingEntity shooter) {
            return WetnessTiers.of(shooter).arrowCostRatio();
        }
        return WetnessTiers.of(null).arrowCostRatio();
    }
}
