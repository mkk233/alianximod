package com.bslx.XueXiMod.shu.technique.release.deflection;

/**
 * 报价前读取宿主身份。宿主抛异常时按缺省接受，避免一个坏实现关掉旧水体。
 */
public final class DeflectionEligibility {
    private DeflectionEligibility() {
    }

    /** 宿主是否接受这次攻击身份。宿主抛错时按缺省接受。 */
    public static boolean allows(DeflectionHost host, DeflectionAttack attack) {
        if (host == null || attack == null) {
            return false;
        }
        try {
            return host.eligible(attack);
        } catch (RuntimeException failure) {
            return true;
        }
    }
}
