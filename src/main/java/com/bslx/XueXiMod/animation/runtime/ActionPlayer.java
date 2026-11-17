package com.bslx.XueXiMod.animation.runtime;

import java.util.ArrayList;
import java.util.List;

import com.bslx.XueXiMod.animation.core.AnimationClip;
import com.bslx.XueXiMod.animation.core.JointTransform;
import com.bslx.XueXiMod.animation.core.Pose;
import com.bslx.XueXiMod.animation.core.math.Vec3f;

import org.joml.Quaternionf;

/**
 * 一条姿态的播放、替换、暂停、结束和取消。
 * 采样走 AnimationClip。时间事件只进入视觉通知，大步长每个事件最多派发一次。
 */
public final class ActionPlayer {
    public enum Phase {
        IDLE,
        PLAYING,
        PAUSED,
        FINISHED,
        CANCELLED
    }

    public static final class Decision {
        private final boolean accepted;
        private final String reasonText;

        private Decision(boolean accepted, String reasonText) {
            this.accepted = accepted;
            this.reasonText = reasonText;
        }

        public static Decision accept() {
            return new Decision(true, "");
        }

        public static Decision reject(String reasonText) {
            return new Decision(false, reasonText == null ? "rejected" : reasonText);
        }

        public boolean accepted() {
            return this.accepted;
        }

        public String reasonText() {
            return this.reasonText;
        }
    }

    private Phase phase = Phase.IDLE;
    private ActionDefinition definition;
    private AnimationClip clip;
    private float speed = 1.0F;
    private boolean looping;
    private long startGameTick;
    private long clockOffsetTicks;
    private long pauseBeganTick = -1L;
    private float pausedElapsedSeconds;
    private float pausedBlendElapsedSeconds;
    private float pausedLocalSeconds;
    private boolean poseFrozen;
    private float frozenLocalSeconds;
    private float frozenBlendElapsedSeconds;
    private float previousElapsedSeconds;
    private boolean terminalEmitted;
    private Pose blendFrom;
    private float blendSeconds;
    private long blendStartGameTick;
    private final List<String> pendingNotices = new ArrayList<>();

    public Phase phase() {
        return this.phase;
    }

    public ActionDefinition definition() {
        return this.definition;
    }

    public boolean terminal() {
        return this.phase == Phase.FINISHED || this.phase == Phase.CANCELLED;
    }

    public long startGameTick() {
        return this.startGameTick;
    }

    public float speed() {
        return this.speed;
    }

    public boolean looping() {
        return this.looping;
    }

    public long clockOffsetTicks() {
        return this.clockOffsetTicks;
    }

    public long pauseBeganTick() {
        return this.pauseBeganTick;
    }

    public float pausedElapsedSeconds() {
        return this.pausedElapsedSeconds;
    }

    public float pausedBlendElapsedSeconds() {
        return this.pausedBlendElapsedSeconds;
    }

    public float unwrappedElapsed(long gameTick, float partialTick) {
        if (this.phase == Phase.PAUSED) {
            return this.pausedElapsedSeconds;
        }
        return this.elapsedSeconds(gameTick, partialTick, this.startGameTick);
    }

    public Decision play(ActionDefinition definition, AnimationClip clip, long startGameTick, float speed, boolean looping, float blendSeconds) {
        return this.adopt(definition, clip, startGameTick, startGameTick, speed, looping, blendSeconds);
    }

    public Decision adopt(ActionDefinition definition, AnimationClip clip, long startGameTick, long adoptGameTick, float speed, boolean looping, float blendSeconds) {
        if (this.phase == Phase.PLAYING || this.phase == Phase.PAUSED) {
            return Decision.reject("姿态还在播放，替换才不会取消当前这条");
        }
        return this.begin(definition, clip, startGameTick, speed, looping, blendSeconds, null, adoptGameTick, 0L, false, 0.0F, 0.0F, -1L);
    }

