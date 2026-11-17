package com.bslx.XueXiMod.shu.technique.release.statusquery;

import com.bslx.XueXiMod.status.WetnessState;
import com.bslx.XueXiMod.status.fire.seed.FireSeedMath;
import com.bslx.XueXiMod.status.fire.seed.FireSeedState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 资源注册表。内建湿度与火种在首次查找或扫描时登记，读取不改数值、不刷新持续时间。 */
public final class ResourceStatuses {
    public static final ResourceLocation WETNESS = new ResourceLocation("xueximod", "wetness");
    public static final ResourceLocation FIRE_SEED = FireSeedState.IDENTIFIER;

    private static final Object GUARD = new Object();
    private static final Map<ResourceLocation, ResourceStatus> PROVIDERS = new HashMap<>();
    private static boolean builtinsRegistered;

    private ResourceStatuses() {
    }

    public static void registerBuiltins() {
        synchronized (GUARD) {
            if (builtinsRegistered) {
                return;
            }
            PROVIDERS.put(WETNESS, new WetnessStatus());
            PROVIDERS.put(FIRE_SEED, new FireSeedStatus());
            builtinsRegistered = true;
        }
    }

    public static void register(ResourceStatus provider) {
        if (provider == null || provider.identifier() == null) {
            throw new IllegalArgumentException("provider");
        }
        registerBuiltins();
        synchronized (GUARD) {
            ResourceLocation resourceKey = provider.identifier();
            ResourceStatus existing = PROVIDERS.get(resourceKey);
            if (existing != null) {
                throw new IllegalStateException("resource already registered: " + resourceKey);
            }
            PROVIDERS.put(resourceKey, provider);
        }
    }

    public static Optional<ResourceStatus> find(ResourceLocation resourceKey) {
        registerBuiltins();
        if (resourceKey == null) {
            return Optional.empty();
        }
        synchronized (GUARD) {
            return Optional.ofNullable(PROVIDERS.get(resourceKey));
        }
    }

    static List<ResourceStatus> registered() {
        registerBuiltins();
        synchronized (GUARD) {
            return List.copyOf(PROVIDERS.values());
        }
    }

    static Map<ResourceLocation, ResourceView> readAll(LivingEntity subject) {
        Map<ResourceLocation, ResourceView> views = new HashMap<>();
        for (ResourceStatus provider : registered()) {
            if (provider == null || provider.identifier() == null) {
                continue;
            }
            ResourceView view = provider.read(subject);
            if (view == null || !provider.identifier().equals(view.identifier())) {
                continue;
            }
            views.put(view.identifier(), view);
        }
        return Map.copyOf(views);
    }

    private static final class WetnessStatus implements ResourceStatus {
        @Override
        public ResourceLocation identifier() {
            return WETNESS;
        }

        @Override
        public ResourceView read(LivingEntity subject) {
            int amount = WetnessState.read(subject);
            int tier = WetnessState.tierOf(amount);
            int durationTicks = WetnessState.remainingDuration(subject);
            return new ResourceView(WETNESS, amount, tier, durationTicks);
        }
    }

    private static final class FireSeedStatus implements ResourceStatus {
        @Override
        public ResourceLocation identifier() {
            return FIRE_SEED;
        }

        @Override
        public ResourceView read(LivingEntity subject) {
            int amount = FireSeedState.value(subject);
            return new ResourceView(FIRE_SEED, amount, FireSeedMath.tiers(amount), FireSeedState.UNLIMITED_DURATION);
        }
    }
}
