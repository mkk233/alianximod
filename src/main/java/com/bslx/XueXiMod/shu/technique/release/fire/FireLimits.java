package com.bslx.XueXiMod.shu.technique.release.fire;

/** 燃烧存档和计划的安全上限。超过上限的输入在扣款前拒绝，损坏存档只丢弃多余部分。 */
public final class FireLimits {
    public static final int PLAN_CAP = 8;
    public static final int SEGMENT_CAP = 32;
    public static final int KEY_CAP = 48;
    public static final int INTERVAL_FLOOR = 1;
    public static final int INTERVAL_CAP = 12000;
    public static final double DAMAGE_CAP = 10000.0d;
    /** 与火种余额上限一致。损坏存档里更大的投入直接丢弃。 */
    public static final int SEED_CAP = 100;
    /** 一个完整段的标准燃料。其它档案可以自带需求，不在结算里写术式编号。 */
    public static final int STANDARD_SEGMENT_DEMAND = 10;
    public static final int DEMAND_CAP = 1000;
    public static final int TIER_CAP = 10;
    /** 旧存档：轨道在挂上时已经付过整段伤害，加载后只播完，不再扣余额。 */
    public static final int PREPAID_VERSION = 1;
    /** 新存档：每条轨道自带燃料模式。共享轨道按段扣唯一余额。 */
    public static final int NBT_VERSION = 2;
    /** 挂载时已经按来源缩放的金额。读取不再乘倍率。缺版本以及 1、2 都是旧最终金额。 */
    public static final int CONFIGURED_VERSION = 3;
    public static final String SAVE_KEY = "xueximod_fire_burn";

    private FireLimits() {
    }
}
