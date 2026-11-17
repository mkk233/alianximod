package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;
import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * 确认版六个方体、48 片和 20 滴的只读网格。
 * 模型 1 单位等于原版 ModelPart 的 1 像素，也就是 1/16 格；脚底对应实体脚底。
 * 文件在资源重载时读一次。六个方体只保留贴图上不透明的纹素，每块纹素单独成面。
 * 绘制时只插值已经缓存的关键帧。
 */
public final class SteveWaterLineMesh {
    static final ResourceLocation LOCATION = ResourceLocation.fromNamespaceAndPath(
            XueXiMod.MOD_ID, "models/effect/water_phantom_steve_v21/lines.bin");
    private static final ResourceLocation SKIN_LOCATION = ResourceLocation.fromNamespaceAndPath(
            XueXiMod.MOD_ID, "textures/entity/xueximod/steve_water_lines.png");
    private static final int MAGIC = 0x57505331;
    private static final int SKIN = 0;
    private static final int FLAKES = 1;
    private static final float HIDDEN_SCALE = 0.015F;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float[] POSITION_SAMPLE = new float[3];
    private static final float[] ROTATION_SAMPLE = new float[3];
    private static final float[] SCALE_SAMPLE = new float[3];
    private static final SteveWaterLineMesh EMPTY = new SteveWaterLineMesh(new Piece[0]);
    private static volatile SteveWaterLineMesh cached;

    private final Piece[] pieces;

    private SteveWaterLineMesh(Piece[] pieces) {
        this.pieces = pieces;
    }

    static SteveWaterLineMesh current() {
        SteveWaterLineMesh held = cached;
        if (held != null) {
            return held;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return EMPTY;
        }
        publish(minecraft.getResourceManager());
        held = cached;
        return held == null ? EMPTY : held;
    }

    static void publish(ResourceManager manager) {
        if (manager == null) {
            cached = EMPTY;
            return;
        }
        Optional<Resource> resource = manager.getResource(LOCATION);
        if (resource.isEmpty()) {
            LOGGER.error("水线网格不在资源包里 {}", LOCATION);
            cached = EMPTY;
            return;
        }
        try (InputStream stream = resource.get().open();
             DataInputStream input = new DataInputStream(new BufferedInputStream(stream))) {
            cached = decode(input).withOpaqueSkin(manager);
        } catch (IOException failure) {
            LOGGER.error("水线网格没有读入 {}", LOCATION, failure);
            cached = EMPTY;
        }
    }

    boolean isEmpty() {
        return this.pieces.length == 0;
    }

    void drawSkin(PoseStack pose, VertexConsumer consumer, float timeSeconds) {
        drawKind(pose, consumer, SKIN, timeSeconds);
    }

    void drawFlakes(PoseStack pose, VertexConsumer consumer, float timeSeconds) {
        drawKind(pose, consumer, FLAKES, timeSeconds);
    }

