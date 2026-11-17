package com.bslx.XueXiMod.player;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueAvailability;
import com.bslx.XueXiMod.shu.TechniqueIds;
import com.bslx.XueXiMod.shu.TechniqueManager;
import com.bslx.XueXiMod.shu.TechniqueType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * 玩家术数据
 * 存储已学术、熟练度、剑术等级、法术等级
 * 使用 PersistentData 持久化
 *
 * 等级系统：
 * - 剑术等级(swordLevel)：通过使用剑术+消耗经验提升，控制剑术解锁
 * - 法术等级(magicLevel)：通过使用法术+消耗经验提升，控制法术解锁
 * - 通用/动作术：始终解锁，不受等级限制
 */
public class PlayerShuData {
    private static final String DATA_KEY = "xueximod_shu_data";

    // 已学术集合
    private final Set<String> learnedTechniques = new HashSet<>();
    private final Set<String> domainPermits = new HashSet<>();
    private int domainMigrateVersion;
    // 术使用次数 <techniqueId, usageCount>
    private final Map<String, Integer> usageCounts = new HashMap<>();

    // 剑术/法术等级
    private int swordLevel = 0;
    private int magicLevel = 0;
    private int swordUsage = 0;  // 剑术总使用次数
    private int magicUsage = 0;  // 法术总使用次数

    // 魔力池
    private int magic = 100;
    private int magicUpperBound = 100;

    /** 水势。和魔力分开，上限 100，超出丢掉。 */
    private float waterMomentum;

    // 等级提升所需使用次数（每级需要 10*level 次使用）
    private static final int USAGE_PER_LEVEL = 10;
    // 等级提升所需经验（每级需要 10*level 经验值）
    private static final int XP_PER_LEVEL = 10;
    public static final int TYPE_LEVEL_UPPER_BOUND = 20;

    /**
     * 获取玩家的术数据
     */
    public static PlayerShuData get(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        PlayerShuData data = new PlayerShuData();
        if (persistentData.contains(DATA_KEY)) {
            data.load(persistentData.getCompound(DATA_KEY));
        }
        return data;
    }

    /**
     * 保存玩家术数据
     */
    public static void save(Player player, PlayerShuData data) {
        CompoundTag persistentData = player.getPersistentData();
        persistentData.put(DATA_KEY, data.save());
    }

    // === 学术 ===

    public boolean hasDomainPermit(String techniqueId) {
        return domainPermits.contains(TechniqueIds.canonicalId(techniqueId));
    }

    public void grantDomainPermit(String techniqueId) {
        if (techniqueId != null && !techniqueId.isEmpty()) {
            domainPermits.add(TechniqueIds.canonicalId(techniqueId));
        }
    }

    public int domainMigrateVersion() {
        return domainMigrateVersion;
    }

    public void setDomainMigrateVersion(int version) {
        domainMigrateVersion = version;
    }

    public boolean hasLearned(String techniqueId) {
        return learnedTechniques.contains(TechniqueIds.canonicalId(techniqueId));
    }

    public void learnTechnique(String techniqueId) {
        String canonicalId = TechniqueIds.canonicalId(techniqueId);
        Technique registered = TechniqueManager.getById(canonicalId);
        if (registered != null && registered.availability() == TechniqueAvailability.DEVELOPMENT_ONLY) {
            return;
        }
        learnedTechniques.add(canonicalId);
    }

    public Set<String> getLearnedTechniqueIds() {
        return Collections.unmodifiableSet(learnedTechniques);
    }

    public List<Technique> getLearnedTechniques() {
        List<Technique> result = new ArrayList<>();
        for (String techniqueId : learnedTechniques) {
            Technique technique = TechniqueManager.getById(techniqueId);
            if (technique != null) result.add(technique);
        }
        return result;
    }

    // === 熟练度 ===

    public int getUsageCount(String techniqueId) {
        return usageCounts.getOrDefault(TechniqueIds.canonicalId(techniqueId), 0);
    }

    public void incrementUsage(String techniqueId) {
        usageCounts.merge(TechniqueIds.canonicalId(techniqueId), 1, Integer::sum);
    }

    public Map<String, Integer> getAllUsageCounts() {
        return Collections.unmodifiableMap(usageCounts);
    }

    // === 剑术/法术等级 ===

    public int getSwordLevel() { return swordLevel; }
    public int getMagicLevel() { return magicLevel; }
    public int getSwordUsage() { return swordUsage; }
    public int getMagicUsage() { return magicUsage; }

