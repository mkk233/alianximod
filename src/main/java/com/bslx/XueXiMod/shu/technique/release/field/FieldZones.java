package com.bslx.XueXiMod.shu.technique.release.field;

import com.bslx.XueXiMod.player.MagicPool;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactCarrier;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactEnchantState;
import com.bslx.XueXiMod.shu.technique.release.TechniqueHits;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.status.WetnessState;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

/**
 * 服务端球形场地表。重叠允许；维护费失败、死亡、下线或换维度就拆掉。
 * 领域存在期间不收维护，护体不进这张表，也不增加场地层数。
 */
public final class FieldZones {
    private static final ArrayList<FieldZone> ZONES = new ArrayList<>();

    private FieldZones() {
    }

    /** 用基础输入登记场地。主人和锚点无效时不登记，维护留到后续节拍。 */
    public static void add(ServerPlayer owner, Vec3 anchor, double radius, int lifetimeTicks,
                           int upkeepCost, int upkeepPeriod, Trajectory attachedPath, boolean attachedLock, boolean attachedTracking,
                           float healAmount, float pullStrength, int wetnessGrant, boolean purify,
                           float pressurePulse, float erosionPulse, float unloadRatio, int slowTicks,
                           float endBurstDamage) {
        add(owner, anchor, radius, lifetimeTicks, upkeepCost, upkeepPeriod, attachedPath, attachedLock, attachedTracking,
                healAmount, pullStrength, wetnessGrant, purify, pressurePulse, erosionPulse, unloadRatio, slowTicks,
                endBurstDamage, ReleaseInput.BASE);
    }

    /** 按释放输入登记场地。半径非正、锚点为空或不是服务端时直接返回。 */
    public static void add(ServerPlayer owner, Vec3 anchor, double radius, int lifetimeTicks,
                           int upkeepCost, int upkeepPeriod, Trajectory attachedPath, boolean attachedLock, boolean attachedTracking,
                           float healAmount, float pullStrength, int wetnessGrant, boolean purify,
                           float pressurePulse, float erosionPulse, float unloadRatio, int slowTicks,
                           float endBurstDamage, ReleaseInput input) {
        if (owner == null || anchor == null || radius <= 0.0 || !(owner.level() instanceof ServerLevel level)) {
            return;
        }
        int now = level.getServer().getTickCount();
        PlannedZone planned = plan(radius, lifetimeTicks, input, now);
        int period = upkeepPeriod > 0 ? upkeepPeriod : 20;
        FieldZone zone = new FieldZone(owner.getUUID(), level.dimension(), anchor, planned.radius, planned.expireTick,
                now + period, Math.max(0, upkeepCost), period, attachedPath, attachedLock, attachedTracking,
                healAmount, pullStrength, wetnessGrant, purify, pressurePulse, erosionPulse,
                unloadRatio, slowTicks, endBurstDamage);
        zone.bornTick = now;
        zone.serverLevel = level;
        ZONES.add(zone);
    }

    /** 按释放输入缩放半径。输入为空时用基础倍率。 */
    public static double scaledRadius(double radius, ReleaseInput input) {
        ReleaseInput frozen = input == null ? ReleaseInput.BASE : input;
        return radius * frozen.sizeMultiplier();
    }

    /** 非正寿命保持原场地的 200 tick 回退，不再乘一次聚能。 */
    public static int scaledLifetime(int lifetimeTicks, ReleaseInput input) {
        ReleaseInput frozen = input == null ? ReleaseInput.BASE : input;
        int scaled = frozen.scaleDuration(lifetimeTicks);
        return scaled > 0 ? scaled : 200;
    }

    /** 按半径计算收招距离。最小保持 1 格。 */
    public static double burstReach(double radius) {
        return Math.max(1.0, radius * 0.45);
    }

    /** 同时冻结缩放半径、寿命、收招距离和到期刻。不扣资源。 */
    public static PlannedZone plan(double radius, int lifetimeTicks, ReleaseInput input, int now) {
        double scaled = scaledRadius(radius, input);
        int life = scaledLifetime(lifetimeTicks, input);
        int expireTick = life > 0 ? ReleaseInput.expiryTick(now, life) : now + life;
        return new PlannedZone(scaled, life, burstReach(scaled), expireTick);
    }

