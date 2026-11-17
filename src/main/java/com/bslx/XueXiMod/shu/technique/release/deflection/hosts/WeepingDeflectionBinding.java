package com.bslx.XueXiMod.shu.technique.release.deflection.hosts;

import com.bslx.XueXiMod.shu.technique.release.WaterSegment;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHosts;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 垂丝偏转的登记口。一个施法实体一个宿主，代次不随拍变化。
 * 服务端在体积算完后调用 {@link #update}；实体移除时调用 {@link #remove}。
 * 空段、死亡、主人不在都会注销。静态表不强持实体和世界，箭仍由原来的捕获路线自己结束。
 */
public final class WeepingDeflectionBinding {
    private static final ConcurrentHashMap<UUID, WeepingDeflectionHost> BOUND = new ConcurrentHashMap<>();

    private WeepingDeflectionBinding() {
    }

    /** 按当前水段登记或更新宿主。不能维持时注销。 */
    public static void update(TechniqueHitEntity entity, WaterSegment segment) {
        if (entity == null || !(entity.level() instanceof ServerLevel serverLevel) || !serverThread(serverLevel)) {
            return;
        }
        UUID entityId = entity.getUUID();
        if (!entity.isAlive() || entity.isRemoved() || !entity.deflectsProjectiles()) {
            release(entityId);
            return;
        }
        WeepingDeflectionHost host = BOUND.get(entityId);
        if (host != null && host.isWithdrawn()) {
            BOUND.remove(entityId, host);
            host = null;
        }
        if (host == null) {
            host = WeepingDeflectionHost.open(entity, serverLevel);
            BOUND.put(entityId, host);
        } else {
            host.attach(entity, serverLevel);
        }
        host.replaceSegment(segment);
        if (!host.active()) {
            release(entityId);
            return;
        }
        DeflectionHosts.register(host);
    }

    /** 实体移除时注销这一面垂丝。 */
    public static void remove(TechniqueHitEntity entity) {
        if (entity == null) {
            return;
        }
        if (entity.level() instanceof ServerLevel serverLevel && !serverThread(serverLevel)) {
            return;
        }
        release(entity.getUUID());
    }

    private static void release(UUID entityId) {
        if (entityId == null) {
            return;
        }
        WeepingDeflectionHost host = BOUND.remove(entityId);
        if (host == null) {
            return;
        }
        host.withdraw();
        ServerLevel serverLevel = host.level();
        if (serverLevel != null && !serverThread(serverLevel)) {
            return;
        }
        DeflectionHosts.unregister(host.key());
    }

    private static boolean serverThread(ServerLevel serverLevel) {
        return serverLevel.getServer() != null && serverLevel.getServer().isSameThread();
    }
}
