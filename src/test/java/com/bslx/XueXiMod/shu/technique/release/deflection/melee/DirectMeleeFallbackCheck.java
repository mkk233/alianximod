package com.bslx.XueXiMod.shu.technique.release.deflection.melee;

import com.bslx.XueXiMod.shu.technique.release.deflection.melee.DirectMeleeAdmission.DirectMeleeFacts;

import java.util.UUID;

/**
 * 不启动世界。检查分类、同事件重入、空账尝试和只关闭自己的帧。
 * 买到宿主之后的扣款仍走原来的报价，这里不重写 20/15/10。
 */
public final class DirectMeleeFallbackCheck {
    private static int assertionCount;

    private DirectMeleeFallbackCheck() {
    }

    public static void main(String[] arguments) {
        try {
            classification();
            scopeNesting();
            sameEventVersusLaterSwing();
            failedAttemptBlocksFallback();
            cleanupAllowsNextAttack();
            nestedFramesCloseSeparately();
            emptyFrameDoesNotLeak();
            illegalPairCreatesNothing();
            System.out.println("直接近战补充检查通过");
            System.out.println("断言 " + assertionCount);
        } finally {
            MeleeDeflection.closeStale();
            DirectMeleeAttempts.discard();
        }
    }

    private static void classification() {
        check(DirectMeleeAdmission.admits(physical(false)), "直接 mob_attack 或 sting 可以进入");
        check(!DirectMeleeAdmission.admits(physical(true)), "射线范围内不进入");
        check(!DirectMeleeAdmission.admits(copy(physical(false), false, false, 6f, true, true, true, true, true)),
                "客户端不进入");
        check(!DirectMeleeAdmission.admits(copy(physical(false), true, true, 6f, true, true, true, true, true)),
                "已取消的事件不进入");
        check(!DirectMeleeAdmission.admits(copy(physical(false), true, false, 0f, true, true, true, true, true)),
                "零伤害不进入");
        check(!DirectMeleeAdmission.admits(copy(physical(false), true, false, -2f, true, true, true, true, true)),
                "负伤害不进入");
        check(!DirectMeleeAdmission.admits(copy(physical(false), true, false, Float.NaN, true, true, true, true, true)),
                "非数伤害不进入");
        check(!DirectMeleeAdmission.admits(copy(physical(false), true, false, Float.POSITIVE_INFINITY, true, true, true, true, true)),
                "无限伤害不进入");
        check(!DirectMeleeAdmission.admits(copy(physical(false), true, false, 6f, false, true, true, true, true)),
                "受害者不可用时不进入");
        check(!DirectMeleeAdmission.admits(copy(physical(false), true, false, 6f, true, false, true, true, true)),
                "直接实体和来源实体不同时不进入");
        check(!DirectMeleeAdmission.admits(copy(physical(false), true, false, 6f, true, true, false, true, true)),
                "玩家或非怪物不进入");
        check(!DirectMeleeAdmission.admits(copy(physical(false), true, false, 6f, true, true, true, false, true)),
                "打到自己不进入");
        check(!DirectMeleeAdmission.admits(copy(physical(false), true, false, 6f, true, true, true, true, false)),
                "player_attack、箭、魔法和环境不进入");
        check(!DirectMeleeAdmission.admits(null), "没有事实时不进入");
    }

