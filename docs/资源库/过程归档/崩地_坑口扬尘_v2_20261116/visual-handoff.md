# 崩地 · 坑口扬尘 v2 候选交接

版本 `earth-ground-mouth-dust-v2-20261116`。关联 `earth_break_ground`；batchId `待制作-20261031`；taskId `earth_break_ground_v2_art_preview`；attemptId `20261116-art-ground-v2-03`。直接父级 `01a1ce06-b681-7c90-b11d-260d3c169911`。

**状态：真实美术候选已制作，待用户同版本确认，未正式归档、未接入。** 不能据此迁待验收。功能状态沿父任务只读包，本次没有修改或重审功能。设计 2/2、实现 3/3 维持不变。**本阶段额度已用完，返修结果未追加复审**。

Blockbench 适用：土尘存在坑口挂点、空间散布、朝向、遮挡和运动关系；本版已使用真实桌面 Blockbench，未跳过三维。源、导出、模型、真实视口和本记录绑定为同一候选版，SHA256 在 `manifest.json`。用户尚未确认本版，2026-10-31 的 v1 暂用授权不是本版确认。

## 输入与范围

- `evidence/requirements-N-O-DV-original.json` 和 `.txt` 保存制作中第 11 行完整 N/O/DV，含全部历史段落，不以摘要替代原话。工作簿 SHA256 `87F22C7DCE22FD0AB83F219C1D18938AF381D2286C106A62CEBCA1A6A56E9C75`，190802 字节，本次只读。
- 最新已批准输入：`D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/approved-v1.md`。只做土尘，没有坑形贴图。无火焰、发光冲击环、额外爆炸或伤害。
- 坑按现行 `BreakGroundShape.java:11–41`：整数 `(east,down,south)`，`east²+south²≤4`，`down=0..2`，`2×down≤4-east²-south²`，cap32，实际23块。中心列3块、r²=1四列各2块、r²=2四列各2块、r²=4四列各1块。完整坐标见 `evidence/crater-offsets-23.json`。
- 半径是方块中心的2格；最深向下偏移2，含撞击层共3层，不是把总深度改成2层。连续方块外缘可到中心±2.5格。预览平地均假设为可破坏块，没有把该示意当作保护判定或游戏实测。

## 二维与三维产物

- `source/ground_dust_v2.aseprite`：真实 Aseprite 1.3.17 / API39 batch 制作，96×32，3个32×32变体 A/B/C 横排，3图层，16帧×50ms=800ms。图层为底尘、破散土尘簇、独立细粒。标签 `single_burst_800ms`；运行只播一次，预览 GIF 循环便于观看。
- `exports/ground_dust_v2-atlas.png`：96×512，RGBA 透明，横向 A/B/C，纵向帧1→16。另有 A/B/C 的 `32×512` 独立竖条，以及 `exports/frames/dust_01.png` 至 `dust_16.png` 原尺寸帧。
- `previews/ground_dust_v2-2d.gif`：768×256，最近邻放大，800ms，16个有效帧；原尺寸动图为 `ground_dust_v2-original.gif`。`ground_dust_v2-2d-contact.png` 用于查看关键阶段，不作为三维证据。
- `source/earth_break_ground_v2_preview.bbmodel`：真实桌面 Blockbench 5.2.2 / MCP plugin1.10.1 free 项目，原生 codec 保存并原生复开。367元素、192组、2贴图、1原生动画。可在 Animate 选择 `animation.earth_ground_v2_preview_only` 重放。
- `previews/earth_break_ground_v2-real-blockbench.gif`：真实活动视口40次取帧编码，1100×900，20fps采样，完整2秒。GIF 合并重复帧后28个编码帧、24个不同解码帧，时长仍2000ms；有效帧和时长实查见 `evidence/gif-verification.json`。不是离线渲染或拼贴。只有 GIF 编码时加统一深灰底、共同调色板，原始透明PNG全部保留。
- 真实视口 PNG：`overall-before.png`、`overall-dust.png`、`overall-clear.png`、`side-west-dust.png`、`side-west-clear.png`、`top-dust.png`、`top-clear.png`；复开后另取 `reopened-native-viewport.png`。原始时序帧在 `previews/3d-frames/`。

## 坐标、挂点与朝向

换算：**16 Blockbench 单位=1游戏方块**。模型原点 C=(0,0,0) 对应 `Vec3.atCenterOf(impact)`，不是方块顶面。X正向东、Y正向上、Z正向南。撞击块范围 `[-8,8]³`，地面顶面 Y=8，即 C+(0,0.5,0)。最深删除块底面 Y=-40，等于 C−2.5格，剩余坑底比原地面低3格。

每个尘团根部是纹理像素坐标 **(16,28)**，即 UV(0.5,0.875)，不是卡片的几何底边。根部世界坐标为 `C + (x,0.53,z) + drift(t)`；也就是初始在方块顶面上方 **0.03格**。底部4行透明留边可以位于根部以下；本版所有有色像素始终位于该根部之上，避免旧版低位内容埋入地下。独立簇的准确 x/z、尺寸、变体和末端位移在 `evidence/emitter-layout-candidate.json`，共11簇。