    /** 直接改这一系的熟练等级和熟练点。等级不超过上限。 */
    public void setTypeRank(TechniqueType type, int level, int points) {
        int boundedLevel = Math.min(TYPE_LEVEL_UPPER_BOUND, Math.max(0, level));
        int boundedPoints = Math.max(0, points);
        if (type == TechniqueType.SWORD) {
            swordLevel = boundedLevel;
            swordUsage = boundedPoints;
            return;
        }
        if (type == TechniqueType.MAGIC) {
            magicLevel = boundedLevel;
            magicUsage = boundedPoints;
        }
    }

    /** 刚到达该等级时的熟练点。下一级仍要再攒一轮。 */
    public static int pointsAtLevel(int level) {
        int boundedLevel = Math.min(TYPE_LEVEL_UPPER_BOUND, Math.max(0, level));
        return USAGE_PER_LEVEL * boundedLevel;
    }

    public int getMagic() { return magic; }
    public int getMagicMax() { return magicUpperBound; }

    public void setMagic(int value) {
        magic = Math.max(0, value);
    }

    public void setMagicMax(int upperBound) {
        magicUpperBound = Math.max(1, upperBound);
        magic = magicUpperBound;
    }

    public float getWaterMomentum() {
        return waterMomentum;
    }

    public void setWaterMomentum(float amount) {
        if (!Float.isFinite(amount) || amount <= 0f) {
            waterMomentum = 0f;
            return;
        }
        waterMomentum = amount > 100f ? 100f : amount;
    }

    /**
     * 获取指定类型术的当前等级
     */
    public int getTypeLevel(TechniqueType type) {
        return switch (type) {
            case SWORD -> swordLevel;
            case MAGIC -> magicLevel;
            default -> 0; // 通用/动作术无等级概念
        };
    }

    /**
     * 检查术是否已解锁
     * - 通用/动作术：始终解锁
     * - 剑术：swordLevel >= 复杂度
     * - 法术：magicLevel >= 复杂度
     */
    public boolean isUnlocked(Technique technique) {
        if (hasDomainPermit(technique.getId())) {
            return true;
        }
        if (technique.getType() == TechniqueType.UNIVERSAL
                || technique.getType() == TechniqueType.MOVEMENT) {
            return true;
        }
        return getTypeLevel(technique.getType()) >= technique.getComplexity();
    }

    /**
     * 使用术后增加使用次数并尝试升级
     * 调用方需传入 player 以扣除经验
     * @return 是否升级了
     */
    public boolean addUsageAndTryLevelUp(TechniqueType type, net.minecraft.world.entity.player.Player player) {
        if (type == TechniqueType.UNIVERSAL || type == TechniqueType.MOVEMENT) {
            return false; // 通用/动作术不参与等级系统
        }

        // 增加使用次数
        if (type == TechniqueType.SWORD) {
            swordUsage++;
        } else {
            magicUsage++;
        }

        // 检查是否可以升级
        int currentLevel = getTypeLevel(type);
        if (currentLevel >= TYPE_LEVEL_UPPER_BOUND) return false;

        int requiredUsage = USAGE_PER_LEVEL * (currentLevel + 1);
        int requiredXp = XP_PER_LEVEL * (currentLevel + 1);

        int currentUsage = (type == TechniqueType.SWORD) ? swordUsage : magicUsage;
        if (currentUsage < requiredUsage) return false;

        // 检查经验是否足够
        if (player.totalExperience < requiredXp) return false;

        // 扣除经验并升级
        player.giveExperiencePoints(-requiredXp);
        if (type == TechniqueType.SWORD) {
            swordLevel++;
        } else {
            magicLevel++;
        }
        return true;
    }

    /**
     * 获取升级到下一级所需使用次数（剩余）
     */
    public int getUsageToNextLevel(TechniqueType type) {
        int currentLevel = getTypeLevel(type);
        if (currentLevel >= TYPE_LEVEL_UPPER_BOUND) return -1;
        int requiredUsage = USAGE_PER_LEVEL * (currentLevel + 1);
        int currentUsage = (type == TechniqueType.SWORD) ? swordUsage : magicUsage;
        return Math.max(0, requiredUsage - currentUsage);
    }

    /**
     * 获取升级到下一级所需经验值（剩余）
     */
    public int getXpToNextLevel(TechniqueType type) {
        int currentLevel = getTypeLevel(type);
        if (currentLevel >= TYPE_LEVEL_UPPER_BOUND) return -1;
        return XP_PER_LEVEL * (currentLevel + 1);
    }

