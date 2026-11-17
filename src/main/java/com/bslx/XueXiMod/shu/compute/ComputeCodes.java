package com.bslx.XueXiMod.shu.compute;

/**
 * Java 与 C 共用的冻结数字契约。这里没有 Minecraft 类型。
 * 东、上、南分别是世界 X、Y、Z。改数值会改变现行玩法，所以常量只记录现行公式。
 */
public final class ComputeCodes {
    public static final int ABI_VERSION = 1;
    /** 批量接口自身的条数上限。不是跨实体调度已经合并的证明。 */
    public static final int COMPUTE_BATCH_LIMIT = 4096;
    /**
     * 只约束公开的六个 Batch 方法：条数小于 8 时，即使 native 已握手，这些批量入口仍走 Java。
     * 已接入的单次计算在显式 native、库有效且能力支持时按 count=1 进入对应 JNI，不受这个下限约束。
     * 8 不是测过的加速阈值，也不是等待凑批的调度器。运行加载和数值都还没有验证。
     */
    public static final int NATIVE_BATCH_FLOOR = 8;

    public static final int STATUS_OK = 0;
    public static final int STATUS_BAD_VERSION = 1;
    public static final int STATUS_BAD_COUNT = 2;
    public static final int STATUS_BAD_LENGTH = 3;
    public static final int STATUS_BAD_CAPACITY = 4;
    public static final int STATUS_OVERFLOW = 5;
    public static final int STATUS_ALIAS = 6;
    public static final int STATUS_UNSUPPORTED = 7;
    public static final int STATUS_NATIVE_FAILURE = 8;
    public static final int STATUS_NULL_BUFFER = 9;
    public static final int STATUS_BAD_HEADER = 10;

    public static final int CAPABILITY_TRAJECTORY = 1;
    public static final int CAPABILITY_TURN = 2;
    public static final int CAPABILITY_TRACKING_STEP = 4;
    public static final int CAPABILITY_CORRIDOR_OVERLAPS = 8;
    public static final int CAPABILITY_CORRIDOR_CONTACT = 16;
    public static final int CAPABILITY_CORRIDOR_BLOCKED = 32;

    /** 文档与日志里的操作编号。v1 不把编号放进缓冲区，一种操作对应一个 JNI 函数。 */
    public static final int OPERATION_TRAJECTORY = 1;
    public static final int OPERATION_TURN = 2;
    public static final int OPERATION_TRACKING_STEP = 3;
    public static final int OPERATION_CORRIDOR_OVERLAPS = 4;
    public static final int OPERATION_CORRIDOR_CONTACT = 5;
    public static final int OPERATION_CORRIDOR_BLOCKED = 6;

    public static final int TRAJECTORY_STRAIGHT = 0;
    public static final int TRAJECTORY_DROP = 1;
    public static final int TRAJECTORY_FOLLOW_LOOK = 2;
    /** 未注册 FUNCTION 的直线退回。已注册回调不进入本内核。 */
    public static final int TRAJECTORY_FUNCTION_FALLBACK = 3;
    public static final int TRAJECTORY_VORTEX = 4;
    public static final int TRAJECTORY_PARABOLA = 5;
    public static final int TRAJECTORY_UPWELL = 6;
    public static final double BALLISTIC_GRAVITY = 0.08D;
    public static final double PARABOLA_PITCH_DEGREES = 15.0D;
    public static final double UPWELL_CONE_DEGREES = 25.0D;
    /** 现行 switch 的 default，与未注册 FUNCTION 一样走无目标直线。 */
    public static final int TRAJECTORY_DEFAULT = -1;

    public static final String PROPERTY_BACKEND = "xueximod.compute.backend";
    public static final String PROPERTY_LIBRARY = "xueximod.compute.library";
    public static final String BACKEND_TOKEN_JAVA = "java";
    public static final String BACKEND_TOKEN_NATIVE = "native";
    public static final String WINDOWS_LIBRARY_FILE = "xueximod_compute.dll";
    public static final String LINUX_LIBRARY_FILE = "libxueximod_compute.so";
    public static final String MACOS_LIBRARY_FILE = "libxueximod_compute.dylib";

