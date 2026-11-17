package com.bslx.XueXiMod.client.furnace;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.client.NetherBlueFlame;
import com.bslx.XueXiMod.client.furnace.FurnaceBodyVisual.Playback;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;

/**
 * 已确认 burn-v3-spatial-v1 的特效网格。顶点是玩家模型局部单位，提交时除以 16，与 1.20.1 立方体一致。
 * 每个面只提交一次。示意人偶和持物杆不在这份数据里。
 */
public final class BurnSpatialMount {
    public static final float MODEL_TO_BLOCK = 0.0625F;
    private static final ResourceLocation MOUNT = new ResourceLocation(
            XueXiMod.MOD_ID, "models/effect/burn_spatial_v1/mount.json");
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    private static MountData loaded;
    private static boolean loadFailed;

    private BurnSpatialMount() {
    }

    public static void draw(PoseStack poseStack, MultiBufferSource bufferSource, Playback playback, String boneName) {
        if (playback == null || !playback.present() || boneName == null) {
            return;
        }
        MountData mount = mount();
        if (mount == null) {
            return;
        }
        List<MeshDraw> meshes = mount.bones.get(boneName);
        if (meshes == null || meshes.isEmpty()) {
            return;
        }
        int frameCount = playback.frameCount();
        float frameShift = playback.frameIndex();
        boolean entryActive = playback.entryActive();
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        for (MeshDraw mesh : meshes) {
            ResourceLocation ordinary = entryActive ? mesh.entryTexture : mesh.loopTexture;
            ResourceLocation blue = entryActive ? mesh.blueEntryTexture : mesh.blueLoopTexture;
            ResourceLocation chosen = playback.gloom() ? blue : NetherBlueFlame.current(ordinary, blue);
            VertexConsumer consumer = FurnaceBodyVisual.emissive(bufferSource, chosen);
            for (float[] face : mesh.faces) {
                submitFace(consumer, matrix, normalMatrix, face, frameShift, frameCount);
            }
        }
    }

    public static void translateGrip(PoseStack poseStack, boolean characterRight) {
        MountData mount = mount();
        if (mount == null) {
            return;
        }
        float[] anchor = characterRight ? mount.rightGrip : mount.leftGrip;
        poseStack.translate(-anchor[0] * MODEL_TO_BLOCK, -anchor[1] * MODEL_TO_BLOCK, -anchor[2] * MODEL_TO_BLOCK);
    }

    private static void submitFace(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix, float[] face,
                                   float frameShift, int frameCount) {
        float firstX = face[0] * MODEL_TO_BLOCK;
        float firstY = face[1] * MODEL_TO_BLOCK;
        float firstZ = face[2] * MODEL_TO_BLOCK;
        float secondX = face[5] * MODEL_TO_BLOCK;
        float secondY = face[6] * MODEL_TO_BLOCK;
        float secondZ = face[7] * MODEL_TO_BLOCK;
        float thirdX = face[10] * MODEL_TO_BLOCK;
        float thirdY = face[11] * MODEL_TO_BLOCK;
        float thirdZ = face[12] * MODEL_TO_BLOCK;
        float fourthX = face[15] * MODEL_TO_BLOCK;
        float fourthY = face[16] * MODEL_TO_BLOCK;
        float fourthZ = face[17] * MODEL_TO_BLOCK;
        float edgeOneX = secondX - firstX;
        float edgeOneY = secondY - firstY;
        float edgeOneZ = secondZ - firstZ;
        float edgeTwoX = thirdX - firstX;
        float edgeTwoY = thirdY - firstY;
        float edgeTwoZ = thirdZ - firstZ;
        float normalX = edgeOneY * edgeTwoZ - edgeOneZ * edgeTwoY;
        float normalY = edgeOneZ * edgeTwoX - edgeOneX * edgeTwoZ;
        float normalZ = edgeOneX * edgeTwoY - edgeOneY * edgeTwoX;
        float normalLength = Mth.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
        if (normalLength < 1.0E-6F) {
            normalX = 0.0F;
            normalY = 0.0F;
            normalZ = 1.0F;
        } else {
            normalX /= normalLength;
            normalY /= normalLength;
            normalZ /= normalLength;
        }
        float frameSpan = frameCount;
        putVertex(consumer, matrix, normalMatrix, firstX, firstY, firstZ, face[3],
                (frameShift + face[4]) / frameSpan, normalX, normalY, normalZ);
        putVertex(consumer, matrix, normalMatrix, secondX, secondY, secondZ, face[8],
                (frameShift + face[9]) / frameSpan, normalX, normalY, normalZ);
        putVertex(consumer, matrix, normalMatrix, thirdX, thirdY, thirdZ, face[13],
                (frameShift + face[14]) / frameSpan, normalX, normalY, normalZ);
        putVertex(consumer, matrix, normalMatrix, fourthX, fourthY, fourthZ, face[18],
                (frameShift + face[19]) / frameSpan, normalX, normalY, normalZ);
    }

