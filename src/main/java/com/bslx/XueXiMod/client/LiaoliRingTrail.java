package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.client.NetherBlueFlame;
import com.bslx.XueXiMod.network.LiaoliRingBridge;
import com.bslx.XueXiMod.network.LiaoliRingPacket;
import com.bslx.XueXiMod.visual.liaoli.LiaoliV6Layout;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * 燎里后环的客户端尾迹。命中体收招后仍把已经触发的三道环播完。
 * 只跟施法者插值眼睛和视线偏航，不保存世界坐标，也不产生伤害。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, value = Dist.CLIENT)
public final class LiaoliRingTrail {
    private static final int WAVE_LIMIT = 12;
    private static final int ABSENT_LIMIT = 10;
    private static final List<RingWave> WAVES = new ArrayList<>();

    private LiaoliRingTrail() {
    }

    public static void bind() {
        LiaoliRingBridge.bind(LiaoliRingTrail::accept);
    }

    public static void remember(UUID casterId, long birthTick) {
        if (casterId == null || birthTick < 0L) {
            return;
        }
        int waveIndex = 0;
        while (waveIndex < WAVES.size()) {
            RingWave wave = WAVES.get(waveIndex);
            if (birthTick == wave.birthTick && casterId.equals(wave.casterId)) {
                return;
            }
            waveIndex += 1;
        }
        if (WAVES.size() >= WAVE_LIMIT) {
            WAVES.remove(0);
        }
        WAVES.add(new RingWave(casterId, birthTick));
    }

    private static void accept(LiaoliRingPacket packet) {
        if (packet == null) {
            return;
        }
        remember(packet.casterId(), packet.birthTick());
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || WAVES.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel clientLevel = minecraft.level;
        if (clientLevel == null) {
            WAVES.clear();
            return;
        }
        float partialTick = event.getPartialTick();
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        boolean drewOrdinary = false;
        boolean drewBlue = false;
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        Iterator<RingWave> waves = WAVES.iterator();
        while (waves.hasNext()) {
            RingWave wave = waves.next();
            double age = clientLevel.getGameTime() + partialTick - wave.birthTick;
            if (age < -40.0d || age >= LiaoliV6Layout.ringShowTicks()) {
                waves.remove();
                continue;
            }
            Player caster = clientLevel.getPlayerByUUID(wave.casterId);
            if (caster == null) {
                wave.absentTicks += 1;
                if (wave.absentTicks > ABSENT_LIMIT) {
                    waves.remove();
                }
                continue;
            }
            if (!caster.isAlive() || caster.isRemoved()) {
                waves.remove();
                continue;
            }
            wave.absentTicks = 0;
            if (age < 0.0d) {
                continue;
            }
            Vec3 eye = caster.getEyePosition(partialTick);
            float lookYaw = Mth.rotLerp(partialTick, caster.yRotO, caster.getYRot()) * ((float) Math.PI / 180.0F);
            pose.pushPose();
            pose.translate(eye.x, eye.y, eye.z);
            Matrix4f matrix = pose.last().pose();
            boolean previousGloom = NetherBlueFlame.open(clientLevel, eye.x, eye.y, eye.z);
            try {
                LiaoliV6Renderer.paintRearRings(buffers, matrix, lookYaw, age);
                if (NetherBlueFlame.current()) {
                    drewBlue = true;
                } else {
                    drewOrdinary = true;
                }
            } finally {
                NetherBlueFlame.close(previousGloom);
            }
            pose.popPose();
        }
        pose.popPose();
        if (drewOrdinary) {
            buffers.endBatch(RenderType.entityTranslucentEmissive(
                    new ResourceLocation(XueXiMod.MOD_ID, LiaoliV6Layout.REAR_RING)));
        }
        if (drewBlue) {
            buffers.endBatch(RenderType.entityTranslucentEmissive(NetherBlueFlame.sheet("liaoli_rear_ring.png")));
        }
    }

    @SubscribeEvent
    public static void onPlayerLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel() == null || !event.getLevel().isClientSide() || !(event.getEntity() instanceof Player player)) {
            return;
        }
        UUID casterId = player.getUUID();
        WAVES.removeIf(wave -> casterId.equals(wave.casterId));
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        WAVES.clear();
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() != null && event.getLevel().isClientSide()) {
            WAVES.clear();
        }
    }

    private static final class RingWave {
        private final UUID casterId;
        private final long birthTick;
        private int absentTicks;

        private RingWave(UUID casterId, long birthTick) {
            this.casterId = casterId;
            this.birthTick = birthTick;
        }
    }
}
