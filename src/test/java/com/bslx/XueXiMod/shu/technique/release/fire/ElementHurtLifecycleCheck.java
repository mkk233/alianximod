package com.bslx.XueXiMod.shu.technique.release.fire;

/** 调用令牌的正常、早退和异常生命周期。不织入 Mixin，也不启动游戏。 */
public final class ElementHurtLifecycleCheck {
    private static int assertionCount;
    private static int failures;

    private ElementHurtLifecycleCheck() {
    }

    public static void main(String[] arguments) {
        section("正常早退", ElementHurtLifecycleCheck::normalAndEarly);
        section("向外传播", ElementHurtLifecycleCheck::propagates);
        section("回调捕获", ElementHurtLifecycleCheck::caughtInside);
        section("进入前抛出", ElementHurtLifecycleCheck::beforeEnter);
        section("非运行时异常", ElementHurtLifecycleCheck::checkedExit);
        section("残留边界", ElementHurtLifecycleCheck::restoreBoundary);
        check(ElementDamageScope.currentHurt() == null, "全部结束后没有残留");
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        System.out.println("未执行：Mixin 织入、真实 hurt。没有启动游戏。");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void normalAndEarly() throws Throwable {
        Object target = new Object();
        Object source = new Object();
        ElementDamageScope.ElementArm arm = ElementDamageScope.armFire(
                java.util.UUID.randomUUID(), 2.0d, false);
        boolean finished = guard(target, source, () -> {
            ElementDamageScope.HurtCall call = ElementDamageScope.currentHurt();
            check(call != null && call.target() == target && call.source() == source && call.frame().fire(),
                    "正常调用领到本次火帧");
            return true;
        });
        ElementDamageScope.disarm(arm);
        check(finished && ElementDamageScope.currentHurt() == null, "正常返回后栈空");
        boolean early = guard(target, source, () -> false);
        check(!early && ElementDamageScope.currentHurt() == null, "早退也结束本次令牌");
    }

    private static void propagates() throws Throwable {
        Object baseTarget = new Object();
        Object baseSource = new Object();
        ElementDamageScope.HurtCall base = ElementDamageScope.enterHurt(baseTarget, baseSource);
        long identity = base.identity();
        try {
            try {
                guard(new Object(), new Object(), () -> {
                    guard(new Object(), new Object(), () -> {
                        throw new IllegalStateException("nested");
                    });
                    return true;
                });
                check(false, "嵌套运行时异常应传出");
            } catch (IllegalStateException failure) {
                check(ElementDamageScope.currentHurt() == base && base.identity() == identity,
                        "多层异常退出后回到进入前的外层");
            }
        } finally {
            ElementDamageScope.exitHurt(base);
        }
    }

    private static void caughtInside() throws Throwable {
        Object outerTarget = new Object();
        Object outerSource = new Object();
        ElementDamageScope.ElementArm arm = ElementDamageScope.armWater();
        boolean finished = guard(outerTarget, outerSource, () -> {
            ElementDamageScope.HurtCall outer = ElementDamageScope.currentHurt();
            long identity = outer.identity();
            try {
                throw new IllegalStateException("unwrapped");
            } catch (IllegalStateException failure) {
                check(ElementDamageScope.currentHurt() == outer && outer.identity() == identity
                                && outer.frame().water(),
                        "未包装的内层异常被捕获后外层帧还在");
            }
            try {
                guard(new Object(), new Object(), () -> {
                    throw new IllegalStateException("wrapped-inner");
                });
                check(false, "包装过的内层异常应传给回调");
            } catch (IllegalStateException failure) {
                check(ElementDamageScope.currentHurt() == outer && outer.identity() == identity,
                        "内层令牌退出后外层身份不变");
            }
            return true;
        });
        ElementDamageScope.disarm(arm);
        check(finished && ElementDamageScope.currentHurt() == null, "外层结束后没有残留");
    }

    private static void beforeEnter() {
        Object outerTarget = new Object();
        Object outerSource = new Object();
        ElementDamageScope.HurtCall outer = ElementDamageScope.enterHurt(outerTarget, outerSource);
        long identity = outer.identity();
        ElementDamageScope.Boundary boundary = ElementDamageScope.mark();
        try {
            throw new IllegalStateException("before super");
        } catch (RuntimeException failure) {
            check(failure.getMessage().equals("before super"), "进入前的异常被调用方接住");
        } finally {
            ElementDamageScope.restore(boundary);
        }
        check(ElementDamageScope.currentHurt() == outer && outer.identity() == identity,
                "super 之前抛出不会弹掉外层");
        ElementDamageScope.HurtCall spent = ElementDamageScope.enterHurt(new Object(), new Object());
        ElementDamageScope.exitHurt(spent);
        ElementDamageScope.exitHurt(spent);
        check(ElementDamageScope.currentHurt() == outer, "已结束的令牌不会再弹外层");
        ElementDamageScope.exitHurt(outer);
    }

    private static void checkedExit() throws Throwable {
        ElementDamageScope.HurtCall base = ElementDamageScope.enterHurt(new Object(), new Object());
        long identity = base.identity();
        try {
            try {
                guard(new Object(), new Object(), () -> {
                    throw new CheckedSignal();
                });
                check(false, "Throwable 应传播");
            } catch (CheckedSignal signal) {
                check(signal.getMessage().equals("checked") && ElementDamageScope.currentHurt() == base
                                && base.identity() == identity,
                        "非 RuntimeException 退出后外层还在且内层不残留");
            }
        } finally {
            ElementDamageScope.exitHurt(base);
        }
        check(ElementDamageScope.currentHurt() == null, "Throwable 路径全部退出后栈空");
    }

    private static void restoreBoundary() {
        ElementDamageScope.HurtCall outer = ElementDamageScope.enterHurt(new Object(), new Object());
        long identity = outer.identity();
        ElementDamageScope.Boundary boundary = ElementDamageScope.mark();
        ElementDamageScope.enterHurt(new Object(), new Object());
        ElementDamageScope.restore(boundary);
        check(ElementDamageScope.currentHurt() == outer && outer.identity() == identity,
                "恢复边界清掉本次遗留并保留外层");
        ElementDamageScope.HurtCall leaked = ElementDamageScope.enterHurt(new Object(), new Object());
        ElementDamageScope.exitHurt(outer);
        check(ElementDamageScope.currentHurt() == null, "结束外层时一并清掉还压在上面的遗留");
        check(leaked != outer, "遗留调用不是外层令牌");
    }

    private static boolean guard(Object target, Object source, Body body) throws Throwable {
        ElementDamageScope.HurtCall call = ElementDamageScope.enterHurt(target, source);
        try {
            return body.run();
        } finally {
            ElementDamageScope.exitHurt(call);
        }
    }

    private static void section(String title, Section body) {
        int before = failures;
        try {
            body.run();
        } catch (Throwable failure) {
            failures += 1;
            assertionCount += 1;
            System.out.println("失败 " + title + " 抛出 " + failure);
        }
        System.out.println(title + (failures == before ? " 通过" : " 失败"));
    }

    @FunctionalInterface
    private interface Body {
        boolean run() throws Throwable;
    }

    @FunctionalInterface
    private interface Section {
        void run() throws Throwable;
    }

    private static final class CheckedSignal extends Throwable {
        private CheckedSignal() {
            super("checked");
        }
    }

    private static void check(boolean condition, String label) {
        assertionCount += 1;
        if (!condition) {
            failures += 1;
            System.out.println("失败 " + label);
        }
    }
}
