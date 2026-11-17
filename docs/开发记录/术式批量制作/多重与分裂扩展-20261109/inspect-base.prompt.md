模型：Cursor CLI --model grok-4.7-high-fast（启动器固定）。本阶段仅只读定位、事实采集，不实施、不做正式正确性复审。完整读取 TaskFile 与本 Prompt。
用户授权八个新术式：三重释放、四重释放、八重释放、十六重释放、穷尽释放；一分为二、一分为五、一分为八。同样底座仅数量不同。现有文档 docs/多重释放底座与扩展指南.md 明确 MulticastSpec.fixed(N)/remaining 与 ReplicationSpec.of(N)。
只读范围：
- D:/alianximod/docs/多重释放底座与扩展指南.md、docs/配置平衡指南.md、docs/开发记录/多重释放底座-20261030/记录.md（只了解旧计数和结构事实，不重新验收旧底座）。
- D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/Multicast*（若实际在 multicast 子目录则定位）、ReplicationSpec、ReleaseDefinitions.java、ReleaseDefinition.java、ChainRole.java；本次入口所依赖的技术定义/术式注册与 BalanceRegistry/BalanceKey/BalanceDisplay 等配置文件，可按引用定位但不整仓通读。
- D:/alianximod/src/main/resources/assets/xueximod/lang/zh_cn.json 和 en_us.json，仅本家规则术式条目。
- D:/alianximod/docs/术式制作表.xlsx：只读当前四张状态页，计数、检查编号唯一；列出本次八个名称或同义名称已有行的完整A:EN（优先N/O/DV原文与相关字段），以及现有双重释放/一分为三的模板行、原始编号、表头枚举/数据表有关值。未匹配到本次项就如实报告，不抓其他待制作任务。可用只读 Python openpyxl 提取，不保存表。
- docs/开发记录 下现存20261109批次的记录文件：仅提取当前文件所有权/是否有作者占用共享注册器、语言文件、配置文件或 Gradle。不读取正在运行任务 raw 或中间源码、不跟踪思考或重复diff。
输出简明事实包，带真实绝对路径和行号：
1. 多重和分裂只声明数值所需的最小写入文件与现有相关片段/方法签名。列现有双重、一分为三元数据、耗魔/冷却默认值、注册方式、图标是否共用。
2. 穷尽是否已有正式语义，remaining()是否指本链剩余原始装配格；任何需要用户选择的实际分歧。没有就写没有。
3. 重要配置怎么声明并消费，哪些数量属于现有结构声明不可配置、哪些平衡值需要接入；不得为本单自行决定新增结构机制。
4. 当前四状态页计数、哈希/LastWriteTime、相关行和是否存在待制作项。该消息是用户直接列出新制作需求；若未入表，不据此擅自登记或领取。
5. 并发占用事实、建议给源码唯一作者的最小路径清单。没有证据不要声称没人写。
禁止写任何文件（含记录/表/源码/运行目录）、编译、打包、测试、启动游戏、提交，保留未提交改动。摘要如截断需在原会话补全。
最后按启动器要求交 <<<CURSOR_TASK_REPORT>>> JSON。changedFiles=[]，writesSettled=true，notRun 写明未实施/编译/游戏。这里只交事实，不声称复审通过。
