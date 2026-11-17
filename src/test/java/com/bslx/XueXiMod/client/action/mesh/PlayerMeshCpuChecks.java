package com.bslx.XueXiMod.client.action.mesh;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import com.bslx.XueXiMod.animation.core.Joint;
import com.bslx.XueXiMod.animation.core.JointTransform;
import com.bslx.XueXiMod.animation.core.Pose;
import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.model.Armature;
import com.bslx.XueXiMod.animation.runtime.ActionBootstrap;
import com.bslx.XueXiMod.animation.runtime.ActionSampleContext;
import com.bslx.XueXiMod.animation.runtime.loader.DevActionLoader;
import com.bslx.XueXiMod.animation.runtime.loader.DevLoadResult;

/** 手算探针和真实授权骨架回归。探针里的合成骨架不算真实资源通过。 */
public final class PlayerMeshCpuChecks {
    private static final Path DEV_ROOT = Path.of("D:/alianximod/dev-authorized/epicfight");
    private static final float TOLERANCE = 1.0E-4F;
    private static int checks;
    private static int realSkips;

    private PlayerMeshCpuChecks() {
    }

    public static void main(String[] programArguments) {
        checkNeutralBindIdentity();
        checkSingleJointRotationAndTranslation();
        checkBlendAndDistinctNormals();
        checkNonUniformNormal();
        checkUniformScaleKeepsNormalDirection();
        checkAlreadySkinnedDoesNotApplyInverseBindAgain();
        checkSingularPaletteFails();
        checkDegenerateQuadOrderAndSeam();
        checkPartMaskAndWholeMeshTranslation();
        checkNormalCacheKey();
        checkWorkspaceNestingAndThread();
        checkSameLeaseDeformReuse();
        checkPublicArrayOwnership();
        checkDecoderConversionAndRejection();
        checkImportedMeshes();
        System.out.println("PlayerMeshCpuChecks assertions=" + checks + " realSkips=" + realSkips);
    }

    private static void checkNeutralBindIdentity() {
        Joint root = new Joint("Root", 0, OpenMatrix4f.ofTranslation(3.0F, 0.0F, 0.0F, null));
        Joint child = new Joint("Child", 1, OpenMatrix4f.ofTranslation(0.0F, 5.0F, 0.0F, null));
        root.addSubJoints(child);
        Armature armature = syntheticArmature("probe", root, root, child);
        armature.bakeOriginMatrices();
        PlayerSkinMesh mesh = triangle(new float[] {1.0F, 2.0F, 3.0F}, new float[] {0.0F, 1.0F, 0.0F}, 2, new int[] {1}, new int[] {1}, new int[] {0}, new float[] {1.0F});
        float[] deformed = deform(mesh, CpuSkinner.preparePalette(armature, new Pose()), 0);
        near(deformed[0], 1.0F, "neutral x");
        near(deformed[1], 2.0F, "neutral y");
        near(deformed[2], 3.0F, "neutral z");
    }

    private static void checkSingleJointRotationAndTranslation() {
        Joint root = new Joint("Root", 0, OpenMatrix4f.ofTranslation(3.0F, 0.0F, 0.0F, null));
        Armature armature = syntheticArmature("probe", root, root);
        armature.bakeOriginMatrices();
        Pose pose = new Pose();
        JointTransform spin = JointTransform.empty();
        float halfTurn = (float) Math.sin(Math.PI / 4.0);
        spin.rotation().set(0.0F, 0.0F, halfTurn, (float) Math.cos(Math.PI / 4.0));
        pose.putJointData("Root", spin);
        PlayerSkinMesh mesh = triangle(new float[] {4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F}, new float[] {1.0F, 0.0F, 0.0F}, 1,
                new int[] {1, 1}, new int[] {0, 0}, new int[] {0, 0}, new float[] {1.0F});
        SkinPalette palette = CpuSkinner.preparePalette(armature, pose);
        float[] moved = deform(mesh, palette, 0);
        float[] normal = normalOf(mesh, palette, 0, 0);
        near(moved[0], 3.0F, "rotated bind x");
        near(moved[1], -1.0F, "rotated bind y");
        near(moved[2], 0.0F, "rotated bind z");
        near(normal[0], 0.0F, "rotated normal x");
        near(normal[1], -1.0F, "rotated normal y");
        near(normal[2], 0.0F, "rotated normal z");
    }

    private static void checkBlendAndDistinctNormals() {
        Joint root = new Joint("Root", 0, new OpenMatrix4f());
        Joint boneA = new Joint("BoneA", 1, new OpenMatrix4f());
        Joint boneB = new Joint("BoneB", 2, new OpenMatrix4f());
        Joint boneC = new Joint("BoneC", 3, new OpenMatrix4f());
        root.addSubJoints(boneA, boneB, boneC);
        Armature armature = syntheticArmature("probe", root, root, boneA, boneB, boneC);
        armature.bakeOriginMatrices();
        Pose pose = new Pose();
        pose.putJointData("BoneA", translation(2.0F, 0.0F, 0.0F));
        pose.putJointData("BoneB", translation(0.0F, 4.0F, 0.0F));
        pose.putJointData("BoneC", translation(0.0F, 0.0F, 8.0F));
        PlayerSkinMesh mesh = triangle(new float[] {1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F}, new float[] {1.0F, 0.0F, 0.0F}, 4,
                new int[] {2, 3}, new int[] {1, 2, 1, 2, 3}, new int[] {0, 1, 2, 3, 4}, new float[] {0.25F, 0.75F, 0.2F, 0.3F, 0.5F});
        float[] blended = deform(mesh, CpuSkinner.preparePalette(armature, pose), 0);
        near(blended[0], 1.5F, "two bone x");
        near(blended[1], 4.0F, "two bone y");
        near(blended[2], 1.0F, "two bone z");
        float[] triple = deform(mesh, CpuSkinner.preparePalette(armature, pose), 1);
        near(triple[0], 0.4F, "three bone x");
        near(triple[1], 1.2F, "three bone y");
        near(triple[2], 4.0F, "three bone z");
    }

