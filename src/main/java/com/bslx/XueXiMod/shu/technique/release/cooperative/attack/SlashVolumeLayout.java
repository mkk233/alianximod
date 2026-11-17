package com.bslx.XueXiMod.shu.technique.release.cooperative.attack;

import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ActorFrame;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.AdapterVolumeSpec;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ExplicitVolumeGeometry;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.FrozenValues;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ResolvedCopyGeometry;
import net.minecraft.world.phys.Vec3;

/**
 * 静止斩击的纯形状。这里不读玩家、世界或结算封套。
 * 局部原点是相对眼点的偏移，还不是世界坐标。
 */
public final class SlashVolumeLayout {
    /** 斜斩中心沿瞄准轴离开眼点的距离。默认纵深 3 时，盒子近端落在眼点。 */
    public static final double CENTER_AHEAD_BLOCKS = 1.5D;
    /** 与 {@link FrozenValues} 相同的退化长度平方。 */
    private static final double DEGENERATE_LENGTH_SQUARED = 1.0E-8D;
    /**
     * 旧横劈 {@code StillAimMotion} 用水平长度而不是长度平方，阈值是 0.0001。
     * 横劈适配保持这个退化和正南回退。
     */
    private static final double HORIZONTAL_DEGENERATE_SPAN = 1.0E-4D;

    private SlashVolumeLayout() {
    }

    /**
     * 斜斩局部形状。瞄准轴是冻结刀向；近垂直时改用回退偏航的水平前向。
     * 刀面绕该轴、从水平右向转过带符号的倾角。镜像只反号倾角，不改前向。
     */
    public static ExplicitVolumeGeometry oblique(AdapterVolumeSpec volume, Vec3 frozenAim,
                                                  float fallbackYaw, boolean mirrored) {
        FrozenValues.present(volume, "斜斩尺寸");
        Vec3 forward = aimAxis(frozenAim, fallbackYaw);
        double signedTilt = mirrored ? -volume.tiltDegrees() : volume.tiltDegrees();
        Vec3 bladeNormal = tiltNormal(forward, signedTilt);
        return new ExplicitVolumeGeometry(
                forward.scale(CENTER_AHEAD_BLOCKS),
                forward,
                bladeNormal,
                (float) signedTilt,
                true,
                volume.boxDepth(),
                volume.boxWidth(),
                volume.boxThickness());
    }

    /**
     * 横劈局部形状。纵深轴是压平后的冻结刀向，中心在眼点前方半个纵深。
     * 厚度沿世界上方。不读取倾角，因此镜像不会把旧横劈改成斜面。
     */
    public static ExplicitVolumeGeometry horizontal(AdapterVolumeSpec volume, Vec3 frozenAim) {
        FrozenValues.present(volume, "横劈尺寸");
        Vec3 forward = flatForward(frozenAim);
        Vec3 bladeNormal = new Vec3(0.0D, 1.0D, 0.0D);
        return new ExplicitVolumeGeometry(
                forward.scale(volume.boxDepth() * 0.5D),
                forward,
                bladeNormal,
                0.0F,
                false,
                volume.boxDepth(),
                volume.boxWidth(),
                volume.boxThickness());
    }

    /**
     * 链落点已经写进 {@code solved.origin()}，它代替施法时的玩家眼点。
     * 先加上局部偏移，再加这名演员眼点相对玩家基准眼点的差。
     * 不采用副本上的刀面法线或滚转，静止斩击只搬生成点。
     */
    public static ExplicitVolumeGeometry placeActor(ExplicitVolumeGeometry localShape,
                                                     ResolvedCopyGeometry solved,
                                                     ActorFrame actor) {
        FrozenValues.present(localShape, "局部形状");
        FrozenValues.present(solved, "已解副本");
        FrozenValues.present(actor, "演员");
        Vec3 solvedAnchor = solved.origin();
        Vec3 playerCenter = solvedAnchor.add(localShape.origin());
        Vec3 eyeShift = actor.eye().subtract(solved.playerEye());
        Vec3 actorCenter = playerCenter.add(eyeShift);
        return new ExplicitVolumeGeometry(
                actorCenter,
                localShape.direction(),
                localShape.bladeNormal(),
                localShape.roll(),
                localShape.pose3d(),
                localShape.boxDepth(),
                localShape.boxWidth(),
                localShape.boxThickness());
    }

    private static Vec3 aimAxis(Vec3 frozenAim, float fallbackYaw) {
        Vec3 finiteAim = FrozenValues.unit(frozenAim, "冻结瞄准");
        double horizontal = finiteAim.x * finiteAim.x + finiteAim.z * finiteAim.z;
        if (horizontal < DEGENERATE_LENGTH_SQUARED) {
            return yawForward(fallbackYaw);
        }
        return finiteAim;
    }

    private static Vec3 yawForward(float fallbackYaw) {
        if (!Float.isFinite(fallbackYaw)) {
            throw new IllegalArgumentException("退化视线的水平朝向必须有限");
        }
        double radians = Math.toRadians(fallbackYaw);
        Vec3 heading = new Vec3(-Math.sin(radians), 0.0D, Math.cos(radians));
        if (heading.lengthSqr() < DEGENERATE_LENGTH_SQUARED) {
            throw new IllegalArgumentException("退化视线无法形成水平刀向");
        }
        return heading.normalize();
    }

    private static Vec3 tiltNormal(Vec3 forward, double signedTilt) {
        if (!Double.isFinite(signedTilt)) {
            throw new IllegalArgumentException("刀面倾角必须有限");
        }
        Vec3 worldUp = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 rightward = worldUp.cross(forward);
        if (rightward.lengthSqr() < DEGENERATE_LENGTH_SQUARED) {
            throw new IllegalArgumentException("瞄准轴无法形成水平刀宽");
        }
        rightward = rightward.normalize();
        Vec3 upAxis = forward.cross(rightward).normalize();
        double radians = Math.toRadians(signedTilt);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        Vec3 bladeNormal = upAxis.scale(cosine).subtract(rightward.scale(sine));
        if (bladeNormal.lengthSqr() < DEGENERATE_LENGTH_SQUARED) {
            throw new IllegalArgumentException("刀面法线退化");
        }
        bladeNormal = bladeNormal.normalize();
        if (Math.abs(forward.dot(bladeNormal)) > 1.0E-6D) {
            throw new IllegalArgumentException("刀向与刀面法线不正交");
        }
        return bladeNormal;
    }

    /** 旧横劈的水平前向。近垂直时固定朝正南，不改用演员偏航。 */
    private static Vec3 flatForward(Vec3 frozenAim) {
        Vec3 finiteAim = FrozenValues.unit(frozenAim, "冻结瞄准");
        double east = finiteAim.x;
        double south = finiteAim.z;
        double horizontal = Math.sqrt(east * east + south * south);
        if (horizontal < HORIZONTAL_DEGENERATE_SPAN) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return new Vec3(east / horizontal, 0.0D, south / horizontal);
    }
}
