import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 读 Mixin 0.8.5 的可见性字节码、负对照 class、修后 class、映射 recomp 和 refmap。
 * 不启动游戏，也不伪造转换器。
 */
public final class StartupMixinGate {
    private static final String NEGATIVE_MIXIN_SHA = "96BA2B4C0AD76CFE4D215EF2D9E62BF77E58E586F69B2759ACBA5967E603C939";
    private static final String NEGATIVE_BOOK_SHA = "21594F6F5919BA3B01DC4AD9819D87728090C1BF2AD1ED7EEDF5B9AE9FAAF5C7";
    private static final String MIXIN_PREFIX = "com/bslx/XueXiMod/mixin/";
    private static final String DEFINE_OWNER = "net/minecraft/network/syncher/SynchedEntityData";
    private static final String ENTITY_HURT = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z";
    private static final String LIVING_HURT = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z";
    private static final String FIND_HIT = "Lnet/minecraft/world/entity/projectile/AbstractArrow;findHitEntity(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/EntityHitResult;";
    private static final String NEXT_INT = "Lnet/minecraft/util/RandomSource;nextInt(I)I";
    private static final String FIRE_ASPECT = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getFireAspect(Lnet/minecraft/world/entity/LivingEntity;)I";
    private static final String HURT_DESC = "(Lnet/minecraft/world/damagesource/DamageSource;F)Z";
    private static final String ACCESSOR_ANNOTATION = "Lorg/spongepowered/asm/mixin/gen/Accessor;";
    private static final String INVOKER_ANNOTATION = "Lorg/spongepowered/asm/mixin/gen/Invoker;";
    private static final String UNIQUE_ANNOTATION = "Lorg/spongepowered/asm/mixin/Unique;";
    private static final String OVERWRITE_ANNOTATION = "Lorg/spongepowered/asm/mixin/Overwrite;";

    private final Path mixinJar;
    private final Path recompJar;
    private final Path classesRoot;
    private final Path mixinsJson;
    private final Path refmap;
    private final Path negativeMixin;
    private final Path negativeBook;
    private final Path manifest;
    private int checks;
    private int failures;
    private boolean ruleMatched;

    private StartupMixinGate(Path mixinJar, Path recompJar, Path classesRoot, Path mixinsJson, Path refmap,
                             Path negativeMixin, Path negativeBook, Path manifest) {
        this.mixinJar = mixinJar;
        this.recompJar = recompJar;
        this.classesRoot = classesRoot;
        this.mixinsJson = mixinsJson;
        this.refmap = refmap;
        this.negativeMixin = negativeMixin;
        this.negativeBook = negativeBook;
        this.manifest = manifest;
    }

    public static void main(String[] parameters) throws Exception {
        Path project = Path.of("D:/alianximod");
        Path task = Path.of("C:/Users/baishang/AppData/Local/CursorAgentCodex/water-deflection-startup-qa-20261026");
        Map<String, String> options = parse(parameters);
        StartupMixinGate gate = new StartupMixinGate(
                Path.of(options.getOrDefault("mixin-jar", "C:/Users/baishang/.gradle/caches/modules-2/files-2.1/org.spongepowered/mixin/0.8.5/9d1c0c3a304ae6697ecd477218fa61b850bf57fc/mixin-0.8.5.jar")),
                Path.of(options.getOrDefault("recomp-jar", "C:/Users/baishang/.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.20.1-47.3.33_mapped_parchment_2023.06.26-1.20.1/forge-1.20.1-47.3.33_mapped_parchment_2023.06.26-1.20.1-recomp.jar")),
                Path.of(options.getOrDefault("classes", project.resolve("build/classes/java/main").toString())),
                Path.of(options.getOrDefault("mixins-json", project.resolve("src/main/resources/xueximod.mixins.json").toString())),
                Path.of(options.getOrDefault("refmap", project.resolve("build/tmp/compileJava/compileJava-refmap.json").toString())),
                Path.of(options.getOrDefault("negative-mixin", task.resolve("startup-negative-fixtures/ArrowDeflectionMixin.class").toString())),
                Path.of(options.getOrDefault("negative-book", task.resolve("startup-negative-fixtures/ArrowCaptureBook.class").toString())),
                Path.of(options.getOrDefault("manifest", task.resolve("startup-negative-fixtures/manifest.json").toString())));
        gate.run();
        System.out.println("断言 " + gate.checks + " 失败 " + gate.failures);
        System.out.println("启动证明 否");
        if (gate.failures > 0) {
            System.exit(1);
        }
    }

    private void run() throws Exception {
        ruleMatched = confirmMixinRule();
        Loaded negativeMixinClass = loadFile(negativeMixin);
        Loaded negativeBookClass = loadFile(negativeBook);
        confirmNegativeHashes(negativeMixinClass.bytes, negativeBookClass.bytes);
        Map<String, Loaded> compiled = loadTree(classesRoot);
        if (compiled.isEmpty()) {
            fail("编译目录没有 class：" + classesRoot);
        }
        if (!ruleMatched) {
            fail("Mixin 0.8.5 可见性字节码未对上，停止把后续分类当成规则已核对");
        } else {
            auditNegativeMixin(negativeMixinClass.node);
            auditNegativeBook(negativeBookClass.node);
            auditCompiledMixins(compiled);
        }
        auditCaptureBridge(compiled);
        auditDefineId(compiled);
        auditConsiderShape(compiled);
        auditTargets();
        auditHurtAnnotations(compiled);
        auditRefmap();
        probeMixinService();
        gap("D 未覆盖：真实组合注入未验证，没有变换后的 AbstractArrow。没有箭实体、穿透、捕获同步、网络包，也没有 WaterMomentum 玩家账。MemoryPurse 或待定表通过不能当成真实玩家账。");
    }

