package com.bslx.XueXiMod.shu.technique.release.statusquery;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** 可注册的资源读口。实现只能读数值、档位和可选时长，不能改实体持久数据。 */
public interface ResourceStatus {
    ResourceLocation identifier();

    ResourceView read(LivingEntity subject);
}
