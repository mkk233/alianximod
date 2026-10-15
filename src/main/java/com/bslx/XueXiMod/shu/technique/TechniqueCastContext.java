package com.bslx.XueXiMod.shu.technique;

import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

public class TechniqueCastContext {
    private final float damage;
    private final Vec3 look;
    private final Vec3 pos;
    private final EnchantmentResolution enchantments;
    private final Consumer<Vec3> onFirstTrigger;
    private final boolean lockThisCast;
    private final boolean dropThisCast;

    public TechniqueCastContext(float damage, Vec3 look, Vec3 pos) {
        this(damage, look, pos, EnchantmentResolution.empty(), null, false, false);
    }

    public TechniqueCastContext(float damage, Vec3 look, Vec3 pos, EnchantmentResolution enchantments) {
        this(damage, look, pos, enchantments, null, false, false);
    }

    public TechniqueCastContext(float damage, Vec3 look, Vec3 pos, EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger) {
        this(damage, look, pos, enchantments, onFirstTrigger, false, false);
    }

    public TechniqueCastContext(float damage, Vec3 look, Vec3 pos, EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger, boolean lockThisCast, boolean dropThisCast) {
        this.damage = damage;
        this.look = look;
        this.pos = pos;
        this.enchantments = enchantments;
        this.onFirstTrigger = onFirstTrigger;
        this.lockThisCast = lockThisCast;
        this.dropThisCast = dropThisCast;
    }

    public boolean lockThisCast() {
        return lockThisCast;
    }

    public boolean dropThisCast() {
        return dropThisCast;
    }

    public float getDamage() {
        return damage;
    }

    public Vec3 getLook() {
        return look;
    }

    public Vec3 getPos() {
        return pos;
    }

    public EnchantmentResolution enchantments() {
        return enchantments;
    }

    public Consumer<Vec3> onFirstTrigger() {
        return onFirstTrigger;
    }
}
