package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.shu.technique.release.fire.FireHurtBoundary;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** 在本配置应用火伤重定向之前，给 LivingEntity.hurt 加上令牌 finally。其它 mixin 不改。 */
public final class FireHurtMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (targetClass == null || mixinClassName == null || !mixinClassName.endsWith("FireResourceHurtMixin")) {
            return;
        }
        for (MethodNode method : targetClass.methods) {
            FireHurtBoundary.wrapMappedHurt(method);
        }
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
