package com.bslx.XueXiMod.shu.technique.enchant.resource;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.EnchantmentDefinition;
import com.bslx.XueXiMod.shu.technique.EnchantmentRegistry;
import com.bslx.XueXiMod.skill.Skill;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** 从已登记定义收集新载荷。相同标识只留一份效果，每个原装配格仍各计一次魔力。 */
public final class ResourceEnchantCatalog {
    private ResourceEnchantCatalog() {
    }

    public static ResourceEnchantDefinition find(String enchantmentId) {
        EnchantmentDefinition definition = EnchantmentRegistry.get(enchantmentId);
        if (definition instanceof ResourceEnchantDefinition resource) {
            return resource;
        }
        return null;
    }

    /**
     * 新附魔的显式魔力。返回负数表示这不是本批资源附魔，调用方继续走旧 magicDamage 报价。
     */
    public static int explicitManaPrice(String enchantmentId) {
        ResourceEnchantDefinition definition = find(enchantmentId);
        if (definition == null) {
            return -1;
        }
        return definition.manaPrice();
    }

    public static ResourceEnchantLoad collect(Skill skill, List<Integer> indexes) {
        if (skill == null || indexes == null || indexes.isEmpty()) {
            return ResourceEnchantLoad.EMPTY;
        }
        List<Technique> techniques = skill.getTechniques();
        int manaPrice = 0;
        int directDamage = 0;
        int scaledSeedBase = 0;
        boolean kindle = false;
        boolean detonate = false;
        boolean trail = false;
        boolean gloom = false;
        boolean breakGround = false;
        LinkedHashMap<String, ResourceEnchantSlice> slices = new LinkedHashMap<>();
        for (int index : indexes) {
            if (index < 0 || index >= techniques.size()) {
                continue;
            }
            Technique technique = techniques.get(index);
            if (technique == null) {
                continue;
            }
            ResourceEnchantDefinition definition = find(technique.getId());
            if (definition == null) {
                continue;
            }
            manaPrice = addPrice(manaPrice, definition.manaPrice());
            ResourceEnchantSlice slice = definition.slice();
            if (slices.containsKey(definition.id())) {
                continue;
            }
            slices.put(definition.id(), slice);
            directDamage = addPrice(directDamage, slice.directDamage());
            scaledSeedBase = addPrice(scaledSeedBase, slice.scaledSeedBase());
            kindle = kindle || slice.hasKindle();
            detonate = detonate || slice.hasDetonate();
            trail = trail || slice.hasTrail();
            gloom = gloom || slice.hasGloom();
            breakGround = breakGround || slice.hasBreakGround();
        }
        if (slices.isEmpty()) {
            return ResourceEnchantLoad.EMPTY;
        }
        return new ResourceEnchantLoad(manaPrice, directDamage, scaledSeedBase, kindle, detonate, trail, gloom,
                breakGround, List.copyOf(new ArrayList<>(slices.values())));
    }

    private static int addPrice(int current, int extra) {
        long combined = (long) current + (long) extra;
        if (combined > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (combined < 0L) {
            return 0;
        }
        return (int) combined;
    }
}
