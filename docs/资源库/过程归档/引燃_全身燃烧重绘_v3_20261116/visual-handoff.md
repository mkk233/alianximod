# 引燃 v3 全身燃烧重绘：视觉交接

状态：`candidate_pending_user_confirmation`。版本：`fire_kindle_v3_redraw_20261116`。
batchId=`待制作-20261031`；taskId=`fire_kindle_v2_art`；attemptId=`20261116-art-kindle-revision-03`。
作者可观察 `CODEX_THREAD_ID=01a1e08a-817e-7b60-b465-7a8baaca1f4b`。直接父级为本次派发主会话；其任务包提供的可观察会话为 `01a1ce06-b681-7c90-b11d-260d3c169911`，不将该值冒称未知的父代理 UUID。

## 版本和输入

旧稿 `fire_kindle_v2_20261116` 已被用户拒绝，未采用；本次没有覆盖旧稿。用户逐字反馈与主会话执行归纳分开记在本目录 `request-addendum.md`。重画授权不等于用户确认本 v3。

完整历史 N/O/DV 快照仍引用：
`D:/alianximod/docs/资源库/过程归档/引燃_贴身分段燃烧_v2_20261116/requirements-snapshot.json`，
SHA256 `98F6D3F7EDB6D53BF524F75E38098C95DEFA7926B354BBAB4DDB587965D39020`。未改该快照，也没有伪称重新提取最新 N/O/DV。
开工时工作簿 `D:/alianximod/docs/术式制作表.xlsx` 的只读 SHA256 为 `6F3EE745CC776B340A3D166DF8799D3E7B859BF48354D46073A396D2406F1496`，与本次任务包一致；未写表或重算。收束时另读得 `744A693B267A357D0CFF1D3F57598C01130B2C5559A9FAAA433C60DC2A115679`（180915 字节），本作者没有写表，变化原因未调查；交主会话唯一记录者核对，不宣称哈希未变或末次原文已重新提取。
O 保持“目标身上的持续火焰，按段亮一下，燃料耗尽或期限到了就熄灭。”

本阶段额度已用完，返修结果未追加复审。作者检查图像、文件和真实视口，不是正式裁决、复审、用户选稿确认或游戏验证。

## 本版视觉内容

不是旧细带加粗：新绘制宽短分叉火团、错相生长和上升破裂，叠在脚边、腿前、胸腹与肩侧；胸腹有连续火面并以断续透明裂口露出身体，不再固定整块空白。暖红局部、橙色外火、黄色热芯和小面积浅亮点采用硬边像素、二值透明。脸部及部分身体仍可辨识。没有新增圆环、爆炸、死亡、倒地或动作。

所谓燃烧体积是像素明暗与翻卷轮廓形成的视觉体积；运行几何仍是单个 CAMERA_PLANE，不把示意声称为真实三维体积火焰。

## 同版文件及展示顺序

根目录：`D:/alianximod/docs/资源库/过程归档/引燃_全身燃烧重绘_v3_20261116/`。manifest 中每项文件都标记待确认，记录字节大小和 SHA256；源、图集、模型、真实视口与输入绑定本版。

1. 原尺寸阅读动画：`previews/kindle_v3_target_native.gif`，160×96，目标高 64 像素；左普通色，右现行幽冥乘色。
2. 最近邻放大动画：`previews/kindle_v3_target_demo.gif`，640×384，前者 4 倍。两者同样 46 帧、每帧 50ms、演示总长 2.3 秒。
3. 原尺寸静态：`previews/kindle_v3_reading_scale.png`，160×96；`previews/kindle_v3_keyframes_native.png`，320×96。
4. 关键帧板：`previews/kindle_v3_keyframes.png`，960×288，3 倍最近邻。从左到右为 F01 持续、F06 不同生长相位、F11 短亮、F16 熄灭中段；底部短线仅为 1–4 顺序标记，不是燃料或时长 UI。
5. 真实斜视：`previews/gui_oblique_sustain.png`；正面：`previews/gui_front_pulse.png`；冻结侧距：`previews/gui_side_distance_frozen_plane.png`。
6. 真实结束阶段：`previews/gui_front_extinguish_F16.png`、`previews/gui_front_off_F19.png`；磁盘复开后：`previews/gui_reopened_front.png`。

