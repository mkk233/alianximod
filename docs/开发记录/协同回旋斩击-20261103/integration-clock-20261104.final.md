先读任务文件和这次时钟提示，只按它们写明的范围做。回旋和斜斩的对外开始刻用了开机刻，客户端却用世界年龄相减。内部调度仍留在开机刻上，只改这两处发布。回旋和斜斩发给客户端的开始刻，已经改成玩家所在世界的 `getGameTime()`。内部的 `runningTick` 仍用开机刻计算前摇 6 刻和收势 6 刻，释放门和冷却没有跟着换钟。

发布器的缓存到期和追踪包本来就用世界年龄相减，包只原样传递这个数值，所以没有要交给视觉侧改的时钟待办。

<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"回旋与斜斩的 ActionPlayback.serverStartTick 已改为玩家所在 ServerLevel.getGameTime()。runningTick 与 startTick 仍是 MinecraftServer.getTickCount()，内部时长和前摇6/收势6不变。发布器缓存与追踪包已使用世界年龄，无新增视觉待办。未改远距 API 与距离默认。命名门禁通过。未编译、未进游戏。","changedFiles":["src/main/java/com/bslx/XueXiMod/shu/technique/release/cooperative/FamilyExecutionService.java","docs/开发记录/协同回旋斩击-20261103/integration-clock-20261104-delivery.md"],"checks":["powershell -NoProfile -File tools/check-name-gate.ps1 退出码 0"],"notRun":["未统一编译","未打包","未进游戏","未正式复审","未写回制作表","未改 CooperativeContent 距离默认","未改配置指南或世界文件"],"writesSettled":true,"blocker":null,"nextAction":"主会话转达时钟修复。客户端临时钟差兜底不是本服务端发布的替代。本回执不是游戏验收。"}
<<<END_CURSOR_TASK_REPORT>>>
