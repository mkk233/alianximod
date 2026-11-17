package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.player.PlayerBoundSkills;
import com.bslx.XueXiMod.skill.Skill;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public class BoundSkillSyncPacket {

    private final Skill[] slots = new Skill[4];

    public BoundSkillSyncPacket(ServerPlayer player) {
        for (int slotIndex = 0; slotIndex < 4; slotIndex++) {
            slots[slotIndex] = PlayerBoundSkills.get(player, slotIndex);
        }
    }

    private BoundSkillSyncPacket(Skill[] slots) {
        System.arraycopy(slots, 0, this.slots, 0, 4);
    }

    public static void send(ServerPlayer player) {
        ModNetwork.sendToPlayer(player, new BoundSkillSyncPacket(player));
    }

    public void encode(FriendlyByteBuf buf) {
        for (int slotIndex = 0; slotIndex < 4; slotIndex++) {
            Skill skill = slots[slotIndex];
            buf.writeBoolean(skill != null);
            if (skill != null) {
                buf.writeNbt(skill.toNbt());
            }
        }
    }

    public static BoundSkillSyncPacket decode(FriendlyByteBuf buf) {
        Skill[] slots = new Skill[4];
        for (int slotIndex = 0; slotIndex < 4; slotIndex++) {
            if (buf.readBoolean()) {
                slots[slotIndex] = Skill.fromNbt(buf.readNbt());
            }
        }
        return new BoundSkillSyncPacket(slots);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> updateClientCache(slots));
        });
        context.get().setPacketHandled(true);
    }

    private static final Skill[] CLIENT_SLOTS = new Skill[4];

    private static void updateClientCache(Skill[] slots) {
        for (int slotIndex = 0; slotIndex < 4; slotIndex++) {
            CLIENT_SLOTS[slotIndex] = slots[slotIndex];
        }
    }

    public static Skill clientSlot(int slot) {
        if (slot < 0 || slot >= 4) return null;
        return CLIENT_SLOTS[slot];
    }

    public static void clearClient() {
        for (int slotIndex = 0; slotIndex < 4; slotIndex++) {
            CLIENT_SLOTS[slotIndex] = null;
        }
    }

    public static void register(SimpleChannel channel, int messageId) {
        channel.registerMessage(messageId,
                BoundSkillSyncPacket.class,
                BoundSkillSyncPacket::encode,
                BoundSkillSyncPacket::decode,
                BoundSkillSyncPacket::handle
        );
    }
}
