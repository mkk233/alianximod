# 幽冥摇曳确认版接入回执

2026-11-16。batchId `待制作-20261031`；taskId `fire_nether_gloom_v2_domain_preview`；attemptId `20261116-impl-gloom-sway-01`。关联 B：`fire_nether_gloom`。

本阶段额度已用完，返修结果未追加复审。设计 2/2、实现 3/3。未派裁决、复审或其他执行者。无子代理，无待关闭子代理。

用户确认原话：「按这版接入」。对象是已展示的 sway04：`close-sway-3p2s.gif`、`inside-sway-3p2s.gif`，以及同版魂谷天空/远景雾。未重画，未改美术源参数。

## 已写入并收束

只写下列文件。未写制作表、共享记录、资源库 README、正式美术归档、实测清单和服务端玩法。

- `src/main/resources/assets/xueximod/textures/effect/nether_gloom_v2/nether_gloom.png`
  - 5310 字节，SHA256 `7699297D207AC3CCACEC25E9CF7226C5B3765C0E16F620F3E1D717C1E23AD0FB`。
  - 与正式归档 `docs/资源库/特效/火焰/幽冥_魂谷摇曳_v2/textures/youming_v2_sway_strip.png` 逐字节相同，也与收束时的过程目录同名 PNG 相同。
  - 路径仍是 `NetherDomainWisps.SHEET`，第 34 行，没有改资源名。
- `src/main/resources/assets/xueximod/textures/effect/nether_gloom_v2/nether_gloom.png.mcmeta`
  - 61 字节，SHA256 `A2313FCC609089214681B434F5959499F068679AA7DFEBDF5B3F0E7551ACC2F9`。`blur=false`，`clamp=true`。
- `src/main/java/com/bslx/XueXiMod/client/NetherDomainWisps.java`
  - 15383 字节，SHA256 `9DF4B74F3440B42254A6534CE388592EEFA5BF77148EBB6D5B6C0A65B57F0E54`。
  - 去掉整簇宽高脉冲。固定宽 `0.6486–0.9964`、高 `0.58–1.2296`，根部 `minY+0.15`，固定 yaw，双面交叉，顶点白色 alpha 210，全亮。
  - 帧序仍是 2 tick 一帧，簇间相位 0/4/8/12。`cross` 在第 266 行，不做整体 rotation/position/scale，也不追镜头。
  - 铺点列数、间距、抖动和 256 簇上限没改。49 簇只是预览样本。
  - `embers` 在第 204 行：每个真实切片 8 枚纯客户端余烬。宽 `0.028–0.048`、高 `0.042–0.072`，升高约 `0.71–1.65`，横向不超过 `0.041`，1.6 秒正弦错相。颜色用确认图热核 `#CCFAF5`，取贴图左上角透明像素，不拉伸火舌。
- `src/main/java/com/bslx/XueXiMod/client/NetherDomainAtmosphere.java`
  - 新文件，5910 字节，SHA256 `910A91A7C80CAEB7E93773A3554EB5FA0F4FD21B813138FC25BBAC368C4FBA6B`。
  - `Dist.CLIENT` 自动订阅。`onClientTick` 第 48 行，`onFogColor` 第 75 行，`sky` 第 90 行，`clear` 第 146 行。
  - 只问当前主摄像机。判定走 `NetherDomainSight.probe`，其内部是逐片 `[min,max)`，不用外包盒。
  - 进入 0.6 秒、离开 0.8 秒，先线性插值再 smoothstep。离开、到期、换维、退出世界后权重回零，不把原天空写成蓝天。
  - 水、熔岩、粉雪摄像机介质不混色。不改群系，不生成天空壳或远景方块，离开不关闭领域地火。
- `src/main/java/com/bslx/XueXiMod/mixin/client/ClientLevelGloomSkyMixin.java`
  - 新文件，883 字节，SHA256 `3B32271479CAE7FCAD779DA119B0C2B5FC37C982619D0DC4071641BEFA779B94`。
  - 第 15 行注入 `ClientLevel.getSkyColor` 返回值。权重为零时原样返回。
- `src/main/resources/xueximod.mixins.json`
  - 2578 字节，SHA256 `11906A0CFB81886DE4560FACECA0654AD482692A7FDE127F2C585EDF276D8BDD`。
  - 只在 client 数组第 77 行加 `client.ClientLevelGloomSkyMixin`。专用服务端不加载这条。
- 本回执。

共享注册类 `XueXiMod` 未改。旧批次所有权记录都是已停写的历史快照，没有声明正在写这些文件。另一个主聊天 `01a1e39f-f155-7e73-b171-59bd929e26f8` 在父方最后通知时仍为 active；本回执不把工作区说成无人在写。

## 资源映射

运行贴图对正式归档源 `textures/youming_v2_sway_strip.png`。64×1024，16 帧。正式 manifest 的 `plannedRuntime.sourceSha256` 就是上面的 `FBBD0F90…`。

派发说明曾写源哈希 `7699297D207AC3CCACEC25E9CF7226C5B3765C0E16F620F3E1D717C1E23AD0FB`。本轮开工时过程目录的 `Get-FileHash` 也曾返回该值；收束时过程目录、正式归档和运行贴图已经逐字节一致，并且 .NET `ReadAllBytes` 的 SHA256 是 `FBBD0F90…`。接入以当前磁盘和正式 manifest 为准，没有回写归档。

Aseprite 源哈希仍是回执中的 `9D7D6867771F216A3CA7552844FB66D15D764DF9A3FAE4737114FF39C5000032`。bbmodel 仍是 `350EF6D81BC057E9C577B09F04F42225009DF19EA2B59CE0C4AD4B52DD7309C9`。这两份不进运行资源。

## 天空与雾

确认色：天空顶部 `#081923`，高空 `#102C3B`，地平线 `#416E77`。

原版 1.20.1 天空穹顶只有一个颜色。这里把整个天空色朝顶部色混合，雾色按仰角从地平线色过到高空色。没有另画天空壳。雾的近远平面仍用当时原版结果，不把预览代理的距离当成雾密度；近处仍走原来的雾起点。

本地依赖证据：`FogRenderer.setupColor` 先算出当前雾色，再交给 `ViewportEvent.ComputeFogColor`；天空穹顶读 `ClientLevel.getSkyColor`。两处都是在当前值上混合，权重为零时不改。水/熔岩/粉雪走 `Camera.getFluidInCamera`，不进这次混合。

## 未做

- 未编译。本轮没有编译权限，没有跑 `gradlew`。没有打包，没有启动游戏，没有游戏内验证。
- 产种、伤害、范围、切片和周期没改。历史周期 5 与批准 20 的差异仍未决，不属本次。
- 地火到 `visualEnd` 仍立刻停画，没有火焰淡出。天空和雾有 0.8 秒退出混合。
- 正式索引里的 `plannedRuntime` 仍标接入中，由记录者更新。

写入已收束。
