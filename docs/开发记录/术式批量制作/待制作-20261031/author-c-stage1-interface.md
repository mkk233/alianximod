# C 第一段接口：地形、无伤残骸、陨石方块

本段没有攻击、产种或接续方法。调用方必须先完成父子结算，再调用生成残骸。本段不读取施法帧。

## 稳定签名

```text
boolean MeteoriteWreckageEntity.spawnAfterSettlement(
    ServerLevel level,
    UUID ownerIdentifier,
    Vec3 spawnPosition,
    Vec3 initialMotion,
    boolean allowOnce)
```

`allowOnce == true` 表示这颗残骸接地后可以崩地一次。`false` 只安放、不挖坑。`initialMotion == null` 时初速为 0。这不是有伤飞行参数，不包含 0.8 格/tick、上偏 15 度或重力 0.08。类型还没挂到总线，或出生点区块未加载时返回 false，不强载。

```text
BreakGroundResult TerrainMutationService.breakExistingOnce(
    ServerLevel level,
    UUID ownerIdentifier,
    BlockPos impact,
    boolean allowOnce,
    boolean alreadyExecuted,
    Runnable markBeforeBlocks)

PlaceOutcome TerrainMutationService.placeMeteorite(
    ServerLevel level,
    UUID ownerIdentifier,
    BlockPos impact)
```

`PlaceOutcome` 是 `(PlaceResult placeResult, BlockPos placedCell)`。只有 `PLACED` 时 `placedCell` 非空。

`breakExistingOnce` 使用 `BreakGroundShape.offsets()` 的现成顺序，不另写坑形，也不再截断 32。坑心是 `impact`，东、下、南偏移来自 `BreakOffset`。

主人不在线、已离开、已死亡、是 `FakePlayer`，或处于冒险、旁观等不能建造的模式时，不会调用 `markBeforeBlocks`。开关关闭同样不调用它。只有开关开启且主人可操作时，才先运行 `markBeforeBlocks`，再破坏方块。残骸把「崩地已执行」写在这个回调里。

`placeMeteorite` 不读取破坏开关。

```text
boolean BatchTerrainBreakSwitch.allowsBreakingExisting()
void BatchTerrainBreakSwitch.applyConfigured(boolean enabled)
ForgeConfigSpec BatchTerrainBreakSwitch.SPEC
String BatchTerrainBreakSwitch.CONFIG_FILE = "xueximod-batch-terrain.toml"
```

内存默认 true。配置键 `breakExistingBlocks`。公共注册把 `SPEC` 挂上之后，已有的 `ModConfigEvent` 监听会写入内存。没挂上时保持默认开启。生成水、分海不读这个开关。

```text
void MeteoriteWreckageTypes.register(IEventBus eventBus)
RegistryObject<EntityType<MeteoriteWreckageEntity>> MeteoriteWreckageTypes.WRECKAGE
String MeteoriteWreckageTypes.ENTITY_ID = "meteorite_wreckage"

RegistryObject<Block> ModBlocks.METEORITE
```

方块注册名 `xueximod:meteorite`。实体注册名 `xueximod:meteorite_wreckage`。方块物品由 `ModBlocks.registerBlock` 用同名写进现有物品 DeferredRegister，不要再登记第二个物品。

残骸只读访问：`ownerId()`、`wreckageId()`、`breakDone()`、`awaitingPlacement()`。

渲染器构造：`MeteoriteWreckageRenderer(EntityRendererProvider.Context)`。贴图常量 `MeteoriteWreckageRenderer.BLOCK_TEXTURE`，指向 `textures/block/meteorite_v1.png`。

## 行为

- 方块硬度 3、抗爆 6，`requiresCorrectToolForDrops`。镐可挖，且在 `minecraft:needs_stone_tool`。工具不够不掉。战利品是自身 1 个。没有燃料、矿物或配方。
- 崩地逐块检查世界边界、已加载区块、`mayInteract`、冒险/旁观限制。跳过空气、流体、负硬度、方块实体、`GameMasterBlock`、`BlockTags.PORTALS`、`BlockTags.WITHER_IMMUNE`、下界门、末地门、末地门框、末地折跃门。
- 破坏事件是本地 Forge 1.20.1 的 `BlockEvent.BreakEvent(Level, BlockPos, BlockState, Player)`，带 `@Cancelable`。取消则不改这块。通过后用 `Level.destroyBlock`。不需要正确工具的块用空工具掉落；需要工具的块不掉落。不生成经验球，不消耗玩家工具，不调用世界爆炸，不伤害生物。
- 安放先试撞击块正上方，再在水平距离平方不超过 4、上下各 2 的位置里由近到远。格子必须是完整空气、下方有朝上的坚实面、没有活着的非旁观生物。不覆盖已有块。
- 放置事件是 `BlockEvent.EntityPlaceEvent(BlockSnapshot, BlockState, Entity)`，带 `@Cancelable`。本地构造对玩家读取快照的当前世界状态，所以顺序是先快照、再 `setBlock`、再发事件。取消或结果不是陨石时 `BlockSnapshot.restore(true, false)`。只有事件没取消且该格确实是陨石才算放好。
- 残骸每 20 tick 重试。不超时删除，掉出世界时拉回最低建造高度，不强载。方块真实放好后才 `discard`。`PlacementCommitted` 为真时加载后直接删除，避免再放一块或再挖一次。
- 未加载的碗沿方块在这一次里跳过，标记已经写下，不再补挖，避免已挖部分重复掉落。

## 存档键

`OwnerUuid`、`WreckageId`、`BreakRequested`、`BreakDone`、`AwaitingPlacement`、`PlacementCommitted`、`ImpactReady`、`ImpactX`、`ImpactY`、`ImpactZ`、`CommittedX`、`CommittedY`、`CommittedZ`、`SettleCooldown`。

## 本段没有的方法

没有命中、伤害、产种、引燃、引爆、接续、领域周期、留焰采样或热波。不要把 `spawnAfterSettlement` 当成攻击回调。
