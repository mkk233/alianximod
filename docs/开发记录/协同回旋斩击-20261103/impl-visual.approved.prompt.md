固定Cursor CLI --model grok-4.7-high-fast，不换模型/降档/spawn。你是本批唯一V客户端/动作协议/正式美术接入作者。完整读TaskFile、本文、AGENTS.md、新流程、max-dispatch、本批implementation-contract.md、interfaces-delivery.md。用户已授权制作；Astra设计1/2定稿、实现1/3已使用。你不重新设计、不改额度、记录.md、TaskFile、启动器或.grok回执。I接口最终回执已valid/reported_done/停写后主会话才会Start本单。

最新实际美术授权：用户2026-11-03明确回答“采用这版，继续接入”。确认的是cooperative-slash-visual-v1-aligned。正式目录D:/alianximod/docs/资源库/动画/剑术/协同回旋斩击_v1/，先完整读取README.md并核对其6文件SHA256。可以接入这一版，无需再次询问同版许可。较早*_native_preview.gif和过程目录初稿不可用。本Prompt代替未派发的impl-visual.prompt.md里“等待美术批准”部分；本单TaskFile已补正式资源写范围。

文件所有权：只写TaskFile writePaths。API/ExecutionFamilyPlan/核心共享链/主入口等I独占，clone/core全部C独占；你不能修改它们、复制新DTO或做反射旁路。公共类型精确签名以interfaces-delivery.md为准。缺必要接口则完成可完成部分并报告具体阻塞交I，不擅自改类型。可以定向阅读相关旧实现，禁止通读整仓。保留已有未提交改动，不reset/clean/checkout；禁止gradlew/build/jar/reobf/runClient、游戏操作、制作表写入或独立测试任务。

实现合同第8/9/10和客户端边界：
- 玩家与普通分身共用ActorActionPublisher、ActionPlayback协议/缓存/采样/姿态模型；serverSession、dimension、family/member、actorUUID/entityID/世代、单调sequence、resourceID、serverStartTick、windup/recovery、visualYaw、mirrored、有界只读武器显示快照全部保留且验证。
- 每actor只保留最新sequence，旧stop不能清新；迟到按服务端startTick快进，新tracking观众补当前未结束快照；结束/卸载/换维/断线/世代变化清缓存，缓存有界；仅start/stop/tracking发包，不逐tick全员pose广播。客户端不提交伤害。
- 动画仅改渲染骨骼与握柄挂点，不改主人逻辑位置、真实碰撞盒、相机或yaw/pitch数据。根部固定，短距归位由C的真实移动带来，不在模型里再移一次。渲染时要恢复共享ModelPart临时变换，避免下一个实体继承偏移/缩放。
- 单独斩击从切入姿态即时出刀、8刻收势；回旋6+6刻，出刀时不能被solo动作覆盖。时长用服务器冻结DTO，前摇和收势分别重采样；不能用客户端动画通知触发玩法。
- 共用spin资源内可以保存normal/mirrored两套轨道，从原生bbmodel的player/clone演员取相应曲线。协同源含前0.10秒归位示意，正式spin的两演员曲线应减去该前缀对齐同startTick，不给动作本身重复加归位。回旋root的Y旋转仅在模型渲染中使用，脚点不随模型绕圈平移。
- 斩光源是已确认128×128 slash_arc_v1.png（SHA256 58E741C8BED808EB4E8CC6DC77FA71FD009A9AAB0B7FEBC104ACCB68E655BFD7）；bbmodel实际纹理名slash_arc_plane_v1，对应相同顶弧像素。不能采用初期右侧C弧缓存。游戏PNG应逐字节相同；转换动作JSON可精简但记录源轨道/模型到游戏坐标/单位/握柄锚点映射。
- 斩光由真正发布的PreparedEmit/emitId驱动，只有单人发布就不能残留第二条。显示宽5深3格载体、眼前1.5格中心、±35度刀面，实际以服务器冻结形状/方向/配置为准；不是镜头公告板。材质双面透明，避免串用另一texture的VertexConsumer，维持0.2秒默认窗口及同版展开/收束相位。正确区分逻辑hitbox厚.5和美术平面本身无实心厚度。
- 协同规则不拥有专属斩光；回旋动作不把示意模型slash_effect轨道当动作内自带攻击。新斩击与横劈使用各自实际攻击视觉；换末端攻击时动作可复用。
- 青灰假人、Steve预览模型、占位钢剑、地面与参考图都不进runtime。普通分身沿现有外观；持物若显示仅不可交互渲染副本，不生成item或伪造装备伤害。
- 现有space_cut动作/资源及其它渲染路径保持；必要扩展既有渲染钩子仅限所列文件。TechniqueHitRenderer的本批斩光接点也归V，可最小接入真正emit的冻结几何、表现ID和已确认PNG；不改TechniqueHitSpec/TechniqueHitEntity等共享服务端类（归I）。CloneEntity服务端字段归C，不改。尤其保持水幻形SteveWaterLineVisual先skin后flakes的提交顺序，水幻形贴图/bin/散水路径不改、不重审旧批。
- 主入口XueXiMod等只由I挂接；提供精确registrar/publisher工厂给I。若需新的共享文件或mixins配置，报告接点由I统一处理。

运行目录只放必要运行数据，制作过程脚本不要塞src；如需要归档转换脚本，交主会话申请本批过程目录专有文件，或在visual-delivery.md写出转换步骤。当前不用重新操作已批准Blockbench工程，不改原美术形状/速度。

交付D:/alianximod/docs/开发记录/协同回旋斩击-20261103/visual-delivery.md：实际文件/行号、网络和清理实现、I要调用的精确工厂、确认资产到runtime路径/哈希/骨骼单位转换、与批准预览的任何差异、已做作者自检和未编译/未游戏验证。实现者必要自检不是正式阶段复审；不可声称游戏观感已验证。
最终唯一机器回执（标记独占一行，字段真实，不能伪造task/attempt/session/inputHash）：
<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"实际客户端与批准美术接入结果","changedFiles":[],"checks":[],"notRun":["未编译","未游戏验证","未正式实现复审"],"writesSettled":true,"blocker":null,"nextAction":"主会话收齐C/E/V最终回执后交I共享整合"}
<<<END_CURSOR_TASK_REPORT>>>
workStatus仅reported_done/blocked/needs_input/failed；缺项/越界阻塞如实报告，不把退出0当业务完成。最终后停止写入。

派发时使用I接口定向返修后的最终清单；实例能力判断与static工厂已区分，按最新interfaces-delivery.md读取，不照抄早先重名签名。主会话下一次集中实现复核累计第2/3轮，你不自行正式复审或改额度。