    public Decision adoptClock(
            ActionDefinition definition,
            AnimationClip clip,
            long startGameTick,
            long adoptGameTick,
            float speed,
            boolean looping,
            float blendSeconds,
            long clockOffsetTicks,
            boolean paused,
            float pausedElapsedSeconds,
            float pausedBlendElapsedSeconds,
            long pauseBeganTick) {
        if (this.phase == Phase.PLAYING || this.phase == Phase.PAUSED) {
            return Decision.reject("姿态还在播放，替换才不会取消当前这条");
        }
        return this.begin(definition, clip, startGameTick, speed, looping, blendSeconds, null, adoptGameTick,
                clockOffsetTicks, paused, pausedElapsedSeconds, pausedBlendElapsedSeconds, pauseBeganTick);
    }

    public Decision replace(ActionDefinition definition, AnimationClip clip, long nowGameTick, long startGameTick, float speed, boolean looping, float blendSeconds) {
        Pose captured = this.phase == Phase.PLAYING || this.phase == Phase.PAUSED
                ? copyPose(this.sample(nowGameTick, 0.0F))
                : null;
        return this.begin(definition, clip, startGameTick, speed, looping, blendSeconds, captured, nowGameTick, 0L, false, 0.0F, 0.0F, -1L);
    }

    public Decision pause(long nowGameTick) {
        if (this.phase != Phase.PLAYING) {
            return Decision.reject("只有播放中的姿态可以暂停");
        }
        float elapsed = this.elapsedSeconds(nowGameTick, 0.0F, this.startGameTick);
        float blendElapsed = this.elapsedSeconds(nowGameTick, 0.0F, this.blendStartGameTick);
        if (!Float.isFinite(elapsed) || elapsed < 0.0F || !Float.isFinite(blendElapsed) || blendElapsed < 0.0F) {
            return Decision.reject("暂停采样时间无效");
        }
        this.pausedElapsedSeconds = elapsed;
        this.pausedBlendElapsedSeconds = blendElapsed;
        this.pausedLocalSeconds = this.looping && this.clip != null ? this.clip.loopedSampleTime(elapsed) : elapsed;
        this.pauseBeganTick = nowGameTick;
        this.phase = Phase.PAUSED;
        return Decision.accept();
    }

    public Decision resume(long nowGameTick) {
        if (this.terminal() || this.phase != Phase.PAUSED) {
            return Decision.reject("只有暂停中的姿态可以恢复");
        }
        if (this.pauseBeganTick < 0L || nowGameTick < this.pauseBeganTick) {
            return Decision.reject("恢复时间早于暂停");
        }
        this.clockOffsetTicks += nowGameTick - this.pauseBeganTick;
        this.pauseBeganTick = -1L;
        this.phase = Phase.PLAYING;
        return Decision.accept();
    }

    public Decision alignClock(long clockOffsetTicks, boolean paused, float pausedElapsedSeconds, float pausedBlendElapsedSeconds, long pauseBeganTick) {
        if (this.terminal() || this.phase == Phase.IDLE || this.definition == null) {
            return Decision.reject("没有可对齐的姿态");
        }
        if (clockOffsetTicks < 0L || !Float.isFinite(pausedElapsedSeconds) || pausedElapsedSeconds < 0.0F
                || !Float.isFinite(pausedBlendElapsedSeconds) || pausedBlendElapsedSeconds < 0.0F) {
            return Decision.reject("时钟无效");
        }
        this.clockOffsetTicks = clockOffsetTicks;
        if (paused) {
            this.pausedElapsedSeconds = pausedElapsedSeconds;
            this.pausedBlendElapsedSeconds = pausedBlendElapsedSeconds;
            this.pausedLocalSeconds = this.looping && this.clip != null
                    ? this.clip.loopedSampleTime(pausedElapsedSeconds)
                    : pausedElapsedSeconds;
            this.pauseBeganTick = pauseBeganTick;
            this.phase = Phase.PAUSED;
            if (this.previousElapsedSeconds < pausedElapsedSeconds) {
                this.previousElapsedSeconds = pausedElapsedSeconds;
            }
            return Decision.accept();
        }
        this.pauseBeganTick = -1L;
        this.phase = Phase.PLAYING;
        if (this.previousElapsedSeconds < pausedElapsedSeconds) {
            this.previousElapsedSeconds = pausedElapsedSeconds;
        }
        return Decision.accept();
    }

