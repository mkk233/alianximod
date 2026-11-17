package com.bslx.XueXiMod.shu.technique.release.barrier;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactPolicy;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 箭的衰减凭据。只统计真实运动段里墙内长度大于 1e-6 的第一面墙。
 * 重新进入世界时采样点重置到当前位置，不把卸载或传送的空隙算成飞行。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class ProjectileBarrierTracker {
    private static final String MARKED = "xuexi_barrier_cut";
    private static final String OWNER = "xuexi_barrier_owner";
    private static final String KEPT = "xuexi_barrier_kept";
    private static final ConcurrentHashMap<UUID, Vec3> SAMPLE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Boolean> HIT_NOW = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Boolean> ROUTE_HELD = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, UUID> SKIP_BODY = new ConcurrentHashMap<>();

    private ProjectileBarrierTracker() {
    }

    /** 记下这一拍自由飞行的采样起点。受控路线只刷新采样。 */
    public static void noteStart(AbstractArrow arrow) {
        if (arrow == null || arrow.level().isClientSide) {
            return;
        }
        if (ROUTE_HELD.containsKey(arrow.getUUID())) {
            SAMPLE.put(arrow.getUUID(), arrow.position());
            return;
        }
        SAMPLE.putIfAbsent(arrow.getUUID(), arrow.position());
    }

    /**
     * 完整承接前先结算自由飞行段，并跳过被这次承接取代的那面墙。
     * 随后的受控路线只刷新采样，不再按自由飞行穿墙。
     */
    public static void takeOver(AbstractArrow arrow, UUID replacedBodyId) {
        if (arrow == null || arrow.level().isClientSide) {
            return;
        }
        UUID arrowId = arrow.getUUID();
        if (replacedBodyId != null) {
            SKIP_BODY.put(arrowId, replacedBodyId);
        }
        Vec3 sampled = SAMPLE.get(arrowId);
        Vec3 here = arrow.position();
        if (sampled != null) {
            cross(arrow, sampled, here);
        }
        ROUTE_HELD.put(arrowId, Boolean.TRUE);
        SAMPLE.put(arrowId, here);
    }

    /** 放出后从当前位置重新计自由飞行。被承接的那面墙继续跳过，直到调用方清掉。 */
    public static void releaseSampling(AbstractArrow arrow) {
        if (arrow == null) {
            return;
        }
        UUID arrowId = arrow.getUUID();
        ROUTE_HELD.remove(arrowId);
        if (!arrow.level().isClientSide) {
            SAMPLE.put(arrowId, arrow.position());
        }
    }

    /** 清掉被这次承接取代的墙，之后恢复自由飞行采样。 */
    public static void clearReplacedBody(UUID arrowId) {
        if (arrowId != null) {
            SKIP_BODY.remove(arrowId);
        }
    }

    /** 结算到命中点。受控路线不按自由飞行穿墙。 */
    public static void advanceToHit(AbstractArrow arrow, Vec3 hit) {
        if (arrow == null || hit == null || arrow.level().isClientSide) {
            return;
        }
        if (ROUTE_HELD.containsKey(arrow.getUUID())) {
            SAMPLE.put(arrow.getUUID(), arrow.position());
            return;
        }
        Vec3 from = SAMPLE.getOrDefault(arrow.getUUID(), arrow.position());
        cross(arrow, from, hit);
        SAMPLE.put(arrow.getUUID(), hit);
        HIT_NOW.put(arrow.getUUID(), Boolean.TRUE);
    }

    /** 结算这一拍终点。已经在命中点处理过时不重复穿墙。 */
    public static void noteEnd(AbstractArrow arrow) {
        if (arrow == null || arrow.level().isClientSide) {
            return;
        }
        UUID arrowId = arrow.getUUID();
        if (ROUTE_HELD.containsKey(arrowId)) {
            HIT_NOW.remove(arrowId);
            SAMPLE.put(arrowId, arrow.position());
            return;
        }
        if (Boolean.TRUE.equals(HIT_NOW.remove(arrowId))) {
            SAMPLE.put(arrowId, arrow.position());
            return;
        }
        Vec3 from = SAMPLE.getOrDefault(arrowId, arrow.position());
        cross(arrow, from, arrow.position());
        SAMPLE.put(arrowId, arrow.position());
    }

    /** 读取箭穿过第一面墙后保留的伤害比例。没有标记时为 1。 */
    public static float keptFraction(Projectile projectile) {
        if (!(projectile instanceof AbstractArrow arrow)) {
            return 1f;
        }
        CompoundTag tag = arrow.getPersistentData();
        if (!tag.getBoolean(MARKED)) {
            return 1f;
        }
        float kept = tag.getFloat(KEPT);
        return kept > 0f && kept < 1f ? kept : 0.5f;
    }

    private static void cross(AbstractArrow arrow, Vec3 start, Vec3 finish) {
        if (!ContactPolicy.mayAttenuateProjectile(arrow) || arrow.getPersistentData().getBoolean(MARKED)) {
            return;
        }
        BarrierBody first = null;
        double earliest = Double.MAX_VALUE;
        UUID replacedBody = SKIP_BODY.get(arrow.getUUID());
        for (BarrierBody wall : BarrierService.active()) {
            if (wall.level() != arrow.level() || !wall.active()) {
                continue;
            }
            if (replacedBody != null && replacedBody.equals(wall.getUUID())) {
                continue;
            }
            double interior = wall.wall().interiorLength(start, finish);
            if (interior <= AxisAlignedWall.CROSS_EPSILON) {
                continue;
            }
            double order = wall.wall().entryFraction(start, finish);
            if (order < earliest) {
                earliest = order;
                first = wall;
            }
        }
        if (first == null || first.ownerId() == null) {
            return;
        }
        CompoundTag tag = arrow.getPersistentData();
        tag.putBoolean(MARKED, true);
        tag.putUUID(OWNER, first.ownerId());
        tag.putFloat(KEPT, 1f - first.attenuation());
    }

    /** 箭进入世界时重置采样点。 */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof AbstractArrow arrow && !event.getLevel().isClientSide()) {
            SAMPLE.put(arrow.getUUID(), arrow.position());
        }
    }

    /** 箭离开世界时丢掉这一支的采样和跳墙标记。 */
    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof AbstractArrow arrow) {
            UUID arrowId = arrow.getUUID();
            SAMPLE.remove(arrowId);
            HIT_NOW.remove(arrowId);
            ROUTE_HELD.remove(arrowId);
            SKIP_BODY.remove(arrowId);
        }
    }
}
