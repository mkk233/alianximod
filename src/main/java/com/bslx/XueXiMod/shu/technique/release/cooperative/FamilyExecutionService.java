package com.bslx.XueXiMod.shu.technique.release.cooperative;

import com.bslx.XueXiMod.animation.runtime.CooperativeActionPublisher;
import com.bslx.XueXiMod.debug.performance.PerfCounter;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.network.BatchReleasePlan;
import com.bslx.XueXiMod.network.CastCooldown;
import com.bslx.XueXiMod.network.UncommittedShare;
import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueManager;
import com.bslx.XueXiMod.shu.balance.BalanceService;
import com.bslx.XueXiMod.shu.balance.BalanceSide;
import com.bslx.XueXiMod.shu.technique.ActionWindow;
import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.TechniqueEffectRegistry;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneServerPorts;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ActionLifecycle;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ActionPlayback;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ActorActionPublisher;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ActorAttackAdapter;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ActorFrame;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.AttackMemberPlan;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.AttackPreparation;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CancelReason;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ContactFamilySource;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperationReservation;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeActionIds;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeStructureLimits;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.FamilyExecutionRuntime;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.FamilyKey;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.FamilyRuntimeHandle;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.LogicalCopyKey;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.MemberKey;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.PreparationState;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.PreparedAttack;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.PreparedEmit;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ReleaseGateHandle;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ReleaseGateKind;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ResolvedCopyGeometry;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ShareRole;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ShareSlot;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.TriggerGeometry;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.WeaponDisplaySnapshot;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 每个服务器一份执行表。计划本身不持有技能或主手，运行态只在这一层。
 */
public final class FamilyExecutionService implements FamilyExecutionRuntime {
    private static final String DASH_ID = "movement_dash";
    private static final String LEAP_ID = "movement_leap";
    private static final ThreadLocal<Vec3> FORCED_ANCHOR = new ThreadLocal<>();

    /** 只覆盖当前调用栈里的领域锚点，返回前必须配对结束。 */
    public static void beginForcedAnchor(Vec3 origin) {
        FORCED_ANCHOR.set(origin);
    }

    public static void endForcedAnchor() {
        FORCED_ANCHOR.remove();
    }
    private final CloneServerPorts ports;
    private final CooperativeServerSessions sessions;
    private final ActorActionPublisher publisher = CooperativeActionPublisher.publisher();
    private final Map<MinecraftServer, ServerBook> books = new IdentityHashMap<>();

    public FamilyExecutionService(CloneServerPorts ports, CooperativeServerSessions sessions) {
        this.ports = ports;
        this.sessions = sessions;
    }

    public int nextFamilyOrdinal(MinecraftServer server) {
        return book(server).nextOrdinal++;
    }

    public CooperativeRuntime.Room roomFor(ServerPlayer player, long soloEmits, boolean cooperation) {
        if (player == null || player.getServer() == null) {
            return CooperativeRuntime.Room.REFUSE;
        }
        ServerBook book = book(player.getServer());
        int owned = 0;
        long liveEmits = 0L;
        for (RunningFamily running : book.families.values()) {
            if (running.lifecycle == ActionLifecycle.CANCELLED || running.lifecycle == ActionLifecycle.FINISHED) {
                continue;
            }
            liveEmits += running.liveEmits();
            if (player.getUUID().equals(running.plan.family().ownerId())) {
                owned += 1;
            }
        }
        if (book.families.size() >= CooperativeStructureLimits.ACTIVE_FAMILY_CAP
                || owned >= CooperativeStructureLimits.ACTIVE_FAMILIES_PER_OWNER
                || book.openedThisTick >= CooperativeStructureLimits.COOPERATION_PREPARE_PER_TICK_CAP) {
            return CooperativeRuntime.Room.REFUSE;
        }
        long dualEmits = cooperation ? Math.multiplyExact(soloEmits, 2L) : soloEmits;
        if (liveEmits + dualEmits > CooperativeStructureLimits.LIVE_PLANNED_EMIT_CAP) {
            if (cooperation && liveEmits + soloEmits <= CooperativeStructureLimits.LIVE_PLANNED_EMIT_CAP) {
                return CooperativeRuntime.Room.SOLO;
            }
            return CooperativeRuntime.Room.REFUSE;
        }
        return CooperativeRuntime.Room.ACCEPT;
    }

    public Opened open(ServerPlayer player, ItemStack weapon, int skillSlot, CooperativeCastBuild build) {
        return open(player, weapon, skillSlot, build, null);
    }

    public Opened open(ServerPlayer player, ItemStack weapon, int skillSlot, CooperativeCastBuild build,
                       @Nullable ManaPaymentReceipt fundedReceipt) {
        if (player == null || player.getServer() == null || build == null || !build.accepted()) {
            return Opened.rejected(null);
        }
        if (!CooperativeCarrier.stillHeld(player, build.plan().carrier())) {
            note(PerfCounter.COOPERATIVE_REFUSALS);
            return Opened.rejected("gui.xueximod.cooperative.clone_slot");
        }
        // 冷却要等这一发结束才记。先占执行锁，按住连放才会等到本族结束。
        // 不占锁时每刻再开一族，第四族之后主人族上限会走体积预算拒绝。
        ItemStack carrierStack = weapon == null ? ItemStack.EMPTY : weapon;
        String executionKey = CastCooldown.carrierKey(player, carrierStack, skillSlot);
        if (!CastCooldown.acquire(executionKey)) {
            note(PerfCounter.COOPERATIVE_REFUSALS);
            return Opened.rejected(null);
        }
        boolean familyHeld = false;
        try {
            int cost = 0;
            for (ShareSlot slot : build.plan().shares().slots()) {
                cost += slot.amount();
            }
            ManaPaymentReceipt receipt = fundedReceipt;
            if (receipt == null && cost > 0) {
                receipt = ManaPaymentReceipt.charge(player, cost, true);
                if (receipt == null) {
                    return Opened.unaffordable();
                }
            }
            RunningFamily running = new RunningFamily(
                    build, receipt, weapon, skillSlot, player.getServer().getTickCount(), executionKey);
            book(player.getServer()).families.put(build.plan().family(), running);
            familyHeld = true;
            book(player.getServer()).openedThisTick += 1;
            note(PerfCounter.COOPERATIVE_PREPARES);
            running.playLeaps(player);
            running.beginGate(player);
            boolean actorReady = running.reservation == null || running.soloFallback
                    || ports.readyFrame(running.reservation).isPresent();
            if (running.plan.gate().kind() == ReleaseGateKind.IMMEDIATE && actorReady) {
                running.tryRelease(player, player.serverLevel(), running.startTick, null);
            }
            return Opened.started(running);
        } finally {
            if (!familyHeld) {
                CastCooldown.release(executionKey);
            }
        }
    }

