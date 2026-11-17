package com.bslx.XueXiMod.shu.technique.release.clone.core;

import java.util.UUID;

public record CapturedResponseTicket(
        long sessionToken,
        UUID ownerId,
        UUID rootEventId,
        UUID deploymentId,
        long actionToken,
        DetachedActorFrame actorFrame,
        CapturedAttackSnapshot payload,
        long executeTick,
        long expireTick,
        boolean manualFeedback) {

    public CapturedResponseTicket {
        if (ownerId == null || rootEventId == null || deploymentId == null || actorFrame == null || payload == null) {
            throw new IllegalArgumentException("捕获行动票不完整");
        }
        if (sessionToken == 0L || actionToken <= 0L || executeTick < 0L || expireTick < executeTick) {
            throw new IllegalArgumentException("捕获行动票的时序无效");
        }
        if (sessionToken != actorFrame.sessionToken() || !ownerId.equals(actorFrame.ownerId())
                || !rootEventId.equals(actorFrame.rootEventId()) || !deploymentId.equals(actorFrame.deploymentId())
                || !ownerId.equals(payload.ownerId()) || !rootEventId.equals(payload.rootEventId())) {
            throw new IllegalArgumentException("捕获行动票和帧不一致");
        }
    }
}
