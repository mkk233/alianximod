package com.bslx.XueXiMod.shu.compute;

import java.util.Arrays;

/**
 * 显式 native 时，把一条现有计算装进既有批量 ABI 的 count=1。
 * 每条线程最多四层复用缓冲；超出时不调用 JNI，避免重入写坏外层输出。
 */
final class ComputeLiveDispatch {
    static final int ACCEPTED = 1;
    static final int REJECTED = 2;
    static final int UNSLOTTED = 3;
    private static final int FRAME_LIMIT = 4;
    private static final ThreadLocal<ComputeLiveFrames> FRAMES = ThreadLocal.withInitial(ComputeLiveFrames::new);

    private ComputeLiveDispatch() {
    }

    static int trajectory(int trajectoryCode,
                          int originPresent, double originEast, double originUp, double originSouth,
                          int forwardPresent, double forwardEast, double forwardUp, double forwardSouth,
                          double speed, int lifetimeTicks, int ageTicks,
                          int targetPresent, double targetEast, double targetUp, double targetSouth,
                          int followCurrentLook,
                          int lookPresent, double lookEast, double lookUp, double lookSouth,
                          ComputeOutput sink) {
        ComputeLiveFrame frame = acquire();
        if (frame == null) {
            return UNSLOTTED;
        }
        try {
            int[] header = frame.trajectoryHeader;
            header[ComputeCodes.TRAJECTORY_HEADER_CODE] = trajectoryCode;
            header[ComputeCodes.TRAJECTORY_HEADER_ORIGIN_PRESENT] = originPresent;
            header[ComputeCodes.TRAJECTORY_HEADER_FORWARD_PRESENT] = forwardPresent;
            header[ComputeCodes.TRAJECTORY_HEADER_TARGET_PRESENT] = targetPresent;
            header[ComputeCodes.TRAJECTORY_HEADER_FOLLOW_CURRENT_LOOK] = followCurrentLook;
            header[ComputeCodes.TRAJECTORY_HEADER_LOOK_PRESENT] = lookPresent;
            header[ComputeCodes.TRAJECTORY_HEADER_LIFETIME_TICKS] = lifetimeTicks;
            header[ComputeCodes.TRAJECTORY_HEADER_AGE_TICKS] = ageTicks;
            double[] input = frame.trajectoryInput;
            input[ComputeCodes.TRAJECTORY_INPUT_ORIGIN_EAST] = originEast;
            input[ComputeCodes.TRAJECTORY_INPUT_ORIGIN_UP] = originUp;
            input[ComputeCodes.TRAJECTORY_INPUT_ORIGIN_SOUTH] = originSouth;
            input[ComputeCodes.TRAJECTORY_INPUT_FORWARD_EAST] = forwardEast;
            input[ComputeCodes.TRAJECTORY_INPUT_FORWARD_UP] = forwardUp;
            input[ComputeCodes.TRAJECTORY_INPUT_FORWARD_SOUTH] = forwardSouth;
            input[ComputeCodes.TRAJECTORY_INPUT_SPEED] = speed;
            input[ComputeCodes.TRAJECTORY_INPUT_TARGET_EAST] = targetEast;
            input[ComputeCodes.TRAJECTORY_INPUT_TARGET_UP] = targetUp;
            input[ComputeCodes.TRAJECTORY_INPUT_TARGET_SOUTH] = targetSouth;
            input[ComputeCodes.TRAJECTORY_INPUT_LOOK_EAST] = lookEast;
            input[ComputeCodes.TRAJECTORY_INPUT_LOOK_UP] = lookUp;
            input[ComputeCodes.TRAJECTORY_INPUT_LOOK_SOUTH] = lookSouth;
            double[] output = frame.trajectoryOutput;
            Arrays.fill(output, 0.0D);
            int status = callTrajectory(header, input, output);
            if (status != ComputeCodes.STATUS_OK) {
                return REJECTED;
            }
            sink.ignoredFollow = followCurrentLook;
            sink.positionPresent = output[ComputeCodes.TRAJECTORY_OUTPUT_POSITION_PRESENT] == 0.0D ? 0 : 1;
            sink.positionEast = output[ComputeCodes.TRAJECTORY_OUTPUT_POSITION_EAST];
            sink.positionUp = output[ComputeCodes.TRAJECTORY_OUTPUT_POSITION_UP];
            sink.positionSouth = output[ComputeCodes.TRAJECTORY_OUTPUT_POSITION_SOUTH];
            sink.directionEast = output[ComputeCodes.TRAJECTORY_OUTPUT_DIRECTION_EAST];
            sink.directionUp = output[ComputeCodes.TRAJECTORY_OUTPUT_DIRECTION_UP];
            sink.directionSouth = output[ComputeCodes.TRAJECTORY_OUTPUT_DIRECTION_SOUTH];
            return ACCEPTED;
        } finally {
            release();
        }
    }

