package com.bslx.XueXiMod.client.action.skinned;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.bslx.XueXiMod.animation.core.Joint;
import com.bslx.XueXiMod.animation.core.Pose;
import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.math.Vec3f;
import com.bslx.XueXiMod.animation.core.model.Armature;
import com.bslx.XueXiMod.client.action.mesh.PlayerMeshKind;
import com.bslx.XueXiMod.client.action.mesh.PlayerMeshPart;
import com.bslx.XueXiMod.client.action.mesh.PlayerSkinMesh;
import com.bslx.XueXiMod.client.action.mesh.SkinDiagnostic;
import com.bslx.XueXiMod.client.action.mesh.SkinLease;
import com.bslx.XueXiMod.client.action.mesh.SkinPalette;
import com.bslx.XueXiMod.client.action.mesh.SkinWorkspacePool;

/** 合成矩阵上的帧数据协议。不读取授权网格，也不改真实资源检查。 */
public final class SkinFrameDataChecks {
    private static final UUID OWNER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final float TOLERANCE = 1.0E-4F;
    private static final float TIGHT = 1.0E-5F;
    private static int checks;

    private SkinFrameDataChecks() {
    }

    public static void main(String[] programArguments) {
        checkBodyMapScaleAndVanillaResidual();
        checkBodyMapIsNotCached();
        checkThirdPersonFrameCopies();
        checkBindRestAndParentRotation();
        checkAlignmentAppliedOnce();
        checkGlobalNotSkin();
        checkFrameKeepsSuppliedGlobalRatherThanFk();
        checkNoLease();
        checkSingularAndNonFinite();
        checkFirstPersonUsesExplicitView();
        checkPlanRejectsIncompleteDecision();
        checkSourceDoesNotReachForbiddenApis();
        System.out.println("SkinFrameDataChecks assertions=" + checks);
    }

    private static void checkBodyMapScaleAndVanillaResidual() {
        OpenMatrix4f bodyMap = AttachmentTransform.sourceToBody();
        near(AttachmentTransform.BODY_MODEL_LIFT, 1.5F, "published lift");
        near(bodyMap.m00, -1.0F, "across scale");
        near(bodyMap.m11, -1.0F, "up scale");
        near(bodyMap.m22, 1.0F, "ahead scale");
        near(bodyMap.m31, 1.5F, "body lift");
        near(bodyMap.m30, 0.0F, "no across shift");
        near(bodyMap.m32, 0.0F, "no ahead shift");
        if (Math.abs(bodyMap.m00 + 0.9375F) < TIGHT) {
            throw new AssertionError("body map absorbed player scale");
        }
        checks++;
        Vec3f mapped = OpenMatrix4f.transform3v(bodyMap, new Vec3f(1.0F, 2.0F, 3.0F), null);
        near(mapped.coordinateX, -1.0F, "mapped x");
        near(mapped.coordinateY, -0.5F, "mapped y");
        near(mapped.coordinateZ, 3.0F, "mapped z");
        OpenMatrix4f vanillaDrop = OpenMatrix4f.createTranslation(0.0F, -1.501F, 0.0F);
        OpenMatrix4f composed = OpenMatrix4f.mul(vanillaDrop, bodyMap, null);
        Vec3f foot = OpenMatrix4f.transform3v(composed, new Vec3f(0.0F, 0.0F, 0.0F), null);
        near(foot.coordinateY, 1.5F - 1.501F, "vanilla foot residual");
        if (Math.abs(foot.coordinateY) < 5.0E-4F) {
            throw new AssertionError("vanilla residual cancelled");
        }
        checks++;
        OpenMatrix4f rewritten = AttachmentTransform.sourceToBody();
        rewritten.m31 = 1.501F;
        OpenMatrix4f placed = AttachmentTransform.composePlacement(new OpenMatrix4f(), rewritten);
        near(placed.m31, 1.501F, "helper keeps the caller view");
        near(AttachmentTransform.sourceToBody().m31, 1.5F, "fresh body map after rewrite");
    }

    private static void checkBodyMapIsNotCached() {
        OpenMatrix4f firstBody = AttachmentTransform.sourceToBody();
        OpenMatrix4f secondBody = AttachmentTransform.sourceToBody();
        same(firstBody == secondBody ? 1 : 0, 0, "body map is not cached");
        firstBody.m31 = 9.0F;
        near(secondBody.m31, 1.5F, "other body map");
        near(AttachmentTransform.sourceToBody().m31, 1.5F, "later body map");
    }

