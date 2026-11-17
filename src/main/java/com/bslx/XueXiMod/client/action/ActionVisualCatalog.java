package com.bslx.XueXiMod.client.action;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.bslx.XueXiMod.animation.core.AnimationClip;
import com.bslx.XueXiMod.animation.core.Pose;
import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.math.Vec3f;
import com.bslx.XueXiMod.animation.core.model.Armature;
import com.bslx.XueXiMod.animation.runtime.ActionDefinition;
import com.bslx.XueXiMod.animation.runtime.ActionRegistry;
import com.bslx.XueXiMod.animation.runtime.OriginalHumanoid;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeActionIds;

import net.minecraft.resources.ResourceLocation;

/**
 * 按 visualActionId 登记类别、适用手和挥斩配方。类别不要求配方。
 * 参考 F/N 只对带配方的动作，在剪辑和骨架对象更换前算一次。退化则标未校准，不回退到通用参考架。
 * 授权剪辑的派生向量只留在这份缓存里。
 */
public final class ActionVisualCatalog {
    private static final float AREA_FLOOR = 1.0E-3F;
    private static final Map<ResourceLocation, Recipe> RECIPES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, CachedPlane> CACHE = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Integer> CALIBRATIONS = new LinkedHashMap<>();

    // 适用手来自 epicfight 9d25f19c，不按 id 子串分类。
    // 双手：相位同时点名 toolR 与 toolL，或该动作只属于双手各持一把的双持生活、格挡、空斩和刃突。
    // 左手：相位只点名 toolL。其余授权剑刀是右手，包括双手握持同一把，以及只打右手的双持连段。
    private static final String[] ROLL_PATHS = {
            "biped/skill/roll_forward",
            "biped/skill/roll_backward",
    };

    private static final String[] BOTH_HAND_PATHS = {
            "biped/living/hold_dual",
            "biped/living/run_dual",
            "biped/skill/guard_dualsword",
            "biped/combat/sword_dual_auto3",
            "biped/combat/sword_dual_dash",
            "biped/combat/dagger_dual_auto4",
            "biped/combat/dagger_dual_dash",
            "biped/combat/sword_dual_airslash",
            "biped/combat/dagger_dual_airslash",
            "biped/combat/mob_sword_dual1",
            "biped/combat/mob_sword_dual2",
            "biped/combat/mob_dagger_twohand1",
            "biped/skill/guard_dualsword_hit",
            "biped/skill/dancing_edge",
            "biped/skill/blade_rush_combo1",
            "biped/skill/blade_rush_combo2",
            "biped/skill/blade_rush_combo3",
            "biped/interact/blade_rush_hit",
            "biped/skill/blade_rush_execute",
            "biped/skill/blade_rush_failed",
            "biped/skill/blade_rush_try",
    };

    private static final String[] LEFT_HAND_PATHS = {
            "biped/combat/sword_dual_auto2",
            "biped/combat/dagger_dual_auto2",
    };

