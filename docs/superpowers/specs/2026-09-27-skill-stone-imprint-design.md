# 技能石刻印 设计（第一期）

日期：2026-09-27

## 目标

删除撰写台与卷轴。刻印台把「一个术」写入原胚，产出技能石。不做装配与释放。

## 物品

- 原胚：唯一 ID `shu_embryo`，NBT `Attribute`（none / wood / fire / water）决定名称、贴图、堆叠。
- 技能石：唯一 ID `skill_stone`，`stacksTo(1)`。
- 技能石 NBT：`spell_id`、`embryo_attribute`、`snapshot{damage,cooldown,projectile_speed}`。
- 属性匹配在刻印时打进 snapshot（伤害 ×1.10，冷却 ×0.90），不另存 bonus 标志。
- 现有 9 个术：复杂度 0、属性 none。木原胚可刻，暂无匹配加成。

## 刻印台

放入原胚 → 列出已学会的术 → 预览匹配 → 确认消耗原胚产出技能石。

## 不做

装配、释放、Noita 拼接、法杖/魔导书/发射器。
