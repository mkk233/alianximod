package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.WeepingCurtain;
import com.bslx.XueXiMod.shu.technique.release.WeepingSettings;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** 垂丝：一份一个盒子，沿着当前水帘移动。水花没有单独的命中体。 */
public final class WeepingCurtainEffect implements TechniqueEffect {
    private static final double STRIP_STEP = 0.72;

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        CastFrame frame = CastFrame.current();
        int lifetime = definition != null ? definition.lifetimeTicks() : WeepingSettings.LIFETIME;
        TechniqueHitSpec spec = new TechniqueHitSpec();
        spec.techniqueId = technique.getId();
        spec.path = TechniqueHitSpec.Path.FUNCTION;
        spec.functionId = WeepingCurtain.FUNCTION_ID;
        spec.length = 0.35;
        spec.width = 0.5;
        spec.height = 0.4;
        spec.speed = 0.0;
        spec.lifetime = lifetime;
        spec.hitInterval = WeepingSettings.HIT_INTERVAL;
        spec.visual = frame != null && frame.heavyWater ? 8 : 7;
        spec.lockThisCast = cast.lockThisCast();
        spec.dropThisCast = cast.dropThisCast();
        if (frame == null || !frame.overrideGeometry) {
            Vec3 look = player.getLookAngle();
            Vec3 ahead = new Vec3(look.x, 0.0, look.z);
            if (ahead.lengthSqr() < 1.0E-6) {
                ahead = new Vec3(0.0, 0.0, 1.0);
            } else {
                ahead = ahead.normalize();
            }
            Vec3 right = new Vec3(ahead.z, 0.0, -ahead.x);
            int index = frame == null ? 0 : frame.copyIndex;
            int total = frame == null ? 1 : Math.max(1, frame.copyCount);
            double side = (index - (total - 1) * 0.5) * STRIP_STEP;
            spec.start = player.getEyePosition().add(ahead.scale(2.2)).add(right.scale(side));
            spec.aim = ahead;
        }
        TechniqueHitSpec.spawn(serverLevel, player, spec);
    }
}
