package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.TechniqueHits;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
import com.bslx.XueXiMod.shu.technique.release.firecast.DamagePermission;
import com.bslx.XueXiMod.shu.technique.release.momentum.WaterMomentum;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** 排澜。固定消耗水势，沿身前通路把敌人向两侧推开。 */
public final class WaterDisperseEffect implements TechniqueEffect {
    public static final String TECHNIQUE_ID = "water_disperse";
    static final float MANA_COST = 12.0F;
    static final int COOLDOWN_TICKS = 60;
    static final int ASSEMBLY_COST = 1;
    static final int LEVEL_CAP = 20;
    public static final float MOMENTUM_COST = 15.0F;
    static final float BASE_DAMAGE = 2.0F;
    static final double PATH_LENGTH = 8.0D;
    static final double PATH_WIDTH = 3.0D;
    static final double PATH_HEIGHT = 2.5D;
    private static final double SIDE_BLOCKS = 2.5D;

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        if (player == null || technique == null || cast == null
                || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!momentumPaid(player)) {
            return;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        Vec3 forward = flatLook(cast.getLook());
        Consumer<Vec3> trigger = cast.onFirstTrigger();
        AtomicBoolean triggered = new AtomicBoolean(false);
        TechniqueHitSpec spec = new TechniqueHitSpec();
        spec.techniqueId = technique.getId();
        spec.path = TechniqueHitSpec.Path.STILL_AIM;
        spec.speed = 0.0D;
        spec.length = PATH_LENGTH;
        spec.width = PATH_WIDTH;
        spec.height = PATH_HEIGHT;
        spec.explicitObb = true;
        spec.suppressRootSettle = true;
        spec.lifetime = definition != null ? definition.lifetimeTicks() : 4;
        spec.hitInterval = 0;
        spec.lockThisCast = false;
        spec.dropThisCast = false;
        spec.anchor = groundedAnchor(player);
        spec.aim = forward;
        spec.listener = new TechniqueHitSpec.HitListener() {
            @Override
            public void onHit(LivingEntity target) {
                part(player, target, forward);
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

    /**
     * 注册保持 momentumSpend 为 0，公共流程不会预扣水势。
     * 可用水势至少 15 时，这里用 settle 一次扣掉固定 15；不足则不产生通路，也不改法力。
     */
    private static boolean momentumPaid(ServerPlayer player) {
        return WaterMomentum.settle(player, MOMENTUM_COST, 0.0F).paid();
    }

    private static void part(ServerPlayer player, LivingEntity target, Vec3 forward) {
        if (!DamagePermission.allows(player, target)) {
            return;
        }
        if (!TechniqueHits.hurtRoot(target, player, BASE_DAMAGE)) {
            return;
        }
        Vec3 sideways = sideOf(forward, target.position().subtract(player.position()));
        WaterWaveEffect.shove(target, sideways, SIDE_BLOCKS);
    }

    private static Vec3 sideOf(Vec3 forward, Vec3 offset) {
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);
        if (right.lengthSqr() < 1.0E-8D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        double side = offset == null ? 1.0D : offset.x * right.x + offset.z * right.z;
        if (Math.abs(side) < 1.0E-4D) {
            side = 1.0D;
        }
        return right.normalize().scale(Math.signum(side));
    }

    private static Vec3 groundedAnchor(ServerPlayer player) {
        return new Vec3(player.getX(), player.getY() + PATH_HEIGHT * 0.5D, player.getZ());
    }

    private static Vec3 flatLook(Vec3 look) {
        if (look == null) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        if (horizontal < 1.0E-4D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return new Vec3(look.x / horizontal, 0.0D, look.z / horizontal);
    }
}
