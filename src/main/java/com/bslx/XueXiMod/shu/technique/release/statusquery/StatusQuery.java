package com.bslx.XueXiMod.shu.technique.release.statusquery;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** 只读条件。未知标识不命中；这里不发放、不对齐、不替换状态。 */
public final class StatusQuery {
    private final Matcher matcher;

    private StatusQuery(Matcher matcher) {
        if (matcher == null) {
            throw new IllegalArgumentException("matcher");
        }
        this.matcher = matcher;
    }

    public boolean matches(LivingEntity subject) {
        return this.matcher.matches(subject);
    }

    public static StatusQuery resourceAtLeast(ResourceLocation resourceKey, int lowestInclusive) {
        if (resourceKey == null) {
            throw new IllegalArgumentException("resourceKey");
        }
        if (lowestInclusive < 0) {
            throw new IllegalArgumentException("lowestInclusive");
        }
        return new StatusQuery(subject -> resourceMatches(subject, resourceKey, lowestInclusive));
    }

    public static StatusQuery effectActive(ResourceLocation effectKey) {
        if (effectKey == null) {
            throw new IllegalArgumentException("effectKey");
        }
        return new StatusQuery(subject -> effectCounts(subject, effectKey));
    }

    public static StatusQuery and(StatusQuery... queries) {
        if (queries == null) {
            throw new IllegalArgumentException("queries");
        }
        StatusQuery[] frozen = queries.clone();
        for (StatusQuery nested : frozen) {
            if (nested == null) {
                throw new IllegalArgumentException("query");
            }
        }
        return new StatusQuery(subject -> {
            boolean[] decisions = new boolean[frozen.length];
            for (int index = 0; index < frozen.length; index++) {
                decisions[index] = frozen[index].matches(subject);
            }
            return combine(decisions);
        });
    }

    static boolean combine(boolean... decisions) {
        if (decisions == null || decisions.length == 0) {
            return false;
        }
        for (boolean decision : decisions) {
            if (!decision) {
                return false;
            }
        }
        return true;
    }

    static boolean resourceMatches(LivingEntity subject, ResourceLocation resourceKey, int lowestInclusive) {
        if (resourceKey == null || lowestInclusive < 0) {
            return false;
        }
        Optional<ResourceStatus> found = ResourceStatuses.find(resourceKey);
        if (found.isEmpty()) {
            return false;
        }
        if (subject == null) {
            return false;
        }
        return matchesView(resourceKey, lowestInclusive, found.get().read(subject));
    }

    static boolean matchesView(ResourceLocation resourceKey, int lowestInclusive, ResourceView view) {
        if (resourceKey == null || lowestInclusive < 0 || view == null || view.identifier() == null) {
            return false;
        }
        if (!resourceKey.equals(view.identifier())) {
            return false;
        }
        return view.value() >= lowestInclusive;
    }

    static boolean effectCounts(boolean registered, boolean instancePresent, int durationTicks, boolean infinite) {
        if (!registered || !instancePresent) {
            return false;
        }
        return durationTicks > 0 || infinite;
    }

    static boolean effectCounts(LivingEntity subject, ResourceLocation effectKey) {
        if (effectKey == null) {
            return effectCounts(false, false, 0, false);
        }
        boolean registered = ForgeRegistries.MOB_EFFECTS.containsKey(effectKey);
        MobEffect effect = registered ? ForgeRegistries.MOB_EFFECTS.getValue(effectKey) : null;
        MobEffectInstance instance = effect == null || subject == null ? null : subject.getEffect(effect);
        int durationTicks = instance == null ? 0 : instance.getDuration();
        boolean infinite = instance != null && instance.isInfiniteDuration();
        return effectCounts(registered && effect != null, instance != null, durationTicks, infinite);
    }

    static Map<ResourceLocation, EffectView> activeEffects(LivingEntity subject) {
        if (subject == null) {
            return Map.of();
        }
        Map<ResourceLocation, EffectView> views = new HashMap<>();
        for (MobEffectInstance instance : subject.getActiveEffects()) {
            if (instance == null || instance.getEffect() == null) {
                continue;
            }
            ResourceLocation effectKey = ForgeRegistries.MOB_EFFECTS.getKey(instance.getEffect());
            if (effectKey == null) {
                continue;
            }
            int durationTicks = instance.getDuration();
            boolean infinite = instance.isInfiniteDuration();
            boolean present = effectCounts(true, true, durationTicks, infinite);
            views.put(effectKey, new EffectView(effectKey, present, instance.getAmplifier(), durationTicks));
        }
        return Map.copyOf(views);
    }

    @FunctionalInterface
    private interface Matcher {
        boolean matches(LivingEntity subject);
    }
}
