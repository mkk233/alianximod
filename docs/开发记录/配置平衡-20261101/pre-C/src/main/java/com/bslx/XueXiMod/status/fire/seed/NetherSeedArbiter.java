package com.bslx.XueXiMod.status.fire.seed;

import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantNumbers;
import com.bslx.XueXiMod.status.fire.furnace.FurnaceService;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.UUID;

/**
 * 幽冥窗口产种仲裁。调用方必须把这一窗口内覆盖该目标的全部领域一次传入。
 * 只给最高请求入账一次；请求相同则用领域标识的字典序较小者。
 * 不占用命中产种的一次性标记，也不新建玩法施法。
 */
public final class NetherSeedArbiter {
    public static final String WINDOW_KEY = "xueximod_nether_seed_window";

    private NetherSeedArbiter() {
    }

    public static NetherWindowResult grant(LivingEntity target, long windowId, List<NetherOffer> offers) {
        if (target == null || !target.isAlive() || target.getUUID() == null || target.level() == null
                || target.level().isClientSide()) {
            return NetherWindowResult.rejected();
        }
        if (target.getPersistentData().contains(WINDOW_KEY, Tag.TAG_LONG)
                && target.getPersistentData().getLong(WINDOW_KEY) == windowId) {
            return NetherWindowResult.already();
        }
        NetherOffer winner = null;
        String winnerText = null;
        int winnerPoints = -1;
        if (offers != null) {
            for (NetherOffer offer : offers) {
                if (offer == null || offer.domainId() == null) {
                    continue;
                }
                double multiplier = FurnaceService.seedMultiplier(offer.owner());
                int points = FireSeedMath.floorProduction(ResourceEnchantNumbers.GLOOM_BASE, multiplier);
                String domainText = offer.domainId().toString();
                if (winner == null || points > winnerPoints
                        || (points == winnerPoints && domainText.compareTo(winnerText) < 0)) {
                    winner = offer;
                    winnerText = domainText;
                    winnerPoints = points;
                }
            }
        }
        if (winner == null || winnerPoints <= 0) {
            return NetherWindowResult.empty();
        }
        int increased = FireSeedState.depositWindow(target, winnerPoints);
        if (increased < 0 || !target.isAlive()) {
            return NetherWindowResult.rejected();
        }
        target.getPersistentData().putLong(WINDOW_KEY, windowId);
        return NetherWindowResult.granted(winner.domainId(), winnerPoints, increased);
    }

    public record NetherOffer(UUID domainId, LivingEntity owner) {
    }
}
