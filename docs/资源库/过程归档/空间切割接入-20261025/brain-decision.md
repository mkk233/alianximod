# 空间切割 v8 接入裁决

2026-10-25。执行：用户指定 Cursor Grok4.7HighFast；本文件只作调度，未修改游戏代码。仅修改并编译，不打包、不进行游戏测试。

## 真实入口与当前状态

- `ModTechniques.java:100` 明确注册 `space_cut`，显示名“空间切割”，类型 SWORD；同文件注册 SpaceCutEffect。美术目录“次元斩”只是素材命名，接入对象是空间切割。
- SpaceCutEffect 当前仍使用 horizontal_slash.png，STILL_AIM、centered，width=5、length=3、height=0、lifetime=10，hitInterval=0。保留这些命中参数、锚点计算、瞄准、listener 与 onExpire 触发时机。
- 当前 effect 已通过 HostGroupContext 注册 OriginalHumanoid.SPACE_CUT_ID；不得覆盖成前次只读时的旧版本。
- ReleaseDefinitions 已用 cohortSword 注册 space_cut，trajectoryMutable=false；无需再开启一分为三能力。
- TechniqueHitSpec.spawn 已按实际副本/生成号 assignCohort，再创建实体。CohortVisual 区分 displayIndex/displayCount 与 laneIndex/laneCount；已经按几何散开的轴不会重复分槽。
- TechniqueHitRenderer.renderSheet 已使用 CohortLaneLayout.sheetWindow。该布局按短轴分槽，每槽60%显示、40%留白。当前5×3面、同锚点三份会沿上下方向各有独立槽；不能绕过这个机制，又在渲染器里循环画三次。
- CastExecutionSession/SkillChainPlan 已循环传递 copyIndex/copies，并共用组Tally。此次不改规则、实体份数、结算或生成布局。
- v8为96×960、20个96×48帧、每帧1tick，正常20TPS下1秒；第19帧透明。主体有峰值停顿，碎片先收；不能改回旧版26帧或压缩为10tick。

## 最小正确方案：命中10tick，独立纯客户端视觉20tick

不能直接把命中实体lifetime从10改20：现有实体在整个存活期持续查找可命中目标，且到期listener可能触发后续链。也不能只改UV：实体10tick移除会截断后半动画。

采用通用、可选的单次贴图视觉描述与客户端快照存续；由SpaceCutEffect声明启用，默认关闭，不在通用规则中判断space_cut字符串。

1. Effect提供新纹理路径、列/行或帧数20、每帧1tick、单次播放等最小描述。TechniqueHitSpec携带，TechniqueHitEntity同步必要视觉信息与实际激活gameTime。其他术式默认仍走原路径。
2. 视觉起点必须是正式激活：HostGroup暂挂期间保持未出生值，在releaseActivation时一次性填写；非HostGroup则在成功发布后激活。不要在applySpec时提前计时，不使用客户端tickCount充当服务器出生时间。
3. 客户端按实体UUID缓存一份只读绘制快照，含最终世界位置/旋转/尺寸、CohortVisual、视觉描述、激活时间与世界身份。由客户端tick/同步可见状态捕获，而不是仅第一次进入视锥时创建，避免从画面外转入时重播或丢失余留。
4. 快照绘制从出生到出生+20tick统一负责整段动画；启用该描述的实体不再走旧renderSheet，避免实体存活期重复绘制。实体在10tick正常移除后，快照继续剩余动画；无新服务端实体、无伤害、无listener、无结算。
5. frame=floor(gameTime-birth+partialTick)，限定0..19；age<0或age>=20不画并按到期清理，不取模。缓存不因同步刷新或再次收到相同UUID重新起播。切世界/断线立即清理；无界缓存不可接受。
6. 只允许本次这种静止、冻结斩面的描述启用快照方案；不顺带推广到移动飞弹或追踪实体。若需要取消语义，可复用明确的取消状态，不能把正常10tick移除当成视觉必须取消。

## 三道独立与画法

- 每个实际命中实体对应一份快照；三份就是三个UUID，保持各自CohortVisual。禁止按术式、锚点或贴图合并去重，也禁止单实体额外复制出三道。
- 绘制时继续调用CohortLaneLayout.sheetWindow，保留最终姿态、锚点和倍率，不再乘一遍倍率。同锚点三份沿已有上下槽分离，已经散开的份不再二次平移。
- 单素材是完整一道柳叶。三槽内分别播放整张当前帧，不把贴图切成三段。保持槽间空白，不额外添加重叠辉光。默认依从现有FaceWindow尺寸；不要为了严格维持素材2:1比例把每条长斩均匀缩到只剩短小碎片。
- 新纹理不走mcmeta自动动画。UV使用v=(frame+localV)/20、u=localU，顶部localV=0，底部=1，防串帧。最近邻，无模糊与mipmap，正式素材透明背景，不用GIF底色。
- 沿现有实体面绘制，不改为相机billboard，不额外旋转到水平：v8自身约8度斜角及收束动作应保留。
- 新动画使用透明混合、正常场景深度测试、只写颜色、不写深度、单份双面无剔除面。避免原renderSheet的双份反向面造成透明重复混合。该修正限新视觉路径，别顺手重写全部旧贴图斩击。

## 文件范围与实施注意

- 预期修改：SpaceCutEffect、TechniqueHitSpec、TechniqueHitEntity、TechniqueHitRenderer；新增通用单次贴图描述/客户端快照渲染与必要客户端事件绑定。优先复用现有同步机制，不改攻击网络流程或新增伤害包。
- 在写上述共享文件前重新读取当前内容，避免覆盖其他正在完成的cohort和HostGroup改动；只做局部补丁。ReleaseDefinitions、RULE_TRIPLE、ReleaseBatch、CohortVisual、CohortLaneLayout原则上无需修改。
- 正式PNG原样复制至assets/xueximod/textures/entity/xueximod/下明确v8文件名，SpaceCutEffect引用；归档PSD和历史版本保留。更新素材README与资源库索引，明确采用v8及未游戏验证。

## 必须验证

1. 同步出生前不显示；激活后帧0开始；age9、10、18、19、20帧/显隐正确，实体10tick移除不截动画，19透明，20清理。刷新数据不得重播；世界切换不残留。
2. 单份一个视觉，同锚点三份三个不同UUID、三个不相交槽；已散开时不二次分槽。缓存与渲染均不调用spawn或伤害接口。
3. 原命中窗口、onExpire时刻、后续触发、Tally分组及settleCopyIndex不变；不得为了视觉增加hitInterval或改无敌帧。
4. PNG哈希与确认版一致，96×960，20帧，引用路径存在；半透明混合无重复面、深度测试保留。
5. Cursor执行compileJava --offline --no-daemon；明确复用已可用的Gradle缓存，避免每轮新隔离缓存重下。如确需联网补依赖只做对应构建，不改无关构建配置。只编译不打包、不启动或操控游戏。
6. 有意义的纯逻辑检查优先覆盖时间边界、UUID去重与三槽布局；编译通过不能宣称三道视觉或游戏时序已实测。交付明确保留游戏内视觉核验事项。

## 主控素材复核
主控gpt-6-astra直接读取PNG IHDR确认dimension_willow_v8.png仍为96x960，20张单帧均96x48。若读成96x192应检查解析方法或读取路径，禁止重建/覆盖确认素材。
