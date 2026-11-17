# C第一段：不依赖A新接口的地形、残骸与资源

固定Cursor CLI --model grok-4.7-high-fast。阶段为已批准内容的实现，不新增设计或正式复审。设计2/2，实现0/3。A仍在公共链/命中/32测试法杖实现中；B资源接口已最终交付且停写；素材归档和表参数已最终交付且停写。为并行推进，这一段仅做与A正在写的源码无依赖的独立部分，不执行author-c-content-task.md中必须等A接口的命中/链路接线。

输入：approved-v1.md、implementation-contract.md、最新基础6/测试法杖32授权说明、author-b-interface/report/ownership、resource-manifest-v1.json/md、intake.json的相关N/O/DV。C完整后续范围见author-c-content-task.md，但本阶段严禁读取A实时日志、未交付的接口或中间源码，不以等A为由改A文件。

授权源码根D:/alianximod/src/main/java/com/bslx/XueXiMod/，只准：ModBlocks的新增陨石方块；新的陨石方块类、新的地形权限/保护/破坏开关与安放服务、新的无伤待安放残骸实体（只含持久保存、合法接地后一次崩地/安放、重试；不要带任何攻击或接续接口）；这些对象新的独立渲染类。先列author-c-ownership.json的实际绝对路径，核对B所有权，不修改任何A公共链/命中/费用/注册/法杖文件。ModItems、ModEntities、ModCreativeModTabs、语言、网络/renderer公共注册都由A最终整合，你只交注册清单。

本段不修改DomainService/DomainInstance/ContactEnchantState，也不实现陨岩有伤飞行/热波/火苗/留焰/幽冥接线，因这些须A新接口。它们在你第一段最终停写后续派发。不得在现有旧火六式或其它载体中加临时ID特判。

实现批准的地形与无伤残骸要求：
- 陨石方块硬度3抗爆6，石镐及以上掉自身1；无额外矿物/燃料用途。真实碰撞、世界存档、方块模型。
- 本批统一破坏开关默认开启，仅控制本批破坏已有块；安放与开关分离。不要顺手改生成水/分海等旧机制，也不要宣称已经全模组统一。准确写出开关接入方式，公共注册需求交A。
- 用B BreakGroundShape.offsets()的浅碗r2深2最多32（当前23）块，不复制坑算法；原子一次执行标记，按距离排序、无世界爆炸无生物伤害。不读取A帧上下文，只接受显式owner/level/impact/是否允许一次破坏等最小调用参数。
- 对世界边界、已加载区块、玩家模式/权限、不可破坏/容器/方块实体/流体/传送门/保护块过滤。Forge1.20.1破坏/放置事件签名查本地依赖，逐块尊重取消。不得直接setBlock后声称权限已验证，不冒用假玩家绕过主人离线/不可解析。
- 无伤残骸只在最终攻击调用方已经完成父子结算之后创建或转换；本类绝不能伤害、产种、接续。落地才按一次标记执行崩地/安放。优先落点上方再水平半径2上下2合法完整空气位、有支撑、无活物占据、放置权限通过，不覆盖地形。没位置/主人离线保留残骸每20tick重试，保存ownerUUID/唯一身份/崩地已执行/待安放标记，不超时丢弃、不强载。方块放置真实成功才删除残骸，避免重启复制或重复挖坑。

素材允许从resource-manifest-v1已批准正式路径复制至其建议src路径，保持hash；16帧64×1024效果条带供后续接线，不写假渲染引用来宣称已完成。实际陨石六面只用32x32 meteorite-block-texture.png，64x1024外观稿不是六面纹理。可新增陨石blockstate/block/item模型、loot/tags JSON；不覆盖任何旧火六式PNG/模型。若矿工具标签要改现有minecraft集合，仅追加本块不破坏旧值。

不写xlsx、资源总索引、记录.md或A/B源文件。不Gradle/javac/build/jar/reobf/runClient，不启动游戏/独立测试阶段。必要自查不算独立正式复审，不spawn代理。

输出author-c-stage1-report.md/json、author-c-stage1-interface.md、author-c-registration.md、author-c-ownership.json（注明后续还会补）。报告新类型稳定签名/注册清单/资源hash/修改mtime/未接的攻击链与域能力/待统一编译，明确全部写入停止。不要等待或监控A的中间文件，完成这段就给最终反馈。
