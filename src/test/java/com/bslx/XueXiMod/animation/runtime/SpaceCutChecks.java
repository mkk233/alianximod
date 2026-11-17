package com.bslx.XueXiMod.animation.runtime;

import com.bslx.XueXiMod.animation.core.AnimationClip;
import com.bslx.XueXiMod.animation.core.JointTransform;
import com.bslx.XueXiMod.animation.core.Keyframe;
import com.bslx.XueXiMod.animation.core.TransformSheet;
import com.bslx.XueXiMod.client.action.ActionVisualCatalog;
import com.bslx.XueXiMod.client.action.SlashAlignmentScope;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import net.minecraft.resources.ResourceLocation;

/** 空间切割动作注册、宿主组开关和视觉分类的无头检查。 */
public final class SpaceCutChecks {
    private static int checks;

    private SpaceCutChecks() {
    }

    public static void main(String[] programArguments) {
        OriginalHumanoid.register();
        ActionDefinition definition = ActionRegistry.definition(OriginalHumanoid.SPACE_CUT_ID);
        AnimationClip clip = ActionRegistry.clip(OriginalHumanoid.SPACE_CUT_ID);
        ActionStatus status = ActionRegistry.status(OriginalHumanoid.SPACE_CUT_ID);

        yes(definition != null, "space cut definition exists");
        yes(clip != null, "space cut clip exists");
        yes(status != null && status.availability() == ActionStatus.Availability.ORIGINAL, "space cut is an original action");
        if (definition != null) {
            near(definition.durationSeconds(), 1.0F, "space cut duration");
            yes(definition.boneMask().containsAll(java.util.Set.of("Body", "Head", "ArmRight", "ArmLeft")),
                    "space cut drives four humanoid bones");
            yes(!definition.boneMask().contains("Root"), "space cut does not drive root");
        }
        if (clip != null) {
            near(clip.getClipTime(), 1.0F, "space cut clip time");
            yes(clip.getJointTransforms().keySet().containsAll(java.util.Set.of("Root", "Body", "Head", "ArmRight", "ArmLeft")),
                    "space cut clip has the humanoid joints");
            for (String bone : new String[] {"Body", "Head", "ArmRight", "ArmLeft"}) {
                TransformSheet sheet = clip.getJointTransforms().get(bone);
                Keyframe[] keys = sheet == null ? null : sheet.getKeyframes();
                yes(keys != null && keys.length == 101, bone + " is sampled at 0.01s");
                if (keys != null && keys.length > 0) {
                    near(keys[0].time(), 0.0F, bone + " starts at zero");
                    near(keys[keys.length - 1].time(), 1.0F, bone + " ends at one second");
                    yes(!identity(keys[0].transform()), bone + " keeps a non-empty first pose");
                    yes(!identity(keys[keys.length - 1].transform()), bone + " keeps a non-empty last pose");
                }
            }
        }

        yes(ReleaseRegistry.submitsHostGroup("space_cut"), "space cut submits through its host group");
        yes(!ReleaseRegistry.submitsHostGroup("water_blade"), "water blade keeps its own host switch");
        yes(ActionVisualFallback.fallbackFor(OriginalHumanoid.SPACE_CUT_ID).equals(OriginalHumanoid.SPACE_CUT_ID),
                "space cut does not fall back to the horizontal water blade");

        yes(ActionVisualCatalog.blade(OriginalHumanoid.SPACE_CUT_ID), "space cut is a blade visual");
        yes(ActionVisualCatalog.usesHand(OriginalHumanoid.SPACE_CUT_ID, true), "space cut is right handed");
        yes(!ActionVisualCatalog.usesHand(OriginalHumanoid.SPACE_CUT_ID, false), "space cut is not left handed");
        yes(ActionVisualCatalog.alignmentScope(OriginalHumanoid.SPACE_CUT_ID) == SlashAlignmentScope.UPPER_BODY_SLASH,
                "space cut uses the upper body writer");
        yes(!ActionVisualCatalog.hasRecipe(OriginalHumanoid.SPACE_CUT_ID), "space cut has no slash plane recipe");

        ResourceLocation unknown = ResourceLocation.fromNamespaceAndPath("xuexicheck", "missing");
        yes(!ReleaseRegistry.submitsHostGroup(unknown.toString()), "unknown release does not submit");
        System.out.println("SpaceCutChecks: " + checks + " assertions");
    }

    private static boolean identity(JointTransform transform) {
        if (transform == null || transform.rotation() == null) {
            return true;
        }
        return Math.abs(transform.rotation().w() - 1.0F) < 1.0E-5F;
    }

    private static void yes(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }

    private static void near(float actual, float expected, String label) {
        checks++;
        if (!Float.isFinite(actual) || Math.abs(actual - expected) > 1.0E-5F) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }
}
