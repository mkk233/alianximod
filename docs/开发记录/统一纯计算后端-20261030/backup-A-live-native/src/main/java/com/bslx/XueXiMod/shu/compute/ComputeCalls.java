package com.bslx.XueXiMod.shu.compute;

import com.bslx.XueXiMod.debug.performance.PerfCounter;
import com.bslx.XueXiMod.debug.performance.PerfMetric;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.mojang.logging.LogUtils;
import java.util.function.IntSupplier;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * 现有单次调用的适配层。标量入口始终执行 Java 参考实现。
 * 批量入口在 native 已握手且条数达到下限时才调用 JNI。
 */
public final class ComputeCalls {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Object LOG_GATE = new Object();
    private static volatile boolean logged;
    private static volatile boolean fallbackLogged;

    private ComputeCalls() {
    }

    public static String selectionText() {
        ensureLogged();
        return ComputeSelection.recordedLine();
    }

    public static void sampleClosed(Trajectory trajectory, Vec3 origin, Vec3 forward, double speed,
                                    int lifetimeTicks, int ageTicks, Vec3 targetPoint,
                                    boolean followCurrentLook, Vec3 currentLook) {
        ensureLogged();
        long started = PerformanceRecorder.begin();
        try {
            int trajectoryCode = mapTrajectory(trajectory);
            if (origin == null && !(trajectoryCode == ComputeCodes.TRAJECTORY_FOLLOW_LOOK && targetPoint == null)) {
                throw absentOrigin(origin);
            }
            JavaReference.sampleClosed(
                    trajectoryCode,
                    origin == null ? 0 : 1,
                    origin == null ? 0.0D : origin.x,
                    origin == null ? 0.0D : origin.y,
                    origin == null ? 0.0D : origin.z,
                    forward == null ? 0 : 1,
                    forward == null ? 0.0D : forward.x,
                    forward == null ? 0.0D : forward.y,
                    forward == null ? 0.0D : forward.z,
                    speed,
                    lifetimeTicks,
                    ageTicks,
                    targetPoint == null ? 0 : 1,
                    targetPoint == null ? 0.0D : targetPoint.x,
                    targetPoint == null ? 0.0D : targetPoint.y,
                    targetPoint == null ? 0.0D : targetPoint.z,
                    followCurrentLook ? 1 : 0,
                    currentLook == null ? 0 : 1,
                    currentLook == null ? 0.0D : currentLook.x,
                    currentLook == null ? 0.0D : currentLook.y,
                    currentLook == null ? 0.0D : currentLook.z,
                    ComputeOutput.current());
            noteJava();
        } finally {
            PerformanceRecorder.end(PerfMetric.COMPUTE_BACKEND, started);
        }
    }

    public static Vec3 sampledPosition(Trajectory trajectory, Vec3 origin) {
        if (trajectory == Trajectory.FOLLOW_LOOK) {
            return origin;
        }
        ComputeOutput sink = ComputeOutput.current();
        if (sink.positionPresent == 0) {
            return null;
        }
        return new Vec3(sink.positionEast, sink.positionUp, sink.positionSouth);
    }

    public static Vec3 sampledDirection() {
        ComputeOutput sink = ComputeOutput.current();
        return new Vec3(sink.directionEast, sink.directionUp, sink.directionSouth);
    }

    public static void turnToward(Vec3 direction, Vec3 desiredDirection) {
        ensureLogged();
        long started = PerformanceRecorder.begin();
        try {
            JavaReference.turnToward(
                    direction == null ? 0 : 1,
                    direction == null ? 0.0D : direction.x,
                    direction == null ? 0.0D : direction.y,
                    direction == null ? 0.0D : direction.z,
                    desiredDirection == null ? 0 : 1,
                    desiredDirection == null ? 0.0D : desiredDirection.x,
                    desiredDirection == null ? 0.0D : desiredDirection.y,
                    desiredDirection == null ? 0.0D : desiredDirection.z,
                    ComputeOutput.current());
            noteJava();
        } finally {
            PerformanceRecorder.end(PerfMetric.COMPUTE_BACKEND, started);
        }
    }

    public static Vec3 turnedDirection() {
        return sampledDirection();
    }

    public static void trackingStep(Vec3 currentDirection, Vec3 towardTarget, double baseSpeed) {
        ensureLogged();
        long started = PerformanceRecorder.begin();
        try {
            JavaReference.trackingStep(
                    currentDirection == null ? 0 : 1,
                    currentDirection == null ? 0.0D : currentDirection.x,
                    currentDirection == null ? 0.0D : currentDirection.y,
                    currentDirection == null ? 0.0D : currentDirection.z,
                    towardTarget == null ? 0 : 1,
                    towardTarget == null ? 0.0D : towardTarget.x,
                    towardTarget == null ? 0.0D : towardTarget.y,
                    towardTarget == null ? 0.0D : towardTarget.z,
                    baseSpeed,
                    ComputeOutput.current());
            noteJava();
        } finally {
            PerformanceRecorder.end(PerfMetric.COMPUTE_BACKEND, started);
        }
    }

