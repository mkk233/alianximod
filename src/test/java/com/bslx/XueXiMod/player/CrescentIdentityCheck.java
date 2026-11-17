package com.bslx.XueXiMod.player;

import com.bslx.XueXiMod.shu.ModTechniques;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.SkillStoneData;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueIds;
import com.bslx.XueXiMod.shu.TechniqueManager;
import com.bslx.XueXiMod.shu.technique.TechniqueEffectRegistry;
import com.bslx.XueXiMod.shu.technique.effects.WaterFormEffect;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.ReleaseTag;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.skill.Skill;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

/**
 * 调用不依赖物品栈的生产登记和 NBT 序列化。
 * 法杖、收纳和术式石物品要在正常 Forge 里测，这里不启动。
 */
public final class CrescentIdentityCheck {
    private CrescentIdentityCheck() {
    }

    public static void main(String[] arguments) {
        ModTechniques.registerAll();
        canonicalStrings();
        registry();
        skillChain();
        stoneSnapshot();
        learnedAndUsage();
        System.out.println("缺月身份兼容检查通过");
        System.out.println("断言 " + assertionCount);
    }

    private static void canonicalStrings() {
        check("water_blade".equals(TechniqueIds.canonicalId("water_open_ring")), "旧环编号指向水刃");
        check("water_blade".equals(TechniqueIds.canonicalId("water_blade")), "现行编号保持不变");
        check("unknown_ring_technique".equals(TechniqueIds.canonicalId("unknown_ring_technique")), "未知编号原样");
        check("water_open_ring_extra".equals(TechniqueIds.canonicalId("water_open_ring_extra")), "更长编号不做子串替换");
        check("缺月".equals(TechniqueIds.canonicalId("缺月")), "显示名不是编号");
        check(TechniqueIds.canonicalId(null) == null, "空编号保持空");
    }

    private static void registry() {
        Technique blade = TechniqueManager.getById("water_blade");
        Technique aliased = TechniqueManager.getById("water_open_ring");
        check(blade != null && blade == aliased, "新旧编号是同一个术式对象");
        check("water_blade".equals(aliased.getId()), "对象编号仍是水刃");
        check("缺月".equals(aliased.getName()), "显示名是缺月");
        check("随挥剑扫出月牙形水流弧斩，可连续衔接".equals(aliased.getDescription()), "描述是月牙弧斩");
        check(aliased.getBaseDamage() == 7f, "伤害仍是 7");
        check(aliased.getBaseCooldown() == 14, "新实例冷却仍是 14");
        check(aliased.manaCost() == 14, "魔力仍是 14");
        check(TechniqueManager.getById("unknown_ring_technique") == null, "未知术式仍然查不到");
        check(TechniqueManager.getById("缺月") == null, "显示名查不到术式");

        int bladeCount = 0;
        int ringCount = 0;
        for (Technique technique : TechniqueManager.getAll()) {
            if ("water_blade".equals(technique.getId())) {
                bladeCount++;
            }
            if ("water_open_ring".equals(technique.getId())) {
                ringCount++;
            }
        }
        check(bladeCount == 1 && ringCount == 0, "枚举里只有一个规范对象");

        ReleaseDefinition definition = ReleaseRegistry.get("water_open_ring");
        check(definition == ReleaseRegistry.get("water_blade"), "释放定义也走同一编号");
        check(definition != null && "water_blade".equals(definition.id()), "释放定义编号是水刃");
        check(definition.trajectory() == Trajectory.STRAIGHT, "别名不是旧函数环");
        check(definition.lifetimeTicks() == 10, "寿命仍是 10");
        check(definition.speed() == 1.3f, "速度仍是 1.3");
        check(definition.energyScale() == 1.35f, "能量仍是 1.35");
        check(definition.heavyMomentumPerCopy() == 0f, "缺月不再预留重水");
        check(definition.pressureWetnessMinimum() == 0, "缺月不再挂重压潮湿门槛");
        check(definition.wetnessGrant() == 6, "潮湿仍是 6");
        check(definition.requiresHeldSword() && definition.usesArcBand() && definition.tracksBeatCycle(), "持剑弧带和三拍还在");
        check(definition.separatesCohortVisuals(), "三份分道还在");
        check(TechniqueEffectRegistry.get("water_open_ring") instanceof WaterFormEffect, "效果是共用水系生成");
        check(TechniqueEffectRegistry.get("water_open_ring") == TechniqueEffectRegistry.get("water_blade"), "效果表只有一份");
        check(TechniqueManager.getById("water_weeping_arc") != null, "垂丝仍在");
        ReleaseDefinition weeping = ReleaseRegistry.get("water_weeping_arc");
        check(weeping != null && weeping.trajectory() == Trajectory.FUNCTION, "垂丝函数弹道还在");
        check(weeping.kind() == ReleaseKind.HIT_VOLUME, "垂丝仍是命中体积");
        check(weeping.tag() == ReleaseTag.TRIGGER, "垂丝触发方式未改");
        check(weeping.moving(), "垂丝仍会移动");
        check(weeping.lifetimeTicks() == 40, "垂丝寿命仍是 40");
        check(weeping.speed() == 0f, "垂丝速度仍是 0");
        check(weeping.thickness() == 0.35f, "垂丝厚度仍是 0.35");
        check(weeping.pullAfterA() == 0, "垂丝不额外拉段");
        check(weeping.role() == ChainRole.ATTACK, "垂丝仍是攻击");
        check(!weeping.trajectoryMutable(), "垂丝弹道仍不可改");
        check(!weeping.manipulatesSwordEntities(), "垂丝不操纵剑实体");
        check(weeping.energyScale() == 1f && weeping.chainEnergyScale() == 1f, "垂丝能量缩放未改");
        check(weeping.heavyMomentumPerCopy() == 0f, "垂丝不再预留重水");
        check(weeping.pressureWetnessMinimum() == 0, "垂丝不再挂重压潮湿门槛");
        check(weeping.wetnessGrant() == 3, "垂丝潮湿仍是 3");
        check(!weeping.separatesCohortVisuals(), "垂丝不分道");
        ReleaseDefinition deepPressure = ReleaseRegistry.get("water_deep_pressure");
        check(deepPressure != null && deepPressure.domain() != null, "深海重压领域还在");
        check(deepPressure.heavyMomentumPerCopy() == 0f && deepPressure.pressureWetnessMinimum() == 0,
                "深海重压没有接上垂丝退出的重水门槛");
    }

