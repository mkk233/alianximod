package com.bslx.XueXiMod.network;

/**
 * 客户端在自己的 Dist 上接住火视觉包。这个类不引用客户端类型。
 */
public final class FireVisualBridge {
    public interface Sink {
        void accept(FireVisualPacket packet);
    }

    private static Sink sink = packet -> {
    };

    private FireVisualBridge() {
    }

    public static void bind(Sink next) {
        sink = next == null ? packet -> {
        } : next;
    }

    public static void accept(FireVisualPacket packet) {
        sink.accept(packet);
    }
}
