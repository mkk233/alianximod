package com.bslx.XueXiMod.shu.technique.release.capture;

/**
 * 捕获拒绝和草稿的玩家文字。装配与释放共用这些句子。
 */
public final class ActorCaptureTexts {
    public static final String STRUCTURE = "水幻形后仅支持一段已适配水系攻击；本版支持薄水切";
    public static final String MISSING_NEXT = "缺少支持的下一攻击";
    public static final String EMBRYO = "原胚会改变这一发，本版不执行";
    public static final String STAFF_ONLY = "该捕获术式只能装配在伞上并由此释放";
    public static final String CARRIER_SLOT = "这个剑术没有被完整捕获计划收走，不能从伞释放";
    public static final String TICK_LIMIT = "这一刻已经部署过水幻形";
    public static final String NOT_RELEASABLE = "当前捕获链不可释放";
    public static final String UNKNOWN_ENTRY = "伞上有无法识别的术式，这一发已拒绝";
    public static final String UNKNOWN_EMBRYO = "伞上的原胚无法识别，这一发已拒绝";
    public static final String CHAIN_MISMATCH = "伞条目和可释放链不一致，这一发已拒绝";
    public static final String QUOTE_UNAVAILABLE = "水幻形的费用或寿命还不能读取，这一发已拒绝";
    public static final String QUOTE_OVERFLOW = "水幻形和载荷的费用合计超出范围，这一发已拒绝";
    public static final String COOLDOWN_OVERFLOW = "水幻形和载荷的冷却合计超出范围，这一发已拒绝";
    public static final String GEOMETRY = "这个位置或方向不能部署水幻形";
    public static final String OWNER = "主人不在线或已经死亡，水幻形没有部署";
    public static final String CAPACITY = "分身数量已经到达上限";
    public static final String ADAPTER = "捕获载荷的适配已经变化，这一发已拒绝";
    public static final String WRITE_KEPT = "伞没有改动，物品已退回";

    private ActorCaptureTexts() {
    }
}
