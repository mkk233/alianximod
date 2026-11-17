# 水幻形v2.1确认后的最终接入
用户最新：“这个版本好接入吧”。这是承接上一轮明确的v2.1散水预览确认，已批准采用并接入，不再要求常规开工或视觉重复确认。
固定Cursor CLI `--model grok-4.7-high-fast`，不降档、不spawn。设计2/2已用完，不新开设计复审；实现当前2/3，待本轮所有写入收束后集中第3轮。你本次实施，不自行开独立正式审查。不改无关系统，不重做已裁决机制。已有未提交修改全部保留，禁止reset/clean/checkout覆盖。
完整读AGENTS、max-dispatch、现行开发流程及配置指南，本批用户决定、implementation-contract、design-round-2-carrier、详细设计。阅读相关implementation报告时注意它们是初次记录，**以当前修复源码以及fix-r1-a/c/d、review-nonvisual-r2、fix-r2-d、fix-compile-a/c和compile-nonvisual-report为最新状态**，不要恢复旧bug。非视觉代码已compileJava通过，当前没有源码作者仍在写。
不运行Gradle、build/jar/reobf、runClient或游戏，不改制作表，不联系其它聊天。默认最终统一编译由另一个指定Cursor在你交回后执行。可运行命名门禁，仅修自己修改的文件。最终一次性报告准确路径/行为/配置与运行资源映射/未跑项/真实阻塞/停止写入；只等最终反馈。

## D作者：确认版归档、Steve水线与连续散水接入
唯一写入：
- client/CloneRenderer、CloneOutlineVisual、CloneAfterimagePlayback/Snapshot及本效果必要新增客户端模型/动画数据类；network/ClonePresentationS2CPacket、ModNetwork、XueXiMod只在本表现接线确有需要时最小改；
- src/main/resources/assets/xueximod/ 下本效果纹理及必要精简运行模型/动画数据（不改lang，它归C）；
- 正式归档目录 docs/资源库/特效/水流/水幻形_Steve水线_v2_1/；docs/资源库/README.md由你唯一更新；本批integrate-d-report.md与runtime-visual-mapping.json；
- A作者已停写，本次你可接管clone/core/CloneEntity.java中**必要只读表现标识同步/访问器**的最小接线，不能改受击/碰撞/租约/付款逻辑；若现有UUID已是deploymentId就直接用，别增加无用同步。其它core和B/C文件不改，超范围必要依赖明确交回。
素材：只读 docs/资源库/过程归档/水幻形_Steve水线_v2_1_流畅散水-20261102/ 的最终模型、两张贴图、预览、visual-handoff-candidate、bb-preview-evidence，按本批visual-candidate-v21-manifest.json核对。模型SHA256 4ADFCC75ED1E06E62ECE5FD05E6B109787FB1AC46B29CBFC5A8AF8C5425695F7；两贴图9E7E5B01A7E09C393D3FFA1800468BD61AE919F68CF628152B00D9C347793DA5 / CEFDEE9E3E3FB7F701E19FC9CB3438C586C4841D3A50E3F8C9399A635BD4D70C。不得用v1或初版v2。用户明确直接Blockbench制作，已有真实多视角/50fps动画，不再要求Aseprite或重画。
先按美术技能归档确认版，保留bbmodel、原尺寸两纹理、真实视口预览和正式交接README/visual-handoff（记用户确认原话与当前版本）。旧过程目录保留。过程脚本/99帧无需全部复制正式目录，保留过程链接。归档不覆盖其它效果。索引标为已采用、接入状态如实写；不能先称游戏已验证。
运行要求：
1. 真正画出用户已确认的六个Steve方体透明水线，正确64逻辑UV/128贴图，宽臂、方头、方角，双面透明与明亮水线，不能借僵尸外观、不退回2D公告板、不显示灰色参照或地面。按Forge1.20.1原版Steve模型渲染变换对齐，记录模型单位到世界单位的依据，不用32/1.8硬拉比例。材质字节不改。
2. 驻留按当前活实体持续可见直到逻辑结束，包括用户默认lifetimeTicks=0、新观察者start-tracking。不能仅靠20tick MAINTAIN临时快照导致一秒后消失。生成朝向冻结，站立姿态按确认版。驻留和临时世界快照不得同位置重复叠画；终止标记要能压住迟到的人形。使用真实deployment/session绑定，避免依赖相邻实体距离把多个重叠幻影认错。
3. 只按确认的动画数据/关键帧接入：48短片、20滴含8驻留高光；驻留循环原生0..1.12秒。逻辑触发即从动画相位1.12秒开始散水，用连续elapsed + partialTick插值，0.84秒内结束；动画快照独立于已discard实体。立即模式下一tick出术再散，接触/受击身体结束后散水仍播完。不要把预览开头1.12秒等待强加给真实反击，不同步99帧GIF，不增加服务端tick/碰撞/伤害频率。
4. 资源加载/BB数据提取可以用制作脚本生成精简不可变运行数据，但运行时不要加载示意假人/地面/整个过程bbmodel。仅从确认模型提取必要几何/曲线，记录对应关系。资源解析/几何预处理缓存，不能每渲染帧读文件/解析JSON或创建大量列表/随机碎片。使用现有批处理缓冲和客户端快照上限，不新增网络逐帧广播。
5. 旧LEGACY_HUMANOID不变；已修复的断潮世界中心、主人补发、移除客户端重复160格裁剪、世界/断线清理、会话/256上限/去重全部保留。不用旧TechniqueHitEntity伤害业务借渲染，不让客户端class泄漏到专用服务器。
6. static /资源reload/断线等资源和动画生命周期按项目现有方式；画完恢复矩阵、混合与buffer状态。保持确认版水滴加速下落与缩放消散，不改动画观感或擅自“优化”数量。若渲染能力实际不能按批准版做，交回具体差异而非冒充一致。
本次不需要打开或改Blockbench活跃项目；确认模型是来源，不再重画。完成后报告真实运行资源引用、源/运行哈希、尺寸与时间换算、驻留与实体移除后播放路径、缓存/预算、专用服务器隔离、所有新增文件、未游戏实测及已停写。C同期只负责正式注册/lang，互不写同文件。

