package com.bslx.XueXiMod.shu.technique.release.fire;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 只给 hurt 包一层进入/退出。原指令、原异常表和原返回值都保留。
 * 迎击帧插在元素令牌旁边：正常出口先按最终布尔结算，异常出口先解除预留，两条路之后仍执行原来的 exitHurt。
 */
public final class FireHurtBoundary {
    public static final String HURT_NAME = "hurt";
    public static final String HURT_DESCRIPTOR = "(Lnet/minecraft/world/damagesource/DamageSource;F)Z";
    static final String SCOPE = "com/bslx/XueXiMod/shu/technique/release/fire/ElementDamageScope";
    static final String CALL = "com/bslx/XueXiMod/shu/technique/release/fire/ElementDamageScope$HurtCall";
    static final String COUNTER_OWNER = "com/bslx/XueXiMod/combat/counter/CounterService";
    static final String COUNTER_FRAME = "com/bslx/XueXiMod/combat/counter/CounterHurtFrame";
    static final String RUNTIME_OWNER = "com/bslx/XueXiMod/combat/event/CounterRuntimeEvents";
    public static final String ENTER = "enterHurt";
    public static final String EXIT = "exitHurt";

    private FireHurtBoundary() {
    }

    public static boolean wrapMappedHurt(MethodNode method) {
        if (method == null || !HURT_NAME.equals(method.name) || !HURT_DESCRIPTOR.equals(method.desc)) {
            return false;
        }
        if ((method.access & Opcodes.ACC_STATIC) != 0 || alreadyWrapped(method)) {
            return false;
        }
        wrapBooleanInstance(method);
        return true;
    }

    public static void wrapBooleanInstance(MethodNode method) {
        if (method == null || alreadyWrapped(method)) {
            return;
        }
        int tokenLocal = nextLocal(method);
        int returnLocal = tokenLocal + 1;
        int thrownLocal = tokenLocal + 2;
        int counterLocal = thrownLocal + 1;
        method.maxLocals = counterLocal + 1;
        if (method.maxStack < 4) {
            method.maxStack = 4;
        }
        LabelNode tryStart = new LabelNode();
        LabelNode tryEnd = new LabelNode();
        LabelNode handler = new LabelNode();
        InsnList prefix = new InsnList();
        prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
        prefix.add(new VarInsnNode(Opcodes.ALOAD, 1));
        prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, SCOPE, ENTER,
                "(Ljava/lang/Object;Ljava/lang/Object;)L" + CALL + ";", false));
        prefix.add(new VarInsnNode(Opcodes.ASTORE, tokenLocal));
        prefix.add(counterEnter());
        prefix.add(new VarInsnNode(Opcodes.ASTORE, counterLocal));
        prefix.add(markHurtEntered());
        prefix.add(tryStart);
        method.instructions.insert(prefix);
        List<AbstractInsnNode> returns = new ArrayList<>();
        for (AbstractInsnNode cursor = method.instructions.getFirst(); cursor != null; cursor = cursor.getNext()) {
            if (cursor.getOpcode() == Opcodes.IRETURN) {
                returns.add(cursor);
            }
        }
        for (AbstractInsnNode returning : returns) {
            InsnList replacement = new InsnList();
            replacement.add(new VarInsnNode(Opcodes.ISTORE, returnLocal));
            replacement.add(new JumpInsnNode(Opcodes.GOTO, tryEnd));
            method.instructions.insert(returning, replacement);
            method.instructions.remove(returning);
        }
        InsnList suffix = new InsnList();
        suffix.add(tryEnd);
        suffix.add(sealDamage());
        suffix.add(new VarInsnNode(Opcodes.ALOAD, counterLocal));
        suffix.add(new VarInsnNode(Opcodes.ILOAD, returnLocal));
        suffix.add(counterFinish());
        suffix.add(new VarInsnNode(Opcodes.ALOAD, tokenLocal));
        suffix.add(exitInsn());
        suffix.add(new VarInsnNode(Opcodes.ILOAD, returnLocal));
        suffix.add(new InsnNode(Opcodes.IRETURN));
        suffix.add(handler);
        suffix.add(new VarInsnNode(Opcodes.ASTORE, thrownLocal));
        suffix.add(discardDamage());
        suffix.add(new VarInsnNode(Opcodes.ALOAD, counterLocal));
        suffix.add(counterAbort());
        suffix.add(new VarInsnNode(Opcodes.ALOAD, tokenLocal));
        suffix.add(exitInsn());
        suffix.add(new VarInsnNode(Opcodes.ALOAD, thrownLocal));
        suffix.add(new InsnNode(Opcodes.ATHROW));
        method.instructions.add(suffix);
        if (method.tryCatchBlocks == null) {
            method.tryCatchBlocks = new ArrayList<>();
        }
        method.tryCatchBlocks.add(new TryCatchBlockNode(tryStart, tryEnd, handler, null));
        for (AbstractInsnNode cursor = method.instructions.getFirst(); cursor != null; ) {
            AbstractInsnNode upcoming = cursor.getNext();
            if (cursor instanceof FrameNode) {
                method.instructions.remove(cursor);
            }
            cursor = upcoming;
        }
    }

    private static MethodInsnNode exitInsn() {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, SCOPE, EXIT, "(L" + CALL + ";)V", false);
    }

    private static MethodInsnNode counterEnter() {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, COUNTER_OWNER, ENTER,
                "()L" + COUNTER_FRAME + ";", false);
    }

    private static MethodInsnNode counterFinish() {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, COUNTER_OWNER, "finishHurt",
                "(L" + COUNTER_FRAME + ";Z)V", false);
    }

    private static MethodInsnNode counterAbort() {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, COUNTER_OWNER, "abortHurt",
                "(L" + COUNTER_FRAME + ";)V", false);
    }

    /** 伤害事件派发已经结束，再读一次最终取消标记。不依赖同优先级里没有更晚的监听器。 */
    private static MethodInsnNode sealDamage() {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, RUNTIME_OWNER, "sealDamage", "()V", false);
    }

    private static MethodInsnNode discardDamage() {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, RUNTIME_OWNER, "discardDamage", "()V", false);
    }

    private static MethodInsnNode markHurtEntered() {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, RUNTIME_OWNER, "markHurtEntered", "()V", false);
    }

    private static boolean alreadyWrapped(MethodNode method) {
        for (AbstractInsnNode cursor = method.instructions.getFirst(); cursor != null; cursor = cursor.getNext()) {
            if (cursor instanceof MethodInsnNode invoked && Opcodes.INVOKESTATIC == invoked.getOpcode()
                    && SCOPE.equals(invoked.owner) && ENTER.equals(invoked.name)) {
                return true;
            }
        }
        return false;
    }

    private static int nextLocal(MethodNode method) {
        int nextSlot = method.maxLocals;
        if (method.localVariables == null) {
            return nextSlot;
        }
        for (LocalVariableNode variable : method.localVariables) {
            int span = "J".equals(variable.desc) || "D".equals(variable.desc) ? 2 : 1;
            int occupied = variable.index + span;
            if (occupied > nextSlot) {
                nextSlot = occupied;
            }
        }
        return nextSlot;
    }
}
