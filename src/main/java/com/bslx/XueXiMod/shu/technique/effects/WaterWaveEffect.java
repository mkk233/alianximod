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
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** 浪。一次消耗当前可用水势，按实扣点数放大贴地拍击。 */
public final class WaterWaveEffect implements TechniqueEffect {
    public static final String TECHNIQUE_ID = "water_wave";
    static final float MANA_COST = 8.0F;
    static final int COOLDOWN_TICKS = 40;
    static final int ASSEMBLY_COST = 1;
    static final int LEVEL_CAP = 20;
    static final float MOMENTUM_CAP = 20.0F;
    static final float DAMAGE_CAP = 3.0F;
    private static final double MINIMUM_LENGTH = 2.0D;
    private static final double MAXIMUM_LENGTH = 4.0D;
    private static final double MINIMUM_WIDTH = 3.0D;
    private static final double MAXIMUM_WIDTH = 5.0D;
    private static final double MINIMUM_DAMAGE = 1.0D;
    private static final double WAVE_HEIGHT = 1.5D;
    private static final double KNOCKBACK_BLOCKS = 2.0D;

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        if (player == null || technique == null || cast == null
                || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        float spent = spentMomentum(player);
        if (!(spent >= 1.0F)) {
            return;
        }
        double progress = Math.min(1.0D, spent / MOMENTUM_CAP);
        double length = MINIMUM_LENGTH + (MAXIMUM_LENGTH - MINIMUM_LENGTH) * progress;
        double width = MINIMUM_WIDTH + (MAXIMUM_WIDTH - MINIMUM_WIDTH) * progress;
        float damage = (float) (MINIMUM_DAMAGE + (DAMAGE_CAP - MINIMUM_DAMAGE) * progress);
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        Consumer<Vec3> trigger = cast.onFirstTrigger();
        AtomicBoolean triggered = new AtomicBoolean(false);
        TechniqueHitSpec spec = new TechniqueHitSpec();
        spec.techniqueId = technique.getId();
        spec.path = TechniqueHitSpec.Path.STILL_AIM;
        spec.speed = 0.0D;
        spec.length = length;
        spec.width = width;
        spec.height = WAVE_HEIGHT;
        spec.explicitObb = true;
        spec.suppressRootSettle = true;
        spec.lifetime = definition != null ? definition.lifetimeTicks() : 4;
        spec.hitInterval = 0;
        spec.lockThisCast = false;
        spec.dropThisCast = false;
        spec.anchor = groundedAnchor(player);
        spec.aim = flatLook(cast.getLook());
        spec.listener = new TechniqueHitSpec.HitListener() {
            @Override
            public void onHit(LivingEntity target) {
                slap(player, target, damage, spec.aim);
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
     * 注册保持 momentumSpend 为 0，公共流程不预扣。
     * 这里一次扣掉当前可用水势，最多 20 点；可用为 0 时不产生浪。
     */
    private static float spentMomentum(ServerPlayer player) {
        float available = WaterMomentum.available(player);
        if (!(available >= 1.0F)) {
            return 0.0F;
        }
        float spent = Math.min(MOMENTUM_CAP, available);
        WaterMomentum.Settlement settlement = WaterMomentum.settle(player, spent, 0.0F);
        return settlement.paid() ? spent : 0.0F;
    }

    private static void slap(ServerPlayer player, LivingEntity target, float damage, Vec3 forward) {
        if (!DamagePermission.allows(player, target)) {
            return;
        }
        if (!TechniqueHits.hurtRoot(target, player, Math.min(DAMAGE_CAP, damage))) {
            return;
        }
        shove(target, forward, KNOCKBACK_BLOCKS);
    }

    static void shove(LivingEntity target, Vec3 direction, double blocks) {
        AttributeInstance resistance = target.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        double resisted = resistance == null ? 0.0D : resistance.getValue();
        if (!target.isPushable() || resisted >= 1.0D || direction == null) {
            return;
        }
        Vec3 flat = new Vec3(direction.x, 0.0D, direction.z);
        if (flat.lengthSqr() < 1.0E-8D) {
            return;
        }
        Vec3 shift = flat.normalize().scale(blocks * (1.0D - Math.max(0.0D, resisted)));
        target.move(MoverType.SELF, shift);
        target.hurtMarked = true;
    }

    private static Vec3 groundedAnchor(ServerPlayer player) {
        return new Vec3(player.getX(), player.getY() + WAVE_HEIGHT * 0.5D, player.getZ());
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
