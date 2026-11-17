package com.bslx.XueXiMod.client.action.mesh;

import java.util.Arrays;

/**
 * 不可变蒙皮网格。内部数组不外泄；读取走标量或拷贝。
 * 位置和法线若来自源文件，载入时已经乘过一次 X 轴 -90 度，单位仍是源米制。
 */
public final class PlayerSkinMesh {
    private final PlayerMeshKind kind;
    private final int jointLimit;
    private final float[] positions;
    private final float[] normals;
    private final float[] textureCoordinates;
    private final float[] weights;
    private final int[] influenceCounts;
    private final int[] influenceStarts;
    private final int[] influenceJoints;
    private final int[] influenceWeightSlots;
    private final int[] partStarts;
    private final int[] partCounts;
    private final int[] cornerPositions;
    private final int[] cornerTextureSlots;
    private final int[] cornerNormals;
    private final String sourceGitPath;
    private final String sourceSha256;
    private final boolean coordinateConversionApplied;

    private PlayerSkinMesh(
            PlayerMeshKind kind,
            int jointLimit,
            float[] positions,
            float[] normals,
            float[] textureCoordinates,
            float[] weights,
            int[] influenceCounts,
            int[] influenceStarts,
            int[] influenceJoints,
            int[] influenceWeightSlots,
            int[] partStarts,
            int[] partCounts,
            int[] cornerPositions,
            int[] cornerTextureSlots,
            int[] cornerNormals,
            String sourceGitPath,
            String sourceSha256,
            boolean coordinateConversionApplied) {
        this.kind = kind;
        this.jointLimit = jointLimit;
        this.positions = positions;
        this.normals = normals;
        this.textureCoordinates = textureCoordinates;
        this.weights = weights;
        this.influenceCounts = influenceCounts;
        this.influenceStarts = influenceStarts;
        this.influenceJoints = influenceJoints;
        this.influenceWeightSlots = influenceWeightSlots;
        this.partStarts = partStarts;
        this.partCounts = partCounts;
        this.cornerPositions = cornerPositions;
        this.cornerTextureSlots = cornerTextureSlots;
        this.cornerNormals = cornerNormals;
        this.sourceGitPath = sourceGitPath == null ? "" : sourceGitPath;
        this.sourceSha256 = sourceSha256 == null ? "" : sourceSha256;
        this.coordinateConversionApplied = coordinateConversionApplied;
    }

