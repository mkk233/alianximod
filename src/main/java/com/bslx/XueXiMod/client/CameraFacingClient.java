package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.carrier.BatchVisualTexture;
import com.bslx.XueXiMod.visual.facing.CameraFacingEffectId;
import com.bslx.XueXiMod.visual.facing.CameraFacingRegistry;
import com.bslx.XueXiMod.visual.facing.CameraFacingTag;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * 持续面向玩家的唯一客户端入口。未声明标签的特效不读取相机朝向。
 */
public final class CameraFacingClient {
    private CameraFacingClient() {
    }

    public static boolean acceptsCameraPlane(BatchVisualTexture texture) {
        return CameraFacingRegistry.tagOf(texture) == CameraFacingTag.CAMERA_PLANE;
    }

    public static boolean acceptsCameraPlane(CameraFacingEffectId effectId) {
        return CameraFacingRegistry.tagOf(effectId) == CameraFacingTag.CAMERA_PLANE;
    }

    public static boolean acceptsUprightCameraYaw(CameraFacingEffectId effectId) {
        return CameraFacingRegistry.tagOf(effectId) == CameraFacingTag.UPRIGHT_CAMERA_YAW;
    }

    /**
     * 种火、引燃和扬尘的全自由度相机平面。
     * 种火额外沿视线拉近。火苗不再持续面向玩家。
     * 引爆的 26 团各自朝向相机，不走这张整组平面。
     */
    public static void orientDeclaredPlane(PoseStack pose, BatchVisualTexture texture, Quaternionf cameraOrientation) {
        if (!acceptsCameraPlane(texture) || pose == null || cameraOrientation == null) {
            return;
        }
        pose.mulPose(cameraOrientation);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        if (texture == BatchVisualTexture.SEED) {
            pose.translate(0.0D, 0.0D, 0.08D);
        }
    }

    /**
     * 焚天天空火云和火种余额小火苗的相机平面。旋转公式与原渲染保持一致。
     */
    public static void orientDeclaredPlane(PoseStack pose, CameraFacingEffectId effectId, Camera camera) {
        if (!acceptsCameraPlane(effectId) || pose == null || camera == null) {
            return;
        }
        if (effectId != CameraFacingEffectId.FENTIAN_SKY_FLAME
                && effectId != CameraFacingEffectId.FIRE_SEED_ORBIT_FLAME) {
            return;
        }
        pose.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
    }

    /**
     * 万剑归宗漩涡：剑尖保持竖直，只按水平角转向镜头，再抵消物品固定朝向。
     */
    public static void orientUprightCameraYaw(PoseStack pose, CameraFacingEffectId effectId, Vec3 cameraPosition,
                                              double entityX, double entityZ) {
        if (!acceptsUprightCameraYaw(effectId) || pose == null || cameraPosition == null) {
            return;
        }
        double deltaX = cameraPosition.x - entityX;
        double deltaZ = cameraPosition.z - entityZ;
        float faceYaw = (float) (Mth.atan2(deltaZ, deltaX) * (180.0 / Math.PI)) - 90.0F;
        pose.mulPose(Axis.YP.rotationDegrees(-faceYaw));
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
    }
}
