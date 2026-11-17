package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.shu.technique.release.relations.Relations;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/**
 * 可裁切移动的领域边界合同。服务端实例和客户端快照都实现它，生命周期仍由各自持有者管理。
 * 球形场地钉在锚点上，护体跟着身体，都不走这份边界合同。
 */
public interface DomainBound {
    UUID ownerId();

    DomainGeometry geometry();

    boolean active();

    default DomainBoundary boundary() {
        return DomainBoundary.SEAL;
    }

    /** 主人或盟友可以穿过。关系不成立时留下，交几何继续裁切，不扣资源。 */
    default boolean passes(Entity member) {
        return Relations.permits(ownerId(), member) || Relations.ally(ownerId(), member);
    }
}
