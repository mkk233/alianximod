package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.shu.balance.BalanceBound;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.balance.BalanceUnit;
import com.bslx.XueXiMod.shu.balance.RawDefault;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.release.domain.motion.VolumeMotionKind;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * 一份领域声明合同。盒子尺寸由各术式传入，公共代码不写死雾雨；存续由领域服务按寿命和主人状态管理。
 * 球形场地声明半径和维护，护体声明随身环，都不使用这份盒子声明。
 */
public final class DomainDefinition {
    public static final String MIST_ID = "water_mist";
    public static final String PRESSURE_ID = "water_deep_pressure";
    public static final String GLOOM_ID = "fire_nether_gloom";
    public static final String VORTEX_ID = "water_vortex";
    public static final String BUBBLE_ID = "water_bubble_column";
    public static final double VORTEX_WIDTH = 5.0D;
    public static final double VORTEX_HEIGHT = 3.0D;
    public static final double BUBBLE_WIDTH = 2.5D;
    public static final double BUBBLE_RADIUS = 1.25D;
    public static final double SHARED_WIDTH = 7.0D;
    public static final double SHARED_DEPTH = 7.0D;
    public static final double TALL_HEIGHT = 8.0D;
    public static final double PRESSURE_HEIGHT = 3.0D;
    public static final int OPEN_COST = 40;
    public static final int DOMAIN_LIFE = 200;
    public static final int CONTACT_UNIT = 1;
    public static final String CONTACT_MANA_COST = "contactManaCost";
    public static final String WET_PERIOD = "wetPeriodTicks";
    public static final String MOMENTUM_PERIOD = "momentumPeriodTicks";
    public static final String SLOW_PERIOD = "slowPeriodTicks";
    public static final String BREATH_MULTIPLIER = "breathMultiplier";
    public static final int WET_PERIOD_TICKS = 10;
    public static final int MOMENTUM_PERIOD_TICKS = 20;
    public static final int SLOW_PERIOD_TICKS = 10;
    public static final float BREATH_MULTIPLIER_RAW = 2.0F;

    private final Placer placer;
    private final int initialCost;
    /** 历史构造参数。不参与存续，领域服务不再据此扣费。 */
    private final int upkeepCost;
    /** 历史构造参数。不参与存续，也不再形成维护节拍。 */
    private final int upkeepPeriod;
    private final int lifetimeTicks;
    private final DomainNativePolicy nativePolicy;
    private final DomainAuraSpec auraSpec;
    private final boolean learnsWhenReady;
    private final DomainBoundary boundary;
    private final DomainVisual visual;
    private String balanceId = "";
    private VolumeMotionKind motionKind = VolumeMotionKind.NONE;
    private boolean stillWater;

    /** 建一份默认封闭体积领域。维护参数固定为 0，不在这里扣起手费。 */
    public DomainDefinition(Placer placer, int initialCost, int lifetimeTicks,
                            DomainNativePolicy nativePolicy, DomainAuraSpec auraSpec, boolean learnsWhenReady) {
        this(placer, initialCost, lifetimeTicks, nativePolicy, auraSpec, learnsWhenReady,
                DomainBoundary.SEAL, DomainVisual.VOLUME);
    }

    /** 建一份指定边界和画法的领域。空边界退回封闭，空画法退回体积，不扣资源。 */
    public DomainDefinition(Placer placer, int initialCost, int lifetimeTicks,
                            DomainNativePolicy nativePolicy, DomainAuraSpec auraSpec, boolean learnsWhenReady,
                            DomainBoundary boundary, DomainVisual visual) {
        this(placer, initialCost, 0, 0, lifetimeTicks, nativePolicy, auraSpec, learnsWhenReady, boundary, visual);
    }

    /**
     * 旧签名保留，范围外调用仍能编译。
     * 维护费和周期只被读出，不再作为领域存续或扣款条件。
     */
    public DomainDefinition(Placer placer, int initialCost, int upkeepCost, int upkeepPeriod, int lifetimeTicks,
                            DomainNativePolicy nativePolicy, DomainAuraSpec auraSpec, boolean learnsWhenReady) {
        this(placer, initialCost, upkeepCost, upkeepPeriod, lifetimeTicks, nativePolicy, auraSpec, learnsWhenReady,
                DomainBoundary.SEAL, DomainVisual.VOLUME);
    }

