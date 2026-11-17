import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * 核对悦灵、恼鬼、末影龙的映射目标和潮湿飞行字节码。不调用 Mixin 服务，也不做变换。
 */
public final class WetnessFlightGate {
    private static final String ALLAY = "net/minecraft/world/entity/animal/allay/Allay";
    private static final String VEX = "net/minecraft/world/entity/monster/Vex";
    private static final String MONSTER = "net/minecraft/world/entity/monster/Monster";
    private static final String DRAGON = "net/minecraft/world/entity/boss/enderdragon/EnderDragon";
    private static final String TRAVEL = "(Lnet/minecraft/world/phys/Vec3;)V";
    private static final String MOVE = "(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V";
    private static final String REDIRECT_MOVE = "(Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V";
    private static final String WALLS = "(Lnet/minecraft/world/phys/AABB;)Z";
    private static final String TICK_PART = "(Lnet/minecraft/world/entity/boss/EnderDragonPart;DDD)V";
    private static final String ALLAY_MIXIN = "com/bslx/XueXiMod/mixin/WetnessAllayMixin";
    private static final String VEX_MIXIN = "com/bslx/XueXiMod/mixin/WetnessVexMixin";
    private static final String DRAGON_MIXIN = "com/bslx/XueXiMod/mixin/WetnessDragonMixin";
    private static final String VEX_MODIFY = "(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;";
    private static final String MIXIN_JAR = "C:/Users/baishang/.gradle/caches/modules-2/files-2.1/org.spongepowered/mixin/0.8.5/9d1c0c3a304ae6697ecd477218fa61b850bf57fc/mixin-0.8.5.jar";
    private static final String FLIGHT = "com/bslx/XueXiMod/status/WetnessFlight";
    private static final String CLIP = "com/bslx/XueXiMod/status/WetnessFlightClip";

    private final Path recompJar;
    private final Path classesRoot;
    private final Path mixinsJson;
    private final Path refmap;
    private int checks;
    private int failures;

    private WetnessFlightGate(Path recompJar, Path classesRoot, Path mixinsJson, Path refmap) {
        this.recompJar = recompJar;
        this.classesRoot = classesRoot;
        this.mixinsJson = mixinsJson;
        this.refmap = refmap;
    }

    public static void main(String[] arguments) throws Exception {
        Path project = Path.of("D:/alianximod");
        WetnessFlightGate gate = new WetnessFlightGate(
                Path.of("C:/Users/baishang/.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.20.1-47.3.33_mapped_parchment_2023.06.26-1.20.1/forge-1.20.1-47.3.33_mapped_parchment_2023.06.26-1.20.1-recomp.jar"),
                project.resolve("build/classes/java/main"),
                project.resolve("src/main/resources/xueximod.mixins.json"),
                project.resolve("build/tmp/compileJava/compileJava-refmap.json"));
        gate.run();
        System.out.println("断言 " + gate.checks + " 失败 " + gate.failures);
        System.out.println("启动证明 否");
        System.out.println("完整变换 未验证");
        if (gate.failures != 0) {
            System.exit(1);
        }
    }

    private void run() throws Exception {
        ClassNode allay = classFromJar(ALLAY);
        ClassNode vex = classFromJar(VEX);
        ClassNode dragon = classFromJar(DRAGON);
        check(allay != null && method(allay, "travel", TRAVEL) != null, "映射 Allay.travel" + TRAVEL + " 不存在");
        MethodNode vexMove = vex == null ? null : method(vex, "move", MOVE);
        check(vexMove != null, "映射 Vex.move" + MOVE + " 不存在");
        if (vexMove != null) {
            int monsterMoves = countInvokeOwner(vexMove, MONSTER, "move", MOVE);
            int entityMoves = countInvokeOwner(vexMove, "net/minecraft/world/entity/Entity", "move", MOVE);
            check(monsterMoves == 1 && entityMoves == 0,
                    "Vex.move 的父类调用不是唯一的 Monster.move，Monster " + monsterMoves + " Entity " + entityMoves);
        }
        MethodNode aiStep = dragon == null ? null : method(dragon, "aiStep", "()V");
        MethodNode tickDeath = dragon == null ? null : method(dragon, "tickDeath", "()V");
        check(aiStep != null && tickDeath != null, "映射 EnderDragon.aiStep 或 tickDeath 不存在");
        if (aiStep != null && tickDeath != null) {
            checkDragonOrder(aiStep, tickDeath);
        }
        ClassNode allayMixin = classFromClasses(ALLAY_MIXIN);
        ClassNode vexMixin = classFromClasses(VEX_MIXIN);
        ClassNode dragonMixin = classFromClasses(DRAGON_MIXIN);
        check(allayMixin != null && vexMixin != null && dragonMixin != null, "编译产物缺少飞行 Mixin");
        if (allayMixin != null) {
            checkAllay(allayMixin);
        }
        if (vexMixin != null) {
            checkVex(vexMixin);
        }
        if (dragonMixin != null) {
            checkDragonMixin(dragonMixin);
        }
        checkJson();
        checkRefmap();
        checkOutside();
        checkWiring();
        gap("没有调用 Mixin Transformer。映射方法和注解存在不等于悦灵、恼鬼、末影龙已经注入进正在运行的类。");
        gap("没有构造 Level。dragonObstacles 的空气和 DRAGON_TRANSPARENT 分支只看了字节码，没有对真实方块执行。");
        gap("没有构造恼鬼、末影龙或悦灵。beforeTravel、adjustVexMove 的裁短分支和 correctDragonDescent 的 setPos 没有在实体上运行。");
    }

