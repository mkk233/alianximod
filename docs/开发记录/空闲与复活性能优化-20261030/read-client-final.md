我先读这份任务说明，确认授权范围后再按它执行。任务是只读排查空闲客户端性能，范围限定在客户端入口。我先按技能约束定位目录，再核对渲染、tick 和复活路径。空闲帧最可疑的是每帧都进的渲染钩子和客户端 tick。我接着核对它们有没有在没施法时提前返回。空闲客户端里，能对上「24 区块、未施法也低帧」的是按已加载实体和可见生物放大的额外工作。没有采样，不能把其中任何一项说成 40–50 FPS 的原因。本报告不是正式正确性结论。

未改文件，未编译，未进游戏，未读日志或 `options.txt`。客户端包内没有每帧扫区块或方块的循环，蒙皮读盘只在资源重载。事件监听都是类加载时的静态注册，复活路径上没有第二次注册。

## 1. 静止斩面每拍、每帧扫全部客户端实体

`StillSheetPlayback` 在客户端 tick 末和每一帧 `AFTER_ENTITIES` 都调用 `captureLoaded`。注释写明不看视锥，循环的是 `entitiesForRendering()`，空缓存也扫。

```50:82:src/main/java/com/bslx/XueXiMod/client/StillSheetPlayback.java
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // ...
        captureLoaded(clientLevel);
        RETAINED.dropExpired(clientLevel.getGameTime(), 0.0F);
    }
    public static void onRender(RenderLevelStageEvent event) {
        // ...
        captureLoaded(clientLevel);
        drawSheets(event, clientLevel, gameTime, partialTick);
        RETAINED.dropExpired(gameTime, partialTick);
    }
```

```110:115:src/main/java/com/bslx/XueXiMod/client/StillSheetPlayback.java
    private static void captureLoaded(ClientLevel clientLevel) {
        for (Entity entity : clientLevel.entitiesForRendering()) {
            if (entity instanceof TechniqueHitEntity hitEntity) {
                offerEntity(clientLevel, hitEntity);
            }
        }
    }
```

`drawSheets` 每帧还执行 `StillSheetCache.copyRetained()`（`StillSheetCache.java` 53–54 行），空表也会 `new ArrayList`。没有「当前没有斩击实体就跳过」的短路。缓存只在登出和客户端世界卸载时清空（85–94 行），同维死亡复活不会停掉扫描。

频率：约 20 次/秒，再加当前帧率一次。范围随 24 区块内客户端已加载实体增长，与是否施法无关。已有缓存是按 UUID 收一份快照，避免同一实体重播；它不阻止空闲扫描。

确定的是多余遍历和空列表分配。每次扫描多少毫秒没有测过。

最小边界：斩击实体加入客户端时自己 `offer`，播放中的快照仍由现有缓存画完。没有这类实体且缓存为空时不要扫全表。不要改「实体先消失、斩面继续播完」的语义。

## 2. 每次客户端移动都分配领域列表

`EntityMoveMixin` 包住全部 `Entity.move`，客户端也进。位移不是近似零时，`DomainMotion.relevant` 必定 `new ArrayList`，再调用 `DomainClientState.targets`，那里再分配一张表。

```105:109:src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainMotion.java
    private static List<DomainBound> relevant(Entity mover) {
        List<DomainBound> found = new ArrayList<>();
        if (mover.level().isClientSide()) {
            found.addAll(DomainClientBridge.targets(mover));
            return found;
        }
```

```22:30:src/main/java/com/bslx/XueXiMod/client/DomainClientState.java
    private static List<DomainBound> targets(Entity mover) {
        ArrayList<DomainBound> found = new ArrayList<>();
        // ...
        if (mover != viewer && mover.getRootVehicle() != viewer.getRootVehicle()) {
            return found;
        }
```

没有领域时，非本地玩家的移动实体仍各分配两张空表。`DomainSightStore.all()` 只是地图视图（32–34 行），本身不复制，但调用发生在分配之后，而且只对本地玩家或其所乘载具执行。`finish` 在没有记录时直接返回（50–56 行）。

