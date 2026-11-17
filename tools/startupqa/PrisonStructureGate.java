import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
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
 * 核对水牢 Mixin 目标、随机传送清理、末地成功分支、伤害字节码和收据划出。
 * 只读当前编译产物和映射 jar，不调用 Mixin，也不做变换。
 */
public final class PrisonStructureGate {
    private static final String RECOMP = "C:/Users/baishang/.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.20.1-47.3.33_mapped_parchment_2023.06.26-1.20.1/forge-1.20.1-47.3.33_mapped_parchment_2023.06.26-1.20.1-recomp.jar";
    private static final String LIVING = "net/minecraft/world/entity/LivingEntity";
    private static final String ENTITY = "net/minecraft/world/entity/Entity";
    private static final String SERVER_PLAYER = "net/minecraft/server/level/ServerPlayer";
    private static final String CONNECTION = "net/minecraft/server/network/ServerGamePacketListenerImpl";
    private static final String DRAGON = "net/minecraft/world/entity/boss/enderdragon/EnderDragon";
    private static final String RANDOM_TELEPORT = "randomTeleport(DDDZ)Z";
    private static final String RANDOM_DESC = "(DDDZ)Z";
    private static final String CHANGE_DIMENSION = "changeDimension(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraftforge/common/util/ITeleporter;)Lnet/minecraft/world/entity/Entity;";
    private static final String CHANGE_DESC = "(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraftforge/common/util/ITeleporter;)Lnet/minecraft/world/entity/Entity;";
    private static final String PRISON = "com/bslx/XueXiMod/shu/technique/release/prison";
    private static final String SAVED_DATA = "net/minecraft/world/level/saveddata/SavedData";
    private static final String INJECT = "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String REDIRECT = "Lorg/spongepowered/asm/mixin/injection/Redirect;";
    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";

    private final Path recompJar;
    private final Path classesRoot;
    private final Path mixinsJson;
    private final Path refmap;
    private int checks;
    private int failures;

    private PrisonStructureGate(Path recompJar, Path classesRoot, Path mixinsJson, Path refmap) {
        this.recompJar = recompJar;
        this.classesRoot = classesRoot;
        this.mixinsJson = mixinsJson;
        this.refmap = refmap;
    }

    public static void main(String[] arguments) throws Exception {
        Path project = Path.of("D:/alianximod");
        PrisonStructureGate gate = new PrisonStructureGate(
                Path.of(RECOMP),
                project.resolve("build/classes/java/main"),
                project.resolve("src/main/resources/xueximod.mixins.json"),
                project.resolve("build/tmp/compileJava/compileJava-refmap.json"));
        gate.run();
        System.out.println("断言 " + gate.checks + " 失败 " + gate.failures);
        System.out.println("启动证明 否");
        System.out.println("完整变换 未验证");
        System.out.println("未执行 实体结算、网络投递、Mixin 应用到正在运行的类");
        if (gate.failures != 0) {
            System.exit(1);
        }
    }

    private void run() throws Exception {
        checkMappedTargets();
        List<ClassNode> mixins = loadPrisonMixins();
        checkNoPublicStatic(mixins);
        checkStableHooks(mixins);
        checkRandomTeleport(mixins);
        checkEndBranch(mixins);
        checkPressure();
        checkWetAndCadence();
        checkReceiptWithdraw();
        checkPersistence();
        checkNetwork();
        checkClientSeparation();
        checkOrdinaryDoesNotCallMixin();
        checkRefmap(mixins);
    }

    private void checkMappedTargets() throws IOException {
        ClassNode living = classFromJar(LIVING);
        ClassNode entity = classFromJar(ENTITY);
        ClassNode player = classFromJar(SERVER_PLAYER);
        ClassNode connection = classFromJar(CONNECTION);
        check(living != null && method(living, "randomTeleport", RANDOM_DESC) != null,
                "映射 LivingEntity.randomTeleport" + RANDOM_DESC + " 不存在");
        check(living != null && method(living, "jumpFromGround", "()V") != null, "映射 jumpFromGround()V 不存在");
        check(living != null && method(living, "knockback", "(DDD)V") != null, "映射 knockback(DDD)V 不存在");
        check(living != null && method(living, "jumpInLiquid", "(Lnet/minecraft/tags/TagKey;)V") != null,
                "映射 jumpInLiquid(TagKey)V 不存在");
        check(entity != null && method(entity, "changeDimension", CHANGE_DESC) != null,
                "映射 Entity.changeDimension 的 Forge 描述符不存在");
        check(entity != null && method(entity, "move", "(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V") != null,
                "映射 Entity.move 不存在");
        if (player == null) {
            check(false, "映射 jar 没有 ServerPlayer");
            return;
        }
        MethodNode change = method(player, "changeDimension", CHANGE_DESC);
        if (change == null) {
            List<String> found = new ArrayList<>();
            for (MethodNode candidate : player.methods) {
                if ("changeDimension".equals(candidate.name)) {
                    found.add(candidate.name + candidate.desc);
                }
            }
            check(false, "ADAPTATION_PENDING ServerPlayer.changeDimension 描述符不是已记录的 Forge 签名，实际 " + found);
            return;
        }
        boolean removesPlayer = countInvoke(change, "remove") >= 1 || countInvoke(change, "removePlayerImmediately") >= 1;
        check(countReturnThis(change) >= 1 && removesPlayer,
                "ServerPlayer.changeDimension 没有 return this，也没有 remove 或 removePlayerImmediately");
        check(connection != null && method(connection, "handleMovePlayer",
                        "(Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;)V") != null,
                "映射 handleMovePlayer 不存在");
    }

