package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.TechniqueHits;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class FireBurstEffect implements TechniqueEffect {
    private static final int BURN_SECONDS = 4;
    private static final double BURST_RADIUS = 3.0;

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        float speed = definition != null ? definition.speed() : 0.6F;
        int lifetime = definition != null ? definition.lifetimeTicks() : 40;
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Consumer<Vec3> trigger = cast.onFirstTrigger();
        AtomicBoolean triggered = new AtomicBoolean(false);
        CastFrame frame = CastFrame.current();
        double burstRadius = BURST_RADIUS * (frame != null ? frame.volumeScale() : 1f);
        TechniqueHitSpec spec = new TechniqueHitSpec();
        spec.techniqueId = technique.getId();
        spec.path = TechniqueHitSpec.Path.STRAIGHT;
        spec.length = 0.8;
        spec.width = 0.8;
        spec.height = 0.8;
        spec.speed = speed;
        spec.lifetime = lifetime;
        spec.lockThisCast = cast.lockThisCast();
        spec.dropThisCast = cast.dropThisCast();
        spec.burstOnImpact = true;
        spec.visual = 4;
        spec.modelScale = 1.8F;
        spec.listenerFactory = () -> burstListener(serverLevel, player, cast, trigger, triggered, burstRadius);
        TechniqueHitSpec.spawn(serverLevel, player, spec);
    }

    private static TechniqueHitSpec.HitListener burstListener(ServerLevel serverLevel, ServerPlayer player,
                                                              TechniqueCastContext cast, Consumer<Vec3> trigger,
                                                              AtomicBoolean triggered, double burstRadius) {
        AtomicBoolean burst = new AtomicBoolean(false);
        return new TechniqueHitSpec.HitListener() {
            @Override
            public void onHit(LivingEntity target) {
                explode(target.position());
            }

            @Override
            public void onExpire(Vec3 position) {
                explode(position);
            }

            private void explode(Vec3 burstPoint) {
                if (!burst.compareAndSet(false, true)) {
                    return;
                }
                AABB area = new AABB(burstPoint, burstPoint).inflate(burstRadius);
                List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                        living -> living.isAlive() && living != player && !living.isSpectator());
                for (LivingEntity victim : victims) {
                    TechniqueHits.hurtRoot(victim, player, cast.getDamage());
                    victim.setSecondsOnFire(BURN_SECONDS);
                }
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, burstPoint.x, burstPoint.y, burstPoint.z, 1, 0.0, 0.0, 0.0, 0.0);
                serverLevel.sendParticles(ParticleTypes.FLAME, burstPoint.x, burstPoint.y, burstPoint.z, 24, 0.6, 0.6, 0.6, 0.02);
                serverLevel.playSound(null, burstPoint.x, burstPoint.y, burstPoint.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 1.0F);
                if (trigger != null && triggered.compareAndSet(false, true)) {
                    trigger.accept(burstPoint);
                }
            }
        };
    }
}