频率：每只发生有效移动的已加载实体，每次 `move`。这跟视锥内可见数量不是一回事，24 区块会加大客户端跟踪到的移动实体数。空闲、未施法也会走。

确定的是分配。这些分配是否占到帧时间没有测过。

同维复活不会清空 `DomainSightStore`（见第 7 项）。没有领域时本来就是空转。

最小边界：客户端且视线领域为空时，`clip` 原样返回位移，不要分配。非观察者继续不参与预测。已有领域时的按轴裁切保持不动。

## 3. 每只可见人形、每帧四次护甲包装

`HumanoidArmorSkinMixin` 拦截 `renderArmorPiece` 的四次调用。包装在原版「这格没有护甲就返回」之前，所以空甲也进。

```43:57:src/main/java/com/bslx/XueXiMod/mixin/client/skin/HumanoidArmorSkinMixin.java
    private void xuexiArmorPiece(...) {
        SkinAttachmentBridge.ArmorToken token = SkinAttachmentBridge.openArmorPiece(entity, layer.getParentModel());
        try {
            ((HumanoidArmorSkinMixin) (Object) layer).xuexiCallArmorPiece(...);
        } finally {
            token.close();
        }
    }
```

`openArmorPiece`（`SkinAttachmentBridge.java` 152–161 行）每次 `new` 层主体和护甲件（`LayerHold.java` 17–19 行，`ArmorDraw.java` 35–38 行）。没有「当前没有蒙皮会话」的短路。随后的模型替换只在已有蒙皮帧时发生（164–176 行）。

频率：每只被画出的人形（玩家、僵尸、骷髅等）每帧 4 次。随 24 区块里实际画出来的人形数量增长。未施法、皮肤资源未就绪也走。

确定的是对象分配和多余调用。原版护甲绘制本身仍在。耗时未测。

最小边界：该实体这次不是蒙皮绘制时，直接调用原来的 `renderArmorPiece`。四件护甲、染色、纹饰和附魔光仍走原版。

## 4. 每只可见生物的每一层都被 Redirect

`StuckLayerScopeMixin` 把 `LivingEntityRenderer` 里每一次 `RenderLayer.render` 都改道。不是箭矢层时只做类型判断再调原方法（39–50 行）。箭矢层则无条件 `openStuck`（`SkinAttachmentBridge.java` 265–274 行），身上没有箭也分配主体和标记。

`LivingEntityRendererBodyMixin`（18–37 行）对每次身体 `renderToBuffer` 先问 `SkinnedBodyDraw.replace`；没有蒙皮会话时立刻返回并画原版（`SkinnedBodyDraw.java` 32–36 行）。

`EntityRenderDispatcherActionMixin` 还包住每一次实体 `render`。非玩家只多一次调用（42–44 行）。玩家则无论有没有动作都快照约 14 个模型部件、压入渲染时钟，并 `SkinSession.openThirdPerson`（46–64 行）。`chooseThird` 在没有活动动作时仍 `inspectLive`，再做一个 `VANILLA` 计划（`SkinSession.java` 213–221、339–351 行）。

频率：dispatcher 按被提交渲染的实体；层包装按可见生物乘层数。第一人称下本地玩家身体通常不走 dispatcher，其他玩家和生物会走。空闲也走。

确定的是调用和玩家路径上的分配。这几层 Redirect 加起来占多少帧时间没有测过。没有蒙皮时几何仍是原版。

最小边界：没有该玩家的活动动作时，dispatcher 直接调用原来的 `renderer.render`，不建会话、不快照。层包装只在这次会话已经是蒙皮时打开箭矢令牌。名字、阴影和其他层保持原调用一次。

## 5. 第一人称每只手都采样「没有动作」

本地玩家第一人称每帧都会进 `ItemInHandRendererActionMixin`，左右手各一次（`require = 2`）。每次都 `ModelPartSnapshot.capturePlayer`（约 14 个部件，`ModelPartSnapshot.java` 33–52 行），并 `SkinSession.openFirstPerson`。

