# 引燃 v2 视觉交接

状态：`candidate_pending_user_confirmation`。版本：`fire_kindle_v2_20261116`。

Blockbench 适用：贴目标位置、比例、相机面朝向、遮挡和结束阶段均有空间关系。已经在真实 Blockbench Desktop 中建模、贴入同版二维图集、保存、从磁盘复开并取得原生视口截图。用户尚未确认本版本，未接入 Forge。

batchId `待制作-20261031`；taskId `fire_kindle_v2_art`；attemptId `20261116-art-kindle-01`。作者可观察 `CODEX_THREAD_ID=01a1e08a-817e-7b60-b465-7a8baaca1f4b`。直接父代理是本轮原生派发主会话，UUID 未暴露。

## 需求绑定

- 工作簿：`D:/alianximod/docs/术式制作表.xlsx`，制作中第 8 行，B=`fire_kindle`。
- 输入 SHA256：`1906BAABEBC490567AD9CDBAC250B17D2472264C2FB9E22BE97E264EF9175E54`；180549 字节；mtime `2026-11-15T22:23:02.096788+08:00`。结束时哈希未变。
- 完整 N/O/DV 原文：`D:/alianximod/docs/资源库/过程归档/引燃_贴身分段燃烧_v2_20261116/requirements-snapshot.json`，SHA256 `98F6D3F7EDB6D53BF524F75E38098C95DEFA7926B354BBAB4DDB587965D39020`。
- O 原文：“目标身上的持续火焰，按段亮一下，燃料耗尽或期限到了就熄灭。”DV 保留 2026-10-31 首版参数与 v1 暂用记录，没有本 v2 确认。
- 主会话派发约束（对既有用户决定的执行归纳，并非用户原话）见同目录 `request-addendum.md`。引燃属于火种附魔；组合命中后仍显示，不套用弹道规则的“组合隐藏”。此约束不构成用户对新候选版本的批准。
- 本阶段额度已用完，返修结果未追加复审。此次作者自检是素材与视口检查，不是设计裁决、代码复审或用户确认。

## 同版文件

根目录：`D:/alianximod/docs/资源库/过程归档/引燃_贴身分段燃烧_v2_20261116/`。完整文件清单、字节数和 SHA256 见同目录 `manifest.json`，每个文件都保持待确认状态。

- 可编辑源：`source/kindle_v2.aseprite`，48×64，RGBA，5 层、19 帧。SHA256 `DFC94355529B24120105BF093166EC22EC560B9E6C59B404127A971DDF1EBC05`。
- 原尺寸图集：`textures/kindle_v2_strip.png`，48×1216，19 帧自上而下，每帧 48×64。SHA256 `662675B80EF5A676C37D93DEA9DC618783D05FC8F32E006800BC0B2E2D1A1A97`。透明度只有 0/255。
- 独立关键帧位于 `textures/`：文件前缀 `kindle_v2_frame_`，编号 `01、06、11、13、14、16、18、19`，扩展名 `.png`；各完整绝对路径见 manifest。19 是全透明结束帧。
- 二维评审源：`source/kindle_v2_target_review.aseprite`，160×100，46 帧，含同一人形的普通色和现行幽冥乘色。
- 最近邻 GIF：`previews/kindle_v2_target_demo.gif`，640×400，46×50ms，总长 2.3 秒。SHA256 `B7BC1E27EA225F15A65B48B0C528215B48EA34DFCD9C07F20C82517536D65E0A`。
- 二维关键帧板：`previews/kindle_v2_keyframes.png`，960×648；上排 BASE F01、BASE F06、PULSE F11、PULSE F13，下排 END F14、END F16、END F18、OFF F19。
- 可编辑空间工程：`blockbench/fire_kindle_v2_candidate_preview.bbmodel`，22 个 cube、3 个 group、2 张贴图，默认正面 F11。SHA256 `FB2662BF19B839E7578DBE9CFAEA8796C54EF35F239F9C468540D53395BD9A68`。
- 真实整体斜视：`previews/gui_oblique_sustain.png`。SHA256 `389DF864B69DF780831AC79C0C072B18A74BD915A9570E2BDFE46D8EE6D961AD`。
- 真实正面短亮：`previews/gui_front_pulse.png`。SHA256 `82E461126F2FAB76778403C85910B207279C4F779D5FC330D261D36E2DDEEAAD`。
- 真实冻结侧距：`previews/gui_side_distance_frozen_plane.png`。SHA256 `F8483B710EACFF535EE3B2EF2AF3AAD35DBA7CEBECF163B6D6DB8CDC0257484E`。
- 真实熄灭/终止：`previews/gui_front_extinguish_F16.png`、`previews/gui_front_off_F19.png`。
- 复开后原生视口：`previews/gui_reopened_front.png`，与保存前正面图的字节 SHA256 相同。