    private static void scopeNesting() {
        Object victim = new Object();
        Object beamSource = new Object();
        Object meleeSource = new Object();
        Object otherVictim = new Object();
        check(!NonPhysicalMeleeScope.excludesIdentity(victim, beamSource), "没有标记时不排除");
        NonPhysicalMeleeScope.Token beamToken = NonPhysicalMeleeScope.openIdentity(victim, beamSource);
        check(NonPhysicalMeleeScope.excludesIdentity(victim, beamSource), "同一受害者和同一来源被排除");
        check(!NonPhysicalMeleeScope.excludesIdentity(victim, meleeSource), "同一受害者的另一份来源仍是近战");
        check(!NonPhysicalMeleeScope.excludesIdentity(otherVictim, beamSource), "另一个受害者不受这层标记影响");
        NonPhysicalMeleeScope.Token nestedToken = NonPhysicalMeleeScope.openIdentity(otherVictim, meleeSource);
        beamToken.close();
        check(!NonPhysicalMeleeScope.excludesIdentity(victim, beamSource), "先关外层不会留下外层标记");
        check(NonPhysicalMeleeScope.excludesIdentity(otherVictim, meleeSource), "先关外层不会清掉内层");
        nestedToken.close();
        check(!NonPhysicalMeleeScope.excludesIdentity(otherVictim, meleeSource), "内层关闭后恢复");
        beamToken.close();
        nestedToken.close();
        boolean cleaned = false;
        NonPhysicalMeleeScope.Token kept = NonPhysicalMeleeScope.openIdentity(victim, meleeSource);
        try {
            NonPhysicalMeleeScope.Token failing = NonPhysicalMeleeScope.openIdentity(victim, beamSource);
            try {
                throw new IllegalStateException("beam failed");
            } finally {
                failing.close();
            }
        } catch (IllegalStateException failure) {
            cleaned = !NonPhysicalMeleeScope.excludesIdentity(victim, beamSource)
                    && NonPhysicalMeleeScope.excludesIdentity(victim, meleeSource);
        } finally {
            kept.close();
        }
        check(cleaned, "异常只清掉自己的标记");
        check(!NonPhysicalMeleeScope.excludesIdentity(victim, meleeSource), "外层随后也能关掉");
    }

    private static void sameEventVersusLaterSwing() {
        Object firstSwing = new Object();
        Object laterSwing = new Object();
        DirectMeleeFacts admitted = physical(false);
        check(DirectMeleeFallback.claim(firstSwing, admitted) == DirectMeleeFallback.Gate.READY, "第一次事件可以尝试");
        check(DirectMeleeFallback.claim(firstSwing, admitted) == DirectMeleeFallback.Gate.SEEN, "同一事件重入不再尝试");
        check(DirectMeleeFallback.claim(laterSwing, admitted) == DirectMeleeFallback.Gate.READY, "下一次真实攻击是另一个事件");
        Object beamSwing = new Object();
        check(DirectMeleeFallback.claim(beamSwing, physical(true)) == DirectMeleeFallback.Gate.REJECTED,
                "射线事件被拒绝且不记成已尝试");
        check(DirectMeleeFallback.claim(beamSwing, admitted) == DirectMeleeFallback.Gate.READY,
                "范围关掉后的另一个事实仍可尝试");
    }

    private static void failedAttemptBlocksFallback() {
        UUID attackerId = new UUID(3L, 1L);
        UUID victimId = new UUID(3L, 2L);
        MeleeDeflection.AttemptToken failed = MeleeDeflection.pushAttempt(attackerId, victimId, null, 4, true);
        check(failed != null && MeleeDeflection.attemptLive(attackerId, victimId), "失败尝试仍占着这一对");
        check(!MeleeDeflection.commitToken(failed), "空账不能提交，也不能扣水");
        int[] fills = {0};
        boolean committed = MeleeDeflection.ownAttempt(attackerId, victimId, null, 0, false, token -> fills[0]++);
        check(!committed && fills[0] == 0, "无宿主、余额不足或几何拒绝后，事件不再 arm");
        check(MeleeDeflection.attemptCount() == 1, "补充入口没有关掉外层那一帧");
        MeleeDeflection.finishToken(failed);
        check(MeleeDeflection.attemptCount() == 0, "旧 close 可以清掉空帧");
    }

    private static void cleanupAllowsNextAttack() {
        UUID attackerId = new UUID(4L, 1L);
        UUID victimId = new UUID(4L, 2L);
        MeleeDeflection.AttemptToken failed = MeleeDeflection.pushAttempt(attackerId, victimId, null, 0, false);
        MeleeDeflection.finishToken(failed);
        int[] fills = {0};
        boolean committed = MeleeDeflection.ownAttempt(attackerId, victimId, null, 0, false, token -> fills[0]++);
        check(!committed && fills[0] == 1, "清掉之后下一次真实攻击可以再试");
        check(MeleeDeflection.attemptCount() == 0, "新的空帧在自己的 finally 里关闭");
    }

