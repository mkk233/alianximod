package com.bslx.XueXiMod.shu.technique.release.clone.capture;

import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CapturedAttackSnapshot;
import com.bslx.XueXiMod.shu.technique.release.clone.core.DetachedActorFrame;

/**
 * 一种可以交给分身静止体积执行的载荷。
 * 报价不扣款、不生成、不写冷却。实例化不读取施法帧。
 */
public interface ActorPayloadAdapter {
    String techniqueId();

    ShuAttribute attribute();

    int version();

    CaptureQuote freezeQuote(CaptureQuoteContext context);

    ActorVolumeSpec instantiate(CapturedAttackSnapshot payload, DetachedActorFrame actorFrame,
                                CapturedActionAuthority authority);
}
