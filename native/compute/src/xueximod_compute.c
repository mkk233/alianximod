/*
 * ABI 1 numeric kernel for xueximod_compute.
 * Formulas follow com.bslx.XueXiMod.shu.compute.JavaReference.
 * sin, cos, atan2, acos, and sqrt come from math.h. They are not promised
 * to match HotSpot bit for bit. No polynomial replacement and no fast-math.
 * This file does not read the world, entities, NBT, random numbers, files,
 * or the network, and it does not install a signal handler.
 */

#if defined(_MSC_VER)
#pragma fp_contract (off)
#else
#pragma STDC FP_CONTRACT OFF
#endif

#include <jni.h>
#include <math.h>
#include <stdint.h>
#include <string.h>

typedef char xueximod_jint_must_be_32_bits[(sizeof(jint) == 4) ? 1 : -1];
typedef char xueximod_jdouble_must_be_64_bits[(sizeof(jdouble) == 8) ? 1 : -1];

enum {
    STATUS_OK = 0,
    STATUS_BAD_VERSION = 1,
    STATUS_BAD_COUNT = 2,
    STATUS_BAD_LENGTH = 3,
    STATUS_BAD_CAPACITY = 4,
    STATUS_OVERFLOW = 5,
    STATUS_ALIAS = 6,
    STATUS_UNSUPPORTED = 7,
    STATUS_NATIVE_FAILURE = 8,
    STATUS_NULL_BUFFER = 9,
    STATUS_BAD_HEADER = 10
};

enum {
    ABI_VERSION = 1,
    COMPUTE_BATCH_LIMIT = 4096,
    CAPABILITY_TRAJECTORY = 1,
    CAPABILITY_TURN = 2,
    CAPABILITY_TRACKING_STEP = 4,
    CAPABILITY_CORRIDOR_OVERLAPS = 8,
    CAPABILITY_CORRIDOR_CONTACT = 16,
    CAPABILITY_CORRIDOR_BLOCKED = 32,
    CAPABILITY_ALL = 63
};

enum {
    TRAJECTORY_STRAIGHT = 0,
    TRAJECTORY_DROP = 1,
    TRAJECTORY_FOLLOW_LOOK = 2,
    TRAJECTORY_VORTEX = 4,
    TRAJECTORY_HEADER_STRIDE = 8,
    TRAJECTORY_INPUT_STRIDE = 13,
    TRAJECTORY_OUTPUT_STRIDE = 7,
    TURN_HEADER_STRIDE = 2,
    TURN_INPUT_STRIDE = 6,
    TURN_OUTPUT_STRIDE = 3,
    TRACKING_HEADER_STRIDE = 2,
    TRACKING_INPUT_STRIDE = 7,
    TRACKING_OUTPUT_STRIDE = 4,
    CORRIDOR_OVERLAPS_INPUT_STRIDE = 15,
    CORRIDOR_OVERLAPS_OUTPUT_STRIDE = 1,
    CORRIDOR_CONTACT_INPUT_STRIDE = 10,
    CORRIDOR_CONTACT_OUTPUT_STRIDE = 1,
    CORRIDOR_BLOCKED_INPUT_STRIDE = 2,
    CORRIDOR_BLOCKED_OUTPUT_STRIDE = 1,
    VORTEX_SPIRAL_TICK_FLOOR = 8
};

static const double TRAJECTORY_LENGTH_SQUARED_LIMIT = 1.0e-8;
static const double MINECRAFT_NORMALIZE_LENGTH = 1.0e-4;
static const double STEERING_LENGTH_LIMIT = 1.0e-8;
static const double HELPER_ABSOLUTE_UP = 0.9;
static const double DROP_LEADING = 0.5;
static const double DROP_FACTOR = 0.08;
static const double VORTEX_MOUTH_RADIUS = 5.0;
static const double VORTEX_COLUMN_HEIGHT = 12.0;
static const double VORTEX_TURN_COUNT = 3.0;
static const double TRACK_ALIGNED = 1.0e-4;
static const double TRACK_ZERO_SPAN = 1.0e-8;
static const double TRACK_APPROACH = 0.8;
static const double CORRIDOR_AXIS_EPSILON = 1.0e-8;
static const double CORRIDOR_BLOCK_SLACK = 1.0e-4;
static const double CORRIDOR_HINT_ABSOLUTE = 0.9;

typedef struct ComputeSink {
    double positionEast;
    double positionUp;
    double positionSouth;
    int positionPresent;
    double directionEast;
    double directionUp;
    double directionSouth;
    double stepLength;
    double scratchEast;
    double scratchUp;
    double scratchSouth;
} ComputeSink;

static double java_pi(void) {
    uint64_t bits = 0x400921FB54442D18ULL;
    double value;
    memcpy(&value, &bits, sizeof value);
    return value;
}

static double java_max_finite(void) {
    uint64_t bits = 0x7FEFFFFFFFFFFFFFULL;
    double value;
    memcpy(&value, &bits, sizeof value);
    return value;
}

static double java_abs(double value) {
    return (value <= 0.0) ? (0.0 - value) : value;
}

static int value_is_finite(double value) {
    return java_abs(value) <= java_max_finite();
}

static double java_min(double left, double right) {
    uint64_t rightBits;
    if (left != left) {
        return left;
    }
    if (left == 0.0 && right == 0.0) {
        memcpy(&rightBits, &right, sizeof rightBits);
        if (rightBits == 0x8000000000000000ULL) {
            return right;
        }
    }
    return (left <= right) ? left : right;
}

static double java_max(double left, double right) {
    uint64_t leftBits;
    if (left != left) {
        return left;
    }
    if (left == 0.0 && right == 0.0) {
        memcpy(&leftBits, &left, sizeof leftBits);
        if (leftBits == 0x8000000000000000ULL) {
            return right;
        }
    }
    return (left >= right) ? left : right;
}

static double turn_limit_radians(void) {
    return (12.0 / 180.0) * java_pi();
}

