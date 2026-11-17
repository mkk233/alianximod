package com.bslx.XueXiMod.shu.technique.release.prison;

import com.bslx.XueXiMod.registry.ModMobEffects;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactCarrier;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactEnchantState;
import com.bslx.XueXiMod.status.WetnessState;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** 占用、提交、结束和耗湿。操作号只留在本次预约里，关闭后丢掉。 */
public final class PrisonRuntime {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SHORT_TICKS = 20;
    private static final int LONG_TICKS = 100;
    private static final long REVISION = 1L;

    private static final Map<UUID, PrisonReservation> SESSIONS = new HashMap<>();
    private static final Map<UUID, UUID> PREPARED_TARGETS = new HashMap<>();
    private static final Map<UUID, PrisonRecord> PRISONS = new HashMap<>();
    private static final Map<UUID, UUID> PRISON_BY_TARGET = new HashMap<>();
    private static final IdentityHashMap<LivingEntity, PrisonRecord> BY_ENTITY = new IdentityHashMap<>();

    private PrisonRuntime() {
    }

    /** 这个子操作号是否还占着本次预约。只读，不刷新牢，也不新建预约。 */
    static boolean reservationOpen(UUID operationId) {
        return operationId != null && SESSIONS.containsKey(operationId);
    }

    public static PrisonReservation prepare(ServerPlayer caster, LivingEntity target, UUID operationId) {
        return prepare(caster, target, operationId, ReleaseInput.BASE);
    }

    public static PrisonReservation prepare(ServerPlayer caster, LivingEntity target, UUID operationId, ReleaseInput offeredInput) {
        if (operationId == null || caster == null || target == null) {
            return rejected(null, caster, target, "missing", bindInput(null, offeredInput));
        }
        if (!target.level().isClientSide()) {
            PrisonWetDue.settleLoaded(target);
        }
        PrisonReservation existing = SESSIONS.get(operationId);
        ReleaseInput frozenInput = bindInput(existing, offeredInput);
        if (existing != null) {
            return existing;
        }
        if (caster.level().isClientSide() || target.level().isClientSide()) {
            return rejected(operationId, caster, target, "client", frozenInput);
        }
        if (PRISON_BY_TARGET.containsKey(target.getUUID()) || PREPARED_TARGETS.containsKey(target.getUUID())) {
            return rejected(operationId, caster, target, "occupied", frozenInput);
        }
        PrisonReservation reservation = new PrisonReservation(operationId, caster, target, true, "", frozenInput);
        PREPARED_TARGETS.put(target.getUUID(), operationId);
        SESSIONS.put(operationId, reservation);
        return reservation;
    }

    static PrisonCommitResult commit(PrisonReservation reservation, EnchantmentSnapshot contact, ReleaseInput offeredInput) {
        LivingEntity target = reservation.target();
        if (target == null || !target.isAlive() || target.isRemoved() || target.level().isClientSide()) {
            return PrisonCommitResult.fail("dead");
        }
        if (!(target.level() instanceof ServerLevel level)) {
            return PrisonCommitResult.fail("dimension");
        }
        MobEffectInstance wet = target.getEffect(ModMobEffects.WETNESS.get());
        int current = WetnessState.read(target);
        int tier = WetnessState.tierOf(current);
        if (wet == null || current <= 0 || tier <= 0) {
            return PrisonCommitResult.fail("not_wet");
        }
        ReleaseInput frozenInput = bindInput(reservation, offeredInput);
        int wetRemaining = wet.getDuration();
        int durationTicks = scaledDuration(tier, wetRemaining, frozenInput);
        if (durationTicks <= 0) {
            return PrisonCommitResult.fail("duration");
        }
        dismount(target);
        Vec3 anchor = target.position();
        AABB bodyBox = target.getBoundingBox();
        double shellScale = frozenInput.sizeMultiplier();
        AABB bounds = PrisonShell.expand(bodyBox, shellScale);
        if (anchor == null || bounds == null || reservation.caster() == null) {
            return PrisonCommitResult.fail("rejected");
        }
        long birthTick = level.getGameTime();
        long endTick = ReleaseInput.expiryGameTime(birthTick, durationTicks);
        PrisonView view = new PrisonView(
                UUID.randomUUID(),
                REVISION,
                reservation.caster().getUUID(),
                target.getUUID(),
                level.dimension(),
                anchor,
                bounds,
                tier,
                birthTick,
                endTick);
        PrisonRecord prison = new PrisonRecord(view, PrisonShell.innerFor(bodyBox, shellScale), target, tier);
        reservation.markCommitted();
        PREPARED_TARGETS.remove(target.getUUID());
        PRISONS.put(view.prisonId(), prison);
        PRISON_BY_TARGET.put(view.targetId(), view.prisonId());
        BY_ENTITY.put(target, prison);
        try {
            extendSupportingWet(target, current, wetRemaining, durationTicks, frozenInput);
            if (contact != null) {
                prison.contact = ContactEnchantState.open(prison, contact);
            }
            PrisonEvents.created(view);
            if (prison.phase == Phase.COMMITTED && tier >= 4 && target.isAlive()) {
                PrisonPressure.pulse(target);
            }
        } catch (RuntimeException failure) {
            LOGGER.error("water prison commit follow-up failed for {}", view.prisonId(), failure);
        }
        return PrisonCommitResult.success(view);
    }

