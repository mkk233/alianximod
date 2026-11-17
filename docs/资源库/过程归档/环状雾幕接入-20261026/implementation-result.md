# 雾雨领域环状雾幕实施结果

日期：2026-10-26。按本目录 `brain-decision.md` 只改雾雨领域 VOLUME 的外围视觉、雨丝 overlay，以及雾环和雨格的距离排序。未改领域定义、边界、碰撞、范围、持续时间、伤害或重压。未 `build`、未打 jar、未启动或操控游戏、未提交、未推送。

## 粉色根因

`DomainWorldRenderer` 旧雾盒和 `DomainMistRainRenderer` 原先都写 `overlayCoords(0)`。本机 1.20.1 的 `OverlayTexture.NO_OVERLAY` 是 `pack(0, 10)`，不是 0。overlay 纹理前几行是受伤红，`pack(0, 0)` 会把白色顶点和雾一起染粉。这是雾和雨的共同代码原因，不是缺纹理。

本次雾环和细雨都改经 `DomainVolumeBatch` 写 `OverlayTexture.NO_OVERLAY`（`DomainVolumeBatch.java:86`）。没有靠改青色、关闭全部 overlay 或把 alpha 加倍来抵消。非 VOLUME 的旧六面水纹盒仍是 `overlayCoords(0)`（`DomainWorldRenderer.java:113`），重压文件未改，其 `DomainPressureRenderer.java:166` 仍是 0。

## 修改清单

改过的已有文件，修改前已按仓库相对路径复制到本目录 `baseline/`。新文件没有 baseline。

| 动作 | 路径 | 说明 |
| --- | --- | --- |
| 修改 | `src/main/java/com/bslx/XueXiMod/client/DomainWorldRenderer.java` | VOLUME 不再画六面水纹。其他非重压视觉仍画含顶底的六面。VOLUME 收集雾环和细雨后统一提交。 |
| 修改 | `src/main/java/com/bslx/XueXiMod/client/DomainSheetMesh.java` | 细雨和旧水纹继续原版实体透明 shader。雾环另持静态专用 shader 状态。 |
| 修改 | `src/main/java/com/bslx/XueXiMod/client/DomainMistRainRenderer.java` | 几何、相位、密度和距离裁剪不变。改为把雨格交给排序批次。 |
| 新增 | `src/main/java/com/bslx/XueXiMod/client/DomainMistRingLayout.java` | 16 面内接椭圆、连续周向 U、整数圈闭合。 |
| 新增 | `src/main/java/com/bslx/XueXiMod/client/DomainMistRingRenderer.java` | VOLUME 雾环入口。只提交一份外向面。 |
| 新增 | `src/main/java/com/bslx/XueXiMod/client/DomainVolumeBatch.java` | 雾环与雨格按中心距离由远到近提交。 |
| 新增 | `src/main/java/com/bslx/XueXiMod/client/DomainMistShaders.java` | 客户端 `RegisterShadersEvent` 注册雾幕 shader。 |
| 新增 | `src/main/resources/assets/xueximod/textures/domain/mist_panel.png` | 归档 PNG 原样复制。 |
| 新增 | `src/main/resources/assets/xueximod/textures/domain/mist_panel.png.mcmeta` | 只声明 `blur: false`、`clamp: false`。 |
| 新增 | `src/main/resources/assets/xueximod/shaders/core/rendertype_entity_translucent_mist.vsh` | 与本机 1.20.1 实体透明顶点 shader 相同。 |
| 新增 | `src/main/resources/assets/xueximod/shaders/core/rendertype_entity_translucent_mist.fsh` | 只把 `alpha < 0.1` 改成 `alpha <= 0.0`。 |
| 新增 | `src/main/resources/assets/xueximod/shaders/core/rendertype_entity_translucent_mist.json` | 只把顶点、片元名改到 `xueximod:` 下的上述文件。 |
| 修改 | `docs/资源库/特效/水流/单面青白雾幕_v1/README.md` | 采用状态、游戏路径、盒子范围仍在、未实测。 |
| 修改 | `docs/资源库/README.md` | 总索引改为已接入，并写明未实测。 |

未改：`DomainDefinition`、`DomainBoundary`、`DomainPressureRenderer`、细雨 PNG，以及 `docs/资源库/特效/水流/水雾细雨80度_v1/README.md`。那份 2026-10-25 记录仍写着雾盒与细雨共用网格；当前 VOLUME 外围已改为雾环，细雨网格本身还是 `DomainSheetMesh.of`。PSD、单帧、GIF 和预览底色仍只在资源库。

baseline 复制时 SHA-256：

