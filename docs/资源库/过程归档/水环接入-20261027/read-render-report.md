# 水环 v8 素材接入现有水环渲染：只读事实摘要

## 结论（最小入口）

渲染入口只有一处：`GuardRingRenderer.onRender`（`src/main/java/com/bslx/XueXiMod/client/GuardRingRenderer.java:31-66`）。它在一张 32 段程序化环带（`ring()`，`:81-121`）上贴一张原版气泡粒子图（`:25`），U 绕周 0→1（`:112-113`），V 覆盖整个带宽 0→1（`:135-138`）。

接入 v8 不动事件、坐标、法线混合、近眼淡出，只动三处：贴图 ResourceLocation（`:25`）、顶点色与 alpha（`:101`、`:144`）、UV 的 V 从「整张贴图」改成「图集里当前帧的子区间」（`:112-113`、`:135-138`）。若内外缘分两张图集，再加一处：第二个 `getBuffer`/`endBatch` 配对（`:48`、`:65`）。

## 渲染事件与坐标系

| 事实 | 位置 |
|---|---|
| 客户端静态订阅，无实例状态 | `GuardRingRenderer.java:23` |
| 只在 `RenderLevelStageEvent.Stage.AFTER_ENTITIES` 执行 | `:31-35` |
| 前置判空：`level`/`player` 非空、`GuardSyncPacket.ClientRings.all()` 非空 | `:36-42` |
| `partialTick`、相机位置、第一人称标志、`PoseStack`、`BufferSource` 都取自事件 | `:43-47` |
| `pose.translate(-camera.x, -camera.y, -camera.z)` 后取 `pose.last().pose()` | `:49-51` |

坐标系的含义：顶点提交的是「以相机为原点的世界坐标」，单位是方块，没有旋转和缩放。整个渲染器只用一次 pose 平移，之后所有顶点直接写世界坐标（`:143`）。`DomainMistRingRenderer` 用 `relativeX/relativeZ` 做同样的减相机处理（`DomainMistRingRenderer.java:43-46`），两者约定一致。

## radius / normal 来源

- 环心：`wearer.xo→getX` 等三轴按 `partialTick` 插值，Y 再加 `wearer.getBbHeight() * view.bodyHeight`（`:57-59`）。
- 半径与厚度：`view.radius`、`view.band` 直接来自包，几何在 `:91-92` 算成 `outer = radius + band/2`、`inner = max(0.05, radius - band/2)`。
- 法线：包内 `from*` 与 `toward*` 三轴按进度插值再归一化（`:68-79`），退化时回落 `(0,1,0)`（`:75-77`）。进度用 `System.currentTimeMillis()` 与 `view.poseStartMillis`、`view.poseTicks * 50ms`（`:69-70`）。
- 环平面基：`helper = |normal.y|>0.9 ? +X : +Y`，`side = normalize(helper × normal)`，`other = normalize(normal × side)`（`:84-90`）。环带就展开在 `side`/`other` 张成的平面里（`place()`，`:123-130`）。
- 上述字段定义在 `GuardSyncPacket.ClientRings.View`，本次材料未提供 → 待查（单位、写入频率、默认值）。

## 现有贴图 / alpha / render state

- 贴图：`new ResourceLocation("minecraft", "textures/particle/bubble.png")`，一张静态图，无动画、无 UV 偏移（`:25`）。
- 分段：`SEGMENTS = 32`（`:26`），每段一个四边形，U 步进 1/32，绕周 0→1（`:112-113`）。
- 顶点 UV：V 固定 0（上两点）/1（下两点），即整张图纵向铺满带宽（`:135-138`）。
- 顶点色：硬编码 `(90, 180, 220)`（`:144`），会与贴图颜色相乘。
- alpha：硬编码 170（`:101`）；自己或第一人称时按到相机距离两段衰减，`<0.35` 格跳过该段，`<0.7` 格线性降 alpha（`:102-111`、`:61`）。
- 光照与 overlay：`LightTexture.FULL_BRIGHT`（`:93`、`:147`），`OverlayTexture.NO_OVERLAY`（`:146`）。
- 双面：每段画两片，反面沿法线偏移 0.002、顶点顺序反转、法线与 U 取反（`:116-119`）。
- 渲染类型：`RenderType.entityTranslucent(BUBBLE)`（`:48`），半透明混合 + 实体顶点格式（pos/color/uv/overlay/light/normal）。是否背面剔除、是否写深度、mipmap 配置由 RenderType 定义，本次材料未含 → 待查。
- 提交：一个 consumer 从 `:48` 用到 `:65`，整批一次 `endBatch(RenderType.entityTranslucent(BUBBLE))`，批次 key 是同一 RenderType 的两次构造。