    private static void skillChain() {
        CompoundTag skillTag = new CompoundTag();
        skillTag.putString("Name", "月牙连斩");
        skillTag.putString("Type", "SWORD");
        ListTag techniques = new ListTag();
        techniques.add(techniqueEntry("water_open_ring", "fire"));
        techniques.add(techniqueEntry("unknown_ring_technique", "wood"));
        techniques.add(techniqueEntry("sword_thrust", "metal"));
        techniques.add(techniqueEntry("water_open_ring", "water"));
        skillTag.put("Techniques", techniques);

        Skill skill = Skill.fromNbt(skillTag);
        check("月牙连斩".equals(skill.getName()), "自定义技名还在");
        check(skill.getTechniques().size() == 3, "未知段仍被跳过，重复段还在");
        check("water_blade".equals(skill.getTechniques().get(0).getId()), "第一段变成规范编号");
        check("sword_thrust".equals(skill.getTechniques().get(1).getId()), "中间段顺序不变");
        check("water_blade".equals(skill.getTechniques().get(2).getId()), "第三段仍是独立的一份");
        check(skill.getEmbryo(0) == ShuAttribute.FIRE, "第一段胚体还在");
        check(skill.getEmbryo(1) == ShuAttribute.METAL, "中间胚体还在");
        check(skill.getEmbryo(2) == ShuAttribute.WATER, "第三段胚体还在");

        CompoundTag written = skill.toNbt();
        check("月牙连斩".equals(written.getString("Name")), "写回仍用自定义技名");
        ListTag writtenList = written.getList("Techniques", 10);
        check(writtenList.size() == 3, "写回段数不变");
        check("water_blade".equals(writtenList.getCompound(0).getString("Id")), "写回第一段是规范编号");
        check("fire".equals(writtenList.getCompound(0).getString("Embryo")), "写回第一段胚体");
        check("sword_thrust".equals(writtenList.getCompound(1).getString("Id")), "写回中间编号不变");
        check("metal".equals(writtenList.getCompound(1).getString("Embryo")), "写回中间胚体");
        check("water_blade".equals(writtenList.getCompound(2).getString("Id")), "写回第三段是规范编号");
        check("water".equals(writtenList.getCompound(2).getString("Embryo")), "写回第三段胚体");
        check("water_open_ring".equals(skillTag.getList("Techniques", 10).getCompound(0).getString("Id")), "未写回的原件仍是旧编号");
    }

