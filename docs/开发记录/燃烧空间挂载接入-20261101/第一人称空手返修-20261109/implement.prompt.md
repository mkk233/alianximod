你是燃烧返修同一任务 burn-spatial-implementation 的唯一源码作者，承接当前 Cursor 会话2450561c-3d9a-4398-b2de-82ee839f6270。执行主会话已授权的最小修复，不追加独立复审，检查是作者必要自查。遵循当前 AGENTS.md 和 docs/统一开发与术式批量制作流程.md。保留所有现存未提交修改。

用户问题：第三人称与批准预览完全一致，第一人称空手时火焰却在手下方。当前你已最终返回的诊断 source-diagnosis.md 指出 BurnFirstPersonPose.applyArmPivot 的 X 符号反了。当前阶段只按该定位修复该方法的 X：sideSign * 5/16 改成 -sideSign * 5/16，保留 Y、Z、zRot 和原版渲染变换；如发现诊断不能成立则先报告，不扩大改动。不得用镜头固定上移补偿。现有手焰随整只手臂转动的设计保持。不得改第三人称 FurnaceBodyLayer、持物 translateGrip、网格、纹理、游戏逻辑或配置。

唯一允许修改的 Java：src/main/java/com/bslx/XueXiMod/client/furnace/BurnFirstPersonPose.java。
唯一报告：本提示同目录 implementation-report.md。不写主会话记录.md、不写表格或资源库索引。制作表的独立作者已完成前置回执，依赖见 task.json。

修改前读取文件现状并保存仅本次差异的前后证据，避免把旧改动算本次。按真实空手左右主手及地图复用分支确认符号改动会一致作用；持物分支应原样。进行作者必要自查与命名门禁 powershell -NoProfile -File tools/check-name-gate.ps1。不要启动编译，编译在下一明确授权阶段；不 build/jar/reobf、不 runClient、不游戏、不桌面控制、不改素材。

报告写明根因、精确改动和文件行号、本次最小差异、必要自查/门禁实际结果、未运行内容，且记录源码唯一作者、待编译源文件hash与写入已停止。交付四行用户测试卡：编号/名称 fire_burn 燃烧；前置 启用燃烧过载且主手空手；步骤 第一人称左右主手站立/走动/挥手，再看持物与第三人称；预期 火焰随拳头无下移，已批准第三人称保持。游戏实际效果由用户测，禁止宣称游戏通过。

最终返回必须包含且严格使用：
<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"实际结果","changedFiles":[],"checks":[],"notRun":[],"writesSettled":true,"blocker":null,"nextAction":"等待统一串行编译授权"}
<<<END_CURSOR_TASK_REPORT>>>
若未完成，如实用 blocked/needs_input/failed。不要把只启动或计划当完成。
