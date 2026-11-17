# 作者 A 地形注册报告

阶段：继续实现，不是正式复审。设计 2/2，实现 0/3。没有改 B、C、D 的实现。`basic_staff` 仍是 6 槽，`test_staff` 仍是 32 槽。本报告不是复审通过。

## 接通位置

1. `XueXiMod.registerContent` 在 `ReleaseEntities.register` 之后只调用一次 `MeteoriteWreckageTypes.register(modEventBus)`。`ReleaseEntities` 没有第二个 `meteorite_wreckage`。
2. `XueXiMod.ClientModEvents.onRegisterEntityRenderers` 标了 `Dist.CLIENT`，在 `EntityRenderersEvent.RegisterRenderers` 里登记 `MeteoriteWreckageTypes.WRECKAGE` 和 `MeteoriteWreckageRenderer::new`。渲染类自身没有订阅。专用服务器不加载这个内部类。
3. 现有全局重绑入口 `EntityRendererFix.onClientTick` 补上 `meteorite_wreckage`。类型对象被换掉时，残骸渲染器和另外三种自定义实体一起重新绑上。
4. `ModCreativeModTabs` 的术标签在原列表末尾追加 `ModBlocks.METEORITE.get()`。没有第二个方块或物品。`ModBlocks` 未改。
5. 语言：`block.xueximod.meteorite` 为陨石 / Meteorite；`entity.xueximod.meteorite_wreckage` 为陨石残骸 / Meteorite Remnant。方块物品沿用方块翻译。
6. `XueXiMod` 构造里保留原来的 `Config.SPEC`，另挂 `BatchTerrainBreakSwitch.SPEC` 到 `xueximod-batch-terrain.toml`。键仍是 `breakExistingBlocks`，默认 true。这只是本批破坏开关，安放、生成水和分海不读它。

## 本轮文件

| sha256 | mtime | 文件 |
| --- | --- | --- |
| 756f58010b2531487cb5729f3f44a9110e76782c815cf64a75555dc0d26519ec | 2026-10-31 04:21:41 | XueXiMod.java |
| 18299bbb497724a8de556d27af6ee58b3da77a0f304d69e0f914edb894db8b91 | 2026-10-31 04:21:44 | EntityRendererFix.java |
| f929ec031b96b725943ab0b1bae819d026c9b2b6fc86de4f8a8dcdb51ac9f1f1 | 2026-10-31 04:21:40 | ModCreativeModTabs.java |
| 048f87673c09e6fd096acb7debe8d11a286aec0f97296ea726f251107c3686c3 | 2026-10-31 04:21:40 | zh_cn.json |
| 3e9661f4fb662bc90cc6e0e128b875cf3fdc1baaf237ad5a3cc30c9468f9bc04 | 2026-10-31 04:21:41 | en_us.json |

归属 56 个路径。`author-a-ownership.json` 的 sha256 是 `7857f376d96443af0cc50b743dbe7e4831b20688caba8a259a2f9c3b69645d44`，mtime `2026-10-31 04:22:12`。与 B、C 独占路径没有重叠。没有 D 的所有权文件。

## 检查

`tools/check-name-gate.ps1` 退出码 1。本轮文件没有新增违规。只剩未改的 `OriginalHumanoid.java` 第 165 行 `i`、第 202 行 `to`。没有跑 Gradle、javac、打包或游戏。状态是待统一编译。

## 仍等 C 和 D

C 的载体还没交付：有伤陨岩飞行、热波、火苗、留焰，以及调用 `spawnAfterSettlement` 的命中结算。D 的幽冥领域还没交付。本段没有新网络包，没有接这些未交付接口。

## 停写

本范围写入到此停止。不再改 B、C、D、表、资源索引或记录。
