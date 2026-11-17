package com.bslx.XueXiMod.event;

import com.bslx.XueXiMod.network.BoundSkillSyncPacket;
import com.bslx.XueXiMod.network.ModNetwork;
import com.bslx.XueXiMod.network.ShuSyncPacket;
import com.bslx.XueXiMod.player.PlayerShuData;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;

public class PlayerConnectionHandler {

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        PlayerShuData data = PlayerShuData.get(player);

        for (Technique technique : TechniqueManager.playerCatalog()) {
            if (technique.getUnlockLevel() == 0 && !data.hasLearned(technique.getId())) {
                data.learnTechnique(technique.getId());
            }
        }

        PlayerShuData.save(player, data);
        com.bslx.XueXiMod.player.DomainLearning.onLogin(player);

        if (player instanceof ServerPlayer serverPlayer) {
            BoundSkillSyncPacket.send(serverPlayer);
            ModNetwork.sendToPlayer(serverPlayer, new ShuSyncPacket(data));
        }
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        CompoundTag oldData = event.getOriginal().getPersistentData();
        CompoundTag newData = event.getEntity().getPersistentData();
        copyKey(oldData, newData, "xueximod_bound_skills");
        copyKey(oldData, newData, "xueximod_shu_data");
        copyKey(oldData, newData, "xueximod_spell_storage");
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            BoundSkillSyncPacket.send(serverPlayer);
            ModNetwork.sendToPlayer(serverPlayer, new ShuSyncPacket(PlayerShuData.get(serverPlayer)));
        }
    }

    private static void copyKey(CompoundTag from, CompoundTag target, String key) {
        if (from.contains(key)) {
            target.put(key, from.get(key).copy());
        }
    }

    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player != null) {
            PlayerShuData.save(player, PlayerShuData.get(player));
        }
    }
}
