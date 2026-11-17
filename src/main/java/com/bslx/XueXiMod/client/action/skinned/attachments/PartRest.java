package com.bslx.XueXiMod.client.action.skinned.attachments;

import java.util.ArrayList;
import java.util.List;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.math.Vec3f;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;

/** 部件初值矩阵，匹配 translateAndRotate 的平移、ZYX 旋转和单位缩放。不读取本帧动画。 */
final class PartRest {
    private PartRest() {
    }

    static OpenMatrix4f fromInitial(ModelPart part) {
        PartPose initial = part.getInitialPose();
        return fromPixels(initial.x, initial.y, initial.z, initial.xRot, initial.yRot, initial.zRot);
    }

    static OpenMatrix4f fromPixels(
            float pixelX,
            float pixelY,
            float pixelZ,
            float xRadians,
            float yRadians,
            float zRadians) {
        OpenMatrix4f translation = OpenMatrix4f.createTranslation(pixelX / 16.0F, pixelY / 16.0F, pixelZ / 16.0F);
        OpenMatrix4f spinZ = OpenMatrix4f.rotate(zRadians, Vec3f.Z_AXIS, new OpenMatrix4f(), null);
        OpenMatrix4f spinY = OpenMatrix4f.rotate(yRadians, Vec3f.Y_AXIS, new OpenMatrix4f(), null);
        OpenMatrix4f spinX = OpenMatrix4f.rotate(xRadians, Vec3f.X_AXIS, new OpenMatrix4f(), null);
        OpenMatrix4f combined = OpenMatrix4f.mul(translation, spinZ, null);
        combined = OpenMatrix4f.mul(combined, spinY, null);
        return OpenMatrix4f.mul(combined, spinX, null);
    }

    static List<SavedPart> capture(ModelPart part) {
        List<SavedPart> saved = new ArrayList<>();
        for (ModelPart bodyPart : part.getAllParts().toList()) {
            saved.add(new SavedPart(bodyPart, bodyPart.storePose(), bodyPart.xScale, bodyPart.yScale, bodyPart.zScale));
        }
        return saved;
    }

    static void loadInitial(ModelPart part) {
        for (ModelPart bodyPart : part.getAllParts().toList()) {
            bodyPart.loadPose(bodyPart.getInitialPose());
            bodyPart.xScale = 1.0F;
            bodyPart.yScale = 1.0F;
            bodyPart.zScale = 1.0F;
        }
    }

    static void restore(List<SavedPart> saved) {
        if (saved == null) {
            return;
        }
        for (SavedPart bodyPart : saved) {
            bodyPart.part.loadPose(bodyPart.pose);
            bodyPart.part.xScale = bodyPart.scaleX;
            bodyPart.part.yScale = bodyPart.scaleY;
            bodyPart.part.zScale = bodyPart.scaleZ;
        }
    }

    static final class SavedPart {
        final ModelPart part;
        final PartPose pose;
        final float scaleX;
        final float scaleY;
        final float scaleZ;

        SavedPart(ModelPart part, PartPose pose, float scaleX, float scaleY, float scaleZ) {
            this.part = part;
            this.pose = pose;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.scaleZ = scaleZ;
        }
    }
}
