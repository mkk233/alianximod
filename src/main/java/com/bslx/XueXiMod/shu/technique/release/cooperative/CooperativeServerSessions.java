package com.bslx.XueXiMod.shu.technique.release.cooperative;

import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneRuntime;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ServerSessionLookup;
import net.minecraft.server.MinecraftServer;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 每个服务器一个协同会话。分身令牌直接读该服务器 {@link CloneRuntime#sessionToken()}，不另造随机数。
 */
public final class CooperativeServerSessions implements ServerSessionLookup {
    private final Map<MinecraftServer, UUID> sessions = new IdentityHashMap<>();

    @Override
    public UUID sessionOf(MinecraftServer server) {
        if (server == null) {
            throw new IllegalArgumentException("协同会话缺少服务器");
        }
        synchronized (sessions) {
            UUID existing = sessions.get(server);
            if (existing != null) {
                return existing;
            }
            UUID created = UUID.randomUUID();
            sessions.put(server, created);
            return created;
        }
    }

    @Override
    public long cloneSessionToken(UUID serverSession) {
        MinecraftServer server = serverFor(serverSession);
        if (server == null) {
            throw new IllegalArgumentException("协同会话没有对应服务器");
        }
        CloneRuntime runtime = CloneRuntime.get(server);
        if (runtime == null) {
            throw new IllegalStateException("协同会话缺少分身运行时");
        }
        return runtime.sessionToken();
    }

    public void forget(MinecraftServer server) {
        if (server == null) {
            return;
        }
        synchronized (sessions) {
            sessions.remove(server);
        }
    }

    private MinecraftServer serverFor(UUID serverSession) {
        if (serverSession == null) {
            return null;
        }
        synchronized (sessions) {
            for (Map.Entry<MinecraftServer, UUID> entry : sessions.entrySet()) {
                if (serverSession.equals(entry.getValue())) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }
}
