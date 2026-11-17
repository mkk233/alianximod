package com.bslx.XueXiMod.client.action.skinned;

import com.bslx.XueXiMod.client.action.RenderPoseScope;
import com.bslx.XueXiMod.client.action.skinned.attachments.AttachmentSupport;

import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.LivingEntity;

/**
 * 现有视角栈的类型化读取门。没有第二套 ThreadLocal。
 * 帧还没在身体矩阵处装好、身份不符或不是 SKINNED 时返回空。
 */
public final class SkinRenderAccess {
    private SkinRenderAccess() {
    }

    public static ImmutableSkinFrame currentThirdPerson(LivingEntity player, EntityModel<?> model) {
        SkinSession session = matching(player);
        if (session == null || model == null || session.model() != model || session.frame() == null) {
            return null;
        }
        return session.frame();
    }

    public static ImmutableSkinFrame currentThirdPerson(LivingEntity player) {
        SkinSession session = matching(player);
        if (session == null || session.frame() == null) {
            return null;
        }
        return session.frame();
    }

    public static AttachmentSupport attachmentSupport(LivingEntity player) {
        SkinSession session = SkinSession.top();
        if (session == null || player == null || session.player() != player || session.view() != RenderPoseScope.View.THIRD_PERSON) {
            return null;
        }
        return session.support();
    }

    private static SkinSession matching(LivingEntity player) {
        SkinSession session = SkinSession.top();
        if (session == null || player == null || session.player() != player || session.view() != RenderPoseScope.View.THIRD_PERSON) {
            return null;
        }
        if (session.plan() == null || session.plan().mode() != RenderPlan.Mode.SKINNED) {
            return null;
        }
        return session;
    }
}
