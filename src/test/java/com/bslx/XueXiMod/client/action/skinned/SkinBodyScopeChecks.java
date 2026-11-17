package com.bslx.XueXiMod.client.action.skinned;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.bslx.XueXiMod.animation.core.Joint;
import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.math.Vec3f;
import com.bslx.XueXiMod.animation.core.model.Armature;
import com.bslx.XueXiMod.animation.runtime.ActionSampleContext;
import com.bslx.XueXiMod.client.action.RenderPoseScope;
import com.bslx.XueXiMod.client.action.SkinBinding;
import com.bslx.XueXiMod.client.action.mesh.MeshLimits;
import com.bslx.XueXiMod.client.action.mesh.MeshLoadResult;
import com.bslx.XueXiMod.client.action.mesh.PartVisibility;
import com.bslx.XueXiMod.client.action.mesh.PlayerMeshKind;
import com.bslx.XueXiMod.client.action.mesh.PlayerMeshPart;
import com.bslx.XueXiMod.client.action.mesh.PlayerSkinMesh;
import com.bslx.XueXiMod.client.action.mesh.SkinLease;
import com.bslx.XueXiMod.client.action.mesh.SkinPalette;
import com.bslx.XueXiMod.client.action.mesh.SkinWorkspacePool;

/** 主路接线。不改已经通过的刚性检查，也不读授权网格。 */
public final class SkinBodyScopeChecks {
    private static final UUID OWNER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final float TOLERANCE = 2.0E-3F;
    private static int checks;

    private SkinBodyScopeChecks() {
    }

    public static void main(String[] programArguments) {
        try {
            checkLandmarksAndHandFrame();
            checkEquipChainOrder();
            checkItemAnchor();
            checkAlignmentOrbitsOnce();
            checkBodyPlacementLeavesVanillaDrop();
            checkFirstPersonFrameRejectsBodyMap();
            checkPartSwitches();
            checkScopeFailureAndNesting();
            checkAssetGenerationAndRig();
            checkWiringText();
            System.out.println("SkinBodyScopeChecks assertions=" + checks);
        } finally {
            SkinAssets.restoreInitialForCheck();
            HandFrameCalibration.invalidate();
        }
    }

    private static void checkLandmarksAndHandFrame() {
        for (PlayerMeshKind kind : PlayerMeshKind.values()) {
            for (int side = 0; side < 2; side++) {
                boolean rightSide = side == 0;
                float shoulderGap = distance(HandFrameCalibration.sourceShoulder(kind, rightSide), HandFrameCalibration.sourceWrist(kind, rightSide));
                float vanillaGap = distance(HandFrameCalibration.vanillaShoulder(kind, rightSide), HandFrameCalibration.vanillaWrist(kind, rightSide));
                near(shoulderGap, 0.75F, kind.wireName() + " source length");
                near(vanillaGap, 0.75F, kind.wireName() + " vanilla length");
                OpenMatrix4f fitted = HandFrameCalibration.modelFrame(kind, rightSide);
                if (fitted == null) {
                    throw new AssertionError(kind.wireName() + " hand did not fit");
                }
                nearPoint(transform(fitted, HandFrameCalibration.sourceShoulder(kind, rightSide)), HandFrameCalibration.vanillaShoulder(kind, rightSide), kind.wireName() + " shoulder");
                nearPoint(transform(fitted, HandFrameCalibration.sourceWrist(kind, rightSide)), HandFrameCalibration.vanillaWrist(kind, rightSide), kind.wireName() + " wrist");
                OpenMatrix4f handFrame = HandFrameCalibration.handFrame(kind, rightSide, 0.0F);
                float separated = distance(transform(handFrame, HandFrameCalibration.sourceShoulder(kind, rightSide)), transform(AttachmentTransform.sourceToBody(), HandFrameCalibration.sourceShoulder(kind, rightSide)));
                if (!(separated > 0.2F)) {
                    throw new AssertionError("hand frame collapsed into the body map");
                }
                checks++;
            }
        }
    }

    private static void checkEquipChainOrder() {
        Vec3f resting = transform(HandFrameCalibration.armChain(1.0F, 0.0F), new Vec3f(0.0F, 0.0F, 0.0F));
        Vec3f equipped = transform(HandFrameCalibration.armChain(1.0F, 1.0F), new Vec3f(0.0F, 0.0F, 0.0F));
        near(equipped.coordinateX - resting.coordinateX, 0.0F, "equip keeps across");
        near(equipped.coordinateY - resting.coordinateY, -0.6F, "equip is the outer translation");
        near(equipped.coordinateZ - resting.coordinateZ, 0.0F, "equip keeps ahead");
    }