    public static final double TRAJECTORY_LENGTH_SQUARED_LIMIT = 1.0E-8D;
    public static final double MINECRAFT_NORMALIZE_LENGTH = 1.0E-4D;
    public static final double STEERING_LENGTH_LIMIT = 1.0E-8D;
    public static final double TURN_DEGREES = 12.0D;
    public static final double HELPER_ABSOLUTE_UP = 0.9D;
    public static final double DROP_LEADING = 0.5D;
    public static final double DROP_FACTOR = 0.08D;
    public static final double VORTEX_MOUTH_RADIUS = 5.0D;
    public static final double VORTEX_COLUMN_HEIGHT = 12.0D;
    public static final double VORTEX_TURN_COUNT = 3.0D;
    public static final int VORTEX_SPIRAL_TICK_FLOOR = 8;
    public static final double TRACK_ALIGNED = 1.0E-4D;
    public static final double TRACK_ZERO_SPAN = 1.0E-8D;
    public static final double TRACK_APPROACH = 0.8D;
    public static final double CORRIDOR_AXIS_EPSILON = 1.0E-8D;
    public static final double CORRIDOR_BLOCK_SLACK = 1.0E-4D;
    public static final double CORRIDOR_HINT_ABSOLUTE = 0.9D;

    public static final int TRAJECTORY_HEADER_STRIDE = 8;
    public static final int TRAJECTORY_HEADER_CODE = 0;
    public static final int TRAJECTORY_HEADER_ORIGIN_PRESENT = 1;
    public static final int TRAJECTORY_HEADER_FORWARD_PRESENT = 2;
    public static final int TRAJECTORY_HEADER_TARGET_PRESENT = 3;
    public static final int TRAJECTORY_HEADER_FOLLOW_CURRENT_LOOK = 4;
    public static final int TRAJECTORY_HEADER_LOOK_PRESENT = 5;
    public static final int TRAJECTORY_HEADER_LIFETIME_TICKS = 6;
    public static final int TRAJECTORY_HEADER_AGE_TICKS = 7;

    public static final int TRAJECTORY_INPUT_STRIDE = 13;
    public static final int TRAJECTORY_INPUT_ORIGIN_EAST = 0;
    public static final int TRAJECTORY_INPUT_ORIGIN_UP = 1;
    public static final int TRAJECTORY_INPUT_ORIGIN_SOUTH = 2;
    public static final int TRAJECTORY_INPUT_FORWARD_EAST = 3;
    public static final int TRAJECTORY_INPUT_FORWARD_UP = 4;
    public static final int TRAJECTORY_INPUT_FORWARD_SOUTH = 5;
    public static final int TRAJECTORY_INPUT_SPEED = 6;
    public static final int TRAJECTORY_INPUT_TARGET_EAST = 7;
    public static final int TRAJECTORY_INPUT_TARGET_UP = 8;
    public static final int TRAJECTORY_INPUT_TARGET_SOUTH = 9;
    public static final int TRAJECTORY_INPUT_LOOK_EAST = 10;
    public static final int TRAJECTORY_INPUT_LOOK_UP = 11;
    public static final int TRAJECTORY_INPUT_LOOK_SOUTH = 12;

    public static final int TRAJECTORY_OUTPUT_STRIDE = 7;
    public static final int TRAJECTORY_OUTPUT_POSITION_PRESENT = 0;
    public static final int TRAJECTORY_OUTPUT_POSITION_EAST = 1;
    public static final int TRAJECTORY_OUTPUT_POSITION_UP = 2;
    public static final int TRAJECTORY_OUTPUT_POSITION_SOUTH = 3;
    public static final int TRAJECTORY_OUTPUT_DIRECTION_EAST = 4;
    public static final int TRAJECTORY_OUTPUT_DIRECTION_UP = 5;
    public static final int TRAJECTORY_OUTPUT_DIRECTION_SOUTH = 6;

    public static final int TURN_HEADER_STRIDE = 2;
    public static final int TURN_HEADER_CURRENT_PRESENT = 0;
    public static final int TURN_HEADER_DESIRED_PRESENT = 1;
    public static final int TURN_INPUT_STRIDE = 6;
    public static final int TURN_INPUT_CURRENT_EAST = 0;
    public static final int TURN_INPUT_CURRENT_UP = 1;
    public static final int TURN_INPUT_CURRENT_SOUTH = 2;
    public static final int TURN_INPUT_DESIRED_EAST = 3;
    public static final int TURN_INPUT_DESIRED_UP = 4;
    public static final int TURN_INPUT_DESIRED_SOUTH = 5;
    public static final int TURN_OUTPUT_STRIDE = 3;
    public static final int TURN_OUTPUT_EAST = 0;
    public static final int TURN_OUTPUT_UP = 1;
    public static final int TURN_OUTPUT_SOUTH = 2;