    private static void checkThirdPersonFrameCopies() {
        Armature armature = syntheticArmature();
        OpenMatrix4f toolGlobal = OpenMatrix4f.createTranslation(1.0F, 2.0F, 0.3F);
        OpenMatrix4f toolSkin = OpenMatrix4f.createTranslation(9.0F, 0.0F, 0.0F);
        SkinPalette palette = paletteWithTool(armature, toolGlobal, toolSkin);
        PlayerSkinMesh mesh = syntheticMesh();
        OpenMatrix4f alignment = OpenMatrix4f.createRotatorDeg(90.0F, Vec3f.Y_AXIS);
        OpenMatrix4f alignmentSnapshot = new OpenMatrix4f(alignment);
        RenderPlan plan = skinnedPlan(RenderPlan.Perspective.THIRD_PERSON, RenderPlan.Hand.NONE);
        ImmutableSkinFrame.Clock clock = clock();
        int depthBefore = leaseDepth();
        ImmutableSkinFrame frame = ImmutableSkinFrame.captureThirdPerson(plan, mesh, palette, alignment, clock);
        same(leaseDepth(), depthBefore, "third person capture lease depth");
        alignment.m00 = 4.0F;
        alignment.m31 = 8.0F;
        toolGlobal.m30 = 99.0F;
        same(frame.plan() == plan ? 1 : 0, 1, "plan retained");
        same(frame.clock() == clock ? 1 : 0, 1, "clock retained");
        same(frame.mesh() == mesh ? 1 : 0, 1, "mesh retained");
        same(frame.palette() == palette ? 1 : 0, 1, "palette retained");
        nearMatrix(frame.copyAlignment(), alignmentSnapshot, "input alignment detached");
        OpenMatrix4f returnedAlignment = frame.copyAlignment();
        returnedAlignment.m00 = 6.0F;
        nearMatrix(frame.copyAlignment(), alignmentSnapshot, "output alignment detached");
        nearMatrix(frame.copySourceToView(), AttachmentTransform.sourceToBody(), "third person view is body map");
        nearMatrix(frame.copyBodyMap(), AttachmentTransform.sourceToBody(), "body map accessor");
        OpenMatrix4f returnedView = frame.copyBodyMap();
        returnedView.m31 = 1.501F;
        near(frame.copyBodyMap().m31, 1.5F, "output body map detached");
        OpenMatrix4f once = AttachmentTransform.composePlacement(alignmentSnapshot, AttachmentTransform.sourceToBody());
        OpenMatrix4f twice = AttachmentTransform.composePlacement(alignmentSnapshot, once);
        nearMatrix(frame.copyMeshPlacement(), once, "placement applies alignment once");
        differ(frame.copyMeshPlacement(), twice, "placement is not alignment twice");
        OpenMatrix4f returnedPlacement = frame.copyMeshPlacement();
        returnedPlacement.m30 = 7.0F;
        nearMatrix(frame.copyMeshPlacement(), once, "output placement detached");
        near(frame.copyGlobalFrame("Tool_R").m30, 1.0F, "global survives input mutation");
        near(frame.copyGlobalFrame(1).m31, 2.0F, "global by id");
        OpenMatrix4f returnedGlobal = frame.copyToolRightFrame();
        returnedGlobal.m30 = 50.0F;
        near(frame.copyToolRightFrame().m30, 1.0F, "output global detached");
        near(frame.clock().elapsedSeconds(), 0.25F, "elapsed");
        near(frame.clock().normalizedProgress(), 0.25F, "progress");
        same(frame.clock().startGameTick() == 19L ? 1 : 0, 1, "tick");
        same(frame.clock().revision(), 3, "revision");
        same(frame.plan().resourceGeneration() == 4L ? 1 : 0, 1, "generation");
        same(frame.plan().entityId(), 7, "entity");
    }

