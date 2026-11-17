package com.bslx.XueXiMod.shu.technique.release.entity;

import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.momentum.HeavyWater;
import com.bslx.XueXiMod.shu.technique.release.payload.HitPayloads;
import com.bslx.XueXiMod.shu.technique.release.payload.WetPressure;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBandSnapshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 一次宿主准备出来的冻结候选。组内复制份都读这一份，不各自再索敌。 */
public final class HostGroupContext {
    private static final ThreadLocal<HostGroupContext> CURRENT = new ThreadLocal<>();

    private final HostCast hostCast;
    private final UUID groupId;
    private final int requiredSpawns;
    private final int spawnCount;
    private final HostGroupListener listener;
    private final List<TechniqueHitEntity> published = new ArrayList<>();
    private Vec3 frozenLook;
    private boolean failed;
    private boolean casterNoted;
    private ServerPlayer caster;
    private float healAmount;
    private boolean healCaster;
    private HeavyWater.Session heavySession;
    private HostGroupReceipt receipt;
    private boolean beatArmed;
    private int beatCandidate;
    private ResourceLocation actionCandidate;

    private HostGroupContext(HostCast hostCast, UUID groupId, int requiredSpawns, int spawnCount, HostGroupListener listener) {
        this.hostCast = hostCast;
        this.groupId = groupId;
        this.requiredSpawns = requiredSpawns;
        this.spawnCount = spawnCount;
        this.listener = listener;
    }

    /** 准备一份还没绑定的冻结候选。 */
    public static HostGroupContext open(HostCast hostCast, UUID groupId, int requiredSpawns, int spawnCount, HostGroupListener listener) {
        return new HostGroupContext(hostCast, groupId, requiredSpawns, spawnCount, listener);
    }

    public static HostGroupContext current() {
        return CURRENT.get();
    }

    /** 绑到当前线程。空上下文表示清掉。 */
    public static void bind(HostGroupContext context) {
        if (context == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(context);
        }
    }

    /** 只解开当前这一份，避免误弹外层。 */
    public static void unbind(HostGroupContext context) {
        if (CURRENT.get() == context) {
            CURRENT.remove();
        }
    }

    public UUID groupId() {
        return groupId;
    }

    public HostCast hostCast() {
        return hostCast;
    }

    public boolean hasBeatCandidate() {
        return beatArmed;
    }

    public int beatCandidate() {
        return beatCandidate;
    }

    /** 同一组只准备一次。复制份都读这一拍，这里不写账。 */
    public void armBeat(int beat) {
        if (beatArmed) {
            return;
        }
        beatArmed = true;
        beatCandidate = Math.floorMod(beat, 3);
    }

    public void clearBeat() {
        beatArmed = false;
        beatCandidate = 0;
    }

    /** 同组重复登记幂等，先到先得。 */
    public void registerAction(ResourceLocation actionId) {
        if (actionCandidate == null && actionId != null) {
            actionCandidate = actionId;
        }
    }

    public ResourceLocation actionCandidate() {
        return actionCandidate;
    }

    /** 已暂存实体里的第一份弧带快照。 */
    public ArcBandSnapshot committedArc() {
        for (TechniqueHitEntity entity : published) {
            ArcBandSnapshot snapshot = entity.arcBand();
            if (snapshot != null) {
                return snapshot;
            }
        }
        return null;
    }

    public int spawnCount() {
        return spawnCount;
    }

    public boolean hasFrozenLook() {
        return frozenLook != null;
    }

    public Vec3 frozenLook() {
        return frozenLook;
    }

    /** 第一次有效视线才冻结，组内复制不再改。 */
    public void freezeLook(Vec3 look) {
        if (frozenLook == null && look != null && look.lengthSqr() > 1.0E-8) {
            frozenLook = look.normalize();
        }
    }

    /** 标成本组不能提交。 */
    public void fail() {
        failed = true;
    }

    public boolean failed() {
        return failed;
    }

    public void stagePublished(TechniqueHitEntity entity) {
        if (entity != null) {
            published.add(entity);
        }
    }

    /** 把施法者治疗请求记一次。 */
    public void noteCaster(ServerPlayer player, CastFrame frame) {
        if (casterNoted || player == null || frame == null) {
            return;
        }
        casterNoted = true;
        caster = player;
        healAmount = frame.healAmount;
        healCaster = frame.healCaster;
    }

    /** 留下这组共用的重水会话。 */
    public void keepHeavy(HeavyWater.Session session) {
        if (session != null) {
            heavySession = session;
        }
    }

    /** 份数不齐、已标失败、父释放结束或施法者离开时丢弃已生成实体。 */
    public HostGroupReceipt seal(ServerPlayer player, int tick, boolean ownerLeft) {
        if (receipt != null) {
            return receipt;
        }
        boolean parentClosed = hostCast != null && hostCast.closed();
        boolean accepted = !ownerLeft && !parentClosed && !failed && published.size() == requiredSpawns && requiredSpawns > 0;
        if (!accepted) {
            discardPublished();
            finishHeavy(player, false);
            if (ownerLeft) {
                receipt = HostGroupReceipt.cancelled(groupId, tick, "施法者已经离开");
            } else if (parentClosed) {
                receipt = HostGroupReceipt.cancelled(groupId, tick, "宿主组已经结束");
            } else {
                receipt = HostGroupReceipt.rejected(groupId, tick, "宿主组没有完整生成");
            }
            return receipt;
        }
        for (TechniqueHitEntity entity : published) {
            entity.releaseActivation();
        }
        finishHeavy(player, true);
        payCaster();
        receipt = HostGroupReceipt.committed(groupId, tick);
        return receipt;
    }

    public HostGroupListener listener() {
        return listener;
    }

    private void payCaster() {
        if (!healCaster || healAmount <= 0f || caster == null) {
            return;
        }
        CastFrame healFrame = new CastFrame();
        healFrame.healAmount = healAmount;
        healFrame.healCaster = true;
        HitPayloads.applyCaster(caster, healFrame);
    }

    private void finishHeavy(ServerPlayer player, boolean committed) {
        if (heavySession == null) {
            return;
        }
        if (committed) {
            HeavyWater.commitFirst(player);
        }
        HeavyWater.finish(heavySession);
        HeavyWater.clearCurrent();
        heavySession = null;
    }

    private void discardPublished() {
        for (TechniqueHitEntity entity : published) {
            HitPayloads.clear(entity);
            WetPressure.clear(entity);
            if (!entity.isRemoved()) {
                entity.discard();
            }
        }
        published.clear();
    }
}
