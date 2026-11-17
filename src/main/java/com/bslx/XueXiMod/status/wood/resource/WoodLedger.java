package com.bslx.XueXiMod.status.wood.resource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * 花瓣与岁华的单账本。包内可见，玩法入口是 {@link WoodResourceService}。
 * 只有 tick 里的到期循环会调用 acceptNatural。丢弃、死亡、登出、卸载和重启都不入账。
 * 本轮生产路径在加载时丢弃地面且不发奖；{@code keepGround} 只给校验坏存档，服务端存档加载固定传 false。
 */
final class WoodLedger {
    private boolean sealed;
    private LedgerImage sealedImage;
    private long nextSerial = 1L;
    private boolean applyingReady;
    private boolean readyViolated;
    private final Map<Long, BatchRecord> batches = new HashMap<>();
    private final Map<UUID, RootRecord> roots = new HashMap<>();
    private final Map<UUID, FragmentRecord> fragments = new HashMap<>();
    private final Map<UUID, SuihuaAccount> accounts = new HashMap<>();
    private final Map<UUID, OpenTicket> openTickets = new HashMap<>();
    private final Map<UUID, Integer> openTicketsByOwner = new HashMap<>();
    private final Set<UUID> dirtyOwners = new LinkedHashSet<>();

    private WoodLedger() {
    }

    static WoodLedger createEmpty() {
        return new WoodLedger();
    }

    static WoodLedger sealedEmpty() {
        WoodLedger ledger = new WoodLedger();
        ledger.sealed = true;
        ledger.sealedImage = new LedgerImage(Integer.MIN_VALUE, 0L, List.of(), List.of(), List.of(), List.of());
        return ledger;
    }

    boolean writable() {
        return !sealed;
    }

    ProductionTicket beginTicket(UUID ownerId, ResourceOrigin origin, int tickCount) {
        if (blockMutation() != null || ownerId == null || origin == null) {
            return null;
        }
        int openCount = openTicketsByOwner.getOrDefault(ownerId, 0);
        if (openCount >= WoodLimits.OPEN_TICKETS_PER_OWNER) {
            return null;
        }
        if (nextSerial <= 0L || nextSerial == Long.MAX_VALUE) {
            return null;
        }
        long serial = nextSerial;
        nextSerial++;
        UUID token = UUID.randomUUID();
        OpenTicket openTicket = new OpenTicket(token, serial, ownerId, origin, tickCount);
        openTickets.put(token, openTicket);
        openTicketsByOwner.put(ownerId, openCount + 1);
        return ProductionTicket.issue(token, ownerId, origin, serial, tickCount);
    }

    boolean burnIfOpen(ProductionTicket ticket) {
        if (blockMutation() != null || ticket == null) {
            return false;
        }
        OpenTicket openTicket = matchingOpen(ticket);
        if (openTicket == null) {
            return false;
        }
        releaseOpen(openTicket);
        return true;
    }

    List<UUID> discardOpenTickets() {
        if (blockMutation() != null) {
            return List.of();
        }
        List<UUID> tokens = new ArrayList<>(openTickets.keySet());
        openTickets.clear();
        openTicketsByOwner.clear();
        return List.copyOf(tokens);
    }

    CreateResult publishTicket(ProductionTicket ticket, int tickCount, String batchDimension,
                               long createdGameTime, List<LotDraft> lots) {
        WoodReject blocked = blockMutation();
        if (blocked != null) {
            return CreateResult.rejected(blocked, ticket == null ? -1L : ticket.serial());
        }
        if (ticket == null) {
            return CreateResult.rejected(WoodReject.TICKET);
        }
        OpenTicket openTicket = matchingOpen(ticket);
        if (openTicket == null) {
            return CreateResult.rejected(WoodReject.TICKET, ticket.serial());
        }
        if (openTicket.issuedTick != tickCount) {
            releaseOpen(openTicket);
            return CreateResult.rejected(WoodReject.TICKET, openTicket.serial);
        }
        releaseOpen(openTicket);
        if (batchDimension == null || batchDimension.isBlank()) {
            return CreateResult.rejected(WoodReject.VALIDATION, openTicket.serial);
        }
        List<LotDraft> drafts;
        try {
            drafts = lots == null ? null : List.copyOf(lots);
        } catch (NullPointerException exception) {
            return CreateResult.rejected(WoodReject.VALIDATION, openTicket.serial);
        }
        if (drafts == null) {
            return CreateResult.rejected(WoodReject.VALIDATION, openTicket.serial);
        }
        if (drafts.size() > WoodLimits.ROOTS_PER_BATCH) {
            return CreateResult.rejected(WoodReject.CAPACITY, openTicket.serial);
        }
        long potentialSum = 0L;
        for (LotDraft draft : drafts) {
            WoodReject draftReject = validateDraft(draft);
            if (draftReject != null) {
                return CreateResult.rejected(draftReject, openTicket.serial);
            }
            if (openTicket.origin == ResourceOrigin.NORMAL_BLOOM) {
                potentialSum += draft.potentialUnits;
                if (potentialSum > WoodLimits.BATCH_POTENTIAL) {
                    return CreateResult.rejected(WoodReject.CAPACITY, openTicket.serial);
                }
            }
        }
        if (drafts.isEmpty()) {
            return new CreateResult(true, WoodReject.NONE, null, openTicket.serial, List.of());
        }
        if (batchesOf(openTicket.ownerId) >= WoodLimits.BATCHES_PER_OWNER) {
            return CreateResult.rejected(WoodReject.CAPACITY, openTicket.serial);
        }
        if ((long) fragmentsOf(openTicket.ownerId) + drafts.size() > WoodLimits.FRAGMENTS_PER_OWNER) {
            return CreateResult.rejected(WoodReject.CAPACITY, openTicket.serial);
        }
        if ((long) fragments.size() + drafts.size() > WoodLimits.FRAGMENTS_PER_SERVER) {
            return CreateResult.rejected(WoodReject.CAPACITY, openTicket.serial);
        }
        if (batches.containsKey(openTicket.serial)) {
            return CreateResult.rejected(WoodReject.TICKET, openTicket.serial);
        }
        UUID batchId = UUID.randomUUID();
        BatchRecord batch = new BatchRecord(openTicket.serial, batchId, openTicket.ownerId, openTicket.origin,
                batchDimension, createdGameTime);
        List<RootRecord> createdRoots = new ArrayList<>();
        List<FragmentRecord> createdFragments = new ArrayList<>();
        List<FragmentHandle> handles = new ArrayList<>();
        int rootSerial = 1;
        for (LotDraft draft : drafts) {
            int storedPotential = openTicket.origin == ResourceOrigin.ULTIMATE ? 0 : draft.potentialUnits;
            UUID rootId = UUID.randomUUID();
            UUID fragmentId = UUID.randomUUID();
            createdRoots.add(new RootRecord(rootId, openTicket.serial, rootSerial, draft.flower,
                    draft.effectiveUnits, storedPotential, draft.expireGameTime));
            FragmentRecord fragment = new FragmentRecord(fragmentId, rootId, draft.dimensionId,
                    draft.coordX, draft.coordY, draft.coordZ);
            fragment.lowInclusive = 0;
            fragment.highExclusive = draft.effectiveUnits;
            fragment.revision = 1;
            createdFragments.add(fragment);
            handles.add(new FragmentHandle(fragmentId, 1));
            rootSerial++;
        }
        batches.put(batch.serial, batch);
        for (RootRecord root : createdRoots) {
            roots.put(root.rootId, root);
        }
        for (FragmentRecord fragment : createdFragments) {
            fragments.put(fragment.fragmentId, fragment);
        }
        return new CreateResult(true, WoodReject.NONE, batchId, openTicket.serial, handles);
    }

