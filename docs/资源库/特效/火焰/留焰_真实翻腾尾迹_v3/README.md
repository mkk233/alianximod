# 留焰 · 真实翻腾尾迹 v3

2026-11-13 用户确认原话：「接入这版」。状态：**v3 已正式归档并接入运行资源，最终编译通过，待游戏实测**。统一 `compileJava` 于 2026-11-13 01:58:53.725 +08:00 至 2026-11-13 01:59:19.149 +08:00，退出码 0。旧目录 [留焰_v1](../留焰_v1/README.md) 与过程稿 [留焰_v3_真实留焰翻腾尾迹_20261113](../../../过程归档/留焰_v3_真实留焰翻腾尾迹_20261113/visual-handoff.md) 均保留，未覆盖。

沿移动载体留下直立火径。采样点生成后停在世界坐标，各自翻腾、升腾、拉断并化作火星后熄灭。不产种，不放原版火。预览弹速只作演示，示意辅助不进游戏。

## 版本与文件

- `flame_trail_v3.aseprite`：64×64、16 帧可编辑像素源。SHA256 `63E13A9356846D981AB111DFE2A774ED50827E741EB8654E9385293E77DDFD9E`。
- `flame-trail-v3-scene.aseprite`：592×160、62 帧完整组合场景源。SHA256 `21EFDFE97C317CF36DB57164A8BE0874434FBE64426FABB96FB40A591B04EC49`。
- `flame_trail_v3_strip.png`：64×1024，16 帧竖条，自上而下每帧 64×64。SHA256 `50946B75FBBEFDA2F4A34FC337FD8A665A18F0428638F574CB2880CA88B06D1D`。
- `flame-trail-v3-complete-effect.gif`：二维完整组合动态预览。SHA256 `C1ACF6A69E6DC464C158F1A7EFD96942A3C54A5C9E51F9AE9F4CDC773110BF44`。
- `flame_trail_v3_single.gif`、`flame_trail_v3_grid_4x4.png`：单贴图动画与 16 帧网格。
- `flame-trail-v3-2d-stage0-launch-t03.png` 至 `flame-trail-v3-2d-stage5-fadeout-t48.png`：六张二维阶段图。
- `flame-trail-v3-projectile-composite.bbmodel`：同版真实 Blockbench 工程。SHA256 `365064F04B05A9816CDCE2E37B77B09F2944A34A4EF31D82B968C9F2701CD85F`。
- `flame-trail-v3-3d-hero-closeup.png`、`flame-trail-v3-3d-side-view.png` 及五张 `flame-trail-v3-3d-stage*.png`：同版实际视口截图。
- `manifest.json`：本目录除清单自身外各文件的相对名、绝对路径、字节数与 SHA256。素材从过程稿逐字节复制后哈希一致。

## 视觉交接

Blockbench 适用。用户已看到同版本二维完整组合预览和真实 Blockbench 三维预览，并明确回复「接入这版」。

换算：16 模型单位 = 1 格。留焰采样面为 `0.5×0.5` 格，`halfSpan = 0.25`。运行朝向为 `CameraFacingTag.NONE`，沿世界三维弹道切向直立排布，不随相机球面旋转（与 `.bbmodel` 三维示意一致）。采样间距 `0.25` 格，上限 `256` 点。单点寿命 `40 tick`。帧公式 `frame = floor(age * 16 / 40)`，纹理 `topV = frame/16` 至 `(frame+1)/16`。材质 `entityCutout`，`FULL_BRIGHT`，顶点 alpha `255`。

预览弹速 `0.625` 格/tick 只用于在同一画幅演示疾掠、留焰、拉断和熄灭，不是游戏弹速，也不改配置。地面、一格刻度、施法假人、载体火苗示意和 helper 参照只在预览里，不进入留焰运行资源。

## 运行接入（2026-11-13）

运行贴图：`assets/xueximod/textures/effect/flame_trail_v3/flame_trail.png`。它由 `flame_trail_v3_strip.png` 逐字节复制，尺寸 `64×1024` RGBA，SHA256 与确认竖条相同。旁有 `flame_trail.png.mcmeta`，`blur=false`、`clamp=true`。`BatchVisualTexture.TRAIL` 指向该路径。旧运行贴图 `textures/effect/flame_trail_v1/flame_trail.png` 保留。

玩法、伤害、寿命、尺寸、采样间距和配置底座未改。统一 `.\gradlew.bat compileJava --offline --no-daemon` 于 2026-11-13 01:58:53.725 +08:00 至 2026-11-13 01:59:19.149 +08:00 执行，退出码 0。游戏内名称是「留焰」，描述是「沿载体留下不产种的火焰轨迹」。

## 运行朝向修正（2026-11-13）

运行朝向为 `CameraFacingTag.NONE`。采样面沿世界三维弹道切向直立排布，贴图顶部朝天空方向自然升腾，不随相机球面旋转，与 `.bbmodel` 三维示意一致。

采样面仍是全宽 `0.5` 格、`halfSpan = 0.25`，采样步长 `0.25` 格。正反两个单面沿侧法线做微米级分层，较新采样压在较旧采样前面，避免斜向弹道出现水平锯齿或顺视线叠成深色横杠。本修正待统一编译，尚未重新进入游戏实测。
