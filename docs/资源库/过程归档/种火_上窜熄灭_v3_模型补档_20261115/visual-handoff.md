# 种火·上窜熄灭 v3 — 确认版模型补档

本次完成的是已确认 v3 的缺失 Blockbench 工程补档，不是新一轮设计、实现复审或新的运行接入。用户已授权“继续做种火的特效吧”，2026-11-11 已确认“按照这版接入”。不重画源图，不改变运行视觉参数。

## 身份、范围与确认

- batchId：待制作-20261031；taskId：fire_seed_sow_v3_model_repair；attemptId：20261115-art-seed-sow-05。
- 本执行上下文为唯一美术作者，未创建任何子代理。直接父级为本派发主会话，真实 ID 未暴露，记“未知”；本执行原生代理 ID、请求追踪 ID、实际服务模型未暴露。
- 据派发方明确提供的原生 spawn 结果：antigravity/gemini-4-carbon → Unknown model；antigravity/gemini-4-argon → Unknown model；按现行流程 11.1 降级至请求模型 gpt-6-astra、reasoning_effort=max。两次均不是 403，也不是额度问题。本执行未独立查询服务模型或 Fast 命中情况。
- 共享额度：设计 2/2、实现 3/3；**本阶段额度已用完，返修结果未追加复审**。以下仅是文件、像素、视口和可打开性检查。
- 只写本目录及指定独立回执。不写资源库共享 README、批次记录.md、工作簿或 src；不编译、不打包、不启动游戏。
- 父方转交的只读核对：制作中第7行，B=fire_seed_sow，N 为有效伤害命中后基础 10×炉心倍率产种、取消失败不生产、同真实释放同目标一次、不改变伤害、不附带引燃引爆、单独装配交给下次普通攻击；O 原文“命中处短暂火星，表示火种挂上，不另做一段持续燃烧。”DV 保留历史 v1 和既有额度，没有新增视觉修改。使用较新的 2026-11-11 v3 确认依据，不回退重画。该信息来自父方已关闭的 Flash 核对，本作者未读取或写回工作簿。

## 同版资源与真实预览

输入源：D:/alianximod/docs/资源库/特效/火焰/种火_上窜_v3/seed_sow_v3.aseprite
- 4754 B；SHA256：D514877D8AFFFFC6D5D54A9A6E64F46FBB12E41F1CB1C0C84A90C2B9129F1C0D。

确认导出：D:/alianximod/docs/资源库/特效/火焰/种火_上窜_v3/seed_sow_v3-strip.png
- 2776 B，64×1024；SHA256：A50EC7ED6A8434F50987A86036201DABFF3AB8A70CFF9D82A31CAC9F12B03EEE。
- 16 个 64×64 帧，均 50 ms；顺序播放一次共 0.8 s，第 16 帧全透明。
- 本目录 frames/frame_01.png 至 frame_16.png 为 Aseprite 只读批处理导出；逐帧 RGBA 与确认竖条对应区域完全相同。
- 02d-frames-as-read.png 为 Aseprite render_preview 返回的原始帧网格，已实际查看；非 Blockbench 证据。

Blockbench 适用：火焰存在身体相对位置、镜头朝向、尺寸与熄灭阶段关系。实际使用桌面版 5.2.2、MCP 插件 1.10.1 的原生 GUI 工程与 WebGL Preview；未使用 headless、图像生成或离线拼贴。

模型：本目录 seed_sow_v3_preview.bbmodel
- SHA256：CE7AC65C0406AE3A338FBB5CD7AE98E70D005A245520C7B4603245715DB9852C。
- 804998 B；free 格式，bbmodel format_version=5.0；634 个元素、17 个组、43 张内嵌贴图。
- 通过 Codecs.project.compile({editor_state:true}) 后以 Codecs.project.write(content, path) 保存，已补足旧脚本漏传的编译内容参数。
- 2026-11-15 22:06:38.950+08 从磁盘 JSON 重新加载到真实 Blockbench，获得不同工程对象；原工程 UUID 51b4a6ea-aa9f-f514-18cb-a1b63cc6d1bb，新工程 UUID 36ce2ac4-996f-33ad-0ae2-824f11da1f5c。随后查询确认 43/43 贴图就绪，无缺图错误。只关闭了本作者已保存的旧工程副本。
- 保存的主画面是 F08 静态参考；另有可切换可见性的阶段组。模型不包含持续播放机制。

