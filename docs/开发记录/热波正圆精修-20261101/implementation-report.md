# 热波水平正圆 v2 接入报告

会话 `5173e9de-7c0f-4280-bad3-d5ea78293aa6`，模型 grok-4.7-high-fast。本阶段是已授权实现和作者自查。原批设计 2/2、实现 3/3 仍耗尽。这里的检查是作者工作，不计入、也不宣称为新的正式复审。

## 结论

确认版 heat-circle-v2 已按原字节进入正式归档，并替换当前运行竖条。文件名保持 `textures/effect/shock_ring_v1/shock_ring.png`，兼容 `BatchVisualTexture.SHOCK`；内容版本是 heat-circle-v2。

命中外半径和视觉 `halfSpan` 已经是同一次读取的外半径，默认 5 格。末帧可见外缘与这 5 格的差在 1 像素以内，所以没有放大载体，也没有改判定。中心命中空区仍是 1.5 格。Java 没有改动。

本阶段没有编译，没有打包，没有启动或操作游戏，没有游戏画面。

## 用户决定

2026-11-01：“按这版接入，同时把范围扩展到这个特效范围”。随后：“保留中心1.5格空区，只确保外缘对齐”。

据此保留水平世界正圆、内半径 1.5 格、外半径基准 5 格、判定总高 3 格、默认视觉 12 tick、一次结算、陨岩中心排除名单，以及既有附魔、接续、伤害、费用、锚点和配置行为。没有改成实心盘，没有按帧重复命中，没有加击退或火种消费，没有改其他术式。

制作中第 7 行 B=`fire_shock_ring` 的现行 N/O/DV 仍要求中间留空、外半径 5 格、内半径 1.5 格、总高 3 格、视觉 12 tick、判定一次。本次没有写制作表。

## 归档与运行贴图

正式目录：`docs/资源库/特效/火焰/热波_水平正圆_v2/`。

从 `docs/资源库/过程归档/热波_水平正圆_v2-20261101/` 原样复制：Aseprite 源、64×1024 竖条、单帧、二维 GIF、bbmodel、两视角 GIF、两张静帧、应用现场图。复制后与过程原件逐字节相同，哈希与 `manifest-pending.json` 一致。脚本和 bb-frames 原帧留在过程目录。

运行贴图替换前 SHA-256 `3cf9f5728c0b70eee2cc09e61b4f6182e6254516cc7b955939907755c654bbc4`，7678 字节，即旧 v1。替换后 SHA-256 `5959d47d14ea8a69394559b13f504686c620230a35505a765731cbb76bda6d0f`，10596 字节，64×1024 RGBA，与确认竖条相同。帧序仍是上小下大，16 帧。`热波_v1` 目录没有改。

游戏资源里没有放入 GIF、bbmodel、假人或地面网格。

## 代码位置

写入前定位：

- `src/main/java/com/bslx/XueXiMod/carrier/BatchShockRingEffect.java`
- `src/main/java/com/bslx/XueXiMod/carrier/BatchCarrierVisual.java`
- `src/main/java/com/bslx/XueXiMod/client/BatchCarrierVisualRenderer.java`（实际在 client 包）
- `src/main/java/com/bslx/XueXiMod/carrier/BatchVisualTexture.java`

`BatchShockRingEffect` 用 `definition.thickness()` 取得外半径，缺定义时用 `BatchCarrierIds.RING_OUTER_RADIUS`（5）。这一值既传给 `BatchVolumeQuery` 的外半径，也作为 `BatchCarrierVisual.spawn` 的 `halfSpan`。内半径来自 `birthVerticalHalf()`，默认 1.5，只参加圆环判定。高度来自 `SpecialBalance.number(fire_shock_ring, boxHeight, 3)`。寿命来自 `lifetimeTicks()`，默认 12。

`RING` 在渲染器里走水平面：`halfSpan` 同时用作 X 和 Z，整帧 UV，Y 再加 0.05。当前只有热波使用 `RING`。客户端没有另一套写死的 5 格。配置改变 `radius` 后，下一次释放的命中和显示一起变。

因此本次不改这些 Java，避免只改视觉或在客户端写死 5。

## 外缘

默认 `halfSpan` 5 格时，64 像素对应直径 10 格，1 像素 = 0.15625 格。纹理中心 (31.5,31.5) 对应载体中心。

确认版末帧设计光晕外缘 31.5 像素，映射 4.921875 格。运行竖条实测：

- 第 0 帧像素中心最大半径 8.8600 像素，映射 1.384379 格。可见边随帧外扩，前面的帧不到 5 格。
- 第 15 帧像素中心最大半径 31.4086 像素，映射 4.907593 格，比 5 格少 0.092407 格。
- 第 15 帧像素外角最大半径 32.0156 像素，映射 5.002441 格，比 5 格多 0.002441 格。
- 第 15 帧轴向包围盒在像素 1–62，外缘 31 像素，映射 4.84375 格。最外一圈像素列和行是透明留边。
- 16 帧包围盒都是正方形，且中心在 (31.5,31.5)。中心像素 alpha 为 0。
- 合成图相对镜像有 1088 个像素不同。光晕和主体在制作脚本里按对称绘制；12 条外缘火星带 0.1 弧度偏移，这些像素差来自火星。火星落在光晕外缘之内，末帧外缘仍是上面的半径。

差值来自确认版留边和离散像素。没有做 padding 补偿，没有扩大判定，没有改已确认图形。

末帧视觉内沿约 23.5 像素（约 3.67 格），那是扩散后的环带空心。玩法空区仍是命中内半径 1.5 格，不跟着每一帧的环带改。

## 已开放配置

本次没有新增或重命名键，默认行为不变。`fire_shock_ring` 已声明并在释放当次消费：

| 键 | 默认 | 单位与范围 | 生效 |
| --- | --- | --- | --- |
| `lifetimeTicks` | 12 | 刻，正刻数 | `lifetimeTicks()`，视觉寿命和空宿主寿命 |
| `radius` | 5 | 格，正尺寸 | `thickness()`，同一次读数进入命中外半径和 `halfSpan` |
| `innerRadius` | 1.5 | 格，正尺寸 | `birthVerticalHalf()`，只进入圆环判定 |
| `boxHeight` | 3 | 格，正尺寸 | `SpecialBalance.number`，判定带总高 |

读取走现有 `SpecialBalance`。键未封存或未声明时退回上表默认。没有因为这次换图改耗魔、伤害或命中次数。

## 作者检查

- 运行 PNG 与确认竖条哈希、尺寸、帧序一致。
- `SHOCK` 路径指向该文件，引用可用。
- 水平 X、Z 使用同一个 `halfSpan`，纹理中心对准载体中心。
- 同一外半径进入命中和显示。
- 内半径 1.5 格仍只用于判定；画面中心保持透明。
- 缺配置时仍回上述默认。
- 运行资源没有辅助模型。

未做：`compileJava`、游戏内观看、名称门禁（没有改 Java）。过程目录里的 Blockbench 预览是确认版原件，本次接入没有重新打开 Blockbench。
