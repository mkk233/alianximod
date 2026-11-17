package com.bslx.XueXiMod.visual.liaoli;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;

/** 燎里 v6 的方向、相位和贴图拷贝。站立八剑尖朝下、柄近尖远。不启动游戏。 */
public final class LiaoliV6LayoutCheck {
    private static int failures;

    private LiaoliV6LayoutCheck() {
    }

    public static void main(String[] programArguments) {
        checkFan();
        checkFlames();
        checkThrust();
        checkRings();
        checkReveal();
        checkCopies();
        checkSources();
        if (failures != 0) {
            System.err.println("liaoli v6 layout failures=" + failures);
            System.exit(1);
        }
        System.out.println("liaoli v6 layout check passed");
    }

    private static void checkFan() {
        expect(LiaoliV6Layout.SWORD_COUNT == 8, "eight swords");
        LiaoliV6Layout.Mark backCenter = LiaoliV6Layout.flameStart();
        int swordIndex = 0;
        while (swordIndex < LiaoliV6Layout.SWORD_COUNT) {
            LiaoliV6Layout.Mark hilt = LiaoliV6Layout.swordHilt(swordIndex);
            LiaoliV6Layout.Mark tip = LiaoliV6Layout.swordTip(swordIndex);
            expect(tip.rise() < hilt.rise(), "tip points down " + swordIndex);
            expect(hilt.rise() - tip.rise() > 0.4d, "tip drops away from the hilt " + swordIndex);
            expect(separation(backCenter, hilt) < separation(backCenter, tip), "hilt nearer the back than the tip " + swordIndex);
            expect(hilt.forward() < 0.0d && tip.forward() < 0.0d, "sword stays behind " + swordIndex);
            int mirror = LiaoliV6Layout.SWORD_COUNT - 1 - swordIndex;
            LiaoliV6Layout.Mark mirrorHilt = LiaoliV6Layout.swordHilt(mirror);
            LiaoliV6Layout.Mark mirrorTip = LiaoliV6Layout.swordTip(mirror);
            expect(close(hilt.lateral(), -mirrorHilt.lateral()), "mirror lateral " + swordIndex);
            expect(close(hilt.rise(), mirrorHilt.rise()), "mirror rise " + swordIndex);
            expect(close(hilt.forward(), mirrorHilt.forward()), "mirror depth " + swordIndex);
            expect(close(tip.lateral(), -mirrorTip.lateral()), "mirror tip lateral " + swordIndex);
            expect(close(tip.rise(), mirrorTip.rise()), "mirror tip rise " + swordIndex);
            expect(close(tip.forward(), mirrorTip.forward()), "mirror tip depth " + swordIndex);
            expect(LiaoliV6Layout.mirrorPair(swordIndex) == LiaoliV6Layout.mirrorPair(mirror), "shared phase " + swordIndex);
            expect(close(LiaoliV6Layout.fanDegrees(swordIndex) + LiaoliV6Layout.fanDegrees(mirror), -180.0d), "mirror angle " + swordIndex);
            swordIndex += 1;
        }
        expect(LiaoliV6Layout.flipFlame(3) == false && LiaoliV6Layout.flipFlame(4), "one side flipped");
    }

    private static void checkFlames() {
        LiaoliV6Layout.Mark start = LiaoliV6Layout.flameStart();
        expect(close(start.lateral(), 0.0d), "flame starts on center line");
        expect(start.forward() < 0.0d, "flame starts behind");
        int swordIndex = 0;
        while (swordIndex < LiaoliV6Layout.SWORD_COUNT) {
            LiaoliV6Layout.Mark hilt = LiaoliV6Layout.swordHilt(swordIndex);
            LiaoliV6Layout.Mark tip = LiaoliV6Layout.swordTip(swordIndex);
            LiaoliV6Layout.Mark end = LiaoliV6Layout.flameEnd(swordIndex);
            expect(separation(start, hilt) < separation(start, tip), "flame root is nearer the hilt than the tip " + swordIndex);
            expect(!close(start.lateral(), tip.lateral()) || !close(start.rise(), tip.rise()), "flame origin is not the tip " + swordIndex);
            expect(progressAlong(hilt, tip, end) > 1.0d, "flame continues past the tip " + swordIndex);
            swordIndex += 1;
        }
    }

