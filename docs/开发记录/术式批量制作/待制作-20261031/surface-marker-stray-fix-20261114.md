# 地表定位标记串台修正

- batchId：`待制作-20261031`
- taskId：`surface_marker_stray_fix`
- attemptId：`20261114-impl-surface-marker-01`
- stage：`implementation`
- 操作角色：实现并编译
- 额度：设计 `2/2`、实现 `3/3` 已用完。本阶段额度已用完，返修结果未追加复审。不重置额度。
- 制作表：未写。未改 `docs/术式制作表.xlsx`、`记录.md`、`实测清单.md`。
- 游戏实测：未做。

## 改动

- `src/main/java/com/bslx/XueXiMod/carrier/BatchFlightBook.java` 第 78–80 行。`publish` 里只保留 `spawnBodyVisual`。删掉原第 80–83 行：`CastFrame.current().anchor != null` 就 `BatchTriggerVisuals.surface`。
- `src/main/java/com/bslx/XueXiMod/carrier/BatchShockRingEffect.java` 第 65–66 行。`ringLife` 之后直接进入 `strike`。删掉原第 66–68 行：`frame.origin` 或 `frame.anchor` 非空就 `BatchTriggerVisuals.surface`。
- `src/main/java/com/bslx/XueXiMod/network/BatchCastSession.java` 第 3 行新增 `BatchTriggerVisuals` 导入；第 97–105 行。仅当 `plan.needsSurface` 且 `SurfaceAnchor.resolve` 返回非空后，第 105 行对 `sample.point()` 调一次 `BatchTriggerVisuals.surface(owner.serverLevel(), sample.point())`。

## 修改前后

修改前：火苗、陨岩载体出生时，只要当前 `CastFrame.anchor` 非空就在该点播地表定位标记。热波在 `frame.origin` 或 `frame.anchor` 非空时，也在热波点播同一张标记。幽冥领域自动触盖会把空中发射点写入这个通用几何字段，所以未装配地表定位也会出现淡青圆环和黄色下箭头。

修改后：这两处不再播。合法播放只在 `BatchCastSession.open`：`plan.needsSurface` 为真且 `SurfaceAnchor.resolve` 成功时，在 `sample.point()` 播一次。`sample == null` 的拒绝返回在播放之前结束，不播。播放位于冻结点赋值之后、扣费和冷却之前；冻结点、扣费、冷却和弹道未改。

## 编译

源码写完并关闭后才编译。工作目录 `D:\alianximod`。命令全文：

```
.\gradlew.bat compileJava --offline --no-daemon
```

- 开始：`2026-11-14 00:13:43.403 +08:00`
- 结束：`2026-11-14 00:14:35.093 +08:00`
- 退出码：`0`
- 结果：`BUILD SUCCESSFUL in 50s`。有既有的过时 API 注解，不在本次三个文件，未改。
- 是否晚于最后一次相关 Java 写入：是。最晚写入是 `BatchCastSession.java` `2026-11-14 00:13:20.804 +08:00`。同批还有 `BatchShockRingEffect.java` `00:13:20.792`、`BatchFlightBook.java` `00:13:14.132`。编译开始于 `00:13:43.403`。

## 未改边界

- 未改 `BatchSurfaceAnchorEffect` 的播放逻辑。
- 未改地表定位选点规则、贴图 `assets/xueximod/textures/effect/surface_anchor_v1/surface_anchor.png`、`BatchVisualTexture.SURFACE`、`CameraFacing`。
- 未改载体视觉、留焰、命中和移除回调。
- 未改热波判定、环体视觉和伤害。
- 未改冻结点赋值、扣费、冷却和弹道。
- 未打包，未 `build` / `jar` / `reobf` / `runClient`，未启动游戏。

## 实测

- 前置：装配幽冥、领域绑定、火苗，且不装配地表定位；另备一套真正装配地表定位并能瞄准可落地面的对照。
- 步骤：空中释放火苗，看射出点；再用地表定位装配瞄准地面释放，看锚点；顺带确认火苗、热波、陨岩的攻击和弹道。
- 预期：幽冥领域绑定加火苗时，空中射出点不再出现地表定位圆环箭头；真正装配地表定位并瞄准地面时，标记仍在地表锚点播一次；火苗、热波、陨岩的原有攻击和弹道不变。
- 用户反馈：
