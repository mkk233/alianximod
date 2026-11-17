package com.bslx.XueXiMod.shu.technique.release.fire;

import java.util.ArrayDeque;
import java.util.UUID;

/**
 * 元素帧只属于把它领走的那一次 hurt。
 * 未标记的嵌套 hurt 领不到外层帧；显式中性会领走自己的空帧，避免误用更早的水或冰。
 */
public final class ElementDamageScope {
    private static final ThreadLocal<ArrayDeque<ElementArm>> WAITING = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<ArrayDeque<HurtCall>> ACTIVE = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<long[]> IDENTITIES = ThreadLocal.withInitial(() -> new long[1]);

    private ElementDamageScope() {
    }

    public static ElementArm armFire(UUID casterId, double fireBase, boolean furnaceSettled) {
        return arm(Frame.fireSource(casterId, fireBase, furnaceSettled), false);
    }

    public static ElementArm armWater() {
        return arm(Frame.waterSource(), false);
    }

    public static ElementArm armIce() {
        return arm(Frame.iceSource(), false);
    }

    public static ElementArm armWaterIce() {
        return arm(Frame.waterIceSource(), false);
    }

    public static ElementArm armNeutral() {
        return arm(null, true);
    }

    public static void disarm(ElementArm arm) {
        if (arm == null || arm.claimed) {
            return;
        }
        WAITING.get().remove(arm);
    }

    public static Boundary mark() {
        return new Boundary(ACTIVE.get().size());
    }

    public static void restore(Boundary boundary) {
        if (boundary == null) {
            return;
        }
        ArrayDeque<HurtCall> stack = ACTIVE.get();
        for (int guard = stack.size(); guard > boundary.depth; guard -= 1) {
            if (stack.isEmpty()) {
                return;
            }
            stack.removeFirst();
        }
    }

    public static HurtCall enterHurt(Object target, Object source) {
        ElementArm arm = WAITING.get().pollFirst();
        Frame claimed = null;
        if (arm != null) {
            arm.claimed = true;
            if (!arm.neutral) {
                claimed = arm.frame;
            }
        }
        long[] tally = IDENTITIES.get();
        tally[0] += 1L;
        HurtCall call = new HurtCall(tally[0], target, source, claimed);
        ACTIVE.get().addFirst(call);
        return call;
    }

    public static void exitHurt(HurtCall call) {
        if (call == null) {
            return;
        }
        ArrayDeque<HurtCall> stack = ACTIVE.get();
        boolean present = false;
        for (HurtCall stacked : stack) {
            if (stacked == call) {
                present = true;
                break;
            }
        }
        if (!present) {
            return;
        }
        for (int guard = stack.size(); guard > 0; guard -= 1) {
            HurtCall top = stack.removeFirst();
            if (top == call) {
                return;
            }
        }
    }

    public static HurtCall currentHurt() {
        ArrayDeque<HurtCall> stack = ACTIVE.get();
        if (stack.isEmpty()) {
            return null;
        }
        return stack.peekFirst();
    }

    private static ElementArm arm(Frame frame, boolean neutral) {
        ElementArm armed = new ElementArm(frame, neutral);
        WAITING.get().addFirst(armed);
        return armed;
    }

    public static final class Boundary {
        private final int depth;

        private Boundary(int depth) {
            this.depth = depth;
        }
    }

    public static final class ElementArm {
        private final Frame frame;
        private final boolean neutral;
        private boolean claimed;

        private ElementArm(Frame frame, boolean neutral) {
            this.frame = frame;
            this.neutral = neutral;
        }
    }

    public static final class HurtCall {
        private final long identity;
        private final Object target;
        private final Object source;
        private final Frame frame;

        private HurtCall(long identity, Object target, Object source, Frame frame) {
            this.identity = identity;
            this.target = target;
            this.source = source;
            this.frame = frame;
        }

        public long identity() {
            return this.identity;
        }

        public Object target() {
            return this.target;
        }

        public Object source() {
            return this.source;
        }

        Frame frame() {
            return this.frame;
        }
    }

    static final class Frame {
        private final boolean fire;
        private final boolean water;
        private final boolean ice;
        private final UUID casterId;
        private final double fireBase;
        private boolean furnaceSettled;
        private boolean vulnerabilityApplied;

        private Frame(boolean fire, boolean water, boolean ice, UUID casterId, double fireBase, boolean furnaceSettled) {
            this.fire = fire;
            this.water = water;
            this.ice = ice;
            this.casterId = casterId;
            this.fireBase = fireBase;
            this.furnaceSettled = furnaceSettled;
        }

        private static Frame fireSource(UUID casterId, double fireBase, boolean furnaceSettled) {
            return new Frame(true, false, false, casterId, fireBase, furnaceSettled);
        }

        private static Frame waterSource() {
            return new Frame(false, true, false, null, 0.0d, true);
        }

        private static Frame iceSource() {
            return new Frame(false, false, true, null, 0.0d, true);
        }

        private static Frame waterIceSource() {
            return new Frame(false, true, true, null, 0.0d, true);
        }

        boolean fire() {
            return this.fire;
        }

        boolean water() {
            return this.water;
        }

        boolean ice() {
            return this.ice;
        }

        UUID casterId() {
            return this.casterId;
        }

        double fireBase() {
            return this.fireBase;
        }

        boolean furnaceSettled() {
            return this.furnaceSettled;
        }

        void settleFurnace() {
            this.furnaceSettled = true;
        }

        boolean vulnerabilityApplied() {
            return this.vulnerabilityApplied;
        }

        void applyVulnerability() {
            this.vulnerabilityApplied = true;
        }
    }
}