    /**
     * 获取当前类型的使用进度（0.0~1.0）
     */
    public float getTypeLevelProgress(TechniqueType type) {
        int currentLevel = getTypeLevel(type);
        if (currentLevel >= TYPE_LEVEL_UPPER_BOUND) return 1.0f;
        int requiredUsage = USAGE_PER_LEVEL * (currentLevel + 1);
        int currentUsage = (type == TechniqueType.SWORD) ? swordUsage : magicUsage;
        return Math.min(1.0f, (float) currentUsage / requiredUsage);
    }

    // === 序列化 ===

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        // 保存已学术
        ListTag learnedList = new ListTag();
        Set<String> learnedKeys = new HashSet<>();
        for (String techniqueId : learnedTechniques) {
            String canonicalKey = TechniqueIds.canonicalId(techniqueId);
            if (learnedKeys.add(canonicalKey)) {
                learnedList.add(StringTag.valueOf(canonicalKey));
            }
        }
        tag.put("Learned", learnedList);
        ListTag permitList = new ListTag();
        Set<String> permitKeys = new HashSet<>();
        for (String techniqueId : domainPermits) {
            String canonicalKey = TechniqueIds.canonicalId(techniqueId);
            if (permitKeys.add(canonicalKey)) {
                permitList.add(StringTag.valueOf(canonicalKey));
            }
        }
        tag.put("DomainPermits", permitList);
        tag.putInt("DomainMigrateVersion", domainMigrateVersion);

        // 保存使用次数
        CompoundTag usageTag = new CompoundTag();
        Map<String, Integer> usageKeys = new HashMap<>();
        for (Map.Entry<String, Integer> entry : usageCounts.entrySet()) {
            usageKeys.merge(TechniqueIds.canonicalId(entry.getKey()), entry.getValue(), PlayerShuData::saturatingSum);
        }
        for (Map.Entry<String, Integer> entry : usageKeys.entrySet()) {
            usageTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("Usage", usageTag);

        // 保存剑术/法术等级
        tag.putInt("SwordLevel", swordLevel);
        tag.putInt("MagicLevel", magicLevel);
        tag.putInt("SwordUsage", swordUsage);
        tag.putInt("MagicUsage", magicUsage);
        tag.putInt("Magic", magic);
        tag.putInt("MagicMax", magicUpperBound);
        tag.putFloat("WaterMomentum", waterMomentum);

        return tag;
    }

    void load(CompoundTag tag) {
        learnedTechniques.clear();
        domainPermits.clear();
        usageCounts.clear();

        // 加载已学术
        if (tag.contains("Learned")) {
            ListTag learnedList = tag.getList("Learned", 8);
            for (int entryIndex = 0; entryIndex < learnedList.size(); entryIndex++) {
                learnedTechniques.add(TechniqueIds.canonicalId(learnedList.getString(entryIndex)));
            }
        }
        if (tag.contains("DomainPermits")) {
            ListTag permitList = tag.getList("DomainPermits", 8);
            for (int entryIndex = 0; entryIndex < permitList.size(); entryIndex++) {
                domainPermits.add(TechniqueIds.canonicalId(permitList.getString(entryIndex)));
            }
        }
        if (tag.contains("DomainMigrateVersion")) {
            domainMigrateVersion = tag.getInt("DomainMigrateVersion");
        }

        // 加载使用次数
        if (tag.contains("Usage")) {
            CompoundTag usageTag = tag.getCompound("Usage");
            for (String storedKey : usageTag.getAllKeys()) {
                usageCounts.merge(TechniqueIds.canonicalId(storedKey), usageTag.getInt(storedKey), PlayerShuData::saturatingSum);
            }
        }

        // 加载剑术/法术等级
        swordLevel = tag.getInt("SwordLevel");
        magicLevel = tag.getInt("MagicLevel");
        swordUsage = tag.getInt("SwordUsage");
        magicUsage = tag.getInt("MagicUsage");

        if (tag.contains("MagicMax")) {
            magicUpperBound = tag.getInt("MagicMax");
        }
        if (magicUpperBound < 1) magicUpperBound = 100;
        if (tag.contains("Magic")) {
            magic = tag.getInt("Magic");
        }
        if (magic < 0) magic = 0;
        if (tag.contains("WaterMomentum")) {
            setWaterMomentum(tag.getFloat("WaterMomentum"));
        }
    }

    private static int saturatingSum(int leftCount, int rightCount) {
        long combined = (long) leftCount + (long) rightCount;
        if (combined > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (combined < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) combined;
    }
}
