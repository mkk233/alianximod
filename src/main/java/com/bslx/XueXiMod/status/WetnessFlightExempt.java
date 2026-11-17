package com.bslx.XueXiMod.status;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 潮湿下降黑名单。只比较注册名语法，加载时不查注册表。 */
public final class WetnessFlightExempt {
    private static final ResourceLocation DEFAULT_DRAGON = new ResourceLocation("minecraft", "ender_dragon");
    private static volatile Set<ResourceLocation> exemptIds = Set.of(DEFAULT_DRAGON);

    private WetnessFlightExempt() {}

    /** 类初始化已经放入默认末影龙。配置加载前调用一次，避免第一次读取发生在加载之后。 */
    public static void ensureDefault() {
        if (exemptIds == null) {
            exemptIds = Set.of(DEFAULT_DRAGON);
        }
    }

    /** 文本是不是恰好一个合法且规范的注册名。 */
    public static boolean validSyntax(Object candidate) {
        return candidate instanceof String text && exactId(text) != null;
    }

    /** 空列表就是没有任何豁免，不退回默认值。语法不对的条目丢掉，合法但未注册的名字保留。 */
    public static void replace(List<? extends String> configured) {
        Set<ResourceLocation> accepted = new HashSet<>();
        if (configured != null) {
            for (String text : configured) {
                ResourceLocation parsed = exactId(text);
                if (parsed != null) {
                    accepted.add(parsed);
                }
            }
        }
        exemptIds = Set.copyOf(accepted);
    }

    /** 实体类型的注册名在不在当前黑名单里。 */
    public static boolean isExempt(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return exemptIds.contains(EntityType.getKey(entity.getType()));
    }

    private static ResourceLocation exactId(String text) {
        if (text == null) {
            return null;
        }
        int separator = text.indexOf(':');
        if (separator <= 0 || separator != text.lastIndexOf(':')) {
            return null;
        }
        if (!ResourceLocation.isValidResourceLocation(text)) {
            return null;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(text);
        if (parsed == null || !text.equals(parsed.toString())) {
            return null;
        }
        return parsed;
    }
}
