package com.bslx.XueXiMod.client.action.skinned.attachments;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.bslx.XueXiMod.animation.core.Joint;
import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.math.Vec3f;
import com.bslx.XueXiMod.animation.core.model.Armature;
import com.bslx.XueXiMod.client.action.mesh.CpuSkinner;
import com.bslx.XueXiMod.client.action.mesh.MeshLimits;
import com.bslx.XueXiMod.client.action.mesh.PlayerMeshKind;
import com.bslx.XueXiMod.client.action.mesh.PlayerMeshPart;
import com.bslx.XueXiMod.client.action.mesh.PlayerSkinMesh;
import com.bslx.XueXiMod.client.action.mesh.SkinDiagnostic;
import com.bslx.XueXiMod.client.action.mesh.SkinLease;
import com.bslx.XueXiMod.client.action.mesh.SkinPalette;
import com.bslx.XueXiMod.client.action.mesh.SkinWorkspacePool;
import com.bslx.XueXiMod.client.action.skinned.AttachmentTransform;
import com.bslx.XueXiMod.client.action.skinned.ImmutableSkinFrame;
import com.bslx.XueXiMod.client.action.skinned.RenderPlan;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** 附件蒙皮的本地检查。不启动游戏，也不读取授权护甲贴图。 */
public final class SkinAttachmentChecks {
    private static final UUID OWNER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final long GENERATION = 11L;
    private static final float TIGHT = 1.0E-4F;
    private static final float LOOSE = 1.0E-3F;
    private static int checks;

    private SkinAttachmentChecks() {
    }

    public static void main(String[] programArguments) {
        int depthBefore = leaseDepth();
        BindJointTable.clearCache();
        ArmorMeshes.clearCache();
        ArmorDraw.discardThread();
        LayerHold.discardThread();
        StuckMarks.discardThread();
        SkinAttachmentBridge.replaceItemDraw(null);
        try {
            checkMixinNames();
            checkNoCopiedArmorAssets();
            Armature armature = syntheticArmature();
            BindJointTable table = BindJointTable.capture(armature, GENERATION);
            checkBakedOrigins(table);
            SkinPalette boundPalette = palette(armature, table, null, 0.0F, false);
            PlayerSkinMesh bodyMesh = bodyMesh();
            checkWeights(table);
            checkBend(table, boundPalette, armature);
            checkGlobalSeparateFromSkin(armature, table, bodyMesh, boundPalette);
            checkMotion(table, bodyMesh, boundPalette);
            checkFirstPersonItem(table, bodyMesh, boundPalette);
            checkVanillaArmor(table, bodyMesh, boundPalette);
            checkLifecycle(table, bodyMesh, boundPalette);
            checkAffine();
            checkGuards();
        } finally {
            ArmorDraw.discardThread();
            LayerHold.discardThread();
            StuckMarks.discardThread();
            ArmorMeshes.clearCache();
            BindJointTable.clearCache();
            SkinAttachmentBridge.replaceItemDraw(null);
        }
        same(leaseDepth(), depthBefore, "leases closed");
        System.out.println("SkinAttachmentChecks assertions=" + checks);
    }

    private static void checkMixinNames() {
        String[] names = SkinAttachmentBridge.MIXIN_CLASS_NAMES;
        same(names.length, 10, "mixin count");
        text(names[0], "client.skin.HumanoidArmorSkinMixin", "armor mixin");
        text(names[1], "client.skin.HumanoidArmorLayerAccess", "armor access");
        text(names[2], "client.skin.LivingRendererLayersAccessor", "layer access");
        text(names[3], "client.skin.ItemInHandSkinMixin", "held mixin");
        text(names[4], "client.skin.CustomHeadSkinMixin", "head mixin");
        text(names[5], "client.skin.CapeSkinMixin", "cape mixin");
        text(names[6], "client.skin.ElytraSkinMixin", "elytra mixin");
        text(names[7], "client.skin.StuckInBodySkinMixin", "stuck mixin");
        text(names[8], "client.skin.PlayerSkinOverlayAccessor", "overlay access");
        text(names[9], "client.skin.StuckLayerScopeMixin", "stuck scope mixin");
        String cape = sourceText("src/main/java/com/bslx/XueXiMod/mixin/client/skin/CapeSkinMixin.java");
        String elytra = sourceText("src/main/java/com/bslx/XueXiMod/mixin/client/skin/ElytraSkinMixin.java");
        String held = sourceText("src/main/java/com/bslx/XueXiMod/mixin/client/skin/ItemInHandSkinMixin.java");
        String head = sourceText("src/main/java/com/bslx/XueXiMod/mixin/client/skin/CustomHeadSkinMixin.java");
        String stuck = sourceText("src/main/java/com/bslx/XueXiMod/mixin/client/skin/StuckInBodySkinMixin.java");
        String stuckScope = sourceText("src/main/java/com/bslx/XueXiMod/mixin/client/skin/StuckLayerScopeMixin.java");
        String armor = sourceText("src/main/java/com/bslx/XueXiMod/mixin/client/skin/HumanoidArmorSkinMixin.java");
        String bridge = sourceText("src/main/java/com/bslx/XueXiMod/client/action/skinned/attachments/SkinAttachmentBridge.java");
        String meshes = sourceText("src/main/java/com/bslx/XueXiMod/client/action/skinned/attachments/ArmorMeshes.java");
        truth(cape.contains("prependChest") && cape.contains("poseStack.translate"), "cape keeps the vanilla shift");
        truth(elytra.contains("prependChest") && elytra.contains("poseStack.translate"), "elytra keeps the vanilla shift");
        absent(cape.contains("PlayerSkinMesh") || elytra.contains("PlayerSkinMesh"), "cape and elytra emit no second mesh");
        absent(cape.contains("beginCape") || elytra.contains("beginElytra") || head.contains("beginHead"), "head cape elytra drop head injects");
        absent(held.contains("beginHeld") || stuck.contains("beginStuck") || armor.contains("beginArmorPiece"), "held stuck armor drop head injects");
        truth(held.contains("applyHeld") && held.contains("renderItem") && held.contains("xuexiCallArm"), "held keeps item renderer and the original pass");
        truth(head.contains("replaceHead") && stuck.contains("replaceStuckPoint"), "head and stuck call the bridge");
        truth(stuckScope.contains("StuckInBodyLayer") && stuckScope.contains("finally") && stuckScope.contains("openStuck"), "stuck wrapper is exclusive");
        truth(armor.contains("finally") && armor.contains("xuexiCallArmorPiece") && armor.contains("require = 4"), "armor finally wraps four calls");
        same(occurrences(armor, "remap = false"), 1, "only the forge hook disables remap");
        absent(bridge.contains("mulPoseMatrix"), "bridge does not use mulPoseMatrix");
        same(occurrences(bridge, "AffinePose.apply"), 5, "five affine sites");
        absent(sourceText("src/main/java/com/bslx/XueXiMod/client/action/skinned/attachments/ArmorDraw.java").contains("AffinePose"), "armor sink keeps its own normal path");
        truth(meshes.contains("void defaultColor") && meshes.contains("void unsetDefaultColor"), "collector implements color defaults");
        truth(bridge.contains("renderStatic") && bridge.contains("leftHand") && bridge.contains("OverlayTexture"), "item draw keeps display and mirror");
        String preflight = sourceText("src/main/java/com/bslx/XueXiMod/client/action/skinned/attachments/ArmorPreflight.java");
        absent(preflight.contains("ArmorDraw") || preflight.contains("VertexConsumer"), "preflight does not draw");
    }

    private static void checkNoCopiedArmorAssets() {
        String[] banned = {"epicfight", "textures/models/armor", "armor_layer_1", "armor_layer_2"};
        try (Stream<Path> walk = Files.walk(Path.of("src/main/java/com/bslx/XueXiMod/client/action/skinned/attachments"))) {
            List<Path> sources = walk.filter(path -> path.toString().endsWith(".java")).toList();
            truth(!sources.isEmpty(), "attachment sources exist");
            for (Path source : sources) {
                String text = Files.readString(source);
                for (String token : banned) {
                    absent(text.contains(token), source.getFileName() + " copied " + token);
                }
            }
        } catch (IOException failure) {
            throw new AssertionError("无法读取附件源码", failure);
        }
    }

    private static void checkBakedOrigins(BindJointTable table) {
        Vec3f arm = table.origin("Arm_R");
        near(arm.coordinateX, 0.35F, TIGHT, "arm bind x");
        near(arm.coordinateY, 1.40F, TIGHT, "arm bind y");
        near(arm.coordinateZ, 0.0F, TIGHT, "arm bind z");
        Vec3f tool = table.origin("Tool_R");
        near(tool.coordinateX, 0.35F, TIGHT, "tool bind x");
        near(tool.coordinateY, 0.60F, TIGHT, "tool bind y");
        near(tool.coordinateZ, 0.10F, TIGHT, "tool bind z");
        differ(table.global("Tool_R"), new OpenMatrix4f(), "tool bind is not identity");
        truth(table.has("Tool_L") && table.has("Elbow_L") && table.has("Knee_L") && table.has("Chest"), "required joints");
        same(table.jointId("Head"), indexOf("Head"), "head id follows the rig");
        same(table.jointLimit(), MeshLimits.JOINT_NAMES.length, "full rig limit");
    }

