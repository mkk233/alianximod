package com.bslx.XueXiMod.shu.technique.release.cooperative;

import com.bslx.XueXiMod.shu.technique.release.cooperative.api.FrozenCarrierCredential;
import com.bslx.XueXiMod.shu.technique.release.water.CarrierWatch;
import com.bslx.XueXiMod.shu.technique.release.water.CastSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 读取当前主手身份。不调用 {@code CarrierWatch.observe}，避免顺手清掉水刃节拍。
 */
public final class CooperativeCarrier {
    private CooperativeCarrier() {
    }

    public static CastSource current(ServerPlayer player, int skillSlot) {
        ItemStack held = player.getMainHandItem();
        int selectedSlot = player.getInventory().selected;
        int generation = CarrierWatch.generation(player.getUUID());
        return new CastSource(skillSlot, held, selectedSlot, generation);
    }

    public static FrozenCarrierCredential freeze(ServerPlayer player, int skillSlot) {
        return new FrozenCarrierCredential(current(player, skillSlot));
    }

    public static boolean stillHeld(ServerPlayer player, FrozenCarrierCredential credential) {
        if (player == null || credential == null) {
            return false;
        }
        return credential.matches(current(player, credential.skillSlot()));
    }
}