    static int turn(int currentPresent, double currentEast, double currentUp, double currentSouth,
                    int desiredPresent, double desiredEast, double desiredUp, double desiredSouth,
                    ComputeOutput sink) {
        ComputeLiveFrame frame = acquire();
        if (frame == null) {
            return UNSLOTTED;
        }
        try {
            int[] header = frame.turnHeader;
            header[ComputeCodes.TURN_HEADER_CURRENT_PRESENT] = currentPresent;
            header[ComputeCodes.TURN_HEADER_DESIRED_PRESENT] = desiredPresent;
            double[] input = frame.turnInput;
            input[ComputeCodes.TURN_INPUT_CURRENT_EAST] = currentEast;
            input[ComputeCodes.TURN_INPUT_CURRENT_UP] = currentUp;
            input[ComputeCodes.TURN_INPUT_CURRENT_SOUTH] = currentSouth;
            input[ComputeCodes.TURN_INPUT_DESIRED_EAST] = desiredEast;
            input[ComputeCodes.TURN_INPUT_DESIRED_UP] = desiredUp;
            input[ComputeCodes.TURN_INPUT_DESIRED_SOUTH] = desiredSouth;
            double[] output = frame.turnOutput;
            Arrays.fill(output, 0.0D);
            int status = callTurn(header, input, output);
            if (status != ComputeCodes.STATUS_OK) {
                return REJECTED;
            }
            sink.directionEast = output[ComputeCodes.TURN_OUTPUT_EAST];
            sink.directionUp = output[ComputeCodes.TURN_OUTPUT_UP];
            sink.directionSouth = output[ComputeCodes.TURN_OUTPUT_SOUTH];
            return ACCEPTED;
        } finally {
            release();
        }
    }

    static int tracking(int currentPresent, double currentEast, double currentUp, double currentSouth,
                        int targetPresent, double targetEast, double targetUp, double targetSouth,
                        double baseSpeed, ComputeOutput sink) {
        ComputeLiveFrame frame = acquire();
        if (frame == null) {
            return UNSLOTTED;
        }
        try {
            int[] header = frame.trackingHeader;
            header[ComputeCodes.TRACKING_HEADER_CURRENT_PRESENT] = currentPresent;
            header[ComputeCodes.TRACKING_HEADER_TARGET_PRESENT] = targetPresent;
            double[] input = frame.trackingInput;
            input[ComputeCodes.TRACKING_INPUT_CURRENT_EAST] = currentEast;
            input[ComputeCodes.TRACKING_INPUT_CURRENT_UP] = currentUp;
            input[ComputeCodes.TRACKING_INPUT_CURRENT_SOUTH] = currentSouth;
            input[ComputeCodes.TRACKING_INPUT_TARGET_EAST] = targetEast;
            input[ComputeCodes.TRACKING_INPUT_TARGET_UP] = targetUp;
            input[ComputeCodes.TRACKING_INPUT_TARGET_SOUTH] = targetSouth;
            input[ComputeCodes.TRACKING_INPUT_BASE_SPEED] = baseSpeed;
            double[] output = frame.trackingOutput;
            Arrays.fill(output, 0.0D);
            int status = callTracking(header, input, output);
            if (status != ComputeCodes.STATUS_OK) {
                return REJECTED;
            }
            sink.directionEast = output[ComputeCodes.TRACKING_OUTPUT_DIRECTION_EAST];
            sink.directionUp = output[ComputeCodes.TRACKING_OUTPUT_DIRECTION_UP];
            sink.directionSouth = output[ComputeCodes.TRACKING_OUTPUT_DIRECTION_SOUTH];
            sink.stepLength = output[ComputeCodes.TRACKING_OUTPUT_STEP_LENGTH];
            return ACCEPTED;
        } finally {
            release();
        }
    }

