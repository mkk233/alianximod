package com.bslx.XueXiMod.shu.technique;

import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

public class TechniqueCastContext {
    private final float damage;
    private final Vec3 look;
    private final Vec3 position;
    private final EnchantmentResolution enchantments;
    private final Consumer<Vec3> onFirstTrigger;
    private final boolean lockThisCast;
    private final boolean dropThisCast;
    private final ActionWindow.Sink actionSink;

    public TechniqueCastContext(float damage, Vec3 look, Vec3 position) {
        this(damage, look, position, EnchantmentResolution.empty(), null, false, false, null);
    }

    public TechniqueCastContext(float damage, Vec3 look, Vec3 position, EnchantmentResolution enchantments) {
        this(damage, look, position, enchantments, null, false, false, null);
    }

    public TechniqueCastContext(float damage, Vec3 look, Vec3 position, EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger) {
        this(damage, look, position, enchantments, onFirstTrigger, false, false, null);
    }

    public TechniqueCastContext(float damage, Vec3 look, Vec3 position, EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger, boolean lockThisCast, boolean dropThisCast) {
        this(damage, look, position, enchantments, onFirstTrigger, lockThisCast, dropThisCast, null);
    }

    public TechniqueCastContext(float damage, Vec3 look, Vec3 position, EnchantmentResolution enchantments, Consumer<Vec3> onFirstTrigger, boolean lockThisCast, boolean dropThisCast, ActionWindow.Sink actionSink) {
        this.damage = damage;
        this.look = look;
        this.position = position;
        this.enchantments = enchantments;
        this.onFirstTrigger = onFirstTrigger;
        this.lockThisCast = lockThisCast;
        this.dropThisCast = dropThisCast;
        this.actionSink = actionSink;
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
        return position;
    }

    public EnchantmentResolution enchantments() {
        return enchantments;
    }

    public Consumer<Vec3> onFirstTrigger() {
        return onFirstTrigger;
    }

    public ActionWindow.Sink actionSink() {
        return actionSink;
    }
}
