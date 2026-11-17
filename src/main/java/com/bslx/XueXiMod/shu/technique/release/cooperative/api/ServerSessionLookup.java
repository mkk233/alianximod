package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/**
 * 协同会话 UUID 和分身运行时 long 会话令牌的唯一对照。
 * 消费者不要自己把其中一个哈希成另一个。
 */
public interface ServerSessionLookup {
    UUID sessionOf(MinecraftServer server);

    long cloneSessionToken(UUID serverSession);
}
