package com.bslx.XueXiMod.shu.compute;

/**
 * 现行公式的 Java 参考实现。标量路径只使用栈上的数字和线程复用出口，不按次分配数组。
 * 批量写入是同一标量公式的循环。C 必须跟这些表达式，不能另写近似。
 */
public final class JavaReference {
    /** 与 {@code Math.toRadians(12.0)} 相同，即 {@code (12.0 / 180.0) * Math.PI}。 */
    private static final double TURN_LIMIT_RADIANS = Math.toRadians(ComputeCodes.TURN_DEGREES);

    private JavaReference() {
    }

    public static double turnLimitRadians() {
        return TURN_LIMIT_RADIANS;
    }

    public static void sampleClosed(int trajectoryCode,
                                    int originPresent, double originEast, double originUp, double originSouth,
                                    int forwardPresent, double forwardEast, double forwardUp, double forwardSouth,
                                    double speed, int lifetimeTicks, int ageTicks,
                                    int targetPresent, double targetEast, double targetUp, double targetSouth,
                                    int followCurrentLook,
                                    int lookPresent, double lookEast, double lookUp, double lookSouth,
                                    ComputeOutput sink) {
        sink.ignoredFollow = followCurrentLook;
        if (originPresent == 0) {
            originEast = 0.0D;
            originUp = 0.0D;
            originSouth = 0.0D;
        }
        if (forwardPresent == 0) {
            forwardEast = 0.0D;
            forwardUp = 0.0D;
            forwardSouth = 0.0D;
        }
        if (targetPresent == 0) {
            targetEast = 0.0D;
            targetUp = 0.0D;
            targetSouth = 0.0D;
        }
        if (lookPresent == 0) {
            lookEast = 0.0D;
            lookUp = 0.0D;
            lookSouth = 0.0D;
        }
        double forwardUnitEast;
        double forwardUnitUp;
        double forwardUnitSouth;
        if (forwardPresent != 0 && trajectoryUnit(forwardEast, forwardUp, forwardSouth, sink)) {
            forwardUnitEast = sink.scratchEast;
            forwardUnitUp = sink.scratchUp;
            forwardUnitSouth = sink.scratchSouth;
        } else {
            forwardUnitEast = 0.0D;
            forwardUnitUp = 0.0D;
            forwardUnitSouth = 1.0D;
        }
        if (trajectoryCode == ComputeCodes.TRAJECTORY_STRAIGHT) {
            writeStraight(originEast, originUp, originSouth, forwardUnitEast, forwardUnitUp, forwardUnitSouth,
                    speed, ageTicks, targetPresent, targetEast, targetUp, targetSouth, sink);
            return;
        }
        if (trajectoryCode == ComputeCodes.TRAJECTORY_DROP) {
            double fall = ComputeCodes.DROP_LEADING * ComputeCodes.DROP_FACTOR * ageTicks * ageTicks;
            assignPosition(sink, 1, originEast, originUp - fall, originSouth);
            assignDirection(sink, 0.0D, -1.0D, 0.0D);
            return;
        }
        if (trajectoryCode == ComputeCodes.TRAJECTORY_FOLLOW_LOOK) {
            writeFollowLook(originPresent, originEast, originUp, originSouth,
                    forwardUnitEast, forwardUnitUp, forwardUnitSouth,
                    targetPresent, targetEast, targetUp, targetSouth,
                    lookPresent, lookEast, lookUp, lookSouth, sink);
            return;
        }
        if (trajectoryCode == ComputeCodes.TRAJECTORY_VORTEX) {
            writeVortex(originEast, originUp, originSouth, forwardUnitEast, forwardUnitUp, forwardUnitSouth,
                    lifetimeTicks, ageTicks, sink);
            return;
        }
        if (trajectoryCode == ComputeCodes.TRAJECTORY_PARABOLA) {
            double pitchedEast;
            double pitchedUp;
            double pitchedSouth;
            double horizontalSpan = Math.sqrt(forwardUnitEast * forwardUnitEast + forwardUnitSouth * forwardUnitSouth);
            double pitch = Math.atan2(forwardUnitUp, horizontalSpan) + Math.toRadians(ComputeCodes.PARABOLA_PITCH_DEGREES);
            double pitchedCos = Math.cos(pitch);
            double pitchedSin = Math.sin(pitch);
            if (horizontalSpan < 1.0E-8D) {
                pitchedEast = 0.0D;
                pitchedUp = forwardUnitUp >= 0.0D ? pitchedSin : -pitchedSin;
                pitchedSouth = pitchedCos;
            } else {
                pitchedEast = forwardUnitEast / horizontalSpan * pitchedCos;
                pitchedUp = pitchedSin;
                pitchedSouth = forwardUnitSouth / horizontalSpan * pitchedCos;
            }
            writeBallistic(originEast, originUp, originSouth,
                    pitchedEast * speed, pitchedUp * speed, pitchedSouth * speed,
                    ComputeCodes.BALLISTIC_GRAVITY, ageTicks, sink);
            return;
        }
        if (trajectoryCode == ComputeCodes.TRAJECTORY_UPWELL) {
            writeBallistic(originEast, originUp, originSouth, 0.0D, speed, 0.0D,
                    ComputeCodes.BALLISTIC_GRAVITY, ageTicks, sink);
            return;
        }
        writeStraight(originEast, originUp, originSouth, forwardUnitEast, forwardUnitUp, forwardUnitSouth,
                speed, ageTicks, 0, 0.0D, 0.0D, 0.0D, sink);
    }

