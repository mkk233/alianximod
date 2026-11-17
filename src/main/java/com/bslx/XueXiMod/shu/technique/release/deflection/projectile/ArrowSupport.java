package com.bslx.XueXiMod.shu.technique.release.deflection.projectile;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;

/**
 * 首版只认原版普通箭和光灵箭。药箭的类仍是 Arrow。
 * 三叉戟和任何重写命中的子类都不进这张表。
 */
public final class ArrowSupport {
    private ArrowSupport() {
    }

    /** 运行时类型是不是首版支持的普通箭或光灵箭。 */
    public static boolean supportsType(Class<?> type) {
        return type == Arrow.class || type == SpectralArrow.class;
    }

    /** 这支箭能不能进入偏转飞行。 */
    public static boolean supported(AbstractArrow arrow) {
        return arrow != null && supportsType(arrow.getClass());
    }
}
