package com.bslx.XueXiMod.shu.technique.enchant;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

import java.util.ArrayDeque;

/**
 * 一次真实 hurt 的同步配对。监听只记下这一下的伤害事件，hurt 返回后才看
 * 返回值、是否取消、最终正伤。嵌套用栈，不按玩家或 tick 认人，也不留到下一拍。
 * 完全吸收或事件被取消不提交。致命击仍提交，不把目标还活着当成门槛。
 */
public final class TideMarkDamageScope {
    private static final ThreadLocal<ArrayDeque<Frame>> FRAMES = new ThreadLocal<>();

    private TideMarkDamageScope() {
    }

    public static Token open(Object target, Object source, TideMarkSuccess contribution) {
        if (target == null || contribution == null || contribution.marks() <= 0) {
            return Token.NONE;
        }
        Frame frame = new Frame(target, source, contribution);
        frames().addLast(frame);
        return new Token(frame);
    }

    /**
     * 和 {@link #open} 一样配对这一下的伤害事件。条数为 0 时旧入口不建帧，这里仍建。
     * 奖励还是只在 {@link #finish} 里按原来的贡献发放，零条贡献自己不会给潮湿或水势。
     */
    public static Token watch(Object target, Object source, TideMarkSuccess contribution) {
        if (target == null) {
            return Token.NONE;
        }
        TideMarkSuccess recorded = contribution == null ? TideMarkSuccess.NONE : contribution;
        Frame frame = new Frame(target, source, recorded);
        frames().addLast(frame);
        return new Token(frame);
    }

    public static void observe(LivingDamageEvent event) {
        if (event == null || event.getSource() == null) {
            return;
        }
        associate(event.getEntity(), event.getSource().getEntity(), new EventView(event));
    }

    /** 测试和没有 Forge 事件对象时的同一条关联。数值在 finish 时读取。 */
    public static void note(Object target, Object source, boolean canceled, float amount) {
        associate(target, source, new FixedView(canceled, amount));
    }

    public static final class Reading {
        private boolean canceled;
        private boolean eventSeen;
        private boolean accepted;
        private float amount;

        public boolean canceled() {
            return canceled;
        }

        public boolean eventSeen() {
            return eventSeen;
        }

        public boolean accepted() {
            return accepted;
        }

        public float amount() {
            return amount;
        }
    }

    /** 和 {@link #finish} 同一次配对，并留下取消和最终伤害，供命中结果区分。 */
    public static Reading finishReading(Token token, boolean hurtSucceeded) {
        Reading reading = new Reading();
        if (token == null || token.frame == null || token.closed) {
            return reading;
        }
        DamageView seen = token.frame.view;
        reading.eventSeen = seen != null;
        reading.canceled = seen != null && seen.canceled();
        reading.amount = seen == null ? 0f : seen.amount();
        reading.accepted = finish(token, hurtSucceeded);
        return reading;
    }

    public static boolean finish(Token token, boolean hurtSucceeded) {
        if (token == null || token.frame == null || token.closed) {
            return false;
        }
        token.closed = true;
        ArrayDeque<Frame> frames = FRAMES.get();
        if (frames == null) {
            return false;
        }
        if (frames.peekLast() != token.frame) {
            frames.remove(token.frame);
            dropIfEmpty(frames);
            return false;
        }
        frames.removeLast();
        dropIfEmpty(frames);
        if (!hurtSucceeded || token.frame.view == null) {
            return false;
        }
        if (token.frame.view.canceled() || token.frame.view.amount() <= 0f) {
            return false;
        }
        if (token.frame.submitted) {
            return false;
        }
        token.frame.submitted = true;
        if (token.frame.target instanceof LivingEntity living) {
            token.frame.contribution.grant(living);
        }
        return true;
    }

    private static void associate(Object target, Object source, DamageView view) {
        ArrayDeque<Frame> frames = FRAMES.get();
        if (frames == null || frames.isEmpty() || view == null || target == null) {
            return;
        }
        Frame top = frames.peekLast();
        if (top.view != null || top.target != target || top.source != source) {
            return;
        }
        top.view = view;
    }

    private static ArrayDeque<Frame> frames() {
        ArrayDeque<Frame> frames = FRAMES.get();
        if (frames == null) {
            frames = new ArrayDeque<>();
            FRAMES.set(frames);
        }
        return frames;
    }

    private static void dropIfEmpty(ArrayDeque<Frame> frames) {
        if (frames.isEmpty()) {
            FRAMES.remove();
        }
    }

    public static final class Token {
        static final Token NONE = new Token(null);

        private final Frame frame;
        private boolean closed;

        private Token(Frame frame) {
            this.frame = frame;
        }
    }

    private interface DamageView {
        boolean canceled();

        float amount();
    }

    private static final class EventView implements DamageView {
        private final LivingDamageEvent event;

        private EventView(LivingDamageEvent event) {
            this.event = event;
        }

        @Override
        public boolean canceled() {
            return this.event.isCanceled();
        }

        @Override
        public float amount() {
            return this.event.getAmount();
        }
    }

    private static final class FixedView implements DamageView {
        private final boolean canceled;
        private final float amount;

        private FixedView(boolean canceled, float amount) {
            this.canceled = canceled;
            this.amount = amount;
        }

        @Override
        public boolean canceled() {
            return this.canceled;
        }

        @Override
        public float amount() {
            return this.amount;
        }
    }

    private static final class Frame {
        private final Object target;
        private final Object source;
        private final TideMarkSuccess contribution;
        private DamageView view;
        private boolean submitted;

        private Frame(Object target, Object source, TideMarkSuccess contribution) {
            this.target = target;
            this.source = source;
            this.contribution = contribution;
        }
    }
}
