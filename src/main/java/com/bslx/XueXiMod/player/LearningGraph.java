package com.bslx.XueXiMod.player;

import com.bslx.XueXiMod.item.ModItems;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueAccess;
import com.bslx.XueXiMod.shu.TechniqueManager;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 生存学习父子关系。不在图中的术式不自动加入。 */
public final class LearningGraph {
    public enum Stage {
        MORTAL,
        MYSTIC,
        TRANSCENDENT
    }

    public record Node(String techniqueId, List<List<String>> parentGroups, Stage stage) {
        public Node {
            parentGroups = List.copyOf(parentGroups);
        }

        public boolean hasParents() {
            return !parentGroups.isEmpty();
        }
    }

    private static final Map<String, Node> NODES = new LinkedHashMap<>();

    static {
        root("sword_thrust");
        root("sword_horizontal_slash");
        root("sword_vertical_slash");
        root("sword_guard");
        root("movement_dash");
        root("movement_leap");
        root("water_weeping_arc");
        root("tide_mark");
        root("water_curtain");
        root("water_wave");
        root("fire_orb");
        root("fire_spark");
        root("fire_seed_sow");
        root("fire_kindle");
        root("rule_cast_anchor");
        root("trajectory_parallel");
        root("trajectory_drop");
        root("rule_dual_release");
        root("rule_double_cast");

        node("sword_come", Stage.MYSTIC, any("phantom_sword"));
        node("sword_array", Stage.MYSTIC, any("phantom_sword"));
        node("sword_flight", Stage.MYSTIC, any("phantom_sword"));
        node("phantom_sword_surge", Stage.MYSTIC, any("sword_array"));
        node("sword_return_all", Stage.TRANSCENDENT, any("sword_array"), any("phantom_sword_surge"));

        node("fire_detonate", Stage.MYSTIC, any("fire_seed_sow", "fire_kindle"));
        node("fire_scorch_field", Stage.MYSTIC, any("fire_seed_sow", "fire_kindle"));
        node("fire_pick_lamp", Stage.MYSTIC, any("fire_seed_sow", "fire_kindle"));
        node("fire_cool", Stage.MYSTIC, any("fire_heat_up"));
        node("fire_burn", Stage.TRANSCENDENT, any("fire_heat_up"), any("fire_detonate"));
        node("fire_liaoli", Stage.TRANSCENDENT, any("fire_pick_lamp"), any("fire_detonate"));
        node("fire_liaoyuan", Stage.TRANSCENDENT, any("fire_heat_up"), any("fire_scorch_field"));
        node("fire_fentian", Stage.TRANSCENDENT, any("fire_heat_up"), any("fire_scorch_field"));

        node("water_disperse", Stage.MYSTIC, any("tide_mark", "water_wave"));
        node("water_prison", Stage.MYSTIC, any("tide_mark", "water_wave"));
        node("water_bubble_column", Stage.MYSTIC, any("water_vortex"));
        node("water_yielding_ribbon", Stage.TRANSCENDENT,
                any("water_blade", "tide_sever"), any("water_curtain", "water_ring"));
        node("water_mist", Stage.TRANSCENDENT, any("water_vortex"), any("water_bubble_column"));
        node("water_deep_pressure", Stage.TRANSCENDENT, any("water_vortex"), any("water_bubble_column"));

        node("rule_surface_anchor", Stage.MYSTIC, any("rule_cast_anchor"));
        node("trajectory_3d_interleave", Stage.MYSTIC, any("rule_cast_anchor"));
        node("trajectory_tracking", Stage.MYSTIC, any("trajectory_parallel", "trajectory_drop"));
        node("trajectory_storm", Stage.MYSTIC, any("trajectory_parallel", "trajectory_drop"));

        node("rule_triple_release", Stage.MYSTIC, any("rule_dual_release"));
        node("rule_quadruple_release", Stage.MYSTIC, any("rule_triple_release"));
        node("rule_octuple_release", Stage.TRANSCENDENT, any("rule_quadruple_release"));
        node("rule_sixteen_release", Stage.TRANSCENDENT, any("rule_octuple_release"));
        node("rule_exhaustive_release", Stage.TRANSCENDENT, any("rule_sixteen_release"));

        node("rule_triple_cast", Stage.MYSTIC, any("rule_double_cast"));
        node("rule_quintuple_cast", Stage.TRANSCENDENT, any("rule_triple_cast"));
        node("rule_octuple_cast", Stage.TRANSCENDENT, any("rule_quintuple_cast"));
    }

    private LearningGraph() {
    }

    public static Node node(String techniqueId) {
        return NODES.get(techniqueId);
    }

    public static List<Node> nodes() {
        return List.copyOf(NODES.values());
    }

    public static Stage stage(String techniqueId) {
        Node node = node(techniqueId);
        return node == null ? null : node.stage();
    }

    public static boolean parentsSatisfied(PlayerShuData data, String techniqueId) {
        Node node = node(techniqueId);
        if (node == null || data == null) {
            return false;
        }
        for (List<String> group : node.parentGroups()) {
            boolean satisfied = false;
            for (String parent : group) {
                if (data.hasLearned(parent)) {
                    satisfied = true;
                    break;
                }
            }
            if (!satisfied) {
                return false;
            }
        }
        return true;
    }

    public static boolean canStudy(PlayerShuData data, String techniqueId) {
        Technique technique = TechniqueManager.getById(techniqueId);
        return technique != null
                && TechniqueAccess.allowsPlayerUse(technique)
                && node(techniqueId) != null
                && data != null
                && !data.hasLearned(techniqueId)
                && parentsSatisfied(data, techniqueId);
    }

    public static List<Technique> studyable(PlayerShuData data) {
        List<Technique> result = new ArrayList<>();
        for (Node node : nodes()) {
            if (!canStudy(data, node.techniqueId())) {
                continue;
            }
            Technique technique = TechniqueManager.getById(node.techniqueId());
            if (technique != null) {
                result.add(technique);
            }
        }
        return result;
    }

    public static Item slip(Stage stage) {
        return switch (stage) {
            case MORTAL -> ModItems.STUDY_SLIP_MORTAL.get();
            case MYSTIC -> ModItems.STUDY_SLIP_MYSTIC.get();
            case TRANSCENDENT -> ModItems.STUDY_SLIP_TRANSCENDENT.get();
        };
    }

    public static ShuAttribute embryo(Technique technique) {
        if (technique == null) {
            return ShuAttribute.NONE;
        }
        ShuAttribute attribute = technique.getAttribute();
        if (attribute == ShuAttribute.FIRE || attribute == ShuAttribute.WATER) {
            return attribute;
        }
        return ShuAttribute.NONE;
    }

    private static void root(String techniqueId) {
        node(techniqueId, Stage.MORTAL);
    }

    @SafeVarargs
    private static void node(String techniqueId, Stage stage, List<String>... groups) {
        List<List<String>> copied = new ArrayList<>();
        Collections.addAll(copied, groups);
        NODES.put(techniqueId, new Node(techniqueId, copied, stage));
    }

    private static List<String> any(String... techniqueIds) {
        return List.of(techniqueIds);
    }

    private static List<String> all(String first, String second) {
        throw new IllegalStateException("all is expanded at the call");
    }
}