    private static void checkNonUniformNormal() {
        Joint root = new Joint("Root", 0, new OpenMatrix4f());
        Armature armature = syntheticArmature("probe", root, root);
        armature.bakeOriginMatrices();
        Pose pose = new Pose();
        JointTransform stretch = JointTransform.empty();
        stretch.scale().set(2.0F, 0.5F, 1.0F);
        pose.putJointData("Root", stretch);
        PlayerSkinMesh mesh = triangle(new float[] {1.0F, 1.0F, 0.0F}, new float[] {1.0F, 1.0F, 0.0F}, 1,
                new int[] {1}, new int[] {0}, new int[] {0}, new float[] {1.0F});
        SkinPalette palette = CpuSkinner.preparePalette(armature, pose);
        float[] moved = deform(mesh, palette, 0);
        float[] normal = normalOf(mesh, palette, 0, 0);
        float unit = (float) (1.0 / Math.sqrt(17.0));
        near(moved[0], 2.0F, "scaled x");
        near(moved[1], 0.5F, "scaled y");
        near(moved[2], 0.0F, "scaled z");
        near(normal[0], unit, "inverse transpose x");
        near(normal[1], unit * 4.0F, "inverse transpose y");
        near(normal[2], 0.0F, "inverse transpose z");
    }

    private static void checkUniformScaleKeepsNormalDirection() {
        Joint root = new Joint("Root", 0, new OpenMatrix4f());
        Armature armature = syntheticArmature("probe", root, root);
        armature.bakeOriginMatrices();
        Pose pose = new Pose();
        JointTransform stretch = JointTransform.empty();
        stretch.scale().set(2.0F, 2.0F, 2.0F);
        pose.putJointData("Root", stretch);
        PlayerSkinMesh mesh = triangle(new float[] {1.0F, 0.0F, 0.0F}, new float[] {0.0F, 1.0F, 0.0F}, 1,
                new int[] {1}, new int[] {0}, new int[] {0}, new float[] {1.0F});
        SkinPalette palette = CpuSkinner.preparePalette(armature, pose);
        float[] moved = deform(mesh, palette, 0);
        float[] normal = normalOf(mesh, palette, 0, 0);
        near(moved[0], 2.0F, "uniform x");
        near(normal[0], 0.0F, "uniform normal x");
        near(normal[1], 1.0F, "uniform normal y");
        near(normal[2], 0.0F, "uniform normal z");
    }

    private static void checkAlreadySkinnedDoesNotApplyInverseBindAgain() {
        Joint root = new Joint("Root", 0, OpenMatrix4f.ofTranslation(3.0F, 0.0F, 0.0F, null));
        Armature armature = syntheticArmature("probe", root, root);
        armature.bakeOriginMatrices();
        OpenMatrix4f[] globalFrames = armature.getPoseAsTransformMatrix(new Pose(), false);
        OpenMatrix4f[] skinMatrices = new OpenMatrix4f[] {OpenMatrix4f.ofTranslation(5.0F, 0.0F, 0.0F, null)};
        SkinPalette palette = SkinPalette.fromAlreadySkinned(armature, globalFrames, skinMatrices);
        PlayerSkinMesh mesh = triangle(new float[] {1.0F, 0.0F, 0.0F}, new float[] {0.0F, 1.0F, 0.0F}, 1,
                new int[] {1}, new int[] {0}, new int[] {0}, new float[] {1.0F});
        float[] moved = deform(mesh, palette, 0);
        near(moved[0], 6.0F, "already skinned x");
        near(palette.copyGlobalFrame(0).m30, 3.0F, "global frame keeps translation");
        near(palette.copySkinMatrix(0).m30, 5.0F, "skin matrix stays explicit");
    }

    private static void checkSingularPaletteFails() {
        Joint root = new Joint("Root", 0, new OpenMatrix4f());
        Armature armature = syntheticArmature("probe", root, root);
        armature.bakeOriginMatrices();
        Pose pose = new Pose();
        JointTransform collapsed = JointTransform.empty();
        collapsed.scale().set(0.0F, 1.0F, 1.0F);
        pose.putJointData("Root", collapsed);
        expectDiagnostic(() -> CpuSkinner.preparePalette(armature, pose), "normal-matrix");
    }

    private static void checkDegenerateQuadOrderAndSeam() {
        Joint root = new Joint("Root", 0, new OpenMatrix4f());
        Armature armature = syntheticArmature("probe", root, root);
        armature.bakeOriginMatrices();
        int[] corners = new int[] {0, 0, 0, 1, 1, 0, 0, 2, 0};
        PlayerSkinMesh mesh = meshWithParts(new float[] {0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F}, new float[] {0.0F, 0.0F, 1.0F},
                new float[] {0.1F, 0.2F, 0.3F, 0.4F, 0.5F, 0.6F}, new float[] {1.0F, 1.0F}, new int[] {1, 1},
                new int[] {0, 0}, new int[] {0, 1}, 1, corners, null);
        RecordSink sink = new RecordSink();
        int emitted;
        try (SkinLease lease = SkinWorkspacePool.currentThread().open(mesh)) {
            emitted = MeshRenderView.emit(mesh, CpuSkinner.preparePalette(armature, new Pose()), lease, PartVisibility.allShown(), null, 7, 9, sink);
        }
        same(emitted, 4, "one triangle emits four vertices");
        same(sink.cornerCount, 4, "sink count");
        near(sink.component(0, 0), 0.0F, "quad a x");
        near(sink.component(1, 0), 1.0F, "quad b x");
        near(sink.component(2, 1), 0.0F, "quad c y");
        near(sink.component(3, 0), sink.component(2, 0), "repeated corner x");
        near(sink.component(3, 1), sink.component(2, 1), "repeated corner y");
        near(sink.component(0, 6), 0.1F, "first uv");
        near(sink.component(2, 6), 0.5F, "seam uv");
        same(sink.seenLight, 7, "light passed through");
        same(sink.seenOverlay, 9, "overlay passed through");
    }

