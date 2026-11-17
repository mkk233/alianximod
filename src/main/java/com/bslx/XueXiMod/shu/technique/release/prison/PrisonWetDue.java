package com.bslx.XueXiMod.shu.technique.release.prison;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 活动牢已经结束后仍未扣到的湿度。只存票据，不留实体引用，也不恢复牢。
 * 同一 prisonId 重复登记保持第一次。真正扣完才删掉，避免停服把未扣意图丢掉。
 */
public final class PrisonWetDue extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String STORAGE_ID = "xueximod_prison_wet_due";
    private static final Set<UUID> APPLYING = new HashSet<>();

    private final Map<UUID, Ticket> tickets = new HashMap<>();

    private PrisonWetDue() {
    }

    /** 登记一张尚未扣到的湿度票据。同一牢编号已有票据时保持第一次。 */
    public static void keep(UUID prisonId, UUID targetId, ResourceKey<Level> dimension, int lockedTier, EndReason reason) {
        if (prisonId == null || targetId == null || dimension == null || reason == null) {
            LOGGER.error("water prison wet ticket missing identity for {}", prisonId);
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            LOGGER.error("water prison wet ticket {} was not stored; no server", prisonId);
            return;
        }
        ServerLevel level = server.getLevel(dimension);
        if (level == null) {
            level = server.overworld();
        }
        PrisonWetDue data = get(level);
        if (data.tickets.containsKey(prisonId)) {
            return;
        }
        data.tickets.put(prisonId, new Ticket(prisonId, targetId, dimension, lockedTier, reason));
        data.setDirty();
    }

    /** 湿度已经写入后删掉对应票据。 */
    public static void drop(UUID prisonId, ResourceKey<Level> dimension) {
        if (prisonId == null) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        if (dimension != null) {
            ServerLevel level = server.getLevel(dimension);
            if (level != null) {
                get(level).remove(prisonId);
            }
        }
        get(server.overworld()).remove(prisonId);
    }

    /** 目标已经加载时按当时湿度结算一次。没有票据或正在结算时直接返回。 */
    public static void settleLoaded(LivingEntity living) {
        if (living == null || !(living.level() instanceof ServerLevel level) || level.isClientSide()) {
            return;
        }
        settleLevel(level, living);
        ServerLevel overworld = level.getServer().overworld();
        if (overworld != level) {
            settleLevel(overworld, living);
        }
    }

    private static void settleLevel(ServerLevel level, LivingEntity living) {
        PrisonWetDue data = get(level);
        ArrayList<Ticket> due = new ArrayList<>();
        for (Ticket ticket : data.tickets.values()) {
            if (living.getUUID().equals(ticket.targetId())) {
                due.add(ticket);
            }
        }
        for (Ticket ticket : due) {
            if (!APPLYING.add(ticket.prisonId())) {
                continue;
            }
            try {
                int current = com.bslx.XueXiMod.status.WetnessState.read(living);
                boolean wrote;
                if (current <= 0) {
                    if (living.getEffect(com.bslx.XueXiMod.registry.ModMobEffects.WETNESS.get()) != null) {
                        com.bslx.XueXiMod.status.WetnessState.clear(living);
                    }
                    wrote = true;
                } else {
                    wrote = PrisonRuntime.settleCarrier(living, ticket.lockedTier(), ticket.reason());
                }
                if (wrote) {
                    data.remove(ticket.prisonId());
                }
            } catch (RuntimeException failure) {
                LOGGER.error("water prison wet ticket {} stayed pending", ticket.prisonId(), failure);
            } finally {
                APPLYING.remove(ticket.prisonId());
            }
        }
    }

    private void remove(UUID prisonId) {
        if (this.tickets.remove(prisonId) != null) {
            this.setDirty();
        }
    }

    public static PrisonWetDue get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PrisonWetDue::load, PrisonWetDue::fresh, STORAGE_ID);
    }

    public static PrisonWetDue fresh() {
        return new PrisonWetDue();
    }

    public static PrisonWetDue load(CompoundTag tag) {
        PrisonWetDue data = new PrisonWetDue();
        ListTag list = tag.getList("tickets", Tag.TAG_COMPOUND);
        for (int cursor = 0; cursor < list.size(); cursor++) {
            CompoundTag ticketTag = list.getCompound(cursor);
            if (!ticketTag.hasUUID("prisonId") || !ticketTag.hasUUID("targetId")) {
                continue;
            }
            ResourceLocation parsed = ResourceLocation.tryParse(ticketTag.getString("dimension"));
            if (parsed == null) {
                continue;
            }
            EndReason reason;
            try {
                reason = EndReason.valueOf(ticketTag.getString("reason"));
            } catch (IllegalArgumentException failure) {
                reason = EndReason.REMOVED;
            }
            UUID prisonId = ticketTag.getUUID("prisonId");
            data.tickets.put(prisonId, new Ticket(
                    prisonId,
                    ticketTag.getUUID("targetId"),
                    ResourceKey.create(Registries.DIMENSION, parsed),
                    ticketTag.getInt("lockedTier"),
                    reason));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Ticket ticket : this.tickets.values()) {
            CompoundTag ticketTag = new CompoundTag();
            ticketTag.putUUID("prisonId", ticket.prisonId());
            ticketTag.putUUID("targetId", ticket.targetId());
            ticketTag.putString("dimension", ticket.dimension().location().toString());
            ticketTag.putInt("lockedTier", ticket.lockedTier());
            ticketTag.putString("reason", ticket.reason().name());
            list.add(ticketTag);
        }
        tag.put("tickets", list);
        return tag;
    }

    public record Ticket(UUID prisonId, UUID targetId, ResourceKey<Level> dimension, int lockedTier, EndReason reason) {
    }
}
