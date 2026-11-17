package com.bslx.XueXiMod.shu.technique.release.clone.core;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.UUID;

final class CapturedBoard {
    final LinkedHashMap<UUID, CapturedDeployment> deployments = new LinkedHashMap<>();
    final LinkedHashMap<UUID, CapturedResponseTicket> tickets = new LinkedHashMap<>();
    final ArrayDeque<UUID> ticketOrder = new ArrayDeque<>();
    final LinkedHashMap<UUID, Integer> detachedByOwner = new LinkedHashMap<>();
    int detachedUnits;

    boolean idle() {
        return this.deployments.isEmpty() && this.ticketOrder.isEmpty();
    }

    void clear() {
        this.deployments.clear();
        this.tickets.clear();
        this.ticketOrder.clear();
        this.detachedByOwner.clear();
        this.detachedUnits = 0;
    }
}
