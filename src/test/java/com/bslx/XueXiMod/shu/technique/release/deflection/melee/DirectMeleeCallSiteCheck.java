package com.bslx.XueXiMod.shu.technique.release.deflection.melee;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 读安装包和编译产物。不启动游戏，也不运行 Mixin 转换。
 */
public final class DirectMeleeCallSiteCheck {
    private static final String HURT = "hurt";
    private static final String HURT_DESC = "(Lnet/minecraft/world/damagesource/DamageSource;F)Z";
    private static final Path RECOMP = Path.of("C:/Users/baishang/.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.20.1-47.3.33_mapped_parchment_2023.06.26-1.20.1/forge-1.20.1-47.3.33_mapped_parchment_2023.06.26-1.20.1-recomp.jar");
    private static final Path CLASSES = Path.of("D:/alianximod/build/classes/java/main");
    private static final Path MIXIN_SOURCE = Path.of("D:/alianximod/src/main/java/com/bslx/XueXiMod/mixin/GuardianBeamMeleeScopeMixin.java");
    private static int assertionCount;

    private DirectMeleeCallSiteCheck() {
    }

    public static void main(String[] arguments) throws Exception {
        try (JarFile jarFile = new JarFile(RECOMP.toFile())) {
            specialAttackers(jarFile);
            attackEventOrder(jarFile);
            guardianBeam(jarFile);
        }
        compiledFallback();
        compiledMixin();
        System.out.println("调用点是静态字节码，不是 Mixin 运行时");
        System.out.println("断言 " + assertionCount);
    }

    private static void specialAttackers(JarFile jarFile) throws Exception {
        ClassNode ironGolem = readJar(jarFile, "net/minecraft/world/entity/animal/IronGolem.class");
        MethodNode ironAttack = method(ironGolem, "doHurtTarget", "(Lnet/minecraft/world/entity/Entity;)Z");
        check(ironAttack != null, "铁傀儡声明 doHurtTarget");
        check(invokes(ironAttack, "net/minecraft/world/entity/Mob", "doHurtTarget", null).isEmpty(),
                "铁傀儡不调用 Mob.doHurtTarget");
        check(previousOwnerName(ironAttack, firstHurt(ironAttack)).contains("mobAttack"), "铁傀儡来源是 mobAttack");
        check(jumpsBefore(ironAttack, firstHurt(ironAttack), "setDeltaMovement"), "铁傀儡返回假后走不到击退");

        ClassNode hoglinBase = readJar(jarFile, "net/minecraft/world/entity/monster/hoglin/HoglinBase.class");
        MethodNode hoglinThrow = method(hoglinBase, "hurtAndThrowTarget", null);
        check(hoglinThrow != null, "HoglinBase 有 hurtAndThrowTarget");
        check(previousOwnerName(hoglinThrow, firstHurt(hoglinThrow)).contains("mobAttack"), "疣猪兽来源是 mobAttack");
        check(jumpsBefore(hoglinThrow, firstHurt(hoglinThrow), "throwTarget"), "返回假后走不到 throwTarget");

        ClassNode slime = readJar(jarFile, "net/minecraft/world/entity/monster/Slime.class");
        check(method(slime, "doHurtTarget", null) == null, "史莱姆不声明 doHurtTarget");
        MethodNode dealDamage = method(slime, "dealDamage", "(Lnet/minecraft/world/entity/LivingEntity;)V");
        check(dealDamage != null && hurtInvokes(dealDamage).size() == 1, "每次 dealDamage 只有一次 hurt");
        check(previousOwnerName(dealDamage, firstHurt(dealDamage)).contains("mobAttack"), "史莱姆来源是 mobAttack");
        check(jumpsBefore(dealDamage, firstHurt(dealDamage), "doEnchantDamageEffects"), "返回假后走不到附魔效果");
        check(namedInvokes(slime, "dealDamage").size() >= 2, "史莱姆有多次独立的 dealDamage 调用");

        ClassNode bee = readJar(jarFile, "net/minecraft/world/entity/animal/Bee.class");
        MethodNode beeAttack = method(bee, "doHurtTarget", "(Lnet/minecraft/world/entity/Entity;)Z");
        check(beeAttack != null, "蜜蜂声明自己的 doHurtTarget");
        check(invokes(beeAttack, "net/minecraft/world/entity/Mob", "doHurtTarget", null).isEmpty(),
                "蜜蜂不调用 Mob.doHurtTarget");
        check(previousOwnerName(beeAttack, firstHurt(beeAttack)).contains("sting"), "蜜蜂来源是 sting");
        check(jumpsBefore(beeAttack, firstHurt(beeAttack), "addEffect"), "返回假后走不到中毒");

        ClassNode enderMan = readJar(jarFile, "net/minecraft/world/entity/monster/EnderMan.class");
        check(method(enderMan, "doHurtTarget", null) == null, "末影人不声明 doHurtTarget");
        check(enderMan.superName != null && enderMan.superName.contains("Monster"), "末影人仍继承怪物近战");
        check(jarFile.getJarEntry("net/minecraft/world/entity/monster/ElderGuardian$GuardianAttackGoal.class") == null,
                "远古守卫者没有另一份射线目标类");
    }

