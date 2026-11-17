package com.bslx.XueXiMod.shu.technique.release.entity;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.UUID;

/** 空间切割静止斩面的时间边界、UUID 去重和三槽布局。不启动游戏。 */
public final class StillSheetChecks {
    private static int checks;

    private StillSheetChecks() {
    }

    public static void main(String[] programArguments) throws Exception {
        Path projectRoot = programArguments.length > 0 ? Path.of(programArguments[0]) : Path.of("").toAbsolutePath();
        checkClock();
        checkCache();
        checkLanes();
        checkSources(projectRoot);
        checkPng(projectRoot);
        System.out.println("StillSheetChecks: " + checks + " assertions");
    }

    private static void checkClock() {
        long birth = 1000L;
        yes(StillSheetClock.frameIndex(birth, 0.0F, StillSheetVisual.UNBORN, 20, 1) < 0, "unborn stays hidden");
        yes(!StillSheetClock.expired(birth, 0.0F, StillSheetVisual.UNBORN, 20, 1), "unborn is not treated as expiry");
        yes(StillSheetClock.frameIndex(birth, 0.0F, birth, 20, 1) == 0, "activation starts at frame 0");
        yes(StillSheetClock.frameIndex(birth + 9L, 0.0F, birth, 20, 1) == 9, "age 9 is frame 9");
        yes(StillSheetClock.frameIndex(birth + 10L, 0.0F, birth, 20, 1) == 10, "age 10 keeps playing after the hit window");
        yes(!StillSheetClock.expired(birth + 10L, 0.0F, birth, 20, 1), "age 10 is not cleaned up");
        yes(StillSheetClock.frameIndex(birth + 18L, 0.0F, birth, 20, 1) == 18, "age 18 is frame 18");
        yes(StillSheetClock.frameIndex(birth + 19L, 0.0F, birth, 20, 1) == 19, "age 19 is frame 19");
        yes(StillSheetClock.frameIndex(birth + 19L, 0.99F, birth, 20, 1) == 19, "age 19.99 stays on frame 19");
        yes(!StillSheetClock.expired(birth + 19L, 0.99F, birth, 20, 1), "frame 19 is not cleaned early");
        yes(StillSheetClock.frameIndex(birth + 20L, 0.0F, birth, 20, 1) < 0, "age 20 is hidden");
        yes(StillSheetClock.expired(birth + 20L, 0.0F, birth, 20, 1), "age 20 is cleaned");
        yes(StillSheetClock.frameIndex(birth + 40L, 0.0F, birth, 20, 1) < 0, "playback does not wrap");
        yes(StillSheetClock.frameIndex(birth - 1L, 0.0F, birth, 20, 1) < 0, "negative age stays hidden");
        yes(!StillSheetClock.expired(birth - 1L, 0.0F, birth, 20, 1), "one tick early is kept");
        yes(!StillSheetClock.stranded(birth - 1L, 0.0F, birth), "one tick early is not stranded");
        yes(StillSheetClock.stranded(birth - 101L, 0.0F, birth), "a far-future birth is stranded");
        near(StillSheetClock.textureV(0, 0.0F, 20), 0.0F, "frame 0 top");
        near(StillSheetClock.textureV(0, 1.0F, 20), 0.05F, "frame 0 bottom");
        near(StillSheetClock.textureV(19, 0.0F, 20), 0.95F, "frame 19 top");
        near(StillSheetClock.textureV(19, 1.0F, 20), 1.0F, "frame 19 bottom");
    }

