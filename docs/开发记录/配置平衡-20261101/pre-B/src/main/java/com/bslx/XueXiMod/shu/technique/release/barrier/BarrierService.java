package com.bslx.XueXiMod.shu.technique.release.barrier;

import com.bslx.XueXiMod.player.MagicPool;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.bind.PublicationAttempt;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactPolicy;
import com.bslx.XueXiMod.shu.technique.release.entity.ReleaseEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** 同一波次先预检再一起放墙。任何一面放不下，这一波不移动、不扣生成费。 */
public final class BarrierService {
    private static final ArrayList<BarrierBody> ACTIVE = new ArrayList<>();
    private static final java.util.HashSet<String> PLACED_THIS_TICK = new java.util.HashSet<>();
    private static int placedTick = Integer.MIN_VALUE;

    private BarrierService() {
    }

    public static boolean tryPlace(ServerPlayer player, BarrierDefinition definition, int copies, EnchantmentSnapshot snapshot) {
        return tryPlace(player, definition, copies, snapshot, ReleaseInput.BASE);
    }

    public static boolean tryPlace(ServerPlayer player, BarrierDefinition definition, int copies, EnchantmentSnapshot snapshot,
                                   ReleaseInput input) {
        return releasePlace(player, definition, copies, snapshot, null, null, false, null, input);
    }

    /** 生成费已由父会话预留。墙放在接触点，朝向用冻结方向。 */
    public static boolean tryPlaceReserved(ServerPlayer player, BarrierDefinition definition, int copies,
                                           EnchantmentSnapshot snapshot, Vec3 feet, Vec3 look, String claimSuffix) {
        return tryPlaceReserved(player, definition, copies, snapshot, feet, look, claimSuffix, ReleaseInput.BASE);
    }

    public static boolean tryPlaceReserved(ServerPlayer player, BarrierDefinition definition, int copies,
                                           EnchantmentSnapshot snapshot, Vec3 feet, Vec3 look, String claimSuffix,
                                           ReleaseInput input) {
        return releasePlace(player, definition, copies, snapshot, feet, look, true, claimSuffix, input);
    }