    static int overlaps(double startEast, double startUp, double startSouth,
                        double forwardEast, double forwardUp, double forwardSouth,
                        double length, double width, double height,
                        double lowerEast, double lowerUp, double lowerSouth,
                        double upperEast, double upperUp, double upperSouth,
                        ComputeOutput sink) {
        ComputeLiveFrame frame = acquire();
        if (frame == null) {
            return UNSLOTTED;
        }
        try {
            double[] input = frame.overlapsInput;
            input[ComputeCodes.CORRIDOR_OVERLAPS_START_EAST] = startEast;
            input[ComputeCodes.CORRIDOR_OVERLAPS_START_UP] = startUp;
            input[ComputeCodes.CORRIDOR_OVERLAPS_START_SOUTH] = startSouth;
            input[ComputeCodes.CORRIDOR_OVERLAPS_FORWARD_EAST] = forwardEast;
            input[ComputeCodes.CORRIDOR_OVERLAPS_FORWARD_UP] = forwardUp;
            input[ComputeCodes.CORRIDOR_OVERLAPS_FORWARD_SOUTH] = forwardSouth;
            input[ComputeCodes.CORRIDOR_OVERLAPS_LENGTH] = length;
            input[ComputeCodes.CORRIDOR_OVERLAPS_WIDTH] = width;
            input[ComputeCodes.CORRIDOR_OVERLAPS_HEIGHT] = height;
            input[ComputeCodes.CORRIDOR_OVERLAPS_LOWER_EAST] = lowerEast;
            input[ComputeCodes.CORRIDOR_OVERLAPS_LOWER_UP] = lowerUp;
            input[ComputeCodes.CORRIDOR_OVERLAPS_LOWER_SOUTH] = lowerSouth;
            input[ComputeCodes.CORRIDOR_OVERLAPS_UPPER_EAST] = upperEast;
            input[ComputeCodes.CORRIDOR_OVERLAPS_UPPER_UP] = upperUp;
            input[ComputeCodes.CORRIDOR_OVERLAPS_UPPER_SOUTH] = upperSouth;
            int[] output = frame.overlapsOutput;
            output[0] = 0;
            int status = callOverlaps(input, output);
            if (status != ComputeCodes.STATUS_OK) {
                return REJECTED;
            }
            sink.positionPresent = output[0] == 0 ? 0 : 1;
            return ACCEPTED;
        } finally {
            release();
        }
    }

    static int contact(double startEast, double startUp, double startSouth,
                       double unitEast, double unitUp, double unitSouth,
                       double centerEast, double centerUp, double centerSouth,
                       double alongPad, ComputeOutput sink) {
        ComputeLiveFrame frame = acquire();
        if (frame == null) {
            return UNSLOTTED;
        }
        try {
            double[] input = frame.contactInput;
            input[ComputeCodes.CORRIDOR_CONTACT_START_EAST] = startEast;
            input[ComputeCodes.CORRIDOR_CONTACT_START_UP] = startUp;
            input[ComputeCodes.CORRIDOR_CONTACT_START_SOUTH] = startSouth;
            input[ComputeCodes.CORRIDOR_CONTACT_UNIT_EAST] = unitEast;
            input[ComputeCodes.CORRIDOR_CONTACT_UNIT_UP] = unitUp;
            input[ComputeCodes.CORRIDOR_CONTACT_UNIT_SOUTH] = unitSouth;
            input[ComputeCodes.CORRIDOR_CONTACT_CENTER_EAST] = centerEast;
            input[ComputeCodes.CORRIDOR_CONTACT_CENTER_UP] = centerUp;
            input[ComputeCodes.CORRIDOR_CONTACT_CENTER_SOUTH] = centerSouth;
            input[ComputeCodes.CORRIDOR_CONTACT_ALONG_PAD] = alongPad;
            double[] output = frame.contactOutput;
            Arrays.fill(output, 0.0D);
            int status = callContact(input, output);
            if (status != ComputeCodes.STATUS_OK) {
                return REJECTED;
            }
            sink.stepLength = output[0];
            return ACCEPTED;
        } finally {
            release();
        }
    }

    static int blocked(double contactDistance, double obstacleDistance, ComputeOutput sink) {
        ComputeLiveFrame frame = acquire();
        if (frame == null) {
            return UNSLOTTED;
        }
        try {
            double[] input = frame.blockedInput;
            input[ComputeCodes.CORRIDOR_BLOCKED_CONTACT] = contactDistance;
            input[ComputeCodes.CORRIDOR_BLOCKED_OBSTACLE] = obstacleDistance;
            int[] output = frame.blockedOutput;
            output[0] = 0;
            int status = callBlocked(input, output);
            if (status != ComputeCodes.STATUS_OK) {
                return REJECTED;
            }
            sink.positionPresent = output[0] == 0 ? 0 : 1;
            return ACCEPTED;
        } finally {
            release();
        }
    }

