package com.bslx.XueXiMod.shu.technique.release.domain.motion;

import com.bslx.XueXiMod.network.DomainSyncPacket;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端运动预览存储。同步包写入模式、半径、根编号和挂载宿主，移除时一起丢掉。
 * 球形场地没有旋流或气泡预览，护体不公布受控根。
 */
public final class DomainMotionPreview {
    private static final ConcurrentHashMap<UUID, MotionFrame> FRAMES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, List<UUID>> ROOTS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, UUID> MOUNTED_HOSTS = new ConcurrentHashMap<>();

    private DomainMotionPreview() {
    }

    /** 接收运动同步。空包忽略；移除时清掉该实例的三份预览，不扣资源。 */
    public static void capture(DomainSyncPacket packet) {
        if (packet == null) {
            return;
        }
        if (packet.removed()) {
            FRAMES.remove(packet.instanceId());
            ROOTS.remove(packet.instanceId());
            MOUNTED_HOSTS.remove(packet.instanceId());
            return;
        }
        FRAMES.put(packet.instanceId(), packet.motionFrame());
        ROOTS.put(packet.instanceId(), List.copyOf(packet.controlledRoots()));
        if (packet.mountedHost() == null) {
            MOUNTED_HOSTS.remove(packet.instanceId());
        } else {
            MOUNTED_HOSTS.put(packet.instanceId(), packet.mountedHost());
        }
    }

    public static MotionFrame frame(UUID instanceId) {
        MotionFrame stored = FRAMES.get(instanceId);
        return stored == null ? MotionFrame.NONE : stored;
    }

    /** 只返回服务端同步过的根实体。附近实体不算受控。 */
    public static List<UUID> controlledRoots(UUID instanceId) {
        List<UUID> stored = ROOTS.get(instanceId);
        return stored == null ? List.of() : stored;
    }

    /** 气泡同步来的真实挂载宿主。没有挂载时为空。 */
    public static UUID mountedHost(UUID instanceId) {
        return instanceId == null ? null : MOUNTED_HOSTS.get(instanceId);
    }
}
