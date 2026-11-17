package com.bslx.XueXiMod.client.action;

import java.util.ArrayList;
import java.util.List;

import com.bslx.XueXiMod.mixin.client.PlayerModelPartsAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

/** 一次玩家渲染的模型快照。嵌套时每层拿自己的副本，不覆盖外层。 */
public final class ModelPartSnapshot {
    private final List<SavedPart> saved = new ArrayList<>();

    private ModelPartSnapshot() {
    }

    public static ModelPartSnapshot capturePlayer(AbstractClientPlayer player) {
        if (player == null) {
            return new ModelPartSnapshot();
        }
        EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof PlayerRenderer playerRenderer)) {
            return new ModelPartSnapshot();
        }
        return capture(playerRenderer.getModel());
    }

    public static ModelPartSnapshot capture(PlayerModel<?> model) {
        ModelPartSnapshot snapshot = new ModelPartSnapshot();
        if (model == null) {
            return snapshot;
        }
        snapshot.keep(model.body);
        snapshot.keep(model.head);
        snapshot.keep(model.hat);
        snapshot.keep(model.rightArm);
        snapshot.keep(model.leftArm);
        snapshot.keep(model.rightLeg);
        snapshot.keep(model.leftLeg);
        snapshot.keep(model.rightSleeve);
        snapshot.keep(model.leftSleeve);
        snapshot.keep(model.rightPants);
        snapshot.keep(model.leftPants);
        snapshot.keep(model.jacket);
        PlayerModelPartsAccessor accessor = (PlayerModelPartsAccessor) model;
        snapshot.keep(accessor.xuexiCloak());
        snapshot.keep(accessor.xuexiEar());
        return snapshot;
    }

    public void restore() {
        for (SavedPart savedPart : this.saved) {
            savedPart.restore();
        }
    }

    private void keep(ModelPart part) {
        if (part != null) {
            this.saved.add(new SavedPart(part));
        }
    }

    private static final class SavedPart {
        private final ModelPart part;
        private final float offsetAcross;
        private final float offsetUpward;
        private final float offsetAhead;
        private final float pitchRadians;
        private final float yawRadians;
        private final float rollRadians;
        private final float scaleAcross;
        private final float scaleUpward;
        private final float scaleAhead;
        private final boolean shown;
        private final boolean skipDrawn;

        private SavedPart(ModelPart part) {
            this.part = part;
            this.offsetAcross = part.x;
            this.offsetUpward = part.y;
            this.offsetAhead = part.z;
            this.pitchRadians = part.xRot;
            this.yawRadians = part.yRot;
            this.rollRadians = part.zRot;
            this.scaleAcross = part.xScale;
            this.scaleUpward = part.yScale;
            this.scaleAhead = part.zScale;
            this.shown = part.visible;
            this.skipDrawn = part.skipDraw;
        }

        private void restore() {
            this.part.x = this.offsetAcross;
            this.part.y = this.offsetUpward;
            this.part.z = this.offsetAhead;
            this.part.xRot = this.pitchRadians;
            this.part.yRot = this.yawRadians;
            this.part.zRot = this.rollRadians;
            this.part.xScale = this.scaleAcross;
            this.part.yScale = this.scaleUpward;
            this.part.zScale = this.scaleAhead;
            this.part.visible = this.shown;
            this.part.skipDraw = this.skipDrawn;
        }
    }
}
