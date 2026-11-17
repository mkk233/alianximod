我先读取任务说明和相关约束，确认这次授权范围后再动手。任务已授权由我直接实现。我先读本目录记录和输入文档，并核对可写源码的准确路径。设计边界已经明确。接下来读可写类和它们的直接依赖，先核对「没有动作记录才能绕过」以及蒙皮是否独立于动作。动作记录和蒙皮是否绑在一起，要看会话和当前渲染作用域怎么建。我接着读这些直接依赖，确认哪些入口能安全短路。蒙皮只挂在当前视角帧上，而且只有活动采样才会变成蒙皮。没有播放记录时可以绕过动作快照；护甲和箭矢仍按当前实体和作用域判断，避免外层蒙皮被内层继承。开始备份现有字节。备份已对齐。接下来只改授权类：没有播放记录就走原版一次，护甲和箭矢则看当前实体的当前作用域。护甲混合类用了 `EntityModel`，但还没导入。我补上这个导入。命名检查里，这次改过的文件没有新的短名问题。我把前后哈希和实现报告写入本目录。# 实现 B 最终报告：动作与蒙皮空闲路径

授权实现已落地。源码写入已经停止，没有留下占用。统一编译未跑。游戏未验证。不估计帧数。

`ActionRenderQueries.java` 没有改。查询放在 `ActionClientPlayback.hasPlaybackRecord`。`SkinSession`、`RenderPoseScope`、姿态体系、斩面、领域移动、火种和燎里都没有写。`记录.md` 没有写。

## 修改文件

增量基线是本目录 `before-b/`，按 `src/main/java/...` 保留包路径。SHA256 均为大写十六进制。最后写入时间是本地时区。

| 文件 | 改动前 SHA256 | 改动后 SHA256 | 最后写入 |
| --- | --- | --- | --- |
| `src/main/java/com/bslx/XueXiMod/client/action/ActionClientPlayback.java` | `D55E57C02EFFC0C96E282A123907D8FB0F8722627FFA1BE411F24B42AA1E9A5C` | `A21A6D89B85683979F60F871049A015B5C0AAA4BA134CD497E34A41CCFCFD5C3` | 2026-10-30 01:53:24.714 +08:00 |
| `src/main/java/com/bslx/XueXiMod/mixin/client/EntityRenderDispatcherActionMixin.java` | `4145487C3A5836BD9C73A400621211265855464AF381A23398C30C0031756560` | `8DD25C2389A109D191EAC8762F1965144619CF9D9E923F7FC085DCC5307920EB` | 2026-10-30 01:53:48.814 +08:00 |
| `src/main/java/com/bslx/XueXiMod/mixin/client/ItemInHandRendererActionMixin.java` | `F76C3541594F669C6BA65E6ED3F06D826C65B68C82734164CBAAF72D3B4EAE24` | `B4EDB90CE70F3ED5002AE7DC7025BF34E1B663F61FDA604475D9F529E5500E11` | 2026-10-30 01:53:52.489 +08:00 |
| `src/main/java/com/bslx/XueXiMod/mixin/client/HumanoidAttackTimeMixin.java` | `BFB7AB71D2C4373FE41C2C95A4CB0BB9EA453D4DCA096FD1A18110EA7314F2EE` | `68721C74962DEEDBD9A5958DD886658F50831A14529A62D7F5EC0A92FFE72EB6` | 2026-10-30 01:54:04.204 +08:00 |
| `src/main/java/com/bslx/XueXiMod/client/action/skinned/attachments/SkinAttachmentBridge.java` | `2F2D4E18FDE85F65A21DEF917131FACF70702AE3920D83BC34556198F7CC7B29` | `9742405AD9171DBBE5FF6EBEE14D8BC2A69BFCA08E66336C998B60991E16CE5C` | 2026-10-30 01:54:05.772 +08:00 |
| `src/main/java/com/bslx/XueXiMod/mixin/client/skin/StuckLayerScopeMixin.java` | `61CF646AB852FBC1C99FCCE776A1F4C3A3B46A11337D2D19904CB6A9CB92A05C` | `F741D20FC84E68FED21B56B0604CEF6762D09C77AD12B9DACC4D1507BEF7848B` | 2026-10-30 01:54:09.341 +08:00 |
| `src/main/java/com/bslx/XueXiMod/mixin/client/skin/HumanoidArmorSkinMixin.java` | `4C401E6548010D998E4E2E19DEC65FC2DDF0BDB1FBCBE69A0E49B8E4791F6C31` | `BBFBAFBC83D9E45BD2B5FF110AB6BB8AB2F29FB20AEEDD85098858C036E673CE` | 2026-10-30 01:54:25.147 +08:00 |

