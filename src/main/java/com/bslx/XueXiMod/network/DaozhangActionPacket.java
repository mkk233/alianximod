package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.gui.DaozhangMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.UUID;
import java.util.function.Supplier;

/** 客户端只提交动作、道长、会话和序号。 */
public class DaozhangActionPacket {
    public enum Action {
        APPRENTICE,
        STUDY,
        REFRESH
    }

    private final Action action;
    private final UUID daozhangId;
    private final UUID sessionNonce;
    private final int containerId;
    private final int sequence;
    private final String techniqueId;

    public DaozhangActionPacket(Action action, UUID daozhangId, UUID sessionNonce,
                                int containerId, int sequence, String techniqueId) {
        this.action = action;
        this.daozhangId = daozhangId;
        this.sessionNonce = sessionNonce;
        this.containerId = containerId;
        this.sequence = sequence;
        this.techniqueId = techniqueId == null ? "" : techniqueId;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeUUID(this.daozhangId);
        buffer.writeUUID(this.sessionNonce);
        buffer.writeVarInt(this.containerId);
        buffer.writeVarInt(this.sequence);
        buffer.writeUtf(this.techniqueId, 128);
    }

    public static DaozhangActionPacket decode(FriendlyByteBuf buffer) {
        return new DaozhangActionPacket(
                buffer.readEnum(Action.class),
                buffer.readUUID(),
                buffer.readUUID(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readUtf(128));
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.containerMenu instanceof DaozhangMenu menu)) {
                return;
            }
            if (player.containerMenu.containerId != this.containerId
                    || !menu.daozhangId().equals(this.daozhangId)
                    || !menu.sessionNonce().equals(this.sessionNonce)
                    || !menu.stillValid(player)
                    || !DaozhangSessions.accept(player.getUUID(), this.sessionNonce, this.daozhangId, this.sequence)) {
                return;
            }
            switch (this.action) {
                case APPRENTICE -> menu.apprentice(player);
                case STUDY -> menu.study(player, this.techniqueId);
                case REFRESH -> menu.refresh(player);
            }
        });
        context.setPacketHandled(true);
    }

    public static void register(SimpleChannel channel, int id) {
        channel.registerMessage(id, DaozhangActionPacket.class, DaozhangActionPacket::encode,
                DaozhangActionPacket::decode, DaozhangActionPacket::handle);
    }
}