三维交付是原生视口关键帧序列，分别展示持续、短亮、熄灭和终止。各视口是手动选取的演示阶段，没有游戏时钟；本次未录制三维连续动画。

`textures/kindle_v2_luminance_strip.png` 只作灰度结构诊断，不是幽冥替换贴图；`textures/aux_mannequin_palette.png` 只供示意假人。接入映射只指向彩色 `kindle_v2_strip.png`。

## 二维与播放参数

每帧 50ms，即常规 20 TPS 下的 1 tick。源文件标签：`sustain_loop` 为 F01–F10；`segment_pulse` 为 F11–F13；`extinguish_once` 为 F14–F19。运行中的帧下标按 0 起算分别是 0–9、10–12、13–18。

持续态循环 10 帧，底焰不会消失。每个真实的分段提示触发一次 3 帧短亮，然后返回仍在进行的持续态相位；不是同时叠绘两张全图。默认段间隔沿用 N 的 10 tick（0.5 秒），首跳沿用 DV 的 10 tick；启动本身不伪造首跳。终止时转入 6 帧、0.30 秒的视觉收束，F19 为空；这一尾帧不延长资源燃烧，不产生伤害。

GIF 的演示调度为：0–0.5 秒持续，0.5–0.65 秒短亮，1.0–1.15 秒短亮，1.5 秒开始收束，1.8–2.3 秒留空。它只说明视觉节奏，不表示实际施法持续 1.5 秒，也不保证伤害段数。实际持续、触发、终止来自已有资源燃烧状态；不由 GIF 时长推导余额、伤害或叠层，不新增施法上下文。

5 层依次为脚边底焰、身体中段火舌、高位侧火舌、上窜火星、短亮热芯。彩色图集已经合成全部层，运行单次采样一帧即可；不要求拆成 5 个运行面。火焰主要占侧身与腿部，脸部和胸腹中间留空，低/中/高形状错开。

UV：一张竖图，帧 i 的 V 区间 `[i/19,(i+1)/19]`、U `[0,1]`。建议最近邻，`blur=false`、`clamp=true`，无帧间混合；帧边使用半 texel 内缩时按整图 48×1216 计算。不要继续按旧的 16 帧或正方形单帧分帧。

## 空间参数

本示意 16 模型单位 = 1 游戏格；人形总高 28.8 单位 = 1.80 格。此数只用于本项参照。

- 世界参照：X 横向，Y 向上，+Z 为本假人正前方。地面 Y=0，身体中心 `(0,14.4,0)` 模型单位。
- 运行面是单个零厚度双面矩形，`KINDLE=CAMERA_PLANE` 保持。不是沿运动方向的交叉面，也不跟目标自身 yaw 固定。
- 正常体型面尺寸为 21.6×28.8 模型单位，即宽 1.35 格、高 1.80 格；原图像素等比，单像素 0.45 模型单位。目标高度 H 改变时以 `height=H, width=0.75H` 同比缩放。
- 原点/锚点为目标包围盒中心，面本地中心 `(0,0)`，每帧跟随目标插值位置。原图底端对应脚边，上端对应头顶；透明孔保留身体可见。
- 面朝向读取当前客户端相机完整四元数。令 n 是面朝镜头的单位法线，目标包围盒半尺寸为 `(hx,hy,hz)`，面中心向镜头的偏移为 `n × (abs(nx)hx + abs(ny)hy + abs(nz)hz + 0.04)` 格。本候选以 0.6×1.8×0.6 格包围盒参照制作；0.04 格是面相对朝镜头的包围盒支撑面的空隙。
- 正面冻结示意中 n=(0,0,1)，面 Z=5.44 单位=0.34 格，包围盒前界 Z=4.8 单位=0.30 格，间隔 0.64 单位=0.04 格。假人可见头部前表面 Z=3.15 单位，面距头部 2.29 单位=0.143125 格；躯干前表面 Z=1.8 单位，面距躯干 3.64 单位=0.2275 格。这些表面距离与包围盒距离分开记录。
- 斜视图已按当时相机调整面朝向及距离；精确相机坐标、面旋转角和偏移写在 `evidence/gui-captures.json`。侧距图故意冻结正面面片，再把观察相机移到侧方，便于检查间距；运行时从侧面观察也仍面朝镜头。
- `.bbmodel` 的 3 层级为目标辅助、候选面、地面辅助。辅助体都以 `AUX_` 开头，不进运行资源。

