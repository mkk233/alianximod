package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.client.SurfaceSnapshotAssembly;
import com.bslx.XueXiMod.network.FireVisualBridge;
import com.bslx.XueXiMod.network.FireVisualPacket;
import com.bslx.XueXiMod.shu.technique.release.firecast.GroundFireFields;
import com.bslx.XueXiMod.shu.technique.release.firecast.SurfaceSnapshotFrames;
import com.bslx.XueXiMod.shu.technique.release.firecast.GroundFireMath;
import com.bslx.XueXiMod.shu.technique.release.firecast.StandSurface;
import com.bslx.XueXiMod.client.furnace.FurnaceBodyVisual;
import com.bslx.XueXiMod.visual.fire.FireVisualLayout;
import com.bslx.XueXiMod.visual.facing.CameraFacingEffectId;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 燎原火场、焚天和炉心冷却蒸汽仍在这里绘制。引燃只保留已同步轨道，交给原版实体火焰显示，不产生伤害。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, value = Dist.CLIENT)
public final class FireVisualClient {
    private static final long POSE_GAP = 40L;
    private static final double SURFACE_LIFT = 0.04d;
    private static final ResourceLocation GROUND_FLAME = sheet(FireVisualLayout.GROUND_FLAME);
    private static final ResourceLocation GROUND_EMBERS = sheet(FireVisualLayout.GROUND_EMBERS);
    private static final ResourceLocation SPREAD_WAVE = sheet(FireVisualLayout.SPREAD_WAVE);
    private static final ResourceLocation[] ASH_SHEETS = new ResourceLocation[] {
            sheet(FireVisualLayout.GROUND_ASH),
            sheet(FireVisualLayout.GROUND_ASH_B),
            sheet(FireVisualLayout.GROUND_ASH_C),
            sheet(FireVisualLayout.GROUND_ASH_D)
    };
    private static final ResourceLocation GROUND_ASH_EMBERS = sheet(FireVisualLayout.GROUND_ASH_EMBERS);
    private static final ResourceLocation SKY_BACKDROP = sheet(FireVisualLayout.CONFLAGRATION);
    private static final ResourceLocation BODY_FLAME = sheet(FireVisualLayout.BODY_FLAME);
    private static final ResourceLocation BLUE_GROUND_FLAME = NetherBlueFlame.sheet("liaoyuan_ground_flame.png");
    private static final ResourceLocation BLUE_GROUND_EMBERS = NetherBlueFlame.sheet("liaoyuan_ground_embers.png");
    private static final ResourceLocation BLUE_SPREAD_WAVE = NetherBlueFlame.sheet("liaoyuan_spread_wave.png");
    private static final ResourceLocation BLUE_ASH_EMBERS = NetherBlueFlame.sheet("liaoyuan_v3_ground_embers.png");
    /** 引燃 v4 与焚天背景共用 128×144×52 的确认版。历史文件名仍是 fentian_conflagration。 */
    private static final ResourceLocation BLUE_KINDLE = NetherBlueFlame.sheet("fentian_conflagration.png");
    /** 焚天全身烈焰 v3。历史文件名仍是 kindle_v3_screenshot，不能按名字退回引燃。 */
    private static final ResourceLocation BLUE_BODY = NetherBlueFlame.sheet("kindle_v3_screenshot.png");

    private static final Map<Long, GroundView> GROUNDS = new HashMap<>();
    private static final Map<Long, SurfaceView> SURFACES = new HashMap<>();
    private static final Map<Long, BurnView> BURNS = new HashMap<>();
    private static final Map<Integer, SkyEmberView> SKY_EMBERS = new HashMap<>();
    private static final Map<Integer, BodyPulseView> BODY_PULSES = new HashMap<>();
    private static final Map<Integer, BodyEndView> BODY_ENDS = new HashMap<>();

    private FireVisualClient() {
    }

    public static void bind() {
        FireVisualBridge.bind(FireVisualClient::accept);
        FireSeedFlameClient.bind();
        LiaoliRingTrail.bind();
    }

