import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 核对末影龙、凋灵的 WETNESS 父类桥，以及三条龙助手在读写表之前返回客户端。
 * 不调用 Mixin 服务，不跑实体计时。
 */
public final class WetnessBossGate {
    private static final String EFFECT_DESC =
            "(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z";
    private static final String INIT_DESC =
            "(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V";
    private static final String DRAGON = "net/minecraft/world/entity/boss/enderdragon/EnderDragon";
    private static final String WITHER = "net/minecraft/world/entity/boss/wither/WitherBoss";
    private static final String MOB = "net/minecraft/world/entity/Mob";
    private static final String MONSTER = "net/minecraft/world/entity/monster/Monster";
    private static final String DRAGON_MIXIN = "com/bslx/XueXiMod/mixin/WetnessDragonMixin";
    private static final String WITHER_MIXIN = "com/bslx/XueXiMod/mixin/WetnessWitherMixin";
    private static final String EFFECTS = "com/bslx/XueXiMod/registry/ModMobEffects";
    private static final String FLIGHT = "com/bslx/XueXiMod/status/WetnessFlight";

    private final Path recompJar;
    private final Path classesRoot;
    private final Path mixinsJson;
    private final Path refmap;
    private final Path beforeFlight;
    private final Path currentFlight;
    private final Path beforeDragon;
    private final Path currentDragon;
    private int checks;
    private int failures;

    private WetnessBossGate(
            Path recompJar,
            Path classesRoot,
            Path mixinsJson,
            Path refmap,
            Path beforeFlight,
            Path currentFlight,
            Path beforeDragon,
            Path currentDragon) {
        this.recompJar = recompJar;
        this.classesRoot = classesRoot;
        this.mixinsJson = mixinsJson;
        this.refmap = refmap;
        this.beforeFlight = beforeFlight;
        this.currentFlight = currentFlight;
        this.beforeDragon = beforeDragon;
        this.currentDragon = currentDragon;
    }

    public static void main(String[] arguments) throws Exception {
        Path project = Path.of("D:/alianximod");
        Path before = Path.of("C:/Users/baishang/AppData/Local/CursorAgentCodex/wetness-tiers-implementation-20261026/before-boss-wetness");
        WetnessBossGate gate = new WetnessBossGate(
                Path.of("C:/Users/baishang/.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.20.1-47.3.33_mapped_parchment_2023.06.26-1.20.1/forge-1.20.1-47.3.33_mapped_parchment_2023.06.26-1.20.1-recomp.jar"),
                project.resolve("build/classes/java/main"),
                project.resolve("src/main/resources/xueximod.mixins.json"),
                project.resolve("build/tmp/compileJava/compileJava-refmap.json"),
                before.resolve("src/main/java/com/bslx/XueXiMod/status/WetnessFlight.java"),
                project.resolve("src/main/java/com/bslx/XueXiMod/status/WetnessFlight.java"),
                before.resolve("src/main/java/com/bslx/XueXiMod/mixin/WetnessDragonMixin.java"),
                project.resolve("src/main/java/com/bslx/XueXiMod/mixin/WetnessDragonMixin.java"));
        gate.run();
        System.out.println("断言 " + gate.checks + " 失败 " + gate.failures);
        System.out.println("启动证明 否");
        System.out.println("实体计时 未运行");
        System.out.println("完整变换 未验证");
        if (gate.failures != 0) {
            System.exit(1);
        }
    }

    private void run() throws Exception {
        ClassNode dragon = classFromJar(DRAGON);
        ClassNode wither = classFromJar(WITHER);
        checkVanilla(dragon, "EnderDragon");
        checkVanilla(wither, "WitherBoss");
        ClassNode dragonMixin = classFromClasses(DRAGON_MIXIN);
        ClassNode witherMixin = classFromClasses(WITHER_MIXIN);
        check(dragonMixin != null && witherMixin != null, "编译产物缺少首领 Mixin");
        if (dragonMixin != null) {
            checkBridge(dragonMixin, MOB, DRAGON, "末影龙");
        }
        if (witherMixin != null) {
            checkBridge(witherMixin, MONSTER, WITHER, "凋灵");
        }
        checkJson();
        checkRefmap();
        ClassNode flight = classFromClasses(FLIGHT);
        if (flight == null) {
            check(false, "缺少 WetnessFlight");
        } else {
            checkGuard(method(flight, "beginDragonStep", null), false);
            checkGuard(method(flight, "adjustDragonMove", null), true);
            checkGuard(method(flight, "correctDragonDescent", null), false);
        }
        checkGeometry();
        gap("没有构造末影龙或凋灵，没有跑效果计时、移速修饰或清除。");
        gap("没有调用 Mixin Transformer。父类 invokespecial 存在不等于正在运行的类已经换过 addEffect。");
    }

