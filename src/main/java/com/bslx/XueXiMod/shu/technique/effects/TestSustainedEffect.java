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

public class TestSustainedEffect implements TechniqueEffect {
    static final double HIT_EDGE = 0.3D;

    public static void declareBalance() {
        SpecialBalance.publish(SpecialBalance.blocks(
                SpecialBalance.begin("test_sustained"), BalanceKeys.HIT_SIZE, HIT_EDGE, "持续试射命中边长"));
    }

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        float damage = cast.getDamage();

        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        float speed = definition != null ? definition.speed() : 0.4f;
        int lifetime = definition != null ? definition.lifetimeTicks() : 20;

        if (player.level() instanceof ServerLevel serverLevel) {
            Consumer<Vec3> trigger = cast.onFirstTrigger();
            AtomicBoolean triggered = new AtomicBoolean(false);

            TechniqueHitSpec spec = new TechniqueHitSpec();
            spec.path = TechniqueHitSpec.Path.DROP;
            double hitEdge = SpecialBalance.number("test_sustained", BalanceKeys.HIT_SIZE, HIT_EDGE);
            spec.length = hitEdge;
            spec.width = hitEdge;
            spec.height = hitEdge;
            spec.speed = speed;
            spec.lifetime = lifetime;
            spec.lockThisCast = false;
            spec.dropThisCast = true;
            spec.visual = 1;
            spec.techniqueId = technique.getId();
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
