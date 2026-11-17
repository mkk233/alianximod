package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.sword.SwordFlightMount;
import com.bslx.XueXiMod.shu.technique.sword.SwordFlightReturn;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 御剑。只接受背包中的真剑，生成承载后由承载会话维持。
 * 注册交给后续唯一作者。空手资格只识别本效果的背包真剑来源。
 */
public final class SwordFlightEffect implements TechniqueEffect {
    public static final String TECHNIQUE_ID = "sword_flight";
    public static final int STARTUP_MANA = 10;
    public static final int HOVER_MANA = 1;
    public static final int FLIGHT_MANA = 5;
    public static final int BILLING_PERIOD = 20;
    public static final int END_COOLDOWN_TICKS = 100;
    public static final int ASSEMBLY_COST = 2;
    public static final int LEVEL_CAP = 20;
    public static final int COMPLEXITY = 3;
    /** 鞘翅烟花加速使用的每拍推进量。 */
    public static final double FIREWORK_BOOST = 1.5D;
    public static final double HOVER_DRAG = 0.91D;
    public static final double RETURN_REACH = 8.0D;

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        if (player == null || technique == null || !TECHNIQUE_ID.equals(technique.getId())) {
            return;
        }
        SwordFlightMount.begin(player, releaseSlot(player));
    }

    /** 只放行御剑自己的背包真剑来源，不放宽其他操纵术式。 */
    public static boolean clientHasInventorySource(Iterable<Technique> techniques) {
        if (techniques == null) {
            return false;
        }
        for (Technique technique : techniques) {
            if (technique != null && TECHNIQUE_ID.equals(technique.getId())) {
                return true;
            }
        }
        return false;
    }

    /** 服务端同样只看定义能力。真正有没有剑由启动时再查。 */
    public static boolean serverAllowsInventorySource(Iterable<Technique> techniques, ServerPlayer player) {
        if (player == null || !clientHasInventorySource(techniques)) {
            return false;
        }
        return SwordFlightReturn.firstRealSwordSlot(player) >= 0;
    }

    private static int releaseSlot(ServerPlayer player) {
        com.bslx.XueXiMod.shu.technique.release.ReleaseBatch batch =
                com.bslx.XueXiMod.shu.technique.release.ReleaseBatch.current();
        return batch == null ? -1 : batch.sustainSlot();
    }
}
