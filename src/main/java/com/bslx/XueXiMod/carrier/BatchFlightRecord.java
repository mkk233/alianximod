package com.bslx.XueXiMod.carrier;

import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad;
import com.bslx.XueXiMod.shu.technique.release.batch.BatchFollow;
import com.bslx.XueXiMod.shu.technique.release.batch.HitClock;
import com.bslx.XueXiMod.shu.technique.release.batch.ImpactPartition;
import com.bslx.XueXiMod.shu.technique.release.batch.OcclusionSnapshot;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** 一颗已经出生的火苗或陨岩。留焰和结算都读这里，不从距离反推。 */
public final class BatchFlightRecord {
    public String techniqueKey = "";
    public TechniqueHitEntity body;
    public HitClock hitClock;
    public UUID rootCastId;
    public UUID ownerIdentifier;
    public boolean resourceCommit;
    public ResourceEnchantLoad resourceLoad = ResourceEnchantLoad.EMPTY;
    public int bodyScaledBase;
    public int legacyFixedPoints;
    public boolean breakGround;
    public float damage;
    public TideMarkSuccess tide = TideMarkSuccess.NONE;
    public ImpactPartition partition = ImpactPartition.none();
    public OcclusionSnapshot occlusion;
    public BatchFollow follow;
    public Vec3 facing = Vec3.ZERO;
    public boolean contacted;
    public boolean expanded;
    public boolean landed;
    /** 实体结算已经做过，飞行体还要继续到地形。 */
    public boolean flyingAfterEntity;
    public boolean followSent;
    public boolean followOwnedByCaller;
    /** 交给 A 分类、尚未成为合法命中的候选。取消后不会用它扩圈。 */
    public UUID openStrike;
    /** A 已经给出可接续裁决后才允许中心圈。等待和取消不会把它设上。 */
    public boolean strikeConfirmed;
    /** 已经取消或无效、不能再进中心圈的候选。 */
    public Set<UUID> declinedStrikes = new HashSet<>();
    public Consumer<Vec3> trigger;
    public AtomicBoolean triggerGate = new AtomicBoolean(false);

    public void fireTrigger(Vec3 point) {
        if (this.trigger != null && point != null && this.triggerGate.compareAndSet(false, true)) {
            this.trigger.accept(point);
        }
    }
}
