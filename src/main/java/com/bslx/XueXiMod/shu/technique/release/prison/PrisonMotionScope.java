package com.bslx.XueXiMod.shu.technique.release.prison;

import java.util.ArrayDeque;

/**
 * 嵌套的位移许可。空栈表示普通写入必须留在锚点。
 * 纠偏和 random 外层只放行试探，不在内层传送返回时破裂。
 */
public final class PrisonMotionScope {
    public enum Kind {
        /** 纠偏。放行位移，但延迟破裂判断。 */
        CORRECTION,
        /** 真正传送。返回后可按实际位移破牢。 */
        TELEPORT,
        /** 随机传送外层。放行试探，不在内层返回时破牢。 */
        RANDOM
    }

    private static final ThreadLocal<ArrayDeque<Kind>> FRAMES = ThreadLocal.withInitial(ArrayDeque::new);

    private PrisonMotionScope() {
    }

    public static final class Open implements AutoCloseable {
        private final Kind kind;
        private boolean closed;

        private Open(Kind kind) {
            this.kind = kind;
            FRAMES.get().addLast(kind);
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            this.closed = true;
            leave(this.kind);
        }
    }

    /** 压入一层位移许可。关闭时按同一种类退出。 */
    public static Open open(Kind kind) {
        return new Open(kind);
    }

    /** 直接压入一层。调用方必须配对离开。 */
    public static void enter(Kind kind) {
        FRAMES.get().addLast(kind);
    }

    /** 只退出栈顶同类的那一层。 */
    public static void leave(Kind kind) {
        ArrayDeque<Kind> frames = FRAMES.get();
        if (!frames.isEmpty() && frames.peekLast() == kind) {
            frames.removeLast();
        }
        if (frames.isEmpty()) {
            FRAMES.remove();
        }
    }

    /** 当前线程是否允许离开锚点。 */
    public static boolean allowsDisplacement() {
        return !FRAMES.get().isEmpty();
    }

    /** 纠偏或随机外层仍在时，传送返回不立刻破牢。 */
    public static boolean defersBreak() {
        for (Kind kind : FRAMES.get()) {
            if (kind == Kind.CORRECTION || kind == Kind.RANDOM) {
                return true;
            }
        }
        return false;
    }
}
