package com.bslx.XueXiMod.client.action.skinned.attachments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.math.Vec3f;
import com.bslx.XueXiMod.client.action.mesh.MeshLimits;
import com.bslx.XueXiMod.client.action.mesh.PlayerMeshKind;
import com.bslx.XueXiMod.client.action.mesh.PlayerMeshPart;
import com.bslx.XueXiMod.client.action.mesh.PlayerSkinMesh;
import com.bslx.XueXiMod.client.action.mesh.SkinDiagnostic;
import com.bslx.XueXiMod.client.action.skinned.AttachmentTransform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * 用公开的 visit 和 Cube.compile 采集原版护甲静止几何。初值姿态只在采集期间套上，结束时恢复。
 * 每个四边形按关节过渡需要细分，UV 线性插值，法线保持该面的法线。点经 inverse(H) 进入源休息坐标后再称重。
 * 缓存键是几何、内层或外层、体型和资源代次，不含物品编号。
 */
final class ArmorMeshes {
    private static final Map<ArmorKey, CachedMesh> CACHE = new HashMap<>();
    private static final Map<InstanceKey, CachedMesh> INSTANCES = new HashMap<>();

    private ArmorMeshes() {
    }

    static void clearCache() {
        synchronized (CACHE) {
            CACHE.clear();
            INSTANCES.clear();
        }
    }

    static ArmorExtract extract(
            HumanoidModel<?> model,
            BindJointTable table,
            PlayerMeshKind kind,
            boolean innerLayer,
            long resourceGeneration) {
        if (model == null || table == null || kind == null) {
            throw new SkinDiagnostic("armor-model", "护甲模型或绑定缺失");
        }
        InstanceKey instanceKey = new InstanceKey(System.identityHashCode(model), innerLayer, kind, resourceGeneration);
        synchronized (CACHE) {
            CachedMesh cachedInstance = INSTANCES.get(instanceKey);
            if (cachedInstance != null) {
                return cachedInstance.extract(false);
            }
        }
        List<Quad> quads = new ArrayList<>();
        collect(model.head, ArmorRegion.HEAD, PlayerMeshPart.HEAD, quads);
        collect(model.hat, ArmorRegion.HEAD, PlayerMeshPart.HAT, quads);
        collect(model.body, ArmorRegion.TORSO, PlayerMeshPart.TORSO, quads);
        collect(model.rightArm, ArmorRegion.ARM_RIGHT, PlayerMeshPart.RIGHT_ARM, quads);
        collect(model.leftArm, ArmorRegion.ARM_LEFT, PlayerMeshPart.LEFT_ARM, quads);
        collect(model.rightLeg, ArmorRegion.LEG_RIGHT, PlayerMeshPart.RIGHT_LEG, quads);
        collect(model.leftLeg, ArmorRegion.LEG_LEFT, PlayerMeshPart.LEFT_LEG, quads);
        if (quads.isEmpty()) {
            throw new SkinDiagnostic("armor-empty", "护甲模型没有可采集的立方体");
        }
        long geometryHash = hashQuads(quads);
        int segments = chooseSplits(quads.size());
        ArmorKey key = new ArmorKey(geometryHash, innerLayer, kind, resourceGeneration, segments);
        synchronized (CACHE) {
            CachedMesh cached = CACHE.get(key);
            if (cached != null) {
                INSTANCES.put(instanceKey, cached);
                return cached.extract(false);
            }
        }
        ArmorExtract built = build(quads, table, kind, segments);
        CachedMesh stored = new CachedMesh(built);
        synchronized (CACHE) {
            CACHE.put(key, stored);
            INSTANCES.put(instanceKey, stored);
        }
        return built;
    }

    static int chooseSplits(int faceCount) {
        int segments = 4;
        while (segments >= 2) {
            long pointCount = (long) faceCount * (segments + 1L) * (segments + 1L);
            long triangleCount = (long) faceCount * segments * segments * 2L;
            long normalPairs = pointCount * Math.max(faceCount, 1);
            long influences = pointCount * 3L;
            if (pointCount <= MeshLimits.POSITION_LIMIT
                    && pointCount <= MeshLimits.TEXTURE_LIMIT
                    && triangleCount <= MeshLimits.TRIANGLE_LIMIT
                    && normalPairs <= MeshLimits.NORMAL_PAIR_LIMIT
                    && influences <= MeshLimits.INFLUENCE_LIMIT) {
                return segments;
            }
            segments--;
        }
        throw new SkinDiagnostic("normal-pairs", "护甲细分后的位置和法线组合超过网格上限");
    }

