package com.bslx.XueXiMod.shu.technique.release.domain.motion;

import com.bslx.XueXiMod.network.DomainSyncPacket;
import com.bslx.XueXiMod.player.PlayerShuData;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.release.deflection.projectile.ProjectileStandardCost;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainInstance;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainService;
import com.bslx.XueXiMod.shu.technique.release.domain.AxisBoxGeometry;
import com.bslx.XueXiMod.shu.technique.release.momentum.WaterMomentum;
import com.bslx.XueXiMod.status.WetnessTiers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 旋流牵引、气泡上升和挂载。持续流场只发布位移意图，抛出仍是一次冲量。 */
public final class VolumeMotionRuntime {
    private static final double GAP = 0.1D;
    private static final double LEAST_EDGE = 0.25D;
    private static final double GOLDEN = 2.399963229728653D;

    private VolumeMotionRuntime() {
    }

    public static void tick(MinecraftServer server) {
        if (server == null) {
            return;
        }
        List<DomainInstance> snapshot = new ArrayList<>(DomainService.active());
        matchMounts(snapshot);
        for (DomainInstance domain : snapshot) {
            if (domain.active() && domain.definition().motionKind() == VolumeMotionKind.VORTEX_HOST) {
                refreshColumn(domain);
            }
        }
        ArrayList<DomainInstance> hosts = new ArrayList<>();
        for (DomainInstance domain : DomainService.active()) {
            if (domain.active() && domain.definition().motionKind() == VolumeMotionKind.VORTEX_HOST) {
                hosts.add(domain);
            }
        }
        hosts.sort(Comparator.comparing(DomainInstance::instanceId));
        for (DomainInstance host : hosts) {
            if (host.active()) {
                processHost(server, host);
            }
        }
        for (DomainInstance domain : DomainService.active()) {
            if (domain.active() && domain.definition().motionKind() == VolumeMotionKind.UPDRAFT
                    && domain.mountedHost() == null && !domain.mountRetired()) {
                liftBubble(domain);
            }
        }
    }

    public static void onClose(DomainInstance domain) {
        if (domain == null || domain.definition() == null) {
            return;
        }
        if (domain.definition().motionKind() == VolumeMotionKind.VORTEX_HOST) {
            FlowMotionIntents.retireInstance(domain);
            UUID providerId = domain.mountedProvider();
            domain.clearMount();
            ReleaseMotionLedger ledger = ReleaseMotionLedger.find(domain);
            if (ledger != null) {
                ServerLevel level = domain.hostLevel();
                for (UUID rootId : ledger.roots(domain.instanceId())) {
                    Entity entity = level == null ? null : level.getEntity(rootId);
                    if (entity == null) {
                        FlowMotionIntents.retireRoot(domain.instanceId(), rootId);
                    }
                }
                ledger.releaseInstance(domain.instanceId());
            }
            ReleaseMotionLedger.dropIfIdle(domain);
            if (providerId != null) {
                DomainInstance provider = DomainService.find(providerId);
                if (provider != null && provider.active()) {
                    provider.retireMount();
                    DomainService.closeInstance(provider);
                }
            }
            return;
        }
        if (domain.definition().motionKind() == VolumeMotionKind.UPDRAFT) {
            FlowMotionIntents.retireInstance(domain);
            UUID hostId = domain.mountedHost();
            domain.clearMount();
            if (hostId != null) {
                DomainInstance host = DomainService.find(hostId);
                if (host != null) {
                    host.clearProvider();
                }
            }
        }
    }

    public static boolean hasEffectiveUpdraft(Entity entity) {
        if (entity == null || entity.level().isClientSide() || !entity.isAlive()) {
            return false;
        }
        return FlowMotionIntents.hasEffectiveRise(entity);
    }

    public static double riseIntent(Entity entity) {
        if (entity == null || entity.level().isClientSide() || !entity.isAlive()) {
            return 0.0D;
        }
        return FlowMotionIntents.riseFloor(entity);
    }

    public static MotionFrame frame(DomainInstance domain) {
        if (domain == null || domain.definition() == null) {
            return MotionFrame.NONE;
        }
        VolumeMotionKind kind = domain.definition().motionKind();
        if (kind == VolumeMotionKind.NONE) {
            return MotionFrame.NONE;
        }
        String balanceId = domain.definition().balanceId();
        double rise = 0.0D;
        double attraction = 0.0D;
        double pull = 0.0D;
        double orbit = 0.0D;
        double travel = 0.0D;
        if (kind == VolumeMotionKind.UPDRAFT) {
            rise = riseSpeed(domain);
        } else {
            attraction = SpecialBalance.number(balanceId, BalanceKeys.ATTRACTION_RADIUS, 8.0D);
            pull = SpecialBalance.number(balanceId, BalanceKeys.PULL_SPEED, 0.35D);
            orbit = SpecialBalance.number(balanceId, BalanceKeys.ORBIT_ANGULAR_SPEED, 0.10D);
            travel = SpecialBalance.number(balanceId, BalanceKeys.MOTION_SPEED, 0.45D);
            if (domain.liftedColumn()) {
                DomainInstance provider = DomainService.find(domain.mountedProvider());
                if (provider != null) {
                    rise = riseSpeed(provider);
                }
            }
        }
        return new MotionFrame(kind.ordinal(), domain.liftedColumn(), attraction, pull, orbit, rise, travel,
                kind == VolumeMotionKind.VORTEX_HOST ? cylinderRadius(domain) : 0.0D);
    }

