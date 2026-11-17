package com.bslx.XueXiMod.shu.technique.release.deflection.projectile;

/**
 * 箭捕获标记的普通实例桥。实现合并在箭上，调用方不引用 Mixin 类。
 */
public interface ArrowCaptureAccess {
    /** 设置箭是否正被捕获路线带着走。 */
    void xuexiSetCaptured(boolean captured);

    /** 箭当前是否处于捕获。 */
    boolean xuexiIsCaptured();
}