- `baseline/src/main/java/com/bslx/XueXiMod/client/DomainWorldRenderer.java`：`78565A1DC85E40023D4473D8267F4983BF29432738EC96D9923076464A49099A`
- `baseline/src/main/java/com/bslx/XueXiMod/client/DomainSheetMesh.java`：`ACC8CC14267B5DE1AE7D4AC4BA9801D8ECE917810457A0D9D4551AF1BB3D429F`
- `baseline/src/main/java/com/bslx/XueXiMod/client/DomainMistRainRenderer.java`：`DB4735A31125EF366286A1E9BB089A98F1F4C2CA4DC143A9AE0EAAF277528FA1`
- `baseline/docs/资源库/README.md`：`C5F215359E5FD102463FED754A4FEDBD1BB0550944834D5322C7B61B4F724763`
- `baseline/docs/资源库/特效/水流/单面青白雾幕_v1/README.md`：`26EC28750A50A1F02CCCB896CF32B59C0EBEA5B3A576062F5677F8BC03E8B233`

## 几何

`DomainWorldRenderer.java:52-54` 见到 VOLUME 就跳过六面水纹。顶面在第 70 行，底面在第 71 行，只有非重压且非 VOLUME 才执行。重压仍走原来的 `DomainPressureRenderer`。

雾环中心是 bounds 的水平中心，半径是半宽和半深，上下是 `minY`/`maxY`。这是内接椭圆，不代表方形四角。16 个侧面首尾用同一组端点，最后一面回到第一点。每个侧面只 `addQuad` 一次（`DomainMistRingRenderer.java:41-54`），没有反向面，也没有顶底盖。`RenderType` 为 `NO_CULL`。

周向 U 按累计边长连续增加，不在每个面重置到 0。整圈 U 的终点是正整数圈：`round(周长 / (2 * 高度))`，至少 1。默认 7×7×8 的十六边形周长比约为 1.37，取 1 圈。周长通常不是高度的两倍，闭合优先于严格 2:1，横向会有小幅比例差。U 不按面取模。纵向 `localV` 只走当前帧，上下各收半个纹素（`0.5/64`），`v = (frame + localV) / 32`。

环的帧是 `floorMod(gameTime - visualOrigin + animSeed, 32)`，16 面共用，算在循环外（`DomainMistRingRenderer.java:32-33`）。`visualOrigin <= gameTime < visualEnd` 才收集。快照移除后不再画，没有另建客户端余留。

顶点色是 `(255, 255, 255, 255)`，不再乘旧雾盒的 `(90, 170, 255, 70)`。

## 远坐标精度

审查 P2：原先把世界中心、高度和 X/Z 顶点存成 float，渲染时又先把相机转成 float 再相减。千万级坐标上 float 间距会到 1 格，7 格宽的 16 边形可能把相邻点收成同一点，零边长检查会让整圈不生成。

现在水平顶点只存相对中心的 double 局部坐标（`DomainMistRingLayout.java:46-47`）。边长和法线用这些局部差值计算，零边长阈值仍是 `1.0E-4`，没有放宽（第 54、61 行）。世界中心和上下高度保持 double。提交时先做 `center - camera`，再加上局部偏移，最后才转 float（`relativeX` 第 109-110 行，高度第 117-122 行）。`DomainMistRingRenderer.java:36-46` 传入的是相机的 double 分量。面数、U、帧、颜色、shader 和排序都没改。

## 细雨

格子大小、80° 贴图方向、32 帧相位、48/64 格裁剪和两组交叉都没改。`collect` 只把原来的局部坐标加上 `盒子最小点 - 相机`，与原先姿态平移等价。雨仍用 `DomainSheetMesh.of`，也就是原版 `rendertype_entity_translucent`。雨的 alpha 档是 0、46、99、168，不受 0.1 丢弃阈值影响。

## 排序与材质

同一帧里所有 VOLUME 雾环面和雨格放进一个列表，按相机空间中心距离由远到近排序（`DomainVolumeBatch.java:42`）。纹理引用变化时只 `endBatch(该 RenderType)`（第 49、57 行），连续同一纹理合并。没有无参数 `endBatch()`。

这只覆盖本次雾环和细雨。重压、方块和其他透明物不在这张列表里。大环面用中心距离，靠近环面边缘的雨格仍可能和该面局部交错。同一次纹理提交里，原版 `sortOnUpload` 还可能按它自己的距离再排这一小段；跨纹理的先后由上面的 `endBatch` 固定。不能据此说全部透明排序已经修好。

