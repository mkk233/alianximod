package com.bslx.XueXiMod.gui;

import com.bslx.XueXiMod.network.ShuSyncPacket;
import com.bslx.XueXiMod.player.PlayerShuData;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueManager;
import com.bslx.XueXiMod.shu.TechniqueType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 术管理容器 - 纯数据展示GUI
 *
 * 功能：
 * - 显示所有已注册的术（按类型分类）
 * - 已学术显示熟练度等级和进度
 * - 未解锁术显示解锁条件提示
 * - 底部显示剑术等级和法术等级
 *
 * 客户端数据来源：ShuSyncPacket.ClientShuCache
 */
public class ShuManagementMenu extends AbstractContainerMenu {
    private final Player player;

    // 当前选中的分类
    private TechniqueType selectedCategory = TechniqueType.SWORD;

    // 客户端构造（IContainerFactory）
    public ShuManagementMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(containerId, playerInv);
    }

    // 服务端构造
    public ShuManagementMenu(int containerId, Inventory playerInv) {
        super(ModMenuTypes.SHU_MANAGEMENT.get(), containerId);
        this.player = playerInv.player;

        // 添加玩家物品栏槽位（纯展示用，不处理交互）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        // 添加快捷栏槽位
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 198));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    // === 分类切换 ===

    public void setSelectedCategory(TechniqueType type) {
        this.selectedCategory = type;
    }

    public TechniqueType getSelectedCategory() {
        return selectedCategory;
    }

    // === 数据查询（客户端使用 ClientShuCache）===

    /**
     * 获取当前分类下的所有术
     */
    public List<Technique> getTechniquesInCategory() {
        List<Technique> result = new ArrayList<>();
        for (Technique technique : TechniqueManager.playerCatalog()) {
            if (technique.getType() == selectedCategory) {
                result.add(technique);
            }
        }
        return result;
    }

    /**
     * 获取术的熟练度等级（客户端缓存）
     */
    public int getTechniqueLevel(String techniqueId) {
        int usage = ShuSyncPacket.ClientShuCache.getUsageCount(techniqueId);
        return com.bslx.XueXiMod.shu.Proficiency.getLevel(usage);
    }

    /**
     * 获取术的熟练度进度（0.0~1.0）
     */
    public float getTechniqueProgress(String techniqueId) {
        int usage = ShuSyncPacket.ClientShuCache.getUsageCount(techniqueId);
        int level = com.bslx.XueXiMod.shu.Proficiency.getLevel(usage);
        if (level >= com.bslx.XueXiMod.shu.Proficiency.LEVEL_UPPER_BOUND) return 1.0f;
        int nextThreshold = com.bslx.XueXiMod.shu.Proficiency.getUsageToNextLevel(usage) + usage;
        int prevThreshold = 0;
        // 找到当前等级的阈值
        for (int levelIndex = level; levelIndex >= 0; levelIndex--) {
            // 简化计算：使用 Proficiency 的方法
            break;
        }
        // 简单进度计算
        if (level >= com.bslx.XueXiMod.shu.Proficiency.LEVEL_UPPER_BOUND) return 1.0f;
        int needed = com.bslx.XueXiMod.shu.Proficiency.getUsageToNextLevel(usage);
        if (needed <= 0) return 1.0f;
        return Math.min(1.0f, (float) usage / (usage + needed));
    }

    /**
     * 术是否已解锁
     */
    public boolean isTechniqueUnlocked(Technique technique) {
        return ShuSyncPacket.ClientShuCache.isUnlocked(technique);
    }

    /**
     * 术是否已学习
     */
    public boolean isTechniqueLearned(String techniqueId) {
        return ShuSyncPacket.ClientShuCache.hasLearned(techniqueId);
    }

    /**
     * 获取剑术等级
     */
    public int getSwordLevel() {
        return ShuSyncPacket.ClientShuCache.getSwordLevel();
    }

    /**
     * 获取法术等级
     */
    public int getMagicLevel() {
        return ShuSyncPacket.ClientShuCache.getMagicLevel();
    }

    /**
     * 获取类型等级进度
     */
    public float getTypeLevelProgress(TechniqueType type) {
        int level = (type == TechniqueType.SWORD) ? getSwordLevel() : getMagicLevel();
        if (level >= PlayerShuData.TYPE_LEVEL_UPPER_BOUND) return 1.0f;
        int usage = (type == TechniqueType.SWORD)
                ? ShuSyncPacket.ClientShuCache.getSwordUsage()
                : ShuSyncPacket.ClientShuCache.getMagicUsage();
        int required = 10 * (level + 1);
        return Math.min(1.0f, (float) usage / required);
    }
}