    private static void checkBindRestAndParentRotation() {
        OpenMatrix4f bindGlobal = OpenMatrix4f.createTranslation(1.0F, 2.0F, 0.3F);
        OpenMatrix4f restTarget = OpenMatrix4f.createTranslation(0.4F, -0.2F, 0.3F);
        OpenMatrix4f bodyMap = AttachmentTransform.sourceToBody();
        OpenMatrix4f identity = new OpenMatrix4f();
        OpenMatrix4f correction = AttachmentTransform.runtimeCorrection(bindGlobal, bodyMap, restTarget);
        float correctionElement = correction.m30;
        restTarget.m30 = 15.0F;
        near(correction.m30, correctionElement, "rest input detached");
        restTarget.m30 = 0.4F;
        OpenMatrix4f savedCorrection = new OpenMatrix4f(correction);
        correction.m30 = 12.0F;
        OpenMatrix4f bound = AttachmentTransform.localDraw(identity, bodyMap, bindGlobal, savedCorrection);
        nearMatrix(bound, restTarget, "bind restores rest");
        Vec3f samplePoint = new Vec3f(0.2F, -0.4F, 0.5F);
        Vec3f boundPoint = OpenMatrix4f.transform3v(bound, samplePoint, null);
        Vec3f restPoint = OpenMatrix4f.transform3v(restTarget, samplePoint, null);
        near(boundPoint.coordinateX, restPoint.coordinateX, "bind point x");
        near(boundPoint.coordinateY, restPoint.coordinateY, "bind point y");
        near(boundPoint.coordinateZ, restPoint.coordinateZ, "bind point z");

        OpenMatrix4f parentTurn = OpenMatrix4f.createRotatorDeg(90.0F, Vec3f.Z_AXIS);
        OpenMatrix4f animatedGlobal = OpenMatrix4f.mul(parentTurn, bindGlobal, null);
        OpenMatrix4f animated = AttachmentTransform.localDraw(identity, bodyMap, animatedGlobal, savedCorrection);
        OpenMatrix4f manual = manualLocal(identity, bodyMap, animatedGlobal, bindGlobal, restTarget);
        nearMatrix(animated, manual, "parent formula");
        differ(animated, restTarget, "parent rotation moves the attachment");
        OpenMatrix4f inverseView = OpenMatrix4f.invert(bodyMap, null);
        OpenMatrix4f inverseBind = OpenMatrix4f.invert(bindGlobal, null);
        Vec3f restOrigin = OpenMatrix4f.transform3v(restTarget, new Vec3f(0.0F, 0.0F, 0.0F), null);
        Vec3f sourceRest = OpenMatrix4f.transform3v(inverseView, restOrigin, null);
        Vec3f rotated = OpenMatrix4f.transform3v(parentTurn, sourceRest, null);
        OpenMatrix4f jointDelta = OpenMatrix4f.mul(animatedGlobal, inverseBind, null);
        Vec3f followed = OpenMatrix4f.transform3v(jointDelta, sourceRest, null);
        near(followed.coordinateX, rotated.coordinateX, "parent x");
        near(followed.coordinateY, rotated.coordinateY, "parent y");
        near(followed.coordinateZ, rotated.coordinateZ, "parent z");
        Vec3f inBody = OpenMatrix4f.transform3v(bodyMap, followed, null);
        Vec3f drawn = OpenMatrix4f.transform3v(animated, new Vec3f(0.0F, 0.0F, 0.0F), null);
        near(drawn.coordinateX, inBody.coordinateX, "drawn x");
        near(drawn.coordinateY, inBody.coordinateY, "drawn y");
        near(drawn.coordinateZ, inBody.coordinateZ, "drawn z");
    }

    private static void checkAlignmentAppliedOnce() {
        OpenMatrix4f bindGlobal = OpenMatrix4f.createTranslation(1.0F, 2.0F, 0.3F);
        OpenMatrix4f restTarget = OpenMatrix4f.createTranslation(0.4F, -0.2F, 0.3F);
        OpenMatrix4f bodyMap = AttachmentTransform.sourceToBody();
        OpenMatrix4f parentTurn = OpenMatrix4f.createRotatorDeg(90.0F, Vec3f.Z_AXIS);
        OpenMatrix4f animatedGlobal = OpenMatrix4f.mul(parentTurn, bindGlobal, null);
        OpenMatrix4f alignment = OpenMatrix4f.createRotatorDeg(90.0F, Vec3f.Y_AXIS);
        OpenMatrix4f correction = AttachmentTransform.runtimeCorrection(bindGlobal, bodyMap, restTarget);
        OpenMatrix4f once = AttachmentTransform.localDraw(alignment, bodyMap, animatedGlobal, correction);
        OpenMatrix4f manual = manualLocal(alignment, bodyMap, animatedGlobal, bindGlobal, restTarget);
        OpenMatrix4f twice = OpenMatrix4f.mul(alignment, once, null);
        nearMatrix(once, manual, "alignment once");
        differ(once, twice, "alignment is not applied twice");
        differ(once, AttachmentTransform.localDraw(new OpenMatrix4f(), bodyMap, animatedGlobal, correction), "alignment changes the draw");
    }

