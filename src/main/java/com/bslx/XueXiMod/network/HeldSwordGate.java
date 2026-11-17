package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.item.StaffItem;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 释放定义上的持剑要求。不按术式编号分支。 */
public final class HeldSwordGate {
    public static final String STAFF_HINT = "缺月已改为剑技，请取回并装入剑技槽";
    public static final String SWORD_HINT = "需要手持剑";

    private static final int TELL_GAP = 40;
    private static final ConcurrentHashMap<UUID, Integer> LAST_TOLD = new ConcurrentHashMap<>();

    private HeldSwordGate() {
    }

    public static boolean allows(Technique action, ItemStack hand) {
        if (action == null) {
            return true;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(action.getId());
        return allows(definition, hand);
    }

    public static boolean allows(ReleaseDefinition definition, ItemStack hand) {
        if (definition == null || !definition.requiresHeldSword()) {
            return true;
        }
        return hand != null && !hand.isEmpty() && hand.getItem() instanceof SwordItem;
    }

    /** 整链里任一术式要求持剑、而主手不是剑时返回提示。没有要求则返回 null。 */
    public static String refusal(ItemStack weapon, List<Technique> techniques) {
        if (techniques == null || weapon == null) {
            return null;
        }
        boolean needsSword = false;
        for (Technique technique : techniques) {
            if (technique == null) {
                continue;
            }
            ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
            if (definition != null && definition.requiresHeldSword()) {
                needsSword = true;
                break;
            }
        }
        if (!needsSword || weapon.getItem() instanceof SwordItem) {
            return null;
        }
        if (StaffItem.holds(weapon)) {
            return STAFF_HINT;
        }
        return SWORD_HINT;
    }

    public static void tellLimited(ServerPlayer player, String messageText) {
        if (player == null || player.getServer() == null || messageText == null || messageText.isEmpty()) {
            return;
        }
        int nowTick = player.getServer().getTickCount();
        Integer previousTick = LAST_TOLD.get(player.getUUID());
        if (previousTick != null && nowTick < previousTick) {
            LAST_TOLD.remove(player.getUUID());
            previousTick = null;
        }
        if (previousTick != null && nowTick - previousTick < TELL_GAP) {
            return;
        }
        LAST_TOLD.put(player.getUUID(), nowTick);
        player.sendSystemMessage(Component.literal("§c" + messageText));
    }
}