    private void drawKind(PoseStack pose, VertexConsumer consumer, int textureId, float timeSeconds) {
        if (pose == null || consumer == null || !Float.isFinite(timeSeconds)) {
            return;
        }
        for (Piece piece : this.pieces) {
            if (piece.texture != textureId) {
                continue;
            }
            sample(piece.scaleKeys, timeSeconds, SCALE_SAMPLE, 1.0F, 1.0F, 1.0F);
            if (hidden(SCALE_SAMPLE[0]) && hidden(SCALE_SAMPLE[1]) && hidden(SCALE_SAMPLE[2])) {
                continue;
            }
            sample(piece.positionKeys, timeSeconds, POSITION_SAMPLE, 0.0F, 0.0F, 0.0F);
            sample(piece.rotationKeys, timeSeconds, ROTATION_SAMPLE, 0.0F, 0.0F, 0.0F);
            pose.pushPose();
            pose.translate(piece.pivotX + POSITION_SAMPLE[0], piece.pivotY + POSITION_SAMPLE[1],
                    piece.pivotZ + POSITION_SAMPLE[2]);
            if (ROTATION_SAMPLE[2] != 0.0F) {
                pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(ROTATION_SAMPLE[2]));
            }
            if (ROTATION_SAMPLE[1] != 0.0F) {
                pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(ROTATION_SAMPLE[1]));
            }
            if (ROTATION_SAMPLE[0] != 0.0F) {
                pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(ROTATION_SAMPLE[0]));
            }
            pose.scale(SCALE_SAMPLE[0], SCALE_SAMPLE[1], SCALE_SAMPLE[2]);
            Matrix4f matrix = pose.last().pose();
            for (int quadCursor = 0; quadCursor < piece.quadCount; quadCursor++) {
                for (int vertexCursor = 0; vertexCursor < 4; vertexCursor++) {
                    int vertexBase = (quadCursor * 4 + vertexCursor) * 5;
                    consumer.vertex(matrix, piece.vertices[vertexBase], piece.vertices[vertexBase + 1],
                                    piece.vertices[vertexBase + 2])
                            .color(255, 255, 255, 255)
                            .uv(piece.vertices[vertexBase + 3], piece.vertices[vertexBase + 4])
                            .endVertex();
                }
            }
            pose.popPose();
        }
    }

    private SteveWaterLineMesh withOpaqueSkin(ResourceManager manager) {
        Optional<Resource> resource = manager.getResource(SKIN_LOCATION);
        if (resource.isEmpty()) {
            LOGGER.error("水线贴图不在资源包里 {}", SKIN_LOCATION);
            return this;
        }
        try (InputStream stream = resource.get().open();
             NativeImage skinImage = NativeImage.read(stream)) {
            Piece[] expanded = new Piece[this.pieces.length];
            for (int pieceCursor = 0; pieceCursor < this.pieces.length; pieceCursor++) {
                expanded[pieceCursor] = expandSkinPiece(this.pieces[pieceCursor], skinImage);
            }
            return new SteveWaterLineMesh(expanded);
        } catch (IOException failure) {
            LOGGER.error("水线贴图没有读入 {}", SKIN_LOCATION, failure);
            return this;
        }
    }

    private static Piece expandSkinPiece(Piece piece, NativeImage skinImage) {
        if (piece.texture != SKIN || skinImage.getWidth() < 1 || skinImage.getHeight() < 1) {
            return piece;
        }
        int opaqueQuads = 0;
        for (int quadCursor = 0; quadCursor < piece.quadCount; quadCursor++) {
            opaqueQuads += coverSkinQuad(piece, quadCursor, skinImage, null, null, 0);
        }
        if (opaqueQuads < 1) {
            return piece;
        }
        float[] vertices = new float[opaqueQuads * 20];
        float[] normals = new float[opaqueQuads * 3];
        int writeAt = 0;
        for (int quadCursor = 0; quadCursor < piece.quadCount; quadCursor++) {
            writeAt = coverSkinQuad(piece, quadCursor, skinImage, vertices, normals, writeAt);
        }
        return piece.replaceQuads(writeAt, normals, vertices);
    }

    private static int coverSkinQuad(Piece piece, int quadCursor, NativeImage skinImage, float[] vertices,
                                     float[] normals, int writeAt) {
        int imageWidth = skinImage.getWidth();
        int imageHeight = skinImage.getHeight();
        int origin = quadCursor * 20;
        float originU = piece.vertices[origin + 3];
        float originV = piece.vertices[origin + 4];
        float acrossU = piece.vertices[origin + 8] - originU;
        float acrossV = piece.vertices[origin + 9] - originV;
        float downU = piece.vertices[origin + 18] - originU;
        float downV = piece.vertices[origin + 19] - originV;
        float determinant = acrossU * downV - acrossV * downU;
        if (determinant > -1.0E-6F && determinant < 1.0E-6F) {
            return copySourceQuad(piece, quadCursor, vertices, normals, writeAt);
        }
        float lowerU = originU;
        float upperU = originU;
        float lowerV = originV;
        float upperV = originV;
        for (int vertexCursor = 1; vertexCursor < 4; vertexCursor++) {
            float cornerU = piece.vertices[origin + vertexCursor * 5 + 3];
            float cornerV = piece.vertices[origin + vertexCursor * 5 + 4];
            if (cornerU < lowerU) {
                lowerU = cornerU;
            }
            if (cornerU > upperU) {
                upperU = cornerU;
            }
            if (cornerV < lowerV) {
                lowerV = cornerV;
            }
            if (cornerV > upperV) {
                upperV = cornerV;
            }
        }
        int startColumn = (int) Math.floor(lowerU * imageWidth + 0.0001F);
        int endColumn = (int) Math.floor(upperU * imageWidth - 0.0001F) + 1;
        int startRow = (int) Math.floor(lowerV * imageHeight + 0.0001F);
        int endRow = (int) Math.floor(upperV * imageHeight - 0.0001F) + 1;
        if (startColumn < 0) {
            startColumn = 0;
        }
        if (startRow < 0) {
            startRow = 0;
        }
        if (endColumn > imageWidth) {
            endColumn = imageWidth;
        }
        if (endRow > imageHeight) {
            endRow = imageHeight;
        }
        float originX = piece.vertices[origin];
        float originY = piece.vertices[origin + 1];
        float originZ = piece.vertices[origin + 2];
        float acrossX = piece.vertices[origin + 5] - originX;
        float acrossY = piece.vertices[origin + 6] - originY;
        float acrossZ = piece.vertices[origin + 7] - originZ;
        float downX = piece.vertices[origin + 15] - originX;
        float downY = piece.vertices[origin + 16] - originY;
        float downZ = piece.vertices[origin + 17] - originZ;
        int written = writeAt;
        for (int row = startRow; row < endRow; row++) {
            for (int column = startColumn; column < endColumn; column++) {
                int pixel = skinImage.getPixelRGBA(column, row);
                int alpha = (pixel >>> 24) & 255;
                if (alpha == 0) {
                    continue;
                }
                if (vertices != null) {
                    writeTexel(vertices, normals, written, piece, quadCursor, imageWidth, imageHeight, column, row,
                            originU, originV, acrossU, acrossV, downU, downV, determinant,
                            originX, originY, originZ, acrossX, acrossY, acrossZ, downX, downY, downZ);
                }
                written++;
            }
        }
        return vertices == null ? written - writeAt : written;
    }

    private static int copySourceQuad(Piece piece, int quadCursor, float[] vertices, float[] normals, int writeAt) {
        if (vertices == null) {
            return 1;
        }
        int source = quadCursor * 20;
        int target = writeAt * 20;
        for (int offset = 0; offset < 20; offset++) {
            vertices[target + offset] = piece.vertices[source + offset];
        }
        int normalSource = quadCursor * 3;
        int normalTarget = writeAt * 3;
        normals[normalTarget] = piece.normals[normalSource];
        normals[normalTarget + 1] = piece.normals[normalSource + 1];
        normals[normalTarget + 2] = piece.normals[normalSource + 2];
        return writeAt + 1;
    }

    private static void writeTexel(float[] vertices, float[] normals, int writeAt, Piece piece, int quadCursor,
                                   int imageWidth, int imageHeight, int column, int row,
                                   float originU, float originV, float acrossU, float acrossV, float downU, float downV,
                                   float determinant, float originX, float originY, float originZ,
                                   float acrossX, float acrossY, float acrossZ, float downX, float downY, float downZ) {
        float leftU = column / (float) imageWidth;
        float rightU = (column + 1) / (float) imageWidth;
        float topV = row / (float) imageHeight;
        float bottomV = (row + 1) / (float) imageHeight;
        float centerU = (column + 0.5F) / imageWidth;
        float centerV = (row + 0.5F) / imageHeight;
        int target = writeAt * 20;
        writeCorner(vertices, target, leftU, topV, centerU, centerV, originU, originV, acrossU, acrossV, downU, downV,
                determinant, originX, originY, originZ, acrossX, acrossY, acrossZ, downX, downY, downZ);
        writeCorner(vertices, target + 5, rightU, topV, centerU, centerV, originU, originV, acrossU, acrossV, downU,
                downV, determinant, originX, originY, originZ, acrossX, acrossY, acrossZ, downX, downY, downZ);
        writeCorner(vertices, target + 10, rightU, bottomV, centerU, centerV, originU, originV, acrossU, acrossV, downU,
                downV, determinant, originX, originY, originZ, acrossX, acrossY, acrossZ, downX, downY, downZ);
        writeCorner(vertices, target + 15, leftU, bottomV, centerU, centerV, originU, originV, acrossU, acrossV, downU,
                downV, determinant, originX, originY, originZ, acrossX, acrossY, acrossZ, downX, downY, downZ);
        int normalSource = quadCursor * 3;
        int normalTarget = writeAt * 3;
        normals[normalTarget] = piece.normals[normalSource];
        normals[normalTarget + 1] = piece.normals[normalSource + 1];
        normals[normalTarget + 2] = piece.normals[normalSource + 2];
    }

    private static void writeCorner(float[] vertices, int target, float cornerU, float cornerV, float centerU,
                                    float centerV, float originU, float originV, float acrossU, float acrossV,
                                    float downU, float downV, float determinant, float originX, float originY,
                                    float originZ, float acrossX, float acrossY, float acrossZ, float downX,
                                    float downY, float downZ) {
        float relativeU = cornerU - originU;
        float relativeV = cornerV - originV;
        float alongAcross = (relativeU * downV - relativeV * downU) / determinant;
        float alongDown = (acrossU * relativeV - acrossV * relativeU) / determinant;
        vertices[target] = originX + acrossX * alongAcross + downX * alongDown;
        vertices[target + 1] = originY + acrossY * alongAcross + downY * alongDown;
        vertices[target + 2] = originZ + acrossZ * alongAcross + downZ * alongDown;
        vertices[target + 3] = centerU;
        vertices[target + 4] = centerV;
    }

    private static boolean hidden(float scale) {
        float magnitude = scale < 0.0F ? -scale : scale;
        return magnitude < HIDDEN_SCALE;
    }

    private static void sample(float[] keys, float timeSeconds, float[] into, float fallbackX, float fallbackY,
                               float fallbackZ) {
        int keyCount = keys.length / 5;
        if (keyCount <= 0) {
            into[0] = fallbackX;
            into[1] = fallbackY;
            into[2] = fallbackZ;
            return;
        }
        int chosen = 0;
        for (int cursor = 0; cursor < keyCount; cursor++) {
            if (keys[cursor * 5] <= timeSeconds) {
                chosen = cursor;
            } else {
                break;
            }
        }
        int base = chosen * 5;
        int following = chosen + 1;
        boolean hold = keys[base + 4] < 0.5F || following >= keyCount;
        if (!hold) {
            int next = following * 5;
            float span = keys[next] - keys[base];
            if (span == 0.0F) {
                hold = true;
            } else {
                float weight = (timeSeconds - keys[base]) / span;
                if (weight < 0.0F) {
                    weight = 0.0F;
                } else if (weight > 1.0F) {
                    weight = 1.0F;
                }
                into[0] = keys[base + 1] + (keys[next + 1] - keys[base + 1]) * weight;
                into[1] = keys[base + 2] + (keys[next + 2] - keys[base + 2]) * weight;
                into[2] = keys[base + 3] + (keys[next + 3] - keys[base + 3]) * weight;
                return;
            }
        }
        into[0] = keys[base + 1];
        into[1] = keys[base + 2];
        into[2] = keys[base + 3];
    }

    private static SteveWaterLineMesh decode(DataInputStream input) throws IOException {
        if (input.readInt() != MAGIC) {
            throw new IOException("水线网格头不匹配");
        }
        int pieceCount = input.readUnsignedShort();
        if (pieceCount < 1 || pieceCount > 128) {
            throw new IOException("水线部件数量无效");
        }
        Piece[] pieces = new Piece[pieceCount];
        for (int pieceCursor = 0; pieceCursor < pieceCount; pieceCursor++) {
            int texture = input.readUnsignedByte();
            if (texture != SKIN && texture != FLAKES) {
                throw new IOException("水线贴图编号无效");
            }
            float pivotX = readFinite(input);
            float pivotY = readFinite(input);
            float pivotZ = readFinite(input);
            int quadCount = input.readUnsignedShort();
            if (quadCount < 1 || quadCount > 6) {
                throw new IOException("水线面数无效");
            }
            float[] normals = new float[quadCount * 3];
            float[] vertices = new float[quadCount * 20];
            for (int quadCursor = 0; quadCursor < quadCount; quadCursor++) {
                int normalBase = quadCursor * 3;
                normals[normalBase] = readFinite(input);
                normals[normalBase + 1] = readFinite(input);
                normals[normalBase + 2] = readFinite(input);
                for (int vertexCursor = 0; vertexCursor < 4; vertexCursor++) {
                    int vertexBase = (quadCursor * 4 + vertexCursor) * 5;
                    vertices[vertexBase] = readFinite(input);
                    vertices[vertexBase + 1] = readFinite(input);
                    vertices[vertexBase + 2] = readFinite(input);
                    vertices[vertexBase + 3] = readFinite(input);
                    vertices[vertexBase + 4] = readFinite(input);
                }
            }
            float[] positionKeys = readKeys(input);
            float[] rotationKeys = readKeys(input);
            float[] scaleKeys = readKeys(input);
            pieces[pieceCursor] = new Piece(texture, pivotX, pivotY, pivotZ, quadCount, normals, vertices,
                    positionKeys, rotationKeys, scaleKeys);
        }
        if (input.read() != -1) {
            throw new IOException("水线网格尾部多余");
        }
        return new SteveWaterLineMesh(pieces);
    }

    private static float[] readKeys(DataInputStream input) throws IOException {
        int keyCount = input.readUnsignedShort();
        if (keyCount > 64) {
            throw new IOException("水线关键帧过多");
        }
        float[] packed = new float[keyCount * 5];
        for (int cursor = 0; cursor < keyCount; cursor++) {
            int offset = cursor * 5;
            packed[offset] = readFinite(input);
            packed[offset + 1] = readFinite(input);
            packed[offset + 2] = readFinite(input);
            packed[offset + 3] = readFinite(input);
            int mode = input.readUnsignedByte();
            if (mode > 1) {
                throw new IOException("水线插值无效");
            }
            packed[offset + 4] = mode;
        }
        return packed;
    }

    private static float readFinite(DataInputStream input) throws IOException {
        float sample = input.readFloat();
        if (!Float.isFinite(sample)) {
            throw new IOException("水线网格含有非有限数");
        }
        return sample;
    }

    private static final class Piece {
        private final int texture;
        private final float pivotX;
        private final float pivotY;
        private final float pivotZ;
        private final int quadCount;
        private final float[] normals;
        private final float[] vertices;
        private final float[] positionKeys;
        private final float[] rotationKeys;
        private final float[] scaleKeys;

        private Piece(int texture, float pivotX, float pivotY, float pivotZ, int quadCount, float[] normals,
                      float[] vertices, float[] positionKeys, float[] rotationKeys, float[] scaleKeys) {
            this.texture = texture;
            this.pivotX = pivotX;
            this.pivotY = pivotY;
            this.pivotZ = pivotZ;
            this.quadCount = quadCount;
            this.normals = normals;
            this.vertices = vertices;
            this.positionKeys = positionKeys;
            this.rotationKeys = rotationKeys;
            this.scaleKeys = scaleKeys;
        }

        private Piece replaceQuads(int quadCount, float[] normals, float[] vertices) {
            return new Piece(this.texture, this.pivotX, this.pivotY, this.pivotZ, quadCount, normals, vertices,
                    this.positionKeys, this.rotationKeys, this.scaleKeys);
        }
    }
}