    private static void checkGlobalNotSkin() {
        Armature armature = syntheticArmature();
        OpenMatrix4f toolGlobal = OpenMatrix4f.createTranslation(1.0F, 2.0F, 0.3F);
        OpenMatrix4f toolSkin = OpenMatrix4f.createTranslation(9.0F, 0.0F, 0.0F);
        SkinPalette palette = paletteWithTool(armature, toolGlobal, toolSkin);
        OpenMatrix4f restTarget = OpenMatrix4f.createTranslation(0.4F, -0.2F, 0.3F);
        OpenMatrix4f bodyMap = AttachmentTransform.sourceToBody();
        OpenMatrix4f global = AttachmentTransform.copyJointGlobal(palette, "Tool_R");
        nearMatrix(global, toolGlobal, "helper reads global");
        differ(global, palette.copySkinMatrix(1), "global is not skin");
        OpenMatrix4f fromGlobal = AttachmentTransform.localDrawFromPalette(
                new OpenMatrix4f(), bodyMap, palette, "Tool_R", toolGlobal, restTarget);
        nearMatrix(fromGlobal, restTarget, "palette global restores rest");
        OpenMatrix4f correction = AttachmentTransform.runtimeCorrection(toolGlobal, bodyMap, restTarget);
        OpenMatrix4f fromSkin = AttachmentTransform.localDraw(new OpenMatrix4f(), bodyMap, palette.copySkinMatrix(1), correction);
        differ(fromSkin, restTarget, "skin matrix does not restore rest");
    }

    private static void checkFrameKeepsSuppliedGlobalRatherThanFk() {
        Armature armature = syntheticArmature();
        OpenMatrix4f toolGlobal = OpenMatrix4f.createTranslation(1.0F, 2.0F, 0.3F);
        OpenMatrix4f toolSkin = OpenMatrix4f.createTranslation(9.0F, 0.0F, 0.0F);
        SkinPalette palette = paletteWithTool(armature, toolGlobal, toolSkin);
        ImmutableSkinFrame frame = ImmutableSkinFrame.captureThirdPerson(
                skinnedPlan(RenderPlan.Perspective.THIRD_PERSON, RenderPlan.Hand.RIGHT),
                syntheticMesh(),
                palette,
                new OpenMatrix4f(),
                clock());
        OpenMatrix4f[] solved = armature.getPoseAsTransformMatrix(new Pose(), false);
        near(solved[1].m30, 5.0F, "fk uses bind local");
        near(solved[1].m31, 0.0F, "fk has no extra lift");
        near(frame.copyGlobalFrame("Tool_R").m30, 1.0F, "frame keeps supplied x");
        near(frame.copyGlobalFrame("Tool_R").m31, 2.0F, "frame keeps supplied y");
        near(frame.copySkinMatrix(1).m30, 9.0F, "frame keeps supplied skin");
        differ(frame.copyGlobalFrame(1), solved[1], "frame is not a second fk");
        same(frame.palette() == palette ? 1 : 0, 1, "palette was not rebuilt");
    }

    private static void checkNoLease() {
        PlayerSkinMesh mesh = syntheticMesh();
        SkinPalette palette = paletteWithTool(syntheticArmature(), OpenMatrix4f.createTranslation(1.0F, 2.0F, 0.3F), OpenMatrix4f.createTranslation(9.0F, 0.0F, 0.0F));
        same(leaseDepth(), 0, "lease starts closed");
        RenderPlan vanilla = RenderPlan.of(
                RenderPlan.Mode.VANILLA, OWNER, 7, RenderPlan.Perspective.THIRD_PERSON, RenderPlan.Hand.NONE,
                PlayerMeshKind.NORMAL, 4L, "inactive");
        expectDiagnostic(() -> ImmutableSkinFrame.captureThirdPerson(vanilla, mesh, palette, new OpenMatrix4f(), clock()), "frame-mode");
        same(leaseDepth(), 0, "rejected vanilla does not lease");
        RenderPlan rigid = RenderPlan.of(
                RenderPlan.Mode.RIGID, OWNER, 7, RenderPlan.Perspective.THIRD_PERSON, RenderPlan.Hand.NONE,
                PlayerMeshKind.SLIM, 4L, "resource-missing");
        expectDiagnostic(() -> ImmutableSkinFrame.capture(rigid, mesh, palette, new OpenMatrix4f(), AttachmentTransform.sourceToBody(), clock()), "frame-mode");
        same(leaseDepth(), 0, "rejected rigid does not lease");
        SkinWorkspacePool pool = SkinWorkspacePool.currentThread();
        try (SkinLease lease = pool.open(mesh)) {
            same(lease.isOpen() ? 1 : 0, 1, "outer lease open");
            same(leaseDepth(), 1, "one lease before frame");
            ImmutableSkinFrame.captureThirdPerson(
                    skinnedPlan(RenderPlan.Perspective.THIRD_PERSON, RenderPlan.Hand.NONE),
                    mesh, palette, new OpenMatrix4f(), clock());
            same(leaseDepth(), 1, "frame does not push a lease");
            same(lease.isOpen() ? 1 : 0, 1, "outer lease still open");
        }
        same(leaseDepth(), 0, "outer lease released");
    }

