package com.bslx.XueXiMod.status.fire.furnace;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** 效果已经加上后又判失败时，必须回到调用前。退出码 0 只说明这些断言通过，不是复审。 */
public final class FurnaceAtomicCheck {
    private static int failures;

    private FurnaceAtomicCheck() {
    }

    public static void main(String[] programArguments) {
        checkUpgradeKeepsHeat();
        checkBlockedAfterAdd();
        checkSources();
        if (failures != 0) {
            System.err.println("furnace atomic failures=" + failures);
            System.exit(1);
        }
        System.out.println("furnace atomic check passed");
    }

    private static void checkUpgradeKeepsHeat() {
        FurnaceMath.Transition upgraded = FurnaceMath.transition(
                FurnaceMath.Tier.NORMAL, 12.5d, 40, FurnaceMath.Action.ENTER_OVERDRIVE);
        expect(upgraded.outcome() == FurnaceMath.Outcome.UPGRADED, "upgrade outcome");
        expect(upgraded.tier() == FurnaceMath.Tier.OVERDRIVE, "upgrade tier");
        expect(close(upgraded.bonus(), 12.5d), "upgrade keeps heat");
    }

    private static void checkBlockedAfterAdd() {
        FurnaceInstall.View before = new FurnaceInstall.View(true, 0, true, 12.5d);
        FurnaceInstall.View added = new FurnaceInstall.View(true, 1, false, 12.5d);
        FurnaceInstall.View restored = FurnaceInstall.blocked(before, added, 1);
        expect(!FurnaceInstall.accepted(true, 1, false, 1), "added effect with finite duration is rejected");
        expect(FurnaceInstall.plan(true, 1, false, 1, true, false, true)
                == FurnaceInstall.Plan.REINSTALL_PREVIOUS, "previous effect is reinstalled");
        expect(restored.present() && restored.amplifier() == 0 && restored.infinite()
                && close(restored.bonus(), 12.5d), "blocked upgrade restores mode and heat");

        FurnaceInstall.View empty = new FurnaceInstall.View(false, 0, false, 0.0d);
        FurnaceInstall.View stuck = new FurnaceInstall.View(true, 0, true, 0.0d);
        FurnaceInstall.View cleared = FurnaceInstall.blocked(empty, stuck, 1);
        expect(FurnaceInstall.plan(true, 0, true, 1, false, false, true)
                == FurnaceInstall.Plan.REMOVE_CURRENT, "new effect is removed");
        expect(!cleared.present() && close(cleared.bonus(), 0.0d), "blocked first enter leaves no furnace");

        expect(FurnaceInstall.plan(true, 0, true, 1, true, true, true)
                == FurnaceInstall.Plan.STORE_ONLY, "unchanged effect still rewrites the saved heat");
        expect(FurnaceInstall.accepted(true, 0, true, 0), "matching normal effect is accepted");
        expect(FurnaceInstall.accepted(true, 1, true, 1), "matching overdrive effect is accepted");
        expect(FurnaceInstall.plan(true, 1, true, 1, true, false, true)
                == FurnaceInstall.Plan.ACCEPT, "successful upgrade does not roll back");
    }

    private static void checkSources() {
        String service = read("src/main/java/com/bslx/XueXiMod/status/fire/furnace/FurnaceService.java");
        String commit = read("src/main/java/com/bslx/XueXiMod/shu/technique/release/selfstatus/SelfStatusCommit.java");
        int enterAt = service.indexOf("private static FurnaceChange enter");
        int restoreAt = service.indexOf("private static void restore");
        int dropAt = service.indexOf("private static void drop");
        String enterBody = slice(service, enterAt, restoreAt);
        String restoreBody = slice(service, restoreAt, dropAt);
        int addedAt = enterBody.indexOf("forceAddEffect(incoming, player)");
        int acceptAt = enterBody.indexOf("Plan.ACCEPT");
        int undoAt = enterBody.indexOf("restore(player");
        int blockedAt = enterBody.lastIndexOf("REJECT_BLOCKED");
        expect(addedAt >= 0 && acceptAt > addedAt && undoAt > acceptAt && blockedAt > undoAt,
                "rejection after add restores before reporting blocked");
        expect(enterBody.contains("enterOverdrive") == false && service.contains("enterOverdrive"),
                "overdrive entry remains");
        expect(restoreBody.contains("replaceTag") && restoreBody.contains("forceAddEffect")
                && restoreBody.contains("removeEffect"), "restore writes the saved effect and heat");
        expect(!restoreBody.contains("SteamBurst") && !restoreBody.contains("endPassive")
                && !restoreBody.contains("cool("), "restore does not cool or end the furnace");
        expect(commit.contains("receipt.refund") && commit.contains("return Result.REJECTED")
                && commit.indexOf("receipt.refund") < commit.lastIndexOf("return Result.COMMITTED"),
                "mana still refunds when the furnace is not committed");
    }

    private static boolean close(double left, double right) {
        return Double.isFinite(left) && Double.isFinite(right) && Math.abs(left - right) < 1.0e-9d;
    }

    private static String slice(String source, int from, int until) {
        if (from < 0 || until < from) {
            return "";
        }
        return source.substring(from, until);
    }

    private static String read(String relative) {
        try {
            return Files.readString(Path.of(relative), StandardCharsets.UTF_8);
        } catch (Exception failure) {
            failures += 1;
            System.err.println("unreadable " + relative);
            return "";
        }
    }

    private static void expect(boolean passed, String label) {
        if (!passed) {
            failures += 1;
            System.err.println("failed: " + label);
        }
    }
}