    private static boolean releasePlace(ServerPlayer player, BarrierDefinition definition, int copies,
                                        EnchantmentSnapshot snapshot, Vec3 feet, Vec3 look, boolean reserved,
                                        String claimSuffix, ReleaseInput input) {
        if (player == null || definition == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        int count = Math.max(1, copies);
        int nowTick = level.getServer().getTickCount();
        if (placedTick != nowTick) {
            PLACED_THIS_TICK.clear();
            placedTick = nowTick;
        }
        String claim = player.getUUID() + ":" + definition.width() + ":" + definition.forward();
        if (claimSuffix != null) {
            claim = claim + ":" + claimSuffix;
        }
        if (PLACED_THIS_TICK.contains(claim)) {
            return true;
        }
        ReleaseInput frozen = input == null ? ReleaseInput.BASE : input;
        BarrierDefinition derived = definition.withInput(frozen);
        AxisAlignedWall wall = feet == null || look == null
                ? placeBox(player.position(), player.getLookAngle(), player.getYRot(), derived)
                : placeBox(feet, look, player.getYRot(), derived);
        List<Shift> shifts = new ArrayList<>();
        if (!planShifts(level, wall, player, shifts)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c水墙里没有安全的让出位置，这次没有释放"));
            return false;
        }
        int bill = definition.spawnCost() * count;
        if (!reserved && !MagicPool.trySpend(player, bill)) {
            return false;
        }
        if (!reserved && bill > 0) {
            MagicPool.showBalance(player);
        }
        int now = level.getServer().getTickCount();
        int scaledLife = derived.lifetimeTicks();
        int lifeEnd = scaledLife > 0 ? ReleaseInput.expiryTick(now, scaledLife) : now + scaledLife;
        List<BarrierBody> spawned = new ArrayList<>();
        try {
            for (int copy = 0; copy < count; copy++) {
                BarrierBody body = ReleaseEntities.BARRIER.get().create(level);
                if (body == null) {
                    rollback(spawned);
                    if (!reserved) {
                        MagicPool.restore(player, bill);
                    }
                    return false;
                }
                body.configure(player.getUUID(), wall, lifeEnd,
                        now + definition.upkeepPeriod(), definition.upkeepCost(), snapshot, derived.deflectsProjectiles());
                if (!level.addFreshEntity(body)) {
                    rollback(spawned);
                    if (!reserved) {
                        MagicPool.restore(player, bill);
                    }
                    return false;
                }
                spawned.add(body);
                ACTIVE.add(body);
                body.openDeflection();
            }
        } catch (RuntimeException failure) {
            if (reserved) {
                notePlacedCopies(spawned.size());
            }
            throw failure;
        }
        if (reserved) {
            notePlacedCopies(spawned.size());
        }
        for (Shift shift : shifts) {
            Vec3 delta = shift.destination.subtract(shift.root.position());
            shift.root.setPos(shift.destination.x, shift.destination.y, shift.destination.z);
            nudgePassengers(shift.root, delta);
        }
        PLACED_THIS_TICK.add(claim);
        return true;
    }

    private static void notePlacedCopies(int placedCount) {
        for (int placedOrdinal = 0; placedOrdinal < placedCount; placedOrdinal++) {
            PublicationAttempt.reportPlacedCopy(placedOrdinal);
        }
    }

    public static void closeOwned(ServerPlayer player) {
        if (player == null) {
            return;
        }
        for (int index = ACTIVE.size() - 1; index >= 0; index--) {
            BarrierBody body = ACTIVE.get(index);
            if (player.getUUID().equals(body.ownerId())) {
                body.discard();
            }
        }
    }

    public static void forget(BarrierBody body) {
        ACTIVE.remove(body);
    }

    public static List<BarrierBody> active() {
        return ACTIVE;
    }

    public static AxisAlignedWall place(ServerPlayer player, BarrierDefinition definition) {
        return place(player, definition, ReleaseInput.BASE);
    }

    public static AxisAlignedWall place(ServerPlayer player, BarrierDefinition definition, ReleaseInput input) {
        return placeAt(player.position(), player.getLookAngle(), player.getYRot(), definition, input);
    }

    public static AxisAlignedWall placeAt(Vec3 feet, Vec3 look, float bodyYaw, BarrierDefinition definition) {
        return placeAt(feet, look, bodyYaw, definition, ReleaseInput.BASE);
    }

    public static AxisAlignedWall placeAt(Vec3 feet, Vec3 look, float bodyYaw, BarrierDefinition definition, ReleaseInput input) {
        return placeBox(feet, look, bodyYaw, definition.withInput(input));
    }

    private static AxisAlignedWall placeBox(Vec3 feet, Vec3 look, float bodyYaw, BarrierDefinition definition) {
        double flatReach = Math.sqrt(look.x * look.x + look.z * look.z);
        double faceX;
        double faceZ;
        if (flatReach < 0.2) {
            double radians = Math.toRadians(bodyYaw);
            faceX = -Math.sin(radians);
            faceZ = Math.cos(radians);
        } else {
            faceX = look.x / flatReach;
            faceZ = look.z / flatReach;
        }
        boolean thinX = Math.abs(faceX) >= Math.abs(faceZ);
        if (thinX) {
            faceX = Math.signum(faceX == 0.0 ? 1.0 : faceX);
            faceZ = 0.0;
        } else {
            faceZ = Math.signum(faceZ == 0.0 ? 1.0 : faceZ);
            faceX = 0.0;
        }
        double centerX = feet.x + faceX * definition.forward();
        double centerZ = feet.z + faceZ * definition.forward();
        double bottom = feet.y;
        double halfWidth = definition.width() * 0.5;
        double halfThick = definition.thickness() * 0.5;
        AABB box = thinX
                ? new AABB(centerX - halfThick, bottom, centerZ - halfWidth,
                centerX + halfThick, bottom + definition.height(), centerZ + halfWidth)
                : new AABB(centerX - halfWidth, bottom, centerZ - halfThick,
                centerX + halfWidth, bottom + definition.height(), centerZ + halfThick);
        return new AxisAlignedWall(box);
    }

    private static boolean planShifts(ServerLevel level, AxisAlignedWall wall, ServerPlayer caster, List<Shift> shifts) {
        AABB area = wall.box().inflate(1.0);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, Entity::isAlive)) {
            Entity root = living.getRootVehicle();
            if (!root.getBoundingBox().intersects(wall.box())) {
                continue;
            }
            if (ContactPolicy.mayBodyPass(caster.getUUID(), root)) {
                continue;
            }
            if (alreadyShifted(shifts, root)) {
                continue;
            }
            Vec3 destination = escape(level, wall.box(), root);
            if (destination == null) {
                return false;
            }
            shifts.add(new Shift(root, destination));
        }
        return true;
    }

    private static boolean alreadyShifted(List<Shift> shifts, Entity root) {
        for (Shift shift : shifts) {
            if (shift.root == root) {
                return true;
            }
        }
        return false;
    }

    private static Vec3 escape(ServerLevel level, AABB wall, Entity root) {
        AABB body = root.getBoundingBox();
        double[] offsets = new double[] {
                wall.maxX - body.minX + 0.05, -(body.maxX - wall.minX + 0.05),
                wall.maxZ - body.minZ + 0.05, -(body.maxZ - wall.minZ + 0.05),
                wall.maxY - body.minY + 0.05, -(body.maxY - wall.minY + 0.05)
        };
        int[] axis = new int[] {0, 0, 2, 2, 1, 1};
        int best = -1;
        double bestDistance = Double.MAX_VALUE;
        for (int index = 0; index < offsets.length; index++) {
            double distance = Math.abs(offsets[index]);
            if (distance >= bestDistance) {
                continue;
            }
            Vec3 moved = move(root.position(), axis[index], offsets[index]);
            AABB destination = body.move(moved.x - root.getX(), moved.y - root.getY(), moved.z - root.getZ());
            if (destination.minY < level.getMinBuildHeight()) {
                continue;
            }
            if (!level.noCollision(root, destination) || destination.intersects(wall) || hitsActiveWall(destination)) {
                continue;
            }
            best = index;
            bestDistance = distance;
        }
        if (best < 0) {
            return null;
        }
        return move(root.position(), axis[best], offsets[best]);
    }

    private static boolean hitsActiveWall(AABB destination) {
        for (BarrierBody body : ACTIVE) {
            if (body.active() && destination.intersects(body.wall().box())) {
                return true;
            }
        }
        return false;
    }

    private static Vec3 move(Vec3 origin, int axis, double offset) {
        if (axis == 0) {
            return origin.add(offset, 0.0, 0.0);
        }
        if (axis == 1) {
            return origin.add(0.0, offset, 0.0);
        }
        return origin.add(0.0, 0.0, offset);
    }

    private static void nudgePassengers(Entity root, Vec3 delta) {
        for (Entity passenger : root.getPassengers()) {
            passenger.setPos(passenger.getX() + delta.x, passenger.getY() + delta.y, passenger.getZ() + delta.z);
            nudgePassengers(passenger, delta);
        }
    }

    private static void rollback(List<BarrierBody> spawned) {
        for (BarrierBody body : spawned) {
            body.discard();
        }
    }

    private static final class Shift {
        final Entity root;
        final Vec3 destination;

        Shift(Entity root, Vec3 destination) {
            this.root = root;
            this.destination = destination;
        }
    }
}
