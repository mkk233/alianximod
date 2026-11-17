package com.bslx.XueXiMod.api;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueManager;
import com.bslx.XueXiMod.shu.technique.EnchantmentDefinition;
import com.bslx.XueXiMod.shu.technique.EnchantmentRegistry;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.TechniqueEffectRegistry;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import org.slf4j.Logger;

import java.util.Set;

/**
 * 别的模组在本模组登记完内置术式之后，往同一条链里加术式或附魔。
 * 监听本模组的 MOD 总线，客户端和专用服务器都要登记同一批 id。
 */
public class RegisterXueXiContentEvent extends Event implements IModBusEvent {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> ENCHANT_EFFECTS = Set.of("fire", "wood", "water", "earth", "metal");

    public boolean registerCast(Technique technique, TechniqueEffect effect, ReleaseDefinition definition) {
        if (technique == null || effect == null || definition == null) {
            LOGGER.warn("拒绝登记术式：参数为空");
            return false;
        }
        String techniqueId = technique.getId();
        if (techniqueId == null || techniqueId.isEmpty() || !techniqueId.equals(definition.id())) {
            LOGGER.warn("拒绝登记术式：术式 id 与释放定义不一致");
            return false;
        }
        if (occupied(techniqueId)) {
            LOGGER.warn("拒绝登记术式 {}：id 已存在", techniqueId);
            return false;
        }
        TechniqueManager.register(technique);
        TechniqueEffectRegistry.register(techniqueId, effect);
        ReleaseRegistry.register(definition);
        return true;
    }

    public boolean registerEnchantment(Technique technique, EnchantmentDefinition definition) {
        if (technique == null || definition == null) {
            LOGGER.warn("拒绝登记附魔：参数为空");
            return false;
        }
        String techniqueId = technique.getId();
        if (techniqueId == null || techniqueId.isEmpty() || !techniqueId.equals(definition.id())) {
            LOGGER.warn("拒绝登记附魔：术式 id 与附魔定义不一致");
            return false;
        }
        if (occupied(techniqueId)) {
            LOGGER.warn("拒绝登记附魔 {}：id 已存在", techniqueId);
            return false;
        }
        String effectId = definition.createAccumulator() == null ? null : definition.createAccumulator().effectId();
        if (!ENCHANT_EFFECTS.contains(effectId)) {
            LOGGER.warn("拒绝登记附魔 {}：effectId 只能是 fire、wood、water、earth、metal", techniqueId);
            return false;
        }
        TechniqueManager.register(technique);
        EnchantmentRegistry.register(definition);
        return true;
    }

    private static boolean occupied(String techniqueId) {
        return TechniqueManager.getById(techniqueId) != null
                || TechniqueEffectRegistry.get(techniqueId) != null
                || ReleaseRegistry.get(techniqueId) != null
                || EnchantmentRegistry.get(techniqueId) != null;
    }
}