    GroundQueryResult query(UUID ownerId, String dimensionId, double lowerX, double lowerY, double lowerZ,
                            double upperX, double upperY, double upperZ, int limit) {
        if (sealed) {
            return GroundQueryResult.rejected(WoodReject.SEALED);
        }
        if (ownerId == null || dimensionId == null || dimensionId.isBlank()) {
            return GroundQueryResult.rejected(WoodReject.VALIDATION);
        }
        if (!Double.isFinite(lowerX) || !Double.isFinite(lowerY) || !Double.isFinite(lowerZ)
                || !Double.isFinite(upperX) || !Double.isFinite(upperY) || !Double.isFinite(upperZ)) {
            return GroundQueryResult.rejected(WoodReject.VALIDATION);
        }
        if (limit < 1) {
            return GroundQueryResult.rejected(WoodReject.LIMIT);
        }
        List<FragmentRecord> matched = new ArrayList<>();
        for (FragmentRecord fragment : fragments.values()) {
            if (!dimensionId.equals(fragment.dimensionId)) {
                continue;
            }
            if (fragment.coordX < lowerX || fragment.coordX >= upperX
                    || fragment.coordY < lowerY || fragment.coordY >= upperY
                    || fragment.coordZ < lowerZ || fragment.coordZ >= upperZ) {
                continue;
            }
            RootRecord root = roots.get(fragment.rootId);
            BatchRecord batch = root == null ? null : batches.get(root.batchSerial);
            if (batch == null || !ownerId.equals(batch.ownerId)) {
                continue;
            }
            matched.add(fragment);
        }
        matched.sort(this::compareFragment);
        List<GroundFragmentView> views = new ArrayList<>();
        for (FragmentRecord fragment : matched) {
            GroundFragmentView view = viewOf(fragment);
            if (view != null) {
                views.add(view);
            }
        }
        boolean truncated = views.size() > limit;
        List<GroundFragmentView> returned = truncated ? views.subList(0, limit) : views;
        return new GroundQueryResult(true, WoodReject.NONE, truncated, returned);
    }

    Optional<GroundConsumeReceipt> consume(UUID actorId, List<FragmentTake> takes,
                                           Predicate<GroundConsumePreview> ready) {
        if (blockMutation() != null || actorId == null || ready == null || takes == null) {
            return Optional.empty();
        }
        if (takes.isEmpty() || takes.size() > WoodLimits.FRAGMENTS_PER_OWNER) {
            return Optional.empty();
        }
        List<FragmentTake> copies;
        try {
            copies = List.copyOf(takes);
        } catch (NullPointerException exception) {
            return Optional.empty();
        }
        Set<UUID> seen = new HashSet<>();
        for (FragmentTake take : copies) {
            if (take == null || take.fragmentId() == null || take.units() <= 0 || !seen.add(take.fragmentId())) {
                return Optional.empty();
            }
        }
        List<TakePlan> plans = new ArrayList<>();
        for (FragmentTake take : copies) {
            TakePlan plan = planTake(actorId, take);
            if (plan == null) {
                return Optional.empty();
            }
            plans.add(plan);
        }
        GroundConsumePreview preview = previewOf(actorId, plans);
        applyingReady = true;
        readyViolated = false;
        boolean accepted;
        try {
            accepted = ready.test(preview);
        } catch (RuntimeException exception) {
            return Optional.empty();
        } finally {
            applyingReady = false;
        }
        if (!accepted || readyViolated) {
            return Optional.empty();
        }
        for (TakePlan plan : plans) {
            if (!plan.matchesLive()) {
                return Optional.empty();
            }
        }
        for (TakePlan plan : plans) {
            plan.apply();
        }
        cleanupEmpty();
        return Optional.of(new GroundConsumeReceipt(actorId, preview.potentialUnits(), preview.pieces()));
    }

    SplitResult split(UUID actorId, UUID fragmentId, int revision, int leftUnits) {
        WoodReject blocked = blockMutation();
        if (blocked != null) {
            return SplitResult.rejected(blocked);
        }
        if (actorId == null || fragmentId == null) {
            return SplitResult.rejected(WoodReject.VALIDATION);
        }
        FragmentRecord fragment = fragments.get(fragmentId);
        if (fragment == null) {
            return SplitResult.rejected(WoodReject.MISSING);
        }
        if (fragment.revision != revision) {
            return SplitResult.rejected(WoodReject.REVISION);
        }
        int length = fragment.highExclusive - fragment.lowInclusive;
        if (leftUnits <= 0 || leftUnits >= length) {
            return SplitResult.rejected(WoodReject.VALIDATION);
        }
        if (fragment.revision == Integer.MAX_VALUE) {
            return SplitResult.rejected(WoodReject.REVISION);
        }
        RootRecord root = roots.get(fragment.rootId);
        BatchRecord batch = root == null ? null : batches.get(root.batchSerial);
        if (root == null || batch == null) {
            return SplitResult.rejected(WoodReject.MISSING);
        }
        if (!actorId.equals(batch.ownerId)) {
            return SplitResult.rejected(WoodReject.OWNER);
        }
        if (fragmentsOf(actorId) >= WoodLimits.FRAGMENTS_PER_OWNER
                || fragments.size() >= WoodLimits.FRAGMENTS_PER_SERVER) {
            return SplitResult.rejected(WoodReject.CAPACITY);
        }
        UUID createdId = UUID.randomUUID();
        FragmentRecord created = new FragmentRecord(createdId, fragment.rootId, fragment.dimensionId,
                fragment.coordX, fragment.coordY, fragment.coordZ);
        created.lowInclusive = fragment.lowInclusive + leftUnits;
        created.highExclusive = fragment.highExclusive;
        created.revision = 1;
        fragment.highExclusive = fragment.lowInclusive + leftUnits;
        fragment.revision++;
        fragments.put(createdId, created);
        return new SplitResult(true, WoodReject.NONE,
                new FragmentHandle(fragment.fragmentId, fragment.revision),
                new FragmentHandle(createdId, 1));
    }

    RemoveResult discard(UUID actorId, UUID fragmentId, int revision, RemovalReason reason) {
        WoodReject blocked = blockMutation();
        if (blocked != null) {
            return RemoveResult.rejected(blocked);
        }
        if (reason != RemovalReason.EXTERNAL) {
            return RemoveResult.rejected(WoodReject.REASON);
        }
        if (actorId == null || fragmentId == null) {
            return RemoveResult.rejected(WoodReject.VALIDATION);
        }
        FragmentRecord fragment = fragments.get(fragmentId);
        if (fragment == null) {
            return RemoveResult.rejected(WoodReject.MISSING);
        }
        if (fragment.revision != revision) {
            return RemoveResult.rejected(WoodReject.REVISION);
        }
        RootRecord root = roots.get(fragment.rootId);
        BatchRecord batch = root == null ? null : batches.get(root.batchSerial);
        if (batch == null || !actorId.equals(batch.ownerId)) {
            return RemoveResult.rejected(batch == null ? WoodReject.MISSING : WoodReject.OWNER);
        }
        fragments.remove(fragmentId);
        cleanupEmpty();
        return RemoveResult.success();
    }

    SuihuaSnapshot snapshot(UUID ownerId) {
        if (ownerId == null) {
            return SuihuaSnapshot.empty();
        }
        SuihuaAccount account = accounts.get(ownerId);
        if (account == null) {
            return SuihuaSnapshot.empty();
        }
        return account.snapshot();
    }

