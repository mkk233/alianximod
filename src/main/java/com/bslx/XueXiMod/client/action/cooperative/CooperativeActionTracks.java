package com.bslx.XueXiMod.client.action.cooperative;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeActionIds;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

/**
 * 读取确认版动作曲线。前摇和收势按服务器冻结刻数分别拉伸，不使用动画通知。
 */
public final class CooperativeActionTracks {
    private static final ResourceLocation SLASH_PATH = ResourceLocation.fromNamespaceAndPath(
            XueXiMod.MOD_ID, "actions/cooperative_v1/oblique_slash.json");
    private static final ResourceLocation SPIN_PATH = ResourceLocation.fromNamespaceAndPath(
            XueXiMod.MOD_ID, "actions/cooperative_v1/spin.json");
    private static Clip slashClip;
    private static Clip spinClip;
    private static boolean slashMissing;
    private static boolean spinMissing;

    private CooperativeActionTracks() {
    }

    public static CooperativePose sample(
            ResourceLocation actionId,
            boolean mirrored,
            int windupTicks,
            int recoveryTicks,
            float elapsedTicks,
            float facingYawDegrees) {
        Clip clip = clipFor(actionId);
        if (clip == null) {
            return null;
        }
        float authoredSeconds = authoredSeconds(clip, windupTicks, recoveryTicks, elapsedTicks);
        Track track = mirrored ? clip.mirrored : clip.normal;
        return new CooperativePose(
                track.body.sample(authoredSeconds, 0),
                track.body.sample(authoredSeconds, 1),
                track.body.sample(authoredSeconds, 2),
                track.head.sample(authoredSeconds, 0),
                track.head.sample(authoredSeconds, 1),
                track.head.sample(authoredSeconds, 2),
                track.armRight.sample(authoredSeconds, 0),
                track.armRight.sample(authoredSeconds, 1),
                track.armRight.sample(authoredSeconds, 2),
                track.armLeft.sample(authoredSeconds, 0),
                track.armLeft.sample(authoredSeconds, 1),
                track.armLeft.sample(authoredSeconds, 2),
                track.legRight.sample(authoredSeconds, 0),
                track.legRight.sample(authoredSeconds, 1),
                track.legRight.sample(authoredSeconds, 2),
                track.legLeft.sample(authoredSeconds, 0),
                track.legLeft.sample(authoredSeconds, 1),
                track.legLeft.sample(authoredSeconds, 2),
                track.rootYaw.sampleYaw(authoredSeconds),
                facingYawDegrees);
    }

    public static boolean loaded(ResourceLocation actionId) {
        return clipFor(actionId) != null;
    }

    private static float authoredSeconds(Clip clip, int windupTicks, int recoveryTicks, float elapsedTicks) {
        float elapsed = elapsedTicks < 0.0F ? 0.0F : elapsedTicks;
        float recoverySpan = recoveryTicks < 1 ? 1.0F : recoveryTicks;
        if (windupTicks <= 0) {
            float recoveryRatio = elapsed / recoverySpan;
            if (recoveryRatio > 1.0F) {
                recoveryRatio = 1.0F;
            }
            return clip.windupSeconds + clip.recoverySeconds * recoveryRatio;
        }
        if (elapsed <= windupTicks) {
            return clip.windupSeconds * (elapsed / windupTicks);
        }
        float recoveryElapsed = elapsed - windupTicks;
        if (recoveryElapsed > recoverySpan) {
            recoveryElapsed = recoverySpan;
        }
        return clip.windupSeconds + clip.recoverySeconds * (recoveryElapsed / recoverySpan);
    }

    private static Clip clipFor(ResourceLocation actionId) {
        if (CooperativeActionIds.OBLIQUE_SLASH.equals(actionId)) {
            if (slashClip == null && !slashMissing) {
                slashClip = read(SLASH_PATH);
                slashMissing = slashClip == null;
            }
            return slashClip;
        }
        if (CooperativeActionIds.SPIN.equals(actionId)) {
            if (spinClip == null && !spinMissing) {
                spinClip = read(SPIN_PATH);
                spinMissing = spinClip == null;
            }
            return spinClip;
        }
        return null;
    }

