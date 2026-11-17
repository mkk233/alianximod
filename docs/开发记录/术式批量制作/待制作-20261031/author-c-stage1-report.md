# C 第一段最终报告

阶段：已批准内容的实现。设计 2/2，正式实现复审未使用，仍是 0/3。模型 grok-4.7-high-fast。没有派代理，没有读 A 的实时日志或未交付源码，没有改 A/B 的源文件。本段写入已经停止。

## 结论

陨石方块、本批破坏开关、权限内的一次崩地、独立安放，以及无伤待安放残骸已经落在 C 自己的文件里。方块会随现有 `ModBlocks.register` 进入游戏。残骸实体类型、渲染器、创造标签、语言和开关配置文件还要 A 按 `author-c-stage1-registration.md` 接上。没接上之前，残骸生成会返回 false。

有伤陨岩飞行、热波、火苗、留焰、幽冥和领域都没做。统一编译没跑。

## 已实现

- `xueximod:meteorite`：硬度 3，抗爆 6，真实碰撞，方块状态和立方体模型会进存档。石镐及以上掉自身 1 个，工具不足不掉。没有燃料、矿物或配方。
- 六面贴图是批准的 32×32 `meteorite-block-texture.png`，复制到 `assets/xueximod/textures/block/meteorite_v1.png`。复制后 SHA256 仍是 `E8CA06D343A6E6A60082C542869FDA9BE4B9DE040904EEF59BAF52D28CBC1D3A`。64×1024 的 `meteorite-strip.png` 没有复制，也没有被模型或渲染器引用。其他 16 帧效果条带留在资源库正式目录，本段没有假渲染引用。
- 破坏开关默认开启，只被 `breakExistingOnce` 读取。`placeMeteorite` 不读。生成水、分海和其他旧世界修改没有改，也不能把这个开关说成全模组统一。
- 崩地只遍历 B 已交付的 `BreakGroundShape.offsets()`。不复制浅碗算法，不再做 32 的第二次截断。B 的合同写当前是 23 块；本段没有另外执行计数。
- 无伤残骸只保存主人、唯一身份、崩地标记和待安放标记。落地后才处理。不伤害、不产种、不接续。每 20 tick 重试，不超时删除，不强载。方块真实放好才删除残骸。

## 开关怎么接

`BatchTerrainBreakSwitch.allowsBreakingExisting()` 的内存初值是 true。A 若要让它出现在配置里，注册 `SPEC` 到 `xueximod-batch-terrain.toml`。键名 `breakExistingBlocks`。类上已经有 `ModConfigEvent` 监听，只处理这份 spec。也可以直接调用 `applyConfigured`。

主人在线且允许建造、开关也开着时，先把残骸的崩地标记写成已执行，再挖。主人离线、是假玩家、已死亡，或冒险/旁观不能建造时，不写标记，20 tick 后再看。开关关着时也不写标记：这一次不挖；若同一次安放成功，残骸删除，之后不再补坑。若安放还没成功，残骸留下，开关稍后打开还可以挖一次。

## 权限

本地 Forge `1.20.1-47.3.33` 映射包里的签名：

- `BlockEvent.BreakEvent(Level, BlockPos, BlockState, Player)`，`@Cancelable`。没有单独的 `PlaceEvent`。
- `BlockEvent.EntityPlaceEvent(BlockSnapshot, BlockState, Entity)`，`@Cancelable`。对玩家，`getPlacedBlock()` 读的是事件发生时的世界方块。

因此放置是先记 `BlockSnapshot`，再 `setBlock`，再发事件；取消则 `restore(true, false)`。成功条件是事件没取消且该格仍然是陨石。没有用 `setBlock` 的返回值代替权限，也没有造假玩家。

破坏在事件通过之后调用 `Level.destroyBlock`。空工具决定掉落：不要求工具的块才掉，要求工具的块不掉。不打经验、不耗工具、不爆炸、不伤生物。世界边界、未加载区块、出生保护 `mayInteract`、冒险限制、流体、负硬度、方块实体、命令方块、传送门和凋灵免疫方块会跳过。未加载的碗沿在这一次里放弃，避免已挖的格子重载后再掉一次。

