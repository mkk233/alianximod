import com.bslx.XueXiMod.combat.event.DamageEventSlots;

/**
 * 直接驱动 combat/event/DamageEventSlots，不另写一套对齐规则。
 * 本次任务禁止运行。以后可用：
 * javac --release 17 -d tools/wood-base-checks/integration/out ^
 *   src/main/java/com/bslx/XueXiMod/combat/event/DamageEventSlots.java ^
 *   tools/wood-base-checks/integration/DamageEventSlotCheck.java
 * java -cp tools/wood-base-checks/integration/out DamageEventSlotCheck
 */
public final class DamageEventSlotCheck {
    private DamageEventSlotCheck() {
    }

    public static void main(String[] args) {
        int failures = 0;
        failures += nestedLateCancel();
        failures += exceptionKeepsOuter();
        failures += emptySlot();
        failures += outsideWriteDoesNotAttach();
        if (failures != 0) {
            throw new IllegalStateException("damage slot failures " + failures);
        }
    }

    /** 外层还没有伤害事件，内层先结束，并且结束前被改成取消。 */
    private static int nestedLateCancel() {
        DamageEventSlots<CancelMark> slots = new DamageEventSlots<>();
        slots.push();
        slots.push();
        CancelMark inner = new CancelMark();
        slots.writeCurrent(inner);
        inner.cancelled = true;
        CancelMark sealed = slots.popSeal();
        if (sealed != inner || !sealed.cancelled || slots.depth() != 1) {
            return 1;
        }
        if (slots.popSeal() != null || slots.depth() != 0) {
            return 1;
        }
        return 0;
    }

    /** 内层在自己的伤害事件之前异常退出，外层已经记下的事件还在。 */
    private static int exceptionKeepsOuter() {
        DamageEventSlots<CancelMark> slots = new DamageEventSlots<>();
        slots.push();
        CancelMark outer = new CancelMark();
        slots.writeCurrent(outer);
        slots.push();
        slots.popDiscard();
        if (slots.depth() != 1) {
            return 1;
        }
        CancelMark sealed = slots.popSeal();
        if (sealed != outer || sealed.cancelled || slots.depth() != 0) {
            return 1;
        }
        return 0;
    }

    private static int emptySlot() {
        DamageEventSlots<CancelMark> slots = new DamageEventSlots<>();
        slots.push();
        if (slots.popSeal() != null || slots.depth() != 0) {
            return 1;
        }
        slots.popDiscard();
        if (slots.depth() != 0 || slots.popSeal() != null) {
            return 1;
        }
        return 0;
    }

    private static int outsideWriteDoesNotAttach() {
        DamageEventSlots<CancelMark> slots = new DamageEventSlots<>();
        CancelMark stray = new CancelMark();
        slots.writeCurrent(stray);
        slots.push();
        if (slots.popSeal() != null || slots.depth() != 0) {
            return 1;
        }
        return 0;
    }

    private static final class CancelMark {
        private boolean cancelled;
    }
}
