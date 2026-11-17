package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.shu.technique.release.domain.DomainBound;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainClientBridge;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainSightStore;
import com.bslx.XueXiMod.shu.technique.release.relations.Relations;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/** 只给本地玩家做移动预测。其他实体等服务端裁决。 */
public final class DomainClientState {
    private DomainClientState() {
    }

    public static void bind() {
        DomainClientBridge.bind(DomainClientState::targets);
    }

    private static List<DomainBound> targets(Entity mover) {
        ArrayList<DomainBound> found = new ArrayList<>();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || mover == null) {
            return found;
        }
        Entity viewer = minecraft.player;
        if (mover != viewer && mover.getRootVehicle() != viewer.getRootVehicle()) {
            return found;
        }
        for (DomainSightStore.Sight sight : DomainSightStore.all()) {
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