    /**
     * 位置是原点加初速乘时间，再加半重力的竖直位移。方向是初速加上重力速度。
     * 调用方负责锥角、仰角和水平截断。
     */
    public record BallisticPoint(double positionEast, double positionUp, double positionSouth,
                                 double directionEast, double directionUp, double directionSouth) {
    }

    public static BallisticPoint ballisticPoint(double originEast, double originUp, double originSouth,
                                                double velocityEast, double velocityUp, double velocitySouth,
                                                double gravity, int ageTicks) {
        ComputeOutput sink = ComputeOutput.current();
        writeBallistic(originEast, originUp, originSouth, velocityEast, velocityUp, velocitySouth, gravity, ageTicks, sink);
        return new BallisticPoint(sink.positionEast, sink.positionUp, sink.positionSouth,
                sink.directionEast, sink.directionUp, sink.directionSouth);
    }

    static void writeBallistic(double originEast, double originUp, double originSouth,
                                      double velocityEast, double velocityUp, double velocitySouth,
                                      double gravity, int ageTicks, ComputeOutput sink) {
        double elapsed = Math.max(0, ageTicks);
        double drop = 0.5D * gravity * elapsed * elapsed;
        assignPosition(sink, 1,
                originEast + velocityEast * elapsed,
                originUp + velocityUp * elapsed - drop,
                originSouth + velocitySouth * elapsed);
        double directionEast = velocityEast;
        double directionUp = velocityUp - gravity * elapsed;
        double directionSouth = velocitySouth;
        double directionLength = Math.sqrt(directionEast * directionEast + directionUp * directionUp + directionSouth * directionSouth);
        if (directionLength < 1.0E-8D) {
            assignDirection(sink, 0.0D, -1.0D, 0.0D);
            return;
        }
        assignDirection(sink, directionEast / directionLength, directionUp / directionLength, directionSouth / directionLength);
    }

    public static void turnToward(int currentPresent, double currentEast, double currentUp, double currentSouth,
                                  int desiredPresent, double desiredEast, double desiredUp, double desiredSouth,
                                  ComputeOutput sink) {
        boolean currentReady = steeringUnit(currentPresent, currentEast, currentUp, currentSouth, sink);
        double currentUnitEast = currentReady ? sink.scratchEast : 0.0D;
        double currentUnitUp = currentReady ? sink.scratchUp : 0.0D;
        double currentUnitSouth = currentReady ? sink.scratchSouth : 0.0D;
        boolean desiredReady = steeringUnit(desiredPresent, desiredEast, desiredUp, desiredSouth, sink);
        double desiredUnitEast = desiredReady ? sink.scratchEast : 0.0D;
        double desiredUnitUp = desiredReady ? sink.scratchUp : 0.0D;
        double desiredUnitSouth = desiredReady ? sink.scratchSouth : 0.0D;
        if (!currentReady) {
            if (desiredReady) {
                assignDirection(sink, desiredUnitEast, desiredUnitUp, desiredUnitSouth);
            } else {
                assignDirection(sink, 0.0D, 0.0D, 1.0D);
            }
            return;
        }
        if (!desiredReady) {
            assignDirection(sink, currentUnitEast, currentUnitUp, currentUnitSouth);
            return;
        }
        double dot = currentUnitEast * desiredUnitEast + currentUnitUp * desiredUnitUp + currentUnitSouth * desiredUnitSouth;
        dot = Math.max(-1.0D, Math.min(1.0D, dot));
        double angle = Math.acos(dot);
        if (angle <= TURN_LIMIT_RADIANS) {
            assignDirection(sink, desiredUnitEast, desiredUnitUp, desiredUnitSouth);
            return;
        }
        double rawTangentEast = desiredUnitEast - currentUnitEast * dot;
        double rawTangentUp = desiredUnitUp - currentUnitUp * dot;
        double rawTangentSouth = desiredUnitSouth - currentUnitSouth * dot;
        double tangentEast;
        double tangentUp;
        double tangentSouth;
        if (steeringUnit(1, rawTangentEast, rawTangentUp, rawTangentSouth, sink)) {
            tangentEast = sink.scratchEast;
            tangentUp = sink.scratchUp;
            tangentSouth = sink.scratchSouth;
        } else {
            double helperEast;
            double helperUp;
            double helperSouth;
            if (Math.abs(currentUnitUp) < ComputeCodes.HELPER_ABSOLUTE_UP) {
                helperEast = 0.0D;
                helperUp = 1.0D;
                helperSouth = 0.0D;
            } else {
                helperEast = 1.0D;
                helperUp = 0.0D;
                helperSouth = 0.0D;
            }
            double crossEast = currentUnitUp * helperSouth - currentUnitSouth * helperUp;
            double crossUp = currentUnitSouth * helperEast - currentUnitEast * helperSouth;
            double crossSouth = currentUnitEast * helperUp - currentUnitUp * helperEast;
            minecraftNormalize(crossEast, crossUp, crossSouth, sink);
            tangentEast = sink.scratchEast;
            tangentUp = sink.scratchUp;
            tangentSouth = sink.scratchSouth;
        }
        double cosine = Math.cos(TURN_LIMIT_RADIANS);
        double sine = Math.sin(TURN_LIMIT_RADIANS);
        double mixedEast = currentUnitEast * cosine + tangentEast * sine;
        double mixedUp = currentUnitUp * cosine + tangentUp * sine;
        double mixedSouth = currentUnitSouth * cosine + tangentSouth * sine;
        minecraftNormalize(mixedEast, mixedUp, mixedSouth, sink);
        assignDirection(sink, sink.scratchEast, sink.scratchUp, sink.scratchSouth);
    }

