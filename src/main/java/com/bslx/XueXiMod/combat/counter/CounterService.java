package com.bslx.XueXiMod.combat.counter;

import com.bslx.XueXiMod.combat.counter.CounterLedger.HurtMitigation;
import com.bslx.XueXiMod.combat.counter.CounterLedger.HurtOffer;
import com.bslx.XueXiMod.combat.counter.CounterLedger.OwnerReadiness;
import com.bslx.XueXiMod.combat.counter.CounterLedger.PlannedStrike;
import com.bslx.XueXiMod.combat.counter.CounterLedger.StrikeFacts;
import com.bslx.XueXiMod.shu.technique.release.statusquery.RelationPolicy;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.List;
import java.util.UUID;

/**
 * 通用一次迎击。不注册事件，也不改公共 hurt 包装。
 * 减伤只改当前伤害数值，不取消事件，也不写无敌帧。
 */
public final class CounterService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final CounterLedger LEDGER = new CounterLedger();

    private CounterService() {
    }

    public static CounterArmResult arm(ServerPlayer owner, CounterWindowSpec spec) {
        if (!authority(owner) || !owner.isAlive() || owner.isDeadOrDying() || owner.isSpectator() || spec == null) {
            return CounterArmResult.REJECTED_INVALID;
        }
        return LEDGER.arm(owner.getUUID().toString(), spec);
    }

    public static boolean cancel(ServerPlayer owner) {
        if (!threadOwner(owner)) {
            return false;
        }
        return LEDGER.cancel(owner.getUUID().toString());
    }

    public static boolean active(ServerPlayer owner) {
        if (!threadOwner(owner)) {
            return false;
        }
        return LEDGER.active(owner.getUUID().toString());
    }

    public static CounterHurtFrame enterHurt() {
        return LEDGER.enterHurt();
    }

    public static void finishHurt(CounterHurtFrame frame, boolean accepted) {
        boolean settled = false;
        try {
            OwnerReadiness readiness = OwnerReadiness.UNKNOWN;
            if (frame != null && frame.reserved()) {
                readiness = readinessOf(frame.ownerKey());
            }
            LEDGER.finishHurt(frame, accepted, readiness);
            settled = true;
        } catch (Throwable failure) {
            LOGGER.error("counter hurt finish failed", failure);
        } finally {
            if (!settled) {
                abortQuietly(frame);
            }
        }
    }

    public static void abortHurt(CounterHurtFrame frame) {
        abortQuietly(frame);
    }

    public static void onLivingHurt(LivingHurtEvent event) {
        if (event == null || event.getEntity() == null || event.getEntity().level().isClientSide()) {
            return;
        }
        CounterHurtFrame frame = LEDGER.current();
        if (frame == null) {
            return;
        }
        if (event.isCanceled()) {
            LEDGER.noteHurtCanceled(frame);
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer owner) || !authority(owner)) {
            return;
        }
        float amount = event.getAmount();
        HurtOffer offer = describe(owner, event.getSource(), amount);
        HurtMitigation mitigation = LEDGER.noteHurt(frame, offer);
        if (!mitigation.reserved()) {
            return;
        }
        float reduced = mitigation.amount();
        if (Float.isFinite(reduced) && reduced != amount) {
            event.setAmount(reduced);
        }
    }

    public static void onLivingDamage(LivingDamageEvent event) {
        if (event == null || event.getEntity() == null || event.getEntity().level().isClientSide()) {
            return;
        }
        CounterHurtFrame frame = LEDGER.current();
        if (frame == null) {
            return;
        }
        // 吸收把生命伤害扣成 0 时事件仍会到来。只记这次调用有没有走到、有没有被取消。
        LEDGER.noteDamage(frame, event.isCanceled());
    }

    public static void onServerTick(MinecraftServer server) {
        if (server == null || !server.isSameThread()) {
            return;
        }
        LEDGER.tick();
        List<PlannedStrike> due = LEDGER.drainStrikes();
        for (PlannedStrike strike : due) {
            perform(server, strike);
        }
    }

    public static void onOwnerDeath(ServerPlayer owner) {
        clear(owner);
    }

    public static void onOwnerLogout(ServerPlayer owner) {
        clear(owner);
    }

    public static void onOwnerDimension(ServerPlayer owner) {
        clear(owner);
    }

    /**
     * 服务端停止时清掉全部内存窗口和待反击。
     * 只接受该服务器自己的线程；不查看在线玩家，也不写存档。
     */
    public static void onServerStopping(MinecraftServer server) {
        if (server == null || !server.isSameThread()) {
            return;
        }
        LEDGER.clearAll();
        CounterDamage.clearCurrentThread();
    }

    private static void clear(ServerPlayer owner) {
        if (!threadOwner(owner)) {
            return;
        }
        LEDGER.clearOwner(owner.getUUID().toString());
    }

    private static void perform(MinecraftServer server, PlannedStrike strike) {
        UUID ownerId = parseId(strike.ownerKey());
        UUID attackerId = parseId(strike.attackerKey());
        ServerPlayer owner = ownerId == null ? null : server.getPlayerList().getPlayer(ownerId);
        LivingEntity attacker = null;
        if (owner != null && attackerId != null) {
            Entity found = owner.serverLevel().getEntity(attackerId);
            if (found instanceof LivingEntity living) {
                attacker = living;
            }
        }
        StrikeFacts facts = new StrikeFacts();
        facts.ownerAlive = owner != null && owner.isAlive() && !owner.isDeadOrDying();
        facts.ownerRemoved = owner == null || owner.isRemoved();
        facts.ownerSpectator = owner != null && owner.isSpectator();
        facts.sameDimension = owner != null
                && dimensionKey(owner).equals(strike.dimensionKey());
        facts.attackerAlive = attacker != null && attacker.isAlive() && !attacker.isDeadOrDying();
        facts.attackerRemoved = attacker == null || attacker.isRemoved();
        facts.attackerSpectator = attacker != null && attacker.isSpectator();
        facts.hostile = hostileTo(owner, attacker);
        facts.damage = strike.damage();
        if (!CounterLedger.strikeReady(facts) || owner == null || attacker == null) {
            return;
        }
        DamageSource source = damageSource(owner.serverLevel(), owner);
        if (source == null) {
            return;
        }
        CounterDamage.Mark mark = CounterDamage.open();
        LEDGER.openCounterScope();
        try {
            attacker.hurt(source, strike.damage());
        } catch (RuntimeException failure) {
            LOGGER.error("counter strike failed for {}", strike.attackerKey(), failure);
        } finally {
            LEDGER.closeCounterScope();
            CounterDamage.close(mark);
        }
    }

    private static HurtOffer describe(ServerPlayer owner, DamageSource source, float amount) {
        boolean counterSource = CounterDamage.isCounter(source);
        boolean bypassInvulnerability = source != null && source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        boolean melee = !counterSource && !bypassInvulnerability && meleeSource(source);
        boolean ownedProjectile = !counterSource && !bypassInvulnerability && projectileSource(source);
        LivingEntity attacker = attackerOf(source, melee, ownedProjectile);
        return HurtOffer.of(
                owner.getUUID().toString(),
                attacker == null ? null : attacker.getUUID().toString(),
                dimensionKey(owner),
                amount,
                false,
                melee,
                ownedProjectile,
                counterSource,
                bypassInvulnerability,
                hostileTo(owner, attacker));
    }

    private static boolean meleeSource(DamageSource source) {
        if (source == null || source.is(DamageTypeTags.IS_PROJECTILE)) {
            return false;
        }
        Entity direct = source.getDirectEntity();
        Entity cause = source.getEntity();
        if (!(direct instanceof LivingEntity living)) {
            return false;
        }
        if (direct != cause) {
            return false;
        }
        if (!living.isAlive() || living.isRemoved() || living.isSpectator()) {
            return false;
        }
        return source.is(DamageTypes.PLAYER_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)
                || source.is(DamageTypes.STING);
    }

    private static boolean projectileSource(DamageSource source) {
        if (source == null) {
            return false;
        }
        Entity direct = source.getDirectEntity();
        if (!(direct instanceof Projectile projectile)) {
            return false;
        }
        Entity cause = source.getEntity();
        Entity projectileOwner = projectile.getOwner();
        if (!(projectileOwner instanceof LivingEntity living)) {
            return false;
        }
        if (projectileOwner != cause) {
            return false;
        }
        return living.isAlive() && !living.isRemoved() && !living.isSpectator();
    }

    private static LivingEntity attackerOf(DamageSource source, boolean melee, boolean ownedProjectile) {
        if (source == null) {
            return null;
        }
        if (melee) {
            Entity direct = source.getDirectEntity();
            if (direct instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
                return living;
            }
            return null;
        }
        if (!ownedProjectile) {
            return null;
        }
        Entity direct = source.getDirectEntity();
        if (!(direct instanceof Projectile projectile)) {
            return null;
        }
        Entity projectileOwner = projectile.getOwner();
        if (projectileOwner instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
            return living;
        }
        return null;
    }

    private static boolean hostileTo(ServerPlayer owner, LivingEntity attacker) {
        if (owner == null || attacker == null || !attacker.isAlive() || attacker.isRemoved() || attacker.isSpectator()) {
            return false;
        }
        if (attacker == owner || attacker.getUUID().equals(owner.getUUID())) {
            return false;
        }
        if (attacker.isAlliedTo(owner) || owner.isAlliedTo(attacker)) {
            return false;
        }
        if (attacker instanceof TamableAnimal tamable) {
            UUID ownerId = tamable.getOwnerUUID();
            if (ownerId != null && ownerId.equals(owner.getUUID())) {
                return false;
            }
        }
        return RelationPolicy.hostileCapture().accepts(owner, attacker);
    }

    private static DamageSource damageSource(ServerLevel level, ServerPlayer owner) {
        try {
            Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
            Holder<DamageType> holder = registry.getHolderOrThrow(CounterDamage.TYPE);
            DamageSource source = new DamageSource(holder, owner);
            if (!source.is(CounterDamage.TYPE)) {
                return null;
            }
            return source;
        } catch (RuntimeException failure) {
            LOGGER.error("counterattack damage type missing", failure);
            return null;
        }
    }

    private static OwnerReadiness readinessOf(String ownerKey) {
        UUID ownerId = parseId(ownerKey);
        if (ownerId == null) {
            return OwnerReadiness.UNKNOWN;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !server.isSameThread()) {
            return OwnerReadiness.UNKNOWN;
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (owner == null) {
            return OwnerReadiness.UNKNOWN;
        }
        if (!owner.isAlive() || owner.isDeadOrDying() || owner.isRemoved() || owner.isSpectator()) {
            return OwnerReadiness.UNFIT;
        }
        return OwnerReadiness.READY;
    }

    private static void abortQuietly(CounterHurtFrame frame) {
        try {
            LEDGER.abortHurt(frame);
        } catch (Throwable failure) {
            LOGGER.error("counter hurt abort failed", failure);
        }
    }

    private static boolean authority(ServerPlayer owner) {
        return threadOwner(owner) && !owner.isRemoved() && !owner.level().isClientSide();
    }

    private static boolean threadOwner(ServerPlayer owner) {
        if (owner == null) {
            return false;
        }
        MinecraftServer server = owner.getServer();
        return server != null && server.isSameThread();
    }

    private static String dimensionKey(ServerPlayer owner) {
        return owner.level().dimension().location().toString();
    }

    private static UUID parseId(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException failure) {
            return null;
        }
    }
}
