package com.bslx.XueXiMod.shu.technique.release.fire;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** 核对映射 hurt 的字段写入，并执行变换后的夹具。不织入游戏，也不启动客户端。 */
public final class FireHurtBytecodeCheck {
    private static final String OWNER = "net/minecraft/world/entity/LivingEntity";
    private static final String HURT = "(Lnet/minecraft/world/damagesource/DamageSource;F)Z";
    private static final String SCOPE = "com/bslx/XueXiMod/shu/technique/release/fire/ElementDamageScope";
    private static final String CALL = "com/bslx/XueXiMod/shu/technique/release/fire/ElementDamageScope$HurtCall";
    private static final String COUNTER_OWNER = "com/bslx/XueXiMod/combat/counter/CounterService";
    private static final String COUNTER_FRAME = "com/bslx/XueXiMod/combat/counter/CounterHurtFrame";
    private static final String RUNTIME_OWNER = "com/bslx/XueXiMod/combat/event/CounterRuntimeEvents";
    private static final String ENTER = "enterHurt";
    private static final String EXIT = "exitHurt";
    private static final String RECOMP = "C:/Users/baishang/.gradle/caches/forge_gradle/minecraft_user_repo/"
            + "net/minecraftforge/forge/1.20.1-47.3.33_mapped_parchment_2023.06.26-1.20.1/"
            + "forge-1.20.1-47.3.33_mapped_parchment_2023.06.26-1.20.1-recomp.jar";
    private static final String FIXTURE = "com.bslx.XueXiMod.shu.technique.release.fire.HurtBoundaryFixture";
    private static int assertionCount;
    private static int failures;

    private FireHurtBytecodeCheck() {
    }

