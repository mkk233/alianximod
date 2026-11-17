package com.bslx.XueXiMod.shu.technique.release.prison;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 直接调用时长和伤害常量，并开关真实位移作用域。不复制公式，不构造生物。
 */
public final class PrisonQaDurationCheck {
    private static int assertionCount;
    private static int failures;

    private PrisonQaDurationCheck() {
    }

    public static void main(String[] arguments) {
        durations();
        constants();
        scopes();
        System.out.println("EXECUTED duration-constants-scope");
        System.out.println("未执行 二级耗湿、重复结束、立即伤害和到期跳过：这些要实体或服务器时刻");
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void durations() {
        Method ticksMethod = findTicks();
        if (ticksMethod == null) {
            check(false, "ADAPTATION_PENDING PrisonRuntime 没有 prisonTicks 或名字含 tick/duration 的 (int,int)->int");
            return;
        }
        System.out.println("EXECUTED " + ticksMethod.getName());
        expect(ticksMethod, 1, 0, 20);
        expect(ticksMethod, 1, 1, 20);
        expect(ticksMethod, 1, 80, 20);
        expect(ticksMethod, 2, 1, 100);
        expect(ticksMethod, 2, 1000, 100);
        expect(ticksMethod, 3, 1, 1);
        expect(ticksMethod, 3, 2, 1);
        expect(ticksMethod, 3, 5, 2);
        expectNonPositive(ticksMethod, 3, 0);
        expectNonPositive(ticksMethod, 3, -5);
        expect(ticksMethod, 4, 1, 1);
        expect(ticksMethod, 4, 80, 80);
        expectNonPositive(ticksMethod, 4, 0);
        expectNonPositive(ticksMethod, 0, 40);
    }

    private static void constants() {
        check(PrisonPressure.AMOUNT == 4.0F, "PrisonPressure.AMOUNT 不是 4，实际 " + PrisonPressure.AMOUNT);
        check(PrisonPressure.INTERVAL_TICKS == 20,
                "PrisonPressure.INTERVAL_TICKS 不是 20，实际 " + PrisonPressure.INTERVAL_TICKS);
    }

    private static void scopes() {
        try {
            try (PrisonMotionScope.Open opened = PrisonMotionScope.open(PrisonMotionScope.Kind.TELEPORT)) {
                check(PrisonMotionScope.allowsDisplacement(), "传送作用域没有放开位移");
                check(!PrisonMotionScope.defersBreak(), "普通传送不应推迟破裂");
                try (PrisonMotionScope.Open nested = PrisonMotionScope.open(PrisonMotionScope.Kind.RANDOM)) {
                    check(PrisonMotionScope.defersBreak(), "随机传送嵌套没有推迟破裂");
                }
                check(PrisonMotionScope.allowsDisplacement() && !PrisonMotionScope.defersBreak(),
                        "内层关闭后外层传送作用域丢了");
            }
            check(!PrisonMotionScope.allowsDisplacement(), "作用域关闭后仍允许位移");
            boolean probed = false;
            try {
                try (PrisonMotionScope.Open opened = PrisonMotionScope.open(PrisonMotionScope.Kind.RANDOM)) {
                    probed = true;
                    throw new IllegalStateException("scope-probe");
                }
            } catch (IllegalStateException caught) {
                check("scope-probe".equals(caught.getMessage()), "探针异常被换成了别的异常");
            }
            check(probed, "探针没有进入作用域");
            check(!PrisonMotionScope.allowsDisplacement(), "异常离开后随机作用域还在");
            check(!PrisonMotionScope.defersBreak(), "异常离开后仍推迟破裂");
            try (PrisonMotionScope.Open opened = PrisonMotionScope.open(PrisonMotionScope.Kind.CORRECTION)) {
                opened.close();
                check(!PrisonMotionScope.allowsDisplacement(), "重复关闭没有离开纠偏作用域");
            }
            check(!PrisonMotionScope.allowsDisplacement(), "纠偏关闭后再关一次把栈弄乱了");
        } finally {
            drainScopes();
        }
    }

    private static void drainScopes() {
        for (int attempt = 0; attempt < 8 && PrisonMotionScope.allowsDisplacement(); attempt++) {
            PrisonMotionScope.leave(PrisonMotionScope.Kind.RANDOM);
            PrisonMotionScope.leave(PrisonMotionScope.Kind.TELEPORT);
            PrisonMotionScope.leave(PrisonMotionScope.Kind.CORRECTION);
        }
        check(!PrisonMotionScope.allowsDisplacement(), "作用域排空后仍有帧");
    }

    private static Method findTicks() {
        Method named = null;
        Method fallback = null;
        for (Method method : PrisonRuntime.class.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != int.class) {
                continue;
            }
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length != 2 || parameters[0] != int.class || parameters[1] != int.class) {
                continue;
            }
            String called = method.getName().toLowerCase();
            if ("prisonticks".equals(called)) {
                named = method;
            } else if (fallback == null && (called.contains("tick") || called.contains("duration"))) {
                fallback = method;
            }
        }
        Method chosen = named != null ? named : fallback;
        if (chosen != null) {
            chosen.setAccessible(true);
        }
        return chosen;
    }

    private static void expect(Method ticksMethod, int tierLevel, int remainingTicks, int expectedTicks) {
        int actualTicks = invokeTicks(ticksMethod, tierLevel, remainingTicks);
        check(actualTicks == expectedTicks, "tier " + tierLevel + " remaining " + remainingTicks
                + " 得到 " + actualTicks + " 期望 " + expectedTicks);
    }

    private static void expectNonPositive(Method ticksMethod, int tierLevel, int remainingTicks) {
        int actualTicks = invokeTicks(ticksMethod, tierLevel, remainingTicks);
        check(actualTicks <= 0, "tier " + tierLevel + " remaining " + remainingTicks
                + " 仍能成牢，得到 " + actualTicks);
    }

    private static int invokeTicks(Method ticksMethod, int tierLevel, int remainingTicks) {
        try {
            Object returned = ticksMethod.invoke(null, tierLevel, remainingTicks);
            if (returned instanceof Integer boxed) {
                return boxed;
            }
            check(false, "时长返回不是整数");
            return Integer.MIN_VALUE;
        } catch (InvocationTargetException failure) {
            check(false, "时长调用抛出 " + failure.getCause());
            return Integer.MIN_VALUE;
        } catch (ReflectiveOperationException failure) {
            check(false, "时长反射失败 " + failure.getClass().getSimpleName());
            return Integer.MIN_VALUE;
        }
    }

    private static void check(boolean passed, String message) {
        assertionCount++;
        if (!passed) {
            failures++;
            System.out.println("失败 " + message);
        }
    }
}
