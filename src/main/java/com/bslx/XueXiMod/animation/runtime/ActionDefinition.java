package com.bslx.XueXiMod.animation.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;

/** 稳定动作定义。播放器只按这个定义取样，不按术式编号分支。 */
public final class ActionDefinition {
    public enum Channel {
        TRANSLATION,
        ROTATION,
        SCALE
    }

    public static final class TimeNotice {
        private final float seconds;
        private final String noticeId;

        public TimeNotice(float seconds, String noticeId) {
            if (!Float.isFinite(seconds) || seconds < 0.0F) {
                throw new IllegalArgumentException("notice time must be finite and non-negative");
            }
            if (noticeId == null || noticeId.isBlank() || noticeId.length() > 64) {
                throw new IllegalArgumentException("notice id is missing");
            }
            this.seconds = seconds;
            this.noticeId = noticeId;
        }

        public float seconds() {
            return this.seconds;
        }

        public String noticeId() {
            return this.noticeId;
        }
    }

    private final ResourceLocation actionId;
    private final String armatureName;
    private final float durationSeconds;
    private final boolean loopingDefault;
    private final float blendSeconds;
    private final Set<String> boneMask;
    private final Set<Channel> channels;
    private final List<TimeNotice> notices;
    private final String originLabel;

    public ActionDefinition(
            ResourceLocation actionId,
            String armatureName,
            float durationSeconds,
            boolean loopingDefault,
            float blendSeconds,
            Set<String> boneMask,
            Set<Channel> channels,
            List<TimeNotice> notices,
            String originLabel) {
        if (actionId == null || armatureName == null || armatureName.isBlank()) {
            throw new IllegalArgumentException("action definition is missing");
        }
        if (!Float.isFinite(durationSeconds) || durationSeconds <= 0.0F) {
            throw new IllegalArgumentException("action duration must be finite and positive");
        }
        if (!Float.isFinite(blendSeconds) || blendSeconds < 0.0F || blendSeconds > 2.0F) {
            throw new IllegalArgumentException("blend duration is outside the supported range");
        }
        if (channels == null || channels.isEmpty()) {
            throw new IllegalArgumentException("pose channels are missing");
        }
        if (originLabel == null || originLabel.isBlank()) {
            throw new IllegalArgumentException("action origin is missing");
        }
        Set<String> maskCopy = new LinkedHashSet<>();
        if (boneMask != null) {
            for (String boneName : boneMask) {
                if (boneName == null || boneName.isBlank()) {
                    throw new IllegalArgumentException("bone mask entry is missing");
                }
                maskCopy.add(boneName);
            }
        }
        if (maskCopy.size() > 64) {
            throw new IllegalArgumentException("bone mask is too large");
        }
        List<TimeNotice> noticeCopy = new ArrayList<>();
        if (notices != null) {
            for (TimeNotice notice : notices) {
                if (notice == null) {
                    throw new IllegalArgumentException("notice is missing");
                }
                if (notice.seconds > durationSeconds) {
                    throw new IllegalArgumentException("notice time is past the action duration");
                }
                noticeCopy.add(notice);
            }
        }
        if (noticeCopy.size() > 32) {
            throw new IllegalArgumentException("notice list is too large");
        }
        this.actionId = actionId;
        this.armatureName = armatureName;
        this.durationSeconds = durationSeconds;
        this.loopingDefault = loopingDefault;
        this.blendSeconds = blendSeconds;
        this.boneMask = Collections.unmodifiableSet(maskCopy);
        this.channels = Collections.unmodifiableSet(EnumSet.copyOf(channels));
        this.notices = List.copyOf(noticeCopy);
        this.originLabel = originLabel;
    }

    public ResourceLocation actionId() {
        return this.actionId;
    }

    public String armatureName() {
        return this.armatureName;
    }

    public float durationSeconds() {
        return this.durationSeconds;
    }

    public boolean loopingDefault() {
        return this.loopingDefault;
    }

    public float blendSeconds() {
        return this.blendSeconds;
    }

    public Set<String> boneMask() {
        return this.boneMask;
    }

    public Set<Channel> channels() {
        return this.channels;
    }

    public List<TimeNotice> notices() {
        return this.notices;
    }

    public String originLabel() {
        return this.originLabel;
    }
}
