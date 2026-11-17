# 视觉接入交接 — 留焰 v3（真实留焰翻腾尾迹）

- **Blockbench 适用性**：适用（留焰涉及移动载体与固定路径采样面的空间尺度比例、`0.25 格` 半幅重叠排布、直立面向相机无滚转关系及原地升腾消散相位，非纯二维界面素材）。
- **关联术式 B / 原 batch**：`fire_flame_trail`（制作中第 3 行；本轮不改表） / `待制作-20261031`（`taskId: flame_trail_art_preview`，`attemptId: 20261113-v3-gemini-carbon`）。
- **资源状态**：**待确认**（过程归档候选稿 v3；待用户同版本视觉确认，未确认不能作为迁待验收交付，也不得直接替换正式运行资源）。
- **版本**：`v3`（2026-11-13，针对前两轮“未结合子弹路径”与“v2 半透明噪点超密直棍/慢爬失真”根因彻底重制）。
- **确认**：待用户确认本目录同版本二维完整组合动态预览（`flame-trail-v3-complete-effect.gif` 及关键帧）与真实 Blockbench 三维空间预览（`flame-trail-v3-3d-hero-closeup.png`、侧视图及 5 阶段视口截图）；不可沿用旧版回复。

## 同版本绑定文件与 SHA256

- **二维贴图源**：`D:/alianximod/docs/资源库/过程归档/留焰_v3_真实留焰翻腾尾迹_20261113/flame_trail_v3.aseprite`
  - 尺寸与结构：`64×64` px，16 帧（`32×32` 逻辑像素构形最近邻 2× 放大为清晰 `2×2` 像素簇；6 级 Alpha=255 实色阶梯对齐 `fire_spark_v3`，无半透明噪点雾）；源内单帧预览时长 80ms，单次正向演化不循环（游戏中按采样点独立年龄 `0..39 tick` 映射 `0..15` 帧，40 tick 销毁）。
  - SHA256：`63E13A9356846D981AB111DFE2A774ED50827E741EB8654E9385293E77DDFD9E`
- **二维运行候选导出（16 帧竖条）**：`D:/alianximod/docs/资源库/过程归档/留焰_v3_真实留焰翻腾尾迹_20261113/flame_trail_v3_strip.png`
  - 尺寸：`64×1024` px（自上而下 `frame 0..15`，每帧 `64×64`）
  - SHA256：`50946B75FBBEFDA2F4A34FC337FD8A665A18F0428638F574CB2880CA88B06D1D`
- **二维完整组合场景源**：`D:/alianximod/docs/资源库/过程归档/留焰_v3_真实留焰翻腾尾迹_20261113/flame-trail-v3-scene.aseprite`
  - 尺寸与结构：`592×160` px（`1 世界格 = 64 px`，覆盖 `9.25×2.5 格`），62 帧，50ms/帧（`1 帧 = 1 tick`），含三层：`示意辅助_暗夜地面与一格刻度`、`留焰轨迹_flame_trail_v3`、`火苗载体_fire_spark_v3`。
  - SHA256：`21EFDFE97C317CF36DB57164A8BE0874434FBE64426FABB96FB40A591B04EC49`
- **二维主预览（完整组合动态 GIF）**：`D:/alianximod/docs/资源库/过程归档/留焰_v3_真实留焰翻腾尾迹_20261113/flame-trail-v3-complete-effect.gif`
  - 实测规格：`1184×320` px（场景源最近邻 2×），62 帧，50ms/帧（总长 3.10s）；Aseprite 原生批处理制作与导出，非 GUI 操作。
  - SHA256：`C1ACF6A69E6DC464C158F1A7EFD96942A3C54A5C9E51F9AE9F4CDC773110BF44`
