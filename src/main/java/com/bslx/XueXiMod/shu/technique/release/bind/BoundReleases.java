package com.bslx.XueXiMod.shu.technique.release.bind;

/** 自动轮执行桥。安装之前调用会失败，不在领域包里放第二套执行器。 */
public final class BoundReleases {
    public interface Executor {
        RoundHandle executeAutomatic(BoundReleasePlan plan, ReleaseExecutionContext context, LifetimeToken token);
    }

    private static Executor installedExecutor;

    private BoundReleases() {
    }

    /** 安装自动轮执行器。没安装时执行会失败。 */
    public static void install(Executor executor) {
        if (executor == null) {
            throw new IllegalArgumentException("自动释放执行器不能为空");
        }
        installedExecutor = executor;
    }

    /** 交给已安装的执行器。这里不另写一套释放。 */
    public static RoundHandle executeAutomatic(BoundReleasePlan plan, ReleaseExecutionContext context, LifetimeToken token) {
        Executor executor = installedExecutor;
        if (executor == null) {
            throw new IllegalStateException("自动释放执行器还没有安装");
        }
        return executor.executeAutomatic(plan, context, token);
    }
}
