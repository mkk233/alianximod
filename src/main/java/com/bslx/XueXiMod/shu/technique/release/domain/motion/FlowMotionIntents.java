package com.bslx.XueXiMod.shu.technique.release.domain.motion;

import com.bslx.XueXiMod.network.FlowPlayerBridge;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainInstance;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainService;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 持续流场意图服务。牵引、环绕和气泡只发布这一拍的位移，不写入持久速度。
 * 球形场地直接加运动向量，护体不发布根实体位移。
 */
public final class FlowMotionIntents {
    private static final Map<UUID, List<HeldShift>> BY_ROOT = new HashMap<>();
    private static final Map<UUID, Set<UUID>> BY_INSTANCE = new HashMap<>();
    private static final Map<UUID, Long> EPOCH = new HashMap<>();
    private static final Map<UUID, Long> STEP_CLOCK = new HashMap<>();
    private static final Set<UUID> CONSUMED = new HashSet<>();
    private static final ThreadLocal<Boolean> SIMULATING = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean> ALREADY_INCLUDED = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private FlowMotionIntents() {
    }

    /** 发布这一拍位移。乘客、空领域或空角色不登记；同领域旧意图被替换。 */
    public static void offer(DomainInstance domain, Entity root, FlowRole role, double horizontalX, double horizontalZ,
                             boolean rising, double riseFloor) {
        if (domain == null || root == null || root.isPassenger() || domain.hostLevel() == null || role == null) {
            return;
        }
        long epoch = EPOCH.computeIfAbsent(domain.instanceId(), ignored -> 1L);
        long step = STEP_CLOCK.merge(domain.instanceId(), 1L, Long::sum);
        int issuedTick = domain.hostLevel().getServer().getTickCount();
        HeldShift shift = new HeldShift(domain.hostLevel().dimension(), domain.instanceId(), root.getUUID(), role,
                epoch, step, horizontalX, horizontalZ, rising, riseFloor, issuedTick);
        List<HeldShift> stored = BY_ROOT.computeIfAbsent(root.getUUID(), ignored -> new ArrayList<>());
        stored.removeIf(existing -> domain.instanceId().equals(existing.instanceId));
        stored.add(shift);
        BY_INSTANCE.computeIfAbsent(domain.instanceId(), ignored -> new HashSet<>()).add(root.getUUID());
        if (root instanceof ServerPlayer serverPlayer && serverPlayer.isSleeping()) {
            serverPlayer.stopSleepInBed(false, true);
        }
        FlowPlayerBridge.notice(domain, root, shift);
    }

    /** 只保留仍被持有的根。不在名单里的根退役，不退回已收水势。 */
    public static void retain(DomainInstance domain, Set<UUID> stillHeld) {
        if (domain == null) {
            return;
        }
        Set<UUID> known = BY_INSTANCE.get(domain.instanceId());
        if (known == null) {
            return;
        }
        ArrayList<UUID> dropped = new ArrayList<>();
        for (UUID rootId : known) {
            if (stillHeld == null || !stillHeld.contains(rootId)) {
                dropped.add(rootId);
            }
        }
        for (UUID rootId : dropped) {
            retireRoot(domain.instanceId(), rootId);
        }
    }

    /** 退役整个领域的意图并推进世代。不关闭其他领域，不退资源。 */
    public static void retireInstance(DomainInstance domain) {
        if (domain == null) {
            return;
        }
        UUID instanceId = domain.instanceId();
        long current = EPOCH.getOrDefault(instanceId, 1L);
        EPOCH.put(instanceId, current + 1L);
        Set<UUID> roots = BY_INSTANCE.remove(instanceId);
        if (roots != null) {
            for (UUID rootId : roots) {
                dropPair(instanceId, rootId);
                FlowPlayerBridge.recall(rootId);
            }
        }
        FlowPlayerBridge.forgetInstance(instanceId);
    }

    /** 退役一个领域和一个根的配对。任一标识为空时不改表。 */
    public static void retireRoot(UUID instanceId, UUID rootId) {
        if (instanceId == null || rootId == null) {
            return;
        }
        dropPair(instanceId, rootId);
        Set<UUID> roots = BY_INSTANCE.get(instanceId);
        if (roots != null) {
            roots.remove(rootId);
            if (roots.isEmpty()) {
                BY_INSTANCE.remove(instanceId);
            }
        }
        FlowPlayerBridge.recall(rootId);
    }

