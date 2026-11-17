# 引燃与焚天特效互换实现回执

- 身份：batchId `待制作-20261031`，taskId `fire_kindle_fentian_visual_swap`，attemptId `20261116-impl-kindle-fentian-swap-01`。
- 角色：本任务唯一 Java 与运行资源作者。直接父级是当前主会话；父级 UUID 未提供，不编造。
- 工具没有 tier 参数，沿用客户端已配置的 Fast（`service_tier="priority"`），不声称后端已独立验证。
- 设计 2/2、实现 3/3 已用完。本阶段额度已用完，返修结果未追加复审。
- 本任务没有派下属。父级前置 Flash `01a1e237-8bcf-7a30-b6dd-16ed64dde350` 已由父级收束，本作者未持有也未再派。

## 调用映射

- 引燃 `fire_kindle`：`FireVisualRelay` 按现有 `BurnLedger` 查 `ResourceEffectKeys.KINDLE`（`fire_kindle`），用既有燃烧包同步为 `FireVisualLayout.KINDLE=3`。登录、开始追踪、死亡或卸载中止都走同一条路径。
- 客户端 `FireVisualClient.drawKindle` 用轨道自己的间隔、段游标和剩余等待计算进度，循环原焚天 52 帧中的 1–40 帧；自然结束后单次播 41–52 余烬。非自然中止不留余烬。
- 几何保留原焚天面：高 `max(1.8, bbHeight)*1.35*skyScale`，宽为高的 `128/144`，中心为 `entityY+bbHeight*0.45`。`skyScale` 仍只按该轨道自身的末段与余量缩小，不引入焚天的 4 tick 扣款。
- 朝向仍走 `CameraFacingClient.orientDeclaredPlane(FENTIAN_SKY_FLAME)`。没有新增相机标签。
- 焚天 `xueximod:fire_fentian`：仍由原 `SKY` 轨道同步。持续阶段循环新图 0–9 帧；段游标真实前进时播 10–12 帧短亮，再回持续。自然结束先播这三帧，随后单次播 13–18 帧，18 为全透明。不循环全部 19 帧。
- 焚天面宽 `0.75H`、高 `H`，H 是目标包围盒高。中心从包围盒中心沿朝镜头法线偏移 `n*(abs(nx)hx+abs(ny)hy+abs(nz)hz+0.04)`。
- 旧 `BatchTriggerVisuals` 的引燃一次性 16 帧闪片已停止生成，不再和持续火焰叠加。种火、引爆、地表和土尘不变。

## 运行深度

两张面仍用现有 `RenderType.entityTranslucentEmissive`。它是半透明、双面（`NO_CULL`）、只写颜色不写深度，深度测试保持构造默认的 `LEQUAL`。顶点仍是 `FULL_BRIGHT`，颜色仍乘现有 `NetherDomainTint`。没有关闭深度或移动批准锚点。因此引燃中心面仍可被身体或地面遮挡；焚天只靠已批准的朝镜头偏移避开身体。

## 文件与哈希

- `src/main/java/com/bslx/XueXiMod/visual/fire/FireVisualLayout.java`：2026-11-16 11:12:16，10415 字节，SHA256 `360B909B1009665936F06DBB724DF0AE0DD159C0630526C94F8E5C9AF3096839`。
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/FireVisualRelay.java`：2026-11-16 11:04:00，12293 字节，SHA256 `94B0A9F00D966616CD32DC9CB85E8DF47356897599DAD2DA4702D66B0A1C7E52`。
- `src/main/java/com/bslx/XueXiMod/client/FireVisualClient.java`：2026-11-16 11:16:46，50034 字节，SHA256 `76DF4FD23540F94302665517A999FFB014182F838EDDD0C392BB43D3472CD397`。
- `src/main/java/com/bslx/XueXiMod/carrier/BatchTriggerVisuals.java`：2026-11-16 11:12:16，4240 字节，SHA256 `7EB550F2A204B8D8E8C664B2F0A2224153FD738393F2EE63ED5E2CB0840F009D`。
- `src/test/java/com/bslx/XueXiMod/visual/fire/FireVisualLayoutCheck.java`：2026-11-16 11:15:38，13568 字节，SHA256 `1FE9D5D10EB62D8D7AD318123E31ED373CBAAA16397772E430B6DD1D12BD795B`。只补了既有离线取样检查的互换帧序，不是新的测试阶段。
- `src/main/resources/assets/xueximod/textures/effect/kindle_v4/kindle.png`：83272 字节，128×7488，SHA256 `3AE6A95E41D92991B828EDC7491CBB40A18D503CDCF700281A50D901D6AA6A76`。与正式源 `docs/资源库/特效/火焰/引燃_承接焚天_v4/kindle_v4-strip.png` 逐字节相同。
- `src/main/resources/assets/xueximod/textures/effect/fentian_v3/conflagration.png`：12139 字节，48×1216，SHA256 `EF3476EB01225D2E7478F9BBA5430F28BCF2E1D12198F3F482A634C155A04070`。与正式源 `docs/资源库/特效/火焰/焚天_全身烈焰_v3/fentian_v3-strip.png` 逐字节相同。
- 两份 `.mcmeta` 均为 `blur=false`、`clamp=true`，SHA256 `2352EF117D2BF2806A2D63325F911C65CE24C42BD58F1D2520912C0B7547ABA7`。
- 没有改 PNG 像素，没有使用 Blockbench 内嵌重编码图。

## 检查

- 已用 JDK 17 单独编译并运行 `FireVisualLayoutCheck`。互换帧序通过。仍有两条既有燎原断言失败：当前 `GroundFireFields` 半径是 10、地面伤害是 4，且不再含检查所期待的 `StandSurface.columnTop`。这不属于本次视觉互换，未改。
- `tools/check-name-gate.ps1` 已跑。本次动过的四个源文件和检查文件没有命名问题；全仓仍因既有无关文件报 295 处，未改那些文件。
- 未跑 `gradlew compileJava`。状态是待统一编译。没有 build、jar、reobf、发布或启动游戏。
- 本地检查产生的 `build/kindle-fentian-check` 类文件目录仍在；递归删除被策略拦下，没有改源码。

## 没做

- 没改伤害、燃料、释放、叠层、规则、权限、相机标签声明或其他术式。
- 没改 `FireConsumerPlans`、`KindlePlan`、`BurnDispatch` 或燃烧账本。当前焚天 `skyDemand=ceil(S/R)`、`skyDamage=3+consumed*0.5*(1.2^(S/10)+F)` 与旧表 EK3 的差异只记录，不回滚、不调整、也不据此迁表。
- 没写制作表、正式美术目录、资源库索引或批次总记录，没有 commit、reset、clean 或 checkout。
- 源码与运行资源写入已收束。下属状态：无。
