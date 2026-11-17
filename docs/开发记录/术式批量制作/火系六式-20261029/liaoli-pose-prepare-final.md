我先读这份燎里姿势准备任务的完整说明，再只按授权范围做。这单只归档已确认的剑阵姿态，并读出背后八剑的接入路径。先对照资源库规范和确认版预览，再看现有渲染从哪里接横置。# 燎里剑阵姿态准备

归档完成。源码没有开始改。`fire_liaoli` 仍在待验收，没有迁入制作中。本单不是正式复审。设计 2/2、实现 3/3 已用完，本阶段额度已用完，返修结果未追加复审。停止归档写入，等待下一份实施任务。

## 归档

确认版在 `docs/资源库/特效/火焰/燎里_剑阵姿态_v7/`。索引已加到 `docs/资源库/README.md`。灰蓝身体和红柄金刃只是方向占位，没有复制进 `src/main/resources`。运行时贴图仍是燎里 v6。

| 文件 | SHA256 | 说明 |
| --- | --- | --- |
| liaoli-dive-leg-array-forward.bbmodel | E55146011A0AF3C3C8AF5A696B7995FE9E5BF10196E40AB409884BBC94767CF9 | 116023 字节，前移后的俯冲模型 |
| dive-leg-forward-side.png | 6B52B4096CAC48CA68840A69599414CFF3B148880474C0498CA4C8C071341308 | 1400×900 |
| standing-side.png | B92FA4DED7DB1880314041C85B73B768CEAB67229C8A5AB900EF1E3671D79490 | 1200×1000 |
| standing-rear.png | 21DF5319647F799CEC1CDB6C991842B75236A2BB1EF66866BCB6BE4D0B686EF6 | 1200×1000 |

用户原话：`ok就用这一版特效还用现在游戏内的`。未采用 continuous 串珠修正稿，也未采用俯冲位移仍是 `[0,-14,0]` 的那一版。

模型里核对过的关键帧：`BodyPose` 原点 `[0,16,0]`，0–1 秒 X 旋转 0°，2–3 秒 -90°。`BackFan` 原点 `[0,18,5.5]`，0–1 秒位置 `[0,0,0]`，2–3 秒 `[0,-12,0]`。八个 `down_mount_sword_*` 与对应柄点重合，旋转 `[90,0,0]`。未旋转的 `sword_*` 是同一柄点上尖端更远的原始方向。以后只取「柄点加绕柄 +90° X」这一套，不要叠成十六把。3 秒时间轴不是施法时长。模型单位按玩家 1.8 格身高换算，32 单位高的占位人体是 1.8/32 格每单位，不能把 7、5.5、18、-12 直接当方块。

## 以后的最小改动

只改背后八剑的位置、朝向，以及横置时整组下移到腿部再前移。火焰、流光、背后起燃、三道后环、手持剑分流、六张 v6 贴图、时机和现有视觉设计保持。火焰锚点和剑不是同一条线段，剑转动时锚点要跟着走，否则火焰会脱离剑身。

现有布局在 `LiaoliV6Layout`。`BLOCKS_PER_PIXEL` 是 `1.8/64`，眼睛在 58 像素。`swordHilt` 用半径 23、深度 -18，`swordTip` 用半径 62、深度 -6。正深度朝前，所以现在是柄更靠后、尖更靠前，和确认版相反。扇角已是 -180、-154、-128、-102、-78、-52、-26、0。`flameStart` 固定在身后中线，`flameEnd` 用半径 118、深度 14，比剑尖更远。

`TechniqueHitRenderer` 在 visual 为 9 时调用 `LiaoliV6Renderer.render`。渲染只取命中体偏航做 `Basis.fromYaw`，`place` 把升高直接当地上的 Y，没有身体俯仰，也没有腿部偏移。环和手持剑仍走这条偏航基，这次确认范围不改它们的造型。

横置入口在 `PlayerModelActionMixin`：`HumanoidModel.setupAnim` 之后调用 `ThirdPersonPoseWriter.applyScoped`，姿态来自 `ActionRenderQueries.thirdPersonPose`。没有活动动作时返回空姿态。这套写入的是玩家模型，不是燎里命中体。Java 和语言文件里没有燎里专用的 -90° 身体剪辑。以后在命中体绘制时读取同一套身体姿态，不要把预览的 0–3 秒做成施法计时，也不要把玩家模型 mixin 改成剑阵的落点。

以后会写的文件，本单只记录哈希，没有改：

- `LiaoliV6Layout.java`：SHA256 `98558537AB9FE6F284F576EC5CB9E6FE5C1925036E9606EC7BFAE2AD275F3D6C`，2026-10-29 03:12:30.1194550
- `LiaoliV6Renderer.java`：SHA256 `5A90BCE8870F15F2446587C2E9F9689819473D13EE54B6B34F4AA49F1BC0F915`，2026-10-29 03:14:25.7414899

只读调用和姿态来源同样未改：`TechniqueHitRenderer.java` `B8C44C1CFE6C9BC36E144AB9A60785EEBAF7AAB30A2372A4362232FEE4B3F9EE`，`LiaoliEffect.java` `D60C8021134459605975438F264CAF31AB0D299F6B02E6F7CB6630A33A05CD4F`，`PlayerModelActionMixin.java` `EF26BF69FF8B502302669DD3737AD4A9CC1D8517C3ACF91EC60BEED79BB23683`，`ThirdPersonPoseWriter.java` `95D176789AB5B5237A2B2A44E2B945583C59D4DD115C88AFF5680AF593A4F3FF`，`ActionRenderQueries.java` `73CC89F7D7773410B74C0D80DAC9DE2FC9D4F55ED1D491A06CB3F4E40D72998F`。

## 占用

表作者 `d6f84d0b-4ea1-4368-a576-8acc20cc2afd` 的 `liaoli-pose-return-final.md` 写明本单停止写入。独占打开制作表失败，现表没有被覆盖。重读时燎里仍在待验收第 3 行。因此还不能按「已经迁入制作中」开始源码。

退回终稿点名的编译者 `b2d5b359-42e3-4a90-9d24-b26acec6d5f8` 只出现在角色说明里。本单没有看到该编译者针对这次姿态的停写声明，记为未确认。

## 未做

未改 Java，未跑名称门禁，未跑 Gradle，未改制作表和 `记录.md`，未进游戏，未提交。报告在 `docs/开发记录/术式批量制作/火系六式-20261029/liaoli-pose-prepare-report.md`，证据在同目录 `liaoli-pose-prepare-evidence.json`。
