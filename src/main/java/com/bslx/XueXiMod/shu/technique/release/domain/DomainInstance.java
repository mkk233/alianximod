package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.domain.motion.StillWaterLease;
import com.bslx.XueXiMod.shu.technique.release.domain.motion.VolumeMotionRuntime;
import com.bslx.XueXiMod.shu.technique.release.bind.BoundReleasePlan;
import com.bslx.XueXiMod.shu.technique.release.bind.RoundHandle;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactCarrier;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactEnchantState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 一个已经落下的领域实例。几何和到期由它自己保存，开关与广播由领域服务管理。
 * 球形场地钉在锚点并收维护，护体跟着身体；这份实例存在期间不收维护。
 */
public final class DomainInstance implements ContactCarrier, DomainBound, HostView {
    private final UUID instanceId = UUID.randomUUID();
    private final UUID ownerId;
    private final ServerLevel level;
    private DomainGeometry geometry;
    private final DomainDefinition definition;
    private final int revision = 1;
    private final int startedAt;
    private final int expiresAt;
    private final long visualOrigin;
    private final long visualEnd;
    private final int animSeed;
    private final @Nullable BoundReleasePlan boundPlan;
    private EnchantmentSnapshot contactSnapshot;
    private ResourceEnchantLoad carriedLoad = ResourceEnchantLoad.EMPTY;
    private UUID releaseGroup;
    private int declarationSlot = -1;
    private UUID mountedProvider;
    private UUID mountedHost;
    private boolean mountRetired;
    private boolean liftedColumn;
    private double columnScale = -1.0D;
    private final ArrayList<UUID> publishedRoots = new ArrayList<>();
    private final ArrayList<RoundHandle> openRounds = new ArrayList<>();
    private int nextBindingBeat;
    private boolean active = true;
    private boolean removalBroadcast;
    private ContactEnchantState contact;

    /** 建没有绑定计划的实例。寿命和非正寿命都按同一冻结规则写下，不扣资源。 */
    DomainInstance(UUID ownerId, ServerLevel level, DomainGeometry geometry, DomainDefinition definition, int startedAt,
                   int scaledLife) {
        this(ownerId, level, geometry, definition, startedAt, scaledLife, null);
    }

    /** 建带绑定计划的实例。计划为空时不安排节拍，资源仍留到领域服务结算。 */
    DomainInstance(UUID ownerId, ServerLevel level, DomainGeometry geometry, DomainDefinition definition, int startedAt,
                   int scaledLife, @Nullable BoundReleasePlan boundPlan) {
        this.ownerId = ownerId;
        this.level = level;
        this.geometry = geometry;
        this.definition = definition;
        this.startedAt = startedAt;
        this.visualOrigin = level == null ? 0L : level.getGameTime();
        FrozenClock clock = FrozenClock.freeze(startedAt, this.visualOrigin, scaledLife);
        this.expiresAt = clock.expiresAt;
        this.visualEnd = clock.visualEnd;
        this.animSeed = Math.floorMod(instanceId.hashCode(), 16);
        this.boundPlan = boundPlan;
        this.nextBindingBeat = boundPlan == null ? 0 : startedAt + DomainBindingRuntime.PERIOD_TICKS;
    }

    /** 同一份已缩放寿命同时写下服务器到期刻和视觉结束时刻。非正寿命不进到期助手。 */
    public static final class FrozenClock {
        public final int expiresAt;
        public final long visualEnd;

        private FrozenClock(int expiresAt, long visualEnd) {
            this.expiresAt = expiresAt;
            this.visualEnd = visualEnd;
        }

        public static FrozenClock freeze(int startedAt, long origin, int scaledLife) {
            if (scaledLife <= 0) {
                return new FrozenClock(startedAt + scaledLife, origin + scaledLife);
            }
            return new FrozenClock(
                    ReleaseInput.expiryTick(startedAt, scaledLife),
                    ReleaseInput.expiryGameTime(origin, scaledLife));
        }
    }

    /** 记下额外附魔。领域不再打开逐目标接触扫描，周期施加由整域节拍负责。 */
    void bindContact(EnchantmentSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        this.contactSnapshot = snapshot;
    }