    private static void collect(ModelPart part, ArmorRegion region, PlayerMeshPart meshPart, List<Quad> quads) {
        if (part == null) {
            return;
        }
        List<PartRest.SavedPart> saved = PartRest.capture(part);
        try {
            PartRest.loadInitial(part);
            RestCollector collector = new RestCollector();
            part.visit(new PoseStack(), (pose, path, cubeIndex, cube) -> cube.compile(pose, collector, 0, 0, 1.0F, 1.0F, 1.0F, 1.0F));
            collector.flush(region, meshPart, quads);
        } finally {
            PartRest.restore(saved);
        }
    }

    private static ArmorExtract build(List<Quad> quads, BindJointTable table, PlayerMeshKind kind, int segments) {
        OpenMatrix4f sourceFromModel = OpenMatrix4f.invert(AttachmentTransform.sourceToBody(), null);
        float[] lowest = new float[ArmorRegion.values().length];
        float[] highest = new float[ArmorRegion.values().length];
        boolean[] seen = new boolean[ArmorRegion.values().length];
        for (Quad quad : quads) {
            int regionIndex = quad.region.ordinal();
            for (int cornerIndex = 0; cornerIndex < 4; cornerIndex++) {
                Vec3f source = OpenMatrix4f.transform3v(sourceFromModel, new Vec3f(quad.modelX[cornerIndex], quad.modelY[cornerIndex], quad.modelZ[cornerIndex]), null);
                quad.sourceX[cornerIndex] = source.coordinateX;
                quad.sourceY[cornerIndex] = source.coordinateY;
                quad.sourceZ[cornerIndex] = source.coordinateZ;
                float parameter = regionParameter(quad.region, table, source.coordinateX, source.coordinateY, source.coordinateZ);
                if (!seen[regionIndex]) {
                    lowest[regionIndex] = parameter;
                    highest[regionIndex] = parameter;
                    seen[regionIndex] = true;
                } else {
                    if (parameter < lowest[regionIndex]) {
                        lowest[regionIndex] = parameter;
                    }
                    if (parameter > highest[regionIndex]) {
                        highest[regionIndex] = parameter;
                    }
                }
            }
        }
        MeshDraft draft = new MeshDraft();
        float witnessX = 0.0F;
        float witnessY = 0.0F;
        float witnessZ = 0.0F;
        boolean witnessReady = false;
        for (Quad quad : quads) {
            int points = segments + 1;
            int[] grid = new int[points * points];
            for (int rowIndex = 0; rowIndex < points; rowIndex++) {
                for (int columnIndex = 0; columnIndex < points; columnIndex++) {
                    float across = columnIndex / (float) segments;
                    float down = rowIndex / (float) segments;
                    float weight0 = (1.0F - across) * (1.0F - down);
                    float weight1 = across * (1.0F - down);
                    float weight2 = across * down;
                    float weight3 = (1.0F - across) * down;
                    float sourceX = weight0 * quad.sourceX[0] + weight1 * quad.sourceX[1] + weight2 * quad.sourceX[2] + weight3 * quad.sourceX[3];
                    float sourceY = weight0 * quad.sourceY[0] + weight1 * quad.sourceY[1] + weight2 * quad.sourceY[2] + weight3 * quad.sourceY[3];
                    float sourceZ = weight0 * quad.sourceZ[0] + weight1 * quad.sourceZ[1] + weight2 * quad.sourceZ[2] + weight3 * quad.sourceZ[3];
                    float textureU = weight0 * quad.textureU[0] + weight1 * quad.textureU[1] + weight2 * quad.textureU[2] + weight3 * quad.textureU[3];
                    float textureV = weight0 * quad.textureV[0] + weight1 * quad.textureV[1] + weight2 * quad.textureV[2] + weight3 * quad.textureV[3];
                    ArmorWeights.Influences influences = ArmorWeights.weigh(
                            quad.region, sourceX, sourceY, sourceZ, table, lowest[quad.region.ordinal()], highest[quad.region.ordinal()]);
                    int positionIndex = draft.add(sourceX, sourceY, sourceZ, quad.normalX, quad.normalY, quad.normalZ, textureU, textureV, influences);
                    grid[rowIndex * points + columnIndex] = positionIndex;
                    if (!witnessReady && positionIndex == 0) {
                        witnessX = quad.modelX[0];
                        witnessY = quad.modelY[0];
                        witnessZ = quad.modelZ[0];
                        witnessReady = true;
                    }
                }
            }
            for (int rowIndex = 0; rowIndex < segments; rowIndex++) {
                for (int columnIndex = 0; columnIndex < segments; columnIndex++) {
                    int corner00 = grid[rowIndex * points + columnIndex];
                    int corner10 = grid[rowIndex * points + columnIndex + 1];
                    int corner11 = grid[(rowIndex + 1) * points + columnIndex + 1];
                    int corner01 = grid[(rowIndex + 1) * points + columnIndex];
                    draft.addTriangle(quad.meshPart, corner00, corner10, corner11);
                    draft.addTriangle(quad.meshPart, corner00, corner11, corner01);
                }
            }
        }
        PlayerSkinMesh mesh = draft.compose(kind, table.jointLimit());
        return new ArmorExtract(mesh, witnessX, witnessY, witnessZ, true);
    }

