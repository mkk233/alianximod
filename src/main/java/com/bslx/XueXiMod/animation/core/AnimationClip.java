/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Copyright (C) Epic Fight Team / Yesman
 * Local modifications Copyright (C) 2026 XueXi Mod contributors.
 *
 * Upstream: https://github.com/Antikythera-Studios/epicfight
 * Commit: 9d25f19c121c5e51b1db9efe01833a563706eeee
 * Source-Path: src/main/java/yesman/epicfight/api/animation/AnimationClip.java
 * Modified: 2026-10-24
 *
 * 修改说明：空关节表返回空姿势。烘焙后只有一个时间点时进度为 0，不除以零。
 * 普通采样不再把负时间加上剪辑时长，低于首帧取首帧、高于末帧取末帧。
 * 只有 loopedSampleTime 取模；clipTime <= 0 时返回 0。时长必须有限，允许小于等于 0。
 * 剪辑持有轨道副本。公开取回的轨道、姿势采样都是副本，烘焙缓存不跟外部修改走。
 */
package com.bslx.XueXiMod.animation.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bslx.XueXiMod.animation.core.math.FiniteChecks;

public class AnimationClip {
    public static final AnimationClip EMPTY_CLIP = new AnimationClip();

    protected Map<String, TransformSheet> jointTransforms = new HashMap<>();
    protected float clipTime;
    protected float[] bakedTimes;

    public void addJointTransform(String jointName, TransformSheet sheet) {
        if (jointName == null || sheet == null) {
            throw new IllegalArgumentException("joint transform sheet is missing");
        }
        this.jointTransforms.put(jointName, sheet.copyAll());
        this.bakedTimes = null;
    }

    public boolean hasJointTransform(String jointName) {
        return this.jointTransforms.containsKey(jointName);
    }

    public void bakeKeyframes() {
        Set<Float> timestamps = new HashSet<>();
        for (TransformSheet transformSheet : this.jointTransforms.values()) {
            if (transformSheet == null) {
                throw new IllegalArgumentException("joint transform sheet is missing");
            }
            transformSheet.forEach((frameIndex, keyframe) -> {
                float time = keyframe.time();
                FiniteChecks.requireFinite("keyframe time", time);
                timestamps.add(time);
            });
        }
        List<Float> ordered = new ArrayList<>(timestamps);
        ordered.sort(Float::compare);
        float[] bakedTimestamps = new float[ordered.size()];
        for (int stampIndex = 0; stampIndex < ordered.size(); stampIndex++) {
            bakedTimestamps[stampIndex] = ordered.get(stampIndex);
            if (stampIndex > 0 && !(bakedTimestamps[stampIndex] > bakedTimestamps[stampIndex - 1])) {
                throw new IllegalArgumentException("baked sample times must be strictly increasing");
            }
        }
        if (bakedTimestamps.length == 0) {
            this.bakedTimes = bakedTimestamps;
            return;
        }
        Map<String, TransformSheet> bakedJointTransforms = new HashMap<>();
        this.jointTransforms.forEach((jointName, transformSheet) -> bakedJointTransforms.put(jointName, transformSheet.createInterpolated(bakedTimestamps)));
        this.jointTransforms = bakedJointTransforms;
        this.bakedTimes = bakedTimestamps;
    }

    public void setBaked() {
        TransformSheet rootSheet = this.jointTransforms.get("Root");
        if (rootSheet == null) {
            this.bakedTimes = null;
            return;
        }
        Keyframe[] frames = rootSheet.getKeyframes();
        float[] times = new float[frames.length];
        for (int stampIndex = 0; stampIndex < frames.length; stampIndex++) {
            times[stampIndex] = frames[stampIndex].time();
        }
        this.bakedTimes = times;
    }

    public TransformSheet getJointTransform(String jointName) {
        TransformSheet sheet = this.jointTransforms.get(jointName);
        if (sheet == null) {
            return null;
        }
        return sheet.copyAll();
    }

    public final Pose getPoseInTime(float time) {
        if (!Float.isFinite(time)) {
            throw new IllegalArgumentException("sample time must be finite");
        }
        Pose pose = new Pose();
        if (this.bakedTimes != null && this.bakedTimes.length > 0) {
            float capturedTime = time;
            SampleSearch.Result bracket = SampleSearch.bracket(this.bakedTimes.length, columnIndex -> this.bakedTimes[columnIndex], capturedTime);
            TransformSheet.InterpolationInfo interpolationInfo = new TransformSheet.InterpolationInfo(bracket.begin(), bracket.end(), bracket.progression());
            for (String jointName : this.jointTransforms.keySet()) {
                TransformSheet sheet = this.jointTransforms.get(jointName);
                pose.putJointData(jointName, sheet == null ? JointTransform.empty() : sheet.getInterpolatedTransform(interpolationInfo));
            }
        } else {
            for (String jointName : this.jointTransforms.keySet()) {
                TransformSheet sheet = this.jointTransforms.get(jointName);
                pose.putJointData(jointName, sheet == null ? JointTransform.empty() : sheet.getInterpolatedTransform(time));
            }
        }
        return pose;
    }

    /**
     * 时长小于等于 0 时返回 0，不对时长取模。
     */
    public float loopedSampleTime(float sampleTime) {
        FiniteChecks.requireFinite("sample time", sampleTime);
        if (!(this.clipTime > 0.0F)) {
            return 0.0F;
        }
        float wrapped = sampleTime % this.clipTime;
        if (wrapped < 0.0F) {
            wrapped += this.clipTime;
        }
        if (!Float.isFinite(wrapped)) {
            throw new IllegalArgumentException("looped sample time must be finite");
        }
        return wrapped;
    }

    public Map<String, TransformSheet> getJointTransforms() {
        Map<String, TransformSheet> snapshot = new HashMap<>();
        for (Map.Entry<String, TransformSheet> entry : this.jointTransforms.entrySet()) {
            TransformSheet sheet = entry.getValue();
            snapshot.put(entry.getKey(), sheet == null ? null : sheet.copyAll());
        }
        return Collections.unmodifiableMap(snapshot);
    }

    public void reset() {
        this.jointTransforms.clear();
        this.bakedTimes = null;
    }

    public void setClipTime(float clipTime) {
        FiniteChecks.requireFinite("clip time", clipTime);
        this.clipTime = clipTime;
    }

    public float getClipTime() {
        return this.clipTime;
    }
}
