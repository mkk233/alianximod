## 定位报告（只读，非实现复审）

已读根目录 `AGENTS.md`。`docs/开发记录/原胚属性获取-20261109/` 没有自己的 `AGENTS.md`。按该目录 `记录.md` 的 v1 及已写入的设计裁决交接定位。未改源码、未编译、未启动游戏。

### 原胚与五属性

五属性不是五个注册物品。唯一物品是 `xueximod:shu_embryo`，类 `ShuEmbryoItem`。

- 注册：`src/main/java/com/bslx/XueXiMod/item/ModItems.java` 16–17 行。`XueXiMod.MOD_ID` 为 `xueximod`（`XueXiMod.java` 69 行）。`DeferredRegister` 已由 `XueXiMod.registerContent` 91–93 行挂上。
- 属性枚举：`src/main/java/com/bslx/XueXiMod/shu/ShuAttribute.java` 6–12 行。id 为 `none`、`wood`、`fire`、`water`、`earth`、`metal`。
- NBT：根标签字符串键 `Attribute`（`ShuEmbryoItem.java` 18、24–39 行）。无标签、无键、或 `fromId` 认不出的字符串都是 `NONE`（`ShuAttribute.java` 36–44 行）。`setAttribute(NONE)` 删键；空标签则 `setTag(null)`。`verifyTagAfterLoad` 42–46 行会删掉无法识别的键。
- 名称按 `item.xueximod.shu_embryo.` + id。客户端谓词 `xueximod:shu_attribute` 已在 `XueXiMod.java` 174–177 行注册，取值 `ordinal * 0.1`（`ShuAttribute.java` 28–31 行）：无 0、木 0.1、火 0.2、水 0.3、土 0.4、金 0.5。
- 模型 `assets/xueximod/models/item/shu_embryo.json` 默认 `embryo_none`，0.1–0.5 分别指向五属性模型。贴图 `embryo_none/wood/fire/water/earth/metal.png` 与五属性模型都在。中文名和 `shu.attribute.*` 在 `lang/zh_cn.json` 2–14 行。`en_us.json` 没有这些键。
- 创造栏已用 `ShuEmbryoItem.create` 放出六种栈：`ModCreativeModTabs.java` 36–41 行。`/shu give` 只发无属性与木属性各 8，且不看 `Inventory.add` 返回值：`ShuCommands.java` 57–64 行。
- `ShuEmbryoItem` 没有 `use` / `useOn`，默认堆叠 64。类注释写明属性决定堆叠分组；不同 `Attribute` 不会和空白原胚合成一叠。空白原胚没有该键。

公开入口已够外部改属性：`getAttribute`、`setAttribute`、`create`。不必新物品 ID，也不必改 `ModItems`。

### 原矿、粗胚、煅烧

保留链已经存在，产物是无 NBT 的 `xueximod:shu_embryo`，读出来就是无属性。

- 粗胚：`xueximod:shu_raw`，普通 `Item`，`ModItems.java` 22 行。贴图 `textures/item/shu_raw.png`，模型在 `src/generated/resources/assets/xueximod/models/item/shu_raw.json`。
- 四种矿：`ModBlocks.java` 28–37 行，`xueximod:ars_ore`、`deeplate_ars_ore`、`nether_ars_ore`、`end_ars_ore`。
- 掉落：`ModBlockLootTables.java` 39–49 行，四矿掉 `shu_raw`（2–5，时运），精准采集走原版丝绸分派。
- 熔炼与高炉：`ModRecipeProvider.java` 30–31、39–40、72–78 行。原料是粗胚加四种矿。熔炼 200 tick、高炉 100 tick、经验 1.0、组 `shu`。生成物在 `src/generated/resources/data/xueximod/recipes/`，例如 `shu_embryo_from_blasting_shu_raw.json`、`shu_embryo_from_smelting_ars_ore.json`。`result` 只有物品 id，不写 NBT。这里的 1.0 是熔炼经验，不是铁砧等级。
- 注释写“术矿石 → 术原胚”，代码原料同时包含粗胚。9 个原胚合成 `shu_block`、方块拆回 9 个原胚也在同一 provider 42–52 行，匹配的是物品 id，没有按 `Attribute` 过滤，产物也不带 NBT。v1 只要求保留这条加工链，定位不把它改成需求。

### 铁砧零经验

`src/main` 的 Java 与 JSON 没有 `Anvil`、`anvil`、`Smithing`、`smithing`。没有自定义铁砧菜单，也没有铁砧 mixin。`xueximod.mixins.json` 的 `mixins` / `client` 列表里都没有。`ModMenuTypes` 只有刻印台、术管理、装配和收纳。因此不存在已接好的零级取出、左右槽各减一、或零经验产物路径。1.20.1 也没有可只写数据包的铁砧配方。