四张 PNG 均为真实 Blockbench 原生视口直接输出，文字和网格也是场景内辅助对象，不是后期拼贴；已本人逐张 view_image 检查：
1. 01-hero-3quarter.png：1600×1100，三分之四视角，面法向随该镜头俯仰和方位。SHA256 9DAA1D183914082A3302BCA53D46BD1410DE01934FA0066F0499EF7F65CCC62A。
2. 02-front-billboard.png：1600×1100，正面核对贴图、1.8 格假人与 1.75 格参考面。SHA256 B8B1F9684518532E4210BF100A53BC86C616B4D62706E3E22BF5F3CEF4545E80。
3. 03-side-offset-thickness.png：1600×1100，冻结正面姿态后移观察相机，专门展示中心偏移及示意薄层。**这张不是宣称运行面会侧对镜头**；运行仍是 CAMERA_PLANE。SHA256 AE7DA9503884306FFBC89DB63DA95D338D1211AA05381D9CD4E8F152EEF5BE40。
4. 04-closeup-burnout-phases.png：2048×760，同一个真实场景中并排摆放 F01/F06/F10/F12/F15/F16 六个静态时相；不是六个同时出现的运行实例。SHA256 1894DF8AF97B4FE1BC66EF483B539A01D46CB2443536F56D46356AAA1F02FCFE。

## 空间与材质参数

16 模型单位 = 1 格。参考坐标 X 向右、Y 向上、未转动参考面的正向为 +Z；地面 Y=0。辅助假人总高 28.8u=1.8 格，脚底锚点 (0,0,0)，身体中心及公告板旋转中心 C=(0,14.4,0)。

参考面本地范围为 X=[-14,+14]、Y=[0.4,28.4]、Z=[1.22,1.34]，即 28×28u=1.75×1.75 格，示意厚度 0.12u=0.0075 格；中面 Z=1.28u=0.08 格。旋转后中面中心为 C + 1.28u × 镜头朝向法线，不是在身体前表面额外加 0.08 格。边框、中心点及测量辅助不参与运行。

必须区分：
- 1.8 格是本场景辅助假人的高度；1.75 格是历史确认展示面高，也是任务包给定的无目标参考正方形边长。
- 本次参考图采用无目标 1.75 格正方形与假人并置做尺寸对照，**不能据此将有目标的运行面固定为 1.75 格**。
- 运行有目标时 halfSpan=实体碰撞高/2，整面边长等于碰撞高度；无目标 halfSpan=0.875，整面边长 1.75 格。若目标碰撞高恰为 1.8 格，运行方形边长应为 1.8 格；本补档未把这一动态缩放仿真成运行验证。
- 旧 shot.js 另有 24×28u、z=2.35、y0 等临时取景变量。本次不将这些历史示意变量冒充当前运行参数，也不声称重现旧截图每个临时坐标。当前中心偏移、方形尺寸语义与朝向以本次任务包及正式说明为准。
- 0.12u 只用于可检查的 Blockbench 薄层，不是要求运行增加厚度或体积火焰。

主层级：01_REFERENCE_VIEW_1.75b_HISTORIC_SIZE 下放置假人、地面和 V3_CAMERA_PLANE_CENTER_0_14.4_0_OFFSET_1.28u；后者包含确认火焰面及边框。02_PHASE_COMPARISON_STATIC_SNAPSHOTS 为独立六阶段对照，平移间距仅为阅读需要。03_SIDE_MEASUREMENT_LABELS 是侧面说明辅助。

UV：各单帧均 64×64；正面 south 面 UV=[0,0,64,64]，north 面作相应水平镜像映射；东/西/上/下面不贴火焰。贴图 render_sides=double，材质使用输入 alpha，双面可见；未为火焰重配色、重绘或增加光晕。辅助假人的面已清空，仅留线框，以免预览遮住确认火焰。Blockbench 标准显示光照/透明排序不代表 Forge。