    /** 退役实体在所有领域的意图。空实体忽略，玩家同时清流场桥。 */
    public static void retireEntity(Entity entity) {
        if (entity == null) {
            return;
        }
        List<HeldShift> stored = BY_ROOT.remove(entity.getUUID());
        if (stored != null) {
            for (HeldShift shift : stored) {
                Set<UUID> roots = BY_INSTANCE.get(shift.instanceId);
                if (roots != null) {
                    roots.remove(entity.getUUID());
                    if (roots.isEmpty()) {
                        BY_INSTANCE.remove(shift.instanceId);
                    }
                }
            }
        }
        CONSUMED.remove(entity.getUUID());
        if (entity instanceof ServerPlayer serverPlayer) {
            FlowPlayerBridge.clear(serverPlayer);
        }
    }

    public static long epochOf(UUID instanceId) {
        if (instanceId == null) {
            return 0L;
        }
        return EPOCH.getOrDefault(instanceId, 0L);
    }

    /** 判断实体是否还有正的上升下限。没有有效意图时返回假。 */
    public static boolean hasEffectiveRise(Entity entity) {
        return riseFloor(entity) > 0.0D;
    }

    /** 读取当前上升下限。客户端改读本地桥，没有意图时返回 0。 */
    public static double riseFloor(Entity entity) {
        if (entity != null && entity.level().isClientSide()) {
            return FlowClientBridge.riseFloor(entity.getRootVehicle());
        }
        HeldShift chosen = chooseRise(entity);
        if (chosen == null) {
            return 0.0D;
        }
        return chosen.riseFloor;
    }

    /** 判断还有水平牵引或上升意图。两者都没有时返回假。 */
    public static boolean hasLiveShift(Entity entity) {
        return chooseHorizontal(entity) != null || chooseRise(entity) != null;
    }

    /** 清除这个根本拍已消费标记。空实体忽略。 */
    public static void beginStep(Entity entity) {
        if (entity != null) {
            CONSUMED.remove(entity.getUUID());
        }
    }

    /** 给未被本拍消费的服务端实体补一次自身移动。客户端、乘客或无效实体跳过。 */
    public static void finishStep(Entity entity) {
        if (entity == null || entity.level().isClientSide() || clientDriven(entity)) {
            return;
        }
        if (CONSUMED.contains(entity.getUUID()) || !entity.isAlive() || entity.isRemoved() || entity.isPassenger()) {
            return;
        }
        if (!hasLiveShift(entity)) {
            return;
        }
        entity.move(MoverType.SELF, entity.getDeltaMovement());
    }

    /** 标记当前线程正在模拟。模拟期间组合位移不再叠加流场。 */
    public static void simulating(boolean active) {
        SIMULATING.set(active);
    }

    /** 标记当前位移已经包含流场。重复组合时保持原位移。 */
    public static void alreadyIncluded(boolean active) {
        ALREADY_INCLUDED.set(active);
    }

    /** 把一份额外水平和上升下限合成到原位移。已消费、模拟或乘客返回原位移。 */
    public static Vec3 compose(Entity mover, Vec3 nativeMove) {
        if (mover == null || nativeMove == null) {
            return nativeMove;
        }
        if (mover.level().isClientSide()) {
            return FlowClientBridge.combine(mover, nativeMove);
        }
        if (SIMULATING.get() || ALREADY_INCLUDED.get() || clientDriven(mover) || mover.isPassenger()) {
            return nativeMove;
        }
        if (CONSUMED.contains(mover.getUUID())) {
            return nativeMove;
        }
        HeldShift horizontal = chooseHorizontal(mover);
        HeldShift rising = chooseRise(mover);
        if (horizontal == null && rising == null) {
            return nativeMove;
        }
        CONSUMED.add(mover.getUUID());
        double nextX = nativeMove.x;
        double nextZ = nativeMove.z;
        if (horizontal != null) {
            nextX += horizontal.horizontalX;
            nextZ += horizontal.horizontalZ;
        }
        double nextY = nativeMove.y;
        if (rising != null && nativeMove.y < rising.riseFloor) {
            nextY = rising.riseFloor;
        }
        if (nextX != nativeMove.x || nextY != nativeMove.y || nextZ != nativeMove.z) {
            mover.hasImpulse = true;
        }
        return new Vec3(nextX, nextY, nextZ);
    }

