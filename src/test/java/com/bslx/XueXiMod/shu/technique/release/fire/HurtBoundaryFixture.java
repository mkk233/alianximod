package com.bslx.XueXiMod.shu.technique.release.fire;

/** 未变换的布尔 hurt。检查会在字节码上套同一层边界后再加载。 */
public class HurtBoundaryFixture {
    public static volatile HurtBoundaryHook hook = (self, source, amount) -> {
    };
    public static int business;

    public boolean hurt(Object source, float amount) throws Throwable {
        if (amount < 0.0f) {
            return false;
        }
        business += 1;
        if (amount > 100.0f) {
            throw new IllegalStateException("direct");
        }
        hook.accept(this, source, amount);
        return true;
    }
}
