package com.bslx.XueXiMod.visual.fire;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

/** 其余五式确认视觉的取样、贴图拷贝和结算边界。不启动游戏。 */
public final class FireVisualLayoutCheck {
    private static int failures;

    private FireVisualLayoutCheck() {
    }

    public static void main(String[] programArguments) {
        checkWave();
        checkSky();
        checkSwap();
        checkFurnace();
        checkCopies();
        checkSources();
        if (failures != 0) {
            System.err.println("fire visual layout failures=" + failures);
            System.exit(1);
        }
        System.out.println("fire visual layout check passed");
    }

    private static void checkWave() {
        expect(FireVisualLayout.FIELD_LIFE_TICKS == 200, "field life stays 200 ticks");
        expect(FireVisualLayout.coveredRadius(6.0d, 0.0d) == 0.0d, "sea starts at the origin");
        expect(close(FireVisualLayout.coveredRadius(6.0d, 6.0d), 3.0d), "sea follows the wave");
        expect(close(FireVisualLayout.coveredRadius(6.0d, 12.0d), 6.0d), "sea reaches the real radius");
        expect(close(FireVisualLayout.coveredRadius(6.0d, 200.0d), 6.0d), "later life does not grow past the radius");
        expect(FireVisualLayout.waveVisible(0.0d) && FireVisualLayout.waveVisible(11.9d), "wave plays through its strip");
        expect(!FireVisualLayout.waveVisible(12.0d) && !FireVisualLayout.waveVisible(200.0d), "wave does not loop for the field life");
        expect(FireVisualLayout.clingScale(0, 1) < FireVisualLayout.clingScale(10, 0), "attached flame strength follows the snapshot");
        expect(FireVisualLayout.burnToken(3, FireVisualLayout.SPREAD) != FireVisualLayout.burnToken(3, FireVisualLayout.SKY),
                "spread and sky stay distinct");
    }

    private static void checkSwap() {
        expect(FireVisualLayout.kindleSustainFrame(10, 0, 10) == 1, "kindle opens on its first sustained frame");
        expect(FireVisualLayout.kindleSustainFrame(10, 3, 1) == 40, "kindle reaches the end of the transferred loop");
        expect(FireVisualLayout.kindleSustainFrame(10, 4, 10) == 1, "kindle loops instead of entering the ember frames");
        expect(FireVisualLayout.kindleEmberFrame(0) == 41 && FireVisualLayout.kindleEmberFrame(11) == 52,
                "kindle ending uses the twelve ember frames");
        expect(FireVisualLayout.kindleEmberFrame(12) == 0, "kindle embers do not repeat");
        expect(FireVisualLayout.bodyLoopFrame(4, 0, 4) == 0 && FireVisualLayout.bodyLoopFrame(4, 1, 1) == 7,
                "fentian loops the first ten body frames");
        expect(FireVisualLayout.bodyLoopFrame(4, 9, 0) == 0, "fentian does not enter the pulse from the loop clock");
        expect(FireVisualLayout.bodyPulseFrame(0) == 10 && FireVisualLayout.bodyPulseFrame(2) == 12,
                "a real fentian beat uses three bright frames");
        expect(FireVisualLayout.bodyPulseFrame(3) < 0, "the bright cue ends after three frames");
        expect(FireVisualLayout.bodyEndFrame(0) == 13 && FireVisualLayout.bodyEndFrame(5) == 18,
                "fentian ending plays six frames once");
        expect(FireVisualLayout.bodyEndFrame(6) < 0, "fentian ending does not loop");
        expect(!FireVisualLayout.KINDLE_SHEET.equals(FireVisualLayout.BODY_FLAME), "the swapped sheets stay separate");
    }

