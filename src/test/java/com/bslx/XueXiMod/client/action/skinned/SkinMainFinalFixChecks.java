package com.bslx.XueXiMod.client.action.skinned;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.bslx.XueXiMod.animation.core.Joint;
import com.bslx.XueXiMod.animation.core.Pose;
import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.math.Vec3f;
import com.bslx.XueXiMod.animation.core.model.Armature;
import com.bslx.XueXiMod.animation.runtime.ActionRegistry;
import com.bslx.XueXiMod.animation.runtime.ActionSampleContext;
import com.bslx.XueXiMod.animation.runtime.OriginalHumanoid;
import com.bslx.XueXiMod.animation.runtime.loader.DevActionLoader;
import com.bslx.XueXiMod.animation.runtime.loader.DevLoadResult;
import com.bslx.XueXiMod.client.action.ActionVisualCatalog;
import com.bslx.XueXiMod.client.action.FirstPersonHandBasis;
import com.bslx.XueXiMod.client.action.RenderSpaceCapture;
import com.bslx.XueXiMod.client.action.SlashPlaneAlign;
import com.bslx.XueXiMod.client.action.mesh.PlayerMeshKind;
import com.bslx.XueXiMod.client.action.mesh.PlayerMeshPart;
import com.bslx.XueXiMod.client.action.mesh.PlayerSkinMesh;
import com.bslx.XueXiMod.client.action.mesh.SkinPalette;
import com.bslx.XueXiMod.client.action.skinned.FirstPersonSkinDraw.PlaneDecision;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.resources.ResourceLocation;

/**
 * 身体矩阵必须用绘制当时的 worldToModel。缺配方的挥斩仍蒙皮。
 * 手架只接受 mesh113 已核实的源版本，不改那组地标。
 */
public final class SkinMainFinalFixChecks {
    private static final float TOLERANCE = 1.0E-4F;
    private static final float SEPARATION = 0.05F;
    private static int checks;

    private SkinMainFinalFixChecks() {
    }

    public static void main(String[] programArguments) {
        try {
            checkBodyUsesLiveCapture();
            checkPlainSwordStaysSkinned();
            checkWaterBladesAlign();
            checkCalibrationGate();
            checkTimingText();
            System.out.println("SkinMainFinalFixChecks assertions=" + checks);
        } finally {
            HandFrameCalibration.invalidate();
        }
    }

