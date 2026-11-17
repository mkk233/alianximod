# 空间切割 v5 左腰右拔动作：实现

固定 Cursor CLI --model grok-4.7-high-fast，沿用会话 2e893923-df99-4d35-be6c-2bbf3b58d518。你是本批唯一源码实现者。继续你已返回的只读定位，不重新通读整仓、不派额外设计复审、不改模型或降档。设计2/2已用完；实现正式复核0/3。用户已明确确认v5并授权作为空间切割挥刀动作接入。以下选择由已确认视觉与用户授权范围直接落实，不再让用户重复确认。

## 输入与决定

正式资源已由主会话归档到 D:/alianximod/docs/资源库/动画/剑术/空间切割_左腰右拔_v5/。必须以这里的 visual-handoff.md、bbmodel、animation-keyframes.json、native-mount.json 和 GIF 为准，源字节与之前过程v5相同。manifest已写用户确认，不再以过程历史pending标记当未授权。

1. 使用完整 v5 的1.6秒视觉动作，once，按原同步入口触发，不引入玩法蓄力、移动/施法锁、服务端伤害延迟，不延长原20tick冷却。后一次实际释放替换尚未结束的姿态，沿用当前动作会话规则。原有斩面出生时钟和20帧破碎特效保持。说明视觉与伤害的现有分离，不把0.48s示意挥斩点变成新的服务端等待。
2. 必须实现真实双节手臂和独立持刀挂点以保留确认版。不能只替换OriginalHumanoid五骨旋转然后宣称已还原，不把前臂折叠丢掉，不用默认挥手充数。运行时应使用玩家现有皮肤/袖层、真实所持剑；示意假人、地面、图集里的演示武器均不能直接替换玩家外观或作为假刀进入游戏。
3. 硬约束：左腰裸刀，刀尖朝玩家背后；右手跨身握住并向前带出、单次连续横斩、停势落刀；左手空手让位。左右不得镜像错。游戏主手设为左手时也需按用户明确的“右手拔刀”表达本动作，真实剑在动作期临时由右手渲染，正确抑制重复默认持物并在动作结束恢复设置；若现有持物来源不满足，明确报告，不永久更改玩家主手或库存。无真实剑的情况下沿用既有可施放条件，不凭空生成预览假剑，不修改是否能施法的玩法限制。
4. 第一人称和第三人称、本人和观察者都需要同一动作/武器绑定。适配普通/细手臂皮肤、袖层及已有护甲路径。只在此动作激活时启用分段臂和独立持物覆盖，动作结束/中断/换世界等必须恢复原版模型、持物、渲染状态。已有发布皮肤20骨格式保持兼容；不得全局改骨数导致其他动作或皮肤退回/失效。
5. 保留现有ActionSyncS2CPacket同步、每组一次玩家动作、三份独立空间斩击的数量和槽位分布；不改伤害、命中体、无敌帧、范围、ChainRole、规则链、配置或服务器释放分支。
6. v5目标坐标是ActorRoot参考系，经Body逆变换计算手臂，不是Body局部轨迹。真实挂点起始右掌[5.3,15.5,-3.6]、刀轴[0,0,1]，刀绑ForearmRight/SwordHand。读取9骨关键帧与expected，正确转换MC玩家坐标和持物模型轴。上臂/前臂各6单位，不拉长手臂。姿态与物品不能仅靠单臂欧拉近似代替。

## 写入授权与并发边界

唯一作者可修改：你定位报告中的 OriginalHumanoid.java、RetargetProfile.java、RetargetedPose.java、RigidPoseRetarget.java、ThirdPersonPoseWriter.java、ActionRenderQueries.java、FirstPersonHandBasis.java、ItemInHandRendererActionMixin.java，以及它们同一动作/客户端皮肤渲染模块中为本版必需的实现类、新增专用姿态资源和必要mixins挂接。先在本批 implementation-ownership.json 列出实际绝对路径、每个既有文件写前sha/mtime；新增文件确认未被占用。新类保持项目命名和client/server隔离。已有未提交改动逐行保留，只做本次最小增量。

明确禁止写分身C拥有的：XueXiMod.java、Technique.java、TechniqueManager.java、ModTechniques.java、BalanceCatalog.java、ReleaseDefinitions.java、ShuCommands.java、PlayerConnectionHandler.java、SkillAssemblyPacket.java、SkillUsePacket.java、StaffSkills.java、PerfMetric.java、PerfCounter.java、PlayerShuData.java、语言文件与菜单/登记；也不写clone/core、clone/action、CloneBalance、CloneRenderer等分身路径。该任务在候选目录且本项动作模块不重叠，允许在本项正式src完成实现。若新增需要共享文件先最终报告精确冲突，不能抢写。

允许新增 D:/alianximod/src/main/resources/assets/xueximod/ 下本动作所需的运行姿态数据，只复制/转换确认版确实使用的数据，不把bbmodel/假人/预览GIF整份塞进运行资源。不得重做既有dimension_willow_v8.png。依赖库不升级、不下载模型、不加入外部动画框架；如果确实无法在既有系统原样表达，最终报告具体阻塞，不悄悄做五骨降级。

文档写入：本批目录的 implementation-report.md、implementation-evidence.json、implementation-ownership.json、before-implementation/。正式资源目录只允许新建 runtime-mapping.md/json 记录实际运行路径和适配，不改确认的二进制源/预览。资源库总README、正式README/manifest由主会话负责，禁止你并发改。docs/术式制作表.xlsx 禁止写；你已定位它在历史已完成第6行，不把它迁走，不修改历史表记录。

## 本阶段检查与交付

无编译授权，不运行任何gradlew，不build/jar/reobf，不runClient或启动游戏，不提交，不重置或清理工作区。写完相关Java后运行 powershell -NoProfile -File tools/check-name-gate.ps1，只修本次动过文件的新问题。必要的作者实现检查限本次动作和真正受影响共享路径，不新开独立测试阶段、不做整仓审查。

最后集中返回：改动路径/关键行、运行资源映射与原版数据的对应、普通与slim皮肤/袖层/护甲/第一第三视角的实际实现覆盖；如何保证左腰右手与独立剑挂点、如何清理和恢复；真实已做检查结果，未验证的运行表现；每个文件最终sha/mtime，以及停止全部写入声明。写“待统一编译”，不得写游戏效果已经验证。不能实现的要明确列出，不以静态示意或编译代替完整功能。实施不改设计/伤害逻辑，正式复核等你最终交回后集中安排。
