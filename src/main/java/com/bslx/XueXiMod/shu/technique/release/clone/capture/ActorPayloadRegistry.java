package com.bslx.XueXiMod.shu.technique.release.clone.capture;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 显式载荷适配表。初始化登记，服务器线程查询。
 * 内置表只有断潮，缺月和其他水系不会被登记成可放。
 */
public final class ActorPayloadRegistry {
    private static final Map<String, ActorPayloadAdapter> REGISTERED = new HashMap<>();

    private ActorPayloadRegistry() {
    }

    public static void register(ActorPayloadAdapter adapter) {
        Objects.requireNonNull(adapter, "adapter");
        String techniqueId = adapter.techniqueId();
        if (techniqueId == null || techniqueId.isBlank()) {
            throw new IllegalArgumentException("载荷适配器缺少术式编号");
        }
        if (adapter.attribute() == null) {
            throw new IllegalArgumentException("载荷适配器缺少属性");
        }
        if (adapter.version() < 1) {
            throw new IllegalArgumentException("载荷适配器版本必须从 1 起");
        }
        if (REGISTERED.containsKey(techniqueId)) {
            throw new IllegalStateException("载荷适配器已经登记: " + techniqueId);
        }
        REGISTERED.put(techniqueId, adapter);
    }

    public static void registerBuiltins() {
        register(new TideSeverActorAdapter());
    }

    public static @Nullable ActorPayloadAdapter find(@Nullable String techniqueId) {
        if (techniqueId == null || techniqueId.isBlank()) {
            return null;
        }
        return REGISTERED.get(techniqueId);
    }
}
