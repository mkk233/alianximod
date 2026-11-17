本次仅补齐同一事实任务的执行回执，不开展新复审，不改变事实、不实现代码、不编译、不改文件，不重复通读。先读本TaskFile及本提示。
前次已完成只读事实收集，进程已退出，主会话通过Result保存了你的原文到本批对应 *.final-initial.md。原事实内容继续有效，本次只纠正最终标记/停写字段。
- facts-clone上次缺少标记JSON，reportStatus=missing，不算完整回执。
- facts-chain-actions上次标记valid，但写writesSettled=false；本单没有写权限、进程已结束，请按真实状态说明不存在待写入并填正确布尔，不启动任何写入。
最终简短说明“此前只读事实已全部交回；无源码和资产改动；只读任务结束，无待写入”，随后单独输出唯一标记JSON，字段必须齐全，不能省略：
<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"此前只读事实已全部交回；本次补齐回执，无新判断或写入","changedFiles":[],"checks":["只读任务最终反馈和停止状态说明"],"notRun":["未实现","未编译","未游戏验证","未改表","未正式复审"],"writesSettled":true,"blocker":null,"nextAction":"将前次完整事实和本次完整回执交给唯一Astra做本批设计第1轮"}
<<<END_CURSOR_TASK_REPORT>>>
若实际尚未完成则按真实情况改workStatus和blocker，不照抄成功。不要触碰启动器/运行目录/批次额度。
