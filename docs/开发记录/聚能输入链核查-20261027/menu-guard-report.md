# 剑技菜单法术门禁

只改了 `SpellAssemblyMenu.java`。没有跑 Gradle，没有打开界面。

## 触发

剑技菜单构造时 `staff` 取当时主手是否为法杖，之后不再变。打开时主手不是法杖，`staff` 为 false。界面仍开着时，快捷栏可以换成允许法术的法杖。`rejectReason` 和 `writeGear` 的非法杖分支原先用当前主手 `SkillCarrier.allowsMagic` 决定 `TechniqueType.MAGIC` 能否进入剑技槽。这时 `staff` 仍是 false，当前手却已经允许法术，MAGIC 聚能会被写进 `SkillType.SWORD`。释放侧还有 BASE 兜底，装配约束已经被绕过。

## 改动

两处都在 `staff == false` 的路径上，不再看当前主手：

- `rejectReason`：非法杖分支遇到 `TechniqueType.MAGIC` 直接返回「无法装配：法术不能装入剑技槽」。
- `writeGear`：非法杖分支遇到 `TechniqueType.MAGIC` 直接跳过，不写入剑技槽。

`staff == true` 的法杖分支未改。UNIVERSAL、MOVEMENT 等其它兼容未改。没有按聚能 id 写特判，没有封快捷栏，没有重做菜单。

改前快照在 `before/src/main/java/com/bslx/XueXiMod/gui/SpellAssemblyMenu.java`。`before-eligibility` 里的副本没有覆盖。

实际 UI 没有启动验证。按约定没有为这两行分支写镜像断言。
