package com.bslx.XueXiMod.client.action;

import java.util.ArrayDeque;

/** 嵌套渲染时按调用栈保存当前 partial tick。采样不在这里推进。 */
public final class ActionRenderClock {
    private static final ArrayDeque<Float> PARTIALS = new ArrayDeque<>();

    private ActionRenderClock() {
    }

    public static void push(float partialTick) {
        float safe = Float.isFinite(partialTick) ? partialTick : 0.0F;
        PARTIALS.push(safe);
    }

    public static void pop() {
        if (!PARTIALS.isEmpty()) {
            PARTIALS.pop();
        }
    }

    public static float partialOr(float fallback) {
        if (PARTIALS.isEmpty()) {
            return Float.isFinite(fallback) ? fallback : 0.0F;
        }
        return PARTIALS.peek();
    }
}
