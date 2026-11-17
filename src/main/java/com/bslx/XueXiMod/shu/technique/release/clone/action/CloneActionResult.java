package com.bslx.XueXiMod.shu.technique.release.clone.action;

import com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict;
import org.jetbrains.annotations.Nullable;

/**
 * 一次已经进入提交的脉冲结果。裁决只供记录，不决定退款或恢复次数。
 */
public record CloneActionResult(@Nullable HitVerdict verdict, boolean damageCalled) {
}
