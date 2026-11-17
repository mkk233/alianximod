package com.bslx.XueXiMod.shu.technique.release.prison;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;

/**
 * 随机传送的外层包装。立刻再进入同一方法时只跳过这一次包装，嵌套调用仍各自有作用域。
 * 标记按线程栈和实体对上才消费，不会在内层全程开着一个全局放行。
 */
public final class PrisonRandomGate {
    private static final ThreadLocal<ArrayDeque<Skip>> SKIPS = ThreadLocal.withInitial(ArrayDeque::new);

    private PrisonRandomGate() {
    }

    /** 消费栈顶属于这个实体的一次跳过标记。 */
    public static boolean consume(LivingEntity entity) {
        ArrayDeque<Skip> stack = SKIPS.get();
        Skip top = stack.peek();
        if (top == null || top.subject != entity) {
            return false;
        }
        stack.pop();
        if (stack.isEmpty()) {
            SKIPS.remove();
        }
        return true;
    }

    /** 包住一次随机传送。成功并真正移动后，活动牢按传送结束。 */
    public static boolean wrap(LivingEntity entity, double worldX, double worldY, double worldZ, boolean broadcast) {
        if (entity == null) {
            return false;
        }
        Skip token = arm(entity);
        Vec3 before = entity.position();
        ResourceKey<Level> dimension = entity.level().dimension();
        boolean succeeded = false;
        try (PrisonMotionScope.Open opened = PrisonMotionScope.open(PrisonMotionScope.Kind.RANDOM)) {
            try {
                succeeded = entity.randomTeleport(worldX, worldY, worldZ, broadcast);
            } finally {
                disarm(token);
            }
        }
        if (succeeded && PrisonMotion.moved(entity, before, dimension) && PrisonMotion.locks(entity)) {
            PrisonRuntime.endTarget(entity, EndReason.TELEPORT, entity);
        }
        return succeeded;
    }

    private static Skip arm(LivingEntity entity) {
        Skip token = new Skip(entity);
        SKIPS.get().addLast(token);
        return token;
    }

    private static void disarm(Skip token) {
        ArrayDeque<Skip> stack = SKIPS.get();
        if (stack.peek() == token) {
            stack.pop();
        }
        if (stack.isEmpty()) {
            SKIPS.remove();
        }
    }

    private static final class Skip {
        private final LivingEntity subject;

        private Skip(LivingEntity subject) {
            this.subject = subject;
        }
    }
}