static void assign_position(ComputeSink *sink, int present, double east, double upward, double south) {
    sink->positionPresent = present;
    sink->positionEast = east;
    sink->positionUp = upward;
    sink->positionSouth = south;
}

static void assign_direction(ComputeSink *sink, double east, double upward, double south) {
    sink->directionEast = east;
    sink->directionUp = upward;
    sink->directionSouth = south;
}

static void minecraft_normalize(double east, double upward, double south, ComputeSink *sink) {
    double length = sqrt((east * east) + (upward * upward) + (south * south));
    if (length < MINECRAFT_NORMALIZE_LENGTH) {
        sink->scratchEast = 0.0;
        sink->scratchUp = 0.0;
        sink->scratchSouth = 0.0;
        return;
    }
    sink->scratchEast = east / length;
    sink->scratchUp = upward / length;
    sink->scratchSouth = south / length;
}

static int trajectory_unit(double east, double upward, double south, ComputeSink *sink) {
    double lengthSquared = (east * east) + (upward * upward) + (south * south);
    if (lengthSquared < TRAJECTORY_LENGTH_SQUARED_LIMIT) {
        return 0;
    }
    minecraft_normalize(east, upward, south, sink);
    return 1;
}

static int steering_unit(int present, double east, double upward, double south, ComputeSink *sink) {
    double length;
    double inverse;
    if (present == 0 || !value_is_finite(east) || !value_is_finite(upward) || !value_is_finite(south)) {
        return 0;
    }
    length = sqrt((east * east) + (upward * upward) + (south * south));
    if (!value_is_finite(length) || length < STEERING_LENGTH_LIMIT) {
        return 0;
    }
    inverse = 1.0 / length;
    sink->scratchEast = east * inverse;
    sink->scratchUp = upward * inverse;
    sink->scratchSouth = south * inverse;
    return 1;
}

static void unit_or_forward(int present, double east, double upward, double south, ComputeSink *sink) {
    double length;
    double inverse;
    if (present == 0 || !value_is_finite(east) || !value_is_finite(upward) || !value_is_finite(south)) {
        sink->scratchEast = 0.0;
        sink->scratchUp = 0.0;
        sink->scratchSouth = 1.0;
        return;
    }
    length = sqrt((east * east) + (upward * upward) + (south * south));
    if (!value_is_finite(length) || length < TRACK_ZERO_SPAN) {
        sink->scratchEast = 0.0;
        sink->scratchUp = 0.0;
        sink->scratchSouth = 1.0;
        return;
    }
    inverse = 1.0 / length;
    sink->scratchEast = east * inverse;
    sink->scratchUp = upward * inverse;
    sink->scratchSouth = south * inverse;
}

static void write_straight(double originEast, double originUp, double originSouth,
                           double forwardEast, double forwardUp, double forwardSouth,
                           double speed, int32_t ageTicks,
                           int targetPresent, double targetEast, double targetUp, double targetSouth,
                           ComputeSink *sink) {
    double directionEast = forwardEast;
    double directionUp = forwardUp;
    double directionSouth = forwardSouth;
    double travel;
    if (targetPresent != 0) {
        double deltaEast = targetEast - originEast;
        double deltaUp = targetUp - originUp;
        double deltaSouth = targetSouth - originSouth;
        if (trajectory_unit(deltaEast, deltaUp, deltaSouth, sink)) {
            directionEast = sink->scratchEast;
            directionUp = sink->scratchUp;
            directionSouth = sink->scratchSouth;
        }
    }
    travel = speed * (double)ageTicks;
    assign_position(sink, 1,
                    originEast + (directionEast * travel),
                    originUp + (directionUp * travel),
                    originSouth + (directionSouth * travel));
    assign_direction(sink, directionEast, directionUp, directionSouth);
}

