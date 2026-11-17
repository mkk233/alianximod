package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.network.DomainSyncPacket;
import com.bslx.XueXiMod.shu.technique.release.domain.motion.DomainMotionPreview;
import net.minecraft.world.phys.AABB;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端领域快照存储。同步包写入和移除都由它处理，不引用客户端渲染类。
 * 球形场地不进这份体积快照，护体使用自己的环同步包。
 */
public final class DomainSightStore {
    private static final ConcurrentHashMap<UUID, Sight> SIGHTS = new ConcurrentHashMap<>();

    private DomainSightStore() {
    }

    /** 接收领域同步。旧版本不覆盖；移除只在版本不旧时生效，不扣资源。 */
    public static void accept(DomainSyncPacket packet) {
        DomainMotionPreview.capture(packet);
        if (packet.removed()) {
            Sight previous = SIGHTS.get(packet.instanceId());
            if (previous != null && packet.revision() >= previous.revision()) {
                SIGHTS.remove(packet.instanceId());
            }
            return;
        }
        Sight previous = SIGHTS.get(packet.instanceId());
        if (previous != null && packet.revision() < previous.revision()) {
            return;
        }
        SIGHTS.put(packet.instanceId(), new Sight(packet));
    }

    public static Collection<Sight> all() {
        return SIGHTS.values();
    }

    /** 清空全部客户端快照。不通知服务端，也不恢复已移除领域。 */
    public static void clear() {
        SIGHTS.clear();
    }

    public static final class Sight implements DomainBound {
        private final UUID instanceId;
        private final UUID ownerId;
        private final int revision;
        private final DomainGeometry geometry;
        private final boolean viewerMayPass;
        private final DomainVisual visual;
        private final DomainBoundary boundary;
        private final long visualOrigin;
        private final long visualEnd;
        private final int animSeed;

        private Sight(DomainSyncPacket packet) {
            this.instanceId = packet.instanceId();
            this.ownerId = packet.ownerId();
            this.revision = packet.revision();
            this.geometry = new AxisBoxGeometry(new AABB(
                    packet.lowX(), packet.lowY(), packet.lowZ(),
                    packet.highX(), packet.highY(), packet.highZ()));
            this.viewerMayPass = packet.viewerMayPass();
            this.visual = packet.visual();
            this.boundary = packet.boundary();
            this.visualOrigin = packet.visualOrigin();
            this.visualEnd = packet.visualEnd();
            this.animSeed = packet.animSeed();
        }

        public UUID instanceId() {
            return instanceId;
        }

        public int revision() {
            return revision;
        }

        public AABB bounds() {
            return geometry.bounds();
        }

        public boolean viewerMayPass() {
            return viewerMayPass;
        }

        public DomainVisual visual() {
            return visual;
        }

        public long visualOrigin() {
            return visualOrigin;
        }

        public long visualEnd() {
            return visualEnd;
        }

        public int animSeed() {
            return animSeed;
        }

        @Override
        public UUID ownerId() {
            return ownerId;
        }

        @Override
        public DomainGeometry geometry() {
            return geometry;
        }

        @Override
        public boolean active() {
            return true;
        }

        @Override
        public DomainBoundary boundary() {
            return boundary;
        }
    }
}
