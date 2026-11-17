package com.bslx.XueXiMod.status.wood.resource;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 全服只有主世界 DataStorage 里的这一份。不要按维度再存岁华。
 * 地面记录会写进同一份快照，避免和岁华分文件；下次加载按本轮裁决丢弃地面且不发奖。
 * 读到更高或无法识别的格式时保留原标签，不把账覆盖成零。
 */
public final class WoodResourceSavedData extends SavedData {
    public static final String STORAGE_ID = "xueximod_wood_resource";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG_VERSION = "formatVersion";
    private static final String TAG_NEXT = "nextSerial";
    private static final String TAG_OWNERS = "owners";
    private static final String TAG_BATCHES = "batches";
    private static final String TAG_ROOTS = "roots";
    private static final String TAG_FRAGMENTS = "fragments";

    private final WoodLedger ledger;
    private final CompoundTag sealedTag;

    private WoodResourceSavedData(WoodLedger ledger, CompoundTag sealedTag, boolean rewrite) {
        this.ledger = ledger;
        this.sealedTag = sealedTag;
        if (rewrite) {
            setDirty();
        }
    }

    static WoodResourceSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                WoodResourceSavedData::load, WoodResourceSavedData::fresh, STORAGE_ID);
    }

    public static WoodResourceSavedData fresh() {
        return new WoodResourceSavedData(WoodLedger.createEmpty(), null, false);
    }

    public static WoodResourceSavedData load(CompoundTag tag) {
        if (tag == null || tag.getAllKeys().isEmpty()) {
            return fresh();
        }
        CompoundTag source = tag.copy();
        int version = source.contains(TAG_VERSION, Tag.TAG_INT) ? source.getInt(TAG_VERSION) : Integer.MIN_VALUE;
        if (version != WoodLimits.FORMAT_VERSION && version != WoodLimits.PREVIOUS_FORMAT_VERSION) {
            LOGGER.warn("木属性资源账本格式版本 {} 不能按当前版本 {} 写入，已保留原存档", version, WoodLimits.FORMAT_VERSION);
            return new WoodResourceSavedData(WoodLedger.sealedEmpty(), source, false);
        }
        try {
            WoodLedger.LedgerImage image = readImage(source);
            WoodLedger.LoadReport report = WoodLedger.restore(image, false, version);
            if (report.discardedGround > 0 || report.droppedBatches > 0 || report.droppedRoots > 0
                    || report.droppedFragments > 0 || report.droppedOwners > 0) {
                LOGGER.warn("木属性资源账本新会话丢弃地面片段 {} 条且不发放岁华；坏批次 {}，坏根 {}，坏片段 {}，坏主人 {}",
                        report.discardedGround, report.droppedBatches, report.droppedRoots,
                        report.droppedFragments, report.droppedOwners);
            }
            return new WoodResourceSavedData(report.ledger, null, report.rewrite);
        } catch (RuntimeException exception) {
            LOGGER.warn("木属性资源账本读取失败，已保留原存档", exception);
            return new WoodResourceSavedData(WoodLedger.sealedEmpty(), source, false);
        }
    }

    WoodLedger ledger() {
        return ledger;
    }

    boolean writable() {
        return sealedTag == null && ledger.writable();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (sealedTag != null) {
            for (String key : List.copyOf(tag.getAllKeys())) {
                tag.remove(key);
            }
            for (String key : sealedTag.getAllKeys()) {
                Tag child = sealedTag.get(key);
                if (child != null) {
                    tag.put(key, child.copy());
                }
            }
            return tag;
        }
        writeImage(tag, ledger.exportImage());
        return tag;
    }

    private static WoodLedger.LedgerImage readImage(CompoundTag source) {
        List<WoodLedger.OwnerImage> owners = new ArrayList<>();
        ListTag ownerList = source.getList(TAG_OWNERS, Tag.TAG_COMPOUND);
        for (int cursor = 0; cursor < ownerList.size(); cursor++) {
            CompoundTag ownerTag = ownerList.getCompound(cursor);
            if (!ownerTag.hasUUID("ownerId")) {
                owners.add(new WoodLedger.OwnerImage(null, 0, 0, 0, 0, 0, 0, 0));
                continue;
            }
            owners.add(new WoodLedger.OwnerImage(ownerTag.getUUID("ownerId"),
                    ownerTag.getInt("peach"), ownerTag.getInt("bamboo"), ownerTag.getInt("wisteria"),
                    ownerTag.getInt("plum"), ownerTag.getInt("orchid"), ownerTag.getInt("higan"),
                    ownerTag.getInt("rose")));
        }
        List<WoodLedger.BatchImage> batches = new ArrayList<>();
        ListTag batchList = source.getList(TAG_BATCHES, Tag.TAG_COMPOUND);
        for (int cursor = 0; cursor < batchList.size(); cursor++) {
            CompoundTag batchTag = batchList.getCompound(cursor);
            batches.add(new WoodLedger.BatchImage(
                    batchTag.getLong("serial"),
                    batchTag.hasUUID("batchId") ? batchTag.getUUID("batchId") : null,
                    batchTag.hasUUID("ownerId") ? batchTag.getUUID("ownerId") : null,
                    batchTag.getString("origin"),
                    batchTag.getString("dimension"),
                    batchTag.contains("createdGameTime", Tag.TAG_LONG) ? batchTag.getLong("createdGameTime") : Long.MIN_VALUE));
        }
        List<WoodLedger.RootImage> roots = new ArrayList<>();
        ListTag rootList = source.getList(TAG_ROOTS, Tag.TAG_COMPOUND);
        for (int cursor = 0; cursor < rootList.size(); cursor++) {
            CompoundTag rootTag = rootList.getCompound(cursor);
            roots.add(new WoodLedger.RootImage(
                    rootTag.hasUUID("rootId") ? rootTag.getUUID("rootId") : null,
                    rootTag.getLong("batchSerial"),
                    rootTag.getInt("rootSerial"),
                    rootTag.getString("flower"),
                    rootTag.getInt("effective"),
                    rootTag.getInt("potential"),
                    rootTag.contains("expireGameTime", Tag.TAG_LONG) ? rootTag.getLong("expireGameTime") : Long.MIN_VALUE));
        }
        List<WoodLedger.FragmentImage> fragments = new ArrayList<>();
        ListTag fragmentList = source.getList(TAG_FRAGMENTS, Tag.TAG_COMPOUND);
        for (int cursor = 0; cursor < fragmentList.size(); cursor++) {
            CompoundTag fragmentTag = fragmentList.getCompound(cursor);
            fragments.add(new WoodLedger.FragmentImage(
                    fragmentTag.hasUUID("fragmentId") ? fragmentTag.getUUID("fragmentId") : null,
                    fragmentTag.hasUUID("rootId") ? fragmentTag.getUUID("rootId") : null,
                    fragmentTag.getLong("batchSerial"),
                    fragmentTag.getInt("rootSerial"),
                    fragmentTag.getString("dimension"),
                    readCoordinate(fragmentTag, "coordX"),
                    readCoordinate(fragmentTag, "coordY"),
                    readCoordinate(fragmentTag, "coordZ"),
                    fragmentTag.getInt("lowInclusive"),
                    fragmentTag.getInt("highExclusive"),
                    fragmentTag.getInt("revision")));
        }
        long nextSerial = source.contains(TAG_NEXT, Tag.TAG_LONG) ? source.getLong(TAG_NEXT) : 0L;
        return new WoodLedger.LedgerImage(WoodLimits.FORMAT_VERSION, nextSerial, owners, batches, roots, fragments);
    }

    private static double readCoordinate(CompoundTag fragmentTag, String key) {
        if (!fragmentTag.contains(key, Tag.TAG_DOUBLE)) {
            return Double.NaN;
        }
        return fragmentTag.getDouble(key);
    }

    private static void writeImage(CompoundTag tag, WoodLedger.LedgerImage image) {
        tag.putInt(TAG_VERSION, image.formatVersion);
        tag.putLong(TAG_NEXT, image.nextSerial);
        ListTag ownerList = new ListTag();
        for (WoodLedger.OwnerImage owner : image.owners) {
            CompoundTag ownerTag = new CompoundTag();
            ownerTag.putUUID("ownerId", owner.ownerId);
            ownerTag.putInt("peach", owner.peach);
            ownerTag.putInt("bamboo", owner.bamboo);
            ownerTag.putInt("wisteria", owner.wisteria);
            ownerTag.putInt("plum", owner.plum);
            ownerTag.putInt("orchid", owner.orchid);
            ownerTag.putInt("higan", owner.higan);
            ownerTag.putInt("rose", owner.rose);
            ownerList.add(ownerTag);
        }
        tag.put(TAG_OWNERS, ownerList);
        ListTag batchList = new ListTag();
        for (WoodLedger.BatchImage batch : image.batches) {
            CompoundTag batchTag = new CompoundTag();
            batchTag.putLong("serial", batch.serial);
            batchTag.putUUID("batchId", batch.batchId);
            batchTag.putUUID("ownerId", batch.ownerId);
            batchTag.putString("origin", batch.originName);
            batchTag.putString("dimension", batch.dimensionId);
            batchTag.putLong("createdGameTime", batch.createdGameTime);
            batchList.add(batchTag);
        }
        tag.put(TAG_BATCHES, batchList);
        ListTag rootList = new ListTag();
        for (WoodLedger.RootImage root : image.roots) {
            CompoundTag rootTag = new CompoundTag();
            rootTag.putUUID("rootId", root.rootId);
            rootTag.putLong("batchSerial", root.batchSerial);
            rootTag.putInt("rootSerial", root.rootSerial);
            rootTag.putString("flower", root.flowerName);
            rootTag.putInt("effective", root.effectiveUnits);
            rootTag.putInt("potential", root.potentialUnits);
            rootTag.putLong("expireGameTime", root.expireGameTime);
            rootList.add(rootTag);
        }
        tag.put(TAG_ROOTS, rootList);
        ListTag fragmentList = new ListTag();
        for (WoodLedger.FragmentImage fragment : image.fragments) {
            CompoundTag fragmentTag = new CompoundTag();
            fragmentTag.putUUID("fragmentId", fragment.fragmentId);
            fragmentTag.putUUID("rootId", fragment.rootId);
            fragmentTag.putLong("batchSerial", fragment.batchSerial);
            fragmentTag.putInt("rootSerial", fragment.rootSerial);
            fragmentTag.putString("dimension", fragment.dimensionId);
            fragmentTag.putDouble("coordX", fragment.coordX);
            fragmentTag.putDouble("coordY", fragment.coordY);
            fragmentTag.putDouble("coordZ", fragment.coordZ);
            fragmentTag.putInt("lowInclusive", fragment.lowInclusive);
            fragmentTag.putInt("highExclusive", fragment.highExclusive);
            fragmentTag.putInt("revision", fragment.revision);
            fragmentList.add(fragmentTag);
        }
        tag.put(TAG_FRAGMENTS, fragmentList);
    }
}
