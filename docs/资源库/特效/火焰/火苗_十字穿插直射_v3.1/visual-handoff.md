# 火苗 v3.1（原版箭矢式 ±45° 十字穿插 · 按发射方向直射）视觉接入交接

- **Blockbench 适用性**：适用。虽然运行端最终由 `BatchCarrierVisualRenderer` 以透明发光面片顶点绘制而不直接加载 `.bbmodel` 网格，但本版将空间结构从「面向相机的单张告示板」定型为「沿发射方向一次锁定生成的原版箭矢式 `±45°` 正交十字穿插双面面片（`×` 形截面）」，直接规定三维朝向、双翼夹角、前后首尾轴向以及玩家第一人称发射视角的眼下视觉偏置，同版 `.bbmodel` 与 5 张真实 Blockbench 视口截图随本正式归档一并固化。
- **关联术式 B / 原 batch**：`fire_spark`（火苗） / `待制作-20261031`
- **资源状态**：**已获用户确认并正式归档（2026-11-13 用户明确回复原话：『采用』）**
- **版本**：`v3.1`（正式目录 `火苗_十字穿插直射_v3.1`；二维贴图同版复用 `fire_spark_v3`）
- **确认**：
  - 二维像素与 `v3` 火舌方向已于 `2026-11-11` 获用户确认原话：「这版可以接入」。
  - `2026-11-13` 用户实测反馈原话：「修改朝向后我释放火苗发现火苗的特效现在会转圈，但是我的预期是火苗直射出去，同时为了让玩家第一人称可以看清特效做成箭矢那样的十字穿插，按照发射方向生成而不是持续面向玩家」。
  - `2026-11-13` 用户审阅本 `v3.1` 十字穿插直射真实 Blockbench 三维视口预览（含第一人称发射视角、3/4 透视、迎面轴向十字截面、正侧视图与同射线双阶段直射对照图）后明确回复原话：**「采用」**。
- **二维源**：`D:/alianximod/docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/fire_spark_v3.aseprite`（单帧 `64×64`，16 帧顺序条带，Aseprite 演示帧长 `80 ms/帧`，循环播放；`10,644` bytes，SHA256 `710957E6C13AB8D8CC0C9EA5BD6CFD424041EE5C8F9BE8231CF75D4EB15C8FCA`；与 `火苗_飞行火舌_v3/fire_spark_v3.aseprite` 逐字节一致）
- **二维导出**：`D:/alianximod/docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/fire_spark_v3_strip.png`（`64×1024`，16 帧自上而下竖排，每帧 `64×64`，纯二值 Alpha `0`/`255`；`5,005` bytes，SHA256 `672DA9132E7E91A83636836A62ADE4778781D51592A541041C511653238DEC5E`；与 `火苗_飞行火舌_v3/fire_spark_v3_strip.png` 逐字节一致）
- **二维预览**：`D:/alianximod/docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/fire_spark_v3.gif`（`26,426` bytes，SHA256 `A3EA8EB30C557859FCE0D04E21EE0B7E1BFD5E6ED6A155DA456E8F8EE12AB0AE`；与 `火苗_飞行火舌_v3/fire_spark_v3.gif` 逐字节一致）
- **素材**：同上 `D:/alianximod/docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/fire_spark_v3_strip.png`（`.bbmodel` 内嵌纹理 `fire_spark_v3_strip` 导出字节实测 SHA256 完全一致：`672DA9132E7E91A83636836A62ADE4778781D51592A541041C511653238DEC5E`）
- **.bbmodel**：`D:/alianximod/docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/fire-spark-v3-1-crossed-launch-preview.bbmodel`（Blockbench `5.2.2` `free` 通用模型，`42,052` bytes，SHA256 `8E676075011EC74FC596BC0E1F046D7CAC62DACEE87A0CF3B9C79A3567B4C47B`；与过程归档同文件逐字节一致）
- **实际预览**（均自真实 Blockbench `5.2.2` 桌面端活跃主视口 `main` 导出，`1366×973` 无损最近邻 PNG，与过程归档同文件逐字节一致）：
  1. 第一人称发射视角：`D:/alianximod/docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/fire-spark-v3-1-3d-01-first-person-launch.png`（`50,738` bytes，SHA256 `A25A668C6ED21C136467E74039802B61F2B8E2FED3AD1C4634F9C86EC3FAF68D`）
  2. 3/4 近景透视主图：`D:/alianximod/docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/fire-spark-v3-1-3d-02-hero-3quarter.png`（`107,329` bytes，SHA256 `B06516E2AB4197529B23851463050E3F5B00525EB4879FEBC47EAEB1190ACA06`）
  3. 迎面轴向十字截面透视图：`D:/alianximod/docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/fire-spark-v3-1-3d-03-axial-cross-section.png`（`91,082` bytes，SHA256 `F4C48F8A7B95B38D1E98BA06F74CB6D930AC686ADB139A2E6F43C6D49CA46F07`）
  4. 正侧视图（正交）：`D:/alianximod/docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/fire-spark-v3-1-3d-04-side-view.png`（`31,892` bytes，SHA256 `0039AC3FB33439C300909DDBB4A9F09BB47FF67AE72327155784144783EF52A8`）
  5. 同射线双阶段直射对照图：`D:/alianximod/docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/fire-spark-v3-1-3d-05-ray-two-stages.png`（`71,835` bytes，SHA256 `7280F8D7ACFFB13D50881AC04340E8F119DA275D6F006218FA3CC4312844C689`）