    private static void putVertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix,
                                  float pointX, float pointY, float pointZ, float textureU, float textureV,
                                  float normalX, float normalY, float normalZ) {
        consumer.vertex(matrix, pointX, pointY, pointZ)
                .color(255, 255, 255, 255)
                .uv(textureU, textureV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normalMatrix, normalX, normalY, normalZ)
                .endVertex();
    }

    private static MountData mount() {
        if (loaded != null || loadFailed) {
            return loaded;
        }
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(MOUNT);
        if (resource.isEmpty()) {
            loadFailed = true;
            LOGGER.error("燃烧空间挂载数据缺失: {}", MOUNT);
            return null;
        }
        try (InputStream stream = resource.get().open();
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            loaded = parse(JsonParser.parseReader(reader).getAsJsonObject());
            return loaded;
        } catch (Exception failure) {
            loadFailed = true;
            LOGGER.error("燃烧空间挂载数据无法读取: {}", MOUNT, failure);
            return null;
        }
    }

    private static MountData parse(JsonObject root) {
        JsonObject anchorObject = root.getAsJsonObject("anchors");
        float[] rightGrip = readVector(anchorObject.getAsJsonArray("right_arm"));
        float[] leftGrip = readVector(anchorObject.getAsJsonArray("left_arm"));
        Map<String, List<MeshDraw>> bones = new LinkedHashMap<>();
        JsonObject boneObject = root.getAsJsonObject("bones");
        for (Map.Entry<String, JsonElement> boneEntry : boneObject.entrySet()) {
            List<MeshDraw> meshes = new ArrayList<>();
            for (JsonElement meshElement : boneEntry.getValue().getAsJsonArray()) {
                JsonObject meshObject = meshElement.getAsJsonObject();
                String textureKey = meshObject.get("texture").getAsString();
                List<float[]> faces = new ArrayList<>();
                float depthTotal = 0.0F;
                int depthSamples = 0;
                for (JsonElement faceElement : meshObject.getAsJsonArray("faces")) {
                    JsonArray cornerArray = faceElement.getAsJsonArray();
                    if (cornerArray.size() != 4) {
                        continue;
                    }
                    float[] face = new float[20];
                    for (int cornerCursor = 0; cornerCursor < 4; cornerCursor++) {
                        JsonArray corner = cornerArray.get(cornerCursor).getAsJsonArray();
                        int offset = cornerCursor * 5;
                        face[offset] = corner.get(0).getAsFloat();
                        face[offset + 1] = corner.get(1).getAsFloat();
                        face[offset + 2] = corner.get(2).getAsFloat();
                        face[offset + 3] = corner.get(3).getAsFloat();
                        face[offset + 4] = corner.get(4).getAsFloat();
                        depthTotal += face[offset + 2];
                        depthSamples++;
                    }
                    faces.add(face);
                }
                float depthAverage = depthSamples == 0 ? 0.0F : depthTotal / depthSamples;
                meshes.add(new MeshDraw(textureKey, faces, depthAverage));
            }
            meshes.sort((leftMesh, rightMesh) -> Float.compare(rightMesh.depthAverage, leftMesh.depthAverage));
            bones.put(boneEntry.getKey(), meshes);
        }
        return new MountData(bones, rightGrip, leftGrip);
    }

    private static float[] readVector(JsonArray array) {
        return new float[] {array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
    }

    private static ResourceLocation sheet(String phase, String textureKey) {
        return new ResourceLocation(XueXiMod.MOD_ID,
                "textures/effect/burn_spatial_v1/" + phase + "/" + textureKey + ".png");
    }

    private static ResourceLocation blueSheet(String phase, String textureKey) {
        return NetherBlueFlame.sheet("burn_" + phase + "_" + textureKey + ".png");
    }

    private static final class MountData {
        private final Map<String, List<MeshDraw>> bones;
        private final float[] rightGrip;
        private final float[] leftGrip;

        private MountData(Map<String, List<MeshDraw>> bones, float[] rightGrip, float[] leftGrip) {
            this.bones = bones;
            this.rightGrip = rightGrip;
            this.leftGrip = leftGrip;
        }
    }

    private static final class MeshDraw {
        private final ResourceLocation loopTexture;
        private final ResourceLocation entryTexture;
        private final ResourceLocation blueLoopTexture;
        private final ResourceLocation blueEntryTexture;
        private final List<float[]> faces;
        private final float depthAverage;

        private MeshDraw(String textureKey, List<float[]> faces, float depthAverage) {
            this.loopTexture = sheet("loop", textureKey);
            this.entryTexture = sheet("entry", textureKey);
            this.blueLoopTexture = blueSheet("loop", textureKey);
            this.blueEntryTexture = blueSheet("entry", textureKey);
            this.faces = faces;
            this.depthAverage = depthAverage;
        }
    }
}
