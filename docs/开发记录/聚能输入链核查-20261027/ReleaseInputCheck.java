import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 对生产 ReleaseInput 的定向检查。期望值按契约写成字面量，不复述实现公式。
 */
public final class ReleaseInputCheck {
    private static int assertions;
    private static int failures;
    private static final List<String> lines = new ArrayList<>();

    private ReleaseInputCheck() {
    }

    public static void main(String[] args) throws Exception {
        Path logPath = Path.of(args.length > 0 ? args[0] : "contract-check.log");
        var codeSource = ReleaseInput.class.getProtectionDomain().getCodeSource();
        line("class=com.bslx.XueXiMod.shu.technique.release.ReleaseInput");
        line("codeSource=" + (codeSource == null ? "null" : codeSource.getLocation()));

        checkBaseTwoAndFour();
        checkNonPositiveDuration();
        checkIllegalMultiplier();
        checkFractionalCeil();
        checkIntSaturation();
        checkWrappedExpiry();
        checkLongSaturation();

        line("assertions=" + assertions);
        line("failures=" + failures);
        Files.write(logPath, lines, StandardCharsets.UTF_8);
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void checkBaseTwoAndFour() {
        ReleaseInput doubled = new ReleaseInput(2f);
        ReleaseInput quadrupled = new ReleaseInput(4f);

        checkFloat("BASE component", ReleaseInput.BASE.chainEnergyMultiplier(), 1f);
        checkFloat("BASE size", ReleaseInput.BASE.sizeMultiplier(), 1f);
        checkInt("BASE duration 20", ReleaseInput.BASE.scaleDuration(20), 20);
        checkInt("BASE duration 1", ReleaseInput.BASE.scaleDuration(1), 1);

        checkFloat("x2 component", doubled.chainEnergyMultiplier(), 2f);
        checkFloat("x2 size", doubled.sizeMultiplier(), 2f);
        checkInt("x2 duration 20", doubled.scaleDuration(20), 40);
        checkInt("x2 duration 1", doubled.scaleDuration(1), 2);

        checkFloat("x4 component", quadrupled.chainEnergyMultiplier(), 4f);
        checkFloat("x4 size", quadrupled.sizeMultiplier(), 4f);
        checkInt("x4 duration 20", quadrupled.scaleDuration(20), 80);
        checkInt("x4 duration 1", quadrupled.scaleDuration(1), 4);
    }

    private static void checkNonPositiveDuration() {
        ReleaseInput doubled = new ReleaseInput(2f);
        ReleaseInput quadrupled = new ReleaseInput(4f);
        checkInt("BASE duration 0", ReleaseInput.BASE.scaleDuration(0), 0);
        checkInt("BASE duration -1", ReleaseInput.BASE.scaleDuration(-1), -1);
        checkInt("BASE duration MIN", ReleaseInput.BASE.scaleDuration(Integer.MIN_VALUE), Integer.MIN_VALUE);
        checkInt("x2 duration 0", doubled.scaleDuration(0), 0);
        checkInt("x2 duration -7", doubled.scaleDuration(-7), -7);
        checkInt("x2 duration MIN", doubled.scaleDuration(Integer.MIN_VALUE), Integer.MIN_VALUE);
        checkInt("x4 duration 0", quadrupled.scaleDuration(0), 0);
        checkInt("x4 duration -7", quadrupled.scaleDuration(-7), -7);
        checkInt("x4 duration MIN", quadrupled.scaleDuration(Integer.MIN_VALUE), Integer.MIN_VALUE);
    }

    private static void checkIllegalMultiplier() {
        checkIllegal("NaN", Float.NaN, true);
        checkIllegal("+Inf", Float.POSITIVE_INFINITY, false);
        checkIllegal("-Inf", Float.NEGATIVE_INFINITY, false);
        checkIllegal("negative", -2f, false);
        checkIllegal("zero", 0f, false);
        checkIllegal("negative zero", -0.0f, false);
    }

    private static void checkIllegal(String label, float rawMultiplier, boolean expectNan) {
        ReleaseInput input = new ReleaseInput(rawMultiplier);
        if (expectNan) {
            check(label + " raw preserved", Float.isNaN(input.chainEnergyMultiplier()),
                    "actual=" + input.chainEnergyMultiplier());
        } else {
            checkFloat(label + " raw preserved", input.chainEnergyMultiplier(), rawMultiplier);
        }
        checkFloat(label + " size", input.sizeMultiplier(), 1f);
        checkInt(label + " duration 20", input.scaleDuration(20), 20);
        checkInt(label + " duration 0", input.scaleDuration(0), 0);
        checkInt(label + " duration -4", input.scaleDuration(-4), -4);
    }

    private static void checkFractionalCeil() {
        checkInt("1.5 x 1", new ReleaseInput(1.5f).scaleDuration(1), 2);
        checkInt("1.5 x 2", new ReleaseInput(1.5f).scaleDuration(2), 3);
        checkInt("2.5 x 3", new ReleaseInput(2.5f).scaleDuration(3), 8);
        checkInt("1.25 x 3", new ReleaseInput(1.25f).scaleDuration(3), 4);
        checkInt("0.5 x 3", new ReleaseInput(0.5f).scaleDuration(3), 2);
        checkInt("0.25 x 1", new ReleaseInput(0.25f).scaleDuration(1), 1);
        checkInt("0.25 x 3", new ReleaseInput(0.25f).scaleDuration(3), 1);
        checkInt("0.25 x 5", new ReleaseInput(0.25f).scaleDuration(5), 2);
    }

    private static void checkIntSaturation() {
        checkInt("x2 below int max", new ReleaseInput(2f).scaleDuration(1_073_741_823), Integer.MAX_VALUE - 1);
        checkInt("x2 crosses int max", new ReleaseInput(2f).scaleDuration(1_073_741_824), Integer.MAX_VALUE);
        checkInt("x2 at int max", new ReleaseInput(2f).scaleDuration(Integer.MAX_VALUE), Integer.MAX_VALUE);
        checkInt("x1 at int max", ReleaseInput.BASE.scaleDuration(Integer.MAX_VALUE), Integer.MAX_VALUE);
        checkInt("1.5 at int max", new ReleaseInput(1.5f).scaleDuration(Integer.MAX_VALUE), Integer.MAX_VALUE);
        checkInt("x4 crosses int max", new ReleaseInput(4f).scaleDuration(1_000_000_000), Integer.MAX_VALUE);
    }

    private static void checkWrappedExpiry() {
        int expiry = ReleaseInput.expiryTick(Integer.MAX_VALUE - 5, 10);
        checkInt("wrapped expiry tick", expiry, Integer.MIN_VALUE + 4);
        check("before wrapped expiry", !ReleaseInput.hasExpired(Integer.MAX_VALUE, Integer.MIN_VALUE + 4),
                "high side of the ring was treated as expired");
        check("immediately before wrapped expiry", !ReleaseInput.hasExpired(Integer.MIN_VALUE + 3, Integer.MIN_VALUE + 4),
                "tick before expiry was treated as expired");
        check("at wrapped expiry", ReleaseInput.hasExpired(Integer.MIN_VALUE + 4, Integer.MIN_VALUE + 4),
                "expiry tick was not expired");
        check("after wrapped expiry", ReleaseInput.hasExpired(Integer.MIN_VALUE + 5, Integer.MIN_VALUE + 4),
                "tick after expiry was not expired");
    }

    private static void checkLongSaturation() {
        checkLong("long one below max", ReleaseInput.expiryGameTime(Long.MAX_VALUE - 6L, 5L), Long.MAX_VALUE - 1L);
        checkLong("long exact max", ReleaseInput.expiryGameTime(Long.MAX_VALUE - 5L, 5L), Long.MAX_VALUE);
        checkLong("long one past max", ReleaseInput.expiryGameTime(Long.MAX_VALUE - 4L, 5L), Long.MAX_VALUE);
        checkLong("long now at max", ReleaseInput.expiryGameTime(Long.MAX_VALUE, 1L), Long.MAX_VALUE);
        checkLong("long duration at max from 1", ReleaseInput.expiryGameTime(1L, Long.MAX_VALUE), Long.MAX_VALUE);
        checkLong("long duration at max from 0", ReleaseInput.expiryGameTime(0L, Long.MAX_VALUE), Long.MAX_VALUE);
        checkLong("long ordinary sum", ReleaseInput.expiryGameTime(100L, 40L), 140L);
    }

    private static void check(String label, boolean passed, String detail) {
        assertions++;
        if (passed) {
            line("PASS " + label);
            return;
        }
        failures++;
        line("FAIL " + label + " " + detail);
    }

    private static void checkInt(String label, int actual, int expected) {
        check(label, actual == expected, "actual=" + actual + " expected=" + expected);
    }

    private static void checkLong(String label, long actual, long expected) {
        check(label, actual == expected, "actual=" + actual + " expected=" + expected);
    }

    private static void checkFloat(String label, float actual, float expected) {
        check(label, Float.compare(actual, expected) == 0, "actual=" + actual + " expected=" + expected);
    }

    private static void line(String text) {
        lines.add(text);
        System.out.println(text);
    }
}