    private static void nestedFramesCloseSeparately() {
        UUID attackerId = new UUID(5L, 1L);
        UUID victimId = new UUID(5L, 2L);
        int[] fills = {0};
        boolean outer = MeleeDeflection.ownAttempt(attackerId, victimId, null, 0, false, token -> {
            fills[0]++;
            boolean nested = MeleeDeflection.ownAttempt(attackerId, victimId, null, 0, false, ignored -> fills[0]++);
            check(!nested, "内层看到外层尝试后不再开新帧");
            check(MeleeDeflection.attemptCount() == 1, "内层返回时外层帧还在");
        });
        check(!outer && fills[0] == 1, "外层空账没有提交");
        check(MeleeDeflection.attemptCount() == 0, "外层 finally 关掉自己的帧");

        MeleeDeflection.AttemptToken outerFrame = MeleeDeflection.pushAttempt(attackerId, victimId, null, 0, false);
        MeleeDeflection.AttemptToken innerFrame = MeleeDeflection.pushAttempt(attackerId, victimId, null, 0, false);
        MeleeDeflection.finishToken(innerFrame);
        check(MeleeDeflection.attemptCount() == 1 && MeleeDeflection.attemptLive(attackerId, victimId),
                "关掉内层不会带走外层");
        MeleeDeflection.finishToken(outerFrame);
        check(MeleeDeflection.attemptCount() == 0, "外层单独关闭");
    }

    private static void emptyFrameDoesNotLeak() {
        UUID attackerId = new UUID(6L, 1L);
        UUID victimId = new UUID(6L, 2L);
        boolean cleaned = false;
        try {
            MeleeDeflection.ownAttempt(attackerId, victimId, null, 0, false, token -> {
                throw new IllegalStateException("attach failed");
            });
        } catch (IllegalStateException failure) {
            cleaned = MeleeDeflection.attemptCount() == 0;
        }
        check(cleaned, "异常后空帧已经出栈");
        Object swing = new Object();
        DirectMeleeAttempts.mark(swing);
        MeleeDeflection.pushAttempt(attackerId, victimId, null, 0, false);
        MeleeDeflection.closeStale();
        check(MeleeDeflection.attemptCount() == 0 && !DirectMeleeAttempts.seen(swing), "收尾同时清掉空帧和事件标记");
    }

    private static void illegalPairCreatesNothing() {
        int before = MeleeDeflection.attemptCount();
        MeleeDeflection.arm(null, null, true);
        check(!MeleeDeflection.settleOwnedDirect(null, null), "没有合法双方时补充入口不取消");
        UUID same = new UUID(8L, 8L);
        check(!MeleeDeflection.ownAttempt(same, same, null, 0, false, token -> {
            throw new IllegalStateException("should not fill");
        }), "同一实体不建立尝试");
        check(MeleeDeflection.attemptCount() == before, "非法调用没有留下帧");
    }

    private static DirectMeleeFacts physical(boolean beamExcluded) {
        return new DirectMeleeFacts(true, false, 6f, true, true, true, true, true, beamExcluded);
    }

    private static DirectMeleeFacts copy(DirectMeleeFacts basis, boolean serverSide, boolean canceled, float amount,
                                         boolean victimViable, boolean sameActor, boolean mobActor,
                                         boolean distinctPair, boolean physicalKind) {
        return new DirectMeleeFacts(serverSide, canceled, amount, victimViable, sameActor, mobActor, distinctPair,
                physicalKind, basis.beamExcluded());
    }

    private static void check(boolean condition, String detail) {
        assertionCount++;
        if (!condition) {
            throw new IllegalStateException(detail);
        }
    }
}
