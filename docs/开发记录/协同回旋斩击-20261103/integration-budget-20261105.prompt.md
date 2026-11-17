# 用户实测返修：指挥分身频繁误报展开预算

继续batch cooperative-slash-20261103、task impl-integration与当前provider会话。该批设计2/2、实现3/3已用完；本单仅用户明确反馈后的定向修复与作者必要自检，不是新设计/正式复审。不要开审查者、子代理，不改阶段额度。主会话只调度/转达最终结果。

用户2026-11-05最新反馈：指挥分身经常提示“这次展开超过了体积或人数上限，没有释放”，但周围空间足够，场上只有一只分身。截图为村庄户外，附近有草、房屋及少量地形高差，一只标注“普通分身·基础”的实体就在旁边；无证据表明是同时生成大量分身。截图路径C:/Users/baishang/AppData/Local/Temp/codex-clipboard-698fc7b2-c3f4-4efb-a719-b8dbd5113365.png，主会话已经看过，不能仅凭空间宽敞就推断服务端预算必然合法。

主会话只做了提示符号定位：src/main/resources/assets/xueximod/lang/zh_cn.json 中gui.xueximod.cooperative.budget；声明在src/main/java/com/bslx/XueXiMod/network/CooperativeChainCompiler.java BUDGET_KEY。用户装配链顺序已提问，尚待回复；本单先从实际触发分支定位，不臆造其装配。当前实际测试世界此前用户确认为run/saves/新的世界，协同selectionRadius=24、moveLimit=32。不要改存档或借提高总预算掩盖问题。

任务：
1. 从BUDGET_KEY全部触发分支及必要调用链定位这次现象的可触发根因。定向读取CooperativeChainCompiler、协同执行族、配置消费者和必要分身端口；不要通读整仓。必要时只读run/logs/latest.log和指定世界配置中的相关非秘密项，看是否已有实际失败证据；不要修改运行中存档。
2. 修复有代码证据的误判或重复计数，特别留意把参与者数量、候选选择半径、归位包围范围或重复规则展开错误混成攻击实体/攻击体积预算的可能性。这只是排查方向，不是预设根因；必须按实际代码。保留真正的数量/体积保护，兼容已批准的单人斩击、回旋、协同与既有规则结算，不改费用/冷却/伤害职责，不回退24/32距离。
3. 若只是合法预算不足或异常配置，请在报告写具体参数、计算过程与触发分支；不能靠修改提示或提高限制假装修好。若修复必须写本单未授权文件，先交回已定位的精确路径与最小改动说明，保持writesSettled=true，不越权修改。
4. 做必要定向作者自检（现有命名门禁适用时可跑），报告修前/修后可触发案例和边界。不要创建无关测试基础设施，不另做正式复审。没有游戏验证就写未验证，不能以截图/推算当游戏实测成功。

本单唯一允许写：
- D:/alianximod/src/main/java/com/bslx/XueXiMod/network/CooperativeChainCompiler.java
- D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/cooperative/FamilyExecutionService.java
- D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/cooperative/CooperativeContent.java
- D:/alianximod/docs/开发记录/协同回旋斩击-20261103/integration-budget-20261105-delivery.md
以上三个源码为原I作者范围，其他作者已收束，没有并发写入。原clone/core属于C作者，本单只读不写；原共享SkillUsePacket等不在本单写范围。禁止改视觉资源/动画、Lang文件、全局配置、世界配置、工作流启动器、旧回执、主会话记录、制作表、业务无关文件。保留全部已有未提交改动，禁止reset/clean/checkout。

当前runClient进程56204、外层65696仍运行。明确禁止本单Gradle/编译/processResources/打包/启动或操控游戏/结束游戏进程；最终编译由主会话等源码收束并由用户正常退出后，另放行原compile-final任务串行执行。用户上轮仅一次主会话Java/编译例外已结束，不适用于本次。

固定当前启动器渠道/模型/auto，不降档。只等你的最终反馈，不需要实时进度报告。返回根因事实、修改路径、实际检查和未跑项，最后唯一JSON标记各占一行：
<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done 或 blocked 或 needs_input","summary":"定位与修复结果，编译待主会话安排","changedFiles":[],"checks":[],"notRun":[],"writesSettled":true,"blocker":null,"nextAction":"交回主会话；若需扩展文件列精确路径，否则待最终编译后用户实测"}
<<<END_CURSOR_TASK_REPORT>>>