备份哈希与改动前哈希一致。

## 实现前提

1. 播放记录就是 `VIEWS` 里的键。`hasPlaybackRecord`（`ActionClientPlayback.java` 50–56 行）只做 `containsKey`，不调用 `sample` 或 `inspect`，也不删除条目。
2. 地图里的记录会一直留到登出（158 行）、同级世界卸载（174 行）、换世界绑定（239 行），或连接对象变了才 `remove`（196 行）。取消包走 `clear`，仍然 `put` 回地图（294–308 行）。淡出发生在记录还在、相位仍是播放或暂停时的 `sample` 里。所以“有记录”包含淡出、结束、取消，以及过期后还没清掉的条目。
3. `inspect` 在没有记录或没有播放器时返回 `ActionSampleContext.inactive`（66–69 行）。`inactive` 会 `new Pose()`（`ActionSampleContext.java` 94–96 行）。有记录时即使相位已经不是播放或暂停，仍会采样（72–73 行）。快路必须在这次调用之前返回。
4. 活动位只在 `entityApplies` 且相位为 `PLAYING` 或 `PAUSED` 时为真（72 行）。`SkinSession.chooseThird` 在采样不活动时把计划写成 `VANILLA` 并返回（`SkinSession.java` 213–221 行），此时不跑附件预检，也不建立蒙皮帧。第一人称绘制同样要求 `sample.active()`（`FirstPersonSkinDraw.java` 56 行）。
5. 第三人称会话只从 `EntityRenderDispatcherActionMixin` 打开，第一人称会话只从 `ItemInHandRendererActionMixin` 打开。全仓没有别的 `SkinSession.openThirdPerson` / `openFirstPerson`。因此没有一条独立于动作采样的蒙皮入口。
6. 蒙皮身体帧挂在当前 `RenderPoseScope` 栈顶，并且要求会话玩家、模型、`SKINNED` 计划和已安装帧同时匹配（`SkinRenderAccess.java` 17–22、41–49 行）。`frozenSample` 还要求采样主人 UUID 相同（`RenderPoseScope.java` 55–64 行）。这是当前实体的当前帧，不是“世界上任意会话存在”。
7. 披风、鞘翅、头颅和第三人称手持物用 `entityFor`（`SkinAttachmentBridge.java` 366–378 行）：栈顶会话的模型必须就是这一层的父模型。玩家渲染器按皮肤模型共用同一只 `PlayerModel`。内层玩家若不压入自己的视角帧，就会读到外层会话。所以视角深度不为 0 时，动作包装保持原路径。
8. 护甲替换发生在 `emitArmorOrPass`，并且先看 `ArmorDraw.current()` 和 `LayerHold.current()`。箭矢的选件和落点同样读这两套栈顶。外层令牌还在时，内层必须再压入自己的令牌，画完再弹出。

## 快路条件

### 实体渲染

`EntityRenderDispatcherActionMixin.java` 45–52 行、74–84 行。

- 非玩家、非玩家渲染器：原版 `renderer.render` 一次。这是原有分支。
- 玩家同时满足：视角深度为 0、该 UUID 的冻结采样为空、`hasPlaybackRecord` 为假。原版 `renderer.render` 一次，参数不变。不拍模型快照，不压渲染时钟和空间捕获，不打开视角帧，不打开 `SkinSession`。
- 有播放记录、深度不为 0，或栈顶已经握着这份采样：53–72 行的原路径整段保留，含 `finally` 里的时钟、空间、快照和矩阵深度恢复。

深度不为 0 仍走原路径，是为了外层动作还握着共享玩家模型时，内层渲染继续快照并压入自己的会话。这样披风和头颅不会读到外层蒙皮，内层 `setupAnim` 也不会把外层已经写好的部件留在模型上。

### 第一人称手

`ItemInHandRendererActionMixin.java` 76–89 行、134–156 行、177–184 行。

