package com.bslx.XueXiMod.shu.technique.release.hitbox;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 按长、宽、高选择几种体积。光束、显式有向盒、三维姿态和普通盒子分开。
 * 只做几何判定，不改无敌帧或账本。
 */
public final class SizedBoxHitbox implements TechniqueHitbox {
    @Override
    /** 搜索盒跨过上一位置和当前位置，再按较长的水平边和命中高度放宽。 */
    public AABB broadPhase(TechniqueHitEntity entity) {
        double reach = Math.max(entity.length(), entity.width()) + 1.0;
        return new AABB(entity.previousPosition(), entity.position()).inflate(reach, Math.max(entity.hitHeight(), 1.0), reach);
    }

    @Override
    /**
     * 挡实心的光束改走近战走廊，普通光束只扫线段。
     * 显式有向盒和三维姿态各用自己的轴。其余再区分精确体积和旧的扫探。
     */
    public boolean contains(TechniqueHitEntity entity, LivingEntity target) {
        if (entity.beam()) {
            if (entity.clipsSolids()) {
                return MeleeCorridor.hits(entity, target);
            }
            return intersectsBeam(entity, target);
        }
        if (entity.explicitObb()) {
            return intersectsExplicitBox(entity, target);
        }
        if (entity.pose3d()) {
            return intersectsTiltedPlane(entity, target);
        }
        boolean narrowHit = holdsPoint(entity.position(), entity.forward(), entity.length(), entity.width(),
                entity.hitHeight(), target.getBoundingBox().getCenter());
        boolean sweptHit = false;
        if (!entity.exactVolume()) {
            sweptHit = sweptSegmentHits(target.getBoundingBox(), entity.previousPosition(), entity.position());
        }
        return acceptContact(entity.exactVolume(), entity.hitHeight(), narrowHit, sweptHit);
    }

    /** 当前盒子里的点。沿用原来的半长、半宽、半高，边界仍算在里面。 */
    public static boolean holdsPoint(Vec3 center, Vec3 forward, double along, double across, double vertical, Vec3 point) {
        if (center == null || point == null || !Double.isFinite(along) || !Double.isFinite(across) || !Double.isFinite(vertical)) {
            return false;
        }
        Vec3 flatForward = flat(forward == null ? new Vec3(0.0, 0.0, 1.0) : forward);
        Vec3 right = new Vec3(flatForward.z, 0.0, -flatForward.x);
        Vec3 offset = point.subtract(center);
        double localForward = offset.x * flatForward.x + offset.z * flatForward.z;
        double localRight = offset.x * right.x + offset.z * right.z;
        return Math.abs(localForward) <= along * 0.5
                && Math.abs(localRight) <= across * 0.5
                && Math.abs(offset.y) <= vertical * 0.5;
    }

    /**
     * 精确体积只认当前盒子。非精确体积仍接受从上一中心扫到当前中心。
     */
    public static boolean acceptContact(boolean exactVolume, double vertical, boolean narrowHit, boolean sweptHit) {
        if (exactVolume) {
            return vertical > 0.05 && narrowHit;
        }
        return narrowHit || sweptHit;
    }

    /** 旧的非精确路径：目标盒膨胀 0.2 后，看上一中心到当前中心的线段。 */
    public static boolean sweptSegmentHits(AABB targetBox, Vec3 from, Vec3 endPoint) {
        if (targetBox == null || from == null || endPoint == null) {
            return false;
        }
        AABB box = targetBox.inflate(0.2);
        return box.contains(endPoint) || box.contains(from) || box.clip(from, endPoint).isPresent();
    }

    /** 光束中心线段。目标盒按半宽和半高膨胀，不检查方块遮挡。 */
    private static boolean intersectsBeam(TechniqueHitEntity entity, LivingEntity target) {
        Vec3 forward = entity.forward();
        if (forward.lengthSqr() < 1.0E-8) {
            forward = new Vec3(0.0, 0.0, 1.0);
        } else {
            forward = forward.normalize();
        }
        Vec3 start = entity.position();
        Vec3 end = start.add(forward.scale(Math.max(entity.length(), 0.0)));
        AABB box = target.getBoundingBox().inflate(entity.width() * 0.5, entity.height() * 0.5, entity.width() * 0.5);
        return box.contains(start) || box.contains(end) || box.clip(start, end).isPresent();
    }

    /** 法线是飞行方向。右轴和上轴再按滚转转一次，目标中心要落在加上其体型后的范围内。 */
    private static boolean intersectsTiltedPlane(TechniqueHitEntity entity, LivingEntity target) {
        Vec3 normal = entity.forward();
        if (normal.lengthSqr() < 1.0E-8) {
            normal = new Vec3(0.0, 0.0, 1.0);
        } else {
            normal = normal.normalize();
        }
        Vec3 helper = Math.abs(normal.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 right = helper.cross(normal);
        if (right.lengthSqr() < 1.0E-8) {
            right = new Vec3(1.0, 0.0, 0.0);
        }
        right = right.normalize();
        Vec3 upAxis = normal.cross(right).normalize();
        double radians = Math.toRadians(entity.roll());
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        Vec3 axisRight = right.scale(cos).add(upAxis.scale(sin));
        Vec3 axisUp = upAxis.scale(cos).subtract(right.scale(sin));
        Vec3 rel = target.getBoundingBox().getCenter().subtract(entity.position());
        double reach = target.getBbWidth() * 0.5;
        return Math.abs(rel.dot(normal)) <= target.getBbHeight() * 0.5 + 0.35
                && Math.abs(rel.dot(axisRight)) <= entity.width() * 0.5 + reach
                && Math.abs(rel.dot(axisUp)) <= entity.length() * 0.5 + reach;
    }

    /** 显式有向盒。深度轴是飞行方向，厚度轴使用高度，不再把方向当成无厚度法线。 */
    private static boolean intersectsExplicitBox(TechniqueHitEntity entity, LivingEntity target) {
        Vec3 depthAxis = entity.forward();
        if (depthAxis.lengthSqr() < 1.0E-8) {
            depthAxis = new Vec3(0.0, 0.0, 1.0);
        } else {
            depthAxis = depthAxis.normalize();
        }
        Vec3 helper = Math.abs(depthAxis.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 rightAxis = helper.cross(depthAxis);
        if (rightAxis.lengthSqr() < 1.0E-8) {
            rightAxis = new Vec3(1.0, 0.0, 0.0);
        }
        rightAxis = rightAxis.normalize();
        Vec3 thicknessAxis = depthAxis.cross(rightAxis).normalize();
        double radians = Math.toRadians(entity.roll());
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        Vec3 tiltedRight = rightAxis.scale(cosine).add(thicknessAxis.scale(sine));
        Vec3 tiltedThickness = thicknessAxis.scale(cosine).subtract(rightAxis.scale(sine));
        Vec3 relative = target.getBoundingBox().getCenter().subtract(entity.position());
        double reach = target.getBbWidth() * 0.5;
        double upright = target.getBbHeight() * 0.5;
        return Math.abs(relative.dot(depthAxis)) <= entity.length() * 0.5 + reach
                && Math.abs(relative.dot(tiltedRight)) <= entity.width() * 0.5 + reach
                && Math.abs(relative.dot(tiltedThickness)) <= entity.height() * 0.5 + upright;
    }

    private static Vec3 flat(Vec3 direction) {
        double length = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        if (length < 1.0E-4) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return new Vec3(direction.x / length, 0.0, direction.z / length);
    }
}
