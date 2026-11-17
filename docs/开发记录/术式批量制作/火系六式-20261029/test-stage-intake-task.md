# 更新流程后的六式测试续跑：表与记录对账

固定Cursor CLI --model grok-4.7-high-fast。用户明确授权：按更新后的流程补做六式测试，并实际更新制作表，符合条件迁待验收，否则留制作中写原因。本单是对账和测试输入采集，不是正式正确性复审。

你仍是唯一表记录者d6f84d0b-4ea1-4368-a576-8acc20cc2afd。依当前流程第8节，从现在起授权并由你独占写docs/术式制作表.xlsx和本批记录.md；主会话不再写记录.md，只写调度任务单。原源码作者已停写，编译者亦停写；另一个Cursor正在准备测试入口，只有只读源码和独立prep文档权限。

先完整读更新后的docs/统一开发与术式批量制作流程.md（特别第7.1、8、9、10节）、.grok/skills/max-dispatch/SKILL.md及本批记录。按Spreadsheets技能执行；技能在C:/Users/baishang/.codex/plugins/cache/openai-primary-runtime/spreadsheets/26.1020.11618/skills/spreadsheets/SKILL.md。读取时可复用你此前的保真提取方式；实际编辑仍依技能与用户保真要求。不要浏览任何历史*.jsonl。

当前先只读真实工作簿的四张状态页和资源状态明细，记录SHA256/LastWriteTime，按B对账六个编号fire_liaoyuan/fire_liaoli/fire_fentian/fire_heat_up/fire_burn/fire_cool。比较上次正式input-snapshot与最新DV决定，完整保留现在N/O/DV、所有实际依赖列和资源关联，输出test-stage-input.json及可读test-stage-intake-report.md。不要从历史已完成领取任务。若用户已改需求/位置/编号，逐项列差异，不覆盖不猜。需要待验收转换的快照必须来自现行表而非旧副本。

在记录.md追加本次授权、更新流程版本hash、文件所有权交接、每项当前页/阶段。设计2/2、实现2/2承接，不重置；测试正式结果核对仍0/2（尚未开始），不能把本次读表/状态对账计成正式测试复审。本批现在恢复为待测试，表仍制作中；旧“本轮结束”作为历史保留，不删。只记录事实，不判断代码是否正确或该迁哪页。

本单暂不修改真实工作簿，不做候选迁移；待测试执行与正式结果核对最终交回后，将另派专用写回任务，届时你实际迁符合条件的行。无需再次询问用户接入/测试/正常迁移授权；这些已明确授权。

不得改src/资源/任务单，不Gradle、不游戏、不打包。最终简明报告输入是否与上次一致、六项位置、四页计数、记录已追加及停止写入声明。不要在聊天输出完整长N/O/DV，JSON中保留即可。
