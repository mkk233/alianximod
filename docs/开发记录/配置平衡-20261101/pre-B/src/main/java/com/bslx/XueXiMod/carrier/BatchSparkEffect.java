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
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 直线火苗。本体产种基数 2 来自释放登记，seedPoints 保持 0，避免再变成一份旧固定火种。
 * 命中或撞块后消失，不爆炸。20 刻点燃在结算回调里补，因为最早扫掠不走 onHit。
 */
public final class BatchSparkEffect implements TechniqueEffect {
    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        TechniqueHitSpec spec = new TechniqueHitSpec();
        spec.techniqueId = technique.getId();
        spec.path = TechniqueHitSpec.Path.STRAIGHT;
        spec.length = BatchCarrierIds.SPARK_SIZE;
        spec.width = BatchCarrierIds.SPARK_SIZE;
        spec.height = BatchCarrierIds.SPARK_SIZE;
        spec.speed = definition != null ? definition.speed() : BatchCarrierIds.SPARK_SPEED;
        spec.lifetime = definition != null ? definition.lifetimeTicks() : BatchCarrierIds.SPARK_LIFE_TICKS;
        spec.seedPoints = 0;
        spec.visual = 0;
        spec.clipsSolids = true;
        spec.burstOnImpact = true;
        spec.lockThisCast = cast.lockThisCast();
        spec.dropThisCast = cast.dropThisCast();
        AtomicBoolean gate = new AtomicBoolean(false);
        spec.listenerFactory = () -> BatchFlightBook.listener(BatchCarrierIds.SPARK, cast.onFirstTrigger(), gate);
        spec.publishedHook = BatchFlightBook::publish;
        TechniqueHitSpec.spawn(serverLevel, player, spec);
    }
}
