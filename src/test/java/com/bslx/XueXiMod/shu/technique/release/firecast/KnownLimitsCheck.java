package com.bslx.XueXiMod.shu.technique.release.firecast;

import com.bslx.XueXiMod.shu.technique.release.selfstatus.SelfStatusOrder;
import com.bslx.XueXiMod.visual.fire.FireVisualLayout;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** 两处已报告限制的纯几何和提交顺序。退出码 0 只说明这些断言通过，不是复审。 */
public final class KnownLimitsCheck {
    private static int failures;

    private KnownLimitsCheck() {
    }

    public static void main(String[] programArguments) {
        checkSurface();
        checkDisc();
        checkOrder();
        checkSources();
        if (failures != 0) {
            System.err.println("known limits failures=" + failures);
            System.exit(1);
        }
        System.out.println("known limits check passed");
    }

    private static void checkSurface() {
        GroundFireMath.Span bottomSlab = new GroundFireMath.Span(0.0d, 0.0d, 0.0d, 1.0d, 0.5d, 1.0d);
        GroundFireMath.Span fullBlock = new GroundFireMath.Span(0.0d, 0.0d, 0.0d, 1.0d, 1.0d, 1.0d);
        GroundFireMath.Span buriedBlock = new GroundFireMath.Span(0.0d, 0.0d, 0.0d, 1.0d, 1.0d, 1.0d);
        GroundFireMath.Span coveringSlab = new GroundFireMath.Span(0.0d, 1.0d, 0.0d, 1.0d, 1.5d, 1.0d);
        GroundFireMath.Span lowStep = new GroundFireMath.Span(0.0d, 0.0d, 0.0d, 1.0d, 0.5d, 0.5d);
        GroundFireMath.Span highStep = new GroundFireMath.Span(0.0d, 0.0d, 0.5d, 1.0d, 1.0d, 1.0d);
        GroundFireMath.Span farTop = new GroundFireMath.Span(0.0d, 3.0d, 0.0d, 1.0d, 4.0d, 1.0d);
        expect(close(GroundFireMath.openTop(0.5d, 0.5d, 0.0d, 3.0d, List.of(bottomSlab)), 0.5d), "bottom slab top is 0.5");
        expect(close(GroundFireMath.openTop(0.5d, 0.5d, 0.0d, 3.0d, List.of(fullBlock)), 1.0d), "full block top is 1");
        expect(close(GroundFireMath.openTop(0.5d, 0.5d, 0.0d, 3.0d, List.of(buriedBlock, coveringSlab)), 1.5d),
                "covered lower top loses");
        expect(close(GroundFireMath.openTop(0.5d, 0.25d, 0.0d, 3.0d, List.of(lowStep, highStep)), 0.5d), "low stair step");
        expect(close(GroundFireMath.openTop(0.5d, 0.75d, 0.0d, 3.0d, List.of(lowStep, highStep)), 1.0d), "high stair step");
        expect(Double.isNaN(GroundFireMath.openTop(0.5d, 0.5d, 0.0d, 3.0d, List.of(farTop))), "top outside slack");
        expect(Double.isNaN(GroundFireMath.openTop(3.0d, 0.5d, 0.0d, 3.0d, List.of(fullBlock))), "sample outside the box");
        expect(GroundFireMath.inLayer(10.0d, 10.5d, 10.5d, 3.0d, 1.0d), "feet on a slab top");
        expect(!GroundFireMath.inLayer(10.0d, 10.5d, 11.5d, 3.0d, 1.0d), "feet on the layer ceiling are outside");
        expect(GroundFireMath.inLayer(64.0d, 64.0d, 64.0d, 3.0d, 1.0d), "feet on surface");
        expect(!GroundFireMath.inLayer(64.0d, 64.0d, 65.0d, 3.0d, 1.0d), "feet above layer");
        expect(GroundFireMath.insideDisc(0.0d, 0.0d, 6.0d, 0.0d, 6.0d), "disc edge");
        expect(!GroundFireMath.insideDisc(0.0d, 0.0d, 6.1d, 0.0d, 6.0d), "outside disc");
    }

    private static void checkDisc() {
        expect(close(FireVisualLayout.discCoordinate(0.0d, 6.0d), 0.5d), "disc center");
        expect(close(FireVisualLayout.discCoordinate(6.0d, 6.0d), 1.0d), "disc east edge");
        expect(close(FireVisualLayout.discCoordinate(-6.0d, 6.0d), 0.0d), "disc west edge");
        expect(FireVisualLayout.waveVisible(0.0d) && !FireVisualLayout.waveVisible(12.0d), "wave still lasts twelve ticks");
        expect(close(FireVisualLayout.coveredRadius(6.0d, 6.0d), 3.0d), "sea still follows the wave");
    }