    /** 判断场地当前是否尚未到期。不延长寿命。 */
    public static boolean withinLife(int now, int expireTick) {
        return !ReleaseInput.hasExpired(now, expireTick);
    }

    public static final class PlannedZone {
        public final double radius;
        public final int lifetimeTicks;
        public final double burstReach;
        public final int expireTick;

        private PlannedZone(double radius, int lifetimeTicks, double burstReach, int expireTick) {
            this.radius = radius;
            this.lifetimeTicks = lifetimeTicks;
            this.burstReach = burstReach;
            this.expireTick = expireTick;
        }
    }

    /** 拆掉符合旧雾雨数值的场地。不退维护费。 */
    public static void retireLegacyMist() {
        for (FieldZone zone : new ArrayList<>(ZONES)) {
            if (!zone.closed && Math.abs(zone.radius - 4.5) < 0.05 && zone.wetnessGrant == 2 && zone.upkeepCost == 1) {
                retire(zone);
            }
        }
    }

    /** 拆掉符合旧深压数值的场地。不退维护费。 */
    public static void retireLegacyPressure() {
        for (FieldZone zone : new ArrayList<>(ZONES)) {
            if (!zone.closed && Math.abs(zone.radius - 5.0) < 0.05 && zone.wetnessGrant == 4
                    && zone.upkeepCost == 4 && zone.pressurePulse > 0f && zone.endBurstDamage > 0f) {
                retire(zone);
            }
        }
    }

    /** 给同一主人、同一出生刻且尚未绑定的场地接上接触。已绑定的不覆盖。 */
    public static void bindBorn(java.util.UUID ownerId, EnchantmentSnapshot snapshot, int bornTick) {
        for (FieldZone zone : ZONES) {
            if (zone.closed || zone.contact != null || !zone.ownerId.equals(ownerId) || zone.bornTick != bornTick) {
                continue;
            }
            zone.contact = ContactEnchantState.open(new FieldTouch(zone), snapshot);
        }
    }

    /** 拆掉这个主人的全部场地。不退已收维护费。 */
    public static void closeOwned(UUID ownerId) {
        for (FieldZone zone : ZONES) {
            if (zone.ownerId.equals(ownerId)) {
                retire(zone);
            }
        }
    }

    /** 读取包住玩家的场地影响。没有覆盖时返回空影响；伤害以外的效果只保留一份。 */
    public static FieldInfluence influence(ServerPlayer player) {
        if (player == null) {
            return FieldInfluence.NONE;
        }
        int occupied = 0;
        Trajectory attachedPath = null;
        boolean attachedLock = false;
        boolean attachedTracking = false;
        float healAmount = 0f;
        float pullStrength = 0f;
        int wetnessGrant = 0;
        boolean purify = false;
        Vec3 pullAnchor = null;
        for (FieldZone zone : ZONES) {
            if (!covers(zone, player)) {
                continue;
            }
            occupied += 1;
            attachedTracking |= zone.attachedTracking;
            if (attachedPath == null && zone.attachedPath != null) {
                attachedPath = zone.attachedPath;
            }
            if (!attachedLock && zone.attachedLock) {
                attachedLock = true;
            }
            if (healAmount <= 0f && zone.healAmount > 0f) {
                healAmount = zone.healAmount;
            }
            if (pullStrength <= 0f && zone.pullStrength > 0f) {
                pullStrength = zone.pullStrength;
                pullAnchor = zone.anchor;
            }
            if (wetnessGrant <= 0 && zone.wetnessGrant > 0) {
                wetnessGrant = zone.wetnessGrant;
            }
            if (!purify && zone.purify) {
                purify = true;
            }
        }
        if (occupied <= 0) {
            return FieldInfluence.NONE;
        }
        return new FieldInfluence(occupied, attachedPath, attachedLock, attachedTracking, healAmount, pullStrength,
                wetnessGrant, purify, pullAnchor);
    }

