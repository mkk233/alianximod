# 作者B：同步作者A最终报告指出的统计说明差异

固定Cursor CLI --model grok-4.7-high-fast，复用作者B会话。A/B已经全部最终返回并释放源码写入权。本任务是已知说明差异的收束，不是新设计/正式复审；计数仍设计2/2、实现2/3。禁止编译、测试、游戏、打包或额外代理。

读本目录implementation-A-final.md中的计数/风险段与作者B现行PerfMetric注释、性能使用说明。作者A最终反馈：原各调用方外围TARGET_QUERY计时已移入LivingCandidateQueries仅包真实getEntitiesOfClass；锁定距离循环、准星方块射线与锚点选择不再计入TARGET_QUERY。B的“射线仍留在该项”注释因此过时。实现任务约定查询计时只计实际底层空间查询，复用不加；此任务只让文档与该实际边界一致，不恢复旧外围计时，不修改A源码。

允许修改仍限作者B的PerfMetric.java注释、PerformanceRecorder.java的note_*说明文字（如相关）、docs/性能记录使用说明.md。不得改变任何Java可执行逻辑、枚举顺序、计数/CSV、源码调用方或其他文件。清楚标明TARGET_QUERY现在只包含实际LivingEntity空间查询，不含查后距离选择和准星方块射线；新旧报告该项口径发生收窄，TrackingSupport接入ENTITY的扩大口径继续保留。A的docs/候选查询复用说明.md若已说明此事实，不需要你修改。

将实际拟修改文件另存backup-B-stat-notes/保留原backup-B/；写repair-B-stat-notes-final.md、evidence-B-stat-notes.json记录修改文件、最终hash/UTCmtime、released=true和结束时间。所有文档/注释写入完结后返回并释放，不另审A或宣称编译成功。
