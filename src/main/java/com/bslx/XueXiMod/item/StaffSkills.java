package com.bslx.XueXiMod.item;

import com.bslx.XueXiMod.player.PlayerShuData;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.skill.SkillType;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueAccess;
import com.bslx.XueXiMod.shu.TechniqueIds;
import com.bslx.XueXiMod.shu.TechniqueManager;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.capture.ActorCaptureCarrierGate;
import com.bslx.XueXiMod.shu.technique.release.capture.ActorCaptureMode;
import com.bslx.XueXiMod.shu.technique.release.capture.ActorCaptureTexts;
import com.bslx.XueXiMod.shu.technique.release.capture.CaptureCarrierCheck;
import com.bslx.XueXiMod.shu.technique.release.capture.CaptureCarrierDecision;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class StaffSkills {
    private static final String ROOT_KEY = "xueximod_staff_skills";
    private static final String TECHNIQUES_KEY = "Techniques";
    private static final String SLOT_PREFIX = "Slot";
    private static final String PRESERVED_KEY = "PreservedSlots";
    private static final String HELD_KEY = "MigrationHeld";

    public static final class Entry {
        public final String techniqueId;
        public final ShuAttribute embryo;

        public Entry(String techniqueId, ShuAttribute embryo) {
            this.techniqueId = techniqueId;
            this.embryo = embryo;
        }
    }

    /** 捕获相关列表的一次替换结果。失败时调用方必须保持原术式列表。 */
    public record ReplaceResult(boolean accepted, String playerText) {
        public ReplaceResult {
            if (playerText == null) {
                playerText = "";
            }
        }

        public static ReplaceResult written() {
            return new ReplaceResult(true, "");
        }

        public static ReplaceResult refused(String playerText) {
            return new ReplaceResult(false, playerText == null ? "" : playerText);
        }
    }

    public static int slotCount(ItemStack stack) {
        if (!StaffItem.holds(stack)) return 0;
        return ((StaffItem) stack.getItem()).data().slotCount();
    }

    public static int count(ItemStack stack) {
        return readTechniques(stack).size();
    }

    public static List<Entry> entries(ItemStack stack) {
        List<Entry> result = new ArrayList<>();
        ListTag list = readTechniques(stack);
        for (int entryIndex = 0; entryIndex < list.size(); entryIndex++) {
            CompoundTag entry = list.getCompound(entryIndex);
            String techniqueId = TechniqueIds.canonicalId(entry.getString("Id"));
            ShuAttribute embryo = entry.contains("Embryo")
                    ? ShuAttribute.fromId(entry.getString("Embryo"))
                    : null;
            result.add(new Entry(techniqueId, embryo));
        }
        return result;
    }

    public static boolean overflowHeld(ItemStack stack) {
        if (!StaffItem.holds(stack)) {
            return false;
        }
        int slots = slotCount(stack);
        if (slots <= 0 || slots > StaffData.SLOT_CAP) {
            return true;
        }
        CompoundTag skills = skillsTag(stack);
        if (skills == null) {
            return false;
        }
        if (skills.getBoolean(HELD_KEY)) {
            return true;
        }
        if (!skills.contains(TECHNIQUES_KEY)) {
            int counted = countOldEntries(skills);
            return counted > slots;
        }
        return skills.getList(TECHNIQUES_KEY, 10).size() > slots;
    }

    public static String signature(ItemStack stack) {
        StringBuilder builder = new StringBuilder();
        for (Entry entry : entries(stack)) {
            builder.append(entry.techniqueId).append('\n');
        }
        return builder.toString();
    }

    public static String castRefusal(ServerPlayer player, ItemStack stack) {
        if (!StaffItem.holds(stack) || player == null) {
            return null;
        }
        int slots = slotCount(stack);
        if (slots <= 0 || slots > StaffData.SLOT_CAP) {
            return "gui.xueximod.staff.bad_slots";
        }
        if (overflowHeld(stack)) {
            return "gui.xueximod.staff.overflow_cast";
        }
        String rawFault = rawEntryFault(stack);
        if (rawFault != null) {
            return rawFault;
        }
        PlayerShuData data = PlayerShuData.get(player);
        List<Entry> rawEntries = entries(stack);
        for (Entry entry : rawEntries) {
            Technique technique = TechniqueManager.getById(entry.techniqueId);
            if (technique == null) {
                return "gui.xueximod.staff.unknown";
            }
            if (!TechniqueAccess.allowsPlayerUse(technique)) {
                return TechniqueAccess.DENIAL_TEXT;
            }
            if (!data.hasLearned(technique.getId()) || !data.isUnlocked(technique)) {
                return "gui.xueximod.staff.unlearned";
            }
        }
        if (rawEntries.isEmpty()) {
            return null;
        }
        Skill skill = asSkill(stack);
        if (skill == null || !matchesSkill(stack, skill)) {
            return ActorCaptureTexts.CHAIN_MISMATCH;
        }
        CaptureCarrierCheck checked = ActorCaptureCarrierGate.inspect(skill, true, false);
        if (checked.decision() == CaptureCarrierDecision.NOT_APPLICABLE) {
            for (Entry entry : rawEntries) {
                Technique technique = TechniqueManager.getById(entry.techniqueId);
                if (technique != null && technique.getType() == TechniqueType.SWORD) {
                    return "gui.xueximod.staff.sword";
                }
            }
            return null;
        }
        if (checked.decision() == CaptureCarrierDecision.READY) {
            List<Technique> techniques = skill.getTechniques();
            for (int techniqueIndex = 0; techniqueIndex < techniques.size(); techniqueIndex++) {
                Technique technique = techniques.get(techniqueIndex);
                if (technique == null || technique.getType() != TechniqueType.SWORD) {
                    continue;
                }
                if (!ActorCaptureCarrierGate.permitsCapturedType(checked, techniqueIndex, technique)) {
                    return "gui.xueximod.staff.sword";
                }
            }
            return null;
        }
        return checked.playerText().isBlank() ? ActorCaptureTexts.STRUCTURE : checked.playerText();
    }

    public static boolean add(ItemStack stack, String techniqueId, ShuAttribute embryo) {
        if (!StaffItem.holds(stack) || overflowHeld(stack)) return false;
        int slotLimit = slotCount(stack);
        CompoundTag root = stack.getTag();
        CompoundTag skills = (root != null && root.contains(ROOT_KEY)) ? root.getCompound(ROOT_KEY) : null;
        ListTag list;
        if (skills != null && skills.contains(TECHNIQUES_KEY)) {
            list = skills.getList(TECHNIQUES_KEY, 10);
        } else if (skills != null) {
            list = flattenOldSlots(skills, slotLimit);
        } else {
            list = new ListTag();
        }
        if (list.size() >= slotLimit) return false;

        if (root == null) {
            root = stack.getOrCreateTag();
        }
        skills = root.getCompound(ROOT_KEY);
        if (!skills.contains(TECHNIQUES_KEY)) {
            skills.put(TECHNIQUES_KEY, list);
            removeOldSlots(skills);
            root.put(ROOT_KEY, skills);
        }

        CompoundTag entry = new CompoundTag();
        entry.putString("Id", TechniqueIds.canonicalId(techniqueId));
        if (embryo != null && embryo != ShuAttribute.NONE) {
            entry.putString("Embryo", embryo.getId());
        }
        list.add(entry);
        return true;
    }

    public static void remove(ItemStack stack, int index) {
        if (!StaffItem.holds(stack) || overflowHeld(stack) || index < 0) return;
        ListTag list = readTechniques(stack);
        if (index >= list.size()) return;
        list.remove(index);
    }

    public static void move(ItemStack stack, int index, int delta) {
        if (!StaffItem.holds(stack) || overflowHeld(stack) || (delta != -1 && delta != 1)) return;
        ListTag list = readTechniques(stack);
        int target = index + delta;
        if (index < 0 || index >= list.size() || target < 0 || target >= list.size()) return;
        Tag moved = list.get(index);
        list.remove(index);
        list.add(target, moved);
    }

    public static Skill asSkill(ItemStack stack) {
        if (overflowHeld(stack)) {
            return null;
        }
        ListTag list = readTechniques(stack);
        if (list.isEmpty()) return null;
        List<Technique> techniques = new ArrayList<>();
        List<ShuAttribute> embryos = new ArrayList<>();
        for (int entryIndex = 0; entryIndex < list.size(); entryIndex++) {
            CompoundTag entry = list.getCompound(entryIndex);
            Technique technique = TechniqueManager.getById(entry.getString("Id"));
            if (technique == null) continue;
            techniques.add(technique);
            embryos.add(entry.contains("Embryo")
                    ? ShuAttribute.fromId(entry.getString("Embryo"))
                    : ShuAttribute.NONE);
        }
        if (techniques.isEmpty()) return null;
        return new Skill("伞", SkillType.MAGIC, techniques, embryos);
    }

    /** 原胚字符串能对应到枚举，或空着表示无原胚。未知拼写不算无原胚。 */
    public static boolean knownEmbryoId(String embryoId) {
        if (embryoId == null || embryoId.isEmpty()) {
            return true;
        }
        for (ShuAttribute attribute : ShuAttribute.values()) {
            if (attribute.getId().equals(embryoId)) {
                return true;
            }
        }
        return false;
    }

    /** 原始条目里的未知术式或损坏原胚。不把它们收成空链。 */
    public static String rawEntryFault(ItemStack stack) {
        if (!StaffItem.holds(stack)) {
            return null;
        }
        ListTag list = readTechniques(stack);
        for (int entryIndex = 0; entryIndex < list.size(); entryIndex++) {
            CompoundTag entry = list.getCompound(entryIndex);
            String techniqueId = entry.getString("Id");
            if (techniqueId.isBlank() || TechniqueManager.getById(techniqueId) == null) {
                return ActorCaptureTexts.UNKNOWN_ENTRY;
            }
            if (entry.contains("Embryo") && !knownEmbryoId(entry.getString("Embryo"))) {
                return ActorCaptureTexts.UNKNOWN_EMBRYO;
            }
        }
        return null;
    }

    public static boolean captureEdit(ItemStack stack) {
        if (rawEntryFault(stack) != null) {
            return true;
        }
        return captureEdit(entries(stack));
    }

    public static boolean captureEdit(List<Entry> listed) {
        if (listed == null) {
            return false;
        }
        for (Entry entry : listed) {
            if (entry == null || entry.techniqueId == null || entry.techniqueId.isBlank()) {
                return true;
            }
            Technique technique = TechniqueManager.getById(entry.techniqueId);
            if (technique == null || technique.getType() == TechniqueType.SWORD) {
                return true;
            }
            ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
            if (definition != null && definition.actorCapture() != null
                    && definition.actorCapture().mode() != ActorCaptureMode.NONE) {
                return true;
            }
        }
        return false;
    }

    /** 缺原胚标签视为无原胚。未知术式返回 null，不跳过。 */
    public static List<Entry> readableEntries(ItemStack stack) {
        if (rawEntryFault(stack) != null) {
            return null;
        }
        List<Entry> readable = new ArrayList<>();
        for (Entry entry : entries(stack)) {
            if (entry == null || entry.techniqueId == null || TechniqueManager.getById(entry.techniqueId) == null) {
                return null;
            }
            ShuAttribute embryo = entry.embryo == null ? ShuAttribute.NONE : entry.embryo;
            readable.add(new Entry(entry.techniqueId, embryo));
        }
        return readable;
    }

    public static boolean matchesSkill(ItemStack stack, Skill skill) {
        List<Entry> readable = readableEntries(stack);
        if (readable == null || skill == null || skill.getTechniques() == null
                || skill.getTechniques().size() != readable.size()) {
            return false;
        }
        for (int techniqueIndex = 0; techniqueIndex < readable.size(); techniqueIndex++) {
            Entry entry = readable.get(techniqueIndex);
            Technique technique = skill.getTechniques().get(techniqueIndex);
            if (technique == null || technique.getId() == null) {
                return false;
            }
            if (!TechniqueIds.canonicalId(technique.getId()).equals(TechniqueIds.canonicalId(entry.techniqueId))) {
                return false;
            }
            if (skill.getEmbryo(techniqueIndex) != entry.embryo) {
                return false;
            }
        }
        return true;
    }

    public static Skill skillFromEntries(List<Entry> listed) {
        if (listed == null || listed.isEmpty()) {
            return null;
        }
        List<Technique> techniques = new ArrayList<>();
        List<ShuAttribute> embryos = new ArrayList<>();
        for (Entry entry : listed) {
            if (entry == null || entry.embryo == null || entry.techniqueId == null) {
                return null;
            }
            Technique technique = TechniqueManager.getById(entry.techniqueId);
            if (technique == null) {
                return null;
            }
            techniques.add(technique);
            embryos.add(entry.embryo);
        }
        return new Skill("伞", SkillType.MAGIC, techniques, embryos);
    }

    /**
     * 只在整份候选都已知、装得下时一次换上术式列表。
     * 失败不改原列表，也不动法杖魔力和其他根标签。
     */
    public static ReplaceResult replaceEntriesExact(ItemStack staff, List<Entry> candidate) {
        if (!StaffItem.holds(staff) || candidate == null) {
            return ReplaceResult.refused("伞上的条目不能写入");
        }
        if (overflowHeld(staff)) {
            return ReplaceResult.refused("伞的槽位溢出，没有改动");
        }
        int slotLimit = slotCount(staff);
        if (slotLimit <= 0 || slotLimit > StaffData.SLOT_CAP || candidate.size() > slotLimit) {
            return ReplaceResult.refused("伞的槽位不够，没有改动");
        }
        ListTag replacement = new ListTag();
        for (Entry entry : candidate) {
            if (entry == null || entry.techniqueId == null || entry.techniqueId.isBlank() || entry.embryo == null) {
                return ReplaceResult.refused(ActorCaptureTexts.UNKNOWN_EMBRYO);
            }
            String canonical = TechniqueIds.canonicalId(entry.techniqueId);
            if (canonical.isEmpty() || TechniqueManager.getById(canonical) == null) {
                return ReplaceResult.refused(ActorCaptureTexts.UNKNOWN_ENTRY);
            }
            CompoundTag stored = new CompoundTag();
            stored.putString("Id", canonical);
            if (entry.embryo != ShuAttribute.NONE) {
                stored.putString("Embryo", entry.embryo.getId());
            }
            replacement.add(stored);
        }
        CompoundTag root = staff.getOrCreateTag();
        CompoundTag skills = root.getCompound(ROOT_KEY);
        skills.put(TECHNIQUES_KEY, replacement);
        root.put(ROOT_KEY, skills);
        return ReplaceResult.written();
    }

    /** 保存后的捕获草稿提示。普通链和可释放的完整计划不提示。 */
    public static String savedChainNotice(ItemStack staff) {
        if (!captureEdit(staff)) {
            return null;
        }
        if (rawEntryFault(staff) != null) {
            return ActorCaptureTexts.NOT_RELEASABLE;
        }
        List<Entry> readable = readableEntries(staff);
        if (readable == null) {
            return ActorCaptureTexts.NOT_RELEASABLE;
        }
        if (readable.isEmpty()) {
            return null;
        }
        Skill skill = skillFromEntries(readable);
        if (skill == null) {
            return ActorCaptureTexts.NOT_RELEASABLE;
        }
        CaptureCarrierCheck checked = ActorCaptureCarrierGate.inspect(skill, true, true);
        if (checked.decision() == CaptureCarrierDecision.READY) {
            return null;
        }
        if (checked.decision() == CaptureCarrierDecision.NOT_APPLICABLE) {
            for (Technique technique : skill.getTechniques()) {
                if (technique != null && technique.getType() == TechniqueType.SWORD) {
                    return ActorCaptureTexts.NOT_RELEASABLE;
                }
            }
            return null;
        }
        if (checked.decision() == CaptureCarrierDecision.DRAFT) {
            return checked.playerText().isBlank() ? ActorCaptureTexts.MISSING_NEXT : checked.playerText();
        }
        return ActorCaptureTexts.NOT_RELEASABLE;
    }

    private static ListTag readTechniques(ItemStack stack) {
        if (!StaffItem.holds(stack)) return new ListTag();
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(ROOT_KEY)) return new ListTag();
        CompoundTag skills = root.getCompound(ROOT_KEY);
        if (!skills.contains(TECHNIQUES_KEY)) {
            return migrate(stack, root, skills);
        }
        return skills.getList(TECHNIQUES_KEY, 10);
    }

    private static CompoundTag skillsTag(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(ROOT_KEY)) {
            return null;
        }
        return root.getCompound(ROOT_KEY);
    }

    private static ListTag migrate(ItemStack stack, CompoundTag root, CompoundTag skills) {
        ListTag list = flattenOldSlots(skills, Integer.MAX_VALUE);
        int slotLimit = Math.min(slotCount(stack), StaffData.SLOT_CAP);
        if (list.size() <= slotLimit) {
            removeOldSlots(skills);
            skills.remove(HELD_KEY);
            skills.put(TECHNIQUES_KEY, list);
            root.put(ROOT_KEY, skills);
            return list;
        }
        if (!skills.contains(PRESERVED_KEY)) {
            skills.put(PRESERVED_KEY, list.copy());
        }
        skills.putBoolean(HELD_KEY, true);
        root.put(ROOT_KEY, skills);
        return new ListTag();
    }

    private static int countOldEntries(CompoundTag skills) {
        return flattenOldSlots(skills, Integer.MAX_VALUE).size();
    }

    private static ListTag flattenOldSlots(CompoundTag skills, int slotLimit) {
        ListTag list = new ListTag();
        List<Integer> slots = new ArrayList<>();
        for (String key : skills.getAllKeys()) {
            if (key.startsWith(SLOT_PREFIX) && !key.equals(PRESERVED_KEY)) {
                try {
                    slots.add(Integer.parseInt(key.substring(SLOT_PREFIX.length())));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        slots.sort(Integer::compareTo);
        for (int slotNumber : slots) {
            if (list.size() >= slotLimit) {
                break;
            }
            Skill skill = Skill.fromNbt(skills.getCompound(SLOT_PREFIX + slotNumber));
            if (skill == null) continue;
            for (int techniqueIndex = 0; techniqueIndex < skill.getTechniques().size() && list.size() < slotLimit; techniqueIndex++) {
                CompoundTag entry = new CompoundTag();
                entry.putString("Id", skill.getTechniques().get(techniqueIndex).getId());
                ShuAttribute embryo = skill.getEmbryo(techniqueIndex);
                if (embryo != ShuAttribute.NONE) {
                    entry.putString("Embryo", embryo.getId());
                }
                list.add(entry);
            }
        }
        return list;
    }

    private static void removeOldSlots(CompoundTag skills) {
        List<String> keys = new ArrayList<>();
        for (String key : skills.getAllKeys()) {
            if (key.startsWith(SLOT_PREFIX)) {
                keys.add(key);
            }
        }
        for (String key : keys) {
            skills.remove(key);
        }
    }
}
