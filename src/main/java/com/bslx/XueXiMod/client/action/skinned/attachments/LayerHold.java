package com.bslx.XueXiMod.client.action.skinned.attachments;

import java.util.ArrayDeque;

import com.bslx.XueXiMod.client.action.mesh.SkinDiagnostic;

import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.LivingEntity;

/** 附件层上下文栈。关闭只弹出本次 token，外层保持。 */
final class LayerHold {
    private static final ThreadLocal<ArrayDeque<Subject>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private LayerHold() {
    }

    static Subject push(LivingEntity entity, EntityModel<?> model) {
        Subject subject = new Subject(entity, model);
        STACK.get().addLast(subject);
        return subject;
    }

    static void pop(Subject subject) {
        ArrayDeque<Subject> stack = STACK.get();
        if (stack.isEmpty() || stack.peekLast() != subject) {
            if (!stack.contains(subject)) {
                return;
            }
            throw new SkinDiagnostic("layer-order", "附件层关闭顺序和打开顺序不一致");
        }
        stack.removeLast();
    }

    static Subject current() {
        ArrayDeque<Subject> stack = STACK.get();
        return stack.isEmpty() ? null : stack.peekLast();
    }

    static int depth() {
        return STACK.get().size();
    }

    static void discardThread() {
        STACK.get().clear();
    }

    static final class Subject {
        final LivingEntity entity;
        final EntityModel<?> model;

        Subject(LivingEntity entity, EntityModel<?> model) {
            this.entity = entity;
            this.model = model;
        }
    }
}