## 空间与渲染交接参数

- **换算**：`16 模型单位 = 1 世界格`（`1 模型单位 = 0.0625 世界格`）。
  - 地面棋盘单格：`16 × 16` 模型单位 = `1.0 × 1.0` 世界格，顶面 `Y = 0`。
  - 施法玩家假人总高：`28.8` 模型单位 = `1.80` 世界格；眼高参考带中心 `Y = 25.92` 模型单位 = `1.62` 世界格。
  - 火苗弹体单翼面尺寸：`12.8 × 12.8` 模型单位 = `0.80 × 0.80` 世界格（对应 `halfSpan = 0.40` 世界格 = `6.4` 模型单位）。
- **轴向与前向**：
  - 示意场景发射方向为 **`+X`**（`unitLaunch = (1, 0, 0)`）。
  - `+X` 前端对应火舌圆润亮黄白火头（`U = 1`），`-X` 后端对应渐窄火尾与飞散火星（`U = 0`）。
  - 参考上轴 `referenceUp = (0, 1, 0)`（当 `|tangent.y| >= 0.999` 近竖直发射时回退为 `(1, 0, 0)`），经 Gram-Schmidt 正交化得到局部 `upAxis` 与右手侧向 `sideAxis = normalize(tangent × upAxis)`。
- **原点 / 锚点 / 旋转中心**：
  - 弹体局部原点设在十字穿插几何中心（示意近枪口弹体中心 `[22.0, 22.0, 0.0]`，即施法者前方 `1.375` 格、高 `1.375` 格，位于眼高 `25.92` 下方 `3.92` 模型单位 = `0.245` 世界格）。
  - 两张翼面均以弹体中心 `[22.0, 22.0, 0.0]` 为旋转枢轴绕前向轴 `+X` 旋转。
- **父子层级**：
  - `fire_spark_crossed_projectile`（根分组，`origin: [22.0, 22.0, 0.0]`）
    - `fire_spark_blade_plus45`（零厚度双面片，`rotation: [+45°, 0, 0]`）
    - `fire_spark_blade_minus45`（零厚度双面片，`rotation: [-45°, 0, 0]`）
  - `示意辅助`（根分组，仅作空间比例与多阶段对照，严禁进运行资源）
    - `示意辅助_地面1格刻度网格`
    - `示意辅助_施法玩家假人_28.8高`
    - `示意辅助_中段直射对照_x56`（默认隐藏，仅在导出 `05-ray-two-stages.png` 时开启）
