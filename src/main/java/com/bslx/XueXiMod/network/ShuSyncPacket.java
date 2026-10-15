package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.player.PlayerShuData;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 术数据同步包 - 服务端→客户端
 * 同步剑术等级、法术等级、使用次数、已学术列表
 * 供术管理GUI显示使用
 */
public class ShuSyncPacket {
    private final int swordLevel;
    private final int magicLevel;
    private final int swordUsage;
    private final int magicUsage;
    private final int magic;
    private final int magicMax;
    private final Map<String, Integer> usageCounts;
    private final Set<String> learnedIds;

    public ShuSyncPacket(PlayerShuData data) {
        this.swordLevel = data.getSwordLevel();
        this.magicLevel = data.getMagicLevel();
        this.swordUsage = data.getSwordUsage();
        this.magicUsage = data.getMagicUsage();
        this.magic = data.getMagic();
        this.magicMax = data.getMagicMax();
        this.usageCounts = new HashMap<>(data.getAllUsageCounts());
        this.learnedIds = new HashSet<>(data.getLearnedTechniqueIds());
    }

    private ShuSyncPacket(int swordLevel, int magicLevel, int swordUsage, int magicUsage, int magic, int magicMax,
                          Map<String, Integer> usageCounts, Set<String> learnedIds) {
        this.swordLevel = swordLevel;
        this.magicLevel = magicLevel;
        this.swordUsage = swordUsage;
        this.magicUsage = magicUsage;
        this.magic = magic;
        this.magicMax = magicMax;
        this.usageCounts = usageCounts;
        this.learnedIds = learnedIds;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(swordLevel);
        buf.writeInt(magicLevel);
        buf.writeInt(swordUsage);
        buf.writeInt(magicUsage);
        buf.writeInt(magic);
        buf.writeInt(magicMax);

        // 写入已学术
        buf.writeInt(learnedIds.size());
        for (String id : learnedIds) {
            buf.writeUtf(id);
        }

        // 写入使用次数
        buf.writeInt(usageCounts.size());
        for (Map.Entry<String, Integer> entry : usageCounts.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeInt(entry.getValue());
        }
    }

    public static ShuSyncPacket decode(FriendlyByteBuf buf) {
        int swordLevel = buf.readInt();
        int magicLevel = buf.readInt();
        int swordUsage = buf.readInt();
        int magicUsage = buf.readInt();
        int magic = buf.readInt();
        int magicMax = buf.readInt();

        int learnedCount = buf.readInt();
        if (learnedCount < 0 || learnedCount > 256) {
            throw new io.netty.handler.codec.DecoderException("learnedCount 越界: " + learnedCount);
        }
        Set<String> learnedIds = new HashSet<>();
        for (int i = 0; i < learnedCount; i++) {
            learnedIds.add(buf.readUtf());
        }

        int usageCount = buf.readInt();
        if (usageCount < 0 || usageCount > 256) {
            throw new io.netty.handler.codec.DecoderException("usageCount 越界: " + usageCount);
        }
        Map<String, Integer> usageCounts = new HashMap<>();
        for (int i = 0; i < usageCount; i++) {
            usageCounts.put(buf.readUtf(), buf.readInt());
        }

        return new ShuSyncPacket(swordLevel, magicLevel, swordUsage, magicUsage, magic, magicMax, usageCounts, learnedIds);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 在客户端存储同步数据
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientShuCache.update(swordLevel, magicLevel, swordUsage, magicUsage, magic, magicMax, usageCounts, learnedIds);
            });
        });
        ctx.get().setPacketHandled(true);
    }

    public static void register(SimpleChannel channel, int id) {
        channel.registerMessage(id,
                ShuSyncPacket.class,
                ShuSyncPacket::encode,
                ShuSyncPacket::decode,
                ShuSyncPacket::handle
        );
    }

    public static class Registration implements NetworkPacket {
        @Override
        public void register(SimpleChannel channel, int id) {
            ShuSyncPacket.register(channel, id);
        }
    }

    /**
     * 客户端缓存 - 存储从服务端同步的术数据
     * 供术管理GUI读取
     */
    public static class ClientShuCache {
        private static int swordLevel = 0;
        private static int magicLevel = 0;
        private static int swordUsage = 0;
        private static int magicUsage = 0;
        private static int magic = 0;
        private static int magicMax = 0;
        private static final Map<String, Integer> usageCounts = new HashMap<>();
        private static final Set<String> learnedIds = new HashSet<>();

        public static void update(int swordLevel, int magicLevel, int swordUsage, int magicUsage, int magic, int magicMax,
                                  Map<String, Integer> usageCounts, Set<String> learnedIds) {
            ClientShuCache.swordLevel = swordLevel;
            ClientShuCache.magicLevel = magicLevel;
            ClientShuCache.swordUsage = swordUsage;
            ClientShuCache.magicUsage = magicUsage;
            ClientShuCache.magic = magic;
            ClientShuCache.magicMax = magicMax;
            ClientShuCache.usageCounts.clear();
            ClientShuCache.usageCounts.putAll(usageCounts);
            ClientShuCache.learnedIds.clear();
            ClientShuCache.learnedIds.addAll(learnedIds);
        }

        public static int getSwordLevel() { return swordLevel; }
        public static int getMagicLevel() { return magicLevel; }
        public static int getSwordUsage() { return swordUsage; }
        public static int getMagicUsage() { return magicUsage; }
        public static int getMagic() { return magic; }
        public static int getMagicMax() { return magicMax; }
        public static int getUsageCount(String id) { return usageCounts.getOrDefault(id, 0); }
        public static boolean hasLearned(String id) { return learnedIds.contains(id); }
        public static Set<String> getLearnedIds() { return learnedIds; }
        public static Map<String, Integer> getUsageCounts() { return usageCounts; }

        /**
         * 检查术是否已解锁（客户端版本）
         */
        public static boolean isUnlocked(Technique technique) {
            if (technique.getType() == com.bslx.XueXiMod.shu.TechniqueType.UNIVERSAL
                    || technique.getType() == com.bslx.XueXiMod.shu.TechniqueType.MOVEMENT) {
                return true;
            }
            int level = (technique.getType() == com.bslx.XueXiMod.shu.TechniqueType.SWORD) ? swordLevel : magicLevel;
            return level >= technique.getUnlockLevel();
        }
    }
}