    private static int callTrajectory(int[] header, double[] input, double[] output) {
        try {
            return finish(XuexiComputeNative.trajectoryBatch(1, header, input, output));
        } catch (UnsatisfiedLinkError linkError) {
            return linkFailure(linkError);
        }
    }

    private static int callTurn(int[] header, double[] input, double[] output) {
        try {
            return finish(XuexiComputeNative.turnBatch(1, header, input, output));
        } catch (UnsatisfiedLinkError linkError) {
            return linkFailure(linkError);
        }
    }

    private static int callTracking(int[] header, double[] input, double[] output) {
        try {
            return finish(XuexiComputeNative.trackingStepBatch(1, header, input, output));
        } catch (UnsatisfiedLinkError linkError) {
            return linkFailure(linkError);
        }
    }

    private static int callOverlaps(double[] input, int[] output) {
        try {
            return finish(XuexiComputeNative.corridorOverlapsBatch(1, input, output));
        } catch (UnsatisfiedLinkError linkError) {
            return linkFailure(linkError);
        }
    }

    private static int callContact(double[] input, double[] output) {
        try {
            return finish(XuexiComputeNative.corridorContactBatch(1, input, output));
        } catch (UnsatisfiedLinkError linkError) {
            return linkFailure(linkError);
        }
    }

    private static int callBlocked(double[] input, int[] output) {
        try {
            return finish(XuexiComputeNative.corridorBlockedBatch(1, input, output));
        } catch (UnsatisfiedLinkError linkError) {
            return linkFailure(linkError);
        }
    }

    private static int finish(int status) {
        if (status != ComputeCodes.STATUS_OK) {
            ComputeCalls.reportStatusFailure(status);
        }
        return status;
    }

    private static int linkFailure(UnsatisfiedLinkError linkError) {
        ComputeSelection.disableNative("link-failed");
        String detail = linkError.getMessage();
        ComputeCalls.reportLinkFailure(detail == null ? "link" : detail);
        return ComputeCodes.STATUS_NATIVE_FAILURE;
    }

    private static ComputeLiveFrame acquire() {
        return FRAMES.get().acquire();
    }

    private static void release() {
        FRAMES.get().release();
    }

    private static final class ComputeLiveFrames {
        private int depth;
        private final ComputeLiveFrame[] frames = new ComputeLiveFrame[FRAME_LIMIT];

        private ComputeLiveFrame acquire() {
            if (depth >= FRAME_LIMIT) {
                return null;
            }
            ComputeLiveFrame frame = frames[depth];
            if (frame == null) {
                frame = new ComputeLiveFrame();
                frames[depth] = frame;
            }
            depth++;
            return frame;
        }

        private void release() {
            if (depth > 0) {
                depth--;
            }
        }
    }

    private static final class ComputeLiveFrame {
        private final int[] trajectoryHeader = new int[ComputeCodes.TRAJECTORY_HEADER_STRIDE];
        private final double[] trajectoryInput = new double[ComputeCodes.TRAJECTORY_INPUT_STRIDE];
        private final double[] trajectoryOutput = new double[ComputeCodes.TRAJECTORY_OUTPUT_STRIDE];
        private final int[] turnHeader = new int[ComputeCodes.TURN_HEADER_STRIDE];
        private final double[] turnInput = new double[ComputeCodes.TURN_INPUT_STRIDE];
        private final double[] turnOutput = new double[ComputeCodes.TURN_OUTPUT_STRIDE];
        private final int[] trackingHeader = new int[ComputeCodes.TRACKING_HEADER_STRIDE];
        private final double[] trackingInput = new double[ComputeCodes.TRACKING_INPUT_STRIDE];
        private final double[] trackingOutput = new double[ComputeCodes.TRACKING_OUTPUT_STRIDE];
        private final double[] overlapsInput = new double[ComputeCodes.CORRIDOR_OVERLAPS_INPUT_STRIDE];
        private final int[] overlapsOutput = new int[ComputeCodes.CORRIDOR_OVERLAPS_OUTPUT_STRIDE];
        private final double[] contactInput = new double[ComputeCodes.CORRIDOR_CONTACT_INPUT_STRIDE];
        private final double[] contactOutput = new double[ComputeCodes.CORRIDOR_CONTACT_OUTPUT_STRIDE];
        private final double[] blockedInput = new double[ComputeCodes.CORRIDOR_BLOCKED_INPUT_STRIDE];
        private final int[] blockedOutput = new int[ComputeCodes.CORRIDOR_BLOCKED_OUTPUT_STRIDE];
    }
}
