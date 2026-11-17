package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.debug.performance.PerfMetric;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
import com.bslx.XueXiMod.shu.technique.release.aim.LaunchAim;
import com.bslx.XueXiMod.shu.technique.release.bind.ReleaseExecutionContext;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** 一次攻击生成的线程内几何和结算数据。生成前绑定，读完解绑。不负责寻路或造成伤害。 */
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
    /** 这一发只保留施放朝向的水平投影，不改生成点。 */
    public boolean parallel;
    /** 独立追踪修饰，不替换基础路径或生成几何。 */
    public boolean tracking;
    public Trajectory pathOverride;
    public boolean trajectoryMutable;
    public float radiusMultiplier = 1f;
    public float energyScale = 1f;
    /** 链上聚能冻结输入。不含术式自身能量、附魔半径或操作身份。 */
    public ReleaseInput releaseInput = ReleaseInput.BASE;
    public int burnSeconds;
    /** 点火附魔带上的固定火种。术式自己的点数在命中规格上相加。 */
    public int seedPoints;
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
    /** 自动轮由链执行器写入。手动路径保持 null。 */
    public ReleaseExecutionContext execution;
    /** 这一次生成时从 execution 抄出的发射快照。 */
    public LaunchAim launchAim;

    /** 返回当前线程绑定的帧。没有绑定时返回空。 */
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

    /** 绑定到当前线程。传入空则清掉绑定。 */
    public static void bind(CastFrame frame) {
        if (frame == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(frame);
        }
    }

    /** 清掉当前线程的帧。 */
    public static void unbind() {
        CURRENT.remove();
    }

    /** 垂落从落点上方出现。显式倾角只横移起点，不再把这段高度改掉。 */
    private static final double DROP_LIFT = 8.0;
    private static final double GOLDEN_TURN = Math.PI * (3.0 - Math.sqrt(5.0));
    private static final double VOLUME_HALF = 1.5;

    /** 无散布、无落点锁且不替换路径时只填伤害账本。否则按弹道是否可变选斩击或弹丸几何。 */
    public static CastFrame forAttack(float damage, EnchantmentResolution enchantments, ReleaseBatch.Tally tally,
                                      int copyIndex, int copyCount, Vec3 center, Vec3 baseLook,
                                      boolean spaceLock, boolean scatter, boolean entityLock, Trajectory pathOverride,
                                      boolean trajectoryMutable, long seed, double groundLevel) {
        long geometryStart = PerformanceRecorder.begin();
        try {
        CastFrame frame = new CastFrame();
        frame.damage = damage;
        frame.enchantments = enchantments != null ? enchantments : EnchantmentResolution.empty();
        frame.tally = tally;
        frame.copyIndex = copyIndex;
        frame.copyCount = copyCount < 1 ? 1 : copyCount;
        frame.pathOverride = pathOverride;
        frame.drop = pathOverride == Trajectory.DROP;
        frame.parallel = pathOverride == Trajectory.PARALLEL;
        frame.sampleSeed = seed;
        frame.trajectoryMutable = trajectoryMutable;
        boolean replacesPath = trajectoryMutable && pathOverride != null && pathOverride != Trajectory.STRAIGHT
                && pathOverride != Trajectory.PARALLEL;

        if (!scatter && !spaceLock && !replacesPath) {
            frame.overrideGeometry = false;
            if (frame.parallel && trajectoryMutable && baseLook != null) {
                frame.look = horizontal(baseLook);
            }
            return frame;
        }

        frame.overrideGeometry = true;
        if (!trajectoryMutable) {
            return slashGeometry(frame, center, baseLook, spaceLock, scatter, copyIndex, copyCount, seed);
        }
        return projectileGeometry(frame, center, baseLook, spaceLock, scatter, entityLock, pathOverride, seed, groundLevel);
        } finally {
            PerformanceRecorder.end(PerfMetric.GEOMETRY, geometryStart);
        }
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
        frame.groundLevel = groundLevel;
        frame.scatterAim = scatter && pathOverride != Trajectory.DROP && pathOverride != Trajectory.PARALLEL;
        frame.look = baseLook.normalize();
        if (cube) {
            frame.origin = frame.volumePoint(0);
        } else if (pathOverride == Trajectory.DROP) {
            frame.origin = center.add(0.0, DROP_LIFT, 0.0);
        } else if (pathOverride != Trajectory.PARALLEL) {
            frame.origin = center;
        }
        frame.look = frame.aimFor(0);
        if (frame.parallel) {
            frame.look = horizontal(baseLook);
        }
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

    /** 按序号取生成点：抖动后不低于地面，垂落再上抬，自动来源最后钳制。 */
    public Vec3 volumePoint(int spawnIndex) {
        long geometryStart = PerformanceRecorder.begin();
        try {
            Vec3 point = jitter(this.anchor, this.volumeHalf, this.sampleSeed, this.copyIndex * 1024 + spawnIndex);
            if (point.y < this.groundLevel) {
                point = new Vec3(point.x, this.groundLevel, point.z);
            }
            if (this.drop) {
                point = point.add(0.0, DROP_LIFT, 0.0);
            }
            return this.clampAutomaticSource(point);
        } finally {
            PerformanceRecorder.end(PerfMetric.GEOMETRY, geometryStart);
        }
    }

    /** 当前帧是否来自领域自动执行。 */
    public boolean automaticSource() {
        return LaunchAim.automatic(this);
    }

    /** 自动来源把实际生成点钳回领域真实范围。手动点和形状偏移不进这里。 */
    public Vec3 clampAutomaticSource(Vec3 point) {
        if (point == null || !this.automaticSource() || this.execution.volume() == null) {
            return point;
        }
        return this.execution.volume().clampPoint(point);
    }

    /** 垂落返回竖直向下；交错返回随机三维方向；否则沿用帧上的视线。 */
    public Vec3 aimFor(int spawnIndex) {
        long geometryStart = PerformanceRecorder.begin();
        try {
            if (this.drop) {
                // 真正的斜向在各自生成口按落点锚点写入。这里仍是竖直占位，避免领域或接触点再抬一次。
                return new Vec3(0.0, -1.0, 0.0);
            }
            if (this.parallel) {
                return horizontal(this.look);
            }
            if (this.scatterAim) {
                return randomDirection(this.sampleSeed, this.copyIndex * 1024 + spawnIndex);
            }
            return this.look == null ? new Vec3(0.0, 0.0, 1.0) : this.look;
        } finally {
            PerformanceRecorder.end(PerfMetric.GEOMETRY, geometryStart);
        }
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
