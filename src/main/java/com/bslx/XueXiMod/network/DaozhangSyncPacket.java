package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.gui.DaozhangMenu;
import com.bslx.XueXiMod.npc.daozhang.DaozhangCourse;
import com.bslx.XueXiMod.npc.daozhang.DaozhangRelation;
import com.bslx.XueXiMod.player.LearningGraph;
import com.bslx.XueXiMod.shu.ShuAttribute;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** 服务端把当前关系和课程名单发给打开对话的玩家。 */
public class DaozhangSyncPacket {
    private final UUID daozhangId;
    private final UUID sessionNonce;
    private final DaozhangRelation relation;
    private final List<DaozhangCourse> courses;

    public DaozhangSyncPacket(UUID daozhangId, UUID sessionNonce, DaozhangRelation relation, List<DaozhangCourse> courses) {
        this.daozhangId = daozhangId;
        this.sessionNonce = sessionNonce;
        this.relation = relation == null ? DaozhangRelation.NEUTRAL : relation;
        this.courses = courses == null ? List.of() : List.copyOf(courses);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(this.daozhangId);
        buffer.writeUUID(this.sessionNonce);
        buffer.writeEnum(this.relation);
        buffer.writeVarInt(this.courses.size());
        for (DaozhangCourse course : this.courses) {
            buffer.writeUtf(course.techniqueId(), 128);
            buffer.writeUtf(course.name(), 128);
            buffer.writeEnum(course.kind());
            buffer.writeEnum(course.stage());
            buffer.writeUtf(course.parents(), 512);
            buffer.writeBoolean(course.parentsSatisfied());
            buffer.writeBoolean(course.learned());
            buffer.writeBoolean(course.canStudy());
            buffer.writeBoolean(course.unlocked());
            buffer.writeEnum(course.embryo());
            buffer.writeUtf(course.slipName(), 128);
        }
    }

    public static DaozhangSyncPacket decode(FriendlyByteBuf buffer) {
        UUID daozhangId = buffer.readUUID();
        UUID sessionNonce = buffer.readUUID();
        DaozhangRelation relation = buffer.readEnum(DaozhangRelation.class);
        int count = Math.max(0, buffer.readVarInt());
        List<DaozhangCourse> courses = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            courses.add(new DaozhangCourse(
                    buffer.readUtf(128),
                    buffer.readUtf(128),
                    buffer.readEnum(DaozhangCourse.Kind.class),
                    buffer.readEnum(LearningGraph.Stage.class),
                    buffer.readUtf(512),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readEnum(ShuAttribute.class),
                    buffer.readUtf(128)));
        }
        return new DaozhangSyncPacket(daozhangId, sessionNonce, relation, courses);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> accept(this)));
        context.setPacketHandled(true);
    }

    private static void accept(DaozhangSyncPacket packet) {
        if (Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.containerMenu instanceof DaozhangMenu menu) {
            menu.acceptSync(packet.daozhangId, packet.sessionNonce, packet.relation, packet.courses);
        }
    }

    public static void register(SimpleChannel channel, int id) {
        channel.registerMessage(id, DaozhangSyncPacket.class, DaozhangSyncPacket::encode,
                DaozhangSyncPacket::decode, DaozhangSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
