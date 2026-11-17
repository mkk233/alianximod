package com.bslx.XueXiMod.mixin.client.skin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;

@Mixin(HumanoidArmorLayer.class)
public interface HumanoidArmorLayerAccess {
    @Accessor("innerModel")
    HumanoidModel<?> xuexiInnerModel();

    @Accessor("outerModel")
    HumanoidModel<?> xuexiOuterModel();

    @Invoker("usesInnerModel")
    boolean xuexiUsesInnerModel(EquipmentSlot slot);
}
