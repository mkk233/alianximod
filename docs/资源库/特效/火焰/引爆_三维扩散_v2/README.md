# 引爆 v2-3D — 用户确认采用版

## 确认与当前状态

- 美术采用状态：`approved_for_adoption`（用户确认，已正式归档）。
- 用户原话：**“可以先采用这一版”**。
- 确认日期：**2026-11-16，Asia/Shanghai**。本轮由直接父会话`01a1ce06-b681-7c90-b11d-260d3c169911`转达，不编造更细的确认时间戳。
- 绑定的已展示版本：`fire_detonate_v2_3d_candidate_20261116`，对应真实Blockbench主视角和侧视角完整GIF。该字符串是原始版本标识，保留以便追溯，**不代表本正式归档仍待确认**。
- 实施状态：**接入进行中；未编译、未游戏验证**。接入者Harvey：`01a1e377-93fa-7cc3-ac7f-4bd275c32916`。美术归档不代替接入完成、编译或迁表回执。
- batchId：`待制作-20261031`；taskId：`fire_detonate_v2_art`；本次attemptId：`20261116-art-detonate-3d-archive-02`。
- 设计2/2、实现3/3不变。本阶段额度已用完，返修结果未追加复审。本次只做确认版归档，不重画、不重审、不再次启动美术MCP。

## 资产入口

- 可编辑源：`source/explosion_puff_orange.aseprite`、`source/explosion_puff_soul.aseprite`。
- 原尺寸导出：`textures/explosion_puff_orange-strip.png`、`textures/explosion_puff_soul-strip.png`。
- 二维关键帧：`previews/puff_keyframes.png`。
- 确认绑定的真实动态预览：`previews/detonate_v2_3d_main.gif`、`previews/detonate_v2_3d_side.gif`。
- 同版强度和幽冥蓝：`previews/detonate_v2_3d_strength.gif`、`previews/detonate_v2_3d_soul.gif`。
- 真实GUI关键视口：`previews/gui_peak_threequarter.png`、`gui_peak_side_top.png`、`gui_breakup_threequarter.png`、`gui_peak_soul.png`；强度PNG为`previews/strength_comparison_detail.png`。
- 可编辑模型：`blockbench/fire_detonate_v2_3d_candidate.bbmodel`。
- 正式交接：`visual-handoff.md`；全部哈希/尺寸/确认与接入状态：`manifest.json`；逐文件候选一致性：`provenance/archive-verification.json`。

模型是**第7帧峰值的可编辑姿态**，含183Cube、17Group、16张嵌入纹理，并不是带原生Animation轨道的动画工程。真实动态预览是在GUI中逐帧更新各团的三维位置、朝向和UV后截图编码，不能把静态模型误称为直接可加载的游戏动画。整个示意模型、假人、文字及地面不进入运行资源。

模型文件名、内部工程名和历史credit仍可能包含`candidate`或旧待确认文字；为保持用户确认的模型字节不变，**没有改写这些内嵌字段**。本次采用状态以本README、正式交接和正式manifest为准，历史字段不覆盖2026-11-16的用户确认。

## 确认绑定SHA256

- 主视角GIF：`054C4B5F8363DA115F0A9472DCADE60F9E7220E869974CE8BBDCED3DEB80E98B`
- 侧视角GIF：`179EF78C79B65A637CEF58251EBD3C7273BDB553A4099D84DA7B939251B1B971`
- 橙色Aseprite：`0C3EC19FE36D420F9D909B57AEFBB059D29739E9883D9ED77795B9EF657DBC86`
- 蓝色Aseprite：`EADC6C6DD861261C65A54A0A09C678C2F36C55DF79F5E9E4ECA8B93F4A8C4F73`
- 橙色strip：`28666B39C8A80E0A18EB04DDAE7F7422A546B179307DB0990F7F8109ADF8D0F7`
- 蓝色strip：`C4B656B506B6CB721BCEF39CFED9BA99AA0AE21F2EDED7839057FC3573835856`
- 模型：`000F4370871D97886DAF2BB8411AAAB417847A62D3807E6CAD9786C4D0C26E0E`

以上源、导出、模型以及归档预览均直接复制并逐字节比较，未重新编码、重绘、改色或改空间参数。正式目录的文档与manifest为本次新增的确认状态记录，不伪称与历史文档字节相同。

## 来源与过程证据

原始已收束候选目录：
`D:/alianximod/docs/资源库/过程归档/引爆_三维扩散爆炸_v2_20261116/`

历史目录保持不变。原始32张像素帧、80张GUI帧、制作/捕获/复开脚本和原始工具错误记录继续从该目录读取，不在正式目录重复复制全部过程文件。详见`visual-handoff.md`中的过程链接。`provenance/candidate-manifest.json`是原候选manifest的字节不变历史副本，其旧状态仅描述制作当时。

本次作者没有写src、共享README索引、制作表、批次记录，也没有调用Gradle或美术MCP，所有既有用户Blockbench工程保持不动。Harvey接入后比对本目录与运行贴图字节；Rawls单独维护共享索引/表/记录。
