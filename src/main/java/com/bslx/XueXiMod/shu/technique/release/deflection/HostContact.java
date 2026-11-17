package com.bslx.XueXiMod.shu.technique.release.deflection;

import net.minecraft.world.phys.Vec3;

/**
 * 一次线段接触。segmentT 是提示里的线段参数 t，范围 0 到 1。
 * 字段不能叫 t：项目命名门禁拒绝少于 3 个字符的名字。
 */
public record HostContact(DeflectionHost host, double segmentT, Vec3 point, Vec3 unitNormal) {
    public HostContact {
        if (host == null || point == null || unitNormal == null) {
            throw new IllegalArgumentException("接触缺少宿主或几何");
        }
    }
}