    public static PlayerSkinMesh compose(
            PlayerMeshKind kind,
            int jointLimit,
            float[] positions,
            float[] normals,
            float[] textureCoordinates,
            float[] weights,
            int[] influenceCounts,
            int[] influenceJoints,
            int[] influenceWeightSlots,
            int[][] partCorners,
            boolean requireCanonicalParts,
            String sourceGitPath,
            String sourceSha256,
            boolean coordinateConversionApplied) {
        if (jointLimit < 1 || jointLimit > MeshLimits.JOINT_LIMIT) {
            throw new SkinDiagnostic("joint-limit", "关节上限超出已授权的 20 骨范围");
        }
        float[] positionCopy = copyFinite(positions, 3, MeshLimits.POSITION_LIMIT, "位置");
        float[] normalCopy = copyFinite(normals, 3, MeshLimits.NORMAL_LIMIT, "法线");
        float[] textureCopy = copyFinite(textureCoordinates, 2, MeshLimits.TEXTURE_LIMIT, "纹理坐标");
        float[] weightCopy = copyFinite(weights, 1, MeshLimits.WEIGHT_LIMIT, "权重");
        int positionCount = positionCopy.length / 3;
        int normalCount = normalCopy.length / 3;
        int textureCount = textureCopy.length / 2;
        if (positionCount < 1) {
            throw new SkinDiagnostic("positions", "网格没有顶点");
        }
        if ((long) positionCount * (long) normalCount > MeshLimits.NORMAL_PAIR_LIMIT) {
            throw new SkinDiagnostic("normal-pairs", "位置和法线的组合超过缓存上限");
        }
        if (influenceCounts == null || influenceCounts.length != positionCount) {
            throw new SkinDiagnostic("influences", "每个位置都要有一条影响计数");
        }
        int influenceTotal = 0;
        int[] countCopy = new int[positionCount];
        int[] starts = new int[positionCount];
        int highestInfluence = 0;
        for (int positionIndex = 0; positionIndex < positionCount; positionIndex++) {
            int influenceCount = influenceCounts[positionIndex];
            if (influenceCount < 1 || influenceCount > MeshLimits.INFLUENCES_PER_POSITION_LIMIT) {
                throw new SkinDiagnostic("influences", "位置 " + positionIndex + " 的影响数不可接受，不能截断");
            }
            if (influenceCount > highestInfluence) {
                highestInfluence = influenceCount;
            }
            starts[positionIndex] = influenceTotal;
            countCopy[positionIndex] = influenceCount;
            influenceTotal += influenceCount;
            if (influenceTotal > MeshLimits.INFLUENCE_LIMIT) {
                throw new SkinDiagnostic("influences", "影响总数超过上限");
            }
        }
        if (influenceJoints == null || influenceWeightSlots == null
                || influenceJoints.length != influenceTotal || influenceWeightSlots.length != influenceTotal) {
            throw new SkinDiagnostic("influences", "影响关节和权重索引长度不一致");
        }
        int[] jointCopy = Arrays.copyOf(influenceJoints, influenceTotal);
        int[] weightSlotCopy = Arrays.copyOf(influenceWeightSlots, influenceTotal);
        for (int positionIndex = 0; positionIndex < positionCount; positionIndex++) {
            float weightSum = 0.0F;
            int start = starts[positionIndex];
            for (int influenceSlot = 0; influenceSlot < countCopy[positionIndex]; influenceSlot++) {
                int packed = start + influenceSlot;
                int jointId = jointCopy[packed];
                int weightSlot = weightSlotCopy[packed];
                if (jointId < 0 || jointId >= jointLimit) {
                    throw new SkinDiagnostic("joint-id", "关节编号 " + jointId + " 不在已授权范围内，不会重排");
                }
                if (weightSlot < 0 || weightSlot >= weightCopy.length) {
                    throw new SkinDiagnostic("weight-index", "权重表下标越界");
                }
                float weight = weightCopy[weightSlot];
                if (!Float.isFinite(weight) || weight <= 0.0F) {
                    throw new SkinDiagnostic("weight", "影响权重必须是正的有限数");
                }
                weightSum += weight;
            }
            if (!Float.isFinite(weightSum) || Math.abs(weightSum - 1.0F) > MeshLimits.WEIGHT_SUM_TOLERANCE) {
                throw new SkinDiagnostic("weight-sum", "位置 " + positionIndex + " 的权重和不是 1");
            }
        }
        if (partCorners == null || partCorners.length != PlayerMeshPart.values().length) {
            throw new SkinDiagnostic("parts", "分区数组长度必须盖住全部部位槽");
        }
        int cornerTotal = 0;
        int[] startsByPart = new int[PlayerMeshPart.values().length];
        int[] countsByPart = new int[PlayerMeshPart.values().length];
        Arrays.fill(countsByPart, -1);
        for (int partIndex = 0; partIndex < partCorners.length; partIndex++) {
            int[] corners = partCorners[partIndex];
            if (corners == null) {
                if (requireCanonicalParts) {
                    throw new SkinDiagnostic("parts", "缺少必需分区 " + PlayerMeshPart.values()[partIndex].wireName());
                }
                continue;
            }
            if (corners.length < 9 || corners.length % 9 != 0) {
                throw new SkinDiagnostic("corners", "分区角点必须按三角形成组");
            }
            int cornerNumber = corners.length / 3;
            startsByPart[partIndex] = cornerTotal;
            countsByPart[partIndex] = cornerNumber;
            cornerTotal += cornerNumber;
        }
        if (requireCanonicalParts) {
            for (PlayerMeshPart part : PlayerMeshPart.values()) {
                if (countsByPart[part.ordinal()] < 3) {
                    throw new SkinDiagnostic("parts", "缺少必需分区 " + part.wireName());
                }
            }
        }
        int triangleCount = cornerTotal / 3;
        if (triangleCount < 1 || triangleCount > MeshLimits.TRIANGLE_LIMIT) {
            throw new SkinDiagnostic("triangles", "三角形数量不可接受");
        }
        int[] positionSlots = new int[cornerTotal];
        int[] textureSlots = new int[cornerTotal];
        int[] normalSlots = new int[cornerTotal];
        int cursor = 0;
        for (int partIndex = 0; partIndex < partCorners.length; partIndex++) {
            int[] corners = partCorners[partIndex];
            if (corners == null) {
                continue;
            }
            for (int cornerIndex = 0; cornerIndex < corners.length; cornerIndex += 3) {
                int positionSlot = corners[cornerIndex];
                int textureSlot = corners[cornerIndex + 1];
                int normalSlot = corners[cornerIndex + 2];
                if (positionSlot < 0 || positionSlot >= positionCount
                        || textureSlot < 0 || textureSlot >= textureCount
                        || normalSlot < 0 || normalSlot >= normalCount) {
                    throw new SkinDiagnostic("corner-index", "角点索引越界");
                }
                positionSlots[cursor] = positionSlot;
                textureSlots[cursor] = textureSlot;
                normalSlots[cursor] = normalSlot;
                cursor++;
            }
        }
        return new PlayerSkinMesh(kind, jointLimit, positionCopy, normalCopy, textureCopy, weightCopy,
                countCopy, starts, jointCopy, weightSlotCopy, startsByPart, countsByPart,
                positionSlots, textureSlots, normalSlots, sourceGitPath, sourceSha256, coordinateConversionApplied);
    }

    public PlayerMeshKind kind() {
        return this.kind;
    }

    public int jointLimit() {
        return this.jointLimit;
    }

    public int positionCount() {
        return this.positions.length / 3;
    }

    public int normalCount() {
        return this.normals.length / 3;
    }

    public int textureCount() {
        return this.textureCoordinates.length / 2;
    }

    public int weightCount() {
        return this.weights.length;
    }

    public int influenceCount() {
        return this.influenceJoints.length;
    }