    private static final String[] RIGHT_HAND_PATHS = {
            "biped/living/hold_greatsword",
            "biped/living/hold_uchigatana_sheath",
            "biped/living/hold_uchigatana",
            "biped/living/hold_tachi",
            "biped/living/hold_longsword",
            "biped/living/hold_liechtenauer",
            "biped/living/walk_greatsword",
            "biped/living/walk_uchigatana_sheath",
            "biped/living/walk_uchigatana",
            "biped/living/walk_longsword",
            "biped/living/walk_liechtenauer",
            "biped/living/run_greatsword",
            "biped/living/run_uchigatana",
            "biped/living/run_uchigatana_sheath",
            "biped/living/run_longsword",
            "biped/living/uchigatana_scrap",
            "biped/living/liechtenauer_ready",
            "biped/skill/guard_sword",
            "biped/skill/guard_greatsword",
            "biped/skill/guard_uchigatana",
            "biped/skill/guard_longsword",
            "biped/skill/steel_whirlwind_charging",
            "biped/combat/sword_auto1",
            "biped/combat/sword_auto2",
            "biped/combat/sword_auto3",
            "biped/combat/sword_dash",
            "biped/combat/greatsword_auto1",
            "biped/combat/greatsword_auto2",
            "biped/combat/greatsword_dash",
            "biped/combat/sword_dual_auto1",
            "biped/combat/uchigatana_auto1",
            "biped/combat/uchigatana_auto2",
            "biped/combat/uchigatana_auto3",
            "biped/combat/uchigatana_dash",
            "biped/combat/uchigatana_sheath_auto",
            "biped/combat/uchigatana_sheath_dash",
            "biped/combat/longsword_auto1",
            "biped/combat/longsword_auto2",
            "biped/combat/longsword_auto3",
            "biped/combat/longsword_dash",
            "biped/combat/longsword_liechtenauer_auto1",
            "biped/combat/longsword_liechtenauer_auto2",
            "biped/combat/longsword_liechtenauer_auto3",
            "biped/combat/tachi_auto1",
            "biped/combat/tachi_auto2",
            "biped/combat/tachi_auto3",
            "biped/combat/tachi_dash",
            "biped/combat/dagger_auto1",
            "biped/combat/dagger_auto2",
            "biped/combat/dagger_auto3",
            "biped/combat/dagger_dash",
            "biped/combat/dagger_dual_auto1",
            "biped/combat/dagger_dual_auto3",
            "biped/combat/sword_airslash",
            "biped/combat/uchigatana_airslash",
            "biped/combat/uchigatana_sheath_airslash",
            "biped/combat/longsword_airslash",
            "biped/combat/greatsword_airslash",
            "biped/combat/dagger_airslash",
            "biped/combat/sword_mount_attack",
            "biped/combat/mob_greatsword1",
            "biped/combat/mob_tachi_special",
            "biped/combat/mob_sword_dual3",
            "biped/combat/mob_longsword1",
            "biped/combat/mob_longsword2",
            "biped/combat/mob_uchigatana1",
            "biped/combat/mob_uchigatana2",
            "biped/combat/mob_uchigatana3",
            "biped/combat/mob_dagger_onehand1",
            "biped/combat/mob_dagger_onehand2",
            "biped/combat/mob_dagger_onehand3",
            "biped/combat/mob_dagger_twohand2",
            "biped/skill/guard_sword_hit",
            "biped/skill/guard_sword_hit_active1",
            "biped/skill/guard_sword_hit_active2",
            "biped/skill/guard_sword_hit_active3",
            "biped/skill/guard_longsword_hit_active1",
            "biped/skill/guard_longsword_hit_active2",
            "biped/skill/guard_break2",
            "biped/skill/guard_longsword_hit",
            "biped/skill/guard_greatsword_hit",
            "biped/skill/guard_uchigatana_hit",
            "biped/skill/greatsword_slam",
            "biped/skill/sweeping_edge",
            "biped/skill/steel_whirlwind",
            "biped/skill/battojutsu",
            "biped/skill/battojutsu_dash",
            "biped/skill/rushing_tempo1",
            "biped/skill/rushing_tempo2",
            "biped/skill/rushing_tempo3",
            "biped/skill/eviscerate_first",
            "biped/skill/eviscerate_second",
            "biped/skill/sharp_stab",
    };

