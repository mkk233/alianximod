package com.bslx.XueXiMod.shu.technique.release.fire;

/** 存档里的燃烧燃料模式。旧版没有这个标记，一律视为已经预付。 */
public final class FuelMode {
    public static final String PREPAID = "prepaid";
    public static final String SHARED = "shared";

    private FuelMode() {
    }

    /** 缺版本和版本 1 整份都是预付。版本 2、3 只有明确写成共享的轨道才按段扣余额。 */
    public static String kind(int version, String fuelTag) {
        if (version == 0 || version == FireLimits.PREPAID_VERSION) {
            return PREPAID;
        }
        if (version == FireLimits.NBT_VERSION || version == FireLimits.CONFIGURED_VERSION) {
            if (SHARED.equals(fuelTag)) {
                return SHARED;
            }
            return PREPAID;
        }
        return null;
    }
}
