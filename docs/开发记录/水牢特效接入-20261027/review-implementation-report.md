我先通读这份任务说明，再按里面写明的范围和约束执行。任务是只读集中复核，不改文件、不编译、不启动游戏。我先读本目录的设计与实现记录，再对照渲染器和贴图。设计与实现记录已对齐。接下来核对渲染器、材质和客户端状态，并校验贴图哈希与尺寸。批准设计里点名了相机向量、投影和法线。我去对一下 1.20.1 的真实 API，并核对贴图哈希。发现两处会让水牢公告板偏离批准设计的问题。

**1. 覆层坐标把蓝色源像素往红色里混**

`PrisonShellRenderer.java` 第 303 行是 `.overlayCoords(0)`。材质来自 `StillSheetMesh.of`：`RENDERTYPE_ENTITY_TRANSLUCENT_SHADER` 且打开了 `OVERLAY`。1.20.1 的覆层贴图里，V 小于 8 的半区是红色，`(0,0)` 落在这里；不染色的白行是 `OverlayTexture.NO_OVERLAY`（U=0，V=10）。同一材质的 `StillSheetPlayback` 第 218 行用的就是这个常量。

触发：只要这张公告板被画出来，每一帧都会乘上红覆层，白顶点和贴图里的蓝被拉开。

最小修复：改成 `overlayCoords(OverlayTexture.NO_OVERLAY)`。

**2. 顶点提交把绝对世界坐标压进 float**

八个角在第 206 行先减去相机，再 `transformPosition`。逆变换在第 265–269 行，相对结果是对的。真正写入时，第 74–76 行 `pose.translate(-camera)` 会把平移收成 float，第 300 行又把绝对的 `pointX/Y/Z` 收成 float。两组大数各自舍入后再相减，小数对不齐。

触发：水牢离世界原点较远（大约十万格以上会看出漂移，远地会跳）。

最小修复：顶点改传 `(float)(point - camera)`，矩阵只用平移前的 `rotation`。

其余核对过的行为与批准设计一致：每个牢一张正方形，宽高同一半边长；朝向用本机相机的前、上、左，右轴取左的反方向，含俯仰。八个角做 NDC 包围盒，近平面是 `m32/(m22-1)`，不是有限正数时用 `0.05`，平面深度是 `max(近平面+0.05, 最近深度-0.08)`。包络以快照 `bounds` 为底，再并上插值后的身体，聚能把盒子变大时画面跟着变大。UUID 优先，网络编号必须对上同一 UUID；死亡或 `isRemoved()` 这一帧不画；实体不在就只画 `bounds`；到期、换维、删除仍只靠 `visible()`。第一人称且目标就是相机实体时不画。帧是 `floor((gameTime-birthTick+partialTick)/2)` 对 16 取模，V 从 `frame/16` 到 `(frame+1)/16`。同一 `StillSheetMesh.of` 返回值同时用于 `getBuffer` 和 `endBatch`（半透明、LEQUAL、只写颜色、无剔除、无第二张反向面）。法线走 `normal(float,float,float)`，对应 `NEW_ENTITY` 的字节法线。本文件未见新的名称门禁违规。渲染器没有改协议或玩法。

贴图 SHA256 为 `8ba44798140ec92fc45b826549cec03cf629bcb7bcd1d34dab750462101e5096`，47640 字节，96×1536，与正式源逐字节相同，旁边没有 mcmeta。

读过：`implement.md`、`design-consolidate-report.md`、`implementation-report.md`、基线与当前 `PrisonShellRenderer.java`、`StillSheetMesh.java`、`PrisonClientState.java`、`StillSheetPlayback` 的绘制段、`WaterRingV8Math.relativeToCamera`，以及 Forge 47.3.33 映射源里的 `Camera`、`RenderLevelStageEvent`。`getPartialTick`、三个相机向量、`getEntity(int)` 都在。`entitiesForRendering` 与本仓 `StillSheetPlayback` 的用法相同。

未编译，未进游戏，未跑名称门禁脚本，未对服务端目录做全树差异，也未再展开 `GameRenderer`、`LevelRenderer`、`OverlayTexture` 源文件。编译和游戏验收都还没有做。