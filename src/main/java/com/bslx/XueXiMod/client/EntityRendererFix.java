package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.carrier.BatchCarrierTypes;
import com.bslx.XueXiMod.terrain.MeteoriteWreckageTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * 进世界时 Forge 会换掉实体类型对象。渲染表如果还拿着旧对象，自定义实体一渲染就空指针。
 */
public final class EntityRendererFix {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation HIT_ID = new ResourceLocation(XueXiMod.MOD_ID, "technique_hit");
    private static final ResourceLocation SWORD_ID = new ResourceLocation(XueXiMod.MOD_ID, "sword_entity");
    private static final ResourceLocation BARRIER_ID = new ResourceLocation(XueXiMod.MOD_ID, "barrier_body");
    private static final ResourceLocation WRECKAGE_ID =
            new ResourceLocation(XueXiMod.MOD_ID, MeteoriteWreckageTypes.ENTITY_ID);
    private static final ResourceLocation CARRIER_VISUAL_ID =
            new ResourceLocation(XueXiMod.MOD_ID, BatchCarrierTypes.ENTITY_ID);
    private static ItemInHandRenderer hands;
    private static int gate;
    private static boolean settled;

    private EntityRendererFix() {}

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            settled = false;
            return;
        }
        if (settled && (++gate % 20) != 0) {
            return;
        }
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        Map<EntityType<?>, EntityRenderer<?>> current = dispatcher.renderers;
        EntityType<?> hit = ForgeRegistries.ENTITY_TYPES.getValue(HIT_ID);
        EntityType<?> sword = ForgeRegistries.ENTITY_TYPES.getValue(SWORD_ID);
        EntityType<?> barrier = ForgeRegistries.ENTITY_TYPES.getValue(BARRIER_ID);
        EntityType<?> wreckage = ForgeRegistries.ENTITY_TYPES.getValue(WRECKAGE_ID);
        EntityType<?> carrierVisual = ForgeRegistries.ENTITY_TYPES.getValue(CARRIER_VISUAL_ID);
        boolean missingHit = hit != null && !current.containsKey(hit);
        boolean missingSword = sword != null && !current.containsKey(sword);
        boolean missingBarrier = barrier != null && !current.containsKey(barrier);
        boolean missingWreckage = wreckage != null && !current.containsKey(wreckage);
        boolean missingCarrier = carrierVisual != null && !current.containsKey(carrierVisual);
        if (!missingHit && !missingSword && !missingBarrier && !missingWreckage && !missingCarrier) {
            settled = true;
            return;
        }
        Map<EntityType<?>, EntityRenderer<?>> copy = new HashMap<>(current);
        EntityRendererProvider.Context context = context(minecraft, dispatcher);
        if (missingHit) {
            EntityRenderer<?> existing = find(current, TechniqueHitRenderer.class);
            copy.put(hit, existing != null ? existing : new TechniqueHitRenderer(context));
        }
        if (missingSword) {
            EntityRenderer<?> existing = find(current, SwordEntityRenderer.class);
            copy.put(sword, existing != null ? existing : new SwordEntityRenderer(context));
        }
        if (missingBarrier) {
            EntityRenderer<?> existing = find(current, WaterBarrierRenderer.class);
            copy.put(barrier, existing != null ? existing : new WaterBarrierRenderer(context));
        }
        if (missingWreckage) {
            EntityRenderer<?> existing = find(current, MeteoriteWreckageRenderer.class);
            copy.put(wreckage, existing != null ? existing : new MeteoriteWreckageRenderer(context));
        }
        if (missingCarrier) {
            EntityRenderer<?> existing = find(current, BatchCarrierVisualRenderer.class);
            copy.put(carrierVisual, existing != null ? existing : new BatchCarrierVisualRenderer(context));
        }
        dispatcher.renderers = copy;
        settled = true;
        LOGGER.info("已把术式实体渲染器重新绑到当前注册表");
    }

    private static EntityRenderer<?> find(Map<EntityType<?>, EntityRenderer<?>> current, Class<?> type) {
        for (EntityRenderer<?> renderer : current.values()) {
            if (type.isInstance(renderer)) {
                return renderer;
            }
        }
        return null;
    }

    private static EntityRendererProvider.Context context(Minecraft minecraft, EntityRenderDispatcher dispatcher) {
        return new EntityRendererProvider.Context(
                dispatcher,
                minecraft.getItemRenderer(),
                minecraft.getBlockRenderer(),
                hands(dispatcher),
                minecraft.getResourceManager(),
                minecraft.getEntityModels(),
                minecraft.font);
    }

    private static ItemInHandRenderer hands(EntityRenderDispatcher dispatcher) {
        if (hands != null) {
            return hands;
        }
        try {
            Field field = EntityRenderDispatcher.class.getDeclaredField("itemInHandRenderer");
            field.setAccessible(true);
            hands = (ItemInHandRenderer) field.get(dispatcher);
            return hands;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("读不到物品手持渲染器", exception);
        }
    }
}