    private static void checkSingularAndNonFinite() {
        OpenMatrix4f singular = new OpenMatrix4f();
        singular.m00 = 0.0F;
        OpenMatrix4f broken = new OpenMatrix4f();
        broken.m11 = Float.NaN;
        OpenMatrix4f infinite = new OpenMatrix4f();
        infinite.m22 = Float.POSITIVE_INFINITY;
        expectDiagnostic(() -> AttachmentTransform.runtimeCorrection(singular, AttachmentTransform.sourceToBody(), new OpenMatrix4f()), "not-invertible");
        expectDiagnostic(() -> AttachmentTransform.runtimeCorrection(null, AttachmentTransform.sourceToBody(), new OpenMatrix4f()), "bind-missing");
        expectDiagnostic(() -> AttachmentTransform.localDraw(new OpenMatrix4f(), broken, new OpenMatrix4f(), new OpenMatrix4f()), "non-finite");
        expectDiagnostic(() -> AttachmentTransform.composePlacement(infinite, AttachmentTransform.sourceToBody()), "non-finite");
        expectDiagnostic(() -> AttachmentTransform.localDraw(singular, AttachmentTransform.sourceToBody(), new OpenMatrix4f(), new OpenMatrix4f()), "not-invertible");
        near(AttachmentTransform.sourceToBody().m00, -1.0F, "rejection leaves body map");
        near(AttachmentTransform.sourceToBody().m31, 1.5F, "rejection does not rewrite lift");
        SkinPalette palette = paletteWithTool(syntheticArmature(), OpenMatrix4f.createTranslation(1.0F, 2.0F, 0.3F), new OpenMatrix4f());
        PlayerSkinMesh mesh = syntheticMesh();
        expectDiagnostic(() -> ImmutableSkinFrame.captureThirdPerson(
                skinnedPlan(RenderPlan.Perspective.THIRD_PERSON, RenderPlan.Hand.NONE),
                mesh, palette, singular, clock()), "not-invertible");
        expectDiagnostic(() -> ImmutableSkinFrame.captureThirdPerson(
                skinnedPlan(RenderPlan.Perspective.THIRD_PERSON, RenderPlan.Hand.NONE),
                mesh, palette, broken, clock()), "non-finite");
        OpenMatrix4f cancelled = AttachmentTransform.sourceToBody();
        cancelled.m31 = 1.501F;
        expectDiagnostic(() -> ImmutableSkinFrame.capture(
                skinnedPlan(RenderPlan.Perspective.THIRD_PERSON, RenderPlan.Hand.NONE),
                mesh, palette, new OpenMatrix4f(), cancelled, clock()), "body-map");
    }