原尺寸是屏幕阅读参照，不是 Forge 截图；实际屏幕尺寸取决于游戏距离、分辨率和 FOV，不声称 64 像素就是所有游戏条件。

二维可编辑源 `source/kindle_v3.aseprite`：48×64，4 层，19 帧。SHA256 `3EC04641BBBC573BAAABCF088DA61B6B79B7418159C2D1F6F3287D5B8B06BB42`。
目标合成可编辑源 `source/kindle_v3_target_review.aseprite`：160×96，46 帧。
原尺寸透明竖图 `textures/kindle_v3_strip.png`：48×1216，SHA256 `EF3476EB01225D2E7478F9BBA5430F28BCF2E1D12198F3F482A634C155A04070`。alpha 只有 0/255，F19 全透明。
单帧原图为 `textures/kindle_v3_F01.png`、`kindle_v3_F06.png`、`kindle_v3_F11.png`、`kindle_v3_F16.png`、`kindle_v3_F19.png`。
真实工程 `blockbench/fire_kindle_v3_redraw_candidate_preview.bbmodel`，SHA256 `3C91724997E889FD58899959E2B871B311AFAF9695D42794EC91690329D55EBC`。

## 接入参数：沿用既定候选，不新增数值方案

本次只重画像素及轮廓，不重设既定尺寸、锚点、朝向、帧数或时序；这些候选数值同样随 v3 等用户确认，旧稿被拒绝不意味着其参数已经被批准。

- 图集：19 帧竖排，单帧 48×64。第 i 帧（0 起算）U=[0,1]、V=[i/19,(i+1)/19]；最近邻、无帧间平滑，clamp=true、blur=false。
- F01–F10 / 索引 0–9：持续循环。F11–F13 / 10–12：每个真实分段的短亮，一次 150ms，再回仍在进行的持续相位；不叠绘两张全幅图。F14–F19 / 13–18：结束时一次 300ms 收束，末帧空白。
- 每帧 50ms（正常 20 TPS 下 1 tick），分段提示默认间隔沿用 10 tick；首次分段提示不提前伪造。时序由现有真实燃烧状态驱动，不用动画创建伤害上下文。
- GIF 演示：0–0.5 秒持续，0.5–0.65 和 1.0–1.15 秒短亮，1.5 秒开始结束，1.8–2.3 秒空白。只是演示时间，不是施法时长、余额保证或伤害段数。
- 16 模型单位=1 游戏格。正常参照高 28.8 单位=1.80 格，包围盒参照 0.6×1.8×0.6 格。面宽 21.6 单位=1.35 格、高 28.8 单位=1.8 格；目标高 H 时 height=H、width=0.75H。
- 单个零厚度双面矩形，`KINDLE=CAMERA_PLANE`，读取相机完整四元数。不是弹道十字，也不固定跟目标 yaw。模型 X 横、Y 上、+Z 假人正前。
- 锚点是插值后的目标包围盒中心；正面参照中心 `(0,14.4,0)` 模型单位。令 n 为朝镜头法线、包围盒半尺寸 `(hx,hy,hz)`，中心向镜头偏移 `n × (abs(nx)hx + abs(ny)hy + abs(nz)hz + 0.04)` 格。
- 正面冻结面 Z=5.44 单位=.34 格；包围盒前界 Z=4.8 单位=.30 格；空隙 .64 单位=.04 格。可见假人头前界 Z=3.15，距离 2.29 单位=.143125 格；躯干前界 Z=1.8，距离 3.64 单位=.2275 格。不能混同模型表面距离与包围盒距离。
- 冻结侧距图只为看间距：固定正面火面，然后转动观察相机。正常运行从侧面观察也依旧面朝镜头，不以侧距图中的薄片外观代替动态朝向。
- 普通顶点颜色 `(255,255,255)`；幽冥沿用现行 `NetherDomainTint.sample` 的 `(90,160,255)` 逐通道乘色。右栏实际呈现该乘色，不偷换成另画的蓝火，不更改领域查询语义。暖色纹理乘蓝色不一定得到纯蓝火。
- 预览火材质关闭面向明暗，表达 FULL_BRIGHT 意图；不改变全局设置。所有像素仍不透明或全透明。实际 Forge 遮挡、亮度与领域着色需接入后实测。

