package com.bslx.XueXiMod.client.action.spacecut;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.joml.Quaternionf;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.runtime.ActionSampleContext;
import com.bslx.XueXiMod.animation.runtime.OriginalHumanoid;
import com.bslx.XueXiMod.client.action.ActionRenderQueries;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.player.Player;

/**
 * 左腰右拔的 9 骨关键帧。先在 Blockbench 空间用 ZYX 沿父级合成，
 * 再把已经合成的旋转做成 (qx, -qy, qz, qw)，平移写成 (x, 24-y, z)。
 * 不对每一级四元数先翻 Y。
 */
public final class SpaceCutSampler {
    static final float PIXEL = 0.0625F;
    private static final ResourceLocation TRACKS = new ResourceLocation(
            XueXiMod.MOD_ID, "actions/space_cut_left_waist_v5.json");
    private static final float PALM_TOLERANCE = 0.05F;
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    private static TrackLibrary library;
    private static boolean loadFailed;

    private SpaceCutSampler() {
    }

    public static SpaceCutFrame sample(Player player, float partialTick) {
        if (player == null) {
            return null;
        }
        ActionSampleContext context = ActionRenderQueries.inspect(player, partialTick);
        if (context == null || !context.active() || context.visualActionId() == null) {
            return null;
        }
        if (!OriginalHumanoid.SPACE_CUT_ID.equals(context.visualActionId())) {
            return null;
        }
        TrackLibrary tracks = tracks();
        if (tracks == null) {
            return null;
        }
        float elapsed = context.elapsedSeconds();
        if (!Float.isFinite(elapsed) || elapsed < 0.0F) {
            elapsed = 0.0F;
        }
        return compose(player.getUUID(), elapsed, tracks);
    }

    private static TrackLibrary tracks() {
        if (library != null) {
            return library;
        }
        if (loadFailed) {
            return null;
        }
        try {
            library = load();
            if (!acceptsStart(library)) {
                library = null;
                loadFailed = true;
                LOGGER.error("空间切割左腰右拔起点掌位和确认帧不一致，停止使用这份姿态");
                return null;
            }
            return library;
        } catch (RuntimeException failure) {
            loadFailed = true;
            LOGGER.error("空间切割左腰右拔姿态没有读入", failure);
            return null;
        }
    }

    private static boolean acceptsStart(TrackLibrary tracks) {
        SolvedBone palm = solve(tracks, "SwordHand", 0.0F, new HashMap<>());
        float errorX = palm.worldX - 5.3F;
        float errorY = palm.worldY - 15.5F;
        float errorZ = palm.worldZ - (-3.6F);
        float error = (float) Math.sqrt(errorX * errorX + errorY * errorY + errorZ * errorZ);
        return error <= PALM_TOLERANCE;
    }

    private static SpaceCutFrame compose(UUID ownerId, float elapsedSeconds, TrackLibrary tracks) {
        Map<String, SolvedBone> solved = new HashMap<>();
        SpaceCutFrame.SpaceCutBone body = bake(tracks, "Body", elapsedSeconds, solved);
        SpaceCutFrame.SpaceCutBone head = bake(tracks, "Head", elapsedSeconds, solved);
        SpaceCutFrame.SpaceCutBone armRight = bake(tracks, "ArmRight", elapsedSeconds, solved);
        SpaceCutFrame.SpaceCutBone forearmRight = bake(tracks, "ForearmRight", elapsedSeconds, solved);
        SpaceCutFrame.SpaceCutBone swordHand = bake(tracks, "SwordHand", elapsedSeconds, solved);
        SpaceCutFrame.SpaceCutBone armLeft = bake(tracks, "ArmLeft", elapsedSeconds, solved);
        SpaceCutFrame.SpaceCutBone forearmLeft = bake(tracks, "ForearmLeft", elapsedSeconds, solved);
        SpaceCutFrame.SpaceCutBone legRight = bake(tracks, "LegRight", elapsedSeconds, solved);
        SpaceCutFrame.SpaceCutBone legLeft = bake(tracks, "LegLeft", elapsedSeconds, solved);
        OpenMatrix4f swordRotation = swordHand.rotation();
        return new SpaceCutFrame(
                ownerId,
                elapsedSeconds,
                body,
                head,
                armRight,
                forearmRight,
                swordHand,
                armLeft,
                forearmLeft,
                legRight,
                legLeft,
                -swordRotation.m10,
                -swordRotation.m11,
                -swordRotation.m12);
    }

    private static SpaceCutFrame.SpaceCutBone bake(
            TrackLibrary tracks,
            String boneName,
            float elapsedSeconds,
            Map<String, SolvedBone> solved) {
        SolvedBone solvedBone = solve(tracks, boneName, elapsedSeconds, solved);
        return new SpaceCutFrame.SpaceCutBone(
                solvedBone.worldX,
                24.0F - solvedBone.worldY,
                solvedBone.worldZ,
                modelRotation(solvedBone.rotation));
    }

