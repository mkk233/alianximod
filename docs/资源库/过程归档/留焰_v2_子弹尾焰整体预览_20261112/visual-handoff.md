# 留焰 v2：子弹与留焰完整效果返修草稿

- batch：`待制作-20261031`；task：`flame_trail_art_preview`；本次 attempt：`20261112-projectile-composite-3d-resume`；二维来源 attempt：`20261112-projectile-composite-feedback`；B：`fire_flame_trail`。
- **状态：二维完整组合预览与同版真实三维草稿预览齐全，待本次用户视觉确认。不是正式采用版，不送接入，不迁待验收。本次仅补三维，已有二维源、PNG、GIF 全部保留不重做。**
- 用户反馈：“这个表现太差了，这个2d预览应该是尾焰拖在子弹身后形成的最终样子而不是拖尾特效本身”。
- 本次主预览为 `flame-trail-v2-complete-effect.gif`，不是单图素循环、帧接触表或多路线展示板。一个子弹从左向右运动，经过位置独立留下火焰，子弹离开后旧尾迹原地衰减至空场。

## 同版文件

所有本次草稿位于 `D:/alianximod/docs/资源库/过程归档/留焰_v2_子弹尾焰整体预览_20261112/`。

- 完整效果：`flame-trail-v2-complete-effect.gif`，1152×336，最近邻 3 倍，104 帧×50 ms，共 5.2 秒循环。
- 代表帧：`composite-t40.png`；阶段帧：`composite-t04.png`、`composite-t20.png`、`composite-t52.png`、`composite-t68.png`、`composite-t84.png`、`composite-t96.png`。图中没有技术长标题或小样拼贴。
- 可编辑场景：`flame-trail-v2-composite.aseprite`，384×112，104 帧，背景、固定路径尾焰、移动子弹三层。
- 新候选尾焰图素：`trail-stamp-v2.aseprite`、`trail-stamp-v2-strip.png`，32×32×16 帧，竖条 32×512。单图素的 80 ms 展示时长不是 40 tick 世界寿命。
- 原尺寸透明过程素材：`trail-age-00.png` 至 `trail-age-79.png`；`projectile-frame-01.png` 至 `projectile-frame-16.png`；`projectile-v3-nn-strip.png`。
- 二维制作脚本：`create-composite-preview.lua`。本次实际在原生 Blockbench 执行的三维脚本：`build-composite-blockbench.js`、`capture-composite-blockbench.js`；模型和视口证据另列，不以脚本代替产物。
- 参数：`composite-parameters.json`；素材、产物、脚本的 SHA256：`manifest.json`。
- 可编辑同版模型：`flame-trail-v2-projectile-composite.bbmodel`，109 元素、4 组、98 贴图，保存为 tick 40 状态；没有创建播放动画轨道。
- 真实近景透视：`viewport-close-perspective-t40.png`；真实侧视：`viewport-side-t40.png`。
- 真实关键阶段：`viewport-phase-t04.png`、`viewport-phase-t20.png`、`viewport-phase-t40.png`、`viewport-phase-t52.png`、`viewport-phase-t68.png`、`viewport-phase-t84.png`、`viewport-phase-t96.png`；七张使用相同相机，依次为生成、飞行、展开、离场前、残留、碎灭、空场。不是离线合成。
- 三维实际采样参数：`blockbench-scene-parameters.json`；实际相机、阶段、活跃采样数、视口尺寸证据：`blockbench-capture-log.json`。上述九张均为真实主视口 1706×1275 原始 PNG。

## 素材沿用与改动

先尝试将既有留焰 v1 组合为水平尾迹，实际看图后仍显细薄、重复；试组合画面只保留为 `process-v1-composite-trial.png`，不是主预览。随后在 Aseprite 原生 batch 的像素 API 内绘制了 **新的 v2 水平火舌候选图素**，以密集重叠、不同出生时刻与独立衰减组成最终尾焰。因此不能称本版“尾焰纹理未变”。

子弹沿用 `D:/alianximod/docs/资源库/特效/火焰/火苗_飞行火舌_v3/fire_spark_v3.aseprite`，仅在本草稿中最近邻 2:1 缩为 32×32；未修改原画文件。场景以 50 ms 重放子弹相位，原素材 80 ms 时长未改。

留焰原文件只读且 SHA 保持：

- `D:/alianximod/docs/资源库/特效/火焰/留焰_v1/flame_trail.aseprite`：`656C31D277D03FDBF9D4A6AAF5C398CB13ED1F0D97B07240928071B25C7FFA55`。
- `D:/alianximod/docs/资源库/特效/火焰/留焰_v1/flame_trail-strip.png`：`F1B0D723B4AEA57B37FCA4928AFAF492E8F1020D864183F36EDABF379BE95284`。

## 构图、运动与衰减

