package com.bslx.XueXiMod.shu.technique.release.deflection;

/**
 * 后批要写进箭 ForgeData 的键。本批没有任何读取或写入，缺省时箭保持原行为。
 */
public final class DeflectionMarks {
    /** 冻结后的剩余直接伤害。缺省表示还没成为水势交易候选。 */
    public static final String REMAINING_DIRECT = "xuexi_deflect_direct";
    /** 已经完成的完整偏转次数。缺省表示 0。部分卸力不加。 */
    public static final String FULL_COUNT = "xuexi_deflect_full";
    /** 当前飞行接触序号。离开全部有效水体后才增加。 */
    public static final String CONTACT_SERIAL = "xuexi_deflect_serial";
    /** 成对的 UUID 高低位。这些生物不再吃这支箭的附带效果。 */
    public static final String EXCLUDE_OWNERS = "xuexi_deflect_exclude";
    /** setOwner 改写拾取模式之前的原版序数。缺省表示没改过。 */
    public static final String SAVED_PICKUP = "xuexi_deflect_pickup";

    private DeflectionMarks() {
    }
}
