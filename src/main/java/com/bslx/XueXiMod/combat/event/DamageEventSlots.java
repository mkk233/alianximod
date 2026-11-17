package com.bslx.XueXiMod.combat.event;

import java.util.ArrayDeque;

/**
 * 一次 hurt 包装对应一个槽。事件只写当前槽，退出只弹出当前槽。
 * 不用队列长度和进入深度互相推断归属。
 */
public final class DamageEventSlots<NotedEvent> {
    private final ArrayDeque<Slot<NotedEvent>> openSlots = new ArrayDeque<>();

    public void push() {
        openSlots.addFirst(new Slot<>());
    }

    /** 没有打开的槽时丢弃，避免包装外的事件挂到下一次调用上。 */
    public void writeCurrent(NotedEvent notedEvent) {
        Slot<NotedEvent> current = openSlots.peekFirst();
        if (current == null || notedEvent == null) {
            return;
        }
        current.notedEvent = notedEvent;
    }

    /**
     * 弹出当前槽。槽是空的时返回 null。栈已经空时也返回 null，不改更外层。
     */
    public NotedEvent popSeal() {
        Slot<NotedEvent> current = openSlots.pollFirst();
        if (current == null) {
            return null;
        }
        return current.notedEvent;
    }

    /** 弹出当前槽并丢掉里面的事件，不把更外层的事件一起丢掉。 */
    public void popDiscard() {
        openSlots.pollFirst();
    }

    public int depth() {
        return openSlots.size();
    }

    private static final class Slot<NotedEvent> {
        private NotedEvent notedEvent;
    }
}
