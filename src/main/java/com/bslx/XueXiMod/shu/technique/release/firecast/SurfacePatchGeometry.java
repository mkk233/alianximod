package com.bslx.XueXiMod.shu.technique.release.firecast;

import com.bslx.XueXiMod.shu.technique.release.domain.DomainGeometry;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 灼地这一发的检测几何。盒子每刻重算，选目标用体积相交，采样落在合法顶面上方、净空盒里面。
 */
final class SurfacePatchGeometry implements DomainGeometry {
    private final SurfaceFireHosts.Host host;

    SurfacePatchGeometry(SurfaceFireHosts.Host host) {
        this.host = host;
    }

    @Override
    public boolean selectsByIntersection() {
        return true;
    }

    @Override
    public AABB bounds() {
        return host.unionBounds();
    }

    @Override
    public Vec3 samplePoint(RandomSource random) {
        List<SurfaceContactField.Slice> slices = host.currentSlices();
        if (random == null || slices.isEmpty()) {
            return null;
        }
        double total = 0.0d;
        for (SurfaceContactField.Slice slice : slices) {
            if (slice.area() > 0.0d) {
                total += slice.area();
            }
        }
        if (total <= 0.0d) {
            return null;
        }
        double roll = random.nextDouble() * total;
        double walked = 0.0d;
        SurfaceContactField.Slice chosen = null;
        for (SurfaceContactField.Slice slice : slices) {
            if (slice.area() <= 0.0d) {
                continue;
            }
            walked += slice.area();
            chosen = slice;
            if (walked >= roll) {
                break;
            }
        }
        if (chosen == null) {
            return null;
        }
        double spanX = chosen.highX() - chosen.lowX();
        double spanZ = chosen.highZ() - chosen.lowZ();
        double insetX = spanX * 0.05d;
        double insetZ = spanZ * 0.05d;
        if (insetX * 2.0d >= spanX) {
            insetX = 0.0d;
        }
        if (insetZ * 2.0d >= spanZ) {
            insetZ = 0.0d;
        }
        double sampleX = chosen.lowX() + insetX + random.nextDouble() * (spanX - insetX * 2.0d);
        double sampleZ = chosen.lowZ() + insetZ + random.nextDouble() * (spanZ - insetZ * 2.0d);
        double sampleY = chosen.lowY() + 0.05d;
        if (sampleY >= chosen.highY()) {
            sampleY = (chosen.lowY() + chosen.highY()) * 0.5d;
        }
        return new Vec3(sampleX, sampleY, sampleZ);
    }

    @Override
    public Vec3 clampPoint(Vec3 point) {
        if (!finite(point)) {
            return point;
        }
        List<SurfaceContactField.Slice> slices = host.currentSlices();
        for (SurfaceContactField.Slice slice : slices) {
            AABB box = slice.box();
            if (point.x > box.minX && point.x < box.maxX && point.y > box.minY && point.y < box.maxY
                    && point.z > box.minZ && point.z < box.maxZ) {
                return point;
            }
        }
        if (slices.isEmpty()) {
            return point;
        }
        SurfaceContactField.Slice first = slices.get(0);
        return new Vec3((first.lowX() + first.highX()) * 0.5d, first.lowY() + 0.05d,
                (first.lowZ() + first.highZ()) * 0.5d);
    }

    @Override
    public boolean fullyContains(AABB body) {
        if (body == null) {
            return false;
        }
        for (SurfaceContactField.Slice slice : host.currentSlices()) {
            AABB box = slice.box();
            if (box.minX <= body.minX && box.maxX >= body.maxX
                    && box.minY <= body.minY && box.maxY >= body.maxY
                    && box.minZ <= body.minZ && box.maxZ >= body.maxZ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean intersects(AABB body) {
        if (body == null) {
            return false;
        }
        for (SurfaceContactField.Slice slice : host.currentSlices()) {
            if (slice.meets(body)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public double[] fullInterval(AABB body, Vec3 displacement) {
        return new double[0];
    }

    @Override
    public boolean fits(AABB body) {
        return false;
    }

    private static boolean finite(Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }
}
