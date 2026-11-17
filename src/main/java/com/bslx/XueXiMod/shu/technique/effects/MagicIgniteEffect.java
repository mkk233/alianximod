package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.release.bind.PublicationAttempt;
import com.bslx.XueXiMod.shu.technique.release.relations.HarmPolicy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class MagicIgniteEffect implements TechniqueEffect {
    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        PublicationAttempt attempt = PublicationAttempt.current();
        if (attempt != null && !attempt.allowsPublication()) {
            return;
        }
        Vec3 look = cast.getLook();
        Vec3 castPoint = cast.getPos();

        // 点燃前方实体
        Vec3 targetPoint = castPoint.add(look.scale(5));
        AABB box = new AABB(castPoint, targetPoint).inflate(0.5);
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != player && entity.isAlive());
        if (!entities.isEmpty()) {
            LivingEntity target = entities.get(0);
            if (HarmPolicy.denies(player, target)) {
                return;
            }
            target.setSecondsOnFire(5);
            if (attempt != null) {
                attempt.reportAccepted();
            }
        }
    }
}
