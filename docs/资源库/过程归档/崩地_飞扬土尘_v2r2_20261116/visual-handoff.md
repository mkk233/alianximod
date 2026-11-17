# 崩地 v2r2：飞扬土尘候选交接

## 身份与采用边界

- 版本：`earth-ground-airborne-dust-v2r2-20261116`。
- batchId：`待制作-20261031`；taskId：`earth_break_ground_v2_art_preview`；attemptId：`20261116-art-ground-v2-04`。
- 作者：本次续派的唯一美术作者；沿用 Carbon/Argon 实际任务均返回 `Unknown model` 后的 `gpt-6-astra / max` 美术降级。不是 Google 已完成；没有重新派模型，没有下属。
- 直接父级：`01a1ce06-b681-7c90-b11d-260d3c169911`。
- 用户原话：“这个尘土效果不好没有飞扬的感觉”。旧版 `earth-ground-mouth-dust-v2-20261116` 已拒绝，不能接入或记录采用。本版是实质返修候选，仍待用户对本版二维、三维与参数共同确认。
- 本阶段额度已用完，返修结果未追加复审。设计 2/2、实现 3/3 不变；这次是制作和作者美术自检，不是正式复审。
- Blockbench 适用：有飞扬高度、抛物线、相机朝向、坑口遮挡和人体比例。已真实制作、原生保存、复开、截图；不是 Forge 验证。
- 写入仅在本候选目录和独立回执；未改 Java、运行资源、原 v1/v2、表、共享记录或索引。未编译、未打包、未启动游戏。

## 需求与协调后的表基线

`evidence/requirements-N-O-DV-current.json` 与同名 `.txt` 保存制作中第 11 行 N/O/DV 完整原文。`requirements-N-O-DV-previous.json` 保存前版原文；`preparation-input.json` 保存父方完整准备快照，不用本手册摘要替代原话。

本轮只读 SHA256 为 `D7681EAAD336B5E4719B9FFE21BEE2652D9EA74303C97E86119A1C0B1ADD3F56`，190956 字节。相对旧基线 `87F22C7DCE22FD0AB83F219C1D18938AF381D2286C106A62CEBCA1A6A56E9C75`，B/N/O 未变、DV 有追加；这是父方协调的唯一记录者追加用户拒绝反馈，时间 `2026-11-16 22:15:59.909+08:00`，不是美术作者改表。对应既有回执 `ground-v2r2-feedback-sheet-receipt-20261116.md`。视觉输入没有变化。

继续按已批准的“只有土尘，没有坑形贴图”：不加火焰、发光环、独立爆炸、永久地贴或额外伤害。魔力 2、冷却 5、复杂度 3、装配 2、等级上限 20 不动。2026-10-31 暂用二维原话不是本候选的用户确认。

## 同版交付绑定

以下路径相对于本目录；所有 SHA256、尺寸、帧数与时长见 `manifest.json`。

