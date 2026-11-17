# C 色调报告

作者 C，模型 grok-4.7-high-fast。设计 2/2，实现正式复审 0/3。这次只把 D 已交付的颜色查询接到自己的载体渲染上，不是新的正式复审。未跑 Gradle、javac、打包和游戏。命名门禁退出码 1，仍只有未改的 `OriginalHumanoid.java` 第 165 行 `i` 和第 202 行 `to`。

## 绘制

`BatchCarrierVisualRenderer.render` 对每颗火焰视觉，用该帧插值后的世界坐标调用 `NetherDomainTint.sample(visual.level(), worldX, worldY, worldZ)`。热波环的查询高度比实体高 0.05，与水平绘制面相同。顶点色用返回的红绿蓝，透明度保持 255，贴图帧的 V 切分不变。

领域内为蓝 `90,160,255`，领域外为白 `255,255,255`。每帧重查，离开领域即恢复白色。不把颜色写进实体，因此 `applyFlatTint` 不能把一条火焰永久染蓝。

取色：火苗、留焰、热波、种火、引燃、引爆。不取色：土尘、地表锚点、陨岩方块模型、无伤残骸的岩体表面。陨岩没有附加火焰网格，没有按色调新造造型。陨岩飞行时的留焰仍是留焰条带，会取色。

查询走 D 的客户端探针和已同步领域，不按玩家距离猜测。旧的 `FireVisualClient`、`FireSeedFlameClient` 和 D 的领域类都没有改。

## 仍要 A 整合

色调不再要求 A 把颜色送进渲染器。领域盒子若还没同步，客户端查询会一直是白色；那一步仍是 D 注册清单里的 `NetherDomainSync.register`。

载体侧没有变化的接线：

- `MeteoriteWreckageTypes.register`、`BatchCarrierTypes.register`、地形开关、`BatchCarrierBootstrap.install()`、两个实体渲染器、三条语言键。
- `TechniqueHitSettle.sweepEarliest` 按终稿接口调用 `BatchCarrierHooks.intercept`。未接上时空中陨岩仍会接续子段。
- `fire_orb` 的 `publishedHook` 调用 `BatchFlameTrail.attach`。火球在留焰合同的移动宿主里，效果文件不归 C。

普通武器、剑实体和光束不是这份留焰合同的显式移动宿主。本轮不为它们补火径或武器陨岩。

## 改动哈希

| SHA256 | 时间 | 文件 |
| --- | --- | --- |
| 1CDDF3A9E2B76847C93C9D6E8C3012891123504FF86619252A5690BF3913738A | 2026-10-31 04:49:39 | src/main/java/com/bslx/XueXiMod/client/BatchCarrierVisualRenderer.java |

文档哈希写在 `author-c-tint-report.json`。停写。
