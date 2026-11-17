# 持续面向玩家标签与留焰弹道修正

- batchId：`待制作-20261031`
- taskId：`camera_facing_tag_and_trail_fix`
- attemptId：`20261113-impl-facing-tag-02`
- stage：`implementation`
- 关联 B：`fire_flame_trail`、`rule_surface_anchor`（搁置，不加面向标签）
- 额度：设计 `2/2`、实现 `3/3`。本次返修未追加复审，不重置额度。
- 编译：未跑。待统一编译：`.\gradlew.bat compileJava --offline --no-daemon`。
- 制作表：未写。未改 `docs/术式制作表.xlsx`、`记录.md`、`实测清单.md`。
- 游戏实测：未做。

## 结果

默认不再持续面向玩家。只有登记为 `CAMERA_PLANE` 或 `UPRIGHT_CAMERA_YAW` 的特效才读取相机朝向，并保留原有旋转公式。

留焰 `BatchVisualTexture.TRAIL` 登记为 `CameraFacingTag.NONE`。采样点保存归一化后的三维弹道切向和采样序号，沿世界切向直立拼接；贴图顶部朝天空，不随相机球面旋转。正反两个 `entityCutout` 单面按侧法线分层，较新采样压在较旧采样前面。碰撞、伤害、寿命、直径、间距、上限和配置键未改。

地表定位 `SURFACE` 不加面向标签，不读相机，留在局部世界空间的双面四边形，仍用世界光照。万剑归宗只在 `uprightFace()` 为真时走 `UPRIGHT_CAMERA_YAW`；飞行姿态不变，斜贴图的 `-45` 度 Z 轴旋转仍在原位置。

## 标签

- `CAMERA_PLANE`：`SPARK`、`SEED`、`KINDLE`、`DETONATE`、`DUST`，以及 `FENTIAN_SKY_FLAME`、`FIRE_SEED_ORBIT_FLAME`、`WATER_PRISON_SHELL`。
- `UPRIGHT_CAMERA_YAW`：`SWORD_RETURN_ALL_VORTEX`。
- `NONE`：`TRAIL`、`SURFACE`、`SHOCK`、空贴图，以及未登记或 null。焚天的地面燎原贴片和 `SPREAD` 十字火舌不使用天空火云标签。

公共包 `visual/facing` 没有导入 `net.minecraft.client`。客户端旋转只在 `CameraFacingClient`。

## 命名门禁

`powershell -NoProfile -File tools/check-name-gate.ps1` 仍以退出码 1 结束。剩余 9 处都在本次未修改的文件：

- `client/NetherDomainWisps.java`：`SPACING_MIN`、`WIDTH_MIN`、`HEIGHT_MIN`
- `client/furnace/HeatUpSpatialMount.java`：`minX`、`maxX`、`minY`、`maxY`、`minZ`、`maxZ`

本次新建和修改的 Java 文件没有新增违规。原 `BatchCarrierVisualRenderer` 中的 `dx`、`dy`、`dz`、`up` 和 `SPARK_ROLL_MIN_PROJECTION` 已随着火苗滚转公式搬到 `CameraFacingClient` 后消失。这 9 处旧违规不在本次写入范围，没有顺手修改。

## 文件

时间为东八区本地时间。回执文件自身的哈希在写入后单独计算，不写回本文，避免改变哈希。

| 路径 | 字节 | mtime | SHA256 |
| --- | ---: | --- | --- |
| `D:/alianximod/src/main/java/com/bslx/XueXiMod/visual/facing/CameraFacingTag.java` | 462 | 2026-11-13 03:18:48.399 +08:00 | `BB3D55BC421B692DC8C014C2E1C0661AA55D97CFD105D561E03E99D11C1DAB4E` |
| `D:/alianximod/src/main/java/com/bslx/XueXiMod/visual/facing/CameraFacingEffectId.java` | 537 | 2026-11-13 03:18:56.014 +08:00 | `EB870665235054602246ED777D7A52F4D20B10BD2C9B5A59DED6711CC37B349D` |
| `D:/alianximod/src/main/java/com/bslx/XueXiMod/visual/facing/CameraFacingRegistry.java` | 1068 | 2026-11-13 03:19:03.871 +08:00 | `7B0BFE01EA01320C60C77A713CC4FAB09604A7C23441FE6EB523E1D1C490A83E` |
| `D:/alianximod/src/main/java/com/bslx/XueXiMod/client/CameraFacingClient.java` | 5690 | 2026-11-13 03:23:45.163 +08:00 | `C9F441D2A88C8752A2BE0208A9655D7B6271BEF99256DBAB0D10413023D4D1D8` |
| `D:/alianximod/src/main/java/com/bslx/XueXiMod/carrier/BatchCarrierVisual.java` | 18821 | 2026-11-13 03:19:33.403 +08:00 | `F70A0D38EE8FB10D3F3D1EAECE54235DBF8FCE195DF29C4691940E623CE15FFF` |
| `D:/alianximod/src/main/java/com/bslx/XueXiMod/carrier/BatchFlameTrail.java` | 9765 | 2026-11-13 03:19:41.170 +08:00 | `34C0A3252FAED95FBDC3ED9127CA108D962C1A652AFB4CC52055981F33B8AAE2` |
| `D:/alianximod/src/main/java/com/bslx/XueXiMod/client/BatchCarrierVisualRenderer.java` | 19502 | 2026-11-13 03:20:05.058 +08:00 | `4AC10F6AD5955C1DFB42AC4CB9825E67CA724C1FF4CCE325EEC1A285E189252B` |
| `D:/alianximod/src/main/java/com/bslx/XueXiMod/client/FireVisualClient.java` | 43052 | 2026-11-13 03:21:10.044 +08:00 | `49468273CF65202DC2E0F7C8030DCB4430532C9B1136386F6AD54A1422E26AAF` |
| `D:/alianximod/src/main/java/com/bslx/XueXiMod/client/FireSeedFlameClient.java` | 10320 | 2026-11-13 03:21:10.056 +08:00 | `F1BF54647425839B2B6FC2719AD084D4026103D4C86D6238ACD1321B4A969CE1` |
| `D:/alianximod/src/main/java/com/bslx/XueXiMod/client/PrisonShellRenderer.java` | 14272 | 2026-11-13 03:23:45.170 +08:00 | `2BE12460D2ABF72A3D82F9337FC8C710433E5A85E74638178B4613728045FB4E` |
| `D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/sword/SwordEntity.java` | 33178 | 2026-11-13 03:20:40.355 +08:00 | `04914D96533237D8B0DA9D762D55ACE29BB45C639487E437A723651729BBE5E3` |
| `D:/alianximod/src/main/java/com/bslx/XueXiMod/client/SwordEntityRenderer.java` | 4253 | 2026-11-13 03:21:09.577 +08:00 | `2558BC186875E16A0FE6A99AAA24E97AEDFF34F7E6C0CD07207CCFF2B7981F3E` |
| `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/README.md` | 4177 | 2026-11-13 03:23:45.177 +08:00 | `808265B801BD3E7CAF85AAF0FED99AA0ACB66843153DCB32E408BB7F5F528579` |
| `D:/alianximod/docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/manifest.json` | 7055 | 2026-11-13 03:25:10.726 +08:00 | `BFA1BFDA3D38C0CE4CA0633C8695D42BD7817C365BDB4F92801467FEC69EE407` |

## 收束

上述文件已写完并关闭。没有其他作者持有这些路径。统一编译可以接着覆盖这些源码。