```73:86:src/main/java/com/bslx/XueXiMod/client/action/skinned/SkinSession.java
    public static void openFirstPerson(Object model, Player player, float partialTick) {
        SkinSession session = new SkinSession(...);
        try {
            SkinAssets.recheckRegistry();
            session.sample = ActionRenderQueries.inspectLive(player, partialTick);
            // ...
        }
        RenderPoseScope.attachSkin(session);
    }
```

没有播放记录时，`ActionClientPlayback.inspect` 走到 `ActionSampleContext.inactive`（94–96 行），每次 `new Pose()`，而 `Pose` 构造会 `new HashMap`（`Pose.java` 41–42 行）。`recheckRegistry` 在状态不是 `READY` 时拿锁后立即返回（`SkinAssets.java` 97–102 行），不读盘。真正的网格文件只在资源重载时读（`SkinAssetReload.java` 36–43 行，`MeshRepository.java` 46–49 行）。

另外，玩家模型 `setupAnim` 里每次读 `attackTime` 都会进 `HumanoidAttackTimeMixin`（29–41 行）。玩家会再进 `thirdPersonPose` → `inspect`，没有动作时又分配一份无效采样（`ActionRenderQueries.java` 96–101、53–56 行）。非玩家在 `instanceof PlayerModel` 处返回，不建姿态。这是全部可见人形 `setupAnim` 上的 Redirect，空闲也在。

这项不随区块半径变大，只跟本地玩家是否在画手、以及附近有多少玩家/人形有关。它解释不了「只有 24 区块才严重」，除非采样证明手部路径本身极慢。确定的是每帧分配。耗时未测。

最小边界：该 UUID 没有活动动作时，手部和 `attackTime` 走原版，不建 `Pose`。有动作时仍只清掉被接管那只手的原版挥砍进度。非玩家的攻击动画不要改。

## 6. 没有视效时，世界阶段和 HUD 仍做空工作

这些不扫世界，也不随 24 区块变大。空闲时成本应远小于第 1–4 项，但是每帧都发生，且没有内容短路。

- `FireVisualClient.onRender`（123–149 行）：四张表都空时仍取缓冲、`new ArrayList`、推矩阵并遍历空表。表只在登出和客户端卸载时 `forget`（153–160 行）。
- `DomainWorldRenderer.onRender`（30–47 行）：没有领域也取相机、姿态和缓冲，再遍历空的 `DomainSightStore`。
- `PrisonShellRenderer` 先调用 `PrisonClientState.visible()`（60–62、87–89 行）。快照为空仍 `purge` 并 `new ArrayList`。真正的全实体 UUID 扫描在 129–135 行，只有已有水牢时才发生，不是空闲主路径。
- `WaterMomentumOverlay` 每帧调用 `freshChanges`（`WaterMomentumNotice.java` 77–84 行）。没有水势变化也 `removeIf` 并 `new ArrayList`。魔力 HUD（`MagicHudOverlay.java` 11–23 行）每帧拼字符串并画字；不是法杖时 `storedMana` 直接返回 0，不读 NBT。

复活不清除火视效和水牢快照。空表继续每帧进入。

最小边界：四张火表、领域集合、水牢快照、水势行都为空时，在取缓冲和分配之前返回。有内容时的绘制公式不动。

## 7. 复活没有重复注册，同维克隆也几乎不清客户端画面

监听都是 `@EventBusSubscriber` 静态方法。`SkinAssetReload` 还有 `registered` 防第二次登记（20–31 行）。源码里看不到复活时再 `register` 一次。

同维死亡走 `ClientPlayerNetworkEvent.Clone` 时，只有旧玩家或新玩家缺失，或维度不同，才清 `DomainSightStore`（`ModEvents.java` 125–129 行）。同一维度复活不会清：

- 静止斩面缓存（只在登出、卸载清）
- 火视效
- 动作 `VIEWS`（登出和换世界才清，`ActionClientPlayback.java` 145–167 行）
- 水牢快照（登出才 `clear`，`PrisonClientEntry.java` 29–31 行）
- `EntityRendererFix.settled`（只有 `level == null` 才复位，34–44 行）

