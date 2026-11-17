# C 注册清单

A 统一接线。C 不改 `ModEntities`、`ReleaseEntities`、`ModItems`、`XueXiMod`、全局客户端入口和语言文件。D 的领域、幽冥同步和既有火系色调入口不在本清单。

## 总线

模组构造里，内容登记之前或同时：

1. `MeteoriteWreckageTypes.register(modBus)`
2. `BatchCarrierTypes.register(modBus)`
3. 第一段的地形开关：把 `BatchTerrainBreakSwitch` 的配置 spec 登记进 `xueximod-batch-terrain.toml`。没登记时内存默认仍是允许崩地。

`ModTechniques.registerAll()` 之后：

4. `BatchCarrierBootstrap.install()`

方块 `xueximod:meteorite` 和同名方块物品已经由 `ModBlocks.register` 放进现有的 DeferredRegister。不要再登记一次。

## 客户端

只在客户端 `EntityRenderersEvent.RegisterRenderers`：

- `MeteoriteWreckageTypes.WRECKAGE` → `MeteoriteWreckageRenderer`
- `BatchCarrierTypes.VISUAL` → `BatchCarrierVisualRenderer`

两个渲染器类自己都不订阅事件。

火焰条带的颜色由 `BatchCarrierVisualRenderer` 在每帧绘制时调用 `NetherDomainTint.sample(Level, worldX, worldY, worldZ)`。坐标是该效果插值后的世界位置；热波环再抬高 0.05，和它的绘制面一致。查询只认 D 已经同步的领域，不按玩家距离猜测，也不把结果写进实体。离开领域下一帧就是白色。

会取色的贴图：火苗、留焰、热波、种火、引燃、引爆。土尘和地表锚点固定白色。陨岩飞行体和残骸用方块模型，不着火色，也不另加火焰网格。`applyFlatTint` 不再参与绘制。

客户端探针由 D 的 `NetherDomainWisps.Setup` 自己绑定。领域盒子要能到客户端，仍要 A 按 D 的注册清单把 `NetherDomainSync.register` 接上。色调本身不再要求 A 往渲染器里塞颜色。

## 语言

交给 A 写入语言文件，C 不改：

- `block.xueximod.meteorite`
- `entity.xueximod.meteorite_wreckage`
- `entity.xueximod.batch_carrier_visual`

创造栏只接受已有的 `ModBlocks.METEORITE`。残骸和载体视觉不可召唤，不进创造栏。

## 效果标识

`install()` 登记：`fire_spark`、`fire_shock_ring`、`fire_meteorite`、`rule_surface_anchor`、`fire_upwell`、`rule_stagger`、`rule_hit_continue`、`fire_flame_trail`、`fire_seed_sow`、`fire_kindle`、`fire_detonate`、`earth_break_ground`。

不登记 `fire_nether_gloom`。该槽若被执行，在 D 登记效果之前会因没有效果而被拒绝。

## 没有新网络包

位置和贴图种类走实体同步。跟踪距离 96，更新间隔 1。类型没挂上总线时，视觉和留焰跟踪生成会返回，不强制加载区块。
