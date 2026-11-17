模型：Cursor CLI --model grok-4.7-high-fast。首次Start写会话，不能从Ask会话转写。完整读取TaskFile、本Prompt、docs/统一开发与术式批量制作流程.md第2/3/8/10节。
本单为开批指定的唯一表格写回者sheet-record，只做新需求转录、首次留档、迁制作中；不是正确性复审。不改任何源码、配置指南、数据表枚举，不编译/打包/游戏/提交，不改额度，不制作其他项。
本批直接用户新需求原话（必须原样保存在每项DV或逐项对应的明确关联处，并记录来源）：
“帮我做几个术式
扩展多重释放和一分为N
三重释放、四重释放、8重释放16重释放、穷尽释放
一分为二、一分为5，一分为8
他们应该用同样的底座功能只是数值不一样而已，记得遵循流程”
用户给定新任务并要求流程，允许将八条新需求先转录到待制作，然后首次原始留档、迁入制作中。现有待制作为空，禁止从已完成领取任务。已完成rule_dual_release和rule_triple_cast仅供同类格式/默认元数据参考，不复用编号、不修改历史数据和旧N/O/DV。
八项明确声明：
- rule_triple_release 三重释放，RULE_DUAL / MulticastSpec.fixed(3)
- rule_quadruple_release 四重释放，RULE_DUAL / fixed(4)
- rule_octuple_release 八重释放，RULE_DUAL / fixed(8)
- rule_sixteen_release 十六重释放，RULE_DUAL / fixed(16)
- rule_exhaustive_release 穷尽释放，RULE_DUAL / remaining()
- rule_double_cast 一分为二，RULE_TRIPLE / ReplicationSpec.of(2)
- rule_quintuple_cast 一分为五，RULE_TRIPLE / of(5)
- rule_octuple_cast 一分为八，RULE_TRIPLE / of(8)
类型通用、属性无、释放种类规则、标签触发；实际枚举中文值只能来自当前数据表：角色仍用“ 双重释放 ”对应RULE_DUAL、“一分为三”对应RULE_TRIPLE（按真实值去空格，不新增角色）。五种多重基础冷却20tick，三种分裂冷却0tick；均规则自身耗魔0、基础伤害0、等级上限20。同类其他元数据参考现有声明，不能把过时模板N/O/X当现行需求；pullAfterA仍多重1/分裂0，实际数量由spec决定，数量保存在N，无需新列/结构配置键。
新行N（详细需求，制作中保持原文）：
固定K多重：沿用双重释放的共享多重释放机制，从自身之后的原始装配格开始，按原始格顺序向后读取最多K格；修饰、附魔、复制声明与内层多重仍按既有解析机制处理，K不是成功攻击次数。遇链尾停止，不回绕、不补足；实际成员释放间隔沿用既有3tick。与同类术式仅数量参数不同，不建立专属执行分支。
穷尽：沿用同一多重释放底座，使用remaining模式，从自身下一个原始装配格读取到当前链尾；包含的各类规则仍走共享解析机制，实际成员间隔沿用既有3tick。不回绕、不无限循环，也不表示持续释放直到魔力耗尽。
分裂K：沿用一分为三的共享复制机制，通过ReplicationSpec.of(K)把后续第一个接受复制的宿主展开为K份；作用范围、复制与多重的组合顺序、嵌套乘积、生成几何及命中结算均沿用共享复制底座，与同类术式仅份数不同，不按术式ID单独实现。不要写成只能复制攻击；现有底座接受屏障/领域/护体/场地/条件释放等宿主，完全沿用既有资格。
O文字视觉意图：规则自身不新增独立特效或美术，沿用现有通用规则图标；宿主视觉继续由原宿主和共享多重/复制底座生成。
DV保留用户原话，可追加本批记录链接和“本批方案默认沿用同类冷却/耗魔，数量由声明决定；开发中，尚未编译/游戏验收”。不要写成已复审或已验收。
并发/所有权：原水流sheet-owner attempt a202611090229196660106经Status/Result证实已结束、reported_done/valid、writesSettled=true，结束于2026-11-09T02:35:43Z；其写后哈希与本批最初基线一致。本窗口表格仅由你写，主会话暂不写记录。提交前核对是否有活跃表写任务/源表发生变化，不凭历史hash覆盖。发现新作者或源表变化立即交回，不强行提交。
实现流程：
1. 重读四状态页、资源状态明细、实际表头/批注/数据表，记实时SHA256+LastWriteTime。基线参考为7EE23F670605C912A393457C9DEE61663BDE69BBF06996CA7DA78ACF04275FE5，已完成37、待制作0、制作中16、待验收9，62且唯一；仅参考，必须现场核对。
2. 八ID四页查重，已有就按编号对账交回，不自行覆盖或重复创建。转录新增八行待制作并保存独立候选，保留所有既有A:EN数据、公式、样式、验证、批注和其他表内容。新增行按同类模板样式制作完整合理元数据；不改变旧行。重开候选确认新增后四页总数70、B唯一。
3. 源表baseline未变且窗口唯一时提交转录候选并重开确认，记录hash。此时八行实际存在待制作，不停留只在记录说明。
4. 对首次八条待制作完整原始A:EN和适用明细留入“待制作原始数据”（每B仅首次、原文不转换），记录源头时间版本；留档失败不能迁走。按流程备份、独立候选，将八条待制作迁入制作中，目标完整后才删来源；不改N/O/DV原文，只可追加制作中流程标记。关闭重开候选核对四页总数70且唯一，档案每B首次唯一，其他62行不动。提交前重算源hash等于当前baseline才替换，提交后再重开。
5. 输出领取写回回执.md和批次记录中的对应状态：逐B转录/留档/迁页、时间、前后hash、LastWriteTime、守恒/唯一/重开检查，原始详细N/O/DV/批准版本及原始行快照路径，样式和验证保留的证据。状态为制作中，未编译/未验收。仅新增与搬迁本批八条。
表格工具：这一步使用spreadsheets技能，先读 C:/Users/baishang/.codex/plugins/cache/openai-primary-runtime/spreadsheets/26.1104.22356/skills/spreadsheets/SKILL.md 中适用的现有文件编辑说明。已通过load_workspace_dependencies发现：Node C:/Users/baishang/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe；包目录同级../node_modules。按技能使用@oai/artifact-tool（现有复杂表必须保留全部原生验证/批注等；若缺少必要保真能力，先保留候选并报告具体限制，不能破坏源表）。Python只读提取/核对可以用，未经允许不要用openpyxl保存源表。技能通用“新建outputs工作簿/改布局”不得覆盖用户已有项目写回流程。备份/候选/脚本放本批sheet-work，源码禁止改。
首次作者命令前按技能执行mark_artifact_operation_started.mjs edit一次；具体脚本位置遵循技能与runtime。保存前后做适用核对与视觉检查（只看受影响视图，不改布局、不重渲染无变化旧表）。若环境能力不支持无损编辑，请报告具体缺口，不擅自跳过留档/迁页。
最后给标准 <<<CURSOR_TASK_REPORT>>> JSON：changedFiles真实路径，writesSettled=true，notRun写未编译/游戏；nextAction交源码作者。返回每B真实表迁移回执，不能以lifecycle=reported代替业务完成。