运行 CAMERA_PLANE 朝当前客户端镜头并包含俯仰，各玩家按自己的镜头观看。本模型保存的是取景时的姿态，不挂持续相机跟踪。主保存姿态旋转约 X=-13.10438°、Y=35.98068°、Z=0°；完整取景参数见 viewport-capture-evidence.json。

## 动画时相与边界

火从脚下燃起，向躯干与头部上窜，随后从下向上断开，分离为余烬并消失；不是飞弹，不常驻。F01=0–50ms 起火；F06=250–300ms 上窜；F10=450–500ms 断开；F12=550–600ms 上部消退；F15=700–750ms 余烬；F16=750–800ms 全透明。持续 16 ticks 的运行参数不变，20 TPS 下对应 0.8 秒。

交付动态证据为关键时相静态对照。附加单次播放尝试因 MCP/后台计时超时未完成，也不作为时长验证；主会话收束指示后已经作废该续行、恢复 F08，未交付任何持续播放机制。历史失败脚本仅留操作证据，并增加禁运行保护。没有以预览计时推断游戏时长。

不能由本预览代表的项目：实体碰撞高动态缩放、移动/插值与镜头逐帧跟随、Forge 深度遮挡/透明排序、幽冥等运行染色、实体材质/光照、多人相机差异、受击与伤害产种逻辑、真实 tick 调度。均未游戏实测。

## 操作证据与收束

Aseprite status 实际成功：1.3.17、batch ready、live.connected=false；get_sprite_info 因 hasAssociatedFile 字段兼容问题失败，随后同一 MCP run_lua 以 backend=batch、mutates=false、save=false 只读导出成功。未调用源文件保存。

本会话实际发现并成功调用了喜鹊聚合的 Blockbench 入口；独立 mcp__blockbench 入口也在工具目录中，未把名称差异误报为未加载。制作前 ModelProject.all=[]，没有其他工程需要关闭或覆盖。

两次截图/附加演示 risky_eval 返回“it didn't answer in 50s”，如实保留为超时；不能把该返回写成成功。第一次在恢复已有 GUI 窗口后原批处理完成并落盘，第二次已有四图落盘但附加演示未完成。用户 Escape 停止桌面控制后不再调用桌面工具；主会话收束指示后通过无计时循环的原生 MCP 保存/复开操作成功，续行指针作废，F08 恢复。私有视口清理工具另返回 onContextMenu is not defined，清理结果未知；没有因此重启应用、循环重试或改变模型。

inputs-before.json 与 verification.json：正式输入目录及旧过程目录共 65 个文件前后 SHA256 和大小全部一致；16 个新帧与正式竖条 RGBA 全等。reopen-validation.json、native-final-state.json 保存实际重开和贴图就绪证据；manifest.json 记录最终产物哈希。viewport-capture-evidence.json 标明最终图与初次超时批次数据的区分。

功能沿用现有记录；美术补档完成；用户同版确认沿用 2026-11-11；本次未做新运行接入、未改占位、未编译、未打包、未启动游戏、未实测。既有运行资源映射为 src/main/resources/assets/xueximod/textures/effect/seed_sow_v3/seed_sow.png 和 .mcmeta，仅引用；父方已核对其哈希，本作者未写入。父方提供的 2026-11-15 批次 compileJava 退出 0 是既有证据，不是本次执行。

写入已收束，无子代理。共享索引/工作簿仍由父方独立核对，本回执不单独宣称迁待验收或已完成。本次没有提出改变运行观感的新版本；若后续改变形状、位置、方向、比例、颜色或动画观感，须重新展示并确认受影响部分。

父方登记：真实美术代理 ID 为 `01a1df75-df60-73c3-b0c0-dd95a7ef14e8`（Helmholtz）。父方收到 final 后已 close。本执行上下文原文中的原生代理 ID 仍记未知，本段只补父方登记。

