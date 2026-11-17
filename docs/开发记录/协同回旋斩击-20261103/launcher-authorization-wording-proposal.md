# 启动器授权文案单句修正（用户已授权并应用）

文件：`D:/alianximod/.grok/scripts/Invoke-CursorTask.ps1`，`New-CliPrompt`，第505行。

仅替换return字符串内这一句：

> Do not compile, review, migrate the sheet, or accept the game on your own.

拟改为：

> Perform compilation, code review, or sheet migration only when that exact action is explicitly authorized by both the task file and prompt file, and obey their stage, quota, and file-ownership constraints. Do not treat compilation or model-reported completion as user game acceptance.

不会改变固定模型、`--auto-review`、Start/Resume/Status/Result、会话排他、回执结构和其它文案。尤其保留“Do not operate the launcher, run records, or batch quota.”。不会增加阶段复审额度或把编译通过当作游戏通过。

理由：用户已授权本compile-final任务，AGENTS明确由指定Cursor CLI执行编译；外层固定“Do not compile…”句多次使自动审批将该已授权动作判为用户禁止。此改动明确任务授权与禁止自行扩展范围的区别，不关闭自动审批。

用户已明确回复“允许主会话仅修正文案并继续”。主会话只替换上述字符串，保留原UTF-8/BOM及换行，其余文本完全相同。修改前备份在同目录launcher-before-authorized-wording.ps1.txt。后续仍由原Cursor compile-final任务正常审批并执行编译。
