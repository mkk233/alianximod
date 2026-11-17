package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.shu.technique.release.domain.DomainBound;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainClientBridge;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainSightStore;
import com.bslx.XueXiMod.shu.technique.release.relations.Relations;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** 只给本地玩家做移动预测。其他实体等服务端裁决。 */
public final class DomainClientState {
    private static final List<DomainBound> SHARED_EMPTY = List.of();

    private DomainClientState() {
    }

    public static void bind() {
        DomainClientBridge.bind(DomainClientState::targets);
        FlowClientMotion.bind();
    }

    private static List<DomainBound> targets(Entity mover) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || mover == null) {
            return SHARED_EMPTY;
        }
        Entity viewer = minecraft.player;
        if (mover != viewer && mover.getRootVehicle() != viewer.getRootVehicle()) {
            return SHARED_EMPTY;
        }
        Collection<DomainSightStore.Sight> sights = DomainSightStore.all();
        if (sights.isEmpty()) {
            return SHARED_EMPTY;
        }
        ArrayList<DomainBound> found = new ArrayList<>();
        for (DomainSightStore.Sight sight : sights) {
            found.add(new ViewerSight(sight, viewer));
        }
        return found;
    }

    private static final class ViewerSight implements DomainBound {
        private final DomainSightStore.Sight sight;
        private final Entity viewer;

        private ViewerSight(DomainSightStore.Sight sight, Entity viewer) {
            this.sight = sight;
            this.viewer = viewer;
        }

        @Override
        public java.util.UUID ownerId() {
            return sight.ownerId();
        }

        @Override
        public com.bslx.XueXiMod.shu.technique.release.domain.DomainGeometry geometry() {
            return sight.geometry();
        }

        @Override
        public boolean active() {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft.level != null && minecraft.level.getGameTime() < sight.visualEnd();
        }

        @Override
        public com.bslx.XueXiMod.shu.technique.release.domain.DomainBoundary boundary() {
            return sight.boundary();
        }

        @Override
        public boolean passes(Entity member) {
            if (sight.viewerMayPass() && member != null && member.getUUID().equals(viewer.getUUID())) {
                return true;
            }
            return Relations.permits(sight.ownerId(), member) || Relations.ally(sight.ownerId(), member);
        }
    }
}
