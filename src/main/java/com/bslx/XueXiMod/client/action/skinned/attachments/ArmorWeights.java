package com.bslx.XueXiMod.client.action.skinned.attachments;

import com.bslx.XueXiMod.animation.core.math.Vec3f;
import com.bslx.XueXiMod.client.action.mesh.SkinDiagnostic;

/**
 * 原创权重。头是硬壳，只跟 Head。
 * 躯干在 Torso 与 Chest 之间做平滑过渡。
 * 手臂在 Arm 与 Hand 之间做平滑过渡；Elbow 是旁支，只在投影到 Arm-Hand 的站点附近占一条有限带。
 * 腿对 Thigh、Leg、Knee 使用同一规则。带的半宽是该部件沿骨骼参数长度的 0.2，峰值 0.35。
 * 权重非负，和为 1。端点可以收成近端单骨，中段不能整段单骨。
 */
final class ArmorWeights {
    static final float BAND_FRACTION = 0.2F;
    static final float SIDE_PEAK = 0.35F;
    private static final float OMIT = 0.001F;

    private ArmorWeights() {
    }

    static final class Influences {
        final int[] jointIds;
        final float[] weights;

        Influences(int[] jointIds, float[] weights) {
            this.jointIds = jointIds;
            this.weights = weights;
        }
    }

    static Influences weigh(
            ArmorRegion region,
            float sourceX,
            float sourceY,
            float sourceZ,
            BindJointTable table,
            float lowestAlong,
            float highestAlong) {
        if (region == null || table == null) {
            throw new SkinDiagnostic("weight", "权重区域或绑定缺失");
        }
        if (region == ArmorRegion.HEAD) {
            return single(table, "Head");
        }
        if (region == ArmorRegion.TORSO) {
            return pair(table, "Torso", "Chest", sourceX, sourceY, sourceZ, lowestAlong, highestAlong, null);
        }
        if (region == ArmorRegion.ARM_RIGHT) {
            return pair(table, "Arm_R", "Hand_R", sourceX, sourceY, sourceZ, lowestAlong, highestAlong, "Elbow_R");
        }
        if (region == ArmorRegion.ARM_LEFT) {
            return pair(table, "Arm_L", "Hand_L", sourceX, sourceY, sourceZ, lowestAlong, highestAlong, "Elbow_L");
        }
        if (region == ArmorRegion.LEG_RIGHT) {
            return pair(table, "Thigh_R", "Leg_R", sourceX, sourceY, sourceZ, lowestAlong, highestAlong, "Knee_R");
        }
        return pair(table, "Thigh_L", "Leg_L", sourceX, sourceY, sourceZ, lowestAlong, highestAlong, "Knee_L");
    }

    static float along(BindJointTable table, String startName, String endName, float sourceX, float sourceY, float sourceZ) {
        Vec3f start = table.origin(startName);
        Vec3f end = table.origin(endName);
        float axisX = end.coordinateX - start.coordinateX;
        float axisY = end.coordinateY - start.coordinateY;
        float axisZ = end.coordinateZ - start.coordinateZ;
        float lengthSquared = axisX * axisX + axisY * axisY + axisZ * axisZ;
        if (!Float.isFinite(lengthSquared) || lengthSquared < 1.0E-8F) {
            throw new SkinDiagnostic("bone-length", startName + " 到 " + endName + " 的长度不可用");
        }
        float offsetX = sourceX - start.coordinateX;
        float offsetY = sourceY - start.coordinateY;
        float offsetZ = sourceZ - start.coordinateZ;
        return (offsetX * axisX + offsetY * axisY + offsetZ * axisZ) / lengthSquared;
    }

    private static Influences single(BindJointTable table, String jointName) {
        return new Influences(new int[] {table.jointId(jointName)}, new float[] {1.0F});
    }

    private static Influences pair(
            BindJointTable table,
            String startName,
            String endName,
            float sourceX,
            float sourceY,
            float sourceZ,
            float lowestAlong,
            float highestAlong,
            String sideName) {
        float parameter = along(table, startName, endName, sourceX, sourceY, sourceZ);
        float clamped = clamp01(parameter);
        float endShare = smooth(clamped);
        float startShare = 1.0F - endShare;
        float sideShare = 0.0F;
        if (sideName != null) {
            float sideParameter = clamp01(along(table, startName, endName,
                    table.origin(sideName).coordinateX,
                    table.origin(sideName).coordinateY,
                    table.origin(sideName).coordinateZ));
            float extent = highestAlong - lowestAlong;
            if (!Float.isFinite(extent) || extent < 1.0E-3F) {
                extent = 1.0F;
            }
            float band = BAND_FRACTION * extent;
            float distance = Math.abs(clamped - sideParameter);
            if (distance < band && band > 0.0F) {
                sideShare = SIDE_PEAK * smooth(1.0F - distance / band);
            }
        }
        float remain = 1.0F - sideShare;
        float startWeight = remain * startShare;
        float endWeight = remain * endShare;
        return pack(table, startName, startWeight, endName, endWeight, sideName, sideShare);
    }

    private static Influences pack(
            BindJointTable table,
            String startName,
            float startWeight,
            String endName,
            float endWeight,
            String sideName,
            float sideWeight) {
        int count = 0;
        if (startWeight >= OMIT) {
            count++;
        }
        if (endWeight >= OMIT) {
            count++;
        }
        if (sideName != null && sideWeight >= OMIT) {
            count++;
        }
        if (count == 0) {
            return single(table, startName);
        }
        int[] jointIds = new int[count];
        float[] weights = new float[count];
        int cursor = 0;
        float sum = 0.0F;
        if (startWeight >= OMIT) {
            jointIds[cursor] = table.jointId(startName);
            weights[cursor] = startWeight;
            sum += startWeight;
            cursor++;
        }
        if (endWeight >= OMIT) {
            jointIds[cursor] = table.jointId(endName);
            weights[cursor] = endWeight;
            sum += endWeight;
            cursor++;
        }
        if (sideName != null && sideWeight >= OMIT) {
            jointIds[cursor] = table.jointId(sideName);
            weights[cursor] = sideWeight;
            sum += sideWeight;
        }
        if (!Float.isFinite(sum) || sum <= 0.0F) {
            throw new SkinDiagnostic("weight-sum", "权重和不可用");
        }
        float inverse = 1.0F / sum;
        float rebuilt = 0.0F;
        for (int weightIndex = 0; weightIndex < weights.length; weightIndex++) {
            weights[weightIndex] = weights[weightIndex] * inverse;
            if (weights[weightIndex] <= 0.0F) {
                throw new SkinDiagnostic("weight", "归一化后出现非正权重");
            }
            rebuilt += weights[weightIndex];
        }
        weights[0] += 1.0F - rebuilt;
        if (weights[0] <= 0.0F) {
            throw new SkinDiagnostic("weight", "归一化后近端权重非正");
        }
        return new Influences(jointIds, weights);
    }

    static float smooth(float amount) {
        float clamped = clamp01(amount);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    static float clamp01(float amount) {
        if (amount < 0.0F) {
            return 0.0F;
        }
        if (amount > 1.0F) {
            return 1.0F;
        }
        return amount;
    }
}