    public Decision hold(float sampleSeconds) {
        if (this.phase != Phase.PLAYING) {
            return Decision.reject("只有播放中的姿态可以暂停");
        }
        if (!Float.isFinite(sampleSeconds) || sampleSeconds < 0.0F) {
            return Decision.reject("暂停采样时间无效");
        }
        this.pausedLocalSeconds = sampleSeconds;
        this.pausedElapsedSeconds = sampleSeconds;
        this.phase = Phase.PAUSED;
        return Decision.accept();
    }

    public Decision finish(long nowGameTick) {
        if (this.phase == Phase.IDLE) {
            return Decision.reject("没有正在播放的姿态");
        }
        if (this.terminal()) {
            return Decision.reject("姿态已经结束");
        }
        this.captureFrozenPose(nowGameTick);
        this.phase = Phase.FINISHED;
        this.emitTerminalOnce("finish");
        return Decision.accept();
    }

    public Decision finish() {
        if (this.phase == Phase.IDLE) {
            return Decision.reject("没有正在播放的姿态");
        }
        if (this.terminal()) {
            return Decision.reject("姿态已经结束");
        }
        return Decision.reject("需要明确的结束时间");
    }

    public Decision stop() {
        if (this.phase == Phase.IDLE) {
            return Decision.reject("没有正在播放的姿态");
        }
        if (this.terminal()) {
            return Decision.reject("姿态已经是终态");
        }
        this.phase = Phase.CANCELLED;
        this.blendFrom = null;
        this.emitTerminalOnce("cancel");
        return Decision.accept();
    }

    public Pose sample(long gameTick, float partialTick) {
        if (this.phase == Phase.IDLE || this.phase == Phase.CANCELLED || this.definition == null || this.clip == null) {
            return new Pose();
        }
        float sampleSeconds = this.sampleSeconds(gameTick, partialTick);
        boolean wrapLoop = this.looping && !this.poseFrozen;
        Pose incoming = this.clip.getPoseInTime(wrapLoop ? this.clip.loopedSampleTime(sampleSeconds) : sampleSeconds);
        Pose blended = incoming;
        if (this.blendFrom != null && this.blendSeconds > 0.0F) {
            float blendElapsed = this.poseFrozen
                    ? this.frozenBlendElapsedSeconds
                    : this.phase == Phase.PAUSED
                            ? this.pausedBlendElapsedSeconds
                            : this.elapsedSeconds(gameTick, partialTick, this.blendStartGameTick);
            float progression = clamp01(blendElapsed / this.blendSeconds);
            blended = Pose.interpolatePose(this.blendFrom, incoming, progression);
        }
        return this.applyMask(blended);
    }

    public float sampleSeconds(long gameTick, float partialTick) {
        if (this.poseFrozen) {
            return this.frozenLocalSeconds;
        }
        if (this.phase == Phase.PAUSED) {
            return this.pausedLocalSeconds;
        }
        float elapsed = this.elapsedSeconds(gameTick, partialTick, this.startGameTick);
        if (this.looping && this.clip != null) {
            return this.clip.loopedSampleTime(elapsed);
        }
        return elapsed;
    }

    public boolean reachedEnd(long gameTick) {
        if (this.looping || this.definition == null || this.phase != Phase.PLAYING) {
            return false;
        }
        return this.elapsedSeconds(gameTick, 0.0F, this.startGameTick) >= this.definition.durationSeconds();
    }

    public List<String> drainNotices() {
        List<String> drained = List.copyOf(this.pendingNotices);
        this.pendingNotices.clear();
        return drained;
    }

    public void advanceEvents(long gameTick, float partialTick) {
        if (this.definition == null || this.phase != Phase.PLAYING) {
            return;
        }
        float elapsedSeconds = this.elapsedSeconds(gameTick, partialTick, this.startGameTick);
        if (!(elapsedSeconds > this.previousElapsedSeconds)) {
            return;
        }
        this.dispatchNotices(this.previousElapsedSeconds, elapsedSeconds);
        this.previousElapsedSeconds = elapsedSeconds;
        if (!this.looping && elapsedSeconds >= this.definition.durationSeconds()) {
            this.finish(gameTick);
        }
    }

