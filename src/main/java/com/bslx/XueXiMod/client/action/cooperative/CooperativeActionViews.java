package com.bslx.XueXiMod.client.action.cooperative;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.network.CooperativeActionS2CPacket;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneEntity;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeActionIds;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeStructureLimits;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.WeaponDisplaySnapshot;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 每个演员只保留最新序号。旧停止不能清掉更新的动作，迟到包按服务器开始刻快进。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, value = Dist.CLIENT)
public final class CooperativeActionViews {
    private static final int CACHE_CAP = CooperativeStructureLimits.ACTIVE_FAMILY_CAP * 2;
    private static final int UNSEEN_GRACE_TICKS = 20;
    /** 同一把钟时，收包世界时间与服务器开始刻只差网络延迟。世界年龄和服务器开机刻会差很远。 */
    private static final long SAME_CLOCK_SKEW_TICKS = 200L;
    private static final Map<UUID, View> VIEWS = new LinkedHashMap<>();

    private CooperativeActionViews() {
    }

    public static void accept(CooperativeActionS2CPacket packet) {
        if (packet == null || !packet.valid()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null) {
            return;
        }
        if (packet.kind() == CooperativeActionS2CPacket.STOP) {
            stop(packet);
            return;
        }
        if (packet.kind() != CooperativeActionS2CPacket.START && packet.kind() != CooperativeActionS2CPacket.TRACKING) {
            return;
        }
        View current = VIEWS.get(packet.actorId());
        if (current != null && packet.sequence() < current.sequence) {
            return;
        }
        if (current != null
                && packet.sequence() != current.sequence
                && CooperativeActionIds.SPIN.equals(current.actionId)
                && CooperativeActionIds.OBLIQUE_SLASH.equals(packet.actionId())
                && !expired(current, minecraft.level.getGameTime())) {
            return;
        }
        if (current != null && !current.serverSession.equals(packet.serverSession())) {
            VIEWS.clear();
            current = null;
        }
        long receivedTick = minecraft.level.getGameTime();
        if (current != null && packet.sequence() == current.sequence) {
            receivedTick = current.receivedTick;
        }
        View view = new View(packet, receivedTick);
        VIEWS.put(packet.actorId(), view);
        trim();
    }

    public static boolean holds(LivingEntity living) {
        return pose(living, 0.0F) != null;
    }

    public static WeaponDisplaySnapshot weapon(LivingEntity living) {
        if (!holds(living)) {
            return null;
        }
        View view = VIEWS.get(living.getUUID());
        return view == null ? null : view.weapon;
    }

