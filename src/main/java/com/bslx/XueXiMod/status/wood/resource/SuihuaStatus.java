package com.bslx.XueXiMod.status.wood.resource;

import com.bslx.XueXiMod.shu.technique.release.statusquery.ResourceStatus;
import com.bslx.XueXiMod.shu.technique.release.statusquery.ResourceView;
import com.bslx.XueXiMod.status.wood.resource.client.SuihuaClientCache;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;

/**
 * 只读的 xueximod:suihua。整合作者注册这一份，不要在这里调用 ResourceStatuses.register。
 * value 和 tier 都是 floor(总额/60000)，durationTicks 为 -1。精确六项读 SuihuaSnapshot。
 */
public final class SuihuaStatus implements ResourceStatus {
    public static final ResourceLocation IDENTIFIER = new ResourceLocation("xueximod", "suihua");
    public static final SuihuaStatus PROVIDER = new SuihuaStatus();
    public static final int UNLIMITED_DURATION = -1;

    private SuihuaStatus() {
    }

    @Override
    public ResourceLocation identifier() {
        return IDENTIFIER;
    }

    @Override
    public ResourceView read(LivingEntity subject) {
        if (subject == null || subject.level() == null) {
            return emptyView();
        }
        SuihuaSnapshot snapshot;
        if (subject.level().isClientSide()) {
            snapshot = SuihuaClientCache.read(subject.getUUID());
        } else {
            MinecraftServer server = subject.level().getServer();
            snapshot = WoodResourceService.readSuihua(server, subject.getUUID());
        }
        int degrees = snapshot.degrees();
        return new ResourceView(IDENTIFIER, degrees, degrees, UNLIMITED_DURATION);
    }

    private static ResourceView emptyView() {
        return new ResourceView(IDENTIFIER, 0, 0, UNLIMITED_DURATION);
    }
}