    private static void checkThrust() {
        LiaoliV6Layout.Mark hilt = LiaoliV6Layout.thrustHilt();
        LiaoliV6Layout.Mark tip = LiaoliV6Layout.thrustTip();
        LiaoliV6Layout.Mark nearTip = LiaoliV6Layout.streamNearTip();
        LiaoliV6Layout.Mark rear = LiaoliV6Layout.streamRear();
        expect(tip.forward() > hilt.forward() && tip.forward() > 0.0d, "held sword points forward");
        expect(rear.forward() < nearTip.forward(), "stream texture axis points backward");
        expect(rear.forward() < 0.0d && nearTip.forward() > 0.0d, "stream runs from the tip side to behind the player");
    }

    private static void checkRings() {
        expect(LiaoliV6Layout.RING_COUNT == 3, "three rings");
        double first = LiaoliV6Layout.ringProgress(0.0d, 0);
        double second = LiaoliV6Layout.ringProgress(0.0d, 1);
        double third = LiaoliV6Layout.ringProgress(0.0d, 2);
        expect(close(first, 0.0d) && close(second, 1.0d / 3.0d) && close(third, 2.0d / 3.0d), "phase offset");
        expect(LiaoliV6Layout.ringRadius(0.0d) < LiaoliV6Layout.ringRadius(1.0d), "radius grows");
        expect(LiaoliV6Layout.ringCenter(0.0d).forward() > LiaoliV6Layout.ringCenter(1.0d).forward(), "center moves backward");
        expect(LiaoliV6Layout.ringCenter(1.0d).forward() < 0.0d, "ring stays behind");
    }

    private static void checkReveal() {
        expect(close(LiaoliV6Layout.hotReach(0.0d), 0.0d), "cold at the start");
        expect(close(LiaoliV6Layout.hotReach(12.0d), 1.0d), "hot after ignition");
        expect(LiaoliV6Layout.hotReach(6.0d) > LiaoliV6Layout.hotReach(2.0d), "gold spreads");
        LiaoliV6Layout.Mark hilt = LiaoliV6Layout.swordHilt(0);
        LiaoliV6Layout.Mark tip = LiaoliV6Layout.swordTip(0);
        LiaoliV6Layout.Mark early = LiaoliV6Layout.between(hilt, tip, LiaoliV6Layout.hotReach(3.0d));
        double traveled = progressAlong(hilt, tip, early);
        expect(traveled > 0.0d && traveled < 1.0d, "gold stays on the blade");
        expect(separation(hilt, early) < separation(early, tip), "gold stays on the handle side first");
    }

    private static void checkCopies() {
        String[] names = {
                "sword_unlit.png", "sword_hot.png", "back_ignite.png", "back_flow.png",
                "thrust_backflow.png", "rear_ring.png"
        };
        Path archive = Path.of("docs/资源库/特效/火焰/燎里_v6");
        Path game = Path.of("src/main/resources/assets/xueximod/textures/effect/liaoli_v6");
        for (String name : names) {
            expect(sameHash(archive.resolve(name), game.resolve(name)), "hash " + name);
        }
        expect(Files.exists(archive.resolve("preview_front.png")), "archive keeps the preview");
        expect(!Files.exists(game.resolve("preview_front.png")), "game folder has no front preview");
        expect(!Files.exists(game.resolve("preview_oblique.png")), "game folder has no oblique preview");
        try {
            List<Path> copied = Files.list(game).toList();
            expect(copied.size() == names.length, "only the six transparent sheets");
        } catch (Exception failure) {
            expect(false, "list game textures");
        }
        expect(pngSize(game.resolve("sword_unlit.png"), 64, 20), "unlit size");
        expect(pngSize(game.resolve("sword_hot.png"), 64, 20), "hot size");
        expect(pngSize(game.resolve("back_ignite.png"), 128, 576), "ignite strip");
        expect(pngSize(game.resolve("back_flow.png"), 128, 1152), "flow strip");
        expect(pngSize(game.resolve("thrust_backflow.png"), 128, 1536), "stream strip");
        expect(pngSize(game.resolve("rear_ring.png"), 96, 2304), "ring strip");
    }