- `source/ground_airborne_v2r2.aseprite`：真实 Aseprite 144×48 RGBA、3 层、24 帧，每帧 50 ms，单次 1.2 秒。三列 48×48 变体，不是三个同时播放的巨大整张 billboard。
- `exports/ground_airborne_v2r2-atlas.png`：144×1152，3 列 × 24 行；`ground_airborne_v2r2-a/b/c-strip.png` 各 48×1152；`exports/frames/dust_01.png` 至 `dust_24.png` 为原尺寸透明逐帧。
- `source/visual_grit_v2r2.aseprite`：24×8、4 帧；`exports/visual_grit_v2r2-atlas.png` 为 24×32。三种土色、四个透明阶段。这 200 ms 源时间用于编辑查看，不是所有粗粒的飞行寿命。
- `previews/ground_airborne_v2r2-2d.gif`：864×288、最近邻 6 倍、24 个不同帧、1.2 秒。`2d-keyframes.png` 为保持像素纵横比的二维抽帧联系图，不是三维截图。
- `source/earth_ground_airborne_v2r2_preview.bbmodel`：真实原生模型，445 elements、279 groups、3 textures、1 条 2.3 秒原生动画。
- `previews/earth_ground_v2r2-overall-real-blockbench.gif`：真实整体视口，1280×668，2.3 秒；46 次原生采样，编码 36 帧，解码后 33 个不同画面。
- `previews/earth_ground_v2r2-side-real-blockbench.gif`：真实侧视口，1280×668，2.3 秒；46 次原生采样，编码 33 帧，解码后 30 个不同画面。
- `previews/overall-{before,launch,apex,fall,thin-tail,clear}.png` 和 `side-...png` 为实际关键时刻；`top-{before,apex,clear}.png` 为俯视。`reopened-native-viewport.png` 是保存文件原生复开后再次取景。
- `previews/overall-frames/`、`side-frames/` 保存全部原生 PNG。GIF 仅将这些真实 PNG 以固定深灰底承接透明并量化编码；没有离线重画几何、拼贴场景、生成图或假 GUI。
- `decoded-gif-*.png` 是最终 GIF 的解码自检图，不冒充新原生截图。`inspection-*.png` 是制作中取景试图，不属于用户确认绑定的最终镜头。
- `preview_auxiliary_palette.aseprite/png` 只读复制旧版地面/人体示意色板，新土尘与粗粒为重新绘制；辅助不进入运行资源。

## 返修的实际差异

旧候选是 11 团低位尘饼，运动根总升高仅 0.2375 格、0.8 秒。本次不沿用该观感：

- 重画为有明暗面、非规则轮廓、内部拆分和独立碎像素的土尘簇；不使用光滑烟云滤镜。
- 9 条不等距、不等高的尘流，出生分 0/50/100 ms 三相；大小 0.84–1.55 格，不排整齐一圈。
- 尘流运动根上升峰值为 0.56–1.64 格；纹理内部同时上提、拆散，而不是一张整团弹性缩放。
- 12 个独立粗土视觉颗粒，宽 0.07–0.16 格，飞行 0.65–1.10 秒，不同方向、不同抛物线，峰高 0.70–2.35 格（相对各自出生根）。最高粗粒中心约高于原地表 2.41 格。
- 细尘在粗粒回落时仍短暂悬浮、破散、透明衰减；没有回落后生成世界方块、掉落物或新增伤害。
- 候选总视觉预算改为 **1.4 秒 / 28 tick**，不是已批准运行参数。单簇贴图 1.2 秒，最晚出生延迟 0.1 秒；最后可见像素在事件后约 1.25 秒散尽，保留透明尾段。相对旧 0.8 秒增加 0.6 秒；不能悄悄沿用旧 16 tick 播放并声称相同效果。

## 坐标、锚点与坑形

- 换算：16 Blockbench 模型单位 = 1 游戏方块。+X 东、+Y 上、+Z 南。
- 主根 `ANCHOR_C_atCenterOf_impact_16units_per_block` 位于模型 `[0,0,0]`，对应世界 `C = Vec3.atCenterOf(impact)`，不是方块顶面。
- 撞击层方块为 `[-8,+8]³`；原地表顶面模型 Y=8，即 `C+(0,0.5,0)`。
- 每个尘流/粗粒运动根为 `C+(x,0.56,z)`，比原顶面高 **0.06 格**，模型 Y=8.96；所有 x/z、偏移与出生相位在 `evidence/motion-recipe.json`，是待确认候选接入参数。
- 细尘纹理逻辑锚点为每个 48×48 单元的 **(24,44)**。屏幕向右为纹理 +u，向下为 +v；卡片局部 X 范围 `[-size/2,+size/2]`，局部 Y 范围 `[-size/12,+11*size/12]`。透明留白不是实体体积；最初低位像素贴近坑口，后续升起，不把整张卡片中心埋在 C。
- 世界平移在 `_WORLD_motion` 父层，CAMERA_PLANE 只作用于子卡片朝向。旋转相机不应旋转世界轨迹。
- 卡片无厚度，只有 south 面，双面材质；不复制两个共面面片。卡片自身不弹性缩放；原生 scale=0/1 是逐帧显隐，不是尘团运动。
- 坑形严格使用 `r²≤4；d=0..2；2d≤4-r²；cap32`。模型记录正好 **23 个**删除单元：中心 3 块、r²=1 四列各 2、r²=2 四列各 2、r²=4 四列各 1。
- 半径是列中心半径 2，最深向下偏移 2，含撞击层共 3 层。最深删除单元底面模型 Y=-40，即 C.y-2.5；没有扩大玩法挖坑半径。
- `AUX_REMOVE_exact_23_cells_preview_simulation` 在时间轴事件时刻隐藏，只是预览模拟，不代表游戏破坏已验证。
- 7×7 平地与 `AUX_PERSON_1p8_blocks_not_runtime` 为辅助。人体高 28.8 模型单位，从地表到头顶 1.8 格；不算本术式资源。没有另造陨岩资源。