## 普通火焰与幽冥

正常颜色使用彩色图集；暖红外缘、橙黄中部、近白热芯。沿用当前火焰 `FULL_BRIGHT` 表现和 `NetherDomainTint.sample` 的查询语义；领域内顶点 RGB 为 `(90,160,255)`，领域外为 `(255,255,255)`。

二维 `kindle_v2_tint_comparison.png` 和 GIF 右栏是相同彩色像素逐通道乘上述 RGB 的结果。因此热芯偏蓝，暖色外缘变暗；没有把它伪装成新蓝色重绘版。不要改成一次命中永久染色，不另换领域判定。实际游戏着色仍待 Grok 接入和用户实测。

Blockbench 原生面片在截图时对候选材质关闭预览面向光照，表达现行 FULL_BRIGHT 意图；人物保留普通视口光照。此预览设置在 `scripts/render_gui.js` 中，未改变全局设置。模型复开默认光照与 Forge 全亮并不等价。

## 真实工具证据与限度

Aseprite 1.3.17 经喜鹊 MCP batch 执行，`live.connected=false`；没有声称实时 GUI 连接。源文件由 Aseprite Lua API 创建、保存和复读，PNG/GIF 由 Aseprite 生成。原图透明度为 0/255；最近邻预览没有平滑插值。

Blockbench Desktop 5.2.2/plugin 1.10.1，独立原生 MCP。初次本连接的 `ModelProject.all` 为空，未对种火或其他项目执行关闭/覆盖。新项目 UUID `41e5bed5-092c-5649-685e-72cee9618239`；保存后原生从磁盘复开为 `44d5b00b-8c00-0021-fca4-1e992906edad`，两张贴图均 loaded=true/error=0。只关闭了自己已保存的旧候选标签。

窗口被遮挡，整窗截图返回过旧欢迎页并明确提示 stale，未将该截图保存成候选证据。交付视口全部由已连接的桌面实例 `Preview.selected.screenshot` 即时 WebGL 渲染而得，不是 headless、生成图或离线拼贴。没有操作桌面、修改 MCP 配置、重启或恢复用户其他工程。

原生模型会重新编码嵌入 PNG，因此嵌入 PNG 文件字节哈希与 Aseprite 导出不同；manifest 同时记录两者哈希以及解码 RGBA 像素一致性。素材、空间工程、视口与需求快照绑定本版。

`get_sprite_info` 曾因桥接层访问 1.3.17 不存在的 `hasAssociatedFile` 字段失败；一次只读 `run_lua` 复读成功确认 48×64、19 帧、5 层、三组标签及全部 50ms。未重复探测或修改配置。

本轮已用 `view_image` 检查原 v1、v2 二维关键帧板、染色对照、真实斜视、正面、侧距、熄灭与复开图。未进行额外正式复审。

## 后续 Grok 接入边界

当前 `D:/alianximod/src/main/java/com/bslx/XueXiMod/carrier/BatchTriggerVisuals.java` 的 KINDLE 仍在命中点上方 1 格一次性生成 BILLBOARD，halfSpan=0.6、16 tick/旧 16 帧；当前没有本候选的目标跟随、持续循环、10 tick 分段短亮和结束收束。本美术交付没有改变这些代码。

用户确认本版后，由主会话的唯一记录者归档确认依据，再交 Grok：采用彩色图集、19 帧分组、矩形面尺寸、目标绑定及相机面参数，接到现有真实燃烧状态。拟运行路径为 `D:/alianximod/src/main/resources/assets/xueximod/textures/effect/kindle_v2/kindle.png`，对应本目录 `textures/kindle_v2_strip.png`；这只是计划路径，当前没有创建或写入。不要把整条 19 帧自动循环成一次性动画，也不要只替换旧 PNG 后宣称持续行为已实现。

本候选不改变火种余额、伤害、叠层、同 tick 分配或施法上下文。组合命中正常展示；n<1 没有资源燃烧层时不凭此图补造燃烧。陨石组合仍只跟随已批准的热波子段挂层。

正式接入：未做。用户确认：未获。编译：本轮未运行。游戏：未运行、未验证。迁待验收：不满足；本项继续制作中，本回执不能替代确认、接入、最终编译或写表证据。