    private static void checkCache() {
        String worldName = "minecraft:overworld";
        UUID firstId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID secondId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID thirdId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        CohortVisual firstLane = new CohortVisual(0, 3, 0, 3, true);
        CohortVisual secondLane = new CohortVisual(1, 3, 1, 3, true);
        CohortVisual thirdLane = new CohortVisual(2, 3, 2, 3, true);
        StillSheetCache cache = new StillSheetCache();

        yes(!cache.offer(snapshot(firstId, worldName, StillSheetVisual.UNBORN, firstLane)), "unborn snapshot is refused");
        yes(cache.size() == 0, "unborn snapshot is not stored");
        yes(cache.offer(snapshot(firstId, worldName, 1000L, firstLane)), "first entity is stored");
        StillSheetSnapshot refreshed = new StillSheetSnapshot(firstId, worldName, 9.0D, 70.0D, 9.0D, 10.0F, 1.0F, 2.0F,
                5.0D, 3.0D, 0.0D, secondLane, "xueximod:textures/entity/xueximod/dimension_willow_v8.png",
                20, 1, 1005L);
        yes(!cache.offer(refreshed), "same uuid does not restart");
        yes(cache.find(firstId).birthGameTime() == 1000L, "refresh keeps the original birth");
        yes(cache.find(firstId).worldY() == 64.0D, "refresh keeps the original pose");
        yes(cache.find(firstId).cohort().laneIndex() == 0, "refresh keeps the original lane");
        yes(cache.offer(snapshot(secondId, worldName, 1000L, secondLane)), "second entity is stored");
        yes(cache.offer(snapshot(thirdId, worldName, 1000L, thirdLane)), "third entity is stored");
        yes(cache.size() == 3, "same anchor and texture stay three uuids");
        yes(cache.find(firstId) != cache.find(secondId) && cache.find(secondId) != cache.find(thirdId), "snapshots stay distinct");
        yes(cache.dropExpired(1009L, 0.0F) == 0, "age 9 keeps every snapshot");
        yes(cache.dropExpired(1010L, 0.0F) == 0, "age 10 does not cancel the visual");
        yes(cache.size() == 3, "hit-window end leaves the visuals");
        yes(cache.cancel(firstId), "explicit cancel removes one uuid");
        yes(cache.find(firstId) == null, "cancelled uuid is gone");
        yes(cache.size() == 2, "cancel does not remove the other lanes");
        yes(cache.dropExpired(1019L, 0.99F) == 0, "frame 19 remains stored");
        yes(cache.dropExpired(1020L, 0.0F) == 2, "age 20 drops the remaining visuals");
        yes(cache.size() == 0, "cache is empty after expiry");

        yes(cache.offer(snapshot(firstId, worldName, 5000L, firstLane)), "far-future birth can be stored");
        yes(cache.dropExpired(1000L, 0.0F) == 1, "stranded birth is bounded");
        yes(cache.offer(snapshot(secondId, worldName, 1000L, secondLane)), "overworld snapshot stored");
        yes(cache.dropOtherWorlds("minecraft:the_nether") == 1, "world change drops the old identity");
        yes(cache.size() == 0, "old world leaves nothing");
        yes(cache.offer(snapshot(thirdId, worldName, 1000L, thirdLane)), "snapshot before clear");
        cache.clear();
        yes(cache.size() == 0, "disconnect clear is empty");
        noCombat(StillSheetCache.class);
        noCombat(StillSheetClock.class);
        noCombat(StillSheetLayout.class);
        noCombat(StillSheetSnapshot.class);
        noCombat(StillSheetVisual.class);
    }

    private static void checkLanes() {
        CohortVisual firstLane = new CohortVisual(0, 3, 0, 3, true);
        CohortVisual secondLane = new CohortVisual(1, 3, 1, 3, true);
        CohortVisual thirdLane = new CohortVisual(2, 3, 2, 3, true);
        CohortLaneLayout.FaceWindow firstWindow = StillSheetLayout.faceWindow(5.0D, 3.0D, 0.0D, firstLane);
        CohortLaneLayout.FaceWindow secondWindow = StillSheetLayout.faceWindow(5.0D, 3.0D, 0.0D, secondLane);
        CohortLaneLayout.FaceWindow thirdWindow = StillSheetLayout.faceWindow(5.0D, 3.0D, 0.0D, thirdLane);
        yes(separated(firstWindow, secondWindow), "first and second slots do not overlap");
        yes(separated(secondWindow, thirdWindow), "second and third slots do not overlap");
        yes(separated(firstWindow, thirdWindow), "outer slots do not overlap");
        near(secondWindow.riseLow() - firstWindow.riseHigh(), 0.4F, "gap after the first slot");
        near(thirdWindow.riseLow() - secondWindow.riseHigh(), 0.4F, "gap after the second slot");
        near(firstWindow.acrossHigh() - firstWindow.acrossLow(), 5.0F, "long side stays the full face");
        near(secondWindow.acrossHigh() - secondWindow.acrossLow(), 5.0F, "middle slot keeps the long side");
        near(thirdWindow.acrossHigh() - thirdWindow.acrossLow(), 5.0F, "last slot keeps the long side");
        near(firstWindow.riseHigh() - firstWindow.riseLow(), 0.6F, "slot uses the existing draw fraction");
        near(firstWindow.acrossLow(), secondWindow.acrossLow(), "slots share the across start");
        near(secondWindow.acrossHigh(), thirdWindow.acrossHigh(), "slots share the across end");

        CohortLaneLayout.FaceWindow singleWindow = StillSheetLayout.faceWindow(5.0D, 3.0D, 0.0D, CohortVisual.SINGLE);
        CohortVisual dispersedFirst = new CohortVisual(0, 3, 0, 1, false);
        CohortVisual dispersedLast = new CohortVisual(2, 3, 0, 1, false);
        sameWindow(singleWindow, StillSheetLayout.faceWindow(5.0D, 3.0D, 0.0D, dispersedFirst), "dispersed first");
        sameWindow(singleWindow, StillSheetLayout.faceWindow(5.0D, 3.0D, 0.0D, dispersedLast), "dispersed last");
    }

