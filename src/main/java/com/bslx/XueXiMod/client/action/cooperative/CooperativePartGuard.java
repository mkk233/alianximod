package com.bslx.XueXiMod.client.action.cooperative;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

/** 分身和玩家共用模型时，画完立刻恢复部件，避免下一个实体继承偏移。 */
public final class CooperativePartGuard {
    private static final List<SavedPart> SAVED = new ArrayList<>();

    private CooperativePartGuard() {
    }

    public static void open(HumanoidModel<?> model) {
        SAVED.clear();
        if (model == null) {
            return;
        }
        keep(model.body);
        keep(model.head);
        keep(model.hat);
        keep(model.rightArm);
        keep(model.leftArm);
        keep(model.rightLeg);
        keep(model.leftLeg);
    }

    public static void close() {
        for (SavedPart savedPart : SAVED) {
            savedPart.restore();
        }
        SAVED.clear();
    }

    private static void keep(ModelPart part) {
        if (part != null) {
            SAVED.add(new SavedPart(part));
        }
    }

    private static final class SavedPart {
        private final ModelPart part;
        private final float across;
        private final float upward;
        private final float ahead;
        private final float pitchRadians;
        private final float yawRadians;
        private final float rollRadians;

        private SavedPart(ModelPart part) {
            this.part = part;
            this.across = part.x;
            this.upward = part.y;
            this.ahead = part.z;
            this.pitchRadians = part.xRot;
            this.yawRadians = part.yRot;
            this.rollRadians = part.zRot;
        }

        private void restore() {
            this.part.x = this.across;
            this.part.y = this.upward;
            this.part.z = this.ahead;
            this.part.xRot = this.pitchRadians;
            this.part.yRot = this.yawRadians;
            this.part.zRot = this.rollRadians;
        }
    }
}