- 接收者不是当前混合对象时，仍抛原来的 `IllegalStateException`。
- 与实体渲染同一组条件成立时，`renderArmWithItem` 一次。挥砍进度用传入的 `swingProgress`，装备进度原样传入。左右手各一次，`require = 2` 未改。
- 有记录、深度不为 0，或已有冻结采样：90–120 行原路径保留。接管剑手时仍把这一次挥砍进度写成 0，其余手保持原进度。
- 物品瞄准注入在没有记录且没有这份冻结采样时直接返回（177–179 行），避免 `firstPersonQuaternion` 走到 `inspect`。栈上已经有这份采样时仍计算四元数。

### attackTime

`HumanoidAttackTimeMixin.java` 43–44 行、53–54 行、57–76 行。

- 两个重定向都先读 `receiver.attackTime`，不写字段。
- 非玩家模型或非玩家：仍调用 `suppressVanillaSwing`。该方法在类型不匹配时返回假，不创建姿态。
- 当前栈已经有该 UUID 的冻结采样，或地图里还有播放记录：仍调用 `suppressVanillaSwing`。冻结采样会从栈顶取回，语义与原来的 `thirdPersonPose` 相同。
- 玩家没有记录且栈上没有这份采样：返回读到的原值，不调用会创建 `Pose` 的查询。

## 护甲和箭矢包装

判断在 `SkinAttachmentBridge.java` 152–169 行。调用点在 `HumanoidArmorSkinMixin.java` 52–64 行、`StuckLayerScopeMixin.java` 40–54 行。

需要包装时：

- `SkinRenderAccess.currentThirdPerson(entity, model)` 非空：当前栈顶就是这个实体、这只模型，并且已经有第三人称蒙皮帧。
- 或者 `LayerHold` / `ArmorDraw` 深度大于 0（护甲），`LayerHold` / `StuckMarks` 深度大于 0（箭矢）。外层令牌还在，内层再包一层，画完 `close` 弹出。

不需要包装时：

- 护甲：`xuexiCallArmorPiece` 一次，参数不变，不创建 `ArmorToken`。
- 箭矢层：`RenderLayer.render` 一次，不创建 `StuckToken`。
- 其他层：原来的一次 `renderLayer`，40–53 行的分支不进入。

护甲染色、纹饰和附魔光的三个 `renderToBuffer` 重定向以及 `xuexiArmorLeaf` 没有改。没有令牌时 `emitArmorOrPass` 看到 `ArmorDraw.current()` 为空并返回假，随后仍调用原版 `model.renderToBuffer`。护甲模型钩子 `cachedArmorModel` 也不依赖令牌，刚性回退里已经预检过的钩子模型仍会返回。

箭矢层内部的选件重定向没有改。没有令牌且外层栈为空时，`noteStuckPart` 因缺少层上下文返回，旋转和落点走原版。

## 保持原路径的入口

- 披风 `CapeSkinMixin`、鞘翅 `ElytraSkinMixin`、头颅 `CustomHeadSkinMixin`、第三人称手持 `ItemInHandSkinMixin`：不在本次可写文件里。它们不创建护甲或箭矢令牌。没有匹配的蒙皮帧时，`prependChest`、`replaceHead`、`heldReplaces` 保持原矩阵和原调用。内层玩家在外层作用域里渲染时，由调度器原路径压入新会话，避免 `entityFor` 配到外层模型。
- 身体 `LivingEntityRendererBodyMixin` / `SkinnedBodyDraw`：没有会话或计划不是 `SKINNED` 时画原版身体。调度器快路不打开会话，因此这里自然走原版。
- `PlayerModelActionMixin`：没有匹配的第三人称帧时不写刚性姿态。
- 有播放记录的玩家，以及嵌在已有视角帧里的玩家渲染：动作快照、采样、会话、手部接管和 `attackTime` 语义都留在原路径，包括淡出和取消后仍留在地图里的记录。

## 命名门禁

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 1。日志：`name-gate-b.log`。

本次改过的 7 个源码文件没有出现在失败列表里。失败的是未改文件 `animation/runtime/OriginalHumanoid.java` 165 行 `i`、202 行 `to`。按授权保留，没有顺手改名。

## 未完成

授权范围内的快路和包装判断已经写完，没有半截分支。

- 统一编译未运行。
- 游戏内效果未验证。
- 帧数收益未估计。

报告全文在 `docs/开发记录/空闲与复活性能优化-20261030/implement-b-report.md`，对照数据在同目录 `implement-b-evidence.json`。
