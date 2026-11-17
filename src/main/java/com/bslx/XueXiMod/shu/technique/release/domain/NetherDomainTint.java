package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.shu.technique.enchant.resource.NetherContract;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * 火系表现查询幽冥色调的唯一入口。判断依据是已同步或仍存活的领域范围，不看本地玩家离谁更近。
 * 青蓝火焰改用确认版贴图和白色顶点，不再把这里的 90,160,255 乘到火焰上。
 */
public final class NetherDomainTint {
    public static final int GLOOM_RED = 90;
    public static final int GLOOM_GREEN = 160;
    public static final int GLOOM_BLUE = 255;

    private static ClientProbe clientProbe = (level, worldX, worldY, worldZ) -> Sample.plain();

    private NetherDomainTint() {
    }

    public interface ClientProbe {
        Sample probe(Level level, double worldX, double worldY, double worldZ);
    }

    public static void bindClient(ClientProbe probe) {
        clientProbe = probe == null ? (level, worldX, worldY, worldZ) -> Sample.plain() : probe;
    }

    public static Sample sample(Level level, double worldX, double worldY, double worldZ) {
        if (level == null || !Double.isFinite(worldX) || !Double.isFinite(worldY) || !Double.isFinite(worldZ)) {
            return Sample.plain();
        }
        if (level.isClientSide()) {
            return clientProbe.probe(level, worldX, worldY, worldZ);
        }
        if (level instanceof ServerLevel serverLevel && NetherDomainCycle.contains(serverLevel, worldX, worldY, worldZ)) {
            return Sample.gloomTint();
        }
        return Sample.plain();
    }

    public static boolean gloom(Level level, double worldX, double worldY, double worldZ) {
        return sample(level, worldX, worldY, worldZ).gloom();
    }

    public record Sample(boolean gloom, int red, int green, int blue, String tintKey) {
        public static Sample plain() {
            return new Sample(false, 255, 255, 255, "");
        }

        public static Sample gloomTint() {
            return new Sample(true, GLOOM_RED, GLOOM_GREEN, GLOOM_BLUE, NetherContract.VISUAL_TINT);
        }

        /** 已换成青蓝贴图的火焰。标记仍在领域内，但顶点保持白色，避免二次乘色。 */
        public static Sample soulWhite() {
            return new Sample(true, 255, 255, 255, NetherContract.VISUAL_TINT);
        }
    }
}
