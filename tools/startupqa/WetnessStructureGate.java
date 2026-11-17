import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
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
 * 核对潮湿 Mixin 的映射目标和接入字节码。不调用 Mixin 服务，也不做变换。
 */
public final class WetnessStructureGate {
    private static final String LIVING = "net/minecraft/world/entity/LivingEntity";
    private static final String FLYING = "net/minecraft/world/entity/FlyingMob";
    private static final String TRAVEL_DESC = "(Lnet/minecraft/world/phys/Vec3;)V";
    private static final String STEP_DESC = "()V";
    private static final String LIVING_MIXIN = "com/bslx/XueXiMod/mixin/WetnessLivingMixin";
    private static final String FLYING_MIXIN = "com/bslx/XueXiMod/mixin/WetnessFlyingMixin";
    private static final String SPEED_UUID = "6b8e1d24-5c7a-4f93-9e10-2a4c8d6f0b31";

    private final Path recompJar;
    private final Path classesRoot;
    private final Path mixinsJson;
    private final Path refmap;
    private int checks;
    private int failures;

    private WetnessStructureGate(Path recompJar, Path classesRoot, Path mixinsJson, Path refmap) {
        this.recompJar = recompJar;
        this.classesRoot = classesRoot;
        this.mixinsJson = mixinsJson;
        this.refmap = refmap;
    }

    public static void main(String[] arguments) throws Exception {
        Path project = Path.of("D:/alianximod");
        WetnessStructureGate gate = new WetnessStructureGate(
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
        ClassNode living = classFromJar(LIVING);
        ClassNode flying = classFromJar(FLYING);
        MethodNode livingTravel = method(living, "travel", TRAVEL_DESC);
        MethodNode flyingTravel = method(flying, "travel", TRAVEL_DESC);
        MethodNode livingStep = method(living, "aiStep", STEP_DESC);
        check(livingTravel != null, "映射 LivingEntity.travel" + TRAVEL_DESC + " 不存在");
        check(flyingTravel != null, "映射 FlyingMob.travel" + TRAVEL_DESC + " 不存在");
        check(livingStep != null, "映射 LivingEntity.aiStep()V 不存在");
        if (livingTravel != null) {
            check(livingTravel.instructions != null, "LivingEntity.travel 没有指令，HEAD 无法定位");
        }
        if (flyingTravel != null) {
            check(flyingTravel.instructions != null, "FlyingMob.travel 没有指令，HEAD 无法定位");
        }
        if (livingStep != null) {
            check(countOpcode(livingStep, Opcodes.RETURN) >= 1, "LivingEntity.aiStep 没有 RETURN，尾部注入点不存在");
        }
        ClassNode livingMixin = classFromClasses(LIVING_MIXIN);
        ClassNode flyingMixin = classFromClasses(FLYING_MIXIN);
        check(livingMixin != null, "编译产物缺少 WetnessLivingMixin");
        check(flyingMixin != null, "编译产物缺少 WetnessFlyingMixin");
        if (livingMixin != null) {
            checkMixin(livingMixin, LIVING, "travel", "HEAD",
                    "com/bslx/XueXiMod/status/WetnessFlight", "beforeTravel");
            checkMixin(livingMixin, LIVING, "aiStep", "RETURN",
                    "com/bslx/XueXiMod/status/WetnessSensitivity", "afterAiStep");
            checkNoPublicStatic(livingMixin);
        }
        if (flyingMixin != null) {
            checkMixin(flyingMixin, FLYING, "travel", "HEAD",
                    "com/bslx/XueXiMod/status/WetnessFlight", "beforeTravel");
            checkNoPublicStatic(flyingMixin);
        }
        checkJson();
        checkRefmap();
        checkOutside();
        checkWiring();
        gap("没有调用 Mixin Transformer。映射方法存在不等于两份 travel 和 aiStep 已经注入进正在运行的类。");
    }

    private void checkMixin(ClassNode node, String owner, String methodName, String atValue, String helperOwner, String helperName) {
        String mixinTarget = annotationType(node.visibleAnnotations, "Lorg/spongepowered/asm/mixin/Mixin;");
        if (mixinTarget == null) {
            mixinTarget = annotationType(node.invisibleAnnotations, "Lorg/spongepowered/asm/mixin/Mixin;");
        }
        check(mixinTarget != null && mixinTarget.contains(owner), node.name + " 的 @Mixin 目标不是 " + owner + " 实际 " + mixinTarget);
        MethodNode inject = null;
        for (MethodNode method : node.methods) {
            AnnotationNode marker = injectMarker(method, methodName, atValue);
            if (marker != null) {
                inject = method;
                break;
            }
        }
        check(inject != null, node.name + " 没有 @" + atValue + " 注入 " + methodName);
        if (inject == null) {
            return;
        }
        boolean instancePrivate = (inject.access & Opcodes.ACC_PRIVATE) != 0 && (inject.access & Opcodes.ACC_STATIC) == 0;
        check(instancePrivate, node.name + "." + inject.name + " 不是 private 实例方法，access " + inject.access);
        int helpers = countInvoke(inject, helperOwner, helperName);
        check(helpers == 1, node.name + " 调用 " + helperName + " 次数 " + helpers);
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

    private void checkJson() throws IOException {
        String json = Files.readString(mixinsJson, StandardCharsets.UTF_8);
        check(json.contains("\"WetnessLivingMixin\"") && json.contains("\"WetnessFlyingMixin\""),
                "xueximod.mixins.json 没有登记两个潮湿 Mixin");
    }

    private void checkRefmap() throws IOException {
        if (!Files.isRegularFile(refmap)) {
            check(false, "缺少 refmap " + refmap);
            return;
        }
        String json = Files.readString(refmap, StandardCharsets.UTF_8);
        requireRefmap(json, LIVING_MIXIN, "travel", LIVING, TRAVEL_DESC);
        requireRefmap(json, LIVING_MIXIN, "aiStep", LIVING, STEP_DESC);
        requireRefmap(json, FLYING_MIXIN, "travel", FLYING, TRAVEL_DESC);
    }

    private void requireRefmap(String json, String mixinName, String methodKey, String owner, String descriptor) {
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
                String value = jsonString(body, methodKey);
                if (value != null && value.contains(owner) && value.contains(descriptor)) {
                    matched++;
                }
            }
            from = at + quoted.length();
        }
        check(seen >= 2 && matched >= 2, "refmap " + mixinName + " " + methodKey
                + " 见到 " + seen + " 份，owner/descriptor 相符 " + matched);
    }