    private static void matchMounts(List<DomainInstance> snapshot) {
        for (DomainInstance provider : snapshot) {
            if (!provider.active() || provider.mountRetired() || provider.mountedHost() != null
                    || provider.definition().motionKind() != VolumeMotionKind.UPDRAFT) {
                continue;
            }
            DomainInstance chosen = null;
            double chosenDistance = Double.MAX_VALUE;
            for (DomainInstance host : snapshot) {
                if (!host.active() || host.mountedProvider() != null
                        || host.definition().motionKind() != VolumeMotionKind.VORTEX_HOST) {
                    continue;
                }
                if (host.ownerId() == null || !host.ownerId().equals(provider.ownerId())) {
                    continue;
                }
                if (!canMount(host, provider)) {
                    continue;
                }
                double distance = center(host).distanceTo(center(provider));
                if (chosen == null || distance < chosenDistance
                        || (distance == chosenDistance && host.instanceId().compareTo(chosen.instanceId()) < 0)) {
                    chosen = host;
                    chosenDistance = distance;
                }
            }
            if (chosen != null) {
                chosen.setMountedProvider(provider.instanceId());
                provider.setMountedHost(chosen.instanceId());
                DomainSyncPacket.broadcast(provider, false);
                DomainSyncPacket.broadcast(chosen, false);
            }
        }
    }

    private static void refreshColumn(DomainInstance host) {
        String balanceId = host.definition().balanceId();
        double dry = SpecialBalance.number(balanceId, BalanceKeys.BOX_HEIGHT, 3.0D);
        double liftedHeight = SpecialBalance.number(balanceId, BalanceKeys.LIFTED_HEIGHT, 8.0D);
        AABB bounds = host.geometry().bounds();
        double current = bounds.maxY - bounds.minY;
        double scale = host.captureColumnScale(dry, current);
        boolean lift = combination(host);
        double target = (lift ? liftedHeight : dry) * scale;
        if (target < 0.5D) {
            target = 0.5D;
        }
        boolean changed = Math.abs(current - target) > 0.02D || lift != host.liftedColumn();
        if (!changed) {
            return;
        }
        host.adoptGeometry(new AxisBoxGeometry(new AABB(
                bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.minY + target, bounds.maxZ)));
        host.setLiftedColumn(lift);
        DomainSyncPacket.broadcast(host, false);
    }

    private static void processHost(MinecraftServer server, DomainInstance host) {
        ReleaseMotionLedger ledger = ReleaseMotionLedger.find(host);
        ServerPlayer owner = server.getPlayerList().getPlayer(host.ownerId());
        if (ledger == null || owner == null || host.hostLevel() == null) {
            return;
        }
        if (!(PlayerShuData.get(owner).getWaterMomentum() > 0F)) {
            DomainService.closeInstance(host);
            return;
        }
        releaseMissing(host, ledger);
        AABB column = host.geometry().bounds();
        double axisX = (column.minX + column.maxX) * 0.5D;
        double axisZ = (column.minZ + column.maxZ) * 0.5D;
        double bodyRadius = cylinderRadius(host);
        double attraction = SpecialBalance.number(host.definition().balanceId(), BalanceKeys.ATTRACTION_RADIUS, 8.0D);
        AABB query = new AABB(axisX - attraction, column.minY, axisZ - attraction,
                axisX + attraction, column.maxY, axisZ + attraction);
        List<Entity> found = host.hostLevel().getEntities((Entity) null, query, VolumeMotionRuntime::selectable);
        for (Entity candidate : found) {
            if (!host.active()) {
                return;
            }
            if (candidate.isPassenger()) {
                continue;
            }
            if (candidate instanceof Projectile projectile) {
                if (deflectProjectile(host, ledger, owner, projectile, axisX, axisZ, bodyRadius)) {
                    return;
                }
                continue;
            }
            if (steerGroup(host, ledger, owner, candidate, axisX, axisZ, bodyRadius, attraction)) {
                return;
            }
        }
        publishMembers(host, ledger);
    }

    private static void publishMembers(DomainInstance host, ReleaseMotionLedger ledger) {
        if (!host.active()) {
            return;
        }
        if (host.replacePublishedRoots(ledger.roots(host.instanceId()))) {
            DomainSyncPacket.broadcast(host, false);
        }
    }

    private static boolean deflectProjectile(DomainInstance host, ReleaseMotionLedger ledger, ServerPlayer owner,
                                             Projectile projectile, double axisX, double axisZ, double bodyRadius) {
        if (projectile.noPhysics || ledger.deflected(projectile.getUUID())) {
            return false;
        }
        AABB column = host.geometry().bounds();
        if (!touchesCylinder(projectile.getBoundingBox(), axisX, axisZ, bodyRadius, column)) {
            return false;
        }
        if (!clearLine(projectile, axisX, axisZ)) {
            return false;
        }
        if (!ProjectileStandardCost.canTurn(projectile)) {
            return false;
        }
        float fallback = (float) SpecialBalance.number(host.definition().balanceId(),
                BalanceKeys.FALLBACK_PROJECTILE_STANDARD_COST, 5.0D);
        float price = ProjectileStandardCost.deflectPrice(projectile, fallback);
        if (!(price > 0F)) {
            return false;
        }
        WaterMomentum.Settlement settlement = WaterMomentum.settle(owner, price, 0F);
        if (!settlement.paid()) {
            DomainService.closeInstance(host);
            return true;
        }
        if (!ProjectileStandardCost.deflect(projectile)) {
            WaterMomentum.settle(owner, 0F, price);
            return false;
        }
        ledger.markDeflected(projectile.getUUID());
        if (!(settlement.after() > 0F)) {
            DomainService.closeInstance(host);
            return true;
        }
        return false;
    }

