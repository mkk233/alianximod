package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class SwordHorizontalSlashEffect implements TechniqueEffect {
    static final double REACH = 3.0D;
    static final double SPAN = 5.0D;

    public static void declareBalance() {
        SpecialBalance.publish(SpecialBalance.blocks(
                SpecialBalance.blocks(SpecialBalance.begin("sword_horizontal_slash"),
                        BalanceKeys.BOX_DEPTH, REACH, "横斩纵深"),
                BalanceKeys.BOX_WIDTH, SPAN, "横斩宽度"));
    }

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        Vec3 look = cast.getLook();
        Vec3 point = cast.getPos();

        float width = 3.0f;
        AttributeInstance reach = player.getAttribute(ForgeMod.ENTITY_REACH.get());
        if (reach != null) {
            width = (float) reach.getValue();
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            double offsetX = look.x * 1.5;
            double offsetY = 1.0;
            double offsetZ = look.z * 1.5;
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                point.x + offsetX, point.y + offsetY, point.z + offsetZ,
                1, 0, 0, 0, 0);
            serverLevel.playSound(null, point.x, point.y, point.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.0f);

            Consumer<Vec3> trigger = cast.onFirstTrigger();
            AtomicBoolean triggered = new AtomicBoolean(false);

            TechniqueHitSpec spec = new TechniqueHitSpec();
            spec.techniqueId = technique.getId();
            spec.path = TechniqueHitSpec.Path.STILL_AIM;
            spec.speed = 0.0;
            ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
            spec.length = SpecialBalance.number("sword_horizontal_slash", BalanceKeys.BOX_DEPTH, REACH);
            spec.width = SpecialBalance.number("sword_horizontal_slash", BalanceKeys.BOX_WIDTH, SPAN);
            spec.height = 0.0;
            spec.texture = "xueximod:textures/entity/xueximod/horizontal_slash.png";
            spec.lifetime = definition != null ? definition.lifetimeTicks() : 4;
            spec.hitInterval = 0;
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
