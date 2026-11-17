我先读取这份审查任务说明，按其中授权范围执行，并给出完整终稿。任务范围已明确：只读对照 A/B/C 三份增量。我先读记录、作者报告和证据，再对照改前备份与当前源码。A 和 C 的改前对照已经对上。接下来核对 B 的快路条件，以及这三份增量的调用方有没有改到返回值。本轮范围内没有阻断项，也没有需要返修的可选项。这只说明相对 `before-a`、`before-b`、`before-c` 的源码对照没有发现功能破坏，不表示游戏内行为已经验证。本轮只读，未编译，未采样，不估计帧数，也不把这次改动说成已经解决复活卡顿。本轮是实现第 1/3 轮，不再另开复审。

对照的是冻结后的 11 个源文件与各自改前备份。当前文件 SHA256 与作者报告、`frozen-source-metadata.json` 一致，备份 SHA256 与报告中的改前哈希一致。`DomainClientBridge.java` 仍是 `CACABD091FC44A61A729E3DB3B2D887FECD6007B854E7186022F47DDCA376476`，未改。没有用 git HEAD 的历史差异当作本批变更。

## A：静止斩面

`entitiesForRendering()` 现在只出现在 `StillSheetPlayback.java` 第 195 行，由 `alignWorld` 在 `coveredLevel` 还不是当前 `ClientLevel` 时调用一次。tick 末和 `AFTER_ENTITIES` 只走 `captureCandidates`，不再逐帧扫全表。

加入时不看斩面是否启用。`onHitEntityJoin`（第 107–114 行）只把客户端世界里的 `TechniqueHitEntity` 放进该世界实例的候选集。`offerEntity`（第 224–235 行）在未启用或出生时间仍为负时直接返回，不把候选删掉，后面的 tick 和帧会再试。取消的加入不入集。

离开、移除和世界清理是分开的。离开只从该世界的候选集移除（第 117–132 行），不调用 `cancel`。`isRemoved()` 只退出候选（第 210–212 行）。已经 `offer` 进去的快照留在 `StillSheetCache`，直到原来的 `dropExpired`、显式取消，或世界切换、卸载、登出。UUID 去重、tick 的 `partialTick` `0`、渲染用事件 `partialTick`、96 格视距和绘制顺序都与备份一致。空缓存的 `copyRetained()` 返回 `List.of()`，唯一调用方 `drawSheets` 只读，空表在取缓冲前返回。

世界身份按 `ClientLevel` 实例，不按维度名。`minecraft.level == null` 时直接返回，不把已经到达的候选清掉。登出走 `forgetWorld`。卸载只删被卸载的那个实例；它若是当前绑定世界，才清快照。切到另一个实例时只删旧实例的候选，然后清快照并绑定新实例；新实例上已经记下的候选会与一次覆盖扫描合并，不会先清空再扫。同维复活没有 `Clone` 监听，`ClientLevel` 实例不变时不会清快照。没有新增监听类。

## B：动作与蒙皮

`hasPlaybackRecord`（`ActionClientPlayback.java` 第 54–56 行）只做 `VIEWS.containsKey`。取消包仍 `put` 回地图（第 294–308 行）。登出、同级卸载和换世界绑定仍会清地图。有记录时，调度器、第一人称手和 `attackTime` 都留在原来的采样路径上，淡出和过期仍由 `inspect` / `sample` 处理。

没有记录、视角深度为 0、并且栈顶没有这个 UUID 的冻结采样时，玩家渲染和左右手各调用一次原版，参数不变。深度不为 0 时整段旧路径还在，含 `finally` 里的时钟、空间、模型快照和矩阵深度恢复。快路本身不压这些状态，所以原版调用中途抛错时没有新的作用域要收。有记录时的 `finally` 没有删。

蒙皮会话仍然只从这两个混合类打开。`SkinSession.chooseThird` 在采样不活动时写成原版计划（`SkinSession.java` 第 219–221 行）。第一人称绘制在 `sample.active()` 为假时返回（`FirstPersonSkinDraw.java` 第 56 行）。没有记录就不会出现本应保留的蒙皮帧。披风、鞘翅、头颅和第三人称手持不在本次可写文件里，无匹配帧时保持原调用，这是设计允许的保留。

`attackTime` 先读字段，不写字段。当前栈已经有该 UUID 的冻结采样，或地图里还有记录时，仍走 `suppressVanillaSwing`。两者都没有时返回读到的原值。非玩家模型仍进原查询，类型不符时该查询返回假，不创建姿态。

护甲和箭矢在 `currentThirdPerson(entity, model)` 非空，或外层 `LayerHold` / `ArmorDraw` / `StuckMarks` 深度大于 0 时才包令牌，画完 `close`。否则护甲 `xuexiCallArmorPiece` 一次，箭矢 `RenderLayer.render` 一次。染色、纹饰、附魔光三个 `renderToBuffer` 重定向没有改；没有令牌时 `emitArmorOrPass` 看到空的 `ArmorDraw` 并回落到原版 `renderToBuffer`。箭矢选件重定向没有改；没有层上下文时 `noteStuckPart` 返回，旋转和落点走原版。

## C：领域移动

本地玩家缺失、移动者为空、移动者不是本地玩家且根载具不同，或 `DomainSightStore.all()` 为空时，`DomainClientState.targets` 返回共享的 `List.of()`。根载具条件与备份相同。集合非空时仍按这一次的 `all()` 新建列表，不放进静态字段。

`DomainMotion.relevant` 的客户端分支只在空结果时换成自己的共享空列表，非空时返回桥接列表本身。全 `src` 里 `DomainClientBridge.targets` 只有这一处调用。`clip` 和 `finish` 只做空判断和只读迭代；`finish` 在 `ALLOWED` 没有记录时先返回，不会在空预测路径上再取列表。服务器分支仍每次新建 `ArrayList`，只收活动且与移动者同一世界的领域。`clip` / `finish` 的裁切、`ALLOWED` 配对和 `forget` 语义未改。领域进入或离开会在下一次 `relevant` 读到。同维复活相关的清理路径没有改。

## 命名与范围

三份命名日志的退出码都是 1，失败都只有未改的 `OriginalHumanoid.java` 第 165 行 `i` 和第 202 行 `to`。本批 11 个文件没有新的短名违规，也没有顺手改历史问题。没有扩到火种、燎里或其他授权外源码。
