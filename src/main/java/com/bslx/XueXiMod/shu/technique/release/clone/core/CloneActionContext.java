package com.bslx.XueXiMod.shu.technique.release.clone.core;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public record CloneActionContext(
        ServerPlayer owner,
        CloneActorFrame actorFrame,
        UUID rootEventId,
        long actionToken,
        CancelToken cancelToken) {

    public interface CancelToken {
        boolean cancelled();
    }

    public CloneActionContext {
        if (owner == null || actorFrame == null || rootEventId == null || cancelToken == null) {
            throw new IllegalArgumentException("分身动作上下文不完整");
        }
    }
}