    private List<ClassNode> loadPrisonMixins() throws IOException {
        List<ClassNode> loaded = new ArrayList<>();
        check(Files.isRegularFile(mixinsJson), "缺少 " + mixinsJson);
        if (!Files.isRegularFile(mixinsJson)) {
            return loaded;
        }
        String json = Files.readString(mixinsJson, StandardCharsets.UTF_8);
        String packageName = jsonString(json, "package");
        check("com.bslx.XueXiMod.mixin".equals(packageName), "Mixin package 实际 " + packageName);
        for (String entry : quotedStrings(json)) {
            if (!entry.contains("Prison")) {
                continue;
            }
            String internal = packageName.replace('.', '/') + "/" + entry.replace('.', '/');
            ClassNode node = classFromClasses(internal);
            check(node != null, "mixins.json 登记了 " + entry + " 但编译产物没有 " + internal);
            if (node != null) {
                loaded.add(node);
            }
        }
        check(!loaded.isEmpty(), "没有加载到任何水牢 Mixin");
        return loaded;
    }

    private void checkNoPublicStatic(List<ClassNode> mixins) {
        for (ClassNode node : mixins) {
            List<String> rejected = new ArrayList<>();
            for (MethodNode method : node.methods) {
                if (method.name.startsWith("<")) {
                    continue;
                }
                boolean publicStatic = (method.access & Opcodes.ACC_PUBLIC) != 0
                        && (method.access & Opcodes.ACC_STATIC) != 0;
                if (publicStatic) {
                    rejected.add(method.name + method.desc);
                }
            }
            check(rejected.isEmpty(), node.name + " 有公开静态方法 " + rejected);
        }
    }

    private void checkStableHooks(List<ClassNode> mixins) {
        check(countInject(mixins, "ClientPlayPrisonMixin", "handleMovePlayer", "HEAD") == 1,
                "ClientPlayPrisonMixin.handleMovePlayer HEAD 不是 1 处");
        check(countInject(mixins, "LocalPlayerPrisonMixin", "tick", "") 
                        + countInject(mixins, "LocalPlayerPrisonMixin", "sendPosition", "HEAD") >= 3,
                "LocalPlayerPrisonMixin 注入不足 3 处");
        check(countInject(mixins, "PrisonLivingMixin", "jumpFromGround", "HEAD") == 1, "跳跃 HEAD 不是 1 处");
        check(countInject(mixins, "PrisonLivingMixin", "knockback", "HEAD") == 1, "击退 HEAD 不是 1 处");
        check(countInject(mixins, "PrisonLivingMixin", "jumpInLiquid", "HEAD") == 1, "液体跳 HEAD 不是 1 处");
        check(countInject(mixins, "PrisonEntityMixin", "move", "HEAD") == 1, "move HEAD 不是 1 处");
        check(countInvokeOwner(mixins, PRISON + "/PrisonRuntime", "crossDimension") >= 1,
                "普通换维没有再调用 PrisonRuntime.crossDimension");
        check(countRedirectTarget(mixins, "handleMovePlayer", "absMoveTo") >= 1, "移动包 absMoveTo 重定向丢了");
        check(ordinalOf(mixins, "handleMovePlayer", "absMoveTo") == 1, "封包落点 absMoveTo 的 ordinal 不是 1");
    }

    private void checkRandomTeleport(List<ClassNode> mixins) throws IOException {
        int head = countInject(mixins, "PrisonLivingMixin", "randomTeleport", "HEAD");
        int returned = countInject(mixins, "", "randomTeleport", "RETURN");
        check(head == 1, "randomTeleport HEAD 注入数 " + head + "，要求恰好 1");
        check(returned == 0, "randomTeleport 仍有 RETURN 注入 " + returned + "，异常时不会配对离开");
        MethodNode wrapper = injectMethod(mixins, "randomTeleport", "HEAD");
        check(wrapper != null && cancellable(wrapper), "randomTeleport HEAD 不是 cancellable");
        if (wrapper == null) {
            return;
        }
        MethodNode body = methodContainingTeleport(wrapper, mixins);
        check(body != null, "ADAPTATION_PENDING 没有在 HEAD 或它调用的普通 helper 里找到 randomTeleport 调用");
        if (body == null) {
            return;
        }
        check(finallyCovers(body, "randomTeleport", RANDOM_DESC),
                "randomTeleport 调用没有被 try/finally 包住");
        check(countInvoke(body, "disarm") >= 1 && countInvoke(body, "close") >= 1,
                "包装没有同时清掉单次跳过标记并关闭随机传送作用域");
        check(hasJumpBefore(body, "randomTeleport", RANDOM_DESC),
                "内层没有在调用原方法前消费跳过标记");
        check(!ownerIsMixin(body, "randomTeleport", RANDOM_DESC),
                "原 randomTeleport 被放在 Mixin 类里调用，桥必须是普通 helper");
        check(clearsInFinally(body), "finally 里没有关闭作用域或清标记");
    }

    private void checkEndBranch(List<ClassNode> mixins) {
        List<MethodNode> hooks = injectsOn(mixins, SERVER_PLAYER, "changeDimension");
        check(!hooks.isEmpty(), "ServerPlayer.changeDimension 没有清理注入，Entity 的 RETURN 覆盖不到末地提前 return this");
        boolean headCleanup = false;
        boolean conditional = false;
        for (MethodNode hook : hooks) {
            String at = atName(hook);
            boolean callsPrison = countInvokePrefix(hook, PRISON) >= 1;
            if ("HEAD".equals(at) && callsPrison) {
                headCleanup = true;
            }
            if (callsPrison && hasJumpBeforePrefix(hook, PRISON)) {
                conditional = true;
            }
        }
        check(!headCleanup, "changeDimension 在 HEAD 就清理，可取消尝试开始时不应结束牢");
        if (!conditional) {
            conditional = helperBranchesBeforeEnd(hooks);
        }
        check(conditional, "末地成功分支的清理没有条件，失败返回也会结束牢");
        System.out.println("未执行 末地通关和克隆的实际事件顺序，这里只核对注入落点");
    }