    /** 多块场地的卸力不叠加，取最大的一份。 */
    public static float unloadRatio(ServerPlayer player) {
        float ratio = 0f;
        if (player == null) {
            return 0f;
        }
        for (FieldZone zone : ZONES) {
            if (zone.unloadRatio > ratio && covers(zone, player)) {
                ratio = zone.unloadRatio;
            }
        }
        if (ratio < 0f) {
            return 0f;
        }
        return ratio > 0.6f ? 0.6f : ratio;
    }

    /** 推进场地寿命、主人和维护。法力不足、主人失效或到期就拆掉，不另退款。 */
    public static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        Iterator<FieldZone> iterator = ZONES.iterator();
        while (iterator.hasNext()) {
            FieldZone zone = iterator.next();
            if (zone.closed) {
                retire(zone);
                iterator.remove();
                continue;
            }
            if (!withinLife(now, zone.expireTick)) {
                endBurst(server, zone);
                retire(zone);
                iterator.remove();
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(zone.ownerId);
            if (owner == null || !owner.isAlive() || owner.level().dimension() != zone.dimension) {
                retire(zone);
                iterator.remove();
                continue;
            }
            ServerLevel level = server.getLevel(zone.dimension);
            if (level == null) {
                retire(zone);
                iterator.remove();
                continue;
            }
            if (now >= zone.nextUpkeepTick) {
                if (!MagicPool.trySpend(owner, zone.upkeepCost, false)) {
                    retire(zone);
                    iterator.remove();
                    continue;
                }
                zone.nextUpkeepTick = now + zone.upkeepPeriod;
            }
            if ((now & 15) == 0) {
                paint(level, zone);
            }
        }
        if (now % 10 == 0) {
            pulse(server);
        }
    }

