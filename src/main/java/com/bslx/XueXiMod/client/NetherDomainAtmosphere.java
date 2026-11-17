package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.release.domain.NetherDomainSight;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/**
 * 只在当前摄像机落在真实幽冥切片内时，把天空色和远景雾混向魂谷色。
 * 离开、到期、换维或退出世界时权重回到零，不写死正常天空颜色。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, value = Dist.CLIENT)
public final class NetherDomainAtmosphere {
    private static final int TOP_RED = 0x08;
    private static final int TOP_GREEN = 0x19;
    private static final int TOP_BLUE = 0x23;
    private static final int HIGH_RED = 0x10;
    private static final int HIGH_GREEN = 0x2C;
    private static final int HIGH_BLUE = 0x3B;
    private static final int HORIZON_RED = 0x41;
    private static final int HORIZON_GREEN = 0x6E;
    private static final int HORIZON_BLUE = 0x77;
    private static final int SKY_RED = TOP_RED;
    private static final int SKY_GREEN = TOP_GREEN;
    private static final int SKY_BLUE = TOP_BLUE;
    private static final float ENTER_SECONDS = 0.6f;
    private static final float EXIT_SECONDS = 0.8f;

    private static float weight;
    private static float previousWeight;
    private static boolean inside;
    private static String levelKey = "";
    private static boolean levelReady;

    private NetherDomainAtmosphere() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        String currentKey = minecraft.level.dimension().location().toString();
        if (!levelReady || !currentKey.equals(levelKey)) {
            levelKey = currentKey;
            levelReady = true;
            inside = false;
            weight = 0.0f;
            previousWeight = 0.0f;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        boolean covered = camera.getFluidInCamera() == FogType.NONE
                && NetherDomainSight.probe(minecraft.level, camera.getPosition().x,
                camera.getPosition().y, camera.getPosition().z).gloom();
        float step = covered ? 1.0f / (ENTER_SECONDS * 20.0f) : 1.0f / (EXIT_SECONDS * 20.0f);
        inside = covered;
        previousWeight = weight;
        weight = Mth.clamp(weight + (covered ? step : -step), 0.0f, 1.0f);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float blend = blend(event.getCamera());
        if (blend <= 0.0f || event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        float pitch = event.getCamera().getXRot();
        float high = Mth.clamp((-pitch - 8.0f) / 52.0f, 0.0f, 1.0f);
        float red = Mth.lerp(high, HORIZON_RED, HIGH_RED) / 255.0f;
        float green = Mth.lerp(high, HORIZON_GREEN, HIGH_GREEN) / 255.0f;
        float blue = Mth.lerp(high, HORIZON_BLUE, HIGH_BLUE) / 255.0f;
        event.setRed(Mth.lerp(blend, event.getRed(), red));
        event.setGreen(Mth.lerp(blend, event.getGreen(), green));
        event.setBlue(Mth.lerp(blend, event.getBlue(), blue));
    }

    public static Vec3 sky(Vec3 original) {
        if (original == null) {
            return original;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameRenderer.getMainCamera().getFluidInCamera() != FogType.NONE) {
            return original;
        }
        float blend = displayed();
        if (blend <= 0.0f) {
            return original;
        }
        return new Vec3(
                Mth.lerp(blend, original.x, SKY_RED / 255.0d),
                Mth.lerp(blend, original.y, SKY_GREEN / 255.0d),
                Mth.lerp(blend, original.z, SKY_BLUE / 255.0d));
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() != null && event.getLevel().isClientSide()) {
            clear();
        }
    }

    private static float blend(Camera camera) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!levelReady || minecraft.level == null || camera == null
                || camera.getFluidInCamera() != FogType.NONE) {
            return 0.0f;
        }
        if (!levelKey.equals(minecraft.level.dimension().location().toString())) {
            return 0.0f;
        }
        return displayed();
    }

    private static float displayed() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!levelReady || minecraft.level == null
                || !levelKey.equals(minecraft.level.dimension().location().toString())) {
            return 0.0f;
        }
        float partial = minecraft.getFrameTime();
        return smooth(Mth.lerp(partial, previousWeight, weight));
    }

    private static float smooth(float value) {
        return value * value * (3.0f - 2.0f * value);
    }

    private static void clear() {
        weight = 0.0f;
        previousWeight = 0.0f;
        inside = false;
        levelKey = "";
        levelReady = false;
    }
}
