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

    private static boolean wasUseDown = false;
    private static boolean wasSlot2Down = false;
    private static boolean wasSlot3Down = false;
    private static boolean wasSlot4Down = false;
    private static boolean wasShuGuiDown = false;

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options == null) return;

        if (mc.screen != null) {
            wasUseDown = mc.options.keyUse.isDown();
            wasSlot2Down = true;
            wasSlot3Down = true;
            wasSlot4Down = true;
            wasShuGuiDown = true;
            return;
        }

        ItemStack mainHand = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean canRelease = SkillCarrier.canRelease(mainHand);
        boolean staff = StaffItem.holds(mainHand);
        boolean empty = mainHand.isEmpty();

        boolean shuGuiDown = ModKeyBindings.OPEN_SHU_GUI.isDown();
        if (shuGuiDown && !wasShuGuiDown && canRelease) {
            ModNetwork.CHANNEL.sendToServer(new SkillAssemblyPacket(
                    SkillAssemblyPacket.ACTION_OPEN, -1, -1, -1));
        }
        wasShuGuiDown = shuGuiDown;

        boolean useDown = mc.options.keyUse.isDown();
        if (useDown && !wasUseDown && castAllowed(mc, 0, canRelease, staff, empty)) {
            ModNetwork.CHANNEL.sendToServer(new SkillUsePacket(0));
        }
        wasUseDown = useDown;

        boolean slot2Down = ModKeyBindings.SKILL_SLOT_2.isDown();
        if (!staff && slot2Down && !wasSlot2Down && castAllowed(mc, 1, canRelease, false, empty)) {
            ModNetwork.CHANNEL.sendToServer(new SkillUsePacket(1));
        }
        wasSlot2Down = slot2Down;

        boolean slot3Down = ModKeyBindings.SKILL_SLOT_3.isDown();
        if (!staff && slot3Down && !wasSlot3Down && castAllowed(mc, 2, canRelease, false, empty)) {
            ModNetwork.CHANNEL.sendToServer(new SkillUsePacket(2));
        }
        wasSlot3Down = slot3Down;

        boolean slot4Down = ModKeyBindings.SKILL_SLOT_4.isDown();
        if (!staff && slot4Down && !wasSlot4Down && castAllowed(mc, 3, canRelease, false, empty)) {
            ModNetwork.CHANNEL.sendToServer(new SkillUsePacket(3));
        }
        wasSlot4Down = slot4Down;
    }

    /** 手持剑或法杖照旧。空手只在这格链上有操纵术式、且世界里有自己的剑实体时才发释放包。 */
    private static boolean castAllowed(Minecraft mc, int slot, boolean carrier, boolean staff, boolean empty) {
        if (staff) {
            return slot == 0;
        }
        if (carrier) {
            return true;
        }
        return empty && emptyHandManipulation(mc, slot);
    }

    private static boolean emptyHandManipulation(Minecraft mc, int slot) {
        Skill skill = BoundSkillSyncPacket.clientSlot(slot);
        if (skill == null || mc.level == null || mc.player == null) {
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
        return hasOwnedSword(mc);
    }

    private static boolean hasOwnedSword(Minecraft mc) {
        if (!(mc.level instanceof ClientLevel)) {
            return false;
        }
        UUID owner = mc.player.getUUID();
        for (Entity entity : ((ClientLevel) mc.level).entitiesForRendering()) {
            if (entity instanceof SwordEntity sword
                    && sword.isAlive()
                    && owner.equals(sword.ownerUuid())) {
                return true;
            }
        }
        return false;
    }
}
