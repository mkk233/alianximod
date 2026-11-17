package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.network.HeldSwordGate;
import com.bslx.XueXiMod.network.LiaoliRingPacket;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.aim.LaunchAim;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.TechniqueHits;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
import com.bslx.XueXiMod.shu.technique.release.fire.FireConsumption;
import com.bslx.XueXiMod.shu.technique.release.firecast.DamagePermission;
import com.bslx.XueXiMod.shu.technique.release.firecast.FireConsumerPlans;
import com.bslx.XueXiMod.status.fire.seed.FireCastContext;
import com.bslx.XueXiMod.visual.liaoli.LiaoliV6Layout;
import com.bslx.XueXiMod.status.fire.seed.FireSeedSnapshot;
import com.bslx.XueXiMod.status.fire.seed.FireSeedState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.Consumer;

/** 燎里。走廊跟随准星，命中先看权限，再引爆火种，然后按迎面速度打一刀物理。 */
public final class LiaoliEffect implements TechniqueEffect {
    static final double CORRIDOR_LENGTH = 3.0d;
    /** 相对刺击方向的迎面速度上限，格/秒。投影之后再封顶。 */
    private static final double APPROACH_CAP = 40.0d;
    private static final double APPROACH_GAIN = 1.5d;
    /** 一拍位移远超物理速度时，当作瞬移，不记成冲击。 */
    private static final double JUMP_LIMIT = 8.0d;
    private static final double JUMP_SLACK = 4.0d;

    public static void declareBalance() {
        SpecialBalance.publish(SpecialBalance.blocks(
                SpecialBalance.begin("fire_liaoli"), BalanceKeys.BEAM_LENGTH, CORRIDOR_LENGTH, "燎里走廊长度"));
    }

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        ReleaseBatch batch = ReleaseBatch.current();
        if (player == null || batch == null || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (!HeldSwordGate.allows(definition, player.getMainHandItem())) {
            return;
        }
        CastFrame frame = CastFrame.current();
        if (LaunchAim.automatic(frame) && !LaunchAim.spawnAllowed(frame, serverLevel)) {
            return;
        }
        FireCastContext fireCast = batch.fireContext();
        float physical = technique.getBaseDamage();
        int lifeTicks = definition == null ? 40 : definition.lifetimeTicks();
        TechniqueHitSpec spec = new TechniqueHitSpec();
        spec.techniqueId = technique.getId();
        spec.path = TechniqueHitSpec.Path.FOLLOW;
        spec.keepFollowing = true;
        spec.fixedLifetime = true;
        spec.clipsSolids = true;
        spec.suppressRootSettle = true;
        spec.endsWhenReleased = definition != null && definition.endsWhenReleased();
        spec.releaseSlot = batch.sustainSlot();
        double corridor = SpecialBalance.number("fire_liaoli", BalanceKeys.BEAM_LENGTH, CORRIDOR_LENGTH);
        double corridorWidth = definition != null ? definition.thickness() : 0.6d;
        spec.speed = 0.0d;
        spec.length = corridor;
        spec.width = corridorWidth;
        spec.height = corridorWidth;
        spec.lifetime = lifeTicks;
        spec.visual = LiaoliV6Layout.VISUAL;
        SeedHit[] staged = new SeedHit[1];
        spec.listenerFactory = () -> {
            SeedHit created = new SeedHit(player, fireCast, physical);
            staged[0] = created;
            return created;
        };
        Consumer<TechniqueHitEntity> earlierHook = spec.publishedHook;
        spec.publishedHook = spawned -> {
            SeedHit created = staged[0];
            staged[0] = null;
            if (created != null) {
                created.attach(spawned);
            }
            if (earlierHook != null) {
                earlierHook.accept(spawned);
            }
        };
        TechniqueHitSpec.spawn(serverLevel, player, spec);
    }

    /**
     * 先看伤害权限，再按当时余额引爆，然后打物理。
     * 后环只看这次成功扣掉的点数，不在扣完之后再读余额。
     * 物理是基础刺击加上迎面相对速度，不读武器伤害。
     */
    private static final class SeedHit implements TechniqueHitSpec.HitListener {
        private final ServerPlayer player;
        private final FireCastContext fireCast;
        private final float physical;
        private TechniqueHitEntity host;