- **二维辅助与关键阶段预览**：
  - 16 帧网格总览：`D:/alianximod/docs/资源库/过程归档/留焰_v3_真实留焰翻腾尾迹_20261113/flame_trail_v3_grid_4x4.png`（SHA256：`5B02474D568E799CB3D42721BBFA8F56787C45478155657F0172817D71A3D6C4`）
  - 单贴图动画：`D:/alianximod/docs/资源库/过程归档/留焰_v3_真实留焰翻腾尾迹_20261113/flame_trail_v3_single.gif`（SHA256：`8F8EFCEB9094E551619AC4CA2ED023035ABE94ECA3C9FF8A1C66C2CABD1C8217`）
  - 起飞破空帧（t=03）：`D:/alianximod/docs/资源库/过程归档/留焰_v3_真实留焰翻腾尾迹_20261113/flame-trail-v3-2d-stage0-launch-t03.png`（SHA256：`E9108FC52EE2E5584F6A45E6F5C198ECED4538413451242E9CDEA2BAE1DA0098`）
  - 疾掠初展帧（t=08）：`D:/alianximod/docs/资源库/过程归档/留焰_v3_真实留焰翻腾尾迹_20261113/flame-trail-v3-2d-stage1-dash-t08.png`（SHA256：`1666042D4892ED4290634E25C394C223F20F687130661F6122D67650DEBCEB7B`）
  - 全幅展开帧（t=14）：`D:/alianximod/docs/资源库/过程归档/留焰_v3_真实留焰翻腾尾迹_20261113/flame-trail-v3-2d-stage2-full-wake-t14.png`（SHA256：`21A3B7E311F154D5B1FC535C3E6CBEE0732EFA02418E4D57D10FDD659CC59106`）
  - 离场升腾拉断帧（t=24）：`D:/alianximod/docs/资源库/过程归档/留焰_v3_真实留焰翻腾尾迹_20261113/flame-trail-v3-2d-stage3-linger-rise-t24.png`（SHA256：`CA15F0214139F336AC56A3496DEF4C65AA97AAD51B1F5C10C7621FC720F88D72`）
  - 碎裂化星帧（t=36）：`D:/alianximod/docs/资源库/过程归档/留焰_v3_真实留焰翻腾尾迹_20261113/flame-trail-v3-2d-stage4-breakup-embers-t36.png`（SHA256：`2C86E2281378D57D22C8049701933E47F54C5768A10885B05A3EA97EF49ACC5E`）
  - 收尾将熄帧（t=48）：`D:/alianximod/docs/资源库/过程归档/留焰_v3_真实留焰翻腾尾迹_20261113/flame-trail-v3-2d-stage5-fadeout-t48.png`（SHA256：`5889648709ADF863A47A90337563E2C895F58368469BBD210C29180F8F18231E`）
- **只读引用载体素材**：`D:/alianximod/docs/资源库/特效/火焰/火苗_飞行火舌_v3/fire_spark_v3_strip.png`（只读未改，SHA256：`672DA9132E7E91A83636836A62ADE4778781D51592A541041C511653238DEC5E`）及本目录同源分帧 `frames/flame_trail_v3_f00.png`..`f15.png`（详见 `manifest.json`）。
- **三维 `.bbmodel`**：`D:/alianximod/docs/资源库/过程归档/留焰_v3_真实留焰翻腾尾迹_20261113/flame-trail-v3-projectile-composite.bbmodel`
  - Blockbench desktop `5.2.2` / plugin `1.10.1` 独立 `free` 工程，UUID `8cdb8be1-7355-a034-3a20-7ae41a0e5af1`，定格于 `tick = 14` 全幅展开状态，内嵌同版纹理。
  - SHA256：`365064F04B05A9816CDCE2E37B77B09F2944A34A4EF31D82B968C9F2701CD85F`