    /**
     * 旧签名保留，范围外调用仍能编译。
     * 维护费和周期只被读出，不再作为领域存续或扣款条件。
     */
    public DomainDefinition(Placer placer, int initialCost, int upkeepCost, int upkeepPeriod, int lifetimeTicks,
                            DomainNativePolicy nativePolicy, DomainAuraSpec auraSpec, boolean learnsWhenReady,
                            DomainBoundary boundary, DomainVisual visual) {
        this.placer = placer;
        this.initialCost = initialCost;
        this.upkeepCost = upkeepCost;
        this.upkeepPeriod = upkeepPeriod;
        this.lifetimeTicks = lifetimeTicks;
        this.nativePolicy = nativePolicy;
        this.auraSpec = auraSpec;
        this.learnsWhenReady = learnsWhenReady;
        this.boundary = boundary == null ? DomainBoundary.SEAL : boundary;
        this.visual = visual == null ? DomainVisual.VOLUME : visual;
    }

    /** 建雾雨声明。尺寸和寿命从平衡读取，费用留到领域服务结算；声明本身不扣资源。 */
    public static DomainDefinition mistRain() {
        return new DomainDefinition(
                player -> AxisBoxGeometry.atFeet(player.position(),
                        SpecialBalance.number(MIST_ID, BalanceKeys.BOX_WIDTH, SHARED_WIDTH),
                        SpecialBalance.number(MIST_ID, BalanceKeys.BOX_DEPTH, SHARED_DEPTH),
                        SpecialBalance.number(MIST_ID, BalanceKeys.BOX_HEIGHT, TALL_HEIGHT)),
                OPEN_COST, DOMAIN_LIFE,
                new DomainNativePolicy("mist_rain", WET_PERIOD_TICKS, 2, 10, MOMENTUM_PERIOD_TICKS, 2, 20).boundTo(MIST_ID),
                DomainAuraSpec.tideNeutral(),
                true).forTechnique(MIST_ID);
    }

    /** 绑定平衡术式编号。空编号记成空串，原声明返回，不扣资源。 */
    public DomainDefinition forTechnique(String techniqueId) {
        this.balanceId = techniqueId == null ? "" : techniqueId;
        return this;
    }

    public String balanceId() {
        return balanceId;
    }

    /** 按声明的放置器落盒。不扣资源；放置器失败时返回它给出的结果。 */
    public DomainGeometry place(ServerPlayer player) {
        return placer.place(player);
    }

    public int initialCost() {
        return initialCost;
    }

    /**
     * @deprecated 领域不再按这份费用维护。返回值不是扣款依据。
     */
    @Deprecated
    public int upkeepCost() {
        return upkeepCost;
    }

    /**
     * @deprecated 领域不再按这份周期维护。返回值不是扣款节拍。
     */
    @Deprecated
    public int upkeepPeriod() {
        return upkeepPeriod;
    }

    /** 读取寿命。绑定了术式时改读平衡值，读不到时退回声明原值。 */
    public int lifetimeTicks() {
        if (balanceId.isEmpty()) {
            return lifetimeTicks;
        }
        return SpecialBalance.integer(balanceId, BalanceKeys.LIFETIME_TICKS, lifetimeTicks);
    }

    public DomainNativePolicy nativePolicy() {
        return nativePolicy;
    }

    public DomainAuraSpec auraSpec() {
        return auraSpec;
    }

    public boolean learnsWhenReady() {
        return learnsWhenReady;
    }

    public DomainBoundary boundary() {
        return boundary;
    }

    public DomainVisual visual() {
        return visual;
    }

    /** 给声明指定体积运动。空种类退回无运动，不改变已生成实例。 */
    public DomainDefinition motion(VolumeMotionKind kind) {
        this.motionKind = kind == null ? VolumeMotionKind.NONE : kind;
        return this;
    }

    public VolumeMotionKind motionKind() {
        return motionKind;
    }

    /** 标记这份声明会放静水。只改声明，不立即改方块。 */
    public DomainDefinition stillWater() {
        this.stillWater = true;
        return this;
    }

    public boolean placesStillWater() {
        return stillWater;
    }

