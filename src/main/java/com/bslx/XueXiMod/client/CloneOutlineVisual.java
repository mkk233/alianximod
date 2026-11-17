package com.bslx.XueXiMod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * 已确认 Steve 水线的维持与散水。
 * 调用时姿态已经减去相机，快照锚点仍是世界坐标里的脚底。
 * 维持只走 0 到 1.12 秒循环；散水从 1.12 秒起，不再先播预览里的等待。
 */
public final class CloneOutlineVisual {
    private CloneOutlineVisual() {
    }

    public static boolean drawMaintained(CloneAfterimageSnapshot snapshot, PoseStack pose, MultiBufferSource buffers,
                                         int packedLight, float partialTick, long gameTime) {
        if (snapshot == null || pose == null || buffers == null) {
            return false;
        }
        float elapsedTicks = (float) (gameTime - snapshot.birthGameTime()) + partialTick;
        return drawAt(snapshot, pose, buffers, packedLight, SteveWaterLineVisual.residencePhase(elapsedTicks / 20.0F));
    }

    public static boolean drawScattered(CloneAfterimageSnapshot snapshot, PoseStack pose, MultiBufferSource buffers,
                                        int packedLight, float partialTick, long gameTime) {
        if (snapshot == null || pose == null || buffers == null) {
            return false;
        }
        float elapsedTicks = (float) (gameTime - snapshot.birthGameTime()) + partialTick;
        return drawAt(snapshot, pose, buffers, packedLight, SteveWaterLineVisual.scatterPhase(elapsedTicks / 20.0F));
    }

    private static boolean drawAt(CloneAfterimageSnapshot snapshot, PoseStack pose, MultiBufferSource buffers,
                                  int packedLight, float timeSeconds) {
        int brightLight = packedLight | LightTexture.FULL_BRIGHT;
        float bodyYaw = (float) (Math.atan2(-snapshot.lookX(), snapshot.lookZ()) * (180.0D / Math.PI));
        pose.pushPose();
        pose.translate(snapshot.worldX(), snapshot.worldY(), snapshot.worldZ());
        SteveWaterLineVisual.alignVanillaSteve(pose, bodyYaw);
        boolean drew = SteveWaterLineVisual.drawPhase(pose, buffers, brightLight, timeSeconds);
        pose.popPose();
        return drew;
    }
}
