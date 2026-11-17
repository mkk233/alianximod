package com.bslx.XueXiMod.status.fire.furnace;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

/** 玩家持久数据里的炉心运行态。不放进 PlayerPersisted，死亡克隆不会自动带走。 */
final class FurnaceStore {
    static final String ROOT = "xueximod_furnace_core";
    private static final String VERSION = "Version";
    private static final String PLAYER_UUID = "PlayerUuid";
    private static final String AMPLIFIER = "Amplifier";
    private static final String BONUS = "Bonus";
    private static final String LAST_TICK = "LastTick";
    private static final String ENTERED_TICK = "EnteredTick";

    static final class Snapshot {
        private final FurnaceMath.Tier tier;
        private final double bonus;
        private final int lastTick;

        private Snapshot(FurnaceMath.Tier tier, double bonus, int lastTick) {
            this.tier = tier;
            this.bonus = bonus;
            this.lastTick = lastTick;
        }

        static Snapshot inactive() {
            return new Snapshot(null, 0.0D, 0);
        }

        FurnaceMath.Tier tier() {
            return this.tier;
        }

        double bonus() {
            return this.bonus;
        }

        int lastTick() {
            return this.lastTick;
        }
    }

    private FurnaceStore() {
    }

    /** 持久数据里有没有炉心根标签。 */
    static boolean present(Player player) {
        return player != null && player.getPersistentData().contains(ROOT);
    }

    /** 进入前的运行态副本。没有标签时返回空。 */
    static CompoundTag copyTag(Player player) {
        if (!present(player)) {
            return null;
        }
        return player.getPersistentData().getCompound(ROOT).copy();
    }