    /** 开域时记下宿主自己的资源效果。幽冥产种留在这里，不进入额外附魔。 */
    void bindCarriedLoad(@Nullable ResourceEnchantLoad load) {
        this.carriedLoad = load == null ? ResourceEnchantLoad.EMPTY : load;
        if (this.carriedLoad.gloom()) {
            NetherDomainCycle.attach(this, this.carriedLoad, null);
        }
    }

    @Nullable
    EnchantmentSnapshot contactSnapshot() {
        return contactSnapshot;
    }

    /** 领域当前承载的额外附魔。自动生成的攻击只读这份快照，不改领域自己的周期结算。 */
    public EnchantmentSnapshot carriedEnchantments() {
        return DomainSpawnInheritance.offer(contactSnapshot(), definition);
    }

    ResourceEnchantLoad carriedLoad() {
        return carriedLoad == null ? ResourceEnchantLoad.EMPTY : carriedLoad;
    }

    /** 关闭实例并取消未完成轮次。重复关闭直接返回；关闭附属失败仍继续清理接触。 */
    void close() {
        if (!active) {
            return;
        }
        active = false;
        try {
            cancelOpenRounds();
        } finally {
            VolumeMotionRuntime.onClose(this);
            StillWaterLease.release(this);
            NetherDomainCycle.drop(this.instanceId);
            if (contact != null) {
                contact.close();
                contact = null;
            }
        }
    }

    /** 开域时冻结的绑定计划。没有绑定时为空。同一波多份实例共用这一份不可变计划。 */
    @Nullable
    public BoundReleasePlan boundPlan() {
        return boundPlan;
    }

    @Override
    public int nextBindingBeat() {
        return nextBindingBeat;
    }

    @Override
    /** 把绑定节拍推进到给出的下一拍。不补跑错过的轮次，也不扣本轮费用。 */
    public void advanceBindingBeat(int followingBeat) {
        this.nextBindingBeat = followingBeat;
    }

