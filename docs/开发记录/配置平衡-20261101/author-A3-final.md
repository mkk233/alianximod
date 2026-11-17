先读 A3 任务说明和它点名的合同，再按授权范围改。A3 只补上次报告里的遗漏。先看这些展示和段间隔的现有读法，以及 C2 已收束的接口。段间隔和这些说明仍在读原始冷却或底伤。先留下改前副本，再只把这些读数接到已生效快照。A3 只补上上次报告里的展示和段间隔遗漏。没有做正式复审，也没有重开架构。未编译、未启动游戏、未提交。写入已收束，所有权已释放。

## 已完成

`TechniqueChain.gapBefore` 先读 `cooldownTicks`，再乘原来的反应倍率，附魔段仍按原样至少加 1 刻。前一段攻击的分支仍用原始 `getBaseDamage() > 0` 决定是否采用，冷却数字则走已生效基础冷却。法杖缩放仍留在 `SkillUsePacket` 对这个返回值调用的 `scaleCooldown`，全局冷却只在 `cooldownTicks` 里乘一次。`Technique.getBaseCooldown()` 没有改。

说明和术石改为读当前快照：

- 术管理界面和 `/shu list` 显示已生效的基础伤害与基础冷却。
- 技能书正文、技能书提示、已绑定技提示，以及 `/shu book` 的合计，改为 `displayedTotalDamage` 和 `displayedTotalCooldown`。冷却先取已生效基础值，再乘原来的反应倍率。
- 术石提示在术式仍存在时按当前快照重算；编号已经不在目录里时，沿用刻印存档里的旧数字。

`Skill.getTotalBaseDamage()`、`getTotalBaseCooldown()` 和 `SkillStoneData.imprint()` 仍按原始字段计算。术石 NBT 继续保存刻印时的结构快照，没有把当前有效值写成新的原始默认，也没有改旧物品的存档。

剑来和万剑归宗走 `shownStructuralDamage`：面板数字仍是结构底伤，不进入伤害倍率，也不写成武器物品伤害。

`fieldSpawnCost` 没有新增键。

## 改动路径

- `shu/technique/chain/TechniqueChain.java`
- `skill/Skill.java`
- `shu/SkillStoneData.java`
- `item/SkillStoneItem.java`
- `item/SkillBookItem.java`
- `event/BoundSkillTooltip.java`
- `gui/ShuManagementScreen.java`
- `command/ShuCommands.java`
- `shu/balance/BalanceDisplay.java`

改前字节在 `docs/开发记录/配置平衡-20261101/pre-A/` 的对应相对路径。`author-A-manifest.json` 已记入 A3。

## 仍未覆盖

- `/shu` 里用 `getBaseDamage() > 0` 判断载具是否匹配的两处仍读结构底伤。那是结构分支，不是说明数字。
- `SpellAssemblyScreen` 和 `ImprintTreeView` 在 A2 已经走平衡读取。剑来和万剑归宗没有伤害键，那里显示的仍是结构底伤。
- `fieldSpawnCost` 仍按原始值乘份数。
- 段间隔以外的战斗扣费、火伤和剑实体物品伤害仍在 B/C 的文件里。`LiaoliEffect` 读取 `getBaseDamage()` 是公式输入，不在这次授权名单里，没有改。

## 命名门禁与编译

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 1。只有 `OriginalHumanoid.java` 第 165 行的 `i` 和第 202 行的 `to`。本次改过的文件没有违规，没有改那个旧文件。

待统一编译。这次是遗漏收尾，不是实现复核。
