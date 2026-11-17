# 引爆 v2-3D 确认版接入回执

- batchId：`待制作-20261031`
- taskId：`fire_detonate_v2_integration`
- attemptId：`20261116-impl-detonate-3d-01`
- 关联 B：`fire_detonate`
- 直接父会话：`01a1ce06-b681-7c90-b11d-260d3c169911`
- 绑定版本：`fire_detonate_v2_3d_candidate_20261116`
- 用户原话：「可以先采用这一版」
- 本阶段额度已用完，返修结果未追加复审。设计 2/2、实现 3/3 未重置。本轮是确认版接入，没有新增裁决或正式复审。

## 状态

功能逻辑未改。确认版视觉已接到现行结算结果，运行贴图已替换 v1 引用。状态是「已接入，待统一编译」。没有执行 `compileJava`，没有启动游戏，不能写成编译通过或游戏实测通过，也不能据此迁待验收。

## 真实消费如何到达视觉

`DetonatePlan.strike` 仍先做原有的就绪、配置结算、同目标去重和余额判断。只有 `FireSeedState.draw` 已经返回后，才把这个 `spent` 交给 `BatchTriggerVisuals.detonate`。

- `NOT_READY`、`DUPLICATE`、`EMPTY` 不进入播放。
- `spent <= 0` 直接返回，不会因为载荷里有引爆标记而补播。
- `STRUCK` 和 `SPENT_UNSTRUCK` 都使用已经记下的同一 `spent`。视觉不再读余额，也不再调用 `draw`。
- 锚点是结算当时的 `target.position()`。不再加旧的 Y+1，也不做相机推移。
- 服务端只生成一个 `BatchCarrierVisual`，时长 20 tick，宿主编号仍是 -1。26 团只在客户端按确认参数展开，不是 26 个实体、26 次结算或 26 份伤害。

`BatchTriggerVisuals.onAccepted` 不再因 `load.detonate()` 播放旧单张。种火、地表和土尘路径不变。

## 视觉参数

参数来自确认版 `visual-handoff.md`、`particle-seeds.json` 和 `scripts/build_scene.js`。24 个外扩团加 2 个中心团；方向、错时、寿命、行程、面宽和 roll 与种子逐项一致。

- 位移：`travel * (1 - exp(-0.32 * age)) * scale / 16` 方块，另加 `age * 0.055 * scale / 16` 方块上浮。
- atlas：32×512、16 帧，`floor(clamp(age / (lifetime - 1), 0, 1) * 15)`。未到龄或寿命结束不画。
- 每团用相机四元数乘自身 Z roll。世界偏移不随相机整组旋转。
- 可见团按相对相机的深度从远到近提交。
- 强度：`S1–10 = 0.40 + 0.025S`，`S10–50 = 0.65 + 0.00875(S-10)`，`S50–100 = 1 + 0.007(S-50)`。S10/50/100 分别回到 0.65/1.00/1.35。
- 未展示的 `S>100`：视觉固定 S100 的 1.35。这个上限只乘视觉位移和面宽，不限制火种消费、伤害、余额或目标数量。

离线复核了种子公式。S50 第 7 帧中心跨度为 X1.0702、Y1.1559、Z1.2063 方块，第 13 帧为 1.3012、1.5723、1.6028 方块，与交接一致。这是粒子中心跨度，不是伤害半径。

## 朝向、蓝焰与遮挡

`CameraFacingRegistry` 中的 `DETONATE` 已从整张 `CAMERA_PLANE` 改为 `NONE`。种火、引燃、扬尘仍是相机平面；火苗、留焰、地表、冲击波和其他特效朝向不变。引爆各团在渲染器内单独面向相机。

普通橙火引用 `explosion_puff_orange.png`。幽冥体积内只换 `explosion_puff_soul.png`，几何、节奏和 scale 不变。顶点保持白色，不把旧幽冥蓝再乘一次。

材质使用实体发光半透明 shader、`NO_CULL`、`LEQUAL_DEPTH_TEST`、`COLOR_WRITE` 和 `FULL_BRIGHT`。贴图 `blur=false`、`clamp=true`，alpha 保持确认版硬边。深度测试保留，因此第一人称和实体前后遮挡仍生效；云团之间不写深度，避免互相挖空。