- **局部坐标与世界坐标**：
  - 未旋转前局部面片范围：`x ∈ [-6.4, +6.4]`，`y ∈ [-6.4, +6.4]`，`z = 0`（世界坐标 `from: [15.6, 15.6, 0.0]`，`to: [28.4, 28.4, 0.0]`）。
  - 绕 `+X` 旋转 `±45°` 后，两片翼面的展向单位向量分别为：
    - `firstFin = (upAxis + sideAxis) / √2`（对应 `(0, +0.7071, +0.7071)`）
    - `secondFin = (upAxis - sideAxis) / √2`（对应 `(0, +0.7071, -0.7071)`）
  - 旋转后弹体世界包围盒：`X ∈ [15.60, 28.40]`（长 `0.80` 格），`Y ∈ [17.47, 26.53]`（高 `0.566` 格），`Z ∈ [-4.53, +4.53]`（宽 `0.566` 格），呈正交 `×` 形穿插。
- **尺寸 / 偏移 / 朝向（含已定型的第一人称眼下纯视觉偏置）**：
  - **朝向策略**：`CameraFacingTag.NONE`。仅在生成瞬间记录一次归一化发射方向 `unitLaunchDirection(launchDirection)`，飞行全程固定朝向，**不读取相机旋转矩阵，也不按每刻位移重算滚转**，彻底消除镜头移动或近轴抖动导致的「转圈」现象。
  - **第一人称眼下纯视觉偏置（已定型）**：眼下视觉偏置已定为沿 `-upAxis` 下移 **`0.245` 世界格**（`BatchCarrierVisualRenderer.SPARK_FIRST_PERSON_DROP = 0.245D`，与示意模型 `25.92 - 22.0 = 3.92` 模型单位 = `0.245` 世界格严格对应）。该偏置仅作用于客户端视觉面片锚点，不改变服务端 `TechniqueHitEntity` 的碰撞与弹道物理起点，确保玩家原地静止施法时第一人称视角下避免针孔相机共轴掠射退化，呈现与 `fire-spark-v3-1-3d-01-first-person-launch.png` 一致的饱满 `±45°` 十字穿插双翼与亮黄白火芯。
- **UV / 帧布局**：
  - 纹理总尺寸 `64 × 1024`（`uv_width = 64, uv_height = 1024`），共 `16` 帧，每帧 `64 × 64`：第 `k` 帧（`k = 0..15`）对应 `topV = k / 16.0`、`bottomV = (k + 1) / 16.0`（像素纵坐标 `[64k, 64(k+1)]`）。
  - 单翼正反两面同 UV 对齐，不镜像翻转火头：
    - 前向火头顶点 `( +half * tangent + half * finAxis )` -> `(U = 1.0, V = topV)`
    - 前向火腹顶点 `( +half * tangent - half * finAxis )` -> `(U = 1.0, V = bottomV)`
    - 后向火尾下缘 `( -half * tangent - half * finAxis )` -> `(U = 0.0, V = bottomV)`
    - 后向火尾上脊 `( -half * tangent + half * finAxis )` -> `(U = 0.0, V = topV)`
  - 两片 `±45°` 翼面的贴图上脊（`V = topV`）均朝斜上方（`+upAxis` 半空间），在中心轴线处汇合亮黄白火芯。
- **材质 / 透明**：
  - Blockbench 示意：`render_mode = "emissive"`、`render_sides = "double"`、`shade = false`、最近邻采样（`NearestFilter`），二值透明裁切。
  - 运行端对应：`RenderType.entityCutout(sheet)` + 正反双侧反向法线提交四边形顶点 + `LightTexture.FULL_BRIGHT`（`15728880`）+ `NetherDomainTint` 火焰通道染色。
- **时间 / 循环 / 相位**：
  - 运行端按弹体存活进度线性采样 16 帧：`frame = clamp(floor((ageTicks + partialTick) * 16 / durationTicks), 0, 15)`（默认 `durationTicks = 30` 刻，单轮播完不倒放）。
  - 两片 `±45°` 翼面同步使用同一帧 `frame`，无相位差。
- **跟随对象与运动绑定**：
  - `BatchCarrierVisual` 绑定宿主 `TechniqueHitEntity`（`fire_spark`），位置每刻同步宿主坐标（渲染端叠加 `-0.245 * upAxis` 纯视觉偏置），宿主移除时同步销毁；方向仅在出生时写入 `DATA_TANGENT_X/Y/Z` 一次并锁定。