    /**
     * 执行返回后还能不能把新句柄留在本实例上。
     * 领域仍开启、未到到期刻，主人在线、存活，并且仍在宿主维度。
     */
    @Override
    public boolean roundStillHosted(int now) {
        if (!active || level == null || ownerId == null) {
            return false;
        }
        if (ReleaseInput.hasExpired(now, expiresAt)) {
            return false;
        }
        MinecraftServer server = level.getServer();
        if (server == null) {
            return false;
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        return owner != null && owner.isAlive() && owner.level() == level;
    }

    @Override
    /** 留存仍可托管的轮次。空句柄忽略；宿主条件失效时取消未完成句柄。 */
    public void trackRound(RoundHandle handle, int now) {
        if (handle == null) {
            return;
        }
        if (!roundStillHosted(now)) {
            if (!handle.done()) {
                handle.cancel();
            }
            return;
        }
        if (handle.done()) {
            return;
        }
        openRounds.add(handle);
    }

    @Override
    /** 摘掉已经完成的轮次。不取消仍在运行的句柄。 */
    public void sweepFinishedRounds() {
        openRounds.removeIf(RoundHandle::done);
    }

    @Override
    /** 取消全部未完成轮次。单个取消失败先记下来，全部尝试后抛出第一份失败。 */
    public void cancelOpenRounds() {
        if (openRounds.isEmpty()) {
            return;
        }
        ArrayList<RoundHandle> pendingRounds = new ArrayList<>(openRounds);
        openRounds.clear();
        Throwable failure = null;
        for (RoundHandle handle : pendingRounds) {
            if (handle == null || handle.done()) {
                continue;
            }
            try {
                handle.cancel();
            } catch (Throwable thrown) {
                if (failure == null) {
                    failure = thrown;
                }
            }
        }
        if (failure instanceof RuntimeException runtimeFailure) {
            throw runtimeFailure;
        }
        if (failure instanceof Error errorFailure) {
            throw errorFailure;
        }
        if (failure != null) {
            throw new IllegalStateException(failure);
        }
    }

    /** 领取一次移除广播权。已经广播过时返回假，避免重复发包。 */
    boolean markRemovalBroadcast() {
        if (removalBroadcast) {
            return false;
        }
        removalBroadcast = true;
        return true;
    }

    public UUID instanceId() {
        return instanceId;
    }

    public DomainGeometry geometry() {
        return geometry;
    }

    /** 换上非空几何。空几何不替换，已有运动和租约不在这里迁移。 */
    public void adoptGeometry(DomainGeometry next) {
        if (next != null) {
            this.geometry = next;
        }
    }

    /** 记下释放组和声明槽。只保存分组，不改变几何或费用。 */
    public void bindRelease(UUID group, int slot) {
        this.releaseGroup = group;
        this.declarationSlot = slot;
    }

    public UUID releaseGroup() {
        return releaseGroup;
    }

    public int declarationSlot() {
        return declarationSlot;
    }

    public UUID mountedProvider() {
        return mountedProvider;
    }

    /** 记下挂上来的气泡提供者。不校验提供者是否存活。 */
    public void setMountedProvider(UUID providerId) {
        this.mountedProvider = providerId;
    }

    public UUID mountedHost() {
        return mountedHost;
    }

    /** 记下气泡挂住的宿主。不校验宿主是否存活。 */
    public void setMountedHost(UUID hostId) {
        this.mountedHost = hostId;
    }

    /** 同时清掉挂载双方标识。不关闭任一领域。 */
    public void clearMount() {
        this.mountedProvider = null;
        this.mountedHost = null;
    }

    /** 只清掉挂上来的提供者。宿主标识保持不变。 */
    public void clearProvider() {
        this.mountedProvider = null;
    }

    /** 标记挂载已经退役，并清掉宿主标识。不在这里关闭领域。 */
    public void retireMount() {
        this.mountRetired = true;
        this.mountedHost = null;
    }

    public boolean mountRetired() {
        return mountRetired;
    }

    public boolean liftedColumn() {
        return liftedColumn;
    }

    /** 记下旋流柱是否已经抬升。只改状态，不立即改几何高度。 */
    public void setLiftedColumn(boolean lifted) {
        this.liftedColumn = lifted;
    }

    public List<UUID> publishedRoots() {
        return List.copyOf(publishedRoots);
    }

    /** 替换已公布根实体。去重排序后没有变化就返回假，避免重复同步。 */
    public boolean replacePublishedRoots(List<UUID> next) {
        ArrayList<UUID> incoming = new ArrayList<>();
        if (next != null) {
            for (UUID entityId : next) {
                if (entityId != null && !incoming.contains(entityId)) {
                    incoming.add(entityId);
                }
            }
        }
        incoming.sort(Comparator.naturalOrder());
        if (incoming.equals(publishedRoots)) {
            return false;
        }
        publishedRoots.clear();
        publishedRoots.addAll(incoming);
        return true;
    }

    /** 首次记下柱高比例。干高过小退回 1，之后重复调用不改已记比例。 */
    public double captureColumnScale(double dryHeight, double currentHeight) {
        if (columnScale < 0.0D) {
            columnScale = dryHeight <= 1.0E-4D ? 1.0D : currentHeight / dryHeight;
        }
        return columnScale;
    }

    public DomainDefinition definition() {
        return definition;
    }

    public int startedAt() {
        return startedAt;
    }

    public int expiresAt() {
        return expiresAt;
    }

    public float auraDamage() {
        return definition.auraSpec() == null ? 0f : definition.auraSpec().bonusDamage();
    }

    @Override
    public UUID hostId() {
        return instanceId;
    }

    @Override
    public int revision() {
        return revision;
    }

    @Override
    public DomainBoundary boundary() {
        return definition.boundary();
    }

    public DomainVisual visual() {
        return definition.visual();
    }

    public long visualOrigin() {
        return visualOrigin;
    }

    public long visualEnd() {
        return visualEnd;
    }

    public int animSeed() {
        return animSeed;
    }

    @Override
    public UUID ownerId() {
        return ownerId;
    }

    @Override
    public boolean active() {
        return active;
    }

    @Override
    public boolean touches(LivingEntity target) {
        return target != null && geometry.intersects(target.getBoundingBox());
    }

    @Override
    public ServerLevel hostLevel() {
        return level;
    }

    @Override
    public AABB queryBox() {
        return geometry.bounds().inflate(1.0);
    }
}
