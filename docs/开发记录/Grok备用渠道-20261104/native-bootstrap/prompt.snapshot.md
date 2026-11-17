# 用户新授权：接入Grok CLI老猪中转站备用渠道

先完整读取同目录fallback-adapter.task.json、设计裁决.md、已核实输入.md、记录.md。你是本批唯一脚本/工作流文档实现者。本机Grok CLI由用户明确授权为Cursor配额耗尽后的Grok4.7备用渠道；本单是一次原生bootstrap，目的是实现统一启动器适配，完成后常规任务全部回Invoke-CursorTask.ps1。新授权优先于尚未更新的旧“仅Cursor”句子。你当前就是被派发的实现者，不递归派任务，不调用其他模型/代理，不修改你自身调用的模型。固守grok-4.7（wire model grok-4.7-build-fast）、reasoning high。

允许读：任务资料、AGENTS.md、.grok/scripts下启动器及其测试、.grok/skills/max-dispatch/SKILL.md、docs/统一开发与术式批量制作流程.md、.cursor/rules、Grok本机C:/Users/baishang/.grok/docs/user-guide中必要CLI/配置/权限/会话文档、只含非敏感配置校验需要的本机配置目标字段。禁止输出/拷贝API密钥、读取其他凭据或整份全局配置；可以在内存解析目标模型配置而仅记录别名、显示名、wire模型、高推理及凭据存在性。

可改路径仅TaskFile.writePaths。主会话独占本批记录.md/任务prompt/设计文档与运行证据，禁止改。禁止业务src、用户全局配置、制作表、其他批次回执；不得git reset/clean/checkout覆盖改动。唯一作者，其他作者停写。没有mod Gradle、打包、游戏、额外模型或正式独立复审授权。

按设计裁决实现最小可用方案：主渠道Cursor固定grok-4.7-high-fast，唯一备用Grok CLI老猪中转站别名grok-4.7/request grok-4.7-build-fast/high。明确的Cursor额度耗尽才允许自动切换；普通错误、网络故障、取消、工具审批拒绝不能触发。历史quota失败必须保留为失败，fallback有独立attempt和provider/session字段；不把Cursor会话当Grok会话。保留batch/task、阶段计数、最新TaskFile/PromptFile、权限范围/依赖/文件所有权/WriteMode。已有旧任务和Status/Result兼容。仅一次有限切换，不后台循环/反复重试，不把失败终态改成功。Grok失败如实交回，不降档、不换其他provider。

Grok每次显式--permission-mode auto，不能继承用户全局always-approve。只读还必须工具限制使写入/shell/MCP/工作流/子代理不可用，不只靠prompt。写入保持auto，不能加always-approve/bypassPermissions/yolo/force或扩大allow来绕过拒绝。禁continue、restore-code、自动换模型，真实resume用本provider确切session。CLI可用--prompt-file、--output-format json，正常stopReason=end_turn，实际格式以本机help/docs与少量真实测试为准。不加载无关MCP、网络搜索或其他模型。凭据留用户原配置，运行日志/任务输入/回执不可含秘密值。

同步AGENTS.md、统一流程第11节、max-dispatch技能，消除与新增授权冲突的绝对“只Cursor/绝不换渠道”句子，其他阶段额度/表格/美术/配置/作者/编译约束不变。不要复制大段规则到新文件；.cursor/rules/execution-provider.mdc如有必要只作简短总入口/优先级指引，现有AGENTS和技能足够则不用增加。

实现后运行必要离线验证：原Cursor测试仍过，Grok正常/失败/缺终局/错session/中断、旧任务兼容、正确限额触发与假阳性不切换、单写锁、跨渠道handoff、只读和写权限参数、固定模型、无密钥记录。离线替身在临时隔离目录，不修改其他真实batch。不要为触发测试消耗已知耗尽的Cursor配额；可使用已有真实限额证据做离线fixture且不修改原证据。如真实CLI验证需要再开/续跑，最多做本批自含只读冒烟；没有实际需求不要重复连通测试。若auto审批实际拒绝工具，不得绕过，只交回阻塞。

最后写实现交付.md：修改文件、真实运行与未运行检查、Grok配置pin与权限映射、自动切换的调用/触发/恢复条件、旧task兼容、session映射/终局归一化、尚存限制。报告不要把自身作者检查当独立复审。确认writesSettled后终止工具调用，最终回复用标记JSON（下面格式），并给必要简短摘要。

<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"具体结果","changedFiles":[],"checks":[],"notRun":[],"writesSettled":true,"blocker":null,"nextAction":"主会话收取结果并按本批额度安排必要集中检查/真实统一入口验证"}
<<<END_CURSOR_TASK_REPORT>>>