    private static float regionParameter(ArmorRegion region, BindJointTable table, float sourceX, float sourceY, float sourceZ) {
        if (region == ArmorRegion.HEAD) {
            return 0.0F;
        }
        if (region == ArmorRegion.TORSO) {
            return ArmorWeights.along(table, "Torso", "Chest", sourceX, sourceY, sourceZ);
        }
        if (region == ArmorRegion.ARM_RIGHT) {
            return ArmorWeights.along(table, "Arm_R", "Hand_R", sourceX, sourceY, sourceZ);
        }
        if (region == ArmorRegion.ARM_LEFT) {
            return ArmorWeights.along(table, "Arm_L", "Hand_L", sourceX, sourceY, sourceZ);
        }
        if (region == ArmorRegion.LEG_RIGHT) {
            return ArmorWeights.along(table, "Thigh_R", "Leg_R", sourceX, sourceY, sourceZ);
        }
        return ArmorWeights.along(table, "Thigh_L", "Leg_L", sourceX, sourceY, sourceZ);
    }

    private static long hashQuads(List<Quad> quads) {
        long hash = 0x9E3779B97F4A7C15L;
        for (Quad quad : quads) {
            hash = hash * 31L + quad.region.ordinal();
            hash = hash * 31L + quad.meshPart.ordinal();
            for (int cornerIndex = 0; cornerIndex < 4; cornerIndex++) {
                hash = hash * 31L + Float.floatToIntBits(quad.modelX[cornerIndex]);
                hash = hash * 31L + Float.floatToIntBits(quad.modelY[cornerIndex]);
                hash = hash * 31L + Float.floatToIntBits(quad.modelZ[cornerIndex]);
                hash = hash * 31L + Float.floatToIntBits(quad.textureU[cornerIndex]);
                hash = hash * 31L + Float.floatToIntBits(quad.textureV[cornerIndex]);
            }
        }
        return hash;
    }

    static final class ArmorExtract {
        final PlayerSkinMesh mesh;
        final float witnessModelX;
        final float witnessModelY;
        final float witnessModelZ;
        final boolean fresh;

        ArmorExtract(PlayerSkinMesh mesh, float witnessModelX, float witnessModelY, float witnessModelZ, boolean fresh) {
            this.mesh = mesh;
            this.witnessModelX = witnessModelX;
            this.witnessModelY = witnessModelY;
            this.witnessModelZ = witnessModelZ;
            this.fresh = fresh;
        }
    }