    private static void checkFirstPersonUsesExplicitView() {
        OpenMatrix4f explicitView = OpenMatrix4f.createTranslation(0.0F, 0.0F, 2.0F);
        OpenMatrix4f bindGlobal = OpenMatrix4f.createTranslation(1.0F, 2.0F, 0.3F);
        OpenMatrix4f restTarget = OpenMatrix4f.createTranslation(0.4F, -0.2F, 0.3F);
        OpenMatrix4f identity = new OpenMatrix4f();
        OpenMatrix4f parentTurn = OpenMatrix4f.createRotatorDeg(90.0F, Vec3f.Z_AXIS);
        OpenMatrix4f animatedGlobal = OpenMatrix4f.mul(parentTurn, bindGlobal, null);
        OpenMatrix4f bound = AttachmentTransform.localDrawFromPalette(
                identity, explicitView, paletteWithTool(syntheticArmature(), bindGlobal, new OpenMatrix4f()), "Tool_R", bindGlobal, restTarget);
        nearMatrix(bound, restTarget, "explicit view restores rest");
        OpenMatrix4f correction = AttachmentTransform.runtimeCorrection(bindGlobal, explicitView, restTarget);
        OpenMatrix4f animated = AttachmentTransform.localDraw(identity, explicitView, animatedGlobal, correction);
        nearMatrix(animated, manualLocal(identity, explicitView, animatedGlobal, bindGlobal, restTarget), "explicit view formula");
        OpenMatrix4f bodyCorrection = AttachmentTransform.runtimeCorrection(bindGlobal, AttachmentTransform.sourceToBody(), restTarget);
        OpenMatrix4f bodyAnimated = AttachmentTransform.localDraw(identity, AttachmentTransform.sourceToBody(), animatedGlobal, bodyCorrection);
        differ(animated, bodyAnimated, "explicit view is not the body map");
        PlayerSkinMesh mesh = syntheticMesh();
        SkinPalette palette = paletteWithTool(syntheticArmature(), bindGlobal, OpenMatrix4f.createTranslation(9.0F, 0.0F, 0.0F));
        RenderPlan plan = skinnedPlan(RenderPlan.Perspective.FIRST_PERSON, RenderPlan.Hand.LEFT);
        expectDiagnostic(() -> ImmutableSkinFrame.capture(plan, mesh, palette, identity, AttachmentTransform.sourceToBody(), clock()), "first-person-body-map");
        expectDiagnostic(() -> ImmutableSkinFrame.captureThirdPerson(plan, mesh, palette, identity, clock()), "perspective");
        ImmutableSkinFrame frame = ImmutableSkinFrame.capture(plan, mesh, palette, identity, explicitView, clock());
        near(frame.copySourceToView().m32, 2.0F, "frame keeps explicit view");
        differ(frame.copySourceToView(), AttachmentTransform.sourceToBody(), "frame view is not body map");
        expectDiagnostic(() -> frame.copyBodyMap(), "first-person-body-map");
        OpenMatrix4f returned = frame.copySourceToView();
        returned.m32 = 5.0F;
        near(frame.copySourceToView().m32, 2.0F, "explicit view copy detached");
        nearMatrix(frame.copyMeshPlacement(), explicitView, "first person placement is the explicit view");
    }

    private static void checkPlanRejectsIncompleteDecision() {
        expectDiagnostic(() -> RenderPlan.of(
                RenderPlan.Mode.VANILLA, null, 1, RenderPlan.Perspective.THIRD_PERSON, RenderPlan.Hand.NONE,
                PlayerMeshKind.NORMAL, 0L, "inactive"), "owner-missing");
        expectDiagnostic(() -> RenderPlan.of(
                RenderPlan.Mode.RIGID, OWNER, 1, RenderPlan.Perspective.THIRD_PERSON, RenderPlan.Hand.NONE,
                PlayerMeshKind.NORMAL, 0L, " "), "reason-missing");
        expectDiagnostic(() -> RenderPlan.of(
                RenderPlan.Mode.SKINNED, OWNER, 1, RenderPlan.Perspective.FIRST_PERSON, RenderPlan.Hand.NONE,
                PlayerMeshKind.SLIM, 2L, "active-authorized-mesh"), "hand-missing");
        expectDiagnostic(() -> RenderPlan.of(
                RenderPlan.Mode.SKINNED, OWNER, 1, RenderPlan.Perspective.THIRD_PERSON, RenderPlan.Hand.NONE,
                null, 2L, "active-authorized-mesh"), "body-kind");
        expectDiagnostic(() -> RenderPlan.of(
                RenderPlan.Mode.SKINNED, OWNER, 1, RenderPlan.Perspective.THIRD_PERSON, RenderPlan.Hand.NONE,
                PlayerMeshKind.NORMAL, -1L, "active-authorized-mesh"), "generation");
        expectDiagnostic(() -> ImmutableSkinFrame.Clock.of(" ", 0L, 0.0F, 0.0F, 0), "action-name");
        expectDiagnostic(() -> ImmutableSkinFrame.Clock.of("probe-action", 0L, -0.1F, 0.0F, 0), "elapsed");
        expectDiagnostic(() -> ImmutableSkinFrame.Clock.of("probe-action", 0L, 0.0F, 1.1F, 0), "progress");
        expectDiagnostic(() -> ImmutableSkinFrame.Clock.of("probe-action", 0L, Float.NaN, 0.0F, 0), "elapsed");
        expectDiagnostic(() -> ImmutableSkinFrame.Clock.of("probe-action", 0L, 0.0F, 0.0F, -1), "revision");
        RenderPlan plan = skinnedPlan(RenderPlan.Perspective.THIRD_PERSON, RenderPlan.Hand.NONE);
        same(plan.mode() == RenderPlan.Mode.SKINNED ? 1 : 0, 1, "mode stored");
        same(plan.reason().equals("active-authorized-mesh") ? 1 : 0, 1, "reason stored");
        same(plan.bodyKind() == PlayerMeshKind.NORMAL ? 1 : 0, 1, "kind stored");
        same(plan.ownerId().equals(OWNER) ? 1 : 0, 1, "owner stored");
    }

