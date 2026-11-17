package com.bslx.XueXiMod.player;

import net.minecraft.world.entity.player.Player;

/** 已学会的雾雨和深海重压各发一次领域许可。不按熟练等级补学领域。 */
public final class DomainLearning {
    private static final int MIGRATION_VERSION = 2;

    private DomainLearning() {
    }

    public static void onLogin(Player player) {
        migrate(player);
    }

    private static void migrate(Player player) {
        PlayerShuData data = PlayerShuData.get(player);
        int version = data.domainMigrateVersion();
        if (version >= MIGRATION_VERSION) {
            return;
        }
        if (version < 1 && data.hasLearned("water_mist")) {
            data.grantDomainPermit("water_mist");
        }
        if (data.hasLearned("water_deep_pressure")) {
            data.grantDomainPermit("water_deep_pressure");
        }
        data.setDomainMigrateVersion(MIGRATION_VERSION);
        PlayerShuData.save(player, data);
    }
}