    static {
        installHands(RIGHT_HAND_PATHS, true, false);
        installHands(LEFT_HAND_PATHS, false, true);
        installHands(BOTH_HAND_PATHS, true, true);
        installRolls(ROLL_PATHS);
        registerBlade(OriginalHumanoid.LEFT_ID, true, false, new float[] {0.30F, 0.45F, 0.60F}, false,
                SlashAlignmentScope.UPPER_BODY_SLASH,
                "原创左斜。三拍各自有蓄力、击中、收势，采样在 30%、45%、60%。没有武器骨，轴是重定向后的右臂方向。前向与模型 -Z 同半球。");
        registerBlade(OriginalHumanoid.RIGHT_ID, true, false, new float[] {0.30F, 0.45F, 0.60F}, false,
                SlashAlignmentScope.UPPER_BODY_SLASH,
                "原创右斜。三拍各自有蓄力、击中、收势，采样在 30%、45%、60%。平面符号由这条负滚转产生，不另选相位。轴是重定向后的右臂方向。前向与模型 -Z 同半球。");
        registerBlade(OriginalHumanoid.HORIZONTAL_ID, true, false, new float[] {0.30F, 0.45F, 0.60F}, false,
                SlashAlignmentScope.UPPER_BODY_SLASH,
                "原创水平。三拍各自有蓄力、击中、收势，采样在 30%、45%、60%。轴是重定向后的右臂方向。前向与模型 -Z 同半球。");
        registerBlade(OriginalHumanoid.SPACE_CUT_ID, true, false, SlashAlignmentScope.UPPER_BODY_SLASH);
        registerBlade(CooperativeActionIds.OBLIQUE_SLASH, true, false, SlashAlignmentScope.UPPER_BODY_SLASH);
        registerBlade(CooperativeActionIds.SPIN, true, false, SlashAlignmentScope.UPPER_BODY_SLASH);
        registerBlade(ResourceLocation.fromNamespaceAndPath("epicfight", "biped/combat/sword_auto1"), true, false,
                new float[] {0.0F, 0.1F / 0.6F, 1.0F}, false,
                "sword_auto1 的 Tool_R、Arm_R、Hand_R 关键帧都是 0、0.1、0.6 秒，时长 0.6。三点就是这三帧。轴是重定向后的右臂下垂轴，也就是手到武器的方向。前向与模型 -Z 同半球。");
        registerBlade(ResourceLocation.fromNamespaceAndPath("epicfight", "biped/combat/sword_auto2"), true, false,
                new float[] {0.0833F / 0.6F, 0.2167F / 0.6F, 1.0F}, false,
                "sword_auto2 的 Tool_R 关键帧是 0、0.0833、0.1、0.2167、0.6 秒。取入斩 0.0833、击中 0.2167 和收势 0.6，不用与入斩只差 0.0167 秒的 0.1。轴是右臂下垂轴。前向与模型 -Z 同半球。");
        registerBlade(ResourceLocation.fromNamespaceAndPath("epicfight", "biped/combat/sword_auto3"), true, false,
                new float[] {0.0F, 0.2F / 0.6F, 1.0F}, false,
                "sword_auto3 的 Tool_R 只有 0、0.2、0.6 秒三帧，全部使用。轴是右臂下垂轴。前向与模型 -Z 同半球。");
    }

    private ActionVisualCatalog() {
    }

    private static void installHands(String[] paths, boolean rightHand, boolean leftHand) {
        for (int pathIndex = 0; pathIndex < paths.length; pathIndex++) {
            registerBlade(epicfight(paths[pathIndex]), rightHand, leftHand);
        }
    }

    private static void installRolls(String[] paths) {
        for (int pathIndex = 0; pathIndex < paths.length; pathIndex++) {
            registerRoll(epicfight(paths[pathIndex]), "授权白名单里的翻滚。类别是 roll，不是 blade。");
        }
    }

    private static ResourceLocation epicfight(String path) {
        return ResourceLocation.fromNamespaceAndPath("epicfight", path);
    }

    public static void registerBlade(
            ResourceLocation visualId,
            boolean rightHand,
            boolean leftHand,
            float[] sampleFractions,
            boolean lateralAxis,
            String basisNote) {
        registerBlade(visualId, rightHand, leftHand, sampleFractions, lateralAxis, SlashAlignmentScope.LEGACY, basisNote);
    }