组合命中照常显示；“单独装配预览、组合隐藏”不适用于本附魔。火种/余额/伤害/叠层、真实释放去重、同 tick 争用与目标动作均不改。n<1 未挂层时，不凭美术补造持续燃烧。300ms 尾焰不延长伤害或资源状态。

## 真实工具及保护证据

喜鹊 `aseprite__status` 成功：Aseprite 1.3.17，batch ready，live.connected=true。本次用隔离 batch 执行新绘制 Lua，未编辑或关闭用户 live 文档；没有声称实际走了 live 绘制。源、原图、GIF、关键帧全部由 Aseprite API 导出。源只读复开验证 48×64、4 层、19 帧、全部 50ms 和三组标签。

独立 Blockbench MCP 成功：Desktop 5.2.2/plugin 1.10.1。新建项目 UUID `c136d4c5-fe9b-4cf8-f91c-3df017448ec0`，22 cube、3 group、2 texture；原生保存后从磁盘 `Codecs.project.load` 复开 UUID `79873578-7708-0ebd-5183-eaedd686d11b`，两张贴图 complete=true/error=0。保留自己原标签和复开标签，未关闭任何用户文档。

用户未保存 `daozhang_v1_candidate` UUID `4f04348e-9d85-4d4f-0c95-9e5e50c4dbe2` 在前后都为 saved=false、38 elements/1 texture；旧 v2 标签仍 saved=true、22 elements/2 texture。只操作 UUID 严格校验的新候选。

实际视口取自已连接桌面实例主 `Preview.selected.screenshot` 即时 WebGL 渲染，900×700；不是 headless、离线拼图或生成图，也没有桌面控制。窗口可见性详记 `evidence/gui-captures.json`；这不是整窗截图或游戏截图。阶段序列展示持续、短亮、结束与终止，未录连续三维动画。

`.bbmodel` 嵌入 PNG 经 Blockbench 重新编码，字节 SHA 不同；解码 RGBA 与两张 Aseprite 导出逐像素完全一致，另记 manifest。复开正面图与保存前正面图 SHA 一致。

已用 view_image 看过关键帧板、原尺寸阅读图、真实斜视/正面/冻结侧距/熄灭及复开图。RA2 视频页面抓取失败，未观看实际燃烧帧；其元信息仅来自派发者，不声称已实看或逐帧复刻。未下载/转码排查，无素材复制。

## 未做与后续边界

当前运行仍按前置事实是旧 KINDLE 命中点+1Y、halfSpan .6、16 tick/16 帧一次性 BILLBOARD；本次未改也未重新复审 Java。v3 所示目标跟随、持续循环、分段短亮、结束收束尚待确认后由 Grok 接入，不是只换 PNG 即完成。

计划确认版图集映射：本目录 `textures/kindle_v3_strip.png` → `D:/alianximod/src/main/resources/assets/xueximod/textures/effect/kindle_v3/kindle.png`。目标路径尚未创建；不能未获确认就接入。本模型及 `AUX_` 假人、地面、辅助配色不进运行资源。

功能：本次未改；美术：v3 候选制作和实际预览完成；用户确认：未获；正式接入：未做；编译、打包、游戏：未运行。本项不满足迁待验收；由主会话展示同版文件，确认后另派接入及唯一记录者同步。
