# 三式刻印台实测反馈：实际备注写回

模型 Cursor CLI --model grok-4.7-high-fast。你仍是唯一表格/记录者 d6f84d0b-4ea1-4368-a576-8acc20cc2afd。
本阶段：反馈记录和实际表同步，不是追加实现或测试复审。设计2/2、实现2/2、测试1/2保持。

最终输入已到齐：unlock-repair-report.md、unlock-repair-evidence.json、unlock-intake-report.md/json。源码作者已最终交回且无任何生产/测试源码修改，无需重跑未变源码编译。本单不重新读源码判断正确性。

用户刚明确当时用的是两条命令：
/shu rank sword 20
/shu rank magic 20

只转记 Cursor 定位结果：当前实现等级门槛与已学会分开，刻印台同时要求；这两条命令提高等级但不把三式加入已学会名单。三式当前已注册，已有 /shu learn fire_fentian、/shu learn fire_liaoyuan、/shu learn fire_liaoli 可定向学习，重新打开刻印台检查。/shu stone 只给物品不写学习名单。获取方式原备注仍待用户另定；不要把作者建议重写成用户新批准，不自动改为满级授予。若当前游戏 /shu list 没有三式，运行版本另需排查；目前没有操作或验证游戏。用户仅报告实测异常，未说验收通过。

实际写回范围：仅 fire_fentian、fire_liaoyuan、fire_liaoli 三行 DV 追加带日期的本次实测反馈、准确命令、学习前置、作者定位且未改源码、待重测，并链接 unlock-repair-report.md；保留 N/O 原文以及 DV 全部原话和历史反馈。状态均仍制作中，不应用候选N/O、不迁待验收、不更改其余三式DV和历史已完成。按B重读对账，保留A:EN、资源状态明细和所有原始格式结构。

使用此前已读取的 spreadsheets skill 和正式流程第8节安全写回。重算当前基线、唯一时间戳备份、独立候选、关闭重开核对编号唯一/总数/三处DV与N/O保留；提交时确认唯一写入及源哈希未变，持有实际独占窗口后替换，提交后重开实际源表并记新hash/mtime。若源表变化按编号重对账，不覆盖新改动。若锁占用，保留候选返回具体阻塞。

你可写 docs/术式制作表.xlsx、当前批次记录.md追加章节、unlock-writeback-report.md/json、独立备份候选和本次专用工具文件。不得改src、资源库索引、既有报告、测试额度，禁止Gradle/游戏/提交。可复用已验证的工作簿保真写回方法，不重复无关探索。

最后返回三项实际来源→目标、具体新增备注摘要、备份、前后hash/mtime、重开核对和总数、明确停写并释放占用。完整报告写 unlock-writeback-report.md。保留原先六式未满足待验收的原因，不能因这次前置定位而宣称全批通过。
