package com.bslx.XueXiMod.shu.technique.release.batch;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 幽冥的领域由释放链上的领域分支打开，产种挂在那一块真实范围上。
 * 这里不再扫描玩家已经存在的领域，避免单独释放去涂无关领域。
 */
public final class GloomDomainEffect implements TechniqueEffect {
    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
    }
}