    private void checkVanilla(ClassNode node, String label) {
        check(node != null, "映射 jar 缺少 " + label);
        if (node == null) {
            return;
        }
        MethodNode effect = method(node, "addEffect", EFFECT_DESC);
        check(effect != null, label + " 没有两参数 addEffect" + EFFECT_DESC);
        if (effect == null) {
            return;
        }
        List<Integer> opcodes = realOpcodes(effect);
        boolean onlyFalse = !opcodes.isEmpty();
        boolean sawZero = false;
        boolean sawReturn = false;
        for (int opcode : opcodes) {
            if (opcode == Opcodes.ICONST_0) {
                sawZero = true;
            } else if (opcode == Opcodes.IRETURN) {
                sawReturn = true;
            } else {
                onlyFalse = false;
            }
        }
        check(onlyFalse && sawZero && sawReturn, label + " 两参数 addEffect 不是直接 false，实际 " + opcodes);
        check(countInvoke(effect, "addEffect") == 0, label + " 自己的 addEffect 仍调用了 addEffect");
    }

    private void checkBridge(ClassNode node, String superclass, String target, String label) {
        check(superclass.equals(node.superName), label + " Mixin 父类不是 " + superclass + " 实际 " + node.superName);
        String mixinTarget = mixinOwner(node);
        check(mixinTarget != null && mixinTarget.contains(target), label + " @Mixin 目标不是 " + target + " 实际 " + mixinTarget);
        MethodNode constructor = method(node, "<init>", INIT_DESC);
        check(constructor != null, label + " 没有 (EntityType, Level) 构造");
        if (constructor != null) {
            boolean visible = (constructor.access & Opcodes.ACC_PROTECTED) != 0
                    && (constructor.access & Opcodes.ACC_STATIC) == 0
                    && (constructor.access & Opcodes.ACC_PUBLIC) == 0;
            check(visible, label + " 构造不是 protected 实例方法，access " + constructor.access);
            int supers = countInvokeOwner(constructor, Opcodes.INVOKESPECIAL, superclass, "<init>", INIT_DESC);
            check(supers == 1, label + " 构造没有正好一次 invokespecial " + superclass + ".<init>，实际 " + supers);
        }
        check(countPublicStatic(node) == 0, label + " Mixin 有公开静态方法");
        MethodNode handler = effectHandler(node);
        check(handler != null, label + " 没有两参数 addEffect 的 cancellable HEAD 注入");
        if (handler == null) {
            return;
        }
        boolean instancePrivate = (handler.access & Opcodes.ACC_PRIVATE) != 0
                && (handler.access & Opcodes.ACC_STATIC) == 0;
        check(instancePrivate, label + " 效果注入不是 private 实例方法");
        check(handler.desc.contains("MobEffectInstance;") && handler.desc.contains("Entity;")
                        && handler.desc.contains("CallbackInfoReturnable;"),
                label + " 注入描述符不是两参数加 CallbackInfoReturnable，实际 " + handler.desc);
        int specials = countInvokeOwner(handler, Opcodes.INVOKESPECIAL, superclass, "addEffect", EFFECT_DESC);
        int virtuals = countInvokeOpcode(handler, Opcodes.INVOKEVIRTUAL, "addEffect")
                + countInvokeOpcode(handler, Opcodes.INVOKEINTERFACE, "addEffect");
        int ownSpecial = countInvokeOwner(handler, Opcodes.INVOKESPECIAL, node.name, "addEffect", null);
        check(specials == 1 && virtuals == 0 && ownSpecial == 0,
                label + " 父类 invokespecial 为 " + specials + "，虚调用 " + virtuals + "，自身 special " + ownSpecial);
        check(countField(handler, EFFECTS, "WETNESS") == 1 && countInvoke(handler, "getEffect") == 1,
                label + " 没有用 getEffect 对比 ModMobEffects.WETNESS");
        checkWetnessBranch(handler, label);
    }

