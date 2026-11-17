/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Copyright (C) Epic Fight Team / Yesman
 * Local modifications Copyright (C) 2026 XueXi Mod contributors.
 *
 * Upstream: https://github.com/Antikythera-Studios/epicfight
 * Commit: 9d25f19c121c5e51b1db9efe01833a563706eeee
 * Source-Path: src/main/java/yesman/epicfight/api/animation/TransformSheet.java
 * Modified: 2026-10-24
 *
 * 修改说明：去掉 LivingEntityPatch 和 Minecraft Vec3。世界坐标修正仍按原公式，输入改为 Vec3f。
 * 两帧及以上的时间必须严格递增；空轨采样返回空变换；单帧不除以零。
 * 普通采样低于首帧取首帧、高于末帧取末帧，负时间不再加末帧时间。循环只走 AnimationClip.loopedSampleTime。
 * 构造和公开 getKeyframes 持有独立副本。校正写入拥有的关键帧，不写公开副本。
 * 水平投影长度不超过 1e-5 时偏航为 0。二分查找见 SampleSearch。extend 后若时间不再严格递增则拒绝写入。
 */
package com.bslx.XueXiMod.animation.core;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.joml.Quaternionf;

import com.bslx.XueXiMod.animation.core.math.FiniteChecks;
import com.bslx.XueXiMod.animation.core.math.MathUtils;
import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.math.Vec3f;

public class TransformSheet {
    public static final TransformSheet EMPTY_SHEET = new TransformSheet(List.of(
            new Keyframe(0.0F, JointTransform.empty()),
            new Keyframe(Float.MAX_VALUE, JointTransform.empty())));
    public static final Function<Vec3f, TransformSheet> EMPTY_SHEET_PROVIDER = translation -> {
        if (translation == null || !translation.validateValues()) {
            throw new IllegalArgumentException("empty sheet translation must be finite");
        }
        return new TransformSheet(List.of(
                new Keyframe(0.0F, JointTransform.translation(translation.copy())),
                new Keyframe(Float.MAX_VALUE, JointTransform.empty())));
    };

    private Keyframe[] keyframes;

    public TransformSheet() {
        this(new Keyframe[0]);
    }

