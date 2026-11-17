package com.bslx.XueXiMod.shu.technique.release.fire;

import com.bslx.XueXiMod.status.fire.seed.FireSeedState;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 燃烧记在目标的持久数据里。卸载后不再推进，加载后从剩余等待继续，不补打离线各段。 */
public final class BurnCarrier {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String GENERATION_KEY = "xueximod_fire_burn_gen";

    private BurnCarrier() {
    }

    public static void tick(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        if (!entity.isAlive() || entity.isRemoved()) {
            if (entity.getPersistentData().contains(FireLimits.SAVE_KEY, Tag.TAG_COMPOUND)) {
                clear(entity);
            }
            return;
        }
        long turn = entity.level().getServer() == null ? 0L : entity.level().getServer().getTickCount();
        BurnDispatch.tick(new EntityMemory(entity), pulse ->
                FireDamage.strikeOnce(entity, pulse.casterId(), pulse.damage(), 0.0d, true),
                new EntityPurse(entity), turn);
    }

    /** 燎原当拍首段。伤害已含炉心，火种基数传 0，不再乘第二次。 */
    public static void settleOpening(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide() || !entity.isAlive() || entity.isRemoved()) {
            return;
        }
        long turn = entity.level().getServer() == null ? 0L : entity.level().getServer().getTickCount();
        BurnDispatch.settleOpening(new EntityMemory(entity), pulse ->
                FireDamage.strikeOnce(entity, pulse.casterId(), pulse.damage(), 0.0d, true),
                new EntityPurse(entity), turn);
    }

    public static BurnLedger read(LivingEntity entity) {
        if (entity == null) {
            return new BurnLedger();
        }
        CompoundTag persistent = entity.getPersistentData();
        if (!persistent.contains(FireLimits.SAVE_KEY, Tag.TAG_COMPOUND)) {
            return new BurnLedger();
        }
        CompoundTag root = persistent.getCompound(FireLimits.SAVE_KEY);
        int version = root.getInt("version");
        if (version != FireLimits.PREPAID_VERSION && version != FireLimits.NBT_VERSION) {
            LOGGER.warn("drop fire burn data with version {} on {}", version, entity.getUUID());
            return new BurnLedger();
        }
        ListTag plans = root.getList("plans", Tag.TAG_COMPOUND);
        int readable = plans.size();
        if (readable > FireLimits.PLAN_CAP) {
            readable = FireLimits.PLAN_CAP;
        }
        ArrayList<BurnTrackState> states = new ArrayList<>();
        int skipped = 0;
        for (int cursor = 0; cursor < readable; cursor++) {
            BurnTrackState state = decode(plans.getCompound(cursor), version);
            if (state == null) {
                skipped += 1;
                continue;
            }
            states.add(state);
        }
        BurnLedger ledger = BurnLedger.restore(states);
        int unread = plans.size() - readable;
        int dropped = skipped + unread + ledger.droppedOnRestore();
        if (dropped > 0) {
            LOGGER.warn("dropped {} fire burn plans on {}", dropped, entity.getUUID());
        }
        return ledger;
    }

    public static void write(LivingEntity entity, BurnLedger ledger) {
        if (entity == null) {
            return;
        }
        if (ledger == null || ledger.trackCount() == 0) {
            entity.getPersistentData().remove(FireLimits.SAVE_KEY);
            return;
        }
        CompoundTag root = new CompoundTag();
        root.putInt("version", FireLimits.NBT_VERSION);
        ListTag plans = new ListTag();
        for (BurnTrackState state : ledger.export()) {
            plans.add(encode(state));
        }
        root.put("plans", plans);
        entity.getPersistentData().put(FireLimits.SAVE_KEY, root);
    }

    public static void clear(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        CompoundTag persistent = entity.getPersistentData();
        persistent.remove(FireLimits.SAVE_KEY);
        persistent.putLong(GENERATION_KEY, persistent.getLong(GENERATION_KEY) + 1L);
    }

    public static void copy(LivingEntity original, LivingEntity fresh) {
        if (original == null || fresh == null || original == fresh) {
            return;
        }
        CompoundTag source = original.getPersistentData();
        if (!source.contains(FireLimits.SAVE_KEY, Tag.TAG_COMPOUND)) {
            return;
        }
        fresh.getPersistentData().put(FireLimits.SAVE_KEY, source.getCompound(FireLimits.SAVE_KEY).copy());
    }

    private static CompoundTag encode(BurnTrackState state) {
        CompoundTag tag = new CompoundTag();
        tag.putString("effect", state.effectKey());
        tag.putBoolean("exclusive", state.targetExclusive());
        tag.putUUID("cast", state.castId());
        if (state.casterId() != null) {
            tag.putUUID("caster", state.casterId());
        }
        tag.putUUID("target", state.targetId());
        tag.putInt("amount", state.amount());
        tag.putInt("tiers", state.tiers());
        tag.putInt("remainder", state.remainder());
        tag.putDouble("furnace", state.furnaceBonus());
        tag.putInt("interval", state.intervalTicks());
        tag.putInt("wait", state.ticksUntilNext());
        tag.putInt("cursor", state.segmentCursor());
        tag.putString("fuel", state.sharedFuel() ? FuelMode.SHARED : FuelMode.PREPAID);
        if (state.sharedFuel()) {
            tag.putInt("demand", state.segmentDemand());
            tag.putDouble("full", state.fullSegmentDamage());
            tag.putInt("limit", state.segmentLimit());
            tag.putLong("layer", state.layerIdentity());
            return tag;
        }
        ListTag damages = new ListTag();
        for (Double damage : state.damages()) {
            damages.add(DoubleTag.valueOf(damage));
        }
        tag.put("damages", damages);
        return tag;
    }

    private static BurnTrackState decode(CompoundTag tag, int version) {
        if (tag == null || !tag.hasUUID("cast") || !tag.hasUUID("target")) {
            return null;
        }
        String effectKey = tag.getString("effect");
        if (effectKey.length() > FireLimits.KEY_CAP) {
            return null;
        }
        UUID casterId = tag.hasUUID("caster") ? tag.getUUID("caster") : null;
        String kind = FuelMode.kind(version, tag.getString("fuel"));
        if (FuelMode.SHARED.equals(kind)) {
            return new BurnTrackState(effectKey, false, tag.getUUID("cast"), casterId, tag.getUUID("target"),
                    tag.getInt("amount"), tag.getInt("tiers"), tag.getInt("remainder"), tag.getDouble("furnace"),
                    tag.getInt("interval"), tag.getInt("wait"), tag.getInt("cursor"), List.of(), FuelMode.SHARED,
                    tag.getInt("demand"), tag.getDouble("full"), tag.getInt("limit"), tag.getLong("layer"));
        }
        ListTag damages = tag.getList("damages", Tag.TAG_DOUBLE);
        if (damages.isEmpty() || damages.size() > FireLimits.SEGMENT_CAP) {
            return null;
        }
        ArrayList<Double> values = new ArrayList<>();
        for (int cursor = 0; cursor < damages.size(); cursor++) {
            values.add(damages.getDouble(cursor));
        }
        return new BurnTrackState(effectKey, tag.getBoolean("exclusive"), tag.getUUID("cast"), casterId,
                tag.getUUID("target"), tag.getInt("amount"), tag.getInt("tiers"), tag.getInt("remainder"),
                tag.getDouble("furnace"), tag.getInt("interval"), tag.getInt("wait"), tag.getInt("cursor"), values);
    }

    private static final class EntityMemory implements BurnDispatch.Memory {
        private final LivingEntity entity;

        private EntityMemory(LivingEntity entity) {
            this.entity = entity;
        }

        @Override
        public BurnLedger read() {
            return BurnCarrier.read(this.entity);
        }

        @Override
        public void write(BurnLedger ledger) {
            BurnCarrier.write(this.entity, ledger);
        }

        @Override
        public long generation() {
            return this.entity.getPersistentData().getLong(GENERATION_KEY);
        }

        @Override
        public void discard() {
            BurnCarrier.clear(this.entity);
        }

        @Override
        public boolean alive() {
            return this.entity.isAlive() && !this.entity.isRemoved();
        }
    }

    private static final class EntityPurse implements BurnDispatch.FuelPurse {
        private final LivingEntity entity;

        private EntityPurse(LivingEntity entity) {
            this.entity = entity;
        }

        @Override
        public int balance() {
            return FireSeedState.value(this.entity);
        }

        @Override
        public void commitSpent(int spent) {
            FireSeedState.draw(this.entity, spent);
        }
    }
}