    /** 返回真表示这个旋流已经关闭。 */
    private static boolean steerGroup(DomainInstance host, ReleaseMotionLedger ledger, ServerPlayer owner, Entity root,
                                      double axisX, double axisZ, double bodyRadius, double attraction) {
        ArrayList<Entity> members = new ArrayList<>();
        collectRiders(root, members);
        if (groupCarries(members, owner)) {
            releaseCasterGroup(host, ledger, root, members);
            return false;
        }
        for (Entity member : members) {
            if (ledger.ejected(member.getUUID())) {
                return false;
            }
            if (ledger.controlledByOther(member.getUUID(), host.instanceId())) {
                return false;
            }
        }
        AABB union = root.getBoundingBox();
        for (Entity member : members) {
            union = union.minmax(member.getBoundingBox());
        }
        AABB column = host.geometry().bounds();
        boolean inside = bodyInside(union, axisX, axisZ, bodyRadius, column);
        boolean touching = touchesCylinder(union, axisX, axisZ, bodyRadius, column);
        boolean near = horizontalDistance(union.getCenter(), axisX, axisZ) <= attraction
                && overlaps(column, union);
        boolean atCeiling = host.liftedColumn() && horizontalInside(union, axisX, axisZ, bodyRadius)
                && union.maxY >= column.maxY - 0.2D && union.minY < column.maxY;
        boolean held = groupHeld(ledger, host, members);
        if (!inside && !touching && !near && !atCeiling) {
            FlowMotionIntents.retireRoot(host.instanceId(), root.getUUID());
            for (Entity member : members) {
                ledger.releaseControl(member.getUUID());
            }
            return false;
        }
        if (!clearLine(root, axisX, axisZ)) {
            if (held && inside && !blocksClear(root, union)) {
                DomainService.closeInstance(host);
                return true;
            }
            return false;
        }
        if (touching && !fits(union, bodyRadius, column)) {
            DomainService.closeInstance(host);
            return true;
        }
        boolean exitOpen = atCeiling && blocksClear(root, union.move(0.0D, 0.6D, 0.0D));
        if (inside) {
            if (!acquire(host, ledger, owner, members, union, bodyRadius)) {
                DomainService.closeInstance(host);
                return true;
            }
            if (exitOpen) {
                ejectGroup(host, ledger, root, members, union, axisX, axisZ);
                return false;
            }
            orbit(host, root, axisX, axisZ, bodyRadius, union, host.liftedColumn());
            return false;
        }
        if (atCeiling && held && exitOpen) {
            ejectGroup(host, ledger, root, members, union, axisX, axisZ);
            return false;
        }
        if (atCeiling) {
            return false;
        }
        if (near) {
            pull(host, root, axisX, axisZ, attraction);
        }
        return false;
    }

    private static boolean acquire(DomainInstance host, ReleaseMotionLedger ledger, ServerPlayer owner,
                                   List<Entity> members, AABB union, double bodyRadius) {
        AABB column = host.geometry().bounds();
        if (!fits(union, bodyRadius, column)) {
            return false;
        }
        double incoming = 0.0D;
        float charge = 0F;
        float refund = 0F;
        ArrayList<Entity> newcomers = new ArrayList<>();
        for (Entity member : members) {
            if (ledger.controls(host.instanceId(), member.getUUID())) {
                continue;
            }
            incoming += volumeOf(member.getBoundingBox());
            if (ledger.settled(member.getUUID())) {
                continue;
            }
            newcomers.add(member);
            if (member instanceof ItemEntity || member instanceof Projectile) {
                continue;
            }
            float[] priced = priceOf(host, member, combination(host));
            charge += priced[0];
            refund += priced[1];
        }
        double limit = SpecialBalance.number(host.definition().balanceId(), BalanceKeys.OCCUPIED_VOLUME_LIMIT, 12.0D);
        if (heldVolume(ledger, host, host.hostLevel()) + incoming > limit) {
            return false;
        }
        double axisX = (column.minX + column.maxX) * 0.5D;
        double axisZ = (column.minZ + column.maxZ) * 0.5D;
        Seat seat = findSeat(host, rootOf(members), union, axisX, axisZ, bodyRadius, host.liftedColumn());
        if (seat == null) {
            return false;
        }
        if (!newcomers.isEmpty() && (charge > 0F || refund > 0F)) {
            if (!Float.isFinite(charge) || !Float.isFinite(refund) || charge < 0F) {
                return false;
            }
            WaterMomentum.Settlement settlement = WaterMomentum.settle(owner, charge, refund);
            if (!settlement.paid()) {
                return false;
            }
            if (!(settlement.after() > 0F)) {
                for (Entity member : newcomers) {
                    ledger.markSettled(member.getUUID());
                }
                return false;
            }
        }
        for (Entity member : newcomers) {
            ledger.markSettled(member.getUUID());
        }
        for (Entity member : members) {
            ledger.claim(host.instanceId(), member.getUUID());
        }
        Entity seatedRoot = rootOf(members);
        if (seatedRoot != null) {
            ledger.claimRoot(host.instanceId(), seatedRoot.getUUID());
            ledger.assignSeat(seatedRoot.getUUID(), seat.angle, seat.ring);
        }
        return true;
    }

    private static void ejectGroup(DomainInstance host, ReleaseMotionLedger ledger, Entity root, List<Entity> members,
                                  AABB union, double axisX, double axisZ) {
        Vec3 away = awayFrom(union, axisX, axisZ);
        double horizontal = SpecialBalance.number(host.definition().balanceId(), BalanceKeys.EJECT_HORIZONTAL_SPEED, 0.45D);
        double vertical = SpecialBalance.number(host.definition().balanceId(), BalanceKeys.EJECT_VERTICAL_SPEED, 0.20D);
        FlowMotionIntents.retireRoot(host.instanceId(), root.getUUID());
        root.setDeltaMovement(away.scale(horizontal).add(0.0D, vertical, 0.0D));
        root.hasImpulse = true;
        for (Entity member : members) {
            ledger.markEjected(member.getUUID());
        }
    }