    private static void checkBodyUsesLiveCapture() {
        PoseStack poseStack = new PoseStack();
        RenderSpaceCapture.push(poseStack);
        try {
            OpenMatrix4f entryMatrix = RenderSpaceCapture.worldToModel();
            poseStack.translate(0.4F, -1.2F, 0.3F);
            poseStack.mulPose(Axis.YP.rotationDegrees(40.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(25.0F));
            OpenMatrix4f bodyMatrix = RenderSpaceCapture.worldToModel();
            require(entryMatrix != null && bodyMatrix != null, "捕获矩阵");
            differ(entryMatrix, bodyMatrix, "入口和身体的 worldToModel 应不同");

            Vec3f referenceForward = new Vec3f(0.0F, 0.0F, -1.0F);
            Vec3f referenceNormal = new Vec3f(0.0F, -1.0F, 0.0F);
            ActionSampleContext sample = slashSample(
                    ResourceLocation.fromNamespaceAndPath("xueximod", "probe/slash"),
                    0.0F, 0.0F, -1.0F,
                    0.0F, -1.0F, 0.0F);
            float pivotAcross = 0.0F;
            float pivotUpward = 0.75F;
            float pivotAhead = 0.0F;
            SkinSession session = SkinSession.readyForBodyCheck(
                    sample, referenceForward, referenceNormal, 0.0F, probeMesh(PlayerMeshKind.NORMAL, PlayerMeshKind.NORMAL.sourceGitPath(), HandFrameCalibration.PINNED_NORMAL_SHA256),
                    probePalette(), clock(), skinnedPlan());
            session.installAt(pivotAcross, pivotUpward, pivotAhead);
            require(session.frame() != null, "身体帧");
            OpenMatrix4f installed = session.frame().copyAlignment();
            OpenMatrix4f bodySpin = SlashPlaneAlign.modelAlignment(
                    sample.directionX(), sample.directionY(), sample.directionZ(),
                    sample.planeX(), sample.planeY(), sample.planeZ(),
                    0.0F, bodyMatrix, referenceForward, referenceNormal);
            OpenMatrix4f entrySpin = SlashPlaneAlign.modelAlignment(
                    sample.directionX(), sample.directionY(), sample.directionZ(),
                    sample.planeX(), sample.planeY(), sample.planeZ(),
                    0.0F, entryMatrix, referenceForward, referenceNormal);
            require(bodySpin != null && entrySpin != null, "对照自旋");
            OpenMatrix4f expected = SkinnedBodyDraw.bodyAlignment(bodySpin, pivotAcross, pivotUpward, pivotAhead);
            OpenMatrix4f stale = SkinnedBodyDraw.bodyAlignment(entrySpin, pivotAcross, pivotUpward, pivotAhead);
            near(installed, expected, "最终对齐等于身体当时的 world F/N");
            differ(installed, stale, "最终对齐不能复用入口矩阵");

            Vec3f worldForward = new Vec3f(sample.directionX(), sample.directionY(), sample.directionZ());
            Vec3f bodyImage = OpenMatrix4f.transform3v(bodyMatrix, worldForward, null);
            Vec3f entryImage = OpenMatrix4f.transform3v(entryMatrix, worldForward, null);
            differVector(bodyImage, entryImage, "世界前向经 yaw/swim 后不同");
            Vec3f landed = pivotDelta(installed, referenceForward, pivotAcross, pivotUpward, pivotAhead);
            Vec3f bodyLanded = OpenMatrix4f.transform3v(bodySpin, referenceForward, null);
            Vec3f entryLanded = OpenMatrix4f.transform3v(entrySpin, referenceForward, null);
            nearVector(landed, bodyLanded, "帧上的前向等于身体当时的模型前向");
            differVector(landed, entryLanded, "帧上的前向不是入口当时的模型前向");

            SkinSession plain = SkinSession.readyForBodyCheck(
                    sample, null, null, 0.0F, probeMesh(PlayerMeshKind.NORMAL, PlayerMeshKind.NORMAL.sourceGitPath(), HandFrameCalibration.PINNED_NORMAL_SHA256),
                    probePalette(), clock(), skinnedPlan());
            plain.installAt(pivotAcross, pivotUpward, pivotAhead);
            require(plain.frame() != null, "缺参考仍安装蒙皮帧");
            near(plain.frame().copyAlignment(), new OpenMatrix4f(), "缺参考时对齐保持单位阵");
            require(plain.alignmentNote().contains("没有平面校准配方"), plain.alignmentNote());
            differ(plain.frame().copyAlignment(), installed, "缺参考不吃 yaw/swim");
        } finally {
            RenderSpaceCapture.pop();
        }
    }

    private static void checkPlainSwordStaysSkinned() {
        ResourceLocation plainSwordKey = ResourceLocation.fromNamespaceAndPath("epicfight", "biped/combat/sword_dash");
        require(ActionVisualCatalog.blade(plainSwordKey), "授权剑仍登记");
        require(!ActionVisualCatalog.hasRecipe(plainSwordKey), "该剑没有三点配方");
        ActionSampleContext sample = slashSample(plainSwordKey, 0.0F, 0.0F, -1.0F, 0.0F, -1.0F, 0.0F);
        require(sample.hasSlashFrame(), "挥斩帧还在");
        OpenMatrix4f handFrame = HandFrameCalibration.handFrame(PlayerMeshKind.NORMAL, true, 0.0F);
        require(handFrame != null, "普通手架");
        OpenMatrix4f suppliedSpin = new OpenMatrix4f();
        suppliedSpin.m00 = 0.0F;
        suppliedSpin.m20 = 1.0F;
        suppliedSpin.m02 = -1.0F;
        suppliedSpin.m22 = 0.0F;
        PlaneDecision decision = FirstPersonSkinDraw.planeDecision(sample, handFrame, PlayerMeshKind.NORMAL, true, suppliedSpin);
        require(decision.alignment != null, "缺配方仍给出蒙皮对齐");
        near(decision.alignment, new OpenMatrix4f(), "缺配方对齐是单位阵");
        require(decision.unalignedReason.contains("没有平面校准配方"), decision.unalignedReason);
    }

    private static void checkWaterBladesAlign() {
        OriginalHumanoid.register();
        DevLoadResult loaded = DevActionLoader.load(Path.of("D:/alianximod/dev-authorized/epicfight"));
        require(loaded.sourceArmature() != null, "水刃剪辑未加载：" + loaded.summaryText());
        ActionRegistry.publishDev(loaded);
        ActionVisualCatalog.invalidate();
        ResourceLocation[] waterBladeKeys = {
                OriginalHumanoid.LEFT_ID,
                OriginalHumanoid.RIGHT_ID,
                OriginalHumanoid.HORIZONTAL_ID,
                ResourceLocation.fromNamespaceAndPath("epicfight", "biped/combat/sword_auto1"),
                ResourceLocation.fromNamespaceAndPath("epicfight", "biped/combat/sword_auto2"),
                ResourceLocation.fromNamespaceAndPath("epicfight", "biped/combat/sword_auto3")
        };
        OpenMatrix4f handFrame = HandFrameCalibration.handFrame(PlayerMeshKind.NORMAL, true, 0.0F);
        require(handFrame != null, "水刃手架");
        Vec3f shoulder = OpenMatrix4f.transform3v(handFrame, HandFrameCalibration.sourceShoulder(PlayerMeshKind.NORMAL, true), null);
        for (int bladeIndex = 0; bladeIndex < waterBladeKeys.length; bladeIndex++) {
            ResourceLocation bladeKey = waterBladeKeys[bladeIndex];
            require(ActionVisualCatalog.hasRecipe(bladeKey), "水刃配方 " + bladeKey);
            require(ActionVisualCatalog.fractions(bladeKey).length == 3, "三点 " + bladeKey);
            Vec3f referenceForward = ActionVisualCatalog.referenceForward(bladeKey);
            Vec3f referenceNormal = ActionVisualCatalog.referenceNormal(bladeKey);
            require(referenceForward != null && referenceNormal != null, "已解析参考 " + bladeKey);
            ActionSampleContext sample = slashSample(bladeKey, 0.0F, 0.0F, -1.0F, 0.0F, -1.0F, 0.0F);
            OpenMatrix4f cameraSpin = FirstPersonHandBasis.cameraAlignment(
                    sample.directionX(), sample.directionY(), sample.directionZ(),
                    sample.planeX(), sample.planeY(), sample.planeZ(),
                    1.0F, 0.0F, 0.0F,
                    0.0F, 1.0F, 0.0F,
                    0.0F, 0.0F, 1.0F,
                    referenceForward,
                    referenceNormal);
            require(cameraSpin != null, "相机自旋 " + bladeKey);
            PlaneDecision decision = FirstPersonSkinDraw.planeDecision(
                    sample, handFrame, PlayerMeshKind.NORMAL, true, cameraSpin);
            require(decision.alignment != null, "水刃对齐 " + bladeKey);
            require(decision.unalignedReason.isEmpty(), "水刃不应记未对齐 " + bladeKey);
            OpenMatrix4f expected = HandFrameCalibration.around(
                    cameraSpin, shoulder.coordinateX, shoulder.coordinateY, shoulder.coordinateZ);
            near(decision.alignment, expected, "水刃绕肩 " + bladeKey);
            differ(decision.alignment, new OpenMatrix4f(), "水刃不是缺配方的单位阵 " + bladeKey);
        }
    }

    private static void checkCalibrationGate() {
        require("021f5a9b2c307f9215510efb507d9f3d8694ba21fa1db8bbee2e6551a0d06d83"
                .equals(HandFrameCalibration.PINNED_NORMAL_SHA256), "normal 哈希保持 mesh113");
        require("8a6aaeb7ff726189559db5df9f592eebd8c55530f89be6ae5745ff12d41d0626"
                .equals(HandFrameCalibration.PINNED_SLIM_SHA256), "slim 哈希保持 mesh113");
        OpenMatrix4f before = HandFrameCalibration.modelFrame(PlayerMeshKind.NORMAL, true);
        require(before != null, "已验证版本仍能配准");
        PlayerSkinMesh normalMesh = probeMesh(
                PlayerMeshKind.NORMAL, PlayerMeshKind.NORMAL.sourceGitPath(), HandFrameCalibration.PINNED_NORMAL_SHA256);
        PlayerSkinMesh slimMesh = probeMesh(
                PlayerMeshKind.SLIM, PlayerMeshKind.SLIM.sourceGitPath(), HandFrameCalibration.PINNED_SLIM_SHA256);
        require(HandFrameCalibration.supportDiagnostic(normalMesh, PlayerMeshKind.NORMAL) == null, "normal 在支持范围内");
        require(HandFrameCalibration.supportDiagnostic(slimMesh, PlayerMeshKind.SLIM) == null, "slim 在支持范围内");
        PlayerSkinMesh foreignMesh = probeMesh(
                PlayerMeshKind.NORMAL, PlayerMeshKind.NORMAL.sourceGitPath(), "a".repeat(64));
        String foreignReason = HandFrameCalibration.supportDiagnostic(foreignMesh, PlayerMeshKind.NORMAL);
        require(foreignReason != null && foreignReason.contains("不在支持范围内"), foreignReason);
        require(!foreignReason.contains("地标") && !foreignReason.contains("验证错误"), "不把已核实网格说成错误");
        PlayerSkinMesh otherPath = probeMesh(
                PlayerMeshKind.NORMAL, "other/biped.json", HandFrameCalibration.PINNED_NORMAL_SHA256);
        require(HandFrameCalibration.supportDiagnostic(otherPath, PlayerMeshKind.NORMAL) != null, "路径不符");
        require(HandFrameCalibration.supportDiagnostic(slimMesh, PlayerMeshKind.NORMAL) != null, "种类不符");
        OpenMatrix4f after = HandFrameCalibration.modelFrame(PlayerMeshKind.NORMAL, true);
        near(before, after, "门禁不改已过的几何");
    }

    private static void checkTimingText() {
        String sessionText;
        String handText;
        String calibrationText;
        try {
            sessionText = Files.readString(Path.of(
                "D:/alianximod/src/main/java/com/bslx/XueXiMod/client/action/skinned/SkinSession.java"));
            handText = Files.readString(Path.of(
                "D:/alianximod/src/main/java/com/bslx/XueXiMod/client/action/skinned/FirstPersonSkinDraw.java"));
            calibrationText = Files.readString(Path.of(
                "D:/alianximod/src/main/java/com/bslx/XueXiMod/client/action/skinned/HandFrameCalibration.java"));
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
        int chooseStart = sessionText.indexOf("private void chooseThird");
        int freezeStart = sessionText.indexOf("private void freezePlane");
        int resolveStart = sessionText.indexOf("private OpenMatrix4f resolveSpin");
        require(chooseStart >= 0 && freezeStart > chooseStart && resolveStart > freezeStart, "时序方法");
        String chooseBody = sessionText.substring(chooseStart, freezeStart);
        require(!chooseBody.contains("worldToModel") && !chooseBody.contains("modelAlignment") && !chooseBody.contains("modelSpin"),
                "计划阶段不读矩阵");
        require(chooseBody.contains("freezePlane"), "计划阶段只冻结参考");
        require(sessionText.substring(resolveStart).contains("RenderSpaceCapture.worldToModel()"), "绘制点读捕获");
        require(sessionText.contains("installAt"), "身体安装入口");
        require(handText.contains("hasRecipe") && handText.contains("没有平面校准配方"), "缺配方记录");
        require(handText.contains("supportDiagnostic"), "手架门禁");
        require(!handText.contains("new Vec3f(0.0F, 0.0F, -1.0F)"), "不伪造默认参考架");
        require(calibrationText.contains("1.500045") && calibrationText.contains("0.750045") && calibrationText.contains("0.375"),
                "地标数字还在");
    }

    private static ActionSampleContext slashSample(
            ResourceLocation bladeKey,
            float forwardAcross,
            float forwardUpward,
            float forwardAhead,
            float normalAcross,
            float normalUpward,
            float normalAhead) {
        UUID ownerKey = new UUID(4L, 8L);
        return ActionSampleContext.capture(
                true, ownerKey, 7, 3L, 1L, ownerKey, bladeKey, bladeKey, "probe-rig", true,
                forwardAcross, forwardUpward, forwardAhead, normalAcross, normalUpward, normalAhead,
                0.25F, new Pose(), 10L, 0.4F, 0.1F, 1, 2, false, 0.0F);
    }

    private static ImmutableSkinFrame.Clock clock() {
        return ImmutableSkinFrame.Clock.of("body-check", 10L, 0.1F, 0.25F, 2);
    }

    private static RenderPlan skinnedPlan() {
        return RenderPlan.of(
                RenderPlan.Mode.SKINNED,
                new UUID(4L, 8L),
                11,
                RenderPlan.Perspective.THIRD_PERSON,
                RenderPlan.Hand.NONE,
                PlayerMeshKind.NORMAL,
                1L,
                "body-check");
    }

    private static Vec3f pivotDelta(OpenMatrix4f alignment, Vec3f referenceForward, float pivotAcross, float pivotUpward, float pivotAhead) {
        Vec3f offset = new Vec3f(
                referenceForward.coordinateX + pivotAcross,
                referenceForward.coordinateY + pivotUpward,
                referenceForward.coordinateZ + pivotAhead);
        Vec3f image = OpenMatrix4f.transform3v(alignment, offset, null);
        return new Vec3f(image.coordinateX - pivotAcross, image.coordinateY - pivotUpward, image.coordinateZ - pivotAhead);
    }

    private static PlayerSkinMesh probeMesh(PlayerMeshKind kind, String sourcePath, String sourceHash) {
        int[] headCorners = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[][] partCorners = new int[PlayerMeshPart.values().length][];
        partCorners[PlayerMeshPart.HEAD.ordinal()] = headCorners;
        return PlayerSkinMesh.compose(
                kind, 1,
                new float[] {0.0F, 0.0F, 0.0F},
                new float[] {0.0F, 1.0F, 0.0F},
                new float[] {0.0F, 0.0F},
                new float[] {1.0F},
                new int[] {1},
                new int[] {0},
                new int[] {0},
                partCorners,
                false,
                sourcePath,
                sourceHash,
                true);
    }

    private static SkinPalette probePalette() {
        Joint root = new Joint("Root", 0, new OpenMatrix4f());
        Map<String, Joint> joints = new LinkedHashMap<>();
        joints.put("Root", root);
        Armature armature = new Armature("probe-palette", 1, root, joints);
        return SkinPalette.fromAlreadySkinned(armature, new OpenMatrix4f[] {new OpenMatrix4f()}, new OpenMatrix4f[] {new OpenMatrix4f()});
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }

    private static void near(OpenMatrix4f left, OpenMatrix4f right, String label) {
        checks++;
        float[] leftElements = elements(left);
        float[] rightElements = elements(right);
        for (int elementIndex = 0; elementIndex < leftElements.length; elementIndex++) {
            if (Math.abs(leftElements[elementIndex] - rightElements[elementIndex]) > TOLERANCE) {
                throw new AssertionError(label);
            }
        }
    }

    private static void differ(OpenMatrix4f left, OpenMatrix4f right, String label) {
        checks++;
        float[] leftElements = elements(left);
        float[] rightElements = elements(right);
        for (int elementIndex = 0; elementIndex < leftElements.length; elementIndex++) {
            if (Math.abs(leftElements[elementIndex] - rightElements[elementIndex]) > SEPARATION) {
                return;
            }
        }
        throw new AssertionError(label);
    }

    private static void nearVector(Vec3f left, Vec3f right, String label) {
        checks++;
        if (Math.abs(left.coordinateX - right.coordinateX) > 2.0E-3F
                || Math.abs(left.coordinateY - right.coordinateY) > 2.0E-3F
                || Math.abs(left.coordinateZ - right.coordinateZ) > 2.0E-3F) {
            throw new AssertionError(label);
        }
    }

    private static void differVector(Vec3f left, Vec3f right, String label) {
        checks++;
        float gap = Math.abs(left.coordinateX - right.coordinateX)
                + Math.abs(left.coordinateY - right.coordinateY)
                + Math.abs(left.coordinateZ - right.coordinateZ);
        if (gap <= SEPARATION) {
            throw new AssertionError(label);
        }
    }

    private static float[] elements(OpenMatrix4f matrix) {
        return new float[] {
                matrix.m00, matrix.m01, matrix.m02, matrix.m03,
                matrix.m10, matrix.m11, matrix.m12, matrix.m13,
                matrix.m20, matrix.m21, matrix.m22, matrix.m23,
                matrix.m30, matrix.m31, matrix.m32, matrix.m33
        };
    }
}