    private void checkWetnessBranch(MethodNode handler, String label) {
        Flow flow = Flow.of(handler);
        int compare = flow.compare();
        int wetEntry = flow.wetEntry(compare);
        int setReturn = flow.invoke("setReturnValue");
        int parentCall = flow.specialAddEffect();
        check(compare >= 0 && wetEntry >= 0 && setReturn >= 0 && parentCall >= 0,
                label + " 缺少 WETNESS 比较、父类调用或 setReturnValue");
        if (compare < 0 || wetEntry < 0 || setReturn < 0 || parentCall < 0) {
            return;
        }
        check(flow.dominatorsOf(parentCall).get(wetEntry) && flow.dominatorsOf(setReturn).get(wetEntry),
                label + " 父类调用或 setReturnValue 不被 WETNESS 相等分支支配");
        boolean plainReturn = false;
        for (int cursor = 0; cursor < flow.size(); cursor++) {
            int opcode = flow.opcode(cursor);
            if (flow.reachable(cursor) && (opcode == Opcodes.RETURN || opcode == Opcodes.IRETURN)
                    && !flow.dominatorsOf(cursor).get(wetEntry)) {
                plainReturn = true;
            }
        }
        check(plainReturn, label + " 没有一条不取消的返回，非 WETNESS 也会 setReturnValue");
        check(flow.countName("cancel") == 0, label + " 注入另调用了 cancel");
    }

    private void checkGuard(MethodNode method, boolean nullMovementReturns) {
        check(method != null, "缺少龙助手方法");
        if (method == null) {
            return;
        }
        Flow flow = Flow.of(method);
        int clientCall = flow.invoke("isClientSide");
        int clientJump = flow.jumpFrom(clientCall);
        int clientTarget = flow.clientTarget(clientCall);
        int serverEntry = flow.serverEntry(clientCall);
        int nullJump = flow.nullJumpBefore(clientCall);
        check(flow.countName("isClientSide") == 1 && clientCall >= 0 && clientJump >= 0
                        && clientTarget >= 0 && serverEntry >= 0 && nullJump >= 0,
                method.name + " 没有唯一的 null/isClientSide 分支，调用 " + clientCall
                        + " 空跳转 " + nullJump + " 客户端 " + clientTarget + " 服务端入口 " + serverEntry);
        if (serverEntry < 0) {
            return;
        }
        List<Integer> mapUses = flow.mapUses();
        boolean dominated = !mapUses.isEmpty();
        for (int site : mapUses) {
            if (!flow.reachable(site) || !flow.dominatorsOf(site).get(serverEntry)) {
                dominated = false;
            }
        }
        check(dominated, method.name + " 的表读写没有全部被客户端提前返回之后的入口支配，位置 " + mapUses);
        Set<Integer> clientReachable = flow.reachableFrom(clientTarget, -1);
        Set<Integer> nullReachable = flow.reachableFrom(flow.jumpTarget(nullJump), -1);
        boolean earlyTouches = false;
        for (int site : mapUses) {
            if (clientReachable.contains(site) || nullReachable.contains(site)) {
                earlyTouches = true;
            }
        }
        int descend = flow.invoke("addVertical");
        int reposition = flow.invoke("setPos");
        if ((descend >= 0 && (clientReachable.contains(descend) || nullReachable.contains(descend)))
                || (reposition >= 0 && (clientReachable.contains(reposition) || nullReachable.contains(reposition)))) {
            earlyTouches = true;
        }
        check(!earlyTouches, method.name + " 空对象或客户端分支仍能到达表或下移");
        if (nullMovementReturns) {
            check(flow.reachesReturnWithout("put", serverEntry),
                    method.name + " 没有一条服务端路径原样返回且不 put");
        }
    }

