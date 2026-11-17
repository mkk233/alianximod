# 唯一作者：制作表资源状态通用化
模型：Cursor CLI --model grok-4.7-high-fast。本阶段：实现（工作簿与表结构说明），不要把本任务当批量术式制作。用户已经明确授权结构迁移，不需再请求许可。

请先完整阅读本目录migration-plan.md和两份read-*-final.md。方案已由唯一gpt-6-astra完成本结构任务设计第1/2轮裁决；同阶段读取均结束，无其它表作者。主会话不再改表，你是唯一写入者。按具体映射完成，自己执行必要文件完整性/结构检查后写回源表，返回最终结果。不要派生代理，不再发起另一轮设计审查，不改火系玩法。

允许写入：
- D:/alianximod/docs/术式制作表.xlsx
- D:/alianximod/docs/统一开发与术式批量制作流程.md，仅表宽、列定位、明细关联等必要结构说明
- D:/alianximod/docs/术式制作表-资源状态字段说明.md
- D:/alianximod/docs/开发记录/制作表资源状态通用化-20261028/下自己创建的migration-*、候选、备份、预览及报告文件；不要修改记录.md、已有read-*报告或run-cursor.ps1。
禁止写任何src、AGENTS、技能文件或其他术式文档；禁止Gradle/打包/游戏/提交/改制作状态/重新设计术式。

源表当前用户已保存并关闭，最后核对SHA256 486D6229C56EB22CEA9BA2BE02E273640BB5F16D77F4E91CDDE34024F30D7A50，更新时间2026-10-28 19:35:56。开始时重读源哈希与时间，提交前再核对。在独占写入窗口更新；有变化不覆盖，按ID重新对账；如仅解锁无需再次问用户。现有input-snapshot.xlsx与源一致，但另建时间戳备份后再写候选。

必须使用Spreadsheets技能：
C:/Users/baishang/.codex/plugins/cache/openai-primary-runtime/spreadsheets/26.1020.11618/skills/spreadsheets/SKILL.md
读全技能及workflows/edit_workflows.md、artifact_tool_docs/API_QUICK_START.md。使用bundled依赖：
Node C:/Users/baishang/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe
node_modules C:/Users/baishang/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules
Python C:/Users/baishang/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe（-X utf8）
在本记录目录建node_modules junction；用@oai/artifact-tool进行authoring，不能直接openpyxl.save。作者首次create/edit执行前恰好一次运行技能container_tools/mark_artifact_operation_started.mjs --operation-kind edit --expected-output-count 1 --output-format xlsx。只标记这一个新迁移操作；上一轮三式入表的标记不是本次。

保真已知问题：Artifact Tool完整导出会归一化旧样式、把部分空值变空字符串。上次已经验证通过的做法在D:/alianximod/docs/开发记录/火属性二级设计评审-20261028/edit_workbook.mjs、preserve_original.py、verify_workbook.py，可参考不修改。此次可以artifact-tool生成新单元格/结构，随后针对缺失保真能力进行最小OOXML合入，确保原A:DV数据不被改写；不要把旧版整包恢复覆盖新的通用字段。数据表新字典和命名范围、明细页、相关关系Content_Types、数据验证都要正确落盘且可重开。

先读/渲染旧表关键范围，候选写完recalculate/inspect/公式错误检查并渲染新增通用组、数据表字典与明细。图片存本目录供主会话查看。主会话只在你完成之后读取结果，你无需阶段性汇报。行高、列宽、冻结、旧批注保持；新列清楚可读，长结算说明适当宽度/换行，别扩大所有旧行高度影响旧布局。

迁移映射遵循migration-plan.md逐项列表，计数从实际数据计算，不按照报告摘要硬凑条数。把“表内字段=已配置”和“代码已支持通用字段”区分；目前代码仍固定字段，说明未来实现须适配/映射，不能声称改表即改游戏。旧水列标旧档但不删除数值，4页同步。新选项允许在数据表连续列表追加后自动出现在下拉，实际验证至少新增一项再回滚的临时副本或原生名称公式边界，报告可验证范围。禁止为了新字典顺手纠正旧数据表枚举。

若额外帮助查询有限，按技能文档；不用反复探索包内部。若工具无法可靠保真，保留候选/备份并报告具体阻塞，不损坏源表。

完成后最终报告：
1. 已写回/未写回、输出绝对路径、前后SHA256、备份路径。
2. 新列范围、数据表具体新列表范围/名称、明细记录数与主表配置数及禁用数，迁移影响ID。
3. 必要检查的结果与图片路径，保留N/O/DV和43ID的证据摘要。
4. 修改的说明文档与行号。
5. 未解决的旧表/代码差异、未运行项（代码未改、不编译、不游戏）。
交付控制约2000中文字符，日志和详细映射写自己的报告文件。
