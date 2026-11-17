package com.bslx.XueXiMod.shu.technique.release.batch;

import com.bslx.XueXiMod.carrier.BatchCarrierIds;
import com.bslx.XueXiMod.shu.balance.BalanceBound;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.balance.BalanceUnit;
import com.bslx.XueXiMod.shu.balance.RawDefault;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.fire.FireLimits;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseTag;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainDefinition;

/**
 * 本批 13 项的释放登记。六个附魔只用 {@link ChainRole#NOT_APPLICABLE}，不登记成攻击。
 * 没有效果实现；载体仍由后续作者注册。
 */
public final class ListedRelease implements ReleaseDefinition {
    public static final String BIRTH_RADIUS = "birthRadius";
    public static final String INNER_RADIUS = "innerRadius";
    private final String releaseId;
    private final ReleaseKind kind;
    private final ReleaseTag tag;
    private final Trajectory trajectory;
    private final ChainRole role;
    private final boolean moving;
    private final int lifetimeTicks;
    private final float speed;
    private final float thickness;
    private final double horizontalRadius;
    private final double verticalHalf;
    private final boolean trajectoryMutable;
    private final boolean acceptsPathOverride;
    private final boolean acceptsTargetOverride;
    private final boolean batchQuote;
    private final int bodySeedBase;
    private final int assemblyCost;
    private final boolean earliestContact;
    private final DomainDefinition domain;

    private ListedRelease(String releaseId, ReleaseKind kind, ReleaseTag tag, Trajectory trajectory, ChainRole role,
                          boolean moving, int lifetimeTicks, float speed, float thickness,
                          double horizontalRadius, double verticalHalf, boolean trajectoryMutable,
                          boolean acceptsPathOverride, boolean acceptsTargetOverride, boolean batchQuote,
                          int bodySeedBase, int assemblyCost, boolean earliestContact) {
        this(releaseId, kind, tag, trajectory, role, moving, lifetimeTicks, speed, thickness,
                horizontalRadius, verticalHalf, trajectoryMutable, acceptsPathOverride, acceptsTargetOverride,
                batchQuote, bodySeedBase, assemblyCost, earliestContact, null);
    }

    private ListedRelease(String releaseId, ReleaseKind kind, ReleaseTag tag, Trajectory trajectory, ChainRole role,
                          boolean moving, int lifetimeTicks, float speed, float thickness,
                          double horizontalRadius, double verticalHalf, boolean trajectoryMutable,
                          boolean acceptsPathOverride, boolean acceptsTargetOverride, boolean batchQuote,
                          int bodySeedBase, int assemblyCost, boolean earliestContact, DomainDefinition domain) {
        this.releaseId = releaseId;
        this.kind = kind;
        this.tag = tag;
        this.trajectory = trajectory;
        this.role = role;
        this.moving = moving;
        this.lifetimeTicks = lifetimeTicks;
        this.speed = speed;
        this.thickness = thickness;
        this.horizontalRadius = horizontalRadius;
        this.verticalHalf = verticalHalf;
        this.trajectoryMutable = trajectoryMutable;
        this.acceptsPathOverride = acceptsPathOverride;
        this.acceptsTargetOverride = acceptsTargetOverride;
        this.batchQuote = batchQuote;
        this.bodySeedBase = bodySeedBase;
        this.assemblyCost = assemblyCost;
        this.earliestContact = earliestContact;
        this.domain = domain;
    }

    public static ListedRelease meteorite() {
        return attack("fire_meteorite", Trajectory.PARABOLA, true, 100, 0.8f, 1.0f, 0.5D, 0.5D,
                true, true, true, 0, -1, true);
    }

    public static ListedRelease shockRing() {
        return attack("fire_shock_ring", Trajectory.STRAIGHT, false, 12, 0.0f, 5.0f, 5.0D, 1.5D,
                false, false, false, 0, -1, false);
    }