    private static void stoneSnapshot() {
        CompoundTag stoneTag = new CompoundTag();
        stoneTag.putString("spell_id", "water_open_ring");
        stoneTag.putString("embryo_attribute", "fire");
        CompoundTag snapshot = new CompoundTag();
        snapshot.putFloat("damage", 6.5f);
        snapshot.putInt("cooldown", 20);
        snapshot.putFloat("projectile_speed", 0f);
        stoneTag.put("snapshot", snapshot);

        SkillStoneData stone = SkillStoneData.fromNbt(stoneTag);
        check(stone != null, "旧石能解析");
        check("water_blade".equals(stone.getSpellId()), "石的内存编号已规范化");
        check(stone.getTechnique() == TechniqueManager.getById("water_blade"), "旧石解析到新对象");
        check(stone.getEmbryoAttribute() == ShuAttribute.FIRE, "石的胚体还在");
        check(stone.getDamage() == 6.5f, "快照伤害不重算");
        check(stone.getCooldown() == 20, "旧快照冷却仍是 20");
        check(stone.getProjectileSpeed() == 0f, "旧快照弹速仍是 0");
        check("water_open_ring".equals(stoneTag.getString("spell_id")), "未写回的原件仍是旧编号");

        CompoundTag stoneWritten = stone.toNbt();
        check("water_blade".equals(stoneWritten.getString("spell_id")), "石写回规范编号");
        check("fire".equals(stoneWritten.getString("embryo_attribute")), "石写回胚体");
        CompoundTag writtenSnapshot = stoneWritten.getCompound("snapshot");
        check(writtenSnapshot.getFloat("damage") == 6.5f, "写回伤害仍是快照");
        check(writtenSnapshot.getInt("cooldown") == 20, "写回冷却仍是快照");
        check(writtenSnapshot.getFloat("projectile_speed") == 0f, "写回弹速仍是快照");

        SkillStoneData fresh = SkillStoneData.imprint(TechniqueManager.getById("water_blade"), ShuAttribute.NONE);
        check(fresh.getCooldown() == 14, "新刻印冷却仍是 14");
        check(fresh.getProjectileSpeed() == 1.3f, "新刻印弹速仍是 1.3");
        check("water_blade".equals(fresh.getSpellId()), "新刻印编号是水刃");
    }

