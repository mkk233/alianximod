package com.bslx.XueXiMod.shu.technique.release.fire;

/** 变换后的夹具在原业务点回调。测试替换这个钩子。 */
public interface HurtBoundaryHook {
    void accept(Object self, Object source, float amount) throws Throwable;
}