    private static void attackEventOrder(JarFile jarFile) throws Exception {
        ClassNode player = readJar(jarFile, "net/minecraft/world/entity/player/Player.class");
        MethodNode playerHurt = method(player, HURT, HURT_DESC);
        List<MethodInsnNode> playerAttack = invokes(playerHurt, "net/minecraftforge/common/ForgeHooks", "onPlayerAttack", null);
        List<MethodInsnNode> playerLiving = invokes(playerHurt, "net/minecraftforge/common/ForgeHooks", "onLivingAttack", null);
        check(playerAttack.size() == 1 && playerLiving.isEmpty(), "玩家 hurt 只发 onPlayerAttack");
        check(realIndex(playerHurt, playerAttack.get(0)) < 8, "onPlayerAttack 在玩家 hurt 开头");
        check(hasEarlyReturn(playerHurt, playerAttack.get(0)), "玩家事件取消后提前返回");

        ClassNode living = readJar(jarFile, "net/minecraft/world/entity/LivingEntity.class");
        MethodNode livingHurt = method(living, HURT, HURT_DESC);
        List<MethodInsnNode> livingAttack = invokes(livingHurt, "net/minecraftforge/common/ForgeHooks", "onLivingAttack", null);
        check(livingAttack.size() == 1, "非玩家 hurt 发 onLivingAttack");
        check(invokes(livingHurt, "net/minecraftforge/common/ForgeHooks", "onPlayerAttack", null).isEmpty(),
                "LivingEntity.hurt 不重复发玩家事件");
        int attackAt = instructionIndex(livingHurt, livingAttack.get(0));
        int frameAt = fieldIndex(livingHurt, "invulnerableTime");
        check(attackAt >= 0 && frameAt > attackAt, "无敌帧读取晚于 LivingAttackEvent");
    }

    private static void guardianBeam(JarFile jarFile) throws Exception {
        ClassNode goal = readJar(jarFile, "net/minecraft/world/entity/monster/Guardian$GuardianAttackGoal.class");
        MethodNode tick = method(goal, "tick", "()V");
        List<MethodInsnNode> livingHurts = invokes(tick, "net/minecraft/world/entity/LivingEntity", HURT, HURT_DESC);
        check(livingHurts.size() == 2, "射线 tick 恰好两次 LivingEntity.hurt");
        check(previousOwnerName(tick, livingHurts.get(0)).contains("indirectMagic"), "序号 0 是 indirectMagic");
        check(previousOwnerName(tick, livingHurts.get(1)).contains("mobAttack"), "序号 1 是 mobAttack");
    }

