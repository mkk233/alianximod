package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.enchant.resource.NetherContract;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad;
import com.bslx.XueXiMod.shu.technique.query.LivingCandidateQueries;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactCarrier;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactPolicy;
import com.bslx.XueXiMod.shu.technique.release.fire.ResourceEffectKeys;
import com.bslx.XueXiMod.status.fire.seed.FireCastContext;
import com.bslx.XueXiMod.status.fire.seed.NetherSeedArbiter;
import com.bslx.XueXiMod.shu.technique.release.batch.RootCastBindings;
import com.bslx.XueXiMod.network.NetherDomainSync;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * 幽冥只挂在已经存在的领域宿主上。雾雨、重压走 {@link DomainInstance}，灼地走现有贴地宿主。
 * 产种在全局 20 tick 窗口把当时全部覆盖领域一次交给 {@link NetherSeedArbiter}，这里不另建施法、不改燃烧时长。
 */
public final class NetherDomainCycle {
    private static final ArrayList<Lease> LEASES = new ArrayList<>();
    private static final double BOUNDS_SHIFT = 0.02d;

    private NetherDomainCycle() {
    }

    /** 快照里带有幽冥标识就登记。接触伤害为 0 也登记，不因此打开接触伤害扫描。 */
    public static boolean consider(ContactCarrier carrier, EnchantmentSnapshot snapshot) {
        if (!marked(snapshot)) {
            return false;
        }
        return openLease(carrier, null);
    }

    /**
     * 真实释放帧上的载荷。没有幽冥标记则什么都不做。
     * 根标识必须已经在 {@link RootCastBindings} 里，否则只记住、不新建上下文。
     */
    public static boolean attach(ContactCarrier carrier, @Nullable ResourceEnchantLoad load, @Nullable UUID rootCastId) {
        if (load == null || !load.gloom()) {
            return false;
        }
        return openLease(carrier, rootCastId);
    }

    /** 领域已经活着时补上根持有。找不到该标识就保持未持有。 */
    public static boolean bindRoot(UUID domainId, @Nullable UUID rootCastId) {
        Lease lease = find(domainId);
        if (lease == null || rootCastId == null) {
            return false;
        }
        return holdRoot(lease, rootCastId);
    }

    public static void drop(UUID domainId) {
        if (domainId == null) {
            return;
        }
        int cursor = 0;
        while (cursor < LEASES.size()) {
            if (domainId.equals(LEASES.get(cursor).domainId)) {
                removeLease(cursor);
                return;
            }
            cursor++;
        }
    }

    public static void dropLevel(ServerLevel level) {
        if (level == null) {
            return;
        }
        int cursor = LEASES.size();
        while (cursor > 0) {
            cursor--;
            Lease lease = LEASES.get(cursor);
            if (lease.carrier.hostLevel() == level) {
                removeLease(cursor);
            }
        }
    }

    public static void clearAll() {
        int cursor = LEASES.size();
        while (cursor > 0) {
            cursor--;
            removeLease(cursor);
        }
    }

