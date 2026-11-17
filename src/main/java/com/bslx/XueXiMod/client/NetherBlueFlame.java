package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.release.domain.NetherDomainTint;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * 幽冥范围内的火焰贴图。普通火保留原图；进入已同步的领域体积后改用确认版青蓝图，顶点保持白色。
 * 不改伤害、火种、附魔、耗蓝、形状或帧时序。
 */
public final class NetherBlueFlame {
    public static final String ROOT = "textures/effect/nether_blue_v1/";
    private static final Material SOUL_FIRE_ZERO = new Material(
            TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("block/soul_fire_0"));
    private static final Material SOUL_FIRE_ONE = new Material(
            TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("block/soul_fire_1"));
    private static boolean gloomActive;

    private NetherBlueFlame() {
    }

    public static ResourceLocation sheet(String fileName) {
        return new ResourceLocation(XueXiMod.MOD_ID, ROOT + fileName);
    }

    public static Material soulFireZero() {
        return SOUL_FIRE_ZERO;
    }

    public static Material soulFireOne() {
        return SOUL_FIRE_ONE;
    }

    public static boolean open(Level level, double worldX, double worldY, double worldZ) {
        boolean previous = gloomActive;
        gloomActive = NetherDomainTint.gloom(level, worldX, worldY, worldZ);
        return previous;
    }

    public static void close(boolean previous) {
        gloomActive = previous;
    }

    public static ResourceLocation choose(Level level, double worldX, double worldY, double worldZ,
                                          ResourceLocation ordinary, ResourceLocation blue) {
        if (blue != null && NetherDomainTint.gloom(level, worldX, worldY, worldZ)) {
            return blue;
        }
        return ordinary;
    }

    public static ResourceLocation current(ResourceLocation ordinary, ResourceLocation blue) {
        if (blue != null && gloomActive) {
            return blue;
        }
        return ordinary;
    }

    public static void setCurrent(boolean active) {
        gloomActive = active;
    }

    public static boolean current() {
        return gloomActive;
    }

    public static boolean covers(Level level, Entity entity, float partialTick) {
        if (level == null || entity == null) {
            return false;
        }
        double worldX = Mth.lerp(partialTick, entity.xo, entity.getX());
        double worldY = Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getBbHeight() * 0.5D;
        double worldZ = Mth.lerp(partialTick, entity.zo, entity.getZ());
        return NetherDomainTint.gloom(level, worldX, worldY, worldZ);
    }

    public static ParticleOptions flameParticle(Level level, double worldX, double worldY, double worldZ,
                                                ParticleOptions particleOptions) {
        if (particleOptions != null && particleOptions.getType() == ParticleTypes.FLAME
                && NetherDomainTint.gloom(level, worldX, worldY, worldZ)) {
            return ParticleTypes.SOUL_FIRE_FLAME;
        }
        return particleOptions;
    }
}