    private static final class Quad {
        final ArmorRegion region;
        final PlayerMeshPart meshPart;
        final float[] modelX = new float[4];
        final float[] modelY = new float[4];
        final float[] modelZ = new float[4];
        final float[] sourceX = new float[4];
        final float[] sourceY = new float[4];
        final float[] sourceZ = new float[4];
        final float[] textureU = new float[4];
        final float[] textureV = new float[4];
        final float normalX;
        final float normalY;
        final float normalZ;

        Quad(ArmorRegion region, PlayerMeshPart meshPart, Corner[] corners) {
            this.region = region;
            this.meshPart = meshPart;
            for (int cornerIndex = 0; cornerIndex < 4; cornerIndex++) {
                this.modelX[cornerIndex] = corners[cornerIndex].modelX;
                this.modelY[cornerIndex] = corners[cornerIndex].modelY;
                this.modelZ[cornerIndex] = corners[cornerIndex].modelZ;
                this.textureU[cornerIndex] = corners[cornerIndex].textureU;
                this.textureV[cornerIndex] = corners[cornerIndex].textureV;
            }
            float normalX = corners[0].normalX;
            float normalY = corners[0].normalY;
            float normalZ = corners[0].normalZ;
            float lengthSquared = normalX * normalX + normalY * normalY + normalZ * normalZ;
            if (lengthSquared < 1.0E-8F) {
                this.normalX = 0.0F;
                this.normalY = 1.0F;
                this.normalZ = 0.0F;
                return;
            }
            float inverse = 1.0F / (float) Math.sqrt(lengthSquared);
            this.normalX = -normalX * inverse;
            this.normalY = -normalY * inverse;
            this.normalZ = normalZ * inverse;
        }
    }

    private static final class Corner {
        float modelX;
        float modelY;
        float modelZ;
        float textureU;
        float textureV;
        float normalX;
        float normalY;
        float normalZ;
    }

    private static final class RestCollector implements VertexConsumer {
        private final List<Corner> corners = new ArrayList<>();
        private float modelX;
        private float modelY;
        private float modelZ;
        private float textureU;
        private float textureV;
        private float normalX;
        private float normalY;
        private float normalZ;

        void flush(ArmorRegion region, PlayerMeshPart meshPart, List<Quad> quads) {
            if (this.corners.size() % 4 != 0) {
                throw new SkinDiagnostic("armor-polygon", "护甲立方体面不是四边形");
            }
            for (int cornerIndex = 0; cornerIndex < this.corners.size(); cornerIndex += 4) {
                Corner[] face = new Corner[] {
                        this.corners.get(cornerIndex),
                        this.corners.get(cornerIndex + 1),
                        this.corners.get(cornerIndex + 2),
                        this.corners.get(cornerIndex + 3)
                };
                quads.add(new Quad(region, meshPart, face));
            }
        }

        @Override
        public VertexConsumer vertex(double pointX, double pointY, double pointZ) {
            this.modelX = (float) pointX;
            this.modelY = (float) pointY;
            this.modelZ = (float) pointZ;
            return this;
        }

        @Override
        public VertexConsumer color(int redAmount, int greenAmount, int blueAmount, int alphaAmount) {
            return this;
        }

        @Override
        public void defaultColor(int redAmount, int greenAmount, int blueAmount, int alphaAmount) {
        }

        @Override
        public void unsetDefaultColor() {
        }

        @Override
        public VertexConsumer uv(float textureU, float textureV) {
            this.textureU = textureU;
            this.textureV = textureV;
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int overlayU, int overlayV) {
            return this;
        }

        @Override
        public VertexConsumer uv2(int lightU, int lightV) {
            return this;
        }

        @Override
        public VertexConsumer normal(float normalX, float normalY, float normalZ) {
            this.normalX = normalX;
            this.normalY = normalY;
            this.normalZ = normalZ;
            return this;
        }

        @Override
        public void endVertex() {
            Corner corner = new Corner();
            corner.modelX = this.modelX;
            corner.modelY = this.modelY;
            corner.modelZ = this.modelZ;
            corner.textureU = this.textureU;
            corner.textureV = this.textureV;
            corner.normalX = this.normalX;
            corner.normalY = this.normalY;
            corner.normalZ = this.normalZ;
            this.corners.add(corner);
        }
    }

