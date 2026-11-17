package com.bslx.XueXiMod.animation.runtime;

import java.util.UUID;

import com.bslx.XueXiMod.animation.core.Pose;
import com.bslx.XueXiMod.network.ActionSyncS2CPacket;

import net.minecraft.resources.ResourceLocation;

/** 冻结帧的规范化和接收端降级。无头可跑。 */
public final class ActionFrameChecks {
    private static int checks;

    private ActionFrameChecks() {
    }

    public static void main(String[] programArguments) {
        checkOrthogonalFrame();
        checkScaledAndTiltedFrame();
        checkSignedNormal();
        checkRejectedFrames();
        checkAbsentFrame();
        checkReceiveDropsInvalidFrame();
        checkLargeFrames();
        checkThresholds();
        System.out.println("ActionFrameChecks: " + checks + " assertions");
    }

    private static void checkOrthogonalFrame() {
        ActionCommit commit = frame(0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F);
        near(commit.directionX(), 0.0F, "forward x");
        near(commit.directionY(), 0.0F, "forward y");
        near(commit.directionZ(), 1.0F, "forward z");
        near(commit.planeX(), 0.0F, "normal x");
        near(commit.planeY(), 1.0F, "normal y");
        near(commit.planeZ(), 0.0F, "normal z");
        float rightX = ActionFrame.crossX(commit.planeX(), commit.planeY(), commit.planeZ(), commit.directionX(), commit.directionY(), commit.directionZ());
        float rightY = ActionFrame.crossY(commit.planeX(), commit.planeY(), commit.planeZ(), commit.directionX(), commit.directionY(), commit.directionZ());
        float rightZ = ActionFrame.crossZ(commit.planeX(), commit.planeY(), commit.planeZ(), commit.directionX(), commit.directionY(), commit.directionZ());
        near(rightX, 1.0F, "right x");
        near(rightY, 0.0F, "right y");
        near(rightZ, 0.0F, "right z");
        near(ActionFrame.crossX(commit.directionX(), commit.directionY(), commit.directionZ(), rightX, rightY, rightZ), commit.planeX(), "restored normal x");
        near(ActionFrame.crossY(commit.directionX(), commit.directionY(), commit.directionZ(), rightX, rightY, rightZ), commit.planeY(), "restored normal y");
        near(ActionFrame.crossZ(commit.directionX(), commit.directionY(), commit.directionZ(), rightX, rightY, rightZ), commit.planeZ(), "restored normal z");
    }

    private static void checkScaledAndTiltedFrame() {
        ActionCommit scaled = frame(0.0F, 0.0F, 4.0F, 0.0F, 3.0F, 0.0F);
        near(scaled.directionZ(), 1.0F, "scaled forward");
        near(scaled.planeY(), 1.0F, "scaled normal");
        ActionCommit tilted = frame(0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 0.05F);
        near(tilted.directionZ(), 1.0F, "tilted forward");
        near(tilted.planeX(), 0.0F, "tilted normal x");
        near(tilted.planeY(), 1.0F, "tilted normal y");
        near(tilted.planeZ(), 0.0F, "forward component removed");
    }

    private static void checkSignedNormal() {
        ActionCommit positive = frame(0.0F, 0.0F, 1.0F, 0.0F, 2.0F, 0.0F);
        ActionCommit negative = frame(0.0F, 0.0F, 1.0F, 0.0F, -2.0F, 0.0F);
        near(positive.planeY(), 1.0F, "positive normal");
        near(negative.planeY(), -1.0F, "negative normal");
    }

    private static void checkRejectedFrames() {
        yes(rejected(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F), "zero frame");
        yes(rejected(0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 2.0F), "parallel frame");
        yes(rejected(0.0F, 0.0F, 1.0F, 1.0E-8F, 0.0F, 1.0F), "nearly parallel frame");
        yes(rejected(Float.NaN, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F), "nan forward");
        yes(rejected(0.0F, 0.0F, 1.0F, 0.0F, Float.POSITIVE_INFINITY, 0.0F), "infinite normal");
    }

