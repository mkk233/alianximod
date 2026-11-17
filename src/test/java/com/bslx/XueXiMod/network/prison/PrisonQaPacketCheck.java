package com.bslx.XueXiMod.network.prison;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * 直接走水牢包的 encode/decode。不构造世界，也不发送。
 */
public final class PrisonQaPacketCheck {
    private static int assertionCount;
    private static int failures;

    private PrisonQaPacketCheck() {
    }

    public static void main(String[] arguments) {
        FriendlyByteBuf buffer = null;
        try {
            buffer = new FriendlyByteBuf(Unpooled.buffer());
            roundTrip(buffer, sample(PrisonSyncPacket.SNAPSHOT, 12));
            buffer.clear();
            roundTrip(buffer, sample(PrisonSyncPacket.REMOVE, -1));
            System.out.println("EXECUTED packet-roundtrip");
        } catch (LinkageError failure) {
            System.out.println("UNEXECUTED packet 需要游戏引导 " + failure.getClass().getName());
        } catch (RuntimeException failure) {
            check(false, "包往返抛出 " + failure.getClass().getName() + " " + failure.getMessage());
        } finally {
            if (buffer != null) {
                buffer.release();
            }
        }
        System.out.println("未执行 handle、sendCreated 和通道方向：不构造网络上下文");
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void roundTrip(FriendlyByteBuf buffer, PrisonSyncPacket original) {
        original.encode(buffer);
        PrisonSyncPacket decoded = PrisonSyncPacket.decode(buffer);
        check(decoded.kind() == original.kind(), "kind 不一致");
        check(original.prisonId().equals(decoded.prisonId()), "prisonId 不一致");
        check(decoded.revision() == original.revision(), "revision 不一致");
        check(original.casterId().equals(decoded.casterId()), "casterId 不一致");
        check(original.targetId().equals(decoded.targetId()), "targetId 不一致");
        check(decoded.targetNetworkId() == original.targetNetworkId(), "targetNetworkId 不一致");
        check(original.dimensionId().equals(decoded.dimensionId()), "dimensionId 不一致");
        check(decoded.anchorX() == original.anchorX() && decoded.anchorY() == original.anchorY()
                        && decoded.anchorZ() == original.anchorZ(),
                "锚点不一致");
        check(decoded.lowX() == original.lowX() && decoded.lowY() == original.lowY()
                        && decoded.lowZ() == original.lowZ(),
                "壳下限不一致");
        check(decoded.highX() == original.highX() && decoded.highY() == original.highY()
                        && decoded.highZ() == original.highZ(),
                "壳上限不一致");
        check(decoded.lockedTier() == original.lockedTier(), "锁定等级不一致");
        check(decoded.birthTick() == original.birthTick() && decoded.endTick() == original.endTick(),
                "起止时刻不一致");
    }

    private static PrisonSyncPacket sample(int kind, int targetNetworkId) {
        return new PrisonSyncPacket(kind, UUID.fromString("11111111-1111-1111-1111-111111111111"), 7L,
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                targetNetworkId, new ResourceLocation("minecraft", "overworld"),
                1.0D, 2.0D, 3.0D, 4.0D, 5.0D, 6.0D, 7.0D, 8.0D, 9.0D, 4, 11L, 31L);
    }

    private static void check(boolean passed, String message) {
        assertionCount++;
        if (!passed) {
            failures++;
            System.out.println("失败 " + message);
        }
    }
}
