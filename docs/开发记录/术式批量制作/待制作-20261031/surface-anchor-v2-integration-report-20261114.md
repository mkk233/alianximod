# 地表定位 v2 确认版运行接入报告

- batchId：`待制作-20261031`
- taskId：`rule_surface_anchor_v2_integration`
- attemptId：`20261114-integrate-surface-anchor-v2-01`
- stage：`implementation`（确认版正式运行接入与占位替换；无独立测试；待统一编译）
- 操作角色：`implement`
- 真实直接父代理：主会话
- 关联 B：`rule_surface_anchor`（地表定位）
- 依赖：`20261114-art-surface-anchor-v2-01`。用户于 2026-11-14 明确确认「之前那个版本的效果可以接入」。
- 额度：设计 `2/2`、实现 `3/3`。本阶段额度已用完，返修/接入结果未追加复审，不重置计数。
- 结果：运行贴图已逐字节对齐，`SURFACE` 改指 v2，生成宽度与正交竖直十字已接入；待统一编译。制作表未写。

## 贴图

- 确认导出 `docs/资源库/过程归档/地表定位_倒三角_v2_20261114/surface_anchor_v2_strip.png` 与运行贴图 `src/main/resources/assets/xueximod/textures/effect/surface_anchor_v2/surface_anchor.png` 均为 `4,741` 字节，SHA256 `4434BDC7B0311E863277E9BD23761139E77489AD6B548FBB48AC202C4FB35A20`。
- 新增 `src/main/resources/assets/xueximod/textures/effect/surface_anchor_v2/surface_anchor.png.mcmeta`：UTF-8 无 BOM，`64` 字节，与 `fire_spark_v3/fire_spark.png.mcmeta` 逐字节一致，SHA256 `B2CAA414505E1C556CF68460294471D97198A50F3D69ED1FD9B717510C0D6F4A`。内容为 `texture.blur=false`、`texture.clamp=true`，CRLF `5`、LF `0`。
- 旧运行贴图 `src/main/resources/assets/xueximod/textures/effect/surface_anchor_v1/surface_anchor.png` 保留：`4,058` 字节，SHA256 `7801A4513A63D26DAA8567F06A33834E46FD9F0AD71F5CDA3CE7F471AC39D018`，未删除。
- 未把 `.bbmodel` 或示意假人复制进 `src/main/resources/`。未改 `docs/资源库/`。

## Java 指纹

|文件|改前字节|改前 SHA256|改后字节|改后 SHA256|换行|
|---|---:|---|---:|---|---|
|`carrier/BatchVisualTexture.java`|1,188|`DD8D646312CB66C64F1BEA53A99D7E54198DBFD50E749241BFAC77458D742B56`|1,188|`2565E67152AA4B5743DDF50734E28886538756D038551E8296786858D8FF64E4`|LF 35|
|`carrier/BatchTriggerVisuals.java`|4,306|`4A22FD2A3942ED03DC75B2F3B699671A62F0121FDE04FAD623D8DB698E9FA91A`|4,306|`049F7BC0EEF4F291143B492E977D2D3BB15978BCCC34BFB48B8759C4AD6E4226`|LF 105|
|`client/BatchCarrierVisualRenderer.java`|27,963|`E9C4E19FBBD9AF111B5091E06136FCCA7C92767D00575CE1A6F2A5D8A6FB7387`|32,520|`B848C0A67C26FC06EA09DB398EB8C7B620B556A80718F1F04AF7E608672696BE`|LF 535|
|`carrier/BatchCarrierVisual.java`|21,162|`2AF18907D35327ADF45F422D8AB5F81B2A99F73E3A91D7A97F1429BFA03B024B`|21,502|`2BF108F5770E7509880CE9CECB847A3B890D7037FCBD4240758012A615F45EFD`|LF 557|

四个 Java 文件均为 LF，CRLF `0`。

## 接入对照

