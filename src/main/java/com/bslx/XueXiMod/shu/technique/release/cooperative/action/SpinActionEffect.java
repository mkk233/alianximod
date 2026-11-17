package com.bslx.XueXiMod.shu.technique.release.cooperative.action;

import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueAvailability;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseTag;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.cooperative.TechniqueExecutionCapabilities;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeTechniqueIds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 回旋的术式、释放定义和旧效果入口。
 * 计时以 {@link SpinTimelineDefinition} 为唯一来源。本效果不扣费、不冷却、不结算。
 */
public final class SpinActionEffect implements TechniqueEffect {
    public static final float BASE_DAMAGE = 0.0F;
    public static final int COOLDOWN_TICKS = 10;
    public static final int LEVEL_CAP = 1;
    public static final int EXPLICIT_MANA_COST = 2;

    private static final SpinActionEffect INSTANCE = new SpinActionEffect();
    private static final ReleaseDefinition DEFINITION = new SpinRelease();

    private SpinActionEffect() {
    }

    public static SpinActionEffect effect() {
        return INSTANCE;
    }

    public static Technique technique() {
        return new Technique(
                CooperativeTechniqueIds.SPIN,
                "回旋",
                TechniqueType.MOVEMENT,
                BASE_DAMAGE,
                COOLDOWN_TICKS,
                LEVEL_CAP,
                "原地定时动作，到点带出后续攻击，自身不造成伤害",
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
        return TechniqueExecutionCapabilities.timedRelease();
    }

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        throw new IllegalStateException(
                "回旋没有单独的效果执行端口。整合必须降低为 ExecutionFamilyPlan，并由 FamilyExecutionRuntime 驱动定时门；这里不能释放、扣费或结算。");
    }

    /**
     * 没有命中体。寿命固定为 1，避免被通用运动声明当成另一套时长。
     * 真正的前摇和收势只在时间线规格里。
     */
    private static final class SpinRelease implements ReleaseDefinition {
        @Override
        public String id() {
            return CooperativeTechniqueIds.SPIN;
        }

        @Override
        public ReleaseKind kind() {
            return ReleaseKind.ACTION;
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
            return 0.0F;
        }

        @Override
        public float thickness() {
            return 0.0F;
        }

        @Override
        public ChainRole role() {
            return ChainRole.ACTION;
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
        public int explicitManaCost() {
            return EXPLICIT_MANA_COST;
        }
    }
}