    public static Vec3 trackingDirection() {
        return sampledDirection();
    }

    public static double trackingStepLength() {
        return ComputeOutput.current().stepLength;
    }

    public static boolean overlaps(double startX, double startY, double startZ,
                                   double forwardX, double forwardY, double forwardZ,
                                   double length, double width, double height,
                                   double lowerX, double lowerY, double lowerZ,
                                   double upperX, double upperY, double upperZ) {
        ensureLogged();
        long started = PerformanceRecorder.begin();
        try {
            boolean overlap = JavaReference.overlaps(startX, startY, startZ, forwardX, forwardY, forwardZ,
                    length, width, height, lowerX, lowerY, lowerZ, upperX, upperY, upperZ);
            noteJava();
            return overlap;
        } finally {
            PerformanceRecorder.end(PerfMetric.COMPUTE_BACKEND, started);
        }
    }

    public static double contactDistance(double startX, double startY, double startZ,
                                         double unitX, double unitY, double unitZ,
                                         double centerX, double centerY, double centerZ,
                                         double alongPad) {
        ensureLogged();
        long started = PerformanceRecorder.begin();
        try {
            double distance = JavaReference.contactDistance(startX, startY, startZ, unitX, unitY, unitZ,
                    centerX, centerY, centerZ, alongPad);
            noteJava();
            return distance;
        } finally {
            PerformanceRecorder.end(PerfMetric.COMPUTE_BACKEND, started);
        }
    }

    public static boolean blocked(double contactDistance, double obstacleDistance) {
        ensureLogged();
        long started = PerformanceRecorder.begin();
        try {
            boolean blocks = JavaReference.blocked(contactDistance, obstacleDistance);
            noteJava();
            return blocks;
        } finally {
            PerformanceRecorder.end(PerfMetric.COMPUTE_BACKEND, started);
        }
    }

    public static int trajectoryBatch(int count, int[] header, double[] input, double[] output) {
        ensureLogged();
        long started = PerformanceRecorder.begin();
        try {
            int prepared = JavaReference.prepareTrajectory(count, header, input, output);
            return dispatch(ComputeCodes.CAPABILITY_TRAJECTORY, count, prepared,
                    () -> XuexiComputeNative.trajectoryBatch(count, header, input, output),
                    () -> JavaReference.writeTrajectory(count, header, input, output));
        } finally {
            PerformanceRecorder.end(PerfMetric.COMPUTE_BACKEND, started);
        }
    }

    public static int turnBatch(int count, int[] header, double[] input, double[] output) {
        ensureLogged();
        long started = PerformanceRecorder.begin();
        try {
            int prepared = JavaReference.prepareTurn(count, header, input, output);
            return dispatch(ComputeCodes.CAPABILITY_TURN, count, prepared,
                    () -> XuexiComputeNative.turnBatch(count, header, input, output),
                    () -> JavaReference.writeTurn(count, header, input, output));
        } finally {
            PerformanceRecorder.end(PerfMetric.COMPUTE_BACKEND, started);
        }
    }

    public static int trackingStepBatch(int count, int[] header, double[] input, double[] output) {
        ensureLogged();
        long started = PerformanceRecorder.begin();
        try {
            int prepared = JavaReference.prepareTracking(count, header, input, output);
            return dispatch(ComputeCodes.CAPABILITY_TRACKING_STEP, count, prepared,
                    () -> XuexiComputeNative.trackingStepBatch(count, header, input, output),
                    () -> JavaReference.writeTracking(count, header, input, output));
        } finally {
            PerformanceRecorder.end(PerfMetric.COMPUTE_BACKEND, started);
        }
    }

    public static int corridorOverlapsBatch(int count, double[] input, int[] output) {
        ensureLogged();
        long started = PerformanceRecorder.begin();
        try {
            int prepared = JavaReference.prepareOverlaps(count, input, output);
            return dispatch(ComputeCodes.CAPABILITY_CORRIDOR_OVERLAPS, count, prepared,
                    () -> XuexiComputeNative.corridorOverlapsBatch(count, input, output),
                    () -> JavaReference.writeOverlaps(count, input, output));
        } finally {
            PerformanceRecorder.end(PerfMetric.COMPUTE_BACKEND, started);
        }
    }

