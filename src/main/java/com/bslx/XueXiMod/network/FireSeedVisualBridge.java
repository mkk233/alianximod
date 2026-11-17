package com.bslx.XueXiMod.network;

/**
 * 客户端在自己的 Dist 上接住火种显示包。这个类不引用客户端类型。
 */
public final class FireSeedVisualBridge {
    public interface Sink {
        void accept(FireSeedVisualPacket packet);
    }

    private static Sink sink = packet -> {
    };

    private FireSeedVisualBridge() {
    }

    public static void bind(Sink next) {
        sink = next == null ? packet -> {
        } : next;
    }

    public static void accept(FireSeedVisualPacket packet) {
        sink.accept(packet);
    }
}
