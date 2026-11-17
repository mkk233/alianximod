package com.bslx.XueXiMod.client.action.skinned.attachments;

import java.util.ArrayDeque;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.client.action.mesh.SkinDiagnostic;

/** 每支箭或蜂刺一层选件。改选择只动栈顶，关闭只弹出本次。 */
final class StuckMarks {
    private static final ThreadLocal<ArrayDeque<Mark>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private StuckMarks() {
    }

    static Mark push() {
        Mark mark = new Mark();
        STACK.get().addLast(mark);
        return mark;
    }

    static void pop(Mark mark) {
        ArrayDeque<Mark> stack = STACK.get();
        if (stack.isEmpty() || stack.peekLast() != mark) {
            if (!stack.contains(mark)) {
                return;
            }
            throw new SkinDiagnostic("stuck-order", "插体标记关闭顺序和打开顺序不一致");
        }
        stack.removeLast();
    }

    static void clearTop() {
        Mark mark = top();
        if (mark != null) {
            mark.clearSelection();
        }
    }

    static void remember(ArmorRegion region, OpenMatrix4f partRest) {
        Mark mark = top();
        if (mark == null) {
            return;
        }
        mark.region = region;
        mark.partRest = partRest;
        mark.chosen = region != null && partRest != null;
        mark.failure = null;
    }

    static void fail(SkinDiagnostic diagnostic) {
        Mark mark = top();
        if (mark == null) {
            throw diagnostic == null ? new SkinDiagnostic("stuck", "插体标记缺失") : diagnostic;
        }
        mark.clearSelection();
        mark.failure = diagnostic;
    }

    static boolean handled() {
        Mark mark = top();
        return mark != null && mark.chosen;
    }

    static SkinDiagnostic pendingFailure() {
        Mark mark = top();
        return mark == null ? null : mark.failure;
    }

    static ArmorRegion region() {
        Mark mark = top();
        return mark == null ? null : mark.region;
    }

    static OpenMatrix4f partRest() {
        Mark mark = top();
        return mark == null || mark.partRest == null ? new OpenMatrix4f() : mark.partRest;
    }

    static int depth() {
        return STACK.get().size();
    }

    static void discardThread() {
        STACK.get().clear();
    }

    private static Mark top() {
        ArrayDeque<Mark> stack = STACK.get();
        return stack.isEmpty() ? null : stack.peekLast();
    }

    static final class Mark {
        private ArmorRegion region;
        private OpenMatrix4f partRest;
        private boolean chosen;
        private SkinDiagnostic failure;

        private void clearSelection() {
            this.region = null;
            this.partRest = null;
            this.chosen = false;
            this.failure = null;
        }
    }
}
