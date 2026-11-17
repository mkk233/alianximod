package com.bslx.XueXiMod.shu.technique.release.clone.core;

import com.bslx.XueXiMod.shu.technique.release.cooperative.api.AcceptedRemoteRecall;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.AcceptedReposition;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ActorFrame;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CancelReason;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CloneDeploymentInput;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CloneDeploymentResult;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CloneStandaloneDeployment;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperationActors;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperationReservation;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperationSettings;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeStructureLimits;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.FamilyKey;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.MemberKey;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.PreparationState;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.RecallAttempt;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.RecallKind;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.RecallRoute;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.RejectedReposition;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.RemoteCheckPhase;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.RemoteRecall;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.RemoteRecallMeter;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.RemoteSpaceGrant;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.RepositionAttempt;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.RepositionMotion;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ServerSessionLookup;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ShortReposition;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.StandaloneDeploymentKind;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.UUID;

/**
 * 分身核心交给整合者的端口。
 * 同一个实例实现协同预约、短距移动、远距归位和普通独立部署。
 * 冻结的 moveLimit 可以大于 8 格；实际位移不超过 8 格才走短距，更远只走一次远距归位。
 * {@code ServerSessionLookup#cloneSessionToken} 必须等于该服务器 {@link CloneRuntime#sessionToken()}。
 */
public final class CloneServerPorts implements CooperationActors, ShortReposition, CloneStandaloneDeployment, RemoteRecall {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double VERTICAL_AIM = 1.0E-4D;
    private static final double SAME_SPOT = 1.0E-8D;

    private final ServerSessionLookup sessions;

    private CloneServerPorts(ServerSessionLookup sessions) {
        this.sessions = sessions;
    }

    public static CloneServerPorts create(ServerSessionLookup sessions) {
        if (sessions == null) {
            throw new IllegalArgumentException("分身端口缺少服务器会话对照");
        }
        return new CloneServerPorts(sessions);
    }

    @Override
    public CooperationReservation tryReserve(ServerPlayer owner, FamilyKey family, ActorFrame ownerFrame,
                                             CooperationSettings settings, long expiryTick) {
        if (family == null) {
            throw new IllegalArgumentException("协同预约缺少攻击族");
        }
        if (owner == null || ownerFrame == null || settings == null || owner.getServer() == null) {
            return absent(family, expiryTick);
        }
        if (!CloneService.sameThread(owner) || !owner.isAlive() || owner.isRemoved() || owner.hasDisconnected()) {
            return absent(family, expiryTick);
        }
        CloneRuntime runtime = CloneRuntime.get(owner.getServer());
        if (runtime == null || !runtime.accepting() || !sameSession(owner.getServer(), family.serverSession())) {
            return absent(family, expiryTick);
        }
        if (!owner.getUUID().equals(family.ownerId()) || !owner.level().dimension().equals(family.dimension())) {
            return absent(family, expiryTick);
        }
        CloneCooperationHold existing = runtime.holdForFamily(family);
        if (existing != null) {
            CloneRuntimeState held = runtime.stateForCooperation(existing.token);
            if (held != null && held.cooperationHold == existing && !existing.closed) {
                return published(runtime, held, existing);
            }
        }
        if (CloneSourceScope.blocksResponse() || runtime.workRemaining() <= 0 || runtime.cooperationFull()) {
            return absent(family, expiryTick);
        }
        long now = runtime.tick();
        long expiry = Math.min(expiryTick, now + CooperativeStructureLimits.UNCOMMITTED_RESERVATION_TICKS);
        if (expiry < now) {
            return absent(family, expiryTick);
        }
        Vec3 forward = horizontalForward(ownerFrame);
        BodyChoice choice = chooseBody(runtime, owner, family, ownerFrame, settings, forward);
        if (choice == null || (!choice.remotePlaced && choice.plan == null)) {
            return absent(family, expiry);
        }
        CloneRuntimeState state = choice.state;
        if (choice.remotePlaced) {
            if (!selectable(state, owner, family) || runtime.cooperationFull()) {
                return absent(family, expiry);
            }
            CloneCooperationHold hold = new CloneCooperationHold(UUID.randomUUID(), family, state.actorGeneration, expiry,
                    now, settings.prepareTicks(), ownerFrame.aim(), forward, choice.rightSide, settings.sideOffset());
            hold.arrived = true;
            faceShared(state.actor, hold.sharedAim);
            runtime.trackCooperation(state, hold);
            return published(runtime, state, hold);
        }
        if (!selectable(state, owner, family) || runtime.cooperationFull() || !runtime.trySpendWork()) {
            this.cancel(choice.plan);
            return absent(family, expiry);
        }
        CloneCooperationHold hold = new CloneCooperationHold(UUID.randomUUID(), family, state.actorGeneration, expiry,
                now, settings.prepareTicks(), ownerFrame.aim(), forward, choice.rightSide, settings.sideOffset());
        hold.reposition = choice.plan;
        CloneRepositionPlan storedPlan = runtime.repositionPlan(choice.plan.planId());
        if (storedPlan != null && storedPlan.arrived()) {
            hold.arrived = true;
            faceShared(state.actor, hold.sharedAim);
        }
        runtime.trackCooperation(state, hold);
        return published(runtime, state, hold);
    }