    private static void checkSourceDoesNotReachForbiddenApis() {
        String[] sharedTokens = {
                "SkinLease", "SkinWorkspace", "ThreadLocal", "PoseStack", "ModelPart", "ActionRegistry",
                "MeshRepository", "DevActionLoader", "getPoseAsTransformMatrix", "RenderPoseScope",
                "ActionSampleContext", "CpuSkinner", "SkinPalette.prepare"
        };
        rejectTokens(sourceText("src/main/java/com/bslx/XueXiMod/client/action/skinned/RenderPlan.java"), sharedTokens);
        rejectTokens(sourceText("src/main/java/com/bslx/XueXiMod/client/action/skinned/ImmutableSkinFrame.java"), sharedTokens);
        rejectTokens(sourceText("src/main/java/com/bslx/XueXiMod/client/action/skinned/AttachmentTransform.java"), sharedTokens);
        rejectTokens(sourceText("src/main/java/com/bslx/XueXiMod/client/action/skinned/AttachmentTransform.java"), new String[] {"copySkinMatrix", "1.501", "0.9375"});
        rejectTokens(sourceText("src/main/java/com/bslx/XueXiMod/client/action/skinned/ImmutableSkinFrame.java"), new String[] {"1.501", "0.9375"});
        rejectStaticMatrix(AttachmentTransform.class);
        rejectStaticState(ImmutableSkinFrame.class);
        rejectStaticState(ImmutableSkinFrame.Clock.class);
        rejectStaticState(RenderPlan.class);
        rejectMethodFragments(RenderPlan.class);
        rejectMethodFragments(ImmutableSkinFrame.class);
        rejectMethodFragments(ImmutableSkinFrame.Clock.class);
        rejectMethodFragments(AttachmentTransform.class);
        checks++;
    }

    private static OpenMatrix4f manualLocal(
            OpenMatrix4f alignment,
            OpenMatrix4f sourceToView,
            OpenMatrix4f currentGlobal,
            OpenMatrix4f bindGlobal,
            OpenMatrix4f restTarget) {
        OpenMatrix4f inverseView = OpenMatrix4f.invert(sourceToView, null);
        OpenMatrix4f inverseBind = OpenMatrix4f.invert(bindGlobal, null);
        OpenMatrix4f correction = OpenMatrix4f.mul(inverseBind, OpenMatrix4f.mul(inverseView, restTarget, null), null);
        OpenMatrix4f placed = OpenMatrix4f.mul(sourceToView, OpenMatrix4f.mul(currentGlobal, correction, null), null);
        return OpenMatrix4f.mul(alignment, placed, null);
    }

    private static RenderPlan skinnedPlan(RenderPlan.Perspective perspective, RenderPlan.Hand hand) {
        return RenderPlan.of(
                RenderPlan.Mode.SKINNED, OWNER, 7, perspective, hand, PlayerMeshKind.NORMAL, 4L, "active-authorized-mesh");
    }

    private static ImmutableSkinFrame.Clock clock() {
        return ImmutableSkinFrame.Clock.of("probe-action", 19L, 0.25F, 0.25F, 3);
    }

    private static Armature syntheticArmature() {
        Joint root = new Joint("Root", 0, new OpenMatrix4f());
        Joint tool = new Joint("Tool_R", 1, OpenMatrix4f.createTranslation(5.0F, 0.0F, 0.0F));
        root.addSubJoints(tool);
        Map<String, Joint> joints = new HashMap<>();
        joints.put("Root", root);
        joints.put("Tool_R", tool);
        return new Armature("probe-frame", 2, root, joints);
    }

    private static SkinPalette paletteWithTool(Armature armature, OpenMatrix4f toolGlobal, OpenMatrix4f toolSkin) {
        OpenMatrix4f rootGlobal = new OpenMatrix4f();
        OpenMatrix4f rootSkin = OpenMatrix4f.createTranslation(4.0F, 0.0F, 0.0F);
        return SkinPalette.fromAlreadySkinned(
                armature,
                new OpenMatrix4f[] {rootGlobal, toolGlobal},
                new OpenMatrix4f[] {rootSkin, toolSkin});
    }