    public static void trackingStep(int currentPresent, double currentEast, double currentUp, double currentSouth,
                                    int targetPresent, double targetEast, double targetUp, double targetSouth,
                                    double baseSpeed, ComputeOutput sink) {
        double travelSpeed = Double.isFinite(baseSpeed) && baseSpeed > 0.0D ? baseSpeed : 0.0D;
        unitOrForward(currentPresent, currentEast, currentUp, currentSouth, sink);
        double heldEast = sink.scratchEast;
        double heldUp = sink.scratchUp;
        double heldSouth = sink.scratchSouth;
        if (targetPresent == 0 || !Double.isFinite(targetEast) || !Double.isFinite(targetUp) || !Double.isFinite(targetSouth)) {
            assignDirection(sink, heldEast, heldUp, heldSouth);
            sink.stepLength = 0.0D;
            return;
        }
        double spanSquared = targetEast * targetEast + targetUp * targetUp + targetSouth * targetSouth;
        if (spanSquared < ComputeCodes.TRACK_ZERO_SPAN) {
            assignDirection(sink, heldEast, heldUp, heldSouth);
            sink.stepLength = 0.0D;
            return;
        }
        double distance = Math.sqrt(targetEast * targetEast + targetUp * targetUp + targetSouth * targetSouth);
        if (!Double.isFinite(distance) || distance < ComputeCodes.TRACK_ZERO_SPAN) {
            assignDirection(sink, heldEast, heldUp, heldSouth);
            sink.stepLength = 0.0D;
            return;
        }
        turnToward(1, heldEast, heldUp, heldSouth, 1, targetEast, targetUp, targetSouth, sink);
        unitOrForward(1, sink.directionEast, sink.directionUp, sink.directionSouth, sink);
        double nextEast = sink.scratchEast;
        double nextUp = sink.scratchUp;
        double nextSouth = sink.scratchSouth;
        double inverseDistance = 1.0D / distance;
        double targetUnitEast = targetEast * inverseDistance;
        double targetUnitUp = targetUp * inverseDistance;
        double targetUnitSouth = targetSouth * inverseDistance;
        double alignment = nextEast * targetUnitEast + nextUp * targetUnitUp + nextSouth * targetUnitSouth;
        if (!Double.isFinite(alignment)) {
            assignDirection(sink, nextEast, nextUp, nextSouth);
            sink.stepLength = 0.0D;
            return;
        }
        double clamped = alignment;
        if (clamped > 1.0D) {
            clamped = 1.0D;
        } else if (clamped < -1.0D) {
            clamped = -1.0D;
        }
        double remaining = Math.acos(clamped);
        if (!Double.isFinite(remaining) || remaining >= Math.PI * 0.5D) {
            assignDirection(sink, nextEast, nextUp, nextSouth);
            sink.stepLength = 0.0D;
            return;
        }
        double stepLength;
        if (remaining <= ComputeCodes.TRACK_ALIGNED) {
            stepLength = Math.min(travelSpeed, distance);
        } else {
            double turnSine = Math.sin(TURN_LIMIT_RADIANS);
            double speedCap = Math.min(travelSpeed, ComputeCodes.TRACK_APPROACH * distance * turnSine);
            double cosine = Math.cos(remaining);
            if (!Double.isFinite(cosine) || cosine < 0.0D) {
                cosine = 0.0D;
            }
            stepLength = speedCap * cosine;
        }
        if (!Double.isFinite(stepLength) || stepLength < 0.0D) {
            stepLength = 0.0D;
        }
        if (stepLength > travelSpeed) {
            stepLength = travelSpeed;
        }
        assignDirection(sink, nextEast, nextUp, nextSouth);
        sink.stepLength = stepLength;
    }