    private static void checkPartMaskAndWholeMeshTranslation() {
        Joint root = new Joint("Root", 0, new OpenMatrix4f());
        Armature armature = syntheticArmature("probe", root, root);
        armature.bakeOriginMatrices();
        int[] head = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[] torso = new int[] {1, 0, 0, 1, 0, 0, 1, 0, 0};
        PlayerSkinMesh mesh = meshWithParts(new float[] {1.0F, 2.0F, 3.0F, 4.0F, 5.0F, 6.0F}, new float[] {0.0F, 1.0F, 0.0F},
                new float[] {0.0F, 0.0F}, new float[] {1.0F, 1.0F}, new int[] {1, 1}, new int[] {0, 0}, new int[] {0, 1}, 1, head, torso);
        SkinPalette palette = CpuSkinner.preparePalette(armature, new Pose());
        PartVisibility hiddenTorso = PartVisibility.allShown().with(PlayerMeshPart.TORSO, false);
        RecordSink masked = new RecordSink();
        try (SkinLease lease = SkinWorkspacePool.currentThread().open(mesh)) {
            int emitted = MeshRenderView.emit(mesh, palette, lease, hiddenTorso, null, 1, 0, masked);
            same(emitted, 4, "hidden part omitted");
            near(masked.component(0, 0), 1.0F, "visible part x");
        }
        OpenMatrix4f lift = OpenMatrix4f.ofTranslation(0.0F, 5.0F, 0.0F, null);
        RecordSink lifted = new RecordSink();
        try (SkinLease lease = SkinWorkspacePool.currentThread().open(mesh)) {
            MeshRenderView.emit(mesh, palette, lease, hiddenTorso, lift, 1, 0, lifted);
            float[] skinSpace = new float[3];
            CpuSkinner.copyDeformed(lease, 0, skinSpace);
            near(skinSpace[0], 1.0F, "deform stays source x");
            near(skinSpace[1], 2.0F, "deform stays source y");
            near(lifted.component(0, 0), 1.0F, "whole mesh x");
            near(lifted.component(0, 1), 7.0F, "whole mesh y");
            near(lifted.component(0, 2), 3.0F, "whole mesh z");
        }
        RecordSink rejected = new RecordSink();
        OpenMatrix4f collapsed = OpenMatrix4f.ofScale(0.0F, 1.0F, 1.0F, null);
        expectDiagnostic(() -> {
            try (SkinLease lease = SkinWorkspacePool.currentThread().open(mesh)) {
                MeshRenderView.emit(mesh, palette, lease, hiddenTorso, collapsed, 1, 0, rejected);
            }
        }, "normal-matrix");
        same(rejected.cornerCount, 0, "singular whole mesh writes nothing");
    }

    private static void checkNormalCacheKey() {
        Joint root = new Joint("Root", 0, new OpenMatrix4f());
        Joint boneA = new Joint("BoneA", 1, new OpenMatrix4f());
        Joint boneB = new Joint("BoneB", 2, new OpenMatrix4f());
        root.addSubJoints(boneA, boneB);
        Armature armature = syntheticArmature("probe", root, root, boneA, boneB);
        armature.bakeOriginMatrices();
        Pose pose = new Pose();
        JointTransform spin = JointTransform.empty();
        float halfTurn = (float) Math.sin(Math.PI / 4.0);
        spin.rotation().set(0.0F, 0.0F, halfTurn, (float) Math.cos(Math.PI / 4.0));
        pose.putJointData("BoneA", spin);
        int[] corners = new int[] {0, 0, 0, 1, 0, 0, 0, 0, 0};
        PlayerSkinMesh mesh = meshWithParts(new float[] {1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F}, new float[] {1.0F, 0.0F, 0.0F},
                new float[] {0.0F, 0.0F}, new float[] {1.0F, 1.0F}, new int[] {1, 1}, new int[] {1, 2}, new int[] {0, 1}, 3, corners, null);
        RecordSink sink = new RecordSink();
        try (SkinLease lease = SkinWorkspacePool.currentThread().open(mesh)) {
            MeshRenderView.emit(mesh, CpuSkinner.preparePalette(armature, pose), lease, PartVisibility.allShown(), null, 0, 0, sink);
            same(lease.normalEvaluations(), 2, "normal cache uses position and normal");
        }
        near(sink.component(0, 4), -1.0F, "first position normal");
        near(sink.component(1, 3), 1.0F, "second position keeps source normal");
    }