    /** 建开放的深压声明。只登记减速、潮湿和耗氧参数，不在这里扣费或封人。 */
    public static DomainDefinition deepPressure() {
        return new DomainDefinition(
                player -> AxisBoxGeometry.atFeet(player.position(),
                        SpecialBalance.number(PRESSURE_ID, BalanceKeys.BOX_WIDTH, SHARED_WIDTH),
                        SpecialBalance.number(PRESSURE_ID, BalanceKeys.BOX_DEPTH, SHARED_DEPTH),
                        SpecialBalance.number(PRESSURE_ID, BalanceKeys.BOX_HEIGHT, PRESSURE_HEIGHT)),
                OPEN_COST, DOMAIN_LIFE,
                new DomainNativePolicy("deep_pressure", WET_PERIOD_TICKS, 4, 10, 20, 0, 20, 0, 20,
                        SLOW_PERIOD_TICKS, 10, BREATH_MULTIPLIER_RAW).boundTo(PRESSURE_ID),
                DomainAuraSpec.tideNeutral(), true,
                DomainBoundary.OPEN, DomainVisual.PRESSURE).stillWater().forTechnique(PRESSURE_ID);
    }

    /** 建开放的旋流宿主声明。运动留到运行时执行，声明本身不收水势。 */
    public static DomainDefinition vortex() {
        return new DomainDefinition(
                player -> AxisBoxGeometry.atFeet(player.position(),
                        SpecialBalance.number(VORTEX_ID, BalanceKeys.BOX_WIDTH, VORTEX_WIDTH),
                        SpecialBalance.number(VORTEX_ID, BalanceKeys.BOX_DEPTH, VORTEX_WIDTH),
                        SpecialBalance.number(VORTEX_ID, BalanceKeys.BOX_HEIGHT, VORTEX_HEIGHT)),
                20, DOMAIN_LIFE,
                new DomainNativePolicy("volume_host", 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0F),
                DomainAuraSpec.tideNeutral(), true,
                DomainBoundary.OPEN, DomainVisual.QUIET)
                .motion(VolumeMotionKind.VORTEX_HOST)
                .forTechnique(VORTEX_ID);
    }

    /** 建开放的气泡柱声明。上升留到运行时执行，声明本身不收水势。 */
    public static DomainDefinition bubble() {
        return new DomainDefinition(
                player -> AxisBoxGeometry.atFeet(player.position(),
                        SpecialBalance.number(BUBBLE_ID, BalanceKeys.BOX_WIDTH, BUBBLE_WIDTH),
                        SpecialBalance.number(BUBBLE_ID, BalanceKeys.BOX_DEPTH, BUBBLE_WIDTH),
                        SpecialBalance.number(BUBBLE_ID, BalanceKeys.BOX_HEIGHT, TALL_HEIGHT)),
                12, DOMAIN_LIFE,
                new DomainNativePolicy("volume_updraft", 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0F),
                DomainAuraSpec.tideNeutral(), true,
                DomainBoundary.OPEN, DomainVisual.QUIET)
                .motion(VolumeMotionKind.UPDRAFT)
                .forTechnique(BUBBLE_ID);
    }

    @FunctionalInterface
    public interface Placer {
        DomainGeometry place(ServerPlayer player);
    }

    /** 用脚底、宽度、纵深和高度落一个轴对齐盒。不扣资源。 */
    public static DomainGeometry feetBox(Vec3 feet, double width, double depth, double height) {
        return AxisBoxGeometry.atFeet(feet, width, depth, height);
    }

    /**
     * 单独释放的幽冥。水平 7 与寿命 200 是雾雨和深压的共同值，高度用体积领域已登记的 8。
     * 初始费用为 0，魔力由按键上的登记价支付。没有潮湿、减速、耗氧或潮印伤害。
     */
    public static DomainDefinition gloom() {
        return new DomainDefinition(
                player -> AxisBoxGeometry.atFeet(player.position(),
                        SpecialBalance.number(GLOOM_ID, BalanceKeys.BOX_WIDTH, SHARED_WIDTH),
                        SpecialBalance.number(GLOOM_ID, BalanceKeys.BOX_DEPTH, SHARED_DEPTH),
                        SpecialBalance.number(GLOOM_ID, BalanceKeys.BOX_HEIGHT, TALL_HEIGHT)),
                0, DOMAIN_LIFE,
                new DomainNativePolicy("fire_nether_gloom", 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0f),
                new DomainAuraSpec("", 0f),
                false,
                DomainBoundary.OPEN,
                DomainVisual.QUIET).forTechnique(GLOOM_ID);
    }