    private static void compiledFallback() throws Exception {
        ClassNode listener = readClass(CLASSES.resolve(
                "com/bslx/XueXiMod/shu/technique/release/deflection/melee/DirectMeleeFallback.class"));
        AnnotationNode subscriber = findAnnotation(classAnnotations(listener), "Lnet/minecraftforge/fml/common/Mod$EventBusSubscriber;");
        check(subscriber != null, "补充入口注册在 Forge 事件总线");
        check("xueximod".equals(annotationValue(subscriber, "modid")), "订阅模组 id 是 xueximod");
        MethodNode handler = method(listener, "onDirectMobMelee", null);
        AnnotationNode subscribe = findAnnotation(methodAnnotations(handler), "Lnet/minecraftforge/eventbus/api/SubscribeEvent;");
        check(subscribe != null, "监听 LivingAttackEvent");
        Object priority = annotationValue(subscribe, "priority");
        check(priority instanceof String[] enumValue && enumValue.length == 2 && "LOWEST".equals(enumValue[1]),
                "优先级是 LOWEST");
        Object receiveCanceled = annotationValue(subscribe, "receiveCanceled");
        check(!(receiveCanceled instanceof Boolean canceled && canceled), "不接收已取消事件");
        List<MethodInsnNode> claimCalls = invokes(handler, "com/bslx/XueXiMod/shu/technique/release/deflection/melee/DirectMeleeFallback", "claim", null);
        List<MethodInsnNode> settleCalls = invokes(handler, "com/bslx/XueXiMod/shu/technique/release/deflection/melee/MeleeDeflection", "settleOwnedDirect", null);
        List<MethodInsnNode> cancelCalls = invokes(handler, "net/minecraftforge/event/entity/living/LivingAttackEvent", "setCanceled", null);
        check(claimCalls.size() == 1 && settleCalls.size() == 1 && cancelCalls.size() == 1, "监听器先分类，再结算，再取消");
        int claimAt = instructionIndex(handler, claimCalls.get(0));
        int settleAt = instructionIndex(handler, settleCalls.get(0));
        int cancelAt = instructionIndex(handler, cancelCalls.get(0));
        check(claimAt < settleAt && settleAt < cancelAt, "取消发生在本次结算返回之后");
        check(jumpsBefore(handler, settleCalls.get(0), "setCanceled"), "结算失败不会取消事件");

        ClassNode admission = readClass(CLASSES.resolve(
                "com/bslx/XueXiMod/shu/technique/release/deflection/melee/DirectMeleeAdmission.class"));
        MethodNode capture = method(admission, "capture", null);
        String captureText = methodText(capture);
        check(captureText.contains("getDirectEntity") && captureText.contains("getEntity"), "分类比较直接实体和来源实体");
        check(captureText.contains("MOB_ATTACK") && captureText.contains("STING"), "只点名 mob_attack 和 sting");
        check(captureText.contains("IS_PROJECTILE") && captureText.contains("isInvulnerableTo"), "排除弹射并看无敌");
        check(captureText.contains("excludesDirectMelee"), "分类会读射线排除范围");

        ClassNode melee = readClass(CLASSES.resolve(
                "com/bslx/XueXiMod/shu/technique/release/deflection/melee/MeleeDeflection.class"));
        MethodNode arm = method(melee, "arm", null);
        check(invokes(arm, "com/bslx/XueXiMod/shu/technique/release/deflection/melee/MeleeDeflection", "pushAttempt", null).size() == 1,
                "旧 arm 会先压尝试帧");
        check(invokes(arm, "com/bslx/XueXiMod/shu/technique/release/deflection/melee/MeleeDeflection", "finishToken", null).isEmpty(),
                "旧 arm 不自己关闭帧");
        MethodNode attach = method(melee, "attachBooking", null);
        check(invokes(attach, "com/bslx/XueXiMod/shu/technique/release/deflection/melee/MeleeDeflection", "finish", null).isEmpty()
                        && invokes(attach, "com/bslx/XueXiMod/shu/technique/release/deflection/melee/MeleeDeflection", "finishToken", null).isEmpty()
                        && invokes(attach, "com/bslx/XueXiMod/shu/technique/release/deflection/melee/MeleeDeflection", "removeFrame", null).isEmpty(),
                "几何或预约失败不会在报价里拆掉尝试帧");
        check(!methodText(method(melee, "attemptLive", null)).contains("booking"), "跳过补充时不看账是否买到");
        MethodNode finish = method(melee, "finish", null);
        check(returnBefore(finish, "retireAttack"), "空账先出栈，不调用 retireAttack");
        MethodNode owned = method(melee, "ownAttempt", null);
        check(owned.tryCatchBlocks != null && !owned.tryCatchBlocks.isEmpty(), "补充入口有 finally");
        int finishCalls = invokes(owned,
                "com/bslx/XueXiMod/shu/technique/release/deflection/melee/MeleeDeflection", "finishToken", null).size();
        check(finishCalls >= 1 && finishCalls <= 2, "finally 关闭这次句柄");
    }

