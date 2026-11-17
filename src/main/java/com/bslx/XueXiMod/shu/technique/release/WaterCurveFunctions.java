package com.bslx.XueXiMod.shu.technique.release;

/**
 * 水系预览曲线。进度 0 到 1，位置相对释放时的起点和朝向。
 * 只注册到 {@link TrajectorySampler}，结算不按术式 id 分支。
 */
public final class WaterCurveFunctions {
    private WaterCurveFunctions() {}

    public static void register() {
        TrajectorySampler.registerFunction("water_weeping", (origin, forward, speed, progress, targetPoint) -> origin);
        WeepingCurtain.register();
    }
}
