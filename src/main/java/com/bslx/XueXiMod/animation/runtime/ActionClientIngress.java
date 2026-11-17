package com.bslx.XueXiMod.animation.runtime;

import com.bslx.XueXiMod.network.ActionSyncS2CPacket;

/** 通用层只保留入口。客户端在自己的加载阶段把实现接进来。 */
public final class ActionClientIngress {
    public interface Sink {
        void accept(ActionSyncS2CPacket packet, Object connectionLink);
    }

    private static final Sink IDLE = (packet, connectionLink) -> {
    };
    private static volatile Sink sink = IDLE;

    private ActionClientIngress() {
    }

    public static void bind(Sink next) {
        sink = next == null ? IDLE : next;
    }

    public static void accept(ActionSyncS2CPacket packet, Object connectionLink) {
        if (packet != null) {
            sink.accept(packet, connectionLink);
        }
    }
}
