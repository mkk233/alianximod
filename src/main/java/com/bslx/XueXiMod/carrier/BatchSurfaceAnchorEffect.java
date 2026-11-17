package com.bslx.XueXiMod.carrier;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/** 地表定位的显示。落点仍由索敌角色决定，这里只在锚点上播 16 帧。 */
public final class BatchSurfaceAnchorEffect implements TechniqueEffect {
    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        if (player.level() instanceof ServerLevel serverLevel) {
            CastFrame frame = CastFrame.current();
            Vec3 point = frame != null && frame.anchor != null ? frame.anchor
                    : (cast.getPos() != null ? cast.getPos() : player.position());
            BatchTriggerVisuals.surface(serverLevel, point);
        }
        Consumer<Vec3> trigger = cast.onFirstTrigger();
        if (trigger != null) {
            Vec3 point = cast.getPos() != null ? cast.getPos() : player.position();
            trigger.accept(point);
        }
    }
}