    private boolean confirmMixinRule() throws IOException {
        if (!Files.isRegularFile(mixinJar)) {
            fail("找不到 Mixin jar " + mixinJar);
            return false;
        }
        ClassNode applicator = classFromJar(mixinJar, "org/spongepowered/asm/mixin/transformer/MixinApplicatorStandard.class");
        if (applicator == null) {
            fail("Mixin jar 里没有 MixinApplicatorStandard");
            return false;
        }
        MethodNode visibility = onlyMethod(applicator, "checkMethodVisibility");
        MethodNode normal = onlyMethod(applicator, "applyNormalMethod");
        boolean visibilityMatched = visibility != null && matchVisibility(visibility);
        boolean normalMatched = normal != null && matchApplyNormal(normal);
        if (visibilityMatched && normalMatched) {
            ok("MixinApplicatorStandard.checkMethodVisibility 仍是 static 且非 private、非 synthetic、无可见 Overwrite 才抛 non-private static method；名字以 < 开头的方法在 applyNormalMethod 里跳过");
            return true;
        }
        fail("本机 mixin-0.8.5 的可见性指令序列和已确认规则不一致");
        return false;
    }

    private boolean matchVisibility(MethodNode method) {
        int stage = 0;
        boolean sawFailureText = false;
        boolean sawException = false;
        boolean sawThrow = false;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (stage == 0 && intOperand(insn, Opcodes.BIPUSH, 8)) {
                stage = 1;
            } else if (stage == 1 && isCall(insn, Opcodes.INVOKESTATIC, "org/spongepowered/asm/util/Bytecode", "hasFlag")) {
                stage = 2;
            } else if (stage == 2 && insn.getOpcode() == Opcodes.IFEQ) {
                stage = 3;
            } else if (stage == 3 && insn.getOpcode() == Opcodes.ICONST_2) {
                stage = 4;
            } else if (stage == 4 && isCall(insn, Opcodes.INVOKESTATIC, "org/spongepowered/asm/util/Bytecode", "hasFlag")) {
                stage = 5;
            } else if (stage == 5 && insn.getOpcode() == Opcodes.IFNE) {
                stage = 6;
            } else if (stage == 6 && intOperand(insn, Opcodes.SIPUSH, 4096)) {
                stage = 7;
            } else if (stage == 7 && isCall(insn, Opcodes.INVOKESTATIC, "org/spongepowered/asm/util/Bytecode", "hasFlag")) {
                stage = 8;
            } else if (stage == 8 && insn.getOpcode() == Opcodes.IFNE) {
                stage = 9;
            } else if (stage == 9 && ldcType(insn, "org/spongepowered/asm/mixin/Overwrite")) {
                stage = 10;
            } else if (stage == 10 && isCall(insn, Opcodes.INVOKESTATIC, "org/spongepowered/asm/util/Annotations", "getVisible")) {
                stage = 11;
            } else if (stage == 11 && insn.getOpcode() == Opcodes.IFNONNULL) {
                stage = 12;
            }
            if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof String text && text.contains("non-private static method")) {
                sawFailureText = true;
            }
            if (insn instanceof TypeInsnNode created && created.getOpcode() == Opcodes.NEW
                    && created.desc.equals("org/spongepowered/asm/mixin/transformer/throwables/InvalidMixinException")) {
                sawException = true;
            }
            if (insn.getOpcode() == Opcodes.ATHROW) {
                sawThrow = true;
            }
        }
        return stage == 12 && sawFailureText && sawException && sawThrow;
    }

    private boolean matchApplyNormal(MethodNode method) {
        int stage = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (stage == 0 && insn instanceof LdcInsnNode ldc && "<".equals(ldc.cst)) {
                stage = 1;
            } else if (stage == 1 && isCall(insn, Opcodes.INVOKEVIRTUAL, "java/lang/String", "startsWith")) {
                stage = 2;
            } else if (stage == 2 && insn.getOpcode() == Opcodes.IFNE) {
                stage = 3;
            } else if (stage == 3 && isCall(insn, Opcodes.INVOKEVIRTUAL, null, "checkMethodVisibility")) {
                stage = 4;
            }
        }
        return stage == 4;
    }

    private void confirmNegativeHashes(byte[] mixinBytes, byte[] bookBytes) throws Exception {
        String mixinSha = sha256(mixinBytes);
        String bookSha = sha256(bookBytes);
        String manifestText = Files.readString(manifest, StandardCharsets.UTF_8);
        if (NEGATIVE_MIXIN_SHA.equals(mixinSha) && manifestText.toUpperCase().contains(mixinSha)) {
            ok("负对照 ArrowDeflectionMixin SHA256 " + mixinSha);
        } else {
            fail("负对照 ArrowDeflectionMixin SHA 与 manifest 不一致，实际 " + mixinSha);
        }
        if (NEGATIVE_BOOK_SHA.equals(bookSha) && manifestText.toUpperCase().contains(bookSha)) {
            ok("负对照 ArrowCaptureBook SHA256 " + bookSha);
        } else {
            fail("负对照 ArrowCaptureBook SHA 与 manifest 不一致，实际 " + bookSha);
        }
    }

    private void auditNegativeMixin(ClassNode node) {
        List<String> rejected = rejectedMethods(node);
        boolean sawSetHeld = rejected.contains("setHeld(Lnet/minecraft/world/entity/projectile/AbstractArrow;Z)V");
        boolean sawIsHeld = rejected.contains("isHeld(Lnet/minecraft/world/entity/projectile/AbstractArrow;)Z");
        if (sawSetHeld && sawIsHeld) {
            ok("负对照公开静态 setHeld 和 isHeld 被同一条 Mixin 规则拒绝");
        } else {
            fail("负对照没有同时拒绝 setHeld 和 isHeld，实际 " + rejected);
        }
    }

    private void auditNegativeBook(ClassNode node) {
        int sites = countCalls(node, Opcodes.INVOKESTATIC, "com/bslx/XueXiMod/mixin/ArrowDeflectionMixin",
                "setHeld", "(Lnet/minecraft/world/entity/projectile/AbstractArrow;Z)V");
        if (sites == 3) {
            ok("负对照 ArrowCaptureBook 有 3 处 invokestatic ArrowDeflectionMixin.setHeld");
        } else {
            fail("负对照 ArrowCaptureBook 的 setHeld invokestatic 数量是 " + sites);
        }
    }

    private void auditCompiledMixins(Map<String, Loaded> compiled) throws Exception {
        String json = Files.readString(mixinsJson, StandardCharsets.UTF_8);
        String packageName = jsonValue(json, "package");
        if (!"com.bslx.XueXiMod.mixin".equals(packageName)) {
            fail("mixins.json package 不是 com.bslx.XueXiMod.mixin");
            return;
        }
        List<String> listed = new ArrayList<>();
        listed.addAll(jsonArray(json, "mixins"));
        listed.addAll(jsonArray(json, "client"));
        Set<String> accessors = new HashSet<>();
        Set<String> implementations = new HashSet<>();
        List<String> rejected = new ArrayList<>();
        for (String relative : listed) {
            String internal = packageName.replace('.', '/') + "/" + relative.replace('.', '/');
            Loaded loaded = compiled.get(internal);
            if (loaded == null) {
                fail("编译产物缺少 " + internal);
                continue;
            }
            if (isAccessorInterface(loaded.node)) {
                accessors.add(internal);
                info("分类 " + internal + " ACCESSOR");
            } else {
                implementations.add(internal);
                info("分类 " + internal + " IMPLEMENTATION");
                for (String method : rejectedMethods(loaded.node)) {
                    rejected.add(internal + " " + method);
                }
            }
        }
        if (accessors.isEmpty()) {
            fail("没有识别出合法 Accessor 接口");
        } else if (!accessors.contains(MIXIN_PREFIX + "EntityRandomAccessor")) {
            fail("EntityRandomAccessor 没有被当成 Accessor");
        } else {
            ok("识别 Accessor 接口 " + accessors.size() + " 个，EntityRandomAccessor 在放行集合里");
        }
        if (rejected.isEmpty()) {
            ok("修后的 mixin 列表里没有会被 checkMethodVisibility 拒绝的普通公开静态方法");
        } else {
            fail("修后仍有公开静态合并方法 " + rejected);
        }
        Loaded fixedMixin = compiled.get(MIXIN_PREFIX + "ArrowDeflectionMixin");
        if (fixedMixin == null) {
            fail("修后缺少 ArrowDeflectionMixin");
            return;
        }
        String fixedSha = sha256(fixedMixin.bytes);
        if (NEGATIVE_MIXIN_SHA.equals(fixedSha)) {
            fail("修后 ArrowDeflectionMixin 与负对照 SHA 相同");
        } else {
            ok("修后 ArrowDeflectionMixin SHA256 " + fixedSha + "，与负对照不同");
        }
        List<String> outsideCalls = new ArrayList<>();
        int accessorUses = 0;
        for (Loaded loaded : compiled.values()) {
            boolean outside = !loaded.node.name.startsWith(MIXIN_PREFIX);
            for (MethodNode method : loaded.node.methods) {
                if (method.instructions == null) {
                    continue;
                }
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    String owner = referencedOwner(insn);
                    if (owner == null) {
                        continue;
                    }
                    if (matchesAny(owner, accessors)) {
                        accessorUses++;
                        continue;
                    }
                    if (outside && matchesAny(owner, implementations)) {
                        outsideCalls.add(loaded.node.name + "." + method.name + " -> " + insn.getOpcode() + " " + owner
                                + " " + referencedDetail(insn));
                    }
                }
            }
        }
        if (accessorUses > 0) {
            ok("Accessor 符号引用 " + accessorUses + " 处被放行，没有按 mixin 包名前缀整体禁止");
        } else {
            fail("编译类里没有 Accessor 符号引用，无法证明放行");
        }
        if (outsideCalls.isEmpty()) {
            ok("普通类不再 invokestatic 或用其它符号引用实现 Mixin");
        } else {
            fail("包外仍引用实现 Mixin " + outsideCalls);
        }
    }

    private void auditCaptureBridge(Map<String, Loaded> compiled) {
        Loaded mixin = compiled.get(MIXIN_PREFIX + "ArrowDeflectionMixin");
        Loaded book = compiled.get("com/bslx/XueXiMod/shu/technique/release/deflection/projectile/ArrowCaptureBook");
        if (mixin == null || book == null) {
            fail("缺少修后的 ArrowDeflectionMixin 或 ArrowCaptureBook");
            return;
        }
        String bridge = null;
        for (String interfaceName : mixin.node.interfaces) {
            if (!interfaceName.startsWith(MIXIN_PREFIX)) {
                bridge = interfaceName;
                break;
            }
        }
        if (bridge == null) {
            fail("ArrowDeflectionMixin 没有实现 mixin 包外的接口");
            return;
        }
        Loaded access = compiled.get(bridge);
        if (access == null || (access.node.access & Opcodes.ACC_INTERFACE) == 0
                || (access.node.access & Opcodes.ACC_PUBLIC) == 0) {
            fail("实例桥不是已编译的公开接口 " + bridge);
            return;
        }
        if (annotated(access.node.visibleAnnotations, "Lorg/spongepowered/asm/mixin/Mixin;")
                || annotated(access.node.invisibleAnnotations, "Lorg/spongepowered/asm/mixin/Mixin;")) {
            fail("实例桥自己带了 @Mixin " + bridge);
            return;
        }
        MethodNode setter = findDescribed(access.node, "(Z)V");
        MethodNode getter = findDescribed(access.node, "()Z");
        if (setter == null || getter == null || !setter.name.startsWith("xuexi") || !getter.name.startsWith("xuexi")) {
            fail("接口缺少 xuexi 前缀的 (Z)V 和 ()Z，实际接口 " + bridge);
            return;
        }
        MethodNode mixinSetter = findExact(mixin.node, setter.name, setter.desc);
        MethodNode mixinGetter = findExact(mixin.node, getter.name, getter.desc);
        if (!instanceBridge(mixinSetter) || !instanceBridge(mixinGetter)
                || !annotated(annotationsOf(mixinSetter), UNIQUE_ANNOTATION)
                || !annotated(annotationsOf(mixinGetter), UNIQUE_ANNOTATION)) {
            fail("Mixin 上的桥不是公开实例方法，或缺少 @Unique");
            return;
        }
        ok("Mixin 实现公开接口 " + bridge + " " + setter.name + "(Z)V / " + getter.name + "()Z，并且方法带 @Unique");
        int setterCalls = countCalls(book.node, Opcodes.INVOKEINTERFACE, bridge, setter.name, setter.desc);
        int staticLeftover = countCalls(book.node, Opcodes.INVOKESTATIC, MIXIN_PREFIX + "ArrowDeflectionMixin",
                "setHeld", "(Lnet/minecraft/world/entity/projectile/AbstractArrow;Z)V");
        if (setterCalls >= 3 && staticLeftover == 0) {
            ok("ArrowCaptureBook 用 invokeinterface " + setter.name + " " + setterCalls + " 处，不再 invokestatic 实现 Mixin");
        } else {
            fail("ArrowCaptureBook 接口调用 " + setterCalls + "，残留 invokestatic " + staticLeftover);
        }
        int internalSetter = countCalls(mixin.node, Opcodes.INVOKEVIRTUAL, mixin.node.name, setter.name, setter.desc);
        int internalGetter = countCalls(mixin.node, Opcodes.INVOKEVIRTUAL, mixin.node.name, getter.name, getter.desc);
        if (internalSetter >= 1 && internalGetter >= 1) {
            ok("Mixin 内部改走实例桥，setter 调用 " + internalSetter + "，getter 调用 " + internalGetter);
        } else {
            fail("Mixin 内部实例桥调用不足，setter " + internalSetter + " getter " + internalGetter);
        }
    }

    private void auditDefineId(Map<String, Loaded> compiled) {
        Loaded mixin = compiled.get(MIXIN_PREFIX + "ArrowDeflectionMixin");
        if (mixin == null) {
            fail("无法核对 defineId，缺少 ArrowDeflectionMixin");
            return;
        }
        List<String> sites = new ArrayList<>();
        for (Loaded loaded : compiled.values()) {
            for (MethodNode method : loaded.node.methods) {
                int found = countArrowDefineId(method);
                if (found > 0) {
                    sites.add(loaded.node.name + "." + method.name + " x" + found);
                }
            }
        }
        if (sites.size() == 1 && sites.get(0).equals(MIXIN_PREFIX + "ArrowDeflectionMixin.<clinit> x1")) {
            ok("AbstractArrow 的 defineId 只在 ArrowDeflectionMixin.<clinit> 出现一次");
        } else {
            fail("AbstractArrow 的 defineId 不是唯一 clinit：" + sites);
        }
        FieldNode accessor = null;
        int accessorFields = 0;
        for (FieldNode field : mixin.node.fields) {
            if ("Lnet/minecraft/network/syncher/EntityDataAccessor;".equals(field.desc)
                    && (field.access & Opcodes.ACC_STATIC) != 0) {
                accessorFields++;
                accessor = field;
            }
        }
        if (accessorFields == 1 && accessor != null && (accessor.access & Opcodes.ACC_PRIVATE) != 0
                && (accessor.access & Opcodes.ACC_PUBLIC) == 0) {
            ok("捕获 accessor 字段 " + accessor.name + " 是 private static");
        } else {
            fail("捕获 accessor 字段不是唯一的 private static，数量 " + accessorFields);
            return;
        }
        MethodNode clinit = findExact(mixin.node, "<clinit>", "()V");
        MethodNode defineMethod = methodCalling(mixin.node, Opcodes.INVOKEVIRTUAL, DEFINE_OWNER, "define");
        if (clinit == null || defineMethod == null) {
            fail("缺少 <clinit> 或 SynchedEntityData.define 调用");
            return;
        }
        int defineIdInDefine = countCalls(defineMethod, Opcodes.INVOKESTATIC, DEFINE_OWNER, "defineId", null);
        int defineInClinit = countCalls(clinit, Opcodes.INVOKEVIRTUAL, DEFINE_OWNER, "define", null);
        boolean putAfterDefine = putstaticAfterDefineId(clinit, mixin.node.name, accessor.name);
        if (defineIdInDefine == 0 && defineInClinit == 0 && putAfterDefine
                && !"<clinit>".equals(defineMethod.name)) {
            ok("defineSynchedData 路径只 define 同一个 accessor，没有第二份 defineId");
        } else {
            fail("define 复用核对失败 defineIdInDefine=" + defineIdInDefine + " defineInClinit=" + defineInClinit
                    + " putAfter=" + putAfterDefine + " method=" + defineMethod.name);
        }
    }

    private void auditConsiderShape(Map<String, Loaded> compiled) {
        Loaded flight = compiled.get("com/bslx/XueXiMod/shu/technique/release/deflection/projectile/ArrowFlight");
        if (flight == null) {
            fail("缺少 ArrowFlight");
            return;
        }
        MethodNode consider = onlyMethod(flight.node, "consider");
        if (consider == null) {
            return;
        }
        int zeroStores = 0;
        int pendingStores = 0;
        for (AbstractInsnNode insn = consider.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode call) || call.getOpcode() != Opcodes.INVOKESTATIC
                    || !"rememberPending".equals(call.name)
                    || !call.owner.equals("com/bslx/XueXiMod/shu/technique/release/deflection/projectile/ArrowBook")) {
                continue;
            }
            pendingStores++;
            if (previousOpcode(insn) == Opcodes.FCONST_0) {
                zeroStores++;
            }
        }
        if (zeroStores >= 1 && pendingStores >= 3) {
            ok("结构：ArrowFlight.consider 有 " + zeroStores + " 处 fconst_0 紧接 rememberPending，另有待定保存共 "
                    + pendingStores + " 处。这只说明字节码形状，consider 本身没有执行");
        } else {
            fail("ArrowFlight.consider 的 0 待定形状不对，fconst_0 保存 " + zeroStores + " 总保存 " + pendingStores);
        }
        gap("C 的 consider 分支未执行：它要 ServerLevel 和箭实体。上面只核对了生产 class 的指令形状。");
    }

    private void auditTargets() throws IOException {
        if (!Files.isRegularFile(recompJar)) {
            fail("找不到映射 recomp jar " + recompJar);
            return;
        }
        ClassNode arrow = classFromJar(recompJar, "net/minecraft/world/entity/projectile/AbstractArrow.class");
        ClassNode mob = classFromJar(recompJar, "net/minecraft/world/entity/Mob.class");
        ClassNode hooks = classFromJar(recompJar, "net/minecraftforge/common/ForgeHooks.class");
        if (arrow == null || mob == null || hooks == null) {
            fail("recomp jar 缺少 AbstractArrow、Mob 或 ForgeHooks");
            return;
        }
        countTarget(arrow, "tick", Opcodes.INVOKEVIRTUAL, "net/minecraft/world/entity/projectile/AbstractArrow",
                "findHitEntity", "(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/EntityHitResult;");
        countTarget(arrow, "onHitEntity", Opcodes.INVOKEINTERFACE, "net/minecraft/util/RandomSource",
                "nextInt", "(I)I");
        countTarget(arrow, "onHitEntity", Opcodes.INVOKEVIRTUAL, "net/minecraft/world/entity/Entity",
                "hurt", HURT_DESC);
        countTarget(mob, "doHurtTarget", Opcodes.INVOKESTATIC, "net/minecraft/world/item/enchantment/EnchantmentHelper",
                "getFireAspect", "(Lnet/minecraft/world/entity/LivingEntity;)I");
        countTarget(mob, "doHurtTarget", Opcodes.INVOKEVIRTUAL, "net/minecraft/world/entity/Entity",
                "hurt", HURT_DESC);
        countTarget(hooks, "onLivingBreathe", Opcodes.INVOKEVIRTUAL, "net/minecraft/world/entity/LivingEntity",
                "hurt", HURT_DESC);
        requireMethod(arrow, "defineSynchedData");
        requireMethod(arrow, "canHitEntity");
        info("目标计数来自 forge-1.20.1-47.3.33 mapped recomp。每一处等于 1 只说明原版方法里有这个调用，不等于三份注入已经一起应用成功。");
    }

    private void auditHurtAnnotations(Map<String, Loaded> compiled) {
        int auraModify = countHurtInjection(compiled.get(MIXIN_PREFIX + "ArrowAuraMixin"),
                "Lorg/spongepowered/asm/mixin/injection/ModifyArg;");
        int deflectionModify = countHurtInjection(compiled.get(MIXIN_PREFIX + "ArrowDeflectionMixin"),
                "Lorg/spongepowered/asm/mixin/injection/ModifyArg;");
        int deflectionRedirect = countHurtInjection(compiled.get(MIXIN_PREFIX + "ArrowDeflectionMixin"),
                "Lorg/spongepowered/asm/mixin/injection/Redirect;");
        if (auraModify == 1 && deflectionModify == 1 && deflectionRedirect == 1) {
            ok("注解各 1 份：ArrowAura ModifyArg、ArrowDeflection ModifyArg、ArrowDeflection Redirect，都指向 onHitEntity 的 Entity.hurt");
        } else {
            fail("hurt 注解数量 AuraModify=" + auraModify + " DeflectionModify=" + deflectionModify
                    + " DeflectionRedirect=" + deflectionRedirect);
        }
        gap("这三份注解和 recomp 里的 1 次 hurt 调用是分开的事实。目标存在不等于三份注入已经组合成功。");
    }

    private static int countHurtInjection(Loaded loaded, String annotationDescriptor) {
        if (loaded == null) {
            return 0;
        }
        int found = 0;
        for (MethodNode method : loaded.node.methods) {
            for (AnnotationNode annotation : annotationsOf(method)) {
                if (!annotationDescriptor.equals(annotation.desc)) {
                    continue;
                }
                if (namesOnHit(annotationValue(annotation, "method"))
                        && targetsHurt(annotationValue(annotation, "at"))) {
                    found++;
                }
            }
        }
        return found;
    }

    private static Object annotationValue(AnnotationNode annotation, String key) {
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

    private static boolean namesOnHit(Object value) {
        if (value instanceof String text) {
            return "onHitEntity".equals(text);
        }
        if (value instanceof List<?> items) {
            for (Object item : items) {
                if (namesOnHit(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean targetsHurt(Object value) {
        if (value instanceof AnnotationNode at) {
            Object target = annotationValue(at, "target");
            return ENTITY_HURT.equals(target);
        }
        if (value instanceof List<?> items) {
            for (Object item : items) {
                if (targetsHurt(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void auditRefmap() throws IOException {
        if (!Files.isRegularFile(refmap)) {
            fail("compileJava 没有留下 refmap " + refmap);
            return;
        }
        String json = Files.readString(refmap, StandardCharsets.UTF_8);
        requireRefmap(json, "com/bslx/XueXiMod/mixin/ArrowDeflectionMixin", ENTITY_HURT, "ArrowDeflection Entity.hurt");
        requireRefmap(json, "com/bslx/XueXiMod/mixin/ArrowDeflectionMixin", NEXT_INT, "ArrowDeflection nextInt");
        requireRefmap(json, "com/bslx/XueXiMod/mixin/ArrowDeflectionMixin", FIND_HIT, "ArrowDeflection findHitEntity");
        requireRefmap(json, "com/bslx/XueXiMod/mixin/ArrowDeflectionMixin", "\"onHitEntity\"", "ArrowDeflection onHitEntity");
        requireRefmap(json, "com/bslx/XueXiMod/mixin/ArrowAuraMixin", ENTITY_HURT, "ArrowAura Entity.hurt");
        requireRefmap(json, "com/bslx/XueXiMod/mixin/ArrowAuraMixin", "\"onHitEntity\"", "ArrowAura onHitEntity");
        requireRefmap(json, "com/bslx/XueXiMod/mixin/MobMeleeDeflectionMixin", FIRE_ASPECT, "MobMelee getFireAspect");
        requireRefmap(json, "com/bslx/XueXiMod/mixin/MobMeleeDeflectionMixin", ENTITY_HURT, "MobMelee Entity.hurt");
        requireRefmap(json, "com/bslx/XueXiMod/mixin/PressureDrownMixin", LIVING_HURT, "PressureDrown LivingEntity.hurt");
        gap("refmap 只证明注解目标写进了映射文件。它不能证明 ArrowAura ModifyArg、Deflection ModifyArg 和 Deflection Redirect 已经同时打进同一次 hurt。");
    }

    private void probeMixinService() {
        System.out.println("真实Mixin服务可用性探测");
        try {
            Class<?> serviceClass = Class.forName("org.spongepowered.asm.service.MixinService");
            Object service = serviceClass.getMethod("getService").invoke(null);
            String serviceName = service == null ? "null" : service.getClass().getName();
            gap("真实Mixin服务可用性探测：MixinService.getService 返回 " + serviceName
                    + "。这次只调用了 getService，没有调用 Transformer，没有变换 AbstractArrow。真实组合注入仍未验证。");
        } catch (Throwable failure) {
            String detail = failure.getClass().getName();
            if (failure.getCause() != null) {
                detail = detail + " / " + failure.getCause().getClass().getName() + ": " + failure.getCause().getMessage();
            } else if (failure.getMessage() != null) {
                detail = detail + ": " + failure.getMessage();
            }
            gap("真实Mixin服务可用性探测：MixinService.getService 失败：" + detail
                    + "。真实宿主或服务依赖缺失，探测在此停止。没有继续造 harness，没有调用 Transformer。真实组合注入仍未验证。");
        }
    }

    private void countTarget(ClassNode node, String methodName, int opcode, String owner, String name, String descriptor) {
        MethodNode method = onlyMethod(node, methodName);
        if (method == null) {
            return;
        }
        int found = countCalls(method, opcode, owner, name, descriptor);
        if (found == 1) {
            ok("recomp " + node.name + "." + methodName + " " + owner + "." + name + descriptor + " 数量 1");
        } else {
            fail("recomp " + node.name + "." + methodName + " " + name + " 数量 " + found);
        }
    }

    private void requireMethod(ClassNode node, String methodName) {
        if (onlyMethod(node, methodName) != null) {
            ok("recomp " + node.name + " 存在方法 " + methodName);
        }
    }

    private void requireRefmap(String json, String mixinKey, String needle, String label) {
        int copies = 0;
        int from = 0;
        String quoted = "\"" + mixinKey + "\"";
        while (from < json.length()) {
            int at = json.indexOf(quoted, from);
            if (at < 0) {
                break;
            }
            int open = json.indexOf('{', at);
            String body = balancedObject(json, open);
            if (body != null && body.contains(needle)) {
                copies++;
            }
            from = at + quoted.length();
        }
        if (copies >= 2) {
            ok("refmap mappings 和 data.searge 都有 " + label);
        } else {
            fail("refmap 里 " + label + " 只找到 " + copies + " 份");
        }
    }

    private static String balancedObject(String json, int open) {
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

    private static boolean putstaticAfterDefineId(MethodNode method, String owner, String fieldName) {
        int defineAt = -1;
        int putAt = -1;
        int cursor = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKESTATIC
                    && DEFINE_OWNER.equals(call.owner) && "defineId".equals(call.name)) {
                defineAt = cursor;
            }
            if (insn instanceof FieldInsnNode field && field.getOpcode() == Opcodes.PUTSTATIC
                    && owner.equals(field.owner) && fieldName.equals(field.name)) {
                putAt = cursor;
            }
            cursor++;
        }
        return defineAt >= 0 && putAt > defineAt;
    }

    private static MethodNode methodCalling(ClassNode node, int opcode, String owner, String name) {
        MethodNode found = null;
        int seen = 0;
        for (MethodNode method : node.methods) {
            if (countCalls(method, opcode, owner, name, null) > 0) {
                seen++;
                found = method;
            }
        }
        return seen == 1 ? found : null;
    }

    private static int previousOpcode(AbstractInsnNode insn) {
        AbstractInsnNode previous = insn.getPrevious();
        while (previous != null && (previous instanceof LineNumberNode || previous instanceof FrameNode
                || previous.getOpcode() == -1)) {
            previous = previous.getPrevious();
        }
        return previous == null ? -1 : previous.getOpcode();
    }

    private static boolean instanceBridge(MethodNode method) {
        if (method == null) {
            return false;
        }
        return (method.access & Opcodes.ACC_PUBLIC) != 0
                && (method.access & Opcodes.ACC_STATIC) == 0
                && (method.access & Opcodes.ACC_ABSTRACT) == 0;
    }

    private static MethodNode findDescribed(ClassNode node, String descriptor) {
        MethodNode found = null;
        for (MethodNode method : node.methods) {
            if (descriptor.equals(method.desc) && !method.name.startsWith("<")) {
                if (found != null) {
                    return null;
                }
                found = method;
            }
        }
        return found;
    }

    private static MethodNode findExact(ClassNode node, String name, String descriptor) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(descriptor)) {
                return method;
            }
        }
        return null;
    }

    private static boolean isAccessorInterface(ClassNode node) {
        if ((node.access & Opcodes.ACC_INTERFACE) == 0) {
            return false;
        }
        for (MethodNode method : node.methods) {
            if (annotated(annotationsOf(method), ACCESSOR_ANNOTATION) || annotated(annotationsOf(method), INVOKER_ANNOTATION)) {
                return true;
            }
        }
        return false;
    }

    private static List<AnnotationNode> annotationsOf(MethodNode method) {
        List<AnnotationNode> found = new ArrayList<>();
        if (method != null && method.visibleAnnotations != null) {
            found.addAll(method.visibleAnnotations);
        }
        if (method != null && method.invisibleAnnotations != null) {
            found.addAll(method.invisibleAnnotations);
        }
        return found;
    }

    private static boolean annotated(List<AnnotationNode> annotations, String descriptor) {
        if (annotations == null) {
            return false;
        }
        for (AnnotationNode annotation : annotations) {
            if (descriptor.equals(annotation.desc)) {
                return true;
            }
        }
        return false;
    }

    private List<String> rejectedMethods(ClassNode node) {
        List<String> rejected = new ArrayList<>();
        for (MethodNode method : node.methods) {
            if (method.name.startsWith("<")) {
                continue;
            }
            boolean overwrite = annotated(method.visibleAnnotations, OVERWRITE_ANNOTATION);
            boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
            boolean isPrivate = (method.access & Opcodes.ACC_PRIVATE) != 0;
            boolean isSynthetic = (method.access & Opcodes.ACC_SYNTHETIC) != 0;
            if (isStatic && !isPrivate && !isSynthetic && !overwrite) {
                rejected.add(method.name + method.desc);
            }
        }
        return rejected;
    }

    private static boolean matchesAny(String owner, Set<String> names) {
        if (names.contains(owner)) {
            return true;
        }
        for (String name : names) {
            if (owner.startsWith(name + "$")) {
                return true;
            }
        }
        return false;
    }

    private static String referencedOwner(AbstractInsnNode insn) {
        if (insn instanceof MethodInsnNode call) {
            return call.owner;
        }
        if (insn instanceof FieldInsnNode field) {
            return field.owner;
        }
        if (insn instanceof TypeInsnNode type) {
            return type.desc;
        }
        if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Type loaded && loaded.getSort() == Type.OBJECT) {
            return loaded.getInternalName();
        }
        return null;
    }

    private static String referencedDetail(AbstractInsnNode insn) {
        if (insn instanceof MethodInsnNode call) {
            return call.name + call.desc;
        }
        if (insn instanceof FieldInsnNode field) {
            return field.name + " " + field.desc;
        }
        if (insn instanceof TypeInsnNode type) {
            return type.desc;
        }
        return "";
    }

    private static int countCalls(ClassNode node, int opcode, String owner, String name, String descriptor) {
        int found = 0;
        for (MethodNode method : node.methods) {
            found += countCalls(method, opcode, owner, name, descriptor);
        }
        return found;
    }

    private static int countArrowDefineId(MethodNode method) {
        if (method == null || method.instructions == null) {
            return 0;
        }
        int found = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode call) || call.getOpcode() != Opcodes.INVOKESTATIC
                    || !DEFINE_OWNER.equals(call.owner) || !"defineId".equals(call.name)) {
                continue;
            }
            if (precedingType(call, "net/minecraft/world/entity/projectile/AbstractArrow")) {
                found++;
            }
        }
        return found;
    }

    private static boolean precedingType(AbstractInsnNode insn, String internalName) {
        AbstractInsnNode previous = insn.getPrevious();
        int looked = 0;
        while (previous != null && looked < 8) {
            if (previous instanceof LdcInsnNode ldc && ldc.cst instanceof Type type && type.getSort() == Type.OBJECT
                    && internalName.equals(type.getInternalName())) {
                return true;
            }
            previous = previous.getPrevious();
            looked++;
        }
        return false;
    }

    private static int countCalls(MethodNode method, int opcode, String owner, String name, String descriptor) {
        if (method == null || method.instructions == null) {
            return 0;
        }
        int found = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call && call.getOpcode() == opcode && call.owner.equals(owner)
                    && call.name.equals(name) && (descriptor == null || call.desc.equals(descriptor))) {
                found++;
            }
        }
        return found;
    }

    private MethodNode onlyMethod(ClassNode node, String name) {
        MethodNode found = null;
        int seen = 0;
        List<String> descriptors = new ArrayList<>();
        for (MethodNode method : node.methods) {
            if (method.name.equals(name)) {
                seen++;
                found = method;
                descriptors.add(method.desc);
            }
        }
        if (seen != 1) {
            fail(node.name + "." + name + " 出现 " + seen + " 次 " + descriptors);
            return null;
        }
        return found;
    }

    private static boolean isCall(AbstractInsnNode insn, int opcode, String owner, String name) {
        return insn instanceof MethodInsnNode call && call.getOpcode() == opcode && call.name.equals(name)
                && (owner == null || owner.equals(call.owner));
    }

    private static boolean intOperand(AbstractInsnNode insn, int opcode, int operand) {
        return insn instanceof IntInsnNode value && insn.getOpcode() == opcode && value.operand == operand;
    }

    private static boolean ldcType(AbstractInsnNode insn, String internalName) {
        return insn instanceof LdcInsnNode ldc && ldc.cst instanceof Type type
                && internalName.equals(type.getInternalName());
    }

    private static ClassNode classFromJar(Path jar, String entry) throws IOException {
        try (JarFile file = new JarFile(jar.toFile())) {
            JarEntry found = file.getJarEntry(entry);
            if (found == null) {
                return null;
            }
            return readNode(file.getInputStream(found).readAllBytes());
        }
    }

    private static Map<String, Loaded> loadTree(Path root) throws IOException {
        Map<String, Loaded> loaded = new HashMap<>();
        if (!Files.isDirectory(root)) {
            return loaded;
        }
        try (var walk = Files.walk(root)) {
            for (Path path : walk.filter(candidate -> candidate.toString().endsWith(".class")).toList()) {
                byte[] bytes = Files.readAllBytes(path);
                ClassNode node = readNode(bytes);
                loaded.put(node.name, new Loaded(bytes, node));
            }
        }
        return loaded;
    }

    private static Loaded loadFile(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        return new Loaded(bytes, readNode(bytes));
    }

    private static ClassNode readNode(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static List<String> jsonArray(String json, String key) {
        int keyAt = json.indexOf("\"" + key + "\"");
        if (keyAt < 0) {
            return List.of();
        }
        int open = json.indexOf('[', keyAt);
        int close = json.indexOf(']', open);
        if (open < 0 || close < 0) {
            return List.of();
        }
        List<String> found = new ArrayList<>();
        Matcher matcher = Pattern.compile("\"([^\"]+)\"").matcher(json.substring(open + 1, close));
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return found;
    }

    private static String jsonValue(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static Map<String, String> parse(String[] parameters) {
        Map<String, String> options = new HashMap<>();
        for (int cursor = 0; cursor + 1 < parameters.length; cursor += 2) {
            String token = parameters[cursor];
            if (token.startsWith("--")) {
                options.put(token.substring(2), parameters[cursor + 1]);
            }
        }
        return options;
    }

    private static String sha256(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder builder = new StringBuilder(digest.length * 2);
        for (byte piece : digest) {
            builder.append(String.format("%02X", piece));
        }
        return builder.toString();
    }

    private void ok(String message) {
        checks++;
        System.out.println("OK " + message);
    }

    private void fail(String message) {
        checks++;
        failures++;
        System.out.println("FAIL " + message);
    }

    private void info(String message) {
        System.out.println("INFO " + message);
    }

    private void gap(String message) {
        System.out.println("GAP " + message);
    }

    private record Loaded(byte[] bytes, ClassNode node) {
    }
}
