package com.bslx.XueXiMod.player;

import com.bslx.XueXiMod.shu.Technique;
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
    // 术使用次数 <techniqueId, usageCount>
    private final Map<String, Integer> usageCounts = new HashMap<>();

    // 剑术/法术等级
    private int swordLevel = 0;
    private int magicLevel = 0;
    private int swordUsage = 0;  // 剑术总使用次数
    private int magicUsage = 0;  // 法术总使用次数

    // 魔力池
    private int magic = 100;
    private int magicMax = 100;

    // 等级提升所需使用次数（每级需要 10*level 次使用）
    private static final int USAGE_PER_LEVEL = 10;
    // 等级提升所需经验（每级需要 10*level 经验值）
    private static final int XP_PER_LEVEL = 10;
    public static final int MAX_TYPE_LEVEL = 20;

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

    public boolean hasLearned(String techniqueId) {
        return learnedTechniques.contains(techniqueId);
    }

    public void learnTechnique(String techniqueId) {
        learnedTechniques.add(techniqueId);
    }

    public Set<String> getLearnedTechniqueIds() {
        return Collections.unmodifiableSet(learnedTechniques);
    }

    public List<Technique> getLearnedTechniques() {
        List<Technique> result = new ArrayList<>();
        for (String id : learnedTechniques) {
            Technique t = TechniqueManager.getById(id);
            if (t != null) result.add(t);
        }
        return result;
    }

    // === 熟练度 ===

    public int getUsageCount(String techniqueId) {
        return usageCounts.getOrDefault(techniqueId, 0);
    }

    public void incrementUsage(String techniqueId) {
        usageCounts.merge(techniqueId, 1, Integer::sum);
    }

    public Map<String, Integer> getAllUsageCounts() {
        return Collections.unmodifiableMap(usageCounts);
    }

    // === 剑术/法术等级 ===

    public int getSwordLevel() { return swordLevel; }
    public int getMagicLevel() { return magicLevel; }
    public int getSwordUsage() { return swordUsage; }
    public int getMagicUsage() { return magicUsage; }

    public int getMagic() { return magic; }
    public int getMagicMax() { return magicMax; }

    public void setMagic(int value) {
        magic = Math.max(0, value);
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
        if (currentLevel >= MAX_TYPE_LEVEL) return false;

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
        if (currentLevel >= MAX_TYPE_LEVEL) return -1;
        int requiredUsage = USAGE_PER_LEVEL * (currentLevel + 1);
        int currentUsage = (type == TechniqueType.SWORD) ? swordUsage : magicUsage;
        return Math.max(0, requiredUsage - currentUsage);
    }

    /**
     * 获取升级到下一级所需经验值（剩余）
     */
    public int getXpToNextLevel(TechniqueType type) {
        int currentLevel = getTypeLevel(type);
        if (currentLevel >= MAX_TYPE_LEVEL) return -1;
        return XP_PER_LEVEL * (currentLevel + 1);
    }

    /**
     * 获取当前类型的使用进度（0.0~1.0）
     */
    public float getTypeLevelProgress(TechniqueType type) {
        int currentLevel = getTypeLevel(type);
        if (currentLevel >= MAX_TYPE_LEVEL) return 1.0f;
        int requiredUsage = USAGE_PER_LEVEL * (currentLevel + 1);
        int currentUsage = (type == TechniqueType.SWORD) ? swordUsage : magicUsage;
        return Math.min(1.0f, (float) currentUsage / requiredUsage);
    }

    // === 序列化 ===

    private CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        // 保存已学术
        ListTag learnedList = new ListTag();
        for (String id : learnedTechniques) {
            learnedList.add(StringTag.valueOf(id));
        }
        tag.put("Learned", learnedList);

        // 保存使用次数
        CompoundTag usageTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : usageCounts.entrySet()) {
            usageTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("Usage", usageTag);

        // 保存剑术/法术等级
        tag.putInt("SwordLevel", swordLevel);
        tag.putInt("MagicLevel", magicLevel);
        tag.putInt("SwordUsage", swordUsage);
        tag.putInt("MagicUsage", magicUsage);
        tag.putInt("Magic", magic);
        tag.putInt("MagicMax", magicMax);

        return tag;
    }

    private void load(CompoundTag tag) {
        learnedTechniques.clear();
        usageCounts.clear();

        // 加载已学术
        if (tag.contains("Learned")) {
            ListTag learnedList = tag.getList("Learned", 8);
            for (int i = 0; i < learnedList.size(); i++) {
                learnedTechniques.add(learnedList.getString(i));
            }
        }

        // 加载使用次数
        if (tag.contains("Usage")) {
            CompoundTag usageTag = tag.getCompound("Usage");
            for (String key : usageTag.getAllKeys()) {
                usageCounts.put(key, usageTag.getInt(key));
            }
        }

        // 加载剑术/法术等级
        swordLevel = tag.getInt("SwordLevel");
        magicLevel = tag.getInt("MagicLevel");
        swordUsage = tag.getInt("SwordUsage");
        magicUsage = tag.getInt("MagicUsage");

        if (tag.contains("MagicMax")) {
            magicMax = tag.getInt("MagicMax");
        }
        if (magicMax < 1) magicMax = 100;
        if (tag.contains("Magic")) {
            magic = tag.getInt("Magic");
        }
        if (magic < 0) magic = 0;
    }
}
