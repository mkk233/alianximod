package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 接口层共用的有限坐标和不可变列表。不读取世界。 */
public final class FrozenValues {
    private static final double DEGENERATE_LENGTH_SQUARED = 1.0E-8D;

    private FrozenValues() {
    }

    public static <T> T present(T value, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        return value;
    }

    public static Vec3 finite(Vec3 vector, String label) {
        if (vector == null || !Double.isFinite(vector.x) || !Double.isFinite(vector.y) || !Double.isFinite(vector.z)) {
            throw new IllegalArgumentException(label + "必须是有限坐标");
        }
        return vector;
    }

    /** 有限且长度可归一化时返回单位向量。调用方传入的原向量不会被保留。 */
    public static Vec3 unit(Vec3 vector, String label) {
        Vec3 checked = finite(vector, label);
        if (checked.lengthSqr() < DEGENERATE_LENGTH_SQUARED) {
            throw new IllegalArgumentException(label + "必须可归一化");
        }
        return checked.normalize();
    }

    public static <T> List<T> list(List<T> source, String label) {
        if (source == null) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        return List.copyOf(source);
    }
}