    private static void checkSources(Path projectRoot) throws Exception {
        String effectSource = Files.readString(projectRoot.resolve(
                "src/main/java/com/bslx/XueXiMod/shu/technique/effects/SpaceCutEffect.java"), StandardCharsets.UTF_8);
        yes(effectSource.contains("LIFETIME = 10"), "space cut hit lifetime stays 10");
        yes(effectSource.contains("hitInterval = 0"), "space cut hit interval stays 0");
        yes(effectSource.contains("dimension_willow_v8.png"), "space cut points at the v8 sheet");
        yes(effectSource.contains("StillSheetVisual.once(TEXTURE, 20, 1)"), "visual is 20 frames at one tick");
        yes(!effectSource.contains("lifetime = 20"), "effect does not extend the hit lifetime");

        String entitySource = Files.readString(projectRoot.resolve(
                "src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitEntity.java"), StandardCharsets.UTF_8);
        int releaseStart = entitySource.indexOf("void releaseActivation");
        int applyStart = entitySource.indexOf("void applySpec");
        int placeStart = entitySource.indexOf("void placeAt");
        yes(releaseStart >= 0 && applyStart > releaseStart && placeStart > applyStart, "activation methods stay in place");
        String releaseBody = entitySource.substring(releaseStart, applyStart);
        String applyBody = entitySource.substring(applyStart, placeStart);
        yes(releaseBody.contains("stampStillBirth"), "host release stamps the visual birth");
        yes(!applyBody.contains("stampStillBirth"), "applySpec does not start the visual clock");
        yes(!applyBody.contains("DATA_STILL_BIRTH"), "applySpec does not write the birth");
        yes(countOf(entitySource, "cancelStillSheet(") == 1, "normal entity flow does not cancel the visual");

        String specSource = Files.readString(projectRoot.resolve(
                "src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitSpec.java"), StandardCharsets.UTF_8);
        yes(specSource.contains("stillSheet = StillSheetVisual.NONE"), "still sheet defaults off");
        int stageStart = specSource.indexOf("hostGroup.stagePublished(entity);");
        int stampStart = specSource.indexOf("entity.stampStillBirth();");
        yes(stageStart >= 0 && stampStart > stageStart, "non-host birth is stamped after publish");
        yes(!specSource.substring(stageStart, stampStart).contains("stampStillBirth"), "host publish does not stamp early");

        String settleSource = Files.readString(projectRoot.resolve(
                "src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitSettle.java"), StandardCharsets.UTF_8);
        yes(!settleSource.contains("cancelStillSheet"), "expire and hit settlement do not cancel the visual");

        String rendererSource = Files.readString(projectRoot.resolve(
                "src/main/java/com/bslx/XueXiMod/client/TechniqueHitRenderer.java"), StandardCharsets.UTF_8);
        yes(countOf(rendererSource, "stillSheet().enabled()") == 2, "old sheet path skips an enabled still visual");

        String playbackSource = Files.readString(projectRoot.resolve(
                "src/main/java/com/bslx/XueXiMod/client/StillSheetPlayback.java"), StandardCharsets.UTF_8);
        yes(playbackSource.contains("entitiesForRendering()"), "capture scans loaded entities");
        yes(playbackSource.contains("getGameTime()"), "frame clock uses world time");
        yes(playbackSource.contains("StillSheetLayout.faceWindow"), "drawing uses the shared lane window");
        yes(playbackSource.contains("textureV(frame, 0.0F"), "top uv starts at the frame");
        yes(playbackSource.contains("textureV(frame, 1.0F"), "bottom uv ends at the frame");
        yes(countOf(playbackSource, ".endVertex()") == 4, "one double-sided quad, not two blended faces");
        yes(playbackSource.contains("0.0F, 0.0F, 1.0F"), "quad keeps a single facing");
        yes(!playbackSource.contains("0.0F, 0.0F, -1.0F"), "reverse face is not submitted again");
        yes(!playbackSource.contains("tickCount"), "client tick count is not the birth clock");
        yes(!playbackSource.contains("getModelScale"), "drawing does not apply another scale");
        yes(!playbackSource.contains("tryHit"), "playback does not hit");
        yes(!playbackSource.contains("spawnReported"), "playback does not spawn");
        yes(!playbackSource.contains("TechniqueHitSpec"), "playback does not touch the spawn spec");

        String meshSource = Files.readString(projectRoot.resolve(
                "src/main/java/com/bslx/XueXiMod/client/StillSheetMesh.java"), StandardCharsets.UTF_8);
        yes(meshSource.contains("NO_CULL"), "sheet is double sided without a second face");
        yes(meshSource.contains("COLOR_WRITE"), "sheet writes color only");
        yes(meshSource.contains("LEQUAL_DEPTH_TEST"), "scene depth test stays on");
        yes(meshSource.contains("TRANSLUCENT_TRANSPARENCY"), "sheet uses translucent blending");
        yes(meshSource.contains("new TextureStateShard(texture, false, false)"), "texture is nearest and has no mipmap");

        String layoutSource = Files.readString(projectRoot.resolve(
                "src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/StillSheetLayout.java"), StandardCharsets.UTF_8);
        yes(layoutSource.contains("CohortLaneLayout.sheetWindow"), "layout delegates to the existing slots");
        yes(!layoutSource.contains("modelScale"), "layout has no second scale");
    }

