package com.bslx.XueXiMod.shu.technique.release.clone.core;

import com.bslx.XueXiMod.shu.technique.release.cooperative.api.AcceptedReposition;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CancelReason;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.FamilyKey;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** 挂在原响应槽上的协同占用。提交标记只由分身核心写入，调用方不能把它改回未提交。 */
final class CloneCooperationHold {
    final UUID token;
    final FamilyKey family;
    final long bodyGeneration;
    final long expiryTick;
    final long openedTick;
    final int prepareTicks;
    final Vec3 sharedAim;
    final Vec3 frozenForward;
    final boolean rightStance;
    final double sideOffset;
    @Nullable
    AcceptedReposition reposition;
    final Set<Integer> committedMembers = new LinkedHashSet<>();
    boolean committed;
    boolean arrived;
    boolean closed;
    int advanceCount;
    @Nullable
    CancelReason failure;

    CloneCooperationHold(UUID token, FamilyKey family, long bodyGeneration, long expiryTick, long openedTick,
                         int prepareTicks, Vec3 sharedAim, Vec3 frozenForward, boolean rightStance,
                         double sideOffset) {
        this.token = token;
        this.family = family;
        this.bodyGeneration = bodyGeneration;
        this.expiryTick = expiryTick;
        this.openedTick = openedTick;
        this.prepareTicks = prepareTicks;
        this.sharedAim = sharedAim;
        this.frozenForward = frozenForward;
        this.rightStance = rightStance;
        this.sideOffset = sideOffset;
    }
}
