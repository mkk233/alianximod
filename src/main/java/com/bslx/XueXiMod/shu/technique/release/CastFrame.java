package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class CastFrame {
    private static final ThreadLocal<CastFrame> CURRENT = new ThreadLocal<>();

    public float damage;
    public EnchantmentResolution enchantments;
    public ReleaseBatch.Tally tally;
    public int copyIndex;
    public Vec3 anchor;
    public Vec3 origin;
    public Vec3 look;
    public Vec3 planeNormal;
    public float roll;
    public boolean pose3d;
    public boolean overrideGeometry;
    public boolean drop;
    public boolean trajectoryMutable;
    public float radiusMultiplier = 1f;
    public int burnSeconds;
    public int controlTicks;
    public boolean extinguish;
    public float knockback;
    public float speedMultiplier = 1f;

    public static CastFrame current() {
        return CURRENT.get();
    }

    public static void bind(CastFrame frame) {
        if (frame == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(frame);
        }
    }

    public static void unbind() {
        CURRENT.remove();
    }

    public static CastFrame forAttack(float damage, EnchantmentResolution enchantments, ReleaseBatch.Tally tally,
                                      int copyIndex, int copyCount, Vec3 center, Vec3 baseLook,
                                      boolean spaceLock, boolean scatter, boolean drop, boolean trajectoryMutable, long seed) {
        CastFrame frame = new CastFrame();
        frame.damage = damage;
        frame.enchantments = enchantments != null ? enchantments : EnchantmentResolution.empty();
        frame.tally = tally;
        frame.copyIndex = copyIndex;
        frame.drop = drop;
        frame.trajectoryMutable = trajectoryMutable;

        if (!scatter && !spaceLock && !(drop && trajectoryMutable)) {
            frame.overrideGeometry = false;
            return frame;
        }

        frame.overrideGeometry = true;
        double half = spaceLock ? 1.5 : 1.0;
        Vec3 origin = (spaceLock || scatter) ? jitter(center, half, seed, copyIndex) : center;
        frame.origin = origin;
        frame.anchor = spaceLock ? center : null;
        frame.look = baseLook.normalize();

        if (!scatter) {
            frame.planeNormal = horizontal(baseLook);
            frame.roll = 0.0F;
            frame.pose3d = false;
            return frame;
        }

        RandomSource random = RandomSource.create(seed ^ copyIndex);
        Vec3 flatLook = horizontal(baseLook);
        Vec3 flatRight = new Vec3(-flatLook.z, 0.0, flatLook.x);
        double radius = 0.35 + random.nextDouble() * 0.9;
        double azimuth = random.nextDouble() * Math.PI * 2.0;
        double along = (random.nextDouble() * 2.0 - 1.0) * 0.75;
        double dy = (random.nextDouble() * 2.0 - 1.0) * 0.75;
        Vec3 scattered = center
                .add(flatRight.scale(Math.cos(azimuth) * radius))
                .add(flatLook.scale(Math.sin(azimuth) * radius))
                .add(flatLook.scale(along))
                .add(0.0, dy, 0.0);

        frame.origin = clampToBox(scattered, center, spaceLock ? 1.5 : 1.0);
        frame.anchor = spaceLock ? center : null;
        frame.look = baseLook.normalize();
        frame.roll = (float) ((random.nextDouble() * 2.0 - 1.0) * 35.0);
        frame.planeNormal = deflectNormal(random, baseLook, copyIndex, copyCount);
        frame.pose3d = Math.abs(frame.roll) > 1.0F || Math.abs(frame.planeNormal.y) > 0.2;
        return frame;
    }

    private static Vec3 deflectNormal(RandomSource random, Vec3 baseLook, int copyIndex, int copyCount) {
        if (copyCount <= 1) {
            return yawPitch(baseLook,
                    (random.nextDouble() * 2.0 - 1.0) * 25.0,
                    (random.nextDouble() * 2.0 - 1.0) * 15.0);
        }
        double baseYaw = copyIndex * 120.0;
        double yaw = baseYaw;
        double pitch = (copyIndex - 1) * 15.0;
        for (int attempt = 0; attempt < 8; attempt++) {
            double jitter = (random.nextDouble() * 2.0 - 1.0) * 25.0;
            if (Math.abs(jitter) >= 12.0) {
                yaw = baseYaw + jitter;
                pitch = (random.nextDouble() * 2.0 - 1.0) * 15.0;
                break;
            }
        }
        return yawPitch(baseLook, yaw, pitch);
    }

    private static Vec3 yawPitch(Vec3 base, double yawDegrees, double pitchDegrees) {
        Vec3 flat = horizontal(base);
        Vec3 flatRight = new Vec3(-flat.z, 0.0, flat.x);
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        Vec3 yawed = flat.scale(Math.cos(yaw)).add(flatRight.scale(Math.sin(yaw)));
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        return new Vec3(yawed.x * cosPitch, sinPitch, yawed.z * cosPitch).normalize();
    }

    private static Vec3 jitter(Vec3 center, double half, long seed, int copyIndex) {
        RandomSource random = RandomSource.create(seed ^ copyIndex);
        return center.add(
                (random.nextDouble() * 2.0 - 1.0) * half,
                (random.nextDouble() * 2.0 - 1.0) * half,
                (random.nextDouble() * 2.0 - 1.0) * half);
    }

    private static Vec3 clampToBox(Vec3 point, Vec3 center, double half) {
        return new Vec3(
                clamp(point.x, center.x - half, center.x + half),
                clamp(point.y, center.y - half, center.y + half),
                clamp(point.z, center.z - half, center.z + half));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Vec3 horizontal(Vec3 v) {
        double length = Math.sqrt(v.x * v.x + v.z * v.z);
        if (length < 1.0E-4) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return new Vec3(v.x / length, 0.0, v.z / length);
    }
}
