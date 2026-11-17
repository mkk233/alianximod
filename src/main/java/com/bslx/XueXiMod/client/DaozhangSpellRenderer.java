package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.npc.daozhang.DaozhangSpellProjectile;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

/** 原版投掷物模型，只作功能占位。 */
public class DaozhangSpellRenderer extends ThrownItemRenderer<DaozhangSpellProjectile> {
    public DaozhangSpellRenderer(EntityRendererProvider.Context context) {
        super(context, 1.0F, true);
    }
}
