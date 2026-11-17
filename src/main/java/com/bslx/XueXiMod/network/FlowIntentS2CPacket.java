package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.technique.release.domain.motion.FlowClientBridge;
import com.bslx.XueXiMod.shu.technique.release.domain.motion.FlowMotionIntents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** 服务端发给驾驶者的一拍流场指令。removed 为真时客户端丢掉该实例。 */
public final class FlowIntentS2CPacket {
    private final boolean removed;
    private final boolean clearInstance;
    private final UUID instanceId;
    private final UUID rootId;
    private final long epoch;
    private final long step;
    private final double horizontalX;
    private final double horizontalZ;
    private final boolean rising;
    private final double riseFloor;

    public FlowIntentS2CPacket(boolean removed, boolean clearInstance, UUID instanceId, UUID rootId, long epoch, long step,
                               double horizontalX, double horizontalZ, boolean rising, double riseFloor) {
        this.removed = removed;
        this.clearInstance = clearInstance;
        this.instanceId = instanceId;
        this.rootId = rootId;
        this.epoch = epoch;
        this.step = step;
        this.horizontalX = horizontalX;
        this.horizontalZ = horizontalZ;
        this.rising = rising;
        this.riseFloor = riseFloor;
    }

    public static void register(SimpleChannel channel, int messageId) {
        channel.registerMessage(messageId, FlowIntentS2CPacket.class, FlowIntentS2CPacket::encode,
                FlowIntentS2CPacket::decode, FlowIntentS2CPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void send(ServerPlayer driver, FlowMotionIntents.HeldShift shift, boolean removed) {
        if (driver == null || shift == null) {
            return;
        }
        ModNetwork.sendToPlayer(driver, new FlowIntentS2CPacket(removed, false, shift.instanceId, shift.rootId,
                shift.epoch, shift.step, shift.horizontalX, shift.horizontalZ, shift.rising, shift.riseFloor));
    }

    public static void sendCleared(ServerPlayer driver, UUID instanceId) {
        if (driver == null || instanceId == null) {
            return;
        }
        ModNetwork.sendToPlayer(driver, new FlowIntentS2CPacket(true, true, instanceId, new UUID(0L, 0L),
                0L, 0L, 0.0D, 0.0D, false, 0.0D));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(removed);
        buffer.writeBoolean(clearInstance);
        buffer.writeUUID(instanceId);
        buffer.writeUUID(rootId);
        buffer.writeLong(epoch);
        buffer.writeLong(step);
        buffer.writeDouble(horizontalX);
        buffer.writeDouble(horizontalZ);
        buffer.writeBoolean(rising);
        buffer.writeDouble(riseFloor);
    }

    public static FlowIntentS2CPacket decode(FriendlyByteBuf buffer) {
        return new FlowIntentS2CPacket(buffer.readBoolean(), buffer.readBoolean(), buffer.readUUID(), buffer.readUUID(),
                buffer.readLong(), buffer.readLong(), buffer.readDouble(), buffer.readDouble(), buffer.readBoolean(),
                buffer.readDouble());
    }

    public static void handle(FlowIntentS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> FlowClientBridge.acceptIntent(packet.removed, packet.clearInstance, packet.instanceId,
                packet.rootId, packet.epoch, packet.step, packet.horizontalX, packet.horizontalZ, packet.rising,
                packet.riseFloor));
        context.setPacketHandled(true);
    }

    public boolean removed() {
        return removed;
    }

    public boolean clearInstance() {
        return clearInstance;
    }

    public UUID instanceId() {
        return instanceId;
    }

    public UUID rootId() {
        return rootId;
    }

    public long epoch() {
        return epoch;
    }

    public long step() {
        return step;
    }

    public double horizontalX() {
        return horizontalX;
    }

    public double horizontalZ() {
        return horizontalZ;
    }

    public boolean rising() {
        return rising;
    }

    public double riseFloor() {
        return riseFloor;
    }
}
