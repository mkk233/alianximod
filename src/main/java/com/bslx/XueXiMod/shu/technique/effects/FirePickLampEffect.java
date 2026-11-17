package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.network.HeldSwordGate;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.TechniqueHits;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
import com.bslx.XueXiMod.shu.technique.release.firecast.DamagePermission;
import com.bslx.XueXiMod.status.fire.seed.FireSeedGrant;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** 挑灯。持剑向前上方一次短火弧，不生成独立飞行物。 */
public final class FirePickLampEffect implements TechniqueEffect {
    static final String TECHNIQUE_ID = "fire_pick_lamp";
    static final float MANA_COST = 10.0F;
    static final int COOLDOWN_TICKS = 30;
    static final int ASSEMBLY_COST = 2;
    static final int LEVEL_CAP = 20;
    static final float TOTAL_DAMAGE = 5.0F;
    static final int SEED_POINTS = 5;
    static final double REACH = 2.5D;
    static final double SPAN = 1.5D;
    static final double ARC_HEIGHT = 2.5D;
    /** 上挫约 35 度，使短弧向前上方展开。 */
    private static final double UPWARD_PITCH = 0.7D;
    /** 原版击退强度 0.35 约为 0.35 格/秒的短促水平速度。 */
    private static final double LIFT_BLOCKS_PER_SECOND = 0.35D;

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        if (player == null || technique == null || cast == null
                || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (!HeldSwordGate.allows(definition, player.getMainHandItem())) {
            return;
        }
        Consumer<Vec3> trigger = cast.onFirstTrigger();
        AtomicBoolean triggered = new AtomicBoolean(false);
        TechniqueHitSpec spec = new TechniqueHitSpec();
        spec.techniqueId = technique.getId();
        spec.path = TechniqueHitSpec.Path.STILL_AIM;
        spec.speed = 0.0D;
        spec.length = REACH;
        spec.width = SPAN;
        spec.height = ARC_HEIGHT;
        spec.explicitObb = true;
        spec.pose3d = true;
        spec.suppressRootSettle = true;
        spec.seedPoints = SEED_POINTS;
        spec.lifetime = definition != null ? definition.lifetimeTicks() : 4;
        spec.hitInterval = 0;
        spec.lockThisCast = false;
        spec.dropThisCast = false;
        Vec3 aim = raisedLook(cast.getLook());
        spec.centered = true;
        spec.anchor = forwardAnchor(player.getEyePosition(), aim);
        spec.aim = aim;
        spec.listener = new TechniqueHitSpec.HitListener() {
            @Override
            public void onHit(LivingEntity target) {
                strike(player, target);
                if (trigger != null && triggered.compareAndSet(false, true)) {
                    trigger.accept(target.position());
                }
            }

            @Override
            public void onExpire(Vec3 position) {
                if (trigger != null && triggered.compareAndSet(false, true)) {
                    trigger.accept(position);
                }
            }
        };
        TechniqueHitSpec.spawn(serverLevel, player, spec);
    }

    /** 现有命中没有剑击与火伤分伤通道，所以只打一次总伤害。 */
    private static void strike(ServerPlayer player, LivingEntity target) {
        if (!DamagePermission.allows(player, target)) {
            return;
        }
        boolean accepted = TechniqueHits.hurtRoot(target, player, TOTAL_DAMAGE);
        if (accepted) {
            FireSeedGrant.deposit(target, player, SEED_POINTS);
            lift(target);
        }
    }

    /** 击退抗性不满 1 才给一次短促上挫，不改飞行或悬浮。 */
    private static void lift(LivingEntity target) {
        AttributeInstance resistance = target.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        double resisted = resistance == null ? 0.0D : resistance.getValue();
        if (!target.isPushable() || resisted >= 1.0D) {
            return;
        }
        double upward = (LIFT_BLOCKS_PER_SECOND / 20.0D) * (1.0D - Math.max(0.0D, resisted));
        Vec3 motion = target.getDeltaMovement();
        target.setDeltaMovement(motion.x, Math.max(motion.y, upward), motion.z);
        target.hurtMarked = true;
        target.hasImpulse = true;
    }

    private static Vec3 raisedLook(Vec3 look) {
        if (look == null || look.lengthSqr() < 1.0E-8D) {
            return new Vec3(0.0D, UPWARD_PITCH, 1.0D).normalize();
        }
        double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        if (horizontal < 1.0E-4D) {
            return new Vec3(0.0D, 1.0D, 0.0D);
        }
        return new Vec3(look.x / horizontal, UPWARD_PITCH, look.z / horizontal).normalize();
    }

    /** 盒子以半长为界。中心放到前方半个纵深，使整段 2.5 格都在身前。 */
    private static Vec3 forwardAnchor(Vec3 eye, Vec3 aim) {
        Vec3 direction = aim == null || aim.lengthSqr() < 1.0E-8D
                ? new Vec3(0.0D, UPWARD_PITCH, 1.0D).normalize()
                : aim.normalize();
        return eye.add(direction.scale(REACH * 0.5D));
    }
}
