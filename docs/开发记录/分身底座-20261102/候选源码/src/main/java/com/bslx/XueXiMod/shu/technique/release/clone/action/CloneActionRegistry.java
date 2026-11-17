package com.bslx.XueXiMod.shu.technique.release.clone.action;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 已登记的分身动作。只在模组初始化登记，运行时在服务器线程查询。
 * 不按术式编号分叉。
 */
public final class CloneActionRegistry {
    public enum Admission {
        ADMITTED,
        UNSUPPORTED_ACTION,
        UNSUPPORTED_CHAIN
    }

    private static final CloneActionCapabilities NO_CHAIN = new CloneActionCapabilities(
            false, false, false, false, false, false, false);
    private static final Map<String, RegisteredAction> REGISTERED = new HashMap<>();

    private CloneActionRegistry() {
    }

    public static void register(CloneAction action) {
        Objects.requireNonNull(action, "action");
        CloneActionCapabilities capabilities = action instanceof ClonePulseAction
                ? CloneActionCapabilities.full()
                : NO_CHAIN;
        bind(action, capabilities);
    }

    public static @Nullable CloneAction get(String actionKey) {
        RegisteredAction registered = REGISTERED.get(actionKey);
        if (registered == null) {
            return null;
        }
        return registered.action;
    }

    public static Admission admit(String actionKey, CloneActionCapabilities required) {
        RegisteredAction registered = actionKey == null ? null : REGISTERED.get(actionKey);
        if (registered == null) {
            return Admission.UNSUPPORTED_ACTION;
        }
        if (required == null || !registered.capabilities.covers(required)) {
            return Admission.UNSUPPORTED_CHAIN;
        }
        return Admission.ADMITTED;
    }

    private static void bind(CloneAction action, CloneActionCapabilities capabilities) {
        String actionKey = action.actionKey();
        if (actionKey == null || actionKey.isBlank()) {
            throw new IllegalArgumentException("动作键不能为空");
        }
        if (REGISTERED.containsKey(actionKey)) {
            throw new IllegalStateException("动作已经登记: " + actionKey);
        }
        REGISTERED.put(actionKey, new RegisteredAction(action, capabilities));
    }

    private record RegisteredAction(CloneAction action, CloneActionCapabilities capabilities) {
    }
}