    private static void checkAbsentFrame() {
        ActionCommit commit = ActionCommit.group(UUID.randomUUID(), ResourceLocation.fromNamespaceAndPath("xuexicheck", "plain"));
        yes(!commit.hasSlashFrame(), "group has no frame");
        near(commit.directionX(), 0.0F, "absent forward x");
        near(commit.directionY(), 0.0F, "absent forward y");
        near(commit.directionZ(), 0.0F, "absent forward z");
        near(commit.planeX(), 0.0F, "absent normal x");
        near(commit.planeY(), 0.0F, "absent normal y");
        near(commit.planeZ(), 0.0F, "absent normal z");
        ActionCommit kept = commit.withFallback(ResourceLocation.fromNamespaceAndPath("xuexicheck", "fallback"));
        yes(!kept.hasSlashFrame(), "fallback copy keeps the empty frame");
    }

    private static void checkReceiveDropsInvalidFrame() {
        ActionSyncS2CPacket absent = packet(false, Float.NaN, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F);
        yes(!absent.hasSlashFrame(), "packet without a frame");
        near(absent.directionX(), 0.0F, "packet absent x");
        near(absent.planeY(), 0.0F, "packet absent normal");
        ActionSyncS2CPacket broken = packet(true, Float.NaN, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F);
        yes(!broken.hasSlashFrame(), "invalid packet frame is dropped");
        near(broken.directionZ(), 0.0F, "dropped packet has no nan forward");
        near(broken.planeY(), 0.0F, "dropped packet has no nan normal");
        ActionSampleContext context = ActionSampleContext.capture(false, UUID.randomUUID(), 1, 1L, 1L, null, null, null, "",
                true, Float.NaN, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F, 0.0F, new Pose(), 0L, 1.0F, 0.0F, 0, 1, false, 0.0F);
        yes(!context.hasSlashFrame(), "context drops an invalid frame");
        near(context.directionZ(), 0.0F, "context forward is finite");
        near(context.planeY(), 0.0F, "context normal is finite");
    }

    private static void checkLargeFrames() {
        ActionCommit hugeForward = frame(1.0E30F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F);
        yes(hugeForward.hasSlashFrame(), "huge forward stays present");
        near(hugeForward.directionX(), 1.0F, "huge forward x");
        near(hugeForward.directionY(), 0.0F, "huge forward y");
        near(hugeForward.directionZ(), 0.0F, "huge forward z");
        near(hugeForward.planeY(), 1.0F, "huge forward normal");
        ActionCommit hugeNormal = frame(1.0F, 0.0F, 0.0F, 0.0F, 1.0E30F, 0.0F);
        yes(hugeNormal.hasSlashFrame(), "huge normal stays present");
        near(hugeNormal.directionX(), 1.0F, "huge normal forward");
        near(hugeNormal.planeX(), 0.0F, "huge normal x");
        near(hugeNormal.planeY(), 1.0F, "huge normal y");
        near(hugeNormal.planeZ(), 0.0F, "huge normal z");
        ActionCommit bothHuge = frame(1.0E30F, 0.0F, 0.0F, 0.0F, 1.0E30F, 0.0F);
        yes(bothHuge.hasSlashFrame(), "both huge stays present");
        near(bothHuge.directionX(), 1.0F, "both huge forward");
        near(bothHuge.planeY(), 1.0F, "both huge normal");
        ActionCommit spread = frame(1.0E30F, 1.0E30F, 1.0E30F, 1.0F, -1.0F, 0.0F);
        near(spread.directionX(), 0.5773503F, "spread forward x");
        near(spread.directionY(), 0.5773503F, "spread forward y");
        near(spread.directionZ(), 0.5773503F, "spread forward z");
        near(spread.planeX(), 0.7071068F, "spread normal x");
        near(spread.planeY(), -0.7071068F, "spread normal y");
        near(spread.planeZ(), 0.0F, "spread normal z");
        ActionSyncS2CPacket hugePacket = packet(true, 1.0E30F, 0.0F, 0.0F, 0.0F, 1.0E30F, 0.0F);
        yes(hugePacket.hasSlashFrame(), "packet keeps a huge frame");
        near(hugePacket.directionX(), 1.0F, "packet huge forward");
        near(hugePacket.planeY(), 1.0F, "packet huge normal");
        ActionSampleContext context = ActionSampleContext.capture(true, UUID.randomUUID(), 1, 1L, 1L, null, null, null, "",
                true, 1.0E30F, 0.0F, 0.0F, 0.0F, 1.0E30F, 0.0F, 0.0F, new Pose(), 0L, 1.0F, 0.0F, 0, 1, false, 0.0F);
        yes(context.hasSlashFrame(), "context keeps a huge frame");
        near(context.directionX(), 1.0F, "context huge forward");
        near(context.planeY(), 1.0F, "context huge normal");
        yes(ActionFrame.canonicalize(1.0E30F, 0.0F, 0.0F, 1.0E30F, 0.0F, 0.0F) == null, "huge parallel is invalid");
        ActionFrame.Basis absent = ActionFrame.resolve(true, 1.0E30F, 0.0F, 0.0F, 1.0E30F, 0.0F, 0.0F);
        yes(!absent.present(), "huge parallel resolve is absent");
        near(absent.forwardX(), 0.0F, "huge parallel forward stays empty");
        near(absent.normalY(), 0.0F, "huge parallel normal stays empty");
    }

