package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainSightStore;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainVisual;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 雾雨领域的竖直雾环。顶点保持白色，只提交一份外向面，不画顶底，也不另画反向面。 */
public final class DomainMistRingRenderer {
    private static final ResourceLocation TEXTURE = new ResourceLocation(XueXiMod.MOD_ID, "textures/domain/mist_panel.png");
    private static final RenderType MESH = DomainSheetMesh.mistCurtain(TEXTURE);
    /** 每帧 64 纹素，雾的不透明像素从底边往上留了 10 纹素。 */
    private static final double FRAME_TEXEL_COUNT = 64.0d;
    private static final double BOTTOM_CLEAR_TEXELS = 10.0d;

    private DomainMistRingRenderer() {
    }

    public static void collect(DomainSightStore.Sight sight, Vec3 camera, long gameTime, DomainVolumeBatch batch) {
        if (sight.visual() != DomainVisual.VOLUME) {
            return;
        }
        if (gameTime < sight.visualOrigin() || gameTime >= sight.visualEnd()) {
            return;
        }
        AABB box = sight.bounds();
        DomainMistRingLayout layout = DomainMistRingLayout.tryBuild(
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        if (layout == null) {
            return;
        }
        int frame = (int) Math.floorMod(
                gameTime - sight.visualOrigin() + sight.animSeed(), (long) DomainMistRingLayout.FRAME_COUNT);
        float topV = DomainMistRingLayout.frameV(frame, DomainMistRingLayout.FRAME_EDGE_PAD);
        float bottomV = DomainMistRingLayout.frameV(frame, 1.0f - DomainMistRingLayout.FRAME_EDGE_PAD);
        double cameraX = camera.x;
        double cameraY = camera.y;
        double cameraZ = camera.z;
        double spanY = box.maxY - box.minY;
        double floorDrop = spanY * (BOTTOM_CLEAR_TEXELS / FRAME_TEXEL_COUNT);
        float upperY = (float) (layout.upperY() - cameraY - floorDrop);
        float lowerY = (float) (layout.lowerY() - cameraY - floorDrop);
        for (int side = 0; side < layout.quadCount(); side++) {
            int next = (side + 1) % layout.quadCount();
            float startX = layout.relativeX(side, cameraX);
            float startZ = layout.relativeZ(side, cameraZ);
            float nextX = layout.relativeX(next, cameraX);
            float nextZ = layout.relativeZ(next, cameraZ);
            batch.addQuad(
                    MESH,
                    startX, upperY, startZ, layout.uAt(side), topV,
                    nextX, upperY, nextZ, layout.uAt(side + 1), topV,
                    nextX, lowerY, nextZ, layout.uAt(side + 1), bottomV,
                    startX, lowerY, startZ, layout.uAt(side), bottomV,
                    layout.outwardX(side), 0.0f, layout.outwardZ(side));
        }
    }
}
