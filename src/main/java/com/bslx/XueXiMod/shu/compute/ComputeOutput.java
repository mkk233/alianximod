package com.bslx.XueXiMod.shu.compute;

/**
 * 同一条线程上复用的标量出口。下一次纯计算调用会覆盖它。
 * 调用方必须在返回后立刻拷贝需要留下的数字。
 */
final class ComputeOutput {
    double positionEast;
    double positionUp;
    double positionSouth;
    int positionPresent;
    double directionEast;
    double directionUp;
    double directionSouth;
    double stepLength;
    int ignoredFollow;
    double scratchEast;
    double scratchUp;
    double scratchSouth;

    private static final ThreadLocal<ComputeOutput> LOCAL = ThreadLocal.withInitial(ComputeOutput::new);

    private ComputeOutput() {
    }

    static ComputeOutput current() {
        return LOCAL.get();
    }
}
