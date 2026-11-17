package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.network.prison.PrisonSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 客户端水牢快照。服务端仍是权威；这里只给本地玩家做位置预测，并在到期、换维、断线时清掉残影。
 * 合法传送先放开预测，画面留到权威删除包。
 */
public final class PrisonClientState {
    private static final double TELEPORT_SLACK = 0.05D;
    private static final ArrayList<PrisonClientSnapshot> SNAPSHOTS = new ArrayList<>();
    private static final HashMap<UUID, Long> TOMBSTONES = new HashMap<>();
    private static final ArrayList<UUID> RELEASED = new ArrayList<>();

    private PrisonClientState() {
    }

    public static void accept(PrisonSyncPacket packet) {
        if (packet == null || packet.prisonId() == null) {
            return;
        }
        if (packet.kind() == PrisonSyncPacket.REMOVE) {
            bury(packet.prisonId(), packet.revision());
            return;
        }
        if (packet.kind() != PrisonSyncPacket.SNAPSHOT) {
            return;
        }
        Long buried = TOMBSTONES.get(packet.prisonId());
        if (buried != null && packet.revision() <= buried) {
            return;
        }
        PrisonClientSnapshot incoming = snapshotOf(packet);
        if (incoming == null) {
            return;
        }
        PrisonClientSnapshot previous = find(packet.prisonId());
        if (previous != null && packet.revision() < previous.revision()) {
            return;
        }
        boolean newer = previous == null || packet.revision() > previous.revision();
        if (previous != null) {
            SNAPSHOTS.remove(previous);
        }
        SNAPSHOTS.add(incoming);
        TOMBSTONES.remove(packet.prisonId());
        if (newer) {
            RELEASED.remove(packet.prisonId());
        }
    }

    public static Optional<PrisonClientSnapshot> lockFor(Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        purge();
        PrisonClientSnapshot networkOnly = null;
        for (PrisonClientSnapshot snapshot : SNAPSHOTS) {
            if (RELEASED.contains(snapshot.prisonId()) || !current(snapshot)) {
                continue;
            }
            boolean uuidHit = snapshot.targetId() != null && snapshot.targetId().equals(entity.getUUID());
            boolean networkHit = snapshot.targetNetworkId() >= 0 && entity.getId() == snapshot.targetNetworkId();
            if (uuidHit) {
                return Optional.of(snapshot);
            }
            if (networkHit && snapshot.targetId() == null && networkOnly == null) {
                networkOnly = snapshot;
            }
        }
        return Optional.ofNullable(networkOnly);
    }

    public static List<PrisonClientSnapshot> visible() {
        purge();
        ArrayList<PrisonClientSnapshot> shown = new ArrayList<>();
        for (PrisonClientSnapshot snapshot : SNAPSHOTS) {
            if (current(snapshot)) {
                shown.add(snapshot);
            }
        }
        return shown;
    }

    public static void holdLocal(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player == null || minecraft.player != player) {
            return;
        }
        Optional<PrisonClientSnapshot> locked = lockFor(player);
        if (locked.isEmpty()) {
            return;
        }
        Vec3 anchor = locked.get().anchor();
        if (anchor == null || !Double.isFinite(anchor.x) || !Double.isFinite(anchor.y) || !Double.isFinite(anchor.z)) {
            return;
        }
        player.xxa = 0.0F;
        player.yya = 0.0F;
        player.zza = 0.0F;
        player.setJumping(false);
        if (player.input != null) {
            player.input.jumping = false;
            player.input.forwardImpulse = 0.0F;
            player.input.leftImpulse = 0.0F;
            player.input.up = false;
            player.input.down = false;
            player.input.left = false;
            player.input.right = false;
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.setPos(anchor.x, anchor.y, anchor.z);
    }