    public TransformSheet(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("keyframe size must be non-negative");
        }
        this.keyframes = new Keyframe[size];
    }

    public TransformSheet(List<Keyframe> keyframeList) {
        if (keyframeList == null) {
            throw new IllegalArgumentException("keyframes are missing");
        }
        this.keyframes = validated(keyframeList.toArray(new Keyframe[0]));
    }

    public TransformSheet(Keyframe[] keyframes) {
        this.keyframes = validated(keyframes);
    }

    private static Keyframe[] validated(Keyframe[] frames) {
        if (frames == null) {
            throw new IllegalArgumentException("keyframes are missing");
        }
        Keyframe[] owned = new Keyframe[frames.length];
        float previousTime = Float.NEGATIVE_INFINITY;
        for (int frameIndex = 0; frameIndex < frames.length; frameIndex++) {
            Keyframe frame = frames[frameIndex];
            if (frame == null) {
                throw new IllegalArgumentException("keyframe is missing");
            }
            float time = frame.time();
            if (!Float.isFinite(time)) {
                throw new IllegalArgumentException("keyframe time must be finite");
            }
            if (frameIndex > 0 && !(time > previousTime)) {
                throw new IllegalArgumentException("keyframe times must be strictly increasing");
            }
            previousTime = time;
            owned[frameIndex] = new Keyframe(frame);
        }
        return owned;
    }

    public JointTransform getStartTransform() {
        if (this.keyframes.length == 0) {
            return JointTransform.empty();
        }
        return this.keyframes[0].transform();
    }

    public Keyframe[] getKeyframes() {
        Keyframe[] exposed = new Keyframe[this.keyframes.length];
        for (int frameIndex = 0; frameIndex < this.keyframes.length; frameIndex++) {
            exposed[frameIndex] = new Keyframe(this.keyframes[frameIndex]);
        }
        return exposed;
    }

    public TransformSheet copyAll() {
        return this.copy(0, this.keyframes.length);
    }

    public TransformSheet copy(int start, int end) {
        if (start < 0 || end > this.keyframes.length || start > end) {
            throw new IllegalArgumentException("keyframe copy range is outside the sheet");
        }
        int span = end - start;
        Keyframe[] copied = new Keyframe[span];
        for (int frameIndex = 0; frameIndex < span; frameIndex++) {
            copied[frameIndex] = new Keyframe(this.keyframes[frameIndex + start]);
        }
        return new TransformSheet(copied);
    }

    public TransformSheet readFrom(TransformSheet opponent) {
        if (opponent == null) {
            throw new IllegalArgumentException("transform sheet is missing");
        }
        Keyframe[] replaced = new Keyframe[opponent.keyframes.length];
        for (int frameIndex = 0; frameIndex < replaced.length; frameIndex++) {
            replaced[frameIndex] = new Keyframe(opponent.keyframes[frameIndex]);
        }
        this.keyframes = validated(replaced);
        return this;
    }

    public TransformSheet createInterpolated(float[] timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException("interpolation timestamps are missing");
        }
        TransformSheet interpolationCreated = new TransformSheet(timestamp.length);
        Keyframe[] created = new Keyframe[timestamp.length];
        for (int frameIndex = 0; frameIndex < timestamp.length; frameIndex++) {
            created[frameIndex] = new Keyframe(timestamp[frameIndex], this.getInterpolatedTransform(timestamp[frameIndex]));
        }
        interpolationCreated.keyframes = validated(created);
        return interpolationCreated;
    }

    public void forEach(BiConsumer<Integer, Keyframe> task) {
        this.forEach(task, 0, this.keyframes.length);
    }

    public void forEach(BiConsumer<Integer, Keyframe> task, int start, int end) {
        if (task == null) {
            throw new IllegalArgumentException("keyframe task is missing");
        }
        if (start < 0) {
            throw new IllegalArgumentException("keyframe range is outside the sheet");
        }
        int limit = Math.min(end, this.keyframes.length);
        for (int frameIndex = start; frameIndex < limit; frameIndex++) {
            task.accept(frameIndex, new Keyframe(this.keyframes[frameIndex]));
        }
    }

    private void forEachOwned(BiConsumer<Integer, Keyframe> task, int start, int end) {
        if (task == null) {
            throw new IllegalArgumentException("keyframe task is missing");
        }
        if (start < 0) {
            throw new IllegalArgumentException("keyframe range is outside the sheet");
        }
        int limit = Math.min(end, this.keyframes.length);
        for (int frameIndex = start; frameIndex < limit; frameIndex++) {
            task.accept(frameIndex, this.keyframes[frameIndex]);
        }
    }

    public Vec3f getInterpolatedTranslation(float currentTime) {
        InterpolationInfo interpolInfo = this.getInterpolationInfo(currentTime);
        if (interpolInfo == InterpolationInfo.INVALID) {
            return new Vec3f();
        }
        return MathUtils.lerpVector(
                this.keyframes[interpolInfo.prev].transform().translation(),
                this.keyframes[interpolInfo.next].transform().translation(),
                interpolInfo.delta);
    }

    public Quaternionf getInterpolatedRotation(float currentTime) {
        InterpolationInfo interpolInfo = this.getInterpolationInfo(currentTime);
        if (interpolInfo == InterpolationInfo.INVALID) {
            return new Quaternionf();
        }
        return MathUtils.lerpQuaternion(
                this.keyframes[interpolInfo.prev].transform().rotation(),
                this.keyframes[interpolInfo.next].transform().rotation(),
                interpolInfo.delta);
    }

    public JointTransform getInterpolatedTransform(float currentTime) {
        return this.getInterpolatedTransform(this.getInterpolationInfo(currentTime));
    }

    public JointTransform getInterpolatedTransform(InterpolationInfo interpolationInfo) {
        if (interpolationInfo == null || interpolationInfo == InterpolationInfo.INVALID) {
            return JointTransform.empty();
        }
        if (interpolationInfo.prev < 0 || interpolationInfo.next < 0 || interpolationInfo.next >= this.keyframes.length || interpolationInfo.prev >= this.keyframes.length) {
            throw new IllegalArgumentException("interpolation info is outside the sheet");
        }
        return JointTransform.interpolate(this.keyframes[interpolationInfo.prev].transform(), this.keyframes[interpolationInfo.next].transform(), interpolationInfo.delta);
    }

    public TransformSheet extend(TransformSheet target) {
        if (target == null) {
            throw new IllegalArgumentException("transform sheet is missing");
        }
        if (target.keyframes.length == 0) {
            return this;
        }
        int combinedLength = this.keyframes.length + target.keyframes.length;
        Keyframe[] combined = new Keyframe[combinedLength];
        for (int frameIndex = 0; frameIndex < this.keyframes.length; frameIndex++) {
            combined[frameIndex] = this.keyframes[frameIndex];
        }
        for (int frameIndex = this.keyframes.length; frameIndex < combinedLength; frameIndex++) {
            combined[frameIndex] = new Keyframe(target.keyframes[frameIndex - this.keyframes.length]);
        }
        this.keyframes = validated(combined);
        return this;
    }

    public TransformSheet getFirstFrame() {
        if (this.keyframes.length < 2) {
            throw new IllegalArgumentException("first frame copy needs at least two keyframes");
        }
        TransformSheet part = this.copy(0, 2);
        part.keyframes[1].ownedTransform().copyFrom(part.keyframes[0].ownedTransform());
        return part;
    }

    public void correctAnimationByNewPosition(Vec3f startPosition, Vec3f startToEnd, Vec3f modifiedStart, Vec3f modifiedStartToEnd) {
        if (this.keyframes.length < 2) {
            throw new IllegalArgumentException("position correction needs at least two keyframes");
        }
        if (startPosition == null || startToEnd == null || modifiedStart == null || modifiedStartToEnd == null
                || !startPosition.validateValues() || !startToEnd.validateValues()
                || !modifiedStart.validateValues() || !modifiedStartToEnd.validateValues()) {
            throw new IllegalArgumentException("position correction vectors must be finite");
        }
        Keyframe[] frames = this.keyframes;
        Keyframe startKeyframe = frames[0];
        Keyframe endKeyframe = frames[frames.length - 1];
        float timeSpan = endKeyframe.time() - startKeyframe.time();
        if (!(timeSpan > 0.0F)) {
            throw new IllegalArgumentException("position correction time span must be positive");
        }
        Vec3f modifiedHorizontal = modifiedStartToEnd.copy().multiply(1.0F, 0.0F, 1.0F);
        Vec3f startHorizontal = startToEnd.copy().multiply(1.0F, 0.0F, 1.0F);
        float yawDegrees = 0.0F;
        if (modifiedHorizontal.length() > MathUtils.NEAR_ZERO_LENGTH && startHorizontal.length() > MathUtils.NEAR_ZERO_LENGTH) {
            yawDegrees = (float) MathUtils.getAngleBetween(modifiedHorizontal, startHorizontal);
        }
        float pitchDegrees = (float) Math.toDegrees(Math.atan2(modifiedStartToEnd.coordinateY - startToEnd.coordinateY, modifiedStartToEnd.length()));
        if (!Float.isFinite(pitchDegrees) || !Float.isFinite(yawDegrees)) {
            throw new IllegalArgumentException("position correction angles must be finite");
        }
        for (Keyframe frame : frames) {
            float blend = (frame.time() - startKeyframe.time()) / timeSpan;
            Vec3f line = MathUtils.lerpVector(new Vec3f(0.0F, 0.0F, 0.0F), startToEnd, blend);
            Vec3f modifiedLine = MathUtils.lerpVector(new Vec3f(0.0F, 0.0F, 0.0F), modifiedStartToEnd, blend);
            Vec3f keyTransform = frame.ownedTransform().translation();
            Vec3f startToKeyTransform = keyTransform.copy().sub(startPosition).multiply(-1.0F, 1.0F, -1.0F);
            Vec3f animationOnLine = startToKeyTransform.copy().sub(line);
            OpenMatrix4f rotator = OpenMatrix4f.createRotatorDeg(pitchDegrees, Vec3f.X_AXIS)
                    .mulFront(OpenMatrix4f.createRotatorDeg(yawDegrees, Vec3f.Y_AXIS));
            Vec3f toNewKeyTransform = modifiedLine.add(OpenMatrix4f.transform3v(rotator, animationOnLine, null));
            keyTransform.set(modifiedStart.copy().add(toNewKeyTransform));
        }
    }

    public TransformSheet getCorrectedModelCoord(Vec3f start, Vec3f destination, int startFrame, int endFrame) {
        if (start == null || destination == null || !start.validateValues() || !destination.validateValues()) {
            throw new IllegalArgumentException("model correction positions must be finite");
        }
        if (startFrame < 0 || endFrame >= this.keyframes.length || startFrame > endFrame) {
            throw new IllegalArgumentException("model correction frames are outside the sheet");
        }
        TransformSheet transform = this.copyAll();
        float horizontalDistance = destination.copy().sub(start).horizontalDistance();
        float verticalDistance = Math.abs(destination.coordinateY - start.coordinateY);
        JointTransform startTransform = transform.keyframes[startFrame].ownedTransform();
        JointTransform endTransform = transform.keyframes[endFrame].ownedTransform();
        startTransform.translation().set(startTransform.translation().coordinateX, verticalDistance, horizontalDistance);
        float endTime = transform.keyframes[endFrame].time();
        for (int frameIndex = startFrame + 1; frameIndex < endFrame; frameIndex++) {
            JointTransform middleTransform = transform.keyframes[frameIndex].ownedTransform();
            float ratio = endTime == 0.0F ? 0.0F : transform.keyframes[frameIndex].time() / endTime;
            FiniteChecks.requireFinite("model correction ratio", ratio);
            middleTransform.translation().set(MathUtils.lerpVector(startTransform.translation(), endTransform.translation(), ratio));
        }
        return transform;
    }

    public TransformSheet extendsZCoord(float multiplier, int startFrame, int endFrame) {
        FiniteChecks.requireFinite("z multiplier", multiplier);
        if (startFrame < 0 || endFrame < startFrame || endFrame >= this.keyframes.length) {
            throw new IllegalArgumentException("z extension frames are outside the sheet");
        }
        TransformSheet transform = this.copyAll();
        Keyframe[] frames = transform.keyframes;
        float extension = 0.0F;
        for (int frameIndex = 0; frameIndex < endFrame + 1; frameIndex++) {
            Vec3f translation = frames[frameIndex].ownedTransform().translation();
            float previousZ = translation.coordinateZ;
            translation.multiply(1.0F, 1.0F, multiplier);
            extension = translation.coordinateZ - previousZ;
        }
        FiniteChecks.requireFinite("z extension", extension);
        for (int frameIndex = endFrame + 1; frameIndex < frames.length; frameIndex++) {
            frames[frameIndex].ownedTransform().translation().add(0.0F, 0.0F, extension);
        }
        return transform;
    }

    public TransformSheet transformToWorldCoordOriginAsDest(
            Vec3f startInWorld,
            Vec3f destinationInWorld,
            float entityYaw,
            float destinationYaw,
            int startFrame,
            int destinationFrame) {
        if (startInWorld == null || destinationInWorld == null || !startInWorld.validateValues() || !destinationInWorld.validateValues()) {
            throw new IllegalArgumentException("world correction positions must be finite");
        }
        FiniteChecks.requireFinite("entity yaw", entityYaw);
        FiniteChecks.requireFinite("destination yaw", destinationYaw);
        if (this.keyframes.length == 0 || startFrame < 0 || destinationFrame < 0 || destinationFrame >= this.keyframes.length) {
            throw new IllegalArgumentException("world correction frames are outside the sheet");
        }
        TransformSheet byStart = this.copy(0, destinationFrame + 1);
        TransformSheet byDestination = this.copy(0, destinationFrame + 1);
        TransformSheet result = new TransformSheet(destinationFrame + 1);
        Vec3f toTargetInWorld = destinationInWorld.copy().sub(startInWorld);
        float worldMagnitude = toTargetInWorld.horizontalDistance();
        float animationMagnitude = this.keyframes[0].transform().translation().horizontalDistance();
        if (!(animationMagnitude > 0.0F) || !Float.isFinite(worldMagnitude)) {
            throw new IllegalArgumentException("world correction magnitude must be finite and non-zero");
        }
        float scale = worldMagnitude / animationMagnitude;
        FiniteChecks.requireFinite("world correction scale", scale);
        byStart.forEachOwned((frameIndex, keyframe) -> {
            keyframe.ownedTransform().translation().sub(this.keyframes[0].ownedTransform().translation());
            keyframe.ownedTransform().translation().multiply(1.0F, 1.0F, scale);
            keyframe.ownedTransform().translation().rotate(-entityYaw, Vec3f.Y_AXIS);
            keyframe.ownedTransform().translation().multiply(-1.0F, 1.0F, -1.0F);
            keyframe.ownedTransform().translation().add(startInWorld);
        }, 0, byStart.keyframes.length);
        byDestination.forEachOwned((frameIndex, keyframe) -> {
            float blend = destinationFrame == 0 ? 0.0F : frameIndex / (float) destinationFrame;
            float multiplied = scale + (1.0F - scale) * blend;
            keyframe.ownedTransform().translation().multiply(1.0F, 1.0F, multiplied);
            keyframe.ownedTransform().translation().rotate(-destinationYaw, Vec3f.Y_AXIS);
            keyframe.ownedTransform().translation().multiply(-1.0F, 1.0F, -1.0F);
            keyframe.ownedTransform().translation().add(destinationInWorld);
        }, 0, byDestination.keyframes.length);
        Keyframe[] corrected = new Keyframe[destinationFrame + 1];
        for (int frameIndex = 0; frameIndex < destinationFrame + 1; frameIndex++) {
            if (frameIndex <= startFrame) {
                corrected[frameIndex] = new Keyframe(this.keyframes[frameIndex].time(), JointTransform.translation(byStart.keyframes[frameIndex].ownedTransform().translation()));
            } else {
                float blend = this.keyframes[frameIndex].time() == 0.0F ? 0.0F : this.keyframes[frameIndex].time() / this.keyframes[destinationFrame].time();
                FiniteChecks.requireFinite("world correction blend", blend);
                Vec3f lerpTranslation = Vec3f.interpolate(
                        byStart.keyframes[frameIndex].ownedTransform().translation(),
                        byDestination.keyframes[frameIndex].ownedTransform().translation(),
                        blend,
                        null);
                corrected[frameIndex] = new Keyframe(this.keyframes[frameIndex].time(), JointTransform.translation(lerpTranslation));
            }
        }
        result.keyframes = validated(corrected);
        if (this.keyframes.length > destinationFrame + 1) {
            TransformSheet behindDestination = this.copy(destinationFrame + 1, this.keyframes.length);
            behindDestination.forEachOwned((frameIndex, keyframe) -> {
                keyframe.ownedTransform().translation().sub(this.keyframes[destinationFrame].ownedTransform().translation());
                keyframe.ownedTransform().translation().rotate(entityYaw, Vec3f.Y_AXIS);
                keyframe.ownedTransform().translation().multiply(-1.0F, 1.0F, -1.0F);
                keyframe.ownedTransform().translation().add(result.keyframes[destinationFrame].ownedTransform().translation());
            }, 0, behindDestination.keyframes.length);
            result.extend(behindDestination);
        }
        return result;
    }

    public InterpolationInfo getInterpolationInfo(float currentTime) {
        if (!Float.isFinite(currentTime)) {
            throw new IllegalArgumentException("sample time must be finite");
        }
        if (this.keyframes.length == 0) {
            return InterpolationInfo.INVALID;
        }
        SampleSearch.Result result = SampleSearch.bracket(this.keyframes.length, columnIndex -> this.keyframes[columnIndex].time(), currentTime);
        return new InterpolationInfo(result.begin(), result.end(), result.progression());
    }

    public float maxFrameTime() {
        float highestTime = -1.0F;
        for (Keyframe frame : this.keyframes) {
            if (frame.time() > highestTime) {
                highestTime = frame.time();
            }
        }
        return highestTime;
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder();
        int shown = 0;
        for (Keyframe frame : this.keyframes) {
            text.append(frame);
            if (++shown < this.keyframes.length) {
                text.append("\n");
            }
        }
        return text.toString();
    }

    public static record InterpolationInfo(int prev, int next, float delta) {
        public static final InterpolationInfo INVALID = new InterpolationInfo(-1, -1, -1.0F);
    }
}