    private static void checkWeights(BindJointTable table) {
        checkChain(table, ArmorRegion.ARM_RIGHT, "Arm_R", "Hand_R", "Elbow_R");
        checkChain(table, ArmorRegion.ARM_LEFT, "Arm_L", "Hand_L", "Elbow_L");
        checkChain(table, ArmorRegion.LEG_RIGHT, "Thigh_R", "Leg_R", "Knee_R");
        checkChain(table, ArmorRegion.LEG_LEFT, "Thigh_L", "Leg_L", "Knee_L");
        Vec3f head = table.origin("Head");
        ArmorWeights.Influences headWeights = ArmorWeights.weigh(
                ArmorRegion.HEAD, head.coordinateX, head.coordinateY, head.coordinateZ, table, 0.0F, 1.0F);
        same(headWeights.jointIds.length, 1, "head is one bone");
        same(headWeights.jointIds[0], table.jointId("Head"), "head bone");
        near(headWeights.weights[0], 1.0F, TIGHT, "head weight");
        Vec3f torsoMid = pointOn(table, "Torso", "Chest", 0.5F);
        ArmorWeights.Influences torso = ArmorWeights.weigh(
                ArmorRegion.TORSO, torsoMid.coordinateX, torsoMid.coordinateY, torsoMid.coordinateZ, table, 0.0F, 1.0F);
        truth(weightOn(torso, table.jointId("Torso")) > 0.2F && weightOn(torso, table.jointId("Chest")) > 0.2F, "torso middle blends");
    }

    private static void checkChain(BindJointTable table, ArmorRegion region, String startName, String endName, String sideName) {
        float sideParameter = ArmorWeights.clamp01(ArmorWeights.along(
                table, startName, endName,
                table.origin(sideName).coordinateX,
                table.origin(sideName).coordinateY,
                table.origin(sideName).coordinateZ));
        ArmorWeights.Influences middle = weighAt(table, region, startName, endName, sideParameter);
        int sideId = table.jointId(sideName);
        int startId = table.jointId(startName);
        int endId = table.jointId(endName);
        truth(weightOn(middle, sideId) > 0.2F, sideName + " middle");
        truth(weightOn(middle, startId) > 0.0F && weightOn(middle, endId) > 0.0F, sideName + " keeps both ends");
        ArmorWeights.Influences start = weighAt(table, region, startName, endName, 0.0F);
        ArmorWeights.Influences end = weighAt(table, region, startName, endName, 1.0F);
        truth(weightOn(start, sideId) < 0.001F && weightOn(start, startId) > 0.99F, sideName + " start collapses");
        truth(weightOn(end, sideId) < 0.001F && weightOn(end, endId) > 0.99F, sideName + " end collapses");
        float[] previous = new float[3];
        int blended = 0;
        int collapsed = 0;
        for (int sampleIndex = 0; sampleIndex <= 20; sampleIndex++) {
            float parameter = sampleIndex / 20.0F;
            ArmorWeights.Influences sample = weighAt(table, region, startName, endName, parameter);
            float[] current = new float[] {
                    weightOn(sample, startId),
                    weightOn(sample, endId),
                    weightOn(sample, sideId)
            };
            float sum = current[0] + current[1] + current[2];
            near(sum, 1.0F, TIGHT, sideName + " sum");
            for (int boneIndex = 0; boneIndex < current.length; boneIndex++) {
                truth(current[boneIndex] >= 0.0F, sideName + " non-negative");
                if (sampleIndex > 0) {
                    truth(Math.abs(current[boneIndex] - previous[boneIndex]) < 0.25F, sideName + " continuous");
                }
                previous[boneIndex] = current[boneIndex];
            }
            if (sample.jointIds.length >= 2) {
                blended++;
            }
            if (sample.jointIds.length == 1) {
                collapsed++;
            }
        }
        truth(blended > 0 && collapsed > 0, sideName + " has both a blend and an endpoint");
    }

    private static void checkBend(BindJointTable table, SkinPalette boundPalette, Armature armature) {
        PlayerSkinMesh elbowMesh = bendMesh(table, ArmorRegion.ARM_RIGHT, PlayerMeshPart.RIGHT_ARM, "Arm_R", "Hand_R", "Elbow_R");
        PlayerSkinMesh kneeMesh = bendMesh(table, ArmorRegion.LEG_RIGHT, PlayerMeshPart.RIGHT_LEG, "Thigh_R", "Leg_R", "Knee_R");
        float elbowWeight = meshWeight(elbowMesh, 0, table.jointId("Elbow_R"));
        float kneeWeight = meshWeight(kneeMesh, 0, table.jointId("Knee_R"));
        truth(elbowWeight > 0.2F && kneeWeight > 0.2F, "bend meshes sit on the side bone");
        truth(meshWeight(elbowMesh, 1, table.jointId("Elbow_R")) < 0.001F, "arm endpoint ignores elbow");
        truth(meshWeight(kneeMesh, 1, table.jointId("Knee_R")) < 0.001F, "leg endpoint ignores knee");
        SkinPalette elbowShift = palette(armature, table, "Elbow_R", 1.0F, false);
        SkinPalette kneeShift = palette(armature, table, "Knee_R", 1.0F, false);
        float[] elbowMiddle = deformedAt(elbowMesh, elbowShift, 0);
        float[] elbowEnd = deformedAt(elbowMesh, elbowShift, 1);
        float[] kneeMiddle = deformedAt(kneeMesh, kneeShift, 0);
        float[] kneeEnd = deformedAt(kneeMesh, kneeShift, 1);
        near(elbowMiddle[2], elbowWeight, LOOSE, "elbow bend follows its weight");
        near(elbowEnd[2], 0.0F, LOOSE, "arm endpoint stays put");
        near(kneeMiddle[2], kneeWeight, LOOSE, "knee bend follows its weight");
        near(kneeEnd[2], 0.0F, LOOSE, "leg endpoint stays put");
        float[] unbound = deformedAt(elbowMesh, boundPalette, 0);
        near(unbound[2], elbowMesh.positionZ(0), LOOSE, "identity skin leaves the vertex");
    }

    private static void checkGlobalSeparateFromSkin(
            Armature armature,
            BindJointTable table,
            PlayerSkinMesh bodyMesh,
            SkinPalette boundPalette) {
        SkinPalette skinShift = palette(armature, table, "Tool_R", 4.0F, false);
        SkinPalette globalShift = palette(armature, table, "Tool_R", 3.0F, true);
        ImmutableSkinFrame.Clock clock = clock();
        OpenMatrix4f alignment = new OpenMatrix4f();
        OpenMatrix4f view = OpenMatrix4f.createTranslation(1.0F, 0.0F, 0.0F);
        RenderPlan plan = plan(RenderPlan.Perspective.FIRST_PERSON, RenderPlan.Hand.RIGHT);
        ImmutableSkinFrame bound = ImmutableSkinFrame.capture(plan, bodyMesh, boundPalette, alignment, view, clock);
        ImmutableSkinFrame skinned = ImmutableSkinFrame.capture(plan, bodyMesh, skinShift, alignment, view, clock);
        ImmutableSkinFrame moved = ImmutableSkinFrame.capture(plan, bodyMesh, globalShift, alignment, view, clock);
        OpenMatrix4f rest = OpenMatrix4f.createTranslation(0.0F, 2.0F, 0.0F);
        OpenMatrix4f boundLocal = AttachmentMotion.attachmentLocal(bound, table, "Tool_R", new OpenMatrix4f(rest));
        OpenMatrix4f skinLocal = AttachmentMotion.attachmentLocal(skinned, table, "Tool_R", new OpenMatrix4f(rest));
        OpenMatrix4f movedLocal = AttachmentMotion.attachmentLocal(moved, table, "Tool_R", new OpenMatrix4f(rest));
        nearMatrix(boundLocal, skinLocal, "skin matrix does not move the attachment");
        differ(movedLocal, boundLocal, "global frame does move the attachment");
        near(boundLocal.m30, 0.0F, TIGHT, "bind item x");
        near(boundLocal.m31, 2.0F, TIGHT, "bind item y");
        near(movedLocal.m30, 3.0F, TIGHT, "shifted global once");
    }