- **预览速度**：
  - `fire-spark-v3-1-3d-05-ray-two-stages.png` 中 `X = 22`（第 1 帧 `frame 0`）与 `X = 56`（第 8 帧 `frame 7`）仅为同射线空间姿态演示，不代表游戏实际每刻飞行距离或施法时长（运行端速度与寿命以 `ListedRelease.spark()` / 配置表为准，默认 `speed = 0.8`、`lifetime = 30` 刻）。

## 归档与接入核对

- **待替换占位**：无新增贴图占位；本版复用已确认二维竖条 `fire_spark_v3_strip.png`，将运行空间形态与朝向正式升级为 `v3.1` 原版箭矢式 `±45°` 十字穿插直射（替换旧单面面向相机告示板表现）。
- **运行资源**：
  - 运行贴图路径：`assets/xueximod/textures/effect/fire_spark_v3/fire_spark.png`（工作区路径 `src/main/resources/assets/xueximod/textures/effect/fire_spark_v3/fire_spark.png`，与本正式目录 `fire_spark_v3_strip.png` 逐字节一致，SHA256 `672DA9132E7E91A83636836A62ADE4778781D51592A541041C511653238DEC5E`）。
  - 运行元数据路径：`assets/xueximod/textures/effect/fire_spark_v3/fire_spark.png.mcmeta`（工作区路径 `src/main/resources/assets/xueximod/textures/effect/fire_spark_v3/fire_spark.png.mcmeta`，配置 `blur=false, clamp=true`）。
- **接入回执（`20261113-integrate-spark-cross-01`，2026-11-13）**：运行贴图 `src/main/resources/assets/xueximod/textures/effect/fire_spark_v3/fire_spark.png` 与本目录 `fire_spark_v3_strip.png` 逐字节一致（`5,005` bytes，SHA256 `672DA9132E7E91A83636836A62ADE4778781D51592A541041C511653238DEC5E`）。已写入 `src/main/resources/assets/xueximod/textures/effect/fire_spark_v3/fire_spark.png.mcmeta`（UTF-8 无 BOM，`blur=false`、`clamp=true`，字节与 `flame_trail_v3/flame_trail.png.mcmeta` 一致）。`CameraFacingRegistry.tagOf(SPARK)=NONE`、`CameraFacingClient` 无火苗滚转、`TechniqueHitEntity.launchDirection()`、`BatchFlightBook.spawnBodyVisual`（`halfSpan=0.4F`）、`BatchCarrierVisual.spawnSpark`、`BatchCarrierVisualRenderer.drawSparkCross`（`SPARK_FIRST_PERSON_DROP=0.245D`）及 `ListedRelease.spark()` / `declareCarrier(spark(), true)` 与本文一致；6 个相关 Java 文件指纹与 `impl-02` 相同，未重写。`.bbmodel`、`示意辅助*` 与 `helper_*` 未进入 `src/main/resources/`。统一 `compileJava` 仍待主会话派指定编译者。正式接入报告：`docs/开发记录/术式批量制作/待制作-20261031/spark-crossed-v31-integration-report-20261113.md`。
- **迁待验收**：按 `docs/统一开发与术式批量制作流程.md` 第 8 节核对全部条件；须待 Grok 完成确认版运行核对、统一编译并写回制作表后迁入 `待验收`，本美术归档回执不单独代替迁页证据。
- **示意辅助**：整个 `fire-spark-v3-1-crossed-launch-preview.bbmodel` 工程以及其中的 `示意辅助`、`示意辅助_地面1格刻度网格`（`示意辅助_grid_*`）、`示意辅助_施法玩家假人_28.8高`（`示意辅助_dummy_*`）、`示意辅助_中段直射对照_x56`（`示意辅助_midflight_*`）与 `helper_*` 辅助纯色贴图均仅保存在资源库作空间比例参照，**严禁进入游戏运行资源**。
- **Blockbench 不能代表的运行效果**：
  1. 施法者移动、视角快速横摇与网络插值（`partialTick`）下的实机第一人称动态观感；
  2. `NetherDomainTint` 领域环境动态染色、留焰附魔（`BatchFlameTrail`）尾迹衔接与实体命中点燃反馈。