    private static void checkSources() {
        String renderer = read("src/main/java/com/bslx/XueXiMod/client/LiaoliV6Renderer.java");
        String chain = read("src/main/java/com/bslx/XueXiMod/network/SkillUsePacket.java");
        String effect = read("src/main/java/com/bslx/XueXiMod/shu/technique/effects/LiaoliEffect.java");
        expect(renderer != null && !renderer.contains("fire_liaoli") && !renderer.contains("preview_front"), "renderer has no id or preview");
        expect(chain != null && !chain.contains("LiaoliV6"), "public cast chain is unchanged for this visual");
        expect(effect != null && effect.contains("LiaoliV6Layout.VISUAL") && !effect.contains("spec.visual = 4"), "thrust uses the v6 visual");
        expect(renderer != null && renderer.contains("streamRear") && renderer.contains("streamNearTip"), "stream keeps the backward axis");
    }

    private static boolean sameHash(Path left, Path right) {
        String leftHash = sha256(left);
        String rightHash = sha256(right);
        return leftHash != null && leftHash.equals(rightHash);
    }

    private static String sha256(Path path) {
        try {
            byte[] body = Files.readAllBytes(path);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(body);
            StringBuilder builder = new StringBuilder();
            for (byte piece : digest) {
                builder.append(String.format("%02X", piece));
            }
            return builder.toString();
        } catch (Exception failure) {
            return null;
        }
    }

    private static boolean pngSize(Path path, long expectedWidth, long expectedHeight) {
        try {
            byte[] body = Files.readAllBytes(path);
            if (body.length < 26 || body[25] != 6) {
                return false;
            }
            long width = be32(body, 16);
            long height = be32(body, 20);
            return width == expectedWidth && height == expectedHeight;
        } catch (Exception failure) {
            return false;
        }
    }

    private static long be32(byte[] body, int offset) {
        return ((long) (body[offset] & 255) << 24)
                | ((long) (body[offset + 1] & 255) << 16)
                | ((long) (body[offset + 2] & 255) << 8)
                | (body[offset + 3] & 255);
    }

    private static String read(String relative) {
        try {
            return Files.readString(Path.of(relative), StandardCharsets.UTF_8);
        } catch (Exception failure) {
            return null;
        }
    }

    private static double separation(LiaoliV6Layout.Mark left, LiaoliV6Layout.Mark right) {
        double across = left.lateral() - right.lateral();
        double upward = left.rise() - right.rise();
        double ahead = left.forward() - right.forward();
        return Math.sqrt(across * across + upward * upward + ahead * ahead);
    }

    /** 样本在柄到尖方向上的比例。大于 1 表示已经越过剑尖。 */
    private static double progressAlong(LiaoliV6Layout.Mark origin, LiaoliV6Layout.Mark toward, LiaoliV6Layout.Mark sample) {
        double across = toward.lateral() - origin.lateral();
        double upward = toward.rise() - origin.rise();
        double ahead = toward.forward() - origin.forward();
        double lengthSquared = across * across + upward * upward + ahead * ahead;
        if (lengthSquared < 1.0e-8d) {
            return 0.0d;
        }
        double sampleAcross = sample.lateral() - origin.lateral();
        double sampleUp = sample.rise() - origin.rise();
        double sampleAhead = sample.forward() - origin.forward();
        return (sampleAcross * across + sampleUp * upward + sampleAhead * ahead) / lengthSquared;
    }

    private static boolean close(double left, double right) {
        return Double.isFinite(left) && Double.isFinite(right) && Math.abs(left - right) < 1.0e-6d;
    }

    private static void expect(boolean passed, String label) {
        if (!passed) {
            failures += 1;
            System.err.println("FAIL " + label);
        }
    }
}