- 原尺寸 64 px＝1 世界格；透明采样面 32×32 px＝0.5×0.5 格。固定地面位于画面 y=76，子弹/采样中心 y=43；飞行高度示意为 33/64 格。
- 子弹初始中心 x=32；tick 0–7 生成，tick 8 发射；演示速度 8 px/tick＝0.125 格/tick，向画面 +X。tick 52 附近离开画面，tick 56 后子弹不可见且不再发出新采样。速度仅用于演示，不是新增运行参数。
- 路径间距循环为 3、5、4、3、4、5 px，最大 5/64＝0.078125 格，小于已批准的 0.25 格；全程 97 点，不超过每载体 256 点。并非机械等间距火苗排栅栏。
- 每个点出生后固定在自己的经过位置，不与子弹整体平移。出生时刻为 `8 + offsetPixels/8`，可见条件为 `0 <= age < 40`。最后一点于 tick 96 熄灭，剩余帧为空场。
- 每点 40 tick 寿命保留。16 图素帧按年龄映射，另输出 0.5 tick 精度的 80 张透明度/冷却/碎化过程图。图素原展示节拍 80 ms、世界寿命 40 tick、完整演示 104×50 ms 是三个不同概念。
- 新尾焰的像素覆盖在固定半格采样面内部随年龄变细，透明度为 `0.44*(1-age/40)^1.6`，晚期碎化；这不是缩小判定直径或修改生命周期。
- 近子弹处是新生、较饱满明亮的尾焰，远处是更旧、更暗、更细碎的尾迹。主 GIF 展示上述组合结果；条纹与分帧仅是制作/映射素材。
- 本稿调整的是图素与合成观感。未证实这些候选透明度、相位、碎化及演示速度已经由当前 Forge 渲染实现，也不把示意转作新游戏逻辑。

## 三维适用性与实际完成结果

**Blockbench 适用**：此效果有载体、运动、固定路径位置、尺寸和 billboarding 关系，不是纯二维界面素材。

沿用原生 Blockbench desktop 5.2.2 / plugin 1.10.1 的独立 free 工程，UUID 为 `8a58e270-0fce-3a38-f8c5-528d850c4a39`。前一次 attempt 的实际阻塞保留为历史：能力可查，但聊天未发现 `risky_eval` schema，空工程未制作。本次按精确查询 `+mcp__magpie blockbench__risky_eval` 一次发现真实 schema，随后只读调用成功，并核对原 UUID、0 元素/0 贴图才继续；没有另建空工程。

原生 MCP 执行构建及阶段脚本成功。`16 模型单位＝1 世界格`；实际查询所有采样面的宽高均为 `8×8` 单位＝半格，中心高度 `8.25` 单位＝0.515625 格，最大路径采样间隔 `1.25` 单位＝0.078125 格。+X 为飞行方向、+Y 向上、Z=0 为中心轨迹。示意原点是发射时子弹中心的地面投影；二维 x=32 对应模型 x=0。97 个采样位置与出生时刻和二维一一对应。

模型层级为总组下的“留焰_97个固定采样点_每点40tick”“火苗v3_移动子弹”“示意辅助_地面与一格刻度”。所有火焰面均采用本目录同版 `trail-age-*.png`，子弹采用同版 `projectile-frame-*.png`；材质透明、双面显示、无光照明暗处理，UV 为 32×32 整图。不是连续体积网格。

每次切换阶段只改变该工程的可见点、年龄贴图、子弹位置/相位以及面朝观察相机的姿态，采样中心不跟随子弹移动。变更都在本任务对象的 Undo 事务内。真实主视口逐次 `render()` 后直接读取其已连接 canvas，未创建离屏视图，未用其他实例或截图拼贴。应用文档当时 `visibilityState=hidden`，因此产物是实际主视口 canvas 导出，**不是桌面窗口截图**。

七个阶段 tick 4/20/40/52/68/84/96 的活跃采样数分别为 0/25/65/80/56/24/0；tick 68、84 无子弹，tick 96 无残留。已实际查看近景透视、侧视和消散/空场输出。最终工程保存并停留在 tick 40 近景，保存前后 UUID 一致。

旧留焰 v1 工程 `e380502f-05e4-5cab-c7a2-4ff5f7e3c7c5`、冷却工程 `fe1b1c81-3962-2fbb-bc94-6a37e1b6e5ef` 及其文件未覆盖、未关闭；本次前后两者均为 saved=true。本版工程现为 saved=true。

限制：`.bbmodel` 是可编辑的 tick 40 场景快照，动画轨道数为 0；动态证据是实际视口阶段截图，不称播放录屏或 GIF。相机朝向、透明排序和不同年龄状态由制作脚本设置；重新打开模型或任意换相机不保证自动运行这些脚本，不能当作已实现的游戏 billboard 或生命周期系统。显卡透明叠加与 Aseprite 合成可有细微色差，地面/网格不进入运行资源。

## 确认与接入边界

- 本次二维操作是 **Aseprite 原生批处理预览，非 GUI 操作**。Lua 制作、原生 CLI GIF 导出成功，实际打开关键帧检查；Aseprite 再次读取 GIF 确认 1152×336、104 帧、逐帧 50 ms、总计 5.2 秒，`isUIAvailable=false`。
- 没有 ImageGen、Python 外绘、Computer Use、headless Blockbench、离线伪造视口。未重试已知超时的 Aseprite live。
- 确认绑定本目录的二维源、导出、完整 GIF、本版 `.bbmodel`、九张真实视口、参数与清单。绑定产物现已齐全，但不能沿用旧 v1 的确认，也不声称用户已认可这次返修。
- 功能：本轮不改；美术：同版二维/三维草稿预览齐全；用户确认：未取得；正式接入：未做。没有运行资源替换路径获准，不复制到 `src`。
- 背景/地面为 `示意辅助_固定地面背景_64px每格`，不进入运行资源。脚本里的地面与网格也全部命名“示意辅助”。
- 未编译、未启动游戏、未游戏验证、未打包、未改表或共享索引。设计 2/2、实现 3/3 不变，本次不是正式正确性复审。