    /**
     * 位置包先落地。落点已经离开壳才解除本地旧锁，纠偏仍留在锚点上。
     * 不改包本身，随后的 tick 也不会把人拉回旧锚点。
     */
    public static void noteServerTeleport(double nextX, double nextY, double nextZ) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        Optional<PrisonClientSnapshot> locked = lockFor(player);
        if (locked.isEmpty()) {
            return;
        }
        AABB bounds = locked.get().bounds();
        if (bounds == null) {
            return;
        }
        boolean outside = nextX < bounds.minX - TELEPORT_SLACK || nextX > bounds.maxX + TELEPORT_SLACK
                || nextY < bounds.minY - TELEPORT_SLACK || nextY > bounds.maxY + TELEPORT_SLACK
                || nextZ < bounds.minZ - TELEPORT_SLACK || nextZ > bounds.maxZ + TELEPORT_SLACK;
        if (outside) {
            if (!RELEASED.contains(locked.get().prisonId())) {
                RELEASED.add(locked.get().prisonId());
            }
        }
    }

    public static void clear() {
        SNAPSHOTS.clear();
        TOMBSTONES.clear();
        RELEASED.clear();
    }

    public static void purge() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clear();
            return;
        }
        ResourceLocation here = minecraft.level.dimension().location();
        long gameTime = minecraft.level.getGameTime();
        Iterator<PrisonClientSnapshot> iterator = SNAPSHOTS.iterator();
        while (iterator.hasNext()) {
            PrisonClientSnapshot snapshot = iterator.next();
            if (snapshot.dimensionId() == null || !snapshot.dimensionId().equals(here)) {
                RELEASED.remove(snapshot.prisonId());
                iterator.remove();
                continue;
            }
            if (gameTime >= snapshot.endTick()) {
                rememberTombstone(snapshot.prisonId(), snapshot.revision());
                RELEASED.remove(snapshot.prisonId());
                iterator.remove();
            }
        }
    }

    private static void bury(UUID prisonId, long revision) {
        PrisonClientSnapshot previous = find(prisonId);
        if (previous != null && revision < previous.revision()) {
            return;
        }
        SNAPSHOTS.removeIf(snapshot -> prisonId.equals(snapshot.prisonId()));
        RELEASED.remove(prisonId);
        rememberTombstone(prisonId, revision);
    }

    private static void rememberTombstone(UUID prisonId, long revision) {
        Long buried = TOMBSTONES.get(prisonId);
        if (buried == null || revision > buried) {
            TOMBSTONES.put(prisonId, revision);
        }
    }

    private static boolean current(PrisonClientSnapshot snapshot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || snapshot.dimensionId() == null) {
            return false;
        }
        if (!snapshot.dimensionId().equals(minecraft.level.dimension().location())) {
            return false;
        }
        return minecraft.level.getGameTime() < snapshot.endTick();
    }

    private static PrisonClientSnapshot find(UUID prisonId) {
        for (PrisonClientSnapshot snapshot : SNAPSHOTS) {
            if (prisonId.equals(snapshot.prisonId())) {
                return snapshot;
            }
        }
        return null;
    }

    private static PrisonClientSnapshot snapshotOf(PrisonSyncPacket packet) {
        if (packet.dimensionId() == null || packet.targetId() == null) {
            return null;
        }
        Vec3 anchor = new Vec3(packet.anchorX(), packet.anchorY(), packet.anchorZ());
        AABB bounds = new AABB(packet.lowX(), packet.lowY(), packet.lowZ(), packet.highX(), packet.highY(), packet.highZ());
        return new PrisonClientSnapshot(packet.prisonId(), packet.revision(), packet.casterId(), packet.targetId(),
                packet.targetNetworkId(), packet.dimensionId(), anchor, bounds, packet.lockedTier(),
                packet.birthTick(), packet.endTick());
    }

    public record PrisonClientSnapshot(
            UUID prisonId,
            long revision,
            UUID casterId,
            UUID targetId,
            int targetNetworkId,
            ResourceLocation dimensionId,
            Vec3 anchor,
            AABB bounds,
            int lockedTier,
            long birthTick,
            long endTick) {
    }
}
