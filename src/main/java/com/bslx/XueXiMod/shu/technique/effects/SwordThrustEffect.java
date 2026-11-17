package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.EnchantmentAccumulator;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Consumer;

public class SwordThrustEffect implements TechniqueEffect {
    static final float BASE_REACH = 3.0F;
    static final double CROSS_SECTION = 1.0D;

    public static void declareBalance() {
        SpecialBalance.publish(SpecialBalance.blocks(
                SpecialBalance.blocks(SpecialBalance.begin("sword_thrust"),
                        BalanceKeys.BEAM_LENGTH, BASE_REACH, "刺击基础距离"),
                BalanceKeys.HIT_SIZE, CROSS_SECTION, "刺击截面"));
    }

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        Vec3 look = cast.getLook();
        Vec3 castPoint = cast.getPos();

        boolean hasFire = cast.enchantments().has("fire");
        EnchantmentAccumulator fire = hasFire ? cast.enchantments().effect("fire") : null;

        float baseReach = SpecialBalance.decimal("sword_thrust", BalanceKeys.BEAM_LENGTH, BASE_REACH);
        float range = hasFire ? baseReach * fire.radiusMultiplier() : baseReach;
        Consumer<Vec3> trigger = cast.onFirstTrigger();
        if (player.level() instanceof ServerLevel serverLevel) {
            java.util.concurrent.atomic.AtomicBoolean triggered = new java.util.concurrent.atomic.AtomicBoolean(false);
            TechniqueHitSpec spec = new TechniqueHitSpec();
            spec.techniqueId = technique.getId();
            spec.path = TechniqueHitSpec.Path.STILL_AIM;
            spec.speed = 0.0;
            ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
            double crossSection = SpecialBalance.number("sword_thrust", BalanceKeys.HIT_SIZE, CROSS_SECTION);
            spec.length = range;
            spec.width = crossSection;
            spec.height = crossSection;
            spec.texture = "xueximod:textures/entity/xueximod/horizontal_slash.png";
            spec.lifetime = definition != null ? definition.lifetimeTicks() : 4;
            spec.lockThisCast = false;
            spec.dropThisCast = false;
            spec.anchor = player.getEyePosition();
            spec.aim = look;
            spec.listener = new TechniqueHitSpec.HitListener() {
                @Override
                public void onHit(LivingEntity target) {
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
    }
}
