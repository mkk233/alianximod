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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Consumer;

public class SwordVerticalSlashEffect implements TechniqueEffect {
    static final double BASE_REACH = 1.5D;
    static final double SPAN = 1.6D;
    static final double SLASH_HEIGHT = 2.0D;

    public static void declareBalance() {
        SpecialBalance.publish(SpecialBalance.blocks(
                SpecialBalance.blocks(
                        SpecialBalance.blocks(SpecialBalance.begin("sword_vertical_slash"),
                                BalanceKeys.BEAM_LENGTH, BASE_REACH, "竖斩基础距离"),
                        BalanceKeys.BOX_WIDTH, SPAN, "竖斩宽度"),
                BalanceKeys.BOX_HEIGHT, SLASH_HEIGHT, "竖斩高度"));
    }

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        Vec3 look = cast.getLook();
        Vec3 castPosition = cast.getPos();

        boolean hasFire = cast.enchantments().has("fire");
        EnchantmentAccumulator fire = hasFire ? cast.enchantments().effect("fire") : null;

        float baseReach = SpecialBalance.decimal("sword_vertical_slash", BalanceKeys.BEAM_LENGTH, (float) BASE_REACH);
        float range = hasFire ? baseReach * fire.radiusMultiplier() : baseReach;
        Consumer<Vec3> trigger = cast.onFirstTrigger();

        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 effectCenter = castPosition.add(look.scale(range));
            for (int segmentIndex = 0; segmentIndex < 12; segmentIndex++) {
                double yOffset = 2.0 - (segmentIndex * 0.2);
                double xOffset = (Math.random() - 0.5) * 0.15;
                double zOffset = (Math.random() - 0.5) * 0.15;

                serverLevel.sendParticles(ParticleTypes.CRIT,
                    effectCenter.x + xOffset, effectCenter.y + yOffset, effectCenter.z + zOffset,
                    1, 0, 0, 0, 0.05);
                serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    effectCenter.x + xOffset, effectCenter.y + yOffset, effectCenter.z + zOffset,
                    1, 0, 0, 0, 0.5);
            }
            for (int burstIndex = 0; burstIndex < 5; burstIndex++) {
                double burstX = (Math.random() - 0.5) * 0.8;
                double burstZ = (Math.random() - 0.5) * 0.8;
                serverLevel.sendParticles(ParticleTypes.CRIT,
                    effectCenter.x + burstX, effectCenter.y + 0.1, effectCenter.z + burstZ,
                    1, 0, 0.1, 0, 0.15);
            }
            if (hasFire) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                    effectCenter.x, effectCenter.y + 0.5, effectCenter.z,
                    1, 0, 0.1, 0, 0.02);
            }
            serverLevel.playSound(null, castPosition.x, castPosition.y, castPosition.z,
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.8f);

            java.util.concurrent.atomic.AtomicBoolean triggered = new java.util.concurrent.atomic.AtomicBoolean(false);
            TechniqueHitSpec spec = new TechniqueHitSpec();
            spec.techniqueId = technique.getId();
            spec.path = TechniqueHitSpec.Path.STILL_AIM;
            spec.speed = 0.0;
            ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
            spec.length = range;
            spec.width = SpecialBalance.number("sword_vertical_slash", BalanceKeys.BOX_WIDTH, SPAN);
            spec.height = SpecialBalance.number("sword_vertical_slash", BalanceKeys.BOX_HEIGHT, SLASH_HEIGHT);
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