    private static void compiledMixin() throws Exception {
        String mixinSource = Files.readString(MIXIN_SOURCE, StandardCharsets.UTF_8);
        check(mixinSource.contains("ordinal = 1"), "源码指定第二次 hurt");
        check(!mixinSource.contains("Args"), "没有 Args 注入");
        check(!mixinSource.contains("distanceTo"), "射线排除不看距离");
        ClassNode mixin = readClass(CLASSES.resolve("com/bslx/XueXiMod/mixin/GuardianBeamMeleeScopeMixin.class"));
        AnnotationNode mixinMark = findAnnotation(classAnnotations(mixin), "Lorg/spongepowered/asm/mixin/Mixin;");
        Object targets = annotationValue(mixinMark, "targets");
        check(targets instanceof List<?> targetList && targetList.contains(
                "net.minecraft.world.entity.monster.Guardian$GuardianAttackGoal"), "Mixin 目标是射线内部类");
        MethodNode redirect = method(mixin, "xuexiGuardianBeamMobAttack", null);
        check((redirect.access & Opcodes.ACC_PUBLIC) == 0 || (redirect.access & Opcodes.ACC_STATIC) == 0,
                "重定向方法不是 public static");
        check((redirect.access & Opcodes.ACC_STATIC) == 0, "重定向方法不是静态辅助");
        for (MethodNode methodNode : mixin.methods) {
            if ("<init>".equals(methodNode.name) || "<clinit>".equals(methodNode.name)) {
                continue;
            }
            boolean publicStatic = (methodNode.access & Opcodes.ACC_PUBLIC) != 0
                    && (methodNode.access & Opcodes.ACC_STATIC) != 0;
            check(!publicStatic, "Mixin 没有 public static 辅助方法");
            for (AnnotationNode annotation : methodAnnotations(methodNode)) {
                check(annotation.desc == null || !annotation.desc.contains("Args"), "没有 Args 注解");
            }
        }
        AnnotationNode redirectMark = findAnnotation(methodAnnotations(redirect), "Lorg/spongepowered/asm/mixin/injection/Redirect;");
        check(annotationContains(annotationValue(redirectMark, "method"), "tick"), "织在 tick");
        Object atValue = annotationValue(redirectMark, "at");
        check(atValue instanceof AnnotationNode atNode, "Redirect 带 At");
        AnnotationNode atNode = (AnnotationNode) atValue;
        check("INVOKE".equals(annotationValue(atNode, "value")), "At 是调用点");
        Object hurtTarget = annotationValue(atNode, "target");
        check(hurtTarget instanceof String targetText && targetText.contains("LivingEntity;hurt")
                && targetText.contains(HURT_DESC), "描述符是 LivingEntity.hurt");
        check(Integer.valueOf(1).equals(annotationValue(atNode, "ordinal")), "ordinal 是 1");
        List<MethodInsnNode> hurtCalls = invokes(redirect, "net/minecraft/world/entity/LivingEntity", HURT, HURT_DESC);
        check(hurtCalls.size() == 1, "包装里原 hurt 只调用一次");
        check(invokes(redirect, "com/bslx/XueXiMod/shu/technique/release/deflection/melee/NonPhysicalMeleeScope", "open",
                "(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)Lcom/bslx/XueXiMod/shu/technique/release/deflection/melee/NonPhysicalMeleeScope$Token;").size() == 1,
                "用原来的受害者和来源打开排除范围");
        int closeCalls = invokes(redirect,
                "com/bslx/XueXiMod/shu/technique/release/deflection/melee/NonPhysicalMeleeScope$Token", "close", "()V").size();
        check(closeCalls >= 1 && closeCalls <= 2, "finally 只关闭这次标记");
        check(redirect.tryCatchBlocks != null && !redirect.tryCatchBlocks.isEmpty(), "范围用 try/finally 包住");

        String mixinJson = Files.readString(Path.of("D:/alianximod/src/main/resources/xueximod.mixins.json"), StandardCharsets.UTF_8);
        check(mixinJson.contains("GuardianBeamMeleeScopeMixin"), "mixins.json 登记了射线 Mixin");
        Path refmap = firstExisting(
                Path.of("D:/alianximod/build/tmp/compileJava/compileJava-refmap.json"),
                Path.of("D:/alianximod/build/generated/mixin-refmap/xueximod.refmap.json"),
                Path.of("D:/alianximod/build/resources/main/xueximod.refmap.json"));
        check(refmap != null, "编译生成了 refmap");
        String refmapText = Files.readString(refmap, StandardCharsets.UTF_8);
        check(refmapText.contains("GuardianBeamMeleeScopeMixin"), "refmap 含这个 Mixin");
        check(refmapText.contains("Guardian$GuardianAttackGoal") || refmapText.contains("m_8037_"),
                "refmap 映射了射线 tick");
        check(refmapText.contains("LivingEntity;hurt") || refmapText.contains("m_6469_"),
                "refmap 映射了 hurt 描述符");
        String ordinary = Files.readString(Path.of(
                "D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/deflection/melee/DirectMeleeFallback.java"),
                StandardCharsets.UTF_8);
        check(!ordinary.contains("GuardianBeamMeleeScopeMixin"), "普通代码不引用 Mixin 类");
    }

