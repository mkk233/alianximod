package com.bslx.XueXiMod.client.action.skinned;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.bslx.XueXiMod.animation.core.Joint;
import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.model.Armature;
import com.bslx.XueXiMod.client.action.mesh.CornerSink;
import com.bslx.XueXiMod.client.action.mesh.PartVisibility;
import com.bslx.XueXiMod.client.action.mesh.PlayerMeshKind;
import com.bslx.XueXiMod.client.action.mesh.PlayerMeshPart;
import com.bslx.XueXiMod.client.action.mesh.PlayerSkinMesh;
import com.bslx.XueXiMod.client.action.mesh.SkinLease;
import com.bslx.XueXiMod.client.action.mesh.SkinPalette;
import com.bslx.XueXiMod.client.action.mesh.SkinWorkspacePool;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;

/**
 * 身体遮罩跟这次 PlayerModel 的 12 个 visible。隐身手不发皮肤，物品仍一次。
 */
public final class SkinVisibilityTailChecks {
    private static int checks;

    private SkinVisibilityTailChecks() {
    }

    public static void main(String[] programArguments) {
        checkSpectatorShowsHeadAndHat();
        checkDefaultShowsAllTwelve();
        checkClosedPartsDoNotLeak();
        checkFirstPersonInvisibleKeepsItem();
        checkForgeGateSkipsBoth();
        System.out.println("SkinVisibilityTailChecks assertions=" + checks);
    }

    private static void checkSpectatorShowsHeadAndHat() {
        PlayerModel<?> model = playerModel();
        model.setAllVisible(false);
        model.head.visible = true;
        model.hat.visible = true;
        model.body.visible = false;
        model.jacket.visible = false;
        model.rightArm.visible = false;
        model.rightSleeve.visible = false;
        model.leftArm.visible = false;
        model.leftSleeve.visible = false;
        model.rightLeg.visible = false;
        model.rightPants.visible = false;
        model.leftLeg.visible = false;
        model.leftPants.visible = false;
        PartVisibility spectator = mask(model, true, true, true, true, true, true);
        expect(spectator, PlayerMeshPart.HEAD, true);
        expect(spectator, PlayerMeshPart.HAT, true);
        expectHidden(spectator, PlayerMeshPart.TORSO, PlayerMeshPart.JACKET, PlayerMeshPart.RIGHT_ARM, PlayerMeshPart.RIGHT_SLEEVE,
                PlayerMeshPart.LEFT_ARM, PlayerMeshPart.LEFT_SLEEVE, PlayerMeshPart.RIGHT_LEG, PlayerMeshPart.RIGHT_PANTS,
                PlayerMeshPart.LEFT_LEG, PlayerMeshPart.LEFT_PANTS);
        PartVisibility hatOff = mask(model, false, true, true, true, true, true);
        expect(hatOff, PlayerMeshPart.HEAD, true);
        expect(hatOff, PlayerMeshPart.HAT, false);
    }

    private static void checkDefaultShowsAllTwelve() {
        PlayerModel<?> model = playerModel();
        model.setAllVisible(true);
        PartVisibility shown = mask(model, true, true, true, true, true, true);
        for (PlayerMeshPart part : PlayerMeshPart.values()) {
            expect(shown, part, true);
        }
    }

    private static void checkClosedPartsDoNotLeak() {
        PlayerModel<?> model = playerModel();
        model.setAllVisible(true);
        model.body.visible = false;
        model.rightSleeve.visible = false;
        model.leftArm.visible = false;
        PartVisibility closed = mask(model, true, false, true, true, true, false);
        expect(closed, PlayerMeshPart.TORSO, false);
        expect(closed, PlayerMeshPart.JACKET, false);
        expect(closed, PlayerMeshPart.RIGHT_ARM, true);
        expect(closed, PlayerMeshPart.RIGHT_SLEEVE, false);
        expect(closed, PlayerMeshPart.LEFT_ARM, false);
        expect(closed, PlayerMeshPart.LEFT_SLEEVE, true);
        expect(closed, PlayerMeshPart.RIGHT_PANTS, false);
        expect(closed, PlayerMeshPart.RIGHT_LEG, true);
        expect(closed, PlayerMeshPart.LEFT_LEG, true);
        expect(closed, PlayerMeshPart.LEFT_PANTS, true);
        expect(closed, PlayerMeshPart.HEAD, true);
        expect(closed, PlayerMeshPart.HAT, true);
        model.jacket.visible = false;
        PartVisibility jacketClosed = mask(model, true, true, true, true, true, true);
        expect(jacketClosed, PlayerMeshPart.JACKET, false);
        expect(jacketClosed, PlayerMeshPart.TORSO, false);
    }

