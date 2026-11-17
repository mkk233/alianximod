package com.bslx.XueXiMod.shu.technique.enchant.resource;

/**
 * 幽冥宿主合同。周期仲裁在 NetherSeedArbiter，领域遍历、敌对过滤和蓝火表现由领域作者执行。
 */
public final class NetherContract {
    public static final int PERIOD_TICKS = ResourceEnchantNumbers.GLOOM_PERIOD_TICKS;
    public static final int BASE_POINTS = ResourceEnchantNumbers.GLOOM_BASE;
    public static final String VISUAL_TINT = "blue";
    public static final boolean REQUIRES_CONTACT_DAMAGE = false;
    public static final boolean APPLIES_SLOW = false;
    public static final boolean CREATES_DOMAIN = false;
    public static final boolean PLACES_VANILLA_FIRE = false;
    public static final boolean REFRESHES_BURN_DURATION = false;

    private NetherContract() {
    }
}
