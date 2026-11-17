# 火苗十字穿插实现回执

- batchId: 待制作-20261031
- taskId: fire_spark_crossed_launch
- attemptId: 20261113-impl-spark-cross-01
- stage: implementation
- 操作角色: implement
- 请求模型: cursor/grok-4.7@500k · high + Fast（沿用客户端 service_tier="priority"，不虚报后端已独立验证 Fast）
- 结果: 已完成源码实现；待统一编译；制作表与资源库未写回

## 改动

- `SPARK` 已从持续面向玩家登记移除，返回 `CameraFacingTag.NONE`。其余 `SEED`、`KINDLE`、`DETONATE`、`DUST`、`FENTIAN_SKY_FLAME`、`FIRE_SEED_ORBIT_FLAME`、`WATER_PRISON_SHELL` 仍为 `CAMERA_PLANE`，`SWORD_RETURN_ALL_VORTEX` 仍为 `UPRIGHT_CAMERA_YAW`。
- 火苗屏幕滚转、`sparkRollRadians`、`SPARK_ROLL_FLOOR_PROJECTION` 和无用导入已移除。`visual/facing` 仍无 `net.minecraft.client.*` 导入。
- 火苗生成时通过只读 `TechniqueHitEntity.launchDirection()` 取得唯一单位发射方向，并写入既有 `DATA_TANGENT_X/Y/Z`。方向生成后锁定，不新增同步字段，不读取相机、位移或 `deltaMovement`。
- 方向优先级为：垂落锚点斜向、锁定目标中心、喷涌初始速度方向，其余使用 `applySpec` 已决定的 `forward`（含强制方向、交错瞄准和普通直线），无效时回退 `(0, 0, 1)`。
- 客户端对 `SPARK + NONE` 绘制 ±45° 双面十字穿插，半幅 `0.4`。`u=1` 在发射前方，`u=0` 在后方；使用 `RenderType.entityCutout`、全亮和原有幽冥染色。
- 未改碰撞、伤害、运动、生命周期、制作表、资源库、`记录.md` 或 `实测清单.md`。

## 文件指纹

| 文件 | 字节 | mtime | SHA256 |
|---|---:|---|---|
| `src/main/java/com/bslx/XueXiMod/visual/facing/CameraFacingRegistry.java` | 1068 | 2026-11-13 11:00:43.585 | `0096b8cef6e46244d2ffc6fe66a9b5ba00fc037cab16ee56021fcb31fe40017b` |
| `src/main/java/com/bslx/XueXiMod/client/CameraFacingClient.java` | 3187 | 2026-11-13 11:01:36.894 | `4f2af5a4b94f569898ec8f9be8c79ccae218ef0cca8ef63fbfb655f867674219` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitEntity.java` | 59101 | 2026-11-13 11:05:21.368 | `b2474242c425e955532b4ec4d2de4121d7760d2088cb1bc61d6787d57e7f029b` |
| `src/main/java/com/bslx/XueXiMod/carrier/BatchFlightBook.java` | 21788 | 2026-11-13 11:00:53.124 | `dea2ab2bf22452133434ff9385446804e3be2fc07647bb9546e8ef53b0c1b23e` |
| `src/main/java/com/bslx/XueXiMod/carrier/BatchCarrierVisual.java` | 21162 | 2026-11-13 11:01:03.531 | `2af18907d35327adf45f422d8ab5f81b2a99f73e3a91d7a97f1429bfa03b024b` |
| `src/main/java/com/bslx/XueXiMod/client/BatchCarrierVisualRenderer.java` | 27572 | 2026-11-13 11:05:15.595 | `ce80d0260c8cda80d868e17e4240279aea57173e84e761d34205a3a67779cb24` |

## 命名门禁

- 命令: `powershell -NoProfile -File tools/check-name-gate.ps1`
- 本次触碰的 6 个 Java 文件: 0 违规。
- 全仓既有 9 处违规，均不在本次触碰范围：`NetherDomainWisps.java` 3 处，`HeatUpSpatialMount.java` 6 处。门禁因此以退出码 1 结束；这些既有文件未修改。

## 收束

- 编译: 待统一编译。本回执不执行 `compileJava`。
- 写回: 制作表、资源库、`记录.md`、`实测清单.md` 均未写入。
- 当前实现写入已收束，文件句柄关闭。
