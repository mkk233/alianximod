package com.bslx.XueXiMod.carrier;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * 喷涌、错时、接续和资源附魔槽。没有新贴图，也不复制账本。
 * 效果为空时链会在进入角色扫描前返回，所以这里只把下一段交回去。
 */
public final class BatchRulePassEffect implements TechniqueEffect {
    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        Consumer<Vec3> trigger = cast.onFirstTrigger();
        if (trigger == null) {
            return;
        }
        Vec3 point = cast.getPos() != null ? cast.getPos() : player.position();
        trigger.accept(point);
    }
}
