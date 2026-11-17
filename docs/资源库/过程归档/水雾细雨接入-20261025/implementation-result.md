# 水雾细雨接入实施结果

日期：2026-10-25。执行方按 `brain-decision.md` 的最小范围改客户端雨层与素材索引。未改领域玩法、费用、范围、持续时间、边界、伤害、水刃、一分为三或网络协议。未 `build`、未打 jar、未启动或操控游戏、未提交、未推送。

## 修改清单

改过的已有文件，修改前已按仓库相对路径复制到本目录 `baseline/`。新文件没有 baseline。

| 动作 | 路径 | 说明 |
| --- | --- | --- |
| 修改 | `src/main/java/com/bslx/XueXiMod/client/DomainWorldRenderer.java` | 雾盒几何与顶点色不变。批次改为先画完非重压雾盒并 `endBatch`，再只对 `DomainVisual.VOLUME` 画细雨并结束雨批次，最后才走原来的重压分支。雾盒改用 `DomainSheetMesh`。 |
| 新增 | `src/main/java/com/bslx/XueXiMod/client/DomainSheetMesh.java` | 雾盒与细雨专用透明类型：实体透明格式、shader、光照、overlay、混合、无剔除，`COLOR_WRITE`，`LEQUAL`。 |
| 新增 | `src/main/java/com/bslx/XueXiMod/client/DomainMistRainRenderer.java` | VOLUME 内部竖直雨幕，使用同一个 `DomainSheetMesh`。 |
| 新增 | `src/main/resources/assets/xueximod/textures/domain/mist_rain_80.png` | 从归档 PNG 原样复制。 |
| 新增 | `src/main/resources/assets/xueximod/textures/domain/mist_rain_80.png.mcmeta` | 只声明 `texture.blur=false`、`texture.clamp=false`。没有动画段。 |
| 修改 | `docs/资源库/特效/水流/水雾细雨80度_v1/README.md` | 采用状态、游戏路径、显式选帧。写明只编译、未游戏实测。 |
| 修改 | `docs/资源库/README.md` | 索引改为已接入，并写游戏路径与未实测。 |

未改：`DomainPressureRenderer`、`DomainDefinition`、`DomainSightStore`、`DomainSyncPacket`，以及费用、范围、持续时间和原生效果。

baseline 相对路径与复制时 SHA-256：

- `baseline/src/main/java/com/bslx/XueXiMod/client/DomainWorldRenderer.java`：`C4C858993E9F365FE4D00C8ACFB81A4AF37E56ECA9748DECCB5F98A02FE3C71E`
- `baseline/docs/资源库/README.md`：`800ABADBD2722298C3F76C15C49605218D35757BEDCB87C3DE64017E72A4793B`
- `baseline/docs/资源库/特效/水流/水雾细雨80度_v1/README.md`：`2E9843664331C3A1FC3F796C36A6882F8D2DE6ED11E1D1565303BE145C4B7D38`

## 渲染规则

调度在 `AFTER_TRANSLUCENT_BLOCKS`。雾盒顶点色仍是 `(90, 170, 255, 70)`，贴图仍是 `minecraft:textures/block/water_still.png`。雾盒和细雨都用 `DomainSheetMesh.of`，不写深度。重压仍调用 `DomainPressureRenderer.draw/end`，该类源码未改，仍是会写深度的 `entityTranslucent`。

雨只在 `sight.visual() == VOLUME` 时画。非重压且非 VOLUME 的未来视觉仍只走原来的雾盒，不会自动带雨。当前 VOLUME 定义只有 mistRain，因此没有新枚举，也没有按术式 id 分支。

起止：`gameTime < visualOrigin` 或 `gameTime >= visualEnd` 时不画。不使用部分刻插值。快照删除后不再有该 sight，不另留客户端实体，也不清理服务端领域。

帧：`frame = floorMod(gameTime - visualOrigin + phase, 32)`。`phase = floorMod(animSeed + (group + slot) * 5, 32)`。X-Y 组 `group=0`，Z-Y 组 `group=16`。近处槽位 `0,1,2`，48 格外只留中间槽位 `1`，所以跨过 48 格时留下的那面相位不变。相位不来自逐帧随机或刷新次数。

雨幕在盒子内部，世界 Y 轴竖直，不是广告牌：