    public static boolean overlaps(double startX, double startY, double startZ,
                                   double forwardX, double forwardY, double forwardZ,
                                   double length, double width, double height,
                                   double lowerX, double lowerY, double lowerZ,
                                   double upperX, double upperY, double upperZ) {
        if (!Double.isFinite(length) || length <= 0.0D || width < 0.0D || height < 0.0D) {
            return false;
        }
        double forwardLength = Math.sqrt(forwardX * forwardX + forwardY * forwardY + forwardZ * forwardZ);
        if (forwardLength < ComputeCodes.CORRIDOR_AXIS_EPSILON) {
            return false;
        }
        double unitX = forwardX / forwardLength;
        double unitY = forwardY / forwardLength;
        double unitZ = forwardZ / forwardLength;
        double hintX = Math.abs(unitY) > ComputeCodes.CORRIDOR_HINT_ABSOLUTE ? 1.0D : 0.0D;
        double hintY = Math.abs(unitY) > ComputeCodes.CORRIDOR_HINT_ABSOLUTE ? 0.0D : 1.0D;
        double hintZ = 0.0D;
        double rightX = unitY * hintZ - unitZ * hintY;
        double rightY = unitZ * hintX - unitX * hintZ;
        double rightZ = unitX * hintY - unitY * hintX;
        double rightLength = Math.sqrt(rightX * rightX + rightY * rightY + rightZ * rightZ);
        if (rightLength < ComputeCodes.CORRIDOR_AXIS_EPSILON) {
            return false;
        }
        rightX /= rightLength;
        rightY /= rightLength;
        rightZ /= rightLength;
        double upX = rightY * unitZ - rightZ * unitY;
        double upY = rightZ * unitX - rightX * unitZ;
        double upZ = rightX * unitY - rightY * unitX;
        double centerX = (lowerX + upperX) * 0.5D;
        double centerY = (lowerY + upperY) * 0.5D;
        double centerZ = (lowerZ + upperZ) * 0.5D;
        double halfX = Math.abs(upperX - lowerX) * 0.5D;
        double halfY = Math.abs(upperY - lowerY) * 0.5D;
        double halfZ = Math.abs(upperZ - lowerZ) * 0.5D;
        double offsetX = centerX - startX;
        double offsetY = centerY - startY;
        double offsetZ = centerZ - startZ;
        double along = offsetX * unitX + offsetY * unitY + offsetZ * unitZ;
        double lateral = offsetX * rightX + offsetY * rightY + offsetZ * rightZ;
        double vertical = offsetX * upX + offsetY * upY + offsetZ * upZ;
        double alongPad = halfX * Math.abs(unitX) + halfY * Math.abs(unitY) + halfZ * Math.abs(unitZ);
        double lateralPad = halfX * Math.abs(rightX) + halfY * Math.abs(rightY) + halfZ * Math.abs(rightZ);
        double verticalPad = halfX * Math.abs(upX) + halfY * Math.abs(upY) + halfZ * Math.abs(upZ);
        boolean alongHit = along + alongPad >= 0.0D && along - alongPad <= length;
        boolean lateralHit = Math.abs(lateral) <= width * 0.5D + lateralPad;
        boolean verticalHit = Math.abs(vertical) <= height * 0.5D + verticalPad;
        return alongHit && lateralHit && verticalHit;
    }

    public static double contactDistance(double startX, double startY, double startZ,
                                         double unitX, double unitY, double unitZ,
                                         double centerX, double centerY, double centerZ,
                                         double alongPad) {
        double offsetX = centerX - startX;
        double offsetY = centerY - startY;
        double offsetZ = centerZ - startZ;
        double along = offsetX * unitX + offsetY * unitY + offsetZ * unitZ;
        double near = along - Math.max(0.0D, alongPad);
        return near < 0.0D ? 0.0D : near;
    }

    public static boolean blocked(double contactDistance, double obstacleDistance) {
        if (!Double.isFinite(contactDistance) || contactDistance < 0.0D) {
            return true;
        }
        if (!Double.isFinite(obstacleDistance)) {
            return false;
        }
        return obstacleDistance + ComputeCodes.CORRIDOR_BLOCK_SLACK < contactDistance;
    }

    public static int prepareTrajectory(int count, int[] header, double[] input, double[] output) {
        return prepareTrio(count, header, ComputeCodes.TRAJECTORY_HEADER_STRIDE,
                input, ComputeCodes.TRAJECTORY_INPUT_STRIDE,
                output, ComputeCodes.TRAJECTORY_OUTPUT_STRIDE);
    }

    public static int writeTrajectory(int count, int[] header, double[] input, double[] output) {
        int status = prepareTrajectory(count, header, input, output);
        if (status != ComputeCodes.STATUS_OK || count == 0) {
            return status;
        }
        ComputeOutput sink = ComputeOutput.current();
        for (int item = 0; item < count; item++) {
            int headerBase = item * ComputeCodes.TRAJECTORY_HEADER_STRIDE;
            int inputBase = item * ComputeCodes.TRAJECTORY_INPUT_STRIDE;
            int outputBase = item * ComputeCodes.TRAJECTORY_OUTPUT_STRIDE;
            sampleClosed(
                    header[headerBase + ComputeCodes.TRAJECTORY_HEADER_CODE],
                    header[headerBase + ComputeCodes.TRAJECTORY_HEADER_ORIGIN_PRESENT],
                    input[inputBase + ComputeCodes.TRAJECTORY_INPUT_ORIGIN_EAST],
                    input[inputBase + ComputeCodes.TRAJECTORY_INPUT_ORIGIN_UP],
                    input[inputBase + ComputeCodes.TRAJECTORY_INPUT_ORIGIN_SOUTH],
                    header[headerBase + ComputeCodes.TRAJECTORY_HEADER_FORWARD_PRESENT],
                    input[inputBase + ComputeCodes.TRAJECTORY_INPUT_FORWARD_EAST],
                    input[inputBase + ComputeCodes.TRAJECTORY_INPUT_FORWARD_UP],
                    input[inputBase + ComputeCodes.TRAJECTORY_INPUT_FORWARD_SOUTH],
                    input[inputBase + ComputeCodes.TRAJECTORY_INPUT_SPEED],
                    header[headerBase + ComputeCodes.TRAJECTORY_HEADER_LIFETIME_TICKS],
                    header[headerBase + ComputeCodes.TRAJECTORY_HEADER_AGE_TICKS],
                    header[headerBase + ComputeCodes.TRAJECTORY_HEADER_TARGET_PRESENT],
                    input[inputBase + ComputeCodes.TRAJECTORY_INPUT_TARGET_EAST],
                    input[inputBase + ComputeCodes.TRAJECTORY_INPUT_TARGET_UP],
                    input[inputBase + ComputeCodes.TRAJECTORY_INPUT_TARGET_SOUTH],
                    header[headerBase + ComputeCodes.TRAJECTORY_HEADER_FOLLOW_CURRENT_LOOK],
                    header[headerBase + ComputeCodes.TRAJECTORY_HEADER_LOOK_PRESENT],
                    input[inputBase + ComputeCodes.TRAJECTORY_INPUT_LOOK_EAST],
                    input[inputBase + ComputeCodes.TRAJECTORY_INPUT_LOOK_UP],
                    input[inputBase + ComputeCodes.TRAJECTORY_INPUT_LOOK_SOUTH],
                    sink);
            output[outputBase + ComputeCodes.TRAJECTORY_OUTPUT_POSITION_PRESENT] = sink.positionPresent == 0 ? 0.0D : 1.0D;
            output[outputBase + ComputeCodes.TRAJECTORY_OUTPUT_POSITION_EAST] = sink.positionEast;
            output[outputBase + ComputeCodes.TRAJECTORY_OUTPUT_POSITION_UP] = sink.positionUp;
            output[outputBase + ComputeCodes.TRAJECTORY_OUTPUT_POSITION_SOUTH] = sink.positionSouth;
            output[outputBase + ComputeCodes.TRAJECTORY_OUTPUT_DIRECTION_EAST] = sink.directionEast;
            output[outputBase + ComputeCodes.TRAJECTORY_OUTPUT_DIRECTION_UP] = sink.directionUp;
            output[outputBase + ComputeCodes.TRAJECTORY_OUTPUT_DIRECTION_SOUTH] = sink.directionSouth;
        }
        return ComputeCodes.STATUS_OK;
    }