## 注册现状

| 对象 | 现状 |
| --- | --- |
| 方块与方块物品 | 已进 `ModBlocks`，物品走原有 DeferredRegister |
| 创造标签、语言 | 未改，清单交给 A |
| 实体类型 | 类已写好，总线未挂，生成返回 false |
| 渲染器 | 类已写好，没有自注册 |
| 开关配置文件 | 默认开启；toml 未挂到 `XueXiMod` |
| 网络包 | 没有新包 |

## 明确没接上的能力

- 陨岩有伤飞行、初速 0.8、上偏 15 度、重力 0.08、100 tick 命中和中心冲击。
- 热波、火苗、留焰几何、幽冥周期和蓝火。
- `DomainService`、`DomainInstance`、`ContactEnchantState`。
- 命中结果、父子结算、费用、法杖 32 槽。残骸生成方法要求调用方已经结算完，但本段没有那个调用方。
- 旧火六式没有加编号特判。

残骸下落用的是 0.04 格/tick 的惰性下落，只为了落到地上。这不是上面的有伤弹道。

## 资源与标签

镐标签和石质工具标签只追加 `xueximod:meteorite`，原来的 `shu_block` 和其他矿石还在。战利品表是自身 1 个。数据生成器同步追加了同样的两处标签和 `dropSelf`，避免下次生成把本块弄丢或因缺少战利品表失败。没有跑数据生成。

## 修改时间

| 文件 | mtime |
| --- | --- |
| `src/main/java/com/bslx/XueXiMod/block/ModBlocks.java` | 2026-10-31 04:07:25 |
| `src/main/java/com/bslx/XueXiMod/block/MeteoriteBlock.java` | 2026-10-31 04:08:44 |
| `src/main/java/com/bslx/XueXiMod/terrain/BatchTerrainBreakSwitch.java` | 2026-10-31 04:08:47 |
| `src/main/java/com/bslx/XueXiMod/terrain/MeteoriteWreckageTypes.java` | 2026-10-31 04:08:49 |
| `src/main/java/com/bslx/XueXiMod/client/MeteoriteWreckageRenderer.java` | 2026-10-31 04:08:53 |
| `src/main/java/com/bslx/XueXiMod/terrain/MeteoriteWreckageEntity.java` | 2026-10-31 04:09:30 |
| `src/main/java/com/bslx/XueXiMod/terrain/TerrainMutationService.java` | 2026-10-31 04:12:51 |
| `src/main/java/com/bslx/XueXiMod/datagen/ModBlockTagGenerator.java` | 2026-10-31 04:07:27 |
| `src/main/java/com/bslx/XueXiMod/datagen/loot/ModBlockLootTables.java` | 2026-10-31 04:07:27 |
| `textures/block/meteorite_v1.png` | 2026-10-31 03:17:23 |
| 方块状态与方块/物品模型 | 2026-10-31 04:09:31 |
| 陨石战利品表 | 2026-10-31 04:09:33 |
| 镐标签、石质工具标签 | 2026-10-31 04:07:28 |

贴图时间是归档文件的时间；复制后两边 SHA256 相同。

## 自查

`tools/check-name-gate.ps1` 退出码 1。本次新文件和改过的文件没有命名违规。剩下两处在未改动的 `OriginalHumanoid.java`（`i`、`to`），没有顺手改。

没有运行 `gradlew`、`javac`、`build`、`jar`、`reobf` 或客户端。编译是否通过还不知道。

## 所有权

B 的 `author-b-ownership.json` 已核对，那些文件没有改。`BreakGroundShape` 只读。C 的路径在 `author-c-ownership.json`，并写明后续还会补载体和领域文件。

## 停写

源码、资源和本目录这五份 C 第一段文档写完即停。没有等待或监控 A。
