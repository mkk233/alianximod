package com.bslx.XueXiMod.event;

import com.bslx.XueXiMod.item.SkillCarrier;
import com.bslx.XueXiMod.item.StaffItem;
import com.bslx.XueXiMod.keybind.ModKeyBindings;
import com.bslx.XueXiMod.network.BoundSkillSyncPacket;
import com.bslx.XueXiMod.network.ModNetwork;
import com.bslx.XueXiMod.network.SkillAssemblyPacket;
import com.bslx.XueXiMod.network.SkillUsePacket;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.effects.SwordFlightEffect;
import com.bslx.XueXiMod.shu.technique.sword.SwordEntity;
import com.bslx.XueXiMod.skill.Skill;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;

import java.util.UUID;

public class SkillHotkeyHandler {

    private static boolean useHeld = false;
    private static boolean slot2Held = false;
    private static boolean slot3Held = false;
    private static boolean slot4Held = false;
    private static boolean wasShuGuiDown = false;

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options == null) return;

        if (minecraft.screen != null) {
            syncHold(0, false);
            syncHold(1, false);
            syncHold(2, false);
            syncHold(3, false);
            wasShuGuiDown = true;
            return;
        }

        ItemStack mainHand = minecraft.player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean canRelease = SkillCarrier.canRelease(mainHand);
        boolean staff = StaffItem.holds(mainHand);
        boolean empty = mainHand.isEmpty();

        boolean shuGuiDown = ModKeyBindings.OPEN_SHU_GUI.isDown();
        if (shuGuiDown && !wasShuGuiDown && canRelease) {
            ModNetwork.CHANNEL.sendToServer(new SkillAssemblyPacket(
                    SkillAssemblyPacket.ACTION_OPEN, -1, -1, -1));
        }
        wasShuGuiDown = shuGuiDown;

        boolean useDown = minecraft.options.keyUse.isDown();
        syncHold(0, useDown && castAllowed(minecraft, 0, canRelease, staff, empty));

        boolean slot2Down = ModKeyBindings.SKILL_SLOT_2.isDown();
        syncHold(1, !staff && slot2Down && castAllowed(minecraft, 1, canRelease, false, empty));

        boolean slot3Down = ModKeyBindings.SKILL_SLOT_3.isDown();
        syncHold(2, !staff && slot3Down && castAllowed(minecraft, 2, canRelease, false, empty));

        boolean slot4Down = ModKeyBindings.SKILL_SLOT_4.isDown();
        syncHold(3, !staff && slot4Down && castAllowed(minecraft, 3, canRelease, false, empty));
    }

    /** 按下时通知服务端开始按住连放，松开或这格不再允许释放时停掉。 */
    private static void syncHold(int slot, boolean wantHold) {
        boolean held = holdFlag(slot);
        if (wantHold == held) {
            return;
        }
        int gesture = wantHold ? SkillUsePacket.GESTURE_HOLD : SkillUsePacket.GESTURE_DROP;
        ModNetwork.CHANNEL.sendToServer(new SkillUsePacket(slot, gesture));
        setHoldFlag(slot, wantHold);
    }

    private static boolean holdFlag(int slot) {
        return switch (slot) {
            case 0 -> useHeld;
            case 1 -> slot2Held;
            case 2 -> slot3Held;
            default -> slot4Held;
        };
    }

    private static void setHoldFlag(int slot, boolean held) {
        switch (slot) {
            case 0 -> useHeld = held;
            case 1 -> slot2Held = held;
            case 2 -> slot3Held = held;
            default -> slot4Held = held;
        }
    }

    /** 手持剑或法杖照旧。空手操纵默认要求世界里有自己的剑；御剑改由背包真剑来源放行。 */
    private static boolean castAllowed(Minecraft minecraft, int slot, boolean carrier, boolean staff, boolean empty) {
        if (staff) {
            return slot == 0;
        }
        if (carrier) {
            return true;
        }
        return empty && emptyHandManipulation(minecraft, slot);
    }

    private static boolean emptyHandManipulation(Minecraft minecraft, int slot) {
        Skill skill = BoundSkillSyncPacket.clientSlot(slot);
        if (skill == null || minecraft.level == null || minecraft.player == null) {
            return false;
        }
        boolean manipulates = false;
        for (Technique technique : skill.getTechniques()) {
            ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
            if (definition != null && definition.manipulatesSwordEntities()) {
                manipulates = true;
                break;
            }
        }
        if (!manipulates) {
            return false;
        }
        if (SwordFlightEffect.clientHasInventorySource(skill.getTechniques())) {
            return true;
        }
        return hasOwnedSword(minecraft);
    }

    private static boolean hasOwnedSword(Minecraft minecraft) {
        ClientLevel clientLevel = minecraft.level;
        if (clientLevel == null) {
            return false;
        }
        UUID owner = minecraft.player.getUUID();
        for (Entity entity : clientLevel.entitiesForRendering()) {
            if (entity instanceof SwordEntity sword
                    && sword.isAlive()
                    && owner.equals(sword.ownerUuid())) {
                return true;
            }
        }
        return false;
    }
}
