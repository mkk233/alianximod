package com.bslx.XueXiMod.client.action.spacecut;

import java.util.ArrayDeque;

import net.minecraft.world.entity.player.Player;

/** 只在这一次玩家绘制或第一人称手绘制期间持有左腰右拔帧。结束、换手和异常都弹出。 */
public final class SpaceCutScope {
    private static final ThreadLocal<ArrayDeque<SpaceCutFrame>> FRAMES = ThreadLocal.withInitial(ArrayDeque::new);

    private SpaceCutScope() {
    }

    public static boolean push(Player player, float partialTick) {
        SpaceCutFrame frame = SpaceCutSampler.sample(player, partialTick);
        if (frame == null) {
            return false;
        }
        FRAMES.get().push(frame);
        return true;
    }

    public static void pop() {
        ArrayDeque<SpaceCutFrame> frames = FRAMES.get();
        if (!frames.isEmpty()) {
            frames.pop();
        }
        if (frames.isEmpty()) {
            FRAMES.remove();
        }
    }

    public static SpaceCutFrame current() {
        ArrayDeque<SpaceCutFrame> frames = FRAMES.get();
        return frames.isEmpty() ? null : frames.peek();
    }

    public static boolean activeFor(Player player) {
        SpaceCutFrame frame = current();
        return frame != null && player != null && player.getUUID().equals(frame.ownerId());
    }
}
