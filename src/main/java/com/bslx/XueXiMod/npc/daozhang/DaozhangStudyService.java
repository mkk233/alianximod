package com.bslx.XueXiMod.npc.daozhang;

import com.bslx.XueXiMod.item.ShuEmbryoItem;
import com.bslx.XueXiMod.player.LearningGraph;
import com.bslx.XueXiMod.player.PlayerShuData;
import com.bslx.XueXiMod.player.StudyService;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueAccess;
import com.bslx.XueXiMod.shu.TechniqueManager;
import com.bslx.XueXiMod.shu.TechniqueType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 只校验师徒和课程，学习仍交给现有参悟。 */
public final class DaozhangStudyService {
    private DaozhangStudyService() {
    }

    public static List<DaozhangCourse> courses(ServerPlayer player) {
        List<DaozhangCourse> courses = new ArrayList<>();
        PlayerShuData data = PlayerShuData.get(player);
        for (LearningGraph.Node node : LearningGraph.nodes()) {
            Technique technique = TechniqueManager.getById(node.techniqueId());
            if (technique == null || !TechniqueAccess.allowsPlayerUse(technique)) {
                continue;
            }
            courses.add(course(data, technique, node));
        }
        return courses;
    }

    public static boolean study(ServerPlayer player, DaozhangEntity daozhang, String techniqueId) {
        if (player == null || daozhang == null || daozhang.relation(player.getUUID()) != DaozhangRelation.APPRENTICE) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.xueximod.daozhang.study_denied"), true);
            }
            return false;
        }
        Technique technique = TechniqueManager.getById(techniqueId);
        LearningGraph.Node node = LearningGraph.node(techniqueId);
        if (technique == null || node == null || !TechniqueAccess.allowsPlayerUse(technique)) {
            player.displayClientMessage(Component.translatable("message.xueximod.study.unavailable"), true);
            return false;
        }
        ItemStack embryo = findEmbryo(player, LearningGraph.embryo(technique));
        return StudyService.study(player, techniqueId, embryo);
    }

    private static DaozhangCourse course(PlayerShuData data, Technique technique, LearningGraph.Node node) {
        String techniqueId = node.techniqueId();
        return new DaozhangCourse(
                techniqueId,
                technique.getName(),
                kind(technique.getType()),
                node.stage(),
                parents(node),
                LearningGraph.parentsSatisfied(data, techniqueId),
                data.hasLearned(techniqueId),
                LearningGraph.canStudy(data, techniqueId),
                data.isUnlocked(technique),
                LearningGraph.embryo(technique),
                slipName(node.stage()));
    }

    static DaozhangCourse.Kind kind(TechniqueType type) {
        if (type == TechniqueType.MAGIC || type == TechniqueType.UNIVERSAL) {
            return DaozhangCourse.Kind.SPELL;
        }
        return DaozhangCourse.Kind.SWORD;
    }

    private static String parents(LearningGraph.Node node) {
        if (!node.hasParents()) {
            return "";
        }
        List<String> groups = new ArrayList<>();
        for (List<String> group : node.parentGroups()) {
            groups.add(String.join("/", group));
        }
        return String.join(" + ", groups);
    }

    private static String slipName(LearningGraph.Stage stage) {
        return switch (stage) {
            case MORTAL -> "item.xueximod.study_slip_mortal";
            case MYSTIC -> "item.xueximod.study_slip_mystic";
            case TRANSCENDENT -> "item.xueximod.study_slip_transcendent";
        };
    }

    private static ItemStack findEmbryo(ServerPlayer player, ShuAttribute attribute) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof ShuEmbryoItem
                    && ShuEmbryoItem.getAttribute(stack) == attribute) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
