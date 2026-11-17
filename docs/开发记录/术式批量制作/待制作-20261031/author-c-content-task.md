# C：具体载体、领域、地形与确认素材接入

本任务只在A/B接口与素材归档均已交回最终反馈并停写后启动。固定Cursor CLI --model grok-4.7-high-fast。阶段实现，设计2/2已用完不追加，实施正式轮次以记录当前值为准（开始时0/3）。用户已批准13项首版机制/数值及暂用二维v1；法杖最新为基础6槽原属性保持，另加test_staff32槽/1000魔力/总16倍回魔/-50%CD，不是扩基础法杖。

必须完整读取本目录 intake.json 的N/O/DV、approved-v1.md、implementation-contract.md、author-a-report/interface/ownership、author-b-report/interface/ownership、resource-manifest-v1.json/md以及素材采用证据。若任何前置报告缺失或接口未收束，返回具体缺口，不读取实时日志猜进度。

权限：源码根D:/alianximod/src/main/java/com/bslx/XueXiMod/内，仅你新建的陨岩/热波/火苗/留焰残留几何执行、地表锚点显示、新幽冥领域周期/色调、地形Mutation/无伤残骸实体/陨石方块；现有DomainService、DomainInstance、ContactEnchantState、ModBlocks及领域对应专用事件接线；新的独立客户端载体/特效渲染类。可小范围在现有火系客户端渲染入口/渲染类接幽冥纯颜色通道，但不改旧六式机制、几何、公式或纹理。先写author-c-ownership.json列实际绝对路径，核对A/B已有所有权，任何重叠先交回而非抢写。

严禁修改A的链/命中/费用/法杖/ModTechniques/ModItems/ModEntities/共享注册文件和语言文件；严禁修改B的附魔语义/火种账本/火伤帧及公共EnchantBonus/Definitions。所有注册需求交 author-c-registration.md 给A统一集成。所有新种火/引燃/引爆调用必须用B公开API，不复制一份资源结算。

具体实现：
1. 火苗：已批准1火伤/2基础产种/5tickCD，0.8速度30tick寿命0.25立方碰撞、20tick原版点燃，支持现有兼容索敌/弹道/复制/附魔。命中/撞墙消失无爆炸。利用A命中结果+B生产事务合并本体2和种火10，后续原版烧伤不产种。
2. 热波：4物理，环1.5–5，总高3，判定一次，中间空，12tick视觉；用A固定锚点/父接触点和排除分区接口，不能自己复制父附魔，不能中心击退后再命中环带、不能透原遮挡。
3. 陨岩：24物理，1方块体积，默认0.8速度上偏15度0.08重力，100tick有伤上限；最先成立实体/地形碰撞中心一次1.5半径×3高物理，不额外直击/原版落物伤害；按A父结算完成后子接续。空中命中后无伤残骸落地，禁止再中心/子/资源结算。每实际岩体最后一块真实陨石或一枚待安放无伤残骸，保持唯一与存档；无空间不丢、不强加载，20tick重试，崩地标记保存。
4. 留焰：B附魔仅载荷，你实现兼容移动宿主的实际扫掠残留几何，40tick直径.5，真实路径、256表现采样上限并集判定；本体/路径同一实际副本共享A HitClock。它不重放爆炸/接续/掉块，不伪造新castId；不按片段重复产种/挂层/消费。陨岩火径仅直接24不变成每段中心冲击。材质与判定路径一致。
5. 幽冥：依附显式支持的既有领域载体（含灼地，不自造领域），每20tick基础5×主人当前产种倍率，完整周期后首笔；用B窗口仲裁，重叠每目标每全局20tick仅最高请求一次，存活燃烧可花新增种。领域范围与时长继承，接触伤害为0也能产种，不减速不额外伤害。领域里的火系表现变蓝、离开恢复，纯视觉不换伤害属性；客户端用明确领域同步/归属，不按距离猜玩家。不得用原版魂火方块或灵魂沙。
6. 崩地：B载荷授权一次有效撞地；新统一破坏开关默认开；浅碗r2深2最多32块，保护过滤/权限/Forge破坏和放置事件签名查本地1.20.1依赖，不能setBlock当已过权限。父子生物与遮挡冻结先结算，再破坏再陨石安放；关闭破坏不关闭合法安放。空中命中残骸接地只破坏一次。跳过不可破坏/容器/方块实体/流体/传送门/保护块与未加载区块，主人无法合法解析保留无伤残骸不冒用权限。
7. 陨石块硬度3抗爆6，石镐及以上掉自身1，无其它矿物/燃料用途。使用manifest的32x32 meteorite-block-texture贴图与原版立方体模型，不把64x64等距方块预览画贴在六面。必要blockstate/model/item/loot/tag JSON你可写新增文件；注册声明交A，ModBlocks唯一由你改。
8. 接入manifest确认素材，原尺寸透明帧条分帧，不按整张1024高拉成长条。资源根仅D:/alianximod/src/main/resources/assets/xueximod/下本批新的textures/effect、textures/block、models/block/item、blockstates、particles和D:/alianximod/src/main/resources/data/xueximod下本批loot/tag所需新增文件；已有同名资源先比hash，不能覆盖旧六式素材。语言键清单交A。

源码/资源不写其他未授权文件，不改xlsx/记录.md/资源库README。新hook跨A/B范围只写handoff请求，不能私改接口。阶段交付仍待A统一注册/编译，不用空实现顶替未接线。

对照实际接线做必要实现检查，强调远端玩家/实体追踪、原地施放、生命周期、区块保存/加载与客户端清理。禁止启动游戏/Gradle/javac/测试/build/jar/reobf，不创建独立测试阶段。不要自行追加设计复审或spawn代理。

最终 author-c-report.md/json、author-c-registration.md、author-c-ownership.json：改动路径与hash/mtime、实际可用接口、注册/接缝剩余项、素材SHA映射、检查与限制、待统一编译、全部停写声明。