    private static void paint(ServerLevel level, FieldZone zone) {
        double radius = zone.radius;
        for (int step = 0; step < 8; step++) {
            double angle = step * Math.PI / 4.0;
            double pointX = zone.anchor.x + Math.cos(angle) * radius;
            double pointZ = zone.anchor.z + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.ENCHANT, pointX, zone.anchor.y + 0.2, pointZ, 1, 0.0, 0.05, 0.0, 0.0);
        }
    }

    /** 压强、侵蚀、灭火各按一份生效，不按场地份数乘，也不把多块场地的数值加在一起。 */
    private static void pulse(MinecraftServer server) {
        ArrayList<BodyPulse> pending = new ArrayList<>();
        for (FieldZone zone : ZONES) {
            if (zone.closed || !hasPulse(zone)) {
                continue;
            }
            ServerLevel level = server.getLevel(zone.dimension);
            ServerPlayer owner = server.getPlayerList().getPlayer(zone.ownerId);
            if (level == null || owner == null) {
                continue;
            }
            AABB box = new AABB(zone.anchor, zone.anchor).inflate(zone.radius);
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
                if (living.getUUID().equals(zone.ownerId) || living.isSpectator()) {
                    continue;
                }
                if (!coversBody(zone.anchor, zone.radius, living.getBoundingBox())) {
                    continue;
                }
                BodyPulse current = bodyPulse(pending, living);
                if (current.owner == null) {
                    current.owner = owner;
                }
                if (current.pressure <= 0f && zone.pressurePulse > 0f) {
                    current.pressure = zone.pressurePulse;
                }
                if (current.erosion <= 0f && zone.erosionPulse > 0f) {
                    current.erosion = zone.erosionPulse;
                }
                if (!current.purify && zone.purify) {
                    current.purify = true;
                }
                if (current.wetnessGrant <= 0 && zone.wetnessGrant > 0) {
                    current.wetnessGrant = zone.wetnessGrant;
                }
                if (current.pullStrength <= 0f && zone.pullStrength > 0f) {
                    current.pullStrength = zone.pullStrength;
                    current.pullAnchor = zone.anchor;
                }
                if (current.slowTicks <= 0 && zone.slowTicks > 0) {
                    current.slowTicks = zone.slowTicks;
                }
            }
        }
        for (BodyPulse current : pending) {
            if (current.owner == null) {
                continue;
            }
            if (current.purify) {
                current.living.clearFire();
            }
            if (current.pressure > 0f) {
                TechniqueHits.hurt(current.living, current.owner, current.pressure);
            }
            if (current.erosion > 0f) {
                TechniqueHits.hurt(current.living, current.owner, current.erosion);
            }
            if (current.wetnessGrant > 0) {
                WetnessState.grant(current.living, current.wetnessGrant);
            }
            if (current.slowTicks > 0) {
                current.living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, current.slowTicks, 1));
            }
            if (current.pullStrength > 0f && current.pullAnchor != null) {
                Vec3 delta = current.pullAnchor.subtract(current.living.position());
                if (delta.lengthSqr() > 0.04) {
                    Vec3 pull = delta.normalize().scale(current.pullStrength);
                    current.living.setDeltaMovement(current.living.getDeltaMovement().add(pull.x, pull.y * 0.2, pull.z));
                    current.living.hurtMarked = true;
                }
            }
        }
    }

    private static boolean hasPulse(FieldZone zone) {
        return zone.pressurePulse > 0f || zone.erosionPulse > 0f || zone.purify || zone.wetnessGrant > 0
                || zone.pullStrength > 0f || zone.slowTicks > 0;
    }

    /** 只有正常到期才收招。死亡、下线和维持失败只拆场地。 */
    private static void retire(FieldZone zone) {
        zone.closed = true;
        if (zone.contact != null) {
            zone.contact.close();
            zone.contact = null;
        }
    }

    private static void endBurst(MinecraftServer server, FieldZone zone) {
        if (zone.endBurstDamage <= 0f) {
            return;
        }
        ServerLevel level = server.getLevel(zone.dimension);
        ServerPlayer owner = server.getPlayerList().getPlayer(zone.ownerId);
        if (level == null || owner == null) {
            return;
        }
        AABB box = new AABB(zone.anchor, zone.anchor).inflate(zone.burstReach);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (living.getUUID().equals(zone.ownerId) || living.isSpectator()) {
                continue;
            }
            TechniqueHits.hurt(living, owner, zone.endBurstDamage);
        }
    }

    private static BodyPulse bodyPulse(ArrayList<BodyPulse> pending, LivingEntity living) {
        for (BodyPulse current : pending) {
            if (current.living == living) {
                return current;
            }
        }
        BodyPulse created = new BodyPulse(living);
        pending.add(created);
        return created;
    }

    private static final class BodyPulse {
        final LivingEntity living;
        ServerPlayer owner;
        float pressure;
        float erosion;
        boolean purify;
        int wetnessGrant;
        float pullStrength;
        Vec3 pullAnchor;
        int slowTicks;

        BodyPulse(LivingEntity living) {
            this.living = living;
        }
    }

    static boolean covers(FieldZone zone, ServerPlayer player) {
        if (zone.closed || player.level().dimension() != zone.dimension) {
            return false;
        }
        return coversBody(zone.anchor, zone.radius, player.getBoundingBox());
    }

    private static boolean coversBody(Vec3 anchor, double radius, AABB body) {
        double closestX = clampTo(anchor.x, body.minX, body.maxX);
        double closestY = clampTo(anchor.y, body.minY, body.maxY);
        double closestZ = clampTo(anchor.z, body.minZ, body.maxZ);
        double deltaX = anchor.x - closestX;
        double deltaY = anchor.y - closestY;
        double deltaZ = anchor.z - closestZ;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ <= radius * radius;
    }

    private static double clampTo(double amount, double lower, double upper) {
        if (amount < lower) {
            return lower;
        }
        if (amount > upper) {
            return upper;
        }
        return amount;
    }

    private static final class FieldTouch implements ContactCarrier {
        private final FieldZone zone;

        private FieldTouch(FieldZone zone) {
            this.zone = zone;
        }

        @Override
        public java.util.UUID hostId() {
            return zone.hostId;
        }

        @Override
        public int revision() {
            return 1;
        }

        @Override
        public java.util.UUID ownerId() {
            return zone.ownerId;
        }

        @Override
        public boolean active() {
            return !zone.closed && zone.serverLevel != null;
        }

        @Override
        public boolean touches(LivingEntity target) {
            return target != null && coversBody(zone.anchor, zone.radius, target.getBoundingBox());
        }

        @Override
        public ServerLevel hostLevel() {
            return zone.serverLevel;
        }

        @Override
        public AABB queryBox() {
            return new AABB(zone.anchor, zone.anchor).inflate(zone.radius + 1.0);
        }
    }
}
