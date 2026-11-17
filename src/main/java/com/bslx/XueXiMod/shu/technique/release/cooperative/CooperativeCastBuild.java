package com.bslx.XueXiMod.shu.technique.release.cooperative;

import com.bslx.XueXiMod.shu.Technique;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** 已经降好、还没扣费的一次释放。拒绝和魔力不足都停在扣费之前。 */
public final class CooperativeCastBuild {
    private final @Nullable ExecutionFamilyPlan plan;
    private final @Nullable CooperativeCastRecipe recipe;
    private final @Nullable String refusalKey;
    private final boolean insufficientMana;
    private final boolean deferredCooldown;
    private final int quotedCooldownTicks;
    private final List<Technique> fired;

    private CooperativeCastBuild(@Nullable ExecutionFamilyPlan plan, @Nullable CooperativeCastRecipe recipe,
                                 @Nullable String refusalKey, boolean insufficientMana, boolean deferredCooldown,
                                 int quotedCooldownTicks, List<Technique> fired) {
        this.plan = plan;
        this.recipe = recipe;
        this.refusalKey = refusalKey;
        this.insufficientMana = insufficientMana;
        this.deferredCooldown = deferredCooldown;
        this.quotedCooldownTicks = quotedCooldownTicks;
        this.fired = fired;
    }

    public static CooperativeCastBuild ready(ExecutionFamilyPlan plan, CooperativeCastRecipe recipe,
                                             boolean deferredCooldown, int quotedCooldownTicks, List<Technique> fired) {
        return new CooperativeCastBuild(plan, recipe, null, false, deferredCooldown, quotedCooldownTicks, List.copyOf(fired));
    }

    public static CooperativeCastBuild refused(String refusalKey) {
        return new CooperativeCastBuild(null, null, refusalKey, false, false, 0, List.of());
    }

    public static CooperativeCastBuild unaffordable() {
        return new CooperativeCastBuild(null, null, null, true, false, 0, List.of());
    }

    public boolean accepted() {
        return plan != null && recipe != null;
    }

    public @Nullable ExecutionFamilyPlan plan() {
        return plan;
    }

    public @Nullable CooperativeCastRecipe recipe() {
        return recipe;
    }

    public @Nullable String refusalKey() {
        return refusalKey;
    }

    public boolean insufficientMana() {
        return insufficientMana;
    }

    public boolean deferredCooldown() {
        return deferredCooldown;
    }

    public int quotedCooldownTicks() {
        return quotedCooldownTicks;
    }

    public List<Technique> fired() {
        return fired;
    }
}
