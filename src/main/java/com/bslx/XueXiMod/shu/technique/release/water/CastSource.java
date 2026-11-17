package com.bslx.XueXiMod.shu.technique.release.water;

/**
 * 一次已通过前置资格的释放来源。创建之后不再改写。
 * 技能槽、主手栈引用、当时选中的背包槽、载体代数各自记下。
 */
public final class CastSource {
    private final int skillSlot;
    private final Object stackRef;
    private final int selectedSlot;
    private final int generation;

    public CastSource(int skillSlot, Object stackRef, int selectedSlot, int generation) {
        this.skillSlot = skillSlot;
        this.stackRef = stackRef;
        this.selectedSlot = selectedSlot;
        this.generation = generation;
    }

    public int skillSlot() {
        return skillSlot;
    }

    public Object stackRef() {
        return stackRef;
    }

    public int selectedSlot() {
        return selectedSlot;
    }

    public int generation() {
        return generation;
    }

    public boolean sameCarrier(Object stackRef, int selectedSlot, int generation) {
        return this.stackRef == stackRef && this.selectedSlot == selectedSlot && this.generation == generation;
    }
}