    public static boolean showsKindleFlame(Entity entity) {
        if (entity == null || entity.isSpectator() || !entity.isAlive() || entity.isRemoved()) {
            return false;
        }
        BurnView view = BURNS.get(FireVisualLayout.burnToken(entity.getId(), FireVisualLayout.KINDLE));
        if (view == null) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }
        return minecraft.level.getGameTime() - view.seenGameTime <= POSE_GAP;
    }

    public static void accept(FireVisualPacket packet) {
        if (packet == null) {
            return;
        }
        long seen = seenNow();
        if (packet.kind() == FireVisualPacket.GROUND) {
            long birth = packet.birthTick();
            long expire = packet.expireTick();
            GroundView kept = GROUNDS.get(packet.zoneId());
            if (kept != null) {
                birth = kept.birthTick;
                expire = kept.expireTick;
            } else {
                long span = expire - birth;
                if (span < 1L) {
                    span = 1L;
                }
                long elapsed = seen - birth;
                if (seen > 0L && (elapsed > span || birth > seen + 5L)) {
                    birth = seen;
                    expire = seen + span;
                }
            }
            GROUNDS.put(packet.zoneId(), new GroundView(packet.dimensionId(), packet.originX(), packet.originY(),
                    packet.originZ(), packet.radius(), birth, expire));
            return;
        }
        if (packet.kind() == FireVisualPacket.GROUND_END) {
            GROUNDS.remove(packet.zoneId());
            return;
        }
        if (packet.kind() == FireVisualPacket.BURN) {
            int entityId = packet.entityId();
            int burnKind = packet.burnKind();
            long token = FireVisualLayout.burnToken(entityId, burnKind);
            BurnView kept = BURNS.get(token);
            if (burnKind == FireVisualLayout.SKY) {
                BODY_ENDS.remove(entityId);
                SKY_EMBERS.remove(entityId);
                BodyPulseView shownPulse = BODY_PULSES.get(entityId);
                if (shownPulse != null && shownPulse.terminal) {
                    BODY_PULSES.remove(entityId);
                }
                if (kept != null && kept.cursor < packet.cursor()) {
                    BODY_PULSES.put(entityId, new BodyPulseView(seen, kept.cursor, false));
                }
            }
            BURNS.put(token, new BurnView(entityId, burnKind, packet.tiers(), packet.remainder(),
                    packet.segmentCount(), packet.interval(), packet.cursor(), packet.waitTicks(), seen));
            return;
        }
        if (packet.kind() == FireVisualPacket.BURN_END) {
            int entityId = packet.entityId();
            int burnKind = packet.burnKind();
            BurnView ending = BURNS.remove(FireVisualLayout.burnToken(entityId, burnKind));
            if (burnKind == FireVisualLayout.SKY) {
                if (packet.naturalEnd() && packet.segmentCount() > 0) {
                    int heldIndex = FireVisualLayout.SKY_HELD_FALLBACK;
                    double heldScale = 1.0d;
                    if (ending != null) {
                        heldIndex = FireVisualLayout.skyHeldIndex(ending.interval, ending.cursor, ending.wait);
                        heldScale = FireVisualLayout.skyScale(ending.segmentCount, ending.cursor, ending.remainder);
                    }
                    BODY_PULSES.put(entityId, new BodyPulseView(seen, 0, true));
                    BODY_ENDS.put(entityId, new BodyEndView(seen + FireVisualLayout.BODY_PULSE_FRAMES));
                    SKY_EMBERS.put(entityId, new SkyEmberView(heldIndex, heldScale, seen));
                } else {
                    BODY_PULSES.remove(entityId);
                    BODY_ENDS.remove(entityId);
                    SKY_EMBERS.remove(entityId);
                }
                return;
            }
            return;
        }
        if (packet.kind() == FireVisualPacket.FURNACE) {
            FurnaceBodyVisual.noteCue(packet.entityId(), packet.furnaceCue());
            return;
        }
        if (packet.kind() == FireVisualPacket.SURFACE) {
            acceptSurface(packet, seen);
            return;
        }
        if (packet.kind() == FireVisualPacket.SURFACE_END) {
            acceptSurfaceEnd(packet, seen);
            return;
        }
        if (packet.kind() == FireVisualPacket.SPARK) {
            spark(packet);
        }
    }

    private static void acceptSurface(FireVisualPacket packet, long seen) {
        SurfaceView kept = SURFACES.get(packet.zoneId());
        int shownVersion = 0;
        if (kept != null && packet.dimensionId() != null && packet.dimensionId().equals(kept.dimensionId)) {
            shownVersion = kept.version;
        }
        SurfaceSnapshotAssembly.Completed ready = SurfaceSnapshotAssembly.accept(packet, seen, shownVersion);
        if (ready == null) {
            return;
        }
        kept = SURFACES.get(ready.hostId);
        if (kept != null) {
            if (!kept.dimensionId.equals(ready.dimensionId) || ready.version <= kept.version) {
                return;
            }
            kept.slices = ready.slices;
            kept.version = ready.version;
            return;
        }
        long birth = ready.birthTick;
        long expire = ready.expireTick;
        long span = expire - birth;
        if (span < 1L) {
            span = 1L;
        }
        long elapsed = seen - birth;
        if (seen > 0L && elapsed > span) {
            return;
        }
        if (seen > 0L && birth > seen + 5L) {
            birth = seen;
            expire = seen + span;
        }
        SURFACES.put(ready.hostId, new SurfaceView(ready.dimensionId, ready.originX, ready.originY, ready.originZ,
                birth, expire, ready.style, ready.version, ready.slices));
    }

    private static void acceptSurfaceEnd(FireVisualPacket packet, long seen) {
        long hostId = packet.zoneId();
        long expire = packet.expireTick();
        String dimensionId = packet.dimensionId() == null ? "" : packet.dimensionId();
        SurfaceView kept = SURFACES.get(hostId);
        if (expire <= packet.birthTick()) {
            if (kept != null) {
                expire = kept.expireTick;
                if (dimensionId.isEmpty()) {
                    dimensionId = kept.dimensionId;
                }
            } else {
                long pendingExpire = SurfaceSnapshotAssembly.pendingExpire(hostId);
                if (pendingExpire > packet.birthTick()) {
                    expire = pendingExpire;
                }
                if (dimensionId.isEmpty()) {
                    dimensionId = SurfaceSnapshotAssembly.pendingDimension(hostId);
                }
            }
        }
        if (expire <= 0L) {
            expire = seen + SurfaceSnapshotFrames.EXPECTED_LIFE;
        }
        SurfaceSnapshotAssembly.end(hostId, dimensionId, expire);
        SURFACES.remove(hostId);
    }

    /** 仍请求普通火焰粒子。幽冥内由统一入口换成原版魂火，不在这里按术式分叉。 */
    private static ParticleOptions sparkParticle(int style) {
        return ParticleTypes.FLAME;
    }

    private static void spark(FireVisualPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel clientLevel = minecraft.level;
        if (clientLevel == null) {
            return;
        }
        Entity entity = clientLevel.getEntity(packet.entityId());
        if (entity == null) {
            return;
        }
        Vec3 target = entity.getBoundingBox().getCenter();
        double fromX = packet.originX();
        double fromY = packet.originY();
        double fromZ = packet.originZ();
        double travelX = target.x - fromX;
        double travelY = target.y - fromY;
        double travelZ = target.z - fromZ;
        ParticleOptions particle = sparkParticle(packet.furnaceCue());
        int burst = 0;
        while (burst < 8) {
            double jitterX = (clientLevel.random.nextDouble() - 0.5d) * 0.2d;
            double jitterY = (clientLevel.random.nextDouble() - 0.5d) * 0.2d;
            double jitterZ = (clientLevel.random.nextDouble() - 0.5d) * 0.2d;
            clientLevel.addParticle(particle, fromX, fromY, fromZ,
                    travelX * 0.08d + jitterX, travelY * 0.08d + jitterY, travelZ * 0.08d + jitterZ);
            burst++;
        }
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel clientLevel = minecraft.level;
        if (clientLevel == null) {
            forget();
            return;
        }
        long gameTime = clientLevel.getGameTime();
        float partialTick = event.getPartialTick();
        String dimensionId = clientLevel.dimension().location().toString();
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        ArrayList<RenderType> drawn = new ArrayList<>();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        drawGrounds(clientLevel, pose, buffers, drawn, dimensionId, gameTime, partialTick);
        SurfaceSnapshotAssembly.expireEnded(gameTime);
        drawSurfaces(pose, buffers, drawn, dimensionId, gameTime, partialTick);
        drawBurns(clientLevel, pose, event.getCamera(), buffers, drawn, gameTime, partialTick);
        drawSkyEmbers(clientLevel, pose, event.getCamera(), buffers, drawn, gameTime, partialTick);
        drawBodyPulses(clientLevel, pose, event.getCamera(), buffers, drawn, gameTime, partialTick);
        drawBodyEnds(clientLevel, pose, event.getCamera(), buffers, drawn, gameTime, partialTick);
        drawFurnaces(clientLevel, pose, event.getCamera(), buffers, drawn);
        pose.popPose();
        for (RenderType renderType : drawn) {
            buffers.endBatch(renderType);
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        forget();
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() != null && event.getLevel().isClientSide()) {
            forget();
        }
    }

    private static void drawGrounds(ClientLevel clientLevel, PoseStack pose, MultiBufferSource.BufferSource buffers,
                                    ArrayList<RenderType> drawn, String dimensionId, long gameTime, float partialTick) {
        Iterator<GroundView> views = GROUNDS.values().iterator();
        while (views.hasNext()) {
            GroundView view = views.next();
            double age = gameTime + partialTick - view.birthTick;
            if (!dimensionId.equals(view.dimensionId) || age > view.expireTick - view.birthTick) {
                if (gameTime > view.expireTick) {
                    views.remove();
                }
                continue;
            }
            drawGround(clientLevel, pose, buffers, drawn, view, age);
        }
    }

    private static void drawGround(ClientLevel clientLevel, PoseStack pose, MultiBufferSource.BufferSource buffers,
                                   ArrayList<RenderType> drawn, GroundView view, double age) {
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        double covered = FireVisualLayout.coveredRadius(view.radius, age);
        boolean spreading = FireVisualLayout.waveVisible(age);
        Band waveBand = spreading ? band(FireVisualLayout.stripFrame(age, FireVisualLayout.WAVE_FRAMES), FireVisualLayout.WAVE_FRAMES) : null;
        int opacity = seaOpacity(age, view.expireTick - view.birthTick);
        int lowColumnX = Mth.floor(view.originX - view.radius);
        int highColumnX = Mth.ceil(view.originX + view.radius) - 1;
        int lowColumnZ = Mth.floor(view.originZ - view.radius);
        int highColumnZ = Mth.ceil(view.originZ + view.radius) - 1;
        int columnX = lowColumnX;
        while (columnX <= highColumnX) {
            int columnZ = lowColumnZ;
            while (columnZ <= highColumnZ) {
                double centerX = columnX + 0.5d;
                double centerZ = columnZ + 0.5d;
                double east = Math.abs(centerX - view.originX);
                double north = Math.abs(centerZ - view.originZ);
                boolean inSquare = east <= view.radius && north <= view.radius;
                boolean inDisc = GroundFireMath.insideDisc(view.originX, view.originZ, centerX, centerZ, view.radius);
                if (inSquare || inDisc) {
                    double surface = StandSurface.columnTop(clientLevel, centerX, centerZ, view.originY,
                            GroundFireFields.VERTICAL_SLACK);
                    if (!Double.isNaN(surface)) {
                        if (spreading && inDisc && waveBand != null) {
                            VertexConsumer wave = consumer(buffers, drawn,
                                    inGloom(SPREAD_WAVE, BLUE_SPREAD_WAVE, centerX, surface, centerZ));
                            discPatch(wave, matrix, normal, columnX, columnZ, surface + SURFACE_LIFT,
                                    view.originX, view.originZ, view.radius, waveBand);
                        }
                        boolean coveredSquare = covered > 0.0d && east <= covered && north <= covered;
                        if (opacity > 0 && coveredSquare) {
                            paintAsh(clientLevel, matrix, normal, buffers, drawn, view, columnX, columnZ,
                                    centerX, centerZ, surface, covered, age, opacity);
                        }
                    }
                }
                columnZ += 1;
            }
            columnX += 1;
        }
    }

    /** 已覆盖地表铺灰烬。圆边留缺口，方形角落仍留可见灰。余烬和上升火星另画。 */
    private static void paintAsh(ClientLevel clientLevel, Matrix4f matrix, Matrix3f normal,
                                 MultiBufferSource.BufferSource buffers, ArrayList<RenderType> drawn, GroundView view,
                                 int columnX, int columnZ, double centerX, double centerZ, double surface,
                                 double covered, double age, int opacity) {
        double left = Math.max(columnX, view.originX - covered);
        double right = Math.min(columnX + 1.0d, view.originX + covered);
        double northEdge = Math.max(columnZ, view.originZ - covered);
        double southEdge = Math.min(columnZ + 1.0d, view.originZ + covered);
        if (right - left <= 1.0E-4d || southEdge - northEdge <= 1.0E-4d) {
            return;
        }
        double distance = Math.hypot(centerX - view.originX, centerZ - view.originZ);
        boolean insideRound = distance <= covered;
        int mixed = columnMix(columnX, columnZ);
        boolean brokenGap = insideRound && distance > covered * 0.78d && mixed % 4 == 0;
        int ashOpacity = opacity;
        if (!insideRound) {
            ashOpacity = opacity * 3 / 4;
            if (ashOpacity <= 0) {
                ashOpacity = 1;
            }
        }
        if (!brokenGap) {
            BlockPos lightBlock = BlockPos.containing(centerX, surface + 0.2d, centerZ);
            int packedLight = LevelRenderer.getLightColor(clientLevel, lightBlock);
            int variant = (mixed / FireVisualLayout.ASH_VARIANTS) % FireVisualLayout.ASH_VARIANTS;
            VertexConsumer ash = styledConsumer(buffers, drawn, ASH_SHEETS[variant], false);
            ashTile(ash, matrix, normal, left, right, northEdge, southEdge, surface + SURFACE_LIFT,
                    columnX, columnZ, ashOpacity, packedLight);
        }
        boolean emberHere = mixed % 13 == 0 || (brokenGap && mixed % 2 == 0);
        if (emberHere) {
            int along = mixed % 50;
            int across = (mixed / 7) % 50;
            double emberX = columnX + 0.2d + along / 100.0d;
            double emberZ = columnZ + 0.2d + across / 100.0d;
            if (Math.abs(emberX - view.originX) <= covered && Math.abs(emberZ - view.originZ) <= covered) {
                int frame = FireVisualLayout.stripFrame(age + (mixed % FireVisualLayout.ASH_EMBER_FRAMES),
                        FireVisualLayout.ASH_EMBER_FRAMES);
                double blink = 0.55d + 0.45d * Math.sin(age * 0.7d + mixed * 0.17d);
                int emberOpacity = (int) Math.round(opacity * blink);
                if (emberOpacity > 0) {
                    VertexConsumer embers = styledConsumer(buffers, drawn,
                            inGloom(GROUND_ASH_EMBERS, BLUE_ASH_EMBERS, emberX, surface, emberZ), true);
                    flat(embers, matrix, normal, emberX, surface + SURFACE_LIFT + 0.01d, emberZ, 0.16d,
                            band(frame, FireVisualLayout.ASH_EMBER_FRAMES), emberOpacity);
                }
            }
        }
        if (insideRound && mixed % 31 == 0) {
            double cycle = 18.0d;
            double risen = (age * 0.62d + (mixed % 18)) % cycle;
            int sparkOpacity = (int) Math.round(opacity * (1.0d - risen / cycle) * 0.9d);
            if (sparkOpacity > 0) {
                int sparkFrame = FireVisualLayout.stripFrame(age + (mixed % FireVisualLayout.ASH_EMBER_FRAMES),
                        FireVisualLayout.ASH_EMBER_FRAMES);
                VertexConsumer spark = styledConsumer(buffers, drawn,
                        inGloom(GROUND_ASH_EMBERS, BLUE_ASH_EMBERS, centerX, surface, centerZ), true);
                cross(spark, matrix, normal, centerX, surface + SURFACE_LIFT + 0.2d + risen * 0.05d, centerZ,
                        0.07d, 0.22d, band(sparkFrame, FireVisualLayout.ASH_EMBER_FRAMES), sparkOpacity);
            }
        }
    }

    private static void ashTile(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, double left, double right,
                                double northEdge, double southEdge, double lift, int columnX, int columnZ,
                                int opacity, int packedLight) {
        float leftU = (float) (left - columnX);
        float rightU = (float) (right - columnX);
        float northV = (float) (northEdge - columnZ);
        float southV = (float) (southEdge - columnZ);
        float rise = (float) lift;
        lit(consumer, matrix, normal, (float) left, rise, (float) northEdge, leftU, northV, opacity, packedLight);
        lit(consumer, matrix, normal, (float) right, rise, (float) northEdge, rightU, northV, opacity, packedLight);
        lit(consumer, matrix, normal, (float) right, rise, (float) southEdge, rightU, southV, opacity, packedLight);
        lit(consumer, matrix, normal, (float) left, rise, (float) southEdge, leftU, southV, opacity, packedLight);
    }

    private static int columnMix(int columnX, int columnZ) {
        int mixed = columnX * 73428767 ^ columnZ * 91293173;
        mixed ^= mixed >>> 16;
        if (mixed < 0) {
            mixed = ~mixed;
        }
        return mixed;
    }

    private static void drawBurns(ClientLevel clientLevel, PoseStack pose, Camera camera,
                                  MultiBufferSource.BufferSource buffers, ArrayList<RenderType> drawn,
                                  long gameTime, float partialTick) {
        Iterator<BurnView> views = BURNS.values().iterator();
        while (views.hasNext()) {
            BurnView view = views.next();
            if (gameTime - view.seenGameTime > POSE_GAP) {
                views.remove();
                continue;
            }
            Entity entity = clientLevel.getEntity(view.entityId);
            if (entity == null) {
                continue;
            }
            if (view.burnKind == FireVisualLayout.SPREAD) {
                drawSpread(pose, buffers, drawn, entity, view, gameTime, partialTick);
            } else if (view.burnKind == FireVisualLayout.SKY) {
                drawSky(pose, camera, buffers, drawn, entity, view, gameTime, partialTick);
            }
        }
    }

    private static void drawSpread(PoseStack pose, MultiBufferSource.BufferSource buffers, ArrayList<RenderType> drawn,
                                   Entity entity, BurnView view, long gameTime, float partialTick) {
        double scale = FireVisualLayout.clingScale(view.tiers, view.remainder);
        double height = Math.max(0.8d, entity.getBbHeight() * 0.85d) * scale;
        double halfWidth = height * (32.0d / 40.0d) * 0.5d;
        int frame = FireVisualLayout.stripFrame(gameTime + partialTick, FireVisualLayout.FLAME_FRAMES);
        double worldX = entity.getX();
        double worldY = entity.getY();
        double worldZ = entity.getZ();
        VertexConsumer flames = consumer(buffers, drawn, inGloom(GROUND_FLAME, BLUE_GROUND_FLAME, worldX, worldY, worldZ));
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        cross(flames, matrix, normal, worldX, worldY + 0.05d, worldZ, halfWidth, height,
                band(frame, FireVisualLayout.FLAME_FRAMES), 255);
        int emberFrame = FireVisualLayout.stripFrame(gameTime + partialTick + entity.getId(), FireVisualLayout.FLAME_FRAMES);
        VertexConsumer embers = consumer(buffers, drawn,
                inGloom(GROUND_EMBERS, BLUE_GROUND_EMBERS, worldX, worldY, worldZ));
        cross(embers, matrix, normal, worldX, worldY + 0.02d, worldZ, halfWidth * 0.7d, height * 0.45d,
                band(emberFrame, FireVisualLayout.FLAME_FRAMES), 255);
    }

    private static void drawSky(PoseStack pose, Camera camera, MultiBufferSource.BufferSource buffers,
                                ArrayList<RenderType> drawn, Entity entity, BurnView view,
                                long gameTime, float partialTick) {
        int frame = FireVisualLayout.bodyLoopFrame(view.interval, view.cursor, view.wait);
        BodyPulseView pulse = BODY_PULSES.get(entity.getId());
        if (pulse != null) {
            if (pulse.terminal || view.cursor <= pulse.startedCursor) {
                BODY_PULSES.remove(entity.getId());
            } else {
                int pulseFrame = FireVisualLayout.bodyPulseFrame((int) (gameTime + partialTick - pulse.birthGameTime));
                if (pulseFrame >= 0) {
                    frame = pulseFrame;
                } else {
                    BODY_PULSES.remove(entity.getId());
                }
            }
        }
        int backdropFrame = FireVisualLayout.skySustainFrame(view.interval, view.cursor, view.wait);
        double backdropScale = FireVisualLayout.skyScale(view.segmentCount, view.cursor, view.remainder);
        paintSky(pose, camera, buffers, drawn, entity, backdropFrame, backdropScale);
        paintBody(pose, camera, buffers, drawn, entity, frame);
    }

    private static void drawBodyPulses(ClientLevel clientLevel, PoseStack pose, Camera camera,
                                       MultiBufferSource.BufferSource buffers, ArrayList<RenderType> drawn,
                                       long gameTime, float partialTick) {
        Iterator<Map.Entry<Integer, BodyPulseView>> views = BODY_PULSES.entrySet().iterator();
        while (views.hasNext()) {
            Map.Entry<Integer, BodyPulseView> entry = views.next();
            BurnView burn = BURNS.get(FireVisualLayout.burnToken(entry.getKey(), FireVisualLayout.SKY));
            BodyPulseView pulse = entry.getValue();
            if (burn != null) {
                if (pulse.terminal || burn.cursor <= pulse.startedCursor) {
                    views.remove();
                }
                continue;
            }
            if (!pulse.terminal) {
                views.remove();
                continue;
            }
            int frame = FireVisualLayout.bodyPulseFrame((int) (gameTime + partialTick - pulse.birthGameTime));
            if (frame < 0) {
                views.remove();
                continue;
            }
            Entity entity = clientLevel.getEntity(entry.getKey());
            if (entity == null) {
                continue;
            }
            paintBody(pose, camera, buffers, drawn, entity, frame);
        }
    }

    private static void drawBodyEnds(ClientLevel clientLevel, PoseStack pose, Camera camera,
                                     MultiBufferSource.BufferSource buffers, ArrayList<RenderType> drawn,
                                     long gameTime, float partialTick) {
        Iterator<Map.Entry<Integer, BodyEndView>> views = BODY_ENDS.entrySet().iterator();
        while (views.hasNext()) {
            Map.Entry<Integer, BodyEndView> entry = views.next();
            double age = gameTime + partialTick - entry.getValue().birthGameTime;
            if (age < 0.0d) {
                continue;
            }
            int frame = FireVisualLayout.bodyEndFrame((int) age);
            if (frame < 0) {
                views.remove();
                continue;
            }
            Entity entity = clientLevel.getEntity(entry.getKey());
            if (entity == null) {
                continue;
            }
            paintBody(pose, camera, buffers, drawn, entity, frame);
        }
    }

    private static void drawSkyEmbers(ClientLevel clientLevel, PoseStack pose, Camera camera,
                                      MultiBufferSource.BufferSource buffers, ArrayList<RenderType> drawn,
                                      long gameTime, float partialTick) {
        Iterator<Map.Entry<Integer, SkyEmberView>> views = SKY_EMBERS.entrySet().iterator();
        while (views.hasNext()) {
            Map.Entry<Integer, SkyEmberView> entry = views.next();
            SkyEmberView ember = entry.getValue();
            double age = gameTime + partialTick - ember.birthGameTime;
            int tick = FireVisualLayout.skyEndTick(age);
            if (tick < 0) {
                views.remove();
                continue;
            }
            Entity entity = clientLevel.getEntity(entry.getKey());
            if (entity == null) {
                continue;
            }
            int heldOpacity = FireVisualLayout.skyEndOpacity(FireVisualLayout.skyEndHeldAlpha(age));
            int emberOpacity = FireVisualLayout.skyEndOpacity(FireVisualLayout.skyEndEmberAlpha(age));
            int emberIndex = FireVisualLayout.skyEndEmberIndex(tick);
            paintSkyRow(pose, camera, buffers, drawn, entity, ember.heldIndex, ember.heldScale, heldOpacity);
            paintSkyRow(pose, camera, buffers, drawn, entity, emberIndex, ember.heldScale, emberOpacity);
        }
    }

    private static void paintSky(PoseStack pose, Camera camera, MultiBufferSource.BufferSource buffers,
                                 ArrayList<RenderType> drawn, Entity entity, int frame, double scale) {
        if (frame <= 0 || frame > FireVisualLayout.SKY_FRAMES) {
            return;
        }
        paintSkyRow(pose, camera, buffers, drawn, entity,
                FireVisualLayout.textureRow(frame, FireVisualLayout.SKY_FRAMES), scale, 255);
    }

    private static void paintSkyRow(PoseStack pose, Camera camera, MultiBufferSource.BufferSource buffers,
                                    ArrayList<RenderType> drawn, Entity entity, int row, double scale, int opacity) {
        if (row < 0 || row >= FireVisualLayout.SKY_FRAMES || opacity <= 0) {
            return;
        }
        double bodyHeight = entity.getBbHeight();
        double cardHeight = Math.max(1.8d, bodyHeight) * 1.35d * scale;
        double cardWidth = cardHeight * (128.0d / 144.0d);
        VertexConsumer backdrop = consumer(buffers, drawn, inGloom(SKY_BACKDROP, BLUE_KINDLE,
                entity.getX(), entity.getY() + bodyHeight * 0.45d, entity.getZ()));
        billboard(pose, camera, backdrop, entity.getX(), entity.getY() + bodyHeight * 0.45d, entity.getZ(),
                cardWidth * 0.5d, cardHeight * 0.5d, band(row, FireVisualLayout.SKY_FRAMES), opacity);
    }

    private static void paintBody(PoseStack pose, Camera camera, MultiBufferSource.BufferSource buffers,
                                  ArrayList<RenderType> drawn, Entity entity, int frame) {
        if (frame < 0 || frame >= FireVisualLayout.BODY_FRAMES) {
            return;
        }
        AABB body = entity.getBoundingBox();
        double bodyHeight = body.getYsize();
        if (!Double.isFinite(bodyHeight) || bodyHeight <= 0.0d) {
            return;
        }
        Vec3 center = body.getCenter();
        Vector3f look = camera.getLookVector();
        double lookLength = Math.sqrt(look.x() * look.x() + look.y() * look.y() + look.z() * look.z());
        if (!Double.isFinite(lookLength) || lookLength <= 1.0E-6d) {
            return;
        }
        double normalX = -look.x() / lookLength;
        double normalY = -look.y() / lookLength;
        double normalZ = -look.z() / lookLength;
        double clearance = Math.abs(normalX) * body.getXsize() * 0.5d
                + Math.abs(normalY) * body.getYsize() * 0.5d
                + Math.abs(normalZ) * body.getZsize() * 0.5d
                + FireVisualLayout.BODY_CAMERA_CLEARANCE;
        VertexConsumer flame = consumer(buffers, drawn, inGloom(BODY_FLAME, BLUE_BODY, center.x, center.y, center.z));
        billboard(pose, camera, flame, center.x + normalX * clearance, center.y + normalY * clearance,
                center.z + normalZ * clearance, bodyHeight * 0.75d * 0.5d, bodyHeight * 0.5d,
                band(frame, FireVisualLayout.BODY_FRAMES));
    }

    private static void drawFurnaces(ClientLevel clientLevel, PoseStack pose, Camera camera,
                                     MultiBufferSource.BufferSource buffers, ArrayList<RenderType> drawn) {
        FurnaceBodyVisual.paintCool(clientLevel, pose, camera, buffers, drawn);
    }

    private static void discPatch(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int columnX, int columnZ,
                                  double lift, double originX, double originZ, double radius, Band band) {
        float left = columnX;
        float right = columnX + 1.0f;
        float north = columnZ;
        float south = columnZ + 1.0f;
        float rise = (float) lift;
        float leftAcross = discTexture(columnX - originX, radius);
        float rightAcross = discTexture(columnX + 1.0d - originX, radius);
        float northAlong = discTexture(columnZ - originZ, radius);
        float southAlong = discTexture(columnZ + 1.0d - originZ, radius);
        float northV = band.farV + northAlong * (band.nearV - band.farV);
        float southV = band.farV + southAlong * (band.nearV - band.farV);
        vertex(consumer, matrix, normal, left, rise, north, leftAcross, northV, 0.0f, 1.0f, 0.0f);
        vertex(consumer, matrix, normal, right, rise, north, rightAcross, northV, 0.0f, 1.0f, 0.0f);
        vertex(consumer, matrix, normal, right, rise, south, rightAcross, southV, 0.0f, 1.0f, 0.0f);
        vertex(consumer, matrix, normal, left, rise, south, leftAcross, southV, 0.0f, 1.0f, 0.0f);
    }

    private static float discTexture(double offset, double radius) {
        double coordinate = FireVisualLayout.discCoordinate(offset, radius);
        if (!Double.isFinite(coordinate)) {
            return 0.5f;
        }
        if (coordinate < 0.0d) {
            coordinate = 0.0d;
        }
        if (coordinate > 1.0d) {
            coordinate = 1.0d;
        }
        return (float) coordinate;
    }

    private static VertexConsumer consumer(MultiBufferSource.BufferSource buffers, ArrayList<RenderType> drawn,
                                           ResourceLocation texture) {
        return styledConsumer(buffers, drawn, texture, true);
    }

    private static VertexConsumer styledConsumer(MultiBufferSource.BufferSource buffers, ArrayList<RenderType> drawn,
                                                 ResourceLocation texture, boolean emissive) {
        RenderType renderType = emissive
                ? RenderType.entityTranslucentEmissive(texture)
                : RenderType.entityTranslucent(texture);
        if (!drawn.contains(renderType)) {
            drawn.add(renderType);
        }
        return buffers.getBuffer(renderType);
    }

    private static Band band(int frame, int frameCount) {
        float nearV = frame / (float) frameCount;
        float farV = (frame + 1) / (float) frameCount;
        return new Band(nearV, farV);
    }

    private static int seaOpacity(double age, long lifeSpan) {
        double remain = lifeSpan - age;
        if (remain >= 20.0d) {
            return 255;
        }
        if (remain <= 0.0d) {
            return 0;
        }
        return (int) Math.round(255.0d * remain / 20.0d);
    }

    private static void flat(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, double centerX, double lift,
                             double centerZ, double half, Band band, int opacity) {
        float left = (float) (centerX - half);
        float right = (float) (centerX + half);
        float north = (float) (centerZ - half);
        float south = (float) (centerZ + half);
        float rise = (float) lift;
        tinted(consumer, matrix, normal, left, rise, north, 0.0f, band.farV, 0.0f, 1.0f, 0.0f, opacity);
        tinted(consumer, matrix, normal, right, rise, north, 1.0f, band.farV, 0.0f, 1.0f, 0.0f, opacity);
        tinted(consumer, matrix, normal, right, rise, south, 1.0f, band.nearV, 0.0f, 1.0f, 0.0f, opacity);
        tinted(consumer, matrix, normal, left, rise, south, 0.0f, band.nearV, 0.0f, 1.0f, 0.0f, opacity);
    }

    private static void cross(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, double centerX, double bottomY,
                              double centerZ, double halfWidth, double height, Band band, int opacity) {
        float half = (float) halfWidth;
        float rise = (float) height;
        float anchorX = (float) centerX;
        float anchorY = (float) bottomY;
        float anchorZ = (float) centerZ;
        tinted(consumer, matrix, normal, anchorX - half, anchorY, anchorZ, 0.0f, band.farV, 0.0f, 0.0f, 1.0f, opacity);
        tinted(consumer, matrix, normal, anchorX + half, anchorY, anchorZ, 1.0f, band.farV, 0.0f, 0.0f, 1.0f, opacity);
        tinted(consumer, matrix, normal, anchorX + half, anchorY + rise, anchorZ, 1.0f, band.nearV, 0.0f, 0.0f, 1.0f, opacity);
        tinted(consumer, matrix, normal, anchorX - half, anchorY + rise, anchorZ, 0.0f, band.nearV, 0.0f, 0.0f, 1.0f, opacity);
        tinted(consumer, matrix, normal, anchorX, anchorY, anchorZ - half, 0.0f, band.farV, 1.0f, 0.0f, 0.0f, opacity);
        tinted(consumer, matrix, normal, anchorX, anchorY, anchorZ + half, 1.0f, band.farV, 1.0f, 0.0f, 0.0f, opacity);
        tinted(consumer, matrix, normal, anchorX, anchorY + rise, anchorZ + half, 1.0f, band.nearV, 1.0f, 0.0f, 0.0f, opacity);
        tinted(consumer, matrix, normal, anchorX, anchorY + rise, anchorZ - half, 0.0f, band.nearV, 1.0f, 0.0f, 0.0f, opacity);
    }

    private static void billboard(PoseStack pose, Camera camera, VertexConsumer consumer, double worldX, double centerY,
                                  double worldZ, double halfWidth, double halfHeight, Band band) {
        billboard(pose, camera, consumer, worldX, centerY, worldZ, halfWidth, halfHeight, band, 255);
    }

    private static void billboard(PoseStack pose, Camera camera, VertexConsumer consumer, double worldX, double centerY,
                                  double worldZ, double halfWidth, double halfHeight, Band band, int opacity) {
        pose.pushPose();
        pose.translate(worldX, centerY, worldZ);
        CameraFacingClient.orientDeclaredPlane(pose, CameraFacingEffectId.FENTIAN_SKY_FLAME, camera);
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        float left = (float) -halfWidth;
        float right = (float) halfWidth;
        float bottom = (float) -halfHeight;
        float top = (float) halfHeight;
        shaded(consumer, matrix, normal, left, bottom, 0.0f, 0.0f, band.farV, 0.0f, 0.0f, 1.0f, 255, 255, 255, opacity);
        shaded(consumer, matrix, normal, right, bottom, 0.0f, 1.0f, band.farV, 0.0f, 0.0f, 1.0f, 255, 255, 255, opacity);
        shaded(consumer, matrix, normal, right, top, 0.0f, 1.0f, band.nearV, 0.0f, 0.0f, 1.0f, 255, 255, 255, opacity);
        shaded(consumer, matrix, normal, left, top, 0.0f, 0.0f, band.nearV, 0.0f, 0.0f, 1.0f, 255, 255, 255, opacity);
        pose.popPose();
    }

    private static void shaded(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float pointX, float pointY,
                               float pointZ, float textureU, float textureV, float normalX, float normalY, float normalZ,
                               int red, int green, int blue) {
        shaded(consumer, matrix, normal, pointX, pointY, pointZ, textureU, textureV, normalX, normalY, normalZ,
                red, green, blue, 255);
    }

    private static void shaded(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float pointX, float pointY,
                               float pointZ, float textureU, float textureV, float normalX, float normalY, float normalZ,
                               int red, int green, int blue, int opacity) {
        consumer.vertex(matrix, pointX, pointY, pointZ)
                .color(red, green, blue, opacity)
                .uv(textureU, textureV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, normalX, normalY, normalZ)
                .endVertex();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float pointX, float pointY,
                               float pointZ, float textureU, float textureV, float normalX, float normalY, float normalZ) {
        tinted(consumer, matrix, normal, pointX, pointY, pointZ, textureU, textureV, normalX, normalY, normalZ, 255);
    }

    private static void tinted(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float pointX, float pointY,
                               float pointZ, float textureU, float textureV, float normalX, float normalY, float normalZ,
                               int opacity) {
        lit(consumer, matrix, normal, pointX, pointY, pointZ, textureU, textureV, normalX, normalY, normalZ, opacity,
                LightTexture.FULL_BRIGHT);
    }

    private static void lit(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float pointX, float pointY,
                            float pointZ, float textureU, float textureV, int opacity, int packedLight) {
        lit(consumer, matrix, normal, pointX, pointY, pointZ, textureU, textureV, 0.0f, 1.0f, 0.0f, opacity, packedLight);
    }

    private static void lit(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float pointX, float pointY,
                            float pointZ, float textureU, float textureV, float normalX, float normalY, float normalZ,
                            int opacity, int packedLight) {
        consumer.vertex(matrix, pointX, pointY, pointZ)
                .color(255, 255, 255, opacity)
                .uv(textureU, textureV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal, normalX, normalY, normalZ)
                .endVertex();
    }

    private static long seenNow() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0L;
        }
        return minecraft.level.getGameTime();
    }

    private static ResourceLocation sheet(String path) {
        return new ResourceLocation(XueXiMod.MOD_ID, path);
    }

    private static void forget() {
        SurfaceSnapshotAssembly.forget();
        GROUNDS.clear();
        SURFACES.clear();
        BURNS.clear();
        SKY_EMBERS.clear();
        BODY_PULSES.clear();
        BODY_ENDS.clear();
        FurnaceBodyVisual.forget();
    }

    private static void drawSurfaces(PoseStack pose, MultiBufferSource.BufferSource buffers, ArrayList<RenderType> drawn,
                                     String dimensionId, long gameTime, float partialTick) {
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        Iterator<SurfaceView> views = SURFACES.values().iterator();
        while (views.hasNext()) {
            SurfaceView view = views.next();
            double age = gameTime + partialTick - view.birthTick;
            if (!dimensionId.equals(view.dimensionId) || age > view.expireTick - view.birthTick) {
                if (gameTime > view.expireTick) {
                    views.remove();
                }
                continue;
            }
            int opacity = seaOpacity(age, view.expireTick - view.birthTick);
            if (opacity <= 0 || view.slices == null) {
                continue;
            }
            int piece = 0;
            while (piece + 5 < view.slices.length) {
                double lowX = view.slices[piece];
                double lowY = view.slices[piece + 1];
                double lowZ = view.slices[piece + 2];
                double highX = view.slices[piece + 3];
                double highZ = view.slices[piece + 5];
                double spanX = highX - lowX;
                double spanZ = highZ - lowZ;
                double tongue = spanX < spanZ ? spanX * 0.35d : spanZ * 0.35d;
                if (tongue > 0.45d) {
                    tongue = 0.45d;
                }
                if (tongue < 0.12d) {
                    tongue = 0.12d;
                }
                double centerX = (lowX + highX) * 0.5d;
                double centerZ = (lowZ + highZ) * 0.5d;
                int flameFrame = FireVisualLayout.stripFrame(gameTime + partialTick + centerX + centerZ,
                        FireVisualLayout.FLAME_FRAMES);
                int emberFrame = FireVisualLayout.stripFrame(gameTime + partialTick + centerX * 3.0d + centerZ,
                        FireVisualLayout.FLAME_FRAMES);
                VertexConsumer embers = consumer(buffers, drawn,
                        inGloom(emberSheet(view.style), BLUE_GROUND_EMBERS, centerX, lowY, centerZ));
                rect(embers, matrix, normal, lowX, lowY + SURFACE_LIFT, lowZ, highX, highZ,
                        band(emberFrame, FireVisualLayout.FLAME_FRAMES), opacity);
                VertexConsumer flames = consumer(buffers, drawn,
                        inGloom(flameSheet(view.style), BLUE_GROUND_FLAME, centerX, lowY, centerZ));
                cross(flames, matrix, normal, centerX, lowY + SURFACE_LIFT, centerZ, tongue, 0.78d,
                        band(flameFrame, FireVisualLayout.FLAME_FRAMES), opacity);
                piece += 6;
            }
        }
    }

    /** 样式 0 以及尚未出厂的其它样式都先用燎原的橙色地表贴图。 */
    private static ResourceLocation flameSheet(int style) {
        return GROUND_FLAME;
    }

    private static ResourceLocation inGloom(ResourceLocation ordinary, ResourceLocation blue,
                                           double worldX, double worldY, double worldZ) {
        return NetherBlueFlame.choose(Minecraft.getInstance().level, worldX, worldY, worldZ, ordinary, blue);
    }

    private static ResourceLocation emberSheet(int style) {
        return GROUND_EMBERS;
    }

    private static void rect(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, double lowX, double lift,
                             double lowZ, double highX, double highZ, Band band, int opacity) {
        float left = (float) lowX;
        float right = (float) highX;
        float north = (float) lowZ;
        float south = (float) highZ;
        float rise = (float) lift;
        tinted(consumer, matrix, normal, left, rise, north, 0.0f, band.farV, 0.0f, 1.0f, 0.0f, opacity);
        tinted(consumer, matrix, normal, right, rise, north, 1.0f, band.farV, 0.0f, 1.0f, 0.0f, opacity);
        tinted(consumer, matrix, normal, right, rise, south, 1.0f, band.nearV, 0.0f, 1.0f, 0.0f, opacity);
        tinted(consumer, matrix, normal, left, rise, south, 0.0f, band.nearV, 0.0f, 1.0f, 0.0f, opacity);
    }

    private static final class SurfaceView {
        private final String dimensionId;
        private final double originX;
        private final double originY;
        private final double originZ;
        private final long birthTick;
        private final long expireTick;
        private final int style;
        private int version;
        private double[] slices;

        private SurfaceView(String dimensionId, double originX, double originY, double originZ, long birthTick,
                            long expireTick, int style, int version, double[] slices) {
            this.dimensionId = dimensionId;
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.birthTick = birthTick;
            this.expireTick = expireTick;
            this.style = style;
            this.version = version;
            this.slices = slices;
        }
    }

    private static final class GroundView {
        private final String dimensionId;
        private final double originX;
        private final double originY;
        private final double originZ;
        private final double radius;
        private final long birthTick;
        private final long expireTick;

        private GroundView(String dimensionId, double originX, double originY, double originZ, double radius,
                           long birthTick, long expireTick) {
            this.dimensionId = dimensionId;
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.radius = radius;
            this.birthTick = birthTick;
            this.expireTick = expireTick;
        }
    }

    private static final class BurnView {
        private final int entityId;
        private final int burnKind;
        private final int tiers;
        private final int remainder;
        private final int segmentCount;
        private final int interval;
        private final int cursor;
        private final int wait;
        private final long seenGameTime;

        private BurnView(int entityId, int burnKind, int tiers, int remainder, int segmentCount, int interval,
                         int cursor, int wait, long seenGameTime) {
            this.entityId = entityId;
            this.burnKind = burnKind;
            this.tiers = tiers;
            this.remainder = remainder;
            this.segmentCount = segmentCount;
            this.interval = interval;
            this.cursor = cursor;
            this.wait = wait;
            this.seenGameTime = seenGameTime;
        }
    }

    private static final class SkyEmberView {
        private final int heldIndex;
        private final double heldScale;
        private final long birthGameTime;

        private SkyEmberView(int heldIndex, double heldScale, long birthGameTime) {
            this.heldIndex = heldIndex;
            this.heldScale = heldScale;
            this.birthGameTime = birthGameTime;
        }
    }

    private static final class BodyPulseView {
        private final long birthGameTime;
        private final int startedCursor;
        private final boolean terminal;

        private BodyPulseView(long birthGameTime, int startedCursor, boolean terminal) {
            this.birthGameTime = birthGameTime;
            this.startedCursor = startedCursor;
            this.terminal = terminal;
        }
    }

    private static final class BodyEndView {
        private final long birthGameTime;

        private BodyEndView(long birthGameTime) {
            this.birthGameTime = birthGameTime;
        }
    }

    private static final class Band {
        private final float nearV;
        private final float farV;

        private Band(float nearV, float farV) {
            this.nearV = nearV;
            this.farV = farV;
        }
    }
}
