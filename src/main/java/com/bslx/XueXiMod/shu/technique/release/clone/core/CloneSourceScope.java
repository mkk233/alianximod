package com.bslx.XueXiMod.shu.technique.release.clone.core;

import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class CloneSourceScope implements AutoCloseable {
    private static final Object GATE = new Object();
    @Nullable
    private static Frame current;

    private final Frame opened;
    private boolean closed;

    private CloneSourceScope(Frame opened) {
        this.opened = opened;
    }

    public enum Origin {
        PLAYER_MANUAL,
        CLONE_RESPONSE
    }

    public static CloneSourceScope open(MinecraftServer server, Origin origin, UUID rootEventId, int depth) {
        if (server == null || !server.isSameThread()) {
            throw new IllegalStateException("分身来源作用域只能在所属服务器线程打开");
        }
        if (origin == null || rootEventId == null || depth < 0) {
            throw new IllegalArgumentException("分身来源作用域不完整");
        }
        synchronized (GATE) {
            Frame opened = new Frame(current, server, origin, rootEventId, depth);
            current = opened;
            return new CloneSourceScope(opened);
        }
    }

    @Override
    public void close() {
        synchronized (GATE) {
            if (this.closed) {
                return;
            }
            this.closed = true;
            if (current == this.opened) {
                current = this.opened.previous;
                return;
            }
            Frame cursor = current;
            Frame followed = null;
            while (cursor != null) {
                if (cursor == this.opened) {
                    if (followed == null) {
                        current = cursor.previous;
                    } else {
                        followed.previous = cursor.previous;
                    }
                    return;
                }
                followed = cursor;
                cursor = cursor.previous;
            }
        }
    }

    @Nullable
    public static Origin origin() {
        Frame frame = currentFrame();
        return frame == null ? null : frame.origin;
    }

    public static int depth() {
        Frame frame = currentFrame();
        return frame == null ? 0 : frame.depth;
    }

    @Nullable
    public static UUID rootEventId() {
        Frame frame = currentFrame();
        return frame == null ? null : frame.rootEventId;
    }

    public static boolean blocksResponse() {
        Frame frame = currentFrame();
        return frame != null && (frame.origin == Origin.CLONE_RESPONSE || frame.depth > 0);
    }

    @Nullable
    private static Frame currentFrame() {
        synchronized (GATE) {
            Frame frame = current;
            if (frame == null) {
                return null;
            }
            if (!frame.server.isSameThread()) {
                return null;
            }
            return frame;
        }
    }

    private static final class Frame {
        @Nullable
        private Frame previous;
        private final MinecraftServer server;
        private final Origin origin;
        private final UUID rootEventId;
        private final int depth;

        private Frame(@Nullable Frame previous, MinecraftServer server, Origin origin, UUID rootEventId, int depth) {
            this.previous = previous;
            this.server = server;
            this.origin = origin;
            this.rootEventId = rootEventId;
            this.depth = depth;
        }
    }
}