    /** 登记雾雨、深压、幽冥和两份流场的平衡参数。只发布声明，不改已有实例。 */
    public static void declareSpecialBalances() {
        declareVolume(MIST_ID, TALL_HEIGHT, true, false, false);
        declareVolume(PRESSURE_ID, PRESSURE_HEIGHT, false, true, true);
        declareVolume(GLOOM_ID, TALL_HEIGHT, false, false, false);
        declareFlowBalances();
    }

    /** 登记旋流和气泡的平衡参数。只发布声明，不启动运动。 */
    public static void declareFlowBalances() {
        declareVortexBalance();
        declareBubbleBalance();
    }

    private static void declareVortexBalance() {
        com.bslx.XueXiMod.shu.balance.TechniqueBalanceSpec.Builder builder = SpecialBalance.manaFee(
                SpecialBalance.speed(
                        SpecialBalance.life(
                                SpecialBalance.blocks(
                                        SpecialBalance.blocks(
                                                SpecialBalance.blocks(SpecialBalance.begin(VORTEX_ID),
                                                        BalanceKeys.BOX_WIDTH, VORTEX_WIDTH, "旋流本体宽度，默认是直径 5"),
                                                BalanceKeys.BOX_DEPTH, VORTEX_WIDTH, "旋流本体纵深"),
                                        BalanceKeys.BOX_HEIGHT, VORTEX_HEIGHT, "干地本体高度"),
                                DOMAIN_LIFE, "旋流寿命。打开时读取"),
                        0.45F, "旋流水平环绕速度上限"),
                CONTACT_MANA_COST, CONTACT_UNIT, "领域接触附魔单价");
        builder.numberParameter(BalanceKeys.ATTRACTION_RADIUS, RawDefault.literal(8.0D),
                        BalanceBound.positiveSize(), BalanceUnit.BLOCKS, "水平吸引半径。默认 8，播放时读取，不用本体半径代替")
                .numberParameter(BalanceKeys.BODY_RADIUS, RawDefault.literal(2.5D),
                        BalanceBound.positiveSize(), BalanceUnit.BLOCKS, "旋流圆柱半径。每拍读取")
                .numberParameter(BalanceKeys.LIFTED_HEIGHT, RawDefault.literal(8.0D),
                        BalanceBound.positiveSize(), BalanceUnit.BLOCKS, "挂上有效上升后的柱高。组合成立时读取")
                .numberParameter(BalanceKeys.PULL_SPEED, RawDefault.literal(0.35D),
                        BalanceBound.positiveSpeed(), BalanceUnit.BLOCKS_PER_TICK, "外围吸引速度上限。每拍读取")
                .numberParameter(BalanceKeys.ORBIT_ANGULAR_SPEED, RawDefault.literal(0.10D),
                        BalanceBound.inclusive(0.0D, 0.5D), BalanceUnit.RADIANS_PER_TICK, "环绕角速度。每拍读取")
                .numberParameter(BalanceKeys.OCCUPIED_VOLUME_LIMIT, RawDefault.literal(12.0D),
                        BalanceBound.strictlyPositive(1_000_000.0D), BalanceUnit.CUBIC_BLOCKS, "独立旋流入卷体积上限。组合成立时不读这份")
                .numberParameter(BalanceKeys.LIFTED_OCCUPIED_VOLUME_LIMIT, RawDefault.literal(48.0D),
                        BalanceBound.strictlyPositive(1_000_000.0D), BalanceUnit.CUBIC_BLOCKS, "水龙卷入卷体积上限。只在组合成立时读取")
                .numberParameter(BalanceKeys.ENTRY_COST, RawDefault.literal(10.0D),
                        BalanceBound.inclusive(0.0D, 100.0D), BalanceUnit.WATER_MOMENTUM, "基础入卷水势。独立旋流固定收这份；三四档毛费用也用它")
                .numberParameter(BalanceKeys.WET_TIER_1_ENTRY_COST, RawDefault.literal(8.0D),
                        BalanceBound.inclusive(0.0D, 100.0D), BalanceUnit.WATER_MOMENTUM, "龙卷对一档湿度的入卷水势。只在组合结算时读取")
                .numberParameter(BalanceKeys.WET_TIER_2_ENTRY_COST, RawDefault.literal(5.0D),
                        BalanceBound.inclusive(0.0D, 100.0D), BalanceUnit.WATER_MOMENTUM, "龙卷对二档湿度的入卷水势。只在组合结算时读取")
                .numberParameter(BalanceKeys.WET_TIER_3_REFUND, RawDefault.literal(13.0D),
                        BalanceBound.inclusive(0.0D, 100.0D), BalanceUnit.WATER_MOMENTUM, "龙卷对三档湿度的返还。先收 entryCost 再返还，结算后一次入账")
                .numberParameter(BalanceKeys.WET_TIER_4_REFUND, RawDefault.literal(16.0D),
                        BalanceBound.inclusive(0.0D, 100.0D), BalanceUnit.WATER_MOMENTUM, "龙卷对四档湿度的返还。改它会改变净收支")
                .numberParameter(BalanceKeys.FALLBACK_PROJECTILE_STANDARD_COST, RawDefault.literal(5.0D),
                        BalanceBound.strictlyPositive(100.0D), BalanceUnit.WATER_MOMENTUM, "没有正直伤标准报价的弹体兜底。偏转价再乘固定 0.2")
                .numberParameter(BalanceKeys.EJECT_HORIZONTAL_SPEED, RawDefault.literal(0.45D),
                        BalanceBound.positiveSpeed(), BalanceUnit.BLOCKS_PER_TICK, "到顶抛出的水平速度。只在真实抛出时读取")
                .numberParameter(BalanceKeys.EJECT_VERTICAL_SPEED, RawDefault.literal(0.20D),
                        BalanceBound.positiveSpeed(), BalanceUnit.BLOCKS_PER_TICK, "到顶抛出的向上速度。只在真实抛出时读取");
        SpecialBalance.publish(builder);
    }