    @Override
    public PreparationState advance(ServerLevel level, CooperationReservation reservation, ActorFrame currentOwnerFrame,
                                    long tick) {
        Resolved resolved = resolve(level, reservation);
        if (resolved == null) {
            return PreparationState.UNAVAILABLE;
        }
        CloneCooperationHold hold = resolved.hold;
        if (hold.arrived) {
            faceShared(resolved.state.actor, hold.sharedAim);
            return PreparationState.READY;
        }
        if (!bodyReady(resolved, currentOwnerFrame)) {
            if (!hold.committed) {
                resolved.runtime.releaseCooperation(resolved.state, false);
            }
            return PreparationState.UNAVAILABLE;
        }
        if (hold.advanceCount >= hold.prepareTicks || tick > hold.expiryTick) {
            if (!hold.committed) {
                resolved.runtime.releaseCooperation(resolved.state, false);
            }
            return PreparationState.UNAVAILABLE;
        }
        if (hold.reposition == null) {
            if (!hold.committed) {
                resolved.runtime.releaseCooperation(resolved.state, false);
            }
            return PreparationState.UNAVAILABLE;
        }
        CloneRepositionPlan storedPlan = resolved.runtime.repositionPlan(hold.reposition.planId());
        if (storedPlan != null && storedPlan.lastAdvancedTick == tick) {
            return PreparationState.PREPARING;
        }
        if (resolved.runtime.workRemaining() <= 0 || !resolved.runtime.trySpendWork()) {
            if (!hold.committed) {
                resolved.runtime.releaseCooperation(resolved.state, false);
            }
            return PreparationState.UNAVAILABLE;
        }
        hold.advanceCount++;
        RepositionMotion motion = advance(level, hold.reposition, tick);
        if (motion == RepositionMotion.FAILED) {
            hold.failure = CancelReason.ACTOR_BLOCKED;
            if (!hold.committed) {
                resolved.runtime.releaseCooperation(resolved.state, false);
            }
            return PreparationState.UNAVAILABLE;
        }
        if (motion == RepositionMotion.ARRIVED) {
            hold.arrived = true;
            faceShared(resolved.state.actor, hold.sharedAim);
            return PreparationState.READY;
        }
        if (hold.advanceCount >= hold.prepareTicks) {
            if (!hold.committed) {
                resolved.runtime.releaseCooperation(resolved.state, false);
            }
            return PreparationState.UNAVAILABLE;
        }
        return PreparationState.PREPARING;
    }

    @Override
    public Optional<ActorFrame> readyFrame(CooperationReservation reservation) {
        if (reservation == null || !reservation.actorPresent() || reservation.body() == null) {
            return Optional.empty();
        }
        CloneRuntime runtime = CloneRuntime.runtimeForSession(reservation.body().sessionToken());
        if (runtime == null) {
            return Optional.empty();
        }
        CloneRuntimeState state = runtime.stateForCooperation(reservation.token());
        CloneCooperationHold hold = state == null ? null : state.cooperationHold;
        if (hold == null || hold.closed || !hold.arrived || !hold.token.equals(reservation.token())) {
            return Optional.empty();
        }
        CloneEntity actor = state.actor;
        if (actor == null || !actor.isAlive() || actor.isRemoved()) {
            return Optional.empty();
        }
        return Optional.of(cloneFrame(actor, hold.sharedAim));
    }

