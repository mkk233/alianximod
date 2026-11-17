package com.bslx.XueXiMod.shu.technique.release.cooperative;

import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueAvailability;
import com.bslx.XueXiMod.shu.TechniqueManager;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.shu.balance.BalanceBound;
import com.bslx.XueXiMod.shu.balance.BalanceRegistry;
import com.bslx.XueXiMod.shu.balance.BalanceUnit;
import com.bslx.XueXiMod.shu.balance.RawDefault;
import com.bslx.XueXiMod.shu.balance.TechniqueBalanceSpec;
import com.bslx.XueXiMod.shu.technique.TechniqueEffectRegistry;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.ReleaseTag;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.cooperative.action.ObliqueSlashEffect;
import com.bslx.XueXiMod.shu.technique.release.cooperative.action.SpinActionEffect;
import com.bslx.XueXiMod.shu.technique.release.cooperative.action.SpinTimelineDefinition;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeTechniqueIds;
import com.bslx.XueXiMod.shu.technique.release.cooperative.attack.HorizontalSlashActorAdapter;

/**
 * 登记本批术式、释放定义、适配器和只消费一次的专项。不调用效果的 execute。
 */
public final class CooperativeContent {
    public static final String SELECTION_RADIUS = "selectionRadius";
    public static final String MOVE_LIMIT = "moveLimit";
    public static final String SIDE_OFFSET = "sideOffset";
    public static final double DEFAULT_SELECTION_RADIUS = 24.0D;
    public static final double DEFAULT_MOVE_LIMIT = 32.0D;
    public static final double DEFAULT_SIDE_OFFSET = 1.25D;
    public static final int RULE_MANA_COST = 2;
    public static final int RULE_COOLDOWN_TICKS = 10;

    private static boolean specialsDeclared;

    private CooperativeContent() {
    }

    public static void register() {
        TechniqueManager.register(ObliqueSlashEffect.technique());
        TechniqueManager.register(SpinActionEffect.technique());
        TechniqueManager.register(ruleTechnique());
        ReleaseRegistry.register(ObliqueSlashEffect.releaseDefinition());
        ReleaseRegistry.register(SpinActionEffect.releaseDefinition());
        ReleaseRegistry.register(new CooperativeRuleRelease());
        TechniqueEffectRegistry.register(CooperativeTechniqueIds.OBLIQUE_SLASH, ObliqueSlashEffect.effect());
        TechniqueEffectRegistry.register(CooperativeTechniqueIds.SPIN, SpinActionEffect.effect());
        TechniqueEffectRegistry.register(CooperativeTechniqueIds.COOPERATIVE,
                (serverPlayer, listedTechnique, heldWeapon, castContext) -> {
                    throw new IllegalStateException("协同规则没有单独的旧效果");
                });
        CooperativeCapabilityIndex.remember(CooperativeTechniqueIds.OBLIQUE_SLASH, ObliqueSlashEffect.capabilities());
        CooperativeCapabilityIndex.remember(CooperativeTechniqueIds.SPIN, SpinActionEffect.capabilities());
        CooperativeCapabilityIndex.remember(CooperativeTechniqueIds.COOPERATIVE, TechniqueExecutionCapabilities.cooperativeRule());
        CooperativeCapabilityIndex.remember(CooperativeTechniqueIds.CLONE_BODY, TechniqueExecutionCapabilities.standaloneDeployment());
        CooperativeCapabilityIndex.remember(CooperativeTechniqueIds.HORIZONTAL_SLASH, TechniqueExecutionCapabilities.none());
        ActorAttackAdapterRegistry.register(CooperativeTechniqueIds.OBLIQUE_SLASH, ObliqueSlashEffect.adapter());
        ActorAttackAdapterRegistry.register(CooperativeTechniqueIds.HORIZONTAL_SLASH, HorizontalSlashActorAdapter.adapter());
    }

    public static void declareSpecials() {
        if (specialsDeclared) {
            return;
        }
        specialsDeclared = true;
        ObliqueSlashEffect.declareSpecialBalance();
        SpinTimelineDefinition.declareSpecialBalance();
        BalanceRegistry.declareSpecial(TechniqueBalanceSpec.builder(CooperativeTechniqueIds.COOPERATIVE)
                .numberParameter(SELECTION_RADIUS, RawDefault.literal(DEFAULT_SELECTION_RADIUS),
                        BalanceBound.inclusive(0, 48), BalanceUnit.BLOCKS, "协同选演员半径")
                .numberParameter(MOVE_LIMIT, RawDefault.literal(DEFAULT_MOVE_LIMIT),
                        BalanceBound.inclusive(0, 64), BalanceUnit.BLOCKS, "协同归位上限，对应运行时的 moveLimit")
                .numberParameter(SIDE_OFFSET, RawDefault.literal(DEFAULT_SIDE_OFFSET),
                        BalanceBound.inclusive(0.75, 2), BalanceUnit.BLOCKS, "协同演员侧向间距")
                .build());
    }

    public static Technique ruleTechnique() {
        return new Technique(
                CooperativeTechniqueIds.COOPERATIVE,
                "协同",
                TechniqueType.UNIVERSAL,
                0f,
                RULE_COOLDOWN_TICKS,
                1,
                "让下一段已适配攻击可由普通分身一起打出。本身不造成伤害，也不播放斩击。",
                0,
                ShuAttribute.NONE,
                1.0F,
                false,
                TechniqueAvailability.PLAYER);
    }

    private static final class CooperativeRuleRelease implements ReleaseDefinition {
        @Override
        public String id() {
            return CooperativeTechniqueIds.COOPERATIVE;
        }

        @Override
        public ReleaseKind kind() {
            return ReleaseKind.SELF_STATUS;
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
            return 1;
        }

        @Override
        public float speed() {
            return 0F;
        }

        @Override
        public float thickness() {
            return 0F;
        }

        @Override
        public ChainRole role() {
            return ChainRole.RULE_COOPERATIVE;
        }

        @Override
        public int explicitManaCost() {
            return RULE_MANA_COST;
        }
    }
}