    private static void declareBubbleBalance() {
        SpecialBalance.publish(SpecialBalance.manaFee(
                SpecialBalance.life(
                        SpecialBalance.blocks(
                                SpecialBalance.blocks(
                                        SpecialBalance.blocks(SpecialBalance.begin(BUBBLE_ID),
                                                BalanceKeys.BOX_WIDTH, BUBBLE_WIDTH, "气泡柱宽度"),
                                        BalanceKeys.BOX_DEPTH, BUBBLE_WIDTH, "气泡柱纵深"),
                                BalanceKeys.BOX_HEIGHT, TALL_HEIGHT, "气泡柱高度"),
                        DOMAIN_LIFE, "气泡寿命。打开时读取"),
                CONTACT_MANA_COST, CONTACT_UNIT, "领域接触附魔单价")
                .numberParameter(BalanceKeys.RADIUS, RawDefault.literal(BUBBLE_RADIUS),
                        BalanceBound.positiveSize(), BalanceUnit.BLOCKS, "气泡圆柱半径。每拍读取")
                .numberParameter(BalanceKeys.RISE_SPEED, RawDefault.literal(0.12D),
                        BalanceBound.positiveSpeed(), BalanceUnit.BLOCKS_PER_TICK, "上升速度。独立水柱和挂载后的螺旋都读这份"));
    }

    private static void declareVolume(String techniqueId, double height, boolean momentum, boolean slow, boolean breath) {
        com.bslx.XueXiMod.shu.balance.TechniqueBalanceSpec.Builder builder = SpecialBalance.manaFee(
                SpecialBalance.life(
                        SpecialBalance.blocks(
                                SpecialBalance.blocks(
                                        SpecialBalance.blocks(SpecialBalance.begin(techniqueId),
                                                BalanceKeys.BOX_WIDTH, SHARED_WIDTH, "领域宽度"),
                                        BalanceKeys.BOX_DEPTH, SHARED_DEPTH, "领域纵深"),
                                BalanceKeys.BOX_HEIGHT, height, "领域高度"),
                        DOMAIN_LIFE, "领域寿命"),
                CONTACT_MANA_COST, CONTACT_UNIT, "领域接触附魔单价");
        if (momentum || slow || breath) {
            builder = SpecialBalance.ticks(builder, WET_PERIOD, WET_PERIOD_TICKS, "领域潮湿周期");
        }
        if (momentum) {
            builder = SpecialBalance.ticks(builder, MOMENTUM_PERIOD, MOMENTUM_PERIOD_TICKS, "领域水势周期");
        }
        if (slow) {
            builder = SpecialBalance.ticks(builder, SLOW_PERIOD, SLOW_PERIOD_TICKS, "领域减速周期");
        }
        if (breath) {
            builder = builder.numberParameter(BREATH_MULTIPLIER, RawDefault.literal(BREATH_MULTIPLIER_RAW),
                    BalanceBound.multiplier(), BalanceUnit.MULTIPLIER, "深压耗氧倍率");
        }
        SpecialBalance.publish(builder);
    }
}
