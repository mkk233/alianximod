package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.bind.PublicationAttempt;
import com.bslx.XueXiMod.shu.technique.release.fire.FireConsumption;
import com.bslx.XueXiMod.shu.technique.release.firecast.DamagePermission;
import com.bslx.XueXiMod.shu.technique.release.firecast.FireConsumerPlans;
import com.bslx.XueXiMod.shu.technique.release.firecast.SphereTouch;
import com.bslx.XueXiMod.status.fire.seed.FireCastContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 焚天。球选可伤害且有火种的目标，一次消费，不看视线。 */
public final class FentianEffect implements TechniqueEffect {
    static final double BASE_RADIUS = 8.0d;

    public static void declareBalance() {
        SpecialBalance.publish(SpecialBalance.blocks(
                SpecialBalance.begin("fire_fentian"), BalanceKeys.RADIUS, BASE_RADIUS, "焚天半径"));
    }

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        ReleaseBatch batch = ReleaseBatch.current();
        if (player == null || batch == null || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        CastFrame frame = CastFrame.current();
        Vec3 origin = player.position().add(0.0d, player.getBbHeight() * 0.5d, 0.0d);
        if (frame != null && frame.overrideGeometry && frame.origin != null) {
            origin = frame.origin;
        }
        double radius = SpecialBalance.number("fire_fentian", BalanceKeys.RADIUS, BASE_RADIUS);
        if (frame != null && (definition == null || definition.energyScalesLength())) {
            radius *= frame.volumeScale();
        }
        FireCastContext fireCast = batch.fireContext();
        AABB area = new AABB(origin, origin).inflate(radius);
        List<LivingEntity> found = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                living -> DamagePermission.allows(player, living));
        PublicationAttempt attempt = PublicationAttempt.current();
        if (attempt != null && !attempt.allowsPublication()) {
            return;
        }
        for (LivingEntity target : found) {
            AABB box = target.getBoundingBox();
            if (!SphereTouch.reaches(origin.x, origin.y, origin.z, radius,
                    box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ)) {
                continue;
            }
            boolean consumed = FireConsumption.schedule(target, fireCast, FireConsumerPlans.SKY_KEY,
                    FireConsumerPlans::skySpec).isPresent();
            if (consumed && attempt != null) {
                attempt.reportAccepted();
            }
        }
    }
}