    public static void main(String[] arguments) throws Exception {
        section("映射字段", FireHurtBytecodeCheck::mappedFields);
        section("夹具边界", FireHurtBytecodeCheck::fixtureBoundary);
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        System.out.println("未执行：游戏内 Mixin 织入、真实伤害结算。没有启动游戏。");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void mappedFields() throws Exception {
        byte[] encoded = readMappedHurt();
        ClassNode owner = new ClassNode();
        new ClassReader(encoded).accept(owner, 0);
        MethodNode hurt = find(owner, "hurt", HURT);
        check(hurt != null, "recomp 里有 LivingEntity.hurt(DamageSource,float)");
        if (hurt == null) {
            return;
        }
        int hurtBefore = countInvoke(hurt, "actuallyHurt");
        int throwBefore = countOpcode(hurt, Opcodes.ATHROW);
        int returnBefore = countOpcode(hurt, Opcodes.IRETURN);
        check(countField(hurt, "lastHurt", "F") == 2 && ownedByLiving(hurt, "lastHurt", "F"),
                "lastHurt 两次 PUTFIELD，owner 都是 LivingEntity");
        check(countField(hurt, "invulnerableTime", "I") == 1 && ownedByLiving(hurt, "invulnerableTime", "I"),
                "invulnerableTime 只有一次 PUTFIELD，owner 是 LivingEntity");
        check(returnBefore > 1, "原方法有多处返回，含早退");
        boolean wrapped = FireHurtBoundary.wrapMappedHurt(hurt);
        check(wrapped, "边界变换套上了映射 hurt");
        check(countField(hurt, "lastHurt", "F") == 2 && countField(hurt, "invulnerableTime", "I") == 1,
                "变换后两处字段写入还在");
        check(countInvoke(hurt, "actuallyHurt") == hurtBefore, "没有删掉原结算调用");
        String elementEnter = "(Ljava/lang/Object;Ljava/lang/Object;)L" + CALL + ";";
        String elementExit = "(L" + CALL + ";)V";
        String counterEnter = "()L" + COUNTER_FRAME + ";";
        String counterFinish = "(L" + COUNTER_FRAME + ";Z)V";
        String counterAbort = "(L" + COUNTER_FRAME + ";)V";
        check(countCall(hurt, SCOPE, ENTER, elementEnter) == 1, "火元素 enterHurt 一次，描述符是令牌");
        check(countCall(hurt, SCOPE, EXIT, elementExit) == 2, "火元素 exitHurt 在正常出口和异常出口各一次");
        check(countInvoke(hurt, EXIT) == 2, "名为 exitHurt 的调用仍只有元素的两次");
        check(countCall(hurt, COUNTER_OWNER, ENTER, counterEnter) == 1, "迎击 enterHurt 一次，描述符是帧");
        check(countCall(hurt, COUNTER_OWNER, "finishHurt", counterFinish) == 1, "迎击正常出口 finishHurt 一次");
        check(countCall(hurt, COUNTER_OWNER, "abortHurt", counterAbort) == 1, "迎击异常出口 abortHurt 一次");
        check(countOpcode(hurt, Opcodes.IRETURN) == 1, "正常返回汇合成一次 ireturn");
        check(countOpcode(hurt, Opcodes.ATHROW) == throwBefore + 1, "原异常抛出都保留，并补上边界的再抛");
        TryCatchBlockNode boundary = catchAll(hurt);
        check(boundary != null, "有覆盖全方法的 finally 异常表");
        if (boundary == null) {
            return;
        }
        check(rangeContains(hurt, boundary.start, boundary.end, "lastHurt")
                        && rangeContains(hurt, boundary.start, boundary.end, "invulnerableTime"),
                "两处字段写入都在进入之后、正常收尾之前");
        String entryExpected = Opcodes.ALOAD + ":" + Opcodes.ALOAD + ":"
                + Opcodes.INVOKESTATIC + ":" + SCOPE + "." + ENTER + elementEnter + ":"
                + Opcodes.ASTORE + ":"
                + Opcodes.INVOKESTATIC + ":" + COUNTER_OWNER + "." + ENTER + counterEnter + ":"
                + Opcodes.ASTORE + ":"
                + Opcodes.INVOKESTATIC + ":" + RUNTIME_OWNER + ".markHurtEntered()V:";
        String normalExpected = Opcodes.INVOKESTATIC + ":" + RUNTIME_OWNER + ".sealDamage()V:"
                + Opcodes.ALOAD + ":"
                + Opcodes.ILOAD + ":"
                + Opcodes.INVOKESTATIC + ":" + COUNTER_OWNER + ".finishHurt" + counterFinish + ":"
                + Opcodes.ALOAD + ":"
                + Opcodes.INVOKESTATIC + ":" + SCOPE + "." + EXIT + elementExit + ":"
                + Opcodes.ILOAD + ":"
                + Opcodes.IRETURN + ":";
        String abortExpected = Opcodes.ASTORE + ":"
                + Opcodes.INVOKESTATIC + ":" + RUNTIME_OWNER + ".discardDamage()V:"
                + Opcodes.ALOAD + ":"
                + Opcodes.INVOKESTATIC + ":" + COUNTER_OWNER + ".abortHurt" + counterAbort + ":"
                + Opcodes.ALOAD + ":"
                + Opcodes.INVOKESTATIC + ":" + SCOPE + "." + EXIT + elementExit + ":"
                + Opcodes.ALOAD + ":"
                + Opcodes.ATHROW + ":";
        check(entryExpected.equals(callTrace(hurt.instructions.getFirst(), 7)),
                "进入顺序是元素令牌、迎击帧、压槽，然后才进 try");
        check(normalExpected.equals(callTrace(boundary.end, 8)),
                "正常出口先封事件再完成迎击，然后结束令牌并返回原布尔");
        check(abortExpected.equals(callTrace(boundary.handler, 8)),
                "异常出口先丢事件再中止迎击，然后结束令牌并原样抛出");
        boolean wrappedAgain = FireHurtBoundary.wrapMappedHurt(hurt);
        check(!wrappedAgain, "已经包过的映射 hurt 不会再包一次");
        check(countCall(hurt, SCOPE, ENTER, elementEnter) == 1
                        && countCall(hurt, COUNTER_OWNER, ENTER, counterEnter) == 1
                        && countCall(hurt, SCOPE, EXIT, elementExit) == 2,
                "重复包装没有插入第二套进入或退出");
    }

    private static void fixtureBoundary() throws Exception {
        Path location = Path.of(FireHurtBytecodeCheck.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        byte[] encoded = Files.readAllBytes(location.resolve(FIXTURE.replace('.', '/') + ".class"));
        ClassNode owner = new ClassNode();
        new ClassReader(encoded).accept(owner, 0);
        MethodNode hurt = find(owner, "hurt", "(Ljava/lang/Object;F)Z");
        check(hurt != null && countFieldOwnerWrites(hurt) == 0, "夹具原方法没有伤害字段写入");
        int businessBefore = countInvoke(hurt, "accept") + countStatic(hurt, "business");
        FireHurtBoundary.wrapBooleanInstance(hurt);
        check(countStatic(hurt, "business") > 0 && countInvoke(hurt, "accept") > 0, "原业务和回调都还在");
        check(businessBefore == countInvoke(hurt, "accept") + countStatic(hurt, "business"), "变换没有复制业务指令");
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        owner.accept(writer);
        Isolated loader = new Isolated(writer.toByteArray());
        Class<?> fixture = loader.loadClass(FIXTURE);
        Object instance = fixture.getDeclaredConstructor().newInstance();
        Method hurtMethod = fixture.getMethod("hurt", Object.class, float.class);
        Field hookField = fixture.getField("hook");
        Field businessField = fixture.getField("business");
        Object source = new Object();
        businessField.setInt(null, 0);
        hookField.set(null, (HurtBoundaryHook) (self, seen, amount) -> {
            ElementDamageScope.HurtCall call = ElementDamageScope.currentHurt();
            check(call != null && call.target() == self && call.source() == seen, "夹具正常路径持有本次令牌");
        });
        Object returned = hurtMethod.invoke(instance, source, 1.0f);
        check(Boolean.TRUE.equals(returned) && businessField.getInt(null) == 1
                        && ElementDamageScope.currentHurt() == null,
                "夹具正常返回执行了业务且没有残留");
        businessField.setInt(null, 0);
        Object early = hurtMethod.invoke(instance, source, -1.0f);
        check(Boolean.FALSE.equals(early) && businessField.getInt(null) == 0
                        && ElementDamageScope.currentHurt() == null,
                "夹具早退不执行后续业务且没有残留");
        businessField.setInt(null, 0);
        try {
            hurtMethod.invoke(instance, source, 101.0f);
            check(false, "夹具里的直接异常应传播");
        } catch (InvocationTargetException failure) {
            check(failure.getCause() instanceof IllegalStateException && businessField.getInt(null) == 1
                            && ElementDamageScope.currentHurt() == null,
                    "直接异常在原业务之后传播，令牌已结束");
        }
        businessField.setInt(null, 0);
        hookField.set(null, (HurtBoundaryHook) (self, seen, amount) -> {
            throw new CheckedSignal();
        });
        try {
            hurtMethod.invoke(instance, source, 1.0f);
            check(false, "夹具中的 Throwable 应传播");
        } catch (InvocationTargetException failure) {
            check(failure.getCause() instanceof CheckedSignal && ElementDamageScope.currentHurt() == null,
                    "非 RuntimeException 传播且没有残留");
        }
        hookField.set(null, (HurtBoundaryHook) (self, seen, amount) -> {
            if (amount > 10.0f) {
                throw new IllegalStateException("inner");
            }
            ElementDamageScope.HurtCall outer = ElementDamageScope.currentHurt();
            long identity = outer.identity();
            try {
                hurtMethod.invoke(self, new Object(), 11.0f);
                check(false, "内层夹具调用应抛出");
            } catch (InvocationTargetException failure) {
                ElementDamageScope.HurtCall still = ElementDamageScope.currentHurt();
                check(failure.getCause() instanceof IllegalStateException && still == outer
                                && still.identity() == identity,
                        "内层异常被捕获后外层令牌和身份保持");
            }
        });
        hurtMethod.invoke(instance, source, 1.0f);
        check(ElementDamageScope.currentHurt() == null, "外层夹具返回后没有残留");
    }

    private static byte[] readMappedHurt() throws Exception {
        try (JarFile archive = new JarFile(RECOMP)) {
            JarEntry entry = archive.getJarEntry("net/minecraft/world/entity/LivingEntity.class");
            check(entry != null, "recomp.jar 含有 LivingEntity.class");
            if (entry == null) {
                return new byte[0];
            }
            try (java.io.InputStream input = archive.getInputStream(entry)) {
                return input.readAllBytes();
            }
        }
    }

    private static MethodNode find(ClassNode owner, String name, String descriptor) {
        for (MethodNode method : owner.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) {
                return method;
            }
        }
        return null;
    }

    private static int countField(MethodNode method, String name, String descriptor) {
        int found = 0;
        for (AbstractInsnNode cursor = method.instructions.getFirst(); cursor != null; cursor = cursor.getNext()) {
            if (cursor.getOpcode() == Opcodes.PUTFIELD && cursor instanceof FieldInsnNode field
                    && name.equals(field.name) && descriptor.equals(field.desc)
                    && OWNER.equals(field.owner)) {
                found += 1;
            }
        }
        return found;
    }

    private static boolean ownedByLiving(MethodNode method, String name, String descriptor) {
        boolean saw = false;
        for (AbstractInsnNode cursor = method.instructions.getFirst(); cursor != null; cursor = cursor.getNext()) {
            if (cursor.getOpcode() != Opcodes.PUTFIELD || !(cursor instanceof FieldInsnNode)) {
                continue;
            }
            FieldInsnNode field = (FieldInsnNode) cursor;
            if (!name.equals(field.name) || !descriptor.equals(field.desc)) {
                continue;
            }
            saw = true;
            if (!OWNER.equals(field.owner)) {
                return false;
            }
        }
        return saw;
    }

    private static int countFieldOwnerWrites(MethodNode method) {
        return countField(method, "lastHurt", "F") + countField(method, "invulnerableTime", "I");
    }

    private static int countCall(MethodNode method, String owner, String name, String descriptor) {
        int found = 0;
        for (AbstractInsnNode cursor = method.instructions.getFirst(); cursor != null; cursor = cursor.getNext()) {
            if (cursor instanceof MethodInsnNode invoked
                    && owner.equals(invoked.owner)
                    && name.equals(invoked.name)
                    && descriptor.equals(invoked.desc)) {
                found += 1;
            }
        }
        return found;
    }

    private static String callTrace(AbstractInsnNode start, int limit) {
        StringBuilder rendered = new StringBuilder();
        int kept = 0;
        for (AbstractInsnNode cursor = start; cursor != null && kept < limit; cursor = cursor.getNext()) {
            if (cursor.getOpcode() < 0) {
                continue;
            }
            rendered.append(cursor.getOpcode()).append(':');
            if (cursor instanceof MethodInsnNode invoked) {
                rendered.append(invoked.owner).append('.').append(invoked.name).append(invoked.desc).append(':');
            }
            kept += 1;
        }
        return rendered.toString();
    }

    private static int countInvoke(MethodNode method, String name) {
        int found = 0;
        for (AbstractInsnNode cursor = method.instructions.getFirst(); cursor != null; cursor = cursor.getNext()) {
            if (cursor instanceof MethodInsnNode invoked && name.equals(invoked.name)) {
                found += 1;
            }
        }
        return found;
    }

    private static int countStatic(MethodNode method, String name) {
        int found = 0;
        for (AbstractInsnNode cursor = method.instructions.getFirst(); cursor != null; cursor = cursor.getNext()) {
            if (cursor.getOpcode() == Opcodes.PUTSTATIC && cursor instanceof FieldInsnNode field
                    && name.equals(field.name)) {
                found += 1;
            }
        }
        return found;
    }

    private static int countOpcode(MethodNode method, int opcode) {
        int found = 0;
        for (AbstractInsnNode cursor = method.instructions.getFirst(); cursor != null; cursor = cursor.getNext()) {
            if (cursor.getOpcode() == opcode) {
                found += 1;
            }
        }
        return found;
    }

    private static TryCatchBlockNode catchAll(MethodNode method) {
        TryCatchBlockNode found = null;
        for (TryCatchBlockNode block : method.tryCatchBlocks) {
            if (block.type == null) {
                found = block;
            }
        }
        return found;
    }

    private static boolean rangeContains(MethodNode method, LabelNode start, LabelNode end, String name) {
        boolean inside = false;
        for (AbstractInsnNode cursor = method.instructions.getFirst(); cursor != null; cursor = cursor.getNext()) {
            if (cursor == end) {
                return false;
            }
            if (cursor == start) {
                inside = true;
            }
            if (inside && cursor.getOpcode() == Opcodes.PUTFIELD && cursor instanceof FieldInsnNode field
                    && name.equals(field.name)) {
                return true;
            }
        }
        return false;
    }

    private static String following(AbstractInsnNode start, int limit) {
        StringBuilder rendered = new StringBuilder();
        int kept = 0;
        for (AbstractInsnNode cursor = start; cursor != null && kept < limit; cursor = cursor.getNext()) {
            if (cursor.getOpcode() < 0) {
                continue;
            }
            rendered.append(cursor.getOpcode()).append(':');
            if (cursor instanceof MethodInsnNode invoked) {
                rendered.append(invoked.name).append(':');
            }
            kept += 1;
        }
        return rendered.toString();
    }

    private static final class Isolated extends ClassLoader {
        private final byte[] transformed;

        private Isolated(byte[] transformed) {
            super(FireHurtBytecodeCheck.class.getClassLoader());
            this.transformed = transformed;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!FIXTURE.equals(name)) {
                return super.loadClass(name, resolve);
            }
            Class<?> existing = findLoadedClass(name);
            if (existing == null) {
                existing = defineClass(name, this.transformed, 0, this.transformed.length);
            }
            if (resolve) {
                resolveClass(existing);
            }
            return existing;
        }
    }

    private static final class CheckedSignal extends Throwable {
        private CheckedSignal() {
            super("checked");
        }
    }

    private static void section(String title, Section body) {
        int before = failures;
        try {
            body.run();
        } catch (Throwable failure) {
            failures += 1;
            assertionCount += 1;
            System.out.println("失败 " + title + " 抛出 " + failure);
        }
        System.out.println(title + (failures == before ? " 通过" : " 失败"));
    }

    @FunctionalInterface
    private interface Section {
        void run() throws Exception;
    }

    private static void check(boolean condition, String label) {
        assertionCount += 1;
        if (!condition) {
            failures += 1;
            System.out.println("失败 " + label);
        }
    }
}
