package com.bslx.XueXiMod.shu.technique.release.statusquery;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;

/**
 * 扫描当时的不可变快照。距离和两张视图用于排序；主体引用要在稍后提交时重新检查。
 */
public record StatusCandidate(
        LivingEntity subject,
        UUID subjectId,
        double closestDistanceSquared,
        Map<ResourceLocation, ResourceView> resources,
        Map<ResourceLocation, EffectView> effects) {
    public StatusCandidate {
        if (subject == null || subjectId == null) {
            throw new IllegalArgumentException("subject");
        }
        if (Double.isNaN(closestDistanceSquared) || closestDistanceSquared < 0.0D
                || Double.isInfinite(closestDistanceSquared)) {
            throw new IllegalArgumentException("distance");
        }
        resources = freezeResources(resources);
        effects = freezeEffects(effects);
    }

    static Map<ResourceLocation, ResourceView> freezeResources(Map<ResourceLocation, ResourceView> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(source);
    }

    static Map<ResourceLocation, EffectView> freezeEffects(Map<ResourceLocation, EffectView> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(source);
    }
}
