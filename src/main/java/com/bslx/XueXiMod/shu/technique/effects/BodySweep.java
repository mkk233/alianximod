package com.bslx.XueXiMod.shu.technique.effects;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 身体沿这一拍真实位移的首次接触。不用输入速度代替已经发生的位移。 */
final class BodySweep {
    private BodySweep() {
    }

    static double entryAlong(AABB moverStart, Vec3 displacement, AABB target) {
        if (moverStart == null || displacement == null || target == null) {
            return -1.0;
        }
        double halfX = (moverStart.maxX - moverStart.minX) * 0.5;
        double halfY = (moverStart.maxY - moverStart.minY) * 0.5;
        double halfZ = (moverStart.maxZ - moverStart.minZ) * 0.5;
        double startX = (moverStart.minX + moverStart.maxX) * 0.5;
        double startY = (moverStart.minY + moverStart.maxY) * 0.5;
        double startZ = (moverStart.minZ + moverStart.maxZ) * 0.5;
        double[] origin = {startX, startY, startZ};
        double[] delta = {displacement.x, displacement.y, displacement.z};
        double[] low = {target.minX - halfX, target.minY - halfY, target.minZ - halfZ};
        double[] high = {target.maxX + halfX, target.maxY + halfY, target.maxZ + halfZ};
        double entryTime = 0.0;
        double exitTime = 1.0;
        for (int axisIndex = 0; axisIndex < 3; axisIndex++) {
            double axisDelta = delta[axisIndex];
            double axisStart = origin[axisIndex];
            if (Math.abs(axisDelta) < 1.0E-8) {
                if (axisStart < low[axisIndex] || axisStart > high[axisIndex]) {
                    return -1.0;
                }
                continue;
            }
            double first = (low[axisIndex] - axisStart) / axisDelta;
            double second = (high[axisIndex] - axisStart) / axisDelta;
            double nearSide = Math.min(first, second);
            double farSide = Math.max(first, second);
            entryTime = Math.max(entryTime, nearSide);
            exitTime = Math.min(exitTime, farSide);
            if (entryTime > exitTime) {
                return -1.0;
            }
        }
        if (exitTime < 0.0 || entryTime > 1.0) {
            return -1.0;
        }
        return Math.max(0.0, entryTime);
    }

    static Vec3 closest(AABB box, Vec3 point) {
        double clampedX = Math.max(box.minX, Math.min(box.maxX, point.x));
        double clampedY = Math.max(box.minY, Math.min(box.maxY, point.y));
        double clampedZ = Math.max(box.minZ, Math.min(box.maxZ, point.z));
        return new Vec3(clampedX, clampedY, clampedZ);
    }
}