    public static void registerBlade(
            ResourceLocation visualId,
            boolean rightHand,
            boolean leftHand,
            float[] sampleFractions,
            boolean lateralAxis,
            SlashAlignmentScope alignmentScope,
            String basisNote) {
        if (visualId == null) {
            throw new IllegalArgumentException("visual id is missing");
        }
        if (sampleFractions == null || sampleFractions.length != 3) {
            throw new IllegalArgumentException("slash recipe needs three sample times");
        }
        float[] copied = Arrays.copyOf(sampleFractions, 3);
        for (int sampleIndex = 0; sampleIndex < copied.length; sampleIndex++) {
            if (!Float.isFinite(copied[sampleIndex]) || copied[sampleIndex] < 0.0F || copied[sampleIndex] > 1.0F) {
                throw new IllegalArgumentException("slash sample time is outside the clip");
            }
        }
        RECIPES.put(visualId, new Recipe(
                VisualKind.BLADE,
                rightHand,
                leftHand,
                copied,
                lateralAxis,
                true,
                alignmentScope == null ? SlashAlignmentScope.LEGACY : alignmentScope,
                note(basisNote)));
        CACHE.remove(visualId);
    }

    /** 只登记类别和适用手；已有三点配方时保留其采样与轴向，仅换 scope。 */
    public static void registerBlade(ResourceLocation visualId, boolean rightHand, boolean leftHand, SlashAlignmentScope scope) {
        if (visualId == null) {
            throw new IllegalArgumentException("visual id is missing");
        }
        SlashAlignmentScope resolvedScope = scope == null ? SlashAlignmentScope.LEGACY : scope;
        Recipe existing = RECIPES.get(visualId);
        if (existing != null && existing.kind == VisualKind.BLADE && existing.sampleFractions.length == 3) {
            RECIPES.put(visualId, new Recipe(
                    VisualKind.BLADE,
                    rightHand,
                    leftHand,
                    existing.sampleFractions,
                    existing.lateralAxis,
                    existing.faceNegativeAhead,
                    resolvedScope,
                    existing.basisNote));
        } else {
            RECIPES.put(visualId, new Recipe(
                    VisualKind.BLADE, rightHand, leftHand, new float[0], false, false, resolvedScope, ""));
        }
        CACHE.remove(visualId);
    }

    public static void registerBlade(ResourceLocation visualId, boolean rightHand, boolean leftHand) {
        if (visualId == null) {
            throw new IllegalArgumentException("visual id is missing");
        }
        Recipe existing = RECIPES.get(visualId);
        if (existing != null && existing.kind == VisualKind.BLADE && existing.sampleFractions.length == 3) {
            RECIPES.put(visualId, new Recipe(
                    VisualKind.BLADE,
                    rightHand,
                    leftHand,
                    existing.sampleFractions,
                    existing.lateralAxis,
                    existing.faceNegativeAhead,
                    existing.alignmentScope,
                    existing.basisNote));
            return;
        }
        RECIPES.put(visualId, new Recipe(
                VisualKind.BLADE, rightHand, leftHand, new float[0], false, false, SlashAlignmentScope.LEGACY, ""));
        CACHE.remove(visualId);
    }

    public static void registerRoll(ResourceLocation visualId, String basisNote) {
        if (visualId == null) {
            throw new IllegalArgumentException("visual id is missing");
        }
        RECIPES.put(visualId, new Recipe(
                VisualKind.ROLL, false, false, new float[0], false, false, SlashAlignmentScope.LEGACY, note(basisNote)));
        CACHE.remove(visualId);
    }

    public static void invalidate() {
        CACHE.clear();
    }

    public static boolean blade(ResourceLocation visualId) {
        Recipe recipe = RECIPES.get(visualId);
        return recipe != null && recipe.kind == VisualKind.BLADE;
    }

    public static boolean hasRecipe(ResourceLocation visualId) {
        Recipe recipe = RECIPES.get(visualId);
        return recipe != null && recipe.kind == VisualKind.BLADE && recipe.sampleFractions.length == 3;
    }

