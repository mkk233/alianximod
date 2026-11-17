package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.gui.SkillAssemblyMenu;
import com.bslx.XueXiMod.item.StaffData;
import com.bslx.XueXiMod.item.SkillCarrier;
import com.bslx.XueXiMod.item.SkillStorageData;
import com.bslx.XueXiMod.item.SkillStorageItem;
import com.bslx.XueXiMod.item.StaffItem;
import com.bslx.XueXiMod.item.StaffSkills;
import com.bslx.XueXiMod.player.PlayerBoundSkills;
import com.bslx.XueXiMod.player.PlayerShuData;
import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.skill.SkillType;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueManager;
import com.bslx.XueXiMod.shu.TechniqueType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SkillAssemblyPacket {

    public static final int ACTION_OPEN = 0;
    public static final int ACTION_PUT = 1;
    public static final int ACTION_REMOVE = 2;
    public static final int ACTION_MOVE_LEFT = 3;
    public static final int ACTION_MOVE_RIGHT = 4;

    private final int action;
    private final int slot;
    private final int storageIndex;
    private final int techniqueIndex;

    public SkillAssemblyPacket(int action, int slot, int storageIndex, int techniqueIndex) {
        this.action = action;
        this.slot = slot;
        this.storageIndex = storageIndex;
        this.techniqueIndex = techniqueIndex;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(action);
        buf.writeInt(slot);
        buf.writeInt(storageIndex);
        buf.writeInt(techniqueIndex);
    }

    public static SkillAssemblyPacket decode(FriendlyByteBuf buf) {
        return new SkillAssemblyPacket(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;
            switch (action) {
                case ACTION_OPEN -> open(player);
                case ACTION_PUT -> put(player);
                case ACTION_REMOVE -> remove(player);
                case ACTION_MOVE_LEFT -> move(player, -1);
                case ACTION_MOVE_RIGHT -> move(player, 1);
                default -> { }
            }
        });
        context.get().setPacketHandled(true);
    }

    private void open(ServerPlayer player) {
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!SkillCarrier.canRelease(mainHand)) return;
        if (!StaffItem.holds(mainHand)) {
            BoundSkillSyncPacket.send(player);
        }
        boolean holdingStaff = StaffItem.holds(mainHand);
        NetworkHooks.openScreen(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal(holdingStaff ? "法杖装配" : "剑技装配");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
                return new com.bslx.XueXiMod.gui.SpellAssemblyMenu(containerId, playerInv);
            }
        });
    }

    private void put(ServerPlayer player) {
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!SkillCarrier.canRelease(mainHand)) return;
        boolean staff = StaffItem.holds(mainHand);

        ItemStack storage = SkillStorageItem.find(player);
        if (storage.isEmpty()) return;
        List<SkillStorageData.Entry> entries = SkillStorageData.entries(storage);
        if (storageIndex < 0 || storageIndex >= entries.size()) return;

        SkillStorageData.Entry entry = entries.get(storageIndex);
        String spellId = entry.spellId();
        String embryoId = entry.embryoId();
        Technique technique = TechniqueManager.getById(spellId);
        if (technique == null) return;

        if (staff) {
            if (staffBlocked(player, mainHand)) {
                return;
            }
            if (technique.getType() == TechniqueType.SWORD) {
                player.displayClientMessage(Component.literal("无法装配：剑术不能装入法杖"), false);
                return;
            }
            PlayerShuData learned = PlayerShuData.get(player);
            if (!learned.hasLearned(technique.getId()) || !learned.isUnlocked(technique)) {
                player.displayClientMessage(Component.translatable("gui.xueximod.staff.unlearned"), false);
                return;
            }
            if (StaffSkills.count(mainHand) >= ((StaffItem) mainHand.getItem()).data().slotCount()) {
                player.displayClientMessage(Component.literal("无法装配：法杖槽位已满"), false);
                return;
            }
        } else {
            if (slot < 0 || slot >= 4) return;
            if (technique.getType() == TechniqueType.MAGIC && !SkillCarrier.allowsMagic(mainHand)) {
                player.displayClientMessage(Component.literal("无法装配：法术不能装入剑技槽"), false);
                return;
            }
            if (SkillChainPlan.suppliesChainEnergy(technique)) {
                player.displayClientMessage(Component.literal("无法装配：聚能不能装入剑技槽"), false);
                return;
            }

            int swordLevel = PlayerShuData.get(player).getSwordLevel();
            int capacity = 10 + (Math.max(swordLevel, 1) - 1) * 5;
            if (assemblyCostOfSlot(player, slot) + technique.assemblyCost() > capacity) {
                player.displayClientMessage(Component.literal("无法装配：超出剑技容量"), false);
                return;
            }
            Skill existing = PlayerBoundSkills.get(player, slot);
            int chainLength = existing == null ? 0 : existing.getTechniques().size();
            if (chainLength >= com.bslx.XueXiMod.skill.Skill.CHAIN_LIMIT) {
                player.displayClientMessage(Component.literal("无法装配：一条剑技最多 8 段"), false);
                return;
            }
        }

        if (!staff) {
            Skill existing = PlayerBoundSkills.get(player, slot);
            List<Technique> proposed = new ArrayList<>();
            if (existing != null) proposed.addAll(existing.getTechniques());
            proposed.add(technique);
            String mixedRefusal = SkillSegmentMath.mixedChainRefusal(proposed);
            if (mixedRefusal != null) {
                player.displayClientMessage(Component.literal("无法装配：" + mixedRefusal), false);
                return;
            }
        }

        if (!SkillStorageData.consume(storage, spellId, embryoId, 1)) {
            player.displayClientMessage(Component.literal("无法装配：匣子里没有这个术式"), false);
            return;
        }
        if (staff) {
            StaffSkills.add(mainHand, spellId, ShuAttribute.fromId(embryoId));
        } else {
            appendToSlot(player, slot, technique, embryoId);
            BoundSkillSyncPacket.send(player);
        }
        syncInventory(player);
    }

    private void remove(ServerPlayer player) {
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!SkillCarrier.canRelease(mainHand)) return;
        boolean staff = StaffItem.holds(mainHand);
        if (staff) {
            removeFromStaff(player, mainHand);
            return;
        }

        if (slot < 0 || slot >= 4) return;
        Skill skill = PlayerBoundSkills.get(player, slot);
        if (skill == null) return;
        if (techniqueIndex < 0 || techniqueIndex >= skill.getTechniques().size()) return;

        ItemStack storage = SkillStorageItem.getOrCreate(player);
        if (storage.isEmpty()) {
            player.displayClientMessage(Component.literal("背包已满，无法收入术式匣"), false);
            return;
        }

        Technique removed = skill.getTechniques().remove(techniqueIndex);
        ShuAttribute embryo = skill.getEmbryos().size() > techniqueIndex
                ? skill.getEmbryos().remove(techniqueIndex) : ShuAttribute.NONE;

        SkillStorageData.add(storage, removed.getId(), embryo.getId(), 1);

        if (skill.getTechniques().isEmpty()) {
            PlayerBoundSkills.set(player, slot, null);
        } else {
            PlayerBoundSkills.set(player, slot, skill);
        }
        BoundSkillSyncPacket.send(player);
        syncInventory(player);
    }

    private void removeFromStaff(ServerPlayer player, ItemStack mainHand) {
        if (staffBlocked(player, mainHand)) {
            return;
        }
        if (slot < 0 || slot >= StaffSkills.count(mainHand)) return;
        ItemStack storage = SkillStorageItem.getOrCreate(player);
        if (storage.isEmpty()) {
            player.displayClientMessage(Component.literal("背包已满，无法收入术式匣"), false);
            return;
        }
        var entry = StaffSkills.entries(mainHand).get(slot);
        SkillStorageData.add(storage, entry.techniqueId,
                entry.embryo != null ? entry.embryo.getId() : ShuAttribute.NONE.getId(), 1);
        StaffSkills.remove(mainHand, slot);
        syncInventory(player);
    }

    private void move(ServerPlayer player, int direction) {
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!SkillCarrier.canRelease(mainHand)) return;
        boolean staff = StaffItem.holds(mainHand);
        if (staff) {
            if (staffBlocked(player, mainHand)) {
                return;
            }
            int index = slot;
            int target = index + direction;
            if (index < 0 || index >= StaffSkills.count(mainHand)
                    || target < 0 || target >= StaffSkills.count(mainHand)) return;
            StaffSkills.move(mainHand, index, direction);
            syncInventory(player);
            return;
        }

        if (slot < 0 || slot >= 4) return;
        Skill skill = PlayerBoundSkills.get(player, slot);
        if (skill == null) return;

        List<Technique> techs = skill.getTechniques();
        List<ShuAttribute> embryos = skill.getEmbryos();
        int fromIndex = techniqueIndex;
        int toIndex = fromIndex + direction;
        if (fromIndex < 0 || fromIndex >= techs.size() || toIndex < 0 || toIndex >= techs.size() || toIndex >= embryos.size()) return;

        java.util.Collections.swap(techs, fromIndex, toIndex);
        java.util.Collections.swap(embryos, fromIndex, toIndex);
        PlayerBoundSkills.set(player, slot, skill);
        BoundSkillSyncPacket.send(player);
    }

    private void appendToSlot(ServerPlayer player, int slot, Technique technique, String embryoId) {
        Skill skill = PlayerBoundSkills.get(player, slot);
        if (skill == null) {
            skill = new Skill(technique.getName(), SkillType.SWORD, new ArrayList<>());
        }
        skill.getTechniques().add(technique);
        skill.getEmbryos().add(ShuAttribute.fromId(embryoId));
        PlayerBoundSkills.set(player, slot, skill);
    }

    private int assemblyCostOfSlot(ServerPlayer player, int slot) {
        Skill skill = PlayerBoundSkills.get(player, slot);
        if (skill == null) return 0;
        int sum = 0;
        for (Technique technique : skill.getTechniques()) {
            sum += technique.assemblyCost();
        }
        return sum;
    }

    private boolean staffBlocked(ServerPlayer player, ItemStack mainHand) {
        int slots = ((StaffItem) mainHand.getItem()).data().slotCount();
        if (slots <= 0 || slots > StaffData.SLOT_CAP) {
            player.displayClientMessage(Component.translatable("gui.xueximod.staff.bad_slots"), false);
            return true;
        }
        if (StaffSkills.overflowHeld(mainHand)) {
            player.displayClientMessage(Component.translatable("gui.xueximod.staff.overflow_edit"), false);
            return true;
        }
        return false;
    }

    private static void syncInventory(ServerPlayer player) {
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
    }

    public static void register(SimpleChannel channel, int messageIndex) {
        channel.registerMessage(messageIndex,
                SkillAssemblyPacket.class,
                SkillAssemblyPacket::encode,
                SkillAssemblyPacket::decode,
                SkillAssemblyPacket::handle);
    }

    public static class Registration implements NetworkPacket {
        @Override
        public void register(SimpleChannel channel, int messageIndex) {
            SkillAssemblyPacket.register(channel, messageIndex);
        }
    }
}
