package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.status.WetnessFlight;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** 字节码里这次父类调用的 owner 是 Monster.move，不是 Entity.move。 */
@Mixin(Vex.class)
public class WetnessVexMixin {
    @ModifyArg(method = "move",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/monster/Monster;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"),
            index = 1,
            require = 1)
    private Vec3 xuexiWetnessVexMove(MoverType type, Vec3 movement) {
        return WetnessFlight.adjustVexMove((LivingEntity) (Object) this, type, movement);
    }
}
