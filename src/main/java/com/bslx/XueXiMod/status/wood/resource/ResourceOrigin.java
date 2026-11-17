package com.bslx.XueXiMod.status.wood.resource;

/** 批次来源。创建后不可改。终极来源的根潜力强制为 0，片段上没有单独的可充能开关。 */
public enum ResourceOrigin {
    NORMAL_BLOOM,
    ULTIMATE;

    public static ResourceOrigin fromName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (ResourceOrigin origin : values()) {
            if (origin.name().equals(name)) {
                return origin;
            }
        }
        return null;
    }
}