    private void checkPressure() throws IOException {
        ClassNode pressure = classFromClasses(PRISON + "/PrisonPressure");
        check(pressure != null, "编译产物缺少 PrisonPressure");
        if (pressure == null) {
            return;
        }
        check(floatConstant(pressure, "AMOUNT", 4.0F), "AMOUNT 不是 4");
        check(intConstant(pressure, "INTERVAL_TICKS", 20), "INTERVAL_TICKS 不是 20");
        MethodNode pulse = method(pressure, "pulse", "(Lnet/minecraft/world/entity/LivingEntity;)V");
        check(pulse != null, "没有 PrisonPressure.pulse");
        if (pulse == null) {
            return;
        }
        check(countInvoke(pulse, "setHealth") == 0, "pulse 直接 setHealth");
        check(countInvoke(pulse, "outOfWorld") == 0 && countInvoke(pulse, "genericKill") == 0,
                "pulse 走了原版虚空或 genericKill");
        check(countInvoke(pulse, "hurt") >= 2, "pulse 没有同时走末影龙头和普通 hurt");
        check(countField(pulse, DRAGON, "head") >= 1, "pulse 没有读 EnderDragon.head");
    }

    private void checkWetAndCadence() throws IOException {
        List<ClassNode> family = loadFamily(PRISON + "/PrisonRuntime");
        check(!family.isEmpty(), "编译产物缺少 PrisonRuntime");
        if (family.isEmpty()) {
            return;
        }
        int preserve = countInvokeOwner(family, "com/bslx/XueXiMod/status/WetnessState", "replacePreservingDuration");
        int grant = countInvokeOwner(family, "com/bslx/XueXiMod/status/WetnessState", "grant");
        int clear = countInvokeOwner(family, "com/bslx/XueXiMod/status/WetnessState", "clear");
        check(preserve >= 1, "ADAPTATION_PENDING 结算没有调用 WetnessState.replacePreservingDuration");
        check(grant == 0, "水牢结算调用了 WetnessState.grant，会把时长刷回 1000");
        check(clear >= 1, "非二级结算没有调用 WetnessState.clear");
        MethodNode halving = methodInvoking(family, "replacePreservingDuration");
        check(halving != null && (countOpcode(halving, Opcodes.IDIV) >= 1 || countInvoke(halving, "floorDiv") >= 1),
                "二级结算方法里没有除法");
        ClassNode wetness = classFromClasses("com/bslx/XueXiMod/status/WetnessState");
        MethodNode preserving = wetness == null ? null : method(wetness, "replacePreservingDuration", null);
        check(preserving != null && countInvoke(preserving, "grant") == 0
                        && countField(preserving, "DURATION_TICKS") == 0
                        && (countInvoke(preserving, "remainingDuration") >= 1 || countInvoke(preserving, "getDuration") >= 1),
                "replacePreservingDuration 没有读剩余时长，或它刷新了 1000 tick");
        MethodNode finisher = methodInvoking(family, "applyWet");
        if (finisher == null) {
            finisher = methodByName(family, "finish");
        }
        if (finisher == null) {
            check(false, "ADAPTATION_PENDING 没有名为 finish 的方法，重复结束无法核对");
        } else {
            int settleAt = firstInvoke(finisher, "settleCarrier");
            if (settleAt < 0) {
                settleAt = firstInvoke(finisher, "replacePreservingDuration");
            }
            if (settleAt < 0) {
                settleAt = firstInvoke(finisher, "applyWet");
            }
            check(settleAt > 0 && earlyFalseReturn(finisher) < settleAt,
                    "finish 没有在扣湿前用 false 返回，重复结束可能再结算");
        }
        MethodNode commit = methodNamedInvoking(family, "commit", "pulse");
        MethodNode tick = methodNamedInvoking(family, "tick", "pulse");
        check(commit != null && countInvokeOwner(commit, PRISON + "/PrisonPressure", "pulse") >= 1,
                "提交成功路径没有立即调用 PrisonPressure.pulse");
        check(tick != null && countInvokeOwner(tick, PRISON + "/PrisonPressure", "pulse") >= 1,
                "后续 tick 没有调用 PrisonPressure.pulse");
        if (tick != null) {
            int finishAt = firstInvoke(tick, "finish");
            int pulseAt = firstInvokeOwner(tick, PRISON + "/PrisonPressure", "pulse");
            check(finishAt >= 0 && pulseAt > finishAt && returnsBetween(tick, finishAt, pulseAt),
                    "tick 没有先按到期结束再跳过本拍伤害");
        }
        boolean intervalUsed = false;
        for (ClassNode node : family) {
            for (MethodNode method : node.methods) {
                if ("<init>".equals(method.name) && usesTwentyTickInterval(method)) {
                    intervalUsed = true;
                }
            }
        }
        check(intervalUsed, "牢记录构造没有把下一跳放在 20 tick 之后");
        System.out.println("未执行 二级当前湿度减半和重复结束的实体调用");
    }

    private void checkReceiptWithdraw() throws IOException {
        ClassNode share = classFromClasses("com/bslx/XueXiMod/network/UncommittedShare");
        ClassNode budget = classFromClasses("com/bslx/XueXiMod/network/CastExecutionSession$GroupBudget");
        check(share != null, "编译产物缺少 UncommittedShare");
        if (share != null) {
            MethodNode retained = method(share, "commitRetained", "()V");
            if (retained == null) {
                check(false, "ADAPTATION_PENDING UncommittedShare.commitRetained()V 不存在");
            } else {
                boolean withdraws = countInvokeOwner(retained, "com/bslx/XueXiMod/player/ManaPaymentReceipt", null) >= 1
                        || countFieldOwner(retained, "com/bslx/XueXiMod/player/ManaPaymentReceipt") >= 1;
                check(withdraws, "UncommittedShare.commitRetained 没有划出收据余额");
            }
        }
        if (budget == null) {
            check(false, "ADAPTATION_PENDING 没有 GroupBudget");
            return;
        }
        MethodNode commit = method(budget, "commit", "(I)V");
        if (commit == null) {
            check(false, "ADAPTATION_PENDING GroupBudget.commit(I)V 不存在");
            return;
        }
        boolean withdraws = countInvokeOwner(commit, "com/bslx/XueXiMod/player/ManaPaymentReceipt", null) >= 1
                || countFieldOwner(commit, "com/bslx/XueXiMod/player/ManaPaymentReceipt") >= 1;
        check(withdraws, "GroupBudget.commit 只改了本地份额，没有划出父收据");
    }