    private static void checkPng(Path projectRoot) throws Exception {
        Path archivePng = projectRoot.resolve("docs/资源库/特效/次元斩/柳叶单斩_v8/dimension_willow_v8.png");
        Path gamePng = projectRoot.resolve("src/main/resources/assets/xueximod/textures/entity/xueximod/dimension_willow_v8.png");
        yes(Files.isRegularFile(archivePng), "archive v8 png exists");
        yes(Files.isRegularFile(gamePng), "game v8 png exists");
        byte[] archiveBytes = Files.readAllBytes(archivePng);
        byte[] gameBytes = Files.readAllBytes(gamePng);
        yes(archiveBytes.length == gameBytes.length, "copied png keeps the archive length");
        yes(sha256(archiveBytes).equals(sha256(gameBytes)), "copied png hash matches the archive");
        yes(archiveBytes.length >= 26 && archiveBytes[0] == (byte) 0x89, "png signature");
        yes(pngInt(archiveBytes, 16) == 96, "png width is 96");
        yes(pngInt(archiveBytes, 20) == 960, "png height is 960");
        yes(archiveBytes[24] == 8, "png bit depth is 8");
        yes(archiveBytes[25] == 6, "png keeps an alpha channel");
        yes(pngInt(archiveBytes, 20) / 48 == 20, "strip holds 20 frames of 96x48");
    }

    private static StillSheetSnapshot snapshot(UUID entityId, String worldName, long birthGameTime, CohortVisual cohort) {
        return new StillSheetSnapshot(entityId, worldName, 4.0D, 64.0D, 8.0D, 90.0F, 0.0F, 0.0F,
                5.0D, 3.0D, 0.0D, cohort, "xueximod:textures/entity/xueximod/dimension_willow_v8.png",
                20, 1, birthGameTime);
    }

    private static boolean separated(CohortLaneLayout.FaceWindow left, CohortLaneLayout.FaceWindow right) {
        boolean acrossApart = left.acrossHigh() <= right.acrossLow() || right.acrossHigh() <= left.acrossLow();
        boolean riseApart = left.riseHigh() <= right.riseLow() || right.riseHigh() <= left.riseLow();
        return acrossApart || riseApart;
    }

    private static void sameWindow(CohortLaneLayout.FaceWindow left, CohortLaneLayout.FaceWindow right, String label) {
        near(left.acrossLow(), right.acrossLow(), label + " across low");
        near(left.acrossHigh(), right.acrossHigh(), label + " across high");
        near(left.riseLow(), right.riseLow(), label + " rise low");
        near(left.riseHigh(), right.riseHigh(), label + " rise high");
    }

    private static void noCombat(Class<?> type) {
        for (java.lang.reflect.Method method : type.getDeclaredMethods()) {
            String methodName = method.getName().toLowerCase(Locale.ROOT);
            yes(!methodName.contains("spawn") && !methodName.contains("damage") && !methodName.contains("tryhit")
                            && !methodName.contains("attack"),
                    type.getSimpleName() + " has no combat method " + method.getName());
        }
    }

    private static int countOf(String source, String token) {
        int found = 0;
        int fromIndex = 0;
        while (fromIndex < source.length()) {
            int nextIndex = source.indexOf(token, fromIndex);
            if (nextIndex < 0) {
                break;
            }
            found++;
            fromIndex = nextIndex + token.length();
        }
        return found;
    }

    private static String sha256(byte[] fileBytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(fileBytes);
        StringBuilder builder = new StringBuilder(digest.length * 2);
        for (byte piece : digest) {
            builder.append(Character.forDigit((piece >> 4) & 0xF, 16));
            builder.append(Character.forDigit(piece & 0xF, 16));
        }
        return builder.toString();
    }

    private static int pngInt(byte[] fileBytes, int offset) {
        return ((fileBytes[offset] & 0xFF) << 24)
                | ((fileBytes[offset + 1] & 0xFF) << 16)
                | ((fileBytes[offset + 2] & 0xFF) << 8)
                | (fileBytes[offset + 3] & 0xFF);
    }

    private static void yes(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }

    private static void near(float actual, float expected, String label) {
        checks++;
        if (!Float.isFinite(actual) || Math.abs(actual - expected) > 1.0E-3F) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }
}
