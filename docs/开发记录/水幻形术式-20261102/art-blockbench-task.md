模型 Cursor CLI --model grok-4.7-high-fast。阶段：水幻形候选v1实际Blockbench美术预览。你是本次唯一Blockbench操作者；主会话已完成Aseprite二维，后续不改这些输入。不是Java实现或正式复审，不编译，不测试，不游戏，不打包，不改表。

先读 C:/Users/baishang/.codex/skills/xueximod-art-preview/SKILL.md 和 references/visual-handoff.md。真实MCP工具本会话已有Blockbench；禁止称未安装，禁止手写离线bbmodel冒充展示。先查当前capabilities/get_project_info，保留已有iaido项目和其他所有用户内容；新建独立free项目，记录UUID，确认后续操作仍目标同一个项目。不关闭/覆盖别人项目。不操作Aseprite（你的会话没有工具）。主会话确认其他相关聊天当前idle；不得向“用 PS 绘制像素图标”发消息。

输入目录 D:/alianximod/docs/资源库/过程归档/水幻形_水线人形_v1-20261102/。先实际读图 water_phantom_2d_front.png、water_phantom_2d.gif或source首帧、water_flow_tile.png、water_drop.png；输入哈希见visual-inputs.json。二维主会话已看：96x128、32帧80ms，帧1–16水线人形维持，17瞬间提亮并开始破散，17–28断线坠成水滴，29–32水落地淡出。water_flow.aseprite是16x16、16帧80ms流水纹；water_flow_strip.png 16x256为原尺寸竖条；water_flow_tile.png第一帧。water_drop.png是16x16透明水滴。2D study是轮廓与节奏参考，不直接拿一张正面板当3D人形。

视觉要求：
- 内部通透的水线勾勒人形，没有实心蓝色身体、皮肤、衣服或僵尸纹理；不要变成贴着玩家的电光或完整水柱人。
- 对齐二维的圆润无脸头环、肩胸与腰部少量水流曲线、下垂手臂、分开的双腿；水线宽窄稍有变化，沿路径用实际Aseprite流水纹。避免整齐CAD盒子的12条直棱，不画密集网格。视角转到侧后仍有真实深度的环线/水丝，不能只有面向镜头的平面人形。
- 本候选计划主水线人体约1.8格高；可以自定例如32个模型单位为这版1.8格，但须按成品实际包围盒记录精确换算、外宽/深、轴向、原点。脚底为锚点，朝向冻结于生成。模型单位不能写成通用1单位1格。水滴溅出额外范围单列。
- 至少一个真实原生动画含维持、一次响应提示、分散下坠、地面水滴淡出。节奏与二维32x80ms对应，动画循环只是预览复播。水线断成较短片段再下落，不把整个人做成一个整体透明淡出，也不整体缩成一块蓝色板。触发后0.8–1秒左右可视收尾是候选观感，实际tick由最终交接/设计冻结。
- 响应术式从装配链指定，首批适配水系攻击。无需在这版设计新的攻击效果；若画一条出手弧线，只能作为标明的响应示意辅助，不将其当成固定水刃技能或运行资源。两触发形式共用这套破散：留置被接触/有效受击时反击；立即模式先施术再散。
- 加灰色人形/地面尺度参照，单独命名 auxiliary_*，可放旁边免遮水线；它们不进游戏资源。不要更改原iaido项目来复用假人。

操作必须实际调用Blockbench工具建模/导入贴图、原生Undo、原生动画、真实视口取图。可使用risky_eval调用真实Group/Mesh/Cube/Texture等原生API并正确Undo，禁止离线伪造截图。选择private offscreen view获得三分之四整体、侧/后视，以及破散中段和地面收尾。最低交付4张真实视口PNG，并让用户能看关键阶段。若能导出真实视口帧序列/GIF或视频更好，但不能用Aseprite图/离线拼贴冒充Blockbench动画。可以仅对真实视口帧做媒体编码，标明来源。没有某个操作的工具就读真实schema，不编造API。权限拒绝不得绕过；保存失败原话报告，不用另一手段绕过fs拒绝。

允许写：本输入目录中新增 water_phantom_v1.bbmodel、bb_*预览/帧/录制、scripts/bb_*脚本、visual-handoff-candidate.md、bb-preview-evidence.json；及新批 docs/开发记录/水幻形术式-20261102/art-blockbench-report.md。不得覆盖Aseprite源、2D PNG/GIF/纹理/visual-inputs.json；不得写正式资源库索引、src或Java。没有用户确认前全是过程候选，不能标正式采用。

交付：真实保存的bbmodel（可编辑），实际MCP视口文件、必要截图时间/动画keytime/项目UUID、输入纹理哈希与使用映射、模型尺寸和轴向、层级和局部世界关系、透明材质、动画相位/时间/预览复播说明、运行实现预期映射与辅助排除、Blockbench不能证明Forge伤害/触碰/网络效果。若工具截图只能返回base64，可通过已授权的Blockbench正常写文件接口保存原始视口数据；不得读写无关目录。记录每个预览真实来源，不生成假的MCP调用日志。

本单不单独开正式美术审查，不派其他代理，不跟踪他人源码。实际预览做好后一次性报告路径、外观及限制并停止写入，不擅接入游戏，不要求用户重说是否开工。主会话会展示具体版本并按技能集中请求视觉确认。