    public static boolean roll(ResourceLocation visualId) {
        Recipe recipe = RECIPES.get(visualId);
        return recipe != null && recipe.kind == VisualKind.ROLL;
    }

    public static boolean usesHand(ResourceLocation visualId, boolean rightHand) {
        Recipe recipe = RECIPES.get(visualId);
        if (recipe == null || recipe.kind != VisualKind.BLADE) {
            return false;
        }
        return rightHand ? recipe.rightHand : recipe.leftHand;
    }

    public static boolean lateralAxis(ResourceLocation visualId) {
        Recipe recipe = RECIPES.get(visualId);
        return recipe != null && recipe.lateralAxis;
    }

    public static SlashAlignmentScope alignmentScope(ResourceLocation visualId) {
        Recipe recipe = RECIPES.get(visualId);
        return recipe == null ? SlashAlignmentScope.LEGACY : recipe.alignmentScope;
    }

    public static float[] fractions(ResourceLocation visualId) {
        Recipe recipe = RECIPES.get(visualId);
        if (recipe == null) {
            return new float[0];
        }
        return Arrays.copyOf(recipe.sampleFractions, recipe.sampleFractions.length);
    }

    public static String basis(ResourceLocation visualId) {
        Recipe recipe = RECIPES.get(visualId);
        return recipe == null ? "" : recipe.basisNote;
    }

    public static int calibrationCount(ResourceLocation visualId) {
        Integer count = CALIBRATIONS.get(visualId);
        return count == null ? 0 : count;
    }

    public static Vec3f referenceForward(ResourceLocation visualId) {
        CachedPlane plane = resolve(visualId);
        return plane == null ? null : plane.forward;
    }

    public static Vec3f referenceNormal(ResourceLocation visualId) {
        CachedPlane plane = resolve(visualId);
        return plane == null ? null : plane.normal;
    }

    public static boolean calibrated(ResourceLocation visualId, AnimationClip clip, Armature armature) {
        CachedPlane plane = refresh(visualId, clip, armature);
        return plane != null && plane.forward != null && plane.normal != null;
    }

    private static CachedPlane resolve(ResourceLocation visualId) {
        if (!hasRecipe(visualId)) {
            return null;
        }
        AnimationClip clip = ActionRegistry.clip(visualId);
        ActionDefinition definition = ActionRegistry.definition(visualId);
        Armature armature = definition == null ? null : ActionRegistry.armature(definition.armatureName());
        return refresh(visualId, clip, armature);
    }

    private static CachedPlane refresh(ResourceLocation visualId, AnimationClip clip, Armature armature) {
        Recipe recipe = RECIPES.get(visualId);
        if (recipe == null || recipe.kind != VisualKind.BLADE || recipe.sampleFractions.length != 3) {
            return null;
        }
        CachedPlane cached = CACHE.get(visualId);
        if (cached != null && cached.clip == clip && cached.armature == armature) {
            return cached;
        }
        CachedPlane fitted = fit(recipe, clip, armature);
        CACHE.put(visualId, fitted);
        Integer previous = CALIBRATIONS.get(visualId);
        CALIBRATIONS.put(visualId, previous == null ? 1 : previous + 1);
        return fitted;
    }

