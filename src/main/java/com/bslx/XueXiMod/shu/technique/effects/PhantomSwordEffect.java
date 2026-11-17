package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.command.SkillEntityCount;
import com.bslx.XueXiMod.debug.performance.PerfCounter;
import com.bslx.XueXiMod.debug.performance.PerfMetric;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.item.SkillCarrier;
import com.bslx.XueXiMod.network.HeldSwordGate;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.DropTilt;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.aim.LaunchAim;
import com.bslx.XueXiMod.shu.technique.release.bind.PublicationAttempt;
import com.bslx.XueXiMod.shu.technique.sword.SwordEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class PhantomSwordEffect implements TechniqueEffect {
    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack heldNow = player.getMainHandItem();
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (!SkillCarrier.acceptsSwordSkill(weapon) || !SkillCarrier.acceptsSwordSkill(heldNow)) {
            return;
        }
        if (!HeldSwordGate.allows(definition, heldNow)) {
            return;
        }
        long spawnStart = PerformanceRecorder.begin();
        try {
        boolean recordingNow = PerformanceRecorder.isRecording();
        CastFrame frame = CastFrame.current();
        boolean automatic = LaunchAim.automatic(frame);
        boolean forcedOrigin = frame != null && frame.overrideGeometry && frame.origin != null;
        boolean lockAim = !automatic && cast.lockThisCast();
        int entityCount = SkillEntityCount.entityCount();
        int constructed = 0;
        int accepted = 0;
        for (int spawnIndex = 0; spawnIndex < entityCount; spawnIndex++) {
            PublicationAttempt attempt = PublicationAttempt.current();
            if (automatic && attempt == null) {
                break;
            }
            if (attempt != null && !attempt.allowsPublication()) {
                break;
            }
            if (automatic && !LaunchAim.spawnAllowed(frame, level)) {
                break;
            }
            Vec3 swordOrigin;
            LaunchAim aim = null;
            boolean explicitDrop = frame != null && frame.drop && frame.trajectoryMutable;
            boolean parallelHeading = frame != null && frame.parallel && frame.trajectoryMutable;
            Vec3 dropHeading = null;
            Vec3 heldHeading = null;
            if (explicitDrop) {
                Vec3 columnStart = forcedOrigin ? frame.origin : behind(player);
                if (spawnIndex > 0) {
                    columnStart = columnStart.add(0.0, (spawnIndex % 7) * 0.35 - 1.0, 0.0);
                }
                if (automatic) {
                    columnStart = frame.clampAutomaticSource(columnStart);
                }
                DropTilt.Placement placement = DropTilt.placement(
                        level, columnStart, frame.sampleSeed, frame.copyIndex, spawnIndex);
                if (placement == null) {
                    swordOrigin = columnStart;
                    dropHeading = new Vec3(0.0, -1.0, 0.0);
                } else {
                    swordOrigin = placement.start();
                    if (automatic) {
                        swordOrigin = frame.clampAutomaticSource(swordOrigin);
                    }
                    dropHeading = DropTilt.unitToward(swordOrigin, placement.anchor());
                }
                if (automatic) {
                    aim = LaunchAim.capture(frame, swordOrigin, dropHeading);
                }
            } else if (parallelHeading) {
                swordOrigin = forcedOrigin ? frame.origin : behind(player);
                if (spawnIndex > 0) {
                    swordOrigin = swordOrigin.add(0.0, (spawnIndex % 7) * 0.35 - 1.0, 0.0);
                }
                if (automatic) {
                    swordOrigin = frame.clampAutomaticSource(swordOrigin);
                }
                heldHeading = horizontalHeading(frame.look);
                if (automatic) {
                    aim = LaunchAim.capture(frame, swordOrigin, heldHeading);
                }
            } else if (automatic) {
                Vec3 base = frame.origin != null ? frame.origin : frame.execution.origin();
                if (spawnIndex > 0) {
                    base = base.add(0.0, (spawnIndex % 7) * 0.35 - 1.0, 0.0);
                }
                swordOrigin = frame.clampAutomaticSource(base);
                Vec3 facing = frame.look != null ? frame.look : frame.execution.baseDirection();
                aim = LaunchAim.capture(frame, swordOrigin, facing);
            } else {
                swordOrigin = forcedOrigin ? frame.origin : behind(player);
                if (forcedOrigin && spawnIndex > 0) {
                    swordOrigin = swordOrigin.add(0.0, (spawnIndex % 7) * 0.35 - 1.0, 0.0);
                }
            }
            SwordEntity sword = SwordEntity.phantom(level, player, swordOrigin, weapon, frame, lockAim);
            com.bslx.XueXiMod.status.wood.vine.VineMarkActions.Action vineAction =
                    com.bslx.XueXiMod.status.wood.vine.VineMarkActions.current();
            if (vineAction != null && vineAction.source()
                    == com.bslx.XueXiMod.status.wood.vine.VineMarkActions.Source.SWORD) {
                com.bslx.XueXiMod.status.wood.vine.VineMarkRuntime.stampSword(
                        sword, vineAction.actionId(), vineAction.spectrumEligible());
            }
            if (aim != null) {
                sword.adoptAutomaticAim(aim, swordOrigin);
            }
            if (dropHeading != null) {
                sword.keepDropHeading(dropHeading);
            } else if (heldHeading != null) {
                sword.holdLaunchHeading(heldHeading);
            }
            constructed++;
            if (attempt != null && !attempt.allowsPublication()) {
                break;
            }
            boolean published = level.addFreshEntity(sword);
            if (published) {
                if (attempt != null) {
                    attempt.reportAccepted();
                }
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

    private static Vec3 horizontalHeading(Vec3 look) {
        if (look == null) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        double horizontalLength = Math.sqrt(look.x * look.x + look.z * look.z);
        if (horizontalLength < 1.0E-4) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return new Vec3(look.x / horizontalLength, 0.0, look.z / horizontalLength);
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
