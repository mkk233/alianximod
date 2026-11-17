package com.bslx.XueXiMod.network;

/**
 * 客户端在自己的 Dist 上接住燎里后环。这个类不引用客户端类型。
 */
public final class LiaoliRingBridge {
    public interface Sink {
        void accept(LiaoliRingPacket packet);
    }

    private static Sink sink = packet -> {
    };

    private LiaoliRingBridge() {
    }

    public static void bind(Sink next) {
        sink = next == null ? packet -> {
        } : next;
    }

    public static void accept(LiaoliRingPacket packet) {
        sink.accept(packet);
    }
}