    /** 判断移动者是否由玩家客户端驱动。玩家为真，矿车为假。 */
    public static boolean clientDriven(Entity mover) {
        if (mover instanceof Player) {
            return true;
        }
        if (mover instanceof AbstractMinecart) {
            return false;
        }
        return mover != null && mover.getControllingPassenger() instanceof Player;
    }

    private static HeldShift chooseHorizontal(Entity entity) {
        List<HeldShift> fresh = fresh(entity);
        HeldShift chosen = null;
        for (HeldShift shift : fresh) {
            if (shift.horizontalX == 0.0D && shift.horizontalZ == 0.0D) {
                continue;
            }
            if (chosen == null || prefer(entity, shift, chosen)) {
                chosen = shift;
            }
        }
        return chosen;
    }

    private static HeldShift chooseRise(Entity entity) {
        List<HeldShift> fresh = fresh(entity);
        HeldShift chosen = null;
        for (HeldShift shift : fresh) {
            if (!shift.rising) {
                continue;
            }
            if (chosen == null || shift.riseFloor > chosen.riseFloor
                    || (shift.riseFloor == chosen.riseFloor && shift.instanceId.compareTo(chosen.instanceId) < 0)) {
                chosen = shift;
            }
        }
        return chosen;
    }

    private static boolean prefer(Entity entity, HeldShift candidate, HeldShift current) {
        boolean candidateHeld = controls(candidate, entity);
        boolean currentHeld = controls(current, entity);
        if (candidateHeld != currentHeld) {
            return candidateHeld;
        }
        return candidate.instanceId.compareTo(current.instanceId) < 0;
    }

    private static boolean controls(HeldShift shift, Entity entity) {
        DomainInstance domain = DomainService.find(shift.instanceId);
        if (domain == null) {
            return false;
        }
        ReleaseMotionLedger ledger = ReleaseMotionLedger.find(domain);
        return ledger != null && ledger.controls(domain.instanceId(), entity.getUUID());
    }

    private static List<HeldShift> fresh(Entity entity) {
        if (entity == null || entity.level().isClientSide()) {
            return List.of();
        }
        List<HeldShift> stored = BY_ROOT.get(entity.getUUID());
        if (stored == null || stored.isEmpty()) {
            return List.of();
        }
        int now = entity.level().getServer().getTickCount();
        ResourceKey<Level> dimension = entity.level().dimension();
        ArrayList<HeldShift> accepted = new ArrayList<>();
        Iterator<HeldShift> iterator = stored.iterator();
        while (iterator.hasNext()) {
            HeldShift shift = iterator.next();
            if (!dimension.equals(shift.dimension) || now > shift.issuedTick + 1
                    || !VolumeMotionRuntime.flowStillHolds(shift.role, shift.instanceId, entity)) {
                iterator.remove();
                Set<UUID> roots = BY_INSTANCE.get(shift.instanceId);
                if (roots != null) {
                    roots.remove(entity.getUUID());
                }
                continue;
            }
            accepted.add(shift);
        }
        if (stored.isEmpty()) {
            BY_ROOT.remove(entity.getUUID());
        }
        accepted.sort(Comparator.comparing(shift -> shift.instanceId));
        return accepted;
    }

    private static void dropPair(UUID instanceId, UUID rootId) {
        List<HeldShift> stored = BY_ROOT.get(rootId);
        if (stored == null) {
            return;
        }
        stored.removeIf(shift -> instanceId.equals(shift.instanceId));
        if (stored.isEmpty()) {
            BY_ROOT.remove(rootId);
        }
    }

    public static final class HeldShift {
        public final ResourceKey<Level> dimension;
        public final UUID instanceId;
        public final UUID rootId;
        public final FlowRole role;
        public final long epoch;
        public final long step;
        public final double horizontalX;
        public final double horizontalZ;
        public final boolean rising;
        public final double riseFloor;
        public final int issuedTick;

        HeldShift(ResourceKey<Level> dimension, UUID instanceId, UUID rootId, FlowRole role, long epoch, long step,
                  double horizontalX, double horizontalZ, boolean rising, double riseFloor, int issuedTick) {
            this.dimension = dimension;
            this.instanceId = instanceId;
            this.rootId = rootId;
            this.role = role;
            this.epoch = epoch;
            this.step = step;
            this.horizontalX = horizontalX;
            this.horizontalZ = horizontalZ;
            this.rising = rising;
            this.riseFloor = riseFloor;
            this.issuedTick = issuedTick;
        }
    }
}
