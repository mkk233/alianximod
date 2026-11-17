# B 编译诊断返修

阶段是明确编译诊断返修，不是正式复审。设计 2/2、实现 3/3 已经用尽。本阶段额度已用完，返修结果未追加复审。没有运行 Gradle、javac、测试、游戏或打包。待重新统一编译。写入到此停止。

## 诊断

`compile-after-client-close-report.md` 记录 `:compileJava` 失败，共 11 个错误。作者 B 占 10 条，作者 A 的 `StaffSkills.java` 第 126 行另有 1 条，本次不改。

10 条都来自同一处重名。`ResourceEnchantSlice` 同时有返回载荷的静态工厂 `kindle()`、`detonate()`、`trail()`、`gloom()`、`breakGround()`，又有同名实例方法。实例方法被判定为重复定义，于是 `slice.kindle()` 这一类调用解析成静态工厂，类型变成 `ResourceEnchantSlice`，不能和布尔值做 `||`。

## 解决

静态工厂保持原名，仍返回对应载荷。实例布尔访问改为 `hasKindle()`、`hasDetonate()`、`hasTrail()`、`hasGloom()`、`hasBreakGround()`，各自返回原来的字段。五个开关、魔力 2、直接伤害 0、种火基数 10 都没有改。引燃档数和 8 条计划上限也不在这次改动里。

`ResourceEnchantCatalog.collect` 第 70–74 行改为读取上述布尔方法。`EnchantmentDefinitions.registerAll` 仍调用静态工厂，文件未改。

`ResourceEnchantLoad` 的公开布尔方法仍是 `kindle()`、`detonate()`、`trail()`、`gloom()`、`breakGround()`。这个类没有同名静态工厂，宿主继续用这组方法。

## 10 条对应

| 原诊断 | 处理 |
| --- | --- |
| Slice 第 80 行 `kindle()` 重复 | 实例方法改为 `hasKindle()`，静态 `kindle()` 保留 |
| Slice 第 84 行 `detonate()` 重复 | 实例方法改为 `hasDetonate()`，静态 `detonate()` 保留 |
| Slice 第 88 行 `trail()` 重复 | 实例方法改为 `hasTrail()`，静态 `trail()` 保留 |
| Slice 第 92 行 `gloom()` 重复 | 实例方法改为 `hasGloom()`，静态 `gloom()` 保留 |
| Slice 第 96 行 `breakGround()` 重复 | 实例方法改为 `hasBreakGround()`，静态 `breakGround()` 保留 |
| Catalog 第 70 行 `kindle \|\| slice.kindle()` | 改为 `slice.hasKindle()` |
| Catalog 第 71 行 `detonate \|\| slice.detonate()` | 改为 `slice.hasDetonate()` |
| Catalog 第 72 行 `trail \|\| slice.trail()` | 改为 `slice.hasTrail()` |
| Catalog 第 73 行 `gloom \|\| slice.gloom()` | 改为 `slice.hasGloom()` |
| Catalog 第 74 行 `breakGround \|\| slice.breakGround()` | 改为 `slice.hasBreakGround()` |

## 跨作者请求

无。作者 A、C、D 的源码没有调用 `ResourceEnchantSlice` 的实例布尔方法。他们使用的是 `ResourceEnchantLoad` 上未改名的布尔方法。`StaffSkills.java` 第 126 行仍由作者 A 处理。

## 哈希

| SHA-256 | 修改时间 | 路径 |
| --- | --- | --- |
| 7659032A12D77DD578CAA9355290959D367FC8C49222E252F42235791F411552 | 2026-10-31 12:48:21.455 | src/main/java/com/bslx/XueXiMod/shu/technique/enchant/resource/ResourceEnchantSlice.java |
| ED91E0DFAA79690378AAA1C77E23D88F3351A15624124D390570C590366F993A | 2026-10-31 12:48:22.122 | src/main/java/com/bslx/XueXiMod/shu/technique/enchant/resource/ResourceEnchantCatalog.java |
| 3010454BF5FC38FD93B617D41DFEF4CE3A589C19E6CAB71DE5F7BAA844866272 | 2026-10-31 03:36:22.103 | src/main/java/com/bslx/XueXiMod/shu/technique/EnchantmentDefinitions.java |
| 0B44F14F6EA429A88460A8EBA68BDF57A254936066187DE450BB5EB4C88E3EEF | 2026-10-31 03:34:51.273 | src/main/java/com/bslx/XueXiMod/shu/technique/enchant/resource/ResourceEnchantLoad.java |
| 0C6DF307D7F7635185AEB8C6FA024915DF4F9FD5EAC91798451136AF6219C3CC | 2026-10-31 12:49:56.856 | docs/开发记录/术式批量制作/待制作-20261031/author-b-ownership.json |

## 停止

本阶段额度已用完，返修结果未追加复审。待重新统一编译。全部停写。
