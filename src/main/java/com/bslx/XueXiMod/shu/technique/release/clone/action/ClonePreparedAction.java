package com.bslx.XueXiMod.shu.technique.release.clone.action;

import com.bslx.XueXiMod.shu.balance.ConfiguredDamage;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneResults;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 一次准备结果。报价和目标在准备时冻结。
 * 未接受时伤害、目标为空，魔力和冷却为 0。提交最多一次。
 */
public interface ClonePreparedAction {
    boolean accepted();

    @Nullable
    CloneResults.RejectReason reason();

    String detail();

    @Nullable
    ConfiguredDamage configuredDamage();

    int quotedMana();

    int quotedCooldownTicks();

    @Nullable
    UUID targetId();

    CloneActionResult commit();

    void cancelBeforeCommit();
}
