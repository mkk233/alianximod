package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.aim.LaunchAim;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.technique.release.WeepingCurtain;
import com.bslx.XueXiMod.shu.technique.release.WeepingSettings;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** 垂丝：一份一个盒子，沿着当前水帘移动。水花没有单独的命中体。 */
public final class WeepingCurtainEffect implements TechniqueEffect {
    private static final double STRIP_STEP = 0.72;

    public static void declareBalance() {
        SpecialBalance.publish(SpecialBalance.ticks(
                SpecialBalance.blocks(SpecialBalance.begin("water_weeping_arc"),
                        BalanceKeys.HIT_SIZE, WeepingSettings.SHEET_ACROSS, "垂丝帘宽"),
                BalanceKeys.HIT_INTERVAL_TICKS, WeepingSettings.HIT_INTERVAL, "垂丝命中间隔"));
    }

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
        spec.length = definition != null ? definition.thickness() : 0.35;
        spec.width = SpecialBalance.number("water_weeping_arc", BalanceKeys.HIT_SIZE, WeepingSettings.SHEET_ACROSS);
        spec.height = 0.4;
        spec.speed = 0.0;
        spec.lifetime = lifetime;
        spec.hitInterval = SpecialBalance.integer("water_weeping_arc", BalanceKeys.HIT_INTERVAL_TICKS, WeepingSettings.HIT_INTERVAL);
        spec.visual = frame != null && frame.heavyWater ? 8 : 7;
        spec.lockThisCast = cast.lockThisCast();
        spec.dropThisCast = cast.dropThisCast();
        if (frame != null && LaunchAim.automatic(frame) && !ruleAlreadyPlaced(frame)) {
            Vec3 look = frame.look != null ? frame.look : frame.execution.baseDirection();
            Vec3 ahead = horizontalFacing(look);
            Vec3 right = new Vec3(ahead.z, 0.0, -ahead.x);
            int index = frame.copyIndex;
            int total = Math.max(1, frame.copyCount);
            double side = (index - (total - 1) * 0.5) * STRIP_STEP;
            Vec3 source = frame.origin != null ? frame.origin : frame.execution.origin();
            spec.start = frame.clampAutomaticSource(source.add(right.scale(side)));
            spec.aim = ahead;
        } else if (frame == null || !frame.overrideGeometry) {
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

    /** 落点、交错、体积撒点和刀面已经摆过位置时，横排不再二次平移。 */
    private static boolean ruleAlreadyPlaced(CastFrame frame) {
        return frame.cohortDispersed || frame.pose3d || frame.scatterAim || frame.volumeHalf > 0.0;
    }

    private static Vec3 horizontalFacing(Vec3 look) {
        if (look == null) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        double east = look.x;
        double south = look.z;
        double span = Math.sqrt(east * east + south * south);
        if (span < 1.0E-4) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return new Vec3(east / span, 0.0, south / span);
    }
}
