package com.bslx.XueXiMod.shu.technique.release.cooperative.action;

import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueAvailability;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.shu.balance.BalanceBound;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.balance.BalanceUnit;
import com.bslx.XueXiMod.shu.balance.RawDefault;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseTag;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.release.cooperative.TechniqueExecutionCapabilities;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ActorAttackAdapter;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.AdapterVolumeSpec;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeActionIds;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeStructureLimits;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeTechniqueIds;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.MovementPolicy;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ReleaseGate;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ReleaseGateKind;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.TimelineSpec;
import com.bslx.XueXiMod.shu.technique.release.cooperative.attack.ObliqueSlashAdapter;
import com.bslx.XueXiMod.shu.technique.release.cooperative.attack.ServerSpecialRead;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 斜斩的术式、释放定义、专项声明和旧效果入口。
 * 几何由适配器准备。配置成零伤害时链角色仍是攻击。
 */
public final class ObliqueSlashEffect implements TechniqueEffect {
    public static final String BOX_THICKNESS = "boxThickness";
    public static final String SLASH_TILT_DEGREES = "slashTiltDegrees";
    public static final double DEFAULT_BOX_DEPTH = 3.0D;
    public static final double DEFAULT_BOX_WIDTH = 5.0D;
    public static final double DEFAULT_BOX_THICKNESS = 0.5D;
    public static final int DEFAULT_LIFETIME_TICKS = 4;
    public static final double DEFAULT_TILT_DEGREES = 35.0D;
    public static final float BASE_DAMAGE = 4.0F;
    public static final int COOLDOWN_TICKS = 20;
    public static final int LEVEL_CAP = 20;
    public static final int EXPLICIT_MANA_COST = 4;

    private static final ObliqueSlashEffect INSTANCE = new ObliqueSlashEffect();
    private static final ReleaseDefinition DEFINITION = new ObliqueSlashRelease();

    private ObliqueSlashEffect() {
    }

    public static ObliqueSlashEffect effect() {
        return INSTANCE;
    }

    public static Technique technique() {
        return new Technique(
                CooperativeTechniqueIds.OBLIQUE_SLASH,
                "斩击",
                TechniqueType.SWORD,
                BASE_DAMAGE,
                COOLDOWN_TICKS,
                LEVEL_CAP,
                "眼前定向斜向斩面，可单独释放",
                0,
                ShuAttribute.NONE,
                1.0F,
                false,
                TechniqueAvailability.PLAYER);
    }

    public static ReleaseDefinition releaseDefinition() {
        return DEFINITION;
    }

    public static TechniqueExecutionCapabilities capabilities() {
        return TechniqueExecutionCapabilities.explicitImmediateAttack();
    }

    public static ActorAttackAdapter adapter() {
        return ObliqueSlashAdapter.adapter();
    }

    public static ResourceLocation presentationId() {
        return CooperativeActionIds.OBLIQUE_SLASH;
    }

    /** 单独斩击的结构门：出刀刻 0，收势 8。这不是回旋的平衡键。 */
    public static ReleaseGate immediateGate() {
        TimelineSpec stance = new TimelineSpec(
                CooperativeStructureLimits.IMMEDIATE_SLASH_RELEASE_TICK,
                CooperativeStructureLimits.IMMEDIATE_SLASH_RECOVERY_TICKS,
                MovementPolicy.IN_PLACE);
        return new ReleaseGate(ReleaseGateKind.IMMEDIATE, stance);
    }

    /**
     * 构建攻击准备时读一次，再把返回值冻进 {@code AttackPreparation}。
     * 适配器之后只使用那份规格，不再读配置。
     */
    public static AdapterVolumeSpec serverVolume() {
        String techniqueId = CooperativeTechniqueIds.OBLIQUE_SLASH;
        return new AdapterVolumeSpec(
                ServerSpecialRead.number(techniqueId, BalanceKeys.BOX_DEPTH, DEFAULT_BOX_DEPTH),
                ServerSpecialRead.number(techniqueId, BalanceKeys.BOX_WIDTH, DEFAULT_BOX_WIDTH),
                ServerSpecialRead.number(techniqueId, BOX_THICKNESS, DEFAULT_BOX_THICKNESS),
                ServerSpecialRead.integer(techniqueId, BalanceKeys.LIFETIME_TICKS, DEFAULT_LIFETIME_TICKS),
                ServerSpecialRead.number(techniqueId, SLASH_TILT_DEGREES, DEFAULT_TILT_DEGREES));
    }

    /**
     * 五项专项。标准魔力、冷却和伤害倍率由封存补上，这里不重复。
     * 倾角没有角度枚举，单位用现有无量纲倍率，描述里写明是度。
     */
    public static void declareSpecialBalance() {
        SpecialBalance.publish(SpecialBalance.begin(CooperativeTechniqueIds.OBLIQUE_SLASH)
                .numberParameter(BalanceKeys.BOX_DEPTH, RawDefault.literal(DEFAULT_BOX_DEPTH),
                        BalanceBound.positiveSize(), BalanceUnit.BLOCKS, "斜斩纵深")
                .numberParameter(BalanceKeys.BOX_WIDTH, RawDefault.literal(DEFAULT_BOX_WIDTH),
                        BalanceBound.positiveSize(), BalanceUnit.BLOCKS, "斜斩宽度")
                .numberParameter(BOX_THICKNESS, RawDefault.literal(DEFAULT_BOX_THICKNESS),
                        BalanceBound.positiveSize(), BalanceUnit.BLOCKS, "斜斩厚度")
                .integerParameter(BalanceKeys.LIFETIME_TICKS, RawDefault.literal(DEFAULT_LIFETIME_TICKS),
                        BalanceBound.positiveTicks(), BalanceUnit.TICKS, "斜斩寿命")
                .numberParameter(SLASH_TILT_DEGREES, RawDefault.literal(DEFAULT_TILT_DEGREES),
                        BalanceBound.inclusive(CooperativeStructureLimits.TILT_LOW, CooperativeStructureLimits.TILT_HIGH),
                        BalanceUnit.MULTIPLIER, "刀面相对水平绕瞄准轴的倾角，度"));
    }

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        throw new IllegalStateException(
                "斜斩没有单独的效果执行端口。整合必须降低为 ExecutionFamilyPlan，再由 FamilyExecutionRuntime 提交几何；这里不能生成体积或结算伤害。");
    }

    private static final class ObliqueSlashRelease implements ReleaseDefinition {
        @Override
        public String id() {
            return CooperativeTechniqueIds.OBLIQUE_SLASH;
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
            return Trajectory.STRAIGHT;
        }

        @Override
        public boolean moving() {
            return false;
        }

        @Override
        public int lifetimeTicks() {
            return serverVolume().lifetimeTicks();
        }

        @Override
        public float speed() {
            return 0.0F;
        }

        @Override
        public float thickness() {
            return (float) serverVolume().boxThickness();
        }

        @Override
        public ChainRole role() {
            return ChainRole.ATTACK;
        }

        @Override
        public boolean trajectoryMutable() {
            return false;
        }

        @Override
        public boolean acceptsPathOverride() {
            return false;
        }

        @Override
        public boolean manipulatesSwordEntities() {
            return false;
        }

        @Override
        public boolean separatesCohortVisuals() {
            return true;
        }

        @Override
        public int explicitManaCost() {
            return EXPLICIT_MANA_COST;
        }
    }
}