    private void checkDragonOrder(MethodNode aiStep, MethodNode tickDeath) {
        int moveCount = countInvoke(aiStep, "move", MOVE);
        int deathMoves = countInvoke(tickDeath, "move", MOVE);
        int wallCount = countInvoke(aiStep, "checkWalls", WALLS);
        int firstMove = indexOfInvoke(aiStep, 0, "move", MOVE);
        int secondMove = indexOfInvoke(aiStep, 1, "move", MOVE);
        int lastPart = lastIndexOfInvoke(aiStep, "tickPart", TICK_PART);
        int knock = indexOfInvoke(aiStep, 0, "knockBack", null);
        int lastHurt = lastIndexOfInvoke(aiStep, "hurt", null);
        int firstWall = indexOfInvoke(aiStep, 0, "checkWalls", WALLS);
        int secondWall = indexOfInvoke(aiStep, 1, "checkWalls", WALLS);
        int thirdWall = indexOfInvoke(aiStep, 2, "checkWalls", WALLS);
        System.out.println("aiStep序号 move " + firstMove + "," + secondMove
                + " tickPart末 " + lastPart + " knock " + knock + " hurt末 " + lastHurt
                + " checkWalls " + firstWall + "," + secondWall + "," + thirdWall);
        check(moveCount == 2 && deathMoves == 1, "aiStep move 应为 2、tickDeath move 应为 1，实际 "
                + moveCount + " 和 " + deathMoves);
        AbstractInsnNode firstMoveInsn = invokeAt(aiStep, 0, "move", MOVE);
        AbstractInsnNode afterMove = firstMoveInsn == null ? null : nextReal(firstMoveInsn.getNext());
        check(afterMove instanceof JumpInsnNode jump && jump.getOpcode() == Opcodes.GOTO,
                "aiStep 两处 move 不是一处 goto 跳过另一处");
        check(wallCount == 3 && firstWall >= 0 && secondWall > firstWall && thirdWall > secondWall,
                "aiStep checkWalls 不是按顺序的三处，实际 " + wallCount);
        boolean order = firstMove >= 0 && secondMove > firstMove && secondMove < firstWall
                && lastPart > secondMove && lastPart < firstWall
                && knock >= 0 && knock < firstWall
                && lastHurt >= 0 && lastHurt < firstWall;
        if (!order) {
            System.out.println("aiStep调用序 " + invokeSequence(aiStep));
        }
        check(order, "飞行 move、tickPart、击退和伤害没有全部位于第一处 checkWalls 之前");
        AbstractInsnNode wallInsn = invokeAt(aiStep, 0, "checkWalls", WALLS);
        AbstractInsnNode beforeWall = wallInsn == null ? null : previousReal(wallInsn);
        check(beforeWall instanceof MethodInsnNode call && "getBoundingBox".equals(call.name),
                "第一处 checkWalls 的前一条调用不是 getBoundingBox");
    }

    private void checkAllay(ClassNode node) {
        checkMixinOwner(node, ALLAY);
        checkNoPublicStatic(node);
        MethodNode inject = annotated(node, "Lorg/spongepowered/asm/mixin/injection/Inject;");
        check(inject != null, "悦灵 Mixin 没有 Inject");
        if (inject == null) {
            return;
        }
        AnnotationNode marker = findAnnotation(annotationsOf(inject), "Lorg/spongepowered/asm/mixin/injection/Inject;");
        check(marker != null && annotationText(marker, "method").contains("travel"), "悦灵注入目标不是 travel");
        check("HEAD".equals(atValue(marker)), "悦灵注入不是 HEAD，实际 " + atValue(marker));
        check(instancePrivate(inject), "悦灵注入不是 private 实例方法");
        check(countInvokeOwner(inject, FLIGHT, "beforeTravel", null) == 1, "悦灵注入没有正好一次 beforeTravel");
        check(countAnnotation(node, "Lorg/spongepowered/asm/mixin/injection/ModifyArg;") == 0
                        && countAnnotation(node, "Lorg/spongepowered/asm/mixin/injection/ModifyArgs;") == 0,
                "悦灵 Mixin 多了参数修改");
    }

    private void checkVex(ClassNode node) throws IOException {
        checkMixinOwner(node, VEX);
        checkNoPublicStatic(node);
        String modifyArg = "Lorg/spongepowered/asm/mixin/injection/ModifyArg;";
        MethodNode modifier = annotated(node, modifyArg);
        check(modifier != null, "恼鬼 Mixin 没有 ModifyArg");
        check(countAnnotation(node, "Lorg/spongepowered/asm/mixin/injection/ModifyArgs;") == 0,
                "恼鬼 Mixin 仍有 ModifyArgs");
        List<String> syntheticArgs = forbiddenArgs(VEX_MIXIN);
        System.out.println("恼鬼字节码引用 " + (syntheticArgs.isEmpty() ? "无 Args$1 / ModifyArgs" : syntheticArgs));
        check(syntheticArgs.isEmpty(), "恼鬼 Mixin 类文件仍引用 " + syntheticArgs);
        if (modifier == null) {
            return;
        }
        AnnotationNode marker = findAnnotation(annotationsOf(modifier), modifyArg);
        AnnotationNode at = atNode(marker);
        String target = at == null ? "" : annotationText(at, "target");
        int argumentIndex = annotationInt(marker, "index", -1);
        int required = annotationInt(marker, "require", 0);
        check(annotationText(marker, "method").contains("move"), "恼鬼 ModifyArg 的方法不是 move");
        check(target.contains(MONSTER + ";move") && target.contains(MOVE),
                "恼鬼目标不是 Monster.move" + MOVE + " 实际 " + target);
        check(argumentIndex == 1, "恼鬼 ModifyArg 的 index 不是 1，实际 " + argumentIndex);
        check(required == 1, "恼鬼 ModifyArg 的 require 不是 1，实际 " + required);
        check(instancePrivate(modifier) && VEX_MODIFY.equals(modifier.desc),
                "恼鬼处理器签名不是 (MoverType,Vec3)Vec3，实际 " + modifier.desc);
        check(countInvokeOwner(modifier, FLIGHT, "adjustVexMove", null) == 1, "恼鬼注入没有正好一次 adjustVexMove");
        check(countInvoke(modifier, "move") == 0, "恼鬼处理器自己调用了 move");
        check(countAnnotation(node, "Lorg/spongepowered/asm/mixin/injection/Inject;") == 0, "恼鬼 Mixin 多了 Inject");
        ClassNode vex = classFromJar(VEX);
        MethodNode vanillaMove = vex == null ? null : method(vex, "move", MOVE);
        int specials = countInvokeOpcode(vanillaMove, Opcodes.INVOKESPECIAL, MONSTER, "move", MOVE);
        System.out.println("Vex.move INVOKESPECIAL Monster.move " + specials);
        check(specials == 1, "映射 Vex.move 里 Monster.move 的 INVOKESPECIAL 不是唯一一处，实际 " + specials);
    }

