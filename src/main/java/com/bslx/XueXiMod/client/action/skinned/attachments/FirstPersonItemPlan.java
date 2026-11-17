package com.bslx.XueXiMod.client.action.skinned.attachments;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.client.action.skinned.ImmutableSkinFrame;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 一次第一人称物品准备。主路只读是否支持；绘制矩阵和物品快照留在本包。
 * 不支持时主路整手走原版，这里的绘制函数不会再补画。
 */
public final class FirstPersonItemPlan {
    private final boolean supported;
    private final String reason;
    private final ImmutableSkinFrame frame;
    private final AbstractClientPlayer player;
    private final InteractionHand hand;
    private final ItemStack stack;
    private final OpenMatrix4f itemMatrix;
    private final ItemDisplayContext displayContext;
    private final boolean leftHand;

    private FirstPersonItemPlan(
            boolean supported,
            String reason,
            ImmutableSkinFrame frame,
            AbstractClientPlayer player,
            InteractionHand hand,
            ItemStack stack,
            OpenMatrix4f itemMatrix,
            ItemDisplayContext displayContext,
            boolean leftHand) {
        this.supported = supported;
        this.reason = reason;
        this.frame = frame;
        this.player = player;
        this.hand = hand;
        this.stack = stack;
        this.itemMatrix = itemMatrix;
        this.displayContext = displayContext;
        this.leftHand = leftHand;
    }

    static FirstPersonItemPlan blocked(String reason) {
        return new FirstPersonItemPlan(false, reason, null, null, null, ItemStack.EMPTY, null, null, false);
    }

    static FirstPersonItemPlan ready(
            ImmutableSkinFrame frame,
            AbstractClientPlayer player,
            InteractionHand hand,
            ItemStack stack,
            OpenMatrix4f itemMatrix,
            boolean leftHand) {
        ItemDisplayContext displayContext = leftHand
                ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        return new FirstPersonItemPlan(true, "item-ready", frame, player, hand, stack, itemMatrix, displayContext, leftHand);
    }

    public boolean supported() {
        return this.supported;
    }

    public String reason() {
        return this.reason;
    }

    ImmutableSkinFrame frame() {
        return this.frame;
    }

    AbstractClientPlayer player() {
        return this.player;
    }

    InteractionHand hand() {
        return this.hand;
    }

    ItemStack stack() {
        return this.stack;
    }

    OpenMatrix4f itemMatrix() {
        return this.itemMatrix == null ? null : new OpenMatrix4f(this.itemMatrix);
    }

    ItemDisplayContext displayContext() {
        return this.displayContext;
    }

    boolean leftHand() {
        return this.leftHand;
    }
}
