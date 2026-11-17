package com.bslx.XueXiMod.shu.technique.release.firecast;

import com.bslx.XueXiMod.shu.technique.release.fire.BurnComponent;
import com.bslx.XueXiMod.shu.technique.release.fire.FireLayer;
import com.bslx.XueXiMod.shu.technique.release.fire.SegmentPlan;
import com.bslx.XueXiMod.shu.technique.release.fire.SharedSpec;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.balance.BalanceRegistry;
import com.bslx.XueXiMod.shu.balance.TechniqueBalanceSpec;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** 六式里不依赖游戏的计划和几何。退出码 0 只说明这些断言通过。 */
public final class SixFireLogicCheck {
    private static int failures;

    private SixFireLogicCheck() {
    }

    public static void main(String[] programArguments) {
        declareDefaultDamage();
        checkSpread();
        checkThrust();
        checkSky();
        checkGround();
        checkSphere();
        checkCorridor();
        checkSources();
        if (failures != 0) {
            System.err.println("six fire logic failures=" + failures);
            System.exit(1);
        }
        System.out.println("six fire logic check passed");
    }

    private static void checkSpread() {
        Optional<SegmentPlan> full = FireConsumerPlans.spread(100, 10, 0);
        expect(full.isPresent(), "spread 100 present");
        if (full.isPresent()) {
            expect(full.get().intervalTicks() == FireConsumerPlans.SPREAD_INTERVAL, "spread interval");
            expect(full.get().components().size() == 13, "spread 100 segments");
            expect(close(full.get().components().get(0).base(), 2.0d), "spread base");
            expect(close(full.get().components().get(12).resourceAboveOne(), 1.0d), "spread bonus");
            expect(close(FireLayer.combine(2.0d, 1.0d, 0.0d), 4.0d), "spread damage");
        }
        Optional<SegmentPlan> partial = FireConsumerPlans.spread(9, 0, 9);
        expect(partial.isPresent() && partial.get().components().size() == 3, "spread partial segments");
        expect(FireConsumerPlans.spread(0, 0, 0).isEmpty(), "spread zero");
    }

    private static void checkThrust() {
        Optional<BurnComponent> struck = FireConsumerPlans.thrust(10, 1, 0);
        expect(struck.isPresent(), "thrust present");
        if (struck.isPresent()) {
            expect(close(struck.get().base(), 3.6d), "thrust base");
            expect(close(struck.get().resourceAboveOne(), 0.0d), "thrust bonus");
            expect(close(FireLayer.combine(3.6d, 0.0d, 0.5d), 5.4d), "thrust with furnace");
        }
        expect(FireConsumerPlans.thrust(0, 0, 0).isEmpty(), "thrust zero");
    }

    private static void checkSky() {
        declareDefaultDamage();
        Optional<SharedSpec> runtime = FireConsumerPlans.skySpec(100, 0.0d);
        expect(runtime.isPresent() && runtime.get().dynamicDemand()
                && runtime.get().segmentLimit() == 10
                && runtime.get().intervalTicks() == FireConsumerPlans.SKY_INTERVAL, "sky runtime stays 10 by 4");
        expect(close(FireConsumerPlans.skyDamage(100, 10, 0.0d), 33.958682112d), "sky 100 first segment");
        expect(FireConsumerPlans.skyDemand(9, 10) == 1, "sky partial demand rounds up");
        expect(FireConsumerPlans.skySpec(0, 0.0d).isEmpty(), "sky zero does not mount");
        Optional<SegmentPlan> full = FireConsumerPlans.sky(100, 10, 0);
        expect(full.isPresent(), "legacy sky list still present");
        if (full.isPresent()) {
            expect(full.get().intervalTicks() == FireConsumerPlans.SKY_INTERVAL, "sky interval");
            expect(full.get().components().size() == 10, "sky 100 segments");
            double segment = FireLayer.combine(full.get().components().get(0).base(),
                    full.get().components().get(0).resourceAboveOne(), 0.0d);
            expect(close(segment * 10.0d, 123.834728448d), "legacy sky list keeps the old total");
        }
        Optional<SegmentPlan> ten = FireConsumerPlans.sky(10, 1, 0);
        expect(ten.isPresent() && ten.get().components().size() == 10, "sky 10 keeps the 10 segment cap");
        if (ten.isPresent()) {
            double once = FireLayer.combine(ten.get().components().get(0).base(),
                    ten.get().components().get(0).resourceAboveOne(), 0.0d);
            expect(close(once, 2.4d), "sky 10 damage");
        }
        Optional<SegmentPlan> fifty = FireConsumerPlans.sky(50, 5, 0);
        expect(fifty.isPresent() && fifty.get().components().size() == 10, "sky 50 keeps the 10 segment cap");
        if (fifty.isPresent()) {
            double piece = FireLayer.combine(fifty.get().components().get(0).base(),
                    fifty.get().components().get(0).resourceAboveOne(), 0.0d);
            expect(close(piece * 5.0d, 24.8832d), "sky 50 paid total");
        }
        Optional<SegmentPlan> partial = FireConsumerPlans.sky(5, 0, 5);
        expect(partial.isPresent() && partial.get().components().size() == 10, "sky partial keeps the cap");
        if (partial.isPresent()) {
            expect(close(partial.get().components().get(0).base(), 2.0d), "sky partial locks the full base");
            expect(close(FireLayer.combine(2.0d, 0.0d, 0.0d), 2.0d), "sky partial full damage");
        }
        expect(FireConsumerPlans.sky(0, 0, 0).isEmpty(), "sky zero");
    }