    private void checkGeometry() throws IOException {
        String beforeFlightText = Files.readString(beforeFlight, StandardCharsets.UTF_8);
        String currentFlightText = Files.readString(currentFlight, StandardCharsets.UTF_8);
        String[] flightMethods = {
                "public static double limitedVertical(int tier, double vertical)",
                "public static double extraDown(int tier, double vanillaVertical)",
                "public static Vec3 adjustVexMove(LivingEntity entity, MoverType type, Vec3 movement)",
                "private static void clampDescent(LivingEntity entity)",
                "private static boolean canAddExtra(LivingEntity entity)",
                "private static double clipDragonPart(EnderDragon dragon, EnderDragonPart part, double extra)",
                "private static void addVertical(LivingEntity dragon, double allowed)",
                "private static void addVertical(EnderDragonPart part, double allowed)"
        };
        for (String signature : flightMethods) {
            String beforeBody = methodText(beforeFlightText, signature);
            String currentBody = methodText(currentFlightText, signature);
            check(beforeBody != null && beforeBody.equals(currentBody), "飞行几何方法被改动 " + signature);
        }
        String beforeDragonText = Files.readString(beforeDragon, StandardCharsets.UTF_8);
        String currentDragonText = Files.readString(currentDragon, StandardCharsets.UTF_8);
        String[] dragonMethods = {
                "private void xuexiWetnessDragonBegin(CallbackInfo callback)",
                "private void xuexiWetnessDragonMove(Args invokeArguments)",
                "private AABB xuexiWetnessDragonWalls(AABB preparedHead)"
        };
        for (String signature : dragonMethods) {
            String beforeBody = methodText(beforeDragonText, signature);
            String currentBody = methodText(currentDragonText, signature);
            check(beforeBody != null && beforeBody.equals(currentBody), "龙飞行钩子被改动 " + signature);
        }
    }

    private void checkJson() throws IOException {
        String json = Files.readString(mixinsJson, StandardCharsets.UTF_8);
        check(json.contains("\"WetnessDragonMixin\"") && json.contains("\"WetnessWitherMixin\""),
                "xueximod.mixins.json 没有同时登记龙和凋灵");
    }

    private void checkRefmap() throws IOException {
        if (!Files.isRegularFile(refmap)) {
            check(false, "缺少 refmap " + refmap);
            return;
        }
        String json = Files.readString(refmap, StandardCharsets.UTF_8);
        requireBody(json, DRAGON_MIXIN, "addEffect", EFFECT_DESC);
        requireBody(json, WITHER_MIXIN, "addEffect", EFFECT_DESC);
    }