    private static SolvedBone solve(
            TrackLibrary tracks,
            String boneName,
            float elapsedSeconds,
            Map<String, SolvedBone> solved) {
        SolvedBone cached = solved.get(boneName);
        if (cached != null) {
            return cached;
        }
        BoneTrack track = tracks.bone(boneName);
        float[] rotationDegrees = track.sampleRotation(elapsedSeconds);
        float[] shift = track.samplePosition(elapsedSeconds);
        float[] pivot = tracks.pivot(boneName);
        Quaternionf localRotation = blockbenchZyx(rotationDegrees[0], rotationDegrees[1], rotationDegrees[2]);
        float localX = pivot[0] + shift[0];
        float localY = pivot[1] + shift[1];
        float localZ = pivot[2] + shift[2];
        String parentName = tracks.parent(boneName);
        SolvedBone solvedBone;
        if (parentName == null) {
            solvedBone = new SolvedBone(localX, localY, localZ, localRotation);
        } else {
            SolvedBone parent = solve(tracks, parentName, elapsedSeconds, solved);
            float[] turned = rotate(parent.rotation, localX, localY, localZ);
            solvedBone = new SolvedBone(
                    parent.worldX + turned[0],
                    parent.worldY + turned[1],
                    parent.worldZ + turned[2],
                    product(parent.rotation, localRotation));
        }
        solved.put(boneName, solvedBone);
        return solvedBone;
    }

    static OpenMatrix4f modelRotation(Quaternionf blockbenchWorld) {
        Quaternionf flipped = new Quaternionf(
                blockbenchWorld.x(),
                -blockbenchWorld.y(),
                blockbenchWorld.z(),
                blockbenchWorld.w());
        float square = flipped.x() * flipped.x() + flipped.y() * flipped.y()
                + flipped.z() * flipped.z() + flipped.w() * flipped.w();
        if (!(square > 1.0E-8F)) {
            return new OpenMatrix4f();
        }
        float inverse = 1.0F / (float) Math.sqrt(square);
        flipped.set(flipped.x() * inverse, flipped.y() * inverse, flipped.z() * inverse, flipped.w() * inverse);
        return OpenMatrix4f.fromQuaternion(flipped);
    }

    private static Quaternionf blockbenchZyx(float pitchDegrees, float yawDegrees, float rollDegrees) {
        double halfPitch = Math.toRadians(pitchDegrees) * 0.5;
        double halfYaw = Math.toRadians(yawDegrees) * 0.5;
        double halfRoll = Math.toRadians(rollDegrees) * 0.5;
        double cosPitch = Math.cos(halfPitch);
        double sinPitch = Math.sin(halfPitch);
        double cosYaw = Math.cos(halfYaw);
        double sinYaw = Math.sin(halfYaw);
        double cosRoll = Math.cos(halfRoll);
        double sinRoll = Math.sin(halfRoll);
        return new Quaternionf(
                (float) (sinPitch * cosYaw * cosRoll - cosPitch * sinYaw * sinRoll),
                (float) (cosPitch * sinYaw * cosRoll + sinPitch * cosYaw * sinRoll),
                (float) (cosPitch * cosYaw * sinRoll - sinPitch * sinYaw * cosRoll),
                (float) (cosPitch * cosYaw * cosRoll + sinPitch * sinYaw * sinRoll));
    }

    private static Quaternionf product(Quaternionf left, Quaternionf right) {
        float leftX = left.x();
        float leftY = left.y();
        float leftZ = left.z();
        float leftW = left.w();
        float rightX = right.x();
        float rightY = right.y();
        float rightZ = right.z();
        float rightW = right.w();
        return new Quaternionf(
                leftW * rightX + leftX * rightW + leftY * rightZ - leftZ * rightY,
                leftW * rightY - leftX * rightZ + leftY * rightW + leftZ * rightX,
                leftW * rightZ + leftX * rightY - leftY * rightX + leftZ * rightW,
                leftW * rightW - leftX * rightX - leftY * rightY - leftZ * rightZ);
    }

    private static float[] rotate(Quaternionf quaternion, float coordinateX, float coordinateY, float coordinateZ) {
        float quatX = quaternion.x();
        float quatY = quaternion.y();
        float quatZ = quaternion.z();
        float quatW = quaternion.w();
        float tempX = 2.0F * (quatY * coordinateZ - quatZ * coordinateY);
        float tempY = 2.0F * (quatZ * coordinateX - quatX * coordinateZ);
        float tempZ = 2.0F * (quatX * coordinateY - quatY * coordinateX);
        return new float[] {
                coordinateX + quatW * tempX + (quatY * tempZ - quatZ * tempY),
                coordinateY + quatW * tempY + (quatZ * tempX - quatX * tempZ),
                coordinateZ + quatW * tempZ + (quatX * tempY - quatY * tempX)
        };
    }