    private void checkDragonMixin(ClassNode node) {
        checkMixinOwner(node, DRAGON);
        checkNoPublicStatic(node);
        MethodNode begin = null;
        int stepHeads = 0;
        for (MethodNode method : node.methods) {
            AnnotationNode marker = findAnnotation(annotationsOf(method), "Lorg/spongepowered/asm/mixin/injection/Inject;");
            if (marker != null && annotationText(marker, "method").contains("aiStep") && "HEAD".equals(atValue(marker))) {
                stepHeads++;
                begin = method;
            }
        }
        String redirect = "Lorg/spongepowered/asm/mixin/injection/Redirect;";
        MethodNode move = annotated(node, redirect);
        MethodNode walls = annotated(node, "Lorg/spongepowered/asm/mixin/injection/ModifyArg;");
        check(begin != null && move != null && walls != null, "末影龙 Mixin 缺少 HEAD、move Redirect 或 checkWalls 钩子");
        check(stepHeads == 1, "末影龙 aiStep HEAD 注入不是唯一一处，实际 " + stepHeads);
        check(countAnnotation(node, redirect) == 1, "末影龙飞行 move 不是唯一的 Redirect");
        check(countAnnotation(node, "Lorg/spongepowered/asm/mixin/injection/ModifyArgs;") == 0,
                "末影龙 Mixin 仍有 ModifyArgs");
        List<String> syntheticArgs = forbiddenArgs(DRAGON_MIXIN);
        System.out.println("启动字节码引用 " + (syntheticArgs.isEmpty() ? "无 Args$1 / ModifyArgs" : syntheticArgs));
        check(syntheticArgs.isEmpty(), "末影龙 Mixin 类文件仍引用 " + syntheticArgs);
        check(countAnnotation(node, "Lorg/spongepowered/asm/mixin/injection/ModifyArg;") == 1, "末影龙改写了第一处以外的 checkWalls");
        if (begin != null) {
            AnnotationNode marker = findAnnotation(annotationsOf(begin), "Lorg/spongepowered/asm/mixin/injection/Inject;");
            check(annotationText(marker, "method").contains("aiStep") && "HEAD".equals(atValue(marker)),
                    "末影龙清理不是 aiStep HEAD");
            check(instancePrivate(begin) && countInvokeOwner(begin, FLIGHT, "beginDragonStep", null) == 1,
                    "末影龙 HEAD 没有 private 调用 beginDragonStep");
            check(!annotationText(atNode(marker), "target").contains("checkWalls"), "末影龙用 Inject 挂了 checkWalls");
        }
        if (move != null) {
            AnnotationNode marker = findAnnotation(annotationsOf(move), redirect);
            String target = annotationText(atNode(marker), "target");
            int required = annotationInt(marker, "require", 1);
            check(annotationText(marker, "method").contains("aiStep") && !annotationText(marker, "method").contains("tickDeath"),
                    "末影龙 move 钩子的方法不是 aiStep");
            check(target.contains(DRAGON + ";move") && target.contains(MOVE) && !target.contains("tickDeath"),
                    "末影龙 move 目标不是 EnderDragon.move，实际 " + target);
            check(required == 2, "末影龙 move Redirect 的 require 不是 2，实际 " + required);
            check(instancePrivate(move) && REDIRECT_MOVE.equals(move.desc),
                    "末影龙 move 处理器签名不是 (EnderDragon,MoverType,Vec3)V，实际 " + move.desc);
            String forwarded = forwardOriginalType(move);
            check(forwarded == null, forwarded == null ? "" : forwarded);
        }
        if (walls != null) {
            AnnotationNode marker = findAnnotation(annotationsOf(walls), "Lorg/spongepowered/asm/mixin/injection/ModifyArg;");
            AnnotationNode at = atNode(marker);
            String target = annotationText(at, "target");
            int ordinal = annotationInt(at, "ordinal", 0);
            int index = annotationInt(marker, "index", -1);
            check(annotationText(marker, "method").contains("aiStep"), "checkWalls 钩子不在 aiStep");
            check(target.contains("checkWalls") && target.contains(WALLS), "ModifyArg 目标不是 checkWalls" + WALLS + " 实际 " + target);
            check(ordinal == 0 && index == 0, "第一处 checkWalls 的 ordinal/index 不是 0/0，实际 " + ordinal + "/" + index);
            check(instancePrivate(walls) && walls.desc.endsWith(")Lnet/minecraft/world/phys/AABB;"),
                    "checkWalls 钩子不是返回 AABB 的 private 实例方法");
            check(countInvokeOwner(walls, FLIGHT, "correctDragonDescent", null) == 1, "checkWalls 钩子没有返回 correctDragonDescent");
            check(countField(walls, "head") >= 1 && countField(walls, "neck") >= 1 && countField(walls, "body") >= 1,
                    "checkWalls 钩子没有把当前头、颈、身传进去");
        }
        check(!mentionsDeath(node), "飞行钩子写到了 tickDeath");
    }

