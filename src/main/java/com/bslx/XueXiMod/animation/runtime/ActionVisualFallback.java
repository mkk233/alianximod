package com.bslx.XueXiMod.animation.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bslx.XueXiMod.animation.core.AnimationClip;
import com.bslx.XueXiMod.animation.core.Keyframe;
import com.bslx.XueXiMod.animation.core.TransformSheet;

import net.minecraft.resources.ResourceLocation;

/**
 * 客户端缺少开发剪辑时，把原创兜底按权威时长拉开。
 * 这只改视觉时间，不改服务端结束、费用或伤害窗。
 */
public final class ActionVisualFallback {
    private static final ResourceLocation SWORD_LEFT_CANDIDATE = ResourceLocation.fromNamespaceAndPath("epicfight", "biped/combat/sword_auto1");
    private static final ResourceLocation SWORD_RIGHT_CANDIDATE = ResourceLocation.fromNamespaceAndPath("epicfight", "biped/combat/sword_auto2");
    private static final ResourceLocation SWORD_HORIZONTAL_CANDIDATE = ResourceLocation.fromNamespaceAndPath("epicfight", "biped/combat/sword_auto3");

    private ActionVisualFallback() {
    }

    public static ResourceLocation fallbackFor(ResourceLocation actionId) {
        if (actionId == null) {
            return OriginalHumanoid.HORIZONTAL_ID;
        }
        if (actionId.equals(OriginalHumanoid.SPACE_CUT_ID)) {
            return OriginalHumanoid.SPACE_CUT_ID;
        }
        if (actionId.equals(OriginalHumanoid.LEFT_ID) || actionId.equals(SWORD_LEFT_CANDIDATE)) {
            return OriginalHumanoid.LEFT_ID;
        }
        if (actionId.equals(OriginalHumanoid.RIGHT_ID) || actionId.equals(SWORD_RIGHT_CANDIDATE)) {
            return OriginalHumanoid.RIGHT_ID;
        }
        if (actionId.equals(OriginalHumanoid.HORIZONTAL_ID) || actionId.equals(SWORD_HORIZONTAL_CANDIDATE)) {
            return OriginalHumanoid.HORIZONTAL_ID;
        }
        if (actionId.equals(OriginalHumanoid.EXAMPLE_ID)) {
            return OriginalHumanoid.EXAMPLE_ID;
        }
        return OriginalHumanoid.HORIZONTAL_ID;
    }

    public static Prepared prepare(ResourceLocation requestedId, ResourceLocation fallbackId, float authoritativeDuration, float blendSeconds) {
        ResourceLocation visualId = requestedId;
        ActionDefinition definition = ActionRegistry.definition(requestedId);
        AnimationClip clip = ActionRegistry.clip(requestedId);
        if (definition == null || clip == null) {
            visualId = fallbackId != null ? fallbackId : fallbackFor(requestedId);
            definition = ActionRegistry.definition(visualId);
            clip = ActionRegistry.clip(visualId);
        }
        if (definition == null || clip == null) {
            visualId = OriginalHumanoid.HORIZONTAL_ID;
            definition = ActionRegistry.definition(visualId);
            clip = ActionRegistry.clip(visualId);
        }
        if (definition == null || clip == null) {
            return null;
        }
        float duration = authoritativeDuration;
        if (!Float.isFinite(duration) || duration <= 0.0F) {
            duration = definition.durationSeconds();
        }
        float blend = blendSeconds;
        if (!Float.isFinite(blend) || blend < 0.0F) {
            blend = 0.0F;
        }
        if (blend > 2.0F) {
            blend = 2.0F;
        }
        AnimationClip stretched = stretchOnto(clip, duration);
        AnimationClip visualClip = stretched != null ? stretched : clip;
        ActionDefinition timed = retimed(definition, visualId, duration, blend);
        return new Prepared(timed, visualClip, visualId, definition.armatureName());
    }

    public static AnimationClip stretchOnto(AnimationClip source, float authoritativeDuration) {
        if (source == null || !Float.isFinite(authoritativeDuration) || authoritativeDuration <= 0.0F) {
            return null;
        }
        float sourceDuration = source.getClipTime();
        if (!(sourceDuration > 0.0F) || !Float.isFinite(sourceDuration)) {
            return null;
        }
        float scale = authoritativeDuration / sourceDuration;
        if (!Float.isFinite(scale) || scale <= 0.0F) {
            return null;
        }
        AnimationClip stretched = new AnimationClip();
        for (Map.Entry<String, TransformSheet> entry : source.getJointTransforms().entrySet()) {
            TransformSheet sheet = entry.getValue();
            if (sheet == null) {
                return null;
            }
            Keyframe[] frames = sheet.getKeyframes();
            List<Keyframe> scaledFrames = new ArrayList<>();
            float previousTime = Float.NEGATIVE_INFINITY;
            for (int frameIndex = 0; frameIndex < frames.length; frameIndex++) {
                Keyframe frame = frames[frameIndex];
                float scaledTime = frame.time() * scale;
                if (!Float.isFinite(scaledTime) || !(scaledTime > previousTime)) {
                    return null;
                }
                previousTime = scaledTime;
                scaledFrames.add(new Keyframe(scaledTime, frame.transform()));
            }
            stretched.addJointTransform(entry.getKey(), new TransformSheet(scaledFrames));
        }
        stretched.setClipTime(authoritativeDuration);
        return stretched;
    }

    private static ActionDefinition retimed(ActionDefinition source, ResourceLocation visualId, float authoritativeDuration, float blendSeconds) {
        float scale = authoritativeDuration / source.durationSeconds();
        List<ActionDefinition.TimeNotice> notices = new ArrayList<>();
        if (Float.isFinite(scale) && scale > 0.0F) {
            for (ActionDefinition.TimeNotice notice : source.notices()) {
                float scaledTime = notice.seconds() * scale;
                if (!Float.isFinite(scaledTime) || scaledTime < 0.0F) {
                    continue;
                }
                if (scaledTime > authoritativeDuration) {
                    scaledTime = authoritativeDuration;
                }
                notices.add(new ActionDefinition.TimeNotice(scaledTime, notice.noticeId()));
            }
        }
        String originLabel = source.originLabel();
        if (Math.abs(authoritativeDuration - source.durationSeconds()) > 0.002F) {
            originLabel = originLabel + "。按权威时长映射，不是另一条上游动作";
        }
        return new ActionDefinition(visualId, source.armatureName(), authoritativeDuration, source.loopingDefault(), blendSeconds,
                source.boneMask(), source.channels(), notices, originLabel);
    }

    public static final class Prepared {
        private final ActionDefinition definition;
        private final AnimationClip clip;
        private final ResourceLocation visualActionId;
        private final String armatureName;

        private Prepared(ActionDefinition definition, AnimationClip clip, ResourceLocation visualActionId, String armatureName) {
            this.definition = definition;
            this.clip = clip;
            this.visualActionId = visualActionId;
            this.armatureName = armatureName;
        }

        public ActionDefinition definition() {
            return this.definition;
        }

        public AnimationClip clip() {
            return this.clip;
        }

        public ResourceLocation visualActionId() {
            return this.visualActionId;
        }

        public String armatureName() {
            return this.armatureName;
        }
    }
}
