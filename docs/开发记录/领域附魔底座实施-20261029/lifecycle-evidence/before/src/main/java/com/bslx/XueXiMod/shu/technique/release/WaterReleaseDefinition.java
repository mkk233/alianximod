package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierDefinition;
import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalEffect;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainDefinition;
import com.bslx.XueXiMod.shu.technique.release.guard.GuardDefinition;

/**
 * 水系成品的释放数据。结算不看术式编号，只读这些字段。
 */
public final class WaterReleaseDefinition implements ReleaseDefinition {
    private final String releaseId;
    private final Trajectory trajectory;
    private final boolean moving;
    private final int lifetimeTicks;
    private final float speed;
    private final float thickness;
    private boolean trajectoryMutable;
    private boolean acceptInitialAim;
    private boolean heldSword;
    private boolean hostGroupSubmit;
    private boolean arcBand;
    private boolean beatCycle;
    private boolean cohortVisual;
    private float energyScale = 1f;
    private int explicitCost = -1;
    private float fieldRadius;
    private int upkeepCost;
    private int fieldLife = 80;
    private float pullStrength;
    private int wetnessGrant;
    private float heavyPerCopy;
    private int pressureMinimum;
    private boolean purify;
    private boolean blocksMovement;
    private float pressurePulse;
    private float knockbackStrength;
    private float liftStrength;
    private float unloadRatio;
    private int slowTicks;
    private float endBurstDamage;
    private int bodyVisual = 5;
    private String curveFunction = "";
    private ReleaseShape shape = ReleaseShape.BOX;
    private float spanScale = 1f;
    private float lengthScale = 1f;
    private GuardDefinition guard;
    private BarrierDefinition barrier;
    private DomainDefinition domain;
    private ConditionalEffect conditionalEffect;
    private boolean deflectsIncoming;

    private WaterReleaseDefinition(String releaseId, Trajectory trajectory, boolean moving, int lifetimeTicks,
                                   float speed, float thickness, boolean trajectoryMutable) {
        this.releaseId = releaseId;
        this.trajectory = trajectory;
        this.moving = moving;
        this.lifetimeTicks = lifetimeTicks;
        this.speed = speed;
        this.thickness = thickness;
        this.trajectoryMutable = trajectoryMutable;
        this.acceptInitialAim = trajectoryMutable;
        this.heldSword = false;
    }

    /** 弹道锁定，仍接受出手瞬间的目标方向，并且必须手持剑。不改其他水系射击的默认值。 */
    public WaterReleaseDefinition swordForm() {
        this.trajectoryMutable = false;
        this.acceptInitialAim = true;
        this.heldSword = true;
        this.hostGroupSubmit = true;
        this.arcBand = true;
        this.beatCycle = true;
        this.cohortVisual = true;
        this.deflectsIncoming = true;
        return this;
    }

    public static WaterReleaseDefinition shot(String releaseId, Trajectory trajectory, int lifetimeTicks,
                                              float speed, float thickness) {
        return new WaterReleaseDefinition(releaseId, trajectory, true, lifetimeTicks, speed, thickness, true);
    }

    public static WaterReleaseDefinition zone(String releaseId, float radius, int lifetimeTicks, int manaCost, int upkeepCost) {
        WaterReleaseDefinition definition = new WaterReleaseDefinition(releaseId, Trajectory.STRAIGHT, false, lifetimeTicks, 0f, radius, false);
        definition.fieldRadius = radius;
        definition.fieldLife = lifetimeTicks;
        definition.explicitCost = manaCost;
        definition.upkeepCost = upkeepCost;
        definition.bodyVisual = 1;
        return definition;
    }

    public static WaterReleaseDefinition barrier(String releaseId, BarrierDefinition barrier) {
        WaterReleaseDefinition definition = new WaterReleaseDefinition(releaseId, Trajectory.STRAIGHT, false,
                barrier.lifetimeTicks(), 0f, 0f, false);
        definition.explicitCost = 0;
        definition.barrier = barrier;
        definition.fieldLife = barrier.lifetimeTicks();
        definition.upkeepCost = barrier.upkeepCost();
        return definition;
    }

    public static WaterReleaseDefinition domain(String releaseId, DomainDefinition domain) {
        WaterReleaseDefinition definition = new WaterReleaseDefinition(releaseId, Trajectory.STRAIGHT, false,
                domain.lifetimeTicks(), 0f, 0f, false);
        definition.explicitCost = 0;
        definition.domain = domain;
        definition.fieldLife = domain.lifetimeTicks();
        definition.upkeepCost = domain.upkeepCost();
        return definition;
    }

    public static WaterReleaseDefinition bodyGuard(String releaseId, GuardDefinition guard) {
        WaterReleaseDefinition definition = new WaterReleaseDefinition(releaseId, Trajectory.STRAIGHT, false,
                guard.lifetimeTicks(), 0f, 0f, false);
        definition.explicitCost = 0;
        definition.guard = guard;
        return definition;
    }

    /** 零伤条件释放。显式魔力只在非负时写下，不顺手变成领域、屏障或护体。 */
    public static WaterReleaseDefinition conditional(String releaseId, int manaCost, ConditionalEffect effect) {
        WaterReleaseDefinition definition = new WaterReleaseDefinition(releaseId, Trajectory.STRAIGHT, false,
                1, 0f, 0f, false);
        if (manaCost >= 0) {
            definition.explicitCost = manaCost;
        }
        definition.conditionalEffect = effect;
        return definition;
    }

    public WaterReleaseDefinition energy(float scale) {
        this.energyScale = scale;
        return this;
    }

    public WaterReleaseDefinition wetness(int amount) {
        this.wetnessGrant = amount;
        return this;
    }

