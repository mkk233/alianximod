package com.bslx.XueXiMod.carrier;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 抛物线陨岩。重力、初速和俯仰用释放登记上的抛物线，不另写一条弹道。
 * 飞行盒只负责接触，中心伤害在第一次合法实体命中时打一次。
 * 命中实体后沿当时运动继续，直到撞块；没碰到实体时，超时仍只留无伤残骸。
 */
public final class BatchMeteoriteEffect implements TechniqueEffect {
    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        TechniqueHitSpec spec = new TechniqueHitSpec();
        spec.techniqueId = technique.getId();
        spec.path = TechniqueHitSpec.Path.STRAIGHT;
        double bodySize = definition != null ? definition.thickness() : BatchCarrierIds.METEOR_SIZE;
        spec.length = bodySize;
        spec.width = bodySize;
        spec.height = bodySize;
        spec.speed = definition != null ? definition.speed() : BatchCarrierIds.METEOR_SPEED;
        spec.lifetime = definition != null ? definition.lifetimeTicks() : BatchCarrierIds.METEOR_LIFE_TICKS;
        spec.seedPoints = 0;
        spec.visual = 0;
        spec.clipsSolids = true;
        spec.burstOnImpact = false;
        spec.retainFlightAfterEntity = true;
        spec.lockThisCast = cast.lockThisCast();
        spec.dropThisCast = cast.dropThisCast();
        AtomicBoolean gate = new AtomicBoolean(false);
        spec.listenerFactory = () -> BatchFlightBook.listener(BatchCarrierIds.METEORITE, cast.onFirstTrigger(), gate);
        spec.publishedHook = BatchFlightBook::publish;
        TechniqueHitSpec.spawn(serverLevel, player, spec);
    }
}