    private void requireBody(String json, String mixinName, String key, String descriptor) {
        int seen = 0;
        int matched = 0;
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
                if (body.contains(key) && body.contains(descriptor)) {
                    matched++;
                }
            }
            from = at + quoted.length();
        }
        check(seen >= 2 && matched >= 2, "refmap " + mixinName + " " + key + " 见到 " + seen + " 份，相符 " + matched);
    }

    private static MethodNode effectHandler(ClassNode node) {
        for (MethodNode method : node.methods) {
            AnnotationNode marker = findAnnotation(annotationsOf(method), "Lorg/spongepowered/asm/mixin/injection/Inject;");
            if (marker == null) {
                continue;
            }
            String target = annotationText(marker, "method");
            if (!target.contains("addEffect") || !target.contains(EFFECT_DESC)) {
                continue;
            }
            if (!"HEAD".equals(atValue(marker)) || !annotationFlag(marker, "cancellable")) {
                continue;
            }
            return method;
        }
        return null;
    }

    private static int countPublicStatic(ClassNode node) {
        int found = 0;
        for (MethodNode method : node.methods) {
            if (method.name.startsWith("<")) {
                continue;
            }
            if ((method.access & Opcodes.ACC_PUBLIC) != 0 && (method.access & Opcodes.ACC_STATIC) != 0) {
                found++;
            }
        }
        return found;
    }

    private static List<Integer> realOpcodes(MethodNode method) {
        List<Integer> opcodes = new ArrayList<>();
        if (method == null || method.instructions == null) {
            return opcodes;
        }
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() >= 0) {
                opcodes.add(insn.getOpcode());
            }
        }
        return opcodes;
    }

    private static String methodText(String source, String signature) {
        int at = source.indexOf(signature);
        if (at < 0) {
            return null;
        }
        int brace = source.indexOf('{', at);
        if (brace < 0) {
            return null;
        }
        int depth = 0;
        for (int cursor = brace; cursor < source.length(); cursor++) {
            char current = source.charAt(cursor);
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(at, cursor + 1);
                }
            }
        }
        return null;
    }

    private static String mixinOwner(ClassNode node) {
        String visible = annotationType(node.visibleAnnotations);
        return visible != null ? visible : annotationType(node.invisibleAnnotations);
    }

    private static String annotationType(List<AnnotationNode> annotations) {
        AnnotationNode marker = findAnnotation(annotations, "Lorg/spongepowered/asm/mixin/Mixin;");
        if (marker == null) {
            return null;
        }
        Object value = annotationRaw(marker, "value");
        if (value instanceof org.objectweb.asm.Type type) {
            return type.getInternalName();
        }
        if (value instanceof List<?> items) {
            StringBuilder joined = new StringBuilder();
            for (Object item : items) {
                if (item instanceof org.objectweb.asm.Type type) {
                    joined.append(type.getInternalName()).append(' ');
                }
            }
            return joined.toString();
        }
        return value == null ? "" : String.valueOf(value);
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

    private static boolean annotationFlag(AnnotationNode annotation, String key) {
        Object value = annotationRaw(annotation, key);
        if (value instanceof Boolean flag) {
            return flag;
        }
        return value instanceof Integer number && number != 0;
    }

    private static String atValue(AnnotationNode marker) {
        Object at = annotationRaw(marker, "at");
        AnnotationNode node = null;
        if (at instanceof AnnotationNode found) {
            node = found;
        } else if (at instanceof List<?> items) {
            for (Object item : items) {
                if (item instanceof AnnotationNode found) {
                    node = found;
                    break;
                }
            }
        }
        return node == null ? "" : annotationText(node, "value");
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
        return countInvokeOpcode(method, -1, name);
    }

    private static int countInvokeOpcode(MethodNode method, int opcode, String name) {
        if (method == null || method.instructions == null) {
            return 0;
        }
        int found = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call && name.equals(call.name) && (opcode < 0 || call.getOpcode() == opcode)) {
                found++;
            }
        }
        return found;
    }

    private static int countInvokeOwner(MethodNode method, int opcode, String owner, String name, String descriptor) {
        if (method == null || method.instructions == null) {
            return 0;
        }
        int found = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call && call.getOpcode() == opcode && owner.equals(call.owner)
                    && name.equals(call.name) && (descriptor == null || descriptor.equals(call.desc))) {
                found++;
            }
        }
        return found;
    }

    private static int countField(MethodNode method, String owner, String name) {
        if (method == null || method.instructions == null) {
            return 0;
        }
        int found = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof FieldInsnNode field && owner.equals(field.owner) && name.equals(field.name)) {
                found++;
            }
        }
        return found;
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
        Path file = classesRoot.resolve(internalName + ".class");
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

    private static final class Flow {
        private final List<AbstractInsnNode> insns;
        private final Map<AbstractInsnNode, Integer> index = new HashMap<>();
        private final List<List<Integer>> successors = new ArrayList<>();
        private final List<List<Integer>> predecessors = new ArrayList<>();
        private final BitSet reachable = new BitSet();

        private Flow(List<AbstractInsnNode> insns) {
            this.insns = insns;
        }

        static Flow of(MethodNode method) {
            List<AbstractInsnNode> insns = new ArrayList<>();
            if (method != null && method.instructions != null) {
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    insns.add(insn);
                }
            }
            Flow flow = new Flow(insns);
            for (int cursor = 0; cursor < insns.size(); cursor++) {
                flow.index.put(insns.get(cursor), cursor);
                flow.successors.add(new ArrayList<>());
                flow.predecessors.add(new ArrayList<>());
            }
            for (int cursor = 0; cursor < insns.size(); cursor++) {
                AbstractInsnNode insn = insns.get(cursor);
                int opcode = insn.getOpcode();
                if (insn instanceof JumpInsnNode jump) {
                    flow.link(cursor, flow.index.get(jump.label));
                    if (opcode != Opcodes.GOTO) {
                        flow.link(cursor, cursor + 1);
                    }
                } else if (opcode != Opcodes.IRETURN && opcode != Opcodes.RETURN && opcode != Opcodes.ARETURN
                        && opcode != Opcodes.DRETURN && opcode != Opcodes.ATHROW) {
                    flow.link(cursor, cursor + 1);
                }
            }
            if (method != null && method.tryCatchBlocks != null) {
                for (TryCatchBlockNode handler : method.tryCatchBlocks) {
                    Integer target = flow.index.get(handler.handler);
                    if (target == null) {
                        continue;
                    }
                    Integer start = flow.index.get(handler.start);
                    Integer end = flow.index.get(handler.end);
                    if (start == null || end == null) {
                        continue;
                    }
                    for (int cursor = start; cursor < end; cursor++) {
                        flow.link(cursor, target);
                    }
                }
            }
            flow.markReachable();
            return flow;
        }

        int size() {
            return insns.size();
        }

        int opcode(int cursor) {
            return cursor >= 0 && cursor < insns.size() ? insns.get(cursor).getOpcode() : -1;
        }

        boolean reachable(int cursor) {
            return cursor >= 0 && reachable.get(cursor);
        }

        int invoke(String name) {
            for (int cursor = 0; cursor < insns.size(); cursor++) {
                AbstractInsnNode insn = insns.get(cursor);
                if (insn instanceof MethodInsnNode call && name.equals(call.name)) {
                    return cursor;
                }
            }
            return -1;
        }

        int countName(String name) {
            int found = 0;
            for (AbstractInsnNode insn : insns) {
                if (insn instanceof MethodInsnNode call && name.equals(call.name)) {
                    found++;
                }
            }
            return found;
        }

        int compare() {
            for (int cursor = 0; cursor < insns.size(); cursor++) {
                int opcode = insns.get(cursor).getOpcode();
                if (opcode == Opcodes.IF_ACMPEQ || opcode == Opcodes.IF_ACMPNE) {
                    return cursor;
                }
            }
            return -1;
        }

        int wetEntry(int compare) {
            if (!(compare >= 0 && insns.get(compare) instanceof JumpInsnNode jump)) {
                return -1;
            }
            int target = index.get(jump.label);
            if (jump.getOpcode() == Opcodes.IF_ACMPNE) {
                return compare + 1;
            }
            if (jump.getOpcode() == Opcodes.IF_ACMPEQ) {
                return target;
            }
            return -1;
        }

        int specialAddEffect() {
            for (int cursor = 0; cursor < insns.size(); cursor++) {
                AbstractInsnNode insn = insns.get(cursor);
                if (insn instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKESPECIAL
                        && "addEffect".equals(call.name) && EFFECT_DESC.equals(call.desc)) {
                    return cursor;
                }
            }
            return -1;
        }

        int jumpFrom(int call) {
            if (call < 0) {
                return -1;
            }
            for (int cursor = call + 1; cursor < insns.size(); cursor++) {
                if (insns.get(cursor) instanceof JumpInsnNode) {
                    return cursor;
                }
                if (insns.get(cursor).getOpcode() >= 0) {
                    return -1;
                }
            }
            return -1;
        }

        int jumpTarget(int jump) {
            if (!(jump >= 0 && insns.get(jump) instanceof JumpInsnNode jumpInsn)) {
                return -1;
            }
            Integer target = index.get(jumpInsn.label);
            return target == null ? -1 : target;
        }

        int clientTarget(int call) {
            int jump = jumpFrom(call);
            if (!(jump >= 0 && insns.get(jump) instanceof JumpInsnNode jumpInsn)) {
                return -1;
            }
            if (jumpInsn.getOpcode() == Opcodes.IFNE) {
                return jumpTarget(jump);
            }
            if (jumpInsn.getOpcode() == Opcodes.IFEQ) {
                return jump + 1;
            }
            return -1;
        }

        int nullJumpBefore(int call) {
            int found = -1;
            for (int cursor = 0; cursor < call && cursor < insns.size(); cursor++) {
                if (insns.get(cursor).getOpcode() == Opcodes.IFNULL) {
                    found = cursor;
                }
            }
            return found;
        }

        int serverEntry(int call) {
            int jump = jumpFrom(call);
            if (!(jump >= 0 && insns.get(jump) instanceof JumpInsnNode jumpInsn)) {
                return -1;
            }
            int opcode = jumpInsn.getOpcode();
            if (opcode == Opcodes.IFNE) {
                return jump + 1;
            }
            if (opcode == Opcodes.IFEQ) {
                Integer target = index.get(jumpInsn.label);
                return target == null ? -1 : target;
            }
            return -1;
        }

        List<Integer> mapUses() {
            List<Integer> sites = new ArrayList<>();
            for (int cursor = 0; cursor < insns.size(); cursor++) {
                AbstractInsnNode insn = insns.get(cursor);
                if (insn instanceof FieldInsnNode field && "DRAGON_EXTRA".equals(field.name)) {
                    sites.add(cursor);
                }
                if (insn instanceof MethodInsnNode call && "java/util/WeakHashMap".equals(call.owner)
                        && ("get".equals(call.name) || "put".equals(call.name) || "remove".equals(call.name))) {
                    sites.add(cursor);
                }
            }
            return sites;
        }

        boolean reachesReturnWithout(String avoided, int start) {
            if (start < 0 || start >= insns.size()) {
                return false;
            }
            boolean[] seen = new boolean[insns.size()];
            List<Integer> pending = new ArrayList<>();
            pending.add(start);
            seen[start] = true;
            while (!pending.isEmpty()) {
                int cursor = pending.remove(pending.size() - 1);
                int opcode = insns.get(cursor).getOpcode();
                if (opcode == Opcodes.ARETURN || opcode == Opcodes.RETURN) {
                    return true;
                }
                AbstractInsnNode insn = insns.get(cursor);
                if (insn instanceof MethodInsnNode call && avoided.equals(call.name)) {
                    continue;
                }
                for (int next : successors.get(cursor)) {
                    if (!seen[next]) {
                        seen[next] = true;
                        pending.add(next);
                    }
                }
            }
            return false;
        }

        Set<Integer> reachableFrom(int start, int blocked) {
            Set<Integer> seen = new HashSet<>();
            if (start < 0 || start >= insns.size()) {
                return seen;
            }
            List<Integer> pending = new ArrayList<>();
            pending.add(start);
            seen.add(start);
            while (!pending.isEmpty()) {
                int cursor = pending.remove(pending.size() - 1);
                for (int next : successors.get(cursor)) {
                    if (next == blocked || seen.contains(next)) {
                        continue;
                    }
                    seen.add(next);
                    pending.add(next);
                }
            }
            return seen;
        }

        BitSet dominatorsOf(int required) {
            int count = insns.size();
            BitSet[] dominator = new BitSet[count];
            if (count == 0 || required < 0) {
                return new BitSet();
            }
            dominator[0] = new BitSet();
            dominator[0].set(0);
            BitSet everything = new BitSet();
            everything.set(0, count);
            for (int cursor = 1; cursor < count; cursor++) {
                dominator[cursor] = (BitSet) everything.clone();
            }
            boolean changed = true;
            while (changed) {
                changed = false;
                for (int cursor = 1; cursor < count; cursor++) {
                    if (!reachable.get(cursor)) {
                        continue;
                    }
                    BitSet next = null;
                    for (int previous : predecessors.get(cursor)) {
                        if (!reachable.get(previous)) {
                            continue;
                        }
                        if (next == null) {
                            next = (BitSet) dominator[previous].clone();
                        } else {
                            next.and(dominator[previous]);
                        }
                    }
                    if (next == null) {
                        continue;
                    }
                    next.set(cursor);
                    if (!next.equals(dominator[cursor])) {
                        dominator[cursor] = next;
                        changed = true;
                    }
                }
            }
            return required < count ? dominator[required] : new BitSet();
        }

        private void markReachable() {
            if (insns.isEmpty()) {
                return;
            }
            List<Integer> pending = new ArrayList<>();
            pending.add(0);
            reachable.set(0);
            while (!pending.isEmpty()) {
                int cursor = pending.remove(pending.size() - 1);
                for (int next : successors.get(cursor)) {
                    if (!reachable.get(next)) {
                        reachable.set(next);
                        pending.add(next);
                    }
                }
            }
        }

        private void link(int from, int to) {
            if (from < 0 || to < 0 || to >= insns.size() || successors.get(from).contains(to)) {
                return;
            }
            successors.get(from).add(to);
            predecessors.get(to).add(from);
        }
    }
}