    private void checkOutside() throws IOException {
        List<String> hits = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(classesRoot)) {
            for (Path file : walk.filter(path -> path.toString().endsWith(".class")).toList()) {
                ClassNode node = classFromFile(file);
                if (node == null || LIVING_MIXIN.equals(node.name) || FLYING_MIXIN.equals(node.name)) {
                    continue;
                }
                if (mentionsMixin(node)) {
                    hits.add(node.name);
                }
            }
        }
        check(hits.isEmpty(), "包外类引用了潮湿实现 Mixin " + hits);
    }

    private boolean mentionsMixin(ClassNode node) {
        if (containsMixin(node.superName) || containsAny(node.interfaces)) {
            return true;
        }
        for (FieldNode field : node.fields) {
            if (containsMixin(field.desc)) {
                return true;
            }
        }
        for (MethodNode method : node.methods) {
            if (containsMixin(method.desc) || containsMixin(method.signature)) {
                return true;
            }
            if (method.instructions == null) {
                continue;
            }
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode call && containsMixin(call.owner)) {
                    return true;
                }
                if (insn instanceof org.objectweb.asm.tree.FieldInsnNode field && containsMixin(field.owner)) {
                    return true;
                }
                if (insn instanceof org.objectweb.asm.tree.TypeInsnNode type && containsMixin(type.desc)) {
                    return true;
                }
                if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof org.objectweb.asm.Type type
                        && containsMixin(type.getInternalName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void checkWiring() throws IOException {
        ClassNode tiers = classFromClasses("com/bslx/XueXiMod/status/WetnessTiers");
        ClassNode flight = classFromClasses("com/bslx/XueXiMod/status/WetnessFlight");
        ClassNode sensitivity = classFromClasses("com/bslx/XueXiMod/status/WetnessSensitivity");
        ClassNode breath = classFromClasses("com/bslx/XueXiMod/shu/technique/release/domain/PressureBreath");
        ClassNode effect = classFromClasses("com/bslx/XueXiMod/status/AttributeStateMobEffect");
        ClassNode reward = classFromClasses("com/bslx/XueXiMod/shu/technique/release/momentum/WetAttackReward");
        ClassNode events = classFromClasses("com/bslx/XueXiMod/shu/technique/release/momentum/ReleaseBaseEvents");
        ClassNode heavy = classFromClasses("com/bslx/XueXiMod/shu/technique/release/momentum/HeavyWater");
        ClassNode pressure = classFromClasses("com/bslx/XueXiMod/shu/technique/release/payload/WetPressure");
        ClassNode arrow = classFromClasses("com/bslx/XueXiMod/shu/technique/release/deflection/projectile/ArrowFlight");
        ClassNode melee = classFromClasses("com/bslx/XueXiMod/shu/technique/release/deflection/melee/MeleeDeflection");
        ClassNode tally = classFromClasses("com/bslx/XueXiMod/shu/technique/release/ReleaseBatch$Tally");
        ClassNode hits = classFromClasses("com/bslx/XueXiMod/shu/technique/release/TechniqueHits");
        check(tiers != null && flight != null && sensitivity != null && breath != null && effect != null
                        && reward != null && events != null && heavy != null && pressure != null
                        && arrow != null && melee != null && tally != null && hits != null,
                "潮湿接线 class 不齐");
        if (tiers == null || flight == null || sensitivity == null || breath == null || effect == null
                || reward == null || events == null || heavy == null || pressure == null
                || arrow == null || melee == null || tally == null || hits == null) {
            return;
        }
        MethodNode ofEntity = method(tiers, "of", "(Lnet/minecraft/world/entity/LivingEntity;)Lcom/bslx/XueXiMod/status/WetnessTiers$Profile;");
        check(ofEntity != null && countInvoke(ofEntity, "isAlive") == 0
                        && countInvoke(ofEntity, "com/bslx/XueXiMod/status/WetnessState", "read") == 1
                        && countInvoke(ofEntity, "com/bslx/XueXiMod/status/WetnessTiers", "ofValue") == 2,
                "WetnessTiers.of 没有按当前湿度读取，或调用了 isAlive");
        MethodNode clamp = method(flight, "clampDescent", null);
        check(clamp != null && countInvoke(clamp, "limitedVertical") == 1 && countInvoke(clamp, "descentEligible") == 1
                        && countInvoke(clamp, "setDeltaMovement") == 1 && countInvoke(clamp, "setBlock") == 0,
                "下降入口没有接到夹取、资格和写回速度");
        check(countInvoke(flight, "setBlock") == 0, "WetnessFlight 写了方块");
        MethodNode afterStep = method(sensitivity, "afterAiStep", "(Lnet/minecraft/world/entity/LivingEntity;)V");
        check(afterStep != null && countInvoke(afterStep, "isSensitiveToWater") == 1
                        && countInvoke(afterStep, "isInWaterRainOrBubble") == 1
                        && countInvoke(afterStep, "drown") == 1 && countInvoke(afterStep, "hurt") == 1
                        && countInvoke(afterStep, "setSwimming") == 0 && countInvoke(afterStep, "clearFire") == 0,
                "怕水入口不是敏感判断后的 drown hurt");
        MethodNode onBreathe = method(breath, "onBreathe", null);
        MethodNode marks = method(breath, "marksSuffocation", null);
        check(onBreathe != null && countInvoke(onBreathe, "resists") == 1 && countInvoke(onBreathe, "combineBreath") == 1
                        && countInvoke(onBreathe, "setCanBreathe") == 1 && countInvoke(onBreathe, "setCanRefillAir") == 1
                        && countInvoke(onBreathe, "setConsumeAirAmount") == 1 && countInvoke(onBreathe, "setAirSupply") == 0,
                "呼吸入口没有同时拒绝呼吸、拒绝回气并写回消耗");
        check(marks != null && countInvoke(marks, "com/bslx/XueXiMod/status/WetnessTiers", null) == 0,
                "marksSuffocation 读了潮湿");
        check(countInvoke(breath, "setAirSupply") == 0, "PressureBreath 写了空气值");
        check(containsString(effect, SPEED_UUID), "移速 UUID 不在 AttributeStateMobEffect");
        MethodNode addModifiers = method(effect, "addAttributeModifiers", null);
        MethodNode removeModifiers = method(effect, "removeAttributeModifiers", null);
        MethodNode amplifierMap = method(effect, "wetnessForAmplifier", "(I)I");
        check(addModifiers != null && invokeOrder(addModifiers, "ofValue", "removeModifier", "addPermanentModifier")
                        && countInvoke(addModifiers, "movementReduction") == 1
                        && countInvoke(addModifiers, "MULTIPLY_TOTAL") + countField(addModifiers, "MULTIPLY_TOTAL") >= 1,
                "潮湿移速不是先删后加的 MULTIPLY_TOTAL");
        check(removeModifiers != null && countInvoke(removeModifiers, "removeModifier") == 1, "移除效果时没有去掉移速修饰");
        check(amplifierMap != null && containsInt(amplifierMap, 76) && containsInt(amplifierMap, 51)
                        && containsInt(amplifierMap, 26) && countOpcode(amplifierMap, Opcodes.ICONST_1) >= 1,
                "药水 amplifier 没有映射到 1/26/51/76");
        check(countInvoke(reward, "isAlive") == 0 && countInvoke(reward, "credit") == 1
                        && countInvoke(reward, "attackReward") == 1
                        && countField(reward, "IS_DROWNING") == 1
                        && countField(reward, "PLAYER_ATTACK") == 1
                        && countField(reward, "ARROW") == 1
                        && countInvoke(reward, "com/bslx/XueXiMod/shu/technique/enchant/TideMarkSuccess", null) == 0
                        && countInvoke(reward, "com/bslx/XueXiMod/status/WetnessState", "grant") == 0,
                "额外奖励没有同时排除溺水、只认玩家攻击或箭，或它改写了潮印");
        MethodNode onDamage = method(events, "onDamage", null);
        check(damageOrder(onDamage), "onDamage 没有先观察潮印，再在取消、非正伤害、客户端和抑制之后只奖励一次");
        MethodNode enter = method(heavy, "enter", null);
        check(enter != null && countInvoke(heavy, "reserve") == 0 && countInvoke(heavy, "commit") == 0
                        && countInvoke(heavy, "release") >= 1
                        && countOpcode(enter, Opcodes.FCONST_0) >= 1 && countOpcode(enter, Opcodes.ICONST_0) >= 1,
                "HeavyWater.enter 仍在预留或提交，或新会话不是 0 金额且非重水");
        check(emptyBodies(pressure), "WetPressure 新方法体不是立即返回");
        MethodNode shooter = method(arrow, "shooterProfile", null);
        MethodNode consider = method(arrow, "consider", null);
        MethodNode attempt = method(arrow, "attempt", null);
        boolean profileField = false;
        for (FieldNode field : arrow.fields) {
            if (field.desc.contains("WetnessTiers$Profile")) {
                profileField = true;
            }
        }
        check(shooter != null && countInvoke(shooter, "getOwner") == 1
                        && countInvoke(shooter, "com/bslx/XueXiMod/status/WetnessTiers", "of") == 2
                        && countInvoke(shooter, "isAlive") == 0,
                "接触读主人没有走 WetnessTiers.of，或看了 isAlive");
        check(consider != null && countInvoke(consider, "shooterProfile") == 1
                        && countInvoke(consider, "com/bslx/XueXiMod/shu/technique/release/deflection/projectile/ArrowBook", "remaining") >= 1
                        && countInvoke(consider, "credit") == 0 && !profileField,
                "consider 没有在接触路径读当前主人和冻结余伤");
        check(attempt != null && countInvoke(attempt, "applyCommit") == 1
                        && countInvoke(attempt, "residualDirect") == 0 && countInvoke(attempt, "cost") == 0,
                "提交落账没有把 quote 交给 ArrowBook.applyCommit，或在调用前改用了费用");
        MethodNode arm = method(melee, "arm", null);
        check(arm != null && countInvoke(arm, "com/bslx/XueXiMod/status/WetnessTiers", "of") == 1 && loadsAttacker(arm),
                "近战没有只对攻击者读取一次潮湿");
        MethodNode flush = method(tally, "flush", null);
        int recordHurt = 0;
        for (MethodNode method : tally.methods) {
            if ("record".equals(method.name)) {
                recordHurt += countInvoke(method, "hurt") + countInvoke(method, "hurtRoot");
            }
        }
        check(flush != null && countInvoke(flush, "hurtRoot") == 1
                        && countInvoke(flush, "com/bslx/XueXiMod/shu/technique/release/momentum/WetAttackReward", null) == 0
                        && recordHurt == 0,
                "Tally.flush 不是恰好一次 hurtRoot，或 record 自己又打了一次");
        MethodNode hurtRoot = method(hits, "hurtRoot", "(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/server/level/ServerPlayer;FLcom/bslx/XueXiMod/shu/technique/enchant/TideMarkSuccess;)Z");
        MethodNode hurt = method(hits, "hurt", "(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/server/level/ServerPlayer;FLcom/bslx/XueXiMod/shu/technique/release/TechniqueHits$Landing;Lcom/bslx/XueXiMod/shu/technique/enchant/TideMarkSuccess;)Z");
        check(hurtRoot != null && countInvoke(hurtRoot, "com/bslx/XueXiMod/shu/technique/release/TechniqueHits", "hurt") == 1,
                "hurtRoot 没有转入一次 hurt");
        check(hurt != null && countInvoke(hurt, "playerAttack") == 2 && countInvoke(hurt, LIVING, "hurt") == 2,
                "术式 hurt 的两支落地分支没有各调用一次 playerAttack 和 LivingEntity.hurt");
        gap("奖励、Tally、箭接触、近战 arm、呼吸事件、下降和怕水都只核对了生产字节码，没有实体执行。");
    }

    private boolean damageOrder(MethodNode method) {
        if (method == null || method.instructions == null) {
            return false;
        }
        int observeAt = -1;
        int matchesAt = -1;
        int grantAt = -1;
        int grantCount = 0;
        int canceledAt = -1;
        int amountAt = -1;
        int clientAt = -1;
        int suppressAt = -1;
        boolean observeFirst = false;
        boolean seenInvoke = false;
        int cursor = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode call)) {
                cursor++;
                continue;
            }
            if (!seenInvoke) {
                observeFirst = "observe".equals(call.name)
                        && "com/bslx/XueXiMod/shu/technique/enchant/TideMarkDamageScope".equals(call.owner);
                seenInvoke = true;
            }
            if (matchesAt < 0 && "matches".equals(call.name)
                    && "com/bslx/XueXiMod/shu/technique/release/prison/PrisonPressure".equals(call.owner)) {
                matchesAt = cursor;
            }
            if ("observe".equals(call.name) && observeAt < 0) {
                observeAt = cursor;
            }
            if ("grant".equals(call.name) && "com/bslx/XueXiMod/shu/technique/release/momentum/WetAttackReward".equals(call.owner)) {
                grantCount++;
                grantAt = cursor;
            }
            if ("isCanceled".equals(call.name) && canceledAt < 0) {
                canceledAt = cursor;
            }
            if ("getAmount".equals(call.name) && amountAt < 0) {
                amountAt = cursor;
            }
            if ("isClientSide".equals(call.name) && clientAt < 0) {
                clientAt = cursor;
            }
            if ("suppresses".equals(call.name) && suppressAt < 0) {
                suppressAt = cursor;
            }
            cursor++;
        }
        boolean observeBeforeGates = observeAt >= 0 && canceledAt > observeAt && amountAt > observeAt
                && clientAt > observeAt && suppressAt > observeAt;
        boolean pressureBeforeObserve = matchesAt >= 0 && observeAt > matchesAt;
        boolean prefixAccepted = observeFirst || pressureBeforeObserve;
        return prefixAccepted && observeBeforeGates && grantCount == 1 && observeAt >= 0 && canceledAt >= 0
                && amountAt >= 0 && clientAt >= 0 && suppressAt >= 0 && canceledAt < grantAt && amountAt < grantAt
                && clientAt < grantAt && suppressAt < grantAt;
    }

    private boolean loadsAttacker(MethodNode method) {
        if (method.instructions == null) {
            return false;
        }
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode call) || !"of".equals(call.name)
                    || !"com/bslx/XueXiMod/status/WetnessTiers".equals(call.owner)) {
                continue;
            }
            AbstractInsnNode previous = previousReal(insn);
            if (previous instanceof org.objectweb.asm.tree.TypeInsnNode) {
                previous = previousReal(previous);
            }
            return previous instanceof VarInsnNode variable && variable.getOpcode() == Opcodes.ALOAD && variable.var == 0;
        }
        return false;
    }

    private boolean emptyBodies(ClassNode node) {
        int bodies = 0;
        for (MethodNode method : node.methods) {
            if (method.name.startsWith("<")) {
                continue;
            }
            bodies++;
            if (countMeaningful(method) != 1 || countOpcode(method, Opcodes.RETURN) != 1) {
                return false;
            }
        }
        return bodies == 5;
    }

    private boolean invokeOrder(MethodNode method, String first, String second, String third) {
        int firstAt = -1;
        int secondAt = -1;
        int thirdAt = -1;
        int cursor = 0;
        if (method == null || method.instructions == null) {
            return false;
        }
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call) {
                if (first.equals(call.name) && firstAt < 0) {
                    firstAt = cursor;
                }
                if (second.equals(call.name) && secondAt < 0) {
                    secondAt = cursor;
                }
                if (third.equals(call.name) && thirdAt < 0) {
                    thirdAt = cursor;
                }
            }
            cursor++;
        }
        return firstAt >= 0 && secondAt > firstAt && thirdAt > secondAt;
    }

    private static int countMeaningful(MethodNode method) {
        if (method.instructions == null) {
            return 0;
        }
        int found = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() >= 0) {
                found++;
            }
        }
        return found;
    }

    private static boolean containsString(ClassNode node, String text) {
        for (MethodNode method : node.methods) {
            if (method.instructions == null) {
                continue;
            }
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof LdcInsnNode ldc && text.equals(ldc.cst)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsInt(MethodNode method, int expected) {
        if (method.instructions == null) {
            return false;
        }
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof IntInsnNode value && value.operand == expected) {
                return true;
            }
            if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Integer number && number == expected) {
                return true;
            }
        }
        return false;
    }

    private static int countInvoke(ClassNode node, String name) {
        int found = 0;
        for (MethodNode method : node.methods) {
            found += countInvoke(method, name);
        }
        return found;
    }

    private static int countInvoke(ClassNode node, String owner, String name) {
        int found = 0;
        for (MethodNode method : node.methods) {
            found += countInvoke(method, owner, name);
        }
        return found;
    }

    private static int countInvoke(MethodNode method, String name) {
        return countInvoke(method, null, name);
    }

    private static int countInvoke(MethodNode method, String owner, String name) {
        if (method == null || method.instructions == null) {
            return 0;
        }
        int found = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode call)) {
                continue;
            }
            if (owner != null && !owner.equals(call.owner)) {
                continue;
            }
            if (name == null || name.equals(call.name)) {
                found++;
            }
        }
        return found;
    }

    private static int countField(MethodNode method, String name) {
        if (method == null || method.instructions == null) {
            return 0;
        }
        int found = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof org.objectweb.asm.tree.FieldInsnNode field && name.equals(field.name)) {
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

    private static int countOpcode(ClassNode node, int opcode) {
        int found = 0;
        for (MethodNode method : node.methods) {
            found += countOpcode(method, opcode);
        }
        return found;
    }

    private static AbstractInsnNode previousReal(AbstractInsnNode insn) {
        AbstractInsnNode previous = insn.getPrevious();
        while (previous != null && previous.getOpcode() < 0) {
            previous = previous.getPrevious();
        }
        return previous;
    }

    private static boolean containsMixin(String text) {
        return text != null && (text.contains(LIVING_MIXIN) || text.contains(FLYING_MIXIN));
    }

    private static boolean containsAny(List<String> names) {
        if (names == null) {
            return false;
        }
        for (String name : names) {
            if (containsMixin(name)) {
                return true;
            }
        }
        return false;
    }

    private static AnnotationNode injectMarker(MethodNode method, String methodName, String atValue) {
        AnnotationNode marker = findAnnotation(method.visibleAnnotations, "Lorg/spongepowered/asm/mixin/injection/Inject;");
        if (marker == null) {
            marker = findAnnotation(method.invisibleAnnotations, "Lorg/spongepowered/asm/mixin/injection/Inject;");
        }
        if (marker == null || !annotationText(marker, "method").contains(methodName)) {
            return null;
        }
        if (!atMatches(annotationRaw(marker, "at"), atValue)) {
            return null;
        }
        return marker;
    }

    private static boolean atMatches(Object at, String atValue) {
        if (at instanceof AnnotationNode atNode) {
            return atValue.equals(annotationText(atNode, "value"));
        }
        if (at instanceof List<?> items) {
            for (Object item : items) {
                if (atMatches(item, atValue)) {
                    return true;
                }
            }
        }
        return false;
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

    private static String jsonString(String body, String key) {
        String needle = "\"" + key + "\"";
        int at = body.indexOf(needle);
        if (at < 0) {
            return null;
        }
        int colon = body.indexOf(':', at + needle.length());
        int openQuote = body.indexOf('"', colon + 1);
        int closeQuote = body.indexOf('"', openQuote + 1);
        if (colon < 0 || openQuote < 0 || closeQuote < 0) {
            return null;
        }
        return body.substring(openQuote + 1, closeQuote);
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

    private ClassNode classFromJar(String internalName) throws IOException {
        if (!Files.isRegularFile(recompJar)) {
            check(false, "找不到映射 recomp " + recompJar);
            return null;
        }
        try (JarFile jar = new JarFile(recompJar.toFile())) {
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