    public static CooperativePose pose(LivingEntity living, float partialTick) {
        if (living == null) {
            return null;
        }
        View view = VIEWS.get(living.getUUID());
        if (view == null || view.entityId != living.getId()) {
            return null;
        }
        Level level = living.level();
        if (level == null || !view.dimension.equals(level.dimension())) {
            return null;
        }
        if (living instanceof CloneEntity cloneBody && cloneBody.generation() != view.actorGeneration) {
            return null;
        }
        if (!(living instanceof CloneEntity) && view.actorGeneration != CooperativeStructureLimits.OWNER_BODY_GENERATION) {
            return null;
        }
        long gameTime = level.getGameTime();
        if (expired(view, gameTime)) {
            return null;
        }
        float elapsedTicks = playedTicks(view, gameTime) + partialTick;
        float bodyYaw = Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot);
        return CooperativeActionTracks.sample(
                view.actionId,
                view.mirrored,
                view.windupTicks,
                view.recoveryTicks,
                elapsedTicks,
                Mth.wrapDegrees(view.visualYaw - bodyYaw));
    }

    public static void clear() {
        VIEWS.clear();
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        VIEWS.clear();
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null) {
            VIEWS.clear();
            return;
        }
        long gameTime = minecraft.level.getGameTime();
        Iterator<View> iterator = VIEWS.values().iterator();
        while (iterator.hasNext()) {
            View view = iterator.next();
            if (!view.dimension.equals(minecraft.level.dimension()) || expired(view, gameTime)) {
                iterator.remove();
                continue;
            }
            Entity actor = minecraft.level.getEntity(view.entityId);
            if (actor == null || !view.actorId.equals(actor.getUUID())) {
                if (view.seen || gameTime - view.receivedTick > UNSEEN_GRACE_TICKS) {
                    iterator.remove();
                }
                continue;
            }
            view.seen = true;
            if (actor instanceof CloneEntity cloneBody && cloneBody.generation() != view.actorGeneration) {
                iterator.remove();
            }
        }
    }

    private static void stop(CooperativeActionS2CPacket packet) {
        View current = VIEWS.get(packet.actorId());
        if (current == null || current.sequence != packet.sequence()) {
            return;
        }
        if (!current.serverSession.equals(packet.serverSession())
                || !current.familyOwnerId.equals(packet.familyOwnerId())
                || !current.familyCastId.equals(packet.familyCastId())
                || current.familyEpoch != packet.familyEpoch()
                || current.familyOrdinal != packet.familyOrdinal()) {
            return;
        }
        VIEWS.remove(packet.actorId());
    }

    private static boolean expired(View view, long gameTime) {
        long duration = (long) view.windupTicks + view.recoveryTicks;
        return playedTicks(view, gameTime) >= duration;
    }

    /**
     * 发布侧若写入的是服务器开机刻，而这里的世界年龄是存档时间，两者相减会立刻超过动作时长。
     * 钟差超出延迟窗口时，从本机收包时刻播放，不再把整段曲线判成结束。
     */
    private static long playedTicks(View view, long gameTime) {
        long skew = view.receivedTick - view.serverStartTick;
        long elapsed;
        if (skew <= SAME_CLOCK_SKEW_TICKS && skew >= -SAME_CLOCK_SKEW_TICKS) {
            elapsed = gameTime - view.serverStartTick;
        } else {
            elapsed = gameTime - view.receivedTick;
        }
        if (elapsed < 0L) {
            return 0L;
        }
        return elapsed;
    }

    private static void trim() {
        while (VIEWS.size() > CACHE_CAP) {
            UUID eldest = VIEWS.keySet().iterator().next();
            VIEWS.remove(eldest);
        }
    }

    private static final class View {
        private final UUID serverSession;
        private final ResourceKey<Level> dimension;
        private final UUID familyOwnerId;
        private final UUID familyCastId;
        private final long familyEpoch;
        private final int familyOrdinal;
        private final UUID actorId;
        private final int entityId;
        private final long actorGeneration;
        private final long sequence;
        private final ResourceLocation actionId;
        private final long serverStartTick;
        private final int windupTicks;
        private final int recoveryTicks;
        private final float visualYaw;
        private final boolean mirrored;
        private final WeaponDisplaySnapshot weapon;
        private final long receivedTick;
        private boolean seen;

        private View(CooperativeActionS2CPacket packet, long receivedTick) {
            this.serverSession = packet.serverSession();
            this.dimension = packet.dimension();
            this.familyOwnerId = packet.familyOwnerId();
            this.familyCastId = packet.familyCastId();
            this.familyEpoch = packet.familyEpoch();
            this.familyOrdinal = packet.familyOrdinal();
            this.actorId = packet.actorId();
            this.entityId = packet.entityId();
            this.actorGeneration = packet.actorGeneration();
            this.sequence = packet.sequence();
            this.actionId = packet.actionId();
            this.serverStartTick = packet.serverStartTick();
            this.windupTicks = packet.windupTicks();
            this.recoveryTicks = packet.recoveryTicks();
            this.visualYaw = packet.visualYaw();
            this.mirrored = packet.mirrored();
            this.weapon = packet.weapon();
            this.receivedTick = receivedTick;
        }
    }
}
