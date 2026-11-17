package com.bslx.XueXiMod.shu.technique.release.bind;

import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 一次发布尝试。松散复制按计划份记账，宿主组只占作用域，不把暂存发布当成松散成功。
 * 自动轮仍在关闭时按原接口保留；手动松散份在真实发布当下降下份额。账本由调用方注册，本类不引用网络包。
 */
public final class PublicationAttempt {
    public enum Kind {
        LOOSE_COPY,
        HOST_GROUP
    }

    /** 自动轮实现。负责按已分解的价格保留费用，并维护在途次数。 */
    public interface Ledger {
        boolean openingAllowed(UUID roundId);

        void attemptOpened(UUID roundId);

        void retainLoose(UUID roundId, int memberIndex, int copyIndex);

        void retainHost(UUID roundId, int memberIndex);

        void attemptFinished(UUID roundId);
    }

    private static final ThreadLocal<ArrayDeque<PublicationAttempt>> STACK = new ThreadLocal<>();
    private static final Map<UUID, Ledger> LEDGERS = new HashMap<>();

    private final Kind attemptKind;
    private final UUID boundRoundId;
    private final int boundMemberIndex;
    private final int boundCopyIndex;
    private boolean accepted;
    private boolean hostCommitted;
    private boolean receiptApplied;
    private boolean closed;

    private PublicationAttempt(Kind attemptKind, UUID boundRoundId, int boundMemberIndex, int boundCopyIndex) {
        this.attemptKind = attemptKind;
        this.boundRoundId = boundRoundId;
        this.boundMemberIndex = boundMemberIndex;
        this.boundCopyIndex = boundCopyIndex;
    }

    /** 调用方把这一轮的账本挂上。空标识或空账本会拒绝。 */
    public static void bindLedger(UUID roundId, Ledger ledger) {
        if (roundId == null || ledger == null) {
            throw new IllegalArgumentException("发布回执账本不能为空");
        }
        LEDGERS.put(roundId, ledger);
    }

    /** 这一轮结束后摘掉账本。 */
    public static void unbindLedger(UUID roundId) {
        if (roundId != null) {
            LEDGERS.remove(roundId);
        }
    }

    /** 当前线程栈顶的发布尝试。没有时为空。 */
    public static @Nullable PublicationAttempt current() {
        ArrayDeque<PublicationAttempt> stack = STACK.get();
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.peek();
    }

    /** 打开一份松散复制的发布尝试。标识或下标不合法、账本不许时为空。 */
    public static @Nullable PublicationAttempt openLoose(UUID roundId, int memberIndex, int copyIndex) {
        if (roundId == null || memberIndex < 0 || copyIndex < 0) {
            return null;
        }
        return open(new PublicationAttempt(Kind.LOOSE_COPY, roundId, memberIndex, copyIndex));
    }

    /** 打开宿主组的发布尝试。复制下标不参与。 */
    public static @Nullable PublicationAttempt openHostGroup(UUID roundId, int memberIndex) {
        if (roundId == null || memberIndex < 0) {
            return null;
        }
        return open(new PublicationAttempt(Kind.HOST_GROUP, roundId, memberIndex, -1));
    }

    /** 只有未关闭的宿主组尝试会记下已经提交。 */
    public void noteHostCommitted(boolean committed) {
        if (closed || attemptKind != Kind.HOST_GROUP) {
            return;
        }
        if (committed) {
            hostCommitted = true;
        }
    }

    /**
     * 手动松散账本上，一份已经进世界的复制立刻留下份额。
     * 自动轮仍只在关闭时保留，这里不改它的结账时刻。
     */
    public static boolean reportPlacedCopy(int copyIndex) {
        PublicationAttempt attempt = current();
        if (attempt == null || attempt.closed || !attempt.isTop() || copyIndex < 0) {
            return false;
        }
        if (attempt.attemptKind != Kind.LOOSE_COPY) {
            return false;
        }
        ReleaseExecutionContext currentContext = ReleaseExecutionContext.current();
        if (currentContext != null && currentContext.mode() == ReleaseInputMode.DOMAIN_AUTOMATIC) {
            return false;
        }
        if (copyIndex == attempt.boundCopyIndex) {
            return attempt.reportAccepted();
        }
        Ledger ledger = LEDGERS.get(attempt.boundRoundId);
        if (ledger == null) {
            return false;
        }
        ledger.retainLoose(attempt.boundRoundId, attempt.boundMemberIndex, copyIndex);
        return true;
    }

    /** 宿主组已经封印成功时，在监听回调之前留下整份份额。重复调用不重复扣。 */
    public static void keepOpenHostShare() {
        PublicationAttempt attempt = current();
        if (attempt == null || attempt.closed || !attempt.isTop() || attempt.attemptKind != Kind.HOST_GROUP) {
            return;
        }
        ReleaseExecutionContext currentContext = ReleaseExecutionContext.current();
        if (currentContext != null && currentContext.mode() == ReleaseInputMode.DOMAIN_AUTOMATIC
                && !attempt.sameAutomaticRound(currentContext)) {
            return;
        }
        attempt.noteHostCommitted(true);
        attempt.applyHostReceipt();
    }