## 客户端帧时钟现状

两条时钟并存，都不是游戏刻：

- 位置插值：`event.getPartialTick()`（`:43`、`:57-59`）。
- 法线混合：墙钟毫秒（`:70`）。
- 事件只提供 `partialTick` 和相机，不给 `gameTime`（`:43-46`）。要按 v8 的 100ms/帧取帧，可以继续用墙钟（与本文件 `:70` 同源），也可以取 `minecraft.level.getGameTime()`（`:37` 已保证 level 非空）并叠加 `partialTick`。两种做法都不需要改包。

## 可借鉴的既有写法（DomainMistRingRenderer）

- 资源引用写法：`new ResourceLocation(XueXiMod.MOD_ID, "textures/domain/mist_panel.png")`（`DomainMistRingRenderer.java:13`），自定义 RenderType 在静态字段里构造一次（`:14`）。
- 图集帧动画只改 V：`frame = floorMod(gameTime - visualOrigin + animSeed, FRAME_COUNT)`，`topV/bottomV = frameV(frame, FRAME_EDGE_PAD)`（`:32-35`）。这与 v8 的 256×640 竖排 16 帧同构，`FRAME_EDGE_PAD` 的作用是帧与帧之间留边。
- 单面提交，不画反向面（`:11`、`:47-53`）；四边形顺序为上起→上终→下终→下起，法线取外向（`:47-53`）。
- 通过 `DomainVolumeBatch.addQuad(RenderType, ...)` 批量收集，而非直接持有 VertexConsumer（`:47`）；该批处理类的释放时机与排序本次未提供 → 待查。

## v8 素材事实（仅取 mesh 与 UV，不涉及 base64）

- 状态：美术验证草稿，待用户确认，未接入游戏（`docs/资源库/过程归档/周身水环3D验证_v8_细碎水膜毛边-20261027/README.md:3`）；未改代码、未编译、无游戏内验证（`:37`）。
- 浪缘贴图：`ragged_splash_outer.png` / `ragged_splash_inner.png`，单帧 256×40，16 帧，每帧 100ms，竖排图集 256×640；内外缘独立错相（`:18-19`）。两个 Aseprite 源均四层（`:20`）。
- 环体 `broken_saw_flow` 沿用 v7 未改（`:21`）。
- 网格与 UV：与 v7 逐项一致，只替换两张浪缘贴图（`:29`）。README 未说明该网格与当前 32 段程序化环带是否同构 → 待查。
- 周期：根组 2.4s 逆时针一圈；纹理 1.6s 局部循环，不做整周 UV 滚动；0 与 4.8s 完全一致（`:31`）；预览 96 帧 / 20fps / 4.8s（`:14`）。4.8s 正好是 1.6s 与 2.4s 的最小公倍数。
- 贴图内嵌在 `.bbmodel`（`:22`），需从工程导出顶点/面/UV；这些数据本次材料未含 → 待查。

## 接入需动的最少文件

1. 必改：`src/main/java/com/bslx/XueXiMod/client/GuardRingRenderer.java`
   - `:25` 换成 v8 图集的 ResourceLocation（外/内两份）。
   - `:112-113`、`:135-138` 的 V 由 0/1 改成当前帧子区间，帧号用墙钟或 `level.getGameTime()`，留帧边距。
   - `:101`、`:144` 的 alpha 170 与 `(90,180,220)` 决定是否保留；v8 是像素画水膜，乘算会压暗压透。
   - `:48`/`:65` 若内外缘分两批，成对增补 `getBuffer`/`endBatch`。