    public static ListedRelease spark() {
        return attack("fire_spark", Trajectory.STRAIGHT, true, 30, 0.8f, 0.25f, 0.125D, 0.125D,
                true, true, true, 2, -1, true);
    }

    public static ListedRelease surfaceAnchor() {
        return rule("rule_surface_anchor", Trajectory.STRAIGHT, ChainRole.SEEK_SURFACE, false, 1, 0.0f);
    }

    public static ListedRelease upwell() {
        return rule("fire_upwell", Trajectory.UPWELL, ChainRole.TRAJECTORY_UPWELL, false, 1, 0.0f);
    }

    public static ListedRelease stagger() {
        return rule("rule_stagger", Trajectory.STRAIGHT, ChainRole.RULE_STAGGER, false, 1, 0.0f);
    }

    public static ListedRelease hitContinue() {
        return rule("rule_hit_continue", Trajectory.STRAIGHT, ChainRole.RULE_HIT_CONTINUE, false, 1, 0.0f);
    }

    public static ListedRelease enchant(String releaseId, ReleaseTag tag) {
        return new ListedRelease(releaseId, ReleaseKind.RULE, tag, Trajectory.STRAIGHT, ChainRole.NOT_APPLICABLE,
                false, 1, 0.0f, 0.0f, 0.0D, 0.0D, false, false, false, true, 0, -1, false);
    }

    /** 幽冥仍是持续附魔，同时带上自己的领域声明。其他附魔不走这里。 */
    public static ListedRelease gloomDomain(DomainDefinition domain) {
        return new ListedRelease("fire_nether_gloom", ReleaseKind.RULE, ReleaseTag.SUSTAINED, Trajectory.STRAIGHT,
                ChainRole.ATTACK, false, 1, 0.0f, 0.0f, 0.0D, 0.0D, false, false, false, false, 0, -1, false,
                domain);
    }

    private static ListedRelease attack(String releaseId, Trajectory trajectory, boolean moving, int lifetimeTicks,
                                        float speed, float thickness, double horizontalRadius, double verticalHalf,
                                        boolean trajectoryMutable, boolean acceptsPathOverride, boolean acceptsTargetOverride,
                                        int bodySeedBase, int assemblyCost, boolean earliestContact) {
        return new ListedRelease(releaseId, ReleaseKind.HIT_VOLUME, ReleaseTag.TRIGGER, trajectory, ChainRole.ATTACK,
                moving, lifetimeTicks, speed, thickness, horizontalRadius, verticalHalf, trajectoryMutable,
                acceptsPathOverride, acceptsTargetOverride, true, bodySeedBase, assemblyCost, earliestContact);
    }

    private static ListedRelease rule(String releaseId, Trajectory trajectory, ChainRole role, boolean moving,
                                      int lifetimeTicks, float speed) {
        return new ListedRelease(releaseId, ReleaseKind.RULE, ReleaseTag.TRIGGER, trajectory, role,
                moving, lifetimeTicks, speed, 0.0f, 0.0D, 0.0D, false, false, false, true, 0, 1, false);
    }

    @Override
    public String id() {
        return releaseId;
    }

    @Override
    public ReleaseKind kind() {
        return kind;
    }

    @Override
    public ReleaseTag tag() {
        return tag;
    }

    @Override
    public Trajectory trajectory() {
        return trajectory;
    }

    @Override
    public boolean moving() {
        return moving;
    }

    @Override
    public int lifetimeTicks() {
        if (lifetimeTicks <= 1) {
            return lifetimeTicks;
        }
        return SpecialBalance.integer(releaseId, BalanceKeys.LIFETIME_TICKS, lifetimeTicks);
    }

    @Override
    public float speed() {
        if (speed <= 0.0f) {
            return speed;
        }
        return SpecialBalance.decimal(releaseId, BalanceKeys.MOTION_SPEED, speed);
    }

