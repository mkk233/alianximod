package com.bslx.XueXiMod.animation.runtime.loader;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bslx.XueXiMod.animation.core.AnimationClip;
import com.bslx.XueXiMod.animation.core.model.Armature;
import com.bslx.XueXiMod.animation.runtime.ActionDefinition;
import com.bslx.XueXiMod.animation.runtime.ActionStatus;

import net.minecraft.resources.ResourceLocation;

/** 一次开发目录读取的结果。清单文件本身不会被改写。 */
public final class DevLoadResult {
    private final String upstreamCommit;
    private final String summaryText;
    private final Armature sourceArmature;
    private final List<ActionStatus> statuses;
    private final Map<ResourceLocation, AnimationClip> clips;
    private final Map<ResourceLocation, ActionDefinition> definitions;
    private final int fileCount;
    private final int aliasCount;

    public DevLoadResult(
            String upstreamCommit,
            String summaryText,
            Armature sourceArmature,
            List<ActionStatus> statuses,
            Map<ResourceLocation, AnimationClip> clips,
            Map<ResourceLocation, ActionDefinition> definitions,
            int fileCount,
            int aliasCount) {
        this.upstreamCommit = upstreamCommit == null ? "" : upstreamCommit;
        this.summaryText = summaryText == null ? "" : summaryText;
        this.sourceArmature = sourceArmature;
        this.statuses = List.copyOf(statuses == null ? List.of() : statuses);
        Map<ResourceLocation, AnimationClip> clipCopy = new LinkedHashMap<>();
        if (clips != null) {
            clipCopy.putAll(clips);
        }
        Map<ResourceLocation, ActionDefinition> definitionCopy = new LinkedHashMap<>();
        if (definitions != null) {
            definitionCopy.putAll(definitions);
        }
        this.clips = Collections.unmodifiableMap(clipCopy);
        this.definitions = Collections.unmodifiableMap(definitionCopy);
        this.fileCount = fileCount;
        this.aliasCount = aliasCount;
    }

    public String upstreamCommit() {
        return this.upstreamCommit;
    }

    public String summaryText() {
        return this.summaryText;
    }

    public Armature sourceArmature() {
        return this.sourceArmature;
    }

    public List<ActionStatus> statuses() {
        return this.statuses;
    }

    public AnimationClip clip(ResourceLocation actionId) {
        return this.clips.get(actionId);
    }

    public ActionDefinition definition(ResourceLocation actionId) {
        return this.definitions.get(actionId);
    }

    public Map<ResourceLocation, AnimationClip> clips() {
        return this.clips;
    }

    public Map<ResourceLocation, ActionDefinition> definitions() {
        return this.definitions;
    }

    public int fileCount() {
        return this.fileCount;
    }

    public int aliasCount() {
        return this.aliasCount;
    }

    public int posePlayableCount() {
        int playable = 0;
        for (ActionStatus status : this.statuses) {
            if (status.posePlayable()) {
                playable++;
            }
        }
        return playable;
    }

    public int degradedCount() {
        int degraded = 0;
        for (ActionStatus status : this.statuses) {
            if (status.degraded()) {
                degraded++;
            }
        }
        return degraded;
    }

    public int quarantinedCount() {
        return this.count(ActionStatus.Availability.QUARANTINED);
    }

    public int rejectedCount() {
        return this.count(ActionStatus.Availability.REJECTED);
    }

    public ActionStatus status(ResourceLocation actionId) {
        for (ActionStatus status : this.statuses) {
            if (status.actionId().equals(actionId)) {
                return status;
            }
        }
        return null;
    }

    private int count(ActionStatus.Availability availability) {
        int matched = 0;
        for (ActionStatus status : this.statuses) {
            if (status.availability() == availability) {
                matched++;
            }
        }
        return matched;
    }
}
