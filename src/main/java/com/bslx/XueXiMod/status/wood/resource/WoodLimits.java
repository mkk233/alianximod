package com.bslx.XueXiMod.status.wood.resource;

/** 花瓣与岁华的账本分辨率和内存保护上限。这些数字不是花瓣产量或技能配置。 */
public final class WoodLimits {
    public static final int FORMAT_VERSION = 2;
    public static final int PREVIOUS_FORMAT_VERSION = 1;
    /** 一度。乘除用 long，结果再落回 int。 */
    public static final int UNITS_PER_DEGREE = 60000;
    /** 三度满额。六项贡献之和不超过这个数。 */
    public static final int FULL_UNITS = 3 * UNITS_PER_DEGREE;
    /** 一次普通批次可提交的岁华潜力合计。允许更少，不允许事后追加。 */
    public static final int BATCH_POTENTIAL = UNITS_PER_DEGREE;
    public static final int ROOTS_PER_BATCH = 256;
    public static final int BATCHES_PER_OWNER = 256;
    public static final int FRAGMENTS_PER_OWNER = 4096;
    public static final int FRAGMENTS_PER_SERVER = 65536;
    /** 同一主人同一 tick 内未发布票据的内存上限，与活跃批次上限相同。 */
    public static final int OPEN_TICKETS_PER_OWNER = BATCHES_PER_OWNER;
    /** 可寻址水平方块。超出的坐标整批拒绝，避免永远卸不掉的记录占满片段上限。 */
    public static final int HORIZONTAL_BLOCK_LIMIT = 30000000;

    private WoodLimits() {
    }
}