static void write_follow_look(int originPresent, double originEast, double originUp, double originSouth,
                              double forwardEast, double forwardUp, double forwardSouth,
                              int targetPresent, double targetEast, double targetUp, double targetSouth,
                              int lookPresent, double lookEast, double lookUp, double lookSouth,
                              ComputeSink *sink) {
    double directionEast;
    double directionUp;
    double directionSouth;
    if (lookPresent != 0 && trajectory_unit(lookEast, lookUp, lookSouth, sink)) {
        directionEast = sink->scratchEast;
        directionUp = sink->scratchUp;
        directionSouth = sink->scratchSouth;
    } else {
        directionEast = forwardEast;
        directionUp = forwardUp;
        directionSouth = forwardSouth;
    }
    if (targetPresent != 0) {
        double deltaEast = targetEast - originEast;
        double deltaUp = targetUp - originUp;
        double deltaSouth = targetSouth - originSouth;
        if (trajectory_unit(deltaEast, deltaUp, deltaSouth, sink)) {
            directionEast = sink->scratchEast;
            directionUp = sink->scratchUp;
            directionSouth = sink->scratchSouth;
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
        assign_position(sink, 0, 0.0, 0.0, 0.0);
    } else {
        assign_position(sink, 1, originEast, originUp, originSouth);
    }
    assign_direction(sink, directionEast, directionUp, directionSouth);
}

static int32_t spiral_tick_count(int32_t lifetimeTicks) {
    uint32_t wrapped = (uint32_t)lifetimeTicks * 2u;
    int32_t product = (int32_t)wrapped;
    int32_t quotient = product / 3;
    if (quotient > VORTEX_SPIRAL_TICK_FLOOR) {
        return quotient;
    }
    return VORTEX_SPIRAL_TICK_FLOOR;
}

static void write_vortex(double originEast, double originUp, double originSouth,
                         double forwardEast, double forwardUp, double forwardSouth,
                         int32_t lifetimeTicks, int32_t ageTicks, ComputeSink *sink) {
    int32_t spiralTicks = spiral_tick_count(lifetimeTicks);
    double pi;
    double spiralProgress;
    double phase;
    double angle;
    double radius;
    double height;
    double spinRate;
    double shrinkRate;
    double dropRate;
    double positionEast;
    double positionUp;
    double positionSouth;
    double tangentEast;
    double tangentUp;
    double tangentSouth;
    double sine;
    double cosine;
    if (ageTicks >= spiralTicks) {
        if (trajectory_unit(forwardEast, 0.0, forwardSouth, sink)) {
            assign_direction(sink, sink->scratchEast, sink->scratchUp, sink->scratchSouth);
        } else {
            assign_direction(sink, 0.0, 0.0, 1.0);
        }
        assign_position(sink, 1, originEast, originUp, originSouth);
        return;
    }
    pi = java_pi();
    spiralProgress = (double)ageTicks / (double)spiralTicks;
    phase = atan2(forwardSouth, forwardEast);
    angle = phase + (((spiralProgress * VORTEX_TURN_COUNT) * pi) * 2.0);
    radius = VORTEX_MOUTH_RADIUS * (1.0 - spiralProgress);
    height = VORTEX_COLUMN_HEIGHT * (1.0 - spiralProgress);
    spinRate = (((VORTEX_TURN_COUNT * pi) * 2.0) / (double)spiralTicks);
    shrinkRate = (-VORTEX_MOUTH_RADIUS) / (double)spiralTicks;
    dropRate = (-VORTEX_COLUMN_HEIGHT) / (double)spiralTicks;
    cosine = cos(angle);
    sine = sin(angle);
    positionEast = originEast + (cosine * radius);
    positionUp = originUp + height;
    positionSouth = originSouth + (sine * radius);
    tangentEast = (((-sine) * radius) * spinRate) + (cosine * shrinkRate);
    tangentUp = dropRate;
    tangentSouth = ((cosine * radius) * spinRate) + (sine * shrinkRate);
    if (trajectory_unit(tangentEast, tangentUp, tangentSouth, sink)) {
        assign_direction(sink, sink->scratchEast, sink->scratchUp, sink->scratchSouth);
    } else {
        assign_direction(sink, 0.0, -1.0, 0.0);
    }
    assign_position(sink, 1, positionEast, positionUp, positionSouth);
}

static void sample_closed(int trajectoryCode,
                          int originPresent, double originEast, double originUp, double originSouth,
                          int forwardPresent, double forwardEast, double forwardUp, double forwardSouth,
                          double speed, int32_t lifetimeTicks, int32_t ageTicks,
                          int targetPresent, double targetEast, double targetUp, double targetSouth,
                          int lookPresent, double lookEast, double lookUp, double lookSouth,
                          ComputeSink *sink) {
    double forwardUnitEast;
    double forwardUnitUp;
    double forwardUnitSouth;
    double fall;
    if (originPresent == 0) {
        originEast = 0.0;
        originUp = 0.0;
        originSouth = 0.0;
    }
    if (forwardPresent == 0) {
        forwardEast = 0.0;
        forwardUp = 0.0;
        forwardSouth = 0.0;
    }
    if (targetPresent == 0) {
        targetEast = 0.0;
        targetUp = 0.0;
        targetSouth = 0.0;
    }
    if (lookPresent == 0) {
        lookEast = 0.0;
        lookUp = 0.0;
        lookSouth = 0.0;
    }
    if (forwardPresent != 0 && trajectory_unit(forwardEast, forwardUp, forwardSouth, sink)) {
        forwardUnitEast = sink->scratchEast;
        forwardUnitUp = sink->scratchUp;
        forwardUnitSouth = sink->scratchSouth;
    } else {
        forwardUnitEast = 0.0;
        forwardUnitUp = 0.0;
        forwardUnitSouth = 1.0;
    }
    if (trajectoryCode == TRAJECTORY_STRAIGHT) {
        write_straight(originEast, originUp, originSouth, forwardUnitEast, forwardUnitUp, forwardUnitSouth,
                       speed, ageTicks, targetPresent, targetEast, targetUp, targetSouth, sink);
        return;
    }
    if (trajectoryCode == TRAJECTORY_DROP) {
        fall = (DROP_LEADING * DROP_FACTOR) * (double)ageTicks;
        fall = fall * (double)ageTicks;
        assign_position(sink, 1, originEast, originUp - fall, originSouth);
        assign_direction(sink, 0.0, -1.0, 0.0);
        return;
    }
    if (trajectoryCode == TRAJECTORY_FOLLOW_LOOK) {
        write_follow_look(originPresent, originEast, originUp, originSouth,
                          forwardUnitEast, forwardUnitUp, forwardUnitSouth,
                          targetPresent, targetEast, targetUp, targetSouth,
                          lookPresent, lookEast, lookUp, lookSouth, sink);
        return;
    }
    if (trajectoryCode == TRAJECTORY_VORTEX) {
        write_vortex(originEast, originUp, originSouth, forwardUnitEast, forwardUnitUp, forwardUnitSouth,
                     lifetimeTicks, ageTicks, sink);
        return;
    }
    write_straight(originEast, originUp, originSouth, forwardUnitEast, forwardUnitUp, forwardUnitSouth,
                   speed, ageTicks, 0, 0.0, 0.0, 0.0, sink);
}

static void turn_toward(int currentPresent, double currentEast, double currentUp, double currentSouth,
                        int desiredPresent, double desiredEast, double desiredUp, double desiredSouth,
                        ComputeSink *sink) {
    int currentReady = steering_unit(currentPresent, currentEast, currentUp, currentSouth, sink);
    double currentUnitEast = currentReady ? sink->scratchEast : 0.0;
    double currentUnitUp = currentReady ? sink->scratchUp : 0.0;
    double currentUnitSouth = currentReady ? sink->scratchSouth : 0.0;
    int desiredReady = steering_unit(desiredPresent, desiredEast, desiredUp, desiredSouth, sink);
    double desiredUnitEast = desiredReady ? sink->scratchEast : 0.0;
    double desiredUnitUp = desiredReady ? sink->scratchUp : 0.0;
    double desiredUnitSouth = desiredReady ? sink->scratchSouth : 0.0;
    double dot;
    double angle;
    double limit;
    double rawTangentEast;
    double rawTangentUp;
    double rawTangentSouth;
    double tangentEast;
    double tangentUp;
    double tangentSouth;
    double cosine;
    double sine;
    double mixedEast;
    double mixedUp;
    double mixedSouth;
    if (!currentReady) {
        if (desiredReady) {
            assign_direction(sink, desiredUnitEast, desiredUnitUp, desiredUnitSouth);
        } else {
            assign_direction(sink, 0.0, 0.0, 1.0);
        }
        return;
    }
    if (!desiredReady) {
        assign_direction(sink, currentUnitEast, currentUnitUp, currentUnitSouth);
        return;
    }
    dot = (currentUnitEast * desiredUnitEast) + (currentUnitUp * desiredUnitUp) + (currentUnitSouth * desiredUnitSouth);
    dot = java_max(-1.0, java_min(1.0, dot));
    angle = acos(dot);
    limit = turn_limit_radians();
    if (angle <= limit) {
        assign_direction(sink, desiredUnitEast, desiredUnitUp, desiredUnitSouth);
        return;
    }
    rawTangentEast = desiredUnitEast - (currentUnitEast * dot);
    rawTangentUp = desiredUnitUp - (currentUnitUp * dot);
    rawTangentSouth = desiredUnitSouth - (currentUnitSouth * dot);
    if (steering_unit(1, rawTangentEast, rawTangentUp, rawTangentSouth, sink)) {
        tangentEast = sink->scratchEast;
        tangentUp = sink->scratchUp;
        tangentSouth = sink->scratchSouth;
    } else {
        double helperEast;
        double helperUp;
        double helperSouth;
        double crossEast;
        double crossUp;
        double crossSouth;
        if (java_abs(currentUnitUp) < HELPER_ABSOLUTE_UP) {
            helperEast = 0.0;
            helperUp = 1.0;
            helperSouth = 0.0;
        } else {
            helperEast = 1.0;
            helperUp = 0.0;
            helperSouth = 0.0;
        }
        crossEast = (currentUnitUp * helperSouth) - (currentUnitSouth * helperUp);
        crossUp = (currentUnitSouth * helperEast) - (currentUnitEast * helperSouth);
        crossSouth = (currentUnitEast * helperUp) - (currentUnitUp * helperEast);
        minecraft_normalize(crossEast, crossUp, crossSouth, sink);
        tangentEast = sink->scratchEast;
        tangentUp = sink->scratchUp;
        tangentSouth = sink->scratchSouth;
    }
    cosine = cos(limit);
    sine = sin(limit);
    mixedEast = (currentUnitEast * cosine) + (tangentEast * sine);
    mixedUp = (currentUnitUp * cosine) + (tangentUp * sine);
    mixedSouth = (currentUnitSouth * cosine) + (tangentSouth * sine);
    minecraft_normalize(mixedEast, mixedUp, mixedSouth, sink);
    assign_direction(sink, sink->scratchEast, sink->scratchUp, sink->scratchSouth);
}

static void tracking_step(int currentPresent, double currentEast, double currentUp, double currentSouth,
                          int targetPresent, double targetEast, double targetUp, double targetSouth,
                          double baseSpeed, ComputeSink *sink) {
    double travelSpeed = (value_is_finite(baseSpeed) && baseSpeed > 0.0) ? baseSpeed : 0.0;
    double heldEast;
    double heldUp;
    double heldSouth;
    double spanSquared;
    double distance;
    double nextEast;
    double nextUp;
    double nextSouth;
    double inverseDistance;
    double targetUnitEast;
    double targetUnitUp;
    double targetUnitSouth;
    double alignment;
    double clamped;
    double remaining;
    double stepLength;
    unit_or_forward(currentPresent, currentEast, currentUp, currentSouth, sink);
    heldEast = sink->scratchEast;
    heldUp = sink->scratchUp;
    heldSouth = sink->scratchSouth;
    if (targetPresent == 0 || !value_is_finite(targetEast) || !value_is_finite(targetUp) || !value_is_finite(targetSouth)) {
        assign_direction(sink, heldEast, heldUp, heldSouth);
        sink->stepLength = 0.0;
        return;
    }
    spanSquared = (targetEast * targetEast) + (targetUp * targetUp) + (targetSouth * targetSouth);
    if (spanSquared < TRACK_ZERO_SPAN) {
        assign_direction(sink, heldEast, heldUp, heldSouth);
        sink->stepLength = 0.0;
        return;
    }
    distance = sqrt((targetEast * targetEast) + (targetUp * targetUp) + (targetSouth * targetSouth));
    if (!value_is_finite(distance) || distance < TRACK_ZERO_SPAN) {
        assign_direction(sink, heldEast, heldUp, heldSouth);
        sink->stepLength = 0.0;
        return;
    }
    turn_toward(1, heldEast, heldUp, heldSouth, 1, targetEast, targetUp, targetSouth, sink);
    unit_or_forward(1, sink->directionEast, sink->directionUp, sink->directionSouth, sink);
    nextEast = sink->scratchEast;
    nextUp = sink->scratchUp;
    nextSouth = sink->scratchSouth;
    inverseDistance = 1.0 / distance;
    targetUnitEast = targetEast * inverseDistance;
    targetUnitUp = targetUp * inverseDistance;
    targetUnitSouth = targetSouth * inverseDistance;
    alignment = (nextEast * targetUnitEast) + (nextUp * targetUnitUp) + (nextSouth * targetUnitSouth);
    if (!value_is_finite(alignment)) {
        assign_direction(sink, nextEast, nextUp, nextSouth);
        sink->stepLength = 0.0;
        return;
    }
    clamped = alignment;
    if (clamped > 1.0) {
        clamped = 1.0;
    } else if (clamped < -1.0) {
        clamped = -1.0;
    }
    remaining = acos(clamped);
    if (!value_is_finite(remaining) || remaining >= (java_pi() * 0.5)) {
        assign_direction(sink, nextEast, nextUp, nextSouth);
        sink->stepLength = 0.0;
        return;
    }
    if (remaining <= TRACK_ALIGNED) {
        stepLength = java_min(travelSpeed, distance);
    } else {
        double turnSine = sin(turn_limit_radians());
        double speedCap = java_min(travelSpeed, (TRACK_APPROACH * distance) * turnSine);
        double cosine = cos(remaining);
        if (!value_is_finite(cosine) || cosine < 0.0) {
            cosine = 0.0;
        }
        stepLength = speedCap * cosine;
    }
    if (!value_is_finite(stepLength) || stepLength < 0.0) {
        stepLength = 0.0;
    }
    if (stepLength > travelSpeed) {
        stepLength = travelSpeed;
    }
    assign_direction(sink, nextEast, nextUp, nextSouth);
    sink->stepLength = stepLength;
}

static int corridor_overlaps(double startX, double startY, double startZ,
                             double forwardX, double forwardY, double forwardZ,
                             double length, double width, double height,
                             double lowerX, double lowerY, double lowerZ,
                             double upperX, double upperY, double upperZ) {
    double forwardLength;
    double unitX;
    double unitY;
    double unitZ;
    double hintX;
    double hintY;
    double hintZ;
    double rightX;
    double rightY;
    double rightZ;
    double rightLength;
    double upX;
    double upY;
    double upZ;
    double centerX;
    double centerY;
    double centerZ;
    double halfX;
    double halfY;
    double halfZ;
    double offsetX;
    double offsetY;
    double offsetZ;
    double along;
    double lateral;
    double vertical;
    double alongPad;
    double lateralPad;
    double verticalPad;
    int alongHit;
    int lateralHit;
    int verticalHit;
    if (!value_is_finite(length) || length <= 0.0 || width < 0.0 || height < 0.0) {
        return 0;
    }
    forwardLength = sqrt((forwardX * forwardX) + (forwardY * forwardY) + (forwardZ * forwardZ));
    if (forwardLength < CORRIDOR_AXIS_EPSILON) {
        return 0;
    }
    unitX = forwardX / forwardLength;
    unitY = forwardY / forwardLength;
    unitZ = forwardZ / forwardLength;
    hintX = java_abs(unitY) > CORRIDOR_HINT_ABSOLUTE ? 1.0 : 0.0;
    hintY = java_abs(unitY) > CORRIDOR_HINT_ABSOLUTE ? 0.0 : 1.0;
    hintZ = 0.0;
    rightX = (unitY * hintZ) - (unitZ * hintY);
    rightY = (unitZ * hintX) - (unitX * hintZ);
    rightZ = (unitX * hintY) - (unitY * hintX);
    rightLength = sqrt((rightX * rightX) + (rightY * rightY) + (rightZ * rightZ));
    if (rightLength < CORRIDOR_AXIS_EPSILON) {
        return 0;
    }
    rightX /= rightLength;
    rightY /= rightLength;
    rightZ /= rightLength;
    upX = (rightY * unitZ) - (rightZ * unitY);
    upY = (rightZ * unitX) - (rightX * unitZ);
    upZ = (rightX * unitY) - (rightY * unitX);
    centerX = (lowerX + upperX) * 0.5;
    centerY = (lowerY + upperY) * 0.5;
    centerZ = (lowerZ + upperZ) * 0.5;
    halfX = java_abs(upperX - lowerX) * 0.5;
    halfY = java_abs(upperY - lowerY) * 0.5;
    halfZ = java_abs(upperZ - lowerZ) * 0.5;
    offsetX = centerX - startX;
    offsetY = centerY - startY;
    offsetZ = centerZ - startZ;
    along = (offsetX * unitX) + (offsetY * unitY) + (offsetZ * unitZ);
    lateral = (offsetX * rightX) + (offsetY * rightY) + (offsetZ * rightZ);
    vertical = (offsetX * upX) + (offsetY * upY) + (offsetZ * upZ);
    alongPad = ((halfX * java_abs(unitX)) + (halfY * java_abs(unitY))) + (halfZ * java_abs(unitZ));
    lateralPad = ((halfX * java_abs(rightX)) + (halfY * java_abs(rightY))) + (halfZ * java_abs(rightZ));
    verticalPad = ((halfX * java_abs(upX)) + (halfY * java_abs(upY))) + (halfZ * java_abs(upZ));
    alongHit = (along + alongPad) >= 0.0 && (along - alongPad) <= length;
    lateralHit = java_abs(lateral) <= ((width * 0.5) + lateralPad);
    verticalHit = java_abs(vertical) <= ((height * 0.5) + verticalPad);
    return alongHit && lateralHit && verticalHit;
}

static double corridor_contact(double startX, double startY, double startZ,
                               double unitX, double unitY, double unitZ,
                               double centerX, double centerY, double centerZ,
                               double alongPad) {
    double offsetX = centerX - startX;
    double offsetY = centerY - startY;
    double offsetZ = centerZ - startZ;
    double along = (offsetX * unitX) + (offsetY * unitY) + (offsetZ * unitZ);
    double near = along - java_max(0.0, alongPad);
    return near < 0.0 ? 0.0 : near;
}

static int corridor_blocked(double contactDistance, double obstacleDistance) {
    if (!value_is_finite(contactDistance) || contactDistance < 0.0) {
        return 1;
    }
    if (!value_is_finite(obstacleDistance)) {
        return 0;
    }
    return (obstacleDistance + CORRIDOR_BLOCK_SLACK) < contactDistance;
}

static jint check_count(jint count) {
    if (count < 0 || count > COMPUTE_BATCH_LIMIT) {
        return STATUS_BAD_COUNT;
    }
    return STATUS_OK;
}

static jint check_span(jint count, jint stride, jsize arrayLength, int outputBuffer, int headerBuffer) {
    long long need = (long long)count * (long long)stride;
    if (need > 2147483647LL) {
        return STATUS_OVERFLOW;
    }
    if ((long long)arrayLength < need) {
        if (headerBuffer) {
            return STATUS_BAD_HEADER;
        }
        return outputBuffer ? STATUS_BAD_CAPACITY : STATUS_BAD_LENGTH;
    }
    return STATUS_OK;
}

static jint *lock_ints(JNIEnv *environment, jintArray array) {
    jint *elements = (*environment)->GetIntArrayElements(environment, array, NULL);
    if (elements == NULL) {
        return NULL;
    }
    if ((*environment)->ExceptionCheck(environment)) {
        (*environment)->ReleaseIntArrayElements(environment, array, elements, JNI_ABORT);
        return NULL;
    }
    return elements;
}

static jdouble *lock_doubles(JNIEnv *environment, jdoubleArray array) {
    jdouble *elements = (*environment)->GetDoubleArrayElements(environment, array, NULL);
    if (elements == NULL) {
        return NULL;
    }
    if ((*environment)->ExceptionCheck(environment)) {
        (*environment)->ReleaseDoubleArrayElements(environment, array, elements, JNI_ABORT);
        return NULL;
    }
    return elements;
}

static void release_ints(JNIEnv *environment, jintArray array, jint *elements, jint mode) {
    if (elements != NULL) {
        (*environment)->ReleaseIntArrayElements(environment, array, elements, mode);
    }
}

static void release_doubles(JNIEnv *environment, jdoubleArray array, jdouble *elements, jint mode) {
    if (elements != NULL) {
        (*environment)->ReleaseDoubleArrayElements(environment, array, elements, mode);
    }
}

static jint prepare_trio(JNIEnv *environment, jint count, jintArray header, jint headerStride,
                         jdoubleArray input, jint inputStride, jdoubleArray output, jint outputStride) {
    jint status = check_count(count);
    if (status != STATUS_OK || count == 0) {
        return status;
    }
    if (header == NULL || input == NULL || output == NULL) {
        return STATUS_NULL_BUFFER;
    }
    if ((*environment)->IsSameObject(environment, input, output) == JNI_TRUE) {
        return STATUS_ALIAS;
    }
    status = check_span(count, headerStride, (*environment)->GetArrayLength(environment, header), 0, 1);
    if (status != STATUS_OK) {
        return status;
    }
    status = check_span(count, inputStride, (*environment)->GetArrayLength(environment, input), 0, 0);
    if (status != STATUS_OK) {
        return status;
    }
    return check_span(count, outputStride, (*environment)->GetArrayLength(environment, output), 1, 0);
}

static jint prepare_contact(JNIEnv *environment, jint count, jdoubleArray input, jdoubleArray output) {
    jint status = check_count(count);
    if (status != STATUS_OK || count == 0) {
        return status;
    }
    if (input == NULL || output == NULL) {
        return STATUS_NULL_BUFFER;
    }
    if ((*environment)->IsSameObject(environment, input, output) == JNI_TRUE) {
        return STATUS_ALIAS;
    }
    status = check_span(count, CORRIDOR_CONTACT_INPUT_STRIDE, (*environment)->GetArrayLength(environment, input), 0, 0);
    if (status != STATUS_OK) {
        return status;
    }
    return check_span(count, CORRIDOR_CONTACT_OUTPUT_STRIDE, (*environment)->GetArrayLength(environment, output), 1, 0);
}

static jint prepare_pair(JNIEnv *environment, jint count, jdoubleArray input, jint inputStride,
                         jintArray output, jint outputStride) {
    jint status = check_count(count);
    if (status != STATUS_OK || count == 0) {
        return status;
    }
    if (input == NULL || output == NULL) {
        return STATUS_NULL_BUFFER;
    }
    status = check_span(count, inputStride, (*environment)->GetArrayLength(environment, input), 0, 0);
    if (status != STATUS_OK) {
        return status;
    }
    return check_span(count, outputStride, (*environment)->GetArrayLength(environment, output), 1, 0);
}

static void write_trajectory_items(jint count, const jint *header, const jdouble *input, jdouble *output) {
    jint item;
    for (item = 0; item < count; item++) {
        jint headerBase = item * TRAJECTORY_HEADER_STRIDE;
        jint inputBase = item * TRAJECTORY_INPUT_STRIDE;
        jint outputBase = item * TRAJECTORY_OUTPUT_STRIDE;
        ComputeSink sink;
        jint followCurrentLook = header[headerBase + 4];
        (void)followCurrentLook;
        memset(&sink, 0, sizeof sink);
        sample_closed(
                header[headerBase + 0],
                header[headerBase + 1],
                input[inputBase + 0], input[inputBase + 1], input[inputBase + 2],
                header[headerBase + 2],
                input[inputBase + 3], input[inputBase + 4], input[inputBase + 5],
                input[inputBase + 6],
                header[headerBase + 6],
                header[headerBase + 7],
                header[headerBase + 3],
                input[inputBase + 7], input[inputBase + 8], input[inputBase + 9],
                header[headerBase + 5],
                input[inputBase + 10], input[inputBase + 11], input[inputBase + 12],
                &sink);
        output[outputBase + 0] = sink.positionPresent == 0 ? 0.0 : 1.0;
        output[outputBase + 1] = sink.positionEast;
        output[outputBase + 2] = sink.positionUp;
        output[outputBase + 3] = sink.positionSouth;
        output[outputBase + 4] = sink.directionEast;
        output[outputBase + 5] = sink.directionUp;
        output[outputBase + 6] = sink.directionSouth;
    }
}

static void write_turn_items(jint count, const jint *header, const jdouble *input, jdouble *output) {
    jint item;
    for (item = 0; item < count; item++) {
        jint headerBase = item * TURN_HEADER_STRIDE;
        jint inputBase = item * TURN_INPUT_STRIDE;
        jint outputBase = item * TURN_OUTPUT_STRIDE;
        ComputeSink sink;
        memset(&sink, 0, sizeof sink);
        turn_toward(
                header[headerBase + 0],
                input[inputBase + 0], input[inputBase + 1], input[inputBase + 2],
                header[headerBase + 1],
                input[inputBase + 3], input[inputBase + 4], input[inputBase + 5],
                &sink);
        output[outputBase + 0] = sink.directionEast;
        output[outputBase + 1] = sink.directionUp;
        output[outputBase + 2] = sink.directionSouth;
    }
}

static void write_tracking_items(jint count, const jint *header, const jdouble *input, jdouble *output) {
    jint item;
    for (item = 0; item < count; item++) {
        jint headerBase = item * TRACKING_HEADER_STRIDE;
        jint inputBase = item * TRACKING_INPUT_STRIDE;
        jint outputBase = item * TRACKING_OUTPUT_STRIDE;
        ComputeSink sink;
        memset(&sink, 0, sizeof sink);
        tracking_step(
                header[headerBase + 0],
                input[inputBase + 0], input[inputBase + 1], input[inputBase + 2],
                header[headerBase + 1],
                input[inputBase + 3], input[inputBase + 4], input[inputBase + 5],
                input[inputBase + 6],
                &sink);
        output[outputBase + 0] = sink.directionEast;
        output[outputBase + 1] = sink.directionUp;
        output[outputBase + 2] = sink.directionSouth;
        output[outputBase + 3] = sink.stepLength;
    }
}

static jint finish_double_output(JNIEnv *environment, jintArray header, jint *headerElements,
                                 jdoubleArray input, jdouble *inputElements,
                                 jdoubleArray output, jdouble *outputElements) {
    if ((*environment)->ExceptionCheck(environment)) {
        release_ints(environment, header, headerElements, JNI_ABORT);
        release_doubles(environment, input, inputElements, JNI_ABORT);
        release_doubles(environment, output, outputElements, JNI_ABORT);
        return STATUS_NATIVE_FAILURE;
    }
    release_ints(environment, header, headerElements, JNI_ABORT);
    release_doubles(environment, input, inputElements, JNI_ABORT);
    release_doubles(environment, output, outputElements, 0);
    return STATUS_OK;
}

JNIEXPORT jint JNICALL
Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_handshake(
        JNIEnv *environment, jclass nativeClass, jintArray sink) {
    jint *elements;
    (void)nativeClass;
    if (sink == NULL) {
        return STATUS_NULL_BUFFER;
    }
    if ((*environment)->GetArrayLength(environment, sink) < 2) {
        return STATUS_BAD_LENGTH;
    }
    elements = lock_ints(environment, sink);
    if (elements == NULL) {
        return STATUS_NATIVE_FAILURE;
    }
    elements[0] = ABI_VERSION;
    elements[1] = CAPABILITY_ALL;
    if ((*environment)->ExceptionCheck(environment)) {
        release_ints(environment, sink, elements, JNI_ABORT);
        return STATUS_NATIVE_FAILURE;
    }
    release_ints(environment, sink, elements, 0);
    return STATUS_OK;
}

JNIEXPORT jint JNICALL
Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_trajectoryBatch(
        JNIEnv *environment, jclass nativeClass, jint count, jintArray header,
        jdoubleArray input, jdoubleArray output) {
    jint status;
    jint *headerElements;
    jdouble *inputElements;
    jdouble *outputElements;
    (void)nativeClass;
    status = prepare_trio(environment, count, header, TRAJECTORY_HEADER_STRIDE,
                          input, TRAJECTORY_INPUT_STRIDE, output, TRAJECTORY_OUTPUT_STRIDE);
    if (status != STATUS_OK || count == 0) {
        return status;
    }
    headerElements = lock_ints(environment, header);
    if (headerElements == NULL) {
        return STATUS_NATIVE_FAILURE;
    }
    inputElements = lock_doubles(environment, input);
    if (inputElements == NULL) {
        release_ints(environment, header, headerElements, JNI_ABORT);
        return STATUS_NATIVE_FAILURE;
    }
    outputElements = lock_doubles(environment, output);
    if (outputElements == NULL) {
        release_ints(environment, header, headerElements, JNI_ABORT);
        release_doubles(environment, input, inputElements, JNI_ABORT);
        return STATUS_NATIVE_FAILURE;
    }
    write_trajectory_items(count, headerElements, inputElements, outputElements);
    return finish_double_output(environment, header, headerElements, input, inputElements, output, outputElements);
}

JNIEXPORT jint JNICALL
Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_turnBatch(
        JNIEnv *environment, jclass nativeClass, jint count, jintArray header,
        jdoubleArray input, jdoubleArray output) {
    jint status;
    jint *headerElements;
    jdouble *inputElements;
    jdouble *outputElements;
    (void)nativeClass;
    status = prepare_trio(environment, count, header, TURN_HEADER_STRIDE,
                          input, TURN_INPUT_STRIDE, output, TURN_OUTPUT_STRIDE);
    if (status != STATUS_OK || count == 0) {
        return status;
    }
    headerElements = lock_ints(environment, header);
    if (headerElements == NULL) {
        return STATUS_NATIVE_FAILURE;
    }
    inputElements = lock_doubles(environment, input);
    if (inputElements == NULL) {
        release_ints(environment, header, headerElements, JNI_ABORT);
        return STATUS_NATIVE_FAILURE;
    }
    outputElements = lock_doubles(environment, output);
    if (outputElements == NULL) {
        release_ints(environment, header, headerElements, JNI_ABORT);
        release_doubles(environment, input, inputElements, JNI_ABORT);
        return STATUS_NATIVE_FAILURE;
    }
    write_turn_items(count, headerElements, inputElements, outputElements);
    return finish_double_output(environment, header, headerElements, input, inputElements, output, outputElements);
}

JNIEXPORT jint JNICALL
Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_trackingStepBatch(
        JNIEnv *environment, jclass nativeClass, jint count, jintArray header,
        jdoubleArray input, jdoubleArray output) {
    jint status;
    jint *headerElements;
    jdouble *inputElements;
    jdouble *outputElements;
    (void)nativeClass;
    status = prepare_trio(environment, count, header, TRACKING_HEADER_STRIDE,
                          input, TRACKING_INPUT_STRIDE, output, TRACKING_OUTPUT_STRIDE);
    if (status != STATUS_OK || count == 0) {
        return status;
    }
    headerElements = lock_ints(environment, header);
    if (headerElements == NULL) {
        return STATUS_NATIVE_FAILURE;
    }
    inputElements = lock_doubles(environment, input);
    if (inputElements == NULL) {
        release_ints(environment, header, headerElements, JNI_ABORT);
        return STATUS_NATIVE_FAILURE;
    }
    outputElements = lock_doubles(environment, output);
    if (outputElements == NULL) {
        release_ints(environment, header, headerElements, JNI_ABORT);
        release_doubles(environment, input, inputElements, JNI_ABORT);
        return STATUS_NATIVE_FAILURE;
    }
    write_tracking_items(count, headerElements, inputElements, outputElements);
    return finish_double_output(environment, header, headerElements, input, inputElements, output, outputElements);
}

JNIEXPORT jint JNICALL
Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_corridorOverlapsBatch(
        JNIEnv *environment, jclass nativeClass, jint count, jdoubleArray input, jintArray output) {
    jint status;
    jdouble *inputElements;
    jint *outputElements;
    jint item;
    (void)nativeClass;
    status = prepare_pair(environment, count, input, CORRIDOR_OVERLAPS_INPUT_STRIDE,
                          output, CORRIDOR_OVERLAPS_OUTPUT_STRIDE);
    if (status != STATUS_OK || count == 0) {
        return status;
    }
    inputElements = lock_doubles(environment, input);
    if (inputElements == NULL) {
        return STATUS_NATIVE_FAILURE;
    }
    outputElements = lock_ints(environment, output);
    if (outputElements == NULL) {
        release_doubles(environment, input, inputElements, JNI_ABORT);
        return STATUS_NATIVE_FAILURE;
    }
    for (item = 0; item < count; item++) {
        jint inputBase = item * CORRIDOR_OVERLAPS_INPUT_STRIDE;
        int overlap = corridor_overlaps(
                inputElements[inputBase + 0], inputElements[inputBase + 1], inputElements[inputBase + 2],
                inputElements[inputBase + 3], inputElements[inputBase + 4], inputElements[inputBase + 5],
                inputElements[inputBase + 6], inputElements[inputBase + 7], inputElements[inputBase + 8],
                inputElements[inputBase + 9], inputElements[inputBase + 10], inputElements[inputBase + 11],
                inputElements[inputBase + 12], inputElements[inputBase + 13], inputElements[inputBase + 14]);
        outputElements[item] = overlap ? 1 : 0;
    }
    if ((*environment)->ExceptionCheck(environment)) {
        release_doubles(environment, input, inputElements, JNI_ABORT);
        release_ints(environment, output, outputElements, JNI_ABORT);
        return STATUS_NATIVE_FAILURE;
    }
    release_doubles(environment, input, inputElements, JNI_ABORT);
    release_ints(environment, output, outputElements, 0);
    return STATUS_OK;
}

JNIEXPORT jint JNICALL
Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_corridorContactBatch(
        JNIEnv *environment, jclass nativeClass, jint count, jdoubleArray input, jdoubleArray output) {
    jint status;
    jdouble *inputElements;
    jdouble *outputElements;
    jint item;
    (void)nativeClass;
    status = prepare_contact(environment, count, input, output);
    if (status != STATUS_OK || count == 0) {
        return status;
    }
    inputElements = lock_doubles(environment, input);
    if (inputElements == NULL) {
        return STATUS_NATIVE_FAILURE;
    }
    outputElements = lock_doubles(environment, output);
    if (outputElements == NULL) {
        release_doubles(environment, input, inputElements, JNI_ABORT);
        return STATUS_NATIVE_FAILURE;
    }
    for (item = 0; item < count; item++) {
        jint inputBase = item * CORRIDOR_CONTACT_INPUT_STRIDE;
        outputElements[item] = corridor_contact(
                inputElements[inputBase + 0], inputElements[inputBase + 1], inputElements[inputBase + 2],
                inputElements[inputBase + 3], inputElements[inputBase + 4], inputElements[inputBase + 5],
                inputElements[inputBase + 6], inputElements[inputBase + 7], inputElements[inputBase + 8],
                inputElements[inputBase + 9]);
    }
    if ((*environment)->ExceptionCheck(environment)) {
        release_doubles(environment, input, inputElements, JNI_ABORT);
        release_doubles(environment, output, outputElements, JNI_ABORT);
        return STATUS_NATIVE_FAILURE;
    }
    release_doubles(environment, input, inputElements, JNI_ABORT);
    release_doubles(environment, output, outputElements, 0);
    return STATUS_OK;
}

JNIEXPORT jint JNICALL
Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_corridorBlockedBatch(
        JNIEnv *environment, jclass nativeClass, jint count, jdoubleArray input, jintArray output) {
    jint status;
    jdouble *inputElements;
    jint *outputElements;
    jint item;
    (void)nativeClass;
    status = prepare_pair(environment, count, input, CORRIDOR_BLOCKED_INPUT_STRIDE,
                          output, CORRIDOR_BLOCKED_OUTPUT_STRIDE);
    if (status != STATUS_OK || count == 0) {
        return status;
    }
    inputElements = lock_doubles(environment, input);
    if (inputElements == NULL) {
        return STATUS_NATIVE_FAILURE;
    }
    outputElements = lock_ints(environment, output);
    if (outputElements == NULL) {
        release_doubles(environment, input, inputElements, JNI_ABORT);
        return STATUS_NATIVE_FAILURE;
    }
    for (item = 0; item < count; item++) {
        jint inputBase = item * CORRIDOR_BLOCKED_INPUT_STRIDE;
        int blocked = corridor_blocked(inputElements[inputBase + 0], inputElements[inputBase + 1]);
        outputElements[item] = blocked ? 1 : 0;
    }
    if ((*environment)->ExceptionCheck(environment)) {
        release_doubles(environment, input, inputElements, JNI_ABORT);
        release_ints(environment, output, outputElements, JNI_ABORT);
        return STATUS_NATIVE_FAILURE;
    }
    release_doubles(environment, input, inputElements, JNI_ABORT);
    release_ints(environment, output, outputElements, 0);
    return STATUS_OK;
}