    private static void checkSky() {
        int wait = 4;
        while (wait >= 1) {
            int frame = FireVisualLayout.pulseFrame(1, 4, 0, wait);
            expect(frame >= 1 && frame <= 3, "low seed opening stays in gather, frame " + frame);
            wait -= 1;
        }
        expect(FireVisualLayout.finalPulseFrame(1, 4) == 4, "one segment ends on the first pulse");
        expect(FireVisualLayout.pulseFrame(1, 4, 9, 1) == 4, "low seed never enters the later pulses");
        expect(FireVisualLayout.emberFrame(0) == 41 && FireVisualLayout.emberFrame(11) == 52, "embers are the tail after frame 40");
        expect(FireVisualLayout.emberFrame(12) == 0, "embers stop after twelve frames");
        expect(FireVisualLayout.finalPulseFrame(10, 4) == 40, "full seed reaches the tenth pulse");
        expect(FireVisualLayout.pulseFrame(10, 4, 0, 1) == 3, "full seed still opens with gather");
        expect(FireVisualLayout.pulseFrame(10, 4, 1, 4) == 4, "first peak is frame 4");
        expect(FireVisualLayout.finalPulseFrame(4, 4) == 16, "remainder adds one beat and stops there");
        expect(FireVisualLayout.skyScale(4, 2, 5) == 1.0d, "earlier beats stay full size");
        expect(FireVisualLayout.skyScale(4, 3, 5) < 1.0d, "the tail beat is weaker");
        expect(FireVisualLayout.textureRow(41, 52) == 40, "ember frame 41 is row 40");
    }

    private static void checkFurnace() {
        expect(FireVisualLayout.furnaceLoop(false, false, false) == FireVisualLayout.HEAT_LOOP, "normal uses the low flame");
        expect(FireVisualLayout.furnaceLoop(true, false, false) == FireVisualLayout.OVERDRIVE_LOOP, "entering overdrive is the strong flame");
        expect(FireVisualLayout.furnaceLoop(false, true, false) == FireVisualLayout.OVERDRIVE_LOOP, "upgrade uses the strong flame");
        expect(FireVisualLayout.furnaceLoop(false, true, false) != FireVisualLayout.HEAT_LOOP, "upgrade does not replay heat");
        expect(FireVisualLayout.furnaceLoop(false, false, true) == FireVisualLayout.COOL_ONCE, "cool is a separate one shot");
        expect(FireVisualLayout.coolFrame(0L) == 0, "cool starts at the first frame");
        expect(FireVisualLayout.coolFrame(75L * 16) < 0, "cool stops after sixteen frames");
        expect(FireVisualLayout.loopFrame(75L * 16) == 0 && FireVisualLayout.loopFrame(75L) == 1, "sustain loops every 75ms");
    }

    private static void checkCopies() {
        expect(copied("燎原_v2", "liaoyuan-ground-flame-v2-strip.png", "liaoyuan_v2", "ground_flame.png", 32, 960), "ground flame");
        expect(copied("燎原_v2", "liaoyuan-ground-embers-v2-strip.png", "liaoyuan_v2", "ground_embers.png", 64, 1536), "ground embers");
        expect(copied("燎原_v2", "liaoyuan-spread-wave-v2-strip.png", "liaoyuan_v2", "spread_wave.png", 128, 1536), "spread wave");
        expect(copied("焚天_v2", "fentian-conflagration-v2-strip.png", "fentian_v2", "conflagration.png", 128, 7488), "conflagration");
        expect(copied("炉心三式_v1", "heat_up-v1-strip.png", "furnace_v1", "heat_up.png", 64, 1024), "heat");
        expect(copied("炉心三式_v1", "overdrive-v1-strip.png", "furnace_v1", "overdrive.png", 64, 1024), "overdrive");
        expect(copied("炉心三式_v1", "cool-v1-strip.png", "furnace_v1", "cool.png", 64, 1024), "cool");
        Path liaoyuan = Path.of("src/main/resources/assets/xueximod/textures/effect/liaoyuan_v2");
        Path fentian = Path.of("src/main/resources/assets/xueximod/textures/effect/fentian_v2");
        Path furnace = Path.of("src/main/resources/assets/xueximod/textures/effect/furnace_v1");
        expect(listCount(liaoyuan) == 3 && listCount(fentian) == 1 && listCount(furnace) == 3, "only the transparent strips");
        expect(!Files.exists(liaoyuan.resolve("liaoyuan-ground-sea-preview-v2.png")), "no liaoyuan preview in game");
        expect(!Files.exists(fentian.resolve("fentian-conflagration-preview-v2.png")), "no fentian preview in game");
        expect(!Files.exists(furnace.resolve("remaining-five-review-v1.png")), "no review board in game");
        expect(Files.exists(Path.of("docs/资源库/特效/火焰/燎原_v2/liaoyuan-ground-sea-preview-v2.png")), "archive keeps its preview");
    }