雾环 `RenderType` 在 `DomainMistRingRenderer` 的静态字段里创建一次。专用 `ShaderStateShard` 也是静态的（`DomainSheetMesh.java:11`），每帧不重新 `create`。深度测试仍是 `LEQUAL`，写掩码是 `COLOR_WRITE`，最近邻且无 mipmap。没有用全局 `depthMask` 开关。

## 低 alpha shader

片元 shader 以本机 Minecraft 1.20.1 `client-extra.jar` 里的 `rendertype_entity_translucent.fsh` 为底。逐字比对：顶点 shader 相同；片元只有第 23 行 `if (color.a < 0.1)` 改为 `if (color.a <= 0.0)`；json 只有第 6、7 行顶点与片元名改为 `xueximod:rendertype_entity_translucent_mist`。光照、雾化、采样和 overlay 混合都还在。

`DomainMistShaders.java:24-29` 在客户端 mod 总线的 `RegisterShadersEvent` 注册，格式是 `DefaultVertexFormat.NEW_ENTITY`。雾环 `RenderType` 引用 `DomainMistShaders::curtain`。细雨和其他材质不引用它。

游戏 PNG 与归档 SHA-256 都是 `393488678ED4E66C4C31650B92282067C299C6C0A443B79D09CB5FB72E62F14E`。IHDR 为 128×2048、位深 8、颜色类型 6。ImageIO 统计 alpha 只有 0（121120）、8（57720）、18（60536）、26（21080）、36（1688），像素总数 262144。8 和 18 低于 0.1，要靠专用 shader 留下。没有把顶点或贴图 alpha 加倍。

## 静态验证

`javac` 后运行 `com.bslx.XueXiMod.client.DomainMistRingLayout`，输出 `RING_LAYOUT_OK turns=1 wideTurns=31 ellipseTurns=1 far=30000000`。检查了 16 面共边并回到首点、周向 U 不逐面归零、终点等于整数圈、外向法线、7×7×8 为一圈、40×2×40 为 31 圈、零宽度被拒绝，以及 32 帧纵向 UV 都落在本帧内。角度 0 的世界位置仍是宽边中点 `(7, 3.5)`，不是盒子方角。

同一套 7×7×8 还放到 `+30000000`、`-30000000`，以及 X 为 `+30000000`、Y/Z 为 `-30000000`。三组都生成了 16 面，闭合检查通过。相机取 `(1.25, 2.5, -0.75)` 再加上同样的平移后，16 个顶点、上下沿、16 条边、周向 U 和法线都与原点布局的 float 结果一致，没有边长退化。

资源路径是 `assets/xueximod/textures/domain/mist_panel.png` 和 `assets/xueximod/shaders/core/rendertype_entity_translucent_mist.json`。这只说明文件在资源根下，不能代替运行时加载。

## 编译与命名

命令：

`.\gradlew.bat -g C:\Users\baishang\AppData\Local\Temp\cursor-sandbox-cache\a6dd7201ae0ea39bc33e5d675cf9f1cf\gradle compileJava --offline --no-daemon --console=plain`

结果：**BUILD SUCCESSFUL**，27 秒，退出码 0。4 个任务里 2 个执行、2 个 UP-TO-DATE。无错误。18 个既有的 `ResourceLocation` 构造器和 `HitVolumes.setCanceled` 弃用警告，其中包含本次新增的雾环贴图和 shader 路径。没有为消除这些警告去改重压或其他文件。

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 1。本次改动的类不在违规列表。未改文件仍是：

- `animation/runtime/OriginalHumanoid.java:165` 变量 `i`
- `animation/runtime/OriginalHumanoid.java:202` 变量 `to`

没有顺手改这两处。

远坐标修复后用同一 Gradle 缓存重跑了这条命令。结果：**BUILD SUCCESSFUL**，22 秒，退出码 0。4 个任务里 2 个执行、2 个 UP-TO-DATE。无错误。仍是原来的 18 个弃用警告，没有为它们改别的文件。

`powershell -NoProfile -File tools/check-name-gate.ps1` 再次退出码 1。本次改动的类不在违规列表。未改文件仍是 `OriginalHumanoid.java:165` 的 `i` 和 `:202` 的 `to`。

`compileJava` 不能加载或执行 GLSL。游戏内粉色、环形观感、低 alpha 档、透明交错和远坐标雾环都还没有看过。

## 最终独立复核
GPT-6-astra 已复核远坐标精度修正并关闭P2；Cursor Grok 4.7 High Fast 独立只读审查最终版本通过，未发现可证实缺陷。详见 review-brain.md、review-cursor.md。最终 compileJava 离线成功22秒；未打包，未游戏测试，GLSL运行时加载与实际观感由用户复核。
