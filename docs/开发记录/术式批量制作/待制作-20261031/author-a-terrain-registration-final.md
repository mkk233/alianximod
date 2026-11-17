先读这份地形登记任务的授权范围，再按它落地并交最终报告。注册清单只有这五项。先核对 A/B/C/D 的所有权，再找现有的总线、客户端渲染和创造栏入口。所有权没有冲突。接下来只接这五项：残骸类型总线、客户端渲染、创造栏、语言和独立破坏配置。C 第一段的五项公共接线已经接到现有入口。`basic_staff` 仍是 6 槽，`test_staff` 仍是 32 槽。这不是正式复审，状态为待统一编译。本范围已停写。

## 接通位置

1. `XueXiMod.registerContent` 在 `ReleaseEntities.register` 之后只调用一次 `MeteoriteWreckageTypes.register`。`ReleaseEntities` 里没有第二个 `meteorite_wreckage`。
2. 客户端渲染只在 `XueXiMod.ClientModEvents.onRegisterEntityRenderers`。该类标了 `Dist.CLIENT`，在 `EntityRenderersEvent.RegisterRenderers` 里登记 `MeteoriteWreckageTypes.WRECKAGE` 和 `MeteoriteWreckageRenderer::new`。渲染类自己没有订阅，专用服务器不加载这个内部类。
3. 现有全局重绑入口 `EntityRendererFix.onClientTick` 补上了 `meteorite_wreckage`。实体类型对象被换掉时，残骸和另外三种自定义实体一起重新绑定。
4. `ModCreativeModTabs` 的术标签在原列表末尾追加 `ModBlocks.METEORITE.get()`。没有第二个方块或物品，也没有改 `ModBlocks`。
5. `block.xueximod.meteorite` 是陨石 / Meteorite。`entity.xueximod.meteorite_wreckage` 是陨石残骸 / Meteorite Remnant。方块物品沿用方块翻译。
6. 原来的 `Config.SPEC` 保留。另挂 `BatchTerrainBreakSwitch.SPEC` 到 `xueximod-batch-terrain.toml`。键仍是 `breakExistingBlocks`，默认 true。它只管本批破坏；安放、生成水和分海不读它。

与 B、C 的独占路径没有重叠。没有 D 的所有权文件。

## 本轮文件

| sha256 | mtime | 文件 |
| --- | --- | --- |
| 756f58010b2531487cb5729f3f44a9110e76782c815cf64a75555dc0d26519ec | 2026-10-31 04:21:41 | XueXiMod.java |
| 18299bbb497724a8de556d27af6ee58b3da77a0f304d69e0f914edb894db8b91 | 2026-10-31 04:21:44 | EntityRendererFix.java |
| f929ec031b96b725943ab0b1bae819d026c9b2b6fc86de4f8a8dcdb51ac9f1f1 | 2026-10-31 04:21:40 | ModCreativeModTabs.java |
| 048f87673c09e6fd096acb7debe8d11a286aec0f97296ea726f251107c3686c3 | 2026-10-31 04:21:40 | zh_cn.json |
| 3e9661f4fb662bc90cc6e0e128b875cf3fdc1baaf237ad5a3cc30c9468f9bc04 | 2026-10-31 04:21:41 | en_us.json |

归属现为 56 个路径。`author-a-ownership.json` 的 sha256 是 `7857f376d96443af0cc50b743dbe7e4831b20688caba8a259a2f9c3b69645d44`，mtime `2026-10-31 04:22:12`。

冻结后的本段报告：

| sha256 | mtime | 文件 |
| --- | --- | --- |
| b1f3c841847727b3379663b8fafaff4548a19599467c56db42bbed14aef0439a | 2026-10-31 04:22:29 | author-a-terrain-registration-report.md |
| d05053d8347f95e23159358e4f67d6fa0e7e36ed4f648d7a0aee2f7280ba84f7 | 2026-10-31 04:22:31 | author-a-terrain-registration-report.json |

## 检查与未接项

`tools/check-name-gate.ps1` 退出码 1。本轮文件没有新增违规。只剩未改的 `OriginalHumanoid.java` 第 165 行 `i`、第 202 行 `to`。没有跑 Gradle、javac、打包或游戏。

C 的载体还没交付，所以有伤陨岩、热波、火苗、留焰，以及 `spawnAfterSettlement` 的调用方都没接。D 的幽冥领域也没接。本段没有新网络包。

## 停写

本范围写入到此停止。不再改 B、C、D、表、资源索引或记录。
