# 燃烧：按用户已确认的真实 Blockbench 版本接入

固定 Cursor CLI --model grok-4.7-high-fast。当前阶段：单作者实现、资源转换与必要的作者自查，交付后停止写入。此阶段禁止 Gradle，随后由调度单独授权最终统一编译。主会话不会审代码，用户已明确“检查完全交给cursor”。不再派 Codex 或新会话做正式复审。

## 已有授权与输入

用户报告燃烧第一、第三人称都明显与原稿不同，要求先做真实 Blockbench 示意给 Cursor 实现。用户明确选择“整团火焰随手臂一起转动”，并在看到真实正背面动画后于 2026-11-01 确认：“可以就这版接入”。空间确认已完成，不要重复询问、不要因为历史文件标题“待确认”阻塞接入。

读 D:/alianximod/AGENTS.md、.grok/skills/max-dispatch/SKILL.md、docs/统一开发与术式批量制作流程.md 与 C:/Users/baishang/.codex/skills/xueximod-art-preview/SKILL.md。遵守当次最新用户分工：Cursor 完成实现、检查及之后的编译，Codex 只调度转达。默认不打包、不启动或操作游戏。

必须完整阅读以下输入，查看现有预览并解析实际 bbmodel，不能只看图片猜几何：
- D:/alianximod/docs/资源库/过程归档/燃烧v3_Blockbench挂载示意-20261101/视觉交接_待确认.md
- 同目录 manifest-pending.json、native-mount-parameters.json、燃烧v3_空间挂载示意_v1.bbmodel、burn-v3-spatial-parts.aseprite、burn-v3-*-strip.png、bb-front-animation.gif、bb-back-animation.gif、bb-side-depth.png、bb-hand-held-detail.png。
- D:/alianximod/docs/开发记录/燃烧视觉偏差排查-20261101/Cursor原因报告.md：这是 Cursor 只读根因报告，不是当前设计。若旧建议与已确认模型冲突，以模型和视觉交接为准。
- D:/alianximod/docs/资源库/特效/火焰/燃烧_双手附焰与火焰披风_v3/：二维原件、5层入场和维持。
- 制作表 D:/alianximod/docs/术式制作表.xlsx 中 B=fire_burn 的当前完整 N/O/DV 及相应资源明细，只读，以当前行而非历史预览文案为逻辑依据；本次只修视觉，不动游戏机制。
- D:/alianximod/docs/开发记录/炉心升温燃烧视觉接入-20261031/调度记录.md：旧作者、检查者与 writer 均已结束，历史 Pre/Post 判断已撤回，勿重引。

## 批准内容与实现要求

1. 使用实际模型全部特效网格、UV、分件及骨骼挂点。灰人偶、持物杆以及所有“示意辅助”不进入运行资源。模型中的抬手动画只验证挂载，不得让玩家强制播放演示动作。
2. 全团手焰及拳头火星在第三人称、真实第一人称空手/持剑/法杖等持物状态都跟随当前手臂、持物、攻击姿态；不要改成世界向上。手焰两层弯曲壳与拳端亮核按模型，禁止恢复三张重复正面大贴片。左右手各自跟随对应骨骼。广角、苗条臂等尚无游戏实测，不得报告已视觉验证。
3. 披风按确认模型的实际向后弯折与浅褶顶点接入，保持上背根部、长度、宽度及脚边下摆，不退化成一张平面。肩腹火簇随 body，髋腿随对应腿；保持火簇完整。身体火星与手部火星分离。
4. 用本机 Forge 1.20.1-47.3.33 的实际 API 修复第一人称持物漏画。不使用不存在的 RenderArmEvent.Pre/Post。不能只依赖仅空手/地图路径触发的 RenderArmEvent；不要取消原版持物、重复画原版手/物品或重复叠火。第一人称显示对应手焰与拳头火星，不把全身披风硬塞到屏幕。执行层面的适配需以实际 API 为证据。
5. 20帧×80ms持续，8帧×60ms已有入场继续沿用同套挂点映射。入场若需拆层，仅允许使用实际 Aseprite（现有 D:/Aseprite/Aseprite.exe 或已暴露MCP）无损分离/导出已有素材，保留可编辑源与转换脚本；禁止 AI 生图、重画设计或改变批准颜色轮廓。纯几何/运行格式转换可脚本执行。已批准持续源和预览必须原样归档，不改变模型形状或动画观感。
6. 使用真实 BB→Minecraft 坐标/局部挂点换算，处理骨骼原点及父子变换，不能继续用错误的 body-width 一把缩放。模型坐标脚0肩24头32，正面+Z；Minecraft坐标换算、0.9375等见交接。苗条臂可按真实骨骼作必要挂点适配，但不得擅自改外观设计。
7. 透明 emissive 双面几何各面只提交一次，避免反绕序重复叠色。最近邻，不插帧，不烘焙光照。保持原透明像素与像素色值。
8. 只修 burn 视觉；升温 v2、冷却、炉心数值/能量/伤害/持续/配置、服务端与其他术式保持当前行为。保留所有已有未提交改动，禁止 reset/clean/checkout 覆盖。