    Optional<SuihuaSnapshot> consumeFull(UUID ownerId, Predicate<SuihuaSnapshot> ready) {
        if (blockMutation() != null || ownerId == null || ready == null) {
            return Optional.empty();
        }
        SuihuaAccount account = accounts.get(ownerId);
        if (account == null || account.total() != WoodLimits.FULL_UNITS) {
            return Optional.empty();
        }
        SuihuaSnapshot seen = account.snapshot();
        if (seen.total() != WoodLimits.FULL_UNITS) {
            return Optional.empty();
        }
        applyingReady = true;
        readyViolated = false;
        boolean accepted;
        try {
            accepted = ready.test(seen);
        } catch (RuntimeException exception) {
            return Optional.empty();
        } finally {
            applyingReady = false;
        }
        if (!accepted || readyViolated) {
            return Optional.empty();
        }
        SuihuaAccount current = accounts.get(ownerId);
        if (current == null || !current.matches(seen)) {
            return Optional.empty();
        }
        accounts.remove(ownerId);
        dirtyOwners.add(ownerId);
        return Optional.of(seen);
    }

    TickReport tick(WorldProbe probe, int tickCount) {
        if (blockMutation() != null) {
            return TickReport.idle();
        }
        List<UUID> expiredTickets = sweepTickets(tickCount);
        List<UUID> dangling = new ArrayList<>();
        for (FragmentRecord fragment : fragments.values()) {
            RootRecord root = roots.get(fragment.rootId);
            BatchRecord batch = root == null ? null : batches.get(root.batchSerial);
            if (root == null || batch == null || !WoodInterval.addressable(fragment.coordX, fragment.coordY, fragment.coordZ)) {
                dangling.add(fragment.fragmentId);
            }
        }
        boolean changed = eraseIds(dangling);
        if (probe == null) {
            return new TickReport(changed, dangling.size(), 0, expiredTickets);
        }
        List<FragmentRecord> due = new ArrayList<>();
        for (FragmentRecord fragment : fragments.values()) {
            RootRecord root = roots.get(fragment.rootId);
            BatchRecord batch = root == null ? null : batches.get(root.batchSerial);
            if (root == null || batch == null) {
                continue;
            }
            if (!probe.ownerOnline(batch.ownerId) || !probe.ownerAlive(batch.ownerId)
                    || !probe.ownerInDimension(batch.ownerId, fragment.dimensionId)) {
                continue;
            }
            Integer blockX = WoodInterval.horizontalBlock(fragment.coordX);
            Integer blockZ = WoodInterval.horizontalBlock(fragment.coordZ);
            if (blockX == null || blockZ == null) {
                continue;
            }
            if (!probe.chunkLoaded(fragment.dimensionId, WoodInterval.chunkCoordinate(blockX),
                    WoodInterval.chunkCoordinate(blockZ))) {
                continue;
            }
            if (probe.gameTime(fragment.dimensionId) < root.expireGameTime) {
                continue;
            }
            due.add(fragment);
        }
        due.sort(this::compareFragment);
        long awardedSum = 0L;
        List<UUID> finished = new ArrayList<>();
        for (FragmentRecord fragment : due) {
            if (!fragments.containsKey(fragment.fragmentId)) {
                continue;
            }
            RootRecord root = roots.get(fragment.rootId);
            BatchRecord batch = root == null ? null : batches.get(root.batchSerial);
            if (root == null || batch == null) {
                continue;
            }
            int potentialUnits = WoodInterval.potential(root.potentialUnits, root.effectiveUnits,
                    fragment.lowInclusive, fragment.highExclusive);
            if (potentialUnits > 0) {
                SuihuaAccount account = accounts.get(batch.ownerId);
                if (account == null) {
                    account = new SuihuaAccount();
                    accounts.put(batch.ownerId, account);
                }
                int accepted = account.acceptNatural(root.flower, potentialUnits);
                if (accepted > 0) {
                    dirtyOwners.add(batch.ownerId);
                    awardedSum += accepted;
                }
                if (account.isZero()) {
                    accounts.remove(batch.ownerId);
                }
            }
            finished.add(fragment.fragmentId);
        }
        changed = eraseIds(finished) || changed;
        int awardedUnits = awardedSum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) awardedSum;
        return new TickReport(changed, dangling.size() + finished.size(), awardedUnits, expiredTickets);
    }

    boolean clearDeath(UUID ownerId) {
        if (blockMutation() != null || ownerId == null) {
            return false;
        }
        boolean changed = eraseIds(fragmentsOfOwner(ownerId));
        SuihuaAccount account = accounts.remove(ownerId);
        if (account != null && !account.isZero()) {
            dirtyOwners.add(ownerId);
            changed = true;
        }
        return changed;
    }

    boolean clearGround(UUID ownerId) {
        if (blockMutation() != null || ownerId == null) {
            return false;
        }
        return eraseIds(fragmentsOfOwner(ownerId));
    }

    boolean clearChunk(String dimensionId, int chunkX, int chunkZ) {
        if (blockMutation() != null || dimensionId == null) {
            return false;
        }
        List<UUID> doomed = new ArrayList<>();
        for (FragmentRecord fragment : fragments.values()) {
            if (!dimensionId.equals(fragment.dimensionId)) {
                continue;
            }
            Integer blockX = WoodInterval.horizontalBlock(fragment.coordX);
            Integer blockZ = WoodInterval.horizontalBlock(fragment.coordZ);
            if (blockX == null || blockZ == null) {
                doomed.add(fragment.fragmentId);
                continue;
            }
            if (WoodInterval.chunkCoordinate(blockX) == chunkX && WoodInterval.chunkCoordinate(blockZ) == chunkZ) {
                doomed.add(fragment.fragmentId);
            }
        }
        return eraseIds(doomed);
    }

    boolean clearDimension(String dimensionId) {
        if (blockMutation() != null || dimensionId == null) {
            return false;
        }
        List<UUID> doomed = new ArrayList<>();
        for (FragmentRecord fragment : fragments.values()) {
            if (dimensionId.equals(fragment.dimensionId)) {
                doomed.add(fragment.fragmentId);
            }
        }
        return eraseIds(doomed);
    }

    List<UUID> peekDirty() {
        return List.copyOf(dirtyOwners);
    }

    List<UUID> drainDirty() {
        if (blockMutation() != null) {
            return List.of();
        }
        List<UUID> owners = List.copyOf(dirtyOwners);
        dirtyOwners.clear();
        return owners;
    }

    void forgetDirty(List<UUID> owners) {
        if (owners == null) {
            return;
        }
        for (UUID ownerId : owners) {
            dirtyOwners.remove(ownerId);
        }
    }

    LedgerImage exportImage() {
        if (sealed) {
            if (sealedImage == null) {
                return new LedgerImage(Integer.MIN_VALUE, 0L, List.of(), List.of(), List.of(), List.of());
            }
            return sealedImage.copy();
        }
        List<OwnerImage> ownerImages = new ArrayList<>();
        for (Map.Entry<UUID, SuihuaAccount> entry : accounts.entrySet()) {
            SuihuaSnapshot snapshot = entry.getValue().snapshot();
            if (snapshot.total() <= 0) {
                continue;
            }
            ownerImages.add(new OwnerImage(entry.getKey(), snapshot.peach(), snapshot.bamboo(), snapshot.wisteria(),
                    snapshot.plum(), snapshot.orchid(), snapshot.higan(), snapshot.rose()));
        }
        ownerImages.sort((left, right) -> compareUuid(left.ownerId, right.ownerId));
        List<BatchImage> batchImages = new ArrayList<>();
        for (BatchRecord batch : batches.values()) {
            batchImages.add(new BatchImage(batch.serial, batch.batchId, batch.ownerId, batch.origin.name(),
                    batch.dimensionId, batch.createdGameTime));
        }
        batchImages.sort(Comparator.comparingLong(batch -> batch.serial));
        List<RootImage> rootImages = new ArrayList<>();
        for (RootRecord root : roots.values()) {
            rootImages.add(new RootImage(root.rootId, root.batchSerial, root.rootSerial, root.flower.name(),
                    root.effectiveUnits, root.potentialUnits, root.expireGameTime));
        }
        rootImages.sort(Comparator.comparingLong((RootImage root) -> root.batchSerial).thenComparingInt(root -> root.rootSerial));
        List<FragmentRecord> ordered = new ArrayList<>(fragments.values());
        ordered.sort(this::compareFragment);
        List<FragmentImage> fragmentImages = new ArrayList<>();
        for (FragmentRecord fragment : ordered) {
            RootRecord root = roots.get(fragment.rootId);
            if (root == null) {
                continue;
            }
            fragmentImages.add(new FragmentImage(fragment.fragmentId, fragment.rootId, root.batchSerial, root.rootSerial,
                    fragment.dimensionId, fragment.coordX, fragment.coordY, fragment.coordZ,
                    fragment.lowInclusive, fragment.highExclusive, fragment.revision));
        }
        return new LedgerImage(WoodLimits.FORMAT_VERSION, nextSerial, ownerImages, batchImages, rootImages, fragmentImages);
    }

    static LoadReport restore(LedgerImage image, boolean keepGround) {
        return restore(image, keepGround, image == null ? Integer.MIN_VALUE : image.formatVersion);
    }

    static LoadReport restore(LedgerImage image, boolean keepGround, int sourceVersion) {
        boolean readable = image != null && (image.formatVersion == WoodLimits.FORMAT_VERSION
                || image.formatVersion == WoodLimits.PREVIOUS_FORMAT_VERSION);
        if (!readable) {
            WoodLedger sealedLedger = new WoodLedger();
            sealedLedger.sealed = true;
            sealedLedger.sealedImage = image == null
                    ? new LedgerImage(Integer.MIN_VALUE, 0L, List.of(), List.of(), List.of(), List.of())
                    : image.copy();
            return new LoadReport(sealedLedger, true, false, 0, 0, 0, 0, 0);
        }
        WoodLedger ledger = new WoodLedger();
        int droppedBatches = 0;
        int droppedRoots = 0;
        int droppedFragments = 0;
        int droppedOwners = 0;
        boolean repaired = false;
        long highestSeen = 0L;
        for (BatchImage batch : image.batches) {
            if (batch != null && batch.serial > highestSeen) {
                highestSeen = batch.serial;
            }
        }
        Set<UUID> seenOwners = new HashSet<>();
        for (OwnerImage owner : image.owners) {
            if (owner == null || owner.ownerId == null) {
                droppedOwners++;
                continue;
            }
            int rose = sourceVersion == WoodLimits.PREVIOUS_FORMAT_VERSION ? 0 : owner.rose;
            SuihuaSnapshot repairedSnapshot = SuihuaSnapshot.repairOrNull(owner.peach, owner.bamboo, owner.wisteria,
                    owner.plum, owner.orchid, owner.higan, rose);
            if (repairedSnapshot == null) {
                droppedOwners++;
                continue;
            }
            if (!seenOwners.add(owner.ownerId)) {
                droppedOwners++;
                continue;
            }
            if (repairedSnapshot.peach() != owner.peach || repairedSnapshot.bamboo() != owner.bamboo
                    || repairedSnapshot.wisteria() != owner.wisteria || repairedSnapshot.plum() != owner.plum
                    || repairedSnapshot.orchid() != owner.orchid || repairedSnapshot.higan() != owner.higan
                    || repairedSnapshot.rose() != rose) {
                repaired = true;
            }
            if (repairedSnapshot.total() > 0) {
                SuihuaAccount account = new SuihuaAccount();
                account.load(repairedSnapshot);
                ledger.accounts.put(owner.ownerId, account);
            }
        }
        Map<Long, List<BatchImage>> batchesBySerial = new HashMap<>();
        for (BatchImage batch : image.batches) {
            if (!validBatch(batch)) {
                droppedBatches++;
                continue;
            }
            batchesBySerial.computeIfAbsent(batch.serial, ignored -> new ArrayList<>()).add(batch);
        }
        Map<Long, BatchRecord> keptBatches = new HashMap<>();
        for (Map.Entry<Long, List<BatchImage>> entry : batchesBySerial.entrySet()) {
            List<BatchImage> group = entry.getValue();
            if (group.size() != 1) {
                droppedBatches += group.size();
                continue;
            }
            BatchImage batch = group.get(0);
            keptBatches.put(batch.serial, new BatchRecord(batch.serial, batch.batchId, batch.ownerId,
                    ResourceOrigin.fromName(batch.originName), batch.dimensionId, batch.createdGameTime));
        }
        Map<UUID, Integer> rootIdCounts = new HashMap<>();
        List<RootImage> rootCandidates = new ArrayList<>();
        for (RootImage root : image.roots) {
            if (root == null || root.rootId == null || root.rootSerial <= 0 || root.effectiveUnits <= 0
                    || root.potentialUnits < 0 || root.expireGameTime < 0 || FlowerKind.fromName(root.flowerName) == null
                    || !keptBatches.containsKey(root.batchSerial)) {
                droppedRoots++;
                continue;
            }
            rootCandidates.add(root);
            rootIdCounts.merge(root.rootId, 1, Integer::sum);
        }
        Map<Long, List<RootRecord>> rootsByBatch = new HashMap<>();
        for (RootImage root : rootCandidates) {
            if (rootIdCounts.getOrDefault(root.rootId, 0) != 1) {
                droppedRoots++;
                continue;
            }
            BatchRecord batch = keptBatches.get(root.batchSerial);
            int storedPotential = batch.origin == ResourceOrigin.ULTIMATE ? 0 : root.potentialUnits;
            if (storedPotential != root.potentialUnits) {
                repaired = true;
            }
            RootRecord record = new RootRecord(root.rootId, root.batchSerial, root.rootSerial,
                    FlowerKind.fromName(root.flowerName), root.effectiveUnits, storedPotential, root.expireGameTime);
            rootsByBatch.computeIfAbsent(root.batchSerial, ignored -> new ArrayList<>()).add(record);
        }
        Map<UUID, RootRecord> keptRoots = new HashMap<>();
        Iterator<Map.Entry<Long, BatchRecord>> batchIterator = keptBatches.entrySet().iterator();
        while (batchIterator.hasNext()) {
            Map.Entry<Long, BatchRecord> entry = batchIterator.next();
            List<RootRecord> grouped = rootsByBatch.getOrDefault(entry.getKey(), List.of());
            if (grouped.isEmpty()) {
                batchIterator.remove();
                continue;
            }
            if (grouped.size() > WoodLimits.ROOTS_PER_BATCH) {
                droppedRoots += grouped.size();
                droppedBatches++;
                batchIterator.remove();
                continue;
            }
            Set<Integer> rootSerials = new HashSet<>();
            boolean duplicateSerial = false;
            long potentialSum = 0L;
            for (RootRecord root : grouped) {
                if (!rootSerials.add(root.rootSerial)) {
                    duplicateSerial = true;
                }
                potentialSum += root.potentialUnits;
            }
            if (duplicateSerial || (entry.getValue().origin == ResourceOrigin.NORMAL_BLOOM
                    && potentialSum > WoodLimits.BATCH_POTENTIAL)) {
                droppedRoots += grouped.size();
                droppedBatches++;
                batchIterator.remove();
                continue;
            }
            for (RootRecord root : grouped) {
                keptRoots.put(root.rootId, root);
            }
        }
        Map<UUID, List<BatchRecord>> batchesByOwner = new HashMap<>();
        for (BatchRecord batch : keptBatches.values()) {
            batchesByOwner.computeIfAbsent(batch.ownerId, ignored -> new ArrayList<>()).add(batch);
        }
        for (List<BatchRecord> owned : batchesByOwner.values()) {
            if (owned.size() <= WoodLimits.BATCHES_PER_OWNER) {
                continue;
            }
            owned.sort(Comparator.comparingLong((BatchRecord batch) -> batch.serial).reversed());
            while (owned.size() > WoodLimits.BATCHES_PER_OWNER) {
                BatchRecord removed = owned.remove(0);
                keptBatches.remove(removed.serial);
                droppedBatches++;
                Iterator<Map.Entry<UUID, RootRecord>> rootIterator = keptRoots.entrySet().iterator();
                while (rootIterator.hasNext()) {
                    if (rootIterator.next().getValue().batchSerial == removed.serial) {
                        rootIterator.remove();
                        droppedRoots++;
                    }
                }
            }
        }
        Map<UUID, Integer> fragmentIdCounts = new HashMap<>();
        List<FragmentImage> fragmentCandidates = new ArrayList<>();
        for (FragmentImage fragment : image.fragments) {
            RootRecord root = fragment == null ? null : keptRoots.get(fragment.rootId);
            if (!validFragment(fragment, root)) {
                droppedFragments++;
                continue;
            }
            fragmentCandidates.add(fragment);
            fragmentIdCounts.merge(fragment.fragmentId, 1, Integer::sum);
        }
        Map<UUID, List<FragmentImage>> fragmentsByRoot = new HashMap<>();
        for (FragmentImage fragment : fragmentCandidates) {
            if (fragmentIdCounts.getOrDefault(fragment.fragmentId, 0) != 1) {
                droppedFragments++;
                continue;
            }
            fragmentsByRoot.computeIfAbsent(fragment.rootId, ignored -> new ArrayList<>()).add(fragment);
        }
        List<FragmentImage> keptFragments = new ArrayList<>();
        for (Map.Entry<UUID, List<FragmentImage>> entry : fragmentsByRoot.entrySet()) {
            List<FragmentImage> grouped = entry.getValue();
            if (intervalsOverlap(grouped)) {
                droppedFragments += grouped.size();
                continue;
            }
            keptFragments.addAll(grouped);
        }
        Map<UUID, List<FragmentImage>> fragmentsByOwner = new HashMap<>();
        for (FragmentImage fragment : keptFragments) {
            RootRecord root = keptRoots.get(fragment.rootId);
            BatchRecord batch = root == null ? null : keptBatches.get(root.batchSerial);
            if (batch == null) {
                continue;
            }
            fragmentsByOwner.computeIfAbsent(batch.ownerId, ignored -> new ArrayList<>()).add(fragment);
        }
        Set<UUID> ownerDropped = new HashSet<>();
        for (List<FragmentImage> owned : fragmentsByOwner.values()) {
            if (owned.size() <= WoodLimits.FRAGMENTS_PER_OWNER) {
                continue;
            }
            owned.sort(WoodLedger::newestFragmentFirst);
            while (owned.size() > WoodLimits.FRAGMENTS_PER_OWNER) {
                ownerDropped.add(owned.remove(0).fragmentId);
                droppedFragments++;
            }
        }
        if (!ownerDropped.isEmpty()) {
            keptFragments.removeIf(fragment -> ownerDropped.contains(fragment.fragmentId));
        }
        if (keptFragments.size() > WoodLimits.FRAGMENTS_PER_SERVER) {
            keptFragments.sort(WoodLedger::newestFragmentFirst);
            while (keptFragments.size() > WoodLimits.FRAGMENTS_PER_SERVER) {
                keptFragments.remove(0);
                droppedFragments++;
            }
        }
        for (FragmentImage fragment : keptFragments) {
            RootRecord root = keptRoots.get(fragment.rootId);
            FragmentRecord record = new FragmentRecord(fragment.fragmentId, fragment.rootId, fragment.dimensionId,
                    fragment.coordX, fragment.coordY, fragment.coordZ);
            record.lowInclusive = fragment.lowInclusive;
            record.highExclusive = fragment.highExclusive;
            record.revision = fragment.revision;
            ledger.fragments.put(record.fragmentId, record);
            ledger.roots.put(root.rootId, root);
            BatchRecord batch = keptBatches.get(root.batchSerial);
            if (batch != null) {
                ledger.batches.put(batch.serial, batch);
            }
        }
        int discardedGround = 0;
        if (!keepGround) {
            discardedGround = ledger.fragments.size();
            ledger.fragments.clear();
            ledger.roots.clear();
            ledger.batches.clear();
        }
        long desired = image.nextSerial;
        if (desired < 1L) {
            desired = 1L;
        }
        if (highestSeen >= desired) {
            desired = highestSeen == Long.MAX_VALUE ? Long.MAX_VALUE : highestSeen + 1L;
        }
        boolean serialRaised = desired != image.nextSerial;
        ledger.nextSerial = desired;
        boolean rewrite = repaired || serialRaised || droppedBatches > 0 || droppedRoots > 0
                || droppedFragments > 0 || droppedOwners > 0 || discardedGround > 0;
        return new LoadReport(ledger, false, rewrite, droppedBatches, droppedRoots, droppedFragments,
                droppedOwners, discardedGround);
    }

    private static boolean validBatch(BatchImage batch) {
        return batch != null && batch.serial > 0L && batch.batchId != null && batch.ownerId != null
                && ResourceOrigin.fromName(batch.originName) != null
                && batch.dimensionId != null && !batch.dimensionId.isBlank();
    }

    private static boolean validFragment(FragmentImage fragment, RootRecord root) {
        if (fragment == null || root == null || fragment.fragmentId == null || fragment.dimensionId == null
                || fragment.dimensionId.isBlank() || fragment.revision < 1) {
            return false;
        }
        if (fragment.batchSerial != root.batchSerial || fragment.rootSerial != root.rootSerial) {
            return false;
        }
        if (fragment.lowInclusive < 0 || fragment.highExclusive <= fragment.lowInclusive
                || fragment.highExclusive > root.effectiveUnits) {
            return false;
        }
        return WoodInterval.addressable(fragment.coordX, fragment.coordY, fragment.coordZ);
    }

    private static boolean intervalsOverlap(List<FragmentImage> grouped) {
        List<FragmentImage> ordered = new ArrayList<>(grouped);
        ordered.sort(Comparator.comparingInt((FragmentImage fragment) -> fragment.lowInclusive)
                .thenComparing(fragment -> fragment.fragmentId, WoodLedger::compareUuid));
        int coveredUntil = ordered.get(0).highExclusive;
        for (int cursor = 1; cursor < ordered.size(); cursor++) {
            FragmentImage next = ordered.get(cursor);
            if (next.lowInclusive < coveredUntil) {
                return true;
            }
            if (next.highExclusive > coveredUntil) {
                coveredUntil = next.highExclusive;
            }
        }
        return false;
    }

    private static int newestFragmentFirst(FragmentImage left, FragmentImage right) {
        int batchOrder = Long.compare(right.batchSerial, left.batchSerial);
        if (batchOrder != 0) {
            return batchOrder;
        }
        int rootOrder = Integer.compare(right.rootSerial, left.rootSerial);
        if (rootOrder != 0) {
            return rootOrder;
        }
        return compareUuid(right.fragmentId, left.fragmentId);
    }

    private List<UUID> sweepTickets(int tickCount) {
        List<UUID> removed = new ArrayList<>();
        Iterator<OpenTicket> iterator = openTickets.values().iterator();
        while (iterator.hasNext()) {
            OpenTicket openTicket = iterator.next();
            if (openTicket.issuedTick == tickCount) {
                continue;
            }
            removed.add(openTicket.token);
            releaseCount(openTicket.ownerId);
            iterator.remove();
        }
        return List.copyOf(removed);
    }

    private OpenTicket matchingOpen(ProductionTicket ticket) {
        OpenTicket openTicket = openTickets.get(ticket.token());
        if (openTicket == null || openTicket.serial != ticket.serial() || openTicket.origin != ticket.origin()
                || !openTicket.ownerId.equals(ticket.ownerId())) {
            return null;
        }
        return openTicket;
    }

    private void releaseOpen(OpenTicket openTicket) {
        if (openTickets.remove(openTicket.token) != null) {
            releaseCount(openTicket.ownerId);
        }
    }

    private void releaseCount(UUID ownerId) {
        Integer count = openTicketsByOwner.get(ownerId);
        if (count == null || count <= 1) {
            openTicketsByOwner.remove(ownerId);
        } else {
            openTicketsByOwner.put(ownerId, count - 1);
        }
    }

    private static WoodReject validateDraft(LotDraft draft) {
        if (draft == null || draft.flower == null || draft.dimensionId == null || draft.dimensionId.isBlank()) {
            return WoodReject.VALIDATION;
        }
        if (draft.effectiveUnits <= 0 || draft.potentialUnits < 0 || draft.lifeTicks <= 0L || draft.expireGameTime < 0L) {
            return WoodReject.VALIDATION;
        }
        if (!WoodInterval.addressable(draft.coordX, draft.coordY, draft.coordZ)) {
            return WoodReject.VALIDATION;
        }
        return null;
    }

    private int batchesOf(UUID ownerId) {
        int count = 0;
        for (BatchRecord batch : batches.values()) {
            if (ownerId.equals(batch.ownerId)) {
                count++;
            }
        }
        return count;
    }

    private int fragmentsOf(UUID ownerId) {
        int count = 0;
        for (FragmentRecord fragment : fragments.values()) {
            RootRecord root = roots.get(fragment.rootId);
            BatchRecord batch = root == null ? null : batches.get(root.batchSerial);
            if (batch != null && ownerId.equals(batch.ownerId)) {
                count++;
            }
        }
        return count;
    }

    private List<UUID> fragmentsOfOwner(UUID ownerId) {
        List<UUID> doomed = new ArrayList<>();
        for (FragmentRecord fragment : fragments.values()) {
            RootRecord root = roots.get(fragment.rootId);
            BatchRecord batch = root == null ? null : batches.get(root.batchSerial);
            if (batch != null && ownerId.equals(batch.ownerId)) {
                doomed.add(fragment.fragmentId);
            }
        }
        return doomed;
    }

    private boolean eraseIds(List<UUID> doomed) {
        boolean changed = false;
        for (UUID fragmentId : doomed) {
            if (fragments.remove(fragmentId) != null) {
                changed = true;
            }
        }
        if (changed) {
            cleanupEmpty();
        }
        return changed;
    }

    private void cleanupEmpty() {
        Set<UUID> usedRoots = new HashSet<>();
        for (FragmentRecord fragment : fragments.values()) {
            usedRoots.add(fragment.rootId);
        }
        roots.keySet().removeIf(rootId -> !usedRoots.contains(rootId));
        Set<Long> usedBatches = new HashSet<>();
        for (RootRecord root : roots.values()) {
            usedBatches.add(root.batchSerial);
        }
        batches.keySet().removeIf(serial -> !usedBatches.contains(serial));
    }

    private TakePlan planTake(UUID actorId, FragmentTake take) {
        FragmentRecord fragment = fragments.get(take.fragmentId());
        if (fragment == null || fragment.revision != take.revision()) {
            return null;
        }
        int length = fragment.highExclusive - fragment.lowInclusive;
        if (take.units() > length) {
            return null;
        }
        boolean exhausts = take.units() == length;
        if (!exhausts && fragment.revision == Integer.MAX_VALUE) {
            return null;
        }
        RootRecord root = roots.get(fragment.rootId);
        BatchRecord batch = root == null ? null : batches.get(root.batchSerial);
        if (root == null || batch == null || !actorId.equals(batch.ownerId)) {
            return null;
        }
        int potentialUnits = WoodInterval.potential(root.potentialUnits, root.effectiveUnits,
                fragment.lowInclusive, fragment.lowInclusive + take.units());
        int nextRevision = exhausts ? fragment.revision : fragment.revision + 1;
        return new TakePlan(fragment, root.flower, batch.origin, take.units(), potentialUnits, exhausts, nextRevision);
    }

    private GroundConsumePreview previewOf(UUID ownerId, List<TakePlan> plans) {
        List<GroundPieceTake> pieces = new ArrayList<>();
        long potentialSum = 0L;
        for (TakePlan plan : plans) {
            potentialSum += plan.potentialUnits;
            pieces.add(plan.piece());
        }
        int shownPotential = potentialSum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) potentialSum;
        return new GroundConsumePreview(ownerId, shownPotential, pieces);
    }

    private GroundFragmentView viewOf(FragmentRecord fragment) {
        RootRecord root = roots.get(fragment.rootId);
        BatchRecord batch = root == null ? null : batches.get(root.batchSerial);
        if (root == null || batch == null) {
            return null;
        }
        return new GroundFragmentView(fragment.fragmentId, fragment.revision, root.rootId, root.rootSerial,
                batch.batchId, batch.serial, batch.ownerId, batch.origin, root.flower, fragment.lowInclusive,
                fragment.highExclusive, root.effectiveUnits, root.potentialUnits, root.expireGameTime,
                fragment.dimensionId, fragment.coordX, fragment.coordY, fragment.coordZ);
    }

    private int compareFragment(FragmentRecord left, FragmentRecord right) {
        RootRecord leftRoot = roots.get(left.rootId);
        RootRecord rightRoot = roots.get(right.rootId);
        long leftBatch = leftRoot == null ? Long.MAX_VALUE : leftRoot.batchSerial;
        long rightBatch = rightRoot == null ? Long.MAX_VALUE : rightRoot.batchSerial;
        int batchOrder = Long.compare(leftBatch, rightBatch);
        if (batchOrder != 0) {
            return batchOrder;
        }
        int leftSerial = leftRoot == null ? Integer.MAX_VALUE : leftRoot.rootSerial;
        int rightSerial = rightRoot == null ? Integer.MAX_VALUE : rightRoot.rootSerial;
        int rootOrder = Integer.compare(leftSerial, rightSerial);
        if (rootOrder != 0) {
            return rootOrder;
        }
        int lowOrder = Integer.compare(left.lowInclusive, right.lowInclusive);
        if (lowOrder != 0) {
            return lowOrder;
        }
        return compareUuid(left.fragmentId, right.fragmentId);
    }

    private WoodReject blockMutation() {
        if (sealed) {
            if (applyingReady) {
                readyViolated = true;
            }
            return WoodReject.SEALED;
        }
        if (applyingReady) {
            readyViolated = true;
            return WoodReject.REENTRY;
        }
        return null;
    }

    static int compareUuid(UUID left, UUID right) {
        int highOrder = Long.compareUnsigned(left.getMostSignificantBits(), right.getMostSignificantBits());
        if (highOrder != 0) {
            return highOrder;
        }
        return Long.compareUnsigned(left.getLeastSignificantBits(), right.getLeastSignificantBits());
    }

    interface WorldProbe {
        boolean ownerOnline(UUID ownerId);

        boolean ownerAlive(UUID ownerId);

        boolean ownerInDimension(UUID ownerId, String dimensionId);

        boolean chunkLoaded(String dimensionId, int chunkX, int chunkZ);

        long gameTime(String dimensionId);
    }

    static final class TickReport {
        final boolean persistentChange;
        final int removedFragments;
        final int awardedUnits;
        final List<UUID> expiredTickets;

        TickReport(boolean persistentChange, int removedFragments, int awardedUnits, List<UUID> expiredTickets) {
            this.persistentChange = persistentChange;
            this.removedFragments = removedFragments;
            this.awardedUnits = awardedUnits;
            this.expiredTickets = expiredTickets == null ? List.of() : List.copyOf(expiredTickets);
        }

        static TickReport idle() {
            return new TickReport(false, 0, 0, List.of());
        }
    }

    static final class LoadReport {
        final WoodLedger ledger;
        final boolean sealed;
        final boolean rewrite;
        final int droppedBatches;
        final int droppedRoots;
        final int droppedFragments;
        final int droppedOwners;
        final int discardedGround;

        LoadReport(WoodLedger ledger, boolean sealed, boolean rewrite, int droppedBatches, int droppedRoots,
                   int droppedFragments, int droppedOwners, int discardedGround) {
            this.ledger = ledger;
            this.sealed = sealed;
            this.rewrite = rewrite;
            this.droppedBatches = droppedBatches;
            this.droppedRoots = droppedRoots;
            this.droppedFragments = droppedFragments;
            this.droppedOwners = droppedOwners;
            this.discardedGround = discardedGround;
        }
    }

    static final class LotDraft {
        final FlowerKind flower;
        final int effectiveUnits;
        final int potentialUnits;
        final long lifeTicks;
        final long expireGameTime;
        final String dimensionId;
        final double coordX;
        final double coordY;
        final double coordZ;

        LotDraft(FlowerKind flower, int effectiveUnits, int potentialUnits, long lifeTicks, long expireGameTime,
                 String dimensionId, double coordX, double coordY, double coordZ) {
            this.flower = flower;
            this.effectiveUnits = effectiveUnits;
            this.potentialUnits = potentialUnits;
            this.lifeTicks = lifeTicks;
            this.expireGameTime = expireGameTime;
            this.dimensionId = dimensionId;
            this.coordX = coordX;
            this.coordY = coordY;
            this.coordZ = coordZ;
        }
    }

    static final class LedgerImage {
        final int formatVersion;
        final long nextSerial;
        final List<OwnerImage> owners;
        final List<BatchImage> batches;
        final List<RootImage> roots;
        final List<FragmentImage> fragments;

        LedgerImage(int formatVersion, long nextSerial, List<OwnerImage> owners, List<BatchImage> batches,
                    List<RootImage> roots, List<FragmentImage> fragments) {
            this.formatVersion = formatVersion;
            this.nextSerial = nextSerial;
            this.owners = owners == null ? List.of() : List.copyOf(owners);
            this.batches = batches == null ? List.of() : List.copyOf(batches);
            this.roots = roots == null ? List.of() : List.copyOf(roots);
            this.fragments = fragments == null ? List.of() : List.copyOf(fragments);
        }

        LedgerImage copy() {
            return new LedgerImage(formatVersion, nextSerial, owners, batches, roots, fragments);
        }

        boolean same(LedgerImage other) {
            if (other == null || formatVersion != other.formatVersion || nextSerial != other.nextSerial
                    || owners.size() != other.owners.size() || batches.size() != other.batches.size()
                    || roots.size() != other.roots.size() || fragments.size() != other.fragments.size()) {
                return false;
            }
            for (int cursor = 0; cursor < owners.size(); cursor++) {
                if (!owners.get(cursor).same(other.owners.get(cursor))) {
                    return false;
                }
            }
            for (int cursor = 0; cursor < batches.size(); cursor++) {
                if (!batches.get(cursor).same(other.batches.get(cursor))) {
                    return false;
                }
            }
            for (int cursor = 0; cursor < roots.size(); cursor++) {
                if (!roots.get(cursor).same(other.roots.get(cursor))) {
                    return false;
                }
            }
            for (int cursor = 0; cursor < fragments.size(); cursor++) {
                if (!fragments.get(cursor).same(other.fragments.get(cursor))) {
                    return false;
                }
            }
            return true;
        }
    }

    static final class OwnerImage {
        final UUID ownerId;
        final int peach;
        final int bamboo;
        final int wisteria;
        final int plum;
        final int orchid;
        final int higan;
        final int rose;

        OwnerImage(UUID ownerId, int peach, int bamboo, int wisteria, int plum, int orchid, int higan, int rose) {
            this.ownerId = ownerId;
            this.peach = peach;
            this.bamboo = bamboo;
            this.wisteria = wisteria;
            this.plum = plum;
            this.orchid = orchid;
            this.higan = higan;
            this.rose = rose;
        }

        boolean same(OwnerImage other) {
            return other != null && ownerId.equals(other.ownerId) && peach == other.peach && bamboo == other.bamboo
                    && wisteria == other.wisteria && plum == other.plum && orchid == other.orchid
                    && higan == other.higan && rose == other.rose;
        }
    }

    static final class BatchImage {
        final long serial;
        final UUID batchId;
        final UUID ownerId;
        final String originName;
        final String dimensionId;
        final long createdGameTime;

        BatchImage(long serial, UUID batchId, UUID ownerId, String originName, String dimensionId, long createdGameTime) {
            this.serial = serial;
            this.batchId = batchId;
            this.ownerId = ownerId;
            this.originName = originName;
            this.dimensionId = dimensionId;
            this.createdGameTime = createdGameTime;
        }

        boolean same(BatchImage other) {
            return other != null && serial == other.serial && batchId.equals(other.batchId) && ownerId.equals(other.ownerId)
                    && originName.equals(other.originName) && dimensionId.equals(other.dimensionId)
                    && createdGameTime == other.createdGameTime;
        }
    }

    static final class RootImage {
        final UUID rootId;
        final long batchSerial;
        final int rootSerial;
        final String flowerName;
        final int effectiveUnits;
        final int potentialUnits;
        final long expireGameTime;

        RootImage(UUID rootId, long batchSerial, int rootSerial, String flowerName, int effectiveUnits,
                  int potentialUnits, long expireGameTime) {
            this.rootId = rootId;
            this.batchSerial = batchSerial;
            this.rootSerial = rootSerial;
            this.flowerName = flowerName;
            this.effectiveUnits = effectiveUnits;
            this.potentialUnits = potentialUnits;
            this.expireGameTime = expireGameTime;
        }

        boolean same(RootImage other) {
            return other != null && rootId.equals(other.rootId) && batchSerial == other.batchSerial
                    && rootSerial == other.rootSerial && flowerName.equals(other.flowerName)
                    && effectiveUnits == other.effectiveUnits && potentialUnits == other.potentialUnits
                    && expireGameTime == other.expireGameTime;
        }
    }

    static final class FragmentImage {
        final UUID fragmentId;
        final UUID rootId;
        final long batchSerial;
        final int rootSerial;
        final String dimensionId;
        final double coordX;
        final double coordY;
        final double coordZ;
        final int lowInclusive;
        final int highExclusive;
        final int revision;

        FragmentImage(UUID fragmentId, UUID rootId, long batchSerial, int rootSerial, String dimensionId,
                      double coordX, double coordY, double coordZ, int lowInclusive, int highExclusive, int revision) {
            this.fragmentId = fragmentId;
            this.rootId = rootId;
            this.batchSerial = batchSerial;
            this.rootSerial = rootSerial;
            this.dimensionId = dimensionId;
            this.coordX = coordX;
            this.coordY = coordY;
            this.coordZ = coordZ;
            this.lowInclusive = lowInclusive;
            this.highExclusive = highExclusive;
            this.revision = revision;
        }

        boolean same(FragmentImage other) {
            return other != null && fragmentId.equals(other.fragmentId) && rootId.equals(other.rootId)
                    && batchSerial == other.batchSerial && rootSerial == other.rootSerial
                    && dimensionId.equals(other.dimensionId)
                    && Double.doubleToLongBits(coordX) == Double.doubleToLongBits(other.coordX)
                    && Double.doubleToLongBits(coordY) == Double.doubleToLongBits(other.coordY)
                    && Double.doubleToLongBits(coordZ) == Double.doubleToLongBits(other.coordZ)
                    && lowInclusive == other.lowInclusive && highExclusive == other.highExclusive
                    && revision == other.revision;
        }
    }

    private final class TakePlan {
        private final UUID fragmentId;
        private final int revision;
        private final int lowInclusive;
        private final int highExclusive;
        private final int units;
        private final FlowerKind flower;
        private final ResourceOrigin origin;
        private final int potentialUnits;
        private final boolean exhausts;
        private final int nextRevision;

        private TakePlan(FragmentRecord fragment, FlowerKind flower, ResourceOrigin origin, int units,
                         int potentialUnits, boolean exhausts, int nextRevision) {
            this.fragmentId = fragment.fragmentId;
            this.revision = fragment.revision;
            this.lowInclusive = fragment.lowInclusive;
            this.highExclusive = fragment.highExclusive;
            this.units = units;
            this.flower = flower;
            this.origin = origin;
            this.potentialUnits = potentialUnits;
            this.exhausts = exhausts;
            this.nextRevision = nextRevision;
        }

        private boolean matchesLive() {
            FragmentRecord fragment = fragments.get(fragmentId);
            return fragment != null && fragment.revision == revision && fragment.lowInclusive == lowInclusive
                    && fragment.highExclusive == highExclusive;
        }

        private void apply() {
            FragmentRecord fragment = fragments.get(fragmentId);
            if (exhausts) {
                fragments.remove(fragmentId);
                return;
            }
            fragment.lowInclusive = lowInclusive + units;
            fragment.revision = nextRevision;
        }

        private GroundPieceTake piece() {
            return new GroundPieceTake(fragmentId, revision, units, flower, origin, potentialUnits, exhausts, nextRevision);
        }
    }

    private static final class OpenTicket {
        private final UUID token;
        private final long serial;
        private final UUID ownerId;
        private final ResourceOrigin origin;
        private final int issuedTick;

        private OpenTicket(UUID token, long serial, UUID ownerId, ResourceOrigin origin, int issuedTick) {
            this.token = token;
            this.serial = serial;
            this.ownerId = ownerId;
            this.origin = origin;
            this.issuedTick = issuedTick;
        }
    }

    private static final class BatchRecord {
        private final long serial;
        private final UUID batchId;
        private final UUID ownerId;
        private final ResourceOrigin origin;
        private final String dimensionId;
        private final long createdGameTime;

        private BatchRecord(long serial, UUID batchId, UUID ownerId, ResourceOrigin origin, String dimensionId,
                            long createdGameTime) {
            this.serial = serial;
            this.batchId = batchId;
            this.ownerId = ownerId;
            this.origin = origin;
            this.dimensionId = dimensionId;
            this.createdGameTime = createdGameTime;
        }
    }

    private static final class RootRecord {
        private final UUID rootId;
        private final long batchSerial;
        private final int rootSerial;
        private final FlowerKind flower;
        private final int effectiveUnits;
        private final int potentialUnits;
        private final long expireGameTime;

        private RootRecord(UUID rootId, long batchSerial, int rootSerial, FlowerKind flower, int effectiveUnits,
                           int potentialUnits, long expireGameTime) {
            this.rootId = rootId;
            this.batchSerial = batchSerial;
            this.rootSerial = rootSerial;
            this.flower = flower;
            this.effectiveUnits = effectiveUnits;
            this.potentialUnits = potentialUnits;
            this.expireGameTime = expireGameTime;
        }
    }

    private static final class FragmentRecord {
        private final UUID fragmentId;
        private final UUID rootId;
        private final String dimensionId;
        private final double coordX;
        private final double coordY;
        private final double coordZ;
        private int lowInclusive;
        private int highExclusive;
        private int revision;

        private FragmentRecord(UUID fragmentId, UUID rootId, String dimensionId, double coordX, double coordY,
                               double coordZ) {
            this.fragmentId = fragmentId;
            this.rootId = rootId;
            this.dimensionId = dimensionId;
            this.coordX = coordX;
            this.coordY = coordY;
            this.coordZ = coordZ;
        }
    }

    private static final class SuihuaAccount {
        private final int[] amounts = new int[FlowerKind.values().length];

        private int total() {
            long sum = 0L;
            for (int amount : amounts) {
                sum += amount;
            }
            return sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
        }

        private boolean isZero() {
            for (int amount : amounts) {
                if (amount != 0) {
                    return false;
                }
            }
            return true;
        }

        private int acceptNatural(FlowerKind flower, int potentialUnits) {
            if (flower == null || potentialUnits <= 0) {
                return 0;
            }
            long sum = 0L;
            for (int amount : amounts) {
                sum += amount;
            }
            long room = (long) WoodLimits.FULL_UNITS - sum;
            if (room <= 0L) {
                return 0;
            }
            int accepted = (int) Math.min(room, (long) potentialUnits);
            amounts[flower.ordinal()] += accepted;
            return accepted;
        }

        private void load(SuihuaSnapshot snapshot) {
            amounts[FlowerKind.PEACH.ordinal()] = snapshot.peach();
            amounts[FlowerKind.BAMBOO.ordinal()] = snapshot.bamboo();
            amounts[FlowerKind.WISTERIA.ordinal()] = snapshot.wisteria();
            amounts[FlowerKind.PLUM.ordinal()] = snapshot.plum();
            amounts[FlowerKind.ORCHID.ordinal()] = snapshot.orchid();
            amounts[FlowerKind.HIGAN.ordinal()] = snapshot.higan();
            amounts[FlowerKind.ROSE.ordinal()] = snapshot.rose();
        }

        private boolean matches(SuihuaSnapshot snapshot) {
            return snapshot != null && snapshot().equals(snapshot);
        }

        private SuihuaSnapshot snapshot() {
            return new SuihuaSnapshot(
                    amounts[FlowerKind.PEACH.ordinal()],
                    amounts[FlowerKind.BAMBOO.ordinal()],
                    amounts[FlowerKind.WISTERIA.ordinal()],
                    amounts[FlowerKind.PLUM.ordinal()],
                    amounts[FlowerKind.ORCHID.ordinal()],
                    amounts[FlowerKind.HIGAN.ordinal()],
                    amounts[FlowerKind.ROSE.ordinal()]);
        }
    }
}
