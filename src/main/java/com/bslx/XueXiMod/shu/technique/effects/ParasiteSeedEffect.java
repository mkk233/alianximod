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

public class ParasiteSeedEffect implements TechniqueEffect {
    static final int SEED_COPIES = 3;
    static final double SPREAD = 1.2D;

    public static void declareBalance() {
        SpecialBalance.publish(SpecialBalance.blocks(
                SpecialBalance.begin("parasite_seed"), BalanceKeys.SPREAD_RANGE, SPREAD, "寄生种子散布半径"));
    }

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Consumer<Vec3> trigger = cast.onFirstTrigger();
        AtomicBoolean triggered = new AtomicBoolean(false);
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        double spread = SpecialBalance.number("parasite_seed", BalanceKeys.SPREAD_RANGE, SPREAD);
        float seedEdge = definition != null ? definition.thickness() : 0.5F;
        int seedLife = definition != null ? definition.lifetimeTicks() : 160;
        for (int seedIndex = 0; seedIndex < SEED_COPIES; seedIndex++) {
            double angle = seedIndex * Math.PI * 2.0 / SEED_COPIES;
            TechniqueHitSpec spec = new TechniqueHitSpec();
            spec.techniqueId = technique.getId();
            spec.path = TechniqueHitSpec.Path.DROP;
            spec.trap = true;
            spec.speed = 0.0;
            spec.length = seedEdge;
            spec.width = seedEdge;
            spec.height = seedEdge;
            spec.lifetime = seedLife;
            spec.visual = 3;
            spec.localOffset = new Vec3(Math.cos(angle) * spread, 0.0, Math.sin(angle) * spread);
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
