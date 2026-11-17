package com.bslx.XueXiMod.status;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class AttributeStateEvents {
    private AttributeStateEvents() {}

    /** 生物进世界后，让潮湿的精确数值和药水效果重新对齐。 */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        WetnessState.reconcile(living);
    }
}