    private void checkWiring() throws IOException {
        ClassNode flight = classFromClasses(FLIGHT);
        ClassNode clip = classFromClasses(CLIP);
        check(flight != null && clip != null, "缺少 WetnessFlight 或 WetnessFlightClip");
        if (flight == null || clip == null) {
            return;
        }
        MethodNode clamp = method(flight, "clampDescent", null);
        MethodNode vexAdjust = method(flight, "adjustVexMove", null);
        MethodNode dragonAdjust = method(flight, "adjustDragonMove", null);
        MethodNode correct = method(flight, "correctDragonDescent", null);
        MethodNode begin = method(flight, "beginDragonStep", null);
        MethodNode partClip = method(flight, "clipDragonPart", null);
        MethodNode addLiving = addVertical(flight, "net/minecraft/world/entity/LivingEntity");
        MethodNode addPart = addVertical(flight, "net/minecraft/world/entity/boss/EnderDragonPart");
        MethodNode clipExtra = method(clip, "clipExtra", null);
        MethodNode strict = method(clip, "strictOverlap", null);
        MethodNode obstacles = method(clip, "dragonObstacles", null);
        check(clamp != null && vexAdjust != null && dragonAdjust != null && correct != null
                        && begin != null && partClip != null && addLiving != null && addPart != null
                        && clipExtra != null && strict != null && obstacles != null,
                "飞行方法不齐");
        if (clamp == null || vexAdjust == null || dragonAdjust == null || correct == null
                || begin == null || partClip == null || addLiving == null || addPart == null
                || clipExtra == null || strict == null || obstacles == null) {
            return;
        }
        int probe = indexOfInvoke(clamp, 0, "affected", null);
        int vexType = indexOfType(clamp, Opcodes.INSTANCEOF, VEX);
        int dragonType = indexOfType(clamp, Opcodes.INSTANCEOF, DRAGON);
        int resting = indexOfInvoke(clamp, 0, "isResting", null);
        int exempt = indexOfInvoke(clamp, 0, "isDescentExempt", null);
        check(probe < 0, "普通夹紧仍把 Probe.affected 当下降资格");
        check(countInvoke(clamp, "wetnessDescentActive") == 0, "普通夹紧仍把 WetnessFlyer 当下降资格");
        check(vexType >= 0 && dragonType >= 0 && resting >= 0 && exempt > resting,
                "速度夹紧没有在黑名单之前跳过恼鬼、末影龙和倒挂蝙蝠");
        check(indexOfType(clamp, Opcodes.INSTANCEOF, "net/minecraft/world/entity/FlyingMob") < 0
                        && indexOfType(clamp, Opcodes.INSTANCEOF, "net/minecraft/world/entity/animal/FlyingAnimal") < 0
                        && indexOfType(clamp, Opcodes.INSTANCEOF, ALLAY) < 0
                        && indexOfType(clamp, Opcodes.INSTANCEOF, "net/minecraft/world/entity/monster/Blaze") < 0
                        && indexOfType(clamp, Opcodes.INSTANCEOF, "net/minecraft/world/entity/boss/wither/WitherBoss") < 0,
                "普通夹紧仍要求 FlyingMob、悦灵、烈焰人或凋灵");
        check(indexOfType(clamp, Opcodes.INSTANCEOF, "net/minecraft/world/entity/player/Player") < 0
                        && countInvoke(clamp, "isAlive") >= 1
                        && countInvoke(clamp, "isClientSide") >= 1
                        && countInvoke(clamp, "isPassenger") >= 1
                        && countInvoke(clamp, "onGround") >= 1
                        && countInvoke(clamp, "actualFluid") >= 1
                        && countInvoke(clamp, "limitedVertical") >= 1
                        && countInvoke(clamp, "isDescentExempt") >= 1,
                "普通夹紧没有同时保留存活、服务端、乘客、落地、流体，并按同一黑名单夹紧");
        check(countInvoke(clamp, "fallDistance") == 0 && countInvoke(clamp, "hurt") == 0
                        && countField(clamp, "fallDistance") == 0,
                "速度夹紧改了摔落距离或调用了伤害");
        check(countInvoke(flight, "fallDistance") == 0 && countField(flight, "fallDistance") == 0
                        && countField(flight, "noPhysics") == 0 && countInvoke(flight, "setOnGround") == 0
                        && countInvoke(flight, "hurt") == 0,
                "飞行辅助改了摔落、穿模或着地");
        check(countInvokeOwner(vexAdjust, "net/minecraft/world/phys/AABB", "move", null) == 1
                        && countInvokeOwner(vexAdjust, CLIP, "clipExtra", null) == 1
                        && countInvoke(vexAdjust, "getBoundingBox") == 1
                        && countType(vexAdjust, Opcodes.NEW, "net/minecraft/world/phys/Vec3") == 1
                        && copiesHorizontal(vexAdjust)
                        && countInvoke(vexAdjust, "setPos") == 0
                        && countInvoke(vexAdjust, "setDeltaMovement") == 0,
                "恼鬼裁短没有从移动后的盒子只改 Y，或写了位置和速度");
        check(countOpcode(vexAdjust, Opcodes.ARETURN) >= 3, "恼鬼裁短缺少追加为 0 时的提前返回");
        check(countType(dragonAdjust, Opcodes.NEW, "net/minecraft/world/phys/Vec3") == 0
                        && returnsSlot(dragonAdjust, 2)
                        && countInvoke(dragonAdjust, "setPos") == 0
                        && countInvoke(dragonAdjust, "setDeltaMovement") == 0
                        && countInvoke(dragonAdjust, "put") >= 1
                        && countInvoke(dragonAdjust, "get") == 0
                        && countOpcode(dragonAdjust, Opcodes.DADD) == 0,
                "末影龙 move 没有原样返回向量，或把追加累加进了位移");
        check(countInvoke(begin, "remove") >= 1, "aiStep 开头没有清掉上一拍的龙追加");
        int cleared = indexOfInvoke(correct, 0, "remove", null);
        int exemptAt = indexOfInvoke(correct, 0, "isDescentExempt", null);
        int strictest = indexOfInvoke(correct, 0, "strictestExtra", null);
        int descend = indexOfInvoke(correct, 0, "addVertical", null);
        check(cleared >= 0 && exemptAt > cleared && descend > exemptAt && returnsBetween(correct, exemptAt, descend),
                "龙修正没有在豁免返回前清掉上一拍追加");
        int readBox = lastIndexOfInvoke(correct, "getBoundingBox", null);
        check(strictest >= 0 && descend > strictest && readBox > descend
                        && returnsBetween(correct, strictest, descend)
                        && countInvoke(correct, "rejectedDown") == 0
                        && countInvokeOwner(correct, "net/minecraft/world/phys/AABB", "move", null) == 0
                        && countOpcode(correct, Opcodes.DSUB) == 0,
                "龙修正没有在零追加时直接返回，也没有下移后返回新的头部盒子");
        check(countInvoke(flight, "rejectedDown") == 0, "生产位移调用了 rejectedDown");
        check(countInvoke(partClip, "getBoundingBox") == 1
                        && countInvoke(partClip, "expandTowards") == 1
                        && countInvokeOwner(partClip, CLIP, "dragonObstacles", null) == 1
                        && countInvokeOwner(partClip, CLIP, "clipExtra", null) == 1
                        && countInvokeOwner(partClip, "net/minecraft/world/phys/AABB", "move", null) == 0,
                "部件裁短没有使用当前盒子，或先把盒子上移");
        check(addsOnlyY(addLiving) && addsOnlyY(addPart), "下移没有只把 Y 加上允许的负值");
        int overlap = indexOfInvoke(clipExtra, 0, "strictOverlap", null);
        int collide = indexOfInvoke(clipExtra, 0, "collide", null);
        check(overlap >= 0 && collide > overlap, "clipExtra 没有先做严格重叠再 collide");
        check(countInvoke(strict, "joinIsNotEmpty") >= 1 && countField(strict, "AND") >= 1,
                "严格重叠没有用 BooleanOp.AND");
        check(countInvoke(obstacles, "getBlockCollisions") >= 1
                        && countInvoke(obstacles, "isAir") >= 1
                        && countField(obstacles, "DRAGON_TRANSPARENT") >= 1
                        && countInvoke(obstacles, "block") >= 1
                        && countInvoke(obstacles, "move") >= 1,
                "龙障碍没有在原版碰撞之外为非透明方块补整块");
        check(countInvoke(vexAdjust, "canAddExtra") >= 1 && countInvoke(dragonAdjust, "canAddExtra") >= 1,
                "恼鬼或末影龙没有走同一份追加资格");
        MethodNode canAdd = method(flight, "canAddExtra", null);
        check(canAdd != null && countInvoke(canAdd, "isClientSide") >= 1
                        && indexOfType(canAdd, Opcodes.INSTANCEOF, "net/minecraft/world/entity/player/Player") < 0
                        && countInvoke(canAdd, "isPassenger") >= 1
                        && countInvoke(canAdd, "actualFluid") >= 1
                        && countInvoke(canAdd, "isDescentExempt") >= 1,
                "追加下降没有同时问黑名单、客户端、乘客和真实流体");
        check(method(flight, "register", null) != null, "Probe 注册入口不在了");
        checkExemptBytecode();
        checkModifyArgFullArguments();
    }