    /** 用进入前的副本盖回运行态。不改治疗效果，也不推进热量。 */
    static void replaceTag(Player player, CompoundTag storeBefore) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        if (storeBefore == null) {
            player.getPersistentData().remove(ROOT);
            return;
        }
        player.getPersistentData().put(ROOT, storeBefore.copy());
    }

    /** 删掉炉心运行态。 */
    static void clear(Player player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        player.getPersistentData().remove(ROOT);
    }

    /** 把旧玩家的炉心根标签整份抄到新玩家。 */
    static void copy(Player previous, Player successor) {
        if (previous == null || successor == null || !present(previous)) {
            return;
        }
        CompoundTag source = previous.getPersistentData().getCompound(ROOT);
        successor.getPersistentData().put(ROOT, source.copy());
    }

    /** 读运行态里收束后的档位。没有标签时是普通档。 */
    static int storedAmplifier(Player player) {
        CompoundTag stored = liveTag(player);
        if (stored == null) {
            return 0;
        }
        return FurnaceMath.legalAmplifier(stored.getInt(AMPLIFIER));
    }

    /** 有炉心效果时读档位、热量和上次推进的拍数。没有效果时视为未激活。 */
    static Snapshot snapshot(Player player) {
        MobEffectInstance instance = player.getEffect(FurnaceMobEffect.effect());
        if (instance == null || !(instance.getEffect() instanceof FurnaceMobEffect)) {
            return Snapshot.inactive();
        }
        CompoundTag stored = liveTag(player);
        double bonus = stored == null ? 0.0D : stored.getDouble(BONUS);
        int lastTick = stored == null ? player.tickCount : stored.getInt(LAST_TICK);
        return new Snapshot(FurnaceMath.tierOfAmplifier(instance.getAmplifier()), bonus, lastTick);
    }

    static void onAttached(Player player, int amplifier) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        int legal = FurnaceMath.legalAmplifier(amplifier);
        CompoundTag stored = liveTag(player);
        if (stored == null) {
            create(player, legal, 0.0D, player.tickCount);
            return;
        }
        stored.putInt(AMPLIFIER, legal);
        FurnaceMath.Sanitized sanitized = FurnaceMath.sanitize(
                stored.getInt(VERSION), legal, stored.getDouble(BONUS));
        stored.putInt(VERSION, FurnaceMath.CURRENT_VERSION);
        stored.putDouble(BONUS, sanitized.bonus());
        if (!stored.contains(ENTERED_TICK)) {
            stored.putInt(ENTERED_TICK, player.tickCount);
        }
        if (!stored.contains(PLAYER_UUID) || stored.getString(PLAYER_UUID).isEmpty()) {
            stored.putString(PLAYER_UUID, player.getUUID().toString());
        }
    }

    /** 损坏归零或越档钳制后写回。返回值表示这次写回了需要报告的修复。 */
    static boolean sanitizeInPlace(Player player, int amplifier) {
        CompoundTag stored = liveTag(player);
        if (stored == null || player.level().isClientSide()) {
            return false;
        }
        int legal = FurnaceMath.legalAmplifier(amplifier);
        boolean foreign = FurnaceMath.foreignOwner(stored.getString(PLAYER_UUID), player.getUUID().toString());
        FurnaceMath.Sanitized sanitized = FurnaceMath.sanitize(stored.getInt(VERSION), legal, stored.getDouble(BONUS));
        boolean zeroed = sanitized.zeroedBonus() || foreign;
        double bonus = zeroed ? 0.0D : sanitized.bonus();
        boolean changed = zeroed
                || sanitized.repairedAmplifier()
                || stored.getInt(AMPLIFIER) != legal
                || stored.getInt(VERSION) != FurnaceMath.CURRENT_VERSION
                || Double.compare(stored.getDouble(BONUS), bonus) != 0
                || !player.getUUID().toString().equals(stored.getString(PLAYER_UUID));
        if (!changed) {
            return false;
        }
        stored.putInt(VERSION, FurnaceMath.CURRENT_VERSION);
        stored.putString(PLAYER_UUID, player.getUUID().toString());
        stored.putInt(AMPLIFIER, legal);
        stored.putDouble(BONUS, bonus);
        if (sanitized.rejectedVersion()) {
            FurnaceLog.repair("炉心存档版本无法识别，热量已归零", player);
        } else if (foreign) {
            FurnaceLog.repair("炉心存档不属于当前玩家，热量已归零", player);
        } else if (sanitized.zeroedBonus()) {
            FurnaceLog.repair("炉心热量损坏，已归零", player);
        } else if (sanitized.repairedAmplifier()) {
            FurnaceLog.repair("炉心档位非法，已收束到普通或过载", player);
        }
        return sanitized.rejectedVersion() || foreign || sanitized.zeroedBonus() || sanitized.repairedAmplifier();
    }

    static void writeAdvance(Player player, double bonus, int clock) {
        CompoundTag stored = liveTag(player);
        if (stored == null || player.level().isClientSide()) {
            return;
        }
        stored.putDouble(BONUS, bonus);
        stored.putInt(LAST_TICK, clock);
    }

    /** 把上次推进拍数改成玩家当前拍数，避免离线间隔被乘进热量。 */
    static void resyncClock(Player player) {
        CompoundTag stored = liveTag(player);
        if (stored == null || player.level().isClientSide()) {
            return;
        }
        stored.putInt(LAST_TICK, player.tickCount);
    }

    private static void create(Player player, int amplifier, double bonus, int clock) {
        CompoundTag stored = new CompoundTag();
        stored.putInt(VERSION, FurnaceMath.CURRENT_VERSION);
        stored.putString(PLAYER_UUID, player.getUUID().toString());
        stored.putInt(AMPLIFIER, amplifier);
        stored.putDouble(BONUS, bonus);
        stored.putInt(LAST_TICK, clock);
        stored.putInt(ENTERED_TICK, clock);
        player.getPersistentData().put(ROOT, stored);
    }

    private static CompoundTag liveTag(Player player) {
        if (player == null || !player.getPersistentData().contains(ROOT)) {
            return null;
        }
        return player.getPersistentData().getCompound(ROOT);
    }
}
