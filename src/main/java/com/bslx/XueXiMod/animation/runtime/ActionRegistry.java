package com.bslx.XueXiMod.animation.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bslx.XueXiMod.animation.core.AnimationClip;
import com.bslx.XueXiMod.animation.core.model.Armature;
import com.bslx.XueXiMod.animation.runtime.loader.DevLoadResult;

import net.minecraft.resources.ResourceLocation;

/** 稳定 ID 注册表。新增动作走 register，播放器里没有术式分支。 */
public final class ActionRegistry {
    private static final Object GUARD = new Object();
    private static final Map<ResourceLocation, ActionDefinition> DEFINITIONS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, AnimationClip> CLIPS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, ActionStatus> STATUSES = new LinkedHashMap<>();
    private static final Map<String, Armature> ARMATURES = new LinkedHashMap<>();
    private static String gateText = "授权动作尚未加载";
    private static Armature sourceArmature;

    private ActionRegistry() {
    }

    public static void noteGate(String text) {
        synchronized (GUARD) {
            gateText = text == null ? "" : text;
        }
    }

    public static String gateText() {
        synchronized (GUARD) {
            return gateText;
        }
    }

    public static void registerArmature(Armature armature) {
        if (armature == null || armature.toString() == null || armature.toString().isBlank()) {
            throw new IllegalArgumentException("armature is missing");
        }
        synchronized (GUARD) {
            ARMATURES.put(armature.toString(), armature);
        }
    }

    public static void registerPlayable(ActionDefinition definition, AnimationClip clip, ActionStatus status) {
        if (definition == null || clip == null || status == null) {
            throw new IllegalArgumentException("playable registration is missing");
        }
        if (!definition.actionId().equals(status.actionId())) {
            throw new IllegalArgumentException("definition and status ids differ");
        }
        if (!status.posePlayable()) {
            throw new IllegalArgumentException("status is not playable");
        }
        if (Math.abs(definition.durationSeconds() - clip.getClipTime()) > 0.002F) {
            throw new IllegalArgumentException("definition duration does not match the clip");
        }
        synchronized (GUARD) {
            DEFINITIONS.put(definition.actionId(), definition);
            CLIPS.put(definition.actionId(), clip);
            STATUSES.put(definition.actionId(), status);
        }
    }

    public static void registerBlocked(ActionStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("blocked status is missing");
        }
        synchronized (GUARD) {
            DEFINITIONS.remove(status.actionId());
            CLIPS.remove(status.actionId());
            STATUSES.put(status.actionId(), status);
        }
    }

    public static void publishDev(DevLoadResult result) {
        if (result == null) {
            throw new IllegalArgumentException("dev load result is missing");
        }
        synchronized (GUARD) {
            sourceArmature = result.sourceArmature();
            if (sourceArmature != null) {
                ARMATURES.put(sourceArmature.toString(), sourceArmature);
            }
            for (ActionStatus status : result.statuses()) {
                ActionDefinition definition = result.definition(status.actionId());
                AnimationClip clip = result.clip(status.actionId());
                if (status.posePlayable() && definition != null && clip != null) {
                    DEFINITIONS.put(definition.actionId(), definition);
                    CLIPS.put(definition.actionId(), clip);
                    STATUSES.put(status.actionId(), status);
                } else {
                    DEFINITIONS.remove(status.actionId());
                    CLIPS.remove(status.actionId());
                    STATUSES.put(status.actionId(), status);
                }
            }
        }
    }

    public static void isolate(ResourceLocation actionId, String reason) {
        if (actionId == null) {
            throw new IllegalArgumentException("action id is missing");
        }
        synchronized (GUARD) {
            ActionStatus previous = STATUSES.get(actionId);
            ActionStatus isolated = new ActionStatus(
                    actionId,
                    ActionStatus.Availability.QUARANTINED,
                    reason == null ? "单项隔离" : reason,
                    previous == null ? "" : previous.armatureName(),
                    previous == null ? "" : previous.sourcePath(),
                    previous == null ? "" : previous.upstreamCommit(),
                    previous == null ? "" : previous.categoryText(),
                    previous == null ? "" : previous.aliasOf(),
                    previous == null ? 0.0F : previous.durationSeconds(),
                    previous == null ? "" : previous.entryShape(),
                    previous == null ? "" : previous.auxiliaryText(),
                    false);
            DEFINITIONS.remove(actionId);
            CLIPS.remove(actionId);
            STATUSES.put(actionId, isolated);
        }
    }

    public static ActionDefinition definition(ResourceLocation actionId) {
        synchronized (GUARD) {
            return DEFINITIONS.get(actionId);
        }
    }

    public static AnimationClip clip(ResourceLocation actionId) {
        synchronized (GUARD) {
            return CLIPS.get(actionId);
        }
    }

    public static ActionStatus status(ResourceLocation actionId) {
        synchronized (GUARD) {
            return STATUSES.get(actionId);
        }
    }

    public static Armature armature(String armatureName) {
        synchronized (GUARD) {
            return ARMATURES.get(armatureName);
        }
    }

    public static Armature sourceArmature() {
        synchronized (GUARD) {
            return sourceArmature;
        }
    }

    public static List<ActionStatus> statuses() {
        synchronized (GUARD) {
            return List.copyOf(STATUSES.values());
        }
    }

    public static List<ResourceLocation> playableIds() {
        synchronized (GUARD) {
            List<ResourceLocation> ids = new ArrayList<>();
            for (ActionStatus status : STATUSES.values()) {
                if (status.posePlayable() && CLIPS.containsKey(status.actionId())) {
                    ids.add(status.actionId());
                }
            }
            return Collections.unmodifiableList(ids);
        }
    }

}
