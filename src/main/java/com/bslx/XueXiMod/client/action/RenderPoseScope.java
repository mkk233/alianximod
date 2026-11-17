package com.bslx.XueXiMod.client.action;

import java.util.ArrayDeque;
import java.util.UUID;

import com.bslx.XueXiMod.animation.runtime.ActionSampleContext;

/**
 * 客户端渲染调用的唯一视角身份栈。
 * 第三人称枢轴只在栈顶同时匹配视图、模型和玩家时写入。
 * 蒙皮上下文挂在同一帧上，不另开视角 ThreadLocal。
 */
public final class RenderPoseScope {
    public enum View {
        THIRD_PERSON,
        FIRST_PERSON
    }

    private static final ThreadLocal<ArrayDeque<Frame>> FRAMES = ThreadLocal.withInitial(ArrayDeque::new);

    private RenderPoseScope() {
    }

    public static void runThirdPerson(Object model, Object player, Runnable body) {
        run(View.THIRD_PERSON, model, player, body);
    }

    public static void runFirstPerson(Object model, Object player, Runnable body) {
        run(View.FIRST_PERSON, model, player, body);
    }

    public static boolean allowsThirdPerson(Object model, Object player) {
        Frame frame = FRAMES.get().peek();
        return frame != null
                && frame.view == View.THIRD_PERSON
                && frame.model == model
                && frame.player == player;
    }

    public static int depth() {
        return FRAMES.get().size();
    }

    public static void attachSkin(SkinBinding binding) {
        Frame frame = FRAMES.get().peek();
        if (frame == null) {
            throw new IllegalStateException("skin binding has no pose scope");
        }
        if (frame.binding != null && frame.binding != binding) {
            frame.binding.close();
        }
        frame.binding = binding;
    }

    public static ActionSampleContext frozenSample(UUID ownerId) {
        Frame frame = FRAMES.get().peek();
        if (frame == null || frame.binding == null || ownerId == null) {
            return null;
        }
        ActionSampleContext sample = frame.binding.sample();
        if (sample == null || !ownerId.equals(sample.ownerId())) {
            return null;
        }
        return sample;
    }

    public static boolean skipsRigidWriter(Object model, Object player) {
        Frame frame = FRAMES.get().peek();
        return frame != null
                && frame.view == View.THIRD_PERSON
                && frame.model == model
                && frame.player == player
                && frame.binding != null
                && frame.binding.skipRigidWriter();
    }

    public static SkinBinding skinBinding() {
        Frame frame = FRAMES.get().peek();
        return frame == null ? null : frame.binding;
    }

    /** 断线时清掉残留帧和租约。资源缓存不在这里清。 */
    public static void discardOpenFrames() {
        ArrayDeque<Frame> frames = FRAMES.get();
        while (!frames.isEmpty()) {
            frames.pop().closeBinding();
        }
    }

    private static void run(View view, Object model, Object player, Runnable body) {
        Frame frame = new Frame(view, model, player);
        FRAMES.get().push(frame);
        try {
            if (body != null) {
                body.run();
            }
        } finally {
            ArrayDeque<Frame> frames = FRAMES.get();
            if (frames.peek() == frame) {
                frames.pop();
            }
            frame.closeBinding();
        }
    }

    private static final class Frame {
        private final View view;
        private final Object model;
        private final Object player;
        private SkinBinding binding;

        private Frame(View view, Object model, Object player) {
            this.view = view;
            this.model = model;
            this.player = player;
        }

        private void closeBinding() {
            SkinBinding attached = this.binding;
            this.binding = null;
            if (attached != null) {
                attached.close();
            }
        }
    }
}
