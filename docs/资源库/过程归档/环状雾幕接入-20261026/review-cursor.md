**结论：通过。** 对照裁决、基线和当前任务文件，没有发现可证实缺陷。游戏内效果本次没有看。

## 几何与范围

`DomainVisual` 只有 `VOLUME` 和 `PRESSURE`。`VOLUME` 在 `DomainWorldRenderer.java:52-54` 离开六面水纹；顶面、底面在第 70、71 行，只有其余视觉才会画到。重压仍走原来的 `DomainPressureRenderer`。

雾环是盒子水平截面的内接椭圆：中心为半宽半深，上下贴 `minY`/`maxY`。`SIDE_COUNT` 为 16，相邻面共用同一组局部端点，最后一面回到第一点。`DomainMistRingRenderer.java:41-54` 每个侧面只 `addQuad` 一次，没有顶底，也没有反向面。`RenderType` 为 `NO_CULL`。顶点是 `(255,255,255,255)`。

世界坐标先在 double 里用中心减去相机，再加上局部偏移，最后才转 float（`DomainMistRingLayout.java:108-122`）。

## 粉色、生命周期、UV

雾环和细雨都经 `DomainVolumeBatch.java:86` 写 `OverlayTexture.NO_OVERLAY`。旧六面水纹仍是 `overlayCoords(0)`（`DomainWorldRenderer.java:113`），重压第 166 行也仍是 0，与“只改雾和雨”一致。

环和雨都要求 `visualOrigin <= gameTime < visualEnd`。帧用 `floorMod(gameTime - visualOrigin + 相位, 32)`。环的帧在循环外算一次，16 面共用。快照移除后不再进入绘制，没有另建客户端余留。细雨的格子、80° 方向、相位、密度和距离裁剪与基线一致，只是把局部坐标加上 `(盒子最小点 - 相机)` 后交给排序批次。

周向 U 按累计边长连续增加，不逐面归零，也不按面取模；终点是 `round(周长 / (2 * 高度))` 圈，至少 1。纵向 `localV` 只走当前帧，上下各收 `0.5/64`，`v = (frame + localV) / 32`，32 帧都落在本帧内。

## 排序与材质

同一帧的雾环面和雨格按相机空间中心距离由远到近排序（`DomainVolumeBatch.java:42`）。纹理变化时只 `endBatch(该 RenderType)`（第 49、57 行），没有无参数 `endBatch()`。雾环 `RenderType` 和专用 `ShaderStateShard` 都是静态的。深度是 `LEQUAL`，写掩码是 `COLOR_WRITE`，混合是 `TRANSLUCENT_TRANSPARENCY`，纹理 `blur=false`、`mipmap=false`，`NO_CULL` 仍开。没有全局 `depthMask`。

`DomainSheetMesh.of` 仍用原版 `RENDERTYPE_ENTITY_TRANSLUCENT_SHADER`。细雨走这条；雾环才走 `mistCurtain`。资源库总索引相对基线只改了雾幕这一条。

## 专用 shader 与资源

注册在客户端 mod 总线的 `RegisterShadersEvent`（`DomainMistShaders.java:15-29`），格式 `NEW_ENTITY`，位置 `xueximod:rendertype_entity_translucent_mist`，对应 `assets/xueximod/shaders/core/rendertype_entity_translucent_mist.json`。片元在乘顶点色之前判断，第 23 行是 `if (color.a <= 0.0)`。顶点 shader 与 1.20.1 原版 `rendertype_entity_translucent.vsh` 相同；json 只把第 7、8 行的顶点、片元名改到 `xueximod:`。光照、雾化、采样和 overlay 混合都还在。没有把 alpha 加倍。

游戏内 `mist_panel.png` 与归档 SHA-256 同为 `393488678ED4E66C4C31650B92282067C299C6C0A443B79D09CB5FB72E62F14E`，IHDR 为 128×2048、位深 8、颜色类型 6。mcmeta 只有 `blur: false`、`clamp: false`，没有动画段。细雨 PNG 与归档同为 `4D578C45469F3F9265AE254BD07A4BE8AA09B36B17D5C6512604DA2B544A66C4`。

## 限制

- 没有进游戏。粉色是否消失、环是否顺、低 alpha 两档是否可见、shader 是否真正加载，都还没看过画面。
- 没有重跑已记录通过的 `compileJava`。`compileJava` 本身也不能执行这段 GLSL。
- 这次没有重新解包本机 `client-extra.jar`；shader 结论来自与 1.20.1 原版实体透明 shader 文本逐项对照。
- 排序只覆盖本次雾环和细雨。大环面用中心距离，贴着环面边缘的雨格仍可能和该面局部交错；同一次纹理提交里，`sortOnUpload` 还可能按它自己的距离再排这一小段。这不能当成所有透明物体都已排序。