    /** 每一份有效攻击预留的重水。非有限或负数留下原值。0 表示不走重水。 */
    public WaterReleaseDefinition heavyMomentum(float perCopy) {
        if (Float.isFinite(perCopy) && perCopy >= 0f) {
            this.heavyPerCopy = perCopy;
        }
        return this;
    }

    /** 重压的潮湿门槛，只接受 0 到 100。越界留下原值。0 表示不挂重压。 */
    public WaterReleaseDefinition pressureWetness(int minimum) {
        if (minimum >= 0 && minimum <= 100) {
            this.pressureMinimum = minimum;
        }
        return this;
    }

    public WaterReleaseDefinition pull(float strength) {
        this.pullStrength = strength;
        return this;
    }

    public WaterReleaseDefinition pressure(float amount) {
        this.pressurePulse = amount;
        return this;
    }

    public WaterReleaseDefinition knock(float strength) {
        this.knockbackStrength = strength;
        return this;
    }

    public WaterReleaseDefinition lift(float strength) {
        this.liftStrength = strength;
        return this;
    }

    public WaterReleaseDefinition unload(float ratio) {
        this.unloadRatio = ratio;
        return this;
    }

    public WaterReleaseDefinition slow(int ticks) {
        this.slowTicks = ticks;
        return this;
    }

    public WaterReleaseDefinition burst(float damage) {
        this.endBurstDamage = damage;
        return this;
    }

    public WaterReleaseDefinition purify() {
        this.purify = true;
        return this;
    }

    public WaterReleaseDefinition wall() {
        this.blocksMovement = true;
        return this;
    }

    public WaterReleaseDefinition visual(int visual) {
        this.bodyVisual = visual;
        return this;
    }

    public WaterReleaseDefinition curve(String functionId) {
        this.curveFunction = functionId == null ? "" : functionId;
        return this;
    }

    public WaterReleaseDefinition shape(ReleaseShape shape) {
        this.shape = shape;
        return this;
    }

    public WaterReleaseDefinition span(float scale) {
        this.spanScale = scale;
        return this;
    }

    public WaterReleaseDefinition along(float scale) {
        this.lengthScale = scale;
        return this;
    }

    @Override
    public String id() {
        return releaseId;
    }

    @Override
    public ReleaseKind kind() {
        return ReleaseKind.HIT_VOLUME;
    }

    @Override
    public ReleaseTag tag() {
        return ReleaseTag.TRIGGER;
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
        return lifetimeTicks;
    }

    @Override
    public float speed() {
        return speed;
    }

    @Override
    public float thickness() {
        return thickness;
    }

    @Override
    public boolean acceptsTargetOverride() {
        return trajectoryMutable;
    }

    @Override
    public boolean acceptsInitialAim() {
        return acceptInitialAim;
    }

    @Override
    public boolean requiresHeldSword() {
        return heldSword;
    }

    @Override
    public boolean submitsHostGroup() {
        return hostGroupSubmit;
    }

    @Override
    public boolean usesArcBand() {
        return arcBand;
    }

    @Override
    public boolean tracksBeatCycle() {
        return beatCycle;
    }

    @Override
    public boolean deflectsProjectiles() {
        if (deflectsIncoming) {
            return true;
        }
        if (barrier != null && barrier.deflectsProjectiles()) {
            return true;
        }
        return guard != null && guard.deflectsProjectiles();
    }

    @Override
    public boolean separatesCohortVisuals() {
        return cohortVisual;
    }

    @Override
    public boolean acceptsPathOverride() {
        return trajectoryMutable;
    }

    @Override
    public ChainRole role() {
        return ChainRole.ATTACK;
    }

    @Override
    public boolean trajectoryMutable() {
        return trajectoryMutable;
    }

    @Override
    public float energyScale() {
        return energyScale;
    }

    @Override
    public int explicitManaCost() {
        return explicitCost;
    }

    @Override
    public float fieldRadius() {
        return fieldRadius;
    }

    @Override
    public GuardDefinition guard() {
        return guard;
    }

    @Override
    public BarrierDefinition barrier() {
        return barrier;
    }

    @Override
    public DomainDefinition domain() {
        return domain;
    }

    @Override
    public ConditionalEffect conditionalEffect() {
        return conditionalEffect;
    }

    @Override
    public int fieldUpkeepCost() {
        return upkeepCost;
    }

    @Override
    public int fieldLifetimeTicks() {
        return fieldLife;
    }

    @Override
    public float pullStrength() {
        return pullStrength;
    }

    @Override
    public int wetnessGrant() {
        return wetnessGrant;
    }

    @Override
    public float heavyMomentumPerCopy() {
        return heavyPerCopy;
    }

    @Override
    public int pressureWetnessMinimum() {
        return pressureMinimum;
    }

    @Override
    public boolean purifyOnHit() {
        return purify;
    }

    @Override
    public boolean blocksMovement() {
        return blocksMovement;
    }

    @Override
    public float pressurePulse() {
        return pressurePulse;
    }

    @Override
    public float knockbackStrength() {
        return knockbackStrength;
    }

    @Override
    public float liftStrength() {
        return liftStrength;
    }

    @Override
    public float unloadRatio() {
        return unloadRatio;
    }

    @Override
    public int slowTicks() {
        return slowTicks;
    }

    @Override
    public float endBurstDamage() {
        return endBurstDamage;
    }

    @Override
    public int bodyVisual() {
        return bodyVisual;
    }

    @Override
    public String functionId() {
        return curveFunction;
    }

    @Override
    public ReleaseShape shape() {
        return shape;
    }

    @Override
    public float spanScale() {
        return spanScale;
    }

    @Override
    public float lengthScale() {
        return lengthScale;
    }
}
