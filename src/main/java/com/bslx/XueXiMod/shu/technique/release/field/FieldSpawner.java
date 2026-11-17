package com.bslx.XueXiMod.shu.technique.release.field;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.bind.PublicationAttempt;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 球形场地生成服务。场地生成在身体中点，链上的索敌和弹道只附加，不改这个点。
 * 领域按声明落固定盒子，护体排在主人身上，都不从这里生成。
 */
public final class FieldSpawner {
    private FieldSpawner() {
    }

    /** 从基础输入的术式链生成场地。参数无效或发布被拒时不添加场地。 */
    public static void releaseFromChain(ServerPlayer player, List<Technique> techniques, int actionIndex,
                                        ReleaseDefinition definition, int copies) {
        releaseFromChain(player, techniques, actionIndex, definition, copies, ReleaseInput.BASE);
    }

    /** 从术式链收集锁定和弹道后生成场地。遇到更早的攻击声明就停止回看。 */
    public static void releaseFromChain(ServerPlayer player, List<Technique> techniques, int actionIndex,
                                        ReleaseDefinition definition, int copies, ReleaseInput input) {
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
            } else if ((role == ChainRole.TRAJECTORY_DROP || role == ChainRole.TRAJECTORY_PARALLEL) && attachedPath == null) {
                ReleaseDefinition pathDefinition = ReleaseRegistry.get(earlier.getId());
                if (pathDefinition != null) {
                    attachedPath = pathDefinition.trajectory();
                }
            }
        }
        releaseAttached(player, definition, copies, attachedLock, attachedPath, attachedTracking, input);
    }

    /** 用基础输入生成已附加索敌和弹道的场地。参数无效时不添加。 */
    public static void releaseAttached(ServerPlayer player, ReleaseDefinition definition, int copies,
                                       boolean attachedLock, Trajectory attachedPath, boolean attachedTracking) {
        releaseAttached(player, definition, copies, attachedLock, attachedPath, attachedTracking, ReleaseInput.BASE);
    }

    /** 逐份生成场地。发布尝试不允许、半径非正或锚点无效时停止，不扣这里的费用。 */
    public static void releaseAttached(ServerPlayer player, ReleaseDefinition definition, int copies,
                                       boolean attachedLock, Trajectory attachedPath, boolean attachedTracking,
                                       ReleaseInput input) {
        if (player == null || definition == null || definition.fieldRadius() <= 0f || copies <= 0) {
            return;
        }
        PublicationAttempt attempt = PublicationAttempt.current();
        if (attempt != null && !attempt.allowsPublication()) {
            return;
        }
        ReleaseInput frozen = input == null ? ReleaseInput.BASE : input;
        Vec3 anchor = anchorAt(player.position(), player.getBbHeight());
        boolean storedZone = anchor != null && player.level() instanceof ServerLevel;
        int made = 0;
        while (made < copies) {
            FieldZones.add(player, anchor, definition.fieldRadius(), definition.fieldLifetimeTicks(),
                    definition.fieldUpkeepCost(), definition.fieldUpkeepPeriod(), attachedPath, attachedLock, attachedTracking,
                    definition.healAmount(), definition.pullStrength(), definition.wetnessGrant(),
                    definition.purifyOnHit(), definition.pressurePulse(), definition.erosionPulse(),
                    definition.unloadRatio(), definition.slowTicks(), definition.endBurstDamage(), frozen);
            made += 1;
            if (storedZone && attempt != null) {
                attempt.reportAccepted();
            }
        }
    }

    /** 把脚底抬到身体中点。不检查世界碰撞。 */
    public static Vec3 anchorAt(Vec3 feet, double bodyHeight) {
        return feet.add(0.0, bodyHeight * 0.5, 0.0);
    }

    private static ChainRole roleOf(Technique technique) {
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition == null) {
            return ChainRole.ATTACK;
        }
        return definition.role();
    }
}
