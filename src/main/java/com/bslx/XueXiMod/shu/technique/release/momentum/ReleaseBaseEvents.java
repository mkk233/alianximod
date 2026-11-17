package com.bslx.XueXiMod.shu.technique.release.momentum;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.network.GuardSyncPacket;
import com.bslx.XueXiMod.network.DomainSyncPacket;
import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierService;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainMotion;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainService;
import com.bslx.XueXiMod.shu.technique.release.barrier.ProjectileBarrierTracker;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactEnchantState;
import com.bslx.XueXiMod.shu.technique.release.field.FieldZones;
import com.bslx.XueXiMod.shu.technique.release.deflection.MitigationCreditScope;
import com.bslx.XueXiMod.shu.technique.release.guard.GuardMitigation;
import com.bslx.XueXiMod.shu.technique.release.guard.GuardPose;
import com.bslx.XueXiMod.shu.technique.release.guard.GuardStates;
import com.bslx.XueXiMod.shu.technique.release.parting.WaterPartingService;
import com.bslx.XueXiMod.shu.technique.release.prison.PrisonPressure;
import com.bslx.XueXiMod.shu.technique.release.payload.ObstacleBodies;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 水势按实际掉血记账。场地维护和障碍推动也在这一拍。 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class ReleaseBaseEvents {
    private ReleaseBaseEvents() {
    }

    /** 先按屏障保留比例改箭伤，再按场地或格挡比例减伤。格挡更高时交给格挡入账，否则按减伤策略记水势。 */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0f) {
            return;
        }
        if (PrisonPressure.matches(event.getSource())) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        Entity directEntity = event.getSource().getDirectEntity();
        if (directEntity instanceof AbstractArrow arrow) {
            float kept = ProjectileBarrierTracker.keptFraction(arrow);
            if (kept < 1f) {
                event.setAmount(event.getAmount() * kept);
            }
        }
        if (!(victim instanceof ServerPlayer player) || event.getAmount() <= 0f) {
            return;
        }
        float fieldRatio = FieldZones.unloadRatio(player);
        boolean bypassGuard = event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        float guardRatio = bypassGuard ? 0f : GuardStates.unloadRatio(player);
        float ratio = fieldRatio > guardRatio ? fieldRatio : guardRatio;
        if (ratio <= 0f) {
            return;
        }
        float before = event.getAmount();
        float reduced = before * ratio;
        if (reduced <= 0f) {
            return;
        }
        event.setAmount(before - reduced);
        int poseKind = hurtPose(event);
        Vec3 incoming = incomingFlat(event, player);
        if (guardRatio > fieldRatio) {
            GuardMitigation.offer(event, player, reduced, poseKind, incoming);
            return;
        }
        if (MitigationCreditScope.allowsMitigatedCredit(reduced,
                MomentumCreditPolicy.suppresses(player, event.getSource()),
                MitigationCreditScope.suppressesMitigated(player, event.getSource()))) {
            WaterMomentum.creditMitigated(player, reduced);
        }
        if (guardRatio > 0f && poseKind != GuardPose.NONE) {
            GuardMitigation.offer(event, player, 0f, poseKind, incoming);
        }
    }

    /** 直接来源是弹射物记成箭势；攻击者本人打出的近战记成近战，其余不给格挡姿态。 */
    private static int hurtPose(LivingHurtEvent event) {
        Entity direct = event.getSource().getDirectEntity();
        Entity attacker = event.getSource().getEntity();
        if (direct instanceof Projectile) {
            return GuardPose.ARROW;
        }
        if (attacker instanceof LivingEntity && direct == attacker) {
            return GuardPose.MELEE;
        }
        return GuardPose.NONE;
    }

    /** 取直接来源到玩家的水平方向。没有直接来源时为零。 */
    private static Vec3 incomingFlat(LivingHurtEvent event, ServerPlayer player) {
        Entity direct = event.getSource().getDirectEntity();
        if (direct == null) {
            return Vec3.ZERO;
        }
        Vec3 delta = direct.position().subtract(player.position());
        return new Vec3(delta.x, 0.0, delta.z);
    }

    /** 按实际掉血给受害玩家和攻击玩家入账，再按潮湿档位给近战或箭矢补一笔。水牢压力不加这份额外奖励。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        boolean prisonPressure = PrisonPressure.matches(event.getSource());
        if (!prisonPressure) {
            com.bslx.XueXiMod.shu.technique.enchant.TideMarkDamageScope.observe(event);
        }
        if (event.isCanceled() || event.getAmount() <= 0f) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        float pool = victim.getHealth() + victim.getAbsorptionAmount();
        float dealt = event.getAmount();
        if (dealt > pool) {
            dealt = pool;
        }
        // 实际掉血和反击。偏转的 partial 不进这里，溺水策略仍只拦溺水本身。
        if (dealt <= 0f || MomentumCreditPolicy.suppresses(victim, event.getSource())) {
            return;
        }
        if (victim instanceof ServerPlayer player) {
            WaterMomentum.credit(player, dealt);
        }
        if (prisonPressure) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (source instanceof ServerPlayer attacker && source != victim) {
            WaterMomentum.credit(attacker, dealt);
        }
        WetAttackReward.grant(victim, event.getSource());
    }

    /** 死亡时结束这位玩家还开着的释放。 */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            endCast(player);
        }
    }

    /** 下线时结束这位玩家还开着的释放。 */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            endCast(player);
        }
    }

    /** 换维度时结束这位玩家还开着的释放。 */
    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            endCast(player);
        }
    }

    /** 每拍结束时推进场地、格挡、障碍、领域、接触附魔、分水和其他释放维护。 */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        FieldZones.tick(event.getServer());
        GuardMitigation.flush();
        GuardStates.tick(event.getServer());
        ObstacleBodies.tick(event.getServer());
        DomainService.tick(event.getServer());
        ContactEnchantState.tick(event.getServer());
        WaterPartingService.tick(event.getServer());
        com.bslx.XueXiMod.network.HoldRepeat.tick(event.getServer());
    }

    /** 区块载入时恢复分水和静水租约。 */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            WaterPartingService.onChunkLoad(serverLevel, event.getChunk());
            com.bslx.XueXiMod.shu.technique.release.domain.motion.StillWaterLease.onChunkLoad(serverLevel, event.getChunk());
        }
    }

    /** 打断蓄力并关掉这位玩家持有的释放、场地、格挡、屏障和领域。 */
    private static void endCast(ServerPlayer player) {
        com.bslx.XueXiMod.network.HoldRepeat.dropAll(player.getUUID());
        ChargeGate.interrupt(player.getUUID());
        WaterPartingService.cancelUncommitted(player.getUUID());
        com.bslx.XueXiMod.network.CastExecutionSession.cancelOwned(player);
        FieldZones.closeOwned(player.getUUID());
        GuardStates.closeOwned(player);
        BarrierService.closeOwned(player);
        DomainService.closeOwned(player);
    }

    /** 开始看见另一位玩家时，补发格挡和领域快照。 */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer watcher) || !(event.getTarget() instanceof ServerPlayer owner)) {
            return;
        }
        GuardSyncPacket.sendSnapshot(watcher, owner);
        DomainSyncPacket.sendSnapshot(watcher);
    }

    /** 实体传送后丢掉领域移动记录。 */
    @SubscribeEvent
    public static void onTeleport(EntityTeleportEvent event) {
        DomainMotion.forget(event.getEntity());
    }
}