    private void checkPersistence() throws IOException {
        List<String> saved = new ArrayList<>();
        List<String> clones = new ArrayList<>();
        if (!Files.isDirectory(classesRoot)) {
            check(false, "没有 main class 目录 " + classesRoot);
            return;
        }
        try (Stream<Path> walk = Files.walk(classesRoot)) {
            for (Path file : walk.filter(path -> path.toString().endsWith(".class")).toList()) {
                ClassNode node = classFromFile(file);
                if (node == null) {
                    continue;
                }
                if (SAVED_DATA.equals(node.superName)
                        && (node.name.contains("rison") || node.name.contains("/prison/"))) {
                    saved.add(node.name);
                }
                if (mentionsClone(node)) {
                    clones.add(node.name);
                }
            }
        }
        check(!saved.isEmpty(), "ADAPTATION_PENDING 没有水牢相关 SavedData 子类");
        check(!clones.isEmpty(), "ADAPTATION_PENDING 没有类引用 PlayerEvent$Clone 或末地结算快照");
        System.out.println("记录 SavedData " + saved);
        System.out.println("记录 Clone " + clones);
        System.out.println("未执行 卸载重载只扣一次、末地克隆保湿、普通死亡不保旧效果，这些需要实体");
    }

    private void checkNetwork() throws IOException {
        ClassNode network = classFromClasses("com/bslx/XueXiMod/network/ModNetwork");
        ClassNode packet = classFromClasses("com/bslx/XueXiMod/network/prison/PrisonSyncPacket");
        check(network != null && packet != null, "编译产物缺少 ModNetwork 或 PrisonSyncPacket");
        if (network == null || packet == null) {
            return;
        }
        MethodNode register = method(network, "register", "()V");
        check(register != null, "ModNetwork.register 不存在");
        if (register != null) {
            List<Integer> identifiers = packetIds(register);
            check(identifiers.size() == identifiers.stream().distinct().count(), "网络包编号重复 " + identifiers);
            check(countInvokeOwner(register, "com/bslx/XueXiMod/network/prison/PrisonSyncPacket", "register") == 1,
                    "PrisonSyncPacket 没有恰好登记一次");
        }
        MethodNode packetRegister = method(packet, "register", null);
        check(packetRegister != null && countField(packetRegister, "PLAY_TO_CLIENT") >= 1,
                "水牢包没有 PLAY_TO_CLIENT");
        check(packetRegister != null && countField(packetRegister, "PLAY_TO_SERVER") == 0,
                "水牢包标成了 PLAY_TO_SERVER");
    }

    private void checkClientSeparation() throws IOException {
        List<String> leaks = new ArrayList<>();
        if (!Files.isDirectory(classesRoot)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(classesRoot)) {
            for (Path file : walk.filter(path -> path.toString().endsWith(".class")).toList()) {
                String normalized = file.toString().replace('\\', '/');
                boolean commonPrison = normalized.contains("/shu/technique/release/prison/")
                        || normalized.contains("/network/prison/")
                        || (normalized.contains("/mixin/Prison") && !normalized.contains("/mixin/client/"));
                if (!commonPrison) {
                    continue;
                }
                ClassNode node = classFromFile(file);
                if (node != null && mentions(node, "com/bslx/XueXiMod/client")) {
                    leaks.add(node.name);
                }
            }
        }
        check(leaks.isEmpty(), "公共或服务端水牢类引用了客户端 " + leaks);
    }

    private void checkOrdinaryDoesNotCallMixin() throws IOException {
        List<String> hits = new ArrayList<>();
        if (!Files.isDirectory(classesRoot)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(classesRoot)) {
            for (Path file : walk.filter(path -> path.toString().endsWith(".class")).toList()) {
                ClassNode node = classFromFile(file);
                if (node == null || node.name.contains("/mixin/")) {
                    continue;
                }
                if (mentions(node, "com/bslx/XueXiMod/mixin/Prison")
                        || mentions(node, "com/bslx/XueXiMod/mixin/client/ClientPlayPrison")
                        || mentions(node, "com/bslx/XueXiMod/mixin/client/LocalPlayerPrison")) {
                    hits.add(node.name);
                }
            }
        }
        check(hits.isEmpty(), "普通类调用了水牢 Mixin " + hits);
    }

    private void checkRefmap(List<ClassNode> mixins) throws IOException {
        if (!Files.isRegularFile(refmap)) {
            check(false, "缺少 refmap " + refmap);
            return;
        }
        String json = Files.readString(refmap, StandardCharsets.UTF_8);
        check(json.contains("PrisonLivingMixin") && json.contains(RANDOM_TELEPORT),
                "refmap 没有 PrisonLivingMixin 的 randomTeleport(DDDZ)Z");
        check(json.contains("PrisonEntityMixin") && json.contains("move("),
                "refmap 没有 PrisonEntityMixin 的已重映射 move");
        check(explicitUnmapped(mixins, "changeDimension") || json.contains("changeDimension"),
                "changeDimension 没有 remap=false，refmap 里也没有它");
        check(json.contains("PrisonServerPlayerMixin"), "refmap 没有 PrisonServerPlayerMixin");
        check(json.contains("ClientPlayPrisonMixin") && json.contains("LocalPlayerPrisonMixin"),
                "refmap 没有客户端水牢 Mixin");
    }

