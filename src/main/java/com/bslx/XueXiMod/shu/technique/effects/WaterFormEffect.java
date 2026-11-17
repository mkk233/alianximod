package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.ReleaseShape;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.entity.HostGroupContext;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBandDraft;
import com.bslx.XueXiMod.shu.technique.release.payload.ObstacleBodies;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** 所有水系成品共用这一条生成。差别在释放定义上，不在术式编号上。 */
public final class WaterFormEffect implements TechniqueEffect {
    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (definition.barrier() != null || definition.domain() != null) {
            return;
        }
        if (definition.fieldRadius() > 0f) {
            if (definition.blocksMovement()) {
                Vec3 look = player.getLookAngle();
                Vec3 spot = player.getEyePosition().add(look.x * 2.0, -0.4, look.z * 2.0);
                ObstacleBodies.place(level, player, spot, 2.6, 2.2, definition.fieldLifetimeTicks());
            }
            return;
        }
        float thickness = definition.thickness() > 0.05f ? definition.thickness() : 0.45f;
        TechniqueHitSpec spec = new TechniqueHitSpec();
        spec.techniqueId = technique.getId();
        spec.lifetime = definition.lifetimeTicks();
        spec.speed = definition.speed();
        spec.visual = definition.bodyVisual();
        spec.functionId = definition.functionId();
        spec.lockThisCast = cast.lockThisCast();
        spec.dropThisCast = cast.dropThisCast() || definition.trajectory() == Trajectory.DROP;
        spec.length = thickness * definition.lengthScale();
        spec.width = thickness * definition.spanScale();
        spec.height = thickness;
        if (definition.shape() == ReleaseShape.SPHERE) {
            spec.length = thickness;
            spec.width = thickness;
            spec.height = thickness;
        }
        if (definition.liftStrength() > 0f && definition.speed() <= 0f) {
            Vec3 spot = aimedPoint(player);
            CastFrame frame = CastFrame.current();
            if (frame == null || !frame.overrideGeometry) {
                spec.anchor = spot;
                spec.start = spot;
            }
            spec.path = TechniqueHitSpec.Path.STILL_AIM;
            spec.centered = true;
        } else {
            spec.path = switch (definition.trajectory()) {
                case DROP -> TechniqueHitSpec.Path.DROP;
                case VORTEX -> TechniqueHitSpec.Path.VORTEX;
                case FUNCTION -> TechniqueHitSpec.Path.FUNCTION;
                case FOLLOW_LOOK -> TechniqueHitSpec.Path.FOLLOW;
                default -> TechniqueHitSpec.Path.STRAIGHT;
            };
        }
        if (definition.dedicatedInitialAim()) {
            Vec3 castLook = cast.getLook();
            if (castLook != null && castLook.lengthSqr() > 1.0E-8) {
                spec.aim = castLook.normalize();
            }
        }
        if (definition.usesArcBand()) {
            int beat = 0;
            if (definition.tracksBeatCycle()) {
                HostGroupContext group = HostGroupContext.current();
                if (group != null && group.hasBeatCandidate()) {
                    beat = group.beatCandidate();
                }
            }
            // 弧带剑形走径向刃片。其它水形态不进这条，也不按术式编号分叉。
            spec.arcDraft = ArcBandDraft.radialBlade(beat, definition.arcBandRadius(), definition.arcBandHalfWidth());
        }
        TechniqueHitSpec.spawn(level, player, spec);
    }

    private static Vec3 aimedPoint(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 far = eye.add(player.getLookAngle().scale(24.0));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit.getLocation();
        }
        return eye.add(player.getLookAngle().scale(6.0));
    }
}
