package com.bslx.XueXiMod.client;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import com.bslx.XueXiMod.XueXiMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

/** 已经减掉展示枢轴的三层水环。读入后缓存，不在每帧解析。 */
public final class WaterRingV8Mesh {
    static final ResourceLocation LOCATION = ResourceLocation.fromNamespaceAndPath(
            XueXiMod.MOD_ID, "waterring/water_ring_v8_mesh.json");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile WaterRingV8Mesh cached;
    private static volatile boolean broken;

    public final double tiltDegrees;
    public final Layer[] layers;

    private WaterRingV8Mesh(double tiltDegrees, Layer[] layers) {
        this.tiltDegrees = tiltDegrees;
        this.layers = layers;
    }

    public static WaterRingV8Mesh current() {
        WaterRingV8Mesh loaded = cached;
        if (loaded != null || broken) {
            return loaded;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return null;
        }
        publish(minecraft.getResourceManager());
        return cached;
    }

    public static void publish(ResourceManager resourceManager) {
        try {
            cached = load(resourceManager);
            broken = false;
        } catch (Exception failure) {
            broken = cached == null;
            LOGGER.error("周身水环网格读取失败", failure);
        }
    }

    private static WaterRingV8Mesh load(ResourceManager resourceManager) throws IOException {
        Optional<Resource> opened = resourceManager.getResource(LOCATION);
        if (opened.isEmpty()) {
            throw new IOException("缺少周身水环网格");
        }
        try (Reader reader = new InputStreamReader(opened.get().open(), StandardCharsets.UTF_8)) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        }
    }

    private static WaterRingV8Mesh parse(JsonObject document) {
        if (document.get("format").getAsInt() != 1) {
            throw new IllegalStateException("不认识的水环网格格式");
        }
        double tiltDegrees = document.get("tiltDegrees").getAsDouble();
        JsonArray layerArray = document.getAsJsonArray("layers");
        if (layerArray.size() != 3) {
            throw new IllegalStateException("水环网格不是三层");
        }
        Layer[] layers = new Layer[3];
        for (int layerIndex = 0; layerIndex < 3; layerIndex++) {
            layers[layerIndex] = readLayer(layerArray.get(layerIndex).getAsJsonObject());
        }
        if (layers[0].quadCount != 576 || layers[1].quadCount != 96 || layers[2].quadCount != 96) {
            throw new IllegalStateException("水环网格面数不是 576+96+96");
        }
        return new WaterRingV8Mesh(tiltDegrees, layers);
    }

    private static Layer readLayer(JsonObject layerObject) {
        JsonArray quads = layerObject.getAsJsonArray("quads");
        int quadCount = quads.size();
        if (layerObject.get("quadCount").getAsInt() != quadCount) {
            throw new IllegalStateException("水环网格面数不一致");
        }
        int cornerCount = quadCount * 4;
        float[] localX = new float[cornerCount];
        float[] localY = new float[cornerCount];
        float[] localZ = new float[cornerCount];
        float[] sourceU = new float[cornerCount];
        float[] sourceV = new float[cornerCount];
        int cursor = 0;
        for (int quadIndex = 0; quadIndex < quadCount; quadIndex++) {
            JsonArray quad = quads.get(quadIndex).getAsJsonArray();
            if (quad.size() != 4) {
                throw new IllegalStateException("水环网格面不是四边形");
            }
            for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
                JsonArray corner = quad.get(vertexIndex).getAsJsonArray();
                localX[cursor] = corner.get(0).getAsFloat();
                localY[cursor] = corner.get(1).getAsFloat();
                localZ[cursor] = corner.get(2).getAsFloat();
                sourceU[cursor] = corner.get(3).getAsFloat();
                sourceV[cursor] = corner.get(4).getAsFloat();
                cursor++;
            }
        }
        return new Layer(layerObject.get("name").getAsString(), layerObject.get("atlasOriginX").getAsInt(),
                layerObject.get("frameHeight").getAsInt(), quadCount, localX, localY, localZ, sourceU, sourceV);
    }

    public static final class Layer {
        public final String name;
        public final int atlasOriginX;
        public final int frameHeight;
        public final int quadCount;
        public final float[] localX;
        public final float[] localY;
        public final float[] localZ;
        public final float[] sourceU;
        public final float[] sourceV;

        private Layer(String name, int atlasOriginX, int frameHeight, int quadCount,
                       float[] localX, float[] localY, float[] localZ, float[] sourceU, float[] sourceV) {
            this.name = name;
            this.atlasOriginX = atlasOriginX;
            this.frameHeight = frameHeight;
            this.quadCount = quadCount;
            this.localX = localX;
            this.localY = localY;
            this.localZ = localZ;
            this.sourceU = sourceU;
            this.sourceV = sourceV;
        }
    }
}
