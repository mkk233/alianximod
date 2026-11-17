package com.bslx.XueXiMod.shu.technique.release.firecast;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.balance.BalanceService;
import com.bslx.XueXiMod.shu.technique.enchant.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.release.fire.BurnCarrier;
import com.bslx.XueXiMod.shu.technique.release.fire.FireConsumption;
import com.bslx.XueXiMod.shu.technique.release.fire.FireDamage;
import com.bslx.XueXiMod.shu.technique.release.fire.FireLayer;
import com.bslx.XueXiMod.shu.technique.release.fire.FireVisualRelay;
import com.bslx.XueXiMod.status.fire.furnace.FurnaceService;
import com.bslx.XueXiMod.status.fire.seed.FireCastContext;
import com.bslx.XueXiMod.status.fire.seed.FireSeedState;
import com.bslx.XueXiMod.visual.fire.FireVisualLayout;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Optional;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 地表火场。不放原版火，不改方块，不毁掉落物。
 * 有效区是随 12 刻展开长大的方形，低矮贴地分片与实体碰撞盒相交。
 * 本体与附着燃烧同时结算。火场只活在这次服务器进程里。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class GroundFireFields {
    public static final double BASE_RADIUS = 10.0d;
    public static final int FIELD_LIFE = 200;
    public static final double VERTICAL_SLACK = 3.0d;
    public static final double LAYER_HEIGHT = 1.0d;
    public static final double GROUND_DAMAGE = 4.0d;
    public static final int BODY_INTERVAL = 10;
    private static final int SAMPLE_PERIOD = 20;

    private static final CopyOnWriteArrayList<Zone> ACTIVE = new CopyOnWriteArrayList<>();
    private static final ArrayList<LivingEntity> OPENING_TARGETS = new ArrayList<>();
    private static final AtomicLong NEXT_ZONE = new AtomicLong(1L);

    private GroundFireFields() {
    }

    /** 已配置的燎原半径。火场 open 使用调用方传入的半径，不再读这一次。 */
    public static double configuredRadius() {
        return BalanceService.number(SpecialBalance.LIAOYUAN, BalanceKeys.RADIUS);
    }

    public static void open(ServerLevel level, FireCastContext cast, Vec3 origin, double radius, int lifeTicks) {
        if (level == null || cast == null || origin == null || level.isClientSide()) {
            return;
        }
        if (!Double.isFinite(radius) || radius <= 0.0d || lifeTicks <= 0) {
            return;
        }
        int nowTick = level.getServer().getTickCount();
        long zoneId = NEXT_ZONE.getAndIncrement();
        int expireTick = nowTick + lifeTicks;
        long birthGameTime = level.getGameTime();
        ACTIVE.add(new Zone(level, cast, origin, radius, expireTick, birthGameTime, zoneId));
        FireVisualRelay.openGround(level, zoneId, origin, radius, birthGameTime, birthGameTime + lifeTicks);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }
        int nowTick = event.getServer().getTickCount();
        List<Zone> snapshot = List.copyOf(ACTIVE);
        OPENING_TARGETS.clear();
        try {
            for (Zone zone : snapshot) {
                if (nowTick >= zone.expireTick) {
                    retire(zone);
                    continue;
                }
                zone.survey(event.getServer(), nowTick);
            }
            settleOpenings();
            for (Zone zone : snapshot) {
                if (nowTick >= zone.expireTick || !ACTIVE.contains(zone)) {
                    continue;
                }
                zone.payBodies(event.getServer());
            }
        } finally {
            settleOpenings();
            OPENING_TARGETS.clear();
        }
    }

    private static void rememberOpening(LivingEntity target) {
        if (target == null) {
            return;
        }
        int cursor = 0;
        while (cursor < OPENING_TARGETS.size()) {
            if (OPENING_TARGETS.get(cursor) == target) {
                return;
            }
            cursor += 1;
        }
        OPENING_TARGETS.add(target);
    }

    private static void settleOpenings() {
        int cursor = 0;
        while (cursor < OPENING_TARGETS.size()) {
            BurnCarrier.settleOpening(OPENING_TARGETS.get(cursor));
            cursor += 1;
        }
    }

    private static double furnaceBonus(ServerPlayer caster) {
        if (caster == null) {
            return 0.0d;
        }
        double queried = FurnaceService.fireBonus(caster);
        if (!Double.isFinite(queried) || queried < 0.0d) {
            return 0.0d;
        }
        return queried;
    }

    private static void retire(Zone zone) {
        if (ACTIVE.remove(zone)) {
            FireVisualRelay.closeGround(zone.level, zone.zoneId);
        }
    }

    private static final class Zone {
        private final ServerLevel level;
        private final FireCastContext cast;
        private final Vec3 origin;
        private final double radius;
        private final int expireTick;
        private final long birthGameTime;
        private final long zoneId;
        private final HashMap<UUID, ContactState> contacts = new HashMap<>();
        private final ArrayList<LivingEntity> bodyDue = new ArrayList<>();
        private List<LiaoyuanGroundContact.ColumnSample> columns = List.of();
        private int samplePeriod = Integer.MIN_VALUE;
        private int surveyedTick;

        private Zone(ServerLevel level, FireCastContext cast, Vec3 origin, double radius, int expireTick,
                     long birthGameTime, long zoneId) {
            this.level = level;
            this.cast = cast;
            this.origin = origin;
            this.radius = radius;
            this.expireTick = expireTick;
            this.birthGameTime = birthGameTime;
            this.zoneId = zoneId;
        }

        private void survey(MinecraftServer server, int nowTick) {
            this.bodyDue.clear();
            this.surveyedTick = nowTick;
            if (this.level != server.getLevel(this.level.dimension())) {
                return;
            }
            ServerPlayer caster = server.getPlayerList().getPlayer(this.cast.casterId());
            if (caster == null || !caster.isAlive()) {
                return;
            }
            double ageTicks = this.level.getGameTime() - this.birthGameTime;
            double covered = FireVisualLayout.coveredRadius(this.radius, ageTicks);
            if (covered <= 0.0d) {
                return;
            }
            this.ensureColumns(nowTick);
            AABB area = new AABB(this.origin, this.origin).inflate(covered, VERTICAL_SLACK + LAYER_HEIGHT, covered);
            List<LivingEntity> found = this.level.getEntitiesOfClass(LivingEntity.class, area,
                    living -> DamagePermission.allows(caster, living));
            for (LivingEntity target : found) {
                if (!this.touches(target, covered)) {
                    continue;
                }
                ContactState state = this.contacts.get(target.getUUID());
                if (state == null) {
                    state = new ContactState();
                    this.contacts.put(target.getUUID(), state);
                }
                if (nowTick >= state.nextBodyTick) {
                    this.bodyDue.add(target);
                }
                if (!state.burnClaimed && FireSeedState.value(target) > 0) {
                    if (BurnCarrier.read(target).duplicate(this.cast.castId(), target.getUUID(),
                            FireConsumerPlans.SPREAD_KEY)) {
                        state.burnClaimed = true;
                    } else if (FireConsumption.scheduleOpening(target, this.cast, FireConsumerPlans.SPREAD_KEY,
                            FireConsumerPlans::spreadSpec).isPresent()) {
                        state.burnClaimed = true;
                        rememberOpening(target);
                    }
                }
            }
        }

        private void payBodies(MinecraftServer server) {
            if (this.level != server.getLevel(this.level.dimension())) {
                this.bodyDue.clear();
                return;
            }
            ServerPlayer caster = server.getPlayerList().getPlayer(this.cast.casterId());
            if (caster == null || !caster.isAlive()) {
                this.bodyDue.clear();
                return;
            }
            for (LivingEntity target : this.bodyDue) {
                double furnaceBonus = furnaceBonus(caster);
                double rawDamage = GROUND_DAMAGE * (1.0d + furnaceBonus);
                Optional<Double> settled = SpecialBalance.settleConfigured(SpecialBalance.LIAOYUAN, rawDamage);
                if (settled.isEmpty() || !FireLayer.acceptableSettled(settled.get())) {
                    continue;
                }
                boolean landed = FireDamage.strikeOnce(target, this.cast.casterId(), settled.get(), 0.0d, true);
                if (!landed) {
                    continue;
                }
                ContactState state = this.contacts.get(target.getUUID());
                if (state != null && state.nextBodyTick <= this.surveyedTick) {
                    state.nextBodyTick = this.surveyedTick + BODY_INTERVAL;
                }
            }
            this.bodyDue.clear();
        }

        private void ensureColumns(int nowTick) {
            int period = nowTick / SAMPLE_PERIOD;
            if (period == this.samplePeriod) {
                return;
            }
            this.samplePeriod = period;
            this.columns = LiaoyuanGroundContact.collect(this.level, this.origin.x, this.origin.y, this.origin.z,
                    this.radius);
        }

        private boolean touches(LivingEntity target, double covered) {
            AABB body = target.getBoundingBox();
            for (LiaoyuanGroundContact.ColumnSample column : this.columns) {
                if (column.meets(this.origin.x, this.origin.z, covered, LAYER_HEIGHT, body)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** 同一火场对一个目标只记一次本体节拍和一次挂层。离场不重置。 */
    private static final class ContactState {
        private int nextBodyTick;
        private boolean burnClaimed;
    }
}