    private void captureFrozenPose(long nowGameTick) {
        if (this.phase == Phase.PAUSED) {
            this.frozenLocalSeconds = this.pausedLocalSeconds;
            this.frozenBlendElapsedSeconds = this.pausedBlendElapsedSeconds;
        } else if (!this.looping) {
            this.frozenLocalSeconds = this.definition.durationSeconds();
            this.frozenBlendElapsedSeconds = this.elapsedSeconds(nowGameTick, 0.0F, this.blendStartGameTick);
        } else {
            this.frozenLocalSeconds = this.sampleSeconds(nowGameTick, 0.0F);
            this.frozenBlendElapsedSeconds = this.elapsedSeconds(nowGameTick, 0.0F, this.blendStartGameTick);
        }
        this.poseFrozen = true;
    }

    private Decision begin(
            ActionDefinition definition,
            AnimationClip clip,
            long startGameTick,
            float speed,
            boolean looping,
            float blendSeconds,
            Pose captured,
            long adoptGameTick,
            long clockOffsetTicks,
            boolean openPaused,
            float pausedElapsedSeconds,
            float pausedBlendElapsedSeconds,
            long pauseBeganTick) {
        String invalid = validate(definition, clip, speed, looping, blendSeconds);
        if (invalid != null) {
            return Decision.reject(invalid);
        }
        if (clockOffsetTicks < 0L || !Float.isFinite(pausedElapsedSeconds) || pausedElapsedSeconds < 0.0F
                || !Float.isFinite(pausedBlendElapsedSeconds) || pausedBlendElapsedSeconds < 0.0F) {
            return Decision.reject("时钟无效");
        }
        this.definition = definition;
        this.clip = clip;
        this.speed = speed;
        this.looping = looping;
        this.startGameTick = startGameTick;
        this.clockOffsetTicks = clockOffsetTicks;
        this.blendSeconds = blendSeconds;
        this.blendFrom = blendSeconds > 0.0F ? captured : null;
        this.blendStartGameTick = adoptGameTick;
        this.terminalEmitted = false;
        this.poseFrozen = false;
        this.frozenLocalSeconds = 0.0F;
        this.frozenBlendElapsedSeconds = 0.0F;
        this.phase = Phase.PLAYING;
        if (openPaused) {
            this.pausedElapsedSeconds = pausedElapsedSeconds;
            this.pausedBlendElapsedSeconds = pausedBlendElapsedSeconds;
            this.pausedLocalSeconds = looping && clip != null ? clip.loopedSampleTime(pausedElapsedSeconds) : pausedElapsedSeconds;
            this.pauseBeganTick = pauseBeganTick;
            this.previousElapsedSeconds = pausedElapsedSeconds;
            this.phase = Phase.PAUSED;
            return Decision.accept();
        }
        this.pauseBeganTick = -1L;
        this.pausedElapsedSeconds = 0.0F;
        this.pausedBlendElapsedSeconds = 0.0F;
        this.pausedLocalSeconds = 0.0F;
        float adoptedElapsed = this.elapsedSeconds(adoptGameTick, 0.0F, startGameTick);
        if (!looping && adoptedElapsed >= definition.durationSeconds()) {
            this.previousElapsedSeconds = adoptedElapsed;
            this.frozenLocalSeconds = definition.durationSeconds();
            this.frozenBlendElapsedSeconds = this.elapsedSeconds(adoptGameTick, 0.0F, this.blendStartGameTick);
            this.poseFrozen = true;
            this.phase = Phase.FINISHED;
            this.emitTerminalOnce("finish");
            return Decision.accept();
        }
        if (adoptedElapsed <= 0.00001F) {
            this.previousElapsedSeconds = -1.0F;
            this.dispatchNotices(-1.0F, 0.0F);
            this.previousElapsedSeconds = 0.0F;
        } else {
            this.previousElapsedSeconds = adoptedElapsed;
        }
        return Decision.accept();
    }

    private void emitTerminalOnce(String noticeId) {
        if (this.terminalEmitted) {
            return;
        }
        this.terminalEmitted = true;
        this.pendingNotices.add(noticeId);
    }