    private static PlayerSkinMesh syntheticMesh() {
        int[] headCorners = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[][] partCorners = new int[PlayerMeshPart.values().length][];
        partCorners[PlayerMeshPart.HEAD.ordinal()] = headCorners;
        return PlayerSkinMesh.compose(
                PlayerMeshKind.NORMAL,
                2,
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

    private static int leaseDepth() {
        try {
            Field activeField = SkinWorkspacePool.class.getDeclaredField("activeWorkspaces");
            activeField.setAccessible(true);
            Object workspaceDeque = activeField.get(SkinWorkspacePool.currentThread());
            return ((ArrayDeque<?>) workspaceDeque).size();
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("无法观察租约深度", failure);
        }
    }

    private static void rejectTokens(String source, String[] tokens) {
        for (String token : tokens) {
            if (source.contains(token)) {
                throw new AssertionError("源码出现了不该有的 " + token);
            }
        }
    }

    private static void rejectStaticMatrix(Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            if (OpenMatrix4f.class.isAssignableFrom(field.getType())) {
                throw new AssertionError("矩阵被缓存: " + field.getName());
            }
            if (Modifier.isStatic(field.getModifiers())) {
                Class<?> fieldType = field.getType();
                if (fieldType != float.class && fieldType != int.class && fieldType != long.class && fieldType != boolean.class && fieldType != String.class) {
                    throw new AssertionError("静态状态 " + field.getName());
                }
            }
        }
    }

    private static void rejectStaticState(Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                throw new AssertionError("静态状态 " + type.getSimpleName() + "." + field.getName());
            }
        }
    }

    private static void rejectMethodFragments(Class<?> type) {
        String[] fragments = {"armor", "equipment", "precheck", "emit", "deform", "reload", "prepare"};
        for (Method method : type.getDeclaredMethods()) {
            String methodName = method.getName().toLowerCase(Locale.ROOT);
            if (methodName.startsWith("set") && method.getParameterCount() > 0) {
                throw new AssertionError(type.getSimpleName() + " " + method.getName());
            }
            for (String fragment : fragments) {
                if (methodName.contains(fragment)) {
                    throw new AssertionError(type.getSimpleName() + " " + method.getName());
                }
            }
        }
    }

    private static String sourceText(String relativePath) {
        Path location = Path.of(relativePath);
        if (!Files.isRegularFile(location)) {
            throw new AssertionError("缺少源文件 " + location.toAbsolutePath());
        }
        try {
            return Files.readString(location);
        } catch (IOException failure) {
            throw new AssertionError("无法读取 " + relativePath, failure);
        }
    }

    private static void expectDiagnostic(Runnable action, String reasonCode) {
        try {
            action.run();
        } catch (SkinDiagnostic diagnostic) {
            if (!reasonCode.equals(diagnostic.reasonCode())) {
                throw new AssertionError("expected " + reasonCode + " but got " + diagnostic.reasonCode() + " " + diagnostic.getMessage());
            }
            checks++;
            return;
        }
        throw new AssertionError("missing diagnostic " + reasonCode);
    }

    private static void near(float actual, float expected, String label) {
        if (!Float.isFinite(actual) || Math.abs(actual - expected) > TIGHT) {
            throw new AssertionError(label + " expected " + expected + " but got " + actual);
        }
        checks++;
    }

    private static void nearMatrix(OpenMatrix4f actual, OpenMatrix4f expected, String label) {
        float[] actualElements = elements(actual);
        float[] expectedElements = elements(expected);
        for (int elementIndex = 0; elementIndex < actualElements.length; elementIndex++) {
            if (!Float.isFinite(actualElements[elementIndex]) || Math.abs(actualElements[elementIndex] - expectedElements[elementIndex]) > TOLERANCE) {
                throw new AssertionError(label + " e" + elementIndex + " expected " + expectedElements[elementIndex] + " but got " + actualElements[elementIndex]);
            }
            checks++;
        }
    }

    private static void differ(OpenMatrix4f actual, OpenMatrix4f unexpected, String label) {
        float[] actualElements = elements(actual);
        float[] unexpectedElements = elements(unexpected);
        for (int elementIndex = 0; elementIndex < actualElements.length; elementIndex++) {
            if (Float.isFinite(actualElements[elementIndex]) && Math.abs(actualElements[elementIndex] - unexpectedElements[elementIndex]) > TOLERANCE) {
                checks++;
                return;
            }
        }
        throw new AssertionError(label + " unexpectedly matched");
    }

    private static float[] elements(OpenMatrix4f matrix) {
        return new float[] {
                matrix.m00, matrix.m01, matrix.m02, matrix.m03,
                matrix.m10, matrix.m11, matrix.m12, matrix.m13,
                matrix.m20, matrix.m21, matrix.m22, matrix.m23,
                matrix.m30, matrix.m31, matrix.m32, matrix.m33
        };
    }

    private static void same(int actual, int expected, String label) {
        if (actual != expected) {
            throw new AssertionError(label + " expected " + expected + " but got " + actual);
        }
        checks++;
    }
}