    private static void checkItemAnchor() {
        OpenMatrix4f rightRest = HandFrameCalibration.itemAnchor(1.0F, 0.0F);
        near(rightRest.m30, 0.56F, "right item across");
        near(rightRest.m31, -0.52F, "right item upward");
        near(rightRest.m32, -0.72F, "right item ahead");
        near(rightRest.m00, 1.0F, "item anchor has no display rotation");
        OpenMatrix4f leftEquipped = HandFrameCalibration.itemAnchor(-1.0F, 1.0F);
        near(leftEquipped.m30, -0.56F, "left item across");
        near(leftEquipped.m31, -1.12F, "left item equip");
        near(leftEquipped.m32, -0.72F, "left item ahead");
    }

    private static void checkAlignmentOrbitsOnce() {
        OpenMatrix4f spin = OpenMatrix4f.createRotatorDeg(90.0F, new Vec3f(0.0F, 1.0F, 0.0F));
        OpenMatrix4f alignment = SkinnedBodyDraw.bodyAlignment(spin, 0.0F, 0.2F, 0.0F);
        Vec3f center = new Vec3f(0.0F, 0.2F, 0.0F);
        nearPoint(transform(alignment, center), center, "orbit center");
        Vec3f offset = new Vec3f(1.0F, 0.2F, 0.0F);
        Vec3f once = transform(alignment, offset);
        Vec3f twice = transform(OpenMatrix4f.mul(alignment, alignment, null), offset);
        if (!(distance(once, offset) > 0.2F) || !(distance(once, twice) > 0.2F)) {
            throw new AssertionError("alignment was not a single orbit");
        }
        near(distance(once, center), 1.0F, "orbit radius");
        checks++;
    }

    private static void checkBodyPlacementLeavesVanillaDrop() {
        OpenMatrix4f alignment = SkinnedBodyDraw.bodyAlignment(new OpenMatrix4f(), 0.0F, 0.0F, 0.0F);
        OpenMatrix4f placed = AttachmentTransform.composePlacement(alignment, AttachmentTransform.sourceToBody());
        Vec3f origin = transform(placed, new Vec3f(0.0F, 0.0F, 0.0F));
        near(origin.coordinateX, 0.0F, "placed origin across");
        near(origin.coordinateY, 1.5F, "placed origin lift");
        near(origin.coordinateZ, 0.0F, "placed origin ahead");
        if (Math.abs(placed.m00 + 0.9375F) < 1.0E-4F || Math.abs(placed.m31 - (1.5F - 1.501F)) < 1.0E-4F) {
            throw new AssertionError("body placement absorbed vanilla scale or foot drop");
        }
        checks++;
    }

    private static void checkFirstPersonFrameRejectsBodyMap() {
        PlayerSkinMesh mesh = syntheticMesh();
        SkinPalette palette = palette();
        ImmutableSkinFrame.Clock clock = ImmutableSkinFrame.Clock.of("probe-action", 4L, 0.0F, 0.0F, 0);
        RenderPlan plan = RenderPlan.of(
                RenderPlan.Mode.SKINNED, OWNER, 3, RenderPlan.Perspective.FIRST_PERSON, RenderPlan.Hand.RIGHT,
                PlayerMeshKind.NORMAL, 1L, "first-person-probe");
        OpenMatrix4f handFrame = HandFrameCalibration.handFrame(PlayerMeshKind.NORMAL, true, 0.25F);
        ImmutableSkinFrame frame = ImmutableSkinFrame.capture(plan, mesh, palette, new OpenMatrix4f(), handFrame, clock);
        if (sameMatrix(frame.copySourceToView(), AttachmentTransform.sourceToBody())) {
            throw new AssertionError("first person view stored the body map");
        }
        nearMatrix(frame.copyMeshPlacement(), AttachmentTransform.composePlacement(new OpenMatrix4f(), handFrame), "placement is one alignment times the hand frame");
        checks++;
    }

    private static void checkPartSwitches() {
        PartVisibility hidden = SkinnedBodyDraw.parts(false, false, false, false, false, false);
        if (hidden.shows(PlayerMeshPart.HAT) || hidden.shows(PlayerMeshPart.JACKET)
                || hidden.shows(PlayerMeshPart.LEFT_SLEEVE) || hidden.shows(PlayerMeshPart.RIGHT_SLEEVE)
                || hidden.shows(PlayerMeshPart.LEFT_PANTS) || hidden.shows(PlayerMeshPart.RIGHT_PANTS)) {
            throw new AssertionError("outer switches stayed on");
        }
        if (!hidden.shows(PlayerMeshPart.HEAD) || !hidden.shows(PlayerMeshPart.TORSO)
                || !hidden.shows(PlayerMeshPart.RIGHT_ARM) || !hidden.shows(PlayerMeshPart.LEFT_LEG)) {
            throw new AssertionError("body partitions were hidden");
        }
        checks++;
    }