    private static void orbit(DomainInstance host, Entity root, double axisX, double axisZ, double bodyRadius,
                              AABB union, boolean lifted) {
        Seat seat = findSeat(host, root, union, axisX, axisZ, bodyRadius, lifted);
        if (seat == null) {
            return;
        }
        ReleaseMotionLedger ledger = ReleaseMotionLedger.find(host);
        if (ledger != null) {
            ledger.assignSeat(root.getUUID(), seat.angle, seat.ring);
        }
        String balanceId = host.definition().balanceId();
        double motionCap = SpecialBalance.number(balanceId, BalanceKeys.MOTION_SPEED, 0.45D);
        double angular = SpecialBalance.number(balanceId, BalanceKeys.ORBIT_ANGULAR_SPEED, 0.10D);
        Vec3 center = union.getCenter();
        double offsetX = center.x - axisX;
        double offsetZ = center.z - axisZ;
        double distance = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);
        double radialX = distance < 1.0E-4D ? 1.0D : offsetX / distance;
        double radialZ = distance < 1.0E-4D ? 0.0D : offsetZ / distance;
        double currentAngle = Math.atan2(offsetZ, offsetX);
        double turn = wrapAngle(seat.angle - currentAngle);
        if (turn > angular) {
            turn = angular;
        }
        if (turn < -angular) {
            turn = -angular;
        }
        double tangentX = -radialZ;
        double tangentZ = radialX;
        double linear = turn * Math.max(distance, 0.4D);
        double radialGap = distance - seat.ring;
        double horizontalX = tangentX * linear - radialX * radialGap * 0.25D;
        double horizontalZ = tangentZ * linear - radialZ * radialGap * 0.25D;
        double horizontalLength = Math.sqrt(horizontalX * horizontalX + horizontalZ * horizontalZ);
        if (horizontalLength > motionCap && horizontalLength > 1.0E-6D) {
            horizontalX = horizontalX / horizontalLength * motionCap;
            horizontalZ = horizontalZ / horizontalLength * motionCap;
        }
        boolean specialBody = root instanceof Vex || root instanceof EnderDragon;
        boolean wroteVertical = lifted && !specialBody;
        double rise = 0.0D;
        if (wroteVertical) {
            DomainInstance provider = DomainService.find(host.mountedProvider());
            rise = provider == null ? 0.12D : riseSpeed(provider);
        }
        double nextY = wroteVertical ? rise : 0.0D;
        if (ledger != null) {
            FlowMotionIntents.offer(host, root, FlowRole.ORBIT, horizontalX, horizontalZ, wroteVertical, nextY);
        }
    }

    private static void pull(DomainInstance host, Entity root, double axisX, double axisZ, double attraction) {
        double pullSpeed = SpecialBalance.number(host.definition().balanceId(), BalanceKeys.PULL_SPEED, 0.35D);
        Vec3 center = root.getBoundingBox().getCenter();
        double offsetX = axisX - center.x;
        double offsetZ = axisZ - center.z;
        double distance = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);
        if (distance < 1.0E-4D) {
            return;
        }
        double closeness = 1.0D - distance / Math.max(attraction, 0.1D);
        if (closeness < 0.15D) {
            closeness = 0.15D;
        }
        double speed = pullSpeed * closeness;
        if (speed > pullSpeed) {
            speed = pullSpeed;
        }
        double preservedY = 0.0D;
        ReleaseMotionLedger ledger = ReleaseMotionLedger.find(host);
        if (ledger == null) {
            return;
        }
        FlowMotionIntents.offer(host, root, FlowRole.PULL, offsetX / distance * speed, offsetZ / distance * speed,
                false, preservedY);
    }

    private static void liftBubble(DomainInstance bubble) {
        AABB column = bubble.geometry().bounds();
        double rise = riseSpeed(bubble);
        HashSet<UUID> refreshed = new HashSet<>();
        ServerPlayer owner = null;
        if (bubble.hostLevel().getServer() != null) {
            owner = bubble.hostLevel().getServer().getPlayerList().getPlayer(bubble.ownerId());
        }
        List<Entity> found = bubble.hostLevel().getEntities((Entity) null, column, VolumeMotionRuntime::selectable);
        for (Entity candidate : found) {
            if (candidate.isPassenger() || candidate instanceof Projectile || candidate instanceof Vex
                    || candidate instanceof EnderDragon) {
                continue;
            }
            if (carriesCaster(candidate, owner)) {
                FlowMotionIntents.retireRoot(bubble.instanceId(), candidate.getUUID());
                continue;
            }
            if (!columnTouch(bubble, candidate.getBoundingBox()) || !inWater(candidate) || vortexHolds(candidate)) {
                continue;
            }
            refreshed.add(candidate.getUUID());
            FlowMotionIntents.offer(bubble, candidate, FlowRole.BUBBLE, 0.0D, 0.0D, true, rise);
        }
        FlowMotionIntents.retain(bubble, refreshed);
    }

    private static boolean vortexHolds(Entity entity) {
        for (DomainInstance domain : DomainService.active()) {
            if (domain.definition().motionKind() != VolumeMotionKind.VORTEX_HOST) {
                continue;
            }
            ReleaseMotionLedger ledger = ReleaseMotionLedger.find(domain);
            if (ledger != null && ledger.controls(domain.instanceId(), entity.getUUID())) {
                return true;
            }
        }
        return false;
    }

    private static void releaseMissing(DomainInstance host, ReleaseMotionLedger ledger) {
        for (UUID entityId : ledger.heldBy(host.instanceId())) {
            Entity entity = host.hostLevel().getEntity(entityId);
            if (entity == null || !entity.isAlive() || entity.level() != host.hostLevel() || entity.isSpectator()) {
                FlowMotionIntents.retireRoot(host.instanceId(), entityId);
                ledger.releaseControl(entityId);
            }
        }
    }

    private static boolean combination(DomainInstance host) {
        if (host.mountedProvider() == null) {
            return false;
        }
        DomainInstance provider = DomainService.find(host.mountedProvider());
        return provider != null && provider.active() && !provider.mountRetired() && baseWater(host);
    }

    private static boolean baseWater(DomainInstance domain) {
        ServerLevel level = domain.hostLevel();
        AABB bounds = domain.geometry().bounds();
        double axisX = (bounds.minX + bounds.maxX) * 0.5D;
        double axisZ = (bounds.minZ + bounds.maxZ) * 0.5D;
        double radius = cylinderRadius(domain);
        int sampleY = (int) Math.floor(bounds.minY);
        int firstX = (int) Math.floor(axisX - radius);
        int lastX = (int) Math.ceil(axisX + radius);
        int firstZ = (int) Math.floor(axisZ - radius);
        int lastZ = (int) Math.ceil(axisZ + radius);
        for (int blockX = firstX; blockX <= lastX; blockX++) {
            for (int blockZ = firstZ; blockZ <= lastZ; blockZ++) {
                double gapX = blockX + 0.5D - axisX;
                double gapZ = blockZ + 0.5D - axisZ;
                if (gapX * gapX + gapZ * gapZ > radius * radius) {
                    continue;
                }
                if (waterCell(level, new BlockPos(blockX, sampleY, blockZ))
                        || waterCell(level, new BlockPos(blockX, sampleY - 1, blockZ))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean waterCell(ServerLevel level, BlockPos cell) {
        return level.isLoaded(cell) && level.getFluidState(cell).is(FluidTags.WATER);
    }

    private static boolean inWater(Entity entity) {
        return entity.isInWater() || entity.level().getFluidState(entity.blockPosition()).is(FluidTags.WATER);
    }

    private static float[] priceOf(DomainInstance host, Entity member, boolean tornado) {
        String balanceId = host.definition().balanceId();
        float entry = (float) SpecialBalance.number(balanceId, BalanceKeys.ENTRY_COST, 10.0D);
        float charge = entry;
        float refund = 0F;
        if (member instanceof LivingEntity living && tornado) {
            int tier = WetnessTiers.of(living).tier();
            if (tier == 1) {
                charge = (float) SpecialBalance.number(balanceId, BalanceKeys.WET_TIER_1_ENTRY_COST, 8.0D);
            } else if (tier == 2) {
                charge = (float) SpecialBalance.number(balanceId, BalanceKeys.WET_TIER_2_ENTRY_COST, 5.0D);
            } else if (tier == 3) {
                refund = (float) SpecialBalance.number(balanceId, BalanceKeys.WET_TIER_3_REFUND, 13.0D);
            } else if (tier >= 4) {
                refund = (float) SpecialBalance.number(balanceId, BalanceKeys.WET_TIER_4_REFUND, 16.0D);
            }
        }
        return new float[] {charge, refund};
    }

    private static double cylinderRadius(DomainInstance domain) {
        if (domain.definition().motionKind() == VolumeMotionKind.UPDRAFT) {
            return SpecialBalance.number(domain.definition().balanceId(), BalanceKeys.RADIUS, 1.25D);
        }
        return SpecialBalance.number(domain.definition().balanceId(), BalanceKeys.BODY_RADIUS, 2.5D);
    }

    private static double riseSpeed(DomainInstance domain) {
        return SpecialBalance.number(domain.definition().balanceId(), BalanceKeys.RISE_SPEED, 0.12D);
    }

    private static double volumeOf(AABB box) {
        double spanX = Math.max(LEAST_EDGE, box.getXsize()) + GAP;
        double spanY = Math.max(LEAST_EDGE, box.getYsize()) + GAP;
        double spanZ = Math.max(LEAST_EDGE, box.getZsize()) + GAP;
        return spanX * spanY * spanZ;
    }

    private static boolean fits(AABB box, double bodyRadius, AABB column) {
        double spanX = Math.max(LEAST_EDGE, box.getXsize()) + GAP;
        double spanY = Math.max(LEAST_EDGE, box.getYsize()) + GAP;
        double spanZ = Math.max(LEAST_EDGE, box.getZsize()) + GAP;
        return spanX <= bodyRadius * 2.0D && spanZ <= bodyRadius * 2.0D && spanY <= column.maxY - column.minY;
    }

    private static double heldVolume(ReleaseMotionLedger ledger, DomainInstance host, ServerLevel level) {
        double total = 0.0D;
        for (UUID entityId : ledger.heldBy(host.instanceId())) {
            Entity entity = level.getEntity(entityId);
            if (entity != null) {
                total += volumeOf(entity.getBoundingBox());
            }
        }
        return total;
    }

    private static double priorityRadius(DomainInstance host, AABB union, double bodyRadius) {
        AABB column = host.geometry().bounds();
        double contour = FunnelOutline.radiusAt(bodyRadius, column.minY, column.maxY - column.minY, union.minY);
        return contour - halfDiagonal(union);
    }

    private static double halfDiagonal(AABB box) {
        double spanX = Math.max(LEAST_EDGE, box.getXsize()) + GAP;
        double spanZ = Math.max(LEAST_EDGE, box.getZsize()) + GAP;
        return Math.sqrt(spanX * spanX + spanZ * spanZ) * 0.5D;
    }

    private static double seatedAngle(DomainInstance host, Entity root, AABB union, double axisX, double axisZ,
                                     double bodyRadius, double ringRadius) {
        ReleaseMotionLedger ledger = ReleaseMotionLedger.find(host);
        if (ledger != null) {
            double kept = ledger.seatAngle(root.getUUID());
            if (Double.isFinite(kept) && seatClear(host, root, union, axisX, axisZ, bodyRadius, ringRadius, kept)) {
                return kept;
            }
        }
        double currentAngle = Math.atan2(union.getCenter().z - axisZ, union.getCenter().x - axisX);
        double chosen = Double.NaN;
        double chosenGap = Double.MAX_VALUE;
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = (attempt + 1) * GOLDEN;
            if (!seatClear(host, root, union, axisX, axisZ, bodyRadius, ringRadius, angle)) {
                continue;
            }
            double separation = Math.abs(wrapAngle(angle - currentAngle));
            if (separation < chosenGap) {
                chosenGap = separation;
                chosen = angle;
            }
        }
        return chosen;
    }

    private static boolean seatClear(DomainInstance host, Entity root, AABB union, double axisX, double axisZ,
                                    double bodyRadius, double ringRadius, double angle) {
        Vec3 center = union.getCenter();
        AABB shifted = union.move(axisX + Math.cos(angle) * ringRadius - center.x, 0.0D,
                axisZ + Math.sin(angle) * ringRadius - center.z);
        if (!bodyInside(shifted, axisX, axisZ, bodyRadius, host.geometry().bounds())) {
            return false;
        }
        if (seatTaken(host, root, ringRadius, angle)) {
            return false;
        }
        return blocksClear(root, shifted) && !overlapsForeign(host, root, shifted);
    }

    private static boolean seatTaken(DomainInstance host, Entity root, double ringRadius, double angle) {
        ReleaseMotionLedger ledger = ReleaseMotionLedger.find(host);
        if (ledger == null) {
            return false;
        }
        for (UUID entityId : ledger.roots(host.instanceId())) {
            if (entityId.equals(root.getUUID())) {
                continue;
            }
            double otherAngle = ledger.seatAngle(entityId);
            double otherRing = ledger.seatRing(entityId);
            if (!Double.isFinite(otherAngle) || !Double.isFinite(otherRing)) {
                continue;
            }
            if (Math.abs(otherRing - ringRadius) > 0.05D) {
                continue;
            }
            if (Math.abs(wrapAngle(otherAngle - angle)) < 0.35D) {
                return true;
            }
        }
        return false;
    }

    private static boolean overlapsForeign(DomainInstance host, Entity root, AABB shifted) {
        ReleaseMotionLedger ledger = ReleaseMotionLedger.find(host);
        if (ledger == null || host.hostLevel() == null) {
            return false;
        }
        AABB padded = shifted.inflate(GAP);
        for (UUID entityId : ledger.heldBy(host.instanceId())) {
            if (entityId.equals(root.getUUID())) {
                continue;
            }
            Entity other = host.hostLevel().getEntity(entityId);
            if (other == null || other == root || carriedBy(other, root)) {
                continue;
            }
            if (padded.intersects(other.getBoundingBox())) {
                return true;
            }
        }
        return false;
    }

    private static double wrapAngle(double turn) {
        double spun = turn;
        while (spun > Math.PI) {
            spun -= Math.PI * 2.0D;
        }
        while (spun < -Math.PI) {
            spun += Math.PI * 2.0D;
        }
        return spun;
    }

    private static Seat findSeat(DomainInstance host, Entity root, AABB union, double axisX, double axisZ,
                                double bodyRadius, boolean lifted) {
        ArrayList<Double> cylinder = new ArrayList<>();
        double reach = halfDiagonal(union);
        boolean funnelOpen = false;
        double funnelRing = 0.0D;
        if (lifted) {
            double priority = priorityRadius(host, union, bodyRadius);
            if (priority >= 0.0D) {
                funnelOpen = true;
                funnelRing = priority;
            }
        }
        addCylinderRings(cylinder, bodyRadius - reach, reach);
        ReleaseMotionLedger ledger = ReleaseMotionLedger.find(host);
        double keptAngle = ledger == null ? Double.NaN : ledger.seatAngle(root.getUUID());
        double keptRing = ledger == null ? Double.NaN : ledger.seatRing(root.getUUID());
        if (funnelOpen) {
            double funnelAngle = seatedAngle(host, root, union, axisX, axisZ, bodyRadius, funnelRing);
            if (Double.isFinite(funnelAngle)) {
                return new Seat(funnelAngle, funnelRing);
            }
        }
        if (Double.isFinite(keptAngle) && Double.isFinite(keptRing)
                && seatClear(host, root, union, axisX, axisZ, bodyRadius, keptRing, keptAngle)) {
            return new Seat(keptAngle, keptRing);
        }
        for (int ordinal = 0; ordinal < cylinder.size(); ordinal++) {
            double ring = cylinder.get(ordinal);
            double angle = seatedAngle(host, root, union, axisX, axisZ, bodyRadius, ring);
            if (Double.isFinite(angle)) {
                return new Seat(angle, ring);
            }
        }
        return null;
    }

    private static void addCylinderRings(List<Double> rings, double outer, double reach) {
        if (outer < 0.0D) {
            return;
        }
        double step = Math.max(LEAST_EDGE, reach * 2.0D);
        double cursor = outer;
        int guard = 0;
        while (guard < 24) {
            rings.add(cursor);
            if (cursor <= 1.0E-4D) {
                return;
            }
            double next = cursor - step;
            if (next < 0.0D) {
                rings.add(0.0D);
                return;
            }
            cursor = next;
            guard++;
        }
    }

    private static boolean groupHeld(ReleaseMotionLedger ledger, DomainInstance host, List<Entity> members) {
        for (Entity member : members) {
            if (ledger.controls(host.instanceId(), member.getUUID())) {
                return true;
            }
        }
        return false;
    }

    private static boolean carriedBy(Entity other, Entity root) {
        Entity carrier = other.getVehicle();
        int guard = 0;
        while (carrier != null && guard < 8) {
            if (carrier == root) {
                return true;
            }
            carrier = carrier.getVehicle();
            guard++;
        }
        return false;
    }

    private static boolean blocksClear(Entity root, AABB box) {
        if (root == null || box == null) {
            return false;
        }
        for (VoxelShape shape : root.level().getBlockCollisions(root, box)) {
            if (!shape.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean horizontalInside(AABB box, double axisX, double axisZ, double radius) {
        double[] samplesX = new double[] {box.minX, box.maxX};
        double[] samplesZ = new double[] {box.minZ, box.maxZ};
        for (double sampleX : samplesX) {
            for (double sampleZ : samplesZ) {
                double gapX = sampleX - axisX;
                double gapZ = sampleZ - axisZ;
                if (gapX * gapX + gapZ * gapZ > radius * radius) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean columnTouch(DomainInstance domain, AABB box) {
        if (domain == null || domain.geometry() == null || box == null) {
            return false;
        }
        AABB column = domain.geometry().bounds();
        double axisX = (column.minX + column.maxX) * 0.5D;
        double axisZ = (column.minZ + column.maxZ) * 0.5D;
        return touchesCylinder(box, axisX, axisZ, cylinderRadius(domain), column);
    }

    private static boolean canMount(DomainInstance host, DomainInstance bubble) {
        if (host == null || bubble == null || bubble.hostLevel() == null || host.geometry() == null
                || bubble.geometry() == null) {
            return false;
        }
        AABB hostSlab = baseSlab(host);
        AABB bubbleSlab = baseSlab(bubble);
        if (hostSlab.maxY < bubbleSlab.minY || bubbleSlab.maxY < hostSlab.minY) {
            return false;
        }
        AABB hostBox = host.geometry().bounds();
        AABB bubbleBox = bubble.geometry().bounds();
        double hostX = (hostBox.minX + hostBox.maxX) * 0.5D;
        double hostZ = (hostBox.minZ + hostBox.maxZ) * 0.5D;
        double bubbleX = (bubbleBox.minX + bubbleBox.maxX) * 0.5D;
        double bubbleZ = (bubbleBox.minZ + bubbleBox.maxZ) * 0.5D;
        double hostRadius = cylinderRadius(host);
        double bubbleRadius = cylinderRadius(bubble);
        double gapX = hostX - bubbleX;
        double gapZ = hostZ - bubbleZ;
        double reach = hostRadius + bubbleRadius;
        if (gapX * gapX + gapZ * gapZ > reach * reach) {
            return false;
        }
        return waterShared(bubble.hostLevel(), bubbleBox.minY, hostX, hostZ, hostRadius, bubbleX, bubbleZ, bubbleRadius);
    }

    private static boolean waterShared(ServerLevel level, double bubbleBottom, double hostX, double hostZ,
                                      double hostRadius, double bubbleX, double bubbleZ, double bubbleRadius) {
        int sampleY = (int) Math.floor(bubbleBottom);
        int firstX = (int) Math.floor(bubbleX - bubbleRadius);
        int lastX = (int) Math.ceil(bubbleX + bubbleRadius);
        int firstZ = (int) Math.floor(bubbleZ - bubbleRadius);
        int lastZ = (int) Math.ceil(bubbleZ + bubbleRadius);
        for (int blockX = firstX; blockX <= lastX; blockX++) {
            for (int blockZ = firstZ; blockZ <= lastZ; blockZ++) {
                double bubbleGapX = blockX + 0.5D - bubbleX;
                double bubbleGapZ = blockZ + 0.5D - bubbleZ;
                if (bubbleGapX * bubbleGapX + bubbleGapZ * bubbleGapZ > bubbleRadius * bubbleRadius) {
                    continue;
                }
                double hostGapX = blockX + 0.5D - hostX;
                double hostGapZ = blockZ + 0.5D - hostZ;
                if (hostGapX * hostGapX + hostGapZ * hostGapZ > hostRadius * hostRadius) {
                    continue;
                }
                if (waterCell(level, new BlockPos(blockX, sampleY, blockZ))
                        || waterCell(level, new BlockPos(blockX, sampleY - 1, blockZ))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean flowStillHolds(FlowRole role, UUID instanceId, Entity root) {
        if (role == null || instanceId == null || root == null || !root.isAlive() || root.isRemoved()
                || root.isSpectator() || root.noPhysics || root.isPassenger() || root.level().getServer() == null) {
            return false;
        }
        DomainInstance domain = DomainService.find(instanceId);
        if (domain == null || !domain.active() || domain.hostLevel() != root.level() || domain.geometry() == null) {
            return false;
        }
        int nowTick = root.level().getServer().getTickCount();
        if (!DomainService.withinLife(nowTick, domain.expiresAt())) {
            return false;
        }
        ServerPlayer owner = root.level().getServer().getPlayerList().getPlayer(domain.ownerId());
        if (owner == null || !owner.isAlive() || owner.level() != domain.hostLevel()) {
            return false;
        }
        if (domain.definition().motionKind() == VolumeMotionKind.VORTEX_HOST
                && !(PlayerShuData.get(owner).getWaterMomentum() > 0F)) {
            return false;
        }
        if (role == FlowRole.BUBBLE) {
            return bubbleStillHolds(domain, root);
        }
        AABB column = domain.geometry().bounds();
        double axisX = (column.minX + column.maxX) * 0.5D;
        double axisZ = (column.minZ + column.maxZ) * 0.5D;
        double bodyRadius = cylinderRadius(domain);
        AABB body = root.getBoundingBox();
        if (!clearLine(root, axisX, axisZ)) {
            return false;
        }
        if (role == FlowRole.PULL) {
            if (bodyInside(body, axisX, axisZ, bodyRadius, column)) {
                return false;
            }
            double attraction = SpecialBalance.number(domain.definition().balanceId(), BalanceKeys.ATTRACTION_RADIUS, 8.0D);
            return horizontalDistance(body.getCenter(), axisX, axisZ) <= attraction && overlaps(column, body);
        }
        if (role == FlowRole.ORBIT) {
            return overlaps(column, body) && (horizontalInside(body, axisX, axisZ, bodyRadius)
                    || touchesCylinder(body, axisX, axisZ, bodyRadius, column));
        }
        return false;
    }

    private static boolean bubbleStillHolds(DomainInstance bubble, Entity root) {
        if (bubble.definition().motionKind() != VolumeMotionKind.UPDRAFT || bubble.mountedHost() != null
                || bubble.mountRetired()) {
            return false;
        }
        if (root instanceof Projectile || root instanceof Vex || root instanceof EnderDragon) {
            return false;
        }
        return columnTouch(bubble, root.getBoundingBox()) && inWater(root) && !vortexHolds(root);
    }

    private static Vec3 awayFrom(AABB union, double axisX, double axisZ) {
        Vec3 center = union.getCenter();
        double gapX = center.x - axisX;
        double gapZ = center.z - axisZ;
        double length = Math.sqrt(gapX * gapX + gapZ * gapZ);
        if (length < 1.0E-4D) {
            return new Vec3(1.0D, 0.0D, 0.0D);
        }
        return new Vec3(gapX / length, 0.0D, gapZ / length);
    }

    private static boolean clearLine(Entity entity, double axisX, double axisZ) {
        Vec3 from = entity.getBoundingBox().getCenter();
        Vec3 toward = new Vec3(axisX, from.y, axisZ);
        BlockHitResult trace = entity.level().clip(new ClipContext(
                from, toward, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        return trace.getType() != HitResult.Type.BLOCK || trace.getLocation().distanceToSqr(toward) < 0.09D;
    }

    private static boolean touchesCylinder(AABB box, double axisX, double axisZ, double radius, AABB column) {
        if (!overlaps(column, box)) {
            return false;
        }
        double closestX = bounded(axisX, box.minX, box.maxX);
        double closestZ = bounded(axisZ, box.minZ, box.maxZ);
        double gapX = closestX - axisX;
        double gapZ = closestZ - axisZ;
        return gapX * gapX + gapZ * gapZ <= radius * radius;
    }

    private static boolean bodyInside(AABB box, double axisX, double axisZ, double radius, AABB column) {
        if (box.minY < column.minY || box.maxY > column.maxY) {
            return false;
        }
        double[] samplesX = new double[] {box.minX, box.maxX};
        double[] samplesZ = new double[] {box.minZ, box.maxZ};
        for (double sampleX : samplesX) {
            for (double sampleZ : samplesZ) {
                double gapX = sampleX - axisX;
                double gapZ = sampleZ - axisZ;
                if (gapX * gapX + gapZ * gapZ > radius * radius) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean overlaps(AABB column, AABB box) {
        return box.maxY >= column.minY && box.minY <= column.maxY
                && box.maxX >= column.minX && box.minX <= column.maxX
                && box.maxZ >= column.minZ && box.minZ <= column.maxZ;
    }

    private static double horizontalDistance(Vec3 center, double axisX, double axisZ) {
        double gapX = center.x - axisX;
        double gapZ = center.z - axisZ;
        return Math.sqrt(gapX * gapX + gapZ * gapZ);
    }

    private static boolean carriesCaster(Entity root, ServerPlayer owner) {
        if (root == null || owner == null) {
            return false;
        }
        ArrayList<Entity> members = new ArrayList<>();
        collectRiders(root, members);
        return groupCarries(members, owner);
    }

    private static boolean groupCarries(List<Entity> members, ServerPlayer owner) {
        if (owner == null) {
            return false;
        }
        UUID casterId = owner.getUUID();
        for (Entity member : members) {
            if (casterId.equals(member.getUUID())) {
                return true;
            }
        }
        return false;
    }

    private static void releaseCasterGroup(DomainInstance host, ReleaseMotionLedger ledger, Entity root, List<Entity> members) {
        FlowMotionIntents.retireRoot(host.instanceId(), root.getUUID());
        for (Entity member : members) {
            ledger.releaseControl(member.getUUID());
            FlowMotionIntents.retireRoot(host.instanceId(), member.getUUID());
        }
    }

    private static void collectRiders(Entity root, List<Entity> members) {
        if (root == null || members.contains(root)) {
            return;
        }
        members.add(root);
        for (Entity rider : root.getPassengers()) {
            collectRiders(rider, members);
        }
    }

    private static Entity rootOf(List<Entity> members) {
        return members.get(0);
    }

    private static boolean selectable(Entity entity) {
        return entity != null && entity.isAlive() && !entity.isSpectator() && !entity.noPhysics
                && !(entity instanceof HangingEntity) && !(entity instanceof Display)
                && !(entity instanceof Interaction) && !(entity instanceof EnderDragonPart);
    }

    private static AABB baseSlab(DomainInstance domain) {
        AABB bounds = domain.geometry().bounds();
        return new AABB(bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, Math.min(bounds.maxY, bounds.minY + 1.0D), bounds.maxZ);
    }

    private static Vec3 center(DomainInstance domain) {
        return domain.geometry().bounds().getCenter();
    }

    private static double bounded(double amount, double lower, double upper) {
        if (amount < lower) {
            return lower;
        }
        if (amount > upper) {
            return upper;
        }
        return amount;
    }

    private static final class Seat {
        private final double angle;
        private final double ring;

        private Seat(double angle, double ring) {
            this.angle = angle;
            this.ring = ring;
        }
    }
}