    private static void checkThresholds() {
        ActionCommit justForward = frame(2.0E-5F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F);
        near(justForward.directionX(), 1.0F, "forward just above floor");
        ActionCommit justNormal = frame(0.0F, 0.0F, 1.0F, 0.0F, 2.0E-5F, 1.0F);
        near(justNormal.directionZ(), 1.0F, "normal floor forward");
        near(justNormal.planeY(), 1.0F, "normal just above floor");
        yes(rejected(1.0E-8F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F), "forward under floor");
        yes(rejected(1.0E30F, 0.0F, 0.0F, 1.0E30F, 1.0E-8F, 0.0F), "huge near collinear");
        ActionCommit ordinary = frame(0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F);
        near(ordinary.directionZ(), 1.0F, "ordinary forward");
        near(ordinary.planeY(), 1.0F, "ordinary normal");
    }

    private static ActionCommit frame(float forwardX, float forwardY, float forwardZ, float normalX, float normalY, float normalZ) {
        return ActionCommit.framed(UUID.randomUUID(), ResourceLocation.fromNamespaceAndPath("xuexicheck", "frame"), 0,
                forwardX, forwardY, forwardZ, normalX, normalY, normalZ);
    }

    private static boolean rejected(float forwardX, float forwardY, float forwardZ, float normalX, float normalY, float normalZ) {
        try {
            frame(forwardX, forwardY, forwardZ, normalX, normalY, normalZ);
            return false;
        } catch (IllegalArgumentException expected) {
            return true;
        }
    }

    private static ActionSyncS2CPacket packet(boolean hasSlashFrame, float directionX, float directionY, float directionZ,
            float planeX, float planeY, float planeZ) {
        return new ActionSyncS2CPacket(0, 0, UUID.randomUUID(), 1, "minecraft:overworld", 1L, 1L, 0L, "xuexicheck:frame",
                1.0F, false, 0.0F, 0.0F, false, 0.0F, false, null, 1, 0L, 1.0F, 0.0F, 0.0F, -1L, "", 0,
                hasSlashFrame, directionX, directionY, directionZ, planeX, planeY, planeZ);
    }

    private static void near(float actual, float expected, String message) {
        checks++;
        if (!Float.isFinite(actual) || Math.abs(actual - expected) > 1.0E-4F) {
            throw new AssertionError(message);
        }
    }

    private static void yes(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