## 运动配方与时间轴

精确数组：`evidence/motion-recipe.json`。dust 字段顺序为 `[x,z,size,dx,dz,peakRise,delay,bank]`；grit 为 `[x,z,dx,dz,apexRise,flightSeconds,size,delay]`；均为游戏格/秒，bank=0/1/2。

细尘局部年龄与 `[水平进度,上升进度]`：

- 0.00：[0,0]；0.10：[0.15,0.46]；0.25：[0.38,0.88]。
- 0.40：[0.62,1]；0.65：[0.82,0.94]；0.90：[0.96,0.84]；1.20：[1.08,0.77]。
- 逐段线性插值。世界偏移为 `(dx*水平进度, peakRise*上升进度, dz*水平进度)`。峰值后轻微沉降，细尘主要靠像素拆散与 alpha 消失。
- 粗粒令 `u=age/flightSeconds`，世界偏移为 `(dx*u,4*apexRise*u*(1-u),dz*u)`。原生模型以 22 等分线性关键帧逼近该曲线；不是只把总根向上移。
- 粗粒局部旋转（index 从 0 起）最终为 `[90+13*index,145-5*index,35+11*index]` 度，按 u 线性旋转；仅预览视觉。
- 每粒最后 0.15 秒开始 alpha 255→160，最后 0.075 秒 alpha→64，寿命结束为 0；不留下任何块。不要将粒子模型当落地实体。

原生动画 `animation.earth_airborne_v2r2_preview_only`，20 fps 采样，演示 1×实际秒：

- 0.00–0.40 秒：平地等待；0.40 秒为模拟 EXECUTED 事件。
- 0.40–0.50 秒：坑形成，分相上冲，3→6→9 条细尘，4→9→12 颗粗粒。
- 0.775–0.95 秒附近：不同粗粒分别到顶；细尘根在 0.80–0.90 秒分别到峰，非同步扁平环。
- 1.00–1.50 秒：粗粒回落、分批淡出；细尘继续外散、悬浮破碎。
- 1.50 秒：已无粗粒，剩极淡细尘；1.65 秒：两类全部不可见。
- 1.65–2.30 秒：清楚保留真实坑形。GIF 循环回到平地只为对比演示；运行时是一次性效果，不能循环删块。
- 独立取景时刻：before=0.20、launch=0.50、apex=0.85、fall=1.20、thin-tail=1.50、clear=1.85 秒。

测到原生粗粒 01 的中心高于地面：0.40 秒 0.06 格、0.55 秒 1.0567、0.70 秒 1.6567、0.85 秒 1.86、1.00 秒 1.6567、1.15 秒 1.0567、1.30 秒回到 0.06。这是原生场景矩阵采样，不是仅用公式自报。

## UV、透明与渲染边界

