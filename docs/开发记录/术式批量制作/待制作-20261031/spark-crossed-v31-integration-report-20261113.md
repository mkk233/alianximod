# 火苗 v3.1 十字穿插直射确认版运行接入报告

- batchId: 待制作-20261031
- taskId: fire_spark_crossed_launch
- attemptId: 20261113-integrate-spark-cross-01
- stage: implementation
- 操作角色: implement（确认版正式运行接入核对；本单资源库 `README.md` 接入状态的唯一作者）
- 请求模型: cursor/grok-4.7@500k · high + Fast（沿用客户端 `service_tier="priority"`；宿主未独立返回后端验证，不写成已验证事实）
- 真实直接父代理: 主会话
- 授权: 2026-11-13 用户对火苗 v3.1 三维视口预览回复「采用」；美术归档 `20261113-art-spark-cross-archive-01` 已完成
- 结果: 运行贴图一致，已补 `fire_spark.png.mcmeta`，Java 参数与 `visual-handoff.md` 一致故未改源码；待统一编译；制作表未写
- 复审计数: 设计 2/2、实现 3/3（本单不重置，未新增正式复审）

## 贴图

- 确认导出 `docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/fire_spark_v3_strip.png` 与运行贴图 `src/main/resources/assets/xueximod/textures/effect/fire_spark_v3/fire_spark.png` 均为 `5,005` 字节，SHA256 `672DA9132E7E91A83636836A62ADE4778781D51592A541041C511653238DEC5E`。
- 旧运行贴图 `src/main/resources/assets/xueximod/textures/effect/fire_spark_v1/fire_spark.png` 保留：`6,788` 字节，SHA256 `07407B8570B6EEAF9EFC764BD1365F1589A9C5427EDE4A8B1B9748D03E7DF58E`，未删除。
- 新增 `src/main/resources/assets/xueximod/textures/effect/fire_spark_v3/fire_spark.png.mcmeta`：UTF-8 无 BOM，`64` 字节，与 `flame_trail_v3/flame_trail.png.mcmeta` 逐字节一致，SHA256 `B2CAA414505E1C556CF68460294471D97198A50F3D69ED1FD9B717510C0D6F4A`。内容为 `texture.blur=false`、`texture.clamp=true`。

## 运行资源隔离

- 全量扫描 `src/main/resources/`：没有 `.bbmodel`，没有文件名含 `示意辅助`，也没有 `helper_*`。命中数 `0`。
- `.bbmodel` 与示意辅助仍只在资源库 `火苗_十字穿插直射_v3.1/`。

## Java 核对

参数与 `visual-handoff.md` 一致，本单没有重写 Java。`impl-02` 的 6 个文件指纹在写入 `.mcmeta` 与文档之后再次核对，字节、mtime 与 SHA256 都未变。

