package com.bslx.XueXiMod.shu.technique.release.capture;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneHandle;
import org.jetbrains.annotations.Nullable;

/**
 * 捕获分发的最终结果。成功时计费链只有宿主这一条。
 */
public record CaptureDispatchResult(
        boolean accepted,
        boolean lacksMana,
        @Nullable CaptureRefusal refusal,
        String playerText,
        @Nullable Technique captureTechnique,
        int unscaledCooldownTicks,
        @Nullable CloneHandle handle) {

    public CaptureDispatchResult {
        if (playerText == null) {
            throw new IllegalArgumentException("捕获结果缺少玩家文字");
        }
        if (accepted && (lacksMana || refusal != null || captureTechnique == null || unscaledCooldownTicks < 0)) {
            throw new IllegalArgumentException("已接受的捕获结果不完整");
        }
        if (lacksMana && accepted) {
            throw new IllegalArgumentException("魔力不足不能同时接受");
        }
    }
}
