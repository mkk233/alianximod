package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.item.StaffItem;
import com.bslx.XueXiMod.item.StaffSkills;
import com.bslx.XueXiMod.player.PlayerBoundSkills;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.skill.Skill;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 按住释放键时，冷却一结束就放链条里的下一发。松开或装配不合法就停。 */
public final class HoldRepeat {
    private static final ConcurrentHashMap<UUID, boolean[]> HELD = new ConcurrentHashMap<>();

    private HoldRepeat() {
    }

    static void arm(ServerPlayer player, int slot) {
        if (player == null || slot < 0 || slot > 3) {
            return;
        }
        boolean[] slots = HELD.computeIfAbsent(player.getUUID(), playerId -> new boolean[4]);
        slots[slot] = true;
    }

    static void drop(ServerPlayer player, int slot) {
        if (player == null) {
            return;
        }
        drop(player.getUUID(), slot);
    }

    public static void dropAll(UUID playerId) {
        if (playerId != null) {
            HELD.remove(playerId);
        }
    }

    static void clear() {
        HELD.clear();
    }

    public static void tick(MinecraftServer server) {
        if (server == null || HELD.isEmpty()) {
            return;
        }
        for (UUID playerId : List.copyOf(HELD.keySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            boolean[] slots = HELD.get(playerId);
            if (player == null || !player.isAlive() || slots == null) {
                if (player == null || slots == null) {
                    HELD.remove(playerId);
                }
                continue;
            }
            for (int slot = 0; slot < slots.length; slot++) {
                if (!slots[slot]) {
                    continue;
                }
                if (CastCooldown.busy(player, player.getMainHandItem(), slot)) {
                    continue;
                }
                if (waitsForFreshPress(player, slot)) {
                    continue;
                }
                if (!SkillUsePacket.offer(player, slot, false)) {
                    slots[slot] = false;
                }
            }
        }
    }

    public static boolean held(UUID playerId, int slot) {
        boolean[] slots = HELD.get(playerId);
        return slots != null && slot >= 0 && slot < slots.length && slots[slot];
    }

    /** 看这一发真正会放的那条链。法杖不读剑技槽，避免槽里的重新点按术式把法杖连放停住。 */
    private static boolean waitsForFreshPress(ServerPlayer player, int slot) {
        ItemStack heldItem = player.getMainHandItem();
        Skill skill = StaffItem.holds(heldItem) ? StaffSkills.asSkill(heldItem) : PlayerBoundSkills.get(player, slot);
        if (skill == null) {
            return false;
        }
        for (Technique technique : skill.getTechniques()) {
            ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
            if (definition != null && definition.requiresFreshPress()) {
                return true;
            }
        }
        return false;
    }

    private static void drop(UUID playerId, int slot) {
        boolean[] slots = HELD.get(playerId);
        if (slots == null || slot < 0 || slot >= slots.length) {
            return;
        }
        slots[slot] = false;
    }
}