    static void close(PrisonReservation reservation) {
        if (reservation.operationId() != null) {
            SESSIONS.remove(reservation.operationId(), reservation);
        }
        if (reservation.wasCommitted() || reservation.target() == null || reservation.operationId() == null) {
            return;
        }
        UUID holder = PREPARED_TARGETS.get(reservation.target().getUUID());
        if (reservation.operationId().equals(holder)) {
            PREPARED_TARGETS.remove(reservation.target().getUUID());
        }
    }

    public static Optional<PrisonView> view(UUID targetId) {
        PrisonRecord prison = prisonOf(targetId);
        if (prison == null || prison.phase != Phase.COMMITTED) {
            return Optional.empty();
        }
        return Optional.of(prison.view);
    }

    public static boolean end(UUID prisonId, EndReason reason) {
        if (prisonId == null) {
            return false;
        }
        PrisonRecord prison = PRISONS.get(prisonId);
        if (prison == null) {
            return false;
        }
        return prison.finish(reason == null ? EndReason.REMOVED : reason, prison.carrier());
    }

    public static boolean suppressesEnhancedSuffocation(LivingEntity entity) {
        PrisonRecord prison = find(entity);
        return prison != null && prison.phase == Phase.COMMITTED && prison.view.lockedTier() >= 4;
    }

    public static boolean locks(Entity entity) {
        return entity instanceof LivingEntity living && find(living) != null;
    }