- **真实 Blockbench 主视口预览（`1706×1215` px，由已连接主视口按需渲染导出并经 `capture_screenshot(view="active")` 核对；当时窗口后台遮挡 `visibilityState=hidden`，属真实主视口 WebGL 画布输出而非桌面窗口截图）**：
  - 近景 3/4 透视主图（t=14）：`D:/alianximod/docs/资源库/过程归档/留焰_v3_真实留焰翻腾尾迹_20261113/flame-trail-v3-3d-hero-closeup.png`（SHA256：`971A4D712280FE532EA627527FEA64C057D8382DB6141E895A4C9E0361ACB377`）
  - 正侧视图（t=14）：`D:/alianximod/docs/资源库/过程归档/留焰_v3_真实留焰翻腾尾迹_20261113/flame-trail-v3-3d-side-view.png`（SHA256：`6578840919FBE79E9415C81F7C5186D6C69BA1403441127A77C44A716024DC1B`）
  - 阶段 1 起飞破空（t=03）：`D:/alianximod/docs/资源库/过程归档/留焰_v3_真实留焰翻腾尾迹_20261113/flame-trail-v3-3d-stage1-launch-t03.png`（SHA256：`9EFF891C5CEB834BA1850E128B51B3DEE6EBE8ABBB3439FC5D1EAD4B7EA6B8A7`）
  - 阶段 2 疾掠展开（t=08）：`D:/alianximod/docs/资源库/过程归档/留焰_v3_真实留焰翻腾尾迹_20261113/flame-trail-v3-3d-stage2-dash-t08.png`（SHA256：`EEE351B9D1BF4DDB7020E5FCB02964383B4D35C731C531A9CB146770117933F9`）
  - 阶段 3 全幅展开（t=14）：`D:/alianximod/docs/资源库/过程归档/留焰_v3_真实留焰翻腾尾迹_20261113/flame-trail-v3-3d-stage3-full-wake-t14.png`（SHA256：`1F876758949F26CDD57651197BB5DEFC2C75B6FFCD8B7B513BA7938F105D7A74`）
  - 阶段 4 离场整条燃烧与左段拉断（t=24）：`D:/alianximod/docs/资源库/过程归档/留焰_v3_真实留焰翻腾尾迹_20261113/flame-trail-v3-3d-stage4-linger-rise-t24.png`（SHA256：`3C0DDE4900A6FF661F4488FE22F29A5D917762D6277FC13BCC7626A7687549C4`）
  - 阶段 5 碎裂火星与熄灭收尾（t=38）：`D:/alianximod/docs/资源库/过程归档/留焰_v3_真实留焰翻腾尾迹_20261113/flame-trail-v3-3d-stage5-breakup-embers-t38.png`（SHA256：`BA1BBE06A522A7526C6DEFE9A45193014F12909AC0F02CB52410BF70D3B1FB28`）

## 空间与渲染映射参数

- **换算**：`16 模型单位 = 1 世界格`（二维场景源中 `64 px = 1 世界格`，导出 GIF 最近邻 2× 后 `128 px = 1 世界格`）。
- **轴向与前向**：`+X` 为载体示例飞行前向，`+Y` 为世界竖直向上（火焰热浮力升腾方向），`Z = 0` 为轨迹中心平面；地面顶面位于 `Y = 0`。
- **原点 / 锚点 / 旋转中心**：每个留焰采样面锚点位于其几何中心 `[sx, pathY, sz]`（对应世界采样坐标，`halfSpan = 0.25 格` = `4.0` 模型单位）。
- **父子层级**：
  - `carrier_fire_spark_v3`：移动火苗载体示意组（运行时为载体渲染）。
  - `trail_flame_trail_v3`：固定在世界路径上的 34 个独立采样面 `trail_sample_00`..`33`（**不作为载体子级跟随移动**，生成后锁定世界坐标）。
  - `示意辅助`：含 `示意辅助_地面1格刻度网格` 与 `示意辅助_施法玩家假人_28.8高`。