    public static int prepareTurn(int count, int[] header, double[] input, double[] output) {
        return prepareTrio(count, header, ComputeCodes.TURN_HEADER_STRIDE,
                input, ComputeCodes.TURN_INPUT_STRIDE,
                output, ComputeCodes.TURN_OUTPUT_STRIDE);
    }

    public static int writeTurn(int count, int[] header, double[] input, double[] output) {
        int status = prepareTurn(count, header, input, output);
        if (status != ComputeCodes.STATUS_OK || count == 0) {
            return status;
        }
        ComputeOutput sink = ComputeOutput.current();
        for (int item = 0; item < count; item++) {
            int headerBase = item * ComputeCodes.TURN_HEADER_STRIDE;
            int inputBase = item * ComputeCodes.TURN_INPUT_STRIDE;
            int outputBase = item * ComputeCodes.TURN_OUTPUT_STRIDE;
            turnToward(
                    header[headerBase + ComputeCodes.TURN_HEADER_CURRENT_PRESENT],
                    input[inputBase + ComputeCodes.TURN_INPUT_CURRENT_EAST],
                    input[inputBase + ComputeCodes.TURN_INPUT_CURRENT_UP],
                    input[inputBase + ComputeCodes.TURN_INPUT_CURRENT_SOUTH],
                    header[headerBase + ComputeCodes.TURN_HEADER_DESIRED_PRESENT],
                    input[inputBase + ComputeCodes.TURN_INPUT_DESIRED_EAST],
                    input[inputBase + ComputeCodes.TURN_INPUT_DESIRED_UP],
                    input[inputBase + ComputeCodes.TURN_INPUT_DESIRED_SOUTH],
                    sink);
            output[outputBase + ComputeCodes.TURN_OUTPUT_EAST] = sink.directionEast;
            output[outputBase + ComputeCodes.TURN_OUTPUT_UP] = sink.directionUp;
            output[outputBase + ComputeCodes.TURN_OUTPUT_SOUTH] = sink.directionSouth;
        }
        return ComputeCodes.STATUS_OK;
    }

    public static int prepareTracking(int count, int[] header, double[] input, double[] output) {
        return prepareTrio(count, header, ComputeCodes.TRACKING_HEADER_STRIDE,
                input, ComputeCodes.TRACKING_INPUT_STRIDE,
                output, ComputeCodes.TRACKING_OUTPUT_STRIDE);
    }

    public static int writeTracking(int count, int[] header, double[] input, double[] output) {
        int status = prepareTracking(count, header, input, output);
        if (status != ComputeCodes.STATUS_OK || count == 0) {
            return status;
        }
        ComputeOutput sink = ComputeOutput.current();
        for (int item = 0; item < count; item++) {
            int headerBase = item * ComputeCodes.TRACKING_HEADER_STRIDE;
            int inputBase = item * ComputeCodes.TRACKING_INPUT_STRIDE;
            int outputBase = item * ComputeCodes.TRACKING_OUTPUT_STRIDE;
            trackingStep(
                    header[headerBase + ComputeCodes.TRACKING_HEADER_CURRENT_PRESENT],
                    input[inputBase + ComputeCodes.TRACKING_INPUT_CURRENT_EAST],
                    input[inputBase + ComputeCodes.TRACKING_INPUT_CURRENT_UP],
                    input[inputBase + ComputeCodes.TRACKING_INPUT_CURRENT_SOUTH],
                    header[headerBase + ComputeCodes.TRACKING_HEADER_TARGET_PRESENT],
                    input[inputBase + ComputeCodes.TRACKING_INPUT_TARGET_EAST],
                    input[inputBase + ComputeCodes.TRACKING_INPUT_TARGET_UP],
                    input[inputBase + ComputeCodes.TRACKING_INPUT_TARGET_SOUTH],
                    input[inputBase + ComputeCodes.TRACKING_INPUT_BASE_SPEED],
                    sink);
            output[outputBase + ComputeCodes.TRACKING_OUTPUT_DIRECTION_EAST] = sink.directionEast;
            output[outputBase + ComputeCodes.TRACKING_OUTPUT_DIRECTION_UP] = sink.directionUp;
            output[outputBase + ComputeCodes.TRACKING_OUTPUT_DIRECTION_SOUTH] = sink.directionSouth;
            output[outputBase + ComputeCodes.TRACKING_OUTPUT_STEP_LENGTH] = sink.stepLength;
        }
        return ComputeCodes.STATUS_OK;
    }