    private static boolean returnBefore(MethodNode methodNode, String laterName) {
        int laterAt = -1;
        int cursor = 0;
        for (AbstractInsnNode instruction : methodNode.instructions) {
            if (instruction instanceof MethodInsnNode invoke && laterName.equals(invoke.name)) {
                laterAt = cursor;
                break;
            }
            cursor++;
        }
        if (laterAt < 0) {
            return false;
        }
        cursor = 0;
        for (AbstractInsnNode instruction : methodNode.instructions) {
            if (cursor >= laterAt) {
                return false;
            }
            if (instruction.getOpcode() == Opcodes.RETURN || instruction.getOpcode() == Opcodes.IRETURN) {
                return true;
            }
            cursor++;
        }
        return false;
    }

    private static boolean jumpsBefore(MethodNode methodNode, AbstractInsnNode start, String laterName) {
        boolean passed = false;
        for (AbstractInsnNode instruction : methodNode.instructions) {
            if (!passed) {
                if (instruction == start) {
                    passed = true;
                }
                continue;
            }
            if (instruction instanceof JumpInsnNode) {
                return true;
            }
            if (instruction instanceof MethodInsnNode invoke && laterName.equals(invoke.name)) {
                return false;
            }
        }
        return false;
    }

    private static boolean hasEarlyReturn(MethodNode methodNode, AbstractInsnNode start) {
        boolean passed = false;
        int seen = 0;
        for (AbstractInsnNode instruction : methodNode.instructions) {
            if (!passed) {
                if (instruction == start) {
                    passed = true;
                }
                continue;
            }
            seen++;
            if (instruction.getOpcode() == Opcodes.IRETURN || instruction instanceof JumpInsnNode) {
                return seen < 8;
            }
        }
        return false;
    }

    private static int realIndex(MethodNode methodNode, AbstractInsnNode target) {
        int cursor = 0;
        for (AbstractInsnNode instruction : methodNode.instructions) {
            if (instruction == target) {
                return cursor;
            }
            if (instruction.getOpcode() >= 0) {
                cursor++;
            }
        }
        return -1;
    }

    private static int instructionIndex(MethodNode methodNode, AbstractInsnNode target) {
        int cursor = 0;
        for (AbstractInsnNode instruction : methodNode.instructions) {
            if (instruction == target) {
                return cursor;
            }
            cursor++;
        }
        return -1;
    }

    private static int fieldIndex(MethodNode methodNode, String fieldName) {
        int cursor = 0;
        for (AbstractInsnNode instruction : methodNode.instructions) {
            if (instruction instanceof org.objectweb.asm.tree.FieldInsnNode field
                    && fieldName.equals(field.name)) {
                return cursor;
            }
            cursor++;
        }
        return -1;
    }

    private static String previousOwnerName(MethodNode methodNode, MethodInsnNode start) {
        AbstractInsnNode cursor = start.getPrevious();
        while (cursor != null) {
            if (cursor instanceof MethodInsnNode invoke
                    && invoke.owner != null
                    && invoke.owner.contains("DamageSources")) {
                return invoke.owner + "." + invoke.name;
            }
            cursor = cursor.getPrevious();
        }
        return "";
    }

    private static MethodInsnNode firstHurt(MethodNode methodNode) {
        List<MethodInsnNode> found = hurtInvokes(methodNode);
        return found.isEmpty() ? null : found.get(0);
    }

    private static List<MethodInsnNode> hurtInvokes(MethodNode methodNode) {
        List<MethodInsnNode> found = new ArrayList<>();
        for (AbstractInsnNode instruction : methodNode.instructions) {
            if (instruction instanceof MethodInsnNode invoke
                    && HURT.equals(invoke.name)
                    && HURT_DESC.equals(invoke.desc)) {
                found.add(invoke);
            }
        }
        return found;
    }

    private static List<MethodInsnNode> namedInvokes(ClassNode typeNode, String methodName) {
        List<MethodInsnNode> found = new ArrayList<>();
        for (MethodNode methodNode : typeNode.methods) {
            for (AbstractInsnNode instruction : methodNode.instructions) {
                if (instruction instanceof MethodInsnNode invoke && methodName.equals(invoke.name)) {
                    found.add(invoke);
                }
            }
        }
        return found;
    }