    private MethodNode methodContainingTeleport(MethodNode wrapper, List<ClassNode> mixins) throws IOException {
        if (countInvokeDesc(wrapper, "randomTeleport", RANDOM_DESC) >= 1) {
            return wrapper;
        }
        for (MethodInsnNode call : invokes(wrapper)) {
            if (call.owner.startsWith("com/bslx/XueXiMod/mixin/")) {
                continue;
            }
            ClassNode owner = classFromClasses(call.owner);
            if (owner == null) {
                continue;
            }
            MethodNode helper = method(owner, call.name, call.desc);
            if (helper != null && countInvokeDesc(helper, "randomTeleport", RANDOM_DESC) >= 1) {
                return helper;
            }
        }
        return null;
    }

    private static boolean finallyCovers(MethodNode method, String name, String descriptor) {
        if (method.tryCatchBlocks == null) {
            return false;
        }
        for (MethodInsnNode call : invokes(method)) {
            if (!name.equals(call.name) || (descriptor != null && !descriptor.equals(call.desc))) {
                continue;
            }
            for (TryCatchBlockNode block : method.tryCatchBlocks) {
                if (block.type == null && covers(method, block, call)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean covers(MethodNode method, TryCatchBlockNode block, AbstractInsnNode target) {
        int start = method.instructions.indexOf(block.start);
        int end = method.instructions.indexOf(block.end);
        int at = method.instructions.indexOf(target);
        return start >= 0 && end >= start && at >= start && at < end;
    }

    private static boolean hasJumpBefore(MethodNode method, String name, String descriptor) {
        int callAt = firstInvokeDesc(method, name, descriptor);
        if (callAt < 0) {
            return false;
        }
        int cursor = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (cursor >= callAt) {
                return false;
            }
            if (insn instanceof JumpInsnNode) {
                return true;
            }
            cursor++;
        }
        return false;
    }

    private static boolean hasJumpBeforePrefix(MethodNode method, String ownerPrefix) {
        int callAt = firstInvokePrefix(method, ownerPrefix);
        if (callAt < 0) {
            return false;
        }
        int cursor = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (cursor >= callAt) {
                return false;
            }
            if (insn instanceof JumpInsnNode) {
                return true;
            }
            cursor++;
        }
        return false;
    }

    private static boolean ownerIsMixin(MethodNode method, String name, String descriptor) {
        for (MethodInsnNode call : invokes(method)) {
            if (name.equals(call.name) && descriptor.equals(call.desc) && call.owner.contains("/mixin/")) {
                return true;
            }
        }
        return false;
    }

    private static boolean clearsInFinally(MethodNode method) {
        if (method.tryCatchBlocks == null) {
            return false;
        }
        for (TryCatchBlockNode block : method.tryCatchBlocks) {
            if (block.type != null || block.handler == null) {
                continue;
            }
            for (AbstractInsnNode insn = block.handler; insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode call) {
                    String called = call.name.toLowerCase();
                    if (called.contains("leave") || called.contains("close") || called.contains("clear")
                            || called.contains("exit") || called.contains("release")
                            || called.contains("scope") || called.contains("token")
                            || called.contains("disarm")) {
                        return true;
                    }
                }
                if (insn.getOpcode() == Opcodes.ATHROW) {
                    break;
                }
            }
        }
        return false;
    }

    private static boolean mentionsClone(ClassNode node) {
        if (node.methods == null) {
            return false;
        }
        for (MethodNode method : node.methods) {
            if (method.desc != null && method.desc.contains("PlayerEvent$Clone")) {
                return true;
            }
            if (method.signature != null && method.signature.contains("PlayerEvent$Clone")) {
                return true;
            }
            for (MethodInsnNode call : invokes(method)) {
                if (call.owner.contains("PlayerEvent$Clone") || call.desc.contains("PlayerEvent$Clone")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<Integer> packetIds(MethodNode method) {
        List<Integer> identifiers = new ArrayList<>();
        AbstractInsnNode previous = null;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call && "register".equals(call.name) && previous != null) {
                if (previous.getOpcode() == Opcodes.BIPUSH || previous.getOpcode() == Opcodes.SIPUSH) {
                    identifiers.add(((org.objectweb.asm.tree.IntInsnNode) previous).operand);
                } else if (previous.getOpcode() >= Opcodes.ICONST_0 && previous.getOpcode() <= Opcodes.ICONST_5) {
                    identifiers.add(previous.getOpcode() - Opcodes.ICONST_0);
                }
            }
            if (insn.getOpcode() >= 0) {
                previous = insn;
            }
        }
        return identifiers;
    }

    private static int countReturnThis(MethodNode method) {
        int count = 0;
        AbstractInsnNode previous = null;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == Opcodes.ARETURN && previous instanceof VarInsnNode load
                    && load.getOpcode() == Opcodes.ALOAD && load.var == 0) {
                count++;
            }
            if (insn.getOpcode() >= 0) {
                previous = insn;
            }
        }
        return count;
    }

    private static int earlyFalseReturn(MethodNode method) {
        int cursor = 0;
        AbstractInsnNode previous = null;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == Opcodes.IRETURN && previous != null && previous.getOpcode() == Opcodes.ICONST_0) {
                return cursor;
            }
            if (insn.getOpcode() >= 0) {
                previous = insn;
            }
            cursor++;
        }
        return Integer.MAX_VALUE;
    }

    private static boolean returnsBetween(MethodNode method, int from, int to) {
        int cursor = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (cursor > from && cursor < to && (insn.getOpcode() == Opcodes.RETURN || insn.getOpcode() == Opcodes.IRETURN
                    || insn.getOpcode() == Opcodes.GOTO)) {
                return true;
            }
            cursor++;
        }
        return false;
    }

    private int countInject(List<ClassNode> mixins, String simpleName, String methodName, String at) {
        int count = 0;
        for (ClassNode node : mixins) {
            if (!simpleName.isEmpty() && !node.name.endsWith("/" + simpleName) && !node.name.endsWith(simpleName)) {
                continue;
            }
            for (MethodNode method : node.methods) {
                AnnotationNode marker = marker(method, INJECT);
                if (marker != null && methodMatches(marker, methodName) && (at.isEmpty() || at.equals(atName(method)))) {
                    count++;
                }
            }
        }
        return count;
    }

    private MethodNode injectMethod(List<ClassNode> mixins, String methodName, String at) {
        for (ClassNode node : mixins) {
            for (MethodNode method : node.methods) {
                AnnotationNode marker = marker(method, INJECT);
                if (marker != null && methodMatches(marker, methodName) && at.equals(atName(method))) {
                    return method;
                }
            }
        }
        return null;
    }

    private List<MethodNode> injectsOn(List<ClassNode> mixins, String ownerInternal, String methodName) {
        List<MethodNode> found = new ArrayList<>();
        for (ClassNode node : mixins) {
            String target = mixinOwner(node);
            if (target == null || !target.contains(ownerInternal)) {
                continue;
            }
            for (MethodNode method : node.methods) {
                AnnotationNode inject = marker(method, INJECT);
                AnnotationNode redirect = marker(method, REDIRECT);
                if ((inject != null && methodMatches(inject, methodName))
                        || (redirect != null && methodMatches(redirect, methodName))) {
                    found.add(method);
                }
            }
        }
        return found;
    }

    private int countRedirectTarget(List<ClassNode> mixins, String methodName, String targetName) {
        int count = 0;
        for (ClassNode node : mixins) {
            for (MethodNode method : node.methods) {
                AnnotationNode redirect = marker(method, REDIRECT);
                if (redirect != null && methodMatches(redirect, methodName) && atTarget(redirect).contains(targetName)) {
                    count++;
                }
            }
        }
        return count;
    }

    private int ordinalOf(List<ClassNode> mixins, String methodName, String targetName) {
        for (ClassNode node : mixins) {
            for (MethodNode method : node.methods) {
                AnnotationNode redirect = marker(method, REDIRECT);
                if (redirect != null && methodMatches(redirect, methodName) && atTarget(redirect).contains(targetName)) {
                    Object ordinal = annotationValue(atNode(redirect), "ordinal");
                    if (ordinal instanceof Integer number) {
                        return number;
                    }
                }
            }
        }
        return -1;
    }

    private static int countInvokeOwner(List<ClassNode> mixins, String owner, String name) {
        int count = 0;
        for (ClassNode node : mixins) {
            count += countInvokeOwner(node, owner, name);
        }
        return count;
    }

    private static int countInvokePrefix(List<ClassNode> mixins, String ownerPrefix) {
        int count = 0;
        for (ClassNode node : mixins) {
            for (MethodNode method : node.methods) {
                count += countInvokePrefix(method, ownerPrefix);
            }
        }
        return count;
    }

    private static int countInvokeOwner(ClassNode node, String owner, String name) {
        int count = 0;
        for (MethodNode method : node.methods) {
            count += countInvokeOwner(method, owner, name);
        }
        return count;
    }

    private static int countInvokeOwner(MethodNode method, String owner, String name) {
        int count = 0;
        for (MethodInsnNode call : invokes(method)) {
            if (owner.equals(call.owner) && (name == null || name.equals(call.name))) {
                count++;
            }
        }
        return count;
    }

    private static int countInvokePrefix(MethodNode method, String ownerPrefix) {
        int count = 0;
        for (MethodInsnNode call : invokes(method)) {
            if (call.owner.startsWith(ownerPrefix)) {
                count++;
            }
        }
        return count;
    }

    private static int countInvoke(MethodNode method, String name) {
        int count = 0;
        for (MethodInsnNode call : invokes(method)) {
            if (name.equals(call.name)) {
                count++;
            }
        }
        return count;
    }

    private static int countInvokeDesc(MethodNode method, String name, String descriptor) {
        int count = 0;
        for (MethodInsnNode call : invokes(method)) {
            if (name.equals(call.name) && descriptor.equals(call.desc)) {
                count++;
            }
        }
        return count;
    }

    private static boolean usesTwentyTickInterval(MethodNode method) {
        if (countField(method, "INTERVAL_TICKS") >= 1) {
            return true;
        }
        if (method.instructions == null) {
            return false;
        }
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof IntInsnNode number && number.operand == 20) {
                return true;
            }
            if (insn instanceof LdcInsnNode constant && constant.cst instanceof Number value && value.longValue() == 20L) {
                return true;
            }
        }
        return false;
    }

    private boolean explicitUnmapped(List<ClassNode> mixins, String methodName) {
        boolean found = false;
        String redirect = "Lorg/spongepowered/asm/mixin/injection/Redirect;";
        for (ClassNode node : mixins) {
            for (MethodNode method : node.methods) {
                AnnotationNode injected = marker(method, INJECT);
                AnnotationNode redirected = marker(method, redirect);
                if (matchesUnmapped(injected, methodName) || matchesUnmapped(redirected, methodName)) {
                    found = true;
                    continue;
                }
                if (namesTarget(injected, methodName) || namesTarget(redirected, methodName)) {
                    return false;
                }
            }
        }
        return found;
    }

    private static boolean matchesUnmapped(AnnotationNode marker, String methodName) {
        return namesTarget(marker, methodName) && Boolean.FALSE.equals(annotationValue(marker, "remap"));
    }

    private static boolean namesTarget(AnnotationNode marker, String methodName) {
        return marker != null && methodMatches(marker, methodName);
    }

    private static int countField(MethodNode method, String name) {
        int count = 0;
        if (method.instructions == null) {
            return 0;
        }
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof FieldInsnNode field && name.equals(field.name)) {
                count++;
            }
        }
        return count;
    }

    private static int countField(MethodNode method, String owner, String name) {
        int count = 0;
        if (method.instructions == null) {
            return 0;
        }
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof FieldInsnNode field && owner.equals(field.owner) && name.equals(field.name)) {
                count++;
            }
        }
        return count;
    }

    private static int countFieldOwner(MethodNode method, String owner) {
        int count = 0;
        if (method.instructions == null) {
            return 0;
        }
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof FieldInsnNode field && owner.equals(field.owner)) {
                count++;
            }
        }
        return count;
    }

    private static int countOpcode(MethodNode method, int opcode) {
        int count = 0;
        if (method.instructions == null) {
            return 0;
        }
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == opcode) {
                count++;
            }
        }
        return count;
    }

    private static int firstInvoke(MethodNode method, String name) {
        return firstInvokeOwner(method, null, name);
    }

    private static int firstInvokeOwner(MethodNode method, String owner, String name) {
        int cursor = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call && name.equals(call.name) && (owner == null || owner.equals(call.owner))) {
                return cursor;
            }
            cursor++;
        }
        return -1;
    }

    private static int firstInvokeDesc(MethodNode method, String name, String descriptor) {
        int cursor = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call && name.equals(call.name) && descriptor.equals(call.desc)) {
                return cursor;
            }
            cursor++;
        }
        return -1;
    }

    private static int firstInvokePrefix(MethodNode method, String ownerPrefix) {
        int cursor = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call && call.owner.startsWith(ownerPrefix)) {
                return cursor;
            }
            cursor++;
        }
        return -1;
    }

    private static List<MethodInsnNode> invokes(MethodNode method) {
        List<MethodInsnNode> calls = new ArrayList<>();
        if (method.instructions == null) {
            return calls;
        }
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call) {
                calls.add(call);
            }
        }
        return calls;
    }

    private boolean helperBranchesBeforeEnd(List<MethodNode> hooks) {
        for (MethodNode hook : hooks) {
            for (MethodInsnNode call : invokes(hook)) {
                if (!call.owner.startsWith(PRISON) || call.owner.contains("/mixin/")) {
                    continue;
                }
                ClassNode owner;
                try {
                    owner = classFromClasses(call.owner);
                } catch (IOException failure) {
                    continue;
                }
                if (owner == null) {
                    continue;
                }
                MethodNode helper = method(owner, call.name, call.desc);
                if (helper != null && hasJumpBeforePrefix(helper, PRISON)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<ClassNode> loadFamily(String internalName) throws IOException {
        List<ClassNode> family = new ArrayList<>();
        ClassNode outer = classFromClasses(internalName);
        if (outer != null) {
            family.add(outer);
        }
        Path folder = classesRoot.resolve(internalName + ".class").getParent();
        String filePrefix = internalName.substring(internalName.lastIndexOf('/') + 1);
        if (folder != null && Files.isDirectory(folder)) {
            try (Stream<Path> walk = Files.list(folder)) {
                for (Path file : walk.toList()) {
                    String fileName = file.getFileName().toString();
                    if (fileName.startsWith(filePrefix + "$") && fileName.endsWith(".class")) {
                        ClassNode inner = classFromFile(file);
                        if (inner != null) {
                            family.add(inner);
                        }
                    }
                }
            }
        }
        return family;
    }

    private static boolean intConstant(ClassNode node, String fieldName, int expected) {
        FieldNode constant = field(node, fieldName);
        if (constant != null && constant.value instanceof Number number && number.intValue() == expected) {
            return true;
        }
        return constantInstruction(node, fieldName, expected, Float.NaN);
    }

    private static boolean floatConstant(ClassNode node, String fieldName, float expected) {
        FieldNode constant = field(node, fieldName);
        if (constant != null && constant.value instanceof Number number && number.floatValue() == expected) {
            return true;
        }
        return constantInstruction(node, fieldName, Integer.MIN_VALUE, expected);
    }

    private static boolean constantInstruction(ClassNode node, String fieldName, int intExpected, float floatExpected) {
        MethodNode clinit = method(node, "<clinit>", "()V");
        if (clinit == null || clinit.instructions == null) {
            return false;
        }
        AbstractInsnNode previous = null;
        for (AbstractInsnNode insn = clinit.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof FieldInsnNode put && put.getOpcode() == Opcodes.PUTSTATIC && fieldName.equals(put.name)) {
                if (!Float.isNaN(floatExpected) && previous instanceof org.objectweb.asm.tree.LdcInsnNode ldc
                        && ldc.cst instanceof Number number) {
                    return number.floatValue() == floatExpected;
                }
                return intOperand(previous) == intExpected;
            }
            if (insn.getOpcode() >= 0) {
                previous = insn;
            }
        }
        return false;
    }

    private static int intOperand(AbstractInsnNode insn) {
        if (insn == null) {
            return Integer.MIN_VALUE;
        }
        if (insn.getOpcode() >= Opcodes.ICONST_M1 && insn.getOpcode() <= Opcodes.ICONST_5) {
            return insn.getOpcode() - Opcodes.ICONST_0;
        }
        if (insn instanceof org.objectweb.asm.tree.IntInsnNode number) {
            return number.operand;
        }
        if (insn instanceof org.objectweb.asm.tree.LdcInsnNode ldc && ldc.cst instanceof Number number) {
            return number.intValue();
        }
        return Integer.MIN_VALUE;
    }

    private static MethodNode methodInvoking(ClassNode node, String name) {
        for (MethodNode method : node.methods) {
            if (countInvoke(method, name) >= 1) {
                return method;
            }
        }
        return null;
    }

    private static MethodNode methodInvoking(List<ClassNode> nodes, String name) {
        for (ClassNode node : nodes) {
            MethodNode found = methodInvoking(node, name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static MethodNode methodByName(ClassNode node, String name) {
        for (MethodNode method : node.methods) {
            if (name.equals(method.name)) {
                return method;
            }
        }
        return null;
    }

    private static MethodNode methodByName(List<ClassNode> nodes, String name) {
        for (ClassNode node : nodes) {
            MethodNode found = methodByName(node, name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static MethodNode methodNamedInvoking(List<ClassNode> nodes, String methodName, String invokeName) {
        for (ClassNode node : nodes) {
            for (MethodNode method : node.methods) {
                if (methodName.equals(method.name) && countInvoke(method, invokeName) >= 1) {
                    return method;
                }
            }
        }
        return null;
    }

    private static MethodNode method(ClassNode node, String name, String descriptor) {
        if (node == null) {
            return null;
        }
        for (MethodNode method : node.methods) {
            if (name.equals(method.name) && (descriptor == null || descriptor.equals(method.desc))) {
                return method;
            }
        }
        return null;
    }

    private static FieldNode field(ClassNode node, String name) {
        for (FieldNode field : node.fields) {
            if (name.equals(field.name)) {
                return field;
            }
        }
        return null;
    }

    private static boolean cancellable(MethodNode method) {
        AnnotationNode inject = marker(method, INJECT);
        return Boolean.TRUE.equals(annotationValue(inject, "cancellable"));
    }

    private static String atName(MethodNode method) {
        AnnotationNode inject = marker(method, INJECT);
        if (inject == null) {
            inject = marker(method, REDIRECT);
        }
        Object value = annotationValue(atNode(inject), "value");
        return value == null ? "" : String.valueOf(value);
    }

    private static String atTarget(AnnotationNode inject) {
        Object value = annotationValue(atNode(inject), "target");
        return value == null ? "" : String.valueOf(value);
    }

    private static AnnotationNode atNode(AnnotationNode inject) {
        Object at = annotationValue(inject, "at");
        if (at instanceof AnnotationNode node) {
            return node;
        }
        if (at instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof AnnotationNode node) {
                    return node;
                }
            }
        }
        return null;
    }

    private static boolean methodMatches(AnnotationNode inject, String methodName) {
        Object value = annotationValue(inject, "method");
        if (value instanceof String text) {
            return text.startsWith(methodName);
        }
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof String text && text.startsWith(methodName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static AnnotationNode marker(MethodNode method, String descriptor) {
        AnnotationNode visible = findAnnotation(method.visibleAnnotations, descriptor);
        if (visible != null) {
            return visible;
        }
        return findAnnotation(method.invisibleAnnotations, descriptor);
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

    private static Object annotationValue(AnnotationNode node, String key) {
        if (node == null || node.values == null) {
            return null;
        }
        for (int cursor = 0; cursor + 1 < node.values.size(); cursor += 2) {
            if (key.equals(node.values.get(cursor))) {
                return node.values.get(cursor + 1);
            }
        }
        return null;
    }

    private static String mixinOwner(ClassNode node) {
        AnnotationNode mixin = findAnnotation(node.visibleAnnotations, MIXIN);
        if (mixin == null) {
            mixin = findAnnotation(node.invisibleAnnotations, MIXIN);
        }
        Object value = annotationValue(mixin, "value");
        if (value instanceof org.objectweb.asm.Type type) {
            return type.getInternalName();
        }
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof org.objectweb.asm.Type type) {
                    return type.getInternalName();
                }
            }
        }
        return null;
    }

    private static boolean mentions(ClassNode node, String prefix) {
        if (node.superName != null && node.superName.startsWith(prefix)) {
            return true;
        }
        if (node.interfaces != null) {
            for (String iface : node.interfaces) {
                if (iface.startsWith(prefix)) {
                    return true;
                }
            }
        }
        for (FieldNode field : node.fields) {
            if (field.desc != null && field.desc.contains(prefix)) {
                return true;
            }
        }
        for (MethodNode method : node.methods) {
            if (method.desc != null && method.desc.contains(prefix)) {
                return true;
            }
            for (MethodInsnNode call : invokes(method)) {
                if (call.owner.startsWith(prefix)) {
                    return true;
                }
            }
            if (method.instructions == null) {
                continue;
            }
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof FieldInsnNode field && field.owner.startsWith(prefix)) {
                    return true;
                }
                if (insn instanceof org.objectweb.asm.tree.TypeInsnNode type && type.desc.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    private ClassNode classFromJar(String internalName) throws IOException {
        if (!Files.isRegularFile(recompJar)) {
            check(false, "映射 jar 不存在 " + recompJar);
            return null;
        }
        try (JarFile jar = new JarFile(recompJar.toFile())) {
            JarEntry entry = jar.getJarEntry(internalName + ".class");
            if (entry == null) {
                return null;
            }
            try (InputStream input = jar.getInputStream(entry)) {
                return readClass(input);
            }
        }
    }

    private ClassNode classFromClasses(String internalName) throws IOException {
        Path file = classesRoot.resolve(internalName + ".class");
        if (!Files.isRegularFile(file)) {
            return null;
        }
        return classFromFile(file);
    }

    private static ClassNode classFromFile(Path file) throws IOException {
        try (InputStream input = Files.newInputStream(file)) {
            return readClass(input);
        }
    }

    private static ClassNode readClass(InputStream input) throws IOException {
        ClassReader reader = new ClassReader(input);
        ClassNode node = new ClassNode();
        reader.accept(node, ClassReader.SKIP_FRAMES);
        return node;
    }

    private static String jsonString(String body, String key) {
        String token = "\"" + key + "\"";
        int at = body.indexOf(token);
        if (at < 0) {
            return null;
        }
        int colon = body.indexOf(':', at + token.length());
        int open = body.indexOf('"', colon + 1);
        int close = body.indexOf('"', open + 1);
        if (colon < 0 || open < 0 || close < 0) {
            return null;
        }
        return body.substring(open + 1, close);
    }

    private static List<String> quotedStrings(String body) {
        List<String> values = new ArrayList<>();
        int cursor = 0;
        while (cursor < body.length()) {
            int open = body.indexOf('"', cursor);
            if (open < 0) {
                break;
            }
            int close = body.indexOf('"', open + 1);
            if (close < 0) {
                break;
            }
            values.add(body.substring(open + 1, close));
            cursor = close + 1;
        }
        return values;
    }

    private void check(boolean passed, String message) {
        checks++;
        if (!passed) {
            failures++;
            System.out.println("失败 " + message);
        }
    }
}