2. 新增资源：`src/main/resources/assets/xueximod/textures/<子目录>/…png`，从归档目录复制两张 256×640 确认版。子目录命名待定，现有同类资源示例是 `textures/domain/mist_panel.png`。
3. 可选新增：一个只含网格数组与帧 V 计算的小类，用于方案 B（直接画 bbmodel 网格，替换 `ring()` 的 `:81-121`）。
4. 不改：事件订阅（`:31-35`）、坐标平移（`:49-51`）、近眼淡出（`:102-111`）、法线混合（`:68-79`）、`GuardSyncPacket`（若帧时钟走墙钟或 `getGameTime()`）。

## 风险点

- 顶点色相乘：`(90,180,220)` × alpha 0.67 会显著改变 v8 作者定的水膜色与透明层次，美术验收容易不通过；改动集中在 `:101`、`:144`。
- 图集串帧：单帧仅 40px 高，配 mipmap 或线性过滤时帧边界会取到邻帧像素。`DomainMistRingRenderer` 专门留 `FRAME_EDGE_PAD`（`:34-35`）处理这一点；v8 的 256×40 分帧是否自带边距，README 未写 → 待查。
- U 接缝：U 0→1 绕一周，第 32 段与第 1 段相接处必有一条纹理接缝。气泡图接缝不明显，v8 的毛边会更显眼；图集能否水平无缝平铺，材料未提 → 待查。
- 横向分辨率：256px 铺满整周、32 段，每段只覆盖 8px，水舌与细叉会被拉伸得比预览图粗。缓解手段是提高 `SEGMENTS`（`:26`）或按角度细分。
- 双面语义不同：现有代码手动补反面（`:116-119`），说明它不依赖背面剔除；`DomainMistRingRenderer` 明确只提交外向面（`:11`）。若照搬 `mistCurtain` 这类单面 RenderType，环会从一侧消失。
- 批次顺序：现在是一个 consumer 一批到底（`:48`、`:65`）。加到两张图集后，同一帧内多次 `getBuffer` 会触发隐式 flush，半透明排序随之改变，必须显式按 RenderType 配对 `endBatch`。
- 几何基准变化：现有厚度由服务端 `view.band` 驱动（`:91-92`）。若改用固定尺寸的 v8 网格，`band` 会退化成统一缩放，水环粗细不再随同步值变化，属于行为变化，需要拍板。
- 旋转实现方式不同：现有渲染器没有旋转。若图集水平可平铺，用 U 偏移最省，但只转贴图、不转几何轮廓；要让破碎环体整体转 2.4s 一圈，需在 `side`/`other` 基（`:84-90`）上加旋转角，或对网格顶点做旋转。
- 每帧分配：`place()` 每顶点 `new Vec3`（`:123-130`）。网格接入应静态缓存顶点/UV，避免每帧重建。

## 材料未含、需要补查

- `src/main/java/com/bslx/XueXiMod/client/WaterArcRenderer.java` 本次完全未提供，无法给出它的 RenderType、VertexConsumer、贴图动画与资源引用事实。
- `GuardSyncPacket` / `ClientRings.View` 的字段定义与同步频率。
- `DomainSheetMesh.mistCurtain`、`DomainVolumeBatch`、`DomainMistRingLayout.frameV`/`FRAME_EDGE_PAD`/`FRAME_COUNT` 的实现。
- v8 的网格顶点/UV/包围盒与原点、图集帧间边距、横向可平铺性。
- `RenderType.entityTranslucent` 的剔除、深度写入与 mipmap 具体配置。

没有改动任何文件，没有编译或启动进程。时间有限，以上只覆盖所给材料的可核对事实；标「待查」的项需要主代理补充对应源码或资源数据后再定。