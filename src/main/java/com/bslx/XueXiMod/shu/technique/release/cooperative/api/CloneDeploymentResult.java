package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneHandle;
import org.jetbrains.annotations.Nullable;

/** 独立部署结果。成功时带身体句柄，拒绝时不带。 */
public record CloneDeploymentResult(DeploymentStatus status, @Nullable CloneHandle handle,
                                    @Nullable String refusalKey) {
    public CloneDeploymentResult {
        if (status == null) {
            throw new IllegalArgumentException("独立部署结果缺少状态");
        }
        if (status == DeploymentStatus.SUCCESS) {
            if (handle == null) {
                throw new IllegalArgumentException("成功的独立部署缺少身体句柄");
            }
        } else if (handle != null) {
            throw new IllegalArgumentException("被拒绝的独立部署不能留下身体句柄");
        }
    }
}