    private void checkJson() throws IOException {
        String json = Files.readString(mixinsJson, StandardCharsets.UTF_8);
        check(json.contains("\"WetnessAllayMixin\"")
                        && json.contains("\"WetnessVexMixin\"")
                        && json.contains("\"WetnessDragonMixin\""),
                "xueximod.mixins.json 没有登记三个飞行 Mixin");
    }

    private void checkRefmap() throws IOException {
        if (!Files.isRegularFile(refmap)) {
            check(false, "缺少 refmap " + refmap);
            return;
        }
        String json = Files.readString(refmap, StandardCharsets.UTF_8);
        requireBody(json, ALLAY_MIXIN, ALLAY, TRAVEL);
        requireBody(json, VEX_MIXIN, MONSTER, MOVE);
        requireBody(json, DRAGON_MIXIN, DRAGON, MOVE);
        requireBody(json, DRAGON_MIXIN, DRAGON, WALLS);
    }

    private void requireBody(String json, String mixinName, String owner, String descriptor) {
        int matched = 0;
        int seen = 0;
        String quoted = "\"" + mixinName + "\"";
        int from = 0;
        while (from < json.length()) {
            int at = json.indexOf(quoted, from);
            if (at < 0) {
                break;
            }
            int open = json.indexOf('{', at);
            String body = balanced(json, open);
            if (body != null) {
                seen++;
                if (body.contains(owner) && body.contains(descriptor)) {
                    matched++;
                }
            }
            from = at + quoted.length();
        }
        check(seen >= 2 && matched >= 2, "refmap " + mixinName + " 见到 " + seen
                + " 份，含 " + owner + " 与 " + descriptor + " 的有 " + matched);
    }