- `BatchVisualTexture.SURFACE` 从 `textures/effect/surface_anchor_v1/surface_anchor.png` 改为 `textures/effect/surface_anchor_v2/surface_anchor.png`。
- `src/main` 内已无 `surface_anchor_v1` 硬编码引用。旧贴图文件仍在。
- 地表定位视觉出口仍汇入 `BatchTriggerVisuals.surface`：`BatchCastSession` 在 `plan.needsSurface` 且 `SurfaceAnchor.resolve` 成功后调一次；`BatchSurfaceAnchorEffect` 也调同一入口。两处源码未改。
- `surface(...)` 的 `halfSpan` 从 `0.75F` 改为 `0.40F`，寿命仍是 `STRIP_FRAMES`（16 tick），同 tick 同方块静标去重未改。
- `CameraFacingRegistry` 对 `SURFACE` 仍返回 `CameraFacingTag.NONE`。该文件只读，未改。
- 仅 `texture == SURFACE && facing == NONE` 走新几何。`RING`、`TRAIL`、`SPARK`、`CAMERA_PLANE` 和其余 billboard 分支未改。
- 锚点是实体原点 `(0, 0, 0)`，即 `sample.point()`，选中方块碰撞顶面 `Y = 0`。
- 竖直区间：`yBottom = 0.04F`，`yTop = 0.04F + halfSpan * 2.0F`。`halfSpan = 0.40F` 时为 `[+0.04, +0.84]`，宽高各 `0.80` 世界格。
- 对角半偏移：`d = halfSpan * (float) INV_SQRT_2`。float32 下 `0.40F * 0.7071067811865476D` 为 `0.28284273F`，与交接手写的 `0.2828427F` 同一值。
- 平面 A（`+45°`）：`(-d, yBottom..yTop, -d)` 至 `(+d, yBottom..yTop, +d)`，正法线 `(-axis, 0, +axis)`。
- 平面 B（`-45°`）：`(-d, yBottom..yTop, +d)` 至 `(+d, yBottom..yTop, -d)`，正法线 `(+axis, 0, +axis)`。
- 每张面显式提交正反两面，共 4 个单向 quad、16 个顶点。反面法线取反。
- UV：`topV = frame / 16` 绑 `yTop`，`bottomV = (frame + 1) / 16` 绑 `yBottom`，水平 `u` 从 `0` 到 `1`。帧仍由现有 `frameIndex` 按存活年龄推进，`0..15` 各一帧后销毁，不循环。
- 材质：`RenderType.entityCutout(sheet)`。光照：`LightTexture.FULL_BRIGHT`。`acceptsFlameTint` 仍不含 `SURFACE`，颜色走 `NetherDomainTint.Sample.plain()`，即 `255, 255, 255, 255`。

## 包围盒

- 实体类型仍是 `sized(0.5F, 0.5F)`，默认箱体约为锚点上方 `[0, 0.5]`，盖不住 `[0.04, 0.84]`。
- 只读确认了 `shouldRender` 会把箱体再 `inflate(max(1, halfSpan))`，视锥判定已经盖住。但任务要核的是碰撞箱本身，所以不把这层判定当成箱体已覆盖。
- 最小调整：只有 `textureKind() == SURFACE` 时，`makeBoundingBox()` 在原箱体上 `expandTowards(0, 0.5, 0)`。顶面从 `+0.5` 提到 `+1.0`，完整含住 `+0.84`；底面仍是锚点 `Y = 0`。其余贴图继续走 `super.makeBoundingBox()`。
- `noCulling` 仍是 `true`，未改。

## 名称门禁

- 命令：`powershell -NoProfile -File tools/check-name-gate.ps1`。
- 全仓退出码 `1`，末行「共 274 处」。命中均在本单未改的既有文件。
- 本单改过的 `BatchVisualTexture.java`、`BatchTriggerVisuals.java`、`BatchCarrierVisual.java`、`BatchCarrierVisualRenderer.java` 命中 `0` 处。未修无关文件。

## 配置兼容

只读 `ListedRelease.surfaceAnchor()`、`ReleaseDefinitions.declareSpecialBalances()`、`Technique` 和 `docs/配置平衡指南.md`。本单没有新增或重命名配置键，稳定 ID 仍是 `rule_surface_anchor`。

|键|代码默认|单位|范围|生效|
|---|---:|---|---|---|
|`cooldownTicks`|`0`|刻|`0` 到 `1200000`；原始默认已是 0，配置 0 合法|下一次服务器启动或重新打开单机世界；世界开着时改磁盘，这一局不变|
|`manaCost`|`0`|法力|`0` 到 `1000000`|同上。`ReleaseKind.RULE` 返回 0|

- 没有 `damageMultiplier`。基础伤害是 `0`，不在指南的伤害倍率名单里。
- 没有专项键。`lifetimeTicks = 1`、`speed = 0`，`declareSpecialBalances()` 不为这条规则登记寿命或速度。
- 显式装配消耗是 `1`。指南写明装配消耗首版不开放。
- 视觉常量不开放：`halfSpan = 0.40` 世界格、高度 `[+0.04, +0.84]`、寿命 16 刻、全亮、纯白。改配置不改这些画面常量。
- 全局 `manaMultiplier`、`cooldownMultiplier` 仍各乘一次。没有全局尺寸倍率。

## 状态拆分

- 功能：落点、失败拒绝、扣费和冷却未在本单改动。确认版只替换显示。
- 美术：`20261114-art-surface-anchor-v2-01` 已交付确认版。
- 用户确认：2026-11-14 用户明确说「之前那个版本的效果可以接入」。
- 正式接入：运行贴图、元数据、引用和几何已接入。正式资源库归档不在本单写权限内，未做。

## 建议待验收展示

当前不迁待验收。缺统一编译，也缺正式资源库归档。条件齐后可用：

- 建议 `N`：`视线内第一个向上承托面上出现一个悬空倒三角。失败则整组不释放`
- 建议 `O`：`textures/effect/surface_anchor_v2/surface_anchor.png`

## 收束

- 编译：待统一编译。本回执不跑 `gradlew` / `compileJava`。
- 写回：未写 `docs/术式制作表.xlsx`、`记录.md`、`实测清单.md`、`docs/资源库/`。
- 未打包，未 `runClient`，未做游戏实测。
- 当前写入已收束，文件句柄关闭。