    private static void checkWorkspaceNestingAndThread() {
        Joint root = new Joint("Root", 0, new OpenMatrix4f());
        Armature armature = syntheticArmature("probe", root, root);
        armature.bakeOriginMatrices();
        SkinPalette palette = CpuSkinner.preparePalette(armature, new Pose());
        PlayerSkinMesh outerMesh = triangle(new float[] {1.0F, 0.0F, 0.0F}, new float[] {0.0F, 1.0F, 0.0F}, 1,
                new int[] {1}, new int[] {0}, new int[] {0}, new float[] {1.0F});
        PlayerSkinMesh innerMesh = triangle(new float[] {4.0F, 0.0F, 0.0F}, new float[] {0.0F, 1.0F, 0.0F}, 1,
                new int[] {1}, new int[] {0}, new int[] {0}, new float[] {1.0F});
        SkinWorkspacePool pool = SkinWorkspacePool.currentThread();
        try (SkinLease outer = pool.open(outerMesh)) {
            CpuSkinner.deform(outerMesh, palette, outer);
            try (SkinLease inner = pool.open(innerMesh)) {
                CpuSkinner.deform(innerMesh, palette, inner);
                float[] innerPoint = new float[3];
                CpuSkinner.copyDeformed(inner, 0, innerPoint);
                near(innerPoint[0], 4.0F, "inner workspace");
            }
            float[] outerPoint = new float[3];
            CpuSkinner.copyDeformed(outer, 0, outerPoint);
            near(outerPoint[0], 1.0F, "outer workspace intact");
            AtomicBoolean rejected = new AtomicBoolean(false);
            SkinLease held = outer;
            Thread worker = new Thread(() -> {
                try {
                    CpuSkinner.deform(outerMesh, palette, held);
                } catch (SkinDiagnostic diagnostic) {
                    rejected.set("thread".equals(diagnostic.reasonCode()));
                }
            });
            worker.start();
            worker.join();
            same(rejected.get() ? 1 : 0, 1, "other thread rejected");
        } catch (InterruptedException failure) {
            throw new AssertionError("worker interrupted");
        }
    }

    private static void checkSameLeaseDeformReuse() {
        Joint root = new Joint("Root", 0, new OpenMatrix4f());
        Armature armature = syntheticArmature("probe", root, root);
        armature.bakeOriginMatrices();
        int[] head = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[] torso = new int[] {1, 0, 0, 1, 0, 0, 1, 0, 0};
        PlayerSkinMesh mesh = meshWithParts(new float[] {1.0F, 2.0F, 3.0F, 4.0F, 5.0F, 6.0F}, new float[] {1.0F, 1.0F, 0.0F},
                new float[] {0.0F, 0.0F}, new float[] {1.0F, 1.0F}, new int[] {1, 1}, new int[] {0, 0}, new int[] {0, 1}, 1, head, torso);
        SkinPalette palette = CpuSkinner.preparePalette(armature, new Pose());
        SkinPalette replacementPalette = CpuSkinner.preparePalette(armature, new Pose());
        PlayerSkinMesh replacement = meshWithParts(new float[] {8.0F, 2.0F, 3.0F, 4.0F, 5.0F, 6.0F}, new float[] {1.0F, 1.0F, 0.0F},
                new float[] {0.0F, 0.0F}, new float[] {1.0F, 1.0F}, new int[] {1, 1}, new int[] {0, 0}, new int[] {0, 1}, 1, head, torso);
        PartVisibility hiddenTorso = PartVisibility.allShown().with(PlayerMeshPart.TORSO, false);
        OpenMatrix4f lift = OpenMatrix4f.ofTranslation(0.0F, 5.0F, 0.0F, null);
        OpenMatrix4f scaled = OpenMatrix4f.ofScale(2.0F, 0.5F, 1.0F, null);
        float axisUnit = (float) Math.sqrt(0.5);
        float inverseRootSeventeen = 1.0F / (float) Math.sqrt(17.0);
        RecordSink lifted = new RecordSink();
        RecordSink scaledSink = new RecordSink();
        RecordSink restored = new RecordSink();
        try (SkinLease lease = SkinWorkspacePool.currentThread().open(mesh)) {
            int liftedCount = MeshRenderView.emit(mesh, palette, lease, hiddenTorso, lift, 3, 4, lifted);
            same(liftedCount, 4, "first pass hides torso");
            same(lease.deformPreparations(), 1, "first pass prepares once");
            near(lifted.component(0, 0), 1.0F, "lifted x");
            near(lifted.component(0, 1), 7.0F, "lifted y");
            near(lifted.component(0, 2), 3.0F, "lifted z");
            near(lifted.component(0, 3), axisUnit, "translation keeps normal x");
            near(lifted.component(0, 4), axisUnit, "translation keeps normal y");
            same(lifted.seenLight, 3, "first light");
            same(lifted.seenOverlay, 4, "first overlay");
            int scaledCount = MeshRenderView.emit(mesh, palette, lease, PartVisibility.allShown(), scaled, 8, 6, scaledSink);
            same(scaledCount, 8, "second pass shows torso");
            same(lease.deformPreparations(), 1, "same palette does not prepare again");
            same(lease.normalEvaluations(), 2, "second pass adds the torso normal");
            near(scaledSink.component(0, 0), 2.0F, "scaled x");
            near(scaledSink.component(0, 1), 1.0F, "scaled y");
            near(scaledSink.component(0, 2), 3.0F, "scaled z");
            near(scaledSink.component(0, 3), inverseRootSeventeen, "scaled normal x");
            near(scaledSink.component(0, 4), 4.0F * inverseRootSeventeen, "scaled normal y");
            near(scaledSink.component(4, 0), 8.0F, "torso scaled x");
            near(scaledSink.component(4, 1), 2.5F, "torso scaled y");
            same(scaledSink.seenLight, 8, "second light");
            same(scaledSink.seenOverlay, 6, "second overlay");
            int restoredCount = MeshRenderView.emit(mesh, palette, lease, PartVisibility.allShown(), null, 1, 2, restored);
            same(restoredCount, 8, "third pass still shows both parts");
            same(lease.deformPreparations(), 1, "null transform still reuses preparation");
            same(lease.normalEvaluations(), 2, "third pass does not reskin normals");
            near(restored.component(0, 0), 1.0F, "restored x");
            near(restored.component(0, 1), 2.0F, "restored y");
            near(restored.component(0, 2), 3.0F, "restored z");
            near(restored.component(0, 3), axisUnit, "restored normal x");
            near(restored.component(0, 4), axisUnit, "restored normal y");
            near(restored.component(4, 0), 4.0F, "torso restored x");
            same(restored.seenLight, 1, "third light");
            same(restored.seenOverlay, 2, "third overlay");
            lease.workspace().deformedPositions[0] = 42.0F;
            RecordSink skipped = new RecordSink();
            MeshRenderView.emit(mesh, palette, lease, hiddenTorso, null, 0, 0, skipped);
            same(lease.deformPreparations(), 1, "poked buffer is not recomputed");
            near(skipped.component(0, 0), 42.0F, "skip keeps cached x");
            RecordSink replaced = new RecordSink();
            MeshRenderView.emit(replacement, palette, lease, hiddenTorso, null, 0, 0, replaced);
            same(lease.deformPreparations(), 2, "another mesh instance prepares again");
            near(replaced.component(0, 0), 8.0F, "replacement mesh x");
            RecordSink repainted = new RecordSink();
            MeshRenderView.emit(replacement, replacementPalette, lease, hiddenTorso, null, 0, 0, repainted);
            same(lease.deformPreparations(), 3, "another palette instance prepares again");
            near(repainted.component(0, 0), 8.0F, "replacement palette x");
            lease.workspace().deformedPositions[0] = 42.0F;
        }
        try (SkinLease reopened = SkinWorkspacePool.currentThread().open(replacement)) {
            RecordSink fresh = new RecordSink();
            MeshRenderView.emit(replacement, replacementPalette, reopened, hiddenTorso, null, 0, 0, fresh);
            same(reopened.deformPreparations(), 1, "reopened lease prepares again");
            near(fresh.component(0, 0), 8.0F, "reopened lease drops the poked cache");
        }
        PlayerSkinMesh huge = triangle(new float[] {Float.MAX_VALUE, 0.0F, 0.0F}, new float[] {0.0F, 1.0F, 0.0F}, 1,
                new int[] {1}, new int[] {0}, new int[] {0}, new float[] {1.0F});
        SkinPalette plainPalette = CpuSkinner.preparePalette(armature, new Pose());
        Pose overflowPose = new Pose();
        overflowPose.putJointData("Root", translation(Float.MAX_VALUE, 0.0F, 0.0F));
        SkinPalette overflowPalette = CpuSkinner.preparePalette(armature, overflowPose);
        try (SkinLease lease = SkinWorkspacePool.currentThread().open(huge)) {
            CpuSkinner.deform(huge, plainPalette, lease);
            same(lease.deformPreparations(), 1, "finite deform prepares");
            float[] finite = new float[3];
            CpuSkinner.copyDeformed(lease, 0, finite);
            near(finite[0], Float.MAX_VALUE, "finite position kept");
            expectDiagnostic(() -> CpuSkinner.deform(huge, overflowPalette, lease), "deform");
            same(lease.deformPreparations(), 1, "failed deform is not prepared");
            CpuSkinner.deform(huge, plainPalette, lease);
            same(lease.deformPreparations(), 2, "retry after failure prepares again");
            CpuSkinner.copyDeformed(lease, 0, finite);
            near(finite[0], Float.MAX_VALUE, "retry restores the finite position");
        }
    }

