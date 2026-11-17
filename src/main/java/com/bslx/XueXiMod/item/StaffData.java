package com.bslx.XueXiMod.item;

public class StaffData {
    /** 法杖链的合法上限。基础法杖仍是 6，测试法杖用满 32。 */
    public static final int SLOT_CAP = 32;

    private final int slotCount;
    private final double magicRecoveryEfficiency;
    private final double cooldownReduction;
    private final int upperManaBonus;
    private final Double attributeDamageBonus;

    public StaffData(int slotCount, double magicRecoveryEfficiency, double cooldownReduction, int upperManaBonus, Double attributeDamageBonus) {
        if (slotCount < 0 || slotCount > SLOT_CAP) {
            throw new IllegalArgumentException("slotCount must be between 0 and 32");
        }
        this.slotCount = slotCount;
        this.magicRecoveryEfficiency = magicRecoveryEfficiency;
        this.cooldownReduction = cooldownReduction;
        this.upperManaBonus = upperManaBonus;
        this.attributeDamageBonus = attributeDamageBonus;
    }

    public int slotCount() { return slotCount; }
    public double magicRecoveryEfficiency() { return magicRecoveryEfficiency; }
    public double cooldownReduction() { return cooldownReduction; }
    public int maxManaBonus() { return upperManaBonus; }
    public Double attributeDamageBonus() { return attributeDamageBonus; }
}