最小接点是新增 `com.bslx.XueXiMod.mixin` 下的铁砧 mixin，并把它写入 `src/main/resources/xueximod.mixins.json` 的 `mixins`。`build.gradle` 233 行已把 `MixinConfigs` 指到这个文件。Mixin 不会像事件类那样自动登记。另开 `MenuType` 会再碰到 `ModMenuTypes` 和 `XueXiMod.ClientModEvents` 的屏幕注册，面更大。这里只说明没有现成入口，不判断哪种 mixin 注入能满足零级可取。

### 右键、流体、作物、权限

全仓物品/方块右键只有两处：

- `StillDomainFluids.java` 31 行 `@Mod.EventBusSubscriber`。`onBlockUse` 94–98 行、`onItemUse` 100–116 行。只在手持桶或玻璃瓶、且命中静水领域流体时取消。不区分服务端。`sourceTrace` 114–126 行是私有方法：眼睛到 `getBlockReach()`，`ClipContext.Block.OUTLINE` + `Fluid.SOURCE_ONLY`。它不处理岩浆、炼药锅，也不能被外部调用。原胚不是桶或瓶，这段取消条件不会因原胚触发。
- `TeShuItem.useOn` 36 行起是矿脉探测，与原胚无关。

源码里没有树苗、`CropBlock`、`isMaxAge`、炼药锅、普通泥土或岩浆源的交互约定。水/火/土/木都没有现成入口。

权限只在地形私有方法里：`TerrainMutationService.java` 268–275 行，`ownerCanMutate` 用 `mayBuild`、非旁观、非放置受限；`ownerMayAffect` 用 `blockActionRestricted` 和 `level.mayInteract`。不是通用物品交互工具。

### 背包容量

没有“用真实产物 NBT 预演、并把主手最后一个原胚腾出的格子算进去”的工具。

- `Inventory.add` 出现在命令和 `SkillStorageItem.getOrCreate` 35–37 行：失败就返回空，不丢地，但会直接写入，不是先模拟。
- `SpellAssemblyMenu.java` 517–518、766–767 行是另一条路径：`add` 失败就 `drop`。这与本次“失败保持原状、不以掉落交付”不是同一个封装。
- 术式匣满包只发消息：`SkillAssemblyPacket.java` 229、255 行。收纳箱 `addItem` 的 leftover 是匣内容量，不是玩家背包。

有属性原胚与空白原胚 NBT 不同，不能堆进已满的空白叠。

### 注册方式

- 带 `@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)` 的静态 `@SubscribeEvent` 会自动进 Forge 总线。`XueXiMod.java` 157–158 行和 `ModEvents.ServerEvents` 28 行、`StillDomainFluids` 31 行都是这样。新事件类不必改 `registerContent`，也不必新物品注册。
- 新 Mixin 必须改 `xueximod.mixins.json`。
- 不新物品、不新谓词时，不必改 `XueXiMod.java` 的客户端注册。

### 资源

现有五属性模型、贴图和中文名够用。本次不需要新美术。`textures/item/shu_embryo.png` 还在，但当前原胚模型用的是 `embryo_none`。

### 独占写入（按最小接点，不是实现方案）

建议新文件，避免锁住无关系统：

- 新建 Forge 事件类，处理木、水、火、土。
- 若走铁砧 mixin：新建 mixin 类。
- 必改共享文件：`src/main/resources/xueximod.mixins.json`。

不必为登记而改：`ModItems.java`、`ModRecipeProvider.java`、`src/generated` 配方与战利品、模型、贴图、语言、创造栏、制作表。`ShuEmbryoItem` 的公开读写已够用，除非另加辅助方法。

不建议改：`StillDomainFluids.java`、`ModEvents.java`、`TerrainMutationService.java`、`SpellAssemblyMenu.java`。`记录.md` 的记录作者是主会话，本次定位不写它。

### 与现机制并存的事实

- 属性转换应写在现有 `shu_embryo` 的 `Attribute` 上，不是新注册五个物品。
- 熔炼、高炉、原矿掉落和方块合成已经产出或消耗无 NBT 原胚；合成不看属性。
- 铁砧零经验取出目前不存在。
- 流体右键只拒绝领域静水上的桶和玻璃瓶；没有岩浆射线工具可直接调用。
- 没有成熟作物、炼药锅水位、源岩浆或普通泥土的既有判断。
- 未知 `Attribute` 会被当成无属性并在读档校验时删除。
- 装配菜单的满包处理会掉落物品；本次容量失败不能沿用那条路径。