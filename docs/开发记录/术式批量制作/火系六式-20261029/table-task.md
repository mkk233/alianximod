# 本批唯一制作表记录者：领取6项

你是固定Cursor CLI --model grok-4.7-high-fast。仅编辑制作表及本批目录下table前缀的作者脚本、备份、候选、验证记录。禁止src/游戏/Gradle/改本批记录.md。本次仅状态领取与保真检查，不做游戏设计或代码复审。

先读AGENTS.md、docs/统一开发与术式批量制作流程.md，严格执行第8节。真实源表docs/术式制作表.xlsx。仅从当前待制作领取fire_liaoyuan、fire_liaoli、fire_fentian、fire_heat_up、fire_burn、fire_cool，必须读取四状态页、表头、批注、数据表和关联明细并按B核对。源数据发生变化以最新已保存原文为准并返回差异，不静默覆盖。初始快照见input-snapshot.json/xlsx，但不能从备份领任务。

当前源表可能仍被用户程序占用。用户已被请求保存关闭。你可以读取已保存源表，制作独立候选；只有FileShare.None独占窗口能打开且锁内hash与本次基线相等才能提交。打开失败保留候选和完整报告，勿杀Excel、勿强行覆盖、勿等待监控。主会话会在用户关闭后再续交。

将6个有效输入完整A:EN行移入制作中，不更改任何N/O原文，DV仅追加“制作中 | 火系六式-20261029 | docs/开发记录/术式批量制作/火系六式-20261029/记录.md”。不改已完成历史，不填资源名，不改数据表枚举。原样保留值、公式、样式、验证、批注、合并、命名区域；关联资源状态明细按编号保持完整。保存候选前先核对目标完整再删来源，保存关闭重开核对四页总数46且编号唯一；若真实源已有新内容则相应重新计算不能硬扣46。

使用spreadsheets技能C:/Users/baishang/.codex/plugins/cache/openai-primary-runtime/spreadsheets/26.1020.11618/skills/spreadsheets/SKILL.md及必读edit_workflows、style_guidelines、API_QUICK_START。使用bundled node C:/Users/baishang/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe及node/node_modules的artifact-tool。首次authoring前按技能mark_artifact_operation_started.mjs成功一次。禁止openpyxl/xlsxwriter全簿保存。允许参考docs/开发记录/炉心三式入待制作-20261029里已经采用的artifact authoring+定向OOXML保真候选方法，解决工具重写无关格式缺口；不要直接跑旧脚本。只做必要局部渲染和保真核对。

返回实际提交与否、6项源/目标行、四状态页计数、源hash/mtime、备份/候选、原文与保真结果。表行仅制作中；即使代码完成也不能自行移待验收。你是唯一表作者，其他会话不会改表。