    public static final int TRACKING_HEADER_STRIDE = 2;
    public static final int TRACKING_HEADER_CURRENT_PRESENT = 0;
    public static final int TRACKING_HEADER_TARGET_PRESENT = 1;
    public static final int TRACKING_INPUT_STRIDE = 7;
    public static final int TRACKING_INPUT_CURRENT_EAST = 0;
    public static final int TRACKING_INPUT_CURRENT_UP = 1;
    public static final int TRACKING_INPUT_CURRENT_SOUTH = 2;
    public static final int TRACKING_INPUT_TARGET_EAST = 3;
    public static final int TRACKING_INPUT_TARGET_UP = 4;
    public static final int TRACKING_INPUT_TARGET_SOUTH = 5;
    public static final int TRACKING_INPUT_BASE_SPEED = 6;
    public static final int TRACKING_OUTPUT_STRIDE = 4;
    public static final int TRACKING_OUTPUT_DIRECTION_EAST = 0;
    public static final int TRACKING_OUTPUT_DIRECTION_UP = 1;
    public static final int TRACKING_OUTPUT_DIRECTION_SOUTH = 2;
    public static final int TRACKING_OUTPUT_STEP_LENGTH = 3;

    public static final int CORRIDOR_OVERLAPS_INPUT_STRIDE = 15;
    public static final int CORRIDOR_OVERLAPS_START_EAST = 0;
    public static final int CORRIDOR_OVERLAPS_START_UP = 1;
    public static final int CORRIDOR_OVERLAPS_START_SOUTH = 2;
    public static final int CORRIDOR_OVERLAPS_FORWARD_EAST = 3;
    public static final int CORRIDOR_OVERLAPS_FORWARD_UP = 4;
    public static final int CORRIDOR_OVERLAPS_FORWARD_SOUTH = 5;
    public static final int CORRIDOR_OVERLAPS_LENGTH = 6;
    public static final int CORRIDOR_OVERLAPS_WIDTH = 7;
    public static final int CORRIDOR_OVERLAPS_HEIGHT = 8;
    public static final int CORRIDOR_OVERLAPS_LOWER_EAST = 9;
    public static final int CORRIDOR_OVERLAPS_LOWER_UP = 10;
    public static final int CORRIDOR_OVERLAPS_LOWER_SOUTH = 11;
    public static final int CORRIDOR_OVERLAPS_UPPER_EAST = 12;
    public static final int CORRIDOR_OVERLAPS_UPPER_UP = 13;
    public static final int CORRIDOR_OVERLAPS_UPPER_SOUTH = 14;
    public static final int CORRIDOR_OVERLAPS_OUTPUT_STRIDE = 1;

    public static final int CORRIDOR_CONTACT_INPUT_STRIDE = 10;
    public static final int CORRIDOR_CONTACT_START_EAST = 0;
    public static final int CORRIDOR_CONTACT_START_UP = 1;
    public static final int CORRIDOR_CONTACT_START_SOUTH = 2;
    public static final int CORRIDOR_CONTACT_UNIT_EAST = 3;
    public static final int CORRIDOR_CONTACT_UNIT_UP = 4;
    public static final int CORRIDOR_CONTACT_UNIT_SOUTH = 5;
    public static final int CORRIDOR_CONTACT_CENTER_EAST = 6;
    public static final int CORRIDOR_CONTACT_CENTER_UP = 7;
    public static final int CORRIDOR_CONTACT_CENTER_SOUTH = 8;
    public static final int CORRIDOR_CONTACT_ALONG_PAD = 9;
    public static final int CORRIDOR_CONTACT_OUTPUT_STRIDE = 1;

    public static final int CORRIDOR_BLOCKED_INPUT_STRIDE = 2;
    public static final int CORRIDOR_BLOCKED_CONTACT = 0;
    public static final int CORRIDOR_BLOCKED_OBSTACLE = 1;
    public static final int CORRIDOR_BLOCKED_OUTPUT_STRIDE = 1;

    private ComputeCodes() {
    }
}