    private static void checkScopeFailureAndNesting() {
        Object model = new Object();
        Object player = new Object();
        SkinLease lease = SkinWorkspacePool.currentThread().open(syntheticMesh());
        int[] closes = new int[1];
        SkinBinding binding = new SkinBinding() {
            private boolean released;

            @Override
            public ActionSampleContext sample() {
                return ActionSampleContext.inactive(OWNER);
            }

            @Override
            public boolean skipRigidWriter() {
                return true;
            }

            @Override
            public void close() {
                if (this.released) {
                    return;
                }
                this.released = true;
                closes[0] = closes[0] + 1;
                lease.close();
            }
        };
        try {
            RenderPoseScope.runThirdPerson(model, player, () -> {
                RenderPoseScope.attachSkin(binding);
                if (!OWNER.equals(RenderPoseScope.frozenSample(OWNER).ownerId())) {
                    throw new AssertionError("frozen sample missing");
                }
                if (!RenderPoseScope.skipsRigidWriter(model, player)) {
                    throw new AssertionError("skinned scope still writes rigid pose");
                }
                RenderPoseScope.runFirstPerson(model, player, () -> {
                    if (RenderPoseScope.skipsRigidWriter(model, player) || RenderPoseScope.allowsThirdPerson(model, player)) {
                        throw new AssertionError("first person inherited third person mode");
                    }
                    if (RenderPoseScope.frozenSample(OWNER) != null) {
                        throw new AssertionError("inner scope reused the outer sample");
                    }
                });
                if (!RenderPoseScope.skipsRigidWriter(model, player)) {
                    throw new AssertionError("third person mode did not resume");
                }
                throw new IllegalStateException("render failed");
            });
            throw new AssertionError("scope swallowed the failure");
        } catch (IllegalStateException failure) {
            if (!"render failed".equals(failure.getMessage())) {
                throw failure;
            }
        }
        if (lease.isOpen() || closes[0] != 1 || RenderPoseScope.depth() != 0 || RenderPoseScope.frozenSample(OWNER) != null) {
            throw new AssertionError("scope failure left a lease or a sample");
        }
        if (SkinRenderAccess.currentThirdPerson(null) != null || SkinRenderAccess.attachmentSupport(null) != null) {
            throw new AssertionError("empty scope returned a frame");
        }
        checks++;
    }

    private static void checkAssetGenerationAndRig() {
        Armature matching = chain(0.0F);
        Armature shifted = chain(0.25F);
        SkinAssets published = SkinAssets.replaceForCheck(SkinAssets.readyForCheck(MeshLoadResult.notLoaded(), matching, 4L));
        if (published.resourceGeneration() != 4L || published.status() != SkinAssets.Status.READY) {
            throw new AssertionError("published generation was not kept");
        }
        if (!published.matches(matching) || published.matches(shifted) || published.usable(PlayerMeshKind.NORMAL)) {
            throw new AssertionError("rig match did not follow bind and mesh presence");
        }
        SkinAssets.recheckRegistry();
        SkinAssets afterRegistry = SkinAssets.current();
        if (afterRegistry.status() != SkinAssets.Status.RIG_MISMATCH || afterRegistry.resourceGeneration() != 5L || afterRegistry.usable(PlayerMeshKind.NORMAL)) {
            throw new AssertionError("registry change did not retire the skin snapshot");
        }
        checks++;
    }

    private static void checkWiringText() {
        String session = sourceText("src/main/java/com/bslx/XueXiMod/client/action/skinned/SkinSession.java");
        String hands = sourceText("src/main/java/com/bslx/XueXiMod/client/action/skinned/FirstPersonSkinDraw.java");
        String body = sourceText("src/main/java/com/bslx/XueXiMod/mixin/client/LivingEntityRendererBodyMixin.java");
        String dispatcher = sourceText("src/main/java/com/bslx/XueXiMod/mixin/client/EntityRenderDispatcherActionMixin.java");
        String held = sourceText("src/main/java/com/bslx/XueXiMod/mixin/client/ItemInHandRendererActionMixin.java");
        String listed = sourceText("src/main/resources/xueximod.mixins.json");
        requireText(session, "SkinAttachmentBridge.preflight");
        requireText(session, "SkinPalette.prepare");
        requireText(hands, "prepareFirstPersonItem");
        requireText(hands, "renderFirstPersonItem");
        requireText(hands, "RenderType.entitySolid");
        requireText(hands, "RenderType.entityTranslucent");
        requireText(body, "EntityModel;renderToBuffer");
        requireText(body, "SkinnedBodyDraw.replace");
        requireText(dispatcher, "SkinSession.openThirdPerson");
        requireText(held, "SkinSession.openFirstPerson");
        requireText(held, "FirstPersonSkinDraw.draw");
        requireText(listed, "client.LivingEntityRendererBodyMixin");
        if (session.contains("publishDev") || hands.contains("advanceEvents")) {
            throw new AssertionError("skin path resets actions or advances events");
        }
        checks++;
    }

