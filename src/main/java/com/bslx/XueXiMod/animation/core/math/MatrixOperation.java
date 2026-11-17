/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Copyright (C) Epic Fight Team / Yesman
 * Local modifications Copyright (C) 2026 XueXi Mod contributors.
 *
 * Upstream: https://github.com/Antikythera-Studios/epicfight
 * Commit: 9d25f19c121c5e51b1db9efe01833a563706eeee
 * Source-Path: src/main/java/yesman/epicfight/api/utils/math/MatrixOperation.java
 * Modified: 2026-10-24
 *
 * 修改说明：只改包名。矩阵乘法策略仍由调用方传入，本身不引用客户端。
 */
package com.bslx.XueXiMod.animation.core.math;

@FunctionalInterface
public interface MatrixOperation {
    OpenMatrix4f mul(OpenMatrix4f left, OpenMatrix4f right, OpenMatrix4f destination);
}