    private static void checkSources() {
        String fields = read("src/main/java/com/bslx/XueXiMod/shu/technique/release/firecast/GroundFireFields.java");
        String fentian = read("src/main/java/com/bslx/XueXiMod/shu/technique/effects/FentianEffect.java");
        String adapter = read("src/main/java/com/bslx/XueXiMod/shu/technique/release/selfstatus/FurnaceSelfStatusCast.java");
        String committer = read("src/main/java/com/bslx/XueXiMod/shu/technique/release/selfstatus/SelfStatusCommit.java");
        String chain = read("src/main/java/com/bslx/XueXiMod/network/SkillUsePacket.java");
        String death = read("src/main/java/com/bslx/XueXiMod/status/fire/furnace/FurnaceEvents.java");
        String client = read("src/main/java/com/bslx/XueXiMod/client/FireVisualClient.java");
        expect(fields != null && fields.contains("BASE_RADIUS = 6.0d") && fields.contains("FIELD_LIFE = 200")
                && fields.contains("GROUND_DAMAGE = 2.0d") && fields.contains("VERTICAL_SLACK = 3.0d")
                && fields.contains("LAYER_HEIGHT = 1.0d"), "liaoyuan numbers unchanged");
        expect(fields != null && !fields.contains("ParticleTypes") && !fields.contains("feetPos")
                && fields.contains("StandSurface.columnTop"), "liaoyuan no longer sprays vanilla flame");
        expect(fentian != null && fentian.contains("BASE_RADIUS = 8.0d") && !fentian.contains("sendParticles")
                && !fentian.contains("ParticleTypes"), "fentian radius unchanged and has no origin flame");
        expect(adapter != null && adapter.contains("ENTERED_OVERDRIVE, UPGRADED -> FireVisualLayout.OVERDRIVE_LOOP")
                        && adapter.contains("COOLED -> FireVisualLayout.COOL_ONCE")
                        && !adapter.contains("ParticleTypes")
                        && !adapter.contains("UPGRADED -> FireVisualLayout.HEAT_LOOP"),
                "upgrade shares overdrive and does not use vanilla particles");
        expect(committer != null && committer.contains("prepared.afterCommit")
                        && !committer.contains("ParticleTypes")
                        && !committer.contains("ENTERED_OVERDRIVE")
                        && !committer.contains("COOL_ONCE"),
                "committer does not own furnace cues");
        expect(chain != null && !chain.contains("FireVisual"), "public cast chain has no visual branch");
        expect(death != null && !death.contains("FireVisual") && !death.contains("COOL_ONCE"), "death does not play cool");
        int spreadAt = client == null ? -1 : client.indexOf("void drawSpread");
        int skyAt = client == null ? -1 : client.indexOf("void drawSky");
        expect(spreadAt >= 0 && skyAt > spreadAt, "spread cling is separate from sky");
        String spreadBody = spreadAt < 0 ? "" : client.substring(spreadAt, skyAt);
        expect(!spreadBody.contains("conflagration") && spreadBody.contains("GROUND_FLAME"), "cling reuses the ground flame");
        expect(client != null && client.contains("KINDLE_SHEET") && client.contains("BODY_FLAME") && !client.contains("preview"), "swapped sheets stay on their tracks");
    }

    private static boolean copied(String archiveFolder, String archiveName, String gameFolder, String gameName,
                                  long width, long height) {
        Path archive = Path.of("docs/资源库/特效/火焰").resolve(archiveFolder).resolve(archiveName);
        Path game = Path.of("src/main/resources/assets/xueximod/textures/effect").resolve(gameFolder).resolve(gameName);
        return sameHash(archive, game) && pngSize(game, width, height);
    }

    private static int listCount(Path folder) {
        try {
            return (int) Files.list(folder).count();
        } catch (Exception failure) {
            return -1;
        }
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
