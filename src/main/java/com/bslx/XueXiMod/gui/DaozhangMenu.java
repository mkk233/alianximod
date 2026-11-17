package com.bslx.XueXiMod.gui;

import com.bslx.XueXiMod.network.DaozhangSyncPacket;
import com.bslx.XueXiMod.network.ModNetwork;
import com.bslx.XueXiMod.network.DaozhangSessions;
import com.bslx.XueXiMod.npc.daozhang.DaozhangAccess;
import com.bslx.XueXiMod.npc.daozhang.DaozhangCourse;
import com.bslx.XueXiMod.npc.daozhang.DaozhangEntity;
import com.bslx.XueXiMod.npc.daozhang.DaozhangRelation;
import com.bslx.XueXiMod.npc.daozhang.DaozhangStudyService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/** 道长对话。没有物品槽，材料直接从玩家真实背包查找。 */
public class DaozhangMenu extends AbstractContainerMenu {
    private final UUID daozhangId;
    private final UUID sessionNonce;
    private final Inventory inventory;
    private DaozhangRelation relation = DaozhangRelation.NEUTRAL;
    private List<DaozhangCourse> courses = List.of();
    private int revision;
    private int clientSequence;

    public DaozhangMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readUUID(), buffer.readUUID());
    }

    public DaozhangMenu(int containerId, Inventory inventory, UUID daozhangId, UUID sessionNonce) {
        super(ModMenuTypes.DAOZHANG.get(), containerId);
        this.inventory = inventory;
        this.daozhangId = daozhangId;
        this.sessionNonce = sessionNonce;
    }

    public UUID daozhangId() {
        return this.daozhangId;
    }

    public UUID sessionNonce() {
        return this.sessionNonce;
    }

    public DaozhangRelation relation() {
        return this.relation;
    }

    public List<DaozhangCourse> courses() {
        return this.courses;
    }

    public int revision() {
        return this.revision;
    }

    public int claimSequence() {
        return this.clientSequence++;
    }

    public void acceptSync(DaozhangRelation relation, List<DaozhangCourse> courses) {
        this.acceptSync(this.daozhangId, this.sessionNonce, relation, courses);
    }

    public boolean acceptSync(UUID daozhangId, UUID sessionNonce, DaozhangRelation relation, List<DaozhangCourse> courses) {
        if (daozhangId == null || sessionNonce == null
                || !daozhangId.equals(this.daozhangId) || !sessionNonce.equals(this.sessionNonce)) {
            return false;
        }
        this.relation = relation == null ? DaozhangRelation.NEUTRAL : relation;
        this.courses = courses == null ? List.of() : List.copyOf(courses);
        this.revision++;
        if (this.inventory.player.level().isClientSide) {
            this.broadcastChanges();
        }
        return true;
    }

    public boolean apprentice(ServerPlayer player) {
        DaozhangEntity daozhang = this.bound(player);
        if (daozhang == null || daozhang.relation(player.getUUID()) != DaozhangRelation.NEUTRAL) {
            return false;
        }
        boolean changed = daozhang.apprentice(player.getUUID());
        this.refresh(player);
        return changed;
    }

    public boolean study(ServerPlayer player, String techniqueId) {
        DaozhangEntity daozhang = this.bound(player);
        if (daozhang == null) {
            return false;
        }
        boolean learned = DaozhangStudyService.study(player, daozhang, techniqueId);
        this.refresh(player);
        return learned;
    }

    public void refresh(ServerPlayer player) {
        DaozhangEntity daozhang = this.bound(player);
        if (daozhang == null) {
            player.closeContainer();
            return;
        }
        this.relation = daozhang.relation(player.getUUID());
        this.courses = this.relation == DaozhangRelation.APPRENTICE
                ? DaozhangStudyService.courses(player) : List.of();
        ModNetwork.sendToPlayer(player, new DaozhangSyncPacket(
                this.daozhangId, this.sessionNonce, this.relation, this.courses));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer serverPlayer) {
            DaozhangSessions.close(serverPlayer.getUUID(), this.sessionNonce);
        }
    }

    public boolean matches(DaozhangEntity daozhang) {
        return daozhang != null && daozhang.getUUID().equals(this.daozhangId);
    }

    private @Nullable DaozhangEntity bound(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return null;
        }
        Entity entity = level.getEntity(this.daozhangId);
        if (!(entity instanceof DaozhangEntity daozhang) || !DaozhangAccess.canTalk(daozhang, player)) {
            return null;
        }
        return daozhang;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return player.isAlive();
        }
        return this.bound(serverPlayer) != null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
