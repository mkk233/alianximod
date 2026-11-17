package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
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
    /** 独立追踪修饰，不替换基础路径或生成几何。 */
    public boolean tracking;
    public Trajectory pathOverride;
    public boolean trajectoryMutable;
    public float radiusMultiplier = 1f;
    public float energyScale = 1f;
    public int burnSeconds;
    public int controlTicks;
    public boolean extinguish;
    public float knockback;
    public float speedMultiplier = 1f;
    public TideMarkSuccess tideSuccess = TideMarkSuccess.NONE;
    /** 玩家身体当前被几块场地包住。0 和 1 都不改伤害。 */
    public float fieldDamageScale = 1f;
    public float healAmount;
    public boolean healCaster;
    public float pullStrength;
    public int wetnessGrant;
    public boolean blocksMovement;
    public Vec3 pullAnchor;
    public float liftStrength;
    public double volumeHalf;
    public double groundLevel = Double.NEGATIVE_INFINITY;
    public long sampleSeed;
    public boolean scatterAim;
    public int copyCount = 1;
    /** 这一份的生成几何已经按复制序号散开。只给视觉读，释放计算不读。 */
    public boolean cohortDispersed;
    public boolean heavyWater;
    public java.util.UUID releaseGroup;

    public static CastFrame current() {
        return CURRENT.get();
    }

    /** 能量倍率乘上已经含有元素反应的半径倍率。无效值按 1。 */
    public float volumeScale() {
        float scale = this.energyScale * this.radiusMultiplier;
        if (!Float.isFinite(scale) || scale <= 0f) {
            return 1f;
        }
        return scale;
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

    /** 垂落从落点上方出现，再自由落体。 */
    private static final double DROP_LIFT = 8.0;
    private static final double GOLDEN_TURN = Math.PI * (3.0 - Math.sqrt(5.0));
    private static final double VOLUME_HALF = 1.5;

    public static CastFrame forAttack(float damage, EnchantmentResolution enchantments, ReleaseBatch.Tally tally,
                                      int copyIndex, int copyCount, Vec3 center, Vec3 baseLook,
                                      boolean spaceLock, boolean scatter, boolean entityLock, Trajectory pathOverride,
                                      boolean trajectoryMutable, long seed, double groundLevel) {
        CastFrame frame = new CastFrame();
        frame.damage = damage;
        frame.enchantments = enchantments != null ? enchantments : EnchantmentResolution.empty();
        frame.tally = tally;
        frame.copyIndex = copyIndex;
        frame.copyCount = copyCount < 1 ? 1 : copyCount;
        frame.pathOverride = pathOverride;
        frame.drop = pathOverride == Trajectory.DROP;
        frame.trajectoryMutable = trajectoryMutable;
        boolean replacesPath = trajectoryMutable && pathOverride != null && pathOverride != Trajectory.STRAIGHT;

        if (!scatter && !spaceLock && !replacesPath) {
            frame.overrideGeometry = false;
            return frame;
        }

        frame.overrideGeometry = true;
        if (!trajectoryMutable) {
            return slashGeometry(frame, center, baseLook, spaceLock, scatter, copyIndex, copyCount, seed);
        }
        return projectileGeometry(frame, center, baseLook, spaceLock, scatter, entityLock, pathOverride, seed, groundLevel);
    }

    /** 斩击保持原有落点和刀面散布，不改成弹丸的随机向量。 */
    private static CastFrame slashGeometry(CastFrame frame, Vec3 center, Vec3 baseLook, boolean spaceLock, boolean scatter,
                                           int copyIndex, int copyCount, long seed) {
        double half = spaceLock ? 1.5 : 1.0;
        Vec3 origin = (spaceLock || scatter) ? jitter(center, half, seed, copyIndex) : center;
        frame.origin = origin;
        frame.anchor = spaceLock ? center : null;
        frame.look = baseLook.normalize();

        if (!scatter) {
            frame.planeNormal = horizontal(baseLook);
            frame.roll = 0.0F;
            frame.pose3d = false;
            frame.cohortDispersed = spaceLock;
            return frame;
        }

        RandomSource random = RandomSource.create(seed ^ copyIndex);
        Vec3 flatLook = horizontal(baseLook);
        Vec3 flatRight = new Vec3(-flatLook.z, 0.0, flatLook.x);
        double radius = 0.35 + random.nextDouble() * 0.9;
        double azimuth = random.nextDouble() * Math.PI * 2.0;
        double along = (random.nextDouble() * 2.0 - 1.0) * 0.75;
        double verticalOffset = (random.nextDouble() * 2.0 - 1.0) * 0.75;
        Vec3 scattered = center
                .add(flatRight.scale(Math.cos(azimuth) * radius))
                .add(flatLook.scale(Math.sin(azimuth) * radius))
                .add(flatLook.scale(along))
                .add(0.0, verticalOffset, 0.0);

        frame.origin = clampToBox(scattered, center, spaceLock ? 1.5 : 1.0);
        frame.anchor = spaceLock ? center : null;
        frame.look = baseLook.normalize();
        frame.roll = (float) ((random.nextDouble() * 2.0 - 1.0) * 35.0);
        frame.planeNormal = deflectNormal(random, baseLook, copyIndex, copyCount);
        frame.pose3d = Math.abs(frame.roll) > 1.0F || Math.abs(frame.planeNormal.y) > 0.2;
        frame.cohortDispersed = true;
        return frame;
    }

    /** 落点指定只撒生成点。三维交错在没有其他索敌时围着玩家撒点，并改成随机三维方向。 */
    private static CastFrame projectileGeometry(CastFrame frame, Vec3 center, Vec3 baseLook, boolean spaceLock, boolean scatter,
                                                boolean entityLock, Trajectory pathOverride, long seed, double groundLevel) {
        if (pathOverride == Trajectory.VORTEX) {
            frame.anchor = center;
            frame.volumeHalf = 0.0;
            frame.origin = center;
            frame.scatterAim = false;
            frame.look = frame.vortexPhase(0);
            frame.planeNormal = horizontal(frame.look);
            frame.roll = 0.0F;
            frame.pose3d = false;
            frame.cohortDispersed = true;
            return frame;
        }
        boolean cube = spaceLock || (scatter && !entityLock);
        frame.anchor = cube ? center : null;
        frame.volumeHalf = cube ? VOLUME_HALF : 0.0;
        frame.sampleSeed = seed;
        frame.groundLevel = groundLevel;
        frame.scatterAim = scatter && pathOverride != Trajectory.DROP;
        frame.look = baseLook.normalize();
        frame.origin = cube ? frame.volumePoint(0) : (pathOverride == Trajectory.DROP ? center.add(0.0, DROP_LIFT, 0.0) : center);
        frame.look = frame.aimFor(0);
        frame.planeNormal = horizontal(frame.look);
        frame.roll = 0.0F;
        frame.pose3d = false;
        frame.cohortDispersed = cube || frame.scatterAim;
        return frame;
    }

    /** 接触点把每一份收到同一个锚点上之后，视觉不能再把覆写当成已经散开。 */
    public void adoptSharedAnchor() {
        this.cohortDispersed = false;
    }

    /** 同一条龙卷风上的起始角。序号连续时仍均匀铺在圆周上。 */
    public Vec3 vortexPhase(int spawnIndex) {
        double phase = (this.copyIndex * 1024 + spawnIndex) * GOLDEN_TURN;
        return new Vec3(Math.cos(phase), 0.0, Math.sin(phase));
    }
    public Vec3 volumePoint(int spawnIndex) {
        Vec3 point = jitter(this.anchor, this.volumeHalf, this.sampleSeed, this.copyIndex * 1024 + spawnIndex);
        if (point.y < this.groundLevel) {
            point = new Vec3(point.x, this.groundLevel, point.z);
        }
        if (this.drop) {
            point = point.add(0.0, DROP_LIFT, 0.0);
        }
        return point;
    }

    public Vec3 aimFor(int spawnIndex) {
        if (this.drop) {
            return new Vec3(0.0, -1.0, 0.0);
        }
        if (this.scatterAim) {
            return randomDirection(this.sampleSeed, this.copyIndex * 1024 + spawnIndex);
        }
        return this.look == null ? new Vec3(0.0, 0.0, 1.0) : this.look;
    }

    private static Vec3 randomDirection(long seed, int copyIndex) {
        RandomSource random = RandomSource.create(seed ^ (copyIndex * 31L) ^ 0x6C62272EL);
        double rise = random.nextDouble() * 2.0 - 1.0;
        double spin = random.nextDouble() * Math.PI * 2.0;
        double ring = Math.sqrt(Math.max(0.0, 1.0 - rise * rise));
        return new Vec3(ring * Math.cos(spin), rise, ring * Math.sin(spin));
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

    private static Vec3 jitter(Vec3 center, double half, long seed, int sampleIndex) {
        long mixed = mix64(seed ^ (sampleIndex * 0x9E3779B97F4A7C15L));
        double east = (unit(mixed ^ 0x9E3779B97F4A7C15L) * 2.0 - 1.0) * half;
        double rise = (unit(mixed ^ 0xBF58476D1CE4E5B9L) * 2.0 - 1.0) * half;
        double south = (unit(mixed ^ 0x94D049BB133111EBL) * 2.0 - 1.0) * half;
        return center.add(east, rise, south);
    }

    /** 连续序号不能直接丢给 RandomSource，否则第一轴几乎不动，点会排成一条线。 */
    private static long mix64(long value) {
        long mixed = value;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }

    private static double unit(long mixed) {
        return (mix64(mixed) >>> 11) * 0x1.0p-53;
    }

    private static Vec3 clampToBox(Vec3 point, Vec3 center, double half) {
        return new Vec3(
                clamp(point.x, center.x - half, center.x + half),
                clamp(point.y, center.y - half, center.y + half),
                clamp(point.z, center.z - half, center.z + half));
    }

    private static double clamp(double value, double lowerBound, double upperBound) {
        return Math.max(lowerBound, Math.min(upperBound, value));
    }

    private static Vec3 horizontal(Vec3 vector) {
        double length = Math.sqrt(vector.x * vector.x + vector.z * vector.z);
        if (length < 1.0E-4) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return new Vec3(vector.x / length, 0.0, vector.z / length);
    }
}