    /** 默认颜色不增加角点，位置、UV 和法线仍只在 endVertex 提交一次。 */
    static float[] colorProbeSample() {
        RestCollector collector = new RestCollector();
        collector.defaultColor(4, 5, 6, 7);
        collector.unsetDefaultColor();
        collector.vertex(1.0, 2.0, 3.0);
        collector.color(8, 9, 10, 11);
        collector.uv(0.25F, 0.75F);
        collector.overlayCoords(1, 2);
        collector.uv2(3, 4);
        collector.normal(0.0F, 1.0F, 0.0F);
        collector.defaultColor(1, 1, 1, 1);
        collector.endVertex();
        collector.unsetDefaultColor();
        if (collector.corners.isEmpty()) {
            return new float[] {0.0F, 0.0F, 0.0F, 0.0F};
        }
        Corner corner = collector.corners.get(0);
        return new float[] {collector.corners.size(), corner.textureU, corner.textureV, corner.normalY};
    }

    private static final class MeshDraft {
        private final List<Float> positions = new ArrayList<>();
        private final List<Float> normals = new ArrayList<>();
        private final List<Float> textures = new ArrayList<>();
        private final List<Float> weights = new ArrayList<>();
        private final List<Integer> influenceCounts = new ArrayList<>();
        private final List<Integer> influenceJoints = new ArrayList<>();
        private final List<Integer> influenceWeightSlots = new ArrayList<>();
        private final List<Integer> normalByPosition = new ArrayList<>();
        private final Map<Long, Integer> normalKeys = new HashMap<>();
        private final List<Integer>[] partCorners = partLists();

        int add(
                float sourceX,
                float sourceY,
                float sourceZ,
                float normalX,
                float normalY,
                float normalZ,
                float textureU,
                float textureV,
                ArmorWeights.Influences influences) {
            int positionIndex = this.positions.size() / 3;
            this.positions.add(sourceX);
            this.positions.add(sourceY);
            this.positions.add(sourceZ);
            this.textures.add(textureU);
            this.textures.add(textureV);
            this.normalByPosition.add(this.normalIndex(normalX, normalY, normalZ));
            this.influenceCounts.add(influences.jointIds.length);
            for (int influenceIndex = 0; influenceIndex < influences.jointIds.length; influenceIndex++) {
                this.influenceJoints.add(influences.jointIds[influenceIndex]);
                this.influenceWeightSlots.add(this.weights.size());
                this.weights.add(influences.weights[influenceIndex]);
            }
            return positionIndex;
        }

        void addTriangle(PlayerMeshPart meshPart, int first, int second, int third) {
            this.writeCorner(meshPart, first);
            this.writeCorner(meshPart, second);
            this.writeCorner(meshPart, third);
        }

        PlayerSkinMesh compose(PlayerMeshKind kind, int jointLimit) {
            int[][] corners = new int[PlayerMeshPart.values().length][];
            for (PlayerMeshPart meshPart : PlayerMeshPart.values()) {
                List<Integer> stored = this.partCorners[meshPart.ordinal()];
                if (stored.isEmpty()) {
                    continue;
                }
                int[] packed = new int[stored.size()];
                for (int cornerIndex = 0; cornerIndex < stored.size(); cornerIndex++) {
                    packed[cornerIndex] = stored.get(cornerIndex);
                }
                corners[meshPart.ordinal()] = packed;
            }
            return PlayerSkinMesh.compose(
                    kind,
                    jointLimit,
                    floats(this.positions),
                    floats(this.normals),
                    floats(this.textures),
                    floats(this.weights),
                    ints(this.influenceCounts),
                    ints(this.influenceJoints),
                    ints(this.influenceWeightSlots),
                    corners,
                    false,
                    "vanilla-armor",
                    "",
                    false);
        }

        private void writeCorner(PlayerMeshPart meshPart, int positionIndex) {
            this.partCorners[meshPart.ordinal()].add(positionIndex);
            this.partCorners[meshPart.ordinal()].add(positionIndex);
            this.partCorners[meshPart.ordinal()].add(this.normalByPosition.get(positionIndex));
        }

