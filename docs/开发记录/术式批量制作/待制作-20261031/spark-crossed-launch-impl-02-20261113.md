# 火苗十字穿插第一人称眼下视觉偏置实现回执

- batchId: 待制作-20261031
- taskId: fire_spark_crossed_launch
- attemptId: 20261113-impl-spark-cross-02
- stage: implementation
- 操作角色: implement
- 请求模型: cursor/grok-4.7@500k · high + Fast（沿用客户端 service_tier="priority"；宿主未独立返回后端验证，不虚报已验证）
- 真实直接父代理: 主会话
- 结果: 已完成源码对齐；待统一编译；制作表与资源库未写回
- 复审计数: 设计 2/2、实现 3/3（本单不重置，未新增正式复审）

## 改动

- 开工前核对 `BatchCarrierVisualRenderer.java` SHA256 仍为 `ce80d0260c8cda80d868e17e4240279aea57173e84e761d34205a3a67779cb24`（`27,572` 字节），与前序 `20261113-impl-spark-cross-01` 一致后才写入。
- 新增 `private static final double SPARK_FIRST_PERSON_DROP = 0.245D;`。
- `drawSparkCross` 在单位正交上轴 `upAxis` 之后，用 `pose.pushPose()` 与 `pose.translate(-upAxis * SPARK_FIRST_PERSON_DROP)` 包裹两片 `putSparkFin`。矩阵与法线在平移之后取样，偏置会进入顶点。`try/finally` 保证 `pose.popPose()`。
- 偏置只沿已算出的 `-upAxis` 下移 `0.245` 格，对齐美术示意眼高下方 `3.92` 模型单位。不读相机，不改碰撞盒、宿主位置或留焰 `drawTrailRibbon`。
- 保持不变：`u = 1` 火头朝 `+tangent`、`u = 0` 火尾朝 `-tangent`、`±45°` `firstFin` / `secondFin`、`half`（来自 `halfSpan` `0.4F`）、`RenderType.entityCutout(sheet)`、`LightTexture.FULL_BRIGHT`、`NetherDomainTint`。
- 未改其余 5 个火苗相关 Java 文件、制作表、资源库、`记录.md` 或 `实测清单.md`。

## 文件指纹

| 文件 | 字节 | mtime | SHA256 | 本单 |
|---|---:|---|---|---|
| `src/main/java/com/bslx/XueXiMod/visual/facing/CameraFacingRegistry.java` | 1068 | 2026-11-13 11:00:43.585 | `0096b8cef6e46244d2ffc6fe66a9b5ba00fc037cab16ee56021fcb31fe40017b` | 未改，与前序一致 |
| `src/main/java/com/bslx/XueXiMod/client/CameraFacingClient.java` | 3187 | 2026-11-13 11:01:36.894 | `4f2af5a4b94f569898ec8f9be8c79ccae218ef0cca8ef63fbfb655f867674219` | 未改，与前序一致 |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitEntity.java` | 59101 | 2026-11-13 11:05:21.368 | `b2474242c425e955532b4ec4d2de4121d7760d2088cb1bc61d6787d57e7f029b` | 未改，与前序一致 |
| `src/main/java/com/bslx/XueXiMod/carrier/BatchFlightBook.java` | 21788 | 2026-11-13 11:00:53.124 | `dea2ab2bf22452133434ff9385446804e3be2fc07647bb9546e8ef53b0c1b23e` | 未改，与前序一致 |
| `src/main/java/com/bslx/XueXiMod/carrier/BatchCarrierVisual.java` | 21162 | 2026-11-13 11:01:03.531 | `2af18907d35327adf45f422d8ab5f81b2a99f73e3a91d7a97f1429bfa03b024b` | 未改，与前序一致 |
| `src/main/java/com/bslx/XueXiMod/client/BatchCarrierVisualRenderer.java` | 27963 | 2026-11-13 12:00:28.504 | `e9c4e19fbbd9af111b5091e06136fcca7c92767d00575ce1a6f2a5d8a6fb7387` | 本单写入 |

## 命名门禁

- 命令: `powershell -NoProfile -File tools/check-name-gate.ps1`
- 退出码: `1`。
- `BatchCarrierVisualRenderer.java`: `0` 违规。
- 全仓既有 `9` 处违规均不在本次触碰范围，未修改：`NetherDomainWisps.java` 3 处（`SPACING_MIN`、`WIDTH_MIN`、`HEIGHT_MIN`），`HeatUpSpatialMount.java` 6 处（`minX`、`maxX`、`minY`、`maxY`、`minZ`、`maxZ`）。

## 收束

- 编译: 待统一编译。本回执不执行 `gradlew` / `compileJava`。
- 写回: 制作表、资源库、`记录.md`、`实测清单.md` 均未写入。
- 下一步: 交回主会话后，由主会话派指定统一编译者运行 `compileJava`，再派唯一写表者更新制作表与批次记录。
- 当前实现与本回执写入已收束，文件句柄关闭。