    public void armDash(FamilyKey family, ServerPlayer player, ItemStack weapon, EnchantmentResolution enchantments) {
        RunningFamily running = find(player == null ? null : player.getServer(), family);
        if (running == null || player == null) {
            return;
        }
        Technique technique = TechniqueManager.getById(DASH_ID);
        TechniqueEffect effect = TechniqueEffectRegistry.get(DASH_ID);
        if (technique == null || effect == null) {
            running.cancel(CancelReason.ADAPTER_MISSING);
            return;
        }
        float damageAmount = BalanceService.damageContribution(
                BalanceSide.SERVER, DASH_ID, technique.getBaseDamage()).amount();
        ActionWindow.Sink sink = new CooperativeContactBridge().sink(new DashSource(running));
        effect.execute(player, technique, weapon, new TechniqueCastContext(
                damageAmount, player.getLookAngle(), player.position(), enchantments, null, false, false, sink));
        running.dashArmed = true;
    }

    @Override
    public FamilyRuntimeHandle start(ExecutionFamilyPlan plan) {
        throw new IllegalStateException("执行族必须从已经扣费的入口启动");
    }

    @Override
    public void tick(ServerLevel level, long tick) {
        if (level == null || level.getServer() == null) {
            return;
        }
        ServerBook book = books.get(level.getServer());
        if (book == null) {
            return;
        }
        if (book.observedTick != tick) {
            book.observedTick = tick;
            book.openedThisTick = 0;
            book.publishedThisTick = 0;
        }
        List<RunningFamily> snapshot = new ArrayList<>(book.families.values());
        for (RunningFamily running : snapshot) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(running.plan.family().ownerId());
            if (owner == null || owner.level() != level) {
                continue;
            }
            running.advance(owner, level, tick, book);
        }
    }

    @Override
    public void cancel(FamilyKey family, CancelReason reason) {
        if (family == null) {
            return;
        }
        for (ServerBook book : books.values()) {
            RunningFamily running = book.families.get(family);
            if (running != null) {
                running.cancel(reason);
            }
        }
    }

    @Override
    public void cancelOwner(UUID serverSession, UUID ownerId, CancelReason reason) {
        for (ServerBook book : books.values()) {
            for (RunningFamily running : new ArrayList<>(book.families.values())) {
                if (running.plan.family().serverSession().equals(serverSession)
                        && running.plan.family().ownerId().equals(ownerId)) {
                    running.cancel(reason);
                }
            }
        }
    }

    @Override
    public void cancelWorld(UUID serverSession, ResourceKey<Level> dimension, CancelReason reason) {
        for (ServerBook book : books.values()) {
            for (RunningFamily running : new ArrayList<>(book.families.values())) {
                if (running.plan.family().serverSession().equals(serverSession)
                        && running.plan.family().dimension().equals(dimension)) {
                    running.cancel(reason);
                }
            }
        }
    }

    @Override
    public void stopServer(UUID serverSession, CancelReason reason) {
        Iterator<Map.Entry<MinecraftServer, ServerBook>> entries = books.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<MinecraftServer, ServerBook> entry = entries.next();
            UUID session = sessions.sessionOf(entry.getKey());
            if (!session.equals(serverSession)) {
                continue;
            }
            for (RunningFamily running : new ArrayList<>(entry.getValue().families.values())) {
                running.cancel(reason);
            }
            sessions.forget(entry.getKey());
            entries.remove();
        }
    }

    private RunningFamily find(@Nullable MinecraftServer server, FamilyKey family) {
        if (server == null || family == null) {
            return null;
        }
        ServerBook book = books.get(server);
        return book == null ? null : book.families.get(family);
    }

    private ServerBook book(MinecraftServer server) {
        return books.computeIfAbsent(server, ignored -> new ServerBook());
    }

    private static void note(PerfCounter counter) {
        PerformanceRecorder.add(counter, 1L);
    }

    public static final class Opened {
        private final boolean insufficient;
        private final boolean rejected;
        private final @Nullable String refusalKey;
        private final boolean deferred;
        private final int cooldownTicks;
        private final List<com.bslx.XueXiMod.shu.Technique> fired;

        private Opened(boolean insufficient, boolean rejected, @Nullable String refusalKey, boolean deferred,
                       int cooldownTicks, List<Technique> fired) {
            this.insufficient = insufficient;
            this.rejected = rejected;
            this.refusalKey = refusalKey;
            this.deferred = deferred;
            this.cooldownTicks = cooldownTicks;
            this.fired = fired;
        }

        static Opened started(RunningFamily running) {
            boolean deferred = running.plan.gate().kind() != ReleaseGateKind.IMMEDIATE || !running.released;
            if (!deferred) {
                running.cooldownDelegated = true;
            }
            return new Opened(false, false, null, deferred, running.committedCooldown(), running.fired);
        }

        static Opened rejected(@Nullable String refusalKey) {
            return new Opened(false, true, refusalKey, false, 0, List.of());
        }

        static Opened unaffordable() {
            return new Opened(true, true, null, false, 0, List.of());
        }

        public boolean insufficient() {
            return insufficient;
        }

        public boolean rejected() {
            return rejected;
        }

        public @Nullable String refusalKey() {
            return refusalKey;
        }

        public boolean deferred() {
            return deferred;
        }

        public int cooldownTicks() {
            return cooldownTicks;
        }

        public List<Technique> fired() {
            return fired;
        }
    }

    private static final class ServerBook {
        private int nextOrdinal;
        private long observedTick = Long.MIN_VALUE;
        private int openedThisTick;
        private int publishedThisTick;
        private long nextSequence = 1L;
        private final Map<FamilyKey, RunningFamily> families = new HashMap<>();
    }

    private final class RunningFamily implements FamilyRuntimeHandle {
        private final ExecutionFamilyPlan plan;
        private final CooperativeCastRecipe recipe;
        private final @Nullable ManaPaymentReceipt receipt;
        private final ItemStack weapon;
        private final int skillSlot;
        private final List<Technique> fired;
        private final Map<Integer, UncommittedShare> attackShares = new HashMap<>();
        private final Map<Integer, ArrayDeque<UncommittedShare>> attackPools = new HashMap<>();
        private final List<ContactStrike> contactStrikes = new ArrayList<>();
        private final Set<UUID> contactTargets = new HashSet<>();
        private final Map<Integer, ReleaseBatch.Tally> tallies = new HashMap<>();
        private final Map<Integer, CooperativeSettlementGuard> guards = new HashMap<>();
        private final Set<String> publishedSlots = new HashSet<>();
        private final Set<Integer> continueReady = new HashSet<>();
        private final Set<String> slashPlayed = new HashSet<>();
        private final List<PlaybackMark> playbacks = new ArrayList<>();
        private UncommittedShare actionShare = UncommittedShare.none();
        private UncommittedShare cooperationShare = UncommittedShare.none();
        private ActionLifecycle lifecycle = ActionLifecycle.PREPARED;
        private final long startTick;
        private boolean released;
        private boolean soloFallback;
        private boolean cooperationCommitted;
        private boolean dashArmed;
        private boolean windowFinished;
        private boolean finishing;
        private boolean cooldownDelegated;
        private boolean runningStarted;
        private long runningTick;
        private int committedCooldown;
        private @Nullable CooperationReservation reservation;
        private int waitedTicks;
        private final String executionKey;
        private boolean executionClosed;

        private RunningFamily(CooperativeCastBuild build, @Nullable ManaPaymentReceipt receipt, ItemStack weapon,
                              int skillSlot, long startTick, String executionKey) {
            this.plan = build.plan();
            this.recipe = build.recipe();
            this.receipt = receipt;
            this.weapon = weapon == null ? ItemStack.EMPTY : weapon.copy();
            this.skillSlot = skillSlot;
            this.fired = build.fired();
            this.startTick = startTick;
            this.executionKey = executionKey;
            for (ShareSlot slot : plan.shares().slots()) {
                UncommittedShare share = receipt == null ? UncommittedShare.none() : UncommittedShare.open(receipt, slot.amount());
                if (slot.role() == ShareRole.ACTION) {
                    actionShare = share;
                } else if (slot.role() == ShareRole.COOPERATION) {
                    cooperationShare = share;
                } else {
                    attackShares.put(slot.memberOrdinal(), share);
                    attackPools.computeIfAbsent(slot.memberOrdinal(), ignored -> new ArrayDeque<>()).add(share);
                }
            }
        }

        private long liveEmits() {
            long total = 0L;
            for (AttackMemberPlan member : plan.attacks()) {
                total += (long) member.copyCount() * member.spawnCount();
            }
            if (plan.cooperation() != null && !soloFallback) {
                total *= 2L;
            }
            return total;
        }

        private int committedCooldown() {
            return committedCooldown;
        }

        private void playLeaps(ServerPlayer player) {
            Technique technique = TechniqueManager.getById(LEAP_ID);
            TechniqueEffect effect = TechniqueEffectRegistry.get(LEAP_ID);
            if (technique == null || effect == null) {
                return;
            }
            for (int ignored : recipe.leapChainIndexes()) {
                effect.execute(player, technique, weapon, new TechniqueCastContext(
                        0F, plan.frozenAim(), player.position()));
            }
        }

        private void beginGate(ServerPlayer player) {
            openTallies(player);
            if (plan.cooperation() != null) {
                ActorFrame ownerFrame = ownerFrame(player);
                // 归位只发生在分身端口内。这里不按距离再调用短距或远距搬移。
                reservation = ports.tryReserve(player, plan.family(), ownerFrame, plan.cooperation().settings(),
                        plan.cooperation().expiryTick());
                if (reservation == null || !reservation.actorPresent()) {
                    fallbackSolo(CancelReason.NO_ACTOR);
                } else if (ports.readyFrame(reservation).isEmpty()) {
                    ports.advance(player.serverLevel(), reservation, ownerFrame, startTick);
                }
            }
            if (!spinWaiting() && (plan.gate().kind() == ReleaseGateKind.TIMED
                    || plan.gate().kind() == ReleaseGateKind.CONTACT)) {
                beginRunning(player);
            }
        }

        private boolean spinWaiting() {
            return plan.gate().kind() == ReleaseGateKind.TIMED && actorStillPreparing();
        }

        private boolean actorStillPreparing() {
            return plan.cooperation() != null && !soloFallback && reservation != null
                    && reservation.actorPresent() && ports.readyFrame(reservation).isEmpty();
        }

        private void beginRunning(ServerPlayer player) {
            if (runningStarted || player.getServer() == null) {
                return;
            }
            runningStarted = true;
            runningTick = player.getServer().getTickCount();
            lifecycle = ActionLifecycle.RUNNING;
            rememberCooldown(actionShare, ShareRole.ACTION, -1);
            actionShare.commitRetained();
            if (plan.gate().kind() == ReleaseGateKind.TIMED) {
                playSpin(player);
            }
        }

        private void openTallies(ServerPlayer player) {
            ReleaseBatch previous = ReleaseBatch.current();
            ReleaseBatch opened = ReleaseBatch.open(player, null);
            try {
                for (AttackMemberPlan member : plan.attacks()) {
                    CooperativeSettlementGuard guard = new CooperativeSettlementGuard(player.getUUID(), plan.family());
                    ReleaseBatch.Tally tally = opened.openGroupTally();
                    tally.bindSettlementGuard(guard);
                    guards.put(member.memberOrdinal(), guard);
                    tallies.put(member.memberOrdinal(), tally);
                    int parentOrdinal = parentOrdinal(member);
                    if (parentOrdinal >= 0) {
                        ReleaseBatch.Tally parentTally = tallies.get(parentOrdinal);
                        int childOrdinal = member.memberOrdinal();
                        if (parentTally != null) {
                            parentTally.whenComplete((targetId, target, verdict, copyIndex) -> continueReady.add(childOrdinal));
                        }
                    }
                }
            } finally {
                ReleaseBatch.bind(previous);
            }
        }

        private int parentOrdinal(AttackMemberPlan member) {
            if (member.continueLink() == null) {
                return -1;
            }
            for (AttackMemberPlan candidate : plan.attacks()) {
                if (candidate.chainIndex() == member.continueLink().parentChainIndex()) {
                    return candidate.memberOrdinal();
                }
            }
            return -1;
        }

        private void advance(ServerPlayer player, ServerLevel level, long tick, ServerBook book) {
            if (lifecycle == ActionLifecycle.CANCELLED || lifecycle == ActionLifecycle.FINISHED) {
                return;
            }
            if (!player.isAlive() || player.isRemoved()) {
                cancel(CancelReason.OWNER_DEATH);
                return;
            }
            if (!CooperativeCarrier.stillHeld(player, plan.carrier())) {
                cancel(CancelReason.CARRIER_INVALID);
                return;
            }
            if (reservation != null && reservation.actorPresent() && !soloFallback) {
                PreparationState state = ports.advance(level, reservation, ownerFrame(player), tick);
                if (state == PreparationState.UNAVAILABLE) {
                    fallbackSolo(CancelReason.NO_ACTOR);
                }
                waitedTicks += 1;
                if (actorStillPreparing() && waitedTicks > CooperativeStructureLimits.PREPARE_TICK_CAP) {
                    fallbackSolo(CancelReason.PREPARE_TIMEOUT);
                }
            }
            if (plan.gate().kind() == ReleaseGateKind.TIMED && !runningStarted && !spinWaiting()) {
                beginRunning(player);
            }
            long releaseTick = plan.gate().timeline() == null ? 0L : plan.gate().timeline().releaseTick();
            long gateOrigin = plan.gate().kind() == ReleaseGateKind.TIMED && runningStarted ? runningTick : startTick;
            boolean contactHold = plan.gate().kind() == ReleaseGateKind.CONTACT;
            if (!contactHold && !actorStillPreparing() && tick >= gateOrigin + releaseTick) {
                tryRelease(player, level, tick, book);
            }
            long tail = plan.gate().timeline() == null ? 20L : plan.gate().timeline().endTickOffset() + 8L;
            long tailOrigin = plan.gate().kind() == ReleaseGateKind.TIMED && runningStarted ? runningTick : startTick;
            boolean contactDone = plan.gate().kind() != ReleaseGateKind.CONTACT || windowFinished || tick > startTick + 80L;
            if (contactDone && tick >= tailOrigin + tail) {
                finish(player);
            }
        }

        private void tryRelease(ServerPlayer player, ServerLevel level, long tick, @Nullable ServerBook book) {
            if (released && plan.gate().kind() != ReleaseGateKind.CONTACT) {
                publishDue(player, level, tick, book, plan.frozenAim(), null, null);
                return;
            }
            released = true;
            if (lifecycle == ActionLifecycle.PREPARED) {
                lifecycle = ActionLifecycle.RUNNING;
            }
            lifecycle = ActionLifecycle.RELEASED;
            publishDue(player, level, tick, book, plan.frozenAim(), null, null);
        }

        private void publishDue(ServerPlayer player, ServerLevel level, long tick, @Nullable ServerBook book, Vec3 aim,
                                @Nullable Vec3 contactPoint, @Nullable UUID contactTarget) {
            ServerBook activeBook = book == null ? books.get(player.getServer()) : book;
            long gateOrigin = plan.gate().kind() == ReleaseGateKind.TIMED && runningStarted ? runningTick : startTick;
            for (AttackMemberPlan member : plan.attacks()) {
                if (member.continueLink() != null && !continueReady.contains(member.memberOrdinal())) {
                    continue;
                }
                long dueTick = gateOrigin + member.delayTicks();
                if (plan.gate().timeline() != null) {
                    dueTick += plan.gate().timeline().releaseTick();
                }
                if (tick < dueTick) {
                    continue;
                }
                publishMember(player, level, activeBook, member, aim, tick, contactPoint, contactTarget, false, null);
            }
        }

        private void publishMember(ServerPlayer player, ServerLevel level, @Nullable ServerBook book,
                                   AttackMemberPlan member, Vec3 aim, long tick,
                                   @Nullable Vec3 contactPoint, @Nullable UUID contactTarget, boolean retryingSolo,
                                   @Nullable ContactStrike strike) {
            if (book != null && book.publishedThisTick >= CooperativeStructureLimits.PUBLISH_PER_TICK_CAP) {
                restoreStrikeMember(player, member, strike);
                return;
            }
            if (!retryingSolo && reservation != null && reservation.actorPresent() && !soloFallback
                    && !ports.validate(reservation, ownerFrame(player))) {
                fallbackSolo(CancelReason.VALIDATION_FAILED);
            }
            long gateOrigin = plan.gate().kind() == ReleaseGateKind.TIMED && runningStarted ? runningTick : startTick;
            List<ActorFrame> actors = actorsFor(player);
            int dueCopies = 0;
            for (int copyOrdinal = 0; copyOrdinal < member.copyCount(); copyOrdinal++) {
                long copyDue = gateOrigin + member.delayTicks();
                if (member.stagger()) {
                    copyDue += (long) BatchReleasePlan.STAGGER_GAP * copyOrdinal;
                }
                if (strike != null || tick >= copyDue) {
                    dueCopies += 1;
                }
            }
            int upcoming = dueCopies * member.spawnCount() * actors.size();
            if (upcoming > CooperativeStructureLimits.PUBLISH_PER_TICK_CAP && actors.size() > 1
                    && dueCopies * member.spawnCount() <= CooperativeStructureLimits.PUBLISH_PER_TICK_CAP) {
                fallbackSolo(CancelReason.BUDGET_EXHAUSTED);
                actors = actorsFor(player);
                upcoming = dueCopies * member.spawnCount();
            }
            if (book != null && book.publishedThisTick + upcoming > CooperativeStructureLimits.PUBLISH_PER_TICK_CAP) {
                restoreStrikeMember(player, member, strike);
                return;
            }
            ActorAttackAdapter adapter = ActorAttackAdapterRegistry.find(member.techniqueId()).orElse(null);
            if (adapter == null) {
                cancel(CancelReason.ADAPTER_MISSING);
                return;
            }
            Set<String> slotBook = strike == null ? publishedSlots : strike.publishedSlots;
            String strokeId = strike == null ? "family" : strike.strikeId.toString();
            Vec3 strokeAim = strike == null ? aim : strike.direction;
            Vec3 strokePoint = strike == null ? contactPoint : strike.point;
            UUID strokeTarget = strike == null ? contactTarget : strike.targetId;
            ReleaseBatch.Tally strokeTally = strike == null
                    ? tallies.get(member.memberOrdinal()) : strike.tallies.get(member.memberOrdinal());
            AttackPreparation shifted = new AttackPreparation(
                    member.techniqueId(), member.chainIndex(), member.prepared().preparation().configuredDamage(),
                    member.prepared().preparation().volume(), member.prepared().preparation().weapon(), strokeAim,
                    member.prepared().preparation().settlement(), member.prepared().preparation().cancelId());
            PreparedAttack prepared = adapter.prepare(shifted);
            List<TechniqueHitEntity> spawned = new ArrayList<>();
            List<String> addedSlots = new ArrayList<>();
            boolean failed = false;
            int emittedNow = 0;
            for (ActorFrame actor : actors) {
                for (int copyOrdinal = 0; copyOrdinal < member.copyCount(); copyOrdinal++) {
                    long copyDue = gateOrigin + member.delayTicks();
                    if (member.stagger()) {
                        copyDue += (long) BatchReleasePlan.STAGGER_GAP * copyOrdinal;
                    }
                    if (strike == null && tick < copyDue) {
                        continue;
                    }
                    for (int spawnOrdinal = 0; spawnOrdinal < member.spawnCount(); spawnOrdinal++) {
                        String slotKey = strokeId + ":" + member.memberOrdinal() + ":" + copyOrdinal + ":"
                                + spawnOrdinal + ":" + actor.actorOrdinal();
                        if (!slotBook.add(slotKey)) {
                            continue;
                        }
                        addedSlots.add(slotKey);
                        LogicalCopyKey copyKey = new LogicalCopyKey(
                                new MemberKey(plan.family(), member.memberOrdinal()), copyOrdinal, spawnOrdinal);
                        ResolvedCopyGeometry geometry = solve(player, level, strokeAim, copyOrdinal, member.copyCount(), strokePoint);
                        List<PreparedEmit> emits = adapter.emit(prepared, copyKey, actor, geometry);
                        if (emits.size() != CooperativeStructureLimits.EMIT_PER_COPY_PER_ACTOR) {
                            failed = true;
                            break;
                        }
                        int settlementOrdinal = settlementOrdinal(member.memberOrdinal(), copyOrdinal, spawnOrdinal);
                        boolean entered = CooperativeVolumePublisher.publish(
                                level, player, emits.get(0), member.techniqueId(),
                                member.prepared().preparation().configuredDamage().amount(),
                                strokeTally, settlementOrdinal,
                                member.prepared().preparation().settlement().enchantments(), spawned::add, strokeTarget);
                        if (!entered) {
                            failed = true;
                            break;
                        }
                        emittedNow += 1;
                        note(PerfCounter.COOPERATIVE_EMITS);
                        if (book != null) {
                            book.publishedThisTick += 1;
                        }
                        playSlash(player, actor, tick, member.techniqueId(), strokeId);
                    }
                    if (failed) {
                        break;
                    }
                }
                if (failed) {
                    break;
                }
            }
            if (failed) {
                for (TechniqueHitEntity entity : spawned) {
                    entity.discard();
                }
                slotBook.removeAll(addedSlots);
                if (book != null) {
                    book.publishedThisTick = Math.max(0, book.publishedThisTick - emittedNow);
                }
                if (!retryingSolo && actors.size() > 1) {
                    fallbackSolo(CancelReason.PUBLISH_FAILED);
                    publishMember(player, level, book, member, aim, tick, contactPoint, contactTarget, true, strike);
                }
                restoreStrikeMember(player, member, strike);
                return;
            }
            if (!spawned.isEmpty()) {
                UncommittedShare share = strike == null
                        ? attackShares.getOrDefault(member.memberOrdinal(), UncommittedShare.none())
                        : strike.attackShares.getOrDefault(member.memberOrdinal(), UncommittedShare.none());
                if (!share.kept()) {
                    if (strike == null || share == attackShares.get(member.memberOrdinal())) {
                        rememberCooldown(share, ShareRole.ATTACK_MEMBER, member.memberOrdinal());
                    }
                    share.commitRetained();
                }
                if (actors.size() > 1 && !cooperationCommitted) {
                    rememberCooldown(cooperationShare, ShareRole.COOPERATION, -1);
                    cooperationShare.commitRetained();
                    cooperationCommitted = true;
                    if (reservation != null) {
                        ports.markCommitted(reservation, new MemberKey(plan.family(), member.memberOrdinal()));
                    }
                }
                note(PerfCounter.COOPERATIVE_PUBLISHES);
            } else {
                restoreStrikeMember(player, member, strike);
            }
        }

        private void restoreStrikeMember(ServerPlayer player, AttackMemberPlan member, @Nullable ContactStrike strike) {
            if (strike == null) {
                return;
            }
            UncommittedShare pending = strike.attackShares.get(member.memberOrdinal());
            if (pending != null && !pending.kept()) {
                restoreAttackShare(player, member.memberOrdinal(), pending);
            }
        }

        private int settlementOrdinal(int memberOrdinal, int copyOrdinal, int spawnOrdinal) {
            for (var spawn : plan.spawns()) {
                if (spawn.memberOrdinal() == memberOrdinal && spawn.copyOrdinal() == copyOrdinal
                        && spawn.spawnOrdinal() == spawnOrdinal) {
                    return spawn.settlementOrdinal();
                }
            }
            return copyOrdinal;
        }

        private List<ActorFrame> actorsFor(ServerPlayer player) {
            List<ActorFrame> actors = new ArrayList<>();
            actors.add(ownerFrame(player));
            if (!soloFallback && reservation != null && reservation.actorPresent()) {
                Optional<ActorFrame> ready = ports.readyFrame(reservation);
                ready.ifPresent(actors::add);
                if (ready.isEmpty()) {
                    fallbackSolo(CancelReason.NO_ACTOR);
                }
            }
            return actors;
        }

        private ActorFrame ownerFrame(ServerPlayer player) {
            return new ActorFrame(player.getUUID(), CooperativeStructureLimits.OWNER_ACTOR_ORDINAL,
                    player.position(), player.getEyePosition(), plan.frozenAim(), player.getYRot(), false);
        }

        private ResolvedCopyGeometry solve(ServerPlayer player, ServerLevel level, Vec3 aim, int copyOrdinal, int copyCount,
                                           @Nullable Vec3 contactPoint) {
            Vec3 playerEye = player.getEyePosition();
            Vec3 forcedAnchor = FORCED_ANCHOR.get();
            Vec3 center = contactPoint != null
                    ? contactPoint
                    : (forcedAnchor != null && !recipe.spaceLock() ? forcedAnchor : playerEye);
            if (contactPoint == null && recipe.spaceLock()) {
                Vec3 reach = playerEye.add(aim.scale(32.0D));
                BlockHitResult hit = level.clip(new ClipContext(playerEye, reach, ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, player));
                if (hit.getType() == HitResult.Type.BLOCK) {
                    center = hit.getLocation();
                }
            }
            long seed = plan.family().castId().getMostSignificantBits() ^ (copyOrdinal * 31L);
            CastFrame solved = CastFrame.forAttack(0F, EnchantmentResolution.empty(), null, copyOrdinal, copyCount,
                    center, aim, recipe.spaceLock(), recipe.scatter(), false,
                    recipe.dropPath() ? Trajectory.DROP : Trajectory.STRAIGHT, false, seed, player.getY());
            Vec3 anchor = solved.origin != null ? solved.origin : center;
            if (recipe.dropPath()) {
                Vec3 below = anchor.subtract(0.0D, 16.0D, 0.0D);
                BlockHitResult ground = level.clip(new ClipContext(anchor, below, ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, player));
                if (ground.getType() == HitResult.Type.BLOCK) {
                    anchor = ground.getLocation();
                }
            }
            Vec3 flat = new Vec3(aim.x, 0.0D, aim.z);
            if (flat.lengthSqr() < 1.0E-8D) {
                flat = new Vec3(0.0D, 0.0D, 1.0D);
            }
            return new ResolvedCopyGeometry(anchor, flat.normalize(), 0F, false, solved.overrideGeometry, playerEye);
        }

        private void playSpin(ServerPlayer player) {
            if (plan.gate().timeline() == null || player.getServer() == null) {
                return;
            }
            int windupTicks = plan.gate().timeline().windupTicks();
            int recoveryTicks = plan.gate().timeline().recoveryTicks();
            WeaponDisplaySnapshot weaponSnapshot = plan.attacks().isEmpty()
                    ? null : plan.attacks().get(0).prepared().preparation().weapon();
            long worldAge = player.serverLevel().getGameTime();
            startSpin(player, player.getUUID(), player.getId(), CooperativeStructureLimits.OWNER_BODY_GENERATION,
                    false, player.getYRot(), worldAge, windupTicks, recoveryTicks, weaponSnapshot);
            if (soloFallback || reservation == null || reservation.body() == null) {
                return;
            }
            Optional<ActorFrame> ready = ports.readyFrame(reservation);
            if (ready.isEmpty()) {
                return;
            }
            Entity body = player.serverLevel().getEntity(ready.get().actorId());
            if (body == null) {
                return;
            }
            startSpin(player, ready.get().actorId(), body.getId(), reservation.body().actorGeneration(),
                    true, ready.get().visualYaw(), worldAge, windupTicks, recoveryTicks, weaponSnapshot);
        }

        private void startSpin(ServerPlayer player, UUID actorId, int entityId, long actorGeneration, boolean mirrored,
                               float visualYaw, long worldAge, int windupTicks, int recoveryTicks,
                               @Nullable WeaponDisplaySnapshot weaponSnapshot) {
            long sequence = nextSequence(player.getServer());
            ActionPlayback playback = new ActionPlayback(
                    plan.family().serverSession(), plan.family().dimension(), plan.family(), null, actorId,
                    entityId, actorGeneration, sequence, CooperativeActionIds.SPIN, worldAge, windupTicks,
                    recoveryTicks, visualYaw, mirrored, weaponSnapshot);
            publisher.start(playback);
            playbacks.add(new PlaybackMark(actorId, sequence));
        }

        private void playSlash(ServerPlayer player, ActorFrame actor, long tick, String techniqueId, String strokeId) {
            if (!com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeTechniqueIds.OBLIQUE_SLASH.equals(techniqueId)) {
                return;
            }
            long spinOrigin = runningStarted ? runningTick : startTick;
            if (plan.gate().kind() == ReleaseGateKind.TIMED && plan.gate().timeline() != null
                    && tick < spinOrigin + plan.gate().timeline().endTickOffset()) {
                return;
            }
            int entityId = player.getId();
            long actorGeneration = CooperativeStructureLimits.OWNER_BODY_GENERATION;
            if (actor.actorOrdinal() != CooperativeStructureLimits.OWNER_ACTOR_ORDINAL) {
                Entity body = player.serverLevel().getEntity(actor.actorId());
                if (body == null || reservation == null || reservation.body() == null) {
                    return;
                }
                entityId = body.getId();
                actorGeneration = reservation.body().actorGeneration();
            }
            if (!slashPlayed.add(strokeId + ":" + actor.actorOrdinal())) {
                return;
            }
            long sequence = nextSequence(player.getServer());
            MemberKey member = plan.attacks().isEmpty() ? null
                    : new MemberKey(plan.family(), plan.attacks().get(0).memberOrdinal());
            ActionPlayback playback = new ActionPlayback(
                    plan.family().serverSession(), plan.family().dimension(), plan.family(), member, actor.actorId(),
                    entityId, actorGeneration,
                    sequence, CooperativeActionIds.OBLIQUE_SLASH, player.serverLevel().getGameTime(),
                    CooperativeStructureLimits.IMMEDIATE_SLASH_RELEASE_TICK,
                    CooperativeStructureLimits.IMMEDIATE_SLASH_RECOVERY_TICKS, actor.visualYaw(), actor.mirrored(),
                    plan.attacks().isEmpty() ? null : plan.attacks().get(0).prepared().preparation().weapon());
            publisher.start(playback);
            playbacks.add(new PlaybackMark(actor.actorId(), sequence));
        }

        private long nextSequence(MinecraftServer server) {
            ServerBook book = books.get(server);
            if (book == null) {
                return 1L;
            }
            long sequence = book.nextSequence;
            book.nextSequence += 1L;
            return sequence;
        }

        private void fallbackSolo(CancelReason reason) {
            if (soloFallback) {
                return;
            }
            soloFallback = true;
            if (!cooperationCommitted) {
                cooperationShare.cancel(playerOrNull());
            }
            if (reservation != null && reservation.body() != null) {
                stopActor(reservation.body().actorId());
            }
            if (reservation != null) {
                ports.cancel(reservation, reason);
            }
            note(PerfCounter.COOPERATIVE_FALLBACKS);
        }

        private void rememberCooldown(UncommittedShare share, ShareRole role, int memberOrdinal) {
            if (share == null || share.kept()) {
                return;
            }
            for (var piece : plan.cooldownPieces()) {
                if (piece.role() == role && piece.memberOrdinal() == memberOrdinal) {
                    committedCooldown += piece.cooldownTicks();
                }
            }
        }

        private void finish(ServerPlayer player) {
            if (finishing || lifecycle == ActionLifecycle.CANCELLED) {
                return;
            }
            finishing = true;
            lifecycle = ActionLifecycle.FINISHED;
            actionShare.cancel(player);
            if (!cooperationCommitted) {
                cooperationShare.cancel(player);
            }
            for (UncommittedShare share : attackShares.values()) {
                share.cancel(player);
            }
            cancelLooseStrikes(player);
            if (reservation != null) {
                ports.finish(reservation, cooperationCommitted);
            }
            if (!cooldownDelegated && committedCooldown > 0 && player.getServer() != null) {
                CastCooldown.mark(player, weapon, skillSlot, player.getServer().getTickCount() + committedCooldown);
            }
            ServerBook book = books.get(player.getServer());
            if (book != null) {
                book.families.remove(plan.family());
            }
            releaseExecution();
        }

        /** 执行锁只挡住这一发还没结束时的连放，不改结束时记下的冷却刻数。 */
        private void releaseExecution() {
            if (executionClosed) {
                return;
            }
            executionClosed = true;
            CastCooldown.release(executionKey);
        }

        @Override
        public FamilyKey family() {
            return plan.family();
        }

        @Override
        public ActionLifecycle lifecycle() {
            return lifecycle;
        }

        private void stopActor(UUID actorId) {
            if (actorId == null) {
                return;
            }
            for (PlaybackMark mark : playbacks) {
                if (actorId.equals(mark.actorId)) {
                    publisher.stop(plan.family(), mark.actorId, mark.sequence);
                }
            }
        }

        @Override
        public void cancel(CancelReason reason) {
            if (lifecycle == ActionLifecycle.CANCELLED || lifecycle == ActionLifecycle.FINISHED) {
                return;
            }
            if (reason == CancelReason.CARRIER_INVALID) {
                releaseUncommitted(playerOrNull());
                lifecycle = ActionLifecycle.CANCELLED;
                if (reservation != null) {
                    ports.cancel(reservation, reason);
                }
                removeFromBook(playerOrNull());
                releaseExecution();
                return;
            }
            boolean hard = reason != null && reason.hard();
            lifecycle = ActionLifecycle.CANCELLED;
            ServerPlayer player = playerOrNull();
            if (!actionShare.kept()) {
                actionShare.cancel(player);
            }
            if (!cooperationCommitted) {
                cooperationShare.cancel(player);
            }
            for (UncommittedShare share : attackShares.values()) {
                if (!share.kept()) {
                    share.cancel(player);
                }
            }
            cancelLooseStrikes(player);
            if (hard) {
                for (CooperativeSettlementGuard guard : guards.values()) {
                    guard.cancel(reason);
                }
                for (ReleaseBatch.Tally tally : tallies.values()) {
                    tally.dropGuardedSettlement();
                }
                for (ContactStrike strike : contactStrikes) {
                    for (CooperativeSettlementGuard guard : strike.guards.values()) {
                        guard.cancel(reason);
                    }
                    for (ReleaseBatch.Tally tally : strike.tallies.values()) {
                        tally.dropGuardedSettlement();
                    }
                }
                for (PlaybackMark mark : playbacks) {
                    publisher.stop(plan.family(), mark.actorId, mark.sequence);
                }
            }
            if (reservation != null) {
                ports.cancel(reservation, reason == null ? CancelReason.PARENT_CANCELLED : reason);
            }
            if (player != null && !cooldownDelegated && committedCooldown > 0 && player.getServer() != null) {
                CastCooldown.mark(player, weapon, skillSlot, player.getServer().getTickCount() + committedCooldown);
            }
            removeFromBook(player);
            releaseExecution();
        }

        private void releaseUncommitted(@Nullable ServerPlayer player) {
            if (!actionShare.kept()) {
                actionShare.cancel(player);
            }
            if (!cooperationCommitted) {
                cooperationShare.cancel(player);
            }
            for (UncommittedShare share : attackShares.values()) {
                if (!share.kept()) {
                    share.cancel(player);
                }
            }
            cancelLooseStrikes(player);
        }

        private void removeFromBook(@Nullable ServerPlayer player) {
            MinecraftServer server = player == null ? null : player.getServer();
            ServerBook book = server == null ? null : books.get(server);
            if (book != null) {
                book.families.remove(plan.family());
            }
        }

        private @Nullable ServerPlayer playerOrNull() {
            for (Map.Entry<MinecraftServer, ServerBook> entry : books.entrySet()) {
                if (entry.getValue().families.containsKey(plan.family()) || entry.getKey().getPlayerList() != null) {
                    ServerPlayer player = entry.getKey().getPlayerList().getPlayer(plan.family().ownerId());
                    if (player != null) {
                        return player;
                    }
                }
            }
            return null;
        }

        private void onContact(TriggerGeometry geometry, ServerPlayer player) {
            if (lifecycle == ActionLifecycle.CANCELLED || lifecycle == ActionLifecycle.FINISHED
                    || geometry == null || geometry.targetId() == null || player.getServer() == null) {
                return;
            }
            if (!contactTargets.add(geometry.targetId())) {
                return;
            }
            Entity found = player.serverLevel().getEntity(geometry.targetId());
            if (!(found instanceof LivingEntity living) || !ActionWindow.hostileTo(player, living)) {
                contactTargets.remove(geometry.targetId());
                return;
            }
            Vec3 point = geometry.point() == null ? living.position() : geometry.point();
            ContactStrike strike = openContactStrike(player, geometry.targetId(), point, geometry.direction());
            if (strike == null) {
                return;
            }
            contactStrikes.add(strike);
            ServerBook book = books.get(player.getServer());
            long tick = player.getServer().getTickCount();
            for (AttackMemberPlan member : plan.attacks()) {
                if (member.continueLink() != null && !continueReady.contains(member.memberOrdinal())) {
                    continue;
                }
                publishMember(player, player.serverLevel(), book, member, strike.direction, tick,
                        strike.point, strike.targetId, false, strike);
            }
        }

        private @Nullable ContactStrike openContactStrike(ServerPlayer player, UUID targetId, Vec3 point, Vec3 direction) {
            ContactStrike strike = new ContactStrike(UUID.randomUUID(), targetId, point, direction);
            bindStrikeTallies(player, strike);
            for (AttackMemberPlan member : plan.attacks()) {
                UncommittedShare share = takeAttackShare(player, member.memberOrdinal());
                if (share == null) {
                    for (Map.Entry<Integer, UncommittedShare> taken : strike.attackShares.entrySet()) {
                        restoreAttackShare(player, taken.getKey(), taken.getValue());
                    }
                    for (CooperativeSettlementGuard guard : strike.guards.values()) {
                        guard.cancel(CancelReason.VALIDATION_FAILED);
                    }
                    for (ReleaseBatch.Tally tally : strike.tallies.values()) {
                        tally.dropGuardedSettlement();
                    }
                    return null;
                }
                strike.attackShares.put(member.memberOrdinal(), share);
            }
            return strike;
        }

        private void bindStrikeTallies(ServerPlayer player, ContactStrike strike) {
            ReleaseBatch previous = ReleaseBatch.current();
            ReleaseBatch opened = ReleaseBatch.open(player, null);
            try {
                for (AttackMemberPlan member : plan.attacks()) {
                    CooperativeSettlementGuard guard = new CooperativeSettlementGuard(player.getUUID(), plan.family());
                    ReleaseBatch.Tally tally = opened.openGroupTally();
                    tally.bindSettlementGuard(guard);
                    strike.guards.put(member.memberOrdinal(), guard);
                    strike.tallies.put(member.memberOrdinal(), tally);
                }
            } finally {
                ReleaseBatch.bind(previous);
            }
        }

        private @Nullable UncommittedShare takeAttackShare(ServerPlayer player, int memberOrdinal) {
            ArrayDeque<UncommittedShare> pool = attackPools.get(memberOrdinal);
            if (pool != null && !pool.isEmpty()) {
                return pool.poll();
            }
            int amount = 0;
            for (ShareSlot slot : plan.shares().slots()) {
                if (slot.role() == ShareRole.ATTACK_MEMBER && slot.memberOrdinal() == memberOrdinal) {
                    amount = slot.amount();
                    break;
                }
            }
            if (amount <= 0) {
                return UncommittedShare.none();
            }
            ManaPaymentReceipt charged = ManaPaymentReceipt.charge(player, amount, false);
            if (charged == null) {
                return null;
            }
            return UncommittedShare.open(charged, amount);
        }

        private void restoreAttackShare(ServerPlayer player, int memberOrdinal, UncommittedShare share) {
            if (share == null || share.kept()) {
                return;
            }
            if (share == attackShares.get(memberOrdinal)) {
                ArrayDeque<UncommittedShare> pool = attackPools.computeIfAbsent(memberOrdinal, ignored -> new ArrayDeque<>());
                if (!pool.contains(share)) {
                    pool.addFirst(share);
                }
                return;
            }
            share.cancel(player);
        }

        private void cancelLooseStrikes(@Nullable ServerPlayer player) {
            for (ContactStrike strike : contactStrikes) {
                for (UncommittedShare share : strike.attackShares.values()) {
                    if (!share.kept()) {
                        share.cancel(player);
                    }
                }
            }
        }

        private void onWindowFinished() {
            windowFinished = true;
        }
    }

    private static final class ContactStrike {
        private final UUID strikeId;
        private final UUID targetId;
        private final Vec3 point;
        private final Vec3 direction;
        private final Map<Integer, ReleaseBatch.Tally> tallies = new HashMap<>();
        private final Map<Integer, CooperativeSettlementGuard> guards = new HashMap<>();
        private final Map<Integer, UncommittedShare> attackShares = new HashMap<>();
        private final Set<String> publishedSlots = new HashSet<>();

        private ContactStrike(UUID strikeId, UUID targetId, Vec3 point, Vec3 direction) {
            this.strikeId = strikeId;
            this.targetId = targetId;
            this.point = point;
            this.direction = direction;
        }
    }

    private final class DashSource implements ContactFamilySource {
        private final RunningFamily running;
        private boolean closed;

        private DashSource(RunningFamily running) {
            this.running = running;
        }

        @Override
        public ReleaseGateHandle openSubFamily(TriggerGeometry geometry) {
            return new OnceGate(running, geometry);
        }

        @Override
        public void onWindowEnded() {
            closed = true;
        }

        @Override
        public void onWindowFinished() {
            closed = true;
            running.onWindowFinished();
        }

        @Override
        public boolean windowClosed() {
            return closed || running.lifecycle == ActionLifecycle.CANCELLED;
        }
    }

    private final class OnceGate implements ReleaseGateHandle {
        private final RunningFamily running;
        private final TriggerGeometry geometry;
        private boolean triggered;

        private OnceGate(RunningFamily running, TriggerGeometry geometry) {
            this.running = running;
            this.geometry = geometry;
        }

        @Override
        public FamilyKey family() {
            return running.plan.family();
        }

        @Override
        public ReleaseGateKind kind() {
            return ReleaseGateKind.CONTACT;
        }

        @Override
        public boolean trigger(TriggerGeometry ignored) {
            if (triggered || running.lifecycle == ActionLifecycle.CANCELLED) {
                return false;
            }
            triggered = true;
            ServerPlayer player = running.playerOrNull();
            if (player != null) {
                running.onContact(geometry, player);
            }
            return true;
        }

        @Override
        public void cancel(CancelReason reason) {
            triggered = true;
        }

        @Override
        public boolean closed() {
            return triggered;
        }

        @Override
        public ActionLifecycle lifecycle() {
            return triggered ? ActionLifecycle.RELEASED : ActionLifecycle.PREPARED;
        }
    }

    private record PlaybackMark(UUID actorId, long sequence) {
    }
}