        private int normalIndex(float normalX, float normalY, float normalZ) {
            long key = quantized(normalX) & 0x1FFFFFL;
            key |= (quantized(normalY) & 0x1FFFFFL) << 21;
            key |= (quantized(normalZ) & 0x1FFFFFL) << 42;
            Integer existing = this.normalKeys.get(key);
            if (existing != null) {
                return existing;
            }
            int normalIndex = this.normals.size() / 3;
            this.normals.add(normalX);
            this.normals.add(normalY);
            this.normals.add(normalZ);
            this.normalKeys.put(key, normalIndex);
            return normalIndex;
        }

        private static int quantized(float value) {
            return Math.round(value * 1000.0F);
        }

        @SuppressWarnings("unchecked")
        private static List<Integer>[] partLists() {
            List<Integer>[] lists = new List[PlayerMeshPart.values().length];
            for (int partIndex = 0; partIndex < lists.length; partIndex++) {
                lists[partIndex] = new ArrayList<>();
            }
            return lists;
        }

        private static float[] floats(List<Float> values) {
            float[] packed = new float[values.size()];
            for (int elementIndex = 0; elementIndex < values.size(); elementIndex++) {
                packed[elementIndex] = values.get(elementIndex);
            }
            return packed;
        }

        private static int[] ints(List<Integer> values) {
            int[] packed = new int[values.size()];
            for (int elementIndex = 0; elementIndex < values.size(); elementIndex++) {
                packed[elementIndex] = values.get(elementIndex);
            }
            return packed;
        }
    }

    private static final class CachedMesh {
        private final PlayerSkinMesh mesh;
        private final float witnessModelX;
        private final float witnessModelY;
        private final float witnessModelZ;

        CachedMesh(ArmorExtract extract) {
            this.mesh = extract.mesh;
            this.witnessModelX = extract.witnessModelX;
            this.witnessModelY = extract.witnessModelY;
            this.witnessModelZ = extract.witnessModelZ;
        }

        ArmorExtract extract(boolean fresh) {
            return new ArmorExtract(this.mesh, this.witnessModelX, this.witnessModelY, this.witnessModelZ, fresh);
        }
    }

    private static final class InstanceKey {
        private final int identity;
        private final boolean innerLayer;
        private final PlayerMeshKind kind;
        private final long generation;

        InstanceKey(int identity, boolean innerLayer, PlayerMeshKind kind, long generation) {
            this.identity = identity;
            this.innerLayer = innerLayer;
            this.kind = kind;
            this.generation = generation;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof InstanceKey otherKey)) {
                return false;
            }
            return this.identity == otherKey.identity
                    && this.innerLayer == otherKey.innerLayer
                    && this.kind == otherKey.kind
                    && this.generation == otherKey.generation;
        }

        @Override
        public int hashCode() {
            int hash = this.identity;
            hash = hash * 31 + (this.innerLayer ? 1 : 0);
            hash = hash * 31 + this.kind.ordinal();
            return hash * 31 + (int) this.generation;
        }
    }

    private static final class ArmorKey {
        private final long geometryHash;
        private final boolean innerLayer;
        private final PlayerMeshKind kind;
        private final long generation;
        private final int segments;

        ArmorKey(long geometryHash, boolean innerLayer, PlayerMeshKind kind, long generation, int segments) {
            this.geometryHash = geometryHash;
            this.innerLayer = innerLayer;
            this.kind = kind;
            this.generation = generation;
            this.segments = segments;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ArmorKey key)) {
                return false;
            }
            return this.geometryHash == key.geometryHash
                    && this.innerLayer == key.innerLayer
                    && this.kind == key.kind
                    && this.generation == key.generation
                    && this.segments == key.segments;
        }

        @Override
        public int hashCode() {
            int hash = (int) (this.geometryHash ^ (this.geometryHash >>> 32));
            hash = hash * 31 + (this.innerLayer ? 1 : 0);
            hash = hash * 31 + this.kind.ordinal();
            hash = hash * 31 + (int) this.generation;
            return hash * 31 + this.segments;
        }
    }
}