    private static void checkOrder() {
        expect(SelfStatusOrder.decide(false, true, true) == SelfStatusOrder.Step.PREVIEW_REJECT, "preview failure");
        expect(SelfStatusOrder.decide(true, false, true) == SelfStatusOrder.Step.UNPAID, "spend failure");
        expect(SelfStatusOrder.decide(true, true, false) == SelfStatusOrder.Step.REFUND, "furnace failure refunds");
        expect(SelfStatusOrder.decide(true, true, true) == SelfStatusOrder.Step.COMMIT, "both succeed");
    }

    private static void checkSources() {
        String fields = read("src/main/java/com/bslx/XueXiMod/shu/technique/release/firecast/GroundFireFields.java");
        String surface = read("src/main/java/com/bslx/XueXiMod/shu/technique/release/firecast/StandSurface.java");
        String client = read("src/main/java/com/bslx/XueXiMod/client/FireVisualClient.java");
        String commit = read("src/main/java/com/bslx/XueXiMod/shu/technique/release/selfstatus/SelfStatusCommit.java");
        String adapter = read("src/main/java/com/bslx/XueXiMod/shu/technique/release/selfstatus/FurnaceSelfStatusCast.java");
        String packet = read("src/main/java/com/bslx/XueXiMod/network/SkillUsePacket.java");
        expect(fields.contains("BASE_RADIUS = 6.0d") && fields.contains("FIELD_LIFE = 200")
                && fields.contains("GROUND_DAMAGE = 2.0d") && fields.contains("VERTICAL_SLACK = 3.0d")
                && fields.contains("LAYER_HEIGHT = 1.0d") && fields.contains("StandSurface.columnTop")
                && !fields.contains("setBlock") && !fields.contains("setSecondsOnFire"), "ground numbers and shared top");
        expect(surface.contains("getCollisionShape") && !surface.contains("setBlock"), "tops come from collision shapes");
        int groundDraw = client.indexOf("void drawGround");
        int burnDraw = client.indexOf("void drawBurns");
        String groundBody = groundDraw < 0 || burnDraw < groundDraw ? "" : client.substring(groundDraw, burnDraw);
        expect(groundBody.contains("discPatch") && groundBody.contains("StandSurface.columnTop")
                && !groundBody.contains("flat(wave"), "wave follows column tops");
        int attemptAt = commit.indexOf("Result attempt");
        int rejectAt = commit.indexOf("private static Result reject");
        String attemptBody = attemptAt < 0 || rejectAt < attemptAt ? "" : commit.substring(attemptAt, rejectAt);
        int chargeAt = attemptBody.indexOf("ManaPaymentReceipt.charge");
        int installAt = attemptBody.indexOf("prepared.commit(player)");
        int refundAt = attemptBody.indexOf("receipt.refund");
        int committedAt = attemptBody.lastIndexOf("return Result.COMMITTED");
        expect(chargeAt >= 0 && installAt > chargeAt && refundAt > installAt && committedAt > refundAt
                && attemptBody.contains("return Result.INSUFFICIENT")
                && attemptBody.indexOf("return Result.INSUFFICIENT") < installAt
                && attemptBody.contains("prepared.afterCommit")
                && !attemptBody.contains("FurnaceService.cool")
                && !attemptBody.contains("OVERDRIVE_LOOP")
                && !attemptBody.contains("HEAT_LOOP")
                && !attemptBody.contains("COOL_ONCE")
                && !attemptBody.contains("end passive")
                && !attemptBody.contains("removeEffect")
                && !attemptBody.contains("trySpend"), "charge before install, refund before committed");
        expect(adapter.contains("FurnaceService.cool(player)")
                && adapter.contains("ENTERED_OVERDRIVE, UPGRADED -> FireVisualLayout.OVERDRIVE_LOOP")
                && adapter.contains("ENTERED_NORMAL -> FireVisualLayout.HEAT_LOOP")
                && adapter.contains("COOLED -> FireVisualLayout.COOL_ONCE")
                && !adapter.contains("UPGRADED -> FireVisualLayout.HEAT_LOOP"),
                "cool still calls furnace service; upgrade shares overdrive and does not replay heat");
        expect(!packet.contains("fire_heat_up") && !packet.contains("fire_burn") && !packet.contains("fire_cool")
                && !packet.contains("fire_liaoyuan") && packet.contains("Result.COMMITTED"), "cooldown still follows committed only");
    }

    private static boolean close(double left, double right) {
        return Double.isFinite(left) && Double.isFinite(right) && Math.abs(left - right) < 1.0e-6d;
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
