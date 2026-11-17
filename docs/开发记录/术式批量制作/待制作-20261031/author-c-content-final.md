我先读取这份续作任务说明，确认授权范围后再动手。第一段的地形和残骸保持不动。这一段补攻击载体、留焰、地表显示和条带贴图的真实分帧引用。贴图已按清单核对。接下来补载体、留焰和分帧渲染，陨岩超时只走无伤残骸。热波在没有合法目标时不应当成父段已经成立。我改成只有实际判定通过才接续子段。命名检查只剩原先未改的两处。正在把接口、注册清单和停写报告写上真实哈希。C 第二段已停写。设计额度 2/2，实现正式复审 0/3；这次自查不是正式复审。未跑 Gradle、javac、打包和游戏。命名门禁退出码 1，只有未改动的 `OriginalHumanoid.java` 第 165 行 `i` 和第 202 行 `to`。

幽冥和领域归 D。没有改 `DomainService`、`DomainInstance`、`ContactEnchantState`，也没有给幽冥登记效果或色调。

## 已落地

`BatchCarrierBootstrap.install()` 会登记火苗、热波、陨岩、地表定位，以及喷涌、错时、命中接续、留焰、种火、引燃、引爆、崩地的通过效果。不登记幽冥。它现在还没挂到共享入口；没调用之前，这些术式仍会因没有效果而被拒绝。

火苗 `seedPoints` 为 0。本体产种基数 2 只从释放登记进 `bodyScaledBase`，由 A 的结算提交一次。C 的 `onResolved` 不再调用 `ResourceHitCommit`。命中或撞块后消失，不爆炸。20 刻点燃补在 `onResolved`，因为最早扫掠不走 `onHit`。

热波按内半径 1.5、外半径 5、总高 3 判定一次，中空，打完把击退前的速度写回。父段排除名单和遮挡快照原样使用。没有合法目标时不接续子段。另有一颗 12 刻、不参与判定的空实体，用来满足宿主组的发布数量。

陨岩走释放定义上的抛物线，飞行体积 1 格。实体接触时，A 的扫掠打直接目标并提交一次资源，C 再给半径 1.5、总高 3 里其余目标补一次。撞块时对整个圆柱打一次，然后崩地、再安放。安放成功就不再留残骸。空中命中或超时只生成第一段的无伤残骸。超时不打中心、不接续，也不叫做撞地。崩地开关仍只挡破坏。

留焰挂在本段生成的火苗和陨岩上。碰撞按直径 0.5 扫整段，表现点间距 0.25、同时不超过 256。不产种、不引爆、不崩地、不放原版火。陨岩留焰用本体物理伤害，不是每段一次中心爆炸。跟踪实体在宿主结束后再留 40 刻，并持有根上下文。

八张 64×1024 竖条已复制到建议路径，渲染按 `frame / 16` 切 V。陨岩六面仍是 `meteorite_v1.png`（`E8CA06D343A6E6A60082C542869FDA9BE4B9DE040904EEF59BAF52D28CBC1D3A`）。条带哈希与清单一致：火苗 `07407B85…F58E`，留焰 `F1B0D723…5284`，热波 `3CF9F572…BBC4`，地表 `7801A451…D018`，种火 `38CFAAE9…0458`，引燃 `2490F9E2…3E73`，引爆 `F3F0FCEB…D858`，土尘 `0C7479F9…476A`。幽冥贴图不由本段引用。

着色入口是 `BatchCarrierVisualRenderer.flatTintArgb`，写入是 `BatchCarrierVisual.applyFlatTint`，默认白色。陨岩方块模型不乘这个颜色。

## 不能写成完成

1. `install()`、`BatchCarrierTypes`、`MeteoriteWreckageTypes`、两个渲染器、地形配置和语言都还要 A 接线。视觉类型未登记时，留焰和条带不会生成。残骸类型未登记或区块未加载时，残骸生成返回 false，不掉落、不强载。
2. 空中陨岩命中仍会由 `TechniqueHitSettle.sweepEarliest` 接续子段。要停掉它，须在 `classifyRespecting` 之前调用 `BatchCarrierHooks.intercept(..., MeteorContactKind.ENTITY)`，并且在 `handled()` 时不要再结算、不要再 `batchFollow`。地形接触用 `TERRAIN`，`continueChildren()` 为真时才在父结算后接续。调用片段在 `author-c-interface-v2.md`。C 没有改 A 的文件。
3. `fire_orb` 留焰未接。`FireOrbEffect` 不归 C。
4. 普通武器只收养根上下文，不会走 `publish`，所以武器上的留焰、崩地和陨岩残骸没有落地。
5. 喷涌运动仍看宿主定义自己的轨迹。通过效果不改 `applySpec`。
6. 含幽冥的槽要等 D 登记效果，否则执行到该槽会被拒绝。

基础法杖 6 槽、测试法杖 32 槽 / 1000 魔力 / 效率 15 是 A 已登记的数据，C 没有改法杖。

完整哈希、时间和所有权在 `docs/开发记录/术式批量制作/待制作-20261031/author-c-report.md`、`author-c-report.json`、`author-c-interface-v2.md`、`author-c-registration.md`、`author-c-ownership.json`。停写。