    private static void learnedAndUsage() {
        CompoundTag exactTag = playerTag(4, 6);
        PlayerShuData exact = new PlayerShuData();
        exact.load(exactTag);
        check(exact.getUsageCount("water_open_ring") == 10, "用法次数按规范编号相加");
        check(exact.getUsageCount("water_blade") == 10, "新旧查询是同一份次数");
        check(exact.getUsageCount("unknown_ring_technique") == 4, "未知用法次数不变");
        check(!exact.getAllUsageCounts().containsKey("water_open_ring"), "内存里不再留旧用法键");
        exact.load(exactTag);
        check(exact.getUsageCount("water_blade") == 10, "重复载入不再相加");
        CompoundTag exactSaved = exact.save();
        check(!exactSaved.getCompound("Usage").contains("water_open_ring"), "写出不留旧用法键");
        check(exactSaved.getCompound("Usage").getInt("water_blade") == 10, "写出的是相加后的次数");
        check(exactSaved.getCompound("Usage").getInt("unknown_ring_technique") == 4, "未知用法键照写");
        PlayerShuData exactAgain = new PlayerShuData();
        exactAgain.load(exactSaved);
        exactAgain.load(exactAgain.save());
        check(exactAgain.getUsageCount("water_blade") == 10, "再读再写次数不变");
        exactAgain.incrementUsage("water_open_ring");
        check(exactAgain.getUsageCount("water_blade") == 11, "旧编号的新次数记到规范键");
        check(!exactAgain.save().getCompound("Usage").contains("water_open_ring"), "增加次数后仍只写规范键");

        check(exact.hasLearned("water_open_ring") && exact.hasLearned("water_blade"), "新旧已学查询都成立");
        check(exact.hasLearned("unknown_ring_technique"), "未知已学仍保留");
        check(exact.getLearnedTechniqueIds().size() == 2, "已学新旧键并成一个");
        check(!exact.getLearnedTechniqueIds().contains("water_open_ring"), "已学集合里是规范键");
        check(exact.getLearnedTechniques().size() == 1, "未知编号仍不进术式列表");
        check(exact.hasDomainPermit("water_open_ring") && exact.hasDomainPermit("mist_permit"), "旧许可并到规范键");
        exact.grantDomainPermit("water_open_ring");
        ListTag permitList = exact.save().getList("DomainPermits", 8);
        check(permitList.size() == 2, "域许可不增加新语义");
        check(listContains(permitList, "water_blade") && listContains(permitList, "mist_permit"), "许可写出现行键和原许可");
        check(!listContains(permitList, "water_open_ring"), "许可写出不留旧键");
        check(exact.getSwordLevel() == 2 && exact.getMagicLevel() == 1, "等级旁路还在");
        check(exact.getSwordUsage() == 11 && exact.getMagicUsage() == 5, "熟练点旁路还在");
        check(exact.getMagic() == 40 && exact.getMagicMax() == 80, "魔力旁路还在");
        check(exact.getWaterMomentum() == 12.5f, "水势旁路还在");
        check(exact.domainMigrateVersion() == 3, "领域迁移版本旁路还在");
        check(exactSaved.getInt("SwordLevel") == 2 && exactSaved.getInt("Magic") == 40, "写出旁路字段");

        PlayerShuData overflow = new PlayerShuData();
        overflow.load(playerTag(Integer.MAX_VALUE, 8));
        check(overflow.getUsageCount("water_blade") == Integer.MAX_VALUE, "用法相加停在整数上限");
        overflow.load(overflow.save());
        check(overflow.getUsageCount("water_blade") == Integer.MAX_VALUE, "上限再读不再回绕");
    }

    private static CompoundTag playerTag(int ringUsage, int bladeUsage) {
        CompoundTag playerTag = new CompoundTag();
        ListTag learned = new ListTag();
        learned.add(StringTag.valueOf("water_open_ring"));
        learned.add(StringTag.valueOf("water_blade"));
        learned.add(StringTag.valueOf("unknown_ring_technique"));
        playerTag.put("Learned", learned);
        ListTag permits = new ListTag();
        permits.add(StringTag.valueOf("water_open_ring"));
        permits.add(StringTag.valueOf("mist_permit"));
        playerTag.put("DomainPermits", permits);
        playerTag.putInt("DomainMigrateVersion", 3);
        CompoundTag usage = new CompoundTag();
        usage.putInt("water_open_ring", ringUsage);
        usage.putInt("water_blade", bladeUsage);
        usage.putInt("unknown_ring_technique", 4);
        playerTag.put("Usage", usage);
        playerTag.putInt("SwordLevel", 2);
        playerTag.putInt("MagicLevel", 1);
        playerTag.putInt("SwordUsage", 11);
        playerTag.putInt("MagicUsage", 5);
        playerTag.putInt("Magic", 40);
        playerTag.putInt("MagicMax", 80);
        playerTag.putFloat("WaterMomentum", 12.5f);
        return playerTag;
    }

    private static CompoundTag techniqueEntry(String techniqueId, String embryoId) {
        CompoundTag entry = new CompoundTag();
        entry.putString("Id", techniqueId);
        if (embryoId != null && !embryoId.isEmpty() && !"none".equals(embryoId)) {
            entry.putString("Embryo", embryoId);
        }
        return entry;
    }

    private static boolean listContains(ListTag list, String expected) {
        for (int entryIndex = 0; entryIndex < list.size(); entryIndex++) {
            if (expected.equals(list.getString(entryIndex))) {
                return true;
            }
        }
        return false;
    }

    private static int assertionCount;

    private static void check(boolean passed, String label) {
        assertionCount++;
        if (!passed) {
            throw new IllegalStateException(label);
        }
    }
}
