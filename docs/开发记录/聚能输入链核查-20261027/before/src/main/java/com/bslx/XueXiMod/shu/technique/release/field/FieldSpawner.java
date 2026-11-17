package com.bslx.XueXiMod.shu.technique.release.field;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 场地生成在身体中点。链上的索敌和弹道只附加，不改这个点。 */
public final class FieldSpawner {
    private FieldSpawner() {
    }

    public static void releaseFromChain(ServerPlayer player, List<Technique> techniques, int actionIndex,
                                        ReleaseDefinition definition, int copies) {
        boolean attachedLock = false;
        boolean attachedTracking = false;
        Trajectory attachedPath = null;
        for (int earlierIndex = actionIndex - 1; earlierIndex >= 0; earlierIndex--) {
            Technique earlier = techniques.get(earlierIndex);
            if (earlier.getBaseDamage() > 0 && !earlier.isEnchantment()) {
                break;
            }
            ChainRole role = roleOf(earlier);
            if (role == ChainRole.SEEK_ENTITY) {
                attachedLock = true;
            } else if (role == ChainRole.TRAJECTORY_TRACKING) {
                attachedTracking = true;
            } else if (role == ChainRole.TRAJECTORY_DROP && attachedPath == null) {
                ReleaseDefinition pathDefinition = ReleaseRegistry.get(earlier.getId());
                if (pathDefinition != null) {
                    attachedPath = pathDefinition.trajectory();
                }
            }
        }
        releaseAttached(player, definition, copies, attachedLock, attachedPath, attachedTracking);
    }

    public static void releaseAttached(ServerPlayer player, ReleaseDefinition definition, int copies,
                                       boolean attachedLock, Trajectory attachedPath, boolean attachedTracking) {
        if (player == null || definition == null || definition.fieldRadius() <= 0f || copies <= 0) {
            return;
        }
        Vec3 anchor = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
        int made = 0;
        while (made < copies) {
            FieldZones.add(player, anchor, definition.fieldRadius(), definition.fieldLifetimeTicks(),
                    definition.fieldUpkeepCost(), definition.fieldUpkeepPeriod(), attachedPath, attachedLock, attachedTracking,
                    definition.healAmount(), definition.pullStrength(), definition.wetnessGrant(),
                    definition.purifyOnHit(), definition.pressurePulse(), definition.erosionPulse(),
                    definition.unloadRatio(), definition.slowTicks(), definition.endBurstDamage());
            made += 1;
        }
    }

    private static ChainRole roleOf(Technique technique) {
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition == null) {
            return ChainRole.ATTACK;
        }
        return definition.role();
    }
}