    private static Clip read(ResourceLocation location) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getResourceManager() == null) {
            return null;
        }
        try {
            Resource resource = minecraft.getResourceManager().getResource(location).orElse(null);
            if (resource == null) {
                return null;
            }
            try (InputStream input = resource.open()) {
                String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                JsonObject root = JsonParser.parseString(text).getAsJsonObject();
                float windupSeconds = root.get("authoredWindupSeconds").getAsFloat();
                float recoverySeconds = root.get("authoredRecoverySeconds").getAsFloat();
                if (!(windupSeconds >= 0.0F) || !(recoverySeconds > 0.0F)) {
                    return null;
                }
                Track normal = track(root.getAsJsonObject("normal"));
                Track mirrored = track(root.getAsJsonObject("mirrored"));
                if (normal == null || mirrored == null) {
                    return null;
                }
                return new Clip(windupSeconds, recoverySeconds, normal, mirrored);
            }
        } catch (RuntimeException | java.io.IOException failure) {
            return null;
        }
    }

    private static Track track(JsonObject source) {
        if (source == null) {
            return null;
        }
        Curve body = curve(source.get("body"));
        Curve head = curve(source.get("head"));
        Curve armRight = curve(source.get("arm_r"));
        Curve armLeft = curve(source.get("arm_l"));
        Curve legRight = curve(source.get("leg_r"));
        Curve legLeft = curve(source.get("leg_l"));
        Curve rootYaw = yawCurve(source.get("rootYaw"));
        if (body == null || head == null || armRight == null || armLeft == null
                || legRight == null || legLeft == null || rootYaw == null) {
            return null;
        }
        return new Track(body, head, armRight, armLeft, legRight, legLeft, rootYaw);
    }

    private static Curve curve(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return null;
        }
        JsonArray rows = element.getAsJsonArray();
        List<float[]> frames = new ArrayList<>(rows.size());
        for (JsonElement rowElement : rows) {
            JsonArray row = rowElement.getAsJsonArray();
            if (row.size() < 4) {
                return null;
            }
            frames.add(new float[] {
                    row.get(0).getAsFloat(),
                    row.get(1).getAsFloat(),
                    row.get(2).getAsFloat(),
                    row.get(3).getAsFloat()
            });
        }
        if (frames.isEmpty()) {
            return null;
        }
        return new Curve(frames);
    }

    private static Curve yawCurve(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return null;
        }
        JsonArray rows = element.getAsJsonArray();
        List<float[]> frames = new ArrayList<>(rows.size());
        for (JsonElement rowElement : rows) {
            JsonArray row = rowElement.getAsJsonArray();
            if (row.size() < 2) {
                return null;
            }
            frames.add(new float[] {row.get(0).getAsFloat(), row.get(1).getAsFloat(), 0.0F, 0.0F});
        }
        if (frames.isEmpty()) {
            return null;
        }
        return new Curve(frames);
    }

    private static final class Clip {
        private final float windupSeconds;
        private final float recoverySeconds;
        private final Track normal;
        private final Track mirrored;

        private Clip(float windupSeconds, float recoverySeconds, Track normal, Track mirrored) {
            this.windupSeconds = windupSeconds;
            this.recoverySeconds = recoverySeconds;
            this.normal = normal;
            this.mirrored = mirrored;
        }
    }

    private static final class Track {
        private final Curve body;
        private final Curve head;
        private final Curve armRight;
        private final Curve armLeft;
        private final Curve legRight;
        private final Curve legLeft;
        private final Curve rootYaw;

        private Track(Curve body, Curve head, Curve armRight, Curve armLeft, Curve legRight, Curve legLeft, Curve rootYaw) {
            this.body = body;
            this.head = head;
            this.armRight = armRight;
            this.armLeft = armLeft;
            this.legRight = legRight;
            this.legLeft = legLeft;
            this.rootYaw = rootYaw;
        }
    }

    private static final class Curve {
        private final List<float[]> frames;

        private Curve(List<float[]> frames) {
            this.frames = frames;
        }

        private float sample(float seconds, int component) {
            float[] first = this.frames.get(0);
            if (seconds <= first[0]) {
                return first[component + 1];
            }
            float[] last = this.frames.get(this.frames.size() - 1);
            if (seconds >= last[0]) {
                return last[component + 1];
            }
            for (int frameIndex = 1; frameIndex < this.frames.size(); frameIndex++) {
                float[] right = this.frames.get(frameIndex);
                float[] left = this.frames.get(frameIndex - 1);
                if (seconds <= right[0]) {
                    float span = right[0] - left[0];
                    float ratio = span <= 1.0E-6F ? 0.0F : (seconds - left[0]) / span;
                    return left[component + 1] + (right[component + 1] - left[component + 1]) * ratio;
                }
            }
            return last[component + 1];
        }

        private float sampleYaw(float seconds) {
            return this.sample(seconds, 0);
        }
    }
}
