package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

/** 雾幕专用着色器。雨和其他材质继续用原版实体透明 shader。 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DomainMistShaders {
    private static final ResourceLocation CURTAIN_LOCATION =
            new ResourceLocation(XueXiMod.MOD_ID, "rendertype_entity_translucent_mist");
    private static ShaderInstance curtainShader;

    private DomainMistShaders() {
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstance(event.getResourceProvider(), CURTAIN_LOCATION, DefaultVertexFormat.NEW_ENTITY),
                    loaded -> curtainShader = loaded);
        } catch (IOException failure) {
            throw new IllegalStateException("雾幕着色器未能注册", failure);
        }
    }

    static ShaderInstance curtain() {
        return curtainShader;
    }
}