    private static List<MethodInsnNode> invokes(MethodNode methodNode, String owner, String methodName, String descriptor) {
        List<MethodInsnNode> found = new ArrayList<>();
        if (methodNode == null) {
            return found;
        }
        for (AbstractInsnNode instruction : methodNode.instructions) {
            if (!(instruction instanceof MethodInsnNode invoke)) {
                continue;
            }
            if (!methodName.equals(invoke.name)) {
                continue;
            }
            if (owner != null && !owner.equals(invoke.owner)) {
                continue;
            }
            if (descriptor != null && !descriptor.equals(invoke.desc)) {
                continue;
            }
            found.add(invoke);
        }
        return found;
    }

    private static String methodText(MethodNode methodNode) {
        StringBuilder builder = new StringBuilder();
        for (AbstractInsnNode instruction : methodNode.instructions) {
            if (instruction instanceof MethodInsnNode invoke) {
                builder.append(invoke.owner).append('.').append(invoke.name).append('\n');
            } else if (instruction instanceof org.objectweb.asm.tree.FieldInsnNode field) {
                builder.append(field.owner).append('.').append(field.name).append('\n');
            } else if (instruction instanceof org.objectweb.asm.tree.LdcInsnNode ldc && ldc.cst != null) {
                builder.append(ldc.cst).append('\n');
            }
        }
        return builder.toString();
    }

    private static MethodNode method(ClassNode typeNode, String methodName, String descriptor) {
        for (MethodNode methodNode : typeNode.methods) {
            if (!methodName.equals(methodNode.name)) {
                continue;
            }
            if (descriptor == null || descriptor.equals(methodNode.desc)) {
                return methodNode;
            }
        }
        return null;
    }

    private static List<AnnotationNode> classAnnotations(ClassNode typeNode) {
        List<AnnotationNode> found = new ArrayList<>();
        if (typeNode.visibleAnnotations != null) {
            found.addAll(typeNode.visibleAnnotations);
        }
        if (typeNode.invisibleAnnotations != null) {
            found.addAll(typeNode.invisibleAnnotations);
        }
        return found;
    }

    private static List<AnnotationNode> methodAnnotations(MethodNode methodNode) {
        List<AnnotationNode> found = new ArrayList<>();
        if (methodNode.visibleAnnotations != null) {
            found.addAll(methodNode.visibleAnnotations);
        }
        if (methodNode.invisibleAnnotations != null) {
            found.addAll(methodNode.invisibleAnnotations);
        }
        return found;
    }

    private static AnnotationNode findAnnotation(List<AnnotationNode> annotations, String descriptor) {
        for (AnnotationNode annotation : annotations) {
            if (descriptor.equals(annotation.desc)) {
                return annotation;
            }
        }
        return null;
    }

    private static boolean annotationContains(Object value, String expected) {
        if (expected.equals(value)) {
            return true;
        }
        if (value instanceof List<?> items) {
            return items.contains(expected);
        }
        if (value instanceof String[] items) {
            for (String item : items) {
                if (expected.equals(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Object annotationValue(AnnotationNode annotation, String key) {
        if (annotation == null || annotation.values == null) {
            return null;
        }
        for (int pairCursor = 0; pairCursor + 1 < annotation.values.size(); pairCursor += 2) {
            if (key.equals(annotation.values.get(pairCursor))) {
                return annotation.values.get(pairCursor + 1);
            }
        }
        return null;
    }

    private static ClassNode readJar(JarFile jarFile, String entryName) throws Exception {
        JarEntry entry = jarFile.getJarEntry(entryName);
        check(entry != null, "安装包含 " + entryName);
        try (InputStream input = jarFile.getInputStream(entry)) {
            ClassReader reader = new ClassReader(input);
            ClassNode typeNode = new ClassNode();
            reader.accept(typeNode, 0);
            return typeNode;
        }
    }

    private static ClassNode readClass(Path classFile) throws Exception {
        check(Files.isRegularFile(classFile), "已编译 " + classFile.getFileName());
        ClassReader reader = new ClassReader(Files.readAllBytes(classFile));
        ClassNode typeNode = new ClassNode();
        reader.accept(typeNode, 0);
        return typeNode;
    }

    private static Path firstExisting(Path... candidates) {
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static void check(boolean condition, String detail) {
        assertionCount++;
        if (!condition) {
            throw new IllegalStateException(detail);
        }
    }
}
