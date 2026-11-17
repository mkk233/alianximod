package com.bslx.XueXiMod.shu.technique.release.deflection.hosts;

import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierBody;

/**
 * 屏障偏转宿主的到期判断。直接调用宿主和墙实体的生产方法，不造世界。
 */
public final class WallHostExpiryCheck {
    private static int assertionCount;
    private static int failures;

    private WallHostExpiryCheck() {
    }

    public static void main(String[] arguments) {
        finiteLife();
        wrappedLife();
        nonPositiveLife();
        System.out.println("未执行 宿主登记、主人离场和方块挡路：这些要服务器");
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void finiteLife() {
        int started = 1_000;
        int expiry = ReleaseInput.expiryTick(started, 80);
        check(!WallDeflectionHost.lifeEnded(started, expiry), "刚放墙宿主就到期");
        check(!WallDeflectionHost.lifeEnded(expiry - 1, expiry), "到期前一拍宿主已死");
        check(WallDeflectionHost.lifeEnded(expiry, expiry), "到期刻宿主还活着");
        check(sameAsWall(started, expiry), "刚放下宿主和墙实体不一致");
        check(sameAsWall(expiry - 1, expiry), "到期前宿主和墙实体不一致");
        check(sameAsWall(expiry, expiry), "到期刻宿主和墙实体不一致");
    }

    private static void wrappedLife() {
        int started = Integer.MAX_VALUE - 10;
        int expiry = ReleaseInput.expiryTick(started, 40);
        check(expiry == started + 40, "环绕到期没有按 int 加法");
        check(started >= expiry, "这组刻没有环绕，看不出绝对比较的误判");
        check(!WallDeflectionHost.lifeEnded(started, expiry), "环绕后宿主会立刻到期");
        check(WallDeflectionHost.lifeEnded(expiry, expiry), "环绕到期刻宿主没有结束");
        check(sameAsWall(started, expiry), "环绕处宿主和墙实体不一致");
        check(sameAsWall(expiry, expiry), "环绕到期刻宿主和墙实体不一致");
    }

    private static void nonPositiveLife() {
        check(WallDeflectionHost.lifeEnded(100, 0), "零到期刻没有结束");
        check(WallDeflectionHost.lifeEnded(100, -5), "负到期刻没有结束");
        check(sameAsWall(100, 0), "零到期刻宿主和墙实体不一致");
        check(sameAsWall(100, -5), "负到期刻宿主和墙实体不一致");
        check(WallDeflectionHost.lifeEnded(80, 80), "非正寿命落到当前刻时没有结束");
        check(sameAsWall(80, 80), "当前刻到期和墙实体不一致");
    }

    private static boolean sameAsWall(int nowTick, int expiryTick) {
        return WallDeflectionHost.lifeEnded(nowTick, expiryTick) == !BarrierBody.withinLife(nowTick, expiryTick);
    }

    private static void check(boolean passed, String detail) {
        assertionCount += 1;
        if (!passed) {
            failures += 1;
            System.out.println("FAIL " + detail);
        }
    }
}
