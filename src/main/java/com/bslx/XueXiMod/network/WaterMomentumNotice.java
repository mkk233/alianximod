package com.bslx.XueXiMod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** 水势结余同步。变化为 0 时只更新结余，不加浮动行。 */
public class WaterMomentumNotice {
    private static final long VISIBLE_MILLIS = 4000L;
    private static final int KEPT_LINES = 4;

    private final float change;
    private final float balance;

    public WaterMomentumNotice(float change, float balance) {
        this.change = change;
        this.balance = balance;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeFloat(change);
        buffer.writeFloat(balance);
    }

    public static WaterMomentumNotice decode(FriendlyByteBuf buffer) {
        return new WaterMomentumNotice(buffer.readFloat(), buffer.readFloat());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        float notedChange = change;
        float notedBalance = balance;
        contextSupplier.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientNotice.push(notedChange, notedBalance)));
        contextSupplier.get().setPacketHandled(true);
    }

    public static void register(SimpleChannel channel, int messageId) {
        channel.registerMessage(messageId,
                WaterMomentumNotice.class,
                WaterMomentumNotice::encode,
                WaterMomentumNotice::decode,
                WaterMomentumNotice::handle);
    }

    public static final class ClientNotice {
        private static final ArrayList<Line> LINES = new ArrayList<>();
        private static float balance;

        private ClientNotice() {
        }

        /** 绝对值大于 0.05 才加浮动行。0 和更小的差额只改结余。 */
        public static void push(float change, float nextBalance) {
            if (change > 0.05f || change < -0.05f) {
                long now = System.currentTimeMillis();
                LINES.add(new Line(change, now));
                while (LINES.size() > KEPT_LINES) {
                    LINES.remove(0);
                }
            }
            balance = nextBalance;
        }

        public static void clear() {
            LINES.clear();
            balance = 0f;
        }

        public static float balance() {
            return balance;
        }

        /** 仍在显示时间内的变化，从旧到新。没有变化时为空。 */
        public static List<Float> freshChanges() {
            long now = System.currentTimeMillis();
            LINES.removeIf(line -> now - line.bornAt >= VISIBLE_MILLIS);
            ArrayList<Float> changes = new ArrayList<>();
            for (Line line : LINES) {
                changes.add(line.change);
            }
            return changes;
        }

        private static final class Line {
            private final float change;
            private final long bornAt;

            private Line(float change, long bornAt) {
                this.change = change;
                this.bornAt = bornAt;
            }
        }
    }
}