`settled` 之后每 20 个客户端 tick 仍做三次注册表查找（43–55 行）。渲染器都在就返回，不复制渲染表。死亡复活通常不把 `level` 置空，所以不会因为复活重新绑一次渲染器，除非类型键真的丢了。

因此「复活后更卡」不能从重复注册推出来。同维复活后，第 1–6 项的空闲工作照旧。若当时已有斩面、火或领域，它们会留下，这是现有生命周期，不是新注册。复活当帧是否多画一个旧玩家、实体列表是否暴涨，源码看不出来。

最小边界：不要为了帧数在同维复活时清掉仍属于世界的斩面、火和领域。动作视图继续按 UUID 对齐，避免旧玩家对象上的蒙皮帧残留即可，登出路径已经会 `discardOpenFrames`。不要新增一套事件注册。

## 8. 空手操纵剑的全实体扫描不是站桩空闲路径

`SkillHotkeyHandler.hasOwnedSword`（137–148 行）会扫 `entitiesForRendering()`。调用条件是：没有打开界面、该键正按着、主手是空的、并且该槽链上有 `manipulatesSwordEntities()`。普通手持武器或没有按键时，`castAllowed` 在扫描前返回（108–134 行）。未施法、未按住技能键时这条不跑。

它可能在「空手按住右键、槽里有剑来」时每 tick 扫全表，那不是本次「刚进世界没施法」的主因。

最小边界：若以后要减，只在按键状态变化时查一次剑实体，不要在按住期间每 tick 重扫。查找结果仍须是「这名玩家自己的存活剑」。

## 对不上空闲主因的路径

- 湿度：`WetnessLivingMixin` 每只生物的 `travel` / `aiStep` 都会进，但 `clampDescent` 和 `afterAiStep` 在客户端立刻返回（`WetnessFlight.java` 175–176 行，`WetnessSensitivity.java` 9–11 行）。
- `PrisonLivingMixin` 的 `tickEffects` 在客户端每只生物每拍加减线程局部计数；`flushDeferred` 在队列空时返回（`AttributeStateService.java` 128–139、166–168 行）。实现细节与复活公共路径留给另一路排查，这里不展开。
- 水牢本地预测每拍最多三次 `holdLocal`，快照为空时只扫空表。
- 粒子、着色器、区块网格：雾着色器只在注册事件建一次（`DomainMistShaders.java` 24–28 行）。客户端目录没有空闲粒子循环，也没有区块或方块扫描。
- `DomainMistRingLayout` 的 `System.out` 在 `main` 自检里（142–179 行），不在渲染帧里。
- 装配界面、提示框只在对应界面打开时工作。

## 还没做的对照

下面任何一项都还没跑，所以不能把某一热点写成根因，也不能估计能回多少帧。

1. 同一世界、不施法、第一人称：调试饼图或采样里，`StillSheetPlayback.captureLoaded`、`DomainMotion.clip`、`HumanoidArmorLayer.render`、`ItemInHandRenderer.renderHandsWithItems`、`EntityRenderDispatcher.render` 各占多少。
2. 模组仍在、不施法：渲染距离 8 与 24 对照，并记下客户端实体数和可见人形数。用来区分第 1–4 项与第 5–6 项。
3. 超平坦空世界与当前世界对照。若空世界也只有 40–50 FPS，第 1–4 项就不是主因。
4. 死亡前、死亡当帧、同点复活后各采一次：实体数、斩面/火/领域表大小、事件监听是否变多。源码预期监听数不变。
5. 分清主线程逻辑时间和区块重建、地形绘制。24 区块的原版地形成本不在这次读到的循环里，需要采样把它和模组时间分开。

共享文件若要改，会碰到 `StillSheetPlayback`、`DomainMotion`、`DomainClientState`、`EntityMoveMixin`、护甲与层 mixin、`ItemInHandRendererActionMixin`、`EntityRenderDispatcherActionMixin`、`SkinSession`、`ActionSampleContext`、`FireVisualClient`、`DomainWorldRenderer`、`PrisonClientState`、`ModEvents` 的客户端克隆。所有权留给主会话分。