    public static int corridorContactBatch(int count, double[] input, double[] output) {
        ensureLogged();
        long started = PerformanceRecorder.begin();
        try {
            int prepared = JavaReference.prepareContact(count, input, output);
            return dispatch(ComputeCodes.CAPABILITY_CORRIDOR_CONTACT, count, prepared,
                    () -> XuexiComputeNative.corridorContactBatch(count, input, output),
                    () -> JavaReference.writeContact(count, input, output));
        } finally {
            PerformanceRecorder.end(PerfMetric.COMPUTE_BACKEND, started);
        }
    }

    public static int corridorBlockedBatch(int count, double[] input, int[] output) {
        ensureLogged();
        long started = PerformanceRecorder.begin();
        try {
            int prepared = JavaReference.prepareBlocked(count, input, output);
            return dispatch(ComputeCodes.CAPABILITY_CORRIDOR_BLOCKED, count, prepared,
                    () -> XuexiComputeNative.corridorBlockedBatch(count, input, output),
                    () -> JavaReference.writeBlocked(count, input, output));
        } finally {
            PerformanceRecorder.end(PerfMetric.COMPUTE_BACKEND, started);
        }
    }

    private static int mapTrajectory(Trajectory trajectory) {
        return switch (trajectory) {
            case STRAIGHT -> ComputeCodes.TRAJECTORY_STRAIGHT;
            case DROP -> ComputeCodes.TRAJECTORY_DROP;
            case FOLLOW_LOOK -> ComputeCodes.TRAJECTORY_FOLLOW_LOOK;
            case FUNCTION -> ComputeCodes.TRAJECTORY_FUNCTION_FALLBACK;
            case VORTEX -> ComputeCodes.TRAJECTORY_VORTEX;
            default -> ComputeCodes.TRAJECTORY_DEFAULT;
        };
    }

    private static NullPointerException absentOrigin(Vec3 origin) {
        return new NullPointerException(String.valueOf(origin.x));
    }

    private static int dispatch(int capability, int count, int prepared,
                                IntSupplier nativeRun, IntSupplier javaRun) {
        if (prepared != ComputeCodes.STATUS_OK) {
            return prepared;
        }
        if (ComputeSelection.useNative(capability, count)) {
            try {
                int nativeStatus = nativeRun.getAsInt();
                if (nativeStatus == ComputeCodes.STATUS_OK) {
                    noteNative();
                    return ComputeCodes.STATUS_OK;
                }
                logCallFallback("native-status-" + nativeStatus);
            } catch (UnsatisfiedLinkError linkError) {
                ComputeSelection.disableNative("link-failed");
                logCallFallback(linkError.getMessage() == null ? "link" : linkError.getMessage());
            }
            int rewritten = javaRun.getAsInt();
            if (rewritten == ComputeCodes.STATUS_OK) {
                noteFallback();
            }
            return rewritten;
        }
        int rewritten = javaRun.getAsInt();
        if (rewritten != ComputeCodes.STATUS_OK) {
            return rewritten;
        }
        if (ComputeSelection.missingNativeCapability(capability, count)) {
            noteFallback();
        } else {
            noteJava();
        }
        return ComputeCodes.STATUS_OK;
    }

    private static void ensureLogged() {
        ComputeSelection.ensureResolved();
        if (logged) {
            return;
        }
        synchronized (LOG_GATE) {
            if (logged) {
                return;
            }
            logged = true;
            LOGGER.info(ComputeSelection.recordedLine());
        }
    }

    private static void logCallFallback(String detail) {
        if (fallbackLogged) {
            return;
        }
        synchronized (LOG_GATE) {
            if (fallbackLogged) {
                return;
            }
            fallbackLogged = true;
            LOGGER.warn("纯计算后端：native 批量未被接受，已改用 Java 参考实现。后续失败不逐条记录。原因={}", detail);
        }
    }

    private static void noteJava() {
        PerformanceRecorder.add(PerfCounter.COMPUTE_LOGICAL_REQUESTS, 1L);
        PerformanceRecorder.add(PerfCounter.COMPUTE_JAVA_EXECUTIONS, 1L);
    }

    private static void noteNative() {
        PerformanceRecorder.add(PerfCounter.COMPUTE_LOGICAL_REQUESTS, 1L);
        PerformanceRecorder.add(PerfCounter.COMPUTE_NATIVE_EXECUTIONS, 1L);
    }

    private static void noteFallback() {
        PerformanceRecorder.add(PerfCounter.COMPUTE_LOGICAL_REQUESTS, 1L);
        PerformanceRecorder.add(PerfCounter.COMPUTE_JAVA_FALLBACKS, 1L);
    }
}
