# 灼地单项：唯一制作表写回者，领取与原始留档
模型：Cursor CLI --model grok-4.7-high-fast。工作区 D:/alianximod。本批灼地-20261030。
用户明确要求实现待制作表中灼地。本会话是主会话开批指定的唯一制作表/批次记录写回者 sheet-writer；后续写回继续恢复本会话。另一 Cursor intake 仅只读，不写表、不写源码。
允许写：D:/alianximod/docs/术式制作表.xlsx、D:/alianximod/docs/开发记录/术式批量制作/灼地-20261030/ 下的记录、候选、备份与支持文件。禁止修改 src、其他文档/其他术式内容，禁止编译、打包、游戏、提交。本次只领取，不迁待验收，不判断设计/实现正确性、不消耗校验额度。
完整读取 AGENTS.md、.grok/skills/max-dispatch/SKILL.md、docs/统一开发与术式批量制作流程.md。按第8节、第10节写回任务单执行。读 Spreadsheets 技能 C:/Users/baishang/.codex/plugins/cache/openai-primary-runtime/spreadsheets/26.1020.11618/skills/spreadsheets/SKILL.md 以及其中 edit_workflows.md、API_QUICK_START.md、style_guidelines.md，用 artifact-tool 编辑，遵守窄范围原生保留和视觉检查。源工作簿固定在项目 docs，用户明确要求原位更新优先于技能输出目录默认。不覆盖已有用户改动。
运行时 C:/Users/baishang/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe；包目录同根 node/node_modules；Python 同根 python/python.exe。使用独立本批目录 junction，读参考API，不看依赖内部。首次编辑前只运行一次技能 mark_artifact_operation_started.mjs --operation-kind edit --expected-output-count 1 --output-format xlsx。
重新读取四张状态页、当次表头/批注/资源状态明细；定位名称灼地（预期编号fire_scorch_field但必须以源表为准），按B核对唯一；检查相关现有批次记录，不重复领取或重置已有轮次。其他任务行只对账、不迁移、不制作。已知历史 docs/开发记录/灼地产种设计入表-20261030 是输入编辑记录，不等于已实现。
有效且未领取时，先备份和候选，按B把待制作完整 A:EN 和适用原始资源状态明细首次留进「待制作原始数据」，记录源、时间、版本，只追加首次不覆盖。留档成功才把灼地行待制作->制作中。N/O/DV保持原文，DV仅允许追加本批流程标记。保留数据公式样式验证批注，资源状态明细保留并随编号关联；不得改数据表列表。四页总数守恒编号唯一，候选关闭重开核对，提交前哈希与读取基线一致且此窗口唯一写者才替换，实际源再重开核对并记录新哈希/LastWriteTime。失败不覆盖。
创建/更新 记录.md，保留完整需求快照 N/O/DV、全部非空依赖列、资源明细、批准版本（当前表+此次实现指令）、原始档案来源、哈希/时间、表迁移。写回者为本会话，源码作者尚待指定、编译待统一编译、游戏未跑。历史如果无本编号制作校验记录，本批设计0/2、实现0/3；若有则沿用，不自定新额度。当前唯一 Astra 等待事实未判断，后续由主会话下达准确轮次。记录其他同工作区Codex会话在本次开批 list_threads 中均 idle 或 notLoaded，只有主会话 active；这只是开批快照，最终编译前另查。
交付完整最终报告：本次留档和迁移是否成功、目标行、N/O/DV原文、资源明细、提交后哈希、四页总数唯一性、轮次来源、会话ID（如可知）。本次交付后停止，不修改源码，不进行正式复审。缺少必要输入或编号问题则报告，不补猜。