## 未改的玩法与配置

没有改 `DetonatePlan.damage`、`ResourceEnchantNumbers.DETONATE_FACTOR`、火抗、焚天、引燃、余额争用或同目标去重。伤害仍是 `0.2 × 当时余额 × (1 + 当时炉心热量)`，并继续走原配置结算。没有新增配置键，也没有把视觉比例声明成平衡字段。已有覆盖值不需要迁移。

视觉比例由客户端读取已同步的 `halfSpan`。它不是碰撞箱、索敌范围或伤害半径；服务端命中体积没有读取这个值。

## 资源映射

- 正式橙条：`docs/资源库/特效/火焰/引爆_三维扩散_v2/textures/explosion_puff_orange-strip.png`
- 运行橙条：`src/main/resources/assets/xueximod/textures/effect/detonate_v2_3d/explosion_puff_orange.png`
- 两者 SHA256：`28666B39C8A80E0A18EB04DDAE7F7422A546B179307DB0990F7F8109ADF8D0F7`，2179 字节，32×512。
- 正式蓝条：`docs/资源库/特效/火焰/引爆_三维扩散_v2/textures/explosion_puff_soul-strip.png`
- 运行蓝条：`src/main/resources/assets/xueximod/textures/effect/detonate_v2_3d/explosion_puff_soul.png`
- 两者 SHA256：`C4B656B506B6CB721BCEF39CFED9BA99AA0AE21F2EDED7839057FC3573835856`，2193 字节，32×512。
- 两份 mcmeta SHA256：`6C5F3AB6BDD0921FB58CC5837E09DADE2660606111271B736860C2FEA01E1651`。
- 旧运行资源保留：`textures/effect/detonate_v1/detonate.png`，SHA256 `F3F0FCEB4A8AAECBA5EAC2B300C1FD5036B0E9EEAF1CD04B1A4F38FC1C79D858`。Java 与资源引用中已无 `detonate_v1`。

`.bbmodel`、假人、地面和预览 GIF 留在正式归档，不进运行资源。

## 检查

实际执行 `powershell -NoProfile -File tools/check-name-gate.ps1`。退出码 1。290 处均在既有无关文件；本次改动文件过滤后为零。只把本次渲染中的过短变量 `up` 改成 `upAxis`，没有改既有命名问题。

种子、方向、S10/50/100 和 S50 两帧中心跨度由只读 Python 对照确认版种子复核，结果一致。这不是项目测试阶段，也不代替 Gradle。

## 写入与收束

本任务独占写入均已落盘，无后台进程，无下属代理。未改 xlsx、共享记录、资源库总 README、正式归档或美术源。

Java 改动 SHA256：

- `DetonateCloudLayout.java`：`8C4BAFEBD291B9B8A57DE8888C43A14E66F938B6BD3D403F54A646DCBA742D68`
- `BatchTriggerVisuals.java`：`97A6B5B7D43965A4C3CB2E2CB3397EA1BFA2CFA3921B9DC4A25B3BAE4B5E1F81`
- `BatchVisualTexture.java`：`83CA33CDAFA36805955D60124B39FACA31AD2203D43E88CD36B9F3275C792AA8`
- `DetonateCloudMesh.java`：`3BA9C02A9B7A51DC278086BBA7DD4C30902BA5DC37B43755B976B7F8DC9543E1`
- `BatchCarrierVisualRenderer.java`：`A587B2F9F255C16A0F3705EFB1ACD9F53D419625A51FF01D767674C7E3AA3FC9`
- `CameraFacingClient.java`：`947DD3C45D02EAFE399335FBE2821434E24D4CE36E9575474ABAE47E1589E84B`
- `CameraFacingRegistry.java`：`2325BDB82679CA7783CEBC184FC90F4C7291E30BCFA4D67BC267D8D3FA3E883D`
- `DetonatePlan.java`：`8B170D851511FC3919DFC9DE8A2859CC118AE7C6D7F9659BC9B17CBE78B3ABE8`

## 未跑项

未跑 `compileJava --offline --no-daemon`，未打包，未 `runClient`，未启动游戏。Forge 透明排序、第一人称、实体遮挡、网络同步和实际时序仍待编译后由用户实测。
