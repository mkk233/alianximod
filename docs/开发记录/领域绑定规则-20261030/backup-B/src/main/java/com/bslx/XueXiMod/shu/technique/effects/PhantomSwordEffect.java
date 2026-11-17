package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.command.SkillEntityCount;
import com.bslx.XueXiMod.debug.performance.PerfCounter;
import com.bslx.XueXiMod.debug.performance.PerfMetric;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.item.SkillCarrier;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.sword.SwordEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class PhantomSwordEffect implements TechniqueEffect {
    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        if (!SkillCarrier.acceptsSwordSkill(weapon)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        long spawnStart = PerformanceRecorder.begin();
        try {
        boolean recordingNow = PerformanceRecorder.isRecording();
        CastFrame frame = CastFrame.current();
        boolean forcedOrigin = frame != null && frame.overrideGeometry && frame.origin != null;
        boolean lockAim = cast.lockThisCast();
        int entityCount = SkillEntityCount.entityCount();
        int constructed = 0;
        int accepted = 0;
        for (int spawnIndex = 0; spawnIndex < entityCount; spawnIndex++) {
            Vec3 swordOrigin = forcedOrigin ? frame.origin : behind(player);
            if (forcedOrigin && spawnIndex > 0) {
                swordOrigin = swordOrigin.add(0.0, (spawnIndex % 7) * 0.35 - 1.0, 0.0);
            }
            SwordEntity sword = SwordEntity.phantom(level, player, swordOrigin, weapon, frame, lockAim);
            constructed++;
            boolean published = level.addFreshEntity(sword);
            if (published) {
                accepted++;
            }
        }
        if (recordingNow) {
            if (entityCount > 0) {
                PerformanceRecorder.add(PerfCounter.SWORD_SPAWN_REQUESTED, entityCount);
            }
            if (constructed > 0) {
                PerformanceRecorder.add(PerfCounter.SWORD_SPAWN_CONSTRUCTED, constructed);
            }
            if (accepted > 0) {
                PerformanceRecorder.add(PerfCounter.SWORD_SPAWN_ACCEPTED, accepted);
            }
        }
        } finally {
            PerformanceRecorder.end(PerfMetric.PHANTOM_SWORD_SPAWN, spawnStart);
        }
    }

    /** 眼后 2 格的竖直门平面。左右各 3 格加中间 1 格，共宽 7；自眼睛往上 4 格。深度固定，同一次释放共面。 */
    private static Vec3 behind(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        Vec3 flat = horizontal < 1.0E-4 ? new Vec3(0, 0, 1) : new Vec3(look.x / horizontal, 0, look.z / horizontal);
        Vec3 back = flat.scale(-1);
        Vec3 right = new Vec3(-flat.z, 0, flat.x);
        var random = player.getRandom();
        double side = (random.nextDouble() * 2.0 - 1.0) * 3.0;
        double height = player.getEyeY() + random.nextDouble() * 4.0;
        Vec3 eyeBase = player.getEyePosition().add(back.scale(2.0)).add(right.scale(side));
        return new Vec3(eyeBase.x, height, eyeBase.z);
    }
}