    private static void checkMotion(BindJointTable table, PlayerSkinMesh bodyMesh, SkinPalette boundPalette) {
        near(AttachmentMotion.HELD_GRIP.m00, 1.0F, TIGHT, "grip scale");
        near(AttachmentMotion.HELD_GRIP.m30, 0.0F, TIGHT, "grip translation");
        ImmutableSkinFrame.Clock clock = clock();
        RenderPlan thirdPlan = plan(RenderPlan.Perspective.THIRD_PERSON, RenderPlan.Hand.NONE);
        ImmutableSkinFrame third = ImmutableSkinFrame.captureThirdPerson(thirdPlan, bodyMesh, boundPalette, new OpenMatrix4f(), clock);
        OpenMatrix4f alignment = OpenMatrix4f.createTranslation(5.0F, 0.0F, 0.0F);
        ImmutableSkinFrame shifted = ImmutableSkinFrame.captureThirdPerson(thirdPlan, bodyMesh, boundPalette, alignment, clock);
        OpenMatrix4f rightRest = PartRest.fromPixels(16.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        OpenMatrix4f leftRest = PartRest.fromPixels(-16.0F, 4.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        OpenMatrix4f rightHeld = AttachmentMotion.heldLocal(third, table, false, rightRest, AttachmentMotion.HELD_GRIP);
        OpenMatrix4f leftHeld = AttachmentMotion.heldLocal(third, table, true, leftRest, AttachmentMotion.HELD_GRIP);
        nearMatrix(rightHeld, rightRest, "right held cancels tool bind");
        nearMatrix(leftHeld, leftRest, "left held uses its own rest");
        differ(leftHeld, rightHeld, "left is not a copy of right");
        near(leftHeld.m00, 1.0F, TIGHT, "left is not mirrored again");
        OpenMatrix4f grip = OpenMatrix4f.createTranslation(0.0F, 0.25F, 0.0F);
        OpenMatrix4f gripped = AttachmentMotion.heldLocal(third, table, false, rightRest, grip);
        nearMatrix(gripped, OpenMatrix4f.mul(rightRest, grip, null), "grip once");
        OpenMatrix4f heldShifted = AttachmentMotion.heldLocal(shifted, table, false, rightRest, AttachmentMotion.HELD_GRIP);
        nearMatrix(heldShifted, OpenMatrix4f.mul(alignment, rightRest, null), "alignment once");
        differ(heldShifted, OpenMatrix4f.mul(alignment, OpenMatrix4f.mul(alignment, rightRest, null), null), "alignment is not squared");
        nearMatrix(AttachmentMotion.chestLocal(third, table), new OpenMatrix4f(), "chest at bind is identity");
        nearMatrix(AttachmentMotion.chestLocal(third, table), AttachmentMotion.chestLocal(third, table), "chest is stable");
        nearMatrix(AttachmentMotion.chestLocal(shifted, table), alignment, "chest alignment once");
        OpenMatrix4f stuck = AttachmentMotion.stuckLocal(third, table, ArmorRegion.TORSO, new OpenMatrix4f(), 0.1F, 0.2F, 0.3F);
        OpenMatrix4f stuckAgain = AttachmentMotion.stuckLocal(third, table, ArmorRegion.TORSO, new OpenMatrix4f(), 0.1F, 0.2F, 0.3F);
        nearMatrix(stuck, stuckAgain, "stuck is deterministic");
        nearMatrix(stuck, OpenMatrix4f.createTranslation(0.1F, 0.2F, 0.3F), "stuck at bind is the local point");
        float[] firstPulls = seededPulls(42L);
        float[] secondPulls = seededPulls(42L);
        for (int pullIndex = 0; pullIndex < firstPulls.length; pullIndex++) {
            near(firstPulls[pullIndex], secondPulls[pullIndex], TIGHT, "entity seed pull");
        }
        absent(SkinAttachmentBridge.acceptsFrame(null, null), "null frame");
        absent(SkinAttachmentBridge.acceptsFrame(third, null), "null entity");
        FirstPersonItemPlan missingFrame = SkinAttachmentBridge.prepareFirstPersonItem(null, null, null, null, null);
        absent(missingFrame.supported(), "missing frame unsupported");
        text(missingFrame.reason(), "frame-missing", "missing frame reason");
        FirstPersonItemPlan missingPlayer = SkinAttachmentBridge.prepareFirstPersonItem(
                third, null, InteractionHand.MAIN_HAND, ItemStack.EMPTY, new OpenMatrix4f());
        absent(missingPlayer.supported(), "missing player unsupported");
        text(missingPlayer.reason(), "player-missing", "missing player reason");
    }

    private static void checkFirstPersonItem(BindJointTable table, PlayerSkinMesh bodyMesh, SkinPalette boundPalette) {
        OpenMatrix4f alignment = OpenMatrix4f.createTranslation(5.0F, 0.0F, 0.0F);
        OpenMatrix4f view = OpenMatrix4f.createTranslation(1.0F, 0.0F, 0.0F);
        OpenMatrix4f itemRest = OpenMatrix4f.createTranslation(0.0F, 2.0F, 0.0F);
        RenderPlan plan = plan(RenderPlan.Perspective.FIRST_PERSON, RenderPlan.Hand.RIGHT);
        ImmutableSkinFrame frame = ImmutableSkinFrame.capture(plan, bodyMesh, boundPalette, alignment, view, clock());
        OpenMatrix4f itemLocal = AttachmentMotion.attachmentLocal(frame, table, "Tool_R", new OpenMatrix4f(itemRest));
        nearMatrix(itemLocal, OpenMatrix4f.mul(alignment, itemRest, null), "item matrix is alignment times neutral");
        Matrix4f copied = JomlCopy.matrix(itemLocal);
        near(copied.m30, 5.0F, TIGHT, "joml translation x");
        near(copied.m31, 2.0F, TIGHT, "joml translation y");
        near(copied.m03, 0.0F, TIGHT, "joml is not transposed");
        int[] calls = new int[1];
        float[] seen = new float[4];
        ItemDisplayContext[] contexts = new ItemDisplayContext[1];
        boolean[] leftFlags = new boolean[1];
        SkinAttachmentBridge.replaceItemDraw((entity, stack, displayContext, leftHand, entryStack, buffers, packedLight) -> {
            calls[0]++;
            seen[0] = entryStack.last().pose().m30;
            seen[1] = entryStack.last().pose().m31;
            seen[2] = entryStack.last().normal().m00;
            seen[3] = entryStack.last().normal().m10;
            contexts[0] = displayContext;
            leftFlags[0] = leftHand;
            same(packedLight, 7, "item light");
        });
        PoseStack pose = new PoseStack();
        pose.translate(4.0F, 0.0F, 0.0F);
        FirstPersonItemPlan blocked = FirstPersonItemPlan.blocked("item-empty");
        SkinAttachmentBridge.renderFirstPersonItem(blocked, pose, null, 7);
        same(calls[0], 0, "unsupported item draws nothing");
        near(pose.last().pose().m30, 4.0F, TIGHT, "unsupported item leaves the stack");
        FirstPersonItemPlan ready = FirstPersonItemPlan.ready(
                frame, null, InteractionHand.MAIN_HAND, ItemStack.EMPTY, itemLocal, false);
        truth(ready.supported(), "ready item");
        text(ready.displayContext().name(), ItemDisplayContext.FIRST_PERSON_RIGHT_HAND.name(), "right display");
        SkinAttachmentBridge.renderFirstPersonItem(ready, pose, null, 7);
        same(calls[0], 1, "item rendered once");
        near(seen[0], 9.0F, TIGHT, "entry plus item x");
        near(seen[1], 2.0F, TIGHT, "item y");
        near(seen[2], 1.0F, TIGHT, "item translation leaves normal x");
        near(seen[3], 0.0F, TIGHT, "item translation leaves normal shear");
        absent(leftFlags[0], "right hand flag");
        text(contexts[0].name(), ItemDisplayContext.FIRST_PERSON_RIGHT_HAND.name(), "right context");
        near(pose.last().pose().m30, 4.0F, TIGHT, "item pop restores entry");
        OpenMatrix4f leaked = ready.itemMatrix();
        leaked.m30 = 80.0F;
        SkinAttachmentBridge.renderFirstPersonItem(ready, pose, null, 7);
        same(calls[0], 2, "explicit second render");
        near(seen[0], 9.0F, TIGHT, "returned matrix is a copy");
        SkinAttachmentBridge.replaceItemDraw((entity, stack, displayContext, leftHand, entryStack, buffers, packedLight) -> {
            throw new IllegalStateException("stop");
        });
        boolean thrown = false;
        try {
            SkinAttachmentBridge.renderFirstPersonItem(ready, pose, null, 7);
        } catch (IllegalStateException failure) {
            thrown = "stop".equals(failure.getMessage());
        }
        truth(thrown, "drawer exception surfaces");
        near(pose.last().pose().m30, 4.0F, TIGHT, "exception still pops");
        near(pose.last().normal().m00, 1.0F, TIGHT, "exception restores item normal");
        OpenMatrix4f singular = new OpenMatrix4f();
        singular.m00 = 0.0F;
        FirstPersonItemPlan broken = FirstPersonItemPlan.ready(
                frame, null, InteractionHand.MAIN_HAND, ItemStack.EMPTY, singular, false);
        int callsBefore = calls[0];
        expectDiagnostic(() -> SkinAttachmentBridge.renderFirstPersonItem(broken, pose, null, 7), "affine");
        same(calls[0], callsBefore, "singular item does not draw");
        near(pose.last().pose().m30, 4.0F, TIGHT, "singular item restores pose");
        near(pose.last().normal().m00, 1.0F, TIGHT, "singular item restores normal");
        FirstPersonItemPlan left = FirstPersonItemPlan.ready(
                frame, null, InteractionHand.OFF_HAND, ItemStack.EMPTY, itemLocal, true);
        text(left.displayContext().name(), ItemDisplayContext.FIRST_PERSON_LEFT_HAND.name(), "left display");
        truth(left.leftHand(), "left flag stored");
        SkinAttachmentBridge.replaceItemDraw((entity, stack, displayContext, leftHand, entryStack, buffers, packedLight) -> {
            calls[0]++;
            contexts[0] = displayContext;
            leftFlags[0] = leftHand;
        });
        SkinAttachmentBridge.renderFirstPersonItem(left, pose, null, 7);
        same(calls[0], 3, "left item once");
        truth(leftFlags[0], "left hand flag");
        text(contexts[0].name(), ItemDisplayContext.FIRST_PERSON_LEFT_HAND.name(), "left context");
        near(pose.last().pose().m30, 4.0F, TIGHT, "left item restores entry");
    }

    private static void checkVanillaArmor(BindJointTable table, PlayerSkinMesh bodyMesh, SkinPalette boundPalette) {
        int depthBefore = leaseDepth();
        HumanoidModel<?> model = humanoid();
        model.head.x = 7.0F;
        model.head.xScale = 2.0F;
        int drawsBefore = ArmorDraw.drawCount;
        ArmorMeshes.ArmorExtract extracted = ArmorMeshes.extract(model, table, PlayerMeshKind.NORMAL, false, GENERATION);
        same(ArmorDraw.drawCount, drawsBefore, "extract does not draw");
        near(model.head.x, 7.0F, TIGHT, "extract restores translation");
        near(model.head.xScale, 2.0F, TIGHT, "extract restores scale");
        truth(extracted.fresh, "first extract is fresh");
        absent(extracted.mesh.coordinateConversionApplied(), "armor keeps source coordinates");
        text(extracted.mesh.sourceGitPath(), "vanilla-armor", "armor source tag");
        same(extracted.mesh.jointLimit(), table.jointLimit(), "armor joint limit");
        ArmorMeshes.ArmorExtract cached = ArmorMeshes.extract(model, table, PlayerMeshKind.NORMAL, false, GENERATION);
        truth(cached.mesh == extracted.mesh, "same model reuses the mesh");
        absent(cached.fresh, "cache hit is not fresh");
        ArmorMeshes.ArmorExtract slim = ArmorMeshes.extract(model, table, PlayerMeshKind.SLIM, false, GENERATION);
        absent(slim.mesh == extracted.mesh, "slim is a separate mesh");
        ArmorMeshes.ArmorExtract inner = ArmorMeshes.extract(model, table, PlayerMeshKind.NORMAL, true, GENERATION);
        absent(inner.mesh == extracted.mesh, "inner layer is a separate mesh");
        ArmorMeshes.ArmorExtract nextGeneration = ArmorMeshes.extract(model, table, PlayerMeshKind.NORMAL, false, GENERATION + 1L);
        absent(nextGeneration.mesh == extracted.mesh, "generation is part of the key");
        ArmorMeshes.ArmorExtract repeated = ArmorMeshes.extract(model, table, PlayerMeshKind.NORMAL, false, GENERATION);
        truth(repeated.mesh == extracted.mesh, "item id is not part of the key");
        scanArmor(extracted.mesh, table);
        float[] deformed = deformedAt(extracted.mesh, boundPalette, 0);
        near(deformed[0], extracted.mesh.positionX(0), LOOSE, "bind skin keeps source x");
        near(deformed[1], extracted.mesh.positionY(0), LOOSE, "bind skin keeps source y");
        near(deformed[2], extracted.mesh.positionZ(0), LOOSE, "bind skin keeps source z");
        Vec3f witnessed = OpenMatrix4f.transform3v(
                AttachmentTransform.sourceToBody(),
                new Vec3f(deformed[0], deformed[1], deformed[2]),
                null);
        near(witnessed.coordinateX, extracted.witnessModelX, LOOSE, "body map roundtrip x");
        near(witnessed.coordinateY, extracted.witnessModelY, LOOSE, "body map roundtrip y");
        near(witnessed.coordinateZ, extracted.witnessModelZ, LOOSE, "body map roundtrip z");
        RenderPlan thirdPlan = plan(RenderPlan.Perspective.THIRD_PERSON, RenderPlan.Hand.NONE);
        ImmutableSkinFrame frame = ImmutableSkinFrame.captureThirdPerson(
                thirdPlan, bodyMesh, boundPalette, new OpenMatrix4f(), clock());
        Map<Model, PlayerSkinMesh> meshes = new IdentityHashMap<>();
        meshes.put(model, extracted.mesh);
        AttachmentSupport support = AttachmentSupport.of(
                true, "armor-ready", PlayerMeshKind.NORMAL, GENERATION, Map.of(), meshes);
        SkinLease outer = SkinWorkspacePool.currentThread().open(bodyMesh);
        SkinAttachmentBridge.ArmorToken token = SkinAttachmentBridge.openArmorPiece(null, null);
        try {
            ProbeConsumer dye = new ProbeConsumer();
            ProbeConsumer overlay = new ProbeConsumer();
            ProbeConsumer trim = new ProbeConsumer();
            ProbeConsumer glint = new ProbeConsumer();
            PoseStack pose = new PoseStack();
            boolean dyeHandled = ArmorDraw.emit(support, frame, model, pose, dye, 0, 0, 1.0F, 0.0F, 0.0F, 1.0F);
            int depthDuring = leaseDepth();
            boolean overlayHandled = ArmorDraw.emit(support, frame, model, pose, overlay, 0, 0, 1.0F, 1.0F, 1.0F, 1.0F);
            boolean trimHandled = ArmorDraw.emit(support, frame, model, pose, trim, 0, 0, 0.0F, 1.0F, 0.0F, 1.0F);
            boolean glintHandled = ArmorDraw.emit(support, frame, model, pose, glint, 0, 0, 0.0F, 0.0F, 1.0F, 1.0F);
            truth(dyeHandled && overlayHandled && trimHandled && glintHandled, "four material passes handled");
            same(leaseDepth(), depthDuring, "passes share one armor lease");
            same(depthDuring, depthBefore + 2, "body lease stays under the armor lease");
            truth(ArmorDraw.pieceLeaseOpen() && outer.isOpen(), "both leases stay open");
            same(ArmorDraw.drawCount, drawsBefore + 4, "four draws");
            truth(dye.vertices > 0 && dye.vertices == overlay.vertices && overlay.vertices == trim.vertices
                    && trim.vertices == glint.vertices, "passes emit the same consumer count");
            same(dye.redChannel, 255, "dye red");
            same(dye.greenChannel, 0, "dye green");
            same(overlay.redChannel, 255, "overlay red");
            same(overlay.greenChannel, 255, "overlay green");
            same(overlay.blueChannel, 255, "overlay blue");
            same(trim.redChannel, 0, "trim red");
            same(trim.greenChannel, 255, "trim green");
            same(glint.blueChannel, 255, "glint blue");
            same(glint.redChannel, 0, "glint red");
            near(dye.firstX, extracted.witnessModelX, LOOSE, "emitted bind x");
            near(dye.firstY, extracted.witnessModelY, LOOSE, "emitted bind y");
            near(dye.firstZ, extracted.witnessModelZ, LOOSE, "emitted bind z");
            near(overlay.firstX, dye.firstX, TIGHT, "overlay matches dye");
            near(trim.firstX, dye.firstX, TIGHT, "trim matches dye");
            near(glint.firstX, dye.firstX, TIGHT, "glint matches dye");
        } finally {
            token.close();
            if (outer.isOpen()) {
                outer.close();
            }
        }
        same(leaseDepth(), depthBefore, "nested leases released in order");
        absent(ArmorDraw.pieceLeaseOpen(), "piece lease closed");
        int blockedDraws = ArmorDraw.drawCount;
        AttachmentSupport blocked = AttachmentSupport.of(
                false, "armor-model", PlayerMeshKind.NORMAL, GENERATION, Map.of(), Map.of());
        ProbeConsumer unused = new ProbeConsumer();
        SkinAttachmentBridge.ArmorToken blockedToken = SkinAttachmentBridge.openArmorPiece(null, null);
        try {
            expectDiagnostic(
                    () -> ArmorDraw.emit(blocked, frame, model, new PoseStack(), unused, 0, 0, 1.0F, 1.0F, 1.0F, 1.0F),
                    "armor-model");
        } finally {
            blockedToken.close();
        }
        same(unused.vertices, 0, "unsupported piece emits nothing");
        same(ArmorDraw.drawCount, blockedDraws, "unsupported piece does not count a draw");
        absent(ArmorDraw.pieceLeaseOpen(), "rejected piece leaves no lease");
        absent(SkinAttachmentBridge.armorPaletteReason(null, boundPalette) != null, "missing support is not a palette failure");
        absent(SkinAttachmentBridge.armorPaletteReason(blocked, boundPalette) != null, "unsupported preflight keeps its own reason");
        absent(SkinAttachmentBridge.armorPaletteReason(support, boundPalette) != null, "matching palette is ready");
        text(SkinAttachmentBridge.armorPaletteReason(support, null), "palette-missing", "missing palette");
        text(SkinAttachmentBridge.armorPaletteReason(support, oneJointPalette()), "palette", "short palette");
        float[] colorSample = ArmorMeshes.colorProbeSample();
        same((int) colorSample[0], 1, "default color adds no corner");
        near(colorSample[1], 0.25F, TIGHT, "collector keeps texture u");
        near(colorSample[2], 0.75F, TIGHT, "collector keeps texture v");
        near(colorSample[3], 1.0F, TIGHT, "collector keeps normal y");
    }

    private static void checkLifecycle(BindJointTable table, PlayerSkinMesh bodyMesh, SkinPalette boundPalette) {
        int leaseBefore = leaseDepth();
        int holdBefore = LayerHold.depth();
        int pieceBefore = ArmorDraw.depth();
        int stuckBefore = StuckMarks.depth();
        HumanoidModel<?> model = humanoid();
        ArmorMeshes.ArmorExtract extracted = ArmorMeshes.extract(model, table, PlayerMeshKind.NORMAL, false, GENERATION);
        RenderPlan thirdPlan = plan(RenderPlan.Perspective.THIRD_PERSON, RenderPlan.Hand.NONE);
        ImmutableSkinFrame frame = ImmutableSkinFrame.captureThirdPerson(
                thirdPlan, bodyMesh, boundPalette, new OpenMatrix4f(), clock());
        Map<Model, PlayerSkinMesh> meshes = new IdentityHashMap<>();
        meshes.put(model, extracted.mesh);
        AttachmentSupport support = AttachmentSupport.of(
                true, "armor-ready", PlayerMeshKind.NORMAL, GENERATION, Map.of(), meshes);
        String[] stages = {"dye", "overlay", "trim", "glint"};
        for (String stage : stages) {
            int drawsBefore = ArmorDraw.drawCount;
            SkinAttachmentBridge.ArmorToken stageToken = SkinAttachmentBridge.openArmorPiece(null, null);
            ProbeConsumer failing = new ProbeConsumer();
            failing.failOnVertex = true;
            try {
                ArmorDraw.emit(support, frame, model, new PoseStack(), failing, 0, 0, 1.0F, 1.0F, 1.0F, 1.0F);
                throw new AssertionError(stage + " should throw");
            } catch (IllegalStateException failure) {
                text(failure.getMessage(), "pass", stage + " throws during emit");
            } finally {
                stageToken.close();
            }
            same(ArmorDraw.drawCount, drawsBefore, stage + " does not count a draw");
            same(failing.vertices, 0, stage + " emits nothing");
            same(leaseDepth(), leaseBefore, stage + " restores lease depth");
            same(LayerHold.depth(), holdBefore, stage + " restores layer depth");
            same(ArmorDraw.depth(), pieceBefore, stage + " restores piece depth");
        }
        SkinLease bodyLease = SkinWorkspacePool.currentThread().open(bodyMesh);
        SkinAttachmentBridge.ArmorToken dyeToken = SkinAttachmentBridge.openArmorPiece(null, null);
        try {
            ProbeConsumer dye = new ProbeConsumer();
            truth(ArmorDraw.emit(support, frame, model, new PoseStack(), dye, 0, 0, 1.0F, 0.0F, 0.0F, 1.0F), "dye before failure");
            ProbeConsumer overlay = new ProbeConsumer();
            overlay.failOnVertex = true;
            try {
                ArmorDraw.emit(support, frame, model, new PoseStack(), overlay, 0, 0, 1.0F, 1.0F, 1.0F, 1.0F);
                throw new AssertionError("overlay should throw");
            } catch (IllegalStateException failure) {
                text(failure.getMessage(), "pass", "overlay throws after dye");
            }
            truth(bodyLease.isOpen(), "body lease stays open after armor failure");
            same(leaseDepth(), leaseBefore + 2, "failed overlay still holds the armor lease until close");
        } finally {
            dyeToken.close();
            if (bodyLease.isOpen()) {
                bodyLease.close();
            }
        }
        same(leaseDepth(), leaseBefore, "dye failure closes only the armor lease");
        SkinAttachmentBridge.ArmorToken outerToken = SkinAttachmentBridge.openArmorPiece(null, null);
        try {
            truth(ArmorDraw.emit(support, frame, model, new PoseStack(), new ProbeConsumer(), 0, 0, 1.0F, 1.0F, 1.0F, 1.0F), "outer piece draws");
            SkinAttachmentBridge.ArmorToken innerToken = SkinAttachmentBridge.openArmorPiece(null, null);
            try {
                truth(ArmorDraw.emit(support, frame, model, new PoseStack(), new ProbeConsumer(), 0, 0, 1.0F, 1.0F, 1.0F, 1.0F), "inner piece draws");
                same(leaseDepth(), leaseBefore + 2, "nested pieces keep two armor leases");
                same(ArmorDraw.depth(), pieceBefore + 2, "nested piece depth");
                boolean rejected = false;
                try {
                    outerToken.close();
                } catch (SkinDiagnostic diagnostic) {
                    rejected = "armor-order".equals(diagnostic.reasonCode());
                }
                truth(rejected, "closing the outer piece does not pop the inner");
                same(ArmorDraw.depth(), pieceBefore + 2, "rejected outer close keeps both pieces");
                same(leaseDepth(), leaseBefore + 2, "rejected outer close keeps both leases");
            } finally {
                innerToken.close();
            }
            truth(ArmorDraw.pieceLeaseOpen(), "outer lease remains after the inner closes");
            same(ArmorDraw.depth(), pieceBefore + 1, "inner close leaves the outer piece");
            same(leaseDepth(), leaseBefore + 1, "inner close leaves the outer lease");
        } finally {
            outerToken.close();
        }
        same(leaseDepth(), leaseBefore, "nested armor leases closed in order");
        same(LayerHold.depth(), holdBefore, "nested armor holds closed in order");
        SkinPalette shortPalette = oneJointPalette();
        ImmutableSkinFrame shortFrame = ImmutableSkinFrame.captureThirdPerson(
                thirdPlan, bodyMesh, shortPalette, new OpenMatrix4f(), clock());
        int shortDraws = ArmorDraw.drawCount;
        SkinAttachmentBridge.ArmorToken shortToken = SkinAttachmentBridge.openArmorPiece(null, null);
        try {
            expectDiagnostic(
                    () -> ArmorDraw.emit(support, shortFrame, model, new PoseStack(), new ProbeConsumer(), 0, 0, 1.0F, 1.0F, 1.0F, 1.0F),
                    "palette");
        } finally {
            shortToken.close();
        }
        same(ArmorDraw.drawCount, shortDraws, "short palette draws nothing");
        same(leaseDepth(), leaseBefore, "short palette opens no lease");
        SkinAttachmentBridge.ArmorToken again = SkinAttachmentBridge.openArmorPiece(null, null);
        try {
            truth(ArmorDraw.emit(support, frame, model, new PoseStack(), new ProbeConsumer(), 0, 0, 1.0F, 1.0F, 1.0F, 1.0F), "player continues after failure");
        } finally {
            again.close();
        }
        Model foreign = new Model((ResourceLocation location) -> {
            throw new IllegalStateException("material");
        }) {
        };
        SkinAttachmentBridge.ArmorToken foreignToken = SkinAttachmentBridge.openArmorPiece(null, null);
        try {
            expectDiagnostic(
                    () -> ArmorDraw.emit(support, frame, foreign, new PoseStack(), new ProbeConsumer(), 0, 0, 1.0F, 1.0F, 1.0F, 1.0F),
                    "armor-model");
        } finally {
            foreignToken.close();
        }
        HumanoidModel<?> missing = humanoid();
        SkinAttachmentBridge.ArmorToken missingToken = SkinAttachmentBridge.openArmorPiece(null, null);
        try {
            expectDiagnostic(
                    () -> ArmorDraw.emit(support, frame, missing, new PoseStack(), new ProbeConsumer(), 0, 0, 1.0F, 1.0F, 1.0F, 1.0F),
                    "armor-mesh");
        } finally {
            missingToken.close();
        }
        SkinAttachmentBridge.StuckToken outerStuck = SkinAttachmentBridge.openStuck(null, null);
        try {
            SkinAttachmentBridge.StuckToken innerStuck = SkinAttachmentBridge.openStuck(null, null);
            try {
                same(StuckMarks.depth(), stuckBefore + 2, "nested stuck marks");
                same(LayerHold.depth(), holdBefore + 2, "nested stuck holds");
                boolean rejected = false;
                try {
                    outerStuck.close();
                } catch (SkinDiagnostic diagnostic) {
                    rejected = "stuck-order".equals(diagnostic.reasonCode());
                }
                truth(rejected, "closing the outer stuck token keeps the inner");
                same(StuckMarks.depth(), stuckBefore + 2, "rejected stuck close keeps both marks");
                SkinAttachmentBridge.noteStuckPart(null);
                absent(StuckMarks.handled(), "unknown stuck part stays vanilla");
            } finally {
                innerStuck.close();
            }
            same(StuckMarks.depth(), stuckBefore + 1, "inner stuck close leaves the outer mark");
            same(LayerHold.depth(), holdBefore + 1, "inner stuck close leaves the outer hold");
            try {
                throw new IllegalStateException("stuck");
            } catch (IllegalStateException failure) {
                text(failure.getMessage(), "stuck", "stuck exception surfaces");
            }
        } finally {
            outerStuck.close();
        }
        same(StuckMarks.depth(), stuckBefore, "stuck mark depth restored");
        same(LayerHold.depth(), holdBefore, "stuck hold depth restored");
        PoseStack untouched = new PoseStack();
        untouched.translate(2.0F, 0.0F, 0.0F);
        absent(SkinAttachmentBridge.heldReplaces(null, null, null), "held without a frame passes");
        absent(SkinAttachmentBridge.replaceHead(null, null, untouched), "head without a frame passes");
        SkinAttachmentBridge.prependChest(null, untouched);
        absent(SkinAttachmentBridge.replaceStuckRotate(), "stuck rotate without a mark passes");
        absent(SkinAttachmentBridge.replaceStuckPoint(untouched, 1.0F, 2.0F, 3.0F), "stuck point without a mark passes");
        near(untouched.last().pose().m30, 2.0F, TIGHT, "unscoped layers leave the stack");
        untouched.pushPose();
        try {
            OpenMatrix4f broken = new OpenMatrix4f();
            broken.m00 = Float.NaN;
            AffinePose.apply(untouched, broken);
            throw new AssertionError("layer affine should reject NaN");
        } catch (SkinDiagnostic diagnostic) {
            text(diagnostic.reasonCode(), "affine", "layer affine rejects NaN");
        } finally {
            untouched.popPose();
        }
        near(untouched.last().pose().m30, 2.0F, TIGHT, "layer affine failure restores pose");
        near(untouched.last().normal().m00, 1.0F, TIGHT, "layer affine failure restores normal");
        same(leaseDepth(), leaseBefore, "lifecycle leases restored");
        same(LayerHold.depth(), holdBefore, "lifecycle holds restored");
        same(ArmorDraw.depth(), pieceBefore, "lifecycle pieces restored");
    }

    private static void checkAffine() {
        PoseStack roll = new PoseStack();
        AffinePose.apply(roll, OpenMatrix4f.createRotatorDeg(90.0F, Vec3f.Z_AXIS));
        Vector3f turned = new Vector3f(1.0F, 0.0F, 0.0F);
        roll.last().normal().transform(turned);
        near(turned.x, 0.0F, TIGHT, "z90 normal x");
        near(turned.y, 1.0F, TIGHT, "z90 normal y");
        near(turned.z, 0.0F, TIGHT, "z90 normal z");
        near(roll.last().pose().m01, 1.0F, TIGHT, "z90 pose matches the normal");
        PoseStack composed = new PoseStack();
        AffinePose.apply(composed, OpenMatrix4f.createRotatorDeg(90.0F, Vec3f.Y_AXIS));
        AffinePose.apply(composed, OpenMatrix4f.createRotatorDeg(90.0F, Vec3f.Z_AXIS));
        Vector4f ordered = new Vector4f(0.0F, 0.0F, 1.0F, 1.0F);
        composed.last().pose().transform(ordered);
        near(ordered.x, 1.0F, TIGHT, "prefix then roll applies roll first");
        near(ordered.y, 0.0F, TIGHT, "prefix then roll y");
        near(ordered.z, 0.0F, TIGHT, "prefix then roll z");
        Vector3f orderedNormal = new Vector3f(0.0F, 0.0F, 1.0F);
        composed.last().normal().transform(orderedNormal);
        near(orderedNormal.x, 1.0F, TIGHT, "prefix normal matches pose");
        near(orderedNormal.y, 0.0F, LOOSE, "prefix normal y");
        near(orderedNormal.z, 0.0F, TIGHT, "prefix normal z");
        OpenMatrix4f scaled = new OpenMatrix4f();
        scaled.m00 = 2.0F;
        scaled.m10 = 0.5F;
        scaled.m11 = 3.0F;
        scaled.m22 = 4.0F;
        scaled.m30 = 8.0F;
        PoseStack scalePose = new PoseStack();
        AffinePose.apply(scalePose, scaled);
        Vector3f scaledNormal = new Vector3f(1.0F, 1.0F, 0.0F);
        scalePose.last().normal().transform(scaledNormal);
        near(scaledNormal.x, 0.5F, TIGHT, "nonuniform normal x");
        near(scaledNormal.y, 0.25F, TIGHT, "nonuniform normal y");
        near(scaledNormal.z, 0.0F, TIGHT, "nonuniform normal z");
        near(scalePose.last().pose().m30, 8.0F, TIGHT, "nonuniform keeps translation");
        PoseStack shifted = new PoseStack();
        AffinePose.apply(shifted, OpenMatrix4f.createTranslation(4.0F, 5.0F, 6.0F));
        Vector3f shiftNormal = new Vector3f(0.0F, 1.0F, 0.0F);
        shifted.last().normal().transform(shiftNormal);
        near(shiftNormal.x, 0.0F, TIGHT, "translation normal x");
        near(shiftNormal.y, 1.0F, TIGHT, "translation normal y");
        near(shiftNormal.z, 0.0F, TIGHT, "translation normal z");
        near(shifted.last().pose().m30, 4.0F, TIGHT, "translation pose x");
        PoseStack kept = new PoseStack();
        kept.translate(3.0F, 0.0F, 0.0F);
        float keptPose = kept.last().pose().m30;
        float keptNormal = kept.last().normal().m00;
        OpenMatrix4f missing = new OpenMatrix4f();
        missing.m00 = Float.NaN;
        expectDiagnostic(() -> AffinePose.apply(kept, missing), "affine");
        OpenMatrix4f singular = new OpenMatrix4f();
        singular.m00 = 0.0F;
        expectDiagnostic(() -> AffinePose.apply(kept, singular), "affine");
        expectDiagnostic(() -> AffinePose.apply(kept, null), "affine");
        near(kept.last().pose().m30, keptPose, TIGHT, "rejected affine leaves pose");
        near(kept.last().normal().m00, keptNormal, TIGHT, "rejected affine leaves normal");
        kept.pushPose();
        try {
            AffinePose.apply(kept, OpenMatrix4f.createRotatorDeg(90.0F, Vec3f.Z_AXIS));
            throw new IllegalStateException("stop");
        } catch (IllegalStateException failure) {
            text(failure.getMessage(), "stop", "affine caller throws");
        } finally {
            kept.popPose();
        }
        near(kept.last().pose().m30, keptPose, TIGHT, "affine finally restores pose");
        near(kept.last().normal().m00, keptNormal, TIGHT, "affine finally restores normal");
    }

    private static SkinPalette oneJointPalette() {
        Joint root = new Joint("Root", 0, new OpenMatrix4f());
        Map<String, Joint> joints = new HashMap<>();
        joints.put("Root", root);
        Armature armature = new Armature("palette-probe", 1, root, joints);
        return SkinPalette.fromAlreadySkinned(
                armature,
                new OpenMatrix4f[] {new OpenMatrix4f()},
                new OpenMatrix4f[] {new OpenMatrix4f()});
    }

    private static int occurrences(String text, String token) {
        int count = 0;
        int from = 0;
        while (from <= text.length()) {
            int found = text.indexOf(token, from);
            if (found < 0) {
                return count;
            }
            count++;
            from = found + token.length();
        }
        return count;
    }

    private static void checkGuards() {
        int drawsBefore = ArmorDraw.drawCount;
        Model foreign = new Model((ResourceLocation location) -> {
            throw new IllegalStateException("material");
        }) {
        };
        text(ArmorPreflight.shapeReason(foreign), "armor-model", "foreign model");
        text(ArmorPreflight.shapeReason(null), "armor-model", "missing model");
        absent(ArmorPreflight.shapeReason(humanoid()) != null, "humanoid shape is accepted");
        same(ArmorDraw.drawCount, drawsBefore, "shape check does not draw");
        same(ArmorMeshes.chooseSplits(6), 4, "coarse armor keeps four splits");
        same(ArmorMeshes.chooseSplits(48), 4, "this probe armor keeps four splits");
        expectDiagnostic(() -> ArmorMeshes.chooseSplits(200), "normal-pairs");
    }

    private static void scanArmor(PlayerSkinMesh mesh, BindJointTable table) {
        int headId = table.jointId("Head");
        int torsoId = table.jointId("Torso");
        int chestId = table.jointId("Chest");
        int armId = table.jointId("Arm_R");
        int handId = table.jointId("Hand_R");
        int elbowId = table.jointId("Elbow_R");
        int thighId = table.jointId("Thigh_R");
        int kneeId = table.jointId("Knee_R");
        int leftElbowId = table.jointId("Elbow_L");
        boolean sawHead = false;
        boolean sawTorso = false;
        boolean sawElbow = false;
        boolean sawArmEnd = false;
        boolean sawKnee = false;
        boolean sawLeftElbow = false;
        for (int positionIndex = 0; positionIndex < mesh.positionCount(); positionIndex++) {
            float sum = 0.0F;
            for (int influenceIndex = 0; influenceIndex < mesh.influenceCountAt(positionIndex); influenceIndex++) {
                float weight = mesh.weight(mesh.influenceWeightSlot(positionIndex, influenceIndex));
                truth(weight > 0.0F, "armor weight positive");
                sum += weight;
            }
            near(sum, 1.0F, TIGHT, "armor weight sum");
            if (mesh.influenceCountAt(positionIndex) == 1 && mesh.influenceJoint(positionIndex, 0) == headId) {
                sawHead = true;
            }
            if (meshWeight(mesh, positionIndex, torsoId) > 0.05F && meshWeight(mesh, positionIndex, chestId) > 0.05F) {
                sawTorso = true;
            }
            if (meshWeight(mesh, positionIndex, elbowId) > 0.2F
                    && meshWeight(mesh, positionIndex, armId) > 0.0F
                    && meshWeight(mesh, positionIndex, handId) > 0.0F) {
                sawElbow = true;
            }
            if (meshWeight(mesh, positionIndex, elbowId) < 0.001F && meshWeight(mesh, positionIndex, armId) > 0.9F) {
                sawArmEnd = true;
            }
            if (meshWeight(mesh, positionIndex, kneeId) > 0.2F && meshWeight(mesh, positionIndex, thighId) > 0.0F) {
                sawKnee = true;
            }
            if (meshWeight(mesh, positionIndex, leftElbowId) > 0.05F) {
                sawLeftElbow = true;
            }
        }
        truth(sawHead, "head cube is rigid");
        truth(sawTorso, "torso cube blends chest");
        truth(sawElbow, "right sleeve keeps the elbow");
        truth(sawArmEnd, "right sleeve still has a single-bone end");
        truth(sawKnee, "right leg keeps the knee");
        truth(sawLeftElbow, "left sleeve keeps the elbow");
    }

    private static ArmorWeights.Influences weighAt(
            BindJointTable table,
            ArmorRegion region,
            String startName,
            String endName,
            float parameter) {
        Vec3f point = pointOn(table, startName, endName, parameter);
        return ArmorWeights.weigh(region, point.coordinateX, point.coordinateY, point.coordinateZ, table, 0.0F, 1.0F);
    }

    private static float weightOn(ArmorWeights.Influences influences, int jointId) {
        float total = 0.0F;
        for (int influenceIndex = 0; influenceIndex < influences.jointIds.length; influenceIndex++) {
            if (influences.jointIds[influenceIndex] == jointId) {
                total += influences.weights[influenceIndex];
            }
        }
        return total;
    }

    private static float meshWeight(PlayerSkinMesh mesh, int positionIndex, int jointId) {
        float total = 0.0F;
        for (int influenceIndex = 0; influenceIndex < mesh.influenceCountAt(positionIndex); influenceIndex++) {
            if (mesh.influenceJoint(positionIndex, influenceIndex) == jointId) {
                total += mesh.weight(mesh.influenceWeightSlot(positionIndex, influenceIndex));
            }
        }
        return total;
    }

    private static PlayerSkinMesh bendMesh(
            BindJointTable table,
            ArmorRegion region,
            PlayerMeshPart meshPart,
            String startName,
            String endName,
            String sideName) {
        float sideParameter = ArmorWeights.clamp01(ArmorWeights.along(
                table, startName, endName,
                table.origin(sideName).coordinateX,
                table.origin(sideName).coordinateY,
                table.origin(sideName).coordinateZ));
        float[] parameters = {sideParameter, 0.0F, 1.0F};
        float[] positions = new float[9];
        int[] influenceCounts = new int[3];
        List<Integer> influenceJoints = new ArrayList<>();
        List<Integer> influenceSlots = new ArrayList<>();
        List<Float> weightValues = new ArrayList<>();
        for (int sampleIndex = 0; sampleIndex < parameters.length; sampleIndex++) {
            Vec3f point = pointOn(table, startName, endName, parameters[sampleIndex]);
            positions[sampleIndex * 3] = point.coordinateX;
            positions[sampleIndex * 3 + 1] = point.coordinateY;
            positions[sampleIndex * 3 + 2] = point.coordinateZ;
            ArmorWeights.Influences influences = ArmorWeights.weigh(
                    region, point.coordinateX, point.coordinateY, point.coordinateZ, table, 0.0F, 1.0F);
            influenceCounts[sampleIndex] = influences.jointIds.length;
            for (int influenceIndex = 0; influenceIndex < influences.jointIds.length; influenceIndex++) {
                influenceJoints.add(influences.jointIds[influenceIndex]);
                influenceSlots.add(weightValues.size());
                weightValues.add(influences.weights[influenceIndex]);
            }
        }
        float[] weights = new float[weightValues.size()];
        int[] joints = new int[influenceJoints.size()];
        int[] slots = new int[influenceSlots.size()];
        for (int weightIndex = 0; weightIndex < weightValues.size(); weightIndex++) {
            weights[weightIndex] = weightValues.get(weightIndex);
            joints[weightIndex] = influenceJoints.get(weightIndex);
            slots[weightIndex] = influenceSlots.get(weightIndex);
        }
        int[][] partCorners = new int[PlayerMeshPart.values().length][];
        partCorners[meshPart.ordinal()] = new int[] {0, 0, 0, 1, 1, 0, 2, 2, 0};
        return PlayerSkinMesh.compose(
                PlayerMeshKind.NORMAL,
                table.jointLimit(),
                positions,
                new float[] {0.0F, 1.0F, 0.0F},
                new float[] {0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F},
                weights,
                influenceCounts,
                joints,
                slots,
                partCorners,
                false,
                "attachment-bend",
                "",
                false);
    }

    private static float[] deformedAt(PlayerSkinMesh mesh, SkinPalette palette, int positionIndex) {
        SkinLease lease = SkinWorkspacePool.currentThread().open(mesh);
        try {
            CpuSkinner.deform(mesh, palette, lease);
            float[] spot = new float[3];
            CpuSkinner.copyDeformed(lease, positionIndex, spot);
            return spot;
        } finally {
            lease.close();
        }
    }

    private static SkinPalette palette(
            Armature armature,
            BindJointTable table,
            String shiftedJoint,
            float shiftAmount,
            boolean shiftGlobal) {
        int jointCount = armature.getJointNumber();
        OpenMatrix4f[] globals = new OpenMatrix4f[jointCount];
        OpenMatrix4f[] skins = new OpenMatrix4f[jointCount];
        for (int jointIndex = 0; jointIndex < jointCount; jointIndex++) {
            String jointName = armature.searchJointById(jointIndex).getName();
            globals[jointIndex] = table.global(jointName);
            skins[jointIndex] = new OpenMatrix4f();
        }
        if (shiftedJoint != null) {
            int shiftedId = table.jointId(shiftedJoint);
            OpenMatrix4f shift = OpenMatrix4f.createTranslation(shiftAmount, 0.0F, shiftGlobal ? 0.0F : shiftAmount);
            if (shiftGlobal) {
                globals[shiftedId] = OpenMatrix4f.mul(globals[shiftedId], shift, null);
            } else {
                skins[shiftedId] = shift;
            }
        }
        return SkinPalette.fromAlreadySkinned(armature, globals, skins);
    }

    private static Vec3f pointOn(BindJointTable table, String startName, String endName, float parameter) {
        Vec3f start = table.origin(startName);
        Vec3f end = table.origin(endName);
        return new Vec3f(
                start.coordinateX + (end.coordinateX - start.coordinateX) * parameter,
                start.coordinateY + (end.coordinateY - start.coordinateY) * parameter,
                start.coordinateZ + (end.coordinateZ - start.coordinateZ) * parameter);
    }

    private static Armature syntheticArmature() {
        Joint root = new Joint("Root", 0, new OpenMatrix4f());
        Map<String, Joint> joints = new HashMap<>();
        joints.put("Root", root);
        List<Joint> children = new ArrayList<>();
        for (int jointIndex = 1; jointIndex < MeshLimits.JOINT_NAMES.length; jointIndex++) {
            String jointName = MeshLimits.JOINT_NAMES[jointIndex];
            float[] place = place(jointName);
            Joint joint = new Joint(jointName, jointIndex, OpenMatrix4f.createTranslation(place[0], place[1], place[2]));
            joints.put(jointName, joint);
            children.add(joint);
        }
        root.addSubJoints(children.toArray(Joint[]::new));
        return new Armature("attachment-probe", joints.size(), root, joints);
    }

    private static float[] place(String jointName) {
        if ("Thigh_R".equals(jointName)) {
            return new float[] {0.12F, 0.70F, 0.0F};
        }
        if ("Leg_R".equals(jointName)) {
            return new float[] {0.12F, 0.20F, 0.0F};
        }
        if ("Knee_R".equals(jointName)) {
            return new float[] {0.22F, 0.45F, 0.0F};
        }
        if ("Thigh_L".equals(jointName)) {
            return new float[] {-0.12F, 0.70F, 0.0F};
        }
        if ("Leg_L".equals(jointName)) {
            return new float[] {-0.12F, 0.20F, 0.0F};
        }
        if ("Knee_L".equals(jointName)) {
            return new float[] {-0.22F, 0.45F, 0.0F};
        }
        if ("Torso".equals(jointName)) {
            return new float[] {0.0F, 1.00F, 0.0F};
        }
        if ("Chest".equals(jointName)) {
            return new float[] {0.0F, 1.35F, 0.0F};
        }
        if ("Head".equals(jointName)) {
            return new float[] {0.0F, 1.60F, 0.0F};
        }
        if ("Shoulder_R".equals(jointName)) {
            return new float[] {0.20F, 1.40F, 0.0F};
        }
        if ("Arm_R".equals(jointName)) {
            return new float[] {0.35F, 1.40F, 0.0F};
        }
        if ("Hand_R".equals(jointName)) {
            return new float[] {0.35F, 0.75F, 0.0F};
        }
        if ("Tool_R".equals(jointName)) {
            return new float[] {0.35F, 0.60F, 0.10F};
        }
        if ("Elbow_R".equals(jointName)) {
            return new float[] {0.55F, 1.05F, 0.0F};
        }
        if ("Shoulder_L".equals(jointName)) {
            return new float[] {-0.20F, 1.40F, 0.0F};
        }
        if ("Arm_L".equals(jointName)) {
            return new float[] {-0.35F, 1.40F, 0.0F};
        }
        if ("Hand_L".equals(jointName)) {
            return new float[] {-0.35F, 0.75F, 0.0F};
        }
        if ("Tool_L".equals(jointName)) {
            return new float[] {-0.35F, 0.60F, -0.10F};
        }
        if ("Elbow_L".equals(jointName)) {
            return new float[] {-0.55F, 1.05F, 0.0F};
        }
        throw new IllegalArgumentException(jointName);
    }

    private static int indexOf(String jointName) {
        for (int jointIndex = 0; jointIndex < MeshLimits.JOINT_NAMES.length; jointIndex++) {
            if (MeshLimits.JOINT_NAMES[jointIndex].equals(jointName)) {
                return jointIndex;
            }
        }
        throw new IllegalArgumentException(jointName);
    }

    private static PlayerSkinMesh bodyMesh() {
        int[] headCorners = {0, 0, 0, 0, 0, 0, 0, 0, 0};
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
                false);
    }

    private static HumanoidModel<?> humanoid() {
        ModelPart head = part(cube(0, 0, -4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F));
        ModelPart hat = part();
        ModelPart body = part(cube(16, 16, -4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F));
        ModelPart rightArm = part(
                cube(40, 16, -8.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                cube(40, 16, -8.0F, 7.2F, -2.0F, 4.0F, 0.4F, 4.0F));
        ModelPart leftArm = part(cube(32, 48, 4.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F));
        ModelPart rightLeg = part(
                cube(0, 16, -2.0F, 8.0F, -2.0F, 4.0F, 16.0F, 4.0F),
                cube(0, 16, -2.0F, 16.8F, -2.0F, 4.0F, 0.4F, 4.0F));
        ModelPart leftLeg = part(cube(16, 48, -2.0F, 8.0F, -2.0F, 4.0F, 16.0F, 4.0F));
        Map<String, ModelPart> children = new HashMap<>();
        children.put("head", head);
        children.put("hat", hat);
        children.put("body", body);
        children.put("right_arm", rightArm);
        children.put("left_arm", leftArm);
        children.put("right_leg", rightLeg);
        children.put("left_leg", leftLeg);
        HumanoidModel<?> model = new HumanoidModel<>(new ModelPart(List.of(), children));
        model.head.visible = true;
        model.hat.visible = true;
        model.body.visible = true;
        model.rightArm.visible = true;
        model.leftArm.visible = true;
        model.rightLeg.visible = true;
        model.leftLeg.visible = true;
        return model;
    }

    private static ModelPart part(ModelPart.Cube... cubes) {
        return new ModelPart(List.of(cubes), Map.of());
    }

    private static ModelPart.Cube cube(
            int textureU,
            int textureV,
            float originX,
            float originY,
            float originZ,
            float sizeX,
            float sizeY,
            float sizeZ) {
        return new ModelPart.Cube(
                textureU,
                textureV,
                originX,
                originY,
                originZ,
                sizeX,
                sizeY,
                sizeZ,
                0.0F,
                0.0F,
                0.0F,
                false,
                64.0F,
                32.0F,
                EnumSet.allOf(Direction.class));
    }

    private static RenderPlan plan(RenderPlan.Perspective perspective, RenderPlan.Hand hand) {
        return RenderPlan.of(
                RenderPlan.Mode.SKINNED,
                OWNER,
                7,
                perspective,
                hand,
                PlayerMeshKind.NORMAL,
                GENERATION,
                "attachment-test");
    }

    private static ImmutableSkinFrame.Clock clock() {
        return ImmutableSkinFrame.Clock.of("attachment-probe", 1L, 0.0F, 0.0F, 0);
    }

    private static float[] seededPulls(long seed) {
        RandomSource random = RandomSource.create(seed);
        return new float[] {random.nextFloat(), random.nextFloat(), random.nextFloat()};
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
            text(diagnostic.reasonCode(), reasonCode, "diagnostic");
            return;
        }
        throw new AssertionError("missing diagnostic " + reasonCode);
    }

    private static void near(float actual, float expected, float tolerance, String label) {
        if (!Float.isFinite(actual) || Math.abs(actual - expected) > tolerance) {
            throw new AssertionError(label + " expected " + expected + " but got " + actual);
        }
        checks++;
    }

    private static void nearMatrix(OpenMatrix4f actual, OpenMatrix4f expected, String label) {
        float[] actualElements = elements(actual);
        float[] expectedElements = elements(expected);
        for (int elementIndex = 0; elementIndex < actualElements.length; elementIndex++) {
            near(actualElements[elementIndex], expectedElements[elementIndex], TIGHT, label + " e" + elementIndex);
        }
    }

    private static void differ(OpenMatrix4f actual, OpenMatrix4f unexpected, String label) {
        float[] actualElements = elements(actual);
        float[] unexpectedElements = elements(unexpected);
        for (int elementIndex = 0; elementIndex < actualElements.length; elementIndex++) {
            if (Float.isFinite(actualElements[elementIndex])
                    && Math.abs(actualElements[elementIndex] - unexpectedElements[elementIndex]) > TIGHT) {
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

    private static void text(String actual, String expected, String label) {
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError(label + " expected " + expected + " but got " + actual);
        }
        checks++;
    }

    private static void truth(boolean actual, String label) {
        if (!actual) {
            throw new AssertionError(label);
        }
        checks++;
    }

    private static void absent(boolean actual, String label) {
        if (actual) {
            throw new AssertionError(label);
        }
        checks++;
    }

    private static final class ProbeConsumer implements VertexConsumer {
        int vertices;
        boolean failOnVertex;
        int redChannel = -1;
        int greenChannel = -1;
        int blueChannel = -1;
        float firstX;
        float firstY;
        float firstZ;
        private float pendingX;
        private float pendingY;
        private float pendingZ;
        private boolean started;

        @Override
        public VertexConsumer vertex(double positionX, double positionY, double positionZ) {
            if (this.failOnVertex) {
                throw new IllegalStateException("pass");
            }
            this.pendingX = (float) positionX;
            this.pendingY = (float) positionY;
            this.pendingZ = (float) positionZ;
            return this;
        }

        @Override
        public VertexConsumer color(int redAmount, int greenAmount, int blueAmount, int alphaAmount) {
            this.redChannel = redAmount;
            this.greenChannel = greenAmount;
            this.blueChannel = blueAmount;
            return this;
        }

        @Override
        public void defaultColor(int redAmount, int greenAmount, int blueAmount, int alphaAmount) {
        }

        @Override
        public void unsetDefaultColor() {
        }

        @Override
        public VertexConsumer uv(float textureU, float textureV) {
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int overlayU, int overlayV) {
            return this;
        }

        @Override
        public VertexConsumer uv2(int lightU, int lightV) {
            return this;
        }

        @Override
        public VertexConsumer normal(float normalX, float normalY, float normalZ) {
            return this;
        }

        @Override
        public void endVertex() {
            if (!this.started) {
                this.firstX = this.pendingX;
                this.firstY = this.pendingY;
                this.firstZ = this.pendingZ;
                this.started = true;
            }
            this.vertices++;
        }
    }
}