    private static void checkFirstPersonInvisibleKeepsItem() {
        PlayerSkinMesh mesh = armMesh();
        SkinPalette palette = palette();
        SkinLease lease = SkinWorkspacePool.currentThread().open(mesh);
        try {
            int[] itemCalls = new int[1];
            int hiddenCorners = FirstPersonSkinDraw.completeHand(
                    true, true, true, mesh, palette, lease, new OpenMatrix4f(), 0, 0,
                    sink(), sink(), () -> itemCalls[0]++);
            require(hiddenCorners == 0, "隐身皮肤角点应为 0");
            require(itemCalls[0] == 1, "隐身物品应画一次");
            itemCalls[0] = 0;
            int shownCorners = FirstPersonSkinDraw.completeHand(
                    false, true, true, mesh, palette, lease, new OpenMatrix4f(), 0, 0,
                    sink(), sink(), () -> itemCalls[0]++);
            require(shownCorners > 0, "正常皮肤角点应大于 0");
            require(itemCalls[0] == 1, "正常物品应画一次");
        } finally {
            lease.close();
        }
    }

    private static void checkForgeGateSkipsBoth() {
        String mixinText = read("D:/alianximod/src/main/java/com/bslx/XueXiMod/mixin/client/ItemInHandRendererActionMixin.java");
        int handStart = mixinText.indexOf("void xuexiRenderArm");
        int handEnd = mixinText.indexOf("private static PlayerModel");
        require(handStart >= 0 && handEnd > handStart, "手入口还在");
        String handBody = mixinText.substring(handStart, handEnd);
        require(count(handBody, "FirstPersonSkinDraw.draw") == 1, "Forge 取消后不会进入手绘制");
        require(!handBody.contains("renderFirstPersonItem"), "取消时物品桥也不会被这条入口以外调用");
        require(mixinText.contains("renderArmWithItem"), "入口仍是原版手方法");
        String handText = read("D:/alianximod/src/main/java/com/bslx/XueXiMod/client/action/skinned/FirstPersonSkinDraw.java");
        require(count(handText, "renderFirstPersonItem") == 1, "物品桥只调一次");
        require(handText.contains("entitySolid") && handText.contains("entityTranslucent"), "原 RenderType 还在");
        require(handText.contains("player.isInvisible()"), "隐身只跳过手臂皮肤");
        String bodyText = read("D:/alianximod/src/main/java/com/bslx/XueXiMod/client/action/skinned/SkinnedBodyDraw.java");
        require(!bodyText.contains("isInvisible"), "第三人称不因隐身整段跳过");
        require(bodyText.contains("fromModel"), "身体遮罩读模型可见性");
    }

    private static PartVisibility mask(
            PlayerModel<?> model,
            boolean hatShown,
            boolean jacketShown,
            boolean leftSleeveShown,
            boolean rightSleeveShown,
            boolean leftPantsShown,
            boolean rightPantsShown) {
        return SkinnedBodyDraw.fromModel(
                model, hatShown, jacketShown, leftSleeveShown, rightSleeveShown, leftPantsShown, rightPantsShown);
    }

    private static void expect(PartVisibility mask, PlayerMeshPart part, boolean shown) {
        checks++;
        if (mask.shows(part) != shown) {
            throw new AssertionError(part.wireName() + " visible=" + shown);
        }
    }

    private static void expectHidden(PartVisibility mask, PlayerMeshPart... hiddenParts) {
        for (int partIndex = 0; partIndex < hiddenParts.length; partIndex++) {
            expect(mask, hiddenParts[partIndex], false);
        }
    }

    private static PlayerModel<?> playerModel() {
        return new PlayerModel<>(PlayerModel.createMesh(CubeDeformation.NONE, false).getRoot().bake(64, 64), false);
    }

    private static PlayerSkinMesh armMesh() {
        int[][] partCorners = new int[PlayerMeshPart.values().length][];
        partCorners[PlayerMeshPart.RIGHT_ARM.ordinal()] = new int[] {0, 0, 0};
        partCorners[PlayerMeshPart.RIGHT_SLEEVE.ordinal()] = new int[] {0, 0, 0};
        return PlayerSkinMesh.compose(
                PlayerMeshKind.NORMAL, 1,
                new float[] {0.0F, 0.0F, 0.0F},
                new float[] {0.0F, 1.0F, 0.0F},
                new float[] {0.0F, 0.0F},
                new float[] {1.0F},
                new int[] {1},
                new int[] {0},
                new int[] {0},
                partCorners,
                false, "", "", true);
    }

    private static SkinPalette palette() {
        Joint root = new Joint("Root", 0, new OpenMatrix4f());
        Map<String, Joint> joints = new LinkedHashMap<>();
        joints.put("Root", root);
        return SkinPalette.fromAlreadySkinned(
                new Armature("probe-palette", 1, root, joints),
                new OpenMatrix4f[] {new OpenMatrix4f()},
                new OpenMatrix4f[] {new OpenMatrix4f()});
    }

    private static CornerSink sink() {
        return (positionAcross, positionUpward, positionAhead, normalAcross, normalUpward, normalAhead,
                textureAcross, textureAhead, packedLight, overlayIndex) -> {
        };
    }

    private static String read(String path) {
        try {
            return Files.readString(Path.of(path));
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static int count(String text, String token) {
        int found = 0;
        int cursor = 0;
        while (cursor >= 0) {
            cursor = text.indexOf(token, cursor);
            if (cursor < 0) {
                break;
            }
            found++;
            cursor += token.length();
        }
        return found;
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
