package com.bslx.XueXiMod.mixin.client.skin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

@Mixin(LivingEntityRenderer.class)
public interface LivingRendererLayersAccessor {
    @Accessor("layers")
    List<RenderLayer<?, ?>> xuexiLayers();
}