| 文件 | 字节 | mtime | SHA256 | 本单 |
|---|---:|---|---|---|
| `src/main/java/com/bslx/XueXiMod/visual/facing/CameraFacingRegistry.java` | 1068 | 2026-11-13 11:00:43.585 | `0096b8cef6e46244d2ffc6fe66a9b5ba00fc037cab16ee56021fcb31fe40017b` | 未改 |
| `src/main/java/com/bslx/XueXiMod/client/CameraFacingClient.java` | 3187 | 2026-11-13 11:01:36.894 | `4f2af5a4b94f569898ec8f9be8c79ccae218ef0cca8ef63fbfb655f867674219` | 未改 |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitEntity.java` | 59101 | 2026-11-13 11:05:21.368 | `b2474242c425e955532b4ec4d2de4121d7760d2088cb1bc61d6787d57e7f029b` | 未改 |
| `src/main/java/com/bslx/XueXiMod/carrier/BatchFlightBook.java` | 21788 | 2026-11-13 11:00:53.124 | `dea2ab2bf22452133434ff9385446804e3be2fc07647bb9546e8ef53b0c1b23e` | 未改 |
| `src/main/java/com/bslx/XueXiMod/carrier/BatchCarrierVisual.java` | 21162 | 2026-11-13 11:01:03.531 | `2af18907d35327adf45f422d8ab5f81b2a99f73e3a91d7a97f1429bfa03b024b` | 未改 |
| `src/main/java/com/bslx/XueXiMod/client/BatchCarrierVisualRenderer.java` | 27963 | 2026-11-13 12:00:28.504 | `e9c4e19fbbd9af111b5091e06136fcca7c92767d00575ce1a6f2a5d8a6fb7387` | 未改 |

对照结果：

- `BatchVisualTexture.SPARK` 为 `textures/effect/fire_spark_v3/fire_spark.png`。该文件本单只读，`1,188` 字节，SHA256 `dd8d646312cb66c64f1bea53a99d7e54198dbfd50e749241bfac77458d742b56`。
- `CameraFacingRegistry.tagOf(BatchVisualTexture.SPARK)` 为 `CameraFacingTag.NONE`。
- `CameraFacingClient` 没有 `SPARK` 滚转分支；只有已声明 `CAMERA_PLANE` 的贴图才乘相机朝向，注释写明火苗不再持续面向玩家。
- `TechniqueHitEntity.launchDirection()` 只读生成当刻已决定的出生方向并归一化，不推进运动、碰撞或伤害。
- `BatchFlightBook.spawnBodyVisual` 对火苗调用 `spawnSpark(..., duration, 0.4F, ...)`。
- `BatchCarrierVisual.spawnSpark` 只在生成时写入一次归一化 `DATA_TANGENT_X/Y/Z`，贴图为 `SPARK`，并绑定宿主。
- `drawSparkCross`：`SPARK_FIRST_PERSON_DROP = 0.245D`，沿 `-upAxis` 平移；`firstFin = (upAxis + sideAxis) / √2`，`secondFin = (upAxis - sideAxis) / √2`；`u=1` 在 `+tangent` 火头，`u=0` 在 `-tangent` 火尾；`RenderType.entityCutout`；非尘土/地表锚点使用 `LightTexture.FULL_BRIGHT`；`acceptsFlameTint` 包含 `SPARK`，颜色走 `NetherDomainTint`。帧采样使用 `STRIP_FRAMES = 16`。
- `ListedRelease.spark()` 为 `fire_spark`、直线、`lifetimeTicks=30`、`speed=0.8`、`thickness=0.25`、出生半径 `0.125/0.125`、产种基数 `2`。`declareCarrier(spark(), true)` 仍在，产种范围 `0..FireLimits.SEED_CAP`，`SEED_CAP = 100`。`ListedRelease.java` 本单只读，`11,943` 字节，SHA256 `78e1f1c6303e9689c68f2017e05084588adab8d1c364a4e7238f80197450da8c`。
- 标准伤害、冷却与魔力仍由既有声明承担，未改：`ModTechniques.FIRE_SPARK` 基础伤害 `1`、基础冷却 `5`、等级上限 `20`；`Technique.manaCost()` 在没有显式魔力时为 `ceil(基础伤害 × 2) = 2`；`BalanceCatalog` 对 `fire_spark` 补标准冷却、魔力与伤害倍率。`ModTechniques.java` 本单只读，`31,404` 字节，SHA256 `9c93466311db0da6703ec64a31d8d04f078b1b2229c8990b5c8f9ee787ab551f`。

## 文档回执

- `docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/visual-handoff.md`：接入回执改为本单已核对并写明 `.mcmeta` 路径。现 `13,554` 字节，SHA256 `42BE461D36BFA43588A6B533C37913E53C4DD2A01C7A46C4EBCDBA0645A9C22A`。
- `docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/README.md`：状态改为已接入、待统一编译与游戏实测，并补 `.mcmeta` 映射与回执。现 `5,954` 字节，SHA256 `5CAC1D1B49B95AF8EC2A05950C694E49E54209AFD8B9A8B848C6D93098DB2C1F`。
- `docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/manifest.json`：新增 `integrationReceipt`，并只更新上述两个文件的字节数与 SHA256。清单内其余 9 个归档文件与磁盘一致，核对 `0` 处失配。现 `7,364` 字节，SHA256 `5E6AD45A8E05808FEED01AD2CC80EC266485698F81886DF25D9FAB32A690322E`。
- `docs/资源库/README.md`：只把顶部火苗 v3.1 括号状态改为 `（已接入，待游戏实测）`。CRLF `3`、LF `154` 与改前相同，其余字节未动。现 `27,047` 字节，SHA256 `8946B14475257685E72DCAB6DF015CD82BAE83511D99FD677A60E33D2C78AE57`。

## 收束

- 编译: 待统一编译。本回执不执行 `gradlew` / `compileJava`。下一步由主会话派指定统一编译者，对含本 `.mcmeta` 的最终 `src/main` 运行 `.\gradlew.bat compileJava --offline --no-daemon`。
- 写回: 未写 `docs/术式制作表.xlsx`、`记录.md`、`实测清单.md`。编译通过后才由唯一写表者把 `fire_spark` 迁回待验收。
- 未打包，未 `runClient`，未做游戏实测。
- 当前写入已收束，文件句柄关闭。