    public static int prepareOverlaps(int count, double[] input, int[] output) {
        return preparePair(count, input, ComputeCodes.CORRIDOR_OVERLAPS_INPUT_STRIDE,
                output, ComputeCodes.CORRIDOR_OVERLAPS_OUTPUT_STRIDE);
    }

    public static int writeOverlaps(int count, double[] input, int[] output) {
        int status = prepareOverlaps(count, input, output);
        if (status != ComputeCodes.STATUS_OK || count == 0) {
            return status;
        }
        for (int item = 0; item < count; item++) {
            int inputBase = item * ComputeCodes.CORRIDOR_OVERLAPS_INPUT_STRIDE;
            boolean overlap = overlaps(
                    input[inputBase + ComputeCodes.CORRIDOR_OVERLAPS_START_EAST],
                    input[inputBase + ComputeCodes.CORRIDOR_OVERLAPS_START_UP],
                    input[inputBase + ComputeCodes.CORRIDOR_OVERLAPS_START_SOUTH],
                    input[inputBase + ComputeCodes.CORRIDOR_OVERLAPS_FORWARD_EAST],
                    input[inputBase + ComputeCodes.CORRIDOR_OVERLAPS_FORWARD_UP],
                    input[inputBase + ComputeCodes.CORRIDOR_OVERLAPS_FORWARD_SOUTH],
                    input[inputBase + ComputeCodes.CORRIDOR_OVERLAPS_LENGTH],
                    input[inputBase + ComputeCodes.CORRIDOR_OVERLAPS_WIDTH],
                    input[inputBase + ComputeCodes.CORRIDOR_OVERLAPS_HEIGHT],
                    input[inputBase + ComputeCodes.CORRIDOR_OVERLAPS_LOWER_EAST],
                    input[inputBase + ComputeCodes.CORRIDOR_OVERLAPS_LOWER_UP],
                    input[inputBase + ComputeCodes.CORRIDOR_OVERLAPS_LOWER_SOUTH],
                    input[inputBase + ComputeCodes.CORRIDOR_OVERLAPS_UPPER_EAST],
                    input[inputBase + ComputeCodes.CORRIDOR_OVERLAPS_UPPER_UP],
                    input[inputBase + ComputeCodes.CORRIDOR_OVERLAPS_UPPER_SOUTH]);
            output[item] = overlap ? 1 : 0;
        }
        return ComputeCodes.STATUS_OK;
    }

    public static int prepareContact(int count, double[] input, double[] output) {
        int countStatus = checkCount(count);
        if (countStatus != ComputeCodes.STATUS_OK || count == 0) {
            return countStatus;
        }
        if (input == null || output == null) {
            return ComputeCodes.STATUS_NULL_BUFFER;
        }
        if (input == output) {
            return ComputeCodes.STATUS_ALIAS;
        }
        int inputStatus = checkSpan(count, ComputeCodes.CORRIDOR_CONTACT_INPUT_STRIDE, input.length, false, false);
        if (inputStatus != ComputeCodes.STATUS_OK) {
            return inputStatus;
        }
        return checkSpan(count, ComputeCodes.CORRIDOR_CONTACT_OUTPUT_STRIDE, output.length, true, false);
    }

    public static int writeContact(int count, double[] input, double[] output) {
        int status = prepareContact(count, input, output);
        if (status != ComputeCodes.STATUS_OK || count == 0) {
            return status;
        }
        for (int item = 0; item < count; item++) {
            int inputBase = item * ComputeCodes.CORRIDOR_CONTACT_INPUT_STRIDE;
            output[item] = contactDistance(
                    input[inputBase + ComputeCodes.CORRIDOR_CONTACT_START_EAST],
                    input[inputBase + ComputeCodes.CORRIDOR_CONTACT_START_UP],
                    input[inputBase + ComputeCodes.CORRIDOR_CONTACT_START_SOUTH],
                    input[inputBase + ComputeCodes.CORRIDOR_CONTACT_UNIT_EAST],
                    input[inputBase + ComputeCodes.CORRIDOR_CONTACT_UNIT_UP],
                    input[inputBase + ComputeCodes.CORRIDOR_CONTACT_UNIT_SOUTH],
                    input[inputBase + ComputeCodes.CORRIDOR_CONTACT_CENTER_EAST],
                    input[inputBase + ComputeCodes.CORRIDOR_CONTACT_CENTER_UP],
                    input[inputBase + ComputeCodes.CORRIDOR_CONTACT_CENTER_SOUTH],
                    input[inputBase + ComputeCodes.CORRIDOR_CONTACT_ALONG_PAD]);
        }
        return ComputeCodes.STATUS_OK;
    }

    public static int prepareBlocked(int count, double[] input, int[] output) {
        return preparePair(count, input, ComputeCodes.CORRIDOR_BLOCKED_INPUT_STRIDE,
                output, ComputeCodes.CORRIDOR_BLOCKED_OUTPUT_STRIDE);
    }