        private SeedHit(ServerPlayer player, FireCastContext fireCast, float physical) {
            this.player = player;
            this.fireCast = fireCast;
            this.physical = physical;
        }

        private void attach(TechniqueHitEntity host) {
            this.host = host;
        }

        @Override
        public void onHit(LivingEntity target) {
            if (!DamagePermission.allows(this.player, target)) {
                return;
            }
            Optional<FireSeedSnapshot> spent = Optional.empty();
            if (FireSeedState.value(target) > 0) {
                spent = FireConsumption.detonate(target, this.fireCast, FireConsumerPlans.ALL_SEEDS, FireConsumerPlans::thrust);
            }
            float struck = openingStrike(target);
            if (struck > 0.0f) {
                TechniqueHits.hurtRoot(target, this.player,
                        SpecialBalance.contributed("fire_liaoli", struck), TideMarkSuccess.NONE);
            }
            if (this.host != null && !this.host.isRemoved() && spent.isPresent() && spent.get().amount() > 50) {
                this.host.pulseRearRing();
                LiaoliRingPacket.send(this.player);
            }
        }

        private float openingStrike(LivingEntity target) {
            double projected = approachBlocksPerSecond(target);
            double struck = this.physical + APPROACH_GAIN * projected;
            if (!Double.isFinite(struck) || struck <= 0.0d) {
                return 0.0f;
            }
            return (float) struck;
        }

        /**
         * 施法者速度减去目标速度，再投到这一拍的刺击方向上。
         * 单位是格/秒。横移没有投影，背离夹成 0，超过 40 的部分不要。
         */
        private double approachBlocksPerSecond(LivingEntity target) {
            if (this.player == null || target == null || target.level() != this.player.level()) {
                return 0.0d;
            }
            Vec3 axis = thrustAxis();
            Vec3 relative = blocksEachSecond(this.player).subtract(blocksEachSecond(target));
            double projected = relative.dot(axis);
            if (!Double.isFinite(projected) || projected < 0.0d) {
                return 0.0d;
            }
            if (projected > APPROACH_CAP) {
                return APPROACH_CAP;
            }
            return projected;
        }

        private Vec3 thrustAxis() {
            Vec3 axis = this.host != null ? this.host.forward() : null;
            if (axis == null || axis.lengthSqr() < 1.0E-8d) {
                LaunchAim aim = this.host != null ? this.host.launchAim() : null;
                if (aim != null && aim.automaticLifetime()) {
                    axis = aim.direction();
                } else {
                    axis = this.player.getLookAngle();
                }
            }
            if (axis == null || axis.lengthSqr() < 1.0E-8d) {
                return new Vec3(0.0d, 0.0d, 1.0d);
            }
            return axis.normalize();
        }

        @Override
        public void onExpire(Vec3 position) {
        }
    }

    /**
     * 格/tick 乘 20。优先用本拍实际位移；位移还没发生时用实体上的速度，避免冲刺被读成 0。
     * 不读 getSpeed，也不读冲刺布尔。位移和速度对不上的大跳跃记 0。
     */
    private static Vec3 blocksEachSecond(Entity entity) {
        if (entity == null || entity.isRemoved() || entity.level() == null) {
            return Vec3.ZERO;
        }
        Vec3 motion = entity.getDeltaMovement();
        if (motion == null || !finite(motion)) {
            motion = Vec3.ZERO;
        }
        double movedX = entity.getX() - entity.xo;
        double movedY = entity.getY() - entity.yo;
        double movedZ = entity.getZ() - entity.zo;
        if (!Double.isFinite(movedX) || !Double.isFinite(movedY) || !Double.isFinite(movedZ)) {
            return Vec3.ZERO;
        }
        Vec3 shifted = new Vec3(movedX, movedY, movedZ);
        double motionDistance = motion.length();
        double shiftedDistance = shifted.length();
        if (shiftedDistance > JUMP_LIMIT && shiftedDistance > motionDistance + JUMP_SLACK) {
            return Vec3.ZERO;
        }
        Vec3 chosen = shiftedDistance > motionDistance ? shifted : motion;
        return chosen.scale(20.0d);
    }

    private static boolean finite(Vec3 sample) {
        return Double.isFinite(sample.x) && Double.isFinite(sample.y) && Double.isFinite(sample.z);
    }
}