    @Override
    public boolean validate(CooperationReservation reservation, ActorFrame ownerFrame) {
        if (reservation == null || ownerFrame == null || !reservation.actorPresent() || reservation.body() == null) {
            return false;
        }
        CloneRuntime runtime = CloneRuntime.runtimeForSession(reservation.body().sessionToken());
        if (runtime == null || !runtime.accepting()) {
            return false;
        }
        CloneRuntimeState state = runtime.stateForCooperation(reservation.token());
        CloneCooperationHold hold = state == null ? null : state.cooperationHold;
        if (!matchingHold(state, hold, reservation) || !hold.arrived) {
            return false;
        }
        if (CloneSourceScope.blocksResponse()) {
            return false;
        }
        if (!hold.committed && runtime.tick() > hold.expiryTick) {
            return false;
        }
        MinecraftServer server = runtime.server();
        if (!sameSession(server, reservation.family().serverSession())) {
            return false;
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(state.ownerId);
        if (owner == null || !owner.isAlive() || owner.isRemoved() || owner.hasDisconnected()) {
            return false;
        }
        if (!owner.level().dimension().equals(state.dimension) || !reservation.family().dimension().equals(state.dimension)) {
            return false;
        }
        CloneEntity actor = state.actor;
        if (!(actor.level() instanceof ServerLevel level) || actor.level() != owner.level()) {
            return false;
        }
        Vec3 expected = stanceFeet(ownerFrame.feet(), hold.frozenForward, hold.rightStance, hold.sideOffset);
        if (actor.position().distanceToSqr(expected)
                > CooperativeStructureLimits.STANCE_DEVIATION_BLOCKS * CooperativeStructureLimits.STANCE_DEVIATION_BLOCKS) {
            return false;
        }
        return CloneBodySweep.stepProblem(level, actor, actor.position(), actor.position()) == null;
    }

    @Override
    public void markCommitted(CooperationReservation reservation, MemberKey member) {
        CloneRuntimeState state = holdState(reservation);
        if (state == null || member == null || state.cooperationHold == null) {
            return;
        }
        CloneCooperationHold hold = state.cooperationHold;
        if (hold.closed || !hold.token.equals(reservation.token()) || !hold.family.equals(member.family())) {
            return;
        }
        if (!hold.committedMembers.add(member.memberOrdinal())) {
            return;
        }
        hold.committed = true;
        CloneRuntime runtime = CloneRuntime.runtimeForSession(reservation.body().sessionToken());
        if (runtime != null) {
            runtime.trySpendWork();
        }
    }

    @Override
    public void finish(CooperationReservation reservation, boolean anyCommitted) {
        closeReservation(reservation);
    }

    @Override
    public void cancel(CooperationReservation reservation, CancelReason reason) {
        closeReservation(reservation);
    }

    @Override
    public RecallAttempt recall(ServerLevel level, CloneHandle handle, Vec3 destination, double frozenMoveLimit) {
        return CloneRemoteRecall.move(level, handle, destination, frozenMoveLimit);
    }

    @Override
    public RepositionAttempt prepare(ServerLevel level, CloneHandle handle, Vec3 destination, double distanceLimit,
                                     int durationTicks) {
        if (level == null || handle == null || !level.getServer().isSameThread()) {
            return new RejectedReposition(CancelReason.VALIDATION_FAILED);
        }
        if (destination == null || !Double.isFinite(destination.x) || !Double.isFinite(destination.y)
                || !Double.isFinite(destination.z)) {
            return new RejectedReposition(CancelReason.UNSAFE_STANCE);
        }
        if (!Double.isFinite(distanceLimit) || distanceLimit < 0.0D
                || distanceLimit > CooperativeStructureLimits.REPOSITION_DISTANCE_CAP
                || durationTicks < 1 || durationTicks > CooperativeStructureLimits.REPOSITION_TICK_CAP) {
            return new RejectedReposition(CancelReason.VALIDATION_FAILED);
        }
        CloneRuntime runtime = CloneRuntime.get(level.getServer());
        if (runtime == null || !runtime.accepting() || runtime.sessionToken() != handle.sessionToken()
                || !level.dimension().equals(handle.dimension())) {
            return new RejectedReposition(CancelReason.VALIDATION_FAILED);
        }
        CloneRuntimeState state = runtime.find(handle.actorId());
        if (state == null || state.actorGeneration != handle.actorGeneration() || !CloneBodySweep.movableOrdinary(state)) {
            return new RejectedReposition(CancelReason.VALIDATION_FAILED);
        }
        CloneEntity actor = state.actor;
        if (actor.level() != level) {
            return new RejectedReposition(CancelReason.REGION_UNLOADED);
        }
        if (state.activePlanId != null) {
            CloneRepositionPlan active = runtime.repositionPlan(state.activePlanId);
            if (active != null && !active.closed && !active.arrived()) {
                return new RejectedReposition(CancelReason.ACTOR_BLOCKED);
            }
            runtime.forgetReposition(state.activePlanId);
        }
        CancelReason path = CloneBodySweep.pathProblem(level, actor, destination, distanceLimit);
        if (path != null) {
            return new RejectedReposition(path);
        }
        Vec3 originFeet = actor.position();
        CloneRepositionPlan plan = new CloneRepositionPlan(UUID.randomUUID(), state.handle(runtime.sessionToken()),
                originFeet, destination, durationTicks);
        if (originFeet.distanceToSqr(destination) <= SAME_SPOT) {
            plan.completedTicks = durationTicks;
        }
        runtime.rememberReposition(state, plan);
        return new AcceptedReposition(plan.planId, plan.handle, destination, distanceLimit, durationTicks);
    }

    @Override
    public RepositionMotion advance(ServerLevel level, AcceptedReposition plan, long tick) {
        if (level == null || plan == null || !level.getServer().isSameThread()) {
            return RepositionMotion.FAILED;
        }
        CloneRuntime runtime = CloneRuntime.get(level.getServer());
        if (runtime == null || !runtime.accepting()) {
            return RepositionMotion.FAILED;
        }
        CloneRepositionPlan stored = runtime.repositionPlan(plan.planId());
        if (stored == null || stored.closed || !stored.handle.actorId().equals(plan.handle().actorId())) {
            return RepositionMotion.FAILED;
        }
        if (stored.arrived()) {
            return RepositionMotion.ARRIVED;
        }
        if (stored.lastAdvancedTick == tick) {
            return RepositionMotion.RUNNING;
        }
        CloneRuntimeState state = runtime.find(stored.handle.actorId());
        if (state == null || state.actorGeneration != stored.handle.actorGeneration()
                || !CloneBodySweep.movableOrdinary(state) || !(state.actor.level() instanceof ServerLevel bodyLevel)
                || bodyLevel != level) {
            stored.failure = CancelReason.ACTOR_DEATH;
            runtime.forgetReposition(stored.planId);
            return RepositionMotion.FAILED;
        }
        CloneEntity actor = state.actor;
        double fraction = (stored.completedTicks + 1.0D) / stored.durationTicks;
        Vec3 nextFeet = stored.originFeet.lerp(stored.targetFeet, fraction);
        CancelReason step = CloneBodySweep.stepProblem(level, actor, actor.position(), nextFeet);
        if (step != null) {
            stored.failure = step;
            runtime.forgetReposition(stored.planId);
            return RepositionMotion.FAILED;
        }
        actor.moveTo(nextFeet.x, nextFeet.y, nextFeet.z, actor.getYRot(), actor.getXRot());
        actor.setDeltaMovement(Vec3.ZERO);
        actor.fallDistance = 0.0F;
        actor.setNoGravity(true);
        stored.completedTicks++;
        stored.lastAdvancedTick = tick;
        if (stored.arrived()) {
            actor.setNoGravity(false);
            return RepositionMotion.ARRIVED;
        }
        return RepositionMotion.RUNNING;
    }

    @Override
    public void cancel(AcceptedReposition plan) {
        if (plan == null) {
            return;
        }
        CloneRuntime runtime = CloneRuntime.runtimeForSession(plan.handle().sessionToken());
        if (runtime == null) {
            return;
        }
        runtime.forgetReposition(plan.planId());
    }

    @Override
    public CloneDeploymentResult deployStandalone(ServerPlayer owner, CloneDeploymentInput input) {
        return CloneService.deployStandaloneBody(owner, input);
    }

    @Override
    public int dismissStandalone(ServerPlayer owner, StandaloneDeploymentKind kind) {
        if (kind != StandaloneDeploymentKind.NORMAL) {
            return 0;
        }
        return CloneService.dismissStandaloneBodies(owner);
    }

    private void closeReservation(CooperationReservation reservation) {
        CloneRuntimeState state = holdState(reservation);
        if (state == null || state.cooperationHold == null) {
            return;
        }
        CloneRuntime runtime = CloneRuntime.runtimeForSession(reservation.body().sessionToken());
        if (runtime == null) {
            return;
        }
        runtime.releaseCooperation(state, state.cooperationHold.committed);
    }

    @Nullable
    private CloneRuntimeState holdState(CooperationReservation reservation) {
        if (reservation == null || !reservation.actorPresent() || reservation.body() == null) {
            return null;
        }
        CloneRuntime runtime = CloneRuntime.runtimeForSession(reservation.body().sessionToken());
        if (runtime == null) {
            return null;
        }
        CloneRuntimeState state = runtime.stateForCooperation(reservation.token());
        if (!matchingHold(state, state == null ? null : state.cooperationHold, reservation)) {
            return null;
        }
        return state;
    }

    private static boolean matchingHold(@Nullable CloneRuntimeState state, @Nullable CloneCooperationHold hold,
                                        CooperationReservation reservation) {
        return state != null && hold != null && !hold.closed && hold.token.equals(reservation.token())
                && state.actorGeneration == reservation.bodyGeneration()
                && state.actorGeneration == hold.bodyGeneration
                && hold.family.equals(reservation.family());
    }

    private boolean bodyReady(Resolved resolved, @Nullable ActorFrame ownerFrame) {
        if (ownerFrame == null || CloneSourceScope.blocksResponse()) {
            return false;
        }
        CloneRuntimeState state = resolved.state;
        CloneEntity actor = state.actor;
        if (!CloneBodySweep.movableOrdinary(state) || state.cooperationHold != resolved.hold) {
            return false;
        }
        if (state.response != CloneResponseState.RESERVED || actor.level() != resolved.level) {
            return false;
        }
        MinecraftServer server = resolved.runtime.server();
        if (!sameSession(server, resolved.hold.family.serverSession())) {
            return false;
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(state.ownerId);
        return owner != null && owner.isAlive() && !owner.isRemoved() && !owner.hasDisconnected()
                && owner.level() == resolved.level;
    }

    @Nullable
    private Resolved resolve(@Nullable ServerLevel level, @Nullable CooperationReservation reservation) {
        if (level == null || reservation == null || !reservation.actorPresent() || reservation.body() == null
                || !level.getServer().isSameThread()) {
            return null;
        }
        CloneRuntime runtime = CloneRuntime.get(level.getServer());
        if (runtime == null || runtime.sessionToken() != reservation.body().sessionToken()) {
            return null;
        }
        CloneRuntimeState state = runtime.stateForCooperation(reservation.token());
        if (!matchingHold(state, state == null ? null : state.cooperationHold, reservation)) {
            return null;
        }
        if (!level.dimension().equals(state.dimension)) {
            return null;
        }
        return new Resolved(runtime, state, state.cooperationHold, level);
    }

    @Nullable
    private BodyChoice chooseBody(CloneRuntime runtime, ServerPlayer owner, FamilyKey family, ActorFrame ownerFrame,
                                  CooperationSettings settings, Vec3 forward) {
        ArrayList<RankedBody> ranked = new ArrayList<>();
        for (CloneRuntimeState state : runtime.ownedActors(owner.getUUID())) {
            if (!selectable(state, owner, family)) {
                continue;
            }
            double distanceSquared = state.actor.position().distanceToSqr(ownerFrame.feet());
            double allowed = settings.selectionRadius() == 0.0D
                    ? SAME_SPOT
                    : settings.selectionRadius() * settings.selectionRadius();
            if (distanceSquared > allowed) {
                continue;
            }
            ranked.add(new RankedBody(state, distanceSquared));
        }
        ranked.sort((leftBody, rightBody) -> {
            int byDistance = Double.compare(leftBody.distanceSquared, rightBody.distanceSquared);
            if (byDistance != 0) {
                return byDistance;
            }
            return leftBody.state.actorId.compareTo(rightBody.state.actorId);
        });
        EndpointLedger ledger = new EndpointLedger();
        int inspected = 0;
        for (RankedBody candidate : ranked) {
            if (inspected >= CooperativeStructureLimits.OWNER_BODY_LOOKUP_CAP) {
                break;
            }
            inspected++;
            ledger.beginBody();
            try {
                StanceSearch search = this.searchBody(runtime, settings, forward, ownerFrame.feet(),
                        candidate.state, ledger);
                if (search.halt) {
                    return null;
                }
                if (search.choice != null) {
                    return search.choice;
                }
            } catch (RuntimeException exception) {
                LOGGER.error("协同选位失败 actor={}", candidate.state.actorId, exception);
            }
        }
        return null;
    }

    private StanceSearch searchBody(CloneRuntime runtime, CooperationSettings settings, Vec3 forward, Vec3 ownerFeet,
                                    CloneRuntimeState state, EndpointLedger ledger) {
        if (!(state.actor.level() instanceof ServerLevel level)) {
            return StanceSearch.none();
        }
        Vec3 rightFeet = stanceFeet(ownerFeet, forward, true, settings.sideOffset());
        Vec3 leftFeet = stanceFeet(ownerFeet, forward, false, settings.sideOffset());
        StanceOption right = stanceOption(state.actor, rightFeet, true, settings.moveLimit());
        StanceOption left = stanceOption(state.actor, leftFeet, false, settings.moveLimit());
        boolean rightFirst = right.distanceSquared <= left.distanceSquared;
        StanceOption first = rightFirst ? right : left;
        StanceOption second = rightFirst ? left : right;
        StanceMark firstMark = this.markStance(level, runtime, state.actor, first, settings, ledger);
        if (firstMark == StanceMark.ABORT) {
            return StanceSearch.halt();
        }
        boolean needSecond = firstMark != StanceMark.LEGAL || second.distanceSquared < first.distanceSquared;
        StanceMark secondMark = StanceMark.ILLEGAL;
        if (needSecond) {
            secondMark = this.markStance(level, runtime, state.actor, second, settings, ledger);
            if (secondMark == StanceMark.ABORT) {
                return StanceSearch.halt();
            }
        }
        StanceOption winner = pickStance(first, firstMark, second, secondMark);
        if (winner == null) {
            return StanceSearch.none();
        }
        if (winner.kind == RecallKind.SHORT) {
            double sweepLimit = RecallRoute.shortSweepLimit(winner.travel, settings.moveLimit());
            RepositionAttempt attempt = this.prepare(level, state.handle(runtime.sessionToken()), winner.feet,
                    sweepLimit, settings.prepareTicks());
            if (!(attempt instanceof AcceptedReposition accepted)) {
                return StanceSearch.none();
            }
            return StanceSearch.found(new BodyChoice(state, accepted, winner.rightSide, winner.distanceSquared, false));
        }
        if (winner.kind != RecallKind.REMOTE_RECALL) {
            return StanceSearch.none();
        }
        if (runtime.workRemaining() < 2 || runtime.cooperationFull()) {
            return StanceSearch.halt();
        }
        RecallAttempt attempt = this.recall(level, state.handle(runtime.sessionToken()), winner.feet, settings.moveLimit());
        if (!(attempt instanceof AcceptedRemoteRecall)) {
            return StanceSearch.halt();
        }
        return StanceSearch.found(new BodyChoice(state, null, winner.rightSide, winner.distanceSquared, true));
    }

    private StanceMark markStance(ServerLevel level, CloneRuntime runtime, CloneEntity actor, StanceOption option,
                                  CooperationSettings settings, EndpointLedger ledger) {
        if (option.kind == RecallKind.OUT_OF_RANGE) {
            return StanceMark.ILLEGAL;
        }
        if (option.kind == RecallKind.SHORT) {
            double sweepLimit = RecallRoute.shortSweepLimit(option.travel, settings.moveLimit());
            CancelReason path = CloneBodySweep.pathProblem(level, actor, option.feet, sweepLimit);
            return path == null ? StanceMark.LEGAL : StanceMark.ILLEGAL;
        }
        if (option.kind != RecallKind.REMOTE_RECALL) {
            return StanceMark.ILLEGAL;
        }
        if (!ledger.accountSource(level, runtime, actor)) {
            return StanceMark.ABORT;
        }
        if (!ledger.sourceSafe) {
            return StanceMark.ILLEGAL;
        }
        int added = CloneBodySweep.absorbFootprint(actor, option.feet, ledger.cells,
                CooperativeStructureLimits.REMOTE_ENDPOINT_CELL_CAP);
        if (added < 0) {
            return StanceMark.ABORT;
        }
        if (added > 0) {
            Optional<RemoteSpaceGrant> grant = RemoteRecallMeter.take(runtime.tick(), RemoteCheckPhase.ENDPOINT,
                    added, 0, 0, 0);
            if (grant.isEmpty()) {
                return StanceMark.ABORT;
            }
        }
        CancelReason endpoint = CloneBodySweep.endpointProblem(level, actor, option.feet);
        return endpoint == null ? StanceMark.LEGAL : StanceMark.ILLEGAL;
    }

    private static StanceOption stanceOption(CloneEntity actor, Vec3 feet, boolean rightSide, double moveLimit) {
        double travel = actor.position().distanceTo(feet);
        return new StanceOption(feet, travel, actor.position().distanceToSqr(feet), rightSide,
                RecallRoute.kindFor(travel, moveLimit));
    }

    @Nullable
    private static StanceOption pickStance(StanceOption first, StanceMark firstMark, StanceOption second,
                                           StanceMark secondMark) {
        boolean firstLegal = firstMark == StanceMark.LEGAL;
        boolean secondLegal = secondMark == StanceMark.LEGAL;
        if (firstLegal && secondLegal) {
            if (second.distanceSquared < first.distanceSquared) {
                return second;
            }
            return first;
        }
        if (firstLegal) {
            return first;
        }
        if (secondLegal) {
            return second;
        }
        return null;
    }

    private boolean selectable(CloneRuntimeState state, ServerPlayer owner, FamilyKey family) {
        if (!CloneBodySweep.movableOrdinary(state) || state.spec.budget() != CloneSpec.ResponseBudget.REPEAT) {
            return false;
        }
        if (state.response != CloneResponseState.READY || state.pending != null || state.cooperationHold != null
                || state.budgetSpent || state.executing || state.commitLine) {
            return false;
        }
        if (!state.ownerId.equals(owner.getUUID()) || !state.dimension.equals(family.dimension())) {
            return false;
        }
        CloneEntity actor = state.actor;
        if (actor.level() != owner.level() || !(actor.level() instanceof ServerLevel level)) {
            return false;
        }
        BlockPos feet = actor.blockPosition();
        BlockPos crown = BlockPos.containing(actor.getX(), actor.getEyeY(), actor.getZ());
        return level.hasChunkAt(feet) && level.hasChunkAt(crown);
    }

    private boolean sameSession(@Nullable MinecraftServer server, @Nullable UUID serverSession) {
        if (server == null || serverSession == null || !server.isSameThread()) {
            return false;
        }
        UUID current = this.sessions.sessionOf(server);
        if (current == null || !current.equals(serverSession)) {
            return false;
        }
        CloneRuntime runtime = CloneRuntime.get(server);
        return runtime != null && this.sessions.cloneSessionToken(current) == runtime.sessionToken();
    }

    private static CooperationReservation published(CloneRuntime runtime, CloneRuntimeState state,
                                                    CloneCooperationHold hold) {
        return new CooperationReservation(hold.token, hold.bodyGeneration, hold.family,
                state.handle(runtime.sessionToken()), hold.expiryTick, true);
    }

    private static CooperationReservation absent(FamilyKey family, long expiryTick) {
        long expiry = expiryTick < 0L ? 0L : expiryTick;
        return new CooperationReservation(UUID.randomUUID(), 0L, family, null, expiry, false);
    }

    private static void faceShared(@Nullable CloneEntity actor, Vec3 aim) {
        if (actor == null || actor.isRemoved()) {
            return;
        }
        actor.face(aim);
        actor.setDeltaMovement(Vec3.ZERO);
    }

    private static ActorFrame cloneFrame(CloneEntity actor, Vec3 aim) {
        float visualYaw = (float) (Math.atan2(-aim.x, aim.z) * (180.0D / Math.PI));
        return new ActorFrame(actor.getUUID(), CooperativeStructureLimits.COOPERATING_ACTOR_ORDINAL,
                actor.position(), actor.getEyePosition(), aim, visualYaw, true);
    }

    private static Vec3 horizontalForward(ActorFrame frame) {
        Vec3 aim = frame.aim();
        double horizontal = Math.sqrt(aim.x * aim.x + aim.z * aim.z);
        if (horizontal < VERTICAL_AIM) {
            double radians = Math.toRadians(frame.visualYaw());
            return new Vec3(-Math.sin(radians), 0.0D, Math.cos(radians));
        }
        return new Vec3(aim.x / horizontal, 0.0D, aim.z / horizontal);
    }

    private static Vec3 horizontalRight(Vec3 forward) {
        return new Vec3(-forward.z, 0.0D, forward.x);
    }

    private static Vec3 stanceFeet(Vec3 ownerFeet, Vec3 forward, boolean rightSide, double sideOffset) {
        Vec3 right = horizontalRight(forward);
        double lateral = rightSide ? sideOffset : -sideOffset;
        return ownerFeet.add(forward.scale(CooperativeStructureLimits.STANCE_FORWARD_BLOCKS))
                .add(right.scale(lateral));
    }

    private record BodyChoice(CloneRuntimeState state, @Nullable AcceptedReposition plan, boolean rightSide,
                              double distanceSquared, boolean remotePlaced) {
    }

    private record RankedBody(CloneRuntimeState state, double distanceSquared) {
    }

    private record StanceOption(Vec3 feet, double travel, double distanceSquared, boolean rightSide, RecallKind kind) {
    }

    private enum StanceMark {
        LEGAL,
        ILLEGAL,
        ABORT
    }

    private static final class StanceSearch {
        private final BodyChoice choice;
        private final boolean halt;

        private StanceSearch(@Nullable BodyChoice choice, boolean halt) {
            this.choice = choice;
            this.halt = halt;
        }

        private static StanceSearch none() {
            return new StanceSearch(null, false);
        }

        private static StanceSearch halt() {
            return new StanceSearch(null, true);
        }

        private static StanceSearch found(BodyChoice choice) {
            return new StanceSearch(choice, false);
        }
    }

    private static final class EndpointLedger {
        private final LinkedHashSet<Long> cells = new LinkedHashSet<>();
        private boolean sourceKnown;
        private boolean sourceSafe = true;

        private void beginBody() {
            this.sourceKnown = false;
            this.sourceSafe = true;
        }

        private boolean accountSource(ServerLevel level, CloneRuntime runtime, CloneEntity actor) {
            if (this.sourceKnown) {
                return true;
            }
            this.sourceKnown = true;
            int added = CloneBodySweep.absorbFootprint(actor, actor.position(), this.cells,
                    CooperativeStructureLimits.REMOTE_ENDPOINT_CELL_CAP);
            if (added < 0) {
                return false;
            }
            if (added > 0) {
                Optional<RemoteSpaceGrant> grant = RemoteRecallMeter.take(runtime.tick(), RemoteCheckPhase.ENDPOINT,
                        added, 0, 0, 0);
                if (grant.isEmpty()) {
                    return false;
                }
            }
            this.sourceSafe = CloneBodySweep.endpointProblem(level, actor, actor.position()) == null;
            return true;
        }
    }

    private record Resolved(CloneRuntime runtime, CloneRuntimeState state, CloneCooperationHold hold,
                            ServerLevel level) {
    }
}
