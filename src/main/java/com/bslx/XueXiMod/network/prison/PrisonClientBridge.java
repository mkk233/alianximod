package com.bslx.XueXiMod.network.prison;

/**
 * 客户端在自己的 Dist 上接住水牢包。这个类不引用客户端类型，专用服务器不会因此去加载客户端。
 */
public final class PrisonClientBridge {
    public interface Sink {
        void accept(PrisonSyncPacket packet);
    }

    private static Sink sink = packet -> {
    };

    private PrisonClientBridge() {
    }

    public static void bind(Sink next) {
        sink = next == null ? packet -> {
        } : next;
    }

    public static void accept(PrisonSyncPacket packet) {
        sink.accept(packet);
    }
}
