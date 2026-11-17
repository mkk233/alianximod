package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.shu.technique.release.relations.Relations;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/** 服务端实例和客户端快照都能拿来裁切移动。 */
public interface DomainBound {
    UUID ownerId();

    DomainGeometry geometry();

    boolean active();

    default DomainBoundary boundary() {
        return DomainBoundary.SEAL;
    }

    default boolean passes(Entity member) {
        return Relations.permits(ownerId(), member) || Relations.ally(ownerId(), member);
    }
}