    /** 栈顶、账本允许，且自动轮令牌仍有效时才可发布。 */
    public boolean allowsPublication() {
        if (closed || !isTop()) {
            return false;
        }
        ReleaseExecutionContext currentContext = ReleaseExecutionContext.current();
        if (sameAutomaticRound(currentContext)) {
            Ledger ledger = LEDGERS.get(boundRoundId);
            if (ledger == null || !ledger.openingAllowed(boundRoundId)) {
                return false;
            }
            LifetimeToken token = currentContext.token();
            ServerPlayer owner = currentContext.owner();
            int nowTick = owner == null || owner.getServer() == null ? 0 : owner.getServer().getTickCount();
            return token != null && token.valid(nowTick);
        }
        if (currentContext != null && currentContext.mode() == ReleaseInputMode.DOMAIN_AUTOMATIC) {
            return false;
        }
        Ledger ledger = LEDGERS.get(boundRoundId);
        return ledger != null && ledger.openingAllowed(boundRoundId);
    }

    /** 松散份接受后留下份额。同一自动轮只先记账，关闭时再保留。 */
    public boolean reportAccepted() {
        if (attemptKind != Kind.LOOSE_COPY) {
            return false;
        }
        if (accepted) {
            return true;
        }
        if (closed || !isTop()) {
            return false;
        }
        ReleaseExecutionContext currentContext = ReleaseExecutionContext.current();
        if (sameAutomaticRound(currentContext)) {
            if (LEDGERS.get(boundRoundId) == null) {
                return false;
            }
            accepted = true;
            return true;
        }
        if (currentContext != null && currentContext.mode() == ReleaseInputMode.DOMAIN_AUTOMATIC) {
            return false;
        }
        Ledger ledger = LEDGERS.get(boundRoundId);
        if (ledger == null) {
            return false;
        }
        accepted = true;
        ledger.retainLoose(boundRoundId, boundMemberIndex, boundCopyIndex);
        return true;
    }

    /** 必须从栈顶关闭。还没落账时按种类保留份额，并通知账本尝试结束。 */
    public void close() {
        if (closed) {
            return;
        }
        ArrayDeque<PublicationAttempt> stack = STACK.get();
        if (stack == null || stack.peek() != this) {
            throw new IllegalStateException("发布尝试必须先关闭栈顶");
        }
        try {
            if (!receiptApplied) {
                receiptApplied = true;
                Ledger ledger = LEDGERS.get(boundRoundId);
                if (ledger != null && attemptKind == Kind.LOOSE_COPY && accepted) {
                    ledger.retainLoose(boundRoundId, boundMemberIndex, boundCopyIndex);
                } else if (ledger != null && attemptKind == Kind.HOST_GROUP && hostCommitted) {
                    ledger.retainHost(boundRoundId, boundMemberIndex);
                }
            }
        } finally {
            closed = true;
            stack.pop();
            if (stack.isEmpty()) {
                STACK.remove();
            }
            Ledger ledger = LEDGERS.get(boundRoundId);
            if (ledger != null) {
                ledger.attemptFinished(boundRoundId);
            }
        }
    }

    public UUID roundId() {
        return boundRoundId;
    }

    public int memberIndex() {
        return boundMemberIndex;
    }

    public int copyIndex() {
        return boundCopyIndex;
    }

    public Kind kind() {
        return attemptKind;
    }

    public boolean accepted() {
        return accepted;
    }

    private static @Nullable PublicationAttempt open(PublicationAttempt attempt) {
        Ledger ledger = LEDGERS.get(attempt.boundRoundId);
        if (ledger == null || !ledger.openingAllowed(attempt.boundRoundId)) {
            return null;
        }
        ArrayDeque<PublicationAttempt> stack = STACK.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            STACK.set(stack);
        }
        stack.push(attempt);
        ledger.attemptOpened(attempt.boundRoundId);
        return attempt;
    }

    private void applyHostReceipt() {
        if (closed || receiptApplied || attemptKind != Kind.HOST_GROUP || !hostCommitted) {
            return;
        }
        Ledger ledger = LEDGERS.get(boundRoundId);
        if (ledger == null) {
            return;
        }
        receiptApplied = true;
        ledger.retainHost(boundRoundId, boundMemberIndex);
    }

    private boolean sameAutomaticRound(@Nullable ReleaseExecutionContext currentContext) {
        return currentContext != null
                && currentContext.mode() == ReleaseInputMode.DOMAIN_AUTOMATIC
                && boundRoundId.equals(currentContext.roundId());
    }

    private boolean isTop() {
        ArrayDeque<PublicationAttempt> stack = STACK.get();
        return stack != null && stack.peek() == this;
    }
}
