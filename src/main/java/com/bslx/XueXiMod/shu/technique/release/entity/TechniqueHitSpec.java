package com.bslx.XueXiMod.shu.technique.release.entity;

import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class TechniqueHitSpec {

    public enum Motion {
        SWING,
        PROJECTILE,
        DROP,
        FOLLOW
    }

    public enum Hitbox {
        ARC,
        OBB,
        BOX,
        SPHERE
    }

    public enum Path {
        STILL_AIM,
        STRAIGHT,
        DROP,
        FOLLOW
    }

    public interface HitListener {
        void onHit(LivingEntity target);

        void onExpire(Vec3 position);
    }

    public String techniqueId = "";
    public Motion motion = Motion.PROJECTILE;
    public Hitbox hitbox = Hitbox.BOX;
    public int lifetime = 20;
    public double speed = 1.0;
    public boolean lockThisCast;
    public boolean dropThisCast;
    public double arcRadius = 2.0;
    public double arcDegrees = 120.0;
    public double arcHeight = 2.0;
    public Vec3 boxSize = new Vec3(1.0, 1.0, 1.0);
    public int hitInterval;
    public int visual;
    public float modelScale = 1.0F;
    public Vec3 start;
    public Vec3 anchor;
    public Vec3 aim;
    public Path path;
    public double length = 1.0;
    public double width = 1.0;
    public double height = 1.0;
    public String texture;
    public boolean centered;
    public boolean pose3d;
    public float roll;
    public HitListener listener;
    public float settleDamage = Float.NaN;
    public EnchantmentResolution settleEnchantments;
    public ReleaseBatch.Tally settleTally;
    public int settleCopyIndex;
    public int settleBurnSeconds;
    public int settleControlTicks;
    public boolean settleExtinguish;
    public float settleKnockback;
    public Vec3 localOffset;
    public boolean trap;

    public static void spawn(ServerLevel level, ServerPlayer caster, TechniqueHitSpec spec) {
        CastFrame frame = CastFrame.current();
        if (frame != null) {
            spec.settleDamage = frame.damage;
            spec.settleEnchantments = frame.enchantments;
            spec.settleTally = frame.tally;
            spec.settleCopyIndex = frame.copyIndex;
            if (frame.radiusMultiplier > 1.001F) {
                spec.length *= frame.radiusMultiplier;
                spec.width *= frame.radiusMultiplier;
            }
            spec.settleBurnSeconds = frame.burnSeconds;
            spec.settleControlTicks = frame.controlTicks;
            spec.settleExtinguish = frame.extinguish;
            spec.settleKnockback = frame.knockback;
            if (frame.speedMultiplier > 1.001F) {
                spec.speed *= frame.speedMultiplier;
            }
            if (frame.overrideGeometry) {
                Path path = spec.path;
                boolean still = path == Path.STILL_AIM || spec.centered || !frame.trajectoryMutable;
                if (frame.drop && frame.trajectoryMutable) {
                    spec.path = Path.DROP;
                    spec.start = frame.origin != null ? frame.origin : caster.getEyePosition();
                    spec.aim = frame.look;
                } else if (still) {
                    spec.centered = true;
                    spec.anchor = frame.origin != null ? frame.origin : frame.anchor;
                    spec.aim = frame.planeNormal != null ? frame.planeNormal : frame.look;
                    spec.roll = frame.roll;
                    Vec3 normal = frame.planeNormal != null ? frame.planeNormal : frame.look;
                    spec.pose3d = frame.pose3d
                            || (normal != null && Math.abs(normal.y) > 0.2)
                            || Math.abs(frame.roll) > 1.0;
                } else if (path == Path.STRAIGHT || path == Path.DROP || path == Path.FOLLOW) {
                    spec.start = frame.origin != null ? frame.origin : caster.getEyePosition();
                    if (frame.look != null) {
                        spec.aim = frame.look;
                    }
                }
            }
        }
        if (spec.localOffset != null) {
            if (spec.start != null) {
                spec.start = spec.start.add(spec.localOffset);
            } else if (spec.anchor != null) {
                spec.anchor = spec.anchor.add(spec.localOffset);
            } else {
                spec.start = caster.getEyePosition().add(spec.localOffset);
            }
        }
        TechniqueHitEntity entity = new TechniqueHitEntity(ReleaseEntities.TECHNIQUE_HIT.get(), level);
        entity.applySpec(caster, spec);
        level.addFreshEntity(entity);
    }
}
