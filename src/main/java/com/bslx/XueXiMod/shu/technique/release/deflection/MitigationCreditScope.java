package com.bslx.XueXiMod.shu.technique.release.deflection;

import java.util.ArrayDeque;

/**
 * 同步的减伤入账抑制。按受害者和伤害来源的对象引用配对。
 * partial 为真时只抑制减伤回势。不读潮印范围，也不改溺水策略。
 * 护体记录在入队时抄走布尔，tick 末尾不再查这条线程栈。
 */
public final class MitigationCreditScope {
    private static final ThreadLocal<ArrayDeque<Frame>> FRAMES = ThreadLocal.withInitial(ArrayDeque::new);

    private MitigationCreditScope() {
    }

    /** 打开一层减伤入账抑制。资料缺失时返回空令牌。 */
    public static Token open(Object victim, Object damageSource, boolean partial) {
        if (victim == null || damageSource == null) {
            return Token.NONE;
        }
        Frame frame = new Frame(victim, damageSource, partial);
        FRAMES.get().addLast(frame);
        return new Token(frame);
    }

    /** 这对受害者和伤害来源是否正在抑制减伤回势。 */
    public static boolean suppressesMitigated(Object victim, Object damageSource) {
        if (victim == null || damageSource == null) {
            return false;
        }
        for (Frame frame : FRAMES.get()) {
            if (frame.partial && frame.victim == victim && frame.damageSource == damageSource) {
                return true;
            }
        }
        return false;
    }

    /**
     * 减伤是否入账。溺水抑制和偏转 partial 都能关掉减伤回势。
     * 实际掉血不走这个方法。
     */
    public static boolean allowsMitigatedCredit(float credit, boolean drowningSuppress, boolean deflectionSuppress) {
        return credit > 0.001f && !drowningSuppress && !deflectionSuppress;
    }

    public static final class Token implements AutoCloseable {
        public static final Token NONE = new Token(null);

        private final Frame frame;
        private boolean closed;

        private Token(Frame frame) {
            this.frame = frame;
            this.closed = frame == null;
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            this.closed = true;
            ArrayDeque<Frame> frames = FRAMES.get();
            frames.remove(this.frame);
            if (frames.isEmpty()) {
                FRAMES.remove();
            }
        }
    }

    private static final class Frame {
        private final Object victim;
        private final Object damageSource;
        private final boolean partial;

        private Frame(Object victim, Object damageSource, boolean partial) {
            this.victim = victim;
            this.damageSource = damageSource;
            this.partial = partial;
        }
    }
}