    private static TrackLibrary load() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            throw new IllegalStateException("客户端尚未就绪");
        }
        Optional<Resource> resource = minecraft.getResourceManager().getResource(TRACKS);
        if (resource.isEmpty()) {
            throw new IllegalStateException("缺少左腰右拔姿态");
        }
        try (InputStreamReader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject bones = root.getAsJsonObject("bones");
            Map<String, BoneTrack> tracks = new HashMap<>();
            for (String boneName : bones.keySet()) {
                tracks.put(boneName, BoneTrack.read(bones.getAsJsonObject(boneName)));
            }
            return new TrackLibrary(tracks);
        } catch (Exception failure) {
            throw new IllegalStateException("左腰右拔姿态无法解析", failure);
        }
    }

    private static final class SolvedBone {
        private final float worldX;
        private final float worldY;
        private final float worldZ;
        private final Quaternionf rotation;

        private SolvedBone(float worldX, float worldY, float worldZ, Quaternionf rotation) {
            this.worldX = worldX;
            this.worldY = worldY;
            this.worldZ = worldZ;
            this.rotation = rotation;
        }
    }

    private static final class TrackLibrary {
        private final Map<String, BoneTrack> tracks;

        private TrackLibrary(Map<String, BoneTrack> tracks) {
            this.tracks = tracks;
        }

        private BoneTrack bone(String boneName) {
            BoneTrack track = this.tracks.get(boneName);
            if (track == null) {
                throw new IllegalStateException("姿态缺少骨骼 " + boneName);
            }
            return track;
        }

        private String parent(String boneName) {
            return switch (boneName) {
                case "Head", "ArmRight", "ArmLeft" -> "Body";
                case "ForearmRight" -> "ArmRight";
                case "SwordHand" -> "ForearmRight";
                case "ForearmLeft" -> "ArmLeft";
                default -> null;
            };
        }

        private float[] pivot(String boneName) {
            return switch (boneName) {
                case "Body" -> new float[] {0.0F, 12.0F, 0.0F};
                case "Head" -> new float[] {0.0F, 12.0F, 0.0F};
                case "ArmRight" -> new float[] {-6.0F, 12.0F, 0.0F};
                case "ForearmRight", "SwordHand", "ForearmLeft" -> new float[] {0.0F, -6.0F, 0.0F};
                case "ArmLeft" -> new float[] {6.0F, 12.0F, 0.0F};
                case "LegRight" -> new float[] {-2.0F, 12.0F, 0.0F};
                case "LegLeft" -> new float[] {2.0F, 12.0F, 0.0F};
                default -> throw new IllegalStateException("姿态缺少枢轴 " + boneName);
            };
        }
    }

    private static final class BoneTrack {
        private final float[] times;
        private final float[][] rotation;
        private final float[][] position;

        private BoneTrack(float[] times, float[][] rotation, float[][] position) {
            this.times = times;
            this.rotation = rotation;
            this.position = position;
        }

        private static BoneTrack read(JsonObject bone) {
            JsonArray timeArray = bone.getAsJsonArray("times");
            JsonArray rotationArray = bone.getAsJsonArray("rotation");
            int count = timeArray.size();
            float[] times = new float[count];
            float[][] rotation = new float[count][3];
            for (int frameSlot = 0; frameSlot < count; frameSlot++) {
                times[frameSlot] = timeArray.get(frameSlot).getAsFloat();
                JsonArray euler = rotationArray.get(frameSlot).getAsJsonArray();
                rotation[frameSlot][0] = euler.get(0).getAsFloat();
                rotation[frameSlot][1] = euler.get(1).getAsFloat();
                rotation[frameSlot][2] = euler.get(2).getAsFloat();
            }
            float[][] position = null;
            if (bone.has("position")) {
                JsonArray positionArray = bone.getAsJsonArray("position");
                position = new float[count][3];
                for (int frameSlot = 0; frameSlot < count; frameSlot++) {
                    JsonArray shift = positionArray.get(frameSlot).getAsJsonArray();
                    position[frameSlot][0] = shift.get(0).getAsFloat();
                    position[frameSlot][1] = shift.get(1).getAsFloat();
                    position[frameSlot][2] = shift.get(2).getAsFloat();
                }
            }
            return new BoneTrack(times, rotation, position);
        }

        private float[] sampleRotation(float time) {
            return sample(this.rotation, time);
        }

        private float[] samplePosition(float time) {
            if (this.position == null) {
                return new float[] {0.0F, 0.0F, 0.0F};
            }
            return sample(this.position, time);
        }

        private float[] sample(float[][] samples, float time) {
            if (time <= this.times[0]) {
                return copy(samples[0]);
            }
            int last = this.times.length - 1;
            if (time >= this.times[last]) {
                return copy(samples[last]);
            }
            int frameSlot = 0;
            while (frameSlot < last && time > this.times[frameSlot + 1]) {
                frameSlot++;
            }
            float span = this.times[frameSlot + 1] - this.times[frameSlot];
            float weight = span > 0.0F ? (time - this.times[frameSlot]) / span : 0.0F;
            float[] from = samples[frameSlot];
            float[] toward = samples[frameSlot + 1];
            return new float[] {
                    from[0] + (toward[0] - from[0]) * weight,
                    from[1] + (toward[1] - from[1]) * weight,
                    from[2] + (toward[2] - from[2]) * weight
            };
        }

        private static float[] copy(float[] source) {
            return new float[] {source[0], source[1], source[2]};
        }
    }
}