- 细尘 atlas：bank 左右布局，时间从上到下。局部帧 `f=min(23,floor(age/0.05))`，UV 像素矩形 `[bank*48,f*48,(bank+1)*48,(f+1)*48]`；归一化除以 `[144,1152]`。最后一帧全透明。
- 细尘 3 个图层分别是淡尘、实体像素簇、飞散细点；末前帧最大 alpha=16，末帧 alpha=0。透明边缘保留 RGBA，不烘焙黑底，不做双线性平滑。
- 粗粒 atlas：每格 8×8，bank 三列，alpha 255/160/64/0 四行。每粒按上述寿命阶段取行，不按 200 ms 整段重复。
- 运行要求沿用 **DUST / CAMERA_PLANE、世界光照、非全亮、不幽冥染色**。没有新增“发光环”或 emissive 材质。
- 本版 9 个细尘卡片仍是相机平面。原生文件保存一个参考相机朝向，捕帧脚本在每个视角将卡片对齐真实相机；仅在 Blockbench 转动相机，不会自动运行 Minecraft 的 CAMERA_PLANE 逻辑。
- 粗粒在示意中是旋转的小长方体，显示空间抛落。若确认此体积表达，运行端需单独的纯视觉粒子/网格支持；**这不等于改变现有 DUST 标签的 CAMERA_PLANE 语义**。不能把整个 DUST 标签改成实体朝向或全亮来迁就示意。用二维粒子替代这些体积颗粒会改变本版观感，需记录差异而不是冒充一致。
- Blockbench 的原生透明 shader、深度排序和光照不是 Forge 等价验证。透明 PNG 的极低 alpha RGB 在某些查看器可显得偏亮；最终 GIF 按 alpha 落在固定灰底后，尾段为暗淡消散，并已解码查看。没有离线修改土尘颜色或添加光效。

## 运行接入差异与未完成项

当前 `BatchTriggerVisuals.dust` 是以 C 为点、halfSpan=1.25（完整 2.5 格）、16 tick 的单 BILLBOARD。它不是此候选的九尘流和十二抛落粗粒。本任务没有改 Java，因此新空间分布、1.4 秒预算、多相出生、24 帧 UV 和粗粒机制均**尚未接入**，不能只覆盖旧 PNG。

若本版获确认，确认版接入者再依据本手册实现纯视觉多粒子播放；运行路径可另定 `textures/effect/ground_dust_v2r2/`，本次没有创建该目录、复制 runtime 文件或占用正式资源名。辅助地面、人体和整个场景 `.bbmodel` 不进入游戏。

实际已知触发链仍仅是陨岩直接撞地 `BatchMeteorLanding:50–58` 与残骸真正接地 `MeteoriteWreckageEntity:256–266`；都只在 `breakExistingOnce` 返回 EXECUTED 后播一次。普通/其他通用攻击调用未有完成事实，作为功能接入缺口保留；本次不修、不宣称全部攻击适用已实现。

关闭统一地形破坏开关、不破坏不可破坏方块、禁止额外伤害等原需求不变。预览隐藏方块没有测试这些服务器规则。

## 原生证据、保留状态与下一步

- Aseprite 1.3.17 / API39，真实 MCP batch 绘制、保存、导出及只读复开成功；live.connected=false，不要求恢复 live。
- Blockbench desktop 5.2.2 / plugin1.10.1。新建 `937be075-0517-53f5-6aa9-c7f18ce35ab8`，原生保存后复开 `1aa7c01f-c3a9-2d54-e824-b3331f4094e9`；三纹理 error=0。证据见 `native-build.json`、`native-reopen.json`、`reopened-render.json`。
- 28 个开工前项目均仍在，名称和 saved 状态保留；包括旧 v2 和幽冥工程，不关闭或覆盖。旧 v2 全目录、原 v1 与运行 v1、协调后制作表合计 115 个保护文件 SHA 全部不变。
- 最终固定视口 `crop:false`，每个镜头相机不随帧重构图。整体与侧面全部对象 alpha 边界在画面内；完整时序的有效不同帧和 2.3 秒总时长已核验。见 `final-validation.json`、`gif-verification.json`。
- 发生过的工具错误与取景修正见 `evidence/tool-evidence.md`；没有绕过错误请求模型，没有 ComputerUse、headless Blockbench、ImageGen 或游戏测试。
- 功能：沿用既有状态且有上述缺口；美术：返修候选交付；用户确认：未获得；正式接入：未做。仍留制作中，不可据本回执迁待验收。
- 下一步：父方展示本版整体/侧面 GIF 与二维版本，等待用户同版确认。获得确认后再安排正式归档、接入及指定编译；不是现在接入被拒绝的 v2。