- **局部坐标与世界坐标**：示意弹道中心离地高度 `Y = 20.0` 模型单位（`1.25 格`）；采样点自 `X = 4.0` 起严格按 `ΔX = 4.0` 模型单位（`0.25 格`）沿 `+X` 排至 `X = 136.0`。三维示意中相邻面设置 `0.015` 模型单位（`< 0.001 格`）极微 Z 偏移仅用于避免静态共面深度冲突。
- **尺寸 / 偏移 / 朝向**：
  - 火苗载体 `fire_spark`：`12.8 × 12.8` 模型单位 = **`0.8 × 0.8 格`**（`halfSpan = 0.4`），火头指向 `+X` 飞行方向。
  - 留焰采样面 `TRAIL`：`8.0 × 8.0` 模型单位 = **`0.5 × 0.5 格`**（`halfSpan = 0.25`），**直立面向相机、无飞行方向滚转**（贴图顶部恒朝世界 `+Y`，使斜向与曲线轨迹下火焰同样自然向上升腾）。
- **UV / 帧布局**：运行时竖条贴图 `64×1024` px，自上而下共 16 帧（`frame = 0..15`），单帧区域 `u ∈ [0, 64], v ∈ [frame * 64, (frame + 1) * 64]`；连贯燃烧帧（`0..4`）采用 `16` 逻辑像素周期谐波与开放左侧热核截面，使 `0.25 格` 半幅重叠无缝熔接成连续金橙火脉，无锯齿栅栏或暗边竹节缝。
- **材质 / 透明**：`entityTranslucent` + `FULL_BRIGHT`（Blockbench 中 `shade = false`），顶点 alpha 固定 `255`；贴图自身为纯净像素抠像（实色像素 Alpha=255，背景 Alpha=0），不依赖任何运行时代码 alpha 衰减或动态缩放。
- **时间 / 循环 / 相位**：单采样点寿命固定 **`40 tick`**（2.0s），帧选择公式严格为 `frame = floor(age * 16 / 40)`（每帧对应 `2.5 tick`，不循环，`age >= 40` 移除）。
- **跟随对象与运动绑定**：仅在载体跨越 `0.25 格` 采样步长时于载体当前位置生成新留焰点；已生成留焰点留在原世界位置独立累计 `age`，载体飞离或销毁后已有火径继续原地翻腾升腾至自然熄灭。
- **预览速度**：二维与三维阶段演示采用 `0.625 格/tick`（约 14 tick 掠过 `8.75 格` 舞台，真实游戏火苗弹速约 `0.8 格/tick`），仅用于在同一画幅清晰展示“疾掠展开 → 离场留焰燃烧 → 逐段拉断化星 → 熄灭”完整过程，不代表施法时长或更改弹速配置。

## 接入与验收边界

- **待替换占位**：由 Grok 接入任务核对当前 `BatchCarrierVisualRenderer` / `TrailContract` 所引用的留焰占位贴图路径（未获用户确认前本轮不改动 `src/main/resources`）。
- **运行资源**：待用户确认本版 `v3` 后，正式归档至 `docs/资源库/特效/火焰/` 并由 Grok 复制 `flame_trail_v3_strip.png` 至游戏内对应特效贴图路径。
- **接入回执（由 Grok 接入者补充）**：未接入（待用户确认本版预览后派发）。
- **迁待验收**：不可迁移；须待用户同版本视觉确认、正式资源库归档、Grok 接入替换占位并编译核对满足流程第 8 节全部条件后方可迁入「待验收」。
- **示意辅助**：2D 图层 `示意辅助_暗夜地面与一格刻度`；3D 分组 `示意辅助`、`示意辅助_地面1格刻度网格`（`示意辅助_grid_*`）、`示意辅助_施法玩家假人_28.8高`（`示意辅助_dummy_*`）及 `helper_*` 纯色参照贴图、只读参考 `fire_spark_v3_ref` 均仅供预览参照，**不进入留焰运行资源**。
- **Blockbench 不能代表的运行效果**：运行时实时面向任意观察相机旋转的动态 billboard 矩阵、曲线/抛物弹道下的逐 tick 连续生成、真实世界方块光照环境对比及命中判定结算，须在接入后由用户在 Forge 1.20.1 客户端实测。