卡片边长为各簇的1.08–1.30格，横纵同尺寸。局部四角相对根部：横向±0.5×size，纵向从−0.125×size到+0.875×size。像素坐标 `(u,v)` 对应 `right×((u−16)/32×size) + up×((28−v)/32×size)`。如果接入代码仍用中心对称 quad，中心应为 `root + cameraUp×0.375×size`，halfSpan=size/2；这个偏移必须随相机基向量处理，不能简单加固定世界Y。

`ANCHOR_C_Vec3_atCenterOf_impact` 是总根，`CANDIDATE_DUST_only_11_separate_emitters` 下每簇依次为 WORLD_motion → CAMERA_PLANE → frame组 → 像素卡片。WORLD_motion 只表示世界位移；卡片面跟随相机完整 yaw/pitch，符合现行 DUST 的 **CAMERA_PLANE** 标签，不是轴向直立 billboard、水平尘环或固定世界面。

保存的原生模型使用整体视角对应的静态相机朝向样本。侧面/俯视截图在真实 Blockbench 中把每簇卡片朝向更新到该相机，模型原生时间轴仍可重放。自由旋转 Blockbench 视角不会自动持续执行 Minecraft billboard 逻辑；重现多视角可用归档 MCP 脚本，游戏接入时须由客户端每帧更新朝向。

原生根部运动从触发到0.8秒向外平移，各簇水平终位移约0.331–0.653格，向上0.2375格；纹理内部像素簇另有分裂和上扬。不是对整团做弹性缩放。原生帧组 scale 0/1 仅用于显隐切帧，不是缩放动画造型。

## 材质与时序

土尘使用低饱和棕土色像素簇，硬像素边界和离散颗粒，无模糊滤镜。RGBA alpha 从主体到零逐帧衰减，第15帧最大alpha33，第16帧完全透明；详见 `evidence/2d-frame-inspection.json`。建议 nearest 采样、无 blur，分帧 UV 防串边；原尺寸 PNG 是接入输入。

候选采用常规透明混合、世界光照，**不全亮、不发光、不作幽冥染色**。Blockbench 使用 default 非 emissive 材质和实际视口照明；它不等于 Forge 光照。Blockbench 原生 ShaderMaterial 开启 transparent、depthWrite，存在编辑器透明排序/细粒显示差异；最终排序、遮挡、粒子深度与帧插值仍需游戏实测。

推荐运行时长仍为 **16 tick / 0.8秒**，每50ms切一帧，播一次。无需沿用旧 Aseprite 的16×80ms=1.28秒。预览原生时间轴是2秒：0–0.4秒平地；0.4秒模拟23块删除并出尘；0.4–1.2秒扬起、向外散开并淡去；1.2–2秒露坑。二维末帧空白从0.75秒开始。2秒只是展示前后停留，不是施法时长、伤害窗口或冷却。

整体/侧面/俯视扬尘图在预览t=0.65秒（出尘后0.25秒），露坑图t=1.4秒；前态t=0.2秒。原生视口采样时间逐帧保存在 `evidence/native-capture-overall.json`。本地已实际查看二维联系图、整体扬尘/散尽、晚期帧、侧面、俯视与复开视口。

## 接入差异与边界

现行 `BatchTriggerVisuals.dust`（约78行）仍是单 BILLBOARD，DUST，16 tick，halfSpan=1.25，整面2.5格，点为 C、没有Y偏移。当前运行贴图是 `textures/effect/ground_dust_v1/ground_dust.png`，64×1024。**本候选的11簇、根部偏心挂点、位移和32像素帧均未在源码实现，不能只覆盖旧PNG就声称接入完成。**

确认后建议运行路径为 `assets/xueximod/textures/effect/ground_dust_v2/ground_dust_a.png`、`ground_dust_b.png`、`ground_dust_c.png`，分别来自三条32×512竖条。这是计划映射，当前文件不存在于运行目录，本任务没有复制。接入者也可按同一确认观感使用96×512图集，但必须保持UV、挂点、尺寸、节奏和三变体一致，并记录实际引用。

当前只读事实：`BatchMeteorLanding.java:50–58` 的陨岩直接地形碰撞，以及 `MeteoriteWreckageEntity.java:256–266` 的残骸真正接地，是两个已见调用链；仅破坏结果 EXECUTED 后各播一次。尚无其他通用攻击调用证据，保留为功能接入缺口，本次未修复或宣称泛化完成。新视觉不能绕开服务器开关、不可破坏规则、一次执行标记或追加伤害。预览只模拟坑口本身，没有模拟陨岩残留及两轮生物结算。

示意辅助：7×7×4平地方块、1.8格蓝灰人体、23块显隐组及 `preview_auxiliary_palette.aseprite/png` 全是预览辅助，不进游戏资源；没有把陨岩外形作为本次资源。Blockbench 坑块删除是演示模拟，不证明游戏已执行该删除、保护过滤、掉落、光照更新或性能表现。

接入回执：**未接入**，待用户确认同版二维、真实三维和上述参数后，由父会话安排 Grok。正式资源索引/制作表由各自唯一作者维护。本次没有更改 Java、src、运行资源、原v1、正式归档、制作表、共享记录或README；没有编译、打包、启动游戏或游戏验证。
