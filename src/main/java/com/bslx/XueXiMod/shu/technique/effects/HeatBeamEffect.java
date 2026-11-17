package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class HeatBeamEffect implements TechniqueEffect {
    static final double BEAM_LENGTH = 6.0D;
    static final int HIT_INTERVAL = 10;

    public static void declareBalance() {
        SpecialBalance.publish(SpecialBalance.blocks(
                SpecialBalance.ticks(SpecialBalance.begin("heat_beam"), BalanceKeys.HIT_INTERVAL_TICKS, HIT_INTERVAL, "热光束命中间隔"),
                BalanceKeys.BEAM_LENGTH, BEAM_LENGTH, "热光束长度"));
    }

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        int lifetime = definition != null ? definition.lifetimeTicks() : 40;

        if (player.level() instanceof ServerLevel serverLevel) {
            Consumer<Vec3> trigger = cast.onFirstTrigger();
            AtomicBoolean triggered = new AtomicBoolean(false);
            TechniqueHitSpec spec = new TechniqueHitSpec();
            spec.techniqueId = technique.getId();
            spec.path = TechniqueHitSpec.Path.FOLLOW;
            double beamLength = SpecialBalance.number("heat_beam", BalanceKeys.BEAM_LENGTH, BEAM_LENGTH);
            float beamWidth = definition != null ? definition.thickness() : 1.0F;
            spec.speed = 0.0;
            spec.length = beamLength;
            spec.width = beamWidth;
            spec.height = beamWidth;
            spec.lifetime = lifetime;
            spec.hitInterval = SpecialBalance.integer("heat_beam", BalanceKeys.HIT_INTERVAL_TICKS, HIT_INTERVAL);
            spec.seedPoints = 1;
            spec.lockThisCast = cast.lockThisCast();
            spec.dropThisCast = cast.dropThisCast();
            spec.visual = 2;
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