    private void checkOutside() throws IOException {
        List<String> hits = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(classesRoot)) {
            for (Path file : walk.filter(path -> path.toString().endsWith(".class")).toList()) {
                ClassNode node = classFromFile(file);
                if (node == null || ALLAY_MIXIN.equals(node.name) || VEX_MIXIN.equals(node.name)
                        || DRAGON_MIXIN.equals(node.name)) {
                    continue;
                }
                if (mentionsFlightMixin(node)) {
                    hits.add(node.name);
                }
            }
        }
        check(hits.isEmpty(), "包外类引用了飞行实现 Mixin " + hits);
    }

    private void checkMixinOwner(ClassNode node, String owner) {
        String mixinTarget = annotationType(node.visibleAnnotations, "Lorg/spongepowered/asm/mixin/Mixin;");
        if (mixinTarget == null) {
            mixinTarget = annotationType(node.invisibleAnnotations, "Lorg/spongepowered/asm/mixin/Mixin;");
        }
        check(mixinTarget != null && mixinTarget.contains(owner), node.name + " 的 @Mixin 目标不是 " + owner + " 实际 " + mixinTarget);
    }

    private void checkNoPublicStatic(ClassNode node) {
        List<String> rejected = new ArrayList<>();
        for (MethodNode method : node.methods) {
            if (method.name.startsWith("<")) {
                continue;
            }
            if ((method.access & Opcodes.ACC_PUBLIC) != 0 && (method.access & Opcodes.ACC_STATIC) != 0) {
                rejected.add(method.name + method.desc);
            }
        }
        check(rejected.isEmpty(), node.name + " 有公开静态方法 " + rejected);
    }

    private static boolean mentionsDeath(ClassNode node) {
        for (MethodNode method : node.methods) {
            if (annotationText(findAny(method, "Lorg/spongepowered/asm/mixin/injection/Inject;"), "method").contains("tickDeath")
                    || annotationText(findAny(method, "Lorg/spongepowered/asm/mixin/injection/ModifyArgs;"), "method").contains("tickDeath")
                    || annotationText(findAny(method, "Lorg/spongepowered/asm/mixin/injection/Redirect;"), "method").contains("tickDeath")
                    || annotationText(findAny(method, "Lorg/spongepowered/asm/mixin/injection/ModifyArg;"), "method").contains("tickDeath")) {
                return true;
            }
        }
        return false;
    }

    private static boolean mentionsFlightMixin(ClassNode node) {
        if (containsFlight(node.superName) || containsFlightList(node.interfaces)) {
            return true;
        }
        for (FieldNode field : node.fields) {
            if (containsFlight(field.desc)) {
                return true;
            }
        }
        for (MethodNode method : node.methods) {
            if (containsFlight(method.desc) || containsFlight(method.signature)) {
                return true;
            }
            if (method.instructions == null) {
                continue;
            }
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode call && containsFlight(call.owner)) {
                    return true;
                }
                if (insn instanceof FieldInsnNode field && containsFlight(field.owner)) {
                    return true;
                }
                if (insn instanceof TypeInsnNode type && containsFlight(type.desc)) {
                    return true;
                }
                if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof org.objectweb.asm.Type type
                        && containsFlight(type.getInternalName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean returnsBetween(MethodNode method, int start, int end) {
        if (method == null || method.instructions == null || start < 0 || end <= start) {
            return false;
        }
        int index = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (index > start && index < end && insn.getOpcode() == Opcodes.ARETURN) {
                return true;
            }
            index++;
        }
        return false;
    }

    private static boolean addsOnlyY(MethodNode method) {
        return countInvoke(method, "setPos") == 1
                && countInvoke(method, "getX") >= 1
                && countInvoke(method, "getY") >= 1
                && countInvoke(method, "getZ") >= 1
                && countOpcode(method, Opcodes.DADD) >= 1
                && countOpcode(method, Opcodes.DSUB) == 0;
    }

    private static boolean copiesHorizontal(MethodNode method) {
        AbstractInsnNode init = invokeOwnerAt(method, "net/minecraft/world/phys/Vec3", "<init>", "(DDD)V");
        if (init == null) {
            return false;
        }
        boolean sawX = false;
        boolean sawZ = false;
        boolean sawAdd = false;
        AbstractInsnNode cursor = init;
        for (int step = 0; step < 16 && cursor != null; step++) {
            cursor = previousReal(cursor);
            if (cursor instanceof FieldInsnNode field && "net/minecraft/world/phys/Vec3".equals(field.owner)) {
                if ("x".equals(field.name)) {
                    sawX = true;
                }
                if ("z".equals(field.name)) {
                    sawZ = true;
                }
            }
            if (cursor != null && cursor.getOpcode() == Opcodes.DADD) {
                sawAdd = true;
            }
        }
        return sawX && sawZ && sawAdd;
    }

    private static boolean returnsSlot(MethodNode method, int slot) {
        if (method == null || method.instructions == null) {
            return false;
        }
        int returns = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() != Opcodes.ARETURN) {
                continue;
            }
            returns++;
            AbstractInsnNode previous = previousReal(insn);
            if (!(previous instanceof VarInsnNode load) || load.var != slot || load.getOpcode() != Opcodes.ALOAD) {
                return false;
            }
        }
        return returns >= 1;
    }

    private static boolean setsArgument(MethodNode method, int argument) {
        if (method == null || method.instructions == null) {
            return false;
        }
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call && "set".equals(call.name) && "(ILjava/lang/Object;)V".equals(call.desc)) {
                AbstractInsnNode cursor = insn;
                int wanted = Opcodes.ICONST_0 + argument;
                for (int step = 0; step < 6 && cursor != null; step++) {
                    cursor = previousReal(cursor);
                    if (cursor != null && cursor.getOpcode() == wanted) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean instancePrivate(MethodNode method) {
        return method != null
                && (method.access & Opcodes.ACC_PRIVATE) != 0
                && (method.access & Opcodes.ACC_STATIC) == 0;
    }

    private static MethodNode addVertical(ClassNode node, String parameter) {
        for (MethodNode method : node.methods) {
            if ("addVertical".equals(method.name) && method.desc.startsWith("(L" + parameter + ";")) {
                return method;
            }
        }
        return null;
    }

    private static MethodNode annotated(ClassNode node, String descriptor) {
        for (MethodNode method : node.methods) {
            if (findAnnotation(annotationsOf(method), descriptor) != null) {
                return method;
            }
        }
        return null;
    }

    private static int countAnnotation(ClassNode node, String descriptor) {
        int found = 0;
        for (MethodNode method : node.methods) {
            if (findAnnotation(annotationsOf(method), descriptor) != null) {
                found++;
            }
        }
        return found;
    }

    private static List<AnnotationNode> annotationsOf(MethodNode method) {
        List<AnnotationNode> joined = new ArrayList<>();
        if (method.visibleAnnotations != null) {
            joined.addAll(method.visibleAnnotations);
        }
        if (method.invisibleAnnotations != null) {
            joined.addAll(method.invisibleAnnotations);
        }
        return joined;
    }

    private static AnnotationNode findAny(MethodNode method, String descriptor) {
        return findAnnotation(annotationsOf(method), descriptor);
    }

    private static String atValue(AnnotationNode marker) {
        AnnotationNode at = atNode(marker);
        return at == null ? "" : annotationText(at, "value");
    }

    private static AnnotationNode atNode(AnnotationNode marker) {
        Object at = annotationRaw(marker, "at");
        if (at instanceof AnnotationNode node) {
            return node;
        }
        if (at instanceof List<?> items) {
            for (Object item : items) {
                if (item instanceof AnnotationNode node) {
                    return node;
                }
            }
        }
        return null;
    }

    private static String annotationType(List<AnnotationNode> annotations, String descriptor) {
        AnnotationNode marker = findAnnotation(annotations, descriptor);
        if (marker == null) {
            return null;
        }
        return typeText(annotationRaw(marker, "value"));
    }

    private static String typeText(Object value) {
        if (value instanceof org.objectweb.asm.Type type) {
            return type.getInternalName();
        }
        if (value instanceof List<?> items) {
            StringBuilder joined = new StringBuilder();
            for (Object item : items) {
                joined.append(typeText(item)).append(' ');
            }
            return joined.toString();
        }
        return value == null ? "" : String.valueOf(value);
    }

    private static AnnotationNode findAnnotation(List<AnnotationNode> annotations, String descriptor) {
        if (annotations == null) {
            return null;
        }
        for (AnnotationNode annotation : annotations) {
            if (descriptor.equals(annotation.desc)) {
                return annotation;
            }
        }
        return null;
    }

    private static Object annotationRaw(AnnotationNode annotation, String key) {
        if (annotation == null || annotation.values == null) {
            return null;
        }
        for (int cursor = 0; cursor + 1 < annotation.values.size(); cursor += 2) {
            if (key.equals(annotation.values.get(cursor))) {
                return annotation.values.get(cursor + 1);
            }
        }
        return null;
    }

    private static String annotationText(AnnotationNode annotation, String key) {
        Object value = annotationRaw(annotation, key);
        if (value instanceof List<?> items && !items.isEmpty()) {
            return String.valueOf(items.get(0));
        }
        return value == null ? "" : String.valueOf(value);
    }

    private static int annotationInt(AnnotationNode annotation, String key, int fallback) {
        Object value = annotationRaw(annotation, key);
        if (value instanceof Integer number) {
            return number;
        }
        return fallback;
    }

    private static String balanced(String json, int open) {
        if (open < 0 || open >= json.length()) {
            return null;
        }
        int depth = 0;
        for (int cursor = open; cursor < json.length(); cursor++) {
            char current = json.charAt(cursor);
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return json.substring(open, cursor + 1);
                }
            }
        }
        return null;
    }

    private static int countInvoke(MethodNode method, String name) {
        return countInvoke(method, name, null);
    }

    private static int countInvoke(MethodNode method, String name, String descriptor) {
        return countInvokeOwner(method, null, name, descriptor);
    }

    private static int countInvoke(ClassNode node, String name) {
        int found = 0;
        for (MethodNode method : node.methods) {
            found += countInvoke(method, name);
        }
        return found;
    }

    private static int countInvokeOwner(MethodNode method, String owner, String name, String descriptor) {
        if (method == null || method.instructions == null) {
            return 0;
        }
        int found = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call && name.equals(call.name)
                    && (owner == null || owner.equals(call.owner))
                    && (descriptor == null || descriptor.equals(call.desc))) {
                found++;
            }
        }
        return found;
    }

    private static int countField(ClassNode node, String name) {
        int found = 0;
        for (MethodNode method : node.methods) {
            found += countField(method, name);
        }
        return found;
    }

    private static int countField(MethodNode method, String name) {
        if (method == null || method.instructions == null) {
            return 0;
        }
        int found = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof FieldInsnNode field && name.equals(field.name)) {
                found++;
            }
        }
        return found;
    }

    private static int countOpcode(MethodNode method, int opcode) {
        if (method == null || method.instructions == null) {
            return 0;
        }
        int found = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == opcode) {
                found++;
            }
        }
        return found;
    }

    private static int countType(MethodNode method, int opcode, String descriptor) {
        if (method == null || method.instructions == null) {
            return 0;
        }
        int found = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == opcode && insn instanceof TypeInsnNode type && descriptor.equals(type.desc)) {
                found++;
            }
        }
        return found;
    }

    private static int indexOfInvoke(MethodNode method, int occurrence, String name, String descriptor) {
        if (method == null || method.instructions == null) {
            return -1;
        }
        int seen = 0;
        int index = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call && name.equals(call.name)
                    && (descriptor == null || descriptor.equals(call.desc))) {
                if (seen == occurrence) {
                    return index;
                }
                seen++;
            }
            index++;
        }
        return -1;
    }

    private static int lastIndexOfInvoke(MethodNode method, String name, String descriptor) {
        if (method == null || method.instructions == null) {
            return -1;
        }
        int found = -1;
        int index = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call && name.equals(call.name)
                    && (descriptor == null || descriptor.equals(call.desc))) {
                found = index;
            }
            index++;
        }
        return found;
    }

    private static int indexOfType(MethodNode method, int opcode, String descriptor) {
        if (method == null || method.instructions == null) {
            return -1;
        }
        int index = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == opcode && insn instanceof TypeInsnNode type && descriptor.equals(type.desc)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private static AbstractInsnNode invokeAt(MethodNode method, int occurrence, String name, String descriptor) {
        if (method == null || method.instructions == null) {
            return null;
        }
        int seen = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call && name.equals(call.name)
                    && (descriptor == null || descriptor.equals(call.desc))) {
                if (seen == occurrence) {
                    return insn;
                }
                seen++;
            }
        }
        return null;
    }

    private static AbstractInsnNode invokeOwnerAt(MethodNode method, String owner, String name, String descriptor) {
        if (method == null || method.instructions == null) {
            return null;
        }
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call && owner.equals(call.owner) && name.equals(call.name)
                    && descriptor.equals(call.desc)) {
                return insn;
            }
        }
        return null;
    }

    private static String invokeSequence(MethodNode method) {
        StringBuilder joined = new StringBuilder();
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call
                    && ("move".equals(call.name) || "tickPart".equals(call.name) || "checkWalls".equals(call.name)
                    || "knockBack".equals(call.name) || "hurt".equals(call.name))) {
                if (joined.length() > 0) {
                    joined.append(" | ");
                }
                joined.append(call.name);
            }
        }
        return joined.toString();
    }

    private static AbstractInsnNode nextReal(AbstractInsnNode insn) {
        AbstractInsnNode cursor = insn;
        while (cursor != null && cursor.getOpcode() < 0) {
            cursor = cursor.getNext();
        }
        return cursor;
    }

    private static AbstractInsnNode previousReal(AbstractInsnNode insn) {
        AbstractInsnNode previous = insn == null ? null : insn.getPrevious();
        while (previous != null && previous.getOpcode() < 0) {
            previous = previous.getPrevious();
        }
        return previous;
    }

    private List<String> forbiddenArgs(String internalName) {
        String[] tokens = {
                "Args$1",
                "org/spongepowered/asm/synthetic/args/Args",
                "org/spongepowered/asm/mixin/injection/invoke/arg/Args",
                "Lorg/spongepowered/asm/mixin/injection/ModifyArgs;"
        };
        List<String> hits = new ArrayList<>();
        Path file = classesRoot.resolve(internalName + ".class");
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file);
        } catch (IOException failure) {
            hits.add("无法读取 " + file);
            return hits;
        }
        for (String token : tokens) {
            if (containsAscii(bytes, token)) {
                hits.add(token);
            }
        }
        return hits;
    }

    private static boolean containsAscii(byte[] bytes, String token) {
        byte[] needle = token.getBytes(StandardCharsets.US_ASCII);
        if (needle.length == 0 || bytes.length < needle.length) {
            return false;
        }
        for (int start = 0; start <= bytes.length - needle.length; start++) {
            boolean matched = true;
            for (int offset = 0; offset < needle.length; offset++) {
                if (bytes[start + offset] != needle[offset]) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return true;
            }
        }
        return false;
    }

    private static String forwardOriginalType(MethodNode method) {
        if (method.instructions == null) {
            return "末影龙 move 处理器没有指令";
        }
        int[] locals = new int[16];
        for (int slot = 0; slot < locals.length; slot++) {
            locals[slot] = slot;
        }
        int[] stack = new int[24];
        int depth = 0;
        int adjusts = 0;
        int moves = 0;
        boolean adjustSawOriginal = false;
        boolean moveSawOriginal = false;
        boolean adjustBeforeMove = false;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            int opcode = insn.getOpcode();
            if (opcode < 0) {
                continue;
            }
            if (opcode == Opcodes.ALOAD && insn instanceof VarInsnNode load) {
                if (depth >= stack.length || load.var < 0 || load.var >= locals.length) {
                    return "末影龙 move 处理器局部变量超出预估";
                }
                stack[depth++] = locals[load.var];
                continue;
            }
            if (opcode == Opcodes.ASTORE && insn instanceof VarInsnNode store) {
                if (depth <= 0 || store.var < 0 || store.var >= locals.length) {
                    return "末影龙 move 处理器存储时栈空";
                }
                locals[store.var] = stack[--depth];
                continue;
            }
            if (opcode == Opcodes.CHECKCAST) {
                continue;
            }
            if (opcode == Opcodes.RETURN) {
                if (depth != 0) {
                    return "末影龙 move 处理器返回时栈上还有 " + depth + " 个值";
                }
                continue;
            }
            if (insn instanceof MethodInsnNode call && "adjustDragonMove".equals(call.name) && FLIGHT.equals(call.owner)) {
                if (depth < 3) {
                    return "adjustDragonMove 参数不足";
                }
                int movement = stack[--depth];
                int mover = stack[--depth];
                int receiver = stack[--depth];
                adjustSawOriginal = receiver == 1 && mover == 2 && movement == 3;
                stack[depth++] = 100;
                adjusts++;
                continue;
            }
            if (insn instanceof MethodInsnNode call && "move".equals(call.name) && MOVE.equals(call.desc)) {
                if (depth < 3) {
                    return "move 参数不足";
                }
                int movement = stack[--depth];
                int mover = stack[--depth];
                int receiver = stack[--depth];
                moveSawOriginal = receiver == 1 && mover == 2 && movement == 100;
                adjustBeforeMove = adjusts == 1 && moves == 0;
                moves++;
                continue;
            }
            return "末影龙 move 处理器出现未预期指令 " + opcode;
        }
        if (adjusts != 1 || moves != 1 || !adjustBeforeMove) {
            return "末影龙 move 不是先 adjustDragonMove 一次再 move 一次，实际 adjust " + adjusts + " move " + moves;
        }
        if (!adjustSawOriginal) {
            return "adjustDragonMove 没有收到原来的 receiver、type 和 movement";
        }
        if (!moveSawOriginal) {
            return "move 没有把原来的 type 和 adjust 结果传回去";
        }
        return null;
    }

    private static boolean containsFlight(String text) {
        return text != null && (text.contains(ALLAY_MIXIN) || text.contains(VEX_MIXIN) || text.contains(DRAGON_MIXIN));
    }

    private static boolean containsFlightList(List<String> names) {
        if (names == null) {
            return false;
        }
        for (String name : names) {
            if (containsFlight(name)) {
                return true;
            }
        }
        return false;
    }

    private ClassNode classFromJar(String internalName) throws IOException {
        return classFromExplicitJar(recompJar, internalName);
    }

    private ClassNode classFromExplicitJar(Path jarPath, String internalName) throws IOException {
        if (!Files.isRegularFile(jarPath)) {
            check(false, "找不到 jar " + jarPath);
            return null;
        }
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry entry = jar.getJarEntry(internalName + ".class");
            if (entry == null) {
                return null;
            }
            try (InputStream input = jar.getInputStream(entry)) {
                ClassReader reader = new ClassReader(input);
                ClassNode node = new ClassNode();
                reader.accept(node, 0);
                return node;
            }
        }
    }

    private void checkExemptBytecode() throws IOException {
        ClassNode exempt = classFromClasses("com/bslx/XueXiMod/status/WetnessFlightExempt");
        check(exempt != null, "编译产物缺少 WetnessFlightExempt");
        if (exempt == null) {
            return;
        }
        MethodNode syntax = method(exempt, "validSyntax", null);
        MethodNode exact = method(exempt, "exactId", null);
        MethodNode replace = method(exempt, "replace", null);
        check(syntax != null && exact != null && replace != null, "豁免语法、解析或替换方法不齐");
        check(!touchesOwner(syntax, "net/minecraftforge/registries/ForgeRegistries")
                        && !touchesOwner(exact, "net/minecraftforge/registries/ForgeRegistries")
                        && !touchesOwner(replace, "net/minecraftforge/registries/ForgeRegistries"),
                "下降黑名单在加载语法时查了注册表");
        ClassNode config = classFromClasses("com/bslx/XueXiMod/Config");
        MethodNode load = config == null ? null : method(config, "onLoad", null);
        check(load != null && countInvokeOwner(load, "com/bslx/XueXiMod/status/WetnessFlightExempt", "replace", null) >= 1,
                "配置加载没有调用 WetnessFlightExempt.replace");
    }

    private void checkModifyArgFullArguments() throws IOException {
        ClassNode injector = classFromExplicitJar(Path.of(MIXIN_JAR),
                "org/spongepowered/asm/mixin/injection/invoke/ModifyArgInjector");
        check(injector != null, "安装的 Mixin 里没有 ModifyArgInjector");
        if (injector == null) {
            return;
        }
        MethodNode single = method(injector, "injectSingleArgHandler", null);
        MethodNode multi = method(injector, "injectMultiArgHandler", null);
        MethodNode atInvoke = method(injector, "injectAtInvoke", null);
        MethodNode constructor = method(injector, "<init>", null);
        check(single != null && multi != null && atInvoke != null && constructor != null,
                "ModifyArgInjector 没有单参数和全参数两条注入");
        boolean choosesByLength = constructor != null
                && countOpcode(constructor, Opcodes.ARRAYLENGTH) >= 1
                && countOpcode(constructor, Opcodes.IF_ICMPNE) >= 1;
        check(choosesByLength, "ModifyArgInjector 构造器没有按参数个数切换单参数模式");
        check(multi != null && countOpcode(multi, Opcodes.ARRAYLENGTH) >= 1,
                "全参数 ModifyArg 没有核对处理器参数个数");
        check(atInvoke != null && countInvoke(atInvoke, "injectMultiArgHandler") >= 1
                        && countInvoke(atInvoke, "injectSingleArgHandler") >= 1,
                "injectAtInvoke 没有同时保留单参数和全参数分支");
        System.out.println("ModifyArg 全参数 安装的校验器含 injectMultiArgHandler");
    }

    private static int countInvokeOpcode(MethodNode method, int opcode, String owner, String name, String descriptor) {
        if (method == null || method.instructions == null) {
            return 0;
        }
        int found = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call && call.getOpcode() == opcode && owner.equals(call.owner)
                    && name.equals(call.name) && descriptor.equals(call.desc)) {
                found++;
            }
        }
        return found;
    }

    private static boolean touchesOwner(MethodNode method, String owner) {
        if (method == null || method.instructions == null) {
            return false;
        }
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call && owner.equals(call.owner)) {
                return true;
            }
            if (insn instanceof FieldInsnNode field && owner.equals(field.owner)) {
                return true;
            }
        }
        return false;
    }

    private ClassNode classFromClasses(String internalName) throws IOException {
        return classFromFile(classesRoot.resolve(internalName + ".class"));
    }

    private static ClassNode classFromFile(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        ClassReader reader = new ClassReader(Files.readAllBytes(file));
        ClassNode node = new ClassNode();
        reader.accept(node, 0);
        return node;
    }

    private static MethodNode method(ClassNode node, String name, String descriptor) {
        if (node == null) {
            return null;
        }
        MethodNode found = null;
        int seen = 0;
        for (MethodNode method : node.methods) {
            if (!name.equals(method.name)) {
                continue;
            }
            if (descriptor != null && !descriptor.equals(method.desc)) {
                continue;
            }
            seen++;
            found = method;
        }
        return seen == 1 ? found : null;
    }

    private void check(boolean condition, String detail) {
        checks++;
        if (!condition) {
            failures++;
            System.out.println("失败[" + checks + "] " + detail);
        }
    }

    private static void gap(String detail) {
        System.out.println("未覆盖 " + detail);
    }
}
