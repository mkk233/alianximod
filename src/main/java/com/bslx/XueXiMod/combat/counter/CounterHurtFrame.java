package com.bslx.XueXiMod.combat.counter;

/**
 * 同一次 {@code LivingEntity.hurt} 的调用帧。嵌套调用各有一帧，不用受害者、来源和 tick 拼键。
 */
public final class CounterHurtFrame {
    String ownerKey;
    String attackerKey;
    String dimensionKey;
    float counterDamage;
    long generation;
    boolean open = true;
    boolean reserved;
    boolean hurtCanceled;
    boolean damageReached;
    boolean damageCanceled;

    CounterHurtFrame() {
    }

    public boolean open() {
        return this.open;
    }

    public boolean reserved() {
        return this.reserved;
    }

    public String ownerKey() {
        return this.ownerKey;
    }
}
