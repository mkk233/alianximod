package com.bslx.XueXiMod.status.fire.furnace;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

/** 效果和运行态对齐。真正移除由效果事件清运行态；这里不负责蒸汽。 */
final class FurnaceRuntime {
    private FurnaceRuntime() {
    }

    /** 效果没了就清运行态和属性；效果还在就收束档位，缺运行态时按当前档位和 0 热量重建。 */
    static void reconcile(Player player) {
        if (player == null || player.level().isClientSide() || !FurnaceThreadGate.onServerThread(player)) {
            return;
        }
        MobEffectInstance instance = player.getEffect(FurnaceMobEffect.effect());
        if (!FurnaceMobEffect.isFurnace(instance)) {
            boolean hadStore = FurnaceStore.present(player);
            if (hadStore) {
                FurnaceStore.clear(player);
                FurnaceLog.repair("炉心效果已不在，清除运行态，不恢复效果、不释放蒸汽", player);
            }
            if (FurnaceAttributes.wearing(player)) {
                FurnaceAttributes.detach(player);
            }
            return;
        }
        boolean amplifierChanged = FurnaceMobEffect.normalize(instance);
        if (amplifierChanged) {
            FurnaceLog.repair("炉心档位非法，已收束到普通或过载", player);
        }
        if (!FurnaceStore.present(player)) {
            FurnaceLog.repair("炉心效果还在但运行态缺失，已按当前档位用热量0重建", player);
            FurnaceAttributes.attach(player, instance.getAmplifier());
            return;
        }
        boolean repaired = FurnaceStore.sanitizeInPlace(player, instance.getAmplifier());
        if (amplifierChanged || repaired || !FurnaceAttributes.wearing(player)) {
            FurnaceAttributes.attach(player, instance.getAmplifier());
        }
    }

    /** 在线推进一拍热量。没有炉心或这一拍已经记过时不写。 */
    static void advance(Player player) {
        if (player == null || player.level().isClientSide() || !FurnaceThreadGate.onServerThread(player)
                || !FurnaceStore.present(player)) {
            return;
        }
        MobEffectInstance instance = player.getEffect(FurnaceMobEffect.effect());
        if (!FurnaceMobEffect.isFurnace(instance)) {
            return;
        }
        FurnaceStore.Snapshot snapshot = FurnaceStore.snapshot(player);
        if (snapshot.tier() == null) {
            return;
        }
        long elapsedTicks = (long) player.tickCount - snapshot.lastTick();
        double nextBonus = FurnaceMath.advanceOnline(snapshot.bonus(), snapshot.tier(), elapsedTicks);
        if (elapsedTicks == 0L && Double.compare(nextBonus, snapshot.bonus()) == 0) {
            return;
        }
        FurnaceStore.writeAdvance(player, nextBonus, player.tickCount);
    }
}