    public static int writeBlocked(int count, double[] input, int[] output) {
        int status = prepareBlocked(count, input, output);
        if (status != ComputeCodes.STATUS_OK || count == 0) {
            return status;
        }
        for (int item = 0; item < count; item++) {
            int inputBase = item * ComputeCodes.CORRIDOR_BLOCKED_INPUT_STRIDE;
            boolean blocks = blocked(
                    input[inputBase + ComputeCodes.CORRIDOR_BLOCKED_CONTACT],
                    input[inputBase + ComputeCodes.CORRIDOR_BLOCKED_OBSTACLE]);
            output[item] = blocks ? 1 : 0;
        }
        return ComputeCodes.STATUS_OK;
    }

    private static void writeStraight(double originEast, double originUp, double originSouth,
                                      double forwardEast, double forwardUp, double forwardSouth,
                                      double speed, int ageTicks,
                                      int targetPresent, double targetEast, double targetUp, double targetSouth,
                                      ComputeOutput sink) {
        double directionEast = forwardEast;
        double directionUp = forwardUp;
        double directionSouth = forwardSouth;
        if (targetPresent != 0) {
            double deltaEast = targetEast - originEast;
            double deltaUp = targetUp - originUp;
            double deltaSouth = targetSouth - originSouth;
            if (trajectoryUnit(deltaEast, deltaUp, deltaSouth, sink)) {
                directionEast = sink.scratchEast;
                directionUp = sink.scratchUp;
                directionSouth = sink.scratchSouth;
            }
        }
        double travel = speed * ageTicks;
        assignPosition(sink, 1, originEast + directionEast * travel, originUp + directionUp * travel,
                originSouth + directionSouth * travel);
        assignDirection(sink, directionEast, directionUp, directionSouth);
    }

    private static void writeFollowLook(int originPresent, double originEast, double originUp, double originSouth,
                                        double forwardEast, double forwardUp, double forwardSouth,
                                        int targetPresent, double targetEast, double targetUp, double targetSouth,
                                        int lookPresent, double lookEast, double lookUp, double lookSouth,
                                        ComputeOutput sink) {
        double directionEast;
        double directionUp;
        double directionSouth;
        if (lookPresent != 0 && trajectoryUnit(lookEast, lookUp, lookSouth, sink)) {
            directionEast = sink.scratchEast;
            directionUp = sink.scratchUp;
            directionSouth = sink.scratchSouth;
        } else {
            directionEast = forwardEast;
            directionUp = forwardUp;
            directionSouth = forwardSouth;
        }
        if (targetPresent != 0) {
            double deltaEast = targetEast - originEast;
            double deltaUp = targetUp - originUp;
            double deltaSouth = targetSouth - originSouth;
            if (trajectoryUnit(deltaEast, deltaUp, deltaSouth, sink)) {
                directionEast = sink.scratchEast;
                directionUp = sink.scratchUp;
                directionSouth = sink.scratchSouth;
            } else if (lookPresent != 0) {
                directionEast = lookEast;
                directionUp = lookUp;
                directionSouth = lookSouth;
            } else {
                directionEast = forwardEast;
                directionUp = forwardUp;
                directionSouth = forwardSouth;
            }
        }
        if (originPresent == 0) {
            assignPosition(sink, 0, 0.0D, 0.0D, 0.0D);
        } else {
            assignPosition(sink, 1, originEast, originUp, originSouth);
        }
        assignDirection(sink, directionEast, directionUp, directionSouth);
    }

    private static void writeVortex(double originEast, double originUp, double originSouth,
                                    double forwardEast, double forwardUp, double forwardSouth,
                                    int lifetimeTicks, int ageTicks, ComputeOutput sink) {
        int spiralTicks = Math.max(ComputeCodes.VORTEX_SPIRAL_TICK_FLOOR, (lifetimeTicks * 2) / 3);
        if (ageTicks >= spiralTicks) {
            if (trajectoryUnit(forwardEast, 0.0D, forwardSouth, sink)) {
                assignDirection(sink, sink.scratchEast, sink.scratchUp, sink.scratchSouth);
            } else {
                assignDirection(sink, 0.0D, 0.0D, 1.0D);
            }
            assignPosition(sink, 1, originEast, originUp, originSouth);
            return;
        }
        double spiralProgress = ageTicks / (double) spiralTicks;
        double phase = Math.atan2(forwardSouth, forwardEast);
        double angle = phase + spiralProgress * ComputeCodes.VORTEX_TURN_COUNT * Math.PI * 2.0D;
        double radius = ComputeCodes.VORTEX_MOUTH_RADIUS * (1.0D - spiralProgress);
        double height = ComputeCodes.VORTEX_COLUMN_HEIGHT * (1.0D - spiralProgress);
        double spinRate = ComputeCodes.VORTEX_TURN_COUNT * Math.PI * 2.0D / spiralTicks;
        double shrinkRate = -ComputeCodes.VORTEX_MOUTH_RADIUS / spiralTicks;
        double dropRate = -ComputeCodes.VORTEX_COLUMN_HEIGHT / spiralTicks;
        double positionEast = originEast + Math.cos(angle) * radius;
        double positionUp = originUp + height;
        double positionSouth = originSouth + Math.sin(angle) * radius;
        double tangentEast = -Math.sin(angle) * radius * spinRate + Math.cos(angle) * shrinkRate;
        double tangentUp = dropRate;
        double tangentSouth = Math.cos(angle) * radius * spinRate + Math.sin(angle) * shrinkRate;
        if (trajectoryUnit(tangentEast, tangentUp, tangentSouth, sink)) {
            assignDirection(sink, sink.scratchEast, sink.scratchUp, sink.scratchSouth);
        } else {
            assignDirection(sink, 0.0D, -1.0D, 0.0D);
        }
        assignPosition(sink, 1, positionEast, positionUp, positionSouth);
    }

