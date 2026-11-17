package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.sword.SwordEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 列剑。只把玩家已有的可选剑原地停驻，不生成新剑，也不造成伤害。
 * 法力、冷却和装配只留命名常量，本次不注册。
 */
public final class SwordArrayEffect implements TechniqueEffect {
    public static final String TECHNIQUE_ID = "sword_array";
    public static final float MANA_COST = 10.0F;
    public static final int COOLDOWN_TICKS = 40;
    public static final int ASSEMBLY_COST = 1;
    public static final double SEARCH_RANGE = 16.0D;
    public static final int SWORD_LIMIT = 8;
    public static final double LIFT_BLOCKS = 2.0D;
    public static final int HOLD_TICKS = 600;

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        if (player == null || technique == null || !(player.level() instanceof ServerLevel level)
                || !TECHNIQUE_ID.equals(technique.getId())) {
            return;
        }
        Vec3 playerCenter = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        AABB searchBox = new AABB(playerCenter, playerCenter).inflate(SEARCH_RANGE);
        List<SwordEntity> selected = new ArrayList<>();
        for (SwordEntity sword : level.getEntitiesOfClass(SwordEntity.class, searchBox, SwordEntity::isAlive)) {
            if (sword.arraySelectable(player) && sword.position().distanceToSqr(playerCenter) <= SEARCH_RANGE * SEARCH_RANGE) {
                selected.add(sword);
            }
        }
        selected.sort(Comparator
                .comparingDouble((SwordEntity sword) -> sword.position().distanceToSqr(playerCenter))
                .thenComparingInt(SwordEntity::getId));
        if (selected.size() > SWORD_LIMIT) {
            selected.subList(SWORD_LIMIT, selected.size()).clear();
        }
        for (SwordEntity sword : selected) {
            sword.beginArrayHold();
        }
    }
}
