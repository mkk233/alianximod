package com.bslx.XueXiMod.shu.technique.release.cooperative;

import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeTechniqueIds;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ExplicitVolumeGeometry;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.PreparedEmit;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 把适配器已经摆好的世界坐标写成一具体积。不再沿用环境释放帧，也不再乘全局实体倍率。
 */
public final class CooperativeVolumePublisher {
    public static final String OBLIQUE_TEXTURE = "xueximod:textures/entity/xueximod/oblique_slash_v1.png";
    public static final String HORIZONTAL_TEXTURE = "xueximod:textures/entity/xueximod/horizontal_slash.png";

    private CooperativeVolumePublisher() {
    }

    public static boolean publish(ServerLevel level, ServerPlayer player, PreparedEmit emit, String techniqueId,
                                  float damageAmount, ReleaseBatch.Tally tally, int settlementOrdinal,
                                  EnchantmentSnapshot enchantments,
                                  java.util.function.Consumer<com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity> spawned,
                                  @Nullable UUID contactTarget) {
        ExplicitVolumeGeometry geometry = emit.geometry();
        TechniqueHitSpec spec = new TechniqueHitSpec();
        spec.techniqueId = techniqueId;
        spec.path = TechniqueHitSpec.Path.STILL_AIM;
        spec.speed = 0.0D;
        spec.length = geometry.boxDepth();
        spec.width = geometry.boxWidth();
        spec.height = geometry.boxThickness();
        spec.centered = true;
        spec.anchor = geometry.origin();
        spec.aim = geometry.direction();
        spec.pose3d = geometry.pose3d();
        spec.roll = geometry.roll();
        spec.lifetime = emit.lifetimeTicks();
        spec.fixedLifetime = true;
        spec.hitInterval = 0;
        spec.localTargetId = contactTarget;
        spec.settleDamage = damageAmount;
        spec.settleTally = tally;
        spec.settleCopyIndex = settlementOrdinal;
        spec.explicitObb = CooperativeTechniqueIds.OBLIQUE_SLASH.equals(techniqueId);
        if (spec.explicitObb) {
            spec.texture = OBLIQUE_TEXTURE;
        } else if (CooperativeTechniqueIds.HORIZONTAL_SLASH.equals(techniqueId)) {
            spec.texture = HORIZONTAL_TEXTURE;
        }
        if (enchantments != null) {
            spec.settleBurnSeconds = enchantments.burnSeconds;
            spec.settleControlTicks = enchantments.controlTicks;
            spec.settleExtinguish = enchantments.extinguish;
            spec.settleKnockback = enchantments.knockback;
            spec.settleTide = enchantments.tideSuccess();
            spec.settleSeedPoints = enchantments.seedPoints;
        }
        spec.publishedHook = spawned;
        TechniqueHitSpec.SpawnReport report = TechniqueHitSpec.spawnPlanned(level, player, spec);
        return report != null && report.accepted() > 0;
    }
}