    public static Vec3 anchor(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return null;
        }
        PrisonRecord prison = find(living);
        if (prison == null) {
            return null;
        }
        return prison.view.anchor();
    }

    public static boolean endTarget(LivingEntity locked, EndReason reason, LivingEntity wetCarrier) {
        PrisonRecord prison = find(locked);
        if (prison == null) {
            return false;
        }
        LivingEntity carrier = wetCarrier == null ? prison.carrier() : wetCarrier;
        return prison.finish(reason == null ? EndReason.REMOVED : reason, carrier);
    }

    public static void crossDimension(Entity original, Entity arrived) {
        if (!(original instanceof LivingEntity living) || arrived == null) {
            return;
        }
        LivingEntity carrier = arrived instanceof LivingEntity survivor && !survivor.isRemoved() ? survivor : living;
        endTarget(living, EndReason.DIMENSION, carrier);
    }

    public static void onConfirmedDeath(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        endTarget(entity, EndReason.DEATH, entity);
        if (entity instanceof ServerPlayer player) {
            endCastBy(player, EndReason.CASTER_DEATH);
        }
    }

    public static void onLogout(ServerPlayer player) {
        if (player == null) {
            return;
        }
        endTarget(player, EndReason.LOGOUT, player);
        endCastBy(player, EndReason.CASTER_LOGOUT);
    }

    public static void onDimension(ServerPlayer player) {
        if (player == null) {
            return;
        }
        endTarget(player, EndReason.DIMENSION, player);
        endCastBy(player, EndReason.CASTER_DIMENSION);
    }

    /** 原版末地回主世界已经接受、玩家即将被丢掉之前的那次返回。普通换维事件不到这里。 */
    public static void onVanillaEndReturn(ServerPlayer player) {
        if (player == null) {
            return;
        }
        boolean wroteOwn = false;
        PrisonRecord own = find(player);
        if (own != null) {
            own.finish(EndReason.DIMENSION, player);
            wroteOwn = own.settled;
        }
        endCastBy(player, EndReason.CASTER_DIMENSION);
        if (wroteOwn) {
            PrisonWetTransfer.capture(player);
        }
    }

    public static void shutdown(MinecraftServer server) {
        if (server == null || PRISONS.isEmpty()) {
            return;
        }
        ArrayList<PrisonRecord> snapshot = new ArrayList<>(PRISONS.values());
        for (PrisonRecord prison : snapshot) {
            if (prison.phase != Phase.COMMITTED) {
                continue;
            }
            LivingEntity carrier = prison.carrier();
            if (!holdsWritableWet(carrier)) {
                LivingEntity loaded = findLive(server, prison.view.targetId());
                if (holdsWritableWet(loaded)) {
                    carrier = loaded;
                }
            }
            prison.finish(EndReason.REMOVED, carrier);
        }
    }

    public static void onWetRemoved(LivingEntity entity, MobEffectInstance instance) {
        if (entity == null || instance == null || com.bslx.XueXiMod.status.AttributeStateService.isDirectEdit()) {
            return;
        }
        if (instance.getEffect() != ModMobEffects.WETNESS.get()) {
            return;
        }
        endTarget(entity, EndReason.WET_CLEARED, entity);
    }

    public static void rebind(LivingEntity living) {
        if (living == null || living.level().isClientSide()) {
            return;
        }
        PrisonWetDue.settleLoaded(living);
        PrisonRecord prison = prisonOf(living.getUUID());
        if (prison == null || prison.phase != Phase.COMMITTED) {
            return;
        }
        long gameTime = living.level().getGameTime();
        if (prison.expiredWhileAway || gameTime >= prison.view.endTick()) {
            prison.finish(EndReason.DURATION, living);
            return;
        }
        prison.attach(living);
    }

    public static void tick(MinecraftServer server) {
        if (server == null || PRISONS.isEmpty()) {
            return;
        }
        ArrayList<PrisonRecord> snapshot = new ArrayList<>(PRISONS.values());
        for (PrisonRecord prison : snapshot) {
            prison.tick(server);
        }
    }

    private static void endCastBy(ServerPlayer player, EndReason reason) {
        ArrayList<PrisonRecord> owned = new ArrayList<>();
        for (PrisonRecord prison : PRISONS.values()) {
            if (player.getUUID().equals(prison.view.casterId())) {
                owned.add(prison);
            }
        }
        for (PrisonRecord prison : owned) {
            prison.finish(reason, prison.carrier());
        }
    }

    private static PrisonRecord find(LivingEntity entity) {
        if (entity == null) {
            return null;
        }
        PrisonRecord direct = BY_ENTITY.get(entity);
        if (direct != null && direct.phase == Phase.COMMITTED && direct.prisoner == entity) {
            return direct;
        }
        return null;
    }

    private static PrisonRecord prisonOf(UUID targetId) {
        if (targetId == null) {
            return null;
        }
        UUID prisonId = PRISON_BY_TARGET.get(targetId);
        if (prisonId == null) {
            return null;
        }
        return PRISONS.get(prisonId);
    }

    /** 已有预约时保留首次输入。没有预约时采用这次提供的输入，空输入回基准。 */
    static ReleaseInput bindInput(PrisonReservation existing, ReleaseInput offeredInput) {
        if (existing != null && existing.releaseInput() != null) {
            return existing.releaseInput();
        }
        if (offeredInput == null) {
            return ReleaseInput.BASE;
        }
        return offeredInput;
    }

    /**
     * 药水递减先于牢到期时，支持期要比牢寿命多 1 拍才盖住 endTick。
     * 已经是 int 上界时不再加，避免绕回负数。
     */
    static int supportingDuration(int prisonTicks) {
        if (prisonTicks <= 0 || prisonTicks == Integer.MAX_VALUE) {
            return prisonTicks;
        }
        return prisonTicks + 1;
    }

    /**
     * 只有倍率大于 1 且原剩余盖不住支持期时，返回要写回的时长。
     * 其余情况返回原剩余，调用方不得改药水。
     */
    static int replacementDuration(int wetRemaining, int prisonTicks, float sizeMultiplier) {
        if (!(sizeMultiplier > 1f) || prisonTicks <= 0) {
            return wetRemaining;
        }
        int requiredDuration = supportingDuration(prisonTicks);
        if (wetRemaining >= requiredDuration) {
            return wetRemaining;
        }
        return requiredDuration;
    }

    /** 成功提交后补潮湿时长。不改湿度数值，也不按新时长重算牢寿命。 */
    private static void extendSupportingWet(LivingEntity target, int currentAmount, int wetRemaining, int prisonTicks,
                                            ReleaseInput frozenInput) {
        float chainScale = frozenInput == null ? 1f : frozenInput.sizeMultiplier();
        int requiredDuration = replacementDuration(wetRemaining, prisonTicks, chainScale);
        if (requiredDuration == wetRemaining) {
            return;
        }
        WetnessState.replaceExact(target, currentAmount, requiredDuration);
    }

    /** 先按潮湿档算基准寿命，再只延长有限正寿命。非正基准原样返回。 */
    static int scaledDuration(int tier, int wetRemaining, ReleaseInput frozenInput) {
        int baseTicks = prisonTicks(tier, wetRemaining);
        if (baseTicks <= 0) {
            return baseTicks;
        }
        ReleaseInput safeInput = frozenInput == null ? ReleaseInput.BASE : frozenInput;
        return safeInput.scaleDuration(baseTicks);
    }

    private static PrisonReservation rejected(UUID operationId, ServerPlayer caster, LivingEntity target, String reason,
                                              ReleaseInput frozenInput) {
        PrisonReservation reservation = new PrisonReservation(operationId, caster, target, false, reason, frozenInput);
        if (operationId != null) {
            SESSIONS.put(operationId, reservation);
        }
        return reservation;
    }

    private static void dismount(LivingEntity target) {
        if (target.isPassenger()) {
            target.stopRiding();
        }
        if (target.isVehicle()) {
            target.ejectPassengers();
        }
    }

    private static int prisonTicks(int tier, int remaining) {
        if (tier == 1) {
            return SHORT_TICKS;
        }
        if (tier == 2) {
            return LONG_TICKS;
        }
        if (tier == 3) {
            if (remaining <= 0) {
                return 0;
            }
            return Math.max(1, remaining / 2);
        }
        if (tier >= 4) {
            return remaining;
        }
        return 0;
    }

    static boolean settleCarrier(LivingEntity carrier, int lockedTier, EndReason reason) {
        if (!holdsWritableWet(carrier)) {
            return false;
        }
        if (reason == EndReason.WET_CLEARED) {
            WetnessState.clear(carrier);
            return true;
        }
        int current = WetnessState.read(carrier);
        if (current <= 0) {
            if (carrier.getEffect(ModMobEffects.WETNESS.get()) != null) {
                WetnessState.clear(carrier);
            }
            return true;
        }
        if (lockedTier == 2) {
            WetnessState.replacePreservingDuration(carrier, current / 2);
            return true;
        }
        WetnessState.clear(carrier);
        return true;
    }

    private static boolean holdsWritableWet(LivingEntity entity) {
        if (entity == null || entity.level() == null || entity.level().isClientSide()) {
            return false;
        }
        if (!entity.isRemoved()) {
            return true;
        }
        return entity instanceof ServerPlayer
                && entity.getRemovalReason() == Entity.RemovalReason.CHANGED_DIMENSION;
    }

    private static LivingEntity findLive(MinecraftServer server, UUID targetId) {
        if (server == null || targetId == null) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            Entity found = level.getEntity(targetId);
            if (found instanceof LivingEntity living && !living.isRemoved()) {
                return living;
            }
        }
        return null;
    }

    private enum Phase {
        COMMITTED,
        ENDING,
        ENDED
    }

    private static final class PrisonRecord implements ContactCarrier {
        private final PrisonView view;
        private final AABB inner;
        private final int tier;
        private LivingEntity prisoner;
        private ContactEnchantState contact;
        private Phase phase = Phase.COMMITTED;
        private boolean settled;
        private boolean expiredWhileAway;
        private long nextPulse;

        private PrisonRecord(PrisonView view, AABB inner, LivingEntity prisoner, int tier) {
            this.view = view;
            this.inner = inner;
            this.prisoner = prisoner;
            this.tier = tier;
            this.nextPulse = tier >= 4 ? view.birthTick() + PrisonPressure.INTERVAL_TICKS : Long.MAX_VALUE;
        }

        private void tick(MinecraftServer server) {
            if (this.phase != Phase.COMMITTED) {
                return;
            }
            ServerLevel level = server.getLevel(this.view.dimension());
            if (level == null) {
                return;
            }
            long gameTime = level.getGameTime();
            LivingEntity live = resolve(level);
            if (live == null) {
                if (this.prisoner != null && this.prisoner.isRemoved()) {
                    handleRemoval(server);
                    return;
                }
                if (gameTime >= this.view.endTick()) {
                    this.expiredWhileAway = true;
                }
                return;
            }
            if (live != this.prisoner) {
                attach(live);
            }
            if (this.expiredWhileAway || gameTime >= this.view.endTick()) {
                finish(EndReason.DURATION, live);
                return;
            }
            if (this.tier >= 4 && gameTime >= this.nextPulse) {
                this.nextPulse = gameTime + PrisonPressure.INTERVAL_TICKS;
                PrisonPressure.pulse(live);
            }
        }

        private LivingEntity resolve(ServerLevel level) {
            Entity found = level.getEntity(this.view.targetId());
            if (found instanceof LivingEntity living && !living.isRemoved()) {
                return living;
            }
            if (this.prisoner != null && !this.prisoner.isRemoved() && this.prisoner.level() == level) {
                return this.prisoner;
            }
            return null;
        }

        private void handleRemoval(MinecraftServer server) {
            Entity.RemovalReason removal = this.prisoner.getRemovalReason();
            if (removal == Entity.RemovalReason.UNLOADED_TO_CHUNK) {
                BY_ENTITY.remove(this.prisoner);
                this.prisoner = null;
                return;
            }
            if (removal == Entity.RemovalReason.CHANGED_DIMENSION) {
                LivingEntity survivor = findLive(server, this.view.targetId());
                finish(EndReason.DIMENSION, survivor == null ? this.prisoner : survivor);
                return;
            }
            if (removal == Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
                finish(EndReason.LOGOUT, this.prisoner);
                return;
            }
            if (removal == Entity.RemovalReason.KILLED) {
                finish(EndReason.DEATH, this.prisoner);
                return;
            }
            finish(EndReason.REMOVED, this.prisoner);
        }

        private void attach(LivingEntity living) {
            if (this.prisoner != null && this.prisoner != living) {
                BY_ENTITY.remove(this.prisoner);
            }
            this.prisoner = living;
            BY_ENTITY.put(living, this);
        }

        private LivingEntity carrier() {
            if (this.prisoner != null) {
                return this.prisoner;
            }
            return null;
        }

        private boolean finish(EndReason reason, LivingEntity wetCarrier) {
            if (this.phase != Phase.COMMITTED) {
                return false;
            }
            this.phase = Phase.ENDING;
            if (this.contact != null) {
                this.contact.close();
                this.contact = null;
            }
            if (this.prisoner != null) {
                BY_ENTITY.remove(this.prisoner);
            }
            PRISONS.remove(this.view.prisonId());
            UUID mapped = PRISON_BY_TARGET.get(this.view.targetId());
            if (this.view.prisonId().equals(mapped)) {
                PRISON_BY_TARGET.remove(this.view.targetId());
            }
            if (!this.settled) {
                LivingEntity carrier = wetCarrier;
                if (!holdsWritableWet(carrier)) {
                    MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
                    if (server != null) {
                        LivingEntity loaded = findLive(server, this.view.targetId());
                        if (holdsWritableWet(loaded)) {
                            carrier = loaded;
                        }
                    }
                }
                if (holdsWritableWet(carrier)) {
                    try {
                        if (settleCarrier(carrier, this.tier, reason)) {
                            this.settled = true;
                            PrisonWetDue.drop(this.view.prisonId(), this.view.dimension());
                        } else {
                            PrisonWetDue.keep(this.view.prisonId(), this.view.targetId(), this.view.dimension(), this.tier, reason);
                        }
                    } catch (RuntimeException failure) {
                        LOGGER.error("water prison wet settlement failed for {}", this.view.prisonId(), failure);
                        PrisonWetDue.keep(this.view.prisonId(), this.view.targetId(), this.view.dimension(), this.tier, reason);
                    }
                } else {
                    PrisonWetDue.keep(this.view.prisonId(), this.view.targetId(), this.view.dimension(), this.tier, reason);
                }
            }
            this.phase = Phase.ENDED;
            PrisonEvents.ended(this.view, reason);
            return true;
        }

        @Override
        public UUID hostId() {
            return this.view.prisonId();
        }

        @Override
        public int revision() {
            return (int) this.view.revision();
        }

        @Override
        public UUID ownerId() {
            return this.view.casterId();
        }

        @Override
        public boolean active() {
            return this.phase == Phase.COMMITTED;
        }

        @Override
        public boolean touches(LivingEntity target) {
            return PrisonShell.touches(this.view.bounds(), this.inner, target, this.view.targetId());
        }

        @Override
        public ServerLevel hostLevel() {
            if (this.prisoner != null && this.prisoner.level() instanceof ServerLevel level
                    && this.view.dimension().equals(level.dimension())) {
                return level;
            }
            return null;
        }

        @Override
        public AABB queryBox() {
            return this.view.bounds();
        }
    }
}