    public int cornerCount() {
        return this.cornerPositions.length;
    }

    public int triangleCount() {
        return this.cornerPositions.length / 3;
    }

    public int partCount() {
        int present = 0;
        for (int count : this.partCounts) {
            if (count >= 0) {
                present++;
            }
        }
        return present;
    }

    public boolean hasPart(PlayerMeshPart part) {
        return part != null && this.partCounts[part.ordinal()] >= 0;
    }

    public int partCornerCount(PlayerMeshPart part) {
        if (!this.hasPart(part)) {
            return 0;
        }
        return this.partCounts[part.ordinal()];
    }

    public String sourceGitPath() {
        return this.sourceGitPath;
    }

    public String sourceSha256() {
        return this.sourceSha256;
    }

    public boolean coordinateConversionApplied() {
        return this.coordinateConversionApplied;
    }

    public float positionX(int positionIndex) {
        return this.positions[this.positionOffset(positionIndex)];
    }

    public float positionY(int positionIndex) {
        return this.positions[this.positionOffset(positionIndex) + 1];
    }

    public float positionZ(int positionIndex) {
        return this.positions[this.positionOffset(positionIndex) + 2];
    }

    public float[] copyPositions() {
        return this.positions.clone();
    }

    public float weight(int weightIndex) {
        if (weightIndex < 0 || weightIndex >= this.weights.length) {
            throw new SkinDiagnostic("weight-index", "权重表下标越界");
        }
        return this.weights[weightIndex];
    }

    public int influenceCountAt(int positionIndex) {
        this.positionOffset(positionIndex);
        return this.influenceCounts[positionIndex];
    }

    public int influenceJoint(int positionIndex, int influenceSlot) {
        return this.influenceJoints[this.influencePacked(positionIndex, influenceSlot)];
    }

    public int influenceWeightSlot(int positionIndex, int influenceSlot) {
        return this.influenceWeightSlots[this.influencePacked(positionIndex, influenceSlot)];
    }

    public int cornerPosition(int cornerIndex) {
        return this.cornerPositions[this.cornerOffset(cornerIndex)];
    }

    public int cornerTexture(int cornerIndex) {
        return this.cornerTextureSlots[this.cornerOffset(cornerIndex)];
    }

    public int cornerNormal(int cornerIndex) {
        return this.cornerNormals[this.cornerOffset(cornerIndex)];
    }

    public float textureU(int textureIndex) {
        return this.textureCoordinates[this.textureOffset(textureIndex)];
    }

    public float textureV(int textureIndex) {
        return this.textureCoordinates[this.textureOffset(textureIndex) + 1];
    }

    int influenceStart(int positionIndex) {
        this.positionOffset(positionIndex);
        return this.influenceStarts[positionIndex];
    }

    float weightAtPacked(int packed) {
        return this.weights[this.influenceWeightSlots[packed]];
    }

    int jointAtPacked(int packed) {
        return this.influenceJoints[packed];
    }

    int partStart(PlayerMeshPart part) {
        return this.partStarts[part.ordinal()];
    }

    float normalX(int normalIndex) {
        return this.normals[normalIndex * 3];
    }

    float normalY(int normalIndex) {
        return this.normals[normalIndex * 3 + 1];
    }

    float normalZ(int normalIndex) {
        return this.normals[normalIndex * 3 + 2];
    }

    private int positionOffset(int positionIndex) {
        if (positionIndex < 0 || positionIndex >= this.positionCount()) {
            throw new SkinDiagnostic("position-index", "位置下标越界");
        }
        return positionIndex * 3;
    }

    private int textureOffset(int textureIndex) {
        if (textureIndex < 0 || textureIndex >= this.textureCount()) {
            throw new SkinDiagnostic("texture-index", "纹理下标越界");
        }
        return textureIndex * 2;
    }

    private int cornerOffset(int cornerIndex) {
        if (cornerIndex < 0 || cornerIndex >= this.cornerPositions.length) {
            throw new SkinDiagnostic("corner-index", "角点下标越界");
        }
        return cornerIndex;
    }

    private int influencePacked(int positionIndex, int influenceSlot) {
        int count = this.influenceCountAt(positionIndex);
        if (influenceSlot < 0 || influenceSlot >= count) {
            throw new SkinDiagnostic("influences", "影响槽越界");
        }
        return this.influenceStarts[positionIndex] + influenceSlot;
    }

    private static float[] copyFinite(float[] source, int stride, int itemLimit, String label) {
        if (source == null || source.length == 0 || source.length % stride != 0) {
            throw new SkinDiagnostic("stride", label + " 的长度和步长不匹配");
        }
        int itemCount = source.length / stride;
        if (itemCount > itemLimit) {
            throw new SkinDiagnostic("limit", label + " 数量超过上限");
        }
        float[] copy = new float[source.length];
        for (int elementIndex = 0; elementIndex < source.length; elementIndex++) {
            float value = source[elementIndex];
            if (!Float.isFinite(value)) {
                throw new SkinDiagnostic("finite", label + " 含有非有限数");
            }
            copy[elementIndex] = value;
        }
        return copy;
    }
}
