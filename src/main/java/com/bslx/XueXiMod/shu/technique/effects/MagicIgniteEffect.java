package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class MagicIgniteEffect implements TechniqueEffect {
    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        Vec3 look = cast.getLook();
        Vec3 pos = cast.getPos();

        // 点燃前方实体
        Vec3 target = pos.add(look.scale(5));
        AABB box = new AABB(pos, target).inflate(0.5);
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive());
        if (!entities.isEmpty()) {
            entities.get(0).setSecondsOnFire(5);
        }
    }
}