- 距离按盒子中心。`reach > 64` 不画。`reach > 48` 只留两组的 50% 位置各一面。否则 X-Y 面在 Z 的 28%、50%、72%，Z-Y 面在 X 的 28%、50%、72%。
- 每面从边界内收 `0.08` 格后再按 `2×2` 分格。宽高都用 `CELL_SIZE`，完整格世界宽高相等。
- 边缘不足 2 格时，几何收到实际剩余长度，`localU/localV` 收到 `剩余/2`，不把整帧 UV 拉满到这个短边。
- 画面上方 `localV` 较小，下方较大，雨丝向下。`u = localU`，`v = (frame + localV) / 32`。
- 完整边的 0 和 1 各收半个纹素（`0.5/64`），避免最近邻在帧缝或横向重复处采到相邻帧。裁在格子中间的局部坐标不收。
- 每个格子只提交一个四边形。雾盒与细雨的 `RenderType` 沿用实体透明的 `NEW_ENTITY`、`rendertype_entity_translucent` shader、光照、overlay、透明混合和无剔除，纹理 `blur=false` 且 `mipmap=false`，写掩码为 `COLOR_WRITE`，深度测试为 `LEQUAL`。不调用 `depthMask`。不叠反向面。顶点色 `(255,255,255,255)`，不乘雾盒的 70。光照沿用雾盒的 `uv2(0x00F000F0)`。
- 雾、雨、重压各自 `getBuffer` 后 `endBatch(具体 RenderType)`。雨的 `VertexConsumer` 不跨到别的 `getBuffer`。没有调用无参数 `endBatch`。

7×7×8、内收 0.08 后，单面约 4×4 格。近处 6 面约 96 个四边形，48 到 64 格约 32 个。不为雨丝生成实体或粒子。

## 素材核对

归档与游戏 PNG 的 SHA-256 都是 `4D578C45469F3F9265AE254BD07A4BE8AA09B36B17D5C6512604DA2B544A66C4`，长度都是 532257。IHDR 为 64×2048、位深 8、颜色类型 6（RGBA）。哈希相同，因此 alpha 与归档一致；归档 `验证记录.json` 记录的 alpha 为 0、46、99、168。未复制 GIF、预览底色或 PSD。

帧 UV 抽查：完整格 `localV` 使用 `0.5/64` 到 `1-0.5/64` 后，32 帧的 v 都落在本帧区间内，且下沿小于 `(frame+1)/32`。`floorMod(31+1, 32) = 0`。示例：第 4 帧、竖向覆盖 0.92 的边缘格，`localV` 为 0.08 到 0.9921875，对应 v 为 0.1275 到 0.156005859375，落在 `[4/32, 5/32)`。`gameTime == visualOrigin` 且相位 0 时帧为 0；更早或 `gameTime >= visualEnd` 的分支直接返回。

## 深度修正

核对后雾盒与细雨不再使用会写深度的 `entityTranslucent`。新增 `DomainSheetMesh`，两层各持有自己的静态 `RenderType` 并分别 `endBatch`。几何、雾色 `(90, 170, 255, 70)` 和雨色 `(255, 255, 255, 255)` 未改。没有全局 `depthMask`。`ReleaseDefinitions.java` 未改；第 23 行末尾现为 `20f, 26, 6, false`。

## 验证结果

当前编译结果：`gradlew.bat compileJava --no-daemon` **BUILD SUCCESSFUL**，7 分 22 秒，退出码 0。4 个任务里 2 个执行、2 个 UP-TO-DATE。无错误。16 个既有弃用警告，含雾盒、细雨和重压贴图的 `ResourceLocation` 构造器，以及 `HitVolumes.setCanceled`。没有为消除这些警告去改重压或其他文件。

同一次修复之前的 `compileJava --offline --no-daemon` 仍因 foojay `0.7.0` 缓存缺失在约 12 秒失败。未改 Gradle 配置，未清缓存。上面的成功结果来自裁决允许的这一次联网编译，取代早先因 `water_weeping_arc` 缺参而失败的记录。

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 1。`DomainSheetMesh`、`DomainWorldRenderer`、`DomainMistRainRenderer` 不在违规列表。未改文件仍是：

- `animation/runtime/OriginalHumanoid.java:113` 变量 `i`
- `shu/technique/release/motion/TrackingSupport.java:16` 字段 `MAX_TURN`

没有顺手改这两处。没有打包，没有启动游戏。

## 风险与未验证

- 雾盒和细雨现在不写深度，实心方块仍能用 LEQUAL 挡住它们。雾、雨、重压仍是三次独立批次，精确跨材质排序没有在游戏里看过。重压自己的 `entityTranslucent` 仍会写深度。
- 两组雨幕在盒内交叉。雨批次内部会按距离排序，但不能和雾盒、重压排进同一次排序。内部层叠和不同视角的疏密都没在游戏里看过。
- 顶点 alpha 为 255，密度只靠 6 面或 2 面控制。实际是否过密或几乎看不见，未实测。
- 半纹素内收让完整边的 `localU/localV` 不是严格的 0 和 1。这是为了不串帧。边缘格只有落在 0 或 1 的那条边内收，世界宽高比会差半个纹素。
- X-Y 面的纹素向 +X 倾斜，Z-Y 面朝 +Z 倾斜。这是两组固定朝向，不是同一个水平风向。屏幕上的夹角仍会随观察方位变化。
- 以后若再注册别的 `VOLUME` 领域，会沿用这套细雨。要按术式分开，需要新的视觉类型或协议，本次没有做。
- 游戏内雾中可见度、盒内观看、暂停时停帧、64 格外消失，都还没有实测。
