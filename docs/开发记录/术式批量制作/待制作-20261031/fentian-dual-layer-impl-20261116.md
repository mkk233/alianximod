# 焚天双层视觉实现回执

- batchId：`待制作-20261031`
- taskId：`fire_kindle_fentian_visual_swap`
- attemptId：`20261116-impl-fentian-dual-layer-02`
- 角色：唯一 Java / 运行资源作者
- 直接父级：主会话 `01a1ce06-b681-7c90-b11d-260d3c169911`
- 模型：`cursor/grok-4.7@500k`，high。工具无 tier 参数，沿用客户端 Fast `service_tier="priority"`，未声称后端独立验证。
- 本阶段额度已用完，返修结果未追加复审。

## 完成

焚天同一 `SKY` 轨道现在同时画旧 52 帧背景和新 19 帧全身烈焰前景。引燃现有持续轨道、贴图、几何和收束全部保留，等待用户另行设计。没有改伤害、燃料、释放、叠层、规则、权限、账本或相机标签声明。

背景先进入 `SKY_BACKDROP` 的缓冲，前景随后进入 `BODY_FLAME` 的缓冲。二者是不同 `RenderType`，渲染末尾按首次入列顺序 `endBatch`，因此旧背景实际先提交，新前景后提交。没有关闭深度，也没有移动已批准锚点。

## 帧与寿命

- 两层共用原焚天 `SKY` 的真实 4 tick 段、游标、等待和寿命。没有新建燃烧轨道，也没有用 GIF 时长代替燃烧期限。
- 旧背景存活期循环 1–40 帧，不因段游标前进改播短亮；自然结束才单次播放 41–52 余烬，播完或目标消失后删除。非自然结束不留余烬。
- 新前景仍循环 0–9；真实段游标前进时覆盖 10–12 三帧，再回持续。自然结束先播 10–12，再单次播 13–18，18 全透明后删除。非自然结束两层都立即清除。
- 背景余烬最长 12 tick，前景终止序列最长 9 tick。各自完成后只删自己的纯视觉状态。目标缺失、登出或客户端维度卸载都会清空，不留漂浮片。

## 几何与深度

- 背景：高 `max(1.8, bbHeight)*1.35*skyScale`，宽为高的 `128/144`，中心 `entityY+bbHeight*0.45`。`skyScale` 只按原焚天轨道末段与余量变化。
- 前景：宽 `0.75H`、高 `H`。中心从包围盒中心沿朝镜头法线偏移 `n*(abs(nx)hx+abs(ny)hy+abs(nz)hz+0.04)`。
- 两层都经现有 `CameraFacingClient.orientDeclaredPlane(FENTIAN_SKY_FLAME)`，保持 `CAMERA_PLANE`、`FULL_BRIGHT` 和现有 `NetherDomainTint`。
- 实际深度语义未变：`RenderType.entityTranslucentEmissive`，半透明、`NO_CULL`、只写颜色不写深度、默认 `LEQUAL` 深度测试。背景中心面因此仍可能被身体或地面遮挡；前景只靠既有朝镜头偏移避开身体。没有按 Blockbench 默认深度移动锚点。

## 文件

- `src/main/java/com/bslx/XueXiMod/client/FireVisualClient.java`：2026-11-16 11:49:22.390，52998 字节，`EEE7620485A157F67FAB14C3B55E84C7BEF60067168AA2739D38111633E33A2E`
- `src/main/java/com/bslx/XueXiMod/visual/fire/FireVisualLayout.java`：2026-11-16 11:48:15.569，10934 字节，`9C57329F4057CE564DC3D2B8E1856559B4F4F33CAF9FAEFAE46945E892678284`
- 背景运行贴图 `textures/effect/fentian_v2/conflagration.png`：2026-10-29 03:09:11.074，83272 字节，`3AE6A95E41D92991B828EDC7491CBB40A18D503CDCF700281A50D901D6AA6A76`
- 前景运行贴图 `textures/effect/fentian_v3/conflagration.png`：2026-11-16 03:27:03.728，12139 字节，`EF3476EB01225D2E7478F9BBA5430F28BCF2E1D12198F3F482A634C155A04070`
- 前景 `.mcmeta`：2026-11-16 11:06:45.707，66 字节，`2352EF117D2BF2806A2D63325F911C65CE24C42BD58F1D2520912C0B7547ABA7`，`blur=false`、`clamp=true`。背景目录仍只有 PNG，没有新增 `.mcmeta`。
- 两张运行 PNG 分别与 `docs/资源库/特效/火焰/焚天_v2/fentian-conflagration-v2-strip.png`、`docs/资源库/特效/火焰/焚天_全身烈焰_v3/fentian_v3-strip.png` 逐字节相同。没有使用 Blockbench 内嵌重编码图，也没有改像素。

引燃 `kindle_v4/kindle.png` 仍为 `3AE6A95E41D92991B828EDC7491CBB40A18D503CDCF700281A50D901D6AA6A76`，其 `.mcmeta` 哈希同上。`FireVisualRelay.java` 仍为 2026-11-16 11:04:00.239、`94B0A9F00D966616CD32DC9CB85E8DF47356897599DAD2DA4702D66B0A1C7E52`。`BatchTriggerVisuals.java` 位于 `carrier` 包，未改。

## 调用映射

- `FireVisualRelay` 仍只同步原 `SKY` 与 `KINDLE` 轨道；本轮没有改它。
- `drawSky`：`skySustainFrame` → `paintSky(fentian_v2)`，随后 `bodyLoopFrame/bodyPulseFrame` → `paintBody(fentian_v3)`。
- 自然 `BURN_END`：`SKY_EMBERS` 走 `skyEmberFrame` 41–52；`BODY_PULSES` 与 `BODY_ENDS` 仍走 10–12、13–18。
- `drawKindle`、`drawKindleEmbers` 与 `kindle_v4` 路径未改。

## 检查与未做

- 已跑 `powershell -NoProfile -File tools/check-name-gate.ps1`。本次两个文件没有新增命名问题；全仓仍有既有 295 处无关命名问题，未修。
- 未跑 Gradle，也未跑 `FireVisualLayoutCheck`。状态为待统一编译。
- 未改测试、伤害实现、燃烧账本、制作表、正式美术目录、资源库索引或批次总记录。
- 未 build、jar、reobf、runClient，也未启动游戏。
- 只记录不修：`FireConsumerPlans` 当前 `skyDemand=ceil(S/R)`、`skyDamage=3+consumed*0.5*(1.2^(S/10)+F)`，与旧表 EK3 的每段 10 和 `2*(1.2^n+F)` 不同。

## 收束

源码写权已收束。最后写入是 2026-11-16 11:49:22.390 的 `FireVisualClient.java`。没有下属，没有待关闭的 Flash。下一步由父方统一协调编译；本回执不覆盖此前 11:23 后出现的无关 `CombatOwnership.java` 写入。