    private static void checkPublicArrayOwnership() {
        PlayerSkinMesh mesh = triangle(new float[] {1.5F, 2.5F, 3.5F}, new float[] {0.0F, 1.0F, 0.0F}, 1,
                new int[] {1}, new int[] {0}, new int[] {0}, new float[] {1.0F});
        float[] copied = mesh.copyPositions();
        float original = mesh.positionX(0);
        copied[0] = original + 9.0F;
        near(mesh.positionX(0), original, "copy does not alias storage");
        expectDiagnostic(() -> PlayerSkinMesh.compose(null, 1, new float[] {0.0F, 0.0F, 0.0F}, new float[] {0.0F, 1.0F, 0.0F},
                new float[] {0.0F, 0.0F}, new float[] {-1.0F}, new int[] {1}, new int[] {0}, new int[] {0},
                parts(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0}, null), false, "", "", true), "weight");
    }

    private static void checkDecoderConversionAndRejection() {
        PlayerSkinMesh converted = MeshJsonDecoder.decode(document(false, 0, 1.0F, 1), PlayerMeshKind.NORMAL);
        near(converted.positionX(0), 1.0F, "converted x");
        near(converted.positionY(0), 3.0F, "converted y");
        near(converted.positionZ(0), -2.0F, "converted z");
        near(converted.textureU(0), 0.25F, "uv unchanged");
        PlayerSkinMesh preserved = MeshJsonDecoder.decode(document(true, 0, 1.0F, 1), PlayerMeshKind.NORMAL);
        near(preserved.positionX(0), 1.0F, "already converted x");
        near(preserved.positionY(0), 2.0F, "already converted y");
        near(preserved.positionZ(0), 3.0F, "already converted z");
        expectDiagnostic(() -> MeshJsonDecoder.decode(document(false, 0, 1.0F, 1).replace("\"schemaVersion\":1", "\"schemaVersion\":2"), PlayerMeshKind.NORMAL), "schema");
        expectDiagnostic(() -> MeshJsonDecoder.decode(document(false, 0, 1.0F, 1).replace("\"schemaVersion\":1", "\"schemaVersion\":1,\"mesh_loader\":\"skinned_mesh\""), PlayerMeshKind.NORMAL), "unknown-field");
        expectDiagnostic(() -> MeshJsonDecoder.decode(document(false, 20, 1.0F, 1), PlayerMeshKind.NORMAL), "joint-id");
        expectDiagnostic(() -> MeshJsonDecoder.decode(document(false, 0, -0.2F, 1), PlayerMeshKind.NORMAL), "weight");
        expectDiagnostic(() -> MeshJsonDecoder.decode(document(false, 0, 1.0F, 1).replace("\"head\":{\"stride\":3,\"array\":[0,0,0,0,0,0,0,0,0]}", "\"head\":{\"stride\":3,\"array\":[0,0,0,0]}"), PlayerMeshKind.NORMAL), "stride");
        String missing = document(false, 0, 1.0F, 1).replace("\"hat\":{\"stride\":3,\"array\":[0,0,0,0,0,0,0,0,0]},", "");
        expectDiagnostic(() -> MeshJsonDecoder.decode(missing, PlayerMeshKind.NORMAL), "parts");
    }

    private static void checkImportedMeshes() {
        MeshLoadResult disabled = MeshRepository.reload(DEV_ROOT, false);
        same(disabled.usable(PlayerMeshKind.NORMAL) ? 1 : 0, 0, "switch off");
        same(disabled.playbackAccepted() ? 1 : 0, 0, "not playback accepted");
        try {
            checkRealAuthorizedResources();
        } finally {
            MeshRepository.clear();
            same(MeshRepository.current().usable(PlayerMeshKind.NORMAL) ? 1 : 0, 0, "cleared");
        }
    }

    private static void checkRealAuthorizedResources() {
        Path directory = authorizedDirectory();
        if (directory == null) {
            return;
        }
        requireAuthorizedFile(directory, "import-manifest.json");
        requireAuthorizedFile(directory, "rig/biped-armature.json");
        requireAuthorizedFile(directory, "mesh-manifest.json");
        requireAuthorizedFile(directory, "mesh/biped-normal.json");
        requireAuthorizedFile(directory, "mesh/biped-slim.json");
        DevLoadResult loadedRig = DevActionLoader.load(directory);
        Armature realArmature = loadedRig.sourceArmature();
        if (realArmature == null) {
            throw new AssertionError("真实骨架被 loader 拒绝：" + loadedRig.summaryText());
        }
        MeshLoadResult loaded = MeshRepository.reload(directory, true);
        if (!loaded.usable(PlayerMeshKind.NORMAL) || !loaded.usable(PlayerMeshKind.SLIM)) {
            throw new AssertionError(loaded.summary());
        }
        assertImported(loaded.mesh(PlayerMeshKind.NORMAL), 321);
        assertImported(loaded.mesh(PlayerMeshKind.SLIM), 332);
        SkinPalette neutralPalette = CpuSkinner.preparePalette(realArmature, new Pose());
        assertNeutralJoints(neutralPalette);
        assertNeutralPositions(neutralPalette, loaded.mesh(PlayerMeshKind.NORMAL));
        assertNeutralPositions(neutralPalette, loaded.mesh(PlayerMeshKind.SLIM));
        PlayerSkinMesh normal = loaded.mesh(PlayerMeshKind.NORMAL);
        RecordSink sink = new RecordSink();
        PartVisibility withoutHat = PartVisibility.allShown().with(PlayerMeshPart.HAT, false);
        try (SkinLease lease = SkinWorkspacePool.currentThread().open(normal)) {
            int emitted = MeshRenderView.emit(normal, neutralPalette, lease, PartVisibility.allShown(), null, 3, 4, sink);
            same(emitted, 1888, "full quad expansion");
            near(sink.component(3, 0), sink.component(2, 0), "real repeated corner");
            same(sink.seenLight, 3, "real light");
        }
        try (SkinLease lease = SkinWorkspacePool.currentThread().open(normal)) {
            int emitted = MeshRenderView.emit(normal, neutralPalette, lease, withoutHat, null, 3, 4, new RecordSink());
            same(emitted, 1888 - normal.partCornerCount(PlayerMeshPart.HAT) / 3 * 4, "hat masked");
        }
        same(findSeam(normal) ? 1 : 0, 1, "uv seam retained");
        ActionSampleContext sample = ActionSampleContext.capture(true, UUID.randomUUID(), 7, 11L, 13L, UUID.randomUUID(),
                null, null, realArmature.toString(), false, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.25F, new Pose(),
                19L, 1.0F, 0.25F, 0, 3, false, 0.0F);
        SkinSampleView view = SkinSampleView.bind(sample, realArmature, null);
        near(view.elapsedSeconds(), 0.25F, "sample clock");
        near(view.normalizedProgress(), 0.25F, "sample progress");
        same(view.startGameTick() == 19L ? 1 : 0, 1, "sample tick");
        near(view.copyWholeMeshTransform().m30, 0.0F, "no standing bake");
        expectDiagnostic(() -> SkinSampleView.bind(ActionSampleContext.inactive(UUID.randomUUID()), realArmature, null), "sample-inactive");
    }

    private static Path authorizedDirectory() {
        String propertyText = System.getProperty(ActionBootstrap.DEV_PROPERTY);
        if (propertyText != null && !propertyText.isBlank()) {
            Path configured;
            try {
                configured = Path.of(propertyText);
            } catch (RuntimeException failure) {
                throw new AssertionError("授权目录无法解析：" + failure.getMessage());
            }
            if (!configured.isAbsolute()) {
                throw new AssertionError("授权目录不是绝对路径");
            }
            if (!Files.isDirectory(configured)) {
                if (!Files.exists(configured)) {
                    throw new AssertionError("已配置的授权目录不存在");
                }
                throw new AssertionError("授权路径不是目录");
            }
            return configured;
        }
        if (!Files.isDirectory(DEV_ROOT)) {
            skipRealIntegration("未配置 " + ActionBootstrap.DEV_PROPERTY + "，默认授权目录不存在");
            return null;
        }
        return DEV_ROOT;
    }

    private static void requireAuthorizedFile(Path directory, String relativePath) {
        if (!Files.isRegularFile(directory.resolve(relativePath))) {
            throw new AssertionError("授权文件缺失：" + relativePath);
        }
    }

    private static void assertNeutralJoints(SkinPalette neutralPalette) {
        same(neutralPalette.jointCount(), MeshLimits.JOINT_LIMIT, "real joint count");
        for (int jointId = 0; jointId < neutralPalette.jointCount(); jointId++) {
            nearIdentity(neutralPalette.copySkinMatrix(jointId), "joint " + jointId);
        }
    }

    private static void assertNeutralPositions(SkinPalette neutralPalette, PlayerSkinMesh mesh) {
        same(mesh.positionCount(), 260, mesh.kind().wireName() + " position count");
        float[] deformedPoint = new float[3];
        try (SkinLease lease = SkinWorkspacePool.currentThread().open(mesh)) {
            CpuSkinner.deform(mesh, neutralPalette, lease);
            for (int positionIndex = 0; positionIndex < mesh.positionCount(); positionIndex++) {
                CpuSkinner.copyDeformed(lease, positionIndex, deformedPoint);
                near(deformedPoint[0], mesh.positionX(positionIndex), mesh.kind().wireName() + " x " + positionIndex);
                near(deformedPoint[1], mesh.positionY(positionIndex), mesh.kind().wireName() + " y " + positionIndex);
                near(deformedPoint[2], mesh.positionZ(positionIndex), mesh.kind().wireName() + " z " + positionIndex);
            }
        }
    }

    private static void nearIdentity(OpenMatrix4f skinMatrix, String label) {
        near(skinMatrix.m00, 1.0F, label + " m00");
        near(skinMatrix.m01, 0.0F, label + " m01");
        near(skinMatrix.m02, 0.0F, label + " m02");
        near(skinMatrix.m03, 0.0F, label + " m03");
        near(skinMatrix.m10, 0.0F, label + " m10");
        near(skinMatrix.m11, 1.0F, label + " m11");
        near(skinMatrix.m12, 0.0F, label + " m12");
        near(skinMatrix.m13, 0.0F, label + " m13");
        near(skinMatrix.m20, 0.0F, label + " m20");
        near(skinMatrix.m21, 0.0F, label + " m21");
        near(skinMatrix.m22, 1.0F, label + " m22");
        near(skinMatrix.m23, 0.0F, label + " m23");
        near(skinMatrix.m30, 0.0F, label + " m30");
        near(skinMatrix.m31, 0.0F, label + " m31");
        near(skinMatrix.m32, 0.0F, label + " m32");
        near(skinMatrix.m33, 1.0F, label + " m33");
    }

    private static void skipRealIntegration(String reason) {
        realSkips++;
        System.out.println("SKIP 真实资源集成: " + reason);
    }

    private static void assertImported(PlayerSkinMesh mesh, int textureCount) {
        same(mesh.positionCount(), 260, "positions");
        same(mesh.normalCount(), 6, "normals");
        same(mesh.textureCount(), textureCount, "uvs");
        same(mesh.influenceCount(), 325, "influences");
        same(mesh.cornerCount(), 1416, "corners");
        same(mesh.triangleCount(), 472, "triangles");
        same(mesh.partCount(), 12, "parts");
        same(mesh.partCornerCount(PlayerMeshPart.HEAD), 36, "head");
        same(mesh.partCornerCount(PlayerMeshPart.HAT), 36, "hat");
        same(mesh.partCornerCount(PlayerMeshPart.TORSO), 240, "torso");
        same(mesh.partCornerCount(PlayerMeshPart.JACKET), 240, "jacket");
        same(mesh.partCornerCount(PlayerMeshPart.LEFT_ARM), 108, "left arm");
        same(mesh.partCornerCount(PlayerMeshPart.RIGHT_PANTS), 108, "right pants");
        same(mesh.coordinateConversionApplied() ? 1 : 0, 1, "converted once");
        float weightSum = 0.0F;
        for (int influenceSlot = 0; influenceSlot < mesh.influenceCountAt(0); influenceSlot++) {
            weightSum += mesh.weight(mesh.influenceWeightSlot(0, influenceSlot));
        }
        near(weightSum, 1.0F, "first weight sum");
    }

    private static boolean findSeam(PlayerSkinMesh mesh) {
        for (PlayerMeshPart part : PlayerMeshPart.values()) {
            int start = 0;
            int count = mesh.partCornerCount(part);
            for (PlayerMeshPart earlier : PlayerMeshPart.values()) {
                if (earlier == part) {
                    break;
                }
                if (mesh.hasPart(earlier)) {
                    start += mesh.partCornerCount(earlier);
                }
            }
            for (int left = 0; left < count; left++) {
                for (int right = left + 1; right < count; right++) {
                    if (mesh.cornerPosition(start + left) == mesh.cornerPosition(start + right)
                            && mesh.cornerTexture(start + left) != mesh.cornerTexture(start + right)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static String document(boolean alreadyConverted, int jointId, float weight, int schemaVersion) {
        String corners = "[0,0,0,0,0,0,0,0,0]";
        StringBuilder text = new StringBuilder();
        text.append("{\"schemaVersion\":").append(schemaVersion);
        text.append(",\"model\":\"normal\",\"upstreamCommit\":\"").append(MeshLimits.UPSTREAM_COMMIT).append('"');
        text.append(",\"sourceGitPath\":\"").append(PlayerMeshKind.NORMAL.sourceGitPath()).append('"');
        text.append(",\"sourceSha256\":\"").append("a".repeat(64)).append('"');
        text.append(",\"localOnly\":true,\"authorization\":\"").append(MeshLimits.AUTHORIZATION).append('"');
        text.append(",\"rigRelativePath\":\"").append(MeshLimits.RIG_RELATIVE_PATH).append('"');
        text.append(",\"coordinateSpace\":\"source-meters\",\"coordinateConversionApplied\":").append(alreadyConverted ? "true" : "false");
        text.append(",\"playbackReady\":false");
        text.append(",\"positions\":{\"stride\":3,\"array\":[1,2,3]}");
        text.append(",\"normals\":{\"stride\":3,\"array\":[0,1,0]}");
        text.append(",\"uvs\":{\"stride\":2,\"array\":[0.25,0.5]}");
        text.append(",\"weights\":{\"stride\":1,\"array\":[").append(weight).append("]}");
        text.append(",\"vcounts\":{\"stride\":1,\"array\":[1]}");
        text.append(",\"vindices\":{\"stride\":2,\"array\":[").append(jointId).append(",0]}");
        text.append(",\"parts\":{");
        PlayerMeshPart[] parts = PlayerMeshPart.values();
        for (int partIndex = 0; partIndex < parts.length; partIndex++) {
            if (partIndex > 0) {
                text.append(',');
            }
            text.append('"').append(parts[partIndex].wireName()).append("\":{\"stride\":3,\"array\":").append(corners).append('}');
        }
        text.append("},\"counts\":{\"parts\":12,\"positions\":1,\"normals\":1,\"uvs\":1,\"weights\":1,\"influences\":1,\"corners\":36,\"triangles\":12,\"highestInfluence\":1}}");
        return text.toString();
    }

    private static float[] deform(PlayerSkinMesh mesh, SkinPalette palette, int positionIndex) {
        float[] destination = new float[3];
        try (SkinLease lease = SkinWorkspacePool.currentThread().open(mesh)) {
            CpuSkinner.deform(mesh, palette, lease);
            CpuSkinner.copyDeformed(lease, positionIndex, destination);
        }
        return destination;
    }

    private static float[] normalOf(PlayerSkinMesh mesh, SkinPalette palette, int positionIndex, int normalIndex) {
        RecordSink sink = new RecordSink();
        try (SkinLease lease = SkinWorkspacePool.currentThread().open(mesh)) {
            MeshRenderView.emit(mesh, palette, lease, PartVisibility.allShown(), null, 0, 0, sink);
        }
        int matched = -1;
        int searchable = sink.cornerCount < mesh.cornerCount() ? sink.cornerCount : mesh.cornerCount();
        for (int cornerIndex = 0; cornerIndex < searchable; cornerIndex++) {
            if (mesh.cornerPosition(cornerIndex) == positionIndex && mesh.cornerNormal(cornerIndex) == normalIndex) {
                matched = cornerIndex;
                break;
            }
        }
        if (matched < 0) {
            throw new AssertionError("missing normal corner");
        }
        return new float[] {sink.component(matched, 3), sink.component(matched, 4), sink.component(matched, 5)};
    }

    private static PlayerSkinMesh triangle(float[] positions, float[] normals, int jointLimit, int[] influenceCounts, int[] joints, int[] weightSlots, float[] weights) {
        int[] corners = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0};
        return meshWithParts(positions, normals, new float[] {0.0F, 0.0F}, weights, influenceCounts, joints, weightSlots, jointLimit, corners, null);
    }

    private static PlayerSkinMesh meshWithParts(
            float[] positions, float[] normals, float[] textureCoordinates, float[] weights, int[] influenceCounts,
            int[] joints, int[] weightSlots, int jointLimit, int[] headCorners, int[] torsoCorners) {
        return PlayerSkinMesh.compose(null, jointLimit, positions, normals, textureCoordinates, weights, influenceCounts,
                joints, weightSlots, parts(headCorners, torsoCorners), false, "", "", true);
    }

    private static int[][] parts(int[] headCorners, int[] torsoCorners) {
        int[][] partCorners = new int[PlayerMeshPart.values().length][];
        partCorners[PlayerMeshPart.HEAD.ordinal()] = headCorners;
        partCorners[PlayerMeshPart.TORSO.ordinal()] = torsoCorners;
        return partCorners;
    }

    private static Armature syntheticArmature(String name, Joint root, Joint... joints) {
        Map<String, Joint> map = new HashMap<>();
        for (Joint joint : joints) {
            map.put(joint.getName(), joint);
        }
        return new Armature(name, joints.length, root, map);
    }

    private static JointTransform translation(float coordinateX, float coordinateY, float coordinateZ) {
        JointTransform transform = JointTransform.empty();
        transform.translation().set(coordinateX, coordinateY, coordinateZ);
        return transform;
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
        if (!Float.isFinite(actual) || Math.abs(actual - expected) > TOLERANCE) {
            throw new AssertionError(label + " expected " + expected + " but got " + actual);
        }
        checks++;
    }

    private static void same(int actual, int expected, String label) {
        if (actual != expected) {
            throw new AssertionError(label + " expected " + expected + " but got " + actual);
        }
        checks++;
    }

    private static final class RecordSink implements CornerSink {
        private final float[] packed = new float[16_384];
        private int cursor;
        private int cornerCount;
        private int seenLight;
        private int seenOverlay;

        @Override
        public void corner(
                float positionX, float positionY, float positionZ,
                float normalX, float normalY, float normalZ,
                float textureU, float textureV, int packedLight, int overlay) {
            if (this.cursor + 8 > this.packed.length) {
                throw new AssertionError("sink overflow");
            }
            this.packed[this.cursor++] = positionX;
            this.packed[this.cursor++] = positionY;
            this.packed[this.cursor++] = positionZ;
            this.packed[this.cursor++] = normalX;
            this.packed[this.cursor++] = normalY;
            this.packed[this.cursor++] = normalZ;
            this.packed[this.cursor++] = textureU;
            this.packed[this.cursor++] = textureV;
            this.cornerCount++;
            this.seenLight = packedLight;
            this.seenOverlay = overlay;
        }

        private float component(int cornerIndex, int element) {
            return this.packed[cornerIndex * 8 + element];
        }
    }
}