## 文件所有权

你是本次唯一源码/运行资源作者。可读当前要求涉及的客户端、实际 Forge/Minecraft 缓存 API、必要构建/注册配置。不要通读无关整仓。

允许写：
- D:/alianximod/src/main/java/com/bslx/XueXiMod/client/furnace/（现有炉心视觉类与本次必要的新客户端帮助类）
- D:/alianximod/src/main/java/com/bslx/XueXiMod/client/FireVisualClient.java（仅必要的燃烧视觉接入，保留升温冷却及其他行为）
- D:/alianximod/src/main/resources/assets/xueximod/textures/effect/furnace_v2/（仅燃烧所需资源，不改变 heat/cool）
- D:/alianximod/src/main/resources/assets/xueximod/textures/effect/burn_spatial_v1/（建议新增明确版本目录）
- D:/alianximod/src/main/resources/assets/xueximod/models/effect/burn_spatial_v1/（如使用经转换的运行数据，只包含FX）
- D:/alianximod/docs/资源库/特效/火焰/燃烧_空间挂载_v1/（本次正式归档）
- D:/alianximod/docs/资源库/过程归档/燃烧空间挂载接入-20261101/（转换过程、无损拆分脚本等）
- D:/alianximod/docs/开发记录/燃烧空间挂载接入-20261101/ 下你自己的 implementation-report.md、implementation-result.json、implementation-ownership.json、index-update.md 与必要 author 输出。

需要范围外源码文件（如 mixin 配置）时，先报告具体最小必要范围，不自行修改。禁止写制作表、资源库总索引、其他批记录与批准原始草稿。相关旧作者已收束；开工时核对有无正在写同文件的作者，遇冲突停写报告。

## 正式归档

用户已确认 burn-v3-spatial-v1。将同版 Aseprite源、15个原尺寸分件条（按实际文件名）、实际bbmodel、native-mount-parameters、最终正背GIF/侧面/持物近景/实际app proof、视觉交接及hash 复制到正式目录 燃烧_空间挂载_v1，保留原件。原始帧可留过程目录并在README引用；不复制旧 draft 预览作为正式稿。更新正式副本的交接状态及manifest：批准原话、日期、同版原始hash、实际运行资源对应关系。原始二维5层和入场若正式目录引用，要链接准确或复制，保留来源。对派生入场等写清无损转换依据。

写出 index-update.md 给唯一 writer 合并资源库索引；自己不要写 docs/资源库/README.md。当前正式状态写“用户已确认，已接入源码，待最终编译与游戏实测”，不能提前写编译通过或已验收。

## 检查与交付

依用户授权，你负责必要的实现自查：实际API、双手/持物/空手/地图避免重复、模型与运行几何/UV/挂点的对应、纹理帧时长和入场映射、辅助排除、heat/cool 保留、透明面一次提交、资源引用与文件匹配。不另开外部评审或以新会话重置既有校验计数；Codex不会做正式复审。不要制造游戏效果已验证的结论。

完成后停写，报告 implementation-report.md 和 implementation-result.json（status、session_id、model、changed_paths、approved_asset_version、archive_path、runtime_resources、checks_run、unverified、needs_compile:true、source_writes_closed:true、blockers）。附精简路径行号及如何实现第一人称实际姿态、与示意仍存在的边界。若未能按确认版实现，明确未完成而非偷偷换版。当前阶段不编译，最终反馈后等待单独编译任务。
