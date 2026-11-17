package com.bslx.XueXiMod.shu.technique.release.deflection;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 一次近战或弹体接触的身份。victim 可以空，表示这条线段还没有合法受击者。
 * 只描述谁在打，不改宿主几何。
 */
public record DeflectionAttack(LivingEntity actor, Entity direct, LivingEntity victim, Kind kind) {
    public enum Kind {
        MELEE,
        PROJECTILE
    }
}