    public static void tick(MinecraftServer server) {
        if (server == null) {
            return;
        }
        int serverTick = server.getTickCount();
        int cursor = LEASES.size();
        while (cursor > 0) {
            cursor--;
            Lease lease = LEASES.get(cursor);
            if (expired(lease)) {
                removeLease(cursor);
                continue;
            }
            if (lease.pendingRoot != null && !lease.rootHeld) {
                holdRoot(lease, lease.pendingRoot);
            }
            refreshVolumes(lease);
        }
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return;
        }
        long gameTime = overworld.getGameTime();
        if (gameTime > 0L && gameTime % NetherContract.PERIOD_TICKS == 0L) {
            grantWindow(server, serverTick, gameTime / NetherContract.PERIOD_TICKS);
        }
    }

    public static void sendSnapshot(ServerPlayer watcher) {
        if (watcher == null || !(watcher.level() instanceof ServerLevel level)) {
            return;
        }
        int cursor = 0;
        while (cursor < LEASES.size()) {
            Lease lease = LEASES.get(cursor);
            if (lease.announced && lease.volumes != null && lease.carrier.hostLevel() == level) {
                emit(watcher, lease, false);
            }
            cursor++;
        }
    }

    public static boolean contains(ServerLevel level, double worldX, double worldY, double worldZ) {
        if (level == null) {
            return false;
        }
        int cursor = 0;
        while (cursor < LEASES.size()) {
            Lease lease = LEASES.get(cursor);
            if (lease.carrier.hostLevel() == level && inside(lease.volumes, worldX, worldY, worldZ)) {
                return true;
            }
            cursor++;
        }
        return false;
    }

    /** 宿主刚算完当前切片时立刻对齐租约，不等下一轮领域节拍。 */
    public static void sync(ContactCarrier carrier) {
        if (!(carrier instanceof HostView hostView)) {
            return;
        }
        Lease lease = find(hostView.instanceId());
        if (lease != null && !expired(lease)) {
            refreshVolumes(lease);
        }
    }

    public static int leaseCount() {
        return LEASES.size();
    }

    private static boolean marked(EnchantmentSnapshot snapshot) {
        return snapshot != null && snapshot.basicTypeIds().contains(ResourceEffectKeys.GLOOM);
    }

    private static boolean supported(ContactCarrier carrier) {
        if (carrier instanceof DomainInstance) {
            return true;
        }
        String typeName = carrier.getClass().getName();
        return typeName.endsWith("SurfaceFireHosts$Host");
    }

    private static boolean openLease(ContactCarrier carrier, @Nullable UUID rootCastId) {
        if (!(carrier instanceof HostView hostView) || !supported(carrier) || !carrier.active()) {
            return false;
        }
        UUID domainId = hostView.instanceId();
        if (domainId == null) {
            return false;
        }
        Lease existing = find(domainId);
        if (existing != null) {
            holdRoot(existing, rootCastId);
            return true;
        }
        ServerLevel level = carrier.hostLevel();
        if (level == null || level.getServer() == null) {
            return false;
        }
        int serverTick = level.getServer().getTickCount();
        long remaining = (long) hostView.expiresAt() - serverTick;
        if (remaining < 0L) {
            remaining = 0L;
        }
        Lease created = new Lease(domainId, carrier, hostView, level.getGameTime() + remaining);
        LEASES.add(created);
        holdRoot(created, rootCastId);
        refreshVolumes(created);
        return true;
    }

    private static boolean holdRoot(Lease lease, @Nullable UUID rootCastId) {
        if (lease == null || lease.rootHeld || rootCastId == null) {
            return lease != null && lease.rootHeld;
        }
        FireCastContext found = RootCastBindings.lookup(rootCastId);
        if (found == null) {
            lease.pendingRoot = rootCastId;
            return false;
        }
        RootCastBindings.retain(rootCastId);
        lease.rootCastId = rootCastId;
        lease.pendingRoot = null;
        lease.rootHeld = true;
        return true;
    }

    private static void releaseRoot(Lease lease) {
        if (lease == null || !lease.rootHeld || lease.rootCastId == null) {
            return;
        }
        RootCastBindings.release(lease.rootCastId);
        lease.rootHeld = false;
    }

    private static Lease find(UUID domainId) {
        if (domainId == null) {
            return null;
        }
        int cursor = 0;
        while (cursor < LEASES.size()) {
            Lease lease = LEASES.get(cursor);
            if (domainId.equals(lease.domainId)) {
                return lease;
            }
            cursor++;
        }
        return null;
    }

    private static boolean expired(Lease lease) {
        ContactCarrier carrier = lease.carrier;
        if (carrier == null || !carrier.active()) {
            return true;
        }
        ServerLevel level = carrier.hostLevel();
        if (level == null || level.getServer() == null) {
            return true;
        }
        return level.getServer().getLevel(level.dimension()) != level;
    }

    private static void refreshVolumes(Lease lease) {
        double[] next = capture(lease);
        if (next == null) {
            return;
        }
        if (!shifted(lease.volumes, next) && lease.announced) {
            return;
        }
        if (lease.announced) {
            lease.revision++;
        }
        lease.volumes = next;
        lease.bounds = outerOf(next);
        announce(lease, false);
    }

    private static double[] capture(Lease lease) {
        if (lease.carrier instanceof GloomVolumes volumes) {
            int count = volumes.exportVolumes(null);
            if (count < 0 || count > NetherDomainSync.VOLUME_CAP) {
                return new double[0];
            }
            double[] packed = new double[count * 6];
            int written = volumes.exportVolumes(packed);
            if (written != count) {
                return new double[0];
            }
            return packed;
        }
        DomainGeometry geometry = lease.hostView.geometry();
        AABB box = geometry == null ? null : geometry.bounds();
        if (box == null) {
            return null;
        }
        return new double[] {box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ};
    }

    private static boolean shifted(double[] previous, double[] next) {
        if (previous == null || next == null || previous.length != next.length) {
            return true;
        }
        int cursor = 0;
        while (cursor < next.length) {
            if (Math.abs(previous[cursor] - next[cursor]) > BOUNDS_SHIFT) {
                return true;
            }
            cursor++;
        }
        return false;
    }

    private static AABB outerOf(double[] volumes) {
        if (volumes == null || volumes.length < 6) {
            return null;
        }
        double lowX = volumes[0];
        double lowY = volumes[1];
        double lowZ = volumes[2];
        double highX = volumes[3];
        double highY = volumes[4];
        double highZ = volumes[5];
        int cursor = 6;
        while (cursor + 5 < volumes.length) {
            lowX = Math.min(lowX, volumes[cursor]);
            lowY = Math.min(lowY, volumes[cursor + 1]);
            lowZ = Math.min(lowZ, volumes[cursor + 2]);
            highX = Math.max(highX, volumes[cursor + 3]);
            highY = Math.max(highY, volumes[cursor + 4]);
            highZ = Math.max(highZ, volumes[cursor + 5]);
            cursor += 6;
        }
        return new AABB(lowX, lowY, lowZ, highX, highY, highZ);
    }

    private static boolean inside(double[] volumes, double worldX, double worldY, double worldZ) {
        if (volumes == null) {
            return false;
        }
        int cursor = 0;
        while (cursor + 5 < volumes.length) {
            if (worldX >= volumes[cursor] && worldX < volumes[cursor + 3]
                    && worldY >= volumes[cursor + 1] && worldY < volumes[cursor + 4]
                    && worldZ >= volumes[cursor + 2] && worldZ < volumes[cursor + 5]) {
                return true;
            }
            cursor += 6;
        }
        return false;
    }

    private static void announce(Lease lease, boolean removed) {
        ServerLevel level = lease.carrier.hostLevel();
        if (level == null) {
            return;
        }
        for (ServerPlayer watcher : level.players()) {
            emit(watcher, lease, removed);
        }
        if (!removed) {
            lease.announced = true;
        }
    }

    private static void emit(ServerPlayer watcher, Lease lease, boolean removed) {
        ServerLevel level = lease.carrier.hostLevel();
        if (level == null || watcher == null) {
            return;
        }
        AABB box = lease.bounds;
        double lowX = box == null ? 0.0d : box.minX;
        double lowY = box == null ? 0.0d : box.minY;
        double lowZ = box == null ? 0.0d : box.minZ;
        double highX = box == null ? 0.0d : box.maxX;
        double highY = box == null ? 0.0d : box.maxY;
        double highZ = box == null ? 0.0d : box.maxZ;
        String dimensionId = level.dimension().location().toString();
        com.bslx.XueXiMod.network.NetherDomainSync.emit(watcher, removed, lease.domainId, lease.revision,
                lease.hostView.ownerId(), dimensionId, lowX, lowY, lowZ, highX, highY, highZ, lease.visualEnd,
                lease.volumes);
    }

    private static void removeLease(int cursor) {
        Lease lease = LEASES.remove(cursor);
        releaseRoot(lease);
        announce(lease, true);
    }

    private static void grantWindow(MinecraftServer server, int serverTick, long windowId) {
        HashMap<UUID, ArrayList<NetherSeedArbiter.NetherOffer>> grouped = new HashMap<>();
        HashMap<UUID, LivingEntity> bodies = new HashMap<>();
        int cursor = 0;
        while (cursor < LEASES.size()) {
            Lease lease = LEASES.get(cursor);
            cursor++;
            if (serverTick - lease.hostView.startedAt() < NetherContract.PERIOD_TICKS) {
                continue;
            }
            ServerLevel level = lease.carrier.hostLevel();
            if (level == null || level.getServer() != server) {
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(lease.hostView.ownerId());
            if (owner == null || !owner.isAlive()) {
                continue;
            }
            AABB query = lease.carrier.queryBox();
            if (query == null) {
                continue;
            }
            List<LivingEntity> found = LivingCandidateQueries.entityQuery(level, query, LivingEntity::isAlive);
            int livingCursor = 0;
            while (livingCursor < found.size()) {
                LivingEntity living = found.get(livingCursor);
                livingCursor++;
                if (living == null || living.level() != level || !living.isAlive()) {
                    continue;
                }
                if (!ContactPolicy.mayContactAffect(lease.hostView.ownerId(), living) || !lease.carrier.touches(living)) {
                    continue;
                }
                UUID targetId = living.getUUID();
                if (targetId == null) {
                    continue;
                }
                ArrayList<NetherSeedArbiter.NetherOffer> offers = grouped.get(targetId);
                if (offers == null) {
                    offers = new ArrayList<>();
                    grouped.put(targetId, offers);
                    bodies.put(targetId, living);
                }
                if (!containsDomain(offers, lease.domainId)) {
                    offers.add(new NetherSeedArbiter.NetherOffer(lease.domainId, owner));
                }
            }
        }
        for (UUID targetId : grouped.keySet()) {
            LivingEntity living = bodies.get(targetId);
            ArrayList<NetherSeedArbiter.NetherOffer> offers = grouped.get(targetId);
            if (living != null && offers != null && !offers.isEmpty()) {
                NetherSeedArbiter.grant(living, windowId, offers);
            }
        }
    }

    private static boolean containsDomain(List<NetherSeedArbiter.NetherOffer> offers, UUID domainId) {
        int cursor = 0;
        while (cursor < offers.size()) {
            NetherSeedArbiter.NetherOffer offer = offers.get(cursor);
            if (offer != null && domainId.equals(offer.domainId())) {
                return true;
            }
            cursor++;
        }
        return false;
    }

    private static final class Lease {
        private final UUID domainId;
        private final ContactCarrier carrier;
        private final HostView hostView;
        private final long visualEnd;
        private UUID rootCastId;
        private UUID pendingRoot;
        private AABB bounds;
        private double[] volumes;
        private int revision = 1;
        private boolean rootHeld;
        private boolean announced;

        private Lease(UUID domainId, ContactCarrier carrier, HostView hostView, long visualEnd) {
            this.domainId = domainId;
            this.carrier = carrier;
            this.hostView = hostView;
            this.visualEnd = visualEnd;
        }
    }
}