    private static boolean trajectoryUnit(double east, double upward, double south, ComputeOutput sink) {
        double lengthSquared = east * east + upward * upward + south * south;
        if (lengthSquared < ComputeCodes.TRAJECTORY_LENGTH_SQUARED_LIMIT) {
            return false;
        }
        minecraftNormalize(east, upward, south, sink);
        return true;
    }

    private static void minecraftNormalize(double east, double upward, double south, ComputeOutput sink) {
        double length = Math.sqrt(east * east + upward * upward + south * south);
        if (length < ComputeCodes.MINECRAFT_NORMALIZE_LENGTH) {
            sink.scratchEast = 0.0D;
            sink.scratchUp = 0.0D;
            sink.scratchSouth = 0.0D;
            return;
        }
        sink.scratchEast = east / length;
        sink.scratchUp = upward / length;
        sink.scratchSouth = south / length;
    }

    private static boolean steeringUnit(int present, double east, double upward, double south, ComputeOutput sink) {
        if (present == 0 || !Double.isFinite(east) || !Double.isFinite(upward) || !Double.isFinite(south)) {
            return false;
        }
        double length = Math.sqrt(east * east + upward * upward + south * south);
        if (!Double.isFinite(length) || length < ComputeCodes.STEERING_LENGTH_LIMIT) {
            return false;
        }
        double inverse = 1.0D / length;
        sink.scratchEast = east * inverse;
        sink.scratchUp = upward * inverse;
        sink.scratchSouth = south * inverse;
        return true;
    }

    private static void unitOrForward(int present, double east, double upward, double south, ComputeOutput sink) {
        if (present == 0 || !Double.isFinite(east) || !Double.isFinite(upward) || !Double.isFinite(south)) {
            sink.scratchEast = 0.0D;
            sink.scratchUp = 0.0D;
            sink.scratchSouth = 1.0D;
            return;
        }
        double length = Math.sqrt(east * east + upward * upward + south * south);
        if (!Double.isFinite(length) || length < ComputeCodes.TRACK_ZERO_SPAN) {
            sink.scratchEast = 0.0D;
            sink.scratchUp = 0.0D;
            sink.scratchSouth = 1.0D;
            return;
        }
        double inverse = 1.0D / length;
        sink.scratchEast = east * inverse;
        sink.scratchUp = upward * inverse;
        sink.scratchSouth = south * inverse;
    }

    private static void assignPosition(ComputeOutput sink, int present, double east, double upward, double south) {
        sink.positionPresent = present;
        sink.positionEast = east;
        sink.positionUp = upward;
        sink.positionSouth = south;
    }

    private static void assignDirection(ComputeOutput sink, double east, double upward, double south) {
        sink.directionEast = east;
        sink.directionUp = upward;
        sink.directionSouth = south;
    }

    private static int prepareTrio(int count, int[] header, int headerStride,
                                   double[] input, int inputStride,
                                   double[] output, int outputStride) {
        int countStatus = checkCount(count);
        if (countStatus != ComputeCodes.STATUS_OK || count == 0) {
            return countStatus;
        }
        if (header == null || input == null || output == null) {
            return ComputeCodes.STATUS_NULL_BUFFER;
        }
        if (input == output) {
            return ComputeCodes.STATUS_ALIAS;
        }
        int headerStatus = checkSpan(count, headerStride, header.length, false, true);
        if (headerStatus != ComputeCodes.STATUS_OK) {
            return headerStatus;
        }
        int inputStatus = checkSpan(count, inputStride, input.length, false, false);
        if (inputStatus != ComputeCodes.STATUS_OK) {
            return inputStatus;
        }
        return checkSpan(count, outputStride, output.length, true, false);
    }

    private static int preparePair(int count, double[] input, int inputStride, int[] output, int outputStride) {
        int countStatus = checkCount(count);
        if (countStatus != ComputeCodes.STATUS_OK || count == 0) {
            return countStatus;
        }
        if (input == null || output == null) {
            return ComputeCodes.STATUS_NULL_BUFFER;
        }
        int inputStatus = checkSpan(count, inputStride, input.length, false, false);
        if (inputStatus != ComputeCodes.STATUS_OK) {
            return inputStatus;
        }
        return checkSpan(count, outputStride, output.length, true, false);
    }

    private static int checkCount(int count) {
        if (count < 0 || count > ComputeCodes.COMPUTE_BATCH_LIMIT) {
            return ComputeCodes.STATUS_BAD_COUNT;
        }
        return ComputeCodes.STATUS_OK;
    }

    private static int checkSpan(int count, int stride, int arrayLength, boolean outputBuffer, boolean headerBuffer) {
        long need = (long) count * (long) stride;
        if (need > Integer.MAX_VALUE) {
            return ComputeCodes.STATUS_OVERFLOW;
        }
        if ((long) arrayLength < need) {
            if (headerBuffer) {
                return ComputeCodes.STATUS_BAD_HEADER;
            }
            return outputBuffer ? ComputeCodes.STATUS_BAD_CAPACITY : ComputeCodes.STATUS_BAD_LENGTH;
        }
        return ComputeCodes.STATUS_OK;
    }
}
