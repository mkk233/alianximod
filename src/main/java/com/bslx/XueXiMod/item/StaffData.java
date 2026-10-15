package com.bslx.XueXiMod.item;

public class StaffData {
    private final int slotCount;
    private final double magicRecoveryEfficiency;
    private final double cooldownReduction;
    private final int maxManaBonus;
    private final Double attributeDamageBonus;

    public StaffData(int slotCount, double magicRecoveryEfficiency, double cooldownReduction, int maxManaBonus, Double attributeDamageBonus) {
        if (slotCount < 0) {
            throw new IllegalArgumentException("slotCount must be >= 0");
        }
        this.slotCount = slotCount;
        this.magicRecoveryEfficiency = magicRecoveryEfficiency;
        this.cooldownReduction = cooldownReduction;
        this.maxManaBonus = maxManaBonus;
        this.attributeDamageBonus = attributeDamageBonus;
    }

    public int slotCount() { return slotCount; }
    public double magicRecoveryEfficiency() { return magicRecoveryEfficiency; }
    public double cooldownReduction() { return cooldownReduction; }
    public int maxManaBonus() { return maxManaBonus; }
    public Double attributeDamageBonus() { return attributeDamageBonus; }
}