    private static Armature chain(float rootShift) {
        Map<String, Joint> joints = new LinkedHashMap<>();
        Joint root = null;
        Joint previous = null;
        for (int jointIndex = 0; jointIndex < MeshLimits.JOINT_NAMES.length; jointIndex++) {
            OpenMatrix4f bind = new OpenMatrix4f();
            if (jointIndex == 0) {
                bind.m30 = rootShift;
            }
            Joint joint = new Joint(MeshLimits.JOINT_NAMES[jointIndex], jointIndex, bind);
            joints.put(joint.getName(), joint);
            if (root == null) {
                root = joint;
            } else {
                previous.addSubJoints(joint);
            }
            previous = joint;
        }
        return new Armature("probe-rig", joints.size(), root, joints);
    }

    private static PlayerSkinMesh syntheticMesh() {
        int[] headCorners = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[][] partCorners = new int[PlayerMeshPart.values().length][];
        partCorners[PlayerMeshPart.HEAD.ordinal()] = headCorners;
        return PlayerSkinMesh.compose(
                PlayerMeshKind.NORMAL,
                1,
                new float[] {0.0F, 0.0F, 0.0F},
                new float[] {0.0F, 1.0F, 0.0F},
                new float[] {0.0F, 0.0F},
                new float[] {1.0F},
                new int[] {1},
                new int[] {0},
                new int[] {0},
                partCorners,
                false,
                "",
                "",
                true);
    }

    private static SkinPalette palette() {
        Joint root = new Joint("Root", 0, new OpenMatrix4f());
        Map<String, Joint> joints = new LinkedHashMap<>();
        joints.put("Root", root);
        Armature armature = new Armature("probe-palette", 1, root, joints);
        return SkinPalette.fromAlreadySkinned(armature, new OpenMatrix4f[] {new OpenMatrix4f()}, new OpenMatrix4f[] {new OpenMatrix4f()});
    }

    private static void requireText(String source, String token) {
        if (!source.contains(token)) {
            throw new AssertionError("缺少接线 " + token);
        }
    }

    private static String sourceText(String relativePath) {
        try {
            return Files.readString(Path.of(relativePath));
        } catch (IOException failure) {
            throw new AssertionError("无法读取 " + relativePath, failure);
        }
    }

    private static Vec3f transform(OpenMatrix4f matrix, Vec3f source) {
        return OpenMatrix4f.transform3v(matrix, source, null);
    }

    private static float distance(Vec3f left, Vec3f right) {
        float across = left.coordinateX - right.coordinateX;
        float upward = left.coordinateY - right.coordinateY;
        float ahead = left.coordinateZ - right.coordinateZ;
        return (float) Math.sqrt(across * across + upward * upward + ahead * ahead);
    }

    private static void near(float actual, float expected, String label) {
        if (!Float.isFinite(actual) || Math.abs(actual - expected) > TOLERANCE) {
            throw new AssertionError(label + " actual=" + actual + " expected=" + expected);
        }
        checks++;
    }

    private static void nearPoint(Vec3f actual, Vec3f expected, String label) {
        near(actual.coordinateX, expected.coordinateX, label + " x");
        near(actual.coordinateY, expected.coordinateY, label + " y");
        near(actual.coordinateZ, expected.coordinateZ, label + " z");
    }

    private static void nearMatrix(OpenMatrix4f actual, OpenMatrix4f expected, String label) {
        near(actual.m00, expected.m00, label + " m00");
        near(actual.m11, expected.m11, label + " m11");
        near(actual.m22, expected.m22, label + " m22");
        near(actual.m30, expected.m30, label + " m30");
        near(actual.m31, expected.m31, label + " m31");
        near(actual.m32, expected.m32, label + " m32");
    }

    private static boolean sameMatrix(OpenMatrix4f left, OpenMatrix4f right) {
        return left.m00 == right.m00 && left.m01 == right.m01 && left.m02 == right.m02 && left.m03 == right.m03
                && left.m10 == right.m10 && left.m11 == right.m11 && left.m12 == right.m12 && left.m13 == right.m13
                && left.m20 == right.m20 && left.m21 == right.m21 && left.m22 == right.m22 && left.m23 == right.m23
                && left.m30 == right.m30 && left.m31 == right.m31 && left.m32 == right.m32 && left.m33 == right.m33;
    }
}
