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
        for (int i = 0; i < 4; i++) {
            slots[i] = PlayerBoundSkills.get(player, i);
        }
    }

    private BoundSkillSyncPacket(Skill[] slots) {
        System.arraycopy(slots, 0, this.slots, 0, 4);
    }

    public static void send(ServerPlayer player) {
        ModNetwork.sendToPlayer(player, new BoundSkillSyncPacket(player));
    }

    public void encode(FriendlyByteBuf buf) {
        for (int i = 0; i < 4; i++) {
            Skill skill = slots[i];
            buf.writeBoolean(skill != null);
            if (skill != null) {
                buf.writeNbt(skill.toNbt());
            }
        }
    }

    public static BoundSkillSyncPacket decode(FriendlyByteBuf buf) {
        Skill[] slots = new Skill[4];
        for (int i = 0; i < 4; i++) {
            if (buf.readBoolean()) {
                slots[i] = Skill.fromNbt(buf.readNbt());
            }
        }
        return new BoundSkillSyncPacket(slots);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> updateClientCache(slots));
        });
        ctx.get().setPacketHandled(true);
    }

    private static final Skill[] CLIENT_SLOTS = new Skill[4];

    private static void updateClientCache(Skill[] slots) {
        for (int i = 0; i < 4; i++) {
            CLIENT_SLOTS[i] = slots[i];
        }
    }

    public static Skill clientSlot(int slot) {
        if (slot < 0 || slot >= 4) return null;
        return CLIENT_SLOTS[slot];
    }

    public static void register(SimpleChannel channel, int id) {
        channel.registerMessage(id,
                BoundSkillSyncPacket.class,
                BoundSkillSyncPacket::encode,
                BoundSkillSyncPacket::decode,
                BoundSkillSyncPacket::handle
        );
    }
}