    private static CachedPlane fit(Recipe recipe, AnimationClip clip, Armature armature) {
        CachedPlane missing = new CachedPlane(clip, armature, null, null);
        if (clip == null || armature == null || !(clip.getClipTime() > 0.0F) || recipe.sampleFractions.length != 3) {
            return missing;
        }
        RetargetProfile profile = RetargetProfile.forArmature(armature);
        if (profile == null) {
            return missing;
        }
        float across = recipe.lateralAxis ? 1.0F : 0.0F;
        float upward = recipe.lateralAxis ? 0.0F : 1.0F;
        Vec3f early = sampleAxis(clip, armature, profile, recipe.sampleFractions[0], across, upward);
        Vec3f middle = sampleAxis(clip, armature, profile, recipe.sampleFractions[1], across, upward);
        Vec3f late = sampleAxis(clip, armature, profile, recipe.sampleFractions[2], across, upward);
        if (early == null || middle == null || late == null) {
            return missing;
        }
        Vec3f spanned = MatrixFrames.cross(subtract(middle, early), subtract(late, middle));
        if (!(MatrixFrames.length(spanned.coordinateX, spanned.coordinateY, spanned.coordinateZ) > AREA_FLOOR)) {
            return missing;
        }
        Vec3f travel = MatrixFrames.normalized(subtract(late, early));
        Vec3f normal = MatrixFrames.normalized(spanned);
        if (travel == null || normal == null) {
            return missing;
        }
        Vec3f forward = MatrixFrames.normalized(MatrixFrames.cross(travel, normal));
        if (forward == null) {
            return missing;
        }
        if (recipe.faceNegativeAhead && forward.coordinateZ > 0.0F) {
            normal = new Vec3f(-normal.coordinateX, -normal.coordinateY, -normal.coordinateZ);
            forward = MatrixFrames.normalized(MatrixFrames.cross(travel, normal));
        }
        if (forward == null || normal == null) {
            return missing;
        }
        float along = forward.coordinateX * normal.coordinateX
                + forward.coordinateY * normal.coordinateY
                + forward.coordinateZ * normal.coordinateZ;
        forward = MatrixFrames.normalized(new Vec3f(
                forward.coordinateX - normal.coordinateX * along,
                forward.coordinateY - normal.coordinateY * along,
                forward.coordinateZ - normal.coordinateZ * along));
        if (forward == null) {
            return missing;
        }
        return new CachedPlane(clip, armature, forward, normal);
    }

    private static Vec3f sampleAxis(
            AnimationClip clip,
            Armature armature,
            RetargetProfile profile,
            float fraction,
            float across,
            float upward) {
        Pose pose = clip.getPoseInTime(clip.getClipTime() * fraction);
        if (pose == null) {
            return null;
        }
        RetargetedPose adapted = RigidPoseRetarget.adapt(armature, pose, profile);
        OpenMatrix4f arm = adapted.rightArm();
        if (arm == null) {
            return null;
        }
        return OpenMatrix4f.transform3v(arm, new Vec3f(across, upward, 0.0F), null);
    }

    private static Vec3f subtract(Vec3f left, Vec3f right) {
        return new Vec3f(
                left.coordinateX - right.coordinateX,
                left.coordinateY - right.coordinateY,
                left.coordinateZ - right.coordinateZ);
    }

    private static String note(String basisNote) {
        return basisNote == null ? "" : basisNote;
    }

    private enum VisualKind {
        BLADE,
        ROLL
    }

    private static final class Recipe {
        private final VisualKind kind;
        private final boolean rightHand;
        private final boolean leftHand;
        private final float[] sampleFractions;
        private final boolean lateralAxis;
        private final boolean faceNegativeAhead;
        private final SlashAlignmentScope alignmentScope;
        private final String basisNote;

        private Recipe(
                VisualKind kind,
                boolean rightHand,
                boolean leftHand,
                float[] sampleFractions,
                boolean lateralAxis,
                boolean faceNegativeAhead,
                SlashAlignmentScope alignmentScope,
                String basisNote) {
            this.kind = kind;
            this.rightHand = rightHand;
            this.leftHand = leftHand;
            this.sampleFractions = sampleFractions;
            this.lateralAxis = lateralAxis;
            this.faceNegativeAhead = faceNegativeAhead;
            this.alignmentScope = alignmentScope;
            this.basisNote = basisNote;
        }
    }

    private static final class CachedPlane {
        private final AnimationClip clip;
        private final Armature armature;
        private final Vec3f forward;
        private final Vec3f normal;

        private CachedPlane(AnimationClip clip, Armature armature, Vec3f forward, Vec3f normal) {
            this.clip = clip;
            this.armature = armature;
            this.forward = forward;
            this.normal = normal;
        }
    }
}