    private static void declareDefaultDamage() {
        if (!BalanceRegistry.isDeclared("fire_fentian", BalanceKeys.DAMAGE_MULTIPLIER)) {
            BalanceRegistry.declareSpecial(TechniqueBalanceSpec.builder("fire_fentian").damageMultiplier().build());
        }
        if (!BalanceRegistry.isDeclared("fire_liaoyuan", BalanceKeys.DAMAGE_MULTIPLIER)) {
            BalanceRegistry.declareSpecial(TechniqueBalanceSpec.builder("fire_liaoyuan").damageMultiplier().build());
        }
    }

    private static void checkGround() {
        expect(GroundFireMath.inLayer(64.0d, 64.0d, 64.0d, 3.0d, 1.0d), "feet on surface");
        expect(!GroundFireMath.inLayer(64.0d, 64.0d, 65.0d, 3.0d, 1.0d), "feet above layer");
        expect(GroundFireMath.inLayer(64.0d, 67.0d, 67.0d, 3.0d, 1.0d), "surface within slack");
        expect(!GroundFireMath.inLayer(64.0d, 68.0d, 68.0d, 3.0d, 1.0d), "surface beyond slack");
        expect(GroundFireMath.insideDisc(0.0d, 0.0d, 6.0d, 0.0d, 6.0d), "disc edge");
        expect(!GroundFireMath.insideDisc(0.0d, 0.0d, 6.1d, 0.0d, 6.0d), "outside disc");
    }

    private static void checkSphere() {
        expect(SphereTouch.reaches(0.0d, 0.0d, 0.0d, 8.0d, 7.0d, -0.2d, -0.2d, 8.2d, 0.2d, 0.2d), "sphere edge");
        expect(!SphereTouch.reaches(0.0d, 0.0d, 0.0d, 8.0d, 9.0d, 0.0d, 0.0d, 10.0d, 1.0d, 1.0d), "outside sphere");
    }

    private static void checkCorridor() {
        expect(MeleeCorridorMath.blocked(1.0d, 0.4d), "wall before target");
        expect(!MeleeCorridorMath.blocked(1.0d, 2.0d), "wall behind target");
        expect(!MeleeCorridorMath.blocked(1.0d, Double.POSITIVE_INFINITY), "no wall");
        boolean inside = MeleeCorridorMath.overlaps(0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 1.0d, 3.0d, 0.6d, 0.6d,
                -0.2d, -0.2d, 1.0d, 0.2d, 0.2d, 1.4d);
        boolean beyond = MeleeCorridorMath.overlaps(0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 1.0d, 3.0d, 0.6d, 0.6d,
                -0.2d, -0.2d, 3.4d, 0.2d, 0.2d, 3.8d);
        expect(inside, "corridor contains target");
        expect(!beyond, "corridor misses far target");
    }

    private static void checkSources() {
        expect(absent("src/main/java/com/bslx/XueXiMod/network/SkillUsePacket.java",
                "fire_heat_up", "fire_burn", "fire_cool", "fire_liaoyuan", "fire_liaoli", "fire_fentian"),
                "packet has no technique id");
        expect(absent("src/main/java/com/bslx/XueXiMod/shu/technique/release/selfstatus/SelfStatusCommit.java",
                "fire_heat_up", "fire_burn", "fire_cool"),
                "commit has no technique id");
        expect(absent("src/main/java/com/bslx/XueXiMod/shu/technique/effects/FentianEffect.java",
                "ClipContext", ".clip("),
                "sky has no block clip");
        expect(absent("src/main/java/com/bslx/XueXiMod/shu/technique/release/firecast/GroundFireFields.java",
                "setBlock", "setSecondsOnFire"),
                "ground does not edit blocks");
        expect(contains("src/main/java/com/bslx/XueXiMod/shu/technique/effects/LiaoliEffect.java", "fixedLifetime"),
                "thrust keeps lifetime");
        expect(contains("src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitSpec.java", "fixedLifetime"),
                "hit spec keeps lifetime");
    }

    private static boolean absent(String relative, String... tokens) {
        String body = read(relative);
        if (body == null) {
            return false;
        }
        for (String token : tokens) {
            if (body.contains(token)) {
                System.err.println("found " + token + " in " + relative);
                return false;
            }
        }
        return true;
    }

    private static boolean contains(String relative, String token) {
        String body = read(relative);
        return body != null && body.contains(token);
    }

    private static String read(String relative) {
        try {
            return Files.readString(Path.of(relative), StandardCharsets.UTF_8);
        } catch (Exception failure) {
            System.err.println("cannot read " + relative);
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