    private void dispatchNotices(float fromSeconds, float toSeconds) {
        List<ActionDefinition.TimeNotice> notices = this.definition.notices();
        if (notices.isEmpty()) {
            return;
        }
        float duration = this.definition.durationSeconds();
        if (!(toSeconds > fromSeconds)) {
            return;
        }
        if (!this.looping) {
            float endSeconds = Math.min(toSeconds, duration);
            for (ActionDefinition.TimeNotice notice : notices) {
                if (notice.seconds() > fromSeconds && notice.seconds() <= endSeconds) {
                    this.pendingNotices.add(notice.noticeId());
                }
            }
            return;
        }
        float span = toSeconds - fromSeconds;
        if (span >= duration) {
            for (ActionDefinition.TimeNotice notice : notices) {
                this.pendingNotices.add(notice.noticeId());
            }
            return;
        }
        float fromWrapped = this.clip.loopedSampleTime(Math.max(fromSeconds, 0.0F));
        float toWrapped = this.clip.loopedSampleTime(toSeconds);
        for (ActionDefinition.TimeNotice notice : notices) {
            boolean crossed = toWrapped >= fromWrapped
                    ? notice.seconds() > fromWrapped && notice.seconds() <= toWrapped
                    : notice.seconds() > fromWrapped || notice.seconds() <= toWrapped;
            if (crossed) {
                this.pendingNotices.add(notice.noticeId());
            }
        }
    }

    private Pose applyMask(Pose source) {
        Pose output = new Pose();
        boolean limited = !this.definition.boneMask().isEmpty();
        if (limited && this.blendFrom != null) {
            this.blendFrom.forEachEnabledTransforms((jointName, transform) -> {
                if (!this.definition.boneMask().contains(jointName)) {
                    output.putJointData(jointName, transform.copy());
                }
            });
        }
        source.forEachEnabledTransforms((jointName, transform) -> {
            if (limited && !this.definition.boneMask().contains(jointName)) {
                return;
            }
            output.putJointData(jointName, filterChannels(transform, this.definition.channels()));
        });
        return output;
    }

    private static JointTransform filterChannels(JointTransform source, java.util.Set<ActionDefinition.Channel> channels) {
        Vec3f translation = channels.contains(ActionDefinition.Channel.TRANSLATION)
                ? source.translation().copy()
                : new Vec3f(0.0F, 0.0F, 0.0F);
        Quaternionf rotation = channels.contains(ActionDefinition.Channel.ROTATION)
                ? new Quaternionf(source.rotation())
                : new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F);
        Vec3f scale = channels.contains(ActionDefinition.Channel.SCALE)
                ? source.scale().copy()
                : new Vec3f(1.0F, 1.0F, 1.0F);
        return new JointTransform(translation, rotation, scale);
    }

    private static Pose copyPose(Pose source) {
        Pose copy = new Pose();
        if (source != null) {
            source.forEachEnabledTransforms((jointName, transform) -> copy.putJointData(jointName, transform.copy()));
        }
        return copy;
    }

    private float elapsedSeconds(long gameTick, float partialTick, long originTick) {
        float fraction = Float.isFinite(partialTick) ? clamp01(partialTick) : 0.0F;
        double elapsedTicks = (gameTick - originTick - this.clockOffsetTicks) + fraction;
        if (elapsedTicks < 0.0D) {
            elapsedTicks = 0.0D;
        }
        double seconds = elapsedTicks / 20.0D * this.speed;
        if (!Double.isFinite(seconds) || seconds > 1.0E6D) {
            return 1.0E6F;
        }
        return (float) seconds;
    }

    private static float clamp01(float value) {
        if (value < 0.0F) {
            return 0.0F;
        }
        if (value > 1.0F) {
            return 1.0F;
        }
        return value;
    }

    public static String validate(ActionDefinition definition, AnimationClip clip, float speed, boolean looping, float blendSeconds) {
        if (definition == null || clip == null) {
            return "动作或剪辑缺失";
        }
        if (!Float.isFinite(speed) || speed <= 0.0F || speed > 20.0F) {
            return "速度必须是 0 到 20 之间的有限正数";
        }
        if (!Float.isFinite(clip.getClipTime()) || clip.getClipTime() <= 0.0F) {
            return "剪辑时长必须是有限正数";
        }
        if (!Float.isFinite(definition.durationSeconds()) || definition.durationSeconds() <= 0.0F) {
            return "动作时长必须是有限正数";
        }
        if (looping && !(clip.getClipTime() > 0.0F)) {
            return "时长不足，不能循环";
        }
        if (!Float.isFinite(blendSeconds) || blendSeconds < 0.0F || blendSeconds > 2.0F) {
            return "混合时长必须在 0 到 2 秒之间";
        }
        return null;
    }
}