    @Override
    public float thickness() {
        if (BatchCarrierIds.SHOCK_RING.equals(releaseId)) {
            return SpecialBalance.decimal(releaseId, BalanceKeys.RADIUS, thickness);
        }
        if (thickness <= 0.0f) {
            return thickness;
        }
        return SpecialBalance.decimal(releaseId, BalanceKeys.THICKNESS, thickness);
    }

    @Override
    public double birthHorizontalRadius() {
        if (BatchCarrierIds.SHOCK_RING.equals(releaseId)) {
            return SpecialBalance.number(releaseId, BalanceKeys.RADIUS, horizontalRadius);
        }
        if (horizontalRadius > 0.0d && horizontalRadius == verticalHalf) {
            return SpecialBalance.number(releaseId, BIRTH_RADIUS, horizontalRadius);
        }
        return horizontalRadius;
    }

    @Override
    public double birthVerticalHalf() {
        if (BatchCarrierIds.SHOCK_RING.equals(releaseId)) {
            return SpecialBalance.number(releaseId, INNER_RADIUS, verticalHalf);
        }
        if (horizontalRadius > 0.0d && horizontalRadius == verticalHalf) {
            return SpecialBalance.number(releaseId, BIRTH_RADIUS, verticalHalf);
        }
        return verticalHalf;
    }

    @Override
    public ChainRole role() {
        return role;
    }

    @Override
    public boolean trajectoryMutable() {
        return trajectoryMutable;
    }

    @Override
    public boolean acceptsPathOverride() {
        return acceptsPathOverride;
    }

    @Override
    public boolean acceptsTargetOverride() {
        return acceptsTargetOverride;
    }

    @Override
    public boolean declaresBatchQuote() {
        return batchQuote;
    }

    /** 自带领域声明的附魔也是领域宿主。普通附魔没有领域，不因此变成宿主。 */
    @Override
    public boolean explicitDomainHost() {
        return domain != null;
    }

    @Override
    public int bodyScaledSeedBase() {
        if (bodySeedBase <= 0) {
            return bodySeedBase;
        }
        return SpecialBalance.integer(releaseId, BalanceKeys.SEED_COUNT, bodySeedBase);
    }

    public static void declareSpecialBalances() {
        declareCarrier(meteorite(), false);
        declareCarrier(spark(), true);
        declareShock(shockRing());
    }

    private static void declareCarrier(ListedRelease release, boolean seeds) {
        com.bslx.XueXiMod.shu.balance.TechniqueBalanceSpec.Builder builder = SpecialBalance.blocks(
                SpecialBalance.thickness(
                        SpecialBalance.speed(
                                SpecialBalance.life(SpecialBalance.begin(release.releaseId), release.lifetimeTicks, "载体寿命"),
                                release.speed, "载体速度"),
                        release.thickness, "载体命中尺寸"),
                BIRTH_RADIUS, release.horizontalRadius, "载体出生半径");
        if (seeds) {
            builder = builder.integerParameter(BalanceKeys.SEED_COUNT, RawDefault.literal(release.bodySeedBase),
                    BalanceBound.count(0.0d, FireLimits.SEED_CAP), BalanceUnit.COUNT, "火苗本体产种基数");
        }
        SpecialBalance.publish(builder);
    }

    private static void declareShock(ListedRelease release) {
        SpecialBalance.publish(SpecialBalance.blocks(
                SpecialBalance.blocks(
                        SpecialBalance.life(
                                SpecialBalance.blocks(SpecialBalance.begin(release.releaseId),
                                        BalanceKeys.RADIUS, release.thickness, "热波外径"),
                                release.lifetimeTicks, "热波寿命"),
                        INNER_RADIUS, release.verticalHalf, "热波内径"),
                BalanceKeys.BOX_HEIGHT, BatchCarrierIds.RING_HEIGHT, "热波高度"));
    }

    @Override
    public int explicitAssemblyCost() {
        return assemblyCost;
    }

    @Override
    public boolean prefersEarliestContact() {
        return earliestContact;
    }

    @Override
    public DomainDefinition domain() {
        return domain;
    }
}
