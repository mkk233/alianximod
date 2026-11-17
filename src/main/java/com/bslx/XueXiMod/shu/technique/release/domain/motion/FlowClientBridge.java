package com.bslx.XueXiMod.shu.technique.release.domain.motion;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 客户端流场桥。启动时接上本地来源；未接入时服务端调用这些方法是空操作。
 * 球形场地不走持续流场回执，护体没有位移意图。
 */
public final class FlowClientBridge {
    public interface Source {
        Vec3 combine(Entity mover, Vec3 nativeMove);

        void beginStep(Entity mover);

        void finishStep(Entity mover);

        void acceptIntent(boolean removed, boolean clearInstance, UUID instanceId, UUID rootId, long epoch, long step,
                          double horizontalX, double horizontalZ, boolean rising, double riseFloor);

        void acceptCorrection(UUID rootId, int sequence, double worldX, double worldY, double worldZ, float yaw, float pitch);

        Receipt takeReceipt(Entity mover);

        double riseFloor(Entity mover);
    }

    public static final class Receipt {
        public final UUID instanceId;
        public final UUID rootId;
        public final long epoch;
        public final long step;
        public final boolean rising;
        public final double nativeVertical;
        public final boolean playerJumped;

        public Receipt(UUID instanceId, UUID rootId, long epoch, long step, boolean rising, double nativeVertical,
                       boolean playerJumped) {
            this.instanceId = instanceId;
            this.rootId = rootId;
            this.epoch = epoch;
            this.step = step;
            this.rising = rising;
            this.nativeVertical = nativeVertical;
            this.playerJumped = playerJumped;
        }
    }

    private static final Source EMPTY = new Source() {
        @Override
        public Vec3 combine(Entity mover, Vec3 nativeMove) {
            return nativeMove;
        }

        @Override
        public void beginStep(Entity mover) {
        }

        @Override
        public void finishStep(Entity mover) {
        }

        @Override
        public void acceptIntent(boolean removed, boolean clearInstance, UUID instanceId, UUID rootId, long epoch,
                                 long step, double horizontalX, double horizontalZ, boolean rising, double riseFloor) {
        }

        @Override
        public void acceptCorrection(UUID rootId, int sequence, double worldX, double worldY, double worldZ, float yaw,
                                     float pitch) {
        }

        @Override
        public Receipt takeReceipt(Entity mover) {
            return null;
        }

        @Override
        public double riseFloor(Entity mover) {
            return 0.0D;
        }
    };

    private static Source source = EMPTY;

    private FlowClientBridge() {
    }

    /** 接上客户端流场来源。传入空时退回空操作，不扣资源。 */
    public static void bind(Source next) {
        source = next == null ? EMPTY : next;
    }

    /** 合并客户端本地位移。没有来源时返回原位移。 */
    public static Vec3 combine(Entity mover, Vec3 nativeMove) {
        return source.combine(mover, nativeMove);
    }

    /** 通知客户端开始一步。没有来源时不记状态。 */
    public static void beginStep(Entity mover) {
        source.beginStep(mover);
    }

    /** 通知客户端结束一步。没有来源时不补位移。 */
    public static void finishStep(Entity mover) {
        source.finishStep(mover);
    }

    /** 把服务端位移意图交给客户端。没有来源时丢弃，不回退资源。 */
    public static void acceptIntent(boolean removed, boolean clearInstance, UUID instanceId, UUID rootId, long epoch,
                                    long step, double horizontalX, double horizontalZ, boolean rising, double riseFloor) {
        source.acceptIntent(removed, clearInstance, instanceId, rootId, epoch, step, horizontalX, horizontalZ, rising, riseFloor);
    }

    /** 把位置修正交给客户端。没有来源时不改实体。 */
    public static void acceptCorrection(UUID rootId, int sequence, double worldX, double worldY, double worldZ, float yaw,
                                        float pitch) {
        source.acceptCorrection(rootId, sequence, worldX, worldY, worldZ, yaw, pitch);
    }

    /** 取走客户端这一步回执。没有来源或没有回执时返回空。 */
    public static Receipt takeReceipt(Entity mover) {
        return source.takeReceipt(mover);
    }

    /** 读取客户端上升下限。没有来源时返回 0。 */
    public static double riseFloor(Entity mover) {
        return source.riseFloor(mover);
    }
}
